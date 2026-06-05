<template>
  <div class="table-card sale-approved-card">
    <div class="table-body">
      <ErpDataTable
        :rows="rows"
        :columns="columns"
        table-key="erp-sale-approved"
        :loading="loading"
        :empty-text="emptyText"
        :row-class-name="rowClassName"
      >
        <template #cell-index="{ index }">
          {{ index + 1 }}
        </template>
        <template #cell-customer="{ row }">
          {{ getCustomerName(row.customerId, row.customerName) }}
        </template>
        <template #cell-status="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">
            {{ formatStatus(row.status) }}
          </el-tag>
        </template>
        <template #cell-netSaleAmount="{ row }">
          {{ formatAmount(row.netSaleAmount) }}
        </template>
        <template #cell-netGrossProfit="{ row }">
          {{ formatAmount(row.netGrossProfit) }}
        </template>
        <template #cell-receivableStatus="{ row }">
          <el-tag :type="financeStatusTagType(row.receivableStatus)" size="small">
            {{ formatFinanceStatus(row.receivableStatus, row.receivableUnpaidAmount) }}
          </el-tag>
        </template>
        <template #cell-returnStatus="{ row }">
          <div class="return-tag-list">
            <template v-if="Number(row.approvedReturnCount || 0) > 0">
              <el-tag
                v-for="returnIndex in buildReturnTagIndexes(row.approvedReturnCount)"
                :key="`${row.id}-return-${returnIndex}`"
                type="warning"
                size="small"
                class="return-tag-item"
              >
                {{ `退货${returnIndex + 1}` }}
              </el-tag>
            </template>
            <el-tag v-else type="info" size="small">
              {{ formatReturnStatus(row.approvedReturnCount) }}
            </el-tag>
          </div>
        </template>
        <template #cell-createdAt="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
        <template #cell-actions="{ row }">
          <el-button link type="primary" size="small" @click="$emit('view', row)">
            {{ $t('action.view') }}
          </el-button>
          <el-button link type="primary" size="small" v-permission="'erp-sale-approved:print'" @click="openPrintPage(row)">
            {{ $t('action.print') }}
          </el-button>
          <el-button v-if="canCopy" link type="primary" size="small" @click="$emit('copy', row)">
            {{ $t('action.copy') }}
          </el-button>
          <el-button
            v-if="row.status === 'APPROVED'"
            link
            type="danger"
            size="small"
            v-permission="'erp-sale-approved:cancel'"
            @click="$emit('cancel', row)"
          >
            {{ $t('action.cancel') }}
          </el-button>
          <el-button
            v-if="row.status === 'APPROVED'"
            link
            type="danger"
            size="small"
            v-permission="'erp-sale-approved:redflush'"
            @click="$emit('red-flush', row)"
          >
            {{ $t('action.redFlush') }}
          </el-button>
        </template>
      </ErpDataTable>
    </div>
    <div class="table-pagination">
      <div class="sale-summary-bar">
        <div class="sale-summary-bar__items">
          <div class="sale-summary-item">
            <span class="sale-summary-item__label">{{ summaryLabel('saleAmount') }}</span>
            <span class="sale-summary-item__value">{{ formatAmount(summary.saleAmountTotal) }}</span>
          </div>
          <div class="sale-summary-item">
            <span class="sale-summary-item__label">{{ summaryLabel('returnAmount') }}</span>
            <span class="sale-summary-item__value">{{ formatAmount(summary.returnAmountTotal) }}</span>
          </div>
          <div class="sale-summary-item">
            <span class="sale-summary-item__label">{{ summaryLabel('netSaleAmount') }}</span>
            <span class="sale-summary-item__value">{{ formatAmount(summary.netSaleAmountTotal) }}</span>
          </div>
          <div v-if="canShowProfit" class="sale-summary-item">
            <span class="sale-summary-item__label">{{ summaryLabel('netGrossProfit') }}</span>
            <span class="sale-summary-item__value">{{ formatAmount(summary.netGrossProfitTotal) }}</span>
          </div>
        </div>
      </div>
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :current-page="page"
        :page-size="size"
        :page-sizes="[10, 20, 50, 100]"
        @size-change="$emit('size-change', $event)"
        @current-change="$emit('page-change', $event)"
      />
    </div>

    <PrintPreviewDialog
      v-if="printDialogVisible"
      v-model="printDialogVisible"
      doc-type="SALE_ORDER_APPROVED"
      :doc-id="printDocId"
      :title="$t('page.erpSaleOrderPrint')"
    />
  </div>
</template>

<script setup lang="ts">
import { defineAsyncComponent, onMounted, ref } from 'vue';
import ErpDataTable, { type ErpDataTableColumn } from '@/components/ErpDataTable.vue';
import { markErpNavigationPerf } from '@/utils/erpNavigationPerfTrace';

const PrintPreviewDialog = defineAsyncComponent(() => import('@/components/PrintPreviewDialog.vue'));
markErpNavigationPerf('sale-order-list:deferred-panel-setup', {
  page: 'approved'
});

interface SaleOrder {
  id: number;
  status: string;
  customerId?: number;
  customerName?: string;
  netSaleAmount?: number;
  netGrossProfit?: number;
  receivableStatus?: string;
  receivableUnpaidAmount?: number;
  approvedReturnCount?: number;
  createdAt?: string;
}

interface SaleOrderSummary {
  saleAmountTotal: number;
  returnAmountTotal: number;
  netSaleAmountTotal: number;
  netGrossProfitTotal: number;
  summaryMode: 'page' | 'range';
}

defineProps<{
  rows: SaleOrder[];
  columns: ErpDataTableColumn[];
  loading: boolean;
  emptyText: string;
  total: number;
  page: number;
  size: number;
  summary: SaleOrderSummary;
  canShowProfit: boolean;
  canCopy: boolean;
  formatAmount: (value?: number | string) => string;
  formatStatus: (status: string) => string;
  statusTagType: (status: string) => string;
  formatFinanceStatus: (status?: string, unpaidAmount?: number) => string;
  financeStatusTagType: (status?: string) => string;
  formatReturnStatus: (count?: number) => string;
  formatDateTime: (value?: string) => string;
  getCustomerName: (id?: number, name?: string) => string;
  buildReturnTagIndexes: (count?: number) => number[];
  rowClassName: (scope: { row: SaleOrder; index: number }) => string;
  summaryLabel: (key: 'saleAmount' | 'returnAmount' | 'netSaleAmount' | 'netGrossProfit') => string;
}>();

defineEmits<{
  (event: 'page-change', page: number): void;
  (event: 'size-change', size: number): void;
  (event: 'view', row: SaleOrder): void;
  (event: 'copy', row: SaleOrder): void;
  (event: 'cancel', row: SaleOrder): void;
  (event: 'red-flush', row: SaleOrder): void;
}>();

const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);

const openPrintPage = (row: SaleOrder) => {
  printDocId.value = row.id;
  printDialogVisible.value = true;
};

onMounted(() => {
  markErpNavigationPerf('sale-order-list:deferred-panel-mounted', {
    page: 'approved'
  });
});
</script>

<style scoped>
:deep(.row-red-flushed > td) {
  background-color: #fff1f0 !important;
}

:deep(.row-red-flushed:hover > td) {
  background-color: #fff1f0 !important;
}

.return-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.return-tag-item {
  cursor: default;
}

.sale-summary-bar {
  width: 100%;
  padding: 0 0 10px;
  box-sizing: border-box;
}

.sale-summary-bar__items {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 20px;
}

.sale-summary-item {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  white-space: nowrap;
}

.sale-summary-item__label {
  color: #6b7280;
  font-size: 12px;
}

.sale-summary-item__value {
  color: #111827;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.2;
}

@media (max-width: 768px) {
  .sale-summary-bar__items {
    gap: 10px 14px;
  }
}
</style>
