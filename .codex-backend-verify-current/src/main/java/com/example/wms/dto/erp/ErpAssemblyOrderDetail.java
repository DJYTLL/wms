package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpAssemblyOrder;
import com.example.wms.entity.erp.ErpAssemblyOrderItem;

import java.util.List;

/**

 * ERP 组装单用于返回详情数据。

 */
public class ErpAssemblyOrderDetail {
    /**
     * 表示主单信息。
     */
    private final ErpAssemblyOrder order;
    /**
     * 表示明细项列表。
     */
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
