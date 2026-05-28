package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.wms.dto.erp.ErpFinanceSummary;
import com.example.wms.dto.erp.ErpCustomerDebtView;
import com.example.wms.dto.erp.ErpSupplierDebtView;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.service.erp.ErpFinanceService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

// ERP 财务汇总服务实现
@Service
public class ErpFinanceServiceImpl implements ErpFinanceService {
    private static final String STATUS_RED_FLUSHED = "RED_FLUSHED";

    private final ErpAccountsReceivableMapper erpAccountsReceivableMapper;
    private final ErpAccountsPayableMapper erpAccountsPayableMapper;

    public ErpFinanceServiceImpl(ErpAccountsReceivableMapper erpAccountsReceivableMapper,
                                 ErpAccountsPayableMapper erpAccountsPayableMapper) {
        this.erpAccountsReceivableMapper = erpAccountsReceivableMapper;
        this.erpAccountsPayableMapper = erpAccountsPayableMapper;
    }

    @Override
    public ErpFinanceSummary getSummary() {
        Long tenantId = TenantContext.requireTenantId();
        BigDecimal customerDebtTotal = sumReceivable(tenantId);
        BigDecimal supplierDebtTotal = sumPayable(tenantId);
        return new ErpFinanceSummary(customerDebtTotal, supplierDebtTotal);
    }

    @Override
    public java.util.List<ErpCustomerDebtView> listCustomerDebts(String keyword) {
        Long tenantId = TenantContext.requireTenantId();
        return erpAccountsReceivableMapper.listCustomerDebt(tenantId, keyword);
    }

    @Override
    public java.util.List<ErpSupplierDebtView> listSupplierDebts(String keyword) {
        Long tenantId = TenantContext.requireTenantId();
        return erpAccountsPayableMapper.listSupplierDebt(tenantId, keyword);
    }

    private BigDecimal sumReceivable(Long tenantId) {
        QueryWrapper<com.example.wms.entity.erp.ErpAccountsReceivable> wrapper = new QueryWrapper<com.example.wms.entity.erp.ErpAccountsReceivable>()
            .select("COALESCE(SUM(unpaid_amount), 0)")
            .eq("tenant_id", tenantId)
            .ne("status", STATUS_RED_FLUSHED);
        List<Object> result = erpAccountsReceivableMapper.selectObjs(wrapper);
        return toBigDecimal(result.isEmpty() ? null : result.get(0));
    }

    private BigDecimal sumPayable(Long tenantId) {
        QueryWrapper<com.example.wms.entity.erp.ErpAccountsPayable> wrapper = new QueryWrapper<com.example.wms.entity.erp.ErpAccountsPayable>()
            .select("COALESCE(SUM(unpaid_amount), 0)")
            .eq("tenant_id", tenantId)
            .ne("status", STATUS_RED_FLUSHED);
        List<Object> result = erpAccountsPayableMapper.selectObjs(wrapper);
        return toBigDecimal(result.isEmpty() ? null : result.get(0));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}
