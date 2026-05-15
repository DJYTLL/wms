package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.wms.dto.erp.ErpPrintLogCreateRequest;
import com.example.wms.entity.erp.ErpPrintLog;
import com.example.wms.entity.erp.ErpAccountsPayable;
import com.example.wms.entity.erp.ErpAccountsReceivable;
import com.example.wms.entity.erp.ErpPayment;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import com.example.wms.entity.erp.ErpPurchaseReturn;
import com.example.wms.entity.erp.ErpReceipt;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.entity.erp.ErpSaleReturn;
import com.example.wms.entity.erp.ErpStockCount;
import com.example.wms.entity.erp.ErpStockTransfer;
import com.example.wms.mapper.erp.ErpPrintLogMapper;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPurchaseOrderMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.mapper.erp.ErpStockCountMapper;
import com.example.wms.mapper.erp.ErpStockTransferMapper;
import com.example.wms.service.erp.ErpPrintLogService;
import com.example.wms.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 打印日志服务实现（ERP进销存）
@Service
public class ErpPrintLogServiceImpl implements ErpPrintLogService {
    private final ErpPrintLogMapper erpPrintLogMapper;
    private final ErpSaleOrderMapper erpSaleOrderMapper;
    private final ErpPurchaseOrderMapper erpPurchaseOrderMapper;
    private final ErpSaleReturnMapper erpSaleReturnMapper;
    private final ErpPurchaseReturnMapper erpPurchaseReturnMapper;
    private final ErpReceiptMapper erpReceiptMapper;
    private final ErpPaymentMapper erpPaymentMapper;
    private final ErpAccountsReceivableMapper erpAccountsReceivableMapper;
    private final ErpAccountsPayableMapper erpAccountsPayableMapper;
    private final ErpStockCountMapper erpStockCountMapper;
    private final ErpStockTransferMapper erpStockTransferMapper;

    public ErpPrintLogServiceImpl(ErpPrintLogMapper erpPrintLogMapper,
                                  ErpSaleOrderMapper erpSaleOrderMapper,
                                  ErpPurchaseOrderMapper erpPurchaseOrderMapper,
                                  ErpSaleReturnMapper erpSaleReturnMapper,
                                  ErpPurchaseReturnMapper erpPurchaseReturnMapper,
                                  ErpReceiptMapper erpReceiptMapper,
                                  ErpPaymentMapper erpPaymentMapper,
                                  ErpAccountsReceivableMapper erpAccountsReceivableMapper,
                                  ErpAccountsPayableMapper erpAccountsPayableMapper,
                                  ErpStockCountMapper erpStockCountMapper,
                                  ErpStockTransferMapper erpStockTransferMapper) {
        this.erpPrintLogMapper = erpPrintLogMapper;
        this.erpSaleOrderMapper = erpSaleOrderMapper;
        this.erpPurchaseOrderMapper = erpPurchaseOrderMapper;
        this.erpSaleReturnMapper = erpSaleReturnMapper;
        this.erpPurchaseReturnMapper = erpPurchaseReturnMapper;
        this.erpReceiptMapper = erpReceiptMapper;
        this.erpPaymentMapper = erpPaymentMapper;
        this.erpAccountsReceivableMapper = erpAccountsReceivableMapper;
        this.erpAccountsPayableMapper = erpAccountsPayableMapper;
        this.erpStockCountMapper = erpStockCountMapper;
        this.erpStockTransferMapper = erpStockTransferMapper;
    }

    @Override
    public ErpPrintLog record(ErpPrintLogCreateRequest request, String clientIp, String userAgent) {
        Long tenantId = TenantContext.requireTenantId();
        String docType = normalizeDocType(request.docType());
        Long docId = request.docId();
        String docNo = resolveDocNo(tenantId, docType, docId);

        ErpPrintLog log = new ErpPrintLog();
        log.setTenantId(tenantId);
        log.setDocType(docType);
        log.setDocId(docId);
        log.setDocNo(docNo);
        log.setTemplateId(request.templateId());
        log.setPrintedBy(resolveActor());
        log.setPrintedAt(Instant.now());
        log.setClientIp(clientIp);
        log.setUserAgent(userAgent);
        erpPrintLogMapper.insert(log);

        return log;
    }

    @Override
    public List<ErpPrintLog> listByDoc(String docType, Long docId) {
        Long tenantId = TenantContext.requireTenantId();
        String normalized = normalizeDocType(docType);
        return erpPrintLogMapper.selectList(new QueryWrapper<ErpPrintLog>()
            .eq("tenant_id", tenantId)
            .eq("doc_type", normalized)
            .eq("doc_id", docId)
            .orderByDesc("printed_at"));
    }

    private String resolveDocNo(Long tenantId, String docType, Long docId) {
        if ("SALE_ORDER".equals(docType)) {
            ErpSaleOrder order = erpSaleOrderMapper.selectOne(new QueryWrapper<ErpSaleOrder>()
                .eq("tenant_id", tenantId)
                .eq("id", docId));
            if (order == null) {
                throw new IllegalArgumentException("销售单不存在");
            }
            bumpPrintCount(tenantId, "erp_sale_order", docId);
            return order.getOrderNo();
        }
        if ("PURCHASE_ORDER".equals(docType)) {
            ErpPurchaseOrder order = erpPurchaseOrderMapper.selectOne(new QueryWrapper<ErpPurchaseOrder>()
                .eq("tenant_id", tenantId)
                .eq("id", docId));
            if (order == null) {
                throw new IllegalArgumentException("采购单不存在");
            }
            bumpPrintCount(tenantId, "erp_purchase_order", docId);
            return order.getOrderNo();
        }
        if ("SALE_RETURN".equals(docType)) {
            ErpSaleReturn order = erpSaleReturnMapper.selectOne(new QueryWrapper<ErpSaleReturn>()
                .eq("tenant_id", tenantId)
                .eq("id", docId));
            if (order == null) {
                throw new IllegalArgumentException("销售退货单不存在");
            }
            bumpPrintCount(tenantId, "erp_sale_return", docId);
            return order.getOrderNo();
        }
        if ("PURCHASE_RETURN".equals(docType)) {
            ErpPurchaseReturn order = erpPurchaseReturnMapper.selectOne(new QueryWrapper<ErpPurchaseReturn>()
                .eq("tenant_id", tenantId)
                .eq("id", docId));
            if (order == null) {
                throw new IllegalArgumentException("采购退货单不存在");
            }
            bumpPrintCount(tenantId, "erp_purchase_return", docId);
            return order.getOrderNo();
        }
        if ("RECEIPT".equals(docType)) {
            ErpReceipt receipt = erpReceiptMapper.selectOne(new QueryWrapper<ErpReceipt>()
                .eq("tenant_id", tenantId)
                .eq("id", docId));
            if (receipt == null) {
                throw new IllegalArgumentException("收款单不存在");
            }
            bumpPrintCount(tenantId, "erp_receipt", docId);
            return receipt.getReceiptNo();
        }
        if ("PAYMENT".equals(docType)) {
            ErpPayment payment = erpPaymentMapper.selectOne(new QueryWrapper<ErpPayment>()
                .eq("tenant_id", tenantId)
                .eq("id", docId));
            if (payment == null) {
                throw new IllegalArgumentException("付款单不存在");
            }
            bumpPrintCount(tenantId, "erp_payment", docId);
            return payment.getPaymentNo();
        }
        if ("ACCOUNTS_RECEIVABLE".equals(docType)) {
            ErpAccountsReceivable receivable = erpAccountsReceivableMapper.selectOne(new QueryWrapper<ErpAccountsReceivable>()
                .eq("tenant_id", tenantId)
                .eq("id", docId));
            if (receivable == null) {
                throw new IllegalArgumentException("应收单不存在");
            }
            bumpPrintCount(tenantId, "erp_accounts_receivable", docId);
            return receivable.getOrderNo();
        }
        if ("ACCOUNTS_PAYABLE".equals(docType)) {
            ErpAccountsPayable payable = erpAccountsPayableMapper.selectOne(new QueryWrapper<ErpAccountsPayable>()
                .eq("tenant_id", tenantId)
                .eq("id", docId));
            if (payable == null) {
                throw new IllegalArgumentException("应付单不存在");
            }
            bumpPrintCount(tenantId, "erp_accounts_payable", docId);
            return payable.getOrderNo();
        }
        if ("STOCK_COUNT".equals(docType) || "STOCK_INIT".equals(docType)) {
            ErpStockCount count = erpStockCountMapper.selectOne(new QueryWrapper<ErpStockCount>()
                .eq("tenant_id", tenantId)
                .eq("id", docId));
            if (count == null) {
                throw new IllegalArgumentException("库存盘点单不存在");
            }
            bumpPrintCount(tenantId, "erp_stock_count", docId);
            return count.getCountNo();
        }
        if ("STOCK_TRANSFER".equals(docType)) {
            ErpStockTransfer transfer = erpStockTransferMapper.selectOne(new QueryWrapper<ErpStockTransfer>()
                .eq("tenant_id", tenantId)
                .eq("id", docId));
            if (transfer == null) {
                throw new IllegalArgumentException("库存移库单不存在");
            }
            bumpPrintCount(tenantId, "erp_stock_transfer", docId);
            return transfer.getTransferNo();
        }
        throw new IllegalArgumentException("不支持的单据类型");
    }

    private void bumpPrintCount(Long tenantId, String table, Long docId) {
        if ("erp_sale_order".equals(table)) {
            erpSaleOrderMapper.update(null, new UpdateWrapper<ErpSaleOrder>()
                .eq("tenant_id", tenantId)
                .eq("id", docId)
                .setSql("print_count = COALESCE(print_count, 0) + 1")
                .set("last_printed_at", Instant.now()));
            return;
        }
        if ("erp_purchase_order".equals(table)) {
            erpPurchaseOrderMapper.update(null, new UpdateWrapper<ErpPurchaseOrder>()
                .eq("tenant_id", tenantId)
                .eq("id", docId)
                .setSql("print_count = COALESCE(print_count, 0) + 1")
                .set("last_printed_at", Instant.now()));
            return;
        }
        if ("erp_sale_return".equals(table)) {
            erpSaleReturnMapper.update(null, new UpdateWrapper<ErpSaleReturn>()
                .eq("tenant_id", tenantId)
                .eq("id", docId)
                .setSql("print_count = COALESCE(print_count, 0) + 1")
                .set("last_printed_at", Instant.now()));
            return;
        }
        if ("erp_purchase_return".equals(table)) {
            erpPurchaseReturnMapper.update(null, new UpdateWrapper<ErpPurchaseReturn>()
                .eq("tenant_id", tenantId)
                .eq("id", docId)
                .setSql("print_count = COALESCE(print_count, 0) + 1")
                .set("last_printed_at", Instant.now()));
            return;
        }
        if ("erp_receipt".equals(table)) {
            erpReceiptMapper.update(null, new UpdateWrapper<ErpReceipt>()
                .eq("tenant_id", tenantId)
                .eq("id", docId)
                .setSql("print_count = COALESCE(print_count, 0) + 1")
                .set("last_printed_at", Instant.now()));
            return;
        }
        if ("erp_payment".equals(table)) {
            erpPaymentMapper.update(null, new UpdateWrapper<ErpPayment>()
                .eq("tenant_id", tenantId)
                .eq("id", docId)
                .setSql("print_count = COALESCE(print_count, 0) + 1")
                .set("last_printed_at", Instant.now()));
            return;
        }
        if ("erp_accounts_receivable".equals(table)) {
            erpAccountsReceivableMapper.update(null, new UpdateWrapper<ErpAccountsReceivable>()
                .eq("tenant_id", tenantId)
                .eq("id", docId)
                .setSql("print_count = COALESCE(print_count, 0) + 1")
                .set("last_printed_at", Instant.now()));
            return;
        }
        if ("erp_accounts_payable".equals(table)) {
            erpAccountsPayableMapper.update(null, new UpdateWrapper<ErpAccountsPayable>()
                .eq("tenant_id", tenantId)
                .eq("id", docId)
                .setSql("print_count = COALESCE(print_count, 0) + 1")
                .set("last_printed_at", Instant.now()));
            return;
        }
        if ("erp_stock_count".equals(table)) {
            erpStockCountMapper.update(null, new UpdateWrapper<ErpStockCount>()
                .eq("tenant_id", tenantId)
                .eq("id", docId)
                .setSql("print_count = COALESCE(print_count, 0) + 1")
                .set("last_printed_at", Instant.now()));
            return;
        }
        if ("erp_stock_transfer".equals(table)) {
            erpStockTransferMapper.update(null, new UpdateWrapper<ErpStockTransfer>()
                .eq("tenant_id", tenantId)
                .eq("id", docId)
                .setSql("print_count = COALESCE(print_count, 0) + 1")
                .set("last_printed_at", Instant.now()));
        }
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getName();
    }

    private String normalizeDocType(String docType) {
        if (docType == null) {
            throw new IllegalArgumentException("单据类型不能为空");
        }
        String normalized = docType.trim().toUpperCase();
        return switch (normalized) {
            case "SALE_ORDER",
                "PURCHASE_ORDER",
                "SALE_RETURN",
                "PURCHASE_RETURN",
                "RECEIPT",
                "PAYMENT",
                "ACCOUNTS_RECEIVABLE",
                "ACCOUNTS_PAYABLE",
                "STOCK_COUNT",
                "STOCK_TRANSFER",
                "STOCK_INIT" -> normalized;
            default -> throw new IllegalArgumentException("不支持的单据类型");
        };
    }
}
