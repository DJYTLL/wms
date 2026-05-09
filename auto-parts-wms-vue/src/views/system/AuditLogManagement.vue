<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ t('page.auditLogManagement') }}</div>
      <div class="audit-toolbar">
        <div class="table-toolbar">
          <div class="table-filters">
            <el-input
              v-model="keyword"
              class="table-search table-search--wide"
              :placeholder="t('placeholder.keyword')"
              clearable
            />
            <el-select
              v-model="action"
              class="table-search table-search--narrow"
              :placeholder="t('field.action')"
              clearable
            >
              <el-option
                v-for="option in actionOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-select
              v-model="status"
              class="table-search table-search--narrow"
              :placeholder="t('field.result')"
              clearable
            >
              <el-option :label="t('status.success')" value="SUCCESS" />
              <el-option :label="t('status.fail')" value="FAIL" />
            </el-select>
            <el-select
              v-if="isSuperAdmin"
              v-model="tenantId"
              class="table-search table-search--narrow"
              :placeholder="t('field.tenant')"
              clearable
            >
              <el-option
                v-for="option in tenantOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-date-picker
              v-model="dateRange"
              type="datetimerange"
              :range-separator="t('separator.to')"
              :start-placeholder="t('field.startTime')"
              :end-placeholder="t('field.endTime')"
              format="YYYY-MM-DD HH:mm"
              class="table-date-range table-date-range--compact"
            />
          </div>
          <div class="table-actions">
            <el-button @click="showAdvancedFilters = !showAdvancedFilters">
              {{ showAdvancedFilters ? t('action.collapseFilters') : t('action.moreFilters') }}
            </el-button>
            <el-button type="primary" @click="fetchLogs">{{ t('action.search') }}</el-button>
            <el-button @click="exportLogs">{{ t('action.export') }}</el-button>
          </div>
        </div>
        <div v-if="showAdvancedFilters" class="audit-toolbar__advanced">
          <el-input
            v-model="actorUsername"
            class="table-search table-search--wide"
            :placeholder="t('field.actor')"
            clearable
          />
          <el-input
            v-model="requestId"
            class="table-search table-search--wide"
            :placeholder="t('field.requestId')"
            clearable
          />
          <el-input
            v-model="method"
            class="table-search table-search--narrow"
            :placeholder="t('field.method')"
            clearable
          />
          <el-input
            v-model="path"
            class="table-search table-search--wide"
            :placeholder="t('field.path')"
            clearable
          />
          <el-input
            v-model="errorCode"
            class="table-search table-search--narrow"
            :placeholder="t('field.errorCode')"
            clearable
          />
          <el-input
            v-model="errorMessage"
            class="table-search table-search--wide"
            :placeholder="t('field.errorMessage')"
            clearable
          />
          <el-input
            v-model.number="httpStatus"
            class="table-search table-search--narrow"
            :placeholder="t('field.httpStatus')"
            clearable
          />
          <el-select
            v-model="entityType"
            class="table-search table-search--narrow"
            :placeholder="t('field.entityType')"
            clearable
          >
            <el-option
              v-for="option in entityTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body" v-loading="loading">
        <el-table :data="items">
          <el-table-column type="index" width="60" :label="t('table.index')" />
          <el-table-column
            v-if="isSuperAdmin && canShow('tenant')"
            prop="tenantCode"
            :label="t('field.tenant')"
            min-width="140"
          />
          <el-table-column v-if="canShow('actor')" prop="actorUsername" :label="t('field.actor')" min-width="140" />
          <el-table-column
            v-if="canShow('action')"
            prop="action"
            :label="t('field.action')"
            min-width="160"
            :formatter="formatAction"
          />
          <el-table-column v-if="canShow('entityType')" prop="entityType" :label="t('field.entityType')" min-width="120" />
          <el-table-column v-if="canShow('entityId')" prop="entityId" :label="t('field.entityId')" min-width="120" />
          <el-table-column v-if="canShow('detail')" prop="detail" :label="t('field.detail')" min-width="220" />
          <el-table-column v-if="canShow('status')" prop="status" :label="t('field.result')" min-width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'FAIL' ? 'danger' : 'success'" size="small">
                {{ row.status === 'FAIL' ? t('status.fail') : t('status.success') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="canShow('requestId')" prop="requestId" :label="t('field.requestId')" min-width="180" show-overflow-tooltip />
          <el-table-column v-if="canShow('clientIp')" prop="clientIp" :label="t('field.clientIp')" min-width="140" />
          <el-table-column v-if="canShow('userAgent')" prop="userAgent" :label="t('field.userAgent')" min-width="200" show-overflow-tooltip />
          <el-table-column v-if="canShow('method')" prop="method" :label="t('field.method')" min-width="100" />
          <el-table-column v-if="canShow('path')" prop="path" :label="t('field.path')" min-width="200" show-overflow-tooltip />
          <el-table-column v-if="canShow('httpStatus')" prop="httpStatus" :label="t('field.httpStatus')" min-width="120" />
          <el-table-column v-if="canShow('errorCode')" prop="errorCode" :label="t('field.errorCode')" min-width="120" />
          <el-table-column v-if="canShow('errorMessage')" prop="errorMessage" :label="t('field.errorMessage')" min-width="200" show-overflow-tooltip />
          <el-table-column v-if="canShow('durationMs')" prop="durationMs" :label="t('field.durationMs')" min-width="120" />
          <el-table-column
            v-if="canShow('createdAt')"
            prop="createdAt"
            :label="t('field.createdTime')"
            min-width="180"
            :formatter="formatTime"
          />
          <template #empty>
            <div class="table-empty">{{ t('table.empty') }}</div>
          </template>
        </el-table>
      </div>
      <div class="table-pagination">
        <el-pagination
          :current-page="page"
          :page-size="size"
          :total="total"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useAuthStore } from '@/stores/auth';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { useSystemConfig } from '@/composables/useSystemConfig';

type AuditLogItem = {
  id: number;
  tenantId?: number | null;
  tenantCode?: string | null;
  actorUsername: string;
  action: string;
  entityType: string;
  entityId: string;
  detail: string;
  status?: string | null;
  requestId?: string | null;
  clientIp?: string | null;
  userAgent?: string | null;
  method?: string | null;
  path?: string | null;
  httpStatus?: number | null;
  errorCode?: string | null;
  errorMessage?: string | null;
  durationMs?: number | null;
  createdAt: string;
};

const { t, te } = useI18n();
const { notifyError } = useApiError();
const authStore = useAuthStore();

const loading = ref(false);
const items = ref<AuditLogItem[]>([]);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const { bindPageSizeSync } = useSystemConfig();

const keyword = ref('');
const actorUsername = ref('');
const requestId = ref('');
const method = ref('');
const path = ref('');
const errorCode = ref('');
const errorMessage = ref('');
const httpStatus = ref<number | undefined>();
const action = ref<string | undefined>();
const entityType = ref<string | undefined>();
const status = ref<string | undefined>();
const dateRange = ref<[Date, Date] | null>(null);
const tenantId = ref<number | undefined>();
const showAdvancedFilters = ref(false);
const tenantOptions = ref<Array<{ value: number; label: string }>>([]);
const isSuperAdmin = computed(() => authStore.hasRole('super_admin'));
const translateAuditAction = (code?: string) => {
  if (!code) return '-';
  const key = `auditAction.${code}`;
  return te(key) ? t(key) : code;
};

const actionOptions = computed(() => {
  const codes = [
    'USER_CREATE',
    'USER_UPDATE',
    'USER_STATUS_UPDATE',
    'USER_DELETE',
    'USER_PASSWORD_CHANGE',
    'USER_PASSWORD_RESET',
    'USER_ROLE_SET',
    'ROLE_CREATE',
    'ROLE_UPDATE',
    'ROLE_DELETE',
    'ROLE_PERMISSION_SET',
    'ROLE_PERMISSION_ADD',
    'ROLE_PERMISSION_REMOVE',
    'PERMISSION_CREATE',
    'PERMISSION_UPDATE',
    'PERMISSION_DELETE',
    'MENU_CREATE',
    'MENU_UPDATE',
    'MENU_DELETE',
    'TENANT_MENU_UPDATE',
    'TENANT_COLUMN_UPDATE',
    'ERP_ASSEMBLY_APPROVE',
    'ERP_ASSEMBLY_CREATE',
    'ERP_ASSEMBLY_DELETE',
    'ERP_ASSEMBLY_UPDATE',
    'ERP_CATEGORY_CREATE',
    'ERP_CATEGORY_DELETE',
    'ERP_CATEGORY_UPDATE',
    'ERP_CUSTOMER_CATEGORY_CREATE',
    'ERP_CUSTOMER_CATEGORY_DELETE',
    'ERP_CUSTOMER_CATEGORY_UPDATE',
    'ERP_CUSTOMER_CREATE',
    'ERP_CUSTOMER_DELETE',
    'ERP_CUSTOMER_UPDATE',
    'ERP_DELIVERY_METHOD_CREATE',
    'ERP_DELIVERY_METHOD_DELETE',
    'ERP_DELIVERY_METHOD_UPDATE',
    'ERP_LOCATION_CREATE',
    'ERP_LOCATION_DELETE',
    'ERP_LOCATION_UPDATE',
    'ERP_PAYMENT_METHOD_CREATE',
    'ERP_PAYMENT_METHOD_DELETE',
    'ERP_PAYMENT_METHOD_UPDATE',
    'ERP_PRODUCT_CREATE',
    'ERP_PRODUCT_DELETE',
    'ERP_PRODUCT_FITMENT_CREATE',
    'ERP_PRODUCT_FITMENT_DELETE',
    'ERP_PRODUCT_FITMENT_UPDATE',
    'ERP_PURCHASE_CANCEL',
    'ERP_PURCHASE_DELETE',
    'ERP_PURCHASE_RETURN_APPROVE',
    'ERP_PURCHASE_RETURN_CREATE',
    'ERP_PURCHASE_RETURN_DELETE',
    'ERP_PURCHASE_RETURN_UPDATE',
    'ERP_PURCHASE_UNAPPROVE',
    'ERP_SALE_CANCEL',
    'ERP_SALE_DELETE',
    'ERP_SALE_RED_FLUSH',
    'ERP_SALE_RETURN_APPROVE',
    'ERP_SALE_RETURN_CREATE',
    'ERP_SALE_RETURN_DELETE',
    'ERP_SALE_RETURN_RED_FLUSH',
    'ERP_SALE_RETURN_UPDATE',
    'ERP_SALE_UNAPPROVE',
    'ERP_SALE_UPDATE',
    'ERP_SETTLEMENT_METHOD_CREATE',
    'ERP_SETTLEMENT_METHOD_DELETE',
    'ERP_SETTLEMENT_METHOD_UPDATE',
    'ERP_STOCK_COUNT_APPROVE',
    'ERP_STOCK_COUNT_CANCEL',
    'ERP_STOCK_COUNT_CREATE',
    'ERP_STOCK_COUNT_UPDATE',
    'ERP_SUPPLIER_CREATE',
    'ERP_SUPPLIER_DELETE',
    'ERP_SUPPLIER_UPDATE',
    'ERP_UNIT_CREATE',
    'ERP_UNIT_DELETE',
    'ERP_UNIT_UPDATE',
    'ERP_VEHICLE_BRAND_CREATE',
    'ERP_VEHICLE_BRAND_DELETE',
    'ERP_VEHICLE_BRAND_UPDATE',
    'ERP_VEHICLE_MODEL_CREATE',
    'ERP_VEHICLE_MODEL_DELETE',
    'ERP_VEHICLE_MODEL_UPDATE',
    'ERP_VEHICLE_SERIES_CREATE',
    'ERP_VEHICLE_SERIES_DELETE',
    'ERP_VEHICLE_SERIES_UPDATE',
    'ERP_WAREHOUSE_CREATE',
    'ERP_WAREHOUSE_DELETE',
    'ERP_WAREHOUSE_UPDATE',
  ];
  return codes.map((code) => ({
    value: code,
    label: translateAuditAction(code),
  }));
});

const entityTypeOptions = computed(() => {
  const types = ['user', 'role', 'permission', 'menu', 'tenant'];
  return types.map((type) => ({
    value: type,
    label: type,
  }));
});

const defaultColumns = [
  'tenant',
  'actor',
  'action',
  'entityType',
  'entityId',
  'detail',
  'status',
  'requestId',
  'clientIp',
  'userAgent',
  'method',
  'path',
  'httpStatus',
  'errorCode',
  'errorMessage',
  'durationMs',
  'createdAt',
];
const { isVisible, fetchTenantKeys } = useColumnSettings('audit-logs', defaultColumns);
const columnPermissionMap: Record<string, string> = {
  tenant: 'column:audit-logs:tenant',
  actor: 'column:audit-logs:actor',
  action: 'column:audit-logs:action',
  entityType: 'column:audit-logs:entityType',
  entityId: 'column:audit-logs:entityId',
  detail: 'column:audit-logs:detail',
  status: 'column:audit-logs:status',
  requestId: 'column:audit-logs:requestId',
  clientIp: 'column:audit-logs:clientIp',
  userAgent: 'column:audit-logs:userAgent',
  method: 'column:audit-logs:method',
  path: 'column:audit-logs:path',
  httpStatus: 'column:audit-logs:httpStatus',
  errorCode: 'column:audit-logs:errorCode',
  errorMessage: 'column:audit-logs:errorMessage',
  durationMs: 'column:audit-logs:durationMs',
  createdAt: 'column:audit-logs:createdAt'
};


const fetchLogs = async () => {
  loading.value = true;
  try {
    const params: Record<string, string | number> = {
      page: page.value,
      size: size.value,
    };
    if (keyword.value) params.keyword = keyword.value;
    if (actorUsername.value) params.actorUsername = actorUsername.value;
    if (requestId.value) params.requestId = requestId.value;
    if (method.value) params.method = method.value;
    if (path.value) params.path = path.value;
    if (errorCode.value) params.errorCode = errorCode.value;
    if (errorMessage.value) params.errorMessage = errorMessage.value;
    if (httpStatus.value) params.httpStatus = httpStatus.value;
    if (action.value) params.action = action.value;
    if (entityType.value) params.entityType = entityType.value;
    if (status.value) params.status = status.value;
    if (isSuperAdmin.value && tenantId.value) params.tenantId = tenantId.value;
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0].toISOString();
      params.endTime = dateRange.value[1].toISOString();
    }

    const res: any = await request.get('/audit-logs/page', { params });
    const data = res.data.data || {};
    items.value = data.items || [];
    total.value = data.total || 0;
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
};

const fetchTenants = async () => {
  if (!isSuperAdmin.value) return;
  try {
    const res: any = await request.get('/tenants');
    const data = res.data.data || [];
    tenantOptions.value = data.map((tenant: any) => ({
      value: tenant.id,
      label: `${tenant.code} - ${tenant.name}`,
    }));
  } catch (error) {
    notifyError(error);
  }
};

const exportLogs = async () => {
  try {
    const params: Record<string, string | number> = {};
    if (keyword.value) params.keyword = keyword.value;
    if (actorUsername.value) params.actorUsername = actorUsername.value;
    if (requestId.value) params.requestId = requestId.value;
    if (method.value) params.method = method.value;
    if (path.value) params.path = path.value;
    if (errorCode.value) params.errorCode = errorCode.value;
    if (errorMessage.value) params.errorMessage = errorMessage.value;
    if (httpStatus.value) params.httpStatus = httpStatus.value;
    if (action.value) params.action = action.value;
    if (entityType.value) params.entityType = entityType.value;
    if (status.value) params.status = status.value;
    if (isSuperAdmin.value && tenantId.value) params.tenantId = tenantId.value;
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0].toISOString();
      params.endTime = dateRange.value[1].toISOString();
    }
    const res: any = await request.get('/audit-logs/export', {
      params,
      responseType: 'blob',
    });
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'audit-logs.csv';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(link.href);
  } catch (error) {
    notifyError(error);
  }
};

const canShow = (key: string) => {
  const permission = columnPermissionMap[key];
  if (permission && !authStore.hasPermission(permission)) {
    return false;
  }
  return isVisible(key);
};

const formatTime = (_row: AuditLogItem, _column: unknown, value?: string) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
};

const formatAction = (_row: AuditLogItem, _column: unknown, value?: string) => {
  return translateAuditAction(value);
};

const onPageChange = (p: number) => {
  page.value = p;
  fetchLogs();
};

const onSizeChange = (newSize: number) => {
  size.value = newSize;
  page.value = 1;
  fetchLogs();
};

onMounted(() => {
  fetchTenants();
  fetchLogs();
  fetchTenantKeys();
  bindPageSizeSync(size, fetchLogs);
});

</script>

<style scoped>
.audit-toolbar {
  width: 100%;
  padding: 16px 18px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

.table-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
}

.table-filters {
  display: grid;
  grid-template-columns: 220px 140px 140px 140px 280px;
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

.audit-toolbar__advanced {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px solid #eef1f4;
}

.table-card {
  min-height: 0;
}

.table-body {
  flex: 1;
  overflow: auto;
}

@media (max-width: 1280px) {
  .audit-toolbar {
    padding: 14px;
  }

  .table-toolbar {
    grid-template-columns: minmax(0, 1fr);
  }

  .table-filters {
    grid-template-columns: 200px 140px 140px 140px 260px;
  }

  .table-actions {
    justify-content: flex-start;
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
}
</style>
