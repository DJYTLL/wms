# ERP 供应商导入与菜单权限初始化说明

## 1. 文档目的

本文档合并说明两件事：

1. 供应商扩展、供应商类型、往来主体、主体财务汇总相关的菜单与权限初始化落地情况
2. 供应商历史表导入到当前系统时的字段映射、缺失字段处理规则、推荐导入口径

本文档对应当前实现时间点：`2026-05-28`

---

## 2. 菜单与权限初始化落地情况

### 2.1 当前 migration 版本

- 当前已扫描最新 Flyway 版本：`V121__erp_counterparty_subject_finance_view.sql`
- 本次新增初始化迁移：`V122__seed_erp_supplier_counterparty_menu_permission.sql`
- 当前未发现版本冲突

### 2.2 本次新增菜单

新增菜单如下：

- `erp-supplier-type`
  - 标题：`供应商类型`
  - 路径：`/erp/supplier-types`
  - 父菜单：`erp-basic`
  - 访问权限：`erp-supplier-type:view`

- `erp-counterparty-subject`
  - 标题：`往来主体管理`
  - 路径：`/erp/counterparty-subjects`
  - 父菜单：`erp-basic`
  - 访问权限：`erp-counterparty-subject:view`

- `erp-finance-counterparty-subject`
  - 标题：`往来主体财务汇总`
  - 路径：`/erp/finance/counterparty-subjects`
  - 父菜单：`erp-finance`
  - 访问权限：`erp-finance-summary:view`

### 2.3 本次新增页面权限

新增页面权限如下：

- `erp-supplier-type:view`
- `erp-supplier-type:add`
- `erp-supplier-type:edit`
- `erp-supplier-type:delete`

- `erp-counterparty-subject:view`
- `erp-counterparty-subject:add`
- `erp-counterparty-subject:edit`
- `erp-counterparty-subject:delete`

说明：

- `往来主体财务汇总` 页面复用已有权限 `erp-finance-summary:view`
- 未新增单独的 `erp-finance-counterparty-subject:view`

### 2.4 本次新增列权限

#### 供应商页面新增列权限

- `column:erp-supplier:supplierTypeId`
- `column:erp-supplier:region`
- `column:erp-supplier:wechat`
- `column:erp-supplier:purchaser`
- `column:erp-supplier:businessScope`
- `column:erp-supplier:counterpartySubjectId`

#### 供应商类型页面列权限

- `column:erp-supplier-type:code`
- `column:erp-supplier-type:name`
- `column:erp-supplier-type:status`
- `column:erp-supplier-type:sort`
- `column:erp-supplier-type:remark`
- `column:erp-supplier-type:createdAt`
- `column:erp-supplier-type:updatedAt`

#### 往来主体页面列权限

- `column:erp-counterparty-subject:name`
- `column:erp-counterparty-subject:region`
- `column:erp-counterparty-subject:unifiedCreditCode`
- `column:erp-counterparty-subject:status`
- `column:erp-counterparty-subject:remark`
- `column:erp-counterparty-subject:createdAt`
- `column:erp-counterparty-subject:updatedAt`

#### 往来主体财务汇总页面列权限

- `column:erp-finance-counterparty-subject:subjectName`
- `column:erp-finance-counterparty-subject:customerCount`
- `column:erp-finance-counterparty-subject:supplierCount`
- `column:erp-finance-counterparty-subject:receivableTotal`
- `column:erp-finance-counterparty-subject:payableTotal`
- `column:erp-finance-counterparty-subject:netAmount`

### 2.5 角色授权回填规则

`V122` 中已包含以下回填策略：

- 所有 `admin`、`super_admin` 自动补齐本次新增页面权限和列权限
- 已拥有 `erp-supplier:view` 的角色，自动补 `erp-supplier-type:view`
- 已拥有 `erp-customer:view` 或 `erp-supplier:view` 的角色，自动补 `erp-counterparty-subject:view`
- 已拥有 `erp-finance-customer-debt:view` 或 `erp-finance-supplier-debt:view` 的角色，自动补 `erp-finance-summary:view`
- 通过页面查看权限，自动补齐对应页面的列权限

### 2.6 当前状态说明

代码层面已完成：

- Flyway 迁移脚本已新增
- `MenuSeedProvider` 已补新菜单
- `PermissionSeedProvider` 已补新权限与列权限
- 前端路由、页面、权限映射、文案已完成

当前未完成项：

- 未在当前环境中实际执行数据库迁移
- 未在当前环境中实际启动系统确认菜单已显示

原因：

- 当前执行环境仍存在前后端验证链路限制，暂未完成运行时验证

---

## 3. 供应商历史表导入口径

### 3.1 导入目标

目标不是机械把源表所有列原样塞进当前系统，而是按当前系统模型做结构化承接：

- 供应商主数据进入 `erp_supplier`
- 供应商类型进入 `erp_supplier_type`
- 往来主体后续按归并规则进入 `erp_counterparty_subject`
- 财务仍按客户/供应商分别记账，主体汇总由视图计算

### 3.2 源表字段清单

源表字段如下：

- 编码
- 名称
- 企业匹配
- 状态
- 默认结算方式
- 价格级别
- 区域
- 微信客服
- 客户类型
- 地址
- 备注
- 创建人
- 创建时间
- 采购员
- 联系方式
- 联系人
- 往来类别

---

## 4. 当前系统字段映射清单

### 4.1 直接映射字段

| 源字段 | 系统字段 | 说明 |
| --- | --- | --- |
| 编码 | `code` | 建议保留原编码 |
| 名称 | `name` | 供应商名称 |
| 默认结算方式 | `defaultSettlementMethodCode` | 需按“结算方式名称”匹配系统结算方式编码 |
| 区域 | `region` | 直接写入 |
| 微信客服 | `wechat` | 直接写入 |
| 地址 | `address` | 直接写入 |
| 备注 | `remark` | 直接写入 |
| 创建人 | `sourceCreatedBy` | 作为来源创建人保存 |
| 创建时间 | `sourceCreatedAt` | 作为来源创建时间保存 |
| 采购员 | `purchaser` | 直接写入 |
| 联系方式 | `contactInfo` | 原文完整保留 |
| 联系人 | `contact` | 直接写入 |
| 往来类别 | `businessScope` | 需做枚举转换 |

### 4.2 条件映射字段

| 源字段 | 系统字段 | 条件 |
| --- | --- | --- |
| 客户类型 | `supplierTypeId` | 当前业务语义应按“供应商类型”承接，需先维护基础资料并按名称匹配 |

### 4.3 暂不直接映射字段

| 源字段 | 当前处理 |
| --- | --- |
| 企业匹配 | 不直接入 `erp_supplier`，用于后续往来主体归并 |
| 价格级别 | 当前系统无对应正式字段，建议忽略或并入备注 |
| 状态 | 源数据多数为空，建议按默认规则推导，不建议原样空导 |

---

## 5. 字段转换规则

### 5.1 默认结算方式

源字段：

- `默认结算方式`

目标字段：

- `defaultSettlementMethodCode`

转换规则：

1. 按源数据文本与系统结算方式名称做精确匹配
2. 匹配成功后写入对应 `code`
3. 匹配失败时不建议写空并默默放过，应输出异常清单

建议异常口径：

- `默认结算方式` 未找到对应基础资料时，该行标记为待人工确认

### 5.2 往来类别

源字段：

- `往来类别`

目标字段：

- `businessScope`

推荐转换规则：

| 源值 | 系统值 |
| --- | --- |
| 供应商 | `SUPPLIER` |
| 既是客户又是供应商 | `CUSTOMER_SUPPLIER` |

兜底规则：

- 空值默认 `SUPPLIER`
- 非法值进入异常清单，不建议直接入库

### 5.3 客户类型

源字段：

- `客户类型`

目标字段：

- `supplierTypeId`

规则说明：

- 该列历史命名虽然叫“客户类型”，但当前供应商页面实际应解释为“供应商类型”
- 导入前需要先维护 `erp_supplier_type`
- 导入时按名称匹配 `erp_supplier_type.name`
- 匹配成功写入对应 `id`
- 匹配失败时进入异常清单

### 5.4 创建时间

源字段：

- `创建时间`

目标字段：

- `sourceCreatedAt`

支持格式：

- `yyyy-MM-dd`
- `yyyy-MM-dd HH:mm:ss`
- ISO 时间格式
- 毫秒时间戳

不建议做法：

- 不要把无法解析的时间默默置空

推荐做法：

- 解析失败进入异常清单，原因写明“来源创建时间格式不正确”

### 5.5 联系方式

源字段：

- `联系方式`

目标字段：

- `contactInfo`

推荐增强处理：

1. 原始文本完整保留到 `contactInfo`
2. 若能识别第一个手机号，可同步提取到 `mobile`
3. 若能识别第一个座机/短号，可同步提取到 `phone`

说明：

- 这是增强规则，不是强制规则
- 如果导入流程暂时不做号码拆分，至少应完整保留到 `contactInfo`

---

## 6. 缺失字段处理规则

### 6.1 企业匹配

当前系统状态：

- `erp_supplier` 没有“企业匹配”字段
- 但系统已具备“往来主体”模型，可用于后续归并

推荐处理：

- 首轮供应商导入时，不直接写入供应商表
- 将其作为后续主体归并依据保留在导入中间表、Excel 结果表或人工处理清单中

原因：

- 一个往来主体可以挂多个客户和多个供应商
- 如果按供应商名称自动生成往来主体，会把“主体归并”做成“一供应商一主体”，与设计目标冲突

导入口径：

- 首轮 `counterpartySubjectId = null`
- 第二轮依据 `企业匹配`、客户档案、供应商档案做主体归并绑定

### 6.2 价格级别

当前系统状态：

- 当前供应商模型无“价格级别”正式字段

推荐处理方式二选一：

1. 严格口径：忽略，不导入
2. 兼容口径：拼接进 `remark`

推荐选择：

- 严格口径优先

原因：

- 该字段没有稳定落点，直接写备注会污染业务备注

### 6.3 状态

当前系统状态：

- 供应商当前状态主要由 `enabled` 与 `blacklisted` 表示
- 你给的样例里该列基本为空

推荐默认规则：

- 空值 → `enabled = true`
- 空值 → `blacklisted = false`

如后续源数据存在明确状态值，可按以下规则扩展：

| 源值 | enabled | blacklisted |
| --- | --- | --- |
| 启用 | `true` | `false` |
| 停用 | `false` | `false` |
| 黑名单 | `false` | `true` |

---

## 7. 推荐导入口径

### 7.1 必导字段

- `code`
- `name`

### 7.2 强建议导入字段

- `defaultSettlementMethodCode`
- `region`
- `wechat`
- `address`
- `remark`
- `sourceCreatedBy`
- `sourceCreatedAt`
- `purchaser`
- `contactInfo`
- `contact`
- `businessScope`

### 7.3 条件导入字段

- `supplierTypeId`

前提：

- 供应商类型基础资料已先维护完成

### 7.4 暂不直接导入字段

- `counterpartySubjectId`
- `企业匹配`
- `价格级别`

---

## 8. 导入前置条件

正式导入前建议先满足以下条件：

1. 系统已执行到 `V122`
2. 供应商类型基础资料已维护
3. 默认结算方式基础资料已核对完整
4. 导入程序具备异常清单输出能力
5. 首轮导入不自动创建往来主体

---

## 9. 推荐异常清单字段

建议导入失败或待确认记录输出以下信息：

| 字段 | 用途 |
| --- | --- |
| 源行号 | 定位原始数据 |
| 编码 | 对账 |
| 名称 | 对账 |
| 异常字段 | 明确哪一列出问题 |
| 异常原因 | 例如“默认结算方式未匹配”“来源创建时间格式不正确” |
| 原始值 | 方便人工修正 |
| 建议处理 | 例如“补基础资料后重试” |

---

## 10. 结论

### 菜单权限侧

代码层面已完成：

- 菜单种子
- 权限种子
- 现有库回填迁移
- 前端菜单翻译

当前未完成：

- 运行环境中的实际迁移执行与菜单显示验证

### 导入侧

当前系统已经可以较完整承接以下供应商历史字段：

- 编码
- 名称
- 默认结算方式
- 区域
- 微信客服
- 地址
- 备注
- 创建人
- 创建时间
- 采购员
- 联系方式
- 联系人
- 往来类别
- 供应商类型

当前不建议在首轮供应商导入中直接落库的字段：

- 企业匹配
- 价格级别
- 往来主体绑定结果

原因不是系统做不到，而是这些字段需要放到“主体归并”第二阶段处理，才能保证模型正确。
