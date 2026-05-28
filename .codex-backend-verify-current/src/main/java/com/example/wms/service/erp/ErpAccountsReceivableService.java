package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpAccountsReceivableDetail;
import com.example.wms.dto.erp.ErpAccountsReceivableView;

import java.time.Instant;
import java.util.List;

// ERP应收单服务
public interface ErpAccountsReceivableService {
    List<ErpAccountsReceivableView> listAll(String keyword, String status, Long customerId, Instant startAt, Instant endAt);

    PageResponse<ErpAccountsReceivableView> page(long page, long size, String keyword, String status, Long customerId, Instant startAt, Instant endAt);

    ErpAccountsReceivableDetail getDetail(Long id);
}
