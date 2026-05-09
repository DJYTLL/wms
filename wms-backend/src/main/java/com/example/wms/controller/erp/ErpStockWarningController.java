package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockWarningView;
import com.example.wms.service.erp.ErpStockWarningService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Stock warning API
@RestController
@RequestMapping("/api/erp/stock-warnings")
public class ErpStockWarningController {
    private final ErpStockWarningService erpStockWarningService;

    public ErpStockWarningController(ErpStockWarningService erpStockWarningService) {
        this.erpStockWarningService = erpStockWarningService;
    }

    @PreAuthorize("hasAuthority('PERM_erp-stock-warning:view')")
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<ErpStockWarningView>>> page(@RequestParam(defaultValue = "1") long page,
                                                                               @RequestParam(defaultValue = "20") long size,
                                                                               @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockWarningService.page(page, size, keyword)));
    }
}
