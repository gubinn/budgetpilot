-- 招行渠道拆分：CMB -> CMB_DEBIT（储蓄卡）/ CMB_CREDIT（信用卡）
-- 信用卡账单按账户对账：批次必须指定 account_id，迁移前导入的 CMB 批次按储蓄卡处理。

ALTER TABLE t_transaction
    MODIFY COLUMN reconcile_channel VARCHAR(16) DEFAULT NULL
        COMMENT '对账渠道 ALIPAY/WECHAT/CMB_DEBIT/CMB_CREDIT';

ALTER TABLE t_reconciliation_batch
    MODIFY COLUMN channel VARCHAR(16) NOT NULL
        COMMENT 'ALIPAY/WECHAT/CMB_DEBIT/CMB_CREDIT';

ALTER TABLE t_reconciliation_item
    MODIFY COLUMN channel VARCHAR(16) NOT NULL
        COMMENT 'ALIPAY/WECHAT/CMB_DEBIT/CMB_CREDIT';

UPDATE t_reconciliation_batch SET channel = 'CMB_DEBIT' WHERE channel = 'CMB';
UPDATE t_reconciliation_item  SET channel = 'CMB_DEBIT' WHERE channel = 'CMB';
UPDATE t_transaction           SET reconcile_channel = 'CMB_DEBIT' WHERE reconcile_channel = 'CMB';