package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSaleOrderCreateRequest;
import com.example.wms.dto.erp.ErpSaleOrderDetail;
import com.example.wms.dto.erp.ErpSaleOrderHistoryItem;
import com.example.wms.dto.erp.ErpIdAmountPair;
import com.example.wms.dto.erp.ErpSaleOrderRecentItem;
import com.example.wms.dto.erp.ErpSaleOrderItemRequest;
import com.example.wms.dto.erp.ErpSaleOrderItemCostSnapshot;
import com.example.wms.dto.erp.ErpSaleOrderSummary;
import com.example.wms.dto.erp.ErpSaleOrderUpdateRequest;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.*;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpProductPriceMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpReceiptMethodMapper;
import com.example.wms.mapper.erp.ErpReceiptReceivableMapper;
import com.example.wms.mapper.erp.ErpSaleOrderItemMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.ErpSaleOrderService;
import com.example.wms.service.erp.support.ErpCostService;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// 销售单服务实现（ERP进销存）
@Service
public class ErpSaleOrderServiceImpl implements ErpSaleOrderService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_RED_FLUSHED = "RED_FLUSHED";
    private static final String STATUS_SETTLED = "SETTLED";
    private static final String STATUS_OPEN = "OPEN";
    private static final String ORDER_TYPE = "SALE";
    private static final String DEFAULT_SETTLEMENT_METHOD = "CASH";
    private static final String RECEIPT_ORDER_TYPE = "RECEIPT";
    private static final String AUTO_RECEIVABLE_REMARK = "销售单审核自动生成";
    private static final String AUTO_RECEIPT_REMARK = "销售单审核自动收款";
    private static final String SOURCE_SALE_ORDER = "SALE_ORDER";

    private final ErpSaleOrderMapper erpSaleOrderMapper;
    private final ErpSaleOrderItemMapper erpSaleOrderItemMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpProductPriceMapper erpProductPriceMapper;
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
    private final ErpSaleReturnMapper erpSaleReturnMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ErpCostService erpCostService;

    public ErpSaleOrderServiceImpl(ErpSaleOrderMapper erpSaleOrderMapper,
                                   ErpSaleOrderItemMapper erpSaleOrderItemMapper,
                                   ErpProductMapper erpProductMapper,
                                   ErpProductPriceMapper erpProductPriceMapper,
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
                                    ErpSaleReturnMapper erpSaleReturnMapper,
                                    SystemConfigMapper systemConfigMapper,
                                    ErpCostService erpCostService) {
        this.erpSaleOrderMapper = erpSaleOrderMapper;
        this.erpSaleOrderItemMapper = erpSaleOrderItemMapper;
        this.erpProductMapper = erpProductMapper;
        this.erpProductPriceMapper = erpProductPriceMapper;
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
        this.erpSaleReturnMapper = erpSaleReturnMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.erpCostService = erpCostService;
    }

    @Override
    public List<ErpSaleOrder> listAll(String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpSaleOrder> wrapper = baseWrapper(keyword, status, customerId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        List<ErpSaleOrder> orders = erpSaleOrderMapper.selectList(wrapper);
        enrichFlowStatus(tenantId(), orders);
        return orders;
    }

    @Override
    public PageResponse<ErpSaleOrder> page(long page, long size, String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        Page<ErpSaleOrder> pageReq = Page.of(page, size);
        QueryWrapper<ErpSaleOrder> wrapper = baseWrapper(keyword, status, customerId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        Page<ErpSaleOrder> result = erpSaleOrderMapper.selectPage(pageReq, wrapper);
        enrichFlowStatus(tenantId(), result.getRecords());
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public PageResponse<ErpSaleOrder> draftPage(long page, long size, String keyword, Long customerId, Instant startAt, Instant endAt) {
        return page(page, size, keyword, STATUS_DRAFT, customerId, startAt, endAt);
    }

    @Override
    public PageResponse<ErpSaleOrder> approvedPage(long page, long size, String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        String normalizedStatus = normalizeApprovedStatusFilter(status);
        return page(page, size, keyword, normalizedStatus, customerId, startAt, endAt);
    }

    @Override
    public ErpSaleOrderSummary summary(String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        Long tenantId = tenantId();
        QueryWrapper<ErpSaleOrder> wrapper = baseWrapper(keyword, status, customerId, startAt, endAt);
        wrapper.select("id", "status", "total_amount_incl_tax");
        List<ErpSaleOrder> orders = erpSaleOrderMapper.selectList(wrapper);
        if (orders == null || orders.isEmpty()) {
            return new ErpSaleOrderSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        List<Long> saleOrderIds = orders.stream()
            .map(ErpSaleOrder::getId)
            .filter(java.util.Objects::nonNull)
            .toList();
        Map<Long, BigDecimal> returnAmountByOrderId = toAmountMap(
            erpSaleReturnMapper.sumApprovedAmountsBySaleOrderIds(tenantId, saleOrderIds)
        );
        Map<Long, BigDecimal> estimatedDraftCostByOrderId = estimateDraftCostsByOrderId(tenantId, saleOrderIds);
        Map<Long, BigDecimal> saleCostByOrderId = toAmountMap(
            erpStockTxnMapper.sumSaleIssueCostsBySaleOrderIds(tenantId, saleOrderIds)
        );
        Map<Long, BigDecimal> returnCostByOrderId = toAmountMap(
            erpStockTxnMapper.sumApprovedSaleReturnCostsBySaleOrderIds(tenantId, saleOrderIds)
        );

        BigDecimal saleAmountTotal = BigDecimal.ZERO;
        BigDecimal returnAmountTotal = BigDecimal.ZERO;
        BigDecimal netGrossProfitTotal = BigDecimal.ZERO;
        for (ErpSaleOrder order : orders) {
            if (order == null || order.getId() == null) {
                continue;
            }
            BigDecimal saleAmount = zeroIfNull(order.getTotalAmountInclTax());
            BigDecimal returnAmount = zeroIfNull(returnAmountByOrderId.get(order.getId()));
            BigDecimal saleCost = STATUS_DRAFT.equals(order.getStatus())
                ? zeroIfNull(estimatedDraftCostByOrderId.get(order.getId()))
                : zeroIfNull(saleCostByOrderId.get(order.getId()));
            BigDecimal returnCost = zeroIfNull(returnCostByOrderId.get(order.getId()));
            BigDecimal netSaleAmount = saleAmount.subtract(returnAmount);
            BigDecimal netCost = saleCost.subtract(returnCost);

            saleAmountTotal = saleAmountTotal.add(saleAmount);
            returnAmountTotal = returnAmountTotal.add(returnAmount);
            netGrossProfitTotal = netGrossProfitTotal.add(netSaleAmount.subtract(netCost));
        }

        BigDecimal netSaleAmountTotal = saleAmountTotal.subtract(returnAmountTotal);
        return new ErpSaleOrderSummary(
            saleAmountTotal,
            returnAmountTotal,
            netSaleAmountTotal,
            netGrossProfitTotal
        );
    }

    @Override
    public ErpSaleOrderSummary draftSummary(String keyword, Long customerId, Instant startAt, Instant endAt) {
        return summary(keyword, STATUS_DRAFT, customerId, startAt, endAt);
    }

    @Override
    public ErpSaleOrderSummary approvedSummary(String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        return summary(keyword, normalizeApprovedStatusFilter(status), customerId, startAt, endAt);
    }

    @Override
    public ErpSaleOrderDetail getDetail(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSaleOrder order = erpSaleOrderMapper.selectOne(new QueryWrapper<ErpSaleOrder>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (order == null) {
            throw new IllegalArgumentException("销售单不存在");
        }
        List<ErpSaleOrderItem> items = erpSaleOrderItemMapper.findByOrderId(tenantId, id);
        enrichItemCostSnapshots(tenantId, id, items);
        enrichFlowStatus(tenantId, List.of(order));
        BigDecimal customerDebtTotal = order.getCustomerId() == null
            ? BigDecimal.ZERO
            : erpAccountsReceivableMapper.sumCustomerDebt(tenantId, order.getCustomerId());
        return new ErpSaleOrderDetail(order, items, customerDebtTotal == null ? BigDecimal.ZERO : customerDebtTotal);
    }

    @Override
    public ErpSaleOrderDetail getDraftDetail(Long id) {
        ErpSaleOrderDetail detail = getDetail(id);
        if (!STATUS_DRAFT.equals(detail.order().getStatus())) {
            throw new IllegalArgumentException("草稿接口不能访问已审核销售单");
        }
        return detail;
    }

    @Override
    public ErpSaleOrderDetail getApprovedDetail(Long id) {
        ErpSaleOrderDetail detail = getDetail(id);
        if (STATUS_DRAFT.equals(detail.order().getStatus())) {
            throw new IllegalArgumentException("已审核接口不能访问草稿销售单");
        }
        return detail;
    }

    @Override
    public String nextOrderNo() {
        Long tenantId = TenantContext.requireTenantId();
        return ensureOrderNo(tenantId, null, ORDER_TYPE, "SO");
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_SALE_CREATE", entityType = "erp_sale_order", entityId = "{result.order.id}", detail = "orderNo={result.order.orderNo}")
    public ErpSaleOrderDetail create(ErpSaleOrderCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String operator = resolveCurrentUsername();
        String orderNo = ensureOrderNo(tenantId, request.orderNo(), ORDER_TYPE, "SO");
        ErpSaleOrder order = new ErpSaleOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(orderNo);
        order.setStatus(STATUS_DRAFT);
        order.setCustomerId(request.customerId());
        Instant orderAt = parseOrderAt(request.orderAt());
        order.setOrderAt(orderAt == null ? Instant.now() : orderAt);
        order.setSettlementMethod(normalizeSettlementMethod(request.settlementMethod(), DEFAULT_SETTLEMENT_METHOD));
        order.setReceiptMethodCode(normalizeCode(request.receiptMethodCode()));
        order.setDeliveryMethod(normalizeCode(request.deliveryMethod()));
        order.setPaidAmount(normalizeAmount(request.paidAmount()));
        normalizeCreditSettlementFields(tenantId, order);
        validateHeaderMasterData(tenantId, order.getCustomerId(), order.getSettlementMethod(), order.getReceiptMethodCode(), order.getPaidAmount());
        order.setDiscountAmount(normalizeAmount(request.discountAmount()));
        order.setVersion(0L);
        order.setInventoryReserved(false);
        order.setRemark(request.remark());
        order.setCreatedAt(Instant.now());
        order.setCreatedBy(operator);
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(operator);

        List<ErpSaleOrderItem> items = buildItems(tenantId, null, order.getCustomerId(), request.items(), Set.of());
        applyTotals(order, items);
        validateSettlementAmounts(order);

        erpSaleOrderMapper.insert(order);
        for (ErpSaleOrderItem item : items) {
            item.setOrderId(order.getId());
            erpSaleOrderItemMapper.insert(item);
        }
        reserveDraftStock(tenantId, items);
        order.setInventoryReserved(true);
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(operator);
        updateWithVersion(tenantId, order);
        return new ErpSaleOrderDetail(order, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_SALE_UPDATE", entityType = "erp_sale_order", entityId = "{arg0}", detail = "orderNo={arg1.orderNo}")
    public ErpSaleOrderDetail update(Long id, ErpSaleOrderUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSaleOrder order = erpSaleOrderMapper.selectOne(new QueryWrapper<ErpSaleOrder>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (order == null) {
            throw new IllegalArgumentException("销售单不存在");
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可编辑");
        }
        String newOrderNo = resolveOrderNoForUpdate(request.orderNo(), order.getOrderNo(), tenantId, order.getId());
        order.setOrderNo(newOrderNo);
        order.setCustomerId(request.customerId());
        Instant orderAt = parseOrderAt(request.orderAt());
        order.setOrderAt(orderAt == null ? order.getOrderAt() : orderAt);
        order.setSettlementMethod(normalizeSettlementMethod(request.settlementMethod(),
            order.getSettlementMethod() == null ? DEFAULT_SETTLEMENT_METHOD : order.getSettlementMethod()));
        order.setReceiptMethodCode(normalizeCode(request.receiptMethodCode()));
        order.setDeliveryMethod(normalizeCode(request.deliveryMethod()));
        order.setPaidAmount(normalizeAmount(request.paidAmount()));
        normalizeCreditSettlementFields(tenantId, order);
        validateHeaderMasterData(tenantId, order.getCustomerId(), order.getSettlementMethod(), order.getReceiptMethodCode(), order.getPaidAmount());
        order.setDiscountAmount(normalizeAmount(request.discountAmount()));
        order.setRemark(request.remark());
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(resolveCurrentUsername());

        List<ErpSaleOrderItem> existingItems = erpSaleOrderItemMapper.findByOrderId(tenantId, id);
        Set<Long> allowedDisabledProductIds = existingProductIds(existingItems);
        if (Boolean.TRUE.equals(order.getInventoryReserved())) {
            releaseDraftStock(tenantId, existingItems);
            order.setInventoryReserved(false);
        }

        erpSaleOrderItemMapper.delete(new QueryWrapper<ErpSaleOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("order_id", id));

        List<ErpSaleOrderItem> items = buildItems(tenantId, id, order.getCustomerId(), request.items(), allowedDisabledProductIds);
        for (ErpSaleOrderItem item : items) {
            erpSaleOrderItemMapper.insert(item);
        }
        applyTotals(order, items);
        validateSettlementAmounts(order);
        reserveDraftStock(tenantId, items);
        order.setInventoryReserved(true);
        updateWithVersion(tenantId, order);
        return new ErpSaleOrderDetail(order, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_SALE_DELETE", entityType = "erp_sale_order", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSaleOrder order = erpSaleOrderMapper.selectOne(new QueryWrapper<ErpSaleOrder>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (order == null) {
            throw new IllegalArgumentException("销售单不存在");
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可删除");
        }
        List<ErpSaleOrderItem> items = erpSaleOrderItemMapper.findByOrderId(tenantId, id);
        if (Boolean.TRUE.equals(order.getInventoryReserved())) {
            releaseDraftStock(tenantId, items);
        }
        erpSaleOrderItemMapper.delete(new QueryWrapper<ErpSaleOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("order_id", id));
        erpSaleOrderMapper.deleteById(id);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_SALE_APPROVE", entityType = "erp_sale_order", entityId = "{arg0}")
    public void approve(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSaleOrder order = loadForUpdate(tenantId, id);
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可审核");
        }
        List<ErpSaleOrderItem> items = erpSaleOrderItemMapper.findByOrderId(tenantId, id);
        validateSettlementAmounts(order);
        boolean inventoryReserved = Boolean.TRUE.equals(order.getInventoryReserved());
        String operator = resolveCurrentUsername();
        ErpSaleOrder approved = erpSaleOrderMapper.approveDraft(tenantId, id, operator);
        if (approved == null) {
            throw new IllegalArgumentException("销售单状态已变化，请刷新重试");
        }
        for (ErpSaleOrderItem item : items) {
            applyStockDelta(tenantId, item, item.getQty().negate(), "SALE_APPROVE", id, inventoryReserved);
        }
        approved.setInventoryReserved(false);
        approved.setUpdatedAt(Instant.now());
        approved.setUpdatedBy(operator);
        erpSaleOrderMapper.updateById(approved);
        ensureReceivableAndReceipt(tenantId, order, operator);
    }

    @Transactional
    @AuditLog(action = "ERP_SALE_CANCEL", entityType = "erp_sale_order", entityId = "{arg0}")
    public void cancel(Long id) {
        cancel(id, null);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_SALE_CANCEL", entityType = "erp_sale_order", entityId = "{arg0}")
    public void cancel(Long id, String reason) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSaleOrder order = loadForUpdate(tenantId, id);
        if (STATUS_APPROVED.equals(order.getStatus())) {
            if (reason == null || reason.trim().isEmpty()) {
                throw new IllegalArgumentException("作废原因不能为空");
            }
            if (hasApprovedReceiptImpact(tenantId, order.getId())) {
                throw new IllegalArgumentException("请先红冲收款单");
            }
            if (hasApprovedSaleReturn(tenantId, order.getId())) {
                throw new IllegalArgumentException("请先红冲销售退货单");
            }
            List<ErpSaleOrderItem> items = erpSaleOrderItemMapper.findByOrderId(tenantId, id);
            for (ErpSaleOrderItem item : items) {
                applyStockDelta(tenantId, item, item.getQty(), "SALE_CANCEL", id, false);
            }
            ErpAccountsReceivable receivable = erpAccountsReceivableMapper.findBySaleOrderId(tenantId, order.getId());
            if (receivable != null) {
                receivable.setTotalAmount(BigDecimal.ZERO);
                receivable.setPaidAmount(BigDecimal.ZERO);
                receivable.setUnpaidAmount(BigDecimal.ZERO);
                receivable.setStatus("CANCELLED");
                receivable.setRemark(appendRedFlushReason(receivable.getRemark(), reason));
                receivable.setRedFlushSourceType(SOURCE_SALE_ORDER);
                receivable.setRedFlushSourceId(order.getId());
                receivable.setUpdatedAt(Instant.now());
                erpAccountsReceivableMapper.updateById(receivable);
            }
            order.setRemark(appendRedFlushReason(order.getRemark(), reason));
        } else if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可取消");
        }
        if (Boolean.TRUE.equals(order.getInventoryReserved())) {
            List<ErpSaleOrderItem> items = erpSaleOrderItemMapper.findByOrderId(tenantId, id);
            releaseDraftStock(tenantId, items);
            order.setInventoryReserved(false);
        }
        String operator = resolveCurrentUsername();
        order.setStatus(STATUS_CANCELLED);
        order.setCancelledBy(operator);
        order.setCancelledAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(operator);
        updateWithVersion(tenantId, order);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_SALE_RED_FLUSH", entityType = "erp_sale_order", entityId = "{arg0}")
    public void redFlush(Long id, String reason) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSaleOrder order = loadForUpdate(tenantId, id);
        if (!STATUS_APPROVED.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅已审核状态可红冲");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("红冲原因不能为空");
        }
        if (hasApprovedReceiptImpact(tenantId, order.getId())) {
            throw new IllegalArgumentException("请先红冲收款单");
        }
        if (hasApprovedSaleReturn(tenantId, order.getId())) {
            throw new IllegalArgumentException("请先红冲销售退货单");
        }
        String redFlushRemark = appendRedFlushReason(order.getRemark(), reason);
        String operator = resolveCurrentUsername();
        ErpSaleOrder redFlushed = erpSaleOrderMapper.redFlushApproved(tenantId, id, redFlushRemark, operator);
        if (redFlushed == null) {
            throw new IllegalArgumentException("销售单状态已变化，请刷新重试");
        }
        List<ErpSaleOrderItem> items = erpSaleOrderItemMapper.findByOrderId(tenantId, id);
        for (ErpSaleOrderItem item : items) {
            applyStockDelta(tenantId, item, item.getQty(), "SALE_RED_FLUSH", id, false);
        }
        ErpAccountsReceivable receivable = erpAccountsReceivableMapper.findBySaleOrderId(tenantId, order.getId());
        if (receivable != null) {
            receivable.setTotalAmount(BigDecimal.ZERO);
            receivable.setPaidAmount(BigDecimal.ZERO);
            receivable.setUnpaidAmount(BigDecimal.ZERO);
            receivable.setStatus("RED_FLUSHED");
            receivable.setRemark(appendRedFlushReason(receivable.getRemark(), reason));
            receivable.setRedFlushSourceType(SOURCE_SALE_ORDER);
            receivable.setRedFlushSourceId(order.getId());
            receivable.setUpdatedAt(Instant.now());
            erpAccountsReceivableMapper.updateById(receivable);
        }
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_SALE_COPY_APPROVED", entityType = "erp_sale_order", entityId = "{arg0}", detail = "newOrderNo={result.order.orderNo}")
    public ErpSaleOrderDetail copyApprovedToDraft(Long id) {
        ErpSaleOrderDetail source = getApprovedDetail(id);
        ErpSaleOrder order = source.order();
        ErpSaleOrderCreateRequest request = new ErpSaleOrderCreateRequest(
            nextOrderNo(),
            order.getOrderAt() == null ? null : order.getOrderAt().toString(),
            order.getCustomerId(),
            order.getSettlementMethod(),
            order.getReceiptMethodCode(),
            order.getDeliveryMethod(),
            order.getPaidAmount(),
            order.getDiscountAmount(),
            source.items().stream()
                .map(item -> new ErpSaleOrderItemRequest(
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
                .toList(),
            order.getRemark()
        );
        return create(request);
    }

    private Long tenantId() {
        return TenantContext.requireTenantId();
    }

    private void enrichFlowStatus(Long tenantId, List<ErpSaleOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Long> orderIds = orders.stream()
            .map(ErpSaleOrder::getId)
            .filter(java.util.Objects::nonNull)
            .toList();
        Map<Long, BigDecimal> estimatedDraftCostByOrderId = estimateDraftCostsByOrderId(tenantId, orderIds);
        for (ErpSaleOrder order : orders) {
            if (order == null || order.getId() == null) {
                continue;
            }
            ErpAccountsReceivable receivable = erpAccountsReceivableMapper.findBySaleOrderId(tenantId, order.getId());
            if (receivable != null) {
                order.setReceivableStatus(receivable.getStatus());
                order.setReceivableUnpaidAmount(receivable.getUnpaidAmount());
            } else if (!STATUS_RED_FLUSHED.equals(order.getStatus())) {
                applyDraftReceivablePreview(order);
            }
            Long approvedReturnCount = erpSaleReturnMapper.countApprovedBySaleOrderId(tenantId, order.getId());
            order.setApprovedReturnCount(approvedReturnCount);
            BigDecimal returnAmount = zeroIfNull(erpSaleReturnMapper.sumApprovedAmountBySaleOrderId(tenantId, order.getId()));
            BigDecimal saleCost = STATUS_DRAFT.equals(order.getStatus())
                ? zeroIfNull(estimatedDraftCostByOrderId.get(order.getId()))
                : zeroIfNull(erpStockTxnMapper.sumSaleIssueCost(tenantId, order.getId()));
            BigDecimal returnCost = zeroIfNull(erpStockTxnMapper.sumApprovedSaleReturnCost(tenantId, order.getId()));
            BigDecimal saleAmount = zeroIfNull(order.getTotalAmountInclTax());
            BigDecimal netSaleAmount = saleAmount.subtract(returnAmount);
            BigDecimal netCost = saleCost.subtract(returnCost);
            order.setCumulativeReturnAmount(returnAmount);
            order.setCumulativeReturnCost(returnCost);
            order.setNetSaleAmount(netSaleAmount);
            order.setNetGrossProfit(netSaleAmount.subtract(netCost));
            order.setRedFlushTrace(resolveRedFlushTrace(order, approvedReturnCount));
        }
    }

    private void applyDraftReceivablePreview(ErpSaleOrder order) {
        BigDecimal total = zeroIfNull(order.getTotalAmountInclTax());
        BigDecimal discount = zeroIfNull(order.getDiscountAmount());
        if (discount.compareTo(total) > 0) {
            discount = total;
        }
        BigDecimal paidCash = zeroIfNull(order.getPaidAmount());
        BigDecimal maxPaid = total.subtract(discount);
        if (maxPaid.compareTo(BigDecimal.ZERO) < 0) {
            maxPaid = BigDecimal.ZERO;
        }
        if (paidCash.compareTo(maxPaid) > 0) {
            paidCash = maxPaid;
        }
        BigDecimal totalApplied = paidCash.add(discount);
        if (totalApplied.compareTo(total) > 0) {
            totalApplied = total;
        }
        BigDecimal unpaid = total.subtract(totalApplied);
        order.setReceivableStatus(unpaid.compareTo(BigDecimal.ZERO) == 0 ? STATUS_SETTLED : STATUS_OPEN);
        order.setReceivableUnpaidAmount(unpaid);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Map<Long, BigDecimal> toAmountMap(List<ErpIdAmountPair> rows) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (rows == null || rows.isEmpty()) {
            return result;
        }
        for (ErpIdAmountPair row : rows) {
            if (row == null || row.getId() == null) {
                continue;
            }
            result.put(row.getId(), zeroIfNull(row.getAmount()));
        }
        return result;
    }

    private Map<Long, BigDecimal> estimateDraftCostsByOrderId(Long tenantId, List<Long> orderIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (orderIds == null || orderIds.isEmpty()) {
            return result;
        }
        List<ErpSaleOrderItem> items = erpSaleOrderItemMapper.findByOrderIds(tenantId, orderIds);
        if (items == null || items.isEmpty()) {
            return result;
        }
        List<Long> productIds = items.stream()
            .map(ErpSaleOrderItem::getProductId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, BigDecimal> productCostById = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<ErpProduct> products = erpProductMapper.selectList(new QueryWrapper<ErpProduct>()
                .eq("tenant_id", tenantId)
                .in("id", productIds));
            for (ErpProduct product : products) {
                if (product == null || product.getId() == null) {
                    continue;
                }
                productCostById.put(product.getId(), zeroIfNull(product.getCostPrice()));
            }
        }
        for (ErpSaleOrderItem item : items) {
            if (item == null || item.getOrderId() == null) {
                continue;
            }
            BigDecimal qty = zeroIfNull(item.getQty());
            BigDecimal costPrice = zeroIfNull(productCostById.get(item.getProductId()));
            BigDecimal lineEstimatedCost = costPrice.multiply(qty);
            result.merge(item.getOrderId(), lineEstimatedCost, BigDecimal::add);
        }
        return result;
    }

    private String resolveRedFlushTrace(ErpSaleOrder order, Long approvedReturnCount) {
        if (order == null) {
            return null;
        }
        if (order.getRedFlushSourceType() != null && order.getRedFlushSourceId() != null) {
            return order.getRedFlushSourceType() + "#" + order.getRedFlushSourceId();
        }
        if (STATUS_RED_FLUSHED.equals(order.getStatus())) {
            return "SALE_ORDER#" + order.getId();
        }
        if (approvedReturnCount != null && approvedReturnCount > 0) {
            return "HAS_RETURN#" + approvedReturnCount;
        }
        return null;
    }

    @Override
    public List<ErpSaleOrderRecentItem> recentItemsByProduct(Long customerId, Long productId, int limit) {
        Long tenantId = TenantContext.requireTenantId();
        if (customerId == null || productId == null) {
            return new ArrayList<>();
        }
        int finalLimit = limit <= 0 ? 10 : Math.min(limit, 50);
        return erpSaleOrderItemMapper.findRecentItems(tenantId, customerId, productId, finalLimit);
    }

    @Override
    public PageResponse<ErpSaleOrderRecentItem> recentItemsByProduct(Long customerId, Long productId, long page, long size) {
        Long tenantId = TenantContext.requireTenantId();
        long finalPage = page <= 0 ? 1 : page;
        long finalSize = size <= 0 ? 10 : Math.min(size, 100);
        if (customerId == null || productId == null) {
            return new PageResponse<>(0, finalPage, finalSize, List.of());
        }
        long total = erpSaleOrderItemMapper.countRecentItems(tenantId, customerId, productId);
        long offset = (finalPage - 1) * finalSize;
        List<ErpSaleOrderRecentItem> items = total == 0
            ? List.of()
            : erpSaleOrderItemMapper.findRecentItemsPage(
                tenantId,
                customerId,
                productId,
                (int) finalSize,
                offset
            );
        return new PageResponse<>(total, finalPage, finalSize, items);
    }

    @Override
    public PageResponse<ErpSaleOrderHistoryItem> productHistory(Long customerId,
                                                                Long productId,
                                                                String keyword,
                                                                Instant startAt,
                                                                Instant endAt,
                                                                long page,
                                                                long size) {
        Long tenantId = TenantContext.requireTenantId();
        if (productId == null) {
            return new PageResponse<>(0, page, size, List.of());
        }
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        long finalSize = size <= 0 ? 10 : Math.min(size, 50);
        long finalPage = page <= 0 ? 1 : page;
        long offset = (finalPage - 1) * finalSize;
        long total = erpSaleOrderItemMapper.countProductHistory(
            tenantId,
            customerId,
            productId,
            normalizedKeyword,
            startAt,
            endAt
        );
        List<ErpSaleOrderHistoryItem> items = total == 0
            ? List.of()
            : erpSaleOrderItemMapper.findProductHistoryPage(
                tenantId,
                customerId,
                productId,
                normalizedKeyword,
                startAt,
                endAt,
                (int) finalSize,
                offset
            );
        return new PageResponse<>(total, finalPage, finalSize, items);
    }

    private QueryWrapper<ErpSaleOrder> baseWrapper(String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpSaleOrder> wrapper = new QueryWrapper<ErpSaleOrder>()
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
            return "APPROVED,CANCELLED,RED_FLUSHED";
        }
        List<String> statuses = java.util.Arrays.stream(status.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .filter(s -> !STATUS_DRAFT.equals(s))
            .toList();
        if (statuses.isEmpty()) {
            throw new IllegalArgumentException("已审核接口不能查询草稿状态");
        }
        return String.join(",", statuses);
    }

    private void validateHeaderMasterData(Long tenantId,
                                          Long customerId,
                                          String settlementMethod,
                                          String receiptMethodCode,
                                          BigDecimal paidAmount) {
        if (customerId == null) {
            throw new IllegalArgumentException("请选择客户");
        }
        requireEnabledCustomer(tenantId, customerId);
        if (settlementMethod == null || settlementMethod.isBlank()) {
            throw new IllegalArgumentException("请选择结算方式");
        }
        ErpSettlementMethod method = resolveSettlementMethod(tenantId, settlementMethod);
        if (method == null || Boolean.FALSE.equals(method.getEnabled())) {
            throw new IllegalArgumentException("结算方式不存在或已停用");
        }
        if (paidAmount != null && paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (receiptMethodCode == null || receiptMethodCode.isBlank()) {
                throw new IllegalArgumentException("请选择收款方式");
            }
            ErpReceiptMethod receiptMethod = erpReceiptMethodMapper.findByCode(tenantId, receiptMethodCode);
            if (receiptMethod == null || Boolean.FALSE.equals(receiptMethod.getEnabled())) {
                throw new IllegalArgumentException("收款方式不存在或已停用");
            }
        }
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

    private List<ErpSaleOrderItem> buildItems(Long tenantId,
                                              Long orderId,
                                              Long customerId,
                                              List<ErpSaleOrderItemRequest> requests,
                                              Set<Long> allowedDisabledProductIds) {
        List<ErpSaleOrderItem> items = new ArrayList<>();
        Long customerCategoryId = resolveCustomerCategoryId(tenantId, customerId);
        int index = 1;
        for (ErpSaleOrderItemRequest request : requests) {
            ErpProduct product = requireUsableProduct(tenantId, request.productId(), allowedDisabledProductIds);
            ErpSaleOrderItem item = new ErpSaleOrderItem();
            item.setTenantId(tenantId);
            item.setOrderId(orderId);
            item.setProductId(product.getId());
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
                throw new IllegalArgumentException("销售数量必须大于0");
            }
            item.setQty(request.qty());
            BigDecimal taxRate = request.taxRate() == null ? BigDecimal.ZERO : request.taxRate();
            if (taxRate.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("税率不能小于0");
            }
            BigDecimal price = request.price();
            BigDecimal priceInclTax = request.priceInclTax();
            if (price == null && priceInclTax == null) {
                price = resolveDefaultSalePrice(tenantId, product, customerCategoryId);
            }
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
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("销售单价不能小于0");
            }
            if (priceInclTax.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("含税单价不能小于0");
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

    private ErpCustomer requireEnabledCustomer(Long tenantId, Long customerId) {
        ErpCustomer customer = erpCustomerMapper.selectOne(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .eq("id", customerId));
        if (customer == null || Boolean.FALSE.equals(customer.getEnabled())) {
            throw new IllegalArgumentException("客户不存在或已停用");
        }
        return customer;
    }

    private Long resolveCustomerCategoryId(Long tenantId, Long customerId) {
        if (customerId == null) {
            return null;
        }
        return requireEnabledCustomer(tenantId, customerId).getCategoryId();
    }

    private BigDecimal resolveDefaultSalePrice(Long tenantId, ErpProduct product, Long customerCategoryId) {
        if (product == null) {
            return BigDecimal.ZERO;
        }
        if (customerCategoryId != null) {
            ErpProductPrice categoryPrice = erpProductPriceMapper.findByProductAndCategory(
                tenantId,
                product.getId(),
                customerCategoryId
            );
            if (categoryPrice != null && categoryPrice.getSalePrice() != null) {
                return categoryPrice.getSalePrice();
            }
        }
        return product.getSalePrice() == null ? BigDecimal.ZERO : product.getSalePrice();
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

    private Set<Long> existingProductIds(List<ErpSaleOrderItem> items) {
        Set<Long> ids = new HashSet<>();
        if (items == null) {
            return ids;
        }
        for (ErpSaleOrderItem item : items) {
            if (item != null && item.getProductId() != null) {
                ids.add(item.getProductId());
            }
        }
        return ids;
    }

    private boolean hasApprovedReceiptImpact(Long tenantId, Long saleOrderId) {
        ErpAccountsReceivable receivable = erpAccountsReceivableMapper.findBySaleOrderId(tenantId, saleOrderId);
        if (receivable != null && hasApprovedReceiptAllocation(tenantId, receivable.getId(), true)) {
            return true;
        }
        List<ErpReceipt> approvedReceipts = erpReceiptMapper.selectList(new QueryWrapper<ErpReceipt>()
            .eq("tenant_id", tenantId)
            .eq("sale_order_id", saleOrderId)
            .eq("status", STATUS_APPROVED));
        for (ErpReceipt receipt : approvedReceipts) {
            if (receiptImpactTotal(receipt).compareTo(BigDecimal.ZERO) > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasApprovedSaleReturn(Long tenantId, Long saleOrderId) {
        return erpSaleReturnMapper.countApprovedBySaleOrderId(tenantId, saleOrderId) > 0;
    }

    private boolean hasApprovedReceiptAllocation(Long tenantId, Long receivableId, boolean positiveImpact) {
        List<ErpReceiptReceivable> allocations = erpReceiptReceivableMapper.findByReceivableId(tenantId, receivableId);
        if (allocations == null || allocations.isEmpty()) {
            return false;
        }
        List<Long> receiptIds = allocations.stream()
            .map(ErpReceiptReceivable::getReceiptId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        if (receiptIds.isEmpty()) {
            return false;
        }
        List<ErpReceipt> receipts = erpReceiptMapper.selectBatchIds(receiptIds);
        if (receipts == null || receipts.isEmpty()) {
            return false;
        }
        java.util.Map<Long, ErpReceipt> receiptMap = receipts.stream()
            .filter(receipt -> tenantId.equals(receipt.getTenantId()))
            .collect(java.util.stream.Collectors.toMap(ErpReceipt::getId, receipt -> receipt, (left, right) -> left));
        for (ErpReceiptReceivable allocation : allocations) {
            ErpReceipt receipt = receiptMap.get(allocation.getReceiptId());
            if (receipt == null || !STATUS_APPROVED.equals(receipt.getStatus())) {
                continue;
            }
            BigDecimal allocatedTotal = allocation.getAllocatedTotal() == null ? BigDecimal.ZERO : allocation.getAllocatedTotal();
            if (positiveImpact && allocatedTotal.compareTo(BigDecimal.ZERO) > 0) {
                return true;
            }
            if (!positiveImpact && allocatedTotal.compareTo(BigDecimal.ZERO) < 0) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal receiptImpactTotal(ErpReceipt receipt) {
        if (receipt == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = receipt.getAmount() == null ? BigDecimal.ZERO : receipt.getAmount();
        BigDecimal discount = receipt.getDiscountAmount() == null ? BigDecimal.ZERO : receipt.getDiscountAmount();
        return amount.add(discount);
    }

    private void normalizeCreditSettlementFields(Long tenantId, ErpSaleOrder order) {
        if (!isCreditSettlement(tenantId, order.getSettlementMethod())) {
            return;
        }
        order.setPaidAmount(BigDecimal.ZERO);
        order.setReceiptMethodCode(null);
    }

    private boolean isCreditSettlement(Long tenantId, String settlementMethod) {
        if (settlementMethod == null || settlementMethod.isBlank()) {
            return false;
        }
        String code = settlementMethod.trim().toUpperCase();
        if ("CREDIT".equals(code) || "ON_ACCOUNT".equals(code) || "AR".equals(code)) {
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

    private void applyTotals(ErpSaleOrder order, List<ErpSaleOrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalExcl = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalIncl = BigDecimal.ZERO;
        for (ErpSaleOrderItem item : items) {
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

    private void validateSettlementAmounts(ErpSaleOrder order) {
        BigDecimal paidAmount = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        BigDecimal discountAmount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        if (paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("付款金额不能小于0");
        }
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("优惠金额不能小于0");
        }
        BigDecimal totalAmountInclTax = order.getTotalAmountInclTax() == null ? BigDecimal.ZERO : order.getTotalAmountInclTax();
        if (paidAmount.add(discountAmount).compareTo(totalAmountInclTax) > 0) {
            throw new IllegalArgumentException("付款金额与优惠金额之和不能大于销售总金额");
        }
    }

    private ErpSaleOrder loadForUpdate(Long tenantId, Long id) {
        ErpSaleOrder order = erpSaleOrderMapper.selectOne(new QueryWrapper<ErpSaleOrder>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (order == null) {
            throw new IllegalArgumentException("销售单不存在");
        }
        return order;
    }

    private void updateWithVersion(Long tenantId, ErpSaleOrder order) {
        Long version = order.getVersion() == null ? 0L : order.getVersion();
        order.setVersion(version + 1);
        int updated = erpSaleOrderMapper.update(order, new QueryWrapper<ErpSaleOrder>()
            .eq("tenant_id", tenantId)
            .eq("id", order.getId())
            .eq("version", version));
        if (updated == 0) {
            throw new IllegalArgumentException("销售单已被修改，请刷新重试");
        }
    }

    private void enrichItemCostSnapshots(Long tenantId, Long saleOrderId, List<ErpSaleOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> unitCostByItemId = erpStockTxnMapper.findSaleItemCostSnapshots(tenantId, saleOrderId).stream()
            .filter(snapshot -> snapshot.bizItemId() != null && snapshot.unitCost() != null)
            .collect(Collectors.toMap(
                ErpSaleOrderItemCostSnapshot::bizItemId,
                ErpSaleOrderItemCostSnapshot::unitCost,
                (left, right) -> right
            ));
        for (ErpSaleOrderItem item : items) {
            if (item.getId() == null) {
                continue;
            }
            BigDecimal unitCost = unitCostByItemId.get(item.getId());
            if (unitCost != null) {
                item.setUnitCost(unitCost);
            }
        }
    }

    private void reserveDraftStock(Long tenantId, List<ErpSaleOrderItem> items) {
        for (ErpSaleOrderItem item : items) {
            changeReservedStock(tenantId, item, item.getQty(), "销售单");
        }
    }

    private void releaseDraftStock(Long tenantId, List<ErpSaleOrderItem> items) {
        for (ErpSaleOrderItem item : items) {
            changeReservedStock(tenantId, item, item.getQty().negate(), "销售单");
        }
    }

    private void changeReservedStock(Long tenantId, ErpSaleOrderItem item, BigDecimal delta, String bizName) {
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        ErpStockBalance updatedBalance = erpStockBalanceMapper.addReservedQtyIfEnough(
            tenantId, item.getProductId(), item.getWarehouseId(), item.getLocationId(), delta, resolveCurrentUsername());
        if (updatedBalance != null) {
            return;
        }
        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(bizName + "库存占用数据异常，请刷新后重试");
        }
        String productLabel = item.getProductName() == null ? item.getProductCode() : item.getProductName();
        throw insufficientStockException(tenantId, item.getProductId(), item.getWarehouseId(), item.getLocationId(), productLabel, delta);
    }

    private void applyStockDelta(Long tenantId, ErpSaleOrderItem item, BigDecimal delta, String bizType, Long orderId, boolean consumeReserved) {
        Long warehouseId = item.getWarehouseId();
        Long locationId = item.getLocationId();
        String operator = resolveCurrentUsername();
        BigDecimal unitCost = resolveSaleStockUnitCost(tenantId, item.getProductId(), bizType, orderId);
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            erpCostService.applyInboundAverageCost(tenantId, item.getProductId(), delta, unitCost);
        }
        ErpStockBalance updatedBalance;
        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            if (consumeReserved) {
                updatedBalance = erpStockBalanceMapper.consumeReservedQty(
                    tenantId, item.getProductId(), warehouseId, locationId, delta.abs(), operator);
            } else {
                updatedBalance = erpStockBalanceMapper.addQtyIfEnoughAvailable(
                    tenantId, item.getProductId(), warehouseId, locationId, delta, operator);
            }
        } else {
            updatedBalance = erpStockBalanceMapper.upsertAddQty(
                tenantId, item.getProductId(), warehouseId, locationId, delta, operator);
        }
        if (updatedBalance == null) {
            String productLabel = item.getProductName() == null ? item.getProductCode() : item.getProductName();
            throw insufficientStockException(tenantId, item.getProductId(), warehouseId, locationId, productLabel, delta.abs());
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
        BigDecimal totalCost = unitCost.multiply(delta).setScale(4, RoundingMode.HALF_UP);
        txn.setUnitCost(unitCost);
        txn.setTotalCost(totalCost);
        txn.setOperator(operator);
        txn.setOperatorId(null);
        txn.setRemark(item.getRemark());
        txn.setCreatedAt(Instant.now());
        erpStockTxnMapper.insert(txn);
    }

    private IllegalArgumentException insufficientStockException(Long tenantId,
                                                                Long productId,
                                                                Long warehouseId,
                                                                Long locationId,
                                                                String productLabel,
                                                                BigDecimal required) {
        ErpStockBalance currentBalance = erpStockBalanceMapper.findByKey(tenantId, productId, warehouseId, locationId);
        BigDecimal currentQty = currentBalance == null || currentBalance.getQtyOnHand() == null
            ? BigDecimal.ZERO
            : currentBalance.getQtyOnHand();
        BigDecimal lockedQty = currentBalance == null || currentBalance.getQtyLocked() == null
            ? BigDecimal.ZERO
            : currentBalance.getQtyLocked();
        return new IllegalArgumentException(
            "库存不足，商品[" + productLabel + "] 可用=" + currentQty.subtract(lockedQty) + "，需求=" + required
        );
    }

    private BigDecimal resolveSaleStockUnitCost(Long tenantId, Long productId, String bizType, Long orderId) {
        if ("SALE_RED_FLUSH".equals(bizType) && orderId != null && productId != null) {
            BigDecimal originalCost = erpStockTxnMapper.findSaleIssueUnitCost(tenantId, orderId, productId);
            if (originalCost != null && originalCost.compareTo(BigDecimal.ZERO) > 0) {
                return originalCost;
            }
        }
        return erpCostService.getProductCost(tenantId, productId);
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }

    private String generateTxnNo() {
        return "TXN-" + UUID.randomUUID();
    }

    private String ensureOrderNo(Long tenantId, String provided, String orderType, String defaultPrefix) {
        String trimmed = provided == null ? "" : provided.trim();
        if (!trimmed.isEmpty()) {
            ErpSaleOrder existing = erpSaleOrderMapper.findByOrderNo(tenantId, trimmed);
            if (existing != null) {
                throw new IllegalArgumentException("销售单号已存在");
            }
            return trimmed;
        }
        String prefix = readConfig("erp.order.no.sale.prefix", defaultPrefix);
        String dateFormat = readConfig("erp.order.no.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.order.no.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, orderType, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, orderType, dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private void ensureReceivableAndReceipt(Long tenantId, ErpSaleOrder order, String operator) {
        BigDecimal total = order.getTotalAmountInclTax() == null ? BigDecimal.ZERO : order.getTotalAmountInclTax();
        BigDecimal discount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        if (discount.compareTo(total) > 0) {
            discount = total;
        }
        BigDecimal paidCash = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
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
        BigDecimal receivableAmount = netAmount.subtract(paidCash);
        if (receivableAmount.compareTo(BigDecimal.ZERO) < 0) {
            receivableAmount = BigDecimal.ZERO;
        }

        ErpAccountsReceivable existing = erpAccountsReceivableMapper.findBySaleOrderId(tenantId, order.getId());
        if (existing == null && receivableAmount.compareTo(BigDecimal.ZERO) > 0) {
            ErpAccountsReceivable ar = new ErpAccountsReceivable();
            ar.setTenantId(tenantId);
            ar.setSaleOrderId(order.getId());
            ar.setOrderNo(order.getOrderNo());
            ar.setCustomerId(order.getCustomerId());
            ar.setTotalAmount(receivableAmount);
            ar.setPaidAmount(BigDecimal.ZERO);
            ar.setUnpaidAmount(receivableAmount);
            ar.setStatus(STATUS_OPEN);
            ar.setSettlementMethod(order.getSettlementMethod());
            ar.setSourceType(SOURCE_SALE_ORDER);
            ar.setSourceId(order.getId());
            ar.setRemark(AUTO_RECEIVABLE_REMARK);
            ar.setCreatedAt(Instant.now());
            ar.setUpdatedAt(Instant.now());
            erpAccountsReceivableMapper.insert(ar);
            existing = ar;
        } else if (existing != null) {
            existing.setOrderNo(order.getOrderNo());
            existing.setCustomerId(order.getCustomerId());
            existing.setTotalAmount(receivableAmount);
            existing.setPaidAmount(BigDecimal.ZERO);
            existing.setUnpaidAmount(receivableAmount);
            existing.setStatus(receivableAmount.compareTo(BigDecimal.ZERO) == 0 ? STATUS_SETTLED : STATUS_OPEN);
            existing.setSettlementMethod(order.getSettlementMethod());
            existing.setSourceType(SOURCE_SALE_ORDER);
            existing.setSourceId(order.getId());
            existing.setRemark(AUTO_RECEIVABLE_REMARK);
            existing.setUpdatedAt(Instant.now());
            erpAccountsReceivableMapper.updateById(existing);
        }

        ErpReceipt receiptExisting = erpReceiptMapper.findBySaleOrderId(tenantId, order.getId());
        if (paidCash.compareTo(BigDecimal.ZERO) > 0) {
            if (receiptExisting == null) {
                ErpReceipt receipt = new ErpReceipt();
                receipt.setTenantId(tenantId);
                receipt.setReceivableId(null);
                receipt.setSaleOrderId(order.getId());
                receipt.setReceiptNo(generateReceiptNo(tenantId));
                receipt.setCustomerId(order.getCustomerId());
                receipt.setAmount(paidCash);
                receipt.setDiscountAmount(BigDecimal.ZERO);
                receipt.setSettlementMethod(order.getSettlementMethod());
                receipt.setReceiptMethodCode(order.getReceiptMethodCode());
                receipt.setStatus(STATUS_APPROVED);
                receipt.setReceivedAt(Instant.now());
                receipt.setRemark(AUTO_RECEIPT_REMARK);
                receipt.setCreatedAt(Instant.now());
                receipt.setCreatedBy(operator);
                receipt.setUpdatedAt(Instant.now());
                receipt.setUpdatedBy(operator);
                erpReceiptMapper.insert(receipt);
            } else if (AUTO_RECEIPT_REMARK.equals(receiptExisting.getRemark())) {
                receiptExisting.setReceivableId(null);
                receiptExisting.setCustomerId(order.getCustomerId());
                receiptExisting.setAmount(paidCash);
                receiptExisting.setDiscountAmount(BigDecimal.ZERO);
                receiptExisting.setSettlementMethod(order.getSettlementMethod());
                receiptExisting.setReceiptMethodCode(order.getReceiptMethodCode());
                receiptExisting.setStatus(STATUS_APPROVED);
                receiptExisting.setReceivedAt(Instant.now());
                receiptExisting.setRemark(AUTO_RECEIPT_REMARK);
                receiptExisting.setUpdatedAt(Instant.now());
                receiptExisting.setUpdatedBy(operator);
                erpReceiptMapper.updateById(receiptExisting);

                erpReceiptReceivableMapper.delete(new QueryWrapper<ErpReceiptReceivable>()
                    .eq("tenant_id", tenantId)
                    .eq("receipt_id", receiptExisting.getId()));
            }
        } else if (receiptExisting != null && AUTO_RECEIPT_REMARK.equals(receiptExisting.getRemark())) {
            receiptExisting.setReceivableId(null);
            receiptExisting.setCustomerId(order.getCustomerId());
            receiptExisting.setAmount(BigDecimal.ZERO);
            receiptExisting.setDiscountAmount(BigDecimal.ZERO);
            receiptExisting.setSettlementMethod(order.getSettlementMethod());
            receiptExisting.setReceiptMethodCode(null);
            receiptExisting.setStatus(STATUS_APPROVED);
            receiptExisting.setReceivedAt(Instant.now());
            receiptExisting.setUpdatedAt(Instant.now());
            receiptExisting.setUpdatedBy(operator);
            erpReceiptMapper.updateById(receiptExisting);
            erpReceiptReceivableMapper.delete(new QueryWrapper<ErpReceiptReceivable>()
                .eq("tenant_id", tenantId)
                .eq("receipt_id", receiptExisting.getId()));
        }
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

    private String resolveOrderNoForUpdate(String provided, String current, Long tenantId, Long orderId) {
        if (provided == null || provided.isBlank()) {
            return current;
        }
        String trimmed = provided.trim();
        if (!trimmed.equals(current)) {
            ErpSaleOrder existing = erpSaleOrderMapper.findByOrderNo(tenantId, trimmed);
            if (existing != null && !existing.getId().equals(orderId)) {
                throw new IllegalArgumentException("销售单号已存在");
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

    private String normalizeSettlementMethod(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        Long tenantId = TenantContext.requireTenantId();
        ErpSettlementMethod method = resolveSettlementMethod(tenantId, value.trim());
        return method == null ? value.trim() : method.getCode();
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

    private int readIntConfig(String key, int fallback) {
        String value = readConfig(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private BigDecimal calcPriceInclTax(BigDecimal price, BigDecimal taxRate) {
        return price.multiply(BigDecimal.ONE.add(taxRate)).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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

    private Instant parseOrderAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
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
}
