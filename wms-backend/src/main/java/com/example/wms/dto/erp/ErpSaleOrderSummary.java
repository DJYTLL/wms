package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * ERP 销售单用于返回汇总统计数据。

 */
public class ErpSaleOrderSummary {
    /**
     * 表示销售金额合计。
     */
    private BigDecimal saleAmountTotal;
    /**
     * 表示退货金额合计。
     */
    private BigDecimal returnAmountTotal;
    /**
     * 表示净销售金额合计。
     */
    private BigDecimal netSaleAmountTotal;
    /**
     * 表示净毛利润合计。
     */
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
