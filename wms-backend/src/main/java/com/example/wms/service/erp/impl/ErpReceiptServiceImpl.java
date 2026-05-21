package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpReceiptAllocationRequest;
import com.example.wms.dto.erp.ErpReceiptCreateRequest;
import com.example.wms.dto.erp.ErpReceiptDetail;
import com.example.wms.dto.erp.ErpReceiptReceivableView;
import com.example.wms.dto.erp.ErpReceiptSourceReceivableDetail;
import com.example.wms.dto.erp.ErpReceiptSourceReceivableOption;
import com.example.wms.dto.erp.ErpReceiptView;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpAccountsReceivable;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpReceipt;
import com.example.wms.entity.erp.ErpReceiptReceivable;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.entity.erp.ErpSettlementMethod;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpReceiptReceivableMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.security.CurrentActor;
import com.example.wms.service.erp.ErpReceiptService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// ERP收款单服务实现
@Service
public class ErpReceiptServiceImpl implements ErpReceiptService {
    private static final String RECEIPT_ORDER_TYPE = "RECEIPT";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_RED_FLUSHED = "RED_FLUSHED";
    private static final String STATUS_SETTLED = "SETTLED";
    private static final String STATUS_OPEN = "OPEN";
    private enum ReceiptMode {
        NORMAL,
        RETURN,
        MIXED
    }
    private final ErpReceiptMapper erpReceiptMapper;
    private final ErpReceiptReceivableMapper erpReceiptReceivableMapper;
    private final ErpCustomerMapper erpCustomerMapper;
    private final ErpAccountsReceivableMapper erpAccountsReceivableMapper;
    private final ErpSaleOrderMapper erpSaleOrderMapper;
    private final ErpSettlementMethodMapper erpSettlementMethodMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;

    public ErpReceiptServiceImpl(ErpReceiptMapper erpReceiptMapper,
                                 ErpReceiptReceivableMapper erpReceiptReceivableMapper,
                                 ErpCustomerMapper erpCustomerMapper,
                                 ErpAccountsReceivableMapper erpAccountsReceivableMapper,
                                 ErpSaleOrderMapper erpSaleOrderMapper,
                                 ErpSettlementMethodMapper erpSettlementMethodMapper,
                                 ErpOrderSequenceMapper erpOrderSequenceMapper,
                                 SystemConfigMapper systemConfigMapper) {
        this.erpReceiptMapper = erpReceiptMapper;
        this.erpReceiptReceivableMapper = erpReceiptReceivableMapper;
        this.erpCustomerMapper = erpCustomerMapper;
        this.erpAccountsReceivableMapper = erpAccountsReceivableMapper;
        this.erpSaleOrderMapper = erpSaleOrderMapper;
        this.erpSettlementMethodMapper = erpSettlementMethodMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
    }

    @Override
    public List<ErpReceiptView> listAll(String keyword, String status, Long customerId, Long receivableId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpReceipt> wrapper = baseWrapper(keyword, status, customerId, receivableId, startAt, endAt);
        wrapper.orderByDesc("created_at");
        List<ErpReceipt> items = erpReceiptMapper.selectList(wrapper);
        return mapViews(items);
    }

    @Override
    public PageResponse<ErpReceiptView> page(long page, long size, String keyword, String status, Long customerId, Long receivableId, Instant startAt, Instant endAt) {
        Page<ErpReceipt> pageReq = Page.of(page, size);
        QueryWrapper<ErpReceipt> wrapper = baseWrapper(keyword, status, customerId, receivableId, startAt, endAt);
        wrapper.orderByDesc("created_at");
        Page<ErpReceipt> result = erpReceiptMapper.selectPage(pageReq, wrapper);
        List<ErpReceiptView> views = mapViews(result.getRecords());
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), views);
    }

    @Override
    public PageResponse<ErpReceiptSourceReceivableOption> sourceReceivablePage(long page, long size, String keyword, Long customerId, String status, Instant startAt, Instant endAt) {
        Long tenantId = TenantContext.requireTenantId();
        long finalPage = page <= 0 ? 1 : page;
        long finalSize = size <= 0 ? 20 : Math.min(size, 200);
        QueryWrapper<ErpAccountsReceivable> wrapper = new QueryWrapper<ErpAccountsReceivable>()
            .eq("tenant_id", tenantId)
            .isNull("deleted_at");
        if (customerId != null) {
            wrapper.eq("customer_id", customerId);
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
        Page<ErpAccountsReceivable> result = erpAccountsReceivableMapper.selectPage(Page.of(finalPage, finalSize), wrapper);
        List<ErpReceiptSourceReceivableOption> items = result.getRecords().stream()
            .map(item -> new ErpReceiptSourceReceivableOption(
                item.getId(),
                item.getOrderNo(),
                item.getCustomerId(),
                item.getTotalAmount(),
                item.getPaidAmount(),
                item.getUnpaidAmount(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getSourceType(),
                item.getSourceId()
            ))
            .toList();
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), items);
    }

    @Override
    public ErpReceiptDetail getDetail(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpReceipt receipt = erpReceiptMapper.selectOne(new QueryWrapper<ErpReceipt>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (receipt == null) {
            throw new IllegalArgumentException("收款单不存在");
        }
        return buildDetail(tenantId, receipt);
    }

    @Override
    public ErpReceiptSourceReceivableDetail getSourceReceivableDetail(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpAccountsReceivable receivable = erpAccountsReceivableMapper.selectOne(new QueryWrapper<ErpAccountsReceivable>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (receivable == null) {
            throw new IllegalArgumentException("应收单不存在");
        }
        String customerName = "";
        if (receivable.getCustomerId() != null) {
            ErpCustomer customer = erpCustomerMapper.selectById(receivable.getCustomerId());
            customerName = customer == null ? "" : customer.getName();
        }
        return new ErpReceiptSourceReceivableDetail(
            receivable.getId(),
            receivable.getOrderNo(),
            receivable.getCustomerId(),
            customerName,
            receivable.getTotalAmount(),
            receivable.getPaidAmount(),
            receivable.getUnpaidAmount(),
            receivable.getStatus(),
            receivable.getSettlementMethod(),
            receivable.getSourceType(),
            receivable.getSourceId(),
            receivable.getRemark(),
            receivable.getCreatedAt()
        );
    }

    @Override
    public String nextReceiptNo() {
        Long tenantId = TenantContext.requireTenantId();
        return generateReceiptNo(tenantId);
    }

    @Override
    @Transactional
    public ErpReceiptDetail create(ErpReceiptCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String operator = CurrentActor.username();
        BigDecimal amount = request.amount() == null ? BigDecimal.ZERO : request.amount();
        BigDecimal discountAmount = request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount();

        List<Long> receivableIds = new ArrayList<>();
        if (request.receivableIds() != null) {
            for (Long id : request.receivableIds()) {
                if (id != null) {
                    receivableIds.add(id);
                }
            }
        }
        if (receivableIds.isEmpty() && request.receivableId() != null) {
            receivableIds.add(request.receivableId());
        }
        if (receivableIds.isEmpty() && request.allocations() != null) {
            for (var alloc : request.allocations()) {
                if (alloc != null && alloc.receivableId() != null) {
                    receivableIds.add(alloc.receivableId());
                }
            }
        }
        if (receivableIds.isEmpty()) {
            throw new IllegalArgumentException("请选择应收单");
        }

        List<ErpAccountsReceivable> receivables = erpAccountsReceivableMapper.selectList(new QueryWrapper<ErpAccountsReceivable>()
            .eq("tenant_id", tenantId)
            .in("id", receivableIds));
        if (receivables.size() != receivableIds.size()) {
            throw new IllegalArgumentException("应收单不存在");
        }
        for (ErpAccountsReceivable receivable : receivables) {
            if ("RED_FLUSHED".equals(receivable.getStatus())) {
                throw new IllegalArgumentException("红冲应收单不可收款");
            }
            BigDecimal totalAmount = receivable.getTotalAmount() == null ? BigDecimal.ZERO : receivable.getTotalAmount();
            if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("金额为0的应收单不可收款");
            }
        }
        ReceiptMode receiptMode = resolveReceiptMode(receivables);
        boolean returnMode = receiptMode == ReceiptMode.RETURN;
        boolean mixedMode = receiptMode == ReceiptMode.MIXED;
        boolean hasAllocations = request.allocations() != null && !request.allocations().isEmpty();
        if (!hasAllocations) {
            if (!returnMode && !mixedMode) {
                if (amount.compareTo(BigDecimal.ZERO) < 0 || discountAmount.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("金额不能小于0");
                }
            } else if (returnMode) {
                if (amount.compareTo(BigDecimal.ZERO) > 0 || discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                    throw new IllegalArgumentException("退货应收金额不能大于0");
                }
            }
        }

        Long customerId = request.customerId();
        Long saleOrderId = request.saleOrderId();
        Long firstReceivableId = receivables.get(0).getId();
        Long resolvedCustomerId = receivables.get(0).getCustomerId();
        boolean sameSaleOrder = true;
        Long resolvedSaleOrderId = receivables.get(0).getSaleOrderId();
        for (ErpAccountsReceivable receivable : receivables) {
            if (!Objects.equals(receivable.getCustomerId(), resolvedCustomerId)) {
                throw new IllegalArgumentException("应收单客户不一致");
            }
            if (!Objects.equals(receivable.getSaleOrderId(), resolvedSaleOrderId)) {
                sameSaleOrder = false;
            }
        }
        if (customerId == null) {
            customerId = resolvedCustomerId;
        }
        if (!Objects.equals(customerId, resolvedCustomerId)) {
            throw new IllegalArgumentException("客户与应收单不一致");
        }
        if (saleOrderId == null && sameSaleOrder) {
            saleOrderId = resolvedSaleOrderId;
        }
        if (customerId == null) {
            throw new IllegalArgumentException("请选择客户");
        }
        validateCashSettlementMethod(tenantId, request.settlementMethod());

        List<ErpReceiptReceivable> allocations = null;
        if (request.allocations() != null && !request.allocations().isEmpty()) {
            allocations = buildAllocationsFromRequest(tenantId, receivables, request.allocations(), returnMode, mixedMode);
            BigDecimal sumAmount = allocations.stream()
                .map(ErpReceiptReceivable::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sumDiscount = allocations.stream()
                .map(ErpReceiptReceivable::getAllocatedDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            amount = sumAmount;
            discountAmount = sumDiscount;
        } else if (mixedMode) {
            throw new IllegalArgumentException("混收需填写分摊金额");
        }
        BigDecimal totalAllocate = amount.add(discountAmount);
        if (!returnMode && !mixedMode) {
            if (totalAllocate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("收款金额或优惠金额必须大于0");
            }
        } else if (returnMode) {
            if (totalAllocate.compareTo(BigDecimal.ZERO) >= 0) {
                throw new IllegalArgumentException("冲减金额必须小于0");
            }
        }

        ErpReceipt receipt = new ErpReceipt();
        receipt.setTenantId(tenantId);
        receipt.setReceivableId(firstReceivableId);
        receipt.setSaleOrderId(saleOrderId);
        receipt.setReceiptNo(resolveReceiptNo(tenantId, request.receiptNo()));
        receipt.setCustomerId(customerId);
        receipt.setAmount(amount);
        receipt.setDiscountAmount(discountAmount);
        receipt.setSettlementMethod(request.settlementMethod());
        receipt.setReceiptMethodCode(request.receiptMethodCode());
        receipt.setStatus(STATUS_DRAFT);
        receipt.setReceivedAt(parseReceivedAt(request.receivedAt()));
        receipt.setRemark(request.remark());
        receipt.setCreatedAt(Instant.now());
        receipt.setCreatedBy(operator);
        receipt.setUpdatedAt(Instant.now());
        receipt.setUpdatedBy(operator);
        erpReceiptMapper.insert(receipt);

        if (allocations == null) {
            if (mixedMode) {
                throw new IllegalArgumentException("混收需填写分摊金额");
            }
            allocations = buildAllocations(tenantId, receipt.getId(), receivables, amount, discountAmount, returnMode);
        } else {
            for (ErpReceiptReceivable allocation : allocations) {
                allocation.setReceiptId(receipt.getId());
                allocation.setTenantId(tenantId);
                allocation.setCreatedAt(Instant.now());
            }
        }
        for (ErpReceiptReceivable allocation : allocations) {
            erpReceiptReceivableMapper.insert(allocation);
        }

        // 草稿不影响应收金额，审核时更新

        return buildDetail(tenantId, receipt);
    }

    @Override
    @Transactional
    public ErpReceiptDetail update(Long id, ErpReceiptCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpReceipt receipt = loadReceipt(tenantId, id);
        if (!STATUS_DRAFT.equals(receipt.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可修改");
        }

        BigDecimal amount = request.amount() == null ? BigDecimal.ZERO : request.amount();
        BigDecimal discountAmount = request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount();

        List<Long> receivableIds = new ArrayList<>();
        if (request.receivableIds() != null) {
            for (Long rid : request.receivableIds()) {
                if (rid != null) {
                    receivableIds.add(rid);
                }
            }
        }
        if (receivableIds.isEmpty() && request.receivableId() != null) {
            receivableIds.add(request.receivableId());
        }
        if (receivableIds.isEmpty() && request.allocations() != null) {
            for (var alloc : request.allocations()) {
                if (alloc != null && alloc.receivableId() != null) {
                    receivableIds.add(alloc.receivableId());
                }
            }
        }
        if (receivableIds.isEmpty()) {
            throw new IllegalArgumentException("请选择应收单");
        }

        List<ErpAccountsReceivable> receivables = erpAccountsReceivableMapper.selectList(new QueryWrapper<ErpAccountsReceivable>()
            .eq("tenant_id", tenantId)
            .in("id", receivableIds));
        if (receivables.size() != receivableIds.size()) {
            throw new IllegalArgumentException("应收单不存在");
        }
        for (ErpAccountsReceivable receivable : receivables) {
            if ("RED_FLUSHED".equals(receivable.getStatus())) {
                throw new IllegalArgumentException("红冲应收单不可收款");
            }
            BigDecimal totalAmount = receivable.getTotalAmount() == null ? BigDecimal.ZERO : receivable.getTotalAmount();
            if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("金额为0的应收单不可收款");
            }
        }
        ReceiptMode receiptMode = resolveReceiptMode(receivables);
        boolean returnMode = receiptMode == ReceiptMode.RETURN;
        boolean mixedMode = receiptMode == ReceiptMode.MIXED;
        boolean hasAllocations = request.allocations() != null && !request.allocations().isEmpty();
        if (!hasAllocations) {
            if (!returnMode && !mixedMode) {
                if (amount.compareTo(BigDecimal.ZERO) < 0 || discountAmount.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("金额不能小于0");
                }
            } else if (returnMode) {
                if (amount.compareTo(BigDecimal.ZERO) > 0 || discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                    throw new IllegalArgumentException("退货应收金额不能大于0");
                }
            }
        }

        Long customerId = request.customerId();
        Long saleOrderId = request.saleOrderId();
        Long firstReceivableId = receivables.get(0).getId();
        Long resolvedCustomerId = receivables.get(0).getCustomerId();
        boolean sameSaleOrder = true;
        Long resolvedSaleOrderId = receivables.get(0).getSaleOrderId();
        for (ErpAccountsReceivable receivable : receivables) {
            if (!Objects.equals(receivable.getCustomerId(), resolvedCustomerId)) {
                throw new IllegalArgumentException("应收单客户不一致");
            }
            if (!Objects.equals(receivable.getSaleOrderId(), resolvedSaleOrderId)) {
                sameSaleOrder = false;
            }
        }
        if (customerId == null) {
            customerId = resolvedCustomerId;
        }
        if (!Objects.equals(customerId, resolvedCustomerId)) {
            throw new IllegalArgumentException("客户与应收单不一致");
        }
        if (saleOrderId == null && sameSaleOrder) {
            saleOrderId = resolvedSaleOrderId;
        }
        if (customerId == null) {
            throw new IllegalArgumentException("请选择客户");
        }
        validateCashSettlementMethod(tenantId, request.settlementMethod());

        List<ErpReceiptReceivable> allocations = null;
        if (request.allocations() != null && !request.allocations().isEmpty()) {
            allocations = buildAllocationsFromRequest(tenantId, receivables, request.allocations(), returnMode, mixedMode);
            BigDecimal sumAmount = allocations.stream()
                .map(ErpReceiptReceivable::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sumDiscount = allocations.stream()
                .map(ErpReceiptReceivable::getAllocatedDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            amount = sumAmount;
            discountAmount = sumDiscount;
        } else if (mixedMode) {
            throw new IllegalArgumentException("混收需填写分摊金额");
        }
        BigDecimal totalAllocate = amount.add(discountAmount);
        if (!returnMode && !mixedMode) {
            if (totalAllocate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("收款金额或优惠金额必须大于0");
            }
        } else if (returnMode) {
            if (totalAllocate.compareTo(BigDecimal.ZERO) >= 0) {
                throw new IllegalArgumentException("冲减金额必须小于0");
            }
        }

        receipt.setReceivableId(firstReceivableId);
        receipt.setSaleOrderId(saleOrderId);
        receipt.setCustomerId(customerId);
        receipt.setAmount(amount);
        receipt.setDiscountAmount(discountAmount);
        receipt.setSettlementMethod(request.settlementMethod());
        receipt.setReceiptMethodCode(request.receiptMethodCode());
        receipt.setReceivedAt(parseReceivedAt(request.receivedAt()));
        receipt.setRemark(request.remark());
        receipt.setUpdatedAt(Instant.now());
        receipt.setUpdatedBy(CurrentActor.username());
        erpReceiptMapper.updateById(receipt);

        erpReceiptReceivableMapper.delete(new QueryWrapper<ErpReceiptReceivable>()
            .eq("tenant_id", tenantId)
            .eq("receipt_id", receipt.getId()));
        if (allocations == null) {
            if (mixedMode) {
                throw new IllegalArgumentException("混收需填写分摊金额");
            }
            allocations = buildAllocations(tenantId, receipt.getId(), receivables, amount, discountAmount, returnMode);
        } else {
            for (ErpReceiptReceivable allocation : allocations) {
                allocation.setReceiptId(receipt.getId());
                allocation.setTenantId(tenantId);
                allocation.setCreatedAt(Instant.now());
            }
        }
        for (ErpReceiptReceivable allocation : allocations) {
            erpReceiptReceivableMapper.insert(allocation);
        }

        return buildDetail(tenantId, receipt);
    }

    private QueryWrapper<ErpReceipt> baseWrapper(String keyword, String status, Long customerId, Long receivableId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpReceipt> wrapper = new QueryWrapper<ErpReceipt>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("receipt_no", keyword.trim());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status.trim());
        }
        if (customerId != null) {
            wrapper.eq("customer_id", customerId);
        }
        if (receivableId != null) {
            wrapper.eq("receivable_id", receivableId);
        }
        if (startAt != null) {
            wrapper.ge("created_at", startAt);
        }
        if (endAt != null) {
            wrapper.le("created_at", endAt);
        }
        return wrapper;
    }

    private List<ErpReceiptView> mapViews(List<ErpReceipt> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Set<Long> customerIds = items.stream()
            .map(ErpReceipt::getCustomerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> customerNameMap = new HashMap<>();
        if (!customerIds.isEmpty()) {
            List<ErpCustomer> customers = erpCustomerMapper.selectBatchIds(customerIds);
            for (ErpCustomer customer : customers) {
                customerNameMap.put(customer.getId(), customer.getName());
            }
        }
        return items.stream()
            .map(item -> new ErpReceiptView(
                item.getId(),
                item.getReceiptNo(),
                item.getCustomerId(),
                customerNameMap.getOrDefault(item.getCustomerId(), "-"),
                item.getReceivableId(),
                item.getAmount(),
                item.getDiscountAmount(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getRemark()
            ))
            .toList();
    }

    private String resolveReceiptNo(Long tenantId, String provided) {
        if (provided == null || provided.isBlank()) {
            return generateReceiptNo(tenantId);
        }
        String trimmed = provided.trim();
        ErpReceipt existing = erpReceiptMapper.selectOne(new QueryWrapper<ErpReceipt>()
            .eq("tenant_id", tenantId)
            .eq("receipt_no", trimmed));
        if (existing != null) {
            throw new IllegalArgumentException("收款单号已存在");
        }
        return trimmed;
    }

    private String generateReceiptNo(Long tenantId) {
        String prefix = readConfig("erp.order.no.receipt.prefix", "RC");
        String dateFormat = readConfig("erp.order.no.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.order.no.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, RECEIPT_ORDER_TYPE, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, RECEIPT_ORDER_TYPE, dateKey);
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

    private Instant parseReceivedAt(String value) {
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
    public ErpReceiptDetail approve(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpReceipt receipt = loadReceipt(tenantId, id);
        if (!STATUS_DRAFT.equals(receipt.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可审核");
        }
        validateReceiptApproval(tenantId, receipt);
        String operator = CurrentActor.username();
        ErpReceipt approvedReceipt = erpReceiptMapper.approveDraft(tenantId, id, operator);
        if (approvedReceipt == null) {
            throw new IllegalArgumentException("收款单状态已变化，请刷新重试");
        }
        receipt = approvedReceipt;

        List<ErpReceiptReceivable> allocations = erpReceiptReceivableMapper.findByReceiptId(tenantId, receipt.getId());
        if (allocations != null && !allocations.isEmpty()) {
            for (ErpReceiptReceivable allocation : allocations) {
                applyReceivablePaidAmount(tenantId, allocation.getReceivableId(), allocation.getAllocatedTotal());
            }
        } else {
            BigDecimal totalDelta = (receipt.getAmount() == null ? BigDecimal.ZERO : receipt.getAmount())
                .add(receipt.getDiscountAmount() == null ? BigDecimal.ZERO : receipt.getDiscountAmount());
            applyReceivablePaidAmount(tenantId, receipt.getReceivableId(), totalDelta);
        }
        return buildDetail(tenantId, receipt);
    }

    @Override
    @Transactional
    public ErpReceiptDetail redFlush(Long id, String reason) {
        Long tenantId = TenantContext.requireTenantId();
        ErpReceipt receipt = loadReceipt(tenantId, id);
        if (!STATUS_APPROVED.equals(receipt.getStatus())) {
            throw new IllegalArgumentException("仅已审核状态可红冲");
        }
        String reasonText = reason == null ? "" : reason.trim();
        if (reasonText.isEmpty()) {
            throw new IllegalArgumentException("请填写红冲原因");
        }
        String originRemark = receipt.getRemark();
        String redFlushRemark;
        if (originRemark == null || originRemark.isBlank()) {
            redFlushRemark = "红冲原因：" + reasonText;
        } else {
            redFlushRemark = originRemark + " | 红冲原因：" + reasonText;
        }
        String operator = CurrentActor.username();
        ErpReceipt redFlushedReceipt = erpReceiptMapper.redFlushApproved(tenantId, id, redFlushRemark, operator);
        if (redFlushedReceipt == null) {
            throw new IllegalArgumentException("收款单状态已变化，请刷新重试");
        }
        receipt = redFlushedReceipt;

        ErpReceipt redReceipt = new ErpReceipt();
        redReceipt.setTenantId(tenantId);
        redReceipt.setReceivableId(receipt.getReceivableId());
        redReceipt.setSaleOrderId(receipt.getSaleOrderId());
        redReceipt.setReceiptNo(generateReceiptNo(tenantId));
        redReceipt.setCustomerId(receipt.getCustomerId());
        BigDecimal receiptAmount = receipt.getAmount() == null ? BigDecimal.ZERO : receipt.getAmount();
        redReceipt.setAmount(receiptAmount.negate());
        redReceipt.setDiscountAmount(receipt.getDiscountAmount() == null ? BigDecimal.ZERO : receipt.getDiscountAmount().negate());
        redReceipt.setSettlementMethod(receipt.getSettlementMethod());
        redReceipt.setStatus(STATUS_APPROVED);
        redReceipt.setRedFlushSourceType("RECEIPT");
        redReceipt.setRedFlushSourceId(receipt.getId());
        redReceipt.setReceivedAt(Instant.now());
        redReceipt.setRemark("红冲收款单：" + reasonText);
        redReceipt.setCreatedAt(Instant.now());
        redReceipt.setCreatedBy(operator);
        redReceipt.setUpdatedAt(Instant.now());
        redReceipt.setUpdatedBy(operator);
        erpReceiptMapper.insert(redReceipt);

        List<ErpReceiptReceivable> allocations = erpReceiptReceivableMapper.findByReceiptId(tenantId, receipt.getId());
        if (allocations != null && !allocations.isEmpty()) {
            for (ErpReceiptReceivable allocation : allocations) {
                ErpReceiptReceivable redAllocation = new ErpReceiptReceivable();
                redAllocation.setTenantId(tenantId);
                redAllocation.setReceiptId(redReceipt.getId());
                redAllocation.setReceivableId(allocation.getReceivableId());
                redAllocation.setAllocatedAmount(allocation.getAllocatedAmount().negate());
                redAllocation.setAllocatedDiscount(allocation.getAllocatedDiscount().negate());
                redAllocation.setAllocatedTotal(allocation.getAllocatedTotal().negate());
                redAllocation.setCreatedAt(Instant.now());
                erpReceiptReceivableMapper.insert(redAllocation);
                applyReceivablePaidAmount(tenantId, allocation.getReceivableId(), allocation.getAllocatedTotal().negate());
            }
        } else {
            BigDecimal totalDelta = (receipt.getAmount() == null ? BigDecimal.ZERO : receipt.getAmount())
                .add(receipt.getDiscountAmount() == null ? BigDecimal.ZERO : receipt.getDiscountAmount());
            applyReceivablePaidAmount(tenantId, receipt.getReceivableId(), totalDelta.negate());
        }
        return buildDetail(tenantId, receipt);
    }

    private ErpReceipt loadReceipt(Long tenantId, Long id) {
        ErpReceipt receipt = erpReceiptMapper.selectOne(new QueryWrapper<ErpReceipt>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (receipt == null) {
            throw new IllegalArgumentException("收款单不存在");
        }
        return receipt;
    }

    private ErpReceiptDetail buildDetail(Long tenantId, ErpReceipt receipt) {
        String customerName = "-";
        if (receipt.getCustomerId() != null) {
            ErpCustomer customer = erpCustomerMapper.selectById(receipt.getCustomerId());
            if (customer != null) {
                customerName = customer.getName();
            }
        }
        String orderNo = null;
        if (receipt.getSaleOrderId() != null) {
            ErpSaleOrder order = erpSaleOrderMapper.selectOne(new QueryWrapper<ErpSaleOrder>()
                .eq("tenant_id", tenantId)
                .eq("id", receipt.getSaleOrderId()));
            if (order != null) {
                orderNo = order.getOrderNo();
            }
        }
        List<ErpReceiptReceivable> allocations = erpReceiptReceivableMapper.findByReceiptId(tenantId, receipt.getId());
        List<ErpAccountsReceivable> receivables = loadReceivables(tenantId, receipt, allocations);
        List<ErpReceiptReceivableView> receivableViews = buildReceivableViews(receivables, allocations);
        String receivableNo = receivableViews.isEmpty() ? null : receivableViews.get(0).orderNo();
        return new ErpReceiptDetail(receipt, customerName, orderNo, receivableNo, receivableViews);
    }

    private List<ErpAccountsReceivable> loadReceivables(Long tenantId,
                                                        ErpReceipt receipt,
                                                        List<ErpReceiptReceivable> allocations) {
        List<Long> ids = new ArrayList<>();
        if (allocations != null && !allocations.isEmpty()) {
            for (ErpReceiptReceivable allocation : allocations) {
                if (allocation.getReceivableId() != null) {
                    ids.add(allocation.getReceivableId());
                }
            }
        }
        if (ids.isEmpty() && receipt.getReceivableId() != null) {
            ids.add(receipt.getReceivableId());
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        return erpAccountsReceivableMapper.selectList(new QueryWrapper<ErpAccountsReceivable>()
            .eq("tenant_id", tenantId)
            .in("id", ids));
    }

    private List<ErpReceiptReceivableView> buildReceivableViews(List<ErpAccountsReceivable> receivables,
                                                                 List<ErpReceiptReceivable> allocations) {
        if (receivables == null || receivables.isEmpty()) {
            return List.of();
        }
        Map<Long, ErpAccountsReceivable> receivableMap = receivables.stream()
            .collect(Collectors.toMap(ErpAccountsReceivable::getId, item -> item));
        List<ErpReceiptReceivableView> views = new ArrayList<>();
        if (allocations != null && !allocations.isEmpty()) {
            for (ErpReceiptReceivable allocation : allocations) {
                Long receivableId = allocation.getReceivableId();
                ErpAccountsReceivable receivable = receivableMap.get(receivableId);
                String orderNo = receivable == null ? null : receivable.getOrderNo();
                views.add(new ErpReceiptReceivableView(
                    receivableId,
                    orderNo,
                    allocation.getAllocatedAmount(),
                    allocation.getAllocatedDiscount(),
                    allocation.getAllocatedTotal()
                ));
            }
            return views;
        }
        for (ErpAccountsReceivable receivable : receivables) {
            views.add(new ErpReceiptReceivableView(
                receivable.getId(),
                receivable.getOrderNo(),
                null,
                null,
                null
            ));
        }
        return views;
    }

    private boolean isReturnReceivable(ErpAccountsReceivable receivable) {
        if (receivable == null) {
            return false;
        }
        BigDecimal totalAmount = receivable.getTotalAmount();
        return totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) < 0;
    }

    private ReceiptMode resolveReceiptMode(List<ErpAccountsReceivable> receivables) {
        boolean hasReturn = false;
        boolean hasNormal = false;
        for (ErpAccountsReceivable receivable : receivables) {
            if (isReturnReceivable(receivable)) {
                hasReturn = true;
            } else {
                hasNormal = true;
            }
        }
        if (hasReturn && hasNormal) {
            return ReceiptMode.MIXED;
        }
        if (hasReturn) {
            return ReceiptMode.RETURN;
        }
        return ReceiptMode.NORMAL;
    }

    private void applyReceivablePaidAmount(Long tenantId, Long receivableId, BigDecimal delta) {
        if (receivableId == null || delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        ErpAccountsReceivable updated = erpAccountsReceivableMapper.applyPaidDeltaIfInRange(tenantId, receivableId, delta);
        if (updated == null) {
            throw new IllegalArgumentException("收款金额不能大于未收金额");
        }
    }

    private void validateReceiptApproval(Long tenantId, ErpReceipt receipt) {
        validateCashSettlementMethod(tenantId, receipt.getSettlementMethod());
        List<ErpReceiptReceivable> allocations = erpReceiptReceivableMapper.findByReceiptId(tenantId, receipt.getId());
        if (allocations != null && !allocations.isEmpty()) {
            for (ErpReceiptReceivable allocation : allocations) {
                ensureReceivableCapacity(tenantId, allocation.getReceivableId(), allocation.getAllocatedTotal());
            }
            return;
        }
        ensureReceivableCapacity(
            tenantId,
            receipt.getReceivableId(),
            resolveReceiptTotal(receipt.getAmount(), receipt.getDiscountAmount())
        );
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
            throw new IllegalArgumentException("收款单不允许使用挂账类结算方式");
        }
    }

    private void ensureReceivableCapacity(Long tenantId, Long receivableId, BigDecimal delta) {
        if (receivableId == null) {
            throw new IllegalArgumentException("应收单不存在");
        }
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("收款金额或优惠金额必须大于0");
        }
        ErpAccountsReceivable receivable = erpAccountsReceivableMapper.selectOne(new QueryWrapper<ErpAccountsReceivable>()
            .eq("tenant_id", tenantId)
            .eq("id", receivableId));
        if (receivable == null) {
            throw new IllegalArgumentException("应收单不存在");
        }
        if (STATUS_RED_FLUSHED.equals(receivable.getStatus())) {
            throw new IllegalArgumentException("红冲应收单不可收款");
        }
        BigDecimal unpaid = receivable.getUnpaidAmount() == null ? BigDecimal.ZERO : receivable.getUnpaidAmount();
        if (unpaid.compareTo(BigDecimal.ZERO) == 0 || delta.signum() != unpaid.signum() || delta.abs().compareTo(unpaid.abs()) > 0) {
            throw new IllegalArgumentException("收款金额不能大于未收金额");
        }
    }

    private BigDecimal resolveReceiptTotal(BigDecimal amount, BigDecimal discountAmount) {
        BigDecimal normalizedAmount = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal normalizedDiscount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        return normalizedAmount.add(normalizedDiscount);
    }

    private List<ErpReceiptReceivable> buildAllocations(Long tenantId,
                                                        Long receiptId,
                                                        List<ErpAccountsReceivable> receivables,
                                                        BigDecimal amount,
                                                        BigDecimal discountAmount,
                                                        boolean returnMode) {
        int count = receivables.size();
        BigDecimal totalAllocate = amount.add(discountAmount);
        List<BigDecimal> weights = new ArrayList<>(count);
        List<BigDecimal> capacities = new ArrayList<>(count);
        BigDecimal totalUnpaid = BigDecimal.ZERO;
        for (ErpAccountsReceivable receivable : receivables) {
            BigDecimal unpaid = receivable.getUnpaidAmount() == null ? BigDecimal.ZERO : receivable.getUnpaidAmount();
            BigDecimal absUnpaid = unpaid.abs();
            weights.add(absUnpaid);
            capacities.add(absUnpaid);
            totalUnpaid = totalUnpaid.add(absUnpaid);
        }

        if (totalUnpaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("未收金额为0，无法收款");
        }
        BigDecimal absTotalAllocate = totalAllocate.abs();
        if (absTotalAllocate.compareTo(totalUnpaid) > 0) {
            throw new IllegalArgumentException("收款金额不能大于未收金额");
        }

        BigDecimal sign = returnMode ? BigDecimal.valueOf(-1) : BigDecimal.ONE;
        List<BigDecimal> totalAllocationsAbs = distributeByWeight(absTotalAllocate, weights, capacities);
        List<BigDecimal> amountAllocationsAbs = distributeByWeight(amount.abs(), totalAllocationsAbs, totalAllocationsAbs);
        List<BigDecimal> discountAllocationsAbs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            discountAllocationsAbs.add(totalAllocationsAbs.get(i).subtract(amountAllocationsAbs.get(i)));
        }
        List<ErpReceiptReceivable> allocations = new ArrayList<>();
        for (int i = 0; i < receivables.size(); i++) {
            ErpAccountsReceivable receivable = receivables.get(i);
            BigDecimal allocAmount = amountAllocationsAbs.get(i).multiply(sign);
            BigDecimal allocDiscount = discountAllocationsAbs.get(i).multiply(sign);
            BigDecimal allocTotal = totalAllocationsAbs.get(i).multiply(sign);
            BigDecimal unpaid = receivable.getUnpaidAmount() == null ? BigDecimal.ZERO : receivable.getUnpaidAmount();
            if (allocTotal.abs().compareTo(unpaid.abs()) > 0) {
                throw new IllegalArgumentException("收款金额不能大于未收金额");
            }
            ErpReceiptReceivable allocation = new ErpReceiptReceivable();
            allocation.setTenantId(tenantId);
            allocation.setReceiptId(receiptId);
            allocation.setReceivableId(receivable.getId());
            allocation.setAllocatedAmount(allocAmount);
            allocation.setAllocatedDiscount(allocDiscount);
            allocation.setAllocatedTotal(allocTotal);
            allocation.setCreatedAt(Instant.now());
            allocations.add(allocation);
        }
        return allocations;
    }

    private List<ErpReceiptReceivable> buildAllocationsFromRequest(Long tenantId,
                                                                   List<ErpAccountsReceivable> receivables,
                                                                   List<ErpReceiptAllocationRequest> allocationRequests,
                                                                   boolean returnMode,
                                                                   boolean mixedMode) {
        Map<Long, ErpReceiptAllocationRequest> allocationMap = new HashMap<>();
        for (ErpReceiptAllocationRequest request : allocationRequests) {
            if (request == null || request.receivableId() == null) {
                continue;
            }
            if (allocationMap.containsKey(request.receivableId())) {
                throw new IllegalArgumentException("应收单分摊重复");
            }
            allocationMap.put(request.receivableId(), request);
        }
        if (allocationMap.isEmpty()) {
            throw new IllegalArgumentException("请填写分摊金额");
        }
        List<ErpReceiptReceivable> allocations = new ArrayList<>();
        for (ErpAccountsReceivable receivable : receivables) {
            ErpReceiptAllocationRequest request = allocationMap.get(receivable.getId());
            if (request == null) {
                throw new IllegalArgumentException("应收单分摊不完整");
            }
            BigDecimal amount = request.amount() == null ? BigDecimal.ZERO : request.amount();
            BigDecimal discount = request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount();
            BigDecimal allocTotal = amount.add(discount);
            BigDecimal unpaid = receivable.getUnpaidAmount() == null ? BigDecimal.ZERO : receivable.getUnpaidAmount();
            if (mixedMode) {
                if (isReturnReceivable(receivable)) {
                    if (amount.compareTo(BigDecimal.ZERO) > 0 || discount.compareTo(BigDecimal.ZERO) > 0) {
                        throw new IllegalArgumentException("退货分摊金额不能大于0");
                    }
                    if (allocTotal.compareTo(BigDecimal.ZERO) >= 0) {
                        throw new IllegalArgumentException("退货分摊金额必须小于0");
                    }
                } else {
                    if (amount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("分摊金额不能小于0");
                    }
                    if (allocTotal.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("分摊金额必须大于0");
                    }
                }
                if (allocTotal.abs().compareTo(unpaid.abs()) > 0) {
                    throw new IllegalArgumentException("分摊金额不能大于未收金额");
                }
            } else if (!returnMode) {
                if (amount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("分摊金额不能小于0");
                }
                if (allocTotal.compareTo(unpaid) > 0) {
                    throw new IllegalArgumentException("分摊金额不能大于未收金额");
                }
            } else {
                if (!isReturnReceivable(receivable)) {
                    throw new IllegalArgumentException("退货应收与普通应收不可混收");
                }
                if (amount.compareTo(BigDecimal.ZERO) > 0 || discount.compareTo(BigDecimal.ZERO) > 0) {
                    throw new IllegalArgumentException("退货分摊金额不能大于0");
                }
                if (allocTotal.compareTo(BigDecimal.ZERO) >= 0) {
                    throw new IllegalArgumentException("退货分摊金额必须小于0");
                }
                if (allocTotal.abs().compareTo(unpaid.abs()) > 0) {
                    throw new IllegalArgumentException("分摊金额不能大于未收金额");
                }
            }
            ErpReceiptReceivable allocation = new ErpReceiptReceivable();
            allocation.setReceivableId(receivable.getId());
            allocation.setAllocatedAmount(amount);
            allocation.setAllocatedDiscount(discount);
            allocation.setAllocatedTotal(allocTotal);
            allocations.add(allocation);
        }
        return allocations;
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
            throw new IllegalArgumentException("收款金额不能大于未收金额");
        }
        return results;
    }
}
