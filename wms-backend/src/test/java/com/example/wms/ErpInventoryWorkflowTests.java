package com.example.wms;

import com.example.wms.dto.erp.ErpAssemblyOrderCreateRequest;
import com.example.wms.dto.erp.ErpAssemblyOrderItemRequest;
import com.example.wms.dto.erp.ErpAssemblyOrderUpdateRequest;
import com.example.wms.dto.erp.ErpStockCountCreateRequest;
import com.example.wms.dto.erp.ErpStockCountItemRequest;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockCount;
import com.example.wms.entity.erp.ErpStockCountItem;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAssemblyOrderItemMapper;
import com.example.wms.mapper.erp.ErpAssemblyOrderMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockCountItemMapper;
import com.example.wms.mapper.erp.ErpStockCountMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.impl.ErpAssemblyOrderServiceImpl;
import com.example.wms.service.erp.impl.ErpStockCountServiceImpl;
import com.example.wms.service.erp.support.ErpCostService;
import com.example.wms.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpInventoryWorkflowTests {
    @Mock
    private ErpStockCountMapper stockCountMapper;
    @Mock
    private ErpStockCountItemMapper stockCountItemMapper;
    @Mock
    private ErpStockBalanceMapper stockBalanceMapper;
    @Mock
    private ErpStockTxnMapper stockTxnMapper;
    @Mock
    private ErpOrderSequenceMapper orderSequenceMapper;
    @Mock
    private SystemConfigMapper systemConfigMapper;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpWarehouseMapper warehouseMapper;
    @Mock
    private ErpLocationMapper locationMapper;
    @Mock
    private ErpAssemblyOrderMapper assemblyOrderMapper;
    @Mock
    private ErpAssemblyOrderItemMapper assemblyOrderItemMapper;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void stockCountApproveRecomputesSystemQtyFromCurrentBalance() {
        ErpStockCountServiceImpl service = stockCountService();
        ErpStockCount count = new ErpStockCount();
        count.setId(10L);
        count.setTenantId(1L);
        count.setCountType("COUNT");
        count.setCountNo("SC202605120001");
        count.setStatus("DRAFT");

        ErpStockCountItem item = new ErpStockCountItem();
        item.setId(20L);
        item.setTenantId(1L);
        item.setCountId(10L);
        item.setLineNo(1);
        item.setProductId(100L);
        item.setWarehouseId(200L);
        item.setLocationId(300L);
        item.setSystemQty(new BigDecimal("99"));
        item.setCountedQty(new BigDecimal("8"));
        item.setDiffQty(new BigDecimal("-91"));

        ErpStockBalance balance = new ErpStockBalance();
        balance.setQtyOnHand(new BigDecimal("5"));
        ErpStockBalance updatedBalance = new ErpStockBalance();
        updatedBalance.setQtyOnHand(new BigDecimal("8"));

        when(stockCountMapper.findByIdForUpdate(1L, 10L)).thenReturn(count);
        when(stockCountItemMapper.selectList(any())).thenReturn(List.of(item));
        when(stockBalanceMapper.findByKey(1L, 100L, 200L, 300L)).thenReturn(balance);
        when(stockBalanceMapper.upsertAddQty(1L, 100L, 200L, 300L, new BigDecimal("3"), "system"))
            .thenReturn(updatedBalance);

        service.approve(10L, "COUNT");

        assertThat(item.getSystemQty()).isEqualByComparingTo("5");
        assertThat(item.getDiffQty()).isEqualByComparingTo("3");
        verify(stockCountItemMapper).updateById(item);
    }

    @Test
    void stockInitCreateIgnoresClientSystemQtyAndAllowsRecreateAfterRedFlush() {
        ErpStockCountServiceImpl service = stockCountService();
        when(stockCountMapper.selectCount(any())).thenReturn(0L);
        when(orderSequenceMapper.incrementAndGet(anyLong(), eq("STOCK_INIT"), any())).thenReturn(1L);
        doAnswer(invocation -> {
            ErpStockCount count = invocation.getArgument(0);
            count.setId(88L);
            return 1;
        }).when(stockCountMapper).insert(any(ErpStockCount.class));
        doAnswer(invocation -> {
            ErpStockCountItem item = invocation.getArgument(0);
            item.setId(99L);
            return 1;
        }).when(stockCountItemMapper).insert(any(ErpStockCountItem.class));
        when(productMapper.selectOne(any())).thenReturn(product(100L));
        when(stockBalanceMapper.findByKey(1L, 100L, 200L, null)).thenReturn(null);

        ErpStockCountCreateRequest request = new ErpStockCountCreateRequest(
            null,
            "INIT",
            null,
            200L,
            null,
            "2026-05-12 08:00:00",
            List.of(new ErpStockCountItemRequest(100L, 200L, null, new BigDecimal("3"), new BigDecimal("12.5"), null, new BigDecimal("999"), "")),
            "recreate"
        );

        var detail = service.create(request, "INIT");

        assertThat(detail.count().getId()).isEqualTo(88L);
        assertThat(detail.items()).hasSize(1);
        assertThat(detail.items().get(0).getSystemQty()).isEqualByComparingTo("0");
        assertThat(detail.items().get(0).getInitUnitCost()).isEqualByComparingTo("12.5000");
        assertThat(detail.items().get(0).getInitTotalAmount()).isEqualByComparingTo("37.5000");
    }

    @Test
    void stockInitApproveRejectsWhenAnotherInitAlreadyApproved() {
        ErpStockCountServiceImpl service = stockCountService();
        ErpStockCount count = new ErpStockCount();
        count.setId(10L);
        count.setTenantId(1L);
        count.setCountType("INIT");
        count.setCountNo("SI202605120001");
        count.setStatus("DRAFT");

        when(stockCountMapper.findByIdForUpdate(1L, 10L)).thenReturn(count);
        when(stockCountMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.approve(10L, "INIT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("初始库存仅允许创建一次");
    }

    @Test
    void stockInitGetDetailRejectsCountDocument() {
        ErpStockCountServiceImpl service = stockCountService();
        ErpStockCount count = new ErpStockCount();
        count.setId(10L);
        count.setTenantId(1L);
        count.setCountType("COUNT");
        count.setCountNo("SC202605120001");

        when(stockCountMapper.selectOne(any())).thenReturn(count);

        assertThatThrownBy(() -> service.getDetail(10L, "INIT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("初始库存单不存在");
    }

    @Test
    void assemblyCreateRejectsInvalidFinishedQty() {
        ErpAssemblyOrderServiceImpl service = assemblyService();
        when(productMapper.selectOne(any())).thenReturn(product(100L));

        ErpAssemblyOrderCreateRequest request = new ErpAssemblyOrderCreateRequest(
            null,
            "ASSEMBLE",
            "2026-05-12 08:00:00",
            100L,
            BigDecimal.ZERO,
            200L,
            null,
            BigDecimal.ZERO,
            List.of(new ErpAssemblyOrderItemRequest(101L, 200L, null, BigDecimal.ONE, null)),
            null
        );

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("成品数量必须大于 0");
    }

    @Test
    void assemblyCreateRejectsLocationOutsideWarehouse() {
        ErpAssemblyOrderServiceImpl service = assemblyService();
        when(productMapper.selectOne(any())).thenReturn(product(100L));
        when(warehouseMapper.findActiveById(1L, 200L)).thenReturn(warehouse(200L));
        when(locationMapper.findActiveById(1L, 300L)).thenReturn(location(300L, 201L));

        ErpAssemblyOrderCreateRequest request = new ErpAssemblyOrderCreateRequest(
            null,
            "ASSEMBLE",
            "2026-05-12 08:00:00",
            100L,
            BigDecimal.ONE,
            200L,
            300L,
            BigDecimal.ZERO,
            List.of(new ErpAssemblyOrderItemRequest(101L, 200L, 300L, BigDecimal.ONE, null)),
            null
        );

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("库位不属于所选仓库");
    }

    @Test
    void assemblyCreateRejectsDuplicateProductWarehouseLocation() {
        ErpAssemblyOrderServiceImpl service = assemblyService();
        when(productMapper.selectOne(any())).thenReturn(product(100L));
        when(warehouseMapper.findActiveById(1L, 200L)).thenReturn(warehouse(200L));

        ErpAssemblyOrderCreateRequest request = new ErpAssemblyOrderCreateRequest(
            null,
            "ASSEMBLE",
            "2026-05-12 08:00:00",
            100L,
            BigDecimal.ONE,
            200L,
            null,
            BigDecimal.ZERO,
            List.of(
                new ErpAssemblyOrderItemRequest(101L, 200L, null, BigDecimal.ONE, null),
                new ErpAssemblyOrderItemRequest(101L, 200L, null, new BigDecimal("2"), null)
            ),
            null
        );

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("同一商品、仓库、库位不能重复录入");
    }

    @Test
    void stockCountCreateRejectsDisabledProduct() {
        ErpStockCountServiceImpl service = stockCountService();
        when(productMapper.selectOne(any())).thenReturn(disabledProduct(100L));

        ErpStockCountCreateRequest request = new ErpStockCountCreateRequest(
            null,
            "COUNT",
            "LOSS",
            200L,
            null,
            "2026-05-12 08:00:00",
            List.of(new ErpStockCountItemRequest(100L, 200L, null, new BigDecimal("3"), null, null, null, "")),
            "disabled"
        );

        assertThatThrownBy(() -> service.create(request, "COUNT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("商品已停用，不能新增引用");
    }

    @Test
    void stockCountCreateRejectsLocationOutsideWarehouse() {
        ErpStockCountServiceImpl service = stockCountService();
        when(productMapper.selectOne(any())).thenReturn(product(100L));
        when(warehouseMapper.findActiveById(1L, 200L)).thenReturn(warehouse(200L));
        when(locationMapper.findActiveById(1L, 300L)).thenReturn(location(300L, 201L));

        ErpStockCountCreateRequest request = new ErpStockCountCreateRequest(
            null,
            "COUNT",
            "LOSS",
            200L,
            null,
            "2026-05-12 08:00:00",
            List.of(new ErpStockCountItemRequest(100L, 200L, 300L, new BigDecimal("3"), null, null, null, "")),
            "invalid-location"
        );

        assertThatThrownBy(() -> service.create(request, "COUNT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("库位不属于所选仓库");
    }

    @Test
    void stockCountApproveRejectsNegativeOnHandAfterConcurrentChange() {
        ErpStockCountServiceImpl service = stockCountService();
        ErpStockCount count = new ErpStockCount();
        count.setId(10L);
        count.setTenantId(1L);
        count.setCountType("COUNT");
        count.setCountNo("SC202605120002");
        count.setStatus("DRAFT");

        ErpStockCountItem item = new ErpStockCountItem();
        item.setId(20L);
        item.setTenantId(1L);
        item.setCountId(10L);
        item.setLineNo(1);
        item.setProductId(100L);
        item.setWarehouseId(200L);
        item.setLocationId(null);
        item.setCountedQty(BigDecimal.ZERO);

        ErpStockBalance currentBalance = new ErpStockBalance();
        currentBalance.setQtyOnHand(new BigDecimal("2"));

        when(stockCountMapper.findByIdForUpdate(1L, 10L)).thenReturn(count);
        when(stockCountItemMapper.selectList(any())).thenReturn(List.of(item));
        when(stockBalanceMapper.findByKey(1L, 100L, 200L, null)).thenReturn(currentBalance);
        when(stockBalanceMapper.addQtyIfEnough(1L, 100L, 200L, null, new BigDecimal("-2"), "system"))
            .thenReturn(null);

        assertThatThrownBy(() -> service.approve(10L, "COUNT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("调整后库存不能小于 0");
    }

    @Test
    void stockInitCreateRejectsDuplicateProductWarehouseLocation() {
        ErpStockCountServiceImpl service = stockCountService();
        when(stockCountMapper.selectCount(any())).thenReturn(0L);
        when(orderSequenceMapper.incrementAndGet(anyLong(), eq("STOCK_INIT"), any())).thenReturn(1L);
        doAnswer(invocation -> {
            ErpStockCount count = invocation.getArgument(0);
            count.setId(88L);
            return 1;
        }).when(stockCountMapper).insert(any(ErpStockCount.class));
        when(productMapper.selectOne(any())).thenReturn(product(100L));
        when(stockBalanceMapper.findByKey(1L, 100L, 200L, null)).thenReturn(null);

        ErpStockCountCreateRequest request = new ErpStockCountCreateRequest(
            null,
            "INIT",
            null,
            200L,
            null,
            "2026-05-12 08:00:00",
            List.of(
                new ErpStockCountItemRequest(100L, 200L, null, new BigDecimal("3"), new BigDecimal("12"), null, null, ""),
                new ErpStockCountItemRequest(100L, 200L, null, new BigDecimal("5"), new BigDecimal("12"), null, null, "")
            ),
            "duplicate"
        );

        assertThatThrownBy(() -> service.create(request, "INIT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("同一商品、仓库、库位不能重复录入");
    }

    @Test
    void stockInitRedFlushRequiresReason() {
        ErpStockCountServiceImpl service = stockCountService();
        ErpStockCount count = new ErpStockCount();
        count.setId(10L);
        count.setTenantId(1L);
        count.setCountType("INIT");
        count.setCountNo("SI202605120001");
        count.setStatus("APPROVED");

        when(stockCountMapper.selectOne(any())).thenReturn(count);

        assertThatThrownBy(() -> service.redFlush(10L, "INIT", " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("红冲原因不能为空");
    }

    @Test
    void stockInitRedFlushRejectsLegacyDocumentWithoutCostSnapshot() {
        ErpStockCountServiceImpl service = stockCountService();
        ErpStockCount count = new ErpStockCount();
        count.setId(10L);
        count.setTenantId(1L);
        count.setCountType("INIT");
        count.setCountNo("SI202605120001");
        count.setStatus("APPROVED");
        count.setApprovedAt(Instant.parse("2026-05-12T08:00:00Z"));

        ErpStockCountItem item = new ErpStockCountItem();
        item.setId(20L);
        item.setTenantId(1L);
        item.setCountId(10L);
        item.setProductId(100L);
        item.setWarehouseId(200L);
        item.setCountedQty(new BigDecimal("3"));
        item.setDiffQty(new BigDecimal("3"));

        when(stockCountMapper.selectOne(any())).thenReturn(count);
        when(stockCountItemMapper.selectList(any())).thenReturn(List.of(item));

        assertThatThrownBy(() -> service.redFlush(10L, "INIT", "legacy"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("老的初始库存单不允许红冲");
    }

    @Test
    void stockInitRedFlushRejectsWhenLaterInventoryTxnExists() {
        ErpStockCountServiceImpl service = stockCountService();
        ErpStockCount count = new ErpStockCount();
        count.setId(10L);
        count.setTenantId(1L);
        count.setCountType("INIT");
        count.setCountNo("SI202605120001");
        count.setStatus("APPROVED");
        count.setApprovedAt(Instant.parse("2026-05-12T08:00:00Z"));

        ErpStockCountItem item = new ErpStockCountItem();
        item.setId(20L);
        item.setTenantId(1L);
        item.setCountId(10L);
        item.setProductId(100L);
        item.setWarehouseId(200L);
        item.setCountedQty(new BigDecimal("3"));
        item.setDiffQty(new BigDecimal("3"));
        item.setInitUnitCost(new BigDecimal("12.5000"));
        item.setInitTotalAmount(new BigDecimal("37.5000"));

        when(stockCountMapper.selectOne(any())).thenReturn(count);
        when(stockCountItemMapper.selectList(any())).thenReturn(List.of(item));
        when(stockTxnMapper.existsLaterTxnForInit(1L, 10L, count.getApprovedAt())).thenReturn(true);

        assertThatThrownBy(() -> service.redFlush(10L, "INIT", "later-txn"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("初始库存单已有后续库存业务，不能红冲");
    }

    @Test
    void assemblyUpdateAllowsExistingDisabledProduct() {
        ErpAssemblyOrderServiceImpl service = assemblyService();
        when(assemblyOrderMapper.findByIdForUpdate(1L, 50L)).thenReturn(draftAssemblyOrder(50L, 101L));
        when(assemblyOrderItemMapper.findByOrderId(1L, 50L)).thenReturn(List.of(assemblyItem(101L, "1")));
        when(productMapper.selectOne(any())).thenReturn(disabledProduct(101L));
        when(warehouseMapper.findActiveById(1L, 200L)).thenReturn(warehouse(200L));
        when(stockBalanceMapper.addReservedQtyIfEnough(1L, 101L, 200L, null, BigDecimal.ONE, "system"))
            .thenReturn(stockBalance("1", "1"));

        service.update(50L, new ErpAssemblyOrderUpdateRequest(
            "AO-050",
            "ASSEMBLE",
            "2026-05-12 08:00:00",
            101L,
            BigDecimal.ONE,
            200L,
            null,
            BigDecimal.ZERO,
            List.of(new ErpAssemblyOrderItemRequest(101L, 200L, null, BigDecimal.ONE, null)),
            null
        ));
    }

    @Test
    void assemblyApproveRejectsWhenConcurrentStockChangeMakesInventoryInsufficient() {
        ErpAssemblyOrderServiceImpl service = assemblyService();
        var order = draftAssemblyOrder(60L, 100L);
        order.setFinishedQty(BigDecimal.ONE);
        order.setWarehouseId(200L);
        order.setUnitCost(new BigDecimal("9.5"));
        var item = assemblyItem(101L, "2");
        item.setId(70L);
        item.setWarehouseId(200L);
        item.setProductName("Part-101");
        item.setUnitCost(new BigDecimal("5"));

        when(assemblyOrderMapper.findByIdForUpdate(1L, 60L)).thenReturn(order);
        when(assemblyOrderItemMapper.findByOrderId(1L, 60L)).thenReturn(List.of(item));
        when(stockBalanceMapper.addQtyIfEnoughAvailable(1L, 101L, 200L, null, new BigDecimal("-2"), "system"))
            .thenReturn(null);
        ErpStockBalance currentBalance = new ErpStockBalance();
        currentBalance.setQtyOnHand(BigDecimal.ONE);
        when(stockBalanceMapper.findByKey(1L, 101L, 200L, null)).thenReturn(currentBalance);

        assertThatThrownBy(() -> service.approve(60L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("库存不足，商品[Part-101] 可用=1，需求=2");
    }

    @Test
    void disassembleApproveRecomputesHeaderCostIncludingLabor() {
        ErpAssemblyOrderServiceImpl service = assemblyService();
        var order = draftAssemblyOrder(61L, 100L);
        order.setOrderType("DISASSEMBLE");
        order.setFinishedQty(new BigDecimal("2"));
        order.setWarehouseId(200L);
        order.setLaborCost(new BigDecimal("4"));
        var item = assemblyItem(101L, "4");
        item.setId(71L);
        item.setWarehouseId(200L);
        item.setUnitCost(BigDecimal.ZERO);
        item.setAmount(BigDecimal.ZERO);

        when(assemblyOrderMapper.findByIdForUpdate(1L, 61L)).thenReturn(order);
        when(assemblyOrderItemMapper.findByOrderId(1L, 61L)).thenReturn(List.of(item));
        when(productMapper.findByIdForUpdate(1L, 101L)).thenReturn(productWithCost(101L, BigDecimal.ZERO));
        when(productMapper.selectOne(any()))
            .thenReturn(
                productWithCost(100L, new BigDecimal("10")),
                productWithCost(101L, BigDecimal.ZERO),
                productWithCost(101L, BigDecimal.ZERO),
                productWithCost(100L, new BigDecimal("10"))
            );
        ErpStockBalance finishedOut = new ErpStockBalance();
        finishedOut.setQtyOnHand(new BigDecimal("3"));
        ErpStockBalance componentIn = new ErpStockBalance();
        componentIn.setQtyOnHand(new BigDecimal("4"));
        when(stockBalanceMapper.addQtyIfEnoughAvailable(1L, 100L, 200L, null, new BigDecimal("-2"), "system"))
            .thenReturn(finishedOut);
        when(stockBalanceMapper.upsertAddQty(1L, 101L, 200L, null, new BigDecimal("4"), "system"))
            .thenReturn(componentIn);
        when(stockBalanceMapper.sumQtyByProduct(1L, 101L)).thenReturn(new BigDecimal("1"));

        service.approve(61L);

        ArgumentCaptor<com.example.wms.entity.erp.ErpAssemblyOrder> captor = ArgumentCaptor.forClass(com.example.wms.entity.erp.ErpAssemblyOrder.class);
        verify(assemblyOrderMapper).updateById(captor.capture());
        assertThat(captor.getValue().getTotalCost()).isEqualByComparingTo("24.0000");
        assertThat(captor.getValue().getUnitCost()).isEqualByComparingTo("12.0000");
    }

    private ErpStockCountServiceImpl stockCountService() {
        return new ErpStockCountServiceImpl(
            stockCountMapper,
            stockCountItemMapper,
            stockBalanceMapper,
            stockTxnMapper,
            orderSequenceMapper,
            systemConfigMapper,
            productMapper,
            warehouseMapper,
            locationMapper,
            costService()
        );
    }

    private ErpAssemblyOrderServiceImpl assemblyService() {
        return new ErpAssemblyOrderServiceImpl(
            assemblyOrderMapper,
            assemblyOrderItemMapper,
            productMapper,
            warehouseMapper,
            locationMapper,
            stockBalanceMapper,
            stockTxnMapper,
            orderSequenceMapper,
            systemConfigMapper,
            costService()
        );
    }

    private ErpCostService costService() {
        return new ErpCostService(productMapper, stockBalanceMapper);
    }

    private ErpProduct disabledProduct(Long id) {
        ErpProduct product = product(id);
        product.setEnabled(false);
        return product;
    }

    private ErpProduct product(Long id) {
        ErpProduct product = new ErpProduct();
        product.setId(id);
        product.setCode("P-" + id);
        product.setName("Product-" + id);
        product.setEnabled(true);
        return product;
    }

    private ErpProduct productWithCost(Long id, BigDecimal cost) {
        ErpProduct product = product(id);
        product.setCostPrice(cost);
        return product;
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

    private com.example.wms.entity.erp.ErpAssemblyOrder draftAssemblyOrder(Long id, Long finishedProductId) {
        com.example.wms.entity.erp.ErpAssemblyOrder order = new com.example.wms.entity.erp.ErpAssemblyOrder();
        order.setId(id);
        order.setTenantId(1L);
        order.setStatus("DRAFT");
        order.setFinishedProductId(finishedProductId);
        order.setOrderType("ASSEMBLE");
        order.setOrderAt(java.time.Instant.now());
        return order;
    }

    private com.example.wms.entity.erp.ErpAssemblyOrderItem assemblyItem(Long productId, String qty) {
        com.example.wms.entity.erp.ErpAssemblyOrderItem item = new com.example.wms.entity.erp.ErpAssemblyOrderItem();
        item.setProductId(productId);
        item.setQty(new BigDecimal(qty));
        return item;
    }

    private ErpStockBalance stockBalance(String onHand, String locked) {
        ErpStockBalance balance = new ErpStockBalance();
        balance.setQtyOnHand(new BigDecimal(onHand));
        balance.setQtyLocked(new BigDecimal(locked));
        return balance;
    }
}
