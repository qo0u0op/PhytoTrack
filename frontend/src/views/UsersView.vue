<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Swal from 'sweetalert2'
import { userApi } from '../api'

// 使用者資料（對應後端 UserResponse）
interface UserRow {
  userId: number
  username: string
  displayName: string
  email: string | null
  role: string
  active: boolean
}

const users = ref<UserRow[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const { data } = await userApi.list()
    users.value = data
  } catch {
    // 錯誤由攔截器處理
  } finally {
    loading.value = false
  }
}

onMounted(load)

const ROLE_OPTIONS = [
  { value: 'ROLE_VIEWER', label: '檢視者' },
  { value: 'ROLE_STAFF', label: '診斷員' },
  { value: 'ROLE_ADMIN', label: '管理者' },
]

async function changeRole(u: UserRow, newRole: string) {
  if (newRole === u.role) return
  try {
    await userApi.updateRole(u.userId, newRole)
    u.role = newRole
    Swal.fire({ icon: 'success', title: '角色已更新', timer: 1200, showConfirmButton: false })
  } catch {
    // 錯誤由攔截器處理
  }
}

async function toggleActive(u: UserRow) {
  const next = !u.active
  const result = await Swal.fire({
    icon: 'warning',
    title: next ? '啟用帳號？' : '停用帳號？',
    text: next ? `確定啟用 ${u.username}？` : `停用後 ${u.username} 將無法登入，既有 token 立即失效`,
    showCancelButton: true,
    confirmButtonText: next ? '啟用' : '停用',
    cancelButtonText: '取消',
  })
  if (!result.isConfirmed) return
  try {
    await userApi.updateActive(u.userId, next)
    u.active = next
    Swal.fire({ icon: 'success', title: next ? '已啟用' : '已停用', timer: 1200, showConfirmButton: false })
  } catch {
    // 錯誤由攔截器處理
  }
}

async function resetPassword(u: UserRow) {
  const { value: newPassword } = await Swal.fire({
    title: `重設密碼 — ${u.username}`,
    input: 'password',
    inputLabel: '新密碼（6–72 字元）',
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
    await userApi.resetPassword(u.userId, newPassword)
    Swal.fire({ icon: 'success', title: '密碼已重設', text: `${u.username} 可使用新密碼登入`, timer: 2000, showConfirmButton: false })
  } catch {
    // 錯誤由攔截器處理
  }
}
</script>

<template>
  <div class="container py-4">
    <h4 class="mb-4">使用者管理</h4>
    <div class="card shadow-sm">
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>ID</th>
              <th>帳號</th>
              <th>顯示名稱</th>
              <th>電子信箱</th>
              <th>角色</th>
              <th>狀態</th>
              <th class="text-end">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="text-center text-muted py-4">載入中…</td>
            </tr>
            <tr v-else-if="users.length === 0">
              <td colspan="7" class="text-center text-muted py-4">尚無使用者</td>
            </tr>
            <tr v-for="u in users" :key="u.userId">
              <td>{{ u.userId }}</td>
              <td>{{ u.username }}</td>
              <td>{{ u.displayName }}</td>
              <td>{{ u.email ?? '—' }}</td>
              <td>
                <select
                  :value="u.role"
                  class="form-select form-select-sm"
                  style="width: 130px"
                  @change="changeRole(u, ($event.target as HTMLSelectElement).value)"
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
                  @click="toggleActive(u)"
                >
                  {{ u.active ? '停用' : '啟用' }}
                </button>
                <button class="btn btn-sm btn-outline-primary" @click="resetPassword(u)">
                  重設密碼
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div class="form-text mt-2 text-muted small">
      提示：停用後該帳號無法登入，既有 token 立即失效；角色變更有於後續請求生效。
    </div>
  </div>
</template>
