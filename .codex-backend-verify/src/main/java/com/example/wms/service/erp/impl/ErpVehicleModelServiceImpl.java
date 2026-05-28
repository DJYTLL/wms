package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpVehicleModelCreateRequest;
import com.example.wms.dto.erp.ErpVehicleModelUpdateRequest;
import com.example.wms.entity.erp.ErpVehicleModel;
import com.example.wms.entity.erp.ErpProductFitment;
import com.example.wms.entity.erp.ErpVehicleSeries;
import com.example.wms.mapper.erp.ErpProductFitmentMapper;
import com.example.wms.mapper.erp.ErpVehicleModelMapper;
import com.example.wms.mapper.erp.ErpVehicleSeriesMapper;
import com.example.wms.service.erp.ErpVehicleModelService;
import com.example.wms.service.erp.support.ErpMasterDataCodeGenerator;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

// 车型服务实现（ERP进销存）
@Service
public class ErpVehicleModelServiceImpl implements ErpVehicleModelService {
    private final ErpVehicleModelMapper erpVehicleModelMapper;
    private final ErpVehicleSeriesMapper erpVehicleSeriesMapper;
    private final ErpProductFitmentMapper erpProductFitmentMapper;
    private final ErpMasterDataCodeGenerator codeGenerator;

    public ErpVehicleModelServiceImpl(ErpVehicleModelMapper erpVehicleModelMapper,
                                      ErpVehicleSeriesMapper erpVehicleSeriesMapper,
                                      ErpProductFitmentMapper erpProductFitmentMapper,
                                      ErpMasterDataCodeGenerator codeGenerator) {
        this.erpVehicleModelMapper = erpVehicleModelMapper;
        this.erpVehicleSeriesMapper = erpVehicleSeriesMapper;
        this.erpProductFitmentMapper = erpProductFitmentMapper;
        this.codeGenerator = codeGenerator;
    }

    @Override
    public List<ErpVehicleModel> listAll(String keyword, Boolean enabled, Long seriesId) {
        QueryWrapper<ErpVehicleModel> wrapper = baseWrapper(keyword, enabled, seriesId);
        wrapper.orderByAsc("id");
        return erpVehicleModelMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpVehicleModel> page(long page, long size, String keyword, Boolean enabled, Long seriesId) {
        Page<ErpVehicleModel> pageReq = Page.of(page, size);
        QueryWrapper<ErpVehicleModel> wrapper = baseWrapper(keyword, enabled, seriesId);
        wrapper.orderByAsc("id");
        Page<ErpVehicleModel> result = erpVehicleModelMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpVehicleModel getById(Long id) {
        ErpVehicleModel model = erpVehicleModelMapper.selectOne(new QueryWrapper<ErpVehicleModel>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (model == null) {
            throw new IllegalArgumentException("车型不存在");
        }
        return model;
    }

    @Override
    public String nextCode() {
        return codeGenerator.nextCode(
            "VEHICLE_MODEL",
            "erp.vehicle-model.code.prefix",
            "VM",
            "erp.vehicle-model.code.date-format",
            "erp.vehicle-model.code.seq-length"
        );
    }

    @Override
    @AuditLog(action = "ERP_VEHICLE_MODEL_CREATE", entityType = "erp_vehicle_model", entityId = "{result.id}", detail = "code={arg0.code}")
@Transactional
    public ErpVehicleModel create(ErpVehicleModelCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ensureSeriesExists(request.seriesId());
        ErpVehicleModel existing = erpVehicleModelMapper.findByCode(tenantId, request.seriesId(), request.code());
        if (existing != null) {
            throw new IllegalArgumentException("车型编码已存在");
        }
        ErpVehicleModel model = new ErpVehicleModel();
        model.setTenantId(tenantId);
        applyRequest(model, request);
        model.setEnabled(request.enabled() == null || request.enabled());
        model.setCreatedAt(Instant.now());
        model.setUpdatedAt(Instant.now());
        erpVehicleModelMapper.insert(model);
        return model;
    }

    @Override
    @AuditLog(action = "ERP_VEHICLE_MODEL_UPDATE", entityType = "erp_vehicle_model", entityId = "{arg0}", detail = "code={arg1.code}")
@Transactional
    public ErpVehicleModel update(Long id, ErpVehicleModelUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ensureSeriesExists(request.seriesId());
        ErpVehicleModel model = erpVehicleModelMapper.selectOne(new QueryWrapper<ErpVehicleModel>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (model == null) {
            throw new IllegalArgumentException("车型不存在");
        }
        ErpVehicleModel existing = erpVehicleModelMapper.findByCode(tenantId, request.seriesId(), request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("车型编码已存在");
        }
        applyRequest(model, request);
        if (request.enabled() != null) {
            model.setEnabled(request.enabled());
        }
        model.setUpdatedAt(Instant.now());
        erpVehicleModelMapper.updateById(model);
        return model;
    }

    @Override
    @AuditLog(action = "ERP_VEHICLE_MODEL_DELETE", entityType = "erp_vehicle_model", entityId = "{arg0}")
@Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpVehicleModel model = erpVehicleModelMapper.selectOne(new QueryWrapper<ErpVehicleModel>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (model == null) {
            throw new IllegalArgumentException("车型不存在");
        }
        if (erpProductFitmentMapper.selectCount(new QueryWrapper<ErpProductFitment>()
            .eq("tenant_id", tenantId)
            .eq("model_id", id)) > 0) {
            throw new IllegalArgumentException("车型已被商品适配关系引用，不能删除");
        }
        erpVehicleModelMapper.deleteById(id);
    }

    private QueryWrapper<ErpVehicleModel> baseWrapper(String keyword, Boolean enabled, Long seriesId) {
        QueryWrapper<ErpVehicleModel> wrapper = new QueryWrapper<ErpVehicleModel>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        if (seriesId != null) {
            wrapper.eq("series_id", seriesId);
        }
        return wrapper;
    }

    private void applyRequest(ErpVehicleModel model, ErpVehicleModelCreateRequest request) {
        model.setSeriesId(request.seriesId());
        model.setCode(request.code());
        model.setName(request.name());
        model.setYearFrom(request.yearFrom());
        model.setYearTo(request.yearTo());
        model.setDisplacement(request.displacement());
        model.setEngine(request.engine());
        model.setRemark(request.remark());
    }

    private void applyRequest(ErpVehicleModel model, ErpVehicleModelUpdateRequest request) {
        model.setSeriesId(request.seriesId());
        model.setCode(request.code());
        model.setName(request.name());
        model.setYearFrom(request.yearFrom());
        model.setYearTo(request.yearTo());
        model.setDisplacement(request.displacement());
        model.setEngine(request.engine());
        model.setRemark(request.remark());
    }

    private void ensureSeriesExists(Long seriesId) {
        if (seriesId == null) {
            throw new IllegalArgumentException("请选择车系");
        }
        ErpVehicleSeries series = erpVehicleSeriesMapper.selectOne(new QueryWrapper<ErpVehicleSeries>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", seriesId));
        if (series == null) {
            throw new IllegalArgumentException("车系不存在");
        }
    }
}
