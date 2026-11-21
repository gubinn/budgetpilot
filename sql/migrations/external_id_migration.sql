-- ============================================
-- 交易外部幂等 ID 迁移脚本
--
-- 用途：为已有数据库增加 external_id 字段，支持外部系统
--       （如邮件记账）以稳定 ID 幂等创建交易。
-- 新部署环境直接使用 01_schema.sql 即可。
--
-- 注意：本脚本只需执行一次；重复执行会因字段/索引已存在而报错。
-- ============================================

USE budgetpilot;

ALTER TABLE t_transaction
    ADD COLUMN external_id VARCHAR(100) DEFAULT NULL COMMENT '外部系统幂等ID（同一用户下唯一）' AFTER user_id,
    ADD UNIQUE INDEX uk_user_external (user_id, external_id);

-- 校验：
-- SHOW CREATE TABLE t_transaction;