package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.entity.erp.ErpWarehouse;

import java.util.List;

public record ErpPurchaseReturnPrintBootstrapData(
    ErpPurchaseReturnDetail detail,
    List<ErpSupplier> suppliers,
    List<ErpWarehouse> warehouses,
    List<ErpLocation> locations
) {
}
