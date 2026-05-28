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

function buildPrintPath(workspace: 'draft' | 'approved', id: string) {
  return `/erp/sale-orders/${workspace}/${id}/print`;
}

onMounted(async () => {
  const id = String(route.params.id ?? '');
  const canDraftPrint = hasAnyPermission(['erp-sale-draft:print']);
  const canApprovedPrint = hasAnyPermission(['erp-sale-approved:print']);
  const canReadStatus = hasAnyPermission(['erp-sale:view', 'erp-sale-draft:view', 'erp-sale-approved:view']);

  try {
    if (!canReadStatus && canDraftPrint !== canApprovedPrint) {
      await router.replace({
        path: buildPrintPath(canDraftPrint ? 'draft' : 'approved', id),
        query: route.query
      });
      return;
    }

    const res: any = await request.get(`/erp/sale-orders/${id}`);
    const status = res.data?.data?.order?.status;
    const target = status === 'DRAFT'
      ? buildPrintPath('draft', id)
      : buildPrintPath('approved', id);
    await router.replace({ path: target, query: route.query });
  } catch (error) {
    notifyError(error);
    const fallback = hasAnyPermission([
      'erp-sale-approved:view',
      'erp-sale-approved:copy',
      'erp-sale-approved:cancel',
      'erp-sale-approved:print'
    ])
      ? '/erp/sale-orders/approved'
      : hasAnyPermission([
        'erp-sale:view',
        'erp-sale-draft:view',
        'erp-sale-draft:add',
        'erp-sale-draft:edit',
        'erp-sale-draft:approve',
        'erp-sale-draft:print'
      ])
        ? '/erp/sale-orders/draft'
        : '/';
    await router.replace(fallback);
  }
});
</script>
