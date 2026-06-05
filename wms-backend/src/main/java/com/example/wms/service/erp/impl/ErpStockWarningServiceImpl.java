package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockWarningView;
import com.example.wms.mapper.erp.ErpStockWarningMapper;
import com.example.wms.service.erp.ErpStockWarningService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

// Stock warning service implementation
@Service
public class ErpStockWarningServiceImpl implements ErpStockWarningService {
    private final ErpStockWarningMapper erpStockWarningMapper;

    public ErpStockWarningServiceImpl(ErpStockWarningMapper erpStockWarningMapper) {
        this.erpStockWarningMapper = erpStockWarningMapper;
    }

    @Override
    public PageResponse<ErpStockWarningView> page(long page,
                                                  long size,
                                                  String keyword,
                                                  Long warehouseId,
                                                  String status,
                                                  String policySource,
                                                  Boolean hasPolicyAnomaly) {
        Long tenantId = TenantContext.requireTenantId();
        Page<ErpStockWarningView> pageReq = Page.of(page, size);
        var result = erpStockWarningMapper.pageWarnings(
            pageReq, tenantId, keyword, warehouseId, status, policySource, hasPolicyAnomaly
        );
        normalize(result.getRecords());
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public PageResponse<ErpStockWarningView> pageAnomalies(long page,
                                                           long size,
                                                           String keyword,
                                                           Long warehouseId,
                                                           String anomalyType) {
        Long tenantId = TenantContext.requireTenantId();
        Page<ErpStockWarningView> pageReq = Page.of(page, size);
        var result = erpStockWarningMapper.pageAnomalies(pageReq, tenantId, keyword, warehouseId, anomalyType);
        normalize(result.getRecords());
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    private void normalize(List<ErpStockWarningView> records) {
        for (ErpStockWarningView record : records) {
            if (record.getHasPolicyAnomaly() == null) {
                record.setHasPolicyAnomaly(Boolean.FALSE);
            }
            String raw = record.getAnomalyTypesText();
            if (raw == null || raw.isBlank()) {
                record.setAnomalyTypes(List.of());
                continue;
            }
            record.setAnomalyTypes(Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList());
        }
    }
}
