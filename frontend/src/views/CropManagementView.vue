<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Swal from 'sweetalert2'
import { refApi, refAdminApi } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore ()

type CropRow = { id: number; name: string; cropCategoryId?: number }
type CropCat = { id: number; name: string }

function escapeHtml (s: string) {
  return s.replace (/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]!))
}

const loading = ref (true)
const showFilter = ref (false)
const filterQ = ref ('')
const filterCategoryId = ref<number | null>(null)
const crops = ref<CropRow[]>([])
const cropCategories = ref<CropCat[]>([])

async function loadAll () {
  loading.value = true
  try {
    const catRes = await refApi.cropCategories ()
    const cats = catRes.data as { id: number; name: string; crops: { id: number; name: string }[] }[]
    cropCategories.value = cats.map ((c) => ({ id: c.id, name: c.name }))
    crops.value = cats.flatMap ((c) => c.crops.map ((cr) => ({ id: cr.id, name: cr.name, cropCategoryId: c.id })))
  } catch {} finally { loading.value = false }
}

onMounted (loadAll)

const filtered = computed (() => {
  const q = filterQ.value.trim ().toLowerCase ()
  const catId = filterCategoryId.value
  return crops.value.filter ((c) => (!catId || c.cropCategoryId === catId) && (!q || c.name.toLowerCase ().includes (q)))
})

// 分頁
const page = ref (0)
const size = ref (10)
const sizeOptions = [10, 20, 50, 100]
const pageInput = ref (1)
const total = computed (() => filtered.value.length)
const totalPages = computed (() => Math.max (1, Math.ceil (total.value / size.value)))
watch (page, (v) => { pageInput.value = v + 1 })
function goToPage (p: number) {
  const c = Math.max (0, Math.min (p, totalPages.value - 1))
  if (c !== page.value) { page.value = c; pageInput.value = c + 1 } else pageInput.value = c + 1
}
function onSizeChange () { page.value = 0; pageInput.value = 1 }
function onPageInputConfirm () {
  let num = Number (pageInput.value)
  if (!Number.isFinite (num) || num < 1) num = 1
  if (num > totalPages.value) num = totalPages.value
  goToPage (num - 1)
}
watch ([filterQ, filterCategoryId], () => { page.value = 0; pageInput.value = 1 })
const paged = computed (() => filtered.value.slice (page.value * size.value, page.value * size.value + size.value))

async function handleCreate () {
  if (cropCategories.value.length === 0) {
    Swal.fire ({ icon: 'warning', title: '請先建立作物分類' })
    return
  }
  const { value: form } = await Swal.fire ({
    title: '新增作物',
    html: `<input id="swal-crop-name" class="swal2-input" placeholder="作物名稱" /><select id="swal-crop-cat" class="swal2-select">${cropCategories.value.map ((c) => `<option value="${c.id}">${escapeHtml (c.name)}</option>`).join ('')}</select>`,
    showCancelButton: true,
    confirmButtonText: '新增',
    cancelButtonText: '取消',
    preConfirm: () => {
      const name = (document.getElementById ('swal-crop-name') as HTMLInputElement).value.trim ()
      const catId = Number ((document.getElementById ('swal-crop-cat') as HTMLSelectElement).value)
      if (!name) return Swal.showValidationMessage ('名稱不可為空白')
      return { name, cropCategoryId: catId }
    },
  })
  if (!form) return
  try { await refAdminApi.createCrop (form); await loadAll (); Swal.fire ({ icon: 'success', title: '已新增', timer: 1200, showConfirmButton: false }) } catch {}
}

async function handleEdit (item: CropRow) {
  const { value: form } = await Swal.fire ({
    title: '編輯作物',
    html: `<input id="swal-crop-name" class="swal2-input" value="${escapeHtml (item.name)}" /><select id="swal-crop-cat" class="swal2-select">${cropCategories.value.map ((c) => `<option value="${c.id}" ${c.id === item.cropCategoryId ? 'selected' : ''}>${escapeHtml (c.name)}</option>`).join ('')}</select>`,
    showCancelButton: true,
    confirmButtonText: '儲存',
    cancelButtonText: '取消',
    preConfirm: () => {
      const name = (document.getElementById ('swal-crop-name') as HTMLInputElement).value.trim ()
      const catId = Number ((document.getElementById ('swal-crop-cat') as HTMLSelectElement).value)
      if (!name) return Swal.showValidationMessage ('名稱不可為空白')
      return { name, cropCategoryId: catId }
    },
  })
  if (!form) return
  try { await refAdminApi.updateCrop (item.id, form); await loadAll (); Swal.fire ({ icon: 'success', title: '已更新', timer: 1200, showConfirmButton: false }) } catch {}
}

async function handleDelete (item: CropRow) {
  const result = await Swal.fire ({ icon: 'warning', title: `確定刪除「${item.name}」？`, text: '此操作無法復原，若已被案件引用將被拒絕', showCancelButton: true, confirmButtonText: '刪除', cancelButtonText: '取消' })
  if (!result.isConfirmed) return
  try { await refAdminApi.deleteCrop (item.id); await loadAll (); Swal.fire ({ icon: 'success', title: '已刪除', timer: 1200, showConfirmButton: false }) } catch {}
}
</script>

<template>
  <div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h4 class="mb-0">作物管理</h4>
      <div class="d-flex gap-1">
        <button v-if="auth.isStaff" class="btn btn-success btn-sm" @click="handleCreate">新增</button>
        <button class="btn btn-outline-primary btn-sm" :aria-expanded="showFilter" aria-controls="cropFilterCard" @click="showFilter = !showFilter">篩選</button>
      </div>
    </div>
    <div v-show="showFilter" id="cropFilterCard" class="card shadow-sm mb-3">
      <div class="card-body py-2">
        <div class="row g-2 align-items-center">
          <div class="col-md-4"><input v-model="filterQ" type="text" class="form-control form-control-sm" placeholder="篩選名稱" /></div>
          <div class="col-md-4"><select v-model.number="filterCategoryId" class="form-select form-select-sm"><option :value="null">全部分類</option><option v-for="c in cropCategories" :key="c.id" :value="c.id">{{ c.name }}</option></select></div>
          <div class="col-md-4 text-muted small">{{ filtered.length }} 筆</div>
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
      <nav aria-label="作物分頁">
        <ul class="pagination pagination-sm mb-0 justify-content-center">
          <li class="page-item" :class="{ disabled: page === 0 }"><button class="page-link border-0 text-secondary" :disabled="page === 0" @click="goToPage(0)" style="height:31px"><<</button></li>
          <li class="page-item" :class="{ disabled: page === 0 }"><button class="page-link border-0 text-secondary" :disabled="page === 0" @click="goToPage(page - 1)" style="height:31px"><</button></li>
          <li class="page-item"><span class="page-link border-0 text-secondary d-flex align-items-center gap-1" style="height:31px;padding:0 0.5rem"><input v-model.number="pageInput" type="number" class="form-control form-control-sm text-center p-0" style="width:64px;height:24px;font-size:0.875rem;line-height:1.5" :min="1" :max="totalPages" @keyup.enter="onPageInputConfirm" @blur="onPageInputConfirm" /> / {{ totalPages }}</span></li>
          <li class="page-item" :class="{ disabled: page >= totalPages - 1 }"><button class="page-link border-0 text-secondary" :disabled="page >= totalPages - 1" @click="goToPage(page + 1)" style="height:31px">></button></li>
          <li class="page-item" :class="{ disabled: page >= totalPages - 1 }"><button class="page-link border-0 text-secondary" :disabled="page >= totalPages - 1" @click="goToPage(totalPages - 1)" style="height:31px">>></button></li>
        </ul>
      </nav>
    </div>

    <div v-if="loading" class="text-center text-muted py-4">載入中…</div>
    <div v-else class="card shadow-sm">
      <div class="table-responsive position-relative" style="overflow-x:auto;-webkit-overflow-scrolling:touch">
        <table class="table table-hover align-middle mb-0 text-nowrap" style="min-width:600px;table-layout:fixed">
          <thead class="table-light"><tr><th style="width:60px;min-width:60px">ID</th><th style="width:200px;min-width:200px">名稱</th><th style="width:120px;min-width:120px">分類</th><th class="text-end" style="width:130px;min-width:130px">操作</th></tr></thead>
          <tbody>
            <tr v-if="filtered.length === 0"><td colspan="4" class="text-center text-muted py-4">尚無資料</td></tr>
            <tr v-for="item in paged" :key="item.id"><td>{{ item.id }}</td><td class="text-truncate" style="max-width:200px" :title="item.name">{{ item.name }}</td><td class="text-truncate" style="max-width:120px" :title="cropCategories.find ((c) => c.id === item.cropCategoryId)?.name ?? '—'">{{ cropCategories.find ((c) => c.id === item.cropCategoryId)?.name ?? '—' }}</td><td class="text-end"><button v-if="auth.isStaff" class="btn btn-sm btn-outline-primary me-1" @click="handleEdit (item)">編輯</button><button v-if="auth.isAdmin" class="btn btn-sm btn-outline-danger" @click="handleDelete (item)">刪除</button></td></tr>
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
      <nav aria-label="作物分頁">
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
