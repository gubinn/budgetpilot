# BudgetPilot

个人预算 / 记账系统。后端 Spring Boot 3.3 + MyBatis-Plus + Sa-Token + MySQL 8 + Redis 7，前端 Vue 3 + Naive UI + ECharts（桌面 + 移动端 PWA）。

往来款：从「借出款 / 收回借出款 / 借入款 / 归还借入款」四类交易实时聚合应收、应付和账龄，账户页底部有一条低干扰的应收款入口，独立页面可按对方查看明细；不新增台账表，也不影响收支报表和预算。

卡券包：把单位发的购物卡、电影票、储值余额记成一本表外备忘，按金额或次数记账，记录所在小程序/账号和有效期；到期前、到期当天和过期次日提醒，不计入总资产、预算和收支报表。

年度复盘：报表里的年度视角，汇总全年收支、储蓄率、净资产变化、应急金、固定支出占比、年度/月度预算执行，以及分类、收入来源、商户和年度之最；当前年按已过月份计算，同比自动对比去年同期，去年无数据时不显示误导性同比。

只读共享：生成带有效期和 PIN 的链接，家人无需注册即可只读查看账本；只读令牌由服务端强制拒绝写请求，可随时撤销，不共享密码。同一浏览器多个标签页会同步登录/只读状态。

转账复核：待复核的转账可以选择「自己转自己（账户互转，不计收支）」，保留转出/转入账户并清空导入时误挂的往来款分类；转账编辑页的分类为可选项，显示全部启用分类。

## 文档索引

| 文档 | 说明 |
|------|------|
| [CHANGELOG.md](CHANGELOG.md) | 变更记录、升级顺序、运维事实、踩坑经验 |
| [API.md](API.md) | 内部 API 文档 |
| [REPORTING.md](REPORTING.md) | 报表口径、聚合接口与净资产月快照 |
| [LOGGING.md](LOGGING.md) | 请求访问日志、认证/权限审计、财务写操作日志与隐私边界 |
| [ALERT_RULES.md](ALERT_RULES.md) | 预警规则类型、触发时机、配置口径与通知渠道 |
| [RECURRING_RULES.md](RECURRING_RULES.md) | 周期交易的日期计算、自动执行、幂等与首次使用建议 |
| [API_EXTERNAL.md](API_EXTERNAL.md) | 面向外部系统的 API 文档（API Key 鉴权、幂等） |
| [DEPLOY.md](DEPLOY.md) | Docker / 裸机部署、数据库迁移 |
| [budgetpilot-design-v2-final.md](budgetpilot-design-v2-final.md) | 系统设计文档 |
| [TEST_REPORT.md](TEST_REPORT.md) | 历史测试报告（2026-04，仅供参考） |

## 本机运行（2026-11-21 状态）

- API：`http://<host>:6060`，前端静态文件已打包进 jar，直接访问根路径即可登录。
- 用户级服务：`systemctl --user status budgetpilot.service`
  - unit：`~/.config/systemd/user/budgetpilot.service`
  - 环境变量：`~/.config/budgetpilot.env`（600，含数据库连接，不入库）
- 依赖服务：`systemctl status mysql redis-server`（已 enabled）。
- 运行日志：`journalctl --user -u budgetpilot.service -f`；日志口径与隐私边界见 [LOGGING.md](LOGGING.md)。
- 配套采集端：`~/accounting_buddy`（招行邮件记账，用户级 timer 每 5 分钟）。

## 构建

```bash
# 后端
mvn clean package -DskipTests

# 前端（如需更新静态页面）
cd frontend && npm ci && npm run build
rm -rf ../src/main/resources/static && mkdir -p ../src/main/resources/static
cp -r dist/* ../src/main/resources/static/
cd .. && mvn -DskipTests package
systemctl --user restart budgetpilot.service
```

## 数据库升级

新增字段/索引时，先备份，再执行 `sql/migrations/*.sql`，最后部署新代码。详见 `DEPLOY.md` 和 `CHANGELOG.md`。

## 测试

```bash
mvn -q -Dtest=ExternalIdIdempotencyTest test
```

> 仓库原有测试套件在 HEAD 基线即存在 `@InjectMocks` 无法注入 MyBatis-Plus `baseMapper` 的问题，会大量失败；新增测试已显式注入，详见 CHANGELOG。