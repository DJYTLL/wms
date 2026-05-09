package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPurchaseOrderCreateRequest;
import com.example.wms.dto.erp.ErpPurchaseOrderDetail;
import com.example.wms.dto.erp.ErpPurchaseOrderHistoryItem;
import com.example.wms.dto.erp.ErpPurchaseOrderRecentItem;
import com.example.wms.dto.erp.ErpPurchaseOrderUpdateRequest;
import com.example.wms.entity.erp.ErpPurchaseOrder;

import java.time.Instant;
import java.util.List;

// 采购单服务接口（ERP进销存）
public interface ErpPurchaseOrderService {
    // 查询采购单列表
    List<ErpPurchaseOrder> listAll(String keyword, String status, Long supplierId, Instant startAt, Instant endAt);

    // 分页查询采购单
    PageResponse<ErpPurchaseOrder> page(long page, long size, String keyword, String status, Long supplierId, Instant startAt, Instant endAt);

    // 查询采购单详情
    ErpPurchaseOrderDetail getDetail(Long id);

    // 预生成采购单号
    String nextOrderNo();

    // 新增采购单
    ErpPurchaseOrderDetail create(ErpPurchaseOrderCreateRequest request);

    // 更新采购单
    ErpPurchaseOrderDetail update(Long id, ErpPurchaseOrderUpdateRequest request);

    // 删除采购单
    void delete(Long id);

    // 审核采购单
    void approve(Long id);

    // 反审核采购单
    void unapprove(Long id);

    // 作废/红冲采购单
    void cancel(Long id, String reason);

    // 最近包含商品的采购单明细（退货参考）
    List<ErpPurchaseOrderRecentItem> recentItemsByProduct(Long supplierId, Long productId, int limit);

    PageResponse<ErpPurchaseOrderHistoryItem> productHistory(Long supplierId,
                                                             Long productId,
                                                             String keyword,
                                                             Instant startAt,
                                                             Instant endAt,
                                                             long page,
                                                             long size);
}
