package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpProductCreateRequest;
import com.example.wms.dto.erp.ErpProductUpdateRequest;
import com.example.wms.entity.erp.ErpProduct;

import java.util.List;

// 商品服务（ERP进销存）
public interface ErpProductService {
    List<ErpProduct> listAll(String keyword, Boolean enabled, Long categoryId);

    PageResponse<ErpProduct> page(long page, long size, String keyword, Boolean enabled, Long categoryId);

    ErpProduct getById(Long id);

    String nextCode();

    ErpProduct create(ErpProductCreateRequest request);

    ErpProduct update(Long id, ErpProductUpdateRequest request);

    void delete(Long id);
}
