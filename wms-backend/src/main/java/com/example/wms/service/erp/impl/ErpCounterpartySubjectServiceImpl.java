package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCounterpartySubjectCreateRequest;
import com.example.wms.dto.erp.ErpCounterpartySubjectUpdateRequest;
import com.example.wms.entity.erp.ErpCounterpartySubject;
import com.example.wms.entity.erp.ErpCounterpartySubjectLink;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.mapper.erp.ErpCounterpartySubjectLinkMapper;
import com.example.wms.mapper.erp.ErpCounterpartySubjectMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.service.erp.ErpCounterpartySubjectService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

// 往来主体服务实现（ERP进销存）
@Service
public class ErpCounterpartySubjectServiceImpl implements ErpCounterpartySubjectService {
    private static final String TARGET_TYPE_SUPPLIER = "SUPPLIER";
    private static final String TARGET_TYPE_CUSTOMER = "CUSTOMER";

    private final ErpCounterpartySubjectMapper counterpartySubjectMapper;
    private final ErpCounterpartySubjectLinkMapper counterpartySubjectLinkMapper;
    private final ErpSupplierMapper supplierMapper;
    private final ErpCustomerMapper customerMapper;

    public ErpCounterpartySubjectServiceImpl(ErpCounterpartySubjectMapper counterpartySubjectMapper,
                                             ErpCounterpartySubjectLinkMapper counterpartySubjectLinkMapper,
                                             ErpSupplierMapper supplierMapper,
                                             ErpCustomerMapper customerMapper) {
        this.counterpartySubjectMapper = counterpartySubjectMapper;
        this.counterpartySubjectLinkMapper = counterpartySubjectLinkMapper;
        this.supplierMapper = supplierMapper;
        this.customerMapper = customerMapper;
    }

    @Override
    public List<ErpCounterpartySubject> listAll(String keyword, Boolean enabled) {
        QueryWrapper<ErpCounterpartySubject> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("id");
        return counterpartySubjectMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpCounterpartySubject> page(long page, long size, String keyword, Boolean enabled) {
        Page<ErpCounterpartySubject> pageReq = Page.of(page, size);
        QueryWrapper<ErpCounterpartySubject> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("id");
        Page<ErpCounterpartySubject> result = counterpartySubjectMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpCounterpartySubject getById(Long id) {
        ErpCounterpartySubject subject = findSubject(id);
        if (subject == null) {
            throw new IllegalArgumentException("往来主体不存在");
        }
        return subject;
    }

    @Override
    @AuditLog(action = "ERP_COUNTERPARTY_SUBJECT_CREATE", entityType = "erp_counterparty_subject", entityId = "{result.id}", detail = "name={arg0.name}")
    @Transactional
    public ErpCounterpartySubject create(ErpCounterpartySubjectCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ensureNameUnique(tenantId, request.name(), null);

        Instant now = Instant.now();
        ErpCounterpartySubject subject = new ErpCounterpartySubject();
        subject.setTenantId(tenantId);
        applyRequest(subject, request);
        subject.setEnabled(request.enabled() == null || request.enabled());
        subject.setCreatedAt(now);
        subject.setUpdatedAt(now);
        counterpartySubjectMapper.insert(subject);
        return subject;
    }

    @Override
    @AuditLog(action = "ERP_COUNTERPARTY_SUBJECT_UPDATE", entityType = "erp_counterparty_subject", entityId = "{arg0}", detail = "name={arg1.name}")
    @Transactional
    public ErpCounterpartySubject update(Long id, ErpCounterpartySubjectUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpCounterpartySubject subject = findSubject(id);
        if (subject == null) {
            throw new IllegalArgumentException("往来主体不存在");
        }
        ensureNameUnique(tenantId, request.name(), id);

        applyRequest(subject, request);
        if (request.enabled() != null) {
            subject.setEnabled(request.enabled());
        }
        subject.setUpdatedAt(Instant.now());
        counterpartySubjectMapper.updateById(subject);
        return subject;
    }

    @Override
    @AuditLog(action = "ERP_COUNTERPARTY_SUBJECT_DELETE", entityType = "erp_counterparty_subject", entityId = "{arg0}")
    @Transactional
    public void delete(Long id) {
        ErpCounterpartySubject subject = findSubject(id);
        if (subject == null) {
            throw new IllegalArgumentException("往来主体不存在");
        }
        Long tenantId = TenantContext.requireTenantId();
        if (counterpartySubjectLinkMapper.selectCount(new QueryWrapper<ErpCounterpartySubjectLink>()
            .eq("tenant_id", tenantId)
            .eq("subject_id", id)) > 0) {
            throw new IllegalArgumentException("往来主体已存在关联记录，不能删除");
        }
        counterpartySubjectMapper.deleteById(id);
    }

    @Override
    @AuditLog(action = "ERP_COUNTERPARTY_SUBJECT_BIND_SUPPLIER", entityType = "erp_counterparty_subject", entityId = "{arg0}", detail = "supplierId={arg1}")
    @Transactional
    public ErpCounterpartySubjectLink bindSupplier(Long id, Long supplierId, Boolean primary, String remark) {
        ErpCounterpartySubject subject = requireSubject(id);
        Long tenantId = TenantContext.requireTenantId();
        ErpSupplier supplier = supplierMapper.selectOne(new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", tenantId)
            .eq("id", supplierId));
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        ErpCounterpartySubjectLink link = bindTarget(subject.getId(), tenantId, TARGET_TYPE_SUPPLIER, supplierId, primary, remark);
        if (supplier.getCounterpartySubjectId() == null || !supplier.getCounterpartySubjectId().equals(subject.getId())) {
            supplier.setCounterpartySubjectId(subject.getId());
            supplierMapper.updateById(supplier);
        }
        return link;
    }

    @Override
    @AuditLog(action = "ERP_COUNTERPARTY_SUBJECT_BIND_CUSTOMER", entityType = "erp_counterparty_subject", entityId = "{arg0}", detail = "customerId={arg1}")
    @Transactional
    public ErpCounterpartySubjectLink bindCustomer(Long id, Long customerId, Boolean primary, String remark) {
        ErpCounterpartySubject subject = requireSubject(id);
        Long tenantId = TenantContext.requireTenantId();
        ErpCustomer customer = customerMapper.selectOne(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .eq("id", customerId));
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        return bindTarget(subject.getId(), tenantId, TARGET_TYPE_CUSTOMER, customerId, primary, remark);
    }

    private QueryWrapper<ErpCounterpartySubject> baseWrapper(String keyword, Boolean enabled) {
        QueryWrapper<ErpCounterpartySubject> wrapper = new QueryWrapper<ErpCounterpartySubject>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("name", keyword)
                .or()
                .like("region", keyword)
                .or()
                .like("unified_credit_code", keyword));
        }
        if (enabled != null) {
            wrapper.eq("enabled", enabled);
        }
        return wrapper;
    }

    private void ensureNameUnique(Long tenantId, String name, Long currentId) {
        ErpCounterpartySubject existing = counterpartySubjectMapper.findByName(tenantId, name.trim());
        if (existing != null && (currentId == null || !existing.getId().equals(currentId))) {
            throw new IllegalArgumentException("往来主体名称已存在");
        }
    }

    private ErpCounterpartySubject requireSubject(Long id) {
        ErpCounterpartySubject subject = findSubject(id);
        if (subject == null) {
            throw new IllegalArgumentException("往来主体不存在");
        }
        return subject;
    }

    private ErpCounterpartySubject findSubject(Long id) {
        return counterpartySubjectMapper.selectOne(new QueryWrapper<ErpCounterpartySubject>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
    }

    private ErpCounterpartySubjectLink bindTarget(Long subjectId,
                                                  Long tenantId,
                                                  String targetType,
                                                  Long targetId,
                                                  Boolean primary,
                                                  String remark) {
        ErpCounterpartySubjectLink existing = counterpartySubjectLinkMapper.selectOne(new QueryWrapper<ErpCounterpartySubjectLink>()
            .eq("tenant_id", tenantId)
            .eq("target_type", targetType)
            .eq("target_id", targetId)
            .eq("role_type", targetType));
        if (existing != null) {
            if (existing.getSubjectId().equals(subjectId)) {
                return existing;
            }
            if (TARGET_TYPE_SUPPLIER.equals(targetType)) {
                throw new IllegalArgumentException("供应商已绑定其他往来主体");
            }
            throw new IllegalArgumentException("客户已绑定其他往来主体");
        }

        Instant now = Instant.now();
        ErpCounterpartySubjectLink link = new ErpCounterpartySubjectLink();
        link.setTenantId(tenantId);
        link.setSubjectId(subjectId);
        link.setTargetType(targetType);
        link.setTargetId(targetId);
        link.setRoleType(targetType);
        link.setPrimary(primary != null && primary);
        link.setRemark(remark);
        link.setCreatedAt(now);
        link.setUpdatedAt(now);
        counterpartySubjectLinkMapper.insert(link);
        return link;
    }

    private void applyRequest(ErpCounterpartySubject subject, ErpCounterpartySubjectCreateRequest request) {
        subject.setName(request.name().trim());
        subject.setRegion(trimToNull(request.region()));
        subject.setUnifiedCreditCode(trimToNull(request.unifiedCreditCode()));
        subject.setRemark(trimToNull(request.remark()));
    }

    private void applyRequest(ErpCounterpartySubject subject, ErpCounterpartySubjectUpdateRequest request) {
        subject.setName(request.name().trim());
        subject.setRegion(trimToNull(request.region()));
        subject.setUnifiedCreditCode(trimToNull(request.unifiedCreditCode()));
        subject.setRemark(trimToNull(request.remark()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
