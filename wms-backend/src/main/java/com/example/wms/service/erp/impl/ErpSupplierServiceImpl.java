package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
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
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.service.erp.ErpSupplierService;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 供应商服务实现（ERP进销存）
@Service
public class ErpSupplierServiceImpl implements ErpSupplierService {
    private final ErpSupplierMapper erpSupplierMapper;
    private final ErpPurchaseOrderMapper erpPurchaseOrderMapper;
    private final ErpPurchaseReturnMapper erpPurchaseReturnMapper;
    private final ErpPaymentMapper erpPaymentMapper;
    private final ErpAccountsPayableMapper erpAccountsPayableMapper;
    private final ObjectMapper objectMapper;

    public ErpSupplierServiceImpl(ErpSupplierMapper erpSupplierMapper,
                                  ErpPurchaseOrderMapper erpPurchaseOrderMapper,
                                  ErpPurchaseReturnMapper erpPurchaseReturnMapper,
                                  ErpPaymentMapper erpPaymentMapper,
                                  ErpAccountsPayableMapper erpAccountsPayableMapper,
                                  ObjectMapper objectMapper) {
        this.erpSupplierMapper = erpSupplierMapper;
        this.erpPurchaseOrderMapper = erpPurchaseOrderMapper;
        this.erpPurchaseReturnMapper = erpPurchaseReturnMapper;
        this.erpPaymentMapper = erpPaymentMapper;
        this.erpAccountsPayableMapper = erpAccountsPayableMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ErpSupplier> listAll(String keyword, Boolean enabled) {
        QueryWrapper<ErpSupplier> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("id");
        return erpSupplierMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpSupplier> page(long page, long size, String keyword, Boolean enabled) {
        Page<ErpSupplier> pageReq = Page.of(page, size);
        QueryWrapper<ErpSupplier> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("id");
        Page<ErpSupplier> result = erpSupplierMapper.selectPage(pageReq, wrapper);
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
    @AuditLog(action = "ERP_SUPPLIER_CREATE", entityType = "erp_supplier", entityId = "{result.id}", detail = "code={arg0.code}")
    public ErpSupplier create(ErpSupplierCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSupplier existing = erpSupplierMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("供应商编码已存在");
        }
        ErpSupplier supplier = new ErpSupplier();
        supplier.setTenantId(tenantId);
        applyRequest(supplier, request);
        supplier.setEnabled(request.enabled() == null || request.enabled());
        supplier.setCreatedAt(Instant.now());
        supplier.setUpdatedAt(Instant.now());
        erpSupplierMapper.insert(supplier);
        return supplier;
    }

    @Override
    @AuditLog(action = "ERP_SUPPLIER_UPDATE", entityType = "erp_supplier", entityId = "{arg0}", detail = "code={arg1.code}")
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
        if (request.enabled() != null) {
            supplier.setEnabled(request.enabled());
        }
        supplier.setUpdatedAt(Instant.now());
        erpSupplierMapper.updateById(supplier);
        return supplier;
    }

    @Override
    @AuditLog(action = "ERP_SUPPLIER_DELETE", entityType = "erp_supplier", entityId = "{arg0}")
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

    private QueryWrapper<ErpSupplier> baseWrapper(String keyword, Boolean enabled) {
        QueryWrapper<ErpSupplier> wrapper = new QueryWrapper<ErpSupplier>()
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
        return wrapper;
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
        supplier.setPaymentTerms(request.paymentTerms());
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
        supplier.setPaymentTerms(request.paymentTerms());
        supplier.setContacts(parseContacts(request.contacts()));
        supplier.setRemark(request.remark());
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
