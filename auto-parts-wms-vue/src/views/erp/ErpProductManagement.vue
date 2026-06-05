<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">{{ $t('page.erpProductManagement') }}</div>
      <div class="page-toolbar-card">
        <div class="erp-basic-toolbar erp-basic-toolbar--fixed-actions">
          <div class="erp-basic-filters erp-basic-filters--6">
          <el-input
            v-model="nameQuery"
            placeholder="名称"
            class="table-search erp-basic-field--narrow"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <el-input
            v-model="codeQuery"
            placeholder="编码"
            class="table-search erp-basic-field--narrow"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <el-input
            v-model="shortNameQuery"
            placeholder="简称"
            class="table-search erp-basic-field--narrow"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <el-input
            v-model="barcodeQuery"
            placeholder="条码"
            class="table-search erp-basic-field--narrow"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="categoryFilter" :placeholder="$t('field.category')" class="table-search erp-basic-field--narrow" clearable @change="handleSearch">
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="statusFilter" :placeholder="$t('field.status')" class="table-search erp-basic-field--narrow" @change="handleSearch">
            <el-option :label="$t('filter.all')" value="all" />
            <el-option :label="$t('status.active')" value="enabled" />
            <el-option :label="$t('status.inactive')" value="disabled" />
          </el-select>
          </div>
          <div class="erp-basic-actions">
            <el-button type="primary" @click="handleSearch">{{ $t('action.search') }}</el-button>
            <el-button v-permission="'erp-product:import'" @click="openProductImportDialog">{{ $t('action.import') }}</el-button>
            <el-button v-permission="'erp-product:import'" @click="openProductImportHistoryDrawer">导入结果</el-button>
            <el-button type="primary" v-permission="'erp-product:add'" @click="openAddModal">{{ $t('action.add') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-body">
        <ErpDataTable :data="tableData" style="width: 100%" stripe v-loading="loading" :empty-text="$t('table.empty')" table-key="erp-product-management">
          <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
          <ErpDataTableColumn v-if="canShow('code')" prop="code" :label="$t('field.code')" min-width="120" />
          <ErpDataTableColumn v-if="canShow('name')" prop="name" :label="$t('field.name')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('manufacturerCode')" prop="manufacturerCode" :label="$t('field.manufacturerCode')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('manufacturerModel')" prop="manufacturerModel" :label="$t('field.manufacturerModel')" min-width="140" />
          <ErpDataTableColumn v-if="canShow('manufacturerName')" prop="manufacturerName" :label="$t('field.manufacturerName')" min-width="160" />
          <ErpDataTableColumn v-if="canShow('sourceSupplier')" :label="$t('field.sourceSupplier')" min-width="160" column-key="sourceSupplier">
            <template #default="{ row }">
              {{ getSourceSupplierName(row.sourceSupplierId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('productType')" label="商品类型" min-width="120" column-key="productType">
            <template #default="{ row }">
              <el-tag size="small" :type="row.productType === 'ASSEMBLY' ? 'warning' : 'info'">
                {{ formatProductType(row.productType) }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('category')" :label="$t('field.category')" min-width="140" column-key="category">
            <template #default="{ row }">
              {{ getCategoryName(row.categoryId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('unit')" :label="$t('field.unit')" min-width="120" column-key="unit">
            <template #default="{ row }">
              {{ getUnitName(row.unitId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('defaultWarehouse')" :label="$t('field.defaultWarehouse')" min-width="140" column-key="defaultWarehouse">
            <template #default="{ row }">
              {{ getWarehouseName(row.defaultWarehouseId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('defaultLocation')" :label="$t('field.defaultLocation')" min-width="140" column-key="defaultLocation">
            <template #default="{ row }">
              {{ getLocationName(row.defaultLocationId) }}
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('price')" prop="salePrice" :label="$t('field.price')" min-width="120" />
          <ErpDataTableColumn v-if="canShow('costPrice') && canViewCostPrice" :label="$t('field.costPrice')" min-width="120" column-key="costPrice">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                class="cost-price-trigger"
                :disabled="!canViewPurchaseHistory"
                @click="openPurchaseHistory(row)"
              >
                {{ formatMoney(row.costPrice) }}
              </el-button>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn v-if="canShow('weight')" prop="weight" :label="$t('field.weight')" min-width="110" />
          <ErpDataTableColumn v-if="canShow('minStock')" prop="minStock" :label="$t('field.minStock')" min-width="120" />
          <ErpDataTableColumn v-if="canShow('maxStock')" prop="maxStock" :label="$t('field.maxStock')" min-width="120" />
          <ErpDataTableColumn v-if="canShow('status')" prop="enabled" :label="$t('field.status')" width="110">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                {{ row.enabled ? $t('status.active') : $t('status.inactive') }}
              </el-tag>
            </template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('table.actions')" width="160" fixed="right" column-key="actions">
            <template #default="{ row }">
              <el-button link type="primary" size="small" v-permission="'erp-product:edit'" @click="openEditModal(row)">{{ $t('action.edit') }}</el-button>
              <el-button link type="danger" size="small" v-permission="'erp-product:delete'" @click="handleDelete(row)">{{ $t('action.delete') }}</el-button>
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

    <el-dialog
      v-if="showModal"
      v-model="showModal"
      class="product-dialog"
      :style="productDialogStyle"
      draggable
      overflow
      @closed="handleProductDialogClosed"
      destroy-on-close
    >
      <template #header>
        <div class="product-dialog__header">
          <div class="product-dialog__header-main">
            <div class="product-dialog__title-block">
              <div class="product-dialog__title">
                {{ isEditing ? $t('action.edit') : $t('action.add') }}
              </div>
              <div class="product-dialog__subtitle">按业务使用顺序维护商品资料、库存策略、价格与扩展字段</div>
            </div>
          </div>
          <div class="product-dialog__drag-hint">拖动标题栏移动，拖动四角调整大小</div>
        </div>
      </template>

      <div class="product-dialog__content">
        <div class="product-dialog__intro">
          <span class="product-dialog__intro-tag">{{ $t('placeholder.autoGenerated') }}</span>
          <span class="product-dialog__intro-text">编码自动生成，名称为必填；其他资料可按需补充。</span>
        </div>

        <el-form :model="formData" label-position="top" label-width="auto" class="product-dialog__form">
          <div class="form-section module-card">
            <div class="section-title">{{ $t('section.basicInfo') }}</div>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item :label="$t('field.code')" required>
                  <el-input v-model="formData.code" :placeholder="$t('placeholder.autoGenerated')" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('field.name')" required>
                  <el-input v-model="formData.name" :placeholder="$t('field.name')" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item :label="$t('field.category')">
                  <el-select v-model="formData.categoryId" clearable filterable style="width: 100%" :placeholder="$t('field.category')">
                    <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('field.unit')">
                  <el-select v-model="formData.unitId" clearable filterable style="width: 100%" :placeholder="$t('field.unit')">
                    <el-option v-for="item in unitOptions" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item :label="$t('field.shortName')">
                  <el-input v-model="formData.shortName" :placeholder="$t('field.shortName')" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('field.spec')">
                  <el-input v-model="formData.spec" :placeholder="$t('field.spec')" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="商品类型">
              <el-radio-group v-model="formData.productType">
                <el-radio-button value="NORMAL">普通商品</el-radio-button>
                <el-radio-button value="ASSEMBLY">组装商品</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item :label="$t('field.model')">
                  <el-input v-model="formData.model" :placeholder="$t('field.model')" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('field.barcode')">
                  <el-input v-model="formData.barcode" :placeholder="$t('field.barcode')" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item :label="$t('field.sku')">
                  <el-input v-model="formData.sku" :placeholder="$t('field.sku')" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('field.brand')">
                  <el-input v-model="formData.brand" :placeholder="$t('field.brand')" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item :label="$t('field.origin')">
                  <el-input v-model="formData.origin" :placeholder="$t('field.origin')" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('field.manufacturerCode')">
                  <el-input v-model="formData.manufacturerCode" :placeholder="$t('field.manufacturerCode')" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item :label="$t('field.manufacturerModel')">
                  <el-input v-model="formData.manufacturerModel" :placeholder="$t('field.manufacturerModel')" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('field.manufacturerName')">
                  <el-input v-model="formData.manufacturerName" :placeholder="$t('field.manufacturerName')" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item :label="$t('field.sourceSupplier')">
                  <el-select v-model="formData.sourceSupplierId" clearable filterable style="width: 100%" :placeholder="$t('field.sourceSupplier')">
                    <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('field.remark')">
                  <el-input v-model="formData.remark" type="textarea" :rows="3" :placeholder="$t('field.remark')" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item :label="$t('field.weight')">
                  <DecimalInput v-model="formData.weight" :scale="4" :placeholder="$t('field.weight')" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('field.volume')">
                  <DecimalInput v-model="formData.volume" :scale="4" :placeholder="$t('field.volume')" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item :label="$t('field.status')">
              <el-switch
                v-model="formData.enabled"
                :active-text="$t('status.active')"
                :inactive-text="$t('status.inactive')"
                inline-prompt
              />
            </el-form-item>
          </div>

          <div class="form-section module-card inventory-policy-card">
            <div class="section-title">{{ $t('section.inventoryInfo') }}</div>
            <div class="inventory-policy-card__header">
              <div class="inventory-policy-card__hint">仓库覆盖策略：为指定仓库单独配置时，将优先覆盖商品默认策略。</div>
              <el-button type="primary" plain @click="addStockPolicy">新增仓库策略</el-button>
            </div>
            <div class="inventory-policy-card__section-label">默认出入库指向</div>
            <div class="inventory-default-location-row">
              <div class="inventory-policy-row__field inventory-policy-row__field--warehouse">
                <el-form-item :label="$t('field.defaultWarehouse')">
                  <el-select
                    v-model="formData.defaultWarehouseId"
                    clearable
                    filterable
                    :placeholder="$t('field.defaultWarehouse')"
                    @change="formData.defaultLocationId = null"
                  >
                    <el-option v-for="item in warehouseOptions" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </el-form-item>
              </div>
              <div class="inventory-policy-row__field inventory-policy-row__field--location">
                <el-form-item :label="$t('field.defaultLocation')">
                  <el-select
                    v-model="formData.defaultLocationId"
                    clearable
                    filterable
                    :placeholder="$t('field.defaultLocation')"
                    :disabled="!formData.defaultWarehouseId"
                  >
                    <el-option
                      v-for="item in getLocationOptions(formData.defaultWarehouseId)"
                      :key="item.id"
                      :label="item.name"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
              </div>
            </div>
            <div class="inventory-policy-card__section-label">商品默认策略</div>
            <div class="inventory-policy-card__subhint">以下安全库存、最低库存、最高库存属于商品级默认策略，对无单独仓库策略的场景生效，不是上方默认仓库的专属阈值。</div>
            <div class="inventory-policy-row inventory-policy-row--default inventory-default-policy-row">
              <div class="inventory-policy-row__field">
                <el-form-item :label="$t('field.safetyStock')">
                  <DecimalInput v-model="formData.safetyStock" :scale="4" :placeholder="$t('field.safetyStock')" />
                </el-form-item>
              </div>
              <div class="inventory-policy-row__field">
                <el-form-item :label="$t('field.minStock')">
                  <DecimalInput v-model="formData.minStock" :scale="4" :placeholder="$t('field.minStock')" />
                </el-form-item>
              </div>
              <div class="inventory-policy-row__field">
                <el-form-item :label="$t('field.maxStock')">
                  <DecimalInput v-model="formData.maxStock" :scale="4" :placeholder="$t('field.maxStock')" />
                </el-form-item>
              </div>
            </div>
            <div
              v-for="(item, index) in formData.stockPolicies"
              :key="`stock-policy-${index}`"
              class="inventory-policy-row"
            >
              <div class="inventory-policy-row__field inventory-policy-row__field--warehouse">
                <el-form-item :label="index === 0 ? $t('field.warehouse') : ' '">
                  <el-select
                    v-model="item.warehouseId"
                    clearable
                    filterable
                    :placeholder="$t('field.warehouse')"
                  >
                    <el-option v-for="option in warehouseOptions" :key="option.id" :label="option.name" :value="option.id" />
                  </el-select>
                </el-form-item>
              </div>
              <div class="inventory-policy-row__field">
                <el-form-item :label="index === 0 ? $t('field.safetyStock') : ' '">
                  <DecimalInput v-model="item.safetyStock" :scale="4" :placeholder="$t('field.safetyStock')" />
                </el-form-item>
              </div>
              <div class="inventory-policy-row__field">
                <el-form-item :label="index === 0 ? $t('field.minStock') : ' '">
                  <DecimalInput v-model="item.minStock" :scale="4" :placeholder="$t('field.minStock')" />
                </el-form-item>
              </div>
              <div class="inventory-policy-row__field">
                <el-form-item :label="index === 0 ? $t('field.maxStock') : ' '">
                  <DecimalInput v-model="item.maxStock" :scale="4" :placeholder="$t('field.maxStock')" />
                </el-form-item>
              </div>
              <div class="inventory-policy-row__actions">
                <el-button link type="danger" @click="removeStockPolicy(index)">删除</el-button>
              </div>
            </div>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item :label="$t('field.shelfLifeDays')">
                  <DecimalInput v-model="formData.shelfLifeDays" :scale="0" :placeholder="$t('field.shelfLifeDays')" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item :label="$t('field.batch')">
              <el-switch v-model="formData.batch" />
            </el-form-item>
          </div>

          <div class="form-section module-card product-price-card">
            <div class="section-title">{{ $t('section.priceInfo') }}</div>
            <el-row :gutter="16">
              <el-col v-if="canViewCostPrice" :span="12">
                <el-form-item :label="$t('field.costPrice')">
                  <DecimalInput
                    v-model="formData.costPrice"
                    :scale="4"
                    :placeholder="$t('field.costPrice')"
                    :disabled="!canEditCostPrice"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('field.price')">
                  <el-input v-model="formData.salePrice" type="number" :placeholder="$t('field.price')" />
                </el-form-item>
              </el-col>
              <el-col :span="12" :class="{ 'product-price-card__span-offset': !canViewCostPrice }">
                <el-form-item :label="$t('field.taxRate')">
                  <DecimalInput v-model="formData.taxRate" :scale="4" :placeholder="$t('field.taxRate')" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <div class="form-section module-card customer-category-price-card">
            <div class="section-title">{{ $t('field.customerCategoryPrice') }}</div>
            <el-row :gutter="16">
              <el-col v-for="item in priceItems" :key="item.categoryId" :span="8">
                <el-form-item :label="item.categoryName">
                  <DecimalInput v-model="item.salePrice" :scale="2" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <div class="form-section module-card">
            <div class="section-title">{{ $t('section.customFields') }}</div>
            <div class="custom-field-table">
              <ErpDataTable :data="customFields" style="width: 100%" size="small" border table-key="erp-product-custom-fields">
                <ErpDataTableColumn :label="$t('field.customFieldKey')" min-width="200" column-key="customFieldKey">
                  <template #default="{ row }">
                    <el-input v-model="row.key" :placeholder="$t('field.customFieldKey')" />
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn :label="$t('field.customFieldValue')" min-width="240" column-key="customFieldValue">
                  <template #default="{ row }">
                    <el-input v-model="row.value" :placeholder="$t('field.customFieldValue')" />
                  </template>
                </ErpDataTableColumn>
                <ErpDataTableColumn :label="$t('table.actions')" width="90" column-key="actions">
                  <template #default="{ $index }">
                    <el-button link type="danger" @click="removeCustomField($index)">{{ $t('action.delete') }}</el-button>
                  </template>
                </ErpDataTableColumn>
              </ErpDataTable>
              <el-button type="primary" plain size="small" class="add-custom-field" @click="addCustomField">
                {{ $t('action.addField') }}
              </el-button>
            </div>
          </div>
      </el-form>
      </div>

      <Teleport v-if="showModal" :to="productDialogResizeHandleTeleportTarget" :disabled="!productDialogResizeHandleTeleportTarget">
        <button
          type="button"
          class="product-dialog__resize-handle product-dialog__resize-handle--nw"
          aria-label="左上角缩放"
          @pointerdown="startProductDialogResize('nw', $event)"
        />
        <button
          type="button"
          class="product-dialog__resize-handle product-dialog__resize-handle--ne"
          aria-label="右上角缩放"
          @pointerdown="startProductDialogResize('ne', $event)"
        />
        <button
          type="button"
          class="product-dialog__resize-handle product-dialog__resize-handle--sw"
          aria-label="左下角缩放"
          @pointerdown="startProductDialogResize('sw', $event)"
        />
        <button
          type="button"
          class="product-dialog__resize-handle product-dialog__resize-handle--se"
          aria-label="右下角缩放"
          @pointerdown="startProductDialogResize('se', $event)"
        />
      </Teleport>

      <template #footer>
        <div class="product-dialog__footer">
          <div class="product-dialog__footer-actions">
            <el-button @click="showModal = false">{{ $t('action.cancel') }}</el-button>
            <el-button type="primary" @click="saveData">{{ $t('action.save') }}</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-if="purchaseHistoryDialogVisible"
      v-model="purchaseHistoryDialogVisible"
      :title="$t('field.purchaseHistory')"
      width="980px"
      class="history-dialog"
      append-to-body
    >
      <div class="history-header">
        <div class="history-header__item">
          <span>{{ $t('field.product') }}：</span>
          <strong>{{ historyProductName }}</strong>
        </div>
      </div>
      <div v-loading="purchaseHistoryLoading">
        <div class="history-toolbar">
          <el-input
            v-model="purchaseHistoryKeyword"
            :placeholder="$t('placeholder.keyword')"
            clearable
            class="history-search"
          />
          <el-date-picker
            v-model="purchaseHistoryRange"
            type="daterange"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :range-separator="$t('separator.to')"
            :start-placeholder="$t('field.startTime')"
            :end-placeholder="$t('field.endTime')"
            class="history-date"
            clearable
          />
        </div>
        <ErpDataTable
          :data="purchaseHistoryItems"
          stripe
          :empty-text="$t('table.empty')"
          height="360"
         table-key="erp-product-purchase-history">
          <ErpDataTableColumn prop="supplierName" :label="$t('field.supplierName')" min-width="180" />
          <ErpDataTableColumn prop="qty" :label="$t('field.quantity')" width="120" />
          <ErpDataTableColumn :label="$t('field.price')" width="140" column-key="price">
            <template #default="{ row }">{{ formatMoney(row.price) }}</template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('field.priceInclTax')" width="140" column-key="priceInclTax">
            <template #default="{ row }">{{ formatMoney(row.priceInclTax) }}</template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('field.orderTime')" width="180" column-key="orderAt">
            <template #default="{ row }">{{ formatHistoryDate(row.orderAt) }}</template>
          </ErpDataTableColumn>
          <ErpDataTableColumn :label="$t('field.orderNo')" min-width="180" column-key="orderNo">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                class="history-order-link"
                @click="openPurchaseOrderDetail(row)"
              >
                {{ row.orderNo || '-' }}
              </el-button>
            </template>
          </ErpDataTableColumn>
        </ErpDataTable>
        <el-pagination
          class="history-pagination"
          background
          layout="total, sizes, prev, pager, next"
          :total="purchaseHistoryTotal"
          :current-page="purchaseHistoryPage"
          :page-size="purchaseHistorySize"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="handlePurchaseHistoryPageChange"
          @size-change="handlePurchaseHistorySizeChange"
        />
      </div>
    </el-dialog>

    <el-dialog
      v-if="purchaseOrderDetailDialogVisible"
      v-model="purchaseOrderDetailDialogVisible"
      :title="purchaseOrderDetailTitle"
      width="1080px"
      class="purchase-order-detail-dialog"
      append-to-body
      destroy-on-close
    >
      <div v-loading="purchaseOrderDetailLoading">
        <template v-if="purchaseOrderDetail">
          <div class="purchase-detail-summary">
            <div class="purchase-detail-summary__item">
              <span>{{ $t('field.orderNo') }}：</span>
              <strong>{{ purchaseOrderDetail.order.orderNo || '-' }}</strong>
            </div>
            <div class="purchase-detail-summary__item">
              <span>{{ $t('field.orderTime') }}：</span>
              <strong>{{ formatHistoryDate(purchaseOrderDetail.order.orderAt || purchaseOrderDetail.order.createdAt) }}</strong>
            </div>
            <div class="purchase-detail-summary__item">
              <span>{{ $t('field.supplier') }}：</span>
              <strong>{{ purchaseOrderDetailSupplierName }}</strong>
            </div>
            <div class="purchase-detail-summary__item">
              <span>{{ $t('field.status') }}：</span>
              <el-tag size="small" :type="purchaseOrderDetailStatusType">
                {{ formatPurchaseOrderStatus(purchaseOrderDetail.order.status) }}
              </el-tag>
            </div>
            <div class="purchase-detail-summary__item">
              <span>{{ $t('field.totalAmount') }}：</span>
              <strong>{{ formatMoney(purchaseOrderDetail.order.totalAmount) }}</strong>
            </div>
          </div>

          <div class="purchase-detail-remark">
            <span>{{ $t('field.remark') }}：</span>
            <span>{{ purchaseOrderDetail.order.remark || '-' }}</span>
          </div>

          <ErpDataTable
            :data="purchaseOrderDetail.items"
            stripe
            border
            :empty-text="$t('table.empty')"
            max-height="420"
           table-key="erp-product-purchase-order-detail">
            <ErpDataTableColumn type="index" :label="$t('table.index')" width="70" />
            <ErpDataTableColumn :label="$t('field.product')" min-width="220" column-key="product">
              <template #default="{ row }">
                {{ getPurchaseOrderDetailProductName(row.productId) }}
              </template>
            </ErpDataTableColumn>
            <ErpDataTableColumn :label="$t('field.warehouse')" min-width="160" column-key="warehouse">
              <template #default="{ row }">
                {{ getWarehouseName(row.warehouseId) }}
              </template>
            </ErpDataTableColumn>
            <ErpDataTableColumn :label="$t('field.location')" min-width="160" column-key="location">
              <template #default="{ row }">
                {{ getLocationName(row.locationId) }}
              </template>
            </ErpDataTableColumn>
            <ErpDataTableColumn :label="$t('field.quantity')" width="120" column-key="quantity">
              <template #default="{ row }">
                {{ formatPurchaseOrderDetailNumber(row.qty) }}
              </template>
            </ErpDataTableColumn>
            <ErpDataTableColumn :label="$t('field.price')" width="140" column-key="price">
              <template #default="{ row }">
                {{ formatMoney(row.price) }}
              </template>
            </ErpDataTableColumn>
            <ErpDataTableColumn :label="$t('field.lineTotal')" width="140" column-key="lineAmount">
              <template #default="{ row }">
                {{ formatMoney(calcPurchaseOrderDetailLineTotal(row)) }}
              </template>
            </ErpDataTableColumn>
            <ErpDataTableColumn :label="$t('field.remark')" min-width="180" column-key="remark">
              <template #default="{ row }">
                {{ row.remark || '-' }}
              </template>
            </ErpDataTableColumn>
          </ErpDataTable>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-if="showProductImportDialog" v-model="showProductImportDialog" title="导入配件档案" width="720px">
      <el-form label-position="top">
        <el-form-item label="来源名称">
          <el-input v-model="productImportSourceName" placeholder="例如：2026-05-配件档案列表" />
        </el-form-item>
        <el-form-item label="Excel 文件">
          <input
            ref="productImportInputRef"
            type="file"
            accept=".xls,.xlsx"
            @change="handleProductImportFile"
          />
          <div style="margin-top: 8px; color: var(--el-text-color-secondary);">
            {{ productImportFile?.name || '请选择 .xls 或 .xlsx 文件' }}
          </div>
        </el-form-item>
        <el-form-item v-if="productImportPreview" label="字段映射">
          <el-table :data="productImportMappings" v-loading="productImportPreviewLoading" size="small" border style="width: 100%">
            <el-table-column prop="excelHeader" label="Excel表头" min-width="140" />
            <el-table-column prop="sampleValue" label="示例值" min-width="140" />
            <el-table-column label="系统字段" min-width="220">
              <template #default="{ row }">
                <el-select v-model="row.fieldKey" clearable filterable placeholder="选择系统字段" style="width: 100%" @change="handleProductImportMappingChange(row)">
                  <el-option
                    v-for="field in productImportFieldOptions"
                    :key="field.key"
                    :label="field.required ? `${field.label} *` : field.label"
                    :value="field.key"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="row.fieldKey ? (row.matchType === 'AUTO' ? 'success' : 'warning') : 'info'">
                  {{ row.fieldKey ? (row.matchType === 'AUTO' ? '自动匹配' : '手动选择') : '未匹配' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showProductImportDialog = false">{{ $t('action.cancel') }}</el-button>
        <el-button
          v-if="productImportPreview && !productImportMappingConfirmed"
          type="primary"
          :loading="productImportPreviewLoading"
          @click="confirmProductImportMapping"
        >
          确认字段映射
        </el-button>
        <el-button
          v-else
          type="primary"
          :loading="productImportSubmitting"
          @click="submitProductImport"
        >
          {{ $t('action.import') }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-if="showProductImportHistoryDrawer" v-model="showProductImportHistoryDrawer" title="配件导入结果" size="70%">
      <div class="supplier-import-drawer">
        <div class="supplier-import-drawer__toolbar">
          <el-button v-permission="'erp-product:import'" @click="loadProductImportBatches">刷新批次</el-button>
        </div>
        <el-table :data="productImportBatches" v-loading="productImportHistoryLoading" style="width: 100%">
          <el-table-column prop="batchNo" label="批次号" min-width="170" />
          <el-table-column prop="sourceName" label="来源" min-width="150" />
          <el-table-column prop="totalCount" label="总行数" width="90" />
          <el-table-column prop="successCount" label="成功" width="90" />
          <el-table-column prop="failedCount" label="失败" width="90" />
          <el-table-column prop="status" label="状态" width="130" />
          <el-table-column prop="createdAt" label="导入时间" min-width="180" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="viewProductImportBatchItems(row)">查看明细</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="selectedProductImportBatch" class="supplier-import-drawer__detail">
          <div class="supplier-import-drawer__detail-title">
            当前批次：{{ selectedProductImportBatch.batchNo }}
          </div>
          <el-table :data="productImportBatchItems" v-loading="productImportItemsLoading" style="width: 100%">
            <el-table-column prop="rowNo" label="行号" width="80" />
            <el-table-column prop="sourceCode" label="编码" min-width="120" />
            <el-table-column prop="sourceName" label="名称" min-width="180" />
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column prop="warningMessage" label="提示" min-width="180" />
            <el-table-column prop="errorMessage" label="异常原因" min-width="180" />
            <el-table-column prop="suggestion" label="建议处理" min-width="180" />
            <el-table-column prop="matchedStrategy" label="识别策略" width="120" />
          </el-table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onActivated, computed, watch, nextTick, onBeforeUnmount, Teleport } from 'vue';
import { useI18n } from 'vue-i18n';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { usePageSizePreference } from '@/composables/pageSizePreference';
import { useColumnSettings } from '@/composables/useColumnSettings';
import { useAuthStore } from '@/stores/auth';
import { getCachedCategories, getCachedCustomerCategories, getCachedLocationOptions, getCachedSuppliers, getCachedUnits, getCachedWarehouseOptions, invalidateErpBaseDataResourceCache } from '@/composables/erpBaseDataCache';
import DecimalInput from '@/components/DecimalInput.vue';
import { mergeOptionById } from '@/utils/erpMasterData';
import { filterByFuzzyKeyword } from '@/utils/fuzzySearch';
import { waitForErpFirstPaint } from './erpFirstPaint';

interface OptionItem {
  id: number;
  name: string;
  isDefault?: boolean;
}

interface LocationOption extends OptionItem {
  warehouseId?: number;
}

interface ErpProduct {
  id: number;
  code: string;
  name: string;
  shortName?: string;
  productType?: string;
  spec?: string;
  model?: string;
  categoryId?: number;
  unitId?: number;
  defaultWarehouseId?: number;
  defaultLocationId?: number;
  barcode?: string;
  sku?: string;
  brand?: string;
  origin?: string;
  manufacturerCode?: string;
  manufacturerModel?: string;
  manufacturerName?: string;
  sourceSupplierId?: number;
  weight?: number;
  volume?: number;
  salePrice?: number;
  costPrice?: number;
  taxRate?: number;
  safetyStock?: number;
  minStock?: number;
  maxStock?: number;
  stockPolicies?: ProductStockPolicy[];
  batch?: boolean;
  shelfLifeDays?: number;
  enabled: boolean;
  remark?: string;
  extAttrs?: Record<string, string> | string | null;
}

interface ProductPriceItem {
  categoryId: number;
  categoryName: string;
  salePrice: string;
}

interface ProductStockPolicy {
  warehouseId: number | null;
  safetyStock: string;
  minStock: string;
  maxStock: string;
}

interface CustomField {
  key: string;
  value: string;
}

interface PurchaseHistoryItem {
  orderId: number;
  orderNo: string;
  orderAt: string;
  productId: number;
  qty: number;
  price: number;
  priceInclTax: number;
  supplierId: number;
  supplierName: string;
}

interface PurchaseOrderDetailItem {
  id?: number;
  productId?: number;
  warehouseId?: number;
  locationId?: number;
  qty?: number | string;
  price?: number;
  remark?: string;
}

interface PurchaseOrderDetailData {
  order: {
    id: number;
    orderNo?: string;
    orderAt?: string;
    createdAt?: string;
    supplierId?: number;
    status?: string;
    totalAmount?: number;
    remark?: string;
  };
  items: PurchaseOrderDetailItem[];
}

interface ProductImportResult {
  batchId: number;
  batchNo: string;
  status: string;
  totalCount: number;
  successCount: number;
  failedCount: number;
}

interface ProductImportBatchSummary {
  id: number;
  batchNo: string;
  sourceName?: string;
  importMode?: string;
  totalCount: number;
  successCount: number;
  failedCount: number;
  status: string;
  summary?: string;
  createdBy?: string;
  createdAt?: string;
}

interface ProductImportItemView {
  id: number;
  rowNo: number;
  sourceCode?: string;
  sourceName?: string;
  status: string;
  warningMessage?: string;
  errorMessage?: string;
  suggestion?: string;
  matchedStrategy?: string;
}

interface ProductImportFieldOption {
  key: string;
  label: string;
  required: boolean;
  customerCategoryId?: number | null;
}

interface ProductImportHeaderMapping {
  excelHeader: string;
  fieldKey?: string | null;
  fieldLabel?: string | null;
  matchType: 'AUTO' | 'MANUAL' | 'UNMATCHED' | string;
  sampleValue?: string | null;
}

interface ProductImportPreview {
  headers: string[];
  fields: ProductImportFieldOption[];
  mappings: ProductImportHeaderMapping[];
  sampleRows: Record<string, string>[];
  totalRows: number;
}

const { t } = useI18n();
const { notifyError, notifySuccess, notifyWarning } = useApiError();
const { bindPageSizeSync } = usePageSizePreference();
const authStore = useAuthStore();
const tenantCacheKey = computed(() => authStore.tenantId ?? authStore.tenantCode ?? 'default');

const nameQuery = ref('');
const codeQuery = ref('');
const shortNameQuery = ref('');
const barcodeQuery = ref('');
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all');
const categoryFilter = ref<number | null>(null);
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const hasActivatedOnce = ref(false);
const pageSizeSyncReady = ref(false);
const pendingInitialLoad = ref(false);
const firstPaintReady = ref(false);
const tableData = ref<ErpProduct[]>([]);
const allTableData = ref<ErpProduct[]>([]);
const showModal = ref(false);
const isEditing = ref(false);
const currentId = ref<number | null>(null);
const purchaseHistoryDialogVisible = ref(false);
const purchaseHistoryLoading = ref(false);
const purchaseHistoryItems = ref<PurchaseHistoryItem[]>([]);
const purchaseHistoryKeyword = ref('');
const purchaseHistoryRange = ref<string[]>([]);
const purchaseHistoryPage = ref(1);
const purchaseHistorySize = ref(10);
const purchaseHistoryTotal = ref(0);
const historyProduct = ref<ErpProduct | null>(null);
const purchaseOrderDetailDialogVisible = ref(false);
const purchaseOrderDetailLoading = ref(false);
const purchaseOrderDetail = ref<PurchaseOrderDetailData | null>(null);
const purchaseOrderDetailSupplierName = ref('-');
const purchaseOrderProductNameMap = ref<Record<number, string>>({});
const productImportInputRef = ref<HTMLInputElement | null>(null);
const showProductImportDialog = ref(false);
const showProductImportHistoryDrawer = ref(false);
const productImportSourceName = ref('');
const productImportFile = ref<File | null>(null);
const productImportPreview = ref<ProductImportPreview | null>(null);
const productImportFieldOptions = ref<ProductImportFieldOption[]>([]);
const productImportMappings = ref<ProductImportHeaderMapping[]>([]);
const productImportPreviewLoading = ref(false);
const productImportMappingConfirmed = ref(false);
const productImportSubmitting = ref(false);
const productImportHistoryLoading = ref(false);
const productImportItemsLoading = ref(false);
const productImportBatches = ref<ProductImportBatchSummary[]>([]);
const selectedProductImportBatch = ref<ProductImportBatchSummary | null>(null);
const productImportBatchItems = ref<ProductImportItemView[]>([]);
const activeProductImportBatchId = ref<number | null>(null);
const productImportPollingTimer = ref<number | null>(null);

const categoryOptions = ref<OptionItem[]>([]);
const customerCategoryOptions = ref<OptionItem[]>([]);
const supplierOptions = ref<OptionItem[]>([]);
const unitOptions = ref<OptionItem[]>([]);
const warehouseOptions = ref<OptionItem[]>([]);
const locationOptions = ref<LocationOption[]>([]);
const priceItems = ref<ProductPriceItem[]>([]);
const currentPriceMap = ref<Map<number, string>>(new Map());
const customFields = ref<CustomField[]>([]);
const productDialogResizeHandleTeleportTarget = ref<HTMLElement | null>(null);
const productDialogState = reactive({
  width: 1040,
  height: 680,
  top: 48,
  left: 0
});
const activeProductDialogResizePointerId = ref<number | null>(null);
const productDialogResizeState = ref<{
  direction: 'nw' | 'ne' | 'sw' | 'se';
  startX: number;
  startY: number;
  startWidth: number;
  startHeight: number;
  startTop: number;
  startLeft: number;
} | null>(null);

const defaultColumns = ['code', 'name', 'manufacturerCode', 'manufacturerModel', 'manufacturerName', 'sourceSupplier', 'productType', 'category', 'unit', 'defaultWarehouse', 'defaultLocation', 'price', 'costPrice', 'weight', 'minStock', 'maxStock', 'status'];
const { isVisible, fetchTenantKeys } = useColumnSettings('erp-product', defaultColumns);

const formData = reactive({
  code: '',
  name: '',
  shortName: '',
  productType: 'NORMAL',
  spec: '',
  model: '',
  categoryId: null as number | null,
  unitId: null as number | null,
  defaultWarehouseId: null as number | null,
  defaultLocationId: null as number | null,
  barcode: '',
  sku: '',
  brand: '',
  origin: '',
  manufacturerCode: '',
  manufacturerModel: '',
  manufacturerName: '',
  sourceSupplierId: null as number | null,
  weight: '' as string,
  volume: '' as string,
  salePrice: undefined as number | undefined,
  costPrice: '' as string,
  taxRate: '' as string,
  safetyStock: '' as string,
  minStock: '' as string,
  maxStock: '' as string,
  stockPolicies: [] as ProductStockPolicy[],
  batch: false,
  shelfLifeDays: '' as string,
  enabled: true,
  remark: ''
});

const canShow = (key: string) => isVisible(key);
const productDialogStyle = computed(() => ({
  '--product-dialog-width': `${productDialogState.width}px`,
  '--product-dialog-height': `${productDialogState.height}px`,
  '--product-dialog-top': `${productDialogState.top}px`,
  '--product-dialog-left': `${productDialogState.left}px`
}));
const hasPermission = (code: string) => authStore.hasPermission(code) || authStore.hasPermission(`PERM_${code}`);
const canViewCostPrice = computed(() => hasPermission('erp-product:cost:view') || hasPermission('erp-product:cost:edit'));
const canEditCostPrice = computed(() => hasPermission('erp-product:cost:edit'));
const canViewPurchaseHistory = computed(() => (
  hasPermission('erp-purchase-draft:view')
  || hasPermission('erp-purchase-approved:view')
  || hasPermission('erp-purchase:view')
));
const historyProductName = computed(() => historyProduct.value?.name || '-');
const purchaseOrderDetailTitle = computed(() => {
  const orderNo = purchaseOrderDetail.value?.order?.orderNo;
  return orderNo ? `${t('page.erpPurchaseOrder')} - ${orderNo}` : t('page.erpPurchaseOrder');
});
const purchaseOrderDetailStatusType = computed(() => {
  const status = purchaseOrderDetail.value?.order?.status;
  if (status === 'APPROVED') return 'success';
  if (status === 'CANCELLED') return 'danger';
  return 'info';
});

const getCategoryName = (id?: number) => categoryOptions.value.find(item => item.id === id)?.name || '-';
const getUnitName = (id?: number) => unitOptions.value.find(item => item.id === id)?.name || '-';
const getWarehouseName = (id?: number) => warehouseOptions.value.find(item => item.id === id)?.name || '-';
const getLocationName = (id?: number) => locationOptions.value.find(item => item.id === id)?.name || '-';
const getSourceSupplierName = (id?: number) => supplierOptions.value.find(item => item.id === id)?.name || '-';
const formatMoney = (value?: number) => {
  if (value == null || Number.isNaN(value)) return '-';
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 });
};
const formatProductType = (value?: string) => value === 'ASSEMBLY' ? '组装商品' : '普通商品';
const getLocationOptions = (warehouseId?: number | null) => {
  if (!warehouseId) return locationOptions.value;
  return locationOptions.value.filter(item => item.warehouseId === warehouseId);
};

const createEmptyStockPolicy = (): ProductStockPolicy => ({
  warehouseId: null,
  safetyStock: '',
  minStock: '',
  maxStock: ''
});

interface ProductDialogViewportBounds {
  minWidth: number;
  maxWidth: number;
  minHeight: number;
  maxHeight: number;
  padding: number;
}

const getProductDialogViewportBounds = (): ProductDialogViewportBounds => {
  if (typeof window === 'undefined') {
    return {
      minWidth: productDialogState.width,
      maxWidth: productDialogState.width,
      minHeight: productDialogState.height,
      maxHeight: productDialogState.height,
      padding: 0
    };
  }

  const padding = 16;
  const minWidth = Math.min(760, window.innerWidth - padding * 2);
  const maxWidth = Math.max(minWidth, window.innerWidth - padding * 2);
  const minHeight = Math.min(560, window.innerHeight - padding * 2);
  const maxHeight = Math.max(minHeight, window.innerHeight - padding * 2);

  return {
    minWidth: Math.max(360, minWidth),
    maxWidth,
    minHeight: Math.max(460, minHeight),
    maxHeight,
    padding
  };
};

const applyProductDialogBounds = (
  nextWidth: number,
  nextHeight: number,
  nextTop: number,
  nextLeft: number
) => {
  if (typeof window === 'undefined') return;
  const { minWidth, maxWidth, minHeight, maxHeight, padding } = getProductDialogViewportBounds();
  const width = Math.min(Math.max(nextWidth, minWidth), maxWidth);
  const height = Math.min(Math.max(nextHeight, minHeight), maxHeight);
  const maxLeft = Math.max(padding, window.innerWidth - width - padding);
  const maxTop = Math.max(padding, window.innerHeight - height - padding);

  productDialogState.width = Math.round(width);
  productDialogState.height = Math.round(height);
  productDialogState.left = Math.round(Math.min(Math.max(nextLeft, padding), maxLeft));
  productDialogState.top = Math.round(Math.min(Math.max(nextTop, padding), maxTop));
};

const centerProductDialog = () => {
  if (typeof window === 'undefined') return;
  const { maxWidth, maxHeight } = getProductDialogViewportBounds();
  const width = Math.min(1040, maxWidth);
  const height = Math.min(680, maxHeight);
  const left = Math.max(16, Math.round((window.innerWidth - width) / 2));
  const top = Math.max(24, Math.round((window.innerHeight - height) / 2) - 24);
  applyProductDialogBounds(width, height, top, left);
};

const clearProductDialogResizeListeners = () => {
  if (typeof window === 'undefined') return;
  window.removeEventListener('pointermove', handleProductDialogResizePointerMove);
  window.removeEventListener('pointerup', stopProductDialogResize);
  window.removeEventListener('pointercancel', stopProductDialogResize);
};

const resolveProductDialogElement = () => document.querySelector('.el-dialog.product-dialog') as HTMLElement | null;

const updateProductDialogResizeHandleTeleportTarget = () => {
  if (typeof window === 'undefined') return;
  productDialogResizeHandleTeleportTarget.value = resolveProductDialogElement();
};

const syncProductDialogPositionFromTransform = () => {
  if (typeof window === 'undefined') return;
  const dialogElement = resolveProductDialogElement();
  if (!dialogElement) return;
  const computedStyle = window.getComputedStyle(dialogElement);
  const transform = computedStyle.transform;
  if (!transform || transform === 'none') return;

  const matrix = new DOMMatrixReadOnly(transform);
  if (!matrix.m41 && !matrix.m42) return;

  applyProductDialogBounds(
    productDialogState.width,
    productDialogState.height,
    productDialogState.top + matrix.m42,
    productDialogState.left + matrix.m41
  );
  dialogElement.style.transform = 'none';
};

const syncProductDialogStateFromDom = () => {
  if (typeof window === 'undefined') return;
  const dialogElement = resolveProductDialogElement();
  if (!dialogElement) return;
  const rect = dialogElement.getBoundingClientRect();
  applyProductDialogBounds(rect.width, rect.height, rect.top, rect.left);
};

const startProductDialogResize = (direction: 'nw' | 'ne' | 'sw' | 'se', event: PointerEvent) => {
  event.preventDefault();
  event.stopPropagation();
  syncProductDialogPositionFromTransform();
  syncProductDialogStateFromDom();
  activeProductDialogResizePointerId.value = event.pointerId;
  productDialogResizeState.value = {
    direction,
    startX: event.clientX,
    startY: event.clientY,
    startWidth: productDialogState.width,
    startHeight: productDialogState.height,
    startTop: productDialogState.top,
    startLeft: productDialogState.left
  };
  window.addEventListener('pointermove', handleProductDialogResizePointerMove);
  window.addEventListener('pointerup', stopProductDialogResize);
  window.addEventListener('pointercancel', stopProductDialogResize);
};

const handleProductDialogResizePointerMove = (event: PointerEvent) => {
  if (!productDialogResizeState.value) return;
  if (
    activeProductDialogResizePointerId.value !== null
    && event.pointerId !== activeProductDialogResizePointerId.value
  ) {
    return;
  }

  const deltaX = event.clientX - productDialogResizeState.value.startX;
  const deltaY = event.clientY - productDialogResizeState.value.startY;
  let width = productDialogResizeState.value.startWidth;
  let height = productDialogResizeState.value.startHeight;
  let top = productDialogResizeState.value.startTop;
  let left = productDialogResizeState.value.startLeft;

  if (productDialogResizeState.value.direction.includes('e')) {
    width = productDialogResizeState.value.startWidth + deltaX;
  }
  if (productDialogResizeState.value.direction.includes('s')) {
    height = productDialogResizeState.value.startHeight + deltaY;
  }
  if (productDialogResizeState.value.direction.includes('w')) {
    width = productDialogResizeState.value.startWidth - deltaX;
    left = productDialogResizeState.value.startLeft + deltaX;
  }
  if (productDialogResizeState.value.direction.includes('n')) {
    height = productDialogResizeState.value.startHeight - deltaY;
    top = productDialogResizeState.value.startTop + deltaY;
  }

  applyProductDialogBounds(width, height, top, left);
};

const stopProductDialogResize = (event?: PointerEvent) => {
  if (
    event &&
    activeProductDialogResizePointerId.value !== null &&
    event.pointerId !== activeProductDialogResizePointerId.value
  ) {
    return;
  }
  productDialogResizeState.value = null;
  activeProductDialogResizePointerId.value = null;
  clearProductDialogResizeListeners();
};

const normalizeDateTimeValue = (value: any) => {
  if (!value) return '-';
  if (typeof value === 'string') {
    const normalized = value.replace('T', ' ').replace(/\.\d{3}Z$/, '').replace(/Z$/, '');
    return normalized;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};
const formatHistoryDate = (value: any) => normalizeDateTimeValue(value);
const formatPurchaseOrderDetailNumber = (value?: number | string) => {
  if (value == null || value === '') return '-';
  const parsed = Number(value);
  if (Number.isNaN(parsed)) return String(value);
  return Number.isInteger(parsed) ? String(parsed) : parsed.toString();
};
const formatPurchaseOrderStatus = (status?: string) => {
  const mapping: Record<string, string> = {
    DRAFT: t('status.draft'),
    APPROVED: t('status.approved'),
    CANCELLED: t('status.cancelled')
  };
  if (!status) return '-';
  return mapping[status] || status;
};
const getPurchaseOrderDetailProductName = (productId?: number) => {
  if (!productId) return '-';
  return purchaseOrderProductNameMap.value[productId] || `#${productId}`;
};
const calcPurchaseOrderDetailLineTotal = (row: PurchaseOrderDetailItem) => {
  const qty = Number(row.qty ?? 0);
  const price = Number(row.price ?? 0);
  if (Number.isNaN(qty) || Number.isNaN(price)) return 0;
  return qty * price;
};

const normalizeArray = <T>(value: any): T[] => {
  if (Array.isArray(value)) return value as T[];
  if (Array.isArray(value?.items)) return value.items as T[];
  if (Array.isArray(value?.list)) return value.list as T[];
  return [];
};

const normalizeHistoryKeyword = (value: string) => value.trim();

const resolveHistoryRange = (range: string[]) => {
  if (!range || range.length < 2) return null;
  const [start, end] = range;
  if (!start || !end) return null;
  const startDate = new Date(`${start}T00:00:00`);
  const endDate = new Date(`${end}T23:59:59.999`);
  if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime())) return null;
  return {
    startAt: startDate.toISOString(),
    endAt: endDate.toISOString()
  };
};

const buildPurchaseHistoryParams = (pageNo: number, pageSize: number) => {
  const productId = historyProduct.value?.id;
  if (!productId) return null;
  const params: Record<string, any> = {
    productId,
    page: pageNo,
    size: pageSize
  };
  const keyword = normalizeHistoryKeyword(purchaseHistoryKeyword.value);
  if (keyword) params.keyword = keyword;
  const range = resolveHistoryRange(purchaseHistoryRange.value);
  if (range) {
    params.startAt = range.startAt;
    params.endAt = range.endAt;
  }
  return params;
};

const parseExtAttrs = (raw: unknown): CustomField[] => {
  if (raw == null || raw === '') return [];
  let data: unknown = raw;
  if (typeof raw === 'string') {
    const trimmed = raw.trim();
    if (!trimmed) return [];
    try {
      data = JSON.parse(trimmed);
    } catch (error) {
      console.warn('Invalid extAttrs JSON:', error);
      return [];
    }
  }
  if (Array.isArray(data)) {
    return data
      .filter(item => item && typeof item === 'object')
      .map(item => ({
        key: String((item as any).key ?? ''),
        value: (item as any).value == null ? '' : String((item as any).value)
      }))
      .filter(item => item.key);
  }
  if (data && typeof data === 'object') {
    return Object.entries(data as Record<string, unknown>).map(([key, value]) => ({
      key,
      value: value == null ? '' : String(value)
    }));
  }
  return [];
};

const buildExtAttrsPayload = (): string | null => {
  const entries = customFields.value
    .map(item => ({
      key: item.key?.trim() ?? '',
      value: item.value == null ? '' : String(item.value)
    }))
    .filter(item => item.key);
  if (!entries.length) return null;
  const payload: Record<string, string> = {};
  entries.forEach(item => {
    payload[item.key] = item.value;
  });
  return JSON.stringify(payload);
};

const addCustomField = () => {
  customFields.value.push({ key: '', value: '' });
};

const removeCustomField = (index: number) => {
  customFields.value.splice(index, 1);
};

const buildPriceItems = () => {
  priceItems.value = customerCategoryOptions.value.map(item => ({
    categoryId: item.id,
    categoryName: item.name,
    salePrice: currentPriceMap.value.get(item.id) || ''
  }));
};

const syncDefaultLocation = () => {
  if (!formData.defaultLocationId) return;
  const location = locationOptions.value.find(item => item.id === formData.defaultLocationId);
  if (location && formData.defaultWarehouseId && location.warehouseId !== formData.defaultWarehouseId) {
    formData.defaultLocationId = null;
  }
};

const applyDefaultWarehouseAndLocation = () => {
  if (!formData.defaultWarehouseId) {
    const defaultWarehouse = warehouseOptions.value.find(item => item.isDefault);
    if (defaultWarehouse) {
      formData.defaultWarehouseId = defaultWarehouse.id;
    }
  }
  if (!formData.defaultWarehouseId || formData.defaultLocationId) return;
  const defaultLocation = locationOptions.value.find(item => item.isDefault && item.warehouseId === formData.defaultWarehouseId);
  if (defaultLocation) {
    formData.defaultLocationId = defaultLocation.id;
  }
};

const fetchCategories = async () => {
  try {
    categoryOptions.value = await getCachedCategories(tenantCacheKey.value);
  } catch (error) {
    notifyError(error);
  }
};

const fetchCustomerCategories = async () => {
  try {
    customerCategoryOptions.value = await getCachedCustomerCategories(tenantCacheKey.value);
    buildPriceItems();
  } catch (error) {
    notifyError(error);
  }
};

const fetchSuppliers = async () => {
  try {
    supplierOptions.value = await getCachedSuppliers(tenantCacheKey.value);
  } catch (error) {
    notifyError(error);
  }
};

const fetchUnits = async () => {
  try {
    unitOptions.value = await getCachedUnits(tenantCacheKey.value);
  } catch (error) {
    notifyError(error);
  }
};

const fetchWarehouses = async () => {
  try {
    warehouseOptions.value = await getCachedWarehouseOptions(tenantCacheKey.value);
    if (!isEditing.value) {
      applyDefaultWarehouseAndLocation();
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchLocations = async () => {
  try {
    locationOptions.value = await getCachedLocationOptions(tenantCacheKey.value);
    syncDefaultLocation();
    if (!isEditing.value) {
      applyDefaultWarehouseAndLocation();
    }
  } catch (error) {
    notifyError(error);
  }
};

const ensureWarehouseOption = async (warehouseId?: number | null) => {
  if (!warehouseId || warehouseOptions.value.some(item => item.id === warehouseId)) return;
  try {
    const res: any = await request.get(`/erp/warehouses/${warehouseId}`);
    const warehouse = res.data.data;
    if (warehouse) {
      warehouseOptions.value = mergeOptionById(warehouseOptions.value, {
        id: warehouse.id,
        name: warehouse.name,
        isDefault: warehouse.isDefault
      });
    }
  } catch (error) {
    notifyError(error);
  }
};

const ensureLocationOption = async (locationId?: number | null) => {
  if (!locationId || locationOptions.value.some(item => item.id === locationId)) return;
  try {
    const res: any = await request.get(`/erp/locations/${locationId}`);
    const location = res.data.data;
    if (location) {
      locationOptions.value = mergeOptionById(locationOptions.value, {
        id: location.id,
        name: location.name,
        warehouseId: location.warehouseId,
        isDefault: location.isDefault
      });
    }
  } catch (error) {
    notifyError(error);
  }
};

const ensurePurchaseOrderDetailProducts = async (items: PurchaseOrderDetailItem[]) => {
  const missingIds = Array.from(new Set(
    items
      .map(item => item.productId)
      .filter((id): id is number => typeof id === 'number' && Number.isFinite(id) && !purchaseOrderProductNameMap.value[id])
  ));
  if (!missingIds.length) return;
  const loaded = await Promise.all(missingIds.map(async (productId) => {
    try {
      const product = await fetchProductDetail(productId);
      return { id: productId, name: product.name || `#${productId}` };
    } catch (error) {
      notifyError(error);
      return { id: productId, name: `#${productId}` };
    }
  }));
  const nextMap = { ...purchaseOrderProductNameMap.value };
  loaded.forEach(item => {
    nextMap[item.id] = item.name;
  });
  purchaseOrderProductNameMap.value = nextMap;
};

const fetchNextCode = async () => {
  try {
    const res: any = await request.get('/erp/products/next-code');
    if (res.data.code === 200 && res.data.data) {
      formData.code = res.data.data;
    }
  } catch (error) {
    notifyError(error);
  }
};

const fetchProductPrices = async (productId: number) => {
  try {
    const res: any = await request.get('/erp/product-prices', { params: { productId } });
    const items = res.data.data || [];
    currentPriceMap.value = new Map(items.map((item: any) => [item.customerCategoryId, String(item.salePrice ?? '')]));
    buildPriceItems();
  } catch (error) {
    notifyError(error);
  }
};

const fetchProductDetail = async (productId: number) => {
  const res: any = await request.get(`/erp/products/${productId}`);
  if (res.data.code !== 200 || !res.data.data) {
    throw new Error(t('message.loadFailed'));
  }
  return res.data.data as ErpProduct;
};

const applySearch = () => {
  let filtered = allTableData.value.slice();
  if (statusFilter.value !== 'all') filtered = filtered.filter(row => row.enabled === (statusFilter.value === 'enabled'));
  if (categoryFilter.value) filtered = filtered.filter(row => row.categoryId === categoryFilter.value);
  filtered = filterByFuzzyKeyword(filtered, nameQuery.value, row => [row.name]);
  filtered = filterByFuzzyKeyword(filtered, codeQuery.value, row => [row.code]);
  filtered = filterByFuzzyKeyword(filtered, shortNameQuery.value, row => [row.shortName]);
  filtered = filterByFuzzyKeyword(filtered, barcodeQuery.value, row => [row.barcode]);
  total.value = filtered.length;
  const start = (page.value - 1) * size.value;
  tableData.value = filtered.slice(start, start + size.value);
};

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await request.get('/erp/products');
    if (res.data.code === 200) {
      allTableData.value = res.data.data || [];
      applySearch();
    }
  } catch (error) {
    notifyError(error);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  page.value = 1;
  fetchList();
};

const handlePageChange = (newPage: number) => {
  page.value = newPage;
  applySearch();
};

const handleSizeChange = (newSize: number) => {
  size.value = newSize;
  page.value = 1;
  applySearch();
};

const fetchPurchaseHistory = async (pageNo = purchaseHistoryPage.value) => {
  const params = buildPurchaseHistoryParams(pageNo, purchaseHistorySize.value);
  if (!params) {
    purchaseHistoryItems.value = [];
    purchaseHistoryTotal.value = 0;
    return;
  }
  purchaseHistoryLoading.value = true;
  try {
    const res: any = await request.get('/erp/purchase-orders/product-history', { params });
    const data = res?.data?.data || {};
    purchaseHistoryItems.value = normalizeArray<PurchaseHistoryItem>(data);
    purchaseHistoryTotal.value = data.total || 0;
    purchaseHistoryPage.value = data.page || pageNo;
    purchaseHistorySize.value = data.size || purchaseHistorySize.value;
  } catch (error) {
    purchaseHistoryItems.value = [];
    purchaseHistoryTotal.value = 0;
    notifyError(error);
  } finally {
    purchaseHistoryLoading.value = false;
  }
};

const openPurchaseHistory = async (row: ErpProduct) => {
  if (!canViewPurchaseHistory.value || !row?.id) return;
  historyProduct.value = row;
  purchaseHistoryKeyword.value = '';
  purchaseHistoryRange.value = [];
  purchaseHistoryPage.value = 1;
  purchaseHistorySize.value = 10;
  purchaseHistoryDialogVisible.value = true;
  await fetchPurchaseHistory(1);
};

const handlePurchaseHistoryPageChange = (newPage: number) => {
  purchaseHistoryPage.value = newPage;
  fetchPurchaseHistory(newPage);
};

const handlePurchaseHistorySizeChange = (newSize: number) => {
  purchaseHistorySize.value = newSize;
  purchaseHistoryPage.value = 1;
  fetchPurchaseHistory(1);
};

const openPurchaseOrderDetail = async (row: PurchaseHistoryItem) => {
  if (!row?.orderId) return;
  purchaseOrderDetailDialogVisible.value = true;
  purchaseOrderDetailLoading.value = true;
  purchaseOrderDetail.value = null;
  purchaseOrderDetailSupplierName.value = row.supplierName || '-';
  try {
    const res: any = await request.get(`/erp/purchase-orders/${row.orderId}`);
    const data = res?.data?.data;
    const order = data?.order || data;
    const items = normalizeArray<PurchaseOrderDetailItem>(data);
    purchaseOrderDetail.value = {
      order,
      items
    };
    await Promise.all(items.flatMap(item => [
      ensureWarehouseOption(item.warehouseId),
      ensureLocationOption(item.locationId)
    ]));
    await ensurePurchaseOrderDetailProducts(items);
  } catch (error) {
    purchaseOrderDetailDialogVisible.value = false;
    notifyError(error);
  } finally {
    purchaseOrderDetailLoading.value = false;
  }
};

const openAddModal = () => {
  isEditing.value = false;
  currentId.value = null;
  resetForm();
  currentPriceMap.value = new Map();
  buildPriceItems();
  applyDefaultWarehouseAndLocation();
  fetchNextCode();
  showModal.value = true;
};

const addStockPolicy = () => {
  formData.stockPolicies.push(createEmptyStockPolicy());
};

const removeStockPolicy = (index: number) => {
  formData.stockPolicies.splice(index, 1);
};

const applyProductDetail = (row: ErpProduct) => {
  formData.code = row.code;
  formData.name = row.name;
  formData.shortName = row.shortName || '';
  formData.productType = row.productType || 'NORMAL';
  formData.spec = row.spec || '';
  formData.model = row.model || '';
  formData.categoryId = row.categoryId || null;
  formData.unitId = row.unitId || null;
  formData.defaultWarehouseId = row.defaultWarehouseId || null;
  formData.defaultLocationId = row.defaultLocationId || null;
  formData.barcode = row.barcode || '';
  formData.sku = row.sku || '';
  formData.brand = row.brand || '';
  formData.origin = row.origin || '';
  formData.manufacturerCode = row.manufacturerCode || '';
  formData.manufacturerModel = row.manufacturerModel || '';
  formData.manufacturerName = row.manufacturerName || '';
  formData.sourceSupplierId = row.sourceSupplierId || null;
  formData.weight = row.weight == null ? '' : String(row.weight);
  formData.volume = row.volume == null ? '' : String(row.volume);
  formData.salePrice = row.salePrice;
  formData.costPrice = row.costPrice == null ? '' : String(row.costPrice);
  formData.taxRate = row.taxRate == null ? '' : String(row.taxRate);
  formData.safetyStock = row.safetyStock == null ? '' : String(row.safetyStock);
  formData.minStock = row.minStock == null ? '' : String(row.minStock);
  formData.maxStock = row.maxStock == null ? '' : String(row.maxStock);
  formData.stockPolicies = (row.stockPolicies || []).map(item => ({
    warehouseId: item.warehouseId ?? null,
    safetyStock: item.safetyStock == null ? '' : String(item.safetyStock),
    minStock: item.minStock == null ? '' : String(item.minStock),
    maxStock: item.maxStock == null ? '' : String(item.maxStock)
  }));
  formData.batch = !!row.batch;
  formData.shelfLifeDays = row.shelfLifeDays == null ? '' : String(row.shelfLifeDays);
  formData.enabled = row.enabled;
  formData.remark = row.remark || '';
  customFields.value = parseExtAttrs(row.extAttrs);
};

const openEditModal = async (row: ErpProduct) => {
  try {
    isEditing.value = true;
    currentId.value = row.id;
    const detail = await fetchProductDetail(row.id);
    applyProductDetail(detail);
    await ensureWarehouseOption(formData.defaultWarehouseId);
    await ensureLocationOption(formData.defaultLocationId);
    syncDefaultLocation();
    currentPriceMap.value = new Map();
    if (row.id) {
      await fetchProductPrices(row.id);
    } else {
      buildPriceItems();
    }
    showModal.value = true;
  } catch (error) {
    notifyError(error);
  }
};

const resetForm = () => {
  formData.code = '';
  formData.name = '';
  formData.shortName = '';
  formData.productType = 'NORMAL';
  formData.spec = '';
  formData.model = '';
  formData.categoryId = null;
  formData.unitId = null;
  formData.defaultWarehouseId = null;
  formData.defaultLocationId = null;
  formData.barcode = '';
  formData.sku = '';
  formData.brand = '';
  formData.origin = '';
  formData.manufacturerCode = '';
  formData.manufacturerModel = '';
  formData.manufacturerName = '';
  formData.sourceSupplierId = null;
  formData.weight = '';
  formData.volume = '';
  formData.salePrice = undefined;
  formData.costPrice = '';
  formData.taxRate = '';
  formData.safetyStock = '';
  formData.minStock = '';
  formData.maxStock = '';
  formData.stockPolicies = [];
  formData.batch = false;
  formData.shelfLifeDays = '';
  formData.enabled = true;
  formData.remark = '';
  customFields.value = [];
};

const handleProductDialogClosed = () => {
  stopProductDialogResize();
  productDialogResizeHandleTeleportTarget.value = null;
  resetForm();
};

const normalizeNumber = (value: string | number | null | undefined) => {
  if (value == null || value === '') return null;
  const parsed = Number(value);
  if (Number.isNaN(parsed)) return null;
  return parsed;
};

const validateStockPolicies = () => {
  const seenWarehouseIds = new Set<number>();
  for (const item of formData.stockPolicies) {
    if (!item.warehouseId) {
      notifyWarning('仓库策略必须选择仓库');
      return false;
    }
    if (seenWarehouseIds.has(item.warehouseId)) {
      notifyWarning('仓库策略中不能重复选择同一个仓库');
      return false;
    }
    const minStock = normalizeNumber(item.minStock);
    const maxStock = normalizeNumber(item.maxStock);
    if (minStock != null && maxStock != null && minStock > maxStock) {
      notifyWarning(t('message.stockLimitInvalid'));
      return false;
    }
    seenWarehouseIds.add(item.warehouseId);
  }
  return true;
};

const saveData = async () => {
  if (!formData.code || !formData.name) {
    notifyWarning(t('message.required'));
    return;
  }
  if (!validateStockPolicies()) {
    return;
  }
  try {
    const payload = {
      ...formData,
      weight: normalizeNumber(formData.weight),
      volume: normalizeNumber(formData.volume),
      costPrice: normalizeNumber(formData.costPrice),
      taxRate: normalizeNumber(formData.taxRate),
      safetyStock: normalizeNumber(formData.safetyStock),
      minStock: normalizeNumber(formData.minStock),
      maxStock: normalizeNumber(formData.maxStock),
      stockPolicies: formData.stockPolicies.map(item => ({
        warehouseId: item.warehouseId,
        safetyStock: normalizeNumber(item.safetyStock),
        minStock: normalizeNumber(item.minStock),
        maxStock: normalizeNumber(item.maxStock)
      })),
      shelfLifeDays: normalizeNumber(formData.shelfLifeDays),
      extAttrs: buildExtAttrsPayload(),
      priceItems: priceItems.value
        .map(item => ({
          customerCategoryId: item.categoryId,
          salePrice: normalizeNumber(item.salePrice)
        }))
        .filter(item => item.salePrice != null)
    };
    const res: any = isEditing.value && currentId.value
      ? await request.put(`/erp/products/${currentId.value}`, payload)
      : await request.post('/erp/products', payload);

    if (res.data.code === 200) {
      invalidateErpBaseDataResourceCache('productOptions', tenantCacheKey.value);
      notifySuccess();
      showModal.value = false;
      fetchList();
    }
  } catch (error) {
    notifyError(error);
  }
};

const handleDelete = async (row: ErpProduct) => {
  try {
    await request.delete(`/erp/products/${row.id}`);
    invalidateErpBaseDataResourceCache('productOptions', tenantCacheKey.value);
    notifySuccess();
    fetchList();
  } catch (error) {
    notifyError(error);
  }
};

const resetProductImportForm = () => {
  productImportSourceName.value = '';
  productImportFile.value = null;
  productImportPreview.value = null;
  productImportFieldOptions.value = [];
  productImportMappings.value = [];
  productImportMappingConfirmed.value = false;
  if (productImportInputRef.value) {
    productImportInputRef.value.value = '';
  }
};

const openProductImportDialog = () => {
  resetProductImportForm();
  showProductImportDialog.value = true;
};

const openProductImportHistoryDrawer = async () => {
  showProductImportHistoryDrawer.value = true;
  await loadProductImportBatches();
};

const handleProductImportFile = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  productImportFile.value = input.files?.[0] || null;
  productImportPreview.value = null;
  productImportFieldOptions.value = [];
  productImportMappings.value = [];
  productImportMappingConfirmed.value = false;
  if (productImportFile.value) {
    await loadProductImportPreview();
  }
};

const loadProductImportPreview = async () => {
  if (!productImportFile.value) return;
  productImportPreviewLoading.value = true;
  try {
    const formData = new FormData();
    formData.append('file', productImportFile.value);
    const res: any = await request.post('/erp/products/import/preview', formData);
    if (res.data.code === 200) {
      productImportPreview.value = res.data.data as ProductImportPreview;
      productImportFieldOptions.value = productImportPreview.value.fields || [];
      productImportMappings.value = (productImportPreview.value.mappings || []).map(item => ({ ...item }));
      productImportMappingConfirmed.value = false;
    }
  } catch (error) {
    notifyError(error);
  } finally {
    productImportPreviewLoading.value = false;
  }
};

const buildProductImportFieldMapping = () => {
  const mapping: Record<string, string> = {};
  productImportMappings.value.forEach(item => {
    if (item.excelHeader && item.fieldKey) {
      mapping[item.excelHeader] = item.fieldKey;
    }
  });
  return mapping;
};

const validateProductImportRequiredMappings = () => {
  const selectedFields = new Set(productImportMappings.value.map(item => item.fieldKey).filter(Boolean));
  const missing = productImportFieldOptions.value
    .filter(item => item.required && !selectedFields.has(item.key))
    .map(item => item.label);
  if (missing.length) {
    notifyError(`请先完成必填字段映射：${missing.join('、')}`);
    return false;
  }
  return true;
};

const handleProductImportMappingChange = (row: ProductImportHeaderMapping) => {
  row.matchType = row.fieldKey ? 'MANUAL' : 'UNMATCHED';
  productImportMappingConfirmed.value = false;
};

const confirmProductImportMapping = () => {
  if (!productImportFile.value) {
    notifyError('请先选择要导入的 Excel 文件');
    return;
  }
  if (!validateProductImportRequiredMappings()) {
    return;
  }
  productImportMappingConfirmed.value = true;
  notifySuccess('字段映射已确认');
};

const loadProductImportBatches = async () => {
  productImportHistoryLoading.value = true;
  try {
    const res: any = await request.get('/erp/products/import-batches');
    if (res.data.code === 200) {
      productImportBatches.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  } finally {
    productImportHistoryLoading.value = false;
  }
};

const viewProductImportBatchItems = async (batch: ProductImportBatchSummary) => {
  selectedProductImportBatch.value = batch;
  productImportItemsLoading.value = true;
  try {
    const res: any = await request.get(`/erp/products/import-batches/${batch.id}/items`);
    if (res.data.code === 200) {
      productImportBatchItems.value = res.data.data || [];
    }
  } catch (error) {
    notifyError(error);
  } finally {
    productImportItemsLoading.value = false;
  }
};

const stopProductImportPolling = () => {
  if (productImportPollingTimer.value != null) {
    window.clearTimeout(productImportPollingTimer.value);
    productImportPollingTimer.value = null;
  }
  activeProductImportBatchId.value = null;
};

const pollProductImportBatch = async (batchId: number) => {
  await loadProductImportBatches();
  const batch = productImportBatches.value.find(item => item.id === batchId) || null;
  if (!batch) {
    stopProductImportPolling();
    return;
  }
  selectedProductImportBatch.value = batch;
  await viewProductImportBatchItems(batch);
  if (batch.status === 'PROCESSING') {
    productImportPollingTimer.value = window.setTimeout(() => {
      void pollProductImportBatch(batchId);
    }, 1500);
    return;
  }
  stopProductImportPolling();
  notifySuccess(batch.summary || `导入完成：成功 ${batch.successCount} 行，失败 ${batch.failedCount} 行`);
  fetchList();
};

const startProductImportPolling = (batchId: number) => {
  stopProductImportPolling();
  activeProductImportBatchId.value = batchId;
  productImportPollingTimer.value = window.setTimeout(() => {
    void pollProductImportBatch(batchId);
  }, 1000);
};

const submitProductImport = async () => {
  if (!productImportFile.value) {
    notifyError('请先选择要导入的 Excel 文件');
    return;
  }
  if (!productImportMappingConfirmed.value) {
    notifyError('请先确认字段映射');
    return;
  }
  if (!validateProductImportRequiredMappings()) {
    return;
  }
  productImportSubmitting.value = true;
  try {
    const formData = new FormData();
    formData.append('file', productImportFile.value);
    formData.append('fieldMapping', JSON.stringify(buildProductImportFieldMapping()));
    const trimmedSourceName = productImportSourceName.value.trim();
    if (trimmedSourceName) {
      formData.append('sourceName', trimmedSourceName);
    }
    const res: any = await request.post('/erp/products/import', formData);
    if (res.data.code === 200) {
      const result = res.data.data as ProductImportResult;
      notifySuccess(`导入任务已创建：批次 ${result.batchNo}`);
      showProductImportDialog.value = false;
      await openProductImportHistoryDrawer();
      resetProductImportForm();
      startProductImportPolling(result.batchId);
    }
  } catch (error) {
    notifyError(error);
  } finally {
    productImportSubmitting.value = false;
  }
};

watch([purchaseHistoryKeyword, purchaseHistoryRange], () => {
  purchaseHistoryPage.value = 1;
  if (purchaseHistoryDialogVisible.value) {
    fetchPurchaseHistory(1);
  }
}, { deep: true });

watch(showModal, (visible) => {
  if (!visible) {
    productDialogResizeHandleTeleportTarget.value = null;
    return;
  }
  nextTick(() => {
    updateProductDialogResizeHandleTeleportTarget();
    centerProductDialog();
  });
});

bindPageSizeSync(size, fetchList, {
  reloadOnInitialSync: false,
  onInitialSyncComplete: () => {
    pageSizeSyncReady.value = true;
    if (pendingInitialLoad.value && firstPaintReady.value) {
      pendingInitialLoad.value = false;
      fetchList();
    }
  }
});

onMounted(async () => {
  await waitForErpFirstPaint();
  firstPaintReady.value = true;
  fetchCategories();
  fetchCustomerCategories();
  fetchSuppliers();
  fetchUnits();
  fetchWarehouses();
  fetchLocations();
  fetchTenantKeys();
  if (pageSizeSyncReady.value) {
    fetchList();
  } else {
    pendingInitialLoad.value = true;
  }
});

onActivated(() => {
  if (!hasActivatedOnce.value) {
    hasActivatedOnce.value = true;
    return;
  }
  fetchList();
});

onBeforeUnmount(() => {
  stopProductImportPolling();
  stopProductDialogResize();
});
</script>

<style scoped>
.cost-price-trigger {
  padding: 0;
  min-height: auto;
  font-weight: 500;
}

.history-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
}

.history-order-link {
  padding: 0;
  min-height: auto;
}

.history-header {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  margin-bottom: 12px;
  color: #2c3e50;
}

.history-header__item {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.history-toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.purchase-order-detail-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
}

.purchase-detail-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px 16px;
  margin-bottom: 12px;
}

.purchase-detail-summary__item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #2c3e50;
}

.purchase-detail-remark {
  margin-bottom: 14px;
  color: #4b5563;
  line-height: 1.6;
}

.history-search {
  width: 220px;
}

.history-date {
  width: 260px;
}

.history-pagination {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.drawer-form {
  padding-right: 16px;
}

.module-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.form-section {
  margin-bottom: 24px;
}

.form-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-top: 24px;
  margin-bottom: 12px;
}

.module-card .section-title {
  margin-top: 0;
}

.product-price-card__span-offset {
  margin-left: auto;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
}

.custom-field-table {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.custom-field-table :deep(.el-table) {
  width: 100%;
}

.add-custom-field {
  align-self: flex-start;
}
</style>
