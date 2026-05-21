package com.example.wms;

import com.example.wms.entity.SystemConfig;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpDeliveryMethodMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPaymentMethodMapper;
import com.example.wms.mapper.erp.ErpPrintLogMapper;
import com.example.wms.mapper.erp.ErpPrintTemplateMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.mapper.erp.ErpUnitMapper;
import com.example.wms.service.erp.impl.ErpDeliveryMethodServiceImpl;
import com.example.wms.service.erp.impl.ErpPaymentMethodServiceImpl;
import com.example.wms.service.erp.impl.ErpPrintTemplateServiceImpl;
import com.example.wms.service.erp.impl.ErpSettlementMethodServiceImpl;
import com.example.wms.service.erp.impl.ErpUnitServiceImpl;
import com.example.wms.service.erp.support.ErpMasterDataCodeGenerator;
import com.example.wms.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpMasterDataNextCodeTests {
    @Mock private ErpOrderSequenceMapper orderSequenceMapper;
    @Mock private SystemConfigMapper systemConfigMapper;
    @Mock private ErpUnitMapper unitMapper;
    @Mock private ErpProductMapper productMapper;
    @Mock private ErpSettlementMethodMapper settlementMethodMapper;
    @Mock private ErpCustomerMapper customerMapper;
    @Mock private ErpSupplierMapper supplierMapper;
    @Mock private ErpSaleOrderMapper saleOrderMapper;
    @Mock private ErpSaleReturnMapper saleReturnMapper;
    @Mock private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock private ErpReceiptMapper receiptMapper;
    @Mock private ErpPaymentMapper paymentMapper;
    @Mock private ErpAccountsReceivableMapper accountsReceivableMapper;
    @Mock private ErpAccountsPayableMapper accountsPayableMapper;
    @Mock private ErpPaymentMethodMapper paymentMethodMapper;
    @Mock private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Mock private ErpDeliveryMethodMapper deliveryMethodMapper;
    @Mock private ErpPrintTemplateMapper printTemplateMapper;
    @Mock private ErpPrintLogMapper printLogMapper;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void unitNextCodeUsesConfiguredPrefix() {
        mockSequence("erp.unit.code.prefix", "UNT");
        ErpUnitServiceImpl service = new ErpUnitServiceImpl(unitMapper, productMapper, codeGenerator());

        assertThat(service.nextCode()).isEqualTo("UNT" + today() + "0007");
    }

    @Test
    void settlementMethodNextCodeUsesConfiguredPrefix() {
        mockSequence("erp.settlement-method.code.prefix", "SET");
        ErpSettlementMethodServiceImpl service = new ErpSettlementMethodServiceImpl(
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

        assertThat(service.nextCode()).isEqualTo("SET" + today() + "0007");
    }

    @Test
    void paymentMethodNextCodeUsesConfiguredPrefix() {
        mockSequence("erp.payment-method.code.prefix", "PAY");
        ErpPaymentMethodServiceImpl service = new ErpPaymentMethodServiceImpl(
            paymentMethodMapper,
            purchaseOrderMapper,
            paymentMapper,
            supplierMapper,
            purchaseReturnMapper,
            codeGenerator()
        );

        assertThat(service.nextCode()).isEqualTo("PAY" + today() + "0007");
    }

    @Test
    void deliveryMethodNextCodeUsesConfiguredPrefix() {
        mockSequence("erp.delivery-method.code.prefix", "DEL");
        ErpDeliveryMethodServiceImpl service = new ErpDeliveryMethodServiceImpl(
            deliveryMethodMapper,
            customerMapper,
            saleOrderMapper,
            codeGenerator()
        );

        assertThat(service.nextCode()).isEqualTo("DEL" + today() + "0007");
    }

    @Test
    void printTemplateNextCodeUsesConfiguredPrefix() {
        mockSequence("erp.print-template.code.prefix", "TPL");
        ErpPrintTemplateServiceImpl service = new ErpPrintTemplateServiceImpl(
            printTemplateMapper,
            printLogMapper,
            codeGenerator()
        );

        assertThat(service.nextCode()).isEqualTo("TPL" + today() + "0007");
    }

    private ErpMasterDataCodeGenerator codeGenerator() {
        return new ErpMasterDataCodeGenerator(orderSequenceMapper, systemConfigMapper);
    }

    private void mockSequence(String prefixKey, String prefixValue) {
        when(systemConfigMapper.findByKey(eq(1L), anyString())).thenReturn(null);
        when(systemConfigMapper.findByKey(1L, prefixKey)).thenReturn(systemConfig(prefixValue));
        when(orderSequenceMapper.incrementAndGet(eq(1L), anyString(), eq(today()))).thenReturn(7L);
    }

    private SystemConfig systemConfig(String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigValue(value);
        return config;
    }

    private String today() {
        return LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
