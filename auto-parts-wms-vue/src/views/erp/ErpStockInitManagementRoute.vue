<template>
  <AsyncErpRouteShell title="初始库存" :loader="loadView" variant="inventory" />
</template>

<script setup lang="ts">
import AsyncErpRouteShell from './AsyncErpRouteShell.vue';

const loadView = () => import('./ErpStockCountManagement.vue');
</script>
