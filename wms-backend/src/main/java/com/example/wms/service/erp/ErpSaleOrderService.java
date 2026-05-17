package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSaleOrderCreateRequest;
import com.example.wms.dto.erp.ErpSaleOrderDetail;
import com.example.wms.dto.erp.ErpSaleOrderHistoryItem;
import com.example.wms.dto.erp.ErpSaleOrderRecentItem;
import com.example.wms.dto.erp.ErpSaleOrderSummary;
import com.example.wms.dto.erp.ErpSaleOrderUpdateRequest;
import com.example.wms.entity.erp.ErpSaleOrder;

import java.time.Instant;
import java.util.List;

// 销售单服务接口（ERP进销存）
public interface ErpSaleOrderService {
    // 查询销售单列表
    List<ErpSaleOrder> listAll(String keyword, String status, Long customerId, Instant startAt, Instant endAt);

    // 分页查询销售单
    PageResponse<ErpSaleOrder> page(long page, long size, String keyword, String status, Long customerId, Instant startAt, Instant endAt);

    // 分页查询销售单草稿
    PageResponse<ErpSaleOrder> draftPage(long page, long size, String keyword, Long customerId, Instant startAt, Instant endAt);

    // 分页查询销售单已审核工作区
    PageResponse<ErpSaleOrder> approvedPage(long page, long size, String keyword, String status, Long customerId, Instant startAt, Instant endAt);

    // 销售单汇总
    ErpSaleOrderSummary summary(String keyword, String status, Long customerId, Instant startAt, Instant endAt);

    // 销售单草稿汇总
    ErpSaleOrderSummary draftSummary(String keyword, Long customerId, Instant startAt, Instant endAt);

    // 销售单已审核工作区汇总
    ErpSaleOrderSummary approvedSummary(String keyword, String status, Long customerId, Instant startAt, Instant endAt);

    // 查询销售单详情
    ErpSaleOrderDetail getDetail(Long id);

    // 查询草稿详情
    ErpSaleOrderDetail getDraftDetail(Long id);

    // 查询已审核工作区详情
    ErpSaleOrderDetail getApprovedDetail(Long id);

    // 生成销售单号
    String nextOrderNo();

    // 新增销售单
    ErpSaleOrderDetail create(ErpSaleOrderCreateRequest request);

    // 更新销售单
    ErpSaleOrderDetail update(Long id, ErpSaleOrderUpdateRequest request);

    // 删除销售单
    void delete(Long id);

    // 审核销售单
    void approve(Long id);

    // 作废销售单
    void cancel(Long id);

    // 作废销售单（已审核作废要求原因）
    void cancel(Long id, String reason);

    // 红冲销售单
    void redFlush(Long id, String reason);

    // 复制已审核销售单为草稿
    ErpSaleOrderDetail copyApprovedToDraft(Long id);

    // 最近包含商品的销售单明细（退货参考）
    List<ErpSaleOrderRecentItem> recentItemsByProduct(Long customerId, Long productId, int limit);

    // 分页查询包含商品的销售单明细（商品退货选择来源单）
    PageResponse<ErpSaleOrderRecentItem> recentItemsByProduct(Long customerId, Long productId, long page, long size);

    PageResponse<ErpSaleOrderHistoryItem> productHistory(Long customerId,
                                                         Long productId,
                                                         String keyword,
                                                         Instant startAt,
                                                         Instant endAt,
                                                         long page,
                                                         long size);
}
