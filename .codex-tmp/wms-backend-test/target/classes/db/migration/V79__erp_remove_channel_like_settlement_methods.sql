-- 从结算方式主数据中移除被误建成资金渠道的记录

UPDATE erp_settlement_method
SET deleted_at = NOW(),
    deleted_by = 'system',
    delete_reason = '统一模型迁移：支付宝/微信支付属于收款方式或付款方式，不属于结算方式'
WHERE deleted_at IS NULL
  AND code IN ('ALIPAY', 'WECHAT')
  AND name IN ('支付宝', '微信支付');
