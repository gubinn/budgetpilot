package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Data
public class TransactionCreateDTO {
    @NotNull(message = "交易类型不能为空")
    @Min(value = 1, message = "交易类型必须为1-3之间的值")
    @Max(value = 3, message = "交易类型必须为1-3之间的值")
    private Integer type;

    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    private BigDecimal amount;

    private String currency = "CNY";
    private LocalDate transactionDate;
    private LocalTime transactionTime;
    private String note;

    @NotNull(message = "账户不能为空")
    private Long accountId;

    private Long targetAccountId;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private Long merchantId;
    private String merchantName;
    private Boolean autoCreateMerchant = true;

    private List<String> tags;
    private List<String> attachmentUrls;
    private Boolean isConfirmed = true;
    private Boolean isRecurring;
    private Long recurringId;
    private Map<String, Object> extFields;
    private Map<String, Object> metadata;
}
