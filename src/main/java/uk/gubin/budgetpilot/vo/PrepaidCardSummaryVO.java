package uk.gubin.budgetpilot.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 卡券包汇总。金额卡与次数卡分开统计，避免把「3 次」加进「元」。
 */
@Data
public class PrepaidCardSummaryVO {
    /** 有效卡可用余额合计（金额卡） */
    private BigDecimal moneyBalanceTotal = BigDecimal.ZERO;
    /** 有效卡剩余次数合计（次数卡） */
    private BigDecimal countBalanceTotal = BigDecimal.ZERO;
    /** 有效且仍有余额的卡数 */
    private int activeCount;
    /** 30 天内到期且仍有余额的卡数 */
    private int expiringSoonCount;
    private BigDecimal expiringSoonMoney = BigDecimal.ZERO;
    private BigDecimal expiringSoonCountValue = BigDecimal.ZERO;
    /** 已过期但仍有余额的卡数 */
    private int expiredCount;
    private BigDecimal expiredMoney = BigDecimal.ZERO;
    private BigDecimal expiredCountValue = BigDecimal.ZERO;
    /** 已用完的卡数 */
    private int usedUpCount;
    /** 已作废的卡数 */
    private int voidCount;
    /** 90 天以上没有动过的有效卡数 */
    private int dormantCount;
    private int totalCount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate asOf;
}