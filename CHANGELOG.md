# 变更记录

## 2026-11-22（账户）— 按真实余额对账

- 按用户提供的真实余额，对 8338、0611、0886、投资账户、定期存款等账户反算 `initial_balance`，保持 `current_balance` 不变。
- 校准口径修正后，7 个活跃账户全部 `IN_SYNC`。
- 具体余额只存在本地数据库，不写入公开仓库；本文档只记录对账方法。

---

## 2026-11-22（交易）— 港币取现归属 0611

- 历史港币柜台取现 1300 从投资账户改为 0611 的支出（金额 857.80，分类「其他支出」）。
- 同步调整 0611 与投资账户的 `initial_balance`，当前余额保持不变。
- accounting_buddy 规则同步：取现支出记在邮件里的银行卡，不再记投资账户。

---

## 2026-11-22（交易）— 购汇改为 0 元自转账

- 转账允许 `amount=0` 且目标账户=源账户，用于购汇等同一账户内部换算；不改变任何账户余额。其他类型仍要求金额 >0。
- 交易列表「对手方」列对自转账显示「自己」，金额显示 0。
- 历史购汇交易 1298 改为 `0611 → 0611`、金额 0；同步调整 0611 与投资账户的 `initial_balance`，当前余额保持不变。

---

## 2026-11-22（交易记录）— 列表增加类型与对手方

- 桌面端交易列表新增「类型」列（支出 / 收入 / 转账 / 退款）和「对手方」列。
- 转账交易的「对手方」显示对方账户；非转账显示 `-`。账户列只显示交易所属账户。
- 移动端同步显示类型标签和「对手方：xxx」。
- 表格启用横向滚动，避免新增列后挤压。

---

## 2026-11-22（交易记录）— 账户筛选包含转入

- 交易记录按账户筛选时，同时匹配 `account_id` 和 `target_account_id`；有转入的账户（如投资账户）不再漏记录。
- 桌面端与移动端的「账户」列对转账显示「转出账户 → 转入账户」，便于识别方向（后续已拆分为「账户 / 对手方」两列）。

---

## 2026-11-22（账户）— 修复每日余额校准任务

- 校准改用 `amount`（账户币种）而不是 `amountBase`（CNY），避免多币种账户把 CNY 金额加到账户币种余额上。
- 修正转入：交易查询同时包含 `target_account_id = 当前账户` 的转账，目标账户会正确加回转入金额。
- 补上退款（`type=4`）：退款不再在每日 02:30 校准中被抹掉。
- 新增 `ScheduledTasksBalanceTest`，覆盖收入、支出、转出、转入、退款、自转账不重复计算。
- 校准只在余额变化时打印 `old -> new` 日志，便于审计。
- 修复校准任务空跑：定时任务线程没有登录上下文，租户拦截器会追加 `user_id = null`，导致查询 0 行、更新 0 行；改用带 `@InterceptorIgnore(tenantLine = "true")` 的 mapper 方法读取账户/交易并更新余额。
- 校准开始时打印活跃账户数量，便于确认任务确实执行。

---

## 2026-11-22（通知）— 按用户隔离 Telegram / WhatsApp 配置

- 公网登录验证码改为**只使用当前用户自己的配置**（`t_user_config`：`telegram_bot_token` / `telegram_chat_id` / `whatsapp_api_key` / `whatsapp_to` / `whatsapp_base_url`），不再回退到系统全局配置。
- 用户未完整配置 Telegram 或 WhatsApp 时，公网登录返回 `80113`，收不到验证码。
- 设置页所有用户可见「默认通知渠道」选择（Telegram / WhatsApp / 两者都发）和「我的 Telegram 通知」「我的 WhatsApp 通知」卡片；未选择默认渠道时按已配置完整的渠道自动推断。
- 原全局卡片改名为「系统 Telegram / 系统 WhatsApp（定时提醒）」，仅 ADMIN 可见，只用于定时任务。
- 为 gubin（user_id=2）迁移原有全局 WhatsApp 配置到个人配置，并将默认渠道设为 `WHATSAPP`，保持其原有公网登录能力。
- 上一版“未配置回退全局”的行为已移除。

---

## 2026-11-22（安全）— ADMIN 账号禁止公网登录

- 公网来源登录时，若账号角色为 `ADMIN`，直接返回 `80115`「管理员账号仅允许内网登录」，不发送验证码、不创建验证码挑战。
- `/api/v1/auth/verify` 同样校验角色，避免历史挑战被用于在公网创建 ADMIN 会话。
- Tailscale、局域网、本机登录不受影响。

---

## 2026-11-22（安全）— 公网访问来源识别与登录验证码

### 1. 来源识别

- 新增 `ingress` 包与 `IngressFilter`，按 **TCP 对端地址 + Cloudflare 头** 区分请求来源，而不是域名：
  - `CLOUDFLARE`：可信隧道 connector（Tailscale IP）或 Tailscale 节点发来的请求，且带 `CF-Ray` + `CF-Connecting-IP`；
  - `TAILSCALE`：对端在 `100.64.0.0/10` 或 `fd7a:115c:a1e0::/48`；
  - `LAN` / `LOOPBACK`：私有网段 / 本机；
  - `UNKNOWN`：默认 403。
- 只有可信代理发来的 CF 头才会被采信；Tailscale 节点伪造 CF 头也只会进入验证码流程，不会绕过校验。
- 响应头增加 `X-BudgetPilot-Ingress`，新增诊断接口 `GET /api/v1/auth/ingress`。
- 新增配置 `budgetpilot.ingress.*`，可信代理列表可通过 `BUDGETPILOT_INGRESS_TRUSTED_PROXIES` 覆盖。

### 2. 公网登录验证码

- 登录流程拆成「密码校验 → 创建会话」，公网来源在密码正确后不直接发 token，而是生成 6 位验证码并通过该用户配置的通知渠道发送。
- 验证码 5 分钟有效、最多尝试 5 次、一次性使用；同一用户 60 秒内不可重发，同一 IP 10 分钟内最多 10 次。
- 新增 `POST /api/v1/auth/verify`、`POST /api/v1/auth/otp/resend`。
- 公网会话 12 小时绝对有效、1 小时无操作过期；Tailscale / 局域网仍使用默认 7 天会话。
- 验证码接收渠道按用户配置（`t_user_config.notify_channel` / `telegram_chat_id` / `whatsapp_to`），未配置时回退到全局通知配置。

### 3. 访问策略与设置页

- 公网来源禁止使用 `X-Api-Key`（返回 403），本机 / 局域网 / Tailscale 不受影响。
- 全局配置读写、通知测试、汇率刷新等接口限制为 **ADMIN**；普通用户只能读写自己的 `/users/config`。
- 桌面端与移动端设置页新增「登录验证（公网访问）」卡片，每个用户可配置自己的验证码渠道；全局通知与汇率配置仅 ADMIN 可见。
- 登录页增加第二步验证码输入、重新发送倒计时与返回重新登录。
- 新增 `IngressResolverTest`，覆盖 Tailscale、公网、局域网、未知来源、CF 头伪造与 IPv4-mapped IPv6 场景。

### 4. 部署

- `application.yml` 增加 `budgetpilot.ingress` 配置；隧道 connector 的 Tailscale IP 通过 `BUDGETPILOT_INGRESS_TRUSTED_PROXIES` 配置，不写入仓库。
- `RestTemplate` 增加 5 秒连接 / 10 秒读取超时，避免通知渠道异常拖死登录请求。
- `DEPLOY.md` 补充隧道回源拓扑、来源识别验证方法与每用户验证码配置步骤。

---

## 2026-11-22（通知）— 新增 WhatsApp 推送渠道

### 1. WhatsApp 通知服务

- 新增 `WhatsAppNotifyService`，通过 WhatsApp 官方 **Agent Platform API**（`https://api.whatsapp.com/agent/v1/messages`）推送文本消息。
- 配置项：`whatsapp_api_key`、`whatsapp_to`、`whatsapp_base_url`；数据库优先，未配置时回退到环境变量 `BUDGETPILOT_WHATSAPP_API_KEY` / `BUDGETPILOT_WHATSAPP_TO` / `BUDGETPILOT_WHATSAPP_BASE_URL`。
- 接收者支持 `user:<id>` 格式，纯数字自动补全前缀；消息体超过 4000 字自动截断。
- 新增 `POST /api/v1/system/whatsapp/test` 测试推送接口。

### 2. 多渠道分发

- 预警规则的通知渠道新增 `WHATSAPP` 和 `BOTH`（Telegram + WhatsApp 同时发送），桌面端与移动端规则页同步支持，列表显示中文渠道名。
- `AlertLogService` 按渠道分发；`BOTH` 时任一渠道成功即记为已推送。
- 定时提醒（信用卡还款、周期账单、预算超支、周异常、月度总结、预算未设定）不再硬编码 Telegram，改用全局配置 `notify_channel`（默认 `TELEGRAM`）。

### 3. 设置页

- 桌面端与移动端设置页新增「WhatsApp 通知」卡片：API Key、接收者 ID、保存与推送测试。
- 「定时提醒渠道」独立为全局配置卡片，不再放在 WhatsApp 卡片内；Telegram 与 WhatsApp 是并列的渠道配置，全局卡片只负责选择定时提醒走哪个渠道。
- 接收者字段标注为「接收者 ID」，占位提示说明不是手机号；WhatsApp 卡片底部不再保留说明文字。
- 部署文档与 `docker-compose.yml` 补充 WhatsApp 环境变量透传；建表与初始化 SQL 增加对应配置项。

---

## 2026-11-22（交易记录）— 查询条件布局与下拉宽度

### 1. 查询条件统一排版

- 桌面端交易记录页筛选区改为等宽网格：宽屏 4 列、窄屏 2 列，类型、账户、分类、标签、入账状态、复核状态、日期区间等条件统一宽度、标签定宽，不再长短不一。
- 日期区间占两列，避免起止日期挤压；“重置 / 查询”按钮右对齐单独一行。

### 2. 下拉选项完整显示

- 所有筛选下拉（类型、账户、分类树、标签、入账状态、复核状态）取消“菜单与输入框等宽”，下拉菜单宽度按选项文字长度自适应，长账户名、分类名不再被截断成省略号。

---

## 2026-11-22（首页）— 预算进度补全与图表去固定支出

### 1. 首页预算进度

- 首页“预算进度”卡片改为两个页签：**月度进度** 和 **年度进度**。
- 月度页签展示月度总预算、已花、总进度，以及各分类的预算进度。
- 年度页签展示年度专项预算（如旅行/学费/大额购物）的总进度和分类进度。
- 当月没有预算时，月度页签直接提示“本月未设置预算”，不回退显示其他月份的预算。
- 桌面端预算卡片与右侧两张图表卡片等高，预算条目在卡片内部滚动，避免条目过多撑高页面。
- 桌面端使用页签切换；移动端把月度和年度进度上下排列。

### 2. 分类占比去掉固定支出

- 月度汇总接口新增 `categorySharesExcludingFixed`：按交易分类的 `budgetType` 剔除 `FIXED` 分类（房租、房贷、保险），百分比按剔除后的总额重新计算。
- 首页“分类占比”增加“不含固定支出 / 全部支出”切换，默认不含固定支出，便于观察可调控的日常与专项消费。
- 原 `categoryShares` 保持不变，报表页等其他调用方不受影响。

### 3. 商户消费去掉固定支出

- `GET /reports/merchant-distribution` 新增 `merchantSharesExcludingFixed`：同样按交易分类的 `budgetType` 剔除 `FIXED`，并按剔除后的总额重新计算商户占比。
- 首页“商户消费”图增加“不含固定支出 / 全部支出”切换，默认不含固定支出。
- 原 `merchantShares` 保持不变。

---

## 2026-11-22（补充）— 复核交互、状态命名与入口清理

### 1. 复核按钮改为按方向选择

- 删除“确认为借出”单一按钮。待复核并不等于借出，可能是还款、本人转账、真实支出或收入。
- 交易列表“复核”弹窗按方向给出选项：
  - 转出：借出、归还借款、本人账户转出、实际支出（选分类）、保持待复核。
  - 转入：收回借款、借入、本人账户转入、实际收入（选分类）、保持待复核。
- 桌面与移动端交易列表均支持该复核弹窗。

### 2. 复核分类与报表排除

- `t_category` 新增 `exclude_from_reports`：为 `true` 时只影响账户余额，不进收支报表/预算/预警。
- 新增分类：
  - `往来款`（type=1，`NONE + exclude`）：借出、还款、本人转出。
  - `往来款收入`（type=2，`NONE + exclude`）：收回借款、借入、本人转入。
- 报表收入和支出都按 `exclude_from_reports` 过滤；余额调整、往来款不再污染收支。

### 3. 状态命名拆分

- `is_confirmed` 保持“入账确认”语义：`false` 时不影响账户余额。
- `review_status` 的值由 `CONFIRMED` 改为 `REVIEWED`，避免与入账确认撞名；默认 `REVIEWED`。
- 交易列表筛选项改为“入账状态”（已入账/待入账）和“复核状态”（已复核/待复核）。
- 文档补充两者组合关系：`is_confirmed=true + review_status=PENDING` 表示钱已入账但尚未复核，余额已变、报表/预算暂不计。

### 4. 页面入口清理

- 移除交易记录页底部的“新增交易”按钮；保留右上角全局“记一笔”和移动端悬浮“记一笔”，两者原本就是同一个新增表单入口。

---

## 2026-11-22 — 分类收敛、预算属性、退款与人工复核

本次把“消费领域 / 预算方式 / 资金流转”三套语义从同一棵分类树里拆开。

### 1. 分类结构

- 支出一级从 17 个收敛到 11 个：餐饮、交通、居住、购物、医疗、教育、休闲、通讯订阅、人情往来、保险、其他支出。
- 停用/合并空分支：社交、金融、副业、报销、旧房租/房贷、旧保险、旧旅行等。
- 合并：通讯 + 订阅服务 → 通讯订阅；物业 + 维修 → 物业维修；数码 + 家具家电 → 数码家电；电影 + 游戏 → 电影游戏；奖金 + 补贴 → 奖金补贴。
- 房租/房贷移到居住下，旅行移到休闲下，学费移到教育下，大额购物移到购物下；不再保留“固定支出/专项支出”两个分类父级。
- 新增兜底叶子：各大类的“其他”，以及通讯订阅/话费宽带、人情往来/亲属转账、往来款/借出。
- 系统预置分类表清除了历史重复的 18 套数据，`DataInitializer` 改为新结构；新用户不会再拿到旧树。

### 2. 预算属性（`t_category.budget_type`）

| 值 | 含义 | 预算 |
|---|---|---|
| `DAILY` | 日常 | 月度预算 |
| `FIXED` | 固定（房租/房贷/保险） | 不设预算，只展示实际 |
| `SPECIAL` | 专项（旅行/学费/大额购物） | 年度预算 |
| `NONE` | 非预算流水（借出、余额调整、收入） | 不进预算 |

预算计算按属性过滤：月度“居住”预算只累计 `DAILY` 的水电/物业，不包含 `FIXED` 的房租/房贷；年度“旅行”只累计 `SPECIAL`。新增 `GET /api/v1/budgets/fixed-summary?month=YYYY-MM` 展示固定支出实际发生额。

### 3. 退款改为独立交易类型

- `t_transaction.type` 新增 `4=退款`。
- 退款分类归到原消费分类（餐饮/购物/交通等），不再进入“退款”收入分类。
- 报表口径：`净支出 = 支出(type=1) - 退款(type=4)`；退款同时冲减对应分类的月度/年度预算。
- 账户余额：退款为入账方向；历史 77 笔退款已迁移为 type=4。

### 4. 人工复核（`t_transaction.review_status`）

- 新增 `review_status`：`PENDING`（待复核）/ `REVIEWED`（已复核），默认 `REVIEWED`。复核状态不再使用 `CONFIRMED`，避免和入账确认 `is_confirmed` 撞名。
- 两者独立：`is_confirmed` 决定余额是否变动；`review_status=PENDING` 时余额已记，但不进报表/预算/预警。
- 对外转账/用途不明时先写 `PENDING`，分类暂放“往来款/借出”；待复核期间不计入收支、预算、预警。
- 交易列表支持按复核状态筛选；“复核”弹窗按方向给出选项，不再预设为借出：
  - 转出：借出、归还借款、本人账户转出、实际支出（选分类）、保持待复核。
  - 转入：收回借款、借入、本人账户转入、实际收入（选分类）、保持待复核。
- 新增 `exclude_from_reports` 分类字段，往来款/余额调整只影响账户余额，不进收支报表和预算。
- `往来款`（借出/还款/本人转出）与 `往来款收入`（收回借款/借入/本人转入）均为 `NONE + excludeFromReports`。

### 5. 接口与迁移

- `GET /api/v1/categories/tree` 返回 `budgetType` 和 `transactionCount`。
- 新增 `POST /api/v1/categories/{sourceId}/merge/{targetId}`：重指向交易、预算、年度预算、商户、周期规则后停用源分类。
- 迁移脚本：`sql/migrations/20261121_category_budget_review.sql`（新增字段/索引）。
- 升级顺序：备份 → 执行迁移脚本 → 部署新代码 → 清理 `report:*` Redis 缓存 → 重启。分类 ID 尽量保留，已有交易/预算/商户不需要重建。

### 6. 配套 accounting_buddy

- `merchant_rules.yaml` 分类路径改为新树（居住/房租、休闲/旅行、通讯订阅/AI订阅等）。
- 退款邮件写 `type=4`，分类按原商户归到消费分类。
- 对外转账写 `reviewStatus=PENDING`，出口转账暂放“往来款/借出”。
- 44 个单元测试通过。

---

## 2026-11-21 — 外部幂等、批量重分类、商户学习、前端打包

本次与 `~/accounting_buddy`（招行邮件记账）配套上线。本文记录接口/数据库改动、升级顺序、本机部署状态和经验，供后续维护参考。

---

### 1. 交易外部幂等（`externalId`）

**背景**

`API_EXTERNAL.md` 曾承诺“3 秒内重复创建交易返回 10004”，但该幂等逻辑在提交 `5d4e465` 中已被移除，文档未同步。邮件记账重试/重放会真实产生重复交易。

**改动**

- `t_transaction` 新增：
  - `external_id VARCHAR(100) DEFAULT NULL`
  - `UNIQUE INDEX uk_user_external (user_id, external_id)`
- 新增迁移脚本 `sql/migrations/external_id_migration.sql`。
- `TransactionCreateDTO` / `Transaction` / `TransactionVO` 增加 `externalId`。
- `TransactionServiceImpl.create`：
  1. 先按当前用户的 `externalId` 查已有交易，命中直接返回（`code=0`）；
  2. 并发穿越预查时，由唯一索引兜底，捕获 `DuplicateKeyException` 后回查并返回已有交易；
  3. 不重复扣减余额。
- 文档修正：`API_EXTERNAL.md` 的幂等说明、错误码 10004 含义、账户类型枚举、金额精度。

**升级顺序（重要）**

1. 备份数据库；
2. 执行 `sql/migrations/external_id_migration.sql`；
3. 部署新代码并重启。

必须先迁移后发代码，否则交易查询/插入会因未知字段失败。详见 `DEPLOY.md`「交易外部幂等 ID 迁移」。

---

### 2. 批量重分类接口

**接口**

```
POST /api/v1/transactions/reclassify
X-Api-Key: ...
Content-Type: application/json

{"items":[{"id":123,"categoryId":163}, ...]}
```

- 只更新 `category_id`（以及 `updated_at`），**不触碰余额、预算、预警**。
- 校验分类存在、分类类型与交易类型一致；转账（type=3）跳过。
- 受租户拦截器约束，只能操作当前用户的数据。
- 更新后清理对应月份的报表缓存。

用于邮件分类规则升级后的历史数据修正，脚本在 `accounting_buddy/reclassify.py`。

---

### 3. 商户分类学习

- `TransactionUpdateDTO` 新增 `syncMerchantCategory`（默认 `true`）。
- 修改交易分类时，同步更新关联商户的 `category_id`；以后外部系统按商户名匹配时可复用。
- 修复部分更新语义：只传 `categoryId` 等字段时**保留原商户关联**，不会把 `merchant_id` 误清空；显式传 `merchantId` 或 `merchantName` 才改变商户。
- 可为将来的“改一次交易，学会一个商户”闭环提供数据。

---

### 4. 其他修复

- `AccountCreateDTO` 账户类型校验从 1–3 放宽到 1–5，与表结构/前端（现金/储蓄卡/信用卡/电子钱包/投资账户）一致。
- `CategoryServiceImpl.getTree` 改为递归组装，支持任意层级；原来只组装两层，三级及以下分类会丢失。
- 文档：`API.md` 增加重分类接口和 `syncMerchantCategory`；`API_EXTERNAL.md` 修正幂等/枚举/精度；`DEPLOY.md` 增加迁移步骤。

---

### 5. 本机部署与数据初始化（2026-11-21）

> 以下为运维事实，不含密码/密钥。

- MySQL 8.0.46 / Redis 7.0.15 已启用（enabled）。
- `budgetpilot` 库初始化：13 张表 + `sql/02_init_data.sql` + 两个固定 ID 的“余额调整”分类（58/59）。
- 用户 `gubin`（业务数据所有者）与其个人分类已创建；账户覆盖银行卡尾号和投资账户。
- BudgetPilot 以用户级 systemd 服务运行：
  - unit：`~/.config/systemd/user/budgetpilot.service`
  - 环境：`~/.config/budgetpilot.env`（600，含 DB/Redis 连接，**不要提交**）
  - 端口：6060
- 邮件定时器：`accounting-buddy.timer`（用户级，每 5 分钟）。
- 前端已构建并复制到 `src/main/resources/static`，随后重新打包；访问 6060 即可打开网页（桌面 + 移动端 PWA）。
- DB 初始化为空库（0 表）后才建表；未找到旧库备份。初始化前务必确认目标库，**不要执行 `mysqld --initialize` 或删除 `/var/lib/mysql`**。

---

### 6. 验证

- `mvn -q -DskipTests compile` 通过。
- 新增 `ExternalIdIdempotencyTest`（预查命中 + 并发唯一索引兜底），通过。
- 手工接口验证：
  - 同一 `externalId` 连续创建两次，返回同一交易 ID；
  - 修改交易分类后商户分类同步；
  - 批量重分类只改分类，账户余额不变。
- 说明：仓库原有 121 个测试在 HEAD 基线就有大量失败（`@InjectMocks` 未注入 MyBatis-Plus `baseMapper`），与本次改动无关；新增测试使用 `ReflectionTestUtils` 显式注入。

---

### 7. 经验与坑

1. **HTTP 200 不代表成功**：业务错误（余额不足、参数校验等）HTTP 仍为 200，必须看响应体 `code`；只有路径不存在才返回 404。
2. **10004 的真实含义**：`DUPLICATE_NAME`（名称已存在），不是交易重复。外部系统不要再用“收到 10004 就当重复交易”的逻辑。
3. **幂等键要稳定且唯一**：邮件记账用 `cmb:<邮件UID>:<交易序号>`；公积金推算收入用独立后缀，避免插入/顺序变化导致键漂移。唯一索引放在 `(user_id, external_id)`，允许其他用户同键。
4. **确认历史交易不能逐笔走 confirm**：借记卡余额不足会报 20003；批量分类请用只改 `category_id` 的接口，或数据库批量确认后再设置余额。
5. **`02_init_data.sql` 与 `DataInitializer` 的重复初始化**：`DataInitializer` 用 `icon in (food,transport,salary,adjust)` 判断系统分类是否已存在；`02_init_data.sql` 没有“余额调整（adjust）”分类，直接启动会再次插入整套分类。初始化时补插入固定 ID 的 58/59（icon=adjust）可跳过重复初始化。
6. **分类类型校验**：交易的 `type` 必须与分类 `type` 一致（支出不能用收入分类），否则报 30004；外部分类器要先按类型过滤。
7. **租户隔离**：MyBatis-Plus `TenantLineInnerInterceptor` 自动注入 `user_id`；`ApiKeyFilter` 用 `X-Api-Key` 自动登录；`t_user/t_user_config/t_config/t_currency_rate` 是豁免表。
8. **部分更新语义**：`PUT /transactions/{id}` 是部分更新，DTO 没传的字段保持原值；但商户关联需要显式传 `merchantId`/`merchantName`，否则保留原商户（本次修复）。
9. **测试基础设施**：MyBatis-Plus 3.5.9 的 `ServiceImpl` 继承 `CrudRepository`，`@InjectMocks` 不会自动注入 `baseMapper`；单元测试用 `ReflectionTestUtils.setField(service, "baseMapper", mapper)`。旧测试的失败不要误判为业务回归。
10. **前端打包**：Maven 只构建后端；前端需 `npm ci && npm run build`，再把 `dist/*` 复制到 `src/main/resources/static` 后重新打包，否则 `/` 返回 404。`static/` 已被 gitignore，重建/清理后要重新复制。
11. **用户级服务**：`systemctl --user enable --now budgetpilot.service` 需要 linger（本机已开启）；系统 MySQL/Redis 需要 `sudo systemctl enable --now mysql redis-server`。
12. **初始化顺序**：先备份 → 再执行 `external_id_migration.sql` → 再部署/重启；反过来会出现未知字段错误。

---

### 8. 相关文件

- `sql/01_schema.sql`、`sql/migrations/external_id_migration.sql`
- `Transaction.java`、`TransactionCreateDTO.java`、`TransactionUpdateDTO.java`、`TransactionVO.java`
- `TransactionReclassifyDTO.java`
- `TransactionServiceImpl.java`、`TransactionController.java`、`TransactionService.java`
- `CategoryServiceImpl.java`、`AccountCreateDTO.java`
- `API.md`、`API_EXTERNAL.md`、`DEPLOY.md`
- `src/test/.../ExternalIdIdempotencyTest.java`

---

## 2026-11-21（补充）— 前端代码审查缺陷修复

代码审查发现并修复了 10 个前端缺陷（桌面端 + 移动端），以及 3 处后端配套改动。

### 前端修复

1. **预算编辑会清空分类明细**
   - 编辑时从现有预算回填总预算、备注和分类金额；已有但不在当前分类树中的明细也会保留。
   - 桌面端 `编辑` 按钮不再写成 `showCreate = !budget`，改为 `openCreate()`。
   - 移动端无预算时也显示 `创建` 按钮。
   - 配套后端 `BudgetProgressVO` 增加 `note`，`BudgetServiceImpl.update` 拒绝“已有明细却传空 items”的请求，防止误清空。
2. **调整账户余额会丢弃其他编辑**：保存时先提交名称/类型/币种/额度/扩展字段，再弹出余额调整确认；确认调整只调用 adjust-balance。
3. **重置密码 payload 字段错误**：前端由 `{newPassword}` 改为后端约定的 `{password}`。
4. **编辑周期规则重置 nextExecute**：编辑时不再发送 `nextExecute`，仅新建时设置，避免重复生成周期交易。
5. **新增按钮未重置编辑状态**：账户、分类、周期规则、用户管理统一改为 `openCreate()`，先清空 `editingId` 和表单。
6. **交易无法清除商户**：商户选择框增加 `@clear` 清空 `merchantName`；清空后提交空名称，后端解除商户关联。
7. **扩展字段无法清空**：交易和账户更新始终发送 `extFields`（无内容时为 `{}`），而不是省略字段。
8. **报表切换空月份仍显示旧图**：每次加载前先 dispose 并清空四类图表，同时重置汇总卡片和对比数据；移动端还重置 `hasPieData` 等标志。

### 后端配套

- `BudgetProgressVO.note` + `getProgress` 返回备注。
- `BudgetServiceImpl.update` 空明细保护。
- `AccountUpdateDTO` 支持 `type`（1–5），`AccountServiceImpl.update` 应用类型变更；前端账户编辑现在能保存类型。
- 修复余额调整分类查找被租户拦截器过滤的问题：优先使用当前用户自己的“余额调整”分类，缺失时回退系统分类（`CategoryMapper.selectSystemBalanceAdjust`，忽略租户）。此问题在浏览器回归时暴露，会导致所有余额调整报“分类未找到”。

### 验证

- `npm run build` 通过。
- `mvn -q -DskipTests compile/package` 通过。
- 使用系统 Chrome + Playwright 实测：
  - 桌面预算编辑弹窗可打开，总预算/分类金额正确回填；
  - 账户“编辑 → 取消 → 新增”弹窗标题为“新增账户”；
  - 移动端无预算时显示“创建”并可打开抽屉，有预算时可编辑并回填；
  - 页面无 console error（无预算月份的“预算不存在”业务提示除外）。
- API 验证：空 items 更新被拒绝；账户类型 2→4 更新成功；重置密码后可正常登录。
- 测试数据（临时预算、临时账户、临时用户）已清理，账户余额和交易数量未受影响。

---

## 2026-11-21（补充 2）— 报表一级/二级分类视图

**问题**

`monthly-summary` 原先直接按 `Transaction.categoryId` 分组并显示该分类名称。有子分类的交易显示二级（外卖、打车），没有子分类或未细分的交易显示一级（购物、交通），同一张饼图粒度混合，既不是完整一级汇总，也不是纯二级视图。

**改动**

- `/api/v1/reports/monthly-summary` 增加 `level` 参数（默认 1）：
  - `level=1`：把子分类交易归并到父分类，返回干净的一级占比；
  - `level=2`：按实际分类返回二级明细，并在 `CategoryShareItem` 中增加 `parentId/parentName`，前端显示为 `父级/子级`（如 `餐饮/外卖`），子分类无颜色时回退父分类颜色。
- 报表缓存 key 增加级别（`:L1` / `:L2`），交易/账户变更时按 `report:monthly-summary:userId:month*` 清理所有级别缓存。
- 前端报表页（桌面 + 移动端）增加「一级 / 二级」切换，切换时重新加载；Dashboard 保持一级汇总。
- `API.md` 更新 `monthly-summary` 参数说明。

**验证**

- 2026-11 接口：
  - `level=1` 返回 9 个一级分类，支出合计 42,064.81；
  - `level=2` 返回 20 个分类（含 房租/房贷、外卖、打车等，带 parentName），合计同样为 42,064.81；
  - `level=3` 返回 10001 参数错误。
- Playwright 桌面/移动端：切换按钮可见，切换时分别发出 `level=1`、`level=2` 请求，页面无异常 JS 错误（无预算月份的“预算不存在”业务提示除外）。
- 浏览器回归使用临时测试账号，测试后已删除，未改动现有用户密码。

---

## 2026-11-21（补充 3）— 交易列表一级/二级分类列

- `TransactionVO` 增加 `parentCategoryId/parentCategoryName/parentCategoryColor`；交易为二级分类时返回父分类，一级分类交易为 null。
- 列表查询批量加载父分类（`loadParentCategories`），避免逐条查询；创建/详情/更新走单条查询。
- 桌面交易列表新增「一级分类」列，原「分类」列改为「二级分类」：
  - 一级分类交易：一级列显示分类名，二级列显示 `-`；
  - 二级分类交易：一级列显示父分类，二级列显示子分类。
- 移动端交易卡片显示 `一级 / 二级` 路径。
- `API_EXTERNAL.md` 补充三个 parent 字段。
- 验证：API 返回 `堂食 <= 餐饮` 等父子字段；Playwright 用临时用户创建“测试一级/测试二级”交易，列表同时显示两列和两层名称；测试数据已清理，账户余额与交易数量未变。

---

## 2026-11-21（补充 4）— 前端视觉改版与新 Logo

**新 Logo**

- 新增 `frontend/public/logo-mark.svg`：深蓝→宝蓝→青绿渐变圆角方块，白色纸飞机/航向针 + 金色圆环（方向 + 金钱意象）。
- 由 SVG 导出 `logo-192.png`、`logo-512.png` 用于 favicon / Apple Touch / PWA。
- `index.html`、`manifest.json`、桌面/移动 Layout、登录页全部切换到新 Logo；删除旧的 3.8MB `frontend/public/budgetpilot-logo.png`。

**视觉主题**

- `App.vue` 主题主色从暗灰 `#2c3e50` 改为宝蓝 `#2563EB`，成功/警告/错误色统一为 `#10B981 / #F59E0B / #EF4444`。
- 卡片统一 16px 圆角、柔和阴影、浅色表头；背景改为带轻微蓝/青渐变的 `#F5F7FB`。
- 桌面 Layout 改为深色侧边栏（Logo + 菜单），顶部栏半透明毛玻璃、页面标题/副标题、渐变“记一笔”按钮、圆形头像。

**页面**

- 登录页改为左右分栏：左侧品牌渐变面板（Logo、标语、能力点），右侧登录表单；移动端自动隐藏品牌面板并显示紧凑 Logo。
- Dashboard 总资产改为深色渐变 Hero 卡，统计卡片/预算/图表统一新主题。
- 移动端 Layout、Dashboard、交易列表等同步更新圆角、阴影、选中色。
- 交易列表一级/二级分类列的样式与主题统一。

**验证**

- 前端构建、后端打包、服务重启通过。
- Playwright 截图检查登录页（桌面/移动）、Dashboard、交易列表；临时用户与数据已清理，账户余额和交易数量未变。

---

## 2026-11-21（补充 5）— 余额调整分类重命名

**现象**

分类管理里出现两个同名的「余额调整」，看起来像重复数据。

**原因**

不是重复数据：余额调整需要按交易类型分别落到收入分类（type=2）和支出分类（type=1），数据库里各有一条：
- `余额调整收入`（type=2，正向调整）
- `余额调整支出`（type=1，负向调整）

之前两者都叫「余额调整」，在“全部”分类列表中显示为两条同名记录。

**改动**

- 系统分类 58/59 和用户分类复制后的两条记录统一重命名为：
  - type=2 → `余额调整收入`
  - type=1 → `余额调整支出`
- `AccountServiceImpl.getBalanceAdjustCategoryId` 改为按 `icon='adjust'` + `type` 查找，不再依赖名称；`CategoryMapper.selectSystemBalanceAdjust` 同步改为按 icon 查询。
- `DataInitializer` 新建系统分类时直接使用新名称。
- 新增迁移脚本 `sql/migrations/rename_balance_adjust_categories.sql`，已有数据库执行一次即可。
- 已有余额调整交易只改分类名称，不改变分类 ID、金额或账户余额。

**验证**

- 迁移后 4 条记录名称正确（系统 58/59 + 用户 190/191）。
- 临时账户调整余额 0→10 成功，生成交易分类为「余额调整收入」；测试账户/交易已删除。
- 现有 6 个账户余额、1306 笔交易数量未变。

---

## 2026-11-21（补充 6）— 标签筛选与标签报表

**后端**

- `TransactionQueryDTO.tags` 真正生效：任意一个标签命中即可（`JSON_CONTAINS`）。
- `extKey` / `extValue` 真正生效：按 `ext_fields` JSON 精确查询；只传 key 时按存在性过滤。
- 新增 `GET /api/v1/transactions/tags`，返回当前用户使用过的全部标签（去重排序）。
- `monthly-summary` 新增 `tagShares`：按支出交易的标签汇总金额、笔数、占比；一笔交易多个标签会在每个标签下各计一次。
- 更新 `API.md` / `API_EXTERNAL.md`。

**前端**

- 桌面/移动端交易列表新增「标签」多选筛选，查询时以逗号分隔传给后端。
- 报表页新增「标签统计」横向柱状图（桌面 + 移动端），数据来自 `tagShares`。
- 标签筛选项从 `/transactions/tags` 动态加载。

**验证**

- 临时数据：4 笔交易，标签分别为 `出差+可报销`、`家庭`、`出差`、无标签。
- `tags=出差` 返回 2 笔；`extKey=invoice_no&extValue=A1` 返回 1 笔；标签列表返回 `["出差","可报销","家庭"]`。
- 月度报表 `tagShares`：出差 130（65%）、可报销 100（50%）、家庭 50（25%）。
- Playwright 验证桌面标签筛选、桌面/移动端标签图表；临时数据已清理，现有账户/交易未改动。

---

## 2026-11-21（补充 7）— 扩展字段标注与通用分布报表

**后端**

- 新增 `POST /api/v1/transactions/annotate`：批量追加标签、合并 `ext_fields`，只更新这两列，不触发余额/预算/预警。
- 新增 `GET /api/v1/transactions/ext-keys`：返回当前用户使用过的扩展字段 key。
- 新增 `GET /api/v1/reports/ext-field-distribution?month&key&type`：按任意扩展字段 key 汇总金额、笔数、占比；缺失该 key 的记录归入「未标注」。
- `API.md` 补充上述接口和 `TransactionAnnotateDTO`。

**前端报表**

- 报表页（桌面 + 移动端）新增「支付渠道 / 扩展字段统计」卡片：
  - 默认 `extKey=channel`，标题显示「支付渠道」，第二级柱状图展示支付宝/微信/京东等；
  - 卡片右上角可切换其他扩展字段 key（来自 `/transactions/ext-keys`）；
  - 无数据时显示空状态。
- 该卡片是通用扩展字段视图，不仅限于支付渠道。

**数据回填（accounting_buddy）**

- 渠道写入 `extFields.channel`：支付宝 950、微信支付 95、京东支付 57、Apple Pay 4、银联 3、抖音支付 3、程支付 3，共 1115 笔有渠道。
- 交易方式标签：内部转账 23、自动扣款 18、资金归集 9、信用卡还款 9、自动转账 7。
- 共标注 1163 笔；账户余额、交易数量未变。

**验证**

- 2026-11 渠道分布：支付宝 37,230.49（88.5%）、微信 2,713.60（6.5%）、未标注 1,520.40（3.6%）、京东 552.40、银联 27.92、Apple Pay 20.00。
- 临时数据验证 `extKeys=[channel]`、按 channel 分组正确；Playwright 检查「支付渠道」卡片渲染和字段选择器。
- 临时测试数据已清理。
---

## 2026-11-21（补充 8）— 预算支持二级分类

**背景**

固定支出/专项支出改为「一级 + 二级」结构后，旅行、学费、大额购物需要分别设预算；原预算页只列一级分类，无法单独选择 `专项支出/旅行`。

**后端**

- 预算分类校验：`BudgetServiceImpl` 新增 `validateBudgetCategories`：
  - 预算只能设在支出分类；
  - 分类必须属于当前用户；
  - 禁止父分类和子分类同时设置预算（否则父分类已包含子分类消费，会重复计算）。
- `getItem` / `updateItemSpent` 支持按分类链命中：交易落在子分类时，可更新子分类预算，也能回退到父分类预算；跨月调整预算 `spent` 改用交易的实际分类，不再强制取一级分类。
- 预算预警改为从交易分类向上查找最近的预算项，子分类预算也能触发阈值预警。
- 修复 `DataInitializer` 每次启动重复插入系统分类的问题：原先在同类内部调用 `selectCount`，`@InterceptorIgnore` 不生效，租户拦截器过滤掉 `user_id=0` 后误判为空。改用 mapper 上带 `@InterceptorIgnore` 的 `selectSystemDefaults()` 判断。

**前端（桌面 + 移动端）**

- 预算分类列表改为「一级 + 缩进二级」，可直接给 `专项支出/旅行`、`/学费`、`/大额购物` 设预算。
- 总预算改为按明细自动合计，避免手工填写与明细不一致。
- 保存前校验父子分类不能同时设置，并给出明确提示。
- 明细区显示合计金额和层级提示。

**预算数据**

- 已创建 `2026-12` 月度预算，包含日常分类和专项二级分类；具体金额不写入文档，以账本数据为准。
- 房租、房贷、保险属于固定支出，不设月度预算。

**验证**

- `mvn -DskipTests package` 通过，前端 `npm run build` 通过。
- API 验证：父分类 `专项支出` 与子分类 `旅行` 同时设置时返回 `code=10001`，提示「父分类与子分类不能同时设置预算」。
- 月度预算创建/更新成功，进度接口返回预算项。
- 重启后系统分类不再重复插入。

---

## 2026-11-21（补充 9）— 年度专项预算

**背景**

旅行、学费、大额购物是跨月/一次性的大额支出，按月设预算会出现“旅行月超支、平时空置、学费月爆预算”。新增独立的年度预算，专项支出按自然年统计，日常分类仍用月度预算。

**数据库**

- 新增 `t_annual_budget`：`user_id + budget_year + total_amount + note + is_locked`，按用户+年份唯一。
- 新增 `t_annual_budget_item`：`annual_budget_id + category_id + amount`，按预算+分类唯一。
- 迁移脚本：`sql/migrations/annual_budget.sql`；`sql/01_schema.sql` 同步增加建表语句。

**后端**

- 新增 `AnnualBudgetController`：
  - `POST /api/v1/annual-budgets`
  - `GET /api/v1/annual-budgets/{year}`
  - `GET /api/v1/annual-budgets/{year}/progress`
  - `PUT /api/v1/annual-budgets/{year}`
- 年度进度按 `1/1–12/31` 汇总已确认支出；支持父/子分类，禁止父子同时设置。
- 新增错误码 `50004`（年度预算不存在）、`50005`（该年度预算已存在）。
- 删除用户、清空测试数据时同步清理年度预算表。

**前端（桌面 + 移动端）**

- 预算页新增「月度 / 年度专项」切换。
- 年度页只列出 `专项支出` 下的二级分类（旅行、学费、大额购物），总预算按明细自动合计。
- 月度页保持原有行为；月度页仍支持一级/二级分类预算，父子互斥。

**数据**

- 已创建 `2026` 年度专项预算，旅行/学费/大额购物分别设置年度额度；具体金额不写入文档，以账本数据为准。
- `2026-12` 月度预算移除三项专项，只保留日常分类。

**验证**

- `mvn -DskipTests package`、前端 `npm run build` 通过。
- 年度预算创建成功，进度接口返回专项项和全年累计支出。
- 父子分类同时设置仍返回 `code=10001`。
- 现有 `2026-12` 月度预算已更新为日常分类项。

---

## 2026-11-21（补充 10）— 商户聚合与分页修复

**问题**

- 商户页面出现三百多条记录，绝大多数只对应一笔交易。
- 桌面端分页器看起来存在，但点击页码不生效；移动端没有分页，一次性渲染全部商户。

**原因**

- accounting_buddy 每笔交易把邮件中的原始商户名（如 `支付宝-北京三快在线科技有限公司`）
  作为 `merchantName` 发送；`TransactionServiceImpl.create` 调用
  `MerchantService.findOrCreate`，原逻辑对每个没见过的字符串都自动建一条商户。
- 原始名称包含支付渠道前缀、公司全称、门店名和 `快捷支付` 后缀，同一品牌会膨胀成多条。
- `GET /api/v1/merchants` 忽略 `page` / `size`，直接返回全部；桌面端又把 `size` 固定成 1000，
  分页器没有 `onUpdatePage`，所以翻页无效。移动端没有分页组件。

**后端**

- 新增 `MerchantNameNormalizer`：
  - 清洗支付渠道前缀（支付宝、财付通、京东支付、抖音支付、滴滴支付、程支付、Apple Pay、数字人民币等）；
  - 清洗 `快捷支付`、`扫码付款`、`消费` 等后缀；
  - 清洗公司/个体户后缀和门店括号；
  - 品牌别名归一（三快/美团 → 美团，拉扎斯 → 饿了么，金拱门/麦当劳 → 麦当劳，
    盒马、山姆、京东、携程、滴滴、12306、航旅纵横等）。
- 新增 `t_merchant_alias`：保存原始商户名 → 聚合商户的映射。
  - `findOrCreate` 先按聚合名查找，再按原始名别名查找；新原始名自动记录别名。
  - 历史遗留的原始名称商户在首次命中时直接改名，避免继续制造重复。
- 新增 `GET /api/v1/merchants/page`，真正按 `page` / `size` 分页，返回 `items/total/page/size/totalPages`。
- 保留 `GET /api/v1/merchants` 全量列表，避免破坏 accounting_buddy 的商户学习映射。
- 新增 `POST /api/v1/merchants/consolidate`（幂等）：
  - 按归一化名称分组合并历史商户；
  - 重新指向交易、周期规则和别名；
  - 删除重复商户；
  - 商户默认分类改为该商户历史交易中出现最多的分类；
  - 重算 `usage_count` / `last_used_at`。
- 列表 VO 增加 `aliases`、`aliasCount`、`transactionCount`。
- 删除用户、清空测试数据时同步清理商户和商户别名。

**前端（桌面 + 移动端）**

- 商户页改为服务端分页：桌面端 10/20/50/100 每页，移动端底部页码。
- 搜索改为服务端搜索，支持名称、手工别名和聚合前的原始名称，带 300ms 防抖。
- 桌面端增加「合并重复」按钮和确认提示。
- 列表展示交易笔数、原始名称数量、最近使用日期。

**accounting_buddy 联动**

- `build_merchant_map` 读取 BudgetPilot 返回的 `aliases`，把每个原始名称都映射到聚合商户的分类。
- 这样在网页上修正一次分类，后续同样原始名称的邮件仍能命中；商户学习映射从 348 条原始记录收敛为 253 个聚合商户、776 条名称映射。

**迁移**

- 执行 `sql/migrations/merchant_alias.sql`。
- 部署后调用一次 `POST /api/v1/merchants/consolidate`。
- 本次执行结果：348 → 253 个商户，合并 42 组，删除 94 条重复商户，重指向 137 笔交易；再次执行返回 0 变更。

**验证**

- `mvn -DskipTests package`、前端 `npm run build` 通过。
- `MerchantNameNormalizerTest` 通过。
- `/merchants/page?page=2&size=10` 返回正确的第二页和总数。
- 搜索 `三快` 能命中聚合商户 `美团` 及其 4 个原始名称。
- 合并前后交易总数不变，无 `merchant_id` 指向已删除商户的孤儿交易。
