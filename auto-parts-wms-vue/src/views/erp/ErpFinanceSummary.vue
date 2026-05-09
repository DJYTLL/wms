<template>
  <div class="page-shell">
    <div class="page-header">
      <h2 class="page-title">{{ $t('page.erpFinanceSummary') }}</h2>
    </div>

    <div class="table-card finance-summary-card">
      <div class="table-body finance-summary-body">
        <div class="finance-summary-grid">
          <div class="finance-summary-item">
            <div class="finance-summary-label">{{ $t('field.customerDebtTotal') }}</div>
            <div class="finance-summary-value">{{ formatAmount(summary.customerDebtTotal) }}</div>
          </div>
          <div class="finance-summary-item">
            <div class="finance-summary-label">{{ $t('field.supplierDebtTotal') }}</div>
            <div class="finance-summary-value">{{ formatAmount(summary.supplierDebtTotal) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive } from 'vue';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';

const { notifyError } = useApiError();

const summary = reactive({
  customerDebtTotal: 0,
  supplierDebtTotal: 0
});

const formatAmount = (value: number | string) => {
  const num = Number(value || 0);
  if (Number.isNaN(num)) return '0.00';
  return num.toFixed(2);
};

const fetchSummary = async () => {
  try {
    const res: any = await request.get('/erp/finance/summary');
    if (res.data.code === 200) {
      summary.customerDebtTotal = res.data.data?.customerDebtTotal ?? 0;
      summary.supplierDebtTotal = res.data.data?.supplierDebtTotal ?? 0;
    }
  } catch (error) {
    notifyError(error);
  }
};

onMounted(() => {
  fetchSummary();
});
</script>

<style scoped>
.finance-summary-card {
  max-width: 720px;
}

.finance-summary-body {
  padding: 20px;
}

.finance-summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.finance-summary-item {
  padding: 16px 18px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: #f8fafc;
}

.finance-summary-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 6px;
}

.finance-summary-value {
  font-size: 22px;
  font-weight: 600;
  color: #111827;
}
</style>
