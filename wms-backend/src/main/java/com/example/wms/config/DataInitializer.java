package com.example.wms.config;

import com.example.wms.entity.Menu;
import com.example.wms.entity.Permission;
import com.example.wms.entity.Role;
import com.example.wms.entity.Tenant;
import com.example.wms.entity.TenantMenu;
import com.example.wms.entity.UserAccount;
import com.example.wms.mapper.MenuMapper;
import com.example.wms.mapper.PermissionMapper;
import com.example.wms.mapper.RoleMapper;
import com.example.wms.mapper.RolePermissionMapper;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.TenantMapper;
import com.example.wms.mapper.TenantMenuMapper;
import com.example.wms.mapper.UserAccountMapper;
import com.example.wms.mapper.UserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;

// 初始化基础权限与管理员账号
@Configuration
public class DataInitializer {
    // 默认租户编码
    @Value("${app.tenant.code:default}")
    private String tenantCode;

    // 默认租户名称
    @Value("${app.tenant.name:默认租户}")
    private String tenantName;

    // 默认管理员用户名
    @Value("${app.admin.username:admin}")
    private String adminUsername;

    // 默认管理员密码（为空则不覆盖已有密码）
    @Value("${app.admin.password:}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initData(PermissionMapper permissionMapper,
                                      RoleMapper roleMapper,
                                      RolePermissionMapper rolePermissionMapper,
                                      TenantMapper tenantMapper,
                                      MenuMapper menuMapper,
                                      TenantMenuMapper tenantMenuMapper,
                                      SystemConfigMapper systemConfigMapper,
                                      UserAccountMapper userAccountMapper,
                                      UserRoleMapper userRoleMapper,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            Tenant tenant = tenantMapper.findByCode(tenantCode);
            if (tenant == null) {
                tenant = new Tenant();
                tenant.setCode(tenantCode);
                tenant.setName(tenantName);
                tenant.setEnabled(true);
                tenant.setCreatedAt(Instant.now());
                tenant.setUpdatedAt(Instant.now());
                tenantMapper.insert(tenant);
            }
            Long tenantId = tenant.getId();

            // 创建基础权限
            List<Permission> allPermissions = PermissionSeedProvider.permissionSeeds().stream()
                .map(seed -> ensurePermission(permissionMapper, seed.code(), seed.name(), seed.description()))
                .toList();
            List<Long> tenantPermissionIds = allPermissions.stream()
                .filter(this::isTenantPermission)
                .map(Permission::getId)
                .toList();
            List<Permission> columnPermissions = allPermissions.stream()
                .filter(permission -> permission.getCode() != null && permission.getCode().startsWith("column:"))
                .toList();
            Permission roleAssignPermission = permissionMapper.findByCode("role:assign:view");
            Permission columnRoleManagePermission = permissionMapper.findByCode("column:role:manage");
            Permission productCostViewPermission = permissionMapper.findByCode("erp-product:cost:view");
            Permission productCostEditPermission = permissionMapper.findByCode("erp-product:cost:edit");

            // 创建菜单定义（全局）
            List<Menu> menus = MenuSeedProvider.menuSeeds().stream()
                .map(seed -> ensureMenu(menuMapper, seed))
                .toList();

            // 初始化租户菜单映射（默认租户 + 已存在租户）
            ensureTenantMenus(tenantMenuMapper, tenantId, menus);
            List<Tenant> tenants = tenantMapper.selectList(new QueryWrapper<Tenant>().isNull("deleted_at"));
            for (Tenant item : tenants) {
                ensureTenantMenus(tenantMenuMapper, item.getId(), menus);
            }

            // 初始化系统配置
            ensureSystemConfig(systemConfigMapper, "default.page.size", "20", "int", "默认分页大小", true);
            ensureSystemConfig(systemConfigMapper, "audit.retention.days", "180", "int", "审计日志保留天数", false);
            ensureSystemConfig(systemConfigMapper, "password.min.length", "8", "int", "密码最小长度", false);
            ensureSystemConfig(systemConfigMapper, "login.max.retry", "5", "int", "登录失败最大次数", false);
            ensureSystemConfig(systemConfigMapper, "erp.order.no.purchase.prefix", "PO", "string", "ERP采购单号前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.order.no.purchase-return.prefix", "PR", "string", "ERP采购退货单号前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.order.no.sale.prefix", "SO", "string", "ERP销售单号前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.order.no.payment.prefix", "PY", "string", "ERP付款单号前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.order.no.ap-return.prefix", "AP", "string", "ERP应付退货单号前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.order.no.date-format", "yyyyMMdd", "string", "ERP单号日期格式", false);
            ensureSystemConfig(systemConfigMapper, "erp.order.no.seq-length", "4", "int", "ERP单号序列长度", false);
            ensureSystemConfig(systemConfigMapper, "erp.order.no.stock-count.prefix", "SC", "string", "ERP库存调整单号前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.order.no.stock-init.prefix", "SI", "string", "ERP初始库存单号前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.customer.code.prefix", "CU", "string", "ERP客户编码前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.customer.code.date-format", "yyyyMMdd", "string", "ERP客户编码日期格式", false);
            ensureSystemConfig(systemConfigMapper, "erp.customer.code.seq-length", "4", "int", "ERP客户编码序列长度", false);
            ensureSystemConfig(systemConfigMapper, "erp.customer-category.code.prefix", "CC", "string", "ERP客户类别编码前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.customer-category.code.date-format", "yyyyMMdd", "string", "ERP客户类别编码日期格式", false);
            ensureSystemConfig(systemConfigMapper, "erp.customer-category.code.seq-length", "4", "int", "ERP客户类别编码序列长度", false);
            ensureSystemConfig(systemConfigMapper, "erp.category.code.prefix", "CA", "string", "ERP商品分类编码前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.category.code.date-format", "yyyyMMdd", "string", "ERP商品分类编码日期格式", false);
            ensureSystemConfig(systemConfigMapper, "erp.category.code.seq-length", "4", "int", "ERP商品分类编码序列长度", false);
            ensureSystemConfig(systemConfigMapper, "erp.supplier.code.prefix", "SU", "string", "ERP供应商编码前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.supplier.code.date-format", "yyyyMMdd", "string", "ERP供应商编码日期格式", false);
            ensureSystemConfig(systemConfigMapper, "erp.supplier.code.seq-length", "4", "int", "ERP供应商编码序列长度", false);
            ensureSystemConfig(systemConfigMapper, "erp.unit.code.prefix", "UN", "string", "ERP单位编码前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.unit.code.date-format", "yyyyMMdd", "string", "ERP单位编码日期格式", false);
            ensureSystemConfig(systemConfigMapper, "erp.unit.code.seq-length", "4", "int", "ERP单位编码序列长度", false);
            ensureSystemConfig(systemConfigMapper, "erp.settlement-method.code.prefix", "SM", "string", "ERP结算方式编码前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.settlement-method.code.date-format", "yyyyMMdd", "string", "ERP结算方式编码日期格式", false);
            ensureSystemConfig(systemConfigMapper, "erp.settlement-method.code.seq-length", "4", "int", "ERP结算方式编码序列长度", false);
            ensureSystemConfig(systemConfigMapper, "erp.payment-method.code.prefix", "PM", "string", "ERP付款方式编码前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.payment-method.code.date-format", "yyyyMMdd", "string", "ERP付款方式编码日期格式", false);
            ensureSystemConfig(systemConfigMapper, "erp.payment-method.code.seq-length", "4", "int", "ERP付款方式编码序列长度", false);
            ensureSystemConfig(systemConfigMapper, "erp.delivery-method.code.prefix", "DM", "string", "ERP送货方式编码前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.delivery-method.code.date-format", "yyyyMMdd", "string", "ERP送货方式编码日期格式", false);
            ensureSystemConfig(systemConfigMapper, "erp.delivery-method.code.seq-length", "4", "int", "ERP送货方式编码序列长度", false);
            ensureSystemConfig(systemConfigMapper, "erp.print-template.code.prefix", "PT", "string", "ERP打印模板编码前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.print-template.code.date-format", "yyyyMMdd", "string", "ERP打印模板编码日期格式", false);
            ensureSystemConfig(systemConfigMapper, "erp.print-template.code.seq-length", "4", "int", "ERP打印模板编码序列长度", false);
            ensureSystemConfig(systemConfigMapper, "erp.warehouse.code.prefix", "WH", "string", "ERP仓库编码前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.warehouse.code.date-format", "yyyyMMdd", "string", "ERP仓库编码日期格式", false);
            ensureSystemConfig(systemConfigMapper, "erp.warehouse.code.seq-length", "4", "int", "ERP仓库编码序列长度", false);
            ensureSystemConfig(systemConfigMapper, "erp.product.code.prefix", "PR", "string", "ERP商品编码前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.product.code.date-format", "yyyyMMdd", "string", "ERP商品编码日期格式", false);
            ensureSystemConfig(systemConfigMapper, "erp.product.code.seq-length", "4", "int", "ERP商品编码序列长度", false);
            ensureSystemConfig(systemConfigMapper, "erp.vehicle-brand.code.prefix", "VB", "string", "ERP车型品牌编码前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.vehicle-brand.code.date-format", "yyyyMMdd", "string", "ERP车型品牌编码日期格式", false);
            ensureSystemConfig(systemConfigMapper, "erp.vehicle-brand.code.seq-length", "4", "int", "ERP车型品牌编码序列长度", false);
            ensureSystemConfig(systemConfigMapper, "erp.vehicle-series.code.prefix", "VS", "string", "ERP车型车系编码前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.vehicle-series.code.date-format", "yyyyMMdd", "string", "ERP车型车系编码日期格式", false);
            ensureSystemConfig(systemConfigMapper, "erp.vehicle-series.code.seq-length", "4", "int", "ERP车型车系编码序列长度", false);
            ensureSystemConfig(systemConfigMapper, "erp.vehicle-model.code.prefix", "VM", "string", "ERP车型编码前缀", false);
            ensureSystemConfig(systemConfigMapper, "erp.vehicle-model.code.date-format", "yyyyMMdd", "string", "ERP车型编码日期格式", false);
            ensureSystemConfig(systemConfigMapper, "erp.vehicle-model.code.seq-length", "4", "int", "ERP车型编码序列长度", false);

            // 创建租户管理员角色并绑定租户内权限
            Role adminRole = roleMapper.findByCode(tenantId, "admin");
            if (adminRole == null) {
                adminRole = new Role();
                adminRole.setTenantId(tenantId);
                adminRole.setCode("admin");
                adminRole.setName("租户管理员");
                adminRole.setDescription("当前租户内的超级管理员角色");
                adminRole.setEnabled(true);
                adminRole.setCreatedAt(Instant.now());
                adminRole.setUpdatedAt(Instant.now());
                roleMapper.insert(adminRole);
            }
            for (Permission permission : allPermissions) {
                // 绑定角色权限关系
                if (!isTenantPermission(permission)) {
                    rolePermissionMapper.insertIgnore(tenantId, adminRole.getId(), permission.getId());
                }
            }

            Role superAdminRole = null;
            if (tenantCode.equalsIgnoreCase(tenant.getCode())) {
                // 仅系统租户保留 super_admin 角色，并绑定全部权限
                superAdminRole = roleMapper.findByCode(tenantId, "super_admin");
                if (superAdminRole == null) {
                    superAdminRole = new Role();
                    superAdminRole.setTenantId(tenantId);
                    superAdminRole.setCode("super_admin");
                    superAdminRole.setName("系统超级管理员");
                    superAdminRole.setDescription("全系统跨租户超级管理员角色");
                    superAdminRole.setEnabled(true);
                    superAdminRole.setCreatedAt(Instant.now());
                    superAdminRole.setUpdatedAt(Instant.now());
                    roleMapper.insert(superAdminRole);
                }
                for (Permission permission : allPermissions) {
                    rolePermissionMapper.insertIgnore(tenantId, superAdminRole.getId(), permission.getId());
                }
            }

            // 清理所有非 super_admin 角色的平台权限，避免被误分配
            if (!tenantPermissionIds.isEmpty()) {
                List<Role> nonSuperRoles = roleMapper.selectList(
                    new QueryWrapper<Role>().ne("code", "super_admin")
                );
                for (Role role : nonSuperRoles) {
                    for (Long permissionId : tenantPermissionIds) {
                        rolePermissionMapper.deleteByRoleIdAndPermissionId(
                            role.getTenantId(),
                            role.getId(),
                            permissionId
                        );
                    }
                }
            }

            // 清理非系统租户遗留的 super_admin 角色与绑定
            for (Tenant item : tenants) {
                if (tenantCode.equalsIgnoreCase(item.getCode())) {
                    continue;
                }
                Role legacySuperAdminRole = roleMapper.findByCode(item.getId(), "super_admin");
                if (legacySuperAdminRole == null) {
                    continue;
                }
                userRoleMapper.deleteByRoleId(item.getId(), legacySuperAdminRole.getId());
                rolePermissionMapper.deleteByRoleId(item.getId(), legacySuperAdminRole.getId());
                roleMapper.deleteById(legacySuperAdminRole.getId());
            }

            // 为已有租户的 admin 角色补齐列权限（补齐缺失项，避免新列权限无法显示）
            if (!columnPermissions.isEmpty()) {
                for (Tenant item : tenants) {
                    Role tenantAdmin = roleMapper.findByCode(item.getId(), "admin");
                    if (tenantAdmin == null) {
                        continue;
                    }
                    java.util.Set<Long> existingColumnIds = rolePermissionMapper
                        .findPermissionsByRoleId(item.getId(), tenantAdmin.getId())
                        .stream()
                        .filter(permission -> permission.getCode() != null && permission.getCode().startsWith("column:"))
                        .map(Permission::getId)
                        .collect(java.util.stream.Collectors.toSet());
                    for (Permission permission : columnPermissions) {
                        if (!existingColumnIds.contains(permission.getId())) {
                            rolePermissionMapper.insertIgnore(item.getId(), tenantAdmin.getId(), permission.getId());
                        }
                    }
                }
            }

            // 为已有租户的 admin 角色补齐角色下拉权限
            if (roleAssignPermission != null) {
                for (Tenant item : tenants) {
                    Role tenantAdmin = roleMapper.findByCode(item.getId(), "admin");
                    if (tenantAdmin == null) {
                        continue;
                    }
                    rolePermissionMapper.insertIgnore(item.getId(), tenantAdmin.getId(), roleAssignPermission.getId());
                }
            }

            // 为已有租户的 admin 角色补齐列权限管理权限
            if (columnRoleManagePermission != null) {
                for (Tenant item : tenants) {
                    Role tenantAdmin = roleMapper.findByCode(item.getId(), "admin");
                    if (tenantAdmin == null) {
                        continue;
                    }
                    rolePermissionMapper.insertIgnore(item.getId(), tenantAdmin.getId(), columnRoleManagePermission.getId());
                }
            }

            // 为已有租户的 admin 角色补齐商品成本价权限
            if (productCostViewPermission != null || productCostEditPermission != null) {
                for (Tenant item : tenants) {
                    Role tenantAdmin = roleMapper.findByCode(item.getId(), "admin");
                    if (tenantAdmin == null) {
                        continue;
                    }
                    if (productCostViewPermission != null) {
                        rolePermissionMapper.insertIgnore(item.getId(), tenantAdmin.getId(), productCostViewPermission.getId());
                    }
                    if (productCostEditPermission != null) {
                        rolePermissionMapper.insertIgnore(item.getId(), tenantAdmin.getId(), productCostEditPermission.getId());
                    }
                }
            }

            // 创建默认管理员用户
            UserAccount existingAdmin = userAccountMapper.findActiveByUsername(tenantId, adminUsername);
            if (existingAdmin == null) {
                UserAccount admin = new UserAccount();
                admin.setTenantId(tenantId);
                admin.setUsername(adminUsername);
                admin.setPasswordHash(passwordEncoder.encode(resolveAdminPassword()));
                admin.setDisplayName(tenantCode.equalsIgnoreCase(tenant.getCode()) ? "系统管理员" : "租户管理员");
                admin.setEmail("admin@example.com");
                admin.setEnabled(true);
                admin.setAccountNonExpired(true);
                admin.setAccountNonLocked(true);
                admin.setCredentialsNonExpired(true);
                admin.setAuthVersion(0);
                admin.setCreatedAt(Instant.now());
                admin.setUpdatedAt(Instant.now());
                userAccountMapper.insert(admin);
                // 绑定用户角色关系
                userRoleMapper.insertIgnore(tenantId, admin.getId(), adminRole.getId());
                if (superAdminRole != null) {
                    userRoleMapper.insertIgnore(tenantId, admin.getId(), superAdminRole.getId());
                }
            } else if (shouldUpdateAdminPassword(existingAdmin, passwordEncoder)) {
                // 根据配置更新管理员密码
                userAccountMapper.updatePasswordHash(tenantId,
                    existingAdmin.getId(),
                    passwordEncoder.encode(adminPassword));
            }
        };
    }

    // 若权限不存在则创建
    private Permission ensurePermission(PermissionMapper mapper,
                                        String code,
                                        String name,
                                        String description) {
        Permission existing = mapper.findByCode(code);
        if (existing != null) {
            return existing;
        }
        Permission permission = new Permission();
        permission.setCode(code);
        permission.setName(name);
        permission.setDescription(description);
        permission.setEnabled(true);
        permission.setCreatedAt(Instant.now());
        permission.setUpdatedAt(Instant.now());
        mapper.insert(permission);
        return permission;
    }

    // 解析管理员密码（为空时回退默认密码）
    private String resolveAdminPassword() {
        return adminPassword == null || adminPassword.isBlank() ? "password" : adminPassword;
    }

    private boolean isTenantPermission(Permission permission) {
        String code = permission.getCode();
        return code != null && (code.startsWith("tenant:") || code.startsWith("system-config:"));
    }

    private void ensureSystemConfig(SystemConfigMapper mapper,
                                    String key,
                                    String value,
                                    String valueType,
                                    String description,
                                    boolean isPublic) {
        com.example.wms.entity.SystemConfig config = mapper.findByKey(key);
        if (config == null) {
            config = new com.example.wms.entity.SystemConfig();
            config.setConfigKey(key);
        }
        boolean changed = false;
        if (config.getConfigValue() == null) {
            config.setConfigValue(value);
            changed = true;
        }
        if (config.getValueType() == null) {
            config.setValueType(valueType);
            changed = true;
        }
        if (config.getDescription() == null) {
            config.setDescription(description);
            changed = true;
        }
        if (config.isPublic() != isPublic) {
            config.setPublic(isPublic);
            changed = true;
        }
        if (!changed && config.getId() != null) {
            return;
        }
        if (config.getId() == null) {
            config.setCreatedAt(Instant.now());
        }
        config.setUpdatedAt(Instant.now());
        if (config.getId() == null) {
            mapper.insert(config);
        } else {
            mapper.update(config, new QueryWrapper<com.example.wms.entity.SystemConfig>()
                .eq("config_key", key));
        }
    }

    private Menu ensureMenu(MenuMapper mapper, MenuSeedProvider.MenuSeed seed) {
        Menu existing = mapper.findByCode(seed.code());
        if (existing != null) {
            boolean updated = false;
            if (shouldSyncMenu(seed)) {
                if (!equalsText(existing.getTitle(), seed.title())) {
                    existing.setTitle(seed.title());
                    updated = true;
                }
                if (!equalsText(existing.getI18nKey(), seed.i18nKey())) {
                    existing.setI18nKey(seed.i18nKey());
                    updated = true;
                }
                if (!equalsText(existing.getPath(), seed.path())) {
                    existing.setPath(seed.path());
                    updated = true;
                }
                if (!equalsText(existing.getIcon(), seed.icon())) {
                    existing.setIcon(seed.icon());
                    updated = true;
                }
                if (!equalsText(existing.getPermissionCode(), seed.permissionCode())) {
                    existing.setPermissionCode(seed.permissionCode());
                    updated = true;
                }
                if (existing.getSort() == null || existing.getSort() != seed.sort()) {
                    existing.setSort(seed.sort());
                    updated = true;
                }
                if (seed.parentCode() != null) {
                    Menu parent = mapper.findByCode(seed.parentCode());
                    if (parent != null && !parent.getId().equals(existing.getParentId())) {
                        existing.setParentId(parent.getId());
                        updated = true;
                    }
                } else if (existing.getParentId() != null) {
                    existing.setParentId(null);
                    updated = true;
                }
            } else if ((existing.getPermissionCode() == null || existing.getPermissionCode().isBlank())
                && seed.permissionCode() != null
                && !seed.permissionCode().isBlank()) {
                existing.setPermissionCode(seed.permissionCode());
                updated = true;
            }
            if (updated) {
                existing.setUpdatedAt(Instant.now());
                mapper.updateById(existing);
            }
            return existing;
        }
        Menu menu = new Menu();
        menu.setCode(seed.code());
        menu.setTitle(seed.title());
        menu.setI18nKey(seed.i18nKey());
        menu.setPath(seed.path());
        menu.setIcon(seed.icon());
        menu.setPermissionCode(seed.permissionCode());
        menu.setSort(seed.sort());
        menu.setEnabled(true);
        menu.setCreatedAt(Instant.now());
        menu.setUpdatedAt(Instant.now());
        if (seed.parentCode() != null) {
            Menu parent = mapper.findByCode(seed.parentCode());
            if (parent != null) {
                menu.setParentId(parent.getId());
            }
        }
        mapper.insert(menu);
        return menu;
    }

    private boolean shouldSyncMenu(MenuSeedProvider.MenuSeed seed) {
        String code = seed.code();
        return code != null && (code.equals("erp") || code.startsWith("erp-"));
    }

    private boolean equalsText(String left, String right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }

    private void ensureTenantMenus(TenantMenuMapper mapper, Long tenantId, List<Menu> menus) {
        List<TenantMenu> existing = mapper.findByTenantId(tenantId);
        java.util.Set<Long> existingMenuIds = existing == null
            ? java.util.Set.of()
            : existing.stream().map(TenantMenu::getMenuId).collect(java.util.stream.Collectors.toSet());
        Instant now = Instant.now();
        for (Menu menu : menus) {
            if (existingMenuIds.contains(menu.getId())) {
                continue;
            }
            TenantMenu tenantMenu = new TenantMenu();
            tenantMenu.setTenantId(tenantId);
            tenantMenu.setMenuId(menu.getId());
            tenantMenu.setEnabled(true);
            tenantMenu.setCreatedAt(now);
            tenantMenu.setUpdatedAt(now);
            mapper.insert(tenantMenu);
        }
    }

    // 判断是否需要更新管理员密码
    private boolean shouldUpdateAdminPassword(UserAccount admin, PasswordEncoder encoder) {
        if (adminPassword == null || adminPassword.isBlank()) {
            return false;
        }
        return !encoder.matches(adminPassword, admin.getPasswordHash());
    }
}
