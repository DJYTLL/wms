package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpProductCreateRequest;
import com.example.wms.dto.erp.ErpProductPriceItemRequest;
import com.example.wms.dto.erp.ErpProductUpdateRequest;
import com.example.wms.entity.erp.ErpCategory;
import com.example.wms.entity.erp.ErpCustomerCategory;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpProductPrice;
import com.example.wms.entity.erp.ErpUnit;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.entity.SystemConfig;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpCategoryMapper;
import com.example.wms.mapper.erp.ErpCustomerCategoryMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpProductPriceMapper;
import com.example.wms.mapper.erp.ErpUnitMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.ErpProductService;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 商品服务实现（ERP进销存）
@Service
public class ErpProductServiceImpl implements ErpProductService {
    private static final String PRODUCT_CODE_TYPE = "PRODUCT";

    private final ErpProductMapper erpProductMapper;
    private final ErpProductPriceMapper erpProductPriceMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ErpCategoryMapper erpCategoryMapper;
    private final ErpUnitMapper erpUnitMapper;
    private final ErpWarehouseMapper erpWarehouseMapper;
    private final ErpLocationMapper erpLocationMapper;
    private final ErpCustomerCategoryMapper erpCustomerCategoryMapper;
    private final ObjectMapper objectMapper;

    public ErpProductServiceImpl(ErpProductMapper erpProductMapper,
                                 ErpProductPriceMapper erpProductPriceMapper,
                                 ErpOrderSequenceMapper erpOrderSequenceMapper,
                                 SystemConfigMapper systemConfigMapper,
                                 ErpCategoryMapper erpCategoryMapper,
                                 ErpUnitMapper erpUnitMapper,
                                 ErpWarehouseMapper erpWarehouseMapper,
                                 ErpLocationMapper erpLocationMapper,
                                 ErpCustomerCategoryMapper erpCustomerCategoryMapper,
                                 ObjectMapper objectMapper) {
        this.erpProductMapper = erpProductMapper;
        this.erpProductPriceMapper = erpProductPriceMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.erpCategoryMapper = erpCategoryMapper;
        this.erpUnitMapper = erpUnitMapper;
        this.erpWarehouseMapper = erpWarehouseMapper;
        this.erpLocationMapper = erpLocationMapper;
        this.erpCustomerCategoryMapper = erpCustomerCategoryMapper;
        this.objectMapper = objectMapper;
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
        validateAssociations(tenantId, request.categoryId(), request.unitId(), request.defaultWarehouseId(), request.defaultLocationId(), request.priceItems());
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
        validateAssociations(tenantId, request.categoryId(), request.unitId(), request.defaultWarehouseId(), request.defaultLocationId(), request.priceItems());
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

    private void applyRequest(ErpProduct product, ErpProductCreateRequest request) {
        product.setCode(request.code());
        product.setName(request.name());
        product.setShortName(request.shortName());
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
        SystemConfig config = systemConfigMapper.findByKey(key);
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

    private void validateAssociations(Long tenantId,
                                      Long categoryId,
                                      Long unitId,
                                      Long defaultWarehouseId,
                                      Long defaultLocationId,
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
}
