package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPurchaseOrderRecentItem;
import com.example.wms.dto.erp.ErpPurchaseReturnCreateRequest;
import com.example.wms.dto.erp.ErpPurchaseReturnDetail;
import com.example.wms.dto.erp.ErpPurchaseReturnPrintBootstrapData;
import com.example.wms.dto.erp.ErpPurchaseReturnRefundSummary;
import com.example.wms.dto.erp.ErpPurchaseReturnSourcePurchaseOrderDetail;
import com.example.wms.dto.erp.ErpPurchaseReturnSourcePurchaseOrderOption;
import com.example.wms.dto.erp.ErpPurchaseReturnUpdateRequest;
import com.example.wms.entity.erp.ErpPurchaseReturn;
import com.example.wms.service.erp.ErpPurchaseReturnService;
import com.example.wms.service.erp.support.ErpDocumentPrintBootstrapService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

// 采购退货接口
@RestController
@RequestMapping("/api/erp/purchase-returns")
public class ErpPurchaseReturnController {
    private final ErpPurchaseReturnService erpPurchaseReturnService;
    private final ErpDocumentPrintBootstrapService printBootstrapService;

    public ErpPurchaseReturnController(ErpPurchaseReturnService erpPurchaseReturnService,
                                       ErpDocumentPrintBootstrapService printBootstrapService) {
        this.erpPurchaseReturnService = erpPurchaseReturnService;
        this.printBootstrapService = printBootstrapService;
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:view')")
    @GetMapping("/draft/page")
    public ResponseEntity<ApiResponse<PageResponse<ErpPurchaseReturn>>> draftPage(@RequestParam(defaultValue = "1") long page,
                                                                                  @RequestParam(defaultValue = "20") long size,
                                                                                  @RequestParam(required = false) String keyword,
                                                                                  @RequestParam(required = false) Long supplierId,
                                                                                  @RequestParam(required = false) Long startAt,
                                                                                  @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.draftPage(page, size, keyword, supplierId, start, end)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:view') or hasAuthority('PERM_erp-purchase-return-approved:view')")
    @GetMapping("/draft/summary")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnRefundSummary>> draftRefundSummary(@RequestParam Long purchaseOrderId) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.getPurchaseOrderRefundSummary(purchaseOrderId)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:add')")
    @GetMapping("/draft/next-no")
    public ResponseEntity<ApiResponse<String>> draftNextNo() {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.nextOrderNo()));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:add')")
    @PostMapping("/draft")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnDetail>> draftCreate(@Valid @RequestBody ErpPurchaseReturnCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.create(request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:view')")
    @GetMapping("/draft/{id}")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnDetail>> draftGet(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.getDraftDetail(id)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:print')")
    @GetMapping("/draft/{id}/print")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnDetail>> draftPrint(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.getDraftDetail(id)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:print')")
    @GetMapping("/draft/{id}/print-bootstrap")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnPrintBootstrapData>> draftPrintBootstrap(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(printBootstrapService.getPurchaseReturnBootstrap(id, false)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:edit')")
    @PutMapping("/draft/{id}")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnDetail>> draftUpdate(@PathVariable Long id,
                                                                            @Valid @RequestBody ErpPurchaseReturnUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.update(id, request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:delete')")
    @DeleteMapping("/draft/{id}")
    public ResponseEntity<ApiResponse<Void>> draftDelete(@PathVariable Long id,
                                                         @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpPurchaseReturnService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:approve')")
    @PostMapping("/draft/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> draftApprove(@PathVariable Long id) {
        erpPurchaseReturnService.approve(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-approved:view')")
    @GetMapping("/approved/page")
    public ResponseEntity<ApiResponse<PageResponse<ErpPurchaseReturn>>> approvedPage(@RequestParam(defaultValue = "1") long page,
                                                                                     @RequestParam(defaultValue = "20") long size,
                                                                                     @RequestParam(required = false) String keyword,
                                                                                     @RequestParam(required = false) Long supplierId,
                                                                                     @RequestParam(required = false) Long startAt,
                                                                                     @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.approvedPage(page, size, keyword, supplierId, start, end)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-purchase-return-draft:source-view','PERM_erp-purchase-approved:view')")
    @GetMapping("/source-purchase-orders/page")
    public ResponseEntity<ApiResponse<PageResponse<ErpPurchaseReturnSourcePurchaseOrderOption>>> sourcePurchaseOrderPage(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "20") long size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long supplierId,
        @RequestParam(required = false) Long currentReturnId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.sourcePurchaseOrderPage(page, size, keyword, supplierId, currentReturnId)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-purchase-return-draft:source-view','PERM_erp-purchase-approved:view')")
    @GetMapping("/source-purchase-orders/recent-items/page")
    public ResponseEntity<ApiResponse<PageResponse<ErpPurchaseOrderRecentItem>>> sourceRecentPurchaseItems(
        @RequestParam Long supplierId,
        @RequestParam Long productId,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long currentReturnId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.sourceRecentPurchaseItems(page, size, supplierId, productId, currentReturnId)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-purchase-return-draft:source-view','PERM_erp-purchase-approved:view')")
    @GetMapping("/source-purchase-orders/{purchaseOrderId}")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnSourcePurchaseOrderDetail>> getSourcePurchaseOrderDetail(
        @PathVariable Long purchaseOrderId,
        @RequestParam(required = false) Long currentReturnId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.getSourcePurchaseOrderDetail(purchaseOrderId, currentReturnId)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:view') or hasAuthority('PERM_erp-purchase-return-approved:view')")
    @GetMapping("/approved/summary")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnRefundSummary>> approvedRefundSummary(@RequestParam Long purchaseOrderId) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.getPurchaseOrderRefundSummary(purchaseOrderId)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-approved:view')")
    @GetMapping("/approved/{id}")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnDetail>> approvedGet(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.getApprovedDetail(id)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-approved:print')")
    @GetMapping("/approved/{id}/print")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnDetail>> approvedPrint(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.getApprovedDetail(id)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-approved:print')")
    @GetMapping("/approved/{id}/print-bootstrap")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnPrintBootstrapData>> approvedPrintBootstrap(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(printBootstrapService.getPurchaseReturnBootstrap(id, true)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-approved:copy') and hasAuthority('PERM_erp-purchase-return-draft:add')")
    @PostMapping("/approved/{id}/copy")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnDetail>> approvedCopy(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.copyApproved(id)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-approved:cancel')")
    @PostMapping("/approved/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> approvedCancel(@PathVariable Long id,
                                                            @RequestBody(required = false) RedFlushRequest request) {
        String reason = request == null ? null : request.reason();
        erpPurchaseReturnService.cancel(id, reason);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-purchase-return-draft:view','PERM_erp-purchase-return-approved:view')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ErpPurchaseReturn>>> list(@RequestParam(required = false) String keyword,
                                                                     @RequestParam(required = false) String status,
                                                                     @RequestParam(required = false) Long supplierId,
                                                                     @RequestParam(required = false) Long startAt,
                                                                     @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.listAll(keyword, status, supplierId, start, end)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-purchase-return-draft:view','PERM_erp-purchase-return-approved:view')")
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<ErpPurchaseReturn>>> page(@RequestParam(defaultValue = "1") long page,
                                                                             @RequestParam(defaultValue = "20") long size,
                                                                             @RequestParam(required = false) String keyword,
                                                                             @RequestParam(required = false) String status,
                                                                             @RequestParam(required = false) Long supplierId,
                                                                             @RequestParam(required = false) Long startAt,
                                                                             @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.page(page, size, keyword, status, supplierId, start, end)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-purchase-return-draft:view','PERM_erp-purchase-return-approved:view')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.getDetail(id)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-purchase-return-draft:view','PERM_erp-purchase-return-approved:view')")
    @GetMapping("/purchase-order/{purchaseOrderId}/refund-summary")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnRefundSummary>> getPurchaseOrderRefundSummary(@PathVariable Long purchaseOrderId) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.getPurchaseOrderRefundSummary(purchaseOrderId)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:add')")
    @GetMapping("/next-no")
    public ResponseEntity<ApiResponse<String>> nextNo() {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.nextOrderNo()));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:add')")
    @PostMapping
    public ResponseEntity<ApiResponse<ErpPurchaseReturnDetail>> create(@Valid @RequestBody ErpPurchaseReturnCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.create(request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnDetail>> update(@PathVariable Long id,
                                                                       @Valid @RequestBody ErpPurchaseReturnUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.update(id, request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpPurchaseReturnService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-draft:approve')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id) {
        erpPurchaseReturnService.approve(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return-approved:cancel')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id,
                                                    @RequestBody(required = false) RedFlushRequest request) {
        String reason = request == null ? null : request.reason();
        erpPurchaseReturnService.cancel(id, reason);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record RedFlushRequest(String reason) {}
}
