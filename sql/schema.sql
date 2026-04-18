-- BudgetPilot Database Schema
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS budgetpilot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE budgetpilot;

-- 账户表
CREATE TABLE t_account (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(50)     NOT NULL COMMENT '账户名称（唯一）',
    type            TINYINT         NOT NULL COMMENT '1-现金 2-储蓄卡 3-信用卡 4-电子钱包 5-投资账户',
    icon            VARCHAR(50)     DEFAULT 'wallet' COMMENT '图标标识',
    currency        CHAR(3)         DEFAULT 'CNY' COMMENT '账户币种 ISO 4217',
    initial_balance DECIMAL(14,2)   DEFAULT 0.00 COMMENT '初始余额（账户币种）',
    current_balance DECIMAL(14,2)   DEFAULT 0.00 COMMENT '当前余额（冗余，定时校准）',
    credit_limit    DECIMAL(14,2)   DEFAULT NULL COMMENT '信用额度（信用卡专用）',
    billing_day     TINYINT         DEFAULT NULL COMMENT '账单日 1-28（信用卡专用）',
    payment_day     TINYINT         DEFAULT NULL COMMENT '还款日 1-28（信用卡专用）',
    sort_order      INT             DEFAULT 0 COMMENT '排序权重，越小越靠前',
    is_active       TINYINT(1)      DEFAULT 1,
    ext_fields      JSON            DEFAULT NULL COMMENT '业务扩展字段',
    metadata        JSON            DEFAULT NULL COMMENT '系统元数据',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_name (name),
    INDEX idx_type (type),
    INDEX idx_active_sort (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户表';

-- 分类表
CREATE TABLE t_category (
    id          BIGINT          PRIMARY KEY AUTO_INCREMENT,
    parent_id   BIGINT          DEFAULT 0 COMMENT '父分类ID，0=顶级',
    name        VARCHAR(30)     NOT NULL COMMENT '分类名称',
    type        TINYINT         NOT NULL COMMENT '1-支出 2-收入',
    icon        VARCHAR(50)     DEFAULT NULL,
    color       CHAR(7)         DEFAULT NULL COMMENT '#RRGGBB',
    sort_order  INT             DEFAULT 0,
    is_system   TINYINT(1)      DEFAULT 0 COMMENT '系统预设，不可删除',
    is_active   TINYINT(1)      DEFAULT 1,
    created_at  DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent (parent_id),
    INDEX idx_type_active (type, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支分类表';

-- 交易记录表
CREATE TABLE t_transaction (
    id                  BIGINT          PRIMARY KEY AUTO_INCREMENT,
    type                TINYINT         NOT NULL COMMENT '1-支出 2-收入 3-转账',
    amount              DECIMAL(14,2)   NOT NULL COMMENT '原始金额（原始币种，正数）',
    currency            CHAR(3)         DEFAULT 'CNY' COMMENT '原始币种 ISO 4217',
    amount_base         DECIMAL(14,2)   NOT NULL COMMENT '本位币金额（CNY）',
    exchange_rate       DECIMAL(12,6)   DEFAULT 1.000000 COMMENT '汇率快照：1原始币种=?CNY',
    account_id          BIGINT          NOT NULL COMMENT '所属账户',
    target_account_id   BIGINT          DEFAULT NULL COMMENT '转账目标账户',
    category_id         BIGINT          NOT NULL COMMENT '分类ID',
    merchant_id         BIGINT          DEFAULT NULL COMMENT '商户ID',
    transaction_date    DATE            NOT NULL COMMENT '交易日期',
    transaction_time    TIME            DEFAULT NULL COMMENT '交易时间',
    note                VARCHAR(200)    DEFAULT NULL COMMENT '备注',
    tags                JSON            DEFAULT NULL COMMENT '标签数组 ["日常","必要"]',
    attachment_urls     JSON            DEFAULT NULL COMMENT '附件路径数组',
    is_confirmed        TINYINT(1)      DEFAULT 1 COMMENT '0=待确认（周期交易自动生成）',
    is_recurring        TINYINT(1)      DEFAULT 0,
    recurring_id        BIGINT          DEFAULT NULL COMMENT '关联周期规则ID',
    ext_fields          JSON            DEFAULT NULL COMMENT '业务扩展字段',
    metadata            JSON            DEFAULT NULL COMMENT '系统元数据',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_date (transaction_date),
    INDEX idx_account (account_id),
    INDEX idx_category (category_id),
    INDEX idx_merchant (merchant_id),
    INDEX idx_type_date (type, transaction_date),
    INDEX idx_confirmed (is_confirmed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易记录表';

-- 汇率表
CREATE TABLE t_currency_rate (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    base_currency   CHAR(3)         NOT NULL DEFAULT 'CNY' COMMENT '基准货币（本位币）',
    target_currency CHAR(3)         NOT NULL COMMENT '目标货币',
    rate            DECIMAL(12,6)   NOT NULL COMMENT '1单位目标货币 = rate 基准货币',
    rate_date       DATE            NOT NULL COMMENT '汇率日期',
    source          VARCHAR(50)     DEFAULT 'exchangerate-api' COMMENT '数据来源',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_pair_date (base_currency, target_currency, rate_date),
    INDEX idx_date (rate_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日汇率表';

-- 周期交易规则表
CREATE TABLE t_recurring_rule (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(50)     NOT NULL COMMENT '规则名称',
    type            TINYINT         NOT NULL COMMENT '1-支出 2-收入',
    amount          DECIMAL(14,2)   NOT NULL,
    currency        CHAR(3)         DEFAULT 'CNY' COMMENT '币种',
    account_id      BIGINT          NOT NULL,
    category_id     BIGINT          NOT NULL,
    frequency       VARCHAR(20)     NOT NULL COMMENT 'DAILY/WEEKLY/MONTHLY/YEARLY',
    execute_day     TINYINT         DEFAULT NULL COMMENT '执行日（月=1-28，周=1-7）',
    start_date      DATE            NOT NULL,
    end_date        DATE            DEFAULT NULL COMMENT 'NULL=永久',
    last_executed   DATE            DEFAULT NULL,
    next_execute    DATE            NOT NULL,
    auto_confirm    TINYINT(1)      DEFAULT 0 COMMENT '0=生成待确认交易',
    note            VARCHAR(200)    DEFAULT NULL,
    ext_fields      JSON            DEFAULT NULL COMMENT '自动填入生成交易的ext_fields',
    is_active       TINYINT(1)      DEFAULT 1,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_next (next_execute),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周期性交易规则表';

-- 月度预算表
CREATE TABLE t_budget (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    `year_month`      CHAR(7)         NOT NULL COMMENT 'YYYY-MM',
    total_amount    DECIMAL(14,2)   NOT NULL COMMENT '月度总预算（CNY）',
    note            VARCHAR(200)    DEFAULT NULL,
    is_locked       TINYINT(1)      DEFAULT 0 COMMENT '锁定后不可修改',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_month (`year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度预算表';

-- 预算分类明细表
CREATE TABLE t_budget_item (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    budget_id       BIGINT          NOT NULL,
    category_id     BIGINT          NOT NULL COMMENT '一级分类',
    amount          DECIMAL(14,2)   NOT NULL COMMENT '该分类预算额度（CNY）',
    spent           DECIMAL(14,2)   DEFAULT 0.00 COMMENT '已消费 amount_base 汇总（冗余，异步更新）',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_budget_cat (budget_id, category_id),
    INDEX idx_budget (budget_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算分类明细表';

-- 预警规则表
CREATE TABLE t_alert_rule (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(50)     NOT NULL COMMENT '规则名称',
    type            TINYINT         NOT NULL COMMENT '类型：1-预算阈值 2-单笔大额 3-日消费上限 4-周异常 5-信用卡还款 6-周期账单 7-预算未设定',
    config          JSON            NOT NULL COMMENT '规则配置',
    notify_channel  VARCHAR(50)     DEFAULT 'TELEGRAM' COMMENT 'TELEGRAM/APP',
    is_active       TINYINT(1)      DEFAULT 1,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警规则表';

-- 预警日志表
CREATE TABLE t_alert_log (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    rule_id         BIGINT          NOT NULL,
    alert_type      TINYINT         NOT NULL,
    title           VARCHAR(100)    NOT NULL,
    content         VARCHAR(500)    NOT NULL,
    notify_channel  VARCHAR(50)     NOT NULL COMMENT '实际推送渠道',
    is_sent         TINYINT(1)      DEFAULT 0 COMMENT 'Telegram 是否发送成功',
    is_read         TINYINT(1)      DEFAULT 0,
    triggered_at    DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_unread (is_read, triggered_at),
    INDEX idx_rule (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警日志表';

-- 商户表
CREATE TABLE t_merchant (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL COMMENT '商户名称',
    alias           VARCHAR(200)    DEFAULT NULL COMMENT '别名（用于模糊匹配）',
    category_id     BIGINT          DEFAULT NULL COMMENT '关联分类ID',
    icon            VARCHAR(50)     DEFAULT NULL COMMENT '图标标识',
    color           CHAR(7)         DEFAULT NULL COMMENT '#RRGGBB',
    description     VARCHAR(200)    DEFAULT NULL COMMENT '商户描述',
    tags            JSON            DEFAULT NULL COMMENT '标签数组',
    usage_count     INT             DEFAULT 0 COMMENT '使用次数',
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

-- 系统配置表
CREATE TABLE t_config (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    config_key      VARCHAR(50)     NOT NULL,
    config_value    TEXT            NOT NULL,
    description     VARCHAR(100)    DEFAULT NULL,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';
