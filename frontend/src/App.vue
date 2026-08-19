<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { authApi } from './api'

// 取得登入狀態與路由（用於登出後跳轉）
const auth = useAuthStore()
const router = useRouter()

async function handleLogout() {
  // 通知後端（JWT 無狀態，主要清除前端 token）
  await authApi.logout()
  auth.logout()
  router.push('/')
}
</script>

<template>
  <nav class="navbar navbar-expand-lg navbar-dark bg-success fixed-top">
    <div class="container">
      <router-link class="navbar-brand fw-bold" to="/">PhytoTrack</router-link>
      <button
        class="navbar-toggler"
        type="button"
        data-bs-toggle="collapse"
        data-bs-target="#mainNav"
      >
        <span class="navbar-toggler-icon"></span>
      </button>
      <div id="mainNav" class="collapse navbar-collapse">
        <ul v-if="auth.isAuthenticated" class="navbar-nav me-auto">
          <li class="nav-item">
            <router-link class="nav-link" to="/dashboard">儀表板</router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" to="/cases">案件管理</router-link>
          </li>
          <li v-if="auth.isAdmin" class="nav-item">
            <router-link class="nav-link" to="/users">使用者管理</router-link>
          </li>
        </ul>
        <ul class="navbar-nav ms-auto">
          <template v-if="auth.isAuthenticated">
            <li class="nav-item">
              <span class="nav-link">
                {{ auth.user?.displayName }}
                <span class="badge bg-light text-success">{{ auth.user?.role }}</span>
              </span>
            </li>
            <li class="nav-item">
              <button class="btn btn-outline-light btn-sm ms-2" @click="handleLogout">
                登出
              </button>
            </li>
          </template>
          <template v-else>
            <li class="nav-item">
              <router-link class="btn btn-outline-light btn-sm me-2" to="/login">登入</router-link>
            </li>
            <li class="nav-item">
              <router-link class="btn btn-light btn-sm" to="/register">註冊</router-link>
            </li>
          </template>
        </ul>
      </div>
    </div>
  </nav>

  <router-view />
</template>
