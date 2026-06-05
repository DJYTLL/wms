<template>
  <ErpSaleOrderApprovedFormPage />
</template>

<script setup lang="ts">
import { defineAsyncComponent } from 'vue';

const ErpSaleOrderApprovedFormPage = defineAsyncComponent(() => import('./ErpSaleOrderApprovedFormPage.vue'));
</script>
