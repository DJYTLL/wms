# 业务场景独立权限设计规范

## 目标

当一个业务单据在自己的流程中引用、选择、预览、带入、核销、冲销或联查其他业务对象时，权限应归属于“当前业务场景”，而不是归属于被引用对象本身。

这样可以避免不同单据的权限互相牵连，便于后续按角色精细分配能力。

## 核心原则

### 1. 权限归属当前业务场景

用户在 A 单据中操作 B 单据相关数据时，应校验 A 单据模块自己的独立权限。

例如：

| 当前场景 | 被引用对象 | 推荐权限 |
| --- | --- | --- |
| 销售退货选择/查看来源销售单 | 销售单 | `erp-sale-return-draft:source-access` |
| 采购退货选择/查看来源采购单 | 采购单 | `erp-purchase-return-draft:source-access` |
| 收款单选择/查看来源应收单 | 应收单 | `erp-receipt:source-access` |
| 付款单选择/查看来源应付单 | 应付单 | `erp-payment:source-access` |

不应因为用户拥有“销售单查看权限”就自动允许他在销售退货流程中查看来源销售单。

### 2. 不复用被引用模块的权限

跨单据场景中，不应直接复用被引用模块的主权限。

不推荐：

```java
@PreAuthorize("hasAuthority('PERM_erp-sale-approved:view')")
```

不推荐：

```java
@PreAuthorize("hasAnyAuthority('PERM_erp-sale-return-draft:source-access','PERM_erp-sale-approved:view')")
```

推荐：

```java
@PreAuthorize("@erpSaleReturnPermissionService.canAccessSourceSaleOrders()")
```

### 3. 不直接调用被引用模块的权限方法

当前模块不应调用被引用模块的权限 service。

不推荐：

```java
@PreAuthorize("@erpSaleOrderPermissionService.canViewApprovedSaleOrders()")
```

推荐：

```java
@PreAuthorize("@erpSaleReturnPermissionService.canAccessSourceSaleOrders()")
```

销售退货是否能访问来源销售单，应由销售退货模块自己的权限方法决定。

### 4. service/controller/API 都按当前场景建模

跨单据能力应优先放在当前业务模块自己的接口中。

推荐接口：

```text
GET /api/erp/sale-returns/source-sale-orders/page
GET /api/erp/sale-returns/source-sale-orders/{saleOrderId}
GET /api/erp/purchase-returns/source-purchase-orders/page
GET /api/erp/purchase-returns/source-purchase-orders/{purchaseOrderId}
```

不推荐在销售退货页面直接调用：

```text
GET /api/erp/sale-orders/approved/{id}
```

原因是后者属于销售单模块，会天然绑定销售单自己的权限模型，不利于销售退货场景单独授权。

## 权限命名建议

### 推荐通用后缀

优先使用：

```text
source-access
```

含义：允许当前业务场景访问来源单据或来源业务对象。

它比 `source-view` 更通用，因为很多场景不只是查看，还可能包含：

- 选择来源单
- 查询来源单列表
- 预览来源单详情
- 带入来源单明细
- 读取可用数量、余额、可核销金额
- 校验来源单占用状态

### 命名格式

```text
{当前业务模块}:{来源对象能力}
```

示例：

```text
erp-sale-return-draft:source-access
erp-purchase-return-draft:source-access
erp-receipt:source-access
erp-payment:source-access
```

如果同一模块有多类来源对象，可以细分：

```text
erp-receipt:source-ar-access
erp-receipt:source-sale-order-access
erp-payment:source-ap-access
erp-payment:source-purchase-order-access
```

如果确实只允许查看，不允许选择或带入，也可以使用：

```text
source-view
```

但新增业务默认优先考虑 `source-access`。

## 前端要求

前端所有入口显示、按钮禁用、弹窗打开、接口请求前置判断，都应使用当前业务场景自己的权限。

推荐：

```ts
const canAccessSourceDocuments = computed(() => (
  hasPermission('erp-sale-return-draft:source-access')
));
```

不推荐：

```ts
const canAccessSourceDocuments = computed(() => (
  hasPermission('erp-sale-return-draft:source-access')
  || hasPermission('erp-sale-approved:view')
));
```

没有当前场景来源权限时：

- 不显示或禁用来源单选择入口
- 不主动请求来源单列表、来源单详情、最近来源记录等接口
- 已有关联来源单号的只读展示可以通过当前单据详情接口返回必要摘要

## 后端要求

每个当前业务模块应建立自己的权限方法，集中表达该模块的跨单据能力。

示例：

```java
@Service("erpSaleReturnPermissionService")
public class ErpSaleReturnPermissionService {
    private static final String SOURCE_ACCESS = "PERM_erp-sale-return-draft:source-access";

    public boolean canAccessSourceSaleOrders() {
        return hasAuthority(SOURCE_ACCESS);
    }
}
```

controller 使用当前模块权限方法：

```java
@PreAuthorize("@erpSaleReturnPermissionService.canAccessSourceSaleOrders()")
@GetMapping("/source-sale-orders/{saleOrderId}")
public ResponseEntity<ApiResponse<SourceSaleOrderDetail>> getSourceSaleOrderDetail(@PathVariable Long saleOrderId) {
    return ResponseEntity.ok(ApiResponse.ok(service.getSourceSaleOrderDetail(saleOrderId)));
}
```

## 数据迁移要求

新增独立权限时，可以在 migration 中做一次性角色迁移，保证旧角色能力不突然丢失。

例如，历史上拥有销售退货草稿查看或销售单查看的角色，可以迁移获得销售退货来源访问权限：

```sql
WITH permission_map(old_code, new_code) AS (
    VALUES
        ('erp-sale-return-draft:view', 'erp-sale-return-draft:source-access'),
        ('erp-sale-approved:view', 'erp-sale-return-draft:source-access')
)
INSERT INTO app_role_permission (...)
SELECT ...
```

注意：迁移只用于平滑升级。运行时权限判断不能继续写成：

```text
当前场景 source-access OR 被引用模块 view
```

## 验收标准

新增或改造跨单据功能时，应满足：

1. 当前业务场景拥有自己的独立权限。
2. 前端入口只判断当前业务场景权限。
3. 后端接口只校验当前业务场景权限方法。
4. 不复用被引用单据的 `view`、`approved:view` 等主权限。
5. 不调用被引用单据模块的权限 service。
6. 当前场景有权限、被引用模块无权限时，跨单据功能可以正常使用。
7. 被引用模块有权限、当前场景无权限时，跨单据功能不能使用。
8. 只读详情需要展示来源单号时，应由当前单据详情返回必要摘要，避免额外请求被引用模块接口。

## 推荐描述模板

业务需求可以这样写：

```text
本功能涉及跨单据来源数据访问，权限需按当前业务场景独立控制。

在【当前单据/当前页面】中访问【来源单据/来源对象】时，不复用【来源单据模块】的查看权限，也不调用来源单据模块的权限方法。

请在【当前业务模块】中新增独立权限【权限编码】，并在当前模块自己的 controller/service/permission service 中完成权限校验。

前端按钮显示、弹窗打开和接口请求前置判断，也统一使用该独立权限。
```

示例：

```text
销售退货单选择来源销售单时，权限按销售退货业务场景独立控制。

不复用销售单已审核查看权限 erp-sale-approved:view，也不调用销售单模块的权限方法。

请在销售退货模块中使用 erp-sale-return-draft:source-access，并在销售退货自己的 controller/service/permission service 中完成权限校验。
```
