package com.example.wms;

import com.example.wms.dto.erp.ErpSaleOrderCreateRequest;
import com.example.wms.dto.erp.ErpSaleOrderItemRequest;
import com.example.wms.dto.erp.ErpSaleReturnCreateRequest;
import com.example.wms.dto.erp.ErpSaleReturnItemRequest;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpReceipt;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.entity.erp.ErpSaleOrderItem;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpReceiptReceivableMapper;
import com.example.wms.mapper.erp.ErpSaleOrderItemMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSaleReturnItemMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.service.erp.impl.ErpSaleOrderServiceImpl;
import com.example.wms.service.erp.impl.ErpSaleReturnServiceImpl;
import com.example.wms.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleFlowValidationTests {
    @Mock
    private ErpSaleOrderMapper saleOrderMapper;
    @Mock
    private ErpSaleOrderItemMapper saleOrderItemMapper;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpStockBalanceMapper stockBalanceMapper;
    @Mock
    private ErpStockTxnMapper stockTxnMapper;
    @Mock
    private ErpOrderSequenceMapper orderSequenceMapper;
    @Mock
    private ErpAccountsReceivableMapper accountsReceivableMapper;
    @Mock
    private ErpReceiptMapper receiptMapper;
    @Mock
    private ErpReceiptReceivableMapper receiptReceivableMapper;
    @Mock
    private SystemConfigMapper systemConfigMapper;
    @Mock
    private ErpSaleReturnMapper saleReturnMapper;
    @Mock
    private ErpSaleReturnItemMapper saleReturnItemMapper;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void saleCreateRejectsNegativePaidAmount() {
        ErpSaleOrderServiceImpl service = saleOrderService();
        when(productMapper.selectOne(any())).thenReturn(product(100L));

        ErpSaleOrderCreateRequest request = new ErpSaleOrderCreateRequest(
            "SO-001",
            "2026-05-12 09:00:00",
            10L,
            "CASH",
            null,
            new BigDecimal("-1"),
            BigDecimal.ZERO,
            List.of(saleOrderItemRequest(100L, "100")),
            null
        );

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("付款金额不能小于0");
    }

    @Test
    void saleCreateRejectsSettlementAmountsExceedingTotal() {
        ErpSaleOrderServiceImpl service = saleOrderService();
        when(productMapper.selectOne(any())).thenReturn(product(100L));

        ErpSaleOrderCreateRequest request = new ErpSaleOrderCreateRequest(
            "SO-002",
            "2026-05-12 09:00:00",
            10L,
            "CASH",
            null,
            new BigDecimal("70"),
            new BigDecimal("40"),
            List.of(saleOrderItemRequest(100L, "100")),
            null
        );

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("付款金额与优惠金额之和不能大于销售总金额");
    }

    @Test
    void saleRedFlushRejectsApprovedDiscountOnlyReceipt() {
        ErpSaleOrderServiceImpl service = saleOrderService();
        ErpSaleOrder order = approvedSaleOrder(88L, 10L);
        ErpReceipt receipt = new ErpReceipt();
        receipt.setId(501L);
        receipt.setTenantId(1L);
        receipt.setSaleOrderId(88L);
        receipt.setStatus("APPROVED");
        receipt.setAmount(BigDecimal.ZERO);
        receipt.setDiscountAmount(new BigDecimal("10"));

        when(saleOrderMapper.selectOne(any())).thenReturn(order);
        when(receiptMapper.selectList(any())).thenReturn(List.of(receipt));

        assertThatThrownBy(() -> service.redFlush(88L, "测试"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("请先红冲收款单");
    }

    @Test
    void saleRedFlushRejectsApprovedSaleReturn() {
        ErpSaleOrderServiceImpl service = saleOrderService();
        when(saleOrderMapper.selectOne(any())).thenReturn(approvedSaleOrder(88L, 10L));
        when(receiptMapper.selectList(any())).thenReturn(List.of());
        when(saleReturnMapper.countApprovedBySaleOrderId(1L, 88L)).thenReturn(1L);

        assertThatThrownBy(() -> service.redFlush(88L, "测试"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("请先红冲销售退货单");
    }

    @Test
    void saleReturnCreateRejectsOverReturnQuantity() {
        ErpSaleReturnServiceImpl service = saleReturnService();
        when(saleOrderMapper.selectOne(any())).thenReturn(approvedSaleOrder(99L, 10L));
        when(saleOrderItemMapper.findByOrderId(1L, 99L)).thenReturn(List.of(soldItem(100L, "1")));
        when(saleReturnMapper.selectList(any())).thenReturn(List.of());

        ErpSaleReturnCreateRequest request = new ErpSaleReturnCreateRequest(
            "SR-001",
            "2026-05-12 10:00:00",
            "RESTOCK",
            10L,
            99L,
            "CASH",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            List.of(saleReturnItemRequest(100L, "2")),
            null
        );

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("商品退货数量不能超过原销售可退数量");
    }

    @Test
    void saleReturnCreateRejectsNegativeRefundAmount() {
        ErpSaleReturnServiceImpl service = saleReturnService();
        when(saleOrderMapper.selectOne(any())).thenReturn(approvedSaleOrder(99L, 10L));
        when(saleOrderItemMapper.findByOrderId(1L, 99L)).thenReturn(List.of(soldItem(100L, "2")));
        when(saleReturnMapper.selectList(any())).thenReturn(List.of());
        when(productMapper.selectOne(any())).thenReturn(product(100L));

        ErpSaleReturnCreateRequest request = new ErpSaleReturnCreateRequest(
            "SR-002",
            "2026-05-12 10:00:00",
            "RESTOCK",
            10L,
            99L,
            "CASH",
            new BigDecimal("-1"),
            BigDecimal.ZERO,
            List.of(saleReturnItemRequest(100L, "1")),
            null
        );

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("退款金额不能小于0");
    }

    private ErpSaleOrderServiceImpl saleOrderService() {
        return new ErpSaleOrderServiceImpl(
            saleOrderMapper,
            saleOrderItemMapper,
            productMapper,
            stockBalanceMapper,
            stockTxnMapper,
            orderSequenceMapper,
            accountsReceivableMapper,
            receiptMapper,
            receiptReceivableMapper,
            saleReturnMapper,
            systemConfigMapper
        );
    }

    private ErpSaleReturnServiceImpl saleReturnService() {
        return new ErpSaleReturnServiceImpl(
            saleReturnMapper,
            saleReturnItemMapper,
            productMapper,
            stockBalanceMapper,
            stockTxnMapper,
            orderSequenceMapper,
            accountsReceivableMapper,
            receiptMapper,
            receiptReceivableMapper,
            saleOrderItemMapper,
            saleOrderMapper,
            systemConfigMapper
        );
    }

    private ErpProduct product(Long id) {
        ErpProduct product = new ErpProduct();
        product.setId(id);
        product.setCode("P-" + id);
        product.setName("Product-" + id);
        return product;
    }

    private ErpSaleOrder approvedSaleOrder(Long id, Long customerId) {
        ErpSaleOrder order = new ErpSaleOrder();
        order.setId(id);
        order.setTenantId(1L);
        order.setStatus("APPROVED");
        order.setCustomerId(customerId);
        return order;
    }

    private ErpSaleOrderItem soldItem(Long productId, String qty) {
        ErpSaleOrderItem item = new ErpSaleOrderItem();
        item.setProductId(productId);
        item.setQty(new BigDecimal(qty));
        return item;
    }

    private ErpSaleOrderItemRequest saleOrderItemRequest(Long productId, String price) {
        return new ErpSaleOrderItemRequest(
            productId,
            1L,
            1L,
            BigDecimal.ONE,
            new BigDecimal(price),
            null,
            BigDecimal.ZERO,
            1,
            null
        );
    }

    private ErpSaleReturnItemRequest saleReturnItemRequest(Long productId, String qty) {
        return new ErpSaleReturnItemRequest(
            productId,
            1L,
            1L,
            new BigDecimal(qty),
            new BigDecimal("100"),
            null,
            BigDecimal.ZERO,
            1,
            null
        );
    }
}
