<template>
  <el-dialog
    v-model="errorVisible"
    :title="$t('message.saveFailed')"
    width="420px"
    append-to-body
  >
    <div class="save-error-dialog__content">{{ errorMessage }}</div>
    <template #footer>
      <el-button type="primary" @click="errorVisible = false">{{ $t('action.confirm') }}</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="successVisible"
    class="save-success-dialog"
    width="520px"
    append-to-body
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    @closed="$emit('successClosed')"
  >
    <div class="save-success-dialog__content">
      <div class="save-success-dialog__header">
        <el-icon class="save-success-dialog__icon"><CircleCheckFilled /></el-icon>
        <span class="save-success-dialog__title">{{ successTitle }}</span>
      </div>
      <div class="save-success-dialog__message">{{ successMessage }}</div>
      <div v-if="successOrderNo" class="save-success-dialog__order-no">
        {{ $t('message.saveSuccessOrderNo', { orderNo: successOrderNo }) }}
      </div>
    </div>
    <template #footer>
      <div class="save-success-dialog__footer">
        <div class="save-success-dialog__actions save-success-dialog__actions--secondary">
          <el-button @click="$emit('continueCreate')">{{ $t('action.continueCreate') }}</el-button>
          <el-button @click="$emit('stayCurrent')">{{ $t('action.stayCurrent') }}</el-button>
          <el-button @click="$emit('backToList')">{{ $t('action.backToList') }}</el-button>
        </div>
        <div class="save-success-dialog__actions save-success-dialog__actions--primary">
          <el-button v-if="mode === 'approve'" type="primary" @click="$emit('printSavedOrder')">{{ $t('action.print') }}</el-button>
          <el-button v-if="mode === 'save' && canPrintSavedOrder" type="primary" @click="$emit('printSavedOrder')">{{ $t('action.print') }}</el-button>
          <el-button v-if="mode === 'save' && canApproveSavedOrder" type="success" @click="$emit('approveSavedOrder')">{{ $t('action.approve') }}</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { CircleCheckFilled } from '@element-plus/icons-vue';

const props = defineProps<{
  errorVisible: boolean;
  errorMessage: string;
  successVisible: boolean;
  successTitle: string;
  successMessage: string;
  successOrderNo: string;
  mode: 'save' | 'approve';
  canPrintSavedOrder: boolean;
  canApproveSavedOrder: boolean;
}>();

const emit = defineEmits<{
  'update:errorVisible': [value: boolean];
  'update:successVisible': [value: boolean];
  successClosed: [];
  continueCreate: [];
  stayCurrent: [];
  backToList: [];
  printSavedOrder: [];
  approveSavedOrder: [];
}>();

const errorVisible = computed({
  get: () => props.errorVisible,
  set: (value) => emit('update:errorVisible', value)
});

const successVisible = computed({
  get: () => props.successVisible,
  set: (value) => emit('update:successVisible', value)
});
</script>

<style scoped>
.save-error-dialog__content {
  line-height: 1.6;
  color: var(--el-text-color-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.save-success-dialog__content {
  display: flex;
  flex-direction: column;
  gap: 14px;
  line-height: 1.6;
}

.save-success-dialog__header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.save-success-dialog__icon {
  color: #67c23a;
  font-size: 22px;
  flex: 0 0 auto;
}

.save-success-dialog__title {
  color: var(--el-text-color-primary);
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.save-success-dialog__message {
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 500;
}

.save-success-dialog__order-no {
  color: var(--el-text-color-regular);
  font-size: 16px;
}

:deep(.save-success-dialog .el-dialog__header) {
  display: none;
}

:deep(.save-success-dialog .el-dialog__body) {
  padding: 20px 24px 18px;
}

:deep(.save-success-dialog .el-dialog__footer) {
  padding: 0 24px 24px;
}

.save-success-dialog__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  width: 100%;
}

.save-success-dialog__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
}

.save-success-dialog__actions--secondary {
  justify-content: flex-start;
  flex: 1 1 auto;
  min-width: 0;
}

.save-success-dialog__actions--primary {
  flex: 0 0 auto;
  margin-left: auto;
  justify-content: flex-end;
}

.save-success-dialog__actions :deep(.el-button) {
  min-width: 0;
  height: 40px;
  padding: 0 16px;
  border-radius: 8px;
  font-weight: 500;
  margin-left: 0;
}

.save-success-dialog__actions--secondary :deep(.el-button) {
  width: 96px;
}

.save-success-dialog__actions--primary :deep(.el-button) {
  width: 92px;
}

@media (max-width: 640px) {
  :deep(.save-success-dialog .el-dialog) {
    width: min(520px, calc(100vw - 24px)) !important;
  }

  .save-success-dialog__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .save-success-dialog__actions {
    justify-content: center;
    flex-wrap: wrap;
  }

  .save-success-dialog__actions--primary {
    margin-left: 0;
  }
}
</style>
