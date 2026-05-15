package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPurchaseReturnCreateRequest;
import com.example.wms.dto.erp.ErpPurchaseReturnDetail;
import com.example.wms.dto.erp.ErpPurchaseReturnRefundSummary;
import com.example.wms.dto.erp.ErpPurchaseReturnUpdateRequest;
import com.example.wms.entity.erp.ErpPurchaseReturn;
import com.example.wms.service.erp.ErpPurchaseReturnService;
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

    public ErpPurchaseReturnController(ErpPurchaseReturnService erpPurchaseReturnService) {
        this.erpPurchaseReturnService = erpPurchaseReturnService;
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return:view')")
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

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return:view')")
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

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return:view')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.getDetail(id)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return:view')")
    @GetMapping("/purchase-order/{purchaseOrderId}/refund-summary")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnRefundSummary>> getPurchaseOrderRefundSummary(@PathVariable Long purchaseOrderId) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.getPurchaseOrderRefundSummary(purchaseOrderId)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return:add')")
    @GetMapping("/next-no")
    public ResponseEntity<ApiResponse<String>> nextNo() {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.nextOrderNo()));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return:add')")
    @PostMapping
    public ResponseEntity<ApiResponse<ErpPurchaseReturnDetail>> create(@Valid @RequestBody ErpPurchaseReturnCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.create(request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ErpPurchaseReturnDetail>> update(@PathVariable Long id,
                                                                       @Valid @RequestBody ErpPurchaseReturnUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpPurchaseReturnService.update(id, request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return:edit')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpPurchaseReturnService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return:approve')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id) {
        erpPurchaseReturnService.approve(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PreAuthorize("hasAuthority('PERM_erp-purchase-return:cancel')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id,
                                                    @RequestBody(required = false) RedFlushRequest request) {
        String reason = request == null ? null : request.reason();
        erpPurchaseReturnService.cancel(id, reason);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record RedFlushRequest(String reason) {}
}
