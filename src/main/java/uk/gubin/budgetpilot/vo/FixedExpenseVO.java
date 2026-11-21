package uk.gubin.budgetpilot.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FixedExpenseVO {
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private String categoryIcon;
    private BigDecimal spent;
}