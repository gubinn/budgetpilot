package uk.gubin.budgetpilot.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gubin.budgetpilot.common.TenantContext;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.MerchantMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.AccountSnapshotService;
import uk.gubin.budgetpilot.service.AnnualBudgetService;
import uk.gubin.budgetpilot.service.BudgetService;
import uk.gubin.budgetpilot.vo.AnnualBudgetProgressVO;
import uk.gubin.budgetpilot.vo.AnnualOverviewVO;
import uk.gubin.budgetpilot.vo.BudgetProgressVO;
import uk.gubin.budgetpilot.vo.NetWorthSnapshotVO;
import uk.gubin.budgetpilot.vo.ReportOverviewVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnnualReviewServiceImplTest {

    @Mock private TransactionMapper transactionMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private BudgetService budgetService;
    @Mock private AnnualBudgetService annualBudgetService;
    @Mock private AccountSnapshotService accountSnapshotService;

    private AnnualReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContext.set(2L);
        service = new AnnualReviewServiceImpl(transactionMapper, categoryMapper,
                merchantMapper, budgetService, annualBudgetService, accountSnapshotService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("历史年：全年汇总、退款冲减、净资产与应急金")
    void historicalYearAggregation() {
        LocalDate today = LocalDate.of(2026, 11, 24);
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, 0L, "工资", 2, "NONE"),
                category(10L, 0L, "餐饮", 1, "DAILY"),
                category(20L, 0L, "房租", 1, "FIXED")));
        when(transactionMapper.selectList(any())).thenReturn(List.of(
                transaction(2, "2024-01-15", "100000", 1L),
                transaction(1, "2024-02-10", "40000", 10L),
                transaction(1, "2024-02-15", "20000", 20L),
                transaction(4, "2024-03-01", "5000", 10L)));
        when(accountSnapshotService.netWorthTrend(any(), any(), anyInt()))
                .thenReturn(List.of(
                        netWorthPoint("2023-12", "100000"),
                        netWorthPoint("2024-12", "150000")));
        NetWorthSnapshotVO detail = new NetWorthSnapshotVO();
        detail.setAccounts(List.of(
                netWorthRow("储蓄卡", "80000"),
                netWorthRow("信用卡", "-10000"),
                netWorthRow("投资账户", "80000")));
        when(accountSnapshotService.snapshotDetail(any(), any())).thenReturn(detail);

        AnnualOverviewVO vo = service.overview(2024, 1, today);

        assertThat(vo.getKpis().getIncome()).isEqualByComparingTo("100000");
        assertThat(vo.getKpis().getExpense()).isEqualByComparingTo("55000");
        assertThat(vo.getKpis().getNet()).isEqualByComparingTo("45000");
        assertThat(vo.getKpis().getSavingsRate()).isEqualByComparingTo("45.0");

        assertThat(vo.getMonthly()).hasSize(12);
        assertThat(vo.getMonthly().get(0).getIncome()).isEqualByComparingTo("100000");
        assertThat(vo.getMonthly().get(1).getExpense()).isEqualByComparingTo("60000");
        assertThat(vo.getMonthly().get(1).getFixed()).isEqualByComparingTo("20000");
        assertThat(vo.getMonthly().get(2).getExpense()).isEqualByComparingTo("-5000");
        assertThat(vo.getBestMonth().getMonth()).isEqualTo("2024-01");
        assertThat(vo.getWorstMonth().getMonth()).isEqualTo("2024-02");

        assertThat(scope(vo, "DAILY").getAmount()).isEqualByComparingTo("35000");
        assertThat(scope(vo, "FIXED").getAmount()).isEqualByComparingTo("20000");
        assertThat(category(vo, "餐饮").getAmount()).isEqualByComparingTo("35000");
        assertThat(category(vo, "房租").getAmount()).isEqualByComparingTo("20000");

        assertThat(vo.getDataStates().isCompareAvailable()).isFalse();
        assertThat(vo.getKpis().getPrevIncome()).isNull();
        assertThat(vo.getNetWorth().getStartNetWorth()).isEqualByComparingTo("100000");
        assertThat(vo.getNetWorth().getEndNetWorth()).isEqualByComparingTo("150000");
        assertThat(vo.getNetWorth().getChange()).isEqualByComparingTo("50000");
        assertThat(vo.getNetWorth().getOtherChange()).isEqualByComparingTo("5000");
        assertThat(vo.getNetWorth().getAssets()).isEqualByComparingTo("160000");
        assertThat(vo.getNetWorth().getLiabilities()).isEqualByComparingTo("10000");
        assertThat(vo.getNetWorth().getLiquidAssets()).isEqualByComparingTo("80000");
        assertThat(vo.getNetWorth().getEmergencyFundMonths()).isEqualByComparingTo("17.5");
        assertThat(vo.getNetWorth().isNetWorthAvailable()).isTrue();

        assertThat(vo.getDataStates().isAnnualBudgetMissing()).isTrue();
        assertThat(vo.getDataStates().isMonthlyBudgetMissing()).isTrue();
        assertThat(vo.getInsights()).isNotEmpty();
    }

    @Test
    @DisplayName("当前年：按已过月份月均、同期同比、未来月份留空")
    void currentYearPartialAndSamePeriodCompare() {
        LocalDate today = LocalDate.of(2026, 6, 15);
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, 0L, "工资", 2, "NONE"),
                category(10L, 0L, "餐饮", 1, "DAILY")));
        when(transactionMapper.selectList(any())).thenReturn(List.of(
                transaction(2, "2026-01-10", "60000", 1L),
                transaction(1, "2026-02-01", "5000", 10L),
                transaction(1, "2026-06-01", "10000", 10L),
                transaction(2, "2025-01-10", "50000", 1L),
                transaction(1, "2025-02-01", "8000", 10L)));
        when(accountSnapshotService.netWorthTrend(any(), any(), anyInt()))
                .thenReturn(List.of(
                        netWorthPoint("2025-12", "100000"),
                        netWorthPoint("2026-06", "120000")));

        when(annualBudgetService.exists("2026")).thenReturn(true);
        when(annualBudgetService.getProgress("2026")).thenReturn(annualBudgetProgress());
        when(budgetService.exists("2026-01")).thenReturn(true);
        when(budgetService.getProgress("2026-01"))
                .thenReturn(monthlyBudgetProgress("2026-01", "10000", "12000"));
        when(budgetService.exists("2026-06")).thenReturn(true);
        when(budgetService.getProgress("2026-06"))
                .thenReturn(monthlyBudgetProgress("2026-06", "10000", "5000"));

        AnnualOverviewVO vo = service.overview(2026, 1, today);

        assertThat(vo.getPeriod().getMonthsCovered()).isEqualTo(6);
        assertThat(vo.getPeriod().isPartialYear()).isTrue();
        assertThat(vo.getPeriod().isPartialMonth()).isTrue();
        assertThat(vo.getPeriod().isCompareSamePeriod()).isTrue();

        assertThat(vo.getKpis().getIncome()).isEqualByComparingTo("60000");
        assertThat(vo.getKpis().getExpense()).isEqualByComparingTo("15000");
        assertThat(vo.getKpis().getAvgMonthlyExpense()).isEqualByComparingTo("2500");
        assertThat(vo.getKpis().getPrevIncome()).isEqualByComparingTo("50000");
        assertThat(vo.getKpis().getPrevExpense()).isEqualByComparingTo("8000");
        assertThat(vo.getKpis().getIncomeChangePct()).isEqualByComparingTo("20.0");
        assertThat(vo.getKpis().getExpenseChangePct()).isEqualByComparingTo("87.5");

        assertThat(vo.getMonthly().get(5).isFuture()).isFalse();
        assertThat(vo.getMonthly().get(6).isFuture()).isTrue();
        assertThat(vo.getMonthly().get(0).getPrevIncome()).isEqualByComparingTo("50000");
        assertThat(vo.getMonthly().get(1).getPrevExpense()).isEqualByComparingTo("8000");

        assertThat(vo.getDataStates().isCompareAvailable()).isTrue();
        assertThat(vo.getDataStates().isCompareInsufficient()).isTrue();
        assertThat(vo.getDataStates().getMonthsWithData()).isEqualTo(3);

        assertThat(vo.getAnnualBudget().isExists()).isTrue();
        assertThat(vo.getAnnualBudget().getPct()).isEqualByComparingTo("25.0");
        assertThat(vo.getAnnualBudget().getTimePct()).isGreaterThan(BigDecimal.ZERO);

        assertThat(vo.getMonthlyBudget().isExists()).isTrue();
        assertThat(vo.getMonthlyBudget().getMonthsWithBudget()).isEqualTo(2);
        assertThat(vo.getMonthlyBudget().getMonthsOver()).isEqualTo(1);
        assertThat(vo.getMonthlyBudget().getBudgetTotal()).isEqualByComparingTo("20000");
        assertThat(vo.getMonthlyBudget().getSpentTotal()).isEqualByComparingTo("17000");
        assertThat(vo.getMonthlyBudget().getPct()).isEqualByComparingTo("85.0");
    }

    @Test
    @DisplayName("分类层级：一级归并、二级带父分类")
    void categoryLevels() {
        LocalDate today = LocalDate.of(2026, 11, 24);
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(10L, 0L, "餐饮", 1, "DAILY"),
                category(11L, 10L, "午餐", 1, "DAILY")));
        when(transactionMapper.selectList(any())).thenReturn(List.of(
                transaction(1, "2024-01-05", "1000", 11L),
                transaction(1, "2024-01-06", "500", 10L)));

        AnnualOverviewVO level1 = service.overview(2024, 1, today);
        AnnualOverviewVO level2 = service.overview(2024, 2, today);

        assertThat(category(level1, "餐饮").getAmount()).isEqualByComparingTo("1500");
        AnnualOverviewVO.CategoryItem lunch = category(level2, "午餐");
        assertThat(lunch.getAmount()).isEqualByComparingTo("1000");
        assertThat(lunch.getParentName()).isEqualTo("餐饮");
    }

    private Category category(Long id, Long parentId, String name, int type, String budgetType) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        category.setType(type);
        category.setBudgetType(budgetType);
        category.setColor("#94a3b8");
        category.setIsActive(true);
        category.setExcludeFromReports(false);
        return category;
    }

    private Transaction transaction(int type, String date, String amount, Long categoryId) {
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setTransactionDate(LocalDate.parse(date));
        transaction.setAmount(new BigDecimal(amount));
        transaction.setAmountBase(new BigDecimal(amount));
        transaction.setCurrency("CNY");
        transaction.setCategoryId(categoryId);
        transaction.setIsConfirmed(true);
        transaction.setReviewStatus("REVIEWED");
        return transaction;
    }

    private ReportOverviewVO.AccountBlock.NetWorthItem netWorthPoint(String month, String amount) {
        ReportOverviewVO.AccountBlock.NetWorthItem item =
                new ReportOverviewVO.AccountBlock.NetWorthItem();
        item.setMonth(month);
        item.setNetWorth(new BigDecimal(amount));
        return item;
    }

    private NetWorthSnapshotVO.Row netWorthRow(String type, String balance) {
        NetWorthSnapshotVO.Row row = new NetWorthSnapshotVO.Row();
        row.setType(type);
        row.setBalance(new BigDecimal(balance));
        return row;
    }

    private AnnualBudgetProgressVO annualBudgetProgress() {
        AnnualBudgetProgressVO progress = new AnnualBudgetProgressVO();
        progress.setYear("2026");
        progress.setTotalBudget(new BigDecimal("100000"));
        progress.setTotalSpent(new BigDecimal("25000"));
        progress.setRemaining(new BigDecimal("75000"));
        progress.setProgressPct(new BigDecimal("25.0"));
        return progress;
    }

    private BudgetProgressVO monthlyBudgetProgress(String month, String budget, String spent) {
        BudgetProgressVO progress = new BudgetProgressVO();
        progress.setYearMonth(month);
        progress.setTotalBudget(new BigDecimal(budget));
        progress.setTotalSpent(new BigDecimal(spent));
        progress.setRemaining(new BigDecimal(budget).subtract(new BigDecimal(spent)));
        progress.setProgressPct(new BigDecimal(spent)
                .divide(new BigDecimal(budget), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, java.math.RoundingMode.HALF_UP));
        return progress;
    }

    private AnnualOverviewVO.ScopeItem scope(AnnualOverviewVO vo, String scope) {
        return vo.getScopes().stream()
                .filter(item -> scope.equals(item.getScope()))
                .findFirst()
                .orElseThrow();
    }

    private AnnualOverviewVO.CategoryItem category(AnnualOverviewVO vo, String name) {
        return vo.getCategories().stream()
                .filter(item -> name.equals(item.getName()))
                .findFirst()
                .orElseThrow();
    }
}