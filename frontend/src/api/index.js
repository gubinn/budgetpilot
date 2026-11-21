import api from './request'

export const accountApi = {
  list: (active = true) => api.get('/accounts', { params: { active } }),
  get: (id) => api.get(`/accounts/${id}`),
  create: (data) => api.post('/accounts', data),
  update: (id, data) => api.patch(`/accounts/${id}`, data),
  remove: (id) => api.delete(`/accounts/${id}`),
  adjustBalance: (id, data) => api.post(`/accounts/${id}/adjust-balance`, data),
  totalAssets: () => api.get('/accounts/total-assets')
}

export const transactionApi = {
  create: (data) => api.post('/transactions', data),
  list: (params) => api.get('/transactions', { params }),
  get: (id) => api.get(`/transactions/${id}`),
  update: (id, data) => api.put(`/transactions/${id}`, data),
  remove: (id) => api.delete(`/transactions/${id}`),
  confirm: (id) => api.post(`/transactions/${id}/confirm`),
  tags: () => api.get('/transactions/tags'),
  extKeys: () => api.get('/transactions/ext-keys')
}

export const categoryApi = {
  tree: (type) => api.get('/categories/tree', { params: { type } }),
  get: (id) => api.get(`/categories/${id}`),
  create: (data) => api.post('/categories', data),
  update: (id, data) => api.put(`/categories/${id}`, data),
  remove: (id) => api.delete(`/categories/${id}`),
  merge: (sourceId, targetId) => api.post(`/categories/${sourceId}/merge/${targetId}`)
}

export const budgetApi = {
  get: (yearMonth) => api.get(`/budgets/${yearMonth}`),
  create: (data) => api.post('/budgets', data),
  update: (yearMonth, data) => api.put(`/budgets/${yearMonth}`, data),
  copy: (yearMonth, sourceMonth) => api.post(`/budgets/${yearMonth}/copy-from/${sourceMonth}`),
  progress: (yearMonth) => api.get(`/budgets/${yearMonth}/progress`),
  fixedSummary: (month) => api.get('/budgets/fixed-summary', { params: { month } })
}

export const annualBudgetApi = {
  get: (year) => api.get(`/annual-budgets/${year}`),
  create: (data) => api.post('/annual-budgets', data),
  update: (year, data) => api.put(`/annual-budgets/${year}`, data),
  progress: (year) => api.get(`/annual-budgets/${year}/progress`)
}

export const recurringApi = {
  list: () => api.get('/recurring-rules'),
  get: (id) => api.get(`/recurring-rules/${id}`),
  create: (data) => api.post('/recurring-rules', data),
  update: (id, data) => api.put(`/recurring-rules/${id}`, data),
  remove: (id) => api.delete(`/recurring-rules/${id}`),
  toggle: (id) => api.post(`/recurring-rules/${id}/toggle`),
  execute: (id) => api.post(`/recurring-rules/${id}/execute`)
}

export const reportApi = {
  monthlySummary: (month, level = 1) => api.get('/reports/monthly-summary', { params: { month, level } }),
  categoryDetail: (month, categoryId) => api.get('/reports/category-detail', { params: { month, categoryId } }),
  trend: (months = 12, type) => api.get('/reports/trend', { params: { months, type } }),
  compare: (month, compareWith) => api.get('/reports/compare', { params: { month, compareWith } }),
  accountSummary: () => api.get('/reports/account-summary'),
  dailyHeatmap: (year) => api.get('/reports/daily-heatmap', { params: { year } }),
  budgetReview: (month) => api.get('/reports/budget-review', { params: { month } }),
  currencyDistribution: (month) => api.get('/reports/currency-distribution', { params: { month } }),
  merchantDistribution: (month) => api.get('/reports/merchant-distribution', { params: { month } }),
  extFieldDistribution: (month, key, type = 1) => api.get('/reports/ext-field-distribution', { params: { month, key, type } })
}

export const merchantApi = {
  list: (params) => api.get('/merchants', { params }),
  page: (params) => api.get('/merchants/page', { params }),
  get: (id) => api.get(`/merchants/${id}`),
  create: (data) => api.post('/merchants', data),
  update: (id, data) => api.put(`/merchants/${id}`, data),
  remove: (id) => api.delete(`/merchants/${id}`),
  consolidate: () => api.post('/merchants/consolidate'),
  search: (keyword, limit = 10) => api.get('/merchants/search', { params: { keyword, limit } })
}

export const systemApi = {
  config: () => api.get('/system/config'),
  setConfig: (key, value) => api.put(`/system/config/${key}`, { value }),
  currencies: () => api.get('/system/currencies'),
  rates: (date) => api.get('/system/rates', { params: { date } }),
  refreshRates: () => api.post('/system/rates/refresh'),
  alerts: () => api.get('/system/alerts'),
  markAlertRead: (id) => api.put(`/system/alerts/${id}/read`),
  testTelegram: () => api.post('/system/telegram/test'),
  testWhatsApp: () => api.post('/system/whatsapp/test')
}

export const userApi = {
  list: () => api.get('/users'),
  create: (data) => api.post('/users', data),
  update: (id, data) => api.patch(`/users/${id}`, data),
  remove: (id) => api.delete(`/users/${id}`),
  resetPassword: (id, newPassword) => api.put(`/users/${id}/password`, { password: newPassword }),
  getMyConfig: () => api.get('/users/config'),
  setMyConfig: (key, value) => api.put(`/users/config/${key}`, { value })
}

export const authApi = {
  login: (data) => api.post('/auth/login', data),
  verifyOtp: (data) => api.post('/auth/verify', data),
  resendOtp: (data) => api.post('/auth/otp/resend', data),
  ingress: () => api.get('/auth/ingress'),
  logout: () => api.post('/auth/logout'),
  info: () => api.get('/auth/info'),
  changePassword: (data) => api.post('/auth/change-password', data),
  getApiKey: () => api.get('/auth/api-key'),
  generateApiKey: () => api.post('/auth/api-key/generate')
}
