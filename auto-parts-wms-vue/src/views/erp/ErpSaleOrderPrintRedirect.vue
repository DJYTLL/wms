<template>
  <div />
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';

const route = useRoute();
const router = useRouter();

onMounted(async () => {
  const id = route.params.id;
  const res: any = await request.get(`/erp/sale-orders/${id}`);
  const status = res.data?.data?.order?.status;
  const target = status === 'DRAFT'
    ? `/erp/sale-orders/draft/${id}/print`
    : `/erp/sale-orders/approved/${id}/print`;
  await router.replace({ path: target, query: route.query });
});
</script>
