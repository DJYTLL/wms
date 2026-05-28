package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockTransferCreateRequest;
import com.example.wms.dto.erp.ErpStockTransferDetail;
import com.example.wms.entity.erp.ErpStockTransfer;

public interface ErpStockTransferService {
    PageResponse<ErpStockTransfer> page(long page, long size, String keyword, String status, String startAt, String endAt);

    ErpStockTransferDetail getDetail(Long id);

    String nextTransferNo();

    ErpStockTransferDetail create(ErpStockTransferCreateRequest request);

    ErpStockTransferDetail update(Long id, ErpStockTransferCreateRequest request);

    void approve(Long id);

    void cancel(Long id);
}
