package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.erp.ErpCounterpartyFinanceSummaryView;
import com.example.wms.dto.erp.ErpCustomerDebtView;
import com.example.wms.dto.erp.ErpFinanceSummary;
import com.example.wms.dto.erp.ErpSupplierDebtView;
import com.example.wms.service.erp.ErpFinanceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// ERP 财务汇总
@RestController
@RequestMapping("/api/erp/finance")
public class ErpFinanceController {
    private final ErpFinanceService erpFinanceService;

    public ErpFinanceController(ErpFinanceService erpFinanceService) {
        this.erpFinanceService = erpFinanceService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('PERM_erp-finance-summary:view')")
    public ApiResponse<ErpFinanceSummary> summary() {
        return ApiResponse.ok(erpFinanceService.getSummary());
    }

    @GetMapping("/customer-debts")
    @PreAuthorize("hasAuthority('PERM_erp-finance-customer-debt:view')")
    public ApiResponse<List<ErpCustomerDebtView>> customerDebts(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(erpFinanceService.listCustomerDebts(keyword));
    }

    @GetMapping("/supplier-debts")
    @PreAuthorize("hasAuthority('PERM_erp-finance-supplier-debt:view')")
    public ApiResponse<List<ErpSupplierDebtView>> supplierDebts(@RequestParam(required = false) String keyword) {
        try {
            return ApiResponse.ok(erpFinanceService.listSupplierDebts(keyword));
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    @GetMapping("/counterparty-subjects/summary")
    @PreAuthorize("hasAuthority('PERM_erp-finance-summary:view')")
    public ApiResponse<List<ErpCounterpartyFinanceSummaryView>> counterpartySubjectSummaries() {
        return ApiResponse.ok(erpFinanceService.listCounterpartySubjectSummaries());
    }
}
