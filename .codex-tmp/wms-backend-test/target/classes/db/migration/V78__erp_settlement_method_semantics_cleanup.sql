-- 统一结算方式语义，剥离被误当作结算方式的资金渠道

-- 历史默认值纠偏：保留原编码，修正显示名称为账务规则
UPDATE erp_settlement_method
SET name = '现结',
    remark = COALESCE(NULLIF(remark, ''), '统一模型迁移：即时结清')
WHERE deleted_at IS NULL
  AND code = 'CASH'
  AND name IN ('现金', '现结');

UPDATE erp_settlement_method
SET name = '月结',
    remark = COALESCE(NULLIF(remark, ''), '统一模型迁移：按月对账结算')
WHERE deleted_at IS NULL
  AND code = 'TRANSFER'
  AND name IN ('银行转账', '月结');

UPDATE erp_settlement_method
SET name = '挂账',
    remark = COALESCE(NULLIF(remark, ''), '统一模型迁移：形成应收/应付后续结清')
WHERE deleted_at IS NULL
  AND code = 'CREDIT'
  AND name IN ('赊账', '挂账');

-- 渠道型结算方式停用，避免继续被选作账务规则
UPDATE erp_settlement_method
SET is_enabled = FALSE,
    remark = COALESCE(NULLIF(remark, ''), '统一模型迁移：该记录属于资金渠道，请改用收款方式/付款方式')
WHERE deleted_at IS NULL
  AND code IN ('ALIPAY', 'WECHAT')
  AND name IN ('支付宝', '微信支付');
