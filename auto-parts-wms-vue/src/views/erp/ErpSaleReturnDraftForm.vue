<template>
  <ErpSaleReturnDraftFormPage />
</template>

<script setup lang="ts">
import { defineAsyncComponent } from 'vue';

const ErpSaleReturnDraftFormPage = defineAsyncComponent(() => import('./ErpSaleReturnDraftFormPage.vue'));
</script>
