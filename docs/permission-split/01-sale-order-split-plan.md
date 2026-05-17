# 销售单草稿/已审核拆分计划

## 目标

将销售单拆成“销售单草稿”和“销售单已审核”两套独立权限、菜单、列表页、表单页、打印页、打印模板和后端接口。

拆分后需要满足：

- 销售草稿权限只能进入草稿工作区，不能进入已审核工作区。
- 销售已审核权限只能进入已审核工作区，不能新增或编辑草稿。
- 草稿和已审核都支持打印，并使用各自独立的打印模板。
- 后端接口按状态隔离，草稿接口不能读取已审核单，已审核接口不能读取草稿单。
- 旧 `erp-sale:*` 权限保留用于迁移和兼容，但不再作为新页面的主权限。

## 权限设计

新增销售单草稿权限：

```text
erp-sale-draft:view
erp-sale-draft:add
erp-sale-draft:edit
erp-sale-draft:delete
erp-sale-draft:approve
erp-sale-draft:print
```

新增销售单已审核权限：

```text
erp-sale-approved:view
erp-sale-approved:copy
erp-sale-approved:cancel
erp-sale-approved:redflush
erp-sale-approved:print
```

旧权限保留但隐藏：

```text
erp-sale:view
erp-sale:add
erp-sale:edit
erp-sale:cancel
erp-sale:approve
erp-sale:redflush
```

旧权限迁移关系：

```text
erp-sale:view     -> erp-sale-draft:view + erp-sale-approved:view + erp-sale-draft:print + erp-sale-approved:print
erp-sale:add      -> erp-sale-draft:add
erp-sale:edit     -> erp-sale-draft:edit + erp-sale-draft:delete
erp-sale:approve  -> erp-sale-draft:approve
erp-sale:cancel   -> erp-sale-approved:cancel
erp-sale:redflush -> erp-sale-approved:redflush
```

复制权限不从旧权限自动推导，除非业务明确希望老角色默认拥有复制能力。默认建议由管理员单独分配 `erp-sale-approved:copy`。

## 前端拆分

新增或拆出 Vue 文件：

```text
ErpSaleOrderDraft.vue
ErpSaleOrderApproved.vue
ErpSaleOrderDraftForm.vue
ErpSaleOrderApprovedForm.vue
ErpSaleOrderDraftPrint.vue
ErpSaleOrderApprovedPrint.vue
```

允许抽公共逻辑，但页面入口必须独立：

```text
useSaleOrderForm.ts
SaleOrderItemsTable.vue
SaleOrderAmountPanel.vue
SaleOrderPrintRenderer.vue
```

`ErpSaleOrderDraft.vue` 职责：

```text
固定查询 DRAFT
显示新增、编辑、删除、审核、打印
不显示复制、红冲、作废
所有按钮使用 erp-sale-draft:* 权限判断
```

`ErpSaleOrderApproved.vue` 职责：

```text
查询 APPROVED / CANCELLED / RED_FLUSHED 等非 DRAFT 状态
显示查看、复制、作废、红冲、打印
不显示新增、编辑、删除、审核
所有按钮使用 erp-sale-approved:* 权限判断
```

`ErpSaleOrderDraftForm.vue` 职责：

```text
新增草稿
编辑草稿
保存草稿
审核草稿
打印草稿
审核成功后跳转销售单已审核详情
```

`ErpSaleOrderApprovedForm.vue` 职责：

```text
只读查看
复制为新草稿
作废
红冲
打印已审核单
不允许保存编辑
```

复制已审核单为草稿时，必须同时具备：

```text
erp-sale-approved:copy
erp-sale-draft:add
```

缺少 `erp-sale-draft:add` 时，不跳转草稿新增页，并提示缺少销售草稿新增权限。

## 路由设计

新路由：

```text
/erp/sale-orders
  redirect -> /erp/sale-orders/draft

/erp/sale-orders/draft
  component -> ErpSaleOrderDraft.vue
  permission -> erp-sale-draft:view

/erp/sale-orders/draft/create
  component -> ErpSaleOrderDraftForm.vue
  permission -> erp-sale-draft:add

/erp/sale-orders/draft/:id/edit
  component -> ErpSaleOrderDraftForm.vue
  permission -> erp-sale-draft:edit

/erp/sale-orders/draft/:id/print
  component -> ErpSaleOrderDraftPrint.vue
  permission -> erp-sale-draft:print

/erp/sale-orders/approved
  component -> ErpSaleOrderApproved.vue
  permission -> erp-sale-approved:view

/erp/sale-orders/approved/:id
  component -> ErpSaleOrderApprovedForm.vue
  permission -> erp-sale-approved:view

/erp/sale-orders/approved/:id/print
  component -> ErpSaleOrderApprovedPrint.vue
  permission -> erp-sale-approved:print
```

旧路由兼容：

```text
/erp/sale-orders/create -> /erp/sale-orders/draft/create
/erp/sale-orders/:id/edit -> /erp/sale-orders/draft/:id/edit
/erp/sale-orders/:id/print -> 根据单据状态跳到 draft 或 approved 打印页
```

## 后端接口设计

新增草稿接口：

```text
GET    /api/erp/sale-orders/draft/page
GET    /api/erp/sale-orders/draft/summary
GET    /api/erp/sale-orders/draft/{id}
GET    /api/erp/sale-orders/draft/{id}/print
GET    /api/erp/sale-orders/draft/next-order-no
POST   /api/erp/sale-orders/draft
PUT    /api/erp/sale-orders/draft/{id}
DELETE /api/erp/sale-orders/draft/{id}
POST   /api/erp/sale-orders/draft/{id}/approve
```

新增已审核接口：

```text
GET  /api/erp/sale-orders/approved/page
GET  /api/erp/sale-orders/approved/summary
GET  /api/erp/sale-orders/approved/{id}
GET  /api/erp/sale-orders/approved/{id}/print
POST /api/erp/sale-orders/approved/{id}/copy
POST /api/erp/sale-orders/approved/{id}/cancel
POST /api/erp/sale-orders/approved/{id}/red-flush
```

权限绑定：

```text
draft/page, draft/summary, draft/{id} -> erp-sale-draft:view
draft/{id}/print                      -> erp-sale-draft:print
draft/next-order-no, POST draft       -> erp-sale-draft:add
PUT draft/{id}                        -> erp-sale-draft:edit
DELETE draft/{id}                     -> erp-sale-draft:delete
draft/{id}/approve                    -> erp-sale-draft:approve

approved/page, approved/summary, approved/{id} -> erp-sale-approved:view
approved/{id}/print                            -> erp-sale-approved:print
approved/{id}/copy                             -> erp-sale-approved:copy + erp-sale-draft:add
approved/{id}/cancel                           -> erp-sale-approved:cancel
approved/{id}/red-flush                        -> erp-sale-approved:redflush
```

状态约束：

```text
draft/* 只能访问 DRAFT
approved/* 只能访问非 DRAFT
draft approve 只能处理 DRAFT
approved cancel/red-flush 只能处理允许操作的已审核状态
copy 从 approved 生成新的 DRAFT
```

旧接口短期保留：

```text
/api/erp/sale-orders/page
/api/erp/sale-orders/{id}
/api/erp/sale-orders
/api/erp/sale-orders/{id}/approve
/api/erp/sale-orders/{id}/cancel
/api/erp/sale-orders/{id}/red-flush
```

新销售页面不再调用旧接口。旧接口保留用于历史链接、外部调用和灰度兼容。

## 打印模板设计

新增打印模板单据类型：

```text
SALE_ORDER_DRAFT
SALE_ORDER_APPROVED
```

模板名称建议：

```text
销售单草稿打印模板
销售单已审核打印模板
```

模板使用规则：

```text
草稿打印只读取 SALE_ORDER_DRAFT
已审核打印只读取 SALE_ORDER_APPROVED
如果没有对应模板，提示“未配置销售单草稿打印模板”或“未配置销售单已审核打印模板”
不再混用旧 SALE_ORDER 模板
```

打印日志：

```text
草稿打印日志要求 erp-sale-draft:print
已审核打印日志要求 erp-sale-approved:print
日志 docType 分别记录 SALE_ORDER_DRAFT / SALE_ORDER_APPROVED
```

## 菜单、角色权限页、迁移

菜单绑定：

```text
erp-sale-draft -> erp-sale-draft:view
erp-sale-approved -> erp-sale-approved:view
```

角色权限页：

```text
销售单草稿页面展示 erp-sale-draft:*
销售单已审核页面展示 erp-sale-approved:*
隐藏旧 erp-sale:*
```

数据库迁移：

```text
插入新权限
插入新打印模板 docType 配置
更新菜单 permission_code
迁移旧角色权限到新权限
旧权限保留但 UI 隐藏
```

迁移要求幂等：

```text
权限不存在才插入
角色权限不存在才插入
菜单存在才更新
打印模板类型不存在才插入
```

## 验收标准

```text
只有销售草稿权限不能进入销售已审核
只有销售已审核权限不能进入销售草稿
销售草稿可打印且使用草稿模板
销售已审核可打印且使用已审核模板
草稿接口不能读取已审核单
已审核接口不能读取草稿单
复制已审核单必须同时具备 erp-sale-approved:copy 和 erp-sale-draft:add
旧角色迁移后不丢失原有销售单访问能力
```
