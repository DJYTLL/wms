package com.example.wms;

import com.example.wms.dto.PageResponse;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpCounterpartySubjectLinkMapper;
import com.example.wms.mapper.erp.ErpCounterpartySubjectMapper;
import com.example.wms.mapper.erp.ErpCustomerCategoryMapper;
import com.example.wms.mapper.erp.ErpCustomerImportBatchMapper;
import com.example.wms.mapper.erp.ErpCustomerImportItemMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpDeliveryMethodMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpReceiptMethodMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.service.erp.impl.ErpCustomerServiceImpl;
import com.example.wms.service.erp.support.ExcelImportParser;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpCustomerSearchRankingTests {
    @Mock private ErpCustomerMapper customerMapper;
    @Mock private ErpCustomerCategoryMapper customerCategoryMapper;
    @Mock private ErpSettlementMethodMapper settlementMethodMapper;
    @Mock private ErpReceiptMethodMapper receiptMethodMapper;
    @Mock private ErpDeliveryMethodMapper deliveryMethodMapper;
    @Mock private ErpOrderSequenceMapper orderSequenceMapper;
    @Mock private SystemConfigMapper systemConfigMapper;
    @Mock private ErpSaleOrderMapper saleOrderMapper;
    @Mock private ErpSaleReturnMapper saleReturnMapper;
    @Mock private ErpReceiptMapper receiptMapper;
    @Mock private ErpAccountsReceivableMapper accountsReceivableMapper;
    @Mock private ErpCounterpartySubjectMapper counterpartySubjectMapper;
    @Mock private ErpCounterpartySubjectLinkMapper counterpartySubjectLinkMapper;
    @Mock private ErpCustomerImportBatchMapper customerImportBatchMapper;
    @Mock private ErpCustomerImportItemMapper customerImportItemMapper;

    @BeforeEach
    void setTenant() {
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void customerPageSearchMatchesChineseFullPinyinInitialsAndRanksBestHitsFirst() {
        when(customerMapper.selectList(any())).thenReturn(List.of(
            customer(1L, "CU-001", "重汽修理厂", "重汽", "张三", "13800000001"),
            customer(2L, "CU-002", "重汽配件门店", "配件门店", "李四", "13800000002"),
            customer(3L, "CU-003", "华东轮胎客户", "轮胎", "王五", "13800000003"),
            customer(4L, "LS-004", "普通客户", "普通", "赵六", "13800000004")
        ));

        ErpCustomerServiceImpl service = customerService();

        List<ErpCustomer> chineseResult = service.searchOptions("重汽", 20);
        assertThat(chineseResult).extracting(ErpCustomer::getName)
            .containsExactly("重汽修理厂", "重汽配件门店");

        List<ErpCustomer> pinyinResult = service.searchOptions("zhongqi", 20);
        assertThat(pinyinResult).extracting(ErpCustomer::getName)
            .containsExactly("重汽修理厂", "重汽配件门店");

        List<ErpCustomer> initialsResult = service.searchOptions("zq", 20);
        assertThat(initialsResult).extracting(ErpCustomer::getName)
            .containsExactly("重汽修理厂", "重汽配件门店");

        List<ErpCustomer> contactResult = service.searchOptions("张三", 20);
        assertThat(contactResult).extracting(ErpCustomer::getName)
            .containsExactly("重汽修理厂");

        List<ErpCustomer> mobileResult = service.searchOptions("13800000002", 20);
        assertThat(mobileResult).extracting(ErpCustomer::getName)
            .containsExactly("重汽配件门店");

        List<ErpCustomer> codeResult = service.searchOptions("LS-004", 20);
        assertThat(codeResult).isEmpty();
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
            new ObjectMapper(),
            customerImportBatchMapper,
            customerImportItemMapper,
            new ExcelImportParser()
        );
    }

    private ErpCustomer customer(Long id, String code, String name, String shortName, String contact, String mobile) {
        ErpCustomer customer = new ErpCustomer();
        customer.setId(id);
        customer.setTenantId(1L);
        customer.setCode(code);
        customer.setName(name);
        customer.setShortName(shortName);
        customer.setContact(contact);
        customer.setMobile(mobile);
        customer.setEnabled(true);
        return customer;
    }
}
