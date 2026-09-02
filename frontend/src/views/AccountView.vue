<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import Swal from 'sweetalert2'
import { accountApi, authApi } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore ()
const router = useRouter ()

const loading = ref (true)
const profile = reactive ({ displayName: '', email: '' })
const originalDisplayName = ref<string | null>(null)
const originalEmail = ref<string | null>(null)
const emailCheckResult = ref<null | boolean>(null)
const checkingEmail = ref (false)
const displayNameCheckResult = ref<null | boolean>(null)
const checkingDisplayName = ref (false)
const showCurrent = ref (false)
const showNew = ref (false)
const showConfirm = ref (false)

const passwordForm = reactive ({ currentPassword: '', newPassword: '', confirmPassword: '' })
const savingProfile = ref (false)
const savingPassword = ref (false)
const requestingDeactivate = ref (false)
const isAdmin = auth.isAdmin

const displayNameDirty = () => originalDisplayName.value !== null && profile.displayName.trim () !== (originalDisplayName.value ?? '')
const canSave = () => {
  const dnOk = displayNameCheckResult.value === true
  const emailVal = profile.email.trim ()
  const emailOk = !emailVal ? true : emailCheckResult.value === true
  return dnOk && emailOk
}

async function loadProfile () {
  loading.value = true
  try {
    const { data } = await accountApi.getProfile ()
    profile.displayName = data.displayName ?? ''
    profile.email = data.email ?? ''
    originalDisplayName.value = data.displayName ?? null
    originalEmail.value = data.email ?? null
    displayNameCheckResult.value = null
    emailCheckResult.value = null
  } catch {} finally { loading.value = false }
}

watch (() => profile.displayName, () => { displayNameCheckResult.value = null })
watch (() => profile.email, () => { emailCheckResult.value = null })

function cancelDisplayName () {
  profile.displayName = originalDisplayName.value ?? ''
  displayNameCheckResult.value = null
}

async function checkDisplayName () {
  const dn = profile.displayName.trim ()
  if (!dn) {
    Swal.fire ({ icon: 'warning', title: '顯示名稱不可為空白' })
    return
  }
  if (dn.length > 50) {
    Swal.fire ({ icon: 'warning', title: '顯示名稱不可超過 50 字元' })
    return
  }
  checkingDisplayName.value = true
  try {
    // 顯示名稱目前無全域唯一限制，僅本地驗證即視為通過；若需後端檢查可在此呼叫
    displayNameCheckResult.value = true
    Swal.fire ({ icon: 'success', title: '顯示名稱可使用', timer: 1200, showConfirmButton: false })
  } finally { checkingDisplayName.value = false }
}

onMounted (loadProfile)

async function checkEmail () {
  const email = profile.email.trim ()
  if (!email) {
    Swal.fire ({ icon: 'warning', title: '請先輸入電子信箱' })
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test (email)) {
    Swal.fire ({ icon: 'warning', title: '電子信箱格式不正確' })
    return
  }
  checkingEmail.value = true
  try {
    const { data } = await accountApi.checkEmail (email)
    emailCheckResult.value = data.available
    if (data.available) {
      Swal.fire ({ icon: 'success', title: '此信箱可使用', timer: 1200, showConfirmButton: false })
    } else {
      Swal.fire ({ icon: 'warning', title: '此信箱已被他人使用' })
    }
  } catch (e: any) {
    emailCheckResult.value = null
    const msg = e?.response?.data?.message || '檢查失敗'
    Swal.fire ({ icon: 'error', title: msg })
  } finally { checkingEmail.value = false }
}

async function saveProfile () {
  if (!profile.displayName.trim ()) {
    Swal.fire ({ icon: 'warning', title: '顯示名稱不可為空白' })
    return
  }
  if (profile.email.trim () && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test (profile.email.trim ())) {
    Swal.fire ({ icon: 'warning', title: '電子信箱格式不正確' })
    return
  }
  savingProfile.value = true
  try {
    const payload: any = { displayName: profile.displayName.trim (), email: profile.email.trim () || null }
    const { data } = await accountApi.updateProfile (payload)
    originalDisplayName.value = data.displayName ?? null
    originalEmail.value = data.email ?? null
    displayNameCheckResult.value = null
    emailCheckResult.value = null
    // 同步更新 auth store 顯示名稱
    if (auth.user) auth.user.displayName = data.displayName
    Swal.fire ({ icon: 'success', title: '已更新個人資料', timer: 1200, showConfirmButton: false })
  } catch (e: any) {
    const msg = e?.response?.data?.message || '更新失敗'
    Swal.fire ({ icon: 'error', title: msg })
  } finally { savingProfile.value = false }
}

async function changePassword () {
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) {
    Swal.fire ({ icon: 'warning', title: '新密碼長度需至少 6 字元' })
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    Swal.fire ({ icon: 'warning', title: '兩次新密碼不一致' })
    return
  }
  if (!isAdmin && !passwordForm.currentPassword) {
    Swal.fire ({ icon: 'warning', title: '請輸入目前密碼' })
    return
  }
  savingPassword.value = true
  try {
    await accountApi.changePassword ({ currentPassword: passwordForm.currentPassword || undefined, newPassword: passwordForm.newPassword })
    Swal.fire ({ icon: 'success', title: '密碼已變更', timer: 1200, showConfirmButton: false })
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (e: any) {
    const msg = e?.response?.data?.message || '變更失敗'
    Swal.fire ({ icon: 'error', title: msg })
  } finally { savingPassword.value = false }
}

async function requestDeactivate () {
  const result = await Swal.fire ({ icon: 'warning', title: '確定申請停用帳號？', text: '申請後將直接登出，需由管理員審核', showCancelButton: true, confirmButtonText: '申請停用', cancelButtonText: '取消' })
  if (!result.isConfirmed) return
  requestingDeactivate.value = true
  try {
    await accountApi.requestDeactivate ()
    await Swal.fire ({ icon: 'success', title: '已送出停用請求，將為您登出', timer: 1500, showConfirmButton: false })
    try { await authApi.logout () } catch {}
    auth.logout ()
    router.push ('/login')
  } catch (e: any) {
    const msg = e?.response?.data?.message || '申請失敗'
    Swal.fire ({ icon: 'error', title: msg })
  } finally { requestingDeactivate.value = false }
}
</script>

<template>
  <div class="container py-4" style="max-width: 720px">
    <h4 class="mb-4">帳號管理</h4>
    <div v-if="loading" class="text-center text-muted py-4">載入中…</div>
    <template v-else>
      <div class="card shadow-sm mb-4">
        <div class="card-header bg-success text-white">個人資料</div>
        <div class="card-body row g-3">
          <div class="col-md-6">
            <label class="form-label">顯示名稱</label>
            <div class="input-group">
              <input v-model.trim="profile.displayName" class="form-control" placeholder="顯示名稱" />
              <button class="btn btn-outline-secondary btn-sm" type="button" :disabled="checkingDisplayName" @click="checkDisplayName">{{ checkingDisplayName ? '檢查中…' : '檢查' }}</button>
              <button v-if="displayNameDirty()" class="btn btn-outline-warning btn-sm" type="button" @click="cancelDisplayName">取消</button>
            </div>
            <div v-if="displayNameCheckResult === true" class="form-text small text-success">顯示名稱可使用</div>
            <div v-else-if="displayNameCheckResult === false" class="form-text small text-danger">顯示名稱不可使用</div>
          </div>
          <div class="col-md-6">
            <label class="form-label">電子信箱</label>
            <div class="input-group">
              <input v-model.trim="profile.email" type="email" class="form-control" placeholder="example@mail.com" />
              <button class="btn btn-outline-secondary btn-sm" type="button" :disabled="checkingEmail" @click="checkEmail">{{ checkingEmail ? '檢查中…' : '檢查' }}</button>
            </div>
            <div v-if="emailCheckResult === true" class="form-text small text-success">此信箱可使用</div>
            <div v-else-if="emailCheckResult === false" class="form-text small text-danger">此信箱已被他人使用</div>
          </div>
          <div class="col-12 text-end">
            <button v-if="canSave()" class="btn btn-success btn-sm" :disabled="savingProfile" @click="saveProfile">{{ savingProfile ? '儲存中…' : '儲存' }}</button>
            <span v-else class="small text-muted">請先完成顯示名稱與信箱檢查</span>
          </div>
        </div>
      </div>

      <div class="card shadow-sm mb-4">
        <div class="card-header bg-success text-white">修改密碼</div>
        <div class="card-body row g-3">
          <div v-if="!isAdmin" class="col-12">
            <label class="form-label">目前密碼</label>
            <div class="input-group">
              <input v-model="passwordForm.currentPassword" :type="showCurrent ? 'text' : 'password'" class="form-control" placeholder="目前密碼" />
              <button class="btn btn-outline-secondary btn-sm d-inline-flex align-items-center justify-content-center" type="button" @click="showCurrent = !showCurrent" :title="showCurrent ? '隱藏' : '顯示'" style="width:38px">
                <svg v-if="showCurrent" xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24"><path d="M0 0h24v24H0z" fill="none" /><path fill="currentColor" fill-rule="evenodd" d="M20.53 4.53a.75.75 0 0 0-1.06-1.06l-16 16a.75.75 0 1 0 1.06 1.06l2.847-2.847c1.367.644 2.94 1.067 4.623 1.067c2.684 0 5.09-1.077 6.82-2.405c.867-.665 1.583-1.407 2.089-2.136c.492-.709.841-1.486.841-2.209s-.35-1.5-.841-2.209c-.506-.729-1.222-1.47-2.088-2.136q-.394-.303-.832-.583zM16.9 8.161l-1.771 1.771a3.75 3.75 0 0 1-5.197 5.197l-1.417 1.416A9.3 9.3 0 0 0 12 17.25c2.287 0 4.38-.923 5.907-2.095c.762-.585 1.364-1.218 1.77-1.801c.419-.604.573-1.077.573-1.354s-.154-.75-.573-1.354c-.406-.583-1.008-1.216-1.77-1.801q-.47-.361-1.008-.684m-5.87 5.87a2.25 2.25 0 0 0 3-3z" clip-rule="evenodd" /><path fill="currentColor" d="M12 5.25c1.032 0 2.024.16 2.951.431a.243.243 0 0 1 .1.407l-.824.825a.25.25 0 0 1-.237.067A9 9 0 0 0 12 6.75c-2.287 0-4.38.923-5.907 2.095c-.762.585-1.364 1.218-1.77 1.801c-.419.604-.573 1.077-.573 1.354s.154.75.573 1.354c.354.51.858 1.057 1.488 1.577c.116.095.127.27.02.377l-.708.709a.246.246 0 0 1-.333.016a9.5 9.5 0 0 1-1.699-1.824C2.6 13.5 2.25 12.723 2.25 12s.35-1.5.841-2.209c.506-.729 1.222-1.47 2.088-2.136C6.91 6.327 9.316 5.25 12 5.25" /><path fill="currentColor" d="M12 8.25q.178 0 .351.016c.197.019.268.254.129.394l-1.213 1.212a2.26 2.26 0 0 0-1.395 1.395L8.66 12.48c-.14.14-.375.068-.394-.129A3.75 3.75 0 0 1 12 8.25" /></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24"><path d="M0 0h24v24H0z" fill="none" /><g fill="currentColor" fill-rule="evenodd" clip-rule="evenodd"><path d="M8.25 12a3.75 3.75 0 1 1 7.5 0a3.75 3.75 0 0 1-7.5 0M12 9.75a2.25 2.25 0 1 0 0 4.5a2.25 2.25 0 0 0 0-4.5" /><path d="M4.323 10.646c-.419.604-.573 1.077-.573 1.354s.154.75.573 1.354c.406.583 1.008 1.216 1.77 1.801C7.62 16.327 9.713 17.25 12 17.25s4.38-.923 5.907-2.095c.762-.585 1.364-1.218 1.77-1.801c.419-.604.573-1.077.573-1.354s-.154-.75-.573-1.354c-.406-.583-1.008-1.216-1.77-1.801C16.38 7.673 14.287 6.75 12 6.75s-4.38.923-5.907 2.095c-.762.585-1.364 1.218-1.77 1.801m.856-2.991C6.91 6.327 9.316 5.25 12 5.25s5.09 1.077 6.82 2.405c.867.665 1.583 1.407 2.089 2.136c.492.709.841 1.486.841 2.209s-.35 1.5-.841 2.209c-.506.729-1.222 1.47-2.088 2.136c-1.73 1.328-4.137 2.405-6.821 2.405s-5.09-1.077-6.82-2.405c-.867-.665-1.583-1.407-2.089-2.136C2.6 13.5 2.25 12.723 2.25 12s.35-1.5.841-2.209c.506-.729 1.222-1.47 2.088-2.136" /></g></svg>
              </button>
            </div>
          </div>
          <div v-else class="col-12">
            <label class="form-label">目前密碼 <span class="text-muted small">(管理員可留空)</span></label>
            <div class="input-group">
              <input v-model="passwordForm.currentPassword" :type="showCurrent ? 'text' : 'password'" class="form-control" placeholder="管理員可留空" />
              <button class="btn btn-outline-secondary btn-sm d-inline-flex align-items-center justify-content-center" type="button" @click="showCurrent = !showCurrent" :title="showCurrent ? '隱藏' : '顯示'" style="width:38px">
                <svg v-if="showCurrent" xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24"><path d="M0 0h24v24H0z" fill="none" /><path fill="currentColor" fill-rule="evenodd" d="M20.53 4.53a.75.75 0 0 0-1.06-1.06l-16 16a.75.75 0 1 0 1.06 1.06l2.847-2.847c1.367.644 2.94 1.067 4.623 1.067c2.684 0 5.09-1.077 6.82-2.405c.867-.665 1.583-1.407 2.089-2.136c.492-.709.841-1.486.841-2.209s-.35-1.5-.841-2.209c-.506-.729-1.222-1.47-2.088-2.136q-.394-.303-.832-.583zM16.9 8.161l-1.771 1.771a3.75 3.75 0 0 1-5.197 5.197l-1.417 1.416A9.3 9.3 0 0 0 12 17.25c2.287 0 4.38-.923 5.907-2.095c.762-.585 1.364-1.218 1.77-1.801c.419-.604.573-1.077.573-1.354s-.154-.75-.573-1.354c-.406-.583-1.008-1.216-1.77-1.801q-.47-.361-1.008-.684m-5.87 5.87a2.25 2.25 0 0 0 3-3z" clip-rule="evenodd" /><path fill="currentColor" d="M12 5.25c1.032 0 2.024.16 2.951.431a.243.243 0 0 1 .1.407l-.824.825a.25.25 0 0 1-.237.067A9 9 0 0 0 12 6.75c-2.287 0-4.38.923-5.907 2.095c-.762.585-1.364 1.218-1.77 1.801c-.419.604-.573 1.077-.573 1.354s.154.75.573 1.354c.354.51.858 1.057 1.488 1.577c.116.095.127.27.02.377l-.708.709a.246.246 0 0 1-.333.016a9.5 9.5 0 0 1-1.699-1.824C2.6 13.5 2.25 12.723 2.25 12s.35-1.5.841-2.209c.506-.729 1.222-1.47 2.088-2.136C6.91 6.327 9.316 5.25 12 5.25" /><path fill="currentColor" d="M12 8.25q.178 0 .351.016c.197.019.268.254.129.394l-1.213 1.212a2.26 2.26 0 0 0-1.395 1.395L8.66 12.48c-.14.14-.375.068-.394-.129A3.75 3.75 0 0 1 12 8.25" /></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24"><path d="M0 0h24v24H0z" fill="none" /><g fill="currentColor" fill-rule="evenodd" clip-rule="evenodd"><path d="M8.25 12a3.75 3.75 0 1 1 7.5 0a3.75 3.75 0 0 1-7.5 0M12 9.75a2.25 2.25 0 1 0 0 4.5a2.25 2.25 0 0 0 0-4.5" /><path d="M4.323 10.646c-.419.604-.573 1.077-.573 1.354s.154.75.573 1.354c.406.583 1.008 1.216 1.77 1.801C7.62 16.327 9.713 17.25 12 17.25s4.38-.923 5.907-2.095c.762-.585 1.364-1.218 1.77-1.801c.419-.604.573-1.077.573-1.354s-.154-.75-.573-1.354c-.406-.583-1.008-1.216-1.77-1.801C16.38 7.673 14.287 6.75 12 6.75s-4.38.923-5.907 2.095c-.762.585-1.364 1.218-1.77 1.801m.856-2.991C6.91 6.327 9.316 5.25 12 5.25s5.09 1.077 6.82 2.405c.867.665 1.583 1.407 2.089 2.136c.492.709.841 1.486.841 2.209s-.35 1.5-.841 2.209c-.506.729-1.222 1.47-2.088 2.136c-1.73 1.328-4.137 2.405-6.821 2.405s-5.09-1.077-6.82-2.405c-.867-.665-1.583-1.407-2.089-2.136C2.6 13.5 2.25 12.723 2.25 12s.35-1.5.841-2.209c.506-.729 1.222-1.47 2.088-2.136" /></g></svg>
              </button>
            </div>
          </div>
          <div class="col-md-6">
            <label class="form-label">新密碼</label>
            <div class="input-group">
              <input v-model="passwordForm.newPassword" :type="showNew ? 'text' : 'password'" class="form-control" placeholder="至少 6 字元" />
              <button class="btn btn-outline-secondary btn-sm d-inline-flex align-items-center justify-content-center" type="button" @click="showNew = !showNew" :title="showNew ? '隱藏' : '顯示'" style="width:38px">
                <svg v-if="showNew" xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24"><path d="M0 0h24v24H0z" fill="none" /><path fill="currentColor" fill-rule="evenodd" d="M20.53 4.53a.75.75 0 0 0-1.06-1.06l-16 16a.75.75 0 1 0 1.06 1.06l2.847-2.847c1.367.644 2.94 1.067 4.623 1.067c2.684 0 5.09-1.077 6.82-2.405c.867-.665 1.583-1.407 2.089-2.136c.492-.709.841-1.486.841-2.209s-.35-1.5-.841-2.209c-.506-.729-1.222-1.47-2.088-2.136q-.394-.303-.832-.583zM16.9 8.161l-1.771 1.771a3.75 3.75 0 0 1-5.197 5.197l-1.417 1.416A9.3 9.3 0 0 0 12 17.25c2.287 0 4.38-.923 5.907-2.095c.762-.585 1.364-1.218 1.77-1.801c.419-.604.573-1.077.573-1.354s-.154-.75-.573-1.354c-.406-.583-1.008-1.216-1.77-1.801q-.47-.361-1.008-.684m-5.87 5.87a2.25 2.25 0 0 0 3-3z" clip-rule="evenodd" /><path fill="currentColor" d="M12 5.25c1.032 0 2.024.16 2.951.431a.243.243 0 0 1 .1.407l-.824.825a.25.25 0 0 1-.237.067A9 9 0 0 0 12 6.75c-2.287 0-4.38.923-5.907 2.095c-.762.585-1.364 1.218-1.77 1.801c-.419.604-.573 1.077-.573 1.354s.154.75.573 1.354c.354.51.858 1.057 1.488 1.577c.116.095.127.27.02.377l-.708.709a.246.246 0 0 1-.333.016a9.5 9.5 0 0 1-1.699-1.824C2.6 13.5 2.25 12.723 2.25 12s.35-1.5.841-2.209c.506-.729 1.222-1.47 2.088-2.136C6.91 6.327 9.316 5.25 12 5.25" /><path fill="currentColor" d="M12 8.25q.178 0 .351.016c.197.019.268.254.129.394l-1.213 1.212a2.26 2.26 0 0 0-1.395 1.395L8.66 12.48c-.14.14-.375.068-.394-.129A3.75 3.75 0 0 1 12 8.25" /></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24"><path d="M0 0h24v24H0z" fill="none" /><g fill="currentColor" fill-rule="evenodd" clip-rule="evenodd"><path d="M8.25 12a3.75 3.75 0 1 1 7.5 0a3.75 3.75 0 0 1-7.5 0M12 9.75a2.25 2.25 0 1 0 0 4.5a2.25 2.25 0 0 0 0-4.5" /><path d="M4.323 10.646c-.419.604-.573 1.077-.573 1.354s.154.75.573 1.354c.406.583 1.008 1.216 1.77 1.801C7.62 16.327 9.713 17.25 12 17.25s4.38-.923 5.907-2.095c.762-.585 1.364-1.218 1.77-1.801c.419-.604.573-1.077.573-1.354s-.154-.75-.573-1.354c-.406-.583-1.008-1.216-1.77-1.801C16.38 7.673 14.287 6.75 12 6.75s-4.38.923-5.907 2.095c-.762.585-1.364 1.218-1.77 1.801m.856-2.991C6.91 6.327 9.316 5.25 12 5.25s5.09 1.077 6.82 2.405c.867.665 1.583 1.407 2.089 2.136c.492.709.841 1.486.841 2.209s-.35 1.5-.841 2.209c-.506.729-1.222 1.47-2.088 2.136c-1.73 1.328-4.137 2.405-6.821 2.405s-5.09-1.077-6.82-2.405c-.867-.665-1.583-1.407-2.089-2.136C2.6 13.5 2.25 12.723 2.25 12s.35-1.5.841-2.209c.506-.729 1.222-1.47 2.088-2.136" /></g></svg>
              </button>
            </div>
          </div>
          <div class="col-md-6">
            <label class="form-label">確認新密碼</label>
            <div class="input-group">
              <input v-model="passwordForm.confirmPassword" :type="showConfirm ? 'text' : 'password'" class="form-control" placeholder="再次輸入新密碼" />
              <button class="btn btn-outline-secondary btn-sm d-inline-flex align-items-center justify-content-center" type="button" @click="showConfirm = !showConfirm" :title="showConfirm ? '隱藏' : '顯示'" style="width:38px">
                <svg v-if="showConfirm" xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24"><path d="M0 0h24v24H0z" fill="none" /><path fill="currentColor" fill-rule="evenodd" d="M20.53 4.53a.75.75 0 0 0-1.06-1.06l-16 16a.75.75 0 1 0 1.06 1.06l2.847-2.847c1.367.644 2.94 1.067 4.623 1.067c2.684 0 5.09-1.077 6.82-2.405c.867-.665 1.583-1.407 2.089-2.136c.492-.709.841-1.486.841-2.209s-.35-1.5-.841-2.209c-.506-.729-1.222-1.47-2.088-2.136q-.394-.303-.832-.583zM16.9 8.161l-1.771 1.771a3.75 3.75 0 0 1-5.197 5.197l-1.417 1.416A9.3 9.3 0 0 0 12 17.25c2.287 0 4.38-.923 5.907-2.095c.762-.585 1.364-1.218 1.77-1.801c.419-.604.573-1.077.573-1.354s-.154-.75-.573-1.354c-.406-.583-1.008-1.216-1.77-1.801q-.47-.361-1.008-.684m-5.87 5.87a2.25 2.25 0 0 0 3-3z" clip-rule="evenodd" /><path fill="currentColor" d="M12 5.25c1.032 0 2.024.16 2.951.431a.243.243 0 0 1 .1.407l-.824.825a.25.25 0 0 1-.237.067A9 9 0 0 0 12 6.75c-2.287 0-4.38.923-5.907 2.095c-.762.585-1.364 1.218-1.77 1.801c-.419.604-.573 1.077-.573 1.354s.154.75.573 1.354c.354.51.858 1.057 1.488 1.577c.116.095.127.27.02.377l-.708.709a.246.246 0 0 1-.333.016a9.5 9.5 0 0 1-1.699-1.824C2.6 13.5 2.25 12.723 2.25 12s.35-1.5.841-2.209c.506-.729 1.222-1.47 2.088-2.136C6.91 6.327 9.316 5.25 12 5.25" /><path fill="currentColor" d="M12 8.25q.178 0 .351.016c.197.019.268.254.129.394l-1.213 1.212a2.26 2.26 0 0 0-1.395 1.395L8.66 12.48c-.14.14-.375.068-.394-.129A3.75 3.75 0 0 1 12 8.25" /></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24"><path d="M0 0h24v24H0z" fill="none" /><g fill="currentColor" fill-rule="evenodd" clip-rule="evenodd"><path d="M8.25 12a3.75 3.75 0 1 1 7.5 0a3.75 3.75 0 0 1-7.5 0M12 9.75a2.25 2.25 0 1 0 0 4.5a2.25 2.25 0 0 0 0-4.5" /><path d="M4.323 10.646c-.419.604-.573 1.077-.573 1.354s.154.75.573 1.354c.406.583 1.008 1.216 1.77 1.801C7.62 16.327 9.713 17.25 12 17.25s4.38-.923 5.907-2.095c.762-.585 1.364-1.218 1.77-1.801c.419-.604.573-1.077.573-1.354s-.154-.75-.573-1.354c-.406-.583-1.008-1.216-1.77-1.801C16.38 7.673 14.287 6.75 12 6.75s-4.38.923-5.907 2.095c-.762.585-1.364 1.218-1.77 1.801m.856-2.991C6.91 6.327 9.316 5.25 12 5.25s5.09 1.077 6.82 2.405c.867.665 1.583 1.407 2.089 2.136c.492.709.841 1.486.841 2.209s-.35 1.5-.841 2.209c-.506.729-1.222 1.47-2.088 2.136c-1.73 1.328-4.137 2.405-6.821 2.405s-5.09-1.077-6.82-2.405c-.867-.665-1.583-1.407-2.089-2.136C2.6 13.5 2.25 12.723 2.25 12s.35-1.5.841-2.209c.506-.729 1.222-1.47 2.088-2.136" /></g></svg>
              </button>
            </div>
          </div>
          <div class="col-12 text-end">
            <button class="btn btn-success btn-sm" :disabled="savingPassword" @click="changePassword">{{ savingPassword ? '變更中…' : '變更密碼' }}</button>
          </div>
        </div>
      </div>

      <div v-if="!isAdmin" class="card shadow-sm mb-4">
        <div class="card-header bg-success text-white d-flex justify-content-between align-items-center">
          <span>停用帳號</span>
          <button class="btn btn-outline-light btn-sm" :disabled="requestingDeactivate" @click="requestDeactivate">申請停用</button>
        </div>
        <div class="card-body">
          <p class="small text-muted mb-0">申請後需由管理員審核，審核通過後帳號將被停用且無法登入。</p>
        </div>
      </div>

    </template>
  </div>
</template>
