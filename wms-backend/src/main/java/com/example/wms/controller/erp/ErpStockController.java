package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockBalanceOption;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockTxn;
import com.example.wms.service.erp.ErpStockService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ERP库存接口
@RestController
@RequestMapping("/api/erp/stock")
public class ErpStockController {
    private final ErpStockService erpStockService;

    public ErpStockController(ErpStockService erpStockService) {
        this.erpStockService = erpStockService;
    }

    // 分页查询库存余额
    @GetMapping("/balances/page")
    @PreAuthorize("hasAuthority('PERM_erp-stock:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpStockBalance>>> pageBalance(@RequestParam(defaultValue = "1") long page,
                                                                                  @RequestParam(defaultValue = "20") long size,
                                                                                  @RequestParam(required = false) Long productId,
                                                                                  @RequestParam(required = false) Long warehouseId,
                                                                                  @RequestParam(required = false) Long locationId) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockService.pageBalance(page, size, productId, warehouseId, locationId)));
    }

    // 查询指定商品的库存明细（仓库-库位）
    @GetMapping("/balances/by-product")
    @PreAuthorize("hasAuthority('PERM_erp-stock:view')")
    public ResponseEntity<ApiResponse<List<ErpStockBalanceOption>>> listBalancesByProduct(@RequestParam Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockService.listBalancesByProduct(productId)));
    }

    // 分页查询库存流水
    @GetMapping("/txns/page")
    @PreAuthorize("hasAuthority('PERM_erp-stock-txn:view')")
    public ResponseEntity<ApiResponse<PageResponse<ErpStockTxn>>> pageTxn(@RequestParam(defaultValue = "1") long page,
                                                                          @RequestParam(defaultValue = "20") long size,
                                                                          @RequestParam(required = false) String bizType,
                                                                          @RequestParam(required = false) Long bizId,
                                                                          @RequestParam(required = false) Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(erpStockService.pageTxn(page, size, bizType, bizId, productId)));
    }
}
