# BudgetPilot API 文档

## 项目概述

- **技术栈**: Java Spring Boot (后端) + Vue.js (前端)
- **API 前缀**: `/api/v1`
- **响应格式**: 统一使用 `Result<T>` 包装

### 统一响应格式

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "timestamp": 1234567890
}
```

**成功响应**: `code: 0`
**错误响应**: `code: 非0值` (见 [错误码汇总](#错误码汇总))

---

## API 接口清单

### 1. 账户管理 (AccountController)

**基础路径**: `/api/v1/accounts`

| 方法 | 路径 | 功能 | 请求体 |
|------|------|------|--------|
| POST | `/api/v1/accounts` | 创建账户 | `AccountCreateDTO` |
| GET | `/api/v1/accounts` | 获取账户列表 | `active` (query, 默认true) |
| GET | `/api/v1/accounts/{id}` | 获取账户详情 | - |
| PATCH | `/api/v1/accounts/{id}` | 更新账户 | `AccountUpdateDTO` |
| DELETE | `/api/v1/accounts/{id}` | 删除账户 | - |
| POST | `/api/v1/accounts/{id}/adjust-balance` | 调整账户余额 | `BalanceAdjustDTO` |

#### AccountCreateDTO

```json
{
  "name": "账户名称 (必填)",
  "type": "账户类型 (必填, 1-借记 2-信用 3-电子钱包)",
  "icon": "图标 (默认 wallet)",
  "currency": "币种 (默认 CNY)",
  "initialBalance": "初始余额 (默认 0)",
  "creditLimit": "信用额度",
  "billingDay": "账单日",
  "paymentDay": "还款日",
  "sortOrder": "排序 (默认 0)",
  "extFields": {},
  "metadata": {}
}
```

#### AccountUpdateDTO

```json
{
  "name": "账户名称",
  "icon": "图标",
  "currency": "币种",
  "initialBalance": "初始余额",
  "currentBalance": "当前余额",
  "creditLimit": "信用额度",
  "billingDay": "账单日",
  "paymentDay": "还款日",
  "sortOrder": "排序",
  "isActive": "是否激活",
  "extFields": {},
  "metadata": {}
}
```

#### BalanceAdjustDTO

```json
{
  "newBalance": "新余额 (必填)",
  "reason": "调整原因 (必填)"
}
```

---

### 2. 交易管理 (TransactionController)

**基础路径**: `/api/v1/transactions`

| 方法 | 路径 | 功能 | 请求体 |
|------|------|------|--------|
| POST | `/api/v1/transactions` | 创建交易 | `TransactionCreateDTO` |
| GET | `/api/v1/transactions` | 查询交易列表 | `TransactionQueryDTO` |
| GET | `/api/v1/transactions/{id}` | 获取交易详情 | - |
| PUT | `/api/v1/transactions/{id}` | 更新交易 | `TransactionUpdateDTO` |
| DELETE | `/api/v1/transactions/{id}` | 删除交易 | - |
| POST | `/api/v1/transactions/{id}/confirm` | 确认交易 | - |

#### TransactionCreateDTO

```json
{
  "type": "交易类型 (必填, 1-支出 2-收入 3-转账)",
  "amount": "金额 (必填, >0)",
  "currency": "币种 (默认 CNY)",
  "transactionDate": "交易日期",
  "transactionTime": "交易时间",
  "note": "备注",
  "accountId": "账户ID (必填)",
  "targetAccountId": "目标账户ID (转账用)",
  "categoryId": "分类ID (必填)",
  "merchantId": "商户ID (支出交易可选)",
  "merchantName": "商户名称 (用于自动创建商户)",
  "autoCreateMerchant": "是否自动创建商户 (默认 false)",
  "tags": ["标签列表"],
  "attachmentUrls": ["附件URL列表"],
  "isConfirmed": "是否确认 (默认 true)",
  "isRecurring": "是否周期交易",
  "recurringId": "周期规则ID",
  "extFields": {},
  "metadata": {}
}
```

#### TransactionQueryDTO (Query Parameters)

| 参数 | 类型 | 说明 |
|------|------|------|
| `type` | int | 交易类型 (1-支出 2-收入 3-转账) |
| `accountId` | long | 账户ID |
| `categoryId` | long | 分类ID |
| `startDate` | string | 开始日期 (yyyy-MM-dd) |
| `endDate` | string | 结束日期 (yyyy-MM-dd) |
| `minAmount` | decimal | 最小金额 |
| `maxAmount` | decimal | 最大金额 |
| `currency` | string | 币种 |
| `keyword` | string | 关键字搜索 |
| `tags` | list | 标签列表 |
| `confirmed` | boolean | 是否已确认 |
| `extKey` | string | 扩展字段键 |
| `extValue` | string | 扩展字段值 |
| `page` | int | 页码 (默认 1) |
| `size` | int | 每页条数 (默认 20) |
| `sort` | string | 排序字段 (默认 transaction_date,desc) |

#### TransactionUpdateDTO

```json
{
  "type": "交易类型 (1-3)",
  "amount": "金额 (>0)",
  "currency": "币种",
  "transactionDate": "交易日期",
  "transactionTime": "交易时间",
  "note": "备注",
  "accountId": "账户ID",
  "targetAccountId": "目标账户ID",
  "categoryId": "分类ID",
  "merchantId": "商户ID",
  "tags": ["标签列表"],
  "attachmentUrls": ["附件URL列表"],
  "isConfirmed": "是否确认",
  "extFields": {},
  "metadata": {}
}
```

---

### 3. 分类管理 (CategoryController)

**基础路径**: `/api/v1/categories`

| 方法 | 路径 | 功能 | 请求体 |
|------|------|------|--------|
| POST | `/api/v1/categories` | 创建分类 | `CategoryCreateDTO` |
| GET | `/api/v1/categories/tree` | 获取分类树 | `type` (query, 可选) |
| GET | `/api/v1/categories/{id}` | 获取分类详情 | - |
| PUT | `/api/v1/categories/{id}` | 更新分类 | `CategoryCreateDTO` |
| DELETE | `/api/v1/categories/{id}` | 删除分类 | - |

#### CategoryCreateDTO

```json
{
  "parentId": "父分类ID (默认 0)",
  "name": "分类名称 (必填)",
  "type": "分类类型 (必填, 1-支出 2-收入 3-转账)",
  "icon": "图标",
  "color": "颜色",
  "sortOrder": "排序 (默认 0)"
}
```

---

### 4. 预算管理 (BudgetController)

**基础路径**: `/api/v1/budgets`

| 方法 | 路径 | 功能 | 请求体 |
|------|------|------|--------|
| POST | `/api/v1/budgets` | 创建预算 | `BudgetCreateDTO` |
| GET | `/api/v1/budgets/{yearMonth}` | 获取预算 | - |
| PUT | `/api/v1/budgets/{yearMonth}` | 更新预算 | `BudgetUpdateDTO` |
| POST | `/api/v1/budgets/{yearMonth}/copy-from/{sourceMonth}` | 复制预算 | - |
| GET | `/api/v1/budgets/{yearMonth}/progress` | 获取预算进度 | - |

#### BudgetCreateDTO

```json
{
  "yearMonth": "年月 (必填, yyyy-MM)",
  "totalAmount": "总预算 (必填, >0)",
  "note": "备注",
  "items": [
    {
      "categoryId": "分类ID (必填)",
      "amount": "分类预算 (必填, >0)"
    }
  ]
}
```

#### BudgetUpdateDTO

```json
{
  "totalAmount": "总预算 (>0)",
  "note": "备注",
  "items": [
    {
      "categoryId": "分类ID (必填)",
      "amount": "分类预算 (必填, >0)"
    }
  ]
}
```

---

### 5. 商户管理 (MerchantController)

**基础路径**: `/api/v1/merchants`

| 方法 | 路径 | 功能 | 请求体 |
|------|------|------|--------|
| POST | `/api/v1/merchants` | 创建商户 | `MerchantCreateDTO` |
| GET | `/api/v1/merchants` | 商户列表（分页） | `MerchantQueryDTO` |
| GET | `/api/v1/merchants/{id}` | 商户详情 | - |
| PUT | `/api/v1/merchants/{id}` | 更新商户 | `MerchantUpdateDTO` |
| DELETE | `/api/v1/merchants/{id}` | 删除商户 | - |
| GET | `/api/v1/merchants/search?keyword=xxx` | 模糊搜索商户 | `keyword` (query) |

#### MerchantCreateDTO

```json
{
  "name": "商户名称 (必填)",
  "alias": "别名（用于模糊匹配，如：SBK,Starbucks）",
  "categoryId": "关联分类ID",
  "color": "颜色 (默认 #3498db)",
  "icon": "图标标识",
  "description": "商户描述",
  "tags": ["标签列表"]
}
```

#### MerchantUpdateDTO

```json
{
  "name": "商户名称",
  "alias": "别名",
  "categoryId": "关联分类ID",
  "color": "颜色",
  "icon": "图标标识",
  "description": "商户描述",
  "tags": ["标签列表"],
  "isActive": "是否启用"
}
```

#### MerchantQueryDTO (Query Parameters)

| 参数 | 类型 | 说明 |
|------|------|------|
| `keyword` | string | 名称/别名模糊搜索 |
| `categoryId` | long | 分类ID筛选 |
| `isActive` | boolean | 是否启用筛选 |
| `page` | int | 页码 (默认 1) |
| `size` | int | 每页条数 (默认 20) |
| `sort` | string | 排序字段 (默认 usage_count,desc) |

---

### 6. 报表管理 (ReportController)

**基础路径**: `/api/v1/reports`

| 方法 | 路径 | 功能 | Query 参数 |
|------|------|------|-------------|
| GET | `/api/v1/reports/monthly-summary` | 月度汇总 | `month` (yyyy-MM, 必填) |
| GET | `/api/v1/reports/category-detail` | 分类明细 | `month`, `categoryId` |
| GET | `/api/v1/reports/trend` | 趋势分析 | `months` (默认12), `type` (可选) |
| GET | `/api/v1/reports/compare` | 对比分析 | `month`, `compareWith` |
| GET | `/api/v1/reports/account-summary` | 账户概况 | - |
| GET | `/api/v1/reports/daily-heatmap` | 每日热力图 | `year` (必填) |
| GET | `/api/v1/reports/budget-review` | 预算审查 | `month` (yyyy-MM, 必填) |
| GET | `/api/v1/reports/currency-distribution` | 货币分布 | `month` (yyyy-MM, 必填) |
| GET | `/api/v1/reports/merchant-distribution` | 商户消费分布 | `month` (yyyy-MM, 必填) |

---

### 6. 系统管理 (SystemController)

**基础路径**: `/api/v1/system`

| 方法 | 路径 | 功能 | 请求体/参数 |
|------|------|------|-------------|
| GET | `/api/v1/system/config` | 获取所有配置 | - |
| PUT | `/api/v1/system/config/{key}` | 设置配置 | `{ "value": "xxx" }` |
| GET | `/api/v1/system/currencies` | 获取支持的货币 | - |
| GET | `/api/v1/system/rates` | 获取汇率 | `date` (yyyy-MM-dd, 可选) |
| POST | `/api/v1/system/rates/refresh` | 刷新汇率 | - |
| GET | `/api/v1/system/alerts` | 获取未读告警 | - |
| PUT | `/api/v1/system/alerts/{id}/read` | 标记告警已读 | - |
| POST | `/api/v1/system/telegram/test` | 测试 Telegram 推送 | - |
| DELETE | `/api/v1/system/test-data` | 清空测试数据 | - |
| POST | `/api/v1/system/check-unconfirmed` | 手动触发待确认交易检查 | - |

#### 清空测试数据响应

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "transactions": 4,
    "accounts": 1,
    "recurringRules": 0,
    "alertRules": 3,
    "alertLogs": 4,
    "budgets": 1,
    "budgetItems": 1
  },
  "timestamp": 1234567890
}
```

**说明**: 清空所有业务数据（交易、账户、周期规则、预警规则、预警日志、预算），保留系统预置分类，同时清除 Redis 缓存。

---

### 7. 周期规则管理 (RecurringRuleController)

**基础路径**: `/api/v1/recurring-rules`

| 方法 | 路径 | 功能 | 请求体 |
|------|------|------|--------|
| GET | `/api/v1/recurring-rules` | 获取规则列表 | - |
| GET | `/api/v1/recurring-rules/{id}` | 获取规则详情 | - |
| POST | `/api/v1/recurring-rules` | 创建规则 | `RecurringRuleCreateDTO` |
| PUT | `/api/v1/recurring-rules/{id}` | 更新规则 | `RecurringRuleUpdateDTO` |
| DELETE | `/api/v1/recurring-rules/{id}` | 删除规则 | - |
| POST | `/api/v1/recurring-rules/{id}/toggle` | 启用/停用规则 | - |
| POST | `/api/v1/recurring-rules/{id}/execute` | 手动执行规则 | - |

#### RecurringRuleCreateDTO

```json
{
  "name": "规则名称 (必填)",
  "type": "类型 (必填, 1-支出 2-收入)",
  "amount": "金额 (必填, >0)",
  "currency": "币种 (默认 CNY)",
  "accountId": "账户ID (必填)",
  "categoryId": "分类ID (必填)",
  "frequency": "频率 (必填, DAILY/WEEKLY/MONTHLY/YEARLY)",
  "executeDay": "执行日 (1-28 或 1-7)",
  "startDate": "开始日期 (必填)",
  "endDate": "结束日期 (可选, null=永久)",
  "nextExecute": "下次执行日期",
  "autoConfirm": "是否自动确认 (默认 false)",
  "note": "备注",
  "extFields": "扩展字段"
}
```

#### RecurringRuleUpdateDTO

```json
{
  "name": "规则名称",
  "type": "类型 (1-支出 2-收入)",
  "amount": "金额 (>0)",
  "currency": "币种",
  "accountId": "账户ID",
  "categoryId": "分类ID",
  "frequency": "频率 (DAILY/WEEKLY/MONTHLY/YEARLY)",
  "executeDay": "执行日",
  "startDate": "开始日期",
  "endDate": "结束日期",
  "autoConfirm": "是否自动确认",
  "note": "备注",
  "extFields": "扩展字段",
  "isActive": "是否激活"
}
```

---

### 8. 告警规则管理 (AlertRuleController)

**基础路径**: `/api/v1/alert-rules`

| 方法 | 路径 | 功能 | 请求体 |
|------|------|------|--------|
| GET | `/api/v1/alert-rules` | 获取规则列表 | - |
| GET | `/api/v1/alert-rules/{id}` | 获取规则详情 | - |
| POST | `/api/v1/alert-rules` | 创建规则 | `AlertRuleCreateDTO` |
| PUT | `/api/v1/alert-rules/{id}` | 更新规则 | `AlertRuleUpdateDTO` |
| DELETE | `/api/v1/alert-rules/{id}` | 删除规则 | - |
| POST | `/api/v1/alert-rules/{id}/toggle` | 启用/停用规则 | - |

#### AlertRuleCreateDTO

```json
{
  "name": "规则名称 (必填)",
  "type": "规则类型 (必填)",
  "config": "规则配置 (必填, JSON字符串)",
  "notifyChannel": "通知渠道 (默认 TELEGRAM)",
  "isActive": "是否激活 (默认 true)"
}
```

#### AlertRuleUpdateDTO

```json
{
  "name": "规则名称",
  "type": "规则类型",
  "config": "规则配置 (JSON字符串)",
  "notifyChannel": "通知渠道",
  "isActive": "是否激活"
}
```

---

## 错误码汇总

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 10001 | 参数校验失败 |
| 10002 | 资源不存在 |
| 10003 | 系统内部异常 |
| 10004 | 名称已存在 |
| 20001 | 账户不存在 |
| 20002 | 账户已停用 |
| 20003 | 账户余额不足 |
| 20004 | 账户名称已存在 |
| 20005 | 账户有关联交易记录，不可停用 |
| 30001 | 交易记录不存在 |
| 30002 | 交易金额无效 |
| 30003 | 转账不能是同一账户 |
| 30004 | 分类与交易类型不匹配 |
| 30005 | 交易已确认 |
| 30006 | 分类不存在 |
| 40001 | 分类不存在 |
| 40002 | 系统分类不可删除或修改类型 |
| 40003 | 分类有子分类，不可删除 |
| 40004 | 分类下有交易记录，不可删除 |
| 40005 | 分类存在循环引用 |
| 50001 | 预算不存在 |
| 50002 | 预算已锁定，不可修改 |
| 50003 | 该月份预算已存在 |
| 60001 | 币种不支持 |
| 60002 | 汇率获取失败 |
| 60003 | 汇率 API 限流 |
| 70001 | 商户不存在 |
| 70002 | 商户名称已存在 |
| 70003 | 商户有关联交易记录，不可删除 |
| 70004 | 系统预设商户不可修改/删除 |

---

## 枚举值参考

### 账户类型 (AccountType)

| 值 | 说明 |
|----|------|
| 1 | 借记账户 (DEBIT) |
| 2 | 信用卡 (CREDIT) |
| 3 | 电子钱包 (E_WALLET) |

### 交易类型 (TransactionType)

| 值 | 说明 |
|----|------|
| 1 | 支出 (EXPENSE) |
| 2 | 收入 (INCOME) |
| 3 | 转账 (TRANSFER) |

### 分类类型 (CategoryType)

| 值 | 说明 |
|----|------|
| 1 | 支出分类 |
| 2 | 收入分类 |
| 3 | 转账分类 |

### 周期频率 (Frequency)

| 值 | 说明 |
|----|------|
| DAILY | 每日 |
| WEEKLY | 每周 |
| MONTHLY | 每月 |
| YEARLY | 每年 |

### 告警类型 (AlertType)

| 值 | 说明 | 配置字段 |
|----|------|----------|
| 1 | 预算阈值预警 | `{ "threshold_pct": 80 }` |
| 2 | 单笔大额预警 | `{ "max_amount": "1000" }` |
| 3 | 日消费上限 | `{ "daily_limit": "500" }` |
| 4 | 周消费异常 | 自动检测（阈值50%） |
| 5 | 信用卡还款提醒 | `{ "advance_days": 3 }` |
| 6 | 周期账单提醒 | `{ "advance_days": 1 }` |
| 7 | 预算未设定提醒 | `{ "check_day": 25 }` |
| 8 | 待确认交易提醒 | `{ "min_count": 1 }` |

### 通知渠道 (NotifyChannel)

| 值 | 说明 |
|----|------|
| TELEGRAM | Telegram 消息 |
| EMAIL | 邮件 |
| IN_APP | 应用内通知 |

---

## 源码文件位置

### Controller 文件

| 文件 | 路径 |
|------|------|
| AccountController | `src/main/java/uk/gubin/budgetpilot/controller/AccountController.java` |
| TransactionController | `src/main/java/uk/gubin/budgetpilot/controller/TransactionController.java` |
| CategoryController | `src/main/java/uk/gubin/budgetpilot/controller/CategoryController.java` |
| BudgetController | `src/main/java/uk/gubin/budgetpilot/controller/BudgetController.java` |
| ReportController | `src/main/java/uk/gubin/budgetpilot/controller/ReportController.java` |
| SystemController | `src/main/java/uk/gubin/budgetpilot/controller/SystemController.java` |
| RecurringRuleController | `src/main/java/uk/gubin/budgetpilot/controller/RecurringRuleController.java` |
| AlertRuleController | `src/main/java/uk/gubin/budgetpilot/controller/AlertRuleController.java` |
| MerchantController | `src/main/java/uk/gubin/budgetpilot/controller/MerchantController.java` |

### DTO 文件

所有 DTO 文件位于: `src/main/java/uk/gubin/budgetpilot/dto/`

---

*文档生成时间: 2026-04-18*