package uk.gubin.budgetpilot.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 往来款明细里的一笔资金流水。
 */
@Data
public class ReceivableTransactionVO {
    private Long id;
    /** RECEIVABLE / PAYABLE */
    private String direction;
    /** LEND / COLLECT / BORROW / REPAY */
    private String effect;
    /** 原交易类型：1-支出 2-收入 */
    private Integer type;
    /** 折合本位币金额 */
    private BigDecimal amount;
    private String currency;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String categoryName;
    private String accountName;
    private String counterparty;
    private String note;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedRepayDate;
}