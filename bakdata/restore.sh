#!/bin/bash
# BudgetPilot 数据恢复脚本
# 用途：从备份文件恢复 gubin 用户的账户和交易数据
# 用法：bash bakdata/restore.sh [backup_file]

set -e

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-root}"
DB_NAME="budgetpilot"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=========================================="
echo "  BudgetPilot 数据恢复"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="

# 确定备份文件
if [ -n "$1" ]; then
    BACKUP_FILE="$1"
else
    # 找最新的备份文件
    BACKUP_FILE=$(ls -t "${SCRIPT_DIR}"/backup_*.sql.gz 2>/dev/null | head -1)
fi

if [ -z "${BACKUP_FILE}" ] || [ ! -f "${BACKUP_FILE}" ]; then
    echo "错误：未找到备份文件"
    echo "用法: bash ${0} [backup_file.sql.gz]"
    exit 1
fi

echo "备份文件: ${BACKUP_FILE}"
echo "数据库:   ${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME}"
echo "------------------------------------------"

# 确认恢复
read -p "此操作将覆盖当前账户和交易数据，是否继续？[y/N] " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "已取消"
    exit 0
fi

echo "开始恢复..."

# 解压并恢复
zcat "${BACKUP_FILE}" | mysql \
    -h "${DB_HOST}" \
    -P "${DB_PORT}" \
    -u "${DB_USER}" \
    -p"${DB_PASS}" \
    "${DB_NAME}"

echo "------------------------------------------"
echo "恢复完成"
echo "=========================================="
