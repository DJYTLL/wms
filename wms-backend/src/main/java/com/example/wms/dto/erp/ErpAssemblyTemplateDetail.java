package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpAssemblyTemplate;
import com.example.wms.entity.erp.ErpAssemblyTemplateItem;

import java.util.List;

/**

 * ERP 组装模板用于返回详情数据。

 */
public class ErpAssemblyTemplateDetail {
    /**
     * 表示模板。
     */
    private final ErpAssemblyTemplate template;
    /**
     * 表示明细项列表。
     */
    private final List<ErpAssemblyTemplateItem> items;

    public ErpAssemblyTemplateDetail(ErpAssemblyTemplate template, List<ErpAssemblyTemplateItem> items) {
        this.template = template;
        this.items = items;
    }

    public ErpAssemblyTemplate getTemplate() { return template; }
    public List<ErpAssemblyTemplateItem> getItems() { return items; }
}
