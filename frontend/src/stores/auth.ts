import { defineStore } from 'pinia'

// 登入使用者的資料形狀 (對應後端 UserResponse)
export interface User {
  userId: number
  username: string
  displayName: string
  email: string | null
  role: string
}

// Pinia 狀態管理：登入狀態存放於 localStorage，重新整理後仍保持登入
export const useAuthStore = defineStore ('auth', {
  state: () => ({
    token: localStorage.getItem ('token') as string | null,
    user: JSON.parse (localStorage.getItem ('user') ?? 'null') as User | null,
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    /** 是否為管理員 (ROLE_ADMIN) */
    isAdmin: (state) => state.user?.role === 'ROLE_ADMIN',
    /** 是否為員工層級以上 (STAFF 或 ADMIN)，可用 AI 診斷與建案 */
    isStaff: (state) => state.user?.role === 'ROLE_STAFF' || state.user?.role === 'ROLE_ADMIN',
    /** 是否為檢視者 (VIEWER) */
    isViewer: (state) => state.user?.role === 'ROLE_VIEWER',
  },
  actions: {
    /** 登入成功後寫入 token 與使用者 */
    setAuth (token: string, user: User) {
      this.token = token
      this.user = user
      localStorage.setItem ('token', token)
      localStorage.setItem ('user', JSON.stringify (user))
    },
    /** 登出：清除本機狀態 */
    logout () {
      this.token = null
      this.user = null
      localStorage.removeItem ('token')
      localStorage.removeItem ('user')
    },
  },
})
