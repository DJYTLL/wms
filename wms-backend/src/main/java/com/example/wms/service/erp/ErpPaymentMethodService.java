package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPaymentMethodCreateRequest;
import com.example.wms.dto.erp.ErpPaymentMethodUpdateRequest;
import com.example.wms.entity.erp.ErpPaymentMethod;

import java.util.List;

// 付款方式服务接口（ERP进销存）
public interface ErpPaymentMethodService {
    List<ErpPaymentMethod> listAll(String keyword, Boolean enabled);

    PageResponse<ErpPaymentMethod> page(long page, long size, String keyword, Boolean enabled);

    ErpPaymentMethod getById(Long id);

    String nextCode();

    ErpPaymentMethod create(ErpPaymentMethodCreateRequest request);

    ErpPaymentMethod update(Long id, ErpPaymentMethodUpdateRequest request);

    void delete(Long id);
}
