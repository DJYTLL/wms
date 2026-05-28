package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSaleOrderCreateRequest;
import com.example.wms.dto.erp.ErpSaleOrderDetail;
import com.example.wms.dto.erp.ErpSaleOrderHistoryItem;
import com.example.wms.dto.erp.ErpSaleOrderPrintBootstrapData;
import com.example.wms.dto.erp.ErpSaleOrderRecentItem;
import com.example.wms.dto.erp.ErpSaleOrderSummary;
import com.example.wms.dto.erp.ErpSaleOrderUpdateRequest;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.service.erp.ErpSaleOrderService;
import com.example.wms.service.erp.support.ErpDocumentPrintBootstrapService;
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
    private final ErpDocumentPrintBootstrapService printBootstrapService;

    public ErpSaleOrderController(ErpSaleOrderService erpSaleOrderService,
                                  ErpDocumentPrintBootstrapService printBootstrapService) {
        this.erpSaleOrderService = erpSaleOrderService;
        this.printBootstrapService = printBootstrapService;
    }

    // 查询销售单列表
    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERM_erp-sale:view','PERM_erp-sale-draft:view','PERM_erp-sale-approved:view')")
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
    @PreAuthorize("hasAnyAuthority('PERM_erp-sale:view','PERM_erp-sale-draft:view','PERM_erp-sale-approved:view')")
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

    @GetMapping("/draft/page")
    @PreAuthorize("hasAuthority('PERM_erp-sale-draft:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpSaleOrder>>> draftPage(@RequestParam(defaultValue = "1") long page,
                                                                             @RequestParam(defaultValue = "20") long size,
                                                                             @RequestParam(required = false) String keyword,
                                                                             @RequestParam(required = false) Long customerId,
                                                                             @RequestParam(required = false) String startAt,
                                                                             @RequestParam(required = false) String endAt) {
        Instant startInstant = parseInstant(startAt);
        Instant endInstant = parseInstant(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.draftPage(page, size, keyword, customerId, startInstant, endInstant)));
    }

    @GetMapping("/approved/page")
    @PreAuthorize("hasAuthority('PERM_erp-sale-approved:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpSaleOrder>>> approvedPage(@RequestParam(defaultValue = "1") long page,
                                                                                @RequestParam(defaultValue = "20") long size,
                                                                                @RequestParam(required = false) String keyword,
                                                                                @RequestParam(required = false) String status,
                                                                                @RequestParam(required = false) Long customerId,
                                                                                @RequestParam(required = false) String startAt,
                                                                                @RequestParam(required = false) String endAt) {
        Instant startInstant = parseInstant(startAt);
        Instant endInstant = parseInstant(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.approvedPage(page, size, keyword, status, customerId, startInstant, endInstant)));
    }

    // 销售单汇总
    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('PERM_erp-sale:view','PERM_erp-sale-draft:view','PERM_erp-sale-approved:view')")
    public ResponseEntity<ApiResponse<ErpSaleOrderSummary>> summary(@RequestParam(required = false) String keyword,
                                                                    @RequestParam(required = false) String status,
                                                                    @RequestParam(required = false) Long customerId,
                                                                    @RequestParam(required = false) String startAt,
                                                                    @RequestParam(required = false) String endAt) {
        Instant startInstant = parseInstant(startAt);
        Instant endInstant = parseInstant(endAt);
        return ResponseEntity.ok(ApiResponse.ok(
            erpSaleOrderService.summary(keyword, status, customerId, startInstant, endInstant)
        ));
    }

    @GetMapping("/draft/summary")
    @PreAuthorize("hasAuthority('PERM_erp-sale-draft:view')")
    public ResponseEntity<ApiResponse<ErpSaleOrderSummary>> draftSummary(@RequestParam(required = false) String keyword,
                                                                         @RequestParam(required = false) Long customerId,
                                                                         @RequestParam(required = false) String startAt,
                                                                         @RequestParam(required = false) String endAt) {
        Instant startInstant = parseInstant(startAt);
        Instant endInstant = parseInstant(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.draftSummary(keyword, customerId, startInstant, endInstant)));
    }

    @GetMapping("/approved/summary")
    @PreAuthorize("hasAuthority('PERM_erp-sale-approved:view')")
    public ResponseEntity<ApiResponse<ErpSaleOrderSummary>> approvedSummary(@RequestParam(required = false) String keyword,
                                                                            @RequestParam(required = false) String status,
                                                                            @RequestParam(required = false) Long customerId,
                                                                            @RequestParam(required = false) String startAt,
                                                                            @RequestParam(required = false) String endAt) {
        Instant startInstant = parseInstant(startAt);
        Instant endInstant = parseInstant(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.approvedSummary(keyword, status, customerId, startInstant, endInstant)));
    }

    // 查询销售单详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PERM_erp-sale:view','PERM_erp-sale-draft:view','PERM_erp-sale-approved:view')")
    public ResponseEntity<ApiResponse<ErpSaleOrderDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.getDetail(id)));
    }

    @GetMapping("/draft/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-sale-draft:view')")
    public ResponseEntity<ApiResponse<ErpSaleOrderDetail>> getDraft(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.getDraftDetail(id)));
    }

    @GetMapping("/draft/{id}/print")
    @PreAuthorize("hasAuthority('PERM_erp-sale-draft:print')")
    public ResponseEntity<ApiResponse<ErpSaleOrderDetail>> getDraftPrint(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.getDraftDetail(id)));
    }

    @GetMapping("/draft/{id}/print-bootstrap")
    @PreAuthorize("hasAuthority('PERM_erp-sale-draft:print')")
    public ResponseEntity<ApiResponse<ErpSaleOrderPrintBootstrapData>> getDraftPrintBootstrap(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(printBootstrapService.getSaleOrderBootstrap(id, false)));
    }

    @GetMapping("/approved/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-sale-approved:view')")
    public ResponseEntity<ApiResponse<ErpSaleOrderDetail>> getApproved(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.getApprovedDetail(id)));
    }

    @GetMapping("/approved/{id}/print")
    @PreAuthorize("hasAuthority('PERM_erp-sale-approved:print')")
    public ResponseEntity<ApiResponse<ErpSaleOrderDetail>> getApprovedPrint(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.getApprovedDetail(id)));
    }

    @GetMapping("/approved/{id}/print-bootstrap")
    @PreAuthorize("hasAuthority('PERM_erp-sale-approved:print')")
    public ResponseEntity<ApiResponse<ErpSaleOrderPrintBootstrapData>> getApprovedPrintBootstrap(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(printBootstrapService.getSaleOrderBootstrap(id, true)));
    }

    // 最近包含指定商品的销售单明细（退货参考）
    @GetMapping("/recent-items")
    @PreAuthorize("hasAuthority('PERM_erp-sale-approved:view')")
    public ResponseEntity<ApiResponse<List<ErpSaleOrderRecentItem>>> recentItems(@RequestParam Long customerId,
                                                                                 @RequestParam Long productId,
                                                                                 @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.recentItemsByProduct(customerId, productId, limit)));
    }

    // 分页查询包含指定商品的销售单明细（商品退货选择来源单）
    @GetMapping("/recent-items/page")
    @PreAuthorize("hasAuthority('PERM_erp-sale-approved:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpSaleOrderRecentItem>>> recentItemsPage(@RequestParam Long customerId,
                                                                                             @RequestParam Long productId,
                                                                                             @RequestParam(defaultValue = "1") long page,
                                                                                             @RequestParam(defaultValue = "10") long size) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.recentItemsByProduct(customerId, productId, page, size)));
    }

    // 商品销售历史（用于商品历史弹窗）
    @GetMapping("/product-history")
    @PreAuthorize("hasAnyAuthority('PERM_erp-sale:view','PERM_erp-sale-draft:view','PERM_erp-sale-approved:view')")
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
    @PreAuthorize("hasAnyAuthority('PERM_erp-sale:add','PERM_erp-sale-draft:add')")
    public ResponseEntity<ApiResponse<String>> nextOrderNo() {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.nextOrderNo()));
    }

    @GetMapping("/draft/next-order-no")
    @PreAuthorize("hasAuthority('PERM_erp-sale-draft:add')")
    public ResponseEntity<ApiResponse<String>> draftNextOrderNo() {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.nextOrderNo()));
    }

    // 新增销售单
    @PostMapping
    @PreAuthorize("hasAnyAuthority('PERM_erp-sale:add','PERM_erp-sale-draft:add')")
    public ResponseEntity<ApiResponse<ErpSaleOrderDetail>> create(@Valid @RequestBody ErpSaleOrderCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.create(request)));
    }

    @PostMapping("/draft")
    @PreAuthorize("hasAuthority('PERM_erp-sale-draft:add')")
    public ResponseEntity<ApiResponse<ErpSaleOrderDetail>> createDraft(@Valid @RequestBody ErpSaleOrderCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.create(request)));
    }

    // 更新销售单
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PERM_erp-sale:edit','PERM_erp-sale-draft:edit')")
    public ResponseEntity<ApiResponse<ErpSaleOrderDetail>> update(@PathVariable Long id,
                                                                  @Valid @RequestBody ErpSaleOrderUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.update(id, request)));
    }

    @PutMapping("/draft/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-sale-draft:edit')")
    public ResponseEntity<ApiResponse<ErpSaleOrderDetail>> updateDraft(@PathVariable Long id,
                                                                       @Valid @RequestBody ErpSaleOrderUpdateRequest request) {
        erpSaleOrderService.getDraftDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.update(id, request)));
    }

    // 删除销售单
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PERM_erp-sale:edit','PERM_erp-sale-draft:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpSaleOrderService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/draft/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-sale-draft:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteDraft(@PathVariable Long id,
                                                         @Valid @RequestBody DeleteRequest request) {
        erpSaleOrderService.getDraftDetail(id);
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpSaleOrderService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 审核销售单
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('PERM_erp-sale:approve','PERM_erp-sale-draft:approve')")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id) {
        erpSaleOrderService.approve(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/draft/{id}/approve")
    @PreAuthorize("hasAuthority('PERM_erp-sale-draft:approve')")
    public ResponseEntity<ApiResponse<Void>> approveDraft(@PathVariable Long id) {
        erpSaleOrderService.getDraftDetail(id);
        erpSaleOrderService.approve(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 作废销售单
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('PERM_erp-sale:cancel','PERM_erp-sale-approved:cancel')")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        erpSaleOrderService.cancel(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/approved/{id}/cancel")
    @PreAuthorize("hasAuthority('PERM_erp-sale-approved:cancel')")
    public ResponseEntity<ApiResponse<Void>> cancelApproved(@PathVariable Long id,
                                                           @RequestBody(required = false) RedFlushRequest request) {
        String reason = request == null ? null : request.reason();
        erpSaleOrderService.getApprovedDetail(id);
        erpSaleOrderService.cancel(id, reason);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 红冲销售单
    @PostMapping("/{id}/red-flush")
    @PreAuthorize("hasAnyAuthority('PERM_erp-sale:redflush','PERM_erp-sale-approved:redflush')")
    public ResponseEntity<ApiResponse<Void>> redFlush(@PathVariable Long id,
                                                      @RequestBody(required = false) RedFlushRequest request) {
        String reason = request == null ? null : request.reason();
        erpSaleOrderService.redFlush(id, reason);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/approved/{id}/red-flush")
    @PreAuthorize("hasAuthority('PERM_erp-sale-approved:redflush')")
    public ResponseEntity<ApiResponse<Void>> redFlushApproved(@PathVariable Long id,
                                                             @RequestBody(required = false) RedFlushRequest request) {
        String reason = request == null ? null : request.reason();
        erpSaleOrderService.getApprovedDetail(id);
        erpSaleOrderService.redFlush(id, reason);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/approved/{id}/copy")
    @PreAuthorize("hasAuthority('PERM_erp-sale-approved:copy') and hasAuthority('PERM_erp-sale-draft:add')")
    public ResponseEntity<ApiResponse<ErpSaleOrderDetail>> copyApproved(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleOrderService.copyApprovedToDraft(id)));
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
