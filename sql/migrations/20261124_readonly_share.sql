-- 只读共享账本
--
-- 主人在「设置 → 只读共享」生成链接，家人打开后输入 PIN 即可只读查看。
-- 不创建第二个用户，不共享密码；只读令牌统一以 ro_ 开头，服务端过滤器
-- 无条件拒绝所有写请求，和前端是否隐藏按钮无关。

CREATE TABLE IF NOT EXISTS t_readonly_share (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '账本主人用户ID',
    label           VARCHAR(60)     DEFAULT NULL COMMENT '分享备注，如：给媳妇看',
    code_hash       CHAR(64)        NOT NULL COMMENT '分享码 SHA-256',
    code_hint       CHAR(4)         NOT NULL COMMENT '分享码末4位，列表展示用',
    pin_hash        VARCHAR(100)    DEFAULT NULL COMMENT 'PIN 的 BCrypt 哈希，NULL=免 PIN',
    expires_at      DATETIME        NOT NULL COMMENT '分享失效时间',
    revoked_at      DATETIME        DEFAULT NULL COMMENT '主人手动撤销时间',
    last_used_at    DATETIME        DEFAULT NULL COMMENT '最近一次成功进入时间',
    last_used_ip    VARCHAR(64)     DEFAULT NULL COMMENT '最近一次进入 IP',
    view_count      INT             NOT NULL DEFAULT 0 COMMENT '成功进入次数',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code_hash (code_hash),
    KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='只读共享账本链接';