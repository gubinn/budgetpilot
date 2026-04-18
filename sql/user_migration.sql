-- 用户体系迁移脚本
-- 执行前请备份数据库！

SET NAMES utf8mb4;

USE budgetpilot;

-- 1. 创建用户表
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT          PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)     NOT NULL UNIQUE COMMENT '登录名',
    password    VARCHAR(255)    NOT NULL COMMENT 'BCrypt加密',
    nickname    VARCHAR(50)     DEFAULT '' COMMENT '显示名',
    role        VARCHAR(20)     DEFAULT 'USER' COMMENT 'ADMIN / USER',
    is_active   TINYINT(1)      DEFAULT 1,
    api_key     VARCHAR(100)    DEFAULT NULL COMMENT 'API Key',
    last_login  DATETIME        DEFAULT NULL COMMENT '最后登录时间',
    created_at  DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 创建用户配置表
CREATE TABLE IF NOT EXISTS t_user_config (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    config_key      VARCHAR(50)     NOT NULL,
    config_value    TEXT            NOT NULL,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_user_key (user_id, config_key),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户配置表';

-- 3. 创建默认管理员用户（如果 t_user 为空）
-- 密码: admin123 (BCrypt $2b$)
INSERT INTO t_user (username, password, nickname, role, is_active)
SELECT 'admin', '$2b$10$knPyjetWQdi.KOAUBc9aXOnUcYkjxL/qsBwhcKw4VP708nKQQhq1i', '管理员', 'ADMIN', 1
WHERE NOT EXISTS (SELECT 1 FROM t_user);

-- 4. 为业务表添加 user_id 字段（如果不存在）
ALTER TABLE t_account       ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1 COMMENT '所属用户ID' AFTER id;
ALTER TABLE t_category      ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1 COMMENT '所属用户ID' AFTER id;
ALTER TABLE t_transaction   ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1 COMMENT '所属用户ID' AFTER id;
ALTER TABLE t_recurring_rule ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1 COMMENT '所属用户ID' AFTER id;
ALTER TABLE t_budget        ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1 COMMENT '所属用户ID' AFTER id;
ALTER TABLE t_budget_item   ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1 COMMENT '所属用户ID' AFTER id;
ALTER TABLE t_alert_rule    ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1 COMMENT '所属用户ID' AFTER id;
ALTER TABLE t_alert_log     ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1 COMMENT '所属用户ID' AFTER id;
ALTER TABLE t_merchant      ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1 COMMENT '所属用户ID' AFTER id;

-- 5. 添加索引
ALTER TABLE t_account       ADD INDEX idx_user (user_id);
ALTER TABLE t_category      ADD INDEX idx_user (user_id);
ALTER TABLE t_transaction   ADD INDEX idx_user (user_id);
ALTER TABLE t_recurring_rule ADD INDEX idx_user (user_id);
ALTER TABLE t_budget        ADD INDEX idx_user (user_id);
ALTER TABLE t_budget_item   ADD INDEX idx_user (user_id);
ALTER TABLE t_alert_rule    ADD INDEX idx_user (user_id);
ALTER TABLE t_alert_log     ADD INDEX idx_user (user_id);
ALTER TABLE t_merchant      ADD INDEX idx_user (user_id);
