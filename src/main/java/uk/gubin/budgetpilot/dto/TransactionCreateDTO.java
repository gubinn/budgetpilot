package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Data
public class TransactionCreateDTO {
    @NotNull(message = "交易类型不能为空")
    @Min(value = 1, message = "交易类型必须为1-4之间的值")
    @Max(value = 4, message = "交易类型必须为1-4之间的值")
    private Integer type;

    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    private BigDecimal amount;

    /** 外部系统幂等 ID：同一用户下相同值只创建一次，重复请求直接返回已有交易 */
    @Size(max = 100, message = "外部幂等ID不能超过100个字符")
    private String externalId;

    private String currency = "CNY";
    private LocalDate transactionDate;
    private LocalTime transactionTime;
    @Size(max = 200, message = "备注不能超过200个字符")
    private String note;

    @NotNull(message = "账户不能为空")
    private Long accountId;

    private Long targetAccountId;

    private Long categoryId;

    private Long merchantId;
    @Size(max = 50, message = "商户名称不能超过50个字符")
    private String merchantName;
    private Boolean autoCreateMerchant = true;

    private List<String> tags;
    private List<String> attachmentUrls;
    private Boolean isConfirmed = true;
    /** PENDING / CONFIRMED，默认 CONFIRMED */
    private String reviewStatus;
    private Boolean isRecurring;
    private Long recurringId;
    private Map<String, Object> extFields;
    private Map<String, Object> metadata;
}
