package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockCountCreateRequest;
import com.example.wms.dto.erp.ErpStockCountDetail;
import com.example.wms.dto.erp.ErpStockCountUpdateRequest;
import com.example.wms.entity.erp.ErpStockCount;

import java.util.List;

// 库存盘点服务（ERP进销存）
public interface ErpStockCountService {
    List<ErpStockCount> listAll(String keyword, String status, String countType);

    PageResponse<ErpStockCount> page(long page, long size, String keyword, String status, String countType);

    ErpStockCountDetail getDetail(Long id);

    String nextCountNo(String countType);

    ErpStockCountDetail create(ErpStockCountCreateRequest request, String countType);

    ErpStockCountDetail update(Long id, ErpStockCountUpdateRequest request);

    void approve(Long id);

    void redFlush(Long id);

    void cancel(Long id);
}
