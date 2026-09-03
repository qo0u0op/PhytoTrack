<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Swal from 'sweetalert2'
import { refApi, refAdminApi } from '../api'
import { useAuthStore } from '../stores/auth'

type Signer = { id: number; name: string; active?: boolean; userId?: number | null; username?: string | null }

const auth = useAuthStore ()

const signers = ref<Signer[]>([])
const filterQ = ref('')
const identityFilter = ref<'all' | 'user' | 'nonuser'>('all')
const showInactive = ref(false)
const loading = ref(true)

async function load () {
  loading.value = true
  try {
    const res = await refApi.identifiers(showInactive.value)
    signers.value = (res.data as Signer[]) ?? []
  } catch {} finally {
    loading.value = false
    page.value = 0
    pageInput.value = 1
  }
}
onMounted(load)
watch(showInactive, load)

const filtered = computed(() => {
  const q = filterQ.value.trim().toLowerCase()
  return signers.value.filter(s => {
    if (identityFilter.value === 'user' && !s.userId) return false
    if (identityFilter.value === 'nonuser' && s.userId) return false
    return !q || s.name.toLowerCase().includes(q)
  })
})

// 分頁（>20 顯示）
const page = ref(0)
const size = ref(10)
const sizeOptions = [10, 20, 50, 100]
const pageInput = ref(1)
const total = computed(() => filtered.value.length)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))
watch(page, (v) => { pageInput.value = v + 1 })
watch(filtered, () => { page.value = 0; pageInput.value = 1 })
function goToPage (p: number) {
  const c = Math.max(0, Math.min(p, totalPages.value - 1))
  if (c !== page.value) { page.value = c; pageInput.value = c + 1 } else pageInput.value = c + 1
}
function onSizeChange () { page.value = 0; pageInput.value = 1 }
function onPageInputConfirm () {
  let num = Number(pageInput.value)
  if (!Number.isFinite(num) || num < 1) num = 1
  if (num > totalPages.value) num = totalPages.value
  goToPage(num - 1)
}
const paged = computed(() => {
  const start = page.value * size.value
  return filtered.value.slice(start, start + size.value)
})

async function handleCreate () {
  const { value: name } = await Swal.fire({
    title: '新增簽名人',
    input: 'text',
    inputLabel: '名稱',
    showCancelButton: true,
    confirmButtonText: '新增',
    cancelButtonText: '取消',
    inputValidator: (v) => (!v?.trim() ? '名稱不可為空白' : null),
  })
  if (!name) return
  try {
    await refAdminApi.createIdentifier({ name: name.trim() })
    await load()
    Swal.fire({ icon: 'success', title: '已新增', timer: 1200, showConfirmButton: false })
  } catch (e: any) {
    const code = e?.response?.data?.error?.code
    if (code === 'DISPLAY_NAME_EXISTS') {
      Swal.fire({ icon: 'error', title: '顯示名稱已存在' })
    }
  }
}
async function handleEdit (item: Signer) {
  const { value: name } = await Swal.fire({
    title: '編輯',
    input: 'text',
    inputValue: item.name,
    showCancelButton: true,
    confirmButtonText: '儲存',
    cancelButtonText: '取消',
    inputValidator: (v) => (!v?.trim() ? '名稱不可為空白' : null),
  })
  if (!name || name.trim() === item.name) return
  try {
    await refAdminApi.updateIdentifier(item.id, { name: name.trim() })
    await load()
    Swal.fire({ icon: 'success', title: '已更新', timer: 1200, showConfirmButton: false })
  } catch (e: any) {
    const code = e?.response?.data?.error?.code
    if (code === 'DISPLAY_NAME_EXISTS') {
      Swal.fire({ icon: 'error', title: '顯示名稱已存在' })
    }
  }
}
async function handleToggle (item: Signer) {
  try {
    const active = !(item.active ?? true)
    await refAdminApi.updateIdentifierActive(item.id, active)
    await load()
    Swal.fire({ icon: 'success', title: active ? '已啟用' : '已停用', timer: 1200, showConfirmButton: false })
  } catch {}
}
</script>

<template>
  <div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h4 class="mb-0">簽名人管理</h4>
      <button class="btn btn-success btn-sm" @click="handleCreate">新增</button>
    </div>
    <div class="card shadow-sm mb-3">
      <div class="card-body py-2">
        <div class="row g-2 align-items-center">
          <div class="col-md-3">
            <input v-model="filterQ" type="text" class="form-control form-control-sm" placeholder="篩選名稱" />
          </div>
          <div class="col-md-3">
            <select v-model="identityFilter" class="form-select form-select-sm">
              <option value="all">全部身分別</option>
              <option value="user">使用者</option>
              <option value="nonuser">非使用者</option>
            </select>
          </div>
          <div class="col-auto">
            <div class="form-check">
              <input class="form-check-input" type="checkbox" id="showInactive" v-model="showInactive" />
              <label class="form-check-label small" for="showInactive">顯示已停用</label>
            </div>
          </div>
          <div class="col-md-3 text-muted small">{{ filtered.length }} 筆</div>
        </div>
      </div>
    </div>
    <div v-if="loading" class="text-center text-muted py-4">載入中…</div>
    <div v-else class="card shadow-sm">
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th style="width:60px">ID</th>
              <th>名稱</th>
              <th>帳號</th>
              <th>身分別</th>
              <th>狀態</th>
              <th class="text-end">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filtered.length===0"><td colspan="6" class="text-center text-muted py-4">尚無資料</td></tr>
            <tr v-for="item in paged" :key="item.id">
              <td>{{ item.id }}</td>
              <td>{{ item.name }}</td>
              <td class="text-muted small">{{ item.username ?? '—' }}</td>
              <td><span class="badge" :class="item.userId ? 'bg-primary' : 'bg-secondary'">{{ item.userId ? '使用者' : '非使用者' }}</span></td>
              <td><span class="badge" :class="item.active===false ? 'text-bg-secondary' : 'text-bg-success'">{{ item.active===false ? '停用' : '啟用' }}</span></td>
              <td class="text-end">
                <button class="btn btn-sm btn-outline-primary me-1" @click="handleEdit(item)">編輯</button>
                <button v-if="auth.isAdmin" class="btn btn-sm me-1" :class="item.active===false ? 'btn-outline-success' : 'btn-outline-warning'" @click="handleToggle(item)">{{ item.active===false ? '啟用' : '停用' }}</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div v-if="total > 20" class="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3">
      <div class="d-flex align-items-center gap-2">
        <label class="form-label small text-muted mb-0 text-nowrap">每頁筆數</label>
        <select v-model.number="size" class="form-select form-select-sm" style="width:auto" @change="onSizeChange">
          <option v-for="opt in sizeOptions" :key="opt" :value="opt">{{ opt }} 筆/頁</option>
        </select>
        <span class="small text-muted text-nowrap">共 {{ total }} 筆，{{ totalPages }} 頁</span>
      </div>
      <nav aria-label="簽名人分頁">
        <ul class="pagination pagination-sm mb-0 justify-content-center">
          <li class="page-item" :class="{ disabled: page === 0 }"><button class="page-link border-0 text-secondary" :disabled="page === 0" @click="goToPage(0)" style="height:31px">&lt;&lt;</button></li>
          <li class="page-item" :class="{ disabled: page === 0 }"><button class="page-link border-0 text-secondary" :disabled="page === 0" @click="goToPage(page - 1)" style="height:31px">&lt;</button></li>
          <li class="page-item"><span class="page-link border-0 text-secondary d-flex align-items-center gap-1" style="height:31px;padding:0 0.5rem"><input v-model.number="pageInput" type="number" class="form-control form-control-sm text-center p-0" style="width:64px;height:24px;font-size:0.875rem;line-height:1.5" :min="1" :max="totalPages" @keyup.enter="onPageInputConfirm" @blur="onPageInputConfirm" /> / {{ totalPages }}</span></li>
          <li class="page-item" :class="{ disabled: page >= totalPages - 1 }"><button class="page-link border-0 text-secondary" :disabled="page >= totalPages - 1" @click="goToPage(page + 1)" style="height:31px">&gt;</button></li>
          <li class="page-item" :class="{ disabled: page >= totalPages - 1 }"><button class="page-link border-0 text-secondary" :disabled="page >= totalPages - 1" @click="goToPage(totalPages - 1)" style="height:31px">&gt;&gt;</button></li>
        </ul>
      </nav>
    </div>
  </div>
</template>
