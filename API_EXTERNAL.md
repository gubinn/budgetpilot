# BudgetPilot 外部 API 文档

面向第三方系统集成记账。外部系统通过 **API Key** 调用接口，无需处理前端交互。

---

## 基础信息

- **API 地址**: `https://<your-domain>/api/v1`
- **认证方式**: `X-Api-Key` 请求头
- **响应格式**: 所有接口返回统一包装

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "timestamp": 1776564000
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | `0` = 成功，非 `0` = 错误（见[错误码](#错误码)） |
| `message` | string | 成功时返回 `"ok"`，失败时返回错误描述 |
| `data` | object/array/null | 业务数据，失败时为 `null` |
| `timestamp` | long | Unix 时间戳（秒） |

**错误响应示例:**

```json
{
  "code": 10001,
  "message": "参数校验失败: amount 必须大于 0",
  "data": null,
  "timestamp": 1776564000
}
```

---

## 快速开始

### 1. 获取 API Key

管理员登录后在「设置」页面生成 API Key，或通过接口：

```bash
curl -X POST https://<your-domain>/api/v1/auth/api-key/generate \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: <your-existing-key>"
```

### 2. 调用接口

每个请求携带 API Key：

```bash
curl https://<your-domain>/api/v1/accounts \
  -H "X-Api-Key: your-api-key-here"
```

### 3. 数据隔离

通过 API Key 创建/查询的数据**自动归属于该 Key 对应的用户**。每个用户的数据完全隔离，互不可见。

---

## 接口清单

### 1. 账户管理

**基础路径**: `/api/v1/accounts`

#### 1.1 获取账户列表

```
GET /api/v1/accounts?active=true
X-Api-Key: your-api-key
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `active` | boolean | 否 | 是否只返回激活账户，默认 `true` |

**响应字段:**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 账户 ID |
| `name` | string | 账户名称 |
| `type` | int | `1` = 借记账户（储蓄/现金），`2` = 信用卡，`3` = 电子钱包 |
| `icon` | string | 图标标识 |
| `currency` | string | 币种代码，默认 `CNY` |
| `initialBalance` | decimal | 初始余额 |
| `currentBalance` | decimal | 当前余额 |
| `creditLimit` | decimal | 信用额度（仅 type=2） |
| `billingDay` | int | 账单日（仅 type=2） |
| `paymentDay` | int | 还款日（仅 type=2） |
| `isActive` | boolean | 是否激活 |
| `extFields` | map | 扩展字段（自定义键值对） |
| `createdAt` | string | 创建时间 (ISO 8601) |

**响应示例:**

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "name": "招商银行储蓄卡",
      "type": 1,
      "currency": "CNY",
      "initialBalance": 0.00,
      "currentBalance": 5230.50,
      "isActive": true,
      "extFields": {},
      "createdAt": "2026-04-19T10:00:00"
    },
    {
      "id": 2,
      "name": "信用卡",
      "type": 2,
      "currency": "CNY",
      "creditLimit": 50000.00,
      "currentBalance": -3200.00,
      "billingDay": 15,
      "paymentDay": 5,
      "isActive": true,
      "extFields": {}
    }
  ]
}
```

#### 1.2 获取账户详情

```
GET /api/v1/accounts/{id}
X-Api-Key: your-api-key
```

#### 1.3 创建账户

```
POST /api/v1/accounts
X-Api-Key: your-api-key
Content-Type: application/json
```

| 字段 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|------|------|------|--------|------|------|
| `name` | string | 是 | - | 最长 50 字符，用户内唯一 | 账户名称 |
| `type` | int | 是 | - | `1` / `2` / `3` | 账户类型 |
| `icon` | string | 否 | `"wallet"` | - | 图标标识 |
| `currency` | string | 否 | `"CNY"` | - | 币种 |
| `initialBalance` | decimal | 否 | `0` | - | 初始余额 |
| `creditLimit` | decimal | 否 | - | - | 信用额度（仅 type=2 有效） |
| `billingDay` | int | 否 | - | 1-31 | 账单日（仅 type=2） |
| `paymentDay` | int | 否 | - | 1-31 | 还款日（仅 type=2） |
| `extFields` | object | 否 | `{}` | - | 扩展字段，任意键值对 |

**请求示例:**

```json
{
  "name": "现金",
  "type": 1,
  "currency": "CNY",
  "initialBalance": 0
}
```

#### 1.4 更新账户

```
PATCH /api/v1/accounts/{id}
X-Api-Key: your-api-key
Content-Type: application/json
```

所有字段可选，只更新传入字段。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `name` | string | 最长 50 字符，用户内唯一 | 账户名称 |
| `icon` | string | - | 图标标识 |
| `currency` | string | - | 币种 |
| `initialBalance` | decimal | - | 初始余额 |
| `currentBalance` | decimal | - | 当前余额（直接覆盖） |
| `creditLimit` | decimal | - | 信用额度 |
| `billingDay` | int | 1-31 | 账单日 |
| `paymentDay` | int | 1-31 | 还款日 |
| `isActive` | boolean | - | 是否激活 |
| `extFields` | object | - | 扩展字段（全量替换） |

#### 1.5 删除账户

```
DELETE /api/v1/accounts/{id}
X-Api-Key: your-api-key
```

> 有关联交易的账户不可删除。

#### 1.6 调整账户余额

```
POST /api/v1/accounts/{id}/adjust-balance
X-Api-Key: your-api-key
Content-Type: application/json

{
  "newBalance": 10000.00,
  "reason": "月初余额校准"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `newBalance` | decimal | 是 | 调整后的新余额 |
| `reason` | string | 是 | 调整原因 |

---

### 2. 分类管理

**基础路径**: `/api/v1/categories`

#### 2.1 获取分类树

```
GET /api/v1/categories/tree?type=1
X-Api-Key: your-api-key
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | int | 否 | `1` = 支出分类，`2` = 收入分类，不传返回全部 |

**响应字段:**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 分类 ID |
| `name` | string | 分类名称 |
| `type` | int | `1` = 支出，`2` = 收入，`3` = 转账 |
| `icon` | string | 图标标识 |
| `color` | string | 颜色（#RRGGBB） |
| `children` | array | 子分类数组（递归结构） |

**响应示例:**

```json
{
  "code": 0,
  "data": [
    {
      "id": 10,
      "name": "餐饮",
      "type": 1,
      "icon": "restaurant",
      "color": "#e74c3c",
      "children": [
        { "id": 11, "name": "午餐", "type": 1, "icon": "lunch", "color": "#e74c3c", "children": [] },
        { "id": 12, "name": "晚餐", "type": 1, "icon": "dinner", "color": "#e74c3c", "children": [] }
      ]
    }
  ]
}
```

> 创建交易时只需传入**子分类**的 ID（如 `11`），无需传父分类 ID。

---

### 3. 交易管理（核心接口）

**基础路径**: `/api/v1/transactions`

#### 3.1 创建交易

```
POST /api/v1/transactions
X-Api-Key: your-api-key
Content-Type: application/json
```

| 字段 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|------|------|------|--------|------|------|
| `type` | int | 是 | - | `1`=支出 / `2`=收入 / `3`=转账 | 交易类型 |
| `amount` | decimal | 是 | - | 必须 > 0 | 交易金额 |
| `currency` | string | 否 | `"CNY"` | - | 币种 |
| `transactionDate` | string | 否 | 今天 | `yyyy-MM-dd` | 交易日期 |
| `transactionTime` | string | 否 | 当前时间 | `HH:mm:ss` | 交易时间 |
| `accountId` | long | 是 | - | 账户必须存在且激活 | 源账户 ID |
| `targetAccountId` | long | 否 | - | 不能与 accountId 相同 | 目标账户 ID（仅 type=3 转账时需要） |
| `categoryId` | long | 条件必填 | - | 分类必须存在 | 分类 ID（type=1/2 必填，type=3 不需要） |
| `merchantId` | long | 否 | - | - | 商户 ID（指定已有商户） |
| `merchantName` | string | 否 | - | 最长 50 字符 | 商户名称（与 merchantId 二选一，系统会自动创建商户） |
| `autoCreateMerchant` | boolean | 否 | `true` | - | 是否自动创建不存在的商户 |
| `note` | string | 否 | - | 最长 200 字符 | 备注 |
| `tags` | string[] | 否 | `[]` | - | 标签列表 |
| `attachmentUrls` | string[] | 否 | `[]` | - | 附件 URL 列表 |
| `isConfirmed` | boolean | 否 | `true` | - | 是否确认。确认的交易会影响账户余额；`false` 则为待确认状态，不影响余额 |
| `extFields` | object | 否 | `{}` | - | 扩展字段，任意键值对 |

> **`merchantId` 与 `merchantName` 的关系**: 传入 `merchantId` 则直接使用指定商户；传入 `merchantName` 则系统会查找同名商户，找不到则自动创建（前提 `autoCreateMerchant=true`）。两者都不传则交易不关联商户。

**支出交易示例:**

```json
{
  "type": 1,
  "amount": 35.00,
  "categoryId": 11,
  "accountId": 1,
  "transactionDate": "2026-04-19",
  "transactionTime": "12:30:00",
  "note": "午餐 - 沙县小吃",
  "merchantName": "沙县小吃",
  "tags": ["工作日午餐"],
  "isConfirmed": true
}
```

**转账示例:**

```json
{
  "type": 3,
  "amount": 1000.00,
  "accountId": 1,
  "targetAccountId": 2,
  "transactionDate": "2026-04-19",
  "note": "信用卡还款"
}
```

> 转账不需要 `categoryId`，`targetAccountId` 必填。

**待确认交易示例:**

```json
{
  "type": 1,
  "amount": 200.00,
  "categoryId": 10,
  "accountId": 1,
  "isConfirmed": false,
  "note": "待确认支出"
}
```

**响应字段:**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 交易 ID |
| `type` | int | 交易类型 |
| `amount` | decimal | 交易金额 |
| `currency` | string | 币种 |
| `amountBase` | decimal | 折合基础币种金额 |
| `exchangeRate` | decimal | 汇率 |
| `accountId` | long | 源账户 ID |
| `accountName` | string | 源账户名称 |
| `targetAccountId` | long | 目标账户 ID（转账时有值） |
| `targetAccountName` | string | 目标账户名称（转账时有值） |
| `categoryId` | long | 分类 ID |
| `categoryName` | string | 分类名称 |
| `categoryIcon` | string | 分类图标 |
| `categoryColor` | string | 分类颜色 |
| `merchantId` | long | 商户 ID |
| `merchantName` | string | 商户名称 |
| `merchantColor` | string | 商户颜色 |
| `transactionDate` | string | 交易日期 (yyyy-MM-dd) |
| `transactionTime` | string | 交易时间 (HH:mm:ss) |
| `note` | string | 备注 |
| `tags` | string[] | 标签列表 |
| `attachmentUrls` | string[] | 附件 URL 列表 |
| `isConfirmed` | boolean | 是否已确认 |
| `extFields` | object | 扩展字段 |
| `createdAt` | string | 创建时间 (ISO 8601) |
| `updatedAt` | string | 更新时间 (ISO 8601) |

**成功响应示例:**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 100,
    "type": 1,
    "amount": 35.00,
    "currency": "CNY",
    "accountId": 1,
    "accountName": "招商银行储蓄卡",
    "categoryId": 11,
    "categoryName": "午餐",
    "categoryIcon": "lunch",
    "categoryColor": "#e74c3c",
    "merchantId": 5,
    "merchantName": "沙县小吃",
    "merchantColor": "#3498db",
    "transactionDate": "2026-04-19",
    "transactionTime": "12:30:00",
    "note": "午餐 - 沙县小吃",
    "tags": ["工作日午餐"],
    "attachmentUrls": [],
    "isConfirmed": true,
    "extFields": {},
    "createdAt": "2026-04-19T12:30:00",
    "updatedAt": "2026-04-19T12:30:00"
  },
  "timestamp": 1776564000
}
```

**错误响应示例:**

```json
{
  "code": 20003,
  "message": "账户余额不足",
  "data": null,
  "timestamp": 1776564000
}
```

#### 3.2 查询交易列表

```
GET /api/v1/transactions?page=1&size=20&startDate=2026-04-01&endDate=2026-04-30
X-Api-Key: your-api-key
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | int | 否 | 交易类型 (`1`/`2`/`3`) |
| `accountId` | long | 否 | 账户 ID |
| `categoryId` | long | 否 | 分类 ID |
| `startDate` | string | 否 | 开始日期 (yyyy-MM-dd) |
| `endDate` | string | 否 | 结束日期 (yyyy-MM-dd) |
| `minAmount` | decimal | 否 | 最小金额 |
| `maxAmount` | decimal | 否 | 最大金额 |
| `keyword` | string | 否 | 备注关键字搜索 |
| `confirmed` | boolean | 否 | 是否已确认 |
| `page` | int | 否 | 页码，默认 `1` |
| `size` | int | 否 | 每页条数，默认 `20` |
| `sort` | string | 否 | 排序字段，默认 `transaction_date,desc` |

**响应字段:**

| 字段 | 类型 | 说明 |
|------|------|------|
| `list` | array | 交易列表（字段同创建交易响应） |
| `total` | long | 总记录数 |
| `page` | int | 当前页码 |
| `size` | int | 每页条数 |

#### 3.3 获取交易详情

```
GET /api/v1/transactions/{id}
X-Api-Key: your-api-key
```

#### 3.4 更新交易

```
PUT /api/v1/transactions/{id}
X-Api-Key: your-api-key
Content-Type: application/json
```

所有字段可选，只更新传入的字段。更新后余额自动重新计算。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `type` | int | `1`/`2`/`3` | 交易类型 |
| `amount` | decimal | > 0 | 交易金额 |
| `currency` | string | - | 币种 |
| `transactionDate` | string | yyyy-MM-dd | 交易日期 |
| `transactionTime` | string | HH:mm:ss | 交易时间 |
| `note` | string | 最长 200 字符 | 备注 |
| `accountId` | long | - | 源账户 ID |
| `targetAccountId` | long | - | 目标账户 ID |
| `categoryId` | long | - | 分类 ID |
| `merchantId` | long | - | 商户 ID |
| `merchantName` | string | 最长 50 字符 | 商户名称（自动创建/匹配） |
| `tags` | string[] | - | 标签列表 |
| `attachmentUrls` | string[] | - | 附件 URL 列表 |
| `isConfirmed` | boolean | - | 是否确认 |
| `extFields` | object | - | 扩展字段（全量替换） |

#### 3.5 删除交易

```
DELETE /api/v1/transactions/{id}
X-Api-Key: your-api-key
```

> 删除已确认的交易会自动回滚余额。

#### 3.6 确认交易

```
POST /api/v1/transactions/{id}/confirm
X-Api-Key: your-api-key
```

> 对 `isConfirmed=false` 的交易调用此接口后，交易生效并影响账户余额。已确认的交易不可重复确认。

---

### 4. 商户管理

**基础路径**: `/api/v1/merchants`

> 创建交易时可通过 `merchantName` 自动创建商户。如需预先管理商户（设置别名、关联分类、图标等），可使用以下接口。

#### 4.1 创建商户

```
POST /api/v1/merchants
X-Api-Key: your-api-key
Content-Type: application/json
```

| 字段 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|------|------|------|--------|------|------|
| `name` | string | 是 | - | 最长 100 字符，用户内唯一 | 商户名称 |
| `alias` | string | 否 | - | 最长 200 字符 | 商户别名，用于模糊匹配（如 `SBK` 和 `Starbucks` 都能匹配到星巴克） |
| `categoryId` | long | 否 | - | - | 关联分类 ID |
| `icon` | string | 否 | - | - | 图标标识 |
| `color` | string | 否 | `"#3498db"` | - | 颜色（#RRGGBB） |
| `description` | string | 否 | - | 最长 200 字符 | 商户描述 |
| `tags` | string[] | 否 | `[]` | - | 标签列表 |

**请求示例:**

```json
{
  "name": "星巴克",
  "alias": "SBK,Starbucks",
  "categoryId": 10,
  "color": "#00704A",
  "icon": "coffee",
  "tags": ["咖啡", "连锁"]
}
```

#### 4.2 商户列表

```
GET /api/v1/merchants?keyword=&isActive=true&page=1&size=20
X-Api-Key: your-api-key
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | string | 否 | 按名称/别名模糊搜索 |
| `categoryId` | long | 否 | 按关联分类筛选 |
| `isActive` | boolean | 否 | 是否只返回启用商户，默认 `true` |
| `page` | int | 否 | 页码，默认 `1` |
| `size` | int | 否 | 每页条数，默认 `20` |
| `sort` | string | 否 | 排序字段，默认 `usage_count,desc`（按使用频率排序） |

**响应字段:**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 商户 ID |
| `name` | string | 商户名称 |
| `alias` | string | 商户别名 |
| `categoryId` | long | 关联分类 ID |
| `categoryName` | string | 关联分类名称 |
| `categoryColor` | string | 关联分类颜色 |
| `icon` | string | 图标标识 |
| `color` | string | 颜色 |
| `description` | string | 描述 |
| `tags` | string[] | 标签列表 |
| `usageCount` | int | 使用次数（交易关联计数） |
| `lastUsedAt` | string | 最近使用日期 (yyyy-MM-dd) |
| `isActive` | boolean | 是否启用 |
| `isSystem` | boolean | 是否系统预设商户（不可删除） |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |

#### 4.3 搜索商户

```
GET /api/v1/merchants/search?keyword=star&limit=10
X-Api-Key: your-api-key
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | string | 否 | 搜索关键字（名称/别名） |
| `limit` | int | 否 | 返回条数，默认 `10` |

> 返回数组，按匹配度排序。适合输入框自动补全。

#### 4.4 获取商户详情

```
GET /api/v1/merchants/{id}
X-Api-Key: your-api-key
```

#### 4.5 更新商户

```
PUT /api/v1/merchants/{id}
X-Api-Key: your-api-key
Content-Type: application/json
```

所有字段可选，只更新传入字段。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `name` | string | 最长 100 字符，用户内唯一 | 商户名称 |
| `alias` | string | 最长 200 字符 | 商户别名 |
| `categoryId` | long | - | 关联分类 ID |
| `icon` | string | - | 图标标识 |
| `color` | string | - | 颜色 |
| `description` | string | 最长 200 字符 | 描述 |
| `tags` | string[] | - | 标签列表 |
| `isActive` | boolean | - | 是否启用 |

#### 4.6 删除商户

```
DELETE /api/v1/merchants/{id}
X-Api-Key: your-api-key
```

> 有关联交易的商户不可删除。系统预设商户（`isSystem=true`）不可删除。

---

## 扩展字段说明

交易和账户支持 `extFields` 扩展字段，用于存储业务自定义的键值对。

```json
{
  "type": 1,
  "amount": 100.00,
  "accountId": 1,
  "categoryId": 10,
  "extFields": {
    "projectId": "PRJ-2026-001",
    "reimbursable": true,
    "receiptNo": "RCP-12345"
  }
}
```

- 查询/详情接口会原样返回
- 更新时传入的 `extFields` 会完全覆盖原有值
- 不传则保持原有值不变

---

## 错误码

| 错误码 | 说明 | 常见原因 |
|--------|------|----------|
| `0` | 成功 | - |
| `10001` | 参数校验失败 | 缺少必填字段、金额 <= 0、字符串超长等 |
| `10002` | 资源不存在 | 指定的 ID 不存在 |
| `10003` | 系统内部异常 | 服务端错误 |
| `10004` | 名称已存在 | 账户/商户名称重复 |
| `20001` | 账户不存在 | |
| `20002` | 账户已停用 | |
| `20003` | 账户余额不足 | 借记账户支出超过当前余额 |
| `20004` | 账户名称已存在 | |
| `20005` | 账户有关联交易记录，不可删除 | |
| `30001` | 交易不存在 | |
| `30002` | 交易金额无效 | |
| `30003` | 转账不能是同一账户 | source 和 target 账户相同 |
| `30004` | 分类与交易类型不匹配 | 收入分类不能用于支出交易 |
| `30005` | 交易已确认 | 重复确认 |
| `30006` | 分类不存在 | |
| `30007` | 请选择目标账户 | 转账交易缺少 targetAccountId |
| `40002` | 系统分类不可删除或修改类型 | |
| `40003` | 分类有子分类，不可删除 | |
| `40004` | 分类下有交易记录，不可删除 | |
| `70001` | 商户不存在 | |
| `70002` | 商户名称已存在 | |
| `70003` | 商户有关联交易记录，不可删除 | |
| `70004` | 系统预设商户不可修改/删除 | |
| `80001` | 未登录或登录已过期 | API Key 无效、已撤销或格式错误 |

---

## 枚举值参考

### 账户类型

| 值 | 说明 |
|----|------|
| `1` | 借记账户（储蓄/现金） |
| `2` | 信用卡 |
| `3` | 电子钱包 |

### 交易类型

| 值 | 说明 |
|----|------|
| `1` | 支出 |
| `2` | 收入 |
| `3` | 转账 |

### 分类类型

| 值 | 说明 |
|----|------|
| `1` | 支出分类 |
| `2` | 收入分类 |
| `3` | 转账分类 |

---

## 常用场景示例

### 场景 1: 日常记账

```bash
# 1. 查看现有分类
curl -s "https://<your-domain>/api/v1/categories/tree?type=1" \
  -H "X-Api-Key: your-key"

# 2. 创建一笔支出（自动创建商户）
curl -X POST "https://<your-domain>/api/v1/transactions" \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: your-key" \
  -d '{
    "type": 1,
    "amount": 15.00,
    "categoryId": 11,
    "accountId": 1,
    "transactionDate": "2026-04-19",
    "transactionTime": "08:30:00",
    "note": "早餐",
    "merchantName": "永和豆浆",
    "tags": ["早餐"]
  }'
```

### 场景 2: 信用卡还款（转账）

```bash
curl -X POST "https://<your-domain>/api/v1/transactions" \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: your-key" \
  -d '{
    "type": 3,
    "amount": 5000.00,
    "accountId": 1,
    "targetAccountId": 2,
    "transactionDate": "2026-04-19",
    "note": "信用卡4月还款"
  }'
```

### 场景 3: 待确认交易（后续确认生效）

```bash
# 1. 创建一笔待确认支出（不影响余额）
curl -X POST "https://<your-domain>/api/v1/transactions" \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: your-key" \
  -d '{
    "type": 1,
    "amount": 200.00,
    "categoryId": 10,
    "accountId": 1,
    "isConfirmed": false,
    "note": "晚餐 - 待确认"
  }'

# 2. 核实后确认交易（此时扣减余额）
curl -X POST "https://<your-domain>/api/v1/transactions/{id}/confirm" \
  -H "X-Api-Key: your-key"
```

### 场景 4: 查询月度消费

```bash
curl -s "https://<your-domain>/api/v1/transactions?type=1&startDate=2026-04-01&endDate=2026-04-30" \
  -H "X-Api-Key: your-key"
```

### 场景 5: 预设商户，后续交易关联

```bash
# 1. 创建商户，关联到"餐饮"分类
curl -X POST "https://<your-domain>/api/v1/merchants" \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: your-key" \
  -d '{
    "name": "星巴克",
    "alias": "SBK,Starbucks",
    "categoryId": 10,
    "color": "#00704A",
    "icon": "coffee"
  }'

# 2. 后续创建交易时，使用 merchantId 指定商户
curl -X POST "https://<your-domain>/api/v1/transactions" \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: your-key" \
  -d '{
    "type": 1,
    "amount": 38.00,
    "categoryId": 10,
    "accountId": 1,
    "merchantId": 5,
    "note": "拿铁"
  }'
```

---

*文档更新日期: 2026-04-19*
