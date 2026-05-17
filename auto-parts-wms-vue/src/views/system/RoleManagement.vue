<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.roleManagement') }}</div>
      <div class="role-toolbar">
        <div class="table-toolbar">
          <div class="table-filters">
            <el-input
              v-model="searchQuery"
              :placeholder="$t('action.search')"
              class="table-search role-toolbar__search--wide"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
            <el-select
              v-model="statusFilter"
              :placeholder="$t('field.status')"
              class="table-search role-toolbar__search--narrow"
              @change="handleSearch"
            >
              <el-option :label="$t('filter.all')" value="all" />
              <el-option :label="$t('status.active')" value="enabled" />
              <el-option :label="$t('status.inactive')" value="disabled" />
            </el-select>
          </div>
          <div class="table-actions">
            <el-button type="primary" @click="openAddModal">{{ $t('action.add') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <el-table :data="filteredData" style="width: 100%" v-loading="loading" :empty-text="$t('table.empty')">
        <el-table-column type="index" :label="$t('table.index')" width="80" />
        <el-table-column v-if="canShow('name')" prop="name" :label="$t('field.name')" min-width="150" />
        <el-table-column v-if="canShow('code')" prop="code" :label="$t('field.code')" min-width="150">
          <template #default="{ row }">
            <code class="code-badge">{{ row.code }}</code>
          </template>
        </el-table-column>
        <el-table-column v-if="canShow('description')" prop="description" :label="$t('field.description')" min-width="200" show-overflow-tooltip />
        <el-table-column v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
              {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('table.actions')" width="150" fixed="right">
          <template #default="{ row }">
            <el-tooltip
              :disabled="canEditRole(row.code)"
              :content="editDisabledReason(row.code)"
              placement="top"
            >
              <el-button
                link
                type="primary"
                size="small"
                :disabled="!canEditRole(row.code)"
                @click="openEditModal(row)"
              >
                {{ $t('action.edit') }}
              </el-button>
            </el-tooltip>
            <el-tooltip
              :disabled="!isReservedRole(row.code)"
              content="保留角色不允许通过角色管理接口删除"
              placement="top"
            >
              <el-button
                link
                type="danger"
                size="small"
                :disabled="isReservedRole(row.code)"
                @click="handleDelete(row)"
              >
                {{ $t('action.delete') }}
              </el-button>
            </el-tooltip>
          </template>
        </el-table-column>
        </el-table>
      </div>
      <div class="table-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :current-page="page"
          :page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 弹窗表单 -->
    <el-dialog
      v-model="showModal"
      :title="isEditing ? $t('action.edit') : $t('action.add')"
      width="960px"
      @closed="resetForm"
      top="5vh"
    >
      <el-form :model="formData" label-width="100px">
        <el-form-item :label="$t('field.name')" required>
          <el-input v-model="formData.name" placeholder="Ex: Administrator" :disabled="isPermissionOnlyMode" />
        </el-form-item>
        <el-form-item :label="$t('field.code')" required>
          <el-input v-model="formData.code" placeholder="Ex: admin" :disabled="isPermissionOnlyMode" />
        </el-form-item>
        <el-form-item :label="$t('field.description')">
          <el-input v-model="formData.description" type="textarea" :disabled="isPermissionOnlyMode" />
        </el-form-item>
        <el-form-item :label="$t('field.status')">
          <el-switch v-model="formData.enabled" :disabled="isPermissionOnlyMode" />
        </el-form-item>
        
        <!-- 权限分配 -->
        <el-form-item :label="$t('field.permissions')">
          <div class="permission-panel">
            <div class="permission-panel__toolbar">
              <el-popover
                placement="bottom-start"
                :width="360"
                trigger="click"
                popper-class="page-tree-popper"
                v-model:visible="pageTreeVisible"
              >
                <template #reference>
                  <button type="button" class="page-tree-trigger permission-panel__page-trigger">
                    <span :class="['page-tree-trigger__text', { 'is-placeholder': !pageKey }]">
                      {{ selectedPageLabel }}
                    </span>
                    <span class="page-tree-trigger__arrow" :class="{ 'is-open': pageTreeVisible }">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="6 9 12 15 18 9"></polyline>
                      </svg>
                    </span>
                  </button>
                </template>

                <div class="page-tree-dropdown">
                  <div v-for="item in pageTreeData" :key="item.id" class="page-tree-node page-tree-node--root">
                    <button
                      type="button"
                      class="page-tree-label page-tree-label--root"
                      :class="{ 'is-active': isTreeNodeActive(item) }"
                      @click.stop="handleTreeNodeClick(item)"
                    >
                      <span v-if="item.icon" class="page-tree-label__icon" v-html="item.icon"></span>
                      <span class="page-tree-label__text">{{ item.label }}</span>
                      <span
                        v-if="item.children.length"
                        class="page-tree-label__arrow"
                        :class="{ 'is-open': item.isOpen }"
                      >
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="9 18 15 12 9 6"></polyline>
                        </svg>
                      </span>
                    </button>

                    <div v-if="item.children.length && item.isOpen" class="page-tree-children">
                      <template v-for="child in item.children" :key="child.id">
                        <button
                          type="button"
                          class="page-tree-label page-tree-label--child"
                          :class="{
                            'is-active': isTreeNodeActive(child),
                            'is-leaf': child.selectable && child.children.length === 0,
                          }"
                          @click.stop="handleTreeNodeClick(child)"
                        >
                          <span
                            v-if="child.selectable && child.children.length === 0"
                            class="page-tree-label__bullet"
                            :class="{ 'is-active': isTreeNodeActive(child) && child.selectable }"
                          ></span>
                          <span class="page-tree-label__text">{{ child.label }}</span>
                          <span
                            v-if="child.children.length"
                            class="page-tree-label__arrow"
                            :class="{ 'is-open': child.isOpen }"
                          >
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                              <polyline points="9 18 15 12 9 6"></polyline>
                            </svg>
                          </span>
                        </button>

                        <div v-if="child.children.length && child.isOpen" class="page-tree-grandchildren">
                          <button
                            v-for="grandChild in child.children"
                            :key="grandChild.id"
                            type="button"
                            class="page-tree-label page-tree-label--leaf"
                            :class="{ 'is-active': isTreeNodeActive(grandChild) }"
                            @click.stop="handleTreeNodeClick(grandChild)"
                          >
                            <span
                              class="page-tree-label__bullet"
                              :class="{ 'is-active': isTreeNodeActive(grandChild) }"
                            ></span>
                            <span class="page-tree-label__text">{{ grandChild.label }}</span>
                          </button>
                        </div>
                      </template>
                    </div>
                  </div>

                  <div v-if="pageTreeData.length === 0" class="page-tree-empty">
                    {{ $t('table.empty') }}
                  </div>
                </div>
              </el-popover>

              <div class="permission-panel__tools">
                <el-tag type="info" effect="plain">
                  {{ currentPagePermissionSummary }}
                </el-tag>
                <el-button size="small" @click="selectCurrentPagePermissions">
                  {{ $t('action.selectAll') }}
                </el-button>
                <el-button size="small" plain @click="clearCurrentPagePermissions">
                  清空本页
                </el-button>
              </div>
            </div>

            <div v-if="!pageKey" class="permission-empty-tip">
              {{ $t('message.required') }}
            </div>

            <div v-else class="permission-checkbox-panel">
              <el-empty v-if="currentPagePermissions.length === 0" :description="$t('table.empty')" />

              <el-checkbox-group v-else v-model="selectedPermissionIds">
                <el-checkbox
                  v-for="permission in currentPagePermissions"
                  :key="permission.id"
                  :value="permission.id"
                >
                  <span class="permission-option">
                    <span class="permission-option__name">{{ permission.name }}</span>
                    <code class="permission-option__code">{{ permission.code }}</code>
                  </span>
                </el-checkbox>
              </el-checkbox-group>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showModal = false">{{ $t('action.cancel') }}</el-button>
          <el-button type="primary" @click="saveData">{{ $t('action.save') }}</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onActivated, watch } from 'vue';
import { ElMessageBox } from 'element-plus';
import { useI18n } from 'vue-i18n';
import { useAuthStore } from '@/stores/auth';
import { useMenuStore, type MenuItem } from '@/stores/menu';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useSystemConfig } from '@/composables/useSystemConfig';
import { useColumnSettings } from '@/composables/useColumnSettings';

// --- 类型定义 ---
interface Permission {
  id: number;
  code: string;
  name: string;
  enabled: boolean;
  pageKey?: string;
}

interface Role {
  id: number;
  name: string;
  code: string;
  description: string;
  enabled: boolean;
  permissionIds?: number[];
}

// --- 初始化 ---
const { t } = useI18n();
const authStore = useAuthStore();
const menuStore = useMenuStore();
// --- 状态 ---
const searchQuery = ref('');
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all');
const showModal = ref(false);
const isEditing = ref(false);
const pageKey = ref('');
const pageTreeVisible = ref(false);
const pageTreeOpenState = ref<Record<string, boolean>>({});
const treeMenus = ref<MenuItem[]>([]);
const selectedPermissionIds = ref<number[]>([]);

const roleList = ref<Role[]>([]);
const permissionList = ref<Permission[]>([]);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const { bindPageSizeSync } = useSystemConfig();
const canUsePlatformPermissions = computed(() => authStore.hasRole('super_admin'));
const { notifyError, notifySuccess, notifyWarning } = useApiError();

const defaultColumns = ['name', 'code', 'description', 'status'];
const { isVisible, fetchTenantKeys } = useColumnSettings('role-management', defaultColumns);
const columnPermissionMap: Record<string, string> = {
  name: 'column:role-management:name',
  code: 'column:role-management:code',
  description: 'column:role-management:description',
  status: 'column:role-management:status'
};
const reservedRoleCodes = new Set(['admin', 'super_admin']);
const isPermissionOnlyMode = ref(false);

type PageOption = {
  key: string;
  label: string;
};

type ExtraPageNode = {
  pageKey: string;
  label?: string;
};

type PageTreeNode = {
  id: string;
  label: string;
  icon?: string;
  pageKey?: string;
  selectable: boolean;
  isOpen: boolean;
  children: PageTreeNode[];
};

const permissionPrefixPageMap: Array<{ prefix: string; pageKeys: string[] }> = [
  { prefix: 'user:', pageKeys: ['user-management'] },
  { prefix: 'role:', pageKeys: ['role-management'] },
  { prefix: 'permission:', pageKeys: ['permission-management'] },
  { prefix: 'audit:', pageKeys: ['audit-logs'] },
  { prefix: 'menu:', pageKeys: ['menu-management'] },
  { prefix: 'system-config:', pageKeys: ['system-configs'] },
  { prefix: 'tenant:', pageKeys: ['tenant-management'] },
  { prefix: 'erp-product:', pageKeys: ['erp-product'] },
  { prefix: 'erp-customer:', pageKeys: ['erp-customer'] },
  { prefix: 'erp-customer-category:', pageKeys: ['erp-customer-category'] },
  { prefix: 'erp-supplier:', pageKeys: ['erp-supplier'] },
  { prefix: 'erp-warehouse:', pageKeys: ['erp-warehouse'] },
  { prefix: 'erp-location:', pageKeys: ['erp-location'] },
  { prefix: 'erp-category:', pageKeys: ['erp-category'] },
  { prefix: 'erp-unit:', pageKeys: ['erp-unit'] },
  { prefix: 'erp-settlement-method:', pageKeys: ['erp-settlement-method'] },
  { prefix: 'erp-payment-method:', pageKeys: ['erp-payment-method'] },
  { prefix: 'erp-receipt-method:', pageKeys: ['erp-receipt-method'] },
  { prefix: 'erp-delivery-method:', pageKeys: ['erp-delivery-method'] },
  { prefix: 'erp-vehicle-brand:', pageKeys: ['erp-vehicle-brand'] },
  { prefix: 'erp-vehicle-series:', pageKeys: ['erp-vehicle-series'] },
  { prefix: 'erp-vehicle-model:', pageKeys: ['erp-vehicle-model'] },
  { prefix: 'erp-product-fitment:', pageKeys: ['erp-product-fitment'] },
  { prefix: 'erp-purchase-draft:', pageKeys: ['erp-purchase-draft'] },
  { prefix: 'erp-purchase-approved:', pageKeys: ['erp-purchase-approved'] },
  { prefix: 'erp-purchase-return-draft:', pageKeys: ['erp-purchase-return-draft'] },
  { prefix: 'erp-purchase-return-approved:', pageKeys: ['erp-purchase-return-approved'] },
  { prefix: 'erp-sale-draft:', pageKeys: ['erp-sale-draft'] },
  { prefix: 'erp-sale-approved:', pageKeys: ['erp-sale-approved'] },
  { prefix: 'erp-sale-return-draft:', pageKeys: ['erp-sale-return-draft'] },
  { prefix: 'erp-sale-return-approved:', pageKeys: ['erp-sale-return-approved'] },
  { prefix: 'erp-ar:', pageKeys: ['erp-ar'] },
  { prefix: 'erp-ap:', pageKeys: ['erp-ap'] },
  { prefix: 'erp-receipt:', pageKeys: ['erp-receipt'] },
  { prefix: 'erp-payment:', pageKeys: ['erp-payment'] },
  { prefix: 'erp-finance-summary:', pageKeys: ['erp-finance-customer-debt'] },
  { prefix: 'erp-finance-customer-debt:', pageKeys: ['erp-finance-customer-debt'] },
  { prefix: 'erp-finance-supplier-debt:', pageKeys: ['erp-finance-supplier-debt'] },
  { prefix: 'erp-print-template:', pageKeys: ['erp-print-template'] },
  { prefix: 'erp-stock-warning:', pageKeys: ['erp-stock-warning'] },
  { prefix: 'erp-stock-transfer:', pageKeys: ['erp-stock-transfer'] },
  { prefix: 'erp-stock-count:', pageKeys: ['erp-stock-count'] },
  { prefix: 'erp-stock-init:', pageKeys: ['erp-stock-init'] },
  { prefix: 'erp-stock-txn:', pageKeys: ['erp-stock-txn'] },
  { prefix: 'erp-stock:', pageKeys: ['erp-stock'] },
  { prefix: 'erp-assembly:', pageKeys: ['erp-assemble-order', 'erp-disassemble-order'] },
  { prefix: 'erp-disassemble:', pageKeys: ['erp-disassemble-order'] },
];

const menuPageKeyMap: Record<string, string[]> = {
  users: ['user-management'],
  roles: ['role-management'],
  permissions: ['permission-management'],
  'audit-logs': ['audit-logs'],
  'column-permissions': ['column-permissions'],
  columnPermissions: ['column-permissions'],
  'menu-management': ['menu-management'],
  'system-config': ['system-configs'],
  tenants: ['tenant-management'],
  'erp-product': ['erp-product'],
  'erp-vehicle-fitment': ['erp-vehicle-brand', 'erp-vehicle-series', 'erp-vehicle-model', 'erp-product-fitment'],
  'erp-customer': ['erp-customer'],
  'erp-customer-category': ['erp-customer-category'],
  'erp-supplier': ['erp-supplier'],
  'erp-warehouse': ['erp-warehouse'],
  'erp-location': ['erp-location'],
  'erp-category': ['erp-category'],
  'erp-unit': ['erp-unit'],
  'erp-settlement-method': ['erp-settlement-method'],
  'erp-payment-method': ['erp-payment-method'],
  'erp-receipt-method': ['erp-receipt-method'],
  'erp-delivery-method': ['erp-delivery-method'],
  'erp-print-template': ['erp-print-template'],
  'erp-purchase-draft': ['erp-purchase-draft'],
  'erp-purchase-approved': ['erp-purchase-approved'],
  'erp-purchase-return-draft': ['erp-purchase-return-draft'],
  'erp-purchase-return-approved': ['erp-purchase-return-approved'],
  'erp-sale-draft': ['erp-sale-draft'],
  'erp-sale-approved': ['erp-sale-approved'],
  'erp-sale-return-draft': ['erp-sale-return-draft'],
  'erp-sale-return-approved': ['erp-sale-return-approved'],
  'erp-stock': ['erp-stock'],
  'erp-stock-txn': ['erp-stock-txn'],
  'erp-stock-count': ['erp-stock-count'],
  'erp-stock-transfer': ['erp-stock-transfer'],
  'erp-stock-init': ['erp-stock-init'],
  'erp-stock-warning': ['erp-stock-warning'],
  'erp-assemble-order': ['erp-assemble-order'],
  'erp-disassemble-order': ['erp-disassemble-order'],
  'erp-ar': ['erp-ar'],
  'erp-finance-summary': ['erp-finance-customer-debt'],
  'erp-finance-customer-debt': ['erp-finance-customer-debt'],
  'erp-finance-supplier-debt': ['erp-finance-supplier-debt'],
  'erp-ap': ['erp-ap'],
  'erp-receipt': ['erp-receipt'],
  'erp-payment': ['erp-payment'],
};

const menuExtraPageMap: Record<string, ExtraPageNode[]> = {};

const hiddenLegacyPermissionPrefixes = ['erp-purchase:', 'erp-sale:'];

const formData = reactive<Omit<Role, 'id'>>({
  name: '',
  code: '',
  description: '',
  enabled: true
});
const currentId = ref<number | null>(null);

// --- 计算属性 ---
const filteredData = computed(() => roleList.value);

const canManageReservedRole = (code?: string) => {
  const normalized = (code || '').trim().toLowerCase();
  if (normalized === 'super_admin') {
    return authStore.hasRole('super_admin');
  }
  if (normalized === 'admin') {
    return authStore.hasRole('super_admin');
  }
  return true;
};

const canEditRole = (code?: string) => !isReservedRole(code) || canManageReservedRole(code);

const editDisabledReason = (code?: string) => {
  const normalized = (code || '').trim().toLowerCase();
  if (!reservedRoleCodes.has(normalized)) {
    return '';
  }
  if (normalized === 'super_admin') {
    return '仅系统超级管理员可维护 super_admin 角色权限';
  }
  return '仅系统超级管理员可维护 admin 角色权限';
};

const pageLabelMap = computed<Record<string, string>>(() => ({
  'user-management': t('page.userManagement'),
  'role-management': t('page.roleManagement'),
  'permission-management': t('page.permissionManagement'),
  'column-permissions': t('page.columnPermissionManagement'),
  'menu-management': t('page.menuManagement'),
  'tenant-management': t('page.tenantManagement'),
  'audit-logs': t('page.auditLogManagement'),
  'system-configs': t('page.systemConfigManagement'),
  'erp-product': t('page.erpProductManagement'),
  'erp-customer': t('page.erpCustomerManagement'),
  'erp-customer-category': t('page.erpCustomerCategoryManagement'),
  'erp-vehicle-brand': `${t('page.erpVehicleFitmentManagement')} - ${t('field.vehicleBrand')}`,
  'erp-vehicle-series': `${t('page.erpVehicleFitmentManagement')} - ${t('field.vehicleSeries')}`,
  'erp-vehicle-model': `${t('page.erpVehicleFitmentManagement')} - ${t('field.vehicleModel')}`,
  'erp-product-fitment': `${t('page.erpVehicleFitmentManagement')} - ${t('field.productFitment')}`,
  'erp-supplier': t('page.erpSupplierManagement'),
  'erp-warehouse': t('page.erpWarehouseManagement'),
  'erp-location': t('page.erpLocationManagement'),
  'erp-category': t('page.erpCategoryManagement'),
  'erp-unit': t('page.erpUnitManagement'),
  'erp-settlement-method': t('page.erpSettlementMethodManagement'),
  'erp-payment-method': t('page.erpPaymentMethodManagement'),
  'erp-receipt-method': t('page.erpReceiptMethodManagement'),
  'erp-delivery-method': t('page.erpDeliveryMethodManagement'),
  'erp-purchase-draft': t('page.erpPurchaseOrderDraft'),
  'erp-purchase-approved': t('page.erpPurchaseOrderApproved'),
  'erp-purchase-return-draft': t('nav.erpPurchaseReturnDraft'),
  'erp-purchase-return-approved': t('nav.erpPurchaseReturnApproved'),
  'erp-sale-draft': t('nav.erpSaleDraft'),
  'erp-sale-approved': t('nav.erpSaleApproved'),
  'erp-sale-return-draft': t('nav.erpSaleReturnDraft'),
  'erp-sale-return-approved': t('nav.erpSaleReturnApproved'),
  'erp-ar': t('page.erpAccountsReceivableManagement'),
  'erp-ap': t('page.erpAccountsPayableManagement'),
  'erp-receipt': t('page.erpReceiptManagement'),
  'erp-payment': t('page.erpPaymentManagement'),
  'erp-finance-customer-debt': t('page.erpCustomerDebtManagement'),
  'erp-finance-supplier-debt': t('page.erpSupplierDebtManagement'),
  'erp-print-template': t('page.erpPrintTemplateManagement'),
  'erp-stock': t('page.erpStockManagement'),
  'erp-stock-txn': t('page.erpStockTxnManagement'),
  'erp-stock-warning': t('page.erpStockWarningManagement'),
  'erp-stock-count': t('page.erpStockCountManagement'),
  'erp-stock-init': t('page.erpStockInitManagement'),
  'erp-stock-transfer': t('page.erpStockTransferManagement'),
  'erp-assemble-order': t('page.erpAssemblyOrderManagement'),
  'erp-disassemble-order': t('page.erpDisassembleOrderManagement'),
  other: '其他权限',
}));

const resolvePermissionPageKeys = (code: string) => {
  if (!code) {
    return ['other'];
  }

  if (hiddenLegacyPermissionPrefixes.some((prefix) => code.startsWith(prefix))) {
    return [];
  }

  if (code === 'column:edit' || code === 'column:role:manage') {
    return ['column-permissions'];
  }

  if (code.startsWith('column:')) {
    const parts = code.split(':');
    const rawPageKey = parts.length > 2 ? (parts[1] ?? 'column-permissions') : 'column-permissions';
    const columnPageAliases: Record<string, string> = {
      'erp-purchase': 'erp-purchase-draft',
      'erp-purchase-return': 'erp-purchase-return-draft',
      'erp-sale': 'erp-sale-draft',
      'erp-sale-form': 'erp-sale-approved',
      'erp-sale-return': 'erp-sale-return-draft',
      'erp-finance-summary': 'erp-finance-customer-debt',
    };
    return [columnPageAliases[rawPageKey] || rawPageKey];
  }

  if (code.startsWith('role:assign:')) {
    return ['user-management'];
  }

  const matched = permissionPrefixPageMap.find((item) => code.startsWith(item.prefix));
  if (matched) {
    return matched.pageKeys;
  }

  return [code.split(':')[0] || 'other'];
};

const resolvePermissionPageKey = (code: string) => {
  return resolvePermissionPageKeys(code)[0] || 'other';
};

const isConcreteColumnPermission = (code?: string) => {
  return Boolean(
    code
    && code.startsWith('column:')
    && code !== 'column:edit'
    && code !== 'column:role:manage',
  );
};

const pageOptions = computed<PageOption[]>(() => {
  const keys = Array.from(
    new Set(
      visiblePermissionList.value
        .map((item) => item.pageKey || resolvePermissionPageKey(item.code))
        .filter((item): item is string => Boolean(item)),
    ),
  );
  return keys.map((key) => ({
    key,
    label: pageLabelMap.value[key] || key,
  }));
});

const pageOptionsMap = computed(() => {
  return new Map(pageOptions.value.map((item) => [item.key, item]));
});

const selectedPageLabel = computed(() => {
  if (!pageKey.value) return t('field.page');
  return pageLabelMap.value[pageKey.value] || pageKey.value;
});

const visiblePermissionList = computed(() => {
  const source = canUsePlatformPermissions.value
    ? permissionList.value
    : permissionList.value.filter(
      p => !p.code.startsWith('tenant:') && !p.code.startsWith('system-config:')
    );

  return source
    .filter((item) => !isConcreteColumnPermission(item.code))
    .flatMap((item) => resolvePermissionPageKeys(item.code).map((itemPageKey) => ({
      ...item,
      pageKey: itemPageKey,
    })));
});

const containsTreeSelection = (node: PageTreeNode): boolean => {
  if (node.pageKey === pageKey.value) return true;
  return node.children.some((child) => containsTreeSelection(child));
};

const resolveTreeNodeOpen = (nodeId: string, defaultOpen: boolean): boolean => {
  const stored = pageTreeOpenState.value[nodeId];
  return stored === undefined ? defaultOpen : stored;
};

const buildPageLeafNodes = (parentId: string, label: string, mappedKeys: string[]): PageTreeNode[] => {
  const matched = mappedKeys
    .map((mappedKey) => pageOptionsMap.value.get(mappedKey))
    .filter((item): item is PageOption => Boolean(item));

  if (matched.length === 0) {
    return [];
  }

  if (matched.length === 1) {
    const single = matched[0];
    if (!single) {
      return [];
    }
    return [{
      id: `${parentId}:${single.key}`,
      label: label || single.label,
      pageKey: single.key,
      selectable: true,
      isOpen: false,
      children: [],
    }];
  }

  return [{
    id: `${parentId}:group`,
    label,
    selectable: false,
    isOpen: resolveTreeNodeOpen(`${parentId}:group`, matched.some((item) => item.key === pageKey.value)),
    children: matched.map((item) => ({
      id: `${parentId}:${item.key}`,
      label: item.label,
      pageKey: item.key,
      selectable: true,
      isOpen: false,
      children: [],
    })),
  }];
};

const buildExtraPageLeafNodes = (parentId: string, menuKey: string): PageTreeNode[] => {
  const extraNodes = menuExtraPageMap[menuKey] || [];
  return extraNodes
    .map<PageTreeNode | null>((item) => {
      const option = pageOptionsMap.value.get(item.pageKey);
      if (!option) {
        return null;
      }
      return {
        id: `${parentId}:extra:${item.pageKey}`,
        label: item.label || option.label,
        pageKey: item.pageKey,
        selectable: true,
        isOpen: false,
        children: [],
      };
    })
    .filter((item): item is PageTreeNode => Boolean(item));
};

const buildTreeNode = (item: MenuItem, parentId: string): PageTreeNode | null => {
  const menuKey = item.key || '';
  const nodeId = `${parentId}:${menuKey || item.id}`;
  const mappedNodes = buildPageLeafNodes(nodeId, item.title || '', menuPageKeyMap[menuKey] || []);
  const extraNodes = buildExtraPageLeafNodes(nodeId, menuKey);
  const childNodes = (item.children || [])
    .map((child) => buildTreeNode(child, nodeId))
    .filter((child): child is PageTreeNode => Boolean(child));

  const firstMappedNode = mappedNodes[0];
  const onlyDirectLeaf = mappedNodes.length === 1 && Boolean(firstMappedNode?.pageKey) && childNodes.length === 0;
  const directPageKey = onlyDirectLeaf ? firstMappedNode?.pageKey : undefined;
  const children = onlyDirectLeaf ? [] : [...mappedNodes, ...extraNodes, ...childNodes];

  if (!onlyDirectLeaf && children.length === 0) {
    return null;
  }

  const containsCurrent = directPageKey === pageKey.value || children.some((child) => containsTreeSelection(child));
  return {
    id: nodeId,
    label: item.title || item.key || '',
    icon: parentId === 'root' ? item.icon : undefined,
    pageKey: directPageKey,
    selectable: Boolean(directPageKey),
    isOpen: resolveTreeNodeOpen(nodeId, containsCurrent || parentId === 'root'),
    children,
  };
};

const pageTreeData = computed<PageTreeNode[]>(() => {
  const tree = treeMenus.value
    .map((item) => buildTreeNode(item, 'root'))
    .filter((item): item is PageTreeNode => Boolean(item));

  const seen = new Set<string>();
  const walk = (node: PageTreeNode) => {
    if (node.pageKey) seen.add(node.pageKey);
    node.children.forEach(walk);
  };
  tree.forEach(walk);

  const fallbackNodes = pageOptions.value
    .filter((item) => !seen.has(item.key))
    .map((item) => ({
      id: `fallback:${item.key}`,
      label: item.label,
      pageKey: item.key,
      selectable: true,
      isOpen: false,
      children: [],
    }));

  if (fallbackNodes.length > 0) {
    tree.push({
      id: 'root:fallback',
      label: '未映射页面',
      selectable: false,
      isOpen: resolveTreeNodeOpen(
        'root:fallback',
        fallbackNodes.some((item) => item.pageKey === pageKey.value),
      ),
      children: fallbackNodes,
    });
  }

  return tree;
});

const isTreeNodeActive = (node: PageTreeNode): boolean => {
  if (node.pageKey && node.pageKey === pageKey.value) return true;
  return node.children.some((child) => isTreeNodeActive(child));
};

const handleTreeNodeClick = (node: PageTreeNode) => {
  if (node.selectable && node.pageKey) {
    pageKey.value = node.pageKey;
    pageTreeVisible.value = false;
    return;
  }

  if (node.children.length > 0) {
    pageTreeOpenState.value = {
      ...pageTreeOpenState.value,
      [node.id]: !node.isOpen,
    };
  }
};

const currentPagePermissions = computed(() => {
  if (!pageKey.value) {
    return [] as Permission[];
  }
  return visiblePermissionList.value.filter((item) => item.pageKey === pageKey.value);
});

const currentPagePermissionSummary = computed(() => {
  if (!pageKey.value) {
    return t('table.empty');
  }
  const currentIds = new Set(currentPagePermissions.value.map((item) => item.id));
  const selectedCount = selectedPermissionIds.value.filter((id) => currentIds.has(id)).length;
  return `${selectedPageLabel.value}: ${selectedCount}/${currentPagePermissions.value.length}`;
});

// --- 数据加载 ---
const fetchRoles = async () => {
  loading.value = true;
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value,
    };
    if (searchQuery.value) params.keyword = searchQuery.value.trim();
    if (statusFilter.value !== 'all') {
      params.enabled = statusFilter.value === 'enabled';
    }

    const res: any = await request.get('/roles/page', { params });
    if (res.data.code === 200) {
      roleList.value = res.data.data.items || [];
      total.value = res.data.data.total || 0;
    }
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
};

const fetchPermissions = async () => {
  try {
    const res: any = await request.get('/permissions');
    if (res.data.code === 200) {
      permissionList.value = res.data.data;
      ensureSelectedPage();
    }
  } catch (error) {
    notifyError(error);
  }
};

const normalizeTreeMenu = (item: any): MenuItem => {
  return {
    id: Number(item.id),
    key: String(item.code || item.key || ''),
    title: item.title || '',
    path: item.path || '',
    icon: item.icon || '',
    permissionCode: item.permissionCode || null,
    children: Array.isArray(item.children) ? item.children.map((child: any) => normalizeTreeMenu(child)) : [],
  };
};

const decorateTreeMenus = (items: MenuItem[]): MenuItem[] => {
  return items.map((item) => ({
    ...item,
    isOpen: false,
    children: Array.isArray(item.children) ? decorateTreeMenus(item.children) : [],
  }));
};

const loadTreeMenus = async () => {
  try {
    if (canUsePlatformPermissions.value) {
      const res: any = await request.get('/menus/all');
      const data = res.data.data || [];
      const normalized = Array.isArray(data) ? data.map((item: any) => normalizeTreeMenu(item)) : [];
      treeMenus.value = decorateTreeMenus(normalized);
      ensureSelectedPage();
      return;
    }
    await menuStore.fetchMenus();
    treeMenus.value = decorateTreeMenus(menuStore.menus);
    ensureSelectedPage();
  } catch (error) {
    notifyError(error);
  }
};

const resolveFirstSelectable = (node?: PageTreeNode): string | undefined => {
  if (!node) return undefined;
  if (node.pageKey) return node.pageKey;
  for (const child of node.children) {
    const matched = resolveFirstSelectable(child);
    if (matched) return matched;
  }
  return undefined;
};

const ensureSelectedPage = () => {
  const available = new Set(pageOptions.value.map((item) => item.key));
  if (pageKey.value && available.has(pageKey.value)) {
    return;
  }
  const firstPageKey = resolveFirstSelectable(pageTreeData.value[0]) || pageOptions.value[0]?.key || '';
  pageKey.value = firstPageKey;
};

onMounted(() => {
  fetchRoles();
  fetchPermissions();
  loadTreeMenus();
  bindPageSizeSync(size, fetchRoles);
  fetchTenantKeys();
});

onActivated(() => {
  // 当组件被激活时，刷新数据以确保权限树和角色列表是最新的
  fetchRoles();
  fetchPermissions();
  loadTreeMenus();
});

// --- 方法 ---
const handleSearch = () => {
  page.value = 1;
  fetchRoles();
};

const handlePageChange = (newPage: number) => {
  page.value = newPage;
  fetchRoles();
};

const handleSizeChange = (newSize: number) => {
  size.value = newSize;
  page.value = 1;
  fetchRoles();
};

const isReservedRole = (code?: string) => reservedRoleCodes.has((code || '').trim().toLowerCase());

const openAddModal = () => {
  isEditing.value = false;
  currentId.value = null;
  isPermissionOnlyMode.value = false;
  resetForm();
  showModal.value = true;
  selectedPermissionIds.value = [];
  ensureSelectedPage();
};

const openEditModal = async (row: Role) => {
  if (!canEditRole(row.code)) {
    notifyWarning(editDisabledReason(row.code));
    return;
  }

  isEditing.value = true;
  currentId.value = row.id;
  isPermissionOnlyMode.value = isReservedRole(row.code);
  formData.name = row.name;
  formData.code = row.code;
  formData.description = row.description || '';
  formData.enabled = row.enabled;
  
  showModal.value = true;
  ensureSelectedPage();

  // 获取该角色的权限列表
  try {
    const res: any = await request.get(`/roles/${row.id}/permissions`);
    if (res.data.code === 200) {
      selectedPermissionIds.value = res.data.data
        .filter((p: any) => !isConcreteColumnPermission(String(p.code || '')))
        .map((p: any) => p.id);
    }
  } catch (e) {
    console.error('Failed to load role permissions', e);
  }
};

const resetForm = () => {
  formData.name = '';
  formData.code = '';
  formData.description = '';
  formData.enabled = true;
  isPermissionOnlyMode.value = false;
  selectedPermissionIds.value = [];
};

const canShow = (key: string) => {
  const permission = columnPermissionMap[key];
  if (permission && !authStore.hasPermission(permission)) {
    return false;
  }
  return isVisible(key);
};

const saveData = async () => {
  if (!formData.name || !formData.code) {
    notifyWarning(t('message.required'));
    return;
  }

  try {
    let roleId = currentId.value;

    if (!isEditing.value || !isPermissionOnlyMode.value) {
      const url = isEditing.value && currentId.value
        ? `/roles/${currentId.value}`
        : '/roles';
      const method = isEditing.value ? request.put : request.post;
      const res: any = await method(url, {
        name: formData.name,
        code: formData.code,
        description: formData.description,
        enabled: formData.enabled
      });
      if (res.data.code !== 200) {
        return;
      }
      roleId = isEditing.value ? currentId.value : res.data.data.id;
    }

    const permissionIds = [...selectedPermissionIds.value];
    await request.put(`/roles/${roleId}/permissions`, { permissionIds });

    notifySuccess();
    showModal.value = false;
    fetchRoles();
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = (row: Role) => {
  if (isReservedRole(row.code)) {
    notifyWarning('保留角色不允许通过角色管理接口删除');
    return;
  }

  ElMessageBox.confirm(
    `${t('message.deleteConfirm')} "${row.name}"?`,
    'Warning',
    { confirmButtonText: t('action.confirm'), cancelButtonText: t('action.cancel'), type: 'warning' }
  ).then(async () => {
    try {
      const res: any = await request.delete(`/roles/${row.id}`);
      if (res.data.code === 200) {
        notifySuccess();
        fetchRoles();
      }
    } catch (error) {
      notifyError(error);
    }
  });
};

const selectCurrentPagePermissions = () => {
  const next = new Set(selectedPermissionIds.value);
  currentPagePermissions.value.forEach((item) => next.add(item.id));
  selectedPermissionIds.value = Array.from(next);
};

const clearCurrentPagePermissions = () => {
  const currentIds = new Set(currentPagePermissions.value.map((item) => item.id));
  selectedPermissionIds.value = selectedPermissionIds.value.filter((id) => !currentIds.has(id));
};

watch(pageOptions, () => {
  ensureSelectedPage();
});
</script>

<style scoped>
.role-toolbar {
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
  grid-template-columns: 220px 140px;
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

:deep(.role-toolbar__search--wide) {
  width: 220px;
}

:deep(.role-toolbar__search--narrow) {
  width: 140px;
}

.code-badge {
  background: #fff8e1;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  color: #b45309;
}

.permission-panel {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  padding: 14px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.permission-panel__toolbar {
  display: grid;
  grid-template-columns: minmax(0, 320px) minmax(0, 1fr);
  align-items: center;
  gap: 12px;
}

.permission-panel__page-trigger {
  width: 100%;
}

.permission-panel__tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.permission-empty-tip {
  padding: 18px 12px;
  color: #909399;
  text-align: center;
}

.permission-checkbox-panel {
  max-height: 380px;
  overflow-y: auto;
}

.permission-option {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.permission-option__name {
  color: #303133;
}

.permission-option__code {
  background: #fff8e1;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #b45309;
}

.page-tree-trigger {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  color: #303133;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.page-tree-trigger:hover,
.page-tree-trigger:focus-visible {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--el-color-primary) 18%, transparent);
  outline: none;
}

.page-tree-trigger__text {
  min-width: 0;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.page-tree-trigger__text.is-placeholder {
  color: #a8abb2;
}

.page-tree-trigger__arrow {
  width: 16px;
  height: 16px;
  color: #909399;
  flex: 0 0 auto;
  transition: transform 0.2s ease;
}

.page-tree-trigger__arrow.is-open {
  transform: rotate(180deg);
}

.page-tree-dropdown {
  max-height: 420px;
  overflow: auto;
  padding: 6px 0;
}

.page-tree-node {
  display: flex;
  flex-direction: column;
}

.page-tree-children,
.page-tree-grandchildren {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.page-tree-children {
  padding: 4px 0 10px 14px;
}

.page-tree-grandchildren {
  padding: 4px 0 4px 18px;
}

.page-tree-label {
  width: 100%;
  border: 0;
  background: transparent;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  cursor: pointer;
}

.page-tree-label__icon {
  display: flex;
  margin-right: 2px;
  opacity: 0.8;
}

.page-tree-label__text {
  min-width: 0;
  flex: 1 1 auto;
}

.page-tree-label__arrow {
  width: 16px;
  height: 16px;
  color: #909399;
  flex: 0 0 auto;
  transition: transform 0.2s ease;
}

.page-tree-label__arrow.is-open {
  transform: rotate(90deg);
}

.page-tree-label__bullet {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: #909399;
  flex: 0 0 auto;
  opacity: 0.5;
}

.page-tree-label__bullet.is-active {
  background: var(--el-color-primary);
  opacity: 1;
}

.page-tree-label--root {
  min-height: 44px;
  padding: 0 16px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
}

.page-tree-label--child {
  min-height: 34px;
  padding: 7px 10px;
  font-size: 13.5px;
  color: #555;
}

.page-tree-label--child.is-leaf {
  padding-left: 10px;
}

.page-tree-label--leaf {
  min-height: 34px;
  padding: 6px 10px;
  font-size: 13px;
  color: #666;
}

.page-tree-label.is-active {
  color: var(--el-color-primary);
}

.page-tree-label--root.is-active {
  background: rgba(64, 158, 255, 0.14);
}

.page-tree-label--child.is-active,
.page-tree-label--leaf.is-active {
  background: rgba(64, 158, 255, 0.1);
  border-radius: 10px;
}

.page-tree-empty {
  padding: 18px 16px;
  color: #909399;
  font-size: 13px;
}

:deep(.permission-checkbox-panel .el-checkbox-group) {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

:deep(.permission-checkbox-panel .el-checkbox) {
  margin-right: 0;
}

:deep(.permission-checkbox-panel .el-checkbox__label) {
  white-space: normal;
}

@media (max-width: 1280px) {
  .role-toolbar {
    padding: 14px;
  }

  .table-toolbar {
    grid-template-columns: minmax(0, 1fr);
  }

  .table-filters {
    grid-template-columns: 200px 140px;
  }

  .table-actions {
    justify-content: flex-start;
  }

  :deep(.role-toolbar__search--wide) {
    width: 200px;
  }

  .permission-panel__toolbar {
    grid-template-columns: 1fr;
  }

  .permission-panel__tools {
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

  :deep(.role-toolbar__search--wide),
  :deep(.role-toolbar__search--narrow) {
    width: 100% !important;
  }
}
</style>
