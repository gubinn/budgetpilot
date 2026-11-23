package uk.gubin.budgetpilot.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 卡券包列表/详情视图。
 *
 * <p>state 由余额、有效期和作废标记实时推导：
 * ACTIVE（有效）、USED_UP（已用完）、EXPIRED（已过期）、VOID（已作废）。
 * expiringSoon 表示 30 天内到期且仍有余额。</p>
 */
@Data
public class PrepaidCardVO {
    private Long id;
    private String name;
    private String issuer;
    private String channel;
    private String accountHint;
    private Integer kind;
    private String kindName;
    private Integer unit;
    private String unitName;
    private String currency;
    private BigDecimal faceValue;
    private BigDecimal balance;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;
    private Integer source;
    private String sourceName;
    private Integer status;
    private String state;
    private String stateName;
    private Boolean expiringSoon;
    /** 距到期天数：负数表示已过期多少天，null 表示长期有效 */
    private Integer daysToExpiry;
    /** 最近一次动账距今的天数（未用过按开卡时间算），非有效卡为 null */
    private Integer dormantDays;
    private String note;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastUsedAt;
    private LocalDateTime createdAt;
}