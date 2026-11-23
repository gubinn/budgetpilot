package uk.gubin.budgetpilot.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 按“对方 + 方向”合并后的往来款余额。
 */
@Data
public class ReceivableGroupVO {
    /** RECEIVABLE / PAYABLE */
    private String direction;
    private String counterparty;

    /** 累计借出（应收）或累计借入（应付） */
    private BigDecimal principalTotal = BigDecimal.ZERO;
    /** 累计收回（应收）或累计归还（应付） */
    private BigDecimal settledTotal = BigDecimal.ZERO;
    /** 按先进先出核销后的未结清金额 */
    private BigDecimal outstanding = BigDecimal.ZERO;
    /** 多收/多还、留待下一笔抵扣的金额 */
    private BigDecimal overpaid = BigDecimal.ZERO;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate firstLoanDate;
    /** 仍未结清的最早一笔本金日期，用于计算账龄 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate firstOutstandingDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastActivityDate;
    /** 仍未结清本金里最早的约定还款日 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedRepayDate;

    private int ageDays;
    /** 超过约定还款日的天数，未约定或未逾期为 0 */
    private int overdueDays;
    private int transactionCount;

    private List<ReceivableTransactionVO> transactions = new ArrayList<>();
}