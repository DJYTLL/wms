package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpLocationCreateRequest;
import com.example.wms.dto.erp.ErpLocationUpdateRequest;
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
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpAssemblyOrderItemMapper;
import com.example.wms.mapper.erp.ErpAssemblyOrderMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.mapper.erp.ErpProductMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderItemMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnItemMapper;
import com.example.wms.mapper.erp.ErpSaleOrderItemMapper;
import com.example.wms.mapper.erp.ErpSaleReturnItemMapper;
import com.example.wms.mapper.erp.ErpStockBalanceMapper;
import com.example.wms.mapper.erp.ErpStockCountItemMapper;
import com.example.wms.mapper.erp.ErpStockCountMapper;
import com.example.wms.mapper.erp.ErpStockTxnMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.ErpLocationService;
import com.example.wms.service.erp.support.ErpMasterDataRules;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

// 库位服务实现（ERP进销存）
@Service
public class ErpLocationServiceImpl implements ErpLocationService {
    private static final String LOCATION_CODE_TYPE = "LOCATION";

    private final ErpLocationMapper erpLocationMapper;
    private final ErpWarehouseMapper erpWarehouseMapper;
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

    public ErpLocationServiceImpl(ErpLocationMapper erpLocationMapper,
                                  ErpWarehouseMapper erpWarehouseMapper,
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
        this.erpLocationMapper = erpLocationMapper;
        this.erpWarehouseMapper = erpWarehouseMapper;
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
    public List<ErpLocation> listAll(String keyword, Boolean enabled, Long warehouseId) {
        QueryWrapper<ErpLocation> wrapper = baseWrapper(keyword, enabled, warehouseId);
        wrapper.orderByAsc("id");
        return erpLocationMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpLocation> page(long page, long size, String keyword, Boolean enabled, Long warehouseId) {
        Page<ErpLocation> pageReq = Page.of(page, size);
        QueryWrapper<ErpLocation> wrapper = baseWrapper(keyword, enabled, warehouseId);
        wrapper.orderByAsc("id");
        Page<ErpLocation> result = erpLocationMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpLocation getById(Long id) {
        ErpLocation location = erpLocationMapper.selectOne(new QueryWrapper<ErpLocation>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (location == null) {
            throw new IllegalArgumentException("库位不存在");
        }
        return location;
    }

    @Override
    public String nextCode() {
        Long tenantId = TenantContext.requireTenantId();
        return generateLocationCode(tenantId);
    }

    @Override
    @AuditLog(action = "ERP_LOCATION_CREATE", entityType = "erp_location", entityId = "{result.id}", detail = "code={arg0.code}")
    public ErpLocation create(ErpLocationCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String normalizedCode = ErpMasterDataRules.normalizeMasterCode(request.code(), "库位编码不能为空");
        ensureWarehouseExists(tenantId, request.warehouseId());
        ErpLocation existing = erpLocationMapper.findByCode(tenantId, request.warehouseId(), normalizedCode);
        if (existing != null) {
            throw new IllegalArgumentException("库位编码已存在");
        }
        ErpLocation location = new ErpLocation();
        location.setTenantId(tenantId);
        applyRequest(location, request, normalizedCode);
        location.setEnabled(request.enabled() == null || request.enabled());
        location.setCreatedAt(Instant.now());
        location.setUpdatedAt(Instant.now());
        erpLocationMapper.insert(location);
        return location;
    }

    @Override
    @AuditLog(action = "ERP_LOCATION_UPDATE", entityType = "erp_location", entityId = "{arg0}", detail = "code={arg1.code}")
    public ErpLocation update(Long id, ErpLocationUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String normalizedCode = ErpMasterDataRules.normalizeMasterCode(request.code(), "库位编码不能为空");
        ErpLocation location = erpLocationMapper.selectOne(new QueryWrapper<ErpLocation>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (location == null) {
            throw new IllegalArgumentException("库位不存在");
        }
        ensureWarehouseExists(tenantId, request.warehouseId());
        ErpLocation existing = erpLocationMapper.findByCode(tenantId, request.warehouseId(), normalizedCode);
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("库位编码已存在");
        }
        if (Boolean.FALSE.equals(request.enabled())) {
            ensureLocationCanDisable(tenantId, id);
        }
        applyRequest(location, request, normalizedCode);
        if (request.enabled() != null) {
            location.setEnabled(request.enabled());
        }
        location.setUpdatedAt(Instant.now());
        erpLocationMapper.updateById(location);
        return location;
    }

    @Override
    @AuditLog(action = "ERP_LOCATION_DELETE", entityType = "erp_location", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpLocation location = erpLocationMapper.selectOne(new QueryWrapper<ErpLocation>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (location == null) {
            throw new IllegalArgumentException("库位不存在");
        }
        ensureLocationNotReferenced(tenantId, id);
        erpLocationMapper.deleteById(id);
    }

    private QueryWrapper<ErpLocation> baseWrapper(String keyword, Boolean enabled, Long warehouseId) {
        QueryWrapper<ErpLocation> wrapper = new QueryWrapper<ErpLocation>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        if (warehouseId != null) {
            wrapper.eq("warehouse_id", warehouseId);
        }
        return wrapper;
    }

    private void ensureWarehouseExists(Long tenantId, Long warehouseId) {
        ErpWarehouse warehouse = erpWarehouseMapper.selectOne(new QueryWrapper<ErpWarehouse>()
            .eq("tenant_id", tenantId)
            .eq("id", warehouseId));
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
    }

    private void applyRequest(ErpLocation location, ErpLocationCreateRequest request, String normalizedCode) {
        location.setCode(normalizedCode);
        location.setName(ErpMasterDataRules.normalizeOptionalText(request.name()));
        location.setWarehouseId(request.warehouseId());
        location.setAisle(ErpMasterDataRules.normalizeOptionalText(request.aisle()));
        location.setRack(ErpMasterDataRules.normalizeOptionalText(request.rack()));
        location.setBin(ErpMasterDataRules.normalizeOptionalText(request.bin()));
        location.setRemark(ErpMasterDataRules.normalizeOptionalText(request.remark()));
    }

    private void applyRequest(ErpLocation location, ErpLocationUpdateRequest request, String normalizedCode) {
        location.setCode(normalizedCode);
        location.setName(ErpMasterDataRules.normalizeOptionalText(request.name()));
        location.setWarehouseId(request.warehouseId());
        location.setAisle(ErpMasterDataRules.normalizeOptionalText(request.aisle()));
        location.setRack(ErpMasterDataRules.normalizeOptionalText(request.rack()));
        location.setBin(ErpMasterDataRules.normalizeOptionalText(request.bin()));
        location.setRemark(ErpMasterDataRules.normalizeOptionalText(request.remark()));
    }

    private void ensureLocationNotReferenced(Long tenantId, Long locationId) {
        if (erpProductMapper.selectCount(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", tenantId)
            .eq("default_location_id", locationId)) > 0) {
            throw new IllegalArgumentException("库位已被商品默认库位引用，不能删除");
        }
        if (erpStockBalanceMapper.selectCount(new QueryWrapper<ErpStockBalance>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)) > 0) {
            throw new IllegalArgumentException("库位仍有关联库存，不能删除");
        }
        if (erpPurchaseOrderItemMapper.selectCount(new QueryWrapper<ErpPurchaseOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)) > 0) {
            throw new IllegalArgumentException("库位已被采购单引用，不能删除");
        }
        if (erpPurchaseReturnItemMapper.selectCount(new QueryWrapper<ErpPurchaseReturnItem>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)) > 0) {
            throw new IllegalArgumentException("库位已被采购退货单引用，不能删除");
        }
        if (erpSaleOrderItemMapper.selectCount(new QueryWrapper<ErpSaleOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)) > 0) {
            throw new IllegalArgumentException("库位已被销售单引用，不能删除");
        }
        if (erpSaleReturnItemMapper.selectCount(new QueryWrapper<ErpSaleReturnItem>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)) > 0) {
            throw new IllegalArgumentException("库位已被销售退货单引用，不能删除");
        }
        if (erpAssemblyOrderMapper.selectCount(new QueryWrapper<ErpAssemblyOrder>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)) > 0) {
            throw new IllegalArgumentException("库位已被组装/拆分单引用，不能删除");
        }
        if (erpAssemblyOrderItemMapper.selectCount(new QueryWrapper<ErpAssemblyOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)) > 0) {
            throw new IllegalArgumentException("库位已被组装明细引用，不能删除");
        }
        if (erpStockCountMapper.selectCount(new QueryWrapper<ErpStockCount>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)) > 0) {
            throw new IllegalArgumentException("库位已被盘点单引用，不能删除");
        }
        if (erpStockCountItemMapper.selectCount(new QueryWrapper<ErpStockCountItem>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)) > 0) {
            throw new IllegalArgumentException("库位已被盘点明细引用，不能删除");
        }
        if (erpStockTxnMapper.selectCount(new QueryWrapper<ErpStockTxn>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)) > 0) {
            throw new IllegalArgumentException("库位已被库存流水引用，不能删除");
        }
    }

    private void ensureLocationCanDisable(Long tenantId, Long locationId) {
        if (erpProductMapper.selectCount(new QueryWrapper<ErpProduct>()
            .eq("tenant_id", tenantId)
            .eq("default_location_id", locationId)) > 0) {
            throw new IllegalArgumentException("库位仍被商品设为默认库位，不能停用");
        }
        if (erpPurchaseOrderItemMapper.selectCount(new QueryWrapper<ErpPurchaseOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)
            .inSql("order_id", draftOrderSubquery("erp_purchase_order", tenantId))) > 0) {
            throw new IllegalArgumentException("库位仍被未完成采购单引用，不能停用");
        }
        if (erpPurchaseReturnItemMapper.selectCount(new QueryWrapper<ErpPurchaseReturnItem>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)
            .inSql("order_id", draftOrderSubquery("erp_purchase_return", tenantId))) > 0) {
            throw new IllegalArgumentException("库位仍被未完成采购退货单引用，不能停用");
        }
        if (erpSaleOrderItemMapper.selectCount(new QueryWrapper<ErpSaleOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)
            .inSql("order_id", draftOrderSubquery("erp_sale_order", tenantId))) > 0) {
            throw new IllegalArgumentException("库位仍被未完成销售单引用，不能停用");
        }
        if (erpSaleReturnItemMapper.selectCount(new QueryWrapper<ErpSaleReturnItem>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)
            .inSql("order_id", draftOrderSubquery("erp_sale_return", tenantId))) > 0) {
            throw new IllegalArgumentException("库位仍被未完成销售退货单引用，不能停用");
        }
        if (erpAssemblyOrderMapper.selectCount(new QueryWrapper<ErpAssemblyOrder>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)
            .in("status", ErpMasterDataRules.PENDING_ORDER_STATUSES)) > 0) {
            throw new IllegalArgumentException("库位仍被未完成组装/拆分单引用，不能停用");
        }
        if (erpAssemblyOrderItemMapper.selectCount(new QueryWrapper<ErpAssemblyOrderItem>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)
            .inSql("order_id", draftOrderSubquery("erp_assembly_order", tenantId))) > 0) {
            throw new IllegalArgumentException("库位仍被未完成组装/拆分明细引用，不能停用");
        }
        if (erpStockCountMapper.selectCount(new QueryWrapper<ErpStockCount>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)
            .in("status", ErpMasterDataRules.PENDING_ORDER_STATUSES)) > 0) {
            throw new IllegalArgumentException("库位仍被未完成盘点单引用，不能停用");
        }
        if (erpStockCountItemMapper.selectCount(new QueryWrapper<ErpStockCountItem>()
            .eq("tenant_id", tenantId)
            .eq("location_id", locationId)
            .inSql("count_id", draftStockCountSubquery(tenantId))) > 0) {
            throw new IllegalArgumentException("库位仍被未完成盘点明细引用，不能停用");
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

    private String generateLocationCode(Long tenantId) {
        String prefix = readConfig("erp.location.code.prefix", "LO");
        String dateFormat = readConfig("erp.location.code.date-format", "yyyyMMdd");
        int seqLength = readIntConfig("erp.location.code.seq-length", 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, LOCATION_CODE_TYPE, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, LOCATION_CODE_TYPE, dateKey);
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
