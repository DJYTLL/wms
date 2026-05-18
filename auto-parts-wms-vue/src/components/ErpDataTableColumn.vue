<script setup lang="ts">
import { computed, inject, onBeforeUnmount, ref, useAttrs, watch } from 'vue'
import { ElTableColumn as ElementTableColumn } from 'element-plus'
import 'element-plus/es/components/table-column/style/css'
import { persistedElementTableKey } from './erpTablePersistence'

defineOptions({
  inheritAttrs: false
})

const attrs = useAttrs()
const tableContext = inject(persistedElementTableKey, null)
const unregister = ref<(() => void) | null>(null)
const nonConfigurableColumnKeys = new Set(['index', 'selection', 'expand', 'actions'])

const toNumber = (value: unknown) => {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : undefined
  }
  return undefined
}

const isFalseLike = (value: unknown) => value === false || value === 'false'

const normalizeFixed = (value: unknown): '' | 'left' | 'right' => {
  if (value === 'left' || value === 'right') {
    return value
  }
  if (value === true || value === '') {
    return 'left'
  }
  return ''
}

const columnKey = computed(() => {
  const rawKey = attrs.columnKey || attrs.prop || attrs.type || attrs.label
  return rawKey === undefined || rawKey === null ? '' : String(rawKey)
})

const columnLabel = computed(() => {
  const rawLabel = attrs.label || attrs.prop || attrs.type || columnKey.value
  return rawLabel === undefined || rawLabel === null ? '' : String(rawLabel)
})

const columnType = computed(() => {
  const rawType = attrs.type
  return rawType === undefined || rawType === null ? '' : String(rawType)
})

const columnConfigurable = computed(() => {
  if (isFalseLike(attrs.configurable)) {
    return false
  }
  return !nonConfigurableColumnKeys.has(columnKey.value) && !nonConfigurableColumnKeys.has(columnType.value)
})

const originalFixed = computed(() => normalizeFixed(attrs.fixed))

const savedWidth = computed(() => (
  tableContext && columnKey.value ? tableContext.resolveSavedWidth(columnKey.value) : undefined
))

const visible = computed(() => {
  if (!tableContext || !columnKey.value) {
    return true
  }
  return tableContext.isColumnVisible(columnKey.value, columnConfigurable.value)
})

const columnAttrs = computed(() => {
  const saved = savedWidth.value
  const originalWidth = toNumber(attrs.width)
  const { configurable, fixed, ...rest } = attrs as Record<string, unknown>
  const nextAttrs: Record<string, unknown> = {
    ...rest,
    columnKey: columnKey.value,
    resizable: !isFalseLike(attrs.resizable)
  }
  if (saved) {
    nextAttrs.width = saved
  } else if (originalWidth) {
    nextAttrs.width = originalWidth
  }
  const resolvedFixed = tableContext && columnKey.value
    ? tableContext.resolveFixed(columnKey.value, originalFixed.value)
    : originalFixed.value
  if (resolvedFixed) {
    nextAttrs.fixed = resolvedFixed
  }
  return nextAttrs
})

watch(
  [columnKey, columnLabel, columnConfigurable, originalFixed],
  ([key, label, configurable, defaultFixed]) => {
    unregister.value?.()
    unregister.value = null
    if (tableContext && key) {
      unregister.value = tableContext.registerColumn({
        key,
        label,
        configurable,
        defaultFixed: defaultFixed || undefined
      })
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  unregister.value?.()
})
</script>

<template>
  <ElementTableColumn v-if="visible" v-bind="columnAttrs">
    <template v-if="$slots.default" #default="scope">
      <slot v-bind="scope" />
    </template>
    <template v-if="$slots.header" #header="scope">
      <slot name="header" v-bind="scope" />
    </template>
  </ElementTableColumn>
</template>
