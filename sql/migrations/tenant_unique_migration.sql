-- ============================================
-- BudgetPilot 商户/账户租户唯一约束迁移脚本
--
-- 用途：为已有数据的 t_merchant 和 t_account 添加 user_id 租户隔离
-- 新部署环境直接使用 schema.sql 即可
-- ============================================

USE budgetpilot;

-- --------------------------------------------
-- 1. 商户表：添加 user_id 并修复唯一约束
-- --------------------------------------------

-- 1.1 为已有商户添加 user_id 列
ALTER TABLE t_merchant
    ADD COLUMN IF NOT EXISTS user_id BIGINT NOT NULL DEFAULT 0 COMMENT '所属用户ID' AFTER id;

-- 1.2 从关联交易中回填商户的 user_id
UPDATE t_merchant m
INNER JOIN (
    SELECT merchant_id, MIN(user_id) AS user_id
    FROM t_transaction
    WHERE merchant_id IS NOT NULL
    GROUP BY merchant_id
) t ON m.id = t.merchant_id
SET m.user_id = t.user_id
WHERE m.user_id = 0;

-- 1.3 仍无关联交易的商户（系统预设/孤立数据），分配给第一个用户
SET @default_user_id = (SELECT MIN(id) FROM t_user);
UPDATE t_merchant SET user_id = @default_user_id WHERE user_id = 0 AND @default_user_id IS NOT NULL;

-- 1.4 删除旧的仅 name 的唯一索引
ALTER TABLE t_merchant DROP INDEX uk_name;

-- 1.5 添加 (user_id, name) 联合唯一索引
ALTER TABLE t_merchant ADD UNIQUE INDEX uk_user_name (user_id, name);


-- --------------------------------------------
-- 2. 账户表：修复唯一约束（user_id 已存在）
-- --------------------------------------------

-- 2.1 删除旧的仅 name 的普通索引
ALTER TABLE t_account DROP INDEX uk_name;

-- 2.2 添加 (user_id, name) 联合唯一索引
ALTER TABLE t_account ADD UNIQUE INDEX uk_user_name (user_id, name);
