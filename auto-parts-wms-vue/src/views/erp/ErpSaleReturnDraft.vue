<template>
  <AsyncErpRouteShell title="销售退货（草稿）" :loader="loadView" />
</template>

<script setup lang="ts">
import AsyncErpRouteShell from './AsyncErpRouteShell.vue';

const loadView = () => import('./ErpSaleReturnDraftManagement.vue');
</script>
