package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpStockTransfer;
import com.example.wms.entity.erp.ErpStockTransferItem;

import java.util.List;

public record ErpStockTransferDetail(
    ErpStockTransfer transfer,
    List<ErpStockTransferItem> items
) {
}
