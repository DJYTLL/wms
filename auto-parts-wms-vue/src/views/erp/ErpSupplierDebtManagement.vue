<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpSupplierDebtManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="table-toolbar finance-toolbar">
          <div class="table-filters finance-filters finance-filters--summary">
            <el-input
              v-model="searchQuery"
              :placeholder="$t('field.supplier')"
              class="table-search finance-field--wide"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <el-table :data="tableData" style="width: 100%" stripe :empty-text="$t('table.empty')">
          <el-table-column type="index" :label="$t('table.index')" width="70" />
          <el-table-column prop="supplierName" :label="$t('field.supplier')" min-width="180" />
          <el-table-column prop="totalDebt" :label="$t('field.supplierDebtTotal')" min-width="160">
            <template #default="{ row }">
              {{ formatAmount(row.totalDebt) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';

const { notifyError } = useApiError();

const tableData = ref<Array<{ supplierId: number; supplierName: string; totalDebt: number }>>([]);
const searchQuery = ref('');

const formatAmount = (value: number | string) => {
  const num = Number(value || 0);
  if (Number.isNaN(num)) return '0.00';
  return num.toFixed(2);
};

const fetchData = async () => {
  try {
    const res: any = await request.get('/erp/finance/supplier-debts', {
      params: { keyword: searchQuery.value || undefined }
    });
    if (res.data.code === 200) {
      tableData.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleSearch = () => {
  fetchData();
};

onMounted(() => {
  fetchData();
});
</script>
