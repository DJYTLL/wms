package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSaleOrderCreateRequest;
import com.example.wms.dto.erp.ErpSaleOrderDetail;
import com.example.wms.dto.erp.ErpSaleOrderHistoryItem;
import com.example.wms.dto.erp.ErpSaleOrderRecentItem;
import com.example.wms.dto.erp.ErpSaleOrderUpdateRequest;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.service.erp.ErpSaleOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

// ERP销售单接口
@RestController
@RequestMapping("/api/erp/sale-orders")
public class ErpSaleOrderController {
    private final ErpSaleOrderService erpSaleOrderService;

    public ErpSaleOrderController(ErpSaleOrderService erpSaleOrderService) {
        this.erpSaleOrderService = erpSaleOrderService;
    }

    // 查询销售单列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-sale:view')")
    public ResponseEntity<ApiResponse<List<ErpSaleOrder>>> list(@RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) String status,
                                                                @RequestParam(required = false) Long customerId,
                                                                @RequestParam(required = false) String startAt,
                                                                @RequestParam(required = false) String endAt) {
        Instant startInstant = parseInstant(startAt);
        Instant endInstant = parseInstant(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.listAll(keyword, status, customerId, startInstant, endInstant)));
    }

    // 分页查询销售单
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-sale:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpSaleOrder>>> page(@RequestParam(defaultValue = "1") long page,
                                                                        @RequestParam(defaultValue = "20") long size,
                                                                        @RequestParam(required = false) String keyword,
                                                                        @RequestParam(required = false) String status,
                                                                        @RequestParam(required = false) Long customerId,
                                                                        @RequestParam(required = false) String startAt,
                                                                        @RequestParam(required = false) String endAt) {
        Instant startInstant = parseInstant(startAt);
        Instant endInstant = parseInstant(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.page(page, size, keyword, status, customerId, startInstant, endInstant)));
    }

    // 查询销售单详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-sale:view')")
    public ResponseEntity<ApiResponse<ErpSaleOrderDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.getDetail(id)));
    }

    // 最近包含指定商品的销售单明细（退货参考）
    @GetMapping("/recent-items")
    @PreAuthorize("hasAuthority('PERM_erp-sale:view')")
    public ResponseEntity<ApiResponse<List<ErpSaleOrderRecentItem>>> recentItems(@RequestParam Long customerId,
                                                                                 @RequestParam Long productId,
                                                                                 @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.recentItemsByProduct(customerId, productId, limit)));
    }

    // 商品销售历史（用于商品历史弹窗）
    @GetMapping("/product-history")
    @PreAuthorize("hasAuthority('PERM_erp-sale:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpSaleOrderHistoryItem>>> productHistory(@RequestParam Long productId,
                                                                                             @RequestParam(required = false) Long customerId,
                                                                                             @RequestParam(required = false) String keyword,
                                                                                             @RequestParam(required = false) String startAt,
                                                                                             @RequestParam(required = false) String endAt,
                                                                                             @RequestParam(defaultValue = "1") long page,
                                                                                             @RequestParam(defaultValue = "10") long size) {
        Instant startInstant = parseInstant(startAt);
        Instant endInstant = parseInstant(endAt);
        return ResponseEntity.ok(ApiResponse.ok(
            erpSaleOrderService.productHistory(customerId, productId, keyword, startInstant, endInstant, page, size)
        ));
    }

    // 预生成销售单号
    @GetMapping("/next-order-no")
    @PreAuthorize("hasAuthority('PERM_erp-sale:add')")
    public ResponseEntity<ApiResponse<String>> nextOrderNo() {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.nextOrderNo()));
    }

    // 新增销售单
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-sale:add')")
    public ResponseEntity<ApiResponse<ErpSaleOrderDetail>> create(@Valid @RequestBody ErpSaleOrderCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.create(request)));
    }

    // 更新销售单
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-sale:edit')")
    public ResponseEntity<ApiResponse<ErpSaleOrderDetail>> update(@PathVariable Long id,
                                                                  @Valid @RequestBody ErpSaleOrderUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.update(id, request)));
    }

    // 删除销售单
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-sale:edit')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpSaleOrderService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 审核销售单
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PERM_erp-sale:approve')")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id) {
        erpSaleOrderService.approve(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 作废销售单
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PERM_erp-sale:cancel')")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        erpSaleOrderService.cancel(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 红冲销售单
    @PostMapping("/{id}/red-flush")
    @PreAuthorize("hasAuthority('PERM_erp-sale:redflush')")
    public ResponseEntity<ApiResponse<Void>> redFlush(@PathVariable Long id,
                                                      @RequestBody(required = false) RedFlushRequest request) {
        String reason = request == null ? null : request.reason();
        erpSaleOrderService.redFlush(id, reason);
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
