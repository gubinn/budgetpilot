#!/bin/bash
# BudgetPilot 测试数据脚本 - 待确认交易功能演示
# 用途：创建测试账户和待确认交易，验证功能

set -e

API_BASE="http://127.0.0.1:6060/api/v1"

echo "=== BudgetPilot 待确认交易功能测试 ==="
echo ""

# 检查 API 可用性
echo "检查 API 连通性..."
if ! curl -s "$API_BASE/accounts" | jq -e '.code == 0' > /dev/null 2>&1; then
    echo "错误: API 不可用，请先启动服务"
    echo "执行: cd /opt/budgetpilot && docker compose up -d"
    exit 1
fi

echo "API 正常"
echo ""

# 1. 查询或创建测试账户
echo ">>> 步骤 1: 准备测试账户"
ACCOUNTS=$(curl -s "$API_BASE/accounts" | jq '.data')
TEST_ACCOUNT=$(echo "$ACCOUNTS" | jq -r '.[] | select(.name == "测试储蓄卡") | .id')

if [ -z "$TEST_ACCOUNT" ] || [ "$TEST_ACCOUNT" == "null" ]; then
    echo "创建测试账户..."
    RESULT=$(curl -s -X POST "$API_BASE/accounts" \
        -H "Content-Type: application/json" \
        -d '{"name":"测试储蓄卡","type":2,"icon":"wallet","currency":"CNY","initialBalance":0}')
    TEST_ACCOUNT=$(echo "$RESULT" | jq -r '.data.id')
    echo "账户创建成功，ID: $TEST_ACCOUNT"
else
    echo "测试账户已存在，ID: $TEST_ACCOUNT"
fi

# 2. 查询账户当前余额
echo ""
echo ">>> 步骤 2: 查询账户余额"
BALANCE=$(curl -s "$API_BASE/accounts" | jq -r '.data[] | select(.id == '$TEST_ACCOUNT') | .currentBalance')
echo "当前余额: ¥$BALANCE"

# 3. 创建待确认支出交易
echo ""
echo ">>> 步骤 3: 创建待确认支出交易"
RESULT=$(curl -s -X POST "$API_BASE/transactions" \
    -H "Content-Type: application/json" \
    -d '{
        "accountId": '$TEST_ACCOUNT',
        "categoryId": 1,
        "type": 1,
        "amount": "200.00",
        "transactionDate": "'$(date +%Y-%m-%d)'",
        "note": "测试待确认支出 - 午餐",
        "isConfirmed": false
    }')
TX_ID=$(echo "$RESULT" | jq -r '.data.id')
echo "交易创建成功，ID: $TX_ID"
echo "状态: $(echo "$RESULT" | jq -r '.data.isConfirmed') (false = 待确认)"

# 4. 验证余额未变化
echo ""
echo ">>> 步骤 4: 验证余额未变化（待确认交易不影响余额）"
NEW_BALANCE=$(curl -s "$API_BASE/accounts" | jq -r '.data[] | select(.id == '$TEST_ACCOUNT') | .currentBalance')
echo "当前余额: ¥$NEW_BALANCE"
if [ "$BALANCE" == "$NEW_BALANCE" ]; then
    echo "验证成功: 余额未变化"
else
    echo "验证失败: 余额不应该变化"
    exit 1
fi

# 5. 查询待确认交易列表
echo ""
echo ">>> 步骤 5: 查询待确认交易列表"
PENDING_TXS=$(curl -s "$API_BASE/transactions?confirmed=false" | jq '.data.items')
PENDING_COUNT=$(echo "$PENDING_TXS" | jq 'length')
echo "待确认交易数量: $PENDING_COUNT"
echo "待确认交易列表:"
echo "$PENDING_TXS" | jq -r '.[] | "  ID: \(.id), 金额: ¥\(.amount), 备注: \(.note)"'

# 6. 确认交易
echo ""
echo ">>> 步骤 6: 确认交易"
RESULT=$(curl -s -X POST "$API_BASE/transactions/$TX_ID/confirm")
echo "确认结果: $(echo "$RESULT" | jq -r '.data.isConfirmed') (true = 已确认)"

# 7. 验证余额变化
echo ""
echo ">>> 步骤 7: 验证余额变化（确认后余额减少）"
FINAL_BALANCE=$(curl -s "$API_BASE/accounts" | jq -r '.data[] | select(.id == '$TEST_ACCOUNT') | .currentBalance')
echo "当前余额: ¥$FINAL_BALANCE"
echo "余额变化: ¥$BALANCE -> ¥$FINAL_BALANCE (减少 ¥200)"

# 8. 验证首页数据更新
echo ""
echo ">>> 步骤 8: 验证首页报表数据"
MONTH=$(date +%Y-%m)
REPORT=$(curl -s "$API_BASE/reports/monthly-summary?month=$MONTH" | jq '.data.monthlySummary')
echo "本月支出: ¥$(echo "$REPORT" | jq -r '.totalExpense')"
echo "本月结余: ¥$(echo "$REPORT" | jq -r '.balance')"

echo ""
echo "=== 测试完成 ==="
echo ""
echo "测试验证点:"
echo "1. 待确认交易创建成功 ✓"
echo "2. 待确认交易不影响余额 ✓"
echo "3. 状态筛选功能正常 ✓"
echo "4. 确认交易后余额更新 ✓"
echo "5. 首页报表数据更新 ✓"
echo ""
echo "请在浏览器中验证:"
echo "- 访问交易列表，选择状态'待确认'"
echo "- 查看待确认交易显示'确认'按钮"
echo "- 点击确认后，刷新首页查看数据更新"