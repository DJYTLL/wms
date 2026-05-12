package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpVehicleBrandCreateRequest;
import com.example.wms.dto.erp.ErpVehicleBrandUpdateRequest;
import com.example.wms.entity.erp.ErpVehicleBrand;
import com.example.wms.entity.erp.ErpVehicleSeries;
import com.example.wms.mapper.erp.ErpVehicleBrandMapper;
import com.example.wms.mapper.erp.ErpVehicleSeriesMapper;
import com.example.wms.service.erp.ErpVehicleBrandService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 车型品牌服务实现（ERP进销存）
@Service
public class ErpVehicleBrandServiceImpl implements ErpVehicleBrandService {
    private final ErpVehicleBrandMapper erpVehicleBrandMapper;
    private final ErpVehicleSeriesMapper erpVehicleSeriesMapper;

    public ErpVehicleBrandServiceImpl(ErpVehicleBrandMapper erpVehicleBrandMapper,
                                      ErpVehicleSeriesMapper erpVehicleSeriesMapper) {
        this.erpVehicleBrandMapper = erpVehicleBrandMapper;
        this.erpVehicleSeriesMapper = erpVehicleSeriesMapper;
    }

    @Override
    public List<ErpVehicleBrand> listAll(String keyword, Boolean enabled) {
        QueryWrapper<ErpVehicleBrand> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("id");
        return erpVehicleBrandMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpVehicleBrand> page(long page, long size, String keyword, Boolean enabled) {
        Page<ErpVehicleBrand> pageReq = Page.of(page, size);
        QueryWrapper<ErpVehicleBrand> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("id");
        Page<ErpVehicleBrand> result = erpVehicleBrandMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpVehicleBrand getById(Long id) {
        ErpVehicleBrand brand = erpVehicleBrandMapper.selectOne(new QueryWrapper<ErpVehicleBrand>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (brand == null) {
            throw new IllegalArgumentException("品牌不存在");
        }
        return brand;
    }

    @Override
    @AuditLog(action = "ERP_VEHICLE_BRAND_CREATE", entityType = "erp_vehicle_brand", entityId = "{result.id}", detail = "code={arg0.code}")
    public ErpVehicleBrand create(ErpVehicleBrandCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpVehicleBrand existing = erpVehicleBrandMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("品牌编码已存在");
        }
        ErpVehicleBrand brand = new ErpVehicleBrand();
        brand.setTenantId(tenantId);
        applyRequest(brand, request);
        brand.setEnabled(request.enabled() == null || request.enabled());
        brand.setCreatedAt(Instant.now());
        brand.setUpdatedAt(Instant.now());
        erpVehicleBrandMapper.insert(brand);
        return brand;
    }

    @Override
    @AuditLog(action = "ERP_VEHICLE_BRAND_UPDATE", entityType = "erp_vehicle_brand", entityId = "{arg0}", detail = "code={arg1.code}")
    public ErpVehicleBrand update(Long id, ErpVehicleBrandUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpVehicleBrand brand = erpVehicleBrandMapper.selectOne(new QueryWrapper<ErpVehicleBrand>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (brand == null) {
            throw new IllegalArgumentException("品牌不存在");
        }
        ErpVehicleBrand existing = erpVehicleBrandMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("品牌编码已存在");
        }
        applyRequest(brand, request);
        if (request.enabled() != null) {
            brand.setEnabled(request.enabled());
        }
        brand.setUpdatedAt(Instant.now());
        erpVehicleBrandMapper.updateById(brand);
        return brand;
    }

    @Override
    @AuditLog(action = "ERP_VEHICLE_BRAND_DELETE", entityType = "erp_vehicle_brand", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpVehicleBrand brand = erpVehicleBrandMapper.selectOne(new QueryWrapper<ErpVehicleBrand>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (brand == null) {
            throw new IllegalArgumentException("品牌不存在");
        }
        if (erpVehicleSeriesMapper.selectCount(new QueryWrapper<ErpVehicleSeries>()
            .eq("tenant_id", tenantId)
            .eq("brand_id", id)) > 0) {
            throw new IllegalArgumentException("品牌下存在车系，不能删除");
        }
        erpVehicleBrandMapper.deleteById(id);
    }

    private QueryWrapper<ErpVehicleBrand> baseWrapper(String keyword, Boolean enabled) {
        QueryWrapper<ErpVehicleBrand> wrapper = new QueryWrapper<ErpVehicleBrand>()
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

    private void applyRequest(ErpVehicleBrand brand, ErpVehicleBrandCreateRequest request) {
        brand.setCode(request.code());
        brand.setName(request.name());
        brand.setRemark(request.remark());
    }

    private void applyRequest(ErpVehicleBrand brand, ErpVehicleBrandUpdateRequest request) {
        brand.setCode(request.code());
        brand.setName(request.name());
        brand.setRemark(request.remark());
    }
}
