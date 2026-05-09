package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpLocationCreateRequest;
import com.example.wms.dto.erp.ErpLocationUpdateRequest;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.service.erp.ErpLocationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP库位管理接口
@RestController
@RequestMapping("/api/erp/locations")
public class ErpLocationController {
    private final ErpLocationService erpLocationService;

    public ErpLocationController(ErpLocationService erpLocationService) {
        this.erpLocationService = erpLocationService;
    }

    // 查询库位列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-location:view')")
    public ResponseEntity<ApiResponse<List<ErpLocation>>> list(@RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) Boolean enabled,
                                                               @RequestParam(required = false) Long warehouseId) {
        return ResponseEntity.ok(ApiResponse.ok(erpLocationService.listAll(keyword, enabled, warehouseId)));
    }

    // 分页查询库位
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-location:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpLocation>>> page(@RequestParam(defaultValue = "1") long page,
                                                                       @RequestParam(defaultValue = "20") long size,
                                                                       @RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) Boolean enabled,
                                                                       @RequestParam(required = false) Long warehouseId) {
        return ResponseEntity.ok(ApiResponse.ok(erpLocationService.page(page, size, keyword, enabled, warehouseId)));
    }

    // 查询库位详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-location:view')")
    public ResponseEntity<ApiResponse<ErpLocation>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpLocationService.getById(id)));
    }

    // 新增库位
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-location:add')")
    public ResponseEntity<ApiResponse<ErpLocation>> create(@Valid @RequestBody ErpLocationCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpLocationService.create(request)));
    }

    // 更新库位
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-location:edit')")
    public ResponseEntity<ApiResponse<ErpLocation>> update(@PathVariable Long id,
                                                           @Valid @RequestBody ErpLocationUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpLocationService.update(id, request)));
    }

    // 删除库位
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-location:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        erpLocationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
