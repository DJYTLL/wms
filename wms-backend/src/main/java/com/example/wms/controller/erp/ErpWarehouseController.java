package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpWarehouseCreateRequest;
import com.example.wms.dto.erp.ErpWarehouseUpdateRequest;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.service.erp.ErpWarehouseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP仓库管理接口
@RestController
@RequestMapping("/api/erp/warehouses")
public class ErpWarehouseController {
    private final ErpWarehouseService erpWarehouseService;

    public ErpWarehouseController(ErpWarehouseService erpWarehouseService) {
        this.erpWarehouseService = erpWarehouseService;
    }

    // 查询仓库列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-warehouse:view')")
    public ResponseEntity<ApiResponse<List<ErpWarehouse>>> list(@RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpWarehouseService.listAll(keyword, enabled)));
    }

    // 分页查询仓库
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-warehouse:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpWarehouse>>> page(@RequestParam(defaultValue = "1") long page,
                                                                        @RequestParam(defaultValue = "20") long size,
                                                                        @RequestParam(required = false) String keyword,
                                                                        @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpWarehouseService.page(page, size, keyword, enabled)));
    }

    // 查询仓库详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-warehouse:view')")
    public ResponseEntity<ApiResponse<ErpWarehouse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpWarehouseService.getById(id)));
    }

    // 新增仓库
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-warehouse:add')")
    public ResponseEntity<ApiResponse<ErpWarehouse>> create(@Valid @RequestBody ErpWarehouseCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpWarehouseService.create(request)));
    }

    // 更新仓库
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-warehouse:edit')")
    public ResponseEntity<ApiResponse<ErpWarehouse>> update(@PathVariable Long id,
                                                            @Valid @RequestBody ErpWarehouseUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpWarehouseService.update(id, request)));
    }

    // 删除仓库
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-warehouse:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        erpWarehouseService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
