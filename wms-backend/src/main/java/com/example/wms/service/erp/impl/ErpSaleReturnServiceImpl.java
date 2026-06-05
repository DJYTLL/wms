package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSaleOrderRecentItem;
import com.example.wms.dto.erp.ErpSaleReturnCreateRequest;
import com.example.wms.dto.erp.ErpSaleReturnDetail;
import com.example.wms.dto.erp.ErpSaleReturnItemRequest;
import com.example.wms.dto.erp.ErpSaleReturnRelatedOrder;
import com.example.wms.dto.erp.ErpSaleReturnRefundSnapshot;
import com.example.wms.dto.erp.ErpSaleReturnRefundSummary;
import com.example.wms.dto.erp.ErpSaleReturnSourceSaleOrderDetail;
import com.example.wms.dto.erp.ErpSaleReturnSourceSaleOrderItem;
import com.example.wms.dto.erp.ErpSaleReturnSourceSaleOrderOption;
import com.example.wms.dto.erp.ErpSaleReturnUpdateRequest;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpAccountsReceivable;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpReceipt;
import com.example.wms.entity.erp.ErpReceiptMethod;
import com.example.wms.entity.erp.ErpReceiptReceivable;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.entity.erp.ErpSaleOrderItem;
import com.example.wms.entity.erp.ErpSaleReturn;
import com.example.wms.entity.erp.ErpSaleReturnItem;
import com.example.wms.entity.erp.ErpSettlementMethod;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockTxn;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpReceiptMethodMapper;
import com.example.wms.mapper.erp.ErpReceiptReceivableMapper;
import com.example.wms.mapper.erp.ErpSaleOrderItemMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSaleReturnItemMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.TenantSettingService;
import com.example.wms.service.erp.ErpSaleReturnService;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// 销售退货服务实现（ERP进销存）
@Service
public class ErpSaleReturnServiceImpl implements ErpSaleReturnService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_RED_FLUSHED = "RED_FLUSHED";
    private static final String STATUS_SETTLED = "SETTLED";
    private static final String STATUS_OPEN = "OPEN";
    private static final String ORDER_TYPE = "SALE_RETURN";
    private static final String RECEIVABLE_ORDER_TYPE = "AR_RETURN";
    private static final String RECEIPT_ORDER_TYPE = "RECEIPT";
    private static final String SOURCE_SALE_ORDER = "SALE_ORDER";
    private static final String SOURCE_SALE_RETURN = "SALE_RETURN";

    private static final String RETURN_RESTOCK = "RESTOCK";
    private static final String RETURN_SCRAP = "SCRAP";
    private static final String REFUND_ACTION_REFUND = "REFUND";
    private static final String REFUND_ACTION_OFFSET_AR = "OFFSET_AR";

    private final ErpSaleReturnMapper erpSaleReturnMapper;
    private final ErpSaleReturnItemMapper erpSaleReturnItemMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpCustomerMapper erpCustomerMapper;
    private final ErpWarehouseMapper erpWarehouseMapper;
    private final ErpLocationMapper erpLocationMapper;
    private final ErpSettlementMethodMapper erpSettlementMethodMapper;
    private final ErpReceiptMethodMapper erpReceiptMethodMapper;
    private final ErpStockBalanceMapper erpStockBalanceMapper;
    private final ErpStockTxnMapper erpStockTxnMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final ErpAccountsReceivableMapper erpAccountsReceivableMapper;
    private final ErpReceiptMapper erpReceiptMapper;
    private final ErpReceiptReceivableMapper erpReceiptReceivableMapper;
    private final ErpSaleOrderItemMapper erpSaleOrderItemMapper;
    private final ErpSaleOrderMapper erpSaleOrderMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ErpCostService erpCostService;
    private final TenantSettingService tenantSettingService;

    public ErpSaleReturnServiceImpl(ErpSaleReturnMapper erpSaleReturnMapper,
                                    ErpSaleReturnItemMapper erpSaleReturnItemMapper,
                                    ErpProductMapper erpProductMapper,
                                    ErpCustomerMapper erpCustomerMapper,
                                    ErpWarehouseMapper erpWarehouseMapper,
                                    ErpLocationMapper erpLocationMapper,
                                    ErpSettlementMethodMapper erpSettlementMethodMapper,
                                    ErpReceiptMethodMapper erpReceiptMethodMapper,
                                    ErpStockBalanceMapper erpStockBalanceMapper,
                                    ErpStockTxnMapper erpStockTxnMapper,
                                    ErpOrderSequenceMapper erpOrderSequenceMapper,
                                    ErpAccountsReceivableMapper erpAccountsReceivableMapper,
                                    ErpReceiptMapper erpReceiptMapper,
                                    ErpReceiptReceivableMapper erpReceiptReceivableMapper,
                                    ErpSaleOrderItemMapper erpSaleOrderItemMapper,
                                    ErpSaleOrderMapper erpSaleOrderMapper,
                                    SystemConfigMapper systemConfigMapper,
                                    ErpCostService erpCostService,
                                    TenantSettingService tenantSettingService) {
        this.erpSaleReturnMapper = erpSaleReturnMapper;
        this.erpSaleReturnItemMapper = erpSaleReturnItemMapper;
        this.erpProductMapper = erpProductMapper;
        this.erpCustomerMapper = erpCustomerMapper;
        this.erpWarehouseMapper = erpWarehouseMapper;
        this.erpLocationMapper = erpLocationMapper;
        this.erpSettlementMethodMapper = erpSettlementMethodMapper;
        this.erpReceiptMethodMapper = erpReceiptMethodMapper;
        this.erpStockBalanceMapper = erpStockBalanceMapper;
        this.erpStockTxnMapper = erpStockTxnMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.erpAccountsReceivableMapper = erpAccountsReceivableMapper;
        this.erpReceiptMapper = erpReceiptMapper;
        this.erpReceiptReceivableMapper = erpReceiptReceivableMapper;
        this.erpSaleOrderItemMapper = erpSaleOrderItemMapper;
        this.erpSaleOrderMapper = erpSaleOrderMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.erpCostService = erpCostService;
        this.tenantSettingService = tenantSettingService;
    }

    @Override
    public List<ErpSaleReturn> listAll(String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpSaleReturn> wrapper = baseWrapper(keyword, status, customerId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        List<ErpSaleReturn> returns = erpSaleReturnMapper.selectList(wrapper);
        enrichFlowStatus(TenantContext.requireTenantId(), returns);
        return returns;
    }

    @Override
    public PageResponse<ErpSaleReturn> page(long page, long size, String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        Page<ErpSaleReturn> pageReq = Page.of(page, size);
        QueryWrapper<ErpSaleReturn> wrapper = baseWrapper(keyword, status, customerId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        Page<ErpSaleReturn> result = erpSaleReturnMapper.selectPage(pageReq, wrapper);
        enrichFlowStatus(TenantContext.requireTenantId(), result.getRecords());
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public PageResponse<ErpSaleReturn> draftPage(long page, long size, String keyword, Long customerId, Instant startAt, Instant endAt) {
        return page(page, size, keyword, STATUS_DRAFT, customerId, startAt, endAt);
    }

    @Override
    public PageResponse<ErpSaleReturn> approvedPage(long page, long size, String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        String approvedStatus = normalizeApprovedStatusFilter(status);
        return page(page, size, keyword, approvedStatus, customerId, startAt, endAt);
    }

    @Override
    public PageResponse<ErpSaleReturnSourceSaleOrderOption> sourceSaleOrderPage(long page, long size, String keyword, Long customerId, Long currentReturnId) {
        Long tenantId = TenantContext.requireTenantId();
        long finalPage = page <= 0 ? 1 : page;
        long finalSize = size <= 0 ? 20 : Math.min(size, 100);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        long total = erpSaleOrderMapper.countReturnableSourceOrders(tenantId, customerId, normalizedKeyword, currentReturnId);
        long offset = (finalPage - 1) * finalSize;
        List<ErpSaleReturnSourceSaleOrderOption> items = total == 0
            ? List.of()
            : erpSaleOrderMapper.findReturnableSourceOrdersPage(
                tenantId,
                customerId,
                normalizedKeyword,
                currentReturnId,
                (int) finalSize,
                offset
            );
        return new PageResponse<>(total, finalPage, finalSize, items);
    }

    @Override
    public PageResponse<ErpSaleOrderRecentItem> sourceRecentSaleItems(long page, long size, Long customerId, Long productId, Long currentReturnId) {
        Long tenantId = TenantContext.requireTenantId();
        long finalPage = page <= 0 ? 1 : page;
        long finalSize = size <= 0 ? 10 : Math.min(size, 100);
        if (customerId == null || productId == null) {
            return new PageResponse<>(0, finalPage, finalSize, List.of());
        }
        long total = erpSaleOrderItemMapper.countRecentItems(tenantId, customerId, productId, currentReturnId);
        long offset = (finalPage - 1) * finalSize;
        List<ErpSaleOrderRecentItem> items = total == 0
            ? List.of()
            : erpSaleOrderItemMapper.findRecentItemsPage(tenantId, customerId, productId, (int) finalSize, offset, currentReturnId);
        return new PageResponse<>(total, finalPage, finalSize, items);
    }

    @Override
    public List<ErpSaleReturn> listBySaleOrderId(Long saleOrderId, boolean includeDraft) {
        Long tenantId = TenantContext.requireTenantId();
        if (saleOrderId == null) {
            return List.of();
        }
        QueryWrapper<ErpSaleReturn> wrapper = new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", tenantId)
            .eq("sale_order_id", saleOrderId)
            .orderByAsc("approved_at")
            .orderByAsc("created_at")
            .orderByAsc("id");
        if (includeDraft) {
            wrapper.and(qw -> qw.eq("status", STATUS_APPROVED).or().eq("status", STATUS_DRAFT));
        } else {
            wrapper.eq("status", STATUS_APPROVED);
        }
        List<ErpSaleReturn> returns = erpSaleReturnMapper.selectList(wrapper);
        enrichFlowStatus(tenantId, returns);
        return returns;
    }

    @Override
    public ErpSaleReturnDetail getDetail(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSaleReturn order = erpSaleReturnMapper.selectOne(new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (order == null) {
            throw new IllegalArgumentException("销售退货单不存在");
        }
        List<ErpSaleReturnItem> items = erpSaleReturnItemMapper.findByReturnId(tenantId, id);
        enrichSourceItemAvailability(tenantId, items, id);
        return new ErpSaleReturnDetail(order, items);
    }

    @Override
    public ErpSaleReturnDetail getDraftDetail(Long id) {
        ErpSaleReturnDetail detail = getDetail(id);
        if (!STATUS_DRAFT.equals(detail.order().getStatus())) {
            throw new IllegalArgumentException("销售退货草稿不存在");
        }
        return detail;
    }

    @Override
    public ErpSaleReturnDetail getApprovedDetail(Long id) {
        ErpSaleReturnDetail detail = getDetail(id);
        if (STATUS_DRAFT.equals(detail.order().getStatus())) {
            throw new IllegalArgumentException("销售退货已审核单不存在");
        }
        return detail;
    }

    @Override
    public ErpSaleReturnSourceSaleOrderDetail getSourceSaleOrderDetail(Long saleOrderId, Long currentReturnId) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSaleOrder saleOrder = loadApprovedSaleOrder(tenantId, saleOrderId);
        List<ErpSaleReturnSourceSaleOrderItem> items = buildSourceSaleOrderItems(tenantId, saleOrderId, currentReturnId);
        ErpSaleReturnRefundSummary refundSummary = buildSaleOrderRefundSummary(tenantId, saleOrder, null);
        List<ErpSaleReturnRelatedOrder> relatedReturns = listBySaleOrderId(saleOrderId, true).stream()
            .map(item -> new ErpSaleReturnRelatedOrder(item.getId(), item.getOrderNo(), item.getStatus()))
            .toList();
        return new ErpSaleReturnSourceSaleOrderDetail(
            saleOrder.getId(),
            saleOrder.getOrderNo(),
            saleOrder.getCustomerId(),
            saleOrder.getOrderAt(),
            items,
            refundSummary,
            relatedReturns
        );
    }

    @Override
    public ErpSaleReturnRefundSummary getSaleOrderRefundSummary(Long saleOrderId) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSaleOrder saleOrder = loadApprovedSaleOrder(tenantId, saleOrderId);
        return buildSaleOrderRefundSummary(tenantId, saleOrder, null);
    }

    @Override
    public String nextOrderNo() {
        Long tenantId = TenantContext.requireTenantId();
        return ensureOrderNo(tenantId, null);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_SALE_RETURN_CREATE", entityType = "erp_sale_return", entityId = "{result.order.id}", detail = "orderNo={result.order.orderNo}")
    public ErpSaleReturnDetail create(ErpSaleReturnCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String operator = resolveCurrentUsername();
        validateSaleReturnSourceFromRequest(tenantId, request.saleOrderId(), request.customerId(), request.items(), null);
        ErpSaleReturn order = new ErpSaleReturn();
        order.setTenantId(tenantId);
        order.setOrderNo(ensureOrderNo(tenantId, request.orderNo()));
        order.setStatus(STATUS_DRAFT);
        order.setReturnType(resolveReturnType(request.returnType()));
        order.setCustomerId(request.customerId());
        order.setSaleOrderId(request.saleOrderId());
        order.setOrderAt(parseOrderAt(request.orderAt()));
        order.setSettlementMethod(request.settlementMethod());
        order.setReceiptMethodCode(normalizeCode(request.receiptMethodCode()));
        order.setRefundAction(resolveRefundAction(request.refundAction()));
        BigDecimal requestedPaidAmount = request.paidAmount() == null ? BigDecimal.ZERO : request.paidAmount();
        BigDecimal requestedDiscountAmount = request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount();
        if (isFundInputHidden(tenantId, order.getSettlementMethod())) {
            order.setReceiptMethodCode(null);
            requestedPaidAmount = BigDecimal.ZERO;
        }
        validateHeaderMasterData(tenantId, order.getCustomerId(), order.getSettlementMethod(),
            order.getReceiptMethodCode(), order.getRefundAction(), requestedPaidAmount);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        applyTotals(order, List.of());
        order.setVersion(0L);
        order.setRemark(request.remark());
        order.setCreatedAt(Instant.now());
        order.setCreatedBy(operator);
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(operator);
        erpSaleReturnMapper.insert(order);

        List<ErpSaleReturnItem> items = buildItems(tenantId, order.getId(), request.items(), Set.of());
        for (ErpSaleReturnItem item : items) {
            erpSaleReturnItemMapper.insert(item);
        }
        validateNoOtherDraftOccupancy(tenantId, order.getSaleOrderId(), items, order.getId());
        applyTotals(order, items);
        order.setPaidAmount(requestedPaidAmount);
        order.setDiscountAmount(requestedDiscountAmount);
        validateSettlementAmounts(tenantId, order, null);
        order.setUpdatedBy(operator);
        erpSaleReturnMapper.updateById(order);

        return new ErpSaleReturnDetail(order, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_SALE_RETURN_CREATE", entityType = "erp_sale_return", entityId = "{result.order.id}", detail = "copyFrom={arg0}")
    public ErpSaleReturnDetail copyToDraft(Long id) {
        ErpSaleReturnDetail source = getApprovedDetail(id);
        ErpSaleReturn order = source.order();
        List<ErpSaleReturnItemRequest> items = source.items().stream()
            .map(item -> new ErpSaleReturnItemRequest(
                item.getProductId(),
                item.getSourceSaleOrderItemId(),
                item.getSourceSaleOrderId(),
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
        ErpSaleReturnCreateRequest request = new ErpSaleReturnCreateRequest(
            null,
            order.getOrderAt() == null ? null : order.getOrderAt().toString(),
            order.getReturnType(),
            order.getCustomerId(),
            order.getSaleOrderId(),
            order.getSettlementMethod(),
            order.getReceiptMethodCode(),
            order.getRefundAction(),
            order.getPaidAmount(),
            order.getDiscountAmount(),
            items,
            order.getRemark()
        );
        return create(request);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_SALE_RETURN_UPDATE", entityType = "erp_sale_return", entityId = "{arg0}", detail = "orderNo={arg1.orderNo}")
    public ErpSaleReturnDetail update(Long id, ErpSaleReturnUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSaleReturn order = loadForUpdate(tenantId, id);
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿可编辑");
        }
        validateSaleReturnSourceFromRequest(tenantId, request.saleOrderId(), request.customerId(), request.items(), id);
        order.setReturnType(resolveReturnType(request.returnType()));
        order.setCustomerId(request.customerId());
        order.setSaleOrderId(request.saleOrderId());
        order.setOrderAt(parseOrderAt(request.orderAt()));
        order.setSettlementMethod(request.settlementMethod());
        order.setReceiptMethodCode(normalizeCode(request.receiptMethodCode()));
        order.setRefundAction(resolveRefundAction(request.refundAction()));
        order.setPaidAmount(request.paidAmount());
        order.setDiscountAmount(request.discountAmount());
        if (isFundInputHidden(tenantId, order.getSettlementMethod())) {
            order.setReceiptMethodCode(null);
            order.setPaidAmount(BigDecimal.ZERO);
        }
        validateHeaderMasterData(tenantId, order.getCustomerId(), order.getSettlementMethod(),
            order.getReceiptMethodCode(), order.getRefundAction(), order.getPaidAmount());
        order.setRemark(request.remark());
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(resolveCurrentUsername());

        Set<Long> allowedDisabledProductIds = existingProductIds(erpSaleReturnItemMapper.findByReturnId(tenantId, id));

        erpSaleReturnItemMapper.delete(new QueryWrapper<ErpSaleReturnItem>()
            .eq("tenant_id", tenantId)
            .eq("return_id", id));
        List<ErpSaleReturnItem> items = buildItems(tenantId, id, request.items(), allowedDisabledProductIds);
        for (ErpSaleReturnItem item : items) {
            erpSaleReturnItemMapper.insert(item);
        }
        validateNoOtherDraftOccupancy(tenantId, order.getSaleOrderId(), items, id);
        applyTotals(order, items);
        validateSettlementAmounts(tenantId, order, id);
        updateWithVersion(tenantId, order);

        return new ErpSaleReturnDetail(order, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_SALE_RETURN_DELETE", entityType = "erp_sale_return", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSaleReturn order = loadForUpdate(tenantId, id);
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿可删除");
        }
        erpSaleReturnItemMapper.delete(new QueryWrapper<ErpSaleReturnItem>()
            .eq("tenant_id", tenantId)
            .eq("return_id", id));
        erpSaleReturnMapper.deleteById(id);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_SALE_RETURN_APPROVE", entityType = "erp_sale_return", entityId = "{arg0}")
    public void approve(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSaleReturn order = loadForUpdate(tenantId, id);
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿可审核");
        }
        List<ErpSaleReturnItem> items = erpSaleReturnItemMapper.findByReturnId(tenantId, id);
        lockSourceSaleOrderItems(tenantId, items);
        validateNoOtherDraftOccupancy(tenantId, order.getSaleOrderId(), items, id);
        validateSaleReturnSourceFromItems(tenantId, order.getSaleOrderId(), order.getCustomerId(), items, id);
        validateSettlementAmounts(tenantId, order, id);
        String operator = resolveCurrentUsername();
        ErpSaleReturn approved = erpSaleReturnMapper.approveDraft(tenantId, id, operator);
        if (approved == null) {
            throw new IllegalArgumentException("销售退货单状态已变化，请刷新重试");
        }

        String returnType = resolveReturnType(order.getReturnType());
        for (ErpSaleReturnItem item : items) {
            if (item.getQty() == null) {
                throw new IllegalArgumentException("退货数量不能为空");
            }
            if (RETURN_RESTOCK.equals(returnType)) {
                applyStockDelta(tenantId, item, item.getQty(), "SALE_RETURN_RESTOCK", id, order.getSaleOrderId(), true);
            } else {
                applyStockDelta(tenantId, item, BigDecimal.ZERO, "SALE_RETURN_SCRAP", id, order.getSaleOrderId(), false);
            }
        }
        createReturnReceivable(tenantId, order, resolveReturnTotal(order), operator);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_SALE_RETURN_RED_FLUSH", entityType = "erp_sale_return", entityId = "{arg0}")
    public void redFlush(Long id, String reason) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSaleReturn order = loadForUpdate(tenantId, id);
        if (!STATUS_APPROVED.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅已审核状态可红冲");
        }
        String reasonText = reason == null ? "" : reason.trim();
        if (reasonText.isEmpty()) {
            throw new IllegalArgumentException("请填写红冲原因");
        }
        ErpAccountsReceivable returnReceivable = findReturnReceivable(tenantId, order);
        if (returnReceivable != null && hasApprovedRefundReceipt(tenantId, returnReceivable.getId())) {
            throw new IllegalArgumentException("请先红冲退款单");
        }
        String originRemark = order.getRemark();
        String redFlushRemark;
        if (originRemark == null || originRemark.isBlank()) {
            redFlushRemark = "红冲原因：" + reasonText;
        } else {
            redFlushRemark = originRemark + " | 红冲原因：" + reasonText;
        }
        String operator = resolveCurrentUsername();
        ErpSaleReturn redFlushed = erpSaleReturnMapper.redFlushApproved(tenantId, id, redFlushRemark, operator);
        if (redFlushed == null) {
            throw new IllegalArgumentException("销售退货单状态已变化，请刷新重试");
        }

        List<ErpSaleReturnItem> items = erpSaleReturnItemMapper.findByReturnId(tenantId, id);
        String returnType = resolveReturnType(order.getReturnType());
        if (RETURN_RESTOCK.equals(returnType)) {
            reverseRestockCosts(tenantId, order.getSaleOrderId(), items);
        }
        for (ErpSaleReturnItem item : items) {
            if (item.getQty() == null) {
                throw new IllegalArgumentException("退货数量不能为空");
            }
            if (RETURN_RESTOCK.equals(returnType)) {
                applyStockDelta(tenantId, item, item.getQty().negate(), "SALE_RETURN_RED_FLUSH", id, order.getSaleOrderId(), true);
            } else {
                applyStockDelta(tenantId, item, BigDecimal.ZERO, "SALE_RETURN_RED_FLUSH", id, order.getSaleOrderId(), false);
            }
        }
        redFlushReturnReceivable(tenantId, order, reasonText);
    }

    private QueryWrapper<ErpSaleReturn> baseWrapper(String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpSaleReturn> wrapper = new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("order_no", keyword.trim());
        }
        if (status != null && !status.isBlank()) {
            String trimmed = status.trim();
            if (trimmed.contains(",")) {
                String[] parts = trimmed.split(",");
                List<String> statuses = new ArrayList<>();
                for (String part : parts) {
                    if (!part.isBlank()) {
                        statuses.add(part.trim());
                    }
                }
                if (!statuses.isEmpty()) {
                    wrapper.in("status", statuses);
                }
            } else {
                wrapper.eq("status", trimmed);
            }
        }
        if (customerId != null) {
            wrapper.eq("customer_id", customerId);
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
            return STATUS_APPROVED + "," + STATUS_RED_FLUSHED;
        }
        List<String> statuses = new ArrayList<>();
        for (String part : status.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isBlank() && !STATUS_DRAFT.equals(trimmed)) {
                statuses.add(trimmed);
            }
        }
        if (statuses.isEmpty()) {
            return STATUS_APPROVED + "," + STATUS_RED_FLUSHED;
        }
        return String.join(",", statuses);
    }

    private void validateHeaderMasterData(Long tenantId,
                                          Long customerId,
                                          String settlementMethod,
                                          String receiptMethodCode,
                                          String refundAction,
                                          BigDecimal paidAmount) {
        if (customerId == null) {
            throw new IllegalArgumentException("请选择客户");
        }
        ErpCustomer customer = erpCustomerMapper.selectOne(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .eq("id", customerId));
        if (customer == null || Boolean.FALSE.equals(customer.getEnabled())) {
            throw new IllegalArgumentException("客户不存在或已停用");
        }
        if (settlementMethod == null || settlementMethod.isBlank()) {
            throw new IllegalArgumentException("请选择结算方式");
        }
        ErpSettlementMethod method = resolveSettlementMethod(tenantId, settlementMethod);
        if (method == null || Boolean.FALSE.equals(method.getEnabled())) {
            throw new IllegalArgumentException("结算方式不存在或已停用");
        }
        if (REFUND_ACTION_REFUND.equals(refundAction) && paidAmount != null && paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (receiptMethodCode == null || receiptMethodCode.isBlank()) {
                throw new IllegalArgumentException("请选择收款方式");
            }
            ErpReceiptMethod receiptMethod = erpReceiptMethodMapper.findByCode(tenantId, receiptMethodCode);
            if (receiptMethod == null || Boolean.FALSE.equals(receiptMethod.getEnabled())) {
                throw new IllegalArgumentException("收款方式不存在或已停用");
            }
        }
    }

    private String resolveRefundAction(String action) {
        if (action == null || action.isBlank()) {
            return REFUND_ACTION_OFFSET_AR;
        }
        String normalized = action.trim().toUpperCase();
        if (REFUND_ACTION_REFUND.equals(normalized)) {
            return REFUND_ACTION_REFUND;
        }
        return REFUND_ACTION_OFFSET_AR;
    }

    private boolean isFundInputHidden(Long tenantId, String settlementMethod) {
        if (settlementMethod == null || settlementMethod.isBlank()) {
            return false;
        }
        ErpSettlementMethod method = resolveSettlementMethod(tenantId, settlementMethod);
        if (method == null) {
            return false;
        }
        return "HIDDEN".equalsIgnoreCase(method.getFundInputMode());
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

    private void validateStockBinding(Long tenantId, Long warehouseId, Long locationId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("请选择仓库");
        }
        ErpWarehouse warehouse = erpWarehouseMapper.selectOne(new QueryWrapper<ErpWarehouse>()
            .eq("tenant_id", tenantId)
            .eq("id", warehouseId));
        if (warehouse == null || Boolean.FALSE.equals(warehouse.getEnabled())) {
            throw new IllegalArgumentException("仓库不存在或已停用");
        }
        if (locationId != null) {
            ErpLocation location = erpLocationMapper.selectOne(new QueryWrapper<ErpLocation>()
                .eq("tenant_id", tenantId)
                .eq("id", locationId));
            if (location == null || Boolean.FALSE.equals(location.getEnabled()) || !warehouseId.equals(location.getWarehouseId())) {
                throw new IllegalArgumentException("库位不存在、已停用或不属于所选仓库");
            }
        }
    }

    private List<ErpSaleReturnItem> buildItems(Long tenantId,
                                               Long returnId,
                                               List<ErpSaleReturnItemRequest> requests,
                                               Set<Long> allowedDisabledProductIds) {
        List<ErpSaleReturnItem> items = new ArrayList<>();
        int index = 1;
        for (ErpSaleReturnItemRequest request : requests) {
            ErpProduct product = requireUsableProduct(tenantId, request.productId(), allowedDisabledProductIds);
            ErpSaleReturnItem item = new ErpSaleReturnItem();
            item.setTenantId(tenantId);
            item.setReturnId(returnId);
            item.setProductId(product.getId());
            item.setSourceSaleOrderItemId(request.sourceSaleOrderItemId());
            item.setSourceSaleOrderId(request.sourceSaleOrderId());
            item.setProductCode(product.getCode());
            item.setProductName(product.getName());
            Long warehouseId;
            Long locationId;
            if (request.warehouseId() == null && request.locationId() == null) {
                warehouseId = product.getDefaultWarehouseId();
                locationId = product.getDefaultLocationId();
            } else {
                warehouseId = request.warehouseId();
                locationId = request.locationId();
            }
            validateStockBinding(tenantId, warehouseId, locationId);
            item.setWarehouseId(warehouseId);
            item.setLocationId(locationId);
            if (request.qty() == null || request.qty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("退货数量必须大于0");
            }
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

    private Set<Long> existingProductIds(List<ErpSaleReturnItem> items) {
        Set<Long> ids = new HashSet<>();
        if (items == null) {
            return ids;
        }
        for (ErpSaleReturnItem item : items) {
            if (item != null && item.getProductId() != null) {
                ids.add(item.getProductId());
            }
        }
        return ids;
    }

    private void validateSaleReturnSourceFromRequest(Long tenantId,
                                                     Long saleOrderId,
                                                     Long customerId,
                                                     List<ErpSaleReturnItemRequest> requests,
                                                     Long currentReturnId) {
        if (saleOrderId == null) {
            throw new IllegalArgumentException("销售退货必须关联原销售单");
        }
        if (customerId == null) {
            throw new IllegalArgumentException("销售退货必须选择客户");
        }
        validateSaleReturnSource(
            tenantId,
            saleOrderId,
            customerId,
            buildRequestedReturnLines(requests),
            currentReturnId
        );
    }

    private void validateSaleReturnSourceFromItems(Long tenantId,
                                                   Long saleOrderId,
                                                   Long customerId,
                                                   List<ErpSaleReturnItem> items,
                                                   Long currentReturnId) {
        if (saleOrderId == null) {
            throw new IllegalArgumentException("销售退货必须关联原销售单");
        }
        if (customerId == null) {
            throw new IllegalArgumentException("销售退货必须选择客户");
        }
        validateSaleReturnSource(
            tenantId,
            saleOrderId,
            customerId,
            buildExistingReturnLines(items),
            currentReturnId
        );
    }

    private void validateSaleReturnSource(Long tenantId,
                                          Long saleOrderId,
                                          Long customerId,
                                          List<ReturnSourceLine> requestLines,
                                          Long currentReturnId) {
        if (requestLines == null || requestLines.isEmpty()) {
            throw new IllegalArgumentException("销售退货明细不能为空");
        }
        Map<Long, List<ReturnSourceLine>> linesBySaleOrder = new LinkedHashMap<>();
        for (ReturnSourceLine line : requestLines) {
            if (line == null) {
                continue;
            }
            if (line.sourceSaleOrderId() == null || line.sourceSaleOrderItemId() == null) {
                throw new IllegalArgumentException("销售退货明细必须绑定来源销售单和来源销售明细");
            }
            Long sourceSaleOrderId = line.sourceSaleOrderId();
            linesBySaleOrder.computeIfAbsent(sourceSaleOrderId, ignored -> new ArrayList<>()).add(line);
        }
        for (Map.Entry<Long, List<ReturnSourceLine>> entry : linesBySaleOrder.entrySet()) {
            validateSaleReturnSourceForOrder(tenantId, entry.getKey(), customerId, entry.getValue(), currentReturnId);
        }
    }

    private void validateSaleReturnSourceForOrder(Long tenantId,
                                                  Long saleOrderId,
                                                  Long customerId,
                                                  List<ReturnSourceLine> requestLines,
                                                  Long currentReturnId) {
        ErpSaleOrder saleOrder = loadApprovedSaleOrder(tenantId, saleOrderId);
        if (!customerId.equals(saleOrder.getCustomerId())) {
            throw new IllegalArgumentException("退货客户必须与原销售单客户一致");
        }
        if (requestLines == null || requestLines.isEmpty()) {
            throw new IllegalArgumentException("销售退货明细不能为空");
        }

        List<ErpSaleOrderItem> saleItems = erpSaleOrderItemMapper.findByOrderId(tenantId, saleOrderId);
        Map<Long, ErpSaleOrderItem> saleItemById = saleItems.stream()
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(ErpSaleOrderItem::getId, item -> item, (left, right) -> left));
        Map<Long, BigDecimal> soldQtyByProduct = new HashMap<>();
        Map<Long, BigDecimal> soldAmountByProduct = new HashMap<>();
        for (ErpSaleOrderItem saleItem : saleItems) {
            if (saleItem.getProductId() == null || saleItem.getQty() == null) {
                continue;
            }
            soldQtyByProduct.merge(saleItem.getProductId(), saleItem.getQty(), BigDecimal::add);
            soldAmountByProduct.merge(
                saleItem.getProductId(),
                saleItem.getAmountInclTax() == null ? BigDecimal.ZERO : saleItem.getAmountInclTax(),
                BigDecimal::add
            );
        }

        Map<Long, BigDecimal> requestQtyBySourceItem = new HashMap<>();
        Map<Long, BigDecimal> requestAmountBySourceItem = new HashMap<>();
        Map<Long, BigDecimal> requestQtyByProduct = new HashMap<>();
        Map<Long, BigDecimal> requestAmountByProduct = new HashMap<>();
        for (ReturnSourceLine line : requestLines) {
            if (line == null || line.productId() == null || line.qty() == null) {
                continue;
            }
            if (line.sourceSaleOrderItemId() != null) {
                requestQtyBySourceItem.merge(line.sourceSaleOrderItemId(), line.qty(), BigDecimal::add);
                requestAmountBySourceItem.merge(line.sourceSaleOrderItemId(), line.amountInclTax(), BigDecimal::add);
            } else {
                requestQtyByProduct.merge(line.productId(), line.qty(), BigDecimal::add);
                requestAmountByProduct.merge(line.productId(), line.amountInclTax(), BigDecimal::add);
            }
        }

        Map<Long, BigDecimal> approvedQtyBySourceItem = loadApprovedReturnQtyBySourceItem(tenantId, saleOrderId, currentReturnId);
        Map<Long, BigDecimal> approvedAmountBySourceItem = loadApprovedReturnAmountBySourceItem(tenantId, saleOrderId, currentReturnId);

        for (Map.Entry<Long, BigDecimal> entry : requestQtyBySourceItem.entrySet()) {
            Long sourceItemId = entry.getKey();
            ErpSaleOrderItem saleItem = saleItemById.get(sourceItemId);
            if (saleItem == null) {
                throw new IllegalArgumentException("退货来源销售明细不存在");
            }
            validateSourceItemLines(requestLines, saleItem);
            validateNoDuplicateDestinationForSourceItem(requestLines, saleItem);
            BigDecimal soldQty = zeroIfNull(saleItem.getQty());
            BigDecimal approvedQty = approvedQtyBySourceItem.getOrDefault(sourceItemId, BigDecimal.ZERO);
            BigDecimal remainingQty = soldQty.subtract(approvedQty);
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("来源销售明细退货数量已达上限");
            }
            BigDecimal requestQty = entry.getValue();
            if (requestQty.compareTo(remainingQty) > 0) {
                throw new IllegalArgumentException("退货数量不能超过来源销售明细可退数量");
            }
            BigDecimal soldAmount = zeroIfNull(saleItem.getAmountInclTax());
            BigDecimal approvedAmount = approvedAmountBySourceItem.getOrDefault(sourceItemId, BigDecimal.ZERO);
            BigDecimal remainingAmount = soldAmount.subtract(approvedAmount);
            BigDecimal requestAmount = requestAmountBySourceItem.getOrDefault(sourceItemId, BigDecimal.ZERO);
            if (requestAmount.compareTo(remainingAmount) > 0) {
                throw new IllegalArgumentException("退货金额不能超过来源销售明细可退金额");
            }
            BigDecimal maxAmountByQty = calcLineUnitAmount(soldAmount, soldQty)
                .multiply(requestQty)
                .setScale(2, RoundingMode.HALF_UP);
            if (requestAmount.compareTo(maxAmountByQty) > 0) {
                throw new IllegalArgumentException("退货单价不能高于来源销售明细单价");
            }
        }

        Map<Long, BigDecimal> approvedReturnQtyByProduct = loadApprovedReturnQtyByProduct(tenantId, saleOrderId, currentReturnId);
        Map<Long, BigDecimal> approvedReturnAmountByProduct = loadApprovedReturnAmountByProduct(tenantId, saleOrderId, currentReturnId);
        for (Map.Entry<Long, BigDecimal> entry : requestQtyByProduct.entrySet()) {
            Long productId = entry.getKey();
            BigDecimal requestQty = entry.getValue();
            BigDecimal soldQty = soldQtyByProduct.get(productId);
            if (soldQty == null || soldQty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("退货商品必须存在于原销售单");
            }
            BigDecimal approvedQty = approvedReturnQtyByProduct.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal remainingQty = soldQty.subtract(approvedQty);
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("商品退货数量已达原销售上限");
            }
            if (requestQty.compareTo(remainingQty) > 0) {
                throw new IllegalArgumentException("商品退货数量不能超过原销售可退数量");
            }
            BigDecimal requestAmount = requestAmountByProduct.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal soldAmount = soldAmountByProduct.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal approvedAmount = approvedReturnAmountByProduct.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal remainingAmount = soldAmount.subtract(approvedAmount);
            if (requestAmount.compareTo(remainingAmount) > 0) {
                throw new IllegalArgumentException("商品退货金额不能超过原销售可退金额");
            }
            BigDecimal maxAmountByQty = soldAmount
                .divide(soldQty, 6, RoundingMode.HALF_UP)
                .multiply(requestQty)
                .setScale(2, RoundingMode.HALF_UP);
            if (requestAmount.compareTo(maxAmountByQty) > 0) {
                throw new IllegalArgumentException("商品退货单价不能高于原销售单价");
            }
        }
    }

    private ErpSaleOrder loadApprovedSaleOrder(Long tenantId, Long saleOrderId) {
        ErpSaleOrder saleOrder = erpSaleOrderMapper.selectOne(new QueryWrapper<ErpSaleOrder>()
            .eq("tenant_id", tenantId)
            .eq("id", saleOrderId));
        if (saleOrder == null) {
            throw new IllegalArgumentException("原销售单不存在");
        }
        if (!STATUS_APPROVED.equals(saleOrder.getStatus())) {
            throw new IllegalArgumentException("原销售单未审核，不能创建销售退货");
        }
        return saleOrder;
    }

    private List<ErpSaleReturnSourceSaleOrderItem> buildSourceSaleOrderItems(Long tenantId, Long saleOrderId, Long currentReturnId) {
        List<ErpSaleOrderItem> saleItems = erpSaleOrderItemMapper.findByOrderId(tenantId, saleOrderId);
        Map<Long, BigDecimal> returnedQtyBySourceItem = loadApprovedReturnQtyBySourceItem(tenantId, saleOrderId, null);
        Map<Long, BigDecimal> draftQtyBySourceItem = loadDraftReturnQtyBySourceItem(tenantId, saleOrderId, currentReturnId);
        List<ErpSaleReturnSourceSaleOrderItem> items = new ArrayList<>();
        for (ErpSaleOrderItem item : saleItems) {
            BigDecimal originalQty = item.getQty() == null ? BigDecimal.ZERO : item.getQty();
            BigDecimal returnedQty = returnedQtyBySourceItem.getOrDefault(item.getId(), BigDecimal.ZERO);
            BigDecimal draftQty = draftQtyBySourceItem.getOrDefault(item.getId(), BigDecimal.ZERO);
            BigDecimal remainingQty = originalQty.subtract(returnedQty).subtract(draftQty).max(BigDecimal.ZERO);
            items.add(new ErpSaleReturnSourceSaleOrderItem(
                item.getId(),
                item.getSortNo(),
                item.getProductId(),
                item.getProductCode(),
                item.getProductName(),
                item.getWarehouseId(),
                item.getLocationId(),
                item.getQty(),
                remainingQty,
                returnedQty,
                draftQty,
                item.getPrice(),
                item.getPriceInclTax(),
                item.getTaxRate()
            ));
        }
        return items;
    }

    private record ReturnSourceLine(Long sourceSaleOrderItemId,
                                    Long sourceSaleOrderId,
                                    Long productId,
                                    Long warehouseId,
                                    Long locationId,
                                    BigDecimal qty,
                                    BigDecimal amountInclTax) {
    }

    private List<ReturnSourceLine> buildRequestedReturnLines(List<ErpSaleReturnItemRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        List<ReturnSourceLine> lines = new ArrayList<>();
        for (ErpSaleReturnItemRequest request : requests) {
            if (request == null || request.productId() == null || request.qty() == null) {
                continue;
            }
            BigDecimal taxRate = request.taxRate() == null ? BigDecimal.ZERO : request.taxRate();
            BigDecimal priceInclTax = request.priceInclTax();
            if (priceInclTax == null && request.price() != null) {
                priceInclTax = calcPriceInclTax(request.price(), taxRate);
            }
            BigDecimal amountInclTax = (priceInclTax == null ? BigDecimal.ZERO : priceInclTax).multiply(request.qty());
            lines.add(new ReturnSourceLine(
                request.sourceSaleOrderItemId(),
                request.sourceSaleOrderId(),
                request.productId(),
                request.warehouseId(),
                request.locationId(),
                request.qty(),
                amountInclTax
            ));
        }
        return lines;
    }

    private List<ReturnSourceLine> buildExistingReturnLines(List<ErpSaleReturnItem> items) {
        if (items == null) {
            return List.of();
        }
        List<ReturnSourceLine> lines = new ArrayList<>();
        for (ErpSaleReturnItem item : items) {
            if (item == null || item.getProductId() == null || item.getQty() == null) {
                continue;
            }
            lines.add(new ReturnSourceLine(
                item.getSourceSaleOrderItemId(),
                item.getSourceSaleOrderId(),
                item.getProductId(),
                item.getWarehouseId(),
                item.getLocationId(),
                item.getQty(),
                item.getAmountInclTax() == null ? BigDecimal.ZERO : item.getAmountInclTax()
            ));
        }
        return lines;
    }

    private void enrichSourceItemAvailability(Long tenantId, List<ErpSaleReturnItem> items, Long currentReturnId) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Long, List<ErpSaleReturnItem>> itemsBySaleOrder = new LinkedHashMap<>();
        for (ErpSaleReturnItem item : items) {
            if (item == null || item.getSourceSaleOrderItemId() == null || item.getSourceSaleOrderId() == null) {
                continue;
            }
            itemsBySaleOrder.computeIfAbsent(item.getSourceSaleOrderId(), ignored -> new ArrayList<>()).add(item);
        }
        for (Map.Entry<Long, List<ErpSaleReturnItem>> entry : itemsBySaleOrder.entrySet()) {
            List<ErpSaleOrderItem> saleItems = erpSaleOrderItemMapper.findByOrderId(tenantId, entry.getKey());
            Map<Long, ErpSaleOrderItem> saleItemById = saleItems.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(ErpSaleOrderItem::getId, item -> item, (left, right) -> left));
            Map<Long, BigDecimal> approvedQtyBySourceItem = loadApprovedReturnQtyBySourceItem(tenantId, entry.getKey(), currentReturnId);
            Map<Long, BigDecimal> draftQtyBySourceItem = loadDraftReturnQtyBySourceItem(tenantId, entry.getKey(), currentReturnId);
            for (ErpSaleReturnItem item : entry.getValue()) {
                ErpSaleOrderItem saleItem = saleItemById.get(item.getSourceSaleOrderItemId());
                if (saleItem == null) {
                    continue;
                }
                BigDecimal originalQty = zeroIfNull(saleItem.getQty());
                BigDecimal approvedQty = approvedQtyBySourceItem.getOrDefault(saleItem.getId(), BigDecimal.ZERO);
                BigDecimal draftQty = draftQtyBySourceItem.getOrDefault(saleItem.getId(), BigDecimal.ZERO);
                item.setSourceSaleOrderItemQty(originalQty);
                item.setSourceSaleOrderItemApprovedReturnedQty(approvedQty);
                item.setSourceSaleOrderItemDraftOccupiedQty(draftQty);
                item.setSourceSaleOrderItemRemainingQty(originalQty.subtract(approvedQty).subtract(draftQty).max(BigDecimal.ZERO));
            }
        }
    }

    private void lockSourceSaleOrderItems(Long tenantId, List<ErpSaleReturnItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Long> sourceItemIds = items.stream()
            .map(ErpSaleReturnItem::getSourceSaleOrderItemId)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .toList();
        if (!sourceItemIds.isEmpty()) {
            erpSaleOrderItemMapper.findByIdsForUpdate(tenantId, sourceItemIds);
        }
    }

    private void validateSourceItemLines(List<ReturnSourceLine> requestLines, ErpSaleOrderItem saleItem) {
        for (ReturnSourceLine line : requestLines) {
            if (line == null || !Objects.equals(line.sourceSaleOrderItemId(), saleItem.getId())) {
                continue;
            }
            if (!Objects.equals(line.productId(), saleItem.getProductId())) {
                throw new IllegalArgumentException("退货商品必须与来源销售明细一致");
            }
        }
    }

    private void validateNoDuplicateDestinationForSourceItem(List<ReturnSourceLine> requestLines, ErpSaleOrderItem saleItem) {
        Set<String> destinationKeys = new HashSet<>();
        for (ReturnSourceLine line : requestLines) {
            if (line == null || !Objects.equals(line.sourceSaleOrderItemId(), saleItem.getId())) {
                continue;
            }
            String key = String.valueOf(line.warehouseId()) + ":" + String.valueOf(line.locationId());
            if (!destinationKeys.add(key)) {
                String lineLabel = saleItem.getSortNo() == null ? String.valueOf(saleItem.getId()) : "第" + saleItem.getSortNo() + "行";
                throw new IllegalArgumentException("来源销售明细" + lineLabel + "存在相同仓库/库位的重复退货明细，请合并数量");
            }
        }
    }

    private BigDecimal calcLineUnitAmount(BigDecimal amount, BigDecimal qty) {
        BigDecimal safeQty = qty == null ? BigDecimal.ZERO : qty;
        if (safeQty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return (amount == null ? BigDecimal.ZERO : amount).divide(safeQty, 6, RoundingMode.HALF_UP);
    }

    private Map<Long, BigDecimal> allocateLegacyProductQtyToSaleItems(List<ErpSaleOrderItem> saleItems,
                                                                      Map<Long, BigDecimal> legacyQtyByProduct) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (saleItems == null || saleItems.isEmpty() || legacyQtyByProduct == null || legacyQtyByProduct.isEmpty()) {
            return result;
        }
        Map<Long, BigDecimal> remainingByProduct = new HashMap<>(legacyQtyByProduct);
        for (ErpSaleOrderItem item : saleItems) {
            if (item == null || item.getId() == null || item.getProductId() == null) {
                continue;
            }
            BigDecimal remaining = remainingByProduct.getOrDefault(item.getProductId(), BigDecimal.ZERO);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal qty = item.getQty() == null ? BigDecimal.ZERO : item.getQty();
            BigDecimal allocated = qty.min(remaining);
            if (allocated.compareTo(BigDecimal.ZERO) > 0) {
                result.put(item.getId(), allocated);
                remainingByProduct.put(item.getProductId(), remaining.subtract(allocated).max(BigDecimal.ZERO));
            }
        }
        return result;
    }

    private Map<Long, BigDecimal> aggregateRequestedQty(List<ErpSaleReturnItemRequest> requests) {
        Map<Long, BigDecimal> qtyByProduct = new HashMap<>();
        if (requests == null) {
            return qtyByProduct;
        }
        for (ErpSaleReturnItemRequest request : requests) {
            if (request == null || request.productId() == null || request.qty() == null) {
                continue;
            }
            qtyByProduct.merge(request.productId(), request.qty(), BigDecimal::add);
        }
        return qtyByProduct;
    }

    private Map<Long, BigDecimal> aggregateRequestedAmountInclTax(List<ErpSaleReturnItemRequest> requests) {
        Map<Long, BigDecimal> amountByProduct = new HashMap<>();
        if (requests == null) {
            return amountByProduct;
        }
        for (ErpSaleReturnItemRequest request : requests) {
            if (request == null || request.productId() == null || request.qty() == null) {
                continue;
            }
            BigDecimal taxRate = request.taxRate() == null ? BigDecimal.ZERO : request.taxRate();
            BigDecimal priceInclTax = request.priceInclTax();
            if (priceInclTax == null && request.price() != null) {
                priceInclTax = calcPriceInclTax(request.price(), taxRate);
            }
            if (priceInclTax == null) {
                priceInclTax = BigDecimal.ZERO;
            }
            amountByProduct.merge(request.productId(), priceInclTax.multiply(request.qty()), BigDecimal::add);
        }
        return amountByProduct;
    }

    private Map<Long, BigDecimal> aggregateExistingQty(List<ErpSaleReturnItem> items) {
        Map<Long, BigDecimal> qtyByProduct = new HashMap<>();
        if (items == null) {
            return qtyByProduct;
        }
        for (ErpSaleReturnItem item : items) {
            if (item == null || item.getProductId() == null || item.getQty() == null) {
                continue;
            }
            qtyByProduct.merge(item.getProductId(), item.getQty(), BigDecimal::add);
        }
        return qtyByProduct;
    }

    private Map<Long, BigDecimal> aggregateExistingAmountInclTax(List<ErpSaleReturnItem> items) {
        Map<Long, BigDecimal> amountByProduct = new HashMap<>();
        if (items == null) {
            return amountByProduct;
        }
        for (ErpSaleReturnItem item : items) {
            if (item == null || item.getProductId() == null) {
                continue;
            }
            amountByProduct.merge(
                item.getProductId(),
                item.getAmountInclTax() == null ? BigDecimal.ZERO : item.getAmountInclTax(),
                BigDecimal::add
            );
        }
        return amountByProduct;
    }

    private Map<Long, BigDecimal> loadApprovedReturnQtyByProduct(Long tenantId, Long saleOrderId, Long currentReturnId) {
        return loadApprovedReturnQtyByProduct(tenantId, saleOrderId, currentReturnId, false);
    }

    private Map<Long, BigDecimal> loadApprovedReturnQtyByProduct(Long tenantId,
                                                                 Long saleOrderId,
                                                                 Long currentReturnId,
                                                                 boolean onlyWithoutSourceItem) {
        List<ErpSaleReturn> approvedReturns = erpSaleReturnMapper.selectList(new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", tenantId)
            .eq("status", STATUS_APPROVED));
        Map<Long, BigDecimal> qtyByProduct = new HashMap<>();
        for (ErpSaleReturn approvedReturn : approvedReturns) {
            if (currentReturnId != null && currentReturnId.equals(approvedReturn.getId())) {
                continue;
            }
            List<ErpSaleReturnItem> returnItems = erpSaleReturnItemMapper.findByReturnId(tenantId, approvedReturn.getId());
            for (ErpSaleReturnItem returnItem : returnItems) {
                if (onlyWithoutSourceItem && returnItem.getSourceSaleOrderItemId() != null) {
                    continue;
                }
                if (returnItem.getProductId() == null || returnItem.getQty() == null) {
                    continue;
                }
                qtyByProduct.merge(returnItem.getProductId(), returnItem.getQty(), BigDecimal::add);
            }
        }
        return qtyByProduct;
    }

    private Map<Long, BigDecimal> loadApprovedReturnQtyBySourceItem(Long tenantId, Long saleOrderId, Long currentReturnId) {
        List<ErpSaleReturn> approvedReturns = erpSaleReturnMapper.selectList(new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", tenantId)
            .eq("status", STATUS_APPROVED));
        Map<Long, BigDecimal> qtyBySourceItem = new HashMap<>();
        for (ErpSaleReturn approvedReturn : approvedReturns) {
            if (currentReturnId != null && currentReturnId.equals(approvedReturn.getId())) {
                continue;
            }
            List<ErpSaleReturnItem> returnItems = erpSaleReturnItemMapper.findByReturnId(tenantId, approvedReturn.getId());
            for (ErpSaleReturnItem returnItem : returnItems) {
                if (returnItem.getSourceSaleOrderItemId() == null || returnItem.getQty() == null) {
                    continue;
                }
                qtyBySourceItem.merge(returnItem.getSourceSaleOrderItemId(), returnItem.getQty(), BigDecimal::add);
            }
        }
        return qtyBySourceItem;
    }

    private Map<Long, BigDecimal> loadDraftReturnQtyBySourceItem(Long tenantId, Long saleOrderId, Long currentReturnId) {
        List<ErpSaleReturn> draftReturns = erpSaleReturnMapper.selectList(new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", tenantId)
            .eq("sale_order_id", saleOrderId)
            .eq("status", STATUS_DRAFT)
            .ne(currentReturnId != null, "id", currentReturnId)
            .isNull("deleted_at"));
        Map<Long, BigDecimal> qtyBySourceItem = new HashMap<>();
        for (ErpSaleReturn draftReturn : draftReturns) {
            List<ErpSaleReturnItem> returnItems = erpSaleReturnItemMapper.findByReturnId(tenantId, draftReturn.getId());
            for (ErpSaleReturnItem returnItem : returnItems) {
                if (returnItem.getSourceSaleOrderItemId() == null || returnItem.getQty() == null) {
                    continue;
                }
                qtyBySourceItem.merge(returnItem.getSourceSaleOrderItemId(), returnItem.getQty(), BigDecimal::add);
            }
        }
        return qtyBySourceItem;
    }

    private Map<Long, BigDecimal> loadApprovedReturnAmountBySourceItem(Long tenantId, Long saleOrderId, Long currentReturnId) {
        List<ErpSaleReturn> approvedReturns = erpSaleReturnMapper.selectList(new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", tenantId)
            .eq("sale_order_id", saleOrderId)
            .eq("status", STATUS_APPROVED));
        Map<Long, BigDecimal> amountBySourceItem = new HashMap<>();
        for (ErpSaleReturn approvedReturn : approvedReturns) {
            if (currentReturnId != null && currentReturnId.equals(approvedReturn.getId())) {
                continue;
            }
            List<ErpSaleReturnItem> returnItems = erpSaleReturnItemMapper.findByReturnId(tenantId, approvedReturn.getId());
            for (ErpSaleReturnItem returnItem : returnItems) {
                if (returnItem.getSourceSaleOrderItemId() == null) {
                    continue;
                }
                amountBySourceItem.merge(
                    returnItem.getSourceSaleOrderItemId(),
                    returnItem.getAmountInclTax() == null ? BigDecimal.ZERO : returnItem.getAmountInclTax(),
                    BigDecimal::add
                );
            }
        }
        return amountBySourceItem;
    }

    private Map<Long, BigDecimal> loadApprovedReturnAmountByProduct(Long tenantId, Long saleOrderId, Long currentReturnId) {
        List<ErpSaleReturn> approvedReturns = erpSaleReturnMapper.selectList(new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", tenantId)
            .eq("sale_order_id", saleOrderId)
            .eq("status", STATUS_APPROVED));
        Map<Long, BigDecimal> amountByProduct = new HashMap<>();
        for (ErpSaleReturn approvedReturn : approvedReturns) {
            if (currentReturnId != null && currentReturnId.equals(approvedReturn.getId())) {
                continue;
            }
            List<ErpSaleReturnItem> returnItems = erpSaleReturnItemMapper.findByReturnId(tenantId, approvedReturn.getId());
            for (ErpSaleReturnItem returnItem : returnItems) {
                if (returnItem.getProductId() == null) {
                    continue;
                }
                amountByProduct.merge(
                    returnItem.getProductId(),
                    returnItem.getAmountInclTax() == null ? BigDecimal.ZERO : returnItem.getAmountInclTax(),
                    BigDecimal::add
                );
            }
        }
        return amountByProduct;
    }

    private void validateNoOtherDraftOccupancy(Long tenantId,
                                               Long saleOrderId,
                                               List<ErpSaleReturnItem> currentItems,
                                               Long currentReturnId) {
        if (currentItems == null || currentItems.isEmpty()) {
            return;
        }
        Map<Long, Map<Long, BigDecimal>> currentQtyBySaleOrderAndSourceItem = new LinkedHashMap<>();
        for (ErpSaleReturnItem item : currentItems) {
            if (item == null || item.getSourceSaleOrderItemId() == null || item.getQty() == null) {
                continue;
            }
            Long effectiveSaleOrderId = item.getSourceSaleOrderId() == null ? saleOrderId : item.getSourceSaleOrderId();
            if (effectiveSaleOrderId == null) {
                continue;
            }
            currentQtyBySaleOrderAndSourceItem
                .computeIfAbsent(effectiveSaleOrderId, ignored -> new LinkedHashMap<>())
                .merge(item.getSourceSaleOrderItemId(), item.getQty(), BigDecimal::add);
        }
        if (currentQtyBySaleOrderAndSourceItem.isEmpty()) {
            return;
        }

        Set<String> conflictOrderNos = new LinkedHashSet<>();
        for (Map.Entry<Long, Map<Long, BigDecimal>> entry : currentQtyBySaleOrderAndSourceItem.entrySet()) {
            Long effectiveSaleOrderId = entry.getKey();
            List<ErpSaleOrderItem> saleItems = erpSaleOrderItemMapper.findByOrderId(tenantId, effectiveSaleOrderId);
            Map<Long, ErpSaleOrderItem> saleItemById = saleItems.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(ErpSaleOrderItem::getId, item -> item, (left, right) -> left));
            Map<Long, BigDecimal> approvedQtyBySourceItem = loadApprovedReturnQtyBySourceItem(tenantId, effectiveSaleOrderId, currentReturnId);
            Map<Long, BigDecimal> otherDraftQtyBySourceItem = new HashMap<>();

            List<ErpSaleReturn> draftReturns = erpSaleReturnMapper.selectList(new QueryWrapper<ErpSaleReturn>()
                .eq("tenant_id", tenantId)
                .eq("sale_order_id", effectiveSaleOrderId)
                .eq("status", STATUS_DRAFT)
                .ne(currentReturnId != null, "id", currentReturnId)
                .orderByAsc("created_at")
                .orderByAsc("id"));
            for (ErpSaleReturn draftReturn : draftReturns) {
                if (draftReturn.getId() == null) {
                    continue;
                }
                List<ErpSaleReturnItem> draftItems = erpSaleReturnItemMapper.findByReturnId(tenantId, draftReturn.getId());
                for (ErpSaleReturnItem draftItem : draftItems) {
                    if (draftItem.getSourceSaleOrderItemId() == null || draftItem.getQty() == null) {
                        continue;
                    }
                    if (!entry.getValue().containsKey(draftItem.getSourceSaleOrderItemId())) {
                        continue;
                    }
                    otherDraftQtyBySourceItem.merge(draftItem.getSourceSaleOrderItemId(), draftItem.getQty(), BigDecimal::add);
                }
            }
            for (Map.Entry<Long, BigDecimal> sourceEntry : entry.getValue().entrySet()) {
                Long sourceItemId = sourceEntry.getKey();
                ErpSaleOrderItem saleItem = saleItemById.get(sourceItemId);
                if (saleItem == null) {
                    continue;
                }
                BigDecimal soldQty = zeroIfNull(saleItem.getQty());
                BigDecimal approvedQty = approvedQtyBySourceItem.getOrDefault(sourceItemId, BigDecimal.ZERO);
                BigDecimal otherDraftQty = otherDraftQtyBySourceItem.getOrDefault(sourceItemId, BigDecimal.ZERO);
                BigDecimal currentQty = sourceEntry.getValue();
                if (approvedQty.add(otherDraftQty).add(currentQty).compareTo(soldQty) > 0) {
                    String lineLabel = saleItem.getSortNo() == null ? String.valueOf(sourceItemId) : "第" + saleItem.getSortNo() + "行";
                    conflictOrderNos.add(lineLabel);
                }
            }
        }
        if (!conflictOrderNos.isEmpty()) {
            throw new IllegalArgumentException("来源销售明细可退数量不足，已被其他草稿或已审核退货占用：" + String.join("、", conflictOrderNos));
        }
    }

    private void enrichFlowStatus(Long tenantId, List<ErpSaleReturn> returns) {
        if (returns == null || returns.isEmpty()) {
            return;
        }
        enrichCustomerNames(tenantId, returns);
        List<Long> returnIds = returns.stream()
            .map(ErpSaleReturn::getId)
            .filter(Objects::nonNull)
            .toList();
        Map<Long, ErpSaleReturnRefundSnapshot> refundSnapshotMap = returnIds.isEmpty()
            ? Map.of()
            : erpSaleReturnMapper.findRefundSnapshotsByReturnIds(tenantId, returnIds).stream()
                .filter(snapshot -> snapshot != null && snapshot.returnId() != null)
                .collect(Collectors.toMap(ErpSaleReturnRefundSnapshot::returnId, snapshot -> snapshot, (left, right) -> left));
        for (ErpSaleReturn order : returns) {
            if (order == null || order.getId() == null) {
                continue;
            }
            ErpSaleReturnRefundSnapshot snapshot = refundSnapshotMap.get(order.getId());
            if (snapshot != null) {
                order.setRefundStatus(snapshot.refundStatus());
                order.setRefundUnpaidAmount(snapshot.refundUnpaidAmount());
            } else if (!STATUS_RED_FLUSHED.equals(order.getStatus())) {
                applyDraftRefundPreview(order);
            }
        }
    }

    private void enrichCustomerNames(Long tenantId, List<ErpSaleReturn> returns) {
        List<Long> customerIds = returns.stream()
            .map(ErpSaleReturn::getCustomerId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (customerIds.isEmpty()) {
            return;
        }
        Map<Long, String> customerNameById = erpCustomerMapper.selectList(new QueryWrapper<ErpCustomer>()
                .eq("tenant_id", tenantId)
                .in("id", customerIds))
            .stream()
            .filter(customer -> customer != null && customer.getId() != null)
            .collect(Collectors.toMap(
                ErpCustomer::getId,
                customer -> customer.getName() == null ? "" : customer.getName(),
                (left, right) -> left
            ));
        for (ErpSaleReturn order : returns) {
            if (order != null && order.getCustomerId() != null) {
                order.setCustomerName(customerNameById.get(order.getCustomerId()));
            }
        }
    }

    private void applyDraftRefundPreview(ErpSaleReturn order) {
        BigDecimal total = resolveReturnTotal(order);
        BigDecimal discount = zeroIfNull(order.getDiscountAmount());
        if (discount.compareTo(total) > 0) {
            discount = total;
        }
        BigDecimal refundedCash = zeroIfNull(order.getPaidAmount());
        BigDecimal maxRefundedCash = total.subtract(discount);
        if (maxRefundedCash.compareTo(BigDecimal.ZERO) < 0) {
            maxRefundedCash = BigDecimal.ZERO;
        }
        if (refundedCash.compareTo(maxRefundedCash) > 0) {
            refundedCash = maxRefundedCash;
        }
        BigDecimal applied = refundedCash.add(discount);
        if (applied.compareTo(total) > 0) {
            applied = total;
        }
        BigDecimal unpaid = total.subtract(applied).negate();
        order.setRefundStatus(unpaid.compareTo(BigDecimal.ZERO) == 0 ? STATUS_SETTLED : STATUS_OPEN);
        order.setRefundUnpaidAmount(unpaid);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void applyTotals(ErpSaleReturn order, List<ErpSaleReturnItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalExcl = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalIncl = BigDecimal.ZERO;
        for (ErpSaleReturnItem item : items) {
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

    private void validateSettlementAmounts(Long tenantId, ErpSaleReturn order, Long currentReturnId) {
        BigDecimal paidAmount = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        BigDecimal discountAmount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        if (paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("退款金额不能小于0");
        }
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("优惠金额不能小于0");
        }
        BigDecimal totalAmountInclTax = order.getTotalAmountInclTax() == null ? BigDecimal.ZERO : order.getTotalAmountInclTax();
        if (paidAmount.add(discountAmount).compareTo(totalAmountInclTax) > 0) {
            throw new IllegalArgumentException("退款金额与优惠金额之和不能大于退货总金额");
        }
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal refundableCash = buildSaleOrderRefundSummary(tenantId, order.getSaleOrderId(), currentReturnId)
                .getRefundableCash();
            if (paidAmount.compareTo(refundableCash) > 0) {
                throw new IllegalArgumentException("退款金额不能超过原销售可退实收金额");
            }
        }
    }

    private ErpSaleReturnRefundSummary buildSaleOrderRefundSummary(Long tenantId, Long saleOrderId, Long currentReturnId) {
        ErpSaleOrder saleOrder = saleOrderId == null ? null : loadApprovedSaleOrder(tenantId, saleOrderId);
        return buildSaleOrderRefundSummary(tenantId, saleOrder, currentReturnId);
    }

    private ErpSaleReturnRefundSummary buildSaleOrderRefundSummary(Long tenantId,
                                                                   ErpSaleOrder saleOrder,
                                                                   Long currentReturnId) {
        ErpSaleReturnRefundSummary summary = new ErpSaleReturnRefundSummary();
        if (saleOrder == null || saleOrder.getId() == null) {
            summary.setCollectedCash(BigDecimal.ZERO);
            summary.setRefundedCash(BigDecimal.ZERO);
            summary.setRefundableCash(BigDecimal.ZERO);
            return summary;
        }
        Long saleOrderId = saleOrder.getId();
        summary.setSaleOrderId(saleOrderId);
        summary.setSaleOrderNo(saleOrder.getOrderNo());
        summary.setDiscountAmount(saleOrder.getDiscountAmount() == null ? BigDecimal.ZERO : saleOrder.getDiscountAmount());
        ErpAccountsReceivable saleReceivable = erpAccountsReceivableMapper.findBySource(
            tenantId, SOURCE_SALE_ORDER, saleOrderId);
        if (saleReceivable == null) {
            saleReceivable = erpAccountsReceivableMapper.findBySaleOrderId(tenantId, saleOrderId);
        }
        BigDecimal collectedCash = BigDecimal.ZERO;
        Set<Long> countedReceiptIds = new HashSet<>();
        if (saleReceivable != null && saleReceivable.getId() != null) {
            List<ErpReceiptReceivable> allocations = erpReceiptReceivableMapper.findByReceivableId(
                tenantId, saleReceivable.getId());
            if (allocations != null && !allocations.isEmpty()) {
                List<Long> receiptIds = allocations.stream()
                    .map(ErpReceiptReceivable::getReceiptId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
                if (!receiptIds.isEmpty()) {
                    Map<Long, ErpReceipt> receiptMap = erpReceiptMapper.selectBatchIds(receiptIds).stream()
                        .filter(receipt -> receipt != null && tenantId.equals(receipt.getTenantId()))
                        .collect(java.util.stream.Collectors.toMap(ErpReceipt::getId, receipt -> receipt, (left, right) -> left));
                    for (ErpReceiptReceivable allocation : allocations) {
                        ErpReceipt receipt = receiptMap.get(allocation.getReceiptId());
                        if (receipt == null || !STATUS_APPROVED.equals(receipt.getStatus())) {
                            continue;
                        }
                        BigDecimal allocatedTotal = allocation.getAllocatedTotal() == null
                            ? BigDecimal.ZERO
                            : allocation.getAllocatedTotal();
                        if (allocatedTotal.compareTo(BigDecimal.ZERO) > 0) {
                            collectedCash = collectedCash.add(allocatedTotal);
                            countedReceiptIds.add(receipt.getId());
                        }
                    }
                }
            }
        }
        List<ErpReceipt> saleOrderReceipts = erpReceiptMapper.selectList(new QueryWrapper<ErpReceipt>()
            .eq("tenant_id", tenantId)
            .eq("sale_order_id", saleOrderId)
            .eq("status", STATUS_APPROVED));
        for (ErpReceipt receipt : saleOrderReceipts) {
            if (receipt == null || receipt.getId() == null || countedReceiptIds.contains(receipt.getId())) {
                continue;
            }
            BigDecimal receiptTotal = receiptImpactTotal(receipt);
            if (receiptTotal.compareTo(BigDecimal.ZERO) > 0) {
                collectedCash = collectedCash.add(receiptTotal);
            }
        }
        BigDecimal refundedCash = BigDecimal.ZERO;
        List<ErpSaleReturn> approvedReturns = erpSaleReturnMapper.selectList(new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", tenantId)
            .eq("sale_order_id", saleOrderId)
            .eq("status", STATUS_APPROVED));
        for (ErpSaleReturn approvedReturn : approvedReturns) {
            if (currentReturnId != null && currentReturnId.equals(approvedReturn.getId())) {
                continue;
            }
            BigDecimal paid = approvedReturn.getPaidAmount() == null ? BigDecimal.ZERO : approvedReturn.getPaidAmount();
            if (paid.compareTo(BigDecimal.ZERO) > 0) {
                refundedCash = refundedCash.add(paid);
            }
        }
        BigDecimal refundable = collectedCash.subtract(refundedCash);
        if (refundable.compareTo(BigDecimal.ZERO) < 0) {
            refundable = BigDecimal.ZERO;
        }
        summary.setCollectedCash(collectedCash);
        summary.setRefundedCash(refundedCash);
        summary.setRefundableCash(refundable);
        return summary;
    }

    private BigDecimal receiptImpactTotal(ErpReceipt receipt) {
        if (receipt == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = receipt.getAmount() == null ? BigDecimal.ZERO : receipt.getAmount();
        BigDecimal discount = receipt.getDiscountAmount() == null ? BigDecimal.ZERO : receipt.getDiscountAmount();
        return amount.add(discount);
    }

    private ErpSaleReturn loadForUpdate(Long tenantId, Long id) {
        ErpSaleReturn order = erpSaleReturnMapper.selectOne(new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (order == null) {
            throw new IllegalArgumentException("销售退货单不存在");
        }
        return order;
    }

    private void updateWithVersion(Long tenantId, ErpSaleReturn order) {
        Long version = order.getVersion() == null ? 0L : order.getVersion();
        order.setVersion(version + 1);
        int updated = erpSaleReturnMapper.update(order, new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", tenantId)
            .eq("id", order.getId())
            .eq("version", version));
        if (updated == 0) {
            throw new IllegalArgumentException("销售退货单已被修改，请刷新重试");
        }
    }

    private void applyStockDelta(Long tenantId,
                                 ErpSaleReturnItem item,
                                 BigDecimal delta,
                                 String bizType,
                                 Long orderId,
                                 Long sourceSaleOrderId,
                                 boolean updateBalance) {
        Long warehouseId = item.getWarehouseId();
        Long locationId = item.getLocationId();
        BigDecimal unitCost = getReturnUnitCost(tenantId, sourceSaleOrderId, item);
        if (updateBalance && delta.compareTo(BigDecimal.ZERO) > 0) {
            erpCostService.applyInboundAverageCost(tenantId, item.getProductId(), delta, unitCost);
        }
        ErpStockBalance balance = null;
        BigDecimal before;
        BigDecimal after;
        if (updateBalance) {
            String operator = resolveCurrentUsername();
            if (delta.compareTo(BigDecimal.ZERO) < 0) {
                balance = erpStockBalanceMapper.addQtyIfEnough(
                    tenantId, item.getProductId(), warehouseId, locationId, delta, operator);
            } else {
                balance = erpStockBalanceMapper.upsertAddQty(
                    tenantId, item.getProductId(), warehouseId, locationId, delta, operator);
            }
            if (balance == null) {
                ErpStockBalance currentBalance = erpStockBalanceMapper.findByKey(tenantId, item.getProductId(), warehouseId, locationId);
                BigDecimal currentQty = currentBalance == null ? BigDecimal.ZERO : currentBalance.getQtyOnHand();
                BigDecimal required = delta.abs();
                String productLabel = item.getProductName() == null ? item.getProductCode() : item.getProductName();
                throw new IllegalArgumentException(
                    "库存不足，商品[" + productLabel + "] 可用=" + currentQty + "，需求=" + required
                );
            }
            after = balance.getQtyOnHand() == null ? BigDecimal.ZERO : balance.getQtyOnHand();
            before = after.subtract(delta);
        } else {
            balance = erpStockBalanceMapper.findByKey(tenantId, item.getProductId(), warehouseId, locationId);
            before = balance == null ? BigDecimal.ZERO : balance.getQtyOnHand();
            after = before;
        }

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
        BigDecimal totalCost = unitCost.multiply(delta).setScale(4, RoundingMode.HALF_UP);
        txn.setUnitCost(unitCost);
        txn.setTotalCost(totalCost);
        txn.setOperator(resolveCurrentUsername());
        txn.setOperatorId(null);
        txn.setRemark(item.getRemark());
        txn.setCreatedAt(Instant.now());
        erpStockTxnMapper.insert(txn);
    }

    private BigDecimal getReturnUnitCost(Long tenantId, Long saleOrderId, ErpSaleReturnItem item) {
        Long productId = item == null ? null : item.getProductId();
        Long sourceSaleOrderItemId = item == null ? null : item.getSourceSaleOrderItemId();
        Long effectiveSaleOrderId = item != null && item.getSourceSaleOrderId() != null ? item.getSourceSaleOrderId() : saleOrderId;
        if (effectiveSaleOrderId != null && sourceSaleOrderItemId != null) {
            BigDecimal originalCost = erpStockTxnMapper.findSaleIssueUnitCostByItem(tenantId, effectiveSaleOrderId, sourceSaleOrderItemId);
            if (originalCost != null && originalCost.compareTo(BigDecimal.ZERO) > 0) {
                return originalCost;
            }
        }
        if (effectiveSaleOrderId != null && productId != null) {
            BigDecimal originalCost = erpStockTxnMapper.findSaleIssueUnitCost(tenantId, effectiveSaleOrderId, productId);
            if (originalCost != null && originalCost.compareTo(BigDecimal.ZERO) > 0) {
                return originalCost;
            }
        }
        return erpCostService.getProductCost(tenantId, productId);
    }

    private void reverseRestockCosts(Long tenantId, Long saleOrderId, List<ErpSaleReturnItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> qtyMap = new HashMap<>();
        Map<Long, BigDecimal> costMap = new HashMap<>();
        for (ErpSaleReturnItem item : items) {
            if (item == null || item.getProductId() == null || item.getQty() == null) {
                continue;
            }
            BigDecimal qty = item.getQty();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal unitCost = getReturnUnitCost(tenantId, saleOrderId, item);
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

    private void createReturnReceivable(Long tenantId, ErpSaleReturn order, BigDecimal delta, String operator) {
        if (order.getSaleOrderId() == null) {
            return;
        }
        BigDecimal amount = delta == null ? BigDecimal.ZERO : delta;
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        FinanceAutoFlowMode mode = tenantSettingService.getFinanceAutoFlowMode();
        BigDecimal negative = amount.negate();
        BigDecimal refundAmount = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        BigDecimal discountAmount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        BigDecimal applied = refundAmount.add(discountAmount);
        BigDecimal negativeApplied = applied.negate();
        boolean autoReceiptApproved = FinanceAutoFlowMode.AR_AP_WITH_APPROVED_PAYMENT == mode;
        BigDecimal receivablePaid = autoReceiptApproved ? negativeApplied : BigDecimal.ZERO;
        BigDecimal unpaid = negative.subtract(receivablePaid);
        ErpAccountsReceivable ar = new ErpAccountsReceivable();
        ar.setTenantId(tenantId);
        ar.setSaleOrderId(order.getSaleOrderId());
        ar.setOrderNo(generateReceivableNo(tenantId));
        ar.setCustomerId(order.getCustomerId());
        ar.setTotalAmount(negative);
        ar.setPaidAmount(receivablePaid);
        ar.setUnpaidAmount(unpaid);
        ar.setStatus(unpaid.compareTo(BigDecimal.ZERO) == 0 ? STATUS_SETTLED : STATUS_OPEN);
        ar.setSettlementMethod(order.getSettlementMethod());
        ar.setSourceType(SOURCE_SALE_RETURN);
        ar.setSourceId(order.getId());
        FinanceAutoFlowSupport.markReceivable(ar, FinanceAutoFlowSupport.SOURCE_SALE_RETURN, order.getId(), mode);
        ar.setRemark("销售退货单号:" + order.getOrderNo());
        ar.setCreatedAt(Instant.now());
        ar.setUpdatedAt(Instant.now());
        erpAccountsReceivableMapper.insert(ar);

        if (FinanceAutoFlowSupport.shouldGeneratePaymentDocument(mode)
            && applied.compareTo(BigDecimal.ZERO) > 0
            && REFUND_ACTION_REFUND.equals(order.getRefundAction())) {
            ErpReceipt receipt = new ErpReceipt();
            receipt.setTenantId(tenantId);
            receipt.setReceivableId(ar.getId());
            receipt.setSaleOrderId(order.getSaleOrderId());
            receipt.setReceiptNo(generateReceiptNo(tenantId));
            receipt.setCustomerId(order.getCustomerId());
            receipt.setAmount(refundAmount.negate());
            receipt.setDiscountAmount(discountAmount.negate());
            receipt.setSettlementMethod(order.getSettlementMethod());
            receipt.setReceiptMethodCode(order.getReceiptMethodCode());
            receipt.setStatus(FinanceAutoFlowSupport.paymentDocumentStatus(mode));
            receipt.setReceivedAt(autoReceiptApproved ? Instant.now() : null);
            FinanceAutoFlowSupport.markReceipt(receipt, FinanceAutoFlowSupport.SOURCE_SALE_RETURN, order.getId(), mode);
            receipt.setRemark("销售退货单审核自动退款/优惠:" + order.getOrderNo());
            receipt.setCreatedAt(Instant.now());
            receipt.setCreatedBy(operator);
            receipt.setUpdatedAt(Instant.now());
            receipt.setUpdatedBy(operator);
            erpReceiptMapper.insert(receipt);

            ErpReceiptReceivable allocation = new ErpReceiptReceivable();
            allocation.setTenantId(tenantId);
            allocation.setReceiptId(receipt.getId());
            allocation.setReceivableId(ar.getId());
            allocation.setAllocatedAmount(refundAmount.negate());
            allocation.setAllocatedDiscount(discountAmount.negate());
            allocation.setAllocatedTotal(negativeApplied);
            allocation.setCreatedAt(Instant.now());
            erpReceiptReceivableMapper.insert(allocation);
        }
    }

    private ErpAccountsReceivable findReturnReceivable(Long tenantId, ErpSaleReturn order) {
        if (order.getSaleOrderId() == null) {
            return null;
        }
        ErpAccountsReceivable sourceReceivable = erpAccountsReceivableMapper.findBySource(
            tenantId, SOURCE_SALE_RETURN, order.getId());
        if (sourceReceivable != null) {
            return sourceReceivable;
        }
        List<ErpAccountsReceivable> list = erpAccountsReceivableMapper.selectList(new QueryWrapper<ErpAccountsReceivable>()
            .eq("tenant_id", tenantId)
            .eq("sale_order_id", order.getSaleOrderId())
            .like("remark", "销售退货单号:" + order.getOrderNo())
            .lt("total_amount", BigDecimal.ZERO)
            .orderByDesc("id")
            .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    private boolean hasApprovedRefundReceipt(Long tenantId, Long receivableId) {
        List<ErpReceiptReceivable> allocations = erpReceiptReceivableMapper.findByReceivableId(tenantId, receivableId);
        if (allocations != null && !allocations.isEmpty()) {
            List<Long> receiptIds = allocations.stream()
                .map(ErpReceiptReceivable::getReceiptId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
            if (!receiptIds.isEmpty()) {
                List<ErpReceipt> receipts = erpReceiptMapper.selectBatchIds(receiptIds);
                if (receipts != null) {
                    java.util.Map<Long, ErpReceipt> receiptMap = receipts.stream()
                        .filter(receipt -> tenantId.equals(receipt.getTenantId()))
                        .collect(java.util.stream.Collectors.toMap(ErpReceipt::getId, receipt -> receipt, (left, right) -> left));
                    for (ErpReceiptReceivable allocation : allocations) {
                        ErpReceipt receipt = receiptMap.get(allocation.getReceiptId());
                        if (receipt == null || !STATUS_APPROVED.equals(receipt.getStatus())) {
                            continue;
                        }
                        BigDecimal allocatedTotal = allocation.getAllocatedTotal() == null ? BigDecimal.ZERO : allocation.getAllocatedTotal();
                        if (allocatedTotal.compareTo(BigDecimal.ZERO) < 0) {
                            return true;
                        }
                    }
                }
            }
        }
        List<ErpReceipt> receipts = erpReceiptMapper.selectList(new QueryWrapper<ErpReceipt>()
            .eq("tenant_id", tenantId)
            .eq("receivable_id", receivableId)
            .eq("status", STATUS_APPROVED));
        for (ErpReceipt receipt : receipts) {
            BigDecimal total = (receipt.getAmount() == null ? BigDecimal.ZERO : receipt.getAmount())
                .add(receipt.getDiscountAmount() == null ? BigDecimal.ZERO : receipt.getDiscountAmount());
            if (total.compareTo(BigDecimal.ZERO) < 0) {
                return true;
            }
        }
        return false;
    }

    private void redFlushReturnReceivable(Long tenantId, ErpSaleReturn order, String reason) {
        ErpAccountsReceivable ar = findReturnReceivable(tenantId, order);
        if (ar == null) {
            return;
        }
        ar.setTotalAmount(BigDecimal.ZERO);
        ar.setPaidAmount(BigDecimal.ZERO);
        ar.setUnpaidAmount(BigDecimal.ZERO);
        ar.setStatus(STATUS_RED_FLUSHED);
        ar.setRedFlushSourceType(SOURCE_SALE_RETURN);
        ar.setRedFlushSourceId(order.getId());
        if (reason != null && !reason.isBlank()) {
            String remark = ar.getRemark();
            ar.setRemark((remark == null || remark.isBlank())
                ? "红冲原因:" + reason
                : remark + " | 红冲原因:" + reason);
        }
        ar.setUpdatedAt(Instant.now());
        erpAccountsReceivableMapper.updateById(ar);
    }

    private BigDecimal resolveReturnTotal(ErpSaleReturn order) {
        if (order.getTotalAmountInclTax() != null) {
            return order.getTotalAmountInclTax();
        }
        return order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
    }

    private String resolveReturnType(String type) {
        if (type == null || type.isBlank()) {
            return RETURN_RESTOCK;
        }
        String trimmed = type.trim().toUpperCase();
        if (RETURN_SCRAP.equals(trimmed)) {
            return RETURN_SCRAP;
        }
        return RETURN_RESTOCK;
    }

    private String ensureOrderNo(Long tenantId, String provided) {
        String trimmed = provided == null ? "" : provided.trim();
        if (!trimmed.isEmpty()) {
            ErpSaleReturn existing = erpSaleReturnMapper.findByOrderNo(tenantId, trimmed);
            if (existing != null) {
                throw new IllegalArgumentException("销售退货单号已存在");
            }
            return trimmed;
        }
        String prefix = readConfig("erp.order.no.sale-return.prefix", "SR");
        String dateFormat = readConfig("erp.order.no.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.order.no.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, ORDER_TYPE, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, ORDER_TYPE, dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private String generateReceivableNo(Long tenantId) {
        String prefix = readConfig("erp.order.no.ar-return.prefix", "AR");
        String dateFormat = readConfig("erp.order.no.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.order.no.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, RECEIVABLE_ORDER_TYPE, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, RECEIVABLE_ORDER_TYPE, dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
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

    private Instant parseOrderAt(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        String trimmed = value.trim();
        try {
            if (trimmed.matches("\\d+")) {
                long epoch = Long.parseLong(trimmed);
                return Instant.ofEpochMilli(epoch);
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
        if (price == null) {
            return BigDecimal.ZERO;
        }
        if (taxRate == null) {
            return price;
        }
        return price.multiply(taxRate.add(BigDecimal.ONE)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcPriceExclTax(BigDecimal priceInclTax, BigDecimal taxRate) {
        if (priceInclTax == null) {
            return BigDecimal.ZERO;
        }
        if (taxRate == null || taxRate.compareTo(BigDecimal.ZERO) == 0) {
            return priceInclTax;
        }
        return priceInclTax.divide(taxRate.add(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }

    private String generateTxnNo() {
        return "TXN-" + UUID.randomUUID();
    }

    private String readConfig(String key, String fallback) {
        SystemConfig config = systemConfigMapper.findByKey(TenantContext.requireTenantId(), key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return fallback;
        }
        return config.getConfigValue().trim();
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int readIntConfig(String key, int fallback) {
        String value = readConfig(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return fallback;
        }
    }
}
