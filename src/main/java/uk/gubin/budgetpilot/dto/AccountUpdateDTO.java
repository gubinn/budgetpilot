package uk.gubin.budgetpilot.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class AccountUpdateDTO {
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
