-- ============================================
-- BudgetPilot 商户功能数据库迁移脚本
--
-- 注意：此脚本仅用于已有数据的迁移
-- 新部署环境请使用 schema.sql（已包含 t_merchant 表）
-- ============================================

USE budgetpilot;

-- --------------------------------------------
-- 1. 创建商户表 t_merchant
-- --------------------------------------------
CREATE TABLE IF NOT EXISTS t_merchant (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL COMMENT '商户名称',
    alias           VARCHAR(200)    DEFAULT NULL COMMENT '别名（用于模糊匹配）',
    category_id     BIGINT          DEFAULT NULL COMMENT '关联分类ID',
    icon            VARCHAR(50)     DEFAULT NULL COMMENT '图标标识',
    color           CHAR(7)         DEFAULT NULL COMMENT '#RRGGBB',
    description     VARCHAR(200)    DEFAULT NULL COMMENT '商户描述',
    tags            JSON            DEFAULT NULL COMMENT '标签数组 ["餐饮","连锁"]',
    usage_count     INT             DEFAULT 0 COMMENT '使用次数（用于排序推荐）',
    last_used_at    DATE            DEFAULT NULL COMMENT '最近使用日期',
    is_active       TINYINT(1)      DEFAULT 1 COMMENT '是否启用',
    is_system       TINYINT(1)      DEFAULT 0 COMMENT '系统预设商户',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_name (name),
    INDEX idx_category (category_id),
    INDEX idx_usage (usage_count DESC),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户表';

-- --------------------------------------------
-- 2. 为交易表新增 merchant_id 字段
-- --------------------------------------------
ALTER TABLE t_transaction
    ADD COLUMN merchant_id BIGINT DEFAULT NULL COMMENT '商户ID' AFTER category_id,
    ADD INDEX idx_merchant (merchant_id);

-- --------------------------------------------
-- 3. 数据迁移：从 ext_fields 提取商户名
-- --------------------------------------------

-- 第一步：提取现有商户名并插入商户表
INSERT INTO t_merchant (name, usage_count, last_used_at, is_active)
SELECT
    JSON_UNQUOTE(JSON_EXTRACT(ext_fields, '$.merchant')) AS name,
    COUNT(*) AS usage_count,
    MAX(transaction_date) AS last_used_at,
    1 AS is_active
FROM t_transaction
WHERE ext_fields IS NOT NULL
    AND JSON_TYPE(JSON_EXTRACT(ext_fields, '$.merchant')) IS NOT NULL
    AND JSON_UNQUOTE(JSON_EXTRACT(ext_fields, '$.merchant')) != ''
GROUP BY JSON_UNQUOTE(JSON_EXTRACT(ext_fields, '$.merchant'))
ON DUPLICATE KEY UPDATE
    usage_count = VALUES(usage_count),
    last_used_at = VALUES(last_used_at);

-- 第二步：更新交易的 merchant_id 关联
UPDATE t_transaction t
JOIN t_merchant m ON m.name = JSON_UNQUOTE(JSON_EXTRACT(t.ext_fields, '$.merchant'))
SET t.merchant_id = m.id
WHERE t.ext_fields IS NOT NULL
    AND JSON_TYPE(JSON_EXTRACT(t.ext_fields, '$.merchant')) IS NOT NULL;

-- 第三步（可选）：清理 ext_fields 中的 merchant 字段
-- 注意：执行此步骤后 ext_fields 将不再包含 merchant 字段
-- UPDATE t_transaction
-- SET ext_fields = JSON_REMOVE(ext_fields, '$.merchant')
-- WHERE ext_fields IS NOT NULL
--     AND JSON_TYPE(JSON_EXTRACT(ext_fields, '$.merchant')) IS NOT NULL;

-- --------------------------------------------
-- 4. 验证迁移结果
-- --------------------------------------------
-- 查看商户数量
SELECT COUNT(*) AS merchant_count FROM t_merchant;

-- 查看已关联商户的交易数量
SELECT COUNT(*) AS linked_transaction_count FROM t_transaction WHERE merchant_id IS NOT NULL;

-- 查看未关联商户的交易数量（ext_fields 中有 merchant 但未匹配）
SELECT COUNT(*) AS unmatched_count FROM t_transaction
WHERE ext_fields IS NOT NULL
    AND JSON_TYPE(JSON_EXTRACT(ext_fields, '$.merchant')) IS NOT NULL
    AND merchant_id IS NULL;