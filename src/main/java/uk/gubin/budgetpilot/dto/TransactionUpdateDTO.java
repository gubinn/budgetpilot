package uk.gubin.budgetpilot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Data
public class TransactionUpdateDTO {
    @Min(value = 1, message = "交易类型必须为1-4之间的值")
    @Max(value = 4, message = "交易类型必须为1-4之间的值")
    private Integer type;

    @Positive(message = "金额必须大于0")
    private BigDecimal amount;

    private String currency;
    private LocalDate transactionDate;
    private LocalTime transactionTime;
    @Size(max = 200, message = "备注不能超过200个字符")
    private String note;
    private Long accountId;
    private Long targetAccountId;
    private Long categoryId;
    private Long merchantId;
    private String merchantName;
    /** 修改分类时是否同步更新关联商户的默认分类（默认 true，用于学习） */
    private Boolean syncMerchantCategory = true;
    private Boolean autoCreateMerchant = true;
    private List<String> tags;
    private List<String> attachmentUrls;
    private Boolean isConfirmed;
    /** PENDING / CONFIRMED */
    private String reviewStatus;
    private Map<String, Object> extFields;
    private Map<String, Object> metadata;
}
