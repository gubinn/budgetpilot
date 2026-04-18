package uk.gubin.budgetpilot.vo;

import lombok.Data;
import uk.gubin.budgetpilot.entity.AlertRule;

import java.time.LocalDateTime;

@Data
public class AlertRuleVO {
    private Long id;
    private String name;
    private Integer type;
    private String typeName;
    private String config;
    private String notifyChannel;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AlertRuleVO fromEntity(AlertRule entity) {
        AlertRuleVO vo = new AlertRuleVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setType(entity.getType());
        vo.setTypeName(getTypeName(entity.getType()));
        vo.setConfig(entity.getConfig());
        vo.setNotifyChannel(entity.getNotifyChannel());
        vo.setIsActive(entity.getIsActive());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private static String getTypeName(Integer type) {
        return switch (type) {
            case 1 -> "预算阈值预警";
            case 2 -> "单笔大额预警";
            case 3 -> "日消费上限";
            case 4 -> "周消费异常";
            case 5 -> "信用卡还款提醒";
            case 6 -> "周期账单提醒";
            case 7 -> "预算未设定提醒";
            case 8 -> "待确认交易提醒";
            default -> "未知类型";
        };
    }
}