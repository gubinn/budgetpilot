# BudgetPilot 系统测试报告

测试日期：2026-04-18（商户功能更新）

## 一、后端API测试结果 ✅ 全部通过

| 模块 | 测试数 | 结果 |
|------|--------|------|
| 账户管理 | 6 | ✅ 全通过 |
| 分类管理 | 4 | ✅ 全通过 |
| 交易管理 | 6 | ✅ 全通过 |
| 预算管理 | 2 | ✅ 全通过 |
| 周期交易 | 4 | ✅ 全通过（修复后） |
| 预警规则 | 4 | ✅ 全通过 |
| 报表统计 | 3 | ✅ 全通过 |
| 商户管理 | 5 | ✅ 全通过 |

**周期交易API修复**：toggle和execute接口之前测试时使用的规则ID不正确，现在已验证正常工作。

---

## 二、前端E2E测试结果 ✅ 全部通过

| 测试项 | 结果 | 备注 |
|--------|------|------|
| L01: 侧边栏导航 | ✅ | 所有菜单可点击 |
| D02: 本月收支 | ✅ | 显示收入/支出/结余 |
| D03: 预算进度 | ✅ | 预算进度卡片显示 |
| D04: 最近交易 | ✅ | 交易列表显示 |
| A01: 账户列表 | ✅ | 页面加载正常 |
| T01: 进入交易创建 | ✅ | URL正确(/add) |
| T07/T09: 类型切换 | ✅ | **核心bug已修复验证** |
| T10: 分类字段 | ✅ | 表单字段可见 |
| T16: 转账目标账户 | ✅ | 类型切换后字段显示 |
| C01: 分类页面 | ✅ | 页面加载正常 |
| B01: 预算页面 | ✅ | 页面加载正常 |
| R01: 周期交易 | ✅ | 页面加载正常 |
| AR01: 预警规则 | ✅ | 页面加载正常 |
| RP01: 报表页面 | ✅ | 页面加载正常 |
| S01: 设置页面 | ✅ | 页面加载正常 |
| L04: 小屏幕适配 | ✅ | 响应式正常 |
| AL01: 告警通知 | ✅ | 路由正确 |

---

## 三、修复内容汇总

### 后端修复
- 周期交易toggle/execute API：确认API正常，之前测试失败是因规则ID错误
- 交易UpdateDTO增加merchantName和autoCreateMerchant字段，支持编辑时补填商户
- TransactionServiceImpl.update()增加商户findOrCreate逻辑
- 周期规则Entity/DTO/VO增加merchantId字段
- RecurringRuleController增加toVO()方法填充商户名称
- RecurringRuleServiceImpl.create/update/generateTransaction传递merchantId

### 前端修复
- 交易类型切换验证：添加`type: 'number'`到验证规则（TransactionForm.vue:111）
- 交易列表新增商户列（分类和金额之间）
- 交易编辑表单商户字段对支出和收入类型均可见（v-if="form.type !== 3"）
- 周期交易表单新增商户选择器（仅支出类型，仅已有商户）
- E2E测试定位器优化：
  - 使用`page.getByRole('menu')`限定菜单范围避免多元素匹配
  - 使用`.n-card-header__main`定位器检查页面标题
  - 所有17个测试现在稳定通过

### 数据库变更
- `t_recurring_rule`表新增`merchant_id BIGINT`字段和`idx_merchant`索引
- 迁移脚本：`sql/recurring_merchant_migration.sql`
- `sql/schema.sql`同步更新

---

## 四、新增功能测试结果 ✅ 全部通过

### 4.1 交易商户补填功能

| 测试项 | 结果 | 备注 |
|--------|------|------|
| 交易列表商户列显示 | ✅ | 有商户显示名称+颜色，无商户显示"-" |
| 编辑交易选择已有商户 | ✅ | 商户关联成功，usage_count+1 |
| 编辑交易自动创建新商户 | ✅ | 新商户创建，交易关联成功 |
| 收入交易关联商户 | ✅ | 收入类型商户字段可见 |
| 转账交易不显示商户 | ✅ | type=3 时商户字段隐藏 |

### 4.2 周期规则商户支持

| 测试项 | 结果 | 备注 |
|--------|------|------|
| 支出规则关联已有商户 | ✅ | 商户下拉仅显示已启用商户 |
| 收入规则不显示商户字段 | ✅ | type=2 时商户字段隐藏 |
| 规则执行时商户传递给交易 | ✅ | 新交易 merchantId=规则商户ID |
| 规则不自动创建商户 | ✅ | 只能选择已有商户 |
| 编辑规则更换商户 | ✅ | 商户更新成功 |

---

## 五、测试文件位置

- 测试案例文档：`/opt/budgetpilot/TEST_CASES.md`
- 测试报告：`/opt/budgetpilot/TEST_REPORT.md`
- 前端E2E测试：`/opt/budgetpilot/frontend/e2e-tests.spec.cjs`
- Playwright配置：`/opt/budgetpilot/frontend/playwright.config.cjs`

---

## 五、运行测试命令

```bash
# 后端API测试
curl http://localhost:6060/api/v1/accounts
curl http://localhost:6060/api/v1/transactions
curl http://localhost:6060/api/v1/recurring-rules/3/toggle
curl http://localhost:6060/api/v1/recurring-rules/3/execute

# 前端E2E测试
cd /opt/budgetpilot/frontend
npx playwright test e2e-tests.spec.cjs --reporter=list
```