package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCategoryCreateRequest;
import com.example.wms.dto.erp.ErpCategoryUpdateRequest;
import com.example.wms.entity.erp.ErpCategory;
import com.example.wms.service.erp.ErpCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP分类管理接口
@RestController
@RequestMapping("/api/erp/categories")
public class ErpCategoryController {
    private final ErpCategoryService erpCategoryService;

    public ErpCategoryController(ErpCategoryService erpCategoryService) {
        this.erpCategoryService = erpCategoryService;
    }

    // 查询分类列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-category:view')")
    public ResponseEntity<ApiResponse<List<ErpCategory>>> list(@RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpCategoryService.listAll(keyword, enabled)));
    }

    // 分页查询分类
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-category:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpCategory>>> page(@RequestParam(defaultValue = "1") long page,
                                                                       @RequestParam(defaultValue = "20") long size,
                                                                       @RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpCategoryService.page(page, size, keyword, enabled)));
    }

    // 查询分类详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-category:view')")
    public ResponseEntity<ApiResponse<ErpCategory>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpCategoryService.getById(id)));
    }

    // 获取下一个分类编码
    @GetMapping("/next-code")
    @PreAuthorize("hasAuthority('PERM_erp-category:add')")
    public ResponseEntity<ApiResponse<String>> nextCode() {
        return ResponseEntity.ok(ApiResponse.ok(erpCategoryService.nextCode()));
    }

    // 新增分类
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-category:add')")
    public ResponseEntity<ApiResponse<ErpCategory>> create(@Valid @RequestBody ErpCategoryCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpCategoryService.create(request)));
    }

    // 更新分类
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-category:edit')")
    public ResponseEntity<ApiResponse<ErpCategory>> update(@PathVariable Long id,
                                                           @Valid @RequestBody ErpCategoryUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpCategoryService.update(id, request)));
    }

    // 删除分类
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-category:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpCategoryService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
