<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { userApi } from '../api'

// 使用者資料（對應後端 UserResponse）
interface UserRow {
  userId: number
  username: string
  displayName: string
  email: string | null
  role: string
}

const users = ref<UserRow[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const { data } = await userApi.list()
    users.value = data
  } catch {
    // 錯誤由攔截器處理
  } finally {
    loading.value = false
  }
})

// 角色顯示名稱
function roleLabel(role: string) {
  const map: Record<string, string> = {
    ROLE_ADMIN: '管理者',
    ROLE_STAFF: '診斷員',
    ROLE_VIEWER: '檢視者',
  }
  return map[role] ?? role
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
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="5" class="text-center text-muted py-4">載入中…</td>
            </tr>
            <tr v-for="u in users" :key="u.userId">
              <td>{{ u.userId }}</td>
              <td>{{ u.username }}</td>
              <td>{{ u.displayName }}</td>
              <td>{{ u.email ?? '—' }}</td>
              <td>
                <span
                  class="badge"
                  :class="{
                    'text-bg-danger': u.role === 'ROLE_ADMIN',
                    'text-bg-primary': u.role === 'ROLE_STAFF',
                    'text-bg-secondary': u.role === 'ROLE_VIEWER',
                  }"
                >
                  {{ roleLabel(u.role) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>