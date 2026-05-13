package com.example.wms.dto.erp;

import com.example.wms.entity.erp.ErpAssemblyTemplate;
import com.example.wms.entity.erp.ErpAssemblyTemplateItem;

import java.util.List;

// Assembly template detail response
public class ErpAssemblyTemplateDetail {
    private final ErpAssemblyTemplate template;
    private final List<ErpAssemblyTemplateItem> items;

    public ErpAssemblyTemplateDetail(ErpAssemblyTemplate template, List<ErpAssemblyTemplateItem> items) {
        this.template = template;
        this.items = items;
    }

    public ErpAssemblyTemplate getTemplate() { return template; }
    public List<ErpAssemblyTemplateItem> getItems() { return items; }
}
