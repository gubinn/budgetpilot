-- 年度预算（专项支出）
-- 现有月度预算表 t_budget / t_budget_item 不变。
USE budgetpilot;

CREATE TABLE IF NOT EXISTS t_annual_budget (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '所属用户ID',
    budget_year     CHAR(4)         NOT NULL COMMENT 'YYYY',
    total_amount    DECIMAL(14,2)   NOT NULL COMMENT '年度总预算（CNY）',
    note            VARCHAR(200)    DEFAULT NULL,
    is_locked       TINYINT(1)      DEFAULT 0 COMMENT '锁定后不可修改',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_user_year (user_id, budget_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年度预算表';

CREATE TABLE IF NOT EXISTS t_annual_budget_item (
    id                BIGINT          PRIMARY KEY AUTO_INCREMENT,
    user_id           BIGINT          NOT NULL COMMENT '所属用户ID',
    annual_budget_id  BIGINT          NOT NULL,
    category_id       BIGINT          NOT NULL COMMENT '分类ID',
    amount            DECIMAL(14,2)   NOT NULL COMMENT '该分类年度预算额度（CNY）',
    created_at        DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_annual_budget_cat (annual_budget_id, category_id),
    INDEX idx_annual_budget (annual_budget_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年度预算分类明细表';

SELECT id, user_id, budget_year, total_amount FROM t_annual_budget ORDER BY user_id, budget_year;