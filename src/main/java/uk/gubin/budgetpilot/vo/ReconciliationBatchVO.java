package uk.gubin.budgetpilot.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ReconciliationBatchVO {
    private Long id;
    private String channel;
    private String channelLabel;
    private String fileName;
    private Long accountId;
    private String accountName;
    private String billAccount;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodStart;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodEnd;
    private Integer totalCount;
    private BigDecimal totalAmount;
    private Integer matchedCount;
    private Integer unmatchedCount;
    private Integer ignoredCount;
    /** 待处理中因多个候选无法自动确认的条数 */
    private Integer ambiguousCount;
    private String status;
    private Map<String, Object> summary;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}