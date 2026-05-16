package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockTransferCreateRequest;
import com.example.wms.dto.erp.ErpStockTransferDetail;
import com.example.wms.dto.erp.ErpStockTransferItemRequest;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockTransfer;
import com.example.wms.entity.erp.ErpStockTransferItem;
import com.example.wms.entity.erp.ErpStockTxn;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockTransferItemMapper;
import com.example.wms.mapper.erp.ErpStockTransferMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.ErpStockTransferService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ErpStockTransferServiceImpl implements ErpStockTransferService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String BIZ_TYPE_OUT = "STOCK_TRANSFER_OUT";
    private static final String BIZ_TYPE_IN = "STOCK_TRANSFER_IN";

    private final ErpStockTransferMapper erpStockTransferMapper;
    private final ErpStockTransferItemMapper erpStockTransferItemMapper;
    private final ErpStockBalanceMapper erpStockBalanceMapper;
    private final ErpStockTxnMapper erpStockTxnMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpWarehouseMapper erpWarehouseMapper;
    private final ErpLocationMapper erpLocationMapper;
    private final ErpCostService erpCostService;

    public ErpStockTransferServiceImpl(ErpStockTransferMapper erpStockTransferMapper,
                                       ErpStockTransferItemMapper erpStockTransferItemMapper,
                                       ErpStockBalanceMapper erpStockBalanceMapper,
                                       ErpStockTxnMapper erpStockTxnMapper,
                                       ErpOrderSequenceMapper erpOrderSequenceMapper,
                                       SystemConfigMapper systemConfigMapper,
                                       ErpProductMapper erpProductMapper,
                                       ErpWarehouseMapper erpWarehouseMapper,
                                       ErpLocationMapper erpLocationMapper,
                                       ErpCostService erpCostService) {
        this.erpStockTransferMapper = erpStockTransferMapper;
        this.erpStockTransferItemMapper = erpStockTransferItemMapper;
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
    public PageResponse<ErpStockTransfer> page(long page, long size, String keyword, String status, String startAt, String endAt) {
        Page<ErpStockTransfer> pageReq = Page.of(page, size);
        QueryWrapper<ErpStockTransfer> wrapper = new QueryWrapper<ErpStockTransfer>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .orderByDesc("transfer_at")
            .orderByDesc("created_at");
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("transfer_no", keyword).or().like("remark", keyword));
        }
        Instant start = parseInstant(startAt);
        Instant end = parseInstant(endAt);
        if (start != null) {
            wrapper.ge("transfer_at", start);
        }
        if (end != null) {
            wrapper.le("transfer_at", end);
        }
        Page<ErpStockTransfer> result = erpStockTransferMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpStockTransferDetail getDetail(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpStockTransfer transfer = erpStockTransferMapper.selectOne(new QueryWrapper<ErpStockTransfer>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (transfer == null) {
            throw new IllegalArgumentException("移库单不存在");
        }
        return new ErpStockTransferDetail(transfer, erpStockTransferItemMapper.findByTransferId(tenantId, id));
    }

    @Override
    public String nextTransferNo() {
        return ensureTransferNo(TenantContext.requireTenantId(), null);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_STOCK_TRANSFER_CREATE", entityType = "erp_stock_transfer", entityId = "{result.transfer.id}", detail = "transferNo={result.transfer.transferNo}")
    public ErpStockTransferDetail create(ErpStockTransferCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String operator = resolveCurrentUsername();
        validateRequest(tenantId, request.items(), Set.of());
        Instant now = Instant.now();

        ErpStockTransfer transfer = new ErpStockTransfer();
        transfer.setTenantId(tenantId);
        transfer.setTransferNo(ensureTransferNo(tenantId, request.transferNo()));
        transfer.setStatus(STATUS_DRAFT);
        transfer.setTransferAt(parseInstant(request.transferAt()) == null ? now : parseInstant(request.transferAt()));
        transfer.setPrintCount(0);
        transfer.setLastPrintedAt(null);
        transfer.setRemark(request.remark());
        transfer.setCreatedAt(now);
        transfer.setCreatedBy(operator);
        transfer.setUpdatedAt(now);
        transfer.setUpdatedBy(operator);
        erpStockTransferMapper.insert(transfer);

        List<ErpStockTransferItem> items = buildItems(tenantId, transfer.getId(), request.items(), now);
        for (ErpStockTransferItem item : items) {
            erpStockTransferItemMapper.insert(item);
        }
        return new ErpStockTransferDetail(transfer, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_STOCK_TRANSFER_UPDATE", entityType = "erp_stock_transfer", entityId = "{arg0}", detail = "transferNo={result.transfer.transferNo}")
    public ErpStockTransferDetail update(Long id, ErpStockTransferCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpStockTransfer transfer = requireTransferById(tenantId, id, true);
        if (!STATUS_DRAFT.equals(transfer.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可编辑");
        }
        Set<Long> allowedDisabledProductIds = existingProductIds(erpStockTransferItemMapper.findByTransferId(tenantId, id));
        validateRequest(tenantId, request.items(), allowedDisabledProductIds);
        transfer.setTransferAt(parseInstant(request.transferAt()) == null ? transfer.getTransferAt() : parseInstant(request.transferAt()));
        transfer.setRemark(request.remark());
        transfer.setUpdatedAt(Instant.now());
        transfer.setUpdatedBy(resolveCurrentUsername());
        erpStockTransferMapper.updateById(transfer);

        erpStockTransferItemMapper.delete(new QueryWrapper<ErpStockTransferItem>()
            .eq("tenant_id", tenantId)
            .eq("transfer_id", id));
        List<ErpStockTransferItem> items = buildItems(tenantId, id, request.items(), Instant.now(), allowedDisabledProductIds);
        for (ErpStockTransferItem item : items) {
            erpStockTransferItemMapper.insert(item);
        }
        return new ErpStockTransferDetail(transfer, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_STOCK_TRANSFER_APPROVE", entityType = "erp_stock_transfer", entityId = "{arg0}")
    public void approve(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpStockTransfer transfer = requireTransferById(tenantId, id, true);
        if (!STATUS_DRAFT.equals(transfer.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可审核");
        }
        List<ErpStockTransferItem> items = erpStockTransferItemMapper.findByTransferId(tenantId, id);
        validateRequest(tenantId, toItemRequests(items), existingProductIds(items));
        String operator = resolveCurrentUsername();
        for (ErpStockTransferItem item : items) {
            applyTransferItem(tenantId, transfer, item, operator);
        }
        transfer.setStatus(STATUS_APPROVED);
        transfer.setUpdatedAt(Instant.now());
        transfer.setUpdatedBy(operator);
        erpStockTransferMapper.updateById(transfer);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_STOCK_TRANSFER_CANCEL", entityType = "erp_stock_transfer", entityId = "{arg0}")
    public void cancel(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpStockTransfer transfer = requireTransferById(tenantId, id, true);
        if (STATUS_CANCELLED.equals(transfer.getStatus())) {
            return;
        }
        if (STATUS_APPROVED.equals(transfer.getStatus())) {
            throw new IllegalArgumentException("已审核的移库单不可作废");
        }
        transfer.setStatus(STATUS_CANCELLED);
        transfer.setUpdatedAt(Instant.now());
        transfer.setUpdatedBy(resolveCurrentUsername());
        erpStockTransferMapper.updateById(transfer);
    }

    private List<ErpStockTransferItem> buildItems(Long tenantId,
                                                  Long transferId,
                                                  List<ErpStockTransferItemRequest> requests,
                                                  Instant now) {
        return buildItems(tenantId, transferId, requests, now, Set.of());
    }

    private List<ErpStockTransferItem> buildItems(Long tenantId,
                                                  Long transferId,
                                                  List<ErpStockTransferItemRequest> requests,
                                                  Instant now,
                                                  Set<Long> allowedDisabledProductIds) {
        List<ErpStockTransferItem> items = new ArrayList<>();
        int lineNo = 1;
        for (ErpStockTransferItemRequest request : requests) {
            BigDecimal unitCost = erpCostService.getProductCost(tenantId, request.productId()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal qty = request.qty().setScale(4, RoundingMode.HALF_UP);

            ErpStockTransferItem item = new ErpStockTransferItem();
            item.setTenantId(tenantId);
            item.setTransferId(transferId);
            item.setLineNo(lineNo++);
            item.setProductId(request.productId());
            item.setFromWarehouseId(request.fromWarehouseId());
            item.setFromLocationId(normalizeLocationId(request.fromLocationId()));
            item.setToWarehouseId(request.toWarehouseId());
            item.setToLocationId(normalizeLocationId(request.toLocationId()));
            item.setQty(qty);
            item.setUnitCost(unitCost);
            item.setAmount(unitCost.multiply(qty).setScale(4, RoundingMode.HALF_UP));
            item.setRemark(request.remark());
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            requireUsableProduct(tenantId, request.productId(), allowedDisabledProductIds);
            items.add(item);
        }
        return items;
    }

    private void validateRequest(Long tenantId, List<ErpStockTransferItemRequest> requests, Set<Long> allowedDisabledProductIds) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("移库明细不能为空");
        }
        Set<String> seen = new HashSet<>();
        for (ErpStockTransferItemRequest request : requests) {
            if (request == null || request.productId() == null) {
                throw new IllegalArgumentException("移库商品不能为空");
            }
            if (request.qty() == null || request.qty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("移库数量必须大于 0");
            }
            requireUsableProduct(tenantId, request.productId(), allowedDisabledProductIds);
            validateScope(tenantId, request.fromWarehouseId(), request.fromLocationId(), "来源");
            validateScope(tenantId, request.toWarehouseId(), request.toLocationId(), "目标");

            Long fromLocationId = normalizeLocationId(request.fromLocationId());
            Long toLocationId = normalizeLocationId(request.toLocationId());
            if (request.fromWarehouseId().equals(request.toWarehouseId()) && sameNullableLong(fromLocationId, toLocationId)) {
                throw new IllegalArgumentException("来源位置和目标位置不能相同");
            }
            if (resolveAvailableQty(tenantId, request.productId(), request.fromWarehouseId(), fromLocationId).compareTo(request.qty()) < 0) {
                throw new IllegalArgumentException("来源位置库存不足");
            }

            String duplicateKey = request.productId() + "|" + request.fromWarehouseId() + "|" + fromLocationId + "|" + request.toWarehouseId() + "|" + toLocationId;
            if (!seen.add(duplicateKey)) {
                throw new IllegalArgumentException("同一商品的相同移库路径不能重复录入");
            }
        }
    }

    private List<ErpStockTransferItemRequest> toItemRequests(List<ErpStockTransferItem> items) {
        List<ErpStockTransferItemRequest> requests = new ArrayList<>();
        for (ErpStockTransferItem item : items) {
            requests.add(new ErpStockTransferItemRequest(
                item.getProductId(),
                item.getFromWarehouseId(),
                item.getFromLocationId(),
                item.getToWarehouseId(),
                item.getToLocationId(),
                item.getQty(),
                item.getRemark()
            ));
        }
        return requests;
    }

    private void applyTransferItem(Long tenantId, ErpStockTransfer transfer, ErpStockTransferItem item, String operator) {
        ErpStockBalance sourceBalance = erpStockBalanceMapper.addQtyIfEnoughAvailable(
            tenantId,
            item.getProductId(),
            item.getFromWarehouseId(),
            item.getFromLocationId(),
            item.getQty().negate(),
            operator
        );
        if (sourceBalance == null || sourceBalance.getQtyOnHand() == null) {
            throw new IllegalArgumentException("来源位置库存不足");
        }
        BigDecimal sourceAfter = sourceBalance.getQtyOnHand();
        BigDecimal sourceBefore = sourceAfter.add(item.getQty());
        recordTxn(tenantId, transfer, item, BIZ_TYPE_OUT, item.getFromWarehouseId(), item.getFromLocationId(), item.getQty().negate(), sourceBefore, sourceAfter, operator);

        ErpStockBalance targetBalance = erpStockBalanceMapper.upsertAddQty(
            tenantId,
            item.getProductId(),
            item.getToWarehouseId(),
            item.getToLocationId(),
            item.getQty(),
            operator
        );
        BigDecimal targetAfter = targetBalance == null || targetBalance.getQtyOnHand() == null ? item.getQty() : targetBalance.getQtyOnHand();
        BigDecimal targetBefore = targetAfter.subtract(item.getQty());
        recordTxn(tenantId, transfer, item, BIZ_TYPE_IN, item.getToWarehouseId(), item.getToLocationId(), item.getQty(), targetBefore, targetAfter, operator);
    }

    private void recordTxn(Long tenantId,
                           ErpStockTransfer transfer,
                           ErpStockTransferItem item,
                           String bizType,
                           Long warehouseId,
                           Long locationId,
                           BigDecimal qtyDelta,
                           BigDecimal qtyBefore,
                           BigDecimal qtyAfter,
                           String operator) {
        ErpStockTxn txn = new ErpStockTxn();
        txn.setTenantId(tenantId);
        txn.setTxnNo(buildTxnNo(transfer, item, bizType));
        txn.setBizType(bizType);
        txn.setBizId(transfer.getId());
        txn.setBizItemId(item.getId());
        txn.setProductId(item.getProductId());
        txn.setWarehouseId(warehouseId);
        txn.setLocationId(locationId);
        txn.setQtyDelta(qtyDelta);
        txn.setQtyBefore(qtyBefore);
        txn.setQtyAfter(qtyAfter);
        txn.setUnitCost(item.getUnitCost());
        txn.setTotalCost(item.getUnitCost().multiply(qtyDelta).setScale(4, RoundingMode.HALF_UP));
        txn.setOperator(operator);
        txn.setOperatorId(null);
        txn.setRemark("库存移库");
        txn.setCreatedAt(Instant.now());
        erpStockTxnMapper.insert(txn);
    }

    private void validateScope(Long tenantId, Long warehouseId, Long locationId, String scopeName) {
        if (warehouseId == null) {
            throw new IllegalArgumentException(scopeName + "仓库不能为空");
        }
        ErpWarehouse warehouse = erpWarehouseMapper.findActiveById(tenantId, warehouseId);
        if (warehouse == null) {
            throw new IllegalArgumentException(scopeName + "仓库不存在");
        }
        if (Boolean.FALSE.equals(warehouse.getEnabled())) {
            throw new IllegalArgumentException(scopeName + "仓库已停用");
        }
        Long normalizedLocationId = normalizeLocationId(locationId);
        if (normalizedLocationId == null) {
            return;
        }
        ErpLocation location = erpLocationMapper.findActiveById(tenantId, normalizedLocationId);
        if (location == null) {
            throw new IllegalArgumentException(scopeName + "库位不存在");
        }
        if (Boolean.FALSE.equals(location.getEnabled())) {
            throw new IllegalArgumentException(scopeName + "库位已停用");
        }
        if (!warehouseId.equals(location.getWarehouseId())) {
            throw new IllegalArgumentException(scopeName + "库位不属于所选仓库");
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

    private Set<Long> existingProductIds(List<ErpStockTransferItem> items) {
        Set<Long> ids = new HashSet<>();
        if (items == null) {
            return ids;
        }
        for (ErpStockTransferItem item : items) {
            if (item.getProductId() != null) {
                ids.add(item.getProductId());
            }
        }
        return ids;
    }

    private ErpStockTransfer requireTransferById(Long tenantId, Long id, boolean lock) {
        ErpStockTransfer transfer = lock
            ? erpStockTransferMapper.findByIdForUpdate(tenantId, id)
            : erpStockTransferMapper.selectOne(new QueryWrapper<ErpStockTransfer>()
                .eq("tenant_id", tenantId)
                .eq("id", id));
        if (transfer == null) {
            throw new IllegalArgumentException("移库单不存在");
        }
        return transfer;
    }

    private BigDecimal resolveAvailableQty(Long tenantId, Long productId, Long warehouseId, Long locationId) {
        ErpStockBalance balance = erpStockBalanceMapper.findByKey(tenantId, productId, warehouseId, locationId);
        if (balance == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal onHand = balance.getQtyOnHand() == null ? BigDecimal.ZERO : balance.getQtyOnHand();
        BigDecimal reserved = balance.getQtyLocked() == null ? BigDecimal.ZERO : balance.getQtyLocked();
        return onHand.subtract(reserved);
    }

    private String ensureTransferNo(Long tenantId, String provided) {
        String trimmed = provided == null ? "" : provided.trim();
        if (!trimmed.isEmpty()) {
            ErpStockTransfer existing = erpStockTransferMapper.findByTransferNo(tenantId, trimmed);
            if (existing != null) {
                throw new IllegalArgumentException("移库单号已存在");
            }
            return trimmed;
        }
        String prefix = readConfig("erp.order.no.stock-transfer.prefix", "ST");
        String dateFormat = readConfig("erp.order.no.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.order.no.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, "STOCK_TRANSFER", dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, "STOCK_TRANSFER", dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private String readConfig(String key, String fallback) {
        SystemConfig config = systemConfigMapper.findByKey(key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return fallback;
        }
        return config.getConfigValue().trim();
    }

    private int readIntConfig(String key, int fallback) {
        try {
            return Integer.parseInt(readConfig(key, String.valueOf(fallback)));
        } catch (NumberFormatException ex) {
            return fallback;
        }
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

    private String buildTxnNo(ErpStockTransfer transfer, ErpStockTransferItem item, String bizType) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return transfer.getTransferNo() + "-" + item.getLineNo() + "-" + bizType + "-" + suffix;
    }

    private Long normalizeLocationId(Long locationId) {
        return locationId == null || locationId <= 0 ? null : locationId;
    }

    private boolean sameNullableLong(Long left, Long right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}
