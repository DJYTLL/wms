package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCustomerImportBatchSummary;
import com.example.wms.dto.erp.ErpCustomerImportItemView;
import com.example.wms.dto.erp.ErpCustomerImportResult;
import com.example.wms.dto.erp.ErpCounterpartyPendingDoc;
import com.example.wms.dto.erp.ErpCounterpartyUnbindCheck;
import com.example.wms.dto.erp.ErpCustomerCreateRequest;
import com.example.wms.dto.erp.ErpCustomerUpdateRequest;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpAccountsReceivable;
import com.example.wms.entity.erp.ErpCounterpartySubjectLink;
import com.example.wms.entity.erp.ErpCounterpartySubject;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpCustomerCategory;
import com.example.wms.entity.erp.ErpCustomerImportBatch;
import com.example.wms.entity.erp.ErpCustomerImportItem;
import com.example.wms.entity.erp.ErpReceipt;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.entity.erp.ErpSaleReturn;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpCounterpartySubjectLinkMapper;
import com.example.wms.mapper.erp.ErpCounterpartySubjectMapper;
import com.example.wms.mapper.erp.ErpCustomerCategoryMapper;
import com.example.wms.mapper.erp.ErpCustomerImportBatchMapper;
import com.example.wms.mapper.erp.ErpCustomerImportItemMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpDeliveryMethodMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpReceiptMethodMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.service.erp.ErpCustomerService;
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
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

// 客户服务实现（ERP进销存）
@Service
public class ErpCustomerServiceImpl implements ErpCustomerService {
    private static final String CUSTOMER_CODE_TYPE = "CUSTOMER";
    private static final int IMPORT_CHUNK_SIZE = 100;

    private final ErpCustomerMapper erpCustomerMapper;
    private final ErpCustomerCategoryMapper erpCustomerCategoryMapper;
    private final ErpSettlementMethodMapper erpSettlementMethodMapper;
    private final ErpReceiptMethodMapper erpReceiptMethodMapper;
    private final ErpDeliveryMethodMapper erpDeliveryMethodMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ErpSaleOrderMapper erpSaleOrderMapper;
    private final ErpSaleReturnMapper erpSaleReturnMapper;
    private final ErpReceiptMapper erpReceiptMapper;
    private final ErpAccountsReceivableMapper erpAccountsReceivableMapper;
    private final ErpCounterpartySubjectMapper erpCounterpartySubjectMapper;
    private final ErpCounterpartySubjectLinkMapper erpCounterpartySubjectLinkMapper;
    private final ObjectMapper objectMapper;
    private final ErpCustomerImportBatchMapper erpCustomerImportBatchMapper;
    private final ErpCustomerImportItemMapper erpCustomerImportItemMapper;
    private final ExcelImportParser excelImportParser;
    private final Executor importExecutor;
    private final TransactionOperations transactionOperations;

    @Autowired
    public ErpCustomerServiceImpl(ErpCustomerMapper erpCustomerMapper,
                                  ErpCustomerCategoryMapper erpCustomerCategoryMapper,
                                  ErpSettlementMethodMapper erpSettlementMethodMapper,
                                  ErpReceiptMethodMapper erpReceiptMethodMapper,
                                  ErpDeliveryMethodMapper erpDeliveryMethodMapper,
                                  ErpOrderSequenceMapper erpOrderSequenceMapper,
                                  SystemConfigMapper systemConfigMapper,
                                  ErpSaleOrderMapper erpSaleOrderMapper,
                                  ErpSaleReturnMapper erpSaleReturnMapper,
                                  ErpReceiptMapper erpReceiptMapper,
                                  ErpAccountsReceivableMapper erpAccountsReceivableMapper,
                                  ErpCounterpartySubjectMapper erpCounterpartySubjectMapper,
                                  ErpCounterpartySubjectLinkMapper erpCounterpartySubjectLinkMapper,
                                  ObjectMapper objectMapper,
                                  ErpCustomerImportBatchMapper erpCustomerImportBatchMapper,
                                  ErpCustomerImportItemMapper erpCustomerImportItemMapper,
                                  ExcelImportParser excelImportParser,
                                  @Qualifier("erpImportTaskExecutor") Executor importExecutor,
                                  PlatformTransactionManager transactionManager) {
        this(
            erpCustomerMapper,
            erpCustomerCategoryMapper,
            erpSettlementMethodMapper,
            erpReceiptMethodMapper,
            erpDeliveryMethodMapper,
            erpOrderSequenceMapper,
            systemConfigMapper,
            erpSaleOrderMapper,
            erpSaleReturnMapper,
            erpReceiptMapper,
            erpAccountsReceivableMapper,
            erpCounterpartySubjectMapper,
            erpCounterpartySubjectLinkMapper,
            objectMapper,
            erpCustomerImportBatchMapper,
            erpCustomerImportItemMapper,
            excelImportParser,
            importExecutor,
            new TransactionTemplate(transactionManager)
        );
    }

    public ErpCustomerServiceImpl(ErpCustomerMapper erpCustomerMapper,
                                  ErpCustomerCategoryMapper erpCustomerCategoryMapper,
                                  ErpSettlementMethodMapper erpSettlementMethodMapper,
                                  ErpReceiptMethodMapper erpReceiptMethodMapper,
                                  ErpDeliveryMethodMapper erpDeliveryMethodMapper,
                                  ErpOrderSequenceMapper erpOrderSequenceMapper,
                                  SystemConfigMapper systemConfigMapper,
                                  ErpSaleOrderMapper erpSaleOrderMapper,
                                  ErpSaleReturnMapper erpSaleReturnMapper,
                                  ErpReceiptMapper erpReceiptMapper,
                                  ErpAccountsReceivableMapper erpAccountsReceivableMapper,
                                  ErpCounterpartySubjectMapper erpCounterpartySubjectMapper,
                                  ErpCounterpartySubjectLinkMapper erpCounterpartySubjectLinkMapper,
                                  ObjectMapper objectMapper,
                                  ErpCustomerImportBatchMapper erpCustomerImportBatchMapper,
                                  ErpCustomerImportItemMapper erpCustomerImportItemMapper,
                                  ExcelImportParser excelImportParser) {
        this(
            erpCustomerMapper,
            erpCustomerCategoryMapper,
            erpSettlementMethodMapper,
            erpReceiptMethodMapper,
            erpDeliveryMethodMapper,
            erpOrderSequenceMapper,
            systemConfigMapper,
            erpSaleOrderMapper,
            erpSaleReturnMapper,
            erpReceiptMapper,
            erpAccountsReceivableMapper,
            erpCounterpartySubjectMapper,
            erpCounterpartySubjectLinkMapper,
            objectMapper,
            erpCustomerImportBatchMapper,
            erpCustomerImportItemMapper,
            excelImportParser,
            Runnable::run,
            (TransactionOperations) null
        );
    }

    public ErpCustomerServiceImpl(ErpCustomerMapper erpCustomerMapper,
                                  ErpCustomerCategoryMapper erpCustomerCategoryMapper,
                                  ErpSettlementMethodMapper erpSettlementMethodMapper,
                                  ErpReceiptMethodMapper erpReceiptMethodMapper,
                                  ErpDeliveryMethodMapper erpDeliveryMethodMapper,
                                  ErpOrderSequenceMapper erpOrderSequenceMapper,
                                  SystemConfigMapper systemConfigMapper,
                                  ErpSaleOrderMapper erpSaleOrderMapper,
                                  ErpSaleReturnMapper erpSaleReturnMapper,
                                  ErpReceiptMapper erpReceiptMapper,
                                  ErpAccountsReceivableMapper erpAccountsReceivableMapper,
                                  ErpCounterpartySubjectMapper erpCounterpartySubjectMapper,
                                  ErpCounterpartySubjectLinkMapper erpCounterpartySubjectLinkMapper,
                                  ObjectMapper objectMapper,
                                  ErpCustomerImportBatchMapper erpCustomerImportBatchMapper,
                                  ErpCustomerImportItemMapper erpCustomerImportItemMapper,
                                  ExcelImportParser excelImportParser,
                                  Executor importExecutor,
                                  TransactionOperations transactionOperations) {
        this.erpCustomerMapper = erpCustomerMapper;
        this.erpCustomerCategoryMapper = erpCustomerCategoryMapper;
        this.erpSettlementMethodMapper = erpSettlementMethodMapper;
        this.erpReceiptMethodMapper = erpReceiptMethodMapper;
        this.erpDeliveryMethodMapper = erpDeliveryMethodMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.erpSaleOrderMapper = erpSaleOrderMapper;
        this.erpSaleReturnMapper = erpSaleReturnMapper;
        this.erpReceiptMapper = erpReceiptMapper;
        this.erpAccountsReceivableMapper = erpAccountsReceivableMapper;
        this.erpCounterpartySubjectMapper = erpCounterpartySubjectMapper;
        this.erpCounterpartySubjectLinkMapper = erpCounterpartySubjectLinkMapper;
        this.objectMapper = objectMapper;
        this.erpCustomerImportBatchMapper = erpCustomerImportBatchMapper;
        this.erpCustomerImportItemMapper = erpCustomerImportItemMapper;
        this.excelImportParser = excelImportParser;
        this.importExecutor = importExecutor == null ? Runnable::run : importExecutor;
        this.transactionOperations = transactionOperations;
    }

    public ErpCustomerServiceImpl(ErpCustomerMapper erpCustomerMapper,
                                  ErpCustomerCategoryMapper erpCustomerCategoryMapper,
                                  ErpSettlementMethodMapper erpSettlementMethodMapper,
                                  ErpReceiptMethodMapper erpReceiptMethodMapper,
                                  ErpDeliveryMethodMapper erpDeliveryMethodMapper,
                                  ErpOrderSequenceMapper erpOrderSequenceMapper,
                                  SystemConfigMapper systemConfigMapper,
                                  ErpSaleOrderMapper erpSaleOrderMapper,
                                  ErpSaleReturnMapper erpSaleReturnMapper,
                                  ErpReceiptMapper erpReceiptMapper,
                                  ErpAccountsReceivableMapper erpAccountsReceivableMapper,
                                  ErpCounterpartySubjectMapper erpCounterpartySubjectMapper,
                                  ErpCounterpartySubjectLinkMapper erpCounterpartySubjectLinkMapper,
                                  ObjectMapper objectMapper) {
        this(
            erpCustomerMapper,
            erpCustomerCategoryMapper,
            erpSettlementMethodMapper,
            erpReceiptMethodMapper,
            erpDeliveryMethodMapper,
            erpOrderSequenceMapper,
            systemConfigMapper,
            erpSaleOrderMapper,
            erpSaleReturnMapper,
            erpReceiptMapper,
            erpAccountsReceivableMapper,
            erpCounterpartySubjectMapper,
            erpCounterpartySubjectLinkMapper,
            objectMapper,
            null,
            null,
            new ExcelImportParser()
        );
    }

    @Override
    public List<ErpCustomer> listAll(String keyword, String contact, String phone, Boolean enabled, Long categoryId) {
        QueryWrapper<ErpCustomer> wrapper = baseWrapper(keyword, contact, phone, enabled, categoryId);
        wrapper.orderByAsc("id");
        return erpCustomerMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpCustomer> page(long page, long size, String keyword, String contact, String phone, Boolean enabled, Long categoryId) {
        Page<ErpCustomer> pageReq = Page.of(page, size);
        QueryWrapper<ErpCustomer> wrapper = baseWrapper(keyword, contact, phone, enabled, categoryId);
        wrapper.orderByAsc("id");
        Page<ErpCustomer> result = erpCustomerMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public List<ErpCustomer> searchOptions(String keyword, int size) {
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword == null) {
            return List.of();
        }
        return rankedSearchOptions(normalizedKeyword, size);
    }

    @Override
    public ErpCustomer getById(Long id) {
        ErpCustomer customer = erpCustomerMapper.selectOne(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        return customer;
    }

    @Override
    public ErpCounterpartyUnbindCheck checkRebind(Long id, Long targetSubjectId) {
        Long tenantId = TenantContext.requireTenantId();
        ErpCustomer customer = erpCustomerMapper.selectOne(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        if (java.util.Objects.equals(customer.getCounterpartySubjectId(), targetSubjectId)) {
            return new ErpCounterpartyUnbindCheck(true, List.of(), List.of());
        }
        return buildCustomerRebindCheck(tenantId, id);
    }

    @Override
    public String nextCode() {
        Long tenantId = TenantContext.requireTenantId();
        return generateCustomerCode(tenantId);
    }

    @Override
    @AuditLog(action = "ERP_CUSTOMER_CREATE", entityType = "erp_customer", entityId = "{result.id}", detail = "code={arg0.code}")
@Transactional
    public ErpCustomer create(ErpCustomerCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Long categoryId = request.categoryId();
        if (categoryId == null) {
            var defaultCategory = erpCustomerCategoryMapper.findDefault(tenantId);
            if (defaultCategory != null) {
                categoryId = defaultCategory.getId();
            } else {
                throw new IllegalArgumentException("请选择客户类别");
            }
        }
        ErpCustomer existing = erpCustomerMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("客户编码已存在");
        }
        ErpCustomer customer = new ErpCustomer();
        customer.setTenantId(tenantId);
        applyRequest(customer, request, categoryId);
        normalizeDefaultSettlementMethodCode(customer, tenantId);
        applyDefaultMethodsIfMissing(customer, tenantId);
        customer.setEnabled(request.enabled() == null || request.enabled());
        customer.setCreatedAt(Instant.now());
        customer.setUpdatedAt(Instant.now());
        erpCustomerMapper.insert(customer);
        return customer;
    }

    @Override
    @AuditLog(action = "ERP_CUSTOMER_UPDATE", entityType = "erp_customer", entityId = "{arg0}", detail = "code={arg1.code}")
@Transactional
    public ErpCustomer update(Long id, ErpCustomerUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        if (request.categoryId() == null) {
            throw new IllegalArgumentException("请选择客户类别");
        }
        ErpCustomer customer = erpCustomerMapper.selectOne(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        ErpCustomer existing = erpCustomerMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("客户编码已存在");
        }
        Long oldSubjectId = customer.getCounterpartySubjectId();
        applyRequest(customer, request, request.categoryId());
        normalizeDefaultSettlementMethodCode(customer, tenantId);
        syncCounterpartySubjectLink(tenantId, customer.getId(), oldSubjectId, customer.getCounterpartySubjectId());
        if (request.enabled() != null) {
            customer.setEnabled(request.enabled());
        }
        customer.setUpdatedAt(Instant.now());
        erpCustomerMapper.updateById(customer);
        return customer;
    }

    @Override
    @AuditLog(action = "ERP_CUSTOMER_DELETE", entityType = "erp_customer", entityId = "{arg0}")
@Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpCustomer customer = erpCustomerMapper.selectOne(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        ensureCustomerNotReferenced(tenantId, id);
        erpCustomerMapper.deleteById(id);
    }

    @Override
    @Transactional
    public ErpCustomerImportResult importCustomers(MultipartFile file, String sourceName) {
        Long tenantId = TenantContext.requireTenantId();
        String uploadedFileName = file == null ? null : trimToNull(file.getOriginalFilename());
        String resolvedSourceName = trimToNull(sourceName);
        if (resolvedSourceName == null) {
            resolvedSourceName = uploadedFileName == null ? "客户档案表" : uploadedFileName;
        }
        byte[] fileBytes = readImportBytes(file);
        ExcelImportSheet sheet = parseImportSheet(uploadedFileName, fileBytes);
        if (sheet.rows().isEmpty()) {
            throw new IllegalArgumentException("导入内容没有有效数据行");
        }

        ErpCustomerCategory defaultCategory = erpCustomerCategoryMapper.findDefault(tenantId);
        if (erpCustomerImportBatchMapper == null || erpCustomerImportItemMapper == null) {
            return importCustomersSynchronously(tenantId, sheet, defaultCategory, resolvedSourceName);
        }
        final String finalSourceName = resolvedSourceName;
        ErpCustomerImportBatch batch = createImportBatch(tenantId, uploadedFileName, finalSourceName, sheet);
        scheduleImportBatch(batch.getId(), tenantId, uploadedFileName, fileBytes, finalSourceName);
        return new ErpCustomerImportResult(batch.getId(), batch.getBatchNo(), batch.getStatus(), batch.getTotalCount(), 0, 0);
    }

    @Override
    public List<ErpCustomerImportBatchSummary> listImportBatches() {
        if (erpCustomerImportBatchMapper == null) {
            return List.of();
        }
        Long tenantId = TenantContext.requireTenantId();
        return erpCustomerImportBatchMapper.selectList(new QueryWrapper<ErpCustomerImportBatch>()
                .eq("tenant_id", tenantId)
                .isNull("deleted_at")
                .orderByDesc("id"))
            .stream()
            .map(item -> new ErpCustomerImportBatchSummary(
                item.getId(),
                item.getBatchNo(),
                item.getSourceName(),
                item.getImportMode(),
                item.getTotalCount(),
                item.getSuccessCount(),
                item.getFailedCount(),
                item.getStatus(),
                item.getSummary(),
                item.getCreatedBy(),
                item.getCreatedAt()
            ))
            .toList();
    }

    @Override
    public List<ErpCustomerImportItemView> listImportBatchItems(Long batchId) {
        if (erpCustomerImportBatchMapper == null || erpCustomerImportItemMapper == null) {
            return List.of();
        }
        Long tenantId = TenantContext.requireTenantId();
        ErpCustomerImportBatch batch = erpCustomerImportBatchMapper.selectOne(new QueryWrapper<ErpCustomerImportBatch>()
            .eq("tenant_id", tenantId)
            .eq("id", batchId)
            .isNull("deleted_at"));
        if (batch == null) {
            throw new IllegalArgumentException("导入批次不存在");
        }
        return erpCustomerImportItemMapper.selectList(new QueryWrapper<ErpCustomerImportItem>()
                .eq("tenant_id", tenantId)
                .eq("batch_id", batchId)
                .isNull("deleted_at")
                .orderByAsc("row_no"))
            .stream()
            .map(item -> new ErpCustomerImportItemView(
                item.getId(),
                item.getRowNo(),
                item.getSourceCode(),
                item.getSourceName(),
                item.getMatchedCustomerId(),
                item.getCategoryName(),
                item.getSettlementMethodName(),
                item.getReceiptMethodName(),
                item.getDeliveryMethodName(),
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

    private ErpCustomerImportResult importCustomersSynchronously(Long tenantId,
                                                                ExcelImportSheet sheet,
                                                                ErpCustomerCategory defaultCategory,
                                                                String resolvedSourceName) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int rowNo = 2;
        for (Map<String, String> row : sheet.rows()) {
            try {
                upsertImportedCustomer(tenantId, rowNo, row, defaultCategory, warnings, resolvedSourceName);
                successCount++;
            } catch (IllegalArgumentException ex) {
                errors.add("第" + rowNo + "行：" + ex.getMessage());
            }
            rowNo++;
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("；", errors));
        }
        return new ErpCustomerImportResult(null, null, warnings.isEmpty() ? "DONE" : "DONE_WITH_ERRORS", sheet.rows().size(), successCount, 0);
    }

    private ErpCustomer upsertImportedCustomer(Long tenantId,
                                               int rowNo,
                                               Map<String, String> row,
                                               ErpCustomerCategory defaultCategory,
                                               List<String> warnings,
                                               String sourceName) {
        return upsertImportedCustomer(tenantId, rowNo, row, defaultCategory, warnings, sourceName, null);
    }

    private ErpCustomer upsertImportedCustomer(Long tenantId,
                                               int rowNo,
                                               Map<String, String> row,
                                               ErpCustomerCategory defaultCategory,
                                               List<String> warnings,
                                               String sourceName,
                                               ErpCustomer existingCustomer) {
        String code = trimToNull(firstNonBlank(row, "编码"));
        String name = trimToNull(firstNonBlank(row, "名称"));
        if (code == null) {
            throw new IllegalArgumentException("编码不能为空");
        }
        if (name == null) {
            throw new IllegalArgumentException("名称不能为空");
        }
        String settlementName = trimToNull(firstNonBlank(row, "默认结算方式"));
        String settlementCode = resolveSettlementMethodCode(tenantId, settlementName);
        if (settlementName != null && settlementCode == null) {
            var defaultSettlement = erpSettlementMethodMapper.findDefault(tenantId);
            if (defaultSettlement == null || trimToNull(defaultSettlement.getCode()) == null) {
                throw new IllegalArgumentException("默认结算方式未匹配，且系统未配置默认结算方式");
            }
            settlementCode = defaultSettlement.getCode();
            warnings.add("第" + rowNo + "行：默认结算方式未匹配，已按系统默认结算方式处理");
        }

        ErpCustomerCategory category = resolveCustomerCategory(tenantId, trimToNull(firstNonBlank(row, "客户类型")));
        if (category == null) {
            category = defaultCategory;
            if (category == null) {
                throw new IllegalArgumentException("客户类型未匹配，且系统未配置默认客户类别");
            }
            warnings.add("第" + rowNo + "行：客户类型未匹配，已落到默认客户类别");
        }

        ErpCustomer customer = existingCustomer == null ? erpCustomerMapper.findByCode(tenantId, code) : existingCustomer;
        boolean created = customer == null;
        if (customer == null) {
            customer = new ErpCustomer();
            customer.setTenantId(tenantId);
            customer.setCode(code);
            customer.setCreatedAt(Instant.now());
        }
        customer.setName(name);
        customer.setShortName(name);
        customer.setCategoryId(category.getId());
        customer.setContact(trimToNull(firstNonBlank(row, "联系人")));
        String legacyContactField = trimToNull(firstNonBlank(row, "联系方式（电话，手机）"));
        String importedPhone = trimToNull(firstNonBlank(row, "电话"));
        String importedMobile = trimToNull(firstNonBlank(row, "手机", "联系人移动电话"));
        List<String> legacyContactValues = splitImportedContactValues(legacyContactField);
        customer.setPhone(importedPhone != null ? importedPhone : resolveFirstPhoneLike(legacyContactValues, legacyContactField));
        customer.setMobile(importedMobile != null ? importedMobile : resolveFirstMobileLike(legacyContactValues, legacyContactField));
        customer.setContacts(buildImportedContacts(customer.getContact(), importedPhone, importedMobile, legacyContactValues, legacyContactField));
        customer.setAddress(trimToNull(firstNonBlank(row, "地址")));
        customer.setRemark(mergeImportRemark(trimToNull(firstNonBlank(row, "备注")), sourceName));
        customer.setTaxNo(trimToNull(firstNonBlank(row, "统一信用代码")));
        customer.setInvoiceTitle(trimToNull(firstNonBlank(row, "开票类型")));
        customer.setDefaultSettlementMethodCode(settlementCode);
        customer.setDefaultReceiptMethodCode(resolveReceiptMethodCode(tenantId, trimToNull(firstNonBlank(row, "默认收款方式"))));
        customer.setDeliveryMethodCode(resolveDeliveryMethodCode(tenantId, trimToNull(firstNonBlank(row, "运输方式"))));
        customer.setCreditLimit(parseOptionalDecimal(firstNonBlank(row, "月销量目标")));
        customer.setEnabled(parseCustomerEnabled(firstNonBlank(row, "状态", "启用/停用状态")));
        customer.setUpdatedAt(Instant.now());
        normalizeDefaultSettlementMethodCode(customer, tenantId);
        applyDefaultMethodsIfMissing(customer, tenantId);
        if (customer.getId() == null) {
            erpCustomerMapper.insert(customer);
        } else {
            erpCustomerMapper.updateById(customer);
        }
        if (!created && trimToNull(firstNonBlank(row, "名称")) != null) {
            warnings.add("第" + rowNo + "行：客户编码 " + code + " 已存在，已按编码更新");
        }
        return customer;
    }

    private ErpCustomerCategory resolveCustomerCategory(Long tenantId, String name) {
        if (name == null) {
            return null;
        }
        List<ErpCustomerCategory> matches = erpCustomerCategoryMapper.selectList(new QueryWrapper<ErpCustomerCategory>()
            .eq("tenant_id", tenantId)
            .eq("name", name));
        return matches.isEmpty() ? null : matches.get(0);
    }

    private String resolveSettlementMethodCode(Long tenantId, String name) {
        if (name == null) {
            return null;
        }
        var methods = erpSettlementMethodMapper.selectList(new QueryWrapper<com.example.wms.entity.erp.ErpSettlementMethod>()
            .eq("tenant_id", tenantId)
            .eq("name", name));
        return methods.isEmpty() ? null : methods.get(0).getCode();
    }

    private String resolveReceiptMethodCode(Long tenantId, String name) {
        if (name == null) {
            return null;
        }
        var methods = erpReceiptMethodMapper.selectList(new QueryWrapper<com.example.wms.entity.erp.ErpReceiptMethod>()
            .eq("tenant_id", tenantId)
            .eq("name", name));
        return methods.isEmpty() ? null : methods.get(0).getCode();
    }

    private String resolveDeliveryMethodCode(Long tenantId, String name) {
        if (name == null) {
            return null;
        }
        var methods = erpDeliveryMethodMapper.selectList(new QueryWrapper<com.example.wms.entity.erp.ErpDeliveryMethod>()
            .eq("tenant_id", tenantId)
            .eq("name", name));
        return methods.isEmpty() ? null : methods.get(0).getCode();
    }

    private void ensureCustomerNotReferenced(Long tenantId, Long customerId) {
        if (erpSaleOrderMapper.selectCount(new QueryWrapper<ErpSaleOrder>()
            .eq("tenant_id", tenantId)
            .eq("customer_id", customerId)) > 0) {
            throw new IllegalArgumentException("客户已被销售单引用，不能删除");
        }
        if (erpSaleReturnMapper.selectCount(new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", tenantId)
            .eq("customer_id", customerId)) > 0) {
            throw new IllegalArgumentException("客户已被销售退货单引用，不能删除");
        }
        if (erpReceiptMapper.selectCount(new QueryWrapper<ErpReceipt>()
            .eq("tenant_id", tenantId)
            .eq("customer_id", customerId)) > 0) {
            throw new IllegalArgumentException("客户已被收款单引用，不能删除");
        }
        if (erpAccountsReceivableMapper.selectCount(new QueryWrapper<ErpAccountsReceivable>()
            .eq("tenant_id", tenantId)
            .eq("customer_id", customerId)) > 0) {
            throw new IllegalArgumentException("客户已被应收单引用，不能删除");
        }
    }

    private QueryWrapper<ErpCustomer> baseWrapper(String keyword, String contact, String phone, Boolean enabled, Long categoryId) {
        QueryWrapper<ErpCustomer> wrapper = new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", TenantContext.requireTenantId());
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
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        return wrapper;
    }

    private List<ErpCustomer> rankedSearchOptions(String keyword, int size) {
        QueryWrapper<ErpCustomer> wrapper = baseEnabledCustomerWrapper();
        List<ErpCustomer> candidates = erpCustomerMapper.selectList(wrapper);
        int safeSize = Math.max(1, Math.min(size, 20));
        return candidates.stream()
            .map(customer -> new CustomerSearchHit(customer, scoreCustomer(customer, keyword)))
            .filter(hit -> hit.score() > 0)
            .sorted(Comparator
                .comparingInt(CustomerSearchHit::score).reversed()
                .thenComparing(hit -> hit.customer().getId(), Comparator.nullsLast(Long::compareTo)))
            .map(CustomerSearchHit::customer)
            .limit(safeSize)
            .toList();
    }

    private QueryWrapper<ErpCustomer> baseEnabledCustomerWrapper() {
        return new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("is_enabled", true)
            .orderByAsc("id");
    }

    private int scoreCustomer(ErpCustomer customer, String rawKeyword) {
        String keyword = normalizeSearchText(rawKeyword);
        if (keyword.isEmpty()) return 0;
        int score = 0;
        score = Math.max(score, scoreText(customer.getName(), keyword, 900));
        score = Math.max(score, scoreText(customer.getShortName(), keyword, 850));
        score = Math.max(score, scoreText(customer.getContact(), keyword, 760));
        score = Math.max(score, scoreText(customer.getPhone(), keyword, 720));
        score = Math.max(score, scoreText(customer.getMobile(), keyword, 720));
        score = Math.max(score, scoreText(customer.getContacts() == null ? null : customer.getContacts().toString(), keyword, 680));
        score = Math.max(score, scorePinyin(customer.getName(), keyword, 820));
        score = Math.max(score, scorePinyin(customer.getShortName(), keyword, 780));
        score = Math.max(score, scorePinyin(customer.getContact(), keyword, 700));
        return score;
    }

    private int scoreText(String value, String keyword, int exactScore) {
        String normalized = normalizeSearchText(value);
        if (normalized.isEmpty()) return 0;
        if (normalized.equals(keyword)) return exactScore;
        if (normalized.startsWith(keyword)) return exactScore - 80;
        if (normalized.contains(keyword)) return exactScore - 220;
        if (isSubsequence(normalized, keyword)) return exactScore - 420;
        return 0;
    }

    private int scorePinyin(String value, String keyword, int exactScore) {
        PinyinText pinyin = toPinyinText(value);
        int score = 0;
        score = Math.max(score, scoreText(pinyin.full(), keyword, exactScore));
        score = Math.max(score, scoreText(pinyin.initials(), keyword, exactScore - 40));
        return score;
    }

    private String normalizeSearchText(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase().replaceAll("\\s+", "");
    }

    private boolean isSubsequence(String source, String query) {
        if (query.isEmpty()) return true;
        int index = 0;
        for (int i = 0; i < source.length() && index < query.length(); i++) {
            if (source.charAt(i) == query.charAt(index)) {
                index++;
            }
        }
        return index == query.length();
    }

    private PinyinText toPinyinText(String value) {
        if (value == null || value.isBlank()) {
            return new PinyinText("", "");
        }
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        StringBuilder full = new StringBuilder();
        StringBuilder initials = new StringBuilder();
        for (char ch : value.toCharArray()) {
            String token = toPinyinToken(ch, format);
            if (token.isEmpty()) {
                continue;
            }
            full.append(token);
            initials.append(token.charAt(0));
        }
        return new PinyinText(full.toString(), initials.toString());
    }

    private String toPinyinToken(char ch, HanyuPinyinOutputFormat format) {
        if (Character.isWhitespace(ch)) return "";
        if (ch < 128) return String.valueOf(Character.toLowerCase(ch));
        try {
            String[] values = PinyinHelper.toHanyuPinyinStringArray(ch, format);
            if (values != null && values.length > 0) {
                return values[0];
            }
        } catch (BadHanyuPinyinOutputFormatCombination ignored) {
            return "";
        }
        return String.valueOf(ch);
    }

    private record CustomerSearchHit(ErpCustomer customer, int score) {
    }

    private record PinyinText(String full, String initials) {
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

    private ErpCustomerImportBatch createImportBatch(Long tenantId,
                                                     String uploadedFileName,
                                                     String sourceName,
                                                     ExcelImportSheet sheet) {
        Instant now = Instant.now();
        ErpCustomerImportBatch batch = new ErpCustomerImportBatch();
        batch.setTenantId(tenantId);
        batch.setBatchNo("CI" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault()).format(now));
        batch.setSourceName(sourceName);
        batch.setImportMode("EXCEL_UPLOAD");
        batch.setTotalCount(sheet.rows().size());
        batch.setSuccessCount(0);
        batch.setFailedCount(0);
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
        erpCustomerImportBatchMapper.insert(batch);
        return batch;
    }

    private void processImportBatch(Long batchId,
                                    Long tenantId,
                                    String uploadedFileName,
                                    byte[] fileBytes,
                                    String sourceName) {
        TenantContext.setTenantId(tenantId);
        try {
            ExcelImportSheet sheet = parseImportSheet(uploadedFileName, fileBytes);
            ErpCustomerCategory defaultCategory = erpCustomerCategoryMapper.findDefault(tenantId);
            int successCount = 0;
            int failedCount = 0;
            for (int start = 0; start < sheet.rows().size(); start += IMPORT_CHUNK_SIZE) {
                int end = Math.min(start + IMPORT_CHUNK_SIZE, sheet.rows().size());
                int chunkStart = start;
                int chunkEnd = end;
                ImportChunkResult result = executeInImportTransaction(() ->
                    processImportChunk(batchId, tenantId, sheet.rows().subList(chunkStart, chunkEnd), chunkStart, defaultCategory, sourceName)
                );
                successCount += result.successCount();
                failedCount += result.failedCount();
            }
            int finalSuccessCount = successCount;
            int finalFailedCount = failedCount;
            executeInImportTransaction(() -> {
                updateImportBatchResult(batchId, tenantId, sheet.rows().size(), finalSuccessCount, finalFailedCount, null);
                return null;
            });
        } catch (Exception ex) {
            executeInImportTransaction(() -> {
                updateImportBatchResult(batchId, tenantId, 0, 0, 0, ex.getMessage());
                return null;
            });
        } finally {
            TenantContext.clear();
        }
    }

    private void scheduleImportBatch(Long batchId,
                                     Long tenantId,
                                     String uploadedFileName,
                                     byte[] fileBytes,
                                     String sourceName) {
        Runnable task = () -> processImportBatch(batchId, tenantId, uploadedFileName, fileBytes, sourceName);
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

    private ImportChunkResult processImportChunk(Long batchId,
                                                Long tenantId,
                                                List<Map<String, String>> rows,
                                                int startIndex,
                                                ErpCustomerCategory defaultCategory,
                                                String sourceName) {
        int successCount = 0;
        int failedCount = 0;
        List<String> codes = rows.stream()
            .map(row -> trimToNull(firstNonBlank(row, "编码")))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<String, ErpCustomer> existingByCode = codes.isEmpty()
            ? Map.of()
            : erpCustomerMapper.findByCodes(tenantId, codes)
                .stream()
                .collect(Collectors.toMap(ErpCustomer::getCode, Function.identity(), (left, right) -> left));
        List<ErpCustomerImportItem> importItems = new ArrayList<>();
        for (int offset = 0; offset < rows.size(); offset++) {
            int rowNo = startIndex + offset + 2;
            Map<String, String> row = new HashMap<>(rows.get(offset));
            ErpCustomerImportItem item = buildImportItem(batchId, tenantId, rowNo, row);
            List<String> warnings = new ArrayList<>();
            try {
                String code = trimToNull(firstNonBlank(row, "编码"));
                ErpCustomer inserted = upsertImportedCustomer(
                    tenantId,
                    rowNo,
                    row,
                    defaultCategory,
                    warnings,
                    sourceName,
                    code == null ? null : existingByCode.get(code)
                );
                item.setMatchedCustomerId(inserted == null ? null : inserted.getId());
                item.setStatus("SUCCESS");
                item.setWarningMessage(warnings.isEmpty() ? null : String.join("；", warnings));
                item.setMatchedStrategy("CODE_UPSERT");
                item.setNormalizedPayload(valueToTree(row));
                successCount++;
            } catch (IllegalArgumentException ex) {
                item.setStatus("FAILED");
                item.setErrorMessage(ex.getMessage());
                item.setSuggestion("修正该行后重新导入");
                failedCount++;
            }
            importItems.add(item);
        }
        if (!importItems.isEmpty()) {
            erpCustomerImportItemMapper.insertBatch(importItems);
        }
        return new ImportChunkResult(successCount, failedCount);
    }

    private <T> T executeInImportTransaction(java.util.concurrent.Callable<T> action) {
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

    private record ImportChunkResult(int successCount, int failedCount) {
    }

    private ErpCustomerImportItem buildImportItem(Long batchId,
                                                  Long tenantId,
                                                  int rowNo,
                                                  Map<String, String> row) {
        Instant now = Instant.now();
        ErpCustomerImportItem item = new ErpCustomerImportItem();
        item.setTenantId(tenantId);
        item.setBatchId(batchId);
        item.setRowNo(rowNo);
        item.setSourceCode(trimToNull(firstNonBlank(row, "编码")));
        item.setSourceName(trimToNull(firstNonBlank(row, "名称")));
        item.setCategoryName(trimToNull(firstNonBlank(row, "客户类型")));
        item.setSettlementMethodName(trimToNull(firstNonBlank(row, "默认结算方式")));
        item.setReceiptMethodName(trimToNull(firstNonBlank(row, "默认收款方式")));
        item.setDeliveryMethodName(trimToNull(firstNonBlank(row, "运输方式")));
        item.setStatus("PENDING");
        item.setRawRow(valueToTree(row));
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return item;
    }

    private void updateImportBatchResult(Long batchId,
                                         Long tenantId,
                                         int totalCount,
                                         int successCount,
                                         int failedCount,
                                         String fatalMessage) {
        ErpCustomerImportBatch batch = erpCustomerImportBatchMapper.selectOne(new QueryWrapper<ErpCustomerImportBatch>()
            .eq("tenant_id", tenantId)
            .eq("id", batchId)
            .isNull("deleted_at"));
        if (batch == null) {
            return;
        }
        batch.setTotalCount(totalCount > 0 ? totalCount : batch.getTotalCount());
        batch.setSuccessCount(successCount);
        batch.setFailedCount(failedCount);
        batch.setUpdatedAt(Instant.now());
        if (fatalMessage != null && !fatalMessage.isBlank()) {
            batch.setStatus("FAILED");
            batch.setSummary("导入失败：" + fatalMessage);
        } else {
            batch.setStatus(failedCount > 0 ? "DONE_WITH_ERRORS" : "DONE");
            batch.setSummary("导入完成：成功 " + successCount + " 行，失败 " + failedCount + " 行");
        }
        erpCustomerImportBatchMapper.updateById(batch);
    }

    private String resolveCurrentUsername() {
        return "system";
    }

    private JsonNode valueToTree(Object value) {
        return objectMapper.valueToTree(value);
    }

    private String wrapLike(String value) {
        return "%" + value.trim() + "%";
    }

    private void applyRequest(ErpCustomer customer, ErpCustomerCreateRequest request, Long categoryId) {
        customer.setCode(request.code());
        customer.setName(request.name());
        customer.setCategoryId(categoryId);
        customer.setShortName(request.shortName());
        customer.setContact(request.contact());
        customer.setPhone(request.phone());
        customer.setMobile(request.mobile());
        customer.setEmail(request.email());
        customer.setAddress(request.address());
        customer.setTaxNo(request.taxNo());
        customer.setBankName(request.bankName());
        customer.setBankAccount(request.bankAccount());
        customer.setInvoiceTitle(request.invoiceTitle());
        customer.setDefaultSettlementMethodCode(request.defaultSettlementMethodCode());
        customer.setDefaultReceiptMethodCode(request.defaultReceiptMethodCode());
        customer.setDeliveryMethodCode(request.deliveryMethodCode());
        customer.setCreditLimit(request.creditLimit());
        customer.setContacts(parseContacts(request.contacts()));
        customer.setCounterpartySubjectId(validateCounterpartySubjectId(request.counterpartySubjectId()));
        customer.setRemark(request.remark());
    }

    private void applyRequest(ErpCustomer customer, ErpCustomerUpdateRequest request, Long categoryId) {
        customer.setCode(request.code());
        customer.setName(request.name());
        customer.setCategoryId(categoryId);
        customer.setShortName(request.shortName());
        customer.setContact(request.contact());
        customer.setPhone(request.phone());
        customer.setMobile(request.mobile());
        customer.setEmail(request.email());
        customer.setAddress(request.address());
        customer.setTaxNo(request.taxNo());
        customer.setBankName(request.bankName());
        customer.setBankAccount(request.bankAccount());
        customer.setInvoiceTitle(request.invoiceTitle());
        customer.setDefaultSettlementMethodCode(request.defaultSettlementMethodCode());
        customer.setDefaultReceiptMethodCode(request.defaultReceiptMethodCode());
        customer.setDeliveryMethodCode(request.deliveryMethodCode());
        customer.setCreditLimit(request.creditLimit());
        customer.setContacts(parseContacts(request.contacts()));
        customer.setCounterpartySubjectId(validateCounterpartySubjectId(request.counterpartySubjectId()));
        customer.setRemark(request.remark());
    }

    private Long validateCounterpartySubjectId(Long subjectId) {
        if (subjectId == null) {
            return null;
        }
        ErpCounterpartySubject subject = erpCounterpartySubjectMapper.selectOne(new QueryWrapper<ErpCounterpartySubject>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", subjectId));
        if (subject == null) {
            throw new IllegalArgumentException("往来主体不存在");
        }
        return subjectId;
    }

    private Boolean parseCustomerEnabled(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return true;
        }
        if (List.of("启用", "正常", "enabled", "ENABLED").contains(normalized)) {
            return true;
        }
        if (List.of("停用", "禁用", "disabled", "DISABLED").contains(normalized)) {
            return false;
        }
        return true;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(Map<String, String> row, String... headers) {
        for (String header : headers) {
            String value = trimToNull(row.get(header));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private JsonNode buildImportedContacts(String contactName,
                                           String importedPhone,
                                           String importedMobile,
                                           List<String> legacyContactValues,
                                           String legacyContactField) {
        ArrayNode contacts = objectMapper.createArrayNode();
        addImportedContactNode(contacts, contactName, importedPhone, importedMobile, contacts.isEmpty());
        for (String value : legacyContactValues) {
            if (isMobileLike(value)) {
                addImportedContactNode(contacts, contactName, null, value, contacts.isEmpty());
            } else {
                addImportedContactNode(contacts, contactName, extractFirstPhone(value), extractFirstMobile(value), contacts.isEmpty());
            }
        }
        if (contacts.isEmpty() && trimToNull(legacyContactField) != null) {
            addImportedContactNode(
                contacts,
                contactName,
                extractFirstPhone(legacyContactField),
                extractFirstMobile(legacyContactField),
                true
            );
        }
        return contacts.isEmpty() ? null : contacts;
    }

    private void addImportedContactNode(ArrayNode contacts,
                                        String contactName,
                                        String phone,
                                        String mobile,
                                        boolean isPrimary) {
        String normalizedPhone = trimToNull(phone);
        String normalizedMobile = trimToNull(mobile);
        if (normalizedPhone == null && normalizedMobile == null) {
            return;
        }
        ObjectNode node = objectMapper.createObjectNode();
        String normalizedName = trimToNull(contactName);
        if (normalizedName != null) {
            node.put("name", normalizedName);
        }
        if (normalizedPhone != null) {
            node.put("phone", normalizedPhone);
        }
        if (normalizedMobile != null) {
            node.put("mobile", normalizedMobile);
        }
        node.put("isPrimary", isPrimary);
        contacts.add(node);
    }

    private List<String> splitImportedContactValues(String rawContactInfo) {
        String normalized = trimToNull(rawContactInfo);
        if (normalized == null) {
            return List.of();
        }
        return java.util.regex.Pattern.compile("[/／,，;；\\n\\r]+")
            .splitAsStream(normalized)
            .map(this::trimToNull)
            .filter(Objects::nonNull)
            .toList();
    }

    private String resolveFirstMobileLike(List<String> contactValues, String fallbackText) {
        for (String value : contactValues) {
            if (isMobileLike(value)) {
                return value;
            }
        }
        return extractFirstMobile(fallbackText);
    }

    private String resolveFirstPhoneLike(List<String> contactValues, String fallbackText) {
        for (String value : contactValues) {
            if (!isMobileLike(value)) {
                String phone = extractFirstPhone(value);
                return phone == null ? value : phone;
            }
        }
        if (!contactValues.isEmpty()) {
            return null;
        }
        String phone = extractFirstPhone(fallbackText);
        return isMobilePrefixOnly(phone, fallbackText) ? null : phone;
    }

    private boolean isMobilePrefixOnly(String phone, String sourceText) {
        if (phone == null || sourceText == null || !phone.matches("^\\d{7,8}$")) {
            return false;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("1\\d{10}").matcher(sourceText);
        while (matcher.find()) {
            if (matcher.group().startsWith(phone)) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal parseOptionalDecimal(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return new BigDecimal(normalized.replace(",", ""));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("数字格式不正确: " + normalized);
        }
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

    private String mergeImportRemark(String remark, String sourceName) {
        String base = trimToNull(remark);
        String suffix = "Excel导入：" + sourceName;
        if (base == null) {
            return suffix;
        }
        return base + "；" + suffix;
    }

    private void syncCounterpartySubjectLink(Long tenantId, Long customerId, Long oldSubjectId, Long newSubjectId) {
        if (java.util.Objects.equals(oldSubjectId, newSubjectId)) {
            return;
        }
        ensureCustomerCanRebind(tenantId, customerId, oldSubjectId, newSubjectId);

        ErpCounterpartySubjectLink existing = erpCounterpartySubjectLinkMapper.selectOne(new QueryWrapper<ErpCounterpartySubjectLink>()
            .eq("tenant_id", tenantId)
            .eq("target_type", "CUSTOMER")
            .eq("target_id", customerId));
        if (existing != null) {
            erpCounterpartySubjectLinkMapper.deleteById(existing.getId());
        }
        if (newSubjectId != null) {
            ErpCounterpartySubjectLink link = new ErpCounterpartySubjectLink();
            link.setTenantId(tenantId);
            link.setSubjectId(newSubjectId);
            link.setTargetType("CUSTOMER");
            link.setTargetId(customerId);
            link.setRoleType("CUSTOMER");
            link.setPrimary(false);
            link.setCreatedAt(Instant.now());
            link.setUpdatedAt(Instant.now());
            erpCounterpartySubjectLinkMapper.insert(link);
        }
    }

    private void ensureCustomerCanRebind(Long tenantId, Long customerId, Long oldSubjectId, Long newSubjectId) {
        if (oldSubjectId == null && newSubjectId == null) {
            return;
        }
        ErpCounterpartyUnbindCheck check = buildCustomerRebindCheck(tenantId, customerId);
        if (!check.allowed()) {
            throw new IllegalArgumentException(String.join("；", check.blockingReasons()));
        }
    }

    private ErpCounterpartyUnbindCheck buildCustomerRebindCheck(Long tenantId, Long customerId) {
        List<String> reasons = new java.util.ArrayList<>();
        List<ErpCounterpartyPendingDoc> docs = new java.util.ArrayList<>();

        List<ErpSaleOrder> openSaleOrders = erpSaleOrderMapper.selectList(new QueryWrapper<ErpSaleOrder>()
            .eq("tenant_id", tenantId)
            .eq("customer_id", customerId)
            .in("status", ErpCounterpartyGuardRules.BLOCKING_DOCUMENT_STATUSES));
        if (!openSaleOrders.isEmpty()) {
            reasons.add("存在未完成销售单");
            docs.addAll(openSaleOrders.stream()
                .map(item -> new ErpCounterpartyPendingDoc("SALE_ORDER", item.getId(), item.getOrderNo(), item.getStatus(), resolveSaleOrderRouteKey(item.getStatus())))
                .toList());
        }

        List<ErpSaleReturn> openSaleReturns = erpSaleReturnMapper.selectList(new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", tenantId)
            .eq("customer_id", customerId)
            .in("status", ErpCounterpartyGuardRules.BLOCKING_DOCUMENT_STATUSES));
        if (!openSaleReturns.isEmpty()) {
            reasons.add("存在未完成销售退货单");
            docs.addAll(openSaleReturns.stream()
                .map(item -> new ErpCounterpartyPendingDoc("SALE_RETURN", item.getId(), item.getOrderNo(), item.getStatus(), resolveSaleReturnRouteKey(item.getStatus())))
                .toList());
        }

        List<ErpReceipt> openReceipts = erpReceiptMapper.selectList(new QueryWrapper<ErpReceipt>()
            .eq("tenant_id", tenantId)
            .eq("customer_id", customerId)
            .in("status", ErpCounterpartyGuardRules.BLOCKING_DOCUMENT_STATUSES));
        if (!openReceipts.isEmpty()) {
            reasons.add("存在未完成收款单");
            docs.addAll(openReceipts.stream()
                .map(item -> new ErpCounterpartyPendingDoc("RECEIPT", item.getId(), item.getReceiptNo(), item.getStatus(), "erp-receipts-detail"))
                .toList());
        }

        List<ErpAccountsReceivable> receivables = erpAccountsReceivableMapper.selectList(new QueryWrapper<ErpAccountsReceivable>()
            .eq("tenant_id", tenantId)
            .eq("customer_id", customerId)
            .ne("status", ErpCounterpartyGuardRules.RED_FLUSHED_STATUS));
        java.math.BigDecimal totalReceivable = receivables.stream()
            .map(ErpAccountsReceivable::getUnpaidAmount)
            .filter(value -> value != null && value.compareTo(java.math.BigDecimal.ZERO) != 0)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        if (totalReceivable.compareTo(java.math.BigDecimal.ZERO) != 0) {
            reasons.add("存在未完成应收，未收金额合计：" + totalReceivable.stripTrailingZeros().toPlainString());
        }

        return new ErpCounterpartyUnbindCheck(reasons.isEmpty(), reasons, docs);
    }

    private String resolveSaleOrderRouteKey(String status) {
        return "DRAFT".equals(status) ? "erp-sale-orders-draft-edit" : "erp-sale-orders-approved-detail";
    }

    private String resolveSaleReturnRouteKey(String status) {
        return "DRAFT".equals(status) ? "erp-sale-returns-draft-edit" : "erp-sale-returns-approved-detail";
    }

    private void applyDefaultMethodsIfMissing(ErpCustomer customer, Long tenantId) {
        if (customer.getDefaultSettlementMethodCode() == null || customer.getDefaultSettlementMethodCode().isBlank()) {
            var defaultSettlement = erpSettlementMethodMapper.findDefault(tenantId);
            if (defaultSettlement != null) {
                customer.setDefaultSettlementMethodCode(defaultSettlement.getCode());
            }
        }
        if (customer.getDefaultReceiptMethodCode() == null || customer.getDefaultReceiptMethodCode().isBlank()) {
            var defaultReceiptMethod = erpReceiptMethodMapper.findDefault(tenantId);
            if (defaultReceiptMethod != null) {
                customer.setDefaultReceiptMethodCode(defaultReceiptMethod.getCode());
            }
        }
        if (customer.getDeliveryMethodCode() == null || customer.getDeliveryMethodCode().isBlank()) {
            var defaultDelivery = erpDeliveryMethodMapper.findDefault(tenantId);
            if (defaultDelivery != null) {
                customer.setDeliveryMethodCode(defaultDelivery.getCode());
            }
        }
    }

    private void normalizeDefaultSettlementMethodCode(ErpCustomer customer, Long tenantId) {
        String submittedCode = trimToNull(customer.getDefaultSettlementMethodCode());
        if (submittedCode == null) {
            return;
        }
        if (erpSettlementMethodMapper.findByCode(tenantId, submittedCode) != null) {
            customer.setDefaultSettlementMethodCode(submittedCode);
            return;
        }
        var defaultSettlement = erpSettlementMethodMapper.findDefault(tenantId);
        customer.setDefaultSettlementMethodCode(defaultSettlement == null ? null : trimToNull(defaultSettlement.getCode()));
    }

    private String generateCustomerCode(Long tenantId) {
        String prefix = readConfig("erp.customer.code.prefix", "CU");
        String dateFormat = readConfig("erp.customer.code.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.customer.code.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, CUSTOMER_CODE_TYPE, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, CUSTOMER_CODE_TYPE, dateKey);
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
}
