package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPaymentMethodCreateRequest;
import com.example.wms.dto.erp.ErpPaymentMethodUpdateRequest;
import com.example.wms.entity.erp.ErpReceiptMethod;

import java.util.List;

// 收款方式服务接口（ERP进销存）
public interface ErpReceiptMethodService {
    List<ErpReceiptMethod> listAll(String keyword, Boolean enabled);

    PageResponse<ErpReceiptMethod> page(long page, long size, String keyword, Boolean enabled);

    ErpReceiptMethod getById(Long id);

    String nextCode();

    ErpReceiptMethod create(ErpPaymentMethodCreateRequest request);

    ErpReceiptMethod update(Long id, ErpPaymentMethodUpdateRequest request);

    void delete(Long id);
}
