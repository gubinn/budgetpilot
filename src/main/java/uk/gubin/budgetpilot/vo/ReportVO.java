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
    /** 商户消费占比（剔除固定支出分类） */
    private List<MerchantShareItem> merchantSharesExcludingFixed;

    @Data
    public static class MonthlySummary {
        private String month;
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal balance;
        private BigDecimal avgDailyExpense;
        private List<CategoryShareItem> categoryShares;
        /** 分类占比（剔除固定支出分类，用于首页“不含固定支出”视图） */
        private List<CategoryShareItem> categorySharesExcludingFixed;
        /** 标签统计（支出交易；一笔交易多个标签会在每个标签下各计一次） */
        private List<TagShareItem> tagShares;
        /** 商户消费占比（月度总览中包含） */
        private List<MerchantShareItem> merchantShares;

        @Data
        public static class CategoryShareItem {
            private Long categoryId;
            private String categoryName;
            private String categoryColor;
            /** level=2 时的父分类，用于显示“父/子”路径 */
            private Long parentId;
            private String parentName;
            private BigDecimal amount;
            private BigDecimal percentage;
        }
    }

    @Data
    public static class TagShareItem {
        private String tag;
        private BigDecimal amount;
        private BigDecimal percentage;
        private Integer count;
    }

    @Data
    public static class ExtFieldShareItem {
        /** 扩展字段的值（或“未标注”） */
        private String value;
        private BigDecimal amount;
        private BigDecimal percentage;
        private Integer count;
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
