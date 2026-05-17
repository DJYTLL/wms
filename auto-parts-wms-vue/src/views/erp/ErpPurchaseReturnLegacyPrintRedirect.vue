<template>
  <div class="page-shell page-shell--system" />
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';

const route = useRoute();
const router = useRouter();
const { notifyError } = useApiError();

onMounted(async () => {
  try {
    const id = route.params.id;
    const res: any = await request.get(`/erp/purchase-returns/${id}`);
    const status = res.data?.data?.order?.status || res.data?.data?.status;
    const workspace = status === 'DRAFT' ? 'draft' : 'approved';
    await router.replace({
      path: `/erp/purchase-returns/${workspace}/${id}/print`,
      query: route.query
    });
  } catch (error) {
    notifyError(error);
    await router.replace('/erp/purchase-returns/draft');
  }
});
</script>
