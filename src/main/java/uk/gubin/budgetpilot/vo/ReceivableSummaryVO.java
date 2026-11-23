package uk.gubin.budgetpilot.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 往来款汇总：应收/应付余额、涉及人数、账龄。
 */
@Data
public class ReceivableSummaryVO {
    /** 应收未结清合计（我借出去还没收回来的钱） */
    private BigDecimal receivableTotal = BigDecimal.ZERO;
    /** 应付未结清合计（我借进来还没还的钱） */
    private BigDecimal payableTotal = BigDecimal.ZERO;
    /** 应收 - 应付 */
    private BigDecimal net = BigDecimal.ZERO;
    /** 有未结清余额的对方人数（同一个人的应收/应付只算一次） */
    private int counterpartyCount;
    /** 最长账龄（天） */
    private int oldestDays;
    /** 逾期项数（配置了约定还款日且已过期的对方数） */
    private int overdueCount;
    /** 逾期未结清金额 */
    private BigDecimal overdueTotal = BigDecimal.ZERO;
    /** 未识别出对方的未结清交易笔数 */
    private int unlabeledCount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate asOf;
}