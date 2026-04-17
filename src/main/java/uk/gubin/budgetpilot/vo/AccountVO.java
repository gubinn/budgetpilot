package uk.gubin.budgetpilot.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AccountVO {
    private Long id;
    private String name;
    private Integer type;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
