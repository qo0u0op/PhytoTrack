<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { aiApi, caseApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { statusBadgeClass, statusLabel } from '../utils/caseStatus'
import type { components } from '../types/api'

const route = useRoute ()
const auth = useAuthStore ()

type CaseResponse = components['schemas']['CaseResponse']

// 路由參數保持響應式：/cases/1 直接切換 /cases/2 時元件被複用，需重抓資料
const id = computed (() => Number (route.params.id))
const loading = ref (true)
const notFound = ref (false)
const detail = ref<CaseResponse | null>(null)

// AI 診斷 (即時分析，不持久化)：STAFF+ 限定
const analyzing = ref (false)
const aiSuggestion = ref<string | null>(null)
const aiElapsed = ref<number | null>(null)
const aiError = ref<string | null>(null)

async function load () {
  loading.value = true
  notFound.value = false
  detail.value = null
  // 換案件時清掉前一件的 AI 結果，避免顯示舊資料
  aiSuggestion.value = null
  aiElapsed.value = null
  aiError.value = null
  try {
    const { data } = await caseApi.detail (id.value)
    detail.value = data
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

watch (id, load, { immediate: true })

const join = (items?: { id?: number; name?: string }[]) =>
  items?.map ((i) => i.name).filter (Boolean).join ('、') ?? '無'

// Q4：其他→其他回覆
const joinHints = (items?: { id?: number; name?: string }[]) =>
  items?.map ((i) => (i.name === '其他' ? '其他回覆' : i.name)).filter (Boolean).join ('、') ?? '無'

// 五類分組已改為逐筆 type-category 顯示，無需 pestGroup

// 送件人顯示：支援 name (displayName)；VIEWER 時後端***，顯示***(***)
function senderLabel (d: CaseResponse) {
  if (auth.isViewer) {
    return '***(***)'
  }
  const name = d.senderName
  const display = d.senderDisplayName
  const hasName = name && name.trim ()
  const hasDisplay = display && display.trim ()
  if (hasName && hasDisplay) return `${name} (${display})`
  if (hasDisplay) return display!
  if (hasName) return name!
  return d.senderPhone ?? '—'
}

const formatTime = (v?: string) => (v ? v.replace ('T', ' ').slice (0, 19) : '—')

function printDetail () {
  window.print ()
}

// AI 診斷：以明細欄位組出請求送後端代理 llama.cpp (結果僅供參考)
async function runAi () {
  if (!detail.value) return
  analyzing.value = true
  aiSuggestion.value = null
  aiError.value = null
  try {
    const pestNotes = detail.value.pestCategories?.map ((p) => p.pestNote).filter ((x): x is string => !!x && x.trim ().length > 0) as string[] | undefined
    const { data } = await aiApi.analyze ({
      cropName: detail.value.cropName ?? '',
      damages: detail.value.damages?.map ((d) => d.name).filter ((x): x is string => !!x),
      pestCategories: detail.value.pestCategories?.map ((p) => p.name).filter ((x): x is string => !!x),
      pestNotes: pestNotes && pestNotes.length > 0 ? pestNotes : undefined,
      caseDescription: detail.value.caseDescription ?? undefined,
      cropScale: detail.value.cropScale ?? undefined,
      damageScale: detail.value.damageScale ?? undefined,
      cultivationMethod: detail.value.methodName ?? undefined,
      hintDescription: detail.value.hintDescription ?? undefined,
    } as unknown as Record<string, unknown>)
    aiSuggestion.value = data.suggestion ?? ''
    aiElapsed.value = data.elapsedMs
    // 提示診斷完成 (僅在成功時)
    import ('sweetalert2').then (({ default: Swal }) => {
      Swal.fire ({ icon: 'success', title: 'AI 診斷完成', timer: 1500, showConfirmButton: false })
    })
  } catch {
    aiError.value = 'AI 診斷暫時無法使用 (模型未啟動或服務異常)'
  } finally {
    analyzing.value = false
  }
}

function showAiTip () {
  // 手機無 hover，改以點擊彈窗顯示提示；桌機則同時支援 title hover
  import ('sweetalert2').then (({ default: Swal }) => {
    Swal.fire ({
      icon: 'info',
      title: 'AI 診斷提示',
      text: '依本案件欄位 (作物、被害部位、病蟲害、病害描述) 呼叫 AI 提供初步診斷與防治建議。',
      confirmButtonText: '了解',
    })
  })
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
        <div class="d-flex align-items-center gap-1">
          <button class="btn btn-outline-secondary btn-sm" @click="printDetail">列印</button>
          <router-link
            v-if="auth.isStaff && (detail.status !== 'CLOSED' || auth.isAdmin)"
            class="btn btn-outline-primary btn-sm"
            :to="`/cases/${id}/edit`"
          >
            編輯
          </router-link>
          <button
            v-if="auth.isStaff"
            class="btn btn-success btn-sm ms-1"
            :disabled="analyzing"
            @click="runAi"
          >
            {{ analyzing ? '診斷中…' : 'AI 診斷' }}
          </button>
          <span
            v-if="auth.isStaff"
            class="text-muted small"
            role="button"
            tabindex="0"
            @click="showAiTip"
            @keydown.enter="showAiTip"
            style="cursor: help; user-select: none;"
            aria-label="AI 診斷說明"
          >ⓘ</span>
        </div>
      </div>


      <!-- 診斷單 (print-area：列印時僅輸出此區) -->
      <div class="card shadow-sm print-area">
        <div class="card-header bg-success text-white d-print-none">
          診斷記錄表
          <span class="badge float-end" :class="statusBadgeClass (detail.status)">
            {{ statusLabel (detail.status) }}
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
            <div class="col-12">
              <strong>病蟲害發生地點：</strong>{{ (detail as any).fieldCityName ?? '' }}{{ (detail as any).fieldDistrictName ?? '無' }}
              <span v-if="(detail as any).fieldDistrictId && (detail as any).fieldDistrictId === detail.senderDistrictId" class="text-muted"> (同寄件人)</span>
            </div>
            <div class="col-md-6">
              <strong>送件人：</strong>{{ senderLabel (detail) }}
            </div>
            <div class="col-md-6">
              <strong>電話：</strong>{{ auth.isViewer ? '***' : (detail.senderPhone ?? '—') }}
            </div>
            <div class="col-md-6">
              <strong>身分別：</strong>{{ (detail as any).senderTypeName ?? '—' }}
            </div>
            <div class="col-12">
              <strong>地址：</strong>{{ detail.senderCityName ?? '' }}{{ detail.senderDistrictName ?? '' }}{{ auth.isViewer ? '***' : (detail.senderAddress ?? '') }}
            </div>
            <div class="col-12"><hr class="my-2" /></div>
            <div class="col-md-6">
              <strong>耕種方式：</strong>{{ detail.methodName ?? '無' }}
            </div>
            <div class="col-md-6">
              <strong>作物種類：</strong>{{ (detail as any).cropCategoryName ?? '—' }}
            </div>
            <div class="col-12">
              <strong>作物名稱：</strong>{{ detail.cropName ?? '無' }}
            </div>
            <div class="col-md-6">
              <strong>被害部位：</strong>{{ join (detail.damages) }}
            </div>
            <div class="col-md-6">
              <strong>栽培面積：</strong>{{ detail.cropScale ?? '無' }}
            </div>
            <div class="col-md-6">
              <strong>被害面積：</strong>{{ detail.damageScale ?? '無' }}
            </div>
            <div class="col-12">
              <strong>土壤、栽培、用藥紀錄：</strong>
              <p class="mb-0">{{ detail.caseDescription ?? '無' }}</p>
            </div>
            <div class="col-12"><hr class="my-2" /></div>
            <div class="col-md-6">
              <strong>服務類別：</strong>{{ detail.serviceName ?? '無' }}
            </div>
            <div class="col-md-6">
              <strong>送件方式：</strong>{{ detail.deliveryName ?? '無' }}
            </div>
            <div class="col-12">
              <strong>鑑定者：</strong>{{ join (detail.identifiers) }}
            </div>
            <template v-if="detail.pestCategories && detail.pestCategories.length > 0">
              <div class="col-12" style="font-size:14px">
                <strong>診斷結果：</strong>
              </div>
              <div class="col-12 ms-3" style="font-size:14px">
                <div v-for="p in detail.pestCategories" :key="p.id">
                  • {{ (p as any).pestTypeName }}-{{ p.name }}<span v-if="p.pestNote"> ({{ p.pestNote }})</span>
                </div>
              </div>
            </template>
            <div class="col-12">
              <strong>防治建議：</strong>{{ joinHints (detail.hints) }}
            </div>
            <div class="col-12">
              <strong>建議採取措施：</strong>
              <p class="mb-0">{{ detail.hintDescription ?? '無' }}</p>
            </div>
            <div class="col-12"><hr class="my-2" /></div>
            <div class="col-12 text-muted">
              建立者：{{ detail.createdByName ?? '—' }}／建立：{{ formatTime (detail.createdAt) }}
              ／更新：{{ formatTime (detail.updatedAt) }}
            </div>
          </div>
        </div>
      </div>

      <!-- AI 診斷結果 (僅診斷員以上，no-print)— 僅在按下後顯示 -->
      <div v-if="auth.isStaff && (aiSuggestion !== null || aiError !== null || analyzing)" class="card shadow-sm mt-4 no-print">
        <div class="card-body">
          <h6 class="card-title">AI 診斷結果</h6>
          <div v-if="aiError" class="alert alert-warning mb-0 small">{{ aiError }}</div>
          <div v-else-if="aiSuggestion !== null" class="small">
            <div class="text-muted mb-1">
              AI 建議 ({{ aiElapsed ? (aiElapsed / 1000).toFixed (1) + ' 秒' : '—' }})——僅供參考，
              正式診斷請由診斷員依專業確認。
            </div>
            <pre class="text-wrap mb-0 bg-light p-3 rounded">{{ aiSuggestion }}</pre>
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