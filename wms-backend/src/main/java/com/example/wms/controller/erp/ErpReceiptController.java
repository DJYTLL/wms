package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpReceiptBootstrapData;
import com.example.wms.dto.erp.ErpReceiptCreateRequest;
import com.example.wms.dto.erp.ErpReceiptDetail;
import com.example.wms.dto.erp.ErpReceiptSourceReceivableDetail;
import com.example.wms.dto.erp.ErpReceiptSourceReceivableOption;
import com.example.wms.dto.erp.ErpReceiptView;
import com.example.wms.service.erp.ErpReceiptService;
import com.example.wms.service.erp.support.ErpFinanceDocumentBootstrapService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

// ERP收款单接口
@RestController
@RequestMapping("/api/erp/receipts")
public class ErpReceiptController {
    private final ErpReceiptService erpReceiptService;
    private final ErpFinanceDocumentBootstrapService bootstrapService;

    public ErpReceiptController(ErpReceiptService erpReceiptService,
                                ErpFinanceDocumentBootstrapService bootstrapService) {
        this.erpReceiptService = erpReceiptService;
        this.bootstrapService = bootstrapService;
    }

    @GetMapping("/bootstrap")
    @PreAuthorize("hasAuthority('PERM_erp-receipt:add')")
    public ResponseEntity<ApiResponse<ErpReceiptBootstrapData>> bootstrap() {
        return ResponseEntity.ok(ApiResponse.ok(bootstrapService.getReceiptBootstrapData()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-receipt:view')")
    public ResponseEntity<ApiResponse<List<ErpReceiptView>>> list(@RequestParam(required = false) String keyword,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(required = false) Long customerId,
                                                                  @RequestParam(required = false) Long receivableId,
                                                                  @RequestParam(required = false) Long startAt,
                                                                  @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(
            erpReceiptService.listAll(keyword, status, customerId, receivableId, start, end)
        ));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-receipt:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpReceiptView>>> page(@RequestParam(defaultValue = "1") long page,
                                                                          @RequestParam(defaultValue = "20") long size,
                                                                          @RequestParam(required = false) String keyword,
                                                                          @RequestParam(required = false) String status,
                                                                          @RequestParam(required = false) Long customerId,
                                                                          @RequestParam(required = false) Long receivableId,
                                                                          @RequestParam(required = false) Long startAt,
                                                                          @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(
            erpReceiptService.page(page, size, keyword, status, customerId, receivableId, start, end)
        ));
    }

    @GetMapping("/source-receivables/page")
    @PreAuthorize("hasAnyAuthority('PERM_erp-receipt:source-view','PERM_erp-ar:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpReceiptSourceReceivableOption>>> sourceReceivablePage(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "20") long size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long customerId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long startAt,
        @RequestParam(required = false) Long endAt
    ) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(
            erpReceiptService.sourceReceivablePage(page, size, keyword, customerId, status, start, end)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-receipt:view')")
    public ResponseEntity<ApiResponse<ErpReceiptDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpReceiptService.getDetail(id)));
    }

    @GetMapping("/source-receivables/{id}")
    @PreAuthorize("hasAnyAuthority('PERM_erp-receipt:source-view','PERM_erp-ar:view')")
    public ResponseEntity<ApiResponse<ErpReceiptSourceReceivableDetail>> getSourceReceivable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpReceiptService.getSourceReceivableDetail(id)));
    }

    @GetMapping("/next-receipt-no")
    @PreAuthorize("hasAuthority('PERM_erp-receipt:add')")
    public ResponseEntity<ApiResponse<String>> nextReceiptNo() {
        return ResponseEntity.ok(ApiResponse.ok(erpReceiptService.nextReceiptNo()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-receipt:add')")
    public ResponseEntity<ApiResponse<ErpReceiptDetail>> create(@Valid @RequestBody ErpReceiptCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpReceiptService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-receipt:add')")
    public ResponseEntity<ApiResponse<ErpReceiptDetail>> update(@PathVariable Long id,
                                                                @Valid @RequestBody ErpReceiptCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpReceiptService.update(id, request)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PERM_erp-receipt:approve')")
    public ResponseEntity<ApiResponse<ErpReceiptDetail>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpReceiptService.approve(id)));
    }

    @PostMapping("/{id}/red-flush")
    @PreAuthorize("hasAuthority('PERM_erp-receipt:red-flush')")
    public ResponseEntity<ApiResponse<ErpReceiptDetail>> redFlush(@PathVariable Long id,
                                                                  @RequestBody RedFlushRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpReceiptService.redFlush(id, request.reason())));
    }

    public record RedFlushRequest(String reason) {}
}
