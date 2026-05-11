package com.example.wms;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpAccountsPayable;
import com.example.wms.entity.erp.ErpAccountsReceivable;
import com.example.wms.entity.erp.ErpPayment;
import com.example.wms.entity.erp.ErpPaymentPayable;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.entity.erp.ErpReceipt;
import com.example.wms.entity.erp.ErpReceiptReceivable;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPaymentPayableMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderItemMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpReceiptReceivableMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.service.erp.impl.ErpPaymentServiceImpl;
import com.example.wms.service.erp.impl.ErpPurchaseOrderServiceImpl;
import com.example.wms.service.erp.impl.ErpReceiptServiceImpl;
import com.example.wms.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ErpFinanceWorkflowTests {
    @Mock
    private ErpReceiptMapper receiptMapper;
    @Mock
    private ErpReceiptReceivableMapper receiptReceivableMapper;
    @Mock
    private ErpCustomerMapper customerMapper;
    @Mock
    private ErpAccountsReceivableMapper receivableMapper;
    @Mock
    private ErpSaleOrderMapper saleOrderMapper;
    @Mock
    private ErpOrderSequenceMapper orderSequenceMapper;
    @Mock
    private SystemConfigMapper systemConfigMapper;

    @Mock
    private ErpPaymentMapper paymentMapper;
    @Mock
    private ErpPaymentPayableMapper paymentPayableMapper;
    @Mock
    private ErpSupplierMapper supplierMapper;
    @Mock
    private ErpAccountsPayableMapper payableMapper;
    @Mock
    private ErpPurchaseOrderMapper purchaseOrderMapper;

    @Mock
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpStockBalanceMapper stockBalanceMapper;
    @Mock
    private ErpStockTxnMapper stockTxnMapper;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void receiptApproveRejectsOverAllocationWhenUnpaidAlreadyChanged() {
        ErpReceiptServiceImpl service = receiptService();
        ErpReceipt receipt = new ErpReceipt();
        receipt.setId(10L);
        receipt.setTenantId(1L);
        receipt.setReceivableId(100L);
        receipt.setStatus("DRAFT");
        receipt.setAmount(new BigDecimal("80"));
        receipt.setDiscountAmount(BigDecimal.ZERO);

        ErpReceiptReceivable allocation = new ErpReceiptReceivable();
        allocation.setReceiptId(10L);
        allocation.setReceivableId(100L);
        allocation.setAllocatedTotal(new BigDecimal("80"));

        ErpAccountsReceivable receivable = new ErpAccountsReceivable();
        receivable.setId(100L);
        receivable.setTenantId(1L);
        receivable.setStatus("OPEN");
        receivable.setUnpaidAmount(new BigDecimal("50"));

        when(receiptMapper.selectOne(any())).thenReturn(receipt);
        when(receiptReceivableMapper.findByReceiptId(1L, 10L)).thenReturn(List.of(allocation));
        when(receivableMapper.selectOne(any())).thenReturn(receivable);

        assertThatThrownBy(() -> service.approve(10L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("收款金额不能大于未收金额");

        verify(receiptMapper, never()).updateById(any(ErpReceipt.class));
    }

    @Test
    void paymentApproveRejectsOverAllocationWhenUnpaidAlreadyChanged() {
        ErpPaymentServiceImpl service = paymentService();
        ErpPayment payment = new ErpPayment();
        payment.setId(20L);
        payment.setTenantId(1L);
        payment.setPayableId(200L);
        payment.setStatus("DRAFT");
        payment.setAmount(new BigDecimal("80"));
        payment.setDiscountAmount(BigDecimal.ZERO);

        ErpPaymentPayable allocation = new ErpPaymentPayable();
        allocation.setPaymentId(20L);
        allocation.setPayableId(200L);
        allocation.setAllocatedTotal(new BigDecimal("80"));

        ErpAccountsPayable payable = new ErpAccountsPayable();
        payable.setId(200L);
        payable.setTenantId(1L);
        payable.setStatus("OPEN");
        payable.setUnpaidAmount(new BigDecimal("50"));

        when(paymentMapper.selectOne(any())).thenReturn(payment);
        when(paymentPayableMapper.findByPaymentId(1L, 20L)).thenReturn(List.of(allocation));
        when(payableMapper.selectOne(any())).thenReturn(payable);

        assertThatThrownBy(() -> service.approve(20L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("付款金额不能大于未付金额");

        verify(paymentMapper, never()).updateById(any(ErpPayment.class));
    }

    @Test
    void purchaseUnapproveRollsBackAutoFinanceDocuments() {
        ErpPurchaseOrderServiceImpl service = purchaseOrderService();
        ErpPurchaseOrder order = new ErpPurchaseOrder();
        order.setId(30L);
        order.setTenantId(1L);
        order.setStatus("APPROVED");
        order.setVersion(0L);

        ErpPayment autoPayment = new ErpPayment();
        autoPayment.setId(31L);
        autoPayment.setTenantId(1L);
        autoPayment.setPurchaseOrderId(30L);
        autoPayment.setRemark("采购单审核自动付款");
        autoPayment.setStatus("APPROVED");

        ErpAccountsPayable payable = new ErpAccountsPayable();
        payable.setId(32L);
        payable.setTenantId(1L);
        payable.setPurchaseOrderId(30L);

        when(purchaseOrderMapper.selectOne(any())).thenReturn(order);
        when(paymentMapper.selectList(any())).thenReturn(List.of(autoPayment));
        when(payableMapper.findByPurchaseOrderId(1L, 30L)).thenReturn(payable);
        when(paymentPayableMapper.findByPayableId(1L, 32L)).thenReturn(List.of());
        when(purchaseOrderItemMapper.findByOrderId(1L, 30L)).thenReturn(List.of());
        when(purchaseOrderMapper.update(any(ErpPurchaseOrder.class), any(QueryWrapper.class))).thenReturn(1);

        service.unapprove(30L);

        verify(paymentPayableMapper).delete(any(QueryWrapper.class));
        verify(paymentMapper).deleteById(31L);
        verify(payableMapper).deleteById(32L);
        assertThat(order.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    void purchaseCancelBlocksApprovedDiscountOnlyPayment() {
        ErpPurchaseOrderServiceImpl service = purchaseOrderService();
        ErpPurchaseOrder order = new ErpPurchaseOrder();
        order.setId(40L);
        order.setTenantId(1L);
        order.setStatus("APPROVED");

        ErpPayment payment = new ErpPayment();
        payment.setId(41L);
        payment.setTenantId(1L);
        payment.setStatus("APPROVED");
        payment.setAmount(BigDecimal.ZERO);
        payment.setDiscountAmount(new BigDecimal("5"));

        when(purchaseOrderMapper.selectOne(any())).thenReturn(order);
        when(paymentMapper.selectList(any())).thenReturn(List.of(payment));

        assertThatThrownBy(() -> service.cancel(40L, "redo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("请先红冲付款单");
    }

    @Test
    void receiptListQueryDoesNotFilterNegativeAmounts() {
        ErpReceiptServiceImpl service = receiptService();
        ArgumentCaptor<QueryWrapper<ErpReceipt>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        when(receiptMapper.selectList(any())).thenReturn(List.of());

        service.listAll(null, null, null, null, null, null);

        verify(receiptMapper).selectList(captor.capture());
        assertThat(String.valueOf(captor.getValue().getSqlSegment())).doesNotContain("amount");
    }

    @Test
    void paymentListQueryDoesNotFilterNegativeAmounts() {
        ErpPaymentServiceImpl service = paymentService();
        ArgumentCaptor<QueryWrapper<ErpPayment>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        when(paymentMapper.selectList(any())).thenReturn(List.of());

        service.listAll(null, null, null, null, null, null);

        verify(paymentMapper).selectList(captor.capture());
        assertThat(String.valueOf(captor.getValue().getSqlSegment())).doesNotContain("amount");
    }

    private ErpReceiptServiceImpl receiptService() {
        return new ErpReceiptServiceImpl(
            receiptMapper,
            receiptReceivableMapper,
            customerMapper,
            receivableMapper,
            saleOrderMapper,
            orderSequenceMapper,
            systemConfigMapper
        );
    }

    private ErpPaymentServiceImpl paymentService() {
        return new ErpPaymentServiceImpl(
            paymentMapper,
            paymentPayableMapper,
            supplierMapper,
            payableMapper,
            purchaseOrderMapper,
            orderSequenceMapper,
            systemConfigMapper
        );
    }

    private ErpPurchaseOrderServiceImpl purchaseOrderService() {
        return new ErpPurchaseOrderServiceImpl(
            purchaseOrderMapper,
            purchaseOrderItemMapper,
            productMapper,
            stockBalanceMapper,
            stockTxnMapper,
            orderSequenceMapper,
            systemConfigMapper,
            payableMapper,
            paymentMapper,
            paymentPayableMapper
        );
    }
}
