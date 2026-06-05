<template>
  <AsyncErpRouteShell title="采购退货（已审核）" :loader="loadView" />
</template>

<script setup lang="ts">
import { h } from 'vue';
import AsyncErpRouteShell from './AsyncErpRouteShell.vue';

const loadView = async () => {
  const component = await import('./ErpPurchaseReturnManagement.vue');
  return {
    name: 'ErpPurchaseReturnApprovedRouteView',
    render() {
      return h(component.default, { workspace: 'approved' });
    }
  };
};
</script>
