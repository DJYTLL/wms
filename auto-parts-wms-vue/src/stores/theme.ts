import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useThemeStore = defineStore('theme', () => {
  const primaryColor = ref(localStorage.getItem('theme-primary-color') || '#0071e3')

  // 预定义的主题色列表
  const themeColors = [
    { name: 'Default Blue', value: '#0071e3' },
    { name: 'Emerald Green', value: '#10b981' },
    { name: 'Rose Red', value: '#f43f5e' },
    { name: 'Amber Orange', value: '#f59e0b' },
    { name: 'Purple', value: '#8b5cf6' },
    { name: 'Dark Gray', value: '#374151' },
  ]

  // 辅助函数：Hex 转 RGB
  const hexToRgb = (hex: string) => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
      r: parseInt(result[1]!, 16),
      g: parseInt(result[2]!, 16),
      b: parseInt(result[3]!, 16)
    } : null;
  }

  const applyTheme = (color: string) => {
    document.documentElement.style.setProperty('--active-blue', color)
    
    const rgb = hexToRgb(color)
    if (rgb) {
      // 设置一个带透明度的背景色变量
      document.documentElement.style.setProperty('--active-bg-blue', `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, 0.1)`)
    }
  }

  const setPrimaryColor = (color: string) => {
    primaryColor.value = color
    localStorage.setItem('theme-primary-color', color)
    applyTheme(color)
  }

  // 初始化应用主题
  applyTheme(primaryColor.value)

  return { primaryColor, setPrimaryColor, themeColors }
})
