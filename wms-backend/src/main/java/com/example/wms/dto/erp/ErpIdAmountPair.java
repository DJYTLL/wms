package com.example.wms.dto.erp;

import java.math.BigDecimal;

// 按业务单据聚合的金额键值对
public class ErpIdAmountPair {
    private Long id;
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
