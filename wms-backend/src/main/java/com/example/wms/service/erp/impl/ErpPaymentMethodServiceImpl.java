package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPaymentMethodCreateRequest;
import com.example.wms.dto.erp.ErpPaymentMethodUpdateRequest;
import com.example.wms.entity.erp.ErpPayment;
import com.example.wms.entity.erp.ErpPaymentMethod;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPaymentMethodMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.service.erp.ErpPaymentMethodService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 付款方式服务实现（ERP进销存）
@Service
public class ErpPaymentMethodServiceImpl implements ErpPaymentMethodService {
    private final ErpPaymentMethodMapper erpPaymentMethodMapper;
    private final ErpPurchaseOrderMapper erpPurchaseOrderMapper;
    private final ErpPaymentMapper erpPaymentMapper;

    public ErpPaymentMethodServiceImpl(ErpPaymentMethodMapper erpPaymentMethodMapper,
                                       ErpPurchaseOrderMapper erpPurchaseOrderMapper,
                                       ErpPaymentMapper erpPaymentMapper) {
        this.erpPaymentMethodMapper = erpPaymentMethodMapper;
        this.erpPurchaseOrderMapper = erpPurchaseOrderMapper;
        this.erpPaymentMapper = erpPaymentMapper;
    }

    @Override
    public List<ErpPaymentMethod> listAll(String keyword, Boolean enabled) {
        QueryWrapper<ErpPaymentMethod> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByDesc("is_default").orderByAsc("sort_no", "id");
        return erpPaymentMethodMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpPaymentMethod> page(long page, long size, String keyword, Boolean enabled) {
        Page<ErpPaymentMethod> pageReq = Page.of(page, size);
        QueryWrapper<ErpPaymentMethod> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByDesc("is_default").orderByAsc("sort_no", "id");
        Page<ErpPaymentMethod> result = erpPaymentMethodMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpPaymentMethod getById(Long id) {
        ErpPaymentMethod method = erpPaymentMethodMapper.selectOne(new QueryWrapper<ErpPaymentMethod>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (method == null) {
            throw new IllegalArgumentException("付款方式不存在");
        }
        return method;
    }

    @Override
    @AuditLog(action = "ERP_PAYMENT_METHOD_CREATE", entityType = "erp_payment_method", entityId = "{result.id}", detail = "code={arg0.code}")
    public ErpPaymentMethod create(ErpPaymentMethodCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPaymentMethod existing = erpPaymentMethodMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("付款方式编码已存在");
        }
        ErpPaymentMethod method = new ErpPaymentMethod();
        method.setTenantId(tenantId);
        applyRequest(method, request);
        method.setEnabled(request.enabled() == null || request.enabled());
        applyDefaultFlag(tenantId, method, request.isDefault());
        method.setCreatedAt(Instant.now());
        method.setUpdatedAt(Instant.now());
        erpPaymentMethodMapper.insert(method);
        return method;
    }

    @Override
    @AuditLog(action = "ERP_PAYMENT_METHOD_UPDATE", entityType = "erp_payment_method", entityId = "{arg0}", detail = "code={arg1.code}")
    public ErpPaymentMethod update(Long id, ErpPaymentMethodUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPaymentMethod method = erpPaymentMethodMapper.selectOne(new QueryWrapper<ErpPaymentMethod>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (method == null) {
            throw new IllegalArgumentException("付款方式不存在");
        }
        ErpPaymentMethod existing = erpPaymentMethodMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("付款方式编码已存在");
        }
        applyRequest(method, request);
        if (request.enabled() != null) {
            method.setEnabled(request.enabled());
        }
        applyDefaultFlag(tenantId, method, request.isDefault());
        method.setUpdatedAt(Instant.now());
        erpPaymentMethodMapper.updateById(method);
        return method;
    }

    @Override
    @AuditLog(action = "ERP_PAYMENT_METHOD_DELETE", entityType = "erp_payment_method", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPaymentMethod method = erpPaymentMethodMapper.selectOne(new QueryWrapper<ErpPaymentMethod>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (method == null) {
            throw new IllegalArgumentException("付款方式不存在");
        }
        ensurePaymentMethodNotReferenced(tenantId, method.getCode());
        erpPaymentMethodMapper.deleteById(id);
    }

    private void ensurePaymentMethodNotReferenced(Long tenantId, String code) {
        if (erpPurchaseOrderMapper.selectCount(new QueryWrapper<ErpPurchaseOrder>()
            .eq("tenant_id", tenantId)
            .eq("payment_method_code", code)) > 0) {
            throw new IllegalArgumentException("付款方式已被采购单引用，不能删除");
        }
        if (erpPaymentMapper.selectCount(new QueryWrapper<ErpPayment>()
            .eq("tenant_id", tenantId)
            .eq("payment_method_code", code)) > 0) {
            throw new IllegalArgumentException("付款方式已被付款单引用，不能删除");
        }
    }

    private QueryWrapper<ErpPaymentMethod> baseWrapper(String keyword, Boolean enabled) {
        QueryWrapper<ErpPaymentMethod> wrapper = new QueryWrapper<ErpPaymentMethod>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        return wrapper;
    }

    private void applyRequest(ErpPaymentMethod method, ErpPaymentMethodCreateRequest request) {
        method.setCode(request.code());
        method.setName(request.name());
        method.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        method.setRemark(request.remark());
    }

    private void applyRequest(ErpPaymentMethod method, ErpPaymentMethodUpdateRequest request) {
        method.setCode(request.code());
        method.setName(request.name());
        method.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        method.setRemark(request.remark());
    }

    private void applyDefaultFlag(Long tenantId, ErpPaymentMethod method, Boolean isDefault) {
        if (isDefault == null) {
            return;
        }
        if (Boolean.TRUE.equals(isDefault)) {
            erpPaymentMethodMapper.update(null, new UpdateWrapper<ErpPaymentMethod>()
                .eq("tenant_id", tenantId)
                .set("is_default", false));
            method.setIsDefault(true);
        } else {
            method.setIsDefault(false);
        }
    }
}
