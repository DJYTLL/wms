<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    width="980px"
    class="history-dialog"
    append-to-body
  >
    <div class="history-header">
      <div
        v-for="item in visibleHeaderItems"
        :key="item.label"
        class="history-header__item"
      >
        <span>{{ item.label }}：</span>
        <strong>{{ item.value }}</strong>
      </div>
      <div v-if="hint" class="history-header__hint">{{ hint }}</div>
    </div>

    <div v-loading="loading" :class="['history-grid', { 'history-grid--tabs': hasMultipleTabs }]">
      <el-tabs
        v-if="hasMultipleTabs"
        :model-value="activeTab"
        type="card"
        class="history-tabs"
        @tab-change="handleTabChange"
      >
        <el-tab-pane
          v-for="tab in tabs"
          :key="tab.name"
          :label="tab.label"
          :name="tab.name"
        >
          <HistoryTabPanel
            :tab="tab"
            :empty-text="emptyText"
            :keyword-placeholder="keywordPlaceholder"
            :range-separator="rangeSeparator"
            :start-placeholder="startPlaceholder"
            :end-placeholder="endPlaceholder"
            @filter-change="handleFilterChange"
            @page-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </el-tab-pane>
      </el-tabs>

      <HistoryTabPanel
        v-else-if="currentTab"
        :tab="currentTab"
        :empty-text="emptyText"
        :keyword-placeholder="keywordPlaceholder"
        :range-separator="rangeSeparator"
        :start-placeholder="startPlaceholder"
        :end-placeholder="endPlaceholder"
        @filter-change="handleFilterChange"
        @page-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, defineComponent, h } from 'vue';
import type { PropType } from 'vue';
import { ElButton, ElDatePicker, ElInput, ElPagination, ElTable, ElTableColumn } from 'element-plus';
import 'element-plus/es/components/button/style/css';
import 'element-plus/es/components/date-picker/style/css';
import 'element-plus/es/components/input/style/css';
import 'element-plus/es/components/pagination/style/css';
import 'element-plus/es/components/table/style/css';

interface HistoryHeaderItem {
  label: string;
  value: string;
  show?: boolean;
}

interface HistoryColumn {
  label: string;
  prop?: string;
  width?: number | string;
  minWidth?: number | string;
  formatter?: (row: any) => string | number;
  type?: 'text' | 'link';
  onClick?: (row: any) => void;
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

const props = defineProps({
  visible: {
    type: Boolean,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  headerItems: {
    type: Array as PropType<HistoryHeaderItem[]>,
    default: () => []
  },
  hint: {
    type: String,
    default: ''
  },
  tabs: {
    type: Array as PropType<HistoryTabConfig[]>,
    default: () => []
  },
  activeTab: {
    type: String,
    default: ''
  },
  emptyText: {
    type: String,
    default: ''
  },
  keywordPlaceholder: {
    type: String,
    default: ''
  },
  rangeSeparator: {
    type: String,
    default: '-'
  },
  startPlaceholder: {
    type: String,
    default: ''
  },
  endPlaceholder: {
    type: String,
    default: ''
  }
});

const emit = defineEmits<{
  (event: 'update:visible', value: boolean): void;
  (event: 'update:activeTab', value: string): void;
  (event: 'tab-change', value: string): void;
  (event: 'filter-change', payload: { tabName: string; keyword?: string; range?: string[] }): void;
  (event: 'page-change', payload: { tabName: string; page: number }): void;
  (event: 'size-change', payload: { tabName: string; size: number }): void;
}>();

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
});

const visibleHeaderItems = computed(() => props.headerItems.filter(item => item.show !== false));
const hasMultipleTabs = computed(() => props.tabs.length > 1);
const currentTab = computed(() => {
  if (!props.tabs.length) return null;
  return props.tabs.find(item => item.name === props.activeTab) || props.tabs[0];
});

const handleTabChange = (value: string | number) => {
  const nextTab = String(value);
  emit('update:activeTab', nextTab);
  emit('tab-change', nextTab);
};

const handlePageChange = (payload: { tabName: string; page: number }) => {
  emit('page-change', payload);
};

const handleSizeChange = (payload: { tabName: string; size: number }) => {
  emit('size-change', payload);
};

const handleFilterChange = (payload: { tabName: string; keyword?: string; range?: string[] }) => {
  emit('filter-change', payload);
};

const HistoryTabPanel = defineComponent({
  name: 'HistoryTabPanel',
  props: {
    tab: {
      type: Object as PropType<HistoryTabConfig>,
      required: true
    },
    emptyText: {
      type: String,
      required: true
    },
    keywordPlaceholder: {
      type: String,
      required: true
    },
    rangeSeparator: {
      type: String,
      required: true
    },
    startPlaceholder: {
      type: String,
      required: true
    },
    endPlaceholder: {
      type: String,
      required: true
    }
  },
  emits: ['filter-change', 'page-change', 'size-change'],
  setup(panelProps, { emit: panelEmit }) {
    const resolveCellValue = (column: HistoryColumn, row: Record<string, any>) => {
      if (column.formatter) return column.formatter(row);
      if (!column.prop) return '';
      return row[column.prop] ?? '';
    };

    return () => {
      const { tab } = panelProps;
      const hasFilter = Boolean(tab.state);
      const hasPagination = Boolean(tab.state);

      const columns = tab.columns.map(column =>
        h(
          ElTableColumn,
          {
            key: `${tab.name}-${column.label}-${column.prop || 'custom'}`,
            prop: column.prop,
            label: column.label,
            width: column.width,
            minWidth: column.minWidth
          },
          {
            default: column.formatter || column.type === 'link'
              ? ({ row }: { row: Record<string, any> }) => {
                  const value = resolveCellValue(column, row);
                  if (column.type === 'link') {
                    return h(
                      ElButton,
                      {
                        link: true,
                        type: 'primary',
                        onClick: () => column.onClick?.(row)
                      },
                      () => String(value || '')
                    );
                  }
                  return String(value ?? '');
                }
              : undefined
          }
        )
      );

      return h('div', { class: 'product-history-tab-panel' }, [
        hasFilter
          ? h('div', { class: 'product-history-toolbar' }, [
              h(ElInput, {
                modelValue: tab.state!.keyword,
                'onUpdate:modelValue': (value: string) => panelEmit('filter-change', { tabName: tab.name, keyword: value }),
                placeholder: panelProps.keywordPlaceholder,
                clearable: true,
                class: 'product-history-search'
              }),
              h(ElDatePicker, {
                modelValue: tab.state!.range,
                'onUpdate:modelValue': (value: string[]) => panelEmit('filter-change', {
                  tabName: tab.name,
                  range: Array.isArray(value) ? value : []
                }),
                type: 'daterange',
                format: 'YYYY-MM-DD',
                valueFormat: 'YYYY-MM-DD',
                rangeSeparator: panelProps.rangeSeparator,
                startPlaceholder: panelProps.startPlaceholder,
                endPlaceholder: panelProps.endPlaceholder,
                class: 'product-history-date',
                clearable: true
              })
            ])
          : null,
        h(
          ElTable,
          {
            data: tab.data,
            stripe: true,
            emptyText: panelProps.emptyText,
            height: tab.height || 260
          },
          () => columns
        ),
        hasPagination
          ? h(ElPagination, {
              class: 'product-history-pagination',
              background: true,
              layout: 'total, sizes, prev, pager, next',
              total: tab.state!.total,
              currentPage: tab.state!.page,
              pageSize: tab.state!.size,
              pageSizes: tab.pageSizes || [5, 10, 20, 50],
              onCurrentChange: (page: number) => panelEmit('page-change', { tabName: tab.name, page }),
              onSizeChange: (size: number) => panelEmit('size-change', { tabName: tab.name, size })
            })
          : null
      ]);
    };
  }
});
</script>

<style scoped>
.history-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
}

.history-header {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  margin-bottom: 12px;
  color: #2c3e50;
}

.history-header__item {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.history-header__hint {
  margin-left: auto;
  font-size: 12px;
  color: #8c8c8c;
}

.history-grid {
  display: grid;
  gap: 16px;
}

.history-grid--tabs {
  grid-template-columns: 1fr;
}

:global(.product-history-tab-panel) {
  display: grid;
  gap: 10px;
}

.history-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.history-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: #e6ebf5;
}

.history-tabs :deep(.el-tabs__item) {
  height: 40px;
  padding: 0 18px;
  font-weight: 600;
}

:global(.product-history-toolbar) {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

:global(.product-history-search) {
  flex: 0 0 220px;
  max-width: 100%;
}

:global(.product-history-date) {
  flex: 0 0 360px;
  max-width: 100%;
}

:global(.product-history-pagination) {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 2px;
  padding: 0;
  white-space: nowrap;
}

:global(.product-history-pagination .el-pagination__total) {
  margin-right: 4px;
  color: #5b6475;
}

:global(.product-history-pagination .el-pagination__sizes) {
  margin: 0 4px;
  width: auto;
}

:global(.product-history-pagination .el-select) {
  width: 128px;
}

:global(.product-history-pagination .el-pager) {
  display: flex;
  align-items: center;
  list-style: none;
  margin: 0;
  padding: 0;
}

:global(.product-history-pagination .btn-prev),
:global(.product-history-pagination .btn-next),
:global(.product-history-pagination .el-pager li) {
  min-width: 32px;
  height: 32px;
  line-height: 32px;
}

@media (max-width: 900px) {
  :global(.product-history-toolbar) {
    flex-wrap: wrap;
  }

  :global(.product-history-search),
  :global(.product-history-date) {
    flex: 1 1 100%;
  }

  :global(.product-history-pagination) {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  :global(.product-history-pagination .el-pagination__total) {
    width: 100%;
  }
}
</style>
