package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockCountCreateRequest;
import com.example.wms.dto.erp.ErpStockCountDetail;
import com.example.wms.dto.erp.ErpStockCountItemView;
import com.example.wms.dto.erp.ErpStockCountItemRequest;
import com.example.wms.dto.erp.ErpStockCountUpdateRequest;
import com.example.wms.dto.erp.ErpStockInitImportBatchSummary;
import com.example.wms.dto.erp.ErpStockInitImportFieldOption;
import com.example.wms.dto.erp.ErpStockInitImportHeaderMapping;
import com.example.wms.dto.erp.ErpStockInitImportItemView;
import com.example.wms.dto.erp.ErpStockInitImportPreview;
import com.example.wms.dto.erp.ErpStockInitImportResult;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockCount;
import com.example.wms.entity.erp.ErpStockInitImportBatch;
import com.example.wms.entity.erp.ErpStockInitImportItem;
import com.example.wms.entity.erp.ErpStockCountItem;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.entity.erp.ErpStockTxn;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpProductStockPolicy;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpProductStockPolicyMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockCountItemMapper;
import com.example.wms.mapper.erp.ErpStockCountMapper;
import com.example.wms.mapper.erp.ErpStockInitImportBatchMapper;
import com.example.wms.mapper.erp.ErpStockInitImportItemMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.ErpStockCountService;
import com.example.wms.service.erp.support.ExcelImportParser;
import com.example.wms.service.erp.support.ExcelImportSheet;
import com.example.wms.service.erp.support.ErpCostService;
import com.example.wms.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

// 库存盘点服务实现（ERP进销存）
@Service
public class ErpStockCountServiceImpl implements ErpStockCountService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVING = "APPROVING";
    private static final String STATUS_APPROVE_FAILED = "APPROVE_FAILED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_RED_FLUSHED = "RED_FLUSHED";
    private static final String TYPE_COUNT = "COUNT";
    private static final String TYPE_INIT = "INIT";
    private static final String STRATEGY_MODE_NONE = "NONE";
    private static final String STRATEGY_MODE_PRODUCT = "PRODUCT";
    private static final String STRATEGY_MODE_WAREHOUSE = "WAREHOUSE";
    private static final int IMPORT_CHUNK_SIZE = 100;
    private static final int APPROVE_CHUNK_SIZE = 200;
    private static final Set<String> ADJUSTMENT_REASONS = Set.of("PROFIT", "LOSS", "CORRECTION", "MIGRATION", "OTHER");
    private static final List<ImportFieldDefinition> STOCK_INIT_IMPORT_FIELDS = List.of(
        field("warehouseName", "仓库", false, "仓库", "仓库名称"),
        field("locationName", "库位", false, "库位", "库位名称"),
        field("code", "商品编码", false, "编码", "商品编码", "配件编码", "物料编号"),
        field("productName", "产品名称", true, "产品名称", "商品名称", "配件名称", "名称"),
        field("spec", "规格", false, "规格"),
        field("brand", "品牌", false, "品牌"),
        field("manufacturerCode", "厂家编码", false, "厂家编码"),
        field("supplierName", "来源供应商", false, "来源供应商", "供应商"),
        field("countedQty", "期初数量", true, "库存数", "期初数量", "数量"),
        field("initUnitCost", "期初单价", false, "库存成本价", "期初单价", "成本价"),
        field("initTotalAmount", "期初金额", false, "金额", "期初金额"),
        field("productSafetyStock", "商品安全库存", false, "商品安全库存"),
        field("productMinStock", "商品最低库存", false, "商品最低库存"),
        field("productMaxStock", "商品最高库存", false, "商品最高库存"),
        field("warehouseSafetyStock", "仓库安全库存", false, "标准库存数", "标准库存", "仓库安全库存"),
        field("warehouseMinStock", "仓库最低库存", false, "库存下限", "仓库最低库存"),
        field("warehouseMaxStock", "仓库最高库存", false, "库存上限", "仓库最高库存"),
        field("remark", "备注", false, "备注")
    );
    private static final Map<String, ImportFieldDefinition> STOCK_INIT_IMPORT_FIELD_BY_KEY = STOCK_INIT_IMPORT_FIELDS.stream()
        .collect(Collectors.toUnmodifiableMap(ImportFieldDefinition::key, Function.identity()));
    private static final Map<String, ImportFieldDefinition> STOCK_INIT_IMPORT_FIELD_BY_ALIAS = STOCK_INIT_IMPORT_FIELDS.stream()
        .flatMap(field -> field.aliases().stream().map(alias -> Map.entry(normalizeImportHeader(alias), field)))
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));

    private final ErpStockCountMapper erpStockCountMapper;
    private final ErpStockCountItemMapper erpStockCountItemMapper;
    private final ErpStockBalanceMapper erpStockBalanceMapper;
    private final ErpStockTxnMapper erpStockTxnMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpProductStockPolicyMapper erpProductStockPolicyMapper;
    private final ErpWarehouseMapper erpWarehouseMapper;
    private final ErpLocationMapper erpLocationMapper;
    private final ErpSupplierMapper erpSupplierMapper;
    private final ErpCostService erpCostService;
    private final ExcelImportParser excelImportParser;
    private final ErpStockInitImportBatchMapper erpStockInitImportBatchMapper;
    private final ErpStockInitImportItemMapper erpStockInitImportItemMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Executor importExecutor;
    private final TransactionOperations transactionOperations;

    public ErpStockCountServiceImpl(ErpStockCountMapper erpStockCountMapper,
                                    ErpStockCountItemMapper erpStockCountItemMapper,
                                    ErpStockBalanceMapper erpStockBalanceMapper,
                                    ErpStockTxnMapper erpStockTxnMapper,
                                    ErpOrderSequenceMapper erpOrderSequenceMapper,
                                    SystemConfigMapper systemConfigMapper,
                                    ErpProductMapper erpProductMapper,
                                    ErpProductStockPolicyMapper erpProductStockPolicyMapper,
                                    ErpStockInitImportBatchMapper erpStockInitImportBatchMapper,
                                    ErpStockInitImportItemMapper erpStockInitImportItemMapper,
                                    ErpWarehouseMapper erpWarehouseMapper,
                                    ErpLocationMapper erpLocationMapper,
                                    ErpSupplierMapper erpSupplierMapper,
                                    ErpCostService erpCostService,
                                    ExcelImportParser excelImportParser,
                                    Executor importExecutor,
                                    TransactionOperations transactionOperations) {
        this.erpStockCountMapper = erpStockCountMapper;
        this.erpStockCountItemMapper = erpStockCountItemMapper;
        this.erpStockBalanceMapper = erpStockBalanceMapper;
        this.erpStockTxnMapper = erpStockTxnMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.erpProductMapper = erpProductMapper;
        this.erpProductStockPolicyMapper = erpProductStockPolicyMapper;
        this.erpWarehouseMapper = erpWarehouseMapper;
        this.erpLocationMapper = erpLocationMapper;
        this.erpSupplierMapper = erpSupplierMapper;
        this.erpCostService = erpCostService;
        this.excelImportParser = excelImportParser;
        this.erpStockInitImportBatchMapper = erpStockInitImportBatchMapper;
        this.erpStockInitImportItemMapper = erpStockInitImportItemMapper;
        this.importExecutor = importExecutor == null ? Runnable::run : importExecutor;
        this.transactionOperations = transactionOperations;
    }

    @Autowired
    public ErpStockCountServiceImpl(ErpStockCountMapper erpStockCountMapper,
                                    ErpStockCountItemMapper erpStockCountItemMapper,
                                    ErpStockBalanceMapper erpStockBalanceMapper,
                                    ErpStockTxnMapper erpStockTxnMapper,
                                    ErpOrderSequenceMapper erpOrderSequenceMapper,
                                    SystemConfigMapper systemConfigMapper,
                                    ErpProductMapper erpProductMapper,
                                    ErpProductStockPolicyMapper erpProductStockPolicyMapper,
                                    ErpStockInitImportBatchMapper erpStockInitImportBatchMapper,
                                    ErpStockInitImportItemMapper erpStockInitImportItemMapper,
                                    ErpWarehouseMapper erpWarehouseMapper,
                                    ErpLocationMapper erpLocationMapper,
                                    ErpSupplierMapper erpSupplierMapper,
                                    ErpCostService erpCostService,
                                    @Qualifier("erpImportTaskExecutor") Executor importExecutor,
                                    PlatformTransactionManager transactionManager) {
        this(
            erpStockCountMapper,
            erpStockCountItemMapper,
            erpStockBalanceMapper,
            erpStockTxnMapper,
            erpOrderSequenceMapper,
            systemConfigMapper,
            erpProductMapper,
            erpProductStockPolicyMapper,
            erpStockInitImportBatchMapper,
            erpStockInitImportItemMapper,
            erpWarehouseMapper,
            erpLocationMapper,
            erpSupplierMapper,
            erpCostService,
            new ExcelImportParser(),
            importExecutor,
            new TransactionTemplate(transactionManager)
        );
    }

    @Override
    public List<ErpStockCount> listAll(String keyword, String status, String countType) {
        QueryWrapper<ErpStockCount> wrapper = baseWrapper(keyword, status, normalizeType(countType));
        wrapper.orderByDesc("created_at");
        return erpStockCountMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpStockCount> page(long page, long size, String keyword, String status, String countType) {
        Page<ErpStockCount> pageReq = Page.of(page, size);
        QueryWrapper<ErpStockCount> wrapper = baseWrapper(keyword, status, normalizeType(countType));
        wrapper.orderByDesc("created_at");
        Page<ErpStockCount> result = erpStockCountMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpStockCountDetail getDetail(Long id, String countType) {
        return getDetail(id, countType, true);
    }

    @Override
    public ErpStockCountDetail getDetail(Long id, String countType, boolean includeItems) {
        Long tenantId = TenantContext.requireTenantId();
        ErpStockCount count = requireCountByType(tenantId, id, countType, false);
        if (!includeItems) {
            return new ErpStockCountDetail(count, List.of());
        }
        List<ErpStockCountItem> items = erpStockCountItemMapper.selectList(new QueryWrapper<ErpStockCountItem>()
            .eq("tenant_id", tenantId)
            .eq("count_id", id)
            .orderByAsc("line_no"));
        return new ErpStockCountDetail(count, items);
    }

    @Override
    public PageResponse<ErpStockCountItemView> pageDetailItems(Long id, long page, long size, String countType) {
        Long tenantId = TenantContext.requireTenantId();
        requireCountByType(tenantId, id, countType, false);
        Page<ErpStockCountItem> pageReq = Page.of(page, size);
        QueryWrapper<ErpStockCountItem> wrapper = new QueryWrapper<ErpStockCountItem>()
            .eq("tenant_id", tenantId)
            .eq("count_id", id)
            .orderByAsc("line_no");
        Page<ErpStockCountItem> result = erpStockCountItemMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), enrichDetailItems(result.getRecords()));
    }

    private List<ErpStockCountItemView> enrichDetailItems(List<ErpStockCountItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = items.stream()
                .map(ErpStockCountItem::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> warehouseIds = items.stream()
                .map(ErpStockCountItem::getWarehouseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> locationIds = items.stream()
                .map(ErpStockCountItem::getLocationId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ErpProduct> productMap = (productIds.isEmpty() ? List.<ErpProduct>of() : erpProductMapper.selectBatchIds(productIds))
            .stream()
            .collect(Collectors.toMap(ErpProduct::getId, Function.identity(), (left, right) -> left));
        Map<Long, ErpWarehouse> warehouseMap = (warehouseIds.isEmpty() ? List.<ErpWarehouse>of() : erpWarehouseMapper.selectBatchIds(warehouseIds))
            .stream()
            .collect(Collectors.toMap(ErpWarehouse::getId, Function.identity(), (left, right) -> left));
        Map<Long, ErpLocation> locationMap = (locationIds.isEmpty() ? List.<ErpLocation>of() : erpLocationMapper.selectBatchIds(locationIds))
            .stream()
            .collect(Collectors.toMap(ErpLocation::getId, Function.identity(), (left, right) -> left));
        return items.stream()
            .map(item -> toDetailItemView(item, productMap, warehouseMap, locationMap))
            .toList();
    }

    private ErpStockCountItemView toDetailItemView(ErpStockCountItem item,
                                                   Map<Long, ErpProduct> productMap,
                                                   Map<Long, ErpWarehouse> warehouseMap,
                                                   Map<Long, ErpLocation> locationMap) {
        ErpProduct product = productMap.get(item.getProductId());
        ErpWarehouse warehouse = warehouseMap.get(item.getWarehouseId());
        ErpLocation location = locationMap.get(item.getLocationId());
        return new ErpStockCountItemView(
            item.getId(),
            item.getTenantId(),
            item.getCountId(),
            item.getLineNo(),
            item.getProductId(),
            product == null ? null : product.getCode(),
            product == null ? null : product.getName(),
            item.getWarehouseId(),
            warehouse == null ? null : warehouse.getName(),
            item.getLocationId(),
            location == null ? null : location.getName(),
            item.getSystemQty(),
            item.getCountedQty(),
            item.getInitUnitCost(),
            item.getInitTotalAmount(),
            item.getDiffQty(),
            item.getRemark(),
            item.getCreatedAt(),
            item.getUpdatedAt()
        );
    }

    @Override
    public String nextCountNo(String countType) {
        Long tenantId = TenantContext.requireTenantId();
        return ensureCountNo(tenantId, null, normalizeType(countType));
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_STOCK_COUNT_CREATE", entityType = "erp_stock_count", entityId = "{result.count.id}", detail = "countNo={result.count.countNo}")
    public ErpStockCountDetail create(ErpStockCountCreateRequest request, String countType) {
        return create(request, countType, Set.of());
    }

    private ErpStockCountDetail create(ErpStockCountCreateRequest request,
                                       String countType,
                                       Set<Long> allowedDisabledProductIds) {
        Long tenantId = TenantContext.requireTenantId();
        String operator = resolveCurrentUsername();
        String type = normalizeType(countType == null ? request.countType() : countType);
        if (TYPE_INIT.equals(type) && hasActiveInit(tenantId)) {
            throw new IllegalArgumentException("初始库存仅允许创建一次");
        }
        Set<Long> allowedIds = allowedDisabledProductIds == null ? Set.of() : allowedDisabledProductIds;
        validateCountRequest(request.items(), tenantId, allowedIds, TYPE_COUNT.equals(type));
        String countNo = ensureCountNo(tenantId, request.countNo(), type);
        ErpStockCount count = new ErpStockCount();
        count.setTenantId(tenantId);
        count.setCountNo(countNo);
        count.setCountType(type);
        count.setStatus(STATUS_DRAFT);
        count.setAdjustmentReason(normalizeAdjustmentReason(request.adjustmentReason(), TYPE_COUNT.equals(type)));
        count.setWarehouseId(request.warehouseId());
        count.setLocationId(request.locationId());
        count.setCountAt(parseInstant(request.countAt()));
        count.setRemark(request.remark());
        count.setCreatedAt(Instant.now());
        count.setCreatedBy(operator);
        count.setUpdatedAt(Instant.now());
        count.setUpdatedBy(operator);
        erpStockCountMapper.insert(count);

        List<ErpStockCountItem> items = buildItems(tenantId, count, request.items(), allowedIds);
        for (ErpStockCountItem item : items) {
            erpStockCountItemMapper.insert(item);
        }
        return new ErpStockCountDetail(count, items);
    }

    @Override
    @Transactional
    public ErpStockInitImportResult importInitStocks(MultipartFile file, String sourceName) {
        return importInitStocks(file, sourceName, null, STRATEGY_MODE_NONE);
    }

    @Override
    public ErpStockInitImportPreview previewInitStockImport(MultipartFile file) {
        String uploadedFileName = file == null ? null : trimToNull(file.getOriginalFilename());
        ExcelImportSheet sheet = parseImportSheet(file, uploadedFileName);
        List<ErpStockInitImportFieldOption> fields = STOCK_INIT_IMPORT_FIELDS.stream()
            .map(field -> new ErpStockInitImportFieldOption(field.key(), field.label(), field.required()))
            .toList();
        List<ErpStockInitImportHeaderMapping> mappings = sheet.headers().stream()
            .map(header -> {
                ImportFieldDefinition field = autoMatchInitImportField(header);
                String sampleValue = sheet.rows().stream()
                    .map(row -> trimToNull(row.get(header)))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
                return new ErpStockInitImportHeaderMapping(
                    header,
                    field == null ? null : field.key(),
                    field == null ? null : field.label(),
                    field == null ? "UNMATCHED" : "AUTO",
                    sampleValue
                );
            })
            .toList();
        return new ErpStockInitImportPreview(
            sheet.headers(),
            fields,
            mappings,
            sheet.rows().stream().limit(3).toList(),
            sheet.rows().size()
        );
    }

    @Override
    @Transactional
    public ErpStockInitImportResult importInitStocks(MultipartFile file, String sourceName, String fieldMapping) {
        return importInitStocks(file, sourceName, fieldMapping, STRATEGY_MODE_NONE);
    }

    @Override
    @Transactional
    public ErpStockInitImportResult importInitStocks(MultipartFile file,
                                                     String sourceName,
                                                     String fieldMapping,
                                                     String strategyMode) {
        Long tenantId = TenantContext.requireTenantId();
        String resolvedStrategyMode = normalizeStrategyMode(strategyMode);
        String uploadedFileName = file == null ? null : trimToNull(file.getOriginalFilename());
        String resolvedSourceName = trimToNull(sourceName);
        if (resolvedSourceName == null) {
            resolvedSourceName = uploadedFileName == null ? "库存明细浏览表" : uploadedFileName;
        }
        byte[] fileBytes = readImportBytes(file);
        Map<String, String> resolvedFieldMapping = parseInitFieldMapping(fieldMapping);
        ExcelImportSheet sheet = applyInitFieldMapping(parseImportSheet(uploadedFileName, fileBytes), resolvedFieldMapping);
        if (sheet.rows().isEmpty()) {
            throw new IllegalArgumentException("导入内容没有有效数据行");
        }

        ErpStockInitImportBatch batch = createStockInitImportBatch(tenantId, uploadedFileName, resolvedSourceName, resolvedStrategyMode, sheet);
        scheduleStockInitImportBatch(batch.getId(), tenantId, uploadedFileName, fileBytes, resolvedSourceName, resolvedFieldMapping, resolvedStrategyMode);
        return new ErpStockInitImportResult(
            batch.getId(),
            batch.getBatchNo(),
            batch.getStatus(),
            null,
            null,
            sheet.rows().size(),
            0,
            0,
            0,
            List.of()
        );
    }

    private StockInitImportProcessResult processStockInitImportBatch(Long batchId,
                                                                      Long tenantId,
                                                                      ExcelImportSheet sheet,
                                                                      String resolvedSourceName,
                                                                      String resolvedStrategyMode) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<ErpStockCountItemRequest> requests = new ArrayList<>();
        List<InitImportPolicyUpdate> policyUpdates = new ArrayList<>();
        Set<Long> allowedDisabledProductIds = new HashSet<>();
        List<ErpStockInitImportItem> importItems = new ArrayList<>();
        Map<String, Integer> stockKeyFirstRowNo = new LinkedHashMap<>();
        int rowNo = 2;
        for (Map<String, String> row : sheet.rows()) {
            List<String> rowWarnings = new ArrayList<>();
            ErpStockInitImportItem importItem = buildStockInitImportItem(batchId, tenantId, rowNo, row);
            try {
                InitImportRowResult rowResult = buildInitImportRowResult(tenantId, rowNo, row, resolvedStrategyMode, rowWarnings);
                String stockKey = buildInitImportStockKey(rowResult.itemRequest());
                Integer firstRowNo = stockKeyFirstRowNo.putIfAbsent(stockKey, rowNo);
                if (firstRowNo != null) {
                    throw new IllegalArgumentException("同一商品、仓库、库位不能重复录入，首次出现在第" + firstRowNo + "行");
                }
                requests.add(rowResult.itemRequest());
                if (Boolean.FALSE.equals(rowResult.product().getEnabled())) {
                    allowedDisabledProductIds.add(rowResult.product().getId());
                }
                if (rowResult.policyUpdate() != null) {
                    policyUpdates.add(rowResult.policyUpdate());
                }
                importItem.setMatchedProductId(rowResult.product().getId());
                importItem.setCountedQty(rowResult.itemRequest().countedQty());
                importItem.setInitUnitCost(rowResult.itemRequest().initUnitCost());
                importItem.setInitTotalAmount(rowResult.itemRequest().initTotalAmount());
                importItem.setStatus("VALIDATED");
                importItem.setWarningMessage(rowWarnings.isEmpty() ? null : String.join("；", rowWarnings));
                importItem.setMatchedStrategy("NAME_MATCH");
                importItem.setNormalizedPayload(valueToTree(Map.of(
                    "productId", rowResult.product().getId(),
                    "warehouseId", rowResult.itemRequest().warehouseId() == null ? "" : rowResult.itemRequest().warehouseId(),
                    "locationId", rowResult.itemRequest().locationId() == null ? "" : rowResult.itemRequest().locationId(),
                    "strategyMode", resolvedStrategyMode
                )));
                warnings.addAll(rowWarnings);
            } catch (IllegalArgumentException ex) {
                errors.add("第" + rowNo + "行：" + ex.getMessage());
                importItem.setStatus("FAILED");
                importItem.setErrorMessage(ex.getMessage());
                importItem.setSuggestion("修正该行后重新导入");
            }
            importItems.add(importItem);
            if (importItems.size() >= IMPORT_CHUNK_SIZE) {
                insertStockInitImportItems(importItems);
                importItems.clear();
            }
            rowNo++;
        }
        insertStockInitImportItems(importItems);
        if (!errors.isEmpty()) {
            return new StockInitImportProcessResult(null, 0, errors.size(), warnings.size(), String.join("；", errors));
        }

        ErpStockCountDetail detail = executeInImportTransaction(() -> {
            String remark = buildInitImportRemark(resolvedSourceName, warnings);
            ErpStockCountDetail created = create(new ErpStockCountCreateRequest(
                null,
                TYPE_INIT,
                null,
                null,
                null,
                null,
                requests,
                remark
            ), TYPE_INIT, allowedDisabledProductIds);
            applyInitImportPolicyUpdates(tenantId, policyUpdates, resolvedStrategyMode);
            markStockInitImportItemsSuccess(tenantId, batchId);
            return created;
        });
        return new StockInitImportProcessResult(detail, requests.size(), 0, warnings.size(), null);
    }

    private String buildInitImportStockKey(ErpStockCountItemRequest request) {
        return request.productId() + "|" + String.valueOf(request.warehouseId()) + "|" + String.valueOf(request.locationId());
    }

    @Override
    public List<ErpStockInitImportBatchSummary> listInitImportBatches() {
        if (erpStockInitImportBatchMapper == null) {
            return List.of();
        }
        Long tenantId = TenantContext.requireTenantId();
        return erpStockInitImportBatchMapper.selectList(new QueryWrapper<ErpStockInitImportBatch>()
                .eq("tenant_id", tenantId)
                .isNull("deleted_at")
                .orderByDesc("id"))
            .stream()
            .map(item -> new ErpStockInitImportBatchSummary(
                item.getId(),
                item.getBatchNo(),
                item.getSourceName(),
                item.getImportMode(),
                item.getStrategyMode(),
                item.getTotalCount(),
                item.getSuccessCount(),
                item.getFailedCount(),
                item.getWarningCount(),
                item.getStatus(),
                item.getSummary(),
                item.getCountId(),
                item.getCountNo(),
                item.getCreatedBy(),
                item.getCreatedAt()
            ))
            .toList();
    }

    @Override
    public List<ErpStockInitImportItemView> listInitImportBatchItems(Long batchId) {
        if (erpStockInitImportBatchMapper == null || erpStockInitImportItemMapper == null) {
            return List.of();
        }
        Long tenantId = TenantContext.requireTenantId();
        ErpStockInitImportBatch batch = erpStockInitImportBatchMapper.selectOne(new QueryWrapper<ErpStockInitImportBatch>()
            .eq("tenant_id", tenantId)
            .eq("id", batchId)
            .isNull("deleted_at"));
        if (batch == null) {
            throw new IllegalArgumentException("导入批次不存在");
        }
        return erpStockInitImportItemMapper.selectList(new QueryWrapper<ErpStockInitImportItem>()
                .eq("tenant_id", tenantId)
                .eq("batch_id", batchId)
                .isNull("deleted_at")
                .orderByAsc("row_no"))
            .stream()
            .map(item -> new ErpStockInitImportItemView(
                item.getId(),
                item.getRowNo(),
                item.getSourceCode(),
                item.getSourceName(),
                item.getMatchedProductId(),
                item.getWarehouseName(),
                item.getLocationName(),
                item.getCountedQty(),
                item.getInitUnitCost(),
                item.getInitTotalAmount(),
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

    @Override
    @Transactional
    @AuditLog(action = "ERP_STOCK_COUNT_UPDATE", entityType = "erp_stock_count", entityId = "{arg0}", detail = "countNo={result.count.countNo}")
    public ErpStockCountDetail update(Long id, ErpStockCountUpdateRequest request, String countType) {
        Long tenantId = TenantContext.requireTenantId();
        ErpStockCount count = requireCountByType(tenantId, id, countType, true);
        if (!STATUS_DRAFT.equals(count.getStatus()) && !STATUS_APPROVE_FAILED.equals(count.getStatus())) {
            throw new IllegalArgumentException("仅草稿或审核失败状态可编辑");
        }
        Set<Long> allowedDisabledProductIds = existingProductIds(erpStockCountItemMapper.selectList(new QueryWrapper<ErpStockCountItem>()
            .eq("tenant_id", tenantId)
            .eq("count_id", id)
            .orderByAsc("line_no")));
        validateCountRequest(request.items(), tenantId, allowedDisabledProductIds, TYPE_COUNT.equals(count.getCountType()));
        count.setAdjustmentReason(normalizeAdjustmentReason(request.adjustmentReason(), TYPE_COUNT.equals(count.getCountType())));
        count.setWarehouseId(request.warehouseId());
        count.setLocationId(request.locationId());
        Instant countAt = parseInstant(request.countAt());
        count.setCountAt(countAt == null ? count.getCountAt() : countAt);
        count.setRemark(request.remark());
        count.setUpdatedAt(Instant.now());
        count.setUpdatedBy(resolveCurrentUsername());
        erpStockCountMapper.updateById(count);

        erpStockCountItemMapper.delete(new QueryWrapper<ErpStockCountItem>()
            .eq("tenant_id", tenantId)
            .eq("count_id", id));
        List<ErpStockCountItem> items = buildItems(tenantId, count, request.items(), allowedDisabledProductIds);
        for (ErpStockCountItem item : items) {
            erpStockCountItemMapper.insert(item);
        }
        return new ErpStockCountDetail(count, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_STOCK_COUNT_APPROVE", entityType = "erp_stock_count", entityId = "{arg0}")
    public void approve(Long id, String countType) {
        Long tenantId = TenantContext.requireTenantId();
        ErpStockCount count = requireCountByType(tenantId, id, countType, true);
        if (TYPE_INIT.equals(normalizeType(countType))) {
            submitInitApproveTask(tenantId, count);
            return;
        }
        if (!STATUS_DRAFT.equals(count.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可审核");
        }
        approveCountSynchronously(tenantId, count);
    }

    private void submitInitApproveTask(Long tenantId, ErpStockCount count) {
        if (!STATUS_DRAFT.equals(count.getStatus()) && !STATUS_APPROVE_FAILED.equals(count.getStatus())) {
            if (STATUS_APPROVING.equals(count.getStatus())) {
                throw new IllegalArgumentException("初始库存正在审核中，请稍后刷新查看");
            }
            throw new IllegalArgumentException("仅草稿或审核失败状态可审核");
        }
        if (isInit(count) && hasOtherApprovedInit(tenantId, count.getId())) {
            throw new IllegalArgumentException("初始库存仅允许创建一次");
        }
        String operator = resolveCurrentUsername();
        count.setStatus(STATUS_APPROVING);
        count.setApprovedBy(null);
        count.setApprovedAt(null);
        count.setUpdatedAt(Instant.now());
        count.setUpdatedBy(operator);
        erpStockCountMapper.updateById(count);
        scheduleStockInitApproval(count.getId(), tenantId);
    }

    private void approveCountSynchronously(Long tenantId, ErpStockCount count) {
        List<ErpStockCountItem> items = erpStockCountItemMapper.selectList(new QueryWrapper<ErpStockCountItem>()
            .eq("tenant_id", tenantId)
            .eq("count_id", count.getId())
            .orderByAsc("line_no"));
        String operator = resolveCurrentUsername();
        for (ErpStockCountItem item : items) {
            BigDecimal systemQty = resolveSystemQty(tenantId, item.getProductId(), item.getWarehouseId(), item.getLocationId());
            BigDecimal countedQty = item.getCountedQty() == null ? BigDecimal.ZERO : item.getCountedQty();
            BigDecimal delta = countedQty.subtract(systemQty);
            item.setSystemQty(systemQty);
            item.setDiffQty(delta);
            item.setUpdatedAt(Instant.now());
            erpStockCountItemMapper.updateById(item);
            if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if (isInit(count)) {
                applyStockDelta(tenantId, count, item, delta, operator, resolveBizType(count), "盘点调整");
            } else {
                applyCountAdjustmentDelta(tenantId, count, item, delta, operator);
            }
        }
        markCountApproved(count, operator);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_STOCK_INIT_RED_FLUSH", entityType = "erp_stock_count", entityId = "{arg0}")
    public void redFlush(Long id, String countType, String reason) {
        Long tenantId = TenantContext.requireTenantId();
        ErpStockCount count = requireCountByType(tenantId, id, countType, false);
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("红冲原因不能为空");
        }
        if (STATUS_RED_FLUSHED.equals(count.getStatus())) {
            return;
        }
        if (STATUS_APPROVING.equals(count.getStatus())) {
            throw new IllegalArgumentException("初始库存审核中，暂不允许红冲");
        }
        if (!STATUS_APPROVED.equals(count.getStatus())) {
            throw new IllegalArgumentException("仅已审核状态可红冲");
        }
        List<ErpStockCountItem> items = erpStockCountItemMapper.selectList(new QueryWrapper<ErpStockCountItem>()
            .eq("tenant_id", tenantId)
            .eq("count_id", id)
            .orderByAsc("line_no"));
        if (hasLegacyInitCostSnapshot(items)) {
            throw new IllegalArgumentException("老的初始库存单不允许红冲");
        }
        if (count.getApprovedAt() == null || erpStockTxnMapper.existsLaterTxnForInit(tenantId, count.getId(), count.getApprovedAt())) {
            throw new IllegalArgumentException("初始库存单已有后续库存业务，不能红冲");
        }
        String operator = resolveCurrentUsername();
        for (ErpStockCountItem item : items) {
            BigDecimal delta = item.getDiffQty();
            if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if (delta.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal unitCost = resolveInitUnitCost(item);
                erpCostService.reverseInboundAverageCost(tenantId, item.getProductId(), delta, unitCost);
            }
            applyStockDelta(
                tenantId,
                count,
                item,
                delta.negate(),
                operator,
                "STOCK_INIT_RED_FLUSH",
                appendRedFlushReason("初始库存红冲", reason),
                resolveInitUnitCost(item)
            );
        }
        count.setStatus(STATUS_RED_FLUSHED);
        count.setCancelledBy(operator);
        count.setCancelledAt(Instant.now());
        count.setRemark(appendRedFlushReason(count.getRemark(), reason));
        count.setUpdatedAt(Instant.now());
        count.setUpdatedBy(operator);
        erpStockCountMapper.updateById(count);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_STOCK_COUNT_CANCEL", entityType = "erp_stock_count", entityId = "{arg0}")
    public void cancel(Long id, String countType) {
        Long tenantId = TenantContext.requireTenantId();
        ErpStockCount count = requireCountByType(tenantId, id, countType, true);
        if (STATUS_CANCELLED.equals(count.getStatus())) {
            return;
        }
        if (STATUS_APPROVING.equals(count.getStatus())) {
            throw new IllegalArgumentException("审核中的单据不可作废");
        }
        if (STATUS_APPROVED.equals(count.getStatus())) {
            throw new IllegalArgumentException("已审核的盘点单不可作废");
        }
        String operator = resolveCurrentUsername();
        count.setStatus(STATUS_CANCELLED);
        count.setCancelledBy(operator);
        count.setCancelledAt(Instant.now());
        count.setUpdatedAt(Instant.now());
        count.setUpdatedBy(operator);
        erpStockCountMapper.updateById(count);
    }

    private ErpStockCountItemRequest buildInitImportItem(Long tenantId,
                                                         int rowNo,
                                                         Map<String, String> row,
                                                         List<String> warnings) {
        return buildInitImportRowResult(tenantId, rowNo, row, STRATEGY_MODE_NONE, warnings).itemRequest();
    }

    private ErpStockInitImportBatch createStockInitImportBatch(Long tenantId,
                                                               String uploadedFileName,
                                                               String sourceName,
                                                               String strategyMode,
                                                               ExcelImportSheet sheet) {
        if (erpStockInitImportBatchMapper == null) {
            throw new IllegalStateException("期初库存导入批次能力未初始化");
        }
        Instant now = Instant.now();
        ErpStockInitImportBatch batch = new ErpStockInitImportBatch();
        batch.setTenantId(tenantId);
        batch.setBatchNo("SII" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault()).format(now));
        batch.setSourceName(sourceName);
        batch.setImportMode("EXCEL_UPLOAD");
        batch.setStrategyMode(strategyMode);
        batch.setTotalCount(sheet.rows().size());
        batch.setSuccessCount(0);
        batch.setFailedCount(0);
        batch.setWarningCount(0);
        batch.setStatus("PROCESSING");
        batch.setSummary("导入任务已创建，正在处理");
        batch.setRawPayload(valueToTree(Map.of(
            "filename", uploadedFileName == null ? "" : uploadedFileName,
            "headers", sheet.headers(),
            "rowCount", sheet.rows().size()
        )));
        batch.setCreatedBy(resolveCurrentUsername());
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);
        erpStockInitImportBatchMapper.insert(batch);
        return batch;
    }

    private ErpStockInitImportItem buildStockInitImportItem(Long batchId,
                                                            Long tenantId,
                                                            int rowNo,
                                                            Map<String, String> row) {
        Instant now = Instant.now();
        ErpStockInitImportItem item = new ErpStockInitImportItem();
        item.setTenantId(tenantId);
        item.setBatchId(batchId);
        item.setRowNo(rowNo);
        item.setSourceCode(trimToNull(firstNonBlank(row, "编码")));
        item.setSourceName(trimToNull(firstNonBlank(row, "产品名称")));
        item.setWarehouseName(trimToNull(firstNonBlank(row, "仓库")));
        item.setLocationName(trimToNull(firstNonBlank(row, "库位")));
        item.setStatus("PENDING");
        item.setRawRow(valueToTree(row));
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return item;
    }

    private void insertStockInitImportItems(List<ErpStockInitImportItem> items) {
        if (erpStockInitImportItemMapper == null || items.isEmpty()) {
            return;
        }
        for (int start = 0; start < items.size(); start += IMPORT_CHUNK_SIZE) {
            int end = Math.min(start + IMPORT_CHUNK_SIZE, items.size());
            erpStockInitImportItemMapper.insertBatch(new ArrayList<>(items.subList(start, end)));
        }
    }

    private void markStockInitImportItemsSuccess(Long tenantId, Long batchId) {
        if (erpStockInitImportItemMapper == null) {
            return;
        }
        erpStockInitImportItemMapper.updateStatusByBatch(tenantId, batchId, "VALIDATED", "SUCCESS");
    }

    private void updateStockInitImportBatchResult(ErpStockInitImportBatch batch,
                                                  ErpStockCountDetail detail,
                                                  int successCount,
                                                  int failedCount,
                                                  int warningCount,
                                                  String errorSummary) {
        if (erpStockInitImportBatchMapper == null || batch == null) {
            return;
        }
        batch.setSuccessCount(successCount);
        batch.setFailedCount(failedCount);
        batch.setWarningCount(warningCount);
        batch.setUpdatedAt(Instant.now());
        if (detail != null) {
            batch.setCountId(detail.count().getId());
            batch.setCountNo(detail.count().getCountNo());
        }
        if (errorSummary != null && !errorSummary.isBlank()) {
            batch.setStatus(successCount > 0 ? "DONE_WITH_ERRORS" : "FAILED");
            batch.setSummary("导入失败：" + errorSummary);
        } else {
            batch.setStatus(failedCount > 0 ? "DONE_WITH_ERRORS" : "DONE");
            batch.setSummary("导入完成：成功 " + successCount + " 行，失败 " + failedCount + " 行，告警 " + warningCount + " 条");
        }
        erpStockInitImportBatchMapper.updateById(batch);
    }

    private void processStockInitImportBatch(Long batchId,
                                             Long tenantId,
                                             String uploadedFileName,
                                             byte[] fileBytes,
                                             String resolvedSourceName,
                                             Map<String, String> fieldMapping,
                                             String strategyMode) {
        TenantContext.setTenantId(tenantId);
        try {
            ExcelImportSheet sheet = applyInitFieldMapping(parseImportSheet(uploadedFileName, fileBytes), fieldMapping);
            StockInitImportProcessResult result = processStockInitImportBatch(batchId, tenantId, sheet, resolvedSourceName, strategyMode);
            executeInImportTransaction(() -> {
                ErpStockInitImportBatch batch = loadStockInitImportBatchForUpdate(tenantId, batchId);
                if (batch != null) {
                    updateStockInitImportBatchResult(
                        batch,
                        result.detail(),
                        result.successCount(),
                        result.failedCount(),
                        result.warningCount(),
                        result.errorSummary()
                    );
                }
                return null;
            });
        } catch (Exception ex) {
            executeInImportTransaction(() -> {
                insertStockInitImportFailureItem(batchId, tenantId, ex);
                ErpStockInitImportBatch batch = loadStockInitImportBatchForUpdate(tenantId, batchId);
                if (batch != null) {
                    updateStockInitImportBatchResult(batch, null, 0, 1, 0, importFailureMessage(ex));
                }
                return null;
            });
        } finally {
            TenantContext.clear();
        }
    }

    private void insertStockInitImportFailureItem(Long batchId, Long tenantId, Exception ex) {
        if (erpStockInitImportItemMapper == null) {
            return;
        }
        Instant now = Instant.now();
        ErpStockInitImportItem item = new ErpStockInitImportItem();
        item.setTenantId(tenantId);
        item.setBatchId(batchId);
        item.setRowNo(0);
        item.setSourceCode("BATCH_FAILURE");
        item.setSourceName("整批导入失败");
        item.setStatus("FAILED");
        item.setErrorMessage(importFailureMessage(ex));
        item.setSuggestion("请检查导入文件、字段映射和库存业务约束后重新导入");
        item.setRawRow(valueToTree(Map.of(
            "type", "BATCH_FAILURE",
            "message", importFailureMessage(ex)
        )));
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        insertStockInitImportItems(List.of(item));
    }

    private String importFailureMessage(Exception ex) {
        Throwable current = ex;
        while (current != null) {
            String message = trimToNull(current.getMessage());
            if (message != null) {
                return message;
            }
            current = current.getCause();
        }
        return "后台导入任务执行失败";
    }

    private void scheduleStockInitImportBatch(Long batchId,
                                              Long tenantId,
                                              String uploadedFileName,
                                              byte[] fileBytes,
                                              String resolvedSourceName,
                                              Map<String, String> fieldMapping,
                                              String strategyMode) {
        Runnable task = () -> processStockInitImportBatch(batchId, tenantId, uploadedFileName, fileBytes, resolvedSourceName, fieldMapping, strategyMode);
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

    private void scheduleStockInitApproval(Long countId, Long tenantId) {
        Runnable task = () -> processStockInitApproval(countId, tenantId);
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

    private void processStockInitApproval(Long countId, Long tenantId) {
        try {
            ErpStockCount current = requireCountByType(tenantId, countId, TYPE_INIT, false);
            if (!STATUS_APPROVING.equals(current.getStatus())) {
                return;
            }
            List<ErpStockCountItem> items = erpStockCountItemMapper.selectList(new QueryWrapper<ErpStockCountItem>()
                .eq("tenant_id", tenantId)
                .eq("count_id", countId)
                .orderByAsc("line_no"));
            String operator = trimToNull(current.getUpdatedBy()) == null ? "system" : current.getUpdatedBy();
            for (int start = 0; start < items.size(); start += APPROVE_CHUNK_SIZE) {
                List<ErpStockCountItem> chunk = items.subList(start, Math.min(start + APPROVE_CHUNK_SIZE, items.size()));
                executeInImportTransaction(() -> {
                    processStockInitApprovalChunk(tenantId, countId, chunk, operator);
                    return null;
                });
            }
            executeInImportTransaction(() -> {
                ErpStockCount fresh = requireCountByType(tenantId, countId, TYPE_INIT, true);
                if (STATUS_APPROVING.equals(fresh.getStatus())) {
                    markCountApproved(fresh, operator);
                }
                return null;
            });
        } catch (Exception ex) {
            executeInImportTransaction(() -> {
                ErpStockCount failed = requireCountByType(tenantId, countId, TYPE_INIT, true);
                failed.setStatus(STATUS_APPROVE_FAILED);
                failed.setUpdatedAt(Instant.now());
                failed.setUpdatedBy(resolveAsyncOperator(failed));
                erpStockCountMapper.updateById(failed);
                return null;
            });
        }
    }

    private void processStockInitApprovalChunk(Long tenantId,
                                               Long countId,
                                               List<ErpStockCountItem> chunk,
                                               String operator) {
        ErpStockCount count = requireCountByType(tenantId, countId, TYPE_INIT, true);
        if (!STATUS_APPROVING.equals(count.getStatus())) {
            return;
        }
        for (ErpStockCountItem item : chunk) {
            BigDecimal systemQty = resolveSystemQty(tenantId, item.getProductId(), item.getWarehouseId(), item.getLocationId());
            BigDecimal countedQty = item.getCountedQty() == null ? BigDecimal.ZERO : item.getCountedQty();
            BigDecimal delta = countedQty.subtract(systemQty);
            item.setSystemQty(systemQty);
            item.setDiffQty(delta);
            item.setUpdatedAt(Instant.now());
            erpStockCountItemMapper.updateById(item);
            if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            applyStockDelta(tenantId, count, item, delta, operator, resolveBizType(count), "盘点调整");
        }
    }

    private ErpStockInitImportBatch loadStockInitImportBatchForUpdate(Long tenantId, Long batchId) {
        if (erpStockInitImportBatchMapper == null) {
            return null;
        }
        return erpStockInitImportBatchMapper.selectOne(new QueryWrapper<ErpStockInitImportBatch>()
            .eq("tenant_id", tenantId)
            .eq("id", batchId)
            .isNull("deleted_at"));
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

    private JsonNode valueToTree(Object value) {
        return objectMapper.valueToTree(value);
    }

    private InitImportRowResult buildInitImportRowResult(Long tenantId,
                                                         int rowNo,
                                                         Map<String, String> row,
                                                         String strategyMode,
                                                         List<String> warnings) {
        String productName = trimToNull(firstNonBlank(row, "产品名称"));
        if (productName == null) {
            throw new IllegalArgumentException("产品名称不能为空");
        }
        List<ErpProduct> matchedProducts = erpProductMapper.selectList(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", tenantId)
            .eq("name", productName)
            .isNull("deleted_at"));
        if (matchedProducts.isEmpty()) {
            throw new IllegalArgumentException("产品名称 " + productName + " 不存在，请先导入配件档案");
        }
        if (matchedProducts.size() > 1) {
            throw new IllegalArgumentException("产品名称 " + productName + " 匹配到多个商品，请先处理商品名称重复");
        }
        ErpProduct product = matchedProducts.get(0);
        if (Boolean.FALSE.equals(product.getEnabled())) {
            warnings.add("商品已停用，按历史库存基线导入");
        }
        String warehouseName = trimToNull(firstNonBlank(row, "仓库"));
        Long warehouseId = resolveImportWarehouseId(tenantId, warehouseName, rowNo, warnings);
        Long locationId = resolveImportLocationId(tenantId, trimToNull(firstNonBlank(row, "库位")), warehouseId, rowNo, warnings);
        warnIfProductFieldDiffers(product.getSpec(), firstNonBlank(row, "规格"), rowNo, "规格", warnings);
        warnIfProductFieldDiffers(product.getBrand(), firstNonBlank(row, "品牌"), rowNo, "品牌", warnings);
        warnIfProductFieldDiffers(product.getManufacturerCode(), firstNonBlank(row, "厂家编码"), rowNo, "厂家编码", warnings);
        warnIfSupplierUnmatched(tenantId, firstNonBlank(row, "来源供应商"), rowNo, warnings);

        BigDecimal countedQty = parseRequiredDecimal(firstNonBlank(row, "库存数"), "库存数");
        BigDecimal unitCost = parseOptionalDecimal(firstNonBlank(row, "库存成本价"));
        BigDecimal totalAmount = parseOptionalDecimal(firstNonBlank(row, "金额"));
        if (unitCost == null && totalAmount == null) {
            throw new IllegalArgumentException("库存成本价或金额至少填写一个");
        }
        if (unitCost == null) {
            unitCost = countedQty.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalAmount.divide(countedQty, 4, RoundingMode.HALF_UP);
        }
        ErpStockCountItemRequest itemRequest = new ErpStockCountItemRequest(
            product.getId(),
            warehouseId,
            locationId,
            countedQty,
            unitCost,
            totalAmount,
            null,
            trimToNull(firstNonBlank(row, "备注"))
        );
        return new InitImportRowResult(
            product,
            itemRequest,
            buildInitImportPolicyUpdate(rowNo, product, warehouseId, row, strategyMode)
        );
    }

    private InitImportPolicyUpdate buildInitImportPolicyUpdate(int rowNo,
                                                               ErpProduct product,
                                                               Long warehouseId,
                                                               Map<String, String> row,
                                                               String strategyMode) {
        if (STRATEGY_MODE_NONE.equals(strategyMode)) {
            return null;
        }
        BigDecimal safetyStock = STRATEGY_MODE_PRODUCT.equals(strategyMode)
            ? parseOptionalDecimal(firstNonBlank(row, "商品安全库存"))
            : parseOptionalDecimal(firstNonBlank(row, "标准库存数"));
        BigDecimal minStock = STRATEGY_MODE_PRODUCT.equals(strategyMode)
            ? parseOptionalDecimal(firstNonBlank(row, "商品最低库存"))
            : parseOptionalDecimal(firstNonBlank(row, "库存下限"));
        BigDecimal maxStock = STRATEGY_MODE_PRODUCT.equals(strategyMode)
            ? parseOptionalDecimal(firstNonBlank(row, "商品最高库存"))
            : parseOptionalDecimal(firstNonBlank(row, "库存上限"));
        if (safetyStock == null && minStock == null && maxStock == null) {
            return null;
        }
        if (minStock != null && maxStock != null && minStock.compareTo(maxStock) > 0) {
            throw new IllegalArgumentException("库存下限不能大于库存上限");
        }
        if (STRATEGY_MODE_WAREHOUSE.equals(strategyMode) && warehouseId == null) {
            throw new IllegalArgumentException("导入仓库层级库存策略时仓库不能为空或未匹配");
        }
        return new InitImportPolicyUpdate(rowNo, product, warehouseId, safetyStock, minStock, maxStock);
    }

    private void applyInitImportPolicyUpdates(Long tenantId,
                                              List<InitImportPolicyUpdate> updates,
                                              String strategyMode) {
        if (updates.isEmpty() || STRATEGY_MODE_NONE.equals(strategyMode)) {
            return;
        }
        if (STRATEGY_MODE_WAREHOUSE.equals(strategyMode)) {
            if (erpProductStockPolicyMapper == null) {
                throw new IllegalStateException("仓库库存策略导入未配置");
            }
            for (InitImportPolicyUpdate update : updates) {
                ErpProductStockPolicy policy = erpProductStockPolicyMapper.selectOne(new QueryWrapper<ErpProductStockPolicy>()
                    .eq("tenant_id", tenantId)
                    .eq("product_id", update.product().getId())
                    .eq("warehouse_id", update.warehouseId())
                    .isNull("deleted_at"));
                boolean created = policy == null;
                if (created) {
                    policy = new ErpProductStockPolicy();
                    policy.setTenantId(tenantId);
                    policy.setProductId(update.product().getId());
                    policy.setWarehouseId(update.warehouseId());
                    policy.setCreatedAt(Instant.now());
                }
                policy.setSafetyStock(update.safetyStock());
                policy.setMinStock(update.minStock());
                policy.setMaxStock(update.maxStock());
                policy.setUpdatedAt(Instant.now());
                if (created) {
                    erpProductStockPolicyMapper.insert(policy);
                } else {
                    erpProductStockPolicyMapper.updateById(policy);
                }
            }
            return;
        }
        for (InitImportPolicyUpdate update : updates) {
            ErpProduct product = update.product();
            product.setSafetyStock(update.safetyStock());
            product.setMinStock(update.minStock());
            product.setMaxStock(update.maxStock());
            product.setUpdatedAt(Instant.now());
            erpProductMapper.updateById(product);
        }
    }

    private Long resolveImportWarehouseId(Long tenantId, String warehouseName, int rowNo, List<String> warnings) {
        if (warehouseName == null) {
            warnings.add("第" + rowNo + "行：仓库为空，已按无仓库导入");
            return null;
        }
        ErpWarehouse warehouse = erpWarehouseMapper.selectOne(new QueryWrapper<ErpWarehouse>()
            .eq("tenant_id", tenantId)
            .eq("name", warehouseName)
            .isNull("deleted_at"));
        if (warehouse == null) {
            warnings.add("第" + rowNo + "行：仓库“" + warehouseName + "”不存在，已按无仓库导入");
            return null;
        }
        return warehouse.getId();
    }

    private Long resolveImportLocationId(Long tenantId,
                                         String locationName,
                                         Long warehouseId,
                                         int rowNo,
                                         List<String> warnings) {
        if (locationName == null) {
            return null;
        }
        ErpLocation location = erpLocationMapper.selectOne(new QueryWrapper<ErpLocation>()
            .eq("tenant_id", tenantId)
            .eq("name", locationName)
            .isNull("deleted_at"));
        if (location == null) {
            warnings.add("第" + rowNo + "行：库位“" + locationName + "”不存在，已按无库位导入");
            return null;
        }
        if (warehouseId != null && !warehouseId.equals(location.getWarehouseId())) {
            warnings.add("第" + rowNo + "行：库位“" + locationName + "”不属于导入仓库，已按无库位导入");
            return null;
        }
        return location.getId();
    }

    private void warnIfSupplierUnmatched(Long tenantId, String supplierName, int rowNo, List<String> warnings) {
        if (supplierName == null) {
            return;
        }
        ErpSupplier supplier = erpSupplierMapper.selectOne(new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", tenantId)
            .eq("name", supplierName)
            .isNull("deleted_at"));
        if (supplier == null) {
            warnings.add("第" + rowNo + "行：来源供应商“" + supplierName + "”未匹配，已忽略");
        }
    }

    private void warnIfProductFieldDiffers(String systemValue, String importedValue, int rowNo, String fieldName, List<String> warnings) {
        String left = trimToNull(systemValue);
        String right = trimToNull(importedValue);
        if (right == null || Objects.equals(left, right)) {
            return;
        }
        warnings.add("第" + rowNo + "行：" + fieldName + "与商品档案不一致，已按现有商品档案为准");
    }

    private String buildInitImportRemark(String sourceName, List<String> warnings) {
        StringBuilder builder = new StringBuilder("Excel导入：").append(sourceName);
        if (!warnings.isEmpty()) {
            builder.append("；告警 ").append(warnings.size()).append(" 条");
        }
        return builder.toString();
    }

    private QueryWrapper<ErpStockCount> baseWrapper(String keyword, String status, String countType) {
        QueryWrapper<ErpStockCount> wrapper = new QueryWrapper<ErpStockCount>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (countType != null) {
            wrapper.eq("count_type", countType);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("count_no", keyword));
        }
        return wrapper;
    }

    private List<ErpStockCountItem> buildItems(Long tenantId,
                                               ErpStockCount count,
                                               List<ErpStockCountItemRequest> requests,
                                               Set<Long> allowedDisabledProductIds) {
        List<ErpStockCountItem> items = new ArrayList<>();
        Set<String> lineKeys = new HashSet<>();
        int lineNo = 1;
        for (ErpStockCountItemRequest request : requests) {
            requireUsableProduct(tenantId, request.productId(), allowedDisabledProductIds);
            if (TYPE_COUNT.equals(count.getCountType())) {
                validateAdjustmentScope(tenantId, request.warehouseId(), request.locationId());
            }
            Long warehouseId = request.warehouseId() != null ? request.warehouseId() : count.getWarehouseId();
            Long locationId = request.locationId() != null ? request.locationId() : count.getLocationId();
            String lineKey = request.productId() + "|" + String.valueOf(warehouseId) + "|" + String.valueOf(locationId);
            if (!lineKeys.add(lineKey)) {
                throw new IllegalArgumentException("同一商品、仓库、库位不能重复录入");
            }
            BigDecimal systemQty = resolveSystemQty(tenantId, request.productId(), warehouseId, locationId);
            BigDecimal countedQty = request.countedQty() == null ? BigDecimal.ZERO : request.countedQty();
            BigDecimal diffQty = countedQty.subtract(systemQty);

            ErpStockCountItem item = new ErpStockCountItem();
            item.setTenantId(tenantId);
            item.setCountId(count.getId());
            item.setLineNo(lineNo++);
            item.setProductId(request.productId());
            item.setWarehouseId(warehouseId);
            item.setLocationId(locationId);
            item.setSystemQty(systemQty);
            item.setCountedQty(countedQty);
            item.setInitUnitCost(resolveInitUnitCost(request));
            item.setInitTotalAmount(resolveInitTotalAmount(request, countedQty, item.getInitUnitCost()));
            item.setDiffQty(diffQty);
            item.setRemark(request.remark());
            item.setCreatedAt(Instant.now());
            item.setUpdatedAt(Instant.now());
            items.add(item);
        }
        return items;
    }

    private BigDecimal resolveSystemQty(Long tenantId, Long productId, Long warehouseId, Long locationId) {
        ErpStockBalance balance = erpStockBalanceMapper.findByKey(tenantId, productId, warehouseId, locationId);
        return balance == null ? BigDecimal.ZERO : balance.getQtyOnHand();
    }

    private void validateCountRequest(List<ErpStockCountItemRequest> requests,
                                      Long tenantId,
                                      Set<Long> allowedDisabledProductIds,
                                      boolean strictAdjustmentValidation) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("盘点明细不能为空");
        }
        for (ErpStockCountItemRequest request : requests) {
            if (request == null || request.productId() == null) {
                throw new IllegalArgumentException("盘点商品不能为空");
            }
            requireUsableProduct(tenantId, request.productId(), allowedDisabledProductIds);
            if (strictAdjustmentValidation) {
                if (request.warehouseId() == null) {
                    throw new IllegalArgumentException("调整仓库不能为空");
                }
                validateAdjustmentScope(tenantId, request.warehouseId(), request.locationId());
            }
            if (request.countedQty() == null) {
                throw new IllegalArgumentException("盘点数量不能为空");
            }
            if (request.countedQty().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("盘点数量不能小于 0");
            }
            if (!strictAdjustmentValidation) {
                if (request.initUnitCost() == null && request.initTotalAmount() == null) {
                    throw new IllegalArgumentException("初始库存必须填写期初单价或期初金额");
                }
                if (request.initUnitCost() != null && request.initUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("期初单价不能小于 0");
                }
                if (request.initTotalAmount() != null && request.initTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("期初金额不能小于 0");
                }
            }
        }
    }

    private String normalizeAdjustmentReason(String reason, boolean required) {
        String normalized = reason == null ? "" : reason.trim().toUpperCase();
        if (!required) {
            return normalized.isEmpty() ? null : normalized;
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("库存调整原因不能为空");
        }
        if (!ADJUSTMENT_REASONS.contains(normalized)) {
            throw new IllegalArgumentException("库存调整原因不合法，可选值为：" + String.join(", ", ADJUSTMENT_REASONS));
        }
        return normalized;
    }

    private void validateAdjustmentScope(Long tenantId, Long warehouseId, Long locationId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("调整仓库不能为空");
        }
        ErpWarehouse warehouse = erpWarehouseMapper.findActiveById(tenantId, warehouseId);
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        if (Boolean.FALSE.equals(warehouse.getEnabled())) {
            throw new IllegalArgumentException("仓库已停用，不能新增引用");
        }
        if (locationId == null) {
            return;
        }
        ErpLocation location = erpLocationMapper.findActiveById(tenantId, locationId);
        if (location == null) {
            throw new IllegalArgumentException("库位不存在");
        }
        if (Boolean.FALSE.equals(location.getEnabled())) {
            throw new IllegalArgumentException("库位已停用，不能新增引用");
        }
        if (!warehouseId.equals(location.getWarehouseId())) {
            throw new IllegalArgumentException("库位不属于所选仓库");
        }
    }

    private ErpProduct requireUsableProduct(Long tenantId, Long productId, Set<Long> allowedDisabledProductIds) {
        ErpProduct product = erpProductMapper.selectOne(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", tenantId)
            .eq("id", productId));
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (Boolean.FALSE.equals(product.getEnabled()) && (allowedDisabledProductIds == null || !allowedDisabledProductIds.contains(productId))) {
            throw new IllegalArgumentException("商品已停用，不能新增引用");
        }
        return product;
    }

    private Set<Long> existingProductIds(List<ErpStockCountItem> items) {
        Set<Long> ids = new HashSet<>();
        if (items == null) {
            return ids;
        }
        for (ErpStockCountItem item : items) {
            if (item != null && item.getProductId() != null) {
                ids.add(item.getProductId());
            }
        }
        return ids;
    }

    private void applyStockDelta(Long tenantId,
                                 ErpStockCount count,
                                 ErpStockCountItem item,
                                 BigDecimal delta,
                                 String operator,
                                 String bizType,
                                 String remark) {
        applyStockDelta(tenantId, count, item, delta, operator, bizType, remark, resolveTxnUnitCost(tenantId, count, item));
    }

    private void applyStockDelta(Long tenantId,
                                 ErpStockCount count,
                                 ErpStockCountItem item,
                                 BigDecimal delta,
                                 String operator,
                                 String bizType,
                                 String remark,
                                 BigDecimal unitCost) {
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            erpCostService.applyInboundAverageCost(tenantId, item.getProductId(), delta, unitCost);
        }
        ErpStockBalance balance = erpStockBalanceMapper.findByKey(tenantId, item.getProductId(), item.getWarehouseId(), item.getLocationId());
        BigDecimal before = balance == null ? BigDecimal.ZERO : balance.getQtyOnHand();
        BigDecimal after = before.add(delta);
        if (balance == null) {
            balance = new ErpStockBalance();
            balance.setTenantId(tenantId);
            balance.setProductId(item.getProductId());
            balance.setWarehouseId(item.getWarehouseId());
            balance.setLocationId(item.getLocationId());
            balance.setQtyOnHand(after);
            balance.setUpdatedBy(operator);
            balance.setUpdatedAt(Instant.now());
            erpStockBalanceMapper.insert(balance);
        } else {
            balance.setQtyOnHand(after);
            balance.setUpdatedBy(operator);
            balance.setUpdatedAt(Instant.now());
            erpStockBalanceMapper.updateById(balance);
        }

        ErpStockTxn txn = new ErpStockTxn();
        txn.setTenantId(tenantId);
        txn.setTxnNo(buildTxnNo(count, item, bizType));
        txn.setBizType(bizType);
        txn.setBizId(count.getId());
        txn.setBizItemId(item.getId());
        txn.setProductId(item.getProductId());
        txn.setWarehouseId(item.getWarehouseId());
        txn.setLocationId(item.getLocationId());
        txn.setQtyDelta(delta);
        txn.setQtyBefore(before);
        txn.setQtyAfter(after);
        BigDecimal totalCost = unitCost.multiply(delta).setScale(4, RoundingMode.HALF_UP);
        txn.setUnitCost(unitCost);
        txn.setTotalCost(totalCost);
        txn.setOperator(operator);
        txn.setOperatorId(null);
        txn.setRemark(remark);
        txn.setCreatedAt(Instant.now());
        erpStockTxnMapper.insert(txn);
    }

    private void applyCountAdjustmentDelta(Long tenantId,
                                           ErpStockCount count,
                                           ErpStockCountItem item,
                                           BigDecimal delta,
                                           String operator) {
        BigDecimal unitCost = getProductCost(tenantId, item.getProductId());
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            erpCostService.applyInboundAverageCost(tenantId, item.getProductId(), delta, unitCost);
        }

        ErpStockBalance balance;
        BigDecimal before;
        BigDecimal after;
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            balance = erpStockBalanceMapper.upsertAddQty(
                tenantId,
                item.getProductId(),
                item.getWarehouseId(),
                item.getLocationId(),
                delta,
                operator
            );
            after = balance == null || balance.getQtyOnHand() == null ? delta : balance.getQtyOnHand();
            before = after.subtract(delta);
        } else {
            balance = erpStockBalanceMapper.addQtyIfEnough(
                tenantId,
                item.getProductId(),
                item.getWarehouseId(),
                item.getLocationId(),
                delta,
                operator
            );
            if (balance == null || balance.getQtyOnHand() == null) {
                throw new IllegalArgumentException("调整后库存不能小于 0");
            }
            after = balance.getQtyOnHand();
            before = after.subtract(delta);
        }

        ErpStockTxn txn = new ErpStockTxn();
        txn.setTenantId(tenantId);
        txn.setTxnNo(buildTxnNo(count, item, "STOCK_COUNT"));
        txn.setBizType("STOCK_COUNT");
        txn.setBizId(count.getId());
        txn.setBizItemId(item.getId());
        txn.setProductId(item.getProductId());
        txn.setWarehouseId(item.getWarehouseId());
        txn.setLocationId(item.getLocationId());
        txn.setQtyDelta(delta);
        txn.setQtyBefore(before);
        txn.setQtyAfter(after);
        BigDecimal totalCost = unitCost.multiply(delta).setScale(4, RoundingMode.HALF_UP);
        txn.setUnitCost(unitCost);
        txn.setTotalCost(totalCost);
        txn.setOperator(operator);
        txn.setOperatorId(null);
        txn.setRemark("库存调整");
        txn.setCreatedAt(Instant.now());
        erpStockTxnMapper.insert(txn);
    }

    private String normalizeType(String countType) {
        if (TYPE_INIT.equalsIgnoreCase(countType)) {
            return TYPE_INIT;
        }
        return TYPE_COUNT;
    }

    private boolean isInit(ErpStockCount count) {
        return TYPE_INIT.equals(count.getCountType());
    }

    private BigDecimal resolveTxnUnitCost(Long tenantId, ErpStockCount count, ErpStockCountItem item) {
        if (count != null && isInit(count)) {
            return resolveInitUnitCost(item);
        }
        return getProductCost(tenantId, item.getProductId());
    }

    private boolean hasActiveInit(Long tenantId) {
        Long count = erpStockCountMapper.selectCount(new QueryWrapper<ErpStockCount>()
            .eq("tenant_id", tenantId)
            .eq("count_type", TYPE_INIT)
            .eq("status", STATUS_APPROVED));
        return count != null && count > 0;
    }

    private boolean hasOtherApprovedInit(Long tenantId, Long currentId) {
        QueryWrapper<ErpStockCount> wrapper = new QueryWrapper<ErpStockCount>()
            .eq("tenant_id", tenantId)
            .eq("count_type", TYPE_INIT)
            .eq("status", STATUS_APPROVED);
        if (currentId != null) {
            wrapper.ne("id", currentId);
        }
        Long count = erpStockCountMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private ErpStockCount requireCountByType(Long tenantId, Long id, String countType, boolean forUpdate) {
        ErpStockCount count = forUpdate
            ? erpStockCountMapper.findByIdForUpdate(tenantId, id)
            : erpStockCountMapper.selectOne(new QueryWrapper<ErpStockCount>()
                .eq("tenant_id", tenantId)
                .eq("id", id));
        if (count == null) {
            throw new IllegalArgumentException("单据不存在");
        }
        String expectedType = normalizeType(countType);
        if (!expectedType.equals(count.getCountType())) {
            throw new IllegalArgumentException(TYPE_INIT.equals(expectedType) ? "初始库存单不存在" : "盘点单不存在");
        }
        return count;
    }

    private void markCountApproved(ErpStockCount count, String operator) {
        count.setStatus(STATUS_APPROVED);
        count.setApprovedBy(operator);
        count.setApprovedAt(Instant.now());
        count.setUpdatedAt(Instant.now());
        count.setUpdatedBy(operator);
        erpStockCountMapper.updateById(count);
    }

    private String resolveAsyncOperator(ErpStockCount count) {
        String operator = count == null ? null : trimToNull(count.getUpdatedBy());
        return operator == null ? "system" : operator;
    }

    private String resolveBizType(ErpStockCount count) {
        return isInit(count) ? "STOCK_INIT" : "STOCK_COUNT";
    }

    private String buildTxnNo(ErpStockCount count, ErpStockCountItem item, String bizType) {
        String base = count.getCountNo() + "-" + item.getLineNo();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return base + "-" + suffix;
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.matches("^\\d+$")) {
            return Instant.ofEpochMilli(Long.parseLong(trimmed));
        }
        if (trimmed.contains("T")) {
            return Instant.parse(trimmed);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return java.time.LocalDateTime.parse(trimmed, formatter)
            .atZone(ZoneId.systemDefault())
            .toInstant();
    }

    private BigDecimal getProductCost(Long tenantId, Long productId) {
        return erpCostService.getProductCost(tenantId, productId);
    }

    private ExcelImportSheet parseImportSheet(MultipartFile file, String uploadedFileName) {
        return parseImportSheet(uploadedFileName, readImportBytes(file));
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

    private Map<String, String> parseInitFieldMapping(String rawFieldMapping) {
        String normalized = trimToNull(rawFieldMapping);
        if (normalized == null) {
            return Map.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<?, ?> raw = objectMapper.readValue(normalized, Map.class);
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                String excelHeader = trimToNull(entry.getKey() == null ? null : String.valueOf(entry.getKey()));
                String fieldKey = trimToNull(entry.getValue() == null ? null : String.valueOf(entry.getValue()));
                if (excelHeader == null || fieldKey == null) {
                    continue;
                }
                if (!STOCK_INIT_IMPORT_FIELD_BY_KEY.containsKey(fieldKey)) {
                    throw new IllegalArgumentException("期初库存导入字段不存在：" + fieldKey);
                }
                result.put(excelHeader, fieldKey);
            }
            return result;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("期初库存导入字段映射格式不正确", ex);
        }
    }

    private ExcelImportSheet applyInitFieldMapping(ExcelImportSheet sheet, Map<String, String> manualMapping) {
        if (sheet == null) {
            return null;
        }
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String header : sheet.headers()) {
            String fieldKey = manualMapping == null ? null : trimToNull(manualMapping.get(header));
            ImportFieldDefinition autoField = fieldKey == null ? autoMatchInitImportField(header) : null;
            ImportFieldDefinition field = fieldKey == null ? autoField : STOCK_INIT_IMPORT_FIELD_BY_KEY.get(fieldKey);
            if (field != null) {
                mapping.put(header, field.key());
            }
        }
        List<Map<String, String>> rows = sheet.rows().stream()
            .map(row -> normalizeInitImportRow(row, mapping))
            .toList();
        return new ExcelImportSheet(sheet.headers(), rows);
    }

    private Map<String, String> normalizeInitImportRow(Map<String, String> row, Map<String, String> mapping) {
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            ImportFieldDefinition field = STOCK_INIT_IMPORT_FIELD_BY_KEY.get(entry.getValue());
            if (field != null) {
                normalized.put(field.primaryHeader(), row.get(entry.getKey()));
            }
        }
        return normalized;
    }

    private ImportFieldDefinition autoMatchInitImportField(String header) {
        return STOCK_INIT_IMPORT_FIELD_BY_ALIAS.get(normalizeImportHeader(header));
    }

    private static ImportFieldDefinition field(String key, String label, boolean required, String... aliases) {
        return new ImportFieldDefinition(key, label, required, List.of(aliases));
    }

    private static String normalizeImportHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.replaceAll("[\\s_\\-（）()【】\\[\\]：:]+", "").toLowerCase(java.util.Locale.ROOT);
    }

    private String ensureCountNo(Long tenantId, String provided, String countType) {
        String trimmed = provided == null ? "" : provided.trim();
        if (!trimmed.isEmpty()) {
            ErpStockCount existing = erpStockCountMapper.selectOne(new QueryWrapper<ErpStockCount>()
                .eq("tenant_id", tenantId)
                .eq("count_no", trimmed));
            if (existing != null) {
                throw new IllegalArgumentException("盘点单号已存在");
            }
            return trimmed;
        }
        String prefixKey = isInitType(countType) ? "erp.order.no.stock-init.prefix" : "erp.order.no.stock-count.prefix";
        String defaultPrefix = isInitType(countType) ? "SI" : "SC";
        String prefix = readConfig(prefixKey, defaultPrefix);
        String dateFormat = readConfig("erp.order.no.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.order.no.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        String orderType = isInitType(countType) ? "STOCK_INIT" : "STOCK_COUNT";
        erpOrderSequenceMapper.insertIgnore(tenantId, orderType, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, orderType, dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private boolean isInitType(String countType) {
        return TYPE_INIT.equalsIgnoreCase(countType);
    }

    private String readConfig(String key, String fallback) {
        SystemConfig config = systemConfigMapper.findByKey(TenantContext.requireTenantId(), key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return fallback;
        }
        return config.getConfigValue().trim();
    }

    private BigDecimal resolveInitUnitCost(ErpStockCountItemRequest request) {
        if (request.initUnitCost() != null) {
            return request.initUnitCost().setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal qty = request.countedQty() == null ? BigDecimal.ZERO : request.countedQty();
        BigDecimal amount = request.initTotalAmount() == null ? BigDecimal.ZERO : request.initTotalAmount();
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return amount.divide(qty, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveInitUnitCost(ErpStockCountItem item) {
        if (item == null || item.getInitUnitCost() == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return item.getInitUnitCost().setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveInitTotalAmount(ErpStockCountItemRequest request, BigDecimal countedQty, BigDecimal unitCost) {
        if (request.initTotalAmount() != null) {
            return request.initTotalAmount().setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal qty = countedQty == null ? BigDecimal.ZERO : countedQty;
        return unitCost.multiply(qty).setScale(4, RoundingMode.HALF_UP);
    }

    private boolean hasLegacyInitCostSnapshot(List<ErpStockCountItem> items) {
        return items.stream().anyMatch(item -> item.getInitUnitCost() == null || item.getInitTotalAmount() == null);
    }

    private String appendRedFlushReason(String remark, String reason) {
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty()) {
            return remark;
        }
        String marker = "红冲原因：";
        String base = remark == null ? "" : remark.trim();
        String append = marker + trimmed;
        if (base.isEmpty()) {
            return append;
        }
        if (base.contains(marker)) {
            return base;
        }
        return base + "；" + append;
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

    private BigDecimal parseRequiredDecimal(String value, String fieldName) {
        BigDecimal parsed = parseOptionalDecimal(value);
        if (parsed == null) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return parsed;
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

    private String normalizeStrategyMode(String strategyMode) {
        String normalized = trimToNull(strategyMode);
        if (normalized == null) {
            return STRATEGY_MODE_NONE;
        }
        String upper = normalized.toUpperCase(java.util.Locale.ROOT);
        if ("WAREHOUSE_POLICY".equals(upper) || "WAREHOUSE_LEVEL".equals(upper)) {
            return STRATEGY_MODE_WAREHOUSE;
        }
        if ("PRODUCT_POLICY".equals(upper) || "PRODUCT_LEVEL".equals(upper)) {
            return STRATEGY_MODE_PRODUCT;
        }
        if (STRATEGY_MODE_NONE.equals(upper)
            || STRATEGY_MODE_PRODUCT.equals(upper)
            || STRATEGY_MODE_WAREHOUSE.equals(upper)) {
            return upper;
        }
        throw new IllegalArgumentException("库存策略导入模式不正确：" + strategyMode);
    }

    private int readIntConfig(String key, int fallback) {
        String value = readConfig(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }

    private record ImportFieldDefinition(String key,
                                         String label,
                                         boolean required,
                                         List<String> aliases) {
        private String primaryHeader() {
            return aliases.get(0);
        }
    }

    private record InitImportRowResult(ErpProduct product,
                                       ErpStockCountItemRequest itemRequest,
                                       InitImportPolicyUpdate policyUpdate) {
    }

    private record StockInitImportProcessResult(ErpStockCountDetail detail,
                                                int successCount,
                                                int failedCount,
                                                int warningCount,
                                                String errorSummary) {
    }

    private record InitImportPolicyUpdate(int rowNo,
                                          ErpProduct product,
                                          Long warehouseId,
                                          BigDecimal safetyStock,
                                          BigDecimal minStock,
                                          BigDecimal maxStock) {
    }
}
