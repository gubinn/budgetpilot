#!/bin/bash
# BudgetPilot 数据备份脚本
# 用途：备份 gubin 用户的账户和交易数据
# 用法：bash bakdata/backup.sh

set -e

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-root}"
DB_NAME="budgetpilot"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_DIR="${SCRIPT_DIR}"
BACKUP_FILE="${BACKUP_DIR}/backup_${TIMESTAMP}.sql"

echo "=========================================="
echo "  BudgetPilot 数据备份"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="

# 检查 mysql 或 mysqldump 是否可用
if command -v mysqldump &> /dev/null; then
    DUMP_CMD="mysqldump"
elif command -v mysql &> /dev/null; then
    DUMP_CMD="mysql"
else
    echo "错误：未找到 mysql 或 mysqldump 命令"
    echo "请安装 MySQL 客户端，或在 Docker 容器中运行此脚本："
    echo "  docker exec budgetpilot-app bash /app/bakdata/backup.sh"
    exit 1
fi

echo "数据库: ${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME}"
echo "备份文件: ${BACKUP_FILE}"
echo "------------------------------------------"

# 使用 mysqldump 导出指定表
mysqldump \
    -h "${DB_HOST}" \
    -P "${DB_PORT}" \
    -u "${DB_USER}" \
    -p"${DB_PASS}" \
    --no-create-db \
    --routines \
    --triggers \
    --single-transaction \
    "${DB_NAME}" \
    t_account \
    t_transaction \
    > "${BACKUP_FILE}"

# 压缩备份文件
gzip -f "${BACKUP_FILE}"
BACKUP_FILE="${BACKUP_FILE}.gz"

SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)

echo "------------------------------------------"
echo "备份完成: ${BACKUP_FILE} (${SIZE})"
echo "=========================================="

# 清理 30 天前的备份
find "${BACKUP_DIR}" -name "backup_*.sql.gz" -mtime +30 -delete 2>/dev/null
echo "已清理 30 天前的旧备份"
