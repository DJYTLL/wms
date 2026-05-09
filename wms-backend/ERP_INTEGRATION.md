# 进销存开发指南（基于当前 WMS Admin）

你的目标是“完成进销存开发”，因此本文档改为：先做出**最小闭环**，再扩展 ERP 集成。

核心原则：所有库存变化必须通过“流水表”落地，当前库存由流水汇总或增量维护。

---

## 1. 最小闭环范围（建议第一期就做这些）

只要完成下面 4 块，就能跑通进销存：

1) 基础资料
- 商品（product）
- 供应商（supplier）
- 客户（customer）
- 仓库/库位（warehouse/location）

2) 采购入库（审核即入库）
- 采购草稿单 -> 审核通过 -> 直接增加库存（不走入库单）

3) 销售出库（审核即出库）
- 销售草稿单 -> 审核通过 -> 直接减少库存（不走出库单）

4) 库存台账
- 库存流水（必须有）
- 当前库存（必须有）

---

## 2. 强约束（必须遵守）

1) 多租户
- 业务表必须带 `tenant_id`
- 任何查询/写入都不能跨租户

2) 审计日志
- 单据创建/审核/反审核/作废/库存变化都要写审计日志
- action 建议：
  - `PURCHASE_CREATE` / `PURCHASE_APPROVE` / `PURCHASE_UNAPPROVE`
  - `SALE_CREATE` / `SALE_APPROVE` / `SALE_UNAPPROVE`
  - `STOCK_ADJUST`

3) 幂等
- 所有“入库/出库执行”接口必须防重复提交
- 外部单号或请求幂等键必须唯一

---

## 3. 建议表结构（Flyway 新增，最关键）

建议至少新增这 4 类表：

### 3.1 单据头（采购/销售）
- `erp_purchase_order`
- `erp_sale_order`

建议字段（按可直接落地的版本给出）：
- `id` / `tenant_id`
- `order_no`
- `status`（DRAFT/APPROVED/CANCELLED）
- `supplier_id` 或 `customer_id`
- `warehouse_id` / `location_id`（改为放在明细，支持一单多仓）
- `total_amount`（总金额，避免每次汇总明细）
- `version`（乐观锁版本号，避免并发审核问题）
- `approved_by` / `approved_at`
- `unapproved_by` / `unapproved_at`
- `cancelled_by` / `cancelled_at`
- `remark`
- `created_at` / `updated_at`

### 3.2 单据明细
- `erp_purchase_order_item`
- `erp_sale_order_item`

建议字段：
- `id` / `tenant_id`
- `order_id`
- `product_id`
- `product_code` / `product_name`（快照字段，避免主数据改名影响历史单据）
- `warehouse_id` / `location_id`
- `qty`
- `price`
- `amount`
- `sort_no`（行号/排序）
- `remark`
- `created_at` / `updated_at`

### 3.3 库存流水（必须有）
- `erp_stock_txn`

建议字段：
- `id` / `tenant_id`
- `txn_no`（流水号/幂等号）
- `biz_type`（PURCHASE_APPROVE_IN / SALE_APPROVE_OUT / APPROVE_UNDO / ADJUST）
- `biz_id` / `biz_item_id`
- `product_id`
- `warehouse_id` / `location_id`
- `qty_delta`（正负）
- `qty_before` / `qty_after`（强烈建议保留，排查极其方便）
- `operator` / `operator_id`
- `remark`
- `created_at`

强烈建议唯一约束：
- `(tenant_id, txn_no)` 唯一

### 3.4 当前库存（建议有）
- `erp_stock_balance`

建议字段：
- `id` / `tenant_id`
- `product_id`
- `warehouse_id` / `location_id`
- `qty_on_hand`
- `updated_by`
- `updated_at`

强烈建议唯一约束：
- `(tenant_id, product_id, warehouse_id, location_id)` 唯一

---

### 3.5 基础信息表（必须补齐）

建议新增：

- `erp_product`（商品）
- `erp_customer`（客户）
- `erp_supplier`（供应商）
- `erp_warehouse`（仓库）
- `erp_location`（库位）
- `erp_category`（分类）
- `erp_unit`（单位）

字段建议见 V5 迁移。

## 4. 业务落地顺序（按这个做最稳）

建议严格按顺序推进：

1) 先上表（Flyway）
- 先把单据 + 流水 + 当前库存建好

2) 先打通“采购审核即入库”
- 创建采购草稿单（DRAFT）
- 审核通过（APPROVED）-> 写库存流水 -> 更新当前库存

3) 再打通“销售审核即出库”
- 创建销售草稿单（DRAFT）
- 审核通过（APPROVED）-> 先校验库存充足 -> 写库存流水 -> 更新当前库存

4) 最后做库存查询
- 库存台账（按流水查）
- 当前库存（按 balance 查）

---

## 5. 审核即入/出库的状态机与库存规则（关键）

这是你当前想要的模式：没有出入库单，审核动作直接影响库存。

### 5.1 状态机（建议固定下来）

采购单 / 销售单统一：

- `DRAFT`：草稿（不影响库存）
- `APPROVED`：已审核（已影响库存）
- `CANCELLED`：已作废（不再允许审核）

建议额外规则：

- 只允许 `DRAFT -> APPROVED`
- 只允许 `DRAFT -> CANCELLED`
- 是否允许反审核：
  - 若允许：`APPROVED -> DRAFT` 时必须写“反向库存流水”

### 5.2 库存变更规则（必须写成硬规则）

1) 审核采购单（入库）
- 触发点：`purchase.status = APPROVED`
- 动作：
  - 为每条明细写一条 `erp_stock_txn`
  - `qty_delta = +qty`
  - 更新 `erp_stock_balance.qty_on_hand += qty`

2) 审核销售单（出库）
- 触发点：`sale.status = APPROVED`
- 动作：
  - 先校验库存是否充足（按 tenant + 仓库/库位 + 商品）
  - 为每条明细写一条 `erp_stock_txn`
  - `qty_delta = -qty`
  - 更新 `erp_stock_balance.qty_on_hand -= qty`

3) 反审核（如果你需要）
- 采购反审核：写负向流水（`qty_delta = -qty`）
- 销售反审核：写正向流水（`qty_delta = +qty`）
- 反审核必须记录审计日志 + 操作人 + 时间

---

## 6. 接口与状态流转（建议直接按这个实现）

下面是一套和当前系统风格一致、且能直接指导开发的接口草案。

### 6.1 采购单接口（审核即入库）

基础路径：`/api/purchase-orders`

建议接口：

1) 新增草稿单
- `POST /api/purchase-orders`
- 行为：创建 `DRAFT`，不影响库存

2) 分页查询
- `GET /api/purchase-orders/page`

3) 详情
- `GET /api/purchase-orders/{id}`

4) 编辑草稿
- `PUT /api/purchase-orders/{id}`
- 规则：仅 `DRAFT` 可编辑

5) 作废
- `POST /api/purchase-orders/{id}/cancel`
- 规则：仅 `DRAFT` 可作废

6) 审核（关键）
- `POST /api/purchase-orders/{id}/approve`
- 行为：
  - 校验状态为 `DRAFT`
  - 生成库存流水（每条明细一条）
  - 更新当前库存
  - 单据状态改为 `APPROVED`

7) 反审核（可选）
- `POST /api/purchase-orders/{id}/unapprove`
- 行为：
  - 校验状态为 `APPROVED`
  - 生成反向流水
  - 回滚库存
  - 单据状态改为 `DRAFT`

---

### 6.2 销售单接口（审核即出库）

基础路径：`/api/sale-orders`

建议接口与采购单对称：

- `POST /api/sale-orders`
- `GET /api/sale-orders/page`
- `GET /api/sale-orders/{id}`
- `PUT /api/sale-orders/{id}`（仅 DRAFT）
- `POST /api/sale-orders/{id}/cancel`（仅 DRAFT）
- `POST /api/sale-orders/{id}/approve`（关键）
- `POST /api/sale-orders/{id}/unapprove`（可选）

销售审核的额外硬规则：

- 审核前必须校验库存充足
- 校验维度至少包含：
  - `tenant_id + product_id + warehouse_id + location_id`

---

### 6.3 库存查询接口（台账 + 当前库存）

基础路径：`/api/stocks`

建议接口：

1) 当前库存分页
- `GET /api/stocks/balances/page`

2) 库存流水分页（台账）
- `GET /api/stocks/txns/page`

3) 单商品库存
- `GET /api/stocks/balances/{productId}`

---

## 7. 权限点与审计点（必须一起落地）

建议在 `PermissionSeedProvider` 中补齐以下权限点：

采购单：
- `purchase:view`
- `purchase:add`
- `purchase:edit`
- `purchase:cancel`
- `purchase:approve`
- `purchase:unapprove`

销售单：
- `sale:view`
- `sale:add`
- `sale:edit`
- `sale:cancel`
- `sale:approve`
- `sale:unapprove`

库存：
- `stock:view`
- `stock:txn:view`

审计日志 action 建议固定为：

- 采购：
  - `PURCHASE_CREATE`
  - `PURCHASE_UPDATE`
  - `PURCHASE_CANCEL`
  - `PURCHASE_APPROVE`
  - `PURCHASE_UNAPPROVE`
- 销售：
  - `SALE_CREATE`
  - `SALE_UPDATE`
  - `SALE_CANCEL`
  - `SALE_APPROVE`
  - `SALE_UNAPPROVE`
- 库存：
  - `STOCK_TXN_CREATE`

---

## 8. 审核实现要点（避免以后踩坑）

请把“审核逻辑”当成一个事务性动作来实现：

1) 必须使用事务
- 一个审核动作应包含：
  - 状态校验
  - 流水写入
  - 库存更新
  - 单据状态更新

2) 并发控制（很重要）
- 推荐组合：
  - 单据头 `version` 乐观锁
  - 库存更新用“条件更新”防止扣成负数

3) 库存扣减建议用条件更新
- 典型写法（思想层面）：
  - `UPDATE balance SET qty = qty - ? WHERE qty >= ?`
- 如果影响行数为 0，说明库存不足

4) 流水号必须幂等
- `txn_no` 建议包含：
  - 业务类型 + 单据ID + 明细ID + 动作（APPROVE/UNAPPROVE）

---

## 9. 和 ERP 集成的正确姿势（放在第二期）

等最小闭环稳定后，再接 ERP：

1) ERP -> 本系统：商品/供应商/客户主数据同步
2) 本系统 -> ERP：采购入库/销售出库结果回传

仍然建议通过“集成层 + 映射表 + 同步任务表”，不要 ERP 直连业务表。

---

## 10. 开发落地清单（按这个顺序做）

这是面向“直接开工”的 checklist：

1) Flyway 迁移
- 新增：
  - 采购单头/明细
  - 销售单头/明细
  - 库存流水
  - 当前库存
- 加唯一约束与关键索引

2) Mapper 层
- 采购/销售/库存各自 mapper
- 库存 mapper 至少提供：
  - 查询当前库存
  - 条件扣减
  - 增量增加

3) Service 层（关键）
- 审核与反审核必须收敛在 service
- 审核方法必须：
  - 事务
  - 状态校验
  - 库存校验/更新
  - 流水写入
  - 审计日志

4) Controller 层
- 只做参数校验与调用 service
- 不直接操作库存

5) 权限种子与菜单
- 补权限
- 补菜单（采购管理/销售管理/库存台账）

6) API 文档
- 在 `API.md` 增加：
  - 采购单接口
  - 销售单接口
  - 库存查询接口
  - 状态流转与错误码说明

---

## 11. 下一步我可以直接落地的内容

如果你回复“继续（先做审核即入/出库表结构）”，我会直接：

1) 新增 Flyway：
- `V5__biz_inventory_core.sql`（采购/销售 + 审核字段 + 流水 + 库存）

2) 同步补文档：
- 更新 `API.md` 的进销存章节（接口草案 + 状态机 + 错误码）
