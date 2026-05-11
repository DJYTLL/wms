package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpUnitCreateRequest;
import com.example.wms.dto.erp.ErpUnitUpdateRequest;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpUnit;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpUnitMapper;
import com.example.wms.service.erp.ErpUnitService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 单位服务实现（ERP进销存）
@Service
public class ErpUnitServiceImpl implements ErpUnitService {
    private final ErpUnitMapper erpUnitMapper;
    private final ErpProductMapper erpProductMapper;

    public ErpUnitServiceImpl(ErpUnitMapper erpUnitMapper, ErpProductMapper erpProductMapper) {
        this.erpUnitMapper = erpUnitMapper;
        this.erpProductMapper = erpProductMapper;
    }

    @Override
    public List<ErpUnit> listAll(String keyword, Boolean enabled) {
        QueryWrapper<ErpUnit> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("id");
        return erpUnitMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpUnit> page(long page, long size, String keyword, Boolean enabled) {
        Page<ErpUnit> pageReq = Page.of(page, size);
        QueryWrapper<ErpUnit> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("id");
        Page<ErpUnit> result = erpUnitMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpUnit getById(Long id) {
        ErpUnit unit = erpUnitMapper.selectOne(new QueryWrapper<ErpUnit>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (unit == null) {
            throw new IllegalArgumentException("单位不存在");
        }
        return unit;
    }

    @Override
    @AuditLog(action = "ERP_UNIT_CREATE", entityType = "erp_unit", entityId = "{result.id}", detail = "code={arg0.code}")
    public ErpUnit create(ErpUnitCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpUnit existing = erpUnitMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("单位编码已存在");
        }
        ErpUnit unit = new ErpUnit();
        unit.setTenantId(tenantId);
        applyRequest(unit, request);
        unit.setEnabled(request.enabled() == null || request.enabled());
        unit.setCreatedAt(Instant.now());
        unit.setUpdatedAt(Instant.now());
        erpUnitMapper.insert(unit);
        return unit;
    }

    @Override
    @AuditLog(action = "ERP_UNIT_UPDATE", entityType = "erp_unit", entityId = "{arg0}", detail = "code={arg1.code}")
    public ErpUnit update(Long id, ErpUnitUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpUnit unit = erpUnitMapper.selectOne(new QueryWrapper<ErpUnit>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (unit == null) {
            throw new IllegalArgumentException("单位不存在");
        }
        ErpUnit existing = erpUnitMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("单位编码已存在");
        }
        applyRequest(unit, request);
        if (request.enabled() != null) {
            unit.setEnabled(request.enabled());
        }
        unit.setUpdatedAt(Instant.now());
        erpUnitMapper.updateById(unit);
        return unit;
    }

    @Override
    @AuditLog(action = "ERP_UNIT_DELETE", entityType = "erp_unit", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpUnit unit = erpUnitMapper.selectOne(new QueryWrapper<ErpUnit>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (unit == null) {
            throw new IllegalArgumentException("单位不存在");
        }
        if (erpProductMapper.selectCount(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", tenantId)
            .eq("unit_id", id)) > 0) {
            throw new IllegalArgumentException("单位已被商品引用，不能删除");
        }
        erpUnitMapper.deleteById(id);
    }

    private QueryWrapper<ErpUnit> baseWrapper(String keyword, Boolean enabled) {
        QueryWrapper<ErpUnit> wrapper = new QueryWrapper<ErpUnit>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword)
                .or()
                .like("symbol", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        return wrapper;
    }

    private void applyRequest(ErpUnit unit, ErpUnitCreateRequest request) {
        unit.setCode(request.code());
        unit.setName(request.name());
        unit.setSymbol(request.symbol());
        unit.setPrecision(request.precision());
        unit.setRemark(request.remark());
    }

    private void applyRequest(ErpUnit unit, ErpUnitUpdateRequest request) {
        unit.setCode(request.code());
        unit.setName(request.name());
        unit.setSymbol(request.symbol());
        unit.setPrecision(request.precision());
        unit.setRemark(request.remark());
    }
}
