package uk.gubin.budgetpilot.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 报表响应 VO
 */
@Data
public class ReportVO {
    /** 月度总览 */
    private MonthlySummary monthlySummary;
    /** 分类明细 */
    private List<CategoryDetailItem> categoryDetails;
    /** 趋势数据 */
    private List<TrendItem> trend;
    /** 同比对比 */
    private CompareItem compare;
    /** 账户汇总 */
    private List<AccountSummaryItem> accountSummary;
    /** 热力图数据 */
    private List<HeatmapItem> heatmap;
    /** 币种分布 */
    private List<CurrencyItem> currencyDistribution;
    /** 商户消费占比 */
    private List<MerchantShareItem> merchantShares;

    @Data
    public static class MonthlySummary {
        private String month;
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal balance;
        private BigDecimal avgDailyExpense;
        private List<CategoryShareItem> categoryShares;
        /** 商户消费占比（月度总览中包含） */
        private List<MerchantShareItem> merchantShares;

        @Data
        public static class CategoryShareItem {
            private Long categoryId;
            private String categoryName;
            private String categoryColor;
            private BigDecimal amount;
            private BigDecimal percentage;
        }
    }

    @Data
    public static class MerchantShareItem {
        private Long merchantId;
        private String merchantName;
        private String merchantColor;
        private Long categoryId;
        private String categoryName;
        private BigDecimal amount;
        private BigDecimal percentage;
        private Integer transactionCount;
    }

    @Data
    public static class CategoryDetailItem {
        private Long id;
        private Integer type;
        private BigDecimal amount;
        private String currency;
        private BigDecimal amountBase;
        private Long accountId;
        private String accountName;
        private String transactionDate;
        private String note;
        private List<String> tags;
        private Map<String, Object> extFields;
    }

    @Data
    public static class TrendItem {
        private String month;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal balance;
    }

    @Data
    public static class CompareItem {
        private String currentMonth;
        private BigDecimal currentIncome;
        private BigDecimal currentExpense;
        private String compareMonth;
        private BigDecimal compareIncome;
        private BigDecimal compareExpense;
        private BigDecimal incomeChangePct;
        private BigDecimal expenseChangePct;
    }

    @Data
    public static class AccountSummaryItem {
        private Long accountId;
        private String accountName;
        private String accountType;
        private String currency;
        private BigDecimal balance;
        private BigDecimal monthIncome;
        private BigDecimal monthExpense;
    }

    @Data
    public static class HeatmapItem {
        private String date;
        private BigDecimal amount;
        private int count;
    }

    @Data
    public static class CurrencyItem {
        private String currency;
        private BigDecimal totalAmount;
        private BigDecimal totalBase;
        private BigDecimal percentage;
    }
}
