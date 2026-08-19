<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Swal from 'sweetalert2'
import { aiApi, caseApi, refApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { escapeHtml } from '../utils/escapeHtml'
import type { components } from '../types/api'

interface IdName {
  id: number
  name: string
}
interface CropCategory {
  id: number
  name: string
  crops: IdName[]
}
interface PestCategoryItem {
  id: number
  code: string
  name: string
}
interface PestType {
  id: number
  name: string
  categories: PestCategoryItem[]
}
interface City {
  id: number
  name: string
  districts: IdName[]
}

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

// 編輯模式：路由帶 :id 即為編輯既有案件
const editId = route.params.id ? Number(route.params.id) : null

// 表單資料（對應後端 CaseCreateRequest）
const form = reactive({
  receiveDate: new Date().toISOString().slice(0, 10),
  cropScale: '',
  damageScale: '',
  pestDescription: '',
  hintDescription: '',
  senderName: '',
  senderPhone: '',
  senderAddress: '',
  senderDistrictId: 0,
  senderTypeId: 0,
  methodId: 0,
  cropId: 0,
  serviceId: 0,
  deliverId: 0,
  damageIds: [] as number[],
  hintIds: [] as number[],
  pestCategoryIds: [] as number[],
  identifierIds: [] as number[],
})

// 參照資料
const cropCategories = ref<CropCategory[]>([])
const pestTypes = ref<PestType[]>([])
const damages = ref<IdName[]>([])
const hints = ref<IdName[]>([])
const methods = ref<IdName[]>([])
const deliveries = ref<IdName[]>([])
const services = ref<IdName[]>([])
const cities = ref<City[]>([])
const senderTypes = ref<IdName[]>([])
const identifiers = ref<IdName[]>([])

const loading = ref(true)
const saving = ref(false)
const analyzing = ref(false)

// 依選定作物反查其所屬分類名稱（供 AI Prompt 使用）
const selectedCropCategory = computed(() => {
  const crop = cropCategories.value
    .flatMap((c) => c.crops.map((cr) => ({ ...cr, category: c.name })))
    .find((c) => c.id === form.cropId)
  return crop?.category ?? ''
})

async function loadRefs() {
  // 平行載入所有下拉選單資料
  const [cc, pt, dm, hn, mt, dl, sv, ct, st, idf] = await Promise.all([
    refApi.cropCategories(),
    refApi.pestTypes(),
    refApi.damages(),
    refApi.hints(),
    refApi.methods(),
    refApi.deliveries(),
    refApi.services(),
    refApi.cities(),
    refApi.senderTypes(),
    refApi.identifiers(),
  ])
  cropCategories.value = cc.data
  pestTypes.value = pt.data
  damages.value = dm.data
  hints.value = hn.data
  methods.value = mt.data
  deliveries.value = dl.data
  services.value = sv.data
  cities.value = ct.data
  senderTypes.value = st.data
  identifiers.value = idf.data

  // 建立模式：套用合理的預設值
  form.methodId = methods.value[0]?.id ?? 0
  form.deliverId = deliveries.value[0]?.id ?? 0
  form.serviceId = services.value[0]?.id ?? 0
  form.senderTypeId = senderTypes.value[0]?.id ?? 0

  if (editId) {
    await loadCase(editId)
  }
}

async function loadCase(id: number) {
  const { data } = await caseApi.detail(id)
  const d = data as components['schemas']['CaseResponse']
  form.receiveDate = d.receiveDate ?? ''
  form.cropScale = d.cropScale ?? ''
  form.damageScale = d.damageScale ?? ''
  form.pestDescription = d.pestDescription ?? ''
  form.hintDescription = d.hintDescription ?? ''
  form.senderName = d.senderName ?? ''
  form.senderPhone = d.senderPhone ?? ''
  form.senderAddress = d.senderAddress ?? ''
  form.damageIds = d.damages?.map((x) => x.id).filter((x): x is number => x != null) ?? []
  form.hintIds = d.hints?.map((x) => x.id).filter((x): x is number => x != null) ?? []
  form.pestCategoryIds = d.pestCategories?.map((x) => x.id).filter((x): x is number => x != null) ?? []
  form.identifierIds = d.identifiers?.map((x) => x.id).filter((x): x is number => x != null) ?? []

  // 由名稱反查 ID（後端詳細回應帶的是名稱而非 ID）
  const crop = cropCategories.value
    .flatMap((c) => c.crops)
    .find((c) => c.name === d.cropName)
  form.cropId = crop?.id ?? 0
  form.methodId = methods.value.find((m) => m.name === d.methodName)?.id ?? 0
  form.serviceId = services.value.find((s) => s.name === d.serviceName)?.id ?? 0
  form.deliverId = deliveries.value.find((x) => x.name === d.deliveryName)?.id ?? 0
}

onMounted(async () => {
  await loadRefs()
  loading.value = false
})

// 切換多選（Checkbox）的輔助函式
function toggle(arr: number[], id: number) {
  const i = arr.indexOf(id)
  if (i >= 0) arr.splice(i, 1)
  else arr.push(id)
}

async function submit() {
  // 送出前檢查（後端仍有完整驗證）
  if (!form.senderDistrictId || !form.cropId) {
    Swal.fire({ icon: 'warning', title: '欄位不完整', text: '請選擇鄉鎮市區與作物' })
    return
  }
  saving.value = true
  try {
    if (editId) {
      await caseApi.update(editId, {
        receiveDate: form.receiveDate,
        cropScale: form.cropScale || undefined,
        damageScale: form.damageScale || undefined,
        pestDescription: form.pestDescription || undefined,
        hintDescription: form.hintDescription || undefined,
        methodId: form.methodId,
        cropId: form.cropId,
        serviceId: form.serviceId,
        deliverId: form.deliverId,
      })
    } else {
      await caseApi.create({
        receiveDate: form.receiveDate,
        cropScale: form.cropScale || undefined,
        damageScale: form.damageScale || undefined,
        pestDescription: form.pestDescription || undefined,
        hintDescription: form.hintDescription || undefined,
        senderName: form.senderName,
        senderPhone: form.senderPhone,
        senderAddress: form.senderAddress,
        senderDistrictId: form.senderDistrictId,
        senderTypeId: form.senderTypeId,
        methodId: form.methodId,
        cropId: form.cropId,
        serviceId: form.serviceId,
        deliverId: form.deliverId,
        damageIds: form.damageIds,
        hintIds: form.hintIds,
        pestCategoryIds: form.pestCategoryIds,
        identifierIds: form.identifierIds,
      })
    }
    Swal.fire({ icon: 'success', title: '儲存成功', timer: 1200, showConfirmButton: false }).then(() => {
      router.push('/cases')
    })
  } catch {
    // 錯誤由攔截器處理
  } finally {
    saving.value = false
  }
}

// AI 診斷：組出欄位資料送後端，再由後端代理 llama.cpp
async function runAi() {
  const category = selectedCropCategory.value
  const crop = cropCategories.value
    .flatMap((c) => c.crops)
    .find((c) => c.id === form.cropId)
  if (!crop) {
    Swal.fire({ icon: 'warning', title: '請先選擇作物' })
    return
  }
  analyzing.value = true
  try {
    const { data } = await aiApi.analyze({
      cropName: crop.name,
      cropCategory: category,
      damages: damages.value.filter((d) => form.damageIds.includes(d.id)).map((d) => d.name),
      pestCategories: pestTypes.value
        .flatMap((p) => p.categories)
        .filter((c) => form.pestCategoryIds.includes(c.id))
        .map((c) => c.name),
      pestDescription: form.pestDescription,
      cropScale: form.cropScale,
      damageScale: form.damageScale,
      cultivationMethod: methods.value.find((m) => m.id === form.methodId)?.name,
      hintDescription: form.hintDescription,
    })
    Swal.fire({
      icon: 'info',
      title: `AI 診斷建議（${(data.elapsedMs / 1000).toFixed(1)} 秒）`,
      width: 700,
      html: `<div class="text-start"><pre class="text-wrap small">${escapeHtml(data.suggestion)}</pre></div>`,
      confirmButtonText: '關閉',
    })
  } catch {
    // 錯誤由攔截器處理（模型未啟動時亦會在此顯示）
  } finally {
    analyzing.value = false
  }
}
</script>

<template>
  <div class="container py-4" style="max-width: 960px">
    <h4 class="mb-4">{{ editId ? `編輯案件 #${editId}` : '建立診斷案件' }}</h4>

    <div v-if="loading" class="text-center text-muted py-5">載入中…</div>

    <form v-else @submit.prevent="submit">
      <!-- 送件人資料 -->
      <div class="card shadow-sm mb-4">
        <div class="card-header bg-success text-white">送件人資料</div>
        <div class="card-body row g-3">
          <div class="col-md-4">
            <label class="form-label">姓名</label>
            <input v-model.trim="form.senderName" class="form-control" required />
          </div>
          <div class="col-md-4">
            <label class="form-label">電話</label>
            <input v-model.trim="form.senderPhone" class="form-control" required />
          </div>
          <div class="col-md-4">
            <label class="form-label">身分別</label>
            <select v-model.number="form.senderTypeId" class="form-select" required>
              <option v-for="s in senderTypes" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label">縣市／鄉鎮市區</label>
            <select v-model.number="form.senderDistrictId" class="form-select">
              <option value="0" disabled>請選擇鄉鎮市區</option>
              <optgroup v-for="c in cities" :key="c.id" :label="c.name">
                <option v-for="d in c.districts" :key="d.id" :value="d.id">{{ d.name }}</option>
              </optgroup>
            </select>
          </div>
          <div class="col-md-8">
            <label class="form-label">地址</label>
            <input v-model.trim="form.senderAddress" class="form-control" required />
          </div>
        </div>
      </div>

      <!-- 作物與診斷資訊 -->
      <div class="card shadow-sm mb-4">
        <div class="card-header bg-success text-white">作物與診斷資訊</div>
        <div class="card-body row g-3">
          <div class="col-md-4">
            <label class="form-label">作物</label>
            <select v-model.number="form.cropId" class="form-select" required>
              <option value="0" disabled>請選擇作物</option>
              <optgroup v-for="cc in cropCategories" :key="cc.id" :label="cc.name">
                <option v-for="cr in cc.crops" :key="cr.id" :value="cr.id">{{ cr.name }}</option>
              </optgroup>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label">耕種方式</label>
            <select v-model.number="form.methodId" class="form-select" required>
              <option v-for="m in methods" :key="m.id" :value="m.id">{{ m.name }}</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label">服務類別</label>
            <select v-model.number="form.serviceId" class="form-select" required>
              <option v-for="s in services" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label">收件日期</label>
            <input v-model="form.receiveDate" type="date" class="form-control" required />
          </div>
          <div class="col-md-4">
            <label class="form-label">種植面積</label>
            <input v-model.trim="form.cropScale" class="form-control" placeholder="例：2 分地" />
          </div>
          <div class="col-md-4">
            <label class="form-label">被害面積或植株數</label>
            <input v-model.trim="form.damageScale" class="form-control" placeholder="例：約 3 成" />
          </div>
          <div class="col-12">
            <label class="form-label">被害部位（可複選）</label>
            <div class="d-flex flex-wrap gap-3">
              <label v-for="d in damages" :key="d.id" class="form-check form-check-inline">
                <input
                  class="form-check-input"
                  type="checkbox"
                  :checked="form.damageIds.includes(d.id)"
                  @change="toggle(form.damageIds, d.id)"
                />
                <span class="form-check-label">{{ d.name }}</span>
              </label>
            </div>
          </div>
          <div class="col-12">
            <label class="form-label">病蟲害分類（可複選）</label>
            <div v-for="p in pestTypes" :key="p.id" class="mb-2">
              <div class="fw-bold small text-muted">{{ p.name }}</div>
              <div class="d-flex flex-wrap gap-3">
                <label v-for="c in p.categories" :key="c.id" class="form-check form-check-inline">
                  <input
                    class="form-check-input"
                    type="checkbox"
                    :checked="form.pestCategoryIds.includes(c.id)"
                    @change="toggle(form.pestCategoryIds, c.id)"
                  />
                  <span class="form-check-label">{{ c.code }} {{ c.name }}</span>
                </label>
              </div>
            </div>
          </div>
          <div class="col-12">
            <label class="form-label">病害情形描述</label>
            <textarea v-model.trim="form.pestDescription" class="form-control" rows="2"></textarea>
          </div>
          <div class="col-12">
            <label class="form-label">是否已採取防治措施及其效果</label>
            <textarea v-model.trim="form.hintDescription" class="form-control" rows="2"></textarea>
          </div>
        </div>
      </div>

      <!-- 防治建議與簽名 -->
      <div class="card shadow-sm mb-4">
        <div class="card-header bg-success text-white">防治建議與簽名</div>
        <div class="card-body row g-3">
          <div class="col-md-6">
            <label class="form-label">防治建議（可複選）</label>
            <div v-for="h in hints" :key="h.id" class="form-check">
              <input
                class="form-check-input"
                type="checkbox"
                :checked="form.hintIds.includes(h.id)"
                @change="toggle(form.hintIds, h.id)"
              />
              <span class="form-check-label">{{ h.name }}</span>
            </div>
          </div>
          <div class="col-md-6">
            <label class="form-label">診斷簽名人（可複選）</label>
            <div v-for="i in identifiers" :key="i.id" class="form-check">
              <input
                class="form-check-input"
                type="checkbox"
                :checked="form.identifierIds.includes(i.id)"
                @change="toggle(form.identifierIds, i.id)"
              />
              <span class="form-check-label">{{ i.name }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="d-flex gap-2 justify-content-end">
        <button
          v-if="auth.isStaff"
          type="button"
          class="btn btn-info text-white"
          :disabled="analyzing"
          @click="runAi"
        >
          {{ analyzing ? 'AI 診斷中…' : 'AI 診斷' }}
        </button>
        <router-link class="btn btn-outline-secondary" to="/cases">取消</router-link>
        <button type="submit" class="btn btn-success" :disabled="saving">
          {{ saving ? '儲存中…' : '儲存案件' }}
        </button>
      </div>
    </form>
  </div>
</template>
