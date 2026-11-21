-- 2026-11-21 分类预算属性 + 交易人工复核状态
-- 先执行本脚本，再部署新代码。

SET @db = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_category' AND COLUMN_NAME = 'budget_type') = 0,
    'ALTER TABLE t_category ADD COLUMN budget_type VARCHAR(16) NOT NULL DEFAULT ''DAILY'' COMMENT ''DAILY-月度日常 FIXED-固定不预算 SPECIAL-年度专项 NONE-非预算流水'' AFTER is_active',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_category' AND COLUMN_NAME = 'exclude_from_reports') = 0,
    'ALTER TABLE t_category ADD COLUMN exclude_from_reports TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1=不计入收支报表'' AFTER budget_type',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_transaction' AND COLUMN_NAME = 'review_status') = 0,
    'ALTER TABLE t_transaction ADD COLUMN review_status VARCHAR(16) NOT NULL DEFAULT ''REVIEWED'' COMMENT ''PENDING-待人工复核 REVIEWED-已复核'' AFTER is_confirmed',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_transaction' AND INDEX_NAME = 'idx_review_status') = 0,
    'ALTER TABLE t_transaction ADD INDEX idx_review_status (review_status)',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 收入分类默认不参与预算；支出先统一按日常，后续按分类单独调整。
UPDATE t_category SET budget_type = 'NONE' WHERE type = 2;
UPDATE t_category SET budget_type = 'NONE'
WHERE type = 1 AND name IN ('余额调整支出');

-- 复核状态不与入账确认 is_confirmed 共用“CONFIRMED”命名
UPDATE t_transaction SET review_status = 'REVIEWED' WHERE review_status = 'CONFIRMED';
ALTER TABLE t_transaction MODIFY COLUMN review_status VARCHAR(16) NOT NULL DEFAULT 'REVIEWED'
    COMMENT 'PENDING-待人工复核 REVIEWED-已复核';

-- 交易类型注释更新（仅元数据）
ALTER TABLE t_transaction MODIFY COLUMN type TINYINT NOT NULL COMMENT '1-支出 2-收入 3-转账 4-退款';