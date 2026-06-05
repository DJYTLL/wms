package com.example.wms.controller.erp;

import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpProductImportBatchSummary;
import com.example.wms.dto.erp.ErpProductCreateRequest;
import com.example.wms.dto.erp.ErpProductImportItemView;
import com.example.wms.dto.erp.ErpProductImportPreview;
import com.example.wms.dto.erp.ErpProductImportResult;
import com.example.wms.dto.erp.ErpProductUpdateRequest;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.service.erp.ErpProductService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// ERP商品管理接口
@RestController
@RequestMapping("/api/erp/products")
public class ErpProductController {
    private final ErpProductService erpProductService;

    public ErpProductController(ErpProductService erpProductService) {
        this.erpProductService = erpProductService;
    }

    // 查询商品列表
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-product:view')")
    public ResponseEntity<ApiResponse<List<ErpProduct>>> list(@RequestParam(required = false) String keyword,
                                                              @RequestParam(required = false) Boolean enabled,
                                                              @RequestParam(required = false) Long categoryId) {
        List<ErpProduct> products = erpProductService.listAll(keyword, enabled, categoryId);
        stripCostPriceIfNeeded(products);
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    // 业务选项：仅返回启用商品
    @GetMapping("/options")
    @PreAuthorize("hasAuthority('PERM_erp-product:view')")
    public ResponseEntity<ApiResponse<List<ErpProduct>>> options(@RequestParam(required = false) Long categoryId) {
        List<ErpProduct> products = erpProductService.listAll(null, true, categoryId);
        stripCostPriceIfNeeded(products);
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    // 分页查询商品
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('PERM_erp-product:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpProduct>>> page(@RequestParam(defaultValue = "1") long page,
                                                                      @RequestParam(defaultValue = "20") long size,
                                                                      @RequestParam(required = false) String keyword,
                                                                      @RequestParam(required = false) Boolean enabled,
                                                                      @RequestParam(required = false) Long categoryId) {
        PageResponse<ErpProduct> result = erpProductService.page(page, size, keyword, enabled, categoryId);
        stripCostPriceIfNeeded(result.items());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // 查询商品详情
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-product:view')")
    public ResponseEntity<ApiResponse<ErpProduct>> get(@PathVariable Long id) {
        ErpProduct product = erpProductService.getById(id);
        stripCostPriceIfNeeded(product);
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    // 获取下一个商品编码
    @GetMapping("/next-code")
    @PreAuthorize("hasAuthority('PERM_erp-product:add')")
    public ResponseEntity<ApiResponse<String>> nextCode() {
        return ResponseEntity.ok(ApiResponse.ok(erpProductService.nextCode()));
    }

    // 新增商品
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_erp-product:add')")
    public ResponseEntity<ApiResponse<ErpProduct>> create(@Valid @RequestBody ErpProductCreateRequest request) {
        ErpProduct product = erpProductService.create(request);
        stripCostPriceIfNeeded(product);
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_erp-product:import')")
    public ResponseEntity<ApiResponse<ErpProductImportResult>> importProducts(@RequestParam("file") MultipartFile file,
                                                                              @RequestParam(value = "sourceName", required = false) String sourceName,
                                                                              @RequestParam(value = "fieldMapping", required = false) String fieldMapping) {
        return ResponseEntity.ok(ApiResponse.ok(erpProductService.importProducts(file, sourceName, fieldMapping)));
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_erp-product:import')")
    public ResponseEntity<ApiResponse<ErpProductImportPreview>> previewImport(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(erpProductService.previewImport(file)));
    }

    @GetMapping("/import-batches")
    @PreAuthorize("hasAuthority('PERM_erp-product:import')")
    public ResponseEntity<ApiResponse<List<ErpProductImportBatchSummary>>> listImportBatches() {
        return ResponseEntity.ok(ApiResponse.ok(erpProductService.listImportBatches()));
    }

    @GetMapping("/import-batches/{batchId}/items")
    @PreAuthorize("hasAuthority('PERM_erp-product:import')")
    public ResponseEntity<ApiResponse<List<ErpProductImportItemView>>> listImportBatchItems(@PathVariable("batchId") Long batchId) {
        return ResponseEntity.ok(ApiResponse.ok(erpProductService.listImportBatchItems(batchId)));
    }

    // 更新商品
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-product:edit')")
    public ResponseEntity<ApiResponse<ErpProduct>> update(@PathVariable Long id,
                                                          @Valid @RequestBody ErpProductUpdateRequest request) {
        ErpProduct product = erpProductService.update(id, request);
        stripCostPriceIfNeeded(product);
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    // 删除商品
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_erp-product:delete')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @Valid @RequestBody DeleteRequest request) {
        try (DeleteAuditScope ignored = DeleteAuditScope.bind(request.reason())) {
            erpProductService.delete(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private void stripCostPriceIfNeeded(List<ErpProduct> products) {
        if (products == null || canViewCostPrice()) {
            return;
        }
        for (ErpProduct product : products) {
            if (product != null) {
                product.setCostPrice(null);
            }
        }
    }

    private void stripCostPriceIfNeeded(ErpProduct product) {
        if (product == null || canViewCostPrice()) {
            return;
        }
        product.setCostPrice(null);
    }

    private boolean canViewCostPrice() {
        return hasAuthority("PERM_erp-product:cost:view") || hasAuthority("PERM_erp-product:cost:edit");
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(item -> authority.equals(item.getAuthority()));
    }
}
