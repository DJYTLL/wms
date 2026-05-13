<template>
  <el-select
    :model-value="innerValue"
    filterable
    clearable
    :style="selectStyle"
    :disabled="disabled || !productId"
    :placeholder="placeholder"
    popper-class="product-stock-select-popper"
    @update:model-value="handleUpdate"
    @visible-change="handleVisibleChange"
  >
    <el-option
      v-for="item in stockOptions"
      :key="item.key"
      :label="item.searchLabel"
      :value="item.key"
    >
      <div class="stock-option">
        <span class="stock-option__name">{{ item.label }}</span>
        <span class="stock-option__qty">
          {{ t('field.qtyOnHand') }}: {{ item.qtyOnHand }}
          · {{ t('field.qtyAvailable') }}: {{ item.qtyAvailable }}
          · {{ t('field.qtyLocked') }}: {{ item.qtyLocked }}
        </span>
      </div>
    </el-option>
  </el-select>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';

interface OptionItem {
  id: number;
  name: string;
  warehouseId?: number;
}

interface StockOption {
  key: string;
  warehouseId: number | null;
  locationId: number | null;
  warehouseName: string;
  locationName: string;
  qtyOnHand: number;
  qtyAvailable: number;
  qtyLocked: number;
  label: string;
  searchLabel: string;
}

const props = withDefaults(defineProps<{
  modelValue?: string;
  productId?: number | null;
  warehouseId?: number | null;
  locationId?: number | null;
  warehouseOptions?: OptionItem[];
  locationOptions?: OptionItem[];
  disabled?: boolean;
  placeholder?: string;
  selectStyle?: string;
}>(), {
  modelValue: '',
  productId: null,
  warehouseId: null,
  locationId: null,
  warehouseOptions: () => [],
  locationOptions: () => [],
  disabled: false,
  placeholder: '',
  selectStyle: 'width: 100%'
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
  'selection-change': [payload: { stockKey: string; warehouseId: number | null; locationId: number | null }];
}>();

const { t } = useI18n();

const stockOptionsMap = ref<Record<number, StockOption[]>>({});
const innerValue = computed(() => props.modelValue || '');

const buildStockKey = (warehouseId: number | null | undefined, locationId: number | null | undefined) => {
  const w = warehouseId == null ? 0 : warehouseId;
  const l = locationId == null ? 0 : locationId;
  return `${w}:${l}`;
};

const parseStockKey = (stockKey?: string) => {
  if (!stockKey) return { warehouseId: null, locationId: null };
  const [warehouseRaw, locationRaw] = stockKey.split(':');
  const warehouseId = Number(warehouseRaw);
  const locationId = Number(locationRaw);
  return {
    warehouseId: Number.isNaN(warehouseId) || warehouseId === 0 ? null : warehouseId,
    locationId: Number.isNaN(locationId) || locationId === 0 ? null : locationId
  };
};

const normalizeStockOption = (option: any): StockOption => {
  const warehouseId = option.warehouseId ?? null;
  const locationId = option.locationId ?? null;
  const warehouseName = option.warehouseName || '-';
  const locationName = option.locationName || t('field.unassignedLocation');
  const qtyOnHand = Number(option.qtyOnHand ?? 0);
  const qtyAvailable = Number(option.qtyAvailable ?? qtyOnHand);
  const qtyLocked = Number(option.qtyLocked ?? 0);
  const baseLabel = `${warehouseName} / ${locationName}`;
  return {
    key: buildStockKey(warehouseId, locationId),
    warehouseId,
    locationId,
    warehouseName,
    locationName,
    qtyOnHand,
    qtyAvailable,
    qtyLocked,
    label: baseLabel,
    searchLabel: `${baseLabel} ${qtyOnHand} ${qtyAvailable} ${qtyLocked}`
  };
};

const buildFallbackStockOption = (): StockOption | null => {
  if (!props.warehouseId && !props.locationId) return null;
  const warehouse = props.warehouseOptions.find(item => item.id === props.warehouseId);
  const location = props.locationOptions.find(item => item.id === props.locationId);
  const warehouseName = warehouse?.name || '-';
  const locationName = location?.name || t('field.unassignedLocation');
  return {
    key: buildStockKey(props.warehouseId ?? null, props.locationId ?? null),
    warehouseId: props.warehouseId ?? null,
    locationId: props.locationId ?? null,
    warehouseName,
    locationName,
    qtyOnHand: 0,
    qtyAvailable: 0,
    qtyLocked: 0,
    label: `${warehouseName} / ${locationName}`,
    searchLabel: `${warehouseName} / ${locationName} 0 0 0`
  };
};

const fetchStockOptions = async (force = false) => {
  if (!props.productId) return;
  if (!force && stockOptionsMap.value[props.productId]) return;
  const res: any = await request.get('/erp/stock/balances/by-product', { params: { productId: props.productId } });
  const data = res.data.data || [];
  stockOptionsMap.value = {
    ...stockOptionsMap.value,
    [props.productId]: data.map(normalizeStockOption)
  };
};

const stockOptions = computed(() => {
  if (!props.productId) return [];
  const options = stockOptionsMap.value[props.productId] || [];
  if (!props.warehouseId && !props.locationId) {
    return options;
  }
  const key = buildStockKey(props.warehouseId ?? null, props.locationId ?? null);
  if (options.some(item => item.key === key)) {
    return options;
  }
  const fallback = buildFallbackStockOption();
  return fallback ? [fallback, ...options] : options;
});

const emitSelection = (stockKey: string) => {
  if (!stockKey) {
    emit('selection-change', { stockKey: '', warehouseId: null, locationId: null });
    return;
  }
  const selected = stockOptions.value.find(item => item.key === stockKey);
  if (selected) {
    emit('selection-change', {
      stockKey,
      warehouseId: selected.warehouseId ?? null,
      locationId: selected.locationId ?? null
    });
    return;
  }
  const parsed = parseStockKey(stockKey);
  emit('selection-change', {
    stockKey,
    warehouseId: parsed.warehouseId,
    locationId: parsed.locationId
  });
};

const handleUpdate = (value: string) => {
  emit('update:modelValue', value || '');
  emitSelection(value || '');
};

const handleVisibleChange = async (visible: boolean) => {
  if (!visible || !props.productId) return;
  await fetchStockOptions(true);
};

watch(() => props.productId, async (productId) => {
  if (!productId) {
    return;
  }
  await fetchStockOptions();
}, { immediate: true });
</script>

<style scoped>
.stock-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.4;
}

.stock-option__name {
  color: #1f2b3d;
}

.stock-option__qty {
  color: #6d7b91;
  font-size: 12px;
}
</style>

<style>
.product-stock-select-popper .el-select-dropdown__item {
  height: auto;
  min-height: 56px;
  padding-top: 8px;
  padding-bottom: 8px;
  line-height: 1.4;
  display: flex;
  align-items: center;
}

.product-stock-select-popper .el-select-dropdown__item.hover,
.product-stock-select-popper .el-select-dropdown__item.is-hovering {
  background-color: #f5f9ff;
}

.product-stock-select-popper .stock-option {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 2px;
  white-space: normal;
}

.product-stock-select-popper .stock-option__name {
  color: #1f2b3d;
  font-weight: 500;
}

.product-stock-select-popper .stock-option__qty {
  color: #6d7b91;
  font-size: 12px;
  line-height: 1.35;
  white-space: normal;
}
</style>
