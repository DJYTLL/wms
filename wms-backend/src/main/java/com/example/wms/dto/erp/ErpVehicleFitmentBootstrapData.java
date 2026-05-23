package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.entity.erp.ErpProductFitment;
import com.example.wms.entity.erp.ErpVehicleBrand;
import com.example.wms.entity.erp.ErpVehicleModel;
import com.example.wms.entity.erp.ErpVehicleSeries;

import java.util.List;

public record ErpVehicleFitmentBootstrapData(
    List<ErpVehicleBrand> brands,
    List<ErpVehicleSeries> series,
    List<ErpVehicleModel> models,
    List<ErpProduct> products,
    List<ErpProductFitment> fitments
) {
}
