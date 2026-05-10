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

export const useAuthStore = defineStore('auth', () => {
  const router = useRouter();
  
  // JWT Token 优先从持久化存储恢复，保证打印页和 iframe 可复用登录态。
  const token = ref<string | null>(getToken());
  
  // 已认证用户对象的响应式状态。
  const user = ref<any>(null);
  
  // 用户权限列表的响应式状态 (例如：['warehouse:view', 'product:add'])。
  const permissions = ref<string[]>([]);
  const tenantId = ref<number | null>(null);
  const tenantCode = ref<string | null>(null);
  const initialized = ref(false);
  let restorePromise: Promise<boolean> | null = null;

  const applyToken = (newToken: string | null) => {
    token.value = newToken;
    if (!newToken) {
      user.value = null;
      permissions.value = [];
      tenantId.value = null;
      tenantCode.value = null;
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
        permissions.value = payload.permissions || [];
        tenantId.value = typeof payload.tid === 'number' ? payload.tid : null;
        tenantCode.value = typeof payload.tcode === 'string' ? payload.tcode : null;
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
      const detail = (event as CustomEvent).detail as { token?: string } | undefined;
      applyToken(detail?.token || null);
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
      const { token: newToken } = res.data.data;
      setTokens(newToken);
      applyToken(newToken);
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
        setTokens(refreshData.data.token);
        applyToken(refreshData.data.token);
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
   * @param permission 要检查的权限字符串 (例如：'product:add')。
   * @returns {boolean} 如果用户拥有该权限，则返回 true。
   */
  const hasPermission = (permission: string) => {
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
    initialized,
    isAuthenticated,
    login,
    restoreSession,
    logout,
    hasPermission,
    hasRole
  };
});
