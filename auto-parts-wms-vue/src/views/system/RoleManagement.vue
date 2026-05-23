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
        <ErpDataTable :data="filteredData" style="width: 100%" v-loading="loading" :empty-text="$t('table.empty')" table-key="role-management">
        <ErpDataTableColumn type="index" :label="$t('table.index')" width="80" />
        <ErpDataTableColumn v-if="canShow('name')" prop="name" :label="$t('field.name')" min-width="150" />
        <ErpDataTableColumn v-if="canShow('code')" prop="code" :label="$t('field.code')" min-width="150">
          <template #default="{ row }">
            <code class="code-badge">{{ row.code }}</code>
          </template>
        </ErpDataTableColumn>
        <ErpDataTableColumn v-if="canShow('description')" prop="description" :label="$t('field.description')" min-width="200" show-overflow-tooltip />
        <ErpDataTableColumn v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
              {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
            </el-tag>
          </template>
        </ErpDataTableColumn>
        <ErpDataTableColumn :label="$t('table.actions')" width="150" fixed="right" column-key="actions">
          <template #default="{ row }">
            <el-tooltip
              :disabled="canEditRole(row)"
              :content="editDisabledReason(row)"
              placement="top"
            >
              <el-button
                link
                type="primary"
                size="small"
                :disabled="!canEditRole(row)"
                @click="openEditModal(row)"
              >
                {{ $t('action.edit') }}
              </el-button>
            </el-tooltip>
            <el-tooltip
              :disabled="canDeleteRole(row)"
              :content="deleteDisabledReason(row)"
              placement="top"
            >
              <el-button
                link
                type="danger"
                size="small"
                :disabled="!canDeleteRole(row)"
                @click="handleDelete(row)"
              >
                {{ $t('action.delete') }}
              </el-button>
            </el-tooltip>
          </template>
        </ErpDataTableColumn>
        </ErpDataTable>
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
              <div class="permission-panel__tools">
                <el-tag type="info" effect="plain">
                  {{ currentPagePermissionSummary }}
                </el-tag>
                <el-tag type="success" effect="plain">
                  {{ allPermissionSummary }}
                </el-tag>
                <el-button size="small" type="primary" plain @click="selectAllPermissions">
                  全部权限全选
                </el-button>
                <el-button size="small" type="danger" plain @click="clearAllPermissions">
                  全部清空
                </el-button>
              </div>
            </div>

            <div class="permission-workspace">
              <div class="permission-tree-list">
                <el-input
                  v-model="permissionTreeSearch"
                  class="permission-tree-search"
                  placeholder="搜索菜单、权限名称或编码"
                  clearable
                />
                <div v-for="item in displayPermissionTreeData" :key="item.id" class="permission-tree-node">
                  <button
                    type="button"
                    class="permission-tree-label permission-tree-label--root"
                    :class="{ 'is-active': isTreeNodeActive(item) }"
                    @click.stop="handlePermissionTreeNodeClick(item)"
                  >
                    <el-checkbox
                      :model-value="isTreeNodeChecked(item)"
                      :indeterminate="isTreeNodeIndeterminate(item)"
                      @click.stop
                      @change="(checked: boolean) => toggleTreeNodePermissions(item, checked)"
                    />
                    <span v-if="item.icon" class="permission-tree-label__icon" v-html="item.icon"></span>
                    <span class="permission-tree-label__text">{{ item.label }}</span>
                    <span class="permission-tree-label__count">
                      {{ treeNodePermissionStats[item.id]?.selected || 0 }}/{{ treeNodePermissionStats[item.id]?.total || 0 }}
                    </span>
                    <span
                      v-if="item.children.length"
                      class="permission-tree-label__arrow"
                      :class="{ 'is-open': item.isOpen }"
                    >
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="9 18 15 12 9 6"></polyline>
                      </svg>
                    </span>
                  </button>

                  <div v-if="item.children.length && item.isOpen" class="permission-tree-children">
                    <template v-for="child in item.children" :key="child.id">
                      <button
                        type="button"
                        class="permission-tree-label permission-tree-label--child"
                        :class="{
                          'is-active': isTreeNodeActive(child),
                          'is-leaf': child.selectable && child.children.length === 0,
                        }"
                        @click.stop="handlePermissionTreeNodeClick(child)"
                      >
                        <el-checkbox
                          :model-value="isTreeNodeChecked(child)"
                          :indeterminate="isTreeNodeIndeterminate(child)"
                          @click.stop
                          @change="(checked: boolean) => toggleTreeNodePermissions(child, checked)"
                        />
                        <span
                          v-if="child.selectable && child.children.length === 0"
                          class="permission-tree-label__bullet"
                          :class="{ 'is-active': isTreeNodeActive(child) && child.selectable }"
                        ></span>
                        <span class="permission-tree-label__text">{{ child.label }}</span>
                        <span class="permission-tree-label__count">
                          {{ treeNodePermissionStats[child.id]?.selected || 0 }}/{{ treeNodePermissionStats[child.id]?.total || 0 }}
                        </span>
                        <span
                          v-if="child.children.length"
                          class="permission-tree-label__arrow"
                          :class="{ 'is-open': child.isOpen }"
                        >
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <polyline points="9 18 15 12 9 6"></polyline>
                          </svg>
                        </span>
                      </button>

                      <div v-if="child.children.length && child.isOpen" class="permission-tree-grandchildren">
                        <template v-for="grandChild in child.children" :key="grandChild.id">
                          <button
                            type="button"
                            class="permission-tree-label permission-tree-label--leaf"
                            :class="{
                              'is-active': isTreeNodeActive(grandChild),
                              'is-leaf': grandChild.selectable && grandChild.children.length === 0,
                            }"
                            @click.stop="handlePermissionTreeNodeClick(grandChild)"
                          >
                            <el-checkbox
                              :model-value="isTreeNodeChecked(grandChild)"
                              :indeterminate="isTreeNodeIndeterminate(grandChild)"
                              @click.stop
                              @change="(checked: boolean) => toggleTreeNodePermissions(grandChild, checked)"
                            />
                            <span
                              v-if="grandChild.selectable && grandChild.children.length === 0"
                              class="permission-tree-label__bullet"
                              :class="{ 'is-active': isTreeNodeActive(grandChild) }"
                            ></span>
                            <span class="permission-tree-label__text">{{ grandChild.label }}</span>
                            <span class="permission-tree-label__count">
                              {{ treeNodePermissionStats[grandChild.id]?.selected || 0 }}/{{ treeNodePermissionStats[grandChild.id]?.total || 0 }}
                            </span>
                            <span
                              v-if="grandChild.children.length"
                              class="permission-tree-label__arrow"
                              :class="{ 'is-open': grandChild.isOpen }"
                            >
                              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <polyline points="9 18 15 12 9 6"></polyline>
                              </svg>
                            </span>
                          </button>

                          <div
                            v-if="grandChild.children.length && grandChild.isOpen"
                            class="permission-tree-great-grandchildren"
                          >
                            <button
                              v-for="greatGrandChild in grandChild.children"
                              :key="greatGrandChild.id"
                              type="button"
                              class="permission-tree-label permission-tree-label--leaf permission-tree-label--deep"
                              :class="{ 'is-active': isTreeNodeActive(greatGrandChild) }"
                              @click.stop="handlePermissionTreeNodeClick(greatGrandChild)"
                            >
                              <el-checkbox
                                :model-value="isTreeNodeChecked(greatGrandChild)"
                                :indeterminate="isTreeNodeIndeterminate(greatGrandChild)"
                                @click.stop
                                @change="(checked: boolean) => toggleTreeNodePermissions(greatGrandChild, checked)"
                              />
                              <span
                                class="permission-tree-label__bullet"
                                :class="{ 'is-active': isTreeNodeActive(greatGrandChild) }"
                              ></span>
                              <span class="permission-tree-label__text">{{ greatGrandChild.label }}</span>
                              <span class="permission-tree-label__count">
                                {{ treeNodePermissionStats[greatGrandChild.id]?.selected || 0 }}/{{ treeNodePermissionStats[greatGrandChild.id]?.total || 0 }}
                              </span>
                            </button>
                          </div>
                        </template>
                      </div>
                    </template>
                  </div>
                </div>
                <div v-if="displayPermissionTreeData.length === 0" class="permission-tree-empty">
                  未找到匹配的权限目录
                </div>
              </div>

              <div v-if="pageKey" class="permission-checkbox-panel">
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
              <div v-else class="permission-checkbox-panel permission-checkbox-panel--empty">
              </div>
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
import { usePageSizePreference } from '@/composables/pageSizePreference';
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
const pageTreeOpenState = ref<Record<string, boolean>>({});
const treeMenus = ref<MenuItem[]>([]);
const selectedPermissionIds = ref<number[]>([]);
const originalPermissionIds = ref<number[]>([]);
const permissionTreeSearch = ref('');

const roleList = ref<Role[]>([]);
const permissionList = ref<Permission[]>([]);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const hasActivatedOnce = ref(false);
const pageSizeSyncReady = ref(false);
const pendingInitialLoad = ref(false);
const { bindPageSizeSync } = usePageSizePreference();
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
  { prefix: 'erp-assemble-order:', pageKeys: ['erp-assemble-order'] },
  { prefix: 'erp-disassemble-order:', pageKeys: ['erp-disassemble-order'] },
  { prefix: 'erp-assembly:', pageKeys: ['erp-assemble-order', 'erp-disassemble-order'] },
  { prefix: 'erp-disassemble:', pageKeys: ['erp-disassemble-order'] },
];

const menuPageKeyMap: Record<string, string[]> = {
  user: ['user-management'],
  users: ['user-management'],
  role: ['role-management'],
  roles: ['role-management'],
  permission: ['permission-management'],
  permissions: ['permission-management'],
  audit: ['audit-logs'],
  'audit-logs': ['audit-logs'],
  column: ['column-permissions'],
  'column-permissions': ['column-permissions'],
  columnPermissions: ['column-permissions'],
  menu: ['menu-management'],
  'menu-management': ['menu-management'],
  'system-config': ['system-configs'],
  tenant: ['tenant-management'],
  tenants: ['tenant-management'],
  'erp-product': ['erp-product'],
  'erp-product-fitment': ['erp-vehicle-brand', 'erp-vehicle-series', 'erp-vehicle-model', 'erp-product-fitment'],
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
  'erp-finance-customer-debt': ['erp-finance-customer-debt'],
  'erp-finance-summary': ['erp-finance-customer-debt'],
  'erp-finance-supplier-debt': ['erp-finance-supplier-debt'],
  'erp-ap': ['erp-ap'],
  'erp-receipt': ['erp-receipt'],
  'erp-payment': ['erp-payment'],
};

const menuExtraPageMap: Record<string, ExtraPageNode[]> = {};

const menuPathKeyMap: Record<string, string> = {
  '/users': 'user',
  '/roles': 'role',
  '/permissions': 'permission',
  '/audit-logs': 'audit',
  '/column-permissions': 'column',
  '/menus': 'menu',
  '/system-config': 'system-config',
  '/tenants': 'tenant',
  '/erp/products': 'erp-product',
  '/erp/vehicle-fitments': 'erp-product-fitment',
  '/erp/customers': 'erp-customer',
  '/erp/customer-categories': 'erp-customer-category',
  '/erp/suppliers': 'erp-supplier',
  '/erp/warehouses': 'erp-warehouse',
  '/erp/locations': 'erp-location',
  '/erp/categories': 'erp-category',
  '/erp/units': 'erp-unit',
  '/erp/settlement-methods': 'erp-settlement-method',
  '/erp/payment-methods': 'erp-payment-method',
  '/erp/receipt-methods': 'erp-receipt-method',
  '/erp/delivery-methods': 'erp-delivery-method',
  '/erp/print-templates': 'erp-print-template',
  '/erp/purchase-orders/draft': 'erp-purchase-draft',
  '/erp/purchase-orders/approved': 'erp-purchase-approved',
  '/erp/purchase-returns/draft': 'erp-purchase-return-draft',
  '/erp/purchase-returns/approved': 'erp-purchase-return-approved',
  '/erp/sale-orders/draft': 'erp-sale-draft',
  '/erp/sale-orders/approved': 'erp-sale-approved',
  '/erp/sale-returns/draft': 'erp-sale-return-draft',
  '/erp/sale-returns/approved': 'erp-sale-return-approved',
  '/erp/stocks': 'erp-stock',
  '/erp/stock-txns': 'erp-stock-txn',
  '/erp/stock-counts': 'erp-stock-count',
  '/erp/stock-transfers': 'erp-stock-transfer',
  '/erp/stock-inits': 'erp-stock-init',
  '/erp/stock-warnings': 'erp-stock-warning',
  '/erp/assemble-orders': 'erp-assemble-order',
  '/erp/disassemble-orders': 'erp-disassemble-order',
  '/erp/finance/customer-debts': 'erp-finance-customer-debt',
  '/erp/finance/supplier-debts': 'erp-finance-supplier-debt',
  '/erp/ar': 'erp-ar',
  '/erp/ap': 'erp-ap',
  '/erp/receipts': 'erp-receipt',
  '/erp/payments': 'erp-payment',
};

const menuTitleKeyMap: Record<string, string> = {
  '用户管理': 'user',
  '角色权限': 'role',
  '权限管理': 'permission',
  '审计日志': 'audit',
  '列权限配置': 'column',
  '菜单管理': 'menu',
  '系统配置': 'system-config',
  '租户管理': 'tenant',
  '商品管理': 'erp-product',
  '车型适配管理': 'erp-product-fitment',
  '客户管理': 'erp-customer',
  '客户类别': 'erp-customer-category',
  '供应商管理': 'erp-supplier',
  '仓库管理': 'erp-warehouse',
  '库位管理': 'erp-location',
  '分类管理': 'erp-category',
  '单位管理': 'erp-unit',
  '结算方式': 'erp-settlement-method',
  '付款方式': 'erp-payment-method',
  '收款方式': 'erp-receipt-method',
  '送货方式': 'erp-delivery-method',
  '打印模板': 'erp-print-template',
  '采购单（草稿）': 'erp-purchase-draft',
  '采购单（已审核）': 'erp-purchase-approved',
  '采购退货（草稿）': 'erp-purchase-return-draft',
  '采购退货（已审核）': 'erp-purchase-return-approved',
  '销售单（草稿）': 'erp-sale-draft',
  '销售单（已审核）': 'erp-sale-approved',
  '销售退货（草稿）': 'erp-sale-return-draft',
  '销售退货（已审核）': 'erp-sale-return-approved',
  '库存台账': 'erp-stock',
  '库存流水': 'erp-stock-txn',
  '库存调整': 'erp-stock-count',
  '库存移库': 'erp-stock-transfer',
  '初始库存': 'erp-stock-init',
  '库存预警': 'erp-stock-warning',
  '组装单': 'erp-assemble-order',
  '拆分单': 'erp-disassemble-order',
  '客户欠款': 'erp-finance-customer-debt',
  '供应商欠款': 'erp-finance-supplier-debt',
  '应收管理': 'erp-ar',
  '应付管理': 'erp-ap',
  '收款单': 'erp-receipt',
  '付款单': 'erp-payment',
};

const hiddenLegacyPermissionPrefixes = ['erp-purchase:', 'erp-sale:', 'erp-assembly:'];

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

const currentUserRoleCodes = computed(() => {
  const roles = (authStore.user as { roles?: Array<string | { code?: string }> } | null)?.roles;
  if (!Array.isArray(roles)) return [] as string[];
  return roles
    .map((item) => {
      if (typeof item === 'string') return item;
      return item?.code || '';
    })
    .filter((item) => Boolean(item))
    .map((item) => item.trim().toLowerCase());
});

const isCurrentUserRole = (role?: Role) => {
  if (!role?.code) return false;
  return currentUserRoleCodes.value.includes(role.code.trim().toLowerCase());
};

const canEditRole = (role?: Role) => {
  if (isCurrentUserRole(role)) return false;
  return !isReservedRole(role?.code) || canManageReservedRole(role?.code);
};

const canDeleteRole = (role?: Role) => {
  if (isCurrentUserRole(role)) return false;
  return !isReservedRole(role?.code);
};

const editDisabledReason = (role?: Role) => {
  if (isCurrentUserRole(role)) {
    return '不能修改当前登录账号所属角色';
  }
  const normalized = (role?.code || '').trim().toLowerCase();
  if (!reservedRoleCodes.has(normalized)) {
    return '';
  }
  if (normalized === 'super_admin') {
    return '仅系统超级管理员可维护 super_admin 角色权限';
  }
  return '仅系统超级管理员可维护 admin 角色权限';
};

const deleteDisabledReason = (role?: Role) => {
  if (isCurrentUserRole(role)) {
    return '不能修改当前登录账号所属角色';
  }
  if (isReservedRole(role?.code)) {
    return '保留角色不允许通过角色管理接口删除';
  }
  return '';
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
  'erp-vehicle-fitment': t('page.erpVehicleFitmentManagement'),
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

const resolveSelectedPermissionPageKeys = (key: string) => {
  const mappedKeys = menuPageKeyMap[key] || [];
  if (mappedKeys.length === 0) {
    return [key];
  }
  const availablePageKeys = new Set(pageOptions.value.map((item) => item.key));
  const visibleMappedKeys = mappedKeys.filter((mappedKey) => availablePageKeys.has(mappedKey));
  return visibleMappedKeys.length > 0 ? visibleMappedKeys : [key];
};

const assignablePermissionSource = computed(() => {
  return canUsePlatformPermissions.value
    ? permissionList.value
    : permissionList.value.filter(
      p => !p.code.startsWith('tenant:') && !p.code.startsWith('system-config:')
    );
});

const baseVisiblePermissionList = computed(() => {
  return assignablePermissionSource.value
    .filter((item) => !isConcreteColumnPermission(item.code))
    .flatMap((item) => resolvePermissionPageKeys(item.code).map((itemPageKey) => ({
      ...item,
      pageKey: itemPageKey,
    })));
});

const menuBoundPermissionList = computed(() => {
  const permissionByCode = new Map(
    assignablePermissionSource.value
      .filter((item) => !isConcreteColumnPermission(item.code))
      .map((item) => [item.code, item]),
  );
  const existingPairs = new Set(
    baseVisiblePermissionList.value.map((item) => `${item.id}:${item.pageKey || ''}`),
  );
  const entries: Permission[] = [];

  const walkMenus = (items: MenuItem[]) => {
    items.forEach((item) => {
      const menuKey = item.key || '';
      const mappedPageKeys = menuPageKeyMap[menuKey] || [];
      const permission = item.permissionCode ? permissionByCode.get(item.permissionCode) : undefined;
      if (permission) {
        mappedPageKeys.forEach((mappedPageKey) => {
          const pairKey = `${permission.id}:${mappedPageKey}`;
          if (!existingPairs.has(pairKey)) {
            existingPairs.add(pairKey);
            entries.push({
              ...permission,
              pageKey: mappedPageKey,
            });
          }
        });
      }
      if (item.children && item.children.length > 0) {
        walkMenus(item.children);
      }
    });
  };

  walkMenus(treeMenus.value);
  return entries;
});

const visiblePermissionList = computed(() => [
  ...baseVisiblePermissionList.value,
  ...menuBoundPermissionList.value,
]);

const containsTreeSelection = (node: PageTreeNode): boolean => {
  if (node.pageKey === pageKey.value) return true;
  return node.children.some((child) => containsTreeSelection(child));
};

const resolveTreeNodeOpen = (nodeId: string, defaultOpen: boolean): boolean => {
  const stored = pageTreeOpenState.value[nodeId];
  return stored === undefined ? defaultOpen : stored;
};

const buildPageLeafNodes = (
  parentId: string,
  label: string,
  mappedKeys: string[],
  collapseSingleLeaf = true,
): PageTreeNode[] => {
  const matched = mappedKeys
    .map((mappedKey) => pageOptionsMap.value.get(mappedKey))
    .filter((item): item is PageOption => Boolean(item));

  if (matched.length === 0) {
    return [];
  }

  if (matched.length === 1 && collapseSingleLeaf) {
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
  const mappedPageKeys = menuPageKeyMap[menuKey] || [];
  const mappedNodes = buildPageLeafNodes(
    nodeId,
    item.title || '',
    mappedPageKeys,
    mappedPageKeys.length <= 1,
  );
  const extraNodes = buildExtraPageLeafNodes(nodeId, menuKey);
  const childNodes = (item.children || [])
    .map((child) => buildTreeNode(child, nodeId))
    .filter((child): child is PageTreeNode => Boolean(child));

  const firstMappedNode = mappedNodes[0];
  const onlyDirectLeaf = mappedNodes.length === 1 && Boolean(firstMappedNode?.pageKey) && childNodes.length === 0;
  const onlyMappedGroup = mappedNodes.length === 1
    && !firstMappedNode?.pageKey
    && firstMappedNode?.label === (item.title || '')
    && extraNodes.length === 0
    && childNodes.length === 0;
  const directPageKey = onlyDirectLeaf
    ? firstMappedNode?.pageKey
    : onlyMappedGroup
      ? menuKey
      : undefined;
  const children = onlyDirectLeaf
    ? []
    : [
      ...(onlyMappedGroup ? (firstMappedNode?.children || []) : mappedNodes),
      ...extraNodes,
      ...childNodes,
    ];

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

const nodeMatchesSearch = (node: PageTreeNode, keyword: string) => {
  const normalized = keyword.toLowerCase();
  const pageKeys = collectTreeNodePageKeys(node);
  const matchedPermission = visiblePermissionList.value.some((permission) => {
    return pageKeys.includes(permission.pageKey || '')
      && `${permission.name} ${permission.code}`.toLowerCase().includes(normalized);
  });
  return node.label.toLowerCase().includes(normalized) || matchedPermission;
};

const filterTreeBySearch = (nodes: PageTreeNode[], keyword: string): PageTreeNode[] => {
  const normalized = keyword.trim().toLowerCase();
  if (!normalized) {
    return nodes;
  }

  return nodes
    .map<PageTreeNode | null>((node) => {
      const children = filterTreeBySearch(node.children, normalized);
      if (!nodeMatchesSearch(node, normalized) && children.length === 0) {
        return null;
      }
      return {
        ...node,
        isOpen: children.length > 0 ? true : node.isOpen,
        children,
      };
    })
    .filter((node): node is PageTreeNode => Boolean(node));
};

const displayPermissionTreeData = computed(() => {
  return filterTreeBySearch(pageTreeData.value, permissionTreeSearch.value);
});

const isTreeNodeActive = (node: PageTreeNode): boolean => {
  const resolvedPageKey = node.pageKey || resolveFallbackPageKeyForNode(node);
  if (resolvedPageKey && resolvedPageKey === pageKey.value) return true;
  return node.children.some((child) => isTreeNodeActive(child));
};
const resolveFallbackPageKeyForNode = (node: PageTreeNode): string => {
  if (node.children.length > 0) {
    return '';
  }
  const normalizedLabel = (node.label || '').trim();
  const menuKey = menuTitleKeyMap[normalizedLabel];
  if (!menuKey) {
    return '';
  }
  const mappedPageKeys = menuPageKeyMap[menuKey] || [];
  return mappedPageKeys.find((key) => pageOptionsMap.value.has(key)) || '';
};

const handlePermissionTreeNodeClick = (node: PageTreeNode) => {
  const nextOpenState = node.children.length > 0 ? !node.isOpen : undefined;
  const resolvedPageKey = node.pageKey || resolveFallbackPageKeyForNode(node);
  if (resolvedPageKey) {
    pageKey.value = resolvedPageKey;
    if (nextOpenState !== undefined) {
      pageTreeOpenState.value = {
        ...pageTreeOpenState.value,
        [node.id]: nextOpenState,
      };
    }
    return;
  }

  if (nextOpenState !== undefined) {
    pageTreeOpenState.value = {
      ...pageTreeOpenState.value,
      [node.id]: nextOpenState,
    };
  }
};

const collectTreeNodePageKeys = (node: PageTreeNode): string[] => {
  const resolvedPageKey = node.pageKey || resolveFallbackPageKeyForNode(node);
  const selfKeys = resolvedPageKey ? resolveSelectedPermissionPageKeys(resolvedPageKey) : [];
  const childKeys = node.children.flatMap((child) => collectTreeNodePageKeys(child));
  return Array.from(new Set([...selfKeys, ...childKeys]));
};

const collectTreeNodePermissionIds = (node: PageTreeNode): number[] => {
  const pageKeys = new Set(collectTreeNodePageKeys(node));
  return Array.from(new Set(
    visiblePermissionList.value
      .filter((permission) => pageKeys.has(permission.pageKey || ''))
      .map((permission) => permission.id),
  ));
};

const isTreeNodeChecked = (node: PageTreeNode) => {
  const ids = collectTreeNodePermissionIds(node);
  if (ids.length === 0) {
    return false;
  }
  const selected = new Set(selectedPermissionIds.value);
  return ids.every((id) => selected.has(id));
};

const isTreeNodeIndeterminate = (node: PageTreeNode) => {
  const ids = collectTreeNodePermissionIds(node);
  if (ids.length === 0) {
    return false;
  }
  const selected = new Set(selectedPermissionIds.value);
  const selectedCount = ids.filter((id) => selected.has(id)).length;
  return selectedCount > 0 && selectedCount < ids.length;
};

const toggleTreeNodePermissions = (node: PageTreeNode, checked: string | number | boolean) => {
  const ids = collectTreeNodePermissionIds(node);
  if (ids.length === 0) {
    return;
  }
  const next = new Set(selectedPermissionIds.value);
  if (Boolean(checked)) {
    ids.forEach((id) => next.add(id));
  } else {
    ids.forEach((id) => next.delete(id));
  }
  selectedPermissionIds.value = Array.from(next);
};

const currentPagePermissions = computed(() => {
  if (!pageKey.value) {
    return [] as Permission[];
  }
  const activePageKeys = new Set(resolveSelectedPermissionPageKeys(pageKey.value));
  const seen = new Set<number>();
  return visiblePermissionList.value.filter((item) => {
    if (!activePageKeys.has(item.pageKey || '')) {
      return false;
    }
    if (seen.has(item.id)) {
      return false;
    }
    seen.add(item.id);
    return true;
  });
});

const currentPagePermissionSummary = computed(() => {
  if (!pageKey.value) {
    return t('table.empty');
  }
  const currentIds = new Set(currentPagePermissions.value.map((item) => item.id));
  const selectedCount = selectedPermissionIds.value.filter((id) => currentIds.has(id)).length;
  return `${selectedPageLabel.value}: ${selectedCount}/${currentPagePermissions.value.length}`;
});

const visiblePermissionIds = computed(() => {
  return Array.from(new Set(visiblePermissionList.value.map((item) => item.id)));
});

const selectedVisiblePermissionCount = computed(() => {
  const visibleIds = new Set(visiblePermissionIds.value);
  return selectedPermissionIds.value.filter((id) => visibleIds.has(id)).length;
});

const allPermissionSummary = computed(() => {
  return `全部: ${selectedVisiblePermissionCount.value}/${visiblePermissionIds.value.length}`;
});

const pagePermissionStats = computed<Record<string, { selected: number; total: number }>>(() => {
  const selectedIds = new Set(selectedPermissionIds.value);
  return pageOptions.value.reduce<Record<string, { selected: number; total: number }>>((acc, item) => {
    const ids = Array.from(new Set(
      visiblePermissionList.value
        .filter((permission) => permission.pageKey === item.key)
        .map((permission) => permission.id),
    ));
    acc[item.key] = {
      selected: ids.filter((id) => selectedIds.has(id)).length,
      total: ids.length,
    };
    return acc;
  }, {});
});

const treeNodePermissionStats = computed<Record<string, { selected: number; total: number }>>(() => {
  const collectPageKeys = (node: PageTreeNode): string[] => {
    const resolvedPageKey = node.pageKey || resolveFallbackPageKeyForNode(node);
    const childKeys = node.children.flatMap((child) => collectPageKeys(child));
    return Array.from(new Set([resolvedPageKey, ...childKeys].filter(Boolean)));
  };

  const stats: Record<string, { selected: number; total: number }> = {};
  const walk = (node: PageTreeNode) => {
    const nodeStats = collectPageKeys(node).reduce(
      (acc, key) => {
        const current = pagePermissionStats.value[key];
        if (!current) {
          return acc;
        }
        acc.selected += current.selected;
        acc.total += current.total;
        return acc;
      },
      { selected: 0, total: 0 },
    );
    stats[node.id] = nodeStats;
    node.children.forEach(walk);
  };

  pageTreeData.value.forEach(walk);
  return stats;
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
      resetUnavailableSelectedPage();
    }
  } catch (error) {
    notifyError(error);
  }
};

const resolveTreeMenuKey = (item: any): string => {
  const candidates = [
    item?.code,
    item?.key,
    item?.i18nKey,
    item?.path ? menuPathKeyMap[String(item.path).trim()] : '',
    item?.title ? menuTitleKeyMap[String(item.title).trim()] : '',
  ];
  const matched = candidates.find((value) => typeof value === 'string' && value.trim().length > 0);
  return matched ? String(matched).trim() : '';
};

const normalizeTreeMenu = (item: any): MenuItem => {
  return {
    id: Number(item.id),
    key: resolveTreeMenuKey(item),
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
      resetUnavailableSelectedPage();
      return;
    }
    await menuStore.fetchMenus();
    treeMenus.value = decorateTreeMenus(menuStore.menus);
    resetUnavailableSelectedPage();
  } catch (error) {
    notifyError(error);
  }
};

const resetUnavailableSelectedPage = () => {
  if (!pageKey.value) {
    return;
  }
  const available = new Set(pageOptions.value.map((item) => item.key));
  const mappedKeys = menuPageKeyMap[pageKey.value] || [];
  const hasMappedPermission = mappedKeys.some((key) => available.has(key));
  if (available.has(pageKey.value) || hasMappedPermission) {
    return;
  }
  pageKey.value = '';
};

onMounted(() => {
  fetchPermissions();
  loadTreeMenus();
  if (pageSizeSyncReady.value) {
    fetchRoles();
  } else {
    pendingInitialLoad.value = true;
  }
  fetchTenantKeys();
});

onActivated(() => {
  if (!hasActivatedOnce.value) {
    hasActivatedOnce.value = true;
    return;
  }
  fetchRoles();
  fetchPermissions();
  loadTreeMenus();
});

bindPageSizeSync(size, fetchRoles, {
  reloadOnInitialSync: false,
  onInitialSyncComplete: () => {
    pageSizeSyncReady.value = true;
    if (pendingInitialLoad.value) {
      pendingInitialLoad.value = false;
      fetchRoles();
    }
  }
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
  permissionTreeSearch.value = '';
  selectedPermissionIds.value = [];
  originalPermissionIds.value = [];
  pageKey.value = '';
};

const openEditModal = async (row: Role) => {
  if (!canEditRole(row)) {
    notifyWarning(editDisabledReason(row));
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
  permissionTreeSearch.value = '';
  selectedPermissionIds.value = [];
  originalPermissionIds.value = [];
  pageKey.value = '';

  // 获取该角色的权限列表
  try {
    const res: any = await request.get(`/roles/${row.id}/permissions`);
    if (res.data.code === 200) {
      selectedPermissionIds.value = res.data.data
        .filter((p: any) => !isConcreteColumnPermission(String(p.code || '')))
        .map((p: any) => p.id);
      originalPermissionIds.value = [...selectedPermissionIds.value];
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
  originalPermissionIds.value = [];
  permissionTreeSearch.value = '';
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
    const confirmed = await confirmPermissionChanges();
    if (!confirmed) {
      return;
    }

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
  if (!canDeleteRole(row)) {
    notifyWarning(deleteDisabledReason(row));
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

const selectAllPermissions = () => {
  const next = new Set(selectedPermissionIds.value);
  visiblePermissionIds.value.forEach((id) => next.add(id));
  selectedPermissionIds.value = Array.from(next);
};

const clearAllPermissions = () => {
  const visibleIds = new Set(visiblePermissionIds.value);
  selectedPermissionIds.value = selectedPermissionIds.value.filter((id) => !visibleIds.has(id));
};

const formatPermissionChangeLines = (ids: number[]) => {
  const permissionMap = new Map(visiblePermissionList.value.map((permission) => [permission.id, permission]));
  return ids.slice(0, 8).map((id) => {
    const permission = permissionMap.get(id);
    return permission ? `${permission.name}（${permission.code}）` : `权限 ID ${id}`;
  });
};

const confirmPermissionChanges = async () => {
  const before = new Set(originalPermissionIds.value);
  const after = new Set(selectedPermissionIds.value);
  const added = Array.from(after).filter((id) => !before.has(id));
  const removed = Array.from(before).filter((id) => !after.has(id));

  if (added.length === 0 && removed.length === 0) {
    return true;
  }

  const addedLines = formatPermissionChangeLines(added);
  const removedLines = formatPermissionChangeLines(removed);
  const message = [
    `本次将新增 ${added.length} 个权限，移除 ${removed.length} 个权限。`,
    addedLines.length ? `新增示例：\n${addedLines.join('\n')}${added.length > addedLines.length ? '\n...' : ''}` : '',
    removedLines.length ? `移除示例：\n${removedLines.join('\n')}${removed.length > removedLines.length ? '\n...' : ''}` : '',
    '确认保存这些权限变更吗？',
  ].filter(Boolean).join('\n\n');

  try {
    await ElMessageBox.confirm(message, '权限变更预览', {
      confirmButtonText: t('action.confirm'),
      cancelButtonText: t('action.cancel'),
      type: removed.length > 0 ? 'warning' : 'info',
    });
    return true;
  } catch {
    return false;
  }
};

watch(pageOptions, () => {
  resetUnavailableSelectedPage();
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
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.permission-panel__tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.permission-workspace {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 12px;
  min-height: 360px;
}

.permission-tree-list {
  max-height: 420px;
  overflow-y: auto;
  padding: 6px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.permission-tree-search {
  margin-bottom: 4px;
}

.permission-tree-empty {
  padding: 18px 10px;
  color: #909399;
  font-size: 13px;
  text-align: center;
}

.permission-tree-node,
.permission-tree-children,
.permission-tree-grandchildren,
.permission-tree-great-grandchildren {
  display: flex;
  flex-direction: column;
}

.permission-tree-children {
  padding: 4px 0 8px 12px;
}

.permission-tree-grandchildren {
  padding: 4px 0 4px 16px;
}

.permission-tree-great-grandchildren {
  padding: 3px 0 3px 16px;
}

.permission-tree-label {
  width: 100%;
  border: 0;
  background: transparent;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

:deep(.permission-tree-label .el-checkbox) {
  height: 16px;
  margin-right: 0;
}

.permission-tree-label:hover {
  background: #eef5ff;
}

.permission-tree-label__icon {
  display: flex;
  margin-right: 2px;
  opacity: 0.8;
}

.permission-tree-label__text {
  min-width: 0;
  flex: 1 1 auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.permission-tree-label__count {
  flex: 0 0 auto;
  color: #909399;
  font-size: 12px;
}

.permission-tree-label__arrow {
  width: 14px;
  height: 14px;
  color: #909399;
  flex: 0 0 auto;
  transition: transform 0.2s ease;
}

.permission-tree-label__arrow.is-open {
  transform: rotate(90deg);
}

.permission-tree-label__bullet {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: #909399;
  flex: 0 0 auto;
  opacity: 0.5;
}

.permission-tree-label__bullet.is-active {
  background: var(--el-color-primary);
  opacity: 1;
}

.permission-tree-label--root {
  min-height: 40px;
  padding: 0 10px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
}

.permission-tree-label--child {
  min-height: 34px;
  padding: 7px 9px;
  border-radius: 9px;
  font-size: 13px;
  color: #555;
}

.permission-tree-label--leaf {
  min-height: 32px;
  padding: 6px 9px;
  border-radius: 9px;
  font-size: 12.5px;
  color: #666;
}

.permission-tree-label--deep {
  color: #707780;
}

.permission-tree-label.is-active {
  color: var(--el-color-primary);
  background: rgba(64, 158, 255, 0.14);
}

.permission-tree-label.is-active .permission-tree-label__count {
  color: var(--el-color-primary);
}

.permission-checkbox-panel {
  max-height: 420px;
  overflow-y: auto;
  padding: 4px 2px;
}

.permission-checkbox-panel--empty {
  border: 1px dashed #dcdfe6;
  border-radius: 10px;
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

  .permission-workspace {
    grid-template-columns: 220px minmax(0, 1fr);
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

  .permission-workspace {
    grid-template-columns: 1fr;
  }

  .permission-tree-list {
    max-height: 220px;
  }
}
</style>
