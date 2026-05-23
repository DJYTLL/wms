import request from '@/utils/request';
import { createTenantScopedResourceCache } from './erpBaseDataCacheCore';

export type BaseOptionItem = {
  id: number;
  name: string;
  code?: string;
  isDefault?: boolean;
  warehouseId?: number;
};

export type CustomerOptionItem = BaseOptionItem & {
  defaultSettlementMethodCode?: string;
  defaultReceiptMethodCode?: string;
  deliveryMethodCode?: string;
};

export type SupplierOptionItem = BaseOptionItem & {
  defaultSettlementMethodCode?: string;
  defaultPaymentMethodCode?: string;
};

export type MethodOptionItem = BaseOptionItem & {
  code: string;
  fundInputMode?: 'HIDDEN' | 'OPTIONAL' | 'REQUIRED';
};

export type ProductOptionItem = BaseOptionItem & {
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

export const getCachedCustomers = (tenantId: number | string): Promise<CustomerOptionItem[]> => (
  cache.getOrLoad(RESOURCE_CUSTOMERS, tenantId, () => loadResource<CustomerOptionItem>('/erp/customers'))
);

export const getCachedSuppliers = (tenantId: number | string): Promise<SupplierOptionItem[]> => (
  cache.getOrLoad(RESOURCE_SUPPLIERS, tenantId, () => loadResource<SupplierOptionItem>('/erp/suppliers'))
);

export const getCachedCategories = (tenantId: number | string): Promise<BaseOptionItem[]> => (
  cache.getOrLoad(RESOURCE_CATEGORIES, tenantId, () => loadResource<BaseOptionItem>('/erp/categories'))
);

export const getCachedCustomerCategories = (tenantId: number | string): Promise<BaseOptionItem[]> => (
  cache.getOrLoad(RESOURCE_CUSTOMER_CATEGORIES, tenantId, () => loadResource<BaseOptionItem>('/erp/customer-categories'))
);

export const getCachedUnits = (tenantId: number | string): Promise<BaseOptionItem[]> => (
  cache.getOrLoad(RESOURCE_UNITS, tenantId, () => loadResource<BaseOptionItem>('/erp/units'))
);

export const getCachedSettlementMethods = (tenantId: number | string): Promise<MethodOptionItem[]> => (
  cache.getOrLoad(RESOURCE_SETTLEMENT_METHODS, tenantId, () => loadResource<MethodOptionItem>('/erp/settlement-methods'))
);

export const getCachedEnabledSettlementMethods = (tenantId: number | string): Promise<MethodOptionItem[]> => (
  cache.getOrLoad(RESOURCE_SETTLEMENT_METHODS_ENABLED, tenantId, () => loadResource<MethodOptionItem>('/erp/settlement-methods?enabled=true'))
);

export const getCachedPaymentMethods = (tenantId: number | string): Promise<MethodOptionItem[]> => (
  cache.getOrLoad(RESOURCE_PAYMENT_METHODS, tenantId, () => loadResource<MethodOptionItem>('/erp/payment-methods'))
);

export const getCachedEnabledPaymentMethods = (tenantId: number | string): Promise<MethodOptionItem[]> => (
  cache.getOrLoad(RESOURCE_PAYMENT_METHODS_ENABLED, tenantId, () => loadResource<MethodOptionItem>('/erp/payment-methods?enabled=true'))
);

export const getCachedReceiptMethods = (tenantId: number | string): Promise<MethodOptionItem[]> => (
  cache.getOrLoad(RESOURCE_RECEIPT_METHODS, tenantId, () => loadResource<MethodOptionItem>('/erp/receipt-methods'))
);

export const getCachedEnabledReceiptMethods = (tenantId: number | string): Promise<MethodOptionItem[]> => (
  cache.getOrLoad(RESOURCE_RECEIPT_METHODS_ENABLED, tenantId, () => loadResource<MethodOptionItem>('/erp/receipt-methods?enabled=true'))
);

export const getCachedDeliveryMethods = (tenantId: number | string): Promise<MethodOptionItem[]> => (
  cache.getOrLoad(RESOURCE_DELIVERY_METHODS, tenantId, () => loadResource<MethodOptionItem>('/erp/delivery-methods'))
);

export const getCachedEnabledDeliveryMethods = (tenantId: number | string): Promise<MethodOptionItem[]> => (
  cache.getOrLoad(RESOURCE_DELIVERY_METHODS_ENABLED, tenantId, () => loadResource<MethodOptionItem>('/erp/delivery-methods?enabled=true'))
);

export const getCachedWarehouses = (tenantId: number | string): Promise<BaseOptionItem[]> => (
  cache.getOrLoad(RESOURCE_WAREHOUSES, tenantId, () => loadResource<BaseOptionItem>('/erp/warehouses'))
);

export const getCachedWarehouseOptions = (tenantId: number | string): Promise<BaseOptionItem[]> => (
  cache.getOrLoad(RESOURCE_WAREHOUSES_OPTIONS, tenantId, () => loadResource<BaseOptionItem>('/erp/warehouses/options'))
);

export const getCachedLocations = (tenantId: number | string): Promise<BaseOptionItem[]> => (
  cache.getOrLoad(RESOURCE_LOCATIONS, tenantId, () => loadResource<BaseOptionItem>('/erp/locations'))
);

export const getCachedLocationOptions = (tenantId: number | string): Promise<BaseOptionItem[]> => (
  cache.getOrLoad(RESOURCE_LOCATIONS_OPTIONS, tenantId, () => loadResource<BaseOptionItem>('/erp/locations/options'))
);

export const getCachedProductOptions = (tenantId: number | string): Promise<ProductOptionItem[]> => (
  cache.getOrLoad(RESOURCE_PRODUCTS_OPTIONS, tenantId, () => loadResource<ProductOptionItem>('/erp/products/options'))
);

export const getCachedVehicleBrands = (tenantId: number | string): Promise<BaseOptionItem[]> => (
  cache.getOrLoad(RESOURCE_VEHICLE_BRANDS, tenantId, () => loadResource<BaseOptionItem>('/erp/vehicle-brands'))
);

export const getCachedVehicleSeries = (tenantId: number | string): Promise<BaseOptionItem[]> => (
  cache.getOrLoad(RESOURCE_VEHICLE_SERIES, tenantId, () => loadResource<BaseOptionItem>('/erp/vehicle-series'))
);

export const getCachedVehicleModels = (tenantId: number | string): Promise<BaseOptionItem[]> => (
  cache.getOrLoad(RESOURCE_VEHICLE_MODELS, tenantId, () => loadResource<BaseOptionItem>('/erp/vehicle-models'))
);

export const invalidateErpBaseDataCache = (tenantId?: number | string) => {
  cache.invalidate(undefined, tenantId);
};
