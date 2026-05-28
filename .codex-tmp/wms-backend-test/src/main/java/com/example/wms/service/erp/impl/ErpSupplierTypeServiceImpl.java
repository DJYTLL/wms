package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSupplierTypeCreateRequest;
import com.example.wms.dto.erp.ErpSupplierTypeUpdateRequest;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.entity.erp.ErpSupplierType;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.mapper.erp.ErpSupplierTypeMapper;
import com.example.wms.service.erp.ErpSupplierTypeService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

// 供应商类型服务实现（ERP进销存）
@Service
public class ErpSupplierTypeServiceImpl implements ErpSupplierTypeService {
    private final ErpSupplierTypeMapper erpSupplierTypeMapper;
    private final ErpSupplierMapper erpSupplierMapper;

    public ErpSupplierTypeServiceImpl(ErpSupplierTypeMapper erpSupplierTypeMapper,
                                      ErpSupplierMapper erpSupplierMapper) {
        this.erpSupplierTypeMapper = erpSupplierTypeMapper;
        this.erpSupplierMapper = erpSupplierMapper;
    }

    @Override
    public List<ErpSupplierType> listAll(String keyword, Boolean enabled) {
        QueryWrapper<ErpSupplierType> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("sort", "id");
        return erpSupplierTypeMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpSupplierType> page(long page, long size, String keyword, Boolean enabled) {
        Page<ErpSupplierType> pageReq = Page.of(page, size);
        QueryWrapper<ErpSupplierType> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("sort", "id");
        Page<ErpSupplierType> result = erpSupplierTypeMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpSupplierType getById(Long id) {
        ErpSupplierType supplierType = erpSupplierTypeMapper.selectOne(new QueryWrapper<ErpSupplierType>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (supplierType == null) {
            throw new IllegalArgumentException("供应商类型不存在");
        }
        return supplierType;
    }

    @Override
    @AuditLog(action = "ERP_SUPPLIER_TYPE_CREATE", entityType = "erp_supplier_type", entityId = "{result.id}", detail = "code={arg0.code}")
    @Transactional
    public ErpSupplierType create(ErpSupplierTypeCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ensureCodeUnique(tenantId, request.code(), null);
        ensureNameUnique(tenantId, request.name(), null);

        ErpSupplierType supplierType = new ErpSupplierType();
        supplierType.setTenantId(tenantId);
        applyRequest(supplierType, request);
        supplierType.setEnabled(request.enabled() == null || request.enabled());
        supplierType.setCreatedAt(Instant.now());
        supplierType.setUpdatedAt(Instant.now());
        erpSupplierTypeMapper.insert(supplierType);
        return supplierType;
    }

    @Override
    @AuditLog(action = "ERP_SUPPLIER_TYPE_UPDATE", entityType = "erp_supplier_type", entityId = "{arg0}", detail = "code={arg1.code}")
    @Transactional
    public ErpSupplierType update(Long id, ErpSupplierTypeUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSupplierType supplierType = erpSupplierTypeMapper.selectOne(new QueryWrapper<ErpSupplierType>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (supplierType == null) {
            throw new IllegalArgumentException("供应商类型不存在");
        }
        ensureCodeUnique(tenantId, request.code(), id);
        ensureNameUnique(tenantId, request.name(), id);

        applyRequest(supplierType, request);
        if (request.enabled() != null) {
            supplierType.setEnabled(request.enabled());
        }
        supplierType.setUpdatedAt(Instant.now());
        erpSupplierTypeMapper.updateById(supplierType);
        return supplierType;
    }

    @Override
    @AuditLog(action = "ERP_SUPPLIER_TYPE_DELETE", entityType = "erp_supplier_type", entityId = "{arg0}")
    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSupplierType supplierType = erpSupplierTypeMapper.selectOne(new QueryWrapper<ErpSupplierType>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (supplierType == null) {
            throw new IllegalArgumentException("供应商类型不存在");
        }
        if (erpSupplierMapper.selectCount(new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", tenantId)
            .eq("supplier_type_id", id)) > 0) {
            throw new IllegalArgumentException("供应商类型已被供应商引用，不能删除");
        }
        erpSupplierTypeMapper.deleteById(id);
    }

    private QueryWrapper<ErpSupplierType> baseWrapper(String keyword, Boolean enabled) {
        QueryWrapper<ErpSupplierType> wrapper = new QueryWrapper<ErpSupplierType>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword));
        }
        if (enabled != null) {
            wrapper.eq("enabled", enabled);
        }
        return wrapper;
    }

    private void ensureCodeUnique(Long tenantId, String code, Long currentId) {
        ErpSupplierType existing = erpSupplierTypeMapper.findByCode(tenantId, code);
        if (existing != null && (currentId == null || !existing.getId().equals(currentId))) {
            throw new IllegalArgumentException("供应商类型编码已存在");
        }
    }

    private void ensureNameUnique(Long tenantId, String name, Long currentId) {
        ErpSupplierType existing = erpSupplierTypeMapper.findByName(tenantId, name);
        if (existing != null && (currentId == null || !existing.getId().equals(currentId))) {
            throw new IllegalArgumentException("供应商类型名称已存在");
        }
    }

    private void applyRequest(ErpSupplierType supplierType, ErpSupplierTypeCreateRequest request) {
        supplierType.setCode(request.code());
        supplierType.setName(request.name());
        supplierType.setSort(request.sort() == null ? 0 : request.sort());
        supplierType.setRemark(request.remark());
    }

    private void applyRequest(ErpSupplierType supplierType, ErpSupplierTypeUpdateRequest request) {
        supplierType.setCode(request.code());
        supplierType.setName(request.name());
        supplierType.setSort(request.sort() == null ? 0 : request.sort());
        supplierType.setRemark(request.remark());
    }
}
