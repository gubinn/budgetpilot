package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_transaction")
public class Transaction extends BaseEntity {
    private Integer type;
    private java.math.BigDecimal amount;
    private String currency;
    private java.math.BigDecimal amountBase;
    private java.math.BigDecimal exchangeRate;
    private Long accountId;
    private Long targetAccountId;
    private Long categoryId;
    private LocalDate transactionDate;
    private LocalTime transactionTime;
    private String note;
    private String tags;
    @TableField("attachment_urls")
    private String attachmentUrls;
    private Boolean isConfirmed;
    private Boolean isRecurring;
    private Long recurringId;
    @TableField("ext_fields")
    private String extFields;
    private String metadata;
}
