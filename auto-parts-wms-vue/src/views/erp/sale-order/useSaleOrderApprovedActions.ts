import type { ComputedRef, Reactive } from 'vue';
import type { RouteLocationNormalizedLoaded, Router } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import type { SaleOrderFormData, SaleOrderItem } from './saleOrderTypes';
import { parseAmount, parseDecimal } from './saleOrderNumberUtils';

interface UseSaleOrderApprovedActionsOptions {
  closePage: () => void;
  ensureStockBinding: (row: SaleOrderItem) => void;
  formData: Reactive<SaleOrderFormData>;
  formatDateTime: (date: Date) => string;
  hasPermission: (code: string) => boolean;
  isEditing: ComputedRef<boolean>;
  notifyError: (error: unknown) => void;
  notifySuccess: (message?: string) => void;
  notifyWarning: (message: string) => void;
  request: {
    get: (url: string, config?: Record<string, any>) => Promise<any>;
    post: (url: string, data?: any) => Promise<any>;
  };
  route: RouteLocationNormalizedLoaded;
  router: Router;
  t: (key: string) => string;
}

export const useSaleOrderApprovedActions = ({
  closePage,
  ensureStockBinding,
  formData,
  formatDateTime,
  hasPermission,
  isEditing,
  notifyError,
  notifySuccess,
  notifyWarning,
  request,
  route,
  router,
  t
}: UseSaleOrderApprovedActionsOptions) => {
  const handleCopy = async () => {
    if (!hasPermission('erp-sale-draft:add')) {
      notifyWarning('缺少销售草稿新增权限');
      return;
    }
    if (!formData.customerId || !formData.items.length) {
      notifyWarning(t('message.noItems'));
      return;
    }
    try {
      await ElMessageBox.confirm(
        t('message.confirmCopyOrder'),
        t('action.confirm'),
        {
          confirmButtonText: t('action.copy'),
          cancelButtonText: t('action.cancel'),
          type: 'warning'
        }
      );
    } catch {
      return;
    }
    try {
      const res: any = route.params.id
        ? await request.post(`/erp/sale-orders/approved/${route.params.id}/copy`)
        : await request.post('/erp/sale-orders/draft', {
            orderNo: ((await request.get('/erp/sale-orders/draft/next-order-no')) as any).data?.data || '',
            orderAt: formatDateTime(new Date()),
            customerId: formData.customerId,
            settlementMethod: formData.settlementMethod,
            deliveryMethod: formData.deliveryMethod || undefined,
            paidAmount: parseAmount(formData.paidAmount),
            discountAmount: parseAmount(formData.discountAmount),
            remark: formData.remark,
            items: formData.items
              .filter(item => item.productId)
              .map((item, index) => {
                ensureStockBinding(item);
                return {
                  productId: item.productId,
                  warehouseId: item.warehouseId,
                  locationId: item.locationId,
                  qty: parseDecimal(item.qty, 4),
                  price: parseDecimal(item.price, 4),
                  taxRate: item.taxRate,
                  remark: item.remark,
                  sortNo: index + 1
                };
              })
          });
      if (res.data.code === 200) {
        const data = res.data.data || {};
        const newId = data.order?.id || data.id;
        notifySuccess();
        if (newId) {
          await router.push({ path: `/erp/sale-orders/draft/${newId}/edit`, query: { from: 'draft' } });
        }
      }
    } catch (error) {
      notifyError(error);
    }
  };

  const handleRedFlush = async () => {
    if (!isEditing.value) return;
    try {
      const { value } = await ElMessageBox.prompt(
        t('message.confirmRedFlush'),
        t('action.redFlush'),
        {
          inputPlaceholder: t('placeholder.required'),
          confirmButtonText: t('action.confirm'),
          cancelButtonText: t('action.cancel')
        }
      );
      if (!value || !String(value).trim()) {
        return;
      }
      await request.post(`/erp/sale-orders/approved/${route.params.id}/red-flush`, { reason: String(value).trim() });
      notifySuccess();
      closePage();
      await router.push('/erp/sale-orders/approved');
    } catch (error) {
      if (error && error !== 'cancel' && error !== 'close') {
        notifyError(error);
      }
    }
  };

  const handleCancel = async () => {
    if (!isEditing.value) return;
    try {
      const { value } = await ElMessageBox.prompt(
        t('message.confirmCancel'),
        t('action.cancel'),
        {
          inputPlaceholder: t('placeholder.required'),
          confirmButtonText: t('action.confirm'),
          cancelButtonText: t('action.cancel')
        }
      );
      if (!value || !String(value).trim()) return;
      await request.post(`/erp/sale-orders/approved/${route.params.id}/cancel`, { reason: String(value).trim() });
      notifySuccess();
      closePage();
      await router.push('/erp/sale-orders/approved');
    } catch (error) {
      if (error && error !== 'cancel' && error !== 'close') {
        notifyError(error);
      }
    }
  };

  return {
    handleCancel,
    handleCopy,
    handleRedFlush
  };
};
