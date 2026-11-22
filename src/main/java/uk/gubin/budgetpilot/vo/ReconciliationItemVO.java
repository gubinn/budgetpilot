package uk.gubin.budgetpilot.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ReconciliationItemVO {
    private Long id;
    private Long batchId;
    private String channel;
    private String billNo;
    private String merchantOrderNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidTime;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate tradeDate;
    private String direction;
    private String directionLabel;
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
    private String matchStatus;
    private String matchType;
    private Long matchTransactionId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime matchedAt;
    private String ignoreReason;
    private Map<String, String> raw;
    private MatchedTransactionVO transaction;
}