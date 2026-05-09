<template>
  <el-popover placement="bottom-end" trigger="click" width="220">
    <template #reference>
      <el-button>{{ label }}</el-button>
    </template>
    <div class="settings-panel">
    <div class="settings-actions">
      <el-button text size="small" @click="reset">{{ resetLabel }}</el-button>
    </div>
    <el-checkbox-group v-model="localValue">
      <el-checkbox
        v-for="column in columns"
        :key="column.key"
        :value="column.key"
      >
        {{ column.label }}
      </el-checkbox>
    </el-checkbox-group>
  </div>
  </el-popover>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type ColumnOption = {
  key: string
  label: string
}

const props = defineProps<{
  columns: ColumnOption[]
  modelValue: string[]
  label?: string
  resetLabel?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string[]): void
  (e: 'reset'): void
  (e: 'change'): void
}>()

const localValue = computed({
  get: () => props.modelValue,
  set: (value: string[]) => {
    emit('update:modelValue', value)
    emit('change')
  }
})

const label = computed(() => props.label || 'Columns')
const resetLabel = computed(() => props.resetLabel || 'Reset')

const reset = () => {
  emit('reset')
}
</script>

<style scoped>
.settings-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.settings-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
