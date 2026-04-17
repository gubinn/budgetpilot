package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_alert_rule")
public class AlertRule extends BaseEntity {
    private String name;
    private Integer type;
    @TableField("config")
    private String config;
    private String notifyChannel;
    private Boolean isActive;
}
