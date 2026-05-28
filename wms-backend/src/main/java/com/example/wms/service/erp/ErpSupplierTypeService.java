package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSupplierTypeCreateRequest;
import com.example.wms.dto.erp.ErpSupplierTypeUpdateRequest;
import com.example.wms.entity.erp.ErpSupplierType;

import java.util.List;

// 供应商类型服务接口（ERP进销存）
public interface ErpSupplierTypeService {
    List<ErpSupplierType> listAll(String keyword, Boolean enabled);

    PageResponse<ErpSupplierType> page(long page, long size, String keyword, Boolean enabled);

    ErpSupplierType getById(Long id);

    ErpSupplierType create(ErpSupplierTypeCreateRequest request);

    ErpSupplierType update(Long id, ErpSupplierTypeUpdateRequest request);

    void delete(Long id);
}
