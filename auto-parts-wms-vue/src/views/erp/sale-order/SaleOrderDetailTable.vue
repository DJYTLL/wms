<template>
  <div class="table-card sale-detail-card">
    <div class="table-body sale-detail-body">
      <div class="detail-section">
        <div class="card-section-header detail-header">
          <h4>{{ $t('section.saleDetailInfo') }}</h4>
          <div v-if="!isReadOnly" class="detail-header-actions">
            <el-button
              class="detail-toolbar-button detail-toolbar-button--primary"
              :icon="Plus"
              @click="addItem"
            >
              {{ $t('action.addItem') }}
            </el-button>
            <el-button
              class="detail-toolbar-button"
              :icon="Delete"
              :disabled="!selectedItems.length"
              @click="removeSelectedItems"
            >
              {{ $t('action.deleteSelected') }}
            </el-button>
          </div>
        </div>
        <div class="detail-table-wrapper">
          <ErpDataTable
            :data="formData.items"
            style="width: 100%"
            border
            @row-click="handleRowClick"
            @selection-change="handleItemSelectionChange"
            :header-cell-style="{ textAlign: 'center' }"
            table-key="erp-sale-order-form"
          >
            <ErpDataTableColumn v-if="!isReadOnly" type="selection" width="52" align="center" />
            <ErpDataTableColumn type="index" :label="$t('table.index')" width="72" align="center" />
            <ErpDataTableColumn :label="$t('field.product')" min-width="240" column-key="product">
              <template #header>
                <span class="required-table-label">{{ $t('field.product') }}</span>
              </template>
              <template #default="{ row, $index }">
                <div class="product-cell">
                  <span v-if="isReadOnly" class="product-cell__label">{{ resolveProductLabel(row) }}</span>
                  <el-select
                    v-else
                    :ref="(el: any) => setProductSelectRef(el, $index)"
                    :key="formData.customerId ?? 'no-customer'"
                    v-model="row.productId"
                    filterable
                    remote
                    clearable
                    reserve-keyword
                    :automatic-dropdown="false"
                    class="product-cell__select"
                    :placeholder="$t('placeholder.selectProduct')"
                    :disabled="!formData.customerId"
                    :remote-method="searchProducts"
                    :loading="productSearchLoading"
                    @change="handleProductChange(row)"
                  >
                    <el-option
                      v-for="item in getSelectableProductOptions(row.productId)"
                      :key="item.id"
                      :label="item.name"
                      :value="item.id"
                    >
                      <div class="product-option-row">
                        <span class="product-option-row__name">{{ item.name }}</span>
                        <el-button
                          v-if="canEditProductInline"
                          class="product-option-row__edit"
                          link
                          type="primary"
                          title="编辑商品详情"
                          @mousedown.prevent.stop
                          @click.prevent.stop="openProductEditFromOption(row, item.id)"
                        >
                          <el-icon><EditPen /></el-icon>
                        </el-button>
                      </div>
                    </el-option>
                  </el-select>
                  <el-tag
                    class="history-inline history-tag history-tag--inline"
                    size="small"
                    :type="row.productId ? 'primary' : 'info'"
                    :title="$t('action.detail')"
                    @click.stop="row.productId && openHistoryForRow(row)"
                  >
                    <el-icon class="history-icon"><View /></el-icon>
                  </el-tag>
                  <el-tooltip
                    v-if="canUseQuickAssembly && row.productId && isAssemblyProduct(row)"
                    content="快捷组装"
                    placement="top"
                  >
                    <el-button
                      class="assembly-inline-button"
                      link
                      type="success"
                      @click.stop="openAssemblyForRow(row)"
                    >
                      <el-icon><Operation /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </template>
            </ErpDataTableColumn>
            <ErpDataTableColumn :label="$t('field.warehouseLocation')" min-width="220" column-key="warehouseLocation">
              <template #header>
                <span class="required-table-label">{{ $t('field.warehouseLocation') }}</span>
              </template>
              <template #default="{ row }">
                <span v-if="isReadOnly">
                  {{ resolveWarehouseLabel(row) }} / {{ resolveLocationLabel(row) }}
                </span>
                <el-select
                  v-else
                  v-model="row.stockKey"
                  filterable
                  clearable
                  style="width: 100%"
                  :placeholder="$t('placeholder.selectLocation')"
                  :disabled="!row.productId"
                  @change="handleStockLocationChange(row)"
                >
                  <el-option
                    v-for="item in getStockOptionsForRow(row)"
                    :key="item.key"
                    :label="item.searchLabel"
                    :value="item.key"
                  >
                    <div class="stock-option">
                      <span class="stock-option__name">{{ item.label }}</span>
                      <span class="stock-option__qty">
                        {{ $t('field.qtyOnHand') }}: {{ item.qtyOnHand }}
                        · {{ $t('field.qtyAvailable') }}: {{ item.qtyAvailable }}
                        · {{ $t('field.qtyLocked') }}: {{ item.qtyLocked }}
                      </span>
                    </div>
                  </el-option>
                </el-select>
              </template>
            </ErpDataTableColumn>
            <ErpDataTableColumn :label="$t('field.quantity')" width="140" column-key="quantity">
              <template #header>
                <span class="required-table-label">{{ $t('field.quantity') }}</span>
              </template>
              <template #default="{ row }">
                <span v-if="isReadOnly" class="readonly-cell">{{ formatPlainNumber(row.qty) }}</span>
                <DecimalInput v-else v-model="row.qty" :scale="4" />
              </template>
            </ErpDataTableColumn>
            <ErpDataTableColumn :label="$t('field.price')" width="140" column-key="price">
              <template #header>
                <span class="required-table-label">{{ $t('field.price') }}</span>
              </template>
              <template #default="{ row }">
                <span v-if="isReadOnly" class="readonly-cell">{{ formatMoney(row.price) }}</span>
                <DecimalInput v-else v-model="row.price" :scale="4" />
              </template>
            </ErpDataTableColumn>
            <ErpDataTableColumn :label="$t('field.lineTotal')" width="140" column-key="lineAmount">
              <template #default="{ row }">
                {{ formatMoney(calcLineAmount(row)) }}
              </template>
            </ErpDataTableColumn>
            <ErpDataTableColumn v-if="canShowDiscountAllocated" :label="$t('field.discountAllocated')" width="140" column-key="discountAllocated">
              <template #default="{ row }">
                {{ formatMoney(calcLineDiscount(row)) }}
              </template>
            </ErpDataTableColumn>
            <ErpDataTableColumn v-if="canShowProfit" :label="$t('field.profit')" min-width="160" column-key="profit">
              <template #default="{ row }">
                {{ formatProfitCell(row) }}
              </template>
            </ErpDataTableColumn>
            <ErpDataTableColumn :label="$t('field.remark')" min-width="180" column-key="remark">
              <template #default="{ row }">
                <span v-if="isReadOnly" class="readonly-cell">{{ row.remark || '-' }}</span>
                <el-input v-else v-model="row.remark" :placeholder="$t('field.remark')" />
              </template>
            </ErpDataTableColumn>
            <ErpDataTableColumn v-if="!isReadOnly" :label="$t('table.actions')" width="88" align="center" fixed="right" column-key="actions">
              <template #default="{ $index }">
                <el-button class="row-delete-button" link type="danger" @click.stop="removeItem($index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </ErpDataTableColumn>
          </ErpDataTable>
        </div>
        <div class="detail-footer">
          <div v-if="!isReadOnly" class="detail-actions">
            <el-button class="detail-add-item-button" type="primary" plain :icon="Plus" :disabled="isReadOnly" @click="addItem">
              {{ $t('action.addItem') }}
            </el-button>
          </div>
          <div class="detail-summary">
            <div class="summary-item">{{ $t('field.productSubtotal') }}: {{ formatMoney(totalSummary.amount) }}</div>
            <div class="summary-item">{{ $t('field.discountAmount') }}: {{ formatMoney(getDiscountAmount()) }}</div>
            <div class="summary-item summary-item--total">
              {{ $t('field.totalAmount') }}:
              <strong>¥ {{ formatMoney(totalSummary.netAmount) }}</strong>
            </div>
            <div class="summary-item" v-if="canShowProfit">{{ $t('field.totalProfit') }}: {{ totalProfitText }}</div>
            <div class="summary-item" v-if="canShowProfit">{{ $t('field.totalProfitRate') }}: {{ totalProfitRateText }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Delete, EditPen, Operation, Plus, View } from '@element-plus/icons-vue';
import DecimalInput from '@/components/DecimalInput.vue';
import ErpDataTable from '@/components/ErpDataTable.vue';
import ErpDataTableColumn from '@/components/ErpDataTableColumn.vue';
import type { ProductOption, SaleOrderFormData, SaleOrderItem, StockOption } from './saleOrderTypes';

interface SaleOrderSummary {
  amount: number;
  netAmount: number;
}

defineProps<{
  addItem: () => void;
  calcLineAmount: (row: SaleOrderItem) => number | null;
  calcLineDiscount: (row: SaleOrderItem) => number;
  canEditProductInline: boolean;
  canShowDiscountAllocated: boolean;
  canShowProfit: boolean;
  canUseQuickAssembly: boolean;
  formatMoney: (value: number | string | null) => string;
  formatPlainNumber: (value: number | string | null | undefined) => string;
  formatProfitCell: (row: SaleOrderItem) => string;
  formData: SaleOrderFormData;
  getDiscountAmount: () => number;
  getSelectableProductOptions: (selectedProductId?: number | null) => ProductOption[];
  getStockOptionsForRow: (row: SaleOrderItem) => StockOption[];
  handleItemSelectionChange: (rows: SaleOrderItem[]) => void;
  handleProductChange: (row: SaleOrderItem) => void;
  handleRowClick: (_row: SaleOrderItem, _column?: unknown, _event?: Event) => void;
  handleStockLocationChange: (row: SaleOrderItem) => void;
  isAssemblyProduct: (row: SaleOrderItem) => boolean;
  isReadOnly: boolean;
  openAssemblyForRow: (row: SaleOrderItem) => void;
  openHistoryForRow: (row: SaleOrderItem) => void;
  openProductEditFromOption: (row: SaleOrderItem, productId: number) => void;
  productSearchLoading: boolean;
  removeItem: (index: number) => void;
  removeSelectedItems: () => void;
  resolveLocationLabel: (row: SaleOrderItem) => string;
  resolveProductLabel: (row: SaleOrderItem) => string;
  resolveWarehouseLabel: (row: SaleOrderItem) => string;
  searchProducts: (keyword: string) => void;
  selectedItems: SaleOrderItem[];
  setProductSelectRef: (el: any, index: number) => void;
  totalProfitRateText: string;
  totalProfitText: string;
  totalSummary: SaleOrderSummary;
}>();
</script>

<style scoped>
.card-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.card-section-header h4 {
  margin: 0;
  font-size: 15px;
  color: #1f2d3d;
}

.sale-detail-card {
  flex: 0 0 auto;
  min-height: 0;
  padding: 18px 22px 18px;
}

.sale-detail-body {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0;
  overflow: visible;
}

.detail-section {
  display: flex;
  flex-direction: column;
  min-height: 0;
  gap: 16px;
}

.detail-header {
  margin-bottom: 8px;
}

.detail-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-toolbar-button {
  height: 34px;
  border-radius: 8px;
  font-weight: 600;
}

.detail-toolbar-button--primary {
  background: #edf5ff;
  border-color: #bad7ff;
  color: #1668dc;
}

.detail-table-wrapper {
  min-height: 0;
  overflow: visible;
}

.required-table-label::before {
  content: '*';
  color: #f56c6c;
  margin-right: 4px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.product-cell__label {
  flex: 1 1 auto;
  min-width: 0;
}

.product-cell__select {
  flex: 1 1 auto;
  min-width: 0;
}

.product-option-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
}

.product-option-row__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-option-row__edit {
  flex: 0 0 auto;
}

.stock-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.35;
}

.stock-option__name {
  font-weight: 600;
}

.stock-option__qty {
  font-size: 12px;
  color: #8c8c8c;
}

.history-inline {
  flex: 0 0 auto;
}

.history-tag {
  cursor: pointer;
  user-select: none;
}

.history-tag--inline {
  height: 28px;
  min-width: 28px;
  justify-content: center;
  padding: 0 7px;
}

.history-icon {
  margin-right: 0;
}

.assembly-inline-button {
  flex: 0 0 auto;
}

.history-tag.el-tag--info {
  cursor: not-allowed;
  opacity: 0.6;
}

.readonly-cell {
  color: #2c3e50;
}

.detail-footer {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-add-item-button {
  border-radius: 8px;
  font-weight: 600;
}

.detail-summary {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px 18px;
  color: #2c3e50;
}

.summary-item {
  font-size: 14px;
}

.summary-item--total strong {
  color: #d4380d;
  font-size: 18px;
}
</style>
