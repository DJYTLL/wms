package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPurchaseOrderCreateRequest;
import com.example.wms.dto.erp.ErpPurchaseOrderDetail;
import com.example.wms.dto.erp.ErpPurchaseOrderItemRequest;
import com.example.wms.dto.erp.ErpPurchaseOrderHistoryItem;
import com.example.wms.dto.erp.ErpPurchaseOrderRecentItem;
import com.example.wms.dto.erp.ErpPurchaseOrderUpdateRequest;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpAccountsPayable;
import com.example.wms.entity.erp.ErpPayment;
import com.example.wms.entity.erp.ErpPaymentMethod;
import com.example.wms.entity.erp.ErpPaymentPayable;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.entity.erp.ErpPurchaseOrderItem;
import com.example.wms.entity.erp.ErpPurchaseReturn;
import com.example.wms.entity.erp.ErpSettlementMethod;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockTxn;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPaymentMethodMapper;
import com.example.wms.mapper.erp.ErpPaymentPayableMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderItemMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.service.TenantSettingService;
import com.example.wms.service.erp.ErpPurchaseOrderService;
import com.example.wms.service.erp.support.ErpCostService;
import com.example.wms.service.erp.support.FinanceAutoFlowMode;
import com.example.wms.service.erp.support.FinanceAutoFlowSupport;
import com.example.wms.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 采购单服务实现（ERP进销存）
@Service
public class ErpPurchaseOrderServiceImpl implements ErpPurchaseOrderService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_RED_FLUSHED = "RED_FLUSHED";
    private static final String STATUS_SETTLED = "SETTLED";
    private static final String STATUS_OPEN = "OPEN";
    private static final String ORDER_TYPE = "PURCHASE";
    private static final String AUTO_PAYABLE_REMARK = "采购单审核生成应付单";
    private static final String AUTO_PAYMENT_REMARK = "采购单审核自动付款";

    private final ErpPurchaseOrderMapper erpPurchaseOrderMapper;
    private final ErpPurchaseOrderItemMapper erpPurchaseOrderItemMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpStockBalanceMapper erpStockBalanceMapper;
    private final ErpStockTxnMapper erpStockTxnMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ErpAccountsPayableMapper erpAccountsPayableMapper;
    private final ErpPaymentMapper erpPaymentMapper;
    private final ErpPaymentMethodMapper erpPaymentMethodMapper;
    private final ErpPaymentPayableMapper erpPaymentPayableMapper;
    private final ErpPurchaseReturnMapper erpPurchaseReturnMapper;
    private final ErpSettlementMethodMapper erpSettlementMethodMapper;
    private final ErpSupplierMapper erpSupplierMapper;
    private final ErpCostService erpCostService;
    private final TenantSettingService tenantSettingService;

    public ErpPurchaseOrderServiceImpl(ErpPurchaseOrderMapper erpPurchaseOrderMapper,
                                       ErpPurchaseOrderItemMapper erpPurchaseOrderItemMapper,
                                       ErpProductMapper erpProductMapper,
                                       ErpStockBalanceMapper erpStockBalanceMapper,
                                       ErpStockTxnMapper erpStockTxnMapper,
                                      ErpOrderSequenceMapper erpOrderSequenceMapper,
                                      SystemConfigMapper systemConfigMapper,
                                       ErpAccountsPayableMapper erpAccountsPayableMapper,
                                       ErpPaymentMapper erpPaymentMapper,
                                       ErpPaymentMethodMapper erpPaymentMethodMapper,
                                       ErpPaymentPayableMapper erpPaymentPayableMapper,
                                       ErpPurchaseReturnMapper erpPurchaseReturnMapper,
                                       ErpSettlementMethodMapper erpSettlementMethodMapper,
                                       ErpSupplierMapper erpSupplierMapper,
                                       ErpCostService erpCostService,
                                       TenantSettingService tenantSettingService) {
        this.erpPurchaseOrderMapper = erpPurchaseOrderMapper;
        this.erpPurchaseOrderItemMapper = erpPurchaseOrderItemMapper;
        this.erpProductMapper = erpProductMapper;
        this.erpStockBalanceMapper = erpStockBalanceMapper;
        this.erpStockTxnMapper = erpStockTxnMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.erpAccountsPayableMapper = erpAccountsPayableMapper;
        this.erpPaymentMapper = erpPaymentMapper;
        this.erpPaymentMethodMapper = erpPaymentMethodMapper;
        this.erpPaymentPayableMapper = erpPaymentPayableMapper;
        this.erpPurchaseReturnMapper = erpPurchaseReturnMapper;
        this.erpSettlementMethodMapper = erpSettlementMethodMapper;
        this.erpSupplierMapper = erpSupplierMapper;
        this.erpCostService = erpCostService;
        this.tenantSettingService = tenantSettingService;
    }

    @Override
    public List<ErpPurchaseOrder> listAll(String keyword, String status, Long supplierId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpPurchaseOrder> wrapper = baseWrapper(keyword, status, supplierId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        return erpPurchaseOrderMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpPurchaseOrder> page(long page, long size, String keyword, String status, Long supplierId, Instant startAt, Instant endAt) {
        Page<ErpPurchaseOrder> pageReq = Page.of(page, size);
        QueryWrapper<ErpPurchaseOrder> wrapper = baseWrapper(keyword, status, supplierId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        Page<ErpPurchaseOrder> result = erpPurchaseOrderMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public PageResponse<ErpPurchaseOrder> pageDraft(long page, long size, String keyword, Long supplierId, Instant startAt, Instant endAt) {
        return page(page, size, keyword, STATUS_DRAFT, supplierId, startAt, endAt);
    }

    @Override
    public PageResponse<ErpPurchaseOrder> pageApproved(long page, long size, String keyword, String status, Long supplierId, Instant startAt, Instant endAt) {
        String finalStatus = normalizeApprovedStatusFilter(status);
        return page(page, size, keyword, finalStatus, supplierId, startAt, endAt);
    }

    @Override
    public Map<String, Object> summaryDraft(String keyword, Long supplierId, Instant startAt, Instant endAt) {
        return buildSummary(baseWrapper(keyword, STATUS_DRAFT, supplierId, startAt, endAt));
    }

    @Override
    public Map<String, Object> summaryApproved(String keyword, String status, Long supplierId, Instant startAt, Instant endAt) {
        return buildSummary(baseWrapper(keyword, normalizeApprovedStatusFilter(status), supplierId, startAt, endAt));
    }

    @Override
    public ErpPurchaseOrderDetail getDetail(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPurchaseOrder order = erpPurchaseOrderMapper.selectOne(new QueryWrapper<ErpPurchaseOrder>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (order == null) {
            throw new IllegalArgumentException("采购单不存在");
        }
        List<ErpPurchaseOrderItem> items = erpPurchaseOrderItemMapper.findByOrderId(tenantId, id);
        return new ErpPurchaseOrderDetail(order, items);
    }

    @Override
    public ErpPurchaseOrderDetail getDraftDetail(Long id) {
        ErpPurchaseOrderDetail detail = getDetail(id);
        if (!STATUS_DRAFT.equals(detail.order().getStatus())) {
            throw new IllegalArgumentException("草稿接口只能访问草稿采购单");
        }
        return detail;
    }

    @Override
    public ErpPurchaseOrderDetail getApprovedDetail(Long id) {
        ErpPurchaseOrderDetail detail = getDetail(id);
        if (STATUS_DRAFT.equals(detail.order().getStatus())) {
            throw new IllegalArgumentException("已审核接口不能访问草稿采购单");
        }
        return detail;
    }

    @Override
    public List<ErpPurchaseOrderRecentItem> recentItemsByProduct(Long supplierId, Long productId, int limit) {
        if (supplierId == null || productId == null) {
            return List.of();
        }
        Long tenantId = TenantContext.requireTenantId();
        int finalLimit = limit <= 0 ? 10 : Math.min(limit, 50);
        return erpPurchaseOrderItemMapper.findRecentItems(tenantId, supplierId, productId, finalLimit);
    }

    @Override
    public PageResponse<ErpPurchaseOrderRecentItem> recentItemsByProduct(Long supplierId, Long productId, long page, long size) {
        Long tenantId = TenantContext.requireTenantId();
        long finalPage = page <= 0 ? 1 : page;
        long finalSize = size <= 0 ? 10 : Math.min(size, 100);
        if (supplierId == null || productId == null) {
            return new PageResponse<>(0, finalPage, finalSize, List.of());
        }
        long total = erpPurchaseOrderItemMapper.countRecentItems(tenantId, supplierId, productId);
        long offset = (finalPage - 1) * finalSize;
        List<ErpPurchaseOrderRecentItem> items = total == 0
            ? List.of()
            : erpPurchaseOrderItemMapper.findRecentItemsPage(
                tenantId,
                supplierId,
                productId,
                (int) finalSize,
                offset
            );
        return new PageResponse<>(total, finalPage, finalSize, items);
    }

    @Override
    public PageResponse<ErpPurchaseOrderHistoryItem> productHistory(Long supplierId,
                                                                    Long productId,
                                                                    String keyword,
                                                                    Instant startAt,
                                                                    Instant endAt,
                                                                    long page,
                                                                    long size) {
        if (productId == null) {
            return new PageResponse<>(0, page, size, List.of());
        }
        Long tenantId = TenantContext.requireTenantId();
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        long finalSize = size <= 0 ? 10 : Math.min(size, 50);
        long finalPage = page <= 0 ? 1 : page;
        long offset = (finalPage - 1) * finalSize;
        long total = erpPurchaseOrderItemMapper.countProductHistory(
            tenantId,
            supplierId,
            productId,
            normalizedKeyword,
            startAt,
            endAt
        );
        List<ErpPurchaseOrderHistoryItem> items = total == 0
            ? List.of()
            : erpPurchaseOrderItemMapper.findProductHistoryPage(
                tenantId,
                supplierId,
                productId,
                normalizedKeyword,
                startAt,
                endAt,
                (int) finalSize,
                offset
            );
        return new PageResponse<>(total, finalPage, finalSize, items);
    }

    @Override
    public String nextOrderNo() {
        Long tenantId = TenantContext.requireTenantId();
        return ensureOrderNo(tenantId, null, ORDER_TYPE, "PO");
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_PURCHASE_CREATE", entityType = "erp_purchase_order", entityId = "{result.order.id}", detail = "orderNo={result.order.orderNo}")
    public ErpPurchaseOrderDetail create(ErpPurchaseOrderCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String operator = resolveCurrentUsername();
        String orderNo = ensureOrderNo(tenantId, request.orderNo(), ORDER_TYPE, "PO");
        ErpPurchaseOrder order = new ErpPurchaseOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(orderNo);
        order.setStatus(STATUS_DRAFT);
        order.setSupplierId(request.supplierId());
        order.setOrderAt(parseOrderAt(request.orderAt()));
        order.setSettlementMethod(normalizeCode(request.settlementMethod()));
        order.setPaymentMethodCode(request.paymentMethodCode());
        order.setPaidAmount(normalizeAmount(request.paidAmount()));
        normalizeCreditSettlementFields(tenantId, order);
        validateHeaderMasterData(tenantId, order.getSupplierId(), order.getSettlementMethod(), order.getPaymentMethodCode(), order.getPaidAmount());
        order.setDiscountAmount(normalizeAmount(request.discountAmount()));
        order.setTotalAmount(BigDecimal.ZERO);
        order.setTotalAmountExclTax(BigDecimal.ZERO);
        order.setTotalTaxAmount(BigDecimal.ZERO);
        order.setTotalAmountInclTax(BigDecimal.ZERO);
        order.setVersion(0L);
        order.setRemark(request.remark());
        order.setCreatedAt(Instant.now());
        order.setCreatedBy(operator);
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(operator);
        erpPurchaseOrderMapper.insert(order);

        List<ErpPurchaseOrderItem> items = buildItems(tenantId, order.getId(), request.items(), Set.of());
        for (ErpPurchaseOrderItem item : items) {
            erpPurchaseOrderItemMapper.insert(item);
        }
        applyTotals(order, items);
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(operator);
        updateWithVersion(tenantId, order);
        return new ErpPurchaseOrderDetail(order, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_PURCHASE_COPY_APPROVED", entityType = "erp_purchase_order", entityId = "{arg0}")
    public ErpPurchaseOrderDetail copyApprovedToDraft(Long id) {
        ErpPurchaseOrderDetail source = getApprovedDetail(id);
        ErpPurchaseOrder sourceOrder = source.order();
        if (!STATUS_APPROVED.equals(sourceOrder.getStatus()) && !STATUS_CANCELLED.equals(sourceOrder.getStatus())) {
            throw new IllegalArgumentException("仅已审核或已作废采购单可复制");
        }
        List<ErpPurchaseOrderItemRequest> itemRequests = source.items().stream()
            .map(item -> new ErpPurchaseOrderItemRequest(
                item.getProductId(),
                item.getWarehouseId(),
                item.getLocationId(),
                item.getQty(),
                item.getPrice(),
                item.getPriceInclTax(),
                item.getTaxRate(),
                item.getSortNo(),
                item.getRemark()
            ))
            .toList();
        ErpPurchaseOrderCreateRequest request = new ErpPurchaseOrderCreateRequest(
            nextOrderNo(),
            null,
            sourceOrder.getSupplierId(),
            sourceOrder.getSettlementMethod(),
            sourceOrder.getPaymentMethodCode(),
            sourceOrder.getPaidAmount(),
            sourceOrder.getDiscountAmount(),
            itemRequests,
            sourceOrder.getRemark()
        );
        return create(request);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_PURCHASE_UPDATE", entityType = "erp_purchase_order", entityId = "{arg0}", detail = "orderNo={arg1.orderNo}")
    public ErpPurchaseOrderDetail update(Long id, ErpPurchaseOrderUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPurchaseOrder order = erpPurchaseOrderMapper.selectOne(new QueryWrapper<ErpPurchaseOrder>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (order == null) {
            throw new IllegalArgumentException("采购单不存在");
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可编辑");
        }
        String newOrderNo = resolveOrderNoForUpdate(request.orderNo(), order.getOrderNo(), tenantId, order.getId());
        order.setOrderNo(newOrderNo);
        order.setSupplierId(request.supplierId());
        order.setOrderAt(parseOrderAt(request.orderAt()));
        order.setSettlementMethod(normalizeCode(request.settlementMethod()));
        order.setPaymentMethodCode(request.paymentMethodCode());
        order.setPaidAmount(normalizeAmount(request.paidAmount()));
        normalizeCreditSettlementFields(tenantId, order);
        validateHeaderMasterData(tenantId, order.getSupplierId(), order.getSettlementMethod(), order.getPaymentMethodCode(), order.getPaidAmount());
        order.setDiscountAmount(normalizeAmount(request.discountAmount()));
        order.setRemark(request.remark());
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(resolveCurrentUsername());

        Set<Long> allowedDisabledProductIds = existingProductIds(erpPurchaseOrderItemMapper.findByOrderId(tenantId, id));

        erpPurchaseOrderItemMapper.delete(new QueryWrapper<ErpPurchaseOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("order_id", id));

        List<ErpPurchaseOrderItem> items = buildItems(tenantId, id, request.items(), allowedDisabledProductIds);
        for (ErpPurchaseOrderItem item : items) {
            erpPurchaseOrderItemMapper.insert(item);
        }
        applyTotals(order, items);
        updateWithVersion(tenantId, order);
        return new ErpPurchaseOrderDetail(order, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_PURCHASE_DELETE", entityType = "erp_purchase_order", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPurchaseOrder order = erpPurchaseOrderMapper.selectOne(new QueryWrapper<ErpPurchaseOrder>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (order == null) {
            throw new IllegalArgumentException("采购单不存在");
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可删除");
        }
        erpPurchaseOrderItemMapper.delete(new QueryWrapper<ErpPurchaseOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("order_id", id));
        erpPurchaseOrderMapper.deleteById(id);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_PURCHASE_APPROVE", entityType = "erp_purchase_order", entityId = "{arg0}")
    public void approve(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPurchaseOrder order = loadForUpdate(tenantId, id);
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可审核");
        }
        List<ErpPurchaseOrderItem> items = erpPurchaseOrderItemMapper.findByOrderId(tenantId, id);
        applyInboundCost(tenantId, items);
        for (ErpPurchaseOrderItem item : items) {
            applyStockDelta(tenantId, item, item.getQty(), "PURCHASE_APPROVE", id);
        }
        String operator = resolveCurrentUsername();
        order.setStatus(STATUS_APPROVED);
        order.setApprovedBy(operator);
        order.setApprovedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(operator);
        updateWithVersion(tenantId, order);
        ensurePayableAndPayment(tenantId, order, operator);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_PURCHASE_UNAPPROVE", entityType = "erp_purchase_order", entityId = "{arg0}")
    public void unapprove(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPurchaseOrder order = loadForUpdate(tenantId, id);
        if (!STATUS_APPROVED.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅已审核采购单可反审核");
        }
        ErpAccountsPayable payable = erpAccountsPayableMapper.findByPurchaseOrderId(tenantId, order.getId());
        if (payable != null && hasApprovedPaymentAllocation(tenantId, payable.getId())) {
            throw new IllegalArgumentException("请先红冲付款单");
        }
        List<ErpPurchaseReturn> approvedReturns = erpPurchaseReturnMapper.findApprovedByPurchaseOrderId(tenantId, order.getId());
        if (approvedReturns != null && !approvedReturns.isEmpty()) {
            throw new IllegalArgumentException("请先红冲采购退货单");
        }
        List<ErpPurchaseOrderItem> items = erpPurchaseOrderItemMapper.findByOrderId(tenantId, id);
        reverseInboundCosts(tenantId, items);
        for (ErpPurchaseOrderItem item : items) {
            applyStockDelta(tenantId, item, item.getQty().negate(), "PURCHASE_UNAPPROVE", id);
        }
        if (payable != null) {
            erpAccountsPayableMapper.deleteById(payable.getId());
        }
        String operator = resolveCurrentUsername();
        order.setStatus(STATUS_DRAFT);
        order.setApprovedBy(null);
        order.setApprovedAt(null);
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(operator);
        updateWithVersion(tenantId, order);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_PURCHASE_CANCEL", entityType = "erp_purchase_order", entityId = "{arg0}")
    public void cancel(Long id, String reason) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPurchaseOrder order = loadForUpdate(tenantId, id);
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("红冲原因不能为空");
        }
        if (STATUS_APPROVED.equals(order.getStatus())) {
            ErpAccountsPayable payable = erpAccountsPayableMapper.findByPurchaseOrderId(tenantId, order.getId());
            if (payable != null && hasApprovedPaymentAllocation(tenantId, payable.getId())) {
                throw new IllegalArgumentException("请先红冲付款单");
            }
            List<ErpPurchaseReturn> approvedReturns = erpPurchaseReturnMapper.findApprovedByPurchaseOrderId(tenantId, order.getId());
            if (approvedReturns != null && !approvedReturns.isEmpty()) {
                throw new IllegalArgumentException("请先红冲采购退货单");
            }
            List<ErpPurchaseOrderItem> items = erpPurchaseOrderItemMapper.findByOrderId(tenantId, id);
            reverseInboundCosts(tenantId, items);
            for (ErpPurchaseOrderItem item : items) {
                applyStockDelta(tenantId, item, item.getQty().negate(), "PURCHASE_CANCEL", id);
            }
            if (payable != null) {
                payable.setTotalAmount(BigDecimal.ZERO);
                payable.setPaidAmount(BigDecimal.ZERO);
                payable.setDiscountAmount(BigDecimal.ZERO);
                payable.setUnpaidAmount(BigDecimal.ZERO);
                payable.setStatus(STATUS_RED_FLUSHED);
                payable.setRemark(appendRedFlushReason(payable.getRemark(), reason));
                payable.setUpdatedAt(Instant.now());
                erpAccountsPayableMapper.updateById(payable);
            }
        } else if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿或已审核状态可作废");
        }
        String operator = resolveCurrentUsername();
        order.setStatus(STATUS_CANCELLED);
        order.setCancelledBy(operator);
        order.setCancelledAt(Instant.now());
        order.setRemark(appendRedFlushReason(order.getRemark(), reason));
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(operator);
        updateWithVersion(tenantId, order);
    }

    private QueryWrapper<ErpPurchaseOrder> baseWrapper(String keyword, String status, Long supplierId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpPurchaseOrder> wrapper = new QueryWrapper<ErpPurchaseOrder>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("order_no", keyword));
        }
        if (status != null && !status.isBlank()) {
            String trimmed = status.trim();
            if (trimmed.contains(",")) {
                List<String> statuses = java.util.Arrays.stream(trimmed.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
                if (!statuses.isEmpty()) {
                    wrapper.in("status", statuses);
                }
            } else {
                wrapper.eq("status", trimmed);
            }
        }
        if (supplierId != null) {
            wrapper.eq("supplier_id", supplierId);
        }
        if (startAt != null) {
            wrapper.ge("order_at", startAt);
        }
        if (endAt != null) {
            wrapper.le("order_at", endAt);
        }
        return wrapper;
    }

    private String normalizeApprovedStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_APPROVED + "," + STATUS_CANCELLED;
        }
        List<String> statuses = java.util.Arrays.stream(status.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .filter(s -> !STATUS_DRAFT.equals(s))
            .toList();
        if (statuses.isEmpty()) {
            throw new IllegalArgumentException("已审核接口不能查询草稿采购单");
        }
        return String.join(",", statuses);
    }

    private Map<String, Object> buildSummary(QueryWrapper<ErpPurchaseOrder> wrapper) {
        List<ErpPurchaseOrder> orders = erpPurchaseOrderMapper.selectList(wrapper);
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ErpPurchaseOrder order : orders) {
            if (order.getTotalAmount() != null) {
                totalAmount = totalAmount.add(order.getTotalAmount());
            }
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("count", orders.size());
        summary.put("totalAmount", totalAmount);
        return summary;
    }

    private List<ErpPurchaseOrderItem> buildItems(Long tenantId,
                                                  Long orderId,
                                                  List<ErpPurchaseOrderItemRequest> requests,
                                                  Set<Long> allowedDisabledProductIds) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("采购明细不能为空");
        }
        List<ErpPurchaseOrderItem> items = new ArrayList<>();
        int index = 1;
        for (ErpPurchaseOrderItemRequest request : requests) {
            if (request == null) {
                throw new IllegalArgumentException("采购明细不能为空");
            }
            validatePositiveQty(request.qty(), "采购数量必须大于0");
            ErpProduct product = requireUsableProduct(tenantId, request.productId(), allowedDisabledProductIds);
            ErpPurchaseOrderItem item = new ErpPurchaseOrderItem();
            item.setTenantId(tenantId);
            item.setOrderId(orderId);
            item.setProductId(product.getId());
            item.setProductCode(product.getCode());
            item.setProductName(product.getName());
            item.setWarehouseId(request.warehouseId());
            item.setLocationId(request.locationId());
            item.setQty(request.qty());
            BigDecimal taxRate = request.taxRate() == null ? BigDecimal.ZERO : request.taxRate();
            BigDecimal price = request.price();
            BigDecimal priceInclTax = request.priceInclTax();
            if (price == null && priceInclTax != null) {
                price = calcPriceExclTax(priceInclTax, taxRate);
            }
            if (priceInclTax == null && price != null) {
                priceInclTax = calcPriceInclTax(price, taxRate);
            }
            if (price == null) {
                price = BigDecimal.ZERO;
            }
            if (priceInclTax == null) {
                priceInclTax = BigDecimal.ZERO;
            }
            item.setPrice(price);
            item.setPriceInclTax(priceInclTax);
            item.setTaxRate(taxRate);
            item.setAmount(price.multiply(request.qty()));
            item.setAmountInclTax(priceInclTax.multiply(request.qty()));
            item.setTaxAmount(item.getAmountInclTax().subtract(item.getAmount()));
            item.setSortNo(request.sortNo() == null ? index : request.sortNo());
            item.setRemark(request.remark());
            item.setCreatedAt(Instant.now());
            item.setUpdatedAt(Instant.now());
            items.add(item);
            index += 1;
        }
        return items;
    }

    private ErpProduct requireUsableProduct(Long tenantId, Long productId, Set<Long> allowedDisabledProductIds) {
        ErpProduct product = erpProductMapper.selectOne(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", tenantId)
            .eq("id", productId));
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (Boolean.FALSE.equals(product.getEnabled()) && (allowedDisabledProductIds == null || !allowedDisabledProductIds.contains(productId))) {
            throw new IllegalArgumentException("商品已停用，不能新增引用");
        }
        return product;
    }

    private Set<Long> existingProductIds(List<ErpPurchaseOrderItem> items) {
        Set<Long> ids = new HashSet<>();
        if (items == null) {
            return ids;
        }
        for (ErpPurchaseOrderItem item : items) {
            if (item != null && item.getProductId() != null) {
                ids.add(item.getProductId());
            }
        }
        return ids;
    }

    private void applyTotals(ErpPurchaseOrder order, List<ErpPurchaseOrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalExcl = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalIncl = BigDecimal.ZERO;
        for (ErpPurchaseOrderItem item : items) {
            total = total.add(item.getAmount());
            totalExcl = totalExcl.add(item.getAmount());
            totalTax = totalTax.add(item.getTaxAmount());
            totalIncl = totalIncl.add(item.getAmountInclTax());
        }
        order.setTotalAmount(total);
        order.setTotalAmountExclTax(totalExcl);
        order.setTotalTaxAmount(totalTax);
        order.setTotalAmountInclTax(totalIncl);
    }

    private void validateSettlementAmounts(ErpPurchaseOrder order, BigDecimal total) {
        BigDecimal paidAmount = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        BigDecimal discountAmount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        if (paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("付款金额不能小于0");
        }
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("优惠金额不能小于0");
        }
        BigDecimal totalAmount = total == null ? BigDecimal.ZERO : total;
        if (paidAmount.add(discountAmount).compareTo(totalAmount) > 0) {
            throw new IllegalArgumentException("付款金额与优惠金额之和不能大于采购总金额");
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private void validatePositiveQty(BigDecimal qty, String message) {
        if (qty == null) {
            throw new IllegalArgumentException(message);
        }
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateHeaderMasterData(Long tenantId,
                                          Long supplierId,
                                          String settlementMethod,
                                          String paymentMethodCode,
                                          BigDecimal paidAmount) {
        if (supplierId == null) {
            throw new IllegalArgumentException("请选择供应商");
        }
        ErpSupplier supplier = erpSupplierMapper.selectOne(new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", tenantId)
            .eq("id", supplierId));
        if (supplier == null || Boolean.FALSE.equals(supplier.getEnabled()) || Boolean.TRUE.equals(supplier.getBlacklisted())) {
            throw new IllegalArgumentException("供应商不存在、已停用或已拉黑");
        }
        if (settlementMethod == null || settlementMethod.isBlank()) {
            throw new IllegalArgumentException("请选择结算方式");
        }
        ErpSettlementMethod settlementMethodEntity = resolveSettlementMethod(tenantId, settlementMethod);
        if (settlementMethodEntity == null || Boolean.FALSE.equals(settlementMethodEntity.getEnabled())) {
            throw new IllegalArgumentException("结算方式不存在或已停用");
        }
        if (paidAmount != null && paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (paymentMethodCode == null || paymentMethodCode.isBlank()) {
                throw new IllegalArgumentException("请选择付款方式");
            }
            ErpPaymentMethod paymentMethod = erpPaymentMethodMapper.findByCode(tenantId, paymentMethodCode);
            if (paymentMethod == null || Boolean.FALSE.equals(paymentMethod.getEnabled())) {
                throw new IllegalArgumentException("付款方式不存在或已停用");
            }
        }
    }

    private void normalizeCreditSettlementFields(Long tenantId, ErpPurchaseOrder order) {
        if (!isCreditSettlement(tenantId, order.getSettlementMethod())) {
            return;
        }
        order.setPaidAmount(BigDecimal.ZERO);
        order.setPaymentMethodCode(null);
    }

    private boolean isCreditSettlement(Long tenantId, String settlementMethod) {
        if (settlementMethod == null || settlementMethod.isBlank()) {
            return false;
        }
        String code = settlementMethod.trim().toUpperCase();
        if ("CREDIT".equals(code) || "ON_ACCOUNT".equals(code) || "AP".equals(code)) {
            return true;
        }
        ErpSettlementMethod method = resolveSettlementMethod(tenantId, settlementMethod);
        if (method == null) {
            return false;
        }
        if ("HIDDEN".equalsIgnoreCase(method.getFundInputMode())) {
            return true;
        }
        return method.getName() != null && method.getName().contains("挂账");
    }

    private ErpSettlementMethod resolveSettlementMethod(Long tenantId, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        ErpSettlementMethod method = erpSettlementMethodMapper.findByCode(tenantId, normalized);
        if (method != null) {
            return method;
        }
        return erpSettlementMethodMapper.findByName(tenantId, normalized);
    }

    private void ensurePayable(Long tenantId, ErpPurchaseOrder order) {
        ErpAccountsPayable existing = erpAccountsPayableMapper.findByPurchaseOrderId(tenantId, order.getId());
        if (existing == null) {
            ErpAccountsPayable payable = new ErpAccountsPayable();
            payable.setTenantId(tenantId);
            payable.setPurchaseOrderId(order.getId());
            payable.setOrderNo(order.getOrderNo());
            payable.setSupplierId(order.getSupplierId());
            BigDecimal totalAmount = resolvePayableTotal(order);
            payable.setTotalAmount(totalAmount);
            payable.setPaidAmount(BigDecimal.ZERO);
            payable.setUnpaidAmount(totalAmount);
            payable.setStatus(STATUS_OPEN);
            payable.setSettlementMethod(order.getSettlementMethod());
            payable.setRemark(AUTO_PAYABLE_REMARK);
            payable.setCreatedAt(Instant.now());
            payable.setUpdatedAt(Instant.now());
            erpAccountsPayableMapper.insert(payable);
        } else {
            BigDecimal totalAmount = resolvePayableTotal(order);
            BigDecimal paidAmount = existing.getPaidAmount() == null ? BigDecimal.ZERO : existing.getPaidAmount();
            existing.setTotalAmount(totalAmount);
            existing.setUnpaidAmount(totalAmount.subtract(paidAmount).max(BigDecimal.ZERO));
            if (existing.getUnpaidAmount().compareTo(BigDecimal.ZERO) == 0) {
                existing.setStatus(STATUS_SETTLED);
            } else {
                existing.setStatus(STATUS_OPEN);
            }
            existing.setUpdatedAt(Instant.now());
            erpAccountsPayableMapper.updateById(existing);
        }
    }

    private void ensurePayableAndPayment(Long tenantId, ErpPurchaseOrder order, String operator) {
        FinanceAutoFlowMode mode = tenantSettingService.getFinanceAutoFlowMode();
        BigDecimal total = resolvePayableTotal(order);
        BigDecimal discount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        BigDecimal paidCash = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        validateSettlementAmounts(order, total);
        BigDecimal maxPaid = total.subtract(discount);
        if (maxPaid.compareTo(BigDecimal.ZERO) < 0) {
            maxPaid = BigDecimal.ZERO;
        }
        if (paidCash.compareTo(maxPaid) > 0) {
            paidCash = maxPaid;
        }
        if (isCreditSettlement(tenantId, order.getSettlementMethod())) {
            paidCash = BigDecimal.ZERO;
        }
        BigDecimal netAmount = total.subtract(discount);
        if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
            netAmount = BigDecimal.ZERO;
        }
        BigDecimal payableAmount = netAmount.subtract(paidCash);
        if (payableAmount.compareTo(BigDecimal.ZERO) < 0) {
            payableAmount = BigDecimal.ZERO;
        }

        ErpAccountsPayable payable = erpAccountsPayableMapper.findByPurchaseOrderId(tenantId, order.getId());
        if (payable == null && payableAmount.compareTo(BigDecimal.ZERO) > 0) {
            payable = new ErpAccountsPayable();
            payable.setTenantId(tenantId);
            payable.setPurchaseOrderId(order.getId());
            payable.setOrderNo(order.getOrderNo());
            payable.setSupplierId(order.getSupplierId());
            payable.setTotalAmount(payableAmount);
            payable.setPaidAmount(BigDecimal.ZERO);
            payable.setDiscountAmount(BigDecimal.ZERO);
            payable.setUnpaidAmount(payableAmount);
            payable.setStatus(STATUS_OPEN);
            payable.setSettlementMethod(order.getSettlementMethod());
            FinanceAutoFlowSupport.markPayable(payable, FinanceAutoFlowSupport.SOURCE_PURCHASE_ORDER, order.getId(), mode);
            payable.setRemark(AUTO_PAYABLE_REMARK);
            payable.setCreatedAt(Instant.now());
            payable.setUpdatedAt(Instant.now());
            erpAccountsPayableMapper.insert(payable);
        } else if (payable != null) {
            payable.setOrderNo(order.getOrderNo());
            payable.setSupplierId(order.getSupplierId());
            payable.setTotalAmount(payableAmount);
            payable.setPaidAmount(BigDecimal.ZERO);
            payable.setDiscountAmount(BigDecimal.ZERO);
            payable.setUnpaidAmount(payableAmount);
            if (!STATUS_RED_FLUSHED.equals(payable.getStatus())) {
                payable.setStatus(payableAmount.compareTo(BigDecimal.ZERO) == 0 ? STATUS_SETTLED : STATUS_OPEN);
            }
            FinanceAutoFlowSupport.markPayable(payable, FinanceAutoFlowSupport.SOURCE_PURCHASE_ORDER, order.getId(), mode);
            payable.setRemark(AUTO_PAYABLE_REMARK);
            payable.setUpdatedAt(Instant.now());
            erpAccountsPayableMapper.updateById(payable);
        }

        ErpPayment paymentExisting = erpPaymentMapper.findByPurchaseOrderId(tenantId, order.getId());
        if (FinanceAutoFlowSupport.shouldGeneratePaymentDocument(mode) && paidCash.compareTo(BigDecimal.ZERO) > 0) {
            FinanceAutoFlowSupport.assertPaymentCanBeOverwritten(paymentExisting, order.getOrderNo(), AUTO_PAYMENT_REMARK);
            if (paymentExisting == null) {
                ErpPayment payment = new ErpPayment();
                payment.setTenantId(tenantId);
                payment.setPayableId(null);
                payment.setPurchaseOrderId(order.getId());
                payment.setPaymentNo(generatePaymentNo(tenantId));
                payment.setSupplierId(order.getSupplierId());
                payment.setAmount(paidCash);
                payment.setDiscountAmount(BigDecimal.ZERO);
                payment.setSettlementMethod(order.getSettlementMethod());
                payment.setPaymentMethodCode(order.getPaymentMethodCode());
                payment.setStatus(FinanceAutoFlowSupport.paymentDocumentStatus(mode));
                payment.setPaidAt(mode == FinanceAutoFlowMode.AR_AP_WITH_APPROVED_PAYMENT ? Instant.now() : null);
                FinanceAutoFlowSupport.markPayment(payment, FinanceAutoFlowSupport.SOURCE_PURCHASE_ORDER, order.getId(), mode);
                payment.setRemark(AUTO_PAYMENT_REMARK);
                payment.setCreatedAt(Instant.now());
                payment.setCreatedBy(operator);
                payment.setUpdatedAt(Instant.now());
                payment.setUpdatedBy(operator);
                erpPaymentMapper.insert(payment);
            } else if (FinanceAutoFlowSupport.isSystemManaged(paymentExisting, AUTO_PAYMENT_REMARK)) {
                paymentExisting.setPayableId(null);
                paymentExisting.setSupplierId(order.getSupplierId());
                paymentExisting.setAmount(paidCash);
                paymentExisting.setDiscountAmount(BigDecimal.ZERO);
                paymentExisting.setSettlementMethod(order.getSettlementMethod());
                paymentExisting.setPaymentMethodCode(order.getPaymentMethodCode());
                paymentExisting.setStatus(FinanceAutoFlowSupport.paymentDocumentStatus(mode));
                paymentExisting.setPaidAt(mode == FinanceAutoFlowMode.AR_AP_WITH_APPROVED_PAYMENT ? Instant.now() : null);
                FinanceAutoFlowSupport.markPayment(paymentExisting, FinanceAutoFlowSupport.SOURCE_PURCHASE_ORDER, order.getId(), mode);
                paymentExisting.setRemark(AUTO_PAYMENT_REMARK);
                paymentExisting.setUpdatedAt(Instant.now());
                paymentExisting.setUpdatedBy(operator);
                erpPaymentMapper.updateById(paymentExisting);

                erpPaymentPayableMapper.delete(new QueryWrapper<ErpPaymentPayable>()
                    .eq("tenant_id", tenantId)
                    .eq("payment_id", paymentExisting.getId()));
            }
        } else if (FinanceAutoFlowSupport.shouldGeneratePaymentDocument(mode)
            && paymentExisting != null
            && FinanceAutoFlowSupport.isSystemManaged(paymentExisting, AUTO_PAYMENT_REMARK)) {
            paymentExisting.setPayableId(null);
            paymentExisting.setSupplierId(order.getSupplierId());
            paymentExisting.setAmount(BigDecimal.ZERO);
            paymentExisting.setDiscountAmount(BigDecimal.ZERO);
            paymentExisting.setSettlementMethod(order.getSettlementMethod());
            paymentExisting.setPaymentMethodCode(null);
            paymentExisting.setStatus(FinanceAutoFlowSupport.paymentDocumentStatus(mode));
            paymentExisting.setPaidAt(mode == FinanceAutoFlowMode.AR_AP_WITH_APPROVED_PAYMENT ? Instant.now() : null);
            FinanceAutoFlowSupport.markPayment(paymentExisting, FinanceAutoFlowSupport.SOURCE_PURCHASE_ORDER, order.getId(), mode);
            paymentExisting.setUpdatedAt(Instant.now());
            paymentExisting.setUpdatedBy(operator);
            erpPaymentMapper.updateById(paymentExisting);
            erpPaymentPayableMapper.delete(new QueryWrapper<ErpPaymentPayable>()
                .eq("tenant_id", tenantId)
                .eq("payment_id", paymentExisting.getId()));
        }
    }

    private boolean hasApprovedPaymentAllocation(Long tenantId, Long payableId) {
        List<ErpPaymentPayable> allocations = erpPaymentPayableMapper.findByPayableId(tenantId, payableId);
        if (allocations == null || allocations.isEmpty()) {
            return false;
        }
        List<Long> paymentIds = allocations.stream()
            .map(ErpPaymentPayable::getPaymentId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        if (paymentIds.isEmpty()) {
            return false;
        }
        List<ErpPayment> payments = erpPaymentMapper.selectBatchIds(paymentIds);
        if (payments == null || payments.isEmpty()) {
            return false;
        }
        for (ErpPayment payment : payments) {
            if (payment != null
                && tenantId.equals(payment.getTenantId())
                && STATUS_APPROVED.equals(payment.getStatus())
                && resolvePaymentAppliedTotal(payment.getAmount(), payment.getDiscountAmount()).compareTo(BigDecimal.ZERO) > 0) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal resolvePaymentAppliedTotal(BigDecimal amount, BigDecimal discountAmount) {
        BigDecimal normalizedAmount = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal normalizedDiscount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        return normalizedAmount.add(normalizedDiscount);
    }

    private BigDecimal resolvePayableTotal(ErpPurchaseOrder order) {
        if (order.getTotalAmountInclTax() != null) {
            return order.getTotalAmountInclTax();
        }
        return order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
    }

    private ErpPurchaseOrder loadForUpdate(Long tenantId, Long id) {
        ErpPurchaseOrder order = erpPurchaseOrderMapper.selectOne(new QueryWrapper<ErpPurchaseOrder>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (order == null) {
            throw new IllegalArgumentException("采购单不存在");
        }
        return order;
    }

    private void updateWithVersion(Long tenantId, ErpPurchaseOrder order) {
        Long version = order.getVersion() == null ? 0L : order.getVersion();
        order.setVersion(version + 1);
        int updated = erpPurchaseOrderMapper.update(order, new QueryWrapper<ErpPurchaseOrder>()
            .eq("tenant_id", tenantId)
            .eq("id", order.getId())
            .eq("version", version));
        if (updated == 0) {
            throw new IllegalArgumentException("采购单已被修改，请刷新重试");
        }
    }

    private void applyStockDelta(Long tenantId, ErpPurchaseOrderItem item, BigDecimal delta, String bizType, Long orderId) {
        Long warehouseId = item.getWarehouseId();
        Long locationId = item.getLocationId();
        String operator = resolveCurrentUsername();
        ErpStockBalance updatedBalance;
        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            updatedBalance = erpStockBalanceMapper.addQtyIfEnough(
                tenantId, item.getProductId(), warehouseId, locationId, delta, operator);
        } else {
            updatedBalance = erpStockBalanceMapper.upsertAddQty(
                tenantId, item.getProductId(), warehouseId, locationId, delta, operator);
        }
        if (updatedBalance == null) {
            ErpStockBalance currentBalance = erpStockBalanceMapper.findByKey(tenantId, item.getProductId(), warehouseId, locationId);
            BigDecimal currentQty = currentBalance == null || currentBalance.getQtyOnHand() == null
                ? BigDecimal.ZERO
                : currentBalance.getQtyOnHand();
            BigDecimal required = delta.abs();
            String productLabel = item.getProductName() == null ? item.getProductCode() : item.getProductName();
            throw new IllegalArgumentException(
                "库存不足，商品[" + productLabel + "] 可用=" + currentQty + "，需求=" + required
            );
        }
        BigDecimal after = updatedBalance.getQtyOnHand() == null ? BigDecimal.ZERO : updatedBalance.getQtyOnHand();
        BigDecimal before = after.subtract(delta);

        ErpStockTxn txn = new ErpStockTxn();
        txn.setTenantId(tenantId);
        txn.setTxnNo(generateTxnNo());
        txn.setBizType(bizType);
        txn.setBizId(orderId);
        txn.setBizItemId(item.getId());
        txn.setProductId(item.getProductId());
        txn.setWarehouseId(warehouseId);
        txn.setLocationId(locationId);
        txn.setQtyDelta(delta);
        txn.setQtyBefore(before);
        txn.setQtyAfter(after);
        BigDecimal unitCost = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
        BigDecimal totalCost = unitCost.multiply(delta).setScale(4, RoundingMode.HALF_UP);
        txn.setUnitCost(unitCost);
        txn.setTotalCost(totalCost);
        txn.setOperator(operator);
        txn.setOperatorId(null);
        txn.setRemark(item.getRemark());
        txn.setCreatedAt(Instant.now());
        erpStockTxnMapper.insert(txn);
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }

    private void applyInboundCost(Long tenantId, List<ErpPurchaseOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> qtyMap = new HashMap<>();
        Map<Long, BigDecimal> costMap = new HashMap<>();
        for (ErpPurchaseOrderItem item : items) {
            if (item.getProductId() == null || item.getQty() == null) {
                continue;
            }
            BigDecimal qty = item.getQty();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal unitCost = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            BigDecimal lineCost = unitCost.multiply(qty);
            qtyMap.merge(item.getProductId(), qty, BigDecimal::add);
            costMap.merge(item.getProductId(), lineCost, BigDecimal::add);
        }
        for (Map.Entry<Long, BigDecimal> entry : qtyMap.entrySet()) {
            Long productId = entry.getKey();
            BigDecimal inboundQty = entry.getValue();
            if (inboundQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal totalCost = costMap.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal inboundUnitCost = totalCost.divide(inboundQty, 4, RoundingMode.HALF_UP);
            erpCostService.applyInboundAverageCost(tenantId, productId, inboundQty, inboundUnitCost);
        }
    }

    private void reverseInboundCosts(Long tenantId, List<ErpPurchaseOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> qtyMap = new HashMap<>();
        Map<Long, BigDecimal> costMap = new HashMap<>();
        for (ErpPurchaseOrderItem item : items) {
            if (item == null || item.getProductId() == null || item.getQty() == null) {
                continue;
            }
            BigDecimal qty = item.getQty();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal unitCost = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            qtyMap.merge(item.getProductId(), qty, BigDecimal::add);
            costMap.merge(item.getProductId(), unitCost.multiply(qty), BigDecimal::add);
        }
        for (Map.Entry<Long, BigDecimal> entry : qtyMap.entrySet()) {
            BigDecimal qty = entry.getValue();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal removedUnitCost = costMap.getOrDefault(entry.getKey(), BigDecimal.ZERO)
                .divide(qty, 4, RoundingMode.HALF_UP);
            erpCostService.reverseInboundAverageCost(tenantId, entry.getKey(), qty, removedUnitCost);
        }
    }

    private String generateTxnNo() {
        return "TXN-" + UUID.randomUUID();
    }

    private String ensureOrderNo(Long tenantId, String provided, String orderType, String defaultPrefix) {
        String trimmed = provided == null ? "" : provided.trim();
        if (!trimmed.isEmpty()) {
            ErpPurchaseOrder existing = erpPurchaseOrderMapper.findByOrderNo(tenantId, trimmed);
            if (existing != null) {
                throw new IllegalArgumentException("采购单号已存在");
            }
            return trimmed;
        }
        String prefix = readConfig("erp.order.no.purchase.prefix", defaultPrefix);
        String dateFormat = readConfig("erp.order.no.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.order.no.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, orderType, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, orderType, dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private String generatePaymentNo(Long tenantId) {
        String prefix = readConfig("erp.order.no.payment.prefix", "PY");
        String dateFormat = readConfig("erp.order.no.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.order.no.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, "PAYMENT", dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, "PAYMENT", dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private String resolveOrderNoForUpdate(String provided, String current, Long tenantId, Long orderId) {
        if (provided == null || provided.isBlank()) {
            return current;
        }
        String trimmed = provided.trim();
        if (!trimmed.equals(current)) {
            ErpPurchaseOrder existing = erpPurchaseOrderMapper.findByOrderNo(tenantId, trimmed);
            if (existing != null && !existing.getId().equals(orderId)) {
                throw new IllegalArgumentException("采购单号已存在");
            }
        }
        return trimmed;
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

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Instant parseOrderAt(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        String trimmed = value.trim();
        try {
            if (trimmed.matches("\\d+")) {
                return Instant.ofEpochMilli(Long.parseLong(trimmed));
            }
            if (trimmed.contains("T")) {
                return Instant.parse(trimmed);
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(trimmed, formatter).atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception ex) {
            return Instant.now();
        }
    }

    private BigDecimal calcPriceInclTax(BigDecimal price, BigDecimal taxRate) {
        return price.multiply(BigDecimal.ONE.add(taxRate)).setScale(4, RoundingMode.HALF_UP);
    }

    private String appendRedFlushReason(String remark, String reason) {
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty()) {
            return remark;
        }
        String marker = "红冲原因：";
        String base = remark == null ? "" : remark.trim();
        String append = marker + trimmed;
        if (base.isEmpty()) {
            return append;
        }
        if (base.contains(marker)) {
            return base;
        }
        return base + "；" + append;
    }

    private BigDecimal calcPriceExclTax(BigDecimal priceInclTax, BigDecimal taxRate) {
        if (taxRate.compareTo(BigDecimal.ZERO) == 0) {
            return priceInclTax;
        }
        return priceInclTax.divide(BigDecimal.ONE.add(taxRate), 4, RoundingMode.HALF_UP);
    }
}
