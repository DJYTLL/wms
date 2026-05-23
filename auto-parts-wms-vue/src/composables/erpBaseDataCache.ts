import request from '@/utils/request';
import { createTenantScopedResourceCache } from './erpBaseDataCacheCore';

type BaseOptionItem = {
  id: number;
  name: string;
  code?: string;
  isDefault?: boolean;
  warehouseId?: number;
};

type CustomerOptionItem = BaseOptionItem & {
  defaultSettlementMethodCode?: string;
  defaultReceiptMethodCode?: string;
  deliveryMethodCode?: string;
};

type SupplierOptionItem = BaseOptionItem & {
  defaultSettlementMethodCode?: string;
  defaultPaymentMethodCode?: string;
};

type MethodOptionItem = BaseOptionItem & {
  code: string;
  fundInputMode?: 'HIDDEN' | 'OPTIONAL' | 'REQUIRED';
};

type ProductOptionItem = BaseOptionItem & {
  defaultWarehouseId?: number;
  defaultLocationId?: number;
  costPrice?: number;
  enabled?: boolean;
};

const RESOURCE_CUSTOMERS = 'customers';
const RESOURCE_SUPPLIERS = 'suppliers';
const RESOURCE_CATEGORIES = 'categories';
const RESOURCE_CUSTOMER_CATEGORIES = 'customer-categories';
const RESOURCE_UNITS = 'units';
const RESOURCE_SETTLEMENT_METHODS = 'settlement-methods';
const RESOURCE_SETTLEMENT_METHODS_ENABLED = 'settlement-methods-enabled';
const RESOURCE_PAYMENT_METHODS = 'payment-methods';
const RESOURCE_PAYMENT_METHODS_ENABLED = 'payment-methods-enabled';
const RESOURCE_RECEIPT_METHODS = 'receipt-methods';
const RESOURCE_RECEIPT_METHODS_ENABLED = 'receipt-methods-enabled';
const RESOURCE_DELIVERY_METHODS = 'delivery-methods';
const RESOURCE_DELIVERY_METHODS_ENABLED = 'delivery-methods-enabled';
const RESOURCE_WAREHOUSES = 'warehouses';
const RESOURCE_WAREHOUSES_OPTIONS = 'warehouses-options';
const RESOURCE_LOCATIONS = 'locations';
const RESOURCE_LOCATIONS_OPTIONS = 'locations-options';
const RESOURCE_PRODUCTS_OPTIONS = 'products-options';
const RESOURCE_VEHICLE_BRANDS = 'vehicle-brands';
const RESOURCE_VEHICLE_SERIES = 'vehicle-series';
const RESOURCE_VEHICLE_MODELS = 'vehicle-models';

const cache = createTenantScopedResourceCache();

const loadResource = async <T = BaseOptionItem>(url: string) => {
  const res: any = await request.get(url);
  return (res.data?.data || []) as T[];
};

export const getCachedCustomers = <T = CustomerOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_CUSTOMERS, tenantId, () => loadResource<T>('/erp/customers'))
);

export const getCachedSuppliers = <T = SupplierOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_SUPPLIERS, tenantId, () => loadResource<T>('/erp/suppliers'))
);

export const getCachedCategories = <T = BaseOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_CATEGORIES, tenantId, () => loadResource<T>('/erp/categories'))
);

export const getCachedCustomerCategories = <T = BaseOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_CUSTOMER_CATEGORIES, tenantId, () => loadResource<T>('/erp/customer-categories'))
);

export const getCachedUnits = <T = BaseOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_UNITS, tenantId, () => loadResource<T>('/erp/units'))
);

export const getCachedSettlementMethods = <T = MethodOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_SETTLEMENT_METHODS, tenantId, () => loadResource<T>('/erp/settlement-methods'))
);

export const getCachedEnabledSettlementMethods = <T = MethodOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_SETTLEMENT_METHODS_ENABLED, tenantId, () => loadResource<T>('/erp/settlement-methods?enabled=true'))
);

export const getCachedPaymentMethods = <T = MethodOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_PAYMENT_METHODS, tenantId, () => loadResource<T>('/erp/payment-methods'))
);

export const getCachedEnabledPaymentMethods = <T = MethodOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_PAYMENT_METHODS_ENABLED, tenantId, () => loadResource<T>('/erp/payment-methods?enabled=true'))
);

export const getCachedReceiptMethods = <T = MethodOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_RECEIPT_METHODS, tenantId, () => loadResource<T>('/erp/receipt-methods'))
);

export const getCachedEnabledReceiptMethods = <T = MethodOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_RECEIPT_METHODS_ENABLED, tenantId, () => loadResource<T>('/erp/receipt-methods?enabled=true'))
);

export const getCachedDeliveryMethods = <T = MethodOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_DELIVERY_METHODS, tenantId, () => loadResource<T>('/erp/delivery-methods'))
);

export const getCachedEnabledDeliveryMethods = <T = MethodOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_DELIVERY_METHODS_ENABLED, tenantId, () => loadResource<T>('/erp/delivery-methods?enabled=true'))
);

export const getCachedWarehouses = <T = BaseOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_WAREHOUSES, tenantId, () => loadResource<T>('/erp/warehouses'))
);

export const getCachedWarehouseOptions = <T = BaseOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_WAREHOUSES_OPTIONS, tenantId, () => loadResource<T>('/erp/warehouses/options'))
);

export const getCachedLocations = <T = BaseOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_LOCATIONS, tenantId, () => loadResource<T>('/erp/locations'))
);

export const getCachedLocationOptions = <T = BaseOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_LOCATIONS_OPTIONS, tenantId, () => loadResource<T>('/erp/locations/options'))
);

export const getCachedProductOptions = <T = ProductOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_PRODUCTS_OPTIONS, tenantId, () => loadResource<T>('/erp/products/options'))
);

export const getCachedVehicleBrands = <T = BaseOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_VEHICLE_BRANDS, tenantId, () => loadResource<T>('/erp/vehicle-brands'))
);

export const getCachedVehicleSeries = <T = BaseOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_VEHICLE_SERIES, tenantId, () => loadResource<T>('/erp/vehicle-series'))
);

export const getCachedVehicleModels = <T = BaseOptionItem>(tenantId: number | string) => (
  cache.getOrLoad(RESOURCE_VEHICLE_MODELS, tenantId, () => loadResource<T>('/erp/vehicle-models'))
);

export const invalidateErpBaseDataCache = (tenantId?: number | string) => {
  cache.invalidate(undefined, tenantId);
};
