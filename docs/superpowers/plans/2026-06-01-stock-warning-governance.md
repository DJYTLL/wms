# 库存预警与异常数据治理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将库存预警页升级为“仓库级预警 + 异常数据治理”的主工作台，支持仓库筛选、策略来源展示、异常排查导出、补货/调拨联动，并统一安全库存/最低库存/最高库存口径。

**Architecture:** 后端在现有 `erp-stock-warnings` 查询口径上继续扩展，新增异常分页与导出接口，并在服务层集中收口异常判定；前端继续复用现有库存预警页，不新增菜单页面，通过列表增强和抽屉承载异常治理；测试继续采用后端 SpringBoot 服务测试和前端 Node 文本/行为回归测试双线锁定。

**Tech Stack:** Spring Boot, MyBatis Mapper SQL, Vue 3 + TypeScript + Element Plus, Node `--test`, Maven Surefire

---

## 文件结构

### 后端核心文件

- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpStockWarningView.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/mapper/erp/ErpStockWarningMapper.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpStockWarningService.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpStockWarningServiceImpl.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpStockWarningController.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpStockWarningAnomalyView.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpStockWarningAnomalyExportRow.java`

### 前端核心文件

- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpStockWarningManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/locales/zh.ts`
- Modify: `D:/project/auto-parts-wms-vue/src/locales/en.ts`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpPurchaseOrderForm.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpStockTransferManagement.vue`

### 测试文件

- Modify: `D:/project/wms-backend/src/test/java/com/example/wms/ErpStockWarningServiceTests.java`
- Create: `D:/project/wms-backend/src/test/java/com/example/wms/ErpStockWarningAnomalyServiceTests.java`
- Create: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpStockWarningFiltersAndAnomalies.test.mjs`
- Create: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpStockWarningActionsContext.test.mjs`

## Task 1: 锁定后端契约与失败测试

**Files:**
- Modify: `D:/project/wms-backend/src/test/java/com/example/wms/ErpStockWarningServiceTests.java`
- Create: `D:/project/wms-backend/src/test/java/com/example/wms/ErpStockWarningAnomalyServiceTests.java`

- [ ] **Step 1: 为预警分页新增仓库筛选和策略来源断言，先写失败测试**

```java
@Test
void pageShouldFilterByWarehouseAndExposePolicySource() {
    long tenantId = 9301L;
    long categoryId = 9302L;
    long unitId = 9303L;
    long productId = 9304L;
    long warehouseAId = 9305L;
    long warehouseBId = 9306L;
    Instant now = Instant.parse("2026-06-01T00:00:00Z");

    clearTenantData(tenantId);
    insertCategory(tenantId, categoryId, now);
    insertUnit(tenantId, unitId, now);
    insertWarehouse(tenantId, warehouseAId, "A仓", now);
    insertWarehouse(tenantId, warehouseBId, "B仓", now);
    insertProduct(tenantId, productId, categoryId, unitId, now);
    insertPolicy(tenantId, productId, warehouseAId, new BigDecimal("10"), null, now);
    insertPolicy(tenantId, productId, warehouseBId, new BigDecimal("10"), null, now);
    insertStockBalance(tenantId, productId, warehouseAId, new BigDecimal("2"), now);
    insertStockBalance(tenantId, productId, warehouseBId, new BigDecimal("20"), now);

    TenantContext.setTenantId(tenantId);

    PageResponse<ErpStockWarningView> page = erpStockWarningService.page(1, 20, null, warehouseAId, "LOW", "WAREHOUSE_POLICY", null);

    assertThat(page.items()).hasSize(1);
    assertThat(page.items().get(0).getWarehouseId()).isEqualTo(warehouseAId);
    assertThat(page.items().get(0).getPolicySource()).isEqualTo("WAREHOUSE_POLICY");
}
```

- [ ] **Step 2: 运行失败测试，确认因缺少新参数/字段而失败**

Run: `mvn -Dtest=ErpStockWarningServiceTests#pageShouldFilterByWarehouseAndExposePolicySource test`

Expected: FAIL，报错集中在 `page(...)` 签名、`getPolicySource()` 或查询结果缺字段。

- [ ] **Step 3: 为异常分页新增失败测试**

```java
@Test
void anomalyPageShouldReturnFallbackAndInvalidPolicyRows() {
    long tenantId = 9401L;
    long categoryId = 9402L;
    long unitId = 9403L;
    long productId = 9404L;
    long warehouseId = 9405L;
    Instant now = Instant.parse("2026-06-01T00:00:00Z");

    clearTenantData(tenantId);
    insertCategory(tenantId, categoryId, now);
    insertUnit(tenantId, unitId, now);
    insertWarehouse(tenantId, warehouseId, "异常仓", now);
    insertProductWithGlobalThresholdsOnly(tenantId, productId, categoryId, unitId, now);

    TenantContext.setTenantId(tenantId);

    PageResponse<ErpStockWarningAnomalyView> page = erpStockWarningService.pageAnomalies(1, 20, null, null, null, null);

    assertThat(page.items()).isNotEmpty();
    assertThat(page.items().get(0).getAnomalyTypes()).contains("PRODUCT_FALLBACK_ONLY");
}
```

- [ ] **Step 4: 运行异常测试，确认因接口未实现而失败**

Run: `mvn -Dtest=ErpStockWarningAnomalyServiceTests#anomalyPageShouldReturnFallbackAndInvalidPolicyRows test`

Expected: FAIL，报错集中在 `pageAnomalies(...)` 未定义或 DTO 缺失。

- [ ] **Step 5: 提交测试基线**

```bash
git add D:/project/wms-backend/src/test/java/com/example/wms/ErpStockWarningServiceTests.java D:/project/wms-backend/src/test/java/com/example/wms/ErpStockWarningAnomalyServiceTests.java
git commit -m "test: lock stock warning governance backend behavior"
```

## Task 2: 实现后端预警分页增强

**Files:**
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpStockWarningView.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/mapper/erp/ErpStockWarningMapper.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpStockWarningService.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpStockWarningServiceImpl.java`

- [ ] **Step 1: 在 DTO 中补齐新增字段**

```java
private BigDecimal safetyStock;
private String policySource;
private Boolean hasPolicyAnomaly;
private List<String> anomalyTypes;
```

- [ ] **Step 2: 扩展 Mapper 查询签名，支持新筛选参数**

```java
IPage<ErpStockWarningView> pageWarnings(
    Page<?> page,
    @Param("tenantId") Long tenantId,
    @Param("keyword") String keyword,
    @Param("warehouseId") Long warehouseId,
    @Param("status") String status,
    @Param("policySource") String policySource
);
```

- [ ] **Step 3: 在 SQL 中补齐 `safety_stock`、`policy_source` 与仓库筛选**

```sql
CASE
    WHEN source_kind = 'WAREHOUSE_POLICY' THEN 'WAREHOUSE_POLICY'
    ELSE 'PRODUCT_FALLBACK'
END AS policy_source
```

并在仓库策略分支取 `s.safety_stock`，商品兜底分支取 `p.safety_stock`。

- [ ] **Step 4: 在 Service 中扩展分页方法，透传新参数并为 `anomalyTypes`/`hasPolicyAnomaly` 预留填充**

```java
PageResponse<ErpStockWarningView> page(
    int page,
    int size,
    String keyword,
    Long warehouseId,
    String status,
    String policySource,
    Boolean hasPolicyAnomaly
);
```

- [ ] **Step 5: 运行后端分页测试，确认转绿**

Run: `mvn -Dtest=ErpStockWarningServiceTests#pageShouldFilterByWarehouseAndExposePolicySource test`

Expected: PASS

- [ ] **Step 6: 提交后端分页增强**

```bash
git add D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpStockWarningView.java D:/project/wms-backend/src/main/java/com/example/wms/mapper/erp/ErpStockWarningMapper.java D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpStockWarningService.java D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpStockWarningServiceImpl.java
git commit -m "feat: extend stock warning page query"
```

## Task 3: 实现后端异常分页与导出

**Files:**
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpStockWarningAnomalyView.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpStockWarningAnomalyExportRow.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpStockWarningService.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpStockWarningServiceImpl.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpStockWarningController.java`

- [ ] **Step 1: 定义异常分页 DTO**

```java
public class ErpStockWarningAnomalyView {
    private Long productId;
    private String productCode;
    private String productName;
    private Long warehouseId;
    private String warehouseName;
    private String policySource;
    private BigDecimal safetyStock;
    private BigDecimal minStock;
    private BigDecimal maxStock;
    private BigDecimal policySafetyStock;
    private BigDecimal policyMinStock;
    private BigDecimal policyMaxStock;
    private List<String> anomalyTypes;
    private String recommendation;
}
```

- [ ] **Step 2: 在 Service 中实现异常判定聚合**

```java
private List<String> detectAnomalyTypes(ErpProduct product, ErpProductStockPolicy policy) {
    List<String> result = new ArrayList<>();
    if (policy == null && (product.getMinStock() != null || product.getMaxStock() != null)) {
        result.add("PRODUCT_FALLBACK_ONLY");
    }
    if (policy != null && policy.getMinStock() != null && policy.getMaxStock() != null
        && policy.getMinStock().compareTo(policy.getMaxStock()) > 0) {
        result.add("MIN_GT_MAX");
    }
    return result;
}
```

- [ ] **Step 3: 暴露异常分页与导出接口**

```java
@GetMapping("/anomalies/page")
public Response<PageResponse<ErpStockWarningAnomalyView>> pageAnomalies(...) { ... }

@GetMapping("/anomalies/export")
public void exportAnomalies(..., HttpServletResponse response) { ... }
```

- [ ] **Step 4: 优先复用现有导出写法输出 CSV 或 Excel**

```java
response.setContentType("text/csv;charset=UTF-8");
response.setHeader("Content-Disposition", "attachment; filename=stock-warning-anomalies.csv");
```

- [ ] **Step 5: 运行异常测试并补一个导出断言测试**

Run: `mvn -Dtest=ErpStockWarningAnomalyServiceTests test`

Expected: PASS

- [ ] **Step 6: 提交异常治理后端能力**

```bash
git add D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpStockWarningAnomalyView.java D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpStockWarningAnomalyExportRow.java D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpStockWarningService.java D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpStockWarningServiceImpl.java D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpStockWarningController.java
git commit -m "feat: add stock warning anomaly governance api"
```

## Task 4: 锁定前端库存预警页失败测试

**Files:**
- Create: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpStockWarningFiltersAndAnomalies.test.mjs`
- Create: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpStockWarningActionsContext.test.mjs`

- [ ] **Step 1: 为搜索区、策略来源和异常抽屉写失败测试**

```javascript
test('stock warning page exposes warehouse filter, policy source column, and anomaly entry', () => {
  const source = readFileSync(new URL('../ErpStockWarningManagement.vue', import.meta.url), 'utf8');
  assert.match(source, /v-model="warehouseId"/);
  assert.match(source, /prop="policySource"/);
  assert.match(source, /anomalyDrawerVisible/);
  assert.match(source, /\$t\('action\.viewAnomalies'\)/);
});
```

- [ ] **Step 2: 运行前端失败测试**

Run: `node --test src/views/erp/__tests__/erpStockWarningFiltersAndAnomalies.test.mjs`

Expected: FAIL，缺少筛选控件或异常抽屉状态。

- [ ] **Step 3: 为补货/调拨上下文跳转写失败测试**

```javascript
test('stock warning actions forward product and warehouse context', () => {
  const source = readFileSync(new URL('../ErpStockWarningManagement.vue', import.meta.url), 'utf8');
  assert.match(source, /query:\s*\{[\s\S]*productId: String\(row\.productId\)[\s\S]*warehouseId: String\(row\.warehouseId\)/);
  assert.match(source, /warningSource:\s*'stock-warning'/);
});
```

- [ ] **Step 4: 运行上下文跳转失败测试**

Run: `node --test src/views/erp/__tests__/erpStockWarningActionsContext.test.mjs`

Expected: FAIL，缺少查询参数传递。

- [ ] **Step 5: 提交前端测试基线**

```bash
git add D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpStockWarningFiltersAndAnomalies.test.mjs D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpStockWarningActionsContext.test.mjs
git commit -m "test: lock stock warning governance frontend behavior"
```

## Task 5: 实现前端库存预警页增强

**Files:**
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpStockWarningManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/locales/zh.ts`
- Modify: `D:/project/auto-parts-wms-vue/src/locales/en.ts`

- [ ] **Step 1: 增加筛选状态与请求参数**

```ts
const warehouseId = ref<string>('');
const status = ref<string>('');
const policySource = ref<string>('');
const anomalyDrawerVisible = ref(false);
const anomalyLoading = ref(false);
```

- [ ] **Step 2: 在搜索区补齐仓库、状态、策略来源筛选，并保证按钮区固定右侧**

```vue
<el-select v-model="warehouseId" class="inventory-field--fixed" clearable />
<el-select v-model="status" class="inventory-field--fixed" clearable />
<el-select v-model="policySource" class="inventory-field--fixed" clearable />
```

- [ ] **Step 3: 在表格中补齐 `safetyStock`、`policySource`、`hasPolicyAnomaly` 展示**

```vue
<ErpDataTableColumn prop="safetyStock" :label="$t('field.safetyStock')" min-width="140" />
<ErpDataTableColumn prop="policySource" :label="$t('field.policySource')" min-width="140">
  <template #default="{ row }">{{ policySourceLabel(row.policySource) }}</template>
</ErpDataTableColumn>
```

- [ ] **Step 4: 增加异常治理抽屉与导出按钮**

```vue
<el-drawer v-model="anomalyDrawerVisible" size="68%">
  <div class="stock-warning-anomaly-toolbar">
    <el-button @click="exportAnomalies">{{ $t('action.export') }}</el-button>
  </div>
</el-drawer>
```

- [ ] **Step 5: 统一中英文文案**

```ts
policySource: '策略来源',
warehousePolicy: '仓库策略',
productFallback: '商品默认策略',
viewAnomalies: '查看异常',
safetyStockHint: '安全库存用于补货参考，不直接触发库存预警。'
```

- [ ] **Step 6: 运行前端页面测试并确认转绿**

Run: `node --test src/views/erp/__tests__/erpStockWarningFiltersAndAnomalies.test.mjs src/views/erp/__tests__/erpStockWarningWarehouseScope.test.mjs src/views/erp/__tests__/erpStockWarningEditPolicy.test.mjs`

Expected: PASS

- [ ] **Step 7: 提交前端页面增强**

```bash
git add D:/project/auto-parts-wms-vue/src/views/erp/ErpStockWarningManagement.vue D:/project/auto-parts-wms-vue/src/locales/zh.ts D:/project/auto-parts-wms-vue/src/locales/en.ts
git commit -m "feat: enhance stock warning governance view"
```

## Task 6: 实现补货/调拨联动上下文

**Files:**
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpStockWarningManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpPurchaseOrderForm.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpStockTransferManagement.vue`

- [ ] **Step 1: 在预警页动作中带上查询参数**

```ts
router.push({
  name: 'ErpStockTransferManagement',
  query: {
    productId: String(row.productId),
    warehouseId: String(row.warehouseId),
    warningSource: 'stock-warning'
  }
});
```

- [ ] **Step 2: 在目标页面读取上下文并做最小默认回填**

```ts
const route = useRoute();
const initialProductId = route.query.productId ? Number(route.query.productId) : null;
const initialWarehouseId = route.query.warehouseId ? Number(route.query.warehouseId) : null;
```

- [ ] **Step 3: 仅在参数存在且用户尚未手动编辑时回填**

```ts
if (initialWarehouseId && !formData.sourceWarehouseId) {
  formData.sourceWarehouseId = initialWarehouseId;
}
```

- [ ] **Step 4: 运行动作上下文测试**

Run: `node --test src/views/erp/__tests__/erpStockWarningActionsContext.test.mjs`

Expected: PASS

- [ ] **Step 5: 提交联动上下文实现**

```bash
git add D:/project/auto-parts-wms-vue/src/views/erp/ErpStockWarningManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpPurchaseOrderForm.vue D:/project/auto-parts-wms-vue/src/views/erp/ErpStockTransferManagement.vue
git commit -m "feat: carry stock warning context into actions"
```

## Task 7: 完整回归验证

**Files:**
- Modify: `D:/project/wms-backend/src/test/java/com/example/wms/ErpStockWarningServiceTests.java`
- Modify: `D:/project/wms-backend/src/test/java/com/example/wms/ErpStockWarningAnomalyServiceTests.java`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpStockWarningFiltersAndAnomalies.test.mjs`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpStockWarningActionsContext.test.mjs`

- [ ] **Step 1: 跑后端全部库存预警相关测试**

Run: `mvn -Dtest=ErpStockWarningServiceTests,ErpStockWarningAnomalyServiceTests test`

Expected: PASS

- [ ] **Step 2: 跑前端全部库存预警相关测试**

Run: `node --test src/views/erp/__tests__/erpStockWarningWarehouseScope.test.mjs src/views/erp/__tests__/erpStockWarningEditPolicy.test.mjs src/views/erp/__tests__/erpStockWarningFiltersAndAnomalies.test.mjs src/views/erp/__tests__/erpStockWarningActionsContext.test.mjs`

Expected: PASS

- [ ] **Step 3: 若可行，补跑受影响的目标页面测试**

Run: `node --test src/views/erp/__tests__/erpStockWarningActionsContext.test.mjs`

Expected: PASS

- [ ] **Step 4: 检查最终 diff，仅保留本次功能相关文件**

Run: `git diff --stat -- D:/project/wms-backend/src/main/java/com/example/wms D:/project/wms-backend/src/test/java/com/example/wms D:/project/auto-parts-wms-vue/src/views/erp D:/project/auto-parts-wms-vue/src/locales`

Expected: 只包含库存预警治理相关改动。

- [ ] **Step 5: 提交最终整体验证**

```bash
git add D:/project/wms-backend/src/main/java/com/example/wms D:/project/wms-backend/src/test/java/com/example/wms D:/project/auto-parts-wms-vue/src/views/erp D:/project/auto-parts-wms-vue/src/locales
git commit -m "feat: complete stock warning governance workflow"
```
