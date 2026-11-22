-- 对账模块：渠道账单批次 + 账单明细 + 交易对账标记
-- 支持支付宝 / 微信 / 招商银行账单导入（当前仅实现支付宝解析）

ALTER TABLE t_transaction
    ADD COLUMN reconciled         TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否已对账',
    ADD COLUMN reconcile_channel  VARCHAR(16) DEFAULT NULL COMMENT '对账渠道 ALIPAY/WECHAT/CMB',
    ADD COLUMN reconcile_batch_id BIGINT      DEFAULT NULL COMMENT '最近一次对账批次',
    ADD COLUMN reconciled_at      DATETIME    DEFAULT NULL COMMENT '最近对账时间';

ALTER TABLE t_transaction
    ADD INDEX idx_user_reconciled (user_id, reconciled, transaction_date);

CREATE TABLE IF NOT EXISTS t_reconciliation_batch (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    channel         VARCHAR(16)  NOT NULL COMMENT 'ALIPAY/WECHAT/CMB',
    file_name       VARCHAR(255) NOT NULL,
    file_hash       CHAR(64)     NOT NULL COMMENT '整份文件 SHA-256，用于重复导入幂等',
    account_id      BIGINT       DEFAULT NULL COMMENT '限定参与对账的账户，NULL=全部',
    bill_account    VARCHAR(100) DEFAULT NULL COMMENT '账单中的渠道账号',
    period_start    DATE         DEFAULT NULL,
    period_end      DATE         DEFAULT NULL,
    total_count     INT          NOT NULL DEFAULT 0,
    total_amount    DECIMAL(14,2) NOT NULL DEFAULT 0,
    matched_count   INT          NOT NULL DEFAULT 0,
    unmatched_count INT          NOT NULL DEFAULT 0,
    ignored_count   INT          NOT NULL DEFAULT 0,
    status          VARCHAR(16)  NOT NULL DEFAULT 'DONE' COMMENT 'DONE/PARTIAL',
    summary_json    JSON         DEFAULT NULL COMMENT '账单汇总、解析告警等',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_channel_file (user_id, channel, file_hash),
    KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道账单导入批次';

CREATE TABLE IF NOT EXISTS t_reconciliation_item (
    id                  BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT       NOT NULL,
    batch_id            BIGINT       NOT NULL,
    channel             VARCHAR(16)  NOT NULL,
    item_key            CHAR(64)     NOT NULL COMMENT '渠道+交易号+时间+金额+方向的稳定摘要',
    bill_no             VARCHAR(100) DEFAULT NULL,
    merchant_order_no   VARCHAR(100) DEFAULT NULL,
    created_time        DATETIME     DEFAULT NULL,
    paid_time           DATETIME     DEFAULT NULL,
    trade_date          DATE         NOT NULL,
    direction           VARCHAR(16)  NOT NULL COMMENT 'EXPENSE/INCOME/NEUTRAL',
    amount              DECIMAL(14,2) NOT NULL,
    counterparty        VARCHAR(255) DEFAULT NULL,
    subject             VARCHAR(500) DEFAULT NULL,
    trade_status        VARCHAR(32)  DEFAULT NULL,
    fund_status         VARCHAR(32)  DEFAULT NULL,
    source              VARCHAR(64)  DEFAULT NULL,
    order_type          VARCHAR(64)  DEFAULT NULL,
    fee                 DECIMAL(14,2) NOT NULL DEFAULT 0,
    refunded            DECIMAL(14,2) NOT NULL DEFAULT 0,
    remark              VARCHAR(500) DEFAULT NULL,
    raw_json            JSON         DEFAULT NULL,
    match_status        VARCHAR(16)  NOT NULL DEFAULT 'UNMATCHED' COMMENT 'MATCHED/AMBIGUOUS/UNMATCHED/IGNORED',
    match_type          VARCHAR(16)  DEFAULT NULL COMMENT 'AUTO/AGGREGATE/MANUAL/CREATED',
    match_transaction_id BIGINT      DEFAULT NULL,
    matched_at          DATETIME     DEFAULT NULL,
    ignore_reason       VARCHAR(100) DEFAULT NULL,
    created_at          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_channel_item (user_id, channel, item_key),
    KEY idx_batch_status (batch_id, match_status),
    KEY idx_match_tx (match_transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道账单明细与对账结果';