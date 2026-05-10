package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpVehicleModelCreateRequest;
import com.example.wms.dto.erp.ErpVehicleModelUpdateRequest;
import com.example.wms.entity.erp.ErpVehicleModel;
import com.example.wms.service.erp.ErpVehicleModelService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP车型管理接口
@RestController
@RequestMapping("/api/erp/vehicle-models")
public class ErpVehicleModelController {
    private final ErpVehicleModelService erpVehicleModelService;

    public ErpVehicleModelController(ErpVehicleModelService erpVehicleModelService) {
        this.erpVehicleModelService = erpVehicleModelService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-model:view')")
    public ResponseEntity<ApiResponse<List<ErpVehicleModel>>> list(@RequestParam(required = false) String keyword,
                                                                   @RequestParam(required = false) Boolean enabled,
                                                                   @RequestParam(required = false) Long seriesId) {
        return ResponseEntity.ok(ApiResponse.ok(erpVehicleModelService.listAll(keyword, enabled, seriesId)));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-model:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpVehicleModel>>> page(@RequestParam(defaultValue = "1") long page,
                                                                           @RequestParam(defaultValue = "20") long size,
                                                                           @RequestParam(required = false) String keyword,
                                                                           @RequestParam(required = false) Boolean enabled,
                                                                           @RequestParam(required = false) Long seriesId) {
        return ResponseEntity.ok(ApiResponse.ok(erpVehicleModelService.page(page, size, keyword, enabled, seriesId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-model:view')")
    public ResponseEntity<ApiResponse<ErpVehicleModel>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpVehicleModelService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-model:add')")
    public ResponseEntity<ApiResponse<ErpVehicleModel>> create(@Valid @RequestBody ErpVehicleModelCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpVehicleModelService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-model:edit')")
    public ResponseEntity<ApiResponse<ErpVehicleModel>> update(@PathVariable Long id,
                                                               @Valid @RequestBody ErpVehicleModelUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpVehicleModelService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-model:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpVehicleModelService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
