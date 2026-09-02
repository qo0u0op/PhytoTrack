<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { aiApi, caseApi } from '../api'
import { useAuthStore } from '../stores/auth'
import type { components } from '../types/api'

const auth = useAuthStore ()

type CaseStatistics = components['schemas']['CaseStatisticsResponse']

// 統計總覽 (由 GET /cases/statistics 提供)＋AI 連線狀態
const stats = ref<CaseStatistics | null>(null)
const modelHealthy = ref<boolean | null>(null)
const period = ref<'HISTORICAL' | 'ANNUAL' | 'MONTHLY' | 'HALF_YEAR'>('HISTORICAL')
const selectedYear = ref<number | null>(null)
const selectedMonth = ref<number | null>(new Date ().getMonth () + 1)
const selectedHalf = ref<1 | 2>(1)


const availableYears = computed (() => stats.value?.availableYears ?? [])



async function loadStats () {
  try {
    const params: Record<string, string | number> = {}
    params.period = period.value
    if (period.value === 'ANNUAL' || period.value === 'MONTHLY' || period.value === 'HALF_YEAR') {
      if (selectedYear.value) params.year = selectedYear.value
    }
    if (period.value === 'MONTHLY' && selectedMonth.value) params.month = selectedMonth.value
    if (period.value === 'HALF_YEAR') params.half = selectedHalf.value
    const { data } = await caseApi.statistics (params)
    stats.value = data as CaseStatistics
    // 若尚未選年且有可用年份，預設最新年
    if (!selectedYear.value && availableYears.value.length > 0) {
      selectedYear.value = availableYears.value[0]
    }
  } catch {
    stats.value = null
  }
}

onMounted (async () => {
  await loadStats ()
  try {
    const { data } = await aiApi.health ()
    modelHealthy.value = (data as unknown as { healthy: boolean }).healthy
  } catch {
    modelHealthy.value = false
  }
})

watch ([period, selectedYear, selectedMonth, selectedHalf], () => {
  loadStats ()
})

const total = () => stats.value?.periodTotal ?? stats.value?.totalCases ?? 0

const percent = (count?: number) => {
  const t = total ()
  return t === 0 ? "0.0" : (((count ?? 0) / t) * 100).toFixed (1)
}

const statusLabel = (status?: string) =>
  status === 'PENDING' ? '待處理' : status === 'RESOLVED' ? '已診斷' : '已結案'

const barClass = (status?: string) =>
  status === 'PENDING' ? 'bg-warning' : status === 'RESOLVED' ? 'bg-success' : 'bg-secondary'
</script>

<template>
  <div class="container py-4">
    <h4 class="mb-4">您好，{{ auth.user?.displayName }}</h4>

    <!-- 期別選擇器 -->
    <div class="card shadow-sm mb-4">
      <div class="card-body">
        <div class="row g-2 align-items-end">
          <div class="col-md-2">
            <label class="form-label small text-muted mb-1">期別</label>
            <select v-model="period" class="form-select form-select-sm">
              <option value="HISTORICAL">歷史</option>
              <option value="ANNUAL">年度</option>
              <option value="HALF_YEAR">半年度</option>
              <option value="MONTHLY">月度</option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">年份</label>
            <select v-model="selectedYear" class="form-select form-select-sm" :disabled="period === 'HISTORICAL'">
              <option v-for="y in availableYears" :key="y" :value="y">{{ y }} 年</option>
            </select>
            <div v-if="availableYears.length === 0" class="form-text small text-muted">尚無歷史年份</div>
          </div>
          <div class="col-md-2">
            <label class="form-label small text-muted mb-1">半年度</label>
            <select v-model="selectedHalf" class="form-select form-select-sm" :disabled="period !== 'HALF_YEAR'">
              <option :value="1">上半年</option>
              <option :value="2">下半年</option>
            </select>
          </div>
          <div class="col-md-2">
            <label class="form-label small text-muted mb-1">月份</label>
            <select v-model="selectedMonth" class="form-select form-select-sm" :disabled="period !== 'MONTHLY'">
              <option v-for="m in 12" :key="m" :value="m">{{ m }} 月</option>
            </select>
          </div>
          <div class="col-md-3">
            <div class="small text-muted">期別案件數：<strong>{{ total () }}</strong> 件</div>
          </div>
        </div>
      </div>
    </div>

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
            <h6 class="text-muted">模型狀態</h6>
            <div class="fs-1 fw-bold">
              <span v-if="modelHealthy === null" class="text-warning">…</span>
              <span v-else-if="modelHealthy" class="text-success">已連線</span>
              <span v-else class="text-danger">未連線</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 複合因素 / 複合建議 (獨立新卡，複合案件已合併至複合因素) -->
    <div class="row g-4 mb-4">
      <div class="col-md-4">
        <div class="card shadow-sm border-warning">
          <div class="card-body">
            <h6 class="text-muted">複合因素</h6>
            <div class="fs-1 fw-bold">{{ stats?.compositeFactorCases ?? stats?.compositeCases ?? '…' }}</div>
            <div class="small text-muted">害物、因素 &gt;1 組</div>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card shadow-sm border-warning">
          <div class="card-body">
            <h6 class="text-muted">複合建議</h6>
            <div class="fs-1 fw-bold">{{ stats?.compositeHintCases ?? '…' }}</div>
            <div class="small text-muted">防治建議 &gt;1 組</div>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card shadow-sm">
          <div class="card-body">
            <h6 class="text-muted">期別案件數</h6>
            <div class="fs-1 fw-bold">{{ total () }}</div>
            <div class="small text-muted">{{ period === 'HISTORICAL' ? '歷史' : period === 'ANNUAL' ? `${selectedYear} 年` : period === 'MONTHLY' ? `${selectedYear} 年 ${selectedMonth} 月` : `${selectedYear} 年 ${selectedHalf === 1 ? '上半年' : '下半年'}` }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 狀態比例 -->
    <div class="card shadow-sm mb-4">
      <div class="card-body">
        <h6 class="card-title text-muted">案件狀態比例 (期別內)</h6>
        <div v-if="!stats" class="text-muted">載入中…</div>
        <div v-for="sc in stats?.statusRatio ?? []" v-else :key="sc.status" class="mb-2">
          <div class="d-flex justify-content-between small mb-1">
            <span>{{ statusLabel (sc.status) }}</span>
            <span>{{ sc.count }} 件 ({{ percent (sc.count) }}%)</span>
          </div>
          <div class="progress" role="progressbar" :aria-valuenow="percent (sc.count)"
            aria-valuemin="0" aria-valuemax="100">
            <div class="progress-bar" :class="barClass (sc.status)"
              :style="{ width: percent (sc.count) + '%' }"></div>
          </div>
        </div>
      </div>
    </div>

        <div class="row g-4 mb-4">
      <div class="col-md-4">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h6 class="card-title text-muted">常見作物 (Top 10)</h6>
            <table class="table table-sm mb-0">
              <thead>
                <tr><th>作物</th><th class="text-end">案件數</th></tr>
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
      <div class="col-md-4">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h6 class="card-title text-muted">常見因素 (Top 10)</h6>
            <table class="table table-sm mb-0">
              <thead>
                <tr><th>因素</th><th class="text-end">案件數</th></tr>
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
      <div class="col-md-4">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h6 class="card-title text-muted">田區位置 (Top 10)</h6>
            <table class="table table-sm mb-0">
              <thead><tr><th>縣市</th><th class="text-end">案件數</th></tr></thead>
              <tbody>
                <tr v-for="c in (stats?.fieldCityBreakdown ?? []).slice(0, 10)" :key="c.name">
                  <td>{{ c.name }}</td><td class="text-end">{{ c.count }}</td>
                </tr>
                <tr v-if="!stats?.fieldCityBreakdown?.length"><td colspan="2" class="text-muted">尚無資料</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- 期別 Breakdown -->
    <div class="row g-4 mb-4">
      <div class="col-md-4">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h6 class="card-title text-muted">作物類別</h6>
            <table class="table table-sm mb-0">
              <thead><tr><th>類別</th><th class="text-end">件數</th><th class="text-end">佔比</th></tr></thead>
              <tbody>
                <tr v-for="c in stats?.cropCategoryBreakdown ?? []" :key="c.name">
                  <td>{{ c.name }}</td><td class="text-end">{{ c.count }}</td><td class="text-end">{{ percent (c.count) }}%</td>
                </tr>
                <tr v-if="!stats?.cropCategoryBreakdown?.length"><td colspan="3" class="text-muted">尚無資料</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h6 class="card-title text-muted">害物</h6>
            <table class="table table-sm mb-0">
              <thead><tr><th>害物</th><th class="text-end">件數</th><th class="text-end">佔比</th></tr></thead>
              <tbody>
                <tr v-for="c in stats?.pestTypeBreakdown ?? []" :key="c.name">
                  <td><span :class="c.name === '複合因素' ? 'badge bg-warning text-dark' : ''">{{ c.name }}</span></td><td class="text-end">{{ c.count }}</td><td class="text-end">{{ percent (c.count) }}%</td>
                </tr>
                <tr v-if="!stats?.pestTypeBreakdown?.length"><td colspan="3" class="text-muted">尚無資料</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h6 class="card-title text-muted">防治建議</h6>
            <table class="table table-sm mb-0">
              <thead><tr><th>建議</th><th class="text-end">件數</th><th class="text-end">佔比</th></tr></thead>
              <tbody>
                <tr v-for="c in stats?.hintBreakdown ?? []" :key="c.name">
                  <td><span :class="c.name === '複合建議' ? 'badge bg-warning text-dark' : ''">{{ c.name }}</span></td><td class="text-end">{{ c.count }}</td><td class="text-end">{{ percent (c.count) }}%</td>
                </tr>
                <tr v-if="!stats?.hintBreakdown?.length"><td colspan="3" class="text-muted">尚無資料</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div class="row g-4 mb-4">
      <div class="col-md-4">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h6 class="card-title text-muted">交付方式</h6>
            <table class="table table-sm mb-0">
              <thead><tr><th>方式</th><th class="text-end">件數</th><th class="text-end">佔比</th></tr></thead>
              <tbody>
                <tr v-for="c in stats?.deliveryBreakdown ?? []" :key="c.name">
                  <td>{{ c.name }}</td><td class="text-end">{{ c.count }}</td><td class="text-end">{{ percent (c.count) }}%</td>
                </tr>
                <tr v-if="!stats?.deliveryBreakdown?.length"><td colspan="3" class="text-muted">尚無資料</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h6 class="card-title text-muted">耕種方式</h6>
            <table class="table table-sm mb-0">
              <thead><tr><th>方式</th><th class="text-end">件數</th><th class="text-end">佔比</th></tr></thead>
              <tbody>
                <tr v-for="c in stats?.methodBreakdown ?? []" :key="c.name">
                  <td>{{ c.name }}</td><td class="text-end">{{ c.count }}</td><td class="text-end">{{ percent (c.count) }}%</td>
                </tr>
                <tr v-if="!stats?.methodBreakdown?.length"><td colspan="3" class="text-muted">尚無資料</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h6 class="card-title text-muted">近半年案件趨勢</h6>
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
      </div>
    </div>

  </div>
</template>
