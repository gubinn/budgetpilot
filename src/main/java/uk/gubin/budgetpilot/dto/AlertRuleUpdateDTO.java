package uk.gubin.budgetpilot.dto;

import lombok.Data;

@Data
public class AlertRuleUpdateDTO {
    private String name;
    private Integer type;
    private String config;
    private String notifyChannel;
    private Boolean isActive;
}