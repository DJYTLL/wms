package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpDeliveryMethodCreateRequest;
import com.example.wms.dto.erp.ErpDeliveryMethodUpdateRequest;
import com.example.wms.entity.erp.ErpDeliveryMethod;
import com.example.wms.service.erp.ErpDeliveryMethodService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP送货方式接口
@RestController
@RequestMapping("/api/erp/delivery-methods")
public class ErpDeliveryMethodController {
    private final ErpDeliveryMethodService erpDeliveryMethodService;

    public ErpDeliveryMethodController(ErpDeliveryMethodService erpDeliveryMethodService) {
        this.erpDeliveryMethodService = erpDeliveryMethodService;
    }

    // 查询送货方式列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-delivery-method:view')")
    public ResponseEntity<ApiResponse<List<ErpDeliveryMethod>>> list(@RequestParam(required = false) String keyword,
                                                                     @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpDeliveryMethodService.listAll(keyword, enabled)));
    }

    // 分页查询送货方式
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-delivery-method:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpDeliveryMethod>>> page(@RequestParam(defaultValue = "1") long page,
                                                                             @RequestParam(defaultValue = "20") long size,
                                                                             @RequestParam(required = false) String keyword,
                                                                             @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpDeliveryMethodService.page(page, size, keyword, enabled)));
    }

    // 查询送货方式详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-delivery-method:view')")
    public ResponseEntity<ApiResponse<ErpDeliveryMethod>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpDeliveryMethodService.getById(id)));
    }

    // 获取下一个送货方式编码
    @GetMapping("/next-code")
    @PreAuthorize("hasAuthority('PERM_erp-delivery-method:add')")
    public ResponseEntity<ApiResponse<String>> nextCode() {
        return ResponseEntity.ok(ApiResponse.ok(erpDeliveryMethodService.nextCode()));
    }

    // 新增送货方式
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-delivery-method:add')")
    public ResponseEntity<ApiResponse<ErpDeliveryMethod>> create(@Valid @RequestBody ErpDeliveryMethodCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpDeliveryMethodService.create(request)));
    }

    // 更新送货方式
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-delivery-method:edit')")
    public ResponseEntity<ApiResponse<ErpDeliveryMethod>> update(@PathVariable Long id,
                                                                 @Valid @RequestBody ErpDeliveryMethodUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpDeliveryMethodService.update(id, request)));
    }

    // 删除送货方式
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-delivery-method:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpDeliveryMethodService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
