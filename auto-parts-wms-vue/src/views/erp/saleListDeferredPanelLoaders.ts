import { markErpNavigationPerf } from '@/utils/erpNavigationPerfTrace';

const createCachedDeferredPanelLoader = <T>(page: string, load: () => Promise<T>) => {
  let cachedPromise: Promise<T> | null = null;
  let hasResolved = false;
  return () => {
    markErpNavigationPerf('sale-order-list:deferred-panel-loader:request', { page });
    if (cachedPromise) {
      if (hasResolved) {
        markErpNavigationPerf('sale-order-list:deferred-panel-loader:cache-hit:resolved', { page });
      } else {
        markErpNavigationPerf('sale-order-list:deferred-panel-loader:cache-hit:pending', { page });
      }
      return cachedPromise;
    }

    hasResolved = false;
    markErpNavigationPerf('sale-order-list:deferred-panel-loader:start', { page });
    cachedPromise = load()
      .then((module) => {
        hasResolved = true;
        markErpNavigationPerf('sale-order-list:deferred-panel-loader:resolved', { page });
        return module;
      })
      .catch((error) => {
        markErpNavigationPerf('sale-order-list:deferred-panel-loader:error', {
          page,
          error: error instanceof Error ? error.message : String(error)
        });
        cachedPromise = null;
        hasResolved = false;
        throw error;
      });
    return cachedPromise;
  };
};

export const loadSaleOrderDraftDeferredPanel = createCachedDeferredPanelLoader('draft', () => import('./SaleOrderDraftDeferredPanel.vue'));
export const loadSaleOrderApprovedDeferredPanel = createCachedDeferredPanelLoader('approved', () => import('./SaleOrderApprovedDeferredPanel.vue'));
export const loadSaleReturnDraftDeferredPanel = createCachedDeferredPanelLoader('return-draft', () => import('./SaleReturnDraftDeferredPanel.vue'));
export const loadSaleReturnApprovedDeferredPanel = createCachedDeferredPanelLoader('return-approved', () => import('./SaleReturnApprovedDeferredPanel.vue'));
