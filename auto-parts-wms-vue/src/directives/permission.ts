import type { Directive } from 'vue';
import { useAuthStore } from '@/stores/auth';

/**
 * 自定义指令: v-permission
 * 
 * 用法: <button v-permission="'product:add'">添加商品</button>
 * 
 * 逻辑:
 * 1. 从 Auth Store 中检查当前用户的权限。
 * 2. 如果用户没有指令值中指定的权限，则将该元素从 DOM 中移除。
 */
export const permission: Directive = {
  mounted(el, binding) {
    const { value } = binding;
    const authStore = useAuthStore();

    if (value && typeof value === 'string') {
      const hasPermission = authStore.hasPermission(value);

      if (!hasPermission) {
        // 如果权限被拒绝，从 DOM 中移除元素
        el.parentNode && el.parentNode.removeChild(el);
      }
    } else {
      throw new Error(`需要指定权限值! 例如: v-permission="'product:add'"`);
    }
  }
};
