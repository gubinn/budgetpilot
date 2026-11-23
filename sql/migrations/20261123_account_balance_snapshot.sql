-- 报表净资产趋势：账户月末余额快照
-- 幂等：可在已有库上重复执行
CREATE TABLE IF NOT EXISTS t_account_balance_snapshot (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '所属用户ID',
    account_id      BIGINT          NOT NULL COMMENT '账户ID',
    snapshot_month  CHAR(7)         NOT NULL COMMENT 'YYYY-MM，该月月末余额',
    balance         DECIMAL(14,2)   NOT NULL DEFAULT 0.00 COMMENT '月末余额（账户币种）',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_user_account_month (user_id, account_id, snapshot_month),
    INDEX idx_user_month (user_id, snapshot_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户月末余额快照';
