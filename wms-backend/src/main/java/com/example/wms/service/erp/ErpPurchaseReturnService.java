package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPurchaseReturnCreateRequest;
import com.example.wms.dto.erp.ErpPurchaseReturnDetail;
import com.example.wms.dto.erp.ErpPurchaseReturnUpdateRequest;
import com.example.wms.entity.erp.ErpPurchaseReturn;

import java.time.Instant;
import java.util.List;

// 采购退货服务接口（ERP进销存）
public interface ErpPurchaseReturnService {
    List<ErpPurchaseReturn> listAll(String keyword, String status, Long supplierId, Instant startAt, Instant endAt);

    PageResponse<ErpPurchaseReturn> page(long page, long size, String keyword, String status, Long supplierId, Instant startAt, Instant endAt);

    ErpPurchaseReturnDetail getDetail(Long id);

    String nextOrderNo();

    ErpPurchaseReturnDetail create(ErpPurchaseReturnCreateRequest request);

    ErpPurchaseReturnDetail update(Long id, ErpPurchaseReturnUpdateRequest request);

    void delete(Long id);

    void approve(Long id);
}
