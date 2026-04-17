package uk.gubin.budgetpilot.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Data
public class TransactionVO {
    private Long id;
    private Integer type;
    private BigDecimal amount;
    private String currency;
    private BigDecimal amountBase;
    private BigDecimal exchangeRate;
    private Long accountId;
    private String accountName;
    private Long targetAccountId;
    private String targetAccountName;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime transactionTime;
    private String note;
    private List<String> tags;
    private List<String> attachmentUrls;
    private Boolean isConfirmed;
    private Boolean isRecurring;
    private Long recurringId;
    private Map<String, Object> extFields;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
