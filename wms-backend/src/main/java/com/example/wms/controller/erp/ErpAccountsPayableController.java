package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpAccountsPayableDetail;
import com.example.wms.dto.erp.ErpAccountsPayableView;
import com.example.wms.service.erp.ErpAccountsPayableService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

// ERP??????
@RestController
@RequestMapping("/api/erp/ap")
public class ErpAccountsPayableController {
    private final ErpAccountsPayableService erpAccountsPayableService;

    public ErpAccountsPayableController(ErpAccountsPayableService erpAccountsPayableService) {
        this.erpAccountsPayableService = erpAccountsPayableService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-ap:view')")
    public ResponseEntity<ApiResponse<List<ErpAccountsPayableView>>> list(@RequestParam(required = false) String keyword,
                                                                          @RequestParam(required = false) String status,
                                                                          @RequestParam(required = false) Long supplierId,
                                                                          @RequestParam(required = false) Long startAt,
                                                                          @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(
            erpAccountsPayableService.listAll(keyword, status, supplierId, start, end)
        ));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-ap:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpAccountsPayableView>>> page(@RequestParam(defaultValue = "1") long page,
                                                                                  @RequestParam(defaultValue = "20") long size,
                                                                                  @RequestParam(required = false) String keyword,
                                                                                  @RequestParam(required = false) String status,
                                                                                  @RequestParam(required = false) Long supplierId,
                                                                                  @RequestParam(required = false) Long startAt,
                                                                                  @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(
            erpAccountsPayableService.page(page, size, keyword, status, supplierId, start, end)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-ap:view')")
    public ResponseEntity<ApiResponse<ErpAccountsPayableDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpAccountsPayableService.getDetail(id)));
    }
}
