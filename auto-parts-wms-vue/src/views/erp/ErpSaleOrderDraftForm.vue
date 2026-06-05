<template>
  <ErpSaleOrderDraftFormPage />
</template>

<script setup lang="ts">
import { defineAsyncComponent } from 'vue';

const ErpSaleOrderDraftFormPage = defineAsyncComponent(() => import('./ErpSaleOrderDraftFormPage.vue'));
</script>
