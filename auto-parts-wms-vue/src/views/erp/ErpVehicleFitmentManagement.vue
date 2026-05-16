<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpVehicleFitmentManagement') }}</div>
      <div class="page-toolbar-card page-toolbar-card--tabs">
        <el-tabs v-model="activeTab" class="vehicle-tabs">
          <el-tab-pane :label="$t('field.vehicleBrand')" name="brands">
            <div class="erp-basic-toolbar vehicle-tab-toolbar">
              <div class="erp-basic-filters erp-basic-filters--2">
            <el-input
              v-model="brandSearch"
              :placeholder="$t('action.search')"
              class="table-search erp-basic-field--wide"
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
                <el-button type="primary" v-permission="'erp-vehicle-brand:add'" @click="openBrandAdd">
                  {{ $t('action.add') }}
                </el-button>
              </div>
            </div>

        <div class="table-card">
          <div class="table-body">
            <el-table :data="brandTable" style="width: 100%" stripe v-loading="brandLoading" :empty-text="$t('table.empty')">
              <el-table-column type="index" :label="$t('table.index')" width="70" />
              <el-table-column v-if="canShowBrand('code')" prop="code" :label="$t('field.code')" min-width="120" />
              <el-table-column v-if="canShowBrand('name')" prop="name" :label="$t('field.name')" min-width="160" />
              <el-table-column v-if="canShowBrand('enabled')" prop="enabled" :label="$t('field.status')" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                    {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column v-if="canShowBrand('remark')" prop="remark" :label="$t('field.remark')" min-width="160" show-overflow-tooltip />
              <el-table-column :label="$t('table.actions')" width="160" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" v-permission="'erp-vehicle-brand:edit'" @click="openBrandEdit(row)">
                    {{ $t('action.edit') }}
                  </el-button>
                  <el-button link type="danger" size="small" v-permission="'erp-vehicle-brand:delete'" @click="handleBrandDelete(row)">
                    {{ $t('action.delete') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
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
            <div class="erp-basic-toolbar vehicle-tab-toolbar">
              <div class="erp-basic-filters erp-basic-filters--3">
            <el-input
              v-model="seriesSearch"
              :placeholder="$t('action.search')"
              class="table-search erp-basic-field--wide"
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
                <el-button type="primary" v-permission="'erp-vehicle-series:add'" @click="openSeriesAdd">
                  {{ $t('action.add') }}
                </el-button>
              </div>
            </div>

        <div class="table-card">
          <div class="table-body">
            <el-table :data="seriesTable" style="width: 100%" stripe v-loading="seriesLoading" :empty-text="$t('table.empty')">
              <el-table-column type="index" :label="$t('table.index')" width="70" />
              <el-table-column v-if="canShowSeries('code')" prop="code" :label="$t('field.code')" min-width="120" />
              <el-table-column v-if="canShowSeries('brand')" :label="$t('field.vehicleBrand')" min-width="160">
                <template #default="{ row }">
                  {{ getBrandName(row.brandId) }}
                </template>
              </el-table-column>
              <el-table-column v-if="canShowSeries('name')" prop="name" :label="$t('field.name')" min-width="160" />
              <el-table-column v-if="canShowSeries('enabled')" prop="enabled" :label="$t('field.status')" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                    {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column v-if="canShowSeries('remark')" prop="remark" :label="$t('field.remark')" min-width="160" show-overflow-tooltip />
              <el-table-column :label="$t('table.actions')" width="160" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" v-permission="'erp-vehicle-series:edit'" @click="openSeriesEdit(row)">
                    {{ $t('action.edit') }}
                  </el-button>
                  <el-button link type="danger" size="small" v-permission="'erp-vehicle-series:delete'" @click="handleSeriesDelete(row)">
                    {{ $t('action.delete') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
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
            <div class="erp-basic-toolbar vehicle-tab-toolbar">
              <div class="erp-basic-filters erp-basic-filters--3">
            <el-input
              v-model="modelSearch"
              :placeholder="$t('action.search')"
              class="table-search erp-basic-field--wide"
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
                <el-button type="primary" v-permission="'erp-vehicle-model:add'" @click="openModelAdd">
                  {{ $t('action.add') }}
                </el-button>
              </div>
            </div>

        <div class="table-card">
          <div class="table-body">
            <el-table :data="modelTable" style="width: 100%" stripe v-loading="modelLoading" :empty-text="$t('table.empty')">
              <el-table-column type="index" :label="$t('table.index')" width="70" />
              <el-table-column v-if="canShowModel('code')" prop="code" :label="$t('field.code')" min-width="120" />
              <el-table-column v-if="canShowModel('series')" :label="$t('field.vehicleSeries')" min-width="180">
                <template #default="{ row }">
                  {{ getSeriesLabel(row.seriesId) }}
                </template>
              </el-table-column>
              <el-table-column v-if="canShowModel('name')" prop="name" :label="$t('field.name')" min-width="160" />
              <el-table-column v-if="canShowModel('yearFrom')" :label="$t('field.yearFrom')" width="100">
                <template #default="{ row }">
                  {{ row.yearFrom || '-' }}
                </template>
              </el-table-column>
              <el-table-column v-if="canShowModel('yearTo')" :label="$t('field.yearTo')" width="100">
                <template #default="{ row }">
                  {{ row.yearTo || '-' }}
                </template>
              </el-table-column>
              <el-table-column v-if="canShowModel('displacement')" prop="displacement" :label="$t('field.displacement')" min-width="120" />
              <el-table-column v-if="canShowModel('engine')" prop="engine" :label="$t('field.engine')" min-width="140" />
              <el-table-column v-if="canShowModel('enabled')" prop="enabled" :label="$t('field.status')" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                    {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column v-if="canShowModel('remark')" prop="remark" :label="$t('field.remark')" min-width="160" show-overflow-tooltip />
              <el-table-column :label="$t('table.actions')" width="160" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" v-permission="'erp-vehicle-model:edit'" @click="openModelEdit(row)">
                    {{ $t('action.edit') }}
                  </el-button>
                  <el-button link type="danger" size="small" v-permission="'erp-vehicle-model:delete'" @click="handleModelDelete(row)">
                    {{ $t('action.delete') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
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
            <div class="erp-basic-toolbar vehicle-tab-toolbar">
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
            <el-table :data="fitmentTable" style="width: 100%" stripe v-loading="fitmentLoading" :empty-text="$t('table.empty')">
              <el-table-column type="index" :label="$t('table.index')" width="70" />
              <el-table-column v-if="canShowFitment('product')" :label="$t('field.product')" min-width="200">
                <template #default="{ row }">
                  {{ getProductLabel(row.productId) }}
                </template>
              </el-table-column>
              <el-table-column v-if="canShowFitment('vehicleModel')" :label="$t('field.vehicleModel')" min-width="240">
                <template #default="{ row }">
                  {{ getModelLabel(row.modelId) }}
                </template>
              </el-table-column>
              <el-table-column v-if="canShowFitment('remark')" prop="remark" :label="$t('field.remark')" min-width="200" show-overflow-tooltip />
              <el-table-column :label="$t('table.actions')" width="160" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" v-permission="'erp-product-fitment:edit'" @click="openFitmentEdit(row)">
                    {{ $t('action.edit') }}
                  </el-button>
                  <el-button link type="danger" size="small" v-permission="'erp-product-fitment:delete'" @click="handleFitmentDelete(row)">
                    {{ $t('action.delete') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <el-dialog v-model="showBrandModal" :title="brandEditing ? $t('action.edit') : $t('action.add')" width="520px" @closed="resetBrandForm">
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
    <el-dialog v-model="showSeriesModal" :title="seriesEditing ? $t('action.edit') : $t('action.add')" width="560px" @closed="resetSeriesForm">
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

    <el-dialog v-model="showModelModal" :title="modelEditing ? $t('action.edit') : $t('action.add')" width="720px" @closed="resetModelForm">
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

    <el-dialog v-model="showFitmentModal" :title="fitmentEditing ? $t('action.edit') : $t('action.add')" width="640px" @closed="resetFitmentForm">
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
import { useSystemConfig } from '@/composables/useSystemConfig';

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
const { bindPageSizeSync } = useSystemConfig();
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

const brandSearch = ref('');
const brandStatus = ref<'all' | 'enabled' | 'disabled'>('all');
const brandLoading = ref(false);
const brandPage = ref(1);
const brandSize = ref(20);
const brandTotal = ref(0);
const brandTable = ref<VehicleBrand[]>([]);
const showBrandModal = ref(false);
const brandEditing = ref(false);
const currentBrandId = ref<number | null>(null);

const seriesSearch = ref('');
const seriesStatus = ref<'all' | 'enabled' | 'disabled'>('all');
const seriesBrandFilter = ref<number | null>(null);
const seriesLoading = ref(false);
const seriesPage = ref(1);
const seriesSize = ref(20);
const seriesTotal = ref(0);
const seriesTable = ref<VehicleSeries[]>([]);
const showSeriesModal = ref(false);
const seriesEditing = ref(false);
const currentSeriesId = ref<number | null>(null);

const modelSearch = ref('');
const modelStatus = ref<'all' | 'enabled' | 'disabled'>('all');
const modelSeriesFilter = ref<number | null>(null);
const modelLoading = ref(false);
const modelPage = ref(1);
const modelSize = ref(20);
const modelTotal = ref(0);
const modelTable = ref<VehicleModel[]>([]);
const showModelModal = ref(false);
const modelEditing = ref(false);
const currentModelId = ref<number | null>(null);

const fitmentProductFilter = ref<number | null>(null);
const fitmentModelFilter = ref<number | null>(null);
const fitmentLoading = ref(false);
const fitmentTable = ref<Fitment[]>([]);
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

const fetchBrandList = async () => {
  brandLoading.value = true;
  try {
    const params: Record<string, any> = { page: brandPage.value, size: brandSize.value };
    if (brandSearch.value) params.keyword = brandSearch.value.trim();
    if (brandStatus.value !== 'all') params.enabled = brandStatus.value === 'enabled';
    const res: any = await request.get('/erp/vehicle-brands/page', { params });
    if (res.data.code === 200) {
      brandTable.value = res.data.data.items || [];
      brandTotal.value = res.data.data.total || 0;
    }
  } catch (error) {
    notifyError(error);
  } finally {
    brandLoading.value = false;
  }
};

const handleBrandSearch = () => {
  brandPage.value = 1;
  fetchBrandList();
};

const handleBrandPageChange = (page: number) => {
  brandPage.value = page;
  fetchBrandList();
};

const handleBrandSizeChange = (size: number) => {
  brandSize.value = size;
  brandPage.value = 1;
  fetchBrandList();
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
      fetchBrandList();
      fetchBrandOptions();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleBrandDelete = async (row: VehicleBrand) => {
  try {
    await request.delete(`/erp/vehicle-brands/${row.id}`);
    notifySuccess();
    fetchBrandList();
    fetchBrandOptions();
  } catch (error) {
    notifyError(error);
  }
};

const fetchSeriesList = async () => {
  seriesLoading.value = true;
  try {
    const params: Record<string, any> = { page: seriesPage.value, size: seriesSize.value };
    if (seriesSearch.value) params.keyword = seriesSearch.value.trim();
    if (seriesStatus.value !== 'all') params.enabled = seriesStatus.value === 'enabled';
    if (seriesBrandFilter.value) params.brandId = seriesBrandFilter.value;
    const res: any = await request.get('/erp/vehicle-series/page', { params });
    if (res.data.code === 200) {
      seriesTable.value = res.data.data.items || [];
      seriesTotal.value = res.data.data.total || 0;
    }
  } catch (error) {
    notifyError(error);
  } finally {
    seriesLoading.value = false;
  }
};

const handleSeriesSearch = () => {
  seriesPage.value = 1;
  fetchSeriesList();
};

const handleSeriesPageChange = (page: number) => {
  seriesPage.value = page;
  fetchSeriesList();
};

const handleSeriesSizeChange = (size: number) => {
  seriesSize.value = size;
  seriesPage.value = 1;
  fetchSeriesList();
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
      fetchSeriesList();
      fetchSeriesOptions();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleSeriesDelete = async (row: VehicleSeries) => {
  try {
    await request.delete(`/erp/vehicle-series/${row.id}`);
    notifySuccess();
    fetchSeriesList();
    fetchSeriesOptions();
  } catch (error) {
    notifyError(error);
  }
};

const fetchModelList = async () => {
  modelLoading.value = true;
  try {
    const params: Record<string, any> = { page: modelPage.value, size: modelSize.value };
    if (modelSearch.value) params.keyword = modelSearch.value.trim();
    if (modelStatus.value !== 'all') params.enabled = modelStatus.value === 'enabled';
    if (modelSeriesFilter.value) params.seriesId = modelSeriesFilter.value;
    const res: any = await request.get('/erp/vehicle-models/page', { params });
    if (res.data.code === 200) {
      modelTable.value = res.data.data.items || [];
      modelTotal.value = res.data.data.total || 0;
    }
  } catch (error) {
    notifyError(error);
  } finally {
    modelLoading.value = false;
  }
};

const handleModelSearch = () => {
  modelPage.value = 1;
  fetchModelList();
};

const handleModelPageChange = (page: number) => {
  modelPage.value = page;
  fetchModelList();
};

const handleModelSizeChange = (size: number) => {
  modelSize.value = size;
  modelPage.value = 1;
  fetchModelList();
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
      fetchModelList();
      fetchModelOptions();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleModelDelete = async (row: VehicleModel) => {
  try {
    await request.delete(`/erp/vehicle-models/${row.id}`);
    notifySuccess();
    fetchModelList();
    fetchModelOptions();
  } catch (error) {
    notifyError(error);
  }
};

const fetchFitmentList = async () => {
  fitmentLoading.value = true;
  try {
    const params: Record<string, any> = {};
    if (fitmentProductFilter.value) params.productId = fitmentProductFilter.value;
    if (fitmentModelFilter.value) params.modelId = fitmentModelFilter.value;
    const res: any = await request.get('/erp/product-fitments', { params });
    if (res.data.code === 200) {
      fitmentTable.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  } finally {
    fitmentLoading.value = false;
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
      fetchFitmentList();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleFitmentDelete = async (row: Fitment) => {
  try {
    await request.delete(`/erp/product-fitments/${row.id}`);
    notifySuccess();
    fetchFitmentList();
  } catch (error) {
    notifyError(error);
  }
};

const initData = () => {
  fetchBrandOptions();
  fetchSeriesOptions();
  fetchModelOptions();
  fetchProductOptions();
  fetchBrandList();
  fetchSeriesList();
  fetchModelList();
  fetchFitmentList();
};

onMounted(() => {
  fetchColumnKeys();
  initData();
  bindPageSizeSync(brandSize, fetchBrandList);
  bindPageSizeSync(seriesSize, fetchSeriesList);
  bindPageSizeSync(modelSize, fetchModelList);
});

onActivated(() => {
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
