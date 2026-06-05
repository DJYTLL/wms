<template>
  <el-dialog
    v-model="visible"
    title="快捷组装"
    width="980px"
    append-to-body
    class="assembly-quick-dialog"
    :close-on-click-modal="!saving"
    :close-on-press-escape="!saving"
  >
    <div v-loading="loading" class="assembly-quick">
      <div class="assembly-quick__summary">
        <div>
          <span class="assembly-quick__label">成品商品</span>
          <strong>{{ form.productName || '-' }}</strong>
        </div>
        <div>
          <span class="assembly-quick__label">来源行数量</span>
          <strong>{{ formatPlainNumber(row?.qty) }}</strong>
        </div>
      </div>

      <el-form label-position="top" class="sale-form sale-form--compact">
        <div class="assembly-quick__grid">
          <el-form-item label="组装模板" required>
            <el-select
              :model-value="templateId"
              filterable
              style="width: 100%"
              placeholder="请选择组装模板"
              @change="$emit('templateChange', $event)"
            >
              <el-option
                v-for="item in getTemplates(form.productId)"
                :key="item.id"
                :label="formatTemplateLabel(item)"
                :value="item.id"
              >
                <div class="assembly-template-option">
                  <span class="assembly-template-option__name">{{ item.name }}</span>
                  <span v-if="item.remark" class="assembly-template-option__remark">{{ item.remark }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="组装数量" required>
            <DecimalInput v-model="form.finishedQty" :scale="4" @blur="$emit('qtyChange')" />
          </el-form-item>
          <el-form-item label="成品入库库位">
            <ProductStockSelect
              v-model="form.finishedStockKey"
              :product-id="form.productId"
              :warehouse-id="form.warehouseId"
              :location-id="form.locationId"
              :warehouse-options="warehouseOptions"
              :location-options="locationOptions"
              :placeholder="$t('placeholder.selectLocation')"
              @selection-change="$emit('finishedStockChange', $event)"
            />
          </el-form-item>
          <el-form-item label="人工成本">
            <DecimalInput v-model="form.laborCost" :scale="4" />
          </el-form-item>
        </div>
        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            maxlength="200"
            show-word-limit
            :autosize="{ minRows: 2, maxRows: 3 }"
          />
        </el-form-item>
      </el-form>

      <ErpDataTable
        :data="form.items"
        border
        stripe
        class="assembly-quick__items"
        :empty-text="$t('table.empty')"
        table-key="erp-sale-order-quick-assembly-items"
      >
        <ErpDataTableColumn type="index" :label="$t('table.index')" width="64" align="center" />
        <ErpDataTableColumn label="物料商品" min-width="200" column-key="product">
          <template #default="{ row: itemRow }">{{ resolveItemProductLabel(itemRow) }}</template>
        </ErpDataTableColumn>
        <ErpDataTableColumn :label="$t('field.warehouseLocation')" min-width="240" column-key="warehouseLocation">
          <template #default="{ row: itemRow }">
            <ProductStockSelect
              v-model="itemRow.stockKey"
              :product-id="itemRow.productId"
              :warehouse-id="itemRow.warehouseId"
              :location-id="itemRow.locationId"
              :warehouse-options="warehouseOptions"
              :location-options="locationOptions"
              :placeholder="$t('placeholder.selectLocation')"
              @selection-change="(payload) => $emit('itemStockChange', itemRow, payload)"
            />
          </template>
        </ErpDataTableColumn>
        <ErpDataTableColumn :label="$t('field.quantity')" width="150" column-key="quantity">
          <template #default="{ row: itemRow }">
            <DecimalInput v-model="itemRow.qty" :scale="4" />
          </template>
        </ErpDataTableColumn>
        <ErpDataTableColumn :label="$t('field.remark')" min-width="160" column-key="remark">
          <template #default="{ row: itemRow }">
            <el-input v-model="itemRow.remark" />
          </template>
        </ErpDataTableColumn>
      </ErpDataTable>
    </div>
    <template #footer>
      <el-button :disabled="saving" @click="visible = false">{{ $t('action.cancel') }}</el-button>
      <el-button :loading="saving" @click="$emit('save', false)">保存草稿</el-button>
      <el-button
        v-if="canApprove"
        type="primary"
        :loading="saving"
        @click="$emit('save', true)"
      >
        保存并审核
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue';
import DecimalInput from '@/components/DecimalInput.vue';
import ErpDataTable from '@/components/ErpDataTable.vue';

const ProductStockSelect = defineAsyncComponent(() => import('@/components/ProductStockSelect.vue'));

interface OptionItem {
  id: number;
  name: string;
  warehouseId?: number;
}

type StockSelection = { stockKey: string; warehouseId: number | null; locationId: number | null };

const props = defineProps<{
  modelValue: boolean;
  loading: boolean;
  saving: boolean;
  row: any | null;
  templateId: number | null;
  form: any;
  warehouseOptions: OptionItem[];
  locationOptions: OptionItem[];
  canApprove: boolean;
  getTemplates: (productId?: number | null) => any[];
  formatTemplateLabel: (item: any) => string;
  resolveItemProductLabel: (row: any) => string;
  formatPlainNumber: (value?: number | string | null) => string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  templateChange: [value: number | null];
  qtyChange: [];
  finishedStockChange: [payload: StockSelection];
  itemStockChange: [row: any, payload: StockSelection];
  save: [approveAfterSave: boolean];
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});
</script>

<style scoped>
.assembly-quick {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.assembly-quick__summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 18px;
  padding: 10px 12px;
  border: 1px solid #e1e9f4;
  border-radius: 8px;
  background: #f8fbff;
  color: #17233c;
}

.assembly-quick__label {
  margin-right: 8px;
  color: #6d7b91;
  font-size: 13px;
}

.assembly-quick__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  gap: 12px;
}

.assembly-quick__items {
  width: 100%;
}

.assembly-template-option {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.assembly-template-option__name {
  flex: 0 0 auto;
  color: #1f2b3d;
  font-weight: 600;
}

.assembly-template-option__remark {
  min-width: 0;
  overflow: hidden;
  color: #7a889c;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1280px) {
  .assembly-quick__grid {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }
}

@media (max-width: 768px) {
  .assembly-quick__grid {
    grid-template-columns: 1fr;
  }
}
</style>
