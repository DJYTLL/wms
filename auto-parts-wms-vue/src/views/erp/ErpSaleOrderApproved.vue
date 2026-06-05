<template>
  <AsyncErpRouteShell title="销售单（已审核）" :loader="loadView" :show-action-placeholder="false" />
</template>

<script setup lang="ts">
import AsyncErpRouteShell from './AsyncErpRouteShell.vue';

const loadView = () => import('./ErpSaleOrderApprovedManagement.vue');
</script>
