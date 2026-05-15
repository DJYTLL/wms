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
    <el-option
      v-if="allowManualLocationSelect"
      :key="MANUAL_ENTRY_KEY"
      :label="t('action.otherLocation')"
      :value="MANUAL_ENTRY_KEY"
    >
      <div class="stock-option stock-option--manual-entry">
        <span class="stock-option__name">
          {{ t('action.otherLocation') }}
          <span class="stock-option__tag">{{ t('action.otherLocation') }}</span>
        </span>
        <span class="stock-option__qty">{{ t('message.selectOtherStockLocation') }}</span>
      </div>
    </el-option>
  </el-select>
  <el-dialog
    v-model="manualLocationDialogVisible"
    :title="t('message.selectOtherStockLocation')"
    width="520px"
    append-to-body
  >
    <div class="product-stock-select__dialog">
      <el-form label-position="top">
        <el-form-item :label="t('field.warehouse')" required>
          <el-select v-model="manualWarehouseId" filterable clearable style="width: 100%">
            <el-option
              v-for="item in warehouseOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('field.location')">
          <el-select v-model="manualLocationId" filterable clearable style="width: 100%">
            <el-option
              v-for="item in manualLocationOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="manualLocationDialogVisible = false">{{ t('action.cancel') }}</el-button>
      <el-button type="primary" :disabled="!manualWarehouseId" @click="confirmManualLocationSelection">
        {{ t('action.confirm') }}
      </el-button>
    </template>
  </el-dialog>
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
  allowManualLocationSelect?: boolean;
}>(), {
  modelValue: '',
  productId: null,
  warehouseId: null,
  locationId: null,
  warehouseOptions: () => [],
  locationOptions: () => [],
  disabled: false,
  placeholder: '',
  selectStyle: 'width: 100%',
  allowManualLocationSelect: false
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
  'selection-change': [payload: { stockKey: string; warehouseId: number | null; locationId: number | null }];
}>();

const { t } = useI18n();

const stockOptionsMap = ref<Record<number, StockOption[]>>({});
const manualLocationDialogVisible = ref(false);
const manualWarehouseId = ref<number | null>(null);
const manualLocationId = ref<number | null>(null);
const innerValue = computed(() => props.modelValue || '');
const warehouseOptions = computed(() => props.warehouseOptions || []);
const locationOptions = computed(() => props.locationOptions || []);
const MANUAL_ENTRY_KEY = '__manual_location__';

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
  const result = [...options];
  const key = buildStockKey(props.warehouseId ?? null, props.locationId ?? null);
  if (!result.some(item => item.key === key)) {
    const fallback = buildFallbackStockOption();
    if (fallback) {
      result.unshift(fallback);
    }
  }
  return result;
});

const manualLocationOptions = computed(() => {
  if (!manualWarehouseId.value) return [];
  return locationOptions.value.filter(item => item.warehouseId === manualWarehouseId.value);
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
  if (value === MANUAL_ENTRY_KEY) {
    openManualLocationDialog();
    return;
  }
  emit('update:modelValue', value || '');
  emitSelection(value || '');
};

const handleVisibleChange = async (visible: boolean) => {
  if (!visible) return;
  if (!props.productId) return;
  await fetchStockOptions(true);
};

const openManualLocationDialog = () => {
  manualWarehouseId.value = props.warehouseId ?? null;
  manualLocationId.value = props.locationId ?? null;
  manualLocationDialogVisible.value = true;
};

const confirmManualLocationSelection = () => {
  if (!manualWarehouseId.value) return;
  const stockKey = buildStockKey(manualWarehouseId.value, manualLocationId.value ?? null);
  emit('update:modelValue', stockKey);
  emitSelection(stockKey);
  manualLocationDialogVisible.value = false;
};

watch(() => props.productId, async (productId) => {
  if (!productId) {
    return;
  }
  await fetchStockOptions();
}, { immediate: true });

watch(manualWarehouseId, (warehouseId) => {
  if (!warehouseId) {
    manualLocationId.value = null;
    return;
  }
  if (!manualLocationOptions.value.some(item => item.id === manualLocationId.value)) {
    manualLocationId.value = null;
  }
});
</script>

<style scoped>
.product-stock-select__dialog {
  padding-top: 4px;
}

.stock-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.4;
}

.stock-option__name {
  color: #1f2b3d;
}

.stock-option__tag {
  display: inline-flex;
  align-items: center;
  margin-left: 8px;
  padding: 1px 6px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 11px;
  font-weight: 600;
}

.stock-option__qty {
  color: #6d7b91;
  font-size: 12px;
}

.stock-option--manual-entry .stock-option__name {
  color: #2563eb;
  font-weight: 600;
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
