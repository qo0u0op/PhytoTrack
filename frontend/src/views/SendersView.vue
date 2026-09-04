<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Swal from 'sweetalert2'
import { senderApi, refApi } from '../api'
import { useAuthStore } from '../stores/auth'

const route = useRoute ()
const router = useRouter ()
const auth = useAuthStore ()

interface SenderRow {
  senderId: number
  name: string | null
  displayName: string | null
  phone: string | null
  address: string
  districtId?: number
  districtName: string
  cityName: string
  senderTypeId?: number
  senderTypeName: string
}

const senders = ref<SenderRow[]>([])
const loading = ref (true)
const searchQ = ref ('')
const filterSenderTypeId = ref<number | undefined>(undefined)
const filterCityId = ref<number | undefined>(undefined)
const filterDistrictId = ref<number | undefined>(undefined)
const showFilter = ref (false)
// 實際套用的篩選條件（按「篩選」後才更新）
const appliedQ = ref ('')
const appliedSenderTypeId = ref<number | undefined>(undefined)
const appliedCityId = ref<number | undefined>(undefined)
const appliedDistrictId = ref<number | undefined>(undefined)
const cities = ref<{ id: number; name: string; districts: { id: number; name: string }[] }[]>([])
const senderTypes = ref<{ id: number; name: string }[]>([])

function displayLabel (s: SenderRow) {
  const hasName = s.name && s.name.trim ()
  const hasDisplay = s.displayName && s.displayName.trim ()
  if (hasName && hasDisplay) return `${s.name} (${s.displayName})`
  if (hasDisplay) return s.displayName!
  if (hasName) return s.name!
  return s.phone ?? ''
}

async function load () {
  loading.value = true
  try {
    const { data } = await senderApi.list ()
    senders.value = data as SenderRow[]
  } catch {
    // 由攔截器處理
  } finally {
    loading.value = false
  }
}

// 篩選：四欄 AND，前端本地過濾
const filteredDistricts = computed (() => {
  if (!filterCityId.value) return []
  const city = cities.value.find ((c) => c.id === filterCityId.value)
  return city ? city.districts : []
})

watch (filterCityId, () => {
  filterDistrictId.value = undefined
})

const filteredSenders = computed (() => {
  const q = appliedQ.value.trim ().toLowerCase ()
  const senderTypeId = appliedSenderTypeId.value
  const cityId = appliedCityId.value
  const districtId = appliedDistrictId.value
  let cityName: string | undefined
  let districtName: string | undefined
  if (cityId) {
    const city = cities.value.find ((c) => c.id === cityId)
    cityName = city?.name
  }
  if (districtId) {
    const city = cities.value.find ((c) => c.id === cityId)
    districtName = city?.districts.find ((d) => d.id === districtId)?.name
    // 若僅選鄉鎮未選縣市，仍需找對應 district 名稱
    if (!districtName) {
      for (const c of cities.value) {
        const d = c.districts.find ((x) => x.id === districtId)
        if (d) { districtName = d.name; break }
      }
    }
  }
  return senders.value.filter ((s) => {
    if (q) {
      const hay = `${s.name ?? ''} ${s.displayName ?? ''} ${s.phone ?? ''}`.toLowerCase ()
      if (!hay.includes (q)) return false
    }
    if (senderTypeId && s.senderTypeId !== senderTypeId) return false
    if (cityName && s.cityName !== cityName) return false
    if (districtName && s.districtName !== districtName) return false
    return true
  })
})

// 排序（除操作外）
const sortStates = ref<Array<{ key: string; order: 'asc' | 'desc' }>>([{ key: 'senderId', order: 'desc' }])
function sortBy (key: string) {
  const idx = sortStates.value.findIndex ((s) => s.key === key)
  if (idx >= 0) {
    const cur = sortStates.value[idx]
    if (cur.order === 'asc') sortStates.value[idx].order = 'desc'
    else sortStates.value.splice (idx, 1)
  } else {
    sortStates.value.push ({ key, order: 'asc' })
  }
}
function sortIcon (key: string) {
  const idx = sortStates.value.findIndex ((s) => s.key === key)
  if (idx < 0) return '↕'
  const order = sortStates.value[idx].order
  const num = `${idx + 1}`
  return order === 'asc' ? `↑${num}` : `↓${num}`
}
const sortedSenders = computed (() => {
  if (sortStates.value.length === 0) return filteredSenders.value
  return [...filteredSenders.value].sort ((a, b) => {
    for (const { key, order } of sortStates.value) {
      let av: any = (a as any)[key]
      let bv: any = (b as any)[key]
      if (key === 'senderLabel') { av = displayLabel (a); bv = displayLabel (b) }
      if (av == null) av = ''
      if (bv == null) bv = ''
      let cmp = 0
      if (typeof av === 'number' && typeof bv === 'number') cmp = av - bv
      else cmp = String (av).localeCompare (String (bv))
      if (cmp !== 0) return order === 'asc' ? cmp : -cmp
    }
    return 0
  })
})

function applyFilters () {
  appliedQ.value = searchQ.value
  appliedSenderTypeId.value = filterSenderTypeId.value
  appliedCityId.value = filterCityId.value
  appliedDistrictId.value = filterDistrictId.value
}

function clearFilters () {
  searchQ.value = ''
  filterSenderTypeId.value = undefined
  filterCityId.value = undefined
  filterDistrictId.value = undefined
  appliedQ.value = ''
  appliedSenderTypeId.value = undefined
  appliedCityId.value = undefined
  appliedDistrictId.value = undefined
  page.value = 0
  pageInput.value = 1
}

// 分頁（與 CasesView 同款，>20 才顯示）
const page = ref (0)
const size = ref (10)
const sizeOptions = [10, 20, 50, 100]
const pageInput = ref (1)
const total = computed (() => filteredSenders.value.length)
const totalPages = computed (() => Math.max (1, Math.ceil (total.value / size.value)))

watch (page, (v) => { pageInput.value = v + 1 })

function goToPage (p: number) {
  const clamped = Math.max (0, Math.min (p, totalPages.value - 1))
  if (clamped !== page.value) {
    page.value = clamped
    pageInput.value = clamped + 1
  } else {
    pageInput.value = clamped + 1
  }
}

function onSizeChange () {
  page.value = 0
  pageInput.value = 1
}

function onPageInputConfirm () {
  let num = Number (pageInput.value)
  if (!Number.isFinite (num) || num < 1) num = 1
  if (num > totalPages.value) num = totalPages.value
  goToPage (num - 1)
}

watch ([appliedQ, appliedSenderTypeId, appliedCityId, appliedDistrictId], () => {
  page.value = 0
  pageInput.value = 1
})

const pagedSenders = computed (() => {
  const start = page.value * size.value
  return sortedSenders.value.slice (start, start + size.value)
})

watch (sortStates, () => {
  page.value = 0
  pageInput.value = 1
}, { deep: true })

onMounted (async () => {
  await Promise.all ([load (), loadRefs ()])
  // 從 query 恢復篩選/分頁/排序（供編輯頁返回保持狀態）
  const q = route.query
  if (q.q !== undefined || q.senderTypeId !== undefined || q.cityId !== undefined || q.districtId !== undefined || q.page !== undefined || q.size !== undefined || q.sort !== undefined) {
    const qs = q.q as string | undefined
    const stId = q.senderTypeId ? Number (q.senderTypeId) : undefined
    const cId = q.cityId ? Number (q.cityId) : undefined
    const dId = q.districtId ? Number (q.districtId) : undefined
    const p = q.page ? Number (q.page) : 0
    const s = q.size ? Number (q.size) : undefined
    const sortStr = q.sort as string | undefined
    searchQ.value = qs ?? ''
    filterSenderTypeId.value = stId
    filterCityId.value = cId
    filterDistrictId.value = dId
    appliedQ.value = qs ?? ''
    appliedSenderTypeId.value = stId
    appliedCityId.value = cId
    appliedDistrictId.value = dId
    if (sortStr) {
      const parsed = sortStr.split (';').map ((part) => {
        const [key, order] = part.split (',')
        return { key, order: (order === 'asc' ? 'asc' : 'desc') as 'asc' | 'desc' }
      }).filter ((x) => x.key)
      if (parsed.length > 0) sortStates.value = parsed
    }
    if (s && sizeOptions.includes (s)) size.value = s
    // page 需在篩選/排序設定後再設，避免被 watch 重置
    setTimeout (() => {
      page.value = Math.max (0, Math.min (p, totalPages.value - 1))
      pageInput.value = page.value + 1
    }, 0)
  }
})

async function loadRefs () {
  try {
    const [cityRes, typeRes] = await Promise.all ([refApi.cities (), refApi.senderTypes ()])
    cities.value = cityRes.data
    senderTypes.value = typeRes.data
  } catch {}
}

async function handleDelete (id: number, label: string) {
  const result = await Swal.fire ({
    icon: 'warning',
    title: `確定刪除「${label}」？`,
    text: '此操作無法復原，若已被案件引用將被拒絕',
    showCancelButton: true,
    confirmButtonText: '刪除',
    cancelButtonText: '取消',
  })
  if (!result.isConfirmed) return
  try {
    await senderApi.remove (id)
    Swal.fire ({ icon: 'success', title: '已刪除', timer: 1200, showConfirmButton: false })
    await load ()
  } catch {}
}

async function handleEdit (s: SenderRow) {
  const query: Record<string, string> = {}
  if (appliedQ.value) query.q = appliedQ.value
  if (appliedSenderTypeId.value) query.senderTypeId = String (appliedSenderTypeId.value)
  if (appliedCityId.value) query.cityId = String (appliedCityId.value)
  if (appliedDistrictId.value) query.districtId = String (appliedDistrictId.value)
  query.page = String (page.value)
  query.size = String (size.value)
  if (sortStates.value.length > 0) query.sort = sortStates.value.map ((s) => `${s.key},${s.order}`).join (';')
  // 攜帶當前篩選排序後的 ID 序列，供編輯頁上一筆/下一筆導航
  const ids = sortedSenders.value.map ((x) => String (x.senderId)).join (',')
  if (ids) query.ids = ids
  router.push ({ name: 'sender-edit', params: { id: s.senderId }, query })
}
</script>

<template>
  <div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h4 class="mb-0">送件人管理</h4>
      <button class="btn btn-outline-primary btn-sm" :aria-expanded="showFilter" aria-controls="senderFilterCard" @click="showFilter = !showFilter">篩選</button>
    </div>
    <div v-show="showFilter" id="senderFilterCard" class="card shadow-sm mb-3">
      <div class="card-body">
        <div class="row g-2 align-items-end">
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">關鍵字 (姓名/電話/顯示名稱)</label>
            <input v-model="searchQ" type="text" class="form-control form-control-sm" placeholder="輸入關鍵字" />
          </div>
          <div class="col-md-2">
            <label class="form-label small text-muted mb-1">身分別</label>
            <select v-model="filterSenderTypeId" class="form-select form-select-sm">
              <option :value="undefined">全部</option>
              <option v-for="t in senderTypes" :key="t.id" :value="t.id">{{ t.name }}</option>
            </select>
          </div>
          <div class="col-md-2">
            <label class="form-label small text-muted mb-1">縣市</label>
            <select v-model="filterCityId" class="form-select form-select-sm">
              <option :value="undefined">全部</option>
              <option v-for="c in cities" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div class="col-md-2">
            <label class="form-label small text-muted mb-1">鄉鎮市區</label>
            <select v-model="filterDistrictId" class="form-select form-select-sm" :disabled="!filterCityId">
              <option :value="undefined">全部</option>
              <option v-for="d in filteredDistricts" :key="d.id" :value="d.id">{{ d.name }}</option>
            </select>
          </div>
          <div class="col-md-3">
            <button class="btn btn-sm btn-primary me-1" @click="applyFilters">篩選</button>
            <button class="btn btn-sm btn-outline-secondary border-0 text-secondary" @click="clearFilters">清除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分頁（篩選卡下方，>20 才顯示） -->
    <div v-if="total > 20" class="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3 mb-3">
      <div class="d-flex align-items-center gap-2">
        <label class="form-label small text-muted mb-0 text-nowrap">每頁筆數</label>
        <select v-model.number="size" class="form-select form-select-sm" style="width:auto" @change="onSizeChange">
          <option v-for="opt in sizeOptions" :key="opt" :value="opt">{{ opt }} 筆/頁</option>
        </select>
        <span class="small text-muted text-nowrap">共 {{ total }} 筆，{{ totalPages }} 頁</span>
      </div>
      <nav aria-label="送件人分頁">
        <ul class="pagination pagination-sm mb-0 justify-content-center">
          <li class="page-item" :class="{ disabled: page === 0 }"><button class="page-link border-0 text-secondary" :disabled="page === 0" @click="goToPage(0)" style="height:31px"><<</button></li>
          <li class="page-item" :class="{ disabled: page === 0 }"><button class="page-link border-0 text-secondary" :disabled="page === 0" @click="goToPage(page - 1)" style="height:31px"><</button></li>
          <li class="page-item"><span class="page-link border-0 text-secondary d-flex align-items-center gap-1" style="height:31px;padding:0 0.5rem"><input v-model.number="pageInput" type="number" class="form-control form-control-sm text-center p-0" style="width:64px;height:24px;font-size:0.875rem;line-height:1.5" :min="1" :max="totalPages" @keyup.enter="onPageInputConfirm" @blur="onPageInputConfirm" /> / {{ totalPages }}</span></li>
          <li class="page-item" :class="{ disabled: page >= totalPages - 1 }"><button class="page-link border-0 text-secondary" :disabled="page >= totalPages - 1" @click="goToPage(page + 1)" style="height:31px">></button></li>
          <li class="page-item" :class="{ disabled: page >= totalPages - 1 }"><button class="page-link border-0 text-secondary" :disabled="page >= totalPages - 1" @click="goToPage(totalPages - 1)" style="height:31px">>></button></li>
        </ul>
      </nav>
    </div>

    <div class="card shadow-sm">
      <div class="table-responsive position-relative" style="overflow-x:auto;-webkit-overflow-scrolling:touch">
        <table class="table table-hover align-middle mb-0 text-nowrap" style="min-width:950px;table-layout:fixed">
          <thead class="table-light">
            <tr>
              <th style="width:60px;min-width:60px;cursor:pointer" @click="sortBy('senderId')">ID {{ sortIcon('senderId') }}</th>
              <th style="width:140px;min-width:140px;cursor:pointer" @click="sortBy('senderLabel')">送件人 {{ sortIcon('senderLabel') }}</th>
              <th style="width:110px;min-width:110px;cursor:pointer" @click="sortBy('phone')">電話 {{ sortIcon('phone') }}</th>
              <th style="width:100px;min-width:100px;cursor:pointer" @click="sortBy('senderTypeName')">身分別 {{ sortIcon('senderTypeName') }}</th>
              <th style="width:80px;min-width:80px;cursor:pointer" @click="sortBy('cityName')">縣市 {{ sortIcon('cityName') }}</th>
              <th style="width:100px;min-width:100px;cursor:pointer" @click="sortBy('districtName')">鄉鎮市區 {{ sortIcon('districtName') }}</th>
              <th style="width:150px;min-width:150px;cursor:pointer" @click="sortBy('address')">地址 {{ sortIcon('address') }}</th>
              <th class="text-end" style="width:130px;min-width:130px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="text-center text-muted py-4">載入中…</td>
            </tr>
            <tr v-else-if="filteredSenders.length === 0">
              <td colspan="8" class="text-center text-muted py-4">尚無資料</td>
            </tr>
            <tr v-for="s in pagedSenders" :key="s.senderId">
              <td>{{ s.senderId }}</td>
              <td class="text-truncate" style="max-width:140px" :title="displayLabel(s)">{{ displayLabel(s) }}</td>
              <td class="text-truncate" style="max-width:110px" :title="s.phone ?? '—'">{{ s.phone ?? '—' }}</td>
              <td class="text-truncate" style="max-width:100px" :title="s.senderTypeName">{{ s.senderTypeName }}</td>
              <td class="text-truncate" style="max-width:80px" :title="s.cityName">{{ s.cityName }}</td>
              <td class="text-truncate" style="max-width:100px" :title="s.districtName">{{ s.districtName }}</td>
              <td class="text-truncate" style="max-width:150px" :title="s.address">{{ s.address }}</td>
              <td class="text-end">
                <button v-if="auth.isStaff" class="btn btn-sm btn-outline-primary me-1" @click="handleEdit (s)">編輯</button>
                <button v-if="auth.isAdmin" class="btn btn-sm btn-outline-danger" @click="handleDelete (s.senderId, displayLabel (s))">刪除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 分頁（表格下方，>20 才顯示） -->
    <div v-if="total > 20" class="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3">
      <div class="d-flex align-items-center gap-2">
        <label class="form-label small text-muted mb-0 text-nowrap">每頁筆數</label>
        <select v-model.number="size" class="form-select form-select-sm" style="width:auto" @change="onSizeChange">
          <option v-for="opt in sizeOptions" :key="opt" :value="opt">{{ opt }} 筆/頁</option>
        </select>
        <span class="small text-muted text-nowrap">共 {{ total }} 筆，{{ totalPages }} 頁</span>
      </div>
      <nav aria-label="送件人分頁">
        <ul class="pagination pagination-sm mb-0 justify-content-center">
          <li class="page-item" :class="{ disabled: page === 0 }"><button class="page-link border-0 text-secondary" :disabled="page === 0" @click="goToPage(0)" style="height:31px"><<</button></li>
          <li class="page-item" :class="{ disabled: page === 0 }"><button class="page-link border-0 text-secondary" :disabled="page === 0" @click="goToPage(page - 1)" style="height:31px"><</button></li>
          <li class="page-item"><span class="page-link border-0 text-secondary d-flex align-items-center gap-1" style="height:31px;padding:0 0.5rem"><input v-model.number="pageInput" type="number" class="form-control form-control-sm text-center p-0" style="width:64px;height:24px;font-size:0.875rem;line-height:1.5" :min="1" :max="totalPages" @keyup.enter="onPageInputConfirm" @blur="onPageInputConfirm" /> / {{ totalPages }}</span></li>
          <li class="page-item" :class="{ disabled: page >= totalPages - 1 }"><button class="page-link border-0 text-secondary" :disabled="page >= totalPages - 1" @click="goToPage(page + 1)" style="height:31px">></button></li>
          <li class="page-item" :class="{ disabled: page >= totalPages - 1 }"><button class="page-link border-0 text-secondary" :disabled="page >= totalPages - 1" @click="goToPage(totalPages - 1)" style="height:31px">>></button></li>
        </ul>
      </nav>
    </div>
  </div>
</template>
