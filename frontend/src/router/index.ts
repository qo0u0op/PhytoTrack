import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

// 路由表：使用動態 import (Lazy Loading)，進入頁面時才載入元件
const router = createRouter ({
  history: createWebHistory (),
  routes: [
    { path: '/', name: 'home', component: () => import ('../views/HomeView.vue') },
    {
      path: '/login',
      name: 'login',
      component: () => import ('../views/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import ('../views/RegisterView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import ('../views/DashboardView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/cases',
      name: 'cases',
      component: () => import ('../views/CasesView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/cases/new',
      name: 'case-new',
      component: () => import ('../views/CaseFormView.vue'),
      meta: { requiresAuth: true, staffOnly: true },
    },
    {
      path: '/cases/:id',
      name: 'case-detail',
      component: () => import ('../views/CaseDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/cases/:id/edit',
      name: 'case-edit',
      component: () => import ('../views/CaseFormView.vue'),
      meta: { requiresAuth: true, staffOnly: true },
    },
    {
      path: '/users',
      name: 'users',
      component: () => import ('../views/UsersView.vue'),
      meta: { requiresAuth: true, adminOnly: true },
    },
    {
      path: '/admin/reference-data',
      name: 'reference-data-admin',
      component: () => import ('../views/ReferenceDataAdminView.vue'),
      meta: { requiresAuth: true, adminOnly: true },
    },
    {
      path: '/admin/senders',
      name: 'senders-admin',
      component: () => import ('../views/SendersView.vue'),
      meta: { requiresAuth: true, staffOnly: true },
    },
    {
      path: '/admin/senders/:id/edit',
      name: 'sender-edit',
      component: () => import ('../views/SenderEditView.vue'),
      meta: { requiresAuth: true, staffOnly: true },
    },
    {
      path: '/admin/crops',
      name: 'crops-admin',
      component: () => import ('../views/CropManagementView.vue'),
      meta: { requiresAuth: true, staffOnly: true },
    },
    {
      path: '/admin/pest-categories',
      name: 'pest-categories-admin',
      component: () => import ('../views/PestManagementView.vue'),
      meta: { requiresAuth: true, adminOnly: true },
    },
    {
      path: '/account',
      name: 'account',
      component: () => import ('../views/AccountView.vue'),
      meta: { requiresAuth: true },
    },
    { path: '/:pathMatch (.*)*', redirect: '/' },
  ],
})

// 全域守衛 (Navigation Guard)：做登入與角色權限控管
router.beforeEach ((to) => {
  const auth = useAuthStore ()
  // 需登入的頁面：未登入則導向登入頁，並記住原本想去的路徑
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // 登入頁/註冊頁：已登入者直接進儀表板
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: 'dashboard' }
  }
  // 員工權限 (STAFF+)：僅限員工層級以上
  if (to.meta.staffOnly && !auth.isStaff) {
    return { name: 'dashboard' }
  }
  // 管理員權限：僅限 ADMIN
  if (to.meta.adminOnly && !auth.isAdmin) {
    return { name: 'dashboard' }
  }
})

export default router
