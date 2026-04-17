package uk.gubin.budgetpilot.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BudgetProgressVO {
    private String yearMonth;
    private Long budgetId;
    private BigDecimal totalBudget;
    private BigDecimal totalSpent;
    private BigDecimal remaining;
    private BigDecimal progressPct;
    private int daysPassed;
    private int daysTotal;
    private BigDecimal dailyAvgSpent;
    private BigDecimal dailyAvgRemaining;
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
