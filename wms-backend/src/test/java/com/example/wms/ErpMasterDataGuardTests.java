package com.example.wms;

import com.example.wms.dto.erp.ErpCategoryUpdateRequest;
import com.example.wms.dto.erp.ErpCustomerCreateRequest;
import com.example.wms.dto.erp.ErpDeliveryMethodCreateRequest;
import com.example.wms.dto.erp.ErpPaymentMethodCreateRequest;
import com.example.wms.dto.erp.ErpPrintTemplateCreateRequest;
import com.example.wms.dto.erp.ErpProductCreateRequest;
import com.example.wms.dto.erp.ErpProductPriceItemRequest;
import com.example.wms.dto.erp.ErpProductUpdateRequest;
import com.example.wms.dto.erp.ErpSettlementMethodCreateRequest;
import com.example.wms.dto.erp.ErpSupplierCreateRequest;
import com.example.wms.dto.erp.ErpVehicleBrandCreateRequest;
import com.example.wms.dto.erp.ErpVehicleModelCreateRequest;
import com.example.wms.dto.erp.ErpVehicleSeriesCreateRequest;
import com.example.wms.entity.erp.ErpCategory;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpPaymentMethod;
import com.example.wms.entity.erp.ErpPrintTemplate;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpSettlementMethod;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.entity.erp.ErpVehicleBrand;
import com.example.wms.entity.erp.ErpVehicleModel;
import com.example.wms.entity.erp.ErpVehicleSeries;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpCategoryMapper;
import com.example.wms.mapper.erp.ErpCustomerCategoryMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpDeliveryMethodMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPaymentMethodMapper;
import com.example.wms.mapper.erp.ErpPrintLogMapper;
import com.example.wms.mapper.erp.ErpPrintTemplateMapper;
import com.example.wms.mapper.erp.ErpProductFitmentMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpProductPriceMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
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
import com.example.wms.service.erp.impl.ErpUnitServiceImpl;
import com.example.wms.service.erp.impl.ErpVehicleBrandServiceImpl;
import com.example.wms.service.erp.impl.ErpVehicleModelServiceImpl;
import com.example.wms.service.erp.impl.ErpVehicleSeriesServiceImpl;
import com.example.wms.service.erp.support.ErpMasterDataCodeGenerator;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Mock private ErpCustomerMapper customerMapper;
    @Mock private ErpSupplierMapper supplierMapper;
    @Mock private ErpSaleOrderMapper saleOrderMapper;
    @Mock private ErpSaleReturnMapper saleReturnMapper;
    @Mock private ErpReceiptMapper receiptMapper;
    @Mock private ErpAccountsReceivableMapper accountsReceivableMapper;
    @Mock private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Mock private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock private ErpPaymentMapper paymentMapper;
    @Mock private ErpAccountsPayableMapper accountsPayableMapper;
    @Mock private ErpSettlementMethodMapper settlementMethodMapper;
    @Mock private ErpDeliveryMethodMapper deliveryMethodMapper;
    @Mock private ErpPaymentMethodMapper paymentMethodMapper;
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
            "P-007", "product", null, null, null, null, null,
            10L, 20L, null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, true, null, null, null
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
            "P-008", "product", null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, true, null, null,
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
    void categoryUpdateRejectsCycleParent() {
        ErpCategoryServiceImpl service = new ErpCategoryServiceImpl(categoryMapper, productMapper, orderSequenceMapper, systemConfigMapper);
        ErpCategory self = category(10L, 20L);
        ErpCategory parent = category(20L, 10L);

        when(categoryMapper.selectOne(any())).thenReturn(self, parent, self);
        when(categoryMapper.findByCode(1L, "CAT-10")).thenReturn(self);

        ErpCategoryUpdateRequest request = new ErpCategoryUpdateRequest("CAT-10", "分类", 20L, 1, 0, true, null);

        assertThatThrownBy(() -> service.update(10L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("父级分类不能形成循环关系");
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
            objectMapper
        );
    }

    private ErpCustomerServiceImpl customerService() {
        return new ErpCustomerServiceImpl(
            customerMapper,
            customerCategoryMapper,
            settlementMethodMapper,
            deliveryMethodMapper,
            orderSequenceMapper,
            systemConfigMapper,
            saleOrderMapper,
            saleReturnMapper,
            receiptMapper,
            accountsReceivableMapper,
            objectMapper
        );
    }

    private ErpSupplierServiceImpl supplierService() {
        return new ErpSupplierServiceImpl(
            supplierMapper,
            purchaseOrderMapper,
            purchaseReturnMapper,
            paymentMapper,
            accountsPayableMapper,
            orderSequenceMapper,
            systemConfigMapper,
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

    private ErpPaymentMethodServiceImpl paymentMethodService() {
        return new ErpPaymentMethodServiceImpl(paymentMethodMapper, purchaseOrderMapper, paymentMapper, codeGenerator());
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

    private ErpCustomer customer(Long id) {
        ErpCustomer customer = new ErpCustomer();
        customer.setId(id);
        customer.setCode("CU-" + id);
        customer.setName("Customer-" + id);
        return customer;
    }

    private ErpSupplier supplier(Long id) {
        ErpSupplier supplier = new ErpSupplier();
        supplier.setId(id);
        supplier.setCode("SU-" + id);
        supplier.setName("Supplier-" + id);
        return supplier;
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
}
