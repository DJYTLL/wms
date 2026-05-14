<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ pageTitle }}</div>
      <div class="erp-toolbar">
        <div class="table-toolbar">
          <div class="table-filters">
          <el-input
            v-model="searchQuery"
            :placeholder="$t('action.search')"
            class="erp-toolbar__search erp-toolbar__search--wide"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <FuzzyProductSelect
            v-model="customerFilter"
            :options="customerOptions"
            :placeholder="$t('field.customer')"
            class="erp-toolbar__search erp-toolbar__search--wide"
            @change="handleSearch"
          />
          <!-- <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="table-search" clearable :disabled="statusLocked" @change="handleSearch">
            <el-option :label="$t('filter.all')" value="" />
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select> -->
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            value-format="x"
            format="YYYY-MM-DD HH:mm:ss"
            :shortcuts="dateRangeShortcuts"
            :start-placeholder="$t('field.startTime')"
            :end-placeholder="$t('field.endTime')"
            class="erp-toolbar__date-range table-date-range table-date-range--compact"
            @change="handleSearch"
          />
          </div>
          <div class="table-actions">
            <el-button
              v-if="canCreate"
              type="primary"
              v-permission="'erp-sale:add'"
              @click="openCreatePage"
            >
              {{ $t('action.add') }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card" :class="{ 'sale-approved-card': isApprovedPage }">
      <div class="table-body">
        <el-table
          :data="tableData"
          style="width: 100%"
          stripe
          v-loading="loading"
          :empty-text="$t('table.empty')"
          :row-class-name="rowClassName"
        >
          <el-table-column type="index" :label="$t('table.index')" width="70" />
          <el-table-column v-if="canShow('orderNo')" prop="orderNo" :label="$t('field.orderNo')" min-width="160" />
          <el-table-column v-if="canShow('customer')" :label="$t('field.customer')" min-width="160">
            <template #default="{ row }">
              {{ getCustomerName(row.customerId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="isApprovedPage && canShow('status')" prop="status" :label="$t('field.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ formatStatus(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('totalAmount')" prop="totalAmount" :label="$t('field.totalAmount')" min-width="140" />
          <el-table-column v-if="canShow('netSaleAmount')" :label="$t('field.netSaleAmount')" min-width="140">
            <template #default="{ row }">
              {{ formatAmount(row.netSaleAmount) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('netGrossProfit')" :label="$t('field.netGrossProfit')" min-width="140">
            <template #default="{ row }">
              {{ formatAmount(row.netGrossProfit) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('receivableStatus')" :label="$t('field.receivableStatus')" min-width="150">
            <template #default="{ row }">
              <el-tag :type="financeStatusTagType(row.receivableStatus)" size="small">
                {{ formatFinanceStatus(row.receivableStatus, row.receivableUnpaidAmount) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isApprovedPage && canShow('returnStatus')" :label="$t('field.returnStatus')" min-width="130">
            <template #default="{ row }">
              <div class="return-tag-list">
                <template v-if="Number(row.approvedReturnCount || 0) > 0">
                  <el-tag
                    v-for="(_, index) in buildReturnTagIndexes(row.approvedReturnCount)"
                    :key="`${row.id}-return-${index}`"
                    type="warning"
                    size="small"
                    class="return-tag-item"
                    :class="{ 'return-tag-item--clickable': canViewSaleReturn }"
                    @click="handleReturnTagClick(row, index)"
                  >
                    {{ `退货${index + 1}` }}
                  </el-tag>
                </template>
                <el-tag v-else type="info" size="small">
                  {{ formatReturnStatus(row.approvedReturnCount) }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column v-if="isApprovedPage && canShow('redFlushTrace')" :label="$t('field.redFlushTrace')" min-width="160">
            <template #default="{ row }">
              <span>{{ row.redFlushTrace || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('createdAt')" prop="createdAt" :label="$t('field.createdTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('table.actions')" width="300" fixed="right">
            <template #default="{ row }">
              <template v-if="isApprovedPage">
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="openViewPage(row)"
                >
                  {{ $t('action.view') }}
                </el-button>
                <el-button
                  link
                  type="primary"
                  size="small"
                  v-permission="'erp-sale:view'"
                  @click="openPrintPage(row)"
                >
                  {{ $t('action.print') }}
                </el-button>
                <el-button
                  link
                  type="primary"
                  size="small"
                  v-permission="'erp-sale:add'"
                  @click="handleCopy(row)"
                >
                  {{ $t('action.copy') }}
                </el-button>
                <el-button
                  v-if="row.status === 'APPROVED'"
                  link
                  type="danger"
                  size="small"
                  v-permission="'erp-sale:redflush'"
                  @click="handleRedFlush(row)"
                >
                  {{ $t('action.redFlush') }}
                </el-button>
              </template>
              <template v-else>
                <el-button
                  v-if="row.status === 'DRAFT'"
                  link
                  type="primary"
                  size="small"
                  v-permission="'erp-sale:edit'"
                  @click="openEditPage(row)"
                >
                  {{ $t('action.edit') }}
                </el-button>
                <el-button
                  link
                  type="primary"
                  size="small"
                  v-permission="'erp-sale:view'"
                  @click="openPrintPage(row)"
                >
                  {{ $t('action.print') }}
                </el-button>
                <el-button
                  v-if="row.status === 'DRAFT'"
                  link
                  type="success"
                  size="small"
                  v-permission="'erp-sale:approve'"
                  @click="handleApprove(row)"
                >
                  {{ $t('action.approve') }}
                </el-button>
                <el-button
                  v-if="row.status === 'DRAFT'"
                  link
                  type="danger"
                  size="small"
                  v-permission="'erp-sale:edit'"
                  @click="handleDelete(row)"
                >
                  {{ $t('action.delete') }}
                </el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="table-pagination">
        <div v-if="showSaleSummaryBar" class="sale-summary-bar">
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
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <PrintPreviewDialog
      v-model="printDialogVisible"
      doc-type="SALE_ORDER"
      :doc-id="printDocId"
      :title="$t('page.erpSaleOrderPrint')"
    />

    <el-dialog
      v-model="saleReturnDetailDialogVisible"
      title="销售退货详情"
      width="1100px"
      destroy-on-close
    >
      <div v-loading="saleReturnDetailLoading" class="sale-return-detail-dialog">
        <template v-if="saleReturnDetail?.order">
          <div class="sale-return-detail-summary">
            <div class="sale-return-detail-summary__item">
              <span class="sale-return-detail-summary__label">{{ $t('field.orderNo') }}</span>
              <span>{{ saleReturnDetail.order.orderNo || '-' }}</span>
            </div>
            <div class="sale-return-detail-summary__item">
              <span class="sale-return-detail-summary__label">{{ $t('field.status') }}</span>
              <el-tag :type="statusTagType(saleReturnDetail.order.status)" size="small">
                {{ formatStatus(saleReturnDetail.order.status) }}
              </el-tag>
            </div>
            <div class="sale-return-detail-summary__item">
              <span class="sale-return-detail-summary__label">{{ $t('field.customer') }}</span>
              <span>{{ getCustomerName(saleReturnDetail.order.customerId) }}</span>
            </div>
            <div class="sale-return-detail-summary__item">
              <span class="sale-return-detail-summary__label">{{ $t('field.returnType') }}</span>
              <span>{{ formatReturnType(saleReturnDetail.order.returnType) }}</span>
            </div>
            <div class="sale-return-detail-summary__item">
              <span class="sale-return-detail-summary__label">{{ $t('field.orderTime') }}</span>
              <span>{{ formatDateTime(saleReturnDetail.order.orderAt) }}</span>
            </div>
            <div class="sale-return-detail-summary__item">
              <span class="sale-return-detail-summary__label">{{ $t('field.settlementMethod') }}</span>
              <span>{{ saleReturnDetail.order.settlementMethod || '-' }}</span>
            </div>
            <div class="sale-return-detail-summary__item">
              <span class="sale-return-detail-summary__label">{{ $t('field.paidAmount') }}</span>
              <span>{{ formatAmount(saleReturnDetail.order.paidAmount) }}</span>
            </div>
            <div class="sale-return-detail-summary__item">
              <span class="sale-return-detail-summary__label">{{ $t('field.discountAmount') }}</span>
              <span>{{ formatAmount(saleReturnDetail.order.discountAmount) }}</span>
            </div>
            <div class="sale-return-detail-summary__item sale-return-detail-summary__item--wide">
              <span class="sale-return-detail-summary__label">{{ $t('field.remark') }}</span>
              <span>{{ saleReturnDetail.order.remark || '-' }}</span>
            </div>
          </div>

          <el-table
            :data="saleReturnDetail.items || []"
            stripe
            border
            class="sale-return-detail-table"
            :empty-text="$t('table.empty')"
          >
            <el-table-column type="index" :label="$t('table.index')" width="70" />
            <el-table-column :label="$t('field.product')" min-width="220">
              <template #default="{ row }">
                {{ row.productName || row.productCode || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="productCode" label="商品编码" min-width="140" />
            <el-table-column :label="$t('field.warehouse')" min-width="140">
              <template #default="{ row }">
                {{ getWarehouseName(row.warehouseId) }}
              </template>
            </el-table-column>
            <el-table-column :label="$t('field.location')" min-width="140">
              <template #default="{ row }">
                {{ getLocationName(row.locationId) }}
              </template>
            </el-table-column>
            <el-table-column :label="$t('field.quantity')" min-width="110">
              <template #default="{ row }">
                {{ formatAmount(row.qty) }}
              </template>
            </el-table-column>
            <el-table-column :label="$t('field.price')" min-width="110">
              <template #default="{ row }">
                {{ formatAmount(row.price) }}
              </template>
            </el-table-column>
            <el-table-column :label="$t('field.lineTotal')" min-width="120">
              <template #default="{ row }">
                {{ formatAmount(row.amount) }}
              </template>
            </el-table-column>
            <el-table-column :label="$t('field.taxRate')" min-width="110">
              <template #default="{ row }">
                {{ formatTaxRate(row.taxRate) }}
              </template>
            </el-table-column>
            <el-table-column label="税额" min-width="120">
              <template #default="{ row }">
                {{ formatAmount(row.taxAmount) }}
              </template>
            </el-table-column>
            <el-table-column label="含税金额" min-width="130">
              <template #default="{ row }">
                {{ formatAmount(row.amountInclTax) }}
              </template>
            </el-table-column>
            <el-table-column :label="$t('field.remark')" min-width="180">
              <template #default="{ row }">
                {{ row.remark || '-' }}
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onActivated, onDeactivated, onBeforeUnmount, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';
import PrintPreviewDialog from '@/components/PrintPreviewDialog.vue';

interface OptionItem {
  id: number;
  name: string;
  warehouseId?: number;
}

interface SaleOrderItem {
  id?: number;
  productId?: number;
  warehouseId?: number;
  locationId?: number;
  qty?: number;
  price?: number;
  taxRate?: number;
  remark?: string;
  sortNo?: number;
}

interface SaleOrder {
  id: number;
  orderNo?: string;
  customerId?: number;
  status: string;
  totalAmount?: number;
  totalAmountInclTax?: number;
  netSaleAmount?: number;
  netGrossProfit?: number;
  cumulativeReturnAmount?: number;
  cumulativeReturnCost?: number;
  receivableStatus?: string;
  receivableUnpaidAmount?: number;
  approvedReturnCount?: number;
  redFlushTrace?: string;
  createdAt?: string;
}

interface SaleReturnSummary {
  id: number;
  orderNo?: string;
  status: string;
  returnType?: string;
  customerId?: number;
  saleOrderId?: number;
  orderAt?: string;
  settlementMethod?: string;
  paidAmount?: number;
  discountAmount?: number;
  remark?: string;
}

interface SaleReturnDetailItem {
  id?: number;
  productId?: number;
  productCode?: string;
  productName?: string;
  warehouseId?: number;
  locationId?: number;
  qty?: number;
  price?: number;
  amount?: number;
  amountInclTax?: number;
  taxRate?: number;
  taxAmount?: number;
  remark?: string;
}

interface SaleReturnDetailData {
  order: SaleReturnSummary;
  items: SaleReturnDetailItem[];
}

type SummaryMode = 'page' | 'range';

interface SaleOrderSummary {
  saleAmountTotal: number;
  returnAmountTotal: number;
  netSaleAmountTotal: number;
  netGrossProfitTotal: number;
  summaryMode: SummaryMode;
}

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = useSystemConfig();

const searchQuery = ref('');
const statusFilter = ref('');
const statusLocked = ref(false);
const customerFilter = ref<number | null>(null);
const dateRange = ref<string[] | null>(null);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<SaleOrder[]>([]);
const printDialogVisible = ref(false);
const printDocId = ref<number | null>(null);
const showProfitColumn = ref(false);
const saleReturnDetailDialogVisible = ref(false);
const saleReturnDetailLoading = ref(false);
const saleReturnDetail = ref<SaleReturnDetailData | null>(null);
const saleReturnSummaryCache = ref<Record<number, SaleReturnSummary[]>>({});
const summary = reactive<SaleOrderSummary>({
  saleAmountTotal: 0,
  returnAmountTotal: 0,
  netSaleAmountTotal: 0,
  netGrossProfitTotal: 0,
  summaryMode: 'page'
});

const customerOptions = ref<OptionItem[]>([]);
const productOptions = ref<OptionItem[]>([]);
const warehouseOptions = ref<OptionItem[]>([]);
const locationOptions = ref<OptionItem[]>([]);

const isDraftPage = computed(() => route.meta.defaultStatus === 'DRAFT');
const isApprovedPage = computed(() => route.meta.defaultStatus === 'APPROVED');
const showSaleSummaryBar = computed(() => isApprovedPage.value || isDraftPage.value);

const statusOptions = computed(() => {
  const base = [
    { value: 'DRAFT', label: t('status.draft') },
    { value: 'APPROVED', label: t('status.approved') },
    { value: 'CANCELLED', label: t('status.cancelled') },
    { value: 'RED_FLUSHED', label: t('status.redFlushed') }
  ];
  if (isApprovedPage.value) {
    base.unshift({ value: 'APPROVED,RED_FLUSHED', label: `${t('status.approved')}/${t('status.redFlushed')}` });
  }
  return base;
});

const pageTitle = computed(() => {
  const key = route.meta.titleKey as string | undefined;
  return key ? t(key) : t('page.erpSaleOrderManagement');
});

const hasPermission = (code: string) => {
  return authStore.hasPermission(code) || authStore.hasPermission(`PERM_${code}`);
};

const canCreate = computed(() => {
  const defaultStatus = route.meta.defaultStatus as string | undefined;
  if (defaultStatus === 'APPROVED') {
    return false;
  }
  if (statusLocked.value && statusFilter.value === 'APPROVED') {
    return false;
  }
  return true;
});

const canViewSaleReturn = computed(() => hasPermission('erp-sale-return:view'));

const canViewProfit = computed(() => {
  return hasPermission('column:erp-sale:profit')
    && (hasPermission('erp-product:cost:view') || hasPermission('erp-product:cost:edit'));
});

const canShowProfit = computed(() => {
  if (!canViewProfit.value) return false;
  return showProfitColumn.value && isVisible('netGrossProfit');
});

const defaultColumns = ['orderNo', 'customer', 'status', 'totalAmount', 'netSaleAmount', 'netGrossProfit', 'receivableStatus', 'returnStatus', 'redFlushTrace', 'createdAt'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-sale', defaultColumns);

const canShow = (key: string) => {
  if (key === 'netGrossProfit') {
    return canShowProfit.value;
  }
  return isVisible(key);
};

const hasSelectedDateRange = computed(() => {
  return Array.isArray(dateRange.value) && dateRange.value.length === 2 && !!dateRange.value[0] && !!dateRange.value[1];
});

const dateRangeShortcuts = computed(() => {
  const now = new Date();
  const buildShortcutRange = (year: number, monthIndex: number) => {
    const start = new Date(year, monthIndex, 1, 0, 0, 0, 0);
    const end = new Date(year, monthIndex + 1, 0, 23, 59, 59, 999);
    return [start, end];
  };
  return [
    {
      text: t('field.thisMonth'),
      value: buildShortcutRange(now.getFullYear(), now.getMonth())
    },
    {
      text: t('field.lastMonth'),
      value: buildShortcutRange(now.getFullYear(), now.getMonth() - 1)
    }
  ];
});

const isTypingTarget = (target: EventTarget | null) => {
  if (!target || !(target instanceof HTMLElement)) return false;
  const tag = target.tagName.toLowerCase();
  if (tag === 'input' || tag === 'textarea' || tag === 'select') return true;
  return target.isContentEditable;
};

const handleKeydown = (event: KeyboardEvent) => {
  if ((!isApprovedPage.value && !isDraftPage.value) || !canViewProfit.value) return;
  if (isTypingTarget(event.target)) return;
  if (event.key && event.key.toLowerCase() === 'u') {
    showProfitColumn.value = !showProfitColumn.value;
  }
};

const statusTagType = (status: string) => {
  if (status === 'APPROVED') return 'success';
  if (status === 'CANCELLED') return 'danger';
  if (status === 'RED_FLUSHED') return 'danger';
  return 'info';
};

const formatStatus = (status: string) => {
  const mapping: Record<string, string> = {
    DRAFT: t('status.draft'),
    APPROVED: t('status.approved'),
    CANCELLED: t('status.cancelled'),
    RED_FLUSHED: t('status.redFlushed')
  };
  return mapping[status] || status;
};

const financeStatusTagType = (status?: string) => {
  if (status === 'SETTLED') return 'success';
  if (status === 'RED_FLUSHED') return 'danger';
  if (status === 'OPEN') return 'warning';
  return 'info';
};

const formatFinanceStatus = (status?: string, unpaidAmount?: number) => {
  if (!status) return '-';
  if (status === 'SETTLED') return t('status.settled');
  if (status === 'RED_FLUSHED') return t('status.redFlushed');
  if (status === 'OPEN') {
    const unpaid = Number(unpaidAmount || 0);
    return unpaid > 0 ? `${t('status.open')} ${formatAmount(unpaid)}` : t('status.open');
  }
  return status;
};

const formatReturnStatus = (count?: number) => {
  const value = Number(count || 0);
  return value > 0 ? `${t('status.hasReturn')} ${value}` : t('status.noReturn');
};

const formatAmount = (value?: number | string) => {
  const num = Number(value || 0);
  return Number.isFinite(num) ? num.toFixed(2) : '0.00';
};

const formatTaxRate = (value?: number | string) => {
  const num = Number(value || 0);
  return Number.isFinite(num) ? `${num.toFixed(2)}%` : '0.00%';
};

const formatReturnType = (value?: string) => {
  if (value === 'RESTOCK') return t('returnType.restock');
  if (value === 'SCRAP') return t('returnType.scrap');
  return value || '-';
};

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', {
    hour12: false,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};

const getCustomerName = (id?: number) => customerOptions.value.find(item => item.id === id)?.name || '-';
const getWarehouseName = (id?: number) => warehouseOptions.value.find(item => item.id === id)?.name || '-';
const getLocationName = (id?: number) => locationOptions.value.find(item => item.id === id)?.name || '-';

const getLocationOptions = (warehouseId?: number) => {
  if (!warehouseId) return locationOptions.value;
  return locationOptions.value.filter(item => item.warehouseId === warehouseId);
};

const buildReturnTagIndexes = (count?: number) => {
  const total = Number(count || 0);
  return total > 0 ? Array.from({ length: total }, (_, index) => index) : [];
};

const resetSummary = (mode: SummaryMode = 'page') => {
  summary.saleAmountTotal = 0;
  summary.returnAmountTotal = 0;
  summary.netSaleAmountTotal = 0;
  summary.netGrossProfitTotal = 0;
  summary.summaryMode = mode;
};

const setSummary = (payload: Partial<SaleOrderSummary> & { summaryMode: SummaryMode }) => {
  summary.saleAmountTotal = Number(payload.saleAmountTotal || 0);
  summary.returnAmountTotal = Number(payload.returnAmountTotal || 0);
  summary.netSaleAmountTotal = Number(payload.netSaleAmountTotal || 0);
  summary.netGrossProfitTotal = Number(payload.netGrossProfitTotal || 0);
  summary.summaryMode = payload.summaryMode;
};

const buildListParams = () => {
  const params: Record<string, any> = {
    page: page.value,
    size: size.value
  };
  if (searchQuery.value) params.keyword = searchQuery.value.trim();
  if (statusFilter.value) params.status = statusFilter.value;
  if (customerFilter.value) params.customerId = customerFilter.value;
  if (hasSelectedDateRange.value && dateRange.value) {
    const start = Number(dateRange.value[0]);
    const end = Number(dateRange.value[1]);
    params.startAt = start;
    params.endAt = end;
  }
  return params;
};

const updateCurrentPageSummary = () => {
  const next = tableData.value.reduce((acc, row) => {
    acc.saleAmountTotal += Number(row.totalAmountInclTax ?? row.totalAmount ?? 0);
    acc.returnAmountTotal += Number(row.cumulativeReturnAmount || 0);
    acc.netSaleAmountTotal += Number(row.netSaleAmount || 0);
    acc.netGrossProfitTotal += Number(row.netGrossProfit || 0);
    return acc;
  }, {
    saleAmountTotal: 0,
    returnAmountTotal: 0,
    netSaleAmountTotal: 0,
    netGrossProfitTotal: 0
  });
  setSummary({
    ...next,
    summaryMode: 'page'
  });
};

const fetchRangeSummary = async () => {
  const params = buildListParams();
  delete params.page;
  delete params.size;
  const res: any = await request.get('/erp/sale-orders/summary', { params });
  if (res.data.code === 200) {
    setSummary({
      saleAmountTotal: res.data.data?.saleAmountTotal,
      returnAmountTotal: res.data.data?.returnAmountTotal,
      netSaleAmountTotal: res.data.data?.netSaleAmountTotal,
      netGrossProfitTotal: res.data.data?.netGrossProfitTotal,
      summaryMode: 'range'
    });
    return;
  }
  resetSummary('range');
};

const summaryLabel = (key: 'saleAmount' | 'returnAmount' | 'netSaleAmount' | 'netGrossProfit') => {
  const labels: Record<typeof key, { page: string; range: string }> = {
    saleAmount: {
      page: t('field.currentPageSaleAmount'),
      range: t('field.rangeSaleAmount')
    },
    returnAmount: {
      page: t('field.currentPageReturnAmount'),
      range: t('field.rangeReturnAmount')
    },
    netSaleAmount: {
      page: t('field.currentPageNetSaleAmount'),
      range: t('field.rangeNetSaleAmount')
    },
    netGrossProfit: {
      page: t('field.currentPageNetGrossProfit'),
      range: t('field.rangeNetGrossProfit')
    }
  };
  return labels[key][summary.summaryMode];
};

const fetchCustomers = async () => {
  try {
    const res: any = await request.get('/erp/customers');
    customerOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchProducts = async () => {
  try {
    const res: any = await request.get('/erp/products');
    productOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchWarehouses = async () => {
  try {
    const res: any = await request.get('/erp/warehouses');
    warehouseOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchLocations = async () => {
  try {
    const res: any = await request.get('/erp/locations');
    locationOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchList = async () => {
  loading.value = true;
  try {
    const params = buildListParams();
    const res: any = await request.get('/erp/sale-orders/page', { params });
    if (res.data.code === 200) {
      tableData.value = res.data.data.items || [];
      total.value = res.data.data.total || 0;
      if (showSaleSummaryBar.value) {
        if (hasSelectedDateRange.value) {
          await fetchRangeSummary();
        } else {
          updateCurrentPageSummary();
        }
      }
    } else if (showSaleSummaryBar.value) {
      resetSummary(hasSelectedDateRange.value ? 'range' : 'page');
    }
  } catch (error) {
    if (showSaleSummaryBar.value) {
      resetSummary(hasSelectedDateRange.value ? 'range' : 'page');
    }
    notifyError(error);
  } finally {
    loading.value = false;
  }
};

const applyRouteStatus = () => {
  const defaultStatus = route.meta.defaultStatus as string | undefined;
  const lockStatus = route.meta.lockStatus === true;
  statusLocked.value = lockStatus;
  if (defaultStatus) {
    if (defaultStatus === 'APPROVED') {
      statusFilter.value = 'APPROVED,RED_FLUSHED';
    } else {
      statusFilter.value = defaultStatus;
    }
    tableData.value = [];
    total.value = 0;
    return;
  }
  if (!lockStatus) {
    statusFilter.value = '';
  }
};

const handleSearch = () => {
  page.value = 1;
  fetchList();
};

const handlePageChange = (newPage: number) => {
  page.value = newPage;
  fetchList();
};

const handleSizeChange = (newSize: number) => {
  size.value = newSize;
  page.value = 1;
  fetchList();
};

const openCreatePage = () => {
  const query: Record<string, string> = { returnTo: route.path };
  if (isDraftPage.value) query.from = 'draft';
  router.push({ path: '/erp/sale-orders/create', query });
};

const openEditPage = (row: SaleOrder) => {
  const query: Record<string, string> = { returnTo: route.path };
  if (isDraftPage.value) query.from = 'draft';
  router.push({ path: `/erp/sale-orders/${row.id}/edit`, query });
};

const openViewPage = (row: SaleOrder) => {
  const query: Record<string, string> = { mode: 'view', returnTo: route.path };
  if (isApprovedPage.value) query.from = 'approved';
  router.push({ path: `/erp/sale-orders/${row.id}/edit`, query });
};

const openPrintPage = (row: SaleOrder) => {
  printDocId.value = row.id;
  printDialogVisible.value = true;
};

const fetchApprovedSaleReturns = async (saleOrderId: number) => {
  if (saleReturnSummaryCache.value[saleOrderId]) {
    return saleReturnSummaryCache.value[saleOrderId];
  }
  const res: any = await request.get(`/erp/sale-returns/sale-order/${saleOrderId}`);
  const records = res.data?.data || [];
  saleReturnSummaryCache.value = {
    ...saleReturnSummaryCache.value,
    [saleOrderId]: records
  };
  return records as SaleReturnSummary[];
};

const handleReturnTagClick = async (row: SaleOrder, index: number) => {
  if (!canViewSaleReturn.value) return;
  try {
    saleReturnDetailLoading.value = true;
    saleReturnDetailDialogVisible.value = true;
    const returns = await fetchApprovedSaleReturns(row.id);
    const targetReturn = returns[index];
    if (!targetReturn?.id) {
      saleReturnDetailDialogVisible.value = false;
      notifyWarning('未找到对应的退货单');
      return;
    }
    const res: any = await request.get(`/erp/sale-returns/${targetReturn.id}`);
    saleReturnDetail.value = res.data?.data || null;
  } catch (error) {
    saleReturnDetailDialogVisible.value = false;
    saleReturnDetail.value = null;
    notifyError(error);
  } finally {
    saleReturnDetailLoading.value = false;
  }
};

const handleApprove = async (row: SaleOrder) => {
  try {
    await ElMessageBox.confirm(
      t('message.confirmApproveDraftOrder', { orderNo: row.orderNo || '-' }),
      t('action.confirm'),
      {
        confirmButtonText: t('action.approve'),
        cancelButtonText: t('action.cancel'),
        type: 'warning'
      }
    );
    await request.post(`/erp/sale-orders/${row.id}/approve`);
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

const handleRedFlush = async (row: SaleOrder) => {
  try {
    const { value } = await ElMessageBox.prompt(
      t('message.confirmRedFlush'),
      t('action.redFlush'),
      {
        inputPlaceholder: t('placeholder.required'),
        confirmButtonText: t('action.confirm'),
        cancelButtonText: t('action.cancel')
      }
    );
    if (!value || !String(value).trim()) {
      return;
    }
    await request.post(`/erp/sale-orders/${row.id}/red-flush`, { reason: String(value).trim() });
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

const handleCopy = async (row: SaleOrder) => {
  try {
    await ElMessageBox.confirm(
      t('message.confirmCopyOrder'),
      t('action.confirm'),
      {
        confirmButtonText: t('action.copy'),
        cancelButtonText: t('action.cancel'),
        type: 'warning'
      }
    );
  } catch {
    return;
  }
  try {
    const detailRes: any = await request.get(`/erp/sale-orders/${row.id}`);
    const detail = detailRes.data?.data;
    if (!detail?.order) {
      notifyWarning(t('message.noItems'));
      return;
    }
    const order = detail.order;
    const items = (detail.items || []).map((item: any, index: number) => ({
      productId: item.productId,
      warehouseId: item.warehouseId,
      locationId: item.locationId,
      qty: item.qty,
      price: item.price,
      taxRate: item.taxRate,
      remark: item.remark,
      sortNo: index + 1
    }));

    const orderNoRes: any = await request.get('/erp/sale-orders/next-order-no');
    const orderNo = orderNoRes.data?.data || '';

    const payload = {
      orderNo,
      orderAt: order.orderAt,
      customerId: order.customerId,
      settlementMethod: order.settlementMethod,
      deliveryMethod: order.deliveryMethod || undefined,
      paidAmount: order.paidAmount,
      discountAmount: order.discountAmount,
      remark: order.remark,
      items
    };
    const createRes: any = await request.post('/erp/sale-orders', payload);
    if (createRes.data.code === 200) {
      const data = createRes.data.data || {};
      const newId = data.order?.id || data.id;
      notifySuccess();
      if (newId) {
        await router.push({ path: `/erp/sale-orders/${newId}/edit`, query: { from: 'draft' } });
      }
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = async (row: SaleOrder) => {
  try {
    await ElMessageBox.confirm(
      t('message.deleteConfirm'),
      t('action.confirm'),
      {
        confirmButtonText: t('action.delete'),
        cancelButtonText: t('action.cancel'),
        type: 'warning'
      }
    );
    await request.delete(`/erp/sale-orders/${row.id}`, {
      data: { reason: '删除销售单草稿' },
      skipDeleteReasonPrompt: true
    } as any);
    notifySuccess();
    fetchList();
  } catch (error) {
    if (error && error !== 'cancel' && error !== 'close') {
      notifyError(error);
    }
  }
};

const rowClassName = ({ row }: { row: SaleOrder }) => {
  if (row.status === 'RED_FLUSHED') return 'row-red-flushed';
  return '';
};

onMounted(() => {
  applyRouteStatus();
  fetchCustomers();
  fetchProducts();
  fetchWarehouses();
  fetchLocations();
  fetchList();
  bindPageSizeSync(size, fetchList);
  fetchTenantKeys();
  window.addEventListener('keydown', handleKeydown);
});

onActivated(() => {
  window.removeEventListener('keydown', handleKeydown);
  window.addEventListener('keydown', handleKeydown);
  applyRouteStatus();
  tableData.value = [];
  total.value = 0;
  fetchCustomers();
  fetchProducts();
  fetchWarehouses();
  fetchLocations();
  fetchList();
});

onDeactivated(() => {
  window.removeEventListener('keydown', handleKeydown);
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown);
});

watch(
  () => route.fullPath,
  () => {
    applyRouteStatus();
    if (showSaleSummaryBar.value) {
      resetSummary(hasSelectedDateRange.value ? 'range' : 'page');
    }
    handleSearch();
  },
  { flush: 'sync' }
);

watch(saleReturnDetailDialogVisible, (visible) => {
  if (!visible) {
    saleReturnDetail.value = null;
    saleReturnDetailLoading.value = false;
  }
});
</script>

<style scoped>
:deep(.erp-toolbar__search--wide) {
  width: 220px;
}

:deep(.erp-toolbar__date-range) {
  width: 380px;
}

:deep(.table-date-range--compact) {
  flex: 0 0 380px;
}

:deep(.table-date-range--compact.el-range-editor) {
  width: 380px !important;
  min-width: 380px !important;
}

:deep(.table-date-range--compact .el-range-input) {
  width: 132px;
}

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

.return-tag-item--clickable {
  cursor: pointer;
}

.sale-return-detail-dialog {
  min-height: 160px;
}

.sale-return-detail-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px 16px;
  margin-bottom: 16px;
}

.sale-return-detail-summary__item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.sale-return-detail-summary__item--wide {
  grid-column: span 4;
}

.sale-return-detail-summary__label {
  color: #6b7280;
  font-size: 12px;
}

.sale-return-detail-table {
  width: 100%;
}

.sale-approved-card .table-body {
  max-height: 100%;
  overflow: auto;
}

.sale-summary-bar {
  width: 100%;
  padding: 0 0 10px;
  border-bottom: 1px solid #eef2f7;
  margin-bottom: 10px;
}

.sale-summary-bar__items {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.sale-summary-item {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  min-width: 0;
  white-space: nowrap;
}

.sale-summary-item__label {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.2;
}

.sale-summary-item__value {
  color: #111827;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.2;
}

.erp-toolbar {
  width: 100%;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  box-sizing: border-box;
}

.table-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
}

.table-filters {
  display: grid;
  grid-template-columns: 220px 220px 380px;
  align-items: center;
  justify-content: start;
  gap: 12px;
  min-width: 0;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: nowrap;
}

@media (max-width: 1280px) {
  .erp-toolbar {
    padding: 14px;
  }

  .table-toolbar {
    grid-template-columns: 1fr;
  }

  .table-filters {
    grid-template-columns: 200px 200px 360px;
  }

  .table-actions {
    justify-content: flex-start;
  }

  :deep(.erp-toolbar__search--wide) {
    width: 200px;
  }

  :deep(.erp-toolbar__date-range) {
    width: 360px;
  }

  :deep(.table-date-range--compact) {
    flex-basis: 360px;
  }

  :deep(.table-date-range--compact.el-range-editor) {
    width: 360px !important;
    min-width: 360px !important;
  }
}

@media (max-width: 768px) {
  .table-filters {
    grid-template-columns: 1fr;
  }

  .table-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .sale-summary-bar__items {
    gap: 10px 14px;
  }

  :deep(.erp-toolbar__search--wide),
  :deep(.erp-toolbar__date-range) {
    width: 100%;
  }

  :deep(.table-date-range--compact) {
    flex-basis: 100%;
  }

  :deep(.table-date-range--compact.el-range-editor) {
    width: 100% !important;
    min-width: 0 !important;
  }
}
</style>
