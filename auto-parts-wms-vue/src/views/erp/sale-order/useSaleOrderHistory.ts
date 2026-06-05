import { computed, ref, watch, type Reactive, type Ref } from 'vue';
import type { Router } from 'vue-router';
import type {
  HistoryDialogTabState,
  OptionItem,
  PriceHistoryItem,
  ProductOption,
  PurchaseHistoryItem,
  SaleHistoryItem,
  SaleOrderFormData,
  SaleOrderItem
} from './saleOrderTypes';

interface UseSaleOrderHistoryOptions {
  activeRowIndex: Ref<number | null>;
  customerCategoryOptions: Ref<OptionItem[]>;
  findKnownProduct: (productId?: number | null) => ProductOption | undefined;
  formData: Reactive<SaleOrderFormData>;
  formatHistoryDate: (value: any) => string;
  formatMoney: (value: number | string | null) => string;
  normalizeArray: <T>(value: any) => T[];
  notifyError: (error: unknown) => void;
  notifyWarning: (message: string) => void;
  request: {
    get: (url: string, config?: Record<string, any>) => Promise<any>;
  };
  router: Router;
  t: (key: string) => string;
}

export const useSaleOrderHistory = ({
  activeRowIndex,
  customerCategoryOptions,
  findKnownProduct,
  formData,
  formatHistoryDate,
  formatMoney,
  normalizeArray,
  notifyError,
  notifyWarning,
  request,
  router,
  t
}: UseSaleOrderHistoryOptions) => {
  const historyDialogVisible = ref(false);
  const historyLoading = ref(false);
  const historyProduct = ref<ProductOption | null>(null);
  const historyTab = ref<'price' | 'purchase' | 'sale-all' | 'sale-customer'>('purchase');
  const saleHistoryItems = ref<SaleHistoryItem[]>([]);
  const customerSaleHistoryItems = ref<SaleHistoryItem[]>([]);
  const purchaseHistoryItems = ref<PurchaseHistoryItem[]>([]);
  const priceHistoryItems = ref<any[]>([]);
  const purchaseHistoryKeyword = ref('');
  const saleHistoryKeyword = ref('');
  const customerSaleHistoryKeyword = ref('');
  const purchaseHistoryRange = ref<string[]>([]);
  const saleHistoryRange = ref<string[]>([]);
  const customerSaleHistoryRange = ref<string[]>([]);
  const purchaseHistoryPage = ref(1);
  const purchaseHistorySize = ref(10);
  const purchaseHistoryTotal = ref(0);
  const saleHistoryPage = ref(1);
  const saleHistorySize = ref(10);
  const saleHistoryTotal = ref(0);
  const customerSaleHistoryPage = ref(1);
  const customerSaleHistorySize = ref(10);
  const customerSaleHistoryTotal = ref(0);
  const historyOrderDialogVisible = ref(false);
  const historyOrderDialogTitle = ref('');
  const historyOrderDialogUrl = ref('');

  const historyProductName = computed(() => {
    if (historyProduct.value?.name) return historyProduct.value.name;
    return '-';
  });

  const saleHistoryHeaderItems = computed(() => [
    { label: t('field.product'), value: historyProductName.value }
  ]);

  const purchaseHistoryTabState = computed<HistoryDialogTabState>(() => ({
    keyword: purchaseHistoryKeyword.value,
    range: purchaseHistoryRange.value,
    page: purchaseHistoryPage.value,
    size: purchaseHistorySize.value,
    total: purchaseHistoryTotal.value
  }));

  const saleHistoryTabState = computed<HistoryDialogTabState>(() => ({
    keyword: saleHistoryKeyword.value,
    range: saleHistoryRange.value,
    page: saleHistoryPage.value,
    size: saleHistorySize.value,
    total: saleHistoryTotal.value
  }));

  const customerSaleHistoryTabState = computed<HistoryDialogTabState>(() => ({
    keyword: customerSaleHistoryKeyword.value,
    range: customerSaleHistoryRange.value,
    page: customerSaleHistoryPage.value,
    size: customerSaleHistorySize.value,
    total: customerSaleHistoryTotal.value
  }));

  const customerCategoryNameMap = computed(() => {
    const map = new Map<number, string>();
    for (const item of customerCategoryOptions.value) {
      if (item.id != null) {
        map.set(item.id, item.name);
      }
    }
    return map;
  });

  const openSaleOrderHistory = (row: SaleHistoryItem) => {
    if (!row?.orderId) return;
    const resolved = router.resolve({ path: `/erp/sale-orders/${row.orderId}/edit`, query: { mode: 'view', embed: '1' } });
    historyOrderDialogTitle.value = `${t('page.erpSaleOrder')} · ${row.orderNo}`;
    historyOrderDialogUrl.value = resolved.href;
    historyOrderDialogVisible.value = true;
  };

  const openPurchaseOrderHistory = (row: PurchaseHistoryItem) => {
    if (!row?.orderId) return;
    const resolved = router.resolve({ path: `/erp/purchase-orders/${row.orderId}/edit`, query: { mode: 'view', embed: '1' } });
    historyOrderDialogTitle.value = `${t('page.erpPurchaseOrder')} · ${row.orderNo}`;
    historyOrderDialogUrl.value = resolved.href;
    historyOrderDialogVisible.value = true;
  };

  const saleHistoryTabs = computed(() => [
    {
      name: 'price',
      label: t('field.customerCategoryPrice'),
      data: priceHistoryItems.value as PriceHistoryItem[],
      columns: [
        { prop: 'customerCategoryName', label: t('field.customerCategory'), minWidth: 160 },
        { label: t('field.price'), width: 120, formatter: (row: PriceHistoryItem) => formatMoney(row.salePrice) },
        { label: t('field.updatedTime'), width: 150, formatter: (row: PriceHistoryItem) => formatHistoryDate(row.updatedAt) }
      ],
      height: 260
    },
    {
      name: 'purchase',
      label: t('field.purchaseHistory'),
      data: purchaseHistoryItems.value,
      state: purchaseHistoryTabState.value,
      columns: [
        { prop: 'supplierName', label: t('field.supplierName'), minWidth: 160 },
        { prop: 'qty', label: t('field.quantity'), width: 90 },
        { label: t('field.price'), width: 110, formatter: (row: PurchaseHistoryItem) => formatMoney(row.price) },
        { label: t('field.priceInclTax'), width: 130, formatter: (row: PurchaseHistoryItem) => formatMoney(row.priceInclTax) },
        { label: t('field.orderTime'), width: 150, formatter: (row: PurchaseHistoryItem) => formatHistoryDate(row.orderAt) },
        { prop: 'orderNo', label: t('field.orderNo'), minWidth: 160, type: 'link', onClick: openPurchaseOrderHistory }
      ],
      height: 260
    },
    {
      name: 'sale-all',
      label: t('field.saleHistory'),
      data: saleHistoryItems.value,
      state: saleHistoryTabState.value,
      columns: [
        { prop: 'customerName', label: t('field.customerName'), minWidth: 160 },
        { prop: 'qty', label: t('field.quantity'), width: 90 },
        { label: t('field.price'), width: 110, formatter: (row: SaleHistoryItem) => formatMoney(row.price) },
        { label: t('field.priceInclTax'), width: 130, formatter: (row: SaleHistoryItem) => formatMoney(row.priceInclTax) },
        { label: t('field.orderTime'), width: 150, formatter: (row: SaleHistoryItem) => formatHistoryDate(row.orderAt) },
        { prop: 'orderNo', label: t('field.orderNo'), minWidth: 160, type: 'link', onClick: openSaleOrderHistory }
      ],
      height: 260
    },
    {
      name: 'sale-customer',
      label: t('field.customerSaleHistory'),
      data: customerSaleHistoryItems.value,
      state: customerSaleHistoryTabState.value,
      columns: [
        { prop: 'customerName', label: t('field.customerName'), minWidth: 160 },
        { prop: 'qty', label: t('field.quantity'), width: 90 },
        { label: t('field.price'), width: 110, formatter: (row: SaleHistoryItem) => formatMoney(row.price) },
        { label: t('field.priceInclTax'), width: 130, formatter: (row: SaleHistoryItem) => formatMoney(row.priceInclTax) },
        { label: t('field.orderTime'), width: 150, formatter: (row: SaleHistoryItem) => formatHistoryDate(row.orderAt) },
        { prop: 'orderNo', label: t('field.orderNo'), minWidth: 160, type: 'link', onClick: openSaleOrderHistory }
      ],
      height: 260
    }
  ]);

  const normalizeHistoryKeyword = (value: string) => value.trim();

  const resolveHistoryRange = (range: string[]) => {
    if (!range || range.length < 2) return null;
    const [start, end] = range;
    if (!start || !end) return null;
    const startDate = new Date(`${start}T00:00:00`);
    const endDate = new Date(`${end}T23:59:59.999`);
    if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime())) return null;
    return {
      startAt: startDate.toISOString(),
      endAt: endDate.toISOString()
    };
  };

  const buildHistoryParams = (range: string[], keyword: string, page: number, size: number) => {
    const productId = historyProduct.value?.id;
    if (!productId) return null;
    const params: Record<string, any> = { productId, page, size };
    const normalized = normalizeHistoryKeyword(keyword);
    if (normalized) params.keyword = normalized;
    const resolved = resolveHistoryRange(range);
    if (resolved) {
      params.startAt = resolved.startAt;
      params.endAt = resolved.endAt;
    }
    return params;
  };

  let historyLoadingCount = 0;
  const withHistoryLoading = async (task: () => Promise<void>) => {
    historyLoadingCount += 1;
    historyLoading.value = true;
    try {
      await task();
    } finally {
      historyLoadingCount -= 1;
      if (historyLoadingCount <= 0) {
        historyLoading.value = false;
        historyLoadingCount = 0;
      }
    }
  };

  const fetchPurchaseHistory = async (page = purchaseHistoryPage.value) => {
    const params = buildHistoryParams(purchaseHistoryRange.value, purchaseHistoryKeyword.value, page, purchaseHistorySize.value);
    if (!params) {
      purchaseHistoryItems.value = [];
      purchaseHistoryTotal.value = 0;
      return;
    }
    await withHistoryLoading(async () => {
      try {
        const res: any = await request.get('/erp/purchase-orders/product-history', { params });
        const data = res?.data?.data || {};
        purchaseHistoryItems.value = normalizeArray<PurchaseHistoryItem>(data);
        purchaseHistoryTotal.value = data.total || 0;
        purchaseHistoryPage.value = data.page || page;
        purchaseHistorySize.value = data.size || purchaseHistorySize.value;
      } catch (error) {
        purchaseHistoryItems.value = [];
        purchaseHistoryTotal.value = 0;
        notifyError(error);
      }
    });
  };

  const fetchSaleHistory = async (page = saleHistoryPage.value) => {
    const params = buildHistoryParams(saleHistoryRange.value, saleHistoryKeyword.value, page, saleHistorySize.value);
    if (!params) {
      saleHistoryItems.value = [];
      saleHistoryTotal.value = 0;
      return;
    }
    await withHistoryLoading(async () => {
      try {
        const res: any = await request.get('/erp/sale-orders/product-history', { params });
        const data = res?.data?.data || {};
        saleHistoryItems.value = normalizeArray<SaleHistoryItem>(data);
        saleHistoryTotal.value = data.total || 0;
        saleHistoryPage.value = data.page || page;
        saleHistorySize.value = data.size || saleHistorySize.value;
      } catch (error) {
        saleHistoryItems.value = [];
        saleHistoryTotal.value = 0;
        notifyError(error);
      }
    });
  };

  const fetchCustomerSaleHistory = async (page = customerSaleHistoryPage.value) => {
    const baseParams = buildHistoryParams(
      customerSaleHistoryRange.value,
      customerSaleHistoryKeyword.value,
      page,
      customerSaleHistorySize.value
    );
    if (!baseParams || !formData.customerId) {
      customerSaleHistoryItems.value = [];
      customerSaleHistoryTotal.value = 0;
      return;
    }
    const params = { ...baseParams, customerId: formData.customerId };
    await withHistoryLoading(async () => {
      try {
        const res: any = await request.get('/erp/sale-orders/product-history', { params });
        const data = res?.data?.data || {};
        customerSaleHistoryItems.value = normalizeArray<SaleHistoryItem>(data);
        customerSaleHistoryTotal.value = data.total || 0;
        customerSaleHistoryPage.value = data.page || page;
        customerSaleHistorySize.value = data.size || customerSaleHistorySize.value;
      } catch (error) {
        customerSaleHistoryItems.value = [];
        customerSaleHistoryTotal.value = 0;
        notifyError(error);
      }
    });
  };

  const fetchProductPrices = async () => {
    const productId = historyProduct.value?.id;
    if (!productId) {
      priceHistoryItems.value = [];
      return;
    }
    try {
      const res: any = await request.get('/erp/product-prices', { params: { productId } });
      const items = normalizeArray<any>(res?.data?.data);
      priceHistoryItems.value = items.map((item: any) => ({
        ...item,
        customerCategoryName: customerCategoryNameMap.value.get(item.customerCategoryId) || '-'
      }));
    } catch (error) {
      notifyError(error);
    }
  };

  const handleHistoryTabChange = (tabName: string | number) => {
    historyTab.value = String(tabName) as typeof historyTab.value;
    if (historyTab.value === 'price') {
      fetchProductPrices();
    } else if (historyTab.value === 'purchase') {
      fetchPurchaseHistory(purchaseHistoryPage.value);
    } else if (historyTab.value === 'sale-all') {
      fetchSaleHistory(saleHistoryPage.value);
    } else if (historyTab.value === 'sale-customer') {
      fetchCustomerSaleHistory(customerSaleHistoryPage.value);
    }
  };

  const handleHistoryDialogFilterChange = (payload: { tabName: string; keyword?: string; range?: string[] }) => {
    if (payload.tabName === 'purchase') {
      if (payload.keyword !== undefined) purchaseHistoryKeyword.value = payload.keyword;
      if (payload.range !== undefined) purchaseHistoryRange.value = payload.range;
      return;
    }
    if (payload.tabName === 'sale-all') {
      if (payload.keyword !== undefined) saleHistoryKeyword.value = payload.keyword;
      if (payload.range !== undefined) saleHistoryRange.value = payload.range;
      return;
    }
    if (payload.tabName === 'sale-customer') {
      if (payload.keyword !== undefined) customerSaleHistoryKeyword.value = payload.keyword;
      if (payload.range !== undefined) customerSaleHistoryRange.value = payload.range;
    }
  };

  const handleHistoryDialogPageChange = (payload: { tabName: string; page: number }) => {
    if (payload.tabName === 'purchase') {
      purchaseHistoryPage.value = payload.page;
      fetchPurchaseHistory(payload.page);
      return;
    }
    if (payload.tabName === 'sale-all') {
      saleHistoryPage.value = payload.page;
      fetchSaleHistory(payload.page);
      return;
    }
    if (payload.tabName === 'sale-customer') {
      customerSaleHistoryPage.value = payload.page;
      fetchCustomerSaleHistory(payload.page);
    }
  };

  const handleHistoryDialogSizeChange = (payload: { tabName: string; size: number }) => {
    if (payload.tabName === 'purchase') {
      purchaseHistorySize.value = payload.size;
      purchaseHistoryPage.value = 1;
      fetchPurchaseHistory(1);
      return;
    }
    if (payload.tabName === 'sale-all') {
      saleHistorySize.value = payload.size;
      saleHistoryPage.value = 1;
      fetchSaleHistory(1);
      return;
    }
    if (payload.tabName === 'sale-customer') {
      customerSaleHistorySize.value = payload.size;
      customerSaleHistoryPage.value = 1;
      fetchCustomerSaleHistory(1);
    }
  };

  const openHistoryForRow = async (row: SaleOrderItem) => {
    activeRowIndex.value = formData.items.indexOf(row);
    if (!row.productId) {
      notifyWarning(t('message.selectProductFirst'));
      return;
    }
    historyProduct.value = findKnownProduct(row.productId)
      || { id: row.productId, name: row.productName || String(row.productId) };
    historyDialogVisible.value = true;
    historyTab.value = 'purchase';
    saleHistoryItems.value = [];
    customerSaleHistoryItems.value = [];
    purchaseHistoryItems.value = [];
    priceHistoryItems.value = [];
    purchaseHistoryKeyword.value = '';
    saleHistoryKeyword.value = '';
    customerSaleHistoryKeyword.value = '';
    purchaseHistoryRange.value = [];
    saleHistoryRange.value = [];
    customerSaleHistoryRange.value = [];
    try {
      purchaseHistoryPage.value = 1;
      saleHistoryPage.value = 1;
      customerSaleHistoryPage.value = 1;
      purchaseHistoryTotal.value = 0;
      saleHistoryTotal.value = 0;
      customerSaleHistoryTotal.value = 0;
      await Promise.all([
        fetchProductPrices(),
        fetchSaleHistory(1),
        fetchPurchaseHistory(1),
        fetchCustomerSaleHistory(1)
      ]);
    } catch (error) {
      notifyError(error);
    }
  };

  watch([purchaseHistoryKeyword, purchaseHistoryRange], () => {
    purchaseHistoryPage.value = 1;
    if (historyDialogVisible.value) {
      fetchPurchaseHistory(1);
    }
  }, { deep: true });

  watch([saleHistoryKeyword, saleHistoryRange], () => {
    saleHistoryPage.value = 1;
    if (historyDialogVisible.value) {
      fetchSaleHistory(1);
    }
  }, { deep: true });

  watch([customerSaleHistoryKeyword, customerSaleHistoryRange], () => {
    customerSaleHistoryPage.value = 1;
    if (historyDialogVisible.value) {
      fetchCustomerSaleHistory(1);
    }
  }, { deep: true });

  watch(() => formData.customerId, () => {
    customerSaleHistoryPage.value = 1;
    if (historyDialogVisible.value && historyTab.value === 'sale-customer') {
      fetchCustomerSaleHistory(1);
    }
  });

  return {
    fetchCustomerSaleHistory,
    fetchProductPrices,
    fetchPurchaseHistory,
    fetchSaleHistory,
    handleHistoryDialogFilterChange,
    handleHistoryDialogPageChange,
    handleHistoryDialogSizeChange,
    handleHistoryTabChange,
    historyDialogVisible,
    historyLoading,
    historyOrderDialogTitle,
    historyOrderDialogUrl,
    historyOrderDialogVisible,
    historyTab,
    openHistoryForRow,
    saleHistoryHeaderItems,
    saleHistoryTabs
  };
};
