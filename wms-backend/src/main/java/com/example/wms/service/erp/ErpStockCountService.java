package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockCountCreateRequest;
import com.example.wms.dto.erp.ErpStockCountDetail;
import com.example.wms.dto.erp.ErpStockCountItemView;
import com.example.wms.dto.erp.ErpStockInitImportBatchSummary;
import com.example.wms.dto.erp.ErpStockInitImportItemView;
import com.example.wms.dto.erp.ErpStockInitImportResult;
import com.example.wms.dto.erp.ErpStockInitImportPreview;
import com.example.wms.dto.erp.ErpStockCountUpdateRequest;
import com.example.wms.entity.erp.ErpStockCount;
import com.example.wms.entity.erp.ErpStockCountItem;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 库存盘点服务（ERP进销存）
public interface ErpStockCountService {
    List<ErpStockCount> listAll(String keyword, String status, String countType);

    PageResponse<ErpStockCount> page(long page, long size, String keyword, String status, String countType);

    ErpStockCountDetail getDetail(Long id, String countType);

    ErpStockCountDetail getDetail(Long id, String countType, boolean includeItems);

    PageResponse<ErpStockCountItemView> pageDetailItems(Long id, long page, long size, String countType);

    String nextCountNo(String countType);

    ErpStockCountDetail create(ErpStockCountCreateRequest request, String countType);

    ErpStockInitImportPreview previewInitStockImport(MultipartFile file);

    ErpStockInitImportResult importInitStocks(MultipartFile file, String sourceName);

    ErpStockInitImportResult importInitStocks(MultipartFile file, String sourceName, String fieldMapping);

    ErpStockInitImportResult importInitStocks(MultipartFile file, String sourceName, String fieldMapping, String strategyMode);

    List<ErpStockInitImportBatchSummary> listInitImportBatches();

    List<ErpStockInitImportItemView> listInitImportBatchItems(Long batchId);

    ErpStockCountDetail update(Long id, ErpStockCountUpdateRequest request, String countType);

    void approve(Long id, String countType);

    void redFlush(Long id, String countType, String reason);

    void cancel(Long id, String countType);
}
