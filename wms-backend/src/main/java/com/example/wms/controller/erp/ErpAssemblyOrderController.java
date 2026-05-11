package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpAssemblyOrderCreateRequest;
import com.example.wms.dto.erp.ErpAssemblyOrderDetail;
import com.example.wms.dto.erp.ErpAssemblyOrderUpdateRequest;
import com.example.wms.entity.erp.ErpAssemblyOrder;
import com.example.wms.service.erp.ErpAssemblyOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

// Assembly order API
@RestController
@RequestMapping("/api/erp/assembly-orders")
public class ErpAssemblyOrderController {
    private final ErpAssemblyOrderService erpAssemblyOrderService;

    public ErpAssemblyOrderController(ErpAssemblyOrderService erpAssemblyOrderService) {
        this.erpAssemblyOrderService = erpAssemblyOrderService;
    }

    @PreAuthorize("hasAuthority('PERM_erp-assembly:view')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ErpAssemblyOrder>>> list(@RequestParam(required = false) String keyword,
                                                                    @RequestParam(required = false) String status,
                                                                    @RequestParam(required = false) String orderType,
                                                                    @RequestParam(required = false) Long startAt,
                                                                    @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(
            erpAssemblyOrderService.listAll(keyword, status, orderType, start, end)
        ));
    }

    @PreAuthorize("hasAuthority('PERM_erp-assembly:view')")
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<ErpAssemblyOrder>>> page(@RequestParam(defaultValue = "1") long page,
                                                                            @RequestParam(defaultValue = "20") long size,
                                                                            @RequestParam(required = false) String keyword,
                                                                            @RequestParam(required = false) String status,
                                                                            @RequestParam(required = false) String orderType,
                                                                            @RequestParam(required = false) Long startAt,
                                                                            @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(
            erpAssemblyOrderService.page(page, size, keyword, status, orderType, start, end)
        ));
    }

    @PreAuthorize("hasAuthority('PERM_erp-assembly:view')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ErpAssemblyOrderDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpAssemblyOrderService.getDetail(id)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-assembly:add')")
    @GetMapping("/next-order-no")
    public ResponseEntity<ApiResponse<String>> nextOrderNo(@RequestParam(required = false) String orderType) {
        return ResponseEntity.ok(ApiResponse.ok(erpAssemblyOrderService.nextOrderNo(orderType)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-assembly:add')")
    @PostMapping
    public ResponseEntity<ApiResponse<ErpAssemblyOrderDetail>> create(@Valid @RequestBody ErpAssemblyOrderCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpAssemblyOrderService.create(request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-assembly:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ErpAssemblyOrderDetail>> update(@PathVariable Long id,
                                                                      @Valid @RequestBody ErpAssemblyOrderUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpAssemblyOrderService.update(id, request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-assembly:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpAssemblyOrderService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PreAuthorize("hasAuthority('PERM_erp-assembly:approve')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id) {
        erpAssemblyOrderService.approve(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
