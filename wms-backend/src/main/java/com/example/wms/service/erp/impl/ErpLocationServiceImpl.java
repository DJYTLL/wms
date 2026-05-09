package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpLocationCreateRequest;
import com.example.wms.dto.erp.ErpLocationUpdateRequest;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.ErpLocationService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 库位服务实现（ERP进销存）
@Service
public class ErpLocationServiceImpl implements ErpLocationService {
    private final ErpLocationMapper erpLocationMapper;
    private final ErpWarehouseMapper erpWarehouseMapper;

    public ErpLocationServiceImpl(ErpLocationMapper erpLocationMapper, ErpWarehouseMapper erpWarehouseMapper) {
        this.erpLocationMapper = erpLocationMapper;
        this.erpWarehouseMapper = erpWarehouseMapper;
    }

    @Override
    public List<ErpLocation> listAll(String keyword, Boolean enabled, Long warehouseId) {
        QueryWrapper<ErpLocation> wrapper = baseWrapper(keyword, enabled, warehouseId);
        wrapper.orderByAsc("id");
        return erpLocationMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpLocation> page(long page, long size, String keyword, Boolean enabled, Long warehouseId) {
        Page<ErpLocation> pageReq = Page.of(page, size);
        QueryWrapper<ErpLocation> wrapper = baseWrapper(keyword, enabled, warehouseId);
        wrapper.orderByAsc("id");
        Page<ErpLocation> result = erpLocationMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpLocation getById(Long id) {
        ErpLocation location = erpLocationMapper.selectOne(new QueryWrapper<ErpLocation>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (location == null) {
            throw new IllegalArgumentException("库位不存在");
        }
        return location;
    }

    @Override
    @AuditLog(action = "ERP_LOCATION_CREATE", entityType = "erp_location", entityId = "{result.id}", detail = "code={arg0.code}")
    public ErpLocation create(ErpLocationCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ensureWarehouseExists(tenantId, request.warehouseId());
        ErpLocation existing = erpLocationMapper.findByCode(tenantId, request.warehouseId(), request.code());
        if (existing != null) {
            throw new IllegalArgumentException("库位编码已存在");
        }
        ErpLocation location = new ErpLocation();
        location.setTenantId(tenantId);
        applyRequest(location, request);
        location.setEnabled(request.enabled() == null || request.enabled());
        location.setCreatedAt(Instant.now());
        location.setUpdatedAt(Instant.now());
        erpLocationMapper.insert(location);
        return location;
    }

    @Override
    @AuditLog(action = "ERP_LOCATION_UPDATE", entityType = "erp_location", entityId = "{arg0}", detail = "code={arg1.code}")
    public ErpLocation update(Long id, ErpLocationUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpLocation location = erpLocationMapper.selectOne(new QueryWrapper<ErpLocation>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (location == null) {
            throw new IllegalArgumentException("库位不存在");
        }
        ensureWarehouseExists(tenantId, request.warehouseId());
        ErpLocation existing = erpLocationMapper.findByCode(tenantId, request.warehouseId(), request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("库位编码已存在");
        }
        applyRequest(location, request);
        if (request.enabled() != null) {
            location.setEnabled(request.enabled());
        }
        location.setUpdatedAt(Instant.now());
        erpLocationMapper.updateById(location);
        return location;
    }

    @Override
    @AuditLog(action = "ERP_LOCATION_DELETE", entityType = "erp_location", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpLocation location = erpLocationMapper.selectOne(new QueryWrapper<ErpLocation>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (location == null) {
            throw new IllegalArgumentException("库位不存在");
        }
        erpLocationMapper.deleteById(id);
    }

    private QueryWrapper<ErpLocation> baseWrapper(String keyword, Boolean enabled, Long warehouseId) {
        QueryWrapper<ErpLocation> wrapper = new QueryWrapper<ErpLocation>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        if (warehouseId != null) {
            wrapper.eq("warehouse_id", warehouseId);
        }
        return wrapper;
    }

    private void ensureWarehouseExists(Long tenantId, Long warehouseId) {
        ErpWarehouse warehouse = erpWarehouseMapper.selectOne(new QueryWrapper<ErpWarehouse>()
            .eq("tenant_id", tenantId)
            .eq("id", warehouseId));
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
    }

    private void applyRequest(ErpLocation location, ErpLocationCreateRequest request) {
        location.setCode(request.code());
        location.setName(request.name());
        location.setWarehouseId(request.warehouseId());
        location.setRemark(request.remark());
    }

    private void applyRequest(ErpLocation location, ErpLocationUpdateRequest request) {
        location.setCode(request.code());
        location.setName(request.name());
        location.setWarehouseId(request.warehouseId());
        location.setRemark(request.remark());
    }
}
