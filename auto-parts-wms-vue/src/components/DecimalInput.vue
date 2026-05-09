<template>
  <el-input
    v-model="localValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :inputmode="inputMode"
    @blur="emit('blur')"
    @focus="emit('focus')"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  modelValue?: string | number;
  scale?: number;
  placeholder?: string;
  disabled?: boolean;
  allowNegative?: boolean;
  inputMode?: 'decimal' | 'numeric' | 'text';
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'blur'): void;
  (e: 'focus'): void;
}>();

const normalizeDecimalInput = (value: string, scale: number, allowNegative = false) => {
  const trimmed = value.trim();
  const hasMinus = allowNegative && trimmed.startsWith('-');
  const cleaned = trimmed.replace(/[^\d.]/g, '');
  if (!cleaned) return hasMinus ? '-' : '';
  const firstDot = cleaned.indexOf('.');
  let integerPart = cleaned;
  let decimalPart = '';
  if (firstDot !== -1) {
    integerPart = cleaned.slice(0, firstDot);
    decimalPart = cleaned.slice(firstDot + 1).replace(/\./g, '');
  }
  if (integerPart === '' && firstDot !== -1) {
    integerPart = '0';
  }
  if (!decimalPart) {
    const normalized = trimmed.endsWith('.') ? `${integerPart}.` : integerPart;
    return hasMinus ? `-${normalized}` : normalized;
  }
  const normalized = `${integerPart}.${decimalPart.slice(0, scale)}`;
  return hasMinus ? `-${normalized}` : normalized;
};

const localValue = computed({
  get: () => (props.modelValue == null ? '' : String(props.modelValue)),
  set: (val: string) => {
    const scale = props.scale == null ? 2 : props.scale;
    emit('update:modelValue', normalizeDecimalInput(val, scale, props.allowNegative));
  }
});

const inputMode = computed(() => props.inputMode ?? 'decimal');
</script>
