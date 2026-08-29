<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Swal from 'sweetalert2'
import { senderApi } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

interface SenderRow {
  senderId: number
  name: string | null
  displayName: string | null
  phone: string | null
  address: string
  districtId?: number
  districtName: string
  cityName: string
  senderTypeId?: number
  senderTypeName: string
}

const senders = ref<SenderRow[]>([])
const loading = ref(true)
const searchQ = ref('')

function displayLabel(s: SenderRow) {
  const hasName = s.name && s.name.trim()
  const hasDisplay = s.displayName && s.displayName.trim()
  if (hasName && hasDisplay) return `${s.name}(${s.displayName})`
  if (hasDisplay) return s.displayName!
  if (hasName) return s.name!
  return s.phone ?? ''
}

async function load() {
  loading.value = true
  try {
    if (searchQ.value.trim()) {
      const { data } = await senderApi.search(searchQ.value.trim())
      senders.value = data
    } else {
      const { data } = await senderApi.list()
      senders.value = data
    }
  } catch {
    // 由攔截器處理
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function handleSearch() {
  await load()
}

async function handleDelete(id: number, label: string) {
  const result = await Swal.fire({
    icon: 'warning',
    title: `確定刪除「${label}」？`,
    text: '此操作無法復原，若已被案件引用將被拒絕',
    showCancelButton: true,
    confirmButtonText: '刪除',
    cancelButtonText: '取消',
  })
  if (!result.isConfirmed) return
  try {
    await senderApi.remove(id)
    Swal.fire({ icon: 'success', title: '已刪除', timer: 1200, showConfirmButton: false })
    await load()
  } catch {}
}

async function handleEdit(s: SenderRow) {
  // 先取完整資料以補齊 districtId / senderTypeId
  let detailData: any = s
  try {
    const { data } = await senderApi.detail(s.senderId)
    detailData = data
  } catch {}
  const { value: form } = await Swal.fire({
    title: `編輯送件人 #${s.senderId}`,
    html: `
      <input id="swal-sender-name" class="swal2-input" placeholder="姓名" value="${(detailData.name ?? '').replace(/"/g, '&quot;')}" />
      <input id="swal-sender-displayName" class="swal2-input" placeholder="顯示名稱" value="${(detailData.displayName ?? '').replace(/"/g, '&quot;')}" />
      <input id="swal-sender-phone" class="swal2-input" placeholder="電話" value="${(detailData.phone ?? '').replace(/"/g, '&quot;')}" />
      <input id="swal-sender-address" class="swal2-input" placeholder="地址" value="${(detailData.address ?? '').replace(/"/g, '&quot;')}" />
    `,
    showCancelButton: true,
    confirmButtonText: '儲存',
    cancelButtonText: '取消',
    preConfirm: () => {
      const name = (document.getElementById('swal-sender-name') as HTMLInputElement).value.trim()
      const displayName = (document.getElementById('swal-sender-displayName') as HTMLInputElement).value.trim()
      const phone = (document.getElementById('swal-sender-phone') as HTMLInputElement).value.trim()
      const address = (document.getElementById('swal-sender-address') as HTMLInputElement).value.trim()
      if (!phone && !displayName) return Swal.showValidationMessage('電話與顯示名稱至少需提供一項')
      if (!address) return Swal.showValidationMessage('地址不可為空白')
      return { name: name || undefined, displayName: displayName || undefined, phone: phone || undefined, address }
    },
  })
  if (!form) return
  try {
    await senderApi.update(s.senderId, {
      name: form.name,
      displayName: form.displayName,
      phone: form.phone,
      address: form.address,
      districtId: detailData.districtId ?? 1,
      senderTypeId: detailData.senderTypeId ?? 1,
    } as any)
    Swal.fire({ icon: 'success', title: '已更新', timer: 1200, showConfirmButton: false })
    await load()
  } catch {}
}
</script>

<template>
  <div class="container py-4">
    <h4 class="mb-4">送件人管理</h4>
    <div class="card shadow-sm mb-3">
      <div class="card-body">
        <div class="row g-2 align-items-end">
          <div class="col-md-6">
            <label class="form-label small text-muted mb-1">搜尋（姓名/電話/顯示名稱）</label>
            <input v-model="searchQ" type="text" class="form-control form-control-sm" placeholder="輸入關鍵字" @keyup.enter="handleSearch" />
          </div>
          <div class="col-md-3">
            <button class="btn btn-sm btn-primary me-1" @click="handleSearch">搜尋</button>
            <button class="btn btn-sm btn-outline-secondary" @click="searchQ = ''; load()">清除</button>
          </div>
        </div>
      </div>
    </div>

    <div class="card shadow-sm">
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>ID</th>
              <th>顯示</th>
              <th>電話</th>
              <th>地址</th>
              <th>鄉鎮</th>
              <th>縣市</th>
              <th>身分別</th>
              <th class="text-end">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="text-center text-muted py-4">載入中…</td>
            </tr>
            <tr v-else-if="senders.length === 0">
              <td colspan="8" class="text-center text-muted py-4">尚無資料</td>
            </tr>
            <tr v-for="s in senders" :key="s.senderId">
              <td>{{ s.senderId }}</td>
              <td>{{ displayLabel(s) }}</td>
              <td>{{ s.phone ?? '—' }}</td>
              <td>{{ s.address }}</td>
              <td>{{ s.districtName }}</td>
              <td>{{ s.cityName }}</td>
              <td>{{ s.senderTypeName }}</td>
              <td class="text-end">
                <button v-if="auth.isStaff" class="btn btn-sm btn-outline-primary me-1" @click="handleEdit(s)">編輯</button>
                <button v-if="auth.isAdmin" class="btn btn-sm btn-outline-danger" @click="handleDelete(s.senderId, displayLabel(s))">刪除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
