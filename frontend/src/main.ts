import { createApp } from 'vue'
import { createPinia } from 'pinia'

// Bootstrap 樣式與 JS（下拉選單、折疊選單等互動元件）
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'

import './style.css'
import App from './App.vue'
import router from './router'

// 建立 Vue 應用：掛載 Pinia（狀態管理）與 Router（路由）
createApp(App).use(createPinia()).use(router).mount('#app')
