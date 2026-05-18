<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ t('page.systemConfigManagement') }}</div>
      <div class="system-config-toolbar">
        <div class="table-toolbar">
          <div class="table-filters">
            <el-input
              v-model="keyword"
              class="table-search system-config-toolbar__search--wide"
              :placeholder="t('placeholder.keyword')"
              clearable
              @keyup.enter="fetchConfigs"
            />
          </div>
          <div class="table-actions">
            <el-button @click="exportConfigs">{{ t('action.export') }}</el-button>
            <el-button type="primary" @click="openCreate">{{ t('action.add') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body" v-loading="loading">
        <ErpDataTable :data="filteredItems" table-key="system-config-management">
          <ErpDataTableColumn v-if="canShow('key')" prop="key" :label="t('field.code')" min-width="180" />
          <ErpDataTableColumn v-if="canShow('value')" prop="value" :label="t('field.value')" min-width="200" />
          <ErpDataTableColumn v-if="canShow('type')" prop="valueType" :label="t('field.type')" min-width="120" />
          <ErpDataTableColumn v-if="canShow('description')" prop="description" :label="t('field.description')" min-width="220" />
          <ErpDataTableColumn v-if="canShow('public')" prop="isPublic" :label="t('field.public')" min-width="120">
            <template #default="{ row }">
              {{ row.isPublic ? t('status.active') : t('status.inactive') }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('updatedAt')" prop="updatedAt" :label="t('field.updatedTime')" min-width="180">
            <template #default="{ row }">
              {{ formatTime(row.updatedAt) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="t('table.actions')" width="140" fixed="right" column-key="actions">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openEdit(row)">
                {{ t('action.edit') }}
              </el-button>
            </template>
          </ErpDataTableColumn>
          <template #empty>
            <div class="table-empty">{{ t('table.empty') }}</div>
          </template>
        </ErpDataTable>
      </div>
    </div>

    <el-dialog v-model="showDialog" :title="dialogTitle" width="520px">
      <el-form :model="form" label-width="120px">
        <el-form-item :label="t('field.code')" required>
          <el-input v-model="form.key" :disabled="isEditing" />
        </el-form-item>
        <el-form-item :label="t('field.value')" required>
          <el-input v-model="form.value" />
        </el-form-item>
        <el-form-item :label="t('field.type')" required>
          <el-select v-model="form.valueType">
            <el-option label="string" value="string" />
            <el-option label="int" value="int" />
            <el-option label="bool" value="bool" />
            <el-option label="json" value="json" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('field.description')">
          <el-input v-model="form.description" />
        </el-form-item>
        <el-form-item :label="t('field.public')">
          <el-switch v-model="form.isPublic" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">{{ t('action.cancel') }}</el-button>
        <el-button type="primary" @click="saveConfig">{{ t('action.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '@/utils/request'
import { useApiError } from '@/composables/useApiError'
import { useAuthStore } from '@/stores/auth'
import { useColumnSettings } from '@/composables/useColumnSettings'
import { exportToCsv } from '@/utils/csv'

type SystemConfigItem = {
  id: number
  key: string
  value: string
  valueType: string
  description?: string
  isPublic: boolean
  updatedAt?: string
}

const { t } = useI18n()
const { notifyError, notifySuccess } = useApiError()
const authStore = useAuthStore()

const loading = ref(false)
const keyword = ref('')
const items = ref<SystemConfigItem[]>([])
const defaultColumns = ['key', 'value', 'type', 'description', 'public', 'updatedAt']
const { isVisible, fetchTenantKeys } = useColumnSettings('system-configs', defaultColumns)
const columnPermissionMap: Record<string, string> = {
  key: 'column:system-configs:key',
  value: 'column:system-configs:value',
  type: 'column:system-configs:type',
  description: 'column:system-configs:description',
  public: 'column:system-configs:public',
  updatedAt: 'column:system-configs:updatedAt'
}


const filteredItems = computed(() => {
  if (!keyword.value) return items.value
  const query = keyword.value.toLowerCase()
  return items.value.filter((item) =>
    `${item.key} ${item.description || ''}`.toLowerCase().includes(query)
  )
})

const showDialog = ref(false)
const isEditing = ref(false)
const form = reactive({
  key: '',
  value: '',
  valueType: 'string',
  description: '',
  isPublic: false
})

const dialogTitle = computed(() =>
  isEditing.value ? t('action.edit') : t('action.add')
)

const fetchConfigs = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/system-configs')
    items.value = res.data.data || []
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  isEditing.value = false
  form.key = ''
  form.value = ''
  form.valueType = 'string'
  form.description = ''
  form.isPublic = false
  showDialog.value = true
}

const openEdit = (row: SystemConfigItem) => {
  isEditing.value = true
  form.key = row.key
  form.value = row.value
  form.valueType = row.valueType
  form.description = row.description || ''
  form.isPublic = row.isPublic
  showDialog.value = true
}

const saveConfig = async () => {
  try {
    const payload = {
      value: form.value,
      valueType: form.valueType,
      description: form.description,
      isPublic: form.isPublic
    }
    if (isEditing.value) {
      await request.put(`/system-configs/${form.key}`, payload)
    } else {
      await request.post(`/system-configs/${form.key}`, payload)
    }
    notifySuccess()
    showDialog.value = false
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new Event('system-config:refresh'))
    }
    fetchConfigs()
  } catch (error) {
    notifyError(error)
  }
}

const exportConfigs = () => {
  exportToCsv('system-configs.csv', [
    { key: 'key', label: t('field.code') },
    { key: 'value', label: t('field.value') },
    { key: 'valueType', label: t('field.type') },
    { key: 'description', label: t('field.description') },
    { key: 'isPublic', label: t('field.public') },
    { key: 'updatedAt', label: t('field.updatedTime') }
  ], filteredItems.value)
}

const canShow = (key: string) => {
  const permission = columnPermissionMap[key]
  if (permission && !authStore.hasPermission(permission)) {
    return false
  }
  return isVisible(key)
}

const formatTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

fetchConfigs()
fetchTenantKeys()
</script>

<style scoped>
.system-config-toolbar {
  width: 100%;
  padding: 16px 18px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  box-sizing: border-box;
}

.table-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
}

.table-filters {
  display: grid;
  grid-template-columns: 220px;
  align-items: center;
  justify-content: start;
  gap: 12px;
  min-width: 0;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
  justify-content: flex-end;
  margin-left: 0;
}

:deep(.system-config-toolbar__search--wide) {
  width: 220px;
}

.table-card {
  min-height: 0;
}

.table-body {
  flex: 1;
  overflow: auto;
}

@media (max-width: 1280px) {
  .system-config-toolbar {
    padding: 14px;
  }

  .table-toolbar {
    grid-template-columns: minmax(0, 1fr);
  }

  .table-filters {
    grid-template-columns: 200px;
  }

  .table-actions {
    justify-content: flex-start;
  }

  :deep(.system-config-toolbar__search--wide) {
    width: 200px;
  }
}

@media (max-width: 768px) {
  .table-filters {
    grid-template-columns: 1fr;
  }

  .table-actions {
    width: 100%;
    justify-content: flex-end;
  }

  :deep(.system-config-toolbar__search--wide) {
    width: 100% !important;
  }
}
</style>
