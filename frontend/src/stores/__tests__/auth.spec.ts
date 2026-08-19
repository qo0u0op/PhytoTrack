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

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('未登入時 isAuthenticated / isAdmin / isStaff 皆為 false', () => {
    const store = useAuthStore()
    expect(store.isAuthenticated).toBe(false)
    expect(store.isAdmin).toBe(false)
    expect(store.isStaff).toBe(false)
  })

  it('setAuth 寫入 token 與使用者，並同步到 localStorage', () => {
    const store = useAuthStore()
    store.setAuth('jwt-token', adminUser)

    expect(store.token).toBe('jwt-token')
    expect(store.user).toEqual(adminUser)
    expect(store.isAuthenticated).toBe(true)
    expect(store.isAdmin).toBe(true)
    expect(store.isStaff).toBe(true)
    expect(localStorage.getItem('token')).toBe('jwt-token')
    expect(localStorage.getItem('user')).toBe(JSON.stringify(adminUser))
  })

  it('STAFF 角色：isStaff true、isAdmin false', () => {
    const store = useAuthStore()
    store.setAuth('jwt-token', staffUser)

    expect(store.isStaff).toBe(true)
    expect(store.isAdmin).toBe(false)
  })

  it('logout 清除狀態與 localStorage', () => {
    const store = useAuthStore()
    store.setAuth('jwt-token', adminUser)

    store.logout()

    expect(store.token).toBeNull()
    expect(store.user).toBeNull()
    expect(store.isAuthenticated).toBe(false)
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })

  it('從 localStorage 恢復登入狀態（重新整理後仍保持）', () => {
    localStorage.setItem('token', 'persisted-token')
    localStorage.setItem('user', JSON.stringify(staffUser))

    const store = useAuthStore()

    expect(store.isAuthenticated).toBe(true)
    expect(store.user?.role).toBe('ROLE_STAFF')
    expect(store.isStaff).toBe(true)
  })
})