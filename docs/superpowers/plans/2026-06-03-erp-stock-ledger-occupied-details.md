# ERP库存台账占用明细实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在库存台账中展示库存数量、当前占用数量、可用库存，并支持鼠标悬浮查看占用明细，单号蓝字点击后在新标签页打开对应单据。

**Architecture:** 前端继续复用现有库存台账页面和列权限框架，只在占用数量列增加悬浮弹层与单号跳转入口。后端新增一个按库存台账行查询占用明细的只读接口，前端悬浮时按需请求，避免首屏把占用明细一次性全拉出来。

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vue Router, Spring Boot, MyBatis-Plus, JUnit 5.

---

### Task 1: 锁定库存台账占用明细的前端行为

**Files:**
- Modify: `auto-parts-wms-vue/src/views/erp/__tests__/erpStockManagementReservedQty.test.mjs`
- Modify: `auto-parts-wms-vue/src/views/erp/ErpStockManagement.vue`

- [ ] **Step 1: Write the failing test**

```js
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const viewsRoot = join(__dirname, '..');
const componentSource = readFileSync(join(viewsRoot, 'ErpStockManagement.vue'), 'utf8');

test('库存台账占用数量可悬浮查看明细并支持单号新标签页跳转', () => {
  assert.match(componentSource, /el-tooltip|el-popover/);
  assert.match(componentSource, /qtyLocked/);
  assert.match(componentSource, /window\.open\(/);
  assert.match(componentSource, /_blank/);
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node auto-parts-wms-vue/src/views/erp/__tests__/erpStockManagementReservedQty.test.mjs`
Expected: FAIL because the hover detail and new-tab jump code are not yet present.

- [ ] **Step 3: Write minimal implementation**

```vue
<template>
  <ErpDataTableColumn v-if="canShow('qtyLocked')" :label="$t('field.qtyLocked')" min-width="120">
    <template #default="{ row }">
      <el-popover trigger="hover" placement="top" width="420">
        <template #reference>
          <span class="stock-occupied-trigger">{{ row.qtyLocked ?? 0 }}</span>
        </template>
        <div v-loading="occupiedLoading[row.id]">
          <div v-for="item in getOccupiedItems(row.id)" :key="item.id" class="occupied-line">
            <span>{{ formatDocType(item.bizType) }}</span>
            <el-link type="primary" @click="openDoc(item)">{{ item.docNo }}</el-link>
            <span>{{ item.qty }}</span>
          </div>
        </div>
      </el-popover>
    </template>
  </ErpDataTableColumn>
</template>

<script setup lang="ts">
const openDoc = (item: OccupiedItem) => {
  const url = resolveDocUrl(item);
  if (!url) return;
  window.open(url, '_blank', 'noopener,noreferrer');
};
</script>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node auto-parts-wms-vue/src/views/erp/__tests__/erpStockManagementReservedQty.test.mjs`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add auto-parts-wms-vue/src/views/erp/ErpStockManagement.vue auto-parts-wms-vue/src/views/erp/__tests__/erpStockManagementReservedQty.test.mjs
git commit -m "feat: show stock occupancy details in ledger"
```

### Task 2: 提供库存台账行的占用明细接口

**Files:**
- Modify: `wms-backend/src/main/java/com/example/wms/service/erp/ErpStockService.java`
- Modify: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpStockServiceImpl.java`
- Modify: `wms-backend/src/main/java/com/example/wms/controller/erp/ErpStockController.java`
- Add: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpStockOccupancyItem.java`
- Add: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpStockOccupancyView.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void pageBalance_shouldExposeOccupancyDetailEndpointData() {
    var result = erpStockService.getOccupancyByBalanceId(tenantId, balanceId);
    assertThat(result).isNotEmpty();
    assertThat(result.get(0).getDocNo()).isNotBlank();
    assertThat(result.get(0).getQty()).isPositive();
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl wms-backend -Dtest=ErpStockServiceImplTest test`
Expected: FAIL because `getOccupancyByBalanceId` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

```java
public record ErpStockOccupancyView(
    String bizType,
    Long bizId,
    String docNo,
    BigDecimal qty,
    String docUrl
) {}

public List<ErpStockOccupancyView> getOccupancyByBalanceId(Long balanceId) {
    // 按库存台账行对应的商品/仓库/库位，汇总草稿和已审核单据的占用明细
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -pl wms-backend -Dtest=ErpStockServiceImplTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add wms-backend/src/main/java/com/example/wms/service/erp/ErpStockService.java wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpStockServiceImpl.java wms-backend/src/main/java/com/example/wms/controller/erp/ErpStockController.java wms-backend/src/main/java/com/example/wms/dto/erp/ErpStockOccupancyItem.java wms-backend/src/main/java/com/example/wms/dto/erp/ErpStockOccupancyView.java
git commit -m "feat: add stock occupancy detail api"
```

### Task 3: 连接前后端并验证页面交互

**Files:**
- Modify: `auto-parts-wms-vue/src/views/erp/ErpStockManagement.vue`
- Add/Modify: `auto-parts-wms-vue/src/views/erp/__tests__/erpStockManagementOccupancyPopover.test.mjs`

- [ ] **Step 1: Write the failing test**

```js
test('库存台账占用明细接口只在悬浮时请求且单号新标签打开', () => {
  assert.match(viewSource, /request\.get\('\/erp\/stock\/balances\/occupancy'/);
  assert.match(viewSource, /window\.open\(.*'_blank'/);
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node auto-parts-wms-vue/src/views/erp/__tests__/erpStockManagementOccupancyPopover.test.mjs`
Expected: FAIL until the hover loader and URL opener are wired in.

- [ ] **Step 3: Write minimal implementation**

```ts
const loadOccupancy = async (balanceId: number) => {
  if (occupiedCache.value[balanceId]) return;
  const res: any = await request.get('/erp/stock/balances/occupancy', { params: { balanceId } });
  occupiedCache.value[balanceId] = res.data.data || [];
};
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node auto-parts-wms-vue/src/views/erp/__tests__/erpStockManagementOccupancyPopover.test.mjs`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add auto-parts-wms-vue/src/views/erp/ErpStockManagement.vue auto-parts-wms-vue/src/views/erp/__tests__/erpStockManagementOccupancyPopover.test.mjs
git commit -m "feat: wire stock occupancy hover details"
```

