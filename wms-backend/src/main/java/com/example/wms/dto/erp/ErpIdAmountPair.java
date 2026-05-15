package com.example.wms.dto.erp;

import java.math.BigDecimal;

/**

 * 用于传输ERP ID 与金额对应项相关数据。

 */
public class ErpIdAmountPair {
    /**
     * 表示数据的主键 ID。
     */
    private Long id;
    /**
     * 表示金额。
     */
    private BigDecimal amount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
