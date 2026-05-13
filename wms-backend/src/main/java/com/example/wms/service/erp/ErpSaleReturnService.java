package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSaleReturnCreateRequest;
import com.example.wms.dto.erp.ErpSaleReturnDetail;
import com.example.wms.dto.erp.ErpSaleReturnUpdateRequest;
import com.example.wms.entity.erp.ErpSaleReturn;

import java.time.Instant;
import java.util.List;

public interface ErpSaleReturnService {
    List<ErpSaleReturn> listAll(String keyword, String status, Long customerId, Instant startAt, Instant endAt);

    PageResponse<ErpSaleReturn> page(long page, long size, String keyword, String status, Long customerId, Instant startAt, Instant endAt);

    List<ErpSaleReturn> listApprovedBySaleOrderId(Long saleOrderId);

    ErpSaleReturnDetail getDetail(Long id);

    String nextOrderNo();

    ErpSaleReturnDetail create(ErpSaleReturnCreateRequest request);

    ErpSaleReturnDetail update(Long id, ErpSaleReturnUpdateRequest request);

    void delete(Long id);

    void approve(Long id);

    void redFlush(Long id, String reason);
}
