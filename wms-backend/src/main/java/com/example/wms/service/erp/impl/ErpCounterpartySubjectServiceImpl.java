package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCounterpartyPendingDoc;
import com.example.wms.dto.erp.ErpCounterpartySubjectDetail;
import com.example.wms.dto.erp.ErpCounterpartySubjectCreateRequest;
import com.example.wms.dto.erp.ErpCounterpartySubjectMember;
import com.example.wms.dto.erp.ErpCounterpartyUnbindCheck;
import com.example.wms.dto.erp.ErpCounterpartySubjectUpdateRequest;
import com.example.wms.entity.erp.ErpAccountsPayable;
import com.example.wms.entity.erp.ErpAccountsReceivable;
import com.example.wms.entity.erp.ErpCounterpartySubject;
import com.example.wms.entity.erp.ErpCounterpartySubjectLink;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpPayment;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.entity.erp.ErpPurchaseReturn;
import com.example.wms.entity.erp.ErpReceipt;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.entity.erp.ErpSaleReturn;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpCounterpartySubjectLinkMapper;
import com.example.wms.mapper.erp.ErpCounterpartySubjectMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.service.erp.ErpCounterpartySubjectService;
import com.example.wms.service.erp.support.ErpCounterpartyGuardRules;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
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
    private final ErpSaleOrderMapper saleOrderMapper;
    private final ErpSaleReturnMapper saleReturnMapper;
    private final ErpReceiptMapper receiptMapper;
    private final ErpAccountsReceivableMapper accountsReceivableMapper;
    private final ErpPurchaseOrderMapper purchaseOrderMapper;
    private final ErpPurchaseReturnMapper purchaseReturnMapper;
    private final ErpPaymentMapper paymentMapper;
    private final ErpAccountsPayableMapper accountsPayableMapper;

    public ErpCounterpartySubjectServiceImpl(ErpCounterpartySubjectMapper counterpartySubjectMapper,
                                             ErpCounterpartySubjectLinkMapper counterpartySubjectLinkMapper,
                                             ErpSupplierMapper supplierMapper,
                                             ErpCustomerMapper customerMapper,
                                             ErpSaleOrderMapper saleOrderMapper,
                                             ErpSaleReturnMapper saleReturnMapper,
                                             ErpReceiptMapper receiptMapper,
                                             ErpAccountsReceivableMapper accountsReceivableMapper,
                                             ErpPurchaseOrderMapper purchaseOrderMapper,
                                             ErpPurchaseReturnMapper purchaseReturnMapper,
                                             ErpPaymentMapper paymentMapper,
                                             ErpAccountsPayableMapper accountsPayableMapper) {
        this.counterpartySubjectMapper = counterpartySubjectMapper;
        this.counterpartySubjectLinkMapper = counterpartySubjectLinkMapper;
        this.supplierMapper = supplierMapper;
        this.customerMapper = customerMapper;
        this.saleOrderMapper = saleOrderMapper;
        this.saleReturnMapper = saleReturnMapper;
        this.receiptMapper = receiptMapper;
        this.accountsReceivableMapper = accountsReceivableMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseReturnMapper = purchaseReturnMapper;
        this.paymentMapper = paymentMapper;
        this.accountsPayableMapper = accountsPayableMapper;
    }

    @Override
    public List<ErpCounterpartySubject> listAll(String keyword, Boolean enabled) {
        QueryWrapper<ErpCounterpartySubject> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("id");
        List<ErpCounterpartySubject> subjects = counterpartySubjectMapper.selectList(wrapper);
        enrichBindingStats(subjects);
        return subjects;
    }

    @Override
    public PageResponse<ErpCounterpartySubject> page(long page, long size, String keyword, Boolean enabled) {
        Page<ErpCounterpartySubject> pageReq = Page.of(page, size);
        QueryWrapper<ErpCounterpartySubject> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByAsc("id");
        Page<ErpCounterpartySubject> result = counterpartySubjectMapper.selectPage(pageReq, wrapper);
        enrichBindingStats(result.getRecords());
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
        ErpCounterpartySubjectLink link = bindTarget(subject.getId(), tenantId, TARGET_TYPE_CUSTOMER, customerId, primary, remark);
        if (customer.getCounterpartySubjectId() == null || !customer.getCounterpartySubjectId().equals(subject.getId())) {
            customer.setCounterpartySubjectId(subject.getId());
            customerMapper.updateById(customer);
        }
        return link;
    }

    @Override
    @Transactional
    public void unbindSupplier(Long id, Long supplierId) {
        Long tenantId = TenantContext.requireTenantId();
        ErpCounterpartyUnbindCheck check = buildSupplierUnbindCheck(tenantId, supplierId);
        if (!check.allowed()) {
            throw new IllegalArgumentException(String.join("；", check.blockingReasons()));
        }
        deleteLink(id, supplierId, tenantId, TARGET_TYPE_SUPPLIER);
        ErpSupplier supplier = supplierMapper.selectOne(new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", tenantId)
            .eq("id", supplierId));
        if (supplier != null && id.equals(supplier.getCounterpartySubjectId())) {
            supplier.setCounterpartySubjectId(null);
            supplierMapper.updateById(supplier);
        }
    }

    @Override
    @Transactional
    public void unbindCustomer(Long id, Long customerId) {
        Long tenantId = TenantContext.requireTenantId();
        ErpCounterpartyUnbindCheck check = buildCustomerUnbindCheck(tenantId, customerId);
        if (!check.allowed()) {
            throw new IllegalArgumentException(String.join("；", check.blockingReasons()));
        }
        deleteLink(id, customerId, tenantId, TARGET_TYPE_CUSTOMER);
        ErpCustomer customer = customerMapper.selectOne(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .eq("id", customerId));
        if (customer != null && id.equals(customer.getCounterpartySubjectId())) {
            customer.setCounterpartySubjectId(null);
            customerMapper.updateById(customer);
        }
    }

    @Override
    public ErpCounterpartySubjectDetail getDetail(Long id) {
        ErpCounterpartySubject subject = requireSubject(id);
        Long tenantId = TenantContext.requireTenantId();
        List<ErpCounterpartySubjectMember> customers = customerMapper.listBySubjectId(tenantId, id).stream()
            .map(item -> new ErpCounterpartySubjectMember(
                item.id(),
                item.code(),
                item.name(),
                item.contact(),
                item.phone(),
                item.mobile(),
                item.roleType(),
                buildCustomerUnbindCheck(tenantId, item.id())
            ))
            .toList();
        List<ErpCounterpartySubjectMember> suppliers = supplierMapper.listBySubjectId(tenantId, id).stream()
            .map(item -> new ErpCounterpartySubjectMember(
                item.id(),
                item.code(),
                item.name(),
                item.contact(),
                item.phone(),
                item.mobile(),
                item.roleType(),
                buildSupplierUnbindCheck(tenantId, item.id())
            ))
            .toList();
        return new ErpCounterpartySubjectDetail(
            subject.getId(),
            subject.getName(),
            subject.getRegion(),
            subject.getUnifiedCreditCode(),
            customers.size(),
            suppliers.size(),
            customers,
            suppliers
        );
    }

    @Override
    public ErpCounterpartyUnbindCheck checkUnbindSupplier(Long id, Long supplierId) {
        requireSubject(id);
        ensureLinkExists(id, supplierId, TARGET_TYPE_SUPPLIER);
        return buildSupplierUnbindCheck(TenantContext.requireTenantId(), supplierId);
    }

    @Override
    public ErpCounterpartyUnbindCheck checkUnbindCustomer(Long id, Long customerId) {
        requireSubject(id);
        ensureLinkExists(id, customerId, TARGET_TYPE_CUSTOMER);
        return buildCustomerUnbindCheck(TenantContext.requireTenantId(), customerId);
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

    private void deleteLink(Long subjectId, Long targetId, Long tenantId, String targetType) {
        ErpCounterpartySubjectLink link = counterpartySubjectLinkMapper.selectOne(new QueryWrapper<ErpCounterpartySubjectLink>()
            .eq("tenant_id", tenantId)
            .eq("subject_id", subjectId)
            .eq("target_type", targetType)
            .eq("target_id", targetId));
        if (link == null) {
            throw new IllegalArgumentException("关联关系不存在");
        }
        counterpartySubjectLinkMapper.deleteById(link.getId());
    }

    private void ensureLinkExists(Long subjectId, Long targetId, String targetType) {
        Long tenantId = TenantContext.requireTenantId();
        ErpCounterpartySubjectLink link = counterpartySubjectLinkMapper.selectOne(new QueryWrapper<ErpCounterpartySubjectLink>()
            .eq("tenant_id", tenantId)
            .eq("subject_id", subjectId)
            .eq("target_type", targetType)
            .eq("target_id", targetId));
        if (link == null) {
            throw new IllegalArgumentException("关联关系不存在");
        }
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

    private void enrichBindingStats(List<ErpCounterpartySubject> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return;
        }
        Long tenantId = TenantContext.requireTenantId();
        for (ErpCounterpartySubject subject : subjects) {
            int customerCount = Math.toIntExact(counterpartySubjectLinkMapper.selectCount(new QueryWrapper<ErpCounterpartySubjectLink>()
                .eq("tenant_id", tenantId)
                .eq("subject_id", subject.getId())
                .eq("target_type", TARGET_TYPE_CUSTOMER)));
            int supplierCount = Math.toIntExact(counterpartySubjectLinkMapper.selectCount(new QueryWrapper<ErpCounterpartySubjectLink>()
                .eq("tenant_id", tenantId)
                .eq("subject_id", subject.getId())
                .eq("target_type", TARGET_TYPE_SUPPLIER)));
            subject.setCustomerCount(customerCount);
            subject.setSupplierCount(supplierCount);
        }
    }

    private ErpCounterpartyUnbindCheck buildCustomerUnbindCheck(Long tenantId, Long customerId) {
        List<String> reasons = new ArrayList<>();
        List<ErpCounterpartyPendingDoc> docs = new ArrayList<>();

        List<ErpSaleOrder> openSaleOrders = saleOrderMapper.selectList(new QueryWrapper<ErpSaleOrder>()
            .eq("tenant_id", tenantId)
            .eq("customer_id", customerId)
            .in("status", ErpCounterpartyGuardRules.BLOCKING_DOCUMENT_STATUSES));
        if (!openSaleOrders.isEmpty()) {
            reasons.add("存在未完成销售单");
            docs.addAll(openSaleOrders.stream()
                .map(item -> new ErpCounterpartyPendingDoc("SALE_ORDER", item.getId(), item.getOrderNo(), item.getStatus(), resolveSaleOrderRouteKey(item.getStatus())))
                .toList());
        }

        List<ErpSaleReturn> openSaleReturns = saleReturnMapper.selectList(new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", tenantId)
            .eq("customer_id", customerId)
            .in("status", ErpCounterpartyGuardRules.BLOCKING_DOCUMENT_STATUSES));
        if (!openSaleReturns.isEmpty()) {
            reasons.add("存在未完成销售退货单");
            docs.addAll(openSaleReturns.stream()
                .map(item -> new ErpCounterpartyPendingDoc("SALE_RETURN", item.getId(), item.getOrderNo(), item.getStatus(), resolveSaleReturnRouteKey(item.getStatus())))
                .toList());
        }

        List<ErpReceipt> openReceipts = receiptMapper.selectList(new QueryWrapper<ErpReceipt>()
            .eq("tenant_id", tenantId)
            .eq("customer_id", customerId)
            .in("status", ErpCounterpartyGuardRules.BLOCKING_DOCUMENT_STATUSES));
        if (!openReceipts.isEmpty()) {
            reasons.add("存在未完成收款单");
            docs.addAll(openReceipts.stream()
                .map(item -> new ErpCounterpartyPendingDoc("RECEIPT", item.getId(), item.getReceiptNo(), item.getStatus(), "erp-receipts-detail"))
                .toList());
        }

        List<ErpAccountsReceivable> receivables = accountsReceivableMapper.selectList(new QueryWrapper<ErpAccountsReceivable>()
            .eq("tenant_id", tenantId)
            .eq("customer_id", customerId)
            .ne("status", ErpCounterpartyGuardRules.RED_FLUSHED_STATUS));
        BigDecimal totalReceivable = receivables.stream()
            .map(ErpAccountsReceivable::getUnpaidAmount)
            .filter(value -> value != null && value.compareTo(BigDecimal.ZERO) != 0)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalReceivable.compareTo(BigDecimal.ZERO) != 0) {
            reasons.add("存在未完成应收，未收金额合计：" + totalReceivable.stripTrailingZeros().toPlainString());
        }

        return new ErpCounterpartyUnbindCheck(reasons.isEmpty(), reasons, docs);
    }

    private ErpCounterpartyUnbindCheck buildSupplierUnbindCheck(Long tenantId, Long supplierId) {
        List<String> reasons = new ArrayList<>();
        List<ErpCounterpartyPendingDoc> docs = new ArrayList<>();

        List<ErpPurchaseOrder> openPurchaseOrders = purchaseOrderMapper.selectList(new QueryWrapper<ErpPurchaseOrder>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)
            .in("status", ErpCounterpartyGuardRules.BLOCKING_DOCUMENT_STATUSES));
        if (!openPurchaseOrders.isEmpty()) {
            reasons.add("存在未完成采购单");
            docs.addAll(openPurchaseOrders.stream()
                .map(item -> new ErpCounterpartyPendingDoc("PURCHASE_ORDER", item.getId(), item.getOrderNo(), item.getStatus(), resolvePurchaseOrderRouteKey(item.getStatus())))
                .toList());
        }

        List<ErpPurchaseReturn> openPurchaseReturns = purchaseReturnMapper.selectList(new QueryWrapper<ErpPurchaseReturn>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)
            .in("status", ErpCounterpartyGuardRules.BLOCKING_DOCUMENT_STATUSES));
        if (!openPurchaseReturns.isEmpty()) {
            reasons.add("存在未完成采购退货单");
            docs.addAll(openPurchaseReturns.stream()
                .map(item -> new ErpCounterpartyPendingDoc("PURCHASE_RETURN", item.getId(), item.getOrderNo(), item.getStatus(), resolvePurchaseReturnRouteKey(item.getStatus())))
                .toList());
        }

        List<ErpPayment> openPayments = paymentMapper.selectList(new QueryWrapper<ErpPayment>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)
            .in("status", ErpCounterpartyGuardRules.BLOCKING_DOCUMENT_STATUSES));
        if (!openPayments.isEmpty()) {
            reasons.add("存在未完成付款单");
            docs.addAll(openPayments.stream()
                .map(item -> new ErpCounterpartyPendingDoc("PAYMENT", item.getId(), item.getPaymentNo(), item.getStatus(), "erp-payments-detail"))
                .toList());
        }

        List<ErpAccountsPayable> payables = accountsPayableMapper.selectList(new QueryWrapper<ErpAccountsPayable>()
            .eq("tenant_id", tenantId)
            .eq("supplier_id", supplierId)
            .ne("status", ErpCounterpartyGuardRules.RED_FLUSHED_STATUS));
        BigDecimal totalPayable = payables.stream()
            .map(ErpAccountsPayable::getUnpaidAmount)
            .filter(value -> value != null && value.compareTo(BigDecimal.ZERO) != 0)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalPayable.compareTo(BigDecimal.ZERO) != 0) {
            reasons.add("存在未完成应付，未付金额合计：" + totalPayable.stripTrailingZeros().toPlainString());
        }

        return new ErpCounterpartyUnbindCheck(reasons.isEmpty(), reasons, docs);
    }

    private String resolveSaleOrderRouteKey(String status) {
        return "DRAFT".equals(status) ? "erp-sale-orders-draft-edit" : "erp-sale-orders-approved-detail";
    }

    private String resolveSaleReturnRouteKey(String status) {
        return "DRAFT".equals(status) ? "erp-sale-returns-draft-edit" : "erp-sale-returns-approved-detail";
    }

    private String resolvePurchaseOrderRouteKey(String status) {
        return "DRAFT".equals(status) ? "erp-purchase-draft-edit" : "erp-purchase-approved-detail";
    }

    private String resolvePurchaseReturnRouteKey(String status) {
        return "DRAFT".equals(status) ? "erp-purchase-returns-draft-edit" : "erp-purchase-returns-approved-detail";
    }
}
