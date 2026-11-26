# 报表与净资产快照

> 实现位置：`ReportController` / `ReportServiceImpl` / `AnnualReviewServiceImpl` / `AccountSnapshotService`
> 前端：`frontend/src/components/report/ReportHub.vue`、`ReportOverview.vue`、`annual/AnnualOverview.vue`、`frontend/src/components/account/NetWorthSnapshot.vue`

## 1. 页面结构

报表页是一条「选口径 → 看结构 → 找变化 → 对预算 → 下钻」的链路：

1. 全局筛选：月份、环比方式、口径（全部/日常/固定/专项）、分类层级（一级/二级）。
2. 主卡：本期支出 + 日常/固定/专项分段条 + 全部支出；右侧收入、结余、储蓄率、日均。
3. 支出趋势：近 12 个月柱状；日常视图带月度预算虚线；全部视图改为日常/固定/专项堆叠。
4. 日常预算执行：总进度 meter + 分类超支排序；所选月份未设预算时显示空状态。
5. 分类排行：金额、占比、笔数、环比、预算完成度；点行打开明细抽屉（二级分类 + 交易）。
6. 消费日历：日粒度热力图，展示消费节奏和大额日。
7. 商户排行：金额、笔数、分类；高频商户标「常客」。
8. 年度预算：固定 + 专项，花费进度 vs 时间进度。
9. 账户与资产：净资产、资产/负债构成、账户余额、近 12 个月净资产曲线。
10. 自动洞察：预算、结构、最大变化三条结论。

PC 和手机共用同一套取数与计算，手机端单列，年度预算/账户折叠在「展开年度预算与账户」后面。

### 1.1 年度复盘页

报表页顶部有「月度复盘 / 年度复盘」切换，两个视图共用 `/reports` 路由，通过 `?view=annual` 深链。年度页按月度和全年两条线组织：

1. 页头：年份、一级/二级分类；本年未过完、上年无可比数据、上年数据不足时给出提示。
2. 年度成绩单：全年结余、储蓄率、收入/支出、日常/固定/专项结构。
3. 收支节奏：12 个月收入/支出柱 + 结余线；未来月份留空；附最佳/最差月份和数据表。
4. 财务健康：储蓄率、应急金月数、固定支出占比、有支出/无支出天数。
5. 年度预算执行：固定 + 年度专项，花费进度 vs 时间进度。
6. 月度预算纪律：已设预算月份数、超支月份、预算/实际合计。
7. 净资产轨迹：上年 12 月到期末；期初/期末/变化、资产/负债/可动用资金、应急金；差额提示。
8. 分类年度排行（可下钻）、收入来源、商户排行、年度之最。
9. 年度洞察：后端生成 4–6 条结论。

移动端单列；分类、收入、商户、年度之最折叠在「展开分类与商户」后面。

## 2. 口径

### 支出与收入

- 支出：`type=1`，`type=4`（退款）冲减；只统计 `is_confirmed=1`、`review_status=REVIEWED`、分类 `type=1`、`budget_type != NONE`、`exclude_from_reports=0` 的流水。
- 收入：`type=2`，排除 `exclude_from_reports=1` 的分类（往来款收入、余额调整等）。
- 转账（`type=3`）不计入收支。
- 金额统一用 `amount_base`（CNY）；多币种交易按入账时汇率折算。

### 日常 / 固定 / 专项

按交易分类的 `budget_type` 归集：

| budget_type | 口径 |
|---|---|
| `FIXED` | 固定支出（房租、房贷、保险、亲属转账等） |
| `SPECIAL` | 年度专项（旅行、学费、大额购物、车牌租赁等） |
| 其他（含 `DAILY`） | 日常 |

月度预算只统计日常分类；年度预算统计固定 + 专项。报表默认口径为「日常」。

### 派生指标

- 结余 = 收入 − 全部支出（不随口径切换）。
- 储蓄率 = 结余 / 收入。
- 日均支出 = 当前口径支出 / 区间天数。
- 环比：整月按上一个自然月；自定义区间按紧邻的等长区间；同比按去年同期。
- 当前月未结束时返回 `dataStates.partialMonth=true`，前端显示提示。

## 3. API

### 3.1 报表聚合

```
GET /api/v1/reports/overview
  ?startDate=2026-10-01
  &endDate=2026-10-31
  &compare=PREV|YOY|NONE
  &scope=ALL|DAILY|FIXED|SPECIAL
  &level=1|2
```

返回：

| 字段 | 说明 |
|---|---|
| `period` | 区间、天数、对比区间、是否部分月份 |
| `kpis` | 收入、当前口径支出、全部支出、结余、储蓄率、日均、笔数及环比 |
| `scopes` | 日常/固定/专项金额、占比、笔数 |
| `categories` | 分类排行；一级含 `children`（二级）、环比、预算完成度 |
| `trend` | 近 12 个月：日常/固定/专项/合计/收入 |
| `daily` | 区间内逐日：日常/固定/专项/合计/笔数 |
| `merchants` | 商户金额、笔数、分类、口径 |
| `budget` | 所选月份月度预算与分类执行；`exists=false` 表示未设置 |
| `annualBudget` | 固定 + 专项年度预算、时间进度 |
| `accounts` | 净资产、资产/负债、分组、账户、近 12 个月净资产 |
| `dataStates` | `budgetMissing` / `partialMonth` / `multiCurrency` / `noData` |
| `insights` | 预算、结构、最大变化三条洞察 |

### 3.2 分类明细（下钻）

```
GET /api/v1/reports/category-detail
  ?startDate=2026-10-01&endDate=2026-10-31
  &categoryId=123
  &scope=DAILY
```

也兼容旧的 `?month=2026-10&categoryId=123`。返回最多 500 条交易。

### 3.3 净资产快照

```
GET /api/v1/reports/net-worth?month=2026-10
```

`month` 可省略，默认上一个已结束月份。返回该月净资产、每个账户的本期/上期余额与变化、最近 12 个月可选月份。

### 3.4 年度复盘

```
GET /api/v1/reports/annual-overview?year=2026&level=1
```

一次返回年度页需要的全部数据：`period`、`kpis`、`monthly`、`scopes`、`categories`、`incomeCategories`、`netWorth`、`annualBudget`、`monthlyBudget`、`merchants`、`largestExpense`、`dataStates`、`insights`。字段明细见 `API.md` 6.1。

年度口径补充：

- 当前年 `monthsCovered = 当前月`，月均按已过月份；未来月份 `future=true` 且值为 0。
- 当前年同比使用上一年同期（1 月 1 日到去年同一天）；历史年使用完整上一年。
- 上年同期没有可统计流水时隐藏同比；上年只有少量月份时 `compareInsufficient=true`，页面标注“同比仅供参考”。
- `spendingDays` 是有支出（type=1）的日期数；`activityDays` 是本年第一笔可统计流水到区间末的天数，`noSpendDays = activityDays - spendingDays`，避免把开始记账前的日子算成无支出日。
- 应急金 = 可随时动用资金（现金/储蓄卡/电子钱包正余额）÷（日常 + 固定）月均支出；不含投资账户和专项支出。

## 4. 净资产月快照

### 4.1 表

`t_account_balance_snapshot`：

| 字段 | 说明 |
|---|---|
| `user_id` | 用户 |
| `account_id` | 账户 |
| `snapshot_month` | `YYYY-MM`，表示该月月末 |
| `balance` | 月末余额（账户原币） |

唯一键 `(user_id, account_id, snapshot_month)`。迁移：`sql/migrations/20261123_account_balance_snapshot.sql`。

### 4.2 算法

不等到月底记账，而是用当前已校准余额反推：

```
某月末余额 = 当前余额 − 该月末之后已确认流水的影响
```

影响方向与每日余额校准一致：

- 支出 / 转出：减
- 收入 / 退款 / 转入：加
- 用账户原币 `amount`

这样即使银行邮件晚几天导入，只要当前余额校准过，历史月末也能算准。

### 4.3 什么时候跑

1. 每天 02:50（在 02:30 余额校准之后）重算最近 12 个已结束月份，覆盖迟到导入和手工改账。
2. 打开报表或净资产快照时，缺失月份即时补算并落库。
3. 当前月不落库，报表用实时余额作为「本月」点。
4. 未来月份不写。

### 4.4 特性与边界

- 快照是**可重算的派生数据**，不是不可变审计快照；修改历史流水后，历史净资产曲线会随之变化。
- 依赖 `current_balance` 已校准；校准任务停跑时，快照会以最近一次校准余额为准，下一次任务修正。
- 多币种账户目前是「余额直接相加」，未做历史汇率折算；新增外币账户时需要扩展。
- 账户页的「净资产快照」入口：PC 在账户卡片右上角，手机在账户列表上方。

## 5. 空状态

| 情况 | 表现 |
|---|---|
| 所选月份未设月度预算 | 预算卡空状态 + 「去设置预算」 |
| 本期无支出 | 顶部警示「本期没有支出记录」 |
| 当前月未过完 | 顶部提示「本月还没过完」；趋势仍按已发生数据画 |
| 多币种 | 页脚提示「本期含外币交易，按入账汇率折算」 |
| 年度预算未设置 | 年度卡空状态 + 「去设置」 |

## 6. 前端实现

- `ReportOverview.vue`：PC/手机共用，`mobile` prop 只控制布局。
- `NetWorthSnapshot.vue`：账户页的净资产快照抽屉，PC 右侧 / 手机底部。
- 月份、口径、层级、对比方式写入 URL query，刷新和收藏不丢。
- 月度/年度复盘在页面重新可见或窗口重新获得焦点时自动重载，可见状态下每 60 秒轮询一次，并提供「刷新」按钮（`composables/useReportRefresh.js`）；只改变数据拉取时机，不改变统计口径。
- 趋势、分类排行、净资产都有「数据表」切换，tooltip 不是唯一读数方式。
- 加载时保留上一次渲染并降透明度，不闪 skeleton。

## 7. 验证

```sql
-- 快照是否齐全
SELECT snapshot_month, COUNT(*) accounts, ROUND(SUM(balance), 2) net_worth
FROM t_account_balance_snapshot
GROUP BY snapshot_month
ORDER BY snapshot_month DESC;

-- 报表合计与交易列表汇总应一致
SELECT SUM(CASE WHEN type=1 THEN amount_base WHEN type=4 THEN -amount_base ELSE 0 END)
FROM t_transaction
WHERE user_id=? AND is_confirmed=1 AND review_status='REVIEWED'
  AND transaction_date BETWEEN '2026-10-01' AND '2026-10-31';
```