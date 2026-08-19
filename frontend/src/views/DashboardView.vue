<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { aiApi, caseApi } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

// 案件總數（向列表 API 要一頁即可取得 totalElements）
const totalCases = ref<number | null>(null)
const modelHealthy = ref<boolean | null>(null)

onMounted(async () => {
  try {
    const { data } = await caseApi.list({ page: 0, size: 1 })
    totalCases.value = data.totalElements
  } catch {
    totalCases.value = 0
  }
  try {
    const { data } = await aiApi.health()
    modelHealthy.value = data.healthy
  } catch {
    modelHealthy.value = false
  }
})
</script>

<template>
  <div class="container py-4">
    <h4 class="mb-4">您好，{{ auth.user?.displayName }}</h4>

    <div class="row g-4 mb-4">
      <div class="col-md-4">
        <div class="card shadow-sm">
          <div class="card-body">
            <h6 class="text-muted">診斷案件總數</h6>
            <div class="fs-1 fw-bold">{{ totalCases ?? '…' }}</div>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card shadow-sm">
          <div class="card-body">
            <h6 class="text-muted">AI 模型（llama.cpp）</h6>
            <div class="fs-1 fw-bold">
              <span v-if="modelHealthy === null" class="text-warning">…</span>
              <span v-else-if="modelHealthy" class="text-success">已連線</span>
              <span v-else class="text-danger">未連線</span>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card shadow-sm">
          <div class="card-body">
            <h6 class="text-muted">我的角色</h6>
            <div class="fs-4 fw-bold">{{ auth.user?.role }}</div>
            <div class="text-muted small">{{ auth.user?.username }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="row g-3">
      <div class="col-md-4">
        <router-link class="btn btn-success w-100 py-3" :to="auth.isStaff ? '/cases/new' : '/cases'">
          {{ auth.isStaff ? '建立新診斷案件' : '瀏覽案件列表' }}
        </router-link>
      </div>
      <div class="col-md-4">
        <router-link class="btn btn-outline-success w-100 py-3" to="/cases">案件管理</router-link>
      </div>
      <div v-if="auth.isAdmin" class="col-md-4">
        <router-link class="btn btn-outline-success w-100 py-3" to="/users">使用者管理</router-link>
      </div>
    </div>
  </div>
</template>
