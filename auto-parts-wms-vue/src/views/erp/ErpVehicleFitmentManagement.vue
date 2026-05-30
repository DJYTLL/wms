<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpVehicleFitmentManagement') }}</div>
      <div class="page-toolbar-card page-toolbar-card--tabs">
        <el-tabs v-model="activeTab" class="vehicle-tabs">
          <el-tab-pane :label="$t('field.vehicleBrand')" name="brands">
            <div class="erp-basic-toolbar erp-basic-toolbar--fixed-actions vehicle-tab-toolbar">
              <div class="erp-basic-filters erp-basic-filters--3">
            <el-input
              v-model="brandNameQuery"
              placeholder="名称"
              class="table-search erp-basic-field--narrow"
              clearable
              @clear="handleBrandSearch"
              @keyup.enter="handleBrandSearch"
            />
            <el-input
              v-model="brandCodeQuery"
              placeholder="编码"
              class="table-search erp-basic-field--narrow"
              clearable
              @clear="handleBrandSearch"
              @keyup.enter="handleBrandSearch"
            />
            <el-select v-model="brandStatus" :placeholder="$t('field.status')" class="table-search erp-basic-field--narrow" @change="handleBrandSearch">
              <el-option :label="$t('filter.all')" value="all" />
              <el-option :label="$t('status.active')" value="enabled" />
              <el-option :label="$t('status.inactive')" value="disabled" />
            </el-select>
              </div>
              <div class="erp-basic-actions">
                <el-button type="primary" @click="handleBrandSearch">
                  {{ $t('action.search') }}
                </el-button>
                <el-button type="primary" v-permission="'erp-vehicle-brand:add'" @click="openBrandAdd">
                  {{ $t('action.add') }}
                </el-button>
              </div>
            </div>

        <div class="table-card">
          <div class="table-body">
            <ErpDataTable :data="brandTable" style="width: 100%" stripe v-loading="brandLoading" :empty-text="$t('table.empty')" table-key="erp-vehicle-fitment-brands">
              <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
              <ErpDataTableColumn v-if="canShowBrand('code')" prop="code" :label="$t('field.code')" min-width="120" />
              <ErpDataTableColumn v-if="canShowBrand('name')" prop="name" :label="$t('field.name')" min-width="160" />
              <ErpDataTableColumn v-if="canShowBrand('enabled')" prop="enabled" :label="$t('field.status')" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                    {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
                  </el-tag>
                </template>
              </ErpDataTableColumn>
              <ErpDataTableColumn v-if="canShowBrand('remark')" prop="remark" :label="$t('field.remark')" min-width="160" show-overflow-tooltip />
              <ErpDataTableColumn :label="$t('table.actions')" width="160" fixed="right" column-key="actions">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" v-permission="'erp-vehicle-brand:edit'" @click="openBrandEdit(row)">
                    {{ $t('action.edit') }}
                  </el-button>
                  <el-button link type="danger" size="small" v-permission="'erp-vehicle-brand:delete'" @click="handleBrandDelete(row)">
                    {{ $t('action.delete') }}
                  </el-button>
                </template>
              </ErpDataTableColumn>
            </ErpDataTable>
          </div>
          <div class="table-pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="brandTotal"
              :current-page="brandPage"
              :page-size="brandSize"
              :page-sizes="[10, 20, 50, 100]"
              @size-change="handleBrandSizeChange"
              @current-change="handleBrandPageChange"
            />
          </div>
        </div>
          </el-tab-pane>
          <el-tab-pane :label="$t('field.vehicleSeries')" name="series">
            <div class="erp-basic-toolbar erp-basic-toolbar--fixed-actions vehicle-tab-toolbar">
              <div class="erp-basic-filters erp-basic-filters--4">
            <el-input
              v-model="seriesNameQuery"
              placeholder="名称"
              class="table-search erp-basic-field--narrow"
              clearable
              @clear="handleSeriesSearch"
              @keyup.enter="handleSeriesSearch"
            />
            <el-input
              v-model="seriesCodeQuery"
              placeholder="编码"
              class="table-search erp-basic-field--narrow"
              clearable
              @clear="handleSeriesSearch"
              @keyup.enter="handleSeriesSearch"
            />
            <el-select v-model="seriesBrandFilter" :placeholder="$t('field.vehicleBrand')" class="table-search erp-basic-field--narrow" clearable @change="handleSeriesSearch">
              <el-option v-for="item in brandOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
            <el-select v-model="seriesStatus" :placeholder="$t('field.status')" class="table-search erp-basic-field--narrow" @change="handleSeriesSearch">
              <el-option :label="$t('filter.all')" value="all" />
              <el-option :label="$t('status.active')" value="enabled" />
              <el-option :label="$t('status.inactive')" value="disabled" />
            </el-select>
              </div>
              <div class="erp-basic-actions">
                <el-button type="primary" @click="handleSeriesSearch">
                  {{ $t('action.search') }}
                </el-button>
                <el-button type="primary" v-permission="'erp-vehicle-series:add'" @click="openSeriesAdd">
                  {{ $t('action.add') }}
                </el-button>
              </div>
            </div>

        <div class="table-card">
          <div class="table-body">
            <ErpDataTable :data="seriesTable" style="width: 100%" stripe v-loading="seriesLoading" :empty-text="$t('table.empty')" table-key="erp-vehicle-fitment-series">
              <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
              <ErpDataTableColumn v-if="canShowSeries('code')" prop="code" :label="$t('field.code')" min-width="120" />
              <ErpDataTableColumn v-if="canShowSeries('brand')" :label="$t('field.vehicleBrand')" min-width="160" column-key="brand">
                <template #default="{ row }">
                  {{ getBrandName(row.brandId) }}
                </template>
              </ErpDataTableColumn>
              <ErpDataTableColumn v-if="canShowSeries('name')" prop="name" :label="$t('field.name')" min-width="160" />
              <ErpDataTableColumn v-if="canShowSeries('enabled')" prop="enabled" :label="$t('field.status')" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                    {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
                  </el-tag>
                </template>
              </ErpDataTableColumn>
              <ErpDataTableColumn v-if="canShowSeries('remark')" prop="remark" :label="$t('field.remark')" min-width="160" show-overflow-tooltip />
              <ErpDataTableColumn :label="$t('table.actions')" width="160" fixed="right" column-key="actions">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" v-permission="'erp-vehicle-series:edit'" @click="openSeriesEdit(row)">
                    {{ $t('action.edit') }}
                  </el-button>
                  <el-button link type="danger" size="small" v-permission="'erp-vehicle-series:delete'" @click="handleSeriesDelete(row)">
                    {{ $t('action.delete') }}
                  </el-button>
                </template>
              </ErpDataTableColumn>
            </ErpDataTable>
          </div>
          <div class="table-pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="seriesTotal"
              :current-page="seriesPage"
              :page-size="seriesSize"
              :page-sizes="[10, 20, 50, 100]"
              @size-change="handleSeriesSizeChange"
              @current-change="handleSeriesPageChange"
            />
          </div>
        </div>
          </el-tab-pane>
          <el-tab-pane :label="$t('field.vehicleModel')" name="models">
            <div class="erp-basic-toolbar erp-basic-toolbar--fixed-actions vehicle-tab-toolbar">
              <div class="erp-basic-filters erp-basic-filters--4">
            <el-input
              v-model="modelNameQuery"
              placeholder="名称"
              class="table-search erp-basic-field--narrow"
              clearable
              @clear="handleModelSearch"
              @keyup.enter="handleModelSearch"
            />
            <el-input
              v-model="modelCodeQuery"
              placeholder="编码"
              class="table-search erp-basic-field--narrow"
              clearable
              @clear="handleModelSearch"
              @keyup.enter="handleModelSearch"
            />
            <el-select v-model="modelSeriesFilter" :placeholder="$t('field.vehicleSeries')" class="table-search erp-basic-field--narrow" clearable @change="handleModelSearch">
              <el-option
                v-for="item in seriesOptions"
                :key="item.id"
                :label="formatSeriesLabel(item)"
                :value="item.id"
              />
            </el-select>
            <el-select v-model="modelStatus" :placeholder="$t('field.status')" class="table-search erp-basic-field--narrow" @change="handleModelSearch">
              <el-option :label="$t('filter.all')" value="all" />
              <el-option :label="$t('status.active')" value="enabled" />
              <el-option :label="$t('status.inactive')" value="disabled" />
            </el-select>
              </div>
              <div class="erp-basic-actions">
                <el-button type="primary" @click="handleModelSearch">
                  {{ $t('action.search') }}
                </el-button>
                <el-button type="primary" v-permission="'erp-vehicle-model:add'" @click="openModelAdd">
                  {{ $t('action.add') }}
                </el-button>
              </div>
            </div>

        <div class="table-card">
          <div class="table-body">
            <ErpDataTable :data="modelTable" style="width: 100%" stripe v-loading="modelLoading" :empty-text="$t('table.empty')" table-key="erp-vehicle-fitment-models">
              <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
              <ErpDataTableColumn v-if="canShowModel('code')" prop="code" :label="$t('field.code')" min-width="120" />
              <ErpDataTableColumn v-if="canShowModel('series')" :label="$t('field.vehicleSeries')" min-width="180" column-key="series">
                <template #default="{ row }">
                  {{ getSeriesLabel(row.seriesId) }}
                </template>
              </ErpDataTableColumn>
              <ErpDataTableColumn v-if="canShowModel('name')" prop="name" :label="$t('field.name')" min-width="160" />
              <ErpDataTableColumn v-if="canShowModel('yearFrom')" :label="$t('field.yearFrom')" width="100" column-key="yearFrom">
                <template #default="{ row }">
                  {{ row.yearFrom || '-' }}
                </template>
              </ErpDataTableColumn>
              <ErpDataTableColumn v-if="canShowModel('yearTo')" :label="$t('field.yearTo')" width="100" column-key="yearTo">
                <template #default="{ row }">
                  {{ row.yearTo || '-' }}
                </template>
              </ErpDataTableColumn>
              <ErpDataTableColumn v-if="canShowModel('displacement')" prop="displacement" :label="$t('field.displacement')" min-width="120" />
              <ErpDataTableColumn v-if="canShowModel('engine')" prop="engine" :label="$t('field.engine')" min-width="140" />
              <ErpDataTableColumn v-if="canShowModel('enabled')" prop="enabled" :label="$t('field.status')" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                    {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
                  </el-tag>
                </template>
              </ErpDataTableColumn>
              <ErpDataTableColumn v-if="canShowModel('remark')" prop="remark" :label="$t('field.remark')" min-width="160" show-overflow-tooltip />
              <ErpDataTableColumn :label="$t('table.actions')" width="160" fixed="right" column-key="actions">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" v-permission="'erp-vehicle-model:edit'" @click="openModelEdit(row)">
                    {{ $t('action.edit') }}
                  </el-button>
                  <el-button link type="danger" size="small" v-permission="'erp-vehicle-model:delete'" @click="handleModelDelete(row)">
                    {{ $t('action.delete') }}
                  </el-button>
                </template>
              </ErpDataTableColumn>
            </ErpDataTable>
          </div>
          <div class="table-pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="modelTotal"
              :current-page="modelPage"
              :page-size="modelSize"
              :page-sizes="[10, 20, 50, 100]"
              @size-change="handleModelSizeChange"
              @current-change="handleModelPageChange"
            />
          </div>
        </div>
          </el-tab-pane>
          <el-tab-pane :label="$t('field.productFitment')" name="fitments">
            <div class="erp-basic-toolbar erp-basic-toolbar--fixed-actions vehicle-tab-toolbar">
              <div class="erp-basic-filters erp-basic-filters--2">
            <el-select v-model="fitmentProductFilter" :placeholder="$t('field.product')" class="table-search erp-basic-field--wide" clearable @change="fetchFitmentList">
              <el-option
                v-for="item in productOptions"
                :key="item.id"
                :label="formatProductLabel(item)"
                :value="item.id"
              />
            </el-select>
            <el-select v-model="fitmentModelFilter" :placeholder="$t('field.vehicleModel')" class="table-search erp-basic-field--narrow" clearable @change="fetchFitmentList">
              <el-option
                v-for="item in modelOptions"
                :key="item.id"
                :label="formatModelLabel(item)"
                :value="item.id"
              />
            </el-select>
              </div>
              <div class="erp-basic-actions">
                <el-button type="primary" v-permission="'erp-product-fitment:add'" @click="openFitmentAdd">
                  {{ $t('action.add') }}
                </el-button>
              </div>
            </div>

        <div class="table-card">
          <div class="table-body">
            <ErpDataTable :data="fitmentTable" style="width: 100%" stripe v-loading="fitmentLoading" :empty-text="$t('table.empty')" table-key="erp-vehicle-fitment-products">
              <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
              <ErpDataTableColumn v-if="canShowFitment('product')" :label="$t('field.product')" min-width="200" column-key="product">
                <template #default="{ row }">
                  {{ getProductLabel(row.productId) }}
                </template>
              </ErpDataTableColumn>
              <ErpDataTableColumn v-if="canShowFitment('vehicleModel')" :label="$t('field.vehicleModel')" min-width="240" column-key="vehicleModel">
                <template #default="{ row }">
                  {{ getModelLabel(row.modelId) }}
                </template>
              </ErpDataTableColumn>
              <ErpDataTableColumn v-if="canShowFitment('remark')" prop="remark" :label="$t('field.remark')" min-width="200" show-overflow-tooltip />
              <ErpDataTableColumn :label="$t('table.actions')" width="160" fixed="right" column-key="actions">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" v-permission="'erp-product-fitment:edit'" @click="openFitmentEdit(row)">
                    {{ $t('action.edit') }}
                  </el-button>
                  <el-button link type="danger" size="small" v-permission="'erp-product-fitment:delete'" @click="handleFitmentDelete(row)">
                    {{ $t('action.delete') }}
                  </el-button>
                </template>
              </ErpDataTableColumn>
            </ErpDataTable>
          </div>
        </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <el-dialog v-model="showBrandModal" :title="brandEditing ? $t('action.edit') : $t('action.add')" width="520px" append-to-body @closed="resetBrandForm">
      <el-form :model="brandForm" label-width="110px">
        <el-form-item :label="$t('field.code')" required>
          <el-input v-model="brandForm.code" :placeholder="$t('placeholder.autoGenerated')" :disabled="!brandEditing" />
        </el-form-item>
        <el-form-item :label="$t('field.name')" required>
          <el-input v-model="brandForm.name" />
        </el-form-item>
        <el-form-item :label="$t('field.status')">
          <el-switch v-model="brandForm.enabled" />
        </el-form-item>
        <el-form-item :label="$t('field.remark')">
          <el-input v-model="brandForm.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBrandModal = false">{{ $t('action.cancel') }}</el-button>
        <el-button type="primary" @click="saveBrand">{{ $t('action.save') }}</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="showSeriesModal" :title="seriesEditing ? $t('action.edit') : $t('action.add')" width="560px" append-to-body @closed="resetSeriesForm">
      <el-form :model="seriesForm" label-width="110px">
        <el-form-item :label="$t('field.vehicleBrand')" required>
          <el-select v-model="seriesForm.brandId" clearable style="width: 100%">
            <el-option v-for="item in brandOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('field.code')" required>
          <el-input v-model="seriesForm.code" :placeholder="$t('placeholder.autoGenerated')" :disabled="!seriesEditing" />
        </el-form-item>
        <el-form-item :label="$t('field.name')" required>
          <el-input v-model="seriesForm.name" />
        </el-form-item>
        <el-form-item :label="$t('field.status')">
          <el-switch v-model="seriesForm.enabled" />
        </el-form-item>
        <el-form-item :label="$t('field.remark')">
          <el-input v-model="seriesForm.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSeriesModal = false">{{ $t('action.cancel') }}</el-button>
        <el-button type="primary" @click="saveSeries">{{ $t('action.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showModelModal" :title="modelEditing ? $t('action.edit') : $t('action.add')" width="720px" append-to-body @closed="resetModelForm">
      <el-form :model="modelForm" label-width="120px" class="form-grid">
        <el-form-item :label="$t('field.vehicleSeries')" required class="span-2">
          <el-select v-model="modelForm.seriesId" clearable style="width: 100%">
            <el-option
              v-for="item in seriesOptions"
              :key="item.id"
              :label="formatSeriesLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('field.code')" required>
          <el-input v-model="modelForm.code" :placeholder="$t('placeholder.autoGenerated')" :disabled="!modelEditing" />
        </el-form-item>
        <el-form-item :label="$t('field.name')" required>
          <el-input v-model="modelForm.name" />
        </el-form-item>
        <el-form-item :label="$t('field.yearFrom')">
          <el-input v-model="modelForm.yearFrom" />
        </el-form-item>
        <el-form-item :label="$t('field.yearTo')">
          <el-input v-model="modelForm.yearTo" />
        </el-form-item>
        <el-form-item :label="$t('field.displacement')">
          <el-input v-model="modelForm.displacement" />
        </el-form-item>
        <el-form-item :label="$t('field.engine')">
          <el-input v-model="modelForm.engine" />
        </el-form-item>
        <el-form-item :label="$t('field.status')">
          <el-switch v-model="modelForm.enabled" />
        </el-form-item>
        <el-form-item :label="$t('field.remark')" class="span-2">
          <el-input v-model="modelForm.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showModelModal = false">{{ $t('action.cancel') }}</el-button>
        <el-button type="primary" @click="saveModel">{{ $t('action.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showFitmentModal" :title="fitmentEditing ? $t('action.edit') : $t('action.add')" width="640px" append-to-body @closed="resetFitmentForm">
      <el-form :model="fitmentForm" label-width="110px">
        <el-form-item :label="$t('field.product')" required>
          <el-select v-model="fitmentForm.productId" :disabled="fitmentEditing" clearable filterable style="width: 100%">
            <el-option
              v-for="item in productOptions"
              :key="item.id"
              :label="formatProductLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('field.vehicleModel')" required>
          <el-select v-model="fitmentForm.modelId" :disabled="fitmentEditing" clearable filterable style="width: 100%">
            <el-option
              v-for="item in modelOptions"
              :key="item.id"
              :label="formatModelLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="`${$t('field.product')}${$t('field.code')}`">
          <el-input :model-value="selectedProductCode" readonly />
        </el-form-item>
        <el-form-item :label="`${$t('field.vehicleModel')}${$t('field.code')}`">
          <el-input :model-value="selectedModelCode" readonly />
        </el-form-item>
        <el-form-item :label="$t('field.remark')">
          <el-input v-model="fitmentForm.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFitmentModal = false">{{ $t('action.cancel') }}</el-button>
        <el-button type="primary" @click="saveFitment">{{ $t('action.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, ref, reactive, onMounted, onActivated } from 'vue';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { filterByFuzzyKeyword } from '@/utils/fuzzySearch';

interface ProductOption {
  id: number;
  code: string;
  name: string;
}

interface VehicleBrand {
  id: number;
  code: string;
  name: string;
  enabled: boolean;
  remark?: string;
}

interface VehicleSeries {
  id: number;
  brandId: number;
  code: string;
  name: string;
  enabled: boolean;
  remark?: string;
}

interface VehicleModel {
  id: number;
  seriesId: number;
  code: string;
  name: string;
  yearFrom?: number;
  yearTo?: number;
  displacement?: string;
  engine?: string;
  enabled: boolean;
  remark?: string;
}

interface Fitment {
  id: number;
  productId: number;
  modelId: number;
  remark?: string;
}

const { t } = useI18n();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = usePageSizePreference();
const brandColumns = ['code', 'name', 'enabled', 'remark'];
const seriesColumns = ['code', 'brand', 'name', 'enabled', 'remark'];
const modelColumns = ['code', 'series', 'name', 'yearFrom', 'yearTo', 'displacement', 'engine', 'enabled', 'remark'];
const fitmentColumns = ['product', 'vehicleModel', 'remark'];
const brandColumnSettings = useColumnSettings('erp-vehicle-brand', brandColumns);
const seriesColumnSettings = useColumnSettings('erp-vehicle-series', seriesColumns);
const modelColumnSettings = useColumnSettings('erp-vehicle-model', modelColumns);
const fitmentColumnSettings = useColumnSettings('erp-product-fitment', fitmentColumns);
const canShowBrand = (key: string) => brandColumnSettings.isVisible(key);
const canShowSeries = (key: string) => seriesColumnSettings.isVisible(key);
const canShowModel = (key: string) => modelColumnSettings.isVisible(key);
const canShowFitment = (key: string) => fitmentColumnSettings.isVisible(key);

const fetchColumnKeys = () => {
  brandColumnSettings.fetchTenantKeys();
  seriesColumnSettings.fetchTenantKeys();
  modelColumnSettings.fetchTenantKeys();
  fitmentColumnSettings.fetchTenantKeys();
};

const activeTab = ref('brands');
const hasActivatedOnce = ref(false);
const pageSizeSyncReadyCount = ref(0);
const pendingInitialLoad = ref(false);

const brandNameQuery = ref('');
const brandCodeQuery = ref('');
const brandStatus = ref<'all' | 'enabled' | 'disabled'>('all');
const brandLoading = ref(false);
const brandPage = ref(1);
const brandSize = ref(20);
const brandTotal = ref(0);
const brandTable = ref<VehicleBrand[]>([]);
const allBrandTable = ref<VehicleBrand[]>([]);
const showBrandModal = ref(false);
const brandEditing = ref(false);
const currentBrandId = ref<number | null>(null);

const seriesNameQuery = ref('');
const seriesCodeQuery = ref('');
const seriesStatus = ref<'all' | 'enabled' | 'disabled'>('all');
const seriesBrandFilter = ref<number | null>(null);
const seriesLoading = ref(false);
const seriesPage = ref(1);
const seriesSize = ref(20);
const seriesTotal = ref(0);
const seriesTable = ref<VehicleSeries[]>([]);
const allSeriesTable = ref<VehicleSeries[]>([]);
const showSeriesModal = ref(false);
const seriesEditing = ref(false);
const currentSeriesId = ref<number | null>(null);

const modelNameQuery = ref('');
const modelCodeQuery = ref('');
const modelStatus = ref<'all' | 'enabled' | 'disabled'>('all');
const modelSeriesFilter = ref<number | null>(null);
const modelLoading = ref(false);
const modelPage = ref(1);
const modelSize = ref(20);
const modelTotal = ref(0);
const modelTable = ref<VehicleModel[]>([]);
const allModelTable = ref<VehicleModel[]>([]);
const showModelModal = ref(false);
const modelEditing = ref(false);
const currentModelId = ref<number | null>(null);

const fitmentProductFilter = ref<number | null>(null);
const fitmentModelFilter = ref<number | null>(null);
const fitmentLoading = ref(false);
const fitmentTable = ref<Fitment[]>([]);
const allFitmentTable = ref<Fitment[]>([]);
const showFitmentModal = ref(false);
const fitmentEditing = ref(false);
const currentFitmentId = ref<number | null>(null);

const brandOptions = ref<VehicleBrand[]>([]);
const seriesOptions = ref<VehicleSeries[]>([]);
const modelOptions = ref<VehicleModel[]>([]);
const productOptions = ref<ProductOption[]>([]);

const brandForm = reactive({
  code: '',
  name: '',
  enabled: true,
  remark: ''
});

const seriesForm = reactive({
  brandId: null as number | null,
  code: '',
  name: '',
  enabled: true,
  remark: ''
});

const modelForm = reactive({
  seriesId: null as number | null,
  code: '',
  name: '',
  yearFrom: '' as string | number,
  yearTo: '' as string | number,
  displacement: '',
  engine: '',
  enabled: true,
  remark: ''
});

const fitmentForm = reactive({
  productId: null as number | null,
  modelId: null as number | null,
  remark: ''
});

const getBrandName = (id?: number) => brandOptions.value.find(item => item.id === id)?.name || '-';

const formatSeriesLabel = (item: VehicleSeries) => {
  const brand = getBrandName(item.brandId);
  return brand !== '-' ? `${brand} / ${item.name}` : item.name;
};

const getSeriesLabel = (seriesId?: number) => {
  const series = seriesOptions.value.find(item => item.id === seriesId);
  if (!series) return '-';
  return formatSeriesLabel(series);
};

const formatModelLabel = (item: VehicleModel) => {
  const seriesLabel = getSeriesLabel(item.seriesId);
  const year = item.yearFrom || item.yearTo ? ` (${item.yearFrom || ''}-${item.yearTo || ''})` : '';
  return `${seriesLabel} / ${item.name}${year}`;
};

const getModelLabel = (modelId?: number) => {
  const model = modelOptions.value.find(item => item.id === modelId);
  if (!model) return '-';
  return formatModelLabel(model);
};

const formatProductLabel = (item: ProductOption) => {
  return item.code ? `${item.code} / ${item.name}` : item.name;
};

const getProductLabel = (productId?: number) => {
  const product = productOptions.value.find(item => item.id === productId);
  if (!product) return '-';
  return formatProductLabel(product);
};

const getProductCode = (productId?: number | null) => {
  return productOptions.value.find(item => item.id === productId)?.code || '';
};

const getModelCode = (modelId?: number | null) => {
  return modelOptions.value.find(item => item.id === modelId)?.code || '';
};

const selectedProductCode = computed(() => getProductCode(fitmentForm.productId));
const selectedModelCode = computed(() => getModelCode(fitmentForm.modelId));

const toNumber = (value: string | number) => {
  if (value === '' || value === null || value === undefined) return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

const fetchNextBrandCode = async () => {
  try {
    const res: any = await request.get('/erp/vehicle-brands/next-code');
    if (res.data.code === 200) {
      brandForm.code = res.data.data || '';
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchNextSeriesCode = async () => {
  try {
    const res: any = await request.get('/erp/vehicle-series/next-code');
    if (res.data.code === 200) {
      seriesForm.code = res.data.data || '';
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchNextModelCode = async () => {
  try {
    const res: any = await request.get('/erp/vehicle-models/next-code');
    if (res.data.code === 200) {
      modelForm.code = res.data.data || '';
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchBrandOptions = async () => {
  try {
    const res: any = await request.get('/erp/vehicle-brands');
    brandOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchSeriesOptions = async () => {
  try {
    const res: any = await request.get('/erp/vehicle-series');
    seriesOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchModelOptions = async () => {
  try {
    const res: any = await request.get('/erp/vehicle-models');
    modelOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const fetchProductOptions = async () => {
  try {
    const res: any = await request.get('/erp/products');
    productOptions.value = res.data.data || [];
  } catch (error) {
    notifyError(error);
  }
};

const applyBrandSearch = () => {
  let filtered = allBrandTable.value.slice();
  if (brandStatus.value !== 'all') filtered = filtered.filter(row => row.enabled === (brandStatus.value === 'enabled'));
  filtered = filterByFuzzyKeyword(filtered, brandNameQuery.value, row => [row.name]);
  filtered = filterByFuzzyKeyword(filtered, brandCodeQuery.value, row => [row.code]);
  brandTotal.value = filtered.length;
  const start = (brandPage.value - 1) * brandSize.value;
  brandTable.value = filtered.slice(start, start + brandSize.value);
};

const fetchBrandList = async () => {
  applyBrandSearch();
};

const handleBrandSearch = () => {
  brandPage.value = 1;
  applyBrandSearch();
};

const handleBrandPageChange = (page: number) => {
  brandPage.value = page;
  applyBrandSearch();
};

const handleBrandSizeChange = (size: number) => {
  brandSize.value = size;
  brandPage.value = 1;
  applyBrandSearch();
};

const openBrandAdd = () => {
  brandEditing.value = false;
  currentBrandId.value = null;
  resetBrandForm();
  showBrandModal.value = true;
  fetchNextBrandCode();
};

const openBrandEdit = (row: VehicleBrand) => {
  brandEditing.value = true;
  currentBrandId.value = row.id;
  brandForm.code = row.code;
  brandForm.name = row.name;
  brandForm.enabled = row.enabled;
  brandForm.remark = row.remark || '';
  showBrandModal.value = true;
};

const resetBrandForm = () => {
  brandForm.code = '';
  brandForm.name = '';
  brandForm.enabled = true;
  brandForm.remark = '';
};

const saveBrand = async () => {
  if (!brandForm.code || !brandForm.name) {
    notifyWarning(t('message.required'));
    return;
  }
  try {
    const payload = { ...brandForm };
    const res: any = brandEditing.value && currentBrandId.value
      ? await request.put(`/erp/vehicle-brands/${currentBrandId.value}`, payload)
      : await request.post('/erp/vehicle-brands', payload);
    if (res.data.code === 200) {
      notifySuccess();
      showBrandModal.value = false;
      fetchBootstrapData();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleBrandDelete = async (row: VehicleBrand) => {
  try {
    await request.delete(`/erp/vehicle-brands/${row.id}`);
    notifySuccess();
    fetchBootstrapData();
  } catch (error) {
    notifyError(error);
  }
};

const applySeriesSearch = () => {
  let filtered = allSeriesTable.value.slice();
  if (seriesStatus.value !== 'all') filtered = filtered.filter(row => row.enabled === (seriesStatus.value === 'enabled'));
  if (seriesBrandFilter.value) filtered = filtered.filter(row => row.brandId === seriesBrandFilter.value);
  filtered = filterByFuzzyKeyword(filtered, seriesNameQuery.value, row => [row.name]);
  filtered = filterByFuzzyKeyword(filtered, seriesCodeQuery.value, row => [row.code]);
  seriesTotal.value = filtered.length;
  const start = (seriesPage.value - 1) * seriesSize.value;
  seriesTable.value = filtered.slice(start, start + seriesSize.value);
};

const fetchSeriesList = async () => {
  applySeriesSearch();
};

const handleSeriesSearch = () => {
  seriesPage.value = 1;
  applySeriesSearch();
};

const handleSeriesPageChange = (page: number) => {
  seriesPage.value = page;
  applySeriesSearch();
};

const handleSeriesSizeChange = (size: number) => {
  seriesSize.value = size;
  seriesPage.value = 1;
  applySeriesSearch();
};
const openSeriesAdd = () => {
  seriesEditing.value = false;
  currentSeriesId.value = null;
  resetSeriesForm();
  showSeriesModal.value = true;
  fetchNextSeriesCode();
};

const openSeriesEdit = (row: VehicleSeries) => {
  seriesEditing.value = true;
  currentSeriesId.value = row.id;
  seriesForm.brandId = row.brandId;
  seriesForm.code = row.code;
  seriesForm.name = row.name;
  seriesForm.enabled = row.enabled;
  seriesForm.remark = row.remark || '';
  showSeriesModal.value = true;
};

const resetSeriesForm = () => {
  seriesForm.brandId = null;
  seriesForm.code = '';
  seriesForm.name = '';
  seriesForm.enabled = true;
  seriesForm.remark = '';
};

const saveSeries = async () => {
  if (!seriesForm.brandId || !seriesForm.code || !seriesForm.name) {
    notifyWarning(t('message.required'));
    return;
  }
  try {
    const payload = { ...seriesForm };
    const res: any = seriesEditing.value && currentSeriesId.value
      ? await request.put(`/erp/vehicle-series/${currentSeriesId.value}`, payload)
      : await request.post('/erp/vehicle-series', payload);
    if (res.data.code === 200) {
      notifySuccess();
      showSeriesModal.value = false;
      fetchBootstrapData();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleSeriesDelete = async (row: VehicleSeries) => {
  try {
    await request.delete(`/erp/vehicle-series/${row.id}`);
    notifySuccess();
    fetchBootstrapData();
  } catch (error) {
    notifyError(error);
  }
};

const applyModelSearch = () => {
  let filtered = allModelTable.value.slice();
  if (modelStatus.value !== 'all') filtered = filtered.filter(row => row.enabled === (modelStatus.value === 'enabled'));
  if (modelSeriesFilter.value) filtered = filtered.filter(row => row.seriesId === modelSeriesFilter.value);
  filtered = filterByFuzzyKeyword(filtered, modelNameQuery.value, row => [row.name]);
  filtered = filterByFuzzyKeyword(filtered, modelCodeQuery.value, row => [row.code]);
  modelTotal.value = filtered.length;
  const start = (modelPage.value - 1) * modelSize.value;
  modelTable.value = filtered.slice(start, start + modelSize.value);
};

const fetchModelList = async () => {
  applyModelSearch();
};

const handleModelSearch = () => {
  modelPage.value = 1;
  applyModelSearch();
};

const handleModelPageChange = (page: number) => {
  modelPage.value = page;
  applyModelSearch();
};

const handleModelSizeChange = (size: number) => {
  modelSize.value = size;
  modelPage.value = 1;
  applyModelSearch();
};
const openModelAdd = () => {
  modelEditing.value = false;
  currentModelId.value = null;
  resetModelForm();
  showModelModal.value = true;
  fetchNextModelCode();
};

const openModelEdit = (row: VehicleModel) => {
  modelEditing.value = true;
  currentModelId.value = row.id;
  modelForm.seriesId = row.seriesId;
  modelForm.code = row.code;
  modelForm.name = row.name;
  modelForm.yearFrom = row.yearFrom ?? '';
  modelForm.yearTo = row.yearTo ?? '';
  modelForm.displacement = row.displacement || '';
  modelForm.engine = row.engine || '';
  modelForm.enabled = row.enabled;
  modelForm.remark = row.remark || '';
  showModelModal.value = true;
};

const resetModelForm = () => {
  modelForm.seriesId = null;
  modelForm.code = '';
  modelForm.name = '';
  modelForm.yearFrom = '';
  modelForm.yearTo = '';
  modelForm.displacement = '';
  modelForm.engine = '';
  modelForm.enabled = true;
  modelForm.remark = '';
};

const saveModel = async () => {
  if (!modelForm.seriesId || !modelForm.code || !modelForm.name) {
    notifyWarning(t('message.required'));
    return;
  }
  try {
    const payload = {
      seriesId: modelForm.seriesId,
      code: modelForm.code,
      name: modelForm.name,
      yearFrom: toNumber(modelForm.yearFrom),
      yearTo: toNumber(modelForm.yearTo),
      displacement: modelForm.displacement || null,
      engine: modelForm.engine || null,
      enabled: modelForm.enabled,
      remark: modelForm.remark || null
    };
    const res: any = modelEditing.value && currentModelId.value
      ? await request.put(`/erp/vehicle-models/${currentModelId.value}`, payload)
      : await request.post('/erp/vehicle-models', payload);
    if (res.data.code === 200) {
      notifySuccess();
      showModelModal.value = false;
      fetchBootstrapData();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleModelDelete = async (row: VehicleModel) => {
  try {
    await request.delete(`/erp/vehicle-models/${row.id}`);
    notifySuccess();
    fetchBootstrapData();
  } catch (error) {
    notifyError(error);
  }
};

const applyFitmentFilters = () => {
  let filtered = allFitmentTable.value.slice();
  if (fitmentProductFilter.value) {
    filtered = filtered.filter(row => row.productId === fitmentProductFilter.value);
  }
  if (fitmentModelFilter.value) {
    filtered = filtered.filter(row => row.modelId === fitmentModelFilter.value);
  }
  fitmentTable.value = filtered;
};

async function fetchFitmentList() {
  applyFitmentFilters();
}

const fetchBootstrapData = async () => {
  fitmentLoading.value = true;
  brandLoading.value = true;
  seriesLoading.value = true;
  modelLoading.value = true;
  try {
    const res: any = await request.get('/erp/vehicle-fitments/bootstrap');
    if (res.data.code === 200) {
      const data = res.data.data || {};
      const brands = data.brands || [];
      const series = data.series || [];
      const models = data.models || [];
      const products = data.products || [];
      const fitments = data.fitments || [];
      brandOptions.value = brands;
      seriesOptions.value = series;
      modelOptions.value = models;
      productOptions.value = products;
      allBrandTable.value = brands;
      allSeriesTable.value = series;
      allModelTable.value = models;
      allFitmentTable.value = fitments;
      applyBrandSearch();
      applySeriesSearch();
      applyModelSearch();
      applyFitmentFilters();
    }
  } catch (error) {
    notifyError(error);
  } finally {
    fitmentLoading.value = false;
    brandLoading.value = false;
    seriesLoading.value = false;
    modelLoading.value = false;
  }
};

const openFitmentAdd = () => {
  fitmentEditing.value = false;
  currentFitmentId.value = null;
  resetFitmentForm();
  showFitmentModal.value = true;
};

const openFitmentEdit = (row: Fitment) => {
  fitmentEditing.value = true;
  currentFitmentId.value = row.id;
  fitmentForm.productId = row.productId;
  fitmentForm.modelId = row.modelId;
  fitmentForm.remark = row.remark || '';
  showFitmentModal.value = true;
};

const resetFitmentForm = () => {
  fitmentForm.productId = null;
  fitmentForm.modelId = null;
  fitmentForm.remark = '';
};

const saveFitment = async () => {
  if (!fitmentForm.productId || !fitmentForm.modelId) {
    notifyWarning(t('message.required'));
    return;
  }
  try {
    const payload = {
      productId: fitmentForm.productId,
      modelId: fitmentForm.modelId,
      remark: fitmentForm.remark || null
    };
    const res: any = fitmentEditing.value && currentFitmentId.value
      ? await request.put(`/erp/product-fitments/${currentFitmentId.value}`, { remark: fitmentForm.remark || null })
      : await request.post('/erp/product-fitments', payload);
    if (res.data.code === 200) {
      notifySuccess();
      showFitmentModal.value = false;
      fetchBootstrapData();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleFitmentDelete = async (row: Fitment) => {
  try {
    await request.delete(`/erp/product-fitments/${row.id}`);
    notifySuccess();
    fetchBootstrapData();
  } catch (error) {
    notifyError(error);
  }
};

const initData = () => {
  fetchBootstrapData();
};

const handlePageSizeSyncReady = () => {
  pageSizeSyncReadyCount.value += 1;
  if (pageSizeSyncReadyCount.value >= 3 && pendingInitialLoad.value) {
    pendingInitialLoad.value = false;
    initData();
  }
};

bindPageSizeSync(brandSize, fetchBrandList, {
  reloadOnInitialSync: false,
  onInitialSyncComplete: handlePageSizeSyncReady
});
bindPageSizeSync(seriesSize, fetchSeriesList, {
  reloadOnInitialSync: false,
  onInitialSyncComplete: handlePageSizeSyncReady
});
bindPageSizeSync(modelSize, fetchModelList, {
  reloadOnInitialSync: false,
  onInitialSyncComplete: handlePageSizeSyncReady
});

onMounted(() => {
  fetchColumnKeys();
  if (pageSizeSyncReadyCount.value >= 3) {
    initData();
  } else {
    pendingInitialLoad.value = true;
  }
});

onActivated(() => {
  if (!hasActivatedOnce.value) {
    hasActivatedOnce.value = true;
    return;
  }
  fetchColumnKeys();
  initData();
});
</script>

<style scoped>
.page-toolbar-card--tabs {
  padding-top: 0;
}

.vehicle-tabs :deep(.el-tab-pane) {
  margin-top: 8px;
}

.vehicle-tab-toolbar {
  margin-bottom: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px 16px;
}

.form-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.span-2 {
  grid-column: span 2;
}

@media (max-width: 1280px) {
  .page-toolbar-card--tabs {
    padding-top: 0;
  }
}
</style>
