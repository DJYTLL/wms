package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockWarningView;
import com.example.wms.mapper.erp.ErpStockWarningMapper;
import com.example.wms.service.erp.ErpStockWarningService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

// Stock warning service implementation
@Service
public class ErpStockWarningServiceImpl implements ErpStockWarningService {
    private final ErpStockWarningMapper erpStockWarningMapper;

    public ErpStockWarningServiceImpl(ErpStockWarningMapper erpStockWarningMapper) {
        this.erpStockWarningMapper = erpStockWarningMapper;
    }

    @Override
    public PageResponse<ErpStockWarningView> page(long page, long size, String keyword) {
        Long tenantId = TenantContext.requireTenantId();
        Page<ErpStockWarningView> pageReq = Page.of(page, size);
        var result = erpStockWarningMapper.pageWarnings(pageReq, tenantId, keyword);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }
}
