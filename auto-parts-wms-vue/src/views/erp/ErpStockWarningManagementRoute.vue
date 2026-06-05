<template>
  <AsyncErpRouteShell title="库存预警" :loader="loadView" variant="inventory" />
</template>

<script setup lang="ts">
import AsyncErpRouteShell from './AsyncErpRouteShell.vue';

const loadView = () => import('./ErpStockWarningManagement.vue');
</script>
