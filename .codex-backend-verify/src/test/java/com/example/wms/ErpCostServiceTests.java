package com.example.wms;

import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.service.erp.support.ErpCostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpCostServiceTests {
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpStockBalanceMapper stockBalanceMapper;

    @Test
    void inboundAverageCostLocksProductAndRecomputesMovingAverage() {
        ErpCostService service = costService();
        ErpProduct product = product(100L, "10.0000");
        when(productMapper.findByIdForUpdate(1L, 100L)).thenReturn(product);
        when(stockBalanceMapper.sumQtyByProduct(1L, 100L)).thenReturn(new BigDecimal("10"));

        service.applyInboundAverageCost(1L, 100L, new BigDecimal("10"), new BigDecimal("20"));

        ArgumentCaptor<ErpProduct> captor = ArgumentCaptor.forClass(ErpProduct.class);
        verify(productMapper).findByIdForUpdate(1L, 100L);
        verify(productMapper).updateById(captor.capture());
        assertThat(captor.getValue().getCostPrice()).isEqualByComparingTo("15.0000");
    }

    @Test
    void reverseInboundAverageCostRestoresPreviousAverageCost() {
        ErpCostService service = costService();
        ErpProduct product = product(100L, "15.0000");
        when(productMapper.findByIdForUpdate(1L, 100L)).thenReturn(product);
        when(stockBalanceMapper.sumQtyByProduct(1L, 100L)).thenReturn(new BigDecimal("20"));

        service.reverseInboundAverageCost(1L, 100L, new BigDecimal("10"), new BigDecimal("20"));

        ArgumentCaptor<ErpProduct> captor = ArgumentCaptor.forClass(ErpProduct.class);
        verify(productMapper).findByIdForUpdate(1L, 100L);
        verify(productMapper).updateById(captor.capture());
        assertThat(captor.getValue().getCostPrice()).isEqualByComparingTo("10.0000");
    }

    @Test
    void reverseInboundAverageCostClearsCostWhenNoStockRemains() {
        ErpCostService service = costService();
        ErpProduct product = product(100L, "20.0000");
        when(productMapper.findByIdForUpdate(1L, 100L)).thenReturn(product);
        when(stockBalanceMapper.sumQtyByProduct(1L, 100L)).thenReturn(new BigDecimal("5"));

        service.reverseInboundAverageCost(1L, 100L, new BigDecimal("5"), new BigDecimal("20"));

        ArgumentCaptor<ErpProduct> captor = ArgumentCaptor.forClass(ErpProduct.class);
        verify(productMapper).updateById(captor.capture());
        assertThat(captor.getValue().getCostPrice()).isEqualByComparingTo("0.0000");
    }

    private ErpCostService costService() {
        return new ErpCostService(productMapper, stockBalanceMapper);
    }

    private ErpProduct product(Long id, String costPrice) {
        ErpProduct product = new ErpProduct();
        product.setId(id);
        product.setTenantId(1L);
        product.setCostPrice(new BigDecimal(costPrice));
        return product;
    }
}
