# 模块开发模板（列表/新增/详情/审核）

> 目标：按本模板开发业务模块页面，保证与现有项目风格与交互保持 >=90% 一致。

## 1. 页面清单与路由
- 列表（状态 A）：`/erp/<module>`（状态筛选 `STATUS_A`）
- 列表（状态 B）：`/erp/<module>`（状态筛选 `STATUS_B`）
- 新增：`/erp/<module>/create`
- 详情/审核：`/erp/<module>/:id`

## 2. 通用布局模板（所有页面必遵循）
- 外层：`<div class="page-shell">`
- 头部：`<div class="page-header">` + `<h2 class="page-title">`
- 列表：`<div class="table-card">` + `<div class="table-body">`
- 分页：`el-pagination` 固定 `layout="total, sizes, prev, pager, next, jumper"`

## 3. 列表页模板（多状态共用）
### 3.1 顶部筛选区
- 搜索：`keyword`
- 业务对象筛选：`<filterId>`
- 状态：`status`（`STATUS_A` / `STATUS_B`）
- 时间范围：`startAt` / `endAt`（毫秒时间戳）

### 3.2 表格字段顺序（示例）
1. index
2. 主单号（可点击进入详情）
3. 业务对象名称
4. status
5. totalAmount
6. createdAt

### 3.3 操作按钮
- 新增按钮：`v-permission="'<module>:add'"`

### 3.4 接口
- 列表：`GET /erp/<module>/page`
- 业务对象列表：`GET /erp/<module-related>`

## 4. 新增页模板
### 4.1 页面结构
- 头部：返回 + 保存
- 表单：`el-form` + `label-position="top"`

### 4.2 字段顺序（示例）
1. 主单号（只读）
2. 业务对象（必填）
3. 关联单号（可选）
4. 结算方式（必填）
5. 金额（必填 > 0）
6. 业务时间
7. 备注

### 4.3 业务联动（示例）
- 选业务对象后加载关联单：
  - `GET /erp/<related>?<filterId>=xxx&status=OPEN`
- 选关联单后自动填金额

### 4.4 接口
- 主单号：`GET /erp/<module>/next-no`
- 配置项：`GET /erp/<config>`
- 新增：`POST /erp/<module>`

### 4.5 返回规则
- 新增页面按钮必须包含：返回、保存、保存返回
- 返回：直接回到该模块列表页：`/erp/<module>`
- 保存：仅保存，不关闭当前页面
- 保存返回：保存成功后关闭当前页面并返回该模块列表页：`/erp/<module>`

### 4.6 关闭/重开规则
- 关闭页面再打开时，除默认值外的所有填写字段必须清空
- 所有单据时间字段默认值为“当前时刻”

## 5. 详情/审核页模板
### 5.1 展示方式
- `el-descriptions`，`column="2"`，`border`

### 5.2 字段顺序（示例）
1. 主单号
2. 关联单号
3. 业务对象
4. 金额
5. 结算方式
6. 业务时间
7. status
8. 备注

### 5.3 接口
- 详情：`GET /erp/<module>/:id`

### 5.4 可选审核按钮
- 权限码：`<module>:approve`
- 建议接口：`POST /erp/<module>/:id/approve`
- 审核按钮行为：审核成功后关闭当前页面，并返回该模块列表页：`/erp/<module>`

## 6. 国际化 key 模板
- `page.<module>Management`
- `page.<module>Create`
- `page.<module>Detail`
- `field.no`
- `field.object`
- `field.relatedNo`
- `field.method`
- `field.amount`
- `field.time`
- `field.status`
- `field.remark`
- `field.createdTime`
- `action.add`
- `action.search`
- `action.save`
- `action.back`
- `filter.all`
- `status.statusA`
- `status.statusB`

## 7. 复用检查清单（交付前确认）
- 页面结构与样式类一致
- 列表筛选/分页参数一致
- 字段顺序一致
- 权限码一致
- 国际化 key 一致
- 接口路径与参数一致
