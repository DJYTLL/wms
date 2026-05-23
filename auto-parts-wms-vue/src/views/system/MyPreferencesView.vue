<template>
  <div class="page-shell page-shell--system preference-page" v-loading="loading">
    <div class="page-header">
      <div class="page-title">{{ t('page.myPreferences') }}</div>
    </div>

    <div class="table-card preference-surface">
      <section class="preference-hero">
        <div>
          <p class="preference-hero__eyebrow">{{ t('page.myPreferences') }}</p>
          <h1 class="preference-hero__title">{{ t('message.myPreferencesTitle') }}</h1>
          <p class="preference-hero__description">
            {{ t('message.myPreferencesLead') }}
          </p>
        </div>
        <div class="preference-hero__badge-group">
          <span class="preference-hero__badge preference-hero__badge--soft">
            {{ t('message.saveAffectsListsImmediately') }}
          </span>
          <span class="preference-hero__badge" :class="sourceBadgeClass">
            {{ currentSourceLabel }}
          </span>
        </div>
      </section>

      <section class="preference-grid">
        <article class="preference-panel preference-panel--primary">
          <div class="preference-panel__header">
            <div>
              <h2 class="preference-panel__title">{{ t('message.listDisplayPreferences') }}</h2>
              <p class="preference-panel__subtitle">{{ t('message.pageSizeRangeHint') }}</p>
            </div>
          </div>

          <div class="preference-setting">
            <div class="preference-setting__info">
              <div class="preference-setting__label-row">
                <span class="preference-setting__label">{{ t('field.pageSize') }}</span>
                <span class="preference-setting__status">{{ currentSourceLabel }}</span>
              </div>
              <p class="preference-setting__description">
                {{ t('message.myPageSizeHint') }}
              </p>
              <p class="preference-setting__meta">
                {{ t('message.pageSizeExamples') }}
              </p>
            </div>

            <div class="preference-setting__control">
              <DecimalInput
                v-model="pageSizeInput"
                input-mode="numeric"
                :scale="0"
                class="preference-setting__input"
              />
            </div>
          </div>

          <div class="preference-panel__actions">
            <el-button type="primary" :loading="saving" @click="save">
              {{ t('action.save') }}
            </el-button>
            <span class="preference-panel__action-hint">
              {{ t('message.fallbackToTenantDefault') }}
            </span>
          </div>
        </article>

        <article class="preference-panel preference-panel--secondary">
          <div class="preference-panel__header">
            <div>
              <h2 class="preference-panel__title">{{ t('message.preferenceRoadmapTitle') }}</h2>
              <p class="preference-panel__subtitle">{{ t('message.preferenceRoadmapHint') }}</p>
            </div>
          </div>

          <ul class="preference-roadmap">
            <li>{{ t('message.preferenceRoadmapColumns') }}</li>
            <li>{{ t('message.preferenceRoadmapSorting') }}</li>
            <li>{{ t('message.preferenceRoadmapFilters') }}</li>
          </ul>
        </article>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DecimalInput from '@/components/DecimalInput.vue'
import { useApiError } from '@/composables/useApiError'
import { usePageSizePreference } from '@/composables/pageSizePreference'
import { sanitizePageSize, type PageSizePreferenceSource } from '@/composables/pageSizePreferenceCore'

const { t } = useI18n()
const { notifyError, notifySuccess, notifyWarning } = useApiError()
const { fetchMyListPreferences, fetchEffectiveListPreferences, updateMyListPreferences } = usePageSizePreference()

const loading = ref(false)
const saving = ref(false)
const effectiveSource = ref<PageSizePreferenceSource>('DEFAULT')
const form = reactive({
  pageSize: 20
})
const pageSizeInput = ref('20')

const currentSourceLabel = computed(() => {
  switch (effectiveSource.value) {
    case 'USER':
      return t('message.preferenceSourceUser')
    case 'TENANT':
      return t('message.preferenceSourceTenant')
    default:
      return t('message.preferenceSourceDefault')
  }
})

const sourceBadgeClass = computed(() => ({
  'preference-hero__badge--user': effectiveSource.value === 'USER',
  'preference-hero__badge--tenant': effectiveSource.value === 'TENANT',
  'preference-hero__badge--default': effectiveSource.value === 'DEFAULT'
}))

const fetchSettings = async () => {
  loading.value = true
  try {
    const [mine, effective] = await Promise.all([
      fetchMyListPreferences(),
      fetchEffectiveListPreferences()
    ])
    form.pageSize = mine.pageSize ?? effective.pageSize ?? 20
    pageSizeInput.value = String(form.pageSize)
    effectiveSource.value = mine.pageSize != null ? 'USER' : effective.source || 'DEFAULT'
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}

const save = async () => {
  const parsedPageSize = sanitizePageSize(pageSizeInput.value)
  if (parsedPageSize == null) {
    notifyWarning(t('message.pageSizeRangeHint'))
    return
  }
  saving.value = true
  try {
    form.pageSize = parsedPageSize
    const result = await updateMyListPreferences(form.pageSize)
    form.pageSize = result.pageSize || form.pageSize
    pageSizeInput.value = String(form.pageSize)
    effectiveSource.value = 'USER'
    notifySuccess()
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

fetchSettings()
</script>

<style scoped>
.preference-page {
  min-height: calc(100vh - 160px);
}

.preference-surface {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px;
  overflow-y: auto;
}

.preference-hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 28px 32px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.12), transparent 38%),
    linear-gradient(135deg, #ffffff 0%, #f6f9ff 52%, #eef4ff 100%);
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.06);
}

.preference-hero__eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #2563eb;
}

.preference-hero__title {
  margin: 0;
  font-size: 30px;
  line-height: 1.15;
  font-weight: 700;
  color: #0f172a;
}

.preference-hero__description {
  margin: 12px 0 0;
  max-width: 720px;
  font-size: 14px;
  line-height: 1.8;
  color: #475569;
}

.preference-hero__badge-group {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 12px;
  min-width: 260px;
}

.preference-hero__badge {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(148, 163, 184, 0.3);
}

.preference-hero__badge--soft {
  color: #334155;
  background: rgba(248, 250, 252, 0.94);
}

.preference-hero__badge--user {
  color: #166534;
  background: rgba(220, 252, 231, 0.95);
  border-color: rgba(34, 197, 94, 0.35);
}

.preference-hero__badge--tenant {
  color: #9a3412;
  background: rgba(255, 237, 213, 0.95);
  border-color: rgba(249, 115, 22, 0.28);
}

.preference-hero__badge--default {
  color: #1d4ed8;
  background: rgba(219, 234, 254, 0.95);
  border-color: rgba(59, 130, 246, 0.26);
}

.preference-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(280px, 0.9fr);
  gap: 20px;
}

.preference-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px 26px;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.05);
}

.preference-panel--primary {
  background:
    linear-gradient(180deg, rgba(248, 250, 252, 0.6), rgba(255, 255, 255, 1)),
    #ffffff;
}

.preference-panel--secondary {
  background:
    linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 1)),
    #ffffff;
}

.preference-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.preference-panel__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.preference-panel__subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.preference-setting {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 22px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.78), rgba(255, 255, 255, 0.96));
  border: 1px solid rgba(191, 219, 254, 0.8);
}

.preference-setting__info {
  flex: 1;
  min-width: 0;
}

.preference-setting__label-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.preference-setting__label {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.preference-setting__status {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #1d4ed8;
  background: rgba(219, 234, 254, 0.95);
}

.preference-setting__description {
  margin: 10px 0 0;
  font-size: 14px;
  line-height: 1.75;
  color: #475569;
}

.preference-setting__meta {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: #64748b;
}

.preference-setting__control {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.preference-setting__input {
  width: 180px;
}

.preference-setting__input :deep(.el-input__wrapper) {
  min-height: 44px;
}

.preference-panel__actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.preference-panel__action-hint {
  font-size: 12px;
  line-height: 1.7;
  color: #64748b;
}

.preference-roadmap {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 12px;
  color: #334155;
  font-size: 14px;
  line-height: 1.7;
}

@media (max-width: 1024px) {
  .preference-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .preference-page {
    min-height: auto;
  }

  .preference-hero {
    flex-direction: column;
    padding: 22px 20px;
  }

  .preference-hero__title {
    font-size: 24px;
  }

  .preference-hero__badge-group {
    justify-content: flex-start;
    min-width: 0;
  }

  .preference-panel {
    padding: 20px;
  }

  .preference-setting {
    flex-direction: column;
    align-items: stretch;
  }

  .preference-setting__control {
    justify-content: flex-start;
  }

  .preference-setting__input {
    width: 100%;
    max-width: 220px;
  }
}
</style>
