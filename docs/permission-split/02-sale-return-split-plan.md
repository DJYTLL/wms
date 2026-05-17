# 销售退货单草稿/已审核拆分计划

## 目标

将销售退货单拆成“销售退货草稿”和“销售退货已审核”两套独立权限、菜单、列表页、表单页、打印页、打印模板和后端接口。

拆分后需要满足：

- 销售退货草稿权限只能进入草稿工作区，不能进入已审核工作区。
- 销售退货已审核权限只能进入已审核工作区，不能新增或编辑草稿。
- 草稿和已审核都支持打印，并使用各自独立的打印模板。
- 后端接口按状态隔离，草稿接口不能读取已审核退货单，已审核接口不能读取草稿退货单。
- 按销售单退货或选择来源销售单时，必须有销售单已审核查看权限；没有权限时不能请求来源销售单接口，避免连续 403。
- 旧 `erp-sale-return:*` 权限保留用于迁移和兼容，但不再作为新页面的主权限。

## 权限设计

新增销售退货草稿权限：

```text
erp-sale-return-draft:view
erp-sale-return-draft:add
erp-sale-return-draft:edit
erp-sale-return-draft:delete
erp-sale-return-draft:approve
erp-sale-return-draft:print
```

新增销售退货已审核权限：

```text
erp-sale-return-approved:view
erp-sale-return-approved:copy
erp-sale-return-approved:cancel
erp-sale-return-approved:redflush
erp-sale-return-approved:print
```

旧权限保留但隐藏：

```text
erp-sale-return:view
erp-sale-return:add
erp-sale-return:edit
erp-sale-return:cancel
erp-sale-return:approve
erp-sale-return:redflush
```

旧权限迁移关系：

```text
erp-sale-return:view     -> erp-sale-return-draft:view + erp-sale-return-approved:view + erp-sale-return-draft:print + erp-sale-return-approved:print
erp-sale-return:add      -> erp-sale-return-draft:add
erp-sale-return:edit     -> erp-sale-return-draft:edit + erp-sale-return-draft:delete
erp-sale-return:approve  -> erp-sale-return-draft:approve
erp-sale-return:cancel   -> erp-sale-return-approved:cancel
erp-sale-return:redflush -> erp-sale-return-approved:redflush
```

复制权限不从旧权限自动推导，除非业务明确希望老角色默认拥有复制能力。默认建议由管理员单独分配 `erp-sale-return-approved:copy`。

来源销售单权限：

```text
erp-sale-approved:view
```

## 前端拆分

新增或拆出 Vue 文件：

```text
ErpSaleReturnDraft.vue
ErpSaleReturnApproved.vue
ErpSaleReturnDraftForm.vue
ErpSaleReturnApprovedForm.vue
ErpSaleReturnDraftPrint.vue
ErpSaleReturnApprovedPrint.vue
```

允许抽公共逻辑，但页面入口必须独立：

```text
useSaleReturnForm.ts
SaleReturnItemsTable.vue
SaleReturnAmountPanel.vue
SaleReturnPrintRenderer.vue
```

`ErpSaleReturnDraft.vue` 职责：

```text
固定查询 DRAFT
显示新增、编辑、删除、审核、打印
不显示复制、红冲、作废
所有按钮使用 erp-sale-return-draft:* 权限判断
```

`ErpSaleReturnApproved.vue` 职责：

```text
查询 APPROVED / CANCELLED / RED_FLUSHED 等非 DRAFT 状态
显示查看、复制、作废、红冲、打印
不显示新增、编辑、删除、审核
所有按钮使用 erp-sale-return-approved:* 权限判断
```

`ErpSaleReturnDraftForm.vue` 职责：

```text
新增销售退货草稿
编辑销售退货草稿
按商品退货
按销售单退货
保存草稿
审核草稿
打印草稿
审核成功后跳转销售退货已审核详情
```

`ErpSaleReturnApprovedForm.vue` 职责：

```text
只读查看
复制为新草稿
作废
红冲
打印已审核销售退货单
不允许保存编辑
```

来源销售单处理：

```text
有 erp-sale-approved:view 时，允许按销售单退货和选择来源销售单
没有 erp-sale-approved:view 时，隐藏按销售单退货入口和来源单选择按钮
没有 erp-sale-approved:view 时，不请求销售单列表、销售单详情、销售单 recent-items 接口
已有关联来源单的只读销售退货，可通过销售退货自己的汇总接口显示来源销售单号
```

复制已审核退货单为草稿时，必须同时具备：

```text
erp-sale-return-approved:copy
erp-sale-return-draft:add
```

## 路由设计

新路由：

```text
/erp/sale-returns
  redirect -> /erp/sale-returns/draft

/erp/sale-returns/draft
  component -> ErpSaleReturnDraft.vue
  permission -> erp-sale-return-draft:view

/erp/sale-returns/draft/create
  component -> ErpSaleReturnDraftForm.vue
  permission -> erp-sale-return-draft:add

/erp/sale-returns/draft/:id/edit
  component -> ErpSaleReturnDraftForm.vue
  permission -> erp-sale-return-draft:edit

/erp/sale-returns/draft/:id/print
  component -> ErpSaleReturnDraftPrint.vue
  permission -> erp-sale-return-draft:print

/erp/sale-returns/approved
  component -> ErpSaleReturnApproved.vue
  permission -> erp-sale-return-approved:view

/erp/sale-returns/approved/:id
  component -> ErpSaleReturnApprovedForm.vue
  permission -> erp-sale-return-approved:view

/erp/sale-returns/approved/:id/print
  component -> ErpSaleReturnApprovedPrint.vue
  permission -> erp-sale-return-approved:print
```

旧路由兼容：

```text
/erp/sale-returns/create -> /erp/sale-returns/draft/create
/erp/sale-returns/:id/edit -> /erp/sale-returns/draft/:id/edit
/erp/sale-returns/:id/print -> 根据单据状态跳到 draft 或 approved 打印页
```

## 后端接口设计

新增草稿接口：

```text
GET    /api/erp/sale-returns/draft/page
GET    /api/erp/sale-returns/draft/summary
GET    /api/erp/sale-returns/draft/{id}
GET    /api/erp/sale-returns/draft/{id}/print
GET    /api/erp/sale-returns/draft/next-no
POST   /api/erp/sale-returns/draft
PUT    /api/erp/sale-returns/draft/{id}
DELETE /api/erp/sale-returns/draft/{id}
POST   /api/erp/sale-returns/draft/{id}/approve
```

新增已审核接口：

```text
GET  /api/erp/sale-returns/approved/page
GET  /api/erp/sale-returns/approved/summary
GET  /api/erp/sale-returns/approved/{id}
GET  /api/erp/sale-returns/approved/{id}/print
POST /api/erp/sale-returns/approved/{id}/copy
POST /api/erp/sale-returns/approved/{id}/cancel
POST /api/erp/sale-returns/approved/{id}/red-flush
```

权限绑定：

```text
draft/page, draft/summary, draft/{id} -> erp-sale-return-draft:view
draft/{id}/print                      -> erp-sale-return-draft:print
draft/next-no, POST draft             -> erp-sale-return-draft:add
PUT draft/{id}                        -> erp-sale-return-draft:edit
DELETE draft/{id}                     -> erp-sale-return-draft:delete
draft/{id}/approve                    -> erp-sale-return-draft:approve

approved/page, approved/summary, approved/{id} -> erp-sale-return-approved:view
approved/{id}/print                            -> erp-sale-return-approved:print
approved/{id}/copy                             -> erp-sale-return-approved:copy + erp-sale-return-draft:add
approved/{id}/cancel                           -> erp-sale-return-approved:cancel
approved/{id}/red-flush                        -> erp-sale-return-approved:redflush
```

状态约束：

```text
draft/* 只能访问 DRAFT
approved/* 只能访问非 DRAFT
draft approve 只能处理 DRAFT
approved cancel/red-flush 只能处理允许操作的已审核状态
copy 从 approved 生成新的 DRAFT
```

来源销售单接口权限：

```text
销售退货选择来源销售单时，调用销售单 approved/recent 接口
接口权限使用 erp-sale-approved:view
销售退货自身 refund-summary 接口使用 erp-sale-return-draft:view 或 erp-sale-return-approved:view
```

旧接口短期保留：

```text
/api/erp/sale-returns/page
/api/erp/sale-returns/{id}
/api/erp/sale-returns
/api/erp/sale-returns/{id}/approve
/api/erp/sale-returns/{id}/cancel
/api/erp/sale-returns/{id}/red-flush
```

新销售退货页面不再调用旧接口。旧接口保留用于历史链接、外部调用和灰度兼容。

## 打印模板设计

新增打印模板单据类型：

```text
SALE_RETURN_DRAFT
SALE_RETURN_APPROVED
```

模板名称建议：

```text
销售退货草稿打印模板
销售退货已审核打印模板
```

模板使用规则：

```text
草稿打印只读取 SALE_RETURN_DRAFT
已审核打印只读取 SALE_RETURN_APPROVED
如果没有对应模板，提示“未配置销售退货草稿打印模板”或“未配置销售退货已审核打印模板”
不再混用旧 SALE_RETURN 模板
```

打印日志：

```text
草稿打印日志要求 erp-sale-return-draft:print
已审核打印日志要求 erp-sale-return-approved:print
日志 docType 分别记录 SALE_RETURN_DRAFT / SALE_RETURN_APPROVED
```

## 菜单、角色权限页、迁移

菜单绑定：

```text
erp-sale-return-draft -> erp-sale-return-draft:view
erp-sale-return-approved -> erp-sale-return-approved:view
```

角色权限页：

```text
销售退货草稿页面展示 erp-sale-return-draft:*
销售退货已审核页面展示 erp-sale-return-approved:*
隐藏旧 erp-sale-return:*
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
只有销售退货草稿权限不能进入销售退货已审核
只有销售退货已审核权限不能进入销售退货草稿
销售退货草稿可打印且使用草稿模板
销售退货已审核可打印且使用已审核模板
没有 erp-sale-approved:view 时，不请求来源销售单接口
草稿接口不能读取已审核退货单
已审核接口不能读取草稿退货单
复制已审核退货单必须同时具备 erp-sale-return-approved:copy 和 erp-sale-return-draft:add
旧角色迁移后不丢失原有销售退货访问能力
```
