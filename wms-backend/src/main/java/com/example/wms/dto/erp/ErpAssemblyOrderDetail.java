package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpAssemblyOrder;
import com.example.wms.entity.erp.ErpAssemblyOrderItem;

import java.util.List;

// Assembly order detail response
public class ErpAssemblyOrderDetail {
    private final ErpAssemblyOrder order;
    private final List<ErpAssemblyOrderItem> items;

    public ErpAssemblyOrderDetail(ErpAssemblyOrder order, List<ErpAssemblyOrderItem> items) {
        this.order = order;
        this.items = items;
    }

    public ErpAssemblyOrder getOrder() {
        return order;
    }

    public List<ErpAssemblyOrderItem> getItems() {
        return items;
    }
}
