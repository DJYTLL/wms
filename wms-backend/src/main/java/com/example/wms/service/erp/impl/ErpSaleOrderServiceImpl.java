package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSaleOrderCreateRequest;
import com.example.wms.dto.erp.ErpSaleOrderDetail;
import com.example.wms.dto.erp.ErpSaleOrderHistoryItem;
import com.example.wms.dto.erp.ErpSaleOrderRecentItem;
import com.example.wms.dto.erp.ErpSaleOrderItemRequest;
import com.example.wms.dto.erp.ErpSaleOrderUpdateRequest;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.*;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpReceiptReceivableMapper;
import com.example.wms.mapper.erp.ErpSaleOrderItemMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.service.erp.ErpSaleOrderService;
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
import java.util.List;
import java.util.UUID;

// 销售单服务实现（ERP进销存）
@Service
public class ErpSaleOrderServiceImpl implements ErpSaleOrderService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_RED_FLUSHED = "RED_FLUSHED";
    private static final String ORDER_TYPE = "SALE";
    private static final String DEFAULT_SETTLEMENT_METHOD = "CASH";
    private static final String RECEIPT_ORDER_TYPE = "RECEIPT";

    private final ErpSaleOrderMapper erpSaleOrderMapper;
    private final ErpSaleOrderItemMapper erpSaleOrderItemMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpStockBalanceMapper erpStockBalanceMapper;
    private final ErpStockTxnMapper erpStockTxnMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final ErpAccountsReceivableMapper erpAccountsReceivableMapper;
    private final ErpReceiptMapper erpReceiptMapper;
    private final ErpReceiptReceivableMapper erpReceiptReceivableMapper;
    private final SystemConfigMapper systemConfigMapper;

    public ErpSaleOrderServiceImpl(ErpSaleOrderMapper erpSaleOrderMapper,
                                   ErpSaleOrderItemMapper erpSaleOrderItemMapper,
                                   ErpProductMapper erpProductMapper,
                                   ErpStockBalanceMapper erpStockBalanceMapper,
                                   ErpStockTxnMapper erpStockTxnMapper,
                                   ErpOrderSequenceMapper erpOrderSequenceMapper,
                                   ErpAccountsReceivableMapper erpAccountsReceivableMapper,
                                   ErpReceiptMapper erpReceiptMapper,
                                   ErpReceiptReceivableMapper erpReceiptReceivableMapper,
                                   SystemConfigMapper systemConfigMapper) {
        this.erpSaleOrderMapper = erpSaleOrderMapper;
        this.erpSaleOrderItemMapper = erpSaleOrderItemMapper;
        this.erpProductMapper = erpProductMapper;
        this.erpStockBalanceMapper = erpStockBalanceMapper;
        this.erpStockTxnMapper = erpStockTxnMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.erpAccountsReceivableMapper = erpAccountsReceivableMapper;
        this.erpReceiptMapper = erpReceiptMapper;
        this.erpReceiptReceivableMapper = erpReceiptReceivableMapper;
        this.systemConfigMapper = systemConfigMapper;
    }

    @Override
    public List<ErpSaleOrder> listAll(String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpSaleOrder> wrapper = baseWrapper(keyword, status, customerId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        return erpSaleOrderMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpSaleOrder> page(long page, long size, String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        Page<ErpSaleOrder> pageReq = Page.of(page, size);
        QueryWrapper<ErpSaleOrder> wrapper = baseWrapper(keyword, status, customerId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        Page<ErpSaleOrder> result = erpSaleOrderMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
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
        return new ErpSaleOrderDetail(order, items);
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
        String orderNo = ensureOrderNo(tenantId, request.orderNo(), ORDER_TYPE, "SO");
        ErpSaleOrder order = new ErpSaleOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(orderNo);
        order.setStatus(STATUS_DRAFT);
        order.setCustomerId(request.customerId());
        Instant orderAt = parseOrderAt(request.orderAt());
        order.setOrderAt(orderAt == null ? Instant.now() : orderAt);
        order.setSettlementMethod(normalizeSettlementMethod(request.settlementMethod(), DEFAULT_SETTLEMENT_METHOD));
        order.setDeliveryMethod(normalizeCode(request.deliveryMethod()));
        order.setPaidAmount(normalizeAmount(request.paidAmount()));
        order.setDiscountAmount(normalizeAmount(request.discountAmount()));
        order.setTotalAmount(BigDecimal.ZERO);
        order.setTotalAmountExclTax(BigDecimal.ZERO);
        order.setTotalTaxAmount(BigDecimal.ZERO);
        order.setTotalAmountInclTax(BigDecimal.ZERO);
        order.setVersion(0L);
        order.setRemark(request.remark());
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        erpSaleOrderMapper.insert(order);

        List<ErpSaleOrderItem> items = buildItems(tenantId, order.getId(), request.items());
        for (ErpSaleOrderItem item : items) {
            erpSaleOrderItemMapper.insert(item);
        }
        applyTotals(order, items);
        order.setUpdatedAt(Instant.now());
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
        order.setDeliveryMethod(normalizeCode(request.deliveryMethod()));
        order.setPaidAmount(normalizeAmount(request.paidAmount()));
        order.setDiscountAmount(normalizeAmount(request.discountAmount()));
        order.setRemark(request.remark());
        order.setUpdatedAt(Instant.now());

        erpSaleOrderItemMapper.delete(new QueryWrapper<ErpSaleOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("order_id", id));

        List<ErpSaleOrderItem> items = buildItems(tenantId, id, request.items());
        for (ErpSaleOrderItem item : items) {
            erpSaleOrderItemMapper.insert(item);
        }
        applyTotals(order, items);
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
        for (ErpSaleOrderItem item : items) {
            applyStockDelta(tenantId, item, item.getQty().negate(), "SALE_APPROVE", id);
        }
        ensureReceivableAndReceipt(tenantId, order);
        order.setStatus(STATUS_APPROVED);
        order.setApprovedBy(resolveCurrentUsername());
        order.setApprovedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        updateWithVersion(tenantId, order);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_SALE_UNAPPROVE", entityType = "erp_sale_order", entityId = "{arg0}")
    public void unapprove(Long id) {
        throw new IllegalArgumentException("销售单已审核后仅允许红冲，不允许反审核");
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_SALE_CANCEL", entityType = "erp_sale_order", entityId = "{arg0}")
    public void cancel(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSaleOrder order = loadForUpdate(tenantId, id);
        if (STATUS_APPROVED.equals(order.getStatus())) {
            throw new IllegalArgumentException("已审核单据不可取消，请使用红冲");
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可取消");
        }
        order.setStatus(STATUS_CANCELLED);
        order.setCancelledBy(resolveCurrentUsername());
        order.setCancelledAt(Instant.now());
        order.setUpdatedAt(Instant.now());
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
        List<ErpSaleOrderItem> items = erpSaleOrderItemMapper.findByOrderId(tenantId, id);
        for (ErpSaleOrderItem item : items) {
            applyStockDelta(tenantId, item, item.getQty(), "SALE_RED_FLUSH", id);
        }
        ErpAccountsReceivable receivable = erpAccountsReceivableMapper.findBySaleOrderId(tenantId, order.getId());
        if (receivable != null) {
            receivable.setTotalAmount(BigDecimal.ZERO);
            receivable.setPaidAmount(BigDecimal.ZERO);
            receivable.setUnpaidAmount(BigDecimal.ZERO);
            receivable.setStatus("RED_FLUSHED");
            receivable.setRemark(appendRedFlushReason(receivable.getRemark(), reason));
            receivable.setUpdatedAt(Instant.now());
            erpAccountsReceivableMapper.updateById(receivable);
        }
        order.setStatus(STATUS_RED_FLUSHED);
        order.setRemark(appendRedFlushReason(order.getRemark(), reason));
        order.setUpdatedAt(Instant.now());
        updateWithVersion(tenantId, order);
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
            wrapper.ge("created_at", startAt);
        }
        if (endAt != null) {
            wrapper.le("created_at", endAt);
        }
        return wrapper;
    }

    private List<ErpSaleOrderItem> buildItems(Long tenantId, Long orderId, List<ErpSaleOrderItemRequest> requests) {
        List<ErpSaleOrderItem> items = new ArrayList<>();
        int index = 1;
        for (ErpSaleOrderItemRequest request : requests) {
            ErpProduct product = erpProductMapper.selectOne(new QueryWrapper<ErpProduct>()
                .eq("tenant_id", tenantId)
                .eq("id", request.productId()));
            if (product == null) {
                throw new IllegalArgumentException("商品不存在");
            }
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
            item.setWarehouseId(warehouseId);
            item.setLocationId(locationId);
            if (request.qty() == null || request.qty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("销售数量必须大于0");
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

    private void applyStockDelta(Long tenantId, ErpSaleOrderItem item, BigDecimal delta, String bizType, Long orderId) {
        Long warehouseId = item.getWarehouseId();
        Long locationId = item.getLocationId();
        ErpStockBalance balance = erpStockBalanceMapper.findByKey(tenantId, item.getProductId(), warehouseId, locationId);
        BigDecimal before = balance == null ? BigDecimal.ZERO : balance.getQtyOnHand();
        BigDecimal after = before.add(delta);
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal required = delta.abs();
            String productLabel = item.getProductName() == null ? item.getProductCode() : item.getProductName();
            throw new IllegalArgumentException(
                "库存不足，商品[" + productLabel + "] 可用=" + before + "，需求=" + required
            );
        }
        if (balance == null) {
            balance = new ErpStockBalance();
            balance.setTenantId(tenantId);
            balance.setProductId(item.getProductId());
            balance.setWarehouseId(warehouseId);
            balance.setLocationId(locationId);
            balance.setQtyOnHand(after);
            balance.setUpdatedBy(resolveCurrentUsername());
            balance.setUpdatedAt(Instant.now());
            erpStockBalanceMapper.insert(balance);
        } else {
            balance.setQtyOnHand(after);
            balance.setUpdatedBy(resolveCurrentUsername());
            balance.setUpdatedAt(Instant.now());
            erpStockBalanceMapper.updateById(balance);
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
        BigDecimal unitCost = getProductCost(tenantId, item.getProductId());
        BigDecimal totalCost = unitCost.multiply(delta).setScale(4, RoundingMode.HALF_UP);
        txn.setUnitCost(unitCost);
        txn.setTotalCost(totalCost);
        txn.setOperator(resolveCurrentUsername());
        txn.setOperatorId(null);
        txn.setRemark(item.getRemark());
        txn.setCreatedAt(Instant.now());
        erpStockTxnMapper.insert(txn);
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }

    private BigDecimal getProductCost(Long tenantId, Long productId) {
        if (productId == null) {
            return BigDecimal.ZERO;
        }
        ErpProduct product = erpProductMapper.selectOne(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", tenantId)
            .eq("id", productId));
        if (product == null || product.getCostPrice() == null) {
            return BigDecimal.ZERO;
        }
        return product.getCostPrice();
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

    private void ensureReceivableAndReceipt(Long tenantId, ErpSaleOrder order) {
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
        BigDecimal totalApplied = paidCash.add(discount);
        if (totalApplied.compareTo(total) > 0) {
            totalApplied = total;
        }

        ErpAccountsReceivable existing = erpAccountsReceivableMapper.findBySaleOrderId(tenantId, order.getId());
        if (existing == null) {
            BigDecimal unpaid = total.subtract(totalApplied);
            String status = unpaid.compareTo(BigDecimal.ZERO) == 0 ? "SETTLED" : "OPEN";

            ErpAccountsReceivable ar = new ErpAccountsReceivable();
            ar.setTenantId(tenantId);
            ar.setSaleOrderId(order.getId());
            ar.setOrderNo(order.getOrderNo());
            ar.setCustomerId(order.getCustomerId());
            ar.setTotalAmount(total);
            ar.setPaidAmount(totalApplied);
            ar.setUnpaidAmount(unpaid);
            ar.setStatus(status);
            ar.setSettlementMethod(order.getSettlementMethod());
            ar.setRemark("销售单审核自动生成");
            ar.setCreatedAt(Instant.now());
            ar.setUpdatedAt(Instant.now());
            erpAccountsReceivableMapper.insert(ar);
            existing = ar;
        }

        if (totalApplied.compareTo(BigDecimal.ZERO) > 0) {
            ErpReceipt receiptExisting = erpReceiptMapper.findBySaleOrderId(tenantId, order.getId());
            if (receiptExisting == null) {
                ErpReceipt receipt = new ErpReceipt();
                receipt.setTenantId(tenantId);
                receipt.setReceivableId(existing.getId());
                receipt.setSaleOrderId(order.getId());
                receipt.setReceiptNo(generateReceiptNo(tenantId));
                receipt.setCustomerId(order.getCustomerId());
                receipt.setAmount(paidCash);
                receipt.setDiscountAmount(discount);
                receipt.setSettlementMethod(order.getSettlementMethod());
                receipt.setStatus("APPROVED");
                receipt.setReceivedAt(Instant.now());
                receipt.setRemark("销售单审核自动收款");
                receipt.setCreatedAt(Instant.now());
                receipt.setUpdatedAt(Instant.now());
                erpReceiptMapper.insert(receipt);

                ErpReceiptReceivable allocation = new ErpReceiptReceivable();
                allocation.setTenantId(tenantId);
                allocation.setReceiptId(receipt.getId());
                allocation.setReceivableId(existing.getId());
                allocation.setAllocatedAmount(paidCash);
                allocation.setAllocatedDiscount(discount);
                allocation.setAllocatedTotal(totalApplied);
                allocation.setCreatedAt(Instant.now());
                erpReceiptReceivableMapper.insert(allocation);
            }
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
        SystemConfig config = systemConfigMapper.findByKey(key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return fallback;
        }
        return config.getConfigValue().trim();
    }

    private String normalizeSettlementMethod(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
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
