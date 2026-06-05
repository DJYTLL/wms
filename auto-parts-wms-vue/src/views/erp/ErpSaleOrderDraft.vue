<template>
  <AsyncErpRouteShell title="销售单（草稿）" :loader="loadView" />
</template>

<script setup lang="ts">
import AsyncErpRouteShell from './AsyncErpRouteShell.vue';

const loadView = () => import('./ErpSaleOrderDraftManagement.vue');
</script>
