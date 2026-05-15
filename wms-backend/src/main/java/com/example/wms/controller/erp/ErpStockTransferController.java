package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockTransferCreateRequest;
import com.example.wms.dto.erp.ErpStockTransferDetail;
import com.example.wms.entity.erp.ErpStockTransfer;
import com.example.wms.service.erp.ErpStockTransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/erp/stock-transfers")
public class ErpStockTransferController {
    private final ErpStockTransferService erpStockTransferService;

    public ErpStockTransferController(ErpStockTransferService erpStockTransferService) {
        this.erpStockTransferService = erpStockTransferService;
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('PERM_erp-stock-transfer:view','PERM_erp-stock-count:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpStockTransfer>>> page(@RequestParam(defaultValue = "1") long page,
                                                                            @RequestParam(defaultValue = "20") long size,
                                                                            @RequestParam(required = false) String keyword,
                                                                            @RequestParam(required = false) String startAt,
                                                                            @RequestParam(required = false) String endAt) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockTransferService.page(page, size, keyword, startAt, endAt)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PERM_erp-stock-transfer:view','PERM_erp-stock-count:view')")
    public ResponseEntity<ApiResponse<ErpStockTransferDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockTransferService.getDetail(id)));
    }

    @GetMapping("/next-transfer-no")
    @PreAuthorize("hasAnyAuthority('PERM_erp-stock-transfer:add','PERM_erp-stock-count:add')")
    public ResponseEntity<ApiResponse<String>> nextTransferNo() {
        return ResponseEntity.ok(ApiResponse.ok(erpStockTransferService.nextTransferNo()));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('PERM_erp-stock-transfer:add','PERM_erp-stock-count:add')")
    public ResponseEntity<ApiResponse<ErpStockTransferDetail>> create(@Valid @RequestBody ErpStockTransferCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockTransferService.create(request)));
    }
}
