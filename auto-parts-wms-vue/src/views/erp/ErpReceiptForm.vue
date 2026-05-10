<template>
  <div class="page-shell">
    <div class="page-header">
      <h2 class="page-title">{{ isEditing ? $t('page.erpReceiptEdit') : $t('page.erpReceiptCreate') }}</h2>
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
              <el-form-item :label="$t('field.receiptNo')">
                <el-input v-model="formData.receiptNo" disabled />
              </el-form-item>
            </div>
            <div class="form-group">
              <el-form-item :label="$t('field.customer')" required>
                <FuzzyProductSelect
                  v-model="formData.customerId"
                  :options="customerOptions"
                  :placeholder="$t('field.customer')"
                  style="width: 100%"
                  @change="handleCustomerChange"
                />
              </el-form-item>
            </div>
            <div class="form-group">
              <el-form-item :label="$t('field.orderNo')">
                <div class="receivable-selector">
                  <el-input
                    :model-value="selectedReceivableLabel"
                    :placeholder="$t('placeholder.selectReceivable')"
                    disabled
                  />
                  <el-button @click="openReceivableDialog">{{ $t('action.select') }}</el-button>
                </div>
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
              <el-form-item :label="$t('field.receiptAmount')" required>
                <DecimalInput v-model="formData.amount" input-mode="decimal" :scale="2" :allow-negative="allowNegativeAmount" />
              </el-form-item>
            </div>
            <div class="form-group">
              <el-form-item :label="$t('field.discountAmount')">
                <DecimalInput v-model="formData.discountAmount" input-mode="decimal" :scale="2" :allow-negative="allowNegativeAmount" />
              </el-form-item>
            </div>
            <div class="form-group">
              <el-form-item :label="$t('field.receivedAt')">
                <el-date-picker
                  v-model="formData.receivedAt"
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

          <div class="receivable-table">
            <div class="section-title">{{ $t('page.erpAccountsReceivableManagement') }}</div>
            <div class="receivable-table__body">
              <el-table :data="selectedReceivables" height="240" style="width: 100%" stripe :empty-text="$t('table.empty')">
                <el-table-column prop="orderNo" :label="$t('field.orderNo')" min-width="160">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="openSaleOrderByNo(row)">{{ row.orderNo }}</el-button>
                  </template>
                </el-table-column>
                <el-table-column prop="totalAmount" :label="$t('field.totalAmount')" min-width="140" />
                <el-table-column prop="paidAmount" :label="$t('field.paidAmount')" min-width="140" />
                <el-table-column prop="unpaidAmount" :label="$t('field.unpaidAmount')" min-width="140" />
                  <el-table-column :label="$t('field.receiptAmount')" min-width="140">
                    <template #default="{ row }">
                      <DecimalInput
                        v-model="getAllocation(row.id).amount"
                        input-mode="decimal"
                        :scale="2"
                        :allow-negative="isReturnReceivable(row.id)"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column :label="$t('field.discountAmount')" min-width="140">
                    <template #default="{ row }">
                      <DecimalInput
                        v-model="getAllocation(row.id).discount"
                        input-mode="decimal"
                        :scale="2"
                        :allow-negative="isReturnReceivable(row.id)"
                      />
                    </template>
                  </el-table-column>
              </el-table>
            </div>
          </div>
        </el-form>
      </div>
    </div>

    <el-dialog v-model="receivableDialogVisible" :title="$t('page.erpAccountsReceivableManagement')" width="920px">
      <div class="receivable-dialog__filters">
        <el-date-picker
          v-model="receivableRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          format="YYYY/MM/DD"
          range-separator="~"
          :start-placeholder="$t('field.startTime')"
          :end-placeholder="$t('field.endTime')"
          :clearable="false"
          @change="handleReceivableDateChange"
        />
        <el-input
          v-model="receivableKeyword"
          :placeholder="$t('placeholder.keyword')"
          clearable
          @keyup.enter="fetchReceivableCandidates"
        />
        <el-button type="primary" @click="fetchReceivableCandidates">{{ $t('action.search') }}</el-button>
      </div>
      <el-table
        ref="receivableTableRef"
        :data="receivableCandidates"
        height="360"
        style="width: 100%"
        stripe
        :loading="receivableLoading"
        :empty-text="$t('table.empty')"
        row-key="id"
        @selection-change="handleReceivableSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="orderNo" :label="$t('field.orderNo')" min-width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openSaleOrderByNo(row)">{{ row.orderNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="$t('field.createdTime')" min-width="180">
          <template #default="{ row }">
            {{ formatDateTimeDisplay(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" :label="$t('field.totalAmount')" min-width="140" />
        <el-table-column prop="paidAmount" :label="$t('field.paidAmount')" min-width="140" />
        <el-table-column prop="unpaidAmount" :label="$t('field.unpaidAmount')" min-width="140" />
        <el-table-column prop="status" :label="$t('field.status')" min-width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="arStatusTagType(row.status)">
              {{ arStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="receivableDialogVisible = false">{{ $t('action.cancel') }}</el-button>
        <el-button type="primary" @click="confirmReceivableSelection">{{ $t('action.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onActivated, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import DecimalInput from '@/components/DecimalInput.vue';
import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';

interface OptionItem {
  id: number;
  name: string;
}

interface SettlementOption {
  code: string;
  name: string;
  isDefault?: boolean;
}

interface ReceivableOption {
  id: number;
  orderNo: string;
  customerId?: number;
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
  receiptNo: '',
  customerId: null as number | null,
  receivableIds: [] as number[],
  settlementMethod: '',
  amount: '',
  discountAmount: '',
  receivedAt: '',
  remark: ''
});

const customerOptions = ref<OptionItem[]>([]);
const settlementOptions = ref<SettlementOption[]>([]);
const receivableOptions = ref<ReceivableOption[]>([]);
const receivableCandidates = ref<ReceivableOption[]>([]);
const receivableDialogVisible = ref(false);
const receivableLoading = ref(false);
const receivableRange = ref<[string, string] | []>([]);
const receivableKeyword = ref('');
const receivableSelection = ref<ReceivableOption[]>([]);
const receivableTableRef = ref();
const saving = ref(false);
const pagePath = ref(route.path);
const createdReceiptId = ref<number | null>(null);
const allocationMap = reactive<Record<number, { amount: string; discount: string }>>({});
const updatingFromLines = ref(false);
const updatingFromTotals = ref(false);

const selectedReceivables = computed(() => {
  const ids = formData.value.receivableIds;
  if (!ids.length) return [];
  const map = new Map(receivableOptions.value.map((item) => [item.id, item]));
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

const selectedReceivableLabel = computed(() => {
  const ids = formData.value.receivableIds;
  if (!ids.length) return '';
  const map = new Map(receivableOptions.value.map((item) => [item.id, item]));
  const labels = ids
    .map((id) => map.get(id)?.orderNo)
    .filter(Boolean) as string[];
  if (!labels.length) return '';
  if (labels.length <= 2) return labels.join('、');
  return `${labels[0]} 等${labels.length}条`;
});

const getAllocation = (id: number) => {
  if (!allocationMap[id]) {
    allocationMap[id] = { amount: '', discount: '' };
  }
  return allocationMap[id];
};

  const allocationTotals = computed(() => {
    const ids = formData.value.receivableIds;
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
    totalAmount += Number.isNaN(amount) ? 0 : amount;
    totalDiscount += Number.isNaN(discount) ? 0 : discount;
  });
    return { totalAmount: round2(totalAmount), totalDiscount: round2(totalDiscount), hasAny };
  });

  const receivableMode = computed(() => {
    const ids = formData.value.receivableIds;
    if (!ids.length) return 'none';
    let hasReturn = false;
    let hasNormal = false;
    ids.forEach((id) => {
      const item = receivableOptions.value.find((opt) => opt.id === id);
      const orderNo = item?.orderNo ?? '';
      if (orderNo.startsWith('AR')) {
        hasReturn = true;
      } else if (orderNo) {
        hasNormal = true;
      }
    });
    if (hasReturn && hasNormal) return 'mixed';
    if (hasReturn) return 'return';
    if (hasNormal) return 'normal';
    return 'none';
  });

  const allowNegativeAmount = computed(
    () => receivableMode.value === 'return' || receivableMode.value === 'mixed'
  );

  const isReturnReceivable = (id: number) => {
    const item = receivableOptions.value.find((opt) => opt.id === id);
    return Boolean(item?.orderNo?.startsWith('AR'));
  };

const receiptId = computed(() => {
  if (!route.params.id) return null;
  const parsed = Number(route.params.id);
  return Number.isFinite(parsed) ? parsed : null;
});
const isReceiptRoute = computed(() => route.path.startsWith('/erp/receipts'));
const isEditing = computed(() => Boolean(receiptId.value));

const normalizeNumber = (value: unknown) => {
  const num = Number(value ?? 0);
  return Number.isNaN(num) ? 0 : num;
};

const round2 = (value: number) => {
  return Math.round(value * 100) / 100;
};

const toFixedString = (value: number) => {
  if (!Number.isFinite(value)) return '';
  const rounded = round2(value);
  return rounded.toFixed(2).replace(/\.00$/, '');
};

const isClose = (a: number, b: number, tolerance = 0.005) => {
  return Math.abs(a - b) <= tolerance;
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
  formData.value.amount = val.totalAmount ? toFixedString(val.totalAmount) : '';
  formData.value.discountAmount = val.totalDiscount ? toFixedString(val.totalDiscount) : '';
  updatingFromLines.value = false;
});

  watch(
    () => [formData.value.amount, formData.value.discountAmount, formData.value.receivableIds],
    () => {
      if (updatingFromLines.value) return;
      const ids = formData.value.receivableIds;
      if (!ids.length) return;
      const isReturn = receivableMode.value === 'return';
      const isMixed = receivableMode.value === 'mixed';
      if (isMixed) {
        return;
      }
      const totalsAmount = normalizeNumber(formData.value.amount);
      const totalsDiscount = normalizeNumber(formData.value.discountAmount);
      if (allocationTotals.value.hasAny) {
        const sumAmount = allocationTotals.value.totalAmount;
        const sumDiscount = allocationTotals.value.totalDiscount;
        if (isClose(sumAmount, totalsAmount) && isClose(sumDiscount, totalsDiscount)) {
          return;
        }
      }
      const totalAllocate = round2(totalsAmount + totalsDiscount);
      if (!isReturn && totalAllocate <= 0) {
        return;
      }
      if (isReturn && totalAllocate >= 0) {
        return;
      }
      const weights = ids.map((id) => {
        const item = receivableOptions.value.find((opt) => opt.id === id);
        return Math.abs(normalizeNumber(item?.unpaidAmount));
      });
      const totalUnpaid = round2(weights.reduce((sum, value) => sum + value, 0));
      if (totalUnpaid <= 0) {
        return;
      }
      let amountToAllocate = totalsAmount;
      let discountToAllocate = totalsDiscount;
      const totalAllocateAbs = Math.abs(totalAllocate);
      if (totalAllocateAbs > totalUnpaid) {
        notifyWarning(t('message.receiptOverUnpaid'));
        const scale = totalUnpaid / totalAllocateAbs;
        amountToAllocate = round2(totalsAmount * scale);
        discountToAllocate = round2(totalsDiscount * scale);
      }
      const sign = isReturn ? -1 : 1;
      const amountAllocations = distributeByWeight(Math.abs(amountToAllocate), weights).map((val) => val * sign);
      const discountAllocations = distributeByWeight(Math.abs(discountToAllocate), weights).map((val) => val * sign);
      updatingFromTotals.value = true;
      ids.forEach((id, index) => {
        allocationMap[id] = {
          amount: toFixedString(amountAllocations[index] ?? 0),
          discount: toFixedString(discountAllocations[index] ?? 0)
      };
    });
    updatingFromTotals.value = false;
  },
  { deep: true }
);

watch(receiptId, (newVal, oldVal) => {
  if (!isReceiptRoute.value) {
    return;
  }
  if (newVal && newVal !== oldVal) {
    loadReceiptDetail();
  }
});

const closePage = () => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('tags:close', { detail: { path: router.currentRoute.value.path } }));
  }
};

const goBack = () => {
  closePage();
  router.push('/erp/receipts');
};

const formatDateTime = (date: Date) => {
  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

const arStatusLabel = (status?: string) => {
  if (status === 'OPEN') return t('status.open');
  if (status === 'SETTLED') return t('status.approved');
  if (status === 'RED_FLUSHED') return t('status.redFlushed');
  return status || '-';
};

const arStatusTagType = (status?: string) => {
  if (status === 'SETTLED') return 'success';
  if (status === 'RED_FLUSHED') return 'danger';
  return 'info';
};

const formatDateOnly = (date: Date) => {
  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};

const buildRangeParams = (rangeValue: [string, string] | []) => {
  if (!rangeValue || rangeValue.length !== 2) {
    return { startAt: undefined, endAt: undefined };
  }
  const [start, end] = rangeValue;
  const startDate = new Date(`${start} 00:00:00`);
  const endDate = new Date(`${end} 23:59:59`);
  return {
    startAt: Number.isNaN(startDate.getTime()) ? undefined : startDate.getTime(),
    endAt: Number.isNaN(endDate.getTime()) ? undefined : endDate.getTime()
  };
};

const buildDefaultRange = () => {
  const now = new Date();
  const start = new Date(now);
  start.setFullYear(start.getFullYear() - 1);
  return [formatDateOnly(start), formatDateOnly(now)] as [string, string];
};

const normalizeReceivedAt = (value?: string) => {
  if (!value) return formatDateTime(new Date());
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return formatDateTime(date);
};

const formatDateTimeDisplay = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())} ${pad(date.getHours())}:${pad(
    date.getMinutes()
  )}:${pad(date.getSeconds())}`;
};

const resetForm = () => {
  formData.value.receiptNo = '';
  formData.value.customerId = null;
  formData.value.receivableIds = [];
  formData.value.settlementMethod = '';
  formData.value.amount = '';
  formData.value.discountAmount = '';
  formData.value.receivedAt = formatDateTime(new Date());
  formData.value.remark = '';
  Object.keys(allocationMap).forEach((key) => {
    delete allocationMap[Number(key)];
  });
  createdReceiptId.value = null;
};

const loadReceiptDetail = async () => {
  if (!isReceiptRoute.value) {
    return;
  }
  if (!receiptId.value) {
    return;
  }
  try {
    const res: any = await request.get(`/erp/receipts/${receiptId.value}`);
    if (res.data.code === 200) {
      const data = res.data.data || {};
      const receipt = data.receipt || {};
      formData.value.receiptNo = receipt.receiptNo || '';
      formData.value.customerId = receipt.customerId ?? null;
      formData.value.settlementMethod = receipt.settlementMethod || '';
      formData.value.amount = receipt.amount != null ? String(receipt.amount) : '';
      formData.value.discountAmount = receipt.discountAmount != null ? String(receipt.discountAmount) : '';
      formData.value.receivedAt = normalizeReceivedAt(receipt.receivedAt);
      formData.value.remark = receipt.remark || '';
      createdReceiptId.value = receipt.id ?? receiptId.value;

      const receivableItems = Array.isArray(data.receivables) ? data.receivables : [];
      const receivableIds: number[] = receivableItems.length
        ? receivableItems.map((item: any) => item.receivableId).filter((id: number | null | undefined): id is number => Boolean(id))
        : receipt.receivableId
            ? [receipt.receivableId]
            : [];

      Object.keys(allocationMap).forEach((key) => {
        delete allocationMap[Number(key)];
      });

      receivableItems.forEach((item: any) => {
        if (!item?.receivableId) return;
        allocationMap[item.receivableId] = {
          amount: item.allocatedAmount != null ? String(item.allocatedAmount) : '',
          discount: item.allocatedDiscount != null ? String(item.allocatedDiscount) : ''
        };
      });

      const list = await fetchReceivables(formData.value.customerId);
      formData.value.receivableIds = receivableIds;
      const map = new Map(list.map((item) => [item.id, item]));
      const fallbackMap = new Map(receivableItems.map((item: any) => [item.receivableId, item]));
      receivableOptions.value = receivableIds
        .map((id) => map.get(id) || fallbackMap.get(id))
        .filter(Boolean)
        .map((item: any) => ({
          id: item.id ?? item.receivableId,
          orderNo: item.orderNo ?? '',
          totalAmount: item.totalAmount ?? 0,
          paidAmount: item.paidAmount ?? 0,
          unpaidAmount: item.unpaidAmount ?? 0,
          status: item.status
        }));
      receivableIds.forEach((id: number) => {
        if (!allocationMap[id]) {
          allocationMap[id] = { amount: '', discount: '' };
        }
      });
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchReceiptNo = async () => {
  try {
    const res: any = await request.get('/erp/receipts/next-receipt-no');
    if (res.data.code === 200) {
      formData.value.receiptNo = res.data.data || '';
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchCustomers = async () => {
  try {
    const res: any = await request.get('/erp/customers');
    if (res.data.code === 200) {
      customerOptions.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchSettlementMethods = async () => {
  try {
    const res: any = await request.get('/erp/settlement-methods');
    if (res.data.code === 200) {
      settlementOptions.value = res.data.data || [];
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

const fetchReceivables = async (customerId: number | null) => {
  try {
    const res: any = await request.get('/erp/ar', {
      params: {
        customerId: customerId ?? undefined,
        status: 'OPEN'
      }
    });
      if (res.data.code === 200) {
        const items: ReceivableOption[] = res.data.data || [];
        receivableOptions.value = items.filter((item) => {
          if (item.status === 'RED_FLUSHED') return false;
          if (typeof item.unpaidAmount === 'number' && item.unpaidAmount === 0) return false;
          return true;
        });
      }
    return receivableOptions.value;
  } catch (error) {
    notifyError(error);
  }
  return [];
};

const handleCustomerChange = () => {
  formData.value.receivableIds = [];
  formData.value.amount = '';
  formData.value.discountAmount = '';
  Object.keys(allocationMap).forEach((key) => {
    delete allocationMap[Number(key)];
  });
  receivableOptions.value = [];
  receivableSelection.value = [];
};

const handleReceivableChange = (value: number[] | number | null) => {
  const ids = Array.isArray(value) ? value : value ? [value] : [];
  if (!formData.value.customerId && ids.length) {
    const first = receivableOptions.value.find((item) => item.id === ids[0]);
    if (first?.customerId) {
      formData.value.customerId = first.customerId;
      fetchReceivables(first.customerId);
    }
  }
    if (ids.length === 1) {
      const target = receivableOptions.value.find((item) => item.id === ids[0]);
      if (target) {
        const unpaidValue = Number(target.unpaidAmount ?? 0);
        const unpaidDisplay = Number.isFinite(unpaidValue) ? toFixedString(unpaidValue) : '';
        formData.value.amount = unpaidDisplay;
        allocationMap[target.id] = {
          amount: unpaidDisplay,
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
      const target = receivableOptions.value.find((item) => item.id === id);
      if (target && !allocationMap[id]) {
        const unpaidValue = Number(target.unpaidAmount ?? 0);
        allocationMap[id] = {
          amount: Number.isFinite(unpaidValue) ? toFixedString(unpaidValue) : '',
          discount: ''
        };
      }
    });
  Object.keys(allocationMap).forEach((key) => {
    const id = Number(key);
    if (!ids.includes(id)) {
      delete allocationMap[id];
    }
  });
    const totalUnpaid = ids.reduce((sum, id) => {
      const item = receivableOptions.value.find((opt) => opt.id === id);
      const value = Number(item?.unpaidAmount ?? 0);
      return sum + (Number.isNaN(value) ? 0 : value);
    }, 0);
    const roundedTotal = round2(totalUnpaid);
    formData.value.amount = Number.isFinite(roundedTotal) ? toFixedString(roundedTotal) : '';
  };

const handleReceivableSelectionChange = (rows: ReceivableOption[]) => {
  receivableSelection.value = rows || [];
};

const openReceivableDialog = async () => {
  if (!formData.value.customerId) {
    notifyWarning(t('message.required'));
    return;
  }
  if (!receivableRange.value.length) {
    receivableRange.value = buildDefaultRange();
  }
  receivableDialogVisible.value = true;
  await fetchReceivableCandidates();
};

const handleReceivableDateChange = () => {
  fetchReceivableCandidates();
};

const syncReceivableSelection = async () => {
  await nextTick();
  if (!receivableTableRef.value) return;
  receivableTableRef.value.clearSelection();
  const selectedIds = new Set(formData.value.receivableIds);
  const selectedRows: ReceivableOption[] = [];
  receivableCandidates.value.forEach((row) => {
    if (selectedIds.has(row.id)) {
      receivableTableRef.value.toggleRowSelection(row, true);
      selectedRows.push(row);
    }
  });
  receivableSelection.value = selectedRows;
};

const fetchReceivableCandidates = async () => {
  if (!formData.value.customerId) return;
  receivableLoading.value = true;
  try {
    if (!receivableRange.value.length) {
      receivableRange.value = buildDefaultRange();
    }
    const range = buildRangeParams(receivableRange.value);
    const res: any = await request.get('/erp/ar', {
      params: {
        customerId: formData.value.customerId,
        status: 'OPEN',
        keyword: receivableKeyword.value || undefined,
        startAt: range.startAt,
        endAt: range.endAt
      }
    });
    if (res.data.code === 200) {
      const items: ReceivableOption[] = res.data.data || [];
      receivableCandidates.value = items.filter((item) => {
        if (item.status === 'RED_FLUSHED') return false;
        if (typeof item.unpaidAmount === 'number' && item.unpaidAmount === 0) return false;
        return true;
      });
      await syncReceivableSelection();
    }
  } catch (error) {
    notifyError(error);
  } finally {
    receivableLoading.value = false;
  }
};

const confirmReceivableSelection = () => {
  const selected = receivableSelection.value.slice();
  formData.value.receivableIds = selected.map((item) => item.id);
  receivableOptions.value = selected;
  receivableDialogVisible.value = false;
  handleReceivableChange(formData.value.receivableIds);
};

const openSaleOrderByNo = async (row?: ReceivableOption) => {
  const orderNo = row?.orderNo;
  if (!orderNo) return;
  if (orderNo.startsWith('AR')) {
    if (!row?.id) {
      notifyWarning(t('message.saleReturnNotFound'));
      return;
    }
    try {
      const detailRes: any = await request.get(`/erp/ar/${row.id}`);
      if (detailRes.data.code !== 200) {
        notifyWarning(t('message.saleReturnNotFound'));
        return;
      }
      const remark: string = detailRes.data.data?.receivable?.remark || '';
      const match = remark.match(/销售退货单号:([^\\s|]+)/);
      const returnNo = match ? match[1] : '';
      if (!returnNo) {
        notifyWarning(t('message.saleReturnNotFound'));
        return;
      }
      const returnRes: any = await request.get('/erp/sale-returns', {
        params: { keyword: returnNo }
      });
      if (returnRes.data.code === 200 && Array.isArray(returnRes.data.data) && returnRes.data.data.length > 0) {
        const target = returnRes.data.data[0];
        if (target?.id) {
          router.push(`/erp/sale-returns/${target.id}/edit?mode=view`);
          return;
        }
      }
      notifyWarning(t('message.saleReturnNotFound'));
    } catch (error) {
      notifyError(error);
    }
    return;
  }
  try {
    const res: any = await request.get('/erp/sale-orders', {
      params: {
        keyword: orderNo
      }
    });
    if (res.data.code === 200 && Array.isArray(res.data.data) && res.data.data.length > 0) {
      const target = res.data.data[0];
      if (target?.id) {
        router.push(`/erp/sale-orders/${target.id}/edit?mode=view`);
        return;
      }
    }
    notifyWarning(t('message.saleOrderNotFound'));
  } catch (error) {
    notifyError(error);
  }
};

  const saveReceipt = async (closeOnSuccess = false) => {
    if (!formData.value.customerId) {
      notifyWarning();
      return;
    }
    if (!formData.value.receivableIds.length) {
      notifyWarning(t('message.required'));
      return;
    }
    const isMixed = receivableMode.value === 'mixed';
    const normalizeSignedValue = (raw: unknown, isReturn: boolean) => {
      if (raw == null || String(raw).trim() === '') return '';
      const num = Number(raw);
      if (Number.isNaN(num)) return '';
      const fixed = toFixedString(Math.abs(num));
      if (!fixed) return '';
      return isReturn ? `-${fixed}` : fixed;
    };
    if (allocationTotals.value.hasAny) {
      formData.value.receivableIds.forEach((id) => {
        const alloc = allocationMap[id] || { amount: '', discount: '' };
        const isReturn = isReturnReceivable(id);
        allocationMap[id] = {
          amount: normalizeSignedValue(alloc.amount, isReturn),
          discount: normalizeSignedValue(alloc.discount, isReturn)
        };
      });
    }
    let amount = allocationTotals.value.hasAny
      ? allocationTotals.value.totalAmount
      : Number(formData.value.amount || 0);
    let discountAmount = allocationTotals.value.hasAny
      ? allocationTotals.value.totalDiscount
      : Number(formData.value.discountAmount || 0);
    let totalAllocate = amount + discountAmount;
    if (isMixed) {
      if (!allocationTotals.value.hasAny) {
        notifyWarning('混收需填写分摊金额');
        return;
      }
    } else if (receivableMode.value === 'return') {
      const normalizedAmount = -Math.abs(amount);
      const normalizedDiscount = -Math.abs(discountAmount);
      formData.value.amount = normalizedAmount ? toFixedString(normalizedAmount) : '';
      formData.value.discountAmount = normalizedDiscount ? toFixedString(normalizedDiscount) : '';
      amount = normalizedAmount;
      discountAmount = normalizedDiscount;
      totalAllocate = amount + discountAmount;
      if (normalizedAmount > 0 || normalizedDiscount > 0 || totalAllocate >= 0) {
        notifyWarning(t('message.required'));
        return;
      }
    } else {
      if (amount < 0 || discountAmount < 0 || totalAllocate <= 0) {
        notifyWarning(t('message.required'));
        return;
      }
    }
  const allocationPayload = allocationTotals.value.hasAny
    ? formData.value.receivableIds.map((id) => {
        const alloc = allocationMap[id];
        const allocAmount = alloc?.amount != null && String(alloc.amount).trim() !== '' ? String(alloc.amount) : '0';
        const allocDiscount = alloc?.discount != null && String(alloc.discount).trim() !== '' ? String(alloc.discount) : '0';
        return {
          receivableId: id,
          amount: allocAmount,
          discountAmount: allocDiscount
        };
      })
    : [];
  saving.value = true;
  try {
    const payload = {
      receiptNo: formData.value.receiptNo,
      customerId: formData.value.customerId,
      receivableId: formData.value.receivableIds.length === 1 ? formData.value.receivableIds[0] : null,
      receivableIds: formData.value.receivableIds,
      allocations: allocationPayload.length ? allocationPayload : undefined,
      amount: String(amount),
      discountAmount: String(discountAmount),
      settlementMethod: formData.value.settlementMethod,
      receivedAt: formData.value.receivedAt,
      remark: formData.value.remark
    };
    const res: any = isEditing.value
      ? await request.put(`/erp/receipts/${receiptId.value}`, payload)
      : await request.post('/erp/receipts', payload);
    if (res.data.code === 200) {
      createdReceiptId.value = res.data.data?.receipt?.id ?? createdReceiptId.value ?? receiptId.value ?? null;
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
  await saveReceipt(false);
};

const handleSaveAndBack = async () => {
  await saveReceipt(true);
};

const handleApprove = async () => {
  await saveReceipt(false);
  if (!createdReceiptId.value) {
    return;
  }
  try {
    const res: any = await request.post(`/erp/receipts/${createdReceiptId.value}/approve`);
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
  fetchCustomers();
  fetchSettlementMethods();
  if (isReceiptRoute.value && isEditing.value) {
    loadReceiptDetail();
  } else {
    fetchReceiptNo();
    receivableRange.value = buildDefaultRange();
  }
  if (typeof window !== 'undefined') {
    window.addEventListener('tags:closing', handleTagClosing as EventListener);
  }
});

onActivated(() => {
  pagePath.value = route.path;
  if (!isReceiptRoute.value) {
    return;
  }
  if (isEditing.value) {
    loadReceiptDetail();
    return;
  }
  resetForm();
  fetchReceiptNo();
  fetchCustomers();
  fetchSettlementMethods();
  receivableRange.value = buildDefaultRange();
});

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('tags:closing', handleTagClosing as EventListener);
  }
  resetForm();
});
</script>

<style scoped>
.receivable-table {
  margin-top: 16px;
}

.receivable-table__body {
  max-height: 280px;
  overflow: auto;
}

.receivable-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.receivable-selector :deep(.el-input) {
  flex: 1;
}

.receivable-dialog__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
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
