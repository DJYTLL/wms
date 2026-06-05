<template>
  <div class="table-card">
    <div class="table-body">
      <ErpDataTable
        :rows="rows"
        :columns="columns"
        table-key="erp-sale-return-draft"
        :loading="loading"
        :empty-text="emptyText"
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
        <template #cell-refundStatus="{ row }">
          <el-tag :type="financeStatusTagType(row.refundStatus)" size="small">
            {{ formatFinanceStatus(row.refundStatus, row.refundUnpaidAmount) }}
          </el-tag>
        </template>
        <template #cell-createdAt="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
        <template #cell-actions="{ row }">
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="primary"
            size="small"
            v-permission="'erp-sale-return-draft:edit'"
            @click="$emit('edit', row)"
          >
            {{ $t('action.edit') }}
          </el-button>
          <el-button link type="primary" size="small" v-permission="'erp-sale-return-draft:print'" @click="openPrintPage(row)">
            {{ $t('action.print') }}
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="success"
            size="small"
            v-permission="'erp-sale-return-draft:approve'"
            @click="$emit('approve', row)"
          >
            {{ $t('action.approve') }}
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="danger"
            size="small"
            v-permission="'erp-sale-return-draft:delete'"
            @click="$emit('delete', row)"
          >
            {{ $t('action.delete') }}
          </el-button>
        </template>
      </ErpDataTable>
    </div>
    <div class="table-pagination">
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
      doc-type="SALE_RETURN_DRAFT"
      :doc-id="printDocId"
      :title="$t('page.erpSaleReturnPrint')"
    />
  </div>
</template>

<script setup lang="ts">
import { defineAsyncComponent, onMounted, ref } from 'vue';
import ErpDataTable, { type ErpDataTableColumn } from '@/components/ErpDataTable.vue';
import { markErpNavigationPerf } from '@/utils/erpNavigationPerfTrace';

const PrintPreviewDialog = defineAsyncComponent(() => import('@/components/PrintPreviewDialog.vue'));
markErpNavigationPerf('sale-order-list:deferred-panel-setup', {
  page: 'return-draft'
});

interface SaleReturn {
  id: number;
  orderNo?: string;
  customerId?: number;
  customerName?: string;
  status: string;
  totalAmount?: number;
  refundStatus?: string;
  refundUnpaidAmount?: number;
  createdAt?: string;
}

defineProps<{
  rows: SaleReturn[];
  columns: ErpDataTableColumn[];
  loading: boolean;
  emptyText: string;
  total: number;
  page: number;
  size: number;
  formatStatus: (status: string) => string;
  statusTagType: (status: string) => string;
  formatFinanceStatus: (status?: string, unpaidAmount?: number) => string;
  financeStatusTagType: (status?: string) => string;
  formatDateTime: (value?: string) => string;
  getCustomerName: (id?: number, name?: string) => string;
}>();

defineEmits<{
  (event: 'page-change', page: number): void;
  (event: 'size-change', size: number): void;
  (event: 'edit', row: SaleReturn): void;
  (event: 'approve', row: SaleReturn): void;
  (event: 'delete', row: SaleReturn): void;
}>();

const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);

const openPrintPage = (row: SaleReturn) => {
  printDocId.value = row.id;
  printDialogVisible.value = true;
};

onMounted(() => {
  markErpNavigationPerf('sale-order-list:deferred-panel-mounted', {
    page: 'return-draft'
  });
});
</script>
