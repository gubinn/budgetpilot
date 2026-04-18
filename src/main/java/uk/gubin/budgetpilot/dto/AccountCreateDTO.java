package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class AccountCreateDTO {
    @NotBlank(message = "账户名称不能为空")
    @Size(max = 50, message = "账户名称不能超过50个字符")
    private String name;

    @NotNull(message = "账户类型不能为空")
    @Min(value = 1, message = "账户类型必须为1-3之间的值")
    @Max(value = 3, message = "账户类型必须为1-3之间的值")
    private Integer type;

    private String icon = "wallet";
    private String currency = "CNY";
    private BigDecimal initialBalance = BigDecimal.ZERO;
    private BigDecimal creditLimit;
    private Integer billingDay;
    private Integer paymentDay;
    private Integer sortOrder = 0;
    private Map<String, Object> extFields;
    private Map<String, Object> metadata;
}
