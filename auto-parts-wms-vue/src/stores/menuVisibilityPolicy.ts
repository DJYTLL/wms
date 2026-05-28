export interface MenuVisibilityItem {
  id: number;
  key?: string;
  path?: string;
  permissionCode?: string | null;
  children?: MenuVisibilityItem[];
}

export type MenuAccessChecker = {
  hasPermission: (permission: string) => boolean;
  hasRole: (role: string) => boolean;
};

const SUPER_ADMIN_ONLY_MENU_KEYS = new Set(['permission', 'menu']);

const canAccessMenuItem = (item: MenuVisibilityItem, authStore: MenuAccessChecker) => {
  if (item.key && SUPER_ADMIN_ONLY_MENU_KEYS.has(item.key) && !authStore.hasRole('super_admin')) {
    return false;
  }

  const requiredPermission = item.permissionCode;
  return !requiredPermission || authStore.hasPermission(requiredPermission);
};

export const filterMenusByPermission = (
  items: MenuVisibilityItem[],
  authStore: MenuAccessChecker,
): MenuVisibilityItem[] => {
  return items.reduce<MenuVisibilityItem[]>((acc, item) => {
    if (!canAccessMenuItem(item, authStore)) {
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
