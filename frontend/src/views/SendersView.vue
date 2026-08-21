<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Swal from 'sweetalert2'
import { senderApi } from '../api'

interface SenderRow {
  senderId: number
  name: string | null
  displayName: string | null
  phone: string | null
  address: string
  districtName: string
  cityName: string
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
                <button class="btn btn-sm btn-outline-danger" @click="handleDelete(s.senderId, displayLabel(s))">刪除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
