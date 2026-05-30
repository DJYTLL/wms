package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.entity.SystemConfig;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCounterpartyPendingDoc;
import com.example.wms.dto.erp.ErpCounterpartyUnbindCheck;
import com.example.wms.dto.erp.ErpSupplierCreateRequest;
import com.example.wms.dto.erp.ErpSupplierImportResult;
import com.example.wms.dto.erp.ErpSupplierImportResultItem;
import com.example.wms.dto.erp.ErpSupplierUpdateRequest;
import com.example.wms.entity.erp.ErpAccountsPayable;
import com.example.wms.entity.erp.ErpCounterpartySubjectLink;
import com.example.wms.entity.erp.ErpPayment;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.entity.erp.ErpPurchaseReturn;
import com.example.wms.entity.erp.ErpSupplierImportBatch;
import com.example.wms.entity.erp.ErpSupplierImportItem;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.entity.erp.ErpSupplierType;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpCounterpartySubjectLinkMapper;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPaymentMethodMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.erp.ErpSupplierImportBatchMapper;
import com.example.wms.mapper.erp.ErpSupplierImportItemMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpSupplierTypeMapper;
import com.example.wms.service.erp.ErpSupplierService;
import com.example.wms.dto.erp.ErpSupplierImportBatchSummary;
import com.example.wms.dto.erp.ErpSupplierImportItemView;
import com.example.wms.service.erp.support.ExcelImportParser;
import com.example.wms.service.erp.support.ExcelImportSheet;
import com.example.wms.service.erp.support.ErpCounterpartyGuardRules;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

// 供应商服务实现（ERP进销存）
@Service
public class ErpSupplierServiceImpl implements ErpSupplierService {
    private static final String SUPPLIER_CODE_TYPE = "SUPPLIER";
    private static final String DEFAULT_BUSINESS_SCOPE = "SUPPLIER";
    private static final int IMPORT_CHUNK_SIZE = 100;

    private final ErpSupplierMapper erpSupplierMapper;
    private final ErpPurchaseOrderMapper erpPurchaseOrderMapper;
    private final ErpPurchaseReturnMapper erpPurchaseReturnMapper;
    private final ErpPaymentMapper erpPaymentMapper;
    private final ErpAccountsPayableMapper erpAccountsPayableMapper;
    private final ErpSettlementMethodMapper erpSettlementMethodMapper;
    private final ErpPaymentMethodMapper erpPaymentMethodMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ErpSupplierTypeMapper erpSupplierTypeMapper;
    private final ErpSupplierImportBatchMapper erpSupplierImportBatchMapper;
    private final ErpSupplierImportItemMapper erpSupplierImportItemMapper;
    private final ErpCounterpartySubjectLinkMapper erpCounterpartySubjectLinkMapper;
    private final ObjectMapper objectMapper;
    private final ExcelImportParser excelImportParser;
    private final Executor importExecutor;
    private final TransactionOperations transactionOperations;

    @Autowired
    public ErpSupplierServiceImpl(ErpSupplierMapper erpSupplierMapper,
                                  ErpPurchaseOrderMapper erpPurchaseOrderMapper,
                                  ErpPurchaseReturnMapper erpPurchaseReturnMapper,
                                  ErpPaymentMapper erpPaymentMapper,
                                  ErpAccountsPayableMapper erpAccountsPayableMapper,
                                  ErpSettlementMethodMapper erpSettlementMethodMapper,
                                  ErpPaymentMethodMapper erpPaymentMethodMapper,
                                  ErpOrderSequenceMapper erpOrderSequenceMapper,
                                  SystemConfigMapper systemConfigMapper,
                                  ErpSupplierTypeMapper erpSupplierTypeMapper,
                                  ErpSupplierImportBatchMapper erpSupplierImportBatchMapper,
                                  ErpSupplierImportItemMapper erpSupplierImportItemMapper,
                                  ErpCounterpartySubjectLinkMapper erpCounterpartySubjectLinkMapper,
                                  ObjectMapper objectMapper,
                                  ExcelImportParser excelImportParser,
                                  @Qualifier("erpImportTaskExecutor") Executor importExecutor,
                                  PlatformTransactionManager transactionManager) {
        this(
            erpSupplierMapper,
            erpPurchaseOrderMapper,
            erpPurchaseReturnMapper,
            erpPaymentMapper,
            erpAccountsPayableMapper,
            erpSettlementMethodMapper,
            erpPaymentMethodMapper,
            erpOrderSequenceMapper,
            systemConfigMapper,
            erpSupplierTypeMapper,
            erpSupplierImportBatchMapper,
            erpSupplierImportItemMapper,
            erpCounterpartySubjectLinkMapper,
            objectMapper,
            excelImportParser,
            importExecutor,
            new TransactionTemplate(transactionManager)
        );
    }

    public ErpSupplierServiceImpl(ErpSupplierMapper erpSupplierMapper,
                                  ErpPurchaseOrderMapper erpPurchaseOrderMapper,
                                  ErpPurchaseReturnMapper erpPurchaseReturnMapper,
                                  ErpPaymentMapper erpPaymentMapper,
                                  ErpAccountsPayableMapper erpAccountsPayableMapper,
                                  ErpSettlementMethodMapper erpSettlementMethodMapper,
                                  ErpPaymentMethodMapper erpPaymentMethodMapper,
                                  ErpOrderSequenceMapper erpOrderSequenceMapper,
                                  SystemConfigMapper systemConfigMapper,
                                  ErpSupplierTypeMapper erpSupplierTypeMapper,
                                  ErpSupplierImportBatchMapper erpSupplierImportBatchMapper,
                                  ErpSupplierImportItemMapper erpSupplierImportItemMapper,
                                  ErpCounterpartySubjectLinkMapper erpCounterpartySubjectLinkMapper,
                                  ObjectMapper objectMapper,
                                  ExcelImportParser excelImportParser) {
        this(
            erpSupplierMapper,
            erpPurchaseOrderMapper,
            erpPurchaseReturnMapper,
            erpPaymentMapper,
            erpAccountsPayableMapper,
            erpSettlementMethodMapper,
            erpPaymentMethodMapper,
            erpOrderSequenceMapper,
            systemConfigMapper,
            erpSupplierTypeMapper,
            erpSupplierImportBatchMapper,
            erpSupplierImportItemMapper,
            erpCounterpartySubjectLinkMapper,
            objectMapper,
            excelImportParser,
            Runnable::run,
            (TransactionOperations) null
        );
    }

    public ErpSupplierServiceImpl(ErpSupplierMapper erpSupplierMapper,
                                  ErpPurchaseOrderMapper erpPurchaseOrderMapper,
                                  ErpPurchaseReturnMapper erpPurchaseReturnMapper,
                                  ErpPaymentMapper erpPaymentMapper,
                                  ErpAccountsPayableMapper erpAccountsPayableMapper,
                                  ErpSettlementMethodMapper erpSettlementMethodMapper,
                                  ErpPaymentMethodMapper erpPaymentMethodMapper,
                                  ErpOrderSequenceMapper erpOrderSequenceMapper,
                                  SystemConfigMapper systemConfigMapper,
                                  ErpSupplierTypeMapper erpSupplierTypeMapper,
                                  ErpSupplierImportBatchMapper erpSupplierImportBatchMapper,
                                  ErpSupplierImportItemMapper erpSupplierImportItemMapper,
                                  ErpCounterpartySubjectLinkMapper erpCounterpartySubjectLinkMapper,
                                  ObjectMapper objectMapper,
                                  ExcelImportParser excelImportParser,
                                  Executor importExecutor,
                                  TransactionOperations transactionOperations) {
        this.erpSupplierMapper = erpSupplierMapper;
        this.erpPurchaseOrderMapper = erpPurchaseOrderMapper;
        this.erpPurchaseReturnMapper = erpPurchaseReturnMapper;
        this.erpPaymentMapper = erpPaymentMapper;
        this.erpAccountsPayableMapper = erpAccountsPayableMapper;
        this.erpSettlementMethodMapper = erpSettlementMethodMapper;
        this.erpPaymentMethodMapper = erpPaymentMethodMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.erpSupplierTypeMapper = erpSupplierTypeMapper;
        this.erpSupplierImportBatchMapper = erpSupplierImportBatchMapper;
        this.erpSupplierImportItemMapper = erpSupplierImportItemMapper;
        this.erpCounterpartySubjectLinkMapper = erpCounterpartySubjectLinkMapper;
        this.objectMapper = objectMapper;
        this.excelImportParser = excelImportParser;
        this.importExecutor = importExecutor == null ? Runnable::run : importExecutor;
        this.transactionOperations = transactionOperations;
    }

    public ErpSupplierServiceImpl(ErpSupplierMapper erpSupplierMapper,
                                  ErpPurchaseOrderMapper erpPurchaseOrderMapper,
                                  ErpPurchaseReturnMapper erpPurchaseReturnMapper,
                                  ErpPaymentMapper erpPaymentMapper,
                                  ErpAccountsPayableMapper erpAccountsPayableMapper,
                                  ErpSettlementMethodMapper erpSettlementMethodMapper,
                                  ErpPaymentMethodMapper erpPaymentMethodMapper,
                                  ErpOrderSequenceMapper erpOrderSequenceMapper,
                                  SystemConfigMapper systemConfigMapper,
                                  ErpSupplierTypeMapper erpSupplierTypeMapper,
                                  ErpSupplierImportBatchMapper erpSupplierImportBatchMapper,
                                  ErpSupplierImportItemMapper erpSupplierImportItemMapper,
                                  ErpCounterpartySubjectLinkMapper erpCounterpartySubjectLinkMapper,
                                  ObjectMapper objectMapper) {
        this(
            erpSupplierMapper,
            erpPurchaseOrderMapper,
            erpPurchaseReturnMapper,
            erpPaymentMapper,
            erpAccountsPayableMapper,
            erpSettlementMethodMapper,
            erpPaymentMethodMapper,
            erpOrderSequenceMapper,
            systemConfigMapper,
            erpSupplierTypeMapper,
            erpSupplierImportBatchMapper,
            erpSupplierImportItemMapper,
            erpCounterpartySubjectLinkMapper,
            objectMapper,
            new ExcelImportParser()
        );
    }

    @Override
    public List<ErpSupplier> listAll(String keyword, String contact, String phone, String status) {
        Long tenantId = TenantContext.requireTenantId();
        QueryWrapper<ErpSupplier> wrapper = baseWrapper(tenantId, keyword, contact, phone, status);
        wrapper.orderByAsc("id");
        List<ErpSupplier> suppliers = erpSupplierMapper.selectList(wrapper);
        enrichRecentTransactionAt(tenantId, suppliers);
        return suppliers;
    }

    @Override
    public PageResponse<ErpSupplier> page(long page, long size, String keyword, String contact, String phone, String status) {
        Long tenantId = TenantContext.requireTenantId();
        Page<ErpSupplier> pageReq = Page.of(page, size);
        QueryWrapper<ErpSupplier> wrapper = baseWrapper(tenantId, keyword, contact, phone, status);
        wrapper.orderByAsc("id");
        Page<ErpSupplier> result = erpSupplierMapper.selectPage(pageReq, wrapper);
        enrichRecentTransactionAt(tenantId, result.getRecords());
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpSupplier getById(Long id) {
        ErpSupplier supplier = erpSupplierMapper.selectOne(new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        return supplier;
    }

    @Override
    public ErpCounterpartyUnbindCheck checkRebind(Long id, Long targetSubjectId) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSupplier supplier = erpSupplierMapper.selectOne(new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        if (Objects.equals(supplier.getCounterpartySubjectId(), targetSubjectId)) {
            return new ErpCounterpartyUnbindCheck(true, List.of(), List.of());
        }
        return buildSupplierRebindCheck(tenantId, id);
    }

    @Override
    public String nextCode() {
        Long tenantId = TenantContext.requireTenantId();
        return generateSupplierCode(tenantId);
    }

    @Override
    @AuditLog(action = "ERP_SUPPLIER_CREATE", entityType = "erp_supplier", entityId = "{result.id}", detail = "code={arg0.code}")
@Transactional
    public ErpSupplier create(ErpSupplierCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSupplier existing = erpSupplierMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("供应商编码已存在");
        }
        ErpSupplier supplier = new ErpSupplier();
        supplier.setTenantId(tenantId);
        Long oldSubjectId = supplier.getCounterpartySubjectId();
        applyRequest(supplier, request);
        syncCounterpartySubjectLink(tenantId, supplier.getId(), oldSubjectId, supplier.getCounterpartySubjectId());
        applyDefaultMethodsIfMissing(supplier, tenantId);
        applyStatus(supplier, request.enabled(), request.blacklisted());
        supplier.setCreatedAt(Instant.now());
        supplier.setUpdatedAt(Instant.now());
        erpSupplierMapper.insert(supplier);
        return supplier;
    }

    @Override
    @AuditLog(action = "ERP_SUPPLIER_UPDATE", entityType = "erp_supplier", entityId = "{arg0}", detail = "code={arg1.code}")
@Transactional
    public ErpSupplier update(Long id, ErpSupplierUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSupplier supplier = erpSupplierMapper.selectOne(new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        ErpSupplier existing = erpSupplierMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("供应商编码已存在");
        }
        applyRequest(supplier, request);
        applyDefaultMethodsIfMissing(supplier, tenantId);
        if (request.enabled() != null || request.blacklisted() != null) {
            applyStatus(supplier, request.enabled(), request.blacklisted());
        }
        supplier.setUpdatedAt(Instant.now());
        erpSupplierMapper.updateById(supplier);
        return supplier;
    }

    @Override
    @AuditLog(action = "ERP_SUPPLIER_DELETE", entityType = "erp_supplier", entityId = "{arg0}")
@Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSupplier supplier = erpSupplierMapper.selectOne(new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        ensureSupplierNotReferenced(tenantId, id);
        erpSupplierMapper.deleteById(id);
    }

    @Override
    public ErpSupplierImportResult importSuppliers(MultipartFile file, String sourceName) {
        Long tenantId = TenantContext.requireTenantId();
        String uploadedFileName = file == null ? null : trimToNull(file.getOriginalFilename());
        String resolvedSourceName = trimToNull(sourceName);
        if (resolvedSourceName == null) {
            resolvedSourceName = uploadedFileName == null ? "供应商历史表" : uploadedFileName;
        }
        final String finalSourceName = resolvedSourceName;
        byte[] fileBytes = readImportBytes(file);
        ExcelImportSheet sheet = parseImportSheet(uploadedFileName, fileBytes);
        if (sheet.rows().isEmpty()) {
            throw new IllegalArgumentException("导入内容没有有效数据行");
        }
        ErpSupplierImportBatch batch = createImportBatch(tenantId, uploadedFileName, finalSourceName, sheet);
        scheduleSupplierImportBatch(batch.getId(), tenantId, uploadedFileName, fileBytes, finalSourceName);
        return new ErpSupplierImportResult(batch.getId(), batch.getBatchNo(), batch.getStatus(), batch.getTotalCount(), 0, 0, List.of());
    }

    private ErpSupplierImportBatch createImportBatch(Long tenantId,
                                                     String uploadedFileName,
                                                     String sourceName,
                                                     ExcelImportSheet sheet) {
        Instant now = Instant.now();
        ErpSupplierImportBatch batch = new ErpSupplierImportBatch();
        batch.setTenantId(tenantId);
        batch.setBatchNo("SI" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault()).format(now));
        batch.setSourceName(sourceName);
        batch.setImportMode("EXCEL_UPLOAD");
        batch.setTotalCount(sheet.rows().size());
        batch.setSuccessCount(0);
        batch.setFailedCount(0);
        batch.setUncategorizedCount(0);
        batch.setSettlementUnmatchedCount(0);
        batch.setPendingSubjectMergeCount(0);
        batch.setStatus("PROCESSING");
        batch.setSummary("导入任务已创建，正在后台处理");
        batch.setRawPayload(valueToTree(Map.of(
            "filename", uploadedFileName == null ? "" : uploadedFileName,
            "headers", sheet.headers(),
            "rowCount", sheet.rows().size()
        )));
        batch.setCreatedBy(resolveCurrentUsername());
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);
        erpSupplierImportBatchMapper.insert(batch);
        return batch;
    }

    private void processSupplierImportBatch(Long batchId,
                                            Long tenantId,
                                            String uploadedFileName,
                                            byte[] fileBytes,
                                            String sourceName) {
        TenantContext.setTenantId(tenantId);
        try {
            ExcelImportSheet sheet = parseImportSheet(uploadedFileName, fileBytes);
            SupplierImportReferenceData referenceData = preloadImportReferenceData(tenantId, sheet.rows());
            ImportBatchCounters counters = ImportBatchCounters.empty();
            for (int start = 0; start < sheet.rows().size(); start += IMPORT_CHUNK_SIZE) {
                int end = Math.min(start + IMPORT_CHUNK_SIZE, sheet.rows().size());
                int chunkStart = start;
                int chunkEnd = end;
                ImportBatchCounters chunkCounters = executeInImportTransaction(() ->
                    processSupplierImportRows(batchId, tenantId, sheet.rows().subList(chunkStart, chunkEnd), chunkStart, referenceData)
                );
                counters = counters.plus(chunkCounters);
            }
            ImportBatchCounters finalCounters = counters;
            executeInImportTransaction(() -> {
                updateImportBatchResult(
                    batchId,
                    tenantId,
                    sheet.rows().size(),
                    finalCounters.successCount(),
                    finalCounters.failedCount(),
                    finalCounters.uncategorizedCount(),
                    finalCounters.settlementUnmatchedCount(),
                    finalCounters.pendingSubjectMergeCount(),
                    null
                );
                return null;
            });
        } catch (Exception ex) {
            executeInImportTransaction(() -> {
                updateImportBatchResult(batchId, tenantId, 0, 0, 0, 0, 0, 0, ex.getMessage());
                return null;
            });
        } finally {
            TenantContext.clear();
        }
    }

    private void scheduleSupplierImportBatch(Long batchId,
                                             Long tenantId,
                                             String uploadedFileName,
                                             byte[] fileBytes,
                                             String sourceName) {
        Runnable task = () -> processSupplierImportBatch(batchId, tenantId, uploadedFileName, fileBytes, sourceName);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    CompletableFuture.runAsync(task, importExecutor);
                }
            });
        } else {
            CompletableFuture.runAsync(task, importExecutor);
        }
    }

    private ImportBatchCounters processSupplierImportRows(Long batchId,
                                                          Long tenantId,
                                                          List<Map<String, String>> rows,
                                                          int startIndex,
                                                          SupplierImportReferenceData referenceData) {
        int successCount = 0;
        int failedCount = 0;
        int uncategorizedCount = 0;
        int settlementUnmatchedCount = 0;
        int pendingSubjectMergeCount = 0;
        List<ErpSupplierImportItem> importItems = new ArrayList<>();

        for (int offset = 0; offset < rows.size(); offset++) {
            int rowNo = startIndex + offset + 2;
            Map<String, String> rowMap = new HashMap<>(rows.get(offset));

            String code = trimToNull(rowMap.get("编码"));
            String name = trimToNull(rowMap.get("名称"));
            String settlementName = trimToNull(rowMap.get("默认结算方式"));
            String supplierTypeName = trimToNull(rowMap.get("客户类型"));
            String businessScope = "SUPPLIER";
            String errorField = null;
            String errorMessage = null;
            String suggestion = null;
            String warningMessage = null;
            String matchedStrategy = null;

            if (code == null) {
                errorField = "编码";
                errorMessage = "编码不能为空";
                suggestion = "补充编码后重试";
            } else if (name == null) {
                errorField = "名称";
                errorMessage = "名称不能为空";
                suggestion = "补充名称后重试";
            }

            String settlementCode = null;
            Long supplierTypeId = null;
            if (errorField == null) {
                try {
                    businessScope = normalizeBusinessScopeByImport(rowMap.get("往来类别"));
                    settlementCode = resolveSettlementMethodCode(referenceData, settlementName);
                    if (settlementName != null && settlementCode == null) {
                        settlementUnmatchedCount++;
                        warningMessage = mergeWarnings(warningMessage, "默认结算方式未匹配，已按系统默认结算方式处理");
                    }

                    supplierTypeId = resolveSupplierTypeId(referenceData, supplierTypeName);
                    if (errorField == null && supplierTypeName != null && supplierTypeId == null) {
                        ErpSupplierType uncategorizedType = resolveUncategorizedSupplierType(referenceData);
                        if (uncategorizedType != null) {
                            supplierTypeId = uncategorizedType.getId();
                            warningMessage = "供应商类型未匹配，已落到未分类";
                            uncategorizedCount++;
                        } else {
                            errorField = "客户类型";
                            errorMessage = "供应商类型未匹配，且系统未配置未分类类型";
                            suggestion = "执行 V125 后重试";
                        }
                    }
                } catch (IllegalArgumentException ex) {
                    errorField = "往来类别";
                    errorMessage = ex.getMessage();
                    suggestion = "按导入口径修正后重试";
                }
            }

            ErpSupplierImportItem importItem = new ErpSupplierImportItem();
            importItem.setTenantId(tenantId);
            importItem.setBatchId(batchId);
            importItem.setRowNo(rowNo);
            importItem.setSourceCode(code);
            importItem.setSourceName(name);
            importItem.setSupplierTypeName(supplierTypeName);
            importItem.setSettlementMethodName(settlementName);
            importItem.setEnterpriseMatch(trimToNull(rowMap.get("企业匹配")));
            importItem.setPriceLevel(trimToNull(rowMap.get("价格级别")));
            importItem.setStatus(errorField == null ? "SUCCESS" : "FAILED");
            importItem.setErrorField(errorField);
            importItem.setErrorMessage(errorMessage);
            importItem.setSuggestion(suggestion);
            importItem.setWarningMessage(warningMessage);
            importItem.setRawRow(valueToTree(rowMap));
            Instant rowNow = Instant.now();
            importItem.setCreatedAt(rowNow);
            importItem.setUpdatedAt(rowNow);

            if (errorField == null) {
                ImportUpsertOutcome outcome = upsertImportedSupplier(referenceData, tenantId, code, name, rowMap, settlementCode, supplierTypeId, businessScope);
                ErpSupplier supplier = outcome.supplier();
                importItem.setMatchedSupplierId(supplier.getId());
                matchedStrategy = outcome.created() ? "CODE_UPSERT" : "CODE_UPDATE";
                importItem.setMatchedStrategy(matchedStrategy);
                String mergedWarning = mergeWarnings(warningMessage, outcome.warningMessage());
                importItem.setWarningMessage(mergedWarning);
                if (trimToNull(rowMap.get("企业匹配")) != null) {
                    pendingSubjectMergeCount++;
                }
                importItem.setNormalizedPayload(valueToTree(Map.of(
                    "code", supplier.getCode(),
                    "name", supplier.getName(),
                    "defaultSettlementMethodCode", settlementCode == null ? "" : settlementCode,
                    "supplierTypeId", supplierTypeId == null ? "" : supplierTypeId,
                    "businessScope", businessScope
                )));
                successCount++;
            } else {
                failedCount++;
            }

            importItems.add(importItem);
        }

        if (!importItems.isEmpty()) {
            erpSupplierImportItemMapper.insertBatch(importItems);
        }
        return new ImportBatchCounters(successCount, failedCount, uncategorizedCount, settlementUnmatchedCount, pendingSubjectMergeCount);
    }

    private <T> T executeInImportTransaction(Callable<T> action) {
        if (transactionOperations != null) {
            return transactionOperations.execute(status -> {
                try {
                    return action.call();
                } catch (RuntimeException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            });
        }
        try {
            return action.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void updateImportBatchResult(Long batchId,
                                         Long tenantId,
                                         int totalCount,
                                         int successCount,
                                         int failedCount,
                                         int uncategorizedCount,
                                         int settlementUnmatchedCount,
                                         int pendingSubjectMergeCount,
                                         String fatalMessage) {
        ErpSupplierImportBatch batch = erpSupplierImportBatchMapper.selectOne(new QueryWrapper<ErpSupplierImportBatch>()
            .eq("tenant_id", tenantId)
            .eq("id", batchId)
            .isNull("deleted_at"));
        if (batch == null) {
            return;
        }
        batch.setTotalCount(totalCount > 0 ? totalCount : batch.getTotalCount());
        batch.setSuccessCount(successCount);
        batch.setFailedCount(failedCount);
        batch.setUncategorizedCount(uncategorizedCount);
        batch.setSettlementUnmatchedCount(settlementUnmatchedCount);
        batch.setPendingSubjectMergeCount(pendingSubjectMergeCount);
        batch.setUpdatedAt(Instant.now());
        if (fatalMessage != null && !fatalMessage.isBlank()) {
            batch.setStatus("FAILED");
            batch.setSummary("导入失败：" + fatalMessage);
        } else {
            batch.setStatus(failedCount > 0 ? "DONE_WITH_ERRORS" : "DONE");
            batch.setSummary("导入完成：成功 " + successCount + " 行，失败 " + failedCount + " 行");
        }
        erpSupplierImportBatchMapper.updateById(batch);
    }

    @Override
    public List<ErpSupplierImportBatchSummary> listImportBatches() {
        Long tenantId = TenantContext.requireTenantId();
        return erpSupplierImportBatchMapper.selectList(new QueryWrapper<ErpSupplierImportBatch>()
                .eq("tenant_id", tenantId)
                .orderByDesc("created_at", "id"))
            .stream()
            .map(item -> new ErpSupplierImportBatchSummary(
                item.getId(),
                item.getBatchNo(),
                item.getSourceName(),
                item.getImportMode(),
                item.getTotalCount(),
                item.getSuccessCount(),
                item.getFailedCount(),
                item.getUncategorizedCount(),
                item.getSettlementUnmatchedCount(),
                item.getPendingSubjectMergeCount(),
                item.getStatus(),
                item.getSummary(),
                item.getCreatedBy(),
                item.getCreatedAt()
            ))
            .toList();
    }

    @Override
    public List<ErpSupplierImportItemView> listImportBatchItems(Long batchId) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSupplierImportBatch batch = erpSupplierImportBatchMapper.selectOne(new QueryWrapper<ErpSupplierImportBatch>()
            .eq("tenant_id", tenantId)
            .eq("id", batchId));
        if (batch == null) {
            throw new IllegalArgumentException("导入批次不存在");
        }
        return erpSupplierImportItemMapper.selectList(new QueryWrapper<ErpSupplierImportItem>()
                .eq("tenant_id", tenantId)
                .eq("batch_id", batchId)
                .orderByAsc("row_no", "id"))
            .stream()
            .map(item -> new ErpSupplierImportItemView(
                item.getId(),
                item.getRowNo(),
                item.getSourceCode(),
                item.getSourceName(),
                item.getMatchedSupplierId(),
                item.getSupplierTypeName(),
                item.getSettlementMethodName(),
                item.getEnterpriseMatch(),
                item.getPriceLevel(),
                item.getStatus(),
                item.getErrorField(),
                item.getErrorMessage(),
                item.getSuggestion(),
                item.getWarningMessage(),
                item.getMatchedStrategy(),
                item.getCreatedAt()
            ))
            .toList();
    }

    private void ensureSupplierNotReferenced(Long tenantId, Long supplierId) {
        if (erpPurchaseOrderMapper.selectCount(new QueryWrapper<ErpPurchaseOrder>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)) > 0) {
            throw new IllegalArgumentException("供应商已被采购单引用，不能删除");
        }
        if (erpPurchaseReturnMapper.selectCount(new QueryWrapper<ErpPurchaseReturn>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)) > 0) {
            throw new IllegalArgumentException("供应商已被采购退货单引用，不能删除");
        }
        if (erpPaymentMapper.selectCount(new QueryWrapper<ErpPayment>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)) > 0) {
            throw new IllegalArgumentException("供应商已被付款单引用，不能删除");
        }
        if (erpAccountsPayableMapper.selectCount(new QueryWrapper<ErpAccountsPayable>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)) > 0) {
            throw new IllegalArgumentException("供应商已被应付单引用，不能删除");
        }
    }

    private void syncCounterpartySubjectLink(Long tenantId, Long supplierId, Long oldSubjectId, Long newSubjectId) {
        if (java.util.Objects.equals(oldSubjectId, newSubjectId)) {
            return;
        }
        ensureSupplierCanRebind(tenantId, supplierId, oldSubjectId, newSubjectId);

        ErpCounterpartySubjectLink existing = erpCounterpartySubjectLinkMapper.selectOne(new QueryWrapper<ErpCounterpartySubjectLink>()
            .eq("tenant_id", tenantId)
            .eq("target_type", "SUPPLIER")
            .eq("target_id", supplierId));
        if (existing != null) {
            erpCounterpartySubjectLinkMapper.deleteById(existing.getId());
        }
        if (newSubjectId != null) {
            ErpCounterpartySubjectLink link = new ErpCounterpartySubjectLink();
            link.setTenantId(tenantId);
            link.setSubjectId(newSubjectId);
            link.setTargetType("SUPPLIER");
            link.setTargetId(supplierId);
            link.setRoleType("SUPPLIER");
            link.setPrimary(false);
            link.setCreatedAt(Instant.now());
            link.setUpdatedAt(Instant.now());
            erpCounterpartySubjectLinkMapper.insert(link);
        }
    }

    private void ensureSupplierCanRebind(Long tenantId, Long supplierId, Long oldSubjectId, Long newSubjectId) {
        if (oldSubjectId == null && newSubjectId == null) {
            return;
        }
        ErpCounterpartyUnbindCheck check = buildSupplierRebindCheck(tenantId, supplierId);
        if (!check.allowed()) {
            throw new IllegalArgumentException(String.join("；", check.blockingReasons()));
        }
    }

    private ErpCounterpartyUnbindCheck buildSupplierRebindCheck(Long tenantId, Long supplierId) {
        List<String> reasons = new ArrayList<>();
        List<ErpCounterpartyPendingDoc> docs = new ArrayList<>();

        List<ErpPurchaseOrder> openPurchaseOrders = erpPurchaseOrderMapper.selectList(new QueryWrapper<ErpPurchaseOrder>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)
            .in("status", ErpCounterpartyGuardRules.BLOCKING_DOCUMENT_STATUSES));
        if (!openPurchaseOrders.isEmpty()) {
            reasons.add("存在未完成采购单");
            docs.addAll(openPurchaseOrders.stream()
                .map(item -> new ErpCounterpartyPendingDoc("PURCHASE_ORDER", item.getId(), item.getOrderNo(), item.getStatus(), resolvePurchaseOrderRouteKey(item.getStatus())))
                .toList());
        }

        List<ErpPurchaseReturn> openPurchaseReturns = erpPurchaseReturnMapper.selectList(new QueryWrapper<ErpPurchaseReturn>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)
            .in("status", ErpCounterpartyGuardRules.BLOCKING_DOCUMENT_STATUSES));
        if (!openPurchaseReturns.isEmpty()) {
            reasons.add("存在未完成采购退货单");
            docs.addAll(openPurchaseReturns.stream()
                .map(item -> new ErpCounterpartyPendingDoc("PURCHASE_RETURN", item.getId(), item.getOrderNo(), item.getStatus(), resolvePurchaseReturnRouteKey(item.getStatus())))
                .toList());
        }

        List<ErpPayment> openPayments = erpPaymentMapper.selectList(new QueryWrapper<ErpPayment>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)
            .in("status", ErpCounterpartyGuardRules.BLOCKING_DOCUMENT_STATUSES));
        if (!openPayments.isEmpty()) {
            reasons.add("存在未完成付款单");
            docs.addAll(openPayments.stream()
                .map(item -> new ErpCounterpartyPendingDoc("PAYMENT", item.getId(), item.getPaymentNo(), item.getStatus(), "erp-payments-detail"))
                .toList());
        }

        List<ErpAccountsPayable> payables = erpAccountsPayableMapper.selectList(new QueryWrapper<ErpAccountsPayable>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)
            .ne("status", ErpCounterpartyGuardRules.RED_FLUSHED_STATUS));
        java.math.BigDecimal totalPayable = payables.stream()
            .map(ErpAccountsPayable::getUnpaidAmount)
            .filter(value -> value != null && value.compareTo(java.math.BigDecimal.ZERO) != 0)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        if (totalPayable.compareTo(java.math.BigDecimal.ZERO) != 0) {
            reasons.add("存在未完成应付，未付金额合计：" + totalPayable.stripTrailingZeros().toPlainString());
        }

        return new ErpCounterpartyUnbindCheck(reasons.isEmpty(), reasons, docs);
    }

    private String resolvePurchaseOrderRouteKey(String status) {
        return "DRAFT".equals(status) ? "erp-purchase-draft-edit" : "erp-purchase-approved-detail";
    }

    private String resolvePurchaseReturnRouteKey(String status) {
        return "DRAFT".equals(status) ? "erp-purchase-returns-draft-edit" : "erp-purchase-returns-approved-detail";
    }

    private QueryWrapper<ErpSupplier> baseWrapper(Long tenantId, String keyword, String contact, String phone, String status) {
        QueryWrapper<ErpSupplier> wrapper = new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", tenantId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword)
                .or()
                .like("short_name", keyword)
                .or()
                .like("contact", keyword)
                .or()
                .like("phone", keyword)
                .or()
                .like("mobile", keyword)
                .or()
                .like("email", keyword)
                .or()
                .like("tax_no", keyword)
                .or()
                .like("bank_account", keyword)
                .or()
                .like("address", keyword)
                .or()
                .apply("CAST(contacts AS TEXT) LIKE {0}", wrapLike(keyword)));
        }
        if (contact != null && !contact.isBlank()) {
            wrapper.and(q -> q.like("contact", contact)
                .or()
                .apply("CAST(contacts AS TEXT) LIKE {0}", wrapLike(contact)));
        }
        if (phone != null && !phone.isBlank()) {
            wrapper.and(q -> q.like("phone", phone)
                .or()
                .like("mobile", phone)
                .or()
                .apply("CAST(contacts AS TEXT) LIKE {0}", wrapLike(phone)));
        }
        if ("enabled".equalsIgnoreCase(status)) {
            wrapper.eq("is_enabled", true).eq("is_blacklisted", false);
        } else if ("disabled".equalsIgnoreCase(status)) {
            wrapper.eq("is_enabled", false).eq("is_blacklisted", false);
        } else if ("blacklisted".equalsIgnoreCase(status)) {
            wrapper.eq("is_blacklisted", true);
        }
        return wrapper;
    }

    private String wrapLike(String value) {
        return "%" + value.trim() + "%";
    }

    private void applyStatus(ErpSupplier supplier, Boolean enabled, Boolean blacklisted) {
        boolean finalBlacklisted = blacklisted != null && blacklisted;
        boolean finalEnabled = enabled == null || enabled;
        if (finalBlacklisted) {
            finalEnabled = false;
        }
        supplier.setBlacklisted(finalBlacklisted);
        supplier.setEnabled(finalEnabled);
    }

    private void enrichRecentTransactionAt(Long tenantId, List<ErpSupplier> suppliers) {
        if (suppliers == null || suppliers.isEmpty()) {
            return;
        }
        List<Long> supplierIds = suppliers.stream()
            .map(ErpSupplier::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (supplierIds.isEmpty()) {
            return;
        }
        Map<Long, Instant> recentTransactionMap = new HashMap<>();
        for (ErpSupplier row : erpSupplierMapper.findRecentTransactionRows(tenantId, supplierIds)) {
            if (row.getId() != null) {
                recentTransactionMap.put(row.getId(), row.getRecentTransactionAt());
            }
        }
        for (ErpSupplier supplier : suppliers) {
            if (supplier.getId() == null) {
                continue;
            }
            supplier.setRecentTransactionAt(recentTransactionMap.get(supplier.getId()));
        }
    }

    private void applyRequest(ErpSupplier supplier, ErpSupplierCreateRequest request) {
        JsonNode contacts = parseContacts(request.contacts());
        ContactSnapshot primaryContact = resolvePrimaryContact(contacts);
        supplier.setCode(request.code());
        supplier.setName(request.name());
        supplier.setShortName(request.shortName());
        supplier.setSupplierTypeId(request.supplierTypeId());
        supplier.setContact(resolveSupplierField(request.contact(), primaryContact.name()));
        supplier.setPhone(resolveSupplierField(request.phone(), primaryContact.phone()));
        supplier.setMobile(resolveSupplierField(request.mobile(), primaryContact.mobile()));
        supplier.setEmail(resolveSupplierField(request.email(), primaryContact.email()));
        supplier.setAddress(request.address());
        supplier.setRegion(request.region());
        supplier.setWechat(resolveSupplierField(request.wechat(), primaryContact.wechat()));
        supplier.setPurchaser(request.purchaser());
        supplier.setContactInfo(resolveSupplierField(request.contactInfo(), summarizeContacts(contacts)));
        supplier.setTaxNo(request.taxNo());
        supplier.setBankName(request.bankName());
        supplier.setBankAccount(request.bankAccount());
        supplier.setDefaultSettlementMethodCode(request.defaultSettlementMethodCode());
        supplier.setDefaultPaymentMethodCode(request.defaultPaymentMethodCode());
        supplier.setContacts(contacts);
        supplier.setSourceCreatedAt(parseSourceCreatedAt(request.sourceCreatedAt()));
        supplier.setSourceCreatedBy(request.sourceCreatedBy());
        supplier.setBusinessScope(normalizeBusinessScope(request.businessScope()));
        supplier.setCounterpartySubjectId(request.counterpartySubjectId());
        supplier.setRemark(request.remark());
    }

    private void applyRequest(ErpSupplier supplier, ErpSupplierUpdateRequest request) {
        JsonNode contacts = parseContacts(request.contacts());
        ContactSnapshot primaryContact = resolvePrimaryContact(contacts);
        supplier.setCode(request.code());
        supplier.setName(request.name());
        supplier.setShortName(request.shortName());
        supplier.setSupplierTypeId(request.supplierTypeId());
        supplier.setContact(resolveSupplierField(request.contact(), primaryContact.name()));
        supplier.setPhone(resolveSupplierField(request.phone(), primaryContact.phone()));
        supplier.setMobile(resolveSupplierField(request.mobile(), primaryContact.mobile()));
        supplier.setEmail(resolveSupplierField(request.email(), primaryContact.email()));
        supplier.setAddress(request.address());
        supplier.setRegion(request.region());
        supplier.setWechat(resolveSupplierField(request.wechat(), primaryContact.wechat()));
        supplier.setPurchaser(request.purchaser());
        supplier.setContactInfo(resolveSupplierField(request.contactInfo(), summarizeContacts(contacts)));
        supplier.setTaxNo(request.taxNo());
        supplier.setBankName(request.bankName());
        supplier.setBankAccount(request.bankAccount());
        supplier.setDefaultSettlementMethodCode(request.defaultSettlementMethodCode());
        supplier.setDefaultPaymentMethodCode(request.defaultPaymentMethodCode());
        supplier.setContacts(contacts);
        supplier.setSourceCreatedAt(parseSourceCreatedAt(request.sourceCreatedAt()));
        supplier.setSourceCreatedBy(request.sourceCreatedBy());
        supplier.setBusinessScope(normalizeBusinessScope(request.businessScope()));
        supplier.setCounterpartySubjectId(request.counterpartySubjectId());
        supplier.setRemark(request.remark());
    }

    private void applyDefaultMethodsIfMissing(ErpSupplier supplier, Long tenantId) {
        if (supplier.getDefaultSettlementMethodCode() == null || supplier.getDefaultSettlementMethodCode().isBlank()) {
            var defaultSettlement = erpSettlementMethodMapper.findDefault(tenantId);
            if (defaultSettlement != null) {
                supplier.setDefaultSettlementMethodCode(defaultSettlement.getCode());
            }
        }
        if (supplier.getDefaultPaymentMethodCode() == null || supplier.getDefaultPaymentMethodCode().isBlank()) {
            var defaultPayment = erpPaymentMethodMapper.findDefault(tenantId);
            if (defaultPayment != null) {
                supplier.setDefaultPaymentMethodCode(defaultPayment.getCode());
            }
        }
    }

    private void applyDefaultMethodsIfMissing(ErpSupplier supplier, SupplierImportReferenceData referenceData) {
        if (supplier.getDefaultSettlementMethodCode() == null || supplier.getDefaultSettlementMethodCode().isBlank()) {
            if (referenceData.defaultSettlementMethodCode() != null) {
                supplier.setDefaultSettlementMethodCode(referenceData.defaultSettlementMethodCode());
            }
        }
        if (supplier.getDefaultPaymentMethodCode() == null || supplier.getDefaultPaymentMethodCode().isBlank()) {
            if (referenceData.defaultPaymentMethodCode() != null) {
                supplier.setDefaultPaymentMethodCode(referenceData.defaultPaymentMethodCode());
            }
        }
    }

    private JsonNode parseContacts(String rawContacts) {
        if (rawContacts == null || rawContacts.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(rawContacts);
        } catch (Exception ex) {
            throw new IllegalArgumentException("联系人列表格式不正确", ex);
        }
    }

    private ContactSnapshot resolvePrimaryContact(JsonNode contacts) {
        if (contacts == null || !contacts.isArray() || contacts.isEmpty()) {
            return ContactSnapshot.empty();
        }
        JsonNode primary = null;
        for (JsonNode contact : contacts) {
            if (contact != null && contact.path("isPrimary").asBoolean(false)) {
                primary = contact;
                break;
            }
        }
        if (primary == null) {
            primary = contacts.get(0);
        }
        if (primary == null || primary.isNull()) {
            return ContactSnapshot.empty();
        }
        return new ContactSnapshot(
            trimToNull(primary.path("name").asText(null)),
            trimToNull(primary.path("phone").asText(null)),
            trimToNull(primary.path("mobile").asText(null)),
            trimToNull(primary.path("wechat").asText(null)),
            trimToNull(primary.path("email").asText(null)),
            trimToNull(primary.path("remark").asText(null))
        );
    }

    private ContactSnapshot resolveImportedPrimaryFields(JsonNode contacts) {
        ContactSnapshot primaryContact = resolvePrimaryContact(contacts);
        if (contacts == null || !contacts.isArray() || contacts.isEmpty()) {
            return primaryContact;
        }
        String phone = primaryContact.phone();
        String mobile = primaryContact.mobile();
        String wechat = primaryContact.wechat();
        String email = primaryContact.email();
        String remark = primaryContact.remark();
        for (JsonNode contact : contacts) {
            if (contact == null || contact.isNull()) {
                continue;
            }
            if (phone == null) {
                phone = trimToNull(contact.path("phone").asText(null));
            }
            if (mobile == null) {
                mobile = trimToNull(contact.path("mobile").asText(null));
            }
            if (wechat == null) {
                wechat = trimToNull(contact.path("wechat").asText(null));
            }
            if (email == null) {
                email = trimToNull(contact.path("email").asText(null));
            }
            if (remark == null) {
                remark = trimToNull(contact.path("remark").asText(null));
            }
        }
        return new ContactSnapshot(primaryContact.name(), phone, mobile, wechat, email, remark);
    }

    private String resolveSupplierField(String explicitValue, String fallbackValue) {
        String explicit = trimToNull(explicitValue);
        return explicit != null ? explicit : trimToNull(fallbackValue);
    }

    private String summarizeContacts(JsonNode contacts) {
        if (contacts == null || !contacts.isArray() || contacts.isEmpty()) {
            return null;
        }
        List<String> summaries = new ArrayList<>();
        for (JsonNode contact : contacts) {
            if (contact == null || contact.isNull()) {
                continue;
            }
            List<String> parts = new ArrayList<>();
            String name = trimToNull(contact.path("name").asText(null));
            String phone = trimToNull(contact.path("phone").asText(null));
            String mobile = trimToNull(contact.path("mobile").asText(null));
            String wechat = trimToNull(contact.path("wechat").asText(null));
            String email = trimToNull(contact.path("email").asText(null));
            String remark = trimToNull(contact.path("remark").asText(null));
            if (name != null) {
                parts.add(name);
            }
            if (phone != null) {
                parts.add("电话:" + phone);
            }
            if (mobile != null) {
                parts.add("手机:" + mobile);
            }
            if (wechat != null) {
                parts.add("微信:" + wechat);
            }
            if (email != null) {
                parts.add("邮箱:" + email);
            }
            if (remark != null) {
                parts.add("备注:" + remark);
            }
            if (!parts.isEmpty()) {
                summaries.add(String.join(" / ", parts));
            }
        }
        return summaries.isEmpty() ? null : String.join("；", summaries);
    }

    private Instant parseSourceCreatedAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            if (trimmed.matches("^\\d+$")) {
                return Instant.ofEpochMilli(Long.parseLong(trimmed));
            }
            if (trimmed.contains("T")) {
                return Instant.parse(trimmed);
            }
            if (trimmed.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(trimmed, formatter)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        } catch (Exception ex) {
            throw new IllegalArgumentException("来源创建时间格式不正确", ex);
        }
    }

    private String normalizeBusinessScope(String businessScope) {
        if (businessScope == null || businessScope.isBlank()) {
            return DEFAULT_BUSINESS_SCOPE;
        }
        return businessScope.trim();
    }

    private String normalizeBusinessScopeByImport(String businessScope) {
        String normalized = trimToNull(businessScope);
        if (normalized == null || "供应商".equals(normalized)) {
            return "SUPPLIER";
        }
        if ("既是客户又是供应商".equals(normalized)) {
            return "CUSTOMER_SUPPLIER";
        }
        throw new IllegalArgumentException("往来类别值不支持: " + normalized);
    }

    private String resolveSettlementMethodCode(SupplierImportReferenceData referenceData, String name) {
        String normalized = trimToNull(name);
        if (normalized == null) {
            return null;
        }
        return referenceData.settlementCodeByName().get(normalized);
    }

    private Long resolveSupplierTypeId(SupplierImportReferenceData referenceData, String name) {
        String normalized = trimToNull(name);
        if (normalized == null) {
            return null;
        }
        return referenceData.supplierTypeIdByName().get(normalized);
    }

    private ErpSupplierType resolveUncategorizedSupplierType(SupplierImportReferenceData referenceData) {
        return referenceData.uncategorizedSupplierType();
    }

    private ImportUpsertOutcome upsertImportedSupplier(SupplierImportReferenceData referenceData,
                                                       Long tenantId,
                                                       String code,
                                                       String name,
                                                       Map<String, String> rowMap,
                                                       String settlementCode,
                                                       Long supplierTypeId,
                                                       String businessScope) {
        ErpSupplier supplier = referenceData.existingSuppliersByCode().get(code);
        boolean created = supplier == null;
        List<String> warnings = new ArrayList<>();
        if (supplier == null) {
            supplier = new ErpSupplier();
            supplier.setTenantId(tenantId);
            supplier.setCode(code);
            supplier.setCreatedAt(Instant.now());
            supplier.setEnabled(true);
            supplier.setBlacklisted(false);
        }
        if (!created && trimToNull(name) != null && !Objects.equals(supplier.getName(), name)) {
            warnings.add("名称已更新");
        }
        JsonNode contacts = buildImportedContacts(rowMap);
        ContactSnapshot primaryContact = resolvePrimaryContact(contacts);
        ContactSnapshot importedContactFields = resolveImportedPrimaryFields(contacts);
        String contactInfo = resolveImportedContactInfo(rowMap, contacts);
        if (!created && contactInfo != null && !Objects.equals(trimToNull(supplier.getContactInfo()), contactInfo)) {
            warnings.add("联系方式已更新");
        }
        supplier.setName(name);
        supplier.setSupplierTypeId(supplierTypeId);
        supplier.setRegion(trimToNull(rowMap.get("区域")));
        supplier.setWechat(trimToNull(rowMap.get("微信客服")));
        supplier.setAddress(trimToNull(rowMap.get("地址")));
        supplier.setRemark(trimToNull(rowMap.get("备注")));
        supplier.setSourceCreatedBy(trimToNull(rowMap.get("创建人")));
        supplier.setSourceCreatedAt(parseSourceCreatedAt(trimToNull(rowMap.get("创建时间"))));
        supplier.setPurchaser(trimToNull(rowMap.get("采购员")));
        supplier.setContactInfo(contactInfo);
        supplier.setContact(resolveSupplierField(trimToNull(rowMap.get("联系人")), primaryContact.name()));
        supplier.setBusinessScope(businessScope);
        supplier.setDefaultSettlementMethodCode(settlementCode);
        supplier.setPhone(importedContactFields.phone());
        supplier.setMobile(importedContactFields.mobile());
        supplier.setEmail(importedContactFields.email());
        supplier.setWechat(resolveSupplierField(trimToNull(rowMap.get("微信客服")), primaryContact.wechat()));
        supplier.setContacts(contacts);
        supplier.setUpdatedAt(Instant.now());
        applyDefaultMethodsIfMissing(supplier, referenceData);
        if (supplier.getId() == null) {
            erpSupplierMapper.insert(supplier);
            referenceData.existingSuppliersByCode().put(code, supplier);
        } else {
            erpSupplierMapper.updateById(supplier);
        }
        return new ImportUpsertOutcome(supplier, created, warnings.isEmpty() ? null : String.join("；", warnings));
    }

    private SupplierImportReferenceData preloadImportReferenceData(Long tenantId, List<Map<String, String>> rows) {
        Map<String, String> settlementCodeByName = erpSettlementMethodMapper.selectList(
                new QueryWrapper<com.example.wms.entity.erp.ErpSettlementMethod>()
                    .eq("tenant_id", tenantId)
                    .isNull("deleted_at"))
            .stream()
            .map(item -> Map.entry(trimToNull(item.getName()), trimToNull(item.getCode())))
            .filter(entry -> entry.getKey() != null && entry.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));

        List<ErpSupplierType> supplierTypes = erpSupplierTypeMapper.selectList(new QueryWrapper<ErpSupplierType>()
            .eq("tenant_id", tenantId)
            .isNull("deleted_at"));
        Map<String, Long> supplierTypeIdByName = supplierTypes.stream()
            .map(item -> Map.entry(trimToNull(item.getName()), item.getId()))
            .filter(entry -> entry.getKey() != null && entry.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
        ErpSupplierType uncategorizedSupplierType = supplierTypes.stream()
            .filter(item -> ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE.equals(item.getCode()))
            .findFirst()
            .orElseGet(() -> erpSupplierTypeMapper.findByCode(tenantId, ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE));

        Collection<String> codes = rows.stream()
            .map(row -> trimToNull(row.get("编码")))
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, ErpSupplier> existingSuppliersByCode = new HashMap<>();
        if (!codes.isEmpty()) {
            erpSupplierMapper.selectList(new QueryWrapper<ErpSupplier>()
                    .eq("tenant_id", tenantId)
                    .in("code", codes)
                    .isNull("deleted_at"))
                .forEach(item -> {
                    String supplierCode = trimToNull(item.getCode());
                    if (supplierCode != null) {
                        existingSuppliersByCode.put(supplierCode, item);
                    }
                });
        }

        String defaultSettlementMethodCode = null;
        var defaultSettlement = erpSettlementMethodMapper.findDefault(tenantId);
        if (defaultSettlement != null) {
            defaultSettlementMethodCode = trimToNull(defaultSettlement.getCode());
        }
        String defaultPaymentMethodCode = null;
        var defaultPayment = erpPaymentMethodMapper.findDefault(tenantId);
        if (defaultPayment != null) {
            defaultPaymentMethodCode = trimToNull(defaultPayment.getCode());
        }

        return new SupplierImportReferenceData(
            settlementCodeByName,
            supplierTypeIdByName,
            uncategorizedSupplierType,
            existingSuppliersByCode,
            defaultSettlementMethodCode,
            defaultPaymentMethodCode
        );
    }

    private JsonNode valueToTree(Object value) {
        return objectMapper.valueToTree(value);
    }

    private JsonNode buildImportedContacts(Map<String, String> rowMap) {
        ArrayNode contacts = objectMapper.createArrayNode();
        java.util.regex.Pattern indexedFieldPattern = java.util.regex.Pattern.compile("^(联系人|电话|手机|微信|邮箱)(\\d+)(?:-(\\d+))?$");
        Map<Integer, ImportContactBucket> buckets = new java.util.TreeMap<>();
        for (Map.Entry<String, String> entry : rowMap.entrySet()) {
            String header = trimToNull(entry.getKey());
            String value = trimToNull(entry.getValue());
            if (header == null || value == null) {
                continue;
            }
            java.util.regex.Matcher matcher = indexedFieldPattern.matcher(header);
            if (!matcher.matches()) {
                continue;
            }
            String fieldType = matcher.group(1);
            int contactIndex = Integer.parseInt(matcher.group(2));
            int slotIndex = matcher.group(3) == null ? 1 : Integer.parseInt(matcher.group(3));
            ImportContactBucket bucket = buckets.computeIfAbsent(contactIndex, ignored -> new ImportContactBucket());
            ImportContactVariant variant = bucket.variant(slotIndex);
            switch (fieldType) {
                case "联系人" -> variant.name = value;
                case "电话" -> variant.phone = value;
                case "手机" -> variant.mobile = value;
                case "微信" -> variant.wechat = value;
                case "邮箱" -> variant.email = value;
                default -> {
                }
            }
        }
        for (Map.Entry<Integer, ImportContactBucket> bucketEntry : buckets.entrySet()) {
            int contactIndex = bucketEntry.getKey();
            ImportContactBucket bucket = bucketEntry.getValue();
            String fallbackName = trimToNull(rowMap.get("联系人" + contactIndex));
            for (Map.Entry<Integer, ImportContactVariant> variantEntry : bucket.variants.entrySet()) {
                int slotIndex = variantEntry.getKey();
                ImportContactVariant variant = variantEntry.getValue();
                String resolvedName = trimToNull(variant.name);
                if (resolvedName == null) {
                    resolvedName = fallbackName;
                }
                if (resolvedName == null && slotIndex == 1) {
                    resolvedName = trimToNull(rowMap.get("联系人"));
                }
                if (resolvedName == null && variant.isEmpty()) {
                    continue;
                }
                contacts.add(importContactNode(
                    resolvedName,
                    variant.phone,
                    variant.mobile,
                    variant.wechat,
                    variant.email,
                    bucketEntry.getKey() == 1 && slotIndex == 1
                ));
            }
        }
        if (!contacts.isEmpty()) {
            return contacts;
        }
        String legacyContact = trimToNull(rowMap.get("联系人"));
        String legacyContactInfo = trimToNull(rowMap.get("联系方式"));
        if (legacyContact == null && legacyContactInfo == null) {
            return null;
        }
        List<String> legacyValues = splitImportedContactValues(legacyContactInfo);
        if (!legacyValues.isEmpty()) {
            for (int index = 0; index < legacyValues.size(); index++) {
                String contactValue = legacyValues.get(index);
                contacts.add(importContactNode(
                    legacyContact,
                    isMobileLike(contactValue) ? null : contactValue,
                    isMobileLike(contactValue) ? contactValue : null,
                    null,
                    null,
                    index == 0
                ));
            }
            return contacts;
        }
        contacts.add(importContactNode(
            legacyContact,
            extractFirstPhone(legacyContactInfo),
            extractFirstMobile(legacyContactInfo),
            null,
            null,
            true
        ));
        return contacts;
    }

    private ObjectNode importContactNode(String name,
                                         String phone,
                                         String mobile,
                                         String wechat,
                                         String email,
                                         boolean isPrimary) {
        ObjectNode node = objectMapper.createObjectNode();
        if (trimToNull(name) != null) {
            node.put("name", trimToNull(name));
        }
        if (trimToNull(phone) != null) {
            node.put("phone", trimToNull(phone));
        }
        if (trimToNull(mobile) != null) {
            node.put("mobile", trimToNull(mobile));
        }
        if (trimToNull(wechat) != null) {
            node.put("wechat", trimToNull(wechat));
        }
        if (trimToNull(email) != null) {
            node.put("email", trimToNull(email));
        }
        node.put("isPrimary", isPrimary);
        return node;
    }

    private String resolveImportedContactInfo(Map<String, String> rowMap, JsonNode contacts) {
        String summary = summarizeContacts(contacts);
        String legacy = trimToNull(rowMap.get("联系方式"));
        return summary != null ? summary : legacy;
    }

    private List<String> splitImportedContactValues(String rawContactInfo) {
        String normalized = trimToNull(rawContactInfo);
        if (normalized == null) {
            return List.of();
        }
        return java.util.regex.Pattern.compile("[/／,，;；\\n]+")
            .splitAsStream(normalized)
            .map(this::trimToNull)
            .filter(Objects::nonNull)
            .toList();
    }

    private String resolveFirstContactField(JsonNode contacts, String fieldName) {
        if (contacts == null || !contacts.isArray()) {
            return null;
        }
        for (JsonNode contact : contacts) {
            String value = trimToNull(contact.path(fieldName).asText(null));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private JsonNode textNode(String text) {
        return objectMapper.getNodeFactory().textNode(text);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String extractFirstMobile(String text) {
        if (text == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("1\\d{10}").matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private boolean isMobileLike(String text) {
        return text != null && text.matches("^1\\d{10}$");
    }

    private String extractFirstPhone(String text) {
        if (text == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(0\\d{2,3}-?\\d{7,8}|\\d{7,8})").matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private String mergeWarnings(String first, String second) {
        String left = trimToNull(first);
        String right = trimToNull(second);
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left + "；" + right;
    }

    private byte[] readImportBytes(MultipartFile file) {
        try {
            return file == null ? null : file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取导入文件失败", ex);
        }
    }

    private ExcelImportSheet parseImportSheet(String uploadedFileName, byte[] fileBytes) {
        return excelImportParser.parse(uploadedFileName, fileBytes);
    }

    private String resolveCurrentUsername() {
        return "system";
    }

    private String generateSupplierCode(Long tenantId) {
        String prefix = readConfig("erp.supplier.code.prefix", "SU");
        String dateFormat = readConfig("erp.supplier.code.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.supplier.code.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, SUPPLIER_CODE_TYPE, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, SUPPLIER_CODE_TYPE, dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private String readConfig(String key, String fallback) {
        SystemConfig config = systemConfigMapper.findByKey(TenantContext.requireTenantId(), key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return fallback;
        }
        return config.getConfigValue().trim();
    }

    private int readIntConfig(String key, int fallback) {
        String value = readConfig(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private record SupplierImportReferenceData(
        Map<String, String> settlementCodeByName,
        Map<String, Long> supplierTypeIdByName,
        ErpSupplierType uncategorizedSupplierType,
        Map<String, ErpSupplier> existingSuppliersByCode,
        String defaultSettlementMethodCode,
        String defaultPaymentMethodCode
    ) {
    }

    private record ImportUpsertOutcome(ErpSupplier supplier, boolean created, String warningMessage) {
    }

    private record ImportBatchCounters(
        int successCount,
        int failedCount,
        int uncategorizedCount,
        int settlementUnmatchedCount,
        int pendingSubjectMergeCount
    ) {
        private static ImportBatchCounters empty() {
            return new ImportBatchCounters(0, 0, 0, 0, 0);
        }

        private ImportBatchCounters plus(ImportBatchCounters other) {
            return new ImportBatchCounters(
                successCount + other.successCount,
                failedCount + other.failedCount,
                uncategorizedCount + other.uncategorizedCount,
                settlementUnmatchedCount + other.settlementUnmatchedCount,
                pendingSubjectMergeCount + other.pendingSubjectMergeCount
            );
        }
    }

    private record ContactSnapshot(String name, String phone, String mobile, String wechat, String email, String remark) {
        private static ContactSnapshot empty() {
            return new ContactSnapshot(null, null, null, null, null, null);
        }
    }

    private static final class ImportContactBucket {
        private final Map<Integer, ImportContactVariant> variants = new java.util.TreeMap<>();

        private ImportContactVariant variant(int slotIndex) {
            return variants.computeIfAbsent(slotIndex, ignored -> new ImportContactVariant());
        }
    }

    private static final class ImportContactVariant {
        private String name;
        private String phone;
        private String mobile;
        private String wechat;
        private String email;

        private boolean isEmpty() {
            return name == null && phone == null && mobile == null && wechat == null && email == null;
        }
    }
}
