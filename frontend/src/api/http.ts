import axios from 'axios'
import Swal from 'sweetalert2'

// axios 實例：統一 baseURL (/api) 與攔截器 (Interceptor)
const http = axios.create ({
  baseURL: '/api',
  timeout: 60000,
})

// 請求攔截器：若已登入，自動在 Header 附上 Bearer Token (JWT)
http.interceptors.request.use ((config) => {
  const token = localStorage.getItem ('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 回應攔截器：統一處理錯誤。401 視為「未登入/登入已過期」。
http.interceptors.response.use ((response) => response,
  (error) => {
     const status = error.response?.status
     const data = error.response?.data
     const message = data?.error?.message ?? error.message

     if (status === 429) {
       const retryAfter = error.response?.headers?.['retry-after']
         ?? error.response?.headers?.['Retry-After']
       const hint = retryAfter ? `（請 ${retryAfter} 秒後再試）` : ''
       // 429 限流不清除 token，避免無限重試
       Swal.fire ({ icon: 'warning', title: '請求過於頻繁', text: message + hint })
     } else if (status === 401) {
       // 清除本機登入狀態，並導回登入頁
       localStorage.removeItem ('token')
       localStorage.removeItem ('user')
       if (window.location.pathname !== '/login') {
         window.location.href = '/login'
       }
     } else {
       Swal.fire ({ icon: 'error', title: '操作失敗', text: message })
     }
     return Promise.reject (error)
   },)

export default http
