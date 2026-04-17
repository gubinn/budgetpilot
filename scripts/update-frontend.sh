#!/bin/bash
# BudgetPilot 前端更新脚本
# 用途：构建前端并同步到 nginx web 目录和 Spring Boot static 目录

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
FRONTEND_DIR="$PROJECT_DIR/frontend"
WEB_DIR="$PROJECT_DIR/web"
STATIC_DIR="$PROJECT_DIR/src/main/resources/static"

echo "=== BudgetPilot 前端更新 ==="
echo "项目目录: $PROJECT_DIR"
echo ""

# 检查 frontend 目录
if [ ! -d "$FRONTEND_DIR" ]; then
    echo "错误: frontend 目录不存在"
    exit 1
fi

cd "$FRONTEND_DIR"

# 安装依赖（如果需要）
if [ ! -d "node_modules" ]; then
    echo "安装 npm 依赖..."
    npm install
fi

# 构建前端
echo "构建前端..."
npm run build

# 检查构建结果
if [ ! -d "dist" ] || [ ! -f "dist/index.html" ]; then
    echo "错误: 构建失败，dist 目录不存在"
    exit 1
fi

echo "构建完成，dist 目录内容:"
ls -la dist/assets/*.js | head -5

# 同步到 nginx web 目录
echo ""
echo "同步到 nginx web 目录: $WEB_DIR"
mkdir -p "$WEB_DIR"
rm -rf "$WEB_DIR"/*
cp -r dist/* "$WEB_DIR/"
echo "完成，文件数量: $(ls -1 "$WEB_DIR/assets" | wc -l)"

# 同步到 Spring Boot static 目录
echo ""
echo "同步到 Spring Boot static 目录: $STATIC_DIR"
mkdir -p "$STATIC_DIR"
rm -rf "$STATIC_DIR"/*
cp -r dist/* "$STATIC_DIR/"
echo "完成，文件数量: $(ls -1 "$STATIC_DIR/assets" | wc -l)"

# 提示刷新 nginx
echo ""
echo "=== 更新完成 ==="
echo "请执行以下命令刷新 nginx 缓存:"
echo "  sudo nginx -s reload"
echo ""
echo "如果使用 Docker 部署，请执行:"
echo "  cd $PROJECT_DIR && mvn package -DskipTests && docker compose up -d --build"