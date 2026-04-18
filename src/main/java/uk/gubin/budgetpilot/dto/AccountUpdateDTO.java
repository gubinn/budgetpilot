package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class AccountUpdateDTO {
    @Size(max = 50, message = "账户名称不能超过50个字符")
    private String name;
    private String icon;
    private String currency;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private BigDecimal creditLimit;
    private Integer billingDay;
    private Integer paymentDay;
    private Integer sortOrder;
    private Boolean isActive;
    private Map<String, Object> extFields;
    private Map<String, Object> metadata;
}
