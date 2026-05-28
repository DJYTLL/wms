import test from 'node:test';
import assert from 'node:assert/strict';

import { filterMenusByPermission } from '../menuVisibilityPolicy.ts';

const systemMenus = [
  { id: 1, key: 'permission', title: '权限管理', path: '/permissions', permissionCode: 'permission:view' },
  { id: 2, key: 'menu', title: '菜单管理', path: '/menus', permissionCode: 'menu:view' },
  { id: 3, key: 'tenant', title: '租户管理', path: '/tenants', permissionCode: 'tenant:view' },
];

test('non super admin with system page permissions does not see permission or menu management entries', () => {
  const filtered = filterMenusByPermission(systemMenus, {
    hasPermission: (permission) => ['permission:view', 'menu:view', 'tenant:view'].includes(permission),
    hasRole: (role) => role === 'admin',
  });

  assert.deepEqual(
    filtered.map((item) => item.key),
    ['tenant'],
  );
});

test('super admin still sees permission and menu management entries', () => {
  const filtered = filterMenusByPermission(systemMenus, {
    hasPermission: () => true,
    hasRole: (role) => role === 'super_admin',
  });

  assert.deepEqual(
    filtered.map((item) => item.key),
    ['permission', 'menu', 'tenant'],
  );
});
