package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_budget")
public class Budget extends BaseEntity {
    private Long userId;
    @TableField("`year_month`")
    private String yearMonth;
    private java.math.BigDecimal totalAmount;
    private String note;
    private Boolean isLocked;
}
