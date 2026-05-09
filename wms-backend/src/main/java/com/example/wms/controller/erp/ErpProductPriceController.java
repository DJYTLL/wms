package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.erp.ErpProductPriceResolveResponse;
import com.example.wms.dto.erp.ErpProductPriceSaveRequest;
import com.example.wms.entity.erp.ErpProductPrice;
import com.example.wms.service.erp.ErpProductPriceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

// ERP商品价格接口
@RestController
@RequestMapping("/api/erp/product-prices")
public class ErpProductPriceController {
    private final ErpProductPriceService erpProductPriceService;

    public ErpProductPriceController(ErpProductPriceService erpProductPriceService) {
        this.erpProductPriceService = erpProductPriceService;
    }

    // 按商品查询价格
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_erp-product:view')")
    public ResponseEntity<ApiResponse<List<ErpProductPrice>>> list(@RequestParam Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(erpProductPriceService.listByProduct(productId)));
    }

    // 保存商品价格
    @PutMapping
    @PreAuthorize("hasAuthority('PERM_erp-product:edit')")
    public ResponseEntity<ApiResponse<Void>> save(@RequestParam Long productId,
                                                  @Valid @RequestBody ErpProductPriceSaveRequest request) {
        erpProductPriceService.saveForProduct(productId, request.items());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 解析商品价格
    @GetMapping("/resolve")
    @PreAuthorize("hasAuthority('PERM_erp-product:view')")
    public ResponseEntity<ApiResponse<ErpProductPriceResolveResponse>> resolve(@RequestParam Long productId,
                                                                               @RequestParam Long customerCategoryId) {
        BigDecimal price = erpProductPriceService.resolvePrice(productId, customerCategoryId);
        return ResponseEntity.ok(ApiResponse.ok(new ErpProductPriceResolveResponse(price)));
    }
}
