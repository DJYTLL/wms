<template>
  <AsyncErpRouteShell title="采购退货（草稿）" :loader="loadView" />
</template>

<script setup lang="ts">
import { h } from 'vue';
import AsyncErpRouteShell from './AsyncErpRouteShell.vue';

const loadView = async () => {
  const component = await import('./ErpPurchaseReturnManagement.vue');
  return {
    name: 'ErpPurchaseReturnDraftRouteView',
    render() {
      return h(component.default, { workspace: 'draft' });
    }
  };
};
</script>
