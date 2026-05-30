package com.example.wms;

import com.example.wms.controller.erp.ErpCustomerController;
import com.example.wms.controller.erp.ErpProductController;
import com.example.wms.dto.erp.ErpCategoryUpdateRequest;
import com.example.wms.dto.erp.ErpCounterpartyUnbindCheck;
import com.example.wms.dto.erp.ErpCustomerCreateRequest;
import com.example.wms.dto.erp.ErpCustomerUpdateRequest;
import com.example.wms.dto.erp.ErpDeliveryMethodCreateRequest;
import com.example.wms.dto.erp.ErpPaymentMethodCreateRequest;
import com.example.wms.dto.erp.ErpPrintTemplateCreateRequest;
import com.example.wms.dto.erp.ErpProductCreateRequest;
import com.example.wms.dto.erp.ErpProductPriceItemRequest;
import com.example.wms.dto.erp.ErpProductUpdateRequest;
import com.example.wms.dto.erp.ErpSettlementMethodCreateRequest;
import com.example.wms.dto.erp.ErpSupplierCreateRequest;
import com.example.wms.entity.erp.ErpSupplierType;
import com.example.wms.dto.erp.ErpVehicleBrandCreateRequest;
import com.example.wms.dto.erp.ErpVehicleModelCreateRequest;
import com.example.wms.dto.erp.ErpVehicleSeriesCreateRequest;
import com.example.wms.entity.erp.ErpCategory;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpPaymentMethod;
import com.example.wms.entity.erp.ErpPrintTemplate;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpProductImportBatch;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.entity.erp.ErpSettlementMethod;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.entity.erp.ErpUnit;
import com.example.wms.entity.erp.ErpVehicleBrand;
import com.example.wms.entity.erp.ErpVehicleModel;
import com.example.wms.entity.erp.ErpVehicleSeries;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpCategoryMapper;
import com.example.wms.mapper.erp.ErpCustomerCategoryMapper;
import com.example.wms.mapper.erp.ErpCustomerImportBatchMapper;
import com.example.wms.mapper.erp.ErpCustomerImportItemMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpCounterpartySubjectMapper;
import com.example.wms.mapper.erp.ErpCounterpartySubjectLinkMapper;
import com.example.wms.mapper.erp.ErpDeliveryMethodMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPaymentMethodMapper;
import com.example.wms.mapper.erp.ErpPrintLogMapper;
import com.example.wms.mapper.erp.ErpPrintTemplateMapper;
import com.example.wms.mapper.erp.ErpProductFitmentMapper;
import com.example.wms.mapper.erp.ErpProductImportBatchMapper;
import com.example.wms.mapper.erp.ErpProductImportItemMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpProductPriceMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpReceiptMethodMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.mapper.erp.ErpSupplierImportBatchMapper;
import com.example.wms.mapper.erp.ErpSupplierImportItemMapper;
import com.example.wms.mapper.erp.ErpSupplierTypeMapper;
import com.example.wms.mapper.erp.ErpUnitMapper;
import com.example.wms.mapper.erp.ErpVehicleBrandMapper;
import com.example.wms.mapper.erp.ErpVehicleModelMapper;
import com.example.wms.mapper.erp.ErpVehicleSeriesMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.impl.ErpCategoryServiceImpl;
import com.example.wms.service.erp.impl.ErpCustomerCategoryServiceImpl;
import com.example.wms.service.erp.impl.ErpCustomerServiceImpl;
import com.example.wms.service.erp.impl.ErpDeliveryMethodServiceImpl;
import com.example.wms.service.erp.impl.ErpPaymentMethodServiceImpl;
import com.example.wms.service.erp.impl.ErpPrintTemplateServiceImpl;
import com.example.wms.service.erp.impl.ErpProductServiceImpl;
import com.example.wms.service.erp.impl.ErpSettlementMethodServiceImpl;
import com.example.wms.service.erp.impl.ErpSupplierServiceImpl;
import com.example.wms.service.erp.impl.ErpSupplierTypeServiceImpl;
import com.example.wms.service.erp.impl.ErpUnitServiceImpl;
import com.example.wms.service.erp.impl.ErpVehicleBrandServiceImpl;
import com.example.wms.service.erp.impl.ErpVehicleModelServiceImpl;
import com.example.wms.service.erp.impl.ErpVehicleSeriesServiceImpl;
import com.example.wms.service.erp.support.ExcelImportParser;
import com.example.wms.service.erp.support.ErpCounterpartyGuardRules;
import com.example.wms.service.erp.support.ErpMasterDataCodeGenerator;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class ErpMasterDataGuardTests {
    @Mock private ErpProductMapper productMapper;
    @Mock private ErpProductPriceMapper productPriceMapper;
    @Mock private ErpOrderSequenceMapper orderSequenceMapper;
    @Mock private SystemConfigMapper systemConfigMapper;
    @Mock private ErpCategoryMapper categoryMapper;
    @Mock private ErpUnitMapper unitMapper;
    @Mock private ErpWarehouseMapper warehouseMapper;
    @Mock private ErpLocationMapper locationMapper;
    @Mock private ErpCustomerCategoryMapper customerCategoryMapper;
    @Mock private ObjectMapper objectMapper;
    @Mock private ErpProductImportBatchMapper productImportBatchMapper;
    @Mock private ErpProductImportItemMapper productImportItemMapper;
    @Mock private ErpCustomerImportBatchMapper customerImportBatchMapper;
    @Mock private ErpCustomerImportItemMapper customerImportItemMapper;

    @Mock private ErpCustomerMapper customerMapper;
    @Mock private ErpSupplierMapper supplierMapper;
    @Mock private ErpSupplierTypeMapper supplierTypeMapper;
    @Mock private ErpCounterpartySubjectMapper counterpartySubjectMapper;
    @Mock private ErpCounterpartySubjectLinkMapper counterpartySubjectLinkMapper;
    @Mock private ErpSaleOrderMapper saleOrderMapper;
    @Mock private ErpSaleReturnMapper saleReturnMapper;
    @Mock private ErpReceiptMapper receiptMapper;
    @Mock private ErpReceiptMethodMapper receiptMethodMapper;
    @Mock private ErpAccountsReceivableMapper accountsReceivableMapper;
    @Mock private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Mock private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock private ErpPaymentMapper paymentMapper;
    @Mock private ErpAccountsPayableMapper accountsPayableMapper;
    @Mock private ErpSettlementMethodMapper settlementMethodMapper;
    @Mock private ErpDeliveryMethodMapper deliveryMethodMapper;
    @Mock private ErpPaymentMethodMapper paymentMethodMapper;
    @Mock private ErpSupplierImportBatchMapper supplierImportBatchMapper;
    @Mock private ErpSupplierImportItemMapper supplierImportItemMapper;
    @Mock private ErpPrintTemplateMapper printTemplateMapper;
    @Mock private ErpPrintLogMapper printLogMapper;
    @Mock private ErpVehicleBrandMapper vehicleBrandMapper;
    @Mock private ErpVehicleSeriesMapper vehicleSeriesMapper;
    @Mock private ErpVehicleModelMapper vehicleModelMapper;
    @Mock private ErpProductFitmentMapper productFitmentMapper;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void productUpdateRejectsLocationOutsideWarehouse() {
        ErpProductServiceImpl service = productService();
        ErpProduct existing = new ErpProduct();
        existing.setId(7L);
        existing.setTenantId(1L);
        existing.setCode("P-007");
        existing.setName("product");

        ErpLocation location = new ErpLocation();
        location.setId(20L);
        location.setWarehouseId(99L);

        when(productMapper.selectOne(any())).thenReturn(existing);
        when(productMapper.findByCode(1L, "P-007")).thenReturn(existing);
        when(warehouseMapper.selectCount(any())).thenReturn(1L);
        when(locationMapper.selectOne(any())).thenReturn(location);

        ErpProductUpdateRequest request = new ErpProductUpdateRequest(
            "P-007",
            "product",
            null,
            null,
            null,
            null,
            null,
            null,
            10L,
            20L,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            null,
            null
        );

        assertThatThrownBy(() -> service.update(7L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("默认库位不属于所选默认仓库");
    }

    @Test
    void productCreateRejectsDuplicateCategoryPrices() {
        ErpProductServiceImpl service = productService();
        when(productMapper.findByCode(1L, "P-008")).thenReturn(null);
        when(customerCategoryMapper.selectCount(any())).thenReturn(1L);

        ErpProductCreateRequest request = new ErpProductCreateRequest(
            "P-008",
            "product",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            null,
            List.of(
                new ErpProductPriceItemRequest(1L, new BigDecimal("10")),
                new ErpProductPriceItemRequest(1L, new BigDecimal("12"))
            )
        );

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("客户类别价格存在重复项");
    }

    @Test
    void productCreateRejectsSourceSupplierOutsideTenant() {
        ErpProductServiceImpl service = productService();
        when(productMapper.findByCode(1L, "P-009")).thenReturn(null);
        when(supplierMapper.selectCount(any())).thenReturn(0L);

        ErpProductCreateRequest request = new ErpProductCreateRequest(
            "P-009",
            "product",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "M-CODE",
            "M-MODEL",
            "M-NAME",
            9L,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            null,
            null
        );

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("来源供应商不存在");
    }

    @Test
    void customerImportEndpointUsesMultipartExcelUploadContract() throws Exception {
        Method controllerMethod = ErpCustomerController.class.getMethod("importCustomers", MultipartFile.class, String.class);
        PostMapping postMapping = controllerMethod.getAnnotation(PostMapping.class);
        Parameter[] parameters = controllerMethod.getParameters();

        assertThat(postMapping).isNotNull();
        assertThat(postMapping.value()).containsExactly("/import");
        assertThat(postMapping.consumes()).contains(MediaType.MULTIPART_FORM_DATA_VALUE);
        assertThat(parameters[0].getAnnotation(RequestParam.class).value()).isEqualTo("file");
        assertThat(parameters[1].getAnnotation(RequestParam.class).value()).isEqualTo("sourceName");
        assertThat(parameters[1].getAnnotation(RequestParam.class).required()).isFalse();
    }

    @Test
    void customerImportEndpointReturnsAsyncBatchResultContract() throws Exception {
        Method controllerMethod = ErpCustomerController.class.getMethod("importCustomers", MultipartFile.class, String.class);

        assertThat(controllerMethod.getGenericReturnType().getTypeName())
            .contains("ErpCustomerImportResult");
    }

    @Test
    void customerImportControllerExposesBatchQueryEndpoints() throws Exception {
        Method batchListMethod = ErpCustomerController.class.getMethod("listImportBatches");
        Method batchItemsMethod = ErpCustomerController.class.getMethod("listImportBatchItems", Long.class);
        GetMapping batchListMapping = batchListMethod.getAnnotation(GetMapping.class);
        GetMapping batchItemsMapping = batchItemsMethod.getAnnotation(GetMapping.class);
        Parameter[] batchItemsParameters = batchItemsMethod.getParameters();

        assertThat(batchListMapping).isNotNull();
        assertThat(batchListMapping.value()).containsExactly("/import-batches");
        assertThat(batchItemsMapping).isNotNull();
        assertThat(batchItemsMapping.value()).containsExactly("/import-batches/{batchId}/items");
        assertThat(batchItemsParameters[0].getAnnotation(PathVariable.class).value()).isEqualTo("batchId");
    }

    @Test
    void customerImportRejectsUnknownSettlementMethod() throws Exception {
        ErpCustomerServiceImpl service = customerAsyncService();
        when(customerCategoryMapper.findDefault(1L)).thenReturn(customerCategory(3L, "默认客户"));
        doAnswer(invocation -> {
            com.example.wms.entity.erp.ErpCustomerImportBatch batch = invocation.getArgument(0);
            batch.setId(66L);
            return 1;
        }).when(customerImportBatchMapper).insert(any(com.example.wms.entity.erp.ErpCustomerImportBatch.class));

        var result = service.importCustomers(
            new MockMultipartFile("file", "customer.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", customerWorkbookBytes()),
            null
        );

        assertThat(result.batchId()).isEqualTo(66L);
    }

    @Test
    void customerImportReturnsProcessingBeforeBackgroundRowsRun() throws Exception {
        List<Runnable> queuedTasks = new ArrayList<>();
        ErpCustomerServiceImpl service = customerAsyncService(queuedTasks::add, null);
        when(customerCategoryMapper.findDefault(1L)).thenReturn(customerCategory(3L, "默认客户"));
        doAnswer(invocation -> {
            com.example.wms.entity.erp.ErpCustomerImportBatch batch = invocation.getArgument(0);
            batch.setId(67L);
            return 1;
        }).when(customerImportBatchMapper).insert(any(com.example.wms.entity.erp.ErpCustomerImportBatch.class));

        var result = service.importCustomers(
            new MockMultipartFile("file", "customer.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", customerWorkbookBytes()),
            "历史客户表"
        );

        assertThat(result.batchId()).isEqualTo(67L);
        assertThat(result.status()).isEqualTo("PROCESSING");
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.successCount()).isZero();
        assertThat(result.failedCount()).isZero();
        assertThat(queuedTasks).hasSize(1);
        verify(customerMapper, never()).insert(any(ErpCustomer.class));
        verify(customerImportItemMapper, never()).insert(any(com.example.wms.entity.erp.ErpCustomerImportItem.class));
        verify(customerImportItemMapper, never()).insertBatch(any());
    }

    @Test
    void customerImportProcessesBackgroundRowsInTransactionChunks() throws Exception {
        AtomicInteger transactionCount = new AtomicInteger();
        ErpCustomerServiceImpl service = customerAsyncService(
            Runnable::run,
            new org.springframework.transaction.support.TransactionOperations() {
                @Override
                public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
                    transactionCount.incrementAndGet();
                    return action.doInTransaction(null);
                }
            }
        );
        AtomicInteger customerId = new AtomicInteger(100);
        when(customerCategoryMapper.findDefault(1L)).thenReturn(customerCategory(3L, "默认客户"));
        doAnswer(invocation -> {
            com.example.wms.entity.erp.ErpCustomerImportBatch batch = invocation.getArgument(0);
            batch.setId(68L);
            return 1;
        }).when(customerImportBatchMapper).insert(any(com.example.wms.entity.erp.ErpCustomerImportBatch.class));
        when(customerImportBatchMapper.selectOne(any())).thenAnswer(invocation -> {
            com.example.wms.entity.erp.ErpCustomerImportBatch batch = new com.example.wms.entity.erp.ErpCustomerImportBatch();
            batch.setId(68L);
            batch.setTenantId(1L);
            batch.setTotalCount(205);
            return batch;
        });
        when(customerMapper.findByCodes(any(), any())).thenReturn(List.of());
        doAnswer(invocation -> {
            ErpCustomer customer = invocation.getArgument(0);
            customer.setId((long) customerId.incrementAndGet());
            return 1;
        }).when(customerMapper).insert(any(ErpCustomer.class));

        var result = service.importCustomers(
            new MockMultipartFile("file", "customer-bulk.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", customerWorkbookRows(205)),
            "历史客户表"
        );

        assertThat(result.status()).isEqualTo("PROCESSING");
        assertThat(transactionCount).hasValue(4);
        verify(customerMapper, times(3)).findByCodes(any(), any());
        verify(customerMapper, times(205)).insert(any(ErpCustomer.class));
        verify(customerImportItemMapper, never()).insert(any(com.example.wms.entity.erp.ErpCustomerImportItem.class));
        verify(customerImportItemMapper, times(3)).insertBatch(any());
    }

    @Test
    void customerImportExtractsPhoneAndMobileFromLongLegacyContactField() throws Exception {
        ErpCustomerServiceImpl service = customerService();
        ErpCustomer[] inserted = new ErpCustomer[1];
        when(customerCategoryMapper.findDefault(1L)).thenReturn(customerCategory(3L, "默认客户"));
        when(settlementMethodMapper.selectList(any())).thenReturn(List.of(settlementMethod("SM-CASH", "现结")));
        when(customerMapper.findByCode(1L, "CU002")).thenReturn(null);
        doAnswer(invocation -> {
            ErpCustomer customer = invocation.getArgument(0);
            inserted[0] = customer;
            customer.setId(9L);
            return 1;
        }).when(customerMapper).insert(any(ErpCustomer.class));

        var result = service.importCustomers(
            new MockMultipartFile("file", "customer-contact.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", customerWorkbookWithLongContactBytes()),
            null
        );

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(inserted[0]).isNotNull();
        assertThat(inserted[0].getPhone()).isEqualTo("021-66554433");
        assertThat(inserted[0].getMobile()).isEqualTo("13800138000");
    }

    @Test
    void customerImportDoesNotTruncateMobileOnlyLegacyContactFieldIntoPhone() throws Exception {
        ErpCustomerServiceImpl service = customerService();
        ErpCustomer[] inserted = new ErpCustomer[1];
        when(customerCategoryMapper.findDefault(1L)).thenReturn(customerCategory(3L, "默认客户"));
        when(settlementMethodMapper.selectList(any())).thenReturn(List.of(settlementMethod("SM-CASH", "现结")));
        when(customerMapper.findByCode(1L, "CU003")).thenReturn(null);
        doAnswer(invocation -> {
            ErpCustomer customer = invocation.getArgument(0);
            inserted[0] = customer;
            customer.setId(10L);
            return 1;
        }).when(customerMapper).insert(any(ErpCustomer.class));

        var result = service.importCustomers(
            new MockMultipartFile("file", "customer-contact.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", customerWorkbookWithLegacyContactBytes("CU003", "客户C", "13508805134/15911504154")),
            null
        );

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(inserted[0]).isNotNull();
        assertThat(inserted[0].getPhone()).isNull();
        assertThat(inserted[0].getMobile()).isEqualTo("13508805134");
        assertThat(inserted[0].getContacts()).isNotNull();
        assertThat(inserted[0].getContacts().toString()).doesNotContain("\"phone\":\"13508805\"");
        assertThat(inserted[0].getContacts().toString()).contains("\"mobile\":\"13508805134\"");
        assertThat(inserted[0].getContacts().toString()).contains("\"mobile\":\"15911504154\"");
    }

    @Test
    void customerImportKeepsSpaceSeparatedLegacyContactAsOneContactValue() throws Exception {
        ErpCustomerServiceImpl service = customerService();
        ErpCustomer[] inserted = new ErpCustomer[1];
        when(customerCategoryMapper.findDefault(1L)).thenReturn(customerCategory(3L, "默认客户"));
        when(settlementMethodMapper.selectList(any())).thenReturn(List.of(settlementMethod("SM-CASH", "现结")));
        when(customerMapper.findByCode(1L, "CU004")).thenReturn(null);
        doAnswer(invocation -> {
            ErpCustomer customer = invocation.getArgument(0);
            inserted[0] = customer;
            customer.setId(11L);
            return 1;
        }).when(customerMapper).insert(any(ErpCustomer.class));

        service.importCustomers(
            new MockMultipartFile("file", "customer-contact.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", customerWorkbookWithLegacyContactBytes("CU004", "客户D", "021-66554433 13800138000")),
            null
        );

        assertThat(inserted[0]).isNotNull();
        assertThat(inserted[0].getPhone()).isEqualTo("021-66554433");
        assertThat(inserted[0].getMobile()).isEqualTo("13800138000");
        assertThat(inserted[0].getContacts().toString()).contains("\"phone\":\"021-66554433\"");
        assertThat(inserted[0].getContacts().toString()).contains("\"mobile\":\"13800138000\"");
    }

    @Test
    void customerCreateFallsBackToDefaultSettlementMethodWhenSubmittedCodeDoesNotExist() {
        ErpCustomerServiceImpl service = customerService();
        when(customerCategoryMapper.findDefault(1L)).thenReturn(customerCategory(3L, "默认客户"));
        when(customerMapper.findByCode(1L, "CU-NEW")).thenReturn(null);
        when(settlementMethodMapper.findByCode(1L, "MISSING")).thenReturn(null);
        when(settlementMethodMapper.findDefault(1L)).thenReturn(settlementMethod("SM-DEFAULT", "默认结算"));

        service.create(new ErpCustomerCreateRequest(
            "CU-NEW",
            "客户A",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "MISSING",
            null,
            null,
            null,
            null,
            null,
            true,
            null
        ));

        org.mockito.ArgumentCaptor<ErpCustomer> captor = org.mockito.ArgumentCaptor.forClass(ErpCustomer.class);
        org.mockito.Mockito.verify(customerMapper).insert(captor.capture());
        assertThat(captor.getValue().getDefaultSettlementMethodCode()).isEqualTo("SM-DEFAULT");
    }

    @Test
    void customerUpdateFallsBackToDefaultSettlementMethodWhenSubmittedCodeDoesNotExist() {
        ErpCustomerServiceImpl service = customerService();
        ErpCustomer existing = customer(5L);
        existing.setDefaultSettlementMethodCode("OLD");
        when(customerMapper.selectOne(any())).thenReturn(existing);
        when(customerMapper.findByCode(1L, "CU-5")).thenReturn(existing);
        when(settlementMethodMapper.findByCode(1L, "MISSING")).thenReturn(null);
        when(settlementMethodMapper.findDefault(1L)).thenReturn(settlementMethod("SM-DEFAULT", "默认结算"));

        service.update(5L, new ErpCustomerUpdateRequest(
            "CU-5",
            "客户A",
            3L,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "MISSING",
            null,
            null,
            null,
            null,
            null,
            true,
            null
        ));

        assertThat(existing.getDefaultSettlementMethodCode()).isEqualTo("SM-DEFAULT");
    }

    @Test
    void productImportEndpointUsesMultipartExcelUploadContract() throws Exception {
        Method controllerMethod = ErpProductController.class.getMethod("importProducts", MultipartFile.class, String.class);
        PostMapping postMapping = controllerMethod.getAnnotation(PostMapping.class);
        Parameter[] parameters = controllerMethod.getParameters();

        assertThat(postMapping).isNotNull();
        assertThat(postMapping.value()).containsExactly("/import");
        assertThat(postMapping.consumes()).contains(MediaType.MULTIPART_FORM_DATA_VALUE);
        assertThat(parameters[0].getAnnotation(RequestParam.class).value()).isEqualTo("file");
        assertThat(parameters[1].getAnnotation(RequestParam.class).value()).isEqualTo("sourceName");
        assertThat(parameters[1].getAnnotation(RequestParam.class).required()).isFalse();
    }

    @Test
    void productImportEndpointReturnsAsyncBatchResultContract() throws Exception {
        Method controllerMethod = ErpProductController.class.getMethod("importProducts", MultipartFile.class, String.class);

        assertThat(controllerMethod.getGenericReturnType().getTypeName())
            .contains("ErpProductImportResult");
    }

    @Test
    void productImportControllerExposesBatchQueryEndpoints() throws Exception {
        Method batchListMethod = ErpProductController.class.getMethod("listImportBatches");
        Method batchItemsMethod = ErpProductController.class.getMethod("listImportBatchItems", Long.class);
        GetMapping batchListMapping = batchListMethod.getAnnotation(GetMapping.class);
        GetMapping batchItemsMapping = batchItemsMethod.getAnnotation(GetMapping.class);
        Parameter[] batchItemsParameters = batchItemsMethod.getParameters();

        assertThat(batchListMapping).isNotNull();
        assertThat(batchListMapping.value()).containsExactly("/import-batches");
        assertThat(batchItemsMapping).isNotNull();
        assertThat(batchItemsMapping.value()).containsExactly("/import-batches/{batchId}/items");
        assertThat(batchItemsParameters[0].getAnnotation(PathVariable.class).value()).isEqualTo("batchId");
    }

    @Test
    void productImportStartsAsyncBatchEvenWhenRowsMayFailLater() throws Exception {
        ErpProductServiceImpl service = productService();
        doAnswer(invocation -> {
            com.example.wms.entity.erp.ErpProductImportBatch batch = invocation.getArgument(0);
            batch.setId(88L);
            return 1;
        }).when(productImportBatchMapper).insert(any(com.example.wms.entity.erp.ErpProductImportBatch.class));

        assertThat(service.importProducts(
            new MockMultipartFile("file", "product.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", productWorkbookBytes()),
            null
        ).batchId()).isEqualTo(88L);
    }

    @Test
    void productImportReturnsProcessingBeforeBackgroundRowsRun() throws Exception {
        List<Runnable> queuedTasks = new ArrayList<>();
        ErpProductServiceImpl service = productAsyncService(queuedTasks::add, null);
        doAnswer(invocation -> {
            com.example.wms.entity.erp.ErpProductImportBatch batch = invocation.getArgument(0);
            batch.setId(89L);
            return 1;
        }).when(productImportBatchMapper).insert(any(com.example.wms.entity.erp.ErpProductImportBatch.class));

        var result = service.importProducts(
            new MockMultipartFile("file", "product.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", productWorkbookBytes()),
            "历史商品表"
        );

        assertThat(result.batchId()).isEqualTo(89L);
        assertThat(result.status()).isEqualTo("PROCESSING");
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.successCount()).isZero();
        assertThat(result.failedCount()).isZero();
        assertThat(queuedTasks).hasSize(1);
        verify(productMapper, never()).insert(any(ErpProduct.class));
        verify(productImportItemMapper, never()).insert(any(com.example.wms.entity.erp.ErpProductImportItem.class));
        verify(productImportItemMapper, never()).insertBatch(any());
    }

    @Test
    void productImportUsesDefaultCategoryWhenSourceCategoryDoesNotMatch() throws Exception {
        ErpProductServiceImpl service = productService();
        doAnswer(invocation -> {
            com.example.wms.entity.erp.ErpProductImportBatch batch = invocation.getArgument(0);
            batch.setId(90L);
            return 1;
        }).when(productImportBatchMapper).insert(any(com.example.wms.entity.erp.ErpProductImportBatch.class));
        when(productImportBatchMapper.selectOne(any())).thenReturn(productImportBatch(90L));
        when(productMapper.findByCodes(1L, List.of("PR001"))).thenReturn(List.of());
        when(productMapper.findByCode(1L, "PR001")).thenReturn(null);
        when(categoryMapper.selectOne(any())).thenReturn(null, category(11L, null));
        when(unitMapper.selectOne(any())).thenReturn(unit(21L, "个"));
        doAnswer(invocation -> {
            ErpProduct product = invocation.getArgument(0);
            product.setId(31L);
            return 1;
        }).when(productMapper).insert(org.mockito.ArgumentMatchers.<ErpProduct>any());
        when(objectMapper.valueToTree(any())).thenAnswer(invocation -> new ObjectMapper().valueToTree(invocation.getArgument(0)));

        service.importProducts(
            new MockMultipartFile("file", "product.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", productWorkbookBytes()),
            null
        );

        verify(productMapper).insert(org.mockito.ArgumentMatchers.<ErpProduct>argThat(product -> Long.valueOf(11L).equals(product.getCategoryId())));
        verify(productImportItemMapper).insertBatch(argThat(items ->
            items.size() == 1
                && "SUCCESS".equals(items.get(0).getStatus())
                && items.get(0).getWarningMessage() != null
                && items.get(0).getWarningMessage().contains("类别“未知类别”未匹配，已使用默认类别")
        ));
    }

    @Test
    void productImportWritesRetailWholesalePricesAndKeepsBackupPrice() throws Exception {
        ErpProductServiceImpl service = productService();
        doAnswer(invocation -> {
            com.example.wms.entity.erp.ErpProductImportBatch batch = invocation.getArgument(0);
            batch.setId(91L);
            return 1;
        }).when(productImportBatchMapper).insert(any(com.example.wms.entity.erp.ErpProductImportBatch.class));
        when(productImportBatchMapper.selectOne(any())).thenReturn(productImportBatch(91L));
        when(productMapper.findByCodes(1L, List.of("PR002"))).thenReturn(List.of());
        when(productMapper.findByCode(1L, "PR002")).thenReturn(null);
        when(categoryMapper.selectOne(any())).thenReturn(category(12L, null));
        when(unitMapper.selectOne(any())).thenReturn(unit(22L, "个"));
        when(customerCategoryMapper.findByCode(1L, "CUST-RETAIL")).thenReturn(customerCategory(101L, "CUST-RETAIL", "零售客户"));
        when(customerCategoryMapper.findByCode(1L, "CUST-WHOLE")).thenReturn(customerCategory(102L, "CUST-WHOLE", "批发客户"));
        doAnswer(invocation -> {
            ErpProduct product = invocation.getArgument(0);
            product.setId(32L);
            return 1;
        }).when(productMapper).insert(org.mockito.ArgumentMatchers.<ErpProduct>any());
        when(objectMapper.valueToTree(any())).thenAnswer(invocation -> new ObjectMapper().valueToTree(invocation.getArgument(0)));

        service.importProducts(
            new MockMultipartFile("file", "product-price.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", productWorkbookWithPricesBytes()),
            null
        );

        verify(productMapper).insert(org.mockito.ArgumentMatchers.<ErpProduct>argThat(product ->
            new BigDecimal("1650").compareTo(product.getCostPrice()) == 0
                && new BigDecimal("88").compareTo(product.getSalePrice()) == 0
                && product.getExtAttrs() != null
                && product.getExtAttrs().has("backupPrice1")
                && product.getExtAttrs().get("backupPrice1").decimalValue().compareTo(BigDecimal.ZERO) == 0
        ));
        verify(productPriceMapper).upsertActivePrice(1L, 32L, 101L, new BigDecimal("88"));
        verify(productPriceMapper).upsertActivePrice(1L, 32L, 102L, BigDecimal.ZERO);
        verify(productImportItemMapper).insertBatch(argThat(items ->
            items.size() == 1
                && "SUCCESS".equals(items.get(0).getStatus())
                && items.get(0).getNormalizedPayload() != null
                && items.get(0).getNormalizedPayload().has("priceCategoryUpdates")
        ));
    }

    @Test
    void categoryUpdateRejectsCycleParent() {
        ErpCategoryServiceImpl service = new ErpCategoryServiceImpl(categoryMapper, productMapper, orderSequenceMapper, systemConfigMapper);
        ErpCategory self = category(10L, 20L);
        ErpCategory parent = category(20L, 10L);

        when(categoryMapper.selectOne(any())).thenReturn(self, parent, self);
        when(categoryMapper.findByCode(1L, "CAT-10")).thenReturn(self);

        ErpCategoryUpdateRequest request = new ErpCategoryUpdateRequest("CAT-10", "分类", 20L, 1, 0, true, null, null);

        assertThatThrownBy(() -> service.update(10L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("父级分类不能形成循环关系");
    }

    @Test
    void categoryCreateClearsOtherDefaultCategories() {
        ErpCategoryServiceImpl service = new ErpCategoryServiceImpl(categoryMapper, productMapper, orderSequenceMapper, systemConfigMapper);
        when(categoryMapper.findByCode(1L, "CAT-DEFAULT")).thenReturn(null);
        doAnswer(invocation -> {
            ErpCategory category = invocation.getArgument(0);
            category.setId(99L);
            return 1;
        }).when(categoryMapper).insert(any(ErpCategory.class));

        service.create(new com.example.wms.dto.erp.ErpCategoryCreateRequest(
            "CAT-DEFAULT",
            "默认分类",
            null,
            1,
            0,
            true,
            true,
            null
        ));

        verify(categoryMapper).clearDefault(1L);
        verify(categoryMapper).insert(org.mockito.ArgumentMatchers.<ErpCategory>argThat(category -> Boolean.TRUE.equals(category.getIsDefault())));
    }

    @Test
    void categoryUpdateClearsOtherDefaultCategories() {
        ErpCategoryServiceImpl service = new ErpCategoryServiceImpl(categoryMapper, productMapper, orderSequenceMapper, systemConfigMapper);
        ErpCategory category = category(99L, null);
        when(categoryMapper.selectOne(any())).thenReturn(category);
        when(categoryMapper.findByCode(1L, "CAT-99")).thenReturn(category);

        service.update(99L, new ErpCategoryUpdateRequest("CAT-99", "默认分类", null, 1, 0, true, true, null));

        verify(categoryMapper).updateById(org.mockito.ArgumentMatchers.<ErpCategory>argThat(updated -> Boolean.TRUE.equals(updated.getIsDefault())));
        verify(categoryMapper).clearDefault(1L, 99L);
    }

    @Test
    void categoryDeleteRejectsReferencedProduct() {
        ErpCategoryServiceImpl service = new ErpCategoryServiceImpl(categoryMapper, productMapper, orderSequenceMapper, systemConfigMapper);
        when(categoryMapper.selectOne(any())).thenReturn(category(10L, null));
        when(categoryMapper.selectCount(any())).thenReturn(0L);
        when(productMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(10L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("分类已被商品引用，不能删除");
    }

    @Test
    void customerCategoryDeleteRejectsReferencedPrices() {
        ErpCustomerCategoryServiceImpl service = new ErpCustomerCategoryServiceImpl(
            customerCategoryMapper, customerMapper, productPriceMapper, orderSequenceMapper, systemConfigMapper
        );
        when(customerCategoryMapper.selectOne(any())).thenReturn(new com.example.wms.entity.erp.ErpCustomerCategory());
        when(customerMapper.selectCount(any())).thenReturn(0L);
        when(productPriceMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(3L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("客户类别已被商品价格引用，不能删除");
    }

    @Test
    void unitDeleteRejectsReferencedProduct() {
        ErpUnitServiceImpl service = new ErpUnitServiceImpl(unitMapper, productMapper, codeGenerator());
        when(unitMapper.selectOne(any())).thenReturn(new com.example.wms.entity.erp.ErpUnit());
        when(productMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(2L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("单位已被商品引用，不能删除");
    }

    @Test
    void customerDeleteRejectsReferencedSaleOrder() {
        ErpCustomerServiceImpl service = customerService();
        when(customerMapper.selectOne(any())).thenReturn(customer(5L));
        when(saleOrderMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(5L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("客户已被销售单引用，不能删除");
    }

    @Test
    void supplierDeleteRejectsReferencedPurchaseOrder() {
        ErpSupplierServiceImpl service = supplierService();
        when(supplierMapper.selectOne(any())).thenReturn(supplier(6L));
        when(purchaseOrderMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(6L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("供应商已被采购单引用，不能删除");
    }

    @Test
    void customerRebindCheckReturnsStructuredBlockingDocs() {
        ErpCustomerServiceImpl service = customerService();
        ErpCustomer customer = customer(5L);
        customer.setCounterpartySubjectId(10L);
        ErpSaleOrder saleOrder = new ErpSaleOrder();
        saleOrder.setId(101L);
        saleOrder.setOrderNo("SO202605290001");
        saleOrder.setStatus("APPROVED");

        when(customerMapper.selectOne(any())).thenReturn(customer);
        when(saleOrderMapper.selectList(any())).thenReturn(List.of(saleOrder));
        when(saleReturnMapper.selectList(any())).thenReturn(List.of());
        when(receiptMapper.selectList(any())).thenReturn(List.of());
        when(accountsReceivableMapper.selectList(any())).thenReturn(List.of());

        ErpCounterpartyUnbindCheck check = service.checkRebind(5L, 20L);

        assertThat(check.allowed()).isFalse();
        assertThat(check.blockingReasons()).contains("存在未完成销售单");
        assertThat(check.pendingDocs()).hasSize(1);
        assertThat(check.pendingDocs().get(0).orderNo()).isEqualTo("SO202605290001");
        assertThat(check.pendingDocs().get(0).routeKey()).isEqualTo("erp-sale-orders-approved-detail");
    }

    @Test
    void supplierRebindCheckReturnsStructuredBlockingDocs() {
        ErpSupplierServiceImpl service = supplierService();
        ErpSupplier supplier = supplier(6L);
        supplier.setCounterpartySubjectId(10L);
        ErpPurchaseOrder purchaseOrder = new ErpPurchaseOrder();
        purchaseOrder.setId(201L);
        purchaseOrder.setOrderNo("PO202605290001");
        purchaseOrder.setStatus("DRAFT");

        when(supplierMapper.selectOne(any())).thenReturn(supplier);
        when(purchaseOrderMapper.selectList(any())).thenReturn(List.of(purchaseOrder));
        when(purchaseReturnMapper.selectList(any())).thenReturn(List.of());
        when(paymentMapper.selectList(any())).thenReturn(List.of());
        when(accountsPayableMapper.selectList(any())).thenReturn(List.of());

        ErpCounterpartyUnbindCheck check = service.checkRebind(6L, 20L);

        assertThat(check.allowed()).isFalse();
        assertThat(check.blockingReasons()).contains("存在未完成采购单");
        assertThat(check.pendingDocs()).hasSize(1);
        assertThat(check.pendingDocs().get(0).orderNo()).isEqualTo("PO202605290001");
        assertThat(check.pendingDocs().get(0).routeKey()).isEqualTo("erp-purchase-draft-edit");
    }

    @Test
    void supplierTypeDeleteRejectsReferencedSupplier() {
        ErpSupplierTypeServiceImpl service = supplierTypeService();
        when(supplierTypeMapper.selectOne(any())).thenReturn(supplierType(7L));
        when(supplierMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(7L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("供应商类型已被供应商引用，不能删除");
    }

    @Test
    void supplierTypeDeleteRejectsBuiltinUncategorizedType() {
        ErpSupplierTypeServiceImpl service = supplierTypeService();
        ErpSupplierType builtInType = supplierType(8L);
        builtInType.setCode(ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_CODE);
        builtInType.setName(ErpCounterpartyGuardRules.UNCATEGORIZED_SUPPLIER_TYPE_NAME);
        when(supplierTypeMapper.selectOne(any())).thenReturn(builtInType);

        assertThatThrownBy(() -> service.delete(8L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("系统内置“未分类”供应商类型不允许删除");
    }

    @Test
    void settlementMethodDeleteRejectsReferencedCustomer() {
        ErpSettlementMethodServiceImpl service = settlementMethodService();
        ErpSettlementMethod method = new ErpSettlementMethod();
        method.setCode("CASH");
        when(settlementMethodMapper.selectOne(any())).thenReturn(method);
        when(customerMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(9L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("结算方式已被客户引用，不能删除");
    }

    @Test
    void paymentMethodDeleteRejectsReferencedPayment() {
        ErpPaymentMethodServiceImpl service = paymentMethodService();
        ErpPaymentMethod method = new ErpPaymentMethod();
        method.setCode("BANK");
        when(paymentMethodMapper.selectOne(any())).thenReturn(method);
        when(purchaseOrderMapper.selectCount(any())).thenReturn(0L);
        when(paymentMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(4L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("付款方式已被付款单引用，不能删除");
    }

    @Test
    void deliveryMethodDeleteRejectsReferencedCustomer() {
        ErpDeliveryMethodServiceImpl service = deliveryMethodService();
        var method = new com.example.wms.entity.erp.ErpDeliveryMethod();
        method.setCode("HOME");
        when(deliveryMethodMapper.selectOne(any())).thenReturn(method);
        when(customerMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(8L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("送货方式已被客户引用，不能删除");
    }

    @Test
    void printTemplateDeleteRejectsReferencedLog() {
        ErpPrintTemplateServiceImpl service = new ErpPrintTemplateServiceImpl(printTemplateMapper, printLogMapper, codeGenerator());
        ErpPrintTemplate template = new ErpPrintTemplate();
        template.setId(11L);
        when(printTemplateMapper.selectOne(any())).thenReturn(template);
        when(printLogMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(11L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("打印模板已被打印日志引用，不能删除");
    }

    @Test
    void vehicleBrandDeleteRejectsChildSeries() {
        ErpVehicleBrandServiceImpl service = new ErpVehicleBrandServiceImpl(vehicleBrandMapper, vehicleSeriesMapper, codeGenerator());
        when(vehicleBrandMapper.selectOne(any())).thenReturn(vehicleBrand(12L));
        when(vehicleSeriesMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(12L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("品牌下存在车系，不能删除");
    }

    @Test
    void vehicleSeriesDeleteRejectsChildModels() {
        ErpVehicleSeriesServiceImpl service = new ErpVehicleSeriesServiceImpl(vehicleSeriesMapper, vehicleBrandMapper, vehicleModelMapper, codeGenerator());
        when(vehicleSeriesMapper.selectOne(any())).thenReturn(vehicleSeries(13L, 12L));
        when(vehicleModelMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(13L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("车系下存在车型，不能删除");
    }

    @Test
    void vehicleModelDeleteRejectsProductFitmentReference() {
        ErpVehicleModelServiceImpl service = new ErpVehicleModelServiceImpl(vehicleModelMapper, vehicleSeriesMapper, productFitmentMapper, codeGenerator());
        when(vehicleModelMapper.selectOne(any())).thenReturn(vehicleModel(14L, 13L));
        when(productFitmentMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(14L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("车型已被商品适配关系引用，不能删除");
    }

    private ErpProductServiceImpl productService() {
        return productAsyncService(Runnable::run, null);
    }

    private ErpProductServiceImpl productAsyncService(Executor importExecutor,
                                                      org.springframework.transaction.support.TransactionOperations transactionOperations) {
        return new ErpProductServiceImpl(
            productMapper,
            productPriceMapper,
            orderSequenceMapper,
            systemConfigMapper,
            categoryMapper,
            unitMapper,
            warehouseMapper,
            locationMapper,
            customerCategoryMapper,
            supplierMapper,
            objectMapper,
            productImportBatchMapper,
            productImportItemMapper,
            new ExcelImportParser(),
            importExecutor,
            transactionOperations
        );
    }

    private ErpCustomerServiceImpl customerService() {
        return new ErpCustomerServiceImpl(
            customerMapper,
            customerCategoryMapper,
            settlementMethodMapper,
            receiptMethodMapper,
            deliveryMethodMapper,
            orderSequenceMapper,
            systemConfigMapper,
            saleOrderMapper,
            saleReturnMapper,
            receiptMapper,
            accountsReceivableMapper,
            counterpartySubjectMapper,
            counterpartySubjectLinkMapper,
            new ObjectMapper()
        );
    }

    private ErpCustomerServiceImpl customerAsyncService() {
        return customerAsyncService(Runnable::run, null);
    }

    private ErpCustomerServiceImpl customerAsyncService(Executor importExecutor,
                                                        org.springframework.transaction.support.TransactionOperations transactionOperations) {
        return new ErpCustomerServiceImpl(
            customerMapper,
            customerCategoryMapper,
            settlementMethodMapper,
            receiptMethodMapper,
            deliveryMethodMapper,
            orderSequenceMapper,
            systemConfigMapper,
            saleOrderMapper,
            saleReturnMapper,
            receiptMapper,
            accountsReceivableMapper,
            counterpartySubjectMapper,
            counterpartySubjectLinkMapper,
            new ObjectMapper(),
            customerImportBatchMapper,
            customerImportItemMapper,
            new ExcelImportParser(),
            importExecutor,
            transactionOperations
        );
    }

    private ErpSupplierServiceImpl supplierService() {
        return new ErpSupplierServiceImpl(
            supplierMapper,
            purchaseOrderMapper,
            purchaseReturnMapper,
            paymentMapper,
            accountsPayableMapper,
            settlementMethodMapper,
            paymentMethodMapper,
            orderSequenceMapper,
            systemConfigMapper,
            supplierTypeMapper,
            supplierImportBatchMapper,
            supplierImportItemMapper,
            counterpartySubjectLinkMapper,
            objectMapper
        );
    }

    private ErpSettlementMethodServiceImpl settlementMethodService() {
        return new ErpSettlementMethodServiceImpl(
            settlementMethodMapper,
            customerMapper,
            supplierMapper,
            saleOrderMapper,
            saleReturnMapper,
            purchaseReturnMapper,
            receiptMapper,
            paymentMapper,
            accountsReceivableMapper,
            accountsPayableMapper,
            codeGenerator()
        );
    }

    private ErpSupplierTypeServiceImpl supplierTypeService() {
        return new ErpSupplierTypeServiceImpl(supplierTypeMapper, supplierMapper);
    }

    private ErpPaymentMethodServiceImpl paymentMethodService() {
        return new ErpPaymentMethodServiceImpl(
            paymentMethodMapper,
            purchaseOrderMapper,
            paymentMapper,
            supplierMapper,
            purchaseReturnMapper,
            codeGenerator()
        );
    }

    private ErpDeliveryMethodServiceImpl deliveryMethodService() {
        return new ErpDeliveryMethodServiceImpl(deliveryMethodMapper, customerMapper, saleOrderMapper, codeGenerator());
    }

    private ErpMasterDataCodeGenerator codeGenerator() {
        return new ErpMasterDataCodeGenerator(orderSequenceMapper, systemConfigMapper);
    }

    private ErpCategory category(Long id, Long parentId) {
        ErpCategory category = new ErpCategory();
        category.setId(id);
        category.setParentId(parentId);
        category.setCode("CAT-" + id);
        category.setName("Category-" + id);
        return category;
    }

    private ErpUnit unit(Long id, String name) {
        ErpUnit unit = new ErpUnit();
        unit.setId(id);
        unit.setName(name);
        unit.setCode("UN-" + id);
        return unit;
    }

    private ErpProductImportBatch productImportBatch(Long id) {
        ErpProductImportBatch batch = new ErpProductImportBatch();
        batch.setId(id);
        batch.setTenantId(1L);
        batch.setTotalCount(1);
        batch.setSuccessCount(0);
        batch.setFailedCount(0);
        batch.setStatus("PROCESSING");
        return batch;
    }

    private ErpCustomer customer(Long id) {
        ErpCustomer customer = new ErpCustomer();
        customer.setId(id);
        customer.setCode("CU-" + id);
        customer.setName("Customer-" + id);
        return customer;
    }

    private com.example.wms.entity.erp.ErpCustomerCategory customerCategory(Long id, String name) {
        com.example.wms.entity.erp.ErpCustomerCategory category = new com.example.wms.entity.erp.ErpCustomerCategory();
        category.setId(id);
        category.setName(name);
        category.setCode("CC-" + id);
        return category;
    }

    private com.example.wms.entity.erp.ErpCustomerCategory customerCategory(Long id, String code, String name) {
        com.example.wms.entity.erp.ErpCustomerCategory category = new com.example.wms.entity.erp.ErpCustomerCategory();
        category.setId(id);
        category.setCode(code);
        category.setName(name);
        return category;
    }

    private ErpSettlementMethod settlementMethod(String code, String name) {
        ErpSettlementMethod method = new ErpSettlementMethod();
        method.setCode(code);
        method.setName(name);
        return method;
    }

    private ErpSupplier supplier(Long id) {
        ErpSupplier supplier = new ErpSupplier();
        supplier.setId(id);
        supplier.setCode("SU-" + id);
        supplier.setName("Supplier-" + id);
        return supplier;
    }

    private ErpSupplierType supplierType(Long id) {
        ErpSupplierType supplierType = new ErpSupplierType();
        supplierType.setId(id);
        supplierType.setCode("ST-" + id);
        supplierType.setName("SupplierType-" + id);
        return supplierType;
    }

    private ErpVehicleBrand vehicleBrand(Long id) {
        ErpVehicleBrand brand = new ErpVehicleBrand();
        brand.setId(id);
        brand.setCode("VB-" + id);
        brand.setName("Brand-" + id);
        return brand;
    }

    private ErpVehicleSeries vehicleSeries(Long id, Long brandId) {
        ErpVehicleSeries series = new ErpVehicleSeries();
        series.setId(id);
        series.setBrandId(brandId);
        series.setCode("VS-" + id);
        series.setName("Series-" + id);
        return series;
    }

    private ErpVehicleModel vehicleModel(Long id, Long seriesId) {
        ErpVehicleModel model = new ErpVehicleModel();
        model.setId(id);
        model.setSeriesId(seriesId);
        model.setCode("VM-" + id);
        model.setName("Model-" + id);
        return model;
    }

    private byte[] customerWorkbookBytes() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("编码");
            header.createCell(1).setCellValue("名称");
            header.createCell(2).setCellValue("默认结算方式");
            header.createCell(3).setCellValue("客户类型");

            Row row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue("CU001");
            row.createCell(1).setCellValue("客户A");
            row.createCell(2).setCellValue("未知结算");
            row.createCell(3).setCellValue("零售客户");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] customerWorkbookWithLongContactBytes() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("编码");
            header.createCell(1).setCellValue("名称");
            header.createCell(2).setCellValue("默认结算方式");
            header.createCell(3).setCellValue("客户类型");
            header.createCell(4).setCellValue("联系方式（电话，手机）");

            Row row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue("CU002");
            row.createCell(1).setCellValue("客户B");
            row.createCell(2).setCellValue("现结");
            row.createCell(3).setCellValue("零售客户");
            row.createCell(4).setCellValue("总机021-66554433 分机8899，老板手机13800138000，售后微信同号");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] customerWorkbookWithLegacyContactBytes(String code, String name, String contactInfo) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("编码");
            header.createCell(1).setCellValue("名称");
            header.createCell(2).setCellValue("默认结算方式");
            header.createCell(3).setCellValue("客户类型");
            header.createCell(4).setCellValue("联系方式（电话，手机）");

            Row row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue(code);
            row.createCell(1).setCellValue(name);
            row.createCell(2).setCellValue("现结");
            row.createCell(3).setCellValue("零售客户");
            row.createCell(4).setCellValue(contactInfo);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] customerWorkbookRows(int rowCount) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("编码");
            header.createCell(1).setCellValue("名称");

            for (int index = 0; index < rowCount; index++) {
                Row row = workbook.getSheetAt(0).createRow(index + 1);
                row.createCell(0).setCellValue("CU" + String.format("%03d", index + 1));
                row.createCell(1).setCellValue("客户" + (index + 1));
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] productWorkbookBytes() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("编码");
            header.createCell(1).setCellValue("配件名称");
            header.createCell(2).setCellValue("类别");
            header.createCell(3).setCellValue("单位");

            Row row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue("PR001");
            row.createCell(1).setCellValue("机滤");
            row.createCell(2).setCellValue("未知类别");
            row.createCell(3).setCellValue("个");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] productWorkbookWithPricesBytes() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("编码");
            header.createCell(1).setCellValue("配件名称");
            header.createCell(2).setCellValue("类别");
            header.createCell(3).setCellValue("单位");
            header.createCell(4).setCellValue("参考价");
            header.createCell(5).setCellValue("备用价1");
            header.createCell(6).setCellValue("零售价");
            header.createCell(7).setCellValue("批发价");

            Row row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellValue("PR002");
            row.createCell(1).setCellValue("空滤");
            row.createCell(2).setCellValue("滤清器");
            row.createCell(3).setCellValue("个");
            row.createCell(4).setCellValue(1650);
            row.createCell(5).setCellValue(0);
            row.createCell(6).setCellValue(88);
            row.createCell(7).setCellValue(0);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
