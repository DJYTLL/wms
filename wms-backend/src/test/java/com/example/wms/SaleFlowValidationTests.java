package com.example.wms;

import com.example.wms.dto.erp.ErpSaleOrderCreateRequest;
import com.example.wms.dto.erp.ErpSaleOrderItemRequest;
import com.example.wms.dto.erp.ErpSaleOrderUpdateRequest;
import com.example.wms.dto.erp.ErpSaleReturnCreateRequest;
import com.example.wms.dto.erp.ErpSaleReturnItemRequest;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpAccountsReceivable;
import com.example.wms.entity.erp.ErpReceipt;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.entity.erp.ErpSaleOrderItem;
import com.example.wms.entity.erp.ErpSaleReturn;
import com.example.wms.entity.erp.ErpSaleReturnItem;
import com.example.wms.entity.erp.ErpSettlementMethod;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpReceiptReceivableMapper;
import com.example.wms.mapper.erp.ErpSaleOrderItemMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSaleReturnItemMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.impl.ErpSaleOrderServiceImpl;
import com.example.wms.service.erp.impl.ErpSaleReturnServiceImpl;
import com.example.wms.service.erp.support.ErpCostService;
import com.example.wms.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
    private ErpCustomerMapper customerMapper;
    @Mock
    private ErpWarehouseMapper warehouseMapper;
    @Mock
    private ErpLocationMapper locationMapper;
    @Mock
    private ErpSettlementMethodMapper settlementMethodMapper;
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
        stubValidMasterData();
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
        stubValidMasterData();
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
    void saleCreateRejectsDisabledProduct() {
        ErpSaleOrderServiceImpl service = saleOrderService();
        stubValidMasterData();
        when(productMapper.selectOne(any())).thenReturn(disabledProduct(100L));

        ErpSaleOrderCreateRequest request = new ErpSaleOrderCreateRequest(
            "SO-003",
            "2026-05-12 09:00:00",
            10L,
            "CASH",
            null,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            List.of(saleOrderItemRequest(100L, "100")),
            null
        );

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("商品已停用，不能新增引用");
    }

    @Test
    void saleUpdateAllowsExistingDisabledProductButRejectsNewDisabledProduct() {
        ErpSaleOrderServiceImpl service = saleOrderService();
        stubValidMasterData();
        ErpSaleOrder order = draftSaleOrder(77L, 10L);
        when(saleOrderMapper.selectOne(any())).thenReturn(order);
        when(saleOrderItemMapper.findByOrderId(1L, 77L)).thenReturn(List.of(soldItem(100L, "1")));
        when(saleOrderMapper.update(any(), any())).thenReturn(1);
        when(productMapper.selectOne(any())).thenReturn(disabledProduct(100L));
        when(stockBalanceMapper.addReservedQtyIfEnough(1L, 100L, 1L, 1L, BigDecimal.ONE, "system"))
            .thenReturn(stockBalance("1", "1"));
        when(stockBalanceMapper.addReservedQtyIfEnough(1L, 100L, 1L, 1L, new BigDecimal("-1"), "system"))
            .thenReturn(stockBalance("1", "0"));

        ErpSaleOrderUpdateRequest keepExistingRequest = new ErpSaleOrderUpdateRequest(
            "SO-077",
            "2026-05-12 09:00:00",
            10L,
            "CASH",
            null,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            List.of(saleOrderItemRequest(100L, "100")),
            null
        );

        service.update(77L, keepExistingRequest);

        when(productMapper.selectOne(any())).thenReturn(disabledProduct(200L));
        ErpSaleOrderUpdateRequest newDisabledRequest = new ErpSaleOrderUpdateRequest(
            "SO-077",
            "2026-05-12 09:00:00",
            10L,
            "CASH",
            null,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            List.of(saleOrderItemRequest(200L, "100")),
            null
        );

        assertThatThrownBy(() -> service.update(77L, newDisabledRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("商品已停用，不能新增引用");
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
        stubValidMasterData();
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

    @Test
    void saleReturnCreateRejectsAmountExceedingOriginalSaleAmount() {
        ErpSaleReturnServiceImpl service = saleReturnService();
        when(saleOrderMapper.selectOne(any())).thenReturn(approvedSaleOrder(99L, 10L));
        when(saleOrderItemMapper.findByOrderId(1L, 99L)).thenReturn(List.of(soldItem(100L, "2")));
        when(saleReturnMapper.selectList(any())).thenReturn(List.of());

        ErpSaleReturnCreateRequest request = new ErpSaleReturnCreateRequest(
            "SR-003",
            "2026-05-12 10:00:00",
            "RESTOCK",
            10L,
            99L,
            "CASH",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            List.of(new ErpSaleReturnItemRequest(
                100L,
                1L,
                1L,
                BigDecimal.ONE,
                new BigDecimal("300"),
                null,
                BigDecimal.ZERO,
                1,
                null
            )),
            null
        );

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("商品退货金额不能超过原销售可退金额");
    }

    @Test
    void saleReturnCreateRejectsUnitPriceExceedingOriginalSaleUnitPrice() {
        ErpSaleReturnServiceImpl service = saleReturnService();
        when(saleOrderMapper.selectOne(any())).thenReturn(approvedSaleOrder(99L, 10L));
        when(saleOrderItemMapper.findByOrderId(1L, 99L)).thenReturn(List.of(soldItem(100L, "2")));
        when(saleReturnMapper.selectList(any())).thenReturn(List.of());

        ErpSaleReturnCreateRequest request = new ErpSaleReturnCreateRequest(
            "SR-004",
            "2026-05-12 10:00:00",
            "RESTOCK",
            10L,
            99L,
            "CASH",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            List.of(new ErpSaleReturnItemRequest(
                100L,
                1L,
                1L,
                BigDecimal.ONE,
                new BigDecimal("150"),
                null,
                BigDecimal.ZERO,
                1,
                null
            )),
            null
        );

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("商品退货单价不能高于原销售单价");
    }

    @Test
    void saleReturnApproveRejectsRefundAmountExceedingCollectedCash() {
        ErpSaleReturnServiceImpl service = saleReturnService();
        ErpSaleReturn saleReturn = draftSaleReturn(66L, 99L, 10L);
        saleReturn.setPaidAmount(new BigDecimal("10"));
        saleReturn.setDiscountAmount(BigDecimal.ZERO);
        saleReturn.setTotalAmountInclTax(new BigDecimal("100"));
        ErpAccountsReceivable saleReceivable = new ErpAccountsReceivable();
        saleReceivable.setId(700L);

        when(saleReturnMapper.selectOne(any())).thenReturn(saleReturn);
        when(saleReturnItemMapper.findByReturnId(1L, 66L)).thenReturn(List.of(returnItem(100L, "1", "100")));
        when(saleOrderMapper.selectOne(any())).thenReturn(approvedSaleOrder(99L, 10L));
        when(saleOrderItemMapper.findByOrderId(1L, 99L)).thenReturn(List.of(soldItem(100L, "2")));
        when(saleReturnMapper.selectList(any())).thenReturn(List.of());
        when(accountsReceivableMapper.findBySource(1L, "SALE_ORDER", 99L)).thenReturn(saleReceivable);
        when(receiptReceivableMapper.sumApprovedAllocatedAmountByReceivableId(1L, 700L)).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.approve(66L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("退款金额不能超过原销售可退实收金额");
    }

    @Test
    void saleApproveUsesAtomicDraftTransitionBeforeSideEffects() {
        ErpSaleOrderServiceImpl service = saleOrderService();
        ErpSaleOrder order = draftSaleOrder(77L, 10L);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmountInclTax(new BigDecimal("100"));

        when(saleOrderMapper.selectOne(any())).thenReturn(order);
        when(saleOrderItemMapper.findByOrderId(1L, 77L)).thenReturn(List.of(soldItem(100L, "1")));
        when(saleOrderMapper.approveDraft(eq(1L), eq(77L), any())).thenReturn(null);

        assertThatThrownBy(() -> service.approve(77L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("销售单状态已变化，请刷新重试");
    }

    @Test
    void saleListIncludesNetSalesProfitAndRedFlushTrace() {
        ErpSaleOrderServiceImpl service = saleOrderService();
        ErpSaleOrder order = approvedSaleOrder(88L, 10L);
        order.setTotalAmountInclTax(new BigDecimal("500"));
        order.setStatus("RED_FLUSHED");

        when(saleOrderMapper.selectList(any())).thenReturn(List.of(order));
        when(accountsReceivableMapper.findBySaleOrderId(1L, 88L)).thenReturn(null);
        when(saleReturnMapper.countApprovedBySaleOrderId(1L, 88L)).thenReturn(1L);
        when(saleReturnMapper.sumApprovedAmountBySaleOrderId(1L, 88L)).thenReturn(new BigDecimal("120"));
        when(stockTxnMapper.sumSaleIssueCost(1L, 88L)).thenReturn(new BigDecimal("300"));
        when(stockTxnMapper.sumApprovedSaleReturnCost(1L, 88L)).thenReturn(new BigDecimal("70"));

        List<ErpSaleOrder> result = service.listAll(null, null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCumulativeReturnAmount()).isEqualByComparingTo("120");
        assertThat(result.get(0).getCumulativeReturnCost()).isEqualByComparingTo("70");
        assertThat(result.get(0).getNetSaleAmount()).isEqualByComparingTo("380");
        assertThat(result.get(0).getNetGrossProfit()).isEqualByComparingTo("150");
        assertThat(result.get(0).getRedFlushTrace()).isEqualTo("SALE_ORDER#88");
    }

    private ErpSaleOrderServiceImpl saleOrderService() {
        return new ErpSaleOrderServiceImpl(
            saleOrderMapper,
            saleOrderItemMapper,
            productMapper,
            customerMapper,
            warehouseMapper,
            locationMapper,
            settlementMethodMapper,
            stockBalanceMapper,
            stockTxnMapper,
            orderSequenceMapper,
            accountsReceivableMapper,
            receiptMapper,
            receiptReceivableMapper,
            saleReturnMapper,
            systemConfigMapper,
            costService()
        );
    }

    private ErpSaleReturnServiceImpl saleReturnService() {
        return new ErpSaleReturnServiceImpl(
            saleReturnMapper,
            saleReturnItemMapper,
            productMapper,
            customerMapper,
            warehouseMapper,
            locationMapper,
            settlementMethodMapper,
            stockBalanceMapper,
            stockTxnMapper,
            orderSequenceMapper,
            accountsReceivableMapper,
            receiptMapper,
            receiptReceivableMapper,
            saleOrderItemMapper,
            saleOrderMapper,
            systemConfigMapper,
            costService()
        );
    }

    private ErpCostService costService() {
        return new ErpCostService(productMapper, stockBalanceMapper);
    }

    private ErpProduct product(Long id) {
        ErpProduct product = new ErpProduct();
        product.setId(id);
        product.setCode("P-" + id);
        product.setName("Product-" + id);
        product.setDefaultWarehouseId(1L);
        product.setEnabled(true);
        return product;
    }

    private ErpProduct disabledProduct(Long id) {
        ErpProduct product = product(id);
        product.setEnabled(false);
        return product;
    }

    private void stubValidMasterData() {
        when(customerMapper.selectOne(any())).thenReturn(customer(10L));
        when(settlementMethodMapper.findByCode(1L, "CASH")).thenReturn(settlementMethod("CASH"));
        lenient().when(warehouseMapper.selectOne(any())).thenReturn(warehouse(1L));
        lenient().when(locationMapper.selectOne(any())).thenReturn(location(1L, 1L));
    }

    private ErpCustomer customer(Long id) {
        ErpCustomer customer = new ErpCustomer();
        customer.setId(id);
        customer.setEnabled(true);
        return customer;
    }

    private ErpWarehouse warehouse(Long id) {
        ErpWarehouse warehouse = new ErpWarehouse();
        warehouse.setId(id);
        warehouse.setEnabled(true);
        return warehouse;
    }

    private ErpLocation location(Long id, Long warehouseId) {
        ErpLocation location = new ErpLocation();
        location.setId(id);
        location.setWarehouseId(warehouseId);
        location.setEnabled(true);
        return location;
    }

    private ErpSettlementMethod settlementMethod(String code) {
        ErpSettlementMethod method = new ErpSettlementMethod();
        method.setCode(code);
        method.setEnabled(true);
        return method;
    }

    private ErpSaleOrder approvedSaleOrder(Long id, Long customerId) {
        ErpSaleOrder order = new ErpSaleOrder();
        order.setId(id);
        order.setTenantId(1L);
        order.setStatus("APPROVED");
        order.setCustomerId(customerId);
        return order;
    }

    private ErpSaleOrder draftSaleOrder(Long id, Long customerId) {
        ErpSaleOrder order = approvedSaleOrder(id, customerId);
        order.setStatus("DRAFT");
        order.setVersion(0L);
        return order;
    }

    private ErpSaleReturn draftSaleReturn(Long id, Long saleOrderId, Long customerId) {
        ErpSaleReturn order = new ErpSaleReturn();
        order.setId(id);
        order.setTenantId(1L);
        order.setStatus("DRAFT");
        order.setSaleOrderId(saleOrderId);
        order.setCustomerId(customerId);
        order.setReturnType("RESTOCK");
        order.setVersion(0L);
        return order;
    }

    private ErpSaleOrderItem soldItem(Long productId, String qty) {
        ErpSaleOrderItem item = new ErpSaleOrderItem();
        item.setProductId(productId);
        item.setWarehouseId(1L);
        item.setLocationId(1L);
        item.setQty(new BigDecimal(qty));
        item.setAmountInclTax(new BigDecimal("100").multiply(new BigDecimal(qty)));
        return item;
    }

    private ErpSaleReturnItem returnItem(Long productId, String qty, String amountInclTax) {
        ErpSaleReturnItem item = new ErpSaleReturnItem();
        item.setProductId(productId);
        item.setQty(new BigDecimal(qty));
        item.setAmountInclTax(new BigDecimal(amountInclTax));
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

    private com.example.wms.entity.erp.ErpStockBalance stockBalance(String onHand, String locked) {
        com.example.wms.entity.erp.ErpStockBalance balance = new com.example.wms.entity.erp.ErpStockBalance();
        balance.setQtyOnHand(new BigDecimal(onHand));
        balance.setQtyLocked(new BigDecimal(locked));
        return balance;
    }
}
