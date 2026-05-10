package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpVehicleSeriesCreateRequest;
import com.example.wms.dto.erp.ErpVehicleSeriesUpdateRequest;
import com.example.wms.entity.erp.ErpVehicleSeries;
import com.example.wms.service.erp.ErpVehicleSeriesService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP车型车系管理接口
@RestController
@RequestMapping("/api/erp/vehicle-series")
public class ErpVehicleSeriesController {
    private final ErpVehicleSeriesService erpVehicleSeriesService;

    public ErpVehicleSeriesController(ErpVehicleSeriesService erpVehicleSeriesService) {
        this.erpVehicleSeriesService = erpVehicleSeriesService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-series:view')")
    public ResponseEntity<ApiResponse<List<ErpVehicleSeries>>> list(@RequestParam(required = false) String keyword,
                                                                    @RequestParam(required = false) Boolean enabled,
                                                                    @RequestParam(required = false) Long brandId) {
        return ResponseEntity.ok(ApiResponse.ok(erpVehicleSeriesService.listAll(keyword, enabled, brandId)));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-series:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpVehicleSeries>>> page(@RequestParam(defaultValue = "1") long page,
                                                                            @RequestParam(defaultValue = "20") long size,
                                                                            @RequestParam(required = false) String keyword,
                                                                            @RequestParam(required = false) Boolean enabled,
                                                                            @RequestParam(required = false) Long brandId) {
        return ResponseEntity.ok(ApiResponse.ok(erpVehicleSeriesService.page(page, size, keyword, enabled, brandId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-series:view')")
    public ResponseEntity<ApiResponse<ErpVehicleSeries>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpVehicleSeriesService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-series:add')")
    public ResponseEntity<ApiResponse<ErpVehicleSeries>> create(@Valid @RequestBody ErpVehicleSeriesCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpVehicleSeriesService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-series:edit')")
    public ResponseEntity<ApiResponse<ErpVehicleSeries>> update(@PathVariable Long id,
                                                                @Valid @RequestBody ErpVehicleSeriesUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpVehicleSeriesService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-vehicle-series:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpVehicleSeriesService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
