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
import com.example.wms.mapper.erp.ErpPurchaseReturnItemMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.service.erp.ErpPurchaseReturnService;
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

// 采购退货服务实现（ERP进销存）
@Service
public class ErpPurchaseReturnServiceImpl implements ErpPurchaseReturnService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_RED_FLUSHED = "RED_FLUSHED";
    private static final String ORDER_TYPE = "PURCHASE_RETURN";
    private static final String PAYABLE_ORDER_TYPE = "AP_RETURN";

    private static final String RETURN_GOODS = "RETURN";
    private static final String RETURN_SCRAP = "SCRAP";

    private final ErpPurchaseReturnMapper erpPurchaseReturnMapper;
    private final ErpPurchaseReturnItemMapper erpPurchaseReturnItemMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpStockBalanceMapper erpStockBalanceMapper;
    private final ErpStockTxnMapper erpStockTxnMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final ErpAccountsPayableMapper erpAccountsPayableMapper;
    private final ErpPaymentMapper erpPaymentMapper;
    private final ErpPaymentPayableMapper erpPaymentPayableMapper;
    private final SystemConfigMapper systemConfigMapper;

    public ErpPurchaseReturnServiceImpl(ErpPurchaseReturnMapper erpPurchaseReturnMapper,
                                        ErpPurchaseReturnItemMapper erpPurchaseReturnItemMapper,
                                        ErpProductMapper erpProductMapper,
                                        ErpStockBalanceMapper erpStockBalanceMapper,
                                        ErpStockTxnMapper erpStockTxnMapper,
                                        ErpOrderSequenceMapper erpOrderSequenceMapper,
                                        ErpAccountsPayableMapper erpAccountsPayableMapper,
                                        ErpPaymentMapper erpPaymentMapper,
                                        ErpPaymentPayableMapper erpPaymentPayableMapper,
                                        SystemConfigMapper systemConfigMapper) {
        this.erpPurchaseReturnMapper = erpPurchaseReturnMapper;
        this.erpPurchaseReturnItemMapper = erpPurchaseReturnItemMapper;
        this.erpProductMapper = erpProductMapper;
        this.erpStockBalanceMapper = erpStockBalanceMapper;
        this.erpStockTxnMapper = erpStockTxnMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.erpAccountsPayableMapper = erpAccountsPayableMapper;
        this.erpPaymentMapper = erpPaymentMapper;
        this.erpPaymentPayableMapper = erpPaymentPayableMapper;
        this.systemConfigMapper = systemConfigMapper;
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
        order.setPaidAmount(request.paidAmount());
        order.setDiscountAmount(request.discountAmount());
        order.setVersion(0L);
        order.setRemark(request.remark());
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        erpPurchaseReturnMapper.insert(order);

        List<ErpPurchaseReturnItem> items = buildItems(tenantId, order.getId(), request.items());
        for (ErpPurchaseReturnItem item : items) {
            erpPurchaseReturnItemMapper.insert(item);
        }
        applyTotals(order, items);
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
        order.setPaidAmount(request.paidAmount());
        order.setDiscountAmount(request.discountAmount());
        order.setRemark(request.remark());
        order.setUpdatedAt(Instant.now());

        erpPurchaseReturnItemMapper.delete(new QueryWrapper<ErpPurchaseReturnItem>()
            .eq("tenant_id", tenantId)
            .eq("return_id", id));
        List<ErpPurchaseReturnItem> items = buildItems(tenantId, id, request.items());
        for (ErpPurchaseReturnItem item : items) {
            erpPurchaseReturnItemMapper.insert(item);
        }
        applyTotals(order, items);
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
        order.setStatus(STATUS_APPROVED);
        order.setApprovedBy(resolveCurrentUsername());
        order.setApprovedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        updateWithVersion(tenantId, order);

        List<ErpPurchaseReturnItem> items = erpPurchaseReturnItemMapper.findByReturnId(tenantId, id);
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

    private List<ErpPurchaseReturnItem> buildItems(Long tenantId, Long returnId, List<ErpPurchaseReturnItemRequest> requests) {
        List<ErpPurchaseReturnItem> items = new ArrayList<>();
        int index = 1;
        for (ErpPurchaseReturnItemRequest request : requests) {
            ErpProduct product = erpProductMapper.selectOne(new QueryWrapper<ErpProduct>()
                .eq("tenant_id", tenantId)
                .eq("id", request.productId()));
            if (product == null) {
                throw new IllegalArgumentException("商品不存在");
            }
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
            if (request.qty() == null) {
                throw new IllegalArgumentException("退货数量不能为空");
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
        ErpStockBalance balance = erpStockBalanceMapper.findByKey(tenantId, item.getProductId(), warehouseId, locationId);
        BigDecimal before = balance == null ? BigDecimal.ZERO : balance.getQtyOnHand();
        BigDecimal after = before.add(delta);
        if (updateBalance && after.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal required = delta.abs();
            String productLabel = item.getProductName() == null ? item.getProductCode() : item.getProductName();
            throw new IllegalArgumentException(
                "库存不足，商品[" + productLabel + "] 可用=" + before + "，需求=" + required
            );
        }
        if (updateBalance) {
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
        } else {
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

    private void createReturnPayable(Long tenantId, ErpPurchaseReturn order, BigDecimal delta) {
        BigDecimal amount = delta == null ? BigDecimal.ZERO : delta;
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        BigDecimal negative = amount.negate();
        ErpAccountsPayable payable = erpAccountsPayableMapper.findByPurchaseReturnId(tenantId, order.getId());
        if (payable == null) {
            payable = new ErpAccountsPayable();
            payable.setTenantId(tenantId);
            payable.setPurchaseReturnId(order.getId());
            payable.setOrderNo(generatePayableNo(tenantId));
            payable.setSupplierId(order.getSupplierId());
            payable.setTotalAmount(negative);
            payable.setPaidAmount(BigDecimal.ZERO);
            payable.setDiscountAmount(BigDecimal.ZERO);
            payable.setUnpaidAmount(negative);
            payable.setStatus("OPEN");
            payable.setSettlementMethod(order.getSettlementMethod());
            payable.setRemark("采购退货单号:" + order.getOrderNo());
            payable.setCreatedAt(Instant.now());
            payable.setUpdatedAt(Instant.now());
            erpAccountsPayableMapper.insert(payable);
        } else {
            payable.setTotalAmount(negative);
            payable.setPaidAmount(BigDecimal.ZERO);
            payable.setDiscountAmount(BigDecimal.ZERO);
            payable.setUnpaidAmount(negative);
            payable.setStatus("OPEN");
            payable.setSettlementMethod(order.getSettlementMethod());
            if (payable.getOrderNo() == null || payable.getOrderNo().isBlank()) {
                payable.setOrderNo(generatePayableNo(tenantId));
            }
            payable.setUpdatedAt(Instant.now());
            erpAccountsPayableMapper.updateById(payable);
        }
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
