package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSupplierTypeCreateRequest;
import com.example.wms.dto.erp.ErpSupplierTypeUpdateRequest;
import com.example.wms.entity.erp.ErpSupplierType;
import com.example.wms.service.erp.ErpSupplierTypeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// ERP供应商类型管理接口
@RestController
@RequestMapping("/api/erp/supplier-types")
public class ErpSupplierTypeController {
    private final ErpSupplierTypeService erpSupplierTypeService;

    public ErpSupplierTypeController(ErpSupplierTypeService erpSupplierTypeService) {
        this.erpSupplierTypeService = erpSupplierTypeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-supplier-type:view')")
    public ResponseEntity<ApiResponse<List<ErpSupplierType>>> list(@RequestParam(required = false) String keyword,
                                                                   @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierTypeService.listAll(keyword, enabled)));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-supplier-type:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpSupplierType>>> page(@RequestParam(defaultValue = "1") long page,
                                                                           @RequestParam(defaultValue = "20") long size,
                                                                           @RequestParam(required = false) String keyword,
                                                                           @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierTypeService.page(page, size, keyword, enabled)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-supplier-type:view')")
    public ResponseEntity<ApiResponse<ErpSupplierType>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierTypeService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-supplier-type:add')")
    public ResponseEntity<ApiResponse<ErpSupplierType>> create(@Valid @RequestBody ErpSupplierTypeCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierTypeService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-supplier-type:edit')")
    public ResponseEntity<ApiResponse<ErpSupplierType>> update(@PathVariable Long id,
                                                               @Valid @RequestBody ErpSupplierTypeUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierTypeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-supplier-type:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpSupplierTypeService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
