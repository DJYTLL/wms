<template>
  <PrintPreviewDialog
    v-if="visible"
    v-model="visible"
    :doc-type="docType"
    :doc-id="docId"
    :title="$t('page.erpSaleOrderPrint')"
  />
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue';

const PrintPreviewDialog = defineAsyncComponent(() => import('@/components/PrintPreviewDialog.vue'));

type PrintDocType =
  | 'SALE_ORDER'
  | 'SALE_ORDER_DRAFT'
  | 'SALE_ORDER_APPROVED'
  | 'PURCHASE_ORDER'
  | 'PURCHASE_ORDER_DRAFT'
  | 'PURCHASE_ORDER_APPROVED'
  | 'SALE_RETURN'
  | 'SALE_RETURN_DRAFT'
  | 'SALE_RETURN_APPROVED'
  | 'PURCHASE_RETURN'
  | 'PURCHASE_RETURN_DRAFT'
  | 'PURCHASE_RETURN_APPROVED'
  | 'RECEIPT'
  | 'PAYMENT'
  | 'ACCOUNTS_RECEIVABLE'
  | 'ACCOUNTS_PAYABLE'
  | 'STOCK_COUNT'
  | 'STOCK_TRANSFER'
  | 'STOCK_INIT';

const props = defineProps<{
  modelValue: boolean;
  docType: PrintDocType;
  docId: number | null;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});
</script>
