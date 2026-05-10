import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import './styles/table.css'

import App from './App.vue'
import router from './router'
import i18n, { setupI18n } from './i18n'
import { permission } from './directives/permission'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(i18n)
app.directive('permission', permission) // 注册全局自定义指令 'permission'

const bootstrap = async () => {
  await setupI18n()
  const authStore = useAuthStore(pinia)
  await authStore.restoreSession()
  app.mount('#app')
}

void bootstrap()
