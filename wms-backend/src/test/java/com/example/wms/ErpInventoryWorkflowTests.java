package com.example.wms;

import com.example.wms.dto.erp.ErpAssemblyOrderCreateRequest;
import com.example.wms.dto.erp.ErpAssemblyOrderItemRequest;
import com.example.wms.dto.erp.ErpAssemblyOrderUpdateRequest;
import com.example.wms.dto.erp.ErpStockCountCreateRequest;
import com.example.wms.dto.erp.ErpStockCountItemRequest;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockCount;
import com.example.wms.entity.erp.ErpStockCountItem;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAssemblyOrderItemMapper;
import com.example.wms.mapper.erp.ErpAssemblyOrderMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockCountItemMapper;
import com.example.wms.mapper.erp.ErpStockCountMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.service.erp.impl.ErpAssemblyOrderServiceImpl;
import com.example.wms.service.erp.impl.ErpStockCountServiceImpl;
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

        when(stockCountMapper.selectOne(any())).thenReturn(count);
        when(stockCountItemMapper.selectList(any())).thenReturn(List.of(item));
        when(stockBalanceMapper.findByKey(1L, 100L, 200L, 300L)).thenReturn(balance);

        service.approve(10L);

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
            200L,
            null,
            "2026-05-12 08:00:00",
            List.of(new ErpStockCountItemRequest(100L, 200L, null, new BigDecimal("3"), new BigDecimal("999"), "")),
            "recreate"
        );

        var detail = service.create(request, "INIT");

        assertThat(detail.count().getId()).isEqualTo(88L);
        assertThat(detail.items()).hasSize(1);
        assertThat(detail.items().get(0).getSystemQty()).isEqualByComparingTo("0");
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
    void stockCountCreateRejectsDisabledProduct() {
        ErpStockCountServiceImpl service = stockCountService();
        when(productMapper.selectOne(any())).thenReturn(disabledProduct(100L));

        ErpStockCountCreateRequest request = new ErpStockCountCreateRequest(
            null,
            "COUNT",
            200L,
            null,
            "2026-05-12 08:00:00",
            List.of(new ErpStockCountItemRequest(100L, 200L, null, new BigDecimal("3"), null, "")),
            "disabled"
        );

        assertThatThrownBy(() -> service.create(request, "COUNT"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("商品已停用，不能新增引用");
    }

    @Test
    void assemblyUpdateAllowsExistingDisabledProduct() {
        ErpAssemblyOrderServiceImpl service = assemblyService();
        when(assemblyOrderMapper.selectOne(any())).thenReturn(draftAssemblyOrder(50L, 101L));
        when(assemblyOrderItemMapper.findByOrderId(1L, 50L)).thenReturn(List.of(assemblyItem(101L, "1")));
        when(productMapper.selectOne(any())).thenReturn(disabledProduct(101L));

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

    private ErpStockCountServiceImpl stockCountService() {
        return new ErpStockCountServiceImpl(
            stockCountMapper,
            stockCountItemMapper,
            stockBalanceMapper,
            stockTxnMapper,
            orderSequenceMapper,
            systemConfigMapper,
            productMapper,
            costService()
        );
    }

    private ErpAssemblyOrderServiceImpl assemblyService() {
        return new ErpAssemblyOrderServiceImpl(
            assemblyOrderMapper,
            assemblyOrderItemMapper,
            productMapper,
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
}
