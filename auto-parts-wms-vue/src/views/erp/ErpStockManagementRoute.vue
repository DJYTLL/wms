<template>
  <AsyncErpRouteShell title="ERP库存台账" :loader="loadView" variant="inventory" :show-action-placeholder="false" />
</template>

<script setup lang="ts">
import AsyncErpRouteShell from './AsyncErpRouteShell.vue';

const loadView = () => import('./ErpStockManagement.vue');
</script>
