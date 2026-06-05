<template>
  <ErpSaleReturnApprovedFormPage />
</template>

<script setup lang="ts">
import { defineAsyncComponent } from 'vue';

const ErpSaleReturnApprovedFormPage = defineAsyncComponent(() => import('./ErpSaleReturnApprovedFormPage.vue'));
</script>
