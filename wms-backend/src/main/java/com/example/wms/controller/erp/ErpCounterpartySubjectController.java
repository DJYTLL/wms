package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCounterpartySubjectDetail;
import com.example.wms.dto.erp.ErpCounterpartySubjectCreateRequest;
import com.example.wms.dto.erp.ErpCounterpartyUnbindCheck;
import com.example.wms.dto.erp.ErpCounterpartySubjectUpdateRequest;
import com.example.wms.entity.erp.ErpCounterpartySubject;
import com.example.wms.entity.erp.ErpCounterpartySubjectLink;
import com.example.wms.service.erp.ErpCounterpartySubjectService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// ERP 往来主体管理接口
@Validated
@RestController
@RequestMapping("/api/erp/counterparty-subjects")
public class ErpCounterpartySubjectController {
    private final ErpCounterpartySubjectService counterpartySubjectService;

    public ErpCounterpartySubjectController(ErpCounterpartySubjectService counterpartySubjectService) {
        this.counterpartySubjectService = counterpartySubjectService;
    }

    @GetMapping
    @PreAuthorize("hasRole('super_admin') or hasAuthority('PERM_erp-counterparty-subject:view')")
    public ResponseEntity<ApiResponse<List<ErpCounterpartySubject>>> list(@RequestParam(required = false) String keyword,
                                                                          @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(counterpartySubjectService.listAll(keyword, enabled)));
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('super_admin') or hasAuthority('PERM_erp-counterparty-subject:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpCounterpartySubject>>> page(@RequestParam(defaultValue = "1") long page,
                                                                                  @RequestParam(defaultValue = "20") long size,
                                                                                  @RequestParam(required = false) String keyword,
                                                                                  @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(counterpartySubjectService.page(page, size, keyword, enabled)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('super_admin') or hasAuthority('PERM_erp-counterparty-subject:view')")
    public ResponseEntity<ApiResponse<ErpCounterpartySubject>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(counterpartySubjectService.getById(id)));
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("hasRole('super_admin') or hasAuthority('PERM_erp-counterparty-subject:view')")
    public ResponseEntity<ApiResponse<ErpCounterpartySubjectDetail>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(counterpartySubjectService.getDetail(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('super_admin') or hasAuthority('PERM_erp-counterparty-subject:add')")
    public ResponseEntity<ApiResponse<ErpCounterpartySubject>> create(@Valid @RequestBody ErpCounterpartySubjectCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(counterpartySubjectService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('super_admin') or hasAuthority('PERM_erp-counterparty-subject:edit')")
    public ResponseEntity<ApiResponse<ErpCounterpartySubject>> update(@PathVariable Long id,
                                                                      @Valid @RequestBody ErpCounterpartySubjectUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(counterpartySubjectService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('super_admin') or hasAuthority('PERM_erp-counterparty-subject:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            counterpartySubjectService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{id}/bind-supplier")
    @PreAuthorize("hasRole('super_admin') or hasAuthority('PERM_erp-counterparty-subject:edit')")
    public ResponseEntity<ApiResponse<ErpCounterpartySubjectLink>> bindSupplier(@PathVariable Long id,
                                                                                @Valid @RequestBody BindRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
            counterpartySubjectService.bindSupplier(id, request.targetId(), request.primary(), request.remark())
        ));
    }

    @PostMapping("/{id}/bind-customer")
    @PreAuthorize("hasRole('super_admin') or hasAuthority('PERM_erp-counterparty-subject:edit')")
    public ResponseEntity<ApiResponse<ErpCounterpartySubjectLink>> bindCustomer(@PathVariable Long id,
                                                                                @Valid @RequestBody BindRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
            counterpartySubjectService.bindCustomer(id, request.targetId(), request.primary(), request.remark())
        ));
    }

    @DeleteMapping("/{id}/bind-supplier/{supplierId}")
    @PreAuthorize("hasRole('super_admin') or hasAuthority('PERM_erp-counterparty-subject:edit')")
    public ResponseEntity<ApiResponse<Void>> unbindSupplier(@PathVariable Long id,
                                                            @PathVariable Long supplierId) {
        counterpartySubjectService.unbindSupplier(id, supplierId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{id}/bind-supplier/{supplierId}/check")
    @PreAuthorize("hasRole('super_admin') or hasAuthority('PERM_erp-counterparty-subject:edit')")
    public ResponseEntity<ApiResponse<ErpCounterpartyUnbindCheck>> checkUnbindSupplier(@PathVariable Long id,
                                                                                        @PathVariable Long supplierId) {
        return ResponseEntity.ok(ApiResponse.ok(counterpartySubjectService.checkUnbindSupplier(id, supplierId)));
    }

    @DeleteMapping("/{id}/bind-customer/{customerId}")
    @PreAuthorize("hasRole('super_admin') or hasAuthority('PERM_erp-counterparty-subject:edit')")
    public ResponseEntity<ApiResponse<Void>> unbindCustomer(@PathVariable Long id,
                                                            @PathVariable Long customerId) {
        counterpartySubjectService.unbindCustomer(id, customerId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{id}/bind-customer/{customerId}/check")
    @PreAuthorize("hasRole('super_admin') or hasAuthority('PERM_erp-counterparty-subject:edit')")
    public ResponseEntity<ApiResponse<ErpCounterpartyUnbindCheck>> checkUnbindCustomer(@PathVariable Long id,
                                                                                        @PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(counterpartySubjectService.checkUnbindCustomer(id, customerId)));
    }

    // 往来主体绑定请求
    public record BindRequest(
        @NotNull Long targetId,
        Boolean primary,
        String remark
    ) {
    }
}
