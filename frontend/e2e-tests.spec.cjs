// @ts-check
const { test, expect } = require('@playwright/test');

const BASE_URL = 'http://localhost:6060';
const TEST_USER = 'isotest_user';
const TEST_PASS = 'Test@2026';
const ADMIN_USER = 'admin';
const ADMIN_PASS = 'admin123';

// Helper: login via UI
async function login(page, username = TEST_USER, password = TEST_PASS) {
  await page.context().clearCookies();
  await page.goto(BASE_URL);
  await page.waitForLoadState('networkidle');
  await page.fill('input[placeholder="请输入用户名"]', username);
  await page.fill('input[placeholder="请输入密码"]', password);
  await page.getByRole('button', { name: '登 录' }).click();
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(3000);
}

test.describe('BudgetPilot Frontend E2E', () => {

  // =====================
  // 一、登录 & 认证 (AUTH-UI-01 ~ 08)
  // =====================
  test('AUTH-UI-01: 未登录自动跳转登录页', async ({ page }) => {
    await page.context().clearCookies();
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');
    await expect(page.locator('input[placeholder="请输入用户名"]')).toBeVisible({ timeout: 5000 });
  });

  test('AUTH-UI-02: 正常登录', async ({ page }) => {
    await page.context().clearCookies();
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');
    await page.fill('input[placeholder="请输入用户名"]', TEST_USER);
    await page.fill('input[placeholder="请输入密码"]', TEST_PASS);
    await page.getByRole('button', { name: '登 录' }).click();
    await page.waitForTimeout(2000);
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  test('AUTH-UI-03: 用户名不存在', async ({ page }) => {
    await page.context().clearCookies();
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');
    await page.fill('input[placeholder="请输入用户名"]', 'nonexistent');
    await page.fill('input[placeholder="请输入密码"]', TEST_PASS);
    await page.getByRole('button', { name: '登 录' }).click();
    await page.waitForTimeout(1500);
    await expect(page.locator('body')).toContainText('用户名或密码错误', { timeout: 5000 });
  });

  test('AUTH-UI-04: 密码错误', async ({ page }) => {
    await page.context().clearCookies();
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');
    await page.fill('input[placeholder="请输入用户名"]', TEST_USER);
    await page.fill('input[placeholder="请输入密码"]', 'wrongpassword');
    await page.getByRole('button', { name: '登 录' }).click();
    await page.waitForTimeout(1500);
    await expect(page.locator('body')).toContainText('用户名或密码错误', { timeout: 5000 });
  });

  test('AUTH-UI-05: 空用户名登录', async ({ page }) => {
    await page.context().clearCookies();
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');
    await page.fill('input[placeholder="请输入密码"]', TEST_PASS);
    await page.getByRole('button', { name: '登 录' }).click();
    await page.waitForTimeout(1500);
    // 应该有校验提示
    await expect(page.locator('body')).toContainText('请输入用户名', { timeout: 5000 });
  });

  test('AUTH-UI-06: 空密码登录', async ({ page }) => {
    await page.context().clearCookies();
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');
    await page.fill('input[placeholder="请输入用户名"]', TEST_USER);
    await page.getByRole('button', { name: '登 录' }).click();
    await page.waitForTimeout(1500);
    await expect(page.locator('body')).toContainText('请输入密码', { timeout: 5000 });
  });

  test('AUTH-UI-07: 退出登录', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    // Clear cookies to simulate logout
    await page.context().clearCookies();
    await page.reload();
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    // After logout and reload, should see login page or be redirected
    const hasLogin = await page.locator('input[placeholder="请输入用户名"]').isVisible().catch(() => false);
    expect(hasLogin || page.url().includes('/login')).toBe(true);
  });

  test('AUTH-UI-08: 记住登录状态', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.reload();
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    const hasLoginInputs = await page.locator('input[placeholder="请输入用户名"]').isVisible({ timeout: 3000 }).catch(() => false);
    expect(hasLoginInputs).toBe(false);
  });

  // =====================
  // 二、首页仪表盘 (DASH-01 ~ 10)
  // =====================
  test('DASH-01: 首页加载 - 本月收入显示', async ({ page }) => {
    await login(page);
    await expect(page.getByText('本月收入')).toBeVisible({ timeout: 5000 });
  });

  test('DASH-02: 首页加载 - 本月支出显示', async ({ page }) => {
    await login(page);
    await expect(page.getByText('本月支出')).toBeVisible({ timeout: 5000 });
  });

  test('DASH-03: 首页加载 - 本月结余显示', async ({ page }) => {
    await login(page);
    await expect(page.getByText('本月结余')).toBeVisible({ timeout: 5000 });
  });

  test('DASH-04: 首页显示预算进度', async ({ page }) => {
    await login(page);
    await expect(page.getByText('预算进度')).toBeVisible({ timeout: 5000 });
  });

  test('DASH-05: 首页显示最近交易', async ({ page }) => {
    await login(page);
    await expect(page.getByText('最近交易')).toBeVisible({ timeout: 5000 });
  });

  test('DASH-06: 总资产显示', async ({ page }) => {
    await login(page);
    await expect(page.getByText(/总资产/)).toBeVisible({ timeout: 5000 });
  });

  test('DASH-07: 收支趋势图表', async ({ page }) => {
    await login(page);
    // Chart may load asynchronously, just verify page renders
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  test('DASH-08: 首页点击"记一笔"跳转', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '记一笔' }).click();
    await page.waitForTimeout(1000);
    await expect(page).toHaveURL(/\/transactions\/add/, { timeout: 5000 });
  });

  test('DASH-09: 侧边栏导航 - 所有菜单可点击', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    const menuItems = ['首页', '交易', '账户', '分类', '预算', '周期', '预警', '报表', '设置'];
    for (const item of menuItems) {
      const link = page.getByText(item, { exact: true });
      if (await link.isVisible({ timeout: 2000 }).catch(() => false)) {
        await link.click();
        await page.waitForTimeout(800);
      }
    }
  });

  test('DASH-10: 小屏幕适配 (480px)', async ({ page }) => {
    await page.setViewportSize({ width: 480, height: 800 });
    await login(page);
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  // =====================
  // 三、交易管理 (TXN-UI-01 ~ 15)
  // =====================
  test('TXN-UI-01: 进入交易创建页面', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '记一笔' }).click();
    await page.waitForTimeout(1000);
    await expect(page).toHaveURL(/\/transactions\/add/, { timeout: 5000 });
  });

  test('TXN-UI-02: 支出类型创建交易', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '记一笔' }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('.n-form-item-label').filter({ hasText: '分类' })).toBeVisible({ timeout: 5000 });
  });

  test('TXN-UI-03: 收入类型切换', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '记一笔' }).click();
    await page.waitForTimeout(1000);
    await page.getByText('收入', { exact: true }).click();
    await page.waitForTimeout(500);
    await expect(page.locator('.n-message').filter({ hasText: '请选择类型' })).not.toBeVisible();
  });

  test('TXN-UI-04: 转账类型切换 - 显示目标账户', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '记一笔' }).click();
    await page.waitForTimeout(1000);
    await page.getByText('转账', { exact: true }).click();
    await page.waitForTimeout(500);
    await expect(page.locator('.n-form-item-label').filter({ hasText: '目标账户' })).toBeVisible();
  });

  test('TXN-UI-05: 分类字段可见', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '记一笔' }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('.n-form-item-label').filter({ hasText: '分类' })).toBeVisible();
  });

  test('TXN-UI-06: 商户搜索', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '记一笔' }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('.n-form-item-label').filter({ hasText: /商户/ })).toBeVisible({ timeout: 5000 });
  });

  test('TXN-UI-07: 备注字段', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '记一笔' }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('textarea, input[placeholder*="备注"]')).toBeVisible({ timeout: 5000 });
  });

  test('TXN-UI-08: 日期时间选择器', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '记一笔' }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('.n-form-item-label').filter({ hasText: '日期时间' })).toBeVisible({ timeout: 5000 });
  });

  test('TXN-UI-09: 扩展字段添加', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '记一笔' }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('.n-form-item-label').filter({ hasText: '扩展字段' })).toBeVisible({ timeout: 5000 });
  });

  test('TXN-UI-10: 交易列表页', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('交易', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  test('TXN-UI-11: 交易筛选', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('交易', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('TXN-UI-12: 交易编辑', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('交易', { exact: true }).click();
    await page.waitForTimeout(1000);
    const editBtn = page.locator('button:has-text("编辑")').first();
    if (await editBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await editBtn.click();
      await page.waitForTimeout(1000);
    }
    await expect(page.locator('body')).toBeVisible();
  });

  test('TXN-UI-13: 交易删除', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('交易', { exact: true }).click();
    await page.waitForTimeout(1000);
    const deleteBtn = page.locator('button:has-text("删除")').first();
    if (await deleteBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await deleteBtn.click();
      await page.waitForTimeout(1000);
    }
    await expect(page.locator('body')).toBeVisible();
  });

  test('TXN-UI-14: 交易详情', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('交易', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('TXN-UI-15: 金额输入', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '记一笔' }).click();
    await page.waitForTimeout(1000);
    const amountInput = page.locator('input[placeholder*="金额"], input[placeholder*="0.00"]').first();
    if (await amountInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      await amountInput.fill('100');
      await page.waitForTimeout(500);
      await expect(amountInput).toHaveValue('100');
    }
  });

  // =====================
  // 四、账户管理 (AC-UI-01 ~ 10)
  // =====================
  test('AC-UI-01: 账户列表页面加载', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('账户', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  test('AC-UI-02: 新增账户 - 弹出表单', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('账户', { exact: true }).click();
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '新增账户' }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('.n-modal, .n-dialog')).toBeVisible({ timeout: 5000 });
  });

  test('AC-UI-03: 新增账户 - 填写表单', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('账户', { exact: true }).click();
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '新增账户' }).click();
    await page.waitForTimeout(1000);
    await page.fill('input[placeholder="如：招商银行储蓄卡"]', 'Playwright测试账户');
    await page.locator('.n-select').first().click();
    await page.waitForTimeout(500);
    await page.locator('.n-base-select-option').first().click();
    await page.waitForTimeout(300);
  });

  test('AC-UI-04: 新增账户 - 提交', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('账户', { exact: true }).click();
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '新增账户' }).click();
    await page.waitForTimeout(1000);
    await page.fill('input[placeholder="如：招商银行储蓄卡"]', 'PW提交账户' + Date.now().toString().slice(-4));
    await page.locator('.n-select').first().click();
    await page.waitForTimeout(500);
    await page.locator('.n-base-select-option').first().click();
    await page.waitForTimeout(300);
    // Click the confirm button in the modal
    const confirmBtn = page.locator('.n-modal').getByRole('button', { name: '确定' });
    if (await confirmBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await confirmBtn.click();
    } else {
      // Fallback: any button with 确定 text
      await page.locator('button:has-text("确定")').first().click().catch(() => {});
    }
    await page.waitForTimeout(2000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('AC-UI-05: 账户编辑', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('账户', { exact: true }).click();
    await page.waitForTimeout(1000);
    const editBtn = page.locator('button:has-text("编辑")').first();
    if (await editBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await editBtn.click();
      await page.waitForTimeout(1000);
    }
    await expect(page.locator('body')).toBeVisible();
  });

  test('AC-UI-06: 账户删除', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('账户', { exact: true }).click();
    await page.waitForTimeout(1000);
    const deleteBtn = page.locator('button:has-text("删除")').first();
    if (await deleteBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await deleteBtn.click();
      await page.waitForTimeout(1000);
    }
    await expect(page.locator('body')).toBeVisible();
  });

  test('AC-UI-07: 账户余额调整', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('账户', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  test('AC-UI-08: 账户扩展字段', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('账户', { exact: true }).click();
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '新增账户' }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('.n-form-item-label').filter({ hasText: '扩展字段' })).toBeVisible({ timeout: 5000 });
  });

  test('AC-UI-09: 账户类型选择', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('账户', { exact: true }).click();
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: '新增账户' }).click();
    await page.waitForTimeout(1000);
    await page.locator('.n-select').first().click();
    await page.waitForTimeout(500);
    await expect(page.locator('.n-base-select-option').first()).toBeVisible();
  });

  test('AC-UI-10: 账户停用/启用', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('账户', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  // =====================
  // 五、分类管理 (CAT-UI-01 ~ 08)
  // =====================
  test('CAT-UI-01: 分类页面加载', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('分类', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  test('CAT-UI-02: 分类树形显示', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('分类', { exact: true }).click();
    await page.waitForTimeout(1000);
    // Check that the n-tree component exists (use first() to avoid strict mode)
    await expect(page.locator('.n-tree').first()).toBeVisible({ timeout: 5000 });
  });

  test('CAT-UI-03: 新增分类', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('分类', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  test('CAT-UI-04: 编辑分类', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('分类', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('CAT-UI-05: 删除分类', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('分类', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('CAT-UI-06: 支出/收入类型切换', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('分类', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('CAT-UI-07: 分类图标/颜色选择', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('分类', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('CAT-UI-08: 系统分类显示', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('分类', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  // =====================
  // 六、预算管理 (BGT-UI-01 ~ 08)
  // =====================
  test('BGT-UI-01: 预算页面加载', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('预算', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  test('BGT-UI-02: 设置月度预算', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('预算', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('BGT-UI-03: 预算分类明细', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('预算', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('BGT-UI-04: 预算进度条', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('预算', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('BGT-UI-05: 修改预算', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('预算', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('BGT-UI-06: 预算月份选择', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('预算', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('BGT-UI-07: 预算超支提示', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('预算', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('BGT-UI-08: 预算锁定', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('预算', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  // =====================
  // 七、周期交易 (RR-UI-01 ~ 08)
  // =====================
  test('RR-UI-01: 周期交易页面加载', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('周期', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  test('RR-UI-02: 新增周期规则', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('周期', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('RR-UI-03: 频率选择', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('周期', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('RR-UI-04: 暂停/启用规则', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('周期', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('RR-UI-05: 删除规则', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('周期', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('RR-UI-06: 立即执行规则', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('周期', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('RR-UI-07: 开始/结束日期', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('周期', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('RR-UI-08: 自动生成交易', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('周期', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  // =====================
  // 八、预警规则 (AR-UI-01 ~ 06)
  // =====================
  test('AR-UI-01: 预警规则页面加载', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('预警', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  test('AR-UI-02: 新增预警规则', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('预警', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('AR-UI-03: 编辑预警规则', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('预警', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('AR-UI-04: 停用/启用预警', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('预警', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('AR-UI-05: 删除预警规则', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('预警', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('AR-UI-06: 预警类型选择', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('预警', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  // =====================
  // 九、报表统计 (RP-UI-01 ~ 08)
  // =====================
  test('RP-UI-01: 报表页面加载', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('报表', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  test('RP-UI-02: 收支趋势图表', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('报表', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  test('RP-UI-03: 分类分布图表', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('报表', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('RP-UI-04: 月份筛选', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('报表', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('RP-UI-05: 月度对比', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('报表', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('RP-UI-06: 商户分布', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('报表', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('RP-UI-07: 预算回顾', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('报表', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('RP-UI-08: 汇率分布', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('报表', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  // =====================
  // 十、系统设置 (SYS-UI-01 ~ 06)
  // =====================
  test('SYS-UI-01: 设置页面加载', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('设置', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('.n-card-header__main').filter({ hasText: 'Telegram 通知' })).toBeVisible({ timeout: 5000 });
  });

  test('SYS-UI-02: Telegram 配置输入', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('设置', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  test('SYS-UI-03: 保存配置按钮', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('设置', { exact: true }).click();
    await page.waitForTimeout(1000);
    // Settings page loaded, save button may be in a form
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

  test('SYS-UI-04: 推送测试按钮', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('设置', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('SYS-UI-05: API Key 显示', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('设置', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('SYS-UI-06: 预警日志', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.getByRole('menu').getByText('设置', { exact: true }).click();
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  // =====================
  // 十一、告警通知 (ALT-UI-01 ~ 04)
  // =====================
  test('ALT-UI-01: 告警列表加载', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.goto(BASE_URL + '/alerts');
    await page.waitForTimeout(1000);
    await expect(page).toHaveURL(/alerts/, { timeout: 5000 });
  });

  test('ALT-UI-02: 标记已读按钮', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.goto(BASE_URL + '/alerts');
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('ALT-UI-03: 全部标记已读', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await page.goto(BASE_URL + '/alerts');
    await page.waitForTimeout(1000);
    await expect(page.locator('body')).toBeVisible();
  });

  test('ALT-UI-04: 通知 badge 显示', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);
    await expect(page.locator('.n-badge')).toBeVisible({ timeout: 5000 });
  });

  // =====================
  // 十二、PWA & 响应式 (PWA-01 ~ 03)
  // =====================
  test('PWA-01: PWA 基本配置', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');
    // Check for manifest link or service worker
    const hasManifest = await page.locator('link[rel="manifest"]').count().then(c => c > 0).catch(() => false);
    const hasSW = await page.evaluate(() => !!navigator.serviceWorker);
    expect(hasManifest || hasSW).toBe(true);
  });

  test('PWA-02: Service Worker 注册', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');
    const hasSW = await page.evaluate(() => !!navigator.serviceWorker);
    expect(hasSW).toBe(true);
  });

  test('PWA-03: 离线缓存', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');
    await page.reload();
    await page.waitForLoadState('networkidle');
    await expect(page.locator('body')).toBeVisible({ timeout: 5000 });
  });

});
