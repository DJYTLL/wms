package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCustomerCreateRequest;
import com.example.wms.dto.erp.ErpCustomerUpdateRequest;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.service.erp.ErpCustomerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP客户管理接口
@RestController
@RequestMapping("/api/erp/customers")
public class ErpCustomerController {
    private final ErpCustomerService erpCustomerService;

    public ErpCustomerController(ErpCustomerService erpCustomerService) {
        this.erpCustomerService = erpCustomerService;
    }

    // 查询客户列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-customer:view')")
    public ResponseEntity<ApiResponse<List<ErpCustomer>>> list(@RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) Boolean enabled,
                                                               @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(ApiResponse.ok(erpCustomerService.listAll(keyword, enabled, categoryId)));
    }

    // 分页查询客户
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-customer:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpCustomer>>> page(@RequestParam(defaultValue = "1") long page,
                                                                       @RequestParam(defaultValue = "20") long size,
                                                                       @RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) Boolean enabled,
                                                                       @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(ApiResponse.ok(erpCustomerService.page(page, size, keyword, enabled, categoryId)));
    }

    // 查询客户详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-customer:view')")
    public ResponseEntity<ApiResponse<ErpCustomer>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpCustomerService.getById(id)));
    }

    // 获取下一个客户编码
    @GetMapping("/next-code")
    @PreAuthorize("hasAuthority('PERM_erp-customer:add')")
    public ResponseEntity<ApiResponse<String>> nextCode() {
        return ResponseEntity.ok(ApiResponse.ok(erpCustomerService.nextCode()));
    }

    // 新增客户
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-customer:add')")
    public ResponseEntity<ApiResponse<ErpCustomer>> create(@Valid @RequestBody ErpCustomerCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpCustomerService.create(request)));
    }

    // 更新客户
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-customer:edit')")
    public ResponseEntity<ApiResponse<ErpCustomer>> update(@PathVariable Long id,
                                                           @Valid @RequestBody ErpCustomerUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpCustomerService.update(id, request)));
    }

    // 删除客户
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-customer:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        erpCustomerService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
