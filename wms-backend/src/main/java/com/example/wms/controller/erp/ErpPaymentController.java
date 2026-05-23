package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPaymentBootstrapData;
import com.example.wms.dto.erp.ErpPaymentCreateRequest;
import com.example.wms.dto.erp.ErpPaymentDetail;
import com.example.wms.dto.erp.ErpPaymentSourcePayableDetail;
import com.example.wms.dto.erp.ErpPaymentSourcePayableOption;
import com.example.wms.dto.erp.ErpPaymentView;
import com.example.wms.service.erp.ErpPaymentService;
import com.example.wms.service.erp.support.ErpFinanceDocumentBootstrapService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

// ERP?????
@RestController
@RequestMapping("/api/erp/payments")
public class ErpPaymentController {
    private final ErpPaymentService erpPaymentService;
    private final ErpFinanceDocumentBootstrapService bootstrapService;

    public ErpPaymentController(ErpPaymentService erpPaymentService,
                                ErpFinanceDocumentBootstrapService bootstrapService) {
        this.erpPaymentService = erpPaymentService;
        this.bootstrapService = bootstrapService;
    }

    @GetMapping("/bootstrap")
    @PreAuthorize("hasAuthority('PERM_erp-payment:add')")
    public ResponseEntity<ApiResponse<ErpPaymentBootstrapData>> bootstrap() {
        return ResponseEntity.ok(ApiResponse.ok(bootstrapService.getPaymentBootstrapData()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-payment:view')")
    public ResponseEntity<ApiResponse<List<ErpPaymentView>>> list(@RequestParam(required = false) String keyword,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(required = false) Long supplierId,
                                                                  @RequestParam(required = false) Long payableId,
                                                                  @RequestParam(required = false) Long startAt,
                                                                  @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(
            erpPaymentService.listAll(keyword, status, supplierId, payableId, start, end)
        ));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-payment:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpPaymentView>>> page(@RequestParam(defaultValue = "1") long page,
                                                                          @RequestParam(defaultValue = "20") long size,
                                                                          @RequestParam(required = false) String keyword,
                                                                          @RequestParam(required = false) String status,
                                                                          @RequestParam(required = false) Long supplierId,
                                                                          @RequestParam(required = false) Long payableId,
                                                                          @RequestParam(required = false) Long startAt,
                                                                          @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(
            erpPaymentService.page(page, size, keyword, status, supplierId, payableId, start, end)
        ));
    }

    @GetMapping("/source-payables/page")
    @PreAuthorize("hasAnyAuthority('PERM_erp-payment:source-view','PERM_erp-ap:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpPaymentSourcePayableOption>>> sourcePayablePage(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "20") long size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long supplierId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long startAt,
        @RequestParam(required = false) Long endAt
    ) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(
            erpPaymentService.sourcePayablePage(page, size, keyword, supplierId, status, start, end)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-payment:view')")
    public ResponseEntity<ApiResponse<ErpPaymentDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpPaymentService.getDetail(id)));
    }

    @GetMapping("/source-payables/{id}")
    @PreAuthorize("hasAnyAuthority('PERM_erp-payment:source-view','PERM_erp-ap:view')")
    public ResponseEntity<ApiResponse<ErpPaymentSourcePayableDetail>> getSourcePayable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpPaymentService.getSourcePayableDetail(id)));
    }

    @GetMapping("/next-payment-no")
    @PreAuthorize("hasAuthority('PERM_erp-payment:add')")
    public ResponseEntity<ApiResponse<String>> nextPaymentNo() {
        return ResponseEntity.ok(ApiResponse.ok(erpPaymentService.nextPaymentNo()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-payment:add')")
    public ResponseEntity<ApiResponse<ErpPaymentDetail>> create(@Valid @RequestBody ErpPaymentCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpPaymentService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-payment:add')")
    public ResponseEntity<ApiResponse<ErpPaymentDetail>> update(@PathVariable Long id,
                                                                @Valid @RequestBody ErpPaymentCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpPaymentService.update(id, request)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PERM_erp-payment:approve')")
    public ResponseEntity<ApiResponse<ErpPaymentDetail>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpPaymentService.approve(id)));
    }

    @PostMapping("/{id}/red-flush")
    @PreAuthorize("hasAuthority('PERM_erp-payment:red-flush')")
    public ResponseEntity<ApiResponse<ErpPaymentDetail>> redFlush(@PathVariable Long id,
                                                                  @RequestBody RedFlushRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpPaymentService.redFlush(id, request.reason())));
    }

    public record RedFlushRequest(String reason) {}
}
