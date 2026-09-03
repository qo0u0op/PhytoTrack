<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import Swal from 'sweetalert2'
import { authApi, EMAIL_PATTERN } from '../api'

const router = useRouter ()

const form = reactive ({
  username: '',
  displayName: '',
  password: '',
  confirmPassword: '',
  email: '',
})
// 各欄位 inline 錯誤（取代全域 alert）
const usernameErr = ref ('')
const displayNameErr = ref ('')
const emailErr = ref ('')
const passwordErr = ref ('')
const confirmErr = ref ('')
const loading = ref (false)
// 密碼可見切換（預設隱藏）
const showPassword = ref (false)
const showConfirm = ref (false)

// debounce 可用性檢查（失焦/輸入停止約 500ms 後呼叫）
let usernameTimer: ReturnType<typeof setTimeout> | null = null
let emailTimer: ReturnType<typeof setTimeout> | null = null

async function checkUsernameNow () {
  const v = form.username.trim ()
  if (!v) {
    usernameErr.value = '帳號不可為空白'
    return
  }
  if (v.length < 3) {
    usernameErr.value = '帳號長度需至少 3 字元'
    return
  }
  try {
    const { data } = await authApi.checkUsername (v)
    usernameErr.value = (data as any).available ? '' : '此帳號已被使用'
  } catch {
    // 檢查失敗不擋輸入，送出時後端仍會驗證
  }
}

function scheduleUsernameCheck () {
  if (usernameTimer) clearTimeout (usernameTimer)
  usernameTimer = setTimeout (checkUsernameNow, 500)
}

async function checkEmailNow () {
  const v = form.email.trim ()
  if (!v) {
    emailErr.value = ''
    return
  }
  if (!EMAIL_PATTERN.test (v)) {
    emailErr.value = '電子信箱格式不正確'
    return
  }
  try {
    const { data } = await authApi.checkEmail (v)
    emailErr.value = (data as any).available ? '' : '此信箱已被使用'
  } catch {
    // 檢查失敗不擋輸入，送出時後端仍會驗證
  }
}

function scheduleEmailCheck () {
  if (emailTimer) clearTimeout (emailTimer)
  emailTimer = setTimeout (checkEmailNow, 500)
}

// 密碼一致即時比對
watch ([() => form.password, () => form.confirmPassword], () => {
  if (!form.confirmPassword) {
    confirmErr.value = ''
    return
  }
  confirmErr.value = form.password === form.confirmPassword ? '' : '兩次輸入的密碼不一致'
})

/** 後端錯誤映射到欄位；未知錯誤重新拋出維持攔截器行為 */
function mapServerError (e: any): boolean {
  const err = e?.response?.data?.error
  const code = err?.code as string | undefined
  if (code === 'USERNAME_TAKEN') {
    usernameErr.value = '此帳號已被使用'
    return true
  }
  if (code === 'EMAIL_TAKEN') {
    emailErr.value = '此信箱已被使用'
    return true
  }
  if (code === 'VALIDATION_ERROR' && err?.details && typeof err.details === 'object') {
    const details = err.details as Record<string, string>
    if (details.username) usernameErr.value = String (details.username)
    if (details.displayName) displayNameErr.value = String (details.displayName)
    if (details.password) passwordErr.value = String (details.password)
    if (details.email) emailErr.value = String (details.email)
    return true
  }
  return false
}

async function submit () {
  // 送出前先做一次本地驗證（與後端 @Valid 對齊）
  usernameErr.value = form.username.trim () ? (form.username.trim ().length < 3 ? '帳號長度需至少 3 字元' : '') : '帳號不可為空白'
  displayNameErr.value = form.displayName.trim () ? '' : '顯示名稱不可為空白'
  passwordErr.value = form.password ? (form.password.length < 6 ? '密碼長度需至少 6 字元' : '') : '密碼不可為空白'
  confirmErr.value = form.password === form.confirmPassword ? '' : '兩次輸入的密碼不一致'
  const ev = form.email.trim ()
  emailErr.value = ev && !EMAIL_PATTERN.test (ev) ? '電子信箱格式不正確' : emailErr.value
  if (usernameErr.value || displayNameErr.value || emailErr.value || passwordErr.value || confirmErr.value) return
  loading.value = true
  try {
    await authApi.register ({
      username: form.username.trim (),
      displayName: form.displayName.trim (),
      password: form.password,
      email: form.email.trim () || undefined,
    })
    Swal.fire ({ icon: 'success', title: '註冊成功', text: '請使用新帳號登入' }).then (() => {
      router.push ('/login')
    })
  } catch (e) {
    if (!mapServerError (e)) throw e
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
        <form @submit.prevent="submit" novalidate>
          <div class="mb-3">
            <label class="form-label">帳號</label>
            <input v-model.trim="form.username" type="text" class="form-control" :class="{ 'is-invalid': usernameErr }" @input="scheduleUsernameCheck" @blur="checkUsernameNow" />
            <div v-if="usernameErr" class="invalid-feedback d-block">{{ usernameErr }}</div>
          </div>
          <div class="mb-3">
            <label class="form-label">顯示名稱</label>
            <input v-model.trim="form.displayName" type="text" class="form-control" :class="{ 'is-invalid': displayNameErr }" />
            <div v-if="displayNameErr" class="invalid-feedback d-block">{{ displayNameErr }}</div>
          </div>
          <div class="mb-3">
            <label class="form-label">電子信箱 (選填)</label>
            <input v-model.trim="form.email" type="email" class="form-control" :class="{ 'is-invalid': emailErr }" @input="scheduleEmailCheck" @blur="checkEmailNow" />
            <div v-if="emailErr" class="invalid-feedback d-block">{{ emailErr }}</div>
          </div>
          <div class="mb-3">
            <label class="form-label">密碼</label>
            <div class="input-group">
              <input v-model="form.password" :type="showPassword ? 'text' : 'password'" class="form-control" :class="{ 'is-invalid': passwordErr }" />
              <button type="button" class="btn btn-outline-secondary" :class="{ 'is-invalid': passwordErr }" @click="showPassword = !showPassword" :aria-label="showPassword ? '隱藏密碼' : '顯示密碼'">
                <i class="bi" :class="showPassword ? 'bi-eye-slash' : 'bi-eye'"></i>
              </button>
              <div v-if="passwordErr" class="invalid-feedback d-block">{{ passwordErr }}</div>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-label">確認密碼</label>
            <div class="input-group">
              <input v-model="form.confirmPassword" :type="showConfirm ? 'text' : 'password'" class="form-control" :class="{ 'is-invalid': confirmErr }" />
              <button type="button" class="btn btn-outline-secondary" :class="{ 'is-invalid': confirmErr }" @click="showConfirm = !showConfirm" :aria-label="showConfirm ? '隱藏密碼' : '顯示密碼'">
                <i class="bi" :class="showConfirm ? 'bi-eye-slash' : 'bi-eye'"></i>
              </button>
              <div v-if="confirmErr" class="invalid-feedback d-block">{{ confirmErr }}</div>
            </div>
          </div>
          <button class="btn btn-success w-100" :disabled="loading">
            {{ loading ? '註冊中…' : '註冊' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>
