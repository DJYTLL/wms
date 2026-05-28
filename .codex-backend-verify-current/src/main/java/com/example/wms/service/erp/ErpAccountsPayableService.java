package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpAccountsPayableDetail;
import com.example.wms.dto.erp.ErpAccountsPayableView;

import java.time.Instant;
import java.util.List;

// ERP应付单服务
public interface ErpAccountsPayableService {
    List<ErpAccountsPayableView> listAll(String keyword, String status, Long supplierId, Instant startAt, Instant endAt);

    PageResponse<ErpAccountsPayableView> page(long page, long size, String keyword, String status, Long supplierId, Instant startAt, Instant endAt);

    ErpAccountsPayableDetail getDetail(Long id);
}
