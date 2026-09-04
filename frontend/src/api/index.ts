import http from './http'
import type { components } from '../types/api'

// 從 openapi-typescript 自動生成的型別取用 (與後端契約一致)
type CaseCreateRequest = components['schemas']['CaseCreateRequest']
type CaseUpdateRequest = components['schemas']['CaseUpdateRequest']
type LoginRequest = components['schemas']['LoginRequest']
type RegisterRequest = components['schemas']['RegisterRequest']
type AnalyzeRequest = components['schemas']['AnalyzeRequest']

/** 前後端共用信箱格式（與後端 @Email 語意對齊） */
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** 認證相關 API */
export const authApi = {
  login: (data: LoginRequest) => http.post ('/auth/login', data),
  register: (data: RegisterRequest) => http.post ('/auth/register', data),
  me: () => http.post ('/auth/me'),
  logout: () => http.post ('/auth/logout'),
  abandonDeactivate: (data: LoginRequest) => http.post ('/auth/abandon-deactivate', data),
  checkUsername: (username: string) => http.get ('/auth/check-username', { params: { username } }),
  checkEmail: (email: string) => http.get ('/auth/check-email', { params: { email } }),
}

/** 案件 (Case) API */
export const caseApi = {
  /** 分頁查詢案件列表；篩選參數皆可選，同時存在時為 AND 組合 (經 v_case_search 視圖) */
  list: (params: {
    page?: number
    size?: number
    cropId?: number
    serviceId?: number
    senderName?: string
    senderQuery?: string
    receiveDateFrom?: string
    receiveDateTo?: string
    status?: 'PENDING' | 'RESOLVED' | 'CLOSED'
    cityId?: number
    districtId?: number
    cropCategoryId?: number
    pestTypeId?: number
    pestCategoryId?: number
    hintId?: number
    deliveryId?: number
    damageId?: number
  }) => http.get ('/cases', { params }),
  detail: (id: number) => http.get (`/cases/${id}`),
  create: (data: CaseCreateRequest) => http.post ('/cases', data),
  update: (id: number, data: CaseUpdateRequest) => http.put (`/cases/${id}`, data),
  remove: (id: number) => http.delete (`/cases/${id}`),
  /** 案件統計總覽 (登入即可)：總數、本月新增、待處理、topN、狀態比例、近 6 月趨勢，支援期別 */
  statistics: (params?: { period?: string; year?: number; month?: number }) => http.get ('/cases/statistics', { params }),
  /** 匯出案件 CSV (登入即可)：以 blob 下載，含 UTF-8 BOM；篩選參數同列表 (可省略＝全量) */
  exportCsv: (params?: {
    cropId?: number
    serviceId?: number
    senderName?: string
    senderQuery?: string
    receiveDateFrom?: string
    receiveDateTo?: string
    status?: 'PENDING' | 'RESOLVED' | 'CLOSED'
    cityId?: number
    districtId?: number
    cropCategoryId?: number
    pestTypeId?: number
    pestCategoryId?: number
    hintId?: number
    deliveryId?: number
    damageId?: number
  }) => http.get ('/cases/export', { params, responseType: 'blob' }),
}

/** 參照資料 (Reference Data) API：診斷表單的下拉選單 */
export const refApi = {
  cropCategories: () => http.get ('/ref/crop-categories'),
  pestTypes: () => http.get ('/ref/pest-types'),
  damages: () => http.get ('/ref/damages'),
  hints: () => http.get ('/ref/hints'),
  methods: () => http.get ('/ref/methods'),
  deliveries: () => http.get ('/ref/deliveries'),
  services: () => http.get ('/ref/services'),
  cities: () => http.get ('/ref/cities'),
  senderTypes: () => http.get ('/ref/sender-types'),
  identifiers: (includeInactive?: boolean) => http.get ('/ref/identifiers', { params: includeInactive ? { includeInactive: true } : {} }),
  myIdentifier: () => http.get ('/ref/identifiers/me'),
  updateMyIdentifierActive: (id: number, active: boolean) => http.patch (`/ref/identifiers/${id}/active`, { active }),
}

/** AI 診斷 API (後端代理 llama.cpp) */
export const aiApi = {
  analyze: (data: AnalyzeRequest) => http.post ('/ai/analyze', data),
  health: () => http.get ('/ai/health'),
}

/** 使用者管理 API (限管理者) */
export const userApi = {
  list: () => http.get ('/admin/users'),
  updateRole: (id: number, role: string, opts?: { bindIdentifierId?: number; force?: boolean }) =>
    http.patch (`/admin/users/${id}/role`, { role, ...opts }),
  updateActive: (id: number, active: boolean) =>
    http.patch (`/admin/users/${id}/active`, { active }),
  resetPassword: (id: number, newPassword: string) =>
    http.post (`/admin/users/${id}/reset-password`, { newPassword }),
}

/** 送件人管理 API */
export const senderApi = {
  search: (q: string) => http.get ('/senders/search', { params: { q } }),
  list: () => http.get ('/senders'),
  detail: (id: number) => http.get (`/senders/${id}`),
  /** 建立 (STAFF+)；地址選填 */
  create: (data: { name?: string; displayName?: string; phone?: string; address?: string; districtId: number; senderTypeId: number }) =>
    http.post ('/senders', data),
  /** 編輯 (STAFF+)；地址選填 */
  update: (id: number, data: { name?: string; displayName?: string; phone?: string; address?: string; districtId: number; senderTypeId: number }) =>
    http.put (`/senders/${id}`, data),
  remove: (id: number) => http.delete (`/senders/${id}`),
}

/** 參照資料管理 API (限管理者) */
export const refAdminApi = {
  // damages
  createDamage: (data: { name: string }) => http.post ('/admin/ref/damages', data),
  updateDamage: (id: number, data: { name: string }) => http.put (`/admin/ref/damages/${id}`, data),
  deleteDamage: (id: number) => http.delete (`/admin/ref/damages/${id}`),
  // hints
  createHint: (data: { name: string }) => http.post ('/admin/ref/hints', data),
  updateHint: (id: number, data: { name: string }) => http.put (`/admin/ref/hints/${id}`, data),
  deleteHint: (id: number) => http.delete (`/admin/ref/hints/${id}`),
  // methods
  createMethod: (data: { name: string }) => http.post ('/admin/ref/methods', data),
  updateMethod: (id: number, data: { name: string }) => http.put (`/admin/ref/methods/${id}`, data),
  deleteMethod: (id: number) => http.delete (`/admin/ref/methods/${id}`),
  // deliveries
  createDelivery: (data: { name: string }) => http.post ('/admin/ref/deliveries', data),
  updateDelivery: (id: number, data: { name: string }) => http.put (`/admin/ref/deliveries/${id}`, data),
  deleteDelivery: (id: number) => http.delete (`/admin/ref/deliveries/${id}`),
  // services
  createService: (data: { name: string }) => http.post ('/admin/ref/services', data),
  updateService: (id: number, data: { name: string }) => http.put (`/admin/ref/services/${id}`, data),
  deleteService: (id: number) => http.delete (`/admin/ref/services/${id}`),
  // identifiers
  createIdentifier: (data: { name: string }) => http.post ('/admin/ref/identifiers', data),
  updateIdentifier: (id: number, data: { name: string }) => http.put (`/admin/ref/identifiers/${id}`, data),
  deleteIdentifier: (id: number) => http.delete (`/admin/ref/identifiers/${id}`),
  updateIdentifierActive: (id: number, active: boolean) => http.patch (`/admin/ref/identifiers/${id}/active`, { active }),
  bindIdentifier: (id: number, userId: number) => http.post (`/admin/ref/identifiers/${id}/bind`, { userId }),
  // sender-types
  createSenderType: (data: { name: string }) => http.post ('/admin/ref/sender-types', data),
  updateSenderType: (id: number, data: { name: string }) => http.put (`/admin/ref/sender-types/${id}`, data),
  deleteSenderType: (id: number) => http.delete (`/admin/ref/sender-types/${id}`),
  // crops
  createCrop: (data: { name: string; cropCategoryId: number }) => http.post ('/admin/ref/crops', data),
  updateCrop: (id: number, data: { name: string; cropCategoryId: number }) =>
    http.put (`/admin/ref/crops/${id}`, data),
  deleteCrop: (id: number) => http.delete (`/admin/ref/crops/${id}`),
  // crop-categories
  createCropCategory: (data: { name: string }) => http.post ('/admin/ref/crop-categories', data),
  updateCropCategory: (id: number, data: { name: string }) =>
    http.put (`/admin/ref/crop-categories/${id}`, data),
  deleteCropCategory: (id: number) => http.delete (`/admin/ref/crop-categories/${id}`),
  // pest-categories
  createPestCategory: (data: { code: string; name: string; pestTypeId: number }) =>
    http.post ('/admin/ref/pest-categories', data),
  updatePestCategory: (id: number, data: { code: string; name: string; pestTypeId: number }) =>
    http.put (`/admin/ref/pest-categories/${id}`, data),
  deletePestCategory: (id: number) => http.delete (`/admin/ref/pest-categories/${id}`),
}

/** 帳號自助管理 API */
export const accountApi = {
  getProfile: () => http.get ('/account'),
  updateProfile: (data: { displayName: string; email?: string | null }) => http.put ('/account/profile', data),
  checkEmail: (email: string) => http.get ('/account/check-email', { params: { email } }),
  changePassword: (data: { currentPassword?: string; newPassword: string }) => http.put ('/account/password', data),
  requestDeactivate: () => http.post ('/account/deactivate-request'),
  getMyDeactivateRequest: () => http.get ('/account/deactivate-request'),
  cancelDeactivate: () => http.delete ('/account/deactivate-request'),
  listDeactivateRequests: () => http.get ('/admin/deactivate-requests'),
  reviewDeactivateRequest: (id: number, status: string) => http.put (`/admin/deactivate-requests/${id}`, { status }),
}
