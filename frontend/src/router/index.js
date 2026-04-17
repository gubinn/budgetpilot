import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue') },
      { path: 'transactions', name: 'Transactions', component: () => import('@/views/TransactionList.vue') },
      { path: 'transactions/add', name: 'AddTransaction', component: () => import('@/views/TransactionForm.vue') },
      { path: 'transactions/edit/:id', name: 'EditTransaction', component: () => import('@/views/TransactionForm.vue') },
      { path: 'accounts', name: 'Accounts', component: () => import('@/views/AccountList.vue') },
      { path: 'categories', name: 'Categories', component: () => import('@/views/CategoryList.vue') },
      { path: 'budget', name: 'Budget', component: () => import('@/views/BudgetPage.vue') },
      { path: 'recurring', name: 'Recurring', component: () => import('@/views/RecurringPage.vue') },
      { path: 'reports', name: 'Reports', component: () => import('@/views/ReportPage.vue') },
      { path: 'settings', name: 'Settings', component: () => import('@/views/SettingsPage.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
