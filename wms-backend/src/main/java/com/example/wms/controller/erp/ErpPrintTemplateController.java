package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPrintTemplateCreateRequest;
import com.example.wms.dto.erp.ErpPrintTemplateUpdateRequest;
import com.example.wms.entity.erp.ErpPrintTemplate;
import com.example.wms.service.erp.ErpPrintTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 打印模板接口（ERP进销存）
@RestController
@RequestMapping("/api/erp/print-templates")
public class ErpPrintTemplateController {
    private final ErpPrintTemplateService erpPrintTemplateService;

    public ErpPrintTemplateController(ErpPrintTemplateService erpPrintTemplateService) {
        this.erpPrintTemplateService = erpPrintTemplateService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-print-template:view')")
    public ResponseEntity<ApiResponse<List<ErpPrintTemplate>>> list(@RequestParam(required = false) String keyword,
                                                                    @RequestParam(required = false) String docType,
                                                                    @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpPrintTemplateService.listAll(keyword, docType, enabled)));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-print-template:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpPrintTemplate>>> page(@RequestParam(defaultValue = "1") long page,
                                                                            @RequestParam(defaultValue = "20") long size,
                                                                            @RequestParam(required = false) String keyword,
                                                                            @RequestParam(required = false) String docType,
                                                                            @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok(erpPrintTemplateService.page(page, size, keyword, docType, enabled)));
    }

    @GetMapping("/default")
    @PreAuthorize("hasAuthority('PERM_erp-print-template:view')")
    public ResponseEntity<ApiResponse<ErpPrintTemplate>> defaultTemplate(@RequestParam String docType) {
        return ResponseEntity.ok(ApiResponse.ok(erpPrintTemplateService.getDefaultByDocType(docType)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-print-template:view')")
    public ResponseEntity<ApiResponse<ErpPrintTemplate>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpPrintTemplateService.getById(id)));
    }

    @GetMapping("/next-code")
    @PreAuthorize("hasAuthority('PERM_erp-print-template:add')")
    public ResponseEntity<ApiResponse<String>> nextCode() {
        return ResponseEntity.ok(ApiResponse.ok(erpPrintTemplateService.nextCode()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-print-template:add')")
    public ResponseEntity<ApiResponse<ErpPrintTemplate>> create(@Valid @RequestBody ErpPrintTemplateCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpPrintTemplateService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-print-template:edit')")
    public ResponseEntity<ApiResponse<ErpPrintTemplate>> update(@PathVariable Long id,
                                                                @Valid @RequestBody ErpPrintTemplateUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpPrintTemplateService.update(id, request)));
    }

    @PostMapping("/{id}/default")
    @PreAuthorize("hasAuthority('PERM_erp-print-template:edit')")
    public ResponseEntity<ApiResponse<ErpPrintTemplate>> setDefault(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpPrintTemplateService.setDefault(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-print-template:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpPrintTemplateService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
