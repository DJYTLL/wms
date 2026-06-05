package com.example.wms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSaleOrderFlowSnapshot;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpProductPriceMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpReceiptMethodMapper;
import com.example.wms.mapper.erp.ErpReceiptReceivableMapper;
import com.example.wms.mapper.erp.ErpSaleOrderItemMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.TenantSettingService;
import com.example.wms.service.erp.impl.ErpSaleOrderServiceImpl;
import com.example.wms.service.erp.support.ErpCostService;
import com.example.wms.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpSaleOrderServiceImplTest {
    @Mock private ErpSaleOrderMapper erpSaleOrderMapper;
    @Mock private ErpSaleOrderItemMapper erpSaleOrderItemMapper;
    @Mock private ErpProductMapper erpProductMapper;
    @Mock private ErpProductPriceMapper erpProductPriceMapper;
    @Mock private ErpCustomerMapper erpCustomerMapper;
    @Mock private ErpWarehouseMapper erpWarehouseMapper;
    @Mock private ErpLocationMapper erpLocationMapper;
    @Mock private ErpSettlementMethodMapper erpSettlementMethodMapper;
    @Mock private ErpReceiptMethodMapper erpReceiptMethodMapper;
    @Mock private ErpStockBalanceMapper erpStockBalanceMapper;
    @Mock private ErpStockTxnMapper erpStockTxnMapper;
    @Mock private ErpOrderSequenceMapper erpOrderSequenceMapper;
    @Mock private ErpAccountsReceivableMapper erpAccountsReceivableMapper;
    @Mock private ErpReceiptMapper erpReceiptMapper;
    @Mock private ErpReceiptReceivableMapper erpReceiptReceivableMapper;
    @Mock private ErpSaleReturnMapper erpSaleReturnMapper;
    @Mock private SystemConfigMapper systemConfigMapper;
    @Mock private ErpCostService erpCostService;
    @Mock private TenantSettingService tenantSettingService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void pageUsesBatchFlowSnapshotInsteadOfPerRowLookups() {
        TenantContext.setTenantId(9L);
        ErpSaleOrder draftOrder = order(101L, "DRAFT", "SO-DRAFT", "120.00");
        draftOrder.setCustomerId(201L);
        ErpSaleOrder approvedOrder = order(102L, "APPROVED", "SO-APPROVED", "300.00");
        approvedOrder.setCustomerId(202L);
        Page<ErpSaleOrder> pageResult = new Page<>(1, 20, 2);
        pageResult.setRecords(List.of(draftOrder, approvedOrder));

        when(erpSaleOrderMapper.selectPage(any(Page.class), any())).thenReturn(pageResult);
        when(erpCustomerMapper.selectList(any())).thenReturn(List.of(
            customer(201L, "客户A"),
            customer(202L, "客户B")
        ));
        when(erpSaleOrderMapper.findFlowSnapshotsByIds(9L, List.of(101L, 102L))).thenReturn(List.of(
            snapshot(101L, null, null, 0L, "0", "40", "0"),
            snapshot(102L, "OPEN", "70", 2L, "50", "210", "15")
        ));

        ErpSaleOrderServiceImpl service = new ErpSaleOrderServiceImpl(
            erpSaleOrderMapper,
            erpSaleOrderItemMapper,
            erpProductMapper,
            erpProductPriceMapper,
            erpCustomerMapper,
            erpWarehouseMapper,
            erpLocationMapper,
            erpSettlementMethodMapper,
            erpReceiptMethodMapper,
            erpStockBalanceMapper,
            erpStockTxnMapper,
            erpOrderSequenceMapper,
            erpAccountsReceivableMapper,
            erpReceiptMapper,
            erpReceiptReceivableMapper,
            erpSaleReturnMapper,
            systemConfigMapper,
            erpCostService,
            tenantSettingService
        );

        PageResponse<ErpSaleOrder> response = service.page(1, 20, null, null, null, null, null);

        assertThat(response.items()).hasSize(2);
        ErpSaleOrder first = response.items().get(0);
        assertThat(first.getCustomerName()).isEqualTo("客户A");
        assertThat(first.getReceivableStatus()).isEqualTo("OPEN");
        assertThat(first.getReceivableUnpaidAmount()).isEqualByComparingTo("120.00");
        assertThat(first.getNetSaleAmount()).isEqualByComparingTo("120.00");
        assertThat(first.getNetGrossProfit()).isEqualByComparingTo("80.00");

        ErpSaleOrder second = response.items().get(1);
        assertThat(second.getCustomerName()).isEqualTo("客户B");
        assertThat(second.getReceivableStatus()).isEqualTo("OPEN");
        assertThat(second.getReceivableUnpaidAmount()).isEqualByComparingTo("70");
        assertThat(second.getApprovedReturnCount()).isEqualTo(2L);
        assertThat(second.getCumulativeReturnAmount()).isEqualByComparingTo("50");
        assertThat(second.getCumulativeReturnCost()).isEqualByComparingTo("15");
        assertThat(second.getNetSaleAmount()).isEqualByComparingTo("250.00");
        assertThat(second.getNetGrossProfit()).isEqualByComparingTo("55.00");

        verify(erpSaleOrderMapper).findFlowSnapshotsByIds(9L, List.of(101L, 102L));
        verify(erpAccountsReceivableMapper, never()).findBySaleOrderId(any(), any());
        verify(erpSaleReturnMapper, never()).countApprovedBySaleOrderId(any(), any());
        verify(erpSaleReturnMapper, never()).sumApprovedAmountBySaleOrderId(any(), any());
        verify(erpStockTxnMapper, never()).sumSaleIssueCost(any(), any());
        verify(erpStockTxnMapper, never()).sumApprovedSaleReturnCost(any(), any());
    }

    private ErpCustomer customer(Long id, String name) {
        ErpCustomer customer = new ErpCustomer();
        customer.setId(id);
        customer.setName(name);
        return customer;
    }

    private ErpSaleOrder order(Long id, String status, String orderNo, String totalAmountInclTax) {
        ErpSaleOrder order = new ErpSaleOrder();
        order.setId(id);
        order.setStatus(status);
        order.setOrderNo(orderNo);
        order.setTotalAmountInclTax(new BigDecimal(totalAmountInclTax));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setCreatedAt(Instant.now());
        return order;
    }

    private ErpSaleOrderFlowSnapshot snapshot(Long saleOrderId,
                                              String receivableStatus,
                                              String receivableUnpaidAmount,
                                              Long approvedReturnCount,
                                              String cumulativeReturnAmount,
                                              String saleCost,
                                              String cumulativeReturnCost) {
        return new ErpSaleOrderFlowSnapshot(
            saleOrderId,
            receivableStatus,
            receivableUnpaidAmount == null ? null : new BigDecimal(receivableUnpaidAmount),
            approvedReturnCount,
            new BigDecimal(cumulativeReturnAmount),
            new BigDecimal(saleCost),
            new BigDecimal(cumulativeReturnCost)
        );
    }
}
