package com.example.wms.service.erp.impl;

import com.example.wms.dto.erp.ErpVehicleFitmentBootstrapData;
import com.example.wms.service.erp.ErpProductFitmentService;
import com.example.wms.service.erp.ErpProductService;
import com.example.wms.service.erp.ErpVehicleBrandService;
import com.example.wms.service.erp.ErpVehicleFitmentBootstrapService;
import com.example.wms.service.erp.ErpVehicleModelService;
import com.example.wms.service.erp.ErpVehicleSeriesService;
import org.springframework.stereotype.Service;

@Service
public class ErpVehicleFitmentBootstrapServiceImpl implements ErpVehicleFitmentBootstrapService {
    private final ErpVehicleBrandService erpVehicleBrandService;
    private final ErpVehicleSeriesService erpVehicleSeriesService;
    private final ErpVehicleModelService erpVehicleModelService;
    private final ErpProductService erpProductService;
    private final ErpProductFitmentService erpProductFitmentService;

    public ErpVehicleFitmentBootstrapServiceImpl(ErpVehicleBrandService erpVehicleBrandService,
                                                 ErpVehicleSeriesService erpVehicleSeriesService,
                                                 ErpVehicleModelService erpVehicleModelService,
                                                 ErpProductService erpProductService,
                                                 ErpProductFitmentService erpProductFitmentService) {
        this.erpVehicleBrandService = erpVehicleBrandService;
        this.erpVehicleSeriesService = erpVehicleSeriesService;
        this.erpVehicleModelService = erpVehicleModelService;
        this.erpProductService = erpProductService;
        this.erpProductFitmentService = erpProductFitmentService;
    }

    @Override
    public ErpVehicleFitmentBootstrapData loadBootstrapData() {
        return new ErpVehicleFitmentBootstrapData(
            erpVehicleBrandService.listAll(null, null),
            erpVehicleSeriesService.listAll(null, null, null),
            erpVehicleModelService.listAll(null, null, null),
            erpProductService.listAll(null, null, null),
            erpProductFitmentService.listAll(null, null)
        );
    }
}
