package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCustomerCreateRequest;
import com.example.wms.dto.erp.ErpCustomerUpdateRequest;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.mapper.erp.ErpCustomerCategoryMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpDeliveryMethodMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.service.erp.ErpCustomerService;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

// 客户服务实现（ERP进销存）
@Service
public class ErpCustomerServiceImpl implements ErpCustomerService {
    private static final String CUSTOMER_CODE_TYPE = "CUSTOMER";

    private final ErpCustomerMapper erpCustomerMapper;
    private final ErpCustomerCategoryMapper erpCustomerCategoryMapper;
    private final ErpSettlementMethodMapper erpSettlementMethodMapper;
    private final ErpDeliveryMethodMapper erpDeliveryMethodMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;

    public ErpCustomerServiceImpl(ErpCustomerMapper erpCustomerMapper,
                                  ErpCustomerCategoryMapper erpCustomerCategoryMapper,
                                  ErpSettlementMethodMapper erpSettlementMethodMapper,
                                  ErpDeliveryMethodMapper erpDeliveryMethodMapper,
                                  ErpOrderSequenceMapper erpOrderSequenceMapper,
                                  SystemConfigMapper systemConfigMapper,
                                  ObjectMapper objectMapper) {
        this.erpCustomerMapper = erpCustomerMapper;
        this.erpCustomerCategoryMapper = erpCustomerCategoryMapper;
        this.erpSettlementMethodMapper = erpSettlementMethodMapper;
        this.erpDeliveryMethodMapper = erpDeliveryMethodMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ErpCustomer> listAll(String keyword, Boolean enabled, Long categoryId) {
        QueryWrapper<ErpCustomer> wrapper = baseWrapper(keyword, enabled, categoryId);
        wrapper.orderByAsc("id");
        return erpCustomerMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpCustomer> page(long page, long size, String keyword, Boolean enabled, Long categoryId) {
        Page<ErpCustomer> pageReq = Page.of(page, size);
        QueryWrapper<ErpCustomer> wrapper = baseWrapper(keyword, enabled, categoryId);
        wrapper.orderByAsc("id");
        Page<ErpCustomer> result = erpCustomerMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpCustomer getById(Long id) {
        ErpCustomer customer = erpCustomerMapper.selectOne(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        return customer;
    }

    @Override
    public String nextCode() {
        Long tenantId = TenantContext.requireTenantId();
        return generateCustomerCode(tenantId);
    }

    @Override
    @AuditLog(action = "ERP_CUSTOMER_CREATE", entityType = "erp_customer", entityId = "{result.id}", detail = "code={arg0.code}")
    public ErpCustomer create(ErpCustomerCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Long categoryId = request.categoryId();
        if (categoryId == null) {
            var defaultCategory = erpCustomerCategoryMapper.findDefault(tenantId);
            if (defaultCategory != null) {
                categoryId = defaultCategory.getId();
            } else {
                throw new IllegalArgumentException("请选择客户类别");
            }
        }
        ErpCustomer existing = erpCustomerMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("客户编码已存在");
        }
        ErpCustomer customer = new ErpCustomer();
        customer.setTenantId(tenantId);
        applyRequest(customer, request, categoryId);
        applyDefaultMethodsIfMissing(customer, tenantId);
        customer.setEnabled(request.enabled() == null || request.enabled());
        customer.setCreatedAt(Instant.now());
        customer.setUpdatedAt(Instant.now());
        erpCustomerMapper.insert(customer);
        return customer;
    }

    @Override
    @AuditLog(action = "ERP_CUSTOMER_UPDATE", entityType = "erp_customer", entityId = "{arg0}", detail = "code={arg1.code}")
    public ErpCustomer update(Long id, ErpCustomerUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        if (request.categoryId() == null) {
            throw new IllegalArgumentException("请选择客户类别");
        }
        ErpCustomer customer = erpCustomerMapper.selectOne(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        ErpCustomer existing = erpCustomerMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("客户编码已存在");
        }
        applyRequest(customer, request, request.categoryId());
        if (request.enabled() != null) {
            customer.setEnabled(request.enabled());
        }
        customer.setUpdatedAt(Instant.now());
        erpCustomerMapper.updateById(customer);
        return customer;
    }

    @Override
    @AuditLog(action = "ERP_CUSTOMER_DELETE", entityType = "erp_customer", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpCustomer customer = erpCustomerMapper.selectOne(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        erpCustomerMapper.deleteById(id);
    }

    private QueryWrapper<ErpCustomer> baseWrapper(String keyword, Boolean enabled, Long categoryId) {
        QueryWrapper<ErpCustomer> wrapper = new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword)
                .or()
                .like("short_name", keyword)
                .or()
                .like("contact", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        return wrapper;
    }

    private void applyRequest(ErpCustomer customer, ErpCustomerCreateRequest request, Long categoryId) {
        customer.setCode(request.code());
        customer.setName(request.name());
        customer.setCategoryId(categoryId);
        customer.setShortName(request.shortName());
        customer.setContact(request.contact());
        customer.setPhone(request.phone());
        customer.setMobile(request.mobile());
        customer.setEmail(request.email());
        customer.setAddress(request.address());
        customer.setTaxNo(request.taxNo());
        customer.setBankName(request.bankName());
        customer.setBankAccount(request.bankAccount());
        customer.setInvoiceTitle(request.invoiceTitle());
        customer.setPaymentTerms(request.paymentTerms());
        customer.setDeliveryMethodCode(request.deliveryMethodCode());
        customer.setCreditLimit(request.creditLimit());
        customer.setContacts(parseContacts(request.contacts()));
        customer.setRemark(request.remark());
    }

    private void applyRequest(ErpCustomer customer, ErpCustomerUpdateRequest request, Long categoryId) {
        customer.setCode(request.code());
        customer.setName(request.name());
        customer.setCategoryId(categoryId);
        customer.setShortName(request.shortName());
        customer.setContact(request.contact());
        customer.setPhone(request.phone());
        customer.setMobile(request.mobile());
        customer.setEmail(request.email());
        customer.setAddress(request.address());
        customer.setTaxNo(request.taxNo());
        customer.setBankName(request.bankName());
        customer.setBankAccount(request.bankAccount());
        customer.setInvoiceTitle(request.invoiceTitle());
        customer.setPaymentTerms(request.paymentTerms());
        customer.setDeliveryMethodCode(request.deliveryMethodCode());
        customer.setCreditLimit(request.creditLimit());
        customer.setContacts(parseContacts(request.contacts()));
        customer.setRemark(request.remark());
    }

    private void applyDefaultMethodsIfMissing(ErpCustomer customer, Long tenantId) {
        if (customer.getPaymentTerms() == null || customer.getPaymentTerms().isBlank()) {
            var defaultSettlement = erpSettlementMethodMapper.findDefault(tenantId);
            if (defaultSettlement != null) {
                customer.setPaymentTerms(defaultSettlement.getCode());
            }
        }
        if (customer.getDeliveryMethodCode() == null || customer.getDeliveryMethodCode().isBlank()) {
            var defaultDelivery = erpDeliveryMethodMapper.findDefault(tenantId);
            if (defaultDelivery != null) {
                customer.setDeliveryMethodCode(defaultDelivery.getCode());
            }
        }
    }

    private String generateCustomerCode(Long tenantId) {
        String prefix = readConfig("erp.customer.code.prefix", "CU");
        String dateFormat = readConfig("erp.customer.code.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.customer.code.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, CUSTOMER_CODE_TYPE, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, CUSTOMER_CODE_TYPE, dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private String readConfig(String key, String fallback) {
        SystemConfig config = systemConfigMapper.findByKey(key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return fallback;
        }
        return config.getConfigValue().trim();
    }

    private int readIntConfig(String key, int fallback) {
        String value = readConfig(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private JsonNode parseContacts(String rawContacts) {
        if (rawContacts == null || rawContacts.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(rawContacts);
        } catch (Exception ex) {
            throw new IllegalArgumentException("联系人列表格式不正确", ex);
        }
    }
}
