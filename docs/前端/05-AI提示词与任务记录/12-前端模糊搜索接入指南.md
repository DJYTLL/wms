# 前端模糊搜索接入指南

这份文档用于把现有列表页的普通搜索改成前端模糊搜索。当前实现基于 `src/utils/fuzzySearch.ts`，适合数据量不大、页面一次性拿到完整列表的场景。

## 支持的搜索效果

以名称 `客户5` 为例：

- 输入 `客户` 可以搜到。
- 输入 `客5` 可以搜到。
- 输入 `5客` 可以搜到。
- 输入 `k5` 可以搜到，因为 `客户5` 的拼音首字母是 `kh5`。
- 输入 `ke5` 可以搜到，因为 `客户5` 的全拼是 `kehu5`。

匹配来源包括：

- 原始文本，例如 `客户5`。
- 全拼文本，例如 `kehu5`。
- 拼音首字母，例如 `kh5`。

匹配规则包括：

- 连续包含：`客户5` 包含 `客户`。
- 无序包含：`客户5` 可以匹配 `5客`。
- 拼音匹配：`kh5` 可以匹配 `k5`。

## 适用范围

推荐用于：

- 页面已经一次性拉取完整列表。
- 数据量通常在几千条以内。
- 需要快速支持中文、拼音首字母、无序关键字搜索。

不推荐用于：

- 后端分页页面。
- 数据量可能上万或几十万。
- 搜索需要跨大量业务单据、复杂排序、权限过滤或高亮。

大数据量场景应优先做后端搜索索引，例如在数据库中维护名称、全拼、首字母等搜索字段，或接入 Elasticsearch。

## 推荐接入方式

从 `@/utils/fuzzySearch` 引入 `filterByFuzzyKeyword`：

```ts
import { filterByFuzzyKeyword } from '@/utils/fuzzySearch';
```

列表页保留两份数据：

```ts
const tableData = ref<CustomerRow[]>([]);
const allTableData = ref<CustomerRow[]>([]);
const searchQuery = ref('');
```

接口请求时保存完整数据，再应用本地搜索：

```ts
const fetchData = async () => {
  const res: any = await request.get('/api/customers');
  if (res.data.code === 200) {
    allTableData.value = res.data.data || [];
    applySearch();
  }
};
```

搜索逻辑写成：

```ts
const applySearch = () => {
  tableData.value = filterByFuzzyKeyword(allTableData.value, searchQuery.value, row => [
    row.name,
    row.code,
    row.shortName
  ]);
};
```

搜索按钮和回车事件可以继续调用 `fetchData()`，这样点击搜索时会重新刷新后端数据，再做模糊筛选：

```ts
const handleSearch = () => {
  fetchData();
};
```

如果页面不需要刷新后端数据，只想对现有列表立即筛选，可以让 `handleSearch` 调用 `applySearch()`。

## 模板接入示例

```vue
<el-input
  v-model="searchQuery"
  :placeholder="$t('action.search')"
  clearable
  @clear="handleSearch"
  @keyup.enter="handleSearch"
/>
<el-button type="primary" @click="handleSearch">
  {{ $t('action.search') }}
</el-button>
```

## 多字段搜索

`filterByFuzzyKeyword` 的第三个参数用于声明哪些字段参与搜索：

```ts
filterByFuzzyKeyword(tableRows, keyword, row => [
  row.name,
  row.code,
  row.shortName,
  row.contact,
  row.phone
]);
```

这些字段会被拼成一个搜索文本，并生成原文、全拼、首字母三个搜索源。

## 当前已接入页面

- `src/views/erp/ErpCustomerDebtManagement.vue`
- `src/views/erp/ErpSupplierDebtManagement.vue`

这两个页面的搜索按钮会重新请求接口刷新数据，然后用前端模糊搜索筛选结果。

## 注意事项

- 前端模糊搜索会遍历当前已加载列表，数据越多越慢。
- 拼音转换发生在浏览器端，中文字段多、数据量大时会增加计算成本。
- 如果页面使用后端分页，不能只筛当前页，否则用户会误以为没有搜到其他页的数据。
- 后端分页页面要接入模糊搜索时，应改后端接口，而不是只改前端。
- 如果某个页面必须支持大量数据搜索，建议新增后端搜索字段，例如 `search_text`、`search_pinyin`、`search_initials`，并建立索引。
