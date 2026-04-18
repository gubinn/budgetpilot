package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlertRuleCreateDTO {
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 50, message = "规则名称不能超过50个字符")
    private String name;

    @NotNull(message = "规则类型不能为空")
    private Integer type;

    @NotBlank(message = "规则配置不能为空")
    private String config;

    private String notifyChannel = "TELEGRAM";

    private Boolean isActive = true;
}