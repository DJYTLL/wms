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

    @PreAuthorize("hasAuthority('PERM_erp-assembly:view')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ErpAssemblyTemplate>>> list(@RequestParam String orderType,
                                                                       @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(assemblyTemplateService.listAll(orderType, keyword)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-assembly:view')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ErpAssemblyTemplateDetail>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(assemblyTemplateService.getDetail(id)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-assembly:add')")
    @PostMapping
    public ResponseEntity<ApiResponse<ErpAssemblyTemplateDetail>> create(@Valid @RequestBody ErpAssemblyTemplateCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(assemblyTemplateService.create(request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-assembly:edit')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ErpAssemblyTemplateDetail>> update(@PathVariable Long id,
                                                                         @Valid @RequestBody ErpAssemblyTemplateUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(assemblyTemplateService.update(id, request)));
    }

    @PreAuthorize("hasAuthority('PERM_erp-assembly:delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            assemblyTemplateService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
