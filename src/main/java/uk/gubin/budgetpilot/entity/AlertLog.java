package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_alert_log")
public class AlertLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ruleId;
    private Integer alertType;
    private String title;
    private String content;
    private String notifyChannel;
    private Boolean isSent;
    private Boolean isRead;
    private LocalDateTime triggeredAt;
}
