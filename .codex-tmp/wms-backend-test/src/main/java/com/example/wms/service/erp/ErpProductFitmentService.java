package com.example.wms.service.erp;

import com.example.wms.dto.erp.ErpProductFitmentCreateRequest;
import com.example.wms.dto.erp.ErpProductFitmentUpdateRequest;
import com.example.wms.entity.erp.ErpProductFitment;

import java.util.List;

// 商品适配车型关系服务（ERP进销存）
public interface ErpProductFitmentService {
    List<ErpProductFitment> listAll(Long productId, Long modelId);

    ErpProductFitment getById(Long id);

    ErpProductFitment create(ErpProductFitmentCreateRequest request);

    ErpProductFitment update(Long id, ErpProductFitmentUpdateRequest request);

    void delete(Long id);
}
