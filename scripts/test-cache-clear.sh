#!/bin/bash
# BudgetPilot 缓存功能测试脚本
# 用途：验证数据变更后缓存是否正确清除

set -e

API_BASE="http://127.0.0.1:6060/api/v1"
REDIS_CONTAINER="budgetpilot-redis-1"

echo "=== BudgetPilot 缓存功能测试 ==="
echo "API 地址: $API_BASE"
echo "时间: $(date)"
echo ""

# 检查 Redis 是否运行
if ! docker ps --format '{{.Names}}' | grep -q "redis"; then
    echo "错误: Redis 容器未运行"
    exit 1
fi

# 清空缓存开始测试
echo ">>> 清空 Redis 缓存..."
docker exec $REDIS_CONTAINER redis-cli FLUSHALL
echo "缓存已清空"
echo ""

# ============ 测试1: 交易变更清除报表缓存 ============
echo ">>> 测试1: 交易变更清除报表缓存"

# 1.1 创建账户
echo "  1.1 创建测试账户..."
ACCOUNT_ID=$(curl -s -X POST "$API_BASE/accounts" \
    -H "Content-Type: application/json" \
    -d '{"name":"缓存测试账户","type":2,"currency":"CNY"}' | jq -r '.data.id')
echo "  账户ID: $ACCOUNT_ID"

# 1.2 查询报表，触发缓存写入
echo "  1.2 查询月度报表（写入缓存）..."
curl -s "$API_BASE/reports/monthly-summary/2026-04" > /dev/null

# 检查缓存是否存在
CACHE_KEY="report:monthly-summary:2026-04"
CACHE_EXISTS=$(docker exec $REDIS_CONTAINER redis-cli EXISTS "$CACHE_KEY")
echo "  缓存状态: $CACHE_KEY exists=$CACHE_EXISTS"

if [ "$CACHE_EXISTS" != "1" ]; then
    echo "  错误: 缓存未写入"
    exit 1
fi

# 1.3 创建交易
echo "  1.3 创建交易..."
TX_ID=$(curl -s -X POST "$API_BASE/transactions" \
    -H "Content-Type: application/json" \
    -d '{"type":1,"amount":100,"currency":"CNY","accountId":$ACCOUNT_ID,"categoryId":1,"transactionDate":"2026-04-15","isConfirmed":true}' | jq -r '.data.id')
echo "  交易ID: $TX_ID"

# 等待异步事件处理
sleep 2

# 1.4 验证缓存已清除
CACHE_EXISTS=$(docker exec $REDIS_CONTAINER redis-cli EXISTS "$CACHE_KEY")
echo "  缓存状态: $CACHE_KEY exists=$CACHE_EXISTS"

if [ "$CACHE_EXISTS" != "0" ]; then
    echo "  ❌ 测试失败: 交易创建后缓存未清除"
    exit 1
fi
echo "  ✅ 测试通过: 交易创建后缓存已清除"
echo ""

# ============ 测试2: 账户更新清除报表缓存 ============
echo ">>> 测试2: 账户更新清除报表缓存"

# 2.1 再次查询报表，写入缓存
curl -s "$API_BASE/reports/monthly-summary/2026-04" > /dev/null
CACHE_EXISTS=$(docker exec $REDIS_CONTAINER redis-cli EXISTS "$CACHE_KEY")
echo "  缓存状态: $CACHE_KEY exists=$CACHE_EXISTS"

# 2.2 更新账户名称
echo "  2.2 更新账户名称..."
curl -s -X PUT "$API_BASE/accounts/$ACCOUNT_ID" \
    -H "Content-Type: application/json" \
    -d '{"name":"缓存测试账户-已修改"}' | jq -r '.code'

sleep 1

# 2.3 验证缓存已清除
CACHE_EXISTS=$(docker exec $REDIS_CONTAINER redis-cli EXISTS "$CACHE_KEY")
echo "  缓存状态: $CACHE_KEY exists=$CACHE_EXISTS"

if [ "$CACHE_EXISTS" != "0" ]; then
    echo "  ❌ 测试失败: 账户更新后缓存未清除"
    exit 1
fi
echo "  ✅ 测试通过: 账户更新后缓存已清除"
echo ""

# ============ 测试3: 分类更新清除报表缓存 ============
echo ">>> 测试3: 分类更新清除报表缓存"

# 3.1 查询报表，写入缓存
curl -s "$API_BASE/reports/monthly-summary/2026-04" > /dev/null
CACHE_EXISTS=$(docker exec $REDIS_CONTAINER redis-cli EXISTS "$CACHE_KEY")
echo "  缓存状态: $CACHE_KEY exists=$CACHE_EXISTS"

# 3.2 更新分类颜色（假设分类ID=1存在）
echo "  3.2 更新分类..."
curl -s -X PUT "$API_BASE/categories/1" \
    -H "Content-Type: application/json" \
    -d '{"color":"#ff0000"}' | jq -r '.code'

sleep 1

# 3.3 验证缓存已清除
CACHE_EXISTS=$(docker exec $REDIS_CONTAINER redis-cli EXISTS "$CACHE_KEY")
echo "  缓存状态: $CACHE_KEY exists=$CACHE_EXISTS"

if [ "$CACHE_EXISTS" != "0" ]; then
    echo "  ❌ 测试失败: 分类更新后缓存未清除"
    exit 1
fi
echo "  ✅ 测试通过: 分类更新后缓存已清除"
echo ""

# ============ 测试4: 删除交易清除缓存 ============
echo ">>> 测试4: 删除交易清除缓存"

# 4.1 查询报表，写入缓存
curl -s "$API_BASE/reports/monthly-summary/2026-04" > /dev/null
CACHE_EXISTS=$(docker exec $REDIS_CONTAINER redis-cli EXISTS "$CACHE_KEY")
echo "  缓存状态: $CACHE_KEY exists=$CACHE_EXISTS"

# 4.2 删除交易
echo "  4.2 删除交易..."
curl -s -X DELETE "$API_BASE/transactions/$TX_ID" | jq -r '.code'

sleep 2

# 4.3 验证缓存已清除
CACHE_EXISTS=$(docker exec $REDIS_CONTAINER redis-cli EXISTS "$CACHE_KEY")
echo "  缓存状态: $CACHE_KEY exists=$CACHE_EXISTS"

if [ "$CACHE_EXISTS" != "0" ]; then
    echo "  ❌ 测试失败: 交易删除后缓存未清除"
    exit 1
fi
echo "  ✅ 测试通过: 交易删除后缓存已清除"
echo ""

# ============ 清理测试数据 ============
echo ">>> 清理测试数据..."
curl -s -X DELETE "$API_BASE/accounts/$ACCOUNT_ID" > /dev/null
echo "测试账户已删除"
echo ""

echo "=== 缓存功能测试完成 ==="
echo ""
echo "测试结果汇总:"
echo "  ✅ 测试1: 交易创建清除缓存"
echo "  ✅ 测试2: 账户更新清除缓存"
echo "  ✅ 测试3: 分类更新清除缓存"
echo "  ✅ 测试4: 交易删除清除缓存"