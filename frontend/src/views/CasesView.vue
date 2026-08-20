<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import Swal from 'sweetalert2'
import { caseApi, refApi } from '../api'
import { useAuthStore } from '../stores/auth'
import type { components } from '../types/api'
import { STATUS_OPTIONS, statusBadgeClass, statusLabel } from '../utils/caseStatus'

// 分頁資料型別（對應後端 Page<CaseSummaryResponse>）
interface CaseSummary {
  caseId: number
  receiveDate: string
  cropName: string
  senderName: string
  serviceName: string
  status: string
  createdAt: string
}

// 篩選條件（對應後端 GET /api/cases 查詢參數）
interface CaseFilters {
  cropId?: number
  serviceId?: number
  senderName: string
  receiveDateFrom: string
  receiveDateTo: string
  status: string
}

const auth = useAuthStore()

const cases = ref<CaseSummary[]>([])
const total = ref(0)
const page = ref(0)
const size = 10
const loading = ref(false)

// 篩選工具列狀態與選單資料
const filters = reactive<CaseFilters>({
  senderName: '',
  receiveDateFrom: '',
  receiveDateTo: '',
  status: '',
})
const cropOptions = ref<{ id?: number; name?: string }[]>([])
const serviceOptions = ref<{ id?: number; name?: string }[]>([])

async function load() {
  loading.value = true
  try {
    const params: Record<string, string | number> = { page: page.value, size }
    if (filters.cropId) params.cropId = filters.cropId
    if (filters.serviceId) params.serviceId = filters.serviceId
    if (filters.senderName.trim()) params.senderName = filters.senderName.trim()
    if (filters.receiveDateFrom) params.receiveDateFrom = filters.receiveDateFrom
    if (filters.receiveDateTo) params.receiveDateTo = filters.receiveDateTo
    if (filters.status) params.status = filters.status
    const { data } = await caseApi.list(params)
    cases.value = data.content
    total.value = data.totalElements
  } catch {
    // 錯誤由攔截器處理
  } finally {
    loading.value = false
  }
}

// 載入作物（由分類攤平）與服務類別做為下拉選單
async function loadFilterOptions() {
  try {
    const [cropRes, serviceRes] = await Promise.all([refApi.cropCategories(), refApi.services()])
    cropOptions.value = (cropRes.data as components['schemas']['CropCategoryResponse'][]).flatMap(
      (cat) => cat.crops ?? [],
    )
    serviceOptions.value = serviceRes.data as components['schemas']['IdNameResponse'][]
  } catch {
    // 錯誤由攔截器處理
  }
}

function applyFilters() {
  page.value = 0
  load()
}

function clearFilters() {
  filters.cropId = undefined
  filters.serviceId = undefined
  filters.senderName = ''
  filters.receiveDateFrom = ''
  filters.receiveDateTo = ''
  filters.status = ''
  page.value = 0
  load()
}

onMounted(() => {
  load()
  loadFilterOptions()
})

// 檢視案件詳細：導向獨立明細頁（列印診斷單、CSV 匯出於該頁）
async function confirmDelete(id: number) {
  const result = await Swal.fire({
    icon: 'warning',
    title: '確定刪除此案件？',
    text: '此操作無法復原',
    showCancelButton: true,
    confirmButtonText: '刪除',
    cancelButtonText: '取消',
  })
  if (result.isConfirmed) {
    try {
      await caseApi.remove(id)
      Swal.fire({ icon: 'success', title: '已刪除', timer: 1200, showConfirmButton: false })
      // 刪除後回到第一頁重新載入
      page.value = 0
      await load()
    } catch {
      // 錯誤由攔截器處理
    }
  }
}
</script>

<template>
  <div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h4>案件管理</h4>
      <router-link v-if="auth.isStaff" class="btn btn-success" to="/cases/new">建立案件</router-link>
    </div>

    <!-- 篩選工具列：條件同時存在時為 AND 組合 -->
    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <div class="row g-2 align-items-end">
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">作物</label>
            <select v-model="filters.cropId" class="form-select form-select-sm">
              <option :value="undefined">全部</option>
              <option v-for="crop in cropOptions" :key="crop.id" :value="crop.id">
                {{ crop.name }}
              </option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">服務類別</label>
            <select v-model="filters.serviceId" class="form-select form-select-sm">
              <option :value="undefined">全部</option>
              <option v-for="service in serviceOptions" :key="service.id" :value="service.id">
                {{ service.name }}
              </option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">送件人（部分比對）</label>
            <input v-model="filters.senderName" type="text" class="form-control form-control-sm" />
          </div>
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">狀態</label>
            <select v-model="filters.status" class="form-select form-select-sm">
              <option value="">全部</option>
              <option v-for="opt in STATUS_OPTIONS" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">收件日期起</label>
            <input v-model="filters.receiveDateFrom" type="date" class="form-control form-control-sm" />
          </div>
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">收件日期迄</label>
            <input v-model="filters.receiveDateTo" type="date" class="form-control form-control-sm" />
          </div>
          <div class="col-md-6 text-md-end">
            <button class="btn btn-sm btn-primary me-1" @click="applyFilters">篩選</button>
            <button class="btn btn-sm btn-outline-secondary" @click="clearFilters">清除</button>
          </div>
        </div>
      </div>
    </div>

    <div class="card shadow-sm">
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>編號</th>
              <th>收件日期</th>
              <th>作物</th>
              <th>送件人</th>
              <th>服務類別</th>
              <th>狀態</th>
              <th class="text-end">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="text-center text-muted py-4">載入中…</td>
            </tr>
            <tr v-else-if="cases.length === 0">
              <td colspan="7" class="text-center text-muted py-4">尚無案件</td>
            </tr>
            <tr v-for="c in cases" :key="c.caseId">
              <td>{{ c.caseId }}</td>
              <td>{{ c.receiveDate }}</td>
              <td>{{ c.cropName }}</td>
              <td>{{ c.senderName }}</td>
              <td>{{ c.serviceName }}</td>
              <td>
                <span class="badge" :class="statusBadgeClass(c.status)">{{ statusLabel(c.status) }}</span>
              </td>
              <td class="text-end">
                <router-link class="btn btn-sm btn-outline-success me-1" :to="`/cases/${c.caseId}`">
                  檢視
                </router-link>
                <template v-if="auth.isStaff">
                  <router-link
                    v-if="c.status !== 'CLOSED' || auth.isAdmin"
                    class="btn btn-sm btn-outline-primary me-1"
                    :to="`/cases/${c.caseId}/edit`"
                  >
                    編輯
                  </router-link>
                  <button
                    v-else
                    type="button"
                    class="btn btn-sm btn-outline-primary me-1"
                    disabled
                    title="案件已結案，僅管理者可編輯"
                  >
                    編輯
                  </button>
                </template>
                <button
                  v-if="auth.isAdmin"
                  class="btn btn-sm btn-outline-danger"
                  @click="confirmDelete(c.caseId)"
                >
                  刪除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 分頁控制 -->
    <nav v-if="total > size" class="mt-3">
      <ul class="pagination justify-content-center">
        <li class="page-item" :class="{ disabled: page === 0 }">
          <button class="page-link" @click="page--; load()">上一頁</button>
        </li>
        <li class="page-item disabled">
          <span class="page-link">
            第 {{ page + 1 }} 頁（共 {{ Math.ceil(total / size) }} 頁，{{ total }} 筆）
          </span>
        </li>
        <li class="page-item" :class="{ disabled: (page + 1) * size >= total }">
          <button class="page-link" @click="page++; load()">下一頁</button>
        </li>
      </ul>
    </nav>
  </div>
</template>
