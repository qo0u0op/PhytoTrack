<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { aiApi, caseApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { statusBadgeClass, statusLabel } from '../utils/caseStatus'
import type { components } from '../types/api'

const route = useRoute()
const auth = useAuthStore()

type CaseResponse = components['schemas']['CaseResponse']

const id = Number(route.params.id)
const loading = ref(true)
const notFound = ref(false)
const detail = ref<CaseResponse | null>(null)

// AI 診斷（即時分析，不持久化）：STAFF+ 限定
const analyzing = ref(false)
const aiSuggestion = ref<string | null>(null)
const aiElapsed = ref<number | null>(null)
const aiError = ref<string | null>(null)

onMounted(async () => {
  try {
    const { data } = await caseApi.detail(id)
    detail.value = data
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
})

const join = (items?: { id?: number; name?: string }[]) =>
  items?.map((i) => i.name).filter(Boolean).join('、') ?? '無'

const formatTime = (v?: string) => (v ? v.replace('T', ' ').slice(0, 19) : '—')

function printDetail() {
  window.print()
}

// AI 診斷：以明細欄位組出請求送後端代理 llama.cpp（結果僅供參考）
async function runAi() {
  if (!detail.value) return
  analyzing.value = true
  aiSuggestion.value = null
  aiError.value = null
  try {
    const { data } = await aiApi.analyze({
      cropName: detail.value.cropName,
      damages: detail.value.damages?.map((d) => d.name).filter((x): x is string => !!x),
      pestCategories: detail.value.pestCategories?.map((p) => p.name).filter((x): x is string => !!x),
      pestDescription: detail.value.pestDescription,
      cropScale: detail.value.cropScale,
      damageScale: detail.value.damageScale,
      cultivationMethod: detail.value.methodName,
      hintDescription: detail.value.hintDescription,
    })
    aiSuggestion.value = data.suggestion ?? ''
    aiElapsed.value = data.elapsedMs
  } catch {
    aiError.value = 'AI 診斷暫時無法使用（模型未啟動或服務異常）'
  } finally {
    analyzing.value = false
  }
}

// 匯出 CSV：以 blob 下載（axios 已自動附 JWT；blob 避免觸發瀏覽器跳頁）
async function downloadCsv() {
  try {
    const res = await caseApi.exportCsv()
    const url = URL.createObjectURL(res.data as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `case-export-${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } catch {
    // 錯誤由攔截器處理
  }
}
</script>

<template>
  <div class="container py-4" style="max-width: 900px">
    <div v-if="loading" class="text-center text-muted py-5">載入中…</div>

    <div v-else-if="notFound || !detail" class="text-center text-muted py-5">
      找不到案件 #{{ id }}
      <router-link class="d-block mt-2" to="/cases">返回案件列表</router-link>
    </div>

    <template v-else>
      <div class="d-flex justify-content-between align-items-center mb-3 no-print">
        <h4 class="mb-0">案件 #{{ detail.caseId }}</h4>
        <div>
          <button class="btn btn-outline-secondary me-1" @click="printDetail">列印</button>
          <button class="btn btn-outline-success me-1" @click="downloadCsv">匯出 CSV</button>
          <router-link class="btn btn-outline-primary" :to="`/cases/${id}/edit`">編輯</router-link>
        </div>
      </div>

      <!-- 診斷單（print-area：列印時僅輸出此區） -->
      <div class="card shadow-sm print-area">
        <div class="card-header bg-success text-white d-print-none">
          診斷記錄表
          <span class="badge float-end" :class="statusBadgeClass(detail.status)">
            {{ statusLabel(detail.status) }}
          </span>
        </div>
        <div class="card-body">
          <h5 class="mb-3 d-print-none">診斷記錄表</h5>
          <div class="row g-3 small">
            <div class="col-md-6">
              <strong>收件日期：</strong>{{ detail.receiveDate }}
            </div>
            <div class="col-md-6">
              <strong>案件編號：</strong>#{{ detail.caseId }}
            </div>
            <div class="col-md-6">
              <strong>作物：</strong>{{ detail.cropName ?? '無' }}
            </div>
            <div class="col-md-6">
              <strong>耕種方式：</strong>{{ detail.methodName ?? '無' }}
            </div>
            <div class="col-md-6">
              <strong>種植面積：</strong>{{ detail.cropScale ?? '無' }}
            </div>
            <div class="col-md-6">
              <strong>被害面積：</strong>{{ detail.damageScale ?? '無' }}
            </div>
            <div class="col-md-6">
              <strong>被害部位：</strong>{{ join(detail.damages) }}
            </div>
            <div class="col-md-6">
              <strong>病蟲害分類：</strong>{{ join(detail.pestCategories) }}
            </div>
            <div class="col-12">
              <strong>送件人：</strong>{{ detail.senderName ?? '無' }}
              <template v-if="detail.senderPhone">（{{ detail.senderPhone }}）</template>
            </div>
            <div class="col-12">
              <strong>縣市鄉鎮：</strong>{{ detail.senderDistrictName ?? '無' }}　
              <strong>地址：</strong>{{ detail.senderAddress ?? '無' }}
            </div>
            <div class="col-12">
              <strong>服務：</strong>{{ detail.serviceName ?? '無' }}　
              <strong>交付：</strong>{{ detail.deliveryName ?? '無' }}
            </div>
            <div class="col-12">
              <strong>防治建議：</strong>{{ join(detail.hints) }}
            </div>
            <div class="col-12">
              <strong>診斷簽名人：</strong>{{ join(detail.identifiers) }}
            </div>
            <div class="col-12">
              <hr class="my-2" />
              <strong>病害情形描述：</strong>
              <p class="mb-2">{{ detail.pestDescription ?? '無' }}</p>
              <strong>防治措施：</strong>
              <p class="mb-0">{{ detail.hintDescription ?? '無' }}</p>
            </div>
            <div class="col-12 text-muted">
              建立者：{{ detail.createdByName ?? '—' }}／建立：{{ formatTime(detail.createdAt) }}
              ／更新：{{ formatTime(detail.updatedAt) }}
            </div>
          </div>
        </div>
      </div>

      <!-- AI 診斷（僅診斷員以上，no-print） -->
      <div v-if="auth.isStaff" class="card shadow-sm mt-4 no-print">
        <div class="card-body">
          <h6 class="card-title">AI 診斷（llama.cpp）</h6>
          <button class="btn btn-sm btn-success mb-3" :disabled="analyzing" @click="runAi">
            {{ analyzing ? '診斷中…' : '執行 AI 診斷' }}
          </button>
          <div v-if="aiError" class="alert alert-warning mb-0 small">{{ aiError }}</div>
          <div v-else-if="aiSuggestion !== null" class="small">
            <div class="text-muted mb-1">
              AI 建議（{{ aiElapsed ? (aiElapsed / 1000).toFixed(1) + ' 秒' : '—' }}）——僅供參考，
              正式診斷請由診斷員依專業確認。
            </div>
            <pre class="text-wrap mb-0 bg-light p-3 rounded">{{ aiSuggestion }}</pre>
          </div>
          <div v-else class="text-muted small">
            依本案件欄位（作物、被害部位、病蟲害、病害描述）呼叫 AI 提供初步診斷與防治建議。
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style>
/* 列印：僅輸出診斷單本體，隱藏導覽與操作區 */
@media print {
  .no-print {
    display: none !important;
  }
  .navbar,
  .footer {
    display: none !important;
  }
  body {
    padding-top: 0 !important;
  }
  .container {
    max-width: 100% !important;
    padding: 0 !important;
  }
  .card {
    border: 1px solid #000 !important;
    box-shadow: none !important;
  }
  .card-header {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
  .d-print-none {
    display: none !important;
  }
}
</style>