<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Swal from 'sweetalert2'
import { senderApi, refApi } from '../api'
import { useAuthStore } from '../stores/auth'

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
  if (hasName && hasDisplay) return `${s.name}(${s.displayName})`
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
  return filteredSenders.value.slice (start, start + size.value)
})

onMounted (async () => {
  await Promise.all ([load (), loadRefs ()])
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
  // 先取完整資料以補齊 districtId / senderTypeId
  let detailData: any = s
  try {
    const { data } = await senderApi.detail (s.senderId)
    detailData = data
  } catch {}
  // 計算當前縣市 id (由 district 反查)
  const currentCity = cities.value.find ((c) => c.districts.some ((d) => d.id === detailData.districtId))
  const currentCityId = currentCity?.id ?? cities.value[0]?.id ?? 1
  const districtOptionsForCity = (cityId: number) => {
    const city = cities.value.find ((c) => c.id === cityId)
    return city ? city.districts : []
  }
  // 預設縣市/鄉鎮選項
  const cityOptionsHtml = cities.value.map ((c) => `<option value="${c.id}" ${c.id === currentCityId ? 'selected' : ''}>${c.name}</option>`).join ('')
  const districtOptionsHtml = districtOptionsForCity (currentCityId).map ((d) => `<option value="${d.id}" ${d.id === detailData.districtId ? 'selected' : ''}>${d.name}</option>`).join ('')
  const typeOptionsHtml = senderTypes.value.map ((t) => `<option value="${t.id}" ${t.id === detailData.senderTypeId ? 'selected' : ''}>${t.name}</option>`).join ('')
  const { value: form } = await Swal.fire ({
    title: `編輯送件人 #${s.senderId}`,
    html: `
      <input id="swal-sender-name" class="swal2-input" placeholder="姓名" value="${(detailData.name ?? '').replace (/"/g, '&quot;')}" />
      <input id="swal-sender-displayName" class="swal2-input" placeholder="顯示名稱" value="${(detailData.displayName ?? '').replace (/"/g, '&quot;')}" />
      <input id="swal-sender-phone" class="swal2-input" placeholder="電話" value="${(detailData.phone ?? '').replace (/"/g, '&quot;')}" />
      <input id="swal-sender-address" class="swal2-input" placeholder="地址" value="${(detailData.address ?? '').replace (/"/g, '&quot;')}" />
      <select id="swal-sender-city" class="swal2-select"><option value="">請選擇縣市</option>${cityOptionsHtml}</select>
      <select id="swal-sender-district" class="swal2-select">${districtOptionsHtml}</select>
      <select id="swal-sender-type" class="swal2-select">${typeOptionsHtml}</select>
    `,
    didOpen: () => {
      const cityEl = document.getElementById ('swal-sender-city') as HTMLSelectElement
      const districtEl = document.getElementById ('swal-sender-district') as HTMLSelectElement
      if (cityEl && districtEl) {
        cityEl.addEventListener ('change', () => {
          const cid = Number (cityEl.value)
          const city = cities.value.find ((c) => c.id === cid)
          districtEl.innerHTML = city ? city.districts.map ((d) => `<option value="${d.id}">${d.name}</option>`).join ('') : ''
        })
      }
    },
    showCancelButton: true,
    confirmButtonText: '儲存',
    cancelButtonText: '取消',
    preConfirm: () => {
      const name = (document.getElementById ('swal-sender-name') as HTMLInputElement).value.trim ()
      const displayName = (document.getElementById ('swal-sender-displayName') as HTMLInputElement).value.trim ()
      const phone = (document.getElementById ('swal-sender-phone') as HTMLInputElement).value.trim ()
      const address = (document.getElementById ('swal-sender-address') as HTMLInputElement).value.trim ()
      const districtId = Number ((document.getElementById ('swal-sender-district') as HTMLSelectElement).value)
      const senderTypeId = Number ((document.getElementById ('swal-sender-type') as HTMLSelectElement).value)
      if (!phone && !displayName) return Swal.showValidationMessage ('電話與顯示名稱至少需提供一項')
      if (!address) return Swal.showValidationMessage ('地址不可為空白')
      if (!districtId) return Swal.showValidationMessage ('請選擇鄉鎮市區')
      if (!senderTypeId) return Swal.showValidationMessage ('請選擇身分別')
      return { name: name || undefined, displayName: displayName || undefined, phone: phone || undefined, address, districtId, senderTypeId }
    },
  })
  if (!form) return
  try {
    await senderApi.update (s.senderId, {
      name: form.name,
      displayName: form.displayName,
      phone: form.phone,
      address: form.address,
      districtId: form.districtId,
      senderTypeId: form.senderTypeId,
    } as any)
    Swal.fire ({ icon: 'success', title: '已更新', timer: 1200, showConfirmButton: false })
    await load ()
  } catch {}
}
</script>

<template>
  <div class="container py-4">
    <h4 class="mb-4">送件人管理</h4>
    <div class="card shadow-sm mb-3">
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
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>ID</th>
              <th>送件人</th>
              <th>電話</th>
              <th>身分別</th>
              <th>縣市</th>
              <th>鄉鎮市區</th>
              <th>地址</th>
              <th class="text-end">操作</th>
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
              <td>{{ displayLabel (s) }}</td>
              <td>{{ s.phone ?? '—' }}</td>
              <td>{{ s.senderTypeName }}</td>
              <td>{{ s.cityName }}</td>
              <td>{{ s.districtName }}</td>
              <td>{{ s.address }}</td>
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
