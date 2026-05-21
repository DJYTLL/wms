package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpAssemblyOrderCreateRequest;
import com.example.wms.dto.erp.ErpAssemblyOrderDetail;
import com.example.wms.dto.erp.ErpAssemblyOrderItemRequest;
import com.example.wms.dto.erp.ErpAssemblyOrderUpdateRequest;
import com.example.wms.dto.erp.ErpAssemblySourceSaleOrderDetail;
import com.example.wms.dto.erp.ErpAssemblySourceSaleOrderItem;
import com.example.wms.dto.erp.ErpAssemblySourceSaleOrderOption;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpAssemblyOrder;
import com.example.wms.entity.erp.ErpAssemblyOrderItem;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.entity.erp.ErpSaleOrderItem;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockTxn;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAssemblyOrderItemMapper;
import com.example.wms.mapper.erp.ErpAssemblyOrderMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpSaleOrderItemMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.ErpAssemblyOrderService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Assembly order service implementation
@Service
public class ErpAssemblyOrderServiceImpl implements ErpAssemblyOrderService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String TYPE_ASSEMBLE = "ASSEMBLE";
    private static final String TYPE_DISASSEMBLE = "DISASSEMBLE";
    private static final String ORDER_TYPE = "ASSEMBLY";
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_SALE_ORDER = "SALE_ORDER";
    private static final String SOURCE_STOCK_PREPARE = "STOCK_PREPARE";

    private final ErpAssemblyOrderMapper erpAssemblyOrderMapper;
    private final ErpAssemblyOrderItemMapper erpAssemblyOrderItemMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpSaleOrderMapper erpSaleOrderMapper;
    private final ErpSaleOrderItemMapper erpSaleOrderItemMapper;
    private final ErpCustomerMapper erpCustomerMapper;
    private final ErpWarehouseMapper erpWarehouseMapper;
    private final ErpLocationMapper erpLocationMapper;
    private final ErpStockBalanceMapper erpStockBalanceMapper;
    private final ErpStockTxnMapper erpStockTxnMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final ErpCostService erpCostService;

    public ErpAssemblyOrderServiceImpl(ErpAssemblyOrderMapper erpAssemblyOrderMapper,
                                       ErpAssemblyOrderItemMapper erpAssemblyOrderItemMapper,
                                       ErpProductMapper erpProductMapper,
                                       ErpSaleOrderMapper erpSaleOrderMapper,
                                       ErpSaleOrderItemMapper erpSaleOrderItemMapper,
                                       ErpCustomerMapper erpCustomerMapper,
                                       ErpWarehouseMapper erpWarehouseMapper,
                                       ErpLocationMapper erpLocationMapper,
                                       ErpStockBalanceMapper erpStockBalanceMapper,
                                       ErpStockTxnMapper erpStockTxnMapper,
                                       ErpOrderSequenceMapper erpOrderSequenceMapper,
                                       SystemConfigMapper systemConfigMapper,
                                       ErpCostService erpCostService) {
        this.erpAssemblyOrderMapper = erpAssemblyOrderMapper;
        this.erpAssemblyOrderItemMapper = erpAssemblyOrderItemMapper;
        this.erpProductMapper = erpProductMapper;
        this.erpSaleOrderMapper = erpSaleOrderMapper;
        this.erpSaleOrderItemMapper = erpSaleOrderItemMapper;
        this.erpCustomerMapper = erpCustomerMapper;
        this.erpWarehouseMapper = erpWarehouseMapper;
        this.erpLocationMapper = erpLocationMapper;
        this.erpStockBalanceMapper = erpStockBalanceMapper;
        this.erpStockTxnMapper = erpStockTxnMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.erpCostService = erpCostService;
    }

    @Override
    public List<ErpAssemblyOrder> listAll(String keyword, String status, String orderType, Instant startAt, Instant endAt) {
        QueryWrapper<ErpAssemblyOrder> wrapper = baseWrapper(keyword, status, orderType, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        return erpAssemblyOrderMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpAssemblyOrder> page(long page, long size, String keyword, String status, String orderType, Instant startAt, Instant endAt) {
        Page<ErpAssemblyOrder> pageReq = Page.of(page, size);
        QueryWrapper<ErpAssemblyOrder> wrapper = baseWrapper(keyword, status, orderType, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        Page<ErpAssemblyOrder> result = erpAssemblyOrderMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public PageResponse<ErpAssemblySourceSaleOrderOption> sourceSaleOrderPage(long page, long size, String keyword, Long customerId) {
        Long tenantId = TenantContext.requireTenantId();
        long safePage = Math.max(page, 1);
        int safeSize = (int) Math.max(1, Math.min(size, 100));
        long offset = (safePage - 1) * safeSize;
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        long total = erpAssemblyOrderMapper.countSourceSaleOrders(tenantId, normalizedKeyword, customerId);
        List<ErpAssemblySourceSaleOrderOption> items = total == 0
            ? List.of()
            : erpAssemblyOrderMapper.findSourceSaleOrders(tenantId, normalizedKeyword, customerId, safeSize, offset);
        return new PageResponse<>(total, safePage, safeSize, items);
    }

    @Override
    public ErpAssemblySourceSaleOrderDetail getSourceSaleOrderDetail(Long saleOrderId) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSaleOrder saleOrder = loadSaleOrder(tenantId, saleOrderId);
        ErpCustomer customer = saleOrder.getCustomerId() == null ? null : erpCustomerMapper.selectOne(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .eq("id", saleOrder.getCustomerId()));
        List<ErpAssemblySourceSaleOrderItem> items = erpAssemblyOrderMapper.findSourceSaleOrderItems(tenantId, saleOrderId);
        List<ErpAssemblyOrder> relatedAssemblies = erpAssemblyOrderMapper.findBySourceSaleOrderId(tenantId, saleOrderId);
        return new ErpAssemblySourceSaleOrderDetail(
            saleOrder.getId(),
            saleOrder.getOrderNo(),
            saleOrder.getStatus(),
            saleOrder.getCustomerId(),
            customer == null ? null : customer.getName(),
            saleOrder.getOrderAt(),
            items,
            relatedAssemblies
        );
    }

    @Override
    public List<ErpAssemblyOrder> listBySaleOrderId(Long saleOrderId) {
        Long tenantId = TenantContext.requireTenantId();
        return erpAssemblyOrderMapper.findBySourceSaleOrderId(tenantId, saleOrderId);
    }

    @Override
    public ErpAssemblyOrderDetail getDetail(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpAssemblyOrder order = erpAssemblyOrderMapper.selectOne(new QueryWrapper<ErpAssemblyOrder>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (order == null) {
            throw new IllegalArgumentException("组装单不存在");
        }
        List<ErpAssemblyOrderItem> items = erpAssemblyOrderItemMapper.findByOrderId(tenantId, id);
        return new ErpAssemblyOrderDetail(order, items);
    }

    @Override
    public String nextOrderNo(String orderType) {
        Long tenantId = TenantContext.requireTenantId();
        return ensureOrderNo(tenantId, null, normalizeType(orderType));
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_ASSEMBLY_CREATE", entityType = "erp_assembly_order", entityId = "{result.order.id}", detail = "orderNo={result.order.orderNo}")
    public ErpAssemblyOrderDetail create(ErpAssemblyOrderCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String operator = resolveCurrentUsername();
        validateOrderRequest(
            request.finishedProductId(),
            request.finishedQty(),
            request.items(),
            request.warehouseId(),
            request.locationId(),
            tenantId,
            Set.of()
        );
        String type = normalizeType(request.orderType());
        String orderNo = ensureOrderNo(tenantId, request.orderNo(), type);
        ErpAssemblyOrder order = new ErpAssemblyOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(orderNo);
        order.setOrderType(type);
        order.setStatus(STATUS_DRAFT);
        applySource(order, request.sourceType(), request.sourceSaleOrderId(), request.sourceSaleOrderItemId(), request.customerId(), request.finishedProductId(), tenantId);
        order.setFinishedProductId(request.finishedProductId());
        order.setFinishedQty(normalizeAmount(request.finishedQty()));
        order.setWarehouseId(request.warehouseId());
        order.setLocationId(request.locationId());
        order.setLaborCost(normalizeAmount(request.laborCost()));
        order.setInventoryReserved(false);
        Instant orderAt = parseOrderAt(request.orderAt());
        order.setOrderAt(orderAt == null ? Instant.now() : orderAt);
        order.setRemark(request.remark());
        order.setCreatedAt(Instant.now());
        order.setCreatedBy(operator);
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(operator);
        erpAssemblyOrderMapper.insert(order);

        List<ErpAssemblyOrderItem> items = buildItems(tenantId, order.getId(), request.items(), Set.of());
        for (ErpAssemblyOrderItem item : items) {
            erpAssemblyOrderItemMapper.insert(item);
        }
        applyTotals(order, items, true);
        reserveDraftStock(tenantId, order, items);
        order.setInventoryReserved(true);
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(operator);
        erpAssemblyOrderMapper.updateById(order);
        return new ErpAssemblyOrderDetail(order, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_ASSEMBLY_UPDATE", entityType = "erp_assembly_order", entityId = "{arg0}", detail = "orderNo={arg1.orderNo}")
    public ErpAssemblyOrderDetail update(Long id, ErpAssemblyOrderUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpAssemblyOrder order = erpAssemblyOrderMapper.findByIdForUpdate(tenantId, id);
        if (order == null) {
            throw new IllegalArgumentException("组装单不存在");
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可编辑");
        }
        List<ErpAssemblyOrderItem> existingItems = erpAssemblyOrderItemMapper.findByOrderId(tenantId, id);
        Set<Long> allowedDisabledProductIds = existingProductIds(existingItems, order.getFinishedProductId());
        validateOrderRequest(
            request.finishedProductId(),
            request.finishedQty(),
            request.items(),
            request.warehouseId(),
            request.locationId(),
            tenantId,
            allowedDisabledProductIds
        );
        String newOrderNo = resolveOrderNoForUpdate(request.orderNo(), order.getOrderNo(), tenantId, order.getId());
        order.setOrderNo(newOrderNo);
        order.setOrderType(normalizeType(request.orderType()));
        applySource(order, request.sourceType(), request.sourceSaleOrderId(), request.sourceSaleOrderItemId(), request.customerId(), request.finishedProductId(), tenantId);
        order.setFinishedProductId(request.finishedProductId());
        order.setFinishedQty(normalizeAmount(request.finishedQty()));
        order.setWarehouseId(request.warehouseId());
        order.setLocationId(request.locationId());
        order.setLaborCost(normalizeAmount(request.laborCost()));
        Instant orderAt = parseOrderAt(request.orderAt());
        order.setOrderAt(orderAt == null ? order.getOrderAt() : orderAt);
        order.setRemark(request.remark());
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(resolveCurrentUsername());

        if (Boolean.TRUE.equals(order.getInventoryReserved())) {
            releaseDraftStock(tenantId, order, existingItems);
            order.setInventoryReserved(false);
        }

        erpAssemblyOrderItemMapper.delete(new QueryWrapper<ErpAssemblyOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("order_id", id));

        List<ErpAssemblyOrderItem> items = buildItems(tenantId, id, request.items(), allowedDisabledProductIds);
        for (ErpAssemblyOrderItem item : items) {
            erpAssemblyOrderItemMapper.insert(item);
        }
        applyTotals(order, items, true);
        reserveDraftStock(tenantId, order, items);
        order.setInventoryReserved(true);
        erpAssemblyOrderMapper.updateById(order);
        return new ErpAssemblyOrderDetail(order, items);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_ASSEMBLY_DELETE", entityType = "erp_assembly_order", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpAssemblyOrder order = erpAssemblyOrderMapper.findByIdForUpdate(tenantId, id);
        if (order == null) {
            throw new IllegalArgumentException("组装单不存在");
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可删除");
        }
        List<ErpAssemblyOrderItem> items = erpAssemblyOrderItemMapper.findByOrderId(tenantId, id);
        if (Boolean.TRUE.equals(order.getInventoryReserved())) {
            releaseDraftStock(tenantId, order, items);
        }
        erpAssemblyOrderItemMapper.delete(new QueryWrapper<ErpAssemblyOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("order_id", id));
        erpAssemblyOrderMapper.deleteById(id);
    }

    @Override
    @Transactional
    @AuditLog(action = "ERP_ASSEMBLY_APPROVE", entityType = "erp_assembly_order", entityId = "{arg0}")
    public void approve(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpAssemblyOrder order = erpAssemblyOrderMapper.findByIdForUpdate(tenantId, id);
        if (order == null) {
            throw new IllegalArgumentException("组装单不存在");
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可审核");
        }
        List<ErpAssemblyOrderItem> items = erpAssemblyOrderItemMapper.findByOrderId(tenantId, id);
        String operator = resolveCurrentUsername();
        String type = normalizeType(order.getOrderType());
        boolean inventoryReserved = Boolean.TRUE.equals(order.getInventoryReserved());
        if (TYPE_ASSEMBLE.equals(type)) {
            for (ErpAssemblyOrderItem item : items) {
                applyStockDelta(tenantId, item, item.getQty().negate(), "ASSEMBLE_OUT", id, operator, inventoryReserved);
            }
            updateFinishedCost(tenantId, order);
            applyFinishedDelta(tenantId, order, order.getFinishedQty(), "ASSEMBLE_IN", id, operator, false);
        } else {
            applyDisassembleCost(tenantId, order, items);
            applyTotals(order, items, false);
            applyFinishedDelta(tenantId, order, order.getFinishedQty().negate(), "DISASSEMBLE_OUT", id, operator, inventoryReserved);
            for (ErpAssemblyOrderItem item : items) {
                applyStockDelta(tenantId, item, item.getQty(), "DISASSEMBLE_IN", id, operator, false);
            }
        }
        order.setStatus(STATUS_APPROVED);
        order.setApprovedBy(operator);
        order.setApprovedAt(Instant.now());
        order.setInventoryReserved(false);
        order.setUpdatedAt(Instant.now());
        order.setUpdatedBy(operator);
        erpAssemblyOrderMapper.updateById(order);
    }

    private void applySource(ErpAssemblyOrder order,
                             String sourceType,
                             Long sourceSaleOrderId,
                             Long sourceSaleOrderItemId,
                             Long customerId,
                             Long finishedProductId,
                             Long tenantId) {
        String normalizedSourceType = normalizeSourceType(sourceType, sourceSaleOrderId);
        order.setSourceType(normalizedSourceType);
        order.setSourceSaleOrderId(null);
        order.setSourceSaleOrderNo(null);
        order.setSourceSaleOrderItemId(null);
        order.setCustomerId(null);
        order.setCustomerName(null);

        if (!SOURCE_SALE_ORDER.equals(normalizedSourceType)) {
            order.setCustomerId(customerId);
            if (customerId != null) {
                ErpCustomer customer = erpCustomerMapper.selectOne(new QueryWrapper<ErpCustomer>()
                    .eq("tenant_id", tenantId)
                    .eq("id", customerId));
                if (customer == null) {
                    throw new IllegalArgumentException("客户不存在");
                }
                order.setCustomerName(customer.getName());
            }
            return;
        }

        if (sourceSaleOrderId == null) {
            throw new IllegalArgumentException("请选择来源销售单");
        }
        ErpSaleOrder saleOrder = loadSaleOrder(tenantId, sourceSaleOrderId);
        if ("CANCELLED".equals(saleOrder.getStatus()) || "RED_FLUSHED".equals(saleOrder.getStatus())) {
            throw new IllegalArgumentException("已作废或已红冲的销售单不能作为组装来源");
        }
        if (sourceSaleOrderItemId != null) {
            ErpSaleOrderItem saleItem = erpSaleOrderItemMapper.selectOne(new QueryWrapper<ErpSaleOrderItem>()
                .eq("tenant_id", tenantId)
                .eq("id", sourceSaleOrderItemId)
                .eq("order_id", sourceSaleOrderId));
            if (saleItem == null) {
                throw new IllegalArgumentException("来源销售单明细不存在");
            }
            if (finishedProductId != null && saleItem.getProductId() != null && !finishedProductId.equals(saleItem.getProductId())) {
                throw new IllegalArgumentException("来源销售行商品必须与组装成品一致");
            }
        }
        ErpCustomer customer = saleOrder.getCustomerId() == null ? null : erpCustomerMapper.selectOne(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .eq("id", saleOrder.getCustomerId()));
        order.setSourceSaleOrderId(saleOrder.getId());
        order.setSourceSaleOrderNo(saleOrder.getOrderNo());
        order.setSourceSaleOrderItemId(sourceSaleOrderItemId);
        order.setCustomerId(saleOrder.getCustomerId());
        order.setCustomerName(customer == null ? null : customer.getName());
    }

    private String normalizeSourceType(String sourceType, Long sourceSaleOrderId) {
        if (sourceSaleOrderId != null) {
            return SOURCE_SALE_ORDER;
        }
        if (sourceType == null || sourceType.isBlank()) {
            return SOURCE_MANUAL;
        }
        String normalized = sourceType.trim().toUpperCase();
        if (SOURCE_SALE_ORDER.equals(normalized) || SOURCE_STOCK_PREPARE.equals(normalized) || SOURCE_MANUAL.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("来源类型不正确");
    }

    private ErpSaleOrder loadSaleOrder(Long tenantId, Long saleOrderId) {
        if (saleOrderId == null) {
            throw new IllegalArgumentException("销售单不存在");
        }
        ErpSaleOrder saleOrder = erpSaleOrderMapper.selectOne(new QueryWrapper<ErpSaleOrder>()
            .eq("tenant_id", tenantId)
            .eq("id", saleOrderId));
        if (saleOrder == null) {
            throw new IllegalArgumentException("销售单不存在");
        }
        return saleOrder;
    }

    private List<ErpAssemblyOrderItem> buildItems(Long tenantId,
                                                  Long orderId,
                                                  List<ErpAssemblyOrderItemRequest> requests,
                                                  Set<Long> allowedDisabledProductIds) {
        List<ErpAssemblyOrderItem> items = new ArrayList<>();
        Set<String> lineKeys = new HashSet<>();
        int index = 1;
        for (ErpAssemblyOrderItemRequest request : requests) {
            ErpProduct product = requireUsableProduct(tenantId, request.productId(), allowedDisabledProductIds);
            validateStockScope(tenantId, request.warehouseId(), request.locationId(), false);
            BigDecimal qty = normalizeAmount(request.qty());
            String lineKey = product.getId() + "|" + String.valueOf(request.warehouseId()) + "|" + String.valueOf(request.locationId());
            if (!lineKeys.add(lineKey)) {
                throw new IllegalArgumentException("同一商品、仓库、库位不能重复录入");
            }
            BigDecimal unitCost = product.getCostPrice() == null ? BigDecimal.ZERO : product.getCostPrice();
            ErpAssemblyOrderItem item = new ErpAssemblyOrderItem();
            item.setTenantId(tenantId);
            item.setOrderId(orderId);
            item.setLineNo(index);
            item.setProductId(product.getId());
            item.setProductCode(product.getCode());
            item.setProductName(product.getName());
            item.setWarehouseId(request.warehouseId());
            item.setLocationId(request.locationId());
            item.setQty(qty);
            item.setUnitCost(unitCost);
            item.setAmount(unitCost.multiply(qty));
            item.setRemark(request.remark());
            item.setCreatedAt(Instant.now());
            item.setUpdatedAt(Instant.now());
            items.add(item);
            index += 1;
        }
        return items;
    }

    private void applyTotals(ErpAssemblyOrder order, List<ErpAssemblyOrderItem> items) {
        applyTotals(order, items, true);
    }

    private void applyTotals(ErpAssemblyOrder order, List<ErpAssemblyOrderItem> items, boolean includeLabor) {
        BigDecimal materialCost = BigDecimal.ZERO;
        for (ErpAssemblyOrderItem item : items) {
            if (item.getAmount() != null) {
                materialCost = materialCost.add(item.getAmount());
            }
        }
        BigDecimal labor = includeLabor && order.getLaborCost() != null ? order.getLaborCost() : BigDecimal.ZERO;
        BigDecimal total = materialCost.add(labor);
        BigDecimal finishedQty = order.getFinishedQty() == null ? BigDecimal.ZERO : order.getFinishedQty();
        BigDecimal unitCost = BigDecimal.ZERO;
        if (finishedQty.compareTo(BigDecimal.ZERO) > 0) {
            unitCost = total.divide(finishedQty, 4, RoundingMode.HALF_UP);
        }
        order.setTotalCost(total);
        order.setUnitCost(unitCost);
    }

    private void reserveDraftStock(Long tenantId, ErpAssemblyOrder order, List<ErpAssemblyOrderItem> items) {
        if (TYPE_ASSEMBLE.equals(normalizeType(order.getOrderType()))) {
            for (ErpAssemblyOrderItem item : items) {
                changeReservedStock(tenantId, item.getProductId(), item.getWarehouseId(), item.getLocationId(),
                    item.getQty(), item.getProductName(), item.getProductCode(), "组装单");
            }
            return;
        }
        changeReservedStock(tenantId, order.getFinishedProductId(), order.getWarehouseId(), order.getLocationId(),
            normalizeAmount(order.getFinishedQty()), null, null, "拆装单");
    }

    private void releaseDraftStock(Long tenantId, ErpAssemblyOrder order, List<ErpAssemblyOrderItem> items) {
        if (TYPE_ASSEMBLE.equals(normalizeType(order.getOrderType()))) {
            for (ErpAssemblyOrderItem item : items) {
                changeReservedStock(tenantId, item.getProductId(), item.getWarehouseId(), item.getLocationId(),
                    item.getQty().negate(), item.getProductName(), item.getProductCode(), "组装单");
            }
            return;
        }
        changeReservedStock(tenantId, order.getFinishedProductId(), order.getWarehouseId(), order.getLocationId(),
            normalizeAmount(order.getFinishedQty()).negate(), null, null, "拆装单");
    }

    private void changeReservedStock(Long tenantId,
                                     Long productId,
                                     Long warehouseId,
                                     Long locationId,
                                     BigDecimal delta,
                                     String productName,
                                     String productCode,
                                     String bizName) {
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        ErpStockBalance updatedBalance = erpStockBalanceMapper.addReservedQtyIfEnough(
            tenantId, productId, warehouseId, locationId, delta, resolveCurrentUsername());
        if (updatedBalance != null) {
            return;
        }
        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(bizName + "库存占用数据异常，请刷新后重试");
        }
        String productLabel = productName == null ? productCode : productName;
        if ((productLabel == null || productLabel.isBlank()) && productId != null) {
            ErpProduct product = erpProductMapper.selectOne(new QueryWrapper<ErpProduct>()
                .eq("tenant_id", tenantId)
                .eq("id", productId));
            if (product != null) {
                productLabel = product.getName() == null ? product.getCode() : product.getName();
            }
        }
        throw insufficientStockException(tenantId, productId, warehouseId, locationId, productLabel, delta);
    }

    private void applyStockDelta(Long tenantId,
                                 ErpAssemblyOrderItem item,
                                 BigDecimal delta,
                                 String bizType,
                                 Long orderId,
                                 String operator,
                                 boolean consumeReserved) {
        Long warehouseId = item.getWarehouseId();
        Long locationId = item.getLocationId();
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
        BigDecimal unitCost = item.getUnitCost();
        if (unitCost == null) {
            unitCost = getProductCost(tenantId, item.getProductId());
        }
        BigDecimal totalCost = unitCost.multiply(delta).setScale(4, RoundingMode.HALF_UP);
        txn.setUnitCost(unitCost);
        txn.setTotalCost(totalCost);
        txn.setOperator(operator);
        txn.setOperatorId(null);
        txn.setRemark(item.getRemark());
        txn.setCreatedAt(Instant.now());
        erpStockTxnMapper.insert(txn);
    }

    private void applyFinishedDelta(Long tenantId,
                                    ErpAssemblyOrder order,
                                    BigDecimal delta,
                                    String bizType,
                                    Long orderId,
                                    String operator,
                                    boolean consumeReserved) {
        Long warehouseId = order.getWarehouseId();
        Long locationId = order.getLocationId();
        ErpStockBalance updatedBalance;
        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            if (consumeReserved) {
                updatedBalance = erpStockBalanceMapper.consumeReservedQty(
                    tenantId, order.getFinishedProductId(), warehouseId, locationId, delta.abs(), operator);
            } else {
                updatedBalance = erpStockBalanceMapper.addQtyIfEnoughAvailable(
                    tenantId, order.getFinishedProductId(), warehouseId, locationId, delta, operator);
            }
        } else {
            updatedBalance = erpStockBalanceMapper.upsertAddQty(
                tenantId, order.getFinishedProductId(), warehouseId, locationId, delta, operator);
        }
        if (updatedBalance == null) {
            throw insufficientStockException(tenantId, order.getFinishedProductId(), warehouseId, locationId, "成品", delta.abs());
        }
        BigDecimal after = updatedBalance.getQtyOnHand() == null ? BigDecimal.ZERO : updatedBalance.getQtyOnHand();
        BigDecimal before = after.subtract(delta);

        ErpStockTxn txn = new ErpStockTxn();
        txn.setTenantId(tenantId);
        txn.setTxnNo(generateTxnNo());
        txn.setBizType(bizType);
        txn.setBizId(orderId);
        txn.setBizItemId(null);
        txn.setProductId(order.getFinishedProductId());
        txn.setWarehouseId(warehouseId);
        txn.setLocationId(locationId);
        txn.setQtyDelta(delta);
        txn.setQtyBefore(before);
        txn.setQtyAfter(after);
        BigDecimal unitCost;
        if (delta.compareTo(BigDecimal.ZERO) >= 0) {
            unitCost = order.getUnitCost() == null ? getProductCost(tenantId, order.getFinishedProductId()) : order.getUnitCost();
        } else {
            unitCost = getProductCost(tenantId, order.getFinishedProductId());
        }
        BigDecimal totalCost = unitCost.multiply(delta).setScale(4, RoundingMode.HALF_UP);
        txn.setUnitCost(unitCost);
        txn.setTotalCost(totalCost);
        txn.setOperator(operator);
        txn.setOperatorId(null);
        txn.setRemark(order.getRemark());
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
        String label = (productLabel == null || productLabel.isBlank()) ? "商品" : productLabel;
        return new IllegalArgumentException(
            "库存不足，商品[" + label + "] 可用=" + currentQty.subtract(lockedQty) + "，需求=" + required
        );
    }

    private void updateFinishedCost(Long tenantId, ErpAssemblyOrder order) {
        if (order.getFinishedProductId() == null) {
            return;
        }
        BigDecimal inboundQty = normalizeAmount(order.getFinishedQty());
        if (inboundQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal inboundUnitCost = order.getUnitCost() == null ? BigDecimal.ZERO : order.getUnitCost();
        erpCostService.applyInboundAverageCost(tenantId, order.getFinishedProductId(), inboundQty, inboundUnitCost);
    }

    private void applyDisassembleCost(Long tenantId, ErpAssemblyOrder order, List<ErpAssemblyOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        BigDecimal finishedQty = normalizeAmount(order.getFinishedQty());
        if (finishedQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal finishedUnitCost = getProductCost(tenantId, order.getFinishedProductId());
        BigDecimal laborCost = normalizeAmount(order.getLaborCost());
        BigDecimal finishedTotalCost = finishedUnitCost.multiply(finishedQty).add(laborCost);
        BigDecimal refTotal = BigDecimal.ZERO;
        BigDecimal qtyTotal = BigDecimal.ZERO;
        for (ErpAssemblyOrderItem item : items) {
            BigDecimal qty = normalizeAmount(item.getQty());
            qtyTotal = qtyTotal.add(qty);
            BigDecimal refCost = getProductCost(tenantId, item.getProductId());
            refTotal = refTotal.add(refCost.multiply(qty));
        }
        for (ErpAssemblyOrderItem item : items) {
            BigDecimal qty = normalizeAmount(item.getQty());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                item.setUnitCost(BigDecimal.ZERO);
                item.setAmount(BigDecimal.ZERO);
                erpAssemblyOrderItemMapper.updateById(item);
                continue;
            }
            BigDecimal ratio;
            if (refTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal refCost = getProductCost(tenantId, item.getProductId()).multiply(qty);
                ratio = refCost.divide(refTotal, 8, RoundingMode.HALF_UP);
            } else if (qtyTotal.compareTo(BigDecimal.ZERO) > 0) {
                ratio = qty.divide(qtyTotal, 8, RoundingMode.HALF_UP);
            } else {
                ratio = BigDecimal.ZERO;
            }
            BigDecimal allocatedTotal = finishedTotalCost.multiply(ratio);
            BigDecimal unitCost = allocatedTotal.divide(qty, 4, RoundingMode.HALF_UP);
            item.setUnitCost(unitCost);
            item.setAmount(unitCost.multiply(qty));
            erpAssemblyOrderItemMapper.updateById(item);
            erpCostService.applyInboundAverageCost(tenantId, item.getProductId(), qty, unitCost);
        }
    }

    private BigDecimal getProductCost(Long tenantId, Long productId) {
        if (productId == null) {
            return BigDecimal.ZERO;
        }
        return erpCostService.getProductCost(tenantId, productId);
    }

    private QueryWrapper<ErpAssemblyOrder> baseWrapper(String keyword, String status, String orderType, Instant startAt, Instant endAt) {
        Long tenantId = TenantContext.requireTenantId();
        QueryWrapper<ErpAssemblyOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("order_no", keyword)
                .or().like("source_sale_order_no", keyword)
                .or().like("customer_name", keyword)
                .or().like("remark", keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status);
        }
        if (orderType != null && !orderType.isBlank()) {
            wrapper.eq("order_type", orderType);
        }
        if (startAt != null) {
            wrapper.ge("order_at", startAt);
        }
        if (endAt != null) {
            wrapper.le("order_at", endAt);
        }
        return wrapper;
    }

    private String normalizeType(String orderType) {
        if (orderType == null || orderType.isBlank()) {
            return TYPE_ASSEMBLE;
        }
        String type = orderType.trim().toUpperCase();
        return TYPE_DISASSEMBLE.equals(type) ? TYPE_DISASSEMBLE : TYPE_ASSEMBLE;
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void validateOrderRequest(Long finishedProductId,
                                      BigDecimal finishedQty,
                                      List<ErpAssemblyOrderItemRequest> items,
                                      Long warehouseId,
                                      Long locationId,
                                      Long tenantId,
                                      Set<Long> allowedDisabledProductIds) {
        if (finishedProductId == null) {
            throw new IllegalArgumentException("成品不能为空");
        }
        requireUsableProduct(tenantId, finishedProductId, allowedDisabledProductIds);
        if (finishedQty == null || finishedQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("成品数量必须大于 0");
        }
        validateStockScope(tenantId, warehouseId, locationId, false);
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("明细不能为空");
        }
        for (ErpAssemblyOrderItemRequest item : items) {
            if (item == null || item.productId() == null) {
                throw new IllegalArgumentException("明细商品不能为空");
            }
            if (item.qty() == null || item.qty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("明细数量必须大于 0");
            }
        }
    }

    private void validateStockScope(Long tenantId, Long warehouseId, Long locationId, boolean warehouseRequired) {
        if (warehouseId == null) {
            if (warehouseRequired || locationId != null) {
                throw new IllegalArgumentException("仓库不能为空");
            }
            return;
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

    private Set<Long> existingProductIds(List<ErpAssemblyOrderItem> items, Long finishedProductId) {
        Set<Long> ids = new HashSet<>();
        if (finishedProductId != null) {
            ids.add(finishedProductId);
        }
        if (items == null) {
            return ids;
        }
        for (ErpAssemblyOrderItem item : items) {
            if (item != null && item.getProductId() != null) {
                ids.add(item.getProductId());
            }
        }
        return ids;
    }

    private Instant parseOrderAt(String orderAt) {
        if (orderAt == null || orderAt.isBlank()) {
            return null;
        }
        String trimmed = orderAt.trim();
        if (trimmed.matches("^\\d+$")) {
            return Instant.ofEpochMilli(Long.parseLong(trimmed));
        }
        if (trimmed.contains("T")) {
            return Instant.parse(trimmed);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(trimmed, formatter).atZone(ZoneId.systemDefault()).toInstant();
    }

    private String resolveOrderNoForUpdate(String requestOrderNo, String currentOrderNo, Long tenantId, Long orderId) {
        String trimmed = requestOrderNo == null ? "" : requestOrderNo.trim();
        if (trimmed.isEmpty() || trimmed.equals(currentOrderNo)) {
            return currentOrderNo;
        }
        ErpAssemblyOrder existing = erpAssemblyOrderMapper.findByOrderNo(tenantId, trimmed);
        if (existing != null && !existing.getId().equals(orderId)) {
            throw new IllegalArgumentException("单号已存在");
        }
        return trimmed;
    }

    private String ensureOrderNo(Long tenantId, String provided, String orderType) {
        String trimmed = provided == null ? "" : provided.trim();
        if (!trimmed.isEmpty()) {
            ErpAssemblyOrder existing = erpAssemblyOrderMapper.findByOrderNo(tenantId, trimmed);
            if (existing != null) {
                throw new IllegalArgumentException("单号已存在");
            }
            return trimmed;
        }
        String prefix = readConfig("erp.order.no.assembly.prefix", "AO");
        String dateFormat = readConfig("erp.order.no.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.order.no.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, ORDER_TYPE + "_" + normalizeType(orderType), dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, ORDER_TYPE + "_" + normalizeType(orderType), dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private String readConfig(String key, String defaultValue) {
        SystemConfig config = systemConfigMapper.findByKey(TenantContext.requireTenantId(), key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return defaultValue;
        }
        return config.getConfigValue().trim();
    }

    private int readIntConfig(String key, int defaultValue) {
        try {
            return Integer.parseInt(readConfig(key, String.valueOf(defaultValue)));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }

    private String generateTxnNo() {
        return "TXN-" + UUID.randomUUID();
    }
}
