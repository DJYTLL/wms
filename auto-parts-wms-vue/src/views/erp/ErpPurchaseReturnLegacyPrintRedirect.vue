<template>
  <div class="page-shell page-shell--system" />
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { useApiError } from '@/composables/useApiError';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const { notifyError } = useApiError();
const authStore = useAuthStore();

function hasAnyPermission(permissions: string[]) {
  return permissions.some((permission) => authStore.hasPermission(permission));
}

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
    const fallback = hasAnyPermission([
      'erp-purchase-return-approved:view',
      'erp-purchase-return-approved:copy',
      'erp-purchase-return-approved:cancel',
      'erp-purchase-return-approved:print'
    ])
      ? '/erp/purchase-returns/approved'
      : hasAnyPermission([
        'erp-purchase-return-draft:view',
        'erp-purchase-return-draft:add',
        'erp-purchase-return-draft:edit',
        'erp-purchase-return-draft:approve',
        'erp-purchase-return-draft:print'
      ])
        ? '/erp/purchase-returns/draft'
        : '/';
    await router.replace(fallback);
  }
});
</script>
