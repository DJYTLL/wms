package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSaleReturnCreateRequest;
import com.example.wms.dto.erp.ErpSaleReturnDetail;
import com.example.wms.dto.erp.ErpSaleOrderRecentItem;
import com.example.wms.dto.erp.ErpSaleReturnRefundSummary;
import com.example.wms.dto.erp.ErpSaleReturnSourceSaleOrderDetail;
import com.example.wms.dto.erp.ErpSaleReturnSourceSaleOrderOption;
import com.example.wms.dto.erp.ErpSaleReturnUpdateRequest;
import com.example.wms.entity.erp.ErpSaleReturn;

import java.time.Instant;
import java.util.List;

public interface ErpSaleReturnService {
    List<ErpSaleReturn> listAll(String keyword, String status, Long customerId, Instant startAt, Instant endAt);

    PageResponse<ErpSaleReturn> page(long page, long size, String keyword, String status, Long customerId, Instant startAt, Instant endAt);

    PageResponse<ErpSaleReturn> draftPage(long page, long size, String keyword, Long customerId, Instant startAt, Instant endAt);

    PageResponse<ErpSaleReturn> approvedPage(long page, long size, String keyword, String status, Long customerId, Instant startAt, Instant endAt);

    PageResponse<ErpSaleReturnSourceSaleOrderOption> sourceSaleOrderPage(long page, long size, String keyword, Long customerId, Long currentReturnId);

    PageResponse<ErpSaleOrderRecentItem> sourceRecentSaleItems(long page, long size, Long customerId, Long productId, Long currentReturnId);

    List<ErpSaleReturn> listBySaleOrderId(Long saleOrderId, boolean includeDraft);

    ErpSaleReturnDetail getDetail(Long id);

    ErpSaleReturnDetail getDraftDetail(Long id);

    ErpSaleReturnDetail getApprovedDetail(Long id);

    ErpSaleReturnSourceSaleOrderDetail getSourceSaleOrderDetail(Long saleOrderId, Long currentReturnId);

    ErpSaleReturnRefundSummary getSaleOrderRefundSummary(Long saleOrderId);

    String nextOrderNo();

    ErpSaleReturnDetail create(ErpSaleReturnCreateRequest request);

    ErpSaleReturnDetail copyToDraft(Long id);

    ErpSaleReturnDetail update(Long id, ErpSaleReturnUpdateRequest request);

    void delete(Long id);

    void approve(Long id);

    void redFlush(Long id, String reason);
}
