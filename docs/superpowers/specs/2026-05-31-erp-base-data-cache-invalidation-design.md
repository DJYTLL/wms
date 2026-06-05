# ERP 主数据本地缓存定向失效设计

**目标**

为 ERP 主数据管理页补齐本地缓存刷新机制。当用户对仓库、商品等主数据执行新增、修改、删除并成功后，前端必须刷新对应资源的本地缓存，但只允许失效当前被修改的数据类型，不连带清理其他主数据缓存。

**背景**

当前前端在 [erpBaseDataCache.ts](D:/project/auto-parts-wms-vue/src/composables/erpBaseDataCache.ts) 中维护了按租户隔离的主数据缓存。业务录单页、筛选器和部分弹窗会复用这些缓存，减少重复请求。

现状问题是：

- 缓存读取已经统一，但写操作成功后的失效逻辑没有统一接入
- 多个主数据管理页在新增、编辑、删除成功后只刷新当前列表，没有同步失效共享缓存
- 结果是其他依赖缓存的页面在同一租户下可能继续读到旧的商品、仓库、库位、客户或结算方式数据

**范围**

本次覆盖所有已经接入 `erpBaseDataCache` 的 ERP 主数据资源：

- 客户 `customers`
- 供应商 `suppliers`
- 商品分类 `categories`
- 客户分类 `customer-categories`
- 单位 `units`
- 结算方式 `settlement-methods`
- 付款方式 `payment-methods`
- 收款方式 `receipt-methods`
- 配送方式 `delivery-methods`
- 仓库 `warehouses`
- 库位 `locations`
- 商品选项 `products-options`
- 车型品牌 `vehicle-brands`
- 车型车系 `vehicle-series`
- 车型型号 `vehicle-models`

明确不在本次范围内：

- 没有接入 `erpBaseDataCache` 的页面级临时状态
- 登录态、菜单、列表列设置、本地表格状态等非 ERP 主数据缓存
- 后端 Redis、数据库或服务端缓存

**设计原则**

- 只失效当前资源，不清空全部主数据缓存
- 失效粒度以“缓存资源键”为准，而不是以页面名为准
- 一个主数据实体若在缓存层映射到多份缓存，则允许清理该实体自己的关联缓存，但不得扩散到其他实体
- 继续保持按租户隔离；失效时只影响当前租户
- 页面侧写操作成功后显式调用，不做隐式 URL 猜测

**方案对比**

方案一：每个管理页直接访问缓存底层 key 并手动失效。

- 优点：实现快
- 缺点：资源 key 分散在页面里，后续容易漏改，也不利于测试

方案二：在缓存层暴露按资源类型的失效方法，由页面在写成功后调用。

- 优点：资源映射集中，页面调用简单，最容易保证“只清自己”
- 缺点：需要先整理所有资源与页面的对应关系

方案三：在请求层根据 URL 自动识别主数据接口并失效缓存。

- 优点：页面改动最少
- 缺点：隐式逻辑重，像仓库、库位、结算方式这种一类资源对应多份缓存时，规则容易变脆

推荐采用方案二。

**缓存资源映射**

缓存层需要从“只有全量失效”调整为“支持定向失效”。建议保留现有全量失效能力，同时新增面向资源类型的失效 API。

建议映射如下：

- 客户变更：失效 `customers`
- 供应商变更：失效 `suppliers`
- 商品分类变更：失效 `categories`
- 客户分类变更：失效 `customer-categories`
- 单位变更：失效 `units`
- 结算方式变更：失效 `settlement-methods` 与 `settlement-methods-enabled`
- 付款方式变更：失效 `payment-methods` 与 `payment-methods-enabled`
- 收款方式变更：失效 `receipt-methods` 与 `receipt-methods-enabled`
- 配送方式变更：失效 `delivery-methods` 与 `delivery-methods-enabled`
- 仓库变更：失效 `warehouses` 与 `warehouses-options`
- 库位变更：失效 `locations` 与 `locations-options`
- 商品变更：失效 `products-options`
- 车型品牌变更：失效 `vehicle-brands`
- 车型车系变更：失效 `vehicle-series`
- 车型型号变更：失效 `vehicle-models`

这里“仓库变更清两份缓存”“结算方式变更清普通与 enabled 两份缓存”属于同一资源类型下的最小必要范围，不算误清其他种类。

**页面接入方式**

各主数据管理页在 `post`、`put`、`delete` 成功后，执行当前资源对应的缓存失效，再继续保留页面现有的列表刷新、成功提示和弹窗关闭逻辑。

接入页面包括但不限于：

- [ErpCustomerManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerManagement.vue)
- [ErpSupplierManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpSupplierManagement.vue)
- [ErpCategoryManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpCategoryManagement.vue)
- [ErpCustomerCategoryManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerCategoryManagement.vue)
- [ErpUnitManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpUnitManagement.vue)
- [ErpSettlementMethodManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpSettlementMethodManagement.vue)
- [ErpPaymentMethodManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpPaymentMethodManagement.vue)
- [ErpReceiptMethodManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpReceiptMethodManagement.vue)
- [ErpDeliveryMethodManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpDeliveryMethodManagement.vue)
- [ErpWarehouseManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpWarehouseManagement.vue)
- [ErpLocationManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpLocationManagement.vue)
- [ErpProductManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpProductManagement.vue)
- [ErpVehicleFitmentManagement.vue](D:/project/auto-parts-wms-vue/src/views/erp/ErpVehicleFitmentManagement.vue)

如果某些页面同时维护多类主数据，则只在对应操作成功后调用对应的失效函数，不共用“全清”入口。

**接口与代码组织**

缓存层建议新增两层能力：

1. 面向内部的“按单个资源 key 失效”能力
2. 面向业务页的“按主数据类型失效”能力

这样页面侧无需了解 `warehouses-options`、`payment-methods-enabled` 这类内部 key，只需要调用如“失效仓库缓存”“失效付款方式缓存”之类的语义化 API。

为避免后续继续漏接，资源类型枚举和值得失效的缓存 key 应集中放在同一文件维护。

**错误处理**

- 只有写接口成功后才失效缓存；请求失败不清缓存
- 缓存失效本身应为同步轻量操作，不应阻断成功提示和列表刷新
- 若页面没有拿到租户键，则继续沿用当前缓存层的租户参数获取方式；不引入跨租户清理

**测试策略**

先补失败测试，再改实现。测试重点：

- 缓存层单测：
  - 失效客户时只清 `customers`
  - 失效仓库时清 `warehouses` 与 `warehouses-options`，但不清 `locations`
  - 失效结算方式时清 `settlement-methods` 与 `settlement-methods-enabled`，但不清 `payment-methods`
- 页面接入测试：
  - 商品新增/编辑/删除成功后会调用商品缓存失效
  - 仓库新增/编辑/删除成功后会调用仓库缓存失效
  - 库位新增/编辑/删除成功后会调用库位缓存失效
  - 至少覆盖一个“无关资源不应被调用”的反例

优先增加前端单测，锁住“只清当前资源”的行为边界。

**风险与兼容性**

- 当前工作区已有大量未提交改动，实现时必须避免覆盖无关文件中的用户变更
- 某些页面可能未统一通过同一个成功提交流程保存数据，接入时需要分别核对保存与删除入口
- 如果未来把更多主数据接入 `erpBaseDataCache`，必须同步把资源映射补到同一处，而不是再次在页面里临时拼 key

**验收标准**

- 所有接入 `erpBaseDataCache` 的主数据管理页，在新增、修改、删除成功后都能失效自身对应缓存
- 商品变更不会清仓库缓存，仓库变更不会清商品缓存
- 仓库、库位、结算方式、付款方式、收款方式、配送方式这类“一类资源对应多份缓存”的场景，能正确清理该类资源自身的关联缓存
- 相关测试和类型检查通过
