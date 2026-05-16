<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpStockTxnManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="table-toolbar inventory-toolbar">
          <div class="table-filters inventory-filters inventory-filters--stock-txn">
            <el-input
              v-model="bizIdFilter"
              :placeholder="$t('field.bizId')"
              class="inventory-field--wide"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-input
              v-model="bizTypeFilter"
              :placeholder="$t('field.bizType')"
              class="inventory-field--wide"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <FuzzyProductSelect
              v-model="productFilter"
              :options="productOptions"
              :placeholder="$t('field.product')"
              class="inventory-field--wide"
              @change="handleSearch"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="table-card stock-txn-card">
      <div class="table-body stock-txn-body">
        <el-table :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')">
          <el-table-column v-if="canShow('docNo')" :label="$t('field.docNo')" min-width="180">
            <template #default="{ row }">
              <el-button
                v-if="canPreviewDoc(row)"
                link
                type="primary"
                class="doc-no-link"
                @click="handlePreviewDoc(row)"
              >
                {{ displayDocNo(row) }}
              </el-button>
              <span v-else>{{ displayDocNo(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('bizType')" prop="bizType" :label="$t('field.bizType')" min-width="200">
            <template #default="{ row }">
              {{ formatBizType(row.bizType) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('product')" :label="$t('field.product')" min-width="180">
            <template #default="{ row }">
              {{ getProductName(row.productId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('warehouse')" :label="$t('field.warehouse')" min-width="160">
            <template #default="{ row }">
              {{ getWarehouseName(row.warehouseId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('location')" :label="$t('field.location')" min-width="160">
            <template #default="{ row }">
              {{ getLocationName(row.locationId) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('adjustmentReason')" :label="$t('field.adjustmentReason')" min-width="140">
            <template #default="{ row }">
              {{ formatAdjustmentReason(row.adjustmentReason) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('qtyDelta')" prop="qtyDelta" :label="$t('field.qtyDelta')" min-width="120" />
          <el-table-column v-if="canShow('qtyBefore')" prop="qtyBefore" :label="$t('field.qtyBefore')" min-width="120" />
          <el-table-column v-if="canShow('qtyAfter')" prop="qtyAfter" :label="$t('field.qtyAfter')" min-width="120" />
          <el-table-column v-if="canShow('operator')" prop="operator" :label="$t('field.actor')" min-width="120" />
          <el-table-column v-if="canShow('remark')" prop="remark" :label="$t('field.remark')" min-width="180" />
          <el-table-column v-if="canShow('unitCost')" :label="$t('field.unitCost')" min-width="120">
            <template #default="{ row }">
              {{ formatMoney(row.unitCost) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('totalCost')" :label="$t('field.totalCost')" min-width="140">
            <template #default="{ row }">
              {{ formatMoney(row.totalCost) }}
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('createdAt')" prop="createdAt" :label="$t('field.createdTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="table-pagination">
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
      v-model="printPreviewVisible"
      :doc-type="printPreviewDocType"
      :doc-id="printPreviewDocId"
      :title="printPreviewTitle"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useColumnSettings } from '@/composables/useColumnSettings';
import FuzzyProductSelect from '@/components/FuzzyProductSelect.vue';
import PrintPreviewDialog from '@/components/PrintPreviewDialog.vue';

interface OptionItem {
  id: number;
  name: string;
  code?: string;
  warehouseId?: number;
}

interface StockTxn {
  id: number;
  txnNo?: string;
  docNo?: string;
  bizType?: string;
  bizId?: number;
  productId?: number;
  warehouseId?: number;
  locationId?: number;
  qtyDelta?: number;
  qtyBefore?: number;
  qtyAfter?: number;
  unitCost?: number;
  totalCost?: number;
  createdAt?: string;
  operator?: string;
  remark?: string;
  adjustmentReason?: string;
}

type PrintDocType = 'SALE_ORDER' | 'PURCHASE_ORDER' | 'SALE_RETURN' | 'PURCHASE_RETURN' | 'STOCK_COUNT' | 'STOCK_TRANSFER' | 'STOCK_INIT';
type DocRouteInfo = {
  endpoint: string;
  noPath: Array<'order' | 'count' | 'transfer' | 'orderNo' | 'countNo' | 'transferNo'>;
};

const { t } = useI18n();
const router = useRouter();
const { notifyError } = useApiError();
const { bindPageSizeSync } = useSystemConfig();

const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const tableData = ref<StockTxn[]>([]);

const productOptions = ref<OptionItem[]>([]);
const warehouseOptions = ref<OptionItem[]>([]);
const locationOptions = ref<OptionItem[]>([]);

const productFilter = ref<number | null>(null);
const bizTypeFilter = ref('');
const bizIdFilter = ref('');
const printPreviewVisible = ref(false);
const printPreviewDocId = ref<number | null>(null);
const printPreviewDocType = ref<PrintDocType>('SALE_ORDER');
const printPreviewTitle = ref('');

const defaultColumns = ['docNo', 'bizType', 'product', 'warehouse', 'location', 'adjustmentReason', 'qtyDelta', 'qtyBefore', 'qtyAfter', 'operator', 'remark', 'unitCost', 'totalCost', 'createdAt'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-stock-txn', defaultColumns);

const canShow = (key: string) => isVisible(key);

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

const formatMoney = (value?: number) => {
  if (value == null || Number.isNaN(value)) return '-';
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4
  });
};

const formatBizType = (value?: string) => {
  if (!value) return '-';
  const key = `bizType.${value}`;
  const translated = t(key);
  return translated === key ? value : translated;
};

const formatAdjustmentReason = (value?: string) => {
  if (!value) return '-';
  const key = `adjustmentReason.${value}`;
  const translated = t(key);
  return translated === key ? value : translated;
};

const getProductName = (id?: number) => productOptions.value.find(item => item.id === id)?.name || '-';
const getWarehouseName = (id?: number) => warehouseOptions.value.find(item => item.id === id)?.name || '-';
const getLocationName = (id?: number) => {
  if (id == null) return t('field.unassignedLocation');
  return locationOptions.value.find(item => item.id === id)?.name || t('field.unassignedLocation');
};

const displayDocNo = (row: StockTxn) => row.docNo || '-';

const previewDocTypeMap: Record<string, PrintDocType> = {
  PURCHASE_APPROVE: 'PURCHASE_ORDER',
  PURCHASE_UNAPPROVE: 'PURCHASE_ORDER',
  PURCHASE_CANCEL: 'PURCHASE_ORDER',
  PURCHASE_RETURN: 'PURCHASE_RETURN',
  PURCHASE_RETURN_SCRAP: 'PURCHASE_RETURN',
  PURCHASE_RETURN_RED_FLUSH: 'PURCHASE_RETURN',
  SALE_APPROVE: 'SALE_ORDER',
  SALE_RED_FLUSH: 'SALE_ORDER',
  SALE_RETURN_RESTOCK: 'SALE_RETURN',
  SALE_RETURN_SCRAP: 'SALE_RETURN',
  SALE_RETURN_RED_FLUSH: 'SALE_RETURN',
  STOCK_COUNT: 'STOCK_COUNT',
  STOCK_TRANSFER_OUT: 'STOCK_TRANSFER',
  STOCK_TRANSFER_IN: 'STOCK_TRANSFER',
  STOCK_INIT: 'STOCK_INIT',
  STOCK_INIT_RED_FLUSH: 'STOCK_INIT'
};

const assemblyViewRouteMap: Record<string, string> = {
  ASSEMBLE_OUT: 'erp-assemble-order-view',
  ASSEMBLE_IN: 'erp-assemble-order-view',
  DISASSEMBLE_OUT: 'erp-disassemble-order-view',
  DISASSEMBLE_IN: 'erp-disassemble-order-view'
};

const docRouteMap: Record<string, DocRouteInfo> = {
  PURCHASE_APPROVE: { endpoint: 'purchase-orders', noPath: ['order', 'orderNo'] },
  PURCHASE_UNAPPROVE: { endpoint: 'purchase-orders', noPath: ['order', 'orderNo'] },
  PURCHASE_CANCEL: { endpoint: 'purchase-orders', noPath: ['order', 'orderNo'] },
  PURCHASE_RETURN: { endpoint: 'purchase-returns', noPath: ['order', 'orderNo'] },
  PURCHASE_RETURN_SCRAP: { endpoint: 'purchase-returns', noPath: ['order', 'orderNo'] },
  PURCHASE_RETURN_RED_FLUSH: { endpoint: 'purchase-returns', noPath: ['order', 'orderNo'] },
  SALE_APPROVE: { endpoint: 'sale-orders', noPath: ['order', 'orderNo'] },
  SALE_RED_FLUSH: { endpoint: 'sale-orders', noPath: ['order', 'orderNo'] },
  SALE_RETURN_RESTOCK: { endpoint: 'sale-returns', noPath: ['order', 'orderNo'] },
  SALE_RETURN_SCRAP: { endpoint: 'sale-returns', noPath: ['order', 'orderNo'] },
  SALE_RETURN_RED_FLUSH: { endpoint: 'sale-returns', noPath: ['order', 'orderNo'] },
  STOCK_COUNT: { endpoint: 'stock-counts', noPath: ['count', 'countNo'] },
  STOCK_TRANSFER_OUT: { endpoint: 'stock-transfers', noPath: ['transfer', 'transferNo'] },
  STOCK_TRANSFER_IN: { endpoint: 'stock-transfers', noPath: ['transfer', 'transferNo'] },
  STOCK_INIT: { endpoint: 'stock-inits', noPath: ['count', 'countNo'] },
  STOCK_INIT_RED_FLUSH: { endpoint: 'stock-inits', noPath: ['count', 'countNo'] },
  ASSEMBLE_OUT: { endpoint: 'assembly-orders', noPath: ['order', 'orderNo'] },
  ASSEMBLE_IN: { endpoint: 'assembly-orders', noPath: ['order', 'orderNo'] },
  DISASSEMBLE_OUT: { endpoint: 'assembly-orders', noPath: ['order', 'orderNo'] },
  DISASSEMBLE_IN: { endpoint: 'assembly-orders', noPath: ['order', 'orderNo'] }
};

const canPreviewDoc = (row: StockTxn) => {
  if (!row.bizType || !row.bizId) return false;
  return !!previewDocTypeMap[row.bizType] || !!assemblyViewRouteMap[row.bizType];
};

const handlePreviewDoc = (row: StockTxn) => {
  if (!row.bizType || !row.bizId) return;
  const printDocType = previewDocTypeMap[row.bizType];
  if (printDocType) {
    printPreviewDocType.value = printDocType;
    printPreviewDocId.value = row.bizId;
    printPreviewTitle.value = `${t('action.preview')}：${displayDocNo(row)}`;
    printPreviewVisible.value = true;
    return;
  }
  const routeName = assemblyViewRouteMap[row.bizType];
  if (routeName) {
    router.push({ name: routeName, params: { id: row.bizId } });
  }
};

const pickDocNo = (detail: any, noPath: DocRouteInfo['noPath']) => {
  let current = detail;
  for (const key of noPath) {
    current = current?.[key];
  }
  return typeof current === 'string' && current.trim() ? current.trim() : '';
};

const resolveDocNos = async (items: StockTxn[]) => {
  const cache = new Map<string, Promise<string>>();
  const resolveOne = (row: StockTxn) => {
    if (row.docNo || !row.bizType || !row.bizId) {
      return Promise.resolve(row.docNo || '');
    }
    const routeInfo = docRouteMap[row.bizType];
    if (!routeInfo) {
      return Promise.resolve('');
    }
    const cacheKey = `${routeInfo.endpoint}:${row.bizId}`;
    if (!cache.has(cacheKey)) {
      cache.set(cacheKey, request.get(`/erp/${routeInfo.endpoint}/${row.bizId}`)
        .then((res: any) => pickDocNo(res.data?.data, routeInfo.noPath))
        .catch(() => ''));
    }
    return cache.get(cacheKey)!;
  };

  await Promise.all(items.map(async (row) => {
    const docNo = await resolveOne(row);
    if (docNo) {
      row.docNo = docNo;
    }
  }));
  return items;
};

const fetchOptions = async () => {
  try {
    const [productsRes, warehousesRes, locationsRes] = await Promise.all([
      request.get('/erp/products'),
      request.get('/erp/warehouses'),
      request.get('/erp/locations')
    ]);
    productOptions.value = productsRes.data.data || [];
    warehouseOptions.value = warehousesRes.data.data || [];
    locationOptions.value = locationsRes.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchList = async () => {
  loading.value = true;
  try {
    const trimmedBizId = bizIdFilter.value.trim();
    if (trimmedBizId && !/^\d+$/.test(trimmedBizId)) {
      notifyError(new Error(t('message.invalidNumber')));
      tableData.value = [];
      total.value = 0;
      return;
    }
    const params: Record<string, any> = {
      page: page.value,
      size: size.value
    };
    if (productFilter.value) params.productId = productFilter.value;
    if (bizTypeFilter.value) params.bizType = bizTypeFilter.value.trim();
    if (trimmedBizId) params.bizId = Number(trimmedBizId);

    const res: any = await request.get('/erp/stock/txns/page', { params });
    if (res.data.code === 200) {
      tableData.value = await resolveDocNos(res.data.data.items || []);
      total.value = res.data.data.total || 0;
    }
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
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

onMounted(() => {
  fetchOptions();
  fetchList();
  bindPageSizeSync(size, fetchList);
  fetchTenantKeys();
});

onActivated(() => {
  fetchOptions();
  fetchList();
});
</script>

<style scoped>
.stock-txn-body {
  max-height: 100%;
  overflow: auto;
}

.doc-no-link {
  padding: 0;
  font-weight: 600;
}
</style>
