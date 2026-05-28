package com.example.wms.service.erp;

import com.example.wms.dto.erp.ErpAssemblyTemplateCreateRequest;
import com.example.wms.dto.erp.ErpAssemblyTemplateDetail;
import com.example.wms.dto.erp.ErpAssemblyTemplateUpdateRequest;
import com.example.wms.entity.erp.ErpAssemblyTemplate;

import java.util.List;

// Assembly template service
public interface ErpAssemblyTemplateService {
    List<ErpAssemblyTemplate> listAll(String orderType, String keyword);
    List<ErpAssemblyTemplate> listByFinishedProduct(String orderType, Long finishedProductId);
    ErpAssemblyTemplateDetail getDetail(Long id);
    ErpAssemblyTemplateDetail create(ErpAssemblyTemplateCreateRequest request);
    ErpAssemblyTemplateDetail update(Long id, ErpAssemblyTemplateUpdateRequest request);
    void delete(Long id);
}
