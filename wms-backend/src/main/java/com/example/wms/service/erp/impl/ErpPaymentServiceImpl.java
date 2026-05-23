package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPaymentAllocationRequest;
import com.example.wms.dto.erp.ErpPaymentCreateRequest;
import com.example.wms.dto.erp.ErpPaymentDetail;
import com.example.wms.dto.erp.ErpPaymentPayableView;
import com.example.wms.dto.erp.ErpPaymentSourcePayableDetail;
import com.example.wms.dto.erp.ErpPaymentSourcePayableOption;
import com.example.wms.dto.erp.ErpPaymentView;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpAccountsPayable;
import com.example.wms.entity.erp.ErpSettlementMethod;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.entity.erp.ErpPayment;
import com.example.wms.entity.erp.ErpPaymentPayable;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPaymentPayableMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.security.CurrentActor;
import com.example.wms.service.erp.ErpPaymentService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// ERP付款单服务实现
@Service
public class ErpPaymentServiceImpl implements ErpPaymentService {
    private static final String PAYMENT_ORDER_TYPE = "PAYMENT";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_RED_FLUSHED = "RED_FLUSHED";
    private static final String STATUS_SETTLED = "SETTLED";
    private static final String STATUS_OPEN = "OPEN";

    private enum PayableMode {
        NORMAL,
        RETURN,
        MIXED
    }
    private final ErpPaymentMapper erpPaymentMapper;
    private final ErpPaymentPayableMapper erpPaymentPayableMapper;
    private final ErpSupplierMapper erpSupplierMapper;
    private final ErpAccountsPayableMapper erpAccountsPayableMapper;
    private final ErpPurchaseOrderMapper erpPurchaseOrderMapper;
    private final ErpSettlementMethodMapper erpSettlementMethodMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;

    public ErpPaymentServiceImpl(ErpPaymentMapper erpPaymentMapper,
                                 ErpPaymentPayableMapper erpPaymentPayableMapper,
                                 ErpSupplierMapper erpSupplierMapper,
                                 ErpAccountsPayableMapper erpAccountsPayableMapper,
                                 ErpPurchaseOrderMapper erpPurchaseOrderMapper,
                                 ErpSettlementMethodMapper erpSettlementMethodMapper,
                                 ErpOrderSequenceMapper erpOrderSequenceMapper,
                                 SystemConfigMapper systemConfigMapper) {
        this.erpPaymentMapper = erpPaymentMapper;
        this.erpPaymentPayableMapper = erpPaymentPayableMapper;
        this.erpSupplierMapper = erpSupplierMapper;
        this.erpAccountsPayableMapper = erpAccountsPayableMapper;
        this.erpPurchaseOrderMapper = erpPurchaseOrderMapper;
        this.erpSettlementMethodMapper = erpSettlementMethodMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
    }

    @Override
    public List<ErpPaymentView> listAll(String keyword, String status, Long supplierId, Long payableId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpPayment> wrapper = baseWrapper(keyword, status, supplierId, payableId, startAt, endAt);
        wrapper.orderByDesc("created_at");
        List<ErpPayment> items = erpPaymentMapper.selectList(wrapper);
        return mapViews(items);
    }

    @Override
    public PageResponse<ErpPaymentView> page(long page, long size, String keyword, String status, Long supplierId, Long payableId, Instant startAt, Instant endAt) {
        Page<ErpPayment> pageReq = Page.of(page, size);
        QueryWrapper<ErpPayment> wrapper = baseWrapper(keyword, status, supplierId, payableId, startAt, endAt);
        wrapper.orderByDesc("created_at");
        Page<ErpPayment> result = erpPaymentMapper.selectPage(pageReq, wrapper);
        List<ErpPaymentView> views = mapViews(result.getRecords());
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), views);
    }

    @Override
    public PageResponse<ErpPaymentSourcePayableOption> sourcePayablePage(long page, long size, String keyword, Long supplierId, String status, Instant startAt, Instant endAt) {
        Long tenantId = TenantContext.requireTenantId();
        long finalPage = page <= 0 ? 1 : page;
        long finalSize = size <= 0 ? 20 : Math.min(size, 200);
        QueryWrapper<ErpAccountsPayable> wrapper = new QueryWrapper<ErpAccountsPayable>()
            .eq("tenant_id", tenantId)
            .isNull("deleted_at");
        if (supplierId != null) {
            wrapper.eq("supplier_id", supplierId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status.trim());
        }
        if (startAt != null) {
            wrapper.ge("created_at", startAt);
        }
        if (endAt != null) {
            wrapper.le("created_at", endAt);
        }
        if (keyword != null && !keyword.isBlank()) {
            String trimmed = keyword.trim();
            wrapper.and(qw -> qw.like("order_no", trimmed));
        }
        wrapper.orderByDesc("created_at").orderByDesc("id");
        Page<ErpAccountsPayable> result = erpAccountsPayableMapper.selectPage(Page.of(finalPage, finalSize), wrapper);
        List<ErpPaymentSourcePayableOption> items = result.getRecords().stream()
            .map(item -> new ErpPaymentSourcePayableOption(
                item.getId(),
                item.getOrderNo(),
                item.getSupplierId(),
                item.getTotalAmount(),
                item.getPaidAmount(),
                item.getDiscountAmount(),
                item.getUnpaidAmount(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getPurchaseOrderId(),
                item.getPurchaseReturnId()
            ))
            .toList();
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), items);
    }

    @Override
    public ErpPaymentDetail getDetail(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPayment receipt = erpPaymentMapper.selectOne(new QueryWrapper<ErpPayment>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (receipt == null) {
            throw new IllegalArgumentException("付款单不存在");
        }
        return buildDetail(tenantId, receipt);
    }

    @Override
    public String nextPaymentNo() {
        Long tenantId = TenantContext.requireTenantId();
        return generatePaymentNo(tenantId);
    }

    @Override
    @Transactional
    public ErpPaymentDetail create(ErpPaymentCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String operator = CurrentActor.username();
        BigDecimal amount = request.amount() == null ? BigDecimal.ZERO : request.amount();
        BigDecimal discountAmount = request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount();

        List<Long> payableIds = new ArrayList<>();
        if (request.payableIds() != null) {
            for (Long id : request.payableIds()) {
                if (id != null) {
                    payableIds.add(id);
                }
            }
        }
        if (payableIds.isEmpty() && request.payableId() != null) {
            payableIds.add(request.payableId());
        }
        if (payableIds.isEmpty() && request.allocations() != null) {
            for (var alloc : request.allocations()) {
                if (alloc != null && alloc.payableId() != null) {
                    payableIds.add(alloc.payableId());
                }
            }
        }
        if (payableIds.isEmpty()) {
            throw new IllegalArgumentException("请选择应付单");
        }

        List<ErpAccountsPayable> payables = erpAccountsPayableMapper.selectList(new QueryWrapper<ErpAccountsPayable>()
            .eq("tenant_id", tenantId)
            .in("id", payableIds));
        if (payables.size() != payableIds.size()) {
            throw new IllegalArgumentException("应付单不存在");
        }
        for (ErpAccountsPayable payable : payables) {
            if ("RED_FLUSHED".equals(payable.getStatus())) {
                throw new IllegalArgumentException("红冲应付单不可付款");
            }
            BigDecimal totalAmount = payable.getTotalAmount() == null ? BigDecimal.ZERO : payable.getTotalAmount();
            if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("金额为0的应付单不可付款");
            }
        }
        PayableMode payableMode = resolvePayableMode(payables);

        Long supplierId = request.supplierId();
        Long purchaseOrderId = request.purchaseOrderId();
        Long firstPayableId = payables.get(0).getId();
        Long resolvedSupplierId = payables.get(0).getSupplierId();
        boolean samePurchaseOrder = true;
        Long resolvedPurchaseOrderId = payables.get(0).getPurchaseOrderId();
        for (ErpAccountsPayable payable : payables) {
            if (!Objects.equals(payable.getSupplierId(), resolvedSupplierId)) {
                throw new IllegalArgumentException("应付单供应商不一致");
            }
            if (!Objects.equals(payable.getPurchaseOrderId(), resolvedPurchaseOrderId)) {
                samePurchaseOrder = false;
            }
        }
        if (supplierId == null) {
            supplierId = resolvedSupplierId;
        }
        if (!Objects.equals(supplierId, resolvedSupplierId)) {
            throw new IllegalArgumentException("供应商与应付单不一致");
        }
        if (purchaseOrderId == null && samePurchaseOrder) {
            purchaseOrderId = resolvedPurchaseOrderId;
        }
        if (supplierId == null) {
            throw new IllegalArgumentException("请选择供应商");
        }
        validateCashSettlementMethod(tenantId, request.settlementMethod());

        List<ErpPaymentPayable> allocations = null;
        boolean hasAllocations = request.allocations() != null && !request.allocations().isEmpty();
        if (hasAllocations) {
            allocations = buildAllocationsFromRequest(tenantId, payables, request.allocations());
            BigDecimal sumAmount = allocations.stream()
                .map(ErpPaymentPayable::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sumDiscount = allocations.stream()
                .map(ErpPaymentPayable::getAllocatedDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            amount = sumAmount;
            discountAmount = sumDiscount;
        } else {
            validateHeaderAmounts(payableMode, amount, discountAmount);
        }
        BigDecimal totalAllocate = amount.add(discountAmount);
        if (payableMode == PayableMode.MIXED && !hasAllocations) {
            throw new IllegalArgumentException("正负应付混合付款需填写分摊金额");
        }
        if (payableMode == PayableMode.RETURN) {
            if (totalAllocate.compareTo(BigDecimal.ZERO) >= 0) {
                throw new IllegalArgumentException("退款金额必须小于0");
            }
        } else if (payableMode != PayableMode.MIXED && totalAllocate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("付款金额或优惠金额必须大于0");
        }

        ErpPayment receipt = new ErpPayment();
        receipt.setTenantId(tenantId);
        receipt.setPayableId(firstPayableId);
        receipt.setPurchaseOrderId(purchaseOrderId);
        receipt.setPaymentNo(resolvePaymentNo(tenantId, request.paymentNo()));
        receipt.setSupplierId(supplierId);
        receipt.setAmount(amount);
        receipt.setDiscountAmount(discountAmount);
        receipt.setSettlementMethod(request.settlementMethod());
        receipt.setPaymentMethodCode(request.paymentMethodCode());
        receipt.setStatus(STATUS_DRAFT);
        receipt.setPaidAt(parsePaidAt(request.paidAt()));
        receipt.setRemark(request.remark());
        receipt.setCreatedAt(Instant.now());
        receipt.setCreatedBy(operator);
        receipt.setUpdatedAt(Instant.now());
        receipt.setUpdatedBy(operator);
        erpPaymentMapper.insert(receipt);

        if (allocations == null) {
            allocations = buildAllocations(tenantId, receipt.getId(), payables, amount, discountAmount, payableMode);
        } else {
            for (ErpPaymentPayable allocation : allocations) {
                allocation.setPaymentId(receipt.getId());
                allocation.setTenantId(tenantId);
                allocation.setCreatedAt(Instant.now());
            }
        }
        for (ErpPaymentPayable allocation : allocations) {
            erpPaymentPayableMapper.insert(allocation);
        }

        // 草稿不影响应付金额，审核时更新

        return buildDetail(tenantId, receipt);
    }

    @Override
    @Transactional
    public ErpPaymentDetail update(Long id, ErpPaymentCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPayment receipt = loadPayment(tenantId, id);
        if (!STATUS_DRAFT.equals(receipt.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可修改");
        }

        BigDecimal amount = request.amount() == null ? BigDecimal.ZERO : request.amount();
        BigDecimal discountAmount = request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount();

        List<Long> payableIds = new ArrayList<>();
        if (request.payableIds() != null) {
            for (Long rid : request.payableIds()) {
                if (rid != null) {
                    payableIds.add(rid);
                }
            }
        }
        if (payableIds.isEmpty() && request.payableId() != null) {
            payableIds.add(request.payableId());
        }
        if (payableIds.isEmpty() && request.allocations() != null) {
            for (var alloc : request.allocations()) {
                if (alloc != null && alloc.payableId() != null) {
                    payableIds.add(alloc.payableId());
                }
            }
        }
        if (payableIds.isEmpty()) {
            throw new IllegalArgumentException("请选择应付单");
        }

        List<ErpAccountsPayable> payables = erpAccountsPayableMapper.selectList(new QueryWrapper<ErpAccountsPayable>()
            .eq("tenant_id", tenantId)
            .in("id", payableIds));
        if (payables.size() != payableIds.size()) {
            throw new IllegalArgumentException("应付单不存在");
        }
        for (ErpAccountsPayable payable : payables) {
            if ("RED_FLUSHED".equals(payable.getStatus())) {
                throw new IllegalArgumentException("红冲应付单不可付款");
            }
            BigDecimal totalAmount = payable.getTotalAmount() == null ? BigDecimal.ZERO : payable.getTotalAmount();
            if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("金额为0的应付单不可付款");
            }
        }
        PayableMode payableMode = resolvePayableMode(payables);

        Long supplierId = request.supplierId();
        Long purchaseOrderId = request.purchaseOrderId();
        Long firstPayableId = payables.get(0).getId();
        Long resolvedSupplierId = payables.get(0).getSupplierId();
        boolean samePurchaseOrder = true;
        Long resolvedPurchaseOrderId = payables.get(0).getPurchaseOrderId();
        for (ErpAccountsPayable payable : payables) {
            if (!Objects.equals(payable.getSupplierId(), resolvedSupplierId)) {
                throw new IllegalArgumentException("应付单供应商不一致");
            }
            if (!Objects.equals(payable.getPurchaseOrderId(), resolvedPurchaseOrderId)) {
                samePurchaseOrder = false;
            }
        }
        if (supplierId == null) {
            supplierId = resolvedSupplierId;
        }
        if (!Objects.equals(supplierId, resolvedSupplierId)) {
            throw new IllegalArgumentException("供应商与应付单不一致");
        }
        if (purchaseOrderId == null && samePurchaseOrder) {
            purchaseOrderId = resolvedPurchaseOrderId;
        }
        if (supplierId == null) {
            throw new IllegalArgumentException("请选择供应商");
        }
        validateCashSettlementMethod(tenantId, request.settlementMethod());

        List<ErpPaymentPayable> allocations = null;
        boolean hasAllocations = request.allocations() != null && !request.allocations().isEmpty();
        if (hasAllocations) {
            allocations = buildAllocationsFromRequest(tenantId, payables, request.allocations());
            BigDecimal sumAmount = allocations.stream()
                .map(ErpPaymentPayable::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sumDiscount = allocations.stream()
                .map(ErpPaymentPayable::getAllocatedDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            amount = sumAmount;
            discountAmount = sumDiscount;
        } else {
            validateHeaderAmounts(payableMode, amount, discountAmount);
        }
        BigDecimal totalAllocate = amount.add(discountAmount);
        if (payableMode == PayableMode.MIXED && !hasAllocations) {
            throw new IllegalArgumentException("正负应付混合付款需填写分摊金额");
        }
        if (payableMode == PayableMode.RETURN) {
            if (totalAllocate.compareTo(BigDecimal.ZERO) >= 0) {
                throw new IllegalArgumentException("退款金额必须小于0");
            }
        } else if (payableMode != PayableMode.MIXED && totalAllocate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("付款金额或优惠金额必须大于0");
        }

        receipt.setPayableId(firstPayableId);
        receipt.setPurchaseOrderId(purchaseOrderId);
        receipt.setSupplierId(supplierId);
        receipt.setAmount(amount);
        receipt.setDiscountAmount(discountAmount);
        receipt.setSettlementMethod(request.settlementMethod());
        receipt.setPaymentMethodCode(request.paymentMethodCode());
        receipt.setPaidAt(parsePaidAt(request.paidAt()));
        receipt.setRemark(request.remark());
        receipt.setUpdatedAt(Instant.now());
        receipt.setUpdatedBy(CurrentActor.username());
        erpPaymentMapper.updateById(receipt);

        erpPaymentPayableMapper.delete(new QueryWrapper<ErpPaymentPayable>()
            .eq("tenant_id", tenantId)
            .eq("payment_id", receipt.getId()));
        if (allocations == null) {
            allocations = buildAllocations(tenantId, receipt.getId(), payables, amount, discountAmount, payableMode);
        } else {
            for (ErpPaymentPayable allocation : allocations) {
                allocation.setPaymentId(receipt.getId());
                allocation.setTenantId(tenantId);
                allocation.setCreatedAt(Instant.now());
            }
        }
        for (ErpPaymentPayable allocation : allocations) {
            erpPaymentPayableMapper.insert(allocation);
        }

        return buildDetail(tenantId, receipt);
    }

    private QueryWrapper<ErpPayment> baseWrapper(String keyword, String status, Long supplierId, Long payableId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpPayment> wrapper = new QueryWrapper<ErpPayment>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("payment_no", keyword.trim());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status.trim());
        }
        if (supplierId != null) {
            wrapper.eq("supplier_id", supplierId);
        }
        if (payableId != null) {
            wrapper.eq("payable_id", payableId);
        }
        if (startAt != null) {
            wrapper.ge("created_at", startAt);
        }
        if (endAt != null) {
            wrapper.le("created_at", endAt);
        }
        return wrapper;
    }

    private List<ErpPaymentView> mapViews(List<ErpPayment> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Set<Long> supplierIds = items.stream()
            .map(ErpPayment::getSupplierId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> supplierNameMap = new HashMap<>();
        if (!supplierIds.isEmpty()) {
            List<ErpSupplier> suppliers = erpSupplierMapper.selectBatchIds(supplierIds);
            for (ErpSupplier supplier : suppliers) {
                supplierNameMap.put(supplier.getId(), supplier.getName());
            }
        }
        return items.stream()
            .map(item -> new ErpPaymentView(
                item.getId(),
                item.getPaymentNo(),
                item.getSupplierId(),
                supplierNameMap.getOrDefault(item.getSupplierId(), "-"),
                item.getPayableId(),
                item.getAmount(),
                item.getDiscountAmount(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getRemark()
            ))
            .toList();
    }

    private String resolvePaymentNo(Long tenantId, String provided) {
        if (provided == null || provided.isBlank()) {
            return generatePaymentNo(tenantId);
        }
        String trimmed = provided.trim();
        ErpPayment existing = erpPaymentMapper.selectOne(new QueryWrapper<ErpPayment>()
            .eq("tenant_id", tenantId)
            .eq("payment_no", trimmed));
        if (existing != null) {
            throw new IllegalArgumentException("付款单号已存在");
        }
        return trimmed;
    }

    private String generatePaymentNo(Long tenantId) {
        String prefix = readConfig("erp.order.no.payment.prefix", "PY");
        String dateFormat = readConfig("erp.order.no.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.order.no.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, PAYMENT_ORDER_TYPE, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, PAYMENT_ORDER_TYPE, dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private String readConfig(String key, String fallback) {
        SystemConfig config = systemConfigMapper.findByKey(TenantContext.requireTenantId(), key);
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

    private Instant parsePaidAt(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        String trimmed = value.trim();
        if (trimmed.matches("^\\d+$")) {
            return Instant.ofEpochMilli(Long.parseLong(trimmed));
        }
        if (trimmed.contains("T")) {
            return Instant.parse(trimmed);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(trimmed, formatter).atZone(ZoneId.systemDefault()).toInstant();
    }

    @Override
    @Transactional
    public ErpPaymentDetail approve(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPayment receipt = loadPayment(tenantId, id);
        if (!STATUS_DRAFT.equals(receipt.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可审核");
        }
        validatePaymentApproval(tenantId, receipt);
        receipt.setStatus(STATUS_APPROVED);
        receipt.setUpdatedAt(Instant.now());
        receipt.setUpdatedBy(CurrentActor.username());
        erpPaymentMapper.updateById(receipt);

        List<ErpPaymentPayable> allocations = erpPaymentPayableMapper.findByPaymentId(tenantId, receipt.getId());
        if (allocations != null && !allocations.isEmpty()) {
            for (ErpPaymentPayable allocation : allocations) {
                applyPayablePaidAmount(
                    tenantId,
                    allocation.getPayableId(),
                    allocation.getAllocatedAmount(),
                    allocation.getAllocatedDiscount()
                );
            }
        } else {
            BigDecimal amountDelta = receipt.getAmount() == null ? BigDecimal.ZERO : receipt.getAmount();
            BigDecimal discountDelta = receipt.getDiscountAmount() == null ? BigDecimal.ZERO : receipt.getDiscountAmount();
            applyPayablePaidAmount(tenantId, receipt.getPayableId(), amountDelta, discountDelta);
        }
        return buildDetail(tenantId, receipt);
    }

    @Override
    public ErpPaymentSourcePayableDetail getSourcePayableDetail(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpAccountsPayable payable = erpAccountsPayableMapper.selectOne(new QueryWrapper<ErpAccountsPayable>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (payable == null) {
            throw new IllegalArgumentException("应付单不存在");
        }
        String supplierName = loadSupplierNameMap(singletonIdSet(payable.getSupplierId())).getOrDefault(payable.getSupplierId(), "");
        return new ErpPaymentSourcePayableDetail(
            payable.getId(),
            payable.getOrderNo(),
            payable.getSupplierId(),
            supplierName,
            payable.getTotalAmount(),
            payable.getPaidAmount(),
            payable.getDiscountAmount(),
            payable.getUnpaidAmount(),
            payable.getStatus(),
            payable.getSettlementMethod(),
            payable.getPurchaseOrderId(),
            payable.getPurchaseReturnId(),
            payable.getRemark(),
            payable.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public ErpPaymentDetail redFlush(Long id, String reason) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPayment receipt = loadPayment(tenantId, id);
        if (!STATUS_APPROVED.equals(receipt.getStatus())) {
            throw new IllegalArgumentException("仅已审核状态可红冲");
        }
        String reasonText = reason == null ? "" : reason.trim();
        if (reasonText.isEmpty()) {
            throw new IllegalArgumentException("请填写红冲原因");
        }
        receipt.setStatus(STATUS_RED_FLUSHED);
        String originRemark = receipt.getRemark();
        if (originRemark == null || originRemark.isBlank()) {
            receipt.setRemark("红冲原因：" + reasonText);
        } else {
            receipt.setRemark(originRemark + " | 红冲原因：" + reasonText);
        }
        receipt.setUpdatedAt(Instant.now());
        String operator = CurrentActor.username();
        receipt.setUpdatedBy(operator);
        erpPaymentMapper.updateById(receipt);

        ErpPayment redPayment = new ErpPayment();
        redPayment.setTenantId(tenantId);
        redPayment.setPayableId(receipt.getPayableId());
        redPayment.setPurchaseOrderId(receipt.getPurchaseOrderId());
        redPayment.setPaymentNo(generatePaymentNo(tenantId));
        redPayment.setSupplierId(receipt.getSupplierId());
        redPayment.setAmount(receipt.getAmount().negate());
        redPayment.setDiscountAmount(receipt.getDiscountAmount() == null ? BigDecimal.ZERO : receipt.getDiscountAmount().negate());
        redPayment.setSettlementMethod(receipt.getSettlementMethod());
        redPayment.setPaymentMethodCode(receipt.getPaymentMethodCode());
        redPayment.setStatus(STATUS_APPROVED);
        redPayment.setPaidAt(Instant.now());
        redPayment.setRemark("红冲付款单：" + reasonText);
        redPayment.setCreatedAt(Instant.now());
        redPayment.setCreatedBy(operator);
        redPayment.setUpdatedAt(Instant.now());
        redPayment.setUpdatedBy(operator);
        erpPaymentMapper.insert(redPayment);

        List<ErpPaymentPayable> allocations = erpPaymentPayableMapper.findByPaymentId(tenantId, receipt.getId());
        if (allocations != null && !allocations.isEmpty()) {
            for (ErpPaymentPayable allocation : allocations) {
                ErpPaymentPayable redAllocation = new ErpPaymentPayable();
                redAllocation.setTenantId(tenantId);
                redAllocation.setPaymentId(redPayment.getId());
                redAllocation.setPayableId(allocation.getPayableId());
                redAllocation.setAllocatedAmount(allocation.getAllocatedAmount().negate());
                redAllocation.setAllocatedDiscount(allocation.getAllocatedDiscount().negate());
                redAllocation.setAllocatedTotal(allocation.getAllocatedTotal().negate());
                redAllocation.setCreatedAt(Instant.now());
                erpPaymentPayableMapper.insert(redAllocation);
                applyPayablePaidAmount(
                    tenantId,
                    allocation.getPayableId(),
                    allocation.getAllocatedAmount().negate(),
                    allocation.getAllocatedDiscount().negate()
                );
            }
        } else {
            BigDecimal amountDelta = receipt.getAmount() == null ? BigDecimal.ZERO : receipt.getAmount();
            BigDecimal discountDelta = receipt.getDiscountAmount() == null ? BigDecimal.ZERO : receipt.getDiscountAmount();
            applyPayablePaidAmount(tenantId, receipt.getPayableId(), amountDelta.negate(), discountDelta.negate());
        }
        return buildDetail(tenantId, receipt);
    }

    private ErpPayment loadPayment(Long tenantId, Long id) {
        ErpPayment receipt = erpPaymentMapper.selectOne(new QueryWrapper<ErpPayment>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (receipt == null) {
            throw new IllegalArgumentException("付款单不存在");
        }
        return receipt;
    }

    private ErpPaymentDetail buildDetail(Long tenantId, ErpPayment receipt) {
        Map<Long, String> supplierNameMap = loadSupplierNameMap(singletonIdSet(receipt.getSupplierId()));
        String supplierName = supplierNameMap.getOrDefault(receipt.getSupplierId(), "-");
        Map<Long, ErpPurchaseOrder> purchaseOrderMap = loadPurchaseOrderMap(tenantId, singletonIdSet(receipt.getPurchaseOrderId()));
        ErpPurchaseOrder order = purchaseOrderMap.get(receipt.getPurchaseOrderId());
        String orderNo = order == null ? null : order.getOrderNo();
        List<ErpPaymentPayable> allocations = erpPaymentPayableMapper.findByPaymentId(tenantId, receipt.getId());
        List<ErpAccountsPayable> payables = loadPayables(tenantId, receipt, allocations);
        List<ErpPaymentPayableView> payableViews = buildPayableViews(payables, allocations);
        String payableNo = payableViews.isEmpty() ? null : payableViews.get(0).orderNo();
        return new ErpPaymentDetail(receipt, supplierName, orderNo, payableNo, payableViews);
    }

    private List<ErpAccountsPayable> loadPayables(Long tenantId,
                                                        ErpPayment receipt,
                                                        List<ErpPaymentPayable> allocations) {
        List<Long> ids = new ArrayList<>();
        if (allocations != null && !allocations.isEmpty()) {
            for (ErpPaymentPayable allocation : allocations) {
                if (allocation.getPayableId() != null) {
                    ids.add(allocation.getPayableId());
                }
            }
        }
        if (ids.isEmpty() && receipt.getPayableId() != null) {
            ids.add(receipt.getPayableId());
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(loadPayableMap(tenantId, ids).values());
    }

    private List<ErpPaymentPayableView> buildPayableViews(List<ErpAccountsPayable> payables,
                                                                List<ErpPaymentPayable> allocations) {
        if (payables == null || payables.isEmpty()) {
            return List.of();
        }
        Map<Long, ErpAccountsPayable> payableMap = payables.stream()
            .collect(Collectors.toMap(ErpAccountsPayable::getId, item -> item));
        List<ErpPaymentPayableView> views = new ArrayList<>();
        if (allocations != null && !allocations.isEmpty()) {
            for (ErpPaymentPayable allocation : allocations) {
                Long payableId = allocation.getPayableId();
                ErpAccountsPayable payable = payableMap.get(payableId);
                String orderNo = payable == null ? null : payable.getOrderNo();
                views.add(new ErpPaymentPayableView(
                    payableId,
                    orderNo,
                    allocation.getAllocatedAmount(),
                    allocation.getAllocatedDiscount(),
                    allocation.getAllocatedTotal()
                ));
            }
            return views;
        }
        for (ErpAccountsPayable payable : payables) {
            views.add(new ErpPaymentPayableView(
                payable.getId(),
                payable.getOrderNo(),
                null,
                null,
                null
            ));
        }
        return views;
    }

    private void applyPayablePaidAmount(Long tenantId, Long payableId, BigDecimal amountDelta, BigDecimal discountDelta) {
        if (payableId == null || amountDelta == null || discountDelta == null) {
            return;
        }
        ErpAccountsPayable payable = erpAccountsPayableMapper.selectOne(new QueryWrapper<ErpAccountsPayable>()
            .eq("tenant_id", tenantId)
            .eq("id", payableId));
        if (payable == null) {
            return;
        }
        BigDecimal paid = payable.getPaidAmount() == null ? BigDecimal.ZERO : payable.getPaidAmount();
        BigDecimal discount = payable.getDiscountAmount() == null ? BigDecimal.ZERO : payable.getDiscountAmount();
        BigDecimal total = payable.getTotalAmount() == null ? BigDecimal.ZERO : payable.getTotalAmount();
        BigDecimal unpaidBefore = payable.getUnpaidAmount() == null ? BigDecimal.ZERO : payable.getUnpaidAmount();
        boolean returnPayable = isReturnPayable(payable);
        BigDecimal applyTotal = amountDelta.add(discountDelta);
        if (!returnPayable && applyTotal.compareTo(BigDecimal.ZERO) > 0 && applyTotal.compareTo(unpaidBefore) > 0) {
            throw new IllegalArgumentException("付款金额不能大于未付金额");
        }
        if (returnPayable && applyTotal.compareTo(BigDecimal.ZERO) < 0 && applyTotal.compareTo(unpaidBefore) < 0) {
            throw new IllegalArgumentException("付款金额不能大于未付金额");
        }
        BigDecimal newPaid = paid.add(amountDelta);
        BigDecimal newDiscount = discount.add(discountDelta);
        if (!returnPayable) {
            if (newPaid.compareTo(BigDecimal.ZERO) < 0) {
                newPaid = BigDecimal.ZERO;
            }
            if (newDiscount.compareTo(BigDecimal.ZERO) < 0) {
                newDiscount = BigDecimal.ZERO;
            }
        } else {
            if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
                newPaid = BigDecimal.ZERO;
            }
            if (newDiscount.compareTo(BigDecimal.ZERO) > 0) {
                newDiscount = BigDecimal.ZERO;
            }
        }
        BigDecimal totalApplied = newPaid.add(newDiscount);
        if (!returnPayable && totalApplied.compareTo(total) > 0) {
            BigDecimal overflow = totalApplied.subtract(total);
            if (newDiscount.compareTo(overflow) >= 0) {
                newDiscount = newDiscount.subtract(overflow);
            } else {
                newPaid = newPaid.subtract(overflow.subtract(newDiscount));
                if (newPaid.compareTo(BigDecimal.ZERO) < 0) {
                    newPaid = BigDecimal.ZERO;
                }
                newDiscount = BigDecimal.ZERO;
            }
        } else if (returnPayable && totalApplied.compareTo(total) < 0) {
            BigDecimal overflow = total.subtract(totalApplied);
            BigDecimal discountCapacity = newDiscount.abs();
            if (discountCapacity.compareTo(overflow) >= 0) {
                newDiscount = newDiscount.add(overflow);
            } else {
                BigDecimal remaining = overflow.subtract(discountCapacity);
                newDiscount = BigDecimal.ZERO;
                newPaid = newPaid.add(remaining);
                if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
                    newPaid = BigDecimal.ZERO;
                }
            }
        }
        BigDecimal unpaid = total.subtract(newPaid.add(newDiscount));
        if (!returnPayable && unpaid.compareTo(BigDecimal.ZERO) < 0) {
            unpaid = BigDecimal.ZERO;
        }
        payable.setPaidAmount(newPaid);
        payable.setDiscountAmount(newDiscount);
        payable.setUnpaidAmount(unpaid);
        if (!"RED_FLUSHED".equals(payable.getStatus())) {
            payable.setStatus(unpaid.compareTo(BigDecimal.ZERO) == 0 ? STATUS_SETTLED : STATUS_OPEN);
        }
        payable.setUpdatedAt(Instant.now());
        erpAccountsPayableMapper.updateById(payable);
    }

    private void validatePaymentApproval(Long tenantId, ErpPayment payment) {
        validateCashSettlementMethod(tenantId, payment.getSettlementMethod());
        List<ErpPaymentPayable> allocations = erpPaymentPayableMapper.findByPaymentId(tenantId, payment.getId());
        if (allocations != null && !allocations.isEmpty()) {
            Map<Long, BigDecimal> payableDeltas = new LinkedHashMap<>();
            for (ErpPaymentPayable allocation : allocations) {
                if (allocation == null) {
                    continue;
                }
                Long payableId = allocation.getPayableId();
                BigDecimal delta = allocation.getAllocatedTotal();
                validatePayableDelta(payableId, delta);
                payableDeltas.merge(payableId, delta, BigDecimal::add);
            }
            ensurePayableCapacities(tenantId, payableDeltas);
            return;
        }
        Map<Long, BigDecimal> payableDeltas = new LinkedHashMap<>();
        payableDeltas.put(payment.getPayableId(), resolvePaymentTotal(payment.getAmount(), payment.getDiscountAmount()));
        ensurePayableCapacities(tenantId, payableDeltas);
    }

    private void validateCashSettlementMethod(Long tenantId, String settlementMethodCode) {
        if (settlementMethodCode == null || settlementMethodCode.isBlank()) {
            throw new IllegalArgumentException("请选择结算方式");
        }
        ErpSettlementMethod method = erpSettlementMethodMapper.findByCode(tenantId, settlementMethodCode.trim());
        if (method == null || Boolean.FALSE.equals(method.getEnabled())) {
            throw new IllegalArgumentException("结算方式不存在或已停用");
        }
        if ("HIDDEN".equalsIgnoreCase(method.getFundInputMode())) {
            throw new IllegalArgumentException("付款单不允许使用挂账类结算方式");
        }
    }

    private void ensurePayableCapacity(Long tenantId, Long payableId, BigDecimal delta) {
        Map<Long, BigDecimal> payableDeltas = new LinkedHashMap<>();
        payableDeltas.put(payableId, delta);
        ensurePayableCapacities(tenantId, payableDeltas);
    }

    private void ensurePayableCapacities(Long tenantId, Map<Long, BigDecimal> payableDeltas) {
        if (payableDeltas == null || payableDeltas.isEmpty()) {
            throw new IllegalArgumentException("应付单不存在");
        }
        for (Map.Entry<Long, BigDecimal> entry : payableDeltas.entrySet()) {
            validatePayableDelta(entry.getKey(), entry.getValue());
        }
        Map<Long, ErpAccountsPayable> payableMap = loadPayableMap(tenantId, payableDeltas.keySet());
        for (Map.Entry<Long, BigDecimal> entry : payableDeltas.entrySet()) {
            ErpAccountsPayable payable = payableMap.get(entry.getKey());
            if (payable == null) {
                throw new IllegalArgumentException("应付单不存在");
            }
            if (STATUS_RED_FLUSHED.equals(payable.getStatus())) {
                throw new IllegalArgumentException("红冲应付单不可付款");
            }
            BigDecimal unpaid = payable.getUnpaidAmount() == null ? BigDecimal.ZERO : payable.getUnpaidAmount();
            if (!isAllocationWithinUnpaid(payable, entry.getValue(), unpaid)) {
                throw new IllegalArgumentException("付款金额不能大于未付金额");
            }
        }
    }

    private void validatePayableDelta(Long payableId, BigDecimal delta) {
        if (payableId == null) {
            throw new IllegalArgumentException("应付单不存在");
        }
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("付款金额或优惠金额必须大于0");
        }
    }

    private Map<Long, String> loadSupplierNameMap(Set<Long> supplierIds) {
        Set<Long> effectiveIds = supplierIds == null
            ? Set.of()
            : supplierIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (effectiveIds.isEmpty()) {
            return Map.of();
        }
        return erpSupplierMapper.selectBatchIds(effectiveIds).stream()
            .collect(Collectors.toMap(ErpSupplier::getId, ErpSupplier::getName, (a, b) -> a));
    }

    private Map<Long, ErpPurchaseOrder> loadPurchaseOrderMap(Long tenantId, Set<Long> purchaseOrderIds) {
        Set<Long> effectiveIds = purchaseOrderIds == null
            ? Set.of()
            : purchaseOrderIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (effectiveIds.isEmpty()) {
            return Map.of();
        }
        return erpPurchaseOrderMapper.selectList(new QueryWrapper<ErpPurchaseOrder>()
                .eq("tenant_id", tenantId)
                .in("id", effectiveIds))
            .stream()
            .collect(Collectors.toMap(ErpPurchaseOrder::getId, item -> item, (a, b) -> a));
    }

    private Map<Long, ErpAccountsPayable> loadPayableMap(Long tenantId, Collection<Long> payableIds) {
        Set<Long> effectiveIds = payableIds == null
            ? Set.of()
            : payableIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (effectiveIds.isEmpty()) {
            return Map.of();
        }
        return erpAccountsPayableMapper.selectList(new QueryWrapper<ErpAccountsPayable>()
                .eq("tenant_id", tenantId)
                .in("id", effectiveIds))
            .stream()
            .collect(Collectors.toMap(ErpAccountsPayable::getId, item -> item, (a, b) -> a));
    }

    private Set<Long> singletonIdSet(Long id) {
        return id == null ? Set.of() : Set.of(id);
    }

    private BigDecimal resolvePaymentTotal(BigDecimal amount, BigDecimal discountAmount) {
        BigDecimal normalizedAmount = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal normalizedDiscount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        return normalizedAmount.add(normalizedDiscount);
    }

    private List<ErpPaymentPayable> buildAllocations(Long tenantId,
                                                        Long receiptId,
                                                        List<ErpAccountsPayable> payables,
                                                        BigDecimal amount,
                                                        BigDecimal discountAmount,
                                                        PayableMode payableMode) {
        if (payableMode == PayableMode.MIXED) {
            throw new IllegalArgumentException("正负应付混合付款需填写分摊金额");
        }
        int count = payables.size();
        BigDecimal totalAllocate = amount.add(discountAmount);
        List<BigDecimal> weights = new ArrayList<>(count);
        List<BigDecimal> capacities = new ArrayList<>(count);
        BigDecimal totalUnpaid = BigDecimal.ZERO;
        for (ErpAccountsPayable payable : payables) {
            BigDecimal unpaid = absUnpaid(payable);
            weights.add(unpaid);
            capacities.add(unpaid);
            totalUnpaid = totalUnpaid.add(unpaid);
        }

        if (totalUnpaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("未付金额为0，无法付款");
        }
        BigDecimal absTotalAllocate = totalAllocate.abs();
        if (absTotalAllocate.compareTo(totalUnpaid) > 0) {
            throw new IllegalArgumentException("付款金额不能大于未付金额");
        }

        List<BigDecimal> totalAllocations = distributeByWeight(absTotalAllocate, weights, capacities);
        List<BigDecimal> amountAllocations = distributeByWeight(amount.abs(), totalAllocations, totalAllocations);
        List<BigDecimal> discountAllocations = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            discountAllocations.add(totalAllocations.get(i).subtract(amountAllocations.get(i)));
        }
        if (payableMode == PayableMode.RETURN) {
            totalAllocations = totalAllocations.stream().map(BigDecimal::negate).toList();
            amountAllocations = amountAllocations.stream().map(BigDecimal::negate).toList();
            discountAllocations = discountAllocations.stream().map(BigDecimal::negate).toList();
        }
        List<ErpPaymentPayable> allocations = new ArrayList<>();
        for (int i = 0; i < payables.size(); i++) {
            ErpAccountsPayable payable = payables.get(i);
            BigDecimal allocAmount = amountAllocations.get(i);
            BigDecimal allocDiscount = discountAllocations.get(i);
            BigDecimal allocTotal = totalAllocations.get(i);
            BigDecimal unpaid = payable.getUnpaidAmount() == null ? BigDecimal.ZERO : payable.getUnpaidAmount();
            if (!isAllocationWithinUnpaid(payable, allocTotal, unpaid)) {
                throw new IllegalArgumentException("付款金额不能大于未付金额");
            }
            ErpPaymentPayable allocation = new ErpPaymentPayable();
            allocation.setTenantId(tenantId);
            allocation.setPaymentId(receiptId);
            allocation.setPayableId(payable.getId());
            allocation.setAllocatedAmount(allocAmount);
            allocation.setAllocatedDiscount(allocDiscount);
            allocation.setAllocatedTotal(allocTotal);
            allocation.setCreatedAt(Instant.now());
            allocations.add(allocation);
        }
        return allocations;
    }

    private List<ErpPaymentPayable> buildAllocationsFromRequest(Long tenantId,
                                                                   List<ErpAccountsPayable> payables,
                                                                   List<ErpPaymentAllocationRequest> allocationRequests) {
        Map<Long, ErpPaymentAllocationRequest> allocationMap = new HashMap<>();
        for (ErpPaymentAllocationRequest request : allocationRequests) {
            if (request == null || request.payableId() == null) {
                continue;
            }
            if (allocationMap.containsKey(request.payableId())) {
                throw new IllegalArgumentException("应付单分摊重复");
            }
            allocationMap.put(request.payableId(), request);
        }
        if (allocationMap.isEmpty()) {
            throw new IllegalArgumentException("请填写分摊金额");
        }
        List<ErpPaymentPayable> allocations = new ArrayList<>();
        for (ErpAccountsPayable payable : payables) {
            ErpPaymentAllocationRequest request = allocationMap.get(payable.getId());
            if (request == null) {
                throw new IllegalArgumentException("应付单分摊不完整");
            }
            BigDecimal amount = request.amount() == null ? BigDecimal.ZERO : request.amount();
            BigDecimal discount = request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount();
            if (!isAllocationSignValid(payable, amount, discount)) {
                throw new IllegalArgumentException("分摊金额方向不正确");
            }
            BigDecimal allocTotal = amount.add(discount);
            BigDecimal unpaid = payable.getUnpaidAmount() == null ? BigDecimal.ZERO : payable.getUnpaidAmount();
            if (!isAllocationWithinUnpaid(payable, allocTotal, unpaid)) {
                throw new IllegalArgumentException("分摊金额不能大于未付金额");
            }
            ErpPaymentPayable allocation = new ErpPaymentPayable();
            allocation.setPayableId(payable.getId());
            allocation.setAllocatedAmount(amount);
            allocation.setAllocatedDiscount(discount);
            allocation.setAllocatedTotal(allocTotal);
            allocations.add(allocation);
        }
        return allocations;
    }

    private PayableMode resolvePayableMode(List<ErpAccountsPayable> payables) {
        boolean hasReturn = false;
        boolean hasNormal = false;
        for (ErpAccountsPayable payable : payables) {
            if (isReturnPayable(payable)) {
                hasReturn = true;
            } else {
                hasNormal = true;
            }
        }
        if (hasReturn && hasNormal) {
            return PayableMode.MIXED;
        }
        if (hasReturn) {
            return PayableMode.RETURN;
        }
        return PayableMode.NORMAL;
    }

    private boolean isReturnPayable(ErpAccountsPayable payable) {
        if (payable == null) {
            return false;
        }
        if (payable.getPurchaseReturnId() != null) {
            return true;
        }
        BigDecimal unpaid = payable.getUnpaidAmount();
        return unpaid != null && unpaid.compareTo(BigDecimal.ZERO) < 0;
    }

    private void validateHeaderAmounts(PayableMode payableMode, BigDecimal amount, BigDecimal discountAmount) {
        if (payableMode == PayableMode.RETURN) {
            if (amount.compareTo(BigDecimal.ZERO) > 0 || discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException("退货应付金额不能大于0");
            }
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0 || discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金额不能小于0");
        }
    }

    private BigDecimal absUnpaid(ErpAccountsPayable payable) {
        BigDecimal unpaid = payable.getUnpaidAmount() == null ? BigDecimal.ZERO : payable.getUnpaidAmount();
        return unpaid.abs();
    }

    private boolean isAllocationSignValid(ErpAccountsPayable payable, BigDecimal amount, BigDecimal discount) {
        if (isReturnPayable(payable)) {
            return amount.compareTo(BigDecimal.ZERO) <= 0 && discount.compareTo(BigDecimal.ZERO) <= 0;
        }
        return amount.compareTo(BigDecimal.ZERO) >= 0 && discount.compareTo(BigDecimal.ZERO) >= 0;
    }

    private boolean isAllocationWithinUnpaid(ErpAccountsPayable payable, BigDecimal allocTotal, BigDecimal unpaid) {
        if (isReturnPayable(payable)) {
            return allocTotal.compareTo(unpaid) >= 0;
        }
        return allocTotal.compareTo(unpaid) <= 0;
    }

    private List<BigDecimal> distributeByWeight(BigDecimal total,
                                                List<BigDecimal> weights,
                                                List<BigDecimal> capacities) {
        int count = weights.size();
        List<BigDecimal> results = new ArrayList<>(count);
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            for (int i = 0; i < count; i++) {
                results.add(BigDecimal.ZERO.setScale(2, RoundingMode.DOWN));
            }
            return results;
        }
        BigDecimal sumWeight = BigDecimal.ZERO;
        for (BigDecimal weight : weights) {
            if (weight != null && weight.compareTo(BigDecimal.ZERO) > 0) {
                sumWeight = sumWeight.add(weight);
            }
        }
        if (sumWeight.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal per = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
            for (int i = 0; i < count; i++) {
                results.add(per);
            }
        } else {
            for (BigDecimal weight : weights) {
                if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
                    results.add(BigDecimal.ZERO.setScale(2, RoundingMode.DOWN));
                    continue;
                }
                BigDecimal alloc = total.multiply(weight)
                    .divide(sumWeight, 2, RoundingMode.DOWN);
                results.add(alloc);
            }
        }

        BigDecimal allocated = BigDecimal.ZERO;
        for (BigDecimal value : results) {
            allocated = allocated.add(value);
        }
        BigDecimal remainder = total.subtract(allocated);
        BigDecimal cent = new BigDecimal("0.01");
        int guard = 0;
        while (remainder.compareTo(cent) >= 0 && guard < 10000) {
            boolean assigned = false;
            for (int i = 0; i < count && remainder.compareTo(cent) >= 0; i++) {
                BigDecimal capacity = capacities == null ? null : capacities.get(i);
                if (capacity != null) {
                    BigDecimal remainCap = capacity.subtract(results.get(i));
                    if (remainCap.compareTo(cent) < 0) {
                        continue;
                    }
                }
                results.set(i, results.get(i).add(cent));
                remainder = remainder.subtract(cent);
                assigned = true;
            }
            if (!assigned) {
                break;
            }
            guard++;
        }
        if (remainder.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("付款金额不能大于未付金额");
        }
        return results;
    }
}
