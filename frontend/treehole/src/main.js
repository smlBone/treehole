import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import './assets/styles/global.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// 初始化主题
import { useThemeStore } from './stores/theme'
const themeStore = useThemeStore(pinia)

app.mount('#app')
