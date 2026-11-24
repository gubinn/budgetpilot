# 日志与审计

本文描述 BudgetPilot 的运行日志口径。实现位置：

- `src/main/java/uk/gubin/budgetpilot/filter/ApiRequestLoggingFilter.java`
- `src/main/java/uk/gubin/budgetpilot/filter/IngressFilter.java`
- `src/main/java/uk/gubin/budgetpilot/filter/ApiKeyFilter.java`
- `src/main/java/uk/gubin/budgetpilot/filter/ReadonlyFilter.java`
- `src/main/java/uk/gubin/budgetpilot/common/GlobalExceptionHandler.java`
- 各 `*ServiceImpl` 的业务审计日志

## 1. 请求访问日志

`ApiRequestLoggingFilter` 对 `/api/**` 请求统一记录：

| 字段 | 说明 |
|---|---|
| 方法、路径 | 只记录 URI，不记录 query string |
| 状态码、耗时 | 响应状态和毫秒耗时 |
| `user` | 当前登录用户 ID；匿名请求为 `-` |
| `share` | 只读分享 ID；普通会话为 `-` |
| `ingress` | `TAILSCALE` / `CLOUDFLARE` / `LAN` / `LOOPBACK` / `UNKNOWN` |

级别：

- 非 GET/HEAD/OPTIONS 写请求成功：`INFO`
- 正常 GET：`DEBUG`
- HTTP 4xx、耗时 ≥3 秒的慢请求：`WARN`
- HTTP 5xx、过滤器/控制器未处理异常：`ERROR`

示例：

```text
API PUT /api/v1/transactions/123 -> 200 (45 ms, user=2, share=-, ingress=CLOUDFLARE)
Readonly session rejected write: shareId=42 PUT /api/v1/transactions/123
Blocked request from untrusted ingress: POST /api/v1/auth/login clientIp=... peer=...
```

## 2. 认证与权限审计

- 登录：成功、失败原因（用户不存在/停用/密码错误/锁定）、会话创建、退出、改密。
- OTP：发送、重发、发送失败、校验成功、验证码错误、次数耗尽、用户失效。
- API Key：生成、查看、无效 Key 拒绝、公网 API Key 拒绝。
- 只读分享：创建、撤销、验证成功/失败（链接无效、缺 PIN、PIN 错误、尝试过多）。
- 只读写保护：拒绝写请求、已撤销会话、Redis 检查失败。
- 来源识别：未知来源拒绝。

登录失败、OTP 校验失败、分享 PIN 失败、只读写拒绝、来源拒绝均为 `WARN`，便于检索异常访问。

## 3. 财务写操作审计

以下操作至少记录对象 ID 和结果，关键金额/状态一并记录：

- 交易：创建、更新（含类型/金额/分类/账户）、确认、删除、批量改分类、批量标注。
- 账户：创建、更新、停用、余额调整。
- 分类：创建、更新、停用、合并。
- 预算：月度预算创建/更新/复制/锁定，年度预算创建/更新。
- 周期规则：创建、更新、启停、删除、手动执行、自动生成交易。
- 卡券包：创建、更新、删除、使用、充值、作废/恢复。
- 对账：导入批次、删除批次、匹配/取消匹配/忽略/补录/重新匹配。
- 系统：配置修改（只记录键名）、清空测试数据（`WARN`）、通知测试、手动触发待确认检查。
- 账户快照：写入的用户、账户数和月份数。

## 4. 异常日志

`GlobalExceptionHandler` 覆盖：

- 业务异常：`WARN`（预算未设置降为 `DEBUG`）
- 参数校验失败、参数绑定失败、JSON 请求体格式错误、参数类型不匹配：`WARN`
- 数据库访问异常、SQL 异常、未预期异常：`ERROR` + 堆栈
- 未登录、角色不足：`WARN`

## 5. 隐私边界

日志中**不记录**：

- 密码、OTP 验证码、Sa-Token 令牌、只读分享令牌
- API Key、Telegram Bot Token、WhatsApp API Key、系统/用户配置值
- 请求体、Authorization 请求头、查询参数（避免搜索词、备注、账号等进入日志）
- 银行原始账单内容

允许记录：用户 ID、分享 ID、账户/分类/交易 ID、金额、状态码、IP、来源类型、操作结果。

## 6. 查询方式

本机 systemd 用户服务：

```bash
# 实时
journalctl --user -u budgetpilot.service -f

# 最近 30 分钟
journalctl --user -u budgetpilot.service --since "30 min ago"

# 只看错误和拒绝
journalctl --user -u budgetpilot.service --since "1 hour ago" \
  | grep -E "ERROR|WARN|rejected|failed"

# 只看某个用户的请求日志
journalctl --user -u budgetpilot.service --since today | grep "user=2"
```

Docker / 裸机部署的日志查看方式见 `DEPLOY.md` 6.1。journald 的保留大小可用 `journalctl --vacuum-size=100M` 控制。

## 7. 新增日志的约定

1. 使用 SLF4J 参数化日志，不拼字符串。
2. 写操作记录对象 ID 和结果；权限/认证失败记录原因，但不记录凭据。
3. 正常高频读取用 `DEBUG`，写操作用 `INFO`，可恢复的异常用 `WARN`，需要人工处理的异常用 `ERROR`。
4. 不记录密码、令牌、API Key、配置值、请求体和查询参数。
5. 新增过滤器/拦截器时，确保拒绝路径有 `WARN`，否则请求不会进入控制器，业务日志里看不到原因。