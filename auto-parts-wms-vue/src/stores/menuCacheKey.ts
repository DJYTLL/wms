export type MenuCacheIdentity = {
  tenantCode?: string | null;
  username?: string | null;
  authVersion?: number | string | null;
};

export const buildMenuUserKey = (identity: MenuCacheIdentity) => {
  const tenantCode = identity.tenantCode || '';
  const username = identity.username || '';
  const authVersion = identity.authVersion ?? '';
  return `${tenantCode}:${username}:${authVersion}`;
};
