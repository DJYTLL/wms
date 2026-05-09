<template>
  <div class="apple-login-container">
    <div class="login-card fade-in">
      <div class="icon-wrapper">
        <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z"/>
          <path d="M12 6v6l4 2"/>
        </svg>
      </div>

      <h1>登录系统</h1>
      <p class="subtitle">使用您的管理员账号访问</p>

      <form @submit.prevent="handleLogin">
        <div class="input-group" :class="{ 'has-value': !!tenantCode }">
          <input
            type="text"
            id="tenantCode"
            v-model="tenantCode"
            required
            placeholder=" "
            autocomplete="organization"
            :disabled="isLoading"
          >
          <label for="tenantCode">租户编码</label>
        </div>

        <div class="input-group" :class="{ 'has-value': !!username }">
          <input
            type="text"
            id="username"
            v-model="username"
            required
            placeholder=" "
            autocomplete="username"
            :disabled="isLoading"
          >
          <label for="username">用户名</label>
        </div>

        <div class="input-group" :class="{ 'has-value': !!password }">
          <input
            type="password"
            id="password"
            v-model="password"
            required
            placeholder=" "
            autocomplete="current-password"
            :disabled="isLoading"
          >
          <label for="password">密码</label>
        </div>

        <button type="submit" class="login-button" :class="{ 'loading': isLoading }" :disabled="isLoading">
          <span v-if="!isLoading">登录</span>
          <div v-else class="spinner"></div>
        </button>
      </form>

      <div class="footer-links">
        <a href="#">忘记密码?</a>
        <span class="divider">•</span>
        <a href="#">创建 Apple ID</a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useApiError } from '@/composables/useApiError';

const username = ref('');
const password = ref('');
const isLoading = ref(false);
const router = useRouter();
const authStore = useAuthStore();
const { notifyError } = useApiError();
const tenantCode = ref('default');

const handleLogin = async () => {
  if (isLoading.value) return;

  isLoading.value = true;

  try {
    await authStore.login(tenantCode.value.trim(), username.value.trim(), password.value);
    console.log('Login successful');
    router.push('/');
  } catch (error: any) {
    console.error('Login failed', error);
    notifyError(error);
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
  /* 修复方案：
     1. 将变量定义移动到 .apple-login-container 内部
     2. 强制指定文字颜色和背景色
  */

  .apple-login-container {
    /* 定义局部变量 */
    --apple-blue: #0071e3;
    --apple-blue-hover: #0077ed;
    --apple-gray: #86868b;
    --text-main: #1d1d1f;
    --border-radius: 12px;

    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    /* 强制背景色，防止透明 */
    background: radial-gradient(circle at 50% 0%, #fbfbfb 0%, #f2f2f4 100%);
    font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", Helvetica, Arial, sans-serif;
    color: var(--text-main);
    position: relative;
    z-index: 1;
  }

  .login-card {
    width: 380px;
    padding: 48px;
    background-color: rgba(255, 255, 255, 0.85); /* 稍微增加不透明度 */
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-radius: 20px;
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.04);
    text-align: center;
    border: 1px solid rgba(255, 255, 255, 0.6);
    z-index: 10;
  }

  /* 图标与标题 */
  .icon-wrapper {
    margin-bottom: 20px;
    color: #1d1d1f;
  }

  h1 {
    margin: 0 0 10px;
    font-size: 28px;
    font-weight: 600;
    color: #1d1d1f; /* 强制黑色标题 */
  }

  .subtitle {
    margin: 0 0 40px;
    color: #86868b;
    font-size: 15px;
  }

  /* 输入框组 */
  .input-group {
    position: relative;
    margin-bottom: 20px;
    text-align: left;
  }

  .input-group input {
    width: 100%;
    padding: 18px 16px 6px;
    height: 56px;
    font-size: 17px;
    border: 1px solid #d2d2d7;
    border-radius: var(--border-radius);
    background-color: #ffffff; /* 强制白色背景 */
    box-sizing: border-box;
    outline: none;
    color: #1d1d1f; /* 强制输入文字黑色 */
    transition: border-color 0.2s, box-shadow 0.2s;
  }

  .input-group input:focus {
    border-color: #0071e3;
    box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.15);
  }

  .input-group label {
    position: absolute;
    top: 18px;
    left: 16px;
    font-size: 17px;
    color: #86868b;
    pointer-events: none;
    transition: 0.2s ease all;
    z-index: 5;
  }

  /* Label 上浮逻辑 */
  .input-group input:focus ~ label,
  .input-group input:-webkit-autofill ~ label,
  .input-group input:not(:placeholder-shown) ~ label {
    top: 8px;
    font-size: 12px;
    color: #0071e3;
  }

  .input-group input:-webkit-autofill ~ label,
  .input-group input:not(:placeholder-shown) ~ label {
    color: #86868b;
  }

  .input-group input:-webkit-autofill {
    -webkit-text-fill-color: #1d1d1f;
    transition: background-color 9999s ease-out 0s;
  }

  .input-group.has-value label {
    top: 8px;
    font-size: 12px;
    color: #86868b;
  }

  /* 按钮样式 - 重点修复部分 */
  .login-button {
    position: relative;
    width: 100%;
    height: 50px;
    padding: 14px;
    margin-top: 10px;

    /* 强制指定颜色，不依赖外部 */
    background-color: #0071e3 !important;
    color: #ffffff !important;

    border: none;
    border-radius: var(--border-radius);
    font-size: 17px;
    font-weight: 500;
    cursor: pointer;
    display: flex;
    justify-content: center;
    align-items: center;
    transition: background-color 0.2s, transform 0.1s;

    /* 确保按钮在最上层 */
    z-index: 20;
  }

  .login-button:hover:not(:disabled) {
    background-color: #0077ed !important;
  }

  .login-button:active:not(:disabled) {
    transform: scale(0.98);
  }

  .login-button:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }

  /* 底部链接 */
  .footer-links {
    margin-top: 30px;
    font-size: 13px;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .footer-links a {
    color: #0071e3; /* 强制蓝色链接 */
    text-decoration: none;
    font-weight: 400;
  }

  .divider {
    margin: 0 8px;
    color: #d2d2d7;
  }

  /* 旋转加载动画 */
  .spinner {
    width: 20px;
    height: 20px;
    border: 2px solid rgba(255, 255, 255, 0.3);
    border-radius: 50%;
    border-top-color: #fff;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  .fade-in {
    animation: fadeIn 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  }
  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
  }
  </style>
