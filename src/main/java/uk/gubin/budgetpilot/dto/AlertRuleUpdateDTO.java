package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlertRuleUpdateDTO {
    @Size(max = 50, message = "规则名称不能超过50个字符")
    private String name;
    private Integer type;
    private String config;
    private String notifyChannel;
    private Boolean isActive;
}