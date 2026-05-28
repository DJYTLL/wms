package com.example.wms.service.erp.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ErpCostService {
    private final ErpProductMapper erpProductMapper;
    private final ErpStockBalanceMapper erpStockBalanceMapper;

    public ErpCostService(ErpProductMapper erpProductMapper,
                          ErpStockBalanceMapper erpStockBalanceMapper) {
        this.erpProductMapper = erpProductMapper;
        this.erpStockBalanceMapper = erpStockBalanceMapper;
    }

    public BigDecimal getProductCost(Long tenantId, Long productId) {
        if (productId == null) {
            return BigDecimal.ZERO;
        }
        ErpProduct product = erpProductMapper.selectOne(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", tenantId)
            .eq("id", productId));
        return costOf(product);
    }

    public void applyInboundAverageCost(Long tenantId, Long productId, BigDecimal inboundQty, BigDecimal inboundUnitCost) {
        if (productId == null || inboundQty == null || inboundUnitCost == null) {
            return;
        }
        if (inboundQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        ErpProduct product = lockProduct(tenantId, productId);
        if (product == null) {
            return;
        }
        BigDecimal oldQty = sumQty(tenantId, productId);
        BigDecimal oldCost = costOf(product);
        BigDecimal newQty = oldQty.add(inboundQty);
        if (newQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal totalCost = oldCost.multiply(oldQty).add(inboundUnitCost.multiply(inboundQty));
        updateCost(product, totalCost.divide(newQty, 4, RoundingMode.HALF_UP));
    }

    public void reverseInboundAverageCost(Long tenantId, Long productId, BigDecimal removedQty, BigDecimal removedUnitCost) {
        if (productId == null || removedQty == null || removedUnitCost == null) {
            return;
        }
        if (removedQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        ErpProduct product = lockProduct(tenantId, productId);
        if (product == null) {
            return;
        }
        BigDecimal currentQty = sumQty(tenantId, productId);
        BigDecimal remainingQty = currentQty.subtract(removedQty);
        if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
            updateCost(product, BigDecimal.ZERO);
            return;
        }
        BigDecimal currentCost = costOf(product);
        BigDecimal remainingTotal = currentCost.multiply(currentQty).subtract(removedUnitCost.multiply(removedQty));
        if (remainingTotal.compareTo(BigDecimal.ZERO) <= 0) {
            updateCost(product, BigDecimal.ZERO);
            return;
        }
        updateCost(product, remainingTotal.divide(remainingQty, 4, RoundingMode.HALF_UP));
    }

    private ErpProduct lockProduct(Long tenantId, Long productId) {
        return erpProductMapper.findByIdForUpdate(tenantId, productId);
    }

    private BigDecimal sumQty(Long tenantId, Long productId) {
        BigDecimal qty = erpStockBalanceMapper.sumQtyByProduct(tenantId, productId);
        return qty == null ? BigDecimal.ZERO : qty;
    }

    private BigDecimal costOf(ErpProduct product) {
        if (product == null || product.getCostPrice() == null) {
            return BigDecimal.ZERO;
        }
        return product.getCostPrice();
    }

    private void updateCost(ErpProduct product, BigDecimal cost) {
        product.setCostPrice(cost == null ? BigDecimal.ZERO : cost.setScale(4, RoundingMode.HALF_UP));
        erpProductMapper.updateById(product);
    }
}
