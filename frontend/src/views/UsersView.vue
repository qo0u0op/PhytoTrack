<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Swal from 'sweetalert2'
import { userApi, accountApi, refAdminApi } from '../api'

// 使用者資料 (對應後端 UserResponse)
interface UserRow {
  userId: number
  username: string
  displayName: string
  email: string | null
  role: string
  active: boolean
}

const users = ref<UserRow[]>([])
const loading = ref (true)
const deactivateRequests = ref<any[]>([])

// 搜尋與停用顯示（前端本地過濾，疊加於分頁前；預設僅列啟用者）
const searchQ = ref ('')
const showInactive = ref (false)
const filteredUsers = computed (() => {
  const q = searchQ.value.trim ().toLowerCase ()
  return users.value.filter ((u) => {
    if (!showInactive.value && !u.active) return false
    if (!q) return true
    return u.username.toLowerCase ().includes (q)
      || u.displayName.toLowerCase ().includes (q)
      || (u.email ?? '').toLowerCase ().includes (q)
  })
})

// 分頁 - 使用者
const page = ref (0)
const size = ref (10)
const sizeOptions = [10, 20, 50, 100]
const pageInput = ref (1)
const total = computed (() => filteredUsers.value.length)
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
const pagedUsers = computed (() => filteredUsers.value.slice (page.value * size.value, page.value * size.value + size.value))
watch ([searchQ, showInactive], () => { page.value = 0; pageInput.value = 1 })

// 分頁 - 停用請求
const reqPage = ref (0)
const reqSize = ref (10)
const reqPageInput = ref (1)
const reqTotal = computed (() => deactivateRequests.value.length)
const reqTotalPages = computed (() => Math.max (1, Math.ceil (reqTotal.value / reqSize.value)))
watch (reqPage, (v) => { reqPageInput.value = v + 1 })
function goToReqPage (p: number) {
  const c = Math.max (0, Math.min (p, reqTotalPages.value - 1))
  if (c !== reqPage.value) { reqPage.value = c; reqPageInput.value = c + 1 } else reqPageInput.value = c + 1
}
function onReqSizeChange () { reqPage.value = 0; reqPageInput.value = 1 }
function onReqPageInputConfirm () {
  let num = Number (reqPageInput.value)
  if (!Number.isFinite (num) || num < 1) num = 1
  if (num > reqTotalPages.value) num = reqTotalPages.value
  goToReqPage (num - 1)
}
const pagedRequests = computed (() => deactivateRequests.value.slice (reqPage.value * reqSize.value, reqPage.value * reqSize.value + reqSize.value))

async function load () {
  loading.value = true
  try {
    const { data } = await userApi.list ()
    users.value = data
  } catch {
    // 錯誤由攔截器處理
  } finally {
    loading.value = false
  }
}

async function loadDeactivateRequests () {
  try {
    const { data } = await accountApi.listDeactivateRequests ()
    deactivateRequests.value = data as any[]
  } catch {}
}

onMounted (async () => {
  await load ()
  await loadDeactivateRequests ()
})

async function reviewDeactivate (id: number, status: string) {
  const result = await Swal.fire ({ icon: 'warning', title: `確定${status === 'APPROVED' ? '通過' : '拒絕'}此停用請求？`, showCancelButton: true, confirmButtonText: '確定', cancelButtonText: '取消' })
  if (!result.isConfirmed) return
  try {
    await accountApi.reviewDeactivateRequest (id, status)
    Swal.fire ({ icon: 'success', title: '已處理', timer: 1200, showConfirmButton: false })
    await loadDeactivateRequests ()
    await load ()
  } catch (e: any) {
    const msg = e?.response?.data?.message || '處理失敗'
    Swal.fire ({ icon: 'error', title: msg })
  }
}

const ROLE_OPTIONS = [
  { value: 'ROLE_VIEWER', label: '檢視者' },
  { value: 'ROLE_STAFF', label: '診斷員' },
  { value: 'ROLE_ADMIN', label: '管理者' },
]

async function changeRole (u: UserRow, newRole: string) {
  if (newRole === u.role) return
  const result = await Swal.fire ({
    icon: 'question',
    title: '變更角色？',
    text: `確定將 ${u.username} 的角色從 ${ROLE_OPTIONS.find ((o) => o.value === u.role)?.label} 變更為 ${ROLE_OPTIONS.find ((o) => o.value === newRole)?.label}？`,
    showCancelButton: true,
    confirmButtonText: '確認變更',
    cancelButtonText: '取消',
  })
  if (!result.isConfirmed) {
    // 還原下拉顯示 (避免停留在未確認的新值)
    const sel = document.querySelector (`select[data-user-id="${u.userId}"]`) as HTMLSelectElement | null
    if (sel) sel.value = u.role
    return
  }
  try {
    await userApi.updateRole (u.userId, newRole)
    u.role = newRole
    Swal.fire ({ icon: 'success', title: '角色已更新', timer: 1200, showConfirmButton: false })
  } catch (e: any) {
    const err = e?.response?.data
    const code = err?.error?.code ?? err?.code
    if (code === 'SIGNER_NAME_CONFLICT') {
      const existingId = err?.error?.details?.existingIdentifierId ?? err?.details?.existingIdentifierId
      const displayName = err?.error?.details?.displayName ?? u.displayName
      const res2 = await Swal.fire ({
        icon: 'question',
        title: '名稱與既有簽名人重名',
        text: `簽名人「${displayName}」已存在（ID ${existingId}），是否綁定至 ${u.username}？`,
        showCancelButton: true,
        confirmButtonText: '綁定',
        cancelButtonText: '新建',
      })
      if (res2.isConfirmed && existingId) {
        try {
          await refAdminApi.bindIdentifier (existingId, u.userId)
          await userApi.updateRole (u.userId, newRole, { force: true })
          u.role = newRole
          Swal.fire ({ icon: 'success', title: '已綁定並更新角色', timer: 1500, showConfirmButton: false })
          return
        } catch {}
      } else {
        try {
          await userApi.updateRole (u.userId, newRole, { force: true })
          u.role = newRole
          Swal.fire ({ icon: 'success', title: '角色已更新（已新建簽名人）', timer: 1500, showConfirmButton: false })
          return
        } catch {}
      }
    }
    // 失敗時還原下拉
    const sel = document.querySelector (`select[data-user-id="${u.userId}"]`) as HTMLSelectElement | null
    if (sel) sel.value = u.role
  }
}

async function toggleActive (u: UserRow) {
  const next = !u.active
  const result = await Swal.fire ({
    icon: 'warning',
    title: next ? '啟用帳號？' : '停用帳號？',
    text: next ? `確定啟用 ${u.username}？` : `停用後 ${u.username} 將無法登入，既有 token 立即失效`,
    showCancelButton: true,
    confirmButtonText: next ? '啟用' : '停用',
    cancelButtonText: '取消',
  })
  if (!result.isConfirmed) return
  try {
    await userApi.updateActive (u.userId, next)
    u.active = next
    Swal.fire ({ icon: 'success', title: next ? '已啟用' : '已停用', timer: 1200, showConfirmButton: false })
  } catch {
    // 錯誤由攔截器處理
  }
}

async function resetPassword (u: UserRow) {
  const { value: newPassword } = await Swal.fire ({
    title: `重設密碼 — ${u.username}`,
    input: 'password',
    inputLabel: '新密碼 (6–72 字元)',
    inputPlaceholder: '請輸入新密碼',
    showCancelButton: true,
    confirmButtonText: '重設',
    cancelButtonText: '取消',
    inputValidator: (v) => {
      if (!v || v.length < 6) return '密碼至少 6 字元'
      if (v.length > 72) return '密碼不可超過 72 字元'
      return null
    },
  })
  if (!newPassword) return
  try {
    await userApi.resetPassword (u.userId, newPassword)
    Swal.fire ({ icon: 'success', title: '密碼已重設', text: `${u.username} 可使用新密碼登入`, timer: 2000, showConfirmButton: false })
  } catch {
    // 錯誤由攔截器處理
  }
}
</script>

<template>
  <div class="container py-4">
    <h4 class="mb-4">使用者管理</h4>

    <!-- 搜尋與停用顯示 -->
    <div class="card shadow-sm mb-3">
      <div class="card-body py-2">
        <div class="row g-2 align-items-center">
          <div class="col-md-4">
            <input v-model="searchQ" type="text" class="form-control form-control-sm" placeholder="搜尋帳號 / 顯示名稱 / 信箱" />
          </div>
          <div class="col-auto">
            <div class="form-check">
              <input class="form-check-input" type="checkbox" id="showInactiveUsers" v-model="showInactive" />
              <label class="form-check-label small" for="showInactiveUsers">顯示已停用</label>
            </div>
          </div>
          <div class="col-md-4 text-muted small">{{ total }} 筆</div>
        </div>
      </div>
    </div>

    <!-- 分頁（使用者，>20 才顯示） -->
    <div v-if="total > 20" class="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3 mb-3">
      <div class="d-flex align-items-center gap-2">
        <label class="form-label small text-muted mb-0 text-nowrap">每頁筆數</label>
        <select v-model.number="size" class="form-select form-select-sm" style="width:auto" @change="onSizeChange">
          <option v-for="opt in sizeOptions" :key="opt" :value="opt">{{ opt }} 筆/頁</option>
        </select>
        <span class="small text-muted text-nowrap">共 {{ total }} 筆，{{ totalPages }} 頁</span>
      </div>
      <nav aria-label="使用者分頁">
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
        <table class="table table-hover align-middle mb-0 text-nowrap" style="min-width:600px;table-layout:fixed">
          <thead class="table-light">
            <tr>
              <th style="width:50px;min-width:50px">ID</th>
              <th style="width:90px;min-width:90px">帳號</th>
              <th style="width:90px;min-width:90px">顯示名稱</th>
              <th style="width:120px;min-width:120px">電子信箱</th>
              <th style="width:70px;min-width:70px">角色</th>
              <th style="width:70px;min-width:70px">狀態</th>
              <th class="text-end" style="width:110px;min-width:110px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="text-center text-muted py-4">載入中…</td>
            </tr>
            <tr v-else-if="pagedUsers.length === 0">
              <td colspan="7" class="text-center text-muted py-4">尚無使用者</td>
            </tr>
            <tr v-for="u in pagedUsers" :key="u.userId">
              <td>{{ u.userId }}</td>
              <td class="text-truncate" style="max-width:90px" :title="u.username">{{ u.username }}</td>
              <td class="text-truncate" style="max-width:90px" :title="u.displayName">{{ u.displayName }}</td>
              <td class="text-truncate" style="max-width:120px" :title="u.email ?? '—'">{{ u.email ?? '—' }}</td>
              <td>
                <select
                  :value="u.role"
                  :data-user-id="u.userId"
                  class="form-select form-select-sm"
                  style="width: 130px"
                  @change="changeRole (u, ($event.target as HTMLSelectElement).value)"
                >
                  <option v-for="opt in ROLE_OPTIONS" :key="opt.value" :value="opt.value">
                    {{ opt.label }}
                  </option>
                </select>
              </td>
              <td>
                <span class="badge" :class="u.active ? 'text-bg-success' : 'text-bg-secondary'">
                  {{ u.active ? '啟用' : '停用' }}
                </span>
              </td>
              <td class="text-end">
                <button
                  class="btn btn-sm me-1"
                  :class="u.active ? 'btn-outline-warning' : 'btn-outline-success'"
                  @click="toggleActive (u)"
                >
                  {{ u.active ? '停用' : '啟用' }}
                </button>
                <button class="btn btn-sm btn-outline-primary" @click="resetPassword (u)">
                  重設密碼
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <!-- 分頁（使用者下方，>20 才顯示） -->
    <div v-if="total > 20" class="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3 mb-3">
      <div class="d-flex align-items-center gap-2">
        <label class="form-label small text-muted mb-0 text-nowrap">每頁筆數</label>
        <select v-model.number="size" class="form-select form-select-sm" style="width:auto" @change="onSizeChange">
          <option v-for="opt in sizeOptions" :key="opt" :value="opt">{{ opt }} 筆/頁</option>
        </select>
        <span class="small text-muted text-nowrap">共 {{ total }} 筆，{{ totalPages }} 頁</span>
      </div>
      <nav aria-label="使用者分頁">
        <ul class="pagination pagination-sm mb-0 justify-content-center">
          <li class="page-item" :class="{ disabled: page === 0 }"><button class="page-link border-0 text-secondary" :disabled="page === 0" @click="goToPage(0)" style="height:31px"><<</button></li>
          <li class="page-item" :class="{ disabled: page === 0 }"><button class="page-link border-0 text-secondary" :disabled="page === 0" @click="goToPage(page - 1)" style="height:31px"><</button></li>
          <li class="page-item"><span class="page-link border-0 text-secondary d-flex align-items-center gap-1" style="height:31px;padding:0 0.5rem"><input v-model.number="pageInput" type="number" class="form-control form-control-sm text-center p-0" style="width:64px;height:24px;font-size:0.875rem;line-height:1.5" :min="1" :max="totalPages" @keyup.enter="onPageInputConfirm" @blur="onPageInputConfirm" /> / {{ totalPages }}</span></li>
          <li class="page-item" :class="{ disabled: page >= totalPages - 1 }"><button class="page-link border-0 text-secondary" :disabled="page >= totalPages - 1" @click="goToPage(page + 1)" style="height:31px">></button></li>
          <li class="page-item" :class="{ disabled: page >= totalPages - 1 }"><button class="page-link border-0 text-secondary" :disabled="page >= totalPages - 1" @click="goToPage(totalPages - 1)" style="height:31px">>></button></li>
        </ul>
      </nav>
    </div>

    <div class="card shadow-sm mt-4">
      <div class="card-header bg-warning text-dark">停用請求審核</div>
      <div class="card-body">
        <div v-if="deactivateRequests.length === 0" class="text-muted small">尚無請求</div>
        <div v-else class="table-responsive">
          <table class="table table-sm align-middle mb-0">
            <thead><tr><th>ID</th><th>帳號</th><th>狀態</th><th>建立時間</th><th class="text-end">操作</th></tr></thead>
            <tbody>
              <tr v-for="r in pagedRequests" :key="r.requestId">
                <td>{{ r.requestId }}</td>
                <td>{{ r.username }}</td>
                <td><span class="badge" :class="r.status === 'PENDING' ? 'bg-warning text-dark' : r.status === 'APPROVED' ? 'bg-danger' : 'bg-secondary'">{{ r.status }}</span></td>
                <td class="small text-muted">{{ r.createdAt }}</td>
                <td class="text-end">
                  <template v-if="r.status === 'PENDING'">
                    <button class="btn btn-sm btn-success me-1" @click="reviewDeactivate(r.requestId, 'APPROVED')">通過</button>
                    <button class="btn btn-sm btn-outline-secondary" @click="reviewDeactivate(r.requestId, 'REJECTED')">拒絕</button>
                  </template>
                  <span v-else class="small text-muted">{{ r.reviewedBy ? '審核者: ' + r.reviewedBy : '' }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <!-- 分頁（停用請求，>20 才顯示） -->
        <div v-if="reqTotal > 20" class="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3">
          <div class="d-flex align-items-center gap-2">
            <label class="form-label small text-muted mb-0 text-nowrap">每頁筆數</label>
            <select v-model.number="reqSize" class="form-select form-select-sm" style="width:auto" @change="onReqSizeChange">
              <option v-for="opt in sizeOptions" :key="opt" :value="opt">{{ opt }} 筆/頁</option>
            </select>
            <span class="small text-muted text-nowrap">共 {{ reqTotal }} 筆，{{ reqTotalPages }} 頁</span>
          </div>
          <nav aria-label="停用請求分頁">
            <ul class="pagination pagination-sm mb-0 justify-content-center">
              <li class="page-item" :class="{ disabled: reqPage === 0 }"><button class="page-link border-0 text-secondary" :disabled="reqPage === 0" @click="goToReqPage(0)" style="height:31px"><<</button></li>
              <li class="page-item" :class="{ disabled: reqPage === 0 }"><button class="page-link border-0 text-secondary" :disabled="reqPage === 0" @click="goToReqPage(reqPage - 1)" style="height:31px"><</button></li>
              <li class="page-item"><span class="page-link border-0 text-secondary d-flex align-items-center gap-1" style="height:31px;padding:0 0.5rem"><input v-model.number="reqPageInput" type="number" class="form-control form-control-sm text-center p-0" style="width:64px;height:24px;font-size:0.875rem;line-height:1.5" :min="1" :max="reqTotalPages" @keyup.enter="onReqPageInputConfirm" @blur="onReqPageInputConfirm" /> / {{ reqTotalPages }}</span></li>
              <li class="page-item" :class="{ disabled: reqPage >= reqTotalPages - 1 }"><button class="page-link border-0 text-secondary" :disabled="reqPage >= reqTotalPages - 1" @click="goToReqPage(reqPage + 1)" style="height:31px">></button></li>
              <li class="page-item" :class="{ disabled: reqPage >= reqTotalPages - 1 }"><button class="page-link border-0 text-secondary" :disabled="reqPage >= reqTotalPages - 1" @click="goToReqPage(reqTotalPages - 1)" style="height:31px">>></button></li>
            </ul>
          </nav>
        </div>
      </div>
    </div>
    <div class="form-text mt-2 text-muted small">
      提示：停用後該帳號無法登入，既有 token 立即失效；角色變更有於後續請求生效。
    </div>
  </div>
</template>
