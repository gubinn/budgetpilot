-- Add merchant_id to recurring rules table
USE budgetpilot;

ALTER TABLE t_recurring_rule
    ADD COLUMN merchant_id BIGINT DEFAULT NULL COMMENT '商户ID' AFTER category_id,
    ADD INDEX idx_merchant (merchant_id);
