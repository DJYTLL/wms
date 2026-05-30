package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCounterpartyUnbindCheck;
import com.example.wms.dto.erp.ErpSupplierImportBatchSummary;
import com.example.wms.dto.erp.ErpSupplierImportItemView;
import com.example.wms.dto.erp.ErpSupplierCreateRequest;
import com.example.wms.dto.erp.ErpSupplierImportResult;
import com.example.wms.dto.erp.ErpSupplierUpdateRequest;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.service.erp.ErpSupplierService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// ERP供应商管理接口
@RestController
@RequestMapping("/api/erp/suppliers")
public class ErpSupplierController {
    private final ErpSupplierService erpSupplierService;

    public ErpSupplierController(ErpSupplierService erpSupplierService) {
        this.erpSupplierService = erpSupplierService;
    }

    // 查询供应商列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-supplier:view')")
    public ResponseEntity<ApiResponse<List<ErpSupplier>>> list(@RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) String contact,
                                                               @RequestParam(required = false) String phone,
                                                               @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierService.listAll(keyword, contact, phone, status)));
    }

    // 分页查询供应商
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-supplier:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpSupplier>>> page(@RequestParam(defaultValue = "1") long page,
                                                                       @RequestParam(defaultValue = "20") long size,
                                                                       @RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) String contact,
                                                                       @RequestParam(required = false) String phone,
                                                                       @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierService.page(page, size, keyword, contact, phone, status)));
    }

    // 查询供应商详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-supplier:view')")
    public ResponseEntity<ApiResponse<ErpSupplier>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierService.getById(id)));
    }

    @GetMapping("/{id}/counterparty-subject-check")
    @PreAuthorize("hasAuthority('PERM_erp-supplier:edit')")
    public ResponseEntity<ApiResponse<ErpCounterpartyUnbindCheck>> checkRebind(@PathVariable Long id,
                                                                               @RequestParam(required = false) Long targetSubjectId) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierService.checkRebind(id, targetSubjectId)));
    }

    // 获取下一个供应商编码
    @GetMapping("/next-code")
    @PreAuthorize("hasAuthority('PERM_erp-supplier:add')")
    public ResponseEntity<ApiResponse<String>> nextCode() {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierService.nextCode()));
    }

    // 新增供应商
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-supplier:add')")
    public ResponseEntity<ApiResponse<ErpSupplier>> create(@Valid @RequestBody ErpSupplierCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierService.create(request)));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_erp-supplier:import')")
    public ResponseEntity<ApiResponse<ErpSupplierImportResult>> importSuppliers(@RequestParam("file") MultipartFile file,
                                                                                @RequestParam(value = "sourceName", required = false) String sourceName) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierService.importSuppliers(file, sourceName)));
    }

    @GetMapping("/import-batches")
    @PreAuthorize("hasAuthority('PERM_erp-supplier:import')")
    public ResponseEntity<ApiResponse<List<ErpSupplierImportBatchSummary>>> listImportBatches() {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierService.listImportBatches()));
    }

    @GetMapping("/import-batches/{batchId}/items")
    @PreAuthorize("hasAuthority('PERM_erp-supplier:import')")
    public ResponseEntity<ApiResponse<List<ErpSupplierImportItemView>>> listImportBatchItems(@PathVariable Long batchId) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierService.listImportBatchItems(batchId)));
    }

    // 更新供应商
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-supplier:edit')")
    public ResponseEntity<ApiResponse<ErpSupplier>> update(@PathVariable Long id,
                                                           @Valid @RequestBody ErpSupplierUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpSupplierService.update(id, request)));
    }

    // 删除供应商
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-supplier:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpSupplierService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
