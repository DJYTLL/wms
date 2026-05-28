package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPaymentMethodCreateRequest;
import com.example.wms.dto.erp.ErpPaymentMethodUpdateRequest;
import com.example.wms.entity.erp.ErpReceiptMethod;
import com.example.wms.service.erp.ErpReceiptMethodService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP收款方式接口
@RestController
@RequestMapping("/api/erp/receipt-methods")
public class ErpReceiptMethodController {
    private final ErpReceiptMethodService erpReceiptMethodService;

    public ErpReceiptMethodController(ErpReceiptMethodService erpReceiptMethodService) {
        this.erpReceiptMethodService = erpReceiptMethodService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-receipt-method:view')")
    public ResponseEntity<ApiResponse<List<ErpReceiptMethod>>> list(@RequestParam(required = false) String keyword,
                                                                    @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpReceiptMethodService.listAll(keyword, enabled)));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-receipt-method:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpReceiptMethod>>> page(@RequestParam(defaultValue = "1") long page,
                                                                            @RequestParam(defaultValue = "20") long size,
                                                                            @RequestParam(required = false) String keyword,
                                                                            @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpReceiptMethodService.page(page, size, keyword, enabled)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-receipt-method:view')")
    public ResponseEntity<ApiResponse<ErpReceiptMethod>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpReceiptMethodService.getById(id)));
    }

    @GetMapping("/next-code")
    @PreAuthorize("hasAuthority('PERM_erp-receipt-method:add')")
    public ResponseEntity<ApiResponse<String>> nextCode() {
        return ResponseEntity.ok(ApiResponse.ok(erpReceiptMethodService.nextCode()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-receipt-method:add')")
    public ResponseEntity<ApiResponse<ErpReceiptMethod>> create(@Valid @RequestBody ErpPaymentMethodCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpReceiptMethodService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-receipt-method:edit')")
    public ResponseEntity<ApiResponse<ErpReceiptMethod>> update(@PathVariable Long id,
                                                                @Valid @RequestBody ErpPaymentMethodUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpReceiptMethodService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-receipt-method:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpReceiptMethodService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
