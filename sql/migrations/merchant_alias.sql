-- 商户聚合：保存支付渠道原始名称，指向归一化后的商户
CREATE TABLE IF NOT EXISTS t_merchant_alias (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '所属用户ID',
    merchant_id     BIGINT          NOT NULL COMMENT '聚合后的商户ID',
    raw_name        VARCHAR(100)    NOT NULL COMMENT '支付渠道原始商户名',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_user_raw (user_id, raw_name),
    INDEX idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户原始名称别名表';