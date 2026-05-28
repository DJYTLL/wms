package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.entity.SystemConfig;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSupplierCreateRequest;
import com.example.wms.dto.erp.ErpSupplierUpdateRequest;
import com.example.wms.entity.erp.ErpAccountsPayable;
import com.example.wms.entity.erp.ErpPayment;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.entity.erp.ErpPurchaseReturn;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPaymentMethodMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.service.erp.ErpSupplierService;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// 供应商服务实现（ERP进销存）
@Service
public class ErpSupplierServiceImpl implements ErpSupplierService {
    private static final String SUPPLIER_CODE_TYPE = "SUPPLIER";

    private final ErpSupplierMapper erpSupplierMapper;
    private final ErpPurchaseOrderMapper erpPurchaseOrderMapper;
    private final ErpPurchaseReturnMapper erpPurchaseReturnMapper;
    private final ErpPaymentMapper erpPaymentMapper;
    private final ErpAccountsPayableMapper erpAccountsPayableMapper;
    private final ErpSettlementMethodMapper erpSettlementMethodMapper;
    private final ErpPaymentMethodMapper erpPaymentMethodMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;

    public ErpSupplierServiceImpl(ErpSupplierMapper erpSupplierMapper,
                                  ErpPurchaseOrderMapper erpPurchaseOrderMapper,
                                  ErpPurchaseReturnMapper erpPurchaseReturnMapper,
                                  ErpPaymentMapper erpPaymentMapper,
                                  ErpAccountsPayableMapper erpAccountsPayableMapper,
                                  ErpSettlementMethodMapper erpSettlementMethodMapper,
                                  ErpPaymentMethodMapper erpPaymentMethodMapper,
                                  ErpOrderSequenceMapper erpOrderSequenceMapper,
                                  SystemConfigMapper systemConfigMapper,
                                  ObjectMapper objectMapper) {
        this.erpSupplierMapper = erpSupplierMapper;
        this.erpPurchaseOrderMapper = erpPurchaseOrderMapper;
        this.erpPurchaseReturnMapper = erpPurchaseReturnMapper;
        this.erpPaymentMapper = erpPaymentMapper;
        this.erpAccountsPayableMapper = erpAccountsPayableMapper;
        this.erpSettlementMethodMapper = erpSettlementMethodMapper;
        this.erpPaymentMethodMapper = erpPaymentMethodMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ErpSupplier> listAll(String keyword, String contact, String phone, String status) {
        Long tenantId = TenantContext.requireTenantId();
        QueryWrapper<ErpSupplier> wrapper = baseWrapper(tenantId, keyword, contact, phone, status);
        wrapper.orderByAsc("id");
        List<ErpSupplier> suppliers = erpSupplierMapper.selectList(wrapper);
        enrichRecentTransactionAt(tenantId, suppliers);
        return suppliers;
    }

    @Override
    public PageResponse<ErpSupplier> page(long page, long size, String keyword, String contact, String phone, String status) {
        Long tenantId = TenantContext.requireTenantId();
        Page<ErpSupplier> pageReq = Page.of(page, size);
        QueryWrapper<ErpSupplier> wrapper = baseWrapper(tenantId, keyword, contact, phone, status);
        wrapper.orderByAsc("id");
        Page<ErpSupplier> result = erpSupplierMapper.selectPage(pageReq, wrapper);
        enrichRecentTransactionAt(tenantId, result.getRecords());
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpSupplier getById(Long id) {
        ErpSupplier supplier = erpSupplierMapper.selectOne(new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        return supplier;
    }

    @Override
    public String nextCode() {
        Long tenantId = TenantContext.requireTenantId();
        return generateSupplierCode(tenantId);
    }

    @Override
    @AuditLog(action = "ERP_SUPPLIER_CREATE", entityType = "erp_supplier", entityId = "{result.id}", detail = "code={arg0.code}")
@Transactional
    public ErpSupplier create(ErpSupplierCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSupplier existing = erpSupplierMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("供应商编码已存在");
        }
        ErpSupplier supplier = new ErpSupplier();
        supplier.setTenantId(tenantId);
        applyRequest(supplier, request);
        applyDefaultMethodsIfMissing(supplier, tenantId);
        applyStatus(supplier, request.enabled(), request.blacklisted());
        supplier.setCreatedAt(Instant.now());
        supplier.setUpdatedAt(Instant.now());
        erpSupplierMapper.insert(supplier);
        return supplier;
    }

    @Override
    @AuditLog(action = "ERP_SUPPLIER_UPDATE", entityType = "erp_supplier", entityId = "{arg0}", detail = "code={arg1.code}")
@Transactional
    public ErpSupplier update(Long id, ErpSupplierUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSupplier supplier = erpSupplierMapper.selectOne(new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        ErpSupplier existing = erpSupplierMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("供应商编码已存在");
        }
        applyRequest(supplier, request);
        applyDefaultMethodsIfMissing(supplier, tenantId);
        if (request.enabled() != null || request.blacklisted() != null) {
            applyStatus(supplier, request.enabled(), request.blacklisted());
        }
        supplier.setUpdatedAt(Instant.now());
        erpSupplierMapper.updateById(supplier);
        return supplier;
    }

    @Override
    @AuditLog(action = "ERP_SUPPLIER_DELETE", entityType = "erp_supplier", entityId = "{arg0}")
@Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSupplier supplier = erpSupplierMapper.selectOne(new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        ensureSupplierNotReferenced(tenantId, id);
        erpSupplierMapper.deleteById(id);
    }

    private void ensureSupplierNotReferenced(Long tenantId, Long supplierId) {
        if (erpPurchaseOrderMapper.selectCount(new QueryWrapper<ErpPurchaseOrder>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)) > 0) {
            throw new IllegalArgumentException("供应商已被采购单引用，不能删除");
        }
        if (erpPurchaseReturnMapper.selectCount(new QueryWrapper<ErpPurchaseReturn>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)) > 0) {
            throw new IllegalArgumentException("供应商已被采购退货单引用，不能删除");
        }
        if (erpPaymentMapper.selectCount(new QueryWrapper<ErpPayment>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)) > 0) {
            throw new IllegalArgumentException("供应商已被付款单引用，不能删除");
        }
        if (erpAccountsPayableMapper.selectCount(new QueryWrapper<ErpAccountsPayable>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)) > 0) {
            throw new IllegalArgumentException("供应商已被应付单引用，不能删除");
        }
    }

    private QueryWrapper<ErpSupplier> baseWrapper(Long tenantId, String keyword, String contact, String phone, String status) {
        QueryWrapper<ErpSupplier> wrapper = new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", tenantId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword)
                .or()
                .like("short_name", keyword)
                .or()
                .like("contact", keyword)
                .or()
                .like("phone", keyword)
                .or()
                .like("mobile", keyword)
                .or()
                .like("email", keyword)
                .or()
                .like("tax_no", keyword)
                .or()
                .like("bank_account", keyword)
                .or()
                .like("address", keyword)
                .or()
                .apply("CAST(contacts AS TEXT) LIKE {0}", wrapLike(keyword)));
        }
        if (contact != null && !contact.isBlank()) {
            wrapper.and(q -> q.like("contact", contact)
                .or()
                .apply("CAST(contacts AS TEXT) LIKE {0}", wrapLike(contact)));
        }
        if (phone != null && !phone.isBlank()) {
            wrapper.and(q -> q.like("phone", phone)
                .or()
                .like("mobile", phone)
                .or()
                .apply("CAST(contacts AS TEXT) LIKE {0}", wrapLike(phone)));
        }
        if ("enabled".equalsIgnoreCase(status)) {
            wrapper.eq("is_enabled", true).eq("is_blacklisted", false);
        } else if ("disabled".equalsIgnoreCase(status)) {
            wrapper.eq("is_enabled", false).eq("is_blacklisted", false);
        } else if ("blacklisted".equalsIgnoreCase(status)) {
            wrapper.eq("is_blacklisted", true);
        }
        return wrapper;
    }

    private String wrapLike(String value) {
        return "%" + value.trim() + "%";
    }

    private void applyStatus(ErpSupplier supplier, Boolean enabled, Boolean blacklisted) {
        boolean finalBlacklisted = blacklisted != null && blacklisted;
        boolean finalEnabled = enabled == null || enabled;
        if (finalBlacklisted) {
            finalEnabled = false;
        }
        supplier.setBlacklisted(finalBlacklisted);
        supplier.setEnabled(finalEnabled);
    }

    private void enrichRecentTransactionAt(Long tenantId, List<ErpSupplier> suppliers) {
        if (suppliers == null || suppliers.isEmpty()) {
            return;
        }
        List<Long> supplierIds = suppliers.stream()
            .map(ErpSupplier::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (supplierIds.isEmpty()) {
            return;
        }
        Map<Long, Instant> recentTransactionMap = new HashMap<>();
        for (ErpSupplier row : erpSupplierMapper.findRecentTransactionRows(tenantId, supplierIds)) {
            if (row.getId() != null) {
                recentTransactionMap.put(row.getId(), row.getRecentTransactionAt());
            }
        }
        for (ErpSupplier supplier : suppliers) {
            if (supplier.getId() == null) {
                continue;
            }
            supplier.setRecentTransactionAt(recentTransactionMap.get(supplier.getId()));
        }
    }

    private void applyRequest(ErpSupplier supplier, ErpSupplierCreateRequest request) {
        supplier.setCode(request.code());
        supplier.setName(request.name());
        supplier.setShortName(request.shortName());
        supplier.setContact(request.contact());
        supplier.setPhone(request.phone());
        supplier.setMobile(request.mobile());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setTaxNo(request.taxNo());
        supplier.setBankName(request.bankName());
        supplier.setBankAccount(request.bankAccount());
        supplier.setDefaultSettlementMethodCode(request.defaultSettlementMethodCode());
        supplier.setDefaultPaymentMethodCode(request.defaultPaymentMethodCode());
        supplier.setContacts(parseContacts(request.contacts()));
        supplier.setRemark(request.remark());
    }

    private void applyRequest(ErpSupplier supplier, ErpSupplierUpdateRequest request) {
        supplier.setCode(request.code());
        supplier.setName(request.name());
        supplier.setShortName(request.shortName());
        supplier.setContact(request.contact());
        supplier.setPhone(request.phone());
        supplier.setMobile(request.mobile());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setTaxNo(request.taxNo());
        supplier.setBankName(request.bankName());
        supplier.setBankAccount(request.bankAccount());
        supplier.setDefaultSettlementMethodCode(request.defaultSettlementMethodCode());
        supplier.setDefaultPaymentMethodCode(request.defaultPaymentMethodCode());
        supplier.setContacts(parseContacts(request.contacts()));
        supplier.setRemark(request.remark());
    }

    private void applyDefaultMethodsIfMissing(ErpSupplier supplier, Long tenantId) {
        if (supplier.getDefaultSettlementMethodCode() == null || supplier.getDefaultSettlementMethodCode().isBlank()) {
            var defaultSettlement = erpSettlementMethodMapper.findDefault(tenantId);
            if (defaultSettlement != null) {
                supplier.setDefaultSettlementMethodCode(defaultSettlement.getCode());
            }
        }
        if (supplier.getDefaultPaymentMethodCode() == null || supplier.getDefaultPaymentMethodCode().isBlank()) {
            var defaultPayment = erpPaymentMethodMapper.findDefault(tenantId);
            if (defaultPayment != null) {
                supplier.setDefaultPaymentMethodCode(defaultPayment.getCode());
            }
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

    private String generateSupplierCode(Long tenantId) {
        String prefix = readConfig("erp.supplier.code.prefix", "SU");
        String dateFormat = readConfig("erp.supplier.code.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.supplier.code.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, SUPPLIER_CODE_TYPE, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, SUPPLIER_CODE_TYPE, dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private String readConfig(String key, String fallback) {
        SystemConfig config = systemConfigMapper.findByKey(TenantContext.requireTenantId(), key);
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
}
