<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import Swal from 'sweetalert2'
import { caseApi, refApi } from '../api'
import { useAuthStore } from '../stores/auth'
import type { components } from '../types/api'
import { STATUS_OPTIONS, statusBadgeClass, statusLabel } from '../utils/caseStatus'
import { escapeHtml } from '../utils/escapeHtml'

// 分頁資料型別 (對應後端 Page<CaseSummaryResponse>)
interface CaseSummary {
  caseId: number
  receiveDate: string
  cropName: string
  senderName: string | null
  senderDisplayName: string | null
  senderPhone: string | null
  senderAddress: string | null
  senderDistrictName: string | null
  senderCityName: string | null
  serviceName: string
  deliveryName?: string | null
  status: string
  createdAt: string
  pestCategoryCount?: number
  pestCategoryNames?: string | null
  pestTypeNames?: string | null
}

function senderLabel (c: CaseSummary) {
  // VIEWER 時後端***為 null，顯示***(***) 與預覽一致
  if (auth.isViewer) {
    return '***(***)'
  }
  const name = c.senderName
  const display = c.senderDisplayName
  const hasName = name && name.trim ()
  const hasDisplay = display && display.trim ()
  if (hasName && hasDisplay) return `${name} (${display})`
  if (hasDisplay) return display!
  if (hasName) return name!
  return c.senderPhone ?? '—'
}

function pestLabel (c: CaseSummary) {
  const names = c.pestTypeNames as string | null
  if (!names) return '—'
  return names
}

function isComposite (c: CaseSummary) {
  const names = c.pestTypeNames as string | null
  return names ? names.split ('、').length > 1 : false
}

// 篩選條件 (對應後端 GET /api/cases 查詢參數，經 v_case_search)
interface CaseFilters {
  cropId?: number
  serviceId?: number
  senderName: string
  senderQuery?: string
  senderTypeId?: number
  methodId?: number
  receiveDateFrom: string
  receiveDateTo: string
  status: string
  cityId?: number
  districtId?: number
  cropCategoryId?: number
  pestTypeId?: number
  pestCategoryId?: number
  hintId?: number
  deliveryId?: number
  damageId?: number
}

const auth = useAuthStore ()
const router = useRouter ()

const allCases = ref<CaseSummary[]>([])
const total = ref (0)
const page = ref (0)
const size = ref (10)
const sizeOptions = [10, 20, 50, 100]
const pageInput = ref (1)
const totalPages = computed (() => Math.max (1, Math.ceil (total.value / size.value)))
const loading = ref (false)

// 排序（前端本地多欄，依點擊順序，循環 asc→desc→無）
const sortStates = ref<Array<{ key: string; order: 'asc' | 'desc' }>>([{ key: 'receiveDate', order: 'desc' }])
function sortBy (key: string) {
  const idx = sortStates.value.findIndex ((s) => s.key === key)
  if (idx >= 0) {
    const cur = sortStates.value[idx]
    if (cur.order === 'asc') {
      sortStates.value[idx].order = 'desc'
    } else {
      sortStates.value.splice (idx, 1)
    }
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
watch (sortStates, () => {
  page.value = 0
  pageInput.value = 1
}, { deep: true })
const sortedCases = computed (() => {
  if (sortStates.value.length === 0) return allCases.value
  return [...allCases.value].sort ((a, b) => {
    for (const { key, order } of sortStates.value) {
      let av: any = (a as any)[key]
      let bv: any = (b as any)[key]
      if (key === 'senderLabel') { av = senderLabel (a); bv = senderLabel (b) }
      if (key === 'pestLabel') { av = pestLabel (a) ?? ''; bv = pestLabel (b) ?? '' }
      if (av == null) av = ''
      if (bv == null) bv = ''
      if (typeof av === 'number' && typeof bv === 'number') {
        if (av < bv) return order === 'asc' ? -1 : 1
        if (av > bv) return order === 'asc' ? 1 : -1
      } else {
        const cmp = String (av).localeCompare (String (bv))
        if (cmp !== 0) return order === 'asc' ? cmp : -cmp
      }
    }
    return 0
  })
})
const pagedCases = computed (() => {
  const start = page.value * size.value
  return sortedCases.value.slice (start, start + size.value)
})

watch (page, (v) => { pageInput.value = v + 1 })

function goToPage (p: number) {
  const clamped = Math.max (0, Math.min (p, totalPages.value - 1))
  if (clamped !== page.value) {
    page.value = clamped
    pageInput.value = clamped + 1
    load ()
  } else {
    pageInput.value = clamped + 1
  }
}

function onSizeChange () {
  page.value = 0
  pageInput.value = 1
  load ()
}

function onPageInputConfirm () {
  let num = Number (pageInput.value)
  if (!Number.isFinite (num) || num < 1) num = 1
  if (num > totalPages.value) num = totalPages.value
  goToPage (num - 1)
}

// 篩選抽屜
const showFilter = ref (false)

// 篩選工具列狀態與選單資料
const filters = reactive<CaseFilters>({
  senderName: '',
  receiveDateFrom: '',
  receiveDateTo: '',
  status: '',
})
const cropOptions = ref<{ id?: number; name?: string }[]>([])
const serviceOptions = ref<{ id?: number; name?: string }[]>([])
const cityOptions = ref<{ id: number; name: string; districts: { id: number; name: string }[] }[]>([])
const cropCategoryOptions = ref<{ id: number; name: string }[]>([])
const pestTypeOptions = ref<{ id: number; name: string; categories: { id: number; name: string }[] }[]>([])
const hintOptions = ref<{ id: number; name: string }[]>([])
const deliveryOptions = ref<{ id: number; name: string }[]>([])
const damageOptions = ref<{ id: number; name: string }[]>([])
const senderTypeOptions = ref<{ id?: number; name?: string }[]>([])
const methodOptions = ref<{ id?: number; name?: string }[]>([])
const allCropCategories = ref<components['schemas']['CropCategoryResponse'][]>([])

const filteredDistricts = computed (() => {
  if (!filters.cityId) return []
  const city = cityOptions.value.find ((c) => c.id === filters.cityId)
  return city ? city.districts : []
})
const filteredPestCategories = computed (() => {
  if (!filters.pestTypeId) return pestTypeOptions.value.flatMap ((p) => p.categories)
  const pt = pestTypeOptions.value.find ((p) => p.id === filters.pestTypeId)
  return pt ? pt.categories : []
})
const filteredCrops = computed (() => {
  if (!filters.cropCategoryId) return cropOptions.value
  const cat = allCropCategories.value.find ((c) => c.id === filters.cropCategoryId)
  return (cat?.crops ?? []) as { id?: number; name?: string }[]
})
watch (() => filters.cityId, (newCityId) => {
  if (!newCityId) {
    filters.districtId = undefined
    return
  }
  const districts = cityOptions.value.find ((c) => c.id === newCityId)?.districts ?? []
  if (districts.length > 0 && !districts.some ((d) => d.id === filters.districtId)) {
    filters.districtId = undefined
  }
})
watch (() => filters.pestTypeId, (newTypeId) => {
  if (!newTypeId) return
  const cats = pestTypeOptions.value.find ((p) => p.id === newTypeId)?.categories ?? []
  if (cats.length > 0 && filters.pestCategoryId && !cats.some ((c) => c.id === filters.pestCategoryId)) {
    filters.pestCategoryId = undefined
  }
})
watch (() => filters.cropCategoryId, (newCatId) => {
  if (!newCatId) return
  const crops = allCropCategories.value.find ((c) => c.id === newCatId)?.crops ?? []
  if (crops.length > 0 && filters.cropId && !crops.some ((c) => c.id === filters.cropId)) {
    filters.cropId = undefined
  }
})

async function load () {
  loading.value = true
  try {
    const params: Record<string, string | number> = {}
    if (filters.receiveDateFrom) params.receiveDateFrom = filters.receiveDateFrom
    if (filters.receiveDateTo) params.receiveDateTo = filters.receiveDateTo
    if (filters.status) params.status = filters.status
    if (filters.cityId) params.cityId = filters.cityId
    if (filters.districtId) params.districtId = filters.districtId
    if (!auth.isViewer && filters.senderName.trim ()) params.senderQuery = filters.senderName.trim ()
    if (filters.senderTypeId) params.senderTypeId = filters.senderTypeId
    if (filters.serviceId) params.serviceId = filters.serviceId
    if (filters.deliveryId) params.deliveryId = filters.deliveryId
    if (filters.methodId) params.methodId = filters.methodId
    if (filters.cropCategoryId) params.cropCategoryId = filters.cropCategoryId
    if (filters.cropId) params.cropId = filters.cropId
    if (filters.damageId) params.damageId = filters.damageId
    if (filters.pestTypeId) params.pestTypeId = filters.pestTypeId
    if (filters.pestCategoryId) params.pestCategoryId = filters.pestCategoryId
    if (filters.hintId) params.hintId = filters.hintId
    // 前端全域排序與分頁：一次取回所有篩選結果（避免後端 sort 500 與分頁影響排序）
    const fetchParams = { ...params, page: 0, size: 10000 }
    const { data } = await caseApi.list (fetchParams as unknown as Record<string, string | number>)
    allCases.value = data.content
    total.value = data.totalElements
    // 若篩選後總頁數縮小導致當前頁越界，自動回到>>
    if (total.value > 0 && page.value >= totalPages.value) {
      page.value = totalPages.value - 1
      pageInput.value = page.value + 1
      const retryParams = { ...params, page: page.value }
      const { data: retryData } = await caseApi.list (retryParams as unknown as Record<string, string | number>)
      allCases.value = retryData.content
      total.value = retryData.totalElements
      // total will be updated via computed, but keep for pagination
    }
  } catch {
    // 錯誤由攔截器處理
  } finally {
    loading.value = false
  }
}

// 載入篩選下拉選單 (視圖多欄所需參照)
async function loadFilterOptions () {
  try {
    const [cropRes, serviceRes, cityRes, pestRes, hintRes, deliveryRes, damageRes, senderTypeRes, methodRes] = await Promise.all ([
      refApi.cropCategories (),
      refApi.services (),
      refApi.cities (),
      refApi.pestTypes (),
      refApi.hints (),
      refApi.deliveries (),
      refApi.damages (),
      refApi.senderTypes (),
      refApi.methods (),
    ])
    allCropCategories.value = cropRes.data as any
    cropOptions.value = (cropRes.data as components['schemas']['CropCategoryResponse'][]).flatMap ((cat) => cat.crops ?? [],)
    serviceOptions.value = serviceRes.data as components['schemas']['IdNameResponse'][]
    cityOptions.value = cityRes.data as any
    cropCategoryOptions.value = (cropRes.data as any).map ((c: any) => ({ id: c.id, name: c.name }))
    pestTypeOptions.value = pestRes.data as any
    hintOptions.value = hintRes.data as any
    deliveryOptions.value = deliveryRes.data as any
    damageOptions.value = damageRes.data as any
    senderTypeOptions.value = senderTypeRes.data as any
    methodOptions.value = methodRes.data as any
  } catch {
    // 錯誤由攔截器處理
  }
}

function applyFilters () {
  page.value = 0
  load ()
}

function clearFilters () {
  filters.receiveDateFrom = ''
  filters.receiveDateTo = ''
  filters.status = ''
  filters.cityId = undefined
  filters.districtId = undefined
  filters.senderName = ''
  filters.senderTypeId = undefined
  filters.serviceId = undefined
  filters.deliveryId = undefined
  filters.methodId = undefined
  filters.cropCategoryId = undefined
  filters.cropId = undefined
  filters.damageId = undefined
  filters.pestTypeId = undefined
  filters.pestCategoryId = undefined
  filters.hintId = undefined
  page.value = 0
  load ()
}

onMounted (() => {
  load ()
  loadFilterOptions ()
})

// 預覽案件詳細：彈窗快速瀏覽，可進一步跳轉明細頁 (列印診斷單)
async function viewDetail (id: number) {
  let data: components['schemas']['CaseResponse']
  try {
    const res = await caseApi.detail (id)
    data = res.data
  } catch {
    return // 錯誤由攔截器處理 (如案件不存在)
  }
  // 彈窗內容以 HTML 插入，所有動態內文必須轉義 (防 XSS)
  const esc = (v?: string | null) => escapeHtml (v ?? '')
  const join = (items?: { id?: number; name?: string }[]) =>
    items?.map ((i) => esc (i.name)).join ('、') ?? '無'

  // 送件人顯示：支援 name (displayName) 與 VIEWER 遮蔽
  const hasName = data.senderName && String (data.senderName).trim ()
  const hasDisplay = data.senderDisplayName && String (data.senderDisplayName).trim ()
  let senderLabel = ''
  if (hasName && hasDisplay) senderLabel = `${data.senderName} (${data.senderDisplayName})`
  else if (hasDisplay) senderLabel = data.senderDisplayName!
  else if (hasName) senderLabel = data.senderName!
  else senderLabel = data.senderPhone ?? '—'
  // VIEWER 時後端***，僅保留縣市鄉鎮
  const isViewer = auth.isViewer
  const displaySender = isViewer ? '***' : esc (senderLabel)

  // 輔助：hints 更名 其他→其他回覆 (Q4)
  const joinHints = (items?: { id?: number; name?: string }[]) =>
    items?.map ((i) => esc (i.name === '其他' ? '其他回覆' : i.name)).join ('、') ?? '無'
  const fieldCity = data.fieldCityName ?? ''
  const fieldDistrict = data.fieldDistrictName ?? ''
  const fieldSame = data.fieldDistrictId && data.fieldDistrictId === data.senderDistrictId
  const cropCategory = data.cropCategoryName ?? '—'
  const senderTypeName = data.senderTypeName ?? '—'
  const pestPreviewHtml = (items?: { id?: number; name?: string; pestNote?: string | null; pestTypeName?: string | null }[]) => {
    if (!items || items.length === 0) return '<p class="ms-3">無</p>'
    return items.map (p => `<p class="ms-3">• ${esc (p.pestTypeName ?? '')}-${esc (p.name ?? '')}</p>`).join ('')
  }

  const result = await Swal.fire ({
    title: `案件 #${data.caseId} 預覽`,
    width: 640,
    html: `
      <div class="text-start small">
        <p><strong>收件日期：</strong>${esc (data.receiveDate)} <strong class="ms-3">收件編號：</strong>#${data.caseId}</p>
        <p><strong>田區位置：</strong>${esc (fieldCity)}${esc (fieldDistrict)}${fieldSame ? ' (同寄件人)' : ''}</p>
        <p><strong>身分別：</strong>${esc (senderTypeName)}</p>
        <p><strong>送件人：</strong>${displaySender}</p>
        <hr />

        <p><strong>服務類別：</strong>${esc (data.serviceName)} <strong class="ms-3">送件方式：</strong>${esc (data.deliveryName)}</p>
        <p><strong>耕種方式：</strong>${esc (data.methodName)}</p>
        <p><strong>作物類別：</strong>${esc (cropCategory)} <strong class="ms-3">作物：</strong>${esc (data.cropName)}</p>
        <p><strong>被害部位：</strong>${join (data.damages)}</p>
        <p><strong>栽培面積：</strong>${esc (data.cropScale ?? '無')} <strong class="ms-3">被害面積：</strong>${esc (data.damageScale ?? '無')}</p>

        <hr />
        <p><strong>鑑定者：</strong>${join (data.identifiers)}</p>
        <p><strong>診斷結果：</strong></p>
        ${pestPreviewHtml (data.pestCategories)}
        <p><strong>防治建議：</strong>${joinHints (data.hints)}</p>
        <hr />

        <p class="text-muted">建立者：${esc (data.createdByName)}／建立時間：${esc (data.createdAt)}</p>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: '檢視',
    cancelButtonText: '關閉',
  })
  if (result.isConfirmed) {
    router.push (`/cases/${id}`)
  }
}

// 匯出 CSV：依目前篩選條件下載 (blob 避免觸發瀏覽器跳頁)，與列表篩選同參
async function exportCsv () {
  try {
    const params: Record<string, string | number> = {}
    if (filters.receiveDateFrom) params.receiveDateFrom = filters.receiveDateFrom
    if (filters.receiveDateTo) params.receiveDateTo = filters.receiveDateTo
    if (filters.status) params.status = filters.status
    if (filters.cityId) params.cityId = filters.cityId
    if (filters.districtId) params.districtId = filters.districtId
    if (!auth.isViewer && filters.senderName.trim ()) params.senderQuery = filters.senderName.trim ()
    if (filters.senderTypeId) params.senderTypeId = filters.senderTypeId
    if (filters.serviceId) params.serviceId = filters.serviceId
    if (filters.deliveryId) params.deliveryId = filters.deliveryId
    if (filters.methodId) params.methodId = filters.methodId
    if (filters.cropCategoryId) params.cropCategoryId = filters.cropCategoryId
    if (filters.cropId) params.cropId = filters.cropId
    if (filters.damageId) params.damageId = filters.damageId
    if (filters.pestTypeId) params.pestTypeId = filters.pestTypeId
    if (filters.pestCategoryId) params.pestCategoryId = filters.pestCategoryId
    if (filters.hintId) params.hintId = filters.hintId
    const res = await caseApi.exportCsv (params as unknown as Record<string, string | number>)
    const url = URL.createObjectURL (res.data as Blob)
    const a = document.createElement ('a')
    a.href = url
    a.download = `case-export-${new Date ().toISOString ().slice (0, 10)}.csv`
    document.body.appendChild (a)
    a.click ()
    a.remove ()
    URL.revokeObjectURL (url)
  } catch {
    // 錯誤由攔截器處理
  }
}

async function confirmDelete (id: number) {
  const result = await Swal.fire ({
    icon: 'warning',
    title: '確定刪除此案件？',
    text: '此操作無法復原',
    showCancelButton: true,
    confirmButtonText: '刪除',
    cancelButtonText: '取消',
  })
  if (result.isConfirmed) {
    try {
      await caseApi.remove (id)
      Swal.fire ({ icon: 'success', title: '已刪除', timer: 1200, showConfirmButton: false })
      // 刪除後回到第一頁重新載入
      page.value = 0
      await load ()
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
      <div class="d-flex gap-1">
        <button class="btn btn-outline-primary btn-sm" :aria-expanded="showFilter" aria-controls="caseFilterCard" @click="showFilter = !showFilter">篩選</button>
        <button v-if="auth.isStaff" class="btn btn-outline-success btn-sm me-1" @click="exportCsv">匯出 CSV</button>
        <router-link v-if="auth.isStaff" class="btn btn-success btn-sm" to="/cases/new">新增</router-link>
      </div>
    </div>

    <!-- 篩選工具列：依指示換行
         第1列：收件日期區間、狀態
         第2列：田區縣市、田區鄉鎮、送件人、身分別
         第3列：服務類別、送件方式、耕種方式
         第4列：作物類別、作物、被害部位
         第5列：害物、害物類別、建議類別 -->
    <div v-show="showFilter" id="caseFilterCard" class="card shadow-sm mb-3">
      <div class="card-body">
        <div class="row g-2 align-items-end">
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">收件日期起</label>
            <input v-model="filters.receiveDateFrom" type="date" class="form-control form-control-sm" />
          </div>
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">收件日期迄</label>
            <input v-model="filters.receiveDateTo" type="date" class="form-control form-control-sm" />
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
          <div class="col-md-3"></div>
        </div>
        <div class="row g-2 align-items-end mt-2">
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">田區縣市</label>
            <select v-model="filters.cityId" class="form-select form-select-sm">
              <option :value="undefined">全部</option>
              <option v-for="city in cityOptions" :key="city.id" :value="city.id">{{ city.name }}</option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">田區鄉鎮</label>
            <select v-model="filters.districtId" class="form-select form-select-sm" :disabled="!filters.cityId">
              <option :value="undefined">全部</option>
              <option v-for="d in filteredDistricts" :key="d.id" :value="d.id">{{ d.name }}</option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">送件人 (姓名/顯示/電話)</label>
            <input v-model="filters.senderName" type="text" class="form-control form-control-sm" :disabled="auth.isViewer" :placeholder="auth.isViewer ? '檢視者無權限篩選' : '輸入關鍵字'" :title="auth.isViewer ? '檢視者無權限篩選送件人' : ''" />
          </div>
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">身分別</label>
            <select v-model="filters.senderTypeId" class="form-select form-select-sm">
              <option :value="undefined">全部</option>
              <option v-for="st in senderTypeOptions" :key="st.id" :value="st.id">{{ st.name }}</option>
            </select>
          </div>
        </div>
        <div class="row g-2 align-items-end mt-2">
          <div class="col-md-4">
            <label class="form-label small text-muted mb-1">服務類別</label>
            <select v-model="filters.serviceId" class="form-select form-select-sm">
              <option :value="undefined">全部</option>
              <option v-for="service in serviceOptions" :key="service.id" :value="service.id">
                {{ service.name }}
              </option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label small text-muted mb-1">送件方式</label>
            <select v-model="filters.deliveryId" class="form-select form-select-sm">
              <option :value="undefined">全部</option>
              <option v-for="d in deliveryOptions" :key="d.id" :value="d.id">{{ d.name }}</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label small text-muted mb-1">耕種方式</label>
            <select v-model="filters.methodId" class="form-select form-select-sm">
              <option :value="undefined">全部</option>
              <option v-for="m in methodOptions" :key="m.id" :value="m.id">{{ m.name }}</option>
            </select>
          </div>
        </div>
        <div class="row g-2 align-items-end mt-2">
          <div class="col-md-4">
            <label class="form-label small text-muted mb-1">作物類別</label>
            <select v-model="filters.cropCategoryId" class="form-select form-select-sm">
              <option :value="undefined">全部</option>
              <option v-for="cc in cropCategoryOptions" :key="cc.id" :value="cc.id">{{ cc.name }}</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label small text-muted mb-1">作物</label>
            <select v-model="filters.cropId" class="form-select form-select-sm">
              <option :value="undefined">全部</option>
              <option v-for="crop in filteredCrops" :key="crop.id" :value="crop.id">
                {{ crop.name }}
              </option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label small text-muted mb-1">被害部位</label>
            <select v-model="filters.damageId" class="form-select form-select-sm">
              <option :value="undefined">全部</option>
              <option v-for="d in damageOptions" :key="d.id" :value="d.id">{{ d.name }}</option>
            </select>
          </div>
        </div>
        <div class="row g-2 align-items-end mt-2">
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">害物</label>
            <select v-model="filters.pestTypeId" class="form-select form-select-sm">
              <option :value="undefined">全部</option>
              <option v-for="pt in pestTypeOptions" :key="pt.id" :value="pt.id">{{ pt.name }}</option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">害物類別</label>
            <select v-model="filters.pestCategoryId" class="form-select form-select-sm">
              <option :value="undefined">全部</option>
              <option v-for="pc in filteredPestCategories" :key="pc.id" :value="pc.id">{{ pc.name }}</option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label small text-muted mb-1">建議類別</label>
            <select v-model="filters.hintId" class="form-select form-select-sm">
              <option :value="undefined">全部</option>
              <option v-for="h in hintOptions" :key="h.id" :value="h.id">{{ h.name }}</option>
            </select>
          </div>
          <div class="col-md-3 text-md-end">
            <label class="form-label small text-muted mb-1 d-block">&nbsp;</label>
            <button class="btn btn-sm btn-primary me-1" @click="applyFilters">篩選</button>
            <button class="btn btn-sm btn-outline-secondary border-0 text-secondary" @click="clearFilters">清除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分頁控制 (篩選卡下方)：可輸入頁碼＋自選每頁筆數，與下方分頁同樣與 card 保持間距 -->
    <div v-if="total > 0" class="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3 mb-3">
      <div class="d-flex align-items-center gap-2">
        <label class="form-label small text-muted mb-0 text-nowrap">每頁筆數</label>
        <select v-model.number="size" class="form-select form-select-sm" style="width:auto" @change="onSizeChange">
          <option v-for="opt in sizeOptions" :key="opt" :value="opt">{{ opt }} 筆/頁</option>
        </select>
        <span class="small text-muted text-nowrap">共 {{ total }} 筆，{{ totalPages }} 頁</span>
      </div>
      <nav aria-label="案件分頁">
        <ul class="pagination pagination-sm mb-0 justify-content-center">
          <li class="page-item" :class="{ disabled: page === 0 }">
            <button class="page-link border-0 text-secondary" :disabled="page === 0" @click="goToPage (0)" style="height:31px"><<</button>
          </li>
          <li class="page-item" :class="{ disabled: page === 0 }">
            <button class="page-link border-0 text-secondary" :disabled="page === 0" @click="goToPage (page - 1)" style="height:31px"><</button>
          </li>
          <li class="page-item">
            <span class="page-link border-0 text-secondary d-flex align-items-center gap-1" style="height:31px;padding:0 0.5rem">
              <input v-model.number="pageInput" type="number" class="form-control form-control-sm text-center p-0" style="width:64px;height:24px;font-size:0.875rem;line-height:1.5" :min="1" :max="totalPages" @keyup.enter="onPageInputConfirm" @blur="onPageInputConfirm" /> / {{ totalPages }}
            </span>
          </li>
          <li class="page-item" :class="{ disabled: page >= totalPages - 1 }">
            <button class="page-link border-0 text-secondary" :disabled="page >= totalPages - 1" @click="goToPage (page + 1)" style="height:31px">></button>
          </li>
          <li class="page-item" :class="{ disabled: page >= totalPages - 1 }">
            <button class="page-link border-0 text-secondary" :disabled="page >= totalPages - 1" @click="goToPage (totalPages - 1)" style="height:31px">>></button>
          </li>
        </ul>
      </nav>
    </div>

    <div class="card shadow-sm">
      <div class="table-responsive position-relative" style="overflow-x:auto;-webkit-overflow-scrolling:touch">
        <table class="table table-hover align-middle mb-0 text-nowrap" style="min-width:1000px;table-layout:fixed">
          <thead class="table-light">
            <tr>
              <th style="width:70px;min-width:70px;cursor:pointer" @click="sortBy('caseId')">編號 {{ sortIcon('caseId') }}</th>
              <th style="width:110px;min-width:110px;cursor:pointer" @click="sortBy('receiveDate')">收件日期 {{ sortIcon('receiveDate') }}</th>
              <th style="width:100px;min-width:100px;cursor:pointer" @click="sortBy('deliveryName')">送件方式 {{ sortIcon('deliveryName') }}</th>
              <th style="width:110px;min-width:110px;cursor:pointer" @click="sortBy('cropName')">作物 {{ sortIcon('cropName') }}</th>
              <th style="width:140px;min-width:140px;cursor:pointer" @click="sortBy('senderLabel')">送件人 {{ sortIcon('senderLabel') }}</th>
              <th style="width:160px;min-width:160px;cursor:pointer" @click="sortBy('pestLabel')">害物 {{ sortIcon('pestLabel') }}</th>
              <th style="width:90px;min-width:90px;cursor:pointer" @click="sortBy('status')">狀態 {{ sortIcon('status') }}</th>
              <th class="text-end" style="width:130px;min-width:130px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="text-center text-muted py-4">載入中…</td>
            </tr>
            <tr v-else-if="pagedCases.length === 0">
              <td colspan="8" class="text-center text-muted py-4">尚無案件</td>
            </tr>
            <tr v-for="c in pagedCases" :key="c.caseId">
              <td style="width:70px">{{ c.caseId }}</td>
              <td style="width:110px">{{ c.receiveDate }}</td>
              <td style="width:100px" class="text-truncate" :title="c.deliveryName ?? '—'">{{ c.deliveryName ?? '—' }}</td>
              <td style="width:110px" class="text-truncate" :title="c.cropName">{{ c.cropName }}</td>
              <td style="width:140px" class="text-truncate" :title="senderLabel (c)">{{ senderLabel (c) }}</td>
              <td style="width:160px" class="text-truncate" :title="pestLabel (c)"><span>{{ pestLabel (c) }}</span> <span v-if="isComposite (c)" class="badge bg-warning text-dark ms-1">複合因素</span></td>
              <td style="width:90px">
                <span class="badge" :class="statusBadgeClass (c.status)">{{ statusLabel (c.status) }}</span>
              </td>
              <td class="text-end" style="width:130px;min-width:130px">
                <button class="btn btn-sm btn-outline-success me-1" @click="viewDetail (c.caseId)">
                  預覽
                </button>
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
                  @click="confirmDelete (c.caseId)"
                >
                  刪除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 分頁控制 (表格下方)：同上方，支援輸入頁碼與自選筆數 -->
    <div v-if="total > 0" class="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3">
      <div class="d-flex align-items-center gap-2">
        <label class="form-label small text-muted mb-0 text-nowrap">每頁筆數</label>
        <select v-model.number="size" class="form-select form-select-sm" style="width:auto" @change="onSizeChange">
          <option v-for="opt in sizeOptions" :key="opt" :value="opt">{{ opt }} 筆/頁</option>
        </select>
        <span class="small text-muted text-nowrap">共 {{ total }} 筆，{{ totalPages }} 頁</span>
      </div>
      <nav aria-label="案件分頁">
        <ul class="pagination pagination-sm mb-0 justify-content-center">
          <li class="page-item" :class="{ disabled: page === 0 }">
            <button class="page-link border-0 text-secondary" :disabled="page === 0" @click="goToPage (0)" style="height:31px"><<</button>
          </li>
          <li class="page-item" :class="{ disabled: page === 0 }">
            <button class="page-link border-0 text-secondary" :disabled="page === 0" @click="goToPage (page - 1)" style="height:31px"><</button>
          </li>
          <li class="page-item">
            <span class="page-link border-0 text-secondary d-flex align-items-center gap-1" style="height:31px;padding:0 0.5rem">
              <input v-model.number="pageInput" type="number" class="form-control form-control-sm text-center p-0" style="width:64px;height:24px;font-size:0.875rem;line-height:1.5" :min="1" :max="totalPages" @keyup.enter="onPageInputConfirm" @blur="onPageInputConfirm" /> / {{ totalPages }}
            </span>
          </li>
          <li class="page-item" :class="{ disabled: page >= totalPages - 1 }">
            <button class="page-link border-0 text-secondary" :disabled="page >= totalPages - 1" @click="goToPage (page + 1)" style="height:31px">></button>
          </li>
          <li class="page-item" :class="{ disabled: page >= totalPages - 1 }">
            <button class="page-link border-0 text-secondary" :disabled="page >= totalPages - 1" @click="goToPage (totalPages - 1)" style="height:31px">>></button>
          </li>
        </ul>
      </nav>
    </div>
  </div>
</template>
