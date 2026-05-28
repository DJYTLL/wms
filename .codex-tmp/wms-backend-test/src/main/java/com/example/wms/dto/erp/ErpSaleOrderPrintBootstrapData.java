package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpDeliveryMethod;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpSettlementMethod;
import com.example.wms.entity.erp.ErpWarehouse;

import java.util.List;

public record ErpSaleOrderPrintBootstrapData(
    ErpSaleOrderDetail detail,
    List<ErpCustomer> customers,
    List<ErpWarehouse> warehouses,
    List<ErpLocation> locations,
    List<ErpSettlementMethod> settlementMethods,
    List<ErpDeliveryMethod> deliveryMethods
) {
}
