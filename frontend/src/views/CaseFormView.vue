<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Swal from 'sweetalert2'
import { aiApi, caseApi, refApi, senderApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { STATUS_OPTIONS } from '../utils/caseStatus'
import { escapeHtml } from '../utils/escapeHtml'
import type { components } from '../types/api'

interface IdName {
  id: number
  name: string
  userId?: number | null
  username?: string | null
}
interface CropCategory {
  id: number
  name: string
  crops: IdName[]
}
interface PestCategoryItem {
  id: number
  code: string
  name: string
}
interface PestType {
  id: number
  name: string
  categories: PestCategoryItem[]
}
interface City {
  id: number
  name: string
  districts: IdName[]
}

const auth = useAuthStore ()
const route = useRoute ()
const router = useRouter ()

// 編輯模式：路由帶 :id 即為編輯既有案件
const editId = route.params.id ? Number (route.params.id) : null

// 表單資料 (對應後端 CaseCreateRequest)
const form = reactive ({
  receiveDate: new Date ().toISOString ().slice (0, 10),
  cropScale: '',
  damageScale: '',
  caseDescription: '',
  hintDescription: '',
  status: '' as string,
  senderId: null as number | null,
  senderName: '',
  senderDisplayName: '',
  senderPhone: '',
  senderAddress: '',
  senderDistrictId: 0,
  senderTypeId: 0,
  fieldDistrictId: null as number | null,
  methodId: 0,
  cropId: 0,
  serviceId: 0,
  deliverId: 0,
  damageIds: [] as number[],
  hintIds: [] as number[],
  pestCategoryIds: [] as number[],
  identifierIds: [] as number[],
  inlineIdentifiers: [] as { name: string }[],
})
const pendingInlineSigners = ref<{ name: string; tempId: number }[]>([])
// 內聯作物暫存（單一，記憶體暫存不落庫；提交時以 inlineCrop 併入，inline 覆蓋 cropId）
const pendingInlineCrop = ref<{ name: string; cropCategoryId: number } | null>(null)
// 暫存作物於下拉中的臨時 id（負值，後端僅認 inlineCrop）
const TEMP_CROP_ID = -1

// 作物下拉：遠端清單合併暫存（標記待提交）
const mergedCrops = computed (() => {
  const base = selectedCropCategoryId.value
    ? (cropCategories.value.find ((c) => c.id === selectedCropCategoryId.value)?.crops ?? [])
    : cropCategories.value.flatMap ((c) => c.crops)
  if (pendingInlineCrop.value && (!selectedCropCategoryId.value || selectedCropCategoryId.value === pendingInlineCrop.value.cropCategoryId)) {
    return [...base, { id: TEMP_CROP_ID, name: `${pendingInlineCrop.value.name}（待提交）` }]
  }
  return base
})

// 作物級聯：所選分類 (null 為全部)
const selectedCropCategoryId = ref<number | null>(null)
const selectedSenderCityId = ref<number | null>(null)
const selectedFieldCityId = ref<number | null>(null)
const fieldSameAsSender = ref (false)

// 害物三段式列編輯：每列為一害物 (類型→分類→學名：描述)，可同分類多筆
interface PestRow {
  pestTypeId: number
  pestCategoryId: number
  pestNote: string
}
const pestRows = ref<PestRow[]>([])

// 參照資料
const cropCategories = ref<CropCategory[]>([])
const pestTypes = ref<PestType[]>([])
const damages = ref<IdName[]>([])
const hints = ref<IdName[]>([])
const methods = ref<IdName[]>([])
const deliveries = ref<IdName[]>([])
const services = ref<IdName[]>([])
const cities = ref<City[]>([])
const senderTypes = ref<IdName[]>([])
const identifiers = ref<IdName[]>([])

const loading = ref (true)
const saving = ref (false)
const savingSender = ref (false)
const analyzing = ref (false)

// 送件人編輯狀態：快照 (取消編輯還原用) 與髒污判定
interface SenderSnapshot {
  senderId: number | null
  senderName: string
  senderDisplayName: string
  senderPhone: string
  senderAddress: string
  senderDistrictId: number
  senderTypeId: number
}
let senderSnapshot: SenderSnapshot | null = null

function snapshotSender (): SenderSnapshot {
  return {
    senderId: form.senderId,
    senderName: form.senderName,
    senderDisplayName: form.senderDisplayName,
    senderPhone: form.senderPhone,
    senderAddress: form.senderAddress,
    senderDistrictId: form.senderDistrictId,
    senderTypeId: form.senderTypeId,
  }
}

function restoreSender () {
  if (!senderSnapshot) return
  form.senderId = senderSnapshot.senderId
  form.senderName = senderSnapshot.senderName
  form.senderDisplayName = senderSnapshot.senderDisplayName
  form.senderPhone = senderSnapshot.senderPhone
  form.senderAddress = senderSnapshot.senderAddress
  form.senderDistrictId = senderSnapshot.senderDistrictId
  form.senderTypeId = senderSnapshot.senderTypeId
}

const senderDirty = computed (() => {
  if (!senderSnapshot) return false
  const s = senderSnapshot
  return form.senderName !== s.senderName
    || form.senderDisplayName !== s.senderDisplayName
    || form.senderPhone !== s.senderPhone
    || form.senderAddress !== s.senderAddress
    || form.senderDistrictId !== s.senderDistrictId
    || form.senderTypeId !== s.senderTypeId
})

const filteredSenderDistricts = computed (() => {
  if (selectedSenderCityId.value == null) return []
  const city = cities.value.find ((c) => c.id === selectedSenderCityId.value)
  return city ? city.districts : []
})
const filteredFieldDistricts = computed (() => {
  if (selectedFieldCityId.value == null) return []
  const city = cities.value.find ((c) => c.id === selectedFieldCityId.value)
  return city ? city.districts : []
})

watch (selectedSenderCityId, (newCityId) => {
  const districts = cities.value.find ((c) => c.id === newCityId)?.districts ?? []
  if (districts.length > 0 && !districts.some ((d) => d.id === form.senderDistrictId)) {
    form.senderDistrictId = districts[0].id
  }
})

watch (() => form.senderDistrictId, (districtId) => {
  if (!districtId) return
  const city = cities.value.find ((c) => c.districts.some ((d) => d.id === districtId))
  if (city && selectedSenderCityId.value !== city.id) {
    selectedSenderCityId.value = city.id
  }
  // 若勾選和送件人相同，同步田區位置
  if (fieldSameAsSender.value) {
    form.fieldDistrictId = districtId
    if (city) selectedFieldCityId.value = city.id
  }
})

watch (selectedFieldCityId, (newCityId) => {
  const districts = cities.value.find ((c) => c.id === newCityId)?.districts ?? []
  if (districts.length > 0 && form.fieldDistrictId && !districts.some ((d) => d.id === form.fieldDistrictId)) {
    form.fieldDistrictId = districts[0].id
  } else if (districts.length > 0 && !form.fieldDistrictId && !fieldSameAsSender.value && fieldLocationVisible.value) {
    form.fieldDistrictId = districts[0].id
  }
})

watch (() => form.fieldDistrictId, (districtId) => {
  if (!districtId) return
  const city = cities.value.find ((c) => c.districts.some ((d) => d.id === districtId))
  if (city && selectedFieldCityId.value !== city.id) {
    selectedFieldCityId.value = city.id
  }
})

watch (fieldSameAsSender, (checked) => {
  if (checked) {
    form.fieldDistrictId = form.senderDistrictId || null
    if (form.senderDistrictId) {
      const city = cities.value.find ((c) => c.districts.some ((d) => d.id === form.senderDistrictId))
      if (city) selectedFieldCityId.value = city.id
    }
  }
})

// 診斷區段顯示條件：編輯模式先顯示但髒時隱藏；新增模式需已儲存送件人且無髒污
const diagnosisVisible = computed (() => {
  if (editId !== null) return !senderDirty.value
  return form.senderId !== null && !senderDirty.value
})
// 田區位置初隱：與診斷區段同訊號（新增時需送件人確定後才可選田區）
const fieldLocationVisible = computed (() => {
  if (editId !== null) return true
  return form.senderId !== null && !senderDirty.value
})

/**
 * 一鍵清空送件人輸入（新增模式，取消按鈕）：直接清空、無任何 Swal/alert，不觸發 fuzzy 搜尋。
 * 田區同步重置，因田區初隱後再次顯示前應為空。
 */
function resetSenderForm () {
  form.senderId = null
  form.senderName = ''
  form.senderDisplayName = ''
  form.senderPhone = ''
  form.senderAddress = ''
  const defaults = { districtId: cities.value[0]?.districts[0]?.id ?? 0, cityId: cities.value[0]?.id ?? null, senderTypeId: senderTypes.value[0]?.id ?? 0 }
  form.senderDistrictId = defaults.districtId
  form.senderTypeId = defaults.senderTypeId
  selectedSenderCityId.value = defaults.cityId
  form.fieldDistrictId = null
  selectedFieldCityId.value = null
  fieldSameAsSender.value = false
  lastFuzzyQuery = ''
  senderSnapshot = snapshotSender ()
}

// 獨立儲存送件人：有 senderId 時 PUT 更新，否則 POST 建立；成功後鎖定 senderId 並解鎖診斷區段

// 診斷儲存阻擋：送件人未儲存 (無 senderId) 或尚有未儲存的送件人編輯
const diagnosisSaveBlocked = computed (() => !editId && (form.senderId === null || senderDirty.value))

// Fuzzy 相似提示：任一欄位輸入後即時 (debounce)，有候選時提示帶入
let fuzzyTimer: ReturnType<typeof setTimeout> | null = null
let lastFuzzyQuery = ''
const inlineCandidates = ref<any[]>([])
const inlineCandidatesQuery = ref('')
const selectedInlineCandidateId = ref('')
const fuzzyFields = computed (() => [form.senderName, form.senderPhone, form.senderDisplayName] as const)
watch (fuzzyFields,
 (newVals, oldVals) => {
    if (fuzzyTimer) clearTimeout (fuzzyTimer)
    // 已選用既有送件人且無編輯時不提示
    if (form.senderId !== null && !senderDirty.value) return
    // 找出本次變動的欄位值作為 q (任一欄位相似即觸發，電話需 4 碼，其餘 2 碼)
    const thresholds = [2, 4, 2]
    let q = ''
    if (oldVals) {
      for (let i = 0; i < newVals.length; i++) {
        if (newVals[i] !== oldVals[i] && newVals[i].trim ().length >= thresholds[i]) {
          q = newVals[i].trim ()
          break
        }
      }
      if (!q) return // 無有效變動
    } else {
      for (let i = 0; i < newVals.length; i++) {
        if (newVals[i].trim ().length >= thresholds[i]) { q = newVals[i].trim (); break }
      }
      if (!q) return
    }
    if (q === lastFuzzyQuery) return
    fuzzyTimer = setTimeout (async () => {
      try {
        const { data } = await senderApi.search (q)
        if (data.length === 0) {
          inlineCandidates.value = []
          inlineCandidatesQuery.value = ''
          return
        }
        lastFuzzyQuery = q
        inlineCandidates.value = data as any[]
        inlineCandidatesQuery.value = q
        selectedInlineCandidateId.value = String ((data as any[])[0]?.senderId ?? '')
      } catch {}
    }, 600)
  },)

function applyCandidate (id: number, candidates: any[]) {
  form.senderId = id
  const chosen = candidates.find ((s) => String (s.senderId) === String (id))
  if (chosen) {
    form.senderName = chosen.name ?? ''
    form.senderDisplayName = chosen.displayName ?? ''
    form.senderPhone = chosen.phone ?? ''
    form.senderAddress = chosen.address ?? form.senderAddress
    if (chosen.districtId) form.senderDistrictId = chosen.districtId
    if (chosen.senderTypeId) form.senderTypeId = chosen.senderTypeId
  }
  senderSnapshot = snapshotSender ()
  inlineCandidates.value = []
  inlineCandidatesQuery.value = ''
}

function useNewSender () {
  form.senderId = null
  senderSnapshot = snapshotSender ()
  inlineCandidates.value = []
  inlineCandidatesQuery.value = ''
  selectedInlineCandidateId.value = ''
  lastFuzzyQuery = ''
}

function confirmInlineSelection () {
  const val = selectedInlineCandidateId.value
  if (!val) return
  applyCandidate (Number (val), inlineCandidates.value)
}

async function searchCandidates () {
  // 手動搜尋：電話需 4 碼以上，其餘 2 碼
  const nameTrim = form.senderName.trim ()
  const phoneTrim = form.senderPhone.trim ()
  const displayTrim = form.senderDisplayName.trim ()
  if (phoneTrim && phoneTrim.length < 4 && !nameTrim && !displayTrim) {
    Swal.fire ({ icon: 'info', title: '電話需輸入 4 碼以上才可搜尋' })
    return
  }
  const q = nameTrim || phoneTrim || displayTrim
  if (!q) {
    Swal.fire ({ icon: 'info', title: '請輸入姓名、電話或顯示名稱關鍵字' })
    return
  }
  if (q === phoneTrim && q.length < 4) {
    Swal.fire ({ icon: 'info', title: '電話需輸入 4 碼以上才可搜尋' })
    return
  }
  try {
    const { data } = await senderApi.search (q)
    if (data.length === 0) {
      inlineCandidates.value = []
      inlineCandidatesQuery.value = q
      form.senderId = null
      return
    }
    inlineCandidates.value = data as any[]
    inlineCandidatesQuery.value = q
    selectedInlineCandidateId.value = String ((data as any[])[0]?.senderId ?? '')
    lastFuzzyQuery = q
  } catch {}
}

/**
 * 一鍵清空送件人輸入（新增模式，取消按鈕）：直接清空、無任何 Swal/alert，不觸發 fuzzy 搜尋。
 * 田區同步重置，因田區初隱後再次顯示前應為空。
 */
// 獨立儲存送件人：有 senderId 時 PUT 更新，否則 POST 建立；成功後鎖定 senderId 並解鎖診斷區段
async function saveSender () {
  if (!form.senderDistrictId) {
    Swal.fire ({ icon: 'warning', title: '欄位不完整', text: '請選擇鄉鎮市區' })
    return
  }
  if (!form.senderPhone.trim () && !form.senderDisplayName.trim ()) {
    Swal.fire ({ icon: 'warning', title: '欄位不完整', text: '電話與顯示名稱至少需提供一項' })
    return
  }
  const payload = {
    name: form.senderName || undefined,
    displayName: form.senderDisplayName || undefined,
    phone: form.senderPhone || undefined,
    address: form.senderAddress,
    districtId: form.senderDistrictId,
    senderTypeId: form.senderTypeId,
  }
  savingSender.value = true
  try {
    if (form.senderId) {
      await senderApi.update (form.senderId, payload)
    } else {
      const { data } = await senderApi.create (payload)
      form.senderId = (data as any).senderId
    }
    senderSnapshot = snapshotSender ()
    lastFuzzyQuery = ''
    Swal.fire ({ icon: 'success', title: '送件人已儲存', timer: 1200, showConfirmButton: false })
  } catch {}
  finally {
    savingSender.value = false
  }
}

// 取消送件人編輯：還原快照並同步縣市
function cancelSenderEdit () {
  restoreSender ()
  if (form.senderDistrictId) {
    const city = cities.value.find ((c) => c.districts.some ((d) => d.id === form.senderDistrictId))
    if (city) selectedSenderCityId.value = city.id
  }
}

async function handleCreateCrop () {
  const { value: formData } = await Swal.fire ({
    title: '新增作物',
    html: `
      <input id="swal-crop-name" class="swal2-input" placeholder="作物名稱" />
      <select id="swal-crop-category" class="swal2-select">
        ${cropCategories.value.map ((c) => `<option value="${c.id}" ${c.id === selectedCropCategoryId.value ? 'selected' : ''}>${c.name}</option>`).join ('')}
      </select>
    `,
    showCancelButton: true,
    confirmButtonText: '新增',
    cancelButtonText: '取消',
    preConfirm: () => {
      const name = (document.getElementById ('swal-crop-name') as HTMLInputElement).value.trim ()
      const catId = Number ((document.getElementById ('swal-crop-category') as HTMLSelectElement).value)
      if (!name) return Swal.showValidationMessage ('名稱不可為空白')
      if (!catId) return Swal.showValidationMessage ('請選擇作物別')
      return { name, categoryId: catId }
    },
  })
  if (!formData) return
  const pending = { name: formData.name, cropCategoryId: formData.categoryId }
  // 同分類同名已存在（去空白大小寫不敏感）直接選用既有項，不入暫存
  const existing = cropCategories.value
    .find ((c) => c.id === formData.categoryId)?.crops
    .find ((cr) => cr.name.trim ().toLowerCase () === formData.name.toLowerCase ())
  if (existing) {
    form.cropId = existing.id
    selectedCropCategoryId.value = formData.categoryId
    pendingInlineCrop.value = null
    Swal.fire ({ icon: 'success', title: '已選用既有作物', timer: 1200, showConfirmButton: false })
    return
  }
  // 原子建：暫存至 pendingInlineCrop，待案件提交時同交易建立
  pendingInlineCrop.value = pending
  form.cropId = TEMP_CROP_ID
  selectedCropCategoryId.value = formData.categoryId
  Swal.fire ({ icon: 'success', title: '已加入待建作物（提交案件時建立）', timer: 1500, showConfirmButton: false })
}

async function handleCreateIdentifier () {
  const { value: name } = await Swal.fire ({
    title: '新增簽名人',
    input: 'text',
    inputLabel: '簽名人名稱',
    inputPlaceholder: '請輸入簽名人名稱',
    showCancelButton: true,
    confirmButtonText: '新增',
    cancelButtonText: '取消',
    inputValidator: (v) => (!v?.trim () ? '名稱不可為空白' : null),
  })
  if (!name) return
  const trimmed = name.trim ()
  const existing = identifiers.value.find ((i) => i.name === trimmed)
  if (existing) {
    if (!form.identifierIds.includes (existing.id)) form.identifierIds.push (existing.id)
    Swal.fire ({ icon: 'success', title: '已選用既有簽名人', timer: 1200, showConfirmButton: false })
    return
  }
  // 原子建：暫存至 pending，待案件提交時同交易建立（user=null）
  const tempId = -Date.now () - Math.floor (Math.random () * 1000)
  identifiers.value.push ({ id: tempId, name: trimmed } as any)
  form.identifierIds.push (tempId)
  pendingInlineSigners.value.push ({ name: trimmed, tempId })
  form.inlineIdentifiers.push ({ name: trimmed })
  Swal.fire ({ icon: 'success', title: '已加入待建簽名人（提交案件時建立）', timer: 1500, showConfirmButton: false })
}

// 依選定作物反查其所屬分類名稱 (供 AI Prompt 使用)；暫存作物以其暫存分類為準
const selectedCropCategory = computed (() => {
  if (pendingInlineCrop.value) {
    return cropCategories.value.find ((c) => c.id === pendingInlineCrop.value!.cropCategoryId)?.name ?? ''
  }
  const crop = cropCategories.value
    .flatMap ((c) => c.crops.map ((cr) => ({ ...cr, category: c.name })))
    .find ((c) => c.id === form.cropId)
  return crop?.category ?? ''
})

// 狀態選項：CLOSED 僅 ADMIN 可選；已選 CLOSED 仍可切換，僅儲存成功後由後端鎖定
const statusOptions = computed (() => {
  return auth.isAdmin ? STATUS_OPTIONS : STATUS_OPTIONS.filter ((o) => o.value !== 'CLOSED')
})

async function loadRefs () {
  // 平行載入所有下拉選單資料
  const [cc, pt, dm, hn, mt, dl, sv, ct, st, idf] = await Promise.all ([
    refApi.cropCategories (),
    refApi.pestTypes (),
    refApi.damages (),
    refApi.hints (),
    refApi.methods (),
    refApi.deliveries (),
    refApi.services (),
    refApi.cities (),
    refApi.senderTypes (),
    refApi.identifiers (),
  ])
  cropCategories.value = cc.data
  pestTypes.value = pt.data
  damages.value = dm.data
  hints.value = hn.data
  methods.value = mt.data
  deliveries.value = dl.data
  services.value = sv.data
  cities.value = ct.data
  senderTypes.value = st.data
  identifiers.value = (idf.data as IdName[]) ?? []
  // 新增案件時自動帶入當前使用者簽名人（後端兜底，前端預選提升 UX）
  if (!editId && form.identifierIds.length === 0) {
    try {
      const me = await refApi.myIdentifier ()
      const myId = (me.data as any)?.id ?? (me.data as any)?.identifierId
      if (myId && !form.identifierIds.includes (myId)) form.identifierIds = [myId]
    } catch {
      const name = auth.user?.displayName
      if (name) {
        const found = identifiers.value.find ((i) => i.name === name)
        if (found && !form.identifierIds.includes (found.id)) form.identifierIds = [found.id]
      }
    }
  }

  // 建立模式：套用合理的預設值
  form.methodId = methods.value[0]?.id ?? 0
  form.deliverId = deliveries.value[0]?.id ?? 0
  form.serviceId = services.value[0]?.id ?? 0
  form.senderTypeId = senderTypes.value[0]?.id ?? 0
  // 初始化縣市選取 (依預設 district 或首個城市)
  if (cities.value.length > 0) {
    const initDistrict = form.senderDistrictId
    const city = cities.value.find ((c) => c.districts.some ((d) => d.id === initDistrict))
    selectedSenderCityId.value = city ? city.id : cities.value[0].id
    if (!initDistrict && city) {
      form.senderDistrictId = city.districts[0]?.id ?? 0
    }
    // 田區位置初始化
    if (form.fieldDistrictId) {
      const fCity = cities.value.find ((c) => c.districts.some ((d) => d.id === form.fieldDistrictId))
      if (fCity) selectedFieldCityId.value = fCity.id
    } else {
      selectedFieldCityId.value = selectedSenderCityId.value
    }
  }

  // 新增模式亦初始化快照，確保髒污判定正確
  if (!editId) {
    senderSnapshot = snapshotSender ()
  }

  if (editId) {
    await loadCase (editId)
  }
}

async function loadCase (id: number) {
  const { data } = await caseApi.detail (id)
  const d = data as components['schemas']['CaseResponse']
  // 已結案案件僅管理者可編輯：STAFF 提示後返回列表 (防直接輸入網址進編輯頁)
  if (d.status === 'CLOSED' && !auth.isAdmin) {
    Swal.fire ({
      icon: 'warning',
      title: '案件已結案',
      text: '已結案案件僅管理者可編輯',
    }).then (() => router.push ({ path: '/cases', query: route.query as any }))
    return
  }
  form.receiveDate = d.receiveDate ?? ''
  form.cropScale = d.cropScale ?? ''
  form.damageScale = d.damageScale ?? ''
  form.caseDescription = (d as any).caseDescription ?? ''
  form.hintDescription = d.hintDescription ?? ''
  form.status = d.status ?? ''
  form.senderName = d.senderName ?? ''
  form.senderDisplayName = (d as any).senderDisplayName ?? ''
  form.senderPhone = d.senderPhone ?? ''
  form.senderAddress = d.senderAddress ?? ''
  form.senderDistrictId = d.senderDistrictId ?? 0
  form.senderTypeId = d.senderTypeId ?? 0
  form.senderId = (d as any).senderId ?? null
  form.fieldDistrictId = (d as any).fieldDistrictId ?? null
  // 同步縣市選取
  if (form.senderDistrictId) {
    const city = cities.value.find ((c) => c.districts.some ((d2) => d2.id === form.senderDistrictId))
    if (city) selectedSenderCityId.value = city.id
  }
  if (form.fieldDistrictId) {
    const fCity = cities.value.find ((c) => c.districts.some ((d2) => d2.id === form.fieldDistrictId))
    if (fCity) selectedFieldCityId.value = fCity.id
    // 若田區與送件人相同，自動勾選
    if (form.fieldDistrictId === form.senderDistrictId) fieldSameAsSender.value = true
  } else {
    selectedFieldCityId.value = selectedSenderCityId.value
    fieldSameAsSender.value = false
  }
  // 編輯模式快照：用於髒污判定與取消還原，修復編輯時無法更新 sender 的 bug
  senderSnapshot = snapshotSender ()
  form.damageIds = d.damages?.map ((x) => x.id).filter ((x): x is number => x != null) ?? []
  form.hintIds = d.hints?.map ((x) => x.id).filter ((x): x is number => x != null) ?? []
  // 新結構：pestCategoryWithNotes (含 note)，回退舊 pestCategoryIds
  const pcs = (d as any).pestCategories ?? []
  // 若為新結構 (有 pestNote)，轉為 pestRows；否則回退為舊的 id 列表
  if (pcs.length > 0 && pcs[0] && 'pestNote' in pcs[0]) {
    pestRows.value = pcs.map ((x: any) => ({
      pestTypeId: pestTypes.value.find ((p) => p.categories.some ((c) => c.id === x.id))?.id ?? pestTypes.value[0]?.id ?? 0,
      pestCategoryId: x.id,
      pestNote: x.pestNote ?? '',
    }))
    // 同步舊的 id 列表供 submit 回退
    form.pestCategoryIds = pcs.map ((x: any) => x.id).filter ((x: any) => x != null)
  } else {
    form.pestCategoryIds = pcs.map ((x: any) => x.id).filter ((x: any) => x != null)
    // 若舊資料無 note，初始化一列空的 pestRow 以便編輯
    if (form.pestCategoryIds.length > 0 && pestRows.value.length === 0) {
      pestRows.value = form.pestCategoryIds.map ((id) => ({
        pestTypeId: pestTypes.value.find ((p) => p.categories.some ((c) => c.id === id))?.id ?? pestTypes.value[0]?.id ?? 0,
        pestCategoryId: id,
        pestNote: '',
      }))
    }
  }
  form.identifierIds = d.identifiers?.map ((x) => x.id).filter ((x): x is number => x != null) ?? []

  // 由名稱反查 ID (後端詳細回應帶的是名稱而非 ID)
  const crop = cropCategories.value
    .flatMap ((c) => c.crops)
    .find ((c) => c.name === d.cropName)
  form.cropId = crop?.id ?? 0
  const catForCrop = cropCategories.value.find ((cc) => cc.crops.some ((cr) => cr.id === form.cropId))
  selectedCropCategoryId.value = catForCrop?.id ?? null
  form.methodId = methods.value.find ((m) => m.name === d.methodName)?.id ?? 0
  form.serviceId = services.value.find ((s) => s.name === d.serviceName)?.id ?? 0
  form.deliverId = deliveries.value.find ((x) => x.name === d.deliveryName)?.id ?? 0
}

onMounted (async () => {
  await loadRefs ()
  loading.value = false
})

// 切換多選 (Checkbox) 的輔助函式
function toggle (arr: number[], id: number) {
  const i = arr.indexOf (id)
  if (i >= 0) arr.splice (i, 1)
  else arr.push (id)
}

async function submit () {
  // 診斷儲存阻擋：送件人未儲存 (無 senderId) 或尚有未儲存的送件人編輯
  if (diagnosisSaveBlocked.value) {
    Swal.fire ({ icon: 'warning', title: '請先儲存送件人', text: '送件人資料已修改，請先點「更新送件人」或「取消編輯」' })
    return
  }
  // 送出前檢查 (後端仍有完整驗證)；暫存作物視為已選（提交時以 inlineCrop 建立）
  if (!form.cropId && !pendingInlineCrop.value) {
    Swal.fire ({ icon: 'warning', title: '欄位不完整', text: '請選擇作物' })
    return
  }
  // 田區位置必填 (與診斷卡片同交易，後端 NOT NULL)
  const effectiveForValidate = fieldSameAsSender.value ? form.senderDistrictId : form.fieldDistrictId
  if (!effectiveForValidate) {
    Swal.fire ({ icon: 'warning', title: '欄位不完整', text: '請選擇田區位置' })
    return
  }
  saving.value = true
  try {
    if (editId) {
      const pestWithNotes = pestRows.value
        .filter ((r) => r.pestCategoryId)
        .map ((r) => ({ pestCategoryId: r.pestCategoryId, pestNote: r.pestNote?.trim () || undefined }))
      const effectiveFieldDistrictId = fieldSameAsSender.value ? form.senderDistrictId : form.fieldDistrictId
      await caseApi.update (editId, {
        receiveDate: form.receiveDate,
        cropScale: form.cropScale ?? undefined,
        damageScale: form.damageScale ?? undefined,
        caseDescription: form.caseDescription,
        hintDescription: form.hintDescription,
        status: form.status || undefined,
        senderId: form.senderId ?? undefined,
        methodId: form.methodId,
        cropId: pendingInlineCrop.value ? undefined : form.cropId,
        inlineCrop: pendingInlineCrop.value ?? undefined,
        serviceId: form.serviceId,
        deliverId: form.deliverId,
        fieldDistrictId: effectiveFieldDistrictId ?? undefined,
        damageIds: form.damageIds,
        hintIds: form.hintIds,
        pestCategoryWithNotes: pestWithNotes,
        identifierIds: form.identifierIds.filter ((id) => id > 0),
        inlineIdentifiers: pendingInlineSigners.value.map ((p) => ({ name: p.name })),
      } as any)
      // 成功後清空暫存並重載下拉（暫存已隨案件提交落庫）
      pendingInlineCrop.value = null
      pendingInlineSigners.value = []
      await loadRefs ()
      Swal.fire ({ icon: 'success', title: '儲存成功', timer: 1200, showConfirmButton: false }).then (() => {
        router.push ({ path: `/cases/${editId}`, query: route.query as any })
      })
    } else {
      const pestWithNotes2 = pestRows.value
        .filter ((r) => r.pestCategoryId)
        .map ((r) => ({ pestCategoryId: r.pestCategoryId, pestNote: r.pestNote?.trim () || undefined }))
      const effectiveFieldDistrictId2 = fieldSameAsSender.value ? form.senderDistrictId : form.fieldDistrictId
      const { data } = await caseApi.create ({
        receiveDate: form.receiveDate,
        cropScale: form.cropScale ?? undefined,
        damageScale: form.damageScale ?? undefined,
        caseDescription: form.caseDescription,
        hintDescription: form.hintDescription,
        senderId: form.senderId ?? undefined,
        senderAddress: form.senderAddress || undefined,
        senderDistrictId: form.senderDistrictId,
        senderTypeId: form.senderTypeId,
        fieldDistrictId: effectiveFieldDistrictId2 ?? undefined,
        methodId: form.methodId,
        cropId: pendingInlineCrop.value ? undefined : form.cropId,
        inlineCrop: pendingInlineCrop.value ?? undefined,
        serviceId: form.serviceId,
        deliverId: form.deliverId,
        damageIds: form.damageIds,
        hintIds: form.hintIds,
        pestCategoryWithNotes: pestWithNotes2,
        identifierIds: form.identifierIds.filter ((id) => id > 0),
        inlineIdentifiers: pendingInlineSigners.value.map ((p) => ({ name: p.name })),
      } as any)
      const newId = (data as any)?.caseId
      // 成功後清空暫存並重載下拉（暫存已隨案件提交落庫）
      pendingInlineCrop.value = null
      pendingInlineSigners.value = []
      await loadRefs ()
      Swal.fire ({ icon: 'success', title: '儲存成功', timer: 1200, showConfirmButton: false }).then (() => {
        if (newId) router.push ({ path: `/cases/${newId}`, query: route.query as any })
        else router.push ({ path: '/cases', query: route.query as any })
      })
    }
  } catch {
    // 錯誤由攔截器處理
  } finally {
    saving.value = false
  }
}

// AI 診斷：組出欄位資料送後端，再由後端代理 llama.cpp
async function runAi () {
  const category = selectedCropCategory.value
  const pendingCrop = pendingInlineCrop.value && form.cropId === TEMP_CROP_ID ? pendingInlineCrop.value : null
  const crop = pendingCrop
    ? { id: TEMP_CROP_ID, name: pendingCrop.name }
    : cropCategories.value.flatMap ((c) => c.crops).find ((c) => c.id === form.cropId)
  if (!crop) {
    Swal.fire ({ icon: 'warning', title: '請先選擇作物' })
    return
  }
  analyzing.value = true
  try {
    const pestCategoryNames = pestRows.value
      .map ((r) => pestTypes.value.flatMap ((p) => p.categories).find ((c) => c.id === r.pestCategoryId)?.name)
      .filter ((n): n is string => !!n)
    const pestNotes = pestRows.value.map ((r) => r.pestNote?.trim ()).filter ((n): n is string => !!n && n.length > 0)
    const { data } = await aiApi.analyze ({
      cropName: crop.name,
      cropCategory: category,
      damages: damages.value.filter ((d) => form.damageIds.includes (d.id)).map ((d) => d.name),
      pestCategories: pestCategoryNames,
      pestNotes: pestNotes.length > 0 ? pestNotes : undefined,
      caseDescription: form.caseDescription,
      cropScale: form.cropScale,
      damageScale: form.damageScale,
      cultivationMethod: methods.value.find ((m) => m.id === form.methodId)?.name,
      hintDescription: form.hintDescription,
    } as any)
    Swal.fire ({
      icon: 'info',
      title: `AI 診斷建議 (${(data.elapsedMs / 1000).toFixed (1)} 秒)`,
      width: 700,
      html: `<div class="text-start"><pre class="text-wrap small">${escapeHtml (data.suggestion)}</pre></div>`,
      confirmButtonText: '關閉',
    })
  } catch {
    // 錯誤由攔截器處理 (模型未啟動時亦會在此顯示)
  } finally {
    analyzing.value = false
  }
}
</script>

<template>
  <div class="container py-4" style="max-width: 960px">
    <h4 class="mb-4">{{ editId ? `編輯案件 #${editId}` : '建立診斷案件' }}</h4>

    <div v-if="loading" class="text-center text-muted py-5">載入中…</div>

    <form v-else @submit.prevent="submit">
      <!-- 送件人資料 -->
      <div class="card shadow-sm mb-4">
        <div class="card-header bg-success text-white d-flex justify-content-between align-items-center">
          <span>送件人資料</span>
          <div>
            <button type="button" class="btn btn-sm btn-light me-1" @click="searchCandidates">搜尋候選</button>
            <button
              v-if="!form.senderId"
              type="button"
              class="btn btn-sm btn-warning me-1"
              :disabled="savingSender"
              @click="saveSender"
            >
              {{ savingSender ? '儲存中…' : '儲存送件人' }}
            </button>
            <button
              v-if="!editId"
              type="button"
              class="btn btn-sm btn-outline-light"
              @click="resetSenderForm"
            >
              取消
            </button>
            <template v-if="form.senderId && senderDirty">
              <button
                type="button"
                class="btn btn-sm btn-warning me-1"
                :disabled="savingSender"
                @click="saveSender"
              >
                {{ savingSender ? '更新中…' : '更新送件人' }}
              </button>
              <button
                type="button"
                class="btn btn-sm btn-outline-light"
                @click="cancelSenderEdit"
              >
                取消編輯
              </button>
            </template>
          </div>
        </div>
        <div class="card-body row g-3">
          <div class="col-md-3">
            <label class="form-label">姓名</label>
            <input v-model.trim="form.senderName" class="form-control" placeholder="可空" />
          </div>
          <div class="col-md-3">
            <label class="form-label">顯示名稱 (Line/FB 暱稱)</label>
            <input v-model.trim="form.senderDisplayName" class="form-control" placeholder="電話與顯示名稱至少一項" />
          </div>
          <div class="col-md-3">
            <label class="form-label">電話</label>
            <input v-model.trim="form.senderPhone" class="form-control" placeholder="電話與顯示名稱至少一項" />
          </div>
          <div class="col-md-3">
            <label class="form-label">身分別</label>
            <select v-model.number="form.senderTypeId" class="form-select" required>
              <option v-for="s in senderTypes" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label">縣市</label>
            <select v-model.number="selectedSenderCityId" class="form-select">
              <option :value="null" disabled>請選擇縣市</option>
              <option v-for="c in cities" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label">鄉鎮市區</label>
            <select v-model.number="form.senderDistrictId" class="form-select">
              <option value="0" disabled>請選擇鄉鎮市區</option>
              <option v-for="d in filteredSenderDistricts" :key="d.id" :value="d.id">{{ d.name }}</option>
            </select>
          </div>
          <div class="col-md-5">
            <label class="form-label">地址 (選填)</label>
            <input v-model.trim="form.senderAddress" class="form-control" />
          </div>
          <div v-if="form.senderId" class="col-12">
            <div class="alert alert-info py-2 mb-0 small">已選用既有送件人 #{{ form.senderId }}，儲存時將沿用該送件人 <button type="button" class="btn btn-sm btn-outline-secondary ms-2" @click="form.senderId = null">取消沿用</button></div>
          </div>
          <div v-if="inlineCandidates.length > 0" class="col-12">
            <div class="border rounded p-2 bg-light">
              <div class="d-flex align-items-center gap-2">
                <select v-model="selectedInlineCandidateId" class="form-select form-select-sm flex-grow-1">
                  <option v-for="c in inlineCandidates" :key="c.senderId" :value="String(c.senderId)">{{ c.name ?? '' }}{{ c.displayName ? '('+c.displayName+')' : '' }} - {{ c.phone ?? '' }} - {{ c.districtName ?? '' }}</option>
                </select>
                <button type="button" class="btn btn-sm btn-primary flex-shrink-0" @click="confirmInlineSelection">使用</button>
                <button type="button" class="btn btn-sm btn-outline-secondary flex-shrink-0" @click="useNewSender">建立新送件人</button>
              </div>
              <small class="text-muted">找到 {{ inlineCandidates.length }} 筆相似送件人（關鍵字：{{ inlineCandidatesQuery }}），預設最接近者；其他欄位編輯會更新比對</small>
            </div>
          </div>
        </div>
      </div>

      <!-- 田區位置 -->
      <div v-if="fieldLocationVisible" class="card shadow-sm mb-4">
        <div class="card-header bg-success text-white d-flex justify-content-between align-items-center">
          <span>田區位置</span>
          <div class="form-check mb-0">
            <input id="fieldSameAsSender" class="form-check-input" type="checkbox" v-model="fieldSameAsSender" />
            <label for="fieldSameAsSender" class="form-check-label text-white small">和送件人相同</label>
          </div>
        </div>
        <div class="card-body row g-3">
          <div class="col-md-6">
            <label class="form-label">縣市</label>
            <select v-model.number="selectedFieldCityId" class="form-select" :disabled="fieldSameAsSender">
              <option :value="null" disabled>請選擇縣市</option>
              <option v-for="c in cities" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">鄉鎮市區</label>
            <select v-model.number="form.fieldDistrictId" class="form-select" :disabled="fieldSameAsSender">
              <option :value="null" disabled>請選擇鄉鎮市區</option>
              <option v-for="d in filteredFieldDistricts" :key="d.id" :value="d.id">{{ d.name }}</option>
            </select>
          </div>
          <div v-if="fieldSameAsSender" class="col-12">
            <div class="alert alert-info py-2 mb-0 small">已勾選和送件人相同，田區位置將與送件人縣市鄉鎮一致</div>
          </div>
        </div>
      </div>

      <!-- 收件資訊：收件日期與狀態獨立卡片 -->
      <div v-if="diagnosisVisible" class="card shadow-sm mb-4">
        <div class="card-header bg-success text-white">收件資訊</div>
        <div class="card-body row g-3">
          <div class="col-md-6">
            <label class="form-label">收件日期</label>
            <input v-model="form.receiveDate" type="date" class="form-control" required />
          </div>
          <div v-if="editId" class="col-md-6">
            <label class="form-label">狀態</label>
            <select v-model="form.status" class="form-select">
              <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
            <div class="form-text small text-muted">
              {{
                form.status === 'CLOSED'
                  ? '已結案狀態不可變更'
                  : auth.isAdmin
                    ? '待處理→已處理→已結案'
                    : '待處理→已處理'
              }}
            </div>
          </div>
          <div v-if="!editId" class="col-md-6">
            <label class="form-label">狀態</label>
            <input class="form-control" value="待處理" disabled />
            <div class="form-text small text-muted">新增案件預設為待處理</div>
          </div>
        </div>
      </div>

      <!-- 作物資訊：送件人帶入/儲存後才顯示 -->
      <div v-if="diagnosisVisible" class="card shadow-sm mb-4">
        <div class="card-header bg-success text-white">作物資訊</div>
        <div class="card-body row g-3">
          <div class="col-md-6">
            <label class="form-label">服務類別</label>
            <select v-model.number="form.serviceId" class="form-select" required>
              <option v-for="s in services" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">交付方式</label>
            <select v-model.number="form.deliverId" class="form-select" required>
              <option v-for="d in deliveries" :key="d.id" :value="d.id">{{ d.name }}</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label">耕種方式</label>
            <select v-model.number="form.methodId" class="form-select" required>
              <option v-for="m in methods" :key="m.id" :value="m.id">{{ m.name }}</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label">作物類別</label>
            <select v-model.number="selectedCropCategoryId" class="form-select">
              <option :value="null">全部</option>
              <option v-for="cc in cropCategories" :key="cc.id" :value="cc.id">{{ cc.name }}</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label d-flex justify-content-between">作物 <button type="button" class="btn btn-sm btn-outline-success py-0" @click="handleCreateCrop">＋新增</button></label>
            <select v-model.number="form.cropId" class="form-select" required :disabled="!selectedCropCategoryId">
              <option value="0" disabled>{{ selectedCropCategoryId ? '請選擇作物' : '請先選擇作物類別' }}</option>
              <option
                v-for="cr in mergedCrops"
                :key="cr.id"
                :value="cr.id"
              >
                {{ cr.name }}
              </option>
            </select>
          </div>
          <div class="col-12">
            <label class="form-label">被害部位 (可複選)</label>
            <div class="d-flex flex-wrap gap-3">
              <label v-for="d in damages" :key="d.id" class="form-check form-check-inline">
                <input
                  class="form-check-input"
                  type="checkbox"
                  :checked="form.damageIds.includes (d.id)"
                  @change="toggle (form.damageIds, d.id)"
                />
                <span class="form-check-label">{{ d.name }}</span>
              </label>
            </div>
          </div>
          <div class="col-md-6">
            <label class="form-label">栽培面積或規模</label>
            <input v-model.trim="form.cropScale" class="form-control" placeholder="例：2 分地" />
          </div>
          <div class="col-md-6">
            <label class="form-label">被害面積或規模</label>
            <input v-model.trim="form.damageScale" class="form-control" placeholder="例：約 3 成" />
          </div>
          <div class="col-12">
            <label class="form-label">土壤、栽培、用藥紀錄</label>
            <textarea v-model.trim="form.caseDescription" class="form-control" rows="2" placeholder=""></textarea>
          </div>
        </div>
      </div>

      <!-- 診斷結果與建議：同診斷區段 -->
      <div v-if="diagnosisVisible" class="card shadow-sm mb-4">
        <div class="card-header bg-success text-white">診斷結果與建議</div>
        <div class="card-body row g-3">
          <div class="col-12">
            <label class="form-label">診斷結果 <span class="text-muted small">(可增刪多列，同分類可多筆)</span></label>
            <div v-for="(row, idx) in pestRows" :key="idx" class="row g-2 align-items-end mb-2">
              <div class="col-md-3">
                <label class="form-label small text-muted mb-1">害物</label>
                <select v-model.number="row.pestTypeId" class="form-select form-select-sm">
                  <option v-for="p in pestTypes" :key="p.id" :value="p.id">{{ p.name }}</option>
                </select>
              </div>
              <div class="col-md-4">
                <label class="form-label small text-muted mb-1">害物因素</label>
                <select v-model.number="row.pestCategoryId" class="form-select form-select-sm">
                  <option
                    v-for="c in (pestTypes.find ((p) => p.id === row.pestTypeId)?.categories ?? [])"
                    :key="c.id"
                    :value="c.id"
                  >
                    {{ c.code }} {{ c.name }}
                  </option>
                </select>
              </div>
              <div class="col-md-4">
                <label class="form-label small text-muted mb-1">學名：描述</label>
                <input v-model.trim="row.pestNote" class="form-control form-control-sm" placeholder="學名：描述 (選填)" maxlength="500" />
              </div>
              <div class="col-md-1">
                <button type="button" class="btn btn-sm btn-outline-danger" @click="pestRows.splice (idx, 1)">刪除</button>
              </div>
            </div>
            <div class="w-100"></div>
            <button type="button" class="btn btn-sm btn-outline-primary mt-2" @click="pestRows.push ({ pestTypeId: pestTypes[0]?.id ?? 0, pestCategoryId: pestTypes[0]?.categories[0]?.id ?? 0, pestNote: '' })">＋新增因素</button>
          </div>
          <div class="col-12">
            <label class="form-label">建議採取措施</label>
            <textarea v-model.trim="form.hintDescription" class="form-control" rows="2"></textarea>
          </div>
          <div class="col-md-6">
            <label class="form-label">防治建議 (可複選)</label>
            <div v-for="h in hints" :key="h.id" class="form-check">
              <input
                class="form-check-input"
                type="checkbox"
                :checked="form.hintIds.includes (h.id)"
                @change="toggle (form.hintIds, h.id)"
              />
              <span class="form-check-label">{{ h.name }}</span>
            </div>
          </div>
          <div class="col-md-6">
            <label class="form-label d-flex justify-content-between">診斷簽名人 (可複選) <button type="button" class="btn btn-sm btn-outline-success py-0" @click="handleCreateIdentifier">＋新增</button></label>
            <div v-for="i in identifiers" :key="i.id" class="form-check">
              <input
                class="form-check-input"
                type="checkbox"
                :checked="form.identifierIds.includes (i.id)"
                @change="toggle (form.identifierIds, i.id)"
              />
              <span class="form-check-label">{{ i.name }} <span class="badge ms-1" :class="i.userId ? 'bg-primary' : 'bg-secondary'">{{ i.userId ? '使用者' : '非使用者' }}</span> <span v-if="i.username" class="text-muted small">· {{ i.username }}</span></span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="diagnosisVisible" class="d-flex gap-2 justify-content-end">
        <button
          v-if="auth.isStaff"
          type="button"
          class="btn btn-info text-white"
          :disabled="analyzing"
          @click="runAi"
        >
          {{ analyzing ? 'AI 診斷中…' : 'AI 診斷' }}
        </button>
        <router-link class="btn btn-outline-secondary" :to="{ path: '/cases', query: route.query as any }">取消</router-link>
        <button
          type="submit"
          class="btn btn-success"
          :disabled="saving || diagnosisSaveBlocked"
          :title="diagnosisSaveBlocked ? '請先儲存送件人' : ''"
        >
          {{ saving ? '儲存中…' : '儲存案件' }}
        </button>
      </div>
    </form>
  </div>
</template>
