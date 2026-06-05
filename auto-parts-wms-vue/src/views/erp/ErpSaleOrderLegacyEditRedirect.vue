<template>
  <div />
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

function buildEditPath(workspace: 'draft' | 'approved', id: string) {
  return workspace === 'draft'
    ? `/erp/sale-orders/draft/${id}/edit`
    : `/erp/sale-orders/approved/${id}`;
}

onMounted(async () => {
  const id = String(route.params.id ?? '');
  const canDraftEdit = hasAnyPermission(['erp-sale-draft:edit', 'erp-sale-draft:view']);
  const canApprovedView = hasAnyPermission(['erp-sale-approved:view']);
  const canReadStatus = hasAnyPermission(['erp-sale:view', 'erp-sale-draft:view', 'erp-sale-approved:view']);

  try {
    if (!canReadStatus && canDraftEdit !== canApprovedView) {
      await router.replace({
        path: buildEditPath(canDraftEdit ? 'draft' : 'approved', id),
        query: route.query
      });
      return;
    }

    const res: any = await request.get(`/erp/sale-orders/${id}`);
    const status = res.data?.data?.order?.status;
    const target = status === 'DRAFT'
      ? buildEditPath('draft', id)
      : buildEditPath('approved', id);
    await router.replace({ path: target, query: route.query });
  } catch (error) {
    notifyError(error);
    const fallback = canApprovedView
      ? '/erp/sale-orders/approved'
      : canDraftEdit
        ? '/erp/sale-orders/draft'
        : '/';
    await router.replace(fallback);
  }
});
</script>
