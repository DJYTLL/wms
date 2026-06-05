import { ref } from 'vue';
import request from '@/utils/request';

export interface SaleListCustomerOption {
  id: number;
  name: string;
}

export const useSaleListCustomerSearch = (notifyError: (error: unknown) => void) => {
  const customerFilter = ref<number | null>(null);
  const customerOptions = ref<SaleListCustomerOption[]>([]);
  const customerSearchLoading = ref(false);
  let customerSearchTimer: ReturnType<typeof window.setTimeout> | null = null;

  const keepSelectedCustomerOption = (options: SaleListCustomerOption[]) => {
    if (!customerFilter.value) return [];
    const selected = options.find((item) => item.id === customerFilter.value)
      || customerOptions.value.find((item) => item.id === customerFilter.value);
    return selected ? [selected] : [];
  };

  const mergeSelectedCustomerOption = (options: SaleListCustomerOption[]) => {
    const selectedOptions = keepSelectedCustomerOption([...customerOptions.value, ...options]);
    const selectedOption = selectedOptions[0];
    if (!selectedOption || options.some((item) => item.id === selectedOption.id)) {
      return options;
    }
    return [selectedOption, ...options];
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
      const customers = (res.data?.data || []) as SaleListCustomerOption[];
      customerOptions.value = mergeSelectedCustomerOption(customers);
    } catch (error) {
      notifyError(error);
    } finally {
      customerSearchLoading.value = false;
    }
  };

  const searchCustomers = (keyword = '') => {
    const normalizedKeyword = keyword.trim();
    if (!normalizedKeyword) {
      customerOptions.value = customerFilter.value ? keepSelectedCustomerOption(customerOptions.value) : [];
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

  const resetCustomerSearch = () => {
    if (customerSearchTimer != null && typeof window !== 'undefined') {
      window.clearTimeout(customerSearchTimer);
      customerSearchTimer = null;
    }
    customerFilter.value = null;
    customerOptions.value = [];
    customerSearchLoading.value = false;
  };

  return {
    customerFilter,
    customerOptions,
    customerSearchLoading,
    keepSelectedCustomerOption,
    searchCustomers,
    resetCustomerSearch
  };
};
