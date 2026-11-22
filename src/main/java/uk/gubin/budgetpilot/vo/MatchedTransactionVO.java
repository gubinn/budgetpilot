package uk.gubin.budgetpilot.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MatchedTransactionVO {
    private Long id;
    private Integer type;
    private BigDecimal amount;
    private BigDecimal amountBase;
    private String currency;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime transactionTime;
    private Long accountId;
    private String accountName;
    private String merchantName;
    private String categoryName;
    private String note;
    private Boolean isConfirmed;
    private String reviewStatus;
    private Boolean reconciled;
    private String reconcileChannel;
}