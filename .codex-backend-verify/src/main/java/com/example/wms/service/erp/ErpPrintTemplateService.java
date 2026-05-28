package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPrintTemplateCreateRequest;
import com.example.wms.dto.erp.ErpPrintTemplateUpdateRequest;
import com.example.wms.entity.erp.ErpPrintTemplate;

import java.util.List;

// 打印模板服务（ERP进销存）
public interface ErpPrintTemplateService {
    List<ErpPrintTemplate> listAll(String keyword, String docType, Boolean enabled);

    PageResponse<ErpPrintTemplate> page(long page, long size, String keyword, String docType, Boolean enabled);

    ErpPrintTemplate getById(Long id);

    String nextCode();

    ErpPrintTemplate getDefaultByDocType(String docType);

    ErpPrintTemplate create(ErpPrintTemplateCreateRequest request);

    ErpPrintTemplate update(Long id, ErpPrintTemplateUpdateRequest request);

    void delete(Long id);

    ErpPrintTemplate setDefault(Long id);
}
