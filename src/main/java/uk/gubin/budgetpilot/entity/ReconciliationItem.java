package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_reconciliation_item")
public class ReconciliationItem extends BaseEntity {
    private Long userId;
    private Long batchId;
    private String channel;
    private String itemKey;
    private String billNo;
    private String merchantOrderNo;
    private LocalDateTime createdTime;
    private LocalDateTime paidTime;
    private LocalDate tradeDate;
    /** EXPENSE / INCOME / NEUTRAL */
    private String direction;
    private BigDecimal amount;
    private String counterparty;
    private String subject;
    private String tradeStatus;
    private String fundStatus;
    private String source;
    private String orderType;
    private BigDecimal fee;
    private BigDecimal refunded;
    private String remark;
    @TableField("raw_json")
    private String rawJson;
    /** MATCHED / AMBIGUOUS / UNMATCHED / IGNORED */
    private String matchStatus;
    /** AUTO / AGGREGATE / MANUAL / CREATED */
    private String matchType;
    private Long matchTransactionId;
    private LocalDateTime matchedAt;
    private String ignoreReason;
}