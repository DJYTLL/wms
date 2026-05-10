package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPurchaseOrderCreateRequest;
import com.example.wms.dto.erp.ErpPurchaseOrderDetail;
import com.example.wms.dto.erp.ErpPurchaseOrderHistoryItem;
import com.example.wms.dto.erp.ErpPurchaseOrderRecentItem;
import com.example.wms.dto.erp.ErpPurchaseOrderUpdateRequest;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.service.erp.ErpPurchaseOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

// ERP采购单接口
@RestController
@RequestMapping("/api/erp/purchase-orders")
public class ErpPurchaseOrderController {
    private final ErpPurchaseOrderService erpPurchaseOrderService;

    public ErpPurchaseOrderController(ErpPurchaseOrderService erpPurchaseOrderService) {
        this.erpPurchaseOrderService = erpPurchaseOrderService;
    }

    // 查询采购单列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-purchase:view')")
    public ResponseEntity<ApiResponse<List<ErpPurchaseOrder>>> list(@RequestParam(required = false) String keyword,
                                                                    @RequestParam(required = false) String status,
                                                                    @RequestParam(required = false) Long supplierId,
                                                                    @RequestParam(required = false) String startAt,
                                                                    @RequestParam(required = false) String endAt) {
        Instant startInstant = parseInstant(startAt);
        Instant endInstant = parseInstant(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseOrderService.listAll(keyword, status, supplierId, startInstant, endInstant)));
    }

    // 分页查询采购单
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-purchase:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpPurchaseOrder>>> page(@RequestParam(defaultValue = "1") long page,
                                                                            @RequestParam(defaultValue = "20") long size,
                                                                            @RequestParam(required = false) String keyword,
                                                                            @RequestParam(required = false) String status,
                                                                            @RequestParam(required = false) Long supplierId,
                                                                            @RequestParam(required = false) String startAt,
                                                                            @RequestParam(required = false) String endAt) {
        Instant startInstant = parseInstant(startAt);
        Instant endInstant = parseInstant(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseOrderService.page(page, size, keyword, status, supplierId, startInstant, endInstant)));
    }

    // 查询采购单详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-purchase:view')")
    public ResponseEntity<ApiResponse<ErpPurchaseOrderDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseOrderService.getDetail(id)));
    }

    // 最近包含指定商品的采购单明细（退货参考）
    @GetMapping("/recent-items")
    @PreAuthorize("hasAuthority('PERM_erp-purchase:view')")
    public ResponseEntity<ApiResponse<List<ErpPurchaseOrderRecentItem>>> recentItems(@RequestParam Long supplierId,
                                                                                     @RequestParam Long productId,
                                                                                     @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseOrderService.recentItemsByProduct(supplierId, productId, limit)));
    }

    // 商品采购历史（用于商品历史弹窗）
    @GetMapping("/product-history")
    @PreAuthorize("hasAuthority('PERM_erp-purchase:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpPurchaseOrderHistoryItem>>> productHistory(@RequestParam Long productId,
                                                                                                 @RequestParam(required = false) Long supplierId,
                                                                                                 @RequestParam(required = false) String keyword,
                                                                                                 @RequestParam(required = false) String startAt,
                                                                                                 @RequestParam(required = false) String endAt,
                                                                                                 @RequestParam(defaultValue = "1") long page,
                                                                                                 @RequestParam(defaultValue = "10") long size) {
        Instant startInstant = parseInstant(startAt);
        Instant endInstant = parseInstant(endAt);
        return ResponseEntity.ok(ApiResponse.ok(
            erpPurchaseOrderService.productHistory(supplierId, productId, keyword, startInstant, endInstant, page, size)
        ));
    }

    // 预生成采购单号
    @GetMapping("/next-order-no")
    @PreAuthorize("hasAuthority('PERM_erp-purchase:add')")
    public ResponseEntity<ApiResponse<String>> nextOrderNo() {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseOrderService.nextOrderNo()));
    }

    // 新增采购单
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-purchase:add')")
    public ResponseEntity<ApiResponse<ErpPurchaseOrderDetail>> create(@Valid @RequestBody ErpPurchaseOrderCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseOrderService.create(request)));
    }

    // 更新采购单
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-purchase:edit')")
    public ResponseEntity<ApiResponse<ErpPurchaseOrderDetail>> update(@PathVariable Long id,
                                                                      @Valid @RequestBody ErpPurchaseOrderUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseOrderService.update(id, request)));
    }

    // 删除采购单
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-purchase:edit')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpPurchaseOrderService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 审核采购单
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PERM_erp-purchase:approve')")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id) {
        erpPurchaseOrderService.approve(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 反审核采购单
    @PostMapping("/{id}/unapprove")
    @PreAuthorize("hasAuthority('PERM_erp-purchase:unapprove')")
    public ResponseEntity<ApiResponse<Void>> unapprove(@PathVariable Long id) {
        erpPurchaseOrderService.unapprove(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 作废采购单
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PERM_erp-purchase:cancel')")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id,
                                                    @RequestBody(required = false) RedFlushRequest request) {
        String reason = request == null ? null : request.reason();
        erpPurchaseOrderService.cancel(id, reason);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record RedFlushRequest(String reason) {}

    // 解析时间参数（支持 ISO-8601 或毫秒时间戳）
    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.matches("^\\d+$")) {
            return Instant.ofEpochMilli(Long.parseLong(trimmed));
        }
        return Instant.parse(trimmed);
    }
}
