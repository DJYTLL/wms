-- 为幂等插入提供唯一约束（不影响外键策略）
-- app_role_permission: 租户+角色+权限 唯一
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_role_permission_trp
ON app_role_permission (tenant_id, role_id, permission_id);

-- app_tenant_menu: 租户+菜单 唯一
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_tenant_menu_tm
ON app_tenant_menu (tenant_id, menu_id);
