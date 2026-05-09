package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.erp.ErpProductFitmentCreateRequest;
import com.example.wms.dto.erp.ErpProductFitmentUpdateRequest;
import com.example.wms.entity.erp.ErpProductFitment;
import com.example.wms.service.erp.ErpProductFitmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP商品适配车型关系接口
@RestController
@RequestMapping("/api/erp/product-fitments")
public class ErpProductFitmentController {
    private final ErpProductFitmentService erpProductFitmentService;

    public ErpProductFitmentController(ErpProductFitmentService erpProductFitmentService) {
        this.erpProductFitmentService = erpProductFitmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-product-fitment:view')")
    public ResponseEntity<ApiResponse<List<ErpProductFitment>>> list(@RequestParam(required = false) Long productId,
                                                                     @RequestParam(required = false) Long modelId) {
        return ResponseEntity.ok(ApiResponse.ok(erpProductFitmentService.listAll(productId, modelId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-product-fitment:view')")
    public ResponseEntity<ApiResponse<ErpProductFitment>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(erpProductFitmentService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-product-fitment:add')")
    public ResponseEntity<ApiResponse<ErpProductFitment>> create(@Valid @RequestBody ErpProductFitmentCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpProductFitmentService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-product-fitment:edit')")
    public ResponseEntity<ApiResponse<ErpProductFitment>> update(@PathVariable Long id,
                                                                 @Valid @RequestBody ErpProductFitmentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(erpProductFitmentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-product-fitment:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        erpProductFitmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
