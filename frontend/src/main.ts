import { createApp } from 'vue'
import { createPinia } from 'pinia'

// Bootstrap 樣式與 JS (下拉選單、折疊選單等互動元件)
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import 'bootstrap-icons/font/bootstrap-icons.css'

import './style.css'
import App from './App.vue'
import router from './router'
import { useThemeStore } from './stores/theme'

// 建立 Vue 應用：掛載 Pinia (狀態管理) 與 Router (路由)
// 主題需早於 mount 寫入 data-bs-theme，避免首屏白閃
const pinia = createPinia ()
const app = createApp (App).use (pinia).use (router)
useThemeStore (pinia).init ()
app.mount ('#app')
