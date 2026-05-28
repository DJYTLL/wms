package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpUnitCreateRequest;
import com.example.wms.dto.erp.ErpUnitUpdateRequest;
import com.example.wms.entity.erp.ErpUnit;
import com.example.wms.service.erp.ErpUnitService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP单位管理接口
@RestController
@RequestMapping("/api/erp/units")
public class ErpUnitController {
    private final ErpUnitService erpUnitService;

    public ErpUnitController(ErpUnitService erpUnitService) {
        this.erpUnitService = erpUnitService;
    }

    // 查询单位列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-unit:view')")
    public ResponseEntity<ApiResponse<List<ErpUnit>>> list(@RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpUnitService.listAll(keyword, enabled)));
    }

    // 分页查询单位
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-unit:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpUnit>>> page(@RequestParam(defaultValue = "1") long page,
                                                                   @RequestParam(defaultValue = "20") long size,
                                                                   @RequestParam(required = false) String keyword,
                                                                   @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpUnitService.page(page, size, keyword, enabled)));
    }

    // 查询单位详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-unit:view')")
    public ResponseEntity<ApiResponse<ErpUnit>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpUnitService.getById(id)));
    }

    // 获取下一个单位编码
    @GetMapping("/next-code")
    @PreAuthorize("hasAuthority('PERM_erp-unit:add')")
    public ResponseEntity<ApiResponse<String>> nextCode() {
        return ResponseEntity.ok(ApiResponse.ok(erpUnitService.nextCode()));
    }

    // 新增单位
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-unit:add')")
    public ResponseEntity<ApiResponse<ErpUnit>> create(@Valid @RequestBody ErpUnitCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpUnitService.create(request)));
    }

    // 更新单位
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-unit:edit')")
    public ResponseEntity<ApiResponse<ErpUnit>> update(@PathVariable Long id,
                                                       @Valid @RequestBody ErpUnitUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpUnitService.update(id, request)));
    }

    // 删除单位
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-unit:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpUnitService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
