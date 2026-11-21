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