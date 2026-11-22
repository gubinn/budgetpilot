package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_reconciliation_batch")
public class ReconciliationBatch extends BaseEntity {
    private Long userId;
    /** ALIPAY / WECHAT / CMB */
    private String channel;
    private String fileName;
    private String fileHash;
    private Long accountId;
    private String billAccount;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer totalCount;
    private BigDecimal totalAmount;
    private Integer matchedCount;
    private Integer unmatchedCount;
    private Integer ignoredCount;
    /** DONE / PARTIAL */
    private String status;
    @TableField("summary_json")
    private String summaryJson;
}