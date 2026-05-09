package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSettlementMethodCreateRequest;
import com.example.wms.dto.erp.ErpSettlementMethodUpdateRequest;
import com.example.wms.entity.erp.ErpSettlementMethod;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.service.erp.ErpSettlementMethodService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 结算方式服务实现（ERP进销存）
@Service
public class ErpSettlementMethodServiceImpl implements ErpSettlementMethodService {
    private final ErpSettlementMethodMapper erpSettlementMethodMapper;

    public ErpSettlementMethodServiceImpl(ErpSettlementMethodMapper erpSettlementMethodMapper) {
        this.erpSettlementMethodMapper = erpSettlementMethodMapper;
    }

    @Override
    public List<ErpSettlementMethod> listAll(String keyword, Boolean enabled) {
        QueryWrapper<ErpSettlementMethod> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByDesc("is_default").orderByAsc("sort_no", "id");
        return erpSettlementMethodMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpSettlementMethod> page(long page, long size, String keyword, Boolean enabled) {
        Page<ErpSettlementMethod> pageReq = Page.of(page, size);
        QueryWrapper<ErpSettlementMethod> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByDesc("is_default").orderByAsc("sort_no", "id");
        Page<ErpSettlementMethod> result = erpSettlementMethodMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpSettlementMethod getById(Long id) {
        ErpSettlementMethod method = erpSettlementMethodMapper.selectOne(new QueryWrapper<ErpSettlementMethod>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (method == null) {
            throw new IllegalArgumentException("结算方式不存在");
        }
        return method;
    }

    @Override
    @AuditLog(action = "ERP_SETTLEMENT_METHOD_CREATE", entityType = "erp_settlement_method", entityId = "{result.id}", detail = "code={arg0.code}")
    public ErpSettlementMethod create(ErpSettlementMethodCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSettlementMethod existing = erpSettlementMethodMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("结算方式编码已存在");
        }
        ErpSettlementMethod method = new ErpSettlementMethod();
        method.setTenantId(tenantId);
        applyRequest(method, request);
        method.setEnabled(request.enabled() == null || request.enabled());
        applyDefaultFlag(tenantId, method, request.isDefault());
        method.setCreatedAt(Instant.now());
        method.setUpdatedAt(Instant.now());
        erpSettlementMethodMapper.insert(method);
        return method;
    }

    @Override
    @AuditLog(action = "ERP_SETTLEMENT_METHOD_UPDATE", entityType = "erp_settlement_method", entityId = "{arg0}", detail = "code={arg1.code}")
    public ErpSettlementMethod update(Long id, ErpSettlementMethodUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSettlementMethod method = erpSettlementMethodMapper.selectOne(new QueryWrapper<ErpSettlementMethod>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (method == null) {
            throw new IllegalArgumentException("结算方式不存在");
        }
        ErpSettlementMethod existing = erpSettlementMethodMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("结算方式编码已存在");
        }
        applyRequest(method, request);
        if (request.enabled() != null) {
            method.setEnabled(request.enabled());
        }
        applyDefaultFlag(tenantId, method, request.isDefault());
        method.setUpdatedAt(Instant.now());
        erpSettlementMethodMapper.updateById(method);
        return method;
    }

    @Override
    @AuditLog(action = "ERP_SETTLEMENT_METHOD_DELETE", entityType = "erp_settlement_method", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSettlementMethod method = erpSettlementMethodMapper.selectOne(new QueryWrapper<ErpSettlementMethod>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (method == null) {
            throw new IllegalArgumentException("结算方式不存在");
        }
        erpSettlementMethodMapper.deleteById(id);
    }

    private QueryWrapper<ErpSettlementMethod> baseWrapper(String keyword, Boolean enabled) {
        QueryWrapper<ErpSettlementMethod> wrapper = new QueryWrapper<ErpSettlementMethod>()
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

    private void applyRequest(ErpSettlementMethod method, ErpSettlementMethodCreateRequest request) {
        method.setCode(request.code());
        method.setName(request.name());
        method.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        method.setRemark(request.remark());
    }

    private void applyRequest(ErpSettlementMethod method, ErpSettlementMethodUpdateRequest request) {
        method.setCode(request.code());
        method.setName(request.name());
        method.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        method.setRemark(request.remark());
    }

    private void applyDefaultFlag(Long tenantId, ErpSettlementMethod method, Boolean isDefault) {
        if (isDefault == null) {
            return;
        }
        if (Boolean.TRUE.equals(isDefault)) {
            erpSettlementMethodMapper.update(null, new UpdateWrapper<ErpSettlementMethod>()
                .eq("tenant_id", tenantId)
                .set("is_default", false));
            method.setIsDefault(true);
        } else {
            method.setIsDefault(false);
        }
    }
}
