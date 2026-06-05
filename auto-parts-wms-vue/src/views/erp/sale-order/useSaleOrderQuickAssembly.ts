import { reactive, ref, type Ref } from 'vue';
import type { RouteLocationNormalizedLoaded } from 'vue-router';
import type { AssemblyQuickItem, AssemblyTemplateOption, OptionItem, ProductOption, SaleOrderFormData, SaleOrderItem } from './saleOrderTypes';
import { formatQuickDecimal, parseDecimal, parsePositiveDecimal } from './saleOrderNumberUtils';

interface UseSaleOrderQuickAssemblyOptions {
  activeRowIndex: Ref<number | null>;
  buildStockKey: (warehouseId: number | null | undefined, locationId: number | null | undefined) => string;
  ensureLocationOption: (locationId?: number | null) => Promise<void>;
  ensureProductOption: (productId?: number | null) => Promise<void>;
  ensureWarehouseOption: (warehouseId?: number | null) => Promise<void>;
  fetchAssemblyTemplatesByProductId: (productId?: number | null, force?: boolean) => Promise<AssemblyTemplateOption[]>;
  fetchStockOptions: (productId?: number, force?: boolean) => Promise<void>;
  findKnownProduct: (productId?: number | null) => ProductOption | undefined;
  formData: SaleOrderFormData;
  formatDateTime: (date: Date) => string;
  locationOptions: Ref<OptionItem[]>;
  normalizeArray: <T>(value: any) => T[];
  notifyError: (error: unknown) => void;
  notifySuccess: (message?: string) => void;
  notifyWarning: (message: string) => void;
  request: {
    get: (url: string, config?: Record<string, any>) => Promise<any>;
    post: (url: string, data?: any) => Promise<any>;
  };
  resolveProductLabel: (row: SaleOrderItem) => string;
  route: RouteLocationNormalizedLoaded;
  syncStockKey: (row: SaleOrderItem) => void;
  t: (key: string) => string;
}

export const useSaleOrderQuickAssembly = ({
  activeRowIndex,
  buildStockKey,
  ensureLocationOption,
  ensureProductOption,
  ensureWarehouseOption,
  fetchAssemblyTemplatesByProductId,
  fetchStockOptions,
  findKnownProduct,
  formData,
  formatDateTime,
  locationOptions,
  normalizeArray,
  notifyError,
  notifySuccess,
  notifyWarning,
  request,
  resolveProductLabel,
  route,
  syncStockKey,
  t
}: UseSaleOrderQuickAssemblyOptions) => {
  const assemblyQuickDialogVisible = ref(false);
  const assemblyQuickLoading = ref(false);
  const assemblyQuickSaving = ref(false);
  const assemblyQuickRow = ref<SaleOrderItem | null>(null);
  const assemblyQuickTemplateId = ref<number | null>(null);
  const assemblyQuickTemplateDetail = ref<any | null>(null);
  const assemblyQuickForm = reactive({
    productId: null as number | null,
    productName: '',
    finishedQty: '',
    finishedStockKey: '',
    warehouseId: null as number | null,
    locationId: null as number | null,
    laborCost: '',
    remark: '',
    items: [] as AssemblyQuickItem[]
  });

  const getProductNameById = (productId?: number | null, fallback = '') => {
    if (fallback) return fallback;
    if (!productId) return '-';
    return findKnownProduct(productId)?.name || String(productId);
  };

  const resolveAssemblyItemProductLabel = (row: AssemblyQuickItem) => {
    return getProductNameById(row.productId, row.productName || '');
  };

  const resetAssemblyQuickForm = () => {
    assemblyQuickRow.value = null;
    assemblyQuickTemplateId.value = null;
    assemblyQuickTemplateDetail.value = null;
    assemblyQuickForm.productId = null;
    assemblyQuickForm.productName = '';
    assemblyQuickForm.finishedQty = '';
    assemblyQuickForm.finishedStockKey = '';
    assemblyQuickForm.warehouseId = null;
    assemblyQuickForm.locationId = null;
    assemblyQuickForm.laborCost = '';
    assemblyQuickForm.remark = '';
    assemblyQuickForm.items = [];
  };

  const applyAssemblyFinishedDefaults = () => {
    if (!assemblyQuickForm.productId) return;
    const product = findKnownProduct(assemblyQuickForm.productId);
    if (!product) return;
    if (!assemblyQuickForm.warehouseId && product.defaultWarehouseId) {
      assemblyQuickForm.warehouseId = product.defaultWarehouseId;
    }
    if (!assemblyQuickForm.locationId && product.defaultLocationId) {
      assemblyQuickForm.locationId = product.defaultLocationId;
    }
    if (assemblyQuickForm.locationId && assemblyQuickForm.warehouseId) {
      const location = locationOptions.value.find(item => item.id === assemblyQuickForm.locationId);
      if (location && location.warehouseId && location.warehouseId !== assemblyQuickForm.warehouseId) {
        assemblyQuickForm.locationId = null;
      }
    }
    assemblyQuickForm.finishedStockKey = assemblyQuickForm.warehouseId || assemblyQuickForm.locationId
      ? buildStockKey(assemblyQuickForm.warehouseId, assemblyQuickForm.locationId)
      : '';
  };

  const applyAssemblyItemDefaults = (row: AssemblyQuickItem) => {
    if (!row.productId) return;
    const product = findKnownProduct(row.productId);
    if (!row.warehouseId) {
      row.warehouseId = product?.defaultWarehouseId || assemblyQuickForm.warehouseId || null;
    }
    if (!row.locationId) {
      row.locationId = product?.defaultLocationId || assemblyQuickForm.locationId || null;
    }
    if (row.locationId && row.warehouseId) {
      const location = locationOptions.value.find(item => item.id === row.locationId);
      if (location && location.warehouseId && location.warehouseId !== row.warehouseId) {
        row.locationId = null;
      }
    }
    row.stockKey = row.warehouseId || row.locationId ? buildStockKey(row.warehouseId, row.locationId) : '';
  };

  const applyAssemblyTemplateDetail = async (detail: any, preserveFinishedQty = true) => {
    const template = detail?.template;
    if (!template) return;
    assemblyQuickTemplateDetail.value = detail;
    const templateFinishedQty = Number(template.finishedQty ?? 1) > 0 ? Number(template.finishedQty) : 1;
    const currentQty = parsePositiveDecimal(assemblyQuickForm.finishedQty, 4);
    const targetQty = preserveFinishedQty && currentQty ? currentQty : templateFinishedQty;
    const ratio = templateFinishedQty > 0 ? targetQty / templateFinishedQty : 1;

    assemblyQuickForm.productId = template.finishedProductId || assemblyQuickForm.productId;
    await ensureProductOption(assemblyQuickForm.productId);
    assemblyQuickForm.productName = getProductNameById(assemblyQuickForm.productId);
    assemblyQuickForm.finishedQty = formatQuickDecimal(targetQty, 4);
    assemblyQuickForm.warehouseId = template.warehouseId ?? null;
    assemblyQuickForm.locationId = template.locationId ?? null;
    await Promise.all([
      ensureWarehouseOption(assemblyQuickForm.warehouseId),
      ensureLocationOption(assemblyQuickForm.locationId)
    ]);
    applyAssemblyFinishedDefaults();
    assemblyQuickForm.laborCost = formatQuickDecimal(Number(template.laborCost || 0) * ratio, 4);
    assemblyQuickForm.remark = template.remark || '';

    const rawItems = normalizeArray<any>(detail.items);
    await Promise.all(rawItems.flatMap(item => [
      ensureProductOption(item.productId),
      ensureWarehouseOption(item.warehouseId ?? template.warehouseId ?? null),
      ensureLocationOption(item.locationId ?? template.locationId ?? null)
    ]));
    assemblyQuickForm.items = rawItems.map((item: any) => {
      const row: AssemblyQuickItem = {
        productId: item.productId || null,
        productName: item.productName || '',
        warehouseId: item.warehouseId ?? template.warehouseId ?? null,
        locationId: item.locationId ?? template.locationId ?? null,
        stockKey: '',
        qty: formatQuickDecimal(Number(item.qty || 0) * ratio, 4),
        remark: item.remark || ''
      };
      applyAssemblyItemDefaults(row);
      return row;
    });
  };

  const openAssemblyForRow = async (row: SaleOrderItem) => {
    activeRowIndex.value = formData.items.indexOf(row);
    if (!row.productId) {
      notifyWarning(t('message.selectProductFirst'));
      return;
    }
    const templates = await fetchAssemblyTemplatesByProductId(row.productId, true);
    if (!templates.length) {
      notifyWarning('该商品未维护组装模板');
      return;
    }
    const defaultTemplate = templates[0];
    if (!defaultTemplate) return;
    resetAssemblyQuickForm();
    assemblyQuickRow.value = row;
    assemblyQuickForm.productId = row.productId;
    assemblyQuickForm.productName = resolveProductLabel(row);
    const rowQty = parsePositiveDecimal(row.qty, 4);
    assemblyQuickForm.finishedQty = formatQuickDecimal(rowQty || Number(defaultTemplate.finishedQty || 1) || 1, 4);
    assemblyQuickDialogVisible.value = true;
    assemblyQuickTemplateId.value = defaultTemplate.id;
    await handleAssemblyTemplateChange(defaultTemplate.id);
  };

  const handleAssemblyTemplateChange = async (templateId: number | null) => {
    if (!templateId) {
      assemblyQuickTemplateDetail.value = null;
      assemblyQuickForm.items = [];
      return;
    }
    assemblyQuickLoading.value = true;
    try {
      const res: any = await request.get(`/erp/assembly-templates/${templateId}`);
      await applyAssemblyTemplateDetail(res.data?.data, true);
    } catch (error) {
      notifyError(error);
    } finally {
      assemblyQuickLoading.value = false;
    }
  };

  const handleAssemblyQtyChange = async () => {
    if (!assemblyQuickTemplateDetail.value) return;
    await applyAssemblyTemplateDetail(assemblyQuickTemplateDetail.value, true);
  };

  const handleAssemblyFinishedStockChange = (payload: { stockKey: string; warehouseId: number | null; locationId: number | null }) => {
    assemblyQuickForm.finishedStockKey = payload.stockKey;
    assemblyQuickForm.warehouseId = payload.warehouseId;
    assemblyQuickForm.locationId = payload.locationId;
  };

  const handleAssemblyItemStockChange = (row: AssemblyQuickItem, payload: { stockKey: string; warehouseId: number | null; locationId: number | null }) => {
    row.stockKey = payload.stockKey;
    row.warehouseId = payload.warehouseId;
    row.locationId = payload.locationId;
  };

  const validateAssemblyQuickForm = () => {
    if (!assemblyQuickForm.productId || !assemblyQuickTemplateId.value) {
      notifyWarning(t('message.required'));
      return false;
    }
    if (!parsePositiveDecimal(assemblyQuickForm.finishedQty, 4)) {
      notifyWarning(t('message.mustBePositive'));
      return false;
    }
    const items = assemblyQuickForm.items.filter(item => item.productId);
    if (!items.length) {
      notifyWarning(t('message.noItems'));
      return false;
    }
    for (const item of items) {
      if (!parsePositiveDecimal(item.qty, 4)) {
        notifyWarning(t('message.mustBePositive'));
        return false;
      }
    }
    return true;
  };

  const buildAssemblyQuickPayload = () => ({
    orderNo: null,
    orderType: 'ASSEMBLE',
    orderAt: formatDateTime(new Date()),
    sourceType: route.params.id ? 'SALE_ORDER' : 'MANUAL',
    sourceSaleOrderId: route.params.id ? Number(route.params.id) : undefined,
    sourceSaleOrderItemId: assemblyQuickRow.value?.id,
    customerId: formData.customerId || undefined,
    finishedProductId: assemblyQuickForm.productId,
    finishedQty: parseDecimal(assemblyQuickForm.finishedQty, 4),
    warehouseId: assemblyQuickForm.warehouseId,
    locationId: assemblyQuickForm.locationId,
    laborCost: parseDecimal(assemblyQuickForm.laborCost, 4),
    items: assemblyQuickForm.items
      .filter(item => item.productId)
      .map(item => ({
        productId: item.productId,
        warehouseId: item.warehouseId,
        locationId: item.locationId,
        qty: parseDecimal(item.qty, 4),
        remark: item.remark
      })),
    remark: assemblyQuickForm.remark || undefined
  });

  const saveAssemblyQuickOrder = async (approveAfterSave: boolean) => {
    if (!validateAssemblyQuickForm()) return;
    try {
      assemblyQuickSaving.value = true;
      const res: any = await request.post('/erp/assembly-orders', buildAssemblyQuickPayload());
      const savedId = res.data?.data?.order?.id || null;
      if (approveAfterSave && savedId) {
        await request.post(`/erp/assembly-orders/${savedId}/approve`);
      }
      const row = assemblyQuickRow.value;
      if (row?.productId) {
        await fetchStockOptions(row.productId, true);
        syncStockKey(row);
      }
      assemblyQuickDialogVisible.value = false;
      resetAssemblyQuickForm();
      notifySuccess(approveAfterSave ? '组装单已审核，库存已刷新' : '组装单已保存');
    } catch (error) {
      notifyError(error);
    } finally {
      assemblyQuickSaving.value = false;
    }
  };

  return {
    applyAssemblyTemplateDetail,
    assemblyQuickDialogVisible,
    assemblyQuickForm,
    assemblyQuickLoading,
    assemblyQuickRow,
    assemblyQuickSaving,
    assemblyQuickTemplateId,
    getProductNameById,
    handleAssemblyFinishedStockChange,
    handleAssemblyItemStockChange,
    handleAssemblyQtyChange,
    handleAssemblyTemplateChange,
    openAssemblyForRow,
    resetAssemblyQuickForm,
    resolveAssemblyItemProductLabel,
    saveAssemblyQuickOrder
  };
};
