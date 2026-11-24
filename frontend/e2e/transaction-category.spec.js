import { test, expect } from '@playwright/test';

const BASE = 'http://127.0.0.1:6060';

const categoryTree = [
  {
    id: 1241, parentId: 0, name: '往来款', type: 1, budgetType: 'NONE', children: [
      { id: 1242, parentId: 1241, name: '借出款', type: 1, budgetType: 'NONE', children: [] },
      { id: 1315, parentId: 1241, name: '收回借出款', type: 2, budgetType: 'NONE', children: [] }
    ]
  },
  { id: 10, parentId: 0, name: '餐饮', type: 1, budgetType: 'DAILY', children: [] },
  { id: 161, parentId: 0, name: '工资', type: 2, budgetType: 'NONE', children: [] }
];

const accounts = [
  { id: 2, name: '储蓄卡', type: 1, balance: 10000, isActive: true },
  { id: 24, name: '目标账户', type: 1, balance: 10000, isActive: true }
];

const transaction = {
  id: 1936,
  type: 3,
  amount: 1000,
  currency: 'CNY',
  accountId: 2,
  targetAccountId: 24,
  categoryId: 1242,
  merchantId: null,
  merchantName: '转账',
  transactionDate: '2026-11-24',
  transactionTime: '15:59:00',
  note: '转账',
  tags: [],
  extFields: {}
};

test.describe('转账交易分类', () => {
  test('转账编辑页分类下拉框显示全部分类', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.addInitScript(() => {
      localStorage.setItem('token', 'e2e-token');
      localStorage.removeItem('readonly');
    });
    await page.route('**/api/v1/auth/info', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: { id: 1, username: 'e2e', nickname: '测试用户', role: 'USER' } })
    }));
    await page.route('**/api/v1/categories/tree*', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: categoryTree })
    }));
    await page.route('**/api/v1/accounts*', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: accounts })
    }));
    await page.route('**/api/v1/transactions?*', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: {
          items: [{
            ...transaction,
            categoryName: '借出款',
            parentCategoryName: '往来款',
            accountName: '储蓄卡',
            targetAccountName: '目标账户',
            isConfirmed: true,
            reviewStatus: 'REVIEWED'
          }],
          total: 1
        }
      })
    }));
    await page.route('**/api/v1/transactions/summary*', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: { count: 1, netExpense: 1000, expense: 1000, refund: 0, income: 0, transfer: 1000 }
      })
    }));
    await page.route('**/api/v1/transactions/tags*', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: [] })
    }));
    await page.route('**/api/v1/transactions/1936*', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: transaction })
    }));
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

    await page.goto(BASE + '/transactions/edit/1936');
    await page.waitForTimeout(1000);

    await expect(page.locator('.m-form')).toBeVisible();
    await expect(page.getByText('分类（可选）')).toBeVisible();

    await page.locator('.n-cascader').click();
    await page.waitForTimeout(300);
    await expect(page.getByText('借出款').first()).toBeVisible();
    await expect(page.getByText('收回借出款').first()).toBeVisible();
    await expect(page.getByText('工资').first()).toBeVisible();
  });
});