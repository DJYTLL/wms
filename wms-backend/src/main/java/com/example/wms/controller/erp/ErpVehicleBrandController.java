package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpVehicleBrandCreateRequest;
import com.example.wms.dto.erp.ErpVehicleBrandUpdateRequest;
import com.example.wms.entity.erp.ErpVehicleBrand;
import com.example.wms.service.erp.ErpVehicleBrandService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP车型品牌管理接口
@RestController
@RequestMapping("/api/erp/vehicle-brands")
public class ErpVehicleBrandController {
    private final ErpVehicleBrandService erpVehicleBrandService;

    public ErpVehicleBrandController(ErpVehicleBrandService erpVehicleBrandService) {
        this.erpVehicleBrandService = erpVehicleBrandService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-brand:view')")
    public ResponseEntity<ApiResponse<List<ErpVehicleBrand>>> list(@RequestParam(required = false) String keyword,
                                                                   @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpVehicleBrandService.listAll(keyword, enabled)));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-brand:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpVehicleBrand>>> page(@RequestParam(defaultValue = "1") long page,
                                                                           @RequestParam(defaultValue = "20") long size,
                                                                           @RequestParam(required = false) String keyword,
                                                                           @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpVehicleBrandService.page(page, size, keyword, enabled)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-brand:view')")
    public ResponseEntity<ApiResponse<ErpVehicleBrand>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpVehicleBrandService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-brand:add')")
    public ResponseEntity<ApiResponse<ErpVehicleBrand>> create(@Valid @RequestBody ErpVehicleBrandCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpVehicleBrandService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-brand:edit')")
    public ResponseEntity<ApiResponse<ErpVehicleBrand>> update(@PathVariable Long id,
                                                               @Valid @RequestBody ErpVehicleBrandUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpVehicleBrandService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-brand:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpVehicleBrandService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
