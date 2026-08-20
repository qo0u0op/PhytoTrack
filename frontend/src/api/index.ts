import http from './http'
import type { components } from '../types/api'

// 從 openapi-typescript 自動生成的型別取用（與後端契約一致）
type CaseCreateRequest = components['schemas']['CaseCreateRequest']
type CaseUpdateRequest = components['schemas']['CaseUpdateRequest']
type LoginRequest = components['schemas']['LoginRequest']
type RegisterRequest = components['schemas']['RegisterRequest']
type AnalyzeRequest = components['schemas']['AnalyzeRequest']

/** 認證相關 API */
export const authApi = {
  login: (data: LoginRequest) => http.post('/auth/login', data),
  register: (data: RegisterRequest) => http.post('/auth/register', data),
  me: () => http.post('/auth/me'),
  logout: () => http.post('/auth/logout'),
}

/** 案件（Case）API */
export const caseApi = {
  /** 分頁查詢案件列表；篩選參數皆可選，同時存在時為 AND 組合 */
  list: (params: {
    page?: number
    size?: number
    cropId?: number
    serviceId?: number
    senderName?: string
    receiveDateFrom?: string
    receiveDateTo?: string
    status?: 'PENDING' | 'RESOLVED' | 'CLOSED'
  }) => http.get('/cases', { params }),
  detail: (id: number) => http.get(`/cases/${id}`),
  create: (data: CaseCreateRequest) => http.post('/cases', data),
  update: (id: number, data: CaseUpdateRequest) => http.put(`/cases/${id}`, data),
  remove: (id: number) => http.delete(`/cases/${id}`),
  /** 案件統計總覽（登入即可）：總數、本月新增、待處理、topN、狀態比例、近 6 月趨勢 */
  statistics: () => http.get('/cases/statistics'),
}

/** 參照資料（Reference Data）API：診斷表單的下拉選單 */
export const refApi = {
  cropCategories: () => http.get('/ref/crop-categories'),
  pestTypes: () => http.get('/ref/pest-types'),
  damages: () => http.get('/ref/damages'),
  hints: () => http.get('/ref/hints'),
  methods: () => http.get('/ref/methods'),
  deliveries: () => http.get('/ref/deliveries'),
  services: () => http.get('/ref/services'),
  cities: () => http.get('/ref/cities'),
  senderTypes: () => http.get('/ref/sender-types'),
  identifiers: () => http.get('/ref/identifiers'),
}

/** AI 診斷 API（後端代理 llama.cpp） */
export const aiApi = {
  analyze: (data: AnalyzeRequest) => http.post('/ai/analyze', data),
  health: () => http.get('/ai/health'),
}

/** 使用者管理 API（限管理者） */
export const userApi = {
  list: () => http.get('/admin/users'),
}
