package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPurchaseReturnCreateRequest;
import com.example.wms.dto.erp.ErpPurchaseReturnDetail;
import com.example.wms.dto.erp.ErpPurchaseReturnItemRequest;
import com.example.wms.dto.erp.ErpPurchaseReturnUpdateRequest;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpAccountsPayable;
import com.example.wms.entity.erp.ErpPayment;
import com.example.wms.entity.erp.ErpPaymentPayable;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.entity.erp.ErpPurchaseOrderItem;
import com.example.wms.entity.erp.ErpPurchaseReturn;
import com.example.wms.entity.erp.ErpPurchaseReturnItem;
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
import com.example.wms.mapper.erp.ErpPurchaseReturnItemMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.service.erp.ErpPurchaseReturnService;
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
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 采购退货服务实现（ERP进销存）
@Service
public class ErpPurchaseReturnServiceImpl implements ErpPurchaseReturnService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_RED_FLUSHED = "RED_FLUSHED";
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_SETTLED = "SETTLED";
    private static final String ORDER_TYPE = "PURCHASE_RETURN";
    private static final String PAYABLE_ORDER_TYPE = "AP_RETURN";
    private static final String PAYMENT_ORDER_TYPE = "PAYMENT";
    private static final String AUTO_RETURN_PAYMENT_REMARK = "采购退货审核自动退款/优惠";

    private static final String RETURN_GOODS = "RETURN";
    private static final String RETURN_SCRAP = "SCRAP";

    private final ErpPurchaseReturnMapper erpPurchaseReturnMapper;
    private final ErpPurchaseReturnItemMapper erpPurchaseReturnItemMapper;
    private final ErpPurchaseOrderMapper erpPurchaseOrderMapper;
    private final ErpPurchaseOrderItemMapper erpPurchaseOrderItemMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpStockBalanceMapper erpStockBalanceMapper;
    private final ErpStockTxnMapper erpStockTxnMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final ErpAccountsPayableMapper erpAccountsPayableMapper;
    private final ErpPaymentMapper erpPaymentMapper;
    private final ErpPaymentPayableMapper erpPaymentPayableMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ErpCostService erpCostService;

    public ErpPurchaseReturnServiceImpl(ErpPurchaseReturnMapper erpPurchaseReturnMapper,
                                        ErpPurchaseReturnItemMapper erpPurchaseReturnItemMapper,
                                        ErpPurchaseOrderMapper erpPurchaseOrderMapper,
                                        ErpPurchaseOrderItemMapper erpPurchaseOrderItemMapper,
                                        ErpProductMapper erpProductMapper,
                                        ErpStockBalanceMapper erpStockBalanceMapper,
                                        ErpStockTxnMapper erpStockTxnMapper,
                                        ErpOrderSequenceMapper erpOrderSequenceMapper,
                                        ErpAccountsPayableMapper erpAccountsPayableMapper,
                                        ErpPaymentMapper erpPaymentMapper,
                                        ErpPaymentPayableMapper erpPaymentPayableMapper,
                                        SystemConfigMapper systemConfigMapper,
                                        ErpCostService erpCostService) {
        this.erpPurchaseReturnMapper = erpPurchaseReturnMapper;
        this.erpPurchaseReturnItemMapper = erpPurchaseReturnItemMapper;
        this.erpPurchaseOrderMapper = erpPurchaseOrderMapper;
        this.erpPurchaseOrderItemMapper = erpPurchaseOrderItemMapper;
        this.erpProductMapper = erpProductMapper;
        this.erpStockBalanceMapper = erpStockBalanceMapper;
        this.erpStockTxnMapper = erpStockTxnMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.erpAccountsPayableMapper = erpAccountsPayableMapper;
        this.erpPaymentMapper = erpPaymentMapper;
        this.erpPaymentPayableMapper = erpPaymentPayableMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.erpCostService = erpCostService;
    }

    @Override
    public List<ErpPurchaseReturn> listAll(String keyword, String status, Long supplierId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpPurchaseReturn> wrapper = baseWrapper(keyword, status, supplierId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        return erpPurchaseReturnMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpPurchaseReturn> page(long page, long size, String keyword, String status, Long supplierId, Instant startAt, Instant endAt) {
        Page<ErpPurchaseReturn> pageReq = Page.of(page, size);
        QueryWrapper<ErpPurchaseReturn> wrapper = baseWrapper(keyword, status, supplierId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        Page<ErpPurchaseReturn> result = erpPurchaseReturnMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpPurchaseReturnDetail getDetail(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPurchaseReturn order = erpPurchaseReturnMapper.selectOne(new QueryWrapper<ErpPurchaseReturn>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (order == null) {
            throw new IllegalArgumentException("采购退货单不存在");
        }
        List<ErpPurchaseReturnItem> items = erpPurchaseReturnItemMapper.findByReturnId(tenantId, id);
        return new ErpPurchaseReturnDetail(order, items);
    }

    @Override
    public String nextOrderNo() {
        Long tenantId = TenantContext.requireTenantId();
        return ensureOrderNo(tenantId, null);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_PURCHASE_RETURN_CREATE", entityType = "erp_purchase_return", entityId = "{result.order.id}", detail = "orderNo={result.order.orderNo}")
    public ErpPurchaseReturnDetail create(ErpPurchaseReturnCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPurchaseReturn order = new ErpPurchaseReturn();
        order.setTenantId(tenantId);
        order.setOrderNo(ensureOrderNo(tenantId, request.orderNo()));
        order.setStatus(STATUS_DRAFT);
        order.setReturnType(resolveReturnType(request.returnType()));
        order.setSupplierId(request.supplierId());
        order.setPurchaseOrderId(request.purchaseOrderId());
        order.setOrderAt(parseOrderAt(request.orderAt()));
        order.setSettlementMethod(request.settlementMethod());
        order.setPaidAmount(normalizeAmount(request.paidAmount()));
        order.setDiscountAmount(normalizeAmount(request.discountAmount()));
        order.setVersion(0L);
        order.setRemark(request.remark());
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        erpPurchaseReturnMapper.insert(order);

        List<ErpPurchaseReturnItem> items = buildItems(tenantId, order.getId(), request.items(), Set.of());
        for (ErpPurchaseReturnItem item : items) {
            erpPurchaseReturnItemMapper.insert(item);
        }
        applyTotals(order, items);
        validatePurchaseReturnSourceFromRequest(tenantId, order.getPurchaseOrderId(), order.getSupplierId(), request.items(), null);
        validateSettlementAmounts(tenantId, order, null);
        erpPurchaseReturnMapper.updateById(order);
        return new ErpPurchaseReturnDetail(order, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_PURCHASE_RETURN_UPDATE", entityType = "erp_purchase_return", entityId = "{arg0}", detail = "orderNo={arg1.orderNo}")
    public ErpPurchaseReturnDetail update(Long id, ErpPurchaseReturnUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPurchaseReturn order = loadForUpdate(tenantId, id);
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿可编辑");
        }
        order.setReturnType(resolveReturnType(request.returnType()));
        order.setSupplierId(request.supplierId());
        order.setPurchaseOrderId(request.purchaseOrderId());
        order.setOrderAt(parseOrderAt(request.orderAt()));
        order.setSettlementMethod(request.settlementMethod());
        order.setPaidAmount(normalizeAmount(request.paidAmount()));
        order.setDiscountAmount(normalizeAmount(request.discountAmount()));
        order.setRemark(request.remark());
        order.setUpdatedAt(Instant.now());

        Set<Long> allowedDisabledProductIds = existingProductIds(erpPurchaseReturnItemMapper.findByReturnId(tenantId, id));

        erpPurchaseReturnItemMapper.delete(new QueryWrapper<ErpPurchaseReturnItem>()
            .eq("tenant_id", tenantId)
            .eq("return_id", id));
        List<ErpPurchaseReturnItem> items = buildItems(tenantId, id, request.items(), allowedDisabledProductIds);
        for (ErpPurchaseReturnItem item : items) {
            erpPurchaseReturnItemMapper.insert(item);
        }
        applyTotals(order, items);
        validatePurchaseReturnSourceFromRequest(tenantId, order.getPurchaseOrderId(), order.getSupplierId(), request.items(), id);
        validateSettlementAmounts(tenantId, order, id);
        updateWithVersion(tenantId, order);
        return new ErpPurchaseReturnDetail(order, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_PURCHASE_RETURN_DELETE", entityType = "erp_purchase_return", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPurchaseReturn order = loadForUpdate(tenantId, id);
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿可删除");
        }
        erpPurchaseReturnItemMapper.delete(new QueryWrapper<ErpPurchaseReturnItem>()
            .eq("tenant_id", tenantId)
            .eq("return_id", id));
        erpPurchaseReturnMapper.deleteById(id);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_PURCHASE_RETURN_APPROVE", entityType = "erp_purchase_return", entityId = "{arg0}")
    public void approve(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPurchaseReturn order = loadForUpdate(tenantId, id);
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿可审核");
        }
        List<ErpPurchaseReturnItem> items = erpPurchaseReturnItemMapper.findByReturnId(tenantId, id);
        validatePurchaseReturnSourceFromItems(tenantId, order.getPurchaseOrderId(), order.getSupplierId(), items, id);
        validateSettlementAmounts(tenantId, order, id);
        order.setStatus(STATUS_APPROVED);
        order.setApprovedBy(resolveCurrentUsername());
        order.setApprovedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        updateWithVersion(tenantId, order);

        String returnType = resolveReturnType(order.getReturnType());
        for (ErpPurchaseReturnItem item : items) {
            if (item.getQty() == null) {
                throw new IllegalArgumentException("退货数量不能为空");
            }
            if (RETURN_GOODS.equals(returnType)) {
                applyStockDelta(tenantId, item, item.getQty().negate(), "PURCHASE_RETURN", id, true);
            } else {
                applyStockDelta(tenantId, item, BigDecimal.ZERO, "PURCHASE_RETURN_SCRAP", id, false);
            }
        }
        createReturnPayable(tenantId, order, resolveReturnTotal(order));
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_PURCHASE_RETURN_RED_FLUSH", entityType = "erp_purchase_return", entityId = "{arg0}")
    public void cancel(Long id, String reason) {
        Long tenantId = TenantContext.requireTenantId();
        ErpPurchaseReturn order = loadForUpdate(tenantId, id);
        if (!STATUS_APPROVED.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅已审核状态可红冲");
        }
        String reasonText = reason == null ? "" : reason.trim();
        if (reasonText.isEmpty()) {
            throw new IllegalArgumentException("请填写红冲原因");
        }

        ErpAccountsPayable payable = erpAccountsPayableMapper.findByPurchaseReturnId(tenantId, order.getId());
        if (payable != null) {
            List<ErpPaymentPayable> allocations = erpPaymentPayableMapper.findByPayableId(tenantId, payable.getId());
            if (allocations != null && !allocations.isEmpty()) {
                List<Long> paymentIds = allocations.stream()
                    .map(ErpPaymentPayable::getPaymentId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
                if (!paymentIds.isEmpty()) {
                    List<ErpPayment> approvedPayments = erpPaymentMapper.selectList(new QueryWrapper<ErpPayment>()
                        .eq("tenant_id", tenantId)
                        .in("id", paymentIds)
                        .eq("status", STATUS_APPROVED));
                    boolean hasAppliedRefunds = approvedPayments.stream()
                        .map(payment -> resolvePaymentAppliedTotal(payment.getAmount(), payment.getDiscountAmount()))
                        .anyMatch(total -> total.compareTo(BigDecimal.ZERO) < 0);
                    if (hasAppliedRefunds) {
                        throw new IllegalArgumentException("请先红冲付款单");
                    }
                }
            }
        }

        order.setStatus(STATUS_RED_FLUSHED);
        order.setRemark(appendRedFlushReason(order.getRemark(), reasonText));
        order.setUpdatedAt(Instant.now());
        updateWithVersion(tenantId, order);

        List<ErpPurchaseReturnItem> items = erpPurchaseReturnItemMapper.findByReturnId(tenantId, id);
        String returnType = resolveReturnType(order.getReturnType());
        for (ErpPurchaseReturnItem item : items) {
            if (item.getQty() == null) {
                throw new IllegalArgumentException("退货数量不能为空");
            }
            if (RETURN_GOODS.equals(returnType)) {
                applyStockDelta(tenantId, item, item.getQty(), "PURCHASE_RETURN_RED_FLUSH", id, true);
            } else {
                applyStockDelta(tenantId, item, BigDecimal.ZERO, "PURCHASE_RETURN_RED_FLUSH", id, false);
            }
        }
        redFlushReturnPayable(tenantId, order, reasonText);
    }

    private QueryWrapper<ErpPurchaseReturn> baseWrapper(String keyword, String status, Long supplierId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpPurchaseReturn> wrapper = new QueryWrapper<ErpPurchaseReturn>()
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

    private List<ErpPurchaseReturnItem> buildItems(Long tenantId,
                                                   Long returnId,
                                                   List<ErpPurchaseReturnItemRequest> requests,
                                                   Set<Long> allowedDisabledProductIds) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("采购退货明细不能为空");
        }
        List<ErpPurchaseReturnItem> items = new ArrayList<>();
        int index = 1;
        for (ErpPurchaseReturnItemRequest request : requests) {
            if (request == null) {
                throw new IllegalArgumentException("采购退货明细不能为空");
            }
            validatePositiveQty(request.qty(), "退货数量必须大于0");
            ErpProduct product = requireUsableProduct(tenantId, request.productId(), allowedDisabledProductIds);
            ErpPurchaseReturnItem item = new ErpPurchaseReturnItem();
            item.setTenantId(tenantId);
            item.setReturnId(returnId);
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

    private Set<Long> existingProductIds(List<ErpPurchaseReturnItem> items) {
        Set<Long> ids = new HashSet<>();
        if (items == null) {
            return ids;
        }
        for (ErpPurchaseReturnItem item : items) {
            if (item != null && item.getProductId() != null) {
                ids.add(item.getProductId());
            }
        }
        return ids;
    }

    private void applyTotals(ErpPurchaseReturn order, List<ErpPurchaseReturnItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalExcl = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalIncl = BigDecimal.ZERO;
        for (ErpPurchaseReturnItem item : items) {
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

    private void validatePurchaseReturnSourceFromRequest(Long tenantId,
                                                         Long purchaseOrderId,
                                                         Long supplierId,
                                                         List<ErpPurchaseReturnItemRequest> requests,
                                                         Long currentReturnId) {
        if (purchaseOrderId == null) {
            throw new IllegalArgumentException("采购退货必须关联原采购单");
        }
        if (supplierId == null) {
            throw new IllegalArgumentException("采购退货必须选择供应商");
        }
        validatePurchaseReturnSource(
            tenantId,
            purchaseOrderId,
            supplierId,
            aggregateRequestedQty(requests),
            aggregateRequestedAmountInclTax(requests),
            currentReturnId
        );
    }

    private void validatePurchaseReturnSourceFromItems(Long tenantId,
                                                       Long purchaseOrderId,
                                                       Long supplierId,
                                                       List<ErpPurchaseReturnItem> items,
                                                       Long currentReturnId) {
        if (purchaseOrderId == null) {
            throw new IllegalArgumentException("采购退货必须关联原采购单");
        }
        if (supplierId == null) {
            throw new IllegalArgumentException("采购退货必须选择供应商");
        }
        validatePurchaseReturnSource(
            tenantId,
            purchaseOrderId,
            supplierId,
            aggregateExistingQty(items),
            aggregateExistingAmountInclTax(items),
            currentReturnId
        );
    }

    private void validatePurchaseReturnSource(Long tenantId,
                                              Long purchaseOrderId,
                                              Long supplierId,
                                              Map<Long, BigDecimal> requestQtyByProduct,
                                              Map<Long, BigDecimal> requestAmountByProduct,
                                              Long currentReturnId) {
        ErpPurchaseOrder purchaseOrder = loadApprovedPurchaseOrder(tenantId, purchaseOrderId);
        if (!supplierId.equals(purchaseOrder.getSupplierId())) {
            throw new IllegalArgumentException("退货供应商必须与原采购单供应商一致");
        }
        if (requestQtyByProduct.isEmpty()) {
            throw new IllegalArgumentException("采购退货明细不能为空");
        }

        List<ErpPurchaseOrderItem> purchaseItems = erpPurchaseOrderItemMapper.findByOrderId(tenantId, purchaseOrderId);
        Map<Long, BigDecimal> purchasedQtyByProduct = new HashMap<>();
        Map<Long, BigDecimal> purchasedAmountByProduct = new HashMap<>();
        for (ErpPurchaseOrderItem purchaseItem : purchaseItems) {
            if (purchaseItem.getProductId() == null || purchaseItem.getQty() == null) {
                continue;
            }
            purchasedQtyByProduct.merge(purchaseItem.getProductId(), purchaseItem.getQty(), BigDecimal::add);
            purchasedAmountByProduct.merge(
                purchaseItem.getProductId(),
                purchaseItem.getAmountInclTax() == null ? BigDecimal.ZERO : purchaseItem.getAmountInclTax(),
                BigDecimal::add
            );
        }

        Map<Long, BigDecimal> approvedReturnQtyByProduct = loadApprovedReturnQtyByProduct(tenantId, purchaseOrderId, currentReturnId);
        Map<Long, BigDecimal> approvedReturnAmountByProduct = loadApprovedReturnAmountByProduct(tenantId, purchaseOrderId, currentReturnId);
        for (Map.Entry<Long, BigDecimal> entry : requestQtyByProduct.entrySet()) {
            Long productId = entry.getKey();
            BigDecimal requestQty = entry.getValue();
            BigDecimal purchasedQty = purchasedQtyByProduct.get(productId);
            if (purchasedQty == null || purchasedQty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("退货商品必须存在于原采购单");
            }
            BigDecimal approvedQty = approvedReturnQtyByProduct.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal remainingQty = purchasedQty.subtract(approvedQty);
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("商品退货数量已达原采购上限");
            }
            if (requestQty.compareTo(remainingQty) > 0) {
                throw new IllegalArgumentException("商品退货数量不能超过原采购可退数量");
            }
            BigDecimal requestAmount = requestAmountByProduct.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal purchasedAmount = purchasedAmountByProduct.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal approvedAmount = approvedReturnAmountByProduct.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal remainingAmount = purchasedAmount.subtract(approvedAmount);
            if (requestAmount.compareTo(remainingAmount) > 0) {
                throw new IllegalArgumentException("商品退货金额不能超过原采购可退金额");
            }
            BigDecimal maxAmountByQty = purchasedAmount
                .divide(purchasedQty, 6, RoundingMode.HALF_UP)
                .multiply(requestQty)
                .setScale(2, RoundingMode.HALF_UP);
            if (requestAmount.compareTo(maxAmountByQty) > 0) {
                throw new IllegalArgumentException("商品退货单价不能高于原采购单价");
            }
        }
    }

    private ErpPurchaseOrder loadApprovedPurchaseOrder(Long tenantId, Long purchaseOrderId) {
        ErpPurchaseOrder purchaseOrder = erpPurchaseOrderMapper.selectOne(new QueryWrapper<ErpPurchaseOrder>()
            .eq("tenant_id", tenantId)
            .eq("id", purchaseOrderId));
        if (purchaseOrder == null) {
            throw new IllegalArgumentException("原采购单不存在");
        }
        if (!STATUS_APPROVED.equals(purchaseOrder.getStatus())) {
            throw new IllegalArgumentException("原采购单未审核，不能创建采购退货");
        }
        return purchaseOrder;
    }

    private Map<Long, BigDecimal> aggregateRequestedQty(List<ErpPurchaseReturnItemRequest> requests) {
        Map<Long, BigDecimal> qtyByProduct = new HashMap<>();
        if (requests == null) {
            return qtyByProduct;
        }
        for (ErpPurchaseReturnItemRequest request : requests) {
            if (request == null || request.productId() == null || request.qty() == null) {
                continue;
            }
            qtyByProduct.merge(request.productId(), request.qty(), BigDecimal::add);
        }
        return qtyByProduct;
    }

    private Map<Long, BigDecimal> aggregateRequestedAmountInclTax(List<ErpPurchaseReturnItemRequest> requests) {
        Map<Long, BigDecimal> amountByProduct = new HashMap<>();
        if (requests == null) {
            return amountByProduct;
        }
        for (ErpPurchaseReturnItemRequest request : requests) {
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

    private Map<Long, BigDecimal> aggregateExistingQty(List<ErpPurchaseReturnItem> items) {
        Map<Long, BigDecimal> qtyByProduct = new HashMap<>();
        if (items == null) {
            return qtyByProduct;
        }
        for (ErpPurchaseReturnItem item : items) {
            if (item == null || item.getProductId() == null || item.getQty() == null) {
                continue;
            }
            qtyByProduct.merge(item.getProductId(), item.getQty(), BigDecimal::add);
        }
        return qtyByProduct;
    }

    private Map<Long, BigDecimal> aggregateExistingAmountInclTax(List<ErpPurchaseReturnItem> items) {
        Map<Long, BigDecimal> amountByProduct = new HashMap<>();
        if (items == null) {
            return amountByProduct;
        }
        for (ErpPurchaseReturnItem item : items) {
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

    private Map<Long, BigDecimal> loadApprovedReturnQtyByProduct(Long tenantId, Long purchaseOrderId, Long currentReturnId) {
        List<ErpPurchaseReturn> approvedReturns = erpPurchaseReturnMapper.findApprovedByPurchaseOrderId(tenantId, purchaseOrderId);
        Map<Long, BigDecimal> qtyByProduct = new HashMap<>();
        if (approvedReturns == null || approvedReturns.isEmpty()) {
            return qtyByProduct;
        }
        for (ErpPurchaseReturn approvedReturn : approvedReturns) {
            if (currentReturnId != null && currentReturnId.equals(approvedReturn.getId())) {
                continue;
            }
            List<ErpPurchaseReturnItem> returnItems = erpPurchaseReturnItemMapper.findByReturnId(tenantId, approvedReturn.getId());
            for (ErpPurchaseReturnItem returnItem : returnItems) {
                if (returnItem.getProductId() == null || returnItem.getQty() == null) {
                    continue;
                }
                qtyByProduct.merge(returnItem.getProductId(), returnItem.getQty(), BigDecimal::add);
            }
        }
        return qtyByProduct;
    }

    private Map<Long, BigDecimal> loadApprovedReturnAmountByProduct(Long tenantId, Long purchaseOrderId, Long currentReturnId) {
        List<ErpPurchaseReturn> approvedReturns = erpPurchaseReturnMapper.findApprovedByPurchaseOrderId(tenantId, purchaseOrderId);
        Map<Long, BigDecimal> amountByProduct = new HashMap<>();
        if (approvedReturns == null || approvedReturns.isEmpty()) {
            return amountByProduct;
        }
        for (ErpPurchaseReturn approvedReturn : approvedReturns) {
            if (currentReturnId != null && currentReturnId.equals(approvedReturn.getId())) {
                continue;
            }
            List<ErpPurchaseReturnItem> returnItems = erpPurchaseReturnItemMapper.findByReturnId(tenantId, approvedReturn.getId());
            for (ErpPurchaseReturnItem returnItem : returnItems) {
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

    private void validateSettlementAmounts(Long tenantId, ErpPurchaseReturn order, Long currentReturnId) {
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
            BigDecimal refundableCash = calculateRefundableCash(tenantId, order.getPurchaseOrderId(), currentReturnId);
            if (paidAmount.compareTo(refundableCash) > 0) {
                throw new IllegalArgumentException("退款金额不能超过原采购可退实付金额");
            }
        }
    }

    private BigDecimal calculateRefundableCash(Long tenantId, Long purchaseOrderId, Long currentReturnId) {
        if (purchaseOrderId == null) {
            return BigDecimal.ZERO;
        }
        ErpAccountsPayable purchasePayable = erpAccountsPayableMapper.findByPurchaseOrderId(tenantId, purchaseOrderId);
        BigDecimal paidCash = BigDecimal.ZERO;
        if (purchasePayable != null && purchasePayable.getId() != null) {
            BigDecimal approvedPayments = erpPaymentPayableMapper.sumApprovedAllocatedAmountByPayableId(
                tenantId, purchasePayable.getId());
            if (approvedPayments != null && approvedPayments.compareTo(BigDecimal.ZERO) > 0) {
                paidCash = approvedPayments;
            }
        }
        List<ErpPurchaseReturn> approvedReturns = erpPurchaseReturnMapper.findApprovedByPurchaseOrderId(tenantId, purchaseOrderId);
        if (approvedReturns == null || approvedReturns.isEmpty()) {
            return paidCash.max(BigDecimal.ZERO);
        }
        for (ErpPurchaseReturn approvedReturn : approvedReturns) {
            if (currentReturnId != null && currentReturnId.equals(approvedReturn.getId())) {
                continue;
            }
            BigDecimal refundAmount = approvedReturn.getPaidAmount() == null ? BigDecimal.ZERO : approvedReturn.getPaidAmount();
            paidCash = paidCash.subtract(refundAmount);
        }
        return paidCash.max(BigDecimal.ZERO);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private void validatePositiveQty(BigDecimal qty, String message) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private ErpPurchaseReturn loadForUpdate(Long tenantId, Long id) {
        ErpPurchaseReturn order = erpPurchaseReturnMapper.selectOne(new QueryWrapper<ErpPurchaseReturn>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (order == null) {
            throw new IllegalArgumentException("采购退货单不存在");
        }
        return order;
    }

    private void updateWithVersion(Long tenantId, ErpPurchaseReturn order) {
        Long version = order.getVersion() == null ? 0L : order.getVersion();
        order.setVersion(version + 1);
        int updated = erpPurchaseReturnMapper.update(order, new QueryWrapper<ErpPurchaseReturn>()
            .eq("tenant_id", tenantId)
            .eq("id", order.getId())
            .eq("version", version));
        if (updated == 0) {
            throw new IllegalArgumentException("采购退货单已被修改，请刷新重试");
        }
    }

    private void applyStockDelta(Long tenantId, ErpPurchaseReturnItem item, BigDecimal delta, String bizType, Long orderId, boolean updateBalance) {
        Long warehouseId = item.getWarehouseId();
        Long locationId = item.getLocationId();
        String operator = resolveCurrentUsername();
        BigDecimal unitCost = resolveStockUnitCost(tenantId, item, bizType, orderId);
        if (updateBalance && delta.compareTo(BigDecimal.ZERO) > 0) {
            erpCostService.applyInboundAverageCost(tenantId, item.getProductId(), delta, unitCost);
        }
        BigDecimal before;
        BigDecimal after;
        if (updateBalance) {
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
            after = updatedBalance.getQtyOnHand() == null ? BigDecimal.ZERO : updatedBalance.getQtyOnHand();
            before = after.subtract(delta);
        } else {
            ErpStockBalance balance = erpStockBalanceMapper.findByKey(tenantId, item.getProductId(), warehouseId, locationId);
            before = balance == null || balance.getQtyOnHand() == null ? BigDecimal.ZERO : balance.getQtyOnHand();
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
        txn.setOperator(operator);
        txn.setOperatorId(null);
        txn.setRemark(item.getRemark());
        txn.setCreatedAt(Instant.now());
        erpStockTxnMapper.insert(txn);
    }

    private void createReturnPayable(Long tenantId, ErpPurchaseReturn order, BigDecimal delta) {
        BigDecimal amount = delta == null ? BigDecimal.ZERO : delta;
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        BigDecimal negative = amount.negate();
        BigDecimal refundAmount = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        BigDecimal discountAmount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        BigDecimal applied = refundAmount.add(discountAmount);
        BigDecimal negativeApplied = applied.negate();
        BigDecimal paidCash = refundAmount.negate();
        BigDecimal discount = discountAmount.negate();
        BigDecimal unpaid = negative.subtract(paidCash.add(discount));
        ErpAccountsPayable payable = erpAccountsPayableMapper.findByPurchaseReturnId(tenantId, order.getId());
        if (payable == null) {
            payable = new ErpAccountsPayable();
            payable.setTenantId(tenantId);
            payable.setPurchaseReturnId(order.getId());
            payable.setOrderNo(generatePayableNo(tenantId));
            payable.setSupplierId(order.getSupplierId());
            payable.setTotalAmount(negative);
            payable.setPaidAmount(paidCash);
            payable.setDiscountAmount(discount);
            payable.setUnpaidAmount(unpaid);
            payable.setStatus(unpaid.compareTo(BigDecimal.ZERO) == 0 ? STATUS_SETTLED : STATUS_OPEN);
            payable.setSettlementMethod(order.getSettlementMethod());
            payable.setRemark("采购退货单号:" + order.getOrderNo());
            payable.setCreatedAt(Instant.now());
            payable.setUpdatedAt(Instant.now());
            erpAccountsPayableMapper.insert(payable);
        } else {
            payable.setTotalAmount(negative);
            payable.setPaidAmount(paidCash);
            payable.setDiscountAmount(discount);
            payable.setUnpaidAmount(unpaid);
            payable.setStatus(unpaid.compareTo(BigDecimal.ZERO) == 0 ? STATUS_SETTLED : STATUS_OPEN);
            payable.setSettlementMethod(order.getSettlementMethod());
            if (payable.getOrderNo() == null || payable.getOrderNo().isBlank()) {
                payable.setOrderNo(generatePayableNo(tenantId));
            }
            payable.setUpdatedAt(Instant.now());
            erpAccountsPayableMapper.updateById(payable);
        }
        createAutoReturnPayment(tenantId, order, payable, refundAmount, discountAmount, negativeApplied);
    }

    private void createAutoReturnPayment(Long tenantId,
                                         ErpPurchaseReturn order,
                                         ErpAccountsPayable payable,
                                         BigDecimal refundAmount,
                                         BigDecimal discountAmount,
                                         BigDecimal negativeApplied) {
        if (negativeApplied.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        ErpPayment payment = new ErpPayment();
        payment.setTenantId(tenantId);
        payment.setPayableId(payable.getId());
        payment.setPurchaseOrderId(order.getPurchaseOrderId());
        payment.setPaymentNo(generatePaymentNo(tenantId));
        payment.setSupplierId(order.getSupplierId());
        payment.setAmount(refundAmount.negate());
        payment.setDiscountAmount(discountAmount.negate());
        payment.setSettlementMethod(order.getSettlementMethod());
        payment.setPaymentMethodCode(null);
        payment.setStatus(STATUS_APPROVED);
        payment.setPaidAt(Instant.now());
        payment.setRemark(AUTO_RETURN_PAYMENT_REMARK + ":" + order.getOrderNo());
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        erpPaymentMapper.insert(payment);

        ErpPaymentPayable allocation = new ErpPaymentPayable();
        allocation.setTenantId(tenantId);
        allocation.setPaymentId(payment.getId());
        allocation.setPayableId(payable.getId());
        allocation.setAllocatedAmount(refundAmount.negate());
        allocation.setAllocatedDiscount(discountAmount.negate());
        allocation.setAllocatedTotal(negativeApplied);
        allocation.setCreatedAt(Instant.now());
        erpPaymentPayableMapper.insert(allocation);
    }

    private void redFlushReturnPayable(Long tenantId, ErpPurchaseReturn order, String reason) {
        ErpAccountsPayable payable = erpAccountsPayableMapper.findByPurchaseReturnId(tenantId, order.getId());
        if (payable == null) {
            return;
        }
        payable.setTotalAmount(BigDecimal.ZERO);
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setDiscountAmount(BigDecimal.ZERO);
        payable.setUnpaidAmount(BigDecimal.ZERO);
        payable.setStatus(STATUS_RED_FLUSHED);
        payable.setRemark(appendRedFlushReason(payable.getRemark(), reason));
        payable.setUpdatedAt(Instant.now());
        erpAccountsPayableMapper.updateById(payable);
    }

    private BigDecimal resolveReturnTotal(ErpPurchaseReturn order) {
        if (order.getTotalAmountInclTax() != null) {
            return order.getTotalAmountInclTax();
        }
        return order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
    }

    private BigDecimal getProductCost(Long tenantId, Long productId) {
        return erpCostService.getProductCost(tenantId, productId);
    }

    private BigDecimal resolveStockUnitCost(Long tenantId, ErpPurchaseReturnItem item, String bizType, Long orderId) {
        if ("PURCHASE_RETURN_RED_FLUSH".equals(bizType) && orderId != null && item.getProductId() != null) {
            BigDecimal originalCost = erpStockTxnMapper.findPurchaseReturnIssueUnitCost(tenantId, orderId, item.getProductId());
            if (originalCost != null && originalCost.compareTo(BigDecimal.ZERO) > 0) {
                return originalCost;
            }
        }
        return getProductCost(tenantId, item.getProductId());
    }

    private String resolveReturnType(String type) {
        if (type == null || type.isBlank()) {
            return RETURN_GOODS;
        }
        String trimmed = type.trim().toUpperCase();
        if (RETURN_SCRAP.equals(trimmed)) {
            return RETURN_SCRAP;
        }
        return RETURN_GOODS;
    }

    private String ensureOrderNo(Long tenantId, String provided) {
        String trimmed = provided == null ? "" : provided.trim();
        if (!trimmed.isEmpty()) {
            ErpPurchaseReturn existing = erpPurchaseReturnMapper.findByOrderNo(tenantId, trimmed);
            if (existing != null) {
                throw new IllegalArgumentException("采购退货单号已存在");
            }
            return trimmed;
        }
        String prefix = readConfig("erp.order.no.purchase-return.prefix", "PR");
        String dateFormat = readConfig("erp.order.no.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.order.no.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, ORDER_TYPE, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, ORDER_TYPE, dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private String generatePayableNo(Long tenantId) {
        String prefix = readConfig("erp.order.no.ap-return.prefix", "AP");
        String dateFormat = readConfig("erp.order.no.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.order.no.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, PAYABLE_ORDER_TYPE, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, PAYABLE_ORDER_TYPE, dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
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
        } catch (Exception ex) {
            return fallback;
        }
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
        return base + " | " + append;
    }

    private BigDecimal resolvePaymentAppliedTotal(BigDecimal amount, BigDecimal discountAmount) {
        BigDecimal paid = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        return paid.add(discount);
    }
}
