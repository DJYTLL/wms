package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSaleOrderRecentItem;
import com.example.wms.dto.erp.ErpSaleReturnCreateRequest;
import com.example.wms.dto.erp.ErpSaleReturnDetail;
import com.example.wms.dto.erp.ErpSaleReturnRefundSummary;
import com.example.wms.dto.erp.ErpSaleReturnSourceSaleOrderDetail;
import com.example.wms.dto.erp.ErpSaleReturnSourceSaleOrderOption;
import com.example.wms.dto.erp.ErpSaleReturnUpdateRequest;
import com.example.wms.entity.erp.ErpSaleReturn;
import com.example.wms.service.erp.ErpSaleReturnService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

// 销售退货接口
@RestController
@RequestMapping("/api/erp/sale-returns")
public class ErpSaleReturnController {
    private final ErpSaleReturnService erpSaleReturnService;

    public ErpSaleReturnController(ErpSaleReturnService erpSaleReturnService) {
        this.erpSaleReturnService = erpSaleReturnService;
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-sale-return-draft:view','PERM_erp-sale-return-approved:view')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ErpSaleReturn>>> list(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) String status,
                                                                 @RequestParam(required = false) Long customerId,
                                                                 @RequestParam(required = false) Long startAt,
                                                                 @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.listAll(keyword, status, customerId, start, end)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-sale-return-draft:view','PERM_erp-sale-return-approved:view')")
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<ErpSaleReturn>>> page(@RequestParam(defaultValue = "1") long page,
                                                                         @RequestParam(defaultValue = "20") long size,
                                                                         @RequestParam(required = false) String keyword,
                                                                         @RequestParam(required = false) String status,
                                                                         @RequestParam(required = false) Long customerId,
                                                                         @RequestParam(required = false) Long startAt,
                                                                         @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.page(page, size, keyword, status, customerId, start, end)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-draft:view')")
    @GetMapping("/draft/page")
    public ResponseEntity<ApiResponse<PageResponse<ErpSaleReturn>>> draftPage(@RequestParam(defaultValue = "1") long page,
                                                                              @RequestParam(defaultValue = "20") long size,
                                                                              @RequestParam(required = false) String keyword,
                                                                              @RequestParam(required = false) Long customerId,
                                                                              @RequestParam(required = false) Long startAt,
                                                                              @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.draftPage(page, size, keyword, customerId, start, end)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-approved:view')")
    @GetMapping("/approved/page")
    public ResponseEntity<ApiResponse<PageResponse<ErpSaleReturn>>> approvedPage(@RequestParam(defaultValue = "1") long page,
                                                                                 @RequestParam(defaultValue = "20") long size,
                                                                                 @RequestParam(required = false) String keyword,
                                                                                 @RequestParam(required = false) String status,
                                                                                 @RequestParam(required = false) Long customerId,
                                                                                 @RequestParam(required = false) Long startAt,
                                                                                 @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.approvedPage(page, size, keyword, status, customerId, start, end)));
    }

    @PreAuthorize("@erpSaleReturnPermissionService.canViewSourceSaleOrders()")
    @GetMapping("/source-sale-orders/page")
    public ResponseEntity<ApiResponse<PageResponse<ErpSaleReturnSourceSaleOrderOption>>> sourceSaleOrderPage(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "20") long size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long customerId,
        @RequestParam(required = false) Long currentReturnId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.sourceSaleOrderPage(page, size, keyword, customerId, currentReturnId)));
    }

    @PreAuthorize("@erpSaleReturnPermissionService.canViewSourceSaleOrders()")
    @GetMapping("/source-sale-orders/recent-items/page")
    public ResponseEntity<ApiResponse<PageResponse<ErpSaleOrderRecentItem>>> sourceRecentSaleItems(
        @RequestParam Long customerId,
        @RequestParam Long productId,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long currentReturnId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.sourceRecentSaleItems(page, size, customerId, productId, currentReturnId)));
    }

    @PreAuthorize("@erpSaleReturnPermissionService.canViewSourceSaleOrders()")
    @GetMapping("/source-sale-orders/{saleOrderId}")
    public ResponseEntity<ApiResponse<ErpSaleReturnSourceSaleOrderDetail>> getSourceSaleOrderDetail(
        @PathVariable Long saleOrderId,
        @RequestParam(required = false) Long currentReturnId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.getSourceSaleOrderDetail(saleOrderId, currentReturnId)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-sale-return-draft:view','PERM_erp-sale-return-approved:view')")
    @GetMapping("/sale-order/{saleOrderId}")
    public ResponseEntity<ApiResponse<List<ErpSaleReturn>>> listBySaleOrderId(@PathVariable Long saleOrderId,
                                                                               @RequestParam(defaultValue = "false") boolean includeDraft) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.listBySaleOrderId(saleOrderId, includeDraft)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-sale-return-draft:view','PERM_erp-sale-return-approved:view')")
    @GetMapping("/sale-order/{saleOrderId}/refund-summary")
    public ResponseEntity<ApiResponse<ErpSaleReturnRefundSummary>> getSaleOrderRefundSummary(@PathVariable Long saleOrderId) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.getSaleOrderRefundSummary(saleOrderId)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-sale-return-draft:view','PERM_erp-sale-return-approved:view')")
    @GetMapping("/sale-order/{saleOrderId}/refund-summary/split")
    public ResponseEntity<ApiResponse<ErpSaleReturnRefundSummary>> getSaleOrderRefundSummarySplit(@PathVariable Long saleOrderId) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.getSaleOrderRefundSummary(saleOrderId)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-sale-return-draft:view','PERM_erp-sale-return-approved:view')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ErpSaleReturnDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.getDetail(id)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-draft:view')")
    @GetMapping("/draft/{id}")
    public ResponseEntity<ApiResponse<ErpSaleReturnDetail>> getDraft(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.getDraftDetail(id)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-draft:print')")
    @GetMapping("/draft/{id}/print")
    public ResponseEntity<ApiResponse<ErpSaleReturnDetail>> getDraftPrint(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.getDraftDetail(id)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-approved:view')")
    @GetMapping("/approved/{id}")
    public ResponseEntity<ApiResponse<ErpSaleReturnDetail>> getApproved(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.getApprovedDetail(id)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-approved:print')")
    @GetMapping("/approved/{id}/print")
    public ResponseEntity<ApiResponse<ErpSaleReturnDetail>> getApprovedPrint(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.getApprovedDetail(id)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-draft:add')")
    @GetMapping("/next-no")
    public ResponseEntity<ApiResponse<String>> nextNo() {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.nextOrderNo()));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-draft:add')")
    @GetMapping("/draft/next-no")
    public ResponseEntity<ApiResponse<String>> draftNextNo() {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.nextOrderNo()));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-draft:add')")
    @PostMapping
    public ResponseEntity<ApiResponse<ErpSaleReturnDetail>> create(@Valid @RequestBody ErpSaleReturnCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.create(request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-draft:add')")
    @PostMapping("/draft")
    public ResponseEntity<ApiResponse<ErpSaleReturnDetail>> createDraft(@Valid @RequestBody ErpSaleReturnCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.create(request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-draft:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ErpSaleReturnDetail>> update(@PathVariable Long id,
                                                                   @Valid @RequestBody ErpSaleReturnUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.update(id, request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-draft:edit')")
    @PutMapping("/draft/{id}")
    public ResponseEntity<ApiResponse<ErpSaleReturnDetail>> updateDraft(@PathVariable Long id,
                                                                        @Valid @RequestBody ErpSaleReturnUpdateRequest request) {
        erpSaleReturnService.getDraftDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.update(id, request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-draft:edit')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpSaleReturnService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-draft:delete')")
    @DeleteMapping("/draft/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDraft(@PathVariable Long id,
                                                         @Valid @RequestBody DeleteRequest request) {
        erpSaleReturnService.getDraftDetail(id);
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpSaleReturnService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-draft:approve')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id) {
        erpSaleReturnService.approve(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-draft:approve')")
    @PostMapping("/draft/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveDraft(@PathVariable Long id) {
        erpSaleReturnService.getDraftDetail(id);
        erpSaleReturnService.approve(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-approved:copy') and hasAuthority('PERM_erp-sale-return-draft:add')")
    @PostMapping("/approved/{id}/copy")
    public ResponseEntity<ApiResponse<ErpSaleReturnDetail>> copyApproved(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.copyToDraft(id)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-approved:redflush')")
    @PostMapping("/{id}/red-flush")
    public ResponseEntity<ApiResponse<Void>> redFlush(@PathVariable Long id, @RequestParam String reason) {
        erpSaleReturnService.redFlush(id, reason);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return-approved:redflush')")
    @PostMapping("/approved/{id}/red-flush")
    public ResponseEntity<ApiResponse<Void>> redFlushApproved(@PathVariable Long id, @RequestParam String reason) {
        erpSaleReturnService.getApprovedDetail(id);
        erpSaleReturnService.redFlush(id, reason);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
