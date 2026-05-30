package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpWarehouseCreateRequest;
import com.example.wms.dto.erp.ErpWarehouseUpdateRequest;
import com.example.wms.entity.SystemConfig;
import com.example.wms.entity.erp.ErpAssemblyOrder;
import com.example.wms.entity.erp.ErpAssemblyOrderItem;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpPurchaseOrderItem;
import com.example.wms.entity.erp.ErpPurchaseReturnItem;
import com.example.wms.entity.erp.ErpSaleOrderItem;
import com.example.wms.entity.erp.ErpSaleReturnItem;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockCount;
import com.example.wms.entity.erp.ErpStockCountItem;
import com.example.wms.entity.erp.ErpStockTxn;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.mapper.erp.ErpAssemblyOrderItemMapper;
import com.example.wms.mapper.erp.ErpAssemblyOrderMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderItemMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnItemMapper;
import com.example.wms.mapper.erp.ErpSaleOrderItemMapper;
import com.example.wms.mapper.erp.ErpSaleReturnItemMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockCountItemMapper;
import com.example.wms.mapper.erp.ErpStockCountMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.service.erp.ErpWarehouseService;
import com.example.wms.service.erp.support.ErpMasterDataRules;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

// 仓库服务实现（ERP进销存）
@Service
public class ErpWarehouseServiceImpl implements ErpWarehouseService {
    private static final String WAREHOUSE_CODE_TYPE = "WAREHOUSE";

    private final ErpWarehouseMapper erpWarehouseMapper;
    private final ErpLocationMapper erpLocationMapper;
    private final ErpProductMapper erpProductMapper;
    private final ErpStockBalanceMapper erpStockBalanceMapper;
    private final ErpPurchaseOrderItemMapper erpPurchaseOrderItemMapper;
    private final ErpPurchaseReturnItemMapper erpPurchaseReturnItemMapper;
    private final ErpSaleOrderItemMapper erpSaleOrderItemMapper;
    private final ErpSaleReturnItemMapper erpSaleReturnItemMapper;
    private final ErpAssemblyOrderMapper erpAssemblyOrderMapper;
    private final ErpAssemblyOrderItemMapper erpAssemblyOrderItemMapper;
    private final ErpStockCountMapper erpStockCountMapper;
    private final ErpStockCountItemMapper erpStockCountItemMapper;
    private final ErpStockTxnMapper erpStockTxnMapper;
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;

    public ErpWarehouseServiceImpl(ErpWarehouseMapper erpWarehouseMapper,
                                   ErpLocationMapper erpLocationMapper,
                                   ErpProductMapper erpProductMapper,
                                   ErpStockBalanceMapper erpStockBalanceMapper,
                                   ErpPurchaseOrderItemMapper erpPurchaseOrderItemMapper,
                                   ErpPurchaseReturnItemMapper erpPurchaseReturnItemMapper,
                                   ErpSaleOrderItemMapper erpSaleOrderItemMapper,
                                   ErpSaleReturnItemMapper erpSaleReturnItemMapper,
                                   ErpAssemblyOrderMapper erpAssemblyOrderMapper,
                                   ErpAssemblyOrderItemMapper erpAssemblyOrderItemMapper,
                                   ErpStockCountMapper erpStockCountMapper,
                                   ErpStockCountItemMapper erpStockCountItemMapper,
                                   ErpStockTxnMapper erpStockTxnMapper,
                                   ErpOrderSequenceMapper erpOrderSequenceMapper,
                                   SystemConfigMapper systemConfigMapper) {
        this.erpWarehouseMapper = erpWarehouseMapper;
        this.erpLocationMapper = erpLocationMapper;
        this.erpProductMapper = erpProductMapper;
        this.erpStockBalanceMapper = erpStockBalanceMapper;
        this.erpPurchaseOrderItemMapper = erpPurchaseOrderItemMapper;
        this.erpPurchaseReturnItemMapper = erpPurchaseReturnItemMapper;
        this.erpSaleOrderItemMapper = erpSaleOrderItemMapper;
        this.erpSaleReturnItemMapper = erpSaleReturnItemMapper;
        this.erpAssemblyOrderMapper = erpAssemblyOrderMapper;
        this.erpAssemblyOrderItemMapper = erpAssemblyOrderItemMapper;
        this.erpStockCountMapper = erpStockCountMapper;
        this.erpStockCountItemMapper = erpStockCountItemMapper;
        this.erpStockTxnMapper = erpStockTxnMapper;
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
    }

    @Override
    public List<ErpWarehouse> listAll(String keyword, Boolean enabled) {
        QueryWrapper<ErpWarehouse> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByDesc("is_default").orderByAsc("id");
        return erpWarehouseMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpWarehouse> page(long page, long size, String keyword, Boolean enabled) {
        Page<ErpWarehouse> pageReq = Page.of(page, size);
        QueryWrapper<ErpWarehouse> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByDesc("is_default").orderByAsc("id");
        Page<ErpWarehouse> result = erpWarehouseMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpWarehouse getById(Long id) {
        ErpWarehouse warehouse = erpWarehouseMapper.selectOne(new QueryWrapper<ErpWarehouse>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        return warehouse;
    }

    @Override
    public String nextCode() {
        Long tenantId = TenantContext.requireTenantId();
        return generateWarehouseCode(tenantId);
    }

    @Override
    @AuditLog(action = "ERP_WAREHOUSE_CREATE", entityType = "erp_warehouse", entityId = "{result.id}", detail = "code={arg0.code}")
@Transactional
    public ErpWarehouse create(ErpWarehouseCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String normalizedCode = ErpMasterDataRules.normalizeMasterCode(request.code(), "仓库编码不能为空");
        ErpWarehouse existing = erpWarehouseMapper.findByCode(tenantId, normalizedCode);
        if (existing != null) {
            throw new IllegalArgumentException("仓库编码已存在");
        }
        ErpWarehouse warehouse = new ErpWarehouse();
        warehouse.setTenantId(tenantId);
        applyRequest(warehouse, request, normalizedCode);
        warehouse.setEnabled(request.enabled() == null || request.enabled());
        warehouse.setCreatedAt(Instant.now());
        warehouse.setUpdatedAt(Instant.now());
        if (Boolean.TRUE.equals(request.isDefault())) {
            erpWarehouseMapper.clearDefault(tenantId, null);
        }
        erpWarehouseMapper.insert(warehouse);
        return warehouse;
    }

    @Override
    @AuditLog(action = "ERP_WAREHOUSE_UPDATE", entityType = "erp_warehouse", entityId = "{arg0}", detail = "code={arg1.code}")
@Transactional
    public ErpWarehouse update(Long id, ErpWarehouseUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String normalizedCode = ErpMasterDataRules.normalizeMasterCode(request.code(), "仓库编码不能为空");
        ErpWarehouse warehouse = erpWarehouseMapper.selectOne(new QueryWrapper<ErpWarehouse>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        ErpWarehouse existing = erpWarehouseMapper.findByCode(tenantId, normalizedCode);
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("仓库编码已存在");
        }
        if (Boolean.FALSE.equals(request.enabled())) {
            ensureWarehouseCanDisable(tenantId, id);
        }
        applyRequest(warehouse, request, normalizedCode);
        if (request.enabled() != null) {
            warehouse.setEnabled(request.enabled());
        }
        warehouse.setUpdatedAt(Instant.now());
        handleDefault(tenantId, warehouse.getId(), request.isDefault());
        erpWarehouseMapper.updateById(warehouse);
        return warehouse;
    }

    @Override
    @AuditLog(action = "ERP_WAREHOUSE_DELETE", entityType = "erp_warehouse", entityId = "{arg0}")
@Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpWarehouse warehouse = erpWarehouseMapper.selectOne(new QueryWrapper<ErpWarehouse>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        ensureWarehouseNotReferenced(tenantId, id);
        erpWarehouseMapper.deleteById(id);
    }

    private QueryWrapper<ErpWarehouse> baseWrapper(String keyword, Boolean enabled) {
        QueryWrapper<ErpWarehouse> wrapper = new QueryWrapper<ErpWarehouse>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword)
                .or()
                .like("manager", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        return wrapper;
    }

    private void applyRequest(ErpWarehouse warehouse, ErpWarehouseCreateRequest request, String normalizedCode) {
        warehouse.setCode(normalizedCode);
        warehouse.setName(ErpMasterDataRules.normalizeOptionalText(request.name()));
        warehouse.setAddress(ErpMasterDataRules.normalizeOptionalText(request.address()));
        warehouse.setManager(ErpMasterDataRules.normalizeOptionalText(request.manager()));
        warehouse.setPhone(ErpMasterDataRules.normalizeOptionalText(request.phone()));
        warehouse.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        warehouse.setRemark(ErpMasterDataRules.normalizeOptionalText(request.remark()));
    }

    private void applyRequest(ErpWarehouse warehouse, ErpWarehouseUpdateRequest request, String normalizedCode) {
        warehouse.setCode(normalizedCode);
        warehouse.setName(ErpMasterDataRules.normalizeOptionalText(request.name()));
        warehouse.setAddress(ErpMasterDataRules.normalizeOptionalText(request.address()));
        warehouse.setManager(ErpMasterDataRules.normalizeOptionalText(request.manager()));
        warehouse.setPhone(ErpMasterDataRules.normalizeOptionalText(request.phone()));
        if (request.isDefault() != null) {
            warehouse.setIsDefault(request.isDefault());
        }
        warehouse.setRemark(ErpMasterDataRules.normalizeOptionalText(request.remark()));
    }

    private void handleDefault(Long tenantId, Long warehouseId, Boolean isDefault) {
        if (Boolean.TRUE.equals(isDefault)) {
            erpWarehouseMapper.clearDefault(tenantId, warehouseId);
        }
    }

    private void ensureWarehouseNotReferenced(Long tenantId, Long warehouseId) {
        if (erpLocationMapper.selectCount(new QueryWrapper<ErpLocation>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)) > 0) {
            throw new IllegalArgumentException("仓库下仍有关联库位，不能删除");
        }
        if (erpProductMapper.selectCount(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", tenantId)
            .eq("default_warehouse_id", warehouseId)) > 0) {
            throw new IllegalArgumentException("仓库已被商品默认仓库引用，不能删除");
        }
        if (erpStockBalanceMapper.selectCount(new QueryWrapper<ErpStockBalance>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)) > 0) {
            throw new IllegalArgumentException("仓库仍有关联库存，不能删除");
        }
        if (erpPurchaseOrderItemMapper.selectCount(new QueryWrapper<ErpPurchaseOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)) > 0) {
            throw new IllegalArgumentException("仓库已被采购单引用，不能删除");
        }
        if (erpPurchaseReturnItemMapper.selectCount(new QueryWrapper<ErpPurchaseReturnItem>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)) > 0) {
            throw new IllegalArgumentException("仓库已被采购退货单引用，不能删除");
        }
        if (erpSaleOrderItemMapper.selectCount(new QueryWrapper<ErpSaleOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)) > 0) {
            throw new IllegalArgumentException("仓库已被销售单引用，不能删除");
        }
        if (erpSaleReturnItemMapper.selectCount(new QueryWrapper<ErpSaleReturnItem>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)) > 0) {
            throw new IllegalArgumentException("仓库已被销售退货单引用，不能删除");
        }
        if (erpAssemblyOrderMapper.selectCount(new QueryWrapper<ErpAssemblyOrder>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)) > 0) {
            throw new IllegalArgumentException("仓库已被组装/拆分单引用，不能删除");
        }
        if (erpAssemblyOrderItemMapper.selectCount(new QueryWrapper<ErpAssemblyOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)) > 0) {
            throw new IllegalArgumentException("仓库已被组装明细引用，不能删除");
        }
        if (erpStockCountMapper.selectCount(new QueryWrapper<ErpStockCount>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)) > 0) {
            throw new IllegalArgumentException("仓库已被盘点单引用，不能删除");
        }
        if (erpStockCountItemMapper.selectCount(new QueryWrapper<ErpStockCountItem>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)) > 0) {
            throw new IllegalArgumentException("仓库已被盘点明细引用，不能删除");
        }
        if (erpStockTxnMapper.selectCount(new QueryWrapper<ErpStockTxn>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)) > 0) {
            throw new IllegalArgumentException("仓库已被库存流水引用，不能删除");
        }
    }

    private void ensureWarehouseCanDisable(Long tenantId, Long warehouseId) {
        if (erpLocationMapper.selectCount(new QueryWrapper<ErpLocation>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)
            .eq("is_enabled", true)) > 0) {
            throw new IllegalArgumentException("仓库下仍有启用库位，不能停用");
        }
        if (erpProductMapper.selectCount(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", tenantId)
            .eq("default_warehouse_id", warehouseId)) > 0) {
            throw new IllegalArgumentException("仓库仍被商品设为默认仓库，不能停用");
        }
        if (erpPurchaseOrderItemMapper.selectCount(new QueryWrapper<ErpPurchaseOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)
            .inSql("order_id", draftOrderSubquery("erp_purchase_order", tenantId))) > 0) {
            throw new IllegalArgumentException("仓库仍被未完成采购单引用，不能停用");
        }
        if (erpPurchaseReturnItemMapper.selectCount(new QueryWrapper<ErpPurchaseReturnItem>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)
            .inSql("order_id", draftOrderSubquery("erp_purchase_return", tenantId))) > 0) {
            throw new IllegalArgumentException("仓库仍被未完成采购退货单引用，不能停用");
        }
        if (erpSaleOrderItemMapper.selectCount(new QueryWrapper<ErpSaleOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)
            .inSql("order_id", draftOrderSubquery("erp_sale_order", tenantId))) > 0) {
            throw new IllegalArgumentException("仓库仍被未完成销售单引用，不能停用");
        }
        if (erpSaleReturnItemMapper.selectCount(new QueryWrapper<ErpSaleReturnItem>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)
            .inSql("order_id", draftOrderSubquery("erp_sale_return", tenantId))) > 0) {
            throw new IllegalArgumentException("仓库仍被未完成销售退货单引用，不能停用");
        }
        if (erpAssemblyOrderMapper.selectCount(new QueryWrapper<ErpAssemblyOrder>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)
            .in("status", ErpMasterDataRules.PENDING_ORDER_STATUSES)) > 0) {
            throw new IllegalArgumentException("仓库仍被未完成组装/拆分单引用，不能停用");
        }
        if (erpAssemblyOrderItemMapper.selectCount(new QueryWrapper<ErpAssemblyOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)
            .inSql("order_id", draftOrderSubquery("erp_assembly_order", tenantId))) > 0) {
            throw new IllegalArgumentException("仓库仍被未完成组装/拆分明细引用，不能停用");
        }
        if (erpStockCountMapper.selectCount(new QueryWrapper<ErpStockCount>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)
            .in("status", ErpMasterDataRules.PENDING_ORDER_STATUSES)) > 0) {
            throw new IllegalArgumentException("仓库仍被未完成盘点单引用，不能停用");
        }
        if (erpStockCountItemMapper.selectCount(new QueryWrapper<ErpStockCountItem>()
            .eq("tenant_id", tenantId)
            .eq("warehouse_id", warehouseId)
            .inSql("count_id", draftStockCountSubquery(tenantId))) > 0) {
            throw new IllegalArgumentException("仓库仍被未完成盘点明细引用，不能停用");
        }
    }

    private String draftOrderSubquery(String tableName, Long tenantId) {
        return String.format(
            "SELECT id FROM %s WHERE tenant_id = %d AND status IN (%s) AND deleted_at IS NULL",
            tableName,
            tenantId,
            ErpMasterDataRules.pendingStatusSqlList()
        );
    }

    private String draftStockCountSubquery(Long tenantId) {
        return String.format(
            "SELECT id FROM erp_stock_count WHERE tenant_id = %d AND status IN (%s) AND deleted_at IS NULL",
            tenantId,
            ErpMasterDataRules.pendingStatusSqlList()
        );
    }

    private String generateWarehouseCode(Long tenantId) {
        String prefix = readConfig("erp.warehouse.code.prefix", "WH");
        String dateFormat = readConfig("erp.warehouse.code.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.warehouse.code.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, WAREHOUSE_CODE_TYPE, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, WAREHOUSE_CODE_TYPE, dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private String readConfig(String key, String fallback) {
        SystemConfig config = systemConfigMapper.findByKey(TenantContext.requireTenantId(), key);
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
}
