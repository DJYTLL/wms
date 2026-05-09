package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSettlementMethodCreateRequest;
import com.example.wms.dto.erp.ErpSettlementMethodUpdateRequest;
import com.example.wms.entity.erp.ErpSettlementMethod;

import java.util.List;

// 结算方式服务接口（ERP进销存）
public interface ErpSettlementMethodService {
    List<ErpSettlementMethod> listAll(String keyword, Boolean enabled);

    PageResponse<ErpSettlementMethod> page(long page, long size, String keyword, Boolean enabled);

    ErpSettlementMethod getById(Long id);

    ErpSettlementMethod create(ErpSettlementMethodCreateRequest request);

    ErpSettlementMethod update(Long id, ErpSettlementMethodUpdateRequest request);

    void delete(Long id);
}
