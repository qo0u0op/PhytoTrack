<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { aiApi, caseApi } from '../api'
import { useAuthStore } from '../stores/auth'
import type { components } from '../types/api'

const auth = useAuthStore()

type CaseStatistics = components['schemas']['CaseStatisticsResponse']

// 統計總覽（由 GET /cases/statistics 提供）＋AI 連線狀態
const stats = ref<CaseStatistics | null>(null)
const modelHealthy = ref<boolean | null>(null)

onMounted(async () => {
  try {
    const { data } = await caseApi.statistics()
    stats.value = data
  } catch {
    stats.value = null
  }
  try {
    const { data } = await aiApi.health()
    modelHealthy.value = data.healthy
  } catch {
    modelHealthy.value = false
  }
})

const total = () => stats.value?.totalCases ?? 0

const percent = (count?: number) => {
  const t = total()
  return t === 0 ? 0 : Math.round(((count ?? 0) / t) * 100)
}

const statusLabel = (status?: string) =>
  status === 'PENDING' ? '待處理' : status === 'RESOLVED' ? '已診斷' : '已結案'

const barClass = (status?: string) =>
  status === 'PENDING' ? 'bg-warning' : status === 'RESOLVED' ? 'bg-success' : 'bg-secondary'
</script>

<template>
  <div class="container py-4">
    <h4 class="mb-4">您好，{{ auth.user?.displayName }}</h4>

    <!-- 統計卡片 -->
    <div class="row g-4 mb-4">
      <div class="col-md-3">
        <div class="card shadow-sm">
          <div class="card-body">
            <h6 class="text-muted">診斷案件總數</h6>
            <div class="fs-1 fw-bold">{{ stats?.totalCases ?? '…' }}</div>
          </div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card shadow-sm">
          <div class="card-body">
            <h6 class="text-muted">本月新增</h6>
            <div class="fs-1 fw-bold">{{ stats?.monthNewCases ?? '…' }}</div>
          </div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="card shadow-sm">
          <div class="card-body">
            <h6 class="text-muted">待處理</h6>
            <div class="fs-1 fw-bold">{{ stats?.pendingCases ?? '…' }}</div>
          </div>
        </div>
      </div>
      <div class="col-md-3">
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
    </div>

    <!-- 狀態比例 -->
    <div class="card shadow-sm mb-4">
      <div class="card-body">
        <h6 class="card-title text-muted">案件狀態比例</h6>
        <div v-if="!stats" class="text-muted">載入中…</div>
        <div v-for="sc in stats?.statusRatio ?? []" v-else :key="sc.status" class="mb-2">
          <div class="d-flex justify-content-between small mb-1">
            <span>{{ statusLabel(sc.status) }}</span>
            <span>{{ sc.count }} 件（{{ percent(sc.count) }}%）</span>
          </div>
          <div class="progress" role="progressbar" :aria-valuenow="percent(sc.count)"
            aria-valuemin="0" aria-valuemax="100">
            <div class="progress-bar" :class="barClass(sc.status)"
              :style="{ width: percent(sc.count) + '%' }"></div>
          </div>
        </div>
      </div>
    </div>

    <div class="row g-4 mb-4">
      <div class="col-md-6">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h6 class="card-title text-muted">常見作物（Top 5）</h6>
            <table class="table table-sm mb-0">
              <thead>
                <tr><th>作物</th><th class="text-end">件數</th></tr>
              </thead>
              <tbody>
                <tr v-for="c in stats?.topCrops ?? []" :key="c.name">
                  <td>{{ c.name }}</td><td class="text-end">{{ c.count }}</td>
                </tr>
                <tr v-if="!stats?.topCrops?.length">
                  <td colspan="2" class="text-muted">尚無資料</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <div class="col-md-6">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h6 class="card-title text-muted">常見病蟲害（Top 5）</h6>
            <table class="table table-sm mb-0">
              <thead>
                <tr><th>病蟲害</th><th class="text-end">件數</th></tr>
              </thead>
              <tbody>
                <tr v-for="c in stats?.topPestCategories ?? []" :key="c.name">
                  <td>{{ c.name }}</td><td class="text-end">{{ c.count }}</td>
                </tr>
                <tr v-if="!stats?.topPestCategories?.length">
                  <td colspan="2" class="text-muted">尚無資料</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- 近 6 月趨勢 -->
    <div class="card shadow-sm mb-4">
      <div class="card-body">
        <h6 class="card-title text-muted">近 6 月案件趨勢</h6>
        <table class="table table-sm mb-0">
          <thead>
            <tr><th>月份</th><th class="text-end">案件數</th></tr>
          </thead>
          <tbody>
            <tr v-for="m in stats?.monthlyTrend ?? []" :key="m.month">
              <td>{{ m.month }}</td><td class="text-end">{{ m.count }}</td>
            </tr>
          </tbody>
        </table>
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