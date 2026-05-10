package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCustomerCategoryCreateRequest;
import com.example.wms.dto.erp.ErpCustomerCategoryUpdateRequest;
import com.example.wms.entity.erp.ErpCustomerCategory;
import com.example.wms.service.erp.ErpCustomerCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP客户类别管理接口
@RestController
@RequestMapping("/api/erp/customer-categories")
public class ErpCustomerCategoryController {
    private final ErpCustomerCategoryService erpCustomerCategoryService;

    public ErpCustomerCategoryController(ErpCustomerCategoryService erpCustomerCategoryService) {
        this.erpCustomerCategoryService = erpCustomerCategoryService;
    }

    // 查询客户类别列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-customer-category:view')")
    public ResponseEntity<ApiResponse<List<ErpCustomerCategory>>> list(@RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpCustomerCategoryService.listAll(keyword, enabled)));
    }

    // 分页查询客户类别
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-customer-category:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpCustomerCategory>>> page(@RequestParam(defaultValue = "1") long page,
                                                                               @RequestParam(defaultValue = "20") long size,
                                                                               @RequestParam(required = false) String keyword,
                                                                               @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpCustomerCategoryService.page(page, size, keyword, enabled)));
    }

    // 查询客户类别详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-customer-category:view')")
    public ResponseEntity<ApiResponse<ErpCustomerCategory>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpCustomerCategoryService.getById(id)));
    }

    // 新增客户类别
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-customer-category:add')")
    public ResponseEntity<ApiResponse<ErpCustomerCategory>> create(@Valid @RequestBody ErpCustomerCategoryCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpCustomerCategoryService.create(request)));
    }

    // 更新客户类别
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-customer-category:edit')")
    public ResponseEntity<ApiResponse<ErpCustomerCategory>> update(@PathVariable Long id,
                                                                   @Valid @RequestBody ErpCustomerCategoryUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpCustomerCategoryService.update(id, request)));
    }

    // 删除客户类别
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-customer-category:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpCustomerCategoryService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
