package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockCountCreateRequest;
import com.example.wms.dto.erp.ErpStockCountDetail;
import com.example.wms.dto.erp.ErpStockCountUpdateRequest;
import com.example.wms.entity.erp.ErpStockCount;
import com.example.wms.service.erp.ErpStockCountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP库存盘点接口
@RestController
@RequestMapping("/api/erp/stock-counts")
public class ErpStockCountController {
    private final ErpStockCountService erpStockCountService;

    public ErpStockCountController(ErpStockCountService erpStockCountService) {
        this.erpStockCountService = erpStockCountService;
    }

    // 查询盘点单列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-stock-count:view')")
    public ResponseEntity<ApiResponse<List<ErpStockCount>>> list(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockCountService.listAll(keyword, status, "COUNT")));
    }

    // 分页查询盘点单
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-stock-count:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpStockCount>>> page(@RequestParam(defaultValue = "1") long page,
                                                                         @RequestParam(defaultValue = "20") long size,
                                                                         @RequestParam(required = false) String keyword,
                                                                         @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockCountService.page(page, size, keyword, status, "COUNT")));
    }

    // 查询盘点单详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-stock-count:view')")
    public ResponseEntity<ApiResponse<ErpStockCountDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockCountService.getDetail(id, "COUNT")));
    }

    // 预生成盘点单号
    @GetMapping("/next-count-no")
    @PreAuthorize("hasAuthority('PERM_erp-stock-count:add')")
    public ResponseEntity<ApiResponse<String>> nextCountNo() {
        return ResponseEntity.ok(ApiResponse.ok(erpStockCountService.nextCountNo("COUNT")));
    }

    // 新增盘点单
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-stock-count:add')")
    public ResponseEntity<ApiResponse<ErpStockCountDetail>> create(@Valid @RequestBody ErpStockCountCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockCountService.create(request, "COUNT")));
    }

    // 更新盘点单
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-stock-count:edit')")
    public ResponseEntity<ApiResponse<ErpStockCountDetail>> update(@PathVariable Long id,
                                                                   @Valid @RequestBody ErpStockCountUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockCountService.update(id, request, "COUNT")));
    }

    // 审核盘点单
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PERM_erp-stock-count:approve')")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id) {
        erpStockCountService.approve(id, "COUNT");
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 作废盘点单
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PERM_erp-stock-count:cancel')")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        erpStockCountService.cancel(id, "COUNT");
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
