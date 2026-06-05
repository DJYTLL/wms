<template>
  <AsyncErpRouteShell title="销售退货（已审核）" :loader="loadView" :show-action-placeholder="false" />
</template>

<script setup lang="ts">
import AsyncErpRouteShell from './AsyncErpRouteShell.vue';

const loadView = () => import('./ErpSaleReturnApprovedManagement.vue');
</script>
