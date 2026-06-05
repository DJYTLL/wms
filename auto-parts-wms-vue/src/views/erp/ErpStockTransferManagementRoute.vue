<template>
  <AsyncErpRouteShell title="库存移库" :loader="loadView" variant="inventory" />
</template>

<script setup lang="ts">
import AsyncErpRouteShell from './AsyncErpRouteShell.vue';

const loadView = () => import('./ErpStockTransferManagement.vue');
</script>
