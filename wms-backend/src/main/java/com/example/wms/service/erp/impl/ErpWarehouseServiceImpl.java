package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpWarehouseCreateRequest;
import com.example.wms.dto.erp.ErpWarehouseUpdateRequest;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.ErpWarehouseService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 仓库服务实现（ERP进销存）
@Service
public class ErpWarehouseServiceImpl implements ErpWarehouseService {
    private final ErpWarehouseMapper erpWarehouseMapper;

    public ErpWarehouseServiceImpl(ErpWarehouseMapper erpWarehouseMapper) {
        this.erpWarehouseMapper = erpWarehouseMapper;
    }

    @Override
    public List<ErpWarehouse> listAll(String keyword, Boolean enabled) {
        QueryWrapper<ErpWarehouse> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("id");
        return erpWarehouseMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpWarehouse> page(long page, long size, String keyword, Boolean enabled) {
        Page<ErpWarehouse> pageReq = Page.of(page, size);
        QueryWrapper<ErpWarehouse> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("id");
        Page<ErpWarehouse> result = erpWarehouseMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpWarehouse getById(Long id) {
        ErpWarehouse warehouse = erpWarehouseMapper.selectOne(new QueryWrapper<ErpWarehouse>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        return warehouse;
    }

    @Override
    @AuditLog(action = "ERP_WAREHOUSE_CREATE", entityType = "erp_warehouse", entityId = "{result.id}", detail = "code={arg0.code}")
    public ErpWarehouse create(ErpWarehouseCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpWarehouse existing = erpWarehouseMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("仓库编码已存在");
        }
        ErpWarehouse warehouse = new ErpWarehouse();
        warehouse.setTenantId(tenantId);
        applyRequest(warehouse, request);
        warehouse.setEnabled(request.enabled() == null || request.enabled());
        warehouse.setCreatedAt(Instant.now());
        warehouse.setUpdatedAt(Instant.now());
        erpWarehouseMapper.insert(warehouse);
        return warehouse;
    }

    @Override
    @AuditLog(action = "ERP_WAREHOUSE_UPDATE", entityType = "erp_warehouse", entityId = "{arg0}", detail = "code={arg1.code}")
    public ErpWarehouse update(Long id, ErpWarehouseUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpWarehouse warehouse = erpWarehouseMapper.selectOne(new QueryWrapper<ErpWarehouse>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        ErpWarehouse existing = erpWarehouseMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("仓库编码已存在");
        }
        applyRequest(warehouse, request);
        if (request.enabled() != null) {
            warehouse.setEnabled(request.enabled());
        }
        warehouse.setUpdatedAt(Instant.now());
        erpWarehouseMapper.updateById(warehouse);
        return warehouse;
    }

    @Override
    @AuditLog(action = "ERP_WAREHOUSE_DELETE", entityType = "erp_warehouse", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpWarehouse warehouse = erpWarehouseMapper.selectOne(new QueryWrapper<ErpWarehouse>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        erpWarehouseMapper.deleteById(id);
    }

    private QueryWrapper<ErpWarehouse> baseWrapper(String keyword, Boolean enabled) {
        QueryWrapper<ErpWarehouse> wrapper = new QueryWrapper<ErpWarehouse>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword)
                .or()
                .like("manager", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        return wrapper;
    }

    private void applyRequest(ErpWarehouse warehouse, ErpWarehouseCreateRequest request) {
        warehouse.setCode(request.code());
        warehouse.setName(request.name());
        warehouse.setAddress(request.address());
        warehouse.setManager(request.manager());
        warehouse.setPhone(request.phone());
        warehouse.setRemark(request.remark());
    }

    private void applyRequest(ErpWarehouse warehouse, ErpWarehouseUpdateRequest request) {
        warehouse.setCode(request.code());
        warehouse.setName(request.name());
        warehouse.setAddress(request.address());
        warehouse.setManager(request.manager());
        warehouse.setPhone(request.phone());
        warehouse.setRemark(request.remark());
    }
}
