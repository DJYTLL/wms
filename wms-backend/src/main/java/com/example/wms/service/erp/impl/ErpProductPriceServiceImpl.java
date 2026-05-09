package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.erp.ErpProductPriceItemRequest;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpProductPrice;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpProductPriceMapper;
import com.example.wms.service.erp.ErpProductPriceService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

// 商品价格服务实现（ERP进销存）
@Service
public class ErpProductPriceServiceImpl implements ErpProductPriceService {
    private final ErpProductPriceMapper erpProductPriceMapper;
    private final ErpProductMapper erpProductMapper;

    public ErpProductPriceServiceImpl(ErpProductPriceMapper erpProductPriceMapper,
                                      ErpProductMapper erpProductMapper) {
        this.erpProductPriceMapper = erpProductPriceMapper;
        this.erpProductMapper = erpProductMapper;
    }

    @Override
    public List<ErpProductPrice> listByProduct(Long productId) {
        if (productId == null) {
            return Collections.emptyList();
        }
        Long tenantId = TenantContext.requireTenantId();
        ensureProductExists(tenantId, productId);
        return erpProductPriceMapper.listByProduct(tenantId, productId);
    }

    @Override
    @AuditLog(action = "ERP_PRODUCT_PRICE_SAVE", entityType = "erp_product_price", entityId = "{arg0}", detail = "items={arg1}")
    public void saveForProduct(Long productId, List<ErpProductPriceItemRequest> items) {
        if (productId == null) {
            throw new IllegalArgumentException("商品不能为空");
        }
        Long tenantId = TenantContext.requireTenantId();
        ensureProductExists(tenantId, productId);
        erpProductPriceMapper.deleteByProduct(tenantId, productId);
        if (items == null || items.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (ErpProductPriceItemRequest item : items) {
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

    @Override
    public BigDecimal resolvePrice(Long productId, Long customerCategoryId) {
        if (productId == null || customerCategoryId == null) {
            return null;
        }
        Long tenantId = TenantContext.requireTenantId();
        ErpProductPrice price = erpProductPriceMapper.findByProductAndCategory(tenantId, productId, customerCategoryId);
        return price == null ? null : price.getSalePrice();
    }

    private void ensureProductExists(Long tenantId, Long productId) {
        ErpProduct product = erpProductMapper.selectOne(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", tenantId)
            .eq("id", productId));
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
    }
}
