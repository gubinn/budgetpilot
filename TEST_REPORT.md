# BudgetPilot 系统测试报告

测试日期：2026-04-18（扩展字段与日期时间优化 + 账户扩展字段实现）

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

## 二、前端E2E测试结果 ✅ 全部通过 (19/19)

| 测试项 | 结果 | 备注 |
|--------|------|------|
| L01: 侧边栏导航 | ✅ | 所有菜单可点击 |
| D02: 本月收支 | ✅ | 显示收入/支出/结余 |
| D03: 预算进度 | ✅ | 预算进度卡片显示 |
| D04: 最近交易 | ✅ | 交易列表显示 |
| A01: 账户列表 | ✅ | 页面加载正常 |
| T01: 进入交易创建 | ✅ | URL正确(/add) |
| T07/T09: 类型切换 | ✅ | 核心bug已修复验证 |
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
| **EF01: 扩展字段添加** | ✅ | 新增测试 |
| **EF02: 日期时间选择器** | ✅ | 新增测试 |

---

## 三、修复与优化内容汇总

### 前端修复
- **扩展字段编辑器**：从原始JSON textarea改为 `n-dynamic-input` 动态键值对编辑器
  - 使用 `:on-create="() => ({ key: '', value: '' })"` 确保新项正确初始化
  - 列表页以 NTag 标签形式展示 `key:value`
- **日期时间选择器**：合并日期+时间为单个 `n-date-picker type="datetime"`
  - 解决 `NTimePicker` 未从 naive-ui 导出的问题
  - 表单数据使用 `YYYY-MM-DDTHH:mm:ss` 格式，提交时自动拆分
- **交易类型切换验证**：添加 `type: 'number'` 到验证规则

### 后端修复
- **merchantMap NPE**：修复 `ImmutableCollections$MapN.get(null)` 导致的500错误
  - `t.getMerchantId() != null ? merchantMap.get(t.getMerchantId()) : null`
- 交易UpdateDTO增加merchantName和autoCreateMerchant字段，支持编辑时补填商户
- TransactionServiceImpl.update()增加商户findOrCreate逻辑
- 周期规则Entity/DTO/VO增加merchantId字段
- RecurringRuleController增加toVO()方法填充商户名称
- RecurringRuleServiceImpl.create/update/generateTransaction传递merchantId

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

### 4.3 扩展字段功能

| 测试项 | 结果 | 备注 |
|--------|------|------|
| 扩展字段标签显示 | ✅ | 表单显示"扩展字段"label |
| 点击添加按钮 | ✅ | 出现键/值输入框 |
| 填写键值对 | ✅ | 输入框正确显示填写内容 |
| 添加第二对 | ✅ | 显示两组输入框 |
| 列表页展示 | ✅ | 以NTag标签展示 key:value |
| 编辑时回显 | ✅ | 从后端加载后正确显示已保存键值 |

### 4.4 日期时间功能

| 测试项 | 结果 | 备注 |
|--------|------|------|
| 日期时间选择器显示 | ✅ | 单个datetime picker |
| 选择日期时间 | ✅ | 表单字段正确更新 |
| 提交时拆分 | ✅ | 自动拆分为date和time字段 |
| 编辑时合并 | ✅ | date+time合并显示 |

---

## 五、测试文件位置

- 测试案例文档：`TEST_CASES.md`
- 测试报告：`TEST_REPORT.md`
- 前端E2E测试：`frontend/e2e-tests.spec.cjs`（19个测试）
- Playwright配置：`frontend/playwright.config.cjs`

---

## 六、运行测试命令

```bash
# 后端API测试
curl http://localhost:6060/api/v1/accounts
curl http://localhost:6060/api/v1/transactions
curl http://localhost:6060/api/v1/recurring-rules/3/toggle
curl http://localhost:6060/api/v1/recurring-rules/3/execute

# 前端E2E测试
cd frontend
npx playwright test e2e-tests.spec.cjs --config=playwright.config.cjs --reporter=list
```
