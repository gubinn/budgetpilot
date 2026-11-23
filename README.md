# BudgetPilot

个人预算 / 记账系统。后端 Spring Boot 3.3 + MyBatis-Plus + Sa-Token + MySQL 8 + Redis 7，前端 Vue 3 + Naive UI + ECharts（桌面 + 移动端 PWA）。

## 文档索引

| 文档 | 说明 |
|------|------|
| [CHANGELOG.md](CHANGELOG.md) | 变更记录、升级顺序、运维事实、踩坑经验 |
| [API.md](API.md) | 内部 API 文档 |
| [REPORTING.md](REPORTING.md) | 报表口径、聚合接口与净资产月快照 |
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