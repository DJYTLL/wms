package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockCountCreateRequest;
import com.example.wms.dto.erp.ErpStockCountDetail;
import com.example.wms.dto.erp.ErpStockInitImportResult;
import com.example.wms.dto.erp.ErpStockCountUpdateRequest;
import com.example.wms.entity.erp.ErpStockCount;
import com.example.wms.service.erp.ErpStockCountService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// ERP初始库存接口
@RestController
@RequestMapping("/api/erp/stock-inits")
public class ErpStockInitController {
    private final ErpStockCountService erpStockCountService;

    public ErpStockInitController(ErpStockCountService erpStockCountService) {
        this.erpStockCountService = erpStockCountService;
    }

    // 查询初始库存列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-stock-init:view')")
    public ResponseEntity<ApiResponse<List<ErpStockCount>>> list(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockCountService.listAll(keyword, status, "INIT")));
    }

    // 分页查询初始库存
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-stock-init:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpStockCount>>> page(@RequestParam(defaultValue = "1") long page,
                                                                         @RequestParam(defaultValue = "20") long size,
                                                                         @RequestParam(required = false) String keyword,
                                                                         @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockCountService.page(page, size, keyword, status, "INIT")));
    }

    // 查询初始库存详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-stock-init:view')")
    public ResponseEntity<ApiResponse<ErpStockCountDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockCountService.getDetail(id, "INIT")));
    }

    // 预生成初始库存单号
    @GetMapping("/next-count-no")
    @PreAuthorize("hasAuthority('PERM_erp-stock-init:add')")
    public ResponseEntity<ApiResponse<String>> nextCountNo() {
        return ResponseEntity.ok(ApiResponse.ok(erpStockCountService.nextCountNo("INIT")));
    }

    // 新增初始库存
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-stock-init:add')")
    public ResponseEntity<ApiResponse<ErpStockCountDetail>> create(@Valid @RequestBody ErpStockCountCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockCountService.create(request, "INIT")));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_erp-stock-init:add')")
    public ResponseEntity<ApiResponse<ErpStockInitImportResult>> importInitStocks(@RequestParam("file") MultipartFile file,
                                                                                   @RequestParam(value = "sourceName", required = false) String sourceName) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockCountService.importInitStocks(file, sourceName)));
    }

    // 更新初始库存
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-stock-init:edit')")
    public ResponseEntity<ApiResponse<ErpStockCountDetail>> update(@PathVariable Long id,
                                                                   @Valid @RequestBody ErpStockCountUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockCountService.update(id, request, "INIT")));
    }

    // 审核初始库存
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PERM_erp-stock-init:approve')")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id) {
        erpStockCountService.approve(id, "INIT");
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 红冲初始库存
    @PostMapping("/{id}/red-flush")
    @PreAuthorize("hasAuthority('PERM_erp-stock-init:redflush')")
    public ResponseEntity<ApiResponse<Void>> redFlush(@PathVariable Long id,
                                                      @RequestBody(required = false) RedFlushRequest request) {
        String reason = request == null ? null : request.reason();
        erpStockCountService.redFlush(id, "INIT", reason);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 作废初始库存
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PERM_erp-stock-init:cancel')")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        erpStockCountService.cancel(id, "INIT");
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record RedFlushRequest(String reason) {}
}
