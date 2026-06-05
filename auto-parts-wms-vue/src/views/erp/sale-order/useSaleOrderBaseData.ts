import { ref } from 'vue';
import type { ComputedRef, Reactive, Ref } from 'vue';
import type { CodeOptionItem, OptionItem, SaleOrderFormData, SaleOrderItem } from './saleOrderTypes';

interface UseSaleOrderBaseDataOptions {
  applyProductDefaults: (row: SaleOrderItem, force?: boolean) => void;
  customerCategoryOptions: Ref<OptionItem[]>;
  customerOptions: Ref<OptionItem[]>;
  deliveryMethodOptions: Ref<CodeOptionItem[]>;
  formData: Reactive<SaleOrderFormData>;
  getCachedCustomerCategories: (tenantCacheKey: string) => Promise<OptionItem[]>;
  getCachedEnabledDeliveryMethods: (tenantCacheKey: string) => Promise<CodeOptionItem[]>;
  getCachedEnabledReceiptMethods: (tenantCacheKey: string) => Promise<CodeOptionItem[]>;
  getCachedEnabledSettlementMethods: (tenantCacheKey: string) => Promise<CodeOptionItem[]>;
  getCachedLocationOptions: (tenantCacheKey: string) => Promise<OptionItem[]>;
  getCachedWarehouseOptions: (tenantCacheKey: string) => Promise<OptionItem[]>;
  isCreditSettlement: ComputedRef<boolean>;
  isEditing: ComputedRef<boolean>;
  locationOptions: Ref<OptionItem[]>;
  notifyError: (error: unknown) => void;
  receiptMethodOptions: Ref<CodeOptionItem[]>;
  request: {
    get: (url: string, config?: Record<string, any>) => Promise<any>;
  };
  settlementMethodOptions: Ref<CodeOptionItem[]>;
  tenantCacheKey: ComputedRef<string>;
  warehouseOptions: Ref<OptionItem[]>;
}

export const useSaleOrderBaseData = ({
  applyProductDefaults,
  customerCategoryOptions,
  customerOptions,
  deliveryMethodOptions,
  formData,
  getCachedCustomerCategories,
  getCachedEnabledDeliveryMethods,
  getCachedEnabledReceiptMethods,
  getCachedEnabledSettlementMethods,
  getCachedLocationOptions,
  getCachedWarehouseOptions,
  isCreditSettlement,
  isEditing,
  locationOptions,
  notifyError,
  receiptMethodOptions,
  request,
  settlementMethodOptions,
  tenantCacheKey,
  warehouseOptions
}: UseSaleOrderBaseDataOptions) => {
  const customerSearchLoading = ref(false);
  let customerSearchTimer: ReturnType<typeof window.setTimeout> | null = null;

  const keepSelectedCustomerOption = (options: OptionItem[]) => {
    if (!formData.customerId) return [];
    const selected = options.find((item) => item.id === formData.customerId)
      || customerOptions.value.find((item) => item.id === formData.customerId);
    return selected ? [selected] : [];
  };

  const mergeSelectedCustomerOption = (options: OptionItem[]) => {
    const selectedOptions = keepSelectedCustomerOption([...customerOptions.value, ...options]);
    const selectedOption = selectedOptions[0];
    if (!selectedOption || options.some((item) => item.id === selectedOption.id)) {
      return options;
    }
    return [selectedOption, ...options];
  };

  const getDefaultSettlementMethod = () => {
    if (!settlementMethodOptions.value.length) return '';
    const defaultItem = settlementMethodOptions.value.find(item => item.isDefault) ?? settlementMethodOptions.value[0];
    return defaultItem?.code || '';
  };

  const resolveSettlementMethodCode = (value?: string) => {
    if (!value) return '';
    const normalized = String(value).trim();
    const matched = settlementMethodOptions.value.find(item => item.code === normalized || item.name === normalized);
    return matched?.code || normalized;
  };

  const getDefaultDeliveryMethod = () => {
    if (!deliveryMethodOptions.value.length) return '';
    const defaultItem = deliveryMethodOptions.value.find(item => item.isDefault) ?? deliveryMethodOptions.value[0];
    return defaultItem?.code || '';
  };

  const getDefaultReceiptMethod = () => {
    if (!receiptMethodOptions.value.length) return '';
    const defaultItem = receiptMethodOptions.value.find(item => item.isDefault) ?? receiptMethodOptions.value[0];
    return defaultItem?.code || '';
  };

  const applyMethodsForCustomer = () => {
    const customer = customerOptions.value.find(item => item.id === formData.customerId);
    const settlement = resolveSettlementMethodCode(customer?.defaultSettlementMethodCode) || getDefaultSettlementMethod();
    const receiptMethod = customer?.defaultReceiptMethodCode || getDefaultReceiptMethod();
    const delivery = customer?.deliveryMethodCode || getDefaultDeliveryMethod();
    if (settlement) {
      formData.settlementMethod = settlement;
    }
    if (!isCreditSettlement.value && receiptMethod) {
      formData.receiptMethodCode = receiptMethod;
    } else if (isCreditSettlement.value) {
      formData.receiptMethodCode = '';
      formData.paidAmount = '0';
    }
    if (delivery) {
      formData.deliveryMethod = delivery;
    }
  };

  const applyDefaultMethods = () => {
    if (isEditing.value) return;
    if (!formData.settlementMethod && settlementMethodOptions.value.length) {
      const defaultItem = settlementMethodOptions.value.find(item => item.isDefault) ?? settlementMethodOptions.value[0];
      if (defaultItem) {
        formData.settlementMethod = defaultItem.code;
      }
    }
    if (!isCreditSettlement.value && !formData.receiptMethodCode && receiptMethodOptions.value.length) {
      const defaultItem = receiptMethodOptions.value.find(item => item.isDefault) ?? receiptMethodOptions.value[0];
      if (defaultItem) {
        formData.receiptMethodCode = defaultItem.code;
      }
    }
    if (!formData.deliveryMethod && deliveryMethodOptions.value.length) {
      const defaultItem = deliveryMethodOptions.value.find(item => item.isDefault) ?? deliveryMethodOptions.value[0];
      if (defaultItem) {
        formData.deliveryMethod = defaultItem.code;
      }
    }
  };

  const searchCustomersNow = async (keyword: string) => {
    customerSearchLoading.value = true;
    try {
      const res: any = await request.get('/erp/customers/search', {
        params: {
          size: 20,
          keyword: keyword.trim()
        }
      });
      customerOptions.value = mergeSelectedCustomerOption(res.data?.data || []);
    } catch (error) {
      notifyError(error);
    } finally {
      customerSearchLoading.value = false;
    }
  };

  const searchCustomers = (keyword = '') => {
    const normalizedKeyword = keyword.trim();
    if (!normalizedKeyword) {
      customerOptions.value = formData.customerId ? keepSelectedCustomerOption(customerOptions.value) : [];
      if (customerSearchTimer != null && typeof window !== 'undefined') {
        window.clearTimeout(customerSearchTimer);
        customerSearchTimer = null;
      }
      customerSearchLoading.value = false;
      return;
    }
    if (customerSearchTimer != null && typeof window !== 'undefined') {
      window.clearTimeout(customerSearchTimer);
    }
    if (typeof window === 'undefined') {
      void searchCustomersNow(keyword);
      return;
    }
    customerSearchTimer = window.setTimeout(() => {
      customerSearchTimer = null;
      void searchCustomersNow(keyword);
    }, 250);
  };

  const ensureCustomerOption = async (customerId?: number | null) => {
    if (!customerId || customerOptions.value.some(item => item.id === customerId)) return;
    try {
      const res: any = await request.get(`/erp/customers/${customerId}`);
      const customer = res.data?.data;
      if (customer) {
        customerOptions.value = mergeSelectedCustomerOption([customer]);
      }
    } catch (error) {
      notifyError(error);
    }
  };

  const fetchCustomerCategories = async () => {
    try {
      customerCategoryOptions.value = await getCachedCustomerCategories(tenantCacheKey.value);
    } catch (error) {
      notifyError(error);
    }
  };

  const fetchWarehouses = async () => {
    try {
      warehouseOptions.value = await getCachedWarehouseOptions(tenantCacheKey.value);
    } catch (error) {
      notifyError(error);
    }
  };

  const fetchLocations = async () => {
    try {
      locationOptions.value = await getCachedLocationOptions(tenantCacheKey.value);
      formData.items.forEach(item => applyProductDefaults(item, false));
    } catch (error) {
      notifyError(error);
    }
  };

  const ensureWarehouseOption = async (warehouseId?: number | null) => {
    if (!warehouseId || warehouseOptions.value.some(item => item.id === warehouseId)) return;
    try {
      const res: any = await request.get(`/erp/warehouses/${warehouseId}`);
      const warehouse = res.data.data;
      if (warehouse) {
        warehouseOptions.value = [...warehouseOptions.value, { id: warehouse.id, name: warehouse.name }];
      }
    } catch (error) {
      notifyError(error);
    }
  };

  const ensureLocationOption = async (locationId?: number | null) => {
    if (!locationId || locationOptions.value.some(item => item.id === locationId)) return;
    try {
      const res: any = await request.get(`/erp/locations/${locationId}`);
      const location = res.data.data;
      if (location) {
        locationOptions.value = [...locationOptions.value, { id: location.id, name: location.name, warehouseId: location.warehouseId }];
      }
    } catch (error) {
      notifyError(error);
    }
  };

  const fetchSettlementMethods = async () => {
    try {
      settlementMethodOptions.value = await getCachedEnabledSettlementMethods(tenantCacheKey.value);
      applyDefaultMethods();
    } catch (error) {
      notifyError(error);
    }
  };

  const fetchReceiptMethods = async () => {
    try {
      receiptMethodOptions.value = await getCachedEnabledReceiptMethods(tenantCacheKey.value);
      applyDefaultMethods();
    } catch (error) {
      notifyError(error);
    }
  };

  const fetchDeliveryMethods = async () => {
    try {
      deliveryMethodOptions.value = await getCachedEnabledDeliveryMethods(tenantCacheKey.value);
      applyDefaultMethods();
    } catch (error) {
      notifyError(error);
    }
  };

  return {
    applyDefaultMethods,
    applyMethodsForCustomer,
    customerSearchLoading,
    ensureCustomerOption,
    ensureLocationOption,
    ensureWarehouseOption,
    fetchCustomerCategories,
    fetchDeliveryMethods,
    fetchLocations,
    fetchReceiptMethods,
    fetchSettlementMethods,
    fetchWarehouses,
    getDefaultDeliveryMethod,
    getDefaultReceiptMethod,
    getDefaultSettlementMethod,
    resolveSettlementMethodCode,
    searchCustomers
  };
};
