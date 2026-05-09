package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.erp.ErpProductFitmentCreateRequest;
import com.example.wms.dto.erp.ErpProductFitmentUpdateRequest;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpProductFitment;
import com.example.wms.entity.erp.ErpVehicleModel;
import com.example.wms.mapper.erp.ErpProductFitmentMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpVehicleModelMapper;
import com.example.wms.service.erp.ErpProductFitmentService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 商品适配车型关系服务实现（ERP进销存）
@Service
public class ErpProductFitmentServiceImpl implements ErpProductFitmentService {
    private final ErpProductFitmentMapper erpProductFitmentMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpVehicleModelMapper erpVehicleModelMapper;

    public ErpProductFitmentServiceImpl(ErpProductFitmentMapper erpProductFitmentMapper,
                                        ErpProductMapper erpProductMapper,
                                        ErpVehicleModelMapper erpVehicleModelMapper) {
        this.erpProductFitmentMapper = erpProductFitmentMapper;
        this.erpProductMapper = erpProductMapper;
        this.erpVehicleModelMapper = erpVehicleModelMapper;
    }

    @Override
    public List<ErpProductFitment> listAll(Long productId, Long modelId) {
        QueryWrapper<ErpProductFitment> wrapper = new QueryWrapper<ErpProductFitment>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (productId != null) {
            wrapper.eq("product_id", productId);
        }
        if (modelId != null) {
            wrapper.eq("model_id", modelId);
        }
        wrapper.orderByAsc("id");
        return erpProductFitmentMapper.selectList(wrapper);
    }

    @Override
    public ErpProductFitment getById(Long id) {
        ErpProductFitment fitment = erpProductFitmentMapper.selectOne(new QueryWrapper<ErpProductFitment>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (fitment == null) {
            throw new IllegalArgumentException("适配关系不存在");
        }
        return fitment;
    }

    @Override
    @AuditLog(action = "ERP_PRODUCT_FITMENT_CREATE", entityType = "erp_product_fitment", entityId = "{result.id}", detail = "product={arg0.productId},model={arg0.modelId}")
    public ErpProductFitment create(ErpProductFitmentCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ensureProductExists(request.productId());
        ensureModelExists(request.modelId());
        ErpProductFitment existing = erpProductFitmentMapper.findByKey(tenantId, request.productId(), request.modelId());
        if (existing != null) {
            throw new IllegalArgumentException("适配关系已存在");
        }
        ErpProductFitment fitment = new ErpProductFitment();
        fitment.setTenantId(tenantId);
        fitment.setProductId(request.productId());
        fitment.setModelId(request.modelId());
        fitment.setRemark(request.remark());
        fitment.setCreatedAt(Instant.now());
        fitment.setUpdatedAt(Instant.now());
        erpProductFitmentMapper.insert(fitment);
        return fitment;
    }

    @Override
    @AuditLog(action = "ERP_PRODUCT_FITMENT_UPDATE", entityType = "erp_product_fitment", entityId = "{arg0}")
    public ErpProductFitment update(Long id, ErpProductFitmentUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpProductFitment fitment = erpProductFitmentMapper.selectOne(new QueryWrapper<ErpProductFitment>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (fitment == null) {
            throw new IllegalArgumentException("适配关系不存在");
        }
        fitment.setRemark(request.remark());
        fitment.setUpdatedAt(Instant.now());
        erpProductFitmentMapper.updateById(fitment);
        return fitment;
    }

    @Override
    @AuditLog(action = "ERP_PRODUCT_FITMENT_DELETE", entityType = "erp_product_fitment", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpProductFitment fitment = erpProductFitmentMapper.selectOne(new QueryWrapper<ErpProductFitment>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (fitment == null) {
            throw new IllegalArgumentException("适配关系不存在");
        }
        erpProductFitmentMapper.deleteById(id);
    }

    private void ensureProductExists(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("请选择商品");
        }
        ErpProduct product = erpProductMapper.selectOne(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", productId));
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
    }

    private void ensureModelExists(Long modelId) {
        if (modelId == null) {
            throw new IllegalArgumentException("请选择车型");
        }
        ErpVehicleModel model = erpVehicleModelMapper.selectOne(new QueryWrapper<ErpVehicleModel>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", modelId));
        if (model == null) {
            throw new IllegalArgumentException("车型不存在");
        }
    }
}
