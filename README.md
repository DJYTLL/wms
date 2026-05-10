# WMS 项目说明书

## 1. 项目概述

本仓库是一个前后端分离的汽配仓储/进销存系统，包含：

- 前端：`auto-parts-wms-vue`
  - 基于 `Vue 3 + Vite + TypeScript + Pinia + Vue Router + Element Plus`
  - 负责登录、菜单展示、权限控制、业务表单、列表、打印页面
- 后端：`wms-backend`
  - 基于 `Spring Boot 3 + Spring Security + JWT + MyBatis-Plus + Flyway + PostgreSQL`
  - 负责认证鉴权、租户隔离、菜单权限、ERP/WMS 业务接口、数据库迁移、审计日志

系统目前覆盖以下业务域：

- 系统管理：租户、用户、角色、权限、菜单、系统配置、审计日志
- 基础资料：仓库、货架、商品、供应商、分类、单位
- ERP 基础资料：商品、车型适配、客户、客户类别、供应商、仓库、库位、分类、单位、结算方式、付款方式、送货方式、打印模板
- 采购管理：采购单、采购退货单
- 销售管理：销售单、销售退货单
- 库存管理：库存台账、库存流水、盘点、初始库存、库存预警、组装单、拆分单
- 财务管理：应收、应付、收款、付款、客户欠款、供应商欠款

## 1.1 近期业务变更

### 全仓逻辑删除收口

- 后端系统管理与 ERP 业务域的运行期删除路径已统一收口为逻辑删除，角色、权限、菜单、租户菜单、用户角色、角色权限、幂等记录，以及 ERP 主数据、单据头、单据明细、收付款分摊、应收应付、盘点等对象不再直接物理删除。
- 新增数据库迁移 `V55__logical_delete_support.sql` 与 `V56__logical_delete_relations_and_finance.sql`，统一补充 `deleted_at`、活动态唯一索引，并把平台关联表从复合主键模型调整为支持逻辑删除的代理主键模型。
- 业务查询默认排除已删除数据，客户欠款、供应商欠款、库存预警、收付款详情、历史成交、租户菜单、角色权限等联表查询已补 `deleted_at IS NULL` 过滤，避免墓碑数据继续参与展示、汇总和唯一性占用。
- 验证方式：在 `D:\project\wms-backend` 执行 `mvn -q -DskipTests compile`，并复查运行期 Java 代码中的 `deleteById`、`mapper.delete(...)`、`@Delete`、`DELETE FROM` 路径，确认业务删除语义均落到逻辑删除。

### 删除治理与审计补强

- 所有后端删除接口现在统一要求提交 `DeleteRequest.reason`，并通过审计切面把删除原因写入审计日志明细；删除人继续使用现有审计日志 `actor` 字段记录。
- ERP 删除接口已与系统管理删除接口统一，不再允许“无原因删除”；同时明确不提供前端恢复入口，也不暴露后端 `restore` 风格接口。
- 新增实体公共基类 `entity/base/*`，把主键、审计字段、逻辑删除字段、租户字段收敛到统一模型，新的业务实体默认复用这些基类。
- 新增删除治理回归测试，覆盖删除原因上下文、删除接口必须带 `DeleteRequest`、控制器不得暴露恢复路由，以及核心逻辑删除实体必须复用公共基类。

### 进销存仓库/库位主数据治理补强

- 仓库、库位删除增加引用保护，若已被库存、默认主数据或业务单据引用，将禁止物理删除。
- 仓库、库位停用增加约束，若仍被未完成单据、默认仓库或默认库位使用，将禁止停用。
- 仓库编码、库位编码统一做标准化处理：提交时会去空格、转大写，并限制为字母、数字、短横线、下划线和斜杠。
- 库位 `aisle`、`rack`、`bin` 字段在新增和编辑时会真实持久化，不再丢失。
- 新增业务选项接口 `/api/erp/warehouses/options` 与 `/api/erp/locations/options`，新建和编辑业务单据时默认只返回启用中的仓库和库位。
- 采购、采购退货、销售、销售退货、组装、拆分、商品默认仓库/库位、盘点等页面在编辑历史单据时，允许回显已停用但仍存在的仓库/库位，避免旧数据打开后丢失当前值。
- 仓库管理页和库位管理页的删除提示已明确“已使用主数据应优先停用”，避免误把删除当作常规退出路径。

## 2. 仓库总览

### 2.1 根目录

| 路径 | 说明 |
| --- | --- |
| `.gitignore` | 根仓库忽略规则，排除前端 `node_modules/dist`、后端 `target`、IDE 目录等本地文件 |
| `auto-parts-wms-vue/` | 前端项目 |
| `wms-backend/` | 后端项目 |
| `.vscode/` | 当前工作区编辑器配置目录，不属于业务代码 |

## 3. 运行与部署

### 3.1 环境要求

- Node.js：`20.19+` 或 `22.12+`
- npm：与 Node 配套版本
- Java：`17`
- Maven：3.9+
- PostgreSQL：建议 `14+`

### 3.2 前端启动

```powershell
cd D:\project\auto-parts-wms-vue
npm install
npm run dev
```

默认由 Vite 启动开发服务，接口通过 `/api` 访问后端。

### 3.3 后端启动

```powershell
cd D:\project\wms-backend
mvn spring-boot:run
```

默认端口为 `8080`。

### 3.4 数据库配置

后端默认连接：

- 数据库：`wms_backend`
- 用户名：`postgres`
- 密码：`123456`

配置文件位置：

- `wms-backend/src/main/resources/application.properties`

### 3.5 默认账号

系统启动后，`DataInitializer` 会确保默认租户和默认管理员存在：

- 租户编码：`default`
- 用户名：`admin`
- 默认密码：`password`

说明：

- 旧版 `wms-backend/README.md` 中写的是 `admin123`，但当前实际配置文件中默认密码是 `password`
- 以 `application.properties` 与 `DataInitializer.java` 为准

### 3.6 租户模型说明

系统当前区分两类租户：

- 系统租户
  - 默认租户编码为 `default`
  - 由 `app.tenant.code` 配置决定，默认值也是 `default`
  - 这是唯一允许存在 `super_admin` 角色的租户
  - 系统级平台能力，例如租户管理、系统配置、跨租户切换、全局菜单管理，只能由该租户内的 `super_admin` 承担
- 普通租户
  - 由 `super_admin` 通过租户管理创建
  - 每个普通租户内部都有自己的 `admin` 角色和租户管理员账号
  - 普通租户不允许保留或分配 `super_admin` 角色
  - 普通租户只能管理本租户内的数据、用户、角色和业务配置

角色边界如下：

- `super_admin`
  - 仅存在于系统租户
  - 是整个系统的超级管理员
  - 可以执行跨租户操作
  - 可以访问 `tenant:*`、`system-config:*` 等平台权限
- `admin`
  - 每个租户都有自己的 `admin`
  - 仅在当前租户内生效
  - 负责本租户内的用户、角色、列配置、业务配置和业务数据管理
  - 不能跨租户，也不能持有平台级权限

登录规则如下：

- 登录必须同时提供 `tenantCode`、`username`、`password`
- 用户名按“租户内唯一”处理，不再要求全局唯一
- 相同用户名可以存在于不同租户，但必须配合正确的租户编码登录
- 跨租户切换只对 `super_admin` 开放，切换后仍保留系统级管理员身份

## 4. 系统操作流程

### 4.1 登录流程

1. 用户在前端登录页输入租户编码、用户名、密码。
2. 前端调用 `POST /api/login`。
3. 后端 `AuthController` 校验租户、用户名、密码。
4. 认证成功后生成 JWT 访问令牌，并通过 HttpOnly Cookie 下发刷新令牌。
5. 前端把访问令牌存到 `localStorage`。
6. 后续请求由 `request.ts` 自动在请求头加 `Authorization: Bearer <token>`。
7. 如果访问令牌过期，前端自动调用 `/api/refresh` 刷新后重试原请求。

### 4.2 权限控制流程

1. JWT 中带有用户、角色、权限、租户信息。
2. 前端 `auth.ts` 解析 JWT，把权限和角色放入 Pinia。
3. 路由守卫 `router/index.ts` 检查：
   - 是否已登录
   - 是否具备角色
   - 是否具备页面权限
4. 后端 `SecurityConfig` 对 API 做统一认证拦截。
5. 具体业务接口再结合服务层和权限数据实现数据隔离与功能控制。

### 4.3 菜单加载流程

1. 系统启动时 `MenuSeedProvider` 提供菜单种子。
2. `DataInitializer` 确保菜单、租户菜单映射、默认角色权限存在。
3. 用户登录后，前端根据用户权限渲染可访问菜单与页面。

### 4.4 业务单据流转

大多数 ERP 单据遵循下面的通用模式：

1. 进入管理页面查看列表。
2. 新增或编辑草稿单据。
3. 保存明细项、往来单位、金额、仓库/库位等信息。
4. 审核后触发库存、应收应付、流水等联动。
5. 可进入详情页查看结果。
6. 可进入打印页输出单据。

适用对象：

- 采购单、采购退货单
- 销售单、销售退货单
- 收款单、付款单
- 盘点单、初始库存单
- 组装单、拆分单

## 5. 前端说明 `auto-parts-wms-vue`

### 5.1 目录结构

| 路径 | 说明 |
| --- | --- |
| `.github/` | GitHub 工作流或仓库辅助配置目录 |
| `.vscode/` | 前端项目本地编辑器配置 |
| `public/` | 静态资源目录 |
| `src/` | 前端业务源码目录 |
| `dist/` | 前端构建产物目录，不提交业务逻辑 |
| `node_modules/` | npm 依赖目录，不属于源码 |

### 5.2 前端根目录文件

| 文件 | 说明 |
| --- | --- |
| `.editorconfig` | 统一编辑器格式规则 |
| `.gitattributes` | Git 行尾与文本属性配置 |
| `.gitignore` | 前端项目忽略规则 |
| `.prettierrc.json` | Prettier 格式化配置 |
| `package.json` | 前端依赖、脚本、运行入口配置 |
| `package-lock.json` | npm 锁定文件，保证依赖版本稳定 |
| `tsconfig.json` | TypeScript 总配置 |
| `tsconfig.app.json` | 应用代码 TS 配置 |
| `tsconfig.node.json` | Node/Vite 工具侧 TS 配置 |
| `vite.config.ts` | Vite 构建与开发服务器配置 |
| `eslint.config.ts` | ESLint 校验规则 |
| `env.d.ts` | Vite 环境变量类型声明 |
| `index.html` | Vite 前端入口 HTML |
| `README.md` | Vue 模板自带说明，参考价值较低 |
| `readme1.md` | 项目内额外说明文档 |
| `api.md` | 接口说明草稿或手工记录 |
| `DEVELOPMENT_GUIDE.md` | 前端开发规范说明 |
| `MODULE_DEV_TEMPLATE.md` | 模块开发模板 |
| `NEW_PAGE_TEMPLATE.md` | 新页面开发模板 |
| `SYSTEM_PAGE_LAYOUT_SPEC.md` | 系统页面布局规范 |
| `qa_log_zh.md` | 中文 QA/测试记录 |
| `gemini.md` | AI 协作或辅助说明文档 |
| `auto-imports.d.ts` | 自动导入生成的类型声明 |
| `components.d.ts` | 自动注册组件生成的类型声明 |

### 5.3 `public/`

| 文件 | 说明 |
| --- | --- |
| `favicon.ico` | 网站图标 |

### 5.4 `src/` 顶层文件

| 文件 | 说明 |
| --- | --- |
| `main.ts` | 前端应用入口，挂载 Vue、Pinia、路由等 |
| `App.vue` | 根组件 |
| `i18n.ts` | 国际化初始化入口 |

### 5.5 `src` 子目录说明

| 路径 | 说明 |
| --- | --- |
| `components/` | 可复用业务组件 |
| `composables/` | 组合式函数，封装公共状态和逻辑 |
| `directives/` | 自定义指令 |
| `layouts/` | 页面布局组件 |
| `locales/` | 国际化词条 |
| `router/` | 路由配置与守卫 |
| `stores/` | Pinia 状态管理 |
| `styles/` | 全局样式 |
| `utils/` | 工具函数、请求封装、打印辅助 |
| `views/` | 业务页面 |

### 5.6 `src/components/`

| 文件 | 说明 |
| --- | --- |
| `DecimalInput.vue` | 金额/数量等小数输入组件 |
| `FuzzyProductSelect.vue` | 商品模糊搜索选择组件 |
| `PrintPreviewDialog.vue` | 打印预览弹窗组件 |
| `SearchableSelect.vue` | 可搜索下拉选择组件 |
| `TableColumnSettings.vue` | 表格列显示配置组件 |

### 5.7 `src/composables/`

| 文件 | 说明 |
| --- | --- |
| `useApiError.ts` | API 错误处理封装 |
| `useColumnSettings.ts` | 表格列配置读取/保存逻辑 |
| `useSystemConfig.ts` | 系统配置读取逻辑 |

### 5.8 `src/directives/`

| 文件 | 说明 |
| --- | --- |
| `permission.ts` | 权限指令，根据权限显示或隐藏元素 |

### 5.9 `src/layouts/`

| 文件 | 说明 |
| --- | --- |
| `MainLayout.vue` | 主体布局，承载菜单、标签页、内容区等 |

### 5.10 `src/locales/`

| 文件 | 说明 |
| --- | --- |
| `en.ts` | 英文语言包 |
| `zh.ts` | 中文语言包 |

### 5.11 `src/router/`

| 文件 | 说明 |
| --- | --- |
| `index.ts` | 前端全部路由定义、页面权限元数据、登录守卫、页面标题处理 |

### 5.12 `src/stores/`

| 文件 | 说明 |
| --- | --- |
| `auth.ts` | 登录状态、JWT 解析、权限/角色判断、登录登出逻辑 |
| `counter.ts` | 示例或基础计数状态 |
| `menu.ts` | 菜单状态管理 |
| `mockData.ts` | 模拟数据 |
| `theme.ts` | 主题状态管理 |

### 5.13 `src/styles/`

| 文件 | 说明 |
| --- | --- |
| `table.css` | 表格相关全局样式 |

### 5.14 `src/utils/`

| 文件 | 说明 |
| --- | --- |
| `request.ts` | Axios 封装，负责统一 baseURL、JWT、401 刷新、幂等键注入 |
| `csv.ts` | CSV 导出/解析等工具 |
| `i18n.ts` | 国际化工具函数 |
| `qzTray.ts` | QZ Tray 打印支持工具 |

### 5.15 `src/views/` 页面说明

#### 通用页面

| 文件 | 说明 |
| --- | --- |
| `HomeView.vue` | 首页/仪表盘 |
| `LoginView.vue` | 登录页 |
| `AboutView.vue` | 关于页面 |

#### `src/views/basic/`

| 文件 | 说明 |
| --- | --- |
| `WarehouseManagement.vue` | 基础仓库管理 |
| `ShelfManagement.vue` | 货架管理 |
| `ProductManagement.vue` | 基础商品管理 |
| `SupplierManagement.vue` | 基础供应商管理 |
| `CategoryManagement.vue` | 基础分类管理 |
| `UnitManagement.vue` | 基础计量单位管理 |

#### `src/views/system/`

| 文件 | 说明 |
| --- | --- |
| `UserManagement.vue` | 用户管理 |
| `RoleManagement.vue` | 角色与权限管理 |
| `PermissionManagement.vue` | 权限定义管理 |
| `AuditLogManagement.vue` | 审计日志查询 |
| `ColumnPermissionManagement.vue` | 列级权限配置 |
| `MenuManagement.vue` | 菜单管理 |
| `SystemConfigManagement.vue` | 系统配置管理 |
| `TenantManagement.vue` | 租户管理 |

#### `src/views/warehouse/`

| 文件 | 说明 |
| --- | --- |
| `InboundManagement.vue` | 入库管理页面 |

#### `src/views/erp/`

ERP 页面较多，但命名规则非常统一，基本可以通过文件名判断用途：

- `...Management.vue`：列表/管理页
- `...Form.vue`：新增/编辑表单页
- `...Detail.vue`：详情页
- `...Print.vue`：打印页
- `...Preview*.vue`：打印或单据预览实验页
- `Draft/Approved`：按状态拆分的列表页

具体文件如下：

| 文件 | 说明 |
| --- | --- |
| `ErpProductManagement.vue` | ERP 商品管理 |
| `ErpVehicleFitmentManagement.vue` | 车型适配管理 |
| `ErpCustomerManagement.vue` | 客户管理 |
| `ErpCustomerCategoryManagement.vue` | 客户类别管理 |
| `ErpSupplierManagement.vue` | 供应商管理 |
| `ErpWarehouseManagement.vue` | ERP 仓库管理 |
| `ErpLocationManagement.vue` | ERP 库位管理 |
| `ErpCategoryManagement.vue` | ERP 分类管理 |
| `ErpUnitManagement.vue` | ERP 单位管理 |
| `ErpSettlementMethodManagement.vue` | 结算方式管理 |
| `ErpPaymentMethodManagement.vue` | 付款方式管理 |
| `ErpDeliveryMethodManagement.vue` | 送货方式管理 |
| `ErpPrintTemplateManagement.vue` | 打印模板管理 |
| `ErpPurchaseOrderManagement.vue` | 采购单管理总页 |
| `ErpPurchaseOrderDraft.vue` | 采购单草稿列表 |
| `ErpPurchaseOrderApproved.vue` | 采购单已审核列表 |
| `ErpPurchaseOrderForm.vue` | 采购单新增/编辑 |
| `ErpPurchaseOrderPrint.vue` | 采购单打印 |
| `ErpPurchaseReturnManagement.vue` | 采购退货列表 |
| `ErpPurchaseReturnForm.vue` | 采购退货新增/编辑 |
| `ErpPurchaseReturnPrint.vue` | 采购退货打印 |
| `ErpSaleOrderManagement.vue` | 销售单管理 |
| `ErpSaleOrderForm.vue` | 销售单新增/编辑 |
| `ErpSaleOrderPrint.vue` | 销售单打印 |
| `ErpSaleOrderFormPreview.vue` | 销售单预览方案 1 |
| `ErpSaleOrderFormPreviewAlt.vue` | 销售单预览方案 2 |
| `ErpSaleOrderFormPreviewPaper.vue` | 销售单纸质风格预览 |
| `ErpSaleReturnManagement.vue` | 销售退货管理 |
| `ErpSaleReturnForm.vue` | 销售退货新增/编辑 |
| `ErpSaleReturnPrint.vue` | 销售退货打印 |
| `ErpStockManagement.vue` | 库存台账 |
| `ErpStockTxnManagement.vue` | 库存流水 |
| `ErpStockCountManagement.vue` | 盘点与初始库存通用管理页 |
| `ErpStockCountPrint.vue` | 盘点单打印 |
| `ErpStockInitPrint.vue` | 初始库存打印 |
| `ErpStockWarningManagement.vue` | 库存预警 |
| `ErpAssemblyOrderManagement.vue` | 组装单管理 |
| `ErpAssemblyOrderForm.vue` | 组装单新增/编辑 |
| `ErpDisassembleOrderManagement.vue` | 拆分单管理 |
| `ErpDisassembleOrderForm.vue` | 拆分单新增/编辑 |
| `ErpAccountsReceivableManagement.vue` | 应收管理 |
| `ErpAccountsReceivableDetail.vue` | 应收详情 |
| `ErpAccountsReceivablePrint.vue` | 应收打印 |
| `ErpAccountsPayableManagement.vue` | 应付管理 |
| `ErpAccountsPayableDetail.vue` | 应付详情 |
| `ErpAccountsPayablePrint.vue` | 应付打印 |
| `ErpReceiptManagement.vue` | 收款单管理 |
| `ErpReceiptForm.vue` | 收款单新增/编辑 |
| `ErpReceiptDetail.vue` | 收款单详情 |
| `ErpReceiptPrint.vue` | 收款单打印 |
| `ErpPaymentManagement.vue` | 付款单管理 |
| `ErpPaymentForm.vue` | 付款单新增/编辑 |
| `ErpPaymentDetail.vue` | 付款单详情 |
| `ErpPaymentPrint.vue` | 付款单打印 |
| `ErpCustomerDebtManagement.vue` | 客户欠款查询 |
| `ErpSupplierDebtManagement.vue` | 供应商欠款查询 |
| `ErpFinanceSummary.vue` | 财务汇总或财务概览 |

## 6. 后端说明 `wms-backend`

### 6.1 目录结构

| 路径 | 说明 |
| --- | --- |
| `.github/` | GitHub 仓库辅助配置 |
| `.idea/` | IntelliJ IDEA 本地配置 |
| `.vscode/` | VS Code 本地配置 |
| `docs/` | SQL 模板等文档资料 |
| `scripts/` | 数据库备份与恢复脚本 |
| `src/` | 后端源码 |
| `target/` | Maven 构建产物，不属于源码 |

### 6.2 后端根目录文件

| 文件 | 说明 |
| --- | --- |
| `pom.xml` | Maven 项目定义，声明 Spring Boot、Security、Flyway、PostgreSQL、MyBatis-Plus 等依赖 |
| `README.md` | 旧版后端简要说明 |
| `API.md` | 接口说明草稿 |
| `ERP_INTEGRATION.md` | ERP 相关集成说明 |
| `DEVELOPMENT_GUIDE.md` | 后端开发指南 |
| `DEVELOPMENT_GUIDE copy.md` | 开发指南副本 |
| `NEW_PAGE_TEMPLATE.md` | 新页面/模块设计模板 |
| `wms-backend-pg.session.sql` | PostgreSQL 会话或手工操作 SQL 记录 |

### 6.3 `docs/`

| 文件 | 说明 |
| --- | --- |
| `Flyway_Template.sql` | Flyway 迁移脚本模板 |

### 6.4 `scripts/`

| 文件 | 说明 |
| --- | --- |
| `pg_backup.ps1` | PostgreSQL 备份脚本 |
| `pg_restore.ps1` | PostgreSQL 恢复脚本 |

### 6.5 `src/main/resources/`

| 文件/目录 | 说明 |
| --- | --- |
| `application.properties` | 后端主配置，包含端口、JWT、数据库、Flyway、监控阈值等 |
| `schema.sql` | 数据库结构参考文件 |
| `db/migration/` | Flyway 数据库迁移脚本目录 |

#### `db/migration/` 命名规则

- `V1__...sql` 到 `V52__...sql`
- 每个文件代表一次数据库结构或初始化数据变更
- 名称直接体现变更主题，例如：
  - `V1__init_schema.sql`：初始化表结构
  - `V8__seed_erp_column_permissions_and_menu.sql`：初始化 ERP 列权限与菜单
  - `V25__erp_stock_count.sql`：库存盘点相关结构
  - `V44__erp_print_template.sql`：打印模板表结构
  - `V47__erp_assembly_order.sql`：组装单结构
  - `V51__erp_vehicle_fitment.sql`：车型适配结构
  - `V52__erp_vehicle_fitment_seed.sql`：车型适配初始化数据

说明：

- 这些文件都属于“数据库演进历史”
- 如果要追某个业务字段或表从何时加入，应从这里开始查

### 6.6 `src/test/`

| 文件/目录 | 说明 |
| --- | --- |
| `src/test/resources/application-test.properties` | 测试环境配置 |
| `src/test/java/com/example/wms/WmsBackendApplicationTests.java` | 应用启动测试 |
| `src/test/java/com/example/wms/AuthPermissionIntegrationTests.java` | 认证与权限集成测试 |

### 6.7 `src/main/java/com/example/wms/` 包结构

| 目录 | 说明 |
| --- | --- |
| `aop/` | AOP 切面，如审计日志切面 |
| `audit/` | 请求审计上下文 |
| `config/` | Spring 配置、初始化、菜单/权限种子 |
| `controller/` | REST API 入口层 |
| `dto/` | 请求响应对象 |
| `entity/` | 数据库实体模型 |
| `exception/` | 全局异常与业务异常 |
| `mapper/` | MyBatis Mapper，数据库访问层 |
| `monitor/` | 慢请求、慢 SQL、幂等等监控组件 |
| `mybatis/` | MyBatis 扩展，例如 JSONB 类型处理 |
| `security/` | JWT 与安全认证组件 |
| `service/` | 业务接口层 |
| `tenant/` | 租户上下文 |
| `WmsBackendApplication.java` | Spring Boot 启动入口 |

### 6.8 关键基础文件

| 文件 | 说明 |
| --- | --- |
| `WmsBackendApplication.java` | 应用启动类 |
| `config/SecurityConfig.java` | Spring Security 配置，定义放行接口、JWT 过滤器、401/403 行为 |
| `config/DataInitializer.java` | 启动时初始化默认租户、管理员、菜单、权限、角色、系统配置 |
| `config/MenuSeedProvider.java` | 系统菜单种子定义 |
| `config/PermissionSeedProvider.java` | 系统权限种子定义 |
| `config/MybatisPlusConfig.java` | MyBatis-Plus 配置 |
| `config/OpenApiConfig.java` | Swagger/OpenAPI 配置 |
| `config/WebMvcConfig.java` | Web MVC 相关配置 |
| `controller/AuthController.java` | 登录、刷新、登出接口 |
| `controller/HealthController.java` | 健康检查接口 |
| `security/JwtAuthenticationFilter.java` | 解析 JWT 并写入认证上下文 |
| `security/JwtTokenService.java` | JWT 生成、解析、校验 |
| `audit/RequestAuditContextFilter.java` | 请求级审计上下文采集 |
| `aop/AuditLogAspect.java` | 审计日志 AOP 切面 |
| `monitor/IdempotencyInterceptor.java` | 幂等控制 |
| `monitor/RequestTimingFilter.java` | 请求耗时监控 |
| `monitor/SlowQueryInterceptor.java` | 慢 SQL 监控 |
| `tenant/TenantContext.java` | 当前请求租户上下文存储 |
| `mybatis/JsonbTypeHandler.java` | PostgreSQL JSONB 类型处理器 |
| `exception/GlobalExceptionHandler.java` | 全局异常转换 |
| `exception/NotFoundException.java` | 资源不存在异常 |
| `exception/DuplicateRequestException.java` | 重复请求异常 |

### 6.9 系统管理模块文件规则

#### `entity/`

| 文件 | 说明 |
| --- | --- |
| `UserAccount.java` | 用户实体 |
| `Role.java` | 角色实体 |
| `Permission.java` | 权限实体 |
| `Menu.java` | 菜单实体 |
| `Tenant.java` | 租户实体 |
| `TenantMenu.java` | 租户与菜单映射 |
| `TenantColumnSetting.java` | 租户列配置 |
| `SystemConfig.java` | 系统配置项 |
| `RefreshToken.java` | 刷新令牌实体 |
| `AuditLog.java` | 审计日志实体 |
| `IdempotencyRecord.java` | 幂等请求记录实体 |

#### `controller/`

| 文件 | 说明 |
| --- | --- |
| `UserController.java` | 用户管理接口 |
| `RoleController.java` | 角色管理接口 |
| `PermissionController.java` | 权限管理接口 |
| `MenuController.java` | 菜单管理接口 |
| `TenantController.java` | 租户管理接口 |
| `TenantColumnSettingController.java` | 租户列设置接口 |
| `SystemConfigController.java` | 系统配置接口 |
| `AuditLogController.java` | 审计日志接口 |
| `AuthController.java` | 登录/登出/刷新接口 |
| `HealthController.java` | 健康检查接口 |

#### `service/`、`service/impl/`、`mapper/`、`dto/`

系统管理模块遵循统一命名：

- `XxxService.java`：业务接口
- `XxxServiceImpl.java`：业务实现
- `XxxMapper.java`：数据库访问
- `XxxCreateRequest / XxxUpdateRequest / XxxResponse`：接口 DTO

已存在的典型文件包括：

- 用户：`UserService*`、`UserAccountService`、`UserAccountMapper`、`UserCreateRequest`、`UserUpdateRequest`、`UserResponse`
- 角色：`RoleService*`、`RoleMapper`、`RoleCreateRequest`、`RoleUpdateRequest`、`RoleOptionResponse`
- 权限：`PermissionService*`、`PermissionMapper`、`PermissionCreateRequest`、`PermissionUpdateRequest`
- 菜单：`MenuService*`、`MenuMapper`、`MenuCreateRequest`、`MenuUpdateRequest`、`MenuResponse`
- 租户：`TenantService*`、`TenantMapper`、`TenantCreateRequest`、`TenantUpdateRequest`、`TenantResponse`
- 系统配置：`SystemConfigService*`、`SystemConfigMapper`、`SystemConfigRequest`、`SystemConfigResponse`
- 刷新令牌：`RefreshTokenService*`、`RefreshTokenMapper`、`RefreshTokenRequest`
- 审计：`AuditLogService*`、`AuditLogMapper`、`AuditLogResponse`

### 6.10 ERP 模块文件规则

ERP 代码数量最多，但组织非常标准，按业务对象横向展开。

#### 统一模式

每个业务对象通常会有以下文件：

- `entity/erp/Xxx.java`：数据库实体
- `controller/erp/XxxController.java`：对外接口
- `service/erp/XxxService.java`：业务接口
- `service/erp/impl/XxxServiceImpl.java`：业务实现
- `mapper/erp/XxxMapper.java`：数据库访问
- `dto/erp/XxxCreateRequest.java`：新增请求
- `dto/erp/XxxUpdateRequest.java`：修改请求
- `dto/erp/XxxDetail.java / XxxView.java / XxxItemRequest.java`：详情、列表视图、明细项对象

#### 已覆盖的业务对象

| 业务对象前缀 | 说明 |
| --- | --- |
| `ErpProduct` | ERP 商品 |
| `ErpProductPrice` | 商品价格 |
| `ErpProductFitment` | 车型适配 |
| `ErpCustomer` | 客户 |
| `ErpCustomerCategory` | 客户类别 |
| `ErpSupplier` | 供应商 |
| `ErpWarehouse` | 仓库 |
| `ErpLocation` | 库位 |
| `ErpCategory` | 分类 |
| `ErpUnit` | 单位 |
| `ErpSettlementMethod` | 结算方式 |
| `ErpPaymentMethod` | 付款方式 |
| `ErpDeliveryMethod` | 送货方式 |
| `ErpPrintTemplate` | 打印模板 |
| `ErpPrintLog` | 打印日志 |
| `ErpPurchaseOrder` | 采购单 |
| `ErpPurchaseOrderItem` | 采购单明细 |
| `ErpPurchaseReturn` | 采购退货单 |
| `ErpPurchaseReturnItem` | 采购退货明细 |
| `ErpSaleOrder` | 销售单 |
| `ErpSaleOrderItem` | 销售单明细 |
| `ErpSaleReturn` | 销售退货单 |
| `ErpSaleReturnItem` | 销售退货明细 |
| `ErpReceipt` | 收款单 |
| `ErpReceiptReceivable` | 收款分摊应收关系 |
| `ErpPayment` | 付款单 |
| `ErpPaymentPayable` | 付款分摊应付关系 |
| `ErpAccountsReceivable` | 应收 |
| `ErpAccountsPayable` | 应付 |
| `ErpStockBalance` | 库存结存 |
| `ErpStockTxn` | 库存流水 |
| `ErpStockCount` | 盘点/初始库存单 |
| `ErpStockCountItem` | 盘点明细 |
| `ErpStockWarning` | 库存预警视图对象 |
| `ErpAssemblyOrder` | 组装单 |
| `ErpAssemblyOrderItem` | 组装明细 |
| `ErpOrderSequence` | 单号序列 |
| `ErpFinanceSummary` | 财务汇总 DTO |
| `ErpVehicleBrand` | 车型品牌 |
| `ErpVehicleSeries` | 车型车系 |
| `ErpVehicleModel` | 车型型号 |

说明：

- 这些对象的文件名基本一一对应，查找非常直接
- 比如要看“采购退货”的完整后端链路，就查：
  - `entity/erp/ErpPurchaseReturn*.java`
  - `dto/erp/ErpPurchaseReturn*.java`
  - `mapper/erp/ErpPurchaseReturn*.java`
  - `service/erp/ErpPurchaseReturnService.java`
  - `service/erp/impl/ErpPurchaseReturnServiceImpl.java`
  - `controller/erp/ErpPurchaseReturnController.java`

### 6.11 一个特殊文件

| 文件 | 说明 |
| --- | --- |
| `src/views/erp/ErpVehicleFitmentManagement.vue` | 这个 Vue 文件出现在后端目录中，属于异常放置文件，理论上应放在前端项目中 |

## 7. 前后端协作关系

### 7.1 请求链路

1. 前端页面在 `views/` 中发起操作。
2. 通过 `utils/request.ts` 调用后端 `/api`。
3. 后端 `controller` 接口接收请求。
4. `service` 处理业务逻辑。
5. `mapper` 访问 PostgreSQL。
6. `entity` 与 `dto` 负责数据承载。
7. Flyway 保证数据库结构与代码同步。

### 7.2 权限与菜单联动

1. 权限种子由后端维护。
2. 菜单种子由后端维护。
3. 租户菜单映射和角色权限关系由启动初始化与管理页面共同维护。
4. 前端路由定义了页面权限元数据。
5. 最终形成“后端授权 + 前端可见性控制”的双层机制。

## 8. 开发维护建议

- 优先以根目录本说明书为总入口。
- 新增业务模块时，前端与后端都应保持现有命名规律。
- 新增数据库字段或表时，必须补充 `db/migration` 脚本，不要直接改线上库。
- 新增页面时，前端应同步补充路由、菜单、权限码。
- 新增接口时，后端应同步补充 DTO、Service、Mapper、Controller。
- 新增业务表默认包含 `deleted_at`，业务删除统一走逻辑删除；如存在唯一键冲突风险，优先使用活动态唯一索引。
- 面向租户的外部查询表默认补 `(tenant_id, deleted_at)` 组合索引，并在自定义 SQL 中显式过滤 `deleted_at IS NULL`。
- 技术性状态表与追加型流水表要单独建模：刷新令牌优先用撤销/过期字段，审计日志与库存流水默认只追加不做恢复接口。
- 建议后续把旧的零散文档合并，避免多份 README 内容不一致。

## 9. 业务完成收口规范

### 9.1 完成标准

一个业务完成时，必须同时完成以下动作：

- 代码已完成与本次改动相关的基础自测或最小校验。
- 只暂存本次业务相关文件，禁止直接使用 `git add .` 收口提交。
- 根目录 `README.md` 已补充本次业务记录。
- 提交信息使用中文动宾短句，例如：`完善仓库接口分页校验`、`修复库位接口空指针问题`。
- 一个业务对应一个提交；如果同时处理多个业务，必须拆分为多个提交。

### 9.2 提交流程

推荐按以下顺序完成业务收口：

1. 完成一个业务后，先自行验证本次改动。
2. 使用 `git add -p` 或显式文件路径，只暂存本次业务代码和根 `README.md`。
3. 执行统一收口脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\finish-task.ps1 -Message "修复库位接口空指针问题"
```

脚本会执行以下检查：

- 仅读取已暂存文件作为本次提交范围。
- 检查是否已暂存根 `README.md`。
- 检查提交信息是否为中文单行业务描述。
- 根据暂存范围执行最小校验：
  - 仅前端改动：执行前端静态检查与类型检查。
  - 仅后端改动：执行后端编译检查。
  - 前后端同时改动：分别执行对应检查。
- 校验通过后，仅提交当前已暂存内容。

如需跳过校验，可显式使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\finish-task.ps1 -Message "补充接口变更说明" -SkipCheck
```

### 9.3 补充约束

- 提交前优先使用 `git add -p`，不要默认 `git add .`。
- 自动生成文件默认不提交，除非本次业务确实需要：
  - 前端 `dist/`
  - 后端 `target/`
  - 前端类型生成文件 `auto-imports.d.ts`、`components.d.ts`
- `README` 记录不能只写“做了什么”，还必须写清楚影响范围和验证方式。
- 禁止使用模糊提交信息，例如：`修改一下`、`更新代码`、`保存当前改动`、`临时提交`。
- 文档型业务允许单独提交，但提交信息仍需清晰描述本次文档变更。

### 9.4 轻提醒钩子

仓库提供了非阻断式 `commit-msg` 提醒脚本，用于在绕过统一收口脚本时提示风险：

- 提交信息不是中文业务描述时提醒。
- 本次提交未包含根 `README.md` 时提醒。
- 一次暂存文件过多时提醒检查是否混入其他业务。

启用方式：

```powershell
git config core.hooksPath tools/git-hooks
```

### 9.5 给 Codex 的标准任务模板

如需让 Codex 在完成业务后自动按本规范收口，可直接在任务中附带以下模板：

```text
请完成这个业务，并在结束时按仓库收口规范自行执行：
- 更新 D:\project\README.md 中的业务变更记录
- 只暂存本次业务相关文件，不要混入其他未完成改动
- 提交信息使用中文动宾短句
- 使用 D:\project\tools\finish-task.ps1 完成提交
- 如果检查失败，先修复问题再提交，并说明原因
```

如果你已经确定本次提交文案，也可以直接写死：

```text
请实现这个需求。完成后不要停在代码修改：
- 补充根 README.md 的业务变更记录
- 只暂存本次业务修改的文件
- 不要带上其他未完成业务的改动
- 提交信息用中文动宾短句，内容为：修复仓库分页查询异常
- 执行 tools/finish-task.ps1 完成本次提交
```

使用建议：

- 如果当前工作区还有其他未提交改动，任务中要明确写“不要混入其他未完成改动”。
- 如果本次业务只允许文档提交，也要在任务里明确说明是“纯文档变更”。
- 如果需要跳过检查，必须在任务里显式说明允许使用 `-SkipCheck`，不要默认跳过。

### 9.6 业务变更记录模板

每次业务完成并提交时，请在根目录 `README.md` 追加一条记录，建议使用以下模板：

```md
### YYYY-MM-DD 业务名称或模块

- 本次变更：一句话概述本次完成的业务内容
- 影响范围：列出受影响的前端页面、后端接口、数据库或权限点
- 验证方式：说明本次提交完成后如何验证
```

### 9.7 业务变更记录

### 2026-05-10 全仓逻辑删除收口

- 本次变更：将后端系统管理与 ERP 全业务域的运行期删除语义统一改为逻辑删除，并补齐已删除数据的默认过滤。
- 影响范围：`README.md`、`wms-backend/src/main/resources/db/migration/V55__logical_delete_support.sql`、`wms-backend/src/main/resources/db/migration/V56__logical_delete_relations_and_finance.sql`，以及后端系统管理/ERP 实体、Mapper、部分服务实现。
- 验证方式：在 `D:\project\wms-backend` 执行 `mvn -q -DskipTests compile`，并复查运行期代码中保留的 `deleteById`/`mapper.delete(...)` 调用目标均已接入 `@TableLogic`。

### 2026-05-10 完善删除治理规范

- 本次变更：补齐 ERP 删除接口的删除原因入参与审计联动，新增逻辑删除治理测试，并把公共实体基类、逻辑删除建模、索引规范和“禁止恢复接口”要求固化到文档。
- 影响范围：`README.md`、`wms-backend/docs/Flyway_Template.sql`、`wms-backend/src/main/java/com/example/wms/controller/erp/*`、`wms-backend/src/main/java/com/example/wms/entity/base/*`、`wms-backend/src/test/java/com/example/wms/LogicalDeleteGovernanceTests.java`。
- 验证方式：在 `D:\project\wms-backend` 执行 `mvn -q -DskipTests compile`，并执行 `mvn -q -Dtest=LogicalDeleteGovernanceTests test` 校验删除接口约束和实体基类约束。

### 2026-05-10 删除元数据与前端约束补强

- 本次变更：新增 `V57__logical_delete_metadata_and_indexes.sql`，给逻辑删除核心表补 `deleted_by`、`delete_reason` 和 `(tenant_id, deleted_at)` 组合索引，并把默认项唯一索引改成活动态唯一索引；后端补 `AuditMetaObjectHandler` 自动填充删除人和删除原因；前端在 `request.delete` 统一要求填写删除原因，审计日志页增加删除原因可视化列。
- 影响范围：`README.md`、`wms-backend/src/main/resources/db/migration/V57__logical_delete_metadata_and_indexes.sql`、`wms-backend/src/main/java/com/example/wms/config/AuditMetaObjectHandler.java`、`wms-backend/src/main/java/com/example/wms/security/CurrentActor.java`、部分 ERP 实体、`auto-parts-wms-vue/src/utils/request.ts`、`auto-parts-wms-vue/src/views/system/AuditLogManagement.vue`、前端中英文语言包，以及后端逻辑删除回归测试。
- 验证方式：在 `D:\project\wms-backend` 执行 `mvn -q -DskipTests compile` 和 `mvn -q -Dtest=LogicalDeleteGovernanceTests,AuditMetaObjectHandlerTests,ErpMasterDataGovernanceTests test`；前端构建在当前环境因审批额度限制未执行。

## 10. 快速定位指南

- 查登录认证：`auto-parts-wms-vue/src/stores/auth.ts`、`auto-parts-wms-vue/src/utils/request.ts`、`wms-backend/src/main/java/com/example/wms/controller/AuthController.java`
- 查路由权限：`auto-parts-wms-vue/src/router/index.ts`
- 查系统初始化：`wms-backend/src/main/java/com/example/wms/config/DataInitializer.java`
- 查菜单定义：`wms-backend/src/main/java/com/example/wms/config/MenuSeedProvider.java`
- 查数据库变更：`wms-backend/src/main/resources/db/migration/`
- 查某个 ERP 业务：按 `Erp业务对象名` 在 `controller/dto/entity/service/mapper` 五层同时搜索
