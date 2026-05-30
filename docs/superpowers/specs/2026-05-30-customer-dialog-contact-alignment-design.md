# 客户弹窗与联系方式对齐供应商设计

## 背景

当前客户管理页的编辑弹窗直接内嵌在 [ErpCustomerManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerManagement.vue) 中，联系方式仍然使用表格逐行录入。供应商管理已经完成了新的弹窗交互：

- 独立弹窗组件
- 可拖拽、四角缩放
- 分区卡片式布局
- 联系人卡片 + 联系方式标签输入
- 主联系人、主号码

为了统一主数据维护体验，这次将客户管理弹窗和联系方式交互整体对齐到供应商当前方案。

## 目标

1. 客户编辑弹窗改为独立组件。
2. 客户弹窗交互骨架与供应商弹窗保持一致。
3. 客户联系方式录入方式改为“联系人卡片 + 联系方式标签输入”。
4. 保持客户现有后端数据结构兼容，不改数据库表结构。
5. 保持客户列表、搜索、导入与现有业务接口兼容。

## 非目标

1. 不改客户导入 Excel 模板结构。
2. 不改客户列表列结构为单列“联系方式”。
3. 不新增数据库字段、索引、约束或 Flyway migration。
4. 不改客户后端接口契约的字段名。

## 方案对比

### 方案 A：只统一样式，不改联系方式模型

- 客户弹窗改成供应商风格
- 联系方式仍保留表格逐行输入

优点：

- 改动较小

缺点：

- 用户体验没有真正统一
- “客户/供应商一致”的目标未完成

### 方案 B：统一弹窗骨架，同时统一联系方式交互

- 客户弹窗改为独立组件
- 联系方式对齐供应商“联系人卡片 + 标签输入”
- 保存时继续兼容旧字段和 `contacts`

优点：

- 用户体验完整统一
- 不需要改表
- 后续维护成本更低

缺点：

- 前端改动范围比方案 A 大

### 方案 C：抽取完全通用的主数据弹窗基类

- 同时重构客户、供应商、商品弹窗为统一底层

优点：

- 长期复用性高

缺点：

- 超出本次最小范围
- 风险和联动面都更大

## 选型

采用方案 B。

原因：

- 满足“客户管理中的弹窗和联系方式与供应商一致”的目标
- 不动数据库，风险可控
- 不把这次任务扩大成跨多个主数据页面的通用框架重构

## 页面结构设计

新增独立组件：

- [ErpCustomerEditDialog.vue](D:/project/auto-parts-wms-vue/src/components/ErpCustomerEditDialog.vue)

客户管理页改为挂载独立弹窗组件：

- [ErpCustomerManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerManagement.vue)

客户弹窗整体结构对齐供应商弹窗，包含：

1. 顶部标题区
   - 标题
   - 副标题
   - 拖拽提示
2. 表单内容区
   - 基础信息
   - 联系人与沟通
   - 业务信息
   - 财务与附加信息
3. 底部操作区
   - 取消
   - 保存
4. 四角缩放手柄

## 客户弹窗字段分区

### 基础信息

- 编码
- 名称
- 客户类别
- 状态

### 联系人与沟通

完全对齐供应商交互：

- 多个联系人卡片
- 每个联系人卡片包含：
  - 联系人姓名
  - 邮箱
  - 备注
  - 主联系人单选
- 每个联系人卡片下方提供一个“联系方式”输入框
- 输入后按回车或失焦，生成多个联系方式标签
- 标签支持：
  - 删除
  - 设为主号码

### 业务信息

- 默认结算方式
- 默认收款方式
- 运输方式
- 往来主体

### 财务与附加信息

- 地址
- 税号
- 开票抬头
- 银行
- 银行账号
- 信用额度
- 备注

## 数据映射设计

### 前端录入模型

前端内部使用与供应商相同的联系人分组模型：

- 联系人卡片
- 每个联系人下有多个联系方式标签
- 联系人可设置主联系人
- 联系方式可设置主号码

### 提交到后端的映射

继续兼容客户现有后端字段：

- `contacts`：存完整联系人数组
- `contact`：同步主联系人姓名
- `phone` / `mobile`：从主号码自动推导
- `email`：优先取表单主邮箱；如未填则取主联系人邮箱

号码录入不区分电话/手机：

- 前端只录“联系方式”
- 保存时自动判断：
  - `1xxxxxxxxxx` 识别为 `mobile`
  - 其他识别为 `phone`

### 老数据回填

若客户没有 `contacts` JSON，仅存在旧字段：

- `contact`
- `phone`
- `mobile`
- `email`

则打开编辑时自动组装一个默认联系人卡片：

- 姓名 = `contact`
- 主号码 = `mobile || phone`
- 邮箱 = `email`

保证旧数据可以无缝进入新交互。

## 搜索兼容设计

客户列表页保持现有列结构，不在本次改成单列“联系方式”。

但搜索需要兼容新数据结构：

- `contactQuery` 同时搜索：
  - `contact`
  - `contacts[].name`
- `phoneQuery` 同时搜索：
  - `phone`
  - `mobile`
  - `contacts[].phone`
  - `contacts[].mobile`

保证新老客户数据都能搜到。

## 样式与交互设计

优先复用供应商弹窗的视觉和行为模式：

- 对话框宽高和位置控制
- 顶部说明与标签
- 分区标题样式
- 可选区折叠
- 四角缩放
- 联系人卡片与标签样式

样式实现策略：

- 优先抽取或复用已有公共样式
- 避免简单复制整份供应商样式导致后续难维护

## 文件改动范围

### 新增

- [ErpCustomerEditDialog.vue](D:/project/auto-parts-wms-vue/src/components/ErpCustomerEditDialog.vue)

### 修改

- [ErpCustomerManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerManagement.vue)
- [table.css](D:/project/auto-parts-wms-vue/src/styles/table.css)
- 客户相关前端测试文件

## 测试策略

先补失败测试，再写实现。

### 前端测试

新增或修改客户页回归测试，锁定：

1. 客户页改为使用独立 `ErpCustomerEditDialog`
2. 客户弹窗具备供应商同类的拖拽/缩放骨架
3. 客户联系方式录入由表格行编辑改为标签输入
4. 客户搜索继续兼容 `contacts`

建议测试文件：

- `auto-parts-wms-vue/src/views/erp/__tests__/erpCustomerDialogRedesign.test.mjs`

### 验证命令

- `node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpCustomerDialogRedesign.test.mjs`
- `npm run type-check`（工作目录：`D:/project/auto-parts-wms-vue`）

## migration 结论

本次不涉及数据库结构、字段、索引、约束、初始化数据或 Flyway 迁移脚本改动。

结论：

- 当前无需扫描 migration 追加版本
- 不新增 migration 文件
- 不存在版本冲突

## 风险与控制

### 风险 1：客户旧数据回填异常

控制：

- 对旧字段构造单联系人默认卡片
- 用前端回归测试锁住

### 风险 2：客户保存后主联系人字段不同步

控制：

- 在弹窗提交前统一通过映射函数生成 `contact/phone/mobile/email/contacts`
- 用测试锁住 payload 结构

### 风险 3：客户搜索回归

控制：

- 搜索逻辑同时覆盖旧字段与 `contacts`
- 添加结构断言测试

## 实施顺序

1. 为客户弹窗改造补失败测试
2. 新增 `ErpCustomerEditDialog.vue`
3. 将客户页内联弹窗替换为独立组件
4. 实现联系方式标签交互与旧数据回填
5. 调整客户搜索兼容 `contacts`
6. 跑测试与类型检查
