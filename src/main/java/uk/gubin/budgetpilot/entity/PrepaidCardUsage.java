package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 卡券使用流水：开卡、使用、充值、调整、作废。
 */
@Data
@TableName("t_prepaid_card_usage")
public class PrepaidCardUsage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long cardId;
    /** 1-开卡 2-使用 3-充值 4-调整 5-作废 6-恢复 */
    private Integer action;
    private BigDecimal delta;
    private BigDecimal balanceAfter;
    private LocalDate occurredAt;
    private String note;
}