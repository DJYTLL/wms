<template>
  <component :is="AsyncView" />
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, h, onBeforeUnmount } from 'vue';
import type { Component } from 'vue';

const props = withDefaults(defineProps<{
  title: string;
  loader: () => Promise<Component | { default: Component }>;
  variant?: 'order' | 'inventory';
  columns?: string[];
  showActionPlaceholder?: boolean;
  showSummaryPlaceholder?: boolean;
}>(), {
  variant: 'order',
  showActionPlaceholder: true,
  showSummaryPlaceholder: false
});

const ShellPlaceholder = computed(() => ({
  name: 'AsyncErpRouteShellPlaceholder',
  setup() {
    const isPurchasePage = props.title.includes('采购');
    const isSalePage = props.title.includes('销售');
    const counterpartyLabel = isPurchasePage ? '供应商' : isSalePage ? '客户' : '仓库';
    const columnLabels = props.columns ?? (props.variant === 'inventory'
      ? ['序号', '商品', '仓库', '库位', '库存数量', '可用数量', '更新时间']
      : ['序号', '单号', counterpartyLabel, '状态', '总金额', '创建时间', '操作']);
    const toolbarCardClass = props.variant === 'inventory'
      ? 'page-toolbar-card async-erp-route-shell__toolbar-card'
      : 'erp-toolbar async-erp-route-shell__toolbar-card';
    const toolbarClass = props.variant === 'inventory'
      ? 'table-toolbar inventory-toolbar async-erp-route-shell__toolbar async-erp-route-shell__toolbar--inventory'
      : 'table-toolbar async-erp-route-shell__toolbar';
    const filtersClass = props.variant === 'inventory'
      ? 'table-filters inventory-filters async-erp-route-shell__filters async-erp-route-shell__filters--inventory'
      : 'table-filters async-erp-route-shell__filters';
    const actionsClass = props.variant === 'inventory'
      ? 'table-actions inventory-actions async-erp-route-shell__actions'
      : 'table-actions async-erp-route-shell__actions';
    const filters = props.variant === 'inventory'
      ? ['商品', '仓库', '库位'].map((label) => h('div', { class: 'async-erp-route-shell__control async-erp-route-shell__control--narrow' }, label))
      : [
        h('div', { class: 'async-erp-route-shell__control async-erp-route-shell__control--wide' }, '搜索'),
        h('div', { class: 'async-erp-route-shell__control async-erp-route-shell__control--wide async-erp-route-shell__select' }, counterpartyLabel),
        h('div', { class: 'async-erp-route-shell__control async-erp-route-shell__control--range' }, [
          h('span', { class: 'async-erp-route-shell__clock' }, '◷'),
          h('span', '开始时间'),
          h('span', { class: 'async-erp-route-shell__range-separator' }, '-'),
          h('span', '结束时间')
        ])
      ];
    const actions = props.showActionPlaceholder
      ? [h('button', { class: 'async-erp-route-shell__button async-erp-route-shell__button--primary', type: 'button', disabled: true }, '新增')]
      : [];

    return () => h('div', { class: 'page-shell page-shell--system async-erp-route-shell' }, [
      h('div', { class: 'page-header' }, [
        h('div', { class: 'page-title' }, props.title),
        h('div', { class: toolbarCardClass }, [
          h('div', { class: toolbarClass }, [
            h('div', { class: filtersClass }, filters),
            h('div', { class: actionsClass }, actions)
          ])
        ])
      ]),
      h('div', { class: 'table-card async-erp-route-shell__table-card' }, [
        h('div', { class: 'table-body async-erp-route-shell__table-body' }, [
          h('div', {
            class: 'async-erp-route-shell__table-head',
            style: {
              gridTemplateColumns: `70px repeat(${Math.max(columnLabels.length - 1, 1)}, minmax(120px, 1fr))`
            }
          }, columnLabels.map((label) => h('div', { class: 'async-erp-route-shell__column-labels' }, label))),
          h('div', { class: 'async-erp-route-shell__loading-panel' }, [
            h('div', { class: 'async-erp-route-shell__spinner', 'aria-label': '加载中' })
          ])
        ]),
        h('div', { class: 'table-pagination async-erp-route-shell__pagination-shell' }, [
          h('span', '共 0 条'),
          h('div', { class: 'async-erp-route-shell__page-size' }, '20条/页'),
          h('button', { class: 'async-erp-route-shell__pager-button', type: 'button', disabled: true }, '‹'),
          h('button', { class: 'async-erp-route-shell__pager-button async-erp-route-shell__pager-button--active', type: 'button', disabled: true }, '1'),
          h('button', { class: 'async-erp-route-shell__pager-button', type: 'button', disabled: true }, '›'),
          h('span', '前往'),
          h('div', { class: 'async-erp-route-shell__page-jumper' }, '1'),
          h('span', '页')
        ])
      ])
    ]);
  }
}));

const waitForShellPaint = () => new Promise<void>((resolve) => {
  if (typeof window === 'undefined' || typeof window.requestAnimationFrame !== 'function') {
    setTimeout(resolve, 0);
    return;
  }

  window.requestAnimationFrame(() => {
    window.requestAnimationFrame(() => resolve());
  });
});

const waitForRouteShellDisposal = () => new Promise<Component>(() => undefined);

let isRouteShellUnmounted = false;

onBeforeUnmount(() => {
  isRouteShellUnmounted = true;
});

const AsyncView = defineAsyncComponent({
  loader: async () => {
    await waitForShellPaint();
    if (isRouteShellUnmounted) {
      return waitForRouteShellDisposal();
    }

    const loaded = await props.loader();
    return 'default' in loaded ? loaded.default : loaded;
  },
  loadingComponent: ShellPlaceholder.value,
  delay: 0,
  suspensible: false
});
</script>

<style>
.async-erp-route-shell__toolbar-card {
  width: 100%;
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #ffffff;
  box-sizing: border-box;
}

.async-erp-route-shell__toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
  width: 100%;
}

.async-erp-route-shell__toolbar--inventory {
  grid-template-columns: minmax(0, 1fr) auto;
}

.async-erp-route-shell__filters {
  display: grid;
  grid-template-columns: 220px 220px 380px;
  align-items: center;
  justify-content: start;
  gap: 12px;
  flex: 1 1 auto;
  min-width: 0;
}

.async-erp-route-shell__filters--inventory {
  grid-template-columns: 140px 140px 140px;
}

.async-erp-route-shell__actions {
  flex-wrap: nowrap;
}

.async-erp-route-shell__control {
  display: flex;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
  box-sizing: border-box;
  color: #a8abb2;
  font-size: 14px;
  line-height: 32px;
}

.async-erp-route-shell__control--wide {
  width: 220px;
}

.async-erp-route-shell__control--narrow {
  width: 140px;
}

.async-erp-route-shell__control--range {
  width: 380px;
  justify-content: space-around;
}

.async-erp-route-shell__select::after {
  content: '⌄';
  margin-left: auto;
  color: #c0c4cc;
}

.async-erp-route-shell__clock {
  color: #c0c4cc;
}

.async-erp-route-shell__range-separator {
  color: #909399;
}

.async-erp-route-shell__table-card {
  margin-top: 0;
}

.async-erp-route-shell__table-body {
  padding: 0;
}

.async-erp-route-shell__table-head {
  display: grid;
  min-width: 100%;
  height: 48px;
  border-bottom: 1px solid #ebeef5;
  background: #ffffff;
}

.async-erp-route-shell__column-labels {
  display: flex;
  align-items: center;
  padding: 0 12px;
  border-right: 1px solid #ebeef5;
  color: #606266;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.async-erp-route-shell__loading-panel {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 340px;
  background: #ffffff;
}

.async-erp-route-shell__spinner {
  width: 34px;
  height: 34px;
  border: 3px solid #d9ecff;
  border-top-color: #409eff;
  border-radius: 50%;
  animation: async-erp-route-shell-spin 0.85s linear infinite;
}

.async-erp-route-shell__button {
  height: 32px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
  color: #606266;
  cursor: default;
  font-size: 14px;
}

.async-erp-route-shell__button--icon {
  width: 32px;
  padding: 0;
}

.async-erp-route-shell__button--primary {
  min-width: 68px;
  padding: 0 15px;
  border-color: #409eff;
  background: #409eff;
  color: #ffffff;
}

.async-erp-route-shell__pagination-shell {
  flex-direction: row;
  align-items: flex-end;
  gap: 12px;
  color: #606266;
  font-size: 14px;
}

.async-erp-route-shell__page-size,
.async-erp-route-shell__page-jumper,
.async-erp-route-shell__pager-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
  box-sizing: border-box;
}

.async-erp-route-shell__page-size {
  min-width: 110px;
  color: #606266;
}

.async-erp-route-shell__page-jumper {
  width: 56px;
}

.async-erp-route-shell__pager-button {
  min-width: 32px;
  border: 0;
  color: #a8abb2;
}

.async-erp-route-shell__pager-button--active {
  background: #409eff;
  color: #ffffff;
}

@keyframes async-erp-route-shell-spin {
  100% {
    transform: rotate(360deg);
  }
}

@media (max-width: 1280px) {
  .async-erp-route-shell__toolbar-card {
    padding: 14px;
  }

  .async-erp-route-shell__filters {
    grid-template-columns: 200px 200px 360px;
  }

  .async-erp-route-shell__control--wide {
    width: 200px;
  }

  .async-erp-route-shell__control--range {
    width: 360px;
  }

  .async-erp-route-shell__filters--inventory {
    grid-template-columns: 140px 140px 140px;
  }

  .async-erp-route-shell__toolbar {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .async-erp-route-shell__filters {
    grid-template-columns: 1fr;
  }

  .async-erp-route-shell__actions {
    width: 100%;
    justify-content: flex-end;
  }

  .async-erp-route-shell__action {
    width: 100%;
  }

  .async-erp-route-shell__control,
  .async-erp-route-shell__control--wide,
  .async-erp-route-shell__control--narrow,
  .async-erp-route-shell__control--range {
    width: 100%;
  }
}
</style>
