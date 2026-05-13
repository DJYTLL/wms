package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.erp.ErpAssemblyTemplateCreateRequest;
import com.example.wms.dto.erp.ErpAssemblyTemplateDetail;
import com.example.wms.dto.erp.ErpAssemblyTemplateItemRequest;
import com.example.wms.dto.erp.ErpAssemblyTemplateUpdateRequest;
import com.example.wms.entity.erp.ErpAssemblyTemplate;
import com.example.wms.entity.erp.ErpAssemblyTemplateItem;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.mapper.erp.ErpAssemblyTemplateItemMapper;
import com.example.wms.mapper.erp.ErpAssemblyTemplateMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.service.erp.ErpAssemblyTemplateService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Assembly template service implementation
@Service
public class ErpAssemblyTemplateServiceImpl implements ErpAssemblyTemplateService {
    private static final String TYPE_ASSEMBLE = "ASSEMBLE";
    private static final String TYPE_DISASSEMBLE = "DISASSEMBLE";

    private final ErpAssemblyTemplateMapper templateMapper;
    private final ErpAssemblyTemplateItemMapper itemMapper;
    private final ErpProductMapper productMapper;

    public ErpAssemblyTemplateServiceImpl(ErpAssemblyTemplateMapper templateMapper,
                                          ErpAssemblyTemplateItemMapper itemMapper,
                                          ErpProductMapper productMapper) {
        this.templateMapper = templateMapper;
        this.itemMapper = itemMapper;
        this.productMapper = productMapper;
    }

    @Override
    public List<ErpAssemblyTemplate> listAll(String orderType, String keyword) {
        Long tenantId = TenantContext.requireTenantId();
        return templateMapper.findByType(tenantId, normalizeType(orderType), normalizeKeyword(keyword));
    }

    @Override
    public ErpAssemblyTemplateDetail getDetail(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpAssemblyTemplate template = requireTemplate(tenantId, id);
        return new ErpAssemblyTemplateDetail(template, itemMapper.findByTemplateId(tenantId, id));
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_ASSEMBLY_CREATE", entityType = "erp_assembly_template", entityId = "{result.template.id}", detail = "name={result.template.name}")
    public ErpAssemblyTemplateDetail create(ErpAssemblyTemplateCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String type = normalizeType(request.orderType());
        ensureTemplateNameUnique(tenantId, type, request.name(), null);
        Set<Long> allowedDisabledProductIds = new HashSet<>();
        validateRequest(tenantId, type, request.finishedProductId(), request.finishedQty(), request.items(), allowedDisabledProductIds);
        ErpAssemblyTemplate template = new ErpAssemblyTemplate();
        applyCreateRequest(template, request, type);
        template.setTenantId(tenantId);
        template.setCreatedAt(Instant.now());
        template.setUpdatedAt(Instant.now());
        templateMapper.insert(template);
        List<ErpAssemblyTemplateItem> items = buildItems(tenantId, template.getId(), request.items(), allowedDisabledProductIds);
        for (ErpAssemblyTemplateItem item : items) {
            itemMapper.insert(item);
        }
        return new ErpAssemblyTemplateDetail(template, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_ASSEMBLY_UPDATE", entityType = "erp_assembly_template", entityId = "{arg0}", detail = "name={arg1.name}")
    public ErpAssemblyTemplateDetail update(Long id, ErpAssemblyTemplateUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpAssemblyTemplate template = requireTemplate(tenantId, id);
        String type = normalizeType(request.orderType());
        ensureTemplateNameUnique(tenantId, type, request.name(), id);
        Set<Long> allowedDisabledProductIds = existingProductIds(itemMapper.findByTemplateId(tenantId, id), template.getFinishedProductId());
        validateRequest(tenantId, type, request.finishedProductId(), request.finishedQty(), request.items(), allowedDisabledProductIds);
        applyUpdateRequest(template, request, type);
        template.setUpdatedAt(Instant.now());
        templateMapper.updateById(template);
        itemMapper.delete(new QueryWrapper<ErpAssemblyTemplateItem>().eq("tenant_id", tenantId).eq("template_id", id));
        List<ErpAssemblyTemplateItem> items = buildItems(tenantId, id, request.items(), allowedDisabledProductIds);
        for (ErpAssemblyTemplateItem item : items) {
            itemMapper.insert(item);
        }
        return new ErpAssemblyTemplateDetail(template, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_ASSEMBLY_DELETE", entityType = "erp_assembly_template", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        requireTemplate(tenantId, id);
        itemMapper.delete(new QueryWrapper<ErpAssemblyTemplateItem>().eq("tenant_id", tenantId).eq("template_id", id));
        templateMapper.deleteById(id);
    }

    private void validateRequest(Long tenantId, String orderType, Long finishedProductId, BigDecimal finishedQty,
                                 List<ErpAssemblyTemplateItemRequest> items, Set<Long> allowedDisabledProductIds) {
        if (!TYPE_ASSEMBLE.equals(orderType) && !TYPE_DISASSEMBLE.equals(orderType)) {
            throw new IllegalArgumentException("单据类型不正确");
        }
        requireUsableProduct(tenantId, finishedProductId, allowedDisabledProductIds);
        if (normalizeAmount(finishedQty).compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("成品数量必须大于0");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("模板明细不能为空");
        }
        Set<Long> dedupe = new HashSet<>();
        for (ErpAssemblyTemplateItemRequest item : items) {
            requireUsableProduct(tenantId, item.productId(), allowedDisabledProductIds);
            if (normalizeAmount(item.qty()).compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("明细数量必须大于0");
            }
            if (!dedupe.add(item.productId())) {
                throw new IllegalArgumentException("模板明细商品不能重复");
            }
        }
    }

    private void applyCreateRequest(ErpAssemblyTemplate template, ErpAssemblyTemplateCreateRequest request, String type) {
        template.setOrderType(type);
        template.setName(request.name().trim());
        template.setFinishedProductId(request.finishedProductId());
        template.setFinishedQty(normalizeAmount(request.finishedQty()));
        template.setWarehouseId(request.warehouseId());
        template.setLocationId(request.locationId());
        template.setLaborCost(normalizeAmount(request.laborCost()));
        template.setRemark(request.remark());
    }

    private void applyUpdateRequest(ErpAssemblyTemplate template, ErpAssemblyTemplateUpdateRequest request, String type) {
        template.setOrderType(type);
        template.setName(request.name().trim());
        template.setFinishedProductId(request.finishedProductId());
        template.setFinishedQty(normalizeAmount(request.finishedQty()));
        template.setWarehouseId(request.warehouseId());
        template.setLocationId(request.locationId());
        template.setLaborCost(normalizeAmount(request.laborCost()));
        template.setRemark(request.remark());
    }

    private List<ErpAssemblyTemplateItem> buildItems(Long tenantId, Long templateId,
                                                     List<ErpAssemblyTemplateItemRequest> requests,
                                                     Set<Long> allowedDisabledProductIds) {
        List<ErpAssemblyTemplateItem> items = new ArrayList<>();
        int lineNo = 1;
        for (ErpAssemblyTemplateItemRequest request : requests) {
            ErpProduct product = requireUsableProduct(tenantId, request.productId(), allowedDisabledProductIds);
            ErpAssemblyTemplateItem item = new ErpAssemblyTemplateItem();
            item.setTenantId(tenantId);
            item.setTemplateId(templateId);
            item.setLineNo(lineNo++);
            item.setProductId(product.getId());
            item.setProductCode(product.getCode());
            item.setProductName(product.getName());
            item.setWarehouseId(request.warehouseId());
            item.setLocationId(request.locationId());
            item.setQty(normalizeAmount(request.qty()));
            item.setRemark(request.remark());
            item.setCreatedAt(Instant.now());
            item.setUpdatedAt(Instant.now());
            items.add(item);
        }
        return items;
    }

    private ErpAssemblyTemplate requireTemplate(Long tenantId, Long id) {
        ErpAssemblyTemplate template = templateMapper.selectOne(new QueryWrapper<ErpAssemblyTemplate>().eq("tenant_id", tenantId).eq("id", id));
        if (template == null) {
            throw new IllegalArgumentException("模板不存在");
        }
        return template;
    }

    private void ensureTemplateNameUnique(Long tenantId, String orderType, String name, Long currentId) {
        ErpAssemblyTemplate existing = templateMapper.findByName(tenantId, orderType, name.trim());
        if (existing != null && (currentId == null || !existing.getId().equals(currentId))) {
            throw new IllegalArgumentException("模板名称已存在");
        }
    }

    private ErpProduct requireUsableProduct(Long tenantId, Long productId, Set<Long> allowedDisabledProductIds) {
        if (productId == null) {
            throw new IllegalArgumentException("商品不能为空");
        }
        ErpProduct product = productMapper.selectOne(new QueryWrapper<ErpProduct>().eq("tenant_id", tenantId).eq("id", productId));
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (Boolean.FALSE.equals(product.getEnabled()) && !allowedDisabledProductIds.contains(productId)) {
            throw new IllegalArgumentException("商品已停用");
        }
        return product;
    }

    private Set<Long> existingProductIds(List<ErpAssemblyTemplateItem> items, Long finishedProductId) {
        Set<Long> ids = new HashSet<>();
        if (finishedProductId != null) {
            ids.add(finishedProductId);
        }
        for (ErpAssemblyTemplateItem item : items) {
            if (item.getProductId() != null) {
                ids.add(item.getProductId());
            }
        }
        return ids;
    }

    private String normalizeType(String orderType) {
        if (orderType == null || orderType.isBlank()) {
            throw new IllegalArgumentException("单据类型不正确");
        }
        String normalized = orderType.trim().toUpperCase();
        if (!TYPE_ASSEMBLE.equals(normalized) && !TYPE_DISASSEMBLE.equals(normalized)) {
            throw new IllegalArgumentException("单据类型不正确");
        }
        return normalized;
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.stripTrailingZeros();
    }
}
