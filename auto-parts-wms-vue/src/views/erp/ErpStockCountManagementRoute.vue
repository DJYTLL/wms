<template>
  <AsyncErpRouteShell title="库存调整" :loader="loadView" variant="inventory" />
</template>

<script setup lang="ts">
import AsyncErpRouteShell from './AsyncErpRouteShell.vue';

const loadView = () => import('./ErpStockCountManagement.vue');
</script>
