import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import { useRouter } from 'vue-router';
import axios from 'axios';
import request, { clearTokens, getToken, setTokens } from '@/utils/request';

/**
 * 认证 Store (Pinia)
 * 
 * 管理用户认证状态、Token 存储和权限。
 * 此实现模拟了基于 JWT 的认证流程。
 */
let listenersRegistered = false;
const AUTH_CONTEXT_STORAGE_KEY = 'auth-context';

type StoredAuthContext = {
  user?: any;
  permissions?: string[];
  tenantId?: number | null;
  tenantCode?: string | null;
  userTenantId?: number | null;
  userTenantCode?: string | null;
};

export const useAuthStore = defineStore('auth', () => {
  const router = useRouter();
  
  // JWT Token 优先从持久化存储恢复，保证打印页和 iframe 可复用登录态。
  const token = ref<string | null>(getToken());
  
  // 已认证用户对象的响应式状态。
  const user = ref<any>(null);
  
  // 用户权限列表的响应式状态 (例如：['erp-warehouse:view', 'erp-product:add'])。
  const permissions = ref<string[]>([]);
  const tenantId = ref<number | null>(null);
  const tenantCode = ref<string | null>(null);
  const userTenantId = ref<number | null>(null);
  const userTenantCode = ref<string | null>(null);
  const initialized = ref(false);
  let restorePromise: Promise<boolean> | null = null;

  const readStoredAuthContext = (): StoredAuthContext | null => {
    if (typeof window === 'undefined') {
      return null;
    }
    const raw = window.localStorage.getItem(AUTH_CONTEXT_STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as StoredAuthContext;
    } catch {
      window.localStorage.removeItem(AUTH_CONTEXT_STORAGE_KEY);
      return null;
    }
  };

  const persistAuthContext = (context: StoredAuthContext | null) => {
    if (typeof window === 'undefined') {
      return;
    }
    if (!context) {
      window.localStorage.removeItem(AUTH_CONTEXT_STORAGE_KEY);
      return;
    }
    window.localStorage.setItem(AUTH_CONTEXT_STORAGE_KEY, JSON.stringify(context));
  };

  const applyAuthContext = (authPayload?: any) => {
    if (!authPayload) {
      permissions.value = [];
      return false;
    }
    user.value = authPayload.user || null;
    permissions.value = Array.isArray(authPayload.permissions) ? authPayload.permissions : [];
    tenantId.value = typeof authPayload.tenantId === 'number' ? authPayload.tenantId : null;
    tenantCode.value = typeof authPayload.tenantCode === 'string' ? authPayload.tenantCode : null;
    userTenantId.value = typeof authPayload.userTenantId === 'number' ? authPayload.userTenantId : null;
    userTenantCode.value = typeof authPayload.userTenantCode === 'string' ? authPayload.userTenantCode : null;
    persistAuthContext({
      user: user.value,
      permissions: permissions.value,
      tenantId: tenantId.value,
      tenantCode: tenantCode.value,
      userTenantId: userTenantId.value,
      userTenantCode: userTenantCode.value,
    });
    return true;
  };

  const applyToken = (newToken: string | null, authPayload?: any) => {
    token.value = newToken;
    if (!newToken) {
      user.value = null;
      permissions.value = [];
      tenantId.value = null;
      tenantCode.value = null;
      userTenantId.value = null;
      userTenantCode.value = null;
      persistAuthContext(null);
      return;
    }

    if (applyAuthContext(authPayload)) {
      return;
    }

    try {
      // 解码 JWT 载荷 (token 的第二部分)。
      // 注意：在真实应用中，你应该在后端验证签名，或者前端仅用于读取非敏感信息。
      const parts = newToken.split('.');
      const payloadPart = parts[1];
      if (parts.length === 3 && typeof payloadPart === 'string') {
        // 处理 base64url 格式
        const base64 = payloadPart.replace(/-/g, '+').replace(/_/g, '/');
        const payload = JSON.parse(atob(base64));
        user.value = payload.user;
        permissions.value = [];
        tenantId.value = typeof payload.tid === 'number' ? payload.tid : null;
        tenantCode.value = typeof payload.tcode === 'string' ? payload.tcode : null;
        userTenantId.value = typeof payload.utid === 'number' ? payload.utid : null;
        userTenantCode.value = typeof payload.utcode === 'string' ? payload.utcode : null;
        const storedContext = readStoredAuthContext();
        if (storedContext && storedContext.user?.username === payload.user?.username) {
          user.value = storedContext.user || user.value;
          permissions.value = Array.isArray(storedContext.permissions) ? storedContext.permissions : [];
          tenantId.value = typeof storedContext.tenantId === 'number' ? storedContext.tenantId : tenantId.value;
          tenantCode.value = typeof storedContext.tenantCode === 'string' ? storedContext.tenantCode : tenantCode.value;
          userTenantId.value = typeof storedContext.userTenantId === 'number' ? storedContext.userTenantId : userTenantId.value;
          userTenantCode.value = typeof storedContext.userTenantCode === 'string' ? storedContext.userTenantCode : userTenantCode.value;
        }
      } else {
        throw new Error('Token format invalid');
      }
    } catch (e) {
      console.error('无效的 token', e);
      clearTokens();
    }
  };

  if (token.value) {
    applyToken(token.value);
  }

  if (!listenersRegistered && typeof window !== 'undefined') {
    listenersRegistered = true;
    window.addEventListener('auth:tokens-updated', (event: Event) => {
      const detail = (event as CustomEvent).detail as { token?: string; authPayload?: any } | undefined;
      applyToken(detail?.token || null, detail?.authPayload);
    });
    window.addEventListener('auth:tokens-cleared', () => {
      applyToken(null);
    });
  }

  // 计算属性：检查用户当前是否已认证。
  const isAuthenticated = computed(() => !!token.value);

  /**
   * 用户登录。
   * 
   * @param username 登录表单提供的用户名。
   * @param password 登录表单提供的密码。
   */
  const login = async (tenantCodeInput: string, username: string, password: string) => {
    try {
      // request 响应拦截器已处理 code !== 200 的情况
      const res: any = await request.post('/login', {
        tenantCode: tenantCodeInput,
        username,
        password
      });
      const { token: newToken, authPayload } = res.data.data;
      setTokens(newToken, authPayload);
      applyToken(newToken, authPayload);
      return true;
    } catch (error: any) {
      console.error('Login error:', error);
      throw error;
    }
  };

  const restoreSession = async () => {
    if (token.value) {
      initialized.value = true;
      return true;
    }
    if (restorePromise) {
      return restorePromise;
    }
    restorePromise = (async () => {
      try {
        const res: any = await axios.post('/api/refresh', {}, { withCredentials: true });
        const refreshData = res.data;
        if (!refreshData || refreshData.code !== 200 || !refreshData.data?.token) {
          clearTokens();
          return false;
        }
        setTokens(refreshData.data.token, refreshData.data.authPayload);
        applyToken(refreshData.data.token, refreshData.data.authPayload);
        return true;
      } catch (_error) {
        clearTokens();
        return false;
      } finally {
        initialized.value = true;
        restorePromise = null;
      }
    })();
    return restorePromise;
  };

  /**
   * 用户登出。
   * 清除状态并重定向到登录页面。
   */
  const logout = async () => {
    try {
      await request.post('/logout', {});
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      clearTokens();
      router.push('/login');
    }
  };

  /**
   * 检查已认证用户是否拥有特定权限。
   * 
   * @param permission 要检查的权限字符串 (例如：'erp-product:add')。
   * @returns {boolean} 如果用户拥有该权限，则返回 true。
   */
  const hasPermission = (permission: string) => {
    if (hasRole('super_admin')) {
      return true;
    }
    return permissions.value.includes(permission);
  };

  /**
   * 检查已认证用户是否拥有特定角色。
   * 
   * @param role 角色编码 (例如：'super_admin')。
   */
  const hasRole = (role: string) => {
    const currentUser = user.value as { role?: string; roles?: Array<string | { code?: string }> } | null;
    if (!currentUser) return false;
    if (Array.isArray(currentUser.roles)) {
      return currentUser.roles.some((item) => {
        if (typeof item === 'string') return item === role;
        return item?.code === role;
      });
    }
    if (typeof currentUser.role === 'string') {
      return currentUser.role === role;
    }
    return false;
  };

  return {
    token,
    user,
    permissions,
    tenantId,
    tenantCode,
    userTenantId,
    userTenantCode,
    initialized,
    isAuthenticated,
    login,
    restoreSession,
    logout,
    hasPermission,
    hasRole
  };
});
