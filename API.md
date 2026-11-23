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
| GET | `/api/v1/accounts/total-assets` | 获取总资产（CNY 汇率折算） | - |

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
| GET | `/api/v1/transactions/summary` | 按同一组条件汇总金额（笔数/支出/收入/转账/退款/净额） | `TransactionQueryDTO` |
| GET | `/api/v1/transactions/tags` | 查询当前用户使用过的标签 | - |
| GET | `/api/v1/transactions/ext-keys` | 查询扩展字段使用过的 key | - |
| POST | `/api/v1/transactions/annotate` | 批量补充标签/扩展字段（不动余额） | `TransactionAnnotateDTO` |
| GET | `/api/v1/transactions/{id}` | 获取交易详情 | - |
| PUT | `/api/v1/transactions/{id}` | 更新交易 | `TransactionUpdateDTO` |
| DELETE | `/api/v1/transactions/{id}` | 删除交易 | - |
| POST | `/api/v1/transactions/{id}/confirm` | 确认交易 | - |
| POST | `/api/v1/transactions/reclassify` | 批量修改分类（只改 category_id） | `TransactionReclassifyDTO` |

#### TransactionCreateDTO

```json
{
  "type": "交易类型 (必填, 1-支出 2-收入 3-转账 4-退款)",
  "amount": "金额 (必填；非转账 >0；转账可为 0，用于购汇等自转账记录)",
  "externalId": "外部幂等ID (可选，同一用户下唯一，重复请求返回已有交易)",
  "currency": "币种 (默认 CNY)",
  "transactionDate": "交易日期",
  "transactionTime": "交易时间",
  "note": "备注",
  "accountId": "账户ID (必填)",
  "targetAccountId": "目标账户ID (转账用)",
  "categoryId": "分类ID (必填)",
  "merchantId": "商户ID (支出交易可选)",
  "merchantName": "商户名称 (用于自动创建商户)",
  "autoCreateMerchant": "是否自动创建商户 (默认 true)",
  "tags": ["标签列表"],
  "attachmentUrls": ["附件URL列表"],
  "isConfirmed": "是否确认 (默认 true)",
  "reviewStatus": "人工复核状态 PENDING/REVIEWED (默认 REVIEWED)",
  "isRecurring": "是否周期交易",
  "recurringId": "周期规则ID",
  "extFields": {},
  "metadata": {}
}
```

> `isConfirmed` 是账务入账确认：`false` 时不影响账户余额。`reviewStatus` 是人工语义复核：`PENDING` 时余额已记，但不计入收支报表、预算和预警。两者互不影响。

> 转账（`type=3`）允许 `amount=0` 且 `accountId=targetAccountId`，用于购汇等“同一账户内部换算”的记录；不会改变任何账户余额。其他交易类型金额必须大于 0。

#### TransactionQueryDTO (Query Parameters)

| 参数 | 类型 | 说明 |
|------|------|------|
| `type` | int | 交易类型 (1-支出 2-收入 3-转账 4-退款) |
| `reviewStatus` | string | PENDING-待人工复核 / REVIEWED-已复核 |
| `accountId` | long | 账户ID（同时匹配转出账户 `account_id` 和转入账户 `target_account_id`） |
| `categoryId` | long | 分类ID |
| `startDate` | string | 开始日期 (yyyy-MM-dd) |
| `endDate` | string | 结束日期 (yyyy-MM-dd) |
| `minAmount` | decimal | 最小金额 |
| `maxAmount` | decimal | 最大金额 |
| `currency` | string | 币种 |
| `keyword` | string | 关键字搜索 |
| `tags` | list | 标签列表（任意一个命中即可；可重复传参或逗号分隔） |
| `confirmed` | boolean | 是否已确认 |
| `reconciled` | boolean | 是否已与渠道账单对账 |
| `extKey` | string | 扩展字段键（JSON path 的 key） |
| `extValue` | string | 扩展字段值；只传 `extKey` 时按字段存在性过滤 |
| `page` | int | 页码 (默认 1) |
| `size` | int | 每页条数 (默认 20) |
| `sort` | string | 排序字段 (默认 transaction_date,desc) |

#### TransactionUpdateDTO

```json
{
  "type": "交易类型 (1-4)",
  "amount": "金额 (>0)",
  "currency": "币种",
  "transactionDate": "交易日期",
  "transactionTime": "交易时间",
  "note": "备注",
  "accountId": "账户ID",
  "targetAccountId": "目标账户ID",
  "categoryId": "分类ID",
  "merchantId": "商户ID",
  "merchantName": "商户名称 (用于匹配已有或自动创建)",
  "autoCreateMerchant": "是否自动创建商户 (默认 true)",
  "syncMerchantCategory": "修改分类时是否同步更新关联商户的默认分类 (默认 true)",
  "tags": ["标签列表"],
  "attachmentUrls": ["附件URL列表"],
  "isConfirmed": "是否确认",
  "reviewStatus": "PENDING/REVIEWED",
  "extFields": {},
  "metadata": {}
}
```

#### TransactionAnnotateDTO

批量补充标签/扩展字段，只改 `tags` 和 `ext_fields`，不触碰余额、分类、预算：

```json
{
  "items": [
    {
      "id": 123,
      "addTags": ["自动扣款"],
      "extFields": { "channel": "支付宝" }
    }
  ]
}
```

- `addTags` 与已有标签合并去重，不会覆盖用户已有标签；
- `extFields` 按 key 合并，不会删除其他扩展字段。

---

### 2.1 对账管理 (ReconciliationController)

**基础路径**: `/api/v1/reconciliation`

| 方法 | 路径 | 功能 | 请求体 |
|------|------|------|--------|
| POST | `/api/v1/reconciliation/import` | 导入渠道账单（multipart） | `file` + `channel` + 可选 `accountId` + 可选 `force` |
| GET | `/api/v1/reconciliation/batches` | 对账批次列表（最近 100 条） | - |
| GET | `/api/v1/reconciliation/batches/{id}` | 批次详情 | - |
| GET | `/api/v1/reconciliation/batches/{id}/items` | 账单明细分页 | `status` (`MATCHED`/`PENDING`/`IGNORED`/`AMBIGUOUS`) + `page` + `size` |
| GET | `/api/v1/reconciliation/batches/{id}/transactions` | 流水有、账单无（账单周期内未匹配流水） | - |
| POST | `/api/v1/reconciliation/batches/{id}/rematch` | 重新自动匹配（保留手工/补录） | - |
| DELETE | `/api/v1/reconciliation/batches/{id}` | 删除批次（取消交易的对账标记，不删交易） | - |
| GET | `/api/v1/reconciliation/items/{id}/candidates` | 账单行的候选流水 | - |
| POST | `/api/v1/reconciliation/items/{id}/match` | 手工匹配 | `{ "transactionId": 123 }` |
| POST | `/api/v1/reconciliation/items/{id}/unmatch` | 取消匹配 / 恢复忽略 | - |
| POST | `/api/v1/reconciliation/items/{id}/ignore` | 忽略账单行 | `{ "reason": "人工忽略" }` |
| POST | `/api/v1/reconciliation/items/{id}/create-transaction` | 账单项补录为交易并完成对账 | 普通收支：`{ "accountId": 1, "categoryId": 163, "confirmed": true }`；转账：`{ "accountId": 转出账户, "targetAccountId": 转入账户, "confirmed": false }`；把「转账汇款」按支出补录：`{ "type": 1, "accountId": 2, "categoryId": 1228, "confirmed": false }`；普通支出改按转账（充值微信余额）：`{ "type": 3, "accountId": 2, "targetAccountId": 19, "confirmed": false }` |

#### 导入说明

- `channel` 取值：`ALIPAY`、`WECHAT`（微信支付）、`CMB_DEBIT`（招商银行储蓄卡）、`CMB_CREDIT`（招商银行信用卡）。
- `CMB_DEBIT` / `CMB_CREDIT` 必须传 `accountId`（对应的储蓄卡/信用卡账户）。候选流水包含两类：`account_id` 是本账户的支出/转出，以及 `target_account_id` 是本账户的转入（例如从另一个储蓄卡账户归集进来的转账）。
- 微信支付不要传 `accountId`：同一份微信账单里每笔的支付方式不同（储蓄卡、信用卡、零钱），限定单一账户会漏掉其它卡的流水；候选流水按 `ext_fields.channel` 或备注中的「财付通/微信支付/微信」识别。
- 同一用户 + 渠道 + 文件 SHA-256 重复上传直接返回已有批次（`duplicate=true`）。传 `force=true` 时可对已有批次原地重新解析：重新生成账单明细、重新自动匹配，并按 `item_key` 保留原来的手工匹配、补录和忽略结果（`summary.restoredManualMatches` 为恢复数量）。
- 解析支持支付宝「交易记录明细查询」CSV（GB18030/UTF-8，带 4 行说明和页脚汇总）、微信支付「账单流水文件」XLSX、招商银行信用卡电子账单 PDF、招商银行储蓄卡交易流水 PDF。
- 匹配按「金额精确 + 收支方向一致 + 时间 ±10 分钟」自动分配；同金额同时间多候选标记 `AMBIGUOUS`；一笔流水对应多笔账单时做 2~3 笔聚合匹配。
- 信用卡/储蓄卡 PDF 没有时分秒，改按交易日匹配（允许跨日 1 天），同金额同日多笔时用账单摘要与流水备注的相似度决定顺序。
- 信用卡账单中的非人民币账户行会跳过并在批次 `summary.warnings` 提示；多卡账单只匹配批次指定账户的流水。
- 储蓄卡流水按「记账日期 + 交易金额 + 联机余额」解析，负数记支出、正数按摘要区分收入/退款/转账；每行会校验「上一行余额 + 本行金额 = 本行余额」，勾稽不一致写入 `summary.balanceMismatches` 并告警。非人民币账户行同样跳过。
- 储蓄卡流水中的「行内转账转入/转出」按交易类型 3（转账）匹配，不会误记为收入或支出；这类账单行可以在对账页补录，但必须选择转出/转入两个账户，且默认 `confirmed=false`（不计入当前余额），避免历史漏记再次影响已经与银行对齐的余额。
- 储蓄卡流水中的「朝朝宝转出/转入」是账户内部理财申赎，导入时自动标记为 `IGNORED`，忽略原因「朝朝宝内部划转」；「大额存单还本」是存单到期资金回活期，自动忽略，原因「大额存单到期内部划转」。
- 微信账单解析：读取文件头的微信昵称、导出区间和「共 N 笔 / 收入 / 支出 / 中性交易」汇总，解析后核对笔数与金额，不一致写入 `summary.warnings`；交易时间是 Excel 日期序列号，导出丢失日期格式时也能按序列号还原。
- 微信账单的「交易类型」决定流水性质：`转账`、`零钱充值`、`零钱提现` 按交易类型 3（转账）处理；`微信红包-退款`、`七鲜-退款` 等带「退款」的类型按退款（类型 4）处理；`商户消费`、`群收款`、`扫二维码付款` 等按收/支记支出或收入。原支付行即使「当前状态」是 `已全额退款`/`已退款(¥x)`，仍按原支出匹配，退款由单独的退款行处理；已退金额写入 `refunded`，零钱提现备注中的服务费写入 `fee`。
- 微信的「当前状态」存入 `fund_status`，「交易类型」存入 `trade_status`，避免把资金状态误当成退款。
- 补录的转账/退款如果没有真实时分秒（创建时用 12:00 占位），微信账单中同金额、同交易日的行允许按日匹配，但排在精确时间候选之后。
- 微信账单还接受对账补录生成的银行侧交易（`tags` 含「对账补录」或 `external_id` 以 `recon:` 开头）：这类交易在银行流水里往往只留下「快捷支付 收款人」，备注没有微信关键字，时分秒也是 12:00 占位；匹配时按金额、日期和类型回退核对。
- 微信零钱提现会先扣服务费，账单金额比银行到账金额多 0.01；账单行带服务费时，允许按「到账金额 + 服务费 = 账单金额」匹配。
- 微信的 `零钱充值` / `零钱提现` 是中性交易，对账页同样提供「补录」按钮，按转账选择银行卡和微信余额两个账户即可，不计入收入或支出。
- 转账/收支类型放宽互看：账单摘要里的「转账汇款/行内转账」在账本中可能被记成普通收支，普通收支也可能被记成转账；类型不同时仍按金额、日期和对手信息匹配，手工候选列表也会同时展示三种类型。
- 本人账户互转：账单对手名等于 PDF 户名或 `RECONCILIATION_SELF_NAMES`（逗号分隔）中的姓名，且摘要含转账/转入/转出时，按交易类型 3 处理，只影响余额，不计入收入/支出。账单项 VO 返回 `selfTransfer=true`，补录时后端强制 `type=3`。
- 退款账单行优先匹配交易类型 4；如果历史流水把退款记成了支出（类型 1），备注含「退款」的支出流水也会作为候选，避免这类退款永远对不上。
- 同一笔流水可被不同渠道的账单交叉核对（例如支付宝账单与信用卡/储蓄卡账单）；交易表保留最先写入的 `reconcile_channel`，后写入的渠道只记在账单明细上，但会合并该渠道的扩展字段。
- 命中后交易写入 `reconciled=1`、`reconcile_channel`、`reconcile_batch_id`、`reconciled_at`，并在 `ext_fields` 合并渠道字段：支付宝为 `支付宝交易号`、`支付宝商家订单号`、`支付宝交易对方`、`支付宝商品名称` 等；招行信用卡为 `招行信用卡卡号末四位`、`招行信用卡交易日`、`招行信用卡记账日`、`招行信用卡交易摘要`、`招行信用卡账单分类`、`招行信用卡交易地金额`、`招行信用卡账单来源`；招行储蓄卡为 `招行储蓄卡账号`、`招行储蓄卡记账日期`、`招行储蓄卡交易摘要`、`招行储蓄卡联机余额`、`招行储蓄卡交易币种`、`招行储蓄卡对手信息`、`招行储蓄卡流水来源`；微信支付为 `微信昵称`、`微信支付方式`、`微信交易类型`、`微信当前状态`、`微信交易单号`、`微信商户单号`、`微信商品`、`微信备注`、`微信账单来源`。
- 修改已对账交易的金额/日期/时间/币种/账户/类型，或删除交易，会自动解绑账单行并取消对账标记。
- 补录会走正常交易创建流程，按所选账户实际增减余额。

---

### 3. 分类管理 (CategoryController)

**基础路径**: `/api/v1/categories`

| 方法 | 路径 | 功能 | 请求体 |
|------|------|------|--------|
| POST | `/api/v1/categories` | 创建分类 | `CategoryCreateDTO` |
| GET | `/api/v1/categories/tree` | 获取分类树 | `type` (query, 可选) |
| GET | `/api/v1/categories/{id}` | 获取分类详情 | - |
| PUT | `/api/v1/categories/{id}` | 更新分类 | `CategoryUpdateDTO` |
| DELETE | `/api/v1/categories/{id}` | 删除分类（有交易/子分类时拒绝） | - |
| POST | `/api/v1/categories/{sourceId}/merge/{targetId}` | 合并分类 | - |

#### CategoryCreateDTO

```json
{
  "parentId": "父分类ID (默认 0)",
  "name": "分类名称 (必填)",
  "type": "分类类型 (必填, 1-支出 2-收入)",
  "budgetType": "DAILY / FIXED / SPECIAL / NONE (支出分类，默认 DAILY)",
  "excludeFromReports": "是否不计入收支报表（往来款/余额调整用）",
  "icon": "图标",
  "color": "颜色",
  "sortOrder": "排序 (默认 0)"
}
```

#### CategoryUpdateDTO

```json
{
  "parentId": "父分类ID",
  "name": "分类名称 (最长 30)",
  "type": "分类类型 (1-2)",
  "budgetType": "DAILY / FIXED / SPECIAL / NONE",
  "excludeFromReports": "是否不计入收支报表",
  "icon": "图标",
  "color": "颜色",
  "sortOrder": "排序"
}
```

分类树的每个节点额外返回：

- `budgetType`：预算属性；收入分类固定为 `NONE`；
- `excludeFromReports`：为 `true` 时该分类的交易只影响账户余额，不进收支报表/预算（往来款、余额调整）；
- `transactionCount`：该分类及其子分类的交易笔数。

合并分类会把交易、月度预算、年度预算、商户默认分类、周期规则重指向目标分类，然后停用源分类；源分类必须是没有子分类的叶子。
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
| GET | `/api/v1/budgets/fixed-summary` | 固定支出实际发生额 | `month` (query) |

月度预算只接受 `budgetType=DAILY` 的分类；固定/专项分类会被拒绝。年度预算只接受 `budgetType=SPECIAL`。

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

**分类层级规则**

- 预算可以设在任意一级支出分类。
- 父分类与子分类不能同时设置，否则父分类已包含子分类消费，会重复计算。
- 交易落在子分类时，会更新子分类预算；没有子分类预算时回退到父分类预算。

---

### 4.1 年度预算 (AnnualBudgetController)

**基础路径**: `/api/v1/annual-budgets`

目前用于专项支出（旅行、学费、大额购物等），按自然年 1/1–12/31 统计。

| 方法 | 路径 | 功能 | 请求体 |
|------|------|------|--------|
| POST | `/api/v1/annual-budgets` | 创建年度预算 | `AnnualBudgetCreateDTO` |
| GET | `/api/v1/annual-budgets/{year}` | 获取年度预算进度 | - |
| GET | `/api/v1/annual-budgets/{year}/progress` | 获取年度预算进度 | - |
| PUT | `/api/v1/annual-budgets/{year}` | 更新年度预算 | `AnnualBudgetUpdateDTO` |

#### AnnualBudgetCreateDTO

```json
{
  "year": "年份 (必填, yyyy)",
  "totalAmount": "年度总预算 (必填, >0)",
  "note": "备注",
  "items": [
    {
      "categoryId": "分类ID (必填)",
      "amount": "分类年度预算 (必填, >0)"
    }
  ]
}
```

#### AnnualBudgetUpdateDTO

```json
{
  "totalAmount": "年度总预算 (>0)",
  "note": "备注",
  "items": [
    {
      "categoryId": "分类ID (必填)",
      "amount": "分类年度预算 (必填, >0)"
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
| GET | `/api/v1/merchants` | 商户列表（兼容接口，返回全部匹配项，供采集端学习映射） | `MerchantQueryDTO` |
| GET | `/api/v1/merchants/page` | 商户分页列表（管理页面使用） | `MerchantQueryDTO` |
| POST | `/api/v1/merchants/consolidate` | 合并重复商户 | - |
| GET | `/api/v1/merchants/{id}` | 商户详情 | - |
| PUT | `/api/v1/merchants/{id}` | 更新商户 | `MerchantUpdateDTO` |
| DELETE | `/api/v1/merchants/{id}` | 删除商户 | - |
| GET | `/api/v1/merchants/search` | 模糊搜索商户 | `keyword` (可选), `limit` (默认 10) |

#### 商户聚合

支付渠道返回的原始名称（如 `支付宝-北京三快在线科技有限公司`）不会逐条创建商户，
而是先归一化为品牌名（如 `美团`），原始名称保存在 `t_merchant_alias`。转换逻辑在
`MerchantNameNormalizer`：清洗渠道前缀/公司后缀/门店后缀，再套品牌别名。

`POST /api/v1/merchants/consolidate` 对当前用户的历史数据执行一次幂等合并：

- 按归一化名称分组，保留交易最多的一条为主商户；
- 把重复商户的交易、周期规则、别名重新指向主商户；
- 原始名称写入别名表；
- 商户默认分类取该商户历史交易中出现最多的分类；
- 重算 `usage_count` 和 `last_used_at`。

响应示例：

```json
{
  "code": 0,
  "data": {
    "groups": 253,
    "mergedGroups": 0,
    "deleted": 0,
    "transactionsRepointed": 0,
    "aliasesCreated": 0
  }
}
```

`GET /api/v1/merchants` 与 `/merchants/page` 的每一项还包含：

```json
{
  "id": 4,
  "name": "美团",
  "alias": null,
  "aliases": [
    "支付宝-北京三快在线科技有限公司",
    "支付宝-支付宝-消费-北京三快在线科技…快捷支付",
    "财付通-美团",
    "财付通-美团平台商户"
  ],
  "aliasCount": 4,
  "transactionCount": 129,
  "categoryId": 163,
  "categoryName": "外卖"
}
```

采集端（accounting_buddy）会把 `aliases` 一起加入商户学习映射，因此网页里修正一次
分类后，后续原始名称相同的邮件也能命中。

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
| `keyword` | string | 名称/别名/原始名称模糊搜索 |
| `categoryId` | long | 分类ID筛选 |
| `isActive` | boolean | 是否启用筛选 |
| `page` | int | 页码 (默认 1，仅 `/merchants/page` 使用) |
| `size` | int | 每页条数 (默认 20，最大 200，仅 `/merchants/page` 使用) |
| `sort` | string | 排序字段 (默认 usage_count,desc) |

---

### 6. 报表管理 (ReportController)

**基础路径**: `/api/v1/reports`

| 方法 | 路径 | 功能 | Query 参数 |
|------|------|------|-------------|
| GET | `/api/v1/reports/overview` | 报表聚合（KPI/结构/排行/趋势/日历/预算/账户/洞察） | `startDate`, `endDate` (yyyy-MM-dd, 必填), `compare` (PREV/YOY/NONE，默认 PREV), `scope` (ALL/DAILY/FIXED/SPECIAL，默认 ALL), `level` (1/2，默认 1) |
| GET | `/api/v1/reports/net-worth` | 某月净资产快照（每账户余额 + 环比） | `month` (yyyy-MM，可选，默认上一个已结束月份) |
| GET | `/api/v1/reports/monthly-summary` | 月度汇总 | `month` (yyyy-MM, 必填), `level` (1=一级汇总, 2=二级明细, 默认 1) |
| GET | `/api/v1/reports/yearly-summary` | 年度汇总 | `year` (yyyy, 必填), `level` (1/2, 默认 1) |
| GET | `/api/v1/reports/category-detail` | 分类明细 | `categoryId` (必填), `month` 或 `startDate`+`endDate`, `scope` (ALL/DAILY/FIXED/SPECIAL，默认 ALL) |
| GET | `/api/v1/reports/trend` | 趋势分析 | `months` (默认12), `type` (可选) |
| GET | `/api/v1/reports/compare` | 对比分析 | `month`, `compareWith` |
| GET | `/api/v1/reports/account-summary` | 账户概况 | - |
| GET | `/api/v1/reports/daily-heatmap` | 每日热力图 | `year` (必填) |
| GET | `/api/v1/reports/budget-review` | 预算审查 | `month` (yyyy-MM, 必填) |
| GET | `/api/v1/reports/currency-distribution` | 货币分布 | `month` (yyyy-MM, 必填) |
| GET | `/api/v1/reports/merchant-distribution` | 商户消费分布 | `month` (yyyy-MM, 必填) |
| GET | `/api/v1/reports/ext-field-distribution` | 任意扩展字段分布 | `month`, `key`, `type` (默认 1) |

`monthly-summary` 的分类占比同时返回两套数据：

- `categoryShares`：全部可统计支出。
- `categorySharesExcludingFixed`：剔除 `budgetType=FIXED` 的分类（房租、房贷、保险等）后的支出占比，百分比以剔除后的总额为分母。

`merchant-distribution` 同样返回两套数据：`merchantShares` 和 `merchantSharesExcludingFixed`，后者剔除固定支出分类后重新计算商户占比。

`yearly-summary` 返回 `yearlySummary`，与月度口径一致地过滤已确认、已复核的交易：

- `totalIncome` / `totalExpense` / `balance`：全年收入、净支出（退款按类型 4 冲减）和结余。
- `avgMonthlyExpense` / `monthCount`：月均支出；历史年份按 12 个月计算，本年按已过月份计算。
- `months`：12 个月的 `income` / `expense` / `balance`，无数据的月份补零。
- `categoryShares` / `categorySharesExcludingFixed`：全年分类占比（可切换一级/二级，并可剔除固定支出分类）。
- `tagShares`：全年支出标签统计。
- `merchantShares`：全年商户消费占比（前 10）。
- `compare`：与上一年的收入/支出同比及变化百分比；上一年为 0 时百分比为 `null`。

---

### 7. 系统管理 (SystemController)

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
| POST | `/api/v1/system/whatsapp/test` | 测试 WhatsApp 推送 | - |
| DELETE | `/api/v1/system/test-data` | 清空测试数据 | - |
| POST | `/api/v1/system/check-unconfirmed` | 手动触发待确认交易检查 | - |

> 全局配置读写（`/system/config`）、通知测试、汇率刷新、清空测试数据、待确认检查均需要 **ADMIN** 角色；普通用户只能读写自己的 `/users/config`。

#### 清空测试数据响应

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "transactions": 4,
    "merchantAliases": 2,
    "merchants": 1,
    "accounts": 1,
    "recurringRules": 0,
    "alertRules": 3,
    "alertLogs": 4,
    "budgets": 1,
    "budgetItems": 1,
    "annualBudgets": 1,
    "annualBudgetItems": 3
  },
  "timestamp": 1234567890
}
```

**说明**: 清空所有业务数据（交易、商户、商户别名、账户、周期规则、预警规则、预警日志、月度预算、年度预算），保留系统预置分类，同时清除 Redis 缓存。

#### Telegram 配置说明

Telegram 推送服务的配置读取优先级为：
1. 数据库 `t_config` 表中的 `telegram_bot_token` 和 `telegram_chat_id`
2. 回退到 `application.yml` 中的 `budgetpilot.telegram.bot-token` / `budgetpilot.telegram.chat-id`

可通过 `PUT /api/v1/system/config/{key}` 修改数据库中的配置，如：
```
PUT /api/v1/system/config/telegram_bot_token
{ "value": "123456:ABC-xxx" }
```

#### WhatsApp 配置说明

WhatsApp 推送走官方 **Agent Platform API**（`https://api.whatsapp.com/agent/v1`），消息只能发送给创建该 Agent 的本人。配置读取优先级：

1. 数据库 `t_config` 表中的 `whatsapp_api_key`、`whatsapp_to`、`whatsapp_base_url`
2. 回退到 `application.yml` 中的 `budgetpilot.whatsapp.api-key` / `.to` / `.base-url`（对应环境变量 `BUDGETPILOT_WHATSAPP_API_KEY`、`BUDGETPILOT_WHATSAPP_TO`、`BUDGETPILOT_WHATSAPP_BASE_URL`）

`whatsapp_to` 使用 Agent Platform 的接收者格式 `user:<id>`，纯数字会自动补全 `user:` 前缀。

定时提醒的默认渠道由 `t_config.notify_channel` 决定（`TELEGRAM` / `WHATSAPP` / `BOTH` / `APP`，默认 `TELEGRAM`）；单条预警规则可在规则中单独指定渠道。

> `t_config` 中的 Telegram / WhatsApp 配置仅用于**系统定时提醒与预警**，不用于用户公网登录验证码；后者只读 `t_user_config`（见 9.3）。

```
PUT /api/v1/system/config/whatsapp_api_key
{ "value": "<Agent API Key>" }

PUT /api/v1/system/config/whatsapp_to
{ "value": "user:12345678901234" }

PUT /api/v1/system/config/notify_channel
{ "value": "WHATSAPP" }
```

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
  "merchantId": "商户ID (可选，仅支持已有商户)",
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
  "merchantId": "商户ID",
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

### 9. 认证与用户管理 (Auth & User)

#### 9.1 认证接口

**基础路径**: `/api/v1/auth`

| 方法 | 路径 | 功能 | 请求体 | 需登录 |
|------|------|------|--------|--------|
| POST | `/api/v1/auth/login` | 用户登录 | `LoginDTO` | 否 |
| POST | `/api/v1/auth/verify` | 校验公网登录验证码并换取 token | `OtpVerifyDTO` | 否 |
| POST | `/api/v1/auth/otp/resend` | 重新发送公网登录验证码 | `OtpResendDTO` | 否 |
| GET | `/api/v1/auth/ingress` | 查看当前请求来源类型（诊断） | - | 否 |
| POST | `/api/v1/auth/logout` | 退出登录 | - | 是 |
| GET | `/api/v1/auth/info` | 获取当前用户信息 | - | 是 |
| POST | `/api/v1/auth/change-password` | 修改密码 | `ChangePasswordDTO` | 是 |
| POST | `/api/v1/auth/api-key/generate` | 生成 API Key | - | 是 |
| GET | `/api/v1/auth/api-key` | 获取当前用户 API Key | - | 是 |

##### LoginDTO（登录请求）

```json
{
  "username": "用户名 (必填)",
  "password": "密码 (必填)"
}
```

##### 登录响应（Tailscale / 局域网 / 本机）

直接返回 `LoginVO`，`data.token` 为 Sa-Token 凭证，默认 7 天有效：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "sa-token 登录凭证",
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "role": "ADMIN"
  }
}
```

##### 登录响应（公网 / Cloudflare Tunnel）

公网来源在用户名密码校验通过后**不直接返回 token**，而是返回验证码挑战；验证码只通过该用户自己在 `/api/v1/users/config` 配置的 Telegram / WhatsApp 发送。**没有个人配置时不会回退到系统全局配置**，而是直接返回 `80113`，无法公网登录：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "needVerify": true,
    "challengeId": "eaa70b0e-3003-4bc3-940e-781183d4a464",
    "channel": "WhatsApp",
    "recipient": "***1234",
    "expireIn": 300
  }
}
```

##### OtpVerifyDTO

```json
{
  "challengeId": "登录响应中的 challengeId",
  "code": "6 位数字验证码"
}
```

##### OtpResendDTO

```json
{
  "challengeId": "登录响应中的 challengeId"
}
```

`POST /api/v1/auth/otp/resend` 返回新的挑战信息（`challengeId` 不变），同一用户 60 秒内只能发送一次，同一 IP 10 分钟内最多 10 次。验证码 5 分钟有效、最多尝试 5 次、使用后立即失效。公网登录成功后的 token 为 12 小时绝对有效、1 小时无操作过期。

> **ADMIN 账号禁止公网登录**：公网来源检测到 `role=ADMIN` 时直接返回 `80115`「管理员账号仅允许内网登录」，不发送验证码；请从 Tailscale / 局域网登录。

##### UserVO（用户信息响应）

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "role": "ADMIN",
    "isActive": true,
    "lastLogin": "2026-04-18T10:00:00",
    "createdAt": "2026-04-15T00:00:00"
  }
}
```

##### ChangePasswordDTO

```json
{
  "oldPassword": "旧密码 (必填)",
  "newPassword": "新密码 (必填)"
}
```

#### 9.2 用户管理 (User)

**基础路径**: `/api/v1/users`

| 方法 | 路径 | 功能 | 请求体 | 权限 |
|------|------|------|--------|------|
| GET | `/api/v1/users` | 获取用户列表 | - | ADMIN |
| POST | `/api/v1/users` | 创建用户 | `UserCreateDTO` | ADMIN |
| PATCH | `/api/v1/users/{id}` | 更新用户 | `UserUpdateDTO` | ADMIN |
| DELETE | `/api/v1/users/{id}` | 删除用户（级联清理关联数据） | - | ADMIN |
| PUT | `/api/v1/users/{id}/password` | 重置密码 | `{ "password": "xxx" }` | ADMIN |

##### UserCreateDTO

```json
{
  "username": "登录名 (必填，唯一)",
  "password": "密码 (必填)",
  "nickname": "显示名",
  "role": "角色 (ADMIN / USER，默认 USER)"
}
```

##### UserUpdateDTO

```json
{
  "nickname": "显示名",
  "role": "角色 (ADMIN / USER)",
  "isActive": "是否激活"
}
```

#### 9.3 用户个人配置

**基础路径**: `/api/v1/users/config`

| 方法 | 路径 | 功能 | 参数 |
|------|------|------|------|
| GET | `/api/v1/users/config` | 获取当前用户所有配置 | - |
| PUT | `/api/v1/users/config/{key}` | 设置配置 | `{ "value": "xxx" }` |

公网登录验证码只使用以下用户级配置（`t_user_config`），**不回退到 `t_config` 全局配置**：

| key | 说明 |
|-----|------|
| `notify_channel` | 默认发送渠道：`TELEGRAM` / `WHATSAPP` / `BOTH`；未设置时按已配置完整的渠道自动推断 |
| `telegram_bot_token` | 该用户自己的 Telegram Bot Token |
| `telegram_chat_id` | 该用户自己的 Telegram Chat ID |
| `whatsapp_api_key` | 该用户自己的 WhatsApp Agent API Key |
| `whatsapp_to` | 该用户自己的 WhatsApp 接收者（`user:<id>`） |
| `whatsapp_base_url` | 可选，默认 `https://api.whatsapp.com/agent/v1` |

Telegram 需要 `telegram_bot_token` + `telegram_chat_id` 同时配置；WhatsApp 需要 `whatsapp_api_key` + `whatsapp_to` 同时配置。所选渠道必须配置完整（`BOTH` 需要两组都完整），否则返回 `80113`；一组都没配置也无法公网登录。Agent Platform 只能发给创建者本人，家人建议用 Telegram。

```json
PUT /api/v1/users/config/notify_channel
{ "value": "WHATSAPP" }

PUT /api/v1/users/config/telegram_bot_token
{ "value": "123456:ABC-xxx" }

PUT /api/v1/users/config/telegram_chat_id
{ "value": "123456789" }

PUT /api/v1/users/config/whatsapp_api_key
{ "value": "<Agent API Key>" }

PUT /api/v1/users/config/whatsapp_to
{ "value": "user:12345678901234" }
```

---

### 10. 鉴权方式说明

所有 `/api/v1/` 开头的接口（除 `/api/v1/auth/login`、`/api/v1/auth/verify`、`/api/v1/auth/otp/resend`、`/api/v1/auth/ingress` 外）都需要鉴权。支持以下两种方式：

#### 方式一：Token 鉴权（浏览器/前端使用）

在请求头中携带 token：

```
Authorization: <token值>
```

token 值来自登录接口 `/api/v1/auth/login` 返回的 `data.token` 字段。

#### 方式二：API Key 鉴权（程序/脚本调用使用）

在请求头中携带 API Key，无需登录流程：

```
X-Api-Key: <API Key 值>
```

API Key 通过以下方式获取：
1. 登录后在「系统设置」页面查看
2. 调用 `GET /api/v1/auth/api-key`
3. 调用 `POST /api/v1/auth/api-key/generate` 重新生成

**两种鉴权方式互斥**：如果请求头中同时存在 `Authorization` 和 `X-Api-Key`，优先使用 `Authorization` token 校验。

**公网来源禁止使用 API Key**：经 Cloudflare Tunnel 进入的请求如果只带 `X-Api-Key`，会返回 HTTP 403 / `code: 80002`，必须走用户名密码 + 验证码登录。本机、局域网、Tailscale 来源不受影响。

#### 来源识别

服务端按 TCP 对端地址 + Cloudflare 头识别来源，并在响应头返回 `X-BudgetPilot-Ingress`：

| 值 | 判定 |
|----|------|
| `CLOUDFLARE` | 可信隧道 connector 或 Tailscale 节点发来的请求，且带 `CF-Ray` + `CF-Connecting-IP`；需要验证码 |
| `TAILSCALE` | 对端在 `100.64.0.0/10` 或 `fd7a:115c:a1e0::/48` |
| `LAN` | 对端在私有网段 |
| `LOOPBACK` | 本机 `127.0.0.1` |
| `UNKNOWN` | 其他来源，默认直接 403 |

诊断接口：

```
GET /api/v1/auth/ingress
{"code":0,"data":{"ingress":"TAILSCALE","clientIp":"100.64.196.104"}}
```

##### API Key 调用示例

```bash
# 使用 API Key 创建交易（无需登录）
curl -X POST http://127.0.0.1:6060/api/v1/transactions \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: your-api-key-here" \
  -d '{"type":1,"amount":50,"accountId":1,"categoryId":1,"transactionDate":"2026-04-18","note":"午餐"}'

# 使用 API Key 查询账户列表
curl http://127.0.0.1:6060/api/v1/accounts \
  -H "X-Api-Key: your-api-key-here"

# 使用 Token 查询（传统方式）
curl http://127.0.0.1:6060/api/v1/accounts \
  -H "Authorization: your-token-here"
```

> **注意**：通过 API Key 创建的交易会归属于该 API Key 对应的用户，数据隔离机制仍然生效。

#### 角色与菜单可见性

前端根据登录接口返回的 `role` 字段控制菜单可见性：

| role | 可见菜单 |
|------|---------|
| `ADMIN` | 仅"用户"管理菜单 |
| `USER` | 首页、交易、账户、分类、商户、预算、周期、预警、报表、设置（不可见"用户"菜单） |

---

## 错误码汇总

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 10001 | 参数校验失败 |
| 10002 | 资源不存在 |
| 10003 | 系统内部异常 |
| 10004 | 名称已存在 |
| 10005 | 用户名或密码错误 |
| 10006 | 用户已停用 |
| 10007 | 旧密码不正确 |
| 10008 | 登录失败次数过多，请稍后再试 |
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
| 50004 | 年度预算不存在 |
| 50005 | 该年度预算已存在 |
| 60001 | 币种不支持 |
| 60002 | 汇率获取失败 |
| 60003 | 汇率 API 限流 |
| 70001 | 商户不存在 |
| 70002 | 商户名称已存在 |
| 70003 | 商户有关联交易记录，不可删除 |
| 70004 | 系统预设商户不可修改/删除 |
| 80001 | 未登录或登录已过期 |
| 80002 | 无权限访问（需要 ADMIN 角色） |
| 80003 | 用户名已存在 |
| 80101 | 用户不存在 |
| 80110 | 验证码错误或已过期 |
| 80111 | 验证码尝试次数过多，请重新登录 |
| 80112 | 验证码发送失败，请检查通知渠道配置 |
| 80113 | 该账号未配置 Telegram 或 WhatsApp，无法公网登录 |
| 80114 | 验证码发送过于频繁，请稍后再试 |
| 80115 | 管理员账号仅允许内网登录 |

---

## 枚举值参考

### 账户类型 (AccountType)

| 值 | 说明 |
|----|------|
| 1 | 现金 (CASH) |
| 2 | 储蓄卡 (DEBIT) |
| 3 | 信用卡 (CREDIT) |
| 4 | 电子钱包 (E_WALLET) |
| 5 | 投资账户 (INVESTMENT) |

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
| WHATSAPP | WhatsApp（Agent Platform）消息 |
| BOTH | Telegram + WhatsApp 同时发送 |
| APP | 仅应用内通知，不推送外部渠道 |

---

## 源码文件位置

### Controller 文件

| 文件 | 路径 |
|------|------|
| AccountController | `src/main/java/uk/gubin/budgetpilot/controller/AccountController.java` |
| TransactionController | `src/main/java/uk/gubin/budgetpilot/controller/TransactionController.java` |
| CategoryController | `src/main/java/uk/gubin/budgetpilot/controller/CategoryController.java` |
| BudgetController | `src/main/java/uk/gubin/budgetpilot/controller/BudgetController.java` |
| AnnualBudgetController | `src/main/java/uk/gubin/budgetpilot/controller/AnnualBudgetController.java` |
| ReportController | `src/main/java/uk/gubin/budgetpilot/controller/ReportController.java` |
| SystemController | `src/main/java/uk/gubin/budgetpilot/controller/SystemController.java` |
| RecurringRuleController | `src/main/java/uk/gubin/budgetpilot/controller/RecurringRuleController.java` |
| AlertRuleController | `src/main/java/uk/gubin/budgetpilot/controller/AlertRuleController.java` |
| MerchantController | `src/main/java/uk/gubin/budgetpilot/controller/MerchantController.java` |
| AuthController | `src/main/java/uk/gubin/budgetpilot/controller/AuthController.java` |
| UserController | `src/main/java/uk/gubin/budgetpilot/controller/UserController.java` |

### DTO 文件

所有 DTO 文件位于: `src/main/java/uk/gubin/budgetpilot/dto/`

---

*文档最后更新: 2026-04-30*