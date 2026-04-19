# BudgetPilot 外部 API 文档

面向第三方系统集成记账。外部系统通过 **API Key** 调用接口，无需处理前端交互。

> 内部完整文档见 [API.md](./API.md)

---

## 基础信息

- **API 地址**: `https://<your-domain>/api/v1`
- **认证方式**: `X-Api-Key` 请求头
- **所有接口返回统一格式**:

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "timestamp": 1776564000
}
```

- `code = 0` 表示成功，非 0 表示错误
- 错误响应格式: `{"code": 10001, "message": "参数校验失败: xxx", "data": null, "timestamp": ...}`

---

## 快速开始

### 1. 获取 API Key

管理员登录后在「设置」页面查看或生成 API Key。也可通过接口操作：

```bash
# 生成新的 API Key
curl -X POST https://<your-domain>/api/v1/auth/api-key/generate \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: <your-existing-key>"
```

### 2. 调用接口

每个请求都携带 API Key：

```bash
curl https://<your-domain>/api/v1/accounts \
  -H "X-Api-Key: your-api-key-here"
```

---

## 接口清单

### 1. 账户管理

记账前需要知道账户 ID。如需新建账户，可调用以下接口：

#### 创建账户

```
POST /api/v1/accounts
X-Api-Key: your-api-key
Content-Type: application/json

{
  "name": "现金",
  "type": 1,
  "currency": "CNY",
  "initialBalance": 0
}
```

**参数说明:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | 是 | 账户名称（最长 50 字符） |
| `type` | int | 是 | `1` = 借记账户（储蓄/现金），`2` = 信用卡，`3` = 电子钱包 |
| `currency` | string | 否 | 币种，默认 `CNY`。支持: CNY, USD, EUR, GBP, JPY, HKD, SGD, THB, KRW |
| `initialBalance` | decimal | 否 | 初始余额，默认 0 |
| `creditLimit` | decimal | 否 | 信用额度（仅 type=2 信用卡） |
| `billingDay` | int | 否 | 账单日（仅 type=2） |
| `paymentDay` | int | 否 | 还款日（仅 type=2） |

**响应示例:**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1,
    "name": "现金",
    "type": 1,
    "icon": "wallet",
    "currency": "CNY",
    "initialBalance": 0.00,
    "currentBalance": 0.00,
    "isActive": true,
    "createdAt": "2026-04-19T10:00:00"
  },
  "timestamp": 1776564000
}
```

#### 获取账户列表

```
GET /api/v1/accounts
X-Api-Key: your-api-key
```

查询参数: `active` (可选, 默认 true)

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
      "currentBalance": 5230.50
    },
    {
      "id": 2,
      "name": "信用卡",
      "type": 2,
      "creditLimit": 50000.00,
      "billingDay": 15,
      "paymentDay": 5
    }
  ]
}
```

---

### 2. 分类管理

创建交易时需要指定分类 ID。可先查询现有分类：

#### 获取分类树

```
GET /api/v1/categories/tree?type=1
X-Api-Key: your-api-key
```

`type=1` 支出分类, `type=2` 收入分类

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
        { "id": 11, "name": "午餐", "type": 1 },
        { "id": 12, "name": "晚餐", "type": 1 }
      ]
    },
    {
      "id": 20,
      "name": "交通",
      "type": 1,
      "icon": "car",
      "color": "#3498db",
      "children": []
    }
  ]
}
```

> 创建交易时只需传入**子分类**的 ID（如 `11`），无需传父分类 ID。

---

### 3. 交易管理（核心接口）

#### 创建交易

```
POST /api/v1/transactions
X-Api-Key: your-api-key
Content-Type: application/json

{
  "type": 1,
  "amount": 35.00,
  "categoryId": 11,
  "accountId": 1,
  "transactionDate": "2026-04-19",
  "note": "午餐 - 沙县小吃"
}
```

**参数说明:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | int | 是 | `1` = 支出，`2` = 收入，`3` = 转账 |
| `amount` | decimal | 是 | 金额（必须 > 0） |
| `categoryId` | long | 是 | 分类 ID（type=1/2 必填，type=3 转账不需要） |
| `accountId` | long | 是 | 账户 ID |
| `targetAccountId` | long | 否 | 目标账户 ID（仅 type=3 转账时需要） |
| `transactionDate` | string | 否 | 交易日期（yyyy-MM-dd），默认今天 |
| `transactionTime` | string | 否 | 交易时间（HH:mm:ss），默认当前时间 |
| `currency` | string | 否 | 币种，默认账户币种 |
| `note` | string | 否 | 备注（最长 200 字符） |
| `merchantName` | string | 否 | 商户名称，系统会自动创建商户 |
| `isConfirmed` | boolean | 否 | 是否确认，默认 true（确认的交易会影响余额） |

**响应示例 (成功):**

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
    "merchantName": "沙县小吃",
    "transactionDate": "2026-04-19",
    "note": "午餐 - 沙县小吃",
    "isConfirmed": true,
    "createdAt": "2026-04-19T12:30:00"
  },
  "timestamp": 1776564000
}
```

**响应示例 (错误):**

```json
{
  "code": 20003,
  "message": "账户余额不足",
  "data": null,
  "timestamp": 1776564000
}
```

#### 转账示例

```json
{
  "type": 3,
  "amount": 1000.00,
  "accountId": 1,
  "targetAccountId": 2,
  "transactionDate": "2026-04-19",
  "note": "转账到信用卡还款"
}
```

> 转账不需要 `categoryId`，`targetAccountId` 必填。

#### 查询交易列表

```
GET /api/v1/transactions?page=1&size=20&startDate=2026-04-01&endDate=2026-04-30
X-Api-Key: your-api-key
```

**查询参数:**

| 参数 | 类型 | 说明 |
|------|------|------|
| `type` | int | 交易类型 (1/2/3) |
| `accountId` | long | 账户 ID |
| `categoryId` | long | 分类 ID |
| `startDate` | string | 开始日期 (yyyy-MM-dd) |
| `endDate` | string | 结束日期 (yyyy-MM-dd) |
| `keyword` | string | 备注关键字搜索 |
| `page` | int | 页码，默认 1 |
| `size` | int | 每页条数，默认 20 |
| `sort` | string | 排序，默认 `transaction_date,desc` |

**响应示例:**

```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 100,
        "type": 1,
        "amount": 35.00,
        "currency": "CNY",
        "accountName": "招商银行储蓄卡",
        "categoryName": "午餐",
        "transactionDate": "2026-04-19",
        "note": "午餐 - 沙县小吃"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20
  }
}
```

#### 获取交易详情

```
GET /api/v1/transactions/{id}
X-Api-Key: your-api-key
```

#### 更新交易

```
PUT /api/v1/transactions/{id}
X-Api-Key: your-api-key
Content-Type: application/json

{
  "amount": 40.00,
  "note": "午餐 - 沙县小吃 (修正金额)"
}
```

> 所有字段可选，只更新传入的字段。更新后余额会自动重新计算。

#### 删除交易

```
DELETE /api/v1/transactions/{id}
X-Api-Key: your-api-key
```

> 删除已确认的交易会自动回滚余额。

#### 确认交易

```
POST /api/v1/transactions/{id}/confirm
X-Api-Key: your-api-key
```

> 如果创建交易时传入 `isConfirmed: false`，后续调用此接口确认，生效后会影响余额。

---

## 错误码参考

| 错误码 | 说明 | 常见原因 |
|--------|------|----------|
| 0 | 成功 | - |
| 10001 | 参数校验失败 | 缺少必填字段、金额 <= 0 等 |
| 10002 | 资源不存在 | 账户/分类/交易 ID 不存在 |
| 10003 | 系统内部异常 | 服务端错误 |
| 20001 | 账户不存在 | |
| 20002 | 账户已停用 | |
| 20003 | 账户余额不足 | 借记账户支出超过当前余额 |
| 20004 | 账户名称已存在 | |
| 30001 | 交易不存在 | |
| 30003 | 转账不能是同一账户 | source 和 target 账户相同 |
| 30004 | 分类与交易类型不匹配 | 收入分类不能用于支出交易 |
| 30006 | 分类不存在 | |
| 30007 | 请选择目标账户 | 转账交易缺少 targetAccountId |
| 70001 | 商户不存在 | |
| 80001 | 未登录或登录已过期 | API Key 无效或已被撤销 |

---

## 数据隔离说明

通过 API Key 创建的交易和账户**自动归属于该 API Key 所属的用户**。每个用户的数据完全隔离，互不可见。

---

## 常用场景示例

### 场景 1: 日常记账

```bash
# 1. 查看现有分类
curl -s "https://<your-domain>/api/v1/categories/tree?type=1" \
  -H "X-Api-Key: your-key"

# 2. 创建一笔支出
curl -X POST "https://<your-domain>/api/v1/transactions" \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: your-key" \
  -d '{
    "type": 1,
    "amount": 15.00,
    "categoryId": 11,
    "accountId": 1,
    "transactionDate": "2026-04-19",
    "note": "早餐",
    "merchantName": "永和豆浆"
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

### 场景 3: 查询月度消费

```bash
curl -s "https://<your-domain>/api/v1/transactions?type=1&startDate=2026-04-01&endDate=2026-04-30" \
  -H "X-Api-Key: your-key"
```

---

*文档更新日期: 2026-04-19*
