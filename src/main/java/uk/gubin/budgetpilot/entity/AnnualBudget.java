package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_annual_budget")
public class AnnualBudget extends BaseEntity {
    private Long userId;
    private String budgetYear;
    private BigDecimal totalAmount;
    private String note;
    private Boolean isLocked;
}