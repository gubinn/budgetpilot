-- 卡券包（预付卡/券备忘台账）
--
-- 定位：表外资产。只做备忘、余额和使用流水，不关联账户、不生成交易、
-- 不计入总资产/预算/收支报表。
--
-- 状态说明：
--   status=1 有效、status=2 已作废；「已用完 / 已过期」由 balance 和 expiry_date
--   实时推导，不落库，避免定时任务漏跑导致状态和显示不一致。

CREATE TABLE IF NOT EXISTS t_prepaid_card (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '所属用户ID',
    name            VARCHAR(60)     NOT NULL COMMENT '卡券名称，如：星巴克星礼卡',
    issuer          VARCHAR(60)     NOT NULL COMMENT '发卡方/品牌，如：星巴克',
    channel         VARCHAR(40)     DEFAULT NULL COMMENT '存放渠道：微信小程序/支付宝/实体卡/短信邮件等',
    account_hint    VARCHAR(60)     DEFAULT NULL COMMENT '所在账号提示，如：尾号1234的手机号',
    kind            TINYINT         NOT NULL DEFAULT 1 COMMENT '1-储值卡 2-礼品卡 3-兑换券 4-次数卡',
    unit            TINYINT         NOT NULL DEFAULT 1 COMMENT '1-金额 2-次数/张',
    currency        CHAR(3)         NOT NULL DEFAULT 'CNY' COMMENT '币种（次数卡无实际意义）',
    face_value      DECIMAL(12,2)   DEFAULT NULL COMMENT '面值/总次数',
    balance         DECIMAL(12,2)   NOT NULL DEFAULT 0.00 COMMENT '当前余额/剩余次数',
    issue_date      DATE            DEFAULT NULL COMMENT '发放/购买日期',
    expiry_date     DATE            DEFAULT NULL COMMENT '有效期至，NULL=长期有效',
    source          TINYINT         NOT NULL DEFAULT 4 COMMENT '1-单位发放 2-自购 3-赠送 4-其他',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '1-有效 2-已作废',
    note            VARCHAR(200)    DEFAULT NULL COMMENT '备注',
    last_used_at    DATE            DEFAULT NULL COMMENT '最近一次使用日期',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_status (user_id, status),
    INDEX idx_user_expiry (user_id, expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卡券包（表外备忘台账）';

CREATE TABLE IF NOT EXISTS t_prepaid_card_usage (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '所属用户ID',
    card_id         BIGINT          NOT NULL COMMENT '卡券ID',
    action          TINYINT         NOT NULL COMMENT '1-开卡 2-使用 3-充值 4-调整 5-作废 6-恢复',
    delta           DECIMAL(12,2)   NOT NULL DEFAULT 0.00 COMMENT '变动值，正=增加 负=减少',
    balance_after   DECIMAL(12,2)   NOT NULL DEFAULT 0.00 COMMENT '变动后余额/剩余次数',
    occurred_at     DATE            NOT NULL COMMENT '发生日期',
    note            VARCHAR(100)    DEFAULT NULL COMMENT '备注',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_card (user_id, card_id, occurred_at),
    INDEX idx_user_occurred (user_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卡券使用流水';

-- 老用户补一条「卡券到期提醒」规则（新用户由 UserService.copyDefaultAlertRules 创建）
INSERT INTO t_alert_rule (user_id, name, type, config, notify_channel, is_active)
SELECT u.id,
       '卡券到期提醒',
       10,
       '{"advance_days": 30}',
       COALESCE(NULLIF(c.config_value, ''), 'TELEGRAM'),
       1
FROM t_user u
LEFT JOIN t_config c ON c.config_key = 'notify_channel'
WHERE u.role = 'USER'
  AND u.is_active = 1
  AND NOT EXISTS (
      SELECT 1 FROM t_alert_rule r WHERE r.user_id = u.id AND r.type = 10
  );