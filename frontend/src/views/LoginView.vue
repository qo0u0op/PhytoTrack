<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Swal from 'sweetalert2'
import { authApi } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore ()
const router = useRouter ()
const route = useRoute ()

const form = reactive ({ username: '', password: '' })
const loading = ref (false)

async function submit () {
  loading.value = true
  try {
    const { data } = await authApi.login ({ ...form })
    // 登入成功：寫入狀態並跳回原本想去的頁面 (或儀表板)
    auth.setAuth (data.token!, data.user!)
    Swal.fire ({ icon: 'success', title: '登入成功', timer: 1200, showConfirmButton: false })
    router.push (String (route.query.redirect ?? '/dashboard'))
  } catch (e: any) {
    const code = e.response?.data?.code || e.response?.data?.error?.code
    const msg = e.response?.data?.message || e.response?.data?.error?.message
    if (code === 'DEACTIVATE_PENDING') {
      const result = await Swal.fire ({
        icon: 'warning',
        title: '有待審核的停用申請',
        text: '是否放棄申請停用並繼續登入？',
        showCancelButton: true,
        confirmButtonText: '是，放棄申請',
        cancelButtonText: '否，禁止登入',
      })
      if (result.isConfirmed) {
        try {
          await authApi.abandonDeactivate ({ ...form })
          const { data } = await authApi.login ({ ...form })
          auth.setAuth (data.token!, data.user!)
          Swal.fire ({ icon: 'success', title: '已放棄停用申請並登入', timer: 1200, showConfirmButton: false })
          router.push (String (route.query.redirect ?? '/dashboard'))
          return
        } catch (e2: any) {
          Swal.fire ({ icon: 'error', title: '放棄失敗', text: e2.response?.data?.message ?? '請稍後再試' })
          return
        }
      } else {
        Swal.fire ({ icon: 'info', title: '已禁止登入', text: '如需放棄請再次登入並選擇是' })
        return
      }
    }
    // 401 等錯誤由攔截器處理；在此補充顯示登入失敗訊息
    Swal.fire ({ icon: 'error', title: '登入失敗', text: msg ?? '帳號或密碼錯誤' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="container py-5" style="max-width: 420px">
    <div class="card shadow-sm">
      <div class="card-body p-4">
        <h4 class="card-title mb-4 text-center">登入</h4>
        <form @submit.prevent="submit">
          <div class="mb-3">
            <label class="form-label">帳號</label>
            <input v-model.trim="form.username" type="text" class="form-control" required />
          </div>
          <div class="mb-3">
            <label class="form-label">密碼</label>
            <input v-model="form.password" type="password" class="form-control" required />
          </div>
          <button class="btn btn-success w-100" :disabled="loading">
            {{ loading ? '登入中…' : '登入' }}
          </button>
        </form>
        <p class="text-center mt-3 mb-0 small">
          還沒有帳號？
          <router-link to="/register">立即註冊</router-link>
        </p>
      </div>
    </div>
  </div>
</template>
