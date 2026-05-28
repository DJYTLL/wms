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
        </ErpDataTable>
      </div>
    </div>
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

const { notifyError } = useApiError();
const defaultColumns = ['subjectName', 'customerCount', 'supplierCount', 'receivableTotal', 'payableTotal', 'netAmount'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-finance-counterparty-subject', defaultColumns);
const canShow = (key: string) => isVisible(key);

const tableData = ref<CounterpartyFinanceSummaryRow[]>([]);
const allTableData = ref<CounterpartyFinanceSummaryRow[]>([]);
const loading = ref(false);
const searchQuery = ref('');

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
