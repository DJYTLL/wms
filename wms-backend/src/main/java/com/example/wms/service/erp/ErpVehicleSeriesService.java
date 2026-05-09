package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpVehicleSeriesCreateRequest;
import com.example.wms.dto.erp.ErpVehicleSeriesUpdateRequest;
import com.example.wms.entity.erp.ErpVehicleSeries;

import java.util.List;

// 车型车系服务（ERP进销存）
public interface ErpVehicleSeriesService {
    List<ErpVehicleSeries> listAll(String keyword, Boolean enabled, Long brandId);

    PageResponse<ErpVehicleSeries> page(long page, long size, String keyword, Boolean enabled, Long brandId);

    ErpVehicleSeries getById(Long id);

    ErpVehicleSeries create(ErpVehicleSeriesCreateRequest request);

    ErpVehicleSeries update(Long id, ErpVehicleSeriesUpdateRequest request);

    void delete(Long id);
}
