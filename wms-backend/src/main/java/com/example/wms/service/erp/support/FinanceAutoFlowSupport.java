package com.example.wms.service.erp.support;

import com.example.wms.entity.erp.ErpAccountsPayable;
import com.example.wms.entity.erp.ErpAccountsReceivable;
import com.example.wms.entity.erp.ErpPayment;
import com.example.wms.entity.erp.ErpReceipt;

import java.util.Objects;

/**
 * 审核后财务自动联动的共享规则，避免四类业务单据各自解释配置。
 */
public final class FinanceAutoFlowSupport {
    public static final String MANAGED_BY_SYSTEM = "SYSTEM_MANAGED";
    public static final String MANUAL_TAKEOVER = "MANUAL_TAKEOVER";
    public static final String RED_FLUSHED = "RED_FLUSHED";

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_APPROVED = "APPROVED";

    public static final String SOURCE_SALE_ORDER = "SALE_ORDER";
    public static final String SOURCE_PURCHASE_ORDER = "PURCHASE_ORDER";
    public static final String SOURCE_SALE_RETURN = "SALE_RETURN";
    public static final String SOURCE_PURCHASE_RETURN = "PURCHASE_RETURN";

    private FinanceAutoFlowSupport() {
    }

    public static boolean shouldGeneratePaymentDocument(FinanceAutoFlowMode mode) {
        return mode == FinanceAutoFlowMode.AR_AP_WITH_DRAFT_PAYMENT
            || mode == FinanceAutoFlowMode.AR_AP_WITH_APPROVED_PAYMENT;
    }

    public static String paymentDocumentStatus(FinanceAutoFlowMode mode) {
        return mode == FinanceAutoFlowMode.AR_AP_WITH_DRAFT_PAYMENT ? STATUS_DRAFT : STATUS_APPROVED;
    }

    public static boolean isSystemManaged(ErpReceipt receipt, String legacyRemark) {
        if (receipt == null) {
            return false;
        }
        if (Boolean.TRUE.equals(receipt.getAutoFlowGenerated())) {
            String state = receipt.getAutoFlowManagedState();
            return state == null || state.isBlank() || MANAGED_BY_SYSTEM.equals(state);
        }
        return Objects.equals(legacyRemark, receipt.getRemark());
    }

    public static boolean isSystemManaged(ErpPayment payment, String legacyRemark) {
        if (payment == null) {
            return false;
        }
        if (Boolean.TRUE.equals(payment.getAutoFlowGenerated())) {
            String state = payment.getAutoFlowManagedState();
            return state == null || state.isBlank() || MANAGED_BY_SYSTEM.equals(state);
        }
        return Objects.equals(legacyRemark, payment.getRemark());
    }

    public static void markReceivable(ErpAccountsReceivable receivable,
                                      String sourceType,
                                      Long sourceId,
                                      FinanceAutoFlowMode mode) {
        receivable.setSourceDocumentType(sourceType);
        receivable.setSourceDocumentId(sourceId);
        receivable.setSourceBusinessFlow(sourceType);
        receivable.setAutoFlowGenerated(true);
        receivable.setAutoFlowMode(mode.name());
        receivable.setAutoFlowManagedState(MANAGED_BY_SYSTEM);
    }

    public static void markPayable(ErpAccountsPayable payable,
                                   String sourceType,
                                   Long sourceId,
                                   FinanceAutoFlowMode mode) {
        payable.setSourceDocumentType(sourceType);
        payable.setSourceDocumentId(sourceId);
        payable.setSourceBusinessFlow(sourceType);
        payable.setAutoFlowGenerated(true);
        payable.setAutoFlowMode(mode.name());
        payable.setAutoFlowManagedState(MANAGED_BY_SYSTEM);
    }

    public static void markReceipt(ErpReceipt receipt,
                                   String sourceType,
                                   Long sourceId,
                                   FinanceAutoFlowMode mode) {
        receipt.setSourceDocumentType(sourceType);
        receipt.setSourceDocumentId(sourceId);
        receipt.setSourceBusinessFlow(sourceType);
        receipt.setAutoFlowGenerated(true);
        receipt.setAutoFlowMode(mode.name());
        receipt.setAutoFlowManagedState(MANAGED_BY_SYSTEM);
    }

    public static void markPayment(ErpPayment payment,
                                   String sourceType,
                                   Long sourceId,
                                   FinanceAutoFlowMode mode) {
        payment.setSourceDocumentType(sourceType);
        payment.setSourceDocumentId(sourceId);
        payment.setSourceBusinessFlow(sourceType);
        payment.setAutoFlowGenerated(true);
        payment.setAutoFlowMode(mode.name());
        payment.setAutoFlowManagedState(MANAGED_BY_SYSTEM);
    }

    public static void markReceiptManualTakeover(ErpReceipt receipt) {
        if (receipt != null && Boolean.TRUE.equals(receipt.getAutoFlowGenerated())) {
            receipt.setAutoFlowManagedState(MANUAL_TAKEOVER);
        }
    }

    public static void markPaymentManualTakeover(ErpPayment payment) {
        if (payment != null && Boolean.TRUE.equals(payment.getAutoFlowGenerated())) {
            payment.setAutoFlowManagedState(MANUAL_TAKEOVER);
        }
    }

    public static void assertReceiptCanBeOverwritten(ErpReceipt receipt, String sourceNo, String legacyRemark) {
        if (receipt == null || isSystemManaged(receipt, legacyRemark)) {
            return;
        }
        String receiptNo = receipt.getReceiptNo() == null ? String.valueOf(receipt.getId()) : receipt.getReceiptNo();
        throw new IllegalArgumentException("收款单" + receiptNo + "已被人工操作，系统不能自动覆盖"
            + sourceLabel(sourceNo) + "。请先红冲或选择恢复系统默认。");
    }

    public static void assertPaymentCanBeOverwritten(ErpPayment payment, String sourceNo, String legacyRemark) {
        if (payment == null || isSystemManaged(payment, legacyRemark)) {
            return;
        }
        String paymentNo = payment.getPaymentNo() == null ? String.valueOf(payment.getId()) : payment.getPaymentNo();
        throw new IllegalArgumentException("付款单" + paymentNo + "已被人工操作，系统不能自动覆盖"
            + sourceLabel(sourceNo) + "。请先红冲或选择恢复系统默认。");
    }

    private static String sourceLabel(String sourceNo) {
        if (sourceNo == null || sourceNo.isBlank()) {
            return "";
        }
        return "（来源单据：" + sourceNo + "）";
    }
}
