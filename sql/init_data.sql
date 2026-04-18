-- BudgetPilot 初始化数据

USE budgetpilot;

-- ==================== 默认管理员用户 ====================
-- 用户名: admin, 密码: admin123 (BCrypt $2b$)
-- 仅在 t_user 为空时插入
INSERT INTO t_user (username, password, nickname, role, is_active)
SELECT 'admin', '$2b$10$knPyjetWQdi.KOAUBc9aXOnUcYkjxL/qsBwhcKw4VP708nKQQhq1i', '管理员', 'ADMIN', 1
WHERE NOT EXISTS (SELECT 1 FROM t_user);

-- ==================== 系统预设分类 ====================
-- 系统预置分类 user_id=0（通过租户拦截器的 ignoreTable 或特殊处理）

-- 支出大类 (type=1, user_id=0)
INSERT INTO t_category (user_id, parent_id, name, type, icon, color, is_system, sort_order) VALUES
(0, 0, '餐饮', 1, 'food', '#FF6B6B', 1, 1),
(0, 0, '交通', 1, 'transport', '#4ECDC4', 1, 2),
(0, 0, '住房', 1, 'home', '#45B7D1', 1, 3),
(0, 0, '购物', 1, 'shopping', '#96CEB4', 1, 4),
(0, 0, '医疗', 1, 'medical', '#FFEAA7', 1, 5),
(0, 0, '教育', 1, 'education', '#DDA0DD', 1, 6),
(0, 0, '娱乐', 1, 'entertainment', '#98D8C8', 1, 7),
(0, 0, '社交', 1, 'social', '#F7DC6F', 1, 8),
(0, 0, '金融', 1, 'finance', '#BB8FCE', 1, 9),
(0, 0, '其他支出', 1, 'other-expense', '#BDC3C7', 1, 10);

-- 餐饮子类
INSERT INTO t_category (user_id, parent_id, name, type, icon, is_system, sort_order) VALUES
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='餐饮' AND type=1 LIMIT 1) tmp), '外卖', 1, 'takeout', 1, 1),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='餐饮' AND type=1 LIMIT 1) tmp), '聚餐', 1, 'dining', 1, 2),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='餐饮' AND type=1 LIMIT 1) tmp), '食材', 1, 'grocery', 1, 3),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='餐饮' AND type=1 LIMIT 1) tmp), '饮品', 1, 'drink', 1, 4);

-- 交通子类
INSERT INTO t_category (user_id, parent_id, name, type, icon, is_system, sort_order) VALUES
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='交通' AND type=1 LIMIT 1) tmp), '公交地铁', 1, 'bus', 1, 1),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='交通' AND type=1 LIMIT 1) tmp), '打车', 1, 'taxi', 1, 2),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='交通' AND type=1 LIMIT 1) tmp), '加油', 1, 'fuel', 1, 3),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='交通' AND type=1 LIMIT 1) tmp), '停车', 1, 'parking', 1, 4);

-- 住房子类
INSERT INTO t_category (user_id, parent_id, name, type, icon, is_system, sort_order) VALUES
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='住房' AND type=1 LIMIT 1) tmp), '房租/房贷', 1, 'rent', 1, 1),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='住房' AND type=1 LIMIT 1) tmp), '水电燃气', 1, 'utility', 1, 2),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='住房' AND type=1 LIMIT 1) tmp), '物业', 1, 'property', 1, 3),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='住房' AND type=1 LIMIT 1) tmp), '维修', 1, 'repair', 1, 4);

-- 购物子类
INSERT INTO t_category (user_id, parent_id, name, type, icon, is_system, sort_order) VALUES
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='购物' AND type=1 LIMIT 1) tmp), '日用品', 1, 'daily', 1, 1),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='购物' AND type=1 LIMIT 1) tmp), '服饰', 1, 'clothes', 1, 2),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='购物' AND type=1 LIMIT 1) tmp), '数码', 1, 'digital', 1, 3),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='购物' AND type=1 LIMIT 1) tmp), '家具家电', 1, 'furniture', 1, 4);

-- 医疗子类
INSERT INTO t_category (user_id, parent_id, name, type, icon, is_system, sort_order) VALUES
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='医疗' AND type=1 LIMIT 1) tmp), '门诊', 1, 'clinic', 1, 1),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='医疗' AND type=1 LIMIT 1) tmp), '药品', 1, 'medicine', 1, 2),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='医疗' AND type=1 LIMIT 1) tmp), '体检', 1, 'checkup', 1, 3),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='医疗' AND type=1 LIMIT 1) tmp), '保险', 1, 'insurance', 1, 4);

-- 教育子类
INSERT INTO t_category (user_id, parent_id, name, type, icon, is_system, sort_order) VALUES
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='教育' AND type=1 LIMIT 1) tmp), '课程', 1, 'course', 1, 1),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='教育' AND type=1 LIMIT 1) tmp), '书籍', 1, 'book', 1, 2),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='教育' AND type=1 LIMIT 1) tmp), '考试', 1, 'exam', 1, 3);

-- 娱乐子类
INSERT INTO t_category (user_id, parent_id, name, type, icon, is_system, sort_order) VALUES
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='娱乐' AND type=1 LIMIT 1) tmp), '电影', 1, 'movie', 1, 1),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='娱乐' AND type=1 LIMIT 1) tmp), '游戏', 1, 'game', 1, 2),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='娱乐' AND type=1 LIMIT 1) tmp), '旅行', 1, 'travel', 1, 3),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='娱乐' AND type=1 LIMIT 1) tmp), '运动', 1, 'sport', 1, 4);

-- 社交子类
INSERT INTO t_category (user_id, parent_id, name, type, icon, is_system, sort_order) VALUES
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='社交' AND type=1 LIMIT 1) tmp), '礼金', 1, 'gift', 1, 1),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='社交' AND type=1 LIMIT 1) tmp), '聚会', 1, 'party', 1, 2);

-- 金融子类
INSERT INTO t_category (user_id, parent_id, name, type, icon, is_system, sort_order) VALUES
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='金融' AND type=1 LIMIT 1) tmp), '利息', 1, 'interest', 1, 1),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='金融' AND type=1 LIMIT 1) tmp), '手续费', 1, 'fee', 1, 2),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='金融' AND type=1 LIMIT 1) tmp), '投资亏损', 1, 'loss', 1, 3);

-- 收入大类 (type=2, user_id=0)
INSERT INTO t_category (user_id, parent_id, name, type, icon, color, is_system, sort_order) VALUES
(0, 0, '工资', 2, 'salary', '#2ECC71', 1, 1),
(0, 0, '副业', 2, 'sidejob', '#27AE60', 1, 2),
(0, 0, '投资', 2, 'investment', '#1ABC9C', 1, 3),
(0, 0, '报销', 2, 'reimburse', '#16A085', 1, 4),
(0, 0, '其他收入', 2, 'other-income', '#BDC3C7', 1, 5);

-- 工资子类
INSERT INTO t_category (user_id, parent_id, name, type, icon, is_system, sort_order) VALUES
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='工资' AND type=2 LIMIT 1) tmp), '基本工资', 2, 'base-salary', 1, 1),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='工资' AND type=2 LIMIT 1) tmp), '奖金', 2, 'bonus', 1, 2),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='工资' AND type=2 LIMIT 1) tmp), '补贴', 2, 'allowance', 1, 3);

-- 副业子类
INSERT INTO t_category (user_id, parent_id, name, type, icon, is_system, sort_order) VALUES
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='副业' AND type=2 LIMIT 1) tmp), '兼职', 2, 'parttime', 1, 1),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='副业' AND type=2 LIMIT 1) tmp), '稿费', 2, 'article', 1, 2),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='副业' AND type=2 LIMIT 1) tmp), '咨询', 2, 'consult', 1, 3);

-- 投资子类
INSERT INTO t_category (user_id, parent_id, name, type, icon, is_system, sort_order) VALUES
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='投资' AND type=2 LIMIT 1) tmp), '股息', 2, 'dividend', 1, 1),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='投资' AND type=2 LIMIT 1) tmp), '利息', 2, 'interest-income', 1, 2),
(0, (SELECT id FROM (SELECT id FROM t_category WHERE parent_id=0 AND name='投资' AND type=2 LIMIT 1) tmp), '理财收益', 2, 'wealth', 1, 3);

-- 特殊：内部转账
INSERT INTO t_category (user_id, parent_id, name, type, icon, color, is_system, sort_order) VALUES
(0, 0, '内部转账', 1, 'transfer', '#95A5A6', 1, 11);

-- 特殊：余额调整（用于账户余额手动修正时的交易记录）
INSERT INTO t_category (id, user_id, parent_id, name, type, icon, color, is_system, sort_order) VALUES
(58, 0, 0, '余额调整', 2, 'adjust', '#FF9800', 1, 99),  -- 收入类型(type=2)，用于余额调增
(59, 0, 0, '余额调整', 1, 'adjust', '#FF9800', 1, 99);  -- 支出类型(type=1)，用于余额调减

-- ==================== 系统配置 ====================

INSERT INTO t_config (config_key, config_value, description) VALUES
('base_currency', 'CNY', '本位币'),
('supported_currencies', 'CNY,USD,EUR,GBP,JPY,HKD,SGD,THB,KRW', '支持的币种列表'),
('month_start_day', '1', '月度统计起始日'),
('telegram_bot_token', '', 'Telegram Bot Token'),
('telegram_chat_id', '', '接收通知的 Chat ID'),
('exchange_rate_api_key', '', 'ExchangeRate-API Key'),
('backup_cron', '0 3 * * *', '自动备份 cron'),
('data_retention_months', '0', '数据保留月数，0=永久');

-- ==================== 默认预警规则 ====================
-- 注意：以下为全局模板，实际用户通过 UserService.registerDefaults() 获得自己的副本
-- 如果租户拦截器包含 t_alert_rule，以下记录将不可见（user_id=0）
-- 新版本建议在代码中为新用户自动创建以下规则
INSERT INTO t_alert_rule (user_id, name, type, config, notify_channel, is_active) VALUES
(0, '预算阈值预警', 1, '{"threshold_pct": 80}', 'TELEGRAM', 1),
(0, '单笔大额预警', 2, '{"max_amount": 1000}', 'TELEGRAM', 1),
(0, '日消费上限', 3, '{"daily_limit": 500}', 'TELEGRAM', 1),
(0, '周消费异常检测', 4, '{"deviation_pct": 50}', 'TELEGRAM', 1),
(0, '信用卡还款提醒', 5, '{"advance_days": 3}', 'TELEGRAM', 1),
(0, '周期账单提醒', 6, '{"advance_days": 1}', 'TELEGRAM', 1),
(0, '月度预算未设定', 7, '{"check_day": 25}', 'TELEGRAM', 1);
