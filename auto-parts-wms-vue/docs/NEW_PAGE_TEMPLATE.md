# 新增页面模板（前端示例：品牌管理）

本模板可直接复制修改，目标是新增“品牌管理”页面，并与后端 `/api/brands` 对齐。

## 1) 路由配置
```ts
{
  path: 'basic/brand-management',
  name: 'brand-management',
  component: () => import('../views/basic/BrandManagement.vue'),
  meta: { title: '品牌管理', permission: 'brand:view' }
}
```

## 2) i18n 约定
新增以下 key（示例）：
- `nav.brand`
- `page.brand`
- `field.brandCode`
- `field.brandName`
- `action.add` / `action.edit` / `action.delete`
- `message.success` / `message.deleteConfirm`

## 3) 页面骨架（可复制）
```vue
<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-title">{{ t('page.brand') }}</div>
      <div class="page-actions">
        <el-button type="primary" v-permission="'brand:add'" @click="openCreate">
          {{ t('action.add') }}
        </el-button>
      </div>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <el-input v-model="keyword" :placeholder="t('placeholder.keyword')" clearable />
        <el-button @click="fetchData">{{ t('action.search') }}</el-button>
      </div>

      <div class="table-body" v-loading="loading">
        <el-table :data="items">
          <el-table-column prop="code" :label="t('field.brandCode')" />
          <el-table-column prop="name" :label="t('field.brandName')" />
          <el-table-column prop="enabled" :label="t('status.enabled')" />
          <el-table-column fixed="right" :label="t('action.actions')">
            <template #default="{ row }">
              <el-button link v-permission="'brand:edit'" @click="openEdit(row)">
                {{ t('action.edit') }}
              </el-button>
              <el-button link type="danger" v-permission="'brand:delete'" @click="onDelete(row)">
                {{ t('action.delete') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-pagination">
        <el-pagination
          :current-page="page"
          :page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="onPageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '@/utils/request'
import { useApiError } from '@/composables/useApiError'

const { t } = useI18n()
const { notifySuccess, notifyError } = useApiError()

const loading = ref(false)
const items = ref<any[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/brands/page', {
      params: { page: page.value, size: size.value, keyword: keyword.value }
    })
    const data = res.data.data
    items.value = data.items || []
    total.value = data.total || 0
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}

const onPageChange = (p: number) => {
  page.value = p
  fetchData()
}

const openCreate = () => {}
const openEdit = (row: any) => {}
const onDelete = async (row: any) => {
  try {
    await request.delete(`/brands/${row.id}`)
    notifySuccess()
    fetchData()
  } catch (error) {
    notifyError(error)
  }
}
</script>
```

## 4) 必做清单
- 路由 `meta.permission` 必须与后端权限码一致。
- 所有按钮使用 `v-permission` 控制。
- 列表页使用统一结构与 `table-card / table-pagination` 样式。
- 使用 `useApiError()` 统一提示。
