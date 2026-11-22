package uk.gubin.budgetpilot.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 按同一组查询条件汇总的交易金额，用于列表页底部的汇总栏。
 * 金额均折算为人民币（amount_base）。
 */
@Data
public class TransactionSummaryVO {
    /** 命中的记录数 */
    private long count;
    private long expenseCount;
    private long incomeCount;
    private long transferCount;
    private long refundCount;

    /** 支出合计（type=1） */
    private BigDecimal expense = BigDecimal.ZERO;
    /** 收入合计（type=2） */
    private BigDecimal income = BigDecimal.ZERO;
    /** 转账合计（type=3，仅作参考，不代表收支） */
    private BigDecimal transfer = BigDecimal.ZERO;
    /** 退款合计（type=4） */
    private BigDecimal refund = BigDecimal.ZERO;

    /** 净支出 = 支出 − 退款 */
    private BigDecimal netExpense = BigDecimal.ZERO;
    /** 净结余 = 收入 − 净支出（转账不计） */
    private BigDecimal net = BigDecimal.ZERO;
}