package com.example.wms;

import com.example.wms.controller.erp.ErpStockInitController;
import com.example.wms.dto.erp.ErpAssemblyOrderCreateRequest;
import com.example.wms.dto.erp.ErpAssemblyOrderItemRequest;
import com.example.wms.dto.erp.ErpAssemblyOrderUpdateRequest;
import com.example.wms.dto.erp.ErpStockCountCreateRequest;
import com.example.wms.dto.erp.ErpStockCountItemView;
import com.example.wms.dto.erp.ErpStockCountItemRequest;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockCount;
import com.example.wms.entity.erp.ErpStockCountItem;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpProductStockPolicy;
import com.example.wms.entity.erp.ErpStockInitImportBatch;
import com.example.wms.entity.erp.ErpStockInitImportItem;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAssemblyOrderItemMapper;
import com.example.wms.mapper.erp.ErpAssemblyOrderMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpProductStockPolicyMapper;
import com.example.wms.mapper.erp.ErpSaleOrderItemMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockCountItemMapper;
import com.example.wms.mapper.erp.ErpStockCountMapper;
import com.example.wms.mapper.erp.ErpStockInitImportBatchMapper;
import com.example.wms.mapper.erp.ErpStockInitImportItemMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.impl.ErpAssemblyOrderServiceImpl;
import com.example.wms.service.erp.impl.ErpStockCountServiceImpl;
import com.example.wms.service.erp.support.ExcelImportParser;
import com.example.wms.service.erp.support.ErpCostService;
import com.example.wms.tenant.TenantContext;
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
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private ErpProductStockPolicyMapper productStockPolicyMapper;
    @Mock
    private ErpStockInitImportBatchMapper stockInitImportBatchMapper;
    @Mock
    private ErpStockInitImportItemMapper stockInitImportItemMapper;
    @Mock
    private ErpWarehouseMapper warehouseMapper;
    @Mock
    private ErpLocationMapper locationMapper;
    @Mock
    private ErpAssemblyOrderMapper assemblyOrderMapper;
    @Mock
    private ErpAssemblyOrderItemMapper assemblyOrderItemMapper;
    @Mock
    private ErpSaleOrderMapper saleOrderMapper;
    @Mock
    private ErpSaleOrderItemMapper saleOrderItemMapper;
    @Mock
    private ErpCustomerMapper customerMapper;
    @Mock
    private ErpSupplierMapper supplierMapper;

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
    void stockInitApproveQueuesAsyncTaskAndMarksDocumentApproving() {
        List<Runnable> queuedTasks = new ArrayList<>();
        ErpStockCountServiceImpl service = stockCountService(queuedTasks::add);
        ErpStockCount count = new ErpStockCount();
        count.setId(10L);
        count.setTenantId(1L);
        count.setCountType("INIT");
        count.setCountNo("SI202605120001");
        count.setStatus("DRAFT");

        when(stockCountMapper.findByIdForUpdate(1L, 10L)).thenReturn(count);
        when(stockCountMapper.selectCount(any())).thenReturn(0L);

        service.approve(10L, "INIT");

        assertThat(count.getStatus()).isEqualTo("APPROVING");
        verify(stockCountMapper).updateById(count);
        assertThat(queuedTasks).hasSize(1);
        verify(stockCountItemMapper, never()).updateById(any(ErpStockCountItem.class));
    }

    @Test
    void stockInitApproveProcessesItemsInChunksOfTwoHundred() {
        List<Runnable> queuedTasks = new ArrayList<>();
        AtomicInteger transactionCalls = new AtomicInteger();
        org.springframework.transaction.support.TransactionOperations transactionOperations = new org.springframework.transaction.support.TransactionOperations() {
            @Override
            public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
                transactionCalls.incrementAndGet();
                return action.doInTransaction(null);
            }
        };
        ErpStockCountServiceImpl service = stockCountService(queuedTasks::add, transactionOperations);
        ErpStockCount count = new ErpStockCount();
        count.setId(10L);
        count.setTenantId(1L);
        count.setCountType("INIT");
        count.setCountNo("SI202605120001");
        count.setStatus("DRAFT");

        List<ErpStockCountItem> items = IntStream.rangeClosed(1, 401)
            .mapToObj(index -> {
                ErpStockCountItem item = new ErpStockCountItem();
                item.setId((long) index);
                item.setTenantId(1L);
                item.setCountId(10L);
                item.setLineNo(index);
                item.setProductId(1000L + index);
                item.setWarehouseId(200L);
                item.setLocationId(null);
                item.setCountedQty(BigDecimal.ONE);
                item.setSystemQty(BigDecimal.ZERO);
                item.setDiffQty(BigDecimal.ZERO);
                item.setInitUnitCost(new BigDecimal("5"));
                item.setInitTotalAmount(new BigDecimal("5"));
                return item;
            })
            .toList();

        when(stockCountMapper.findByIdForUpdate(1L, 10L)).thenReturn(count);
        when(stockCountMapper.selectCount(any())).thenReturn(0L);
        when(stockCountMapper.selectOne(any())).thenReturn(count);
        when(stockCountItemMapper.selectList(any())).thenReturn(items);
        when(stockBalanceMapper.findByKey(anyLong(), anyLong(), any(), any())).thenReturn(null);
        when(productMapper.findByIdForUpdate(anyLong(), anyLong())).thenAnswer(invocation -> productWithCost(invocation.getArgument(1), BigDecimal.ZERO));

        service.approve(10L, "INIT");
        assertThat(queuedTasks).hasSize(1);

        queuedTasks.get(0).run();

        assertThat(transactionCalls.get()).isEqualTo(4);
        verify(stockCountItemMapper, times(401)).updateById(any(ErpStockCountItem.class));
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
    void stockInitImportEndpointUsesMultipartExcelUploadContract() throws Exception {
        Method controllerMethod = ErpStockInitController.class.getMethod("importInitStocks", MultipartFile.class, String.class, String.class, String.class);
        PostMapping postMapping = controllerMethod.getAnnotation(PostMapping.class);
        Parameter[] parameters = controllerMethod.getParameters();

        assertThat(postMapping).isNotNull();
        assertThat(postMapping.value()).containsExactly("/import");
        assertThat(postMapping.consumes()).contains(MediaType.MULTIPART_FORM_DATA_VALUE);
        assertThat(parameters).hasSize(4);
        assertThat(parameters[0].getType()).isEqualTo(MultipartFile.class);
        assertThat(parameters[0].getAnnotation(RequestParam.class).value()).isEqualTo("file");
        assertThat(parameters[1].getType()).isEqualTo(String.class);
        assertThat(parameters[1].getAnnotation(RequestParam.class).value()).isEqualTo("sourceName");
        assertThat(parameters[1].getAnnotation(RequestParam.class).required()).isFalse();
        assertThat(parameters[2].getType()).isEqualTo(String.class);
        assertThat(parameters[2].getAnnotation(RequestParam.class).value()).isEqualTo("fieldMapping");
        assertThat(parameters[2].getAnnotation(RequestParam.class).required()).isFalse();
        assertThat(parameters[3].getType()).isEqualTo(String.class);
        assertThat(parameters[3].getAnnotation(RequestParam.class).value()).isEqualTo("strategyMode");
        assertThat(parameters[3].getAnnotation(RequestParam.class).required()).isFalse();
    }

    @Test
    void stockInitImportPreviewEndpointReturnsHeaderMappingContract() throws Exception {
        Method controllerMethod = ErpStockInitController.class.getMethod("previewImport", MultipartFile.class);
        PostMapping postMapping = controllerMethod.getAnnotation(PostMapping.class);
        Parameter[] parameters = controllerMethod.getParameters();

        assertThat(postMapping).isNotNull();
        assertThat(postMapping.value()).containsExactly("/import/preview");
        assertThat(postMapping.consumes()).contains(MediaType.MULTIPART_FORM_DATA_VALUE);
        assertThat(parameters).hasSize(1);
        assertThat(parameters[0].getType()).isEqualTo(MultipartFile.class);
        assertThat(parameters[0].getAnnotation(RequestParam.class).value()).isEqualTo("file");
        assertThat(controllerMethod.getGenericReturnType().getTypeName())
            .contains("ErpStockInitImportPreview");
    }

    @Test
    void stockInitImportHistoryEndpointsExposeBatchAndItemContracts() throws Exception {
        Method batchMethod = ErpStockInitController.class.getMethod("listImportBatches");
        Method itemMethod = ErpStockInitController.class.getMethod("listImportBatchItems", Long.class);

        assertThat(batchMethod.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value())
            .containsExactly("/import-batches");
        assertThat(batchMethod.getGenericReturnType().getTypeName())
            .contains("ErpStockInitImportBatchSummary");
        assertThat(itemMethod.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value())
            .containsExactly("/import-batches/{batchId}/items");
        assertThat(itemMethod.getParameters()[0].getAnnotation(org.springframework.web.bind.annotation.PathVariable.class).value())
            .isEqualTo("batchId");
        assertThat(itemMethod.getGenericReturnType().getTypeName())
            .contains("ErpStockInitImportItemView");
    }

    @Test
    void stockInitImportPreviewAutoMatchesHeadersAndReturnsSampleRows() throws Exception {
        ErpStockCountServiceImpl service = stockCountService();

        var preview = service.previewInitStockImport(new MockMultipartFile(
            "file",
            "stock-init.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            stockInitWorkbookBytes("主仓")
        ));

        assertThat(preview.headers()).contains("仓库", "编码", "产品名称", "库存数", "库存成本价", "金额");
        assertThat(preview.fields()).anySatisfy(field -> {
            assertThat(field.key()).isEqualTo("code");
            assertThat(field.required()).isFalse();
        });
        assertThat(preview.fields()).anySatisfy(field -> {
            assertThat(field.key()).isEqualTo("productName");
            assertThat(field.required()).isTrue();
        });
        assertThat(preview.mappings()).anySatisfy(mapping -> {
            assertThat(mapping.excelHeader()).isEqualTo("编码");
            assertThat(mapping.fieldKey()).isEqualTo("code");
            assertThat(mapping.matchType()).isEqualTo("AUTO");
            assertThat(mapping.sampleValue()).isEqualTo("PR001");
        });
        assertThat(preview.mappings()).anySatisfy(mapping -> {
            assertThat(mapping.excelHeader()).isEqualTo("产品名称");
            assertThat(mapping.fieldKey()).isEqualTo("productName");
            assertThat(mapping.matchType()).isEqualTo("AUTO");
            assertThat(mapping.sampleValue()).isEqualTo("Product-100");
        });
        assertThat(preview.mappings()).anySatisfy(mapping -> {
            assertThat(mapping.excelHeader()).isEqualTo("库存成本价");
            assertThat(mapping.fieldKey()).isEqualTo("initUnitCost");
        });
        assertThat(preview.sampleRows()).hasSize(1);
    }

    @Test
    void stockInitImportPreviewExposesProductAndWarehousePolicyFields() throws Exception {
        ErpStockCountServiceImpl service = stockCountService();

        var preview = service.previewInitStockImport(new MockMultipartFile(
            "file",
            "stock-init-policy.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            stockInitWorkbookWithPolicyHeadersBytes()
        ));

        assertThat(preview.fields())
            .extracting("key")
            .contains(
                "productSafetyStock",
                "productMinStock",
                "productMaxStock",
                "warehouseSafetyStock",
                "warehouseMinStock",
                "warehouseMaxStock"
            );
        assertThat(preview.mappings()).anySatisfy(mapping -> {
            assertThat(mapping.excelHeader()).isEqualTo("标准库存数");
            assertThat(mapping.fieldKey()).isEqualTo("warehouseSafetyStock");
            assertThat(mapping.matchType()).isEqualTo("AUTO");
            assertThat(mapping.sampleValue()).isEqualTo("20");
        });
        assertThat(preview.mappings()).anySatisfy(mapping -> {
            assertThat(mapping.excelHeader()).isEqualTo("库存下限");
            assertThat(mapping.fieldKey()).isEqualTo("warehouseMinStock");
        });
        assertThat(preview.mappings()).anySatisfy(mapping -> {
            assertThat(mapping.excelHeader()).isEqualTo("库存上限");
            assertThat(mapping.fieldKey()).isEqualTo("warehouseMaxStock");
        });
    }

    @Test
    void stockInitImportCreatesDraftDocumentAndIgnoresUnknownWarehouse() throws Exception {
        List<Runnable> queuedTasks = new ArrayList<>();
        ErpStockCountServiceImpl service = stockCountService(queuedTasks::add);
        when(stockCountMapper.selectCount(any())).thenReturn(0L);
        when(orderSequenceMapper.incrementAndGet(anyLong(), eq("STOCK_INIT"), any())).thenReturn(1L);
        doAnswer(invocation -> {
            ErpStockInitImportBatch batch = invocation.getArgument(0);
            batch.setId(811L);
            batch.setBatchNo("SII20260602010111");
            return 1;
        }).when(stockInitImportBatchMapper).insert(any(ErpStockInitImportBatch.class));
        doAnswer(invocation -> {
            ErpStockCount count = invocation.getArgument(0);
            count.setId(501L);
            return 1;
        }).when(stockCountMapper).insert(any(ErpStockCount.class));
        doAnswer(invocation -> {
            ErpStockCountItem item = invocation.getArgument(0);
            item.setId(701L);
            return 1;
        }).when(stockCountItemMapper).insert(any(ErpStockCountItem.class));
        when(productMapper.selectList(any())).thenReturn(List.of(product(100L)));
        when(productMapper.selectOne(any())).thenReturn(product(100L));
        when(stockBalanceMapper.findByKey(1L, 100L, null, null)).thenReturn(null);
        when(warehouseMapper.selectOne(any())).thenReturn(null);
        when(supplierMapper.selectOne(any())).thenReturn(null);

        var result = service.importInitStocks(
            new MockMultipartFile(
                "file",
                "stock-init.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                stockInitWorkbookBytes("未知仓库")
            ),
            "库存明细浏览表",
            null
        );

        assertThat(result.batchId()).isEqualTo(811L);
        assertThat(result.status()).isEqualTo("PROCESSING");
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(queuedTasks).hasSize(1);
        when(stockInitImportBatchMapper.selectOne(any())).thenReturn(batchForUpdate(811L));
        queuedTasks.get(0).run();
        ArgumentCaptor<ErpStockCountItem> itemCaptor = ArgumentCaptor.forClass(ErpStockCountItem.class);
        verify(stockCountItemMapper).insert(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getWarehouseId()).isNull();
        assertThat(itemCaptor.getValue().getCountedQty()).isEqualByComparingTo("6");
        assertThat(itemCaptor.getValue().getInitUnitCost()).isEqualByComparingTo("18.5000");
    }

    @Test
    void stockInitImportAllowsDisabledProductAndRecordsWarning() throws Exception {
        ErpStockCountServiceImpl service = stockCountService();
        when(stockCountMapper.selectCount(any())).thenReturn(0L);
        when(orderSequenceMapper.incrementAndGet(anyLong(), eq("STOCK_INIT"), any())).thenReturn(1L);
        doAnswer(invocation -> {
            ErpStockCount count = invocation.getArgument(0);
            count.setId(502L);
            return 1;
        }).when(stockCountMapper).insert(any(ErpStockCount.class));
        doAnswer(invocation -> {
            ErpStockCountItem item = invocation.getArgument(0);
            item.setId(702L);
            return 1;
        }).when(stockCountItemMapper).insert(any(ErpStockCountItem.class));
        doAnswer(invocation -> {
            ErpStockInitImportBatch batch = invocation.getArgument(0);
            batch.setId(812L);
            batch.setBatchNo("SII20260602010102");
            return 1;
        }).when(stockInitImportBatchMapper).insert(any(ErpStockInitImportBatch.class));
        when(productMapper.selectList(any())).thenReturn(List.of(disabledProduct(100L)));
        when(productMapper.selectOne(any())).thenReturn(disabledProduct(100L));
        when(stockBalanceMapper.findByKey(1L, 100L, 200L, null)).thenReturn(null);
        when(warehouseMapper.selectOne(any())).thenReturn(warehouse(200L));
        when(supplierMapper.selectOne(any())).thenReturn(null);

        var result = service.importInitStocks(
            new MockMultipartFile(
                "file",
                "stock-init.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                stockInitWorkbookBytes("主仓")
            ),
            "库存明细浏览表",
            null
        );

        assertThat(result.status()).isEqualTo("PROCESSING");
        assertThat(result.countId()).isNull();
        assertThat(result.warningCount()).isZero();
        verify(stockCountItemMapper).insert(any(ErpStockCountItem.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpStockInitImportItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockInitImportItemMapper).insertBatch(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).hasSize(1);
        assertThat(itemsCaptor.getValue().get(0).getStatus()).isEqualTo("VALIDATED");
        assertThat(itemsCaptor.getValue().get(0).getWarningMessage()).contains("商品已停用");
    }

    @Test
    void stockInitImportMatchesProductByNameInsteadOfCode() throws Exception {
        ErpStockCountServiceImpl service = stockCountService();
        when(stockCountMapper.selectCount(any())).thenReturn(0L);
        when(orderSequenceMapper.incrementAndGet(anyLong(), eq("STOCK_INIT"), any())).thenReturn(1L);
        doAnswer(invocation -> {
            ErpStockCount count = invocation.getArgument(0);
            count.setId(505L);
            return 1;
        }).when(stockCountMapper).insert(any(ErpStockCount.class));
        doAnswer(invocation -> {
            ErpStockCountItem item = invocation.getArgument(0);
            item.setId(705L);
            return 1;
        }).when(stockCountItemMapper).insert(any(ErpStockCountItem.class));
        when(productMapper.selectList(any())).thenReturn(List.of(product(105L)));
        when(productMapper.selectOne(any())).thenReturn(product(105L));
        when(stockBalanceMapper.findByKey(1L, 105L, null, null)).thenReturn(null);
        when(warehouseMapper.selectOne(any())).thenReturn(null);
        when(supplierMapper.selectOne(any())).thenReturn(null);

        service.importInitStocks(
            new MockMultipartFile(
                "file",
                "stock-init.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                stockInitWorkbookBytes("主仓", "OUTSIDE-001", "Product-105")
            ),
            "库存明细浏览表",
            null
        );

        ArgumentCaptor<ErpStockCountItem> itemCaptor = ArgumentCaptor.forClass(ErpStockCountItem.class);
        verify(stockCountItemMapper).insert(itemCaptor.capture());
        verify(productMapper, never()).findByCode(1L, "OUTSIDE-001");
        assertThat(itemCaptor.getValue().getProductId()).isEqualTo(105L);
    }

    @Test
    void stockInitImportCreatesBatchAndItemLogsForFrontendResultDrawer() throws Exception {
        List<Runnable> queuedTasks = new ArrayList<>();
        ErpStockCountServiceImpl service = stockCountService(queuedTasks::add);
        when(stockCountMapper.selectCount(any())).thenReturn(0L);
        when(orderSequenceMapper.incrementAndGet(anyLong(), eq("STOCK_INIT"), any())).thenReturn(1L);
        doAnswer(invocation -> {
            ErpStockInitImportBatch batch = invocation.getArgument(0);
            batch.setId(801L);
            batch.setBatchNo("SII20260602010101");
            return 1;
        }).when(stockInitImportBatchMapper).insert(any(ErpStockInitImportBatch.class));
        doAnswer(invocation -> {
            ErpStockCount count = invocation.getArgument(0);
            count.setId(506L);
            count.setCountNo("SI202606020001");
            return 1;
        }).when(stockCountMapper).insert(any(ErpStockCount.class));
        doAnswer(invocation -> {
            ErpStockCountItem item = invocation.getArgument(0);
            item.setId(706L);
            return 1;
        }).when(stockCountItemMapper).insert(any(ErpStockCountItem.class));
        when(productMapper.selectList(any())).thenReturn(List.of(product(106L)));
        when(productMapper.selectOne(any())).thenReturn(product(106L));
        when(stockBalanceMapper.findByKey(1L, 106L, null, null)).thenReturn(null);
        when(warehouseMapper.selectOne(any())).thenReturn(null);
        when(supplierMapper.selectOne(any())).thenReturn(null);

        var result = service.importInitStocks(
            new MockMultipartFile(
                "file",
                "stock-init-log.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                stockInitWorkbookBytes("主仓", "OUTSIDE-LOG", "Product-106")
            ),
            "库存明细浏览表",
            null
        );

        assertThat(result.batchId()).isEqualTo(801L);
        assertThat(result.batchNo()).isEqualTo("SII20260602010101");
        assertThat(result.status()).isEqualTo("PROCESSING");
        assertThat(result.successCount()).isZero();
        assertThat(result.failedCount()).isZero();
        assertThat(queuedTasks).hasSize(1);
        when(stockInitImportBatchMapper.selectOne(any())).thenReturn(batchForUpdate(801L));
        queuedTasks.get(0).run();
        ArgumentCaptor<ErpStockInitImportBatch> batchCaptor = ArgumentCaptor.forClass(ErpStockInitImportBatch.class);
        verify(stockInitImportBatchMapper).insert(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getSourceName()).isEqualTo("库存明细浏览表");
        assertThat(batchCaptor.getValue().getTotalCount()).isEqualTo(1);
        ArgumentCaptor<ErpStockInitImportBatch> updateBatchCaptor = ArgumentCaptor.forClass(ErpStockInitImportBatch.class);
        verify(stockInitImportBatchMapper).updateById(updateBatchCaptor.capture());
        assertThat(updateBatchCaptor.getValue().getStatus()).isEqualTo("DONE");
        assertThat(updateBatchCaptor.getValue().getSuccessCount()).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpStockInitImportItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockInitImportItemMapper).insertBatch(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).hasSize(1);
        assertThat(itemsCaptor.getValue().get(0).getStatus()).isEqualTo("VALIDATED");
        assertThat(itemsCaptor.getValue().get(0).getSourceName()).isEqualTo("Product-106");
        assertThat(itemsCaptor.getValue().get(0).getMatchedProductId()).isEqualTo(106L);
        verify(stockInitImportItemMapper).updateStatusByBatch(1L, 801L, "VALIDATED", "SUCCESS");
    }

    @Test
    void stockInitDetailItemsCanBeLoadedByPageForLargeDocuments() {
        ErpStockCountServiceImpl service = stockCountService();
        ErpStockCount count = new ErpStockCount();
        count.setId(506L);
        count.setTenantId(1L);
        count.setCountType("INIT");
        count.setCountNo("SI202606020001");
        when(stockCountMapper.selectOne(any())).thenReturn(count);
        when(stockCountItemMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<ErpStockCountItem> page = invocation.getArgument(0);
            page.setTotal(2005L);
            page.setRecords(List.of(stockCountItem(101L, 1), stockCountItem(102L, 2)));
            return page;
        });
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product(101L), product(102L)));

        var result = service.pageDetailItems(506L, 2, 2, "INIT");

        assertThat(result.total()).isEqualTo(2005);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.items()).extracting(ErpStockCountItemView::productId).containsExactly(101L, 102L);
        assertThat(result.items()).extracting(ErpStockCountItemView::productName).containsExactly("Product-101", "Product-102");
        verify(stockCountItemMapper).selectPage(any(), any());
        verify(productMapper).selectBatchIds(List.of(101L, 102L));
    }

    @Test
    void stockInitDetailCanSkipItemsWhenFrontendLoadsRowsByPage() {
        ErpStockCountServiceImpl service = stockCountService();
        ErpStockCount count = new ErpStockCount();
        count.setId(506L);
        count.setTenantId(1L);
        count.setCountType("INIT");
        count.setCountNo("SI202606020001");
        when(stockCountMapper.selectOne(any())).thenReturn(count);

        var result = service.getDetail(506L, "INIT", false);

        assertThat(result.count().getCountNo()).isEqualTo("SI202606020001");
        assertThat(result.items()).isEmpty();
        verify(stockCountItemMapper, never()).selectList(any());
    }

    @Test
    void stockInitImportReturnsProcessingBeforeBackgroundRowsRun() throws Exception {
        List<Runnable> queuedTasks = new ArrayList<>();
        ErpStockCountServiceImpl service = stockCountService(queuedTasks::add);
        doAnswer(invocation -> {
            ErpStockInitImportBatch batch = invocation.getArgument(0);
            batch.setId(804L);
            batch.setBatchNo("SII20260602010104");
            return 1;
        }).when(stockInitImportBatchMapper).insert(any(ErpStockInitImportBatch.class));

        var result = service.importInitStocks(
            new MockMultipartFile(
                "file",
                "stock-init-async.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                stockInitWorkbookBytes("主仓", "OUTSIDE-ASYNC", "Product-108")
            ),
            "库存明细浏览表",
            null
        );

        assertThat(result.batchId()).isEqualTo(804L);
        assertThat(result.batchNo()).isEqualTo("SII20260602010104");
        assertThat(result.status()).isEqualTo("PROCESSING");
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.successCount()).isZero();
        assertThat(result.failedCount()).isZero();
        assertThat(queuedTasks).hasSize(1);
        verify(stockCountMapper, never()).insert(any(ErpStockCount.class));
        verify(stockCountItemMapper, never()).insert(any(ErpStockCountItem.class));
        verify(stockInitImportItemMapper, never()).insertBatch(any());
    }

    @Test
    void stockInitImportWritesRowLogsInChunks() throws Exception {
        List<Runnable> queuedTasks = new ArrayList<>();
        ErpStockCountServiceImpl service = stockCountService(queuedTasks::add);
        doAnswer(invocation -> {
            ErpStockInitImportBatch batch = invocation.getArgument(0);
            batch.setId(805L);
            batch.setBatchNo("SII20260602010105");
            return 1;
        }).when(stockInitImportBatchMapper).insert(any(ErpStockInitImportBatch.class));
        doAnswer(invocation -> {
            ErpStockCount count = invocation.getArgument(0);
            count.setId(507L);
            count.setCountNo("SI202606020005");
            return 1;
        }).when(stockCountMapper).insert(any(ErpStockCount.class));
        doAnswer(invocation -> {
            ErpStockCountItem item = invocation.getArgument(0);
            item.setId(800L + item.getLineNo());
            return 1;
        }).when(stockCountItemMapper).insert(any(ErpStockCountItem.class));
        AtomicLong warehouseId = new AtomicLong(900L);
        when(productMapper.selectList(any())).thenReturn(List.of(product(108L)));
        when(productMapper.selectOne(any())).thenReturn(product(108L));
        when(stockBalanceMapper.findByKey(eq(1L), eq(108L), anyLong(), eq(null))).thenReturn(null);
        when(warehouseMapper.selectOne(any())).thenAnswer(invocation -> warehouse(warehouseId.incrementAndGet()));

        var result = service.importInitStocks(
            new MockMultipartFile(
                "file",
                "stock-init-chunk.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                stockInitWorkbookBytes(105)
            ),
            "库存明细浏览表",
            null
        );

        assertThat(result.status()).isEqualTo("PROCESSING");
        assertThat(result.totalCount()).isEqualTo(105);
        when(stockInitImportBatchMapper.selectOne(any())).thenReturn(batchForUpdate(805L));
        queuedTasks.get(0).run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpStockInitImportItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockInitImportItemMapper, org.mockito.Mockito.times(2)).insertBatch(itemsCaptor.capture());
        assertThat(itemsCaptor.getAllValues()).extracting(List::size).containsExactly(100, 5);
    }

    @Test
    void stockInitImportWritesRowLogsBeforeFinalDocumentCreationFails() throws Exception {
        List<Runnable> queuedTasks = new ArrayList<>();
        ErpStockCountServiceImpl service = stockCountService(queuedTasks::add);
        doAnswer(invocation -> {
            ErpStockInitImportBatch batch = invocation.getArgument(0);
            batch.setId(806L);
            batch.setBatchNo("SII20260602010106");
            return 1;
        }).when(stockInitImportBatchMapper).insert(any(ErpStockInitImportBatch.class));
        when(stockCountMapper.selectCount(any())).thenReturn(1L);
        when(productMapper.selectList(any())).thenReturn(List.of(product(109L)));
        when(warehouseMapper.selectOne(any())).thenReturn(null);
        when(supplierMapper.selectOne(any())).thenReturn(null);

        var result = service.importInitStocks(
            new MockMultipartFile(
                "file",
                "stock-init-final-fail.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                stockInitWorkbookBytes("主仓", "OUTSIDE-FINAL", "Product-109")
            ),
            "库存明细浏览表",
            null
        );

        assertThat(result.status()).isEqualTo("PROCESSING");
        when(stockInitImportBatchMapper.selectOne(any())).thenReturn(batchForUpdate(806L));
        queuedTasks.get(0).run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpStockInitImportItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockInitImportItemMapper, org.mockito.Mockito.times(2)).insertBatch(itemsCaptor.capture());
        List<ErpStockInitImportItem> insertedItems = itemsCaptor.getAllValues().stream()
            .flatMap(List::stream)
            .toList();
        assertThat(insertedItems).extracting(ErpStockInitImportItem::getStatus).contains("VALIDATED", "FAILED");
        assertThat(insertedItems).noneSatisfy(item -> assertThat(item.getStatus()).isEqualTo("SUCCESS"));
        assertThat(insertedItems).anySatisfy(item -> {
            assertThat(item.getStatus()).isEqualTo("FAILED");
            assertThat(item.getErrorMessage()).contains("初始库存仅允许创建一次");
        });
        ArgumentCaptor<ErpStockInitImportBatch> batchCaptor = ArgumentCaptor.forClass(ErpStockInitImportBatch.class);
        verify(stockInitImportBatchMapper).updateById(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(batchCaptor.getValue().getSummary()).contains("初始库存仅允许创建一次");
    }

    @Test
    void stockInitImportWritesFailureItemWhenBackgroundParseFails() throws Exception {
        List<Runnable> queuedTasks = new ArrayList<>();
        ErpStockCountServiceImpl service = stockCountService(queuedTasks::add);
        doAnswer(invocation -> {
            ErpStockInitImportBatch batch = invocation.getArgument(0);
            batch.setId(807L);
            batch.setBatchNo("SII20260602010107");
            return 1;
        }).when(stockInitImportBatchMapper).insert(any(ErpStockInitImportBatch.class));
        when(productMapper.selectList(any())).thenReturn(List.of(product(103L)));
        when(warehouseMapper.selectOne(any())).thenReturn(null);

        var result = service.importInitStocks(
            new MockMultipartFile(
                "file",
                "stock-init-parse-fail.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                stockInitWorkbookWithPolicyHeadersBytes()
            ),
            "库存明细浏览表",
            null,
            "WAREHOUSE"
        );

        assertThat(result.status()).isEqualTo("PROCESSING");
        when(stockInitImportBatchMapper.selectOne(any())).thenReturn(batchForUpdate(807L));
        queuedTasks.get(0).run();

        ArgumentCaptor<ErpStockInitImportBatch> batchCaptor = ArgumentCaptor.forClass(ErpStockInitImportBatch.class);
        verify(stockInitImportBatchMapper).updateById(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(batchCaptor.getValue().getSummary()).contains("导入仓库层级库存策略时仓库不能为空或未匹配");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpStockInitImportItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockInitImportItemMapper).insertBatch(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).hasSize(1);
        assertThat(itemsCaptor.getValue().get(0).getStatus()).isEqualTo("FAILED");
        assertThat(itemsCaptor.getValue().get(0).getErrorMessage()).contains("导入仓库层级库存策略时仓库不能为空或未匹配");
    }

    @Test
    void stockInitImportLogsDuplicateStockKeyOnExcelRowInsteadOfBatchFailureRow() throws Exception {
        List<Runnable> queuedTasks = new ArrayList<>();
        ErpStockCountServiceImpl service = stockCountService(queuedTasks::add);
        doAnswer(invocation -> {
            ErpStockInitImportBatch batch = invocation.getArgument(0);
            batch.setId(808L);
            batch.setBatchNo("SII20260602010108");
            return 1;
        }).when(stockInitImportBatchMapper).insert(any(ErpStockInitImportBatch.class));
        when(productMapper.selectList(any())).thenReturn(List.of(product(111L)));
        when(warehouseMapper.selectOne(any())).thenReturn(warehouse(901L));

        var result = service.importInitStocks(
            new MockMultipartFile(
                "file",
                "stock-init-duplicate.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                stockInitDuplicateStockKeyWorkbookBytes()
            ),
            "库存明细浏览表",
            null
        );

        assertThat(result.status()).isEqualTo("PROCESSING");
        when(stockInitImportBatchMapper.selectOne(any())).thenReturn(batchForUpdate(808L));
        queuedTasks.get(0).run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpStockInitImportItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockInitImportItemMapper, org.mockito.Mockito.atLeastOnce()).insertBatch(itemsCaptor.capture());
        List<ErpStockInitImportItem> insertedItems = itemsCaptor.getAllValues().stream()
            .flatMap(List::stream)
            .toList();
        assertThat(insertedItems).anySatisfy(item -> {
            assertThat(item.getRowNo()).isEqualTo(3);
            assertThat(item.getSourceCode()).isEqualTo("DUP-002");
            assertThat(item.getSourceName()).isEqualTo("Product-111");
            assertThat(item.getWarehouseName()).isEqualTo("默认仓库");
            assertThat(item.getStatus()).isEqualTo("FAILED");
            assertThat(item.getErrorMessage()).contains("同一商品、仓库、库位不能重复录入");
        });
        assertThat(insertedItems).noneSatisfy(item -> assertThat(item.getRowNo()).isZero());
    }

    @Test
    void stockInitImportUsesManualFieldMappingAndLocation() throws Exception {
        ErpStockCountServiceImpl service = stockCountService();
        when(stockCountMapper.selectCount(any())).thenReturn(0L);
        when(orderSequenceMapper.incrementAndGet(anyLong(), eq("STOCK_INIT"), any())).thenReturn(1L);
        doAnswer(invocation -> {
            ErpStockCount count = invocation.getArgument(0);
            count.setId(502L);
            return 1;
        }).when(stockCountMapper).insert(any(ErpStockCount.class));
        doAnswer(invocation -> {
            ErpStockCountItem item = invocation.getArgument(0);
            item.setId(702L);
            return 1;
        }).when(stockCountItemMapper).insert(any(ErpStockCountItem.class));
        when(productMapper.selectList(any())).thenReturn(List.of(product(101L)));
        when(productMapper.selectOne(any())).thenReturn(product(101L));
        when(stockBalanceMapper.findByKey(1L, 101L, 201L, 301L)).thenReturn(null);
        when(warehouseMapper.selectOne(any())).thenReturn(warehouse(201L));
        when(locationMapper.selectOne(any())).thenReturn(location(301L, 201L));

        service.importInitStocks(
            new MockMultipartFile(
                "file",
                "stock-init-custom.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                stockInitWorkbookWithCustomHeadersBytes()
            ),
            null,
            "{\"仓库名称\":\"warehouseName\",\"库位名称\":\"locationName\",\"物料编号\":\"code\",\"物料名称\":\"productName\",\"期初数量\":\"countedQty\",\"期初单价\":\"initUnitCost\"}"
        );

        ArgumentCaptor<ErpStockCountItem> itemCaptor = ArgumentCaptor.forClass(ErpStockCountItem.class);
        verify(stockCountItemMapper).insert(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getWarehouseId()).isEqualTo(201L);
        assertThat(itemCaptor.getValue().getLocationId()).isEqualTo(301L);
        assertThat(itemCaptor.getValue().getCountedQty()).isEqualByComparingTo("9");
        assertThat(itemCaptor.getValue().getInitUnitCost()).isEqualByComparingTo("21.0000");
    }

    @Test
    void stockInitImportWritesWarehousePolicyWhenStrategyModeIsWarehouse() throws Exception {
        ErpStockCountServiceImpl service = stockCountService();
        when(stockCountMapper.selectCount(any())).thenReturn(0L);
        when(orderSequenceMapper.incrementAndGet(anyLong(), eq("STOCK_INIT"), any())).thenReturn(1L);
        doAnswer(invocation -> {
            ErpStockCount count = invocation.getArgument(0);
            count.setId(503L);
            return 1;
        }).when(stockCountMapper).insert(any(ErpStockCount.class));
        doAnswer(invocation -> {
            ErpStockCountItem item = invocation.getArgument(0);
            item.setId(703L);
            return 1;
        }).when(stockCountItemMapper).insert(any(ErpStockCountItem.class));
        when(productMapper.selectList(any())).thenReturn(List.of(product(103L)));
        when(productMapper.selectOne(any())).thenReturn(product(103L));
        when(stockBalanceMapper.findByKey(1L, 103L, 203L, null)).thenReturn(null);
        when(warehouseMapper.selectOne(any())).thenReturn(warehouse(203L));

        service.importInitStocks(
            new MockMultipartFile(
                "file",
                "stock-init-policy.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                stockInitWorkbookWithPolicyHeadersBytes()
            ),
            "库存明细浏览表",
            "{\"仓库\":\"warehouseName\",\"编码\":\"code\",\"产品名称\":\"productName\",\"库存数\":\"countedQty\",\"库存成本价\":\"initUnitCost\",\"标准库存数\":\"warehouseSafetyStock\",\"库存下限\":\"warehouseMinStock\",\"库存上限\":\"warehouseMaxStock\"}",
            "WAREHOUSE"
        );

        ArgumentCaptor<ErpProductStockPolicy> policyCaptor = ArgumentCaptor.forClass(ErpProductStockPolicy.class);
        verify(productStockPolicyMapper).insert(policyCaptor.capture());
        assertThat(policyCaptor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(policyCaptor.getValue().getProductId()).isEqualTo(103L);
        assertThat(policyCaptor.getValue().getWarehouseId()).isEqualTo(203L);
        assertThat(policyCaptor.getValue().getSafetyStock()).isEqualByComparingTo("20");
        assertThat(policyCaptor.getValue().getMinStock()).isEqualByComparingTo("5");
        assertThat(policyCaptor.getValue().getMaxStock()).isEqualByComparingTo("100");
    }

    @Test
    void stockInitImportWritesProductPolicyWhenStrategyModeIsProduct() throws Exception {
        ErpStockCountServiceImpl service = stockCountService();
        when(stockCountMapper.selectCount(any())).thenReturn(0L);
        when(orderSequenceMapper.incrementAndGet(anyLong(), eq("STOCK_INIT"), any())).thenReturn(1L);
        doAnswer(invocation -> {
            ErpStockCount count = invocation.getArgument(0);
            count.setId(504L);
            return 1;
        }).when(stockCountMapper).insert(any(ErpStockCount.class));
        doAnswer(invocation -> {
            ErpStockCountItem item = invocation.getArgument(0);
            item.setId(704L);
            return 1;
        }).when(stockCountItemMapper).insert(any(ErpStockCountItem.class));
        when(productMapper.selectList(any())).thenReturn(List.of(product(104L)));
        when(productMapper.selectOne(any())).thenReturn(product(104L));
        when(stockBalanceMapper.findByKey(1L, 104L, 204L, null)).thenReturn(null);
        when(warehouseMapper.selectOne(any())).thenReturn(warehouse(204L));

        service.importInitStocks(
            new MockMultipartFile(
                "file",
                "stock-init-policy.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                stockInitWorkbookWithPolicyHeadersBytes()
            ),
            "库存明细浏览表",
            "{\"仓库\":\"warehouseName\",\"编码\":\"code\",\"产品名称\":\"productName\",\"库存数\":\"countedQty\",\"库存成本价\":\"initUnitCost\",\"标准库存数\":\"productSafetyStock\",\"库存下限\":\"productMinStock\",\"库存上限\":\"productMaxStock\"}",
            "PRODUCT"
        );

        ArgumentCaptor<ErpProduct> productCaptor = ArgumentCaptor.forClass(ErpProduct.class);
        verify(productMapper).updateById(productCaptor.capture());
        verify(productStockPolicyMapper, never()).insert(any(ErpProductStockPolicy.class));
        assertThat(productCaptor.getValue().getId()).isEqualTo(104L);
        assertThat(productCaptor.getValue().getSafetyStock()).isEqualByComparingTo("20");
        assertThat(productCaptor.getValue().getMinStock()).isEqualByComparingTo("5");
        assertThat(productCaptor.getValue().getMaxStock()).isEqualByComparingTo("100");
    }

    @Test
    void stockInitImportLogsMissingLocalProductAsFailedItem() throws Exception {
        List<Runnable> queuedTasks = new ArrayList<>();
        ErpStockCountServiceImpl service = stockCountService(queuedTasks::add);
        doAnswer(invocation -> {
            ErpStockInitImportBatch batch = invocation.getArgument(0);
            batch.setId(802L);
            batch.setBatchNo("SII20260602010102");
            return 1;
        }).when(stockInitImportBatchMapper).insert(any(ErpStockInitImportBatch.class));

        var result = service.importInitStocks(
            new MockMultipartFile(
                "file",
                "stock-init.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                stockInitWorkbookBytes("主仓", "PR404")
            ),
            null,
            null
        );

        assertThat(result.status()).isEqualTo("PROCESSING");
        assertThat(result.failedCount()).isZero();
        when(stockInitImportBatchMapper.selectOne(any())).thenReturn(batchForUpdate(802L));
        queuedTasks.get(0).run();
        ArgumentCaptor<ErpStockInitImportBatch> batchCaptor = ArgumentCaptor.forClass(ErpStockInitImportBatch.class);
        verify(stockInitImportBatchMapper).updateById(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(batchCaptor.getValue().getFailedCount()).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpStockInitImportItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockInitImportItemMapper).insertBatch(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue().get(0).getStatus()).isEqualTo("FAILED");
        assertThat(itemsCaptor.getValue().get(0).getErrorMessage()).contains("产品名称 PR404 不存在");
    }

    @Test
    void stockInitImportLogsDuplicateProductNameAsFailedItem() throws Exception {
        List<Runnable> queuedTasks = new ArrayList<>();
        ErpStockCountServiceImpl service = stockCountService(queuedTasks::add);
        doAnswer(invocation -> {
            ErpStockInitImportBatch batch = invocation.getArgument(0);
            batch.setId(803L);
            batch.setBatchNo("SII20260602010103");
            return 1;
        }).when(stockInitImportBatchMapper).insert(any(ErpStockInitImportBatch.class));
        when(productMapper.selectList(any())).thenReturn(List.of(product(106L), product(107L)));

        var result = service.importInitStocks(
            new MockMultipartFile(
                "file",
                "stock-init.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                stockInitWorkbookBytes("主仓", "OUTSIDE-002", "重复商品")
            ),
            null,
            null
        );

        assertThat(result.status()).isEqualTo("PROCESSING");
        assertThat(result.failedCount()).isZero();
        when(stockInitImportBatchMapper.selectOne(any())).thenReturn(batchForUpdate(803L));
        queuedTasks.get(0).run();
        ArgumentCaptor<ErpStockInitImportBatch> batchCaptor = ArgumentCaptor.forClass(ErpStockInitImportBatch.class);
        verify(stockInitImportBatchMapper).updateById(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(batchCaptor.getValue().getFailedCount()).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpStockInitImportItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockInitImportItemMapper).insertBatch(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue().get(0).getStatus()).isEqualTo("FAILED");
        assertThat(itemsCaptor.getValue().get(0).getErrorMessage()).contains("产品名称 重复商品 匹配到多个商品");
    }

    @Test
    void assemblyCreateRejectsInvalidFinishedQty() {
        ErpAssemblyOrderServiceImpl service = assemblyService();
        when(productMapper.selectOne(any())).thenReturn(product(100L));

        ErpAssemblyOrderCreateRequest request = new ErpAssemblyOrderCreateRequest(
            null,
            "ASSEMBLE",
            "2026-05-12 08:00:00",
            null,
            null,
            null,
            null,
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
            null,
            null,
            null,
            null,
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
            null,
            null,
            null,
            null,
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
            null,
            null,
            null,
            null,
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
        return stockCountService(Runnable::run);
    }

    private ErpStockCountServiceImpl stockCountService(Executor importExecutor) {
        return stockCountService(importExecutor, null);
    }

    private ErpStockCountServiceImpl stockCountService(Executor importExecutor,
                                                       org.springframework.transaction.support.TransactionOperations transactionOperations) {
        return new ErpStockCountServiceImpl(
            stockCountMapper,
            stockCountItemMapper,
            stockBalanceMapper,
            stockTxnMapper,
            orderSequenceMapper,
            systemConfigMapper,
            productMapper,
            productStockPolicyMapper,
            stockInitImportBatchMapper,
            stockInitImportItemMapper,
            warehouseMapper,
            locationMapper,
            supplierMapper,
            costService(),
            new ExcelImportParser(),
            importExecutor,
            transactionOperations
        );
    }

    private ErpStockInitImportBatch batchForUpdate(Long id) {
        ErpStockInitImportBatch batch = new ErpStockInitImportBatch();
        batch.setId(id);
        batch.setTenantId(1L);
        batch.setBatchNo("SII-" + id);
        batch.setStatus("PROCESSING");
        batch.setTotalCount(1);
        batch.setSuccessCount(0);
        batch.setFailedCount(0);
        batch.setWarningCount(0);
        return batch;
    }

    private ErpAssemblyOrderServiceImpl assemblyService() {
        return new ErpAssemblyOrderServiceImpl(
            assemblyOrderMapper,
            assemblyOrderItemMapper,
            productMapper,
            saleOrderMapper,
            saleOrderItemMapper,
            customerMapper,
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

    private ErpStockCountItem stockCountItem(Long productId, int lineNo) {
        ErpStockCountItem item = new ErpStockCountItem();
        item.setTenantId(1L);
        item.setCountId(506L);
        item.setProductId(productId);
        item.setLineNo(lineNo);
        item.setSystemQty(BigDecimal.ZERO);
        item.setCountedQty(BigDecimal.ONE);
        item.setDiffQty(BigDecimal.ONE);
        return item;
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

    private byte[] stockInitWorkbookBytes(String warehouseName) throws IOException {
        return stockInitWorkbookBytes(warehouseName, "PR001");
    }

    private byte[] stockInitWorkbookBytes(String warehouseName, String code) throws IOException {
        return stockInitWorkbookBytes(warehouseName, code, "PR001".equals(code) ? "Product-100" : code);
    }

    private byte[] stockInitWorkbookBytes(String warehouseName, String code, String productName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("仓库");
            header.createCell(1).setCellValue("编码");
            header.createCell(2).setCellValue("产品名称");
            header.createCell(3).setCellValue("规格");
            header.createCell(4).setCellValue("品牌");
            header.createCell(5).setCellValue("库存数");
            header.createCell(6).setCellValue("库存成本价");
            header.createCell(7).setCellValue("金额");
            header.createCell(8).setCellValue("来源供应商");

            Row row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue(warehouseName);
            row.createCell(1).setCellValue(code);
            row.createCell(2).setCellValue(productName);
            row.createCell(3).setCellValue("S1");
            row.createCell(4).setCellValue("B1");
            row.createCell(5).setCellValue("6");
            row.createCell(6).setCellValue("18.5");
            row.createCell(7).setCellValue("111");
            row.createCell(8).setCellValue("未匹配供应商");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] stockInitDuplicateStockKeyWorkbookBytes() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("仓库");
            header.createCell(1).setCellValue("编码");
            header.createCell(2).setCellValue("产品名称");
            header.createCell(3).setCellValue("库存数");
            header.createCell(4).setCellValue("库存成本价");

            Row first = workbook.getSheetAt(0).createRow(1);
            first.createCell(0).setCellValue("默认仓库");
            first.createCell(1).setCellValue("DUP-001");
            first.createCell(2).setCellValue("Product-111");
            first.createCell(3).setCellValue("12");
            first.createCell(4).setCellValue("25");

            Row second = workbook.getSheetAt(0).createRow(2);
            second.createCell(0).setCellValue("默认仓库");
            second.createCell(1).setCellValue("DUP-002");
            second.createCell(2).setCellValue("Product-111");
            second.createCell(3).setCellValue("8");
            second.createCell(4).setCellValue("26");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] stockInitWorkbookBytes(int rowCount) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("仓库");
            header.createCell(1).setCellValue("编码");
            header.createCell(2).setCellValue("产品名称");
            header.createCell(3).setCellValue("库存数");
            header.createCell(4).setCellValue("库存成本价");

            IntStream.rangeClosed(1, rowCount).forEach(index -> {
                Row row = workbook.getSheetAt(0).createRow(index);
                row.createCell(0).setCellValue("主仓-" + index);
                row.createCell(1).setCellValue("PR" + index);
                row.createCell(2).setCellValue("Product-108");
                row.createCell(3).setCellValue("6");
                row.createCell(4).setCellValue("18.5");
            });

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] stockInitWorkbookWithCustomHeadersBytes() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("仓库名称");
            header.createCell(1).setCellValue("库位名称");
            header.createCell(2).setCellValue("物料编号");
            header.createCell(3).setCellValue("物料名称");
            header.createCell(4).setCellValue("期初数量");
            header.createCell(5).setCellValue("期初单价");

            Row row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue("主仓");
            row.createCell(1).setCellValue("A库位");
            row.createCell(2).setCellValue("PR002");
            row.createCell(3).setCellValue("Product-101");
            row.createCell(4).setCellValue("9");
            row.createCell(5).setCellValue("21");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] stockInitWorkbookWithPolicyHeadersBytes() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("仓库");
            header.createCell(1).setCellValue("编码");
            header.createCell(2).setCellValue("产品名称");
            header.createCell(3).setCellValue("库存数");
            header.createCell(4).setCellValue("库存成本价");
            header.createCell(5).setCellValue("标准库存数");
            header.createCell(6).setCellValue("库存下限");
            header.createCell(7).setCellValue("库存上限");

            Row row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue("正品仓");
            row.createCell(1).setCellValue("PR003");
            row.createCell(2).setCellValue("Product-103");
            row.createCell(3).setCellValue("12");
            row.createCell(4).setCellValue("25");
            row.createCell(5).setCellValue("20");
            row.createCell(6).setCellValue("5");
            row.createCell(7).setCellValue("100");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
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
