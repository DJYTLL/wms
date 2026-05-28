package com.example.wms.service.impl;

import com.example.wms.aop.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.wms.dto.MenuCreateRequest;
import com.example.wms.dto.MenuManageResponse;
import com.example.wms.dto.MenuResponse;
import com.example.wms.dto.MenuUpdateRequest;
import com.example.wms.dto.TenantMenuResponse;
import com.example.wms.dto.TenantMenuUpdateRequest;
import com.example.wms.entity.Menu;
import com.example.wms.entity.Tenant;
import com.example.wms.entity.TenantMenu;
import com.example.wms.exception.NotFoundException;
import com.example.wms.mapper.MenuMapper;
import com.example.wms.mapper.TenantMapper;
import com.example.wms.mapper.TenantMenuMapper;
import com.example.wms.service.MenuService;
import com.example.wms.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 菜单服务实现
@Service
public class MenuServiceImpl implements MenuService {
    private final MenuMapper menuMapper;
    private final TenantMenuMapper tenantMenuMapper;
    private final TenantMapper tenantMapper;

    public MenuServiceImpl(MenuMapper menuMapper,
                           TenantMenuMapper tenantMenuMapper,
                           TenantMapper tenantMapper) {
        this.menuMapper = menuMapper;
        this.tenantMenuMapper = tenantMenuMapper;
        this.tenantMapper = tenantMapper;
    }

    @Override
    public List<MenuResponse> listVisibleMenus() {
        Long tenantId = TenantContext.requireTenantId();
        List<Menu> menus = menuMapper.listEnabled();
        Set<Long> enabledMenuIds = resolveEnabledMenuIds(tenantId, menus);
        Set<String> authorities = resolveAuthorities();

        Map<Long, Menu> menuMap = new HashMap<>();
        for (Menu menu : menus) {
            menuMap.put(menu.getId(), menu);
        }

        List<Menu> allowedMenus = new ArrayList<>();
        for (Menu menu : menus) {
            if (isMenuAllowed(menu, menuMap, enabledMenuIds, authorities)) {
                allowedMenus.add(menu);
            }
        }

        return buildMenuTree(allowedMenus);
    }

    @Override
    public List<TenantMenuResponse> listTenantMenus(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getDeletedAt() != null) {
            throw new NotFoundException("租户不存在");
        }
        List<Menu> menus = menuMapper.listAllOrdered();
        List<TenantMenu> tenantMenus = tenantMenuMapper.findByTenantId(tenantId);
        Map<Long, Boolean> enabledMap = new HashMap<>();
        for (TenantMenu tenantMenu : tenantMenus) {
            enabledMap.put(tenantMenu.getMenuId(), tenantMenu.isEnabled());
        }
        return buildTenantMenuTree(menus, enabledMap);
    }

    @Override
    @Transactional
    @AuditLog(action = "TENANT_MENU_UPDATE", entityType = "tenant", entityId = "{arg0}", detail = "menuIds={arg1.menuIds}")
    public void updateTenantMenus(Long tenantId, TenantMenuUpdateRequest request) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getDeletedAt() != null) {
            throw new NotFoundException("租户不存在");
        }
        List<Menu> menus = menuMapper.listAllOrdered();
        Set<Long> enabledIds = request.menuIds() == null ? Set.of() : new HashSet<>(request.menuIds());
        enabledIds = expandParentMenuIds(enabledIds, menus);

        tenantMenuMapper.deleteByTenantId(tenantId);
        for (Menu menu : menus) {
            TenantMenu mapping = new TenantMenu();
            mapping.setTenantId(tenantId);
            mapping.setMenuId(menu.getId());
            mapping.setEnabled(enabledIds.contains(menu.getId()));
            mapping.setCreatedAt(Instant.now());
            mapping.setUpdatedAt(Instant.now());
            tenantMenuMapper.insert(mapping);
        }
    }

    @Override
    public List<MenuManageResponse> listAllMenus() {
        List<Menu> menus = menuMapper.listAllOrdered();
        return buildManageMenuTree(menus);
    }

    @Override
    @Transactional
    @AuditLog(action = "MENU_CREATE", entityType = "menu", entityId = "{result.id}", detail = "code={result.code}")
    public MenuManageResponse createMenu(MenuCreateRequest request) {
        if (menuMapper.findByCode(request.code()) != null) {
            throw new IllegalArgumentException("菜单编码已存在");
        }
        validateParent(request.parentId(), null);
        Menu menu = new Menu();
        menu.setCode(request.code());
        menu.setParentId(request.parentId());
        menu.setTitle(request.title());
        menu.setI18nKey(request.i18nKey());
        menu.setPath(request.path());
        menu.setIcon(request.icon());
        menu.setPermissionCode(request.permissionCode());
        menu.setSort(request.sort() == null ? 0 : request.sort());
        menu.setEnabled(request.enabled() == null || request.enabled());
        menu.setCreatedAt(Instant.now());
        menu.setUpdatedAt(Instant.now());
        menuMapper.insert(menu);
        insertTenantMenuMappings(menu.getId());
        return toManageResponse(menu, List.of());
    }

    @Override
    @Transactional
    @AuditLog(action = "MENU_UPDATE", entityType = "menu", entityId = "{arg0}", detail = "code={arg1.code}")
    public MenuManageResponse updateMenu(Long id, MenuUpdateRequest request) {
        Menu existing = menuMapper.selectById(id);
        if (existing == null) {
            throw new NotFoundException("菜单不存在");
        }
        Menu duplicate = menuMapper.findByCode(request.code());
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw new IllegalArgumentException("菜单编码已存在");
        }
        validateParent(request.parentId(), id);
        existing.setCode(request.code());
        existing.setParentId(request.parentId());
        existing.setTitle(request.title());
        existing.setI18nKey(request.i18nKey());
        existing.setPath(request.path());
        existing.setIcon(request.icon());
        existing.setPermissionCode(request.permissionCode());
        existing.setSort(request.sort() == null ? 0 : request.sort());
        if (request.enabled() != null) {
            existing.setEnabled(request.enabled());
        }
        existing.setUpdatedAt(Instant.now());
        menuMapper.updateById(existing);
        return toManageResponse(existing, List.of());
    }

    @Override
    @Transactional
    @AuditLog(action = "MENU_DELETE", entityType = "menu", entityId = "{arg0}")
    public void deleteMenu(Long id) {
        Menu existing = menuMapper.selectById(id);
        if (existing == null) {
            throw new NotFoundException("菜单不存在");
        }
        Long children = menuMapper.selectCount(new QueryWrapper<Menu>().eq("parent_id", id));
        if (children != null && children > 0) {
            throw new IllegalArgumentException("请先删除子菜单");
        }
        menuMapper.deleteById(id);
        tenantMenuMapper.deleteByMenuId(id);
    }

    private Set<Long> expandParentMenuIds(Set<Long> enabledIds, List<Menu> menus) {
        Map<Long, Menu> menuMap = new HashMap<>();
        for (Menu menu : menus) {
            menuMap.put(menu.getId(), menu);
        }
        Set<Long> expanded = new HashSet<>(enabledIds);
        for (Long menuId : enabledIds) {
            Menu menu = menuMap.get(menuId);
            while (menu != null && menu.getParentId() != null) {
                Long parentId = menu.getParentId();
                if (!expanded.add(parentId)) {
                    break;
                }
                menu = menuMap.get(parentId);
            }
        }
        return expanded;
    }

    private Set<Long> resolveEnabledMenuIds(Long tenantId, List<Menu> menus) {
        List<TenantMenu> tenantMenus = tenantMenuMapper.findByTenantId(tenantId);
        if (tenantMenus == null || tenantMenus.isEmpty()) {
            return toMenuIdSet(menus);
        }
        Set<Long> disabled = new HashSet<>();
        for (TenantMenu tenantMenu : tenantMenus) {
            if (!tenantMenu.isEnabled()) {
                disabled.add(tenantMenu.getMenuId());
            }
        }
        Set<Long> enabled = new HashSet<>();
        for (Menu menu : menus) {
            if (!disabled.contains(menu.getId())) {
                enabled.add(menu.getId());
            }
        }
        return enabled;
    }

    private Set<Long> toMenuIdSet(List<Menu> menus) {
        Set<Long> ids = new HashSet<>();
        for (Menu menu : menus) {
            ids.add(menu.getId());
        }
        return ids;
    }

    private boolean isMenuAllowed(Menu menu,
                                  Map<Long, Menu> menuMap,
                                  Set<Long> enabledMenuIds,
                                  Set<String> authorities) {
        if (!menu.isEnabled() || !enabledMenuIds.contains(menu.getId())) {
            return false;
        }
        if (!hasPermission(menu.getPermissionCode(), authorities)) {
            return false;
        }

        Long parentId = menu.getParentId();
        while (parentId != null) {
            Menu parent = menuMap.get(parentId);
            if (parent == null) {
                return false;
            }
            if (!parent.isEnabled() || !enabledMenuIds.contains(parent.getId())) {
                return false;
            }
            if (!hasPermission(parent.getPermissionCode(), authorities)) {
                return false;
            }
            parentId = parent.getParentId();
        }
        return true;
    }

    private boolean hasPermission(String permissionCode, Set<String> authorities) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return true;
        }
        String authority = "PERM_" + permissionCode;
        return authorities.contains(authority);
    }

    private Set<String> resolveAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> authorities = new HashSet<>();
        if (authentication == null) {
            return authorities;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            authorities.add(authority.getAuthority());
        }
        return authorities;
    }

    private List<MenuResponse> buildMenuTree(List<Menu> menus) {
        Map<Long, List<Menu>> childrenMap = buildChildrenMap(menus);
        List<MenuResponse> roots = new ArrayList<>();
        for (Menu menu : menus) {
            if (menu.getParentId() == null) {
                roots.add(toMenuResponse(menu, childrenMap));
            }
        }
        return roots;
    }

    private List<MenuManageResponse> buildManageMenuTree(List<Menu> menus) {
        Map<Long, List<Menu>> childrenMap = buildChildrenMap(menus);
        List<MenuManageResponse> roots = new ArrayList<>();
        Set<Long> menuIds = new HashSet<>();
        for (Menu menu : menus) {
            menuIds.add(menu.getId());
        }
        for (Menu menu : menus) {
            Long parentId = menu.getParentId();
            if (parentId == null || !menuIds.contains(parentId)) {
                roots.add(toManageResponse(menu, childrenMap));
            }
        }
        return roots;
    }

    private List<TenantMenuResponse> buildTenantMenuTree(List<Menu> menus, Map<Long, Boolean> enabledMap) {
        Map<Long, List<Menu>> childrenMap = buildChildrenMap(menus);
        List<TenantMenuResponse> roots = new ArrayList<>();
        for (Menu menu : menus) {
            if (menu.getParentId() == null) {
                roots.add(toTenantMenuResponse(menu, childrenMap, enabledMap));
            }
        }
        return roots;
    }

    private Map<Long, List<Menu>> buildChildrenMap(List<Menu> menus) {
        Map<Long, List<Menu>> childrenMap = new HashMap<>();
        for (Menu menu : menus) {
            if (menu.getParentId() == null) {
                continue;
            }
            childrenMap.computeIfAbsent(menu.getParentId(), key -> new ArrayList<>()).add(menu);
        }
        return childrenMap;
    }

    private MenuResponse toMenuResponse(Menu menu, Map<Long, List<Menu>> childrenMap) {
        List<MenuResponse> children = new ArrayList<>();
        List<Menu> childMenus = childrenMap.get(menu.getId());
        if (childMenus != null) {
            for (Menu child : childMenus) {
                children.add(toMenuResponse(child, childrenMap));
            }
        }
        return new MenuResponse(menu.getId(),
            menu.getI18nKey(),
            menu.getTitle(),
            menu.getPath(),
            menu.getIcon(),
            children);
    }

    private MenuManageResponse toManageResponse(Menu menu, Map<Long, List<Menu>> childrenMap) {
        List<MenuManageResponse> children = new ArrayList<>();
        List<Menu> childMenus = childrenMap.get(menu.getId());
        if (childMenus != null) {
            for (Menu child : childMenus) {
                children.add(toManageResponse(child, childrenMap));
            }
        }
        return toManageResponse(menu, children);
    }

    private MenuManageResponse toManageResponse(Menu menu, List<MenuManageResponse> children) {
        return new MenuManageResponse(menu.getId(),
            menu.getCode(),
            menu.getParentId(),
            menu.getTitle(),
            menu.getI18nKey(),
            menu.getPath(),
            menu.getIcon(),
            menu.getPermissionCode(),
            menu.getSort(),
            menu.isEnabled(),
            children);
    }

    private TenantMenuResponse toTenantMenuResponse(Menu menu,
                                                    Map<Long, List<Menu>> childrenMap,
                                                    Map<Long, Boolean> enabledMap) {
        List<TenantMenuResponse> children = new ArrayList<>();
        List<Menu> childMenus = childrenMap.get(menu.getId());
        if (childMenus != null) {
            for (Menu child : childMenus) {
                children.add(toTenantMenuResponse(child, childrenMap, enabledMap));
            }
        }
        boolean enabled = enabledMap.getOrDefault(menu.getId(), true);
        return new TenantMenuResponse(menu.getId(),
            menu.getI18nKey(),
            menu.getTitle(),
            menu.getPath(),
            menu.getIcon(),
            enabled,
            children);
    }

    private void validateParent(Long parentId, Long menuId) {
        if (parentId == null) {
            return;
        }
        if (menuId != null && parentId.equals(menuId)) {
            throw new IllegalArgumentException("父菜单不能是自身");
        }
        Menu parent = menuMapper.selectById(parentId);
        if (parent == null) {
            throw new IllegalArgumentException("父菜单不存在");
        }
        if (menuId == null) {
            return;
        }
        List<Menu> menus = menuMapper.listAllOrdered();
        Map<Long, Long> parentMap = new HashMap<>();
        for (Menu menu : menus) {
            parentMap.put(menu.getId(), menu.getParentId());
        }
        parentMap.put(menuId, parentId);
        Long cursor = parentId;
        while (cursor != null) {
            if (cursor.equals(menuId)) {
                throw new IllegalArgumentException("不能选择子菜单作为父级");
            }
            cursor = parentMap.get(cursor);
        }
    }

    private void insertTenantMenuMappings(Long menuId) {
        List<Tenant> tenants = tenantMapper.selectList(new QueryWrapper<Tenant>().isNull("deleted_at"));
        Instant now = Instant.now();
        for (Tenant tenant : tenants) {
            TenantMenu mapping = new TenantMenu();
            mapping.setTenantId(tenant.getId());
            mapping.setMenuId(menuId);
            mapping.setEnabled(true);
            mapping.setCreatedAt(now);
            mapping.setUpdatedAt(now);
            tenantMenuMapper.insert(mapping);
        }
    }
}
