-- 交易查询性能：租户过滤始终带 user_id，日期/类型是最常见的区间条件。
-- 小数据量下影响不明显，但可避免数据增长后回表扫描。
ALTER TABLE t_transaction ADD INDEX idx_user_date (user_id, transaction_date);
ALTER TABLE t_transaction ADD INDEX idx_user_type_date (user_id, type, transaction_date);