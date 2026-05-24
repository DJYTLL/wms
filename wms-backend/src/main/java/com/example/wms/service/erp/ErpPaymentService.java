package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPaymentCreateRequest;
import com.example.wms.dto.erp.ErpPaymentDetail;
import com.example.wms.dto.erp.ErpPaymentSourcePayableDetail;
import com.example.wms.dto.erp.ErpPaymentSourcePayableOption;
import com.example.wms.dto.erp.ErpPaymentView;

import java.time.Instant;
import java.util.List;

// ERP付款单服务
public interface ErpPaymentService {
    List<ErpPaymentView> listAll(String keyword, String status, Long supplierId, Long payableId, Instant startAt, Instant endAt);

    PageResponse<ErpPaymentView> page(long page, long size, String keyword, String status, Long supplierId, Long payableId, Instant startAt, Instant endAt);

    PageResponse<ErpPaymentSourcePayableOption> sourcePayablePage(long page, long size, String keyword, Long supplierId, String status, Instant startAt, Instant endAt);

    ErpPaymentDetail getDetail(Long id);

    ErpPaymentSourcePayableDetail getSourcePayableDetail(Long id);

    String nextPaymentNo();

    ErpPaymentDetail create(ErpPaymentCreateRequest request);

    ErpPaymentDetail update(Long id, ErpPaymentCreateRequest request);

    ErpPaymentDetail approve(Long id);

    ErpPaymentDetail restoreSystemDefault(Long id);

    ErpPaymentDetail redFlush(Long id, String reason);
}
