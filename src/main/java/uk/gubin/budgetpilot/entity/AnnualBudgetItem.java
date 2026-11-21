package uk.gubin.budgetpilot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gubin.budgetpilot.common.BaseEntity;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_annual_budget_item")
public class AnnualBudgetItem extends BaseEntity {
    private Long userId;
    private Long annualBudgetId;
    private Long categoryId;
    private BigDecimal amount;
}