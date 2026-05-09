package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSaleReturnCreateRequest;
import com.example.wms.dto.erp.ErpSaleReturnDetail;
import com.example.wms.dto.erp.ErpSaleReturnUpdateRequest;
import com.example.wms.entity.erp.ErpSaleReturn;
import com.example.wms.service.erp.ErpSaleReturnService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

// 销售退货接口
@RestController
@RequestMapping("/api/erp/sale-returns")
public class ErpSaleReturnController {
    private final ErpSaleReturnService erpSaleReturnService;

    public ErpSaleReturnController(ErpSaleReturnService erpSaleReturnService) {
        this.erpSaleReturnService = erpSaleReturnService;
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return:view')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ErpSaleReturn>>> list(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) String status,
                                                                 @RequestParam(required = false) Long customerId,
                                                                 @RequestParam(required = false) Long startAt,
                                                                 @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.listAll(keyword, status, customerId, start, end)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return:view')")
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<ErpSaleReturn>>> page(@RequestParam(defaultValue = "1") long page,
                                                                         @RequestParam(defaultValue = "20") long size,
                                                                         @RequestParam(required = false) String keyword,
                                                                         @RequestParam(required = false) String status,
                                                                         @RequestParam(required = false) Long customerId,
                                                                         @RequestParam(required = false) Long startAt,
                                                                         @RequestParam(required = false) Long endAt) {
        Instant start = startAt == null ? null : Instant.ofEpochMilli(startAt);
        Instant end = endAt == null ? null : Instant.ofEpochMilli(endAt);
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.page(page, size, keyword, status, customerId, start, end)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return:view')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ErpSaleReturnDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.getDetail(id)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return:add')")
    @GetMapping("/next-no")
    public ResponseEntity<ApiResponse<String>> nextNo() {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.nextOrderNo()));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return:add')")
    @PostMapping
    public ResponseEntity<ApiResponse<ErpSaleReturnDetail>> create(@Valid @RequestBody ErpSaleReturnCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.create(request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ErpSaleReturnDetail>> update(@PathVariable Long id,
                                                                   @Valid @RequestBody ErpSaleReturnUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSaleReturnService.update(id, request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return:edit')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        erpSaleReturnService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return:approve')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id) {
        erpSaleReturnService.approve(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PreAuthorize("hasAuthority('PERM_erp-sale-return:redflush')")
    @PostMapping("/{id}/red-flush")
    public ResponseEntity<ApiResponse<Void>> redFlush(@PathVariable Long id, @RequestParam String reason) {
        erpSaleReturnService.redFlush(id, reason);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
