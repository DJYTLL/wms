package com.example.wms;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.wms.controller.erp.ErpSupplierController;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpAccountsPayable;
import com.example.wms.entity.erp.ErpAccountsReceivable;
import com.example.wms.entity.erp.ErpPayment;
import com.example.wms.entity.erp.ErpPaymentPayable;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.entity.erp.ErpPurchaseOrderItem;
import com.example.wms.entity.erp.ErpPurchaseReturn;
import com.example.wms.entity.erp.ErpPurchaseReturnItem;
import com.example.wms.entity.erp.ErpReceipt;
import com.example.wms.entity.erp.ErpReceiptReceivable;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.entity.erp.ErpSupplierImportBatch;
import com.example.wms.entity.erp.ErpSupplierImportItem;
import com.example.wms.entity.erp.ErpSupplierType;
import com.example.wms.dto.erp.ErpFinanceSummary;
import com.example.wms.dto.erp.ErpCounterpartyFinanceSummaryView;
import com.example.wms.dto.erp.ErpCounterpartyFinanceDetailRow;
import com.example.wms.dto.erp.ErpPaymentCreateRequest;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpCounterpartySubjectMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPaymentMethodMapper;
import com.example.wms.mapper.erp.ErpPaymentPayableMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderItemMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnItemMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpReceiptReceivableMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.mapper.erp.ErpSupplierImportBatchMapper;
import com.example.wms.mapper.erp.ErpSupplierImportItemMapper;
import com.example.wms.mapper.erp.ErpSupplierTypeMapper;
import com.example.wms.service.TenantSettingService;
import com.example.wms.service.erp.ErpSupplierService;
import com.example.wms.service.erp.impl.ErpFinanceServiceImpl;
import com.example.wms.service.erp.impl.ErpPaymentServiceImpl;
import com.example.wms.service.erp.impl.ErpPurchaseOrderServiceImpl;
import com.example.wms.service.erp.impl.ErpPurchaseReturnServiceImpl;
import com.example.wms.service.erp.impl.ErpReceiptServiceImpl;
import com.example.wms.service.erp.impl.ErpSupplierServiceImpl;
import com.example.wms.service.erp.support.ExcelImportParser;
import com.example.wms.service.erp.support.ErpCostService;
import com.example.wms.service.erp.support.ErpCounterpartyGuardRules;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
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
    private ErpSettlementMethodMapper settlementMethodMapper;
    @Mock
    private ErpPaymentMethodMapper paymentMethodMapper;
    @Mock
    private TenantSettingService tenantSettingService;

    @Mock
    private ErpPaymentMapper paymentMapper;
    @Mock
    private ErpPaymentPayableMapper paymentPayableMapper;
    @Mock
    private ErpSupplierMapper supplierMapper;
    @Mock
    private ErpAccountsPayableMapper payableMapper;
    @Mock
    private ErpCounterpartySubjectMapper counterpartySubjectMapper;
    @Mock
    private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Mock
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;

    @Mock
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpStockBalanceMapper stockBalanceMapper;
    @Mock
    private ErpStockTxnMapper stockTxnMapper;
    @Mock
    private ErpSupplierImportBatchMapper supplierImportBatchMapper;
    @Mock
    private ErpSupplierImportItemMapper supplierImportItemMapper;
    @Mock
    private ErpSupplierTypeMapper supplierTypeMapper;
    @Mock
    private ObjectMapper objectMapper;

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
        payable.setTotalAmount(new BigDecimal("100"));
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
    void purchaseUnapproveIsRejectedBecausePurchaseUsesRedFlushOnly() {
        ErpPurchaseOrderServiceImpl service = purchaseOrderService();

        assertThatThrownBy(() -> service.unapprove(30L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("采购单仅支持红冲，不支持反审核");

        verify(purchaseOrderMapper, never()).selectOne(any());
    }

    @Test
    void paymentCreateAllowsNegativePurchaseReturnPayable() {
        ErpPaymentServiceImpl service = paymentService();
        ErpAccountsPayable payable = new ErpAccountsPayable();
        payable.setId(60L);
        payable.setTenantId(1L);
        payable.setSupplierId(6L);
        payable.setPurchaseReturnId(600L);
        payable.setOrderNo("PRT001");
        payable.setStatus("OPEN");
        payable.setTotalAmount(new BigDecimal("-80"));
        payable.setUnpaidAmount(new BigDecimal("-80"));

        when(payableMapper.selectList(any())).thenReturn(List.of(payable));
        when(paymentMapper.selectOne(any())).thenReturn(null);
        when(orderSequenceMapper.incrementAndGet(1L, "PAYMENT", java.time.LocalDate.now(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")))).thenReturn(1L);
        when(paymentPayableMapper.findByPaymentId(eq(1L), any())).thenReturn(List.of());

        service.create(new ErpPaymentCreateRequest(
            null,
            6L,
            60L,
            List.of(60L),
            null,
            new BigDecimal("-80"),
            BigDecimal.ZERO,
            null,
            "CASH",
            null,
            null,
            null
        ));

        ArgumentCaptor<ErpPayment> paymentCaptor = ArgumentCaptor.forClass(ErpPayment.class);
        ArgumentCaptor<ErpPaymentPayable> allocationCaptor = ArgumentCaptor.forClass(ErpPaymentPayable.class);
        verify(paymentMapper).insert(paymentCaptor.capture());
        verify(paymentPayableMapper).insert(allocationCaptor.capture());
        assertThat(paymentCaptor.getValue().getAmount()).isEqualByComparingTo("-80");
        assertThat(allocationCaptor.getValue().getAllocatedTotal()).isEqualByComparingTo("-80");
    }

    @Test
    void purchaseReturnCancelRedFlushesPayableAndRestoresReturnStock() {
        ErpPurchaseReturnServiceImpl service = purchaseReturnService();
        ErpPurchaseReturn order = new ErpPurchaseReturn();
        order.setId(70L);
        order.setTenantId(1L);
        order.setStatus("APPROVED");
        order.setReturnType("RETURN");
        order.setVersion(0L);

        ErpPurchaseReturnItem item = new ErpPurchaseReturnItem();
        item.setReturnId(70L);
        item.setProductId(700L);
        item.setQty(new BigDecimal("3"));

        ErpAccountsPayable payable = new ErpAccountsPayable();
        payable.setId(71L);
        payable.setTenantId(1L);
        payable.setPurchaseReturnId(70L);
        payable.setTotalAmount(new BigDecimal("-30"));
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setDiscountAmount(BigDecimal.ZERO);
        payable.setUnpaidAmount(new BigDecimal("-30"));
        payable.setStatus("OPEN");

        when(purchaseReturnMapper.selectOne(any())).thenReturn(order);
        when(purchaseReturnMapper.update(any(ErpPurchaseReturn.class), any(QueryWrapper.class))).thenReturn(1);
        when(purchaseReturnItemMapper.findByReturnId(1L, 70L)).thenReturn(List.of(item));
        when(payableMapper.findByPurchaseReturnId(1L, 70L)).thenReturn(payable);
        when(paymentPayableMapper.findByPayableId(1L, 71L)).thenReturn(List.of());
        ErpStockBalance updatedBalance = new ErpStockBalance();
        updatedBalance.setQtyOnHand(new BigDecimal("3"));
        when(stockBalanceMapper.upsertAddQty(eq(1L), eq(700L), any(), any(), eq(new BigDecimal("3")), any()))
            .thenReturn(updatedBalance);
        when(stockTxnMapper.findPurchaseReturnIssueUnitCost(1L, 70L, 700L)).thenReturn(new BigDecimal("12.34"));

        service.cancel(70L, "wrong qty");

        assertThat(order.getStatus()).isEqualTo("RED_FLUSHED");
        assertThat(order.getRemark()).contains("红冲原因：wrong qty");
        assertThat(payable.getStatus()).isEqualTo("RED_FLUSHED");
        assertThat(payable.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        ArgumentCaptor<com.example.wms.entity.erp.ErpStockTxn> txnCaptor = ArgumentCaptor.forClass(com.example.wms.entity.erp.ErpStockTxn.class);
        verify(stockTxnMapper).insert(txnCaptor.capture());
        assertThat(txnCaptor.getValue().getBizType()).isEqualTo("PURCHASE_RETURN_RED_FLUSH");
        assertThat(txnCaptor.getValue().getQtyDelta()).isEqualByComparingTo("3");
        assertThat(txnCaptor.getValue().getUnitCost()).isEqualByComparingTo("12.34");
    }

    @Test
    void purchaseReturnCancelAllowsAlreadyRedFlushedRefundPayment() {
        ErpPurchaseReturnServiceImpl service = purchaseReturnService();
        ErpPurchaseReturn order = new ErpPurchaseReturn();
        order.setId(80L);
        order.setTenantId(1L);
        order.setStatus("APPROVED");
        order.setReturnType("SCRAP");
        order.setVersion(0L);

        ErpPurchaseReturnItem item = new ErpPurchaseReturnItem();
        item.setReturnId(80L);
        item.setProductId(800L);
        item.setQty(new BigDecimal("2"));

        ErpAccountsPayable payable = new ErpAccountsPayable();
        payable.setId(81L);
        payable.setTenantId(1L);
        payable.setPurchaseReturnId(80L);
        payable.setTotalAmount(new BigDecimal("-20"));
        payable.setUnpaidAmount(BigDecimal.ZERO);
        payable.setStatus("OPEN");

        ErpPaymentPayable allocation = new ErpPaymentPayable();
        allocation.setPayableId(81L);
        allocation.setPaymentId(82L);

        ErpPayment redFlushPayment = new ErpPayment();
        redFlushPayment.setId(82L);
        redFlushPayment.setStatus("APPROVED");
        redFlushPayment.setAmount(new BigDecimal("20"));
        redFlushPayment.setDiscountAmount(BigDecimal.ZERO);

        when(purchaseReturnMapper.selectOne(any())).thenReturn(order);
        when(purchaseReturnMapper.update(any(ErpPurchaseReturn.class), any(QueryWrapper.class))).thenReturn(1);
        when(purchaseReturnItemMapper.findByReturnId(1L, 80L)).thenReturn(List.of(item));
        when(payableMapper.findByPurchaseReturnId(1L, 80L)).thenReturn(payable);
        when(paymentPayableMapper.findByPayableId(1L, 81L)).thenReturn(List.of(allocation));
        when(paymentMapper.selectList(any())).thenReturn(List.of(redFlushPayment));

        service.cancel(80L, "refund reversed");

        assertThat(order.getStatus()).isEqualTo("RED_FLUSHED");
        assertThat(payable.getStatus()).isEqualTo("RED_FLUSHED");
    }

    @Test
    void purchaseReturnListFiltersByBusinessOrderTime() {
        ErpPurchaseReturnServiceImpl service = purchaseReturnService();
        ArgumentCaptor<QueryWrapper<ErpPurchaseReturn>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        when(purchaseReturnMapper.selectList(any())).thenReturn(List.of());

        service.listAll(null, null, null, Instant.parse("2026-05-01T00:00:00Z"), Instant.parse("2026-05-02T00:00:00Z"));

        verify(purchaseReturnMapper).selectList(captor.capture());
        assertThat(String.valueOf(captor.getValue().getSqlSegment())).contains("order_at");
        assertThat(String.valueOf(captor.getValue().getSqlSegment())).doesNotContain("created_at");
    }

    @Test
    void purchaseCancelBlocksApprovedDiscountOnlyPayment() {
        ErpPurchaseOrderServiceImpl service = purchaseOrderService();
        ErpPurchaseOrder order = new ErpPurchaseOrder();
        order.setId(40L);
        order.setTenantId(1L);
        order.setStatus("APPROVED");

        ErpAccountsPayable payable = new ErpAccountsPayable();
        payable.setId(42L);
        payable.setTenantId(1L);
        payable.setPurchaseOrderId(40L);

        ErpPaymentPayable allocation = new ErpPaymentPayable();
        allocation.setPayableId(42L);
        allocation.setPaymentId(41L);

        ErpPayment payment = new ErpPayment();
        payment.setId(41L);
        payment.setTenantId(1L);
        payment.setStatus("APPROVED");
        payment.setAmount(BigDecimal.ZERO);
        payment.setDiscountAmount(new BigDecimal("5"));

        when(purchaseOrderMapper.selectOne(any())).thenReturn(order);
        when(payableMapper.findByPurchaseOrderId(1L, 40L)).thenReturn(payable);
        when(paymentPayableMapper.findByPayableId(1L, 42L)).thenReturn(List.of(allocation));
        when(paymentMapper.selectBatchIds(List.of(41L))).thenReturn(List.of(payment));

        assertThatThrownBy(() -> service.cancel(40L, "redo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("请先红冲付款单");
    }

    @Test
    void purchaseCancelBlocksApprovedPurchaseReturn() {
        ErpPurchaseOrderServiceImpl service = purchaseOrderService();
        ErpPurchaseOrder order = new ErpPurchaseOrder();
        order.setId(90L);
        order.setTenantId(1L);
        order.setStatus("APPROVED");

        ErpPurchaseReturn purchaseReturn = new ErpPurchaseReturn();
        purchaseReturn.setId(91L);
        purchaseReturn.setStatus("APPROVED");

        when(purchaseOrderMapper.selectOne(any())).thenReturn(order);
        when(payableMapper.findByPurchaseOrderId(1L, 90L)).thenReturn(null);
        when(purchaseReturnMapper.findApprovedByPurchaseOrderId(1L, 90L)).thenReturn(List.of(purchaseReturn));

        assertThatThrownBy(() -> service.cancel(90L, "redo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("请先红冲采购退货单");
    }

    @Test
    void purchaseReturnApproveRejectsQtyAboveOriginalPurchase() {
        ErpPurchaseReturnServiceImpl service = purchaseReturnService();
        ErpPurchaseReturn order = new ErpPurchaseReturn();
        order.setId(100L);
        order.setTenantId(1L);
        order.setStatus("DRAFT");
        order.setReturnType("SCRAP");
        order.setPurchaseOrderId(101L);
        order.setSupplierId(10L);
        order.setTotalAmountInclTax(new BigDecimal("30"));
        order.setPaidAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setVersion(0L);

        ErpPurchaseReturnItem returnItem = new ErpPurchaseReturnItem();
        returnItem.setReturnId(100L);
        returnItem.setProductId(1000L);
        returnItem.setQty(new BigDecimal("4"));
        returnItem.setAmountInclTax(new BigDecimal("40"));

        ErpPurchaseOrder purchaseOrder = new ErpPurchaseOrder();
        purchaseOrder.setId(101L);
        purchaseOrder.setSupplierId(10L);
        purchaseOrder.setStatus("APPROVED");

        ErpPurchaseOrderItem purchaseItem = new ErpPurchaseOrderItem();
        purchaseItem.setProductId(1000L);
        purchaseItem.setQty(new BigDecimal("3"));
        purchaseItem.setAmountInclTax(new BigDecimal("30"));

        when(purchaseReturnMapper.selectOne(any())).thenReturn(order);
        when(purchaseReturnItemMapper.findByReturnId(1L, 100L)).thenReturn(List.of(returnItem));
        when(purchaseOrderMapper.selectOne(any())).thenReturn(purchaseOrder);
        when(purchaseOrderItemMapper.findByOrderId(1L, 101L)).thenReturn(List.of(purchaseItem));
        when(purchaseReturnMapper.findApprovedByPurchaseOrderId(1L, 101L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.approve(100L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("商品退货数量不能超过原采购可退数量");
    }

    @Test
    void purchaseReturnApproveCreatesNegativePayableAndAutoRefundPayment() {
        ErpPurchaseReturnServiceImpl service = purchaseReturnService();
        ErpPurchaseReturn order = new ErpPurchaseReturn();
        order.setId(110L);
        order.setTenantId(1L);
        order.setOrderNo("PR110");
        order.setStatus("DRAFT");
        order.setReturnType("SCRAP");
        order.setPurchaseOrderId(111L);
        order.setSupplierId(11L);
        order.setSettlementMethod("CASH");
        order.setTotalAmountInclTax(new BigDecimal("50"));
        order.setPaidAmount(new BigDecimal("20"));
        order.setDiscountAmount(new BigDecimal("5"));
        order.setVersion(0L);

        ErpPurchaseReturnItem returnItem = new ErpPurchaseReturnItem();
        returnItem.setReturnId(110L);
        returnItem.setProductId(1100L);
        returnItem.setQty(new BigDecimal("2"));
        returnItem.setAmountInclTax(new BigDecimal("50"));

        ErpPurchaseOrder purchaseOrder = new ErpPurchaseOrder();
        purchaseOrder.setId(111L);
        purchaseOrder.setSupplierId(11L);
        purchaseOrder.setStatus("APPROVED");

        ErpPurchaseOrderItem purchaseItem = new ErpPurchaseOrderItem();
        purchaseItem.setProductId(1100L);
        purchaseItem.setQty(new BigDecimal("2"));
        purchaseItem.setAmountInclTax(new BigDecimal("50"));

        ErpAccountsPayable originalPayable = new ErpAccountsPayable();
        originalPayable.setId(112L);

        when(purchaseReturnMapper.selectOne(any())).thenReturn(order);
        when(purchaseReturnMapper.update(any(ErpPurchaseReturn.class), any(QueryWrapper.class))).thenReturn(1);
        when(purchaseReturnItemMapper.findByReturnId(1L, 110L)).thenReturn(List.of(returnItem));
        when(purchaseOrderMapper.selectOne(any())).thenReturn(purchaseOrder);
        when(purchaseOrderItemMapper.findByOrderId(1L, 111L)).thenReturn(List.of(purchaseItem));
        when(purchaseReturnMapper.findApprovedByPurchaseOrderId(1L, 111L)).thenReturn(List.of());
        when(payableMapper.findByPurchaseOrderId(1L, 111L)).thenReturn(originalPayable);
        when(paymentPayableMapper.sumApprovedAllocatedAmountByPayableId(1L, 112L)).thenReturn(new BigDecimal("50"));
        when(payableMapper.findByPurchaseReturnId(1L, 110L)).thenReturn(null);
        when(orderSequenceMapper.incrementAndGet(eq(1L), eq("AP_RETURN"), any())).thenReturn(1L);
        when(orderSequenceMapper.incrementAndGet(eq(1L), eq("PAYMENT"), any())).thenReturn(1L);

        service.approve(110L);

        ArgumentCaptor<ErpAccountsPayable> payableCaptor = ArgumentCaptor.forClass(ErpAccountsPayable.class);
        ArgumentCaptor<ErpPayment> paymentCaptor = ArgumentCaptor.forClass(ErpPayment.class);
        ArgumentCaptor<ErpPaymentPayable> allocationCaptor = ArgumentCaptor.forClass(ErpPaymentPayable.class);
        verify(payableMapper).insert(payableCaptor.capture());
        verify(paymentMapper).insert(paymentCaptor.capture());
        verify(paymentPayableMapper).insert(allocationCaptor.capture());
        assertThat(payableCaptor.getValue().getTotalAmount()).isEqualByComparingTo("-50");
        assertThat(payableCaptor.getValue().getPaidAmount()).isEqualByComparingTo("-20");
        assertThat(payableCaptor.getValue().getDiscountAmount()).isEqualByComparingTo("-5");
        assertThat(payableCaptor.getValue().getUnpaidAmount()).isEqualByComparingTo("-25");
        assertThat(paymentCaptor.getValue().getAmount()).isEqualByComparingTo("-20");
        assertThat(paymentCaptor.getValue().getDiscountAmount()).isEqualByComparingTo("-5");
        assertThat(allocationCaptor.getValue().getAllocatedTotal()).isEqualByComparingTo("-25");
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

    @Test
    void counterpartySubjectSummaryUsesTenantScopedAggregationView() {
        ErpFinanceServiceImpl service = financeService();
        ErpCounterpartyFinanceSummaryView summary = new ErpCounterpartyFinanceSummaryView(
            501L,
            "昆明坤润汽车维修服务有限公司",
            new BigDecimal("1200.50"),
            new BigDecimal("300.25"),
            new BigDecimal("900.25"),
            2,
            1
        );
        when(counterpartySubjectMapper.listFinanceSummaries(1L)).thenReturn(List.of(summary));

        List<ErpCounterpartyFinanceSummaryView> result = service.listCounterpartySubjectSummaries();

        assertThat(result).containsExactly(summary);
        verify(counterpartySubjectMapper).listFinanceSummaries(1L);
    }

    @Test
    void counterpartySubjectSummaryReturnsZeroValuesForSubjectsWithoutReceivableOrPayable() {
        ErpFinanceServiceImpl service = financeService();
        ErpCounterpartyFinanceSummaryView summary = new ErpCounterpartyFinanceSummaryView(
            502L,
            "仅建主体未挂财务单据",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            0,
            0
        );
        when(counterpartySubjectMapper.listFinanceSummaries(1L)).thenReturn(List.of(summary));

        List<ErpCounterpartyFinanceSummaryView> result = service.listCounterpartySubjectSummaries();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getReceivableTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getPayableTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getNetAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    @Test
    void counterpartySubjectDetailUsesTenantScopedDetailQuery() {
        ErpFinanceServiceImpl service = financeService();
        ErpCounterpartyFinanceDetailRow detail = new ErpCounterpartyFinanceDetailRow(
            "RECEIVABLE",
            901L,
            "AR-901",
            11L,
            "CU001",
            "昆明坤润汽车维修服务有限公司",
            new BigDecimal("1200.50"),
            new BigDecimal("300.25"),
            "OPEN",
            Instant.parse("2026-05-28T08:00:00Z")
        );
        when(counterpartySubjectMapper.listFinanceDetailRows(1L, 501L)).thenReturn(List.of(detail));

        List<ErpCounterpartyFinanceDetailRow> result = service.listCounterpartySubjectDetails(501L);

        assertThat(result).containsExactly(detail);
        verify(counterpartySubjectMapper).listFinanceDetailRows(1L, 501L);
    }

    @Test
    void financeSummaryExcludesRedFlushedRowsWhenAggregatingReceivableAndPayable() {
        ErpFinanceServiceImpl service = financeService();
        when(receivableMapper.selectObjs(any())).thenReturn(List.of(new BigDecimal("120")));
        when(payableMapper.selectObjs(any())).thenReturn(List.of(new BigDecimal("35")));

        ErpFinanceSummary result = service.getSummary();

        assertThat(result.getCustomerDebtTotal()).isEqualByComparingTo("120");
        assertThat(result.getSupplierDebtTotal()).isEqualByComparingTo("35");
    }

    @Test
    void importSuppliersFallsBackToBuiltinUncategorizedTypeAndRecordsWarning() throws Exception {
        ErpSupplierServiceImpl service = supplierService();
        ErpSupplierType uncategorized = new ErpSupplierType();
        uncategorized.setId(88L);
        uncategorized.setCode(ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE);
        uncategorized.setName(ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_NAME);
        when(supplierTypeMapper.selectList(any())).thenReturn(List.of());
        when(supplierTypeMapper.findByCode(1L, ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE)).thenReturn(uncategorized);
        when(settlementMethodMapper.selectList(any())).thenReturn(List.of());
        when(supplierMapper.findByCode(1L, "SU001")).thenReturn(null);

        var result = service.importSuppliers(
            new MockMultipartFile(
                "file",
                "supplier.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                supplierImportWorkbookBytes("不存在的类型")
            ),
            "历史供应商表"
        );

        assertThat(result.status()).isEqualTo("PROCESSING");
        verify(supplierImportItemMapper, timeout(3_000)).insertBatch(any());
    }

    @Test
    void listImportBatchesReturnsBatchSummaryWithManualFollowUpCounters() {
        ErpSupplierServiceImpl service = supplierService();
        ErpSupplierImportBatch batch = new ErpSupplierImportBatch();
        batch.setId(1L);
        batch.setBatchNo("SI20260529010101");
        batch.setSourceName("历史供应商表");
        batch.setImportMode("PASTE_TABLE");
        batch.setTotalCount(10);
        batch.setSuccessCount(8);
        batch.setFailedCount(2);
        batch.setUncategorizedCount(3);
        batch.setSettlementUnmatchedCount(1);
        batch.setPendingSubjectMergeCount(4);
        batch.setStatus("DONE");
        batch.setSummary("成功 8 行，失败 2 行");
        batch.setCreatedBy("system");
        batch.setCreatedAt(Instant.parse("2026-05-29T01:00:00Z"));
        when(supplierImportBatchMapper.selectList(any())).thenReturn(List.of(batch));

        var result = service.listImportBatches();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.uncategorizedCount()).isEqualTo(3);
            assertThat(item.settlementUnmatchedCount()).isEqualTo(1);
            assertThat(item.pendingSubjectMergeCount()).isEqualTo(4);
        });
    }

    @Test
    void listImportBatchItemsRejectsCrossTenantBatchLookup() {
        ErpSupplierServiceImpl service = supplierService();
        when(supplierImportBatchMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.listImportBatchItems(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("导入批次不存在");
    }

    @Test
    void supplierImportEndpointUsesMultipartExcelUploadContract() throws Exception {
        Method controllerMethod = ErpSupplierController.class.getMethod("importSuppliers", MultipartFile.class, String.class);
        PostMapping postMapping = controllerMethod.getAnnotation(PostMapping.class);
        Parameter[] controllerParameters = controllerMethod.getParameters();

        assertThat(postMapping).isNotNull();
        assertThat(postMapping.value()).containsExactly("/import");
        assertThat(postMapping.consumes()).contains(MediaType.MULTIPART_FORM_DATA_VALUE);
        assertThat(controllerParameters).hasSize(2);
        assertThat(controllerParameters[0].getType()).isEqualTo(MultipartFile.class);
        assertThat(controllerParameters[0].getAnnotation(RequestParam.class)).isNotNull();
        assertThat(controllerParameters[0].getAnnotation(RequestParam.class).value()).isEqualTo("file");
        assertThat(controllerParameters[1].getType()).isEqualTo(String.class);
        assertThat(controllerParameters[1].getAnnotation(RequestParam.class)).isNotNull();
        assertThat(controllerParameters[1].getAnnotation(RequestParam.class).value()).isEqualTo("sourceName");
        assertThat(controllerParameters[1].getAnnotation(RequestParam.class).required()).isFalse();

        Method serviceMethod = ErpSupplierService.class.getMethod("importSuppliers", MultipartFile.class, String.class);
        assertThat(serviceMethod.getReturnType().getSimpleName()).isEqualTo("ErpSupplierImportResult");
    }

    @Test
    void importSuppliersParsesExcelUploadAndRecordsExcelBatchMode() throws Exception {
        ErpSupplierServiceImpl service = supplierService();
        when(supplierTypeMapper.selectList(any())).thenReturn(List.of());
        when(supplierTypeMapper.findByCode(1L, ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE)).thenReturn(null);
        when(settlementMethodMapper.selectList(any())).thenReturn(List.of());
        when(supplierMapper.findByCode(1L, "SU001")).thenReturn(null);

        Method importMethod = ErpSupplierServiceImpl.class.getMethod("importSuppliers", MultipartFile.class, String.class);
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "supplier.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            supplierImportWorkbookBytes()
        );

        Object rawResult = importMethod.invoke(service, file, "历史供应商表");

        assertThat(rawResult).isNotNull();
        ArgumentCaptor<ErpSupplierImportBatch> batchCaptor = ArgumentCaptor.forClass(ErpSupplierImportBatch.class);
        verify(supplierImportBatchMapper).insert(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getSourceName()).isEqualTo("历史供应商表");
        assertThat(batchCaptor.getValue().getImportMode()).isEqualTo("EXCEL_UPLOAD");
        assertThat(batchCaptor.getValue().getRawPayload().toString()).contains("supplier.xlsx");
        verify(supplierImportItemMapper, never()).insert(any(ErpSupplierImportItem.class));
        verify(supplierImportItemMapper, never()).insertBatch(any());
    }

    @Test
    void importSuppliersReturnsProcessingBatchBeforeRowUpsertWorkCompletes() throws Exception {
        List<Runnable> queuedTasks = new ArrayList<>();
        ErpSupplierServiceImpl service = supplierService(queuedTasks::add, null);
        doAnswer(invocation -> {
            ErpSupplierImportBatch batch = invocation.getArgument(0);
            batch.setId(77L);
            return 1;
        }).when(supplierImportBatchMapper).insert(any(ErpSupplierImportBatch.class));

        var result = service.importSuppliers(
            new MockMultipartFile(
                "file",
                "supplier.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                supplierImportWorkbookBytesForTwoRows()
            ),
            "历史供应商表"
        );

        assertThat(result.batchId()).isEqualTo(77L);
        assertThat(result.status()).isEqualTo("PROCESSING");
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.successCount()).isZero();
        assertThat(result.failedCount()).isZero();
        assertThat(queuedTasks).hasSize(1);
        verify(supplierMapper, never()).insert(any(ErpSupplier.class));
        verify(supplierImportItemMapper, never()).insert(any(ErpSupplierImportItem.class));
        verify(supplierImportItemMapper, never()).insertBatch(any());
    }

    @Test
    void importSuppliersDoesNotFailWhenSettlementMethodIsUnmatched() throws Exception {
        ErpSupplierServiceImpl service = supplierService();
        ErpSupplierType uncategorized = new ErpSupplierType();
        uncategorized.setId(88L);
        uncategorized.setCode(ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE);
        uncategorized.setName(ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_NAME);
        when(supplierTypeMapper.selectList(any())).thenReturn(List.of());
        when(supplierTypeMapper.findByCode(1L, ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE)).thenReturn(uncategorized);
        when(settlementMethodMapper.selectList(any())).thenReturn(List.of());
        when(supplierMapper.findByCode(1L, "SU001")).thenReturn(null);

        var result = service.importSuppliers(
            new MockMultipartFile(
                "file",
                "supplier.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                supplierImportWorkbookBytesWithSettlement("月结")
            ),
            "历史供应商表"
        );

        assertThat(result.status()).isEqualTo("PROCESSING");
        ArgumentCaptor<List<ErpSupplierImportItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(supplierImportItemMapper, timeout(3_000)).insertBatch(itemCaptor.capture());
        assertThat(itemCaptor.getValue()).singleElement().satisfies(item -> {
            assertThat(item.getStatus()).isEqualTo("SUCCESS");
            assertThat(item.getWarningMessage()).contains("默认结算方式未匹配");
        });
    }

    @Test
    void importSuppliersBuildsMultipleContactsFromExpandedExcelColumns() throws Exception {
        ErpSupplierServiceImpl service = supplierService();
        ErpSupplierType uncategorized = new ErpSupplierType();
        uncategorized.setId(88L);
        uncategorized.setCode(ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE);
        uncategorized.setName(ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_NAME);
        when(supplierTypeMapper.selectList(any())).thenReturn(List.of());
        when(supplierTypeMapper.findByCode(1L, ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE)).thenReturn(uncategorized);
        when(settlementMethodMapper.selectList(any())).thenReturn(List.of());
        when(supplierMapper.findByCode(1L, "SU001")).thenReturn(null);

        var result = service.importSuppliers(
            new MockMultipartFile(
                "file",
                "supplier.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                supplierImportWorkbookBytesWithExpandedContacts()
            ),
            "历史供应商表"
        );

        assertThat(result.status()).isEqualTo("PROCESSING");
        ArgumentCaptor<ErpSupplier> captor = ArgumentCaptor.forClass(ErpSupplier.class);
        verify(supplierMapper, timeout(3_000)).insert(captor.capture());
        ErpSupplier inserted = captor.getValue();

        assertThat(inserted.getContact()).isEqualTo("张三");
        assertThat(inserted.getMobile()).isEqualTo("13800138000");
        assertThat(inserted.getPhone()).isEqualTo("0871-6666666");
        assertThat(inserted.getContacts()).isNotNull();
        assertThat(inserted.getContacts().toString()).contains("\"name\":\"张三\"");
        assertThat(inserted.getContacts().toString()).contains("\"mobile\":\"13800138000\"");
        assertThat(inserted.getContacts().toString()).contains("\"phone\":\"0871-6666666\"");
        assertThat(inserted.getContacts().toString()).contains("\"phone\":\"0871-7777777\"");
        assertThat(inserted.getContacts().toString()).contains("\"name\":\"李四\"");
        assertThat(inserted.getContactInfo()).contains("张三");
        assertThat(inserted.getContactInfo()).contains("李四");
    }

    @Test
    void importSuppliersSplitsLegacyContactInfoIntoMultipleContacts() throws Exception {
        ErpSupplierServiceImpl service = supplierService();
        ErpSupplierType uncategorized = new ErpSupplierType();
        uncategorized.setId(88L);
        uncategorized.setCode(ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE);
        uncategorized.setName(ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_NAME);
        when(supplierTypeMapper.selectList(any())).thenReturn(List.of());
        when(supplierTypeMapper.findByCode(1L, ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE)).thenReturn(uncategorized);
        when(settlementMethodMapper.selectList(any())).thenReturn(List.of());
        when(supplierMapper.findByCode(1L, "SU001")).thenReturn(null);

        var result = service.importSuppliers(
            new MockMultipartFile(
                "file",
                "supplier.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                supplierImportWorkbookBytesWithLegacyContactInfo("67268918/13700651938")
            ),
            "历史供应商表"
        );

        assertThat(result.status()).isEqualTo("PROCESSING");
        ArgumentCaptor<ErpSupplier> captor = ArgumentCaptor.forClass(ErpSupplier.class);
        verify(supplierMapper, timeout(3_000)).insert(captor.capture());
        ErpSupplier inserted = captor.getValue();

        assertThat(inserted.getContact()).isEqualTo("鑫泰中原");
        assertThat(inserted.getPhone()).isEqualTo("67268918");
        assertThat(inserted.getMobile()).isEqualTo("13700651938");
        assertThat(inserted.getContacts()).isNotNull();
        assertThat(inserted.getContacts().toString()).contains("\"phone\":\"67268918\"");
        assertThat(inserted.getContacts().toString()).contains("\"mobile\":\"13700651938\"");
    }

    @Test
    void importSuppliersKeepsMultipleMobilesFromLegacyContactInfo() throws Exception {
        ErpSupplierServiceImpl service = supplierService();
        ErpSupplierType uncategorized = new ErpSupplierType();
        uncategorized.setId(88L);
        uncategorized.setCode(ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE);
        uncategorized.setName(ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_NAME);
        when(supplierTypeMapper.selectList(any())).thenReturn(List.of());
        when(supplierTypeMapper.findByCode(1L, ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE)).thenReturn(uncategorized);
        when(settlementMethodMapper.selectList(any())).thenReturn(List.of());
        when(supplierMapper.findByCode(1L, "SU001")).thenReturn(null);

        var result = service.importSuppliers(
            new MockMultipartFile(
                "file",
                "supplier.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                supplierImportWorkbookBytesWithLegacyContactInfo("15825267218/15825267219")
            ),
            "历史供应商表"
        );

        assertThat(result.status()).isEqualTo("PROCESSING");
        ArgumentCaptor<ErpSupplier> captor = ArgumentCaptor.forClass(ErpSupplier.class);
        verify(supplierMapper, timeout(3_000)).insert(captor.capture());
        ErpSupplier inserted = captor.getValue();

        assertThat(inserted.getContacts()).isNotNull();
        assertThat(inserted.getContacts().toString()).contains("\"mobile\":\"15825267218\"");
        assertThat(inserted.getContacts().toString()).contains("\"mobile\":\"15825267219\"");
        assertThat(inserted.getPhone()).isNull();
        assertThat(inserted.getMobile()).isEqualTo("15825267218");
        assertThat(inserted.getContactInfo()).contains("15825267218");
        assertThat(inserted.getContactInfo()).contains("15825267219");
    }

    @Test
    void importSuppliersKeepsDuplicateMobilesFromLegacyContactInfoWithoutFallingBackToPhone() throws Exception {
        ErpSupplierServiceImpl service = supplierService();
        ErpSupplierType uncategorized = new ErpSupplierType();
        uncategorized.setId(88L);
        uncategorized.setCode(ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE);
        uncategorized.setName(ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_NAME);
        when(supplierTypeMapper.selectList(any())).thenReturn(List.of());
        when(supplierTypeMapper.findByCode(1L, ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE)).thenReturn(uncategorized);
        when(settlementMethodMapper.selectList(any())).thenReturn(List.of());
        when(supplierMapper.findByCode(1L, "SU001")).thenReturn(null);

        var result = service.importSuppliers(
            new MockMultipartFile(
                "file",
                "supplier.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                supplierImportWorkbookBytesWithLegacyContactInfo("15368088819/15368088819")
            ),
            "历史供应商表"
        );

        assertThat(result.status()).isEqualTo("PROCESSING");
        ArgumentCaptor<ErpSupplier> captor = ArgumentCaptor.forClass(ErpSupplier.class);
        verify(supplierMapper, timeout(3_000)).insert(captor.capture());
        ErpSupplier inserted = captor.getValue();

        assertThat(inserted.getPhone()).isNull();
        assertThat(inserted.getMobile()).isEqualTo("15368088819");
        assertThat(inserted.getContacts()).isNotNull();
        assertThat(inserted.getContacts().toString()).doesNotContain("\"phone\":\"15368088819\"");
        assertThat(inserted.getContacts().toString()).contains("\"mobile\":\"15368088819\"");
    }

    @Test
    void importSuppliersPrefetchesReferenceDataAndExistingSuppliersInsteadOfQueryingEachRow() throws Exception {
        ErpSupplierServiceImpl service = supplierService();
        ErpSupplierType typed = new ErpSupplierType();
        typed.setId(9L);
        typed.setName("挂账");
        when(supplierTypeMapper.selectList(any())).thenReturn(List.of(typed));

        when(settlementMethodMapper.selectList(any())).thenReturn(List.of());
        when(supplierMapper.selectList(any())).thenReturn(List.of());

        var result = service.importSuppliers(
            new MockMultipartFile(
                "file",
                "supplier.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                supplierImportWorkbookBytesForTwoRows()
            ),
            "历史供应商表"
        );

        assertThat(result.status()).isEqualTo("PROCESSING");
        verify(supplierMapper, timeout(3_000).times(2)).insert(any(ErpSupplier.class));
        verify(settlementMethodMapper, timeout(3_000).times(1)).selectList(any());
        verify(supplierTypeMapper, timeout(3_000).times(1)).selectList(any());
        verify(supplierMapper, timeout(3_000).times(1)).selectList(any());
        verify(supplierMapper, never()).findByCode(any(), any());
    }

    private ErpReceiptServiceImpl receiptService() {
        return new ErpReceiptServiceImpl(
            receiptMapper,
            receiptReceivableMapper,
            customerMapper,
            receivableMapper,
            saleOrderMapper,
            settlementMethodMapper,
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
            settlementMethodMapper,
            orderSequenceMapper,
            systemConfigMapper
        );
    }

    private ErpFinanceServiceImpl financeService() {
        return new ErpFinanceServiceImpl(
            receivableMapper,
            payableMapper,
            counterpartySubjectMapper
        );
    }

    private ErpSupplierServiceImpl supplierService() {
        return supplierService(Runnable::run, null);
    }

    private ErpSupplierServiceImpl supplierService(Executor importExecutor,
                                                   org.springframework.transaction.support.TransactionOperations transactionOperations) {
        return new ErpSupplierServiceImpl(
            supplierMapper,
            purchaseOrderMapper,
            purchaseReturnMapper,
            paymentMapper,
            payableMapper,
            settlementMethodMapper,
            paymentMethodMapper,
            orderSequenceMapper,
            systemConfigMapper,
            supplierTypeMapper,
            supplierImportBatchMapper,
            supplierImportItemMapper,
            null,
            new ObjectMapper(),
            new ExcelImportParser(),
            importExecutor,
            transactionOperations
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
            paymentMethodMapper,
            paymentPayableMapper,
            purchaseReturnMapper,
            settlementMethodMapper,
            supplierMapper,
            costService(),
            tenantSettingService
        );
    }

    private ErpPurchaseReturnServiceImpl purchaseReturnService() {
        return new ErpPurchaseReturnServiceImpl(
            purchaseReturnMapper,
            purchaseReturnItemMapper,
            purchaseOrderMapper,
            purchaseOrderItemMapper,
            productMapper,
            stockBalanceMapper,
            stockTxnMapper,
            orderSequenceMapper,
            payableMapper,
            paymentMapper,
            paymentMethodMapper,
            paymentPayableMapper,
            settlementMethodMapper,
            supplierMapper,
            systemConfigMapper,
            costService(),
            tenantSettingService
        );
    }

    private ErpCostService costService() {
        return new ErpCostService(productMapper, stockBalanceMapper);
    }

    private byte[] supplierImportWorkbookBytes() throws IOException {
        return supplierImportWorkbookBytes("");
    }

    private byte[] supplierImportWorkbookBytes(String supplierTypeName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("编码");
            header.createCell(1).setCellValue("名称");
            header.createCell(2).setCellValue("默认结算方式");
            header.createCell(3).setCellValue("客户类型");
            header.createCell(4).setCellValue("联系方式");

            Row row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue("SU001");
            row.createCell(1).setCellValue("昆明坤润汽车维修服务有限公司");
            row.createCell(2).setCellValue("");
            row.createCell(3).setCellValue(supplierTypeName);
            row.createCell(4).setCellValue("13800138000");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] supplierImportWorkbookBytesWithSettlement(String settlementName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("编码");
            header.createCell(1).setCellValue("名称");
            header.createCell(2).setCellValue("默认结算方式");
            header.createCell(3).setCellValue("客户类型");
            header.createCell(4).setCellValue("联系方式");

            Row row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue("SU001");
            row.createCell(1).setCellValue("昆明坤润汽车维修服务有限公司");
            row.createCell(2).setCellValue(settlementName);
            row.createCell(3).setCellValue("");
            row.createCell(4).setCellValue("13800138000");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] supplierImportWorkbookBytesWithExpandedContacts() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("编码");
            header.createCell(1).setCellValue("名称");
            header.createCell(2).setCellValue("默认结算方式");
            header.createCell(3).setCellValue("客户类型");
            header.createCell(4).setCellValue("联系人1");
            header.createCell(5).setCellValue("手机1");
            header.createCell(6).setCellValue("电话1");
            header.createCell(7).setCellValue("电话1-2");
            header.createCell(8).setCellValue("联系人2");
            header.createCell(9).setCellValue("手机2");

            Row row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue("SU001");
            row.createCell(1).setCellValue("昆明坤润汽车维修服务有限公司");
            row.createCell(2).setCellValue("");
            row.createCell(3).setCellValue("");
            row.createCell(4).setCellValue("张三");
            row.createCell(5).setCellValue("13800138000");
            row.createCell(6).setCellValue("0871-6666666");
            row.createCell(7).setCellValue("0871-7777777");
            row.createCell(8).setCellValue("李四");
            row.createCell(9).setCellValue("13900139000");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] supplierImportWorkbookBytesWithLegacyContactInfo(String contactInfo) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("编码");
            header.createCell(1).setCellValue("名称");
            header.createCell(2).setCellValue("默认结算方式");
            header.createCell(3).setCellValue("客户类型");
            header.createCell(4).setCellValue("联系方式");
            header.createCell(5).setCellValue("联系人");

            Row row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue("SU001");
            row.createCell(1).setCellValue("昆明坤润汽车维修服务有限公司");
            row.createCell(2).setCellValue("");
            row.createCell(3).setCellValue("");
            row.createCell(4).setCellValue(contactInfo);
            row.createCell(5).setCellValue("鑫泰中原");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] supplierImportWorkbookBytesForTwoRows() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("编码");
            header.createCell(1).setCellValue("名称");
            header.createCell(2).setCellValue("默认结算方式");
            header.createCell(3).setCellValue("客户类型");
            header.createCell(4).setCellValue("联系方式");

            Row firstRow = workbook.getSheetAt(0).createRow(1);
            firstRow.createCell(0).setCellValue("SU001");
            firstRow.createCell(1).setCellValue("供应商一");
            firstRow.createCell(2).setCellValue("");
            firstRow.createCell(3).setCellValue("挂账");
            firstRow.createCell(4).setCellValue("13800138000");

            Row secondRow = workbook.getSheetAt(0).createRow(2);
            secondRow.createCell(0).setCellValue("SU002");
            secondRow.createCell(1).setCellValue("供应商二");
            secondRow.createCell(2).setCellValue("");
            secondRow.createCell(3).setCellValue("挂账");
            secondRow.createCell(4).setCellValue("13900139000");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
