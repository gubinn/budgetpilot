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

const READONLY_ALLOWED = new Set([
  'Dashboard', 'Transactions', 'Accounts', 'PrepaidCards', 'Receivables', 'Reports'
])

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginPage.vue')
  },
  {
    path: '/share/:code',
    name: 'ShareAccess',
    component: () => import('@/views/ShareAccessPage.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: loadView('Layout'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: loadView('Dashboard') },
      { path: 'transactions', name: 'Transactions', component: loadView('TransactionList') },
      { path: 'reconciliation', name: 'Reconciliation', component: loadView('Reconciliation') },
      { path: 'transactions/add', name: 'AddTransaction', component: loadView('TransactionForm') },
      { path: 'transactions/edit/:id', name: 'EditTransaction', component: loadView('TransactionForm') },
      { path: 'accounts', name: 'Accounts', component: loadView('AccountList') },
      { path: 'cards', name: 'PrepaidCards', component: loadView('PrepaidCardList') },
      { path: 'receivables', name: 'Receivables', component: loadView('ReceivableList') },
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
  if (to.meta.public) {
    next()
    return
  }

  if (!authInit) {
    authInit = true
    const auth = useAuthStore()
    if (auth.token) {
      await auth.fetchUser()
    }
  }

  const token = localStorage.getItem('token')
  let readonly = null
  try {
    readonly = JSON.parse(localStorage.getItem('readonly') || 'null')
  } catch {
    readonly = null
  }

  if (readonly && !token) {
    next(`/share/${readonly.shareCode}`)
    return
  }
  if (readonly && !READONLY_ALLOWED.has(to.name)) {
    next({ name: 'Dashboard' })
    return
  }
  // 对账在手机端不提供入口，直接跳回交易列表
  if (to.name === 'Reconciliation' && window.innerWidth < 768) {
    next({ name: 'Transactions' })
    return
  }
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
