<template>
  <div class="page-shell">
    <div class="page-header">
      <h2 class="page-title">{{ isEditing ? $t('page.erpPaymentEdit') : $t('page.erpPaymentCreate') }}</h2>
      <div class="page-actions">
        <el-button @click="goBack">{{ $t('action.back') }}</el-button>
        <el-button :loading="saving" @click="handleSave">{{ $t('action.save') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveAndBack">{{ $t('action.saveAndBack') }}</el-button>
        <el-button type="success" :loading="saving" @click="handleApprove">{{ $t('action.approve') }}</el-button>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <el-form :model="formData" label-position="top" class="sale-form">
          <div class="form-grid">
            <div class="form-group">
              <el-form-item :label="$t('field.paymentNo')">
                <el-input v-model="formData.paymentNo" disabled />
              </el-form-item>
            </div>
            <div class="form-group">
              <el-form-item :label="$t('field.supplier')" required>
                <FuzzyProductSelect
                  v-model="formData.supplierId"
                  :options="supplierOptions"
                  :placeholder="$t('field.supplier')"
                  style="width: 100%"
                  @change="handleSupplierChange"
                />
              </el-form-item>
            </div>
            <div class="form-group">
              <el-form-item :label="$t('field.orderNo')">
                <el-select
                  v-model="formData.payableIds"
                  filterable
                  clearable
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  @change="handlePayableChange"
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in payableOptions"
                    :key="item.id"
                    :label="`${item.orderNo}（${item.unpaidAmount || 0}）`"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
            </div>
            <div class="form-group">
              <el-form-item :label="$t('field.settlementMethod')" required>
                <el-select v-model="formData.settlementMethod" filterable style="width: 100%">
                  <el-option v-for="item in settlementOptions" :key="item.code" :label="item.name" :value="item.code" />
                </el-select>
              </el-form-item>
            </div>
            <div class="form-group">
              <el-form-item :label="$t('field.paymentMethod')">
                <el-select v-model="formData.paymentMethodCode" filterable clearable style="width: 100%">
                  <el-option v-for="item in paymentMethodOptions" :key="item.code" :label="item.name" :value="item.code" />
                </el-select>
              </el-form-item>
            </div>
            <div class="form-group">
              <el-form-item :label="$t('field.paymentAmount')" required>
                <DecimalInput v-model="formData.amount" input-mode="decimal" :scale="2" :allow-negative="allowNegativeAmount" />
              </el-form-item>
            </div>
            <div class="form-group">
              <el-form-item :label="$t('field.discountAmount')">
                <DecimalInput v-model="formData.discountAmount" input-mode="decimal" :scale="2" :allow-negative="allowNegativeAmount" />
              </el-form-item>
            </div>
            <div class="form-group">
              <el-form-item :label="$t('field.paidAt')">
                <el-date-picker
                  v-model="formData.paidAt"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  format="YYYY/MM/DD HH:mm:ss"
                  style="width: 100%"
                />
              </el-form-item>
            </div>
          </div>
          <el-form-item :label="$t('field.remark')">
            <el-input v-model="formData.remark" type="textarea" :rows="3" />
          </el-form-item>

          <div class="payable-table">
            <div class="section-title">{{ $t('page.erpAccountsPayableManagement') }}</div>
            <div class="payable-table__body">
              <el-table :data="selectedPayables" height="240" style="width: 100%" stripe :empty-text="$t('table.empty')">
                <el-table-column prop="orderNo" :label="$t('field.orderNo')" min-width="160" />
                <el-table-column prop="totalAmount" :label="$t('field.totalAmount')" min-width="140" />
                <el-table-column prop="paidAmount" :label="$t('field.paidAmount')" min-width="140" />
                <el-table-column prop="unpaidAmount" :label="$t('field.unpaidAmount')" min-width="140" />
                <el-table-column :label="$t('field.paymentAmount')" min-width="140">
                  <template #default="{ row }">
                    <DecimalInput v-model="getAllocation(row.id).amount" input-mode="decimal" :scale="2" :allow-negative="isReturnPayable(row.id)" />
                  </template>
                </el-table-column>
                <el-table-column :label="$t('field.discountAmount')" min-width="140">
                  <template #default="{ row }">
                    <DecimalInput v-model="getAllocation(row.id).discount" input-mode="decimal" :scale="2" :allow-negative="isReturnPayable(row.id)" />
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import DecimalInput from '@/components/DecimalInput.vue';
import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';

interface OptionItem {
  id: number;
  name: string;
  defaultSettlementMethodCode?: string;
  defaultPaymentMethodCode?: string;
}

interface SettlementOption {
  code: string;
  name: string;
  isDefault?: boolean;
  fundInputMode?: 'HIDDEN' | 'OPTIONAL' | 'REQUIRED';
}

interface PayableOption {
  id: number;
  orderNo: string;
  supplierId?: number;
  totalAmount?: number;
  paidAmount?: number;
  unpaidAmount: number;
  status?: string;
}

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const { notifyError, notifySuccess, notifyWarning } = useApiError();

const formData = ref({
  paymentNo: '',
  supplierId: null as number | null,
  payableIds: [] as number[],
  settlementMethod: '',
  paymentMethodCode: '',
  amount: '',
  discountAmount: '',
  paidAt: '',
  remark: ''
});

const supplierOptions = ref<OptionItem[]>([]);
const settlementOptions = ref<SettlementOption[]>([]);
const paymentMethodOptions = ref<SettlementOption[]>([]);
const payableOptions = ref<PayableOption[]>([]);
const saving = ref(false);
const pagePath = ref(route.path);
const createdPaymentId = ref<number | null>(null);
const allocationMap = reactive<Record<number, { amount: string; discount: string }>>({});
const updatingFromLines = ref(false);
const updatingFromTotals = ref(false);

const selectedPayables = computed(() => {
  const ids = formData.value.payableIds;
  if (!ids.length) return [];
  const map = new Map(payableOptions.value.map((item) => [item.id, item]));
  return ids
    .map((id) => map.get(id))
    .filter(Boolean)
    .map((item) => ({
      ...item,
      totalAmount: item?.totalAmount ?? 0,
      paidAmount: item?.paidAmount ?? 0,
      unpaidAmount: item?.unpaidAmount ?? 0
    }));
});

const payableMode = computed<'none' | 'normal' | 'return' | 'mixed'>(() => {
  if (!selectedPayables.value.length) return 'none';
  let hasNormal = false;
  let hasReturn = false;
  selectedPayables.value.forEach((item) => {
    if ((item.unpaidAmount ?? 0) < 0) {
      hasReturn = true;
    } else {
      hasNormal = true;
    }
  });
  if (hasNormal && hasReturn) return 'mixed';
  if (hasReturn) return 'return';
  return 'normal';
});

const allowNegativeAmount = computed(() => payableMode.value === 'return' || payableMode.value === 'mixed');

const getAllocation = (id: number) => {
  if (!allocationMap[id]) {
    allocationMap[id] = { amount: '', discount: '' };
  }
  return allocationMap[id];
};

const allocationTotals = computed(() => {
  const ids = formData.value.payableIds;
  let totalAmount = 0;
  let totalDiscount = 0;
  let hasAny = false;
  ids.forEach((id) => {
    const alloc = allocationMap[id];
    if (!alloc) return;
    if (alloc.amount !== '' || alloc.discount !== '') {
      hasAny = true;
    }
    const amount = Number(alloc.amount ?? 0);
    const discount = Number(alloc.discount ?? 0);
    totalAmount = roundCurrency(totalAmount + (Number.isNaN(amount) ? 0 : amount));
    totalDiscount = roundCurrency(totalDiscount + (Number.isNaN(discount) ? 0 : discount));
  });
  return { totalAmount: roundCurrency(totalAmount), totalDiscount: roundCurrency(totalDiscount), hasAny };
});

const paymentId = computed(() => {
  if (!route.params.id) return null;
  const parsed = Number(route.params.id);
  return Number.isFinite(parsed) ? parsed : null;
});
const isPaymentRoute = computed(() => route.path.startsWith('/erp/payments'));
const isEditing = computed(() => Boolean(paymentId.value));

const normalizeNumber = (value: unknown) => {
  const num = Number(value ?? 0);
  return Number.isNaN(num) ? 0 : num;
};

const toFixedString = (value: number) => {
  return Number.isFinite(value) ? value.toFixed(2).replace(/\.00$/, '') : '';
};

const isClose = (a: number, b: number, tolerance = 0.005) => {
  return Math.abs(a - b) <= tolerance;
};

const roundCurrency = (value: number) => {
  return Math.round(value * 100) / 100;
};

const isReturnPayable = (id: number) => {
  const item = payableOptions.value.find((option) => option.id === id);
  return Number(item?.unpaidAmount ?? 0) < 0;
};

const distributeByWeight = (total: number, weights: number[]) => {
  const count = weights.length;
  const results = new Array<number>(count).fill(0);
  if (count === 0 || total <= 0) {
    return results;
  }
  const sumWeight = weights.reduce((sum, weight) => (weight > 0 ? sum + weight : sum), 0);
  if (sumWeight <= 0) {
    const per = Math.floor((total / count) * 100) / 100;
    return results.map(() => per);
  }
  for (let i = 0; i < count; i += 1) {
    const weight = weights[i] ?? 0;
    if (weight <= 0) {
      results[i] = 0;
      continue;
    }
    const alloc = total * (weight / sumWeight);
    results[i] = Math.floor(alloc * 100) / 100;
  }
  let allocated = results.reduce((sum, value) => sum + value, 0);
  let remainder = Math.round((total - allocated) * 100) / 100;
  let guard = 0;
  while (remainder >= 0.01 && guard < 10000) {
    let assigned = false;
    for (let i = 0; i < count && remainder >= 0.01; i += 1) {
      if ((weights[i] ?? 0) <= 0) {
        continue;
      }
      results[i] = Math.round(((results[i] ?? 0) + 0.01) * 100) / 100;
      remainder = Math.round((remainder - 0.01) * 100) / 100;
      assigned = true;
    }
    if (!assigned) {
      break;
    }
    guard += 1;
  }
  return results;
};

watch(allocationTotals, (val) => {
  if (updatingFromTotals.value) {
    return;
  }
  updatingFromLines.value = true;
  formData.value.amount = val.totalAmount ? String(val.totalAmount) : '';
  formData.value.discountAmount = val.totalDiscount ? String(val.totalDiscount) : '';
  updatingFromLines.value = false;
});

watch(
  () => [formData.value.amount, formData.value.discountAmount, formData.value.payableIds],
  () => {
    if (updatingFromLines.value) return;
    const ids = formData.value.payableIds;
    if (!ids.length) return;
    const totalsAmount = normalizeNumber(formData.value.amount);
    const totalsDiscount = normalizeNumber(formData.value.discountAmount);
    if (allocationTotals.value.hasAny) {
      const sumAmount = allocationTotals.value.totalAmount;
      const sumDiscount = allocationTotals.value.totalDiscount;
      if (isClose(sumAmount, totalsAmount) && isClose(sumDiscount, totalsDiscount)) {
        return;
      }
    }
    if (payableMode.value === 'mixed') {
      return;
    }
    const totalAllocate = totalsAmount + totalsDiscount;
    if (
      (payableMode.value === 'return' && totalAllocate >= 0) ||
      (payableMode.value !== 'return' && totalAllocate <= 0)
    ) {
      return;
    }
    const weights = ids.map((id) => {
      const item = payableOptions.value.find((opt) => opt.id === id);
      return Math.abs(normalizeNumber(item?.unpaidAmount));
    });
    const totalUnpaid = weights.reduce((sum, value) => sum + value, 0);
    if (totalUnpaid <= 0) {
      return;
    }
    const totalAllocateAbs = Math.abs(totalAllocate);
    let amountToAllocate = Math.abs(totalsAmount);
    let discountToAllocate = Math.abs(totalsDiscount);
    if (totalAllocateAbs > totalUnpaid) {
      notifyWarning(t('message.paymentOverUnpaid'));
      const scale = totalUnpaid / totalAllocateAbs;
      amountToAllocate = Math.floor(amountToAllocate * scale * 100) / 100;
      discountToAllocate = Math.floor(discountToAllocate * scale * 100) / 100;
    }
    const amountAllocations = distributeByWeight(amountToAllocate, weights);
    const discountAllocations = distributeByWeight(discountToAllocate, weights);
    const sign = payableMode.value === 'return' ? -1 : 1;
    updatingFromTotals.value = true;
    ids.forEach((id, index) => {
      allocationMap[id] = {
        amount: toFixedString((amountAllocations[index] ?? 0) * sign),
        discount: toFixedString((discountAllocations[index] ?? 0) * sign)
      };
    });
    updatingFromTotals.value = false;
  },
  { deep: true }
);

watch(paymentId, (newVal, oldVal) => {
  if (!isPaymentRoute.value) {
    return;
  }
  if (newVal && newVal !== oldVal) {
    loadPaymentDetail();
  }
});

const closePage = () => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('tags:close', { detail: { path: router.currentRoute.value.path } }));
  }
};

const goBack = () => {
  closePage();
  router.push('/erp/payments');
};

const formatDateTime = (date: Date) => {
  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

const normalizePaidAt = (value?: string) => {
  if (!value) return formatDateTime(new Date());
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return formatDateTime(date);
};

const resetForm = () => {
  formData.value.paymentNo = '';
  formData.value.supplierId = null;
  formData.value.payableIds = [];
  formData.value.settlementMethod = '';
  formData.value.paymentMethodCode = '';
  formData.value.amount = '';
  formData.value.discountAmount = '';
  formData.value.paidAt = formatDateTime(new Date());
  formData.value.remark = '';
  Object.keys(allocationMap).forEach((key) => {
    delete allocationMap[Number(key)];
  });
  createdPaymentId.value = null;
};

const loadPaymentDetail = async () => {
  if (!isPaymentRoute.value) {
    return;
  }
  if (!paymentId.value) {
    return;
  }
  try {
    const res: any = await request.get(`/erp/payments/${paymentId.value}`);
    if (res.data.code === 200) {
      const data = res.data.data || {};
      const payment = data.payment || {};
      formData.value.paymentNo = payment.paymentNo || '';
      formData.value.supplierId = payment.supplierId ?? null;
      formData.value.settlementMethod = payment.settlementMethod || '';
      formData.value.paymentMethodCode = payment.paymentMethodCode || '';
      formData.value.amount = payment.amount != null ? String(payment.amount) : '';
      formData.value.discountAmount = payment.discountAmount != null ? String(payment.discountAmount) : '';
      formData.value.paidAt = normalizePaidAt(payment.paidAt);
      formData.value.remark = payment.remark || '';
      createdPaymentId.value = payment.id ?? paymentId.value;

      const payableItems = Array.isArray(data.payables) ? data.payables : [];
      const payableIds: number[] = payableItems.length
        ? payableItems.map((item: any) => item.payableId).filter((id: number | null | undefined): id is number => Boolean(id))
        : payment.payableId
            ? [payment.payableId]
            : [];

      Object.keys(allocationMap).forEach((key) => {
        delete allocationMap[Number(key)];
      });

      payableItems.forEach((item: any) => {
        if (!item?.payableId) return;
        allocationMap[item.payableId] = {
          amount: item.allocatedAmount != null ? String(item.allocatedAmount) : '',
          discount: item.allocatedDiscount != null ? String(item.allocatedDiscount) : ''
        };
      });

      await fetchPayables(formData.value.supplierId);
      formData.value.payableIds = payableIds;
      payableIds.forEach((id: number) => {
        if (!allocationMap[id]) {
          allocationMap[id] = { amount: '', discount: '' };
        }
      });
    }
  } catch (error) {
    const message = (error as any)?.response?.data?.message;
    if (message && String(message).includes('付款单不存在')) {
      notifyWarning(message);
      goBack();
      return;
    }
    notifyError(error);
  }
};

const fetchPaymentNo = async () => {
  try {
    const res: any = await request.get('/erp/payments/next-payment-no');
    if (res.data.code === 200) {
      formData.value.paymentNo = res.data.data || '';
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchSuppliers = async () => {
  try {
    const res: any = await request.get('/erp/suppliers');
    if (res.data.code === 200) {
      supplierOptions.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchSettlementMethods = async () => {
  try {
    const res: any = await request.get('/erp/settlement-methods');
    if (res.data.code === 200) {
      const allOptions: SettlementOption[] = res.data.data || [];
      settlementOptions.value = allOptions.filter((item) => item.fundInputMode !== 'HIDDEN');
      if (!settlementOptions.value.some((item) => item.code === formData.value.settlementMethod)) {
        formData.value.settlementMethod = '';
      }
      if (!formData.value.settlementMethod && settlementOptions.value.length > 0) {
        const defaultMethod = settlementOptions.value.find((item) => item.isDefault) ?? settlementOptions.value[0];
        if (defaultMethod) {
          formData.value.settlementMethod = defaultMethod.code;
        }
      }
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchPaymentMethods = async () => {
  try {
    const res: any = await request.get('/erp/payment-methods');
    if (res.data.code === 200) {
      paymentMethodOptions.value = res.data.data || [];
      if (!formData.value.paymentMethodCode && paymentMethodOptions.value.length > 0) {
        const defaultMethod = paymentMethodOptions.value.find((item) => item.isDefault) ?? paymentMethodOptions.value[0];
        if (defaultMethod) {
          formData.value.paymentMethodCode = defaultMethod.code;
        }
      }
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchPayables = async (supplierId: number | null) => {
  try {
    const res: any = await request.get('/erp/ap', {
      params: {
        supplierId: supplierId ?? undefined,
        status: 'OPEN'
      }
    });
    if (res.data.code === 200) {
      const items: PayableOption[] = res.data.data || [];
      payableOptions.value = items.filter((item) => {
        if (item.status === 'RED_FLUSHED') return false;
        if (Number(item.unpaidAmount ?? 0) === 0) return false;
        return true;
      });
    }
    return payableOptions.value;
  } catch (error) {
    notifyError(error);
  }
  return [];
};

const handleSupplierChange = (value: number | null) => {
  const supplier = supplierOptions.value.find((item) => item.id === value);
  if (
    supplier?.defaultSettlementMethodCode
    && settlementOptions.value.some((item) => item.code === supplier.defaultSettlementMethodCode)
  ) {
    formData.value.settlementMethod = supplier.defaultSettlementMethodCode;
  } else if (
    !formData.value.settlementMethod
    || !settlementOptions.value.some((item) => item.code === formData.value.settlementMethod)
  ) {
    formData.value.settlementMethod = settlementOptions.value.find((item) => item.isDefault)?.code
      ?? settlementOptions.value[0]?.code
      ?? '';
  }
  if (supplier?.defaultPaymentMethodCode) {
    formData.value.paymentMethodCode = supplier.defaultPaymentMethodCode;
  }
  formData.value.payableIds = [];
  formData.value.amount = '';
  formData.value.discountAmount = '';
  Object.keys(allocationMap).forEach((key) => {
    delete allocationMap[Number(key)];
  });
  fetchPayables(value);
};

const handlePayableChange = (value: number[] | number | null) => {
  const ids = Array.isArray(value) ? value : value ? [value] : [];
  if (!formData.value.supplierId && ids.length) {
    const first = payableOptions.value.find((item) => item.id === ids[0]);
    if (first?.supplierId) {
      formData.value.supplierId = first.supplierId;
      fetchPayables(first.supplierId);
    }
  }
  if (ids.length === 1) {
    const target = payableOptions.value.find((item) => item.id === ids[0]);
    if (target) {
      formData.value.amount = String(target.unpaidAmount ?? '');
      formData.value.discountAmount = allocationMap[target.id]?.discount ?? '';
      allocationMap[target.id] = {
        amount: String(target.unpaidAmount ?? ''),
        discount: allocationMap[target.id]?.discount ?? ''
      };
    }
    return;
  }
  if (ids.length === 0) {
    formData.value.amount = '';
    formData.value.discountAmount = '';
    Object.keys(allocationMap).forEach((key) => {
      delete allocationMap[Number(key)];
    });
    return;
  }
  ids.forEach((id) => {
    const target = payableOptions.value.find((item) => item.id === id);
    if (target && !allocationMap[id]) {
      allocationMap[id] = { amount: String(target.unpaidAmount ?? ''), discount: '' };
    }
  });
  Object.keys(allocationMap).forEach((key) => {
    const id = Number(key);
    if (!ids.includes(id)) {
      delete allocationMap[id];
    }
  });
  const totalUnpaid = ids.reduce((sum, id) => {
    const item = payableOptions.value.find((opt) => opt.id === id);
    const value = Number(item?.unpaidAmount ?? 0);
    return sum + (Number.isNaN(value) ? 0 : value);
  }, 0);
  formData.value.amount = totalUnpaid ? String(totalUnpaid) : '';
  formData.value.discountAmount = '';
};

const savePayment = async (closeOnSuccess = false) => {
  if (!formData.value.supplierId) {
    notifyWarning();
    return;
  }
  if (!formData.value.payableIds.length) {
    notifyWarning(t('message.required'));
    return;
  }
  const amount = allocationTotals.value.hasAny
    ? allocationTotals.value.totalAmount
    : Number(formData.value.amount || 0);
  const discountAmount = allocationTotals.value.hasAny
    ? allocationTotals.value.totalDiscount
    : Number(formData.value.discountAmount || 0);
  const totalAmount = amount + discountAmount;
  if (payableMode.value === 'mixed' && !allocationTotals.value.hasAny) {
    notifyWarning('正负应付混合付款需填写分摊金额');
    return;
  }
  if (payableMode.value === 'return') {
    if (amount > 0 || discountAmount > 0 || totalAmount >= 0) {
      notifyWarning('退款金额必须小于0');
      return;
    }
  } else if (payableMode.value === 'normal' && (amount < 0 || discountAmount < 0 || totalAmount <= 0)) {
    notifyWarning(t('message.required'));
    return;
  }
  const allocationPayload = allocationTotals.value.hasAny
    ? formData.value.payableIds.map((id) => {
        const alloc = allocationMap[id];
        const allocAmount = alloc?.amount != null && String(alloc.amount).trim() !== '' ? String(alloc.amount) : '0';
        const allocDiscount = alloc?.discount != null && String(alloc.discount).trim() !== '' ? String(alloc.discount) : '0';
        return {
          payableId: id,
          amount: allocAmount,
          discountAmount: allocDiscount
        };
      })
    : [];
  saving.value = true;
  try {
    const payload = {
      paymentNo: formData.value.paymentNo,
      supplierId: formData.value.supplierId,
      payableId: formData.value.payableIds.length === 1 ? formData.value.payableIds[0] : null,
      payableIds: formData.value.payableIds,
      allocations: allocationPayload.length ? allocationPayload : undefined,
      amount: String(amount),
      discountAmount: String(discountAmount),
      settlementMethod: formData.value.settlementMethod,
      paymentMethodCode: formData.value.paymentMethodCode || undefined,
      paidAt: formData.value.paidAt,
      remark: formData.value.remark
    };
    const res: any = isEditing.value
      ? await request.put(`/erp/payments/${paymentId.value}`, payload)
      : await request.post('/erp/payments', payload);
    if (res.data.code === 200) {
      createdPaymentId.value = res.data.data?.payment?.id ?? createdPaymentId.value ?? paymentId.value ?? null;
      notifySuccess();
      if (closeOnSuccess) {
        goBack();
      }
    }
  } catch (error) {
    notifyError(error);
  } finally {
    saving.value = false;
  }
};

const handleSave = async () => {
  await savePayment(false);
};

const handleSaveAndBack = async () => {
  await savePayment(true);
};

const handleApprove = async () => {
  await savePayment(false);
  if (!createdPaymentId.value) {
    return;
  }
  try {
    const res: any = await request.post(`/erp/payments/${createdPaymentId.value}/approve`);
    if (res.data.code === 200) {
      notifySuccess();
      goBack();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleTagClosing = (event: Event) => {
  const customEvent = event as CustomEvent<{ path?: string }>;
  if (customEvent.detail?.path === pagePath.value) {
    resetForm();
  }
};

onMounted(() => {
  pagePath.value = route.path;
  resetForm();
  fetchSuppliers();
  fetchSettlementMethods();
  fetchPaymentMethods();
  if (isPaymentRoute.value && isEditing.value) {
    loadPaymentDetail();
  } else {
    fetchPaymentNo();
    fetchPayables(formData.value.supplierId);
  }
  if (typeof window !== 'undefined') {
    window.addEventListener('tags:closing', handleTagClosing as EventListener);
  }
});

onActivated(() => {
  pagePath.value = route.path;
  if (!isPaymentRoute.value) {
    return;
  }
  if (isEditing.value) {
    loadPaymentDetail();
    return;
  }
  resetForm();
  fetchPaymentNo();
  fetchSuppliers();
  fetchSettlementMethods();
  fetchPaymentMethods();
  fetchPayables(formData.value.supplierId);
});

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('tags:closing', handleTagClosing as EventListener);
  }
  resetForm();
});
</script>

<style scoped>
.payable-table {
  margin-top: 16px;
}

.payable-table__body {
  max-height: 280px;
  overflow: auto;
}

.table-card {
  max-height: calc(100vh - 200px);
  overflow: hidden;
  padding: 16px;
}

.table-body {
  overflow: auto;
  min-height: 0;
}

.sale-form :deep(.el-form-item) {
  margin-bottom: 4px;
}

.form-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding-bottom: 4px;
}

.form-group {
  flex: 1 1 220px;
  min-width: 220px;
}

@media (max-width: 1200px) {
  .form-grid {
    gap: 4px;
  }
}
</style>
