import type { ComputedRef, Ref } from 'vue';
import { mergeOptionById } from '@/utils/erpMasterData';
import type { ErpProductDetail, ProductOption, SaleOrderFormData, SaleOrderItem } from './saleOrderTypes';

interface UseSaleOrderProductSelectionOptions {
  activeRowIndex: Ref<number | null>;
  applyPriceForRow: (row: SaleOrderItem, force?: boolean) => Promise<void>;
  applyProductDefaults: (row: SaleOrderItem, force?: boolean) => void;
  canEditProductInline: ComputedRef<boolean>;
  fetchAssemblyTemplatesByProductId: (productId?: number | null, force?: boolean) => Promise<any[]>;
  fetchStockOptions: (productId?: number, force?: boolean) => Promise<void>;
  findKnownProduct: (productId?: number | null) => ProductOption | undefined;
  formData: SaleOrderFormData;
  getCachedProductOptions: (tenantCacheKey: string) => Promise<ProductOption[]>;
  notifyError: (error: unknown) => void;
  productEditDrawerVisible: Ref<boolean>;
  productEditProductId: Ref<number | null>;
  productEditRow: Ref<SaleOrderItem | null>;
  productOptions: Ref<ProductOption[]>;
  productSearchLoading: Ref<boolean>;
  productSearchOptions: Ref<ProductOption[]>;
  productSearchTimer: Ref<number | null>;
  request: {
    get: (url: string, config?: Record<string, any>) => Promise<any>;
  };
  syncStockKey: (row: SaleOrderItem) => void;
  tenantCacheKey: ComputedRef<string>;
}

export const useSaleOrderProductSelection = ({
  activeRowIndex,
  applyPriceForRow,
  applyProductDefaults,
  canEditProductInline,
  fetchAssemblyTemplatesByProductId,
  fetchStockOptions,
  findKnownProduct,
  formData,
  getCachedProductOptions,
  notifyError,
  productEditDrawerVisible,
  productEditProductId,
  productEditRow,
  productOptions,
  productSearchLoading,
  productSearchOptions,
  productSearchTimer,
  request,
  syncStockKey,
  tenantCacheKey
}: UseSaleOrderProductSelectionOptions) => {
  const rememberProductOption = (product?: Partial<ProductOption> | null) => {
    if (!product?.id) return;
    productOptions.value = mergeOptionById(productOptions.value, {
      id: product.id,
      name: product.name || String(product.id),
      productType: product.productType,
      defaultWarehouseId: product.defaultWarehouseId,
      defaultLocationId: product.defaultLocationId,
      salePrice: product.salePrice,
      costPrice: product.costPrice,
      enabled: product.enabled
    });
  };

  const rememberProductOptions = (products: ProductOption[]) => {
    products.forEach(product => rememberProductOption(product));
  };

  const openProductEditFromOption = (row: SaleOrderItem, productId: number) => {
    if (!canEditProductInline.value || !productId) return;
    activeRowIndex.value = formData.items.indexOf(row);
    productEditRow.value = row;
    productEditProductId.value = productId;
    productEditDrawerVisible.value = true;
  };

  const syncEditedProductToItems = async (product: ErpProductDetail) => {
    const matchedItems = formData.items.filter(item => item.productId === product.id);
    if (!matchedItems.length) return;
    await fetchStockOptions(product.id, true);
    await fetchAssemblyTemplatesByProductId(product.id, true);
    for (const item of matchedItems) {
      item.productName = product.name;
      applyProductDefaults(item, true);
      syncStockKey(item);
      await applyPriceForRow(item, false);
    }
  };

  const handleInlineProductSaved = async (product: ErpProductDetail) => {
    rememberProductOption({
      id: product.id,
      name: product.name,
      productType: product.productType,
      defaultWarehouseId: product.defaultWarehouseId,
      defaultLocationId: product.defaultLocationId,
      salePrice: product.salePrice,
      costPrice: product.costPrice,
      enabled: product.enabled
    });
    productSearchOptions.value = mergeOptionById(productSearchOptions.value, {
      id: product.id,
      name: product.name,
      productType: product.productType,
      defaultWarehouseId: product.defaultWarehouseId,
      defaultLocationId: product.defaultLocationId,
      salePrice: product.salePrice,
      costPrice: product.costPrice,
      enabled: product.enabled
    });
    await syncEditedProductToItems(product);
    productEditRow.value = null;
    productEditProductId.value = null;
  };

  const fetchProducts = async () => {
    try {
      const products = await getCachedProductOptions(tenantCacheKey.value);
      rememberProductOptions(products);
      for (const item of formData.items) {
        await fetchStockOptions(item.productId);
        applyProductDefaults(item, false);
        syncStockKey(item);
        await fetchAssemblyTemplatesByProductId(item.productId);
      }
    } catch (error) {
      notifyError(error);
    }
  };

  const searchProductsNow = async (keyword = '') => {
    productSearchLoading.value = true;
    try {
      const res: any = await request.get('/erp/products/page', {
        params: {
          page: 1,
          size: 20,
          keyword: keyword.trim() || undefined,
          enabled: true
        }
      });
      const products = (res.data?.data?.items || []) as ProductOption[];
      rememberProductOptions(products);
      productSearchOptions.value = products;
    } catch (error) {
      notifyError(error);
    } finally {
      productSearchLoading.value = false;
    }
  };

  const searchProducts = (keyword = '') => {
    const normalizedKeyword = keyword.trim();
    if (!normalizedKeyword) {
      productSearchOptions.value = [];
      if (productSearchTimer.value != null && typeof window !== 'undefined') {
        window.clearTimeout(productSearchTimer.value);
        productSearchTimer.value = null;
      }
      productSearchLoading.value = false;
      return;
    }
    if (productSearchTimer.value != null && typeof window !== 'undefined') {
      window.clearTimeout(productSearchTimer.value);
    }
    if (typeof window === 'undefined') {
      void searchProductsNow(keyword);
      return;
    }
    productSearchTimer.value = window.setTimeout(() => {
      productSearchTimer.value = null;
      void searchProductsNow(keyword);
    }, 250);
  };

  const ensureProductOption = async (productId?: number | null) => {
    if (!productId || findKnownProduct(productId)) return;
    try {
      const res: any = await request.get(`/erp/products/${productId}`);
      const product = res.data.data;
      if (product) {
        rememberProductOption({
          id: product.id,
          name: product.name,
          productType: product.productType,
          defaultWarehouseId: product.defaultWarehouseId,
          defaultLocationId: product.defaultLocationId,
          salePrice: product.salePrice,
          costPrice: product.costPrice,
          enabled: product.enabled
        });
      }
    } catch (error) {
      notifyError(error);
    }
  };

  const getSelectableProductOptions = (currentProductId?: number | null) =>
    productSearchOptions.value
      .filter(item => item.enabled !== false || item.id === currentProductId)
      .concat(
        currentProductId && !productSearchOptions.value.some(item => item.id === currentProductId)
          ? [findKnownProduct(currentProductId)].filter(Boolean) as ProductOption[]
          : []
      );

  return {
    ensureProductOption,
    fetchProducts,
    getSelectableProductOptions,
    handleInlineProductSaved,
    openProductEditFromOption,
    rememberProductOption,
    rememberProductOptions,
    searchProducts,
    searchProductsNow,
    syncEditedProductToItems
  };
};
