import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore, type User } from '../auth'

const adminUser: User = {
  userId: 1,
  username: 'admin',
  displayName: '管理者',
  email: null,
  role: 'ROLE_ADMIN',
}

const staffUser: User = {
  userId: 2,
  username: 'staff',
  displayName: '診斷員',
  email: null,
  role: 'ROLE_STAFF',
}

describe ('auth store', () => {
  beforeEach (() => {
    localStorage.clear ()
    sessionStorage.clear ()
    setActivePinia (createPinia ())
  })

  it ('未登入時 isAuthenticated / isAdmin / isStaff 皆為 false', () => {
    const store = useAuthStore ()
    expect (store.isAuthenticated).toBe (false)
    expect (store.isAdmin).toBe (false)
    expect (store.isStaff).toBe (false)
  })

  it ('setAuth 預設寫入 sessionStorage（未勾選記住我）', () => {
    const store = useAuthStore ()
    store.setAuth ('jwt-token', adminUser)

    expect (store.token).toBe ('jwt-token')
    expect (store.user).toEqual (adminUser)
    expect (store.isAuthenticated).toBe (true)
    expect (store.isAdmin).toBe (true)
    expect (store.isStaff).toBe (true)
    expect (sessionStorage.getItem ('token')).toBe ('jwt-token')
    expect (sessionStorage.getItem ('user')).toBe (JSON.stringify (adminUser))
    expect (localStorage.getItem ('token')).toBeNull ()
  })

  it ('STAFF 角色：isStaff true、isAdmin false', () => {
    const store = useAuthStore ()
    store.setAuth ('jwt-token', staffUser)

    expect (store.isStaff).toBe (true)
    expect (store.isAdmin).toBe (false)
  })

  it ('logout 清除狀態與 localStorage', () => {
    const store = useAuthStore ()
    store.setAuth ('jwt-token', adminUser)

    store.logout ()

    expect (store.token).toBeNull ()
    expect (store.user).toBeNull ()
    expect (store.isAuthenticated).toBe (false)
    expect (localStorage.getItem ('token')).toBeNull ()
    expect (localStorage.getItem ('user')).toBeNull ()
    expect (sessionStorage.getItem ('token')).toBeNull ()
    expect (sessionStorage.getItem ('user')).toBeNull ()
  })

  it ('rememberMe=false 寫入 sessionStorage 並清 localStorage', () => {
    const store = useAuthStore ()
    store.setAuth ('jwt-token', adminUser, false)

    expect (sessionStorage.getItem ('token')).toBe ('jwt-token')
    expect (sessionStorage.getItem ('user')).toBe (JSON.stringify (adminUser))
    expect (localStorage.getItem ('token')).toBeNull ()
    expect (localStorage.getItem ('user')).toBeNull ()
  })

  it ('rememberMe=true 寫入 localStorage 並清 sessionStorage', () => {
    const store = useAuthStore ()
    sessionStorage.setItem ('token', 'stale-token')
    sessionStorage.setItem ('user', JSON.stringify (staffUser))
    store.setAuth ('jwt-token', adminUser, true)

    expect (localStorage.getItem ('token')).toBe ('jwt-token')
    expect (localStorage.getItem ('user')).toBe (JSON.stringify (adminUser))
    expect (sessionStorage.getItem ('token')).toBeNull ()
    expect (sessionStorage.getItem ('user')).toBeNull ()
  })

  it ('從 sessionStorage 恢復登入狀態 (未勾選記住我)', () => {
    sessionStorage.setItem ('token', 'session-token')
    sessionStorage.setItem ('user', JSON.stringify (staffUser))

    const store = useAuthStore ()

    expect (store.isAuthenticated).toBe (true)
    expect (store.user?.role).toBe ('ROLE_STAFF')
  })

  it ('登出保留 lastUsername（供登入頁帶入）', () => {
    const store = useAuthStore ()
    store.setAuth ('jwt-token', adminUser, true)

    store.logout ()

    expect (store.token).toBeNull ()
    expect (store.lastUsername).toBe ('admin')
    expect (localStorage.getItem ('lastUsername')).toBe ('admin')
  })

  it ('setAuth 更新 lastUsername 為最新登入者', () => {
    const store = useAuthStore ()
    store.setAuth ('t1', adminUser)
    store.setAuth ('t2', staffUser)

    expect (store.lastUsername).toBe ('staff')
    expect (localStorage.getItem ('lastUsername')).toBe ('staff')
  })

  it ('localStorage 優先於 sessionStorage', () => {
    localStorage.setItem ('token', 'local-token')
    localStorage.setItem ('user', JSON.stringify (adminUser))
    sessionStorage.setItem ('token', 'session-token')
    sessionStorage.setItem ('user', JSON.stringify (staffUser))

    const store = useAuthStore ()

    expect (store.token).toBe ('local-token')
    expect (store.user?.role).toBe ('ROLE_ADMIN')
  })

  it ('從 localStorage 恢復登入狀態 (重新整理後仍保持)', () => {
    localStorage.setItem ('token', 'persisted-token')
    localStorage.setItem ('user', JSON.stringify (staffUser))

    const store = useAuthStore ()

    expect (store.isAuthenticated).toBe (true)
    expect (store.user?.role).toBe ('ROLE_STAFF')
    expect (store.isStaff).toBe (true)
  })
})