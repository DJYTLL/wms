package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPaymentMethodCreateRequest;
import com.example.wms.dto.erp.ErpPaymentMethodUpdateRequest;
import com.example.wms.entity.erp.ErpPaymentMethod;
import com.example.wms.service.erp.ErpPaymentMethodService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP付款方式接口
@RestController
@RequestMapping("/api/erp/payment-methods")
public class ErpPaymentMethodController {
    private final ErpPaymentMethodService erpPaymentMethodService;

    public ErpPaymentMethodController(ErpPaymentMethodService erpPaymentMethodService) {
        this.erpPaymentMethodService = erpPaymentMethodService;
    }

    // 查询付款方式列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-payment-method:view')")
    public ResponseEntity<ApiResponse<List<ErpPaymentMethod>>> list(@RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpPaymentMethodService.listAll(keyword, enabled)));
    }

    // 分页查询付款方式
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-payment-method:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpPaymentMethod>>> page(@RequestParam(defaultValue = "1") long page,
                                                                               @RequestParam(defaultValue = "20") long size,
                                                                               @RequestParam(required = false) String keyword,
                                                                               @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpPaymentMethodService.page(page, size, keyword, enabled)));
    }

    // 查询付款方式详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-payment-method:view')")
    public ResponseEntity<ApiResponse<ErpPaymentMethod>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpPaymentMethodService.getById(id)));
    }

    // 新增付款方式
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-payment-method:add')")
    public ResponseEntity<ApiResponse<ErpPaymentMethod>> create(@Valid @RequestBody ErpPaymentMethodCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpPaymentMethodService.create(request)));
    }

    // 更新付款方式
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-payment-method:edit')")
    public ResponseEntity<ApiResponse<ErpPaymentMethod>> update(@PathVariable Long id,
                                                                   @Valid @RequestBody ErpPaymentMethodUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpPaymentMethodService.update(id, request)));
    }

    // 删除付款方式
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-payment-method:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpPaymentMethodService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
