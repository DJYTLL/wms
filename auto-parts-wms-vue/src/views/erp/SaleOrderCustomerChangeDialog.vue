<template>
  <el-dialog
    v-model="visible"
    :title="$t('message.confirmCustomerChangeTitle')"
    width="420px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <div>{{ $t('message.confirmCustomerChange') }}</div>
    <template #footer>
      <el-button @click="$emit('apply', 'cancel')">{{ $t('action.cancel') }}</el-button>
      <el-button @click="$emit('apply', 'clear')">{{ $t('action.clearItems') }}</el-button>
      <el-button type="primary" @click="$emit('apply', 'price')">{{ $t('action.updatePrice') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  apply: [value: 'cancel' | 'clear' | 'price'];
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});
</script>
