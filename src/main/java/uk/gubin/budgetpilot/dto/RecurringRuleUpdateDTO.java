package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecurringRuleUpdateDTO {
    @Size(max = 50, message = "规则名称不能超过50个字符")
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
    @Size(max = 200, message = "备注不能超过200个字符")
    private String note;
    private String extFields;
}