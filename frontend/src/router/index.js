import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

function loadView(name) {
  return () => {
    const isMobile = window.innerWidth < 768
    return isMobile
      ? import(`@/views/mobile/${name}.vue`)
      : import(`@/views/${name}.vue`)
  }
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginPage.vue')
  },
  {
    path: '/',
    component: loadView('Layout'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: loadView('Dashboard') },
      { path: 'transactions', name: 'Transactions', component: loadView('TransactionList') },
      { path: 'transactions/add', name: 'AddTransaction', component: loadView('TransactionForm') },
      { path: 'transactions/edit/:id', name: 'EditTransaction', component: loadView('TransactionForm') },
      { path: 'accounts', name: 'Accounts', component: loadView('AccountList') },
      { path: 'categories', name: 'Categories', component: loadView('CategoryList') },
      { path: 'merchants', name: 'Merchants', component: loadView('MerchantList') },
      { path: 'budget', name: 'Budget', component: loadView('BudgetPage') },
      { path: 'recurring', name: 'Recurring', component: loadView('RecurringPage') },
      { path: 'alert-rules', name: 'AlertRules', component: loadView('AlertRulePage') },
      { path: 'alerts', name: 'Alerts', component: loadView('AlertList') },
      { path: 'reports', name: 'Reports', component: loadView('ReportPage') },
      { path: 'settings', name: 'Settings', component: loadView('SettingsPage') },
      { path: 'users', name: 'Users', component: loadView('UserList') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

let authInit = false

router.beforeEach(async (to, from, next) => {
  if (!authInit) {
    authInit = true
    const auth = useAuthStore()
    if (auth.token) {
      await auth.fetchUser()
    }
  }

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
