import { ref } from 'vue';
import { defineStore } from 'pinia';
import request from '@/utils/request';
import { useAuthStore } from '@/stores/auth';

export interface MenuItem {
  id: number;
  key?: string;
  title?: string;
  path?: string;
  icon?: string;
  permissionCode?: string | null;
  children?: MenuItem[];
}

export const useMenuStore = defineStore('menu', () => {
  const menus = ref<MenuItem[]>([]);
  const loading = ref(false);
  const loadedTenantCode = ref<string | null>(null);
  const loadedUserKey = ref<string | null>(null);

  const clearMenus = () => {
    menus.value = [];
    loadedTenantCode.value = null;
    loadedUserKey.value = null;
  };

  const fetchMenus = async (force = false) => {
    const authStore = useAuthStore();
    const currentTenantCode = authStore.tenantCode || null;
    const currentUserKey = `${currentTenantCode || ''}:${authStore.user?.username || ''}`;
    const hasToken = !!authStore.token;

    if (!hasToken) {
      clearMenus();
      return menus.value;
    }

    if (!force && menus.value.length > 0 && loadedTenantCode.value === currentTenantCode
      && loadedUserKey.value === currentUserKey) {
      return menus.value;
    }

    loading.value = true;
    try {
      const res: any = await request.get('/menus');
      if (res.data.code === 200) {
        const rawMenus = (res.data.data || []) as MenuItem[];
        menus.value = filterMenusByPermission(rawMenus, authStore);
        loadedTenantCode.value = currentTenantCode;
        loadedUserKey.value = currentUserKey;
      }
      return menus.value;
    } finally {
      loading.value = false;
    }
  };

  const filterMenusByPermission = (items: MenuItem[], authStore: ReturnType<typeof useAuthStore>): MenuItem[] => {
    return items.reduce<MenuItem[]>((acc, item) => {
      const requiredPermission = item.permissionCode;
      const hasPermission = !requiredPermission || authStore.hasPermission(requiredPermission);
      if (!hasPermission) {
        return acc;
      }

      if (item.children && item.children.length > 0) {
        const filteredChildren = filterMenusByPermission(item.children, authStore);
        if (filteredChildren.length > 0) {
          acc.push({ ...item, children: filteredChildren });
          return acc;
        }
        if (item.path) {
          acc.push({ ...item, children: [] });
        }
        return acc;
      }

      acc.push(item);
      return acc;
    }, []);
  };

  return {
    menus,
    loading,
    fetchMenus,
    clearMenus,
  };
});
