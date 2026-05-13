package com.example.wms.dto.erp;

import java.math.BigDecimal;

// 销售单汇总
public class ErpSaleOrderSummary {
    private BigDecimal saleAmountTotal;
    private BigDecimal returnAmountTotal;
    private BigDecimal netSaleAmountTotal;
    private BigDecimal netGrossProfitTotal;

    public ErpSaleOrderSummary() {
    }

    public ErpSaleOrderSummary(BigDecimal saleAmountTotal,
                               BigDecimal returnAmountTotal,
                               BigDecimal netSaleAmountTotal,
                               BigDecimal netGrossProfitTotal) {
        this.saleAmountTotal = saleAmountTotal;
        this.returnAmountTotal = returnAmountTotal;
        this.netSaleAmountTotal = netSaleAmountTotal;
        this.netGrossProfitTotal = netGrossProfitTotal;
    }

    public BigDecimal getSaleAmountTotal() {
        return saleAmountTotal;
    }

    public void setSaleAmountTotal(BigDecimal saleAmountTotal) {
        this.saleAmountTotal = saleAmountTotal;
    }

    public BigDecimal getReturnAmountTotal() {
        return returnAmountTotal;
    }

    public void setReturnAmountTotal(BigDecimal returnAmountTotal) {
        this.returnAmountTotal = returnAmountTotal;
    }

    public BigDecimal getNetSaleAmountTotal() {
        return netSaleAmountTotal;
    }

    public void setNetSaleAmountTotal(BigDecimal netSaleAmountTotal) {
        this.netSaleAmountTotal = netSaleAmountTotal;
    }

    public BigDecimal getNetGrossProfitTotal() {
        return netGrossProfitTotal;
    }

    public void setNetGrossProfitTotal(BigDecimal netGrossProfitTotal) {
        this.netGrossProfitTotal = netGrossProfitTotal;
    }
}
