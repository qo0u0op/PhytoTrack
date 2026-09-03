<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Swal from 'sweetalert2'
import { refApi, senderApi } from '../api'

const route = useRoute ()
const router = useRouter ()

const senderId = computed (() => Number (route.params.id))

const loading = ref (true)
const saving = ref (false)
const cities = ref<{ id: number; name: string; districts: { id: number; name: string }[] }[]>([])
const senderTypes = ref<{ id: number; name: string }[]>([])

const form = ref ({
  name: '',
  displayName: '',
  phone: '',
  address: '',
  districtId: 0,
  senderTypeId: 0,
})
const selectedCityId = ref<number | null>(null)

const original = ref<typeof form.value | null>(null)

const filteredDistricts = computed (() => {
  if (!selectedCityId.value) return []
  const city = cities.value.find ((c) => c.id === selectedCityId.value)
  return city ? city.districts : []
})

watch (selectedCityId, () => {
  // 縣市切換時重置鄉鎮為該縣市首個
  const districts = filteredDistricts.value
  if (districts.length > 0 && !districts.some ((d) => d.id === form.value.districtId)) {
    form.value.districtId = districts[0].id
  }
})

// 上一筆/下一筆導航：基於 query 攜帶的篩選/排序狀態重建 ID 序列
const orderedIds = ref<number[]>([])
const currentIndex = computed (() => orderedIds.value.indexOf (senderId.value))
const prevId = computed (() => currentIndex.value > 0 ? orderedIds.value[currentIndex.value - 1] : null)
const nextId = computed (() => currentIndex.value >= 0 && currentIndex.value < orderedIds.value.length - 1 ? orderedIds.value[currentIndex.value + 1] : null)

function buildPreservedQuery () {
  // 保留進入時的 query 作為返回依據
  return { ...route.query }
}

function goPrev () {
  if (prevId.value) router.replace ({ name: 'sender-edit', params: { id: prevId.value }, query: buildPreservedQuery () })
}
function goNext () {
  if (nextId.value) router.replace ({ name: 'sender-edit', params: { id: nextId.value }, query: buildPreservedQuery () })
}
function goBack () {
  router.push ({ name: 'senders-admin', query: buildPreservedQuery () })
}

async function loadRefs () {
  try {
    const [cityRes, typeRes] = await Promise.all ([refApi.cities (), refApi.senderTypes ()])
    cities.value = cityRes.data
    senderTypes.value = typeRes.data
  } catch {}
}

async function loadSender () {
  loading.value = true
  try {
    const { data } = await senderApi.detail (senderId.value)
    const s: any = data
    form.value = {
      name: s.name ?? '',
      displayName: s.displayName ?? '',
      phone: s.phone ?? '',
      address: s.address ?? '',
      districtId: s.districtId ?? 0,
      senderTypeId: s.senderTypeId ?? senderTypes.value[0]?.id ?? 0,
    }
    original.value = { ...form.value }
    const city = cities.value.find ((c) => c.districts.some ((d) => d.id === s.districtId))
    selectedCityId.value = city?.id ?? cities.value[0]?.id ?? null
  } catch {
    Swal.fire ({ icon: 'error', title: '載入失敗', text: '送件人不存在或無權限' }).then (() => goBack ())
  } finally {
    loading.value = false
  }
}

async function loadOrderedIds () {
  // 若 query 有攜帶 ids 則直接使用，否則以前端重建（與 SendersView 相同邏輯）
  const idsParam = route.query.ids as string | undefined
  if (idsParam) {
    orderedIds.value = idsParam.split (',').map (Number).filter (Boolean)
    return
  }
  try {
    const { data } = await senderApi.list ()
    const senders: any[] = data as any[]
    // 簡化重建：若 query 有篩選參數則套用，否則全量依 senderId 降冪（與 SendersView 預設一致）
    const q = (route.query.q as string ?? '').trim ().toLowerCase ()
    const senderTypeId = route.query.senderTypeId ? Number (route.query.senderTypeId) : undefined
    const cityId = route.query.cityId ? Number (route.query.cityId) : undefined
    const districtId = route.query.districtId ? Number (route.query.districtId) : undefined
    let filtered = senders
    if (q) filtered = filtered.filter ((s) => `${s.name ?? ''} ${s.displayName ?? ''} ${s.phone ?? ''}`.toLowerCase ().includes (q))
    if (senderTypeId) filtered = filtered.filter ((s) => s.senderTypeId === senderTypeId)
    if (cityId) {
      const city = cities.value.find ((c) => c.id === cityId)
      if (city) filtered = filtered.filter ((s) => s.cityName === city.name)
    }
    if (districtId) {
      let districtName: string | undefined
      for (const c of cities.value) {
        const d = c.districts.find ((x) => x.id === districtId)
        if (d) { districtName = d.name; break }
      }
      if (districtName) filtered = filtered.filter ((s) => s.districtName === districtName)
    }
    // 排序：query sort 如 "senderId,desc;phone,asc"
    const sortParam = route.query.sort as string | undefined
    if (sortParam) {
      const states = sortParam.split (';').map ((p) => { const [key, order] = p.split (','); return { key, order: order as 'asc'|'desc' } })
      filtered = [...filtered].sort ((a, b) => {
        for (const { key, order } of states) {
          let av: any = (a as any)[key]
          let bv: any = (b as any)[key]
          if (av == null) av = ''
          if (bv == null) bv = ''
          let cmp = typeof av === 'number' && typeof bv === 'number' ? av - bv : String (av).localeCompare (String (bv))
          if (cmp !== 0) return order === 'asc' ? cmp : -cmp
        }
        return 0
      })
    } else {
      filtered = [...filtered].sort ((a, b) => b.senderId - a.senderId)
    }
    orderedIds.value = filtered.map ((s) => s.senderId)
  } catch {
    orderedIds.value = []
  }
}

watch (() => route.params.id, () => { loadSender () })

onMounted (async () => {
  await loadRefs ()
  await Promise.all ([loadSender (), loadOrderedIds ()])
})

async function submit () {
  if (!form.value.phone.trim () && !form.value.displayName.trim ()) {
    Swal.fire ({ icon: 'warning', title: '欄位不完整', text: '電話與顯示名稱至少需提供一項' })
    return
  }
  if (!form.value.districtId) {
    Swal.fire ({ icon: 'warning', title: '欄位不完整', text: '請選擇鄉鎮市區' })
    return
  }
  if (!form.value.senderTypeId) {
    Swal.fire ({ icon: 'warning', title: '欄位不完整', text: '請選擇身分別' })
    return
  }
  saving.value = true
  try {
    await senderApi.update (senderId.value, {
      name: form.value.name || undefined,
      displayName: form.value.displayName || undefined,
      phone: form.value.phone || undefined,
      address: form.value.address || undefined,
      districtId: form.value.districtId,
      senderTypeId: form.value.senderTypeId,
    } as any)
    Swal.fire ({ icon: 'success', title: '已更新', timer: 1200, showConfirmButton: false }).then (() => goBack ())
  } catch (e: any) {
    const msg = e?.response?.data?.error?.message ?? e?.response?.data?.message ?? '更新失敗'
    Swal.fire ({ icon: 'error', title: msg })
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="container py-4" style="max-width: 960px">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h4 class="mb-0">編輯送件人 #{{ senderId }}</h4>
      <div class="d-flex gap-2">
        <button class="btn btn-sm btn-outline-secondary" :disabled="!prevId" @click="goPrev">上一筆</button>
        <button class="btn btn-sm btn-outline-secondary" :disabled="!nextId" @click="goNext">下一筆</button>
        <button class="btn btn-sm btn-outline-secondary" @click="goBack">返回列表</button>
      </div>
    </div>

    <div v-if="loading" class="text-center text-muted py-5">載入中…</div>

    <!-- 送件人資料卡片：與 CaseFormView 一致 -->
    <div v-else class="card shadow-sm mb-4">
      <div class="card-header bg-success text-white">送件人資料</div>
      <div class="card-body row g-3">
        <div class="col-md-3">
          <label class="form-label">姓名</label>
          <input v-model.trim="form.name" class="form-control" placeholder="可空" />
        </div>
        <div class="col-md-3">
          <label class="form-label">顯示名稱 (Line/FB 暱稱)</label>
          <input v-model.trim="form.displayName" class="form-control" placeholder="電話與顯示名稱至少一項" />
        </div>
        <div class="col-md-3">
          <label class="form-label">電話</label>
          <input v-model.trim="form.phone" class="form-control" placeholder="電話與顯示名稱至少一項" />
        </div>
        <div class="col-md-3">
          <label class="form-label">身分別</label>
          <select v-model.number="form.senderTypeId" class="form-select" required>
            <option v-for="s in senderTypes" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>
        <div class="col-md-4">
          <label class="form-label">縣市</label>
          <select v-model.number="selectedCityId" class="form-select">
            <option :value="null" disabled>請選擇縣市</option>
            <option v-for="c in cities" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
        <div class="col-md-3">
          <label class="form-label">鄉鎮市區</label>
          <select v-model.number="form.districtId" class="form-select">
            <option value="0" disabled>請選擇鄉鎮市區</option>
            <option v-for="d in filteredDistricts" :key="d.id" :value="d.id">{{ d.name }}</option>
          </select>
        </div>
        <div class="col-md-5">
          <label class="form-label">地址 (選填)</label>
          <input v-model.trim="form.address" class="form-control" />
        </div>
      </div>
      <div class="card-footer d-flex justify-content-end gap-2">
        <button class="btn btn-outline-secondary" @click="goBack">取消</button>
        <button class="btn btn-success" :disabled="saving" @click="submit">{{ saving ? '儲存中…' : '儲存' }}</button>
      </div>
    </div>
  </div>
</template>
