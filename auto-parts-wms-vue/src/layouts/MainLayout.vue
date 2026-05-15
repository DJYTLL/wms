<template>
  <div class="mac-layout" :class="{ 'mac-layout--embed': isEmbedded }">
    <aside v-if="!isEmbedded" class="sidebar">
      <div class="sidebar-header">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
          </svg>
        </div>
        <h1 class="brand-name">{{ $t('app.brandName') }}</h1>
      </div>

      <nav class="nav-menu">
        <div class="search-box-wrapper">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
          <input
            ref="menuSearchInput"
            type="text"
            v-model="searchQuery"
            :placeholder="$t('action.search')"
            class="search-input"
          >
        </div>
        <div class="nav-scroll-area">
          <ul class="menu-root">
            <li v-for="item in filteredMenuData" :key="item.id" class="menu-item-l1">

              <div class="menu-label l1" @click="toggleMenu(item)" :class="{ 'is-active': isMenuItemActive(item) }">
                <span class="icon-box" v-html="item.icon"></span>
                <span class="label-text">{{ menuLabel(item) }}</span>
                <svg v-if="hasChildren(item)" class="chevron" :class="{ 'rotated': item.isOpen }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
              </div>

              <transition name="slide-down">
                <ul v-if="hasChildren(item) && item.isOpen" class="submenu-l2">
                  <li v-for="subItem in item.children" :key="subItem.id">

                    <div class="menu-label l2" @click="handleMenuClick(subItem)" :class="{ 'is-active': isMenuItemActive(subItem) }">
                      <span class="dot" v-if="!hasChildren(subItem)"></span>
                      <span class="label-text">{{ menuLabel(subItem) }}</span>
                      <svg v-if="hasChildren(subItem)" class="chevron" :class="{ 'rotated': subItem.isOpen }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
                    </div>

                    <transition name="slide-down">
                      <ul v-if="hasChildren(subItem) && subItem.isOpen" class="submenu-l3">
                        <li v-for="leaf in subItem.children" :key="leaf.id">
                          <div class="menu-label l3" @click="handleMenuClick(leaf)" :class="{ 'is-active': isMenuItemActive(leaf) }">
                            <span class="label-text">{{ menuLabel(leaf) }}</span>
                          </div>
                        </li>
                      </ul>
                    </transition>

                  </li>
                </ul>
              </transition>
            </li>
          </ul>
        </div>
      </nav>

      <div class="sidebar-footer">v2.1.0 (Build 302)</div>
    </aside>

    <div class="main-wrapper">

      <header v-if="!isEmbedded" class="top-bar">
        <div class="breadcrumbs">
          <transition-group name="breadcrumb">
            <div v-for="(crumb, index) in breadcrumbs" :key="crumb.path || crumb.title" class="crumb-item">
              <span 
                :class="{ 'crumb-link': index < breadcrumbs.length - 1 }"
                @click="index < breadcrumbs.length - 1 && crumb.path && router.push(crumb.path)"
              >
                {{ crumb.title }}
              </span>
              <span v-if="index < breadcrumbs.length - 1" class="crumb-separator">/</span>
            </div>
          </transition-group>
        </div>

        <div class="user-actions-group">
          <div class="tenant-indicator" :title="`${t('field.tenant')}: ${currentTenantCode}`">
            <span class="tenant-indicator__label">{{ t('field.tenant') }}</span>
            <span class="tenant-indicator__value">{{ currentTenantCode }}</span>
          </div>
          <!-- Theme Switcher -->
          <div class="theme-dropdown-wrapper">
            <button @click="toggleThemeDropdown" class="action-btn" :title="$t('action.selectTheme')">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="13.5" cy="6.5" r=".5"/><circle cx="17.5" cy="10.5" r=".5"/><circle cx="8.5" cy="7.5" r=".5"/><circle cx="6.5" cy="12.5" r=".5"/><path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.926 0 1.648-.746 1.648-1.688 0-.437-.18-.835-.437-1.125-.29-.289-.438-.652-.438-1.125a1.64 1.64 0 0 1 1.668-1.668h1.996c3.051 0 5.555-2.503 5.555-5.554C21.965 6.012 17.461 2 12 2z"/></svg>
            </button>
            <div v-if="showThemeDropdown" class="theme-dropdown-menu">
              <div class="theme-grid">
                <button 
                  v-for="color in themeStore.themeColors" 
                  :key="color.value"
                  class="theme-color-btn"
                  :style="{ backgroundColor: color.value }"
                  :title="color.name"
                  @click="selectTheme(color.value)"
                >
                  <svg v-if="themeStore.primaryColor === color.value" class="check-icon" viewBox="0 0 24 24" width="14" height="14" stroke="white" stroke-width="3" fill="none"><polyline points="20 6 9 17 4 12"></polyline></svg>
                </button>
              </div>
            </div>
          </div>

          <button @click="switchLanguage" class="action-btn" :title="$t('action.switchLanguage')">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m5 8 6 6"/><path d="m4 14 6-6 2-3"/><path d="M2 5h12"/><path d="M7 2h1"/><path d="m22 22-5-10-5 10"/><path d="M14 18h6"/></svg>
          </button>
          <div class="avatar-circle">A</div>
          <button class="action-btn" @click="handleLogout" :title="$t('action.logout')">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
              <polyline points="16 17 21 12 16 7"></polyline>
              <line x1="21" y1="12" x2="9" y2="12"></line>
            </svg>
          </button>
        </div>
      </header>

      <div v-if="!isEmbedded" class="tags-bar-container">
        <div class="tags-scroll-wrapper">
          <div
            v-for="tag in visitedViews"
            :key="tag.path"
            class="tag-item"
            :class="{ active: route.path === tag.path }"
            @click="router.push(tag.path)"
            @mousedown.middle.prevent
            @mouseup.middle.prevent="closeView(tag)"
          >
            <span class="tag-dot" v-if="route.path === tag.path"></span>
            {{ labelFromKey(tag.key, tag.title) }}
            <span class="close-icon" @click.stop="closeView(tag)" v-if="visitedViews.length > 1">
              <svg viewBox="0 0 24 24" width="12" height="12" stroke="currentColor" stroke-width="3" fill="none"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
            </span>
          </div>
        </div>
      </div>

      <main class="content-area" :class="{ 'content-area--embed': isEmbedded }">
        <router-view v-slot="{ Component }">
          <keep-alive :max="30">
            <component :is="Component" :key="route.path + ':' + (viewKeyVersions[route.path] || 0)" />
          </keep-alive>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, watch, ref, onMounted, onActivated, onBeforeUnmount } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { setLocale } from '@/i18n';
import { useThemeStore } from '@/stores/theme';
import { useAuthStore } from '@/stores/auth';
import { useMenuStore } from '@/stores/menu';
import { useApiError } from '@/composables/useApiError';
import { ElMessageBox } from 'element-plus';
import { normalizeMenuKey } from '@/utils/i18n';
import type { RouteLocationNormalizedLoaded } from 'vue-router';

const i18n = useI18n();
const t = i18n.t;
const te = i18n.te;
const router = useRouter();
const route = useRoute();
const themeStore = useThemeStore();
const authStore = useAuthStore();
const menuStore = useMenuStore();
const { notifyError } = useApiError();
const searchQuery = ref('');
const menuSearchInput = ref<HTMLInputElement | null>(null);
const showThemeDropdown = ref(false);
const isEmbedded = computed(() => {
  const embed = route.query.embed;
  return embed === '1' || embed === 'true';
});
const currentTenantCode = computed(() => authStore.tenantCode || '-');
const handleCloseTagEvent = (event: Event) => {
  const customEvent = event as CustomEvent<{ path?: string; redirectPath?: string }>;
  const targetPath = customEvent.detail?.path || route.path;
  const targetView = visitedViews.find(v => v.path === targetPath);
  if (targetView) {
    closeView(targetView, customEvent.detail?.redirectPath);
  }
};

const toggleThemeDropdown = () => {
  showThemeDropdown.value = !showThemeDropdown.value;
};

const selectTheme = (color: string) => {
  themeStore.setPrimaryColor(color);
  showThemeDropdown.value = false;
};

// 点击外部关闭下拉菜单 (简单的实现，实际项目可以用 v-click-outside)
// 这里仅做简单演示，不添加复杂的全局事件监听以免内存泄漏风险


// --- 1. 模拟菜单数据 (支持三级) ---
interface MenuItem {
  id: number;
  key?: string;
  title?: string;
  path?: string;
  icon?: string;
  isOpen?: boolean;
  children?: MenuItem[];
}

const menuData = ref<MenuItem[]>([]);

const menuRouteFallbackMap: Record<string, string> = {
  'erp-stock-transfer': '/erp/stock-transfers',
};

const resolveMenuPath = (item: MenuItem): string | undefined => {
  const candidate = item.path?.trim();
  if (candidate) {
    const resolved = router.resolve(candidate);
    if (resolved.matched.length > 0) {
      return resolved.path;
    }
  }
  if (item.key && menuRouteFallbackMap[item.key]) {
    return menuRouteFallbackMap[item.key];
  }
  return candidate || undefined;
};

const decorateMenus = (items: MenuItem[]): MenuItem[] => {
  return items.map((item) => ({
    ...item,
    isOpen: false,
    children: item.children && item.children.length ? decorateMenus(item.children) : undefined
  }));
};

const flattenRedundantRootMenus = (items: MenuItem[]): MenuItem[] => {
  return items.flatMap((item) => {
    const children = item.children || [];
    const shouldFlattenWarehouseRoot = item.key === 'warehouse' && !item.path && children.length > 0;

    if (!shouldFlattenWarehouseRoot) {
      return item.key === 'outbound' ? [] : [item];
    }

    return children
      .filter(child => child.key !== 'outbound')
      .map((child) => ({
        ...child,
        icon: child.icon || item.icon,
      }));
  });
};

const refreshMenus = async (force = false) => {
  try {
    await menuStore.fetchMenus(force);
    menuData.value = flattenRedundantRootMenus(decorateMenus(menuStore.menus));
    findAndExpand(menuData.value);
  } catch (error) {
    notifyError(error);
  }
};

// 逻辑复用：展开当前菜单
const findAndExpand = (items: MenuItem[]) => {
  const currentPath = route.path;
  for (const item of items) {
    if (hasChildren(item)) {
      const children = item.children || [];
      const hasActiveChild = children.some(child => {
          if (resolveMenuPath(child) === currentPath) return true;
          if (hasChildren(child)) {
            return (child.children || []).some(grandChild => resolveMenuPath(grandChild) === currentPath);
          }
          return false;
      });

      if (hasActiveChild) {
        item.isOpen = true;
        findAndExpand(children);
        return true;
      }
    }
  }
  return false;
};

const handleMenuRefresh = () => {
  menuStore.clearMenus();
  refreshMenus(true);
};

// 自动展开当前菜单逻辑
onMounted(() => {
  refreshMenus();
  if (typeof window !== 'undefined') {
    window.addEventListener('auth:tokens-updated', handleMenuRefresh);
    window.addEventListener('menu:refresh', handleMenuRefresh);
    window.addEventListener('tags:close', handleCloseTagEvent as EventListener);
    window.addEventListener('keydown', handleSearchShortcut);
  }
});

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('auth:tokens-updated', handleMenuRefresh);
    window.removeEventListener('menu:refresh', handleMenuRefresh);
    window.removeEventListener('tags:close', handleCloseTagEvent as EventListener);
    window.removeEventListener('keydown', handleSearchShortcut);
  }
});

// 当组件被激活时（例如从其他标签页切回），也尝试刷新状态或重新展开菜单
onActivated(() => {
  // 可以在这里刷新 authStore 的权限，确保菜单显示最新
  // authStore.fetchPermissions(); // 如果 store 有这个方法
  findAndExpand(menuData.value);
});

watch(menuData, () => {
  visitedViews.forEach((view) => {
    const key = findKeyByPath(menuData.value, view.path);
    if (key) {
      view.key = key;
      view.title = labelFromKey(key);
    }
  });
}, { deep: true });

const filteredMenuData = computed(() => {
  const lowerCaseQuery = searchQuery.value ? searchQuery.value.toLowerCase() : '';

  const filterItems = (items: MenuItem[], isRoot = false): MenuItem[] => {
    return items.reduce((acc, item) => {
      // 搜索过滤
      const translatedTitle = menuLabel(item).toLowerCase();
      const matchesSearch = !lowerCaseQuery || translatedTitle.includes(lowerCaseQuery);

      if (hasChildren(item)) {
        const filteredChildren = filterItems(item.children || []);
        // 如果有子项匹配（且有权限），或者父项本身匹配搜索，则保留
        if (filteredChildren.length > 0 || matchesSearch) {
          // 如果是靠子项匹配保留的父项，但父项本身不匹配搜索，可能需要保持原有逻辑
          // 这里简单的逻辑是：只要子项有留下的，父项就留下。
          // 注意：我们这里没有强制展开，只是过滤显示。
          
          // 如果父项有权限，但所有子项都被权限过滤掉了，那父项也不应该显示（除非它自己是个链接）
          if (filteredChildren.length > 0) {
            acc.push({ ...item, children: filteredChildren });
          } else if (matchesSearch && item.path && !isRoot) {
            acc.push({ ...item, children: [] });
          }
        }
      } else {
        if (matchesSearch && !isRoot) {
          acc.push(item);
        }
      }

      return acc;
    }, [] as MenuItem[]);
  };

  return filterItems(menuData.value, true);
});

// --- 2. 菜单交互逻辑 ---
const findAndToggle = (items: MenuItem[], id: number): boolean => {
  for (const item of items) {
    if (item.id === id) {
      if (hasChildren(item)) {
        item.isOpen = !item.isOpen;
      }
      return true;
    }
    if (hasChildren(item)) {
      if (findAndToggle(item.children || [], id)) return true;
    }
  }
  return false;
};

const toggleMenu = (item: MenuItem) => {
  if (hasChildren(item)) {
    findAndToggle(menuData.value, item.id);
  } else {
    const targetPath = resolveMenuPath(item);
    if (targetPath) {
      router.push(targetPath);
    }
  }
};

const handleMenuClick = (item: MenuItem) => {
  if (hasChildren(item)) {
    findAndToggle(menuData.value, item.id);
  } else {
    const targetPath = resolveMenuPath(item);
    if (targetPath) {
      router.push(targetPath);
    }
  }
};

const isMenuItemActive = (item: MenuItem): boolean => {
  const resolvedPath = resolveMenuPath(item);
  if (resolvedPath && route.path === resolvedPath) {
    return true;
  }
  if (!hasChildren(item)) {
    return false;
  }
  return (item.children || []).some(child => isMenuItemActive(child));
};

// --- 3. 标签页 (Multi-Tabs) 逻辑 ---
const visitedViews = reactive<{ key?: string; title: string; path: string }[]>([]);
const viewKeyVersions = reactive<Record<string, number>>({});

// 监听路由变化，添加标签
watch(() => route.path, () => {
  // 查找当前路径对应的菜单标题（简单的扁平化查找，实际项目可用递归）
  let title = t('page.newPage');
  let id = 'page.newPage'; // 默认

  // 简单遍历查找标题 (仅演示用)
  const findTitle = (items: MenuItem[]): string | null => {
    for (const item of items) {
      if (resolveMenuPath(item) === route.path) return item.key || item.title || null;
      if (hasChildren(item)) {
        const found: string | null = findTitle(item.children || []);
        if (found) return found;
      }
    }
    return null;
  };

  const foundId = findTitle(menuData.value);
  if (foundId) {
    id = foundId;
    title = labelFromKey(foundId);
  } else {
    const metaTitleKey = route.meta?.titleKey as string | undefined;
    const metaTitle = route.meta?.title as string | undefined;
    if (metaTitleKey) {
      id = metaTitleKey;
      title = t(metaTitleKey);
    } else if (metaTitle) {
      id = metaTitle;
      title = metaTitle;
    }
  }

  // 如果不存在则添加
  const exist = visitedViews.find(v => v.path === route.path);
  if (!exist) {
    visitedViews.push({ key: id, title, path: route.path });
  }
  if (viewKeyVersions[route.path] === undefined) {
    viewKeyVersions[route.path] = 0;
  }
}, { immediate: true });

const closeView = (view: { id?: string; title: string; path: string }, redirectPath?: string) => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('tags:closing', { detail: { path: view.path } }));
  }
  viewKeyVersions[view.path] = (viewKeyVersions[view.path] || 0) + 1;
  const index = visitedViews.findIndex(v => v.path === view.path);
  if (index > -1) {
    visitedViews.splice(index, 1);
    // 如果关闭的是当前页，跳转到最后一个 tag
    if (view.path === route.path) {
      const queryReturnTo = typeof route.query.returnTo === 'string' ? route.query.returnTo.trim() : '';
      const resolvedRedirectPath = redirectPath || queryReturnTo;
      const target = resolvedRedirectPath && resolvedRedirectPath !== view.path
        ? visitedViews.find(v => v.path === resolvedRedirectPath)
        : null;
      if (target) {
        router.push(target.path);
        return;
      }
      if (resolvedRedirectPath && resolvedRedirectPath !== view.path) {
        router.push(resolvedRedirectPath);
        return;
      }
      const last = visitedViews[visitedViews.length - 1];
      if (last) router.push(last.path);
      else router.push('/');
    }
  }
};

// --- 4. 面包屑逻辑 ---
interface BreadcrumbItem {
  title: string;
  path?: string;
}
const breadcrumbs = computed<BreadcrumbItem[]>(() => {
  // 依赖 locale 确保语言切换时更新
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const _ = i18n.locale.value;

  const matched: { key: string, path?: string, title?: string }[] = [];

  const findPath = (items: MenuItem[], targetPath: string, parentChain: { key: string, path?: string, title?: string }[] = []): boolean => {
    for (const item of items) {
      const currentPath = resolveMenuPath(item);
      const currentChain = [...parentChain, { key: item.key || '', title: item.title, path: currentPath }];
      if (currentPath === targetPath) {
        matched.push(...currentChain);
        return true;
      }
      if (hasChildren(item)) {
        if (findPath(item.children || [], targetPath, currentChain)) return true;
      }
    }
    return false;
  };

  findPath(menuData.value, route.path);
  
  if (matched.length) {
    return matched.map(item => ({
      title: labelFromKey(item.key, item.title),
      path: item.path,
    }));
  }
  
  return [{ title: t('nav.dashboard'), path: '/' }];
});


// ---5. 登出逻辑---
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      t('message.confirmLogout'),
      t('action.logout'),
      {
        confirmButtonText: t('action.confirm'),
        cancelButtonText: t('action.cancel'),
        type: 'warning',
      }
    );
    await authStore.logout();
  } catch {
    // cancelled
  }
};

const switchLanguage = async () => {
  const newLocale = i18n.locale.value === 'en' ? 'zh' : 'en';
  await setLocale(newLocale);
};

const labelFromKey = (key?: string, fallback?: string) => {
  if (!key) return fallback || '';
  const normalizedKey = normalizeMenuKey(key);
  if (te(normalizedKey)) {
    return t(normalizedKey);
  }
  if (fallback) {
    return fallback;
  }
  return key;
};

const menuLabel = (item: MenuItem) => {
  return labelFromKey(item.key, item.title);
};

const findKeyByPath = (items: MenuItem[], targetPath: string): string | null => {
  for (const item of items) {
    if (resolveMenuPath(item) === targetPath) return item.key || item.title || null;
    if (hasChildren(item)) {
      const found = findKeyByPath(item.children || [], targetPath);
      if (found) return found;
    }
  }
  return null;
};

const hasChildren = (item: MenuItem) => {
  return Array.isArray(item.children) && item.children.length > 0;
};

const handleSearchShortcut = (event: KeyboardEvent) => {
  if (event.ctrlKey && event.key.toLowerCase() === 'k') {
    event.preventDefault();
    menuSearchInput.value?.focus();
  }
};
</script>

<style scoped>

/* 变量定义 */

.mac-layout {

  --sidebar-bg: #f5f5f7;

  --main-bg: #ffffff;

  --border-color: #d1d1d6;

  /* --active-blue 和 --active-bg-blue 现在是全局的，由 theme store 管理 */

  --text-main: #1d1d1f;

  --text-sub: #86868b;



  display: flex;

  height: 100%;

  width: 100%;

  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", sans-serif;

  color: var(--text-main);

  background-color: var(--main-bg);

}



/* --- 侧边栏 --- */

.sidebar {

  width: 240px;

  background-color: var(--sidebar-bg);

  border-right: 1px solid #e5e5e5;

  display: flex;

  flex-direction: column;

  flex-shrink: 0;

  user-select: none;

}



.sidebar-header {

  height: 50px; /* Match top-bar height */

  display: flex;

  align-items: center;

  padding: 0 16px;

  border-bottom: 1px solid rgba(0,0,0,0.03);

}



.logo-icon {

  width: 28px;

  height: 28px;

  background: var(--active-blue);

  border-radius: 6px;

  color: white;

  display: flex;

  justify-content: center;

  align-items: center;

  margin-right: 10px;

}

.logo-icon svg { width: 18px; }



.brand-name {

  font-size: 15px;

  font-weight: 600;

  letter-spacing: -0.01em;

}



.nav-menu {

  flex-grow: 1;

  display: flex;

  flex-direction: column;

  padding: 10px 0;

  min-height: 0;

  overflow: hidden;

}



/* 搜索框 */

.search-box-wrapper {

  position: relative;

  margin: 0 12px 10px;

  flex-shrink: 0; /* 防止搜索框被压缩 */

}

.search-input {

  width: 100%;

  padding: 8px 12px 8px 34px;

  border-radius: 8px;

  border: 1px solid transparent;

  background-color: rgba(0,0,0,0.05);

  font-size: 13px;

  outline: none;

  transition: all 0.2s;

  box-sizing: border-box; /* 新增此行 */

}

.search-input:focus {

  background-color: #fff;

  border-color: var(--active-blue);

  box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.2);

}

.search-icon {

  position: absolute;

  left: 10px;

  top: 50%;

  transform: translateY(-50%);

  width: 16px;

  height: 16px;

  color: var(--text-sub);

}



/* 菜单通用样式 */

.nav-scroll-area {

  flex-grow: 1;

  overflow-y: auto;

  padding: 0 12px;
  min-height: 0;

}



.menu-root {

  list-style: none;

  padding: 0;

  margin: 0;

  flex: 1;

  overflow-y: auto;

}



.menu-label {

  display: flex;

  align-items: center;

  padding: 8px 10px;

  border-radius: 6px;

  cursor: pointer;

  transition: all 0.2s;

  font-size: 14px;

  color: #424245;

  margin-bottom: 2px;

}



.menu-label:hover {

  background-color: rgba(0,0,0,0.05);

}



/* 一级菜单特定样式 */

.menu-label.l1 {

  font-weight: 500;

}



/* 激活状态 */

.menu-label.is-active {

  background-color: var(--active-bg-blue);

  color: var(--active-blue);

}



.icon-box {

  display: flex;

  margin-right: 10px;

  opacity: 0.8;

}



.label-text {

  flex-grow: 1;

}



.chevron {

  width: 16px;

  height: 16px;

  color: var(--text-sub);

  transition: transform 0.2s ease;

}



.chevron.rotated {

  transform: rotate(90deg);

}



/* 二级菜单缩进与样式 */

.submenu-l2 {

  list-style: none;

  padding: 0;

  margin: 0;

  padding-left: 14px; /* 缩进 */

  overflow: hidden;

}



.menu-label.l2 {

  font-size: 13.5px;

  padding: 7px 10px;

  color: #555;

}



/* 三级菜单缩进 */

.submenu-l3 {

  list-style: none;

  padding: 0;

  margin: 0;

  padding-left: 18px; /* 再缩进 */

  overflow: hidden;

}



.menu-label.l3 {

  font-size: 13px;

  color: #666;

  padding: 6px 10px;

}



.dot {

  width: 4px;

  height: 4px;

  background: var(--text-sub);

  border-radius: 50%;

  margin-right: 10px;

  opacity: 0.5;

}

.is-active .dot {

  background: var(--active-blue);

  opacity: 1;

}



/* 菜单展开动画 */

.slide-down-enter-active,

.slide-down-leave-active {

  transition: max-height 0.25s ease, opacity 0.2s ease;

  max-height: 1000px; /* 避免较长菜单分段停顿 */

  opacity: 1;

}

.slide-down-enter-from,

.slide-down-leave-to {

  max-height: 0;

  opacity: 0;

}



.sidebar-footer {

  padding: 12px;

  font-size: 11px;

  color: #999;

  text-align: center;

}



/* --- 主体区域 --- */

.main-wrapper {

  flex-grow: 1;
  min-height: 0;

  display: flex;

  flex-direction: column;

  overflow: hidden;

  background-color: #fff;

}



/* 顶部 Header */

.top-bar {

  flex: 0 0 50px;
  height: 50px;
  min-height: 50px;
  max-height: 50px;

  padding: 0 20px;

  display: flex;

  align-items: center;

  justify-content: space-between;

  border-bottom: 1px solid #f0f0f0;

  background-color: #fff;

  z-index: 10;

}



/* 面包屑 */

.breadcrumbs {

  display: flex;
  align-items: center;
  min-height: 24px;
  overflow: hidden;

  font-size: 13px;

  color: var(--text-sub);

}

.crumb-item {

  display: flex;

  align-items: center;
  min-height: 24px;
  line-height: 24px;

}

.crumb-item .crumb-link {

  cursor: pointer;

  font-weight: 400;

  color: var(--text-sub);

}

.crumb-item .crumb-link:hover {

  color: var(--active-blue);

  text-decoration: underline;

}

.crumb-item span {

  font-weight: 500;

  color: var(--text-main);
  line-height: 24px;

}

.crumb-separator {

  margin: 0 8px;

  color: #ccc;

  font-size: 12px;

}



/* 标签页 (Tags Bar) */

.tags-bar-container {

  flex: 0 0 40px;
  height: 40px;
  min-height: 40px;
  max-height: 40px;

  background-color: #f5f5f7;

  border-bottom: 1px solid #e5e5e5;

  display: flex;

  align-items: flex-end; /* 标签贴底 */

  padding: 0 10px;
  overflow: hidden;

  box-shadow: inset 0 -1px 0 #e5e5e5;

}



.tags-scroll-wrapper {

  display: flex;
  align-items: flex-end;

  gap: 6px;

  overflow-x: auto;
  overflow-y: hidden;

  width: 100%;
  height: 100%;

  scrollbar-width: none; /* 隐藏滚动条 */

}



.tag-item {

  flex: 0 0 auto;
  height: 32px;
  min-height: 32px;
  max-height: 32px;

  padding: 0 12px;

  background-color: transparent;

  border-radius: 6px 6px 0 0;

  display: flex;

  align-items: center;
  line-height: 32px;

  font-size: 13px;

  color: #555;

  cursor: pointer;

  transition: all 0.15s;

  min-width: fit-content;

  border: 1px solid transparent;

  border-bottom: none;

}



.tag-item:hover {

  background-color: rgba(0,0,0,0.03);

}



.tag-item.active {

  background-color: #fff;

  color: var(--active-blue);

  border-color: #e5e5e5;

  box-shadow: 0 -2px 5px rgba(0,0,0,0.02);

  z-index: 1;

}



.tag-dot {

  width: 6px;

  height: 6px;

  background-color: var(--active-blue);

  border-radius: 50%;

  margin-right: 6px;

}



.close-icon {

  margin-left: 8px;

  width: 16px;

  height: 16px;

  border-radius: 50%;

  display: flex;

  align-items: center;

  justify-content: center;

  opacity: 0; /* 默认隐藏关闭按钮 */

  transition: all 0.2s;

}



.tag-item:hover .close-icon {

  opacity: 1; /* hover时显示 */

  background-color: rgba(0,0,0,0.1);

}



.close-icon:hover {

  background-color: #ff3b30;

  color: white;

}



/* 内容区 */

.content-area {

  flex: 1 1 0;
  min-height: 0;

  padding: 24px;

  overflow-y: auto;

  position: relative;

}

.mac-layout--embed {
  background-color: #fff;
}

.content-area--embed {
  padding: 16px;
}



.user-actions-group {

  display: flex;

  align-items: center;

  gap: 16px;

}

.tenant-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid rgba(0, 113, 227, 0.18);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(0, 113, 227, 0.08), rgba(0, 113, 227, 0.03));
  color: var(--text-main);
  white-space: nowrap;
}

.tenant-indicator__label {
  font-size: 12px;
  color: var(--text-sub);
}

.tenant-indicator__value {
  font-size: 13px;
  font-weight: 600;
  color: var(--active-blue);
}



.action-btn {

  background: transparent;

  border: none;

  cursor: pointer;

  color: var(--text-sub);

  display: flex;

  align-items: center;

  justify-content: center;

  padding: 4px;

  border-radius: 6px;

  transition: all 0.2s;

}

.action-btn:hover {

  background-color: rgba(0,0,0,0.05);

  color: var(--text-main);

}



.avatar-circle {

  width: 28px;

  height: 28px;

  background: #eee;

  border-radius: 50%;

  text-align: center;

  line-height: 28px;

  font-size: 12px;

  color: #666;

}



/* 页面切换动画 */

.fade-scale-enter-active,

.fade-scale-leave-active {

  transition: opacity 0.3s ease, transform 0.3s ease;

}



.fade-scale-enter-from {

  opacity: 0;

  transform: scale(0.98);

}

.fade-scale-leave-to {

  opacity: 0;

  transform: scale(1.02);

}

/* Theme Switcher Styles */
.theme-dropdown-wrapper {
  position: relative;
}

.theme-dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: white;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  z-index: 100;
  min-width: 160px;
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.theme-color-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s;
}

.theme-color-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
}

.check-icon {
  filter: drop-shadow(0 1px 2px rgba(0,0,0,0.3));
}

@media (max-width: 900px) {
  .tenant-indicator {
    padding: 0 10px;
  }

  .tenant-indicator__label {
    display: none;
  }
}
</style>
