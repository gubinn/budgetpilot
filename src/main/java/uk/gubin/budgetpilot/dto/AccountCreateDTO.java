package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class AccountCreateDTO {
    @NotBlank(message = "账户名称不能为空")
    private String name;

    @NotNull(message = "账户类型不能为空")
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
