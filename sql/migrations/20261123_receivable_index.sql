-- 往来款汇总：按用户 + 分类 + 日期读取「借出款/收回借出款/借入款/归还借入款」
-- 只扫描这四个分类，不扫全量流水。复合索引避免大表回表扫描。
-- 幂等：已存在同名索引时跳过。

SET @db := DATABASE();
SET @idx_count := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = @db
      AND table_name = 't_transaction'
      AND index_name = 'idx_user_category_date'
);
SET @sql := IF(
    @idx_count = 0,
    'ALTER TABLE t_transaction ADD INDEX idx_user_category_date (user_id, category_id, transaction_date)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;