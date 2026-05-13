package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpVehicleSeriesCreateRequest;
import com.example.wms.dto.erp.ErpVehicleSeriesUpdateRequest;
import com.example.wms.entity.erp.ErpVehicleBrand;
import com.example.wms.entity.erp.ErpVehicleModel;
import com.example.wms.entity.erp.ErpVehicleSeries;
import com.example.wms.mapper.erp.ErpVehicleBrandMapper;
import com.example.wms.mapper.erp.ErpVehicleModelMapper;
import com.example.wms.mapper.erp.ErpVehicleSeriesMapper;
import com.example.wms.service.erp.ErpVehicleSeriesService;
import com.example.wms.service.erp.support.ErpMasterDataCodeGenerator;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 车型车系服务实现（ERP进销存）
@Service
public class ErpVehicleSeriesServiceImpl implements ErpVehicleSeriesService {
    private final ErpVehicleSeriesMapper erpVehicleSeriesMapper;
    private final ErpVehicleBrandMapper erpVehicleBrandMapper;
    private final ErpVehicleModelMapper erpVehicleModelMapper;
    private final ErpMasterDataCodeGenerator codeGenerator;

    public ErpVehicleSeriesServiceImpl(ErpVehicleSeriesMapper erpVehicleSeriesMapper,
                                       ErpVehicleBrandMapper erpVehicleBrandMapper,
                                       ErpVehicleModelMapper erpVehicleModelMapper,
                                       ErpMasterDataCodeGenerator codeGenerator) {
        this.erpVehicleSeriesMapper = erpVehicleSeriesMapper;
        this.erpVehicleBrandMapper = erpVehicleBrandMapper;
        this.erpVehicleModelMapper = erpVehicleModelMapper;
        this.codeGenerator = codeGenerator;
    }

    @Override
    public List<ErpVehicleSeries> listAll(String keyword, Boolean enabled, Long brandId) {
        QueryWrapper<ErpVehicleSeries> wrapper = baseWrapper(keyword, enabled, brandId);
        wrapper.orderByAsc("id");
        return erpVehicleSeriesMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpVehicleSeries> page(long page, long size, String keyword, Boolean enabled, Long brandId) {
        Page<ErpVehicleSeries> pageReq = Page.of(page, size);
        QueryWrapper<ErpVehicleSeries> wrapper = baseWrapper(keyword, enabled, brandId);
        wrapper.orderByAsc("id");
        Page<ErpVehicleSeries> result = erpVehicleSeriesMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpVehicleSeries getById(Long id) {
        ErpVehicleSeries series = erpVehicleSeriesMapper.selectOne(new QueryWrapper<ErpVehicleSeries>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (series == null) {
            throw new IllegalArgumentException("车系不存在");
        }
        return series;
    }

    @Override
    public String nextCode() {
        return codeGenerator.nextCode(
            "VEHICLE_SERIES",
            "erp.vehicle-series.code.prefix",
            "VS",
            "erp.vehicle-series.code.date-format",
            "erp.vehicle-series.code.seq-length"
        );
    }

    @Override
    @AuditLog(action = "ERP_VEHICLE_SERIES_CREATE", entityType = "erp_vehicle_series", entityId = "{result.id}", detail = "code={arg0.code}")
    public ErpVehicleSeries create(ErpVehicleSeriesCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ensureBrandExists(request.brandId());
        ErpVehicleSeries existing = erpVehicleSeriesMapper.findByCode(tenantId, request.brandId(), request.code());
        if (existing != null) {
            throw new IllegalArgumentException("车系编码已存在");
        }
        ErpVehicleSeries series = new ErpVehicleSeries();
        series.setTenantId(tenantId);
        applyRequest(series, request);
        series.setEnabled(request.enabled() == null || request.enabled());
        series.setCreatedAt(Instant.now());
        series.setUpdatedAt(Instant.now());
        erpVehicleSeriesMapper.insert(series);
        return series;
    }

    @Override
    @AuditLog(action = "ERP_VEHICLE_SERIES_UPDATE", entityType = "erp_vehicle_series", entityId = "{arg0}", detail = "code={arg1.code}")
    public ErpVehicleSeries update(Long id, ErpVehicleSeriesUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ensureBrandExists(request.brandId());
        ErpVehicleSeries series = erpVehicleSeriesMapper.selectOne(new QueryWrapper<ErpVehicleSeries>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (series == null) {
            throw new IllegalArgumentException("车系不存在");
        }
        ErpVehicleSeries existing = erpVehicleSeriesMapper.findByCode(tenantId, request.brandId(), request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("车系编码已存在");
        }
        applyRequest(series, request);
        if (request.enabled() != null) {
            series.setEnabled(request.enabled());
        }
        series.setUpdatedAt(Instant.now());
        erpVehicleSeriesMapper.updateById(series);
        return series;
    }

    @Override
    @AuditLog(action = "ERP_VEHICLE_SERIES_DELETE", entityType = "erp_vehicle_series", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpVehicleSeries series = erpVehicleSeriesMapper.selectOne(new QueryWrapper<ErpVehicleSeries>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (series == null) {
            throw new IllegalArgumentException("车系不存在");
        }
        if (erpVehicleModelMapper.selectCount(new QueryWrapper<ErpVehicleModel>()
            .eq("tenant_id", tenantId)
            .eq("series_id", id)) > 0) {
            throw new IllegalArgumentException("车系下存在车型，不能删除");
        }
        erpVehicleSeriesMapper.deleteById(id);
    }

    private QueryWrapper<ErpVehicleSeries> baseWrapper(String keyword, Boolean enabled, Long brandId) {
        QueryWrapper<ErpVehicleSeries> wrapper = new QueryWrapper<ErpVehicleSeries>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        if (brandId != null) {
            wrapper.eq("brand_id", brandId);
        }
        return wrapper;
    }

    private void applyRequest(ErpVehicleSeries series, ErpVehicleSeriesCreateRequest request) {
        series.setBrandId(request.brandId());
        series.setCode(request.code());
        series.setName(request.name());
        series.setRemark(request.remark());
    }

    private void applyRequest(ErpVehicleSeries series, ErpVehicleSeriesUpdateRequest request) {
        series.setBrandId(request.brandId());
        series.setCode(request.code());
        series.setName(request.name());
        series.setRemark(request.remark());
    }

    private void ensureBrandExists(Long brandId) {
        if (brandId == null) {
            throw new IllegalArgumentException("请选择品牌");
        }
        ErpVehicleBrand brand = erpVehicleBrandMapper.selectOne(new QueryWrapper<ErpVehicleBrand>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", brandId));
        if (brand == null) {
            throw new IllegalArgumentException("品牌不存在");
        }
    }
}
