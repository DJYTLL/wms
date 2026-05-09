package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPrintTemplateCreateRequest;
import com.example.wms.dto.erp.ErpPrintTemplateUpdateRequest;
import com.example.wms.entity.erp.ErpPrintTemplate;
import com.example.wms.mapper.erp.ErpPrintTemplateMapper;
import com.example.wms.service.erp.ErpPrintTemplateService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

// 打印模板服务实现（ERP进销存）
@Service
public class ErpPrintTemplateServiceImpl implements ErpPrintTemplateService {
    private static final Set<String> DOC_TYPES = Set.of(
        "SALE_ORDER",
        "PURCHASE_ORDER",
        "SALE_RETURN",
        "PURCHASE_RETURN",
        "RECEIPT",
        "PAYMENT",
        "ACCOUNTS_RECEIVABLE",
        "ACCOUNTS_PAYABLE",
        "STOCK_COUNT",
        "STOCK_INIT"
    );

    private final ErpPrintTemplateMapper erpPrintTemplateMapper;

    public ErpPrintTemplateServiceImpl(ErpPrintTemplateMapper erpPrintTemplateMapper) {
        this.erpPrintTemplateMapper = erpPrintTemplateMapper;
    }

    @Override
    public List<ErpPrintTemplate> listAll(String keyword, String docType, Boolean enabled) {
        QueryWrapper<ErpPrintTemplate> wrapper = baseWrapper(keyword, docType, enabled);
        wrapper.orderByAsc("sort_no").orderByDesc("updated_at");
        return erpPrintTemplateMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpPrintTemplate> page(long page, long size, String keyword, String docType, Boolean enabled) {
        Page<ErpPrintTemplate> pageReq = Page.of(page, size);
        QueryWrapper<ErpPrintTemplate> wrapper = baseWrapper(keyword, docType, enabled);
        wrapper.orderByAsc("sort_no").orderByDesc("updated_at");
        Page<ErpPrintTemplate> result = erpPrintTemplateMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpPrintTemplate getById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPrintTemplate template = erpPrintTemplateMapper.selectOne(new QueryWrapper<ErpPrintTemplate>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (template == null) {
            throw new IllegalArgumentException("打印模板不存在");
        }
        return template;
    }

    @Override
    public ErpPrintTemplate getDefaultByDocType(String docType) {
        String normalized = normalizeDocType(docType);
        Long tenantId = TenantContext.requireTenantId();
        return erpPrintTemplateMapper.findDefault(tenantId, normalized);
    }

    @Override
    public ErpPrintTemplate create(ErpPrintTemplateCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String normalizedDocType = normalizeDocType(request.docType());
        ErpPrintTemplate existing = erpPrintTemplateMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("模板编码已存在");
        }
        ErpPrintTemplate template = new ErpPrintTemplate();
        template.setTenantId(tenantId);
        applyRequest(template, request, normalizedDocType);
        template.setCreatedAt(Instant.now());
        template.setUpdatedAt(Instant.now());
        erpPrintTemplateMapper.insert(template);
        applyDefaultFlag(tenantId, normalizedDocType, template);
        return template;
    }

    @Override
    public ErpPrintTemplate update(Long id, ErpPrintTemplateUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPrintTemplate template = erpPrintTemplateMapper.selectOne(new QueryWrapper<ErpPrintTemplate>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (template == null) {
            throw new IllegalArgumentException("打印模板不存在");
        }
        String normalizedDocType = normalizeDocType(request.docType());
        ErpPrintTemplate existing = erpPrintTemplateMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(template.getId())) {
            throw new IllegalArgumentException("模板编码已存在");
        }
        applyRequest(template, request, normalizedDocType);
        template.setUpdatedAt(Instant.now());
        erpPrintTemplateMapper.updateById(template);
        applyDefaultFlag(tenantId, normalizedDocType, template);
        return template;
    }

    @Override
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPrintTemplate template = erpPrintTemplateMapper.selectOne(new QueryWrapper<ErpPrintTemplate>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (template == null) {
            return;
        }
        erpPrintTemplateMapper.deleteById(id);
    }

    @Override
    public ErpPrintTemplate setDefault(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPrintTemplate template = erpPrintTemplateMapper.selectOne(new QueryWrapper<ErpPrintTemplate>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (template == null) {
            throw new IllegalArgumentException("打印模板不存在");
        }
        String normalizedDocType = normalizeDocType(template.getDocType());
        applyDefaultFlag(tenantId, normalizedDocType, template);
        template.setIsDefault(true);
        template.setUpdatedAt(Instant.now());
        erpPrintTemplateMapper.updateById(template);
        return template;
    }

    private QueryWrapper<ErpPrintTemplate> baseWrapper(String keyword, String docType, Boolean enabled) {
        Long tenantId = TenantContext.requireTenantId();
        QueryWrapper<ErpPrintTemplate> wrapper = new QueryWrapper<ErpPrintTemplate>()
            .eq("tenant_id", tenantId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("name", keyword).or().like("code", keyword));
        }
        if (docType != null && !docType.isBlank()) {
            wrapper.eq("doc_type", docType);
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        return wrapper;
    }

    private void applyRequest(ErpPrintTemplate template, ErpPrintTemplateCreateRequest request, String docType) {
        template.setCode(request.code());
        template.setName(request.name());
        template.setDocType(docType);
        template.setHeaderTitle(request.headerTitle());
        template.setSubTitle(request.subTitle());
        template.setFooterNote(request.footerNote());
        template.setFieldConfig(request.fieldConfig());
        template.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        template.setEnabled(request.enabled() == null ? Boolean.TRUE : request.enabled());
        template.setIsDefault(request.isDefault() != null && request.isDefault());
        template.setRemark(request.remark());
    }

    private void applyRequest(ErpPrintTemplate template, ErpPrintTemplateUpdateRequest request, String docType) {
        template.setCode(request.code());
        template.setName(request.name());
        template.setDocType(docType);
        template.setHeaderTitle(request.headerTitle());
        template.setSubTitle(request.subTitle());
        template.setFooterNote(request.footerNote());
        template.setFieldConfig(request.fieldConfig());
        template.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        template.setEnabled(request.enabled() == null ? Boolean.TRUE : request.enabled());
        template.setIsDefault(request.isDefault() != null && request.isDefault());
        template.setRemark(request.remark());
    }

    private void applyDefaultFlag(Long tenantId, String docType, ErpPrintTemplate template) {
        if (Boolean.TRUE.equals(template.getIsDefault())) {
            erpPrintTemplateMapper.update(null, new UpdateWrapper<ErpPrintTemplate>()
                .eq("tenant_id", tenantId)
                .eq("doc_type", docType)
                .ne("id", template.getId() == null ? -1 : template.getId())
                .set("is_default", false)
                .set("updated_at", Instant.now()));
        }
    }

    private String normalizeDocType(String docType) {
        if (docType == null) {
            throw new IllegalArgumentException("单据类型不能为空");
        }
        String normalized = docType.trim().toUpperCase();
        if (!DOC_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("不支持的单据类型");
        }
        return normalized;
    }
}
