package uk.gubin.budgetpilot.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 年度复盘聚合响应：一次请求返回年度页需要的全部数据。
 *
 * <p>口径与月度复盘一致：只统计已入账、已复核、可统计的流水；
 * 退款冲减原分类；固定/专项/日常按分类的 budgetType 归集；
 * 金额均为 CNY（amountBase）。</p>
 */
@Data
public class AnnualOverviewVO {

    private Period period;
    private Kpis kpis;
    private List<MonthItem> monthly = new ArrayList<>();
    private MonthHighlight bestMonth;
    private MonthHighlight worstMonth;
    private List<ScopeItem> scopes = new ArrayList<>();
    private List<CategoryItem> categories = new ArrayList<>();
    private List<IncomeCategoryItem> incomeCategories = new ArrayList<>();
    private NetWorthBlock netWorth = new NetWorthBlock();
    private AnnualBudgetBlock annualBudget = new AnnualBudgetBlock();
    private MonthlyBudgetBlock monthlyBudget = new MonthlyBudgetBlock();
    private List<MerchantItem> merchants = new ArrayList<>();
    private LargestExpense largestExpense;
    private DataStates dataStates = new DataStates();
    private List<InsightItem> insights = new ArrayList<>();

    @Data
    public static class Period {
        private Integer year;
        private String label;
        private String startDate;
        private String endDate;
        /** 本年按当前月，历史年按 12 */
        private int monthsCovered;
        private boolean partialYear;
        private boolean partialMonth;
        private int daysCovered;
        private Integer compareYear;
        private String compareLabel;
        private String compareStartDate;
        private String compareEndDate;
        /** 本年为“去年同期”，历史年为完整上一年 */
        private boolean compareSamePeriod;
    }

    @Data
    public static class Kpis {
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal expense = BigDecimal.ZERO;
        private BigDecimal net = BigDecimal.ZERO;
        private BigDecimal savingsRate = BigDecimal.ZERO;
        private BigDecimal avgMonthlyIncome = BigDecimal.ZERO;
        private BigDecimal avgMonthlyExpense = BigDecimal.ZERO;
        private BigDecimal avgMonthlyNet = BigDecimal.ZERO;
        private int transactionCount;
        private int spendingDays;
        private int activityDays;
        private int noSpendDays;

        private BigDecimal prevIncome;
        private BigDecimal prevExpense;
        private BigDecimal prevNet;
        private BigDecimal prevSavingsRate;

        private BigDecimal incomeChangePct;
        private BigDecimal expenseChangePct;
        private BigDecimal netChangePct;
        private BigDecimal savingsRateChangePp;
    }

    @Data
    public static class MonthItem {
        private String month;
        private String label;
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal expense = BigDecimal.ZERO;
        private BigDecimal net = BigDecimal.ZERO;
        private BigDecimal savingsRate = BigDecimal.ZERO;
        private BigDecimal daily = BigDecimal.ZERO;
        private BigDecimal fixed = BigDecimal.ZERO;
        private BigDecimal special = BigDecimal.ZERO;
        private BigDecimal cumulativeNet = BigDecimal.ZERO;
        private BigDecimal prevIncome;
        private BigDecimal prevExpense;
        /** 本年当前月之后 */
        private boolean future;
        private boolean hasData;
    }

    @Data
    public static class MonthHighlight {
        private String month;
        private String label;
        private BigDecimal net = BigDecimal.ZERO;
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal expense = BigDecimal.ZERO;
    }

    @Data
    public static class ScopeItem {
        private String scope;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal share = BigDecimal.ZERO;
        private int count;
        private BigDecimal avgPerMonth = BigDecimal.ZERO;
        private BigDecimal prevAmount;
        private BigDecimal deltaPct;
    }

    @Data
    public static class CategoryItem {
        private Long categoryId;
        private String name;
        private String color;
        private Long parentId;
        private String parentName;
        /** 该分类的主导口径 */
        private String scope;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal share = BigDecimal.ZERO;
        private int count;
        private BigDecimal avgPerMonth = BigDecimal.ZERO;
        private BigDecimal prevAmount;
        private BigDecimal deltaPct;
    }

    @Data
    public static class IncomeCategoryItem {
        private Long categoryId;
        private String name;
        private String color;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal share = BigDecimal.ZERO;
        private int count;
        private BigDecimal avgPerMonth = BigDecimal.ZERO;
        private BigDecimal prevAmount;
        private BigDecimal deltaPct;
    }

    @Data
    public static class NetWorthBlock {
        private String startMonth;
        private String startLabel;
        private BigDecimal startNetWorth = BigDecimal.ZERO;
        private String endMonth;
        private String endLabel;
        private BigDecimal endNetWorth = BigDecimal.ZERO;
        private BigDecimal change = BigDecimal.ZERO;
        private BigDecimal changePct;
        private BigDecimal annualNet = BigDecimal.ZERO;
        /** 净资产变化与全年结余的差额：投资增值、余额调整或未记账等 */
        private BigDecimal otherChange;
        private BigDecimal assets = BigDecimal.ZERO;
        private BigDecimal liabilities = BigDecimal.ZERO;
        /** 现金、储蓄卡、电子钱包的正余额 */
        private BigDecimal liquidAssets = BigDecimal.ZERO;
        /** 日常 + 固定的月均支出，用于应急金 */
        private BigDecimal essentialMonthlyExpense = BigDecimal.ZERO;
        private BigDecimal emergencyFundMonths;
        private List<Point> trend = new ArrayList<>();
        private List<Group> groups = new ArrayList<>();
        private boolean netWorthAvailable;

        @Data
        public static class Point {
            private String month;
            private BigDecimal netWorth = BigDecimal.ZERO;
        }

        @Data
        public static class Group {
            private String type;
            private BigDecimal balance = BigDecimal.ZERO;
        }
    }

    @Data
    public static class AnnualBudgetBlock {
        private boolean exists;
        private int year;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal spent = BigDecimal.ZERO;
        private BigDecimal remaining = BigDecimal.ZERO;
        private BigDecimal pct = BigDecimal.ZERO;
        private BigDecimal timePct = BigDecimal.ZERO;
        private List<Item> items = new ArrayList<>();

        @Data
        public static class Item {
            private Long categoryId;
            private String name;
            private BigDecimal amount = BigDecimal.ZERO;
            private BigDecimal spent = BigDecimal.ZERO;
            private BigDecimal remaining = BigDecimal.ZERO;
            private BigDecimal pct = BigDecimal.ZERO;
        }
    }

    @Data
    public static class MonthlyBudgetBlock {
        private boolean exists;
        private int monthsWithBudget;
        private int monthsOver;
        private int monthsCovered;
        private BigDecimal budgetTotal = BigDecimal.ZERO;
        private BigDecimal spentTotal = BigDecimal.ZERO;
        private BigDecimal pct = BigDecimal.ZERO;
        private List<Item> items = new ArrayList<>();

        @Data
        public static class Item {
            private String month;
            private BigDecimal budget = BigDecimal.ZERO;
            private BigDecimal spent = BigDecimal.ZERO;
            private BigDecimal pct = BigDecimal.ZERO;
            private boolean over;
        }
    }

    @Data
    public static class MerchantItem {
        private Long merchantId;
        private String name;
        private BigDecimal amount = BigDecimal.ZERO;
        private int count;
        private BigDecimal share = BigDecimal.ZERO;
        private String categoryName;
    }

    @Data
    public static class LargestExpense {
        private String date;
        private BigDecimal amount = BigDecimal.ZERO;
        private String note;
        private String categoryName;
        private String merchantName;
    }

    @Data
    public static class DataStates {
        private boolean partialYear;
        private boolean noData;
        private boolean multiCurrency;
        private boolean annualBudgetMissing;
        private boolean monthlyBudgetMissing;
        private int monthsWithData;
        private boolean compareAvailable;
        private boolean compareInsufficient;
        private boolean netWorthAvailable;
    }

    @Data
    public static class InsightItem {
        /** good / warn / bad / neutral */
        private String tone;
        private String title;
        private String text;
    }
}