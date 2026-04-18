package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_recurring_rule")
public class RecurringRule extends BaseEntity {
    private String name;
    private Integer type;
    private java.math.BigDecimal amount;
    private String currency;
    private Long accountId;
    private Long categoryId;
    private Long merchantId;
    private String frequency;
    private Integer executeDay;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate lastExecuted;
    private LocalDate nextExecute;
    private Boolean autoConfirm;
    private String note;
    @TableField("ext_fields")
    private String extFields;
    private Boolean isActive;
}
