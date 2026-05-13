package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCategoryCreateRequest;
import com.example.wms.dto.erp.ErpCategoryUpdateRequest;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpCategory;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpCategoryMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.service.erp.ErpCategoryService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

// 分类服务实现（ERP进销存）
@Service
public class ErpCategoryServiceImpl implements ErpCategoryService {
    private static final String CATEGORY_CODE_TYPE = "CATEGORY";

    private final ErpCategoryMapper erpCategoryMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;

    public ErpCategoryServiceImpl(ErpCategoryMapper erpCategoryMapper,
                                  ErpProductMapper erpProductMapper,
                                  ErpOrderSequenceMapper erpOrderSequenceMapper,
                                  SystemConfigMapper systemConfigMapper) {
        this.erpCategoryMapper = erpCategoryMapper;
        this.erpProductMapper = erpProductMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
    }

    @Override
    public List<ErpCategory> listAll(String keyword, Boolean enabled) {
        QueryWrapper<ErpCategory> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("sort_no", "id");
        return erpCategoryMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpCategory> page(long page, long size, String keyword, Boolean enabled) {
        Page<ErpCategory> pageReq = Page.of(page, size);
        QueryWrapper<ErpCategory> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("sort_no", "id");
        Page<ErpCategory> result = erpCategoryMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpCategory getById(Long id) {
        ErpCategory category = erpCategoryMapper.selectOne(new QueryWrapper<ErpCategory>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        return category;
    }

    @Override
    public String nextCode() {
        Long tenantId = TenantContext.requireTenantId();
        return generateCategoryCode(tenantId);
    }

    @Override
    @AuditLog(action = "ERP_CATEGORY_CREATE", entityType = "erp_category", entityId = "{result.id}", detail = "code={arg0.code}")
    public ErpCategory create(ErpCategoryCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpCategory existing = erpCategoryMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("分类编码已存在");
        }
        validateParentRelation(tenantId, null, request.parentId());
        ErpCategory category = new ErpCategory();
        category.setTenantId(tenantId);
        applyRequest(category, request);
        category.setEnabled(request.enabled() == null || request.enabled());
        category.setCreatedAt(Instant.now());
        category.setUpdatedAt(Instant.now());
        erpCategoryMapper.insert(category);
        return category;
    }

    @Override
    @AuditLog(action = "ERP_CATEGORY_UPDATE", entityType = "erp_category", entityId = "{arg0}", detail = "code={arg1.code}")
    public ErpCategory update(Long id, ErpCategoryUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpCategory category = erpCategoryMapper.selectOne(new QueryWrapper<ErpCategory>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        ErpCategory existing = erpCategoryMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("分类编码已存在");
        }
        validateParentRelation(tenantId, id, request.parentId());
        applyRequest(category, request);
        if (request.enabled() != null) {
            category.setEnabled(request.enabled());
        }
        category.setUpdatedAt(Instant.now());
        erpCategoryMapper.updateById(category);
        return category;
    }

    @Override
    @AuditLog(action = "ERP_CATEGORY_DELETE", entityType = "erp_category", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpCategory category = erpCategoryMapper.selectOne(new QueryWrapper<ErpCategory>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        long childCount = erpCategoryMapper.selectCount(new QueryWrapper<ErpCategory>()
            .eq("tenant_id", tenantId)
            .eq("parent_id", id));
        if (childCount > 0) {
            throw new IllegalArgumentException("分类下存在子分类，不能删除");
        }
        long productCount = erpProductMapper.selectCount(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", tenantId)
            .eq("category_id", id));
        if (productCount > 0) {
            throw new IllegalArgumentException("分类已被商品引用，不能删除");
        }
        erpCategoryMapper.deleteById(id);
    }

    private void validateParentRelation(Long tenantId, Long currentId, Long parentId) {
        if (parentId == null) {
            return;
        }
        if (currentId != null && currentId.equals(parentId)) {
            throw new IllegalArgumentException("父级分类不能选择自己");
        }
        ErpCategory parent = erpCategoryMapper.selectOne(new QueryWrapper<ErpCategory>()
            .eq("tenant_id", tenantId)
            .eq("id", parentId));
        if (parent == null) {
            throw new IllegalArgumentException("父级分类不存在");
        }
        Long cursor = parent.getParentId();
        while (cursor != null) {
            if (currentId != null && currentId.equals(cursor)) {
                throw new IllegalArgumentException("父级分类不能形成循环关系");
            }
            ErpCategory ancestor = erpCategoryMapper.selectOne(new QueryWrapper<ErpCategory>()
                .eq("tenant_id", tenantId)
                .eq("id", cursor));
            if (ancestor == null) {
                break;
            }
            cursor = ancestor.getParentId();
        }
    }

    private QueryWrapper<ErpCategory> baseWrapper(String keyword, Boolean enabled) {
        QueryWrapper<ErpCategory> wrapper = new QueryWrapper<ErpCategory>()
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

    private void applyRequest(ErpCategory category, ErpCategoryCreateRequest request) {
        category.setCode(request.code());
        category.setName(request.name());
        category.setParentId(request.parentId());
        category.setLevel(request.level());
        category.setSortNo(request.sortNo());
        category.setRemark(request.remark());
    }

    private void applyRequest(ErpCategory category, ErpCategoryUpdateRequest request) {
        category.setCode(request.code());
        category.setName(request.name());
        category.setParentId(request.parentId());
        category.setLevel(request.level());
        category.setSortNo(request.sortNo());
        category.setRemark(request.remark());
    }

    private String generateCategoryCode(Long tenantId) {
        String prefix = readConfig("erp.category.code.prefix", "CA");
        String dateFormat = readConfig("erp.category.code.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.category.code.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, CATEGORY_CODE_TYPE, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, CATEGORY_CODE_TYPE, dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private String readConfig(String key, String fallback) {
        SystemConfig config = systemConfigMapper.findByKey(key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return fallback;
        }
        return config.getConfigValue().trim();
    }

    private int readIntConfig(String key, int fallback) {
        String value = readConfig(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
