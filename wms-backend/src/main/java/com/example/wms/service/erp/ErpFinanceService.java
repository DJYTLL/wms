package com.example.wms.service.erp;

import com.example.wms.dto.erp.ErpCounterpartyFinanceSummaryView;
import com.example.wms.dto.erp.ErpCounterpartyFinanceDetailRow;
import com.example.wms.dto.erp.ErpFinanceSummary;
import com.example.wms.dto.erp.ErpCustomerDebtView;
import com.example.wms.dto.erp.ErpSupplierDebtView;

import java.util.List;

// ERP 财务汇总服务
public interface ErpFinanceService {
    ErpFinanceSummary getSummary();

    List<ErpCustomerDebtView> listCustomerDebts(String keyword);

    List<ErpSupplierDebtView> listSupplierDebts(String keyword);

    List<ErpCounterpartyFinanceSummaryView> listCounterpartySubjectSummaries();

    List<ErpCounterpartyFinanceDetailRow> listCounterpartySubjectDetails(Long subjectId);
}
