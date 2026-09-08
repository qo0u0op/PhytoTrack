import { defineStore } from 'pinia'

// 登入使用者的資料形狀 (對應後端 UserResponse)
export interface User {
  userId: number
  username: string
  displayName: string
  email: string | null
  role: string
}

function readStored (key: string): string | null {
  // localStorage 優先（記住我），回落 sessionStorage（單次會話）
  return localStorage.getItem (key) ?? sessionStorage.getItem (key)
}

// Pinia 狀態管理：勾選記住我存 localStorage，未勾選存 sessionStorage
// lastUsername 恆存 localStorage，登出不清除，供登入頁自動帶入
export const useAuthStore = defineStore ('auth', {
  state: () => ({
    token: readStored ('token') as string | null,
    user: JSON.parse (readStored ('user') ?? 'null') as User | null,
    lastUsername: localStorage.getItem ('lastUsername') as string | null,
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
    /** 登入成功後寫入 token 與使用者（rememberMe=true 存 localStorage，否則 sessionStorage） */
    setAuth (token: string, user: User, rememberMe = false) {
      this.token = token
      this.user = user
      this.lastUsername = user.username
      localStorage.setItem ('lastUsername', user.username)
      const primary = rememberMe ? localStorage : sessionStorage
      const secondary = rememberMe ? sessionStorage : localStorage
      primary.setItem ('token', token)
      primary.setItem ('user', JSON.stringify (user))
      secondary.removeItem ('token')
      secondary.removeItem ('user')
    },
    /** 登出：清除兩處 token/使用者，但保留 lastUsername 供下次帶入 */
    logout () {
      this.token = null
      this.user = null
      localStorage.removeItem ('token')
      localStorage.removeItem ('user')
      sessionStorage.removeItem ('token')
      sessionStorage.removeItem ('user')
    },
  },
})
