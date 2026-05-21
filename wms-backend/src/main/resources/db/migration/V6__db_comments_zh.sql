-- Add Chinese comments for all existing tables/columns.

-- app_tenant
COMMENT ON TABLE app_tenant IS '租户表';
COMMENT ON COLUMN app_tenant.id IS '主键';
COMMENT ON COLUMN app_tenant.code IS '租户编码';
COMMENT ON COLUMN app_tenant.name IS '租户名称';
COMMENT ON COLUMN app_tenant.is_enabled IS '是否启用';
COMMENT ON COLUMN app_tenant.created_at IS '创建时间';
COMMENT ON COLUMN app_tenant.updated_at IS '更新时间';
COMMENT ON COLUMN app_tenant.deleted_at IS '删除时间';

-- app_user
COMMENT ON TABLE app_user IS '用户表';
COMMENT ON COLUMN app_user.id IS '主键';
COMMENT ON COLUMN app_user.tenant_id IS '租户ID';
COMMENT ON COLUMN app_user.username IS '用户名';
COMMENT ON COLUMN app_user.password_hash IS '密码哈希';
COMMENT ON COLUMN app_user.display_name IS '显示名';
COMMENT ON COLUMN app_user.email IS '邮箱';
COMMENT ON COLUMN app_user.phone IS '手机号';
COMMENT ON COLUMN app_user.avatar_url IS '头像URL';
COMMENT ON COLUMN app_user.is_enabled IS '是否启用';
COMMENT ON COLUMN app_user.account_non_expired IS '账号是否过期';
COMMENT ON COLUMN app_user.account_non_locked IS '账号是否锁定';
COMMENT ON COLUMN app_user.credentials_non_expired IS '凭证是否过期';
COMMENT ON COLUMN app_user.auth_version IS '权限版本';
COMMENT ON COLUMN app_user.last_login_at IS '最近登录时间';
COMMENT ON COLUMN app_user.created_at IS '创建时间';
COMMENT ON COLUMN app_user.updated_at IS '更新时间';
COMMENT ON COLUMN app_user.deleted_at IS '删除时间';
COMMENT ON COLUMN app_user.remark IS '备注';

-- app_role
COMMENT ON TABLE app_role IS '角色表';
COMMENT ON COLUMN app_role.id IS '主键';
COMMENT ON COLUMN app_role.tenant_id IS '租户ID';
COMMENT ON COLUMN app_role.code IS '角色编码';
COMMENT ON COLUMN app_role.name IS '角色名称';
COMMENT ON COLUMN app_role.description IS '角色描述';
COMMENT ON COLUMN app_role.is_enabled IS '是否启用';
COMMENT ON COLUMN app_role.created_at IS '创建时间';
COMMENT ON COLUMN app_role.updated_at IS '更新时间';

-- app_permission
COMMENT ON TABLE app_permission IS '权限表';
COMMENT ON COLUMN app_permission.id IS '主键';
COMMENT ON COLUMN app_permission.code IS '权限编码';
COMMENT ON COLUMN app_permission.name IS '权限名称';
COMMENT ON COLUMN app_permission.description IS '权限描述';
COMMENT ON COLUMN app_permission.is_enabled IS '是否启用';
COMMENT ON COLUMN app_permission.created_at IS '创建时间';
COMMENT ON COLUMN app_permission.updated_at IS '更新时间';

-- app_user_role
COMMENT ON TABLE app_user_role IS '用户角色关联表';
COMMENT ON COLUMN app_user_role.tenant_id IS '租户ID';
COMMENT ON COLUMN app_user_role.user_id IS '用户ID';
COMMENT ON COLUMN app_user_role.role_id IS '角色ID';
COMMENT ON COLUMN app_user_role.created_at IS '创建时间';

-- app_role_permission
COMMENT ON TABLE app_role_permission IS '角色权限关联表';
COMMENT ON COLUMN app_role_permission.tenant_id IS '租户ID';
COMMENT ON COLUMN app_role_permission.role_id IS '角色ID';
COMMENT ON COLUMN app_role_permission.permission_id IS '权限ID';
COMMENT ON COLUMN app_role_permission.created_at IS '创建时间';

-- app_refresh_token
COMMENT ON TABLE app_refresh_token IS '刷新令牌表';
COMMENT ON COLUMN app_refresh_token.id IS '主键';
COMMENT ON COLUMN app_refresh_token.tenant_id IS '租户ID';
COMMENT ON COLUMN app_refresh_token.user_id IS '用户ID';
COMMENT ON COLUMN app_refresh_token.audience_tenant_id IS '目标租户ID';
COMMENT ON COLUMN app_refresh_token.token_hash IS '令牌哈希';
COMMENT ON COLUMN app_refresh_token.expires_at IS '过期时间';
COMMENT ON COLUMN app_refresh_token.revoked_at IS '撤销时间';
COMMENT ON COLUMN app_refresh_token.created_at IS '创建时间';
COMMENT ON COLUMN app_refresh_token.updated_at IS '更新时间';

-- app_audit_log
COMMENT ON TABLE app_audit_log IS '审计日志表';
COMMENT ON COLUMN app_audit_log.id IS '主键';
COMMENT ON COLUMN app_audit_log.tenant_id IS '租户ID';
COMMENT ON COLUMN app_audit_log.actor_username IS '操作者用户名';
COMMENT ON COLUMN app_audit_log.action IS '动作标识';
COMMENT ON COLUMN app_audit_log.entity_type IS '实体类型';
COMMENT ON COLUMN app_audit_log.entity_id IS '实体ID';
COMMENT ON COLUMN app_audit_log.detail IS '详情';
COMMENT ON COLUMN app_audit_log.status IS '结果状态';
COMMENT ON COLUMN app_audit_log.request_id IS '请求ID';
COMMENT ON COLUMN app_audit_log.client_ip IS '客户端IP';
COMMENT ON COLUMN app_audit_log.user_agent IS '客户端UA';
COMMENT ON COLUMN app_audit_log.method IS '请求方法';
COMMENT ON COLUMN app_audit_log.path IS '请求路径';
COMMENT ON COLUMN app_audit_log.http_status IS 'HTTP状态';
COMMENT ON COLUMN app_audit_log.error_code IS '错误码';
COMMENT ON COLUMN app_audit_log.error_message IS '错误信息';
COMMENT ON COLUMN app_audit_log.duration_ms IS '耗时(毫秒)';
COMMENT ON COLUMN app_audit_log.created_at IS '创建时间';

-- app_system_config
COMMENT ON TABLE app_system_config IS '系统配置表（租户隔离）';
COMMENT ON COLUMN app_system_config.id IS '主键';
COMMENT ON COLUMN app_system_config.config_key IS '配置键';
COMMENT ON COLUMN app_system_config.config_value IS '配置值';
COMMENT ON COLUMN app_system_config.value_type IS '值类型';
COMMENT ON COLUMN app_system_config.description IS '描述';
COMMENT ON COLUMN app_system_config.is_public IS '是否公开';
COMMENT ON COLUMN app_system_config.created_at IS '创建时间';
COMMENT ON COLUMN app_system_config.updated_at IS '更新时间';

-- app_idempotency
COMMENT ON TABLE app_idempotency IS '幂等记录表';
COMMENT ON COLUMN app_idempotency.idempotency_key IS '幂等键';
COMMENT ON COLUMN app_idempotency.method IS '请求方法';
COMMENT ON COLUMN app_idempotency.path IS '请求路径';
COMMENT ON COLUMN app_idempotency.tenant_id IS '租户ID';
COMMENT ON COLUMN app_idempotency.username IS '用户名';
COMMENT ON COLUMN app_idempotency.created_at IS '创建时间';
COMMENT ON COLUMN app_idempotency.expires_at IS '过期时间';

-- app_menu
COMMENT ON TABLE app_menu IS '菜单表';
COMMENT ON COLUMN app_menu.id IS '主键';
COMMENT ON COLUMN app_menu.code IS '菜单编码';
COMMENT ON COLUMN app_menu.parent_id IS '父级菜单ID';
COMMENT ON COLUMN app_menu.title IS '菜单标题';
COMMENT ON COLUMN app_menu.i18n_key IS '国际化键';
COMMENT ON COLUMN app_menu.path IS '路由路径';
COMMENT ON COLUMN app_menu.icon IS '图标';
COMMENT ON COLUMN app_menu.permission_code IS '权限编码';
COMMENT ON COLUMN app_menu.sort IS '排序';
COMMENT ON COLUMN app_menu.is_enabled IS '是否启用';
COMMENT ON COLUMN app_menu.created_at IS '创建时间';
COMMENT ON COLUMN app_menu.updated_at IS '更新时间';

-- app_tenant_menu
COMMENT ON TABLE app_tenant_menu IS '租户菜单映射表';
COMMENT ON COLUMN app_tenant_menu.tenant_id IS '租户ID';
COMMENT ON COLUMN app_tenant_menu.menu_id IS '菜单ID';
COMMENT ON COLUMN app_tenant_menu.is_enabled IS '是否启用';
COMMENT ON COLUMN app_tenant_menu.created_at IS '创建时间';
COMMENT ON COLUMN app_tenant_menu.updated_at IS '更新时间';

-- app_tenant_column_setting
COMMENT ON TABLE app_tenant_column_setting IS '租户列配置表';
COMMENT ON COLUMN app_tenant_column_setting.tenant_id IS '租户ID';
COMMENT ON COLUMN app_tenant_column_setting.page_key IS '页面标识';
COMMENT ON COLUMN app_tenant_column_setting.visible_columns IS '可见列列表';
COMMENT ON COLUMN app_tenant_column_setting.updated_by IS '更新人';
COMMENT ON COLUMN app_tenant_column_setting.updated_at IS '更新时间';

-- erp_purchase_order
COMMENT ON TABLE erp_purchase_order IS '采购单头表（ERP进销存）';
COMMENT ON COLUMN erp_purchase_order.id IS '主键';
COMMENT ON COLUMN erp_purchase_order.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_purchase_order.order_no IS '单号';
COMMENT ON COLUMN erp_purchase_order.status IS '状态';
COMMENT ON COLUMN erp_purchase_order.supplier_id IS '供应商ID';
COMMENT ON COLUMN erp_purchase_order.total_amount IS '总金额';
COMMENT ON COLUMN erp_purchase_order.total_amount_excl_tax IS '未税总金额';
COMMENT ON COLUMN erp_purchase_order.total_tax_amount IS '税额合计';
COMMENT ON COLUMN erp_purchase_order.total_amount_incl_tax IS '含税总金额';
COMMENT ON COLUMN erp_purchase_order.version IS '乐观锁版本';
COMMENT ON COLUMN erp_purchase_order.approved_by IS '审核人';
COMMENT ON COLUMN erp_purchase_order.approved_at IS '审核时间';
COMMENT ON COLUMN erp_purchase_order.unapproved_by IS '反审核人';
COMMENT ON COLUMN erp_purchase_order.unapproved_at IS '反审核时间';
COMMENT ON COLUMN erp_purchase_order.cancelled_by IS '作废人';
COMMENT ON COLUMN erp_purchase_order.cancelled_at IS '作废时间';
COMMENT ON COLUMN erp_purchase_order.remark IS '备注';
COMMENT ON COLUMN erp_purchase_order.created_at IS '创建时间';
COMMENT ON COLUMN erp_purchase_order.updated_at IS '更新时间';

-- erp_purchase_order_item
COMMENT ON TABLE erp_purchase_order_item IS '采购单明细表（ERP进销存）';
COMMENT ON COLUMN erp_purchase_order_item.id IS '主键';
COMMENT ON COLUMN erp_purchase_order_item.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_purchase_order_item.order_id IS '采购单ID';
COMMENT ON COLUMN erp_purchase_order_item.product_id IS '商品ID';
COMMENT ON COLUMN erp_purchase_order_item.product_code IS '商品编码快照';
COMMENT ON COLUMN erp_purchase_order_item.product_name IS '商品名称快照';
COMMENT ON COLUMN erp_purchase_order_item.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_purchase_order_item.location_id IS '库位ID';
COMMENT ON COLUMN erp_purchase_order_item.qty IS '数量';
COMMENT ON COLUMN erp_purchase_order_item.price IS '单价';
COMMENT ON COLUMN erp_purchase_order_item.price_incl_tax IS '含税单价';
COMMENT ON COLUMN erp_purchase_order_item.amount IS '金额';
COMMENT ON COLUMN erp_purchase_order_item.amount_incl_tax IS '含税金额';
COMMENT ON COLUMN erp_purchase_order_item.tax_rate IS '税率';
COMMENT ON COLUMN erp_purchase_order_item.tax_amount IS '税额';
COMMENT ON COLUMN erp_purchase_order_item.sort_no IS '排序';
COMMENT ON COLUMN erp_purchase_order_item.remark IS '备注';
COMMENT ON COLUMN erp_purchase_order_item.created_at IS '创建时间';
COMMENT ON COLUMN erp_purchase_order_item.updated_at IS '更新时间';

-- erp_sale_order
COMMENT ON TABLE erp_sale_order IS '销售单头表（ERP进销存）';
COMMENT ON COLUMN erp_sale_order.id IS '主键';
COMMENT ON COLUMN erp_sale_order.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_sale_order.order_no IS '单号';
COMMENT ON COLUMN erp_sale_order.status IS '状态';
COMMENT ON COLUMN erp_sale_order.customer_id IS '客户ID';
COMMENT ON COLUMN erp_sale_order.total_amount IS '总金额';
COMMENT ON COLUMN erp_sale_order.total_amount_excl_tax IS '未税总金额';
COMMENT ON COLUMN erp_sale_order.total_tax_amount IS '税额合计';
COMMENT ON COLUMN erp_sale_order.total_amount_incl_tax IS '含税总金额';
COMMENT ON COLUMN erp_sale_order.version IS '乐观锁版本';
COMMENT ON COLUMN erp_sale_order.approved_by IS '审核人';
COMMENT ON COLUMN erp_sale_order.approved_at IS '审核时间';
COMMENT ON COLUMN erp_sale_order.unapproved_by IS '反审核人';
COMMENT ON COLUMN erp_sale_order.unapproved_at IS '反审核时间';
COMMENT ON COLUMN erp_sale_order.cancelled_by IS '作废人';
COMMENT ON COLUMN erp_sale_order.cancelled_at IS '作废时间';
COMMENT ON COLUMN erp_sale_order.remark IS '备注';
COMMENT ON COLUMN erp_sale_order.created_at IS '创建时间';
COMMENT ON COLUMN erp_sale_order.updated_at IS '更新时间';

-- erp_sale_order_item
COMMENT ON TABLE erp_sale_order_item IS '销售单明细表（ERP进销存）';
COMMENT ON COLUMN erp_sale_order_item.id IS '主键';
COMMENT ON COLUMN erp_sale_order_item.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_sale_order_item.order_id IS '销售单ID';
COMMENT ON COLUMN erp_sale_order_item.product_id IS '商品ID';
COMMENT ON COLUMN erp_sale_order_item.product_code IS '商品编码快照';
COMMENT ON COLUMN erp_sale_order_item.product_name IS '商品名称快照';
COMMENT ON COLUMN erp_sale_order_item.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_sale_order_item.location_id IS '库位ID';
COMMENT ON COLUMN erp_sale_order_item.qty IS '数量';
COMMENT ON COLUMN erp_sale_order_item.price IS '单价';
COMMENT ON COLUMN erp_sale_order_item.price_incl_tax IS '含税单价';
COMMENT ON COLUMN erp_sale_order_item.amount IS '金额';
COMMENT ON COLUMN erp_sale_order_item.amount_incl_tax IS '含税金额';
COMMENT ON COLUMN erp_sale_order_item.tax_rate IS '税率';
COMMENT ON COLUMN erp_sale_order_item.tax_amount IS '税额';
COMMENT ON COLUMN erp_sale_order_item.sort_no IS '排序';
COMMENT ON COLUMN erp_sale_order_item.remark IS '备注';
COMMENT ON COLUMN erp_sale_order_item.created_at IS '创建时间';
COMMENT ON COLUMN erp_sale_order_item.updated_at IS '更新时间';

-- erp_stock_txn
COMMENT ON TABLE erp_stock_txn IS '库存流水表（ERP进销存）';
COMMENT ON COLUMN erp_stock_txn.id IS '主键';
COMMENT ON COLUMN erp_stock_txn.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_stock_txn.txn_no IS '流水号';
COMMENT ON COLUMN erp_stock_txn.biz_type IS '业务类型';
COMMENT ON COLUMN erp_stock_txn.biz_id IS '业务单据ID';
COMMENT ON COLUMN erp_stock_txn.biz_item_id IS '业务明细ID';
COMMENT ON COLUMN erp_stock_txn.product_id IS '商品ID';
COMMENT ON COLUMN erp_stock_txn.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_stock_txn.location_id IS '库位ID';
COMMENT ON COLUMN erp_stock_txn.qty_delta IS '变更数量';
COMMENT ON COLUMN erp_stock_txn.qty_before IS '变更前数量';
COMMENT ON COLUMN erp_stock_txn.qty_after IS '变更后数量';
COMMENT ON COLUMN erp_stock_txn.operator IS '操作者';
COMMENT ON COLUMN erp_stock_txn.operator_id IS '操作者ID';
COMMENT ON COLUMN erp_stock_txn.remark IS '备注';
COMMENT ON COLUMN erp_stock_txn.created_at IS '创建时间';

-- erp_stock_balance
COMMENT ON TABLE erp_stock_balance IS '当前库存表（ERP进销存）';
COMMENT ON COLUMN erp_stock_balance.id IS '主键';
COMMENT ON COLUMN erp_stock_balance.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_stock_balance.product_id IS '商品ID';
COMMENT ON COLUMN erp_stock_balance.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_stock_balance.location_id IS '库位ID';
COMMENT ON COLUMN erp_stock_balance.qty_on_hand IS '当前库存';
COMMENT ON COLUMN erp_stock_balance.updated_by IS '更新人';
COMMENT ON COLUMN erp_stock_balance.updated_at IS '更新时间';

-- erp_product
COMMENT ON TABLE erp_product IS '商品表（ERP进销存）';
COMMENT ON COLUMN erp_product.id IS '主键';
COMMENT ON COLUMN erp_product.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_product.code IS '商品编码';
COMMENT ON COLUMN erp_product.name IS '商品名称';
COMMENT ON COLUMN erp_product.short_name IS '商品简称';
COMMENT ON COLUMN erp_product.spec IS '规格型号';
COMMENT ON COLUMN erp_product.model IS '型号';
COMMENT ON COLUMN erp_product.category_id IS '分类ID';
COMMENT ON COLUMN erp_product.unit_id IS '单位ID';
COMMENT ON COLUMN erp_product.barcode IS '条码';
COMMENT ON COLUMN erp_product.sku IS 'SKU';
COMMENT ON COLUMN erp_product.brand IS '品牌';
COMMENT ON COLUMN erp_product.origin IS '产地';
COMMENT ON COLUMN erp_product.weight IS '重量';
COMMENT ON COLUMN erp_product.volume IS '体积';
COMMENT ON COLUMN erp_product.cost_price IS '成本价';
COMMENT ON COLUMN erp_product.sale_price IS '销售价';
COMMENT ON COLUMN erp_product.tax_rate IS '默认税率';
COMMENT ON COLUMN erp_product.safety_stock IS '安全库存';
COMMENT ON COLUMN erp_product.is_batch IS '是否批次管理';
COMMENT ON COLUMN erp_product.shelf_life_days IS '保质期(天)';
COMMENT ON COLUMN erp_product.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_product.ext_attrs IS '扩展属性(JSON)';
COMMENT ON COLUMN erp_product.remark IS '备注';
COMMENT ON COLUMN erp_product.created_at IS '创建时间';
COMMENT ON COLUMN erp_product.updated_at IS '更新时间';

-- erp_customer
COMMENT ON TABLE erp_customer IS '客户表（ERP进销存）';
COMMENT ON COLUMN erp_customer.id IS '主键';
COMMENT ON COLUMN erp_customer.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_customer.code IS '客户编码';
COMMENT ON COLUMN erp_customer.name IS '客户名称';
COMMENT ON COLUMN erp_customer.short_name IS '客户简称';
COMMENT ON COLUMN erp_customer.contact IS '联系人';
COMMENT ON COLUMN erp_customer.phone IS '联系电话';
COMMENT ON COLUMN erp_customer.mobile IS '联系手机';
COMMENT ON COLUMN erp_customer.email IS '邮箱';
COMMENT ON COLUMN erp_customer.address IS '地址';
COMMENT ON COLUMN erp_customer.tax_no IS '税号';
COMMENT ON COLUMN erp_customer.bank_name IS '开户行';
COMMENT ON COLUMN erp_customer.bank_account IS '银行账号';
COMMENT ON COLUMN erp_customer.invoice_title IS '发票抬头';
COMMENT ON COLUMN erp_customer.payment_terms IS '结算方式';
COMMENT ON COLUMN erp_customer.credit_limit IS '授信额度';
COMMENT ON COLUMN erp_customer.contacts IS '联系人列表(JSON)';
COMMENT ON COLUMN erp_customer.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_customer.remark IS '备注';
COMMENT ON COLUMN erp_customer.created_at IS '创建时间';
COMMENT ON COLUMN erp_customer.updated_at IS '更新时间';

-- erp_supplier
COMMENT ON TABLE erp_supplier IS '供应商表（ERP进销存）';
COMMENT ON COLUMN erp_supplier.id IS '主键';
COMMENT ON COLUMN erp_supplier.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_supplier.code IS '供应商编码';
COMMENT ON COLUMN erp_supplier.name IS '供应商名称';
COMMENT ON COLUMN erp_supplier.short_name IS '供应商简称';
COMMENT ON COLUMN erp_supplier.contact IS '联系人';
COMMENT ON COLUMN erp_supplier.phone IS '联系电话';
COMMENT ON COLUMN erp_supplier.mobile IS '联系手机';
COMMENT ON COLUMN erp_supplier.email IS '邮箱';
COMMENT ON COLUMN erp_supplier.address IS '地址';
COMMENT ON COLUMN erp_supplier.tax_no IS '税号';
COMMENT ON COLUMN erp_supplier.bank_name IS '开户行';
COMMENT ON COLUMN erp_supplier.bank_account IS '银行账号';
COMMENT ON COLUMN erp_supplier.payment_terms IS '结算方式';
COMMENT ON COLUMN erp_supplier.contacts IS '联系人列表(JSON)';
COMMENT ON COLUMN erp_supplier.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_supplier.remark IS '备注';
COMMENT ON COLUMN erp_supplier.created_at IS '创建时间';
COMMENT ON COLUMN erp_supplier.updated_at IS '更新时间';

-- erp_warehouse
COMMENT ON TABLE erp_warehouse IS '仓库表（ERP进销存）';
COMMENT ON COLUMN erp_warehouse.id IS '主键';
COMMENT ON COLUMN erp_warehouse.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_warehouse.code IS '仓库编码';
COMMENT ON COLUMN erp_warehouse.name IS '仓库名称';
COMMENT ON COLUMN erp_warehouse.address IS '地址';
COMMENT ON COLUMN erp_warehouse.manager IS '负责人';
COMMENT ON COLUMN erp_warehouse.phone IS '联系电话';
COMMENT ON COLUMN erp_warehouse.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_warehouse.remark IS '备注';
COMMENT ON COLUMN erp_warehouse.created_at IS '创建时间';
COMMENT ON COLUMN erp_warehouse.updated_at IS '更新时间';

-- erp_location
COMMENT ON TABLE erp_location IS '库位表（ERP进销存）';
COMMENT ON COLUMN erp_location.id IS '主键';
COMMENT ON COLUMN erp_location.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_location.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_location.code IS '库位编码';
COMMENT ON COLUMN erp_location.name IS '库位名称';
COMMENT ON COLUMN erp_location.aisle IS '巷道';
COMMENT ON COLUMN erp_location.rack IS '货架';
COMMENT ON COLUMN erp_location.bin IS '货位';
COMMENT ON COLUMN erp_location.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_location.remark IS '备注';
COMMENT ON COLUMN erp_location.created_at IS '创建时间';
COMMENT ON COLUMN erp_location.updated_at IS '更新时间';

-- erp_category
COMMENT ON TABLE erp_category IS '商品分类表（ERP进销存）';
COMMENT ON COLUMN erp_category.id IS '主键';
COMMENT ON COLUMN erp_category.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_category.code IS '分类编码';
COMMENT ON COLUMN erp_category.name IS '分类名称';
COMMENT ON COLUMN erp_category.parent_id IS '父级分类ID';
COMMENT ON COLUMN erp_category.level IS '层级';
COMMENT ON COLUMN erp_category.sort_no IS '排序';
COMMENT ON COLUMN erp_category.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_category.remark IS '备注';
COMMENT ON COLUMN erp_category.created_at IS '创建时间';
COMMENT ON COLUMN erp_category.updated_at IS '更新时间';

-- erp_unit
COMMENT ON TABLE erp_unit IS '计量单位表（ERP进销存）';
COMMENT ON COLUMN erp_unit.id IS '主键';
COMMENT ON COLUMN erp_unit.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_unit.code IS '单位编码';
COMMENT ON COLUMN erp_unit.name IS '单位名称';
COMMENT ON COLUMN erp_unit.symbol IS '单位符号';
COMMENT ON COLUMN erp_unit.precision IS '小数精度';
COMMENT ON COLUMN erp_unit.is_enabled IS '是否启用';
COMMENT ON COLUMN erp_unit.remark IS '备注';
COMMENT ON COLUMN erp_unit.created_at IS '创建时间';
COMMENT ON COLUMN erp_unit.updated_at IS '更新时间';
