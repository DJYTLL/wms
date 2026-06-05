package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpProductImportBatchSummary;
import com.example.wms.dto.erp.ErpProductCreateRequest;
import com.example.wms.dto.erp.ErpProductImportItemView;
import com.example.wms.dto.erp.ErpProductImportPreview;
import com.example.wms.dto.erp.ErpProductImportResult;
import com.example.wms.dto.erp.ErpProductUpdateRequest;
import com.example.wms.entity.erp.ErpProduct;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 商品服务（ERP进销存）
public interface ErpProductService {
    List<ErpProduct> listAll(String keyword, Boolean enabled, Long categoryId);

    PageResponse<ErpProduct> page(long page, long size, String keyword, Boolean enabled, Long categoryId);

    ErpProduct getById(Long id);

    String nextCode();

    ErpProduct create(ErpProductCreateRequest request);

    ErpProductImportPreview previewImport(MultipartFile file);

    ErpProductImportResult importProducts(MultipartFile file, String sourceName);

    ErpProductImportResult importProducts(MultipartFile file, String sourceName, String fieldMapping);

    List<ErpProductImportBatchSummary> listImportBatches();

    List<ErpProductImportItemView> listImportBatchItems(Long batchId);

    ErpProduct update(Long id, ErpProductUpdateRequest request);

    void delete(Long id);
}
