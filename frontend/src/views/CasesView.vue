<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Swal from 'sweetalert2'
import { caseApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { escapeHtml } from '../utils/escapeHtml'

// 分頁資料型別（對應後端 Page<CaseSummaryResponse>）
interface CaseSummary {
  caseId: number
  receiveDate: string
  cropName: string
  senderName: string
  serviceName: string
  status: number
  createdAt: string
}

const auth = useAuthStore()

const cases = ref<CaseSummary[]>([])
const total = ref(0)
const page = ref(0)
const size = 10
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const { data } = await caseApi.list({ page: page.value, size })
    cases.value = data.content
    total.value = data.totalElements
  } catch {
    // 錯誤由攔截器處理
  } finally {
    loading.value = false
  }
}

onMounted(load)

// 檢視案件詳細：以 SweetAlert 彈窗呈現
async function viewDetail(id: number) {
  const { data } = await caseApi.detail(id)
  // 彈窗內容以 HTML 插入，所有動態內文必須轉義（防 XSS）
  const esc = (v?: string | null) => escapeHtml(v ?? '')
  const join = (items?: { id: number; name: string }[]) =>
    items?.map((i) => esc(i.name)).join('、') ?? '無'

  Swal.fire({
    title: `案件 #${data.caseId}`,
    width: 640,
    html: `
      <div class="text-start small">
        <p><strong>收件日期：</strong>${esc(data.receiveDate)}</p>
        <p><strong>作物：</strong>${esc(data.cropName)}</p>
        <p><strong>送件人：</strong>${esc(data.senderName)}（${esc(data.senderPhone)}）</p>
        <p><strong>地址：</strong>${esc(data.senderAddress ?? '無')}</p>
        <p><strong>耕種方式：</strong>${esc(data.methodName)}</p>
        <p><strong>被害部位：</strong>${join(data.damages)}</p>
        <p><strong>病蟲害分類：</strong>${join(data.pestCategories)}</p>
        <p><strong>防治建議：</strong>${join(data.hints)}</p>
        <p><strong>診斷簽名人：</strong>${join(data.identifiers)}</p>
        <hr />
        <p><strong>病害情形：</strong>${esc(data.pestDescription ?? '無')}</p>
        <p><strong>防治措施：</strong>${esc(data.hintDescription ?? '無')}</p>
        <p class="text-muted">建立者：${esc(data.createdByName)}／建立時間：${esc(data.createdAt)}</p>
      </div>
    `,
    confirmButtonText: '關閉',
  })
}

async function confirmDelete(id: number) {
  const result = await Swal.fire({
    icon: 'warning',
    title: '確定刪除此案件？',
    text: '此操作無法復原',
    showCancelButton: true,
    confirmButtonText: '刪除',
    cancelButtonText: '取消',
  })
  if (result.isConfirmed) {
    try {
      await caseApi.remove(id)
      Swal.fire({ icon: 'success', title: '已刪除', timer: 1200, showConfirmButton: false })
      // 刪除後回到第一頁重新載入
      page.value = 0
      await load()
    } catch {
      // 錯誤由攔截器處理
    }
  }
}
</script>

<template>
  <div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h4>案件管理</h4>
      <router-link v-if="auth.isStaff" class="btn btn-success" to="/cases/new">建立案件</router-link>
    </div>

    <div class="card shadow-sm">
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>編號</th>
              <th>收件日期</th>
              <th>作物</th>
              <th>送件人</th>
              <th>服務類別</th>
              <th>狀態</th>
              <th class="text-end">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="text-center text-muted py-4">載入中…</td>
            </tr>
            <tr v-else-if="cases.length === 0">
              <td colspan="7" class="text-center text-muted py-4">尚無案件</td>
            </tr>
            <tr v-for="c in cases" :key="c.caseId">
              <td>{{ c.caseId }}</td>
              <td>{{ c.receiveDate }}</td>
              <td>{{ c.cropName }}</td>
              <td>{{ c.senderName }}</td>
              <td>{{ c.serviceName }}</td>
              <td>
                <span class="badge text-bg-secondary">待處理</span>
              </td>
              <td class="text-end">
                <button class="btn btn-sm btn-outline-success me-1" @click="viewDetail(c.caseId)">
                  檢視
                </button>
                <router-link
                  v-if="auth.isStaff"
                  class="btn btn-sm btn-outline-primary me-1"
                  :to="`/cases/${c.caseId}/edit`"
                >
                  編輯
                </router-link>
                <button
                  v-if="auth.isAdmin"
                  class="btn btn-sm btn-outline-danger"
                  @click="confirmDelete(c.caseId)"
                >
                  刪除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 分頁控制 -->
    <nav v-if="total > size" class="mt-3">
      <ul class="pagination justify-content-center">
        <li class="page-item" :class="{ disabled: page === 0 }">
          <button class="page-link" @click="page--; load()">上一頁</button>
        </li>
        <li class="page-item disabled">
          <span class="page-link">
            第 {{ page + 1 }} 頁（共 {{ Math.ceil(total / size) }} 頁，{{ total }} 筆）
          </span>
        </li>
        <li class="page-item" :class="{ disabled: (page + 1) * size >= total }">
          <button class="page-link" @click="page++; load()">下一頁</button>
        </li>
      </ul>
    </nav>
  </div>
</template>
