package uk.gubin.budgetpilot.service.reconciliation;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/** 从渠道账单解析出的一行明细（尚未落库） */
@Data
public class ParsedBillItem {
    private String itemKey;
    private String billNo;
    private String merchantOrderNo;
    private LocalDateTime createdTime;
    private LocalDateTime paidTime;
    private LocalDate tradeDate;
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
    private Map<String, String> raw;
}