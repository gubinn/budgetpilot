package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_budget_item")
public class BudgetItem extends BaseEntity {
    private Long budgetId;
    private Long categoryId;
    private BigDecimal amount;
    private BigDecimal spent;
}
