package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 卡券包：预付卡、购物卡、电影兑换券等表外备忘。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_prepaid_card")
public class PrepaidCard extends BaseEntity {
    private Long userId;
    private String name;
    private String issuer;
    private String channel;
    private String accountHint;
    /** 1-储值卡 2-礼品卡 3-兑换券 4-次数卡 */
    private Integer kind;
    /** 1-金额 2-次数/张 */
    private Integer unit;
    private String currency;
    private BigDecimal faceValue;
    private BigDecimal balance;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    /** 1-单位发放 2-自购 3-赠送 4-其他 */
    private Integer source;
    /** 1-有效 2-已作废 */
    private Integer status;
    private String note;
    private LocalDate lastUsedAt;
}