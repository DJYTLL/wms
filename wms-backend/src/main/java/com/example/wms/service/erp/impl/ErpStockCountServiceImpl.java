package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockCountCreateRequest;
import com.example.wms.dto.erp.ErpStockCountDetail;
import com.example.wms.dto.erp.ErpStockCountItemRequest;
import com.example.wms.dto.erp.ErpStockCountUpdateRequest;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockCount;
import com.example.wms.entity.erp.ErpStockCountItem;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpStockTxn;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockCountItemMapper;
import com.example.wms.mapper.erp.ErpStockCountMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.ErpStockCountService;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// 库存盘点服务实现（ERP进销存）
@Service
public class ErpStockCountServiceImpl implements ErpStockCountService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_RED_FLUSHED = "RED_FLUSHED";
    private static final String TYPE_COUNT = "COUNT";
    private static final String TYPE_INIT = "INIT";
    private static final Set<String> ADJUSTMENT_REASONS = Set.of("PROFIT", "LOSS", "CORRECTION", "MIGRATION", "OTHER");

    private final ErpStockCountMapper erpStockCountMapper;
    private final ErpStockCountItemMapper erpStockCountItemMapper;
    private final ErpStockBalanceMapper erpStockBalanceMapper;
    private final ErpStockTxnMapper erpStockTxnMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpWarehouseMapper erpWarehouseMapper;
    private final ErpLocationMapper erpLocationMapper;
    private final ErpCostService erpCostService;

    public ErpStockCountServiceImpl(ErpStockCountMapper erpStockCountMapper,
                                    ErpStockCountItemMapper erpStockCountItemMapper,
                                    ErpStockBalanceMapper erpStockBalanceMapper,
                                    ErpStockTxnMapper erpStockTxnMapper,
                                    ErpOrderSequenceMapper erpOrderSequenceMapper,
                                    SystemConfigMapper systemConfigMapper,
                                    ErpProductMapper erpProductMapper,
                                    ErpWarehouseMapper erpWarehouseMapper,
                                    ErpLocationMapper erpLocationMapper,
                                    ErpCostService erpCostService) {
        this.erpStockCountMapper = erpStockCountMapper;
        this.erpStockCountItemMapper = erpStockCountItemMapper;
        this.erpStockBalanceMapper = erpStockBalanceMapper;
        this.erpStockTxnMapper = erpStockTxnMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.erpProductMapper = erpProductMapper;
        this.erpWarehouseMapper = erpWarehouseMapper;
        this.erpLocationMapper = erpLocationMapper;
        this.erpCostService = erpCostService;
    }

    @Override
    public List<ErpStockCount> listAll(String keyword, String status, String countType) {
        QueryWrapper<ErpStockCount> wrapper = baseWrapper(keyword, status, normalizeType(countType));
        wrapper.orderByDesc("created_at");
        return erpStockCountMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpStockCount> page(long page, long size, String keyword, String status, String countType) {
        Page<ErpStockCount> pageReq = Page.of(page, size);
        QueryWrapper<ErpStockCount> wrapper = baseWrapper(keyword, status, normalizeType(countType));
        wrapper.orderByDesc("created_at");
        Page<ErpStockCount> result = erpStockCountMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpStockCountDetail getDetail(Long id, String countType) {
        Long tenantId = TenantContext.requireTenantId();
        ErpStockCount count = requireCountByType(tenantId, id, countType, false);
        List<ErpStockCountItem> items = erpStockCountItemMapper.selectList(new QueryWrapper<ErpStockCountItem>()
            .eq("tenant_id", tenantId)
            .eq("count_id", id)
            .orderByAsc("line_no"));
        return new ErpStockCountDetail(count, items);
    }

    @Override
    public String nextCountNo(String countType) {
        Long tenantId = TenantContext.requireTenantId();
        return ensureCountNo(tenantId, null, normalizeType(countType));
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_STOCK_COUNT_CREATE", entityType = "erp_stock_count", entityId = "{result.count.id}", detail = "countNo={result.count.countNo}")
    public ErpStockCountDetail create(ErpStockCountCreateRequest request, String countType) {
        Long tenantId = TenantContext.requireTenantId();
        String type = normalizeType(countType == null ? request.countType() : countType);
        if (TYPE_INIT.equals(type) && hasActiveInit(tenantId)) {
            throw new IllegalArgumentException("初始库存仅允许创建一次");
        }
        validateCountRequest(request.items(), tenantId, Set.of(), TYPE_COUNT.equals(type));
        String countNo = ensureCountNo(tenantId, request.countNo(), type);
        ErpStockCount count = new ErpStockCount();
        count.setTenantId(tenantId);
        count.setCountNo(countNo);
        count.setCountType(type);
        count.setStatus(STATUS_DRAFT);
        count.setAdjustmentReason(normalizeAdjustmentReason(request.adjustmentReason(), TYPE_COUNT.equals(type)));
        count.setWarehouseId(request.warehouseId());
        count.setLocationId(request.locationId());
        count.setCountAt(parseInstant(request.countAt()));
        count.setRemark(request.remark());
        count.setCreatedAt(Instant.now());
        count.setUpdatedAt(Instant.now());
        erpStockCountMapper.insert(count);

        List<ErpStockCountItem> items = buildItems(tenantId, count, request.items(), Set.of());
        for (ErpStockCountItem item : items) {
            erpStockCountItemMapper.insert(item);
        }
        return new ErpStockCountDetail(count, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_STOCK_COUNT_UPDATE", entityType = "erp_stock_count", entityId = "{arg0}", detail = "countNo={result.count.countNo}")
    public ErpStockCountDetail update(Long id, ErpStockCountUpdateRequest request, String countType) {
        Long tenantId = TenantContext.requireTenantId();
        ErpStockCount count = requireCountByType(tenantId, id, countType, true);
        if (!STATUS_DRAFT.equals(count.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可编辑");
        }
        Set<Long> allowedDisabledProductIds = existingProductIds(erpStockCountItemMapper.selectList(new QueryWrapper<ErpStockCountItem>()
            .eq("tenant_id", tenantId)
            .eq("count_id", id)
            .orderByAsc("line_no")));
        validateCountRequest(request.items(), tenantId, allowedDisabledProductIds, TYPE_COUNT.equals(count.getCountType()));
        count.setAdjustmentReason(normalizeAdjustmentReason(request.adjustmentReason(), TYPE_COUNT.equals(count.getCountType())));
        count.setWarehouseId(request.warehouseId());
        count.setLocationId(request.locationId());
        Instant countAt = parseInstant(request.countAt());
        count.setCountAt(countAt == null ? count.getCountAt() : countAt);
        count.setRemark(request.remark());
        count.setUpdatedAt(Instant.now());
        erpStockCountMapper.updateById(count);

        erpStockCountItemMapper.delete(new QueryWrapper<ErpStockCountItem>()
            .eq("tenant_id", tenantId)
            .eq("count_id", id));
        List<ErpStockCountItem> items = buildItems(tenantId, count, request.items(), allowedDisabledProductIds);
        for (ErpStockCountItem item : items) {
            erpStockCountItemMapper.insert(item);
        }
        return new ErpStockCountDetail(count, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_STOCK_COUNT_APPROVE", entityType = "erp_stock_count", entityId = "{arg0}")
    public void approve(Long id, String countType) {
        Long tenantId = TenantContext.requireTenantId();
        ErpStockCount count = requireCountByType(tenantId, id, countType, true);
        if (!STATUS_DRAFT.equals(count.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可审核");
        }
        if (isInit(count) && hasOtherApprovedInit(tenantId, count.getId())) {
            throw new IllegalArgumentException("初始库存仅允许创建一次");
        }
        List<ErpStockCountItem> items = erpStockCountItemMapper.selectList(new QueryWrapper<ErpStockCountItem>()
            .eq("tenant_id", tenantId)
            .eq("count_id", id)
            .orderByAsc("line_no"));
        String operator = resolveCurrentUsername();
        for (ErpStockCountItem item : items) {
            BigDecimal systemQty = resolveSystemQty(tenantId, item.getProductId(), item.getWarehouseId(), item.getLocationId());
            BigDecimal countedQty = item.getCountedQty() == null ? BigDecimal.ZERO : item.getCountedQty();
            BigDecimal delta = countedQty.subtract(systemQty);
            item.setSystemQty(systemQty);
            item.setDiffQty(delta);
            item.setUpdatedAt(Instant.now());
            erpStockCountItemMapper.updateById(item);
            if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if (isInit(count)) {
                applyStockDelta(tenantId, count, item, delta, operator, resolveBizType(count), "盘点调整");
            } else {
                applyCountAdjustmentDelta(tenantId, count, item, delta, operator);
            }
        }
        count.setStatus(STATUS_APPROVED);
        count.setApprovedBy(operator);
        count.setApprovedAt(Instant.now());
        count.setUpdatedAt(Instant.now());
        erpStockCountMapper.updateById(count);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_STOCK_INIT_RED_FLUSH", entityType = "erp_stock_count", entityId = "{arg0}")
    public void redFlush(Long id, String countType, String reason) {
        Long tenantId = TenantContext.requireTenantId();
        ErpStockCount count = requireCountByType(tenantId, id, countType, false);
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("红冲原因不能为空");
        }
        if (STATUS_RED_FLUSHED.equals(count.getStatus())) {
            return;
        }
        if (!STATUS_APPROVED.equals(count.getStatus())) {
            throw new IllegalArgumentException("仅已审核状态可红冲");
        }
        List<ErpStockCountItem> items = erpStockCountItemMapper.selectList(new QueryWrapper<ErpStockCountItem>()
            .eq("tenant_id", tenantId)
            .eq("count_id", id)
            .orderByAsc("line_no"));
        if (hasLegacyInitCostSnapshot(items)) {
            throw new IllegalArgumentException("老的初始库存单不允许红冲");
        }
        if (count.getApprovedAt() == null || erpStockTxnMapper.existsLaterTxnForInit(tenantId, count.getId(), count.getApprovedAt())) {
            throw new IllegalArgumentException("初始库存单已有后续库存业务，不能红冲");
        }
        String operator = resolveCurrentUsername();
        for (ErpStockCountItem item : items) {
            BigDecimal delta = item.getDiffQty();
            if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if (delta.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal unitCost = resolveInitUnitCost(item);
                erpCostService.reverseInboundAverageCost(tenantId, item.getProductId(), delta, unitCost);
            }
            applyStockDelta(
                tenantId,
                count,
                item,
                delta.negate(),
                operator,
                "STOCK_INIT_RED_FLUSH",
                appendRedFlushReason("初始库存红冲", reason),
                resolveInitUnitCost(item)
            );
        }
        count.setStatus(STATUS_RED_FLUSHED);
        count.setCancelledBy(operator);
        count.setCancelledAt(Instant.now());
        count.setRemark(appendRedFlushReason(count.getRemark(), reason));
        count.setUpdatedAt(Instant.now());
        erpStockCountMapper.updateById(count);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_STOCK_COUNT_CANCEL", entityType = "erp_stock_count", entityId = "{arg0}")
    public void cancel(Long id, String countType) {
        Long tenantId = TenantContext.requireTenantId();
        ErpStockCount count = requireCountByType(tenantId, id, countType, true);
        if (STATUS_CANCELLED.equals(count.getStatus())) {
            return;
        }
        if (STATUS_APPROVED.equals(count.getStatus())) {
            throw new IllegalArgumentException("已审核的盘点单不可作废");
        }
        count.setStatus(STATUS_CANCELLED);
        count.setCancelledBy(resolveCurrentUsername());
        count.setCancelledAt(Instant.now());
        count.setUpdatedAt(Instant.now());
        erpStockCountMapper.updateById(count);
    }

    private QueryWrapper<ErpStockCount> baseWrapper(String keyword, String status, String countType) {
        QueryWrapper<ErpStockCount> wrapper = new QueryWrapper<ErpStockCount>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (countType != null) {
            wrapper.eq("count_type", countType);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("count_no", keyword));
        }
        return wrapper;
    }

    private List<ErpStockCountItem> buildItems(Long tenantId,
                                               ErpStockCount count,
                                               List<ErpStockCountItemRequest> requests,
                                               Set<Long> allowedDisabledProductIds) {
        List<ErpStockCountItem> items = new ArrayList<>();
        Set<String> lineKeys = new HashSet<>();
        int lineNo = 1;
        for (ErpStockCountItemRequest request : requests) {
            requireUsableProduct(tenantId, request.productId(), allowedDisabledProductIds);
            if (TYPE_COUNT.equals(count.getCountType())) {
                validateAdjustmentScope(tenantId, request.warehouseId(), request.locationId());
            }
            Long warehouseId = request.warehouseId() != null ? request.warehouseId() : count.getWarehouseId();
            Long locationId = request.locationId() != null ? request.locationId() : count.getLocationId();
            String lineKey = request.productId() + "|" + String.valueOf(warehouseId) + "|" + String.valueOf(locationId);
            if (!lineKeys.add(lineKey)) {
                throw new IllegalArgumentException("同一商品、仓库、库位不能重复录入");
            }
            BigDecimal systemQty = resolveSystemQty(tenantId, request.productId(), warehouseId, locationId);
            BigDecimal countedQty = request.countedQty() == null ? BigDecimal.ZERO : request.countedQty();
            BigDecimal diffQty = countedQty.subtract(systemQty);

            ErpStockCountItem item = new ErpStockCountItem();
            item.setTenantId(tenantId);
            item.setCountId(count.getId());
            item.setLineNo(lineNo++);
            item.setProductId(request.productId());
            item.setWarehouseId(warehouseId);
            item.setLocationId(locationId);
            item.setSystemQty(systemQty);
            item.setCountedQty(countedQty);
            item.setInitUnitCost(resolveInitUnitCost(request));
            item.setInitTotalAmount(resolveInitTotalAmount(request, countedQty, item.getInitUnitCost()));
            item.setDiffQty(diffQty);
            item.setRemark(request.remark());
            item.setCreatedAt(Instant.now());
            item.setUpdatedAt(Instant.now());
            items.add(item);
        }
        return items;
    }

    private BigDecimal resolveSystemQty(Long tenantId, Long productId, Long warehouseId, Long locationId) {
        ErpStockBalance balance = erpStockBalanceMapper.findByKey(tenantId, productId, warehouseId, locationId);
        return balance == null ? BigDecimal.ZERO : balance.getQtyOnHand();
    }

    private void validateCountRequest(List<ErpStockCountItemRequest> requests,
                                      Long tenantId,
                                      Set<Long> allowedDisabledProductIds,
                                      boolean strictAdjustmentValidation) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("盘点明细不能为空");
        }
        for (ErpStockCountItemRequest request : requests) {
            if (request == null || request.productId() == null) {
                throw new IllegalArgumentException("盘点商品不能为空");
            }
            requireUsableProduct(tenantId, request.productId(), allowedDisabledProductIds);
            if (strictAdjustmentValidation) {
                if (request.warehouseId() == null) {
                    throw new IllegalArgumentException("调整仓库不能为空");
                }
                validateAdjustmentScope(tenantId, request.warehouseId(), request.locationId());
            }
            if (request.countedQty() == null) {
                throw new IllegalArgumentException("盘点数量不能为空");
            }
            if (request.countedQty().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("盘点数量不能小于 0");
            }
            if (!strictAdjustmentValidation) {
                if (request.initUnitCost() == null && request.initTotalAmount() == null) {
                    throw new IllegalArgumentException("初始库存必须填写期初单价或期初金额");
                }
                if (request.initUnitCost() != null && request.initUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("期初单价不能小于 0");
                }
                if (request.initTotalAmount() != null && request.initTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("期初金额不能小于 0");
                }
            }
        }
    }

    private String normalizeAdjustmentReason(String reason, boolean required) {
        String normalized = reason == null ? "" : reason.trim().toUpperCase();
        if (!required) {
            return normalized.isEmpty() ? null : normalized;
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("库存调整原因不能为空");
        }
        if (!ADJUSTMENT_REASONS.contains(normalized)) {
            throw new IllegalArgumentException("库存调整原因不合法，可选值为：" + String.join(", ", ADJUSTMENT_REASONS));
        }
        return normalized;
    }

    private void validateAdjustmentScope(Long tenantId, Long warehouseId, Long locationId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("调整仓库不能为空");
        }
        ErpWarehouse warehouse = erpWarehouseMapper.findActiveById(tenantId, warehouseId);
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        if (Boolean.FALSE.equals(warehouse.getEnabled())) {
            throw new IllegalArgumentException("仓库已停用，不能新增引用");
        }
        if (locationId == null) {
            return;
        }
        ErpLocation location = erpLocationMapper.findActiveById(tenantId, locationId);
        if (location == null) {
            throw new IllegalArgumentException("库位不存在");
        }
        if (Boolean.FALSE.equals(location.getEnabled())) {
            throw new IllegalArgumentException("库位已停用，不能新增引用");
        }
        if (!warehouseId.equals(location.getWarehouseId())) {
            throw new IllegalArgumentException("库位不属于所选仓库");
        }
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

    private Set<Long> existingProductIds(List<ErpStockCountItem> items) {
        Set<Long> ids = new HashSet<>();
        if (items == null) {
            return ids;
        }
        for (ErpStockCountItem item : items) {
            if (item != null && item.getProductId() != null) {
                ids.add(item.getProductId());
            }
        }
        return ids;
    }

    private void applyStockDelta(Long tenantId,
                                 ErpStockCount count,
                                 ErpStockCountItem item,
                                 BigDecimal delta,
                                 String operator,
                                 String bizType,
                                 String remark) {
        applyStockDelta(tenantId, count, item, delta, operator, bizType, remark, resolveTxnUnitCost(tenantId, count, item));
    }

    private void applyStockDelta(Long tenantId,
                                 ErpStockCount count,
                                 ErpStockCountItem item,
                                 BigDecimal delta,
                                 String operator,
                                 String bizType,
                                 String remark,
                                 BigDecimal unitCost) {
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            erpCostService.applyInboundAverageCost(tenantId, item.getProductId(), delta, unitCost);
        }
        ErpStockBalance balance = erpStockBalanceMapper.findByKey(tenantId, item.getProductId(), item.getWarehouseId(), item.getLocationId());
        BigDecimal before = balance == null ? BigDecimal.ZERO : balance.getQtyOnHand();
        BigDecimal after = before.add(delta);
        if (balance == null) {
            balance = new ErpStockBalance();
            balance.setTenantId(tenantId);
            balance.setProductId(item.getProductId());
            balance.setWarehouseId(item.getWarehouseId());
            balance.setLocationId(item.getLocationId());
            balance.setQtyOnHand(after);
            balance.setUpdatedBy(operator);
            balance.setUpdatedAt(Instant.now());
            erpStockBalanceMapper.insert(balance);
        } else {
            balance.setQtyOnHand(after);
            balance.setUpdatedBy(operator);
            balance.setUpdatedAt(Instant.now());
            erpStockBalanceMapper.updateById(balance);
        }

        ErpStockTxn txn = new ErpStockTxn();
        txn.setTenantId(tenantId);
        txn.setTxnNo(buildTxnNo(count, item, bizType));
        txn.setBizType(bizType);
        txn.setBizId(count.getId());
        txn.setBizItemId(item.getId());
        txn.setProductId(item.getProductId());
        txn.setWarehouseId(item.getWarehouseId());
        txn.setLocationId(item.getLocationId());
        txn.setQtyDelta(delta);
        txn.setQtyBefore(before);
        txn.setQtyAfter(after);
        BigDecimal totalCost = unitCost.multiply(delta).setScale(4, RoundingMode.HALF_UP);
        txn.setUnitCost(unitCost);
        txn.setTotalCost(totalCost);
        txn.setOperator(operator);
        txn.setOperatorId(null);
        txn.setRemark(remark);
        txn.setCreatedAt(Instant.now());
        erpStockTxnMapper.insert(txn);
    }

    private void applyCountAdjustmentDelta(Long tenantId,
                                           ErpStockCount count,
                                           ErpStockCountItem item,
                                           BigDecimal delta,
                                           String operator) {
        BigDecimal unitCost = getProductCost(tenantId, item.getProductId());
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            erpCostService.applyInboundAverageCost(tenantId, item.getProductId(), delta, unitCost);
        }

        ErpStockBalance balance;
        BigDecimal before;
        BigDecimal after;
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            balance = erpStockBalanceMapper.upsertAddQty(
                tenantId,
                item.getProductId(),
                item.getWarehouseId(),
                item.getLocationId(),
                delta,
                operator
            );
            after = balance == null || balance.getQtyOnHand() == null ? delta : balance.getQtyOnHand();
            before = after.subtract(delta);
        } else {
            balance = erpStockBalanceMapper.addQtyIfEnough(
                tenantId,
                item.getProductId(),
                item.getWarehouseId(),
                item.getLocationId(),
                delta,
                operator
            );
            if (balance == null || balance.getQtyOnHand() == null) {
                throw new IllegalArgumentException("调整后库存不能小于 0");
            }
            after = balance.getQtyOnHand();
            before = after.subtract(delta);
        }

        ErpStockTxn txn = new ErpStockTxn();
        txn.setTenantId(tenantId);
        txn.setTxnNo(buildTxnNo(count, item, "STOCK_COUNT"));
        txn.setBizType("STOCK_COUNT");
        txn.setBizId(count.getId());
        txn.setBizItemId(item.getId());
        txn.setProductId(item.getProductId());
        txn.setWarehouseId(item.getWarehouseId());
        txn.setLocationId(item.getLocationId());
        txn.setQtyDelta(delta);
        txn.setQtyBefore(before);
        txn.setQtyAfter(after);
        BigDecimal totalCost = unitCost.multiply(delta).setScale(4, RoundingMode.HALF_UP);
        txn.setUnitCost(unitCost);
        txn.setTotalCost(totalCost);
        txn.setOperator(operator);
        txn.setOperatorId(null);
        txn.setRemark("库存调整");
        txn.setCreatedAt(Instant.now());
        erpStockTxnMapper.insert(txn);
    }

    private String normalizeType(String countType) {
        if (TYPE_INIT.equalsIgnoreCase(countType)) {
            return TYPE_INIT;
        }
        return TYPE_COUNT;
    }

    private boolean isInit(ErpStockCount count) {
        return TYPE_INIT.equals(count.getCountType());
    }

    private BigDecimal resolveTxnUnitCost(Long tenantId, ErpStockCount count, ErpStockCountItem item) {
        if (count != null && isInit(count)) {
            return resolveInitUnitCost(item);
        }
        return getProductCost(tenantId, item.getProductId());
    }

    private boolean hasActiveInit(Long tenantId) {
        Long count = erpStockCountMapper.selectCount(new QueryWrapper<ErpStockCount>()
            .eq("tenant_id", tenantId)
            .eq("count_type", TYPE_INIT)
            .eq("status", STATUS_APPROVED));
        return count != null && count > 0;
    }

    private boolean hasOtherApprovedInit(Long tenantId, Long currentId) {
        QueryWrapper<ErpStockCount> wrapper = new QueryWrapper<ErpStockCount>()
            .eq("tenant_id", tenantId)
            .eq("count_type", TYPE_INIT)
            .eq("status", STATUS_APPROVED);
        if (currentId != null) {
            wrapper.ne("id", currentId);
        }
        Long count = erpStockCountMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private ErpStockCount requireCountByType(Long tenantId, Long id, String countType, boolean forUpdate) {
        ErpStockCount count = forUpdate
            ? erpStockCountMapper.findByIdForUpdate(tenantId, id)
            : erpStockCountMapper.selectOne(new QueryWrapper<ErpStockCount>()
                .eq("tenant_id", tenantId)
                .eq("id", id));
        if (count == null) {
            throw new IllegalArgumentException("单据不存在");
        }
        String expectedType = normalizeType(countType);
        if (!expectedType.equals(count.getCountType())) {
            throw new IllegalArgumentException(TYPE_INIT.equals(expectedType) ? "初始库存单不存在" : "盘点单不存在");
        }
        return count;
    }

    private String resolveBizType(ErpStockCount count) {
        return isInit(count) ? "STOCK_INIT" : "STOCK_COUNT";
    }

    private String buildTxnNo(ErpStockCount count, ErpStockCountItem item, String bizType) {
        String base = count.getCountNo() + "-" + item.getLineNo();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return base + "-" + suffix;
    }

    private Instant parseInstant(String value) {
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
        return java.time.LocalDateTime.parse(trimmed, formatter)
            .atZone(ZoneId.systemDefault())
            .toInstant();
    }

    private BigDecimal getProductCost(Long tenantId, Long productId) {
        return erpCostService.getProductCost(tenantId, productId);
    }

    private String ensureCountNo(Long tenantId, String provided, String countType) {
        String trimmed = provided == null ? "" : provided.trim();
        if (!trimmed.isEmpty()) {
            ErpStockCount existing = erpStockCountMapper.selectOne(new QueryWrapper<ErpStockCount>()
                .eq("tenant_id", tenantId)
                .eq("count_no", trimmed));
            if (existing != null) {
                throw new IllegalArgumentException("盘点单号已存在");
            }
            return trimmed;
        }
        String prefixKey = isInitType(countType) ? "erp.order.no.stock-init.prefix" : "erp.order.no.stock-count.prefix";
        String defaultPrefix = isInitType(countType) ? "SI" : "SC";
        String prefix = readConfig(prefixKey, defaultPrefix);
        String dateFormat = readConfig("erp.order.no.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.order.no.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        String orderType = isInitType(countType) ? "STOCK_INIT" : "STOCK_COUNT";
        erpOrderSequenceMapper.insertIgnore(tenantId, orderType, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, orderType, dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private boolean isInitType(String countType) {
        return TYPE_INIT.equalsIgnoreCase(countType);
    }

    private String readConfig(String key, String fallback) {
        SystemConfig config = systemConfigMapper.findByKey(key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return fallback;
        }
        return config.getConfigValue().trim();
    }

    private BigDecimal resolveInitUnitCost(ErpStockCountItemRequest request) {
        if (request.initUnitCost() != null) {
            return request.initUnitCost().setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal qty = request.countedQty() == null ? BigDecimal.ZERO : request.countedQty();
        BigDecimal amount = request.initTotalAmount() == null ? BigDecimal.ZERO : request.initTotalAmount();
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return amount.divide(qty, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveInitUnitCost(ErpStockCountItem item) {
        if (item == null || item.getInitUnitCost() == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return item.getInitUnitCost().setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveInitTotalAmount(ErpStockCountItemRequest request, BigDecimal countedQty, BigDecimal unitCost) {
        if (request.initTotalAmount() != null) {
            return request.initTotalAmount().setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal qty = countedQty == null ? BigDecimal.ZERO : countedQty;
        return unitCost.multiply(qty).setScale(4, RoundingMode.HALF_UP);
    }

    private boolean hasLegacyInitCostSnapshot(List<ErpStockCountItem> items) {
        return items.stream().anyMatch(item -> item.getInitUnitCost() == null || item.getInitTotalAmount() == null);
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

    private int readIntConfig(String key, int fallback) {
        String value = readConfig(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}
