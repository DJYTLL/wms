import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import './styles/table.css'

import App from './App.vue'
import router from './router'
import i18n, { setupI18n } from './i18n'
import { permission } from './directives/permission'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)
app.directive('permission', permission) // 注册全局自定义指令 'permission'

const bootstrap = async () => {
  await setupI18n()
  app.mount('#app')
}

void bootstrap()
