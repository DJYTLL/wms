# ERP 供应商导入与菜单权限初始化说明

## 1. 文档目的

本文档合并说明两件事：

1. 供应商扩展、供应商类型、往来主体、主体财务汇总相关的菜单与权限初始化落地情况
2. 供应商历史表导入到当前系统时的字段映射、缺失字段处理规则、推荐导入口径

本文档对应当前实现时间点：`2026-05-29`

---

## 2. 菜单与权限初始化落地情况

### 2.1 当前 migration 版本

- 当前已扫描最新 Flyway 版本：`V125__seed_erp_supplier_type_uncategorized.sql`
- 本次新增初始化迁移：
  - `V122__seed_erp_supplier_counterparty_menu_permission.sql`
  - `V123__erp_counterparty_import_and_customer_subject.sql`
  - `V124__seed_erp_counterparty_runtime_permissions.sql`
  - `V125__seed_erp_supplier_type_uncategorized.sql`
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

当前仍待最终确认项：

- 需在真实业务环境执行 `V125` 并确认现有租户均已生成内置“未分类”供应商类型
- 需在已登录运行环境做一次完整菜单可见性与权限树人工验收

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

当前实现口径：

- `默认结算方式` 未找到对应基础资料时，该行标记失败
- 导入结果会统计“未匹配结算方式数”，方便人工回补基础资料后重导

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
- 导入时按名称匹配 `erp_supplier_type.name`
- 匹配成功写入对应 `id`
- 匹配失败时优先落到系统内置类型：
  - `code = UNCATEGORIZED`
  - `name = 未分类`
- 导入结果会提示：`供应商类型未匹配，已落到未分类`

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

1. 系统已执行到 `V125`
2. 已确认存在系统内置供应商类型 `未分类`
3. 默认结算方式基础资料已核对完整
4. 导入程序具备异常清单与导入批次结果输出能力
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

## 10. 当前系统字段映射清单 + 缺失字段处理规则

### 10.1 当前系统字段映射清单

| 源字段 | 当前系统字段 | 导入口径 |
| --- | --- | --- |
| 编码 | `erp_supplier.code` | 必填，保留原编码 |
| 名称 | `erp_supplier.name` | 必填 |
| 默认结算方式 | `erp_supplier.defaultSettlementMethodCode` | 按结算方式名称匹配系统编码 |
| 区域 | `erp_supplier.region` | 直接导入 |
| 微信客服 | `erp_supplier.wechat` | 直接导入 |
| 客户类型 | `erp_supplier.supplierTypeId` | 作为“供应商类型”按名称匹配 |
| 地址 | `erp_supplier.address` | 直接导入 |
| 备注 | `erp_supplier.remark` | 直接导入 |
| 创建人 | `erp_supplier.sourceCreatedBy` | 作为来源创建人保存 |
| 创建时间 | `erp_supplier.sourceCreatedAt` | 支持 `yyyy-MM-dd` / `yyyy-MM-dd HH:mm:ss` / ISO / 毫秒时间戳 |
| 采购员 | `erp_supplier.purchaser` | 直接导入 |
| 联系方式 | `erp_supplier.contactInfo` | 原文保留，并尽量拆首个手机/座机 |
| 联系人 | `erp_supplier.contact` | 直接导入 |
| 往来类别 | `erp_supplier.businessScope` | `供应商 -> SUPPLIER`，`既是客户又是供应商 -> CUSTOMER_SUPPLIER` |

### 10.2 缺失字段处理规则

| 源字段 | 当前处理规则 |
| --- | --- |
| 企业匹配 | 不直接入供应商表，进入导入明细/异常清单，留给第二阶段主体归并 |
| 状态 | 为空时默认 `enabled=true`、`blacklisted=false` |
| 价格级别 | 当前无正式字段，首轮不导入，必要时人工并入备注 |
| 往来主体 | 首轮不自动创建，不自动绑定，导入后在“往来主体管理”中人工归并 |

### 10.3 导入异常输出口径

导入批次和明细建议至少输出以下字段：

- 源行号
- 编码
- 名称
- 异常字段
- 异常原因
- 原始值
- 建议处理

当前实现已新增以下承接表：

- `erp_supplier_import_batch`
- `erp_supplier_import_item`

用于保存首轮供应商历史导入日志与异常清单。

当前实现还补充了以下统计字段：

- `uncategorizedCount`
- `settlementUnmatchedCount`
- `pendingSubjectMergeCount`

当前实现还补充了以下导入明细辅助字段：

- `warningMessage`
- `matchedStrategy`

## 11. 结论

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

---

## 12. 往来主体业务补充规则

### 12.1 主体解绑约束

往来主体下已绑定的客户或供应商，在以下任一场景存在时，不允许解绑：

- 存在未完成销售单
- 存在未完成采购单
- 存在未完成销售退货单
- 存在未完成采购退货单
- 存在未完成收款单
- 存在未完成付款单
- 存在未完成应收
- 存在未完成应付

其中“未完成”的统一业务口径为：

- 单据状态属于 `DRAFT / APPROVED / OPEN / SETTLED`
- 应收应付状态不为 `RED_FLUSHED`，且 `unpaid_amount != 0`

前端交互要求：

- 用户点击解绑时，先弹出校验结果弹窗
- 若存在阻塞业务，弹窗中必须逐项列出阻塞原因
- 若阻塞业务包含单据，必须展示单号，并允许直接点击跳转至对应单据页面处理

示例：

- 未完成销售单
  - `SO202605290001`
  - `SO202605290008`
- 未完成应收
  - 未收金额合计：`1200`

### 12.2 主体改绑约束

“改绑”是指把某个客户或供应商从主体 A 调整到主体 B。

改绑时执行与解绑相同的校验规则：

- 只要该客户/供应商名下仍存在未完成业务，就不允许改绑
- 改绑失败时返回明确错误原因

改绑后的财务原则：

- 只调整档案归属
- 不回写、不重算历史财务单据归属
- 历史应收应付、收付款、销售采购单据仍保持原有业务关系

目的：

- 避免主体之间财务口径被切乱
- 保证往来主体汇总与应收应付台账始终一致

### 12.3 往来类别口径

往来类别统一使用以下 3 个业务值：

| 业务值 | 含义 |
| --- | --- |
| `CUSTOMER` | 仅客户 |
| `SUPPLIER` | 仅供应商 |
| `CUSTOMER_SUPPLIER` | 既是客户又是供应商 |

页面展示口径：

- 客户页：展示 `CUSTOMER`、`CUSTOMER_SUPPLIER`
- 供应商页：展示 `SUPPLIER`、`CUSTOMER_SUPPLIER`
- 往来主体页/财务汇总页：可展示全部，并支持按往来类别筛选

导入口径：

- `供应商` -> `SUPPLIER`
- `既是客户又是供应商` -> `CUSTOMER_SUPPLIER`
- 空值默认 `SUPPLIER`
- 后续若客户历史导入涉及“仅客户”，则写入 `CUSTOMER`

### 12.4 供应商类型导入口径

源表中的“客户类型”，在当前供应商导入场景下按“供应商类型”解释。

推荐导入规则：

- 先按名称匹配系统中的 `erp_supplier_type`
- 匹配成功：写入 `supplierTypeId`
- 匹配失败：不终止整批导入，当前记录落入“未分类”或进入导入结果提示

当前业务建议：

- 首轮历史导入优先采用“未分类”兜底
- 导入结果中明确提示“供应商类型未匹配，已落到未分类”

### 12.5 导入幂等与重复策略

供应商历史导入统一按以下策略执行：

- 同编码不同名称：
  - 认定为同一供应商
  - 允许更新名称
  - 在导入结果中标记“名称已更新”

- 无编码但名称相同：
  - 不自动覆盖
  - 进入人工确认清单

- 联系人/联系方式变更：
  - 允许更新
  - 在导入批次结果中保留变更痕迹

总体原则：

- 编码优先识别主体身份
- 名称仅作辅助判断
- 联系信息属于可更新字段

### 12.6 财务汇总口径

往来主体财务汇总必须遵守以下原则：

- 只统计最终有效财务结果
- 草稿不进入最终汇总
- 红冲后按净结果统计
- 作废/删除记录不应导致主体财务出现重复或残留金额
- 往来主体汇总结果必须与应收应付页面最终结果一致

一致性要求：

- 不能出现往来主体汇总为 `1000`，而应收应付页为 `0` 的不一致情况
- 红冲、退货、退款、核销后的最终结果，应在主体汇总与应收应付中保持同口径

### 12.7 当前导入结果闭环

当前系统已支持在供应商页直接完成以下操作：

1. 粘贴 Markdown 表格并发起导入
2. 查看导入批次列表
3. 查看单批次导入明细
4. 查看以下统计结果：
   - 成功数
   - 失败数
   - 未分类数
   - 未匹配结算方式数
   - 待人工归并主体数
5. 查看每行导入提示：
   - `warningMessage`
   - `matchedStrategy`
   - `errorMessage`
   - `suggestion`

当前 `matchedStrategy` 口径：

- `CODE_UPSERT`
  - 新编码新增
- `CODE_UPDATE`
  - 同编码更新

当前 `warningMessage` 口径：

- `供应商类型未匹配，已落到未分类`
- `名称已更新`
- `联系方式已更新`
- 若同时命中多个提示，则按 `；` 拼接返回

### 12.8 往来主体人工归并 SOP

首轮导入完成后，建议业务或财务按以下顺序人工归并主体：

1. 先看 `企业匹配`
   - 若同一批历史数据中多个供应商明显属于同一公司主体，优先作为强提示
2. 再看名称
   - 判断是否存在简称、门店名、档口名、仓位名与公司主体名混用
3. 再看联系人、联系方式、区域、地址
   - 若多个档案联系人、电话、地址高度一致，通常属于同一往来主体
4. 判断是否应归并到同一主体
   - 能确认属于同一公司时，再执行绑定或改绑
5. 若系统提示存在未完成业务
   - 先点开阻塞单号处理未完成单据
   - 待采购、销售、收付款、应收应付处理完成后，再改绑
6. 改绑完成后复核
   - 到“往来主体财务汇总”确认净额口径与应收应付一致
   - 到客户页、供应商页确认挂靠主体已更新

---

## 13. 当前业务还需完善与建议优先级

本次改造已完成供应商扩展、往来主体、主体财务汇总、菜单权限接入、解绑/改绑基础约束与导入口径整理，但从长期稳定运行角度，仍建议继续补齐以下事项。

### 13.1 P1：改绑前结构化校验接口

当前状态：

- 往来主体页面的“解绑”已经支持结构化校验与阻塞单据跳转
- 客户页、供应商页已新增改绑前校验接口
- 客户页、供应商页保存前已接入结构化阻塞弹窗
- 返回结构已与解绑校验一致：
  - `allowed`
  - `blockingReasons`
  - `pendingDocs`

结论：

- `13.1` 已完成

### 13.2 P1：未完成业务状态口径固化

当前状态：

- 已抽出统一口径常量：
  - `BLOCKING_DOCUMENT_STATUSES = DRAFT / APPROVED / OPEN / SETTLED`
  - `RED_FLUSHED_STATUS = RED_FLUSHED`
- 客户、供应商、往来主体、财务汇总相关服务已统一复用该口径
- 本文档已同步口径说明

结论：

- `13.2` 已完成

### 13.3 P1：主体财务汇总与应收应付对账测试

当前状态：

- 已补服务层回归测试，锁住至少以下口径：
  - 主体财务汇总查询走租户隔离视图
  - 无财务单据主体返回 0 值
  - 汇总聚合排除 `RED_FLUSHED`

当前仍建议后续继续补的深层场景：

- 核销、退货冲减后的完整净额链路集成测试
- 红冲后的视图与台账全链路一致性测试

结论：

- `13.3` 已完成第一阶段，本轮已达成基础回归保护

### 13.4 P2：导入结果可视化与人工处理闭环

当前状态：

- 已有 `erp_supplier_import_batch`、`erp_supplier_import_item`
- 已补后端查询接口：
  - `GET /api/erp/suppliers/import-batches`
  - `GET /api/erp/suppliers/import-batches/{id}/items`
- 已补供应商页内导入弹窗与导入结果抽屉
- 当前已支持查看：
  - 成功数
  - 失败数
  - 未分类数
  - 未匹配结算方式数
  - 待人工归并主体数
  - 行级提示 / 异常 / 建议处理 / 识别策略

当前尚未完成的增强项：

- 按异常类型筛选
- 导出异常明细 Excel

结论：

- `13.4` 已完成主闭环，增强项可后续追加

### 13.5 P2：往来主体人工归并操作指引

当前状态：

- 已在本文档新增 `12.8 往来主体人工归并 SOP`

结论：

- `13.5` 已完成

### 13.6 P2：供应商类型“未分类”治理

当前状态：

- 已新增 `V125__seed_erp_supplier_type_uncategorized.sql`
- 已确保系统初始化补种内置供应商类型：
  - `code = UNCATEGORIZED`
  - `name = 未分类`
- 已限制该类型不允许删除
- 导入类型未匹配时，已自动落到“未分类”并输出 warning

当前尚未完成的增强项：

- 单独的“未分类供应商列表”快捷筛选或治理页面

结论：

- `13.6` 已完成主治理闭环

### 13.7 P3：导入重复识别策略增强

当前状态：

- 当前已明确“编码优先、名称辅助、联系方式可更新”
- 本轮已补：
  - 同编码不同名称时输出 `名称已更新`
  - 联系方式变更时输出 `联系方式已更新`
  - 识别策略输出 `CODE_UPSERT / CODE_UPDATE`

当前尚未完成的增强项：

- 无编码同名的疑似重复识别
- 联系人/区域/地址的相似度提示

结论：

- `13.7` 已完成第一阶段

### 13.8 P3：菜单与权限运行态回归

当前状态：

- 代码、seed、迁移已补齐
- 当前已完成：
  - 后端编译
  - 新增后端定向测试
  - 前端静态测试待本轮收尾后再次执行

当前仍待完成：

- 在真实运行环境执行 migration 后，做一次已登录态人工页面验收：
  - 菜单是否可见
  - 角色授权树是否可见
  - 权限管理页是否可见
  - 列权限配置页是否可见
  - 页面是否落入“未映射页面”

结论：

- `13.8` 仍保留为运行环境验收项

### 13.9 建议执行顺序

建议优先按以下顺序推进：

1. 在真实环境执行 `V125`
2. 用已登录账号验证菜单、权限树、列权限树、页面映射
3. 若需要，再补“未分类供应商列表”与导入异常筛选/导出
4. 再继续补深层财务链路一致性集成测试
