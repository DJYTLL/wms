package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpAccountsReceivableDetail;
import com.example.wms.dto.erp.ErpAccountsReceivableView;
import com.example.wms.service.erp.ErpAccountsReceivableService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

// ERP应收管理接口
@RestController
@RequestMapping("/api/erp/ar")
public class ErpAccountsReceivableController {
    private final ErpAccountsReceivableService erpAccountsReceivableService;

    public ErpAccountsReceivableController(ErpAccountsReceivableService erpAccountsReceivableService) {
        this.erpAccountsReceivableService = erpAccountsReceivableService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-ar:view')")
    public ResponseEntity<ApiResponse<List<ErpAccountsReceivableView>>> list(@RequestParam(required = false) String keyword,
                                                                             @RequestParam(required = false) String status,
                                                                             @RequestParam(required = false) Long customerId,
                                                                             @RequestParam(required = false) Long startAt,
                                                                             @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(
            erpAccountsReceivableService.listAll(keyword, status, customerId, start, end)
        ));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-ar:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpAccountsReceivableView>>> page(@RequestParam(defaultValue = "1") long page,
                                                                                     @RequestParam(defaultValue = "20") long size,
                                                                                     @RequestParam(required = false) String keyword,
                                                                                     @RequestParam(required = false) String status,
                                                                                     @RequestParam(required = false) Long customerId,
                                                                                     @RequestParam(required = false) Long startAt,
                                                                                     @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(
            erpAccountsReceivableService.page(page, size, keyword, status, customerId, start, end)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-ar:view')")
    public ResponseEntity<ApiResponse<ErpAccountsReceivableDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpAccountsReceivableService.getDetail(id)));
    }
}
