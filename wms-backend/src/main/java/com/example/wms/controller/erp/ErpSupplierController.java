package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSupplierCreateRequest;
import com.example.wms.dto.erp.ErpSupplierUpdateRequest;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.service.erp.ErpSupplierService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP供应商管理接口
@RestController
@RequestMapping("/api/erp/suppliers")
public class ErpSupplierController {
    private final ErpSupplierService erpSupplierService;

    public ErpSupplierController(ErpSupplierService erpSupplierService) {
        this.erpSupplierService = erpSupplierService;
    }

    // 查询供应商列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-supplier:view')")
    public ResponseEntity<ApiResponse<List<ErpSupplier>>> list(@RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierService.listAll(keyword, enabled)));
    }

    // 分页查询供应商
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-supplier:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpSupplier>>> page(@RequestParam(defaultValue = "1") long page,
                                                                       @RequestParam(defaultValue = "20") long size,
                                                                       @RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierService.page(page, size, keyword, enabled)));
    }

    // 查询供应商详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-supplier:view')")
    public ResponseEntity<ApiResponse<ErpSupplier>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierService.getById(id)));
    }

    // 新增供应商
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-supplier:add')")
    public ResponseEntity<ApiResponse<ErpSupplier>> create(@Valid @RequestBody ErpSupplierCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierService.create(request)));
    }

    // 更新供应商
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-supplier:edit')")
    public ResponseEntity<ApiResponse<ErpSupplier>> update(@PathVariable Long id,
                                                           @Valid @RequestBody ErpSupplierUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierService.update(id, request)));
    }

    // 删除供应商
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-supplier:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        erpSupplierService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
