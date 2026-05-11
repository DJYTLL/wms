package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockBalanceOption;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockTxn;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
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

    public ErpStockServiceImpl(ErpStockBalanceMapper erpStockBalanceMapper,
                               ErpStockTxnMapper erpStockTxnMapper,
                               ErpWarehouseMapper erpWarehouseMapper,
                               ErpLocationMapper erpLocationMapper) {
        this.erpStockBalanceMapper = erpStockBalanceMapper;
        this.erpStockTxnMapper = erpStockTxnMapper;
        this.erpWarehouseMapper = erpWarehouseMapper;
        this.erpLocationMapper = erpLocationMapper;
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
        for (ErpStockBalance balance : result.getRecords()) {
            BigDecimal onHand = balance.getQtyOnHand() == null ? BigDecimal.ZERO : balance.getQtyOnHand();
            BigDecimal locked = BigDecimal.ZERO;
            balance.setQtyLocked(locked);
            balance.setQtyAvailable(onHand.subtract(locked));
        }
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
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
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
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
            option.setQtyOnHand(onHand);
            option.setQtyAvailable(onHand);
            option.setQtyLocked(BigDecimal.ZERO);
            options.add(option);
        }
        return options;
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
}
