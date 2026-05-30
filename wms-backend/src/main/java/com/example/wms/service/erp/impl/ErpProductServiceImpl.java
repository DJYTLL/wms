package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpProductImportBatchSummary;
import com.example.wms.dto.erp.ErpProductCreateRequest;
import com.example.wms.dto.erp.ErpProductImportItemView;
import com.example.wms.dto.erp.ErpProductImportResult;
import com.example.wms.dto.erp.ErpProductPriceItemRequest;
import com.example.wms.dto.erp.ErpProductUpdateRequest;
import com.example.wms.entity.erp.ErpCategory;
import com.example.wms.entity.erp.ErpCustomerCategory;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpProductImportBatch;
import com.example.wms.entity.erp.ErpProductImportItem;
import com.example.wms.entity.erp.ErpProductPrice;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.entity.erp.ErpUnit;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.entity.SystemConfig;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpCategoryMapper;
import com.example.wms.mapper.erp.ErpCustomerCategoryMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductImportBatchMapper;
import com.example.wms.mapper.erp.ErpProductImportItemMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpProductPriceMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.mapper.erp.ErpUnitMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.ErpProductService;
import com.example.wms.service.erp.support.ExcelImportParser;
import com.example.wms.service.erp.support.ExcelImportSheet;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

// 商品服务实现（ERP进销存）
@Service
public class ErpProductServiceImpl implements ErpProductService {
    private static final String PRODUCT_TYPE_NORMAL = "NORMAL";
    private static final String PRODUCT_TYPE_ASSEMBLY = "ASSEMBLY";
    private static final String RETAIL_CUSTOMER_CATEGORY_CODE = "CUST-RETAIL";
    private static final String WHOLESALE_CUSTOMER_CATEGORY_CODE = "CUST-WHOLE";

    private static final String PRODUCT_CODE_TYPE = "PRODUCT";
    private static final int IMPORT_CHUNK_SIZE = 100;

    private final ErpProductMapper erpProductMapper;
    private final ErpProductPriceMapper erpProductPriceMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ErpCategoryMapper erpCategoryMapper;
    private final ErpUnitMapper erpUnitMapper;
    private final ErpWarehouseMapper erpWarehouseMapper;
    private final ErpLocationMapper erpLocationMapper;
    private final ErpCustomerCategoryMapper erpCustomerCategoryMapper;
    private final ErpSupplierMapper erpSupplierMapper;
    private final ObjectMapper objectMapper;
    private final ErpProductImportBatchMapper erpProductImportBatchMapper;
    private final ErpProductImportItemMapper erpProductImportItemMapper;
    private final ExcelImportParser excelImportParser;
    private final Executor importExecutor;
    private final TransactionOperations transactionOperations;

    @Autowired
    public ErpProductServiceImpl(ErpProductMapper erpProductMapper,
                                 ErpProductPriceMapper erpProductPriceMapper,
                                 ErpOrderSequenceMapper erpOrderSequenceMapper,
                                 SystemConfigMapper systemConfigMapper,
                                 ErpCategoryMapper erpCategoryMapper,
                                 ErpUnitMapper erpUnitMapper,
                                 ErpWarehouseMapper erpWarehouseMapper,
                                 ErpLocationMapper erpLocationMapper,
                                 ErpCustomerCategoryMapper erpCustomerCategoryMapper,
                                 ErpSupplierMapper erpSupplierMapper,
                                 ObjectMapper objectMapper,
                                 ErpProductImportBatchMapper erpProductImportBatchMapper,
                                 ErpProductImportItemMapper erpProductImportItemMapper,
                                 ExcelImportParser excelImportParser,
                                 @Qualifier("erpImportTaskExecutor") Executor importExecutor,
                                 PlatformTransactionManager transactionManager) {
        this(
            erpProductMapper,
            erpProductPriceMapper,
            erpOrderSequenceMapper,
            systemConfigMapper,
            erpCategoryMapper,
            erpUnitMapper,
            erpWarehouseMapper,
            erpLocationMapper,
            erpCustomerCategoryMapper,
            erpSupplierMapper,
            objectMapper,
            erpProductImportBatchMapper,
            erpProductImportItemMapper,
            excelImportParser,
            importExecutor,
            new TransactionTemplate(transactionManager)
        );
    }

    public ErpProductServiceImpl(ErpProductMapper erpProductMapper,
                                 ErpProductPriceMapper erpProductPriceMapper,
                                 ErpOrderSequenceMapper erpOrderSequenceMapper,
                                 SystemConfigMapper systemConfigMapper,
                                 ErpCategoryMapper erpCategoryMapper,
                                 ErpUnitMapper erpUnitMapper,
                                 ErpWarehouseMapper erpWarehouseMapper,
                                 ErpLocationMapper erpLocationMapper,
                                 ErpCustomerCategoryMapper erpCustomerCategoryMapper,
                                 ErpSupplierMapper erpSupplierMapper,
                                 ObjectMapper objectMapper,
                                 ErpProductImportBatchMapper erpProductImportBatchMapper,
                                 ErpProductImportItemMapper erpProductImportItemMapper,
                                 ExcelImportParser excelImportParser) {
        this(
            erpProductMapper,
            erpProductPriceMapper,
            erpOrderSequenceMapper,
            systemConfigMapper,
            erpCategoryMapper,
            erpUnitMapper,
            erpWarehouseMapper,
            erpLocationMapper,
            erpCustomerCategoryMapper,
            erpSupplierMapper,
            objectMapper,
            erpProductImportBatchMapper,
            erpProductImportItemMapper,
            excelImportParser,
            Runnable::run,
            (TransactionOperations) null
        );
    }

    public ErpProductServiceImpl(ErpProductMapper erpProductMapper,
                                 ErpProductPriceMapper erpProductPriceMapper,
                                 ErpOrderSequenceMapper erpOrderSequenceMapper,
                                 SystemConfigMapper systemConfigMapper,
                                 ErpCategoryMapper erpCategoryMapper,
                                 ErpUnitMapper erpUnitMapper,
                                 ErpWarehouseMapper erpWarehouseMapper,
                                 ErpLocationMapper erpLocationMapper,
                                 ErpCustomerCategoryMapper erpCustomerCategoryMapper,
                                 ErpSupplierMapper erpSupplierMapper,
                                 ObjectMapper objectMapper,
                                 ErpProductImportBatchMapper erpProductImportBatchMapper,
                                 ErpProductImportItemMapper erpProductImportItemMapper,
                                 ExcelImportParser excelImportParser,
                                 Executor importExecutor,
                                 TransactionOperations transactionOperations) {
        this.erpProductMapper = erpProductMapper;
        this.erpProductPriceMapper = erpProductPriceMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.erpCategoryMapper = erpCategoryMapper;
        this.erpUnitMapper = erpUnitMapper;
        this.erpWarehouseMapper = erpWarehouseMapper;
        this.erpLocationMapper = erpLocationMapper;
        this.erpCustomerCategoryMapper = erpCustomerCategoryMapper;
        this.erpSupplierMapper = erpSupplierMapper;
        this.objectMapper = objectMapper;
        this.erpProductImportBatchMapper = erpProductImportBatchMapper;
        this.erpProductImportItemMapper = erpProductImportItemMapper;
        this.excelImportParser = excelImportParser;
        this.importExecutor = importExecutor == null ? Runnable::run : importExecutor;
        this.transactionOperations = transactionOperations;
    }

    public ErpProductServiceImpl(ErpProductMapper erpProductMapper,
                                 ErpProductPriceMapper erpProductPriceMapper,
                                 ErpOrderSequenceMapper erpOrderSequenceMapper,
                                 SystemConfigMapper systemConfigMapper,
                                 ErpCategoryMapper erpCategoryMapper,
                                 ErpUnitMapper erpUnitMapper,
                                 ErpWarehouseMapper erpWarehouseMapper,
                                 ErpLocationMapper erpLocationMapper,
                                 ErpCustomerCategoryMapper erpCustomerCategoryMapper,
                                 ErpSupplierMapper erpSupplierMapper,
                                 ObjectMapper objectMapper) {
        this(
            erpProductMapper,
            erpProductPriceMapper,
            erpOrderSequenceMapper,
            systemConfigMapper,
            erpCategoryMapper,
            erpUnitMapper,
            erpWarehouseMapper,
            erpLocationMapper,
            erpCustomerCategoryMapper,
            erpSupplierMapper,
            objectMapper,
            null,
            null,
            new ExcelImportParser()
        );
    }

    @Override
    public List<ErpProduct> listAll(String keyword, Boolean enabled, Long categoryId) {
        QueryWrapper<ErpProduct> wrapper = baseWrapper(keyword, enabled, categoryId);
        wrapper.orderByAsc("id");
        return erpProductMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpProduct> page(long page, long size, String keyword, Boolean enabled, Long categoryId) {
        Page<ErpProduct> pageReq = Page.of(page, size);
        QueryWrapper<ErpProduct> wrapper = baseWrapper(keyword, enabled, categoryId);
        wrapper.orderByAsc("id");
        Page<ErpProduct> result = erpProductMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpProduct getById(Long id) {
        ErpProduct product = erpProductMapper.selectOne(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        return product;
    }

    @Override
    public String nextCode() {
        Long tenantId = TenantContext.requireTenantId();
        return generateProductCode(tenantId);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_PRODUCT_CREATE", entityType = "erp_product", entityId = "{result.id}", detail = "code={arg0.code}")
    public ErpProduct create(ErpProductCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpProduct existing = erpProductMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("商品编码已存在");
        }
        validateAssociations(tenantId, request.categoryId(), request.unitId(), request.defaultWarehouseId(), request.defaultLocationId(), request.sourceSupplierId(), request.priceItems());
        ErpProduct product = new ErpProduct();
        product.setTenantId(tenantId);
        applyRequest(product, request);
        product.setEnabled(request.enabled() == null || request.enabled());
        product.setCreatedAt(Instant.now());
        product.setUpdatedAt(Instant.now());
        erpProductMapper.insert(product);
        saveProductPrices(tenantId, product.getId(), request.priceItems());
        return product;
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_PRODUCT_UPDATE", entityType = "erp_product", entityId = "{arg0}", detail = "code={arg1.code}")
    public ErpProduct update(Long id, ErpProductUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpProduct product = erpProductMapper.selectOne(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        ErpProduct existing = erpProductMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("商品编码已存在");
        }
        validateAssociations(tenantId, request.categoryId(), request.unitId(), request.defaultWarehouseId(), request.defaultLocationId(), request.sourceSupplierId(), request.priceItems());
        applyRequest(product, request);
        if (request.enabled() != null) {
            product.setEnabled(request.enabled());
        }
        product.setUpdatedAt(Instant.now());
        erpProductMapper.updateById(product);
        saveProductPrices(tenantId, product.getId(), request.priceItems());
        return product;
    }

    @Override
    @AuditLog(action = "ERP_PRODUCT_DELETE", entityType = "erp_product", entityId = "{arg0}")
@Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpProduct product = erpProductMapper.selectOne(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        erpProductMapper.deleteById(id);
    }

    @Override
    @Transactional
    public ErpProductImportResult importProducts(MultipartFile file, String sourceName) {
        Long tenantId = TenantContext.requireTenantId();
        String uploadedFileName = file == null ? null : trimToNull(file.getOriginalFilename());
        String resolvedSourceName = trimToNull(sourceName);
        if (resolvedSourceName == null) {
            resolvedSourceName = uploadedFileName == null ? "配件档案列表" : uploadedFileName;
        }
        final String finalSourceName = resolvedSourceName;
        byte[] fileBytes = readImportBytes(file);
        ExcelImportSheet sheet = parseImportSheet(uploadedFileName, fileBytes);
        if (sheet.rows().isEmpty()) {
            throw new IllegalArgumentException("导入内容没有有效数据行");
        }
        ErpProductImportBatch batch = createImportBatch(tenantId, uploadedFileName, finalSourceName, sheet);
        scheduleImportBatch(batch.getId(), tenantId, uploadedFileName, fileBytes, finalSourceName);
        return new ErpProductImportResult(batch.getId(), batch.getBatchNo(), batch.getStatus(), batch.getTotalCount(), 0, 0);
    }

    @Override
    public List<ErpProductImportBatchSummary> listImportBatches() {
        if (erpProductImportBatchMapper == null) {
            return List.of();
        }
        Long tenantId = TenantContext.requireTenantId();
        return erpProductImportBatchMapper.selectList(new QueryWrapper<ErpProductImportBatch>()
                .eq("tenant_id", tenantId)
                .isNull("deleted_at")
                .orderByDesc("id"))
            .stream()
            .map(item -> new ErpProductImportBatchSummary(
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
    public List<ErpProductImportItemView> listImportBatchItems(Long batchId) {
        if (erpProductImportBatchMapper == null || erpProductImportItemMapper == null) {
            return List.of();
        }
        Long tenantId = TenantContext.requireTenantId();
        ErpProductImportBatch batch = erpProductImportBatchMapper.selectOne(new QueryWrapper<ErpProductImportBatch>()
            .eq("tenant_id", tenantId)
            .eq("id", batchId)
            .isNull("deleted_at"));
        if (batch == null) {
            throw new IllegalArgumentException("导入批次不存在");
        }
        return erpProductImportItemMapper.selectList(new QueryWrapper<ErpProductImportItem>()
                .eq("tenant_id", tenantId)
                .eq("batch_id", batchId)
                .isNull("deleted_at")
                .orderByAsc("row_no"))
            .stream()
            .map(item -> new ErpProductImportItemView(
                item.getId(),
                item.getRowNo(),
                item.getSourceCode(),
                item.getSourceName(),
                item.getMatchedProductId(),
                item.getCategoryName(),
                item.getUnitName(),
                item.getWarehouseName(),
                item.getSupplierName(),
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

    private ImportUpsertOutcome upsertImportedProduct(Long tenantId,
                                                      int rowNo,
                                                      Map<String, String> row,
                                                      String sourceName) {
        return upsertImportedProduct(tenantId, rowNo, row, sourceName, null);
    }

    private ImportUpsertOutcome upsertImportedProduct(Long tenantId,
                                                      int rowNo,
                                                      Map<String, String> row,
                                                      String sourceName,
                                                      ErpProduct existingProduct) {
        String code = trimToNull(firstNonBlank(row, "编码"));
        String name = trimToNull(firstNonBlank(row, "配件名称", "名称"));
        if (code == null) {
            throw new IllegalArgumentException("编码不能为空");
        }
        if (name == null) {
            throw new IllegalArgumentException("配件名称不能为空");
        }
        List<String> warnings = new ArrayList<>();
        Long categoryId = resolveCategoryId(tenantId, trimToNull(firstNonBlank(row, "类别")), rowNo, warnings);
        Long unitId = resolveUnitId(tenantId, trimToNull(firstNonBlank(row, "单位")));
        if (unitId == null) {
            throw new IllegalArgumentException("单位未匹配");
        }
        Long warehouseId = resolveWarehouseIdByName(tenantId, trimToNull(firstNonBlank(row, "默认仓库")), rowNo, warnings);
        Long supplierId = resolveSupplierIdByName(tenantId, trimToNull(firstNonBlank(row, "供应商名称", "来源供应商")), rowNo, warnings);

        ErpProduct product = existingProduct == null ? erpProductMapper.findByCode(tenantId, code) : existingProduct;
        boolean created = product == null;
        if (product == null) {
            product = new ErpProduct();
            product.setTenantId(tenantId);
            product.setCode(code);
            product.setCreatedAt(Instant.now());
        }
        product.setName(name);
        product.setShortName(name);
        product.setProductType(PRODUCT_TYPE_NORMAL);
        product.setSpec(trimToNull(firstNonBlank(row, "规格")));
        product.setModel(trimToNull(firstNonBlank(row, "特征码", "图号")));
        product.setCategoryId(categoryId);
        product.setUnitId(unitId);
        product.setDefaultWarehouseId(warehouseId);
        product.setBarcode(trimToNull(firstNonBlank(row, "条形码")));
        product.setBrand(trimToNull(firstNonBlank(row, "品牌")));
        product.setOrigin(trimToNull(firstNonBlank(row, "产地")));
        product.setManufacturerCode(trimToNull(firstNonBlank(row, "厂家编码")));
        product.setManufacturerModel(trimToNull(firstNonBlank(row, "厂家型号")));
        product.setManufacturerName(trimToNull(firstNonBlank(row, "厂家名称")));
        product.setSourceSupplierId(supplierId);
        product.setWeight(parseOptionalDecimal(firstNonBlank(row, "重量")));
        product.setVolume(parseOptionalDecimal(firstNonBlank(row, "体积")));
        BigDecimal referencePrice = parseOptionalDecimal(firstNonBlank(row, "参考价", "最后一次采购入库价格"));
        BigDecimal backupPrice1 = parseOptionalDecimal(firstNonBlank(row, "备用价1"));
        BigDecimal retailPrice = parseOptionalDecimal(firstNonBlank(row, "零售价"));
        BigDecimal wholesalePrice = parseOptionalDecimal(firstNonBlank(row, "批发价"));
        product.setCostPrice(referencePrice);
        product.setSalePrice(retailPrice);
        applyImportedBackupPrice(product, backupPrice1);
        product.setSafetyStock(parseOptionalDecimal(firstNonBlank(row, "标准库存", "标准库存数")));
        product.setMinStock(parseOptionalDecimal(firstNonBlank(row, "库存下限")));
        product.setMaxStock(parseOptionalDecimal(firstNonBlank(row, "库存上限")));
        product.setEnabled(parseEnabled(firstNonBlank(row, "状态")));
        product.setRemark(mergeImportRemark(trimToNull(firstNonBlank(row, "备注")), sourceName));
        product.setUpdatedAt(Instant.now());
        if (product.getId() == null) {
            erpProductMapper.insert(product);
        } else {
            erpProductMapper.updateById(product);
        }
        List<Map<String, Object>> priceCategoryUpdates = saveImportedCustomerCategoryPrices(
            tenantId,
            product.getId(),
            rowNo,
            retailPrice,
            wholesalePrice,
            warnings
        );
        if (!created) {
            warnings.add("第" + rowNo + "行：商品编码 " + code + " 已存在，已按编码更新");
        }
        Map<String, Object> normalizedPayload = new HashMap<>();
        normalizedPayload.put("categoryId", categoryId);
        normalizedPayload.put("unitId", unitId);
        normalizedPayload.put("warehouseId", warehouseId);
        normalizedPayload.put("supplierId", supplierId);
        normalizedPayload.put("referencePrice", referencePrice);
        normalizedPayload.put("backupPrice1", backupPrice1);
        normalizedPayload.put("retailPrice", retailPrice);
        normalizedPayload.put("wholesalePrice", wholesalePrice);
        normalizedPayload.put("priceCategoryUpdates", priceCategoryUpdates);
        return new ImportUpsertOutcome(product, created, warnings.isEmpty() ? null : String.join("；", warnings), valueToTree(normalizedPayload));
    }

    private Long resolveCategoryId(Long tenantId, String categoryName, int rowNo, List<String> warnings) {
        if (categoryName != null) {
            ErpCategory category = erpCategoryMapper.selectOne(new QueryWrapper<ErpCategory>()
                .eq("tenant_id", tenantId)
                .eq("name", categoryName)
                .isNull("deleted_at"));
            if (category != null) {
                return category.getId();
            }
        }
        ErpCategory defaultCategory = resolveDefaultCategory(tenantId);
        if (defaultCategory == null) {
            throw new IllegalArgumentException("类别未匹配，且默认类别不存在");
        }
        String defaultName = trimToNull(defaultCategory.getName());
        String suffix = defaultName == null ? "" : "“" + defaultName + "”";
        if (categoryName == null) {
            warnings.add("第" + rowNo + "行：类别未填写，已使用默认类别" + suffix);
        } else {
            warnings.add("第" + rowNo + "行：类别“" + categoryName + "”未匹配，已使用默认类别" + suffix);
        }
        return defaultCategory.getId();
    }

    private ErpCategory resolveDefaultCategory(Long tenantId) {
        ErpCategory defaultCategory = erpCategoryMapper.findDefault(tenantId);
        if (defaultCategory != null && Boolean.TRUE.equals(defaultCategory.getEnabled())) {
            return defaultCategory;
        }
        return erpCategoryMapper.selectOne(new QueryWrapper<ErpCategory>()
            .eq("tenant_id", tenantId)
            .eq("is_enabled", true)
            .isNull("parent_id")
            .isNull("deleted_at")
            .orderByAsc("sort_no", "id")
            .last("LIMIT 1"));
    }

    private Long resolveUnitId(Long tenantId, String unitName) {
        if (unitName == null) {
            return null;
        }
        ErpUnit unit = erpUnitMapper.selectOne(new QueryWrapper<ErpUnit>()
            .eq("tenant_id", tenantId)
            .eq("name", unitName));
        return unit == null ? null : unit.getId();
    }

    private Long resolveWarehouseIdByName(Long tenantId, String warehouseName, int rowNo, List<String> warnings) {
        if (warehouseName == null) {
            return null;
        }
        ErpWarehouse warehouse = erpWarehouseMapper.selectOne(new QueryWrapper<ErpWarehouse>()
            .eq("tenant_id", tenantId)
            .eq("name", warehouseName)
            .isNull("deleted_at"));
        if (warehouse == null) {
            warnings.add("第" + rowNo + "行：默认仓库“" + warehouseName + "”未匹配，已忽略");
            return null;
        }
        return warehouse.getId();
    }

    private Long resolveSupplierIdByName(Long tenantId, String supplierName, int rowNo, List<String> warnings) {
        if (supplierName == null) {
            return null;
        }
        ErpSupplier supplier = erpSupplierMapper.selectOne(new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", tenantId)
            .eq("name", supplierName)
            .isNull("deleted_at"));
        if (supplier == null) {
            warnings.add("第" + rowNo + "行：来源供应商“" + supplierName + "”未匹配，已忽略");
            return null;
        }
        return supplier.getId();
    }

    private void applyImportedBackupPrice(ErpProduct product, BigDecimal backupPrice1) {
        if (backupPrice1 == null) {
            return;
        }
        ObjectNode extAttrs = product.getExtAttrs() instanceof ObjectNode objectNode
            ? objectNode.deepCopy()
            : JsonNodeFactory.instance.objectNode();
        extAttrs.put("backupPrice1", backupPrice1);
        product.setExtAttrs(extAttrs);
    }

    private List<Map<String, Object>> saveImportedCustomerCategoryPrices(Long tenantId,
                                                                         Long productId,
                                                                         int rowNo,
                                                                         BigDecimal retailPrice,
                                                                         BigDecimal wholesalePrice,
                                                                         List<String> warnings) {
        List<Map<String, Object>> updates = new ArrayList<>();
        saveImportedCustomerCategoryPrice(tenantId, productId, rowNo, RETAIL_CUSTOMER_CATEGORY_CODE, "零售价", retailPrice, warnings, updates);
        saveImportedCustomerCategoryPrice(tenantId, productId, rowNo, WHOLESALE_CUSTOMER_CATEGORY_CODE, "批发价", wholesalePrice, warnings, updates);
        return updates;
    }

    private void saveImportedCustomerCategoryPrice(Long tenantId,
                                                   Long productId,
                                                   int rowNo,
                                                   String customerCategoryCode,
                                                   String sourceColumn,
                                                   BigDecimal salePrice,
                                                   List<String> warnings,
                                                   List<Map<String, Object>> updates) {
        if (salePrice == null) {
            return;
        }
        ErpCustomerCategory category = erpCustomerCategoryMapper.findByCode(tenantId, customerCategoryCode);
        if (category == null) {
            warnings.add("第" + rowNo + "行：" + sourceColumn + "未写入，客户类别“" + customerCategoryCode + "”不存在");
            return;
        }
        erpProductPriceMapper.upsertActivePrice(tenantId, productId, category.getId(), salePrice);
        updates.add(Map.of(
            "sourceColumn", sourceColumn,
            "customerCategoryCode", customerCategoryCode,
            "customerCategoryId", category.getId(),
            "salePrice", salePrice
        ));
    }

    private QueryWrapper<ErpProduct> baseWrapper(String keyword, Boolean enabled, Long categoryId) {
        QueryWrapper<ErpProduct> wrapper = new QueryWrapper<ErpProduct>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword)
                .or()
                .like("short_name", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        return wrapper;
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

    private ErpProductImportBatch createImportBatch(Long tenantId,
                                                    String uploadedFileName,
                                                    String sourceName,
                                                    ExcelImportSheet sheet) {
        if (erpProductImportBatchMapper == null) {
            throw new IllegalStateException("商品导入批次能力未初始化");
        }
        Instant now = Instant.now();
        ErpProductImportBatch batch = new ErpProductImportBatch();
        batch.setTenantId(tenantId);
        batch.setBatchNo("PI" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault()).format(now));
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
        erpProductImportBatchMapper.insert(batch);
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
            int successCount = 0;
            int failedCount = 0;
            for (int start = 0; start < sheet.rows().size(); start += IMPORT_CHUNK_SIZE) {
                int end = Math.min(start + IMPORT_CHUNK_SIZE, sheet.rows().size());
                int chunkStart = start;
                int chunkEnd = end;
                ImportChunkResult result = executeInImportTransaction(() ->
                    processImportChunk(batchId, tenantId, sheet.rows().subList(chunkStart, chunkEnd), chunkStart, sourceName)
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
                                                String sourceName) {
        int successCount = 0;
        int failedCount = 0;
        List<String> codes = rows.stream()
            .map(row -> trimToNull(firstNonBlank(row, "编码")))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<String, ErpProduct> existingByCode = codes.isEmpty()
            ? Map.of()
            : erpProductMapper.findByCodes(tenantId, codes)
                .stream()
                .collect(Collectors.toMap(ErpProduct::getCode, Function.identity(), (left, right) -> left));
        List<ErpProductImportItem> importItems = new ArrayList<>();
        for (int offset = 0; offset < rows.size(); offset++) {
            int rowNo = startIndex + offset + 2;
            Map<String, String> row = new HashMap<>(rows.get(offset));
            ErpProductImportItem item = buildImportItem(batchId, tenantId, rowNo, row);
            try {
                String code = trimToNull(firstNonBlank(row, "编码"));
                ImportUpsertOutcome outcome = upsertImportedProduct(
                    tenantId,
                    rowNo,
                    row,
                    sourceName,
                    code == null ? null : existingByCode.get(code)
                );
                item.setMatchedProductId(outcome.product().getId());
                item.setStatus("SUCCESS");
                item.setWarningMessage(outcome.warningMessage());
                item.setMatchedStrategy(outcome.created() ? "CODE_UPSERT" : "CODE_UPDATE");
                item.setNormalizedPayload(outcome.normalizedPayload());
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
            erpProductImportItemMapper.insertBatch(importItems);
        }
        return new ImportChunkResult(successCount, failedCount);
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

    private record ImportChunkResult(int successCount, int failedCount) {
    }

    private ErpProductImportItem buildImportItem(Long batchId,
                                                 Long tenantId,
                                                 int rowNo,
                                                 Map<String, String> row) {
        Instant now = Instant.now();
        ErpProductImportItem item = new ErpProductImportItem();
        item.setTenantId(tenantId);
        item.setBatchId(batchId);
        item.setRowNo(rowNo);
        item.setSourceCode(trimToNull(firstNonBlank(row, "编码")));
        item.setSourceName(trimToNull(firstNonBlank(row, "配件名称", "名称")));
        item.setCategoryName(trimToNull(firstNonBlank(row, "类别")));
        item.setUnitName(trimToNull(firstNonBlank(row, "单位")));
        item.setWarehouseName(trimToNull(firstNonBlank(row, "默认仓库")));
        item.setSupplierName(trimToNull(firstNonBlank(row, "供应商名称", "来源供应商")));
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
        if (erpProductImportBatchMapper == null) {
            return;
        }
        ErpProductImportBatch batch = erpProductImportBatchMapper.selectOne(new QueryWrapper<ErpProductImportBatch>()
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
        erpProductImportBatchMapper.updateById(batch);
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "system";
        }
        String name = trimToNull(authentication.getName());
        return name == null ? "system" : name;
    }

    private JsonNode valueToTree(Object value) {
        return objectMapper.valueToTree(value);
    }

    private void applyRequest(ErpProduct product, ErpProductCreateRequest request) {
        product.setCode(request.code());
        product.setName(request.name());
        product.setShortName(request.shortName());
        product.setProductType(normalizeProductType(request.productType()));
        product.setSpec(request.spec());
        product.setModel(request.model());
        product.setCategoryId(request.categoryId());
        product.setUnitId(request.unitId());
        product.setDefaultWarehouseId(request.defaultWarehouseId());
        product.setDefaultLocationId(request.defaultLocationId());
        product.setBarcode(request.barcode());
        product.setSku(request.sku());
        product.setBrand(request.brand());
        product.setOrigin(request.origin());
        product.setManufacturerCode(request.manufacturerCode());
        product.setManufacturerModel(request.manufacturerModel());
        product.setManufacturerName(request.manufacturerName());
        product.setSourceSupplierId(request.sourceSupplierId());
        product.setWeight(request.weight());
        product.setVolume(request.volume());
        if (canEditCostPrice()) {
            product.setCostPrice(request.costPrice());
        }
        product.setSalePrice(request.salePrice());
        product.setTaxRate(request.taxRate());
        product.setSafetyStock(request.safetyStock());
        product.setMinStock(request.minStock());
        product.setMaxStock(request.maxStock());
        product.setBatch(request.batch());
        product.setShelfLifeDays(request.shelfLifeDays());
        product.setExtAttrs(parseExtAttrs(request.extAttrs()));
        product.setRemark(request.remark());
    }

    private void applyRequest(ErpProduct product, ErpProductUpdateRequest request) {
        product.setCode(request.code());
        product.setName(request.name());
        product.setShortName(request.shortName());
        product.setProductType(normalizeProductType(request.productType()));
        product.setSpec(request.spec());
        product.setModel(request.model());
        product.setCategoryId(request.categoryId());
        product.setUnitId(request.unitId());
        product.setDefaultWarehouseId(request.defaultWarehouseId());
        product.setDefaultLocationId(request.defaultLocationId());
        product.setBarcode(request.barcode());
        product.setSku(request.sku());
        product.setBrand(request.brand());
        product.setOrigin(request.origin());
        product.setManufacturerCode(request.manufacturerCode());
        product.setManufacturerModel(request.manufacturerModel());
        product.setManufacturerName(request.manufacturerName());
        product.setSourceSupplierId(request.sourceSupplierId());
        product.setWeight(request.weight());
        product.setVolume(request.volume());
        if (canEditCostPrice()) {
            product.setCostPrice(request.costPrice());
        }
        product.setSalePrice(request.salePrice());
        product.setTaxRate(request.taxRate());
        product.setSafetyStock(request.safetyStock());
        product.setMinStock(request.minStock());
        product.setMaxStock(request.maxStock());
        product.setBatch(request.batch());
        product.setShelfLifeDays(request.shelfLifeDays());
        product.setExtAttrs(parseExtAttrs(request.extAttrs()));
        product.setRemark(request.remark());
    }

    private boolean canEditCostPrice() {
        return hasAuthority("PERM_erp-product:cost:edit");
    }

    private String normalizeProductType(String productType) {
        if (productType == null || productType.isBlank()) {
            return PRODUCT_TYPE_NORMAL;
        }
        String normalized = productType.trim().toUpperCase();
        if (PRODUCT_TYPE_ASSEMBLY.equals(normalized) || PRODUCT_TYPE_NORMAL.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("商品类型不正确");
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(item -> authority.equals(item.getAuthority()));
    }

    private String generateProductCode(Long tenantId) {
        String prefix = readConfig("erp.product.code.prefix", "PR");
        String dateFormat = readConfig("erp.product.code.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.product.code.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, PRODUCT_CODE_TYPE, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, PRODUCT_CODE_TYPE, dateKey);
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

    private JsonNode parseExtAttrs(String rawExtAttrs) {
        if (rawExtAttrs == null || rawExtAttrs.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(rawExtAttrs);
        } catch (Exception ex) {
            throw new IllegalArgumentException("自定义字段格式不正确", ex);
        }
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

    private Boolean parseEnabled(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return true;
        }
        if (List.of("停用", "禁用", "disabled", "DISABLED").contains(normalized)) {
            return false;
        }
        return true;
    }

    private String mergeImportRemark(String remark, String sourceName) {
        String base = trimToNull(remark);
        String suffix = "Excel导入：" + sourceName;
        if (base == null) {
            return suffix;
        }
        return base + "；" + suffix;
    }

    private void validateAssociations(Long tenantId,
                                      Long categoryId,
                                      Long unitId,
                                      Long defaultWarehouseId,
                                      Long defaultLocationId,
                                      Long sourceSupplierId,
                                      List<ErpProductPriceItemRequest> priceItems) {
        if (categoryId != null && !existsById(erpCategoryMapper, ErpCategory.class, tenantId, categoryId)) {
            throw new IllegalArgumentException("商品分类不存在");
        }
        if (unitId != null && !existsById(erpUnitMapper, ErpUnit.class, tenantId, unitId)) {
            throw new IllegalArgumentException("计量单位不存在");
        }
        if (defaultWarehouseId != null && !existsById(erpWarehouseMapper, ErpWarehouse.class, tenantId, defaultWarehouseId)) {
            throw new IllegalArgumentException("默认仓库不存在");
        }
        if (defaultLocationId != null) {
            ErpLocation location = erpLocationMapper.selectOne(new QueryWrapper<ErpLocation>()
                .eq("tenant_id", tenantId)
                .eq("id", defaultLocationId));
            if (location == null) {
                throw new IllegalArgumentException("默认库位不存在");
            }
            if (defaultWarehouseId == null) {
                throw new IllegalArgumentException("选择默认库位时必须同时选择默认仓库");
            }
            if (!defaultWarehouseId.equals(location.getWarehouseId())) {
                throw new IllegalArgumentException("默认库位不属于所选默认仓库");
            }
        }
        if (sourceSupplierId != null && !existsById(erpSupplierMapper, ErpSupplier.class, tenantId, sourceSupplierId)) {
            throw new IllegalArgumentException("来源供应商不存在");
        }
        validatePriceItems(tenantId, priceItems);
    }

    private void validatePriceItems(Long tenantId, List<ErpProductPriceItemRequest> priceItems) {
        if (priceItems == null || priceItems.isEmpty()) {
            return;
        }
        Set<Long> seenCategoryIds = new HashSet<>();
        for (ErpProductPriceItemRequest item : priceItems) {
            if (item == null || item.customerCategoryId() == null || item.salePrice() == null) {
                continue;
            }
            if (!seenCategoryIds.add(item.customerCategoryId())) {
                throw new IllegalArgumentException("客户类别价格存在重复项");
            }
            if (!existsById(erpCustomerCategoryMapper, ErpCustomerCategory.class, tenantId, item.customerCategoryId())) {
                throw new IllegalArgumentException("客户类别不存在");
            }
        }
    }

    private void saveProductPrices(Long tenantId, Long productId, List<ErpProductPriceItemRequest> priceItems) {
        erpProductPriceMapper.deleteByProduct(tenantId, productId);
        if (priceItems == null || priceItems.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (ErpProductPriceItemRequest item : priceItems) {
            if (item == null || item.customerCategoryId() == null || item.salePrice() == null) {
                continue;
            }
            ErpProductPrice price = new ErpProductPrice();
            price.setTenantId(tenantId);
            price.setProductId(productId);
            price.setCustomerCategoryId(item.customerCategoryId());
            price.setSalePrice(item.salePrice());
            price.setCreatedAt(now);
            price.setUpdatedAt(now);
            erpProductPriceMapper.insert(price);
        }
    }

    private <T> boolean existsById(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,
                                   Class<T> entityClass,
                                   Long tenantId,
                                   Long id) {
        return mapper.selectCount(new QueryWrapper<T>()
            .eq("tenant_id", tenantId)
            .eq("id", id)) > 0;
    }

    private record ImportUpsertOutcome(ErpProduct product,
                                       boolean created,
                                       String warningMessage,
                                       JsonNode normalizedPayload) {
    }
}
