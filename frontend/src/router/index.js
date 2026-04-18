import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginPage.vue')
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue') },
      { path: 'transactions', name: 'Transactions', component: () => import('@/views/TransactionList.vue') },
      { path: 'transactions/add', name: 'AddTransaction', component: () => import('@/views/TransactionForm.vue') },
      { path: 'transactions/edit/:id', name: 'EditTransaction', component: () => import('@/views/TransactionForm.vue') },
      { path: 'accounts', name: 'Accounts', component: () => import('@/views/AccountList.vue') },
      { path: 'categories', name: 'Categories', component: () => import('@/views/CategoryList.vue') },
      { path: 'merchants', name: 'Merchants', component: () => import('@/views/MerchantList.vue') },
      { path: 'budget', name: 'Budget', component: () => import('@/views/BudgetPage.vue') },
      { path: 'recurring', name: 'Recurring', component: () => import('@/views/RecurringPage.vue') },
      { path: 'alert-rules', name: 'AlertRules', component: () => import('@/views/AlertRulePage.vue') },
      { path: 'alerts', name: 'Alerts', component: () => import('@/views/AlertList.vue') },
      { path: 'reports', name: 'Reports', component: () => import('@/views/ReportPage.vue') },
      { path: 'settings', name: 'Settings', component: () => import('@/views/SettingsPage.vue') },
      { path: 'users', name: 'Users', component: () => import('@/views/UserList.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard: redirect to login if not authenticated
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
