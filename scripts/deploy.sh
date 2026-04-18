#!/bin/bash
# BudgetPilot 完整部署脚本
# 用途：构建后端和前端，部署到 Docker

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "=== BudgetPilot 完整部署 ==="
echo "项目目录: $PROJECT_DIR"
echo "时间: $(date)"
echo ""

cd "$PROJECT_DIR"

# 1. 构建前端
echo ">>> 步骤 1: 构建前端"
"$SCRIPT_DIR/update-frontend.sh"

cd "$PROJECT_DIR"

# 2. 构建后端 JAR
echo ""
echo ">>> 步骤 2: 构建后端 JAR"
echo "当前目录: $(pwd)"
mvn clean package -DskipTests -q

JAR_FILE=$(ls target/budgetpilot-*.jar 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "错误: JAR 文件不存在 (target/budgetpilot-*.jar)"
    ls -la target/ 2>/dev/null || echo "target 目录不存在"
    exit 1
fi
echo "JAR 文件: $JAR_FILE ($(du -h "$JAR_FILE" | cut -f1))"

# 3. 重新构建 Docker 镜像
echo ""
echo ">>> 步骤 3: 重新构建 Docker 镜像"
docker compose build --no-cache budgetpilot-api

# 4. 启动服务
echo ""
echo ">>> 步骤 4: 启动 Docker 服务"
docker compose up -d

# 5. 等待服务启动
echo ""
echo ">>> 步骤 5: 等待服务启动..."
sleep 10

# 6. 验证部署
echo ""
echo ">>> 步骤 6: 验证部署"

# 检查容器状态
echo "容器状态:"
docker compose ps

# 检查 API
echo ""
echo "API 测试:"
if curl -s "http://127.0.0.1:6060/api/v1/accounts" | jq -e '.code == 0' > /dev/null 2>&1; then
    echo "  API 正常: /api/v1/accounts 返回 code=0"
else
    echo "  API 异常，请检查日志"
    docker compose logs budgetpilot-api --tail 50
    exit 1
fi

# 清理 Redis 缓存
echo ""
echo "清理 Redis 缓存..."
docker exec $(docker compose ps -q redis) redis-cli FLUSHALL > /dev/null 2>&1 || echo "  Redis 缓存已清理"

echo ""
echo "=== 部署完成 ==="
echo "API 地址: http://127.0.0.1:6060"
echo "请刷新 nginx: sudo nginx -s reload"