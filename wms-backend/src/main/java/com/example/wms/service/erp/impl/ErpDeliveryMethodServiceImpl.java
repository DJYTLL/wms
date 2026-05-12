package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpDeliveryMethodCreateRequest;
import com.example.wms.dto.erp.ErpDeliveryMethodUpdateRequest;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpDeliveryMethod;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpDeliveryMethodMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.service.erp.ErpDeliveryMethodService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 送货方式服务实现（ERP进销存）
@Service
public class ErpDeliveryMethodServiceImpl implements ErpDeliveryMethodService {
    private final ErpDeliveryMethodMapper erpDeliveryMethodMapper;
    private final ErpCustomerMapper erpCustomerMapper;
    private final ErpSaleOrderMapper erpSaleOrderMapper;

    public ErpDeliveryMethodServiceImpl(ErpDeliveryMethodMapper erpDeliveryMethodMapper,
                                        ErpCustomerMapper erpCustomerMapper,
                                        ErpSaleOrderMapper erpSaleOrderMapper) {
        this.erpDeliveryMethodMapper = erpDeliveryMethodMapper;
        this.erpCustomerMapper = erpCustomerMapper;
        this.erpSaleOrderMapper = erpSaleOrderMapper;
    }

    @Override
    public List<ErpDeliveryMethod> listAll(String keyword, Boolean enabled) {
        QueryWrapper<ErpDeliveryMethod> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByDesc("is_default").orderByAsc("sort_no", "id");
        return erpDeliveryMethodMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpDeliveryMethod> page(long page, long size, String keyword, Boolean enabled) {
        Page<ErpDeliveryMethod> pageReq = Page.of(page, size);
        QueryWrapper<ErpDeliveryMethod> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByDesc("is_default").orderByAsc("sort_no", "id");
        Page<ErpDeliveryMethod> result = erpDeliveryMethodMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpDeliveryMethod getById(Long id) {
        ErpDeliveryMethod method = erpDeliveryMethodMapper.selectOne(new QueryWrapper<ErpDeliveryMethod>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (method == null) {
            throw new IllegalArgumentException("送货方式不存在");
        }
        return method;
    }

    @Override
    @AuditLog(action = "ERP_DELIVERY_METHOD_CREATE", entityType = "erp_delivery_method", entityId = "{result.id}", detail = "code={arg0.code}")
    public ErpDeliveryMethod create(ErpDeliveryMethodCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpDeliveryMethod existing = erpDeliveryMethodMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("送货方式编码已存在");
        }
        ErpDeliveryMethod method = new ErpDeliveryMethod();
        method.setTenantId(tenantId);
        applyRequest(method, request);
        method.setEnabled(request.enabled() == null || request.enabled());
        applyDefaultFlag(tenantId, method, request.isDefault());
        method.setCreatedAt(Instant.now());
        method.setUpdatedAt(Instant.now());
        erpDeliveryMethodMapper.insert(method);
        return method;
    }

    @Override
    @AuditLog(action = "ERP_DELIVERY_METHOD_UPDATE", entityType = "erp_delivery_method", entityId = "{arg0}", detail = "code={arg1.code}")
    public ErpDeliveryMethod update(Long id, ErpDeliveryMethodUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpDeliveryMethod method = erpDeliveryMethodMapper.selectOne(new QueryWrapper<ErpDeliveryMethod>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (method == null) {
            throw new IllegalArgumentException("送货方式不存在");
        }
        ErpDeliveryMethod existing = erpDeliveryMethodMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("送货方式编码已存在");
        }
        applyRequest(method, request);
        if (request.enabled() != null) {
            method.setEnabled(request.enabled());
        }
        applyDefaultFlag(tenantId, method, request.isDefault());
        method.setUpdatedAt(Instant.now());
        erpDeliveryMethodMapper.updateById(method);
        return method;
    }

    @Override
    @AuditLog(action = "ERP_DELIVERY_METHOD_DELETE", entityType = "erp_delivery_method", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpDeliveryMethod method = erpDeliveryMethodMapper.selectOne(new QueryWrapper<ErpDeliveryMethod>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (method == null) {
            throw new IllegalArgumentException("送货方式不存在");
        }
        ensureDeliveryMethodNotReferenced(tenantId, method.getCode());
        erpDeliveryMethodMapper.deleteById(id);
    }

    private void ensureDeliveryMethodNotReferenced(Long tenantId, String code) {
        if (erpCustomerMapper.selectCount(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .eq("delivery_method_code", code)) > 0) {
            throw new IllegalArgumentException("送货方式已被客户引用，不能删除");
        }
        if (erpSaleOrderMapper.selectCount(new QueryWrapper<ErpSaleOrder>()
            .eq("tenant_id", tenantId)
            .eq("delivery_method_code", code)) > 0) {
            throw new IllegalArgumentException("送货方式已被销售单引用，不能删除");
        }
    }

    private QueryWrapper<ErpDeliveryMethod> baseWrapper(String keyword, Boolean enabled) {
        QueryWrapper<ErpDeliveryMethod> wrapper = new QueryWrapper<ErpDeliveryMethod>()
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

    private void applyRequest(ErpDeliveryMethod method, ErpDeliveryMethodCreateRequest request) {
        method.setCode(request.code());
        method.setName(request.name());
        method.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        method.setRemark(request.remark());
    }

    private void applyRequest(ErpDeliveryMethod method, ErpDeliveryMethodUpdateRequest request) {
        method.setCode(request.code());
        method.setName(request.name());
        method.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        method.setRemark(request.remark());
    }

    private void applyDefaultFlag(Long tenantId, ErpDeliveryMethod method, Boolean isDefault) {
        if (isDefault == null) {
            return;
        }
        if (Boolean.TRUE.equals(isDefault)) {
            erpDeliveryMethodMapper.update(null, new UpdateWrapper<ErpDeliveryMethod>()
                .eq("tenant_id", tenantId)
                .set("is_default", false));
            method.setIsDefault(true);
        } else {
            method.setIsDefault(false);
        }
    }
}
