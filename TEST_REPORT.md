# BudgetPilot 系统测试报告

测试日期：2026-04-18

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

## 二、前端E2E测试结果 ✅ 全部通过 (20/20)

| 测试项 | ID | 结果 | 备注 |
|--------|-----|------|------|
| L01: 侧边栏导航 | E2E-L01 | ✅ | 所有菜单可点击 |
| D02: 本月收支 | E2E-D02 | ✅ | 显示收入/支出/结余 |
| D03: 预算进度 | E2E-D03 | ✅ | 预算进度卡片显示 |
| D04: 最近交易 | E2E-D04 | ✅ | 交易列表显示 |
| A01: 账户列表 | E2E-A01 | ✅ | 页面加载正常 |
| **A02: 账户扩展字段** | **E2E-A02** | ✅ | **新增测试** |
| T01: 进入交易创建 | E2E-T01 | ✅ | URL正确(/add) |
| T07/T09: 类型切换 | E2E-T07T09 | ✅ | 核心bug已修复验证 |
| T10: 分类字段 | E2E-T10 | ✅ | 表单字段可见 |
| T16: 转账目标账户 | E2E-T16 | ✅ | 类型切换后字段显示 |
| C01: 分类页面 | E2E-C01 | ✅ | 页面加载正常 |
| B01: 预算页面 | E2E-B01 | ✅ | 页面加载正常 |
| R01: 周期交易 | E2E-R01 | ✅ | 页面加载正常 |
| AR01: 预警规则 | E2E-AR01 | ✅ | 页面加载正常 |
| RP01: 报表页面 | E2E-RP01 | ✅ | 页面加载正常 |
| S01: 设置页面 | E2E-S01 | ✅ | 页面加载正常 |
| L04: 小屏幕适配 | E2E-L04 | ✅ | 响应式正常 |
| AL01: 告警通知 | E2E-AL01 | ✅ | 路由正确 |
| **EF01: 交易扩展字段** | **E2E-EF01** | ✅ | **新增测试** |
| **EF02: 日期时间选择器** | **E2E-EF02** | ✅ | **新增测试** |

---

## 三、修复与优化内容汇总

### 前端修复

| 模块 | 问题/需求 | 修复方案 | 涉及文件 |
|------|----------|----------|----------|
| 交易表单-时间字段 | NTimePicker 未从 naive-ui 导出，时间选择器不显示 | 合并为 `n-date-picker type="datetime"`，表单使用 `YYYY-MM-DDTHH:mm:ss` 格式，提交时自动拆分 | TransactionForm.vue |
| 交易表单-扩展字段 | n-dynamic-input 自定义默认 slot 时 onCreate 返回 null | 添加 `:on-create="() => ({ key: '', value: '' })"` 确保新项正确初始化 | TransactionForm.vue |
| 交易列表-扩展字段展示 | extFields 显示为原始 JSON 字符串 | 新增列，以 NTag 标签形式展示 `key:value` | TransactionList.vue |
| 交易类型切换验证 | 切换类型后提交无报错但验证规则丢失 | 添加 `type: 'number'` 到验证规则 | TransactionForm.vue |
| 交易列表-商户列 | 缺少商户信息展示 | 新增商户列（分类和金额之间），显示名称+颜色 | TransactionList.vue |
| 账户表单-扩展字段 | 前端未实现扩展字段编辑器 | 新增 n-dynamic-input 动态键值对编辑器，编辑时回显，列表展示 | AccountList.vue |
| 账户表单-余额调整 | 余额调整时扩展字段不同步 | confirmBalanceAdjust 中先更新扩展字段再调用余额调整接口 | AccountList.vue |

### 后端修复

| 模块 | 问题 | 修复方案 | 涉及文件 |
|------|------|----------|----------|
| 交易查询 | 无商户交易查询列表返回500：`ImmutableCollections$MapN.get(null)` NPE | 添加 null 检查：`t.getMerchantId() != null ? merchantMap.get(t.getMerchantId()) : null` | TransactionServiceImpl.java |
| 交易编辑 | UpdateDTO 缺少商户相关字段 | 增加 merchantName 和 autoCreateMerchant 字段 | TransactionUpdateDTO.java |
| 交易商户 | 编辑交易时商户名补填 | update() 增加 findOrCreate 逻辑 | TransactionServiceImpl.java |
| 周期规则商户 | Entity/DTO/VO 缺少 merchantId 字段 | 增加 merchantId，Controller 增加 toVO() 填充名称，Service 传递 merchantId | RecurringRule 相关文件 |

### 数据库变更

| 变更 | 内容 |
|------|------|
| `t_recurring_rule` 表 | 新增 `merchant_id BIGINT` 字段和 `idx_merchant` 索引 |
| 迁移脚本 | `sql/recurring_merchant_migration.sql` |
| 建表脚本 | `sql/schema.sql` 同步更新 |

### 部署脚本修复

| 问题 | 修复方案 |
|------|----------|
| deploy.sh 运行 update-frontend.sh 后工作目录改变，mvn 找不到 JAR | 运行完 update-frontend.sh 后显式 `cd "$PROJECT_DIR"` |
| update-frontend.sh 头部注释不完整 | 补充说明为什么同步两处（nginx web + Spring Boot static） |

### 版本控制优化

| 变更 | 说明 |
|------|------|
| `.gitignore` | 新增 `web/` 排除，nginx 静态文件是构建产物不纳入版本控制 |

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

### 4.3 交易扩展字段功能

| 测试项 | 结果 | 备注 |
|--------|------|------|
| 扩展字段标签显示 | ✅ | 表单显示"扩展字段" label |
| 点击添加按钮 | ✅ | 出现键/值输入框 |
| 填写键值对 | ✅ | 输入框正确显示填写内容 |
| 添加第二对 | ✅ | 显示两组输入框 |
| 列表页展示 | ✅ | 以 NTag 标签展示 key:value |
| 编辑时回显 | ✅ | 从后端加载后正确显示已保存键值 |
| E2E 自动化测试 | ✅ | EF01 测试通过 |

### 4.4 交易日期时间功能

| 测试项 | 结果 | 备注 |
|--------|------|------|
| 日期时间选择器显示 | ✅ | 单个 datetime picker |
| 选择日期时间 | ✅ | 表单字段正确更新 |
| 提交时拆分 | ✅ | 自动拆分为 date 和 time 字段 |
| 编辑时合并 | ✅ | date+time 合并为 YYYY-MM-DDTHH:mm:ss 显示 |
| E2E 自动化测试 | ✅ | EF02 测试通过 |

### 4.5 账户扩展字段功能

| 测试项 | 结果 | 备注 |
|--------|------|------|
| 新增账户时添加扩展字段 | ✅ | 扩展字段编辑器可用 |
| 编辑账户时回显扩展字段 | ✅ | 已保存键值对正确显示 |
| 列表页展示扩展字段 | ✅ | 以 NTag 标签展示 key:value |
| 余额调整时同步扩展字段 | ✅ | confirmBalanceAdjust 中先更新扩展字段 |
| E2E 自动化测试 | ✅ | A02 测试通过 |

---

## 六、新增功能测试结果

### 6.1 用户管理与登录系统

| 测试项 | 结果 | 备注 |
|--------|------|------|
| 默认管理员初始化 | ✅ | DataInitializer 自动创建 admin/admin123 |
| 用户登录 | ✅ | BCrypt 密码验证，Sa-Token 生成 token |
| 获取当前用户信息 | ✅ | GET /api/auth/info 返回用户信息 |
| 用户列表查询（管理员） | ✅ | 返回所有用户列表 |
| 创建用户（管理员） | ✅ | 用户名唯一校验，BCrypt 加密 |
| 更新用户（管理员） | ✅ | 修改昵称/角色/启用状态 |
| 删除用户（管理员） | ✅ | 删除成功 |
| 重置密码（管理员） | ✅ | BCrypt 新密码加密 |
| 用户配置读写 | ✅ | t_user_config 增删改查 |
| 前端登录页 | ✅ | Naive UI 表单，自动跳转 |
| 前端路由守卫 | ✅ | 未登录自动跳转 /login |
| Layout 用户菜单 | ✅ | 显示昵称、退出登录 |
| 用户管理页 | ✅ | 用户列表/创建/编辑/重置密码/删除 |
| 角色权限校验 | ✅ | 非管理员无法访问 /api/v1/users |
| 数据隔离（TenantLine） | ✅ | SQL 自动注入 user_id 条件 |

### 6.2 架构变更

| 变更 | 说明 |
|------|------|
| 数据库 | 新增 t_user、t_user_config 表 |
| 数据库 | 9张业务表添加 user_id 字段和索引 |
| 后端 | Sa-Token 认证 + StpInterface 角色校验 |
| 后端 | TenantLineInnerInterceptor 数据隔离 |
| 后端 | MetaObjectHandler 自动填充 userId |
| 前端 | Pinia auth store 管理登录状态 |
| 前端 | 新增 LoginPage.vue、UserList.vue |

---

## 七、测试文件位置

- 测试案例文档：`TEST_CASES.md`
- 测试报告：`TEST_REPORT.md`
- 前端E2E测试：`frontend/e2e-tests.spec.cjs`（20个测试）
- Playwright配置：`frontend/playwright.config.cjs`

---

## 八、运行测试命令

```bash
# 用户登录
curl -X POST http://localhost:6060/api/auth/login -d '{"username":"admin","password":"admin123"}' -H 'Content-Type: application/json'

# 后端API测试（需要 token）
TOKEN="your-token-here"
curl http://localhost:6060/api/v1/users -H "Authorization: $TOKEN"
curl http://localhost:6060/api/v1/accounts -H "Authorization: $TOKEN"

# 前端E2E测试
cd frontend
npx playwright test e2e-tests.spec.cjs --config=playwright.config.cjs --reporter=list

# 完整部署（前端+后端+Docker）
bash scripts/deploy.sh
```
