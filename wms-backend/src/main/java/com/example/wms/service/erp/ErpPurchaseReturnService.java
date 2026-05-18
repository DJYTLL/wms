package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPurchaseOrderRecentItem;
import com.example.wms.dto.erp.ErpPurchaseReturnCreateRequest;
import com.example.wms.dto.erp.ErpPurchaseReturnDetail;
import com.example.wms.dto.erp.ErpPurchaseReturnRefundSummary;
import com.example.wms.dto.erp.ErpPurchaseReturnSourcePurchaseOrderDetail;
import com.example.wms.dto.erp.ErpPurchaseReturnSourcePurchaseOrderOption;
import com.example.wms.dto.erp.ErpPurchaseReturnUpdateRequest;
import com.example.wms.entity.erp.ErpPurchaseReturn;

import java.time.Instant;
import java.util.List;

// 采购退货服务接口（ERP进销存）
public interface ErpPurchaseReturnService {
    List<ErpPurchaseReturn> listAll(String keyword, String status, Long supplierId, Instant startAt, Instant endAt);

    PageResponse<ErpPurchaseReturn> page(long page, long size, String keyword, String status, Long supplierId, Instant startAt, Instant endAt);

    PageResponse<ErpPurchaseReturn> draftPage(long page, long size, String keyword, Long supplierId, Instant startAt, Instant endAt);

    PageResponse<ErpPurchaseReturn> approvedPage(long page, long size, String keyword, Long supplierId, Instant startAt, Instant endAt);

    PageResponse<ErpPurchaseReturnSourcePurchaseOrderOption> sourcePurchaseOrderPage(long page, long size, String keyword, Long supplierId);

    PageResponse<ErpPurchaseOrderRecentItem> sourceRecentPurchaseItems(long page, long size, Long supplierId, Long productId);

    ErpPurchaseReturnDetail getDetail(Long id);

    ErpPurchaseReturnDetail getDraftDetail(Long id);

    ErpPurchaseReturnDetail getApprovedDetail(Long id);

    ErpPurchaseReturnSourcePurchaseOrderDetail getSourcePurchaseOrderDetail(Long purchaseOrderId);

    ErpPurchaseReturnRefundSummary getPurchaseOrderRefundSummary(Long purchaseOrderId);

    String nextOrderNo();

    ErpPurchaseReturnDetail create(ErpPurchaseReturnCreateRequest request);

    ErpPurchaseReturnDetail update(Long id, ErpPurchaseReturnUpdateRequest request);

    void delete(Long id);

    void approve(Long id);

    ErpPurchaseReturnDetail copyApproved(Long id);

    void cancel(Long id, String reason);
}
