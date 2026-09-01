<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Swal from 'sweetalert2'
import { refApi, refAdminApi } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore ()

function escapeHtml (s: string) {
  return s.replace (/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]!))
}

const loading = ref (true)
const filterQ = ref ('')
const filterPestTypeId = ref<number | null>(null)
const pestCategories = ref<{ id: number; code: string; name: string; pestTypeId: number; sortOrder: number }[]>([])
const pestTypes = ref<{ id: number; name: string }[]>([])

async function loadAll () {
  loading.value = true
  try {
    const pestTypeRes = await refApi.pestTypes ()
    const pts = pestTypeRes.data as { id: number; name: string; categories: { id: number; code: string; name: string; sortOrder: number }[] }[]
    pestTypes.value = pts.map ((p) => ({ id: p.id, name: p.name }))
    pestCategories.value = pts.flatMap ((p) => p.categories.map ((cat) => ({ id: cat.id, code: cat.code, name: cat.name, pestTypeId: p.id, sortOrder: cat.sortOrder })))
  } catch {} finally { loading.value = false }
}

onMounted (loadAll)

const filtered = computed (() => {
  const q = filterQ.value.trim ().toLowerCase ()
  const typeId = filterPestTypeId.value
  return pestCategories.value.filter ((p) => (!typeId || p.pestTypeId === typeId) && (!q || p.name.toLowerCase ().includes (q) || p.code.toLowerCase ().includes (q)))
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
watch ([filterQ, filterPestTypeId], () => { page.value = 0; pageInput.value = 1 })
const paged = computed (() => filtered.value.slice (page.value * size.value, page.value * size.value + size.value))

async function handleCreate () {
  if (pestTypes.value.length === 0) { Swal.fire ({ icon: 'warning', title: '無害物類型可選' }); return }
  const { value: form } = await Swal.fire ({
    title: '新增病蟲害分類',
    html: `<input id="swal-pc-code" class="swal2-input" placeholder="代碼" /><input id="swal-pc-name" class="swal2-input" placeholder="名稱" /><select id="swal-pc-type" class="swal2-select">${pestTypes.value.map ((p) => `<option value="${p.id}">${escapeHtml (p.name)}</option>`).join ('')}</select><input id="swal-pc-order" class="swal2-input" placeholder="排序" type="number" value="0" />`,
    showCancelButton: true,
    confirmButtonText: '新增',
    cancelButtonText: '取消',
    preConfirm: () => {
      const code = (document.getElementById ('swal-pc-code') as HTMLInputElement).value.trim ()
      const name = (document.getElementById ('swal-pc-name') as HTMLInputElement).value.trim ()
      const pestTypeId = Number ((document.getElementById ('swal-pc-type') as HTMLSelectElement).value)
      const sortOrder = Number ((document.getElementById ('swal-pc-order') as HTMLInputElement).value)
      if (!code) return Swal.showValidationMessage ('代碼不可為空白')
      if (!name) return Swal.showValidationMessage ('名稱不可為空白')
      if (!Number.isFinite (sortOrder) || sortOrder < 0) return Swal.showValidationMessage ('排序不可為負')
      return { code, name, pestTypeId, sortOrder }
    },
  })
  if (!form) return
  try { await refAdminApi.createPestCategory (form); await loadAll (); Swal.fire ({ icon: 'success', title: '已新增', timer: 1200, showConfirmButton: false }) } catch {}
}

async function handleEdit (item: any) {
  const { value: form } = await Swal.fire ({
    title: '編輯病蟲害分類',
    html: `<input id="swal-pc-code" class="swal2-input" value="${escapeHtml (item.code)}" /><input id="swal-pc-name" class="swal2-input" value="${escapeHtml (item.name)}" /><select id="swal-pc-type" class="swal2-select">${pestTypes.value.map ((p) => `<option value="${p.id}" ${p.id === item.pestTypeId ? 'selected' : ''}>${escapeHtml (p.name)}</option>`).join ('')}</select><input id="swal-pc-order" class="swal2-input" type="number" value="${item.sortOrder}" />`,
    showCancelButton: true,
    confirmButtonText: '儲存',
    cancelButtonText: '取消',
    preConfirm: () => {
      const code = (document.getElementById ('swal-pc-code') as HTMLInputElement).value.trim ()
      const name = (document.getElementById ('swal-pc-name') as HTMLInputElement).value.trim ()
      const pestTypeId = Number ((document.getElementById ('swal-pc-type') as HTMLSelectElement).value)
      const sortOrder = Number ((document.getElementById ('swal-pc-order') as HTMLInputElement).value)
      if (!code) return Swal.showValidationMessage ('代碼不可為空白')
      if (!name) return Swal.showValidationMessage ('名稱不可為空白')
      if (!Number.isFinite (sortOrder) || sortOrder < 0) return Swal.showValidationMessage ('排序不可為負')
      return { code, name, pestTypeId, sortOrder }
    },
  })
  if (!form) return
  try { await refAdminApi.updatePestCategory (item.id, form); await loadAll (); Swal.fire ({ icon: 'success', title: '已更新', timer: 1200, showConfirmButton: false }) } catch {}
}

async function handleDelete (item: any) {
  const result = await Swal.fire ({ icon: 'warning', title: `確定刪除「${item.name}」？`, text: '此操作無法復原，若已被案件引用將被拒絕', showCancelButton: true, confirmButtonText: '刪除', cancelButtonText: '取消' })
  if (!result.isConfirmed) return
  try { await refAdminApi.deletePestCategory (item.id); await loadAll (); Swal.fire ({ icon: 'success', title: '已刪除', timer: 1200, showConfirmButton: false }) } catch {}
}
</script>

<template>
  <div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h4 class="mb-0">害物管理</h4>
      <button v-if="auth.isAdmin" class="btn btn-success" @click="handleCreate">新增</button>
    </div>
    <div class="card shadow-sm mb-3">
      <div class="card-body py-2">
        <div class="row g-2 align-items-center">
          <div class="col-md-4"><input v-model="filterQ" type="text" class="form-control form-control-sm" placeholder="篩選名稱／代碼" /></div>
          <div class="col-md-4"><select v-model.number="filterPestTypeId" class="form-select form-select-sm"><option :value="null">全部類型</option><option v-for="p in pestTypes" :key="p.id" :value="p.id">{{ p.name }}</option></select></div>
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
      <nav aria-label="害物分頁">
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
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light"><tr><th>ID</th><th>代碼</th><th>名稱</th><th>類型</th><th>排序</th><th class="text-end">操作</th></tr></thead>
          <tbody>
            <tr v-if="filtered.length === 0"><td colspan="6" class="text-center text-muted py-4">尚無資料</td></tr>
            <tr v-for="item in paged" :key="item.id"><td>{{ item.id }}</td><td>{{ item.code }}</td><td>{{ item.name }}</td><td>{{ pestTypes.find ((p) => p.id === item.pestTypeId)?.name ?? '—' }}</td><td>{{ item.sortOrder }}</td><td class="text-end"><button class="btn btn-sm btn-outline-primary me-1" @click="handleEdit (item)">編輯</button><button v-if="auth.isAdmin" class="btn btn-sm btn-outline-danger" @click="handleDelete (item)">刪除</button></td></tr>
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
      <nav aria-label="害物分頁">
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
