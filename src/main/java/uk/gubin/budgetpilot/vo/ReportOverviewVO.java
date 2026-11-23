package uk.gubin.budgetpilot.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 报表聚合响应：一次请求返回月报页需要的全部数据。
 *
 * <p>口径：支出只统计已入账、已复核、可统计的流水；退款冲减原分类；
 * 固定/专项/日常按分类的 budgetType 归集。所有金额均为 CNY（amountBase）。</p>
 */
@Data
public class ReportOverviewVO {

    private Period period;
    private Kpis kpis;
    private List<ScopeItem> scopes;
    private List<CategoryItem> categories;
    private List<TrendItem> trend;
    private List<DailyItem> daily;
    private List<MerchantItem> merchants;
    private BudgetBlock budget;
    private AnnualBlock annualBudget;
    private AccountBlock accounts;
    private DataStates dataStates;
    private List<InsightItem> insights;

    @Data
    public static class Period {
        private String startDate;
        private String endDate;
        private String label;
        private int days;
        private boolean partialMonth;
        private String compareStartDate;
        private String compareEndDate;
        private String compareLabel;
    }

    @Data
    public static class Kpis {
        private BigDecimal income = BigDecimal.ZERO;
        /** 当前口径支出（scope=ALL 时等于 expenseAll） */
        private BigDecimal expense = BigDecimal.ZERO;
        /** 全部支出，用于“全部支出”副标题 */
        private BigDecimal expenseAll = BigDecimal.ZERO;
        /** 结余始终按全部支出计算 */
        private BigDecimal net = BigDecimal.ZERO;
        private BigDecimal savingsRate = BigDecimal.ZERO;
        private BigDecimal avgDaily = BigDecimal.ZERO;
        private int transactionCount;

        private BigDecimal prevIncome;
        private BigDecimal prevExpense;
        private BigDecimal prevExpenseAll;
        private BigDecimal prevNet;
        private BigDecimal prevSavingsRate;
        private BigDecimal prevAvgDaily;

        private BigDecimal incomeChangePct;
        private BigDecimal expenseChangePct;
        private BigDecimal netChangePct;
        private BigDecimal savingsRateChangePp;
        private BigDecimal avgDailyChangePct;
    }

    @Data
    public static class ScopeItem {
        /** DAILY / FIXED / SPECIAL */
        private String scope;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal share = BigDecimal.ZERO;
        private int count;
    }

    @Data
    public static class CategoryItem {
        private Long categoryId;
        private String name;
        private String color;
        private Long parentId;
        private String parentName;
        /** 该分类在所选区间的主导口径（混合分类取金额最大者） */
        private String scope;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal share = BigDecimal.ZERO;
        private int count;
        private BigDecimal prevAmount;
        private BigDecimal deltaPct;
        private BigDecimal budgetAmount;
        private BigDecimal budgetPct;
        private List<CategoryItem> children;
    }

    @Data
    public static class TrendItem {
        private String month;
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal daily = BigDecimal.ZERO;
        private BigDecimal fixed = BigDecimal.ZERO;
        private BigDecimal special = BigDecimal.ZERO;
        private BigDecimal expense = BigDecimal.ZERO;
    }

    @Data
    public static class DailyItem {
        private String date;
        private BigDecimal daily = BigDecimal.ZERO;
        private BigDecimal fixed = BigDecimal.ZERO;
        private BigDecimal special = BigDecimal.ZERO;
        private BigDecimal expense = BigDecimal.ZERO;
        private int count;
    }

    @Data
    public static class MerchantItem {
        private Long merchantId;
        private String name;
        private BigDecimal amount = BigDecimal.ZERO;
        private int count;
        private String categoryName;
        private String scope;
    }

    @Data
    public static class BudgetBlock {
        private boolean exists;
        private String month;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal spent = BigDecimal.ZERO;
        private BigDecimal remaining = BigDecimal.ZERO;
        private BigDecimal pct = BigDecimal.ZERO;
        private List<Item> items;

        @Data
        public static class Item {
            private Long categoryId;
            private String name;
            private BigDecimal amount = BigDecimal.ZERO;
            private BigDecimal spent = BigDecimal.ZERO;
            private BigDecimal pct = BigDecimal.ZERO;
        }
    }

    @Data
    public static class AnnualBlock {
        private int year;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal spent = BigDecimal.ZERO;
        private BigDecimal remaining = BigDecimal.ZERO;
        private BigDecimal pct = BigDecimal.ZERO;
        private BigDecimal timePct = BigDecimal.ZERO;
        private List<Item> items;

        @Data
        public static class Item {
            private Long categoryId;
            private String name;
            private BigDecimal amount = BigDecimal.ZERO;
            private BigDecimal spent = BigDecimal.ZERO;
            private BigDecimal pct = BigDecimal.ZERO;
        }
    }

    @Data
    public static class AccountBlock {
        private BigDecimal netWorth = BigDecimal.ZERO;
        private BigDecimal assets = BigDecimal.ZERO;
        private BigDecimal liabilities = BigDecimal.ZERO;
        private List<Group> groups;
        private List<Item> accounts;
        private List<NetWorthItem> netWorthTrend;

        @Data
        public static class Group {
            private String type;
            private BigDecimal balance = BigDecimal.ZERO;
        }

        @Data
        public static class Item {
            private Long id;
            private String name;
            private String type;
            private String currency;
            private BigDecimal balance = BigDecimal.ZERO;
        }

        @Data
        public static class NetWorthItem {
            private String month;
            private BigDecimal netWorth = BigDecimal.ZERO;
        }
    }

    @Data
    public static class DataStates {
        private boolean budgetMissing;
        private boolean partialMonth;
        private boolean multiCurrency;
        private boolean noData;
    }

    @Data
    public static class InsightItem {
        /** good / warn / bad / neutral */
        private String tone;
        private String title;
        private String text;
    }
}