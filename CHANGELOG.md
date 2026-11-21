# 变更记录

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