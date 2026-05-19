<template>
  <div class="erp-data-table-shell" :class="{ 'is-layout-pending': isLayoutPending }">
    <div v-if="shouldReserveColumnTools" class="erp-data-table-tools">
      <el-popover placement="bottom-end" trigger="click" width="360">
        <template #reference>
          <el-button v-show="showColumnSettings" size="small" text class="erp-data-table-tools__button">
            <el-icon><Operation /></el-icon>
            <span>列设置</span>
          </el-button>
        </template>
        <div class="erp-data-table-settings">
          <div class="erp-data-table-settings__header">
            <span>个人列设置</span>
            <el-button text size="small" @click="resetTableSettings">恢复默认</el-button>
          </div>
          <div
            v-for="(column, index) in customizableColumns"
            :key="column.key"
            class="erp-data-table-settings__row"
          >
            <el-checkbox
              :model-value="isColumnVisible(column)"
              @change="(value: string | number | boolean) => handleColumnVisibleChange(column, Boolean(value))"
            >
              {{ column.label }}
            </el-checkbox>
            <div class="erp-data-table-settings__actions">
              <el-button text size="small" :disabled="index === 0" @click="moveColumn(column, -1)">上移</el-button>
              <el-button text size="small" :disabled="index === customizableColumns.length - 1" @click="moveColumn(column, 1)">下移</el-button>
              <el-select
                :model-value="resolveColumnFixed(column)"
                size="small"
                class="erp-data-table-settings__fixed"
                @change="(value: '' | 'left' | 'right') => handleColumnFixedChange(column, value)"
              >
                <el-option label="不固定" value="" />
                <el-option label="左固定" value="left" />
                <el-option label="右固定" value="right" />
              </el-select>
            </div>
          </div>
        </div>
      </el-popover>
    </div>
    <ElementTable
      v-if="usesColumnSlots"
      v-bind="elementTableAttrs"
      class="erp-data-table-element"
      @header-dragend="handleElementHeaderDragend"
    >
      <ColumnSlotRenderer :nodes="orderedColumnSlotNodes" />
      <template v-if="$slots.append" #append>
        <slot name="append" />
      </template>
      <template v-if="$slots.empty" #empty>
        <slot name="empty" />
      </template>
    </ElementTable>
    <div v-else class="erp-data-table-scroll" v-loading="loading">
      <table class="erp-data-table" :style="{ width: `${tableWidth}px` }">
        <colgroup>
          <col
            v-for="column in displayColumns"
            :key="column.key"
            :style="{ width: `${resolveColumnWidth(column)}px` }"
          />
        </colgroup>
        <thead>
          <tr>
            <th
              v-for="(column, index) in displayColumns"
              :key="column.key"
              :class="buildHeaderClass(column)"
              :style="buildStickyStyle(column, index)"
            >
              <div class="erp-data-table__header-content">
                <slot :name="`header-${column.key}`" :column="column">
                  {{ column.label }}
                </slot>
              </div>
              <span
                v-if="column.resizable !== false"
                class="erp-data-table__resize-handle"
                @mousedown.prevent="startResize($event, column)"
              />
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!normalizedRows.length">
            <td class="erp-data-table__empty" :colspan="displayColumns.length">{{ emptyText }}</td>
          </tr>
          <tr
            v-for="(row, index) in normalizedRows"
            :key="resolveRowKey(row, index)"
            :class="rowClassName ? rowClassName({ row, index }) : ''"
          >
            <td
              v-for="(column, columnIndex) in displayColumns"
              :key="column.key"
              :class="buildCellClass(column)"
              :style="buildStickyStyle(column, columnIndex)"
            >
              <slot :name="`cell-${column.key}`" :row="row" :index="index" :column="column">
                {{ column.prop ? row[column.prop] : '' }}
              </slot>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts" generic="T extends Record<string, any>">
import { computed, defineComponent, nextTick, onBeforeUnmount, provide, ref, useAttrs, useSlots, watch, type PropType, type VNode } from 'vue'
import { ElTable as ElementTable } from 'element-plus'
import 'element-plus/es/components/table/style/css'
import { useUserTableSettings } from '@/composables/useUserTableSettings'
import { Operation } from '@element-plus/icons-vue'
import { persistedElementTableKey, type PersistedElementTableColumn } from './erpTablePersistence'

export type ErpDataTableColumn = {
  key: string
  label: string
  prop?: string
  width?: number
  minWidth?: number
  className?: string
  headerClassName?: string
  stickyRight?: boolean
  stickyLeft?: boolean
  nowrap?: boolean
  resizable?: boolean
  configurable?: boolean
}

defineOptions({
  inheritAttrs: false
})

const ColumnSlotRenderer = defineComponent({
  name: 'ColumnSlotRenderer',
  props: {
    nodes: {
      type: Array as PropType<VNode[]>,
      required: true
    }
  },
  setup(rendererProps) {
    return () => rendererProps.nodes
  }
})

const props = withDefaults(defineProps<{
  rows?: T[]
  data?: T[]
  columns?: ErpDataTableColumn[]
  tableKey?: string
  rowKey?: string | ((row: T, index: number) => string | number)
  loading?: boolean
  emptyText?: string
  rowClassName?: (scope: { row: T; index: number }) => string
}>(), {
  rowKey: 'id',
  emptyText: '',
  loading: false
})

const attrs = useAttrs()
const slots = useSlots()
const effectiveTableKey = computed(() => props.tableKey || '')
const {
  fetchConfig,
  loaded: tableSettingsLoaded,
  resetConfig,
  getColumnLayout,
  getColumnWidth,
  setColumnWidth,
  setColumnVisible,
  setColumnFixed,
  setColumnOrder
} = useUserTableSettings(effectiveTableKey)
const minColumnWidth = 48
const draftWidths = ref<Record<string, number>>({})
const registeredColumns = ref<Array<PersistedElementTableColumn & { uid: symbol }>>([])
const fetchTimer = ref<number | null>(null)
const resizeState = ref<{
  key: string
  startX: number
  startWidth: number
  minWidth: number
} | null>(null)
const normalizedRows = computed(() => props.rows || props.data || [])
const normalizedColumns = computed(() => props.columns || [])
const usesColumnSlots = computed(() => normalizedColumns.value.length === 0 && Boolean(slots.default))
const nonConfigurableColumnKeys = new Set(['index', 'selection', 'expand', 'actions'])

const resolveColumnWidth = (column: ErpDataTableColumn) => {
  const fallback = column.width || column.minWidth || 120
  return draftWidths.value[column.key] || (effectiveTableKey.value ? getColumnWidth(column.key, fallback) : fallback)
}

const resolveColumnFixed = (column: ErpDataTableColumn) => {
  const fixed = getColumnLayout(column.key).fixed
  if (fixed === false) {
    return ''
  }
  if (fixed === 'left' || fixed === 'right') {
    return fixed
  }
  if (column.stickyLeft) return 'left'
  if (column.stickyRight) return 'right'
  return ''
}

const isConfigurable = (column: ErpDataTableColumn) => (
  column.configurable !== false && !nonConfigurableColumnKeys.has(column.key)
)

const isColumnVisible = (column: ErpDataTableColumn) => {
  if (!isConfigurable(column)) {
    return true
  }
  return getColumnLayout(column.key).visible !== false
}

const registeredSettingColumns = computed<ErpDataTableColumn[]>(() => {
  const seen = new Set<string>()
  return registeredColumns.value.reduce<ErpDataTableColumn[]>((result, column) => {
    if (!column.key || seen.has(column.key)) {
      return result
    }
    seen.add(column.key)
    result.push({
      key: column.key,
      label: column.label,
      configurable: column.configurable,
      stickyLeft: column.defaultFixed === 'left',
      stickyRight: column.defaultFixed === 'right'
    })
    return result
  }, [])
})

const settingColumns = computed(() => (
  usesColumnSlots.value ? registeredSettingColumns.value : normalizedColumns.value
))

const orderedColumns = computed(() => {
  return settingColumns.value
    .map((column, index) => ({
      column,
      index,
      order: typeof getColumnLayout(column.key).order === 'number'
        ? Number(getColumnLayout(column.key).order)
        : index
    }))
    .sort((left, right) => {
      if (!isConfigurable(left.column) || !isConfigurable(right.column)) {
        return left.index - right.index
      }
      return left.order - right.order || left.index - right.index
    })
    .map((item) => item.column)
})

const displayColumns = computed(() => {
  const ordered = orderedColumns.value.filter(isColumnVisible)

  const left = ordered.filter((column) => resolveColumnFixed(column) === 'left')
  const middle = ordered.filter((column) => !resolveColumnFixed(column))
  const right = ordered.filter((column) => resolveColumnFixed(column) === 'right')
  return [...left, ...middle, ...right]
})

const customizableColumns = computed(() => orderedColumns.value.filter(isConfigurable))
const showColumnSettings = computed(() => Boolean(effectiveTableKey.value) && customizableColumns.value.length > 0)
const shouldReserveColumnTools = computed(() => Boolean(effectiveTableKey.value))
const isLayoutPending = computed(() => Boolean(effectiveTableKey.value) && !tableSettingsLoaded.value)

const orderedColumnIndex = computed(() => new Map(
  orderedColumns.value.map((column, index) => [column.key, index])
))

const resolveVNodeColumnKey = (node: VNode) => {
  const nodeProps = (node.props || {}) as Record<string, unknown>
  const rawKey = nodeProps.columnKey || nodeProps['column-key'] || nodeProps.prop || nodeProps.type || nodeProps.label
  return rawKey === undefined || rawKey === null ? '' : String(rawKey)
}

const orderedColumnSlotNodes = computed(() => {
  const nodes = slots.default?.() || []
  const orderMap = orderedColumnIndex.value
  return [...nodes]
    .map((node, index) => {
      const key = resolveVNodeColumnKey(node)
      return {
        node,
        index,
        order: key && orderMap.has(key) ? orderMap.get(key)! : index
      }
    })
    .sort((left, right) => left.order - right.order || left.index - right.index)
    .map((item) => item.node)
})

const tableWidth = computed(() => displayColumns.value.reduce((total, column) => (
  total + resolveColumnWidth(column)
), 0))

const elementRowKey = computed(() => {
  const rowKey = props.rowKey
  if (typeof rowKey !== 'function') {
    return rowKey
  }
  return (row: T) => String(rowKey(row, normalizedRows.value.indexOf(row)))
})

const elementRowClassName = computed(() => {
  const rowClassName = props.rowClassName
  if (!rowClassName) {
    return undefined
  }
  return ({ row, rowIndex }: { row: T; rowIndex: number }) => rowClassName({
    row,
    index: rowIndex
  }) || ''
})

const elementTableAttrs = computed(() => {
  const { onHeaderDragend, tableKey, ...rest } = attrs
  const hasExplicitBorder = Object.prototype.hasOwnProperty.call(rest, 'border')
  const border = hasExplicitBorder
    ? rest.border !== false && rest.border !== 'false'
    : true
  return {
    ...rest,
    border,
    data: normalizedRows.value,
    rowKey: elementRowKey.value,
    emptyText: props.emptyText,
    rowClassName: elementRowClassName.value
  }
})

const registerColumn = (column: PersistedElementTableColumn) => {
  const uid = Symbol(column.key)
  registeredColumns.value = [...registeredColumns.value, { ...column, uid }]
  return () => {
    registeredColumns.value = registeredColumns.value.filter((item) => item.uid !== uid)
  }
}

const resolveSavedElementWidth = (columnKey: string) => {
  const width = getColumnLayout(columnKey).width
  return typeof width === 'number' && Number.isFinite(width) && width > 0 ? width : undefined
}

const saveElementWidth = async (columnKey: string, width: number) => {
  if (!effectiveTableKey.value || !Number.isFinite(width) || width <= 0) {
    return
  }
  await setColumnWidth(columnKey, Math.round(width))
}

const resolveElementFixed = (columnKey: string, fallback: '' | 'left' | 'right' = '') => {
  const fixed = getColumnLayout(columnKey).fixed
  if (fixed === false) {
    return ''
  }
  if (fixed === 'left' || fixed === 'right') {
    return fixed
  }
  return fallback
}

const isElementColumnVisible = (columnKey: string, configurable = true) => {
  if (!configurable || nonConfigurableColumnKeys.has(columnKey)) {
    return true
  }
  return getColumnLayout(columnKey).visible !== false
}

provide(persistedElementTableKey, {
  tableKey: effectiveTableKey,
  registerColumn,
  resolveSavedWidth: resolveSavedElementWidth,
  resolveFixed: resolveElementFixed,
  isColumnVisible: isElementColumnVisible,
  saveWidth: saveElementWidth
})

const resolveDraggedColumnKey = (column: Record<string, unknown>) => {
  const rawKey = column.rawColumnKey || column.columnKey || column.property || column.type || column.id
  return rawKey === undefined || rawKey === null ? '' : String(rawKey)
}

const handleElementHeaderDragend = async (
  newWidth: number,
  oldWidth: number,
  column: Record<string, unknown>,
  event: MouseEvent
) => {
  const columnKey = resolveDraggedColumnKey(column)
  if (columnKey) {
    await saveElementWidth(columnKey, newWidth)
  }
  const handler = attrs.onHeaderDragend
  if (typeof handler === 'function') {
    handler(newWidth, oldWidth, column, event)
  }
}

const scheduleFetchConfig = () => {
  if (!effectiveTableKey.value) {
    tableSettingsLoaded.value = true
    return
  }
  if (fetchTimer.value !== null) {
    window.clearTimeout(fetchTimer.value)
  }
  fetchTimer.value = window.setTimeout(async () => {
    fetchTimer.value = null
    await nextTick()
    await fetchConfig()
  }, 80)
}

const resolveRowKey = (row: T, index: number) => {
  if (typeof props.rowKey === 'function') {
    return props.rowKey(row, index)
  }
  return row[props.rowKey] ?? index
}

const buildHeaderClass = (column: ErpDataTableColumn) => [
  column.headerClassName,
  {
    'erp-data-table__cell--sticky': Boolean(resolveColumnFixed(column)),
    'erp-data-table__cell--resizable': column.resizable !== false
  }
]

const buildCellClass = (column: ErpDataTableColumn) => [
  column.className,
  {
    'erp-data-table__cell--sticky': Boolean(resolveColumnFixed(column)),
    'erp-data-table__nowrap': column.nowrap || Boolean(resolveColumnFixed(column))
  }
]

const buildStickyStyle = (column: ErpDataTableColumn, index: number) => {
  const fixed = resolveColumnFixed(column)
  if (!fixed) {
    return {}
  }
  if (fixed === 'left') {
    const left = displayColumns.value.slice(0, index).reduce((total, item) => {
      return resolveColumnFixed(item) === 'left' ? total + resolveColumnWidth(item) : total
    }, 0)
    return { left: `${left}px` }
  }
  const right = displayColumns.value.slice(index + 1).reduce((total, item) => {
    return resolveColumnFixed(item) === 'right' ? total + resolveColumnWidth(item) : total
  }, 0)
  return { right: `${right}px` }
}

const stopResize = async () => {
  if (!resizeState.value) {
    return
  }
  const current = resizeState.value
  resizeState.value = null
  document.body.classList.remove('erp-data-table-resizing')
  window.removeEventListener('mousemove', handleResizeMove)
  window.removeEventListener('mouseup', stopResize)
  const nextWidth = draftWidths.value[current.key]
  if (effectiveTableKey.value && nextWidth) {
    await setColumnWidth(current.key, nextWidth)
  }
}

const handleResizeMove = (event: MouseEvent) => {
  if (!resizeState.value) {
    return
  }
  const delta = event.clientX - resizeState.value.startX
  const width = Math.max(resizeState.value.minWidth, resizeState.value.startWidth + delta)
  draftWidths.value = {
    ...draftWidths.value,
    [resizeState.value.key]: Math.round(width)
  }
}

const startResize = (event: MouseEvent, column: ErpDataTableColumn) => {
  const startWidth = resolveColumnWidth(column)
  resizeState.value = {
    key: column.key,
    startX: event.clientX,
    startWidth,
    minWidth: column.minWidth || minColumnWidth
  }
  document.body.classList.add('erp-data-table-resizing')
  window.addEventListener('mousemove', handleResizeMove)
  window.addEventListener('mouseup', stopResize)
}

const normalizeOrders = async (columns: ErpDataTableColumn[]) => {
  const orders = columns.reduce<Record<string, number>>((result, column, index) => {
    if (isConfigurable(column)) {
      result[column.key] = index
    }
    return result
  }, {})
  await setColumnOrder(orders)
}

const moveColumn = async (column: ErpDataTableColumn, offset: number) => {
  const columns = customizableColumns.value
  const currentIndex = columns.findIndex((item) => item.key === column.key)
  const targetIndex = currentIndex + offset
  if (currentIndex < 0 || targetIndex < 0 || targetIndex >= columns.length) {
    return
  }
  const next = [...columns]
  const [current] = next.splice(currentIndex, 1)
  if (!current) {
    return
  }
  next.splice(targetIndex, 0, current)
  await normalizeOrders(next)
}

const handleColumnVisibleChange = async (column: ErpDataTableColumn, visible: boolean) => {
  await setColumnVisible(column.key, visible)
}

const handleColumnFixedChange = async (column: ErpDataTableColumn, value: '' | 'left' | 'right') => {
  await setColumnFixed(column.key, value || false)
}

const resetTableSettings = async () => {
  draftWidths.value = {}
  await resetConfig()
}

watch(
  effectiveTableKey,
  (key) => {
    draftWidths.value = {}
    if (key) {
      scheduleFetchConfig()
    }
  },
  { immediate: true }
)

watch(
  () => registeredColumns.value
    .map((column) => `${column.key}:${column.label}:${column.configurable}:${column.defaultFixed}`)
    .join('|'),
  () => {
    if (usesColumnSlots.value) {
      scheduleFetchConfig()
    }
  }
)

onBeforeUnmount(() => {
  if (fetchTimer.value !== null) {
    window.clearTimeout(fetchTimer.value)
  }
  document.body.classList.remove('erp-data-table-resizing')
  window.removeEventListener('mousemove', handleResizeMove)
  window.removeEventListener('mouseup', stopResize)
})
</script>

<style scoped>
.erp-data-table-scroll {
  width: 100%;
  height: 100%;
  overflow: auto;
}

.erp-data-table-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.erp-data-table-tools {
  display: flex;
  justify-content: flex-end;
  min-height: 37px;
  padding: 4px 6px 6px;
  border-bottom: 1px solid #eef2f7;
  box-sizing: border-box;
}

.erp-data-table-tools__button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #606266;
}

.erp-data-table-shell.is-layout-pending .erp-data-table-element,
.erp-data-table-shell.is-layout-pending .erp-data-table-scroll {
  visibility: hidden;
}

.erp-data-table-settings {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 360px;
  overflow: auto;
}

.erp-data-table-settings__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid #eef2f7;
  color: #606266;
  font-size: 13px;
}

.erp-data-table-settings__row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
}

.erp-data-table-settings__actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.erp-data-table-settings__fixed {
  width: 92px;
}

.erp-data-table {
  border-collapse: collapse;
  table-layout: fixed;
  color: #374151;
  font-size: 14px;
}

.erp-data-table th,
.erp-data-table td {
  height: 48px;
  padding: 8px 12px;
  border-bottom: 1px solid #ebeef5;
  text-align: left;
  vertical-align: middle;
  box-sizing: border-box;
}

.erp-data-table th {
  position: sticky;
  top: 0;
  z-index: 2;
  color: #6b7280;
  font-weight: 600;
  background: #ffffff;
  white-space: nowrap;
}

.erp-data-table__header-content {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.erp-data-table__resize-handle {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 10px;
  cursor: col-resize;
  z-index: 4;
}

.erp-data-table__resize-handle::after {
  content: '';
  position: absolute;
  top: 8px;
  right: 4px;
  width: 1px;
  height: calc(100% - 16px);
  background: #d5dbe5;
  opacity: 0.75;
  transition: opacity 0.15s ease, background 0.15s ease;
}

.erp-data-table__cell--resizable:hover .erp-data-table__resize-handle::after {
  opacity: 1;
}

.erp-data-table__resize-handle:hover::after {
  width: 2px;
  background: #409eff;
  opacity: 1;
}

.erp-data-table__cell--sticky {
  position: sticky;
  z-index: 1;
  background: #ffffff;
  box-shadow: -1px 0 0 #ebeef5;
}

th.erp-data-table__cell--sticky {
  z-index: 5;
}

.erp-data-table tbody tr:nth-child(even) td {
  background: #fafafa;
}

.erp-data-table tbody tr:nth-child(even) .erp-data-table__cell--sticky {
  background: #fafafa;
}

.erp-data-table tbody tr:hover td {
  background: #f5f7fa;
}

.erp-data-table tbody tr:hover .erp-data-table__cell--sticky {
  background: #f5f7fa;
}

.erp-data-table__nowrap {
  white-space: nowrap;
}

.erp-data-table__empty {
  height: 96px;
  color: #909399;
  text-align: center !important;
}
</style>

<style>
.erp-data-table-resizing {
  cursor: col-resize !important;
  user-select: none;
}
</style>
