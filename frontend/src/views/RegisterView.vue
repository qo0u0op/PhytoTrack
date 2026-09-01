<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import Swal from 'sweetalert2'
import { authApi } from '../api'

const router = useRouter ()

const form = reactive ({
  username: '',
  displayName: '',
  password: '',
  confirmPassword: '',
  email: '',
})
const loading = ref (false)

async function submit () {
  // 前端先做一次確認密碼檢查 (後端仍會做正式驗證)
  if (form.password !== form.confirmPassword) {
    Swal.fire ({ icon: 'warning', title: '密碼不一致', text: '請確認兩次輸入的密碼相同' })
    return
  }
  loading.value = true
  try {
    await authApi.register ({
      username: form.username,
      displayName: form.displayName,
      password: form.password,
      email: form.email || undefined,
    })
    Swal.fire ({ icon: 'success', title: '註冊成功', text: '請使用新帳號登入' }).then (() => {
      router.push ('/login')
    })
  } catch {
    // 錯誤訊息由攔截器統一彈出
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="container py-5" style="max-width: 460px">
    <div class="card shadow-sm">
      <div class="card-body p-4">
        <h4 class="card-title mb-4 text-center">註冊帳號</h4>
        <form @submit.prevent="submit">
          <div class="mb-3">
            <label class="form-label">帳號</label>
            <input v-model.trim="form.username" type="text" class="form-control" required minlength="3" />
          </div>
          <div class="mb-3">
            <label class="form-label">顯示名稱</label>
            <input v-model.trim="form.displayName" type="text" class="form-control" required />
          </div>
          <div class="mb-3">
            <label class="form-label">電子信箱 (選填)</label>
            <input v-model.trim="form.email" type="email" class="form-control" />
          </div>
          <div class="mb-3">
            <label class="form-label">密碼</label>
            <input v-model="form.password" type="password" class="form-control" required minlength="6" />
          </div>
          <div class="mb-3">
            <label class="form-label">確認密碼</label>
            <input v-model="form.confirmPassword" type="password" class="form-control" required />
          </div>
          <button class="btn btn-success w-100" :disabled="loading">
            {{ loading ? '註冊中…' : '註冊' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>
