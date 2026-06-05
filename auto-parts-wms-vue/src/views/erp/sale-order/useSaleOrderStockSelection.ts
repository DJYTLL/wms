import type { Ref } from 'vue';
import type { OptionItem, ProductOption, SaleOrderItem, StockOption } from './saleOrderTypes';

interface UseSaleOrderStockSelectionOptions {
  locationOptions: Ref<OptionItem[]>;
  notifyError: (error: unknown) => void;
  productOptions: Ref<ProductOption[]>;
  productStockMap: Ref<Record<number, StockOption[]>>;
  request: {
    get: (url: string, config?: Record<string, any>) => Promise<any>;
  };
  t: (key: string) => string;
  warehouseOptions: Ref<OptionItem[]>;
  findKnownProduct: (productId?: number | null) => ProductOption | undefined;
}

export const useSaleOrderStockSelection = ({
  locationOptions,
  notifyError,
  productOptions,
  productStockMap,
  request,
  t,
  warehouseOptions,
  findKnownProduct
}: UseSaleOrderStockSelectionOptions) => {
  const parseStockKey = (stockKey?: string) => {
    if (!stockKey) return { warehouseId: undefined, locationId: undefined };
    const [warehouseRaw, locationRaw] = stockKey.split(':');
    const warehouseId = Number(warehouseRaw);
    const locationId = Number(locationRaw);
    return {
      warehouseId: Number.isNaN(warehouseId) || warehouseId === 0 ? undefined : warehouseId,
      locationId: Number.isNaN(locationId) || locationId === 0 ? undefined : locationId
    };
  };

  const resolveOptionName = (list: OptionItem[], id?: number) => {
    if (!id) return '-';
    return list.find(item => item.id === id)?.name || '-';
  };

  const resolveProductLabel = (row: SaleOrderItem) => {
    return row.productName || resolveOptionName(productOptions.value, row.productId);
  };

  const resolveWarehouseLabel = (row: SaleOrderItem) => {
    return resolveOptionName(warehouseOptions.value, row.warehouseId);
  };

  const resolveLocationLabel = (row: SaleOrderItem) => {
    if (row.locationId) {
      return resolveOptionName(locationOptions.value, row.locationId);
    }
    if (row.stockKey) {
      const { locationId } = parseStockKey(row.stockKey);
      return resolveOptionName(locationOptions.value, locationId);
    }
    return '-';
  };

  const getLocationOptions = (warehouseId?: number) => {
    if (!warehouseId) return locationOptions.value;
    return locationOptions.value.filter(item => item.warehouseId === warehouseId);
  };

  const buildStockKey = (warehouseId: number | null | undefined, locationId: number | null | undefined) => {
    const w = warehouseId == null ? 0 : warehouseId;
    const l = locationId == null ? 0 : locationId;
    return `${w}:${l}`;
  };

  const buildStockOptionLabel = (option: StockOption) => {
    return `${option.warehouseName} / ${option.locationName}`;
  };

  const normalizeStockOption = (option: any): StockOption => {
    const warehouseId = option.warehouseId ?? null;
    const locationId = option.locationId ?? null;
    const warehouseName = option.warehouseName || '-';
    const locationName = option.locationName || t('field.unassignedLocation');
    const qtyOnHand = Number(option.qtyOnHand ?? 0);
    const qtyAvailable = Number(option.qtyAvailable ?? qtyOnHand);
    const qtyLocked = Number(option.qtyLocked ?? 0);
    const baseLabel = `${warehouseName} / ${locationName}`;
    return {
      key: buildStockKey(warehouseId, locationId),
      warehouseId,
      locationId,
      warehouseName,
      locationName,
      qtyOnHand,
      qtyAvailable,
      qtyLocked,
      label: baseLabel,
      searchLabel: `${baseLabel} ${qtyOnHand} ${qtyAvailable} ${qtyLocked}`
    };
  };

  const fetchStockOptions = async (productId?: number, force = false) => {
    if (!productId) return;
    if (!force && productStockMap.value[productId]) return;
    try {
      const res: any = await request.get('/erp/stock/balances/by-product', { params: { productId } });
      const data = res.data.data || [];
      productStockMap.value[productId] = data.map(normalizeStockOption);
    } catch (error) {
      notifyError(error);
    }
  };

  const buildLocationOnlyOptions = (warehouseId: number): StockOption[] => {
    const warehouse = warehouseOptions.value.find(item => item.id === warehouseId);
    const warehouseName = warehouse?.name || '-';
    const locations = locationOptions.value.filter(item => item.warehouseId === warehouseId);
    return locations.map(location => {
      const baseLabel = `${warehouseName} / ${location.name}`;
      return {
        key: buildStockKey(warehouseId, location.id),
        warehouseId,
        locationId: location.id,
        warehouseName,
        locationName: location.name,
        qtyOnHand: 0,
        qtyAvailable: 0,
        qtyLocked: 0,
        label: baseLabel,
        searchLabel: `${baseLabel} 0 0 0`
      };
    });
  };

  const buildFallbackStockOption = (warehouseId?: number, locationId?: number): StockOption | null => {
    if (!warehouseId && !locationId) return null;
    const warehouse = warehouseOptions.value.find(item => item.id === warehouseId);
    const location = locationOptions.value.find(item => item.id === locationId);
    const warehouseName = warehouse?.name || '-';
    const locationName = location?.name || t('field.unassignedLocation');
    return {
      key: buildStockKey(warehouseId ?? null, locationId ?? null),
      warehouseId: warehouseId ?? null,
      locationId: locationId ?? null,
      warehouseName,
      locationName,
      qtyOnHand: 0,
      qtyAvailable: 0,
      qtyLocked: 0,
      label: `${warehouseName} / ${locationName}`,
      searchLabel: `${warehouseName} / ${locationName}`
    };
  };

  const getStockOptionsForRow = (row: SaleOrderItem) => {
    if (!row.productId) return [];
    const allOptions = productStockMap.value[row.productId] || [];
    const options = [...allOptions];
    if (!row.warehouseId && !row.locationId) return options;
    const key = buildStockKey(row.warehouseId ?? null, row.locationId ?? null);
    if (options.some(item => item.key === key)) {
      return options;
    }
    const fallback = buildFallbackStockOption(row.warehouseId, row.locationId);
    if (fallback) {
      options.unshift(fallback);
    }
    return options;
  };

  const syncStockKey = (row: SaleOrderItem) => {
    if (!row.productId) {
      row.stockKey = '';
      return;
    }
    const options = productStockMap.value[row.productId] || [];
    const key = buildStockKey(row.warehouseId ?? null, row.locationId ?? null);
    const matched = options.find(item => item.key === key);
    row.stockKey = matched ? key : (row.warehouseId || row.locationId ? key : '');
  };

  const ensureStockBinding = (row: SaleOrderItem) => {
    if (!row.stockKey || !row.productId) return;
    const options = productStockMap.value[row.productId] || [];
    const selected = options.find(item => item.key === row.stockKey);
    if (selected) {
      row.warehouseId = selected.warehouseId ?? undefined;
      row.locationId = selected.locationId ?? undefined;
      return;
    }
    const parsed = parseStockKey(row.stockKey);
    if (row.warehouseId == null) {
      row.warehouseId = parsed.warehouseId;
    }
    if (row.locationId == null) {
      row.locationId = parsed.locationId;
    }
  };

  const applyProductDefaults = (row: SaleOrderItem, force = true) => {
    if (!row.productId) return;
    const product = findKnownProduct(row.productId);
    if (!product) return;
    if (force) {
      row.warehouseId = product.defaultWarehouseId ?? undefined;
      row.locationId = product.defaultLocationId ?? undefined;
    } else if (product.defaultWarehouseId && !row.warehouseId) {
      row.warehouseId = product.defaultWarehouseId;
    }
    if (!force && product.defaultLocationId && !row.locationId) {
      row.locationId = product.defaultLocationId;
    }
    if (row.locationId && row.warehouseId) {
      const location = locationOptions.value.find(item => item.id === row.locationId);
      if (location && location.warehouseId && location.warehouseId !== row.warehouseId) {
        row.locationId = undefined;
      }
    }
  };

  return {
    applyProductDefaults,
    buildLocationOnlyOptions,
    buildStockKey,
    buildStockOptionLabel,
    ensureStockBinding,
    fetchStockOptions,
    getLocationOptions,
    getStockOptionsForRow,
    parseStockKey,
    resolveLocationLabel,
    resolveProductLabel,
    resolveWarehouseLabel,
    syncStockKey
  };
};
