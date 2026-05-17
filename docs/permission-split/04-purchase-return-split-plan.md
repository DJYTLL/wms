# 采购退货单草稿/已审核拆分计划

## 目标

将采购退货单拆成“采购退货草稿”和“采购退货已审核”两套独立权限、菜单、列表页、表单页、打印页、打印模板和后端接口。

拆分后需要满足：

- 采购退货草稿权限只能进入草稿工作区，不能进入已审核工作区。
- 采购退货已审核权限只能进入已审核工作区，不能新增或编辑草稿。
- 草稿和已审核都支持打印，并使用各自独立的打印模板。
- 后端接口按状态隔离，草稿接口不能读取已审核采购退货单，已审核接口不能读取草稿采购退货单。
- 按采购单退货或选择来源采购单时，必须有采购单已审核查看权限；没有权限时不能请求来源采购单接口，避免连续 403。
- 旧 `erp-purchase-return:*` 权限保留用于迁移和兼容，但不再作为新页面的主权限。

## 权限设计

新增采购退货草稿权限：

```text
erp-purchase-return-draft:view
erp-purchase-return-draft:add
erp-purchase-return-draft:edit
erp-purchase-return-draft:delete
erp-purchase-return-draft:approve
erp-purchase-return-draft:print
```

新增采购退货已审核权限：

```text
erp-purchase-return-approved:view
erp-purchase-return-approved:copy
erp-purchase-return-approved:cancel
erp-purchase-return-approved:print
```

旧权限保留但隐藏：

```text
erp-purchase-return:view
erp-purchase-return:add
erp-purchase-return:edit
erp-purchase-return:approve
erp-purchase-return:cancel
```

旧权限迁移关系：

```text
erp-purchase-return:view    -> erp-purchase-return-draft:view + erp-purchase-return-approved:view + erp-purchase-return-draft:print + erp-purchase-return-approved:print
erp-purchase-return:add     -> erp-purchase-return-draft:add
erp-purchase-return:edit    -> erp-purchase-return-draft:edit + erp-purchase-return-draft:delete
erp-purchase-return:approve -> erp-purchase-return-draft:approve
erp-purchase-return:cancel  -> erp-purchase-return-approved:cancel
```

如果旧系统没有采购退货 `cancel` 权限但接口支持作废，需要同步补齐：

```text
erp-purchase-return:cancel
erp-purchase-return-approved:cancel
```

复制权限不从旧权限自动推导，除非业务明确希望老角色默认拥有复制能力。默认建议由管理员单独分配 `erp-purchase-return-approved:copy`。

来源采购单权限：

```text
erp-purchase-approved:view
```

## 前端拆分

新增或拆出 Vue 文件：

```text
ErpPurchaseReturnDraft.vue
ErpPurchaseReturnApproved.vue
ErpPurchaseReturnDraftForm.vue
ErpPurchaseReturnApprovedForm.vue
ErpPurchaseReturnDraftPrint.vue
ErpPurchaseReturnApprovedPrint.vue
```

允许抽公共逻辑，但页面入口必须独立：

```text
usePurchaseReturnForm.ts
PurchaseReturnItemsTable.vue
PurchaseReturnAmountPanel.vue
PurchaseReturnPrintRenderer.vue
```

`ErpPurchaseReturnDraft.vue` 职责：

```text
固定查询 DRAFT
显示新增、编辑、删除、审核、打印
不显示复制、作废
所有按钮使用 erp-purchase-return-draft:* 权限判断
```

`ErpPurchaseReturnApproved.vue` 职责：

```text
查询 APPROVED / CANCELLED 等非 DRAFT 状态
显示查看、复制、作废、打印
不显示新增、编辑、删除、审核
所有按钮使用 erp-purchase-return-approved:* 权限判断
```

`ErpPurchaseReturnDraftForm.vue` 职责：

```text
新增采购退货草稿
编辑采购退货草稿
按商品退货
按采购单退货
保存草稿
审核草稿
打印草稿
审核成功后跳转采购退货已审核详情
```

`ErpPurchaseReturnApprovedForm.vue` 职责：

```text
只读查看
复制为新草稿
作废
打印已审核采购退货单
不允许保存编辑
```

来源采购单处理：

```text
有 erp-purchase-approved:view 时，允许按采购单退货和选择来源采购单
没有 erp-purchase-approved:view 时，隐藏按采购单退货入口和来源单选择按钮
没有 erp-purchase-approved:view 时，不请求采购单列表、采购单详情、采购单 recent-items 接口
已有关联来源单的只读采购退货，可通过采购退货自己的汇总接口显示来源采购单号
```

复制已审核采购退货单为草稿时，必须同时具备：

```text
erp-purchase-return-approved:copy
erp-purchase-return-draft:add
```

## 路由设计

新路由：

```text
/erp/purchase-returns
  redirect -> /erp/purchase-returns/draft

/erp/purchase-returns/draft
  component -> ErpPurchaseReturnDraft.vue
  permission -> erp-purchase-return-draft:view

/erp/purchase-returns/draft/create
  component -> ErpPurchaseReturnDraftForm.vue
  permission -> erp-purchase-return-draft:add

/erp/purchase-returns/draft/:id/edit
  component -> ErpPurchaseReturnDraftForm.vue
  permission -> erp-purchase-return-draft:edit

/erp/purchase-returns/draft/:id/print
  component -> ErpPurchaseReturnDraftPrint.vue
  permission -> erp-purchase-return-draft:print

/erp/purchase-returns/approved
  component -> ErpPurchaseReturnApproved.vue
  permission -> erp-purchase-return-approved:view

/erp/purchase-returns/approved/:id
  component -> ErpPurchaseReturnApprovedForm.vue
  permission -> erp-purchase-return-approved:view

/erp/purchase-returns/approved/:id/print
  component -> ErpPurchaseReturnApprovedPrint.vue
  permission -> erp-purchase-return-approved:print
```

旧路由兼容：

```text
/erp/purchase-returns/create -> /erp/purchase-returns/draft/create
/erp/purchase-returns/:id/edit -> /erp/purchase-returns/draft/:id/edit
/erp/purchase-returns/:id/print -> 根据单据状态跳到 draft 或 approved 打印页
```

## 后端接口设计

新增草稿接口：

```text
GET    /api/erp/purchase-returns/draft/page
GET    /api/erp/purchase-returns/draft/summary
GET    /api/erp/purchase-returns/draft/{id}
GET    /api/erp/purchase-returns/draft/{id}/print
GET    /api/erp/purchase-returns/draft/next-no
POST   /api/erp/purchase-returns/draft
PUT    /api/erp/purchase-returns/draft/{id}
DELETE /api/erp/purchase-returns/draft/{id}
POST   /api/erp/purchase-returns/draft/{id}/approve
```

新增已审核接口：

```text
GET  /api/erp/purchase-returns/approved/page
GET  /api/erp/purchase-returns/approved/summary
GET  /api/erp/purchase-returns/approved/{id}
GET  /api/erp/purchase-returns/approved/{id}/print
POST /api/erp/purchase-returns/approved/{id}/copy
POST /api/erp/purchase-returns/approved/{id}/cancel
```

权限绑定：

```text
draft/page, draft/summary, draft/{id} -> erp-purchase-return-draft:view
draft/{id}/print                      -> erp-purchase-return-draft:print
draft/next-no, POST draft             -> erp-purchase-return-draft:add
PUT draft/{id}                        -> erp-purchase-return-draft:edit
DELETE draft/{id}                     -> erp-purchase-return-draft:delete
draft/{id}/approve                    -> erp-purchase-return-draft:approve

approved/page, approved/summary, approved/{id} -> erp-purchase-return-approved:view
approved/{id}/print                            -> erp-purchase-return-approved:print
approved/{id}/copy                             -> erp-purchase-return-approved:copy + erp-purchase-return-draft:add
approved/{id}/cancel                           -> erp-purchase-return-approved:cancel
```

状态约束：

```text
draft/* 只能访问 DRAFT
approved/* 只能访问非 DRAFT
draft approve 只能处理 DRAFT
approved cancel 只能处理允许操作的已审核状态
copy 从 approved 生成新的 DRAFT
```

来源采购单接口权限：

```text
采购退货选择来源采购单时，调用采购单 approved/recent 接口
接口权限使用 erp-purchase-approved:view
采购退货自身 refund-summary 接口使用 erp-purchase-return-draft:view 或 erp-purchase-return-approved:view
```

旧接口短期保留：

```text
/api/erp/purchase-returns/page
/api/erp/purchase-returns/{id}
/api/erp/purchase-returns
/api/erp/purchase-returns/{id}/approve
/api/erp/purchase-returns/{id}/cancel
```

新采购退货页面不再调用旧接口。旧接口保留用于历史链接、外部调用和灰度兼容。

## 打印模板设计

新增打印模板单据类型：

```text
PURCHASE_RETURN_DRAFT
PURCHASE_RETURN_APPROVED
```

模板名称建议：

```text
采购退货草稿打印模板
采购退货已审核打印模板
```

模板使用规则：

```text
草稿打印只读取 PURCHASE_RETURN_DRAFT
已审核打印只读取 PURCHASE_RETURN_APPROVED
如果没有对应模板，提示“未配置采购退货草稿打印模板”或“未配置采购退货已审核打印模板”
不再混用旧 PURCHASE_RETURN 模板
```

打印日志：

```text
草稿打印日志要求 erp-purchase-return-draft:print
已审核打印日志要求 erp-purchase-return-approved:print
日志 docType 分别记录 PURCHASE_RETURN_DRAFT / PURCHASE_RETURN_APPROVED
```

## 菜单、角色权限页、迁移

菜单绑定：

```text
erp-purchase-return-draft -> erp-purchase-return-draft:view
erp-purchase-return-approved -> erp-purchase-return-approved:view
```

角色权限页：

```text
采购退货草稿页面展示 erp-purchase-return-draft:*
采购退货已审核页面展示 erp-purchase-return-approved:*
隐藏旧 erp-purchase-return:*
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
只有采购退货草稿权限不能进入采购退货已审核
只有采购退货已审核权限不能进入采购退货草稿
采购退货草稿可打印且使用草稿模板
采购退货已审核可打印且使用已审核模板
没有 erp-purchase-approved:view 时，不请求来源采购单接口
草稿接口不能读取已审核采购退货单
已审核接口不能读取草稿采购退货单
复制已审核采购退货单必须同时具备 erp-purchase-return-approved:copy 和 erp-purchase-return-draft:add
旧角色迁移后不丢失原有采购退货访问能力
```
