<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Swal from 'sweetalert2'
import { refApi, refAdminApi } from '../api'

type IdName = { id: number; name: string }
type TabKey =
  | 'damages'
  | 'hints'
  | 'methods'
  | 'deliveries'
  | 'services'
  | 'identifiers'
  | 'senderTypes'
  | 'crops'
  | 'cropCategories'
  | 'pestCategories'

function escapeHtml(s: string) {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]!))
}

const currentTab = ref<TabKey>('damages')
const loading = ref(true)

// 各類資料
const damages = ref<IdName[]>([])
const hints = ref<IdName[]>([])
const methods = ref<IdName[]>([])
const deliveries = ref<IdName[]>([])
const services = ref<IdName[]>([])
const identifiers = ref<IdName[]>([])
const senderTypes = ref<IdName[]>([])
const crops = ref<{ id: number; name: string; cropCategoryId?: number }[]>([])
const cropCategories = ref<IdName[]>([])
const pestCategories = ref<{ id: number; code: string; name: string; pestTypeId: number; sortOrder: number }[]>([])
const pestTypes = ref<{ id: number; name: string }[]>([])

// 載入所有參照資料
async function loadAll() {
  loading.value = true
  try {
    const [damRes, hintRes, methodRes, deliverRes, serviceRes, identRes, senderTypeRes, cropCatRes, pestTypeRes] =
      await Promise.all([
        refApi.damages(),
        refApi.hints(),
        refApi.methods(),
        refApi.deliveries(),
        refApi.services(),
        refApi.identifiers(),
        refApi.senderTypes(),
        refApi.cropCategories(),
        refApi.pestTypes(),
      ])
    damages.value = (damRes.data as IdName[]) ?? []
    hints.value = (hintRes.data as IdName[]) ?? []
    methods.value = (methodRes.data as IdName[]) ?? []
    deliveries.value = (deliverRes.data as IdName[]) ?? []
    services.value = (serviceRes.data as IdName[]) ?? []
    identifiers.value = (identRes.data as IdName[]) ?? []
    senderTypes.value = (senderTypeRes.data as IdName[]) ?? []
    // cropCategories 含 crops
    const cats = cropCatRes.data as { id: number; name: string; crops: { id: number; name: string }[] }[]
    cropCategories.value = cats.map((c) => ({ id: c.id, name: c.name }))
    crops.value = cats.flatMap((c) => c.crops.map((cr) => ({ id: cr.id, name: cr.name, cropCategoryId: c.id })))
    // pestTypes 含 categories
    const pts = pestTypeRes.data as { id: number; name: string; categories: { id: number; code: string; name: string; sortOrder: number }[] }[]
    pestTypes.value = pts.map((p) => ({ id: p.id, name: p.name }))
    pestCategories.value = pts.flatMap((p) =>
      p.categories.map((cat) => ({
        id: cat.id,
        code: cat.code,
        name: cat.name,
        pestTypeId: p.id,
        sortOrder: cat.sortOrder,
      })),
    )
  } catch {
    // 錯誤由攔截器處理
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)

// 通用新增/編輯/刪除處理
async function handleCreate() {
  if (['damages', 'hints', 'methods', 'deliveries', 'services', 'identifiers', 'senderTypes', 'cropCategories'].includes(currentTab.value)) {
    const { value: name } = await Swal.fire({
      title: '新增',
      input: 'text',
      inputLabel: '名稱',
      inputPlaceholder: '請輸入名稱',
      showCancelButton: true,
      confirmButtonText: '新增',
      cancelButtonText: '取消',
      inputValidator: (v) => (!v?.trim() ? '名稱不可為空白' : null),
    })
    if (!name) return
    try {
      switch (currentTab.value) {
        case 'damages':
          await refAdminApi.createDamage({ name: name.trim() })
          break
        case 'hints':
          await refAdminApi.createHint({ name: name.trim() })
          break
        case 'methods':
          await refAdminApi.createMethod({ name: name.trim() })
          break
        case 'deliveries':
          await refAdminApi.createDelivery({ name: name.trim() })
          break
        case 'services':
          await refAdminApi.createService({ name: name.trim() })
          break
        case 'identifiers':
          await refAdminApi.createIdentifier({ name: name.trim() })
          break
        case 'senderTypes':
          await refAdminApi.createSenderType({ name: name.trim() })
          break
        case 'cropCategories':
          await refAdminApi.createCropCategory({ name: name.trim() })
          break
      }
      await loadAll()
      Swal.fire({ icon: 'success', title: '已新增', timer: 1200, showConfirmButton: false })
    } catch {}
  } else if (currentTab.value === 'crops') {
    if (cropCategories.value.length === 0) {
      Swal.fire({ icon: 'warning', title: '請先建立作物分類' })
      return
    }
    const { value: form } = await Swal.fire({
      title: '新增作物',
      html: `
        <input id="swal-crop-name" class="swal2-input" placeholder="作物名稱" />
        <select id="swal-crop-cat" class="swal2-select">
          ${cropCategories.value.map((c) => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('')}
        </select>
      `,
      showCancelButton: true,
      confirmButtonText: '新增',
      cancelButtonText: '取消',
      preConfirm: () => {
        const name = (document.getElementById('swal-crop-name') as HTMLInputElement).value.trim()
        const catId = Number((document.getElementById('swal-crop-cat') as HTMLSelectElement).value)
        if (!name) return Swal.showValidationMessage('名稱不可為空白')
        return { name, cropCategoryId: catId }
      },
    })
    if (!form) return
    try {
      await refAdminApi.createCrop(form)
      await loadAll()
      Swal.fire({ icon: 'success', title: '已新增', timer: 1200, showConfirmButton: false })
    } catch {}
  } else if (currentTab.value === 'pestCategories') {
    if (pestTypes.value.length === 0) {
      Swal.fire({ icon: 'warning', title: '無害物類型可選' })
      return
    }
    const { value: form } = await Swal.fire({
      title: '新增病蟲害分類',
      html: `
        <input id="swal-pc-code" class="swal2-input" placeholder="代碼" />
        <input id="swal-pc-name" class="swal2-input" placeholder="名稱" />
        <select id="swal-pc-type" class="swal2-select">
          ${pestTypes.value.map((p) => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join('')}
        </select>
        <input id="swal-pc-order" class="swal2-input" placeholder="排序" type="number" value="0" />
      `,
      showCancelButton: true,
      confirmButtonText: '新增',
      cancelButtonText: '取消',
      preConfirm: () => {
        const code = (document.getElementById('swal-pc-code') as HTMLInputElement).value.trim()
        const name = (document.getElementById('swal-pc-name') as HTMLInputElement).value.trim()
        const pestTypeId = Number((document.getElementById('swal-pc-type') as HTMLSelectElement).value)
        const sortOrder = Number((document.getElementById('swal-pc-order') as HTMLInputElement).value)
        if (!code) return Swal.showValidationMessage('代碼不可為空白')
        if (!name) return Swal.showValidationMessage('名稱不可為空白')
        if (!Number.isFinite(sortOrder)) return Swal.showValidationMessage('排序須為數字')
        if (sortOrder < 0) return Swal.showValidationMessage('排序不可為負')
        return { code, name, pestTypeId, sortOrder }
      },
    })
    if (!form) return
    try {
      await refAdminApi.createPestCategory(form)
      await loadAll()
      Swal.fire({ icon: 'success', title: '已新增', timer: 1200, showConfirmButton: false })
    } catch {}
  }
}

async function handleEdit(item: any) {
  if (['damages', 'hints', 'methods', 'deliveries', 'services', 'identifiers', 'senderTypes', 'cropCategories'].includes(currentTab.value)) {
    const { value: name } = await Swal.fire({
      title: '編輯',
      input: 'text',
      inputValue: item.name,
      inputLabel: '名稱',
      showCancelButton: true,
      confirmButtonText: '儲存',
      cancelButtonText: '取消',
      inputValidator: (v) => (!v?.trim() ? '名稱不可為空白' : null),
    })
    if (!name) return
    const trimmed = name.trim()
    if (trimmed === item.name) return
    try {
      switch (currentTab.value) {
        case 'damages':
          await refAdminApi.updateDamage(item.id, { name: trimmed })
          break
        case 'hints':
          await refAdminApi.updateHint(item.id, { name: trimmed })
          break
        case 'methods':
          await refAdminApi.updateMethod(item.id, { name: trimmed })
          break
        case 'deliveries':
          await refAdminApi.updateDelivery(item.id, { name: trimmed })
          break
        case 'services':
          await refAdminApi.updateService(item.id, { name: trimmed })
          break
        case 'identifiers':
          await refAdminApi.updateIdentifier(item.id, { name: trimmed })
          break
        case 'senderTypes':
          await refAdminApi.updateSenderType(item.id, { name: trimmed })
          break
        case 'cropCategories':
          await refAdminApi.updateCropCategory(item.id, { name: trimmed })
          break
      }
      await loadAll()
      Swal.fire({ icon: 'success', title: '已更新', timer: 1200, showConfirmButton: false })
    } catch {}
  } else if (currentTab.value === 'crops') {
    const { value: form } = await Swal.fire({
      title: '編輯作物',
      html: `
        <input id="swal-crop-name" class="swal2-input" placeholder="作物名稱" value="${escapeHtml(item.name)}" />
        <select id="swal-crop-cat" class="swal2-select">
          ${cropCategories.value.map((c) => `<option value="${c.id}" ${c.id === item.cropCategoryId ? 'selected' : ''}>${escapeHtml(c.name)}</option>`).join('')}
        </select>
      `,
      showCancelButton: true,
      confirmButtonText: '儲存',
      cancelButtonText: '取消',
      preConfirm: () => {
        const name = (document.getElementById('swal-crop-name') as HTMLInputElement).value.trim()
        const catId = Number((document.getElementById('swal-crop-cat') as HTMLSelectElement).value)
        if (!name) return Swal.showValidationMessage('名稱不可為空白')
        return { name, cropCategoryId: catId }
      },
    })
    if (!form) return
    try {
      await refAdminApi.updateCrop(item.id, form)
      await loadAll()
      Swal.fire({ icon: 'success', title: '已更新', timer: 1200, showConfirmButton: false })
    } catch {}
  } else if (currentTab.value === 'pestCategories') {
    const { value: form } = await Swal.fire({
      title: '編輯病蟲害分類',
      html: `
        <input id="swal-pc-code" class="swal2-input" placeholder="代碼" value="${escapeHtml(item.code)}" />
        <input id="swal-pc-name" class="swal2-input" placeholder="名稱" value="${escapeHtml(item.name)}" />
        <select id="swal-pc-type" class="swal2-select">
          ${pestTypes.value.map((p) => `<option value="${p.id}" ${p.id === item.pestTypeId ? 'selected' : ''}>${escapeHtml(p.name)}</option>`).join('')}
        </select>
        <input id="swal-pc-order" class="swal2-input" placeholder="排序" type="number" value="${item.sortOrder}" />
      `,
      showCancelButton: true,
      confirmButtonText: '儲存',
      cancelButtonText: '取消',
      preConfirm: () => {
        const code = (document.getElementById('swal-pc-code') as HTMLInputElement).value.trim()
        const name = (document.getElementById('swal-pc-name') as HTMLInputElement).value.trim()
        const pestTypeId = Number((document.getElementById('swal-pc-type') as HTMLSelectElement).value)
        const sortOrder = Number((document.getElementById('swal-pc-order') as HTMLInputElement).value)
        if (!code) return Swal.showValidationMessage('代碼不可為空白')
        if (!name) return Swal.showValidationMessage('名稱不可為空白')
        if (!Number.isFinite(sortOrder)) return Swal.showValidationMessage('排序須為數字')
        if (sortOrder < 0) return Swal.showValidationMessage('排序不可為負')
        return { code, name, pestTypeId, sortOrder }
      },
    })
    if (!form) return
    try {
      await refAdminApi.updatePestCategory(item.id, form)
      await loadAll()
      Swal.fire({ icon: 'success', title: '已更新', timer: 1200, showConfirmButton: false })
    } catch {}
  }
}

async function handleDelete(item: any) {
  const result = await Swal.fire({
    icon: 'warning',
    title: `確定刪除「${item.name}」？`,
    text: '此操作無法復原，若已被案件引用將被拒絕',
    showCancelButton: true,
    confirmButtonText: '刪除',
    cancelButtonText: '取消',
  })
  if (!result.isConfirmed) return
  try {
    switch (currentTab.value) {
      case 'damages':
        await refAdminApi.deleteDamage(item.id)
        break
      case 'hints':
        await refAdminApi.deleteHint(item.id)
        break
      case 'methods':
        await refAdminApi.deleteMethod(item.id)
        break
      case 'deliveries':
        await refAdminApi.deleteDelivery(item.id)
        break
      case 'services':
        await refAdminApi.deleteService(item.id)
        break
      case 'identifiers':
        await refAdminApi.deleteIdentifier(item.id)
        break
      case 'senderTypes':
        await refAdminApi.deleteSenderType(item.id)
        break
      case 'crops':
        await refAdminApi.deleteCrop(item.id)
        break
      case 'cropCategories':
        await refAdminApi.deleteCropCategory(item.id)
        break
      case 'pestCategories':
        await refAdminApi.deletePestCategory(item.id)
        break
    }
    await loadAll()
    Swal.fire({ icon: 'success', title: '已刪除', timer: 1200, showConfirmButton: false })
  } catch {}
}

const tabs: { key: TabKey; label: string }[] = [
  { key: 'damages', label: '被害部位' },
  { key: 'hints', label: '防治建議' },
  { key: 'methods', label: '耕種方式' },
  { key: 'deliveries', label: '送件方式' },
  { key: 'services', label: '服務類別' },
  { key: 'identifiers', label: '簽名人' },
  { key: 'senderTypes', label: '身分別' },
  { key: 'crops', label: '作物' },
  { key: 'cropCategories', label: '作物分類' },
  { key: 'pestCategories', label: '病蟲害分類' },
]

function currentList(): any[] {
  switch (currentTab.value) {
    case 'damages':
      return damages.value
    case 'hints':
      return hints.value
    case 'methods':
      return methods.value
    case 'deliveries':
      return deliveries.value
    case 'services':
      return services.value
    case 'identifiers':
      return identifiers.value
    case 'senderTypes':
      return senderTypes.value
    case 'crops':
      return crops.value
    case 'cropCategories':
      return cropCategories.value
    case 'pestCategories':
      return pestCategories.value
  }
}
</script>

<template>
  <div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h4 class="mb-0">參照資料管理</h4>
      <button class="btn btn-success" @click="handleCreate">新增</button>
    </div>

    <ul class="nav nav-tabs mb-3">
      <li v-for="t in tabs" :key="t.key" class="nav-item">
        <button class="nav-link" :class="{ active: currentTab === t.key }" @click="currentTab = t.key">
          {{ t.label }}
        </button>
      </li>
    </ul>

    <div v-if="loading" class="text-center text-muted py-4">載入中…</div>
    <div v-else class="card shadow-sm">
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>ID</th>
              <th>名稱</th>
              <th v-if="currentTab === 'crops'">分類</th>
              <th v-if="currentTab === 'pestCategories'">代碼</th>
              <th v-if="currentTab === 'pestCategories'">類型</th>
              <th v-if="currentTab === 'pestCategories'">排序</th>
              <th class="text-end">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="currentList().length === 0">
              <td colspan="7" class="text-center text-muted py-4">尚無資料</td>
            </tr>
            <tr v-for="item in currentList()" :key="item.id">
              <td>{{ item.id }}</td>
              <td>{{ item.name }}</td>
              <td v-if="currentTab === 'crops'">{{ cropCategories.find((c) => c.id === (item as any).cropCategoryId)?.name ?? '—' }}</td>
              <td v-if="currentTab === 'pestCategories'">{{ (item as any).code }}</td>
              <td v-if="currentTab === 'pestCategories'">{{ pestTypes.find((p) => p.id === (item as any).pestTypeId)?.name ?? '—' }}</td>
              <td v-if="currentTab === 'pestCategories'">{{ (item as any).sortOrder }}</td>
              <td class="text-end">
                <button class="btn btn-sm btn-outline-primary me-1" @click="handleEdit(item)">編輯</button>
                <button class="btn btn-sm btn-outline-danger" @click="handleDelete(item)">刪除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
