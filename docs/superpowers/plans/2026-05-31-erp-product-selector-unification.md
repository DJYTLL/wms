# ERP 商品选择框统一实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 ERP 录单/弹窗中的商品选择控件统一为销售单当前使用的远程商品搜索模式，同时保留各页面自身业务约束与联动逻辑。

**Architecture:** 以销售单的商品搜索模式为基准，抽取可复用的商品远程搜索、当前值兜底和候选项合并逻辑，再逐页接入。页面自身继续负责商品选中后的业务联动，不把业务规则硬编码进公共逻辑。

**Tech Stack:** Vue 3、TypeScript、Element Plus、现有前端测试（`.test.mjs`）

---

### Task 1: 盘点现有商品选择实现并确定公共边界

**Files:**
- Modify: `D:/project/docs/superpowers/specs/2026-05-31-erp-product-selector-unification-design.md`
- Read: `D:/project/auto-parts-wms-vue/src/views/erp/ErpSaleOrderForm.vue`
- Read: `D:/project/auto-parts-wms-vue/src/views/erp/ErpPurchaseOrderForm.vue`
- Read: `D:/project/auto-parts-wms-vue/src/views/erp/ErpSaleReturnForm.vue`
- Read: `D:/project/auto-parts-wms-vue/src/views/erp/ErpPurchaseReturnForm.vue`
- Read: `D:/project/auto-parts-wms-vue/src/views/erp/ErpAssemblyOrderForm.vue`
- Read: `D:/project/auto-parts-wms-vue/src/views/erp/ErpDisassembleOrderForm.vue`
- Read: `D:/project/auto-parts-wms-vue/src/views/erp/ErpStockTransferManagement.vue`
- Read: `D:/project/auto-parts-wms-vue/src/views/erp/ErpStockCountManagement.vue`

- [ ] **Step 1: 确认销售单基准能力**

检查 `ErpSaleOrderForm.vue` 中以下函数和状态：

```ts
const productSearchLoading = ref(false);
const searchProductsNow = async (keyword = '') => {};
const searchProducts = (keyword = '') => {};
const handleProductDropdownVisibleChange = (visible: boolean) => {};
const ensureProductOption = async (productId?: number | null) => {};
const getSelectableProductOptions = (currentProductId?: number | null) => {};
```

- [ ] **Step 2: 确认待改页面的差异点**

重点标记哪些页面仍在使用本地 `filterable` 下拉，哪些页面已经有 `ensureProductOption` 但没有远程搜索，哪些页面使用 `FuzzyProductSelect`。

- [ ] **Step 3: 回写设计文档中的边界说明**

如果发现某个页面商品来源并不走通用搜索接口，就把该差异补充到设计文档中，避免后续误改。

### Task 2: 先补失败测试锁定统一目标

**Files:**
- Create: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpProductSelectorUnification.test.mjs`

- [ ] **Step 1: 编写失败测试，锁定目标页面都使用远程商品搜索模式**

```js
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(process.cwd(), 'src/views/erp');

const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');

const remotePages = [
  'ErpPurchaseOrderForm.vue',
  'ErpSaleReturnForm.vue',
  'ErpPurchaseReturnForm.vue',
  'ErpAssemblyOrderForm.vue',
  'ErpDisassembleOrderForm.vue',
  'ErpStockTransferManagement.vue',
  'ErpStockCountManagement.vue'
];

describe('ERP 商品选择统一为销售单模式', () => {
  it('目标页面的商品选择控件应启用 remote 搜索', () => {
    for (const file of remotePages) {
      const source = read(file);
      assert.match(source, /remote/);
      assert.match(source, /:remote-method=/);
    }
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm test -- erpProductSelectorUnification.test.mjs`

Expected: FAIL，因为多个页面当前还没有 `remote` / `:remote-method`

### Task 3: 抽取公共商品远程搜索能力

**Files:**
- Create: `D:/project/auto-parts-wms-vue/src/composables/useErpRemoteProductSelect.ts`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpSaleOrderForm.vue`

- [ ] **Step 1: 提炼公共 composable 的输入输出接口**

```ts
type ProductOption = {
  id: number;
  code?: string;
  name: string;
  enabled?: boolean;
};

type UseErpRemoteProductSelectOptions = {
  fetchProducts: (keyword: string) => Promise<ProductOption[]>;
  mergeOptionById: (items: ProductOption[], item: ProductOption) => ProductOption[];
};
```

- [ ] **Step 2: 在 composable 中实现远程搜索、默认展开加载、当前值兜底合并**

```ts
export const useErpRemoteProductSelect = (options: UseErpRemoteProductSelectOptions) => {
  const searchLoading = ref(false);
  const searchOptions = ref<ProductOption[]>([]);
  let timer: ReturnType<typeof setTimeout> | null = null;

  const mergeCurrentOption = (selectedOptions: ProductOption[], current?: ProductOption | null) => {
    if (!current?.id) return selectedOptions;
    return options.mergeOptionById(selectedOptions, current);
  };

  const searchNow = async (keyword = '') => {
    searchLoading.value = true;
    try {
      searchOptions.value = await options.fetchProducts(keyword);
    } finally {
      searchLoading.value = false;
    }
  };

  const search = (keyword = '') => {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      void searchNow(keyword);
    }, keyword ? 120 : 0);
  };

  const loadOnVisible = async (visible: boolean) => {
    if (!visible || searchOptions.value.length || searchLoading.value) return;
    await searchNow('');
  };

  return {
    searchLoading,
    searchOptions,
    searchNow,
    search,
    loadOnVisible,
    mergeCurrentOption
  };
};
```

- [ ] **Step 3: 先让销售单接到 composable，保证基准页行为不回退**

把 `ErpSaleOrderForm.vue` 中已有的 `searchProductsNow/searchProducts/handleProductDropdownVisibleChange` 接到新 composable，保持模板 API 不变。

- [ ] **Step 4: 运行销售单相关测试**

Run: `npm test -- erpSaleOrderRemoteProductSearch.test.mjs erpSaleOrderInlineProductSync.test.mjs`

Expected: PASS

### Task 4: 接入采购单与退货单

**Files:**
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpPurchaseOrderForm.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpSaleReturnForm.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpPurchaseReturnForm.vue`

- [ ] **Step 1: 将商品选择模板切换为 `remote + reserve-keyword + loading + visible-change`**

```vue
<el-select
  v-model="row.productId"
  filterable
  remote
  clearable
  reserve-keyword
  class="product-cell__select"
  :disabled="!formData.supplierId"
  :remote-method="searchProducts"
  :loading="productSearchLoading"
  @visible-change="handleProductDropdownVisibleChange"
  @change="handleProductChange(row)"
>
```

- [ ] **Step 2: 为每个页面补齐远程搜索状态与方法**

```ts
const {
  searchLoading: productSearchLoading,
  search: searchProducts,
  searchNow: searchProductsNow,
  searchOptions: productSearchOptions,
  loadOnVisible: handleProductDropdownVisibleChange
} = useErpRemoteProductSelect({
  fetchProducts: async (keyword) => {
    const res = await request.get('/erp/products/options', { params: { keyword } });
    return res.data?.data || [];
  },
  mergeOptionById
});
```

- [ ] **Step 3: 调整 `getSelectableProductOptions`，合并搜索结果与当前值**

```ts
const getSelectableProductOptions = (currentProductId?: number | null) => {
  const baseOptions = mergeCurrentProductOptions(productOptions.value, productSearchOptions.value);
  return baseOptions.filter((item) => item.enabled !== false || item.id === currentProductId);
};
```

- [ ] **Step 4: 保留客户/供应商前置约束**

采购单与采购退货保留 `!formData.supplierId` 禁用，销售退货保留 `!formData.customerId` 或原有来源单据约束。

### Task 5: 接入组装单与拆装单

**Files:**
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpAssemblyOrderForm.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpDisassembleOrderForm.vue`

- [ ] **Step 1: 同时覆盖成品商品和明细商品选择器**

这两个页面既有成品选择，也有子项商品选择，都要统一远程搜索模式。

- [ ] **Step 2: 不引入客户/供应商禁用规则**

```ts
const isProductSelectDisabled = false;
```

实际实现中保持页面现有的业务禁用逻辑，不新增交易单据特有约束。

- [ ] **Step 3: 保证已有 `ensureProductOption` 与远程搜索结果协同**

编辑旧单据时，成品与子项商品都必须能通过兜底逻辑正确回显。

### Task 6: 接入库存调拨与盘点

**Files:**
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpStockTransferManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpStockCountManagement.vue`

- [ ] **Step 1: 将 `FuzzyProductSelect` / 本地下拉改为销售单式远程商品选择**

如果这两个页面当前使用 `FuzzyProductSelect` 或普通 `el-select`，则切换为与销售单一致的远程搜索模式。

- [ ] **Step 2: 仅保留适合库存作业页的能力**

保留：

- 远程搜索
- 当前值兜底
- 默认展开加载

不新增：

- 客户/供应商前置约束
- 商品下拉内联编辑按钮

- [ ] **Step 3: 验证商品切换后库位/库存联动不回退**

库存作业页商品选择完成后，仍要继续触发原有的仓库、库位、库存数量联动。

### Task 7: 跑测试和类型检查

**Files:**
- Test: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpProductSelectorUnification.test.mjs`

- [ ] **Step 1: 运行统一测试**

Run: `npm test -- erpProductSelectorUnification.test.mjs`

Expected: PASS

- [ ] **Step 2: 运行受影响销售/采购/退货页面相关测试**

Run: `npm test -- erpSaleOrderRemoteProductSearch.test.mjs erpSaleOrderInlineProductSync.test.mjs erpProductSelectorUnification.test.mjs`

Expected: PASS

- [ ] **Step 3: 运行前端类型检查**

Run: `npm run type-check`

Expected: PASS

### Task 8: 自检并整理交付说明

**Files:**
- Modify: `D:/project/docs/superpowers/specs/2026-05-31-erp-product-selector-unification-design.md`
- Modify: `D:/project/docs/superpowers/plans/2026-05-31-erp-product-selector-unification.md`

- [ ] **Step 1: 检查计划和实现是否覆盖全部目标页面**

确认以下页面均已处理：

- `ErpPurchaseOrderForm.vue`
- `ErpSaleReturnForm.vue`
- `ErpPurchaseReturnForm.vue`
- `ErpAssemblyOrderForm.vue`
- `ErpDisassembleOrderForm.vue`
- `ErpStockTransferManagement.vue`
- `ErpStockCountManagement.vue`

- [ ] **Step 2: 检查没有误改筛选区、预览页、只读页**

只保留本次范围内的商品选择器改动。

- [ ] **Step 3: 准备交付说明**

说明应覆盖：

- 哪些页面已统一为销售单模式
- 哪些页面保留了客户/供应商约束
- 哪些库存作业页只接入远程搜索与兜底能力
- 跑了哪些测试与类型检查
