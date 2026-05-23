<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ t('page.systemConfigManagement') }}</div>
    </div>

    <div class="table-card system-config-page">
      <section class="system-config-hero">
        <div>
          <p class="system-config-hero__eyebrow">{{ t('page.systemConfigManagement') }}</p>
          <h1 class="system-config-hero__title">平台级系统配置</h1>
          <p class="system-config-hero__description">
            这里仅维护平台安全、审计和治理基线。ERP 编码规则、单号规则以及默认分页大小已经迁移到租户设置。
          </p>
        </div>
        <div class="system-config-hero__actions">
          <el-button @click="goTenantSettings">前往租户设置</el-button>
        </div>
      </section>

      <section class="system-config-summary">
        <article class="system-config-summary__card">
          <span class="system-config-summary__label">当前范围</span>
          <strong class="system-config-summary__value">平台安全与审计</strong>
          <p class="system-config-summary__hint">登录安全、密码策略、审计保留等统一基线。</p>
        </article>
        <article class="system-config-summary__card">
          <span class="system-config-summary__label">已迁出内容</span>
          <strong class="system-config-summary__value">租户展示默认与业务规则</strong>
          <p class="system-config-summary__hint">默认分页大小、ERP 编码规则、单号规则请在租户设置维护。</p>
        </article>
      </section>

      <section class="system-config-groups">
        <article v-for="group in groupedConfigSections" :key="group.key" class="system-config-group">
          <header class="system-config-group__header">
            <div>
              <h2 class="system-config-group__title">{{ group.title }}</h2>
              <p class="system-config-group__subtitle">{{ group.description }}</p>
            </div>
          </header>

          <div class="system-config-group__items">
            <div v-for="item in group.items" :key="item.id" class="system-config-group__item">
              <div class="system-config-group__meta">
                <span class="system-config-group__label">{{ item.description || item.key }}</span>
                <span class="system-config-group__key">{{ item.key }}</span>
              </div>
              <div class="system-config-group__value-block">
                <strong class="system-config-group__value">{{ formatConfigValue(item) }}</strong>
                <span class="system-config-group__type">{{ item.valueType }}</span>
              </div>
              <div class="system-config-group__footer">
                <span class="system-config-group__time">{{ formatTime(item.updatedAt) }}</span>
                <el-button link type="primary" size="small" @click="openEdit(item)">
                  {{ t('action.edit') }}
                </el-button>
              </div>
            </div>
          </div>
        </article>
      </section>

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
          <el-select v-if="!isEditing" v-model="form.key" style="width: 100%" @change="applySelectedDefinition">
            <el-option
              v-for="option in createConfigOptions"
              :key="option.key"
              :label="option.label"
              :value="option.key"
            />
          </el-select>
          <el-input v-else v-model="form.key" disabled />
        </el-form-item>
        <el-form-item :label="t('field.value')" required>
          <el-input-number
            v-if="form.valueType === 'int'"
            v-model="form.numberValue"
            :min="0"
            :controls-position="'right'"
            style="width: 100%"
          />
          <el-select v-else-if="form.valueType === 'bool'" v-model="form.value" style="width: 100%">
            <el-option label="true" value="true" />
            <el-option label="false" value="false" />
          </el-select>
          <el-input v-else v-model="form.value" />
        </el-form-item>
        <el-form-item :label="t('field.type')" required>
          <el-select v-model="form.valueType" :disabled="Boolean(selectedDefinition)">
            <el-option label="string" value="string" />
            <el-option label="int" value="int" />
            <el-option label="bool" value="bool" />
            <el-option label="json" value="json" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('field.description')">
          <el-input v-model="form.description" :disabled="Boolean(selectedDefinition)" />
        </el-form-item>
        <el-form-item :label="t('field.public')">
          <el-switch v-model="form.isPublic" :disabled="Boolean(selectedDefinition)" />
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
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
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

type SystemConfigGroup = {
  key: string
  title: string
  description: string
  matchKeys: string[]
}

type PlatformConfigDefinition = {
  key: string
  label: string
  description: string
  valueType: string
  isPublic: boolean
}

const { t } = useI18n()
const { notifyError, notifySuccess } = useApiError()
const authStore = useAuthStore()
const router = useRouter()

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

const PLATFORM_GROUPS: SystemConfigGroup[] = [
  {
    key: 'audit',
    title: '审计策略',
    description: '统一控制日志保留和平台审计基线。',
    matchKeys: ['audit.retention.days']
  },
  {
    key: 'login',
    title: '登录安全',
    description: '控制登录失败次数和账号保护阈值。',
    matchKeys: ['login.max.retry']
  },
  {
    key: 'password',
    title: '密码安全',
    description: '统一约束密码复杂度和最小长度要求。',
    matchKeys: ['password.min.length']
  }
]

const PLATFORM_CONFIG_DEFINITIONS: PlatformConfigDefinition[] = [
  {
    key: 'audit.retention.days',
    label: '审计日志保留天数',
    description: '审计日志保留天数',
    valueType: 'int',
    isPublic: false
  },
  {
    key: 'login.max.retry',
    label: '登录失败最大次数',
    description: '登录失败最大次数',
    valueType: 'int',
    isPublic: false
  },
  {
    key: 'password.min.length',
    label: '密码最小长度',
    description: '密码最小长度',
    valueType: 'int',
    isPublic: false
  }
]


const filteredItems = computed(() => {
  if (!keyword.value) return items.value
  const query = keyword.value.toLowerCase()
  return items.value.filter((item) =>
    `${item.key} ${item.description || ''}`.toLowerCase().includes(query)
  )
})

const groupedConfigSections = computed(() => PLATFORM_GROUPS.map((group) => ({
  ...group,
  items: filteredItems.value.filter((item) => group.matchKeys.includes(item.key))
})).filter((group) => group.items.length > 0))

const showDialog = ref(false)
const isEditing = ref(false)
const form = reactive({
  key: '',
  value: '',
  numberValue: undefined as number | undefined,
  valueType: 'string',
  description: '',
  isPublic: false
})

const selectedDefinition = computed(() => PLATFORM_CONFIG_DEFINITIONS.find((item) => item.key === form.key) || null)
const createConfigOptions = computed(() => PLATFORM_CONFIG_DEFINITIONS.map((item) => ({
  key: item.key,
  label: `${item.label} (${item.key})`
})))

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
  const firstDefinition = PLATFORM_CONFIG_DEFINITIONS[0]
  form.key = firstDefinition?.key || ''
  form.value = ''
  form.numberValue = undefined
  form.valueType = firstDefinition?.valueType || 'string'
  form.description = firstDefinition?.description || ''
  form.isPublic = firstDefinition?.isPublic || false
  showDialog.value = true
}

const goTenantSettings = () => {
  router.push('/tenant-settings')
}

const openEdit = (row: SystemConfigItem) => {
  isEditing.value = true
  form.key = row.key
  form.value = row.value
  form.numberValue = row.valueType === 'int' ? Number(row.value) : undefined
  form.valueType = row.valueType
  form.description = row.description || ''
  form.isPublic = row.isPublic
  showDialog.value = true
}

const applySelectedDefinition = () => {
  const definition = selectedDefinition.value
  if (!definition || isEditing.value) {
    return
  }
  form.valueType = definition.valueType
  form.description = definition.description
  form.isPublic = definition.isPublic
  form.value = ''
  form.numberValue = undefined
}

const saveConfig = async () => {
  try {
    const normalizedValue = form.valueType === 'int'
      ? (form.numberValue == null ? '' : String(form.numberValue))
      : form.value
    const payload = {
      value: normalizedValue,
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

const formatConfigValue = (item: SystemConfigItem) => {
  if (item.valueType === 'bool') {
    return item.value === 'true' ? t('status.active') : t('status.inactive')
  }
  return item.value || '-'
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
.system-config-page {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
}

.system-config-hero {
  padding: 24px 28px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.14), transparent 36%),
    linear-gradient(135deg, #ffffff 0%, #f8fafc 48%, #eef7ff 100%);
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.06);
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
}

.system-config-hero__eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #0284c7;
}

.system-config-hero__title {
  margin: 0;
  font-size: 28px;
  line-height: 1.15;
  font-weight: 700;
  color: #0f172a;
}

.system-config-hero__description {
  margin: 12px 0 0;
  max-width: 780px;
  font-size: 14px;
  line-height: 1.8;
  color: #475569;
}

.system-config-hero__actions {
  display: flex;
  align-items: center;
}

.system-config-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.system-config-groups {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.system-config-group {
  padding: 20px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.94), rgba(255, 255, 255, 0.98));
  border: 1px solid rgba(226, 232, 240, 0.92);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
}

.system-config-group__header {
  margin-bottom: 16px;
}

.system-config-group__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.system-config-group__subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.system-config-group__items {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 14px;
}

.system-config-group__item {
  padding: 18px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.system-config-group__meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.system-config-group__label {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.system-config-group__key {
  font-size: 12px;
  color: #64748b;
}

.system-config-group__value-block {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.system-config-group__value {
  font-size: 24px;
  color: #0284c7;
}

.system-config-group__type {
  font-size: 12px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.system-config-group__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.system-config-group__time {
  font-size: 12px;
  color: #94a3b8;
}

.system-config-summary__card {
  padding: 18px 20px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 0.98));
  border: 1px solid rgba(226, 232, 240, 0.92);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
}

.system-config-summary__label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.system-config-summary__value {
  display: block;
  font-size: 18px;
  color: #0f172a;
}

.system-config-summary__hint {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.system-config-toolbar {
  width: 100%;
  padding: 16px 18px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
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
  .system-config-page {
    padding: 20px;
  }

  .system-config-hero {
    padding: 20px;
    flex-direction: column;
  }

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
  .system-config-page {
    padding: 16px;
  }

  .system-config-hero__title {
    font-size: 24px;
  }

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
