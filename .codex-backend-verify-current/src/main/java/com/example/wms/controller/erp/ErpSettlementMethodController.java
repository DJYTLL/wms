package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSettlementMethodCreateRequest;
import com.example.wms.dto.erp.ErpSettlementMethodUpdateRequest;
import com.example.wms.entity.erp.ErpSettlementMethod;
import com.example.wms.service.erp.ErpSettlementMethodService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP结算方式接口
@RestController
@RequestMapping("/api/erp/settlement-methods")
public class ErpSettlementMethodController {
    private final ErpSettlementMethodService erpSettlementMethodService;

    public ErpSettlementMethodController(ErpSettlementMethodService erpSettlementMethodService) {
        this.erpSettlementMethodService = erpSettlementMethodService;
    }

    // 查询结算方式列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-settlement-method:view')")
    public ResponseEntity<ApiResponse<List<ErpSettlementMethod>>> list(@RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpSettlementMethodService.listAll(keyword, enabled)));
    }

    // 分页查询结算方式
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-settlement-method:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpSettlementMethod>>> page(@RequestParam(defaultValue = "1") long page,
                                                                               @RequestParam(defaultValue = "20") long size,
                                                                               @RequestParam(required = false) String keyword,
                                                                               @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpSettlementMethodService.page(page, size, keyword, enabled)));
    }

    // 查询结算方式详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-settlement-method:view')")
    public ResponseEntity<ApiResponse<ErpSettlementMethod>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSettlementMethodService.getById(id)));
    }

    // 获取下一个结算方式编码
    @GetMapping("/next-code")
    @PreAuthorize("hasAuthority('PERM_erp-settlement-method:add')")
    public ResponseEntity<ApiResponse<String>> nextCode() {
        return ResponseEntity.ok(ApiResponse.ok(erpSettlementMethodService.nextCode()));
    }

    // 新增结算方式
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-settlement-method:add')")
    public ResponseEntity<ApiResponse<ErpSettlementMethod>> create(@Valid @RequestBody ErpSettlementMethodCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSettlementMethodService.create(request)));
    }

    // 更新结算方式
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-settlement-method:edit')")
    public ResponseEntity<ApiResponse<ErpSettlementMethod>> update(@PathVariable Long id,
                                                                   @Valid @RequestBody ErpSettlementMethodUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSettlementMethodService.update(id, request)));
    }

    // 删除结算方式
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-settlement-method:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpSettlementMethodService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
