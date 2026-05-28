-- Seed default print templates for every ERP document type supported by the UI.
-- Existing system template codes are refreshed in place, while user-created
-- default templates keep their default status.
DROP TABLE IF EXISTS tmp_erp_print_template_defaults;

CREATE TEMP TABLE tmp_erp_print_template_defaults ON COMMIT DROP AS
SELECT *
FROM (
    VALUES
    (
        'SALE_DEFAULT',
        '销售单默认模板',
        'SALE_ORDER',
        '销售单',
        'Sales Order',
        '感谢您的惠顾',
        '{"headerFields":["orderNo","orderAt","customerName","settlementMethod","deliveryMethod","paidAmount","discountAmount","printCount","lastPrintedAt","remark"],"detailColumns":["productCode","productName","warehouse","location","qty","price","amount","taxRate","amountInclTax","remark"],"showTotals":true,"columnWidths":{"productCode":10,"productName":18,"warehouse":12,"location":12,"qty":6,"price":10,"amount":10,"taxRate":8,"amountInclTax":12,"remark":16}}',
        10
    ),
    (
        'PURCHASE_DEFAULT',
        '采购单默认模板',
        'PURCHASE_ORDER',
        '采购单',
        'Purchase Order',
        '请核对无误后签字',
        '{"headerFields":["orderNo","orderAt","supplierName","paymentMethod","paidAmount","discountAmount","printCount","lastPrintedAt","remark"],"detailColumns":["productCode","productName","warehouse","location","qty","price","amount","taxRate","amountInclTax","remark"],"showTotals":true,"columnWidths":{"productCode":10,"productName":18,"warehouse":12,"location":12,"qty":6,"price":10,"amount":10,"taxRate":8,"amountInclTax":12,"remark":16}}',
        20
    ),
    (
        'SALE_RETURN_DEFAULT',
        '销售退货单默认模板',
        'SALE_RETURN',
        '销售退货单',
        'Sales Return',
        '请核对退货明细后签字',
        '{"headerFields":["orderNo","orderAt","customerName","returnSource","returnType","saleOrderNo","printCount","lastPrintedAt","remark"],"detailColumns":["productCode","productName","warehouse","location","qty","price","amount","taxRate","amountInclTax","remark"],"showTotals":true,"columnWidths":{"productCode":10,"productName":18,"warehouse":12,"location":12,"qty":6,"price":10,"amount":10,"taxRate":8,"amountInclTax":12,"remark":16}}',
        30
    ),
    (
        'PURCHASE_RETURN_DEFAULT',
        '采购退货单默认模板',
        'PURCHASE_RETURN',
        '采购退货单',
        'Purchase Return',
        '请核对退货明细后签字',
        '{"headerFields":["orderNo","orderAt","supplierName","returnSource","returnType","purchaseOrderNo","printCount","lastPrintedAt","remark"],"detailColumns":["productCode","productName","warehouse","location","qty","price","amount","taxRate","amountInclTax","remark"],"showTotals":true,"columnWidths":{"productCode":10,"productName":18,"warehouse":12,"location":12,"qty":6,"price":10,"amount":10,"taxRate":8,"amountInclTax":12,"remark":16}}',
        40
    ),
    (
        'RECEIPT_DEFAULT',
        '收款单默认模板',
        'RECEIPT',
        '收款单',
        'Receipt',
        '感谢您的付款',
        '{"headerFields":["receiptNo","receivedAt","customerName","settlementMethod","receiptAmount","discountAmount","status","printCount","lastPrintedAt","remark"],"detailColumns":["orderNo","allocatedAmount","allocatedDiscount","allocatedTotal"],"showTotals":true,"columnWidths":{"orderNo":14,"allocatedAmount":12,"allocatedDiscount":12,"allocatedTotal":12}}',
        50
    ),
    (
        'PAYMENT_DEFAULT',
        '付款单默认模板',
        'PAYMENT',
        '付款单',
        'Payment',
        '请核对付款信息',
        '{"headerFields":["paymentNo","paidAt","supplierName","paymentMethod","paymentAmount","discountAmount","status","printCount","lastPrintedAt","remark"],"detailColumns":["orderNo","allocatedAmount","allocatedDiscount","allocatedTotal"],"showTotals":true,"columnWidths":{"orderNo":14,"allocatedAmount":12,"allocatedDiscount":12,"allocatedTotal":12}}',
        60
    ),
    (
        'AR_DEFAULT',
        '应收单默认模板',
        'ACCOUNTS_RECEIVABLE',
        '应收单',
        'Accounts Receivable',
        '请核对应收与核销记录',
        '{"headerFields":["receivableNo","orderNo","customerName","totalAmount","paidAmount","discountAmount","unpaidAmount","status","printCount","lastPrintedAt","remark"],"detailColumns":["receiptNo","status","amount","discountAmount","redFlushReason","createdAt"],"showTotals":true,"columnWidths":{"receiptNo":14,"status":10,"amount":10,"discountAmount":10,"redFlushReason":16,"createdAt":18}}',
        70
    ),
    (
        'AP_DEFAULT',
        '应付单默认模板',
        'ACCOUNTS_PAYABLE',
        '应付单',
        'Accounts Payable',
        '请核对应付与核销记录',
        '{"headerFields":["payableNo","orderNo","supplierName","totalAmount","paidAmount","discountAmount","unpaidAmount","status","printCount","lastPrintedAt","remark"],"detailColumns":["paymentNo","status","amount","discountAmount","redFlushReason","createdAt"],"showTotals":true,"columnWidths":{"paymentNo":14,"status":10,"amount":10,"discountAmount":10,"redFlushReason":16,"createdAt":18}}',
        80
    ),
    (
        'STOCK_COUNT_DEFAULT',
        '库存盘点单默认模板',
        'STOCK_COUNT',
        '库存盘点单',
        'Stock Count',
        '请核对盘点差异后签字',
        '{"headerFields":["countNo","countAt","status","printCount","lastPrintedAt","remark"],"detailColumns":["productCode","productName","warehouse","location","systemQty","countedQty","diffQty","remark"],"showTotals":false,"columnWidths":{"productCode":10,"productName":18,"warehouse":12,"location":12,"systemQty":8,"countedQty":8,"diffQty":8,"remark":16}}',
        90
    ),
    (
        'STOCK_INIT_DEFAULT',
        '初始库存单默认模板',
        'STOCK_INIT',
        '初始库存单',
        'Initial Stock',
        '请核对期初库存后签字',
        '{"headerFields":["stockInitNo","countAt","status","printCount","lastPrintedAt","remark"],"detailColumns":["productCode","productName","warehouse","location","systemQty","countedQty","diffQty","remark"],"showTotals":false,"columnWidths":{"productCode":10,"productName":18,"warehouse":12,"location":12,"systemQty":8,"countedQty":8,"diffQty":8,"remark":16}}',
        100
    )
) AS v(code, name, doc_type, header_title, sub_title, footer_note, field_config, sort_no);

UPDATE erp_print_template p
SET name = v.name,
    doc_type = v.doc_type,
    header_title = v.header_title,
    sub_title = v.sub_title,
    footer_note = v.footer_note,
    field_config = v.field_config,
    sort_no = v.sort_no,
    is_enabled = TRUE,
    remark = '系统默认模板',
    updated_at = NOW()
FROM app_tenant t
JOIN tmp_erp_print_template_defaults v ON TRUE
WHERE p.tenant_id = t.id
  AND p.code = v.code
  AND p.deleted_at IS NULL
  AND t.deleted_at IS NULL;

INSERT INTO erp_print_template (
    tenant_id,
    code,
    name,
    doc_type,
    header_title,
    sub_title,
    footer_note,
    field_config,
    sort_no,
    is_default,
    is_enabled,
    remark,
    created_at,
    updated_at
)
SELECT t.id,
       v.code,
       v.name,
       v.doc_type,
       v.header_title,
       v.sub_title,
       v.footer_note,
       v.field_config,
       v.sort_no,
       NOT EXISTS (
           SELECT 1
           FROM erp_print_template existing
           WHERE existing.tenant_id = t.id
             AND existing.doc_type = v.doc_type
             AND existing.is_default = TRUE
             AND existing.deleted_at IS NULL
       ),
       TRUE,
       '系统默认模板',
       NOW(),
       NOW()
FROM app_tenant t
CROSS JOIN tmp_erp_print_template_defaults v
WHERE t.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM erp_print_template existing
      WHERE existing.tenant_id = t.id
        AND existing.code = v.code
        AND existing.deleted_at IS NULL
  );
