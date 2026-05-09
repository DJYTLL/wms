package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpReceiptCreateRequest;
import com.example.wms.dto.erp.ErpReceiptDetail;
import com.example.wms.dto.erp.ErpReceiptView;

import java.time.Instant;
import java.util.List;

// ERP收款单服务
public interface ErpReceiptService {
    List<ErpReceiptView> listAll(String keyword, String status, Long customerId, Long receivableId, Instant startAt, Instant endAt);

    PageResponse<ErpReceiptView> page(long page, long size, String keyword, String status, Long customerId, Long receivableId, Instant startAt, Instant endAt);

    ErpReceiptDetail getDetail(Long id);

    String nextReceiptNo();

    ErpReceiptDetail create(ErpReceiptCreateRequest request);

    ErpReceiptDetail update(Long id, ErpReceiptCreateRequest request);

    ErpReceiptDetail approve(Long id);

    ErpReceiptDetail redFlush(Long id, String reason);
}
