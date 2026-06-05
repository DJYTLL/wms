<template>
  <ProductHistoryDialog
    v-model:visible="historyVisible"
    v-model:active-tab="activeTab"
    :title="$t('action.productHistory')"
    :loading="loading"
    :header-items="headerItems"
    :hint="$t('message.historyShortcutHint')"
    :tabs="tabs"
    :empty-text="$t('table.empty')"
    :keyword-placeholder="$t('placeholder.keyword')"
    :range-separator="$t('separator.to')"
    :start-placeholder="$t('field.startTime')"
    :end-placeholder="$t('field.endTime')"
    @tab-change="$emit('tabChange', $event)"
    @filter-change="$emit('filterChange', $event)"
    @page-change="$emit('pageChange', $event)"
    @size-change="$emit('sizeChange', $event)"
  />

  <el-dialog
    v-model="orderVisible"
    :title="orderTitle"
    width="92vw"
    class="history-order-dialog"
    append-to-body
  >
    <iframe v-if="orderUrl" :src="orderUrl" class="history-order-frame" />
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue';

const ProductHistoryDialog = defineAsyncComponent(() => import('@/components/ProductHistoryDialog.vue'));

interface HistoryHeaderItem {
  label: string;
  value: string;
  show?: boolean;
}

interface HistoryTabState {
  keyword: string;
  range: string[];
  page: number;
  size: number;
  total: number;
}

interface HistoryTabConfig {
  name: string;
  label: string;
  data: Record<string, any>[];
  columns: any[];
  state?: HistoryTabState;
  height?: number | string;
  pageSizes?: number[];
}

type HistoryFilterPayload = { tabName: string; keyword?: string; range?: string[] };
type HistoryPagePayload = { tabName: string; page: number };
type HistorySizePayload = { tabName: string; size: number };

const props = defineProps<{
  visible: boolean;
  activeTab: string;
  loading: boolean;
  headerItems: HistoryHeaderItem[];
  tabs: HistoryTabConfig[];
  orderVisible: boolean;
  orderTitle: string;
  orderUrl: string;
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  'update:activeTab': [value: string];
  'update:orderVisible': [value: boolean];
  tabChange: [value: string];
  filterChange: [value: HistoryFilterPayload];
  pageChange: [value: HistoryPagePayload];
  sizeChange: [value: HistorySizePayload];
}>();

const historyVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
});

const activeTab = computed({
  get: () => props.activeTab,
  set: (value) => emit('update:activeTab', value)
});

const orderVisible = computed({
  get: () => props.orderVisible,
  set: (value) => emit('update:orderVisible', value)
});
</script>

<style scoped>
.history-order-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.history-order-frame {
  width: 100%;
  height: 78vh;
  border: none;
  display: block;
}
</style>
