package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpDeliveryMethodCreateRequest;
import com.example.wms.dto.erp.ErpDeliveryMethodUpdateRequest;
import com.example.wms.entity.erp.ErpDeliveryMethod;

import java.util.List;

// 送货方式服务接口（ERP进销存）
public interface ErpDeliveryMethodService {
    List<ErpDeliveryMethod> listAll(String keyword, Boolean enabled);

    PageResponse<ErpDeliveryMethod> page(long page, long size, String keyword, Boolean enabled);

    ErpDeliveryMethod getById(Long id);

    ErpDeliveryMethod create(ErpDeliveryMethodCreateRequest request);

    ErpDeliveryMethod update(Long id, ErpDeliveryMethodUpdateRequest request);

    void delete(Long id);
}
