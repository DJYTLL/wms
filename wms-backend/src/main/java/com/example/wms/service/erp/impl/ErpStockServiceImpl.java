package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockBalanceOption;
import com.example.wms.dto.erp.ErpStockOccupancyView;
import com.example.wms.entity.erp.ErpAssemblyOrder;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockTxn;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.entity.erp.ErpPurchaseReturn;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.entity.erp.ErpSaleReturn;
import com.example.wms.entity.erp.ErpStockCount;
import com.example.wms.entity.erp.ErpStockTransfer;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpAssemblyOrderMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockCountMapper;
import com.example.wms.mapper.erp.ErpStockTransferMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.ErpStockService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// 库存服务实现（ERP进销存）
@Service
public class ErpStockServiceImpl implements ErpStockService {
    private final ErpStockBalanceMapper erpStockBalanceMapper;
    private final ErpStockTxnMapper erpStockTxnMapper;
    private final ErpWarehouseMapper erpWarehouseMapper;
    private final ErpLocationMapper erpLocationMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpPurchaseOrderMapper erpPurchaseOrderMapper;
    private final ErpSaleOrderMapper erpSaleOrderMapper;
    private final ErpPurchaseReturnMapper erpPurchaseReturnMapper;
    private final ErpSaleReturnMapper erpSaleReturnMapper;
    private final ErpStockCountMapper erpStockCountMapper;
    private final ErpStockTransferMapper erpStockTransferMapper;
    private final ErpAssemblyOrderMapper erpAssemblyOrderMapper;

    public ErpStockServiceImpl(ErpStockBalanceMapper erpStockBalanceMapper,
                               ErpStockTxnMapper erpStockTxnMapper,
                               ErpWarehouseMapper erpWarehouseMapper,
                               ErpLocationMapper erpLocationMapper,
                               ErpProductMapper erpProductMapper,
                               ErpPurchaseOrderMapper erpPurchaseOrderMapper,
                               ErpSaleOrderMapper erpSaleOrderMapper,
                               ErpPurchaseReturnMapper erpPurchaseReturnMapper,
                               ErpSaleReturnMapper erpSaleReturnMapper,
                               ErpStockCountMapper erpStockCountMapper,
                               ErpStockTransferMapper erpStockTransferMapper,
                               ErpAssemblyOrderMapper erpAssemblyOrderMapper) {
        this.erpStockBalanceMapper = erpStockBalanceMapper;
        this.erpStockTxnMapper = erpStockTxnMapper;
        this.erpWarehouseMapper = erpWarehouseMapper;
        this.erpLocationMapper = erpLocationMapper;
        this.erpProductMapper = erpProductMapper;
        this.erpPurchaseOrderMapper = erpPurchaseOrderMapper;
        this.erpSaleOrderMapper = erpSaleOrderMapper;
        this.erpPurchaseReturnMapper = erpPurchaseReturnMapper;
        this.erpSaleReturnMapper = erpSaleReturnMapper;
        this.erpStockCountMapper = erpStockCountMapper;
        this.erpStockTransferMapper = erpStockTransferMapper;
        this.erpAssemblyOrderMapper = erpAssemblyOrderMapper;
    }

    @Override
    public PageResponse<ErpStockBalance> pageBalance(long page, long size, Long productId, Long warehouseId, Long locationId) {
        Page<ErpStockBalance> pageReq = Page.of(page, size);
        QueryWrapper<ErpStockBalance> wrapper = new QueryWrapper<ErpStockBalance>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (productId != null) {
            wrapper.eq("product_id", productId);
        }
        if (warehouseId != null) {
            wrapper.eq("warehouse_id", warehouseId);
        }
        if (locationId != null) {
            if (locationId < 0) {
                wrapper.isNull("location_id");
            } else {
                wrapper.eq("location_id", locationId);
            }
        }
        wrapper.orderByDesc("updated_at");
        Page<ErpStockBalance> result = erpStockBalanceMapper.selectPage(pageReq, wrapper);
        fillBalanceNames(result.getRecords());
        for (ErpStockBalance balance : result.getRecords()) {
            BigDecimal onHand = balance.getQtyOnHand() == null ? BigDecimal.ZERO : balance.getQtyOnHand();
            BigDecimal locked = balance.getQtyLocked() == null ? BigDecimal.ZERO : balance.getQtyLocked();
            balance.setQtyLocked(locked);
            balance.setQtyAvailable(onHand.subtract(locked));
        }
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    private void fillBalanceNames(List<ErpStockBalance> balances) {
        if (balances == null || balances.isEmpty()) {
            return;
        }
        Set<Long> productIds = balances.stream()
            .map(ErpStockBalance::getProductId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Set<Long> warehouseIds = balances.stream()
            .map(ErpStockBalance::getWarehouseId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Set<Long> locationIds = balances.stream()
            .map(ErpStockBalance::getLocationId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, String> productNameMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<ErpProduct> products = erpProductMapper.selectBatchIds(productIds);
            for (ErpProduct product : products) {
                productNameMap.put(product.getId(), product.getName());
            }
        }

        Map<Long, String> warehouseNameMap = new HashMap<>();
        if (!warehouseIds.isEmpty()) {
            List<ErpWarehouse> warehouses = erpWarehouseMapper.selectBatchIds(warehouseIds);
            for (ErpWarehouse warehouse : warehouses) {
                warehouseNameMap.put(warehouse.getId(), warehouse.getName());
            }
        }

        Map<Long, String> locationNameMap = new HashMap<>();
        if (!locationIds.isEmpty()) {
            List<ErpLocation> locations = erpLocationMapper.selectBatchIds(locationIds);
            for (ErpLocation location : locations) {
                locationNameMap.put(location.getId(), location.getName());
            }
        }

        for (ErpStockBalance balance : balances) {
            balance.setProductName(productNameMap.getOrDefault(balance.getProductId(), "-"));
            balance.setWarehouseName(warehouseNameMap.getOrDefault(balance.getWarehouseId(), "-"));
            balance.setLocationName(balance.getLocationId() == null
                ? "未指定库位"
                : locationNameMap.getOrDefault(balance.getLocationId(), "-"));
        }
    }

    @Override
    public PageResponse<ErpStockTxn> pageTxn(long page, long size, String bizType, Long bizId, Long productId) {
        Page<ErpStockTxn> pageReq = Page.of(page, size);
        QueryWrapper<ErpStockTxn> wrapper = new QueryWrapper<ErpStockTxn>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (bizType != null && !bizType.isBlank()) {
            wrapper.eq("biz_type", bizType);
        }
        if (bizId != null) {
            wrapper.eq("biz_id", bizId);
        }
        if (productId != null) {
            wrapper.eq("product_id", productId);
        }
        wrapper.orderByDesc("created_at");
        Page<ErpStockTxn> result = erpStockTxnMapper.selectPage(pageReq, wrapper);
        fillTxnNames(result.getRecords());
        fillDocNo(result.getRecords());
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    private void fillTxnNames(List<ErpStockTxn> txns) {
        if (txns == null || txns.isEmpty()) {
            return;
        }
        Set<Long> productIds = txns.stream()
            .map(ErpStockTxn::getProductId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Set<Long> warehouseIds = txns.stream()
            .map(ErpStockTxn::getWarehouseId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Set<Long> locationIds = txns.stream()
            .map(ErpStockTxn::getLocationId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, String> productNameMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<ErpProduct> products = erpProductMapper.selectBatchIds(productIds);
            for (ErpProduct product : products) {
                productNameMap.put(product.getId(), product.getName());
            }
        }

        Map<Long, String> warehouseNameMap = new HashMap<>();
        if (!warehouseIds.isEmpty()) {
            List<ErpWarehouse> warehouses = erpWarehouseMapper.selectBatchIds(warehouseIds);
            for (ErpWarehouse warehouse : warehouses) {
                warehouseNameMap.put(warehouse.getId(), warehouse.getName());
            }
        }

        Map<Long, String> locationNameMap = new HashMap<>();
        if (!locationIds.isEmpty()) {
            List<ErpLocation> locations = erpLocationMapper.selectBatchIds(locationIds);
            for (ErpLocation location : locations) {
                locationNameMap.put(location.getId(), location.getName());
            }
        }

        for (ErpStockTxn txn : txns) {
            txn.setProductName(productNameMap.getOrDefault(txn.getProductId(), "-"));
            txn.setWarehouseName(warehouseNameMap.getOrDefault(txn.getWarehouseId(), "-"));
            txn.setLocationName(txn.getLocationId() == null
                ? "未指定库位"
                : locationNameMap.getOrDefault(txn.getLocationId(), "未指定库位"));
        }
    }

    private void fillDocNo(List<ErpStockTxn> txns) {
        if (txns == null || txns.isEmpty()) {
            return;
        }
        Long tenantId = TenantContext.requireTenantId();
        Map<Long, String> purchaseNos = orderNoMap(txns, tenantId, Set.of("PURCHASE_APPROVE", "PURCHASE_UNAPPROVE", "PURCHASE_CANCEL"), erpPurchaseOrderMapper, ErpPurchaseOrder::getOrderNo);
        Map<Long, String> saleNos = orderNoMap(txns, tenantId, Set.of("SALE_APPROVE", "SALE_RED_FLUSH"), erpSaleOrderMapper, ErpSaleOrder::getOrderNo);
        Map<Long, String> purchaseReturnNos = orderNoMap(txns, tenantId, Set.of("PURCHASE_RETURN", "PURCHASE_RETURN_SCRAP", "PURCHASE_RETURN_RED_FLUSH"), erpPurchaseReturnMapper, ErpPurchaseReturn::getOrderNo);
        Map<Long, String> saleReturnNos = orderNoMap(txns, tenantId, Set.of("SALE_RETURN_RESTOCK", "SALE_RETURN_SCRAP", "SALE_RETURN_RED_FLUSH"), erpSaleReturnMapper, ErpSaleReturn::getOrderNo);
        Map<Long, ErpStockCount> stockCountDocs = stockCountDocMap(txns, tenantId);
        Map<Long, String> transferNos = orderNoMap(txns, tenantId, Set.of("STOCK_TRANSFER_OUT", "STOCK_TRANSFER_IN"), erpStockTransferMapper, ErpStockTransfer::getTransferNo);
        Map<Long, String> assemblyNos = orderNoMap(txns, tenantId, Set.of("ASSEMBLE_OUT", "ASSEMBLE_IN", "DISASSEMBLE_OUT", "DISASSEMBLE_IN"), erpAssemblyOrderMapper, ErpAssemblyOrder::getOrderNo);

        for (ErpStockTxn txn : txns) {
            String bizType = txn.getBizType();
            Long bizId = txn.getBizId();
            if (bizType == null || bizId == null) {
                txn.setDocNo(txn.getTxnNo());
            } else if (bizType.startsWith("PURCHASE_RETURN")) {
                txn.setDocNo(purchaseReturnNos.getOrDefault(bizId, txn.getTxnNo()));
            } else if (bizType.startsWith("PURCHASE")) {
                txn.setDocNo(purchaseNos.getOrDefault(bizId, txn.getTxnNo()));
            } else if (bizType.startsWith("SALE_RETURN")) {
                txn.setDocNo(saleReturnNos.getOrDefault(bizId, txn.getTxnNo()));
            } else if (bizType.startsWith("SALE")) {
                txn.setDocNo(saleNos.getOrDefault(bizId, txn.getTxnNo()));
            } else if (bizType.startsWith("STOCK_TRANSFER")) {
                txn.setDocNo(transferNos.getOrDefault(bizId, txn.getTxnNo()));
            } else if (bizType.startsWith("STOCK_")) {
                ErpStockCount doc = stockCountDocs.get(bizId);
                txn.setDocNo(doc == null ? txn.getTxnNo() : doc.getCountNo());
                if ("STOCK_COUNT".equals(bizType) && doc != null) {
                    txn.setAdjustmentReason(doc.getAdjustmentReason());
                }
            } else if (bizType.startsWith("ASSEMBLE") || bizType.startsWith("DISASSEMBLE")) {
                txn.setDocNo(assemblyNos.getOrDefault(bizId, txn.getTxnNo()));
            } else {
                txn.setDocNo(txn.getTxnNo());
            }
        }
    }

    private <T> Map<Long, String> orderNoMap(List<ErpStockTxn> txns,
                                             Long tenantId,
                                             Set<String> bizTypes,
                                             com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,
                                             java.util.function.Function<T, String> noGetter) {
        Set<Long> ids = txns.stream()
            .filter(txn -> bizTypes.contains(txn.getBizType()))
            .map(ErpStockTxn::getBizId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<T> docs = mapper.selectList(new QueryWrapper<T>()
            .eq("tenant_id", tenantId)
            .in("id", ids)
            .isNull("deleted_at"));
        Map<Long, String> result = new HashMap<>();
        for (T doc : docs) {
            if (doc instanceof ErpPurchaseOrder purchaseOrder) {
                result.put(purchaseOrder.getId(), noGetter.apply(doc));
            } else if (doc instanceof ErpSaleOrder saleOrder) {
                result.put(saleOrder.getId(), noGetter.apply(doc));
            } else if (doc instanceof ErpPurchaseReturn purchaseReturn) {
                result.put(purchaseReturn.getId(), noGetter.apply(doc));
            } else if (doc instanceof ErpSaleReturn saleReturn) {
                result.put(saleReturn.getId(), noGetter.apply(doc));
            } else if (doc instanceof ErpAssemblyOrder assemblyOrder) {
                result.put(assemblyOrder.getId(), noGetter.apply(doc));
            } else if (doc instanceof ErpStockTransfer transfer) {
                result.put(transfer.getId(), noGetter.apply(doc));
            }
        }
        return result;
    }

    private Map<Long, ErpStockCount> stockCountDocMap(List<ErpStockTxn> txns, Long tenantId) {
        Set<Long> ids = txns.stream()
            .filter(txn -> txn.getBizType() != null && txn.getBizType().startsWith("STOCK_"))
            .map(ErpStockTxn::getBizId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<ErpStockCount> docs = erpStockCountMapper.selectList(new QueryWrapper<ErpStockCount>()
            .eq("tenant_id", tenantId)
            .in("id", ids)
            .isNull("deleted_at"));
        Map<Long, ErpStockCount> result = new HashMap<>();
        for (ErpStockCount doc : docs) {
            result.put(doc.getId(), doc);
        }
        return result;
    }

    @Override
    public List<ErpStockBalanceOption> listBalancesByProduct(Long productId) {
        if (productId == null) {
            return List.of();
        }
        Long tenantId = TenantContext.requireTenantId();
        List<ErpStockBalance> balances = erpStockBalanceMapper.selectList(
            new QueryWrapper<ErpStockBalance>()
                .eq("tenant_id", tenantId)
                .eq("product_id", productId)
                .orderByDesc("updated_at")
        );
        if (balances.isEmpty()) {
            return List.of();
        }

        Set<Long> warehouseIds = balances.stream()
            .map(ErpStockBalance::getWarehouseId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Set<Long> locationIds = balances.stream()
            .map(ErpStockBalance::getLocationId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, String> warehouseNameMap = new HashMap<>();
        if (!warehouseIds.isEmpty()) {
            List<ErpWarehouse> warehouses = erpWarehouseMapper.selectBatchIds(warehouseIds);
            for (ErpWarehouse warehouse : warehouses) {
                warehouseNameMap.put(warehouse.getId(), warehouse.getName());
            }
        }

        Map<Long, String> locationNameMap = new HashMap<>();
        if (!locationIds.isEmpty()) {
            List<ErpLocation> locations = erpLocationMapper.selectBatchIds(locationIds);
            for (ErpLocation location : locations) {
                locationNameMap.put(location.getId(), location.getName());
            }
        }

        List<ErpStockBalanceOption> options = new ArrayList<>();
        for (ErpStockBalance balance : balances) {
            ErpStockBalanceOption option = new ErpStockBalanceOption();
            option.setWarehouseId(balance.getWarehouseId());
            option.setWarehouseName(warehouseNameMap.getOrDefault(balance.getWarehouseId(), "-"));
            option.setLocationId(balance.getLocationId());
            option.setLocationName(balance.getLocationId() == null
                ? "未指定库位"
                : locationNameMap.getOrDefault(balance.getLocationId(), "-"));
            BigDecimal onHand = balance.getQtyOnHand() == null ? BigDecimal.ZERO : balance.getQtyOnHand();
            BigDecimal locked = balance.getQtyLocked() == null ? BigDecimal.ZERO : balance.getQtyLocked();
            option.setQtyOnHand(onHand);
            option.setQtyAvailable(onHand.subtract(locked));
            option.setQtyLocked(locked);
            options.add(option);
        }
        return options;
    }

    @Override
    public List<ErpStockOccupancyView> listOccupancy(Long balanceId) {
        if (balanceId == null) {
            return List.of();
        }
        Long tenantId = TenantContext.requireTenantId();
        ErpStockBalance balance = erpStockBalanceMapper.selectOne(new QueryWrapper<ErpStockBalance>()
            .eq("tenant_id", tenantId)
            .eq("id", balanceId));
        if (balance == null) {
            return List.of();
        }
        List<ErpStockOccupancyView> rows = new ArrayList<>();
        rows.addAll(loadSaleOccupancy(tenantId, balance));
        rows.addAll(loadPurchaseReturnOccupancy(tenantId, balance));
        rows.addAll(loadAssemblyOccupancy(tenantId, balance));
        rows.sort((left, right) -> {
            int typeCompare = occupancyRank(left.docType()) - occupancyRank(right.docType());
            if (typeCompare != 0) return typeCompare;
            return right.orderAt() == null
                ? (left.orderAt() == null ? 0 : -1)
                : left.orderAt() == null ? 1 : right.orderAt().compareTo(left.orderAt());
        });
        return rows;
    }

    @Override
    public BigDecimal getQtyOnHand(Long productId, Long warehouseId, Long locationId) {
        if (productId == null) {
            return BigDecimal.ZERO;
        }
        QueryWrapper<ErpStockBalance> wrapper = new QueryWrapper<ErpStockBalance>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("product_id", productId);
        if (warehouseId != null) {
            wrapper.eq("warehouse_id", warehouseId);
        }
        if (locationId != null) {
            if (locationId < 0) {
                wrapper.isNull("location_id");
            } else {
                wrapper.eq("location_id", locationId);
            }
        }
        List<ErpStockBalance> balances = erpStockBalanceMapper.selectList(wrapper);
        BigDecimal total = BigDecimal.ZERO;
        for (ErpStockBalance balance : balances) {
            if (balance.getQtyOnHand() != null) {
                total = total.add(balance.getQtyOnHand());
            }
        }
        return total;
    }

    private List<ErpStockOccupancyView> loadSaleOccupancy(Long tenantId, ErpStockBalance balance) {
        return erpSaleOrderMapper.findStockOccupancy(
            tenantId,
            balance.getProductId(),
            balance.getWarehouseId(),
            balance.getLocationId()
        );
    }

    private List<ErpStockOccupancyView> loadPurchaseReturnOccupancy(Long tenantId, ErpStockBalance balance) {
        return erpPurchaseReturnMapper.findStockOccupancy(
            tenantId,
            balance.getProductId(),
            balance.getWarehouseId(),
            balance.getLocationId()
        );
    }

    private List<ErpStockOccupancyView> loadAssemblyOccupancy(Long tenantId, ErpStockBalance balance) {
        return erpAssemblyOrderMapper.findStockOccupancy(
            tenantId,
            balance.getProductId(),
            balance.getWarehouseId(),
            balance.getLocationId()
        );
    }

    private int occupancyRank(String docType) {
        if ("SALE_ORDER".equals(docType)) return 1;
        if ("PURCHASE_RETURN".equals(docType)) return 2;
        if ("ASSEMBLE".equals(docType)) return 3;
        return 99;
    }
}
