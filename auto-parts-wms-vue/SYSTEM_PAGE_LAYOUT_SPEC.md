# 系统页布局约束规范

基准页面：`src/views/system/AuditLogManagement.vue`

关联公共样式：
- `src/styles/table.css`
- `src/layouts/MainLayout.vue`

适用范围：
- 系统设置下的列表页、筛选页、配置页
- 后续 AI 新增或改造的系统页

目标：
- 页面切换时不抖动
- 同类页面标题、筛选区、表格区、按钮区保持统一节奏
- 中英文切换时布局不因文案长度发生明显偏移

## 0. 本次抖动问题根因

这次从审计日志页切换到列权限配置页一直抖动，不是单纯的 `font-size`、`margin-bottom` 或 `h2` 默认样式问题，真正根因是两页的 DOM 结构不同。

审计日志页结构：

```vue
<div class="page-header">
  <div class="page-title">审计日志</div>
  <div class="audit-toolbar">...</div>
</div>
```

列权限配置页之前的结构：

```vue
<div class="page-header">
  <div class="page-title">列权限配置</div>
  <div class="card-actions">保存</div>
</div>

<div class="top-card">...</div>
```

这会造成两个问题：

- `page-header` 在审计日志页包含“标题 + 首块内容”，而列权限配置页只包含“标题 + 按钮”，首块内容在外面。
- 路由切换时，浏览器和布局系统计算 `page-header` 高度、`margin-bottom`、首块内容位置的方式不同，所以即使标题字号、行高、间距数值一致，视觉上仍然会跳一下。

正确修法：

```vue
<div class="page-header">
  <div class="page-title">列权限配置</div>
  <div class="top-card">...</div>
</div>
```

结论：
- 页面布局一致性不能只看 CSS 数值，必须同时保证 DOM 层级一致。
- 首个业务内容块必须和审计日志页一样放在 `page-header` 内，作为标题后的第二个子元素。
- 页面级操作按钮应放进首个业务内容块的操作区，不能单独作为 `page-header` 的第二个子元素去撑标题行。

## 0.1 首个输入框横向抖动根因

租户管理页曾经已经把首个业务内容块放进了 `page-header`，但切换到审计日志页时第一个输入框仍然横向抖动。原因是两页虽然 DOM 层级接近，但首个业务内容块的容器结构不同。

审计日志页结构：

```vue
<div class="page-header">
  <div class="page-title">审计日志</div>
  <div class="audit-toolbar">
    <div class="table-toolbar">
      <div class="table-filters">
        <el-input />
      </div>
    </div>
  </div>
</div>
```

租户管理页之前的结构：

```vue
<div class="page-header">
  <div class="page-title">租户管理</div>
  <div class="table-toolbar">
    <div class="table-filters">
      <el-input />
    </div>
  </div>
</div>
```

审计日志页第一个输入框的实际横向起点是：

```text
page-shell 左内边距 20px + audit-toolbar 左内边距 18px = 38px
```

租户管理页之前第一个输入框的实际横向起点是：

```text
page-shell 左内边距 20px
```

所以两个页面即使标题和 `page-header` 结构一致，第一个输入框仍然相差 `18px`，页面切换时会出现横向跳动。

正确修法：

```vue
<div class="page-header">
  <div class="page-title">租户管理</div>
  <div class="tenant-toolbar">
    <div class="table-toolbar">
      <div class="table-filters">
        <el-input />
      </div>
    </div>
  </div>
</div>
```

结论：
- 列表页首个筛选区必须有一层和 `audit-toolbar` 等价的卡片容器。
- 该容器必须 `width: 100%`，并使用 `padding: 16px 18px`。
- 不能把 `table-toolbar` 直接作为 `page-header` 的第二个子元素，否则第一个输入框横向起点会比审计日志少 `18px`。

## 1. 页面骨架

系统页必须使用以下结构：

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

禁止事项：
- 禁止页面根节点自定义额外 `padding-top`
- 禁止标题区和首个内容块之间再叠加第二层 `margin-top`
- 禁止每个页面重新定义一套 `page-shell`、`page-header`、`page-title`
- 禁止直接使用带默认外边距的 `h1`、`h2`、`h3` 作为系统页主标题，除非完全重置默认样式
- 禁止把首个业务内容块放在 `page-header` 外面
- 禁止把页面级按钮单独作为 `page-header` 第二个子元素，按钮应进入首个业务内容块内部
- 禁止列表页把 `table-toolbar` 直接作为 `page-header` 第二个子元素，外层必须有工具栏卡片容器

## 2. 页面外边距与标题区

这些值来自 `src/styles/table.css`，属于公共基线：

- 页面内容区内边距：`20px`
- 即标题距离内容区上边缘：`20px`
- 标题元素必须是 `page-header` 的第一个子元素
- 标题元素推荐写法：`<div class="page-title">...</div>`
- 标题行最小高度：`40px`
- 标题文字行高：`32px`
- 标题文字盒子最小高度：`32px`
- 标题字号：`24px`
- 标题字重：`600`
- 标题区与下一个元素之间距离：`16px`
- 标题区内部多元素间距：`12px`

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
  font-size: 24px;
  line-height: 32px;
  margin: 0;
}
```

解释：
- “标题距离上边框多远” = `20px`
- “标题元素盒子顶部距离标题行顶部多远” = `4px`
  原因：`page-header` 最小高度 `40px`，`page-title` 最小高度 `32px`，垂直居中后上下各剩 `4px`
- “标题下一个元素距离标题多远” = `16px`
- “标题所处的位置” = 页面内容区上边缘向下 `20px` 进入标题行，标题元素在标题行中垂直居中，标题盒子顶部实际落点约为页面内容区上边缘向下 `24px`

## 3. 审计日志页筛选区规范

审计日志页作为系统筛选页基准，筛选区整体采用卡片式工具栏。

### 3.1 筛选区容器

- 筛选卡片内边距：`16px 18px`
- 边框：`1px solid #e5e7eb`
- 圆角：`10px`
- 背景：`#ffffff`
- 宽度：`100%`
- 第一个输入框横向起点：页面内容区左边缘 `20px` + 筛选卡片左内边距 `18px`

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

列表页如果不用 `audit-toolbar` 这个类名，也必须提供等价容器，例如：

```css
.tenant-toolbar {
  width: 100%;
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #ffffff;
  box-sizing: border-box;
}
```

### 3.2 主筛选区布局

主筛选区必须固定列宽，不允许依赖文案长度自动挤压：

- 主布局：两列
- 左列：筛选项区域
- 右列：操作按钮区域
- 主区域横向间距：`12px`

```css
.table-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
}
```

### 3.3 筛选项宽度

以审计日志页为基准：

- 关键词输入框宽度：`220px`
- 普通下拉宽度：`140px`
- 日期范围框宽度：`336px`
- 筛选项之间水平间距：`12px`

```css
.table-filters {
  display: grid;
  grid-template-columns: 220px 140px 140px 140px 336px;
  gap: 12px;
}
```

解释：
- “时间选择框多长” = `336px`
- “input 框之间距离多宽” = `12px`

### 3.4 日期范围选择框

日期范围框必须满足“带年份且完整显示”：

- 显示格式：`YYYY-MM-DD HH:mm`
- 外层宽度：`336px`
- 内部两个输入框宽度：`112px`
- 内部字号：`12px`

```vue
<el-date-picker
  type="datetimerange"
  format="YYYY-MM-DD HH:mm"
  class="table-date-range audit-toolbar__date-range"
/>
```

```css
:deep(.audit-toolbar__date-range) {
  width: 336px;
}

:deep(.audit-toolbar__date-range.el-range-editor) {
  width: 336px !important;
  min-width: 336px !important;
}

:deep(.audit-toolbar__date-range .el-range-input) {
  width: 112px;
  font-size: 12px;
}
```

约束说明：
- 禁止为了省空间把显示格式改成每页都不一样
- 禁止不同系统页出现第二套日期框宽度
- 如果必须展示更长内容，优先拆成两个独立时间框，而不是继续挤压

## 4. 操作按钮区

按钮区必须与筛选项分离，不允许混在同一行自由挤压。

基准值：

- 按钮区与筛选区间距：`12px`
- 按钮之间间距：`10px`
- 按钮区不换行：`flex-wrap: nowrap`

```css
.table-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
  justify-content: flex-end;
}
```

约束说明：
- 中英文切换后，按钮文案允许变长
- 但按钮区不能反向挤压日期框和输入框

## 5. 高级筛选区

如果页面存在“更多筛选”展开区，使用以下节奏：

- 上边距：`12px`
- 上内边距：`12px`
- 与主筛选区分隔线：`1px solid #eef1f4`
- 筛选项间距：`12px`

```css
.audit-toolbar__advanced {
  gap: 12px;
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px solid #eef1f4;
}
```

## 6. 表格区

系统列表页表格卡片统一采用以下基线：

- 标题区下方首个表格卡片不再额外追加 `margin-top`
- 表格卡片圆角：`8px`
- 表格卡片边框：`1px solid #e5e5e5`
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

## 7. 响应式断点

### 7.1 小于等于 `1280px`

- 工具栏内边距改为 `14px`
- 主布局改为单列
- 按钮区左对齐
- 关键词输入框改为 `200px`
- 日期范围框改为 `320px`

```css
@media (max-width: 1280px) {
  .audit-toolbar {
    padding: 14px;
  }

  .table-toolbar {
    grid-template-columns: minmax(0, 1fr);
  }

  .table-filters {
    grid-template-columns: 200px 140px 140px 140px 320px;
  }
}
```

### 7.2 小于等于 `768px`

- 筛选项改为单列
- 按钮区独占一行
- 所有输入框、选择框、日期框宽度改为 `100%`

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

## 8. AI 改页时的强制规则

以后让 AI 生成或修改系统页时，必须附带这些约束：

1. 必须复用 `page-shell page-shell--system`，禁止自定义页面根间距。
2. 标题必须是 `page-header` 的第一个子元素，首个业务内容块必须是 `page-header` 内的后续子元素。
3. 列表页首个业务内容块必须有一层工具栏卡片容器，容器内边距固定 `16px 18px`。
4. 标题到页面上边缘固定 `20px`，标题到首个内容块固定 `16px`。
5. 列表筛选区必须复用审计日志页布局节奏，不允许局部自定义第二套 `gap/padding/margin-top`。
6. 日期范围框统一按基线宽度实现，禁止每个页面单独决定长短。
7. 按钮区必须进入首个业务内容块内部占位，不能单独撑高标题行，也不能与筛选项互相挤压。
8. 中英文切换不能导致主输入控件宽度、位置、行数明显变化。
9. 改完后必须扫描：
   - `page-shell`
   - `page-header`
   - `page-title`
   - `gap:`
   - `margin-top:`
   - `padding:`
   - `table-filters`
   - `table-actions`

## 9. 推荐改造顺序

修改其他系统页时，按下面顺序执行：

1. 先统一页面根结构到 `page-shell page-shell--system`
2. 再统一 `page-header` 内部 DOM 结构，确认标题和首个业务内容块在同一个 `page-header` 中
3. 再统一首个业务内容块外层工具栏卡片，确认第一个输入框从卡片左内边距后开始
4. 再统一标题区数值
5. 再统一筛选区卡片样式
6. 再统一输入框、下拉框、日期框宽度
7. 最后处理响应式和中英文切换

## 10. 不允许出现的常见问题

- 标题区下面再手写一个 `margin-top: 20px`
- 标题在 `page-header` 内，首个业务内容块却放在 `page-header` 外
- `table-toolbar` 直接作为 `page-header` 第二个子元素，缺少工具栏卡片外层
- 页面级保存、新增、搜索按钮单独作为 `page-header` 第二个子元素撑高标题行
- 同类输入框一页 `180px`，另一页 `200px`
- 日期框在中文一套长度、英文另一套长度
- 按钮文案变长后把筛选区挤换行
- 页面根容器自己再写 `gap`
- `page-shell`、`table-toolbar`、`table-filters` 在每个页面出现不同版本

这份文档应作为系统页视觉与布局改造的唯一基线。如果某个页面确实需要偏离，必须先说明原因，再局部例外，不能默认自由发挥。
