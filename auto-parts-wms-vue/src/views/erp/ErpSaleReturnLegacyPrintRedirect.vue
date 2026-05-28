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

function buildPrintPath(workspace: 'draft' | 'approved', id: string) {
  return `/erp/sale-returns/${workspace}/${id}/print`;
}

onMounted(async () => {
  const id = String(route.params.id ?? '');
  const canDraftPrint = hasAnyPermission(['erp-sale-return-draft:print']);
  const canApprovedPrint = hasAnyPermission(['erp-sale-return-approved:print']);
  const canReadStatus = hasAnyPermission([
    'erp-sale-return-draft:view',
    'erp-sale-return-approved:view'
  ]);

  try {
    if (!canReadStatus && canDraftPrint !== canApprovedPrint) {
      await router.replace({
        path: buildPrintPath(canDraftPrint ? 'draft' : 'approved', id),
        query: route.query
      });
      return;
    }

    const res: any = await request.get(`/erp/sale-returns/${id}`);
    const status = res.data?.data?.order?.status || res.data?.data?.status;
    const workspace = status === 'DRAFT' ? 'draft' : 'approved';
    await router.replace({
      path: buildPrintPath(workspace, id),
      query: route.query
    });
  } catch (error) {
    notifyError(error);
    const fallback = hasAnyPermission([
      'erp-sale-return-approved:view',
      'erp-sale-return-approved:copy',
      'erp-sale-return-approved:cancel',
      'erp-sale-return-approved:print'
    ])
      ? '/erp/sale-returns/approved'
      : hasAnyPermission([
        'erp-sale-return-draft:view',
        'erp-sale-return-draft:add',
        'erp-sale-return-draft:edit',
        'erp-sale-return-draft:approve',
        'erp-sale-return-draft:print'
      ])
        ? '/erp/sale-returns/draft'
        : '/';
    await router.replace(fallback);
  }
});
</script>
