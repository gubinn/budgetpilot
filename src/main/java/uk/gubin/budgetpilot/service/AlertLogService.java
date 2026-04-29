package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gubin.budgetpilot.entity.AlertLog;
import uk.gubin.budgetpilot.mapper.AlertLogMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertLogService extends ServiceImpl<AlertLogMapper, AlertLog> {

    private final TelegramNotifyService telegramNotifyService;

    /**
     * 记录预警日志并尝试推送
     */
    public void logAndNotify(Long ruleId, Integer alertType, String title, String content, String channel) {
        AlertLog alertLog = new AlertLog();
        alertLog.setRuleId(ruleId);
        alertLog.setAlertType(alertType);
        alertLog.setTitle(title);
        alertLog.setContent(content);
        alertLog.setNotifyChannel(channel);
        alertLog.setIsRead(false);
        alertLog.setIsSent(false);

        // 尝试 Telegram 推送
        if ("TELEGRAM".equalsIgnoreCase(channel)) {
            boolean sent = telegramNotifyService.send(title, content);
            alertLog.setIsSent(sent);
        }

        baseMapper.insert(alertLog);
        log.info("Alert logged: {} - {} (sent={})", title, channel, alertLog.getIsSent());
    }

    /**
     * 查询未读预警
     */
    public List<AlertLog> getUnreadAlerts() {
        LambdaQueryWrapper<AlertLog> query = new LambdaQueryWrapper<>();
        query.eq(AlertLog::getIsRead, false)
                .orderByDesc(AlertLog::getTriggeredAt);
        return baseMapper.selectList(query);
    }

    /**
     * 标记已读
     */
    public void markRead(Long id) {
        AlertLog alertLog = baseMapper.selectById(id);
        if (alertLog != null) {
            alertLog.setIsRead(true);
            baseMapper.updateById(alertLog);
        }
    }
}
