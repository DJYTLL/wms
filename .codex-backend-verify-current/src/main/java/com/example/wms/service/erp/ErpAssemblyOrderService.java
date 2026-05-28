package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpAssemblyOrderCreateRequest;
import com.example.wms.dto.erp.ErpAssemblyOrderDetail;
import com.example.wms.dto.erp.ErpAssemblyOrderUpdateRequest;
import com.example.wms.dto.erp.ErpAssemblySourceSaleOrderDetail;
import com.example.wms.dto.erp.ErpAssemblySourceSaleOrderOption;
import com.example.wms.entity.erp.ErpAssemblyOrder;

import java.time.Instant;
import java.util.List;

// Assembly order service
public interface ErpAssemblyOrderService {
    List<ErpAssemblyOrder> listAll(String keyword, String status, String orderType, Instant startAt, Instant endAt);

    PageResponse<ErpAssemblyOrder> page(long page, long size, String keyword, String status, String orderType, Instant startAt, Instant endAt);

    PageResponse<ErpAssemblySourceSaleOrderOption> sourceSaleOrderPage(long page, long size, String keyword, Long customerId);

    ErpAssemblySourceSaleOrderDetail getSourceSaleOrderDetail(Long saleOrderId);

    List<ErpAssemblyOrder> listBySaleOrderId(Long saleOrderId);

    ErpAssemblyOrderDetail getDetail(Long id);

    String nextOrderNo(String orderType);

    ErpAssemblyOrderDetail create(ErpAssemblyOrderCreateRequest request);

    ErpAssemblyOrderDetail update(Long id, ErpAssemblyOrderUpdateRequest request);

    void approve(Long id);

    void delete(Long id);
}
