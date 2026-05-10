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
import com.example.wms.entity.erp.ErpPaymentPayable;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.entity.erp.ErpPurchaseOrderItem;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockTxn;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPaymentPayableMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderItemMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.service.erp.ErpPurchaseOrderService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 采购单服务实现（ERP进销存）
@Service
public class ErpPurchaseOrderServiceImpl implements ErpPurchaseOrderService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String ORDER_TYPE = "PURCHASE";

    private final ErpPurchaseOrderMapper erpPurchaseOrderMapper;
    private final ErpPurchaseOrderItemMapper erpPurchaseOrderItemMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpStockBalanceMapper erpStockBalanceMapper;
    private final ErpStockTxnMapper erpStockTxnMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ErpAccountsPayableMapper erpAccountsPayableMapper;
    private final ErpPaymentMapper erpPaymentMapper;
    private final ErpPaymentPayableMapper erpPaymentPayableMapper;

    public ErpPurchaseOrderServiceImpl(ErpPurchaseOrderMapper erpPurchaseOrderMapper,
                                       ErpPurchaseOrderItemMapper erpPurchaseOrderItemMapper,
                                       ErpProductMapper erpProductMapper,
                                       ErpStockBalanceMapper erpStockBalanceMapper,
                                       ErpStockTxnMapper erpStockTxnMapper,
                                      ErpOrderSequenceMapper erpOrderSequenceMapper,
                                      SystemConfigMapper systemConfigMapper,
                                      ErpAccountsPayableMapper erpAccountsPayableMapper,
                                      ErpPaymentMapper erpPaymentMapper,
                                      ErpPaymentPayableMapper erpPaymentPayableMapper) {
        this.erpPurchaseOrderMapper = erpPurchaseOrderMapper;
        this.erpPurchaseOrderItemMapper = erpPurchaseOrderItemMapper;
        this.erpProductMapper = erpProductMapper;
        this.erpStockBalanceMapper = erpStockBalanceMapper;
        this.erpStockTxnMapper = erpStockTxnMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.erpAccountsPayableMapper = erpAccountsPayableMapper;
        this.erpPaymentMapper = erpPaymentMapper;
        this.erpPaymentPayableMapper = erpPaymentPayableMapper;
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
    public List<ErpPurchaseOrderRecentItem> recentItemsByProduct(Long supplierId, Long productId, int limit) {
        if (supplierId == null || productId == null) {
            return List.of();
        }
        Long tenantId = TenantContext.requireTenantId();
        int finalLimit = limit <= 0 ? 10 : Math.min(limit, 50);
        return erpPurchaseOrderItemMapper.findRecentItems(tenantId, supplierId, productId, finalLimit);
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
        String orderNo = ensureOrderNo(tenantId, request.orderNo(), ORDER_TYPE, "PO");
        ErpPurchaseOrder order = new ErpPurchaseOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(orderNo);
        order.setStatus(STATUS_DRAFT);
        order.setSupplierId(request.supplierId());
        order.setOrderAt(parseOrderAt(request.orderAt()));
        order.setPaymentMethodCode(request.paymentMethodCode());
        order.setPaidAmount(request.paidAmount());
        order.setDiscountAmount(request.discountAmount());
        order.setTotalAmount(BigDecimal.ZERO);
        order.setTotalAmountExclTax(BigDecimal.ZERO);
        order.setTotalTaxAmount(BigDecimal.ZERO);
        order.setTotalAmountInclTax(BigDecimal.ZERO);
        order.setVersion(0L);
        order.setRemark(request.remark());
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        erpPurchaseOrderMapper.insert(order);

        List<ErpPurchaseOrderItem> items = buildItems(tenantId, order.getId(), request.items());
        for (ErpPurchaseOrderItem item : items) {
            erpPurchaseOrderItemMapper.insert(item);
        }
        applyTotals(order, items);
        order.setUpdatedAt(Instant.now());
        updateWithVersion(tenantId, order);
        return new ErpPurchaseOrderDetail(order, items);
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
        order.setPaymentMethodCode(request.paymentMethodCode());
        order.setPaidAmount(request.paidAmount());
        order.setDiscountAmount(request.discountAmount());
        order.setRemark(request.remark());
        order.setUpdatedAt(Instant.now());

        erpPurchaseOrderItemMapper.delete(new QueryWrapper<ErpPurchaseOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("order_id", id));

        List<ErpPurchaseOrderItem> items = buildItems(tenantId, id, request.items());
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
        order.setStatus(STATUS_APPROVED);
        order.setApprovedBy(resolveCurrentUsername());
        order.setApprovedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        updateWithVersion(tenantId, order);
        ensurePayableAndPayment(tenantId, order);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_PURCHASE_UNAPPROVE", entityType = "erp_purchase_order", entityId = "{arg0}")
    public void unapprove(Long id) {
        throw new IllegalArgumentException("采购单仅支持红冲，不支持反审核");
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
            List<ErpPayment> approvedPayments = erpPaymentMapper.selectList(new QueryWrapper<ErpPayment>()
                .eq("tenant_id", tenantId)
                .eq("purchase_order_id", order.getId())
                .eq("status", "APPROVED")
                .gt("amount", BigDecimal.ZERO));
            if (!approvedPayments.isEmpty()) {
                throw new IllegalArgumentException("请先红冲付款单");
            }
            List<ErpPurchaseOrderItem> items = erpPurchaseOrderItemMapper.findByOrderId(tenantId, id);
            for (ErpPurchaseOrderItem item : items) {
                applyStockDelta(tenantId, item, item.getQty().negate(), "PURCHASE_CANCEL", id);
            }
            ErpAccountsPayable payable = erpAccountsPayableMapper.findByPurchaseOrderId(tenantId, order.getId());
            if (payable != null) {
                payable.setTotalAmount(BigDecimal.ZERO);
                payable.setPaidAmount(BigDecimal.ZERO);
                payable.setUnpaidAmount(BigDecimal.ZERO);
                payable.setStatus("RED_FLUSHED");
                payable.setRemark(appendRedFlushReason(payable.getRemark(), reason));
                payable.setUpdatedAt(Instant.now());
                erpAccountsPayableMapper.updateById(payable);
            }
        } else if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿或已审核状态可作废");
        }
        order.setStatus(STATUS_CANCELLED);
        order.setCancelledBy(resolveCurrentUsername());
        order.setCancelledAt(Instant.now());
        order.setRemark(appendRedFlushReason(order.getRemark(), reason));
        order.setUpdatedAt(Instant.now());
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

    private List<ErpPurchaseOrderItem> buildItems(Long tenantId, Long orderId, List<ErpPurchaseOrderItemRequest> requests) {
        List<ErpPurchaseOrderItem> items = new ArrayList<>();
        int index = 1;
        for (ErpPurchaseOrderItemRequest request : requests) {
            ErpProduct product = erpProductMapper.selectOne(new QueryWrapper<ErpProduct>()
                .eq("tenant_id", tenantId)
                .eq("id", request.productId()));
            if (product == null) {
                throw new IllegalArgumentException("商品不存在");
            }
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
            payable.setStatus("OPEN");
            payable.setSettlementMethod(null);
            payable.setRemark("采购单审核生成应付单");
            payable.setCreatedAt(Instant.now());
            payable.setUpdatedAt(Instant.now());
            erpAccountsPayableMapper.insert(payable);
        } else {
            BigDecimal totalAmount = resolvePayableTotal(order);
            BigDecimal paidAmount = existing.getPaidAmount() == null ? BigDecimal.ZERO : existing.getPaidAmount();
            existing.setTotalAmount(totalAmount);
            existing.setUnpaidAmount(totalAmount.subtract(paidAmount).max(BigDecimal.ZERO));
            if (existing.getUnpaidAmount().compareTo(BigDecimal.ZERO) == 0) {
                existing.setStatus("APPROVED");
            } else {
                existing.setStatus("OPEN");
            }
            existing.setUpdatedAt(Instant.now());
            erpAccountsPayableMapper.updateById(existing);
        }
    }

    private void ensurePayableAndPayment(Long tenantId, ErpPurchaseOrder order) {
        BigDecimal total = resolvePayableTotal(order);
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

        BigDecimal unpaid = total.subtract(totalApplied);
        if (unpaid.compareTo(BigDecimal.ZERO) < 0) {
            unpaid = BigDecimal.ZERO;
        }

        ErpAccountsPayable payable = erpAccountsPayableMapper.findByPurchaseOrderId(tenantId, order.getId());
        if (payable == null) {
            payable = new ErpAccountsPayable();
            payable.setTenantId(tenantId);
            payable.setPurchaseOrderId(order.getId());
            payable.setOrderNo(order.getOrderNo());
            payable.setSupplierId(order.getSupplierId());
            payable.setTotalAmount(total);
            payable.setPaidAmount(paidCash);
            payable.setDiscountAmount(discount);
            payable.setUnpaidAmount(unpaid);
            payable.setStatus(totalApplied.compareTo(total) == 0 ? "SETTLED" : "OPEN");
            payable.setSettlementMethod(null);
            payable.setRemark("采购单审核生成应付单");
            payable.setCreatedAt(Instant.now());
            payable.setUpdatedAt(Instant.now());
            erpAccountsPayableMapper.insert(payable);
        } else {
            payable.setTotalAmount(total);
            payable.setPaidAmount(paidCash);
            payable.setDiscountAmount(discount);
            payable.setUnpaidAmount(unpaid);
            if (!"RED_FLUSHED".equals(payable.getStatus())) {
                payable.setStatus(totalApplied.compareTo(total) == 0 ? "SETTLED" : "OPEN");
            }
            payable.setUpdatedAt(Instant.now());
            erpAccountsPayableMapper.updateById(payable);
        }

        if (totalApplied.compareTo(BigDecimal.ZERO) > 0) {
            ErpPayment paymentExisting = erpPaymentMapper.findByPurchaseOrderId(tenantId, order.getId());
            if (paymentExisting == null) {
                ErpPayment payment = new ErpPayment();
                payment.setTenantId(tenantId);
                payment.setPayableId(payable.getId());
                payment.setPurchaseOrderId(order.getId());
                payment.setPaymentNo(generatePaymentNo(tenantId));
                payment.setSupplierId(order.getSupplierId());
                payment.setAmount(paidCash);
                payment.setDiscountAmount(discount);
                payment.setSettlementMethod(null);
                payment.setPaymentMethodCode(order.getPaymentMethodCode());
                payment.setStatus("APPROVED");
                payment.setPaidAt(Instant.now());
                payment.setRemark("采购单审核自动付款");
                payment.setCreatedAt(Instant.now());
                payment.setUpdatedAt(Instant.now());
                erpPaymentMapper.insert(payment);

                ErpPaymentPayable allocation = new ErpPaymentPayable();
                allocation.setTenantId(tenantId);
                allocation.setPaymentId(payment.getId());
                allocation.setPayableId(payable.getId());
                allocation.setAllocatedAmount(paidCash);
                allocation.setAllocatedDiscount(discount);
                allocation.setAllocatedTotal(totalApplied);
                allocation.setCreatedAt(Instant.now());
                erpPaymentPayableMapper.insert(allocation);
            }
        }
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
        ErpStockBalance balance = erpStockBalanceMapper.findByKey(tenantId, item.getProductId(), warehouseId, locationId);
        BigDecimal before = balance == null ? BigDecimal.ZERO : balance.getQtyOnHand();
        BigDecimal after = before.add(delta);
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("库存不足，无法完成操作");
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
        BigDecimal unitCost = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
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
            updateProductAvgCost(tenantId, productId, inboundQty, inboundUnitCost);
        }
    }

    private void updateProductAvgCost(Long tenantId, Long productId, BigDecimal inboundQty, BigDecimal inboundUnitCost) {
        if (productId == null || inboundQty == null || inboundUnitCost == null) {
            return;
        }
        if (inboundQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        ErpProduct product = erpProductMapper.selectOne(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", tenantId)
            .eq("id", productId));
        if (product == null) {
            return;
        }
        BigDecimal oldQty = erpStockBalanceMapper.sumQtyByProduct(tenantId, productId);
        if (oldQty == null) {
            oldQty = BigDecimal.ZERO;
        }
        BigDecimal oldCost = product.getCostPrice() == null ? BigDecimal.ZERO : product.getCostPrice();
        BigDecimal newQty = oldQty.add(inboundQty);
        if (newQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal totalCost = oldCost.multiply(oldQty).add(inboundUnitCost.multiply(inboundQty));
        BigDecimal newCost = totalCost.divide(newQty, 4, RoundingMode.HALF_UP);
        product.setCostPrice(newCost);
        erpProductMapper.updateById(product);
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
        SystemConfig config = systemConfigMapper.findByKey(key);
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
