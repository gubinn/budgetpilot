package uk.gubin.budgetpilot.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecurringRuleUpdateDTO {
    private String name;
    private Integer type;
    private BigDecimal amount;
    private String currency;
    private Long accountId;
    private Long categoryId;
    private Long merchantId;
    private String frequency;
    private Integer executeDay;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextExecute;
    private Boolean autoConfirm;
    private Boolean isActive;
    private String note;
    private String extFields;
}