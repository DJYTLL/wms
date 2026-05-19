# 系统页布局约束规范

基准页面：
- 列表页基准：`src/views/system/AuditLogManagement.vue`
- 单据表单页基准：`src/views/erp/ErpSaleOrderForm.vue`

关联公共样式：
- `src/styles/table.css`
- `src/layouts/MainLayout.vue`

适用范围：
- 系统设置下的列表页、筛选页、配置页
- ERP 下的新增页、编辑页、单据页
- 后续 AI 新增或改造的后台业务页

目标：
- 页面切换时不抖动
- 同类页面标题、首屏、卡片区、按钮区保持统一节奏
- 列表页和表单页各自复用稳定骨架，不混用错误基线
- 中英文切换时布局不因文案长度发生明显偏移

## 0. 为什么要从“单一规范”改成“两套基线”

旧文档默认所有系统页都应向审计日志页靠齐，这只适用于“标题 + 筛选卡片 + 表格”的列表页，不适用于新增销售单这种“标题 + 全局操作 + 多段表单卡片”的单据页。

新增销售单正式页 `ErpSaleOrderForm.vue` 的真实结构是：

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
  <div class="table-card sale-detail-card">明细信息</div>
  <div class="table-card payment-card">结算信息</div>
</div>
```

这和审计日志页有本质区别：

- 列表页的首个业务内容块需要放进 `page-header` 内，保证标题和筛选卡片联动稳定。
- 单据表单页的 `page-header` 允许承载“标题组 + 全局操作按钮”，首个业务卡片放在 `page-header` 外，作为独立内容段落。
- 如果继续拿列表页规则硬套单据页，就会得出“销售单页面结构不合规”的错误结论；反过来如果把单据页规则套回列表页，又会重新引入筛选区抖动。

结论：
- 以后必须先判断页面类型，再套对应基线。
- 页面分为两类：
  - `A 类：列表/筛选页`
  - `B 类：单据/表单页`
- 禁止把 A 类和 B 类的 DOM 层级规则混写。

## 1. 共享基线

无论是列表页还是单据页，都必须复用以下公共基线：

- 页面根容器必须包含 `page-shell`
- 需要使用系统内容区基线时，必须包含 `page-shell--system`
- 主标题统一使用 `.page-title`
- 页面根节点禁止额外再叠一层无来源的 `padding-top`
- 页面首屏禁止随意新增第二套 `page-shell`、`page-header`、`page-title`

共享尺寸基线：

- 页面横向内边距默认基线：`20px`
- 标题字号：`24px`
- 标题行高：`32px`
- 标题字重：`600` 到 `800`
- 标题区与下一段内容默认垂直距离：`16px`
- 区块之间常用纵向间距：`16px` 或 `18px`

共享标题推荐写法：

```vue
<div class="page-header">
  <div class="page-title">页面标题</div>
</div>
```

如果页面需要标题辅助信息，例如面包屑、副标题、状态标签，应放进标题组中，不应再造第二个主标题。

## 2. A 类页面：列表 / 筛选页规范

适用页面：
- 审计日志
- 租户管理
- 各类配置列表页
- ERP 管理类列表页

### 2.1 页面骨架

列表页必须使用以下结构：

```vue
<div class="page-shell page-shell--system">
  <div class="page-header">
    <div class="page-title">页面标题</div>
    <div class="page-toolbar-card">
      <div class="table-toolbar">首个业务内容块或筛选卡片</div>
    </div>
  </div>

  <div class="table-card">...</div>
</div>
```

约束：
- 标题必须是 `page-header` 的第一个子元素
- 首个业务内容块必须放在 `page-header` 内
- 列表页首个业务内容块外层必须有卡片容器
- 页面级按钮必须进入该卡片内部，不能单独作为 `page-header` 第二个子元素
- 禁止把 `table-toolbar` 直接作为 `page-header` 第二个子元素

### 2.2 标题区数值

这些值来自 `src/styles/table.css`，属于列表页公共基线：

- 页面内容区内边距：`20px`
- 标题行最小高度：`40px`
- 标题文字盒子最小高度：`32px`
- 标题区内部多元素间距：`12px`
- 标题区与下一个元素之间距离：`16px`

对应规则：

```css
.page-shell--system {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  min-height: 40px;
  margin-bottom: 16px !important;
  gap: 12px;
}

.page-title {
  display: flex;
  align-items: center;
  min-height: 32px;
  margin: 0;
  font-size: 24px;
  line-height: 32px;
}
```

### 2.3 筛选卡片

审计日志页是列表筛选页基准：

- 卡片内边距：`16px 18px`
- 边框：`1px solid #e5e7eb`
- 圆角：`10px`
- 背景：`#ffffff`
- 宽度：`100%`
- 第一个输入框横向起点：页面内容区左边缘 `20px` + 卡片左内边距 `18px`

```css
.audit-toolbar {
  width: 100%;
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #ffffff;
  box-sizing: border-box;
}
```

如果不用 `audit-toolbar` 这个类名，也必须提供等价容器。

### 2.4 筛选区布局

- 主布局：左右两列
- 左列：筛选项
- 右列：操作按钮
- 主区域横向间距：`12px`

```css
.table-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
}
```

筛选项宽度基线：

- 关键词输入框：`220px`
- 普通下拉：`140px`
- 日期范围框：`380px`
- 控件间距：`12px`

```css
.table-filters {
  display: grid;
  grid-template-columns: 220px 140px 140px 140px 380px;
  gap: 12px;
}
```

### 2.5 日期时间范围

只要页面使用 `type="datetimerange"` 且格式为 `YYYY-MM-DD HH:mm:ss`，统一按以下基线：

- 外层宽度：`380px`
- 内部两个输入框宽度：`132px`
- 内部字号：`12px`

```vue
<el-date-picker
  type="datetimerange"
  format="YYYY-MM-DD HH:mm:ss"
  class="table-date-range audit-toolbar__date-range"
/>
```

```css
:deep(.audit-toolbar__date-range) {
  width: 380px;
}

:deep(.audit-toolbar__date-range.el-range-editor) {
  width: 380px !important;
  min-width: 380px !important;
}

:deep(.audit-toolbar__date-range .el-range-input) {
  width: 132px;
  font-size: 12px;
}
```

禁止事项：
- 禁止继续复用旧的 `336px / 112px`
- 禁止不同页面各自定义一套秒级日期范围宽度

### 2.6 列表页按钮区

- 按钮区与筛选区间距：`12px`
- 按钮之间间距：`10px`
- 按钮区不换行

```css
.table-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
  justify-content: flex-end;
}
```

### 2.7 列表页表格卡片

- 首个表格卡片不再额外追加 `margin-top`
- 圆角：`8px`
- 边框：`1px solid #e5e5e5`
- 分页区内边距：`12px 16px`

```css
.table-card {
  border: 1px solid #e5e5e5;
  border-radius: 8px;
}

.table-pagination {
  padding: 12px 16px;
}
```

## 3. B 类页面：单据 / 表单页规范

适用页面：
- 新增销售单
- 编辑销售单
- 后续新增、编辑、查看类 ERP 单据页

### 3.1 页面骨架

单据页以 `ErpSaleOrderForm.vue` 为正式基线：

```vue
<div class="page-shell page-shell--system sale-page-surface">
  <div class="page-header sale-page-header">
    <div class="sale-title-group">
      <div class="page-title">页面标题</div>
      <div class="sale-breadcrumb">面包屑</div>
    </div>
    <div class="table-actions sale-page-toolbar__actions">全局操作</div>
  </div>

  <div class="page-toolbar-card sale-header-card">基础信息</div>
  <div class="table-card sale-detail-card">明细信息</div>
  <div class="table-card payment-card">结算信息</div>
</div>
```

关键结论：
- 单据页允许 `page-header` 承载“标题组 + 全局操作按钮”
- 单据页的首个业务卡片允许放在 `page-header` 外
- 单据页不要求把第一个表单卡片塞进 `page-header`
- 列表页和单据页的“首个业务内容块位置”规则不同，禁止互相套用

### 3.2 页面外层与背景

新增销售单页已经验证过的安全基线：

- 页面根内边距：`16px 20px`
- 内容区背景：页面外层浅灰，卡片白底
- 页面最小高度：`100%`

```css
:global(.content-area:has(.sale-page-surface)) {
  background: #f5f7fb;
}

.sale-page-surface {
  min-height: 100%;
  height: auto;
  padding: 16px 20px;
  box-sizing: border-box;
  background: transparent;
}
```

说明：
- 单据页顶部更适合 `16px 20px` 的紧凑节奏，而不是列表页 `20px`
- 这属于单据页基线，不应回流覆盖列表页

### 3.3 标题组与全局操作区

新增销售单页标题区基线：

- `sale-page-header` 间距：`14px`
- 标题组内部间距：`18px`
- 面包屑字体：`13px`
- 面包屑与按钮共存时，标题仍保持主视觉
- 全局操作按钮放在标题行，不进入表单卡片

```css
.sale-page-header {
  align-items: center !important;
  margin-bottom: 16px !important;
  gap: 14px;
}

.sale-title-group {
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
}

.sale-breadcrumb {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  font-size: 13px;
  white-space: nowrap;
}
```

适用场景：
- 返回
- 复制
- 打印
- 红冲
- 保存
- 审核
- 保存并返回

约束：
- 这些按钮属于页面级全局动作，可以合法地作为 `page-header` 的第二块内容
- 但只对 B 类单据页开放，不适用于 A 类列表页

### 3.4 卡片基线

新增销售单页的三大主卡片统一使用：

- 白底
- `1px` 边框
- `12px` 圆角
- 阴影卡片
- 卡片内容默认不裁剪

```css
.sale-page-surface .sale-header-card,
.sale-page-surface .sale-detail-card,
.sale-page-surface .payment-card {
  border: 1px solid var(--sale-card-border);
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 16px 36px rgba(28, 45, 76, 0.08), 0 4px 12px rgba(28, 45, 76, 0.04);
  overflow: visible;
}
```

卡片间距：
- 基础信息卡片到明细卡片：`18px`
- 明细卡片到结算卡片：`18px`

### 3.5 分节标题

单据页卡片标题统一使用 `card-section-header`：

- 左右布局
- 标题与操作区同一行
- 标题前带 `4px x 20px` 主色竖条
- 标题字号：`15px`
- 标题字重：`700`
- 与正文间距：`16px`

```css
.card-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.card-section-header h4 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 15px;
  font-weight: 700;
}

.card-section-header h4::before {
  content: '';
  width: 4px;
  height: 20px;
  border-radius: 999px;
}
```

### 3.6 基础信息区

基础信息卡片使用 `page-toolbar-card sale-header-card`，当前安全基线为：

- 卡片内边距：`20px 22px`
- 表单标签位置：`top`
- 主表单网格：4 列
- 列定义：`repeat(4, minmax(190px, 1fr))`
- 网格间距：`18px 32px`

```css
.sale-header-card {
  padding: 20px 22px;
}

.sale-header-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(190px, 1fr));
  gap: 18px 32px;
}
```

字段基线：
- 单号
- 下单时间
- 客户
- 交货方式
- 备注

约束：
- 基础信息区优先使用固定列数网格，不使用随文案漂移的自由流式布局
- 备注作为长字段允许单独占更大宽度，但不能破坏整个网格节奏

### 3.7 明细区

明细卡片当前基线：

- 卡片内边距：`18px 22px 18px`
- 明细区内部纵向间距：`16px`
- 标题行允许右侧放批量删除等局部操作
- 表格外层再包一层带边框容器

```css
.sale-detail-card {
  padding: 18px 22px 18px;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-table-wrapper {
  border: 1px solid #e1e9f4;
  border-radius: 10px;
  background: #fbfdff;
  overflow: hidden;
}
```

表格列设计原则：
- 选择列、序号列固定宽度
- 核心业务列如商品、库位使用 `min-width`
- 数量、单价、行金额使用固定宽度
- 只读态与编辑态必须共用相同列宽，不允许来回跳动

### 3.8 明细底部操作和汇总

新增销售单页使用“左操作、右汇总”的底栏模式：

- 底栏两端对齐
- 区块间距：`18px`
- 汇总项间距：`26px`
- 总金额字体强化

```css
.detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.detail-summary {
  display: flex;
  align-items: baseline;
  gap: 26px;
  justify-content: flex-end;
  margin-left: auto;
}
```

约束：
- “添加商品”等局部编辑按钮放左侧
- 金额汇总、利润汇总放右侧
- 不允许把汇总直接塞到表格最后一行伪装成数据行

### 3.9 结算区

结算卡片当前基线：

- 卡片上边距：`18px`
- 卡片内边距：`18px 22px 18px`
- 结算表单使用独立网格
- 列定义：
  - `minmax(260px, 430px)`
  - `minmax(220px, 320px)`
  - `minmax(220px, 320px)`
  - `minmax(220px, 320px)`
- 行列间距：`16px 32px`

```css
.payment-card {
  margin-top: 18px;
  padding: 18px 22px 18px;
}

.payment-grid {
  display: grid;
  grid-template-columns: minmax(260px, 430px) minmax(220px, 320px) minmax(220px, 320px) minmax(220px, 320px);
  gap: 16px 32px;
  align-items: start;
}
```

字段基线：
- 结算方式
- 已付金额
- 优惠金额
- 客户欠款合计

提示文案区：
- 放在表单区下方
- 使用轻提示色
- 与正文保持 `8px` 左右的上间距

### 3.10 控件样式基线

单据页表单控件统一使用更轻的业务卡片风格：

- 输入框/下拉最小高度：`36px`
- 圆角：`6px`
- 文本字号：`14px`
- 聚焦态带主色描边和弱阴影
- 禁用态使用浅灰底

```css
:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  min-height: 36px;
  border-radius: 6px;
}

:deep(.el-textarea__inner) {
  padding: 10px 12px;
  font-size: 14px;
}
```

说明：
- 这套控件视觉属于单据页主题，可局部跟随页面主题变量
- 但高度、圆角、字号应保持统一，不要每张单据各写一套

## 4. 响应式断点

### 4.1 A 类列表页

小于等于 `1280px`：

- 工具栏内边距改为 `14px`
- 主布局改为单列
- 按钮区左对齐
- 关键词输入框改为 `200px`
- 日期范围框改为 `360px`

```css
@media (max-width: 1280px) {
  .audit-toolbar {
    padding: 14px;
  }

  .table-toolbar {
    grid-template-columns: minmax(0, 1fr);
  }

  .table-filters {
    grid-template-columns: 200px 140px 140px 140px 360px;
  }
}
```

小于等于 `768px`：

- 筛选项改单列
- 按钮区独占一行
- 所有输入框、选择框、日期框改为 `100%`

```css
@media (max-width: 768px) {
  .table-filters {
    grid-template-columns: 1fr;
  }

  .table-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
```

### 4.2 B 类单据页

小于等于 `1280px`：

- 基础信息区、结算区网格改为 2 列
- 备注改为占满整行
- 明细底栏改为纵向堆叠
- 汇总区允许换行并左对齐

```css
@media (max-width: 1280px) {
  .sale-header-grid,
  .payment-grid {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }

  .detail-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .detail-summary {
    flex-wrap: wrap;
    justify-content: flex-start;
    margin-left: 0;
  }
}
```

小于等于 `768px`：

- 页面内边距改为 `16px`
- 标题组、按钮组都改为纵向堆叠
- 页面级按钮宽度改为 `100%`
- 面包屑允许换行
- 基础信息区、结算区都改为单列

```css
@media (max-width: 768px) {
  .sale-page-surface {
    padding: 16px;
  }

  .sale-page-header,
  .sale-title-group,
  .sale-page-toolbar__actions {
    align-items: flex-start !important;
    flex-direction: column;
  }

  .sale-page-toolbar__actions {
    width: 100%;
    margin-left: 0;
  }

  .sale-page-surface .action-button {
    width: 100%;
  }

  .sale-breadcrumb {
    flex-wrap: wrap;
    white-space: normal;
  }

  .sale-header-grid,
  .payment-grid {
    grid-template-columns: 1fr;
  }
}
```

## 5. AI 改页时的强制规则

以后让 AI 生成或修改后台页面时，必须先判断页面类型，再套用以下规则：

1. 必须先判断这是 `A 类列表页` 还是 `B 类单据页`。
2. 必须复用 `page-shell`，需要系统内容区基线时复用 `page-shell--system`。
3. 列表页：
   - 标题必须是 `page-header` 第一个子元素
   - 首个业务内容块必须在 `page-header` 内
   - 筛选区必须带工具栏卡片外层
4. 单据页：
   - `page-header` 允许放“标题组 + 全局操作按钮”
   - 首个表单卡片允许放在 `page-header` 外
   - 多卡片纵向排布必须保持统一间距
5. 列表页不能照搬销售单的标题操作栏结构；单据页也不能照搬审计日志的筛选区结构。
6. 日期范围框如果使用秒级格式，统一复用 `380px / 132px` 基线。
7. 中英文切换后，主输入控件、按钮组、汇总区不能出现明显跳位。
8. 改完后至少扫描这些关键词：
   - `page-shell`
   - `page-header`
   - `page-title`
   - `table-toolbar`
   - `table-actions`
   - `table-filters`
   - `sale-page-header`
   - `sale-header-grid`
   - `payment-grid`
   - `margin-top:`
   - `padding:`

## 6. 推荐改造顺序

1. 先判定页面属于列表页还是单据页
2. 再统一页面根结构到正确骨架
3. 再统一 `page-header` 的 DOM 层级
4. 再统一首屏区块位置
5. 再统一卡片间距、内边距和标题区节奏
6. 再统一输入控件、日期控件、按钮区宽度
7. 最后处理响应式和中英文切换

## 7. 不允许出现的常见问题

- 没有先判页面类型，直接拿旧规范硬套
- 列表页标题在 `page-header` 内，筛选卡片却放到 `page-header` 外
- 单据页明明有全局操作，却把所有按钮硬塞进基础信息卡片首行
- 把销售单的按钮行结构复制到审计日志、租户管理这类列表页
- 把列表页筛选卡片结构复制到销售单这种表单页
- 同类输入框一页 `180px`，另一页 `200px`
- 秒级日期框继续沿用 `336px / 112px`
- 中英文切换后按钮把主表单列挤坏
- 页面根容器自己再写一套来源不明的 `gap`、`padding-top`、`margin-top`

这份文档现在是“后台业务页布局约束”的统一基线，但它不是“所有页面只能有一种结构”的单一模板。以后任何页面改造都必须先归类，再落到对应骨架，不能默认自由发挥。
