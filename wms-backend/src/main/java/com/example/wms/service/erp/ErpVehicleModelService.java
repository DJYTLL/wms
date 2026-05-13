package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpVehicleModelCreateRequest;
import com.example.wms.dto.erp.ErpVehicleModelUpdateRequest;
import com.example.wms.entity.erp.ErpVehicleModel;

import java.util.List;

// 车型服务（ERP进销存）
public interface ErpVehicleModelService {
    List<ErpVehicleModel> listAll(String keyword, Boolean enabled, Long seriesId);

    PageResponse<ErpVehicleModel> page(long page, long size, String keyword, Boolean enabled, Long seriesId);

    ErpVehicleModel getById(Long id);

    String nextCode();

    ErpVehicleModel create(ErpVehicleModelCreateRequest request);

    ErpVehicleModel update(Long id, ErpVehicleModelUpdateRequest request);

    void delete(Long id);
}
