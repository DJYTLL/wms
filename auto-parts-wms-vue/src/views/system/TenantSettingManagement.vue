<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ t('page.tenantSettingManagement') }}</div>
    </div>

    <div class="table-card tenant-setting-page" v-loading="loading">
      <section class="tenant-setting-hero">
        <div>
          <p class="tenant-setting-hero__eyebrow">{{ t('page.tenantSettingManagement') }}</p>
          <h1 class="tenant-setting-hero__title">{{ t('message.tenantSettingsTitle') }}</h1>
          <p class="tenant-setting-hero__description">
            {{ t('message.tenantSettingsLead') }}
          </p>
        </div>
      </section>

      <el-tabs v-model="activeTab" class="tenant-setting-tabs">
        <el-tab-pane :label="t('message.tenantDisplayDefaults')" name="display">
          <section class="tenant-setting-section">
            <div class="tenant-setting-section__header">
              <div>
                <h2 class="tenant-setting-section__title">{{ t('message.tenantDisplayDefaults') }}</h2>
                <p class="tenant-setting-section__subtitle">{{ t('message.tenantDisplayDefaultsHint') }}</p>
              </div>
            </div>

            <div class="tenant-setting-card-grid tenant-setting-card-grid--compact">
              <article class="tenant-setting-card tenant-setting-card--compact">
                <div class="tenant-setting-card__meta">
                  <span class="tenant-setting-card__label">{{ t('field.defaultPageSize') }}</span>
                  <span class="tenant-setting-card__hint">{{ t('message.tenantDefaultPageSizeHint') }}</span>
                </div>
                <DecimalInput
                  v-model="displayPageSizeInput"
                  input-mode="numeric"
                  :scale="0"
                  class="tenant-setting-card__input"
                />
              </article>
            </div>

            <div class="tenant-setting-actions">
              <el-button type="primary" :loading="savingDisplay" @click="saveDisplay">
                {{ t('action.save') }}
              </el-button>
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane :label="t('message.tenantCodeRules')" name="codeRules">
          <section class="tenant-setting-section">
            <div class="tenant-setting-section__header">
              <div>
                <h2 class="tenant-setting-section__title">{{ t('message.tenantCodeRules') }}</h2>
                <p class="tenant-setting-section__subtitle">{{ t('message.tenantCodeRulesHint') }}</p>
              </div>
            </div>

            <div class="tenant-setting-group-stack">
              <section v-for="group in groupedCodeRules" :key="group.title" class="tenant-setting-group">
                <header class="tenant-setting-group__header">
                  <h3 class="tenant-setting-group__title">{{ group.title }}</h3>
                </header>

                <div class="tenant-setting-card-grid">
                  <article v-for="item in group.items" :key="item.key" class="tenant-setting-card">
                    <div class="tenant-setting-card__meta">
                      <span class="tenant-setting-card__label">{{ item.label }}</span>
                      <span class="tenant-setting-card__hint">{{ item.description }}</span>
                    </div>
                    <DecimalInput
                      v-if="item.valueType === 'int'"
                      v-model="item.numberInputValue"
                      input-mode="numeric"
                      :scale="0"
                      class="tenant-setting-card__input"
                    />
                    <el-input
                      v-else
                      v-model="item.value"
                      class="tenant-setting-card__input"
                      :placeholder="item.defaultValue"
                    />
                  </article>
                </div>
              </section>
            </div>

            <div class="tenant-setting-actions">
              <el-button type="primary" :loading="savingBusiness" @click="saveBusiness('codeRules')">
                {{ t('action.save') }}
              </el-button>
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane :label="t('message.tenantOrderRules')" name="orderRules">
          <section class="tenant-setting-section">
            <div class="tenant-setting-section__header">
              <div>
                <h2 class="tenant-setting-section__title">{{ t('message.tenantOrderRules') }}</h2>
                <p class="tenant-setting-section__subtitle">{{ t('message.tenantOrderRulesHint') }}</p>
              </div>
            </div>

            <div class="tenant-setting-group-stack">
              <section v-for="group in groupedOrderRules" :key="group.title" class="tenant-setting-group">
                <header class="tenant-setting-group__header">
                  <h3 class="tenant-setting-group__title">{{ group.title }}</h3>
                </header>

                <div class="tenant-setting-card-grid">
                  <article v-for="item in group.items" :key="item.key" class="tenant-setting-card">
                    <div class="tenant-setting-card__meta">
                      <span class="tenant-setting-card__label">{{ item.label }}</span>
                      <span class="tenant-setting-card__hint">{{ item.description }}</span>
                    </div>
                    <DecimalInput
                      v-if="item.valueType === 'int'"
                      v-model="item.numberInputValue"
                      input-mode="numeric"
                      :scale="0"
                      class="tenant-setting-card__input"
                    />
                    <el-input
                      v-else
                      v-model="item.value"
                      class="tenant-setting-card__input"
                      :placeholder="item.defaultValue"
                    />
                  </article>
                </div>
              </section>
            </div>

            <div class="tenant-setting-actions">
              <el-button type="primary" :loading="savingBusiness" @click="saveBusiness('orderRules')">
                {{ t('action.save') }}
              </el-button>
            </div>
          </section>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DecimalInput from '@/components/DecimalInput.vue'
import request from '@/utils/request'
import { useApiError } from '@/composables/useApiError'
import { usePageSizePreference } from '@/composables/pageSizePreference'
import { sanitizePageSize } from '@/composables/pageSizePreferenceCore'

type TenantBusinessSettingItem = {
  key: string
  label: string
  valueType: string
  value: string
  defaultValue: string
  description: string
  numberValue: number | null
  numberInputValue: string
}

type TenantBusinessSettingsResponse = {
  codeRules?: Array<{
    key: string
    label: string
    valueType: string
    value: string
    defaultValue: string
    description: string
  }>
  orderRules?: Array<{
    key: string
    label: string
    valueType: string
    value: string
    defaultValue: string
    description: string
  }>
}

const activeTab = ref('display')
const loading = ref(false)
const savingDisplay = ref(false)
const savingBusiness = ref(false)
const displayForm = reactive({
  defaultPageSize: 20
})
const displayPageSizeInput = ref('20')
const codeRules = ref<TenantBusinessSettingItem[]>([])
const orderRules = ref<TenantBusinessSettingItem[]>([])

const { t } = useI18n()
const { notifyError, notifySuccess, notifyWarning } = useApiError()
const { fetchTenantDisplaySettings, updateTenantDisplaySettings } = usePageSizePreference()

const normalizeItems = (items: TenantBusinessSettingsResponse['codeRules'] = []) => items.map((item) => ({
  ...item,
  value: item.value ?? item.defaultValue ?? '',
  numberValue: item.valueType === 'int' ? Number(item.value ?? item.defaultValue ?? 0) : null,
  numberInputValue: item.valueType === 'int' ? String(item.value ?? item.defaultValue ?? '') : ''
}))

const CODE_RULE_GROUP_TITLES: Record<string, string> = {
  category: '商品分类',
  'customer-category': '客户类别',
  customer: '客户',
  'delivery-method': '送货方式',
  location: '库位',
  'payment-method': '付款方式',
  'print-template': '打印模板',
  product: '商品',
  'receipt-method': '收款方式',
  'settlement-method': '结算方式',
  supplier: '供应商',
  unit: '单位',
  'vehicle-brand': '车型品牌',
  'vehicle-series': '车型车系',
  'vehicle-model': '车型',
  warehouse: '仓库'
}

const ORDER_RULE_GROUP_TITLES: Record<string, string> = {
  common: '通用规则',
  purchase: '采购单',
  'purchase-return': '采购退货单',
  sale: '销售单',
  'sale-return': '销售退货单',
  receipt: '收款单',
  payment: '付款单',
  'ar-return': '应收退回单',
  'ap-return': '应付退回单',
  'stock-count': '库存调整单',
  'stock-init': '初始库存单',
  'stock-transfer': '库存移库单',
  assembly: '组装单'
}

const buildGroups = (items: TenantBusinessSettingItem[], resolveTitle: (item: TenantBusinessSettingItem) => string) => {
  const grouped = new Map<string, TenantBusinessSettingItem[]>()
  items.forEach((item) => {
    const title = resolveTitle(item)
    const current = grouped.get(title)
    if (current) {
      current.push(item)
      return
    }
    grouped.set(title, [item])
  })
  return Array.from(grouped.entries()).map(([title, groupedItems]) => ({
    title,
    items: groupedItems
  }))
}

const resolveCodeRuleGroupTitle = (item: TenantBusinessSettingItem) => {
  const match = item.key.match(/^erp\.(.+?)\.code\./)
  if (!match) {
    return '其他编码规则'
  }
  const groupKey = match[1] ?? ''
  return CODE_RULE_GROUP_TITLES[groupKey] ?? item.label.replace(/(编码前缀|日期格式|序列长度)$/, '') ?? '其他编码规则'
}

const resolveOrderRuleGroupTitle = (item: TenantBusinessSettingItem) => {
  if (item.key === 'erp.order.no.date-format' || item.key === 'erp.order.no.seq-length') {
    return ORDER_RULE_GROUP_TITLES.common ?? '通用规则'
  }
  const match = item.key.match(/^erp\.order\.no\.(.+?)\.prefix$/)
  if (!match) {
    return '其他单号规则'
  }
  const groupKey = match[1] ?? ''
  return ORDER_RULE_GROUP_TITLES[groupKey] ?? item.label.replace(/前缀$/, '') ?? '其他单号规则'
}

const groupedCodeRules = computed(() => buildGroups(codeRules.value, resolveCodeRuleGroupTitle))
const groupedOrderRules = computed(() => buildGroups(orderRules.value, resolveOrderRuleGroupTitle))

const sanitizeBusinessInt = (value: string): number | null => {
  if (value == null || value.trim() === '') {
    return null
  }
  const parsed = Number(value)
  if (!Number.isInteger(parsed) || parsed < 1 || parsed > 200) {
    return null
  }
  return parsed
}

const fetchSettings = async () => {
  loading.value = true
  try {
    const [display, businessRes] = await Promise.all([
      fetchTenantDisplaySettings(),
      request.get('/tenant-settings/business')
    ])
    displayForm.defaultPageSize = display.defaultPageSize || 20
    displayPageSizeInput.value = String(displayForm.defaultPageSize)
    const business = (businessRes.data.data || {}) as TenantBusinessSettingsResponse
    codeRules.value = normalizeItems(business.codeRules)
    orderRules.value = normalizeItems(business.orderRules)
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}

const saveDisplay = async () => {
  const parsedPageSize = sanitizePageSize(displayPageSizeInput.value)
  if (parsedPageSize == null) {
    notifyWarning(t('message.pageSizeRangeHint'))
    return
  }
  savingDisplay.value = true
  try {
    displayForm.defaultPageSize = parsedPageSize
    const result = await updateTenantDisplaySettings(displayForm.defaultPageSize)
    displayForm.defaultPageSize = result.defaultPageSize || displayForm.defaultPageSize
    displayPageSizeInput.value = String(displayForm.defaultPageSize)
    notifySuccess()
  } catch (error) {
    notifyError(error)
  } finally {
    savingDisplay.value = false
  }
}

const buildPayload = (items: TenantBusinessSettingItem[]) => {
  const values: Record<string, string> = {}
  items.forEach((item) => {
    values[item.key] = item.valueType === 'int'
      ? String(sanitizeBusinessInt(item.numberInputValue) ?? item.defaultValue ?? '1')
      : (item.value || item.defaultValue || '').trim()
  })
  return values
}

const saveBusiness = async (scope: 'codeRules' | 'orderRules') => {
  const targetItems = scope === 'codeRules' ? codeRules.value : orderRules.value
  const invalidIntItem = targetItems.find((item) => item.valueType === 'int' && sanitizeBusinessInt(item.numberInputValue) == null)
  if (invalidIntItem) {
    notifyWarning(`${invalidIntItem.label}必须为 1 到 200 的整数`)
    return
  }
  savingBusiness.value = true
  try {
    const payload = {
      values: buildPayload(targetItems)
    }
    const res: any = await request.put('/tenant-settings/business', payload)
    const business = (res.data.data || {}) as TenantBusinessSettingsResponse
    codeRules.value = normalizeItems(business.codeRules)
    orderRules.value = normalizeItems(business.orderRules)
    notifySuccess()
  } catch (error) {
    notifyError(error)
  } finally {
    savingBusiness.value = false
  }
}

fetchSettings()
</script>

<style scoped>
.tenant-setting-page {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
}

.tenant-setting-hero {
  padding: 24px 28px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.12), transparent 38%),
    linear-gradient(135deg, #ffffff 0%, #f6f9ff 55%, #eef4ff 100%);
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.06);
}

.tenant-setting-hero__eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #2563eb;
}

.tenant-setting-hero__title {
  margin: 0;
  font-size: 28px;
  line-height: 1.15;
  font-weight: 700;
  color: #0f172a;
}

.tenant-setting-hero__description {
  margin: 12px 0 0;
  max-width: 820px;
  font-size: 14px;
  line-height: 1.8;
  color: #475569;
}

.tenant-setting-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.tenant-setting-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.tenant-setting-section__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.tenant-setting-section__subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.tenant-setting-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.tenant-setting-group-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.tenant-setting-group {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(255, 255, 255, 0.98));
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.tenant-setting-group__header {
  margin-bottom: 14px;
}

.tenant-setting-group__title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.tenant-setting-card-grid--compact {
  grid-template-columns: minmax(280px, 360px);
}

.tenant-setting-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.9), rgba(255, 255, 255, 1));
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
}

.tenant-setting-card--compact {
  max-width: 360px;
}

.tenant-setting-card__meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tenant-setting-card__label {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.tenant-setting-card__hint {
  font-size: 12px;
  line-height: 1.7;
  color: #64748b;
}

.tenant-setting-card__input {
  width: 100%;
}

.tenant-setting-card__input :deep(.el-input__wrapper) {
  min-height: 42px;
}

.tenant-setting-actions {
  display: flex;
  justify-content: flex-start;
}

@media (max-width: 768px) {
  .tenant-setting-page {
    padding: 18px;
  }

  .tenant-setting-hero {
    padding: 20px;
  }

  .tenant-setting-hero__title {
    font-size: 24px;
  }

  .tenant-setting-card-grid--compact,
  .tenant-setting-card--compact {
    max-width: none;
  }
}
</style>
