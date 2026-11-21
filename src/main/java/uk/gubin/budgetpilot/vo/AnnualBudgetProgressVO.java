package uk.gubin.budgetpilot.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AnnualBudgetProgressVO {
    private String year;
    private Long annualBudgetId;
    private String note;
    private BigDecimal totalBudget;
    private BigDecimal totalSpent;
    private BigDecimal remaining;
    private BigDecimal progressPct;
    private Boolean isLocked;
    private List<ItemProgress> items;

    @Data
    public static class ItemProgress {
        private Long categoryId;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;
        private BigDecimal budget;
        private BigDecimal spent;
        private BigDecimal remaining;
        private BigDecimal progressPct;
        private String status;
    }
}