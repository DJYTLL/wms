package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.erp.ErpAssemblyTemplateCreateRequest;
import com.example.wms.dto.erp.ErpAssemblyTemplateDetail;
import com.example.wms.dto.erp.ErpAssemblyTemplateUpdateRequest;
import com.example.wms.entity.erp.ErpAssemblyTemplate;
import com.example.wms.service.erp.ErpAssemblyTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Assembly/disassembly template API
@RestController
@RequestMapping("/api/erp/assembly-templates")
public class ErpAssemblyTemplateController {
    private final ErpAssemblyTemplateService assemblyTemplateService;

    public ErpAssemblyTemplateController(ErpAssemblyTemplateService assemblyTemplateService) {
        this.assemblyTemplateService = assemblyTemplateService;
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-assembly:view','PERM_erp-assemble-order:view','PERM_erp-disassemble-order:view')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ErpAssemblyTemplate>>> list(@RequestParam String orderType,
                                                                       @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(assemblyTemplateService.listAll(orderType, keyword)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-assembly:view','PERM_erp-assemble-order:view','PERM_erp-disassemble-order:view')")
    @GetMapping("/by-finished-product")
    public ResponseEntity<ApiResponse<List<ErpAssemblyTemplate>>> listByFinishedProduct(@RequestParam String orderType,
                                                                                        @RequestParam Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(assemblyTemplateService.listByFinishedProduct(orderType, productId)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-assembly:view','PERM_erp-assemble-order:view','PERM_erp-disassemble-order:view')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ErpAssemblyTemplateDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(assemblyTemplateService.getDetail(id)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-assembly:add','PERM_erp-assemble-order:add','PERM_erp-disassemble-order:add')")
    @PostMapping
    public ResponseEntity<ApiResponse<ErpAssemblyTemplateDetail>> create(@Valid @RequestBody ErpAssemblyTemplateCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(assemblyTemplateService.create(request)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-assembly:edit','PERM_erp-assemble-order:edit','PERM_erp-disassemble-order:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ErpAssemblyTemplateDetail>> update(@PathVariable Long id,
                                                                         @Valid @RequestBody ErpAssemblyTemplateUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(assemblyTemplateService.update(id, request)));
    }

    @PreAuthorize("hasAnyAuthority('PERM_erp-assembly:delete','PERM_erp-assemble-order:delete','PERM_erp-disassemble-order:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            assemblyTemplateService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
