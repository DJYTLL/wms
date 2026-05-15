# ERP 单据页面交互与视觉规范

基准页面：
- 列表页：`src/views/erp/ErpSaleOrderManagement.vue`
- 单据页：`src/views/erp/ErpSaleOrderForm.vue`
- 打印弹窗：`src/components/PrintPreviewDialog.vue`
- 标签页容器：`src/layouts/MainLayout.vue`

适用范围：
- 销售单草稿列表、销售单已审核列表
- 新增销售单、编辑销售单、销售单查看页
- 后续采购单、销售退货单、采购退货单等 ERP 单据模块

目标：
- 同类单据页面视觉、按钮语义、跳转行为保持一致
- 打开页面时按钮、图标、操作区不闪现、不跳动
- 保存、审核、打印、返回、标签关闭行为可预期
- 列表和明细表格的密度、对齐、操作方式保持统一

## 1. 页面范围与路由约定

销售单当前页面分为 5 类：

| 页面 | 路由 | 说明 | 标签页标题建议 |
| --- | --- | --- | --- |
| 销售单草稿列表 | `/erp/sale-orders/draft` | 只看草稿，支持新增、编辑、审核入口 | 销售单（草稿） |
| 销售单已审核列表 | `/erp/sale-orders/approved` | 只看已审核，支持查看、打印、红冲入口 | 销售单（已审核） |
| 新增销售单 | `/erp/sale-orders/create?returnTo=/erp/sale-orders/draft&from=draft` | 创建草稿单据 | 新增销售单 |
| 编辑销售单 | `/erp/sale-orders/:id/edit?returnTo=/erp/sale-orders/draft&from=draft` | 编辑草稿单据 | 编辑销售单 |
| 销售单查看 | `/erp/sale-orders/:id/edit?mode=view&returnTo=/erp/sale-orders/approved&from=approved` | 查看已审核单据，只读 | 销售单 |

规则：
- 从草稿列表进入新增/编辑时，必须带 `returnTo=/erp/sale-orders/draft&from=draft`。
- 从已审核列表进入查看时，必须带 `returnTo=/erp/sale-orders/approved&from=approved`。
- 如果旧链接缺少 `returnTo`，详情页应按状态兜底：
  - `from=draft` 回 `/erp/sale-orders/draft`
  - `from=approved`、`mode=view` 或 `status=APPROVED` 回 `/erp/sale-orders/approved`
  - 其他情况才回模块默认列表

迁移到采购单、销售退货单时：
- 不要只写 `mode=view`。
- 查看入口必须携带来源列表，否则返回和关闭标签会落到错误页面。

## 2. 页面结构

单据页采用 B 类单据表单页结构：

```vue
<div class="page-shell page-shell--system sale-page-surface">
  <div class="page-header sale-page-header">
    <div class="sale-title-group">
      <div class="page-title">新增销售单</div>
      <div class="sale-breadcrumb">...</div>
    </div>
    <div class="table-actions sale-page-toolbar__actions">...</div>
  </div>

  <div class="page-toolbar-card sale-header-card">基础信息</div>
  <div class="table-card sale-detail-card">单据明细</div>
  <div class="table-card payment-card">结算信息</div>
</div>
```

约束：
- `page-header` 只承载标题、面包屑、全局操作按钮。
- 基础信息、明细信息、结算信息必须作为独立卡片放在 header 外。
- 不要把列表页的筛选卡片结构套到单据表单页上。
- 不要额外包一层无意义 card，避免页面显得厚重且产生滚动抖动。
- 采购退货单、销售退货单的页头必须共用同一套结构：
  - 左侧为 `page-title + sale-breadcrumb`
  - 右侧为全局操作按钮区
  - “基础信息”标题条必须位于独立卡片内部，不能塞回 header 区域

### 2.1 页面背景色

单据表单页整体内容区背景统一使用白色：

```css
#ffffff
```

实现要求：
- 新增、编辑、审核后查看页都必须让 `MainLayout` 的 `content-area` 使用该背景色。
- 销售、销售退货表单页使用 `sale-page-surface` 时：

```css
:global(.content-area:has(.sale-page-surface)) {
  background: #ffffff;
}
```

- 采购单表单页使用 `purchase-page-surface` 时：

```css
:global(.content-area:has(.purchase-page-surface)) {
  background: #ffffff;
}
```

- 采购退货等仍使用旧 `sale-theme` 根类的表单页，也必须同步：

```css
:global(.content-area:has(.sale-theme)) {
  background: #ffffff;
}
```

迁移要求：
- 不要只给根容器设置透明背景，必须同步设置外层 `content-area`，否则侧边栏右侧页面底色可能继承为非白色。
- 单据页根容器可以继续 `background: transparent`，由 `content-area` 承载统一底色。
- 纸质主题等特殊模式可以在根容器内部覆盖局部视觉，但默认表单页底色仍以白色为基准。

## 3. 顶部按钮设计

### 3.1 按钮语义层级

| 操作 | 使用场景 | 视觉风格 | 类名建议 |
| --- | --- | --- | --- |
| 返回 | 所有单据页 | 中性白底、深色文字 | `action-button` |
| 保存 | 新增/编辑草稿 | 轻量蓝色次主按钮 | `action-button action-button--save` |
| 审核 | 新增/编辑草稿 | 浅绿色业务流转按钮 | `action-button action-button--success` |
| 复制 | 已审核/已红冲查看页 | 轻量蓝色次操作 | `action-button action-button--secondary` |
| 打印 | 查看页主操作 | 主蓝色按钮 | `action-button action-button--primary` |
| 红冲 | 已审核查看页风险操作 | 浅红风险按钮 | `action-button action-button--danger` |

设计原则：
- 同一组按钮里只能有一个强主按钮。查看页中“打印”是主按钮。
- “复制”和“保存”都是次主操作，使用轻量蓝色，不要和打印一样做成大蓝块。
- “审核”属于业务状态流转，使用绿色，不和保存抢同一种蓝色。
- “红冲”属于风险操作，使用浅红，必须覆盖 Element Plus `plain/disabled` 默认白底。

### 3.2 新增/编辑页按钮

顺序：

```text
返回 / 保存 / 审核
```

行为：
- 返回：关闭当前标签，回 `returnTo`。
- 保存：保存后弹“保存成功”后续操作弹窗。
- 审核：只弹一次审核确认，确认后内部先保存当前修改，再调用审核接口。

### 3.3 查看页按钮

顺序：

```text
返回 / 复制 / 打印 / 红冲
```

行为：
- 返回：关闭当前标签，优先回 `returnTo`，已审核查看页回已审核列表。
- 复制：复制为新草稿。
- 打印：打开打印预览弹窗，不新建浏览器窗口，不跳转当前页面。
- 红冲：风险操作，必须二次确认并填写原因。

## 3.4 结算信息显示规则

- 结算信息卡片继续沿用统一三列栅格，不要退回老式的横向紧凑条状布局。
- 当结算方式为“挂账”类方式时，`付款金额` 输入框不展示；仅展示结算方式和优惠金额。
- 当单据填写了 `优惠金额` 时，必须按商品行金额占比将优惠金额分摊到各商品行。
- 分摊公式：

```text
单行分摊优惠金额 = 单行金额 / 全部商品行金额合计 * 单据优惠金额
```

- 当全部商品行金额合计为 0 时，不做分摊，单行分摊优惠金额按 0 处理。
- 需要展示分摊结果的单据页，在有列权限时应提供 `优惠分摊` 列，且列值必须与上述规则一致。
- “挂账”识别至少覆盖：
  - 编码为 `CREDIT`、`ON_ACCOUNT`、`AR`、`AP`
  - 结算方式名称中包含“挂账”

## 4. 防闪现与防跳动

问题来源：
- 按钮是否显示通常依赖详情接口返回的 `status`。
- 页面初次渲染时 `formData.status` 为空，详情返回后才变成 `DRAFT` 或 `APPROVED`。
- 如果直接写 `v-if="canRedFlush"`、`v-if="canCopy"`，按钮会在接口返回后突然插入，导致工具栏闪现。

规范做法：
- 每个异步状态按钮拆成两个 computed：
  - `canXxx`：真实可点击条件
  - `shouldShowXxxButton`：是否先渲染占位
- 初始化期间通过 `isInitializing` 渲染禁用占位按钮。
- 详情返回后再根据真实状态启用或隐藏。

示例：

```ts
const canRedFlush = computed(() => {
  return isReadOnly.value
    && formData.status === 'APPROVED'
    && hasPermission('erp-sale:redflush');
});

const shouldShowRedFlushButton = computed(() => {
  if (canRedFlush.value) return true;
  return isInitializing.value
    && isEditing.value
    && (route.query.mode === 'view' || route.query.from === 'approved')
    && hasPermission('erp-sale:redflush');
});
```

模板：

```vue
<el-button
  v-if="shouldShowRedFlushButton"
  type="danger"
  plain
  class="action-button action-button--danger"
  :disabled="isInitializing || !canRedFlush"
>
  红冲
</el-button>
```

必须遵守：
- 审核、复制、红冲这类依赖详情状态的按钮都必须防闪现。
- 初始化时不要显示转圈加载图标，禁用占位比 loading 更稳。
- 按钮宽度必须稳定，`min-width` 不小于 `78px`。
- 禁用态也要保持轻微底色，不要变成突然的白色。

## 5. 保存成功与审核成功弹窗

### 5.1 保存成功弹窗

标题：

```text
保存成功
```

正文：

```text
单据已保存，接下来要做什么？
单号：SOxxxx
```

按钮：

```text
继续新增 / 留在当前页 / 返回列表 / 审核
```

规则：
- 新增页保存后，弹窗必须出现。
- 编辑页保存后，弹窗也必须出现。
- `返回列表` 回 `returnTo`。
- `审核` 调用审核接口，审核成功后进入已审核查看页并切换成审核成功弹窗。

### 5.2 审核成功弹窗

标题：

```text
审核成功
```

正文：

```text
单据已审核，接下来要做什么？
单号：SOxxxx
```

按钮：

```text
继续新增 / 留在当前页 / 返回列表 / 打印
```

规则：
- 审核成功弹窗不再显示“审核”按钮。
- `留在当前页` 停留在已审核查看页。
- `返回列表` 回已审核列表。
- `打印` 关闭当前成功弹窗后，打开打印预览弹窗。

## 6. 审核流程

推荐流程：

```text
点击审核
  -> 只确认一次：确认审核当前单据吗？
  -> 前端静默保存当前表单
  -> 调用审核接口
  -> 跳转到已审核查看页
  -> 关闭新增/编辑标签
  -> 打开审核成功弹窗
```

确认文案：

```text
确认审核当前单据吗？系统会先保存当前修改并审核，审核后将影响库存和应收数据。
```

不要这样做：
- 不要先问“是否保存”，再问“是否审核”。
- 不要跳过保存直接审核，否则用户眼前修改可能没有落库。
- 审核成功后不要跳回草稿列表。

跳转目标：

```ts
router.replace({
  path: `/erp/sale-orders/${savedId}/edit`,
  query: {
    mode: 'view',
    from: 'approved',
    returnTo: '/erp/sale-orders/approved'
  }
});
```

## 7. 打印弹窗

统一使用 `PrintPreviewDialog`，不要新建浏览器窗口，不要跳转当前页到 `/print`。

打印预览弹窗必须：
- 使用 `append-to-body`
- 通过 `v-model` 控制显示
- 传入 `doc-type` 和 `doc-id`
- iframe 内加载 `/erp/<module>/<id>/print?preview=1`

审核成功弹窗内点击“打印”的正确流程：

```text
点击打印
  -> 记录待打印 docId
  -> 关闭审核成功弹窗
  -> 等审核成功弹窗 closed
  -> 设置 printDocId
  -> 打开 PrintPreviewDialog
```

原因：
- 成功弹窗和打印弹窗都是 Element Plus dialog。
- 两个弹窗同一时刻切换容易受过渡和 Teleport 影响。
- 必须等第一个弹窗 `closed` 后再打开第二个。

`PrintPreviewDialog` 规范：

```vue
<el-dialog
  v-model="visible"
  append-to-body
  class="print-preview-dialog"
>
  ...
</el-dialog>
```

## 8. 标签页与返回规则

### 8.1 进入页面

列表页进入新增/编辑/查看时必须带来源：

```ts
router.push({
  path: `/erp/sale-orders/${row.id}/edit`,
  query: {
    mode: 'view',
    returnTo: route.path,
    from: 'approved'
  }
});
```

### 8.2 审核成功后

从新增页审核成功：
- 跳转到已审核查看页。
- 关闭原来的新增销售单标签。
- 已审核查看页保留。

从编辑页审核成功：
- 跳转到已审核查看页。
- 关闭原来的编辑销售单标签。
- 已审核查看页保留。

### 8.3 关闭标签

关闭当前标签时：
- 如果有 `redirectPath`，优先跳 `redirectPath`。
- 否则如果当前路由 query 有 `returnTo`，跳 `returnTo`。
- 否则才跳上一个标签。

`MainLayout.vue` 需要支持：

```ts
const queryReturnTo = typeof route.query.returnTo === 'string'
  ? route.query.returnTo.trim()
  : '';
const resolvedRedirectPath = redirectPath || queryReturnTo;
```

## 9. 列表页表格设计

### 9.1 页面结构

列表页采用：

```vue
<div class="page-shell page-shell--system">
  <div class="page-header">
    <div class="page-title">销售单管理</div>
    <div class="page-toolbar-card">筛选区</div>
  </div>
  <div class="table-card">
    <div class="table-body">表格</div>
  </div>
</div>
```

草稿列表和已审核列表共用组件，通过路由 meta 区分：

| 列表 | `defaultStatus` | `lockStatus` | 状态筛选 |
| --- | --- | --- | --- |
| 草稿列表 | `DRAFT` | `true` | 锁定草稿 |
| 已审核列表 | `APPROVED` | `true` | 锁定已审核 |

### 9.2 筛选区

推荐字段顺序：

| 顺序 | 字段 | 说明 |
| --- | --- | --- |
| 1 | 搜索 | 单号、客户、备注等关键词 |
| 2 | 客户 | 下拉选择 |
| 3 | 开始时间 | 日期时间范围起点 |
| 4 | 结束时间 | 日期时间范围终点 |
| 5 | 新增 | 草稿列表显示 |

规则：
- 筛选控件高度统一。
- 筛选卡片不要过度装饰。
- 状态已锁定的列表不要再暴露状态下拉。

### 9.3 列表表格

推荐列顺序：

| 顺序 | 列 | 对齐 | 宽度建议 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | 序号 | 左/中 | 60 | 使用分页序号 |
| 2 | 单号 | 左 | 160-190 | 可点击进入编辑/查看 |
| 3 | 客户 | 左 | 160 | 长文本省略 |
| 4 | 单据时间 | 左 | 170 | `YYYY-MM-DD HH:mm:ss` |
| 5 | 金额 | 右 | 120 | 金额高亮但不过度放大 |
| 6 | 状态 | 中 | 100 | 草稿/已审核/已红冲 |
| 7 | 操作 | 左 | 160-220 | 编辑/查看/打印/红冲 |

表格规则：
- 表头背景使用中性浅灰白 `#f8fafc`，避免偏蓝色块在白色页面里显脏；不建议纯白，以免和内容行边界不清。
- 表格行高保持紧凑，适合业务数据扫描。
- 操作列使用文字按钮或轻量按钮，避免一行多个高饱和按钮。
- 金额列右对齐，主金额可以使用主蓝色。
- 长文本必须省略，不允许撑破表格。
- 横向滚动条出现时，操作列应保持可见或宽度足够。

## 10. 单据明细表格设计

新增/编辑页明细表格推荐列：

| 顺序 | 列 | 控件 | 宽度建议 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | 勾选 | checkbox | 48 | 批量删除 |
| 2 | 序号 | 文本 | 64 | 稳定窄列 |
| 3 | 商品 | 模糊选择 | 220-280 | 必填 |
| 4 | 仓库/库位 | 下拉 | 180-240 | 必填 |
| 5 | 数量 | 数字输入 | 120 | 4 位小数 |
| 6 | 单价 | 数字输入 | 120 | 4 位小数 |
| 7 | 金额 | 文本 | 120 | 自动计算 |
| 8 | 操作 | 图标按钮 | 80 | 删除 |

查看页明细表格推荐列：

| 顺序 | 列 | 展示 | 说明 |
| --- | --- | --- | --- |
| 1 | 序号 | 文本 | 稳定窄列 |
| 2 | 商品 | 文本 | 可带查看历史图标 |
| 3 | 仓库/库位 | 文本 | 格式：仓库 / 库位 |
| 4 | 数量 | 文本 | 保留业务精度 |
| 5 | 单价 | 文本 | 金额格式 |
| 6 | 金额 | 文本 | 右对齐 |

明细区规则：
- 卡片标题使用左侧蓝色竖条。
- “删除选中”放在明细卡片右上角。
- 新增行按钮放在表格下方，不要放到顶部主操作区。
- 删除建议使用图标按钮，避免文字按钮造成列宽抖动。
- 表格内图标按钮必须固定宽高，避免 hover 导致布局变化。
- 当用户在商品行选择商品后，如该商品已维护默认仓库/库位，系统应自动带出默认仓库/库位，减少重复录入。
- 自动带出仅作为默认预填值，用户必须仍可按当前业务场景手动修改仓库/库位。
- 如商品未维护默认仓库/库位，则仓库/库位保持为空，由用户继续选择；不要强行带出无明确来源的默认值。

## 11. 图标、按钮、控件不闪现细节

必须避免：
- 数据回来后按钮突然插入工具栏。
- 图标按钮 hover 后改变尺寸。
- loading 小圈在页面打开时出现又消失。
- 文案长度变化导致按钮宽度变化。
- 弹窗 A 关闭动画未结束时立即打开弹窗 B。

推荐做法：
- 使用稳定 `min-width` 和固定高度。
- 初始化阶段显示禁用占位，而不是 loading。
- `v-if` 只控制真正不该出现的场景；初始化不确定时用 `shouldShowXxxButton` 占位。
- 弹窗切换使用 `@closed`，不要用固定延时猜动画。
- 打印弹窗、成功弹窗都使用 `append-to-body`。

## 12. 后续模块迁移清单

改采购单、销售退货单、采购退货单时，逐项对照：

| 检查项 | 要求 |
| --- | --- |
| 路由来源 | 列表进入新增/编辑/查看必须带 `returnTo` 和 `from` |
| 返回 | 按来源回草稿列表或已审核列表 |
| 标签关闭 | 关闭详情标签优先跳 `returnTo` |
| 按钮颜色 | 返回中性、保存轻蓝、审核浅绿、打印主蓝、红冲浅红 |
| 防闪现 | 审核、复制、红冲等按钮使用 `shouldShowXxxButton` |
| 审核流程 | 只确认一次，内部静默保存再审核 |
| 审核成功 | 跳已审核查看页，弹审核成功弹窗 |
| 保存成功 | 新增和编辑保存后都弹保存成功弹窗 |
| 打印 | 使用 `PrintPreviewDialog` 弹窗，不新开窗口，不跳转当前页 |
| 打印弹窗 | `append-to-body`，由前一个弹窗 `closed` 后打开 |
| 页面底色 | 新增、编辑、审核后查看页外层 `content-area` 统一为 `#ffffff` |
| 列表表格 | 草稿/已审核复用组件，通过 route meta 锁状态 |
| 明细表格 | 新增/编辑控件密集但稳定，查看页只读展示 |

## 13. 命名建议

为后续模块统一命名：

```ts
const canApprove = computed(...);
const shouldShowApproveButton = computed(...);

const canRedFlush = computed(...);
const shouldShowRedFlushButton = computed(...);

const canCopy = computed(...);
const shouldShowCopyButton = computed(...);

const openSaveSuccessDialog = (..., mode: 'save' | 'approve' = 'save') => {};
const closeSaveSuccessDialog = () => {};
const handleSaveSuccessDialogClosed = async () => {};
```

CSS 类：

```css
.action-button
.action-button--save
.action-button--success
.action-button--secondary
.action-button--primary
.action-button--danger
```

不要为每个模块重新发明一套按钮色彩和弹窗流程。采购单、销售退货单等模块应只替换业务名、接口路径、权限码和列表路由。
