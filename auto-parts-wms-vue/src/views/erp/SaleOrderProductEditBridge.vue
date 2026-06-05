<template>
  <ErpProductEditDrawer
    v-model="visible"
    :product-id="productId"
    @saved="$emit('saved', $event)"
  />
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue';

const ErpProductEditDrawer = defineAsyncComponent(() => import('@/components/ErpProductEditDrawer.vue'));

interface ErpProductDetail {
  id: number;
  name: string;
  productType?: string;
  defaultWarehouseId?: number;
  defaultLocationId?: number;
  salePrice?: number;
  costPrice?: number;
  enabled?: boolean;
}

const props = defineProps<{
  modelValue: boolean;
  productId: number | null;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  saved: [value: ErpProductDetail];
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});
</script>
