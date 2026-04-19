# 数据备份

备份 gubin 用户下的账户（t_account）和交易（t_transaction）数据。

## 使用方法

### 本机运行（需安装 MySQL 客户端）

```bash
# 备份
bash bakdata/backup.sh

# 恢复（默认恢复最新备份）
bash bakdata/restore.sh

# 恢复指定备份
bash bakdata/restore.sh bakdata/backup_20250101_120000.sql.gz
```

### Docker 容器内运行

```bash
# 备份
docker exec budgetpilot-app bash /app/bakdata/backup.sh

# 恢复
docker exec -it budgetpilot-app bash /app/bakdata/restore.sh
```

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_HOST` | `127.0.0.1` | 数据库地址 |
| `DB_PORT` | `3306` | 数据库端口 |
| `DB_USER` | `root` | 数据库用户 |
| `DB_PASS` | `root` | 数据库密码 |

备份文件会保留 30 天，旧备份自动清理。
