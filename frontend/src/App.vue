<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { authApi } from './api'

// 取得登入狀態與路由 (用於登出後跳轉)
const auth = useAuthStore ()
const router = useRouter ()

async function handleLogout () {
  // 通知後端 (JWT 無狀態，主要清除前端 token)
  await authApi.logout ()
  auth.logout ()
  router.push ('/')
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
          <li v-if="auth.isStaff" class="nav-item">
            <router-link class="nav-link" to="/admin/senders">送件人管理</router-link>
          </li>
          <li v-if="auth.isStaff" class="nav-item">
            <router-link class="nav-link" to="/admin/crops">作物管理</router-link>
          </li>
          <li v-if="auth.isAdmin" class="nav-item">
            <router-link class="nav-link" to="/admin/pest-categories">害物管理</router-link>
          </li>
          <li v-if="auth.isAdmin" class="nav-item">
            <router-link class="nav-link" to="/admin/reference-data">參照資料管理</router-link>
          </li>
          <li v-if="auth.isAdmin" class="nav-item">
            <router-link class="nav-link" to="/users">使用者管理</router-link>
          </li>
        </ul>
        <ul class="navbar-nav ms-auto">
          <template v-if="auth.isAuthenticated">
            <li class="nav-item">
              <router-link class="nav-link d-flex align-items-center gap-1" to="/account">
                {{ auth.user?.displayName }}
                <span
                  class="badge bg-light rounded-pill p-1 d-inline-flex align-items-center justify-content-center"
                  :title="auth.user?.role === 'ROLE_ADMIN' ? '管理者' : auth.user?.role === 'ROLE_STAFF' ? '診斷員' : '檢視者'"
                  :class="auth.user?.role === 'ROLE_ADMIN' ? 'text-warning' : auth.user?.role === 'ROLE_STAFF' ? 'text-primary' : 'text-info'"
                >
                  <!-- ROLE_VIEWER：eye -->
                  <svg v-if="auth.user?.role === 'ROLE_VIEWER'" xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" viewBox="0 0 16 16" aria-hidden="true">
                    <path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>
                    <path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0z"/>
                  </svg>
                  <!-- ROLE_STAFF：clipboard-check / 診斷 -->
                  <svg v-else-if="auth.user?.role === 'ROLE_STAFF'" xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" viewBox="0 0 16 16" aria-hidden="true">
                    <path d="M10.5 3a.5.5 0 0 0-.5-.5h-3a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h3a.5.5 0 0 0 .5-.5V3z"/>
                    <path d="M4 1.5H3a2 2 0 0 0-2 2V14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V3.5a2 2 0 0 0-2-2h-1v1h1a1 1 0 0 1 1 1V14a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V3.5a1 1 0 0 1 1-1h1v-1z"/>
                    <path d="M10.854 7.146a.5.5 0 0 1 0 .708l-3 3a.5.5 0 0 1-.708 0l-1.5-1.5a.5.5 0 1 1 .708-.708L7.5 9.793l2.646-2.647a.5.5 0 0 1 .708 0z"/>
                  </svg>
                  <!-- ROLE_ADMIN：shield-lock -->
                  <svg v-else xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" viewBox="0 0 16 16" aria-hidden="true">
                    <path d="M5.338 1.59a61.44 61.44 0 0 0-2.837.856.481.481 0 0 0-.328.39c-.554 4.157.726 7.19 2.253 9.188a10.725 10.725 0 0 0 2.287 2.233c.346.244.652.42.893.533.12.057.218.095.293.118a.55.55 0 0 0 .101.025.615.615 0 0 0 .1-.025c.076-.023.174-.061.294-.118.24-.113.547-.29.893-.533a10.726 10.726 0 0 0 2.287-2.233c1.527-1.997 2.807-5.031 2.253-9.188a.48.48 0 0 0-.328-.39c-.651-.213-1.75-.56-2.837-.855C9.552 1.29 8.531 1.067 8 1.067c-.53 0-1.552.223-2.662.524z"/>
                    <path d="M9.5 6.5a1.5 1.5 0 0 1-1 1.415l.385 1.99a.5.5 0 0 1-.491.595h-.788a.5.5 0 0 1-.49-.595l.384-1.99a1.5 1.5 0 1 1 2-1.415z"/>
                  </svg>
                </span>
              </router-link>
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
