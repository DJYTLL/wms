package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.erp.ErpPrintLogCreateRequest;
import com.example.wms.entity.erp.ErpPrintLog;
import com.example.wms.service.erp.ErpPrintLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 打印日志接口（ERP进销存）
@RestController
@RequestMapping("/api/erp/print/logs")
public class ErpPrintLogController {
    private final ErpPrintLogService erpPrintLogService;

    public ErpPrintLogController(ErpPrintLogService erpPrintLogService) {
        this.erpPrintLogService = erpPrintLogService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('PERM_erp-sale:view','PERM_erp-sale-draft:print','PERM_erp-sale-approved:print','PERM_erp-purchase:view','PERM_erp-purchase-draft:print','PERM_erp-purchase-approved:print','PERM_erp-sale-return:view','PERM_erp-sale-return-draft:print','PERM_erp-sale-return-approved:print','PERM_erp-purchase-return:view','PERM_erp-purchase-return-draft:print','PERM_erp-purchase-return-approved:print','PERM_erp-receipt:view','PERM_erp-payment:view','PERM_erp-ar:view','PERM_erp-ap:view','PERM_erp-stock-count:view','PERM_erp-stock-transfer:view','PERM_erp-stock-init:view')")
    public ResponseEntity<ApiResponse<ErpPrintLog>> record(@Valid @RequestBody ErpPrintLogCreateRequest request,
                                                           HttpServletRequest httpServletRequest) {
        String ip = httpServletRequest.getRemoteAddr();
        String userAgent = httpServletRequest.getHeader("User-Agent");
        return ResponseEntity.ok(ApiResponse.ok(erpPrintLogService.record(request, ip, userAgent)));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERM_erp-sale:view','PERM_erp-sale-draft:print','PERM_erp-sale-approved:print','PERM_erp-purchase:view','PERM_erp-purchase-draft:print','PERM_erp-purchase-approved:print','PERM_erp-sale-return:view','PERM_erp-sale-return-draft:print','PERM_erp-sale-return-approved:print','PERM_erp-purchase-return:view','PERM_erp-purchase-return-draft:print','PERM_erp-purchase-return-approved:print','PERM_erp-receipt:view','PERM_erp-payment:view','PERM_erp-ar:view','PERM_erp-ap:view','PERM_erp-stock-count:view','PERM_erp-stock-transfer:view','PERM_erp-stock-init:view')")
    public ResponseEntity<ApiResponse<List<ErpPrintLog>>> list(@RequestParam String docType,
                                                               @RequestParam Long docId) {
        return ResponseEntity.ok(ApiResponse.ok(erpPrintLogService.listByDoc(docType, docId)));
    }
}
