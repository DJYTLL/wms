<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="92%"
    top="4vh"
    class="print-preview-dialog"
    append-to-body
    @closed="handleClosed"
  >
    <div class="print-preview-toolbar">
      <el-button size="small" type="primary" :disabled="!isLoaded" @click="handlePrint">
        {{ $t('action.print') }}
      </el-button>
      <el-button size="small" @click="visible = false">{{ $t('action.close') }}</el-button>
    </div>
    <div class="print-preview-body">
      <iframe
        ref="frameRef"
        :src="previewUrl"
        class="print-preview-frame"
        @load="handleLoad"
      />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';
import { fetchPrintTemplate } from '@/utils/printTemplate';
import { directPrintWindow } from '@/utils/directPrint';

interface Props {
  modelValue: boolean;
  docType:
    | 'SALE_ORDER'
    | 'PURCHASE_ORDER'
    | 'SALE_RETURN'
    | 'PURCHASE_RETURN'
    | 'RECEIPT'
    | 'PAYMENT'
    | 'ACCOUNTS_RECEIVABLE'
    | 'ACCOUNTS_PAYABLE'
    | 'STOCK_COUNT'
    | 'STOCK_TRANSFER'
    | 'STOCK_INIT';
  docId?: number | null;
  templateId?: number | null;
  previewConfigKey?: string | null;
  title?: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>();
const { t } = useI18n();

const frameRef = ref<HTMLIFrameElement | null>(null);
const isLoaded = ref(false);
const templateId = ref<number | null>(null);

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
});

const dialogTitle = computed(() => props.title || t('action.print'));

const previewUrl = computed(() => {
  if (!props.docId) return 'about:blank';
  const prefixMap: Record<string, string> = {
    SALE_ORDER: 'sale-orders',
    PURCHASE_ORDER: 'purchase-orders',
    SALE_RETURN: 'sale-returns',
    PURCHASE_RETURN: 'purchase-returns',
    RECEIPT: 'receipts',
    PAYMENT: 'payments',
    ACCOUNTS_RECEIVABLE: 'ar',
    ACCOUNTS_PAYABLE: 'ap',
    STOCK_COUNT: 'stock-counts',
    STOCK_TRANSFER: 'stock-transfers',
    STOCK_INIT: 'stock-inits'
  };
  const prefix = prefixMap[props.docType] || 'sale-orders';
  const query = new URLSearchParams({ preview: '1' });
  if (props.templateId) {
    query.set('templateId', String(props.templateId));
  }
  if (props.previewConfigKey) {
    query.set('previewConfigKey', props.previewConfigKey);
  }
  return `/erp/${prefix}/${props.docId}/print?${query.toString()}`;
});

const fetchTemplate = async () => {
  if (!props.docType) return;
  try {
    const template = await fetchPrintTemplate(props.docType, props.templateId);
    templateId.value = template?.id || null;
  } catch {
    templateId.value = null;
  }
};

const handleLoad = () => {
  isLoaded.value = true;
};

const handlePrint = async () => {
  if (!props.docId) return;
  try {
    await request.post('/erp/print/logs', {
      docType: props.docType,
      docId: props.docId,
      templateId: templateId.value
    });
  } catch {
    // ignore log errors
  }
  const frame = frameRef.value?.contentWindow;
  if (frame) {
    const printed = await directPrintWindow(frame, { removeSelectors: ['.print-toolbar'] });
    if (!printed) {
      frame.focus();
      frame.print();
    }
  } else {
    ElMessage.error(t('message.printContentEmpty'));
  }
};

const handleClosed = () => {
  isLoaded.value = false;
  templateId.value = null;
};

watch(
  () => [visible.value, props.docType, props.docId],
  async ([open, docType, docId]) => {
    if (!open || !docType || !docId) return;
    isLoaded.value = false;
    await fetchTemplate();
  }
);
</script>

<style scoped>
.print-preview-dialog :deep(.el-dialog) {
  max-width: 1300px;
}

.print-preview-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}

.print-preview-body {
  height: 78vh;
  border: 1px solid #e3e6ec;
  border-radius: 8px;
  overflow: hidden;
  background: #f6f7f9;
}

.print-preview-frame {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
