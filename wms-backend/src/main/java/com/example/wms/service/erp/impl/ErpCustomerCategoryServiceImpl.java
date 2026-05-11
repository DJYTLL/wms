package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCustomerCategoryCreateRequest;
import com.example.wms.dto.erp.ErpCustomerCategoryUpdateRequest;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpCustomerCategory;
import com.example.wms.entity.erp.ErpProductPrice;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpCustomerCategoryMapper;
import com.example.wms.mapper.erp.ErpProductPriceMapper;
import com.example.wms.service.erp.ErpCustomerCategoryService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 客户类别服务实现（ERP进销存）
@Service
public class ErpCustomerCategoryServiceImpl implements ErpCustomerCategoryService {
    private final ErpCustomerCategoryMapper erpCustomerCategoryMapper;
    private final ErpCustomerMapper erpCustomerMapper;
    private final ErpProductPriceMapper erpProductPriceMapper;

    public ErpCustomerCategoryServiceImpl(ErpCustomerCategoryMapper erpCustomerCategoryMapper,
                                          ErpCustomerMapper erpCustomerMapper,
                                          ErpProductPriceMapper erpProductPriceMapper) {
        this.erpCustomerCategoryMapper = erpCustomerCategoryMapper;
        this.erpCustomerMapper = erpCustomerMapper;
        this.erpProductPriceMapper = erpProductPriceMapper;
    }

    @Override
    public List<ErpCustomerCategory> listAll(String keyword, Boolean enabled) {
        QueryWrapper<ErpCustomerCategory> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("sort_no").orderByAsc("id");
        return erpCustomerCategoryMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpCustomerCategory> page(long page, long size, String keyword, Boolean enabled) {
        Page<ErpCustomerCategory> pageReq = Page.of(page, size);
        QueryWrapper<ErpCustomerCategory> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("sort_no").orderByAsc("id");
        Page<ErpCustomerCategory> result = erpCustomerCategoryMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpCustomerCategory getById(Long id) {
        ErpCustomerCategory category = erpCustomerCategoryMapper.selectOne(new QueryWrapper<ErpCustomerCategory>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (category == null) {
            throw new IllegalArgumentException("客户类别不存在");
        }
        return category;
    }

    @Override
    @AuditLog(action = "ERP_CUSTOMER_CATEGORY_CREATE", entityType = "erp_customer_category", entityId = "{result.id}", detail = "code={arg0.code}")
    public ErpCustomerCategory create(ErpCustomerCategoryCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpCustomerCategory existing = erpCustomerCategoryMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("客户类别编码已存在");
        }
        ErpCustomerCategory category = new ErpCustomerCategory();
        category.setTenantId(tenantId);
        applyRequest(category, request);
        category.setEnabled(request.enabled() == null || request.enabled());
        category.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        category.setCreatedAt(Instant.now());
        category.setUpdatedAt(Instant.now());
        erpCustomerCategoryMapper.insert(category);
        handleDefault(tenantId, category.getId(), request.isDefault());
        return category;
    }

    @Override
    @AuditLog(action = "ERP_CUSTOMER_CATEGORY_UPDATE", entityType = "erp_customer_category", entityId = "{arg0}", detail = "code={arg1.code}")
    public ErpCustomerCategory update(Long id, ErpCustomerCategoryUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpCustomerCategory category = erpCustomerCategoryMapper.selectOne(new QueryWrapper<ErpCustomerCategory>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (category == null) {
            throw new IllegalArgumentException("客户类别不存在");
        }
        ErpCustomerCategory existing = erpCustomerCategoryMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("客户类别编码已存在");
        }
        applyRequest(category, request);
        if (request.enabled() != null) {
            category.setEnabled(request.enabled());
        }
        if (request.isDefault() != null) {
            category.setIsDefault(request.isDefault());
        }
        category.setUpdatedAt(Instant.now());
        erpCustomerCategoryMapper.updateById(category);
        handleDefault(tenantId, category.getId(), request.isDefault());
        return category;
    }

    @Override
    @AuditLog(action = "ERP_CUSTOMER_CATEGORY_DELETE", entityType = "erp_customer_category", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpCustomerCategory category = erpCustomerCategoryMapper.selectOne(new QueryWrapper<ErpCustomerCategory>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (category == null) {
            throw new IllegalArgumentException("客户类别不存在");
        }
        if (erpCustomerMapper.selectCount(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .eq("category_id", id)) > 0) {
            throw new IllegalArgumentException("客户类别已被客户引用，不能删除");
        }
        if (erpProductPriceMapper.selectCount(new QueryWrapper<ErpProductPrice>()
            .eq("tenant_id", tenantId)
            .eq("customer_category_id", id)) > 0) {
            throw new IllegalArgumentException("客户类别已被商品价格引用，不能删除");
        }
        erpCustomerCategoryMapper.deleteById(id);
    }

    private QueryWrapper<ErpCustomerCategory> baseWrapper(String keyword, Boolean enabled) {
        QueryWrapper<ErpCustomerCategory> wrapper = new QueryWrapper<ErpCustomerCategory>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword)
                .or()
                .like("description", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        return wrapper;
    }

    private void applyRequest(ErpCustomerCategory category, ErpCustomerCategoryCreateRequest request) {
        category.setCode(request.code());
        category.setName(request.name());
        category.setDescription(request.description());
        category.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        category.setRemark(request.remark());
    }

    private void applyRequest(ErpCustomerCategory category, ErpCustomerCategoryUpdateRequest request) {
        category.setCode(request.code());
        category.setName(request.name());
        category.setDescription(request.description());
        category.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        category.setRemark(request.remark());
    }

    private void handleDefault(Long tenantId, Long categoryId, Boolean isDefault) {
        if (Boolean.TRUE.equals(isDefault)) {
            erpCustomerCategoryMapper.clearDefault(tenantId, categoryId);
        }
    }
}
