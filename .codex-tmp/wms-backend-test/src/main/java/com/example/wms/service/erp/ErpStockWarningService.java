package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockWarningView;

// Stock warning service
public interface ErpStockWarningService {
    PageResponse<ErpStockWarningView> page(long page, long size, String keyword);
}
