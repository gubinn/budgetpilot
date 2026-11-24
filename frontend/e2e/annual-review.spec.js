import { test, expect } from '@playwright/test';

const BASE = 'http://127.0.0.1:6060';

const annualData = {
  period: {
    year: 2026,
    label: '2026年',
    startDate: '2026-01-01',
    endDate: '2026-11-24',
    monthsCovered: 11,
    partialYear: true,
    partialMonth: true,
    daysCovered: 328,
    compareYear: 2025,
    compareLabel: '2025年同期（1–11月）',
    compareStartDate: '2025-01-01',
    compareEndDate: '2025-11-24',
    compareSamePeriod: true
  },
  kpis: {
    income: 300000,
    expense: 180000,
    net: 120000,
    savingsRate: 40.0,
    avgMonthlyIncome: 27272.73,
    avgMonthlyExpense: 16363.64,
    avgMonthlyNet: 10909.09,
    transactionCount: 500,
    spendingDays: 260,
    activityDays: 328,
    noSpendDays: 68,
    prevIncome: null,
    prevExpense: null,
    prevNet: null,
    prevSavingsRate: null,
    incomeChangePct: null,
    expenseChangePct: null,
    netChangePct: null,
    savingsRateChangePp: null
  },
  monthly: Array.from({ length: 12 }, (_, i) => ({
    month: `2026-${String(i + 1).padStart(2, '0')}`,
    label: `${i + 1}月`,
    income: i < 11 ? 25000 : 0,
    expense: i < 11 ? 15000 : 0,
    net: i < 11 ? 10000 : 0,
    savingsRate: i < 11 ? 40 : 0,
    daily: i < 11 ? 9000 : 0,
    fixed: i < 11 ? 5000 : 0,
    special: i < 11 ? 1000 : 0,
    cumulativeNet: i < 11 ? (i + 1) * 10000 : 110000,
    prevIncome: null,
    prevExpense: null,
    future: i === 11,
    hasData: i < 11
  })),
  bestMonth: { month: '2026-03', label: '3月', net: 20000, income: 30000, expense: 10000 },
  worstMonth: { month: '2026-06', label: '6月', net: 5000, income: 20000, expense: 15000 },
  scopes: [
    { scope: 'DAILY', amount: 110000, share: 61.1, count: 400, avgPerMonth: 10000 },
    { scope: 'FIXED', amount: 50000, share: 27.8, count: 40, avgPerMonth: 4545.45 },
    { scope: 'SPECIAL', amount: 20000, share: 11.1, count: 10, avgPerMonth: 1818.18 }
  ],
  categories: [
    { categoryId: 1, name: '餐饮', color: '#f97316', scope: 'DAILY', amount: 60000, share: 33.3, count: 220, avgPerMonth: 5454.55 },
    { categoryId: 2, name: '房租', color: '#2a78d6', scope: 'FIXED', amount: 50000, share: 27.8, count: 11, avgPerMonth: 4545.45 },
    { categoryId: 3, name: '旅行', color: '#1baf7a', scope: 'SPECIAL', amount: 20000, share: 11.1, count: 3, avgPerMonth: 1818.18 }
  ],
  incomeCategories: [
    { categoryId: 10, name: '工资', color: '#0ea5a4', amount: 280000, share: 93.3, count: 11, avgPerMonth: 25454.55 },
    { categoryId: 11, name: '理财', color: '#2a78d6', amount: 20000, share: 6.7, count: 5, avgPerMonth: 1818.18 }
  ],
  netWorth: {
    startMonth: '2025-12',
    startLabel: '2025年12月',
    endMonth: '2026-11',
    endLabel: '2026年11月',
    startNetWorth: 800000,
    endNetWorth: 920000,
    change: 120000,
    changePct: 15.0,
    annualNet: 120000,
    otherChange: 0,
    assets: 1000000,
    liabilities: 80000,
    liquidAssets: 300000,
    essentialMonthlyExpense: 12000,
    emergencyFundMonths: 25.0,
    netWorthAvailable: true,
    trend: Array.from({ length: 12 }, (_, i) => ({
      month: i === 0 ? '2025-12' : `2026-${String(i).padStart(2, '0')}`,
      netWorth: 800000 + i * 10909
    })),
    groups: [
      { type: '储蓄卡', balance: 300000 },
      { type: '投资账户', balance: 700000 },
      { type: '信用卡', balance: -80000 }
    ]
  },
  annualBudget: {
    exists: true,
    year: 2026,
    total: 200000,
    spent: 150000,
    remaining: 50000,
    pct: 75.0,
    timePct: 90.0,
    items: [
      { categoryId: 2, name: '房租', amount: 60000, spent: 55000, remaining: 5000, pct: 91.7 },
      { categoryId: 3, name: '旅行', amount: 40000, spent: 20000, remaining: 20000, pct: 50.0 }
    ]
  },
  monthlyBudget: {
    exists: true,
    monthsWithBudget: 2,
    monthsOver: 1,
    monthsCovered: 11,
    budgetTotal: 30000,
    spentTotal: 32000,
    pct: 106.7,
    items: [
      { month: '2026-01', budget: 10000, spent: 12000, pct: 120.0, over: true },
      { month: '2026-02', budget: 20000, spent: 20000, pct: 100.0, over: false }
    ]
  },
  merchants: [
    { merchantId: 1, name: '示例商户', amount: 10000, count: 20, share: 5.6, categoryName: '餐饮' }
  ],
  largestExpense: {
    date: '2026-03-01',
    amount: 8000,
    note: '示例支出',
    categoryName: '旅行',
    merchantName: '示例商户'
  },
  dataStates: {
    partialYear: true,
    noData: false,
    multiCurrency: false,
    annualBudgetMissing: false,
    monthlyBudgetMissing: false,
    monthsWithData: 11,
    compareAvailable: false,
    compareInsufficient: false,
    netWorthAvailable: true
  },
  insights: [
    { tone: 'good', title: '储蓄率 40.0%', text: '全年结余 ¥120,000，收支结构健康。' },
    { tone: 'neutral', title: '净资产增加 ¥120,000', text: '从 ¥800,000 到 ¥920,000。' },
    { tone: 'good', title: '应急金约 25.0 个月', text: '可随时动用 ¥300,000。' }
  ]
};

const monthlyData = {
  period: {
    label: '2026年11月',
    startDate: '2026-11-01',
    endDate: '2026-11-30',
    days: 30,
    partialMonth: true
  },
  kpis: {
    income: 0,
    expense: 0,
    expenseAll: 0,
    net: 0,
    savingsRate: 0,
    avgDaily: 0,
    transactionCount: 0
  },
  scopes: [
    { scope: 'DAILY', amount: 0, share: 0, count: 0 },
    { scope: 'FIXED', amount: 0, share: 0, count: 0 },
    { scope: 'SPECIAL', amount: 0, share: 0, count: 0 }
  ],
  categories: [],
  trend: [],
  daily: [],
  merchants: [],
  budget: { exists: false, items: [] },
  annualBudget: { items: [] },
  accounts: {
    netWorth: 0,
    assets: 0,
    liabilities: 0,
    groups: [],
    accounts: [],
    netWorthTrend: []
  },
  dataStates: { budgetMissing: true, partialMonth: false, multiCurrency: false, noData: true },
  insights: []
};

async function preparePage(page) {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'e2e-token');
    localStorage.removeItem('readonly');
  });
  await page.route('**/api/v1/auth/info', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 0,
      message: 'ok',
      data: { id: 1, username: 'e2e', nickname: '测试用户', role: 'USER' }
    })
  }));
  await page.route('**/api/v1/reports/annual-overview*', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 0, message: 'ok', data: annualData })
  }));
  await page.route('**/api/v1/reports/overview*', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 0, message: 'ok', data: monthlyData })
  }));
  // Layout 顶部角标会请求这两个接口；未 mock 时真实后端返回 401，
  // axios 拦截器会清 token 并跳登录页，导致年度页测试被重定向。
  await page.route('**/api/v1/system/alerts', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 0, message: 'ok', data: [] })
  }));
  await page.route('**/api/v1/prepaid-cards/summary', route => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 0, message: 'ok', data: { expiringSoonCount: 0 } })
  }));
}

test.describe('年度复盘', () => {
  test('年度复盘页可打开，且不破坏月度复盘', async ({ page }) => {
    await preparePage(page);
    await page.goto(BASE + '/reports?view=annual');
    await page.waitForTimeout(1200);

    await expect(page.locator('.report-hub-switch button.active')).toHaveText('年度复盘');
    await expect(page.locator('.an-eyebrow')).toContainText('年度成绩单');
    await expect(page.locator('.an-head h2')).toHaveText('年度复盘');
    await expect(page.locator('.an-figure')).toContainText('120,000');
    await expect(page.locator('.an-stat')).toHaveCount(4);
    await expect(page.locator('.an-health-row')).toHaveCount(4);

    // 切换回月度复盘，确认原页面仍正常
    await page.locator('.report-hub-switch button', { hasText: '月度复盘' }).click();
    await page.waitForTimeout(800);
    await expect(page.locator('.rp-head h2')).toHaveText('月度复盘');
    await expect(page.locator('.report-hub-switch button.active')).toHaveText('月度复盘');

    // 再切回年度，切换分类层级
    await page.locator('.report-hub-switch button', { hasText: '年度复盘' }).click();
    await page.waitForTimeout(800);
    await page.locator('.an-filter-level .n-radio-button').nth(1).click();
    await page.waitForTimeout(600);
    await expect(page.locator('.an-body')).toBeVisible();
  });

  test('移动端年度复盘不横向溢出', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await preparePage(page);
    await page.goto(BASE + '/reports?view=annual');
    await page.waitForTimeout(1200);

    await expect(page.locator('.an-page.mobile')).toBeVisible();
    await expect(page.locator('.an-eyebrow')).toContainText('年度成绩单');

    const overflow = await page.evaluate(() =>
      document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow).toBeLessThanOrEqual(1);

    await page.getByRole('button', { name: /展开分类、商户与年度之最/ }).click();
    await page.waitForTimeout(500);
    const expandedOverflow = await page.evaluate(() =>
      document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(expandedOverflow).toBeLessThanOrEqual(1);

    await page.setViewportSize({ width: 1280, height: 720 });
  });
});