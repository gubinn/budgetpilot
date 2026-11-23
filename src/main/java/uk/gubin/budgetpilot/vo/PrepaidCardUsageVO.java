package uk.gubin.budgetpilot.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PrepaidCardUsageVO {
    private Long id;
    private Integer action;
    private String actionName;
    private BigDecimal delta;
    private BigDecimal balanceAfter;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate occurredAt;
    private String note;
}