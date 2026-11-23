# 预警规则（Alert Rules）

本文档描述**当前实现**的提醒规则、触发时机、配置项和通知渠道。设计稿中的早期方案如有出入，以本文档和代码为准。

- 相关代码：`src/main/java/uk/gubin/budgetpilot/service/ScheduledTasks.java`、`TransactionEventListener.java`、`AlertRuleService.java`
- 数据表：`t_alert_rule`、`t_alert_log`
- 页面：`/alert-rules`（桌面端与手机端）
- 最近修订：2026-11-23

## 1. 规则总览

| type | 名称 | 触发方式 | 时间 | config |
|------|------|----------|------|--------|
| 1 | 预算阈值预警 | 交易事件 + 定时检查 | 每笔支出后；每日 20:00 汇总 | `{"threshold_pct": 80}` |
| 2 | 单笔大额预警 | 交易事件 | 每笔支出后 | `{"max_amount": "1000"}` |
| 3 | 日消费上限 | 交易事件 | 每笔支出后 | `{"daily_limit": "500"}` |
| 4 | 周消费异常 | 定时检查 | 每周日 22:00 | `{"deviation_pct": 50}` |
| 5 | 信用卡还款提醒 | 定时检查 | 每日 09:00 | `{"advance_days": 3}` |
| 6 | 周期账单提醒 | 定时检查 | 每日 09:00 | `{"advance_days": 1}` |
| 7 | 预算未设定提醒 | 定时检查 | 每日 09:00，按 `check_day` 发送 | `{"check_day": 25}` |
| 8 | 待确认交易提醒 | 定时检查 | 每日 10:00 | `{"min_count": 1}` |

每月 1 日 03:00 还有一条「月度预算总结」，它不对应可配置规则，作为系统消息写入预警日志（`alert_type=9`、`rule_id=0`）。

## 2. 通用机制

### 2.1 规则读取与启停

- 每个用户拥有自己的一份预置规则副本（新建用户时自动复制）。
- 定时任务按「当前用户 + type + `is_active=1`」查找规则，并读取规则里的 config。
- 找不到启用规则（被删除或停用）时，对应提醒直接跳过。
- 同一类型存在多条启用规则时，**当前只使用 id 最小的一条**。页面仍允许创建重复类型，建议只保留一条。
- 规则配置缺失或 JSON 非法时使用代码默认值，并记录 WARN 日志。

### 2.2 通知渠道

渠道解析优先级：

1. 规则自身的 `notify_channel`；
2. 规则未配置（空）时，回落到全局 `t_config.notify_channel`；
3. 全局也未配置时，默认 `TELEGRAM`。

`t_config.notify_channel` 可选 `TELEGRAM` / `WHATSAPP` / `BOTH` / `APP`。`APP` 只写站内预警日志，不调用外部推送。

新建用户时，预置规则的渠道取当时的全局 `notify_channel`；之后修改全局设置不会自动改写已有规则，需要逐条调整。月度预算总结没有规则，固定使用全局渠道。

### 2.3 日志与租户

- 每次触发写入一条 `t_alert_log`，`user_id` 由租户上下文写入。
- `rule_id` 使用当前用户真实规则 ID，不再使用全局模板 ID。
- 外部推送失败时 `is_sent=0`，站内日志仍保留；预警页面只展示未读日志。
- 月度总结使用 `rule_id=0`、`alert_type=9`。

### 2.4 交易事件的过滤条件

事件型规则（1/2/3）只在以下交易后检查：

- `type=1`（支出）；
- `is_confirmed=1`（已入账）；
- `review_status != PENDING`（未被人工挂起复核）。

退款（type=4）、收入（type=2）、转账（type=3）不触发消费类预警。

## 3. 各规则说明

### 3.1 预算阈值预警（type=1）

**事件触发**：每笔已确认日常支出后，沿分类链找到对应的月度预算项，实时汇总该分类及其日常子分类的净支出（退款冲减），达到 `threshold_pct` 时提醒。

**定时触发**：每日 20:00 检查当月总预算进度，达到 `threshold_pct`（默认 80%）发「预算预警」，达到 100% 发「预算超支」。

限制：

- 只对 `budget_type=DAILY` 的分类生效；固定/专项分类不进入日常预算阈值判断。
- 分类没有预算项或预算金额为 0 时跳过。
- 事件触发在超过阈值后，后续每笔支出都会再次提醒；当前没有「每周期只提醒一次」的去重。

### 3.2 单笔大额预警（type=2）

交易 `amount_base > max_amount` 时提醒。对所有已确认支出生效，不受分类预算类型限制；无分类交易也会检查。

### 3.3 日消费上限（type=3）

统计当天所有已确认支出（`type=1 AND is_confirmed=1`）的 `amount_base` 合计，超过 `daily_limit` 时提醒。超过后当天后续每笔支出都会再次提醒。

### 3.4 周消费异常（type=4）

每周日 22:00：

1. 统计本周一至周日的已确认支出；
2. 统计之前四周的平均周支出；
3. 偏离度 = `(本周 - 四周平均) / 四周平均 × 100%`；
4. 高于 `deviation_pct` 发「周消费异常」，低于 `-deviation_pct` 发「周消费偏低」。

四周平均为 0（没有历史消费）时跳过。

### 3.5 信用卡还款提醒（type=5）

每日 09:00 遍历启用的信用卡账户：

- 取账户的 `payment_day`，当月没有该日期时取当月最后一天；
- 如果本月还款日已过，顺延到下月；
- 距还款日 `0 ~ advance_days` 天时提醒（`advance_days` 默认 3，最大 30）。

提醒会在窗口内每天发送一次，直到还款日当天。

### 3.6 周期账单提醒（type=6）

每日 09:00 检查 `next_execute = 今天 + advance_days`（默认 1 天）的启用周期规则，逐条提醒。`next_execute` 由每日 08:00 的周期交易任务推进。

### 3.7 预算未设定提醒（type=7）

每日 09:00 检查当天是否等于规则里的 `check_day`（默认 25 日，范围 1–28）；只有当天匹配时才检查下个月预算是否存在，不存在则提醒。

### 3.8 待确认交易提醒（type=8）

每日 10:00 统计当前用户 `is_confirmed=0` 的交易；数量达到 `min_count`（默认 1）时提醒，消息中列出最多 5 条明细和总金额。

也可以手动触发：`POST /api/v1/system/check-unconfirmed`。

## 4. API

基础路径：`/api/v1/alert-rules`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/alert-rules` | 当前用户规则列表 |
| POST | `/api/v1/alert-rules` | 创建规则 |
| PUT | `/api/v1/alert-rules/{id}` | 更新规则 |
| DELETE | `/api/v1/alert-rules/{id}` | 删除规则 |
| POST | `/api/v1/alert-rules/{id}/toggle` | 启用/停用 |

创建时 `notifyChannel` 为空或省略表示「跟随全局渠道」。规则的 `config` 必须是 JSON 字符串。

预警日志：

- `GET /api/v1/system/alerts`：未读预警
- `PUT /api/v1/system/alerts/{id}/read`：标记已读

## 5. 升级与验证

2026-11-23 之前创建的预置规则渠道写死为 `TELEGRAM`，且缺少 type 8。升级时执行：

```bash
mysql -ubudgetpilot -p budgetpilot < sql/migrations/20261123_alert_rule_builtin_fix.sql
```

脚本可重复执行。验证：

```sql
SELECT user_id, type, name, notify_channel, is_active
FROM t_alert_rule ORDER BY user_id, type;

SELECT alert_type, rule_id, notify_channel, is_sent, COUNT(*)
FROM t_alert_log GROUP BY alert_type, rule_id, notify_channel, is_sent;
```

`is_sent=0` 表示外部推送失败（通常是渠道未配置 token），站内日志仍然可用。

## 6. 已知限制

- 同一类型多条规则时只取 id 最小的一条。
- 预算阈值、日消费上限在持续超限时会重复提醒，尚未实现按天/按周期去重。
- 定时任务依赖进程存活；当天进程未运行则不会补发。
- 预置规则渠道在用户创建时固化，修改全局渠道后需要逐条更新已有规则。