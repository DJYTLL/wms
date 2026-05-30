<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpCounterpartyFinanceSummary') }}</div>
      <div class="page-toolbar-card">
        <div class="table-toolbar finance-toolbar">
          <div class="table-filters finance-filters finance-filters--summary">
            <el-input v-model="searchQuery" :placeholder="$t('field.counterpartySubject')" class="table-search finance-field--wide" clearable @clear="handleSearch" @keyup.enter="handleSearch" />
          </div>
          <div class="finance-actions">
            <el-button type="primary" @click="handleSearch">{{ $t('action.search') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <ErpDataTable :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')" table-key="erp-counterparty-finance-summary">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('subjectName')" prop="subjectName" :label="$t('field.counterpartySubject')" min-width="200" />
          <ErpDataTableColumn v-if="canShow('customerCount')" prop="customerCount" :label="$t('field.customerCount')" min-width="120" />
          <ErpDataTableColumn v-if="canShow('supplierCount')" prop="supplierCount" :label="$t('field.supplierCount')" min-width="120" />
          <ErpDataTableColumn v-if="canShow('receivableTotal')" prop="receivableTotal" :label="$t('field.receivableTotal')" min-width="150">
            <template #default="{ row }">{{ formatAmount(row.receivableTotal) }}</template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('payableTotal')" prop="payableTotal" :label="$t('field.payableTotal')" min-width="150">
            <template #default="{ row }">{{ formatAmount(row.payableTotal) }}</template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('netAmount')" prop="netAmount" :label="$t('field.netAmount')" min-width="150">
            <template #default="{ row }">
              <span :class="{ 'amount-positive': Number(row.netAmount || 0) >= 0, 'amount-negative': Number(row.netAmount || 0) < 0 }">
                {{ formatAmount(row.netAmount) }}
              </span>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('table.actions')" width="120">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openDetail(row)">查看明细</el-button>
            </template>
          </ErpDataTableColumn>
        </ErpDataTable>
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="往来明细" size="60%">
      <ErpDataTable :data="detailRows" stripe v-loading="detailLoading" :empty-text="$t('table.empty')" table-key="erp-counterparty-finance-detail">
        <ErpDataTableColumn prop="detailType" label="类型" min-width="100" />
        <ErpDataTableColumn prop="bizNo" label="单号" min-width="150" />
        <ErpDataTableColumn prop="targetName" :label="$t('field.name')" min-width="180" />
        <ErpDataTableColumn prop="targetCode" :label="$t('field.code')" min-width="140" />
        <ErpDataTableColumn prop="totalAmount" :label="$t('field.totalAmount')" min-width="120">
          <template #default="{ row }">{{ formatAmount(row.totalAmount) }}</template>
        </ErpDataTableColumn>
        <ErpDataTableColumn prop="unpaidAmount" label="未结金额" min-width="120">
          <template #default="{ row }">{{ formatAmount(row.unpaidAmount) }}</template>
        </ErpDataTableColumn>
        <ErpDataTableColumn prop="status" :label="$t('field.status')" min-width="120" />
        <ErpDataTableColumn prop="createdAt" :label="$t('field.createdTime')" min-width="180" />
      </ErpDataTable>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { filterByFuzzyKeyword } from '@/utils/fuzzySearch';

interface CounterpartyFinanceSummaryRow {
  subjectId: number;
  subjectName: string;
  receivableTotal: number | string;
  payableTotal: number | string;
  netAmount: number | string;
  customerCount: number;
  supplierCount: number;
}

interface CounterpartyFinanceDetailRow {
  detailType: string;
  bizNo: string;
  targetName: string;
  targetCode: string;
  totalAmount: number | string;
  unpaidAmount: number | string;
  status: string;
  createdAt: string;
}

const { notifyError } = useApiError();
const defaultColumns = ['subjectName', 'customerCount', 'supplierCount', 'receivableTotal', 'payableTotal', 'netAmount'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-finance-counterparty-subject', defaultColumns);
const canShow = (key: string) => isVisible(key);

const tableData = ref<CounterpartyFinanceSummaryRow[]>([]);
const allTableData = ref<CounterpartyFinanceSummaryRow[]>([]);
const loading = ref(false);
const searchQuery = ref('');
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailRows = ref<CounterpartyFinanceDetailRow[]>([]);

const formatAmount = (value: number | string) => {
  const num = Number(value || 0);
  if (Number.isNaN(num)) return '0.00';
  return num.toFixed(2);
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res: any = await request.get('/erp/finance/counterparty-subjects/summary');
    if (res.data.code === 200) {
      allTableData.value = res.data.data || [];
      applySearch();
    }
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
};

const applySearch = () => {
  tableData.value = filterByFuzzyKeyword(allTableData.value, searchQuery.value, row => [row.subjectName]);
};

const handleSearch = () => {
  applySearch();
};

const openDetail = async (row: CounterpartyFinanceSummaryRow) => {
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    const res: any = await request.get(`/erp/finance/counterparty-subjects/${row.subjectId}/details`);
    if (res.data.code === 200) {
      detailRows.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  } finally {
    detailLoading.value = false;
  }
};

onMounted(() => {
  fetchTenantKeys();
  fetchData();
});
</script>

<style scoped>
.amount-positive {
  color: #16a34a;
}

.amount-negative {
  color: #dc2626;
}
</style>
