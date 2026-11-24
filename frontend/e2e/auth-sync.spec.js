import { test, expect } from '@playwright/test';

const BASE = 'http://127.0.0.1:6060';

const monthlyData = {
  period: { label: '2026年11月', startDate: '2026-11-01', endDate: '2026-11-30', days: 30, partialMonth: true },
  kpis: { income: 0, expense: 0, expenseAll: 0, net: 0, savingsRate: 0, avgDaily: 0, transactionCount: 0 },
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
  accounts: { netWorth: 0, assets: 0, liabilities: 0, groups: [], accounts: [], netWorthTrend: [] },
  dataStates: { budgetMissing: true, partialMonth: false, multiCurrency: false, noData: true },
  insights: []
};

test.describe('登录态跨标签同步', () => {
  test('一个标签进入只读分享后，另一个标签同步隐藏编辑入口', async ({ browser }) => {
    const context = await browser.newContext();
    await context.addInitScript(() => {
      localStorage.setItem('token', 'e2e-normal-token');
      localStorage.removeItem('readonly');
    });
    await context.route('**/api/v1/auth/info', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: { id: 1, username: 'e2e', nickname: '测试用户', role: 'USER' } })
    }));
    await context.route('**/api/v1/reports/overview*', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: monthlyData })
    }));
    await context.route('**/api/v1/system/alerts', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: [] })
    }));
    await context.route('**/api/v1/prepaid-cards/summary', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: { expiringSoonCount: 0 } })
    }));

    const pageA = await context.newPage();
    await pageA.goto(BASE + '/reports');
    await pageA.waitForTimeout(800);
    await expect(pageA.locator('.readonly-strip')).toHaveCount(0);

    const pageB = await context.newPage();
    await pageB.goto(BASE + '/reports');
    await pageB.waitForTimeout(400);
    await pageB.evaluate(() => {
      localStorage.setItem('token', 'ro_19_e2e-token');
      localStorage.setItem('readonly', JSON.stringify({
        ownerName: '共享账本', shareId: 19, expiresAt: null, shareCode: 'e2e'
      }));
    });

    await pageA.waitForTimeout(500);
    await expect(pageA.locator('.readonly-strip')).toBeVisible();
    await expect(pageA.locator('.readonly-strip')).toContainText('只读查看');

    await pageB.evaluate(() => {
      localStorage.removeItem('readonly');
      localStorage.removeItem('token');
    });
    await pageA.waitForTimeout(500);
    await expect(pageA.locator('.readonly-strip')).toHaveCount(0);

    await context.close();
  });
});