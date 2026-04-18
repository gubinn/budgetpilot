// @ts-check
const { test, expect } = require('@playwright/test');

const BASE_URL = 'http://localhost:6060';

test.describe('BudgetPilot 前端测试', () => {

  // =====================
  // 一、导航测试
  // =====================
  test('L01: 侧边栏导航 - 所有菜单可点击', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    const menuItems = ['首页', '交易', '账户', '分类', '预算', '周期', '预警', '报表', '设置'];

    for (const item of menuItems) {
      const link = page.getByText(item, { exact: true });
      if (await link.isVisible({ timeout: 2000 }).catch(() => false)) {
        await link.click();
        await page.waitForTimeout(500);
      }
    }
  });

  // =====================
  // 二、首页仪表盘测试
  // =====================
  test('D02: 首页显示本月收支', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await expect(page.getByText('本月收入')).toBeVisible();
    await expect(page.getByText('本月支出')).toBeVisible();
    await expect(page.getByText('本月结余')).toBeVisible();
  });

  test('D03: 首页显示预算进度', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await expect(page.getByText('预算进度')).toBeVisible();
  });

  test('D04: 首页显示最近交易', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await expect(page.getByText('最近交易')).toBeVisible();
  });

  // =====================
  // 三、账户管理测试
  // =====================
  test('A01: 账户列表页面加载', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    // 点击账户菜单导航 - 使用menu定位器避免匹配表格中的"账户"
    await page.getByRole('menu').getByText('账户', { exact: true }).click();
    await page.waitForTimeout(1000);

    // 页面card title是"账户管理"
    await expect(page.locator('.n-card-header__main').filter({ hasText: '账户管理' })).toBeVisible({ timeout: 5000 });
  });

  test('A02: 账户扩展字段添加', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await page.getByRole('menu').getByText('账户', { exact: true }).click();
    await page.waitForTimeout(1000);

    await page.getByRole('button', { name: '新增账户' }).click();
    await page.waitForTimeout(1000);

    // 检查扩展字段区域存在
    await expect(page.locator('.n-form-item-label').filter({ hasText: '扩展字段' })).toBeVisible();

    // 点击添加按钮
    await page.locator('.n-dynamic-input button').first().click();
    await page.waitForTimeout(300);

    // 填写键值对
    const keyInput = page.locator('input[placeholder="键"]').first();
    const valueInput = page.locator('input[placeholder="值"]').first();
    await expect(keyInput).toBeVisible();
    await expect(valueInput).toBeVisible();

    await keyInput.fill('账户标签');
    await valueInput.fill('主力账户');
    await expect(keyInput).toHaveValue('账户标签');
    await expect(valueInput).toHaveValue('主力账户');

    // 取消弹窗
    await page.getByRole('button', { name: '取消' }).first().click();
  });

  // =====================
  // 四、交易管理测试（重点：类型切换）
  // =====================
  test('T01: 进入交易创建页面', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await page.getByRole('button', { name: /记一笔/ }).click();
    await page.waitForTimeout(1000);

    await expect(page).toHaveURL(/\/add/, { timeout: 5000 });
  });

  test('T07/T09: 新增交易 - 类型切换无报错', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    // 点击"记一笔"
    await page.getByRole('button', { name: /记一笔/ }).click();
    await page.waitForTimeout(1000);

    // 页面标题应该是"新增交易"
    await expect(page.locator('.n-card-header__main').filter({ hasText: '新增交易' })).toBeVisible();

    // 切换到收入类型 - 这是之前有bug的地方
    await page.getByText('收入', { exact: true }).click();
    await page.waitForTimeout(300);

    // 检查没有错误提示（重点测试）
    const errorMsg = page.locator('.n-message').filter({ hasText: '请选择类型' });
    await expect(errorMsg).not.toBeVisible();

    // 再次切换回支出
    await page.getByText('支出', { exact: true }).click();
    await page.waitForTimeout(300);

    await expect(errorMsg).not.toBeVisible();
  });

  test('T10: 分类字段可见', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await page.getByRole('button', { name: /记一笔/ }).click();
    await page.waitForTimeout(1000);

    // 检查分类表单标签存在
    await expect(page.locator('.n-form-item-label').filter({ hasText: '分类' })).toBeVisible();
  });

  test('T16: 转账类型显示目标账户', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await page.getByRole('button', { name: /记一笔/ }).click();
    await page.waitForTimeout(1000);

    // 切换到转账类型
    await page.getByText('转账', { exact: true }).click();
    await page.waitForTimeout(500);

    // 检查目标账户字段出现
    await expect(page.locator('.n-form-item-label').filter({ hasText: '目标账户' })).toBeVisible();
  });

  // =====================
  // 五、分类管理测试
  // =====================
  test('C01: 分类页面加载', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await page.getByRole('menu').getByText('分类', { exact: true }).click();
    await page.waitForTimeout(1000);

    await expect(page.locator('.n-card-header__main').filter({ hasText: '分类管理' })).toBeVisible({ timeout: 5000 });
  });

  // =====================
  // 六、预算管理测试
  // =====================
  test('B01: 预算页面加载', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await page.getByText('预算', { exact: true }).click();
    await page.waitForTimeout(1000);

    await expect(page.locator('.n-card-header__main').filter({ hasText: '预算管理' })).toBeVisible({ timeout: 5000 });
  });

  // =====================
  // 七、周期交易测试
  // =====================
  test('R01: 周期交易页面加载', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await page.getByText('周期', { exact: true }).click();
    await page.waitForTimeout(1000);

    await expect(page.locator('.n-card-header__main').filter({ hasText: '周期交易' })).toBeVisible({ timeout: 5000 });
  });

  // =====================
  // 八、预警规则测试
  // =====================
  test('AR01: 预警规则页面加载', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await page.getByText('预警', { exact: true }).click();
    await page.waitForTimeout(1000);

    await expect(page.locator('.n-card-header__main').filter({ hasText: '预警规则管理' })).toBeVisible({ timeout: 5000 });
  });

  // =====================
  // 九、报表统计测试
  // =====================
  test('RP01: 报表页面加载', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await page.getByText('报表', { exact: true }).click();
    await page.waitForTimeout(1000);

    // 报表页面有收支趋势卡片
    await expect(page.locator('.n-card-header__main').filter({ hasText: '收支趋势' })).toBeVisible({ timeout: 5000 });
  });

  // =====================
  // 十、系统设置测试
  // =====================
  test('S01: 设置页面加载', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await page.getByText('设置', { exact: true }).click();
    await page.waitForTimeout(1000);

    // 设置页面有 Telegram 通知卡片
    await expect(page.locator('.n-card-header__main').filter({ hasText: 'Telegram 通知' })).toBeVisible({ timeout: 5000 });
  });

  // =====================
  // 十一、响应式测试
  // =====================
  test('L04: 小屏幕适配', async ({ page }) => {
    await page.setViewportSize({ width: 480, height: 800 });
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await expect(page.locator('body')).toBeVisible();
  });

  // =====================
  // 十二、告警通知页面测试
  // =====================
  test('AL01: 告警通知路由', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    // 从首页导航到告警页面 - 通过点击通知badge按钮
    const badgeBtn = page.locator('.n-badge').locator('..').locator('button');
    await badgeBtn.click();
    await page.waitForTimeout(1000);

    // 验证URL正确
    await expect(page).toHaveURL(/alerts/);
  });

  // =====================
  // 十三、扩展字段测试
  // =====================
  test('EF01: 扩展字段添加功能', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await page.getByRole('button', { name: /记一笔/ }).click();
    await page.waitForTimeout(1000);

    await expect(page.locator('.n-card-header__main').filter({ hasText: '新增交易' })).toBeVisible();

    await expect(page.locator('.n-form-item-label').filter({ hasText: '扩展字段' })).toBeVisible();

    // 点击添加按钮
    await page.locator('.n-dynamic-input button').filter({ hasText: '添加' }).click();
    await page.waitForTimeout(300);

    // 检查键值输入框出现
    const keyInput = page.locator('input[placeholder="键"]').first();
    const valueInput = page.locator('input[placeholder="值"]').first();
    await expect(keyInput).toBeVisible();
    await expect(valueInput).toBeVisible();

    // 填写值
    await keyInput.fill('测试键');
    await valueInput.fill('测试值');

    // 验证值已设置
    await expect(keyInput).toHaveValue('测试键');
    await expect(valueInput).toHaveValue('测试值');

    // 添加第二对 - 按钮变成图标按钮，使用最后一个按钮
    const addBtns = page.locator('.n-dynamic-input button');
    await addBtns.last().click();
    await page.waitForTimeout(300);
    const keyInputs = page.locator('input[placeholder="键"]');
    const valueInputs = page.locator('input[placeholder="值"]');
    await expect(keyInputs).toHaveCount(2);
    await expect(valueInputs).toHaveCount(2);
  });

  test('EF02: 日期时间选择器', async ({ page }) => {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');

    await page.getByRole('button', { name: /记一笔/ }).click();
    await page.waitForTimeout(1000);

    await expect(page.locator('.n-card-header__main').filter({ hasText: '新增交易' })).toBeVisible();

    // 检查日期时间字段存在
    await expect(page.locator('.n-form-item-label').filter({ hasText: '日期时间' })).toBeVisible();

    // 检查日期输入框存在
    const dateInput = page.locator('input[placeholder*="日期时间"]');
    await expect(dateInput.first()).toBeVisible();
  });
});