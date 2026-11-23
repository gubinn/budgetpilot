-- 预置预警规则修正
--
-- 1. 预置规则过去把 notify_channel 写死为 TELEGRAM。全局渠道切到 WhatsApp 后，
--    交易触发的 1/2/3/8 规则仍尝试发 Telegram（未配置 token），日志 is_sent=0。
--    此处只回填「名称仍是预置名称、渠道仍是 TELEGRAM」的规则，用户自建规则不动。
-- 2. 补齐缺失的「待确认交易提醒」(type 8) 预置规则。
--    该规则类型代码与页面早已支持，但历史用户没有副本，定时任务永远提前返回。

UPDATE t_alert_rule r
JOIN t_config c ON c.config_key = 'notify_channel'
SET r.notify_channel = c.config_value
WHERE r.notify_channel = 'TELEGRAM'
  AND c.config_value IS NOT NULL
  AND c.config_value <> ''
  AND r.name IN (
      '预算阈值预警',
      '单笔大额预警',
      '日消费上限',
      '周消费异常检测',
      '信用卡还款提醒',
      '周期账单提醒',
      '月度预算未设定',
      '月度预算总结'
  );

INSERT INTO t_alert_rule (user_id, name, type, config, notify_channel, is_active)
SELECT u.id,
       '待确认交易提醒',
       8,
       '{"min_count": 1}',
       COALESCE(NULLIF(c.config_value, ''), 'TELEGRAM'),
       1
FROM t_user u
LEFT JOIN t_config c ON c.config_key = 'notify_channel'
WHERE u.role = 'USER'
  AND u.is_active = 1
  AND NOT EXISTS (
      SELECT 1 FROM t_alert_rule r WHERE r.user_id = u.id AND r.type = 8
  );