package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSaleReturnCreateRequest;
import com.example.wms.dto.erp.ErpSaleReturnDetail;
import com.example.wms.dto.erp.ErpSaleReturnItemRequest;
import com.example.wms.dto.erp.ErpSaleReturnUpdateRequest;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpAccountsReceivable;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpReceipt;
import com.example.wms.entity.erp.ErpReceiptReceivable;
import com.example.wms.entity.erp.ErpSaleReturn;
import com.example.wms.entity.erp.ErpSaleReturnItem;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockTxn;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpReceiptReceivableMapper;
import com.example.wms.mapper.erp.ErpSaleReturnItemMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.service.erp.ErpSaleReturnService;
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

// 销售退货服务实现（ERP进销存）
@Service
public class ErpSaleReturnServiceImpl implements ErpSaleReturnService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_RED_FLUSHED = "RED_FLUSHED";
    private static final String ORDER_TYPE = "SALE_RETURN";
    private static final String RECEIVABLE_ORDER_TYPE = "AR_RETURN";

    private static final String RETURN_RESTOCK = "RESTOCK";
    private static final String RETURN_SCRAP = "SCRAP";

    private final ErpSaleReturnMapper erpSaleReturnMapper;
    private final ErpSaleReturnItemMapper erpSaleReturnItemMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpStockBalanceMapper erpStockBalanceMapper;
    private final ErpStockTxnMapper erpStockTxnMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final ErpAccountsReceivableMapper erpAccountsReceivableMapper;
    private final ErpReceiptMapper erpReceiptMapper;
    private final ErpReceiptReceivableMapper erpReceiptReceivableMapper;
    private final SystemConfigMapper systemConfigMapper;

    public ErpSaleReturnServiceImpl(ErpSaleReturnMapper erpSaleReturnMapper,
                                    ErpSaleReturnItemMapper erpSaleReturnItemMapper,
                                    ErpProductMapper erpProductMapper,
                                    ErpStockBalanceMapper erpStockBalanceMapper,
                                    ErpStockTxnMapper erpStockTxnMapper,
                                    ErpOrderSequenceMapper erpOrderSequenceMapper,
                                    ErpAccountsReceivableMapper erpAccountsReceivableMapper,
                                    ErpReceiptMapper erpReceiptMapper,
                                    ErpReceiptReceivableMapper erpReceiptReceivableMapper,
                                    SystemConfigMapper systemConfigMapper) {
        this.erpSaleReturnMapper = erpSaleReturnMapper;
        this.erpSaleReturnItemMapper = erpSaleReturnItemMapper;
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
    public List<ErpSaleReturn> listAll(String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpSaleReturn> wrapper = baseWrapper(keyword, status, customerId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        return erpSaleReturnMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpSaleReturn> page(long page, long size, String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        Page<ErpSaleReturn> pageReq = Page.of(page, size);
        QueryWrapper<ErpSaleReturn> wrapper = baseWrapper(keyword, status, customerId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        Page<ErpSaleReturn> result = erpSaleReturnMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
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
        return new ErpSaleReturnDetail(order, items);
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
        ErpSaleReturn order = new ErpSaleReturn();
        order.setTenantId(tenantId);
        order.setOrderNo(ensureOrderNo(tenantId, request.orderNo()));
        order.setStatus(STATUS_DRAFT);
        order.setReturnType(resolveReturnType(request.returnType()));
        order.setCustomerId(request.customerId());
        order.setSaleOrderId(request.saleOrderId());
        order.setOrderAt(parseOrderAt(request.orderAt()));
        order.setSettlementMethod(request.settlementMethod());
        order.setPaidAmount(request.paidAmount());
        order.setDiscountAmount(request.discountAmount());
        order.setVersion(0L);
        order.setRemark(request.remark());
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        erpSaleReturnMapper.insert(order);

        List<ErpSaleReturnItem> items = buildItems(tenantId, order.getId(), request.items());
        for (ErpSaleReturnItem item : items) {
            erpSaleReturnItemMapper.insert(item);
        }
        applyTotals(order, items);
        erpSaleReturnMapper.updateById(order);

        return new ErpSaleReturnDetail(order, items);
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
        order.setReturnType(resolveReturnType(request.returnType()));
        order.setCustomerId(request.customerId());
        order.setSaleOrderId(request.saleOrderId());
        order.setOrderAt(parseOrderAt(request.orderAt()));
        order.setSettlementMethod(request.settlementMethod());
        order.setPaidAmount(request.paidAmount());
        order.setDiscountAmount(request.discountAmount());
        order.setRemark(request.remark());
        order.setUpdatedAt(Instant.now());

        erpSaleReturnItemMapper.delete(new QueryWrapper<ErpSaleReturnItem>()
            .eq("tenant_id", tenantId)
            .eq("return_id", id));
        List<ErpSaleReturnItem> items = buildItems(tenantId, id, request.items());
        for (ErpSaleReturnItem item : items) {
            erpSaleReturnItemMapper.insert(item);
        }
        applyTotals(order, items);
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
        order.setStatus(STATUS_APPROVED);
        order.setApprovedBy(resolveCurrentUsername());
        order.setApprovedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        updateWithVersion(tenantId, order);

        List<ErpSaleReturnItem> items = erpSaleReturnItemMapper.findByReturnId(tenantId, id);
        String returnType = resolveReturnType(order.getReturnType());
        for (ErpSaleReturnItem item : items) {
            if (item.getQty() == null) {
                throw new IllegalArgumentException("退货数量不能为空");
            }
            if (RETURN_RESTOCK.equals(returnType)) {
                applyStockDelta(tenantId, item, item.getQty(), "SALE_RETURN_RESTOCK", id, true);
            } else {
                applyStockDelta(tenantId, item, BigDecimal.ZERO, "SALE_RETURN_SCRAP", id, false);
            }
        }
        createReturnReceivable(tenantId, order, resolveReturnTotal(order));
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
        order.setStatus(STATUS_RED_FLUSHED);
        String originRemark = order.getRemark();
        if (originRemark == null || originRemark.isBlank()) {
            order.setRemark("红冲原因：" + reasonText);
        } else {
            order.setRemark(originRemark + " | 红冲原因：" + reasonText);
        }
        order.setUpdatedAt(Instant.now());
        updateWithVersion(tenantId, order);

        List<ErpSaleReturnItem> items = erpSaleReturnItemMapper.findByReturnId(tenantId, id);
        String returnType = resolveReturnType(order.getReturnType());
        for (ErpSaleReturnItem item : items) {
            if (item.getQty() == null) {
                throw new IllegalArgumentException("退货数量不能为空");
            }
            if (RETURN_RESTOCK.equals(returnType)) {
                applyStockDelta(tenantId, item, item.getQty().negate(), "SALE_RETURN_RED_FLUSH", id, true);
            } else {
                applyStockDelta(tenantId, item, BigDecimal.ZERO, "SALE_RETURN_RED_FLUSH", id, false);
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
            wrapper.ge("created_at", startAt);
        }
        if (endAt != null) {
            wrapper.le("created_at", endAt);
        }
        return wrapper;
    }

    private List<ErpSaleReturnItem> buildItems(Long tenantId, Long returnId, List<ErpSaleReturnItemRequest> requests) {
        List<ErpSaleReturnItem> items = new ArrayList<>();
        int index = 1;
        for (ErpSaleReturnItemRequest request : requests) {
            ErpProduct product = erpProductMapper.selectOne(new QueryWrapper<ErpProduct>()
                .eq("tenant_id", tenantId)
                .eq("id", request.productId()));
            if (product == null) {
                throw new IllegalArgumentException("商品不存在");
            }
            ErpSaleReturnItem item = new ErpSaleReturnItem();
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

    private void applyStockDelta(Long tenantId, ErpSaleReturnItem item, BigDecimal delta, String bizType, Long orderId, boolean updateBalance) {
        Long warehouseId = item.getWarehouseId();
        Long locationId = item.getLocationId();
        BigDecimal unitCost = getProductCost(tenantId, item.getProductId());
        if (updateBalance && delta.compareTo(BigDecimal.ZERO) > 0) {
            updateProductAvgCost(tenantId, item.getProductId(), delta, unitCost);
        }
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
        BigDecimal totalCost = unitCost.multiply(delta).setScale(4, RoundingMode.HALF_UP);
        txn.setUnitCost(unitCost);
        txn.setTotalCost(totalCost);
        txn.setOperator(resolveCurrentUsername());
        txn.setOperatorId(null);
        txn.setRemark(item.getRemark());
        txn.setCreatedAt(Instant.now());
        erpStockTxnMapper.insert(txn);
    }

    private void createReturnReceivable(Long tenantId, ErpSaleReturn order, BigDecimal delta) {
        if (order.getSaleOrderId() == null) {
            return;
        }
        BigDecimal amount = delta == null ? BigDecimal.ZERO : delta;
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        BigDecimal negative = amount.negate();
        ErpAccountsReceivable ar = new ErpAccountsReceivable();
        ar.setTenantId(tenantId);
        ar.setSaleOrderId(order.getSaleOrderId());
        ar.setOrderNo(generateReceivableNo(tenantId));
        ar.setCustomerId(order.getCustomerId());
        ar.setTotalAmount(negative);
        ar.setPaidAmount(BigDecimal.ZERO);
        ar.setUnpaidAmount(negative);
        ar.setStatus("OPEN");
        ar.setSettlementMethod(order.getSettlementMethod());
        ar.setRemark("销售退货单号:" + order.getOrderNo());
        ar.setCreatedAt(Instant.now());
        ar.setUpdatedAt(Instant.now());
        erpAccountsReceivableMapper.insert(ar);
    }

    private ErpAccountsReceivable findReturnReceivable(Long tenantId, ErpSaleReturn order) {
        if (order.getSaleOrderId() == null) {
            return null;
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
}
