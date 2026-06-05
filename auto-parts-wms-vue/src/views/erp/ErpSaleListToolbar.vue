<template>
  <div class="erp-sale-list-toolbar">
    <div class="erp-sale-list-toolbar__content">
      <div class="erp-sale-list-toolbar__filters">
        <slot name="filters">
          <el-input
            :placeholder="t('action.search')"
            class="erp-toolbar__search erp-toolbar__search--wide"
            clearable
            disabled
          />
          <FuzzyProductSelect
            :model-value="null"
            :options="[]"
            :placeholder="t('field.customer')"
            class="erp-toolbar__search erp-toolbar__search--wide"
            disabled
          />
          <el-date-picker
            type="datetimerange"
            :start-placeholder="t('field.startTime')"
            :end-placeholder="t('field.endTime')"
            class="erp-toolbar__date-range table-date-range table-date-range--compact"
            disabled
          />
        </slot>
      </div>
      <div class="erp-sale-list-toolbar__actions">
        <slot name="actions">
          <el-button
            v-if="showActionPlaceholder"
            type="primary"
            disabled
          >
            {{ t('action.add') }}
          </el-button>
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';

withDefaults(defineProps<{
  showActionPlaceholder?: boolean;
}>(), {
  showActionPlaceholder: true
});

const { t } = useI18n();
</script>

<style scoped>
.erp-sale-list-toolbar {
  width: 100%;
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  box-sizing: border-box;
}

.erp-sale-list-toolbar__content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
  width: 100%;
}

.erp-sale-list-toolbar__filters {
  display: grid;
  grid-template-columns: 220px 220px 380px;
  align-items: center;
  justify-content: start;
  gap: 12px;
  min-width: 0;
}

.erp-sale-list-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: nowrap;
}

:deep(.erp-toolbar__search--wide) {
  width: 220px;
}

:deep(.erp-toolbar__date-range) {
  width: 380px;
}

:deep(.table-date-range--compact) {
  flex: 0 0 380px;
}

:deep(.table-date-range--compact.el-range-editor) {
  width: 380px !important;
  min-width: 380px !important;
}

:deep(.table-date-range--compact .el-range-input) {
  width: 132px;
}

@media (max-width: 1280px) {
  .erp-sale-list-toolbar {
    padding: 14px;
  }

  .erp-sale-list-toolbar__content {
    grid-template-columns: 1fr;
  }

  .erp-sale-list-toolbar__filters {
    grid-template-columns: 200px 200px 360px;
  }

  .erp-sale-list-toolbar__actions {
    justify-content: flex-start;
  }

  :deep(.erp-toolbar__search--wide) {
    width: 200px;
  }

  :deep(.erp-toolbar__date-range) {
    width: 360px;
  }

  :deep(.table-date-range--compact) {
    flex-basis: 360px;
  }

  :deep(.table-date-range--compact.el-range-editor) {
    width: 360px !important;
    min-width: 360px !important;
  }
}

@media (max-width: 768px) {
  .erp-sale-list-toolbar__filters {
    grid-template-columns: 1fr;
  }

  .erp-sale-list-toolbar__actions {
    width: 100%;
    justify-content: flex-end;
  }

  :deep(.erp-toolbar__search--wide),
  :deep(.erp-toolbar__date-range) {
    width: 100%;
  }

  :deep(.table-date-range--compact) {
    flex-basis: 100%;
  }

  :deep(.table-date-range--compact.el-range-editor) {
    width: 100% !important;
    min-width: 0 !important;
  }
}
</style>
