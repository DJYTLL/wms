import type { ComputedRef, Reactive, Ref } from 'vue';
import type { RouteLocationNormalizedLoaded, Router } from 'vue-router';
import type { SaleOrderFormData, SaleOrderItem } from './saleOrderTypes';
import { parseAmount, parseDecimal } from './saleOrderNumberUtils';

interface SaveDataOptions {
  closeOnSuccess?: boolean;
  reloadAfterCreate?: boolean;
  showPostSaveDialog?: boolean;
  silentSuccess?: boolean;
}

interface UseSaleOrderSaveFlowOptions {
  closePage: (redirectPath?: string) => void;
  closeTagByPath: (path: string) => void;
  ensureStockBinding: (row: SaleOrderItem) => void;
  formData: Reactive<SaleOrderFormData>;
  getReturnPath: () => string;
  invalidRowFieldMessage: (rowNumber: number, fieldLabel: string) => string;
  isCreditSettlement: ComputedRef<boolean>;
  isEditing: ComputedRef<boolean>;
  isReadOnly: ComputedRef<boolean>;
  isSaving: Ref<boolean>;
  loadDetail: () => Promise<void>;
  nextTick: () => Promise<void>;
  notifyError: (error: unknown) => void;
  notifySuccess: (message?: string) => void;
  notifyWarning: (message: string) => void;
  pendingPrintDocId: Ref<number | null>;
  positiveRowFieldMessage: (rowNumber: number, fieldLabel: string) => string;
  printDialogVisible: Ref<boolean>;
  printDocId: Ref<number | null>;
  request: {
    post: (url: string, data?: any) => Promise<any>;
    put: (url: string, data?: any) => Promise<any>;
  };
  requiredFieldMessage: (fieldLabel: string) => string;
  route: RouteLocationNormalizedLoaded;
  router: Router;
  saveErrorDialogVisible: Ref<boolean>;
  saveErrorMessage: Ref<string>;
  saveSuccessDialogMode: Ref<'save' | 'approve'>;
  saveSuccessDialogVisible: Ref<boolean>;
  saveSuccessOrderId: Ref<number | null>;
  saveSuccessOrderNo: Ref<string>;
  t: (key: string) => string;
}

export const useSaleOrderSaveFlow = ({
  closePage,
  closeTagByPath,
  ensureStockBinding,
  formData,
  getReturnPath,
  invalidRowFieldMessage,
  isCreditSettlement,
  isEditing,
  isReadOnly,
  isSaving,
  loadDetail,
  nextTick,
  notifyError,
  notifySuccess,
  notifyWarning,
  pendingPrintDocId,
  positiveRowFieldMessage,
  printDialogVisible,
  printDocId,
  request,
  requiredFieldMessage,
  route,
  router,
  saveErrorDialogVisible,
  saveErrorMessage,
  saveSuccessDialogMode,
  saveSuccessDialogVisible,
  saveSuccessOrderId,
  saveSuccessOrderNo,
  t
}: UseSaleOrderSaveFlowOptions) => {
  const resolveSaveErrorMessage = (error: unknown) => {
    const payload = (error as any)?.response?.data;
    if (typeof payload?.message === 'string' && payload.message.trim()) {
      return payload.message.trim();
    }
    if (typeof payload?.errorMessage === 'string' && payload.errorMessage.trim()) {
      return payload.errorMessage.trim();
    }
    if (error instanceof Error && error.message.trim()) {
      return error.message.trim();
    }
    return t('message.networkError');
  };

  const showSaveErrorDialog = async (error: unknown) => {
    saveErrorMessage.value = resolveSaveErrorMessage(error);
    saveErrorDialogVisible.value = true;
  };

  const openSaveSuccessDialog = (savedId: number | null, savedOrderNo?: string, mode: 'save' | 'approve' = 'save') => {
    saveSuccessOrderId.value = savedId;
    saveSuccessOrderNo.value = savedOrderNo || formData.orderNo || '';
    saveSuccessDialogMode.value = mode;
    saveSuccessDialogVisible.value = true;
  };

  const closeSaveSuccessDialog = () => {
    saveSuccessDialogVisible.value = false;
    saveSuccessOrderId.value = null;
    saveSuccessOrderNo.value = '';
    saveSuccessDialogMode.value = 'save';
  };

  const saveData = async (options: SaveDataOptions = {}) => {
    if (isSaving.value) return null;
    const closeOnSuccess = options.closeOnSuccess !== false;
    const showPostSaveDialog = options.showPostSaveDialog === true;
    const silentSuccess = options.silentSuccess === true;
    if (!formData.customerId) {
      notifyWarning(requiredFieldMessage(t('field.customer')));
      return;
    }
    if (!formData.settlementMethod) {
      notifyWarning(requiredFieldMessage(t('field.settlementMethod')));
      return;
    }
    const validItems = formData.items.filter(item => item.productId);
    if (!validItems.length) {
      notifyWarning(t('message.noItems'));
      return;
    }
    for (const [index, item] of validItems.entries()) {
      const rowNumber = index + 1;
      const qtyValue = parseDecimal(item.qty, 4);
      if (qtyValue == null || qtyValue <= 0) {
        notifyWarning(positiveRowFieldMessage(rowNumber, t('field.quantity')));
        return;
      }
      const priceValue = parseDecimal(item.price, 4);
      if (priceValue == null) {
        notifyWarning(invalidRowFieldMessage(rowNumber, t('field.price')));
        return;
      }
    }

    const paidAmount = isCreditSettlement.value ? 0 : parseAmount(formData.paidAmount);
    const discountAmount = parseAmount(formData.discountAmount);
    if (paidAmount == null || discountAmount == null) {
      notifyWarning(t('message.invalidNumber'));
      return;
    }

    const payload = {
      orderNo: formData.orderNo || undefined,
      orderAt: formData.orderAt || undefined,
      customerId: formData.customerId,
      settlementMethod: formData.settlementMethod,
      receiptMethodCode: isCreditSettlement.value ? undefined : (formData.receiptMethodCode || undefined),
      deliveryMethod: formData.deliveryMethod || undefined,
      paidAmount,
      discountAmount,
      remark: formData.remark,
      items: validItems.map((item, index) => {
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
    };

    try {
      isSaving.value = true;
      const res: any = isEditing.value
        ? await request.put(`/erp/sale-orders/draft/${route.params.id}`, payload)
        : await request.post('/erp/sale-orders/draft', payload);

      if (res.data.code === 200) {
        const data = res.data.data || {};
        const savedId = data.order?.id || data.id || Number(route.params.id || 0) || null;
        const savedOrderNo = data.order?.orderNo || data.orderNo || formData.orderNo || '';
        if (!isEditing.value && savedId && options.reloadAfterCreate) {
          if (!silentSuccess) {
            notifySuccess(t('message.saveSuccess'));
          }
          await router.replace(`/erp/sale-orders/draft/${savedId}/edit`);
        }
        if (savedId && showPostSaveDialog) {
          openSaveSuccessDialog(savedId, savedOrderNo);
          return savedId;
        }
        if (!silentSuccess) {
          notifySuccess(t('message.saveSuccess'));
        }
        if (closeOnSuccess) {
          closePage(getReturnPath());
        }
        return savedId;
      }
    } catch (error) {
      await showSaveErrorDialog(error);
    } finally {
      isSaving.value = false;
    }
    return null;
  };

  const handleSave = async () => {
    if (isSaving.value) return;
    await saveData({ closeOnSuccess: false, showPostSaveDialog: true });
  };

  const handleSaveAndBack = async () => {
    if (isSaving.value) return;
    await saveData({ closeOnSuccess: true });
  };

  const handleSaveSuccessDialogClosed = async () => {
    const docId = pendingPrintDocId.value;
    pendingPrintDocId.value = null;
    if (!docId) return;
    printDocId.value = docId;
    await nextTick();
    printDialogVisible.value = true;
  };

  const handleContinueCreate = async () => {
    closeSaveSuccessDialog();
    const createRoute = {
      path: '/erp/sale-orders/draft/create',
      query: { from: 'draft', returnTo: '/erp/sale-orders/draft' }
    };
    const targetFullPath = router.resolve(createRoute).fullPath;
    if (isEditing.value) {
      if (route.fullPath !== targetFullPath) {
        await router.replace(createRoute);
      }
      await loadDetail();
      return;
    }
    if (route.query.mode === 'view' || route.query.from !== 'draft') {
      if (route.fullPath !== targetFullPath) {
        await router.replace(createRoute);
      }
    }
    await loadDetail();
  };

  const handleStayOnCurrentOrder = async () => {
    const savedId = saveSuccessOrderId.value;
    const dialogMode = saveSuccessDialogMode.value;
    closeSaveSuccessDialog();
    if (!savedId) return;
    if (dialogMode === 'approve') return;
    await router.replace(`/erp/sale-orders/draft/${savedId}/edit`);
  };

  const handleBackToList = async () => {
    closeSaveSuccessDialog();
    closePage(getReturnPath());
  };

  const handlePrintSavedOrder = () => {
    const savedId = saveSuccessOrderId.value;
    if (!savedId) return;
    pendingPrintDocId.value = savedId;
    closeSaveSuccessDialog();
  };

  const handleApproveSavedOrder = async () => {
    const savedId = saveSuccessOrderId.value;
    if (!savedId) return;
    const sourcePath = route.path;
    try {
      const savedOrderNo = saveSuccessOrderNo.value;
      await request.post(`/erp/sale-orders/draft/${savedId}/approve`);
      closeSaveSuccessDialog();
      await router.replace({
        path: `/erp/sale-orders/approved/${savedId}`,
        query: { mode: 'view', from: 'approved', returnTo: '/erp/sale-orders/approved' }
      });
      await nextTick();
      if (sourcePath !== route.path) {
        closeTagByPath(sourcePath);
      }
      await loadDetail();
      openSaveSuccessDialog(savedId, savedOrderNo, 'approve');
    } catch (error) {
      notifyError(error);
    }
  };

  const openPrintPreview = (docId?: number | null) => {
    if (!docId) return;
    printDocId.value = docId;
    printDialogVisible.value = true;
  };

  const handlePrint = async () => {
    if (!isEditing.value) {
      const savedId = await saveData({ closeOnSuccess: false, showPostSaveDialog: false, silentSuccess: true });
      openPrintPreview(savedId);
      return;
    }
    const id = route.params.id;
    if (!id) return;
    openPrintPreview(Number(id));
  };

  const handleApprove = async () => {
    if (isSaving.value) return;
    if (isReadOnly.value) return;
    const sourcePath = route.path;
    try {
      await import('element-plus').then(({ ElMessageBox }) => ElMessageBox.confirm(
        t('message.confirmApprove'),
        t('action.confirm'),
        {
          confirmButtonText: t('action.approve'),
          cancelButtonText: t('action.cancel'),
          type: 'warning'
        }
      ));
    } catch {
      return;
    }

    const savedId = await saveData({ closeOnSuccess: false, reloadAfterCreate: true, silentSuccess: true });
    if (!savedId) return;

    try {
      const savedOrderNo = formData.orderNo;
      await request.post(`/erp/sale-orders/draft/${savedId}/approve`);
      formData.status = 'APPROVED';
      await router.replace({
        path: `/erp/sale-orders/approved/${savedId}`,
        query: { mode: 'view', from: 'approved', returnTo: '/erp/sale-orders/approved' }
      });
      await nextTick();
      if (sourcePath !== route.path) {
        closeTagByPath(sourcePath);
      }
      await loadDetail();
      openSaveSuccessDialog(savedId, savedOrderNo, 'approve');
    } catch (error) {
      if (error && error !== 'cancel' && error !== 'close') {
        notifyError(error);
      }
    }
  };

  return {
    closeSaveSuccessDialog,
    handleApprove,
    handleApproveSavedOrder,
    handleBackToList,
    handleContinueCreate,
    handlePrint,
    handlePrintSavedOrder,
    handleSave,
    handleSaveAndBack,
    handleSaveSuccessDialogClosed,
    handleStayOnCurrentOrder,
    openSaveSuccessDialog,
    saveData
  };
};
