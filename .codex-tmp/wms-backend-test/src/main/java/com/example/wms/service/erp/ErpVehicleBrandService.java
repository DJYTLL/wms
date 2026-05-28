package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpVehicleBrandCreateRequest;
import com.example.wms.dto.erp.ErpVehicleBrandUpdateRequest;
import com.example.wms.entity.erp.ErpVehicleBrand;

import java.util.List;

// 车型品牌服务（ERP进销存）
public interface ErpVehicleBrandService {
    List<ErpVehicleBrand> listAll(String keyword, Boolean enabled);

    PageResponse<ErpVehicleBrand> page(long page, long size, String keyword, Boolean enabled);

    ErpVehicleBrand getById(Long id);

    String nextCode();

    ErpVehicleBrand create(ErpVehicleBrandCreateRequest request);

    ErpVehicleBrand update(Long id, ErpVehicleBrandUpdateRequest request);

    void delete(Long id);
}
