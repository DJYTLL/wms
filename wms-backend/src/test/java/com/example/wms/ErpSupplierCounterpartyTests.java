package com.example.wms;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.wms.dto.erp.ErpCounterpartySubjectCreateRequest;
import com.example.wms.dto.erp.ErpSupplierCreateRequest;
import com.example.wms.entity.erp.ErpCounterpartySubject;
import com.example.wms.entity.erp.ErpCounterpartySubjectLink;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpCounterpartySubjectLinkMapper;
import com.example.wms.mapper.erp.ErpCounterpartySubjectMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPaymentMethodMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.mapper.erp.ErpSupplierImportBatchMapper;
import com.example.wms.mapper.erp.ErpSupplierImportItemMapper;
import com.example.wms.mapper.erp.ErpSupplierTypeMapper;
import com.example.wms.service.erp.impl.ErpCounterpartySubjectServiceImpl;
import com.example.wms.service.erp.impl.ErpSupplierServiceImpl;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpSupplierCounterpartyTests {
    @Mock private ErpSupplierMapper supplierMapper;
    @Mock private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Mock private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock private ErpPaymentMapper paymentMapper;
    @Mock private ErpAccountsPayableMapper accountsPayableMapper;
    @Mock private ErpSettlementMethodMapper settlementMethodMapper;
    @Mock private ErpPaymentMethodMapper paymentMethodMapper;
    @Mock private ErpOrderSequenceMapper orderSequenceMapper;
    @Mock private SystemConfigMapper systemConfigMapper;
    @Mock private ErpSupplierTypeMapper supplierTypeMapper;
    @Mock private ErpSupplierImportBatchMapper supplierImportBatchMapper;
    @Mock private ErpSupplierImportItemMapper supplierImportItemMapper;
    @Mock private ErpCounterpartySubjectMapper counterpartySubjectMapper;
    @Mock private ErpCounterpartySubjectLinkMapper counterpartySubjectLinkMapper;
    @Mock private ErpCustomerMapper customerMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createPersistsExtendedFields() {
        ErpSupplierServiceImpl service = supplierService();
        when(supplierMapper.findByCode(1L, "SU-202605280001")).thenReturn(null);

        ErpSupplierCreateRequest request = new ErpSupplierCreateRequest(
            "SU-202605280001",
            "昆明坤润汽车维修服务有限公司",
            "坤润汽修",
            12L,
            "西山环卫",
            "0871-1234567",
            "13800001111",
            "supplier@example.com",
            "昆明市西山区",
            "昆明",
            "kr-service",
            "张采购",
            "1725888889/1725888889",
            "TAX-001",
            "建设银行昆明支行",
            "6222000000001",
            "MONTHLY",
            "BANK",
            "[{\"name\":\"西山环卫\",\"phone\":\"1725888889\"}]",
            true,
            false,
            "2026-04-12 08:30:00",
            "管理员",
            "CUSTOMER_SUPPLIER",
            88L,
            "历史导入"
        );

        ErpSupplier created = service.create(request);

        ArgumentCaptor<ErpSupplier> captor = ArgumentCaptor.forClass(ErpSupplier.class);
        verify(supplierMapper).insert(captor.capture());
        ErpSupplier inserted = captor.getValue();

        assertThat(created).isSameAs(inserted);
        assertThat(inserted.getSupplierTypeId()).isEqualTo(12L);
        assertThat(inserted.getRegion()).isEqualTo("昆明");
        assertThat(inserted.getWechat()).isEqualTo("kr-service");
        assertThat(inserted.getPurchaser()).isEqualTo("张采购");
        assertThat(inserted.getContactInfo()).isEqualTo("1725888889/1725888889");
        assertThat(inserted.getSourceCreatedAt()).isEqualTo(
            LocalDateTime.of(2026, 4, 12, 8, 30, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        );
        assertThat(inserted.getSourceCreatedBy()).isEqualTo("管理员");
        assertThat(inserted.getBusinessScope()).isEqualTo("CUSTOMER_SUPPLIER");
        assertThat(inserted.getCounterpartySubjectId()).isEqualTo(88L);
        assertThat(inserted.getContacts()).isNotNull();
    }

    @Test
    void createRejectsInvalidSourceCreatedAt() {
        ErpSupplierServiceImpl service = supplierService();
        when(supplierMapper.findByCode(1L, "SU-202605280002")).thenReturn(null);

        ErpSupplierCreateRequest request = new ErpSupplierCreateRequest(
            "SU-202605280002",
            "测试供应商",
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
            false,
            "2026/04/12",
            null,
            null,
            null,
            null
        );

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("来源创建时间格式不正确");
    }

    @Test
    void createSyncsPrimaryContactFieldsFromContactsJson() {
        ErpSupplierServiceImpl service = supplierService();
        when(supplierMapper.findByCode(1L, "SU-202605280003")).thenReturn(null);

        ErpSupplierCreateRequest request = new ErpSupplierCreateRequest(
            "SU-202605280003",
            "多联系人供应商",
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
            """
            [
              {"name":"财务联系人","phone":"0871-6666666","mobile":"13900001111","wechat":"finance-wechat","email":"finance@example.com","remark":"财务","isPrimary":false},
              {"name":"采购联系人","phone":"0871-8888888","mobile":"13800002222","wechat":"buyer-wechat","email":"buyer@example.com","remark":"采购","isPrimary":true}
            ]
            """,
            true,
            false,
            null,
            null,
            null,
            null,
            null
        );

        service.create(request);

        ArgumentCaptor<ErpSupplier> captor = ArgumentCaptor.forClass(ErpSupplier.class);
        verify(supplierMapper).insert(captor.capture());
        ErpSupplier inserted = captor.getValue();

        assertThat(inserted.getContact()).isEqualTo("采购联系人");
        assertThat(inserted.getPhone()).isEqualTo("0871-8888888");
        assertThat(inserted.getMobile()).isEqualTo("13800002222");
        assertThat(inserted.getWechat()).isEqualTo("buyer-wechat");
        assertThat(inserted.getEmail()).isEqualTo("buyer@example.com");
        assertThat(inserted.getContactInfo()).contains("采购联系人");
        assertThat(inserted.getContactInfo()).contains("13800002222");
        assertThat(inserted.getContacts()).isNotNull();
    }

    @Test
    void createSubjectPersistsFields() {
        ErpCounterpartySubjectServiceImpl service = counterpartySubjectService();

        ErpCounterpartySubjectCreateRequest request = new ErpCounterpartySubjectCreateRequest(
            "昆明坤润汽车维修服务有限公司",
            "昆明",
            "91530100TEST001",
            true,
            "历史导入主体"
        );

        ErpCounterpartySubject created = service.create(request);

        ArgumentCaptor<ErpCounterpartySubject> captor = ArgumentCaptor.forClass(ErpCounterpartySubject.class);
        verify(counterpartySubjectMapper).insert(captor.capture());
        ErpCounterpartySubject inserted = captor.getValue();

        assertThat(created).isSameAs(inserted);
        assertThat(inserted.getTenantId()).isEqualTo(1L);
        assertThat(inserted.getName()).isEqualTo("昆明坤润汽车维修服务有限公司");
        assertThat(inserted.getRegion()).isEqualTo("昆明");
        assertThat(inserted.getUnifiedCreditCode()).isEqualTo("91530100TEST001");
        assertThat(inserted.getEnabled()).isTrue();
        assertThat(inserted.getRemark()).isEqualTo("历史导入主体");
        assertThat(inserted.getCreatedAt()).isNotNull();
        assertThat(inserted.getUpdatedAt()).isNotNull();
    }

    @Test
    void deleteSubjectRejectsWhenLinksExist() {
        ErpCounterpartySubjectServiceImpl service = counterpartySubjectService();
        ErpCounterpartySubject subject = new ErpCounterpartySubject();
        subject.setId(9L);
        subject.setTenantId(1L);
        subject.setName("坤润主体");

        when(counterpartySubjectMapper.selectOne(any())).thenReturn(subject);
        when(counterpartySubjectLinkMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(9L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("往来主体已存在关联记录，不能删除");
    }

    @Test
    void bindSupplierCreatesLinkAndUpdatesSupplierSubjectId() {
        ErpCounterpartySubjectServiceImpl service = counterpartySubjectService();
        ErpCounterpartySubject subject = new ErpCounterpartySubject();
        subject.setId(20L);
        subject.setTenantId(1L);
        subject.setName("坤润主体");
        ErpSupplier supplier = new ErpSupplier();
        supplier.setId(88L);
        supplier.setTenantId(1L);
        supplier.setName("坤润供应商");

        when(counterpartySubjectMapper.selectOne(any())).thenReturn(subject);
        when(supplierMapper.selectOne(any())).thenReturn(supplier);
        when(counterpartySubjectLinkMapper.selectOne(any())).thenReturn(null);

        ErpCounterpartySubjectLink link = service.bindSupplier(20L, 88L, true, "主供应商");

        ArgumentCaptor<ErpCounterpartySubjectLink> linkCaptor = ArgumentCaptor.forClass(ErpCounterpartySubjectLink.class);
        verify(counterpartySubjectLinkMapper).insert(linkCaptor.capture());
        ErpCounterpartySubjectLink insertedLink = linkCaptor.getValue();

        assertThat(link).isSameAs(insertedLink);
        assertThat(insertedLink.getSubjectId()).isEqualTo(20L);
        assertThat(insertedLink.getTargetType()).isEqualTo("SUPPLIER");
        assertThat(insertedLink.getTargetId()).isEqualTo(88L);
        assertThat(insertedLink.getRoleType()).isEqualTo("SUPPLIER");
        assertThat(insertedLink.getPrimary()).isTrue();
        assertThat(insertedLink.getRemark()).isEqualTo("主供应商");

        verify(supplierMapper).updateById(argThat((ErpSupplier updated) ->
            updated.getId().equals(88L) && updated.getCounterpartySubjectId().equals(20L)
        ));
    }

    @Test
    void bindSupplierRejectsWhenAlreadyBoundToAnotherSubject() {
        ErpCounterpartySubjectServiceImpl service = counterpartySubjectService();
        ErpCounterpartySubject subject = new ErpCounterpartySubject();
        subject.setId(20L);
        subject.setTenantId(1L);

        ErpSupplier supplier = new ErpSupplier();
        supplier.setId(88L);
        supplier.setTenantId(1L);

        ErpCounterpartySubjectLink existing = new ErpCounterpartySubjectLink();
        existing.setId(100L);
        existing.setSubjectId(21L);
        existing.setTargetId(88L);
        existing.setTargetType("SUPPLIER");
        existing.setRoleType("SUPPLIER");

        when(counterpartySubjectMapper.selectOne(any())).thenReturn(subject);
        when(supplierMapper.selectOne(any())).thenReturn(supplier);
        when(counterpartySubjectLinkMapper.selectOne(any())).thenReturn(existing);

        assertThatThrownBy(() -> service.bindSupplier(20L, 88L, false, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("供应商已绑定其他往来主体");
    }

    @Test
    void bindCustomerCreatesLink() {
        ErpCounterpartySubjectServiceImpl service = counterpartySubjectService();
        ErpCounterpartySubject subject = new ErpCounterpartySubject();
        subject.setId(30L);
        subject.setTenantId(1L);

        ErpCustomer customer = new ErpCustomer();
        customer.setId(66L);
        customer.setTenantId(1L);
        customer.setName("坤润客户");

        when(counterpartySubjectMapper.selectOne(any())).thenReturn(subject);
        when(customerMapper.selectOne(any())).thenReturn(customer);
        when(counterpartySubjectLinkMapper.selectOne(any())).thenReturn(null);

        ErpCounterpartySubjectLink link = service.bindCustomer(30L, 66L, null, "客户身份");

        assertThat(link.getSubjectId()).isEqualTo(30L);
        assertThat(link.getTargetType()).isEqualTo("CUSTOMER");
        assertThat(link.getTargetId()).isEqualTo(66L);
        assertThat(link.getRoleType()).isEqualTo("CUSTOMER");
        assertThat(link.getPrimary()).isFalse();
        verify(counterpartySubjectLinkMapper).insert(any(ErpCounterpartySubjectLink.class));
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

    private ErpCounterpartySubjectServiceImpl counterpartySubjectService() {
        return new ErpCounterpartySubjectServiceImpl(
            counterpartySubjectMapper,
            counterpartySubjectLinkMapper,
            supplierMapper,
            customerMapper,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}
