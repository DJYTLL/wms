<template>
  <div class="deferred-erp-list-route-stage">
    <div class="deferred-erp-list-route-host">
      <component v-if="LoadedView" :is="LoadedView" v-bind="loadedViewProps" />
    </div>
    <div
      v-if="showPlaceholder"
      class="page-shell page-shell--system deferred-erp-list-route"
    >
      <div class="page-header">
        <div class="page-title">{{ titleText }}</div>
        <ErpSaleListToolbar :show-action-placeholder="showActionPlaceholder" />
      </div>

      <div class="table-card deferred-erp-list-route__table-card">
        <div class="table-body deferred-erp-list-route__table-body">
          <ErpDataTable
            :rows="[]"
            :columns="columnDefs"
            :table-key="tableKey"
            :loading="true"
            :empty-text="loadError ? '' : t('message.loading')"
          />
          <div v-if="loadError" class="deferred-erp-list-route__error-overlay">
            <span>{{ t('message.loadFailed') }}</span>
            <button type="button" class="deferred-erp-list-route__retry" @click="loadHeavyView">
              {{ t('action.retry') }}
            </button>
          </div>
        </div>
        <div class="table-pagination deferred-erp-list-route__pagination-shell">
          <span>共 0 条</span>
          <div class="deferred-erp-list-route__page-size">20条/页</div>
          <button class="deferred-erp-list-route__pager-button" type="button" disabled>‹</button>
          <button
            class="deferred-erp-list-route__pager-button deferred-erp-list-route__pager-button--active"
            type="button"
            disabled
          >
            1
          </button>
          <button class="deferred-erp-list-route__pager-button" type="button" disabled>›</button>
          <span>前往</span>
          <div class="deferred-erp-list-route__page-jumper">1</div>
          <span>页</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, shallowRef } from 'vue';
import { useI18n } from 'vue-i18n';
import ErpDataTable, { type ErpDataTableColumn } from '@/components/ErpDataTable.vue';
import ErpSaleListToolbar from './ErpSaleListToolbar.vue';
import type { Component } from 'vue';

const props = withDefaults(defineProps<{
  title?: string;
  titleKey?: string;
  workspace?: 'draft' | 'approved';
  loader: () => Promise<Component | { default: Component }>;
  columns?: ErpDataTableColumn[];
  tableKey?: string;
  showActionPlaceholder?: boolean;
}>(), {
  title: '',
  tableKey: '',
  showActionPlaceholder: true
});

const { t } = useI18n();
const LoadedView = shallowRef<Component | null>(null);
const showPlaceholder = shallowRef(true);
const loadError = shallowRef<unknown>(null);
let disposed = false;
let active = true;
let loadingPromise: Promise<void> | null = null;

const columnDefs = computed<ErpDataTableColumn[]>(() => props.columns?.length ? props.columns : [
  {
    key: 'index',
    label: t('table.index'),
    width: 70,
    minWidth: 56,
    resizable: false,
    configurable: false
  },
  {
    key: 'orderNo',
    label: t('field.orderNo'),
    width: 160,
    minWidth: 56
  },
  {
    key: 'customer',
    label: t('field.customer'),
    width: 160,
    minWidth: 56
  },
  {
    key: 'status',
    label: t('field.status'),
    width: 120,
    minWidth: 56
  },
  {
    key: 'totalAmount',
    label: t('field.totalAmount'),
    width: 140,
    minWidth: 56
  },
  {
    key: 'createdAt',
    label: t('field.createdTime'),
    width: 180,
    minWidth: 56
  },
  {
    key: 'actions',
    label: t('table.actions'),
    width: 300,
    minWidth: 180,
    stickyRight: true,
    resizable: false,
    configurable: false
  }
]);
const titleText = computed(() => props.titleKey ? t(props.titleKey) : props.title);
const loadedViewProps = computed(() => props.workspace ? { workspace: props.workspace } : {});

const resolveLoadedComponent = (loaded: Component | { default: Component }) => (
  'default' in loaded ? loaded.default : loaded
);

const waitForStableFrame = () => new Promise<void>((resolve) => {
  if (typeof window === 'undefined' || typeof window.requestAnimationFrame !== 'function') {
    resolve();
    return;
  }

  let settled = false;
  const done = () => {
    if (settled) return;
    settled = true;
    window.clearTimeout(fallbackTimer);
    resolve();
  };
  const fallbackTimer = window.setTimeout(done, 240);
  window.requestAnimationFrame(() => {
    window.requestAnimationFrame(done);
  });
});

const loadHeavyView = async () => {
  if (LoadedView.value) {
    return;
  }
  if (loadingPromise) {
    return loadingPromise;
  }
  loadError.value = null;
  loadingPromise = (async () => {
    try {
      const loaded = await props.loader();
      if (!disposed && active) {
        LoadedView.value = resolveLoadedComponent(loaded);
        await nextTick();
        await waitForStableFrame();
        if (!disposed && active) {
          showPlaceholder.value = false;
        }
      }
    } catch (error) {
      if (!disposed) {
        loadError.value = error;
      }
    } finally {
      loadingPromise = null;
    }
  })();
  return loadingPromise;
};

onMounted(() => {
  void loadHeavyView();
});

onActivated(() => {
  active = true;
  showPlaceholder.value = true;
  void loadHeavyView();
});

onDeactivated(() => {
  active = false;
  showPlaceholder.value = true;
  LoadedView.value = null;
});

onBeforeUnmount(() => {
  disposed = true;
});
</script>

<style scoped>
.deferred-erp-list-route-stage {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 0;
}

.deferred-erp-list-route-host,
.deferred-erp-list-route {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
}

.deferred-erp-list-route-host {
  z-index: 1;
}

.deferred-erp-list-route {
  z-index: 2;
  background: #fff;
}

.deferred-erp-list-route__table-card {
  margin-top: 0;
}

.deferred-erp-list-route__table-body {
  position: relative;
  padding: 0;
}

.deferred-erp-list-route__error-overlay {
  position: absolute;
  inset: 37px 0 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  gap: 12px;
  color: #909399;
  font-size: 14px;
  z-index: 3;
}

.deferred-erp-list-route__retry {
  height: 30px;
  padding: 0 12px;
  border: 1px solid #409eff;
  border-radius: 4px;
  background: #fff;
  color: #409eff;
  cursor: pointer;
}

.deferred-erp-list-route__pagination-shell {
  flex-direction: row;
  align-items: flex-end;
  gap: 12px;
  color: #606266;
  font-size: 14px;
}

.deferred-erp-list-route__page-size,
.deferred-erp-list-route__page-jumper,
.deferred-erp-list-route__pager-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  box-sizing: border-box;
}

.deferred-erp-list-route__page-size {
  min-width: 110px;
  color: #606266;
}

.deferred-erp-list-route__page-jumper {
  width: 56px;
}

.deferred-erp-list-route__pager-button {
  min-width: 32px;
  border: 0;
  color: #a8abb2;
}

.deferred-erp-list-route__pager-button--active {
  background: #409eff;
  color: #fff;
}

</style>
