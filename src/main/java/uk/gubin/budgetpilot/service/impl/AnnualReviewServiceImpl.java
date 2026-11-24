package uk.gubin.budgetpilot.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.common.TenantContext;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Merchant;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.MerchantMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.AccountSnapshotService;
import uk.gubin.budgetpilot.service.AnnualBudgetService;
import uk.gubin.budgetpilot.service.AnnualReviewService;
import uk.gubin.budgetpilot.service.BudgetService;
import uk.gubin.budgetpilot.vo.AnnualBudgetProgressVO;
import uk.gubin.budgetpilot.vo.AnnualOverviewVO;
import uk.gubin.budgetpilot.vo.BudgetProgressVO;
import uk.gubin.budgetpilot.vo.NetWorthSnapshotVO;
import uk.gubin.budgetpilot.vo.ReportOverviewVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 年度复盘聚合。
 *
 * <p>口径与月度复盘保持一致：只统计已入账、已复核、可统计的流水；
 * 退款冲减原分类；固定/专项/日常按分类的 budgetType 归集；金额用 amountBase（CNY）。</p>
 *
 * <p>本类刻意与 {@link ReportServiceImpl} 解耦，避免改动月度复盘；
 * 如果月度口径发生变化，这里的过滤规则需要同步。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnualReviewServiceImpl implements AnnualReviewService {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.004");
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final List<String> SCOPE_ORDER = List.of("DAILY", "FIXED", "SPECIAL");
    private static final Set<String> LIQUID_ACCOUNT_TYPES =
            Set.of("现金", "储蓄卡", "电子钱包");
    private static final List<String> ACCOUNT_TYPE_ORDER =
            List.of("储蓄卡", "信用卡", "投资账户", "电子钱包", "现金", "未知");

    private final TransactionMapper transactionMapper;
    private final CategoryMapper categoryMapper;
    private final MerchantMapper merchantMapper;
    private final BudgetService budgetService;
    private final AnnualBudgetService annualBudgetService;
    private final AccountSnapshotService accountSnapshotService;

    @Override
    public AnnualOverviewVO overview(Integer year, int level) {
        return overview(year, level, LocalDate.now());
    }

    /**
     * package-private，便于单测传入固定日期。
     */
    AnnualOverviewVO overview(Integer year, int level, LocalDate today) {
        int y = year == null ? today.getYear() : year;
        if (y < 2000 || y > today.getYear()) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "年份不合法，应在 2000 到 " + today.getYear() + " 之间");
        }
        int reportLevel = level == 2 ? 2 : 1;
        boolean currentYear = y == today.getYear();
        int monthsCovered = currentYear ? today.getMonthValue() : 12;

        LocalDate start = LocalDate.of(y, 1, 1);
        LocalDate end = currentYear ? today : LocalDate.of(y, 12, 31);
        boolean compareSamePeriod = currentYear;
        LocalDate compareStart = start.minusYears(1);
        LocalDate compareEnd = compareSamePeriod ? today.minusYears(1) : end.minusYears(1);

        LocalDate queryStart = start.isBefore(compareStart) ? start : compareStart;
        LocalDate queryEnd = end.isAfter(compareEnd) ? end : compareEnd;

        List<Transaction> all = transactionMapper.selectList(
                new LambdaQueryWrapper<Transaction>()
                        .in(Transaction::getType, 1, 2, 4)
                        .eq(Transaction::getIsConfirmed, true)
                        .eq(Transaction::getReviewStatus, "REVIEWED")
                        .ge(Transaction::getTransactionDate, queryStart)
                        .le(Transaction::getTransactionDate, queryEnd));
        if (all == null) {
            all = List.of();
        }
        Map<Long, Category> catMap = loadCategoryMap();

        List<Transaction> currentExpenses = reportableExpenses(all, start, end, catMap);
        List<Transaction> currentIncomes = reportableIncomes(all, start, end, catMap);
        List<Transaction> prevExpenses = reportableExpenses(all, compareStart, compareEnd, catMap);
        List<Transaction> prevIncomes = reportableIncomes(all, compareStart, compareEnd, catMap);

        BigDecimal income = sumBase(currentIncomes);
        BigDecimal expense = netOf(currentExpenses);
        BigDecimal net = income.subtract(expense);
        BigDecimal prevIncome = sumBase(prevIncomes);
        BigDecimal prevExpense = netOf(prevExpenses);
        BigDecimal prevNet = prevIncome.subtract(prevExpense);

        boolean compareAvailable = !prevExpenses.isEmpty() || !prevIncomes.isEmpty();
        Set<YearMonth> currentMonths = monthsOf(currentExpenses, currentIncomes);
        Set<YearMonth> prevMonths = monthsOf(prevExpenses, prevIncomes);
        int minCompareMonths = Math.min(3, Math.max(1, monthsCovered));
        boolean compareInsufficient = compareAvailable
                && prevMonths.size() < minCompareMonths;

        AnnualOverviewVO vo = new AnnualOverviewVO();
        vo.setPeriod(buildPeriod(y, start, end, monthsCovered, currentYear, today,
                compareStart, compareEnd, compareSamePeriod));

        List<AnnualOverviewVO.MonthItem> monthly = buildMonthly(y, currentYear, today,
                currentExpenses, currentIncomes, prevExpenses, prevIncomes, compareAvailable, catMap);
        vo.setMonthly(monthly);
        setHighlights(vo, monthly);

        vo.setKpis(buildKpis(income, expense, net, currentExpenses, currentIncomes, end,
                prevIncome, prevExpense, prevNet, compareAvailable, monthsCovered));

        List<AnnualOverviewVO.ScopeItem> scopes = buildScopes(currentExpenses, prevExpenses,
                expense, compareAvailable, monthsCovered, catMap);
        vo.setScopes(scopes);
        vo.setCategories(buildCategories(currentExpenses, prevExpenses, expense,
                compareAvailable, monthsCovered, reportLevel, catMap));
        vo.setIncomeCategories(buildIncomeCategories(currentIncomes, prevIncomes, income,
                compareAvailable, monthsCovered, catMap));
        vo.setNetWorth(buildNetWorth(y, currentYear, today, monthsCovered,
                currentMonths.size(), net, scopes));
        vo.setAnnualBudget(buildAnnualBudget(y, today));
        vo.setMonthlyBudget(buildMonthlyBudget(y, monthsCovered));
        vo.setMerchants(buildMerchants(currentExpenses, expense, catMap));
        vo.setLargestExpense(buildLargestExpense(currentExpenses, catMap));
        vo.setDataStates(buildDataStates(vo, income, expense, currentExpenses,
                currentIncomes, currentMonths, compareAvailable, compareInsufficient));
        vo.setInsights(buildInsights(vo, compareAvailable, compareInsufficient));
        return vo;
    }

    private AnnualOverviewVO.Period buildPeriod(int year, LocalDate start, LocalDate end,
                                                int monthsCovered, boolean currentYear,
                                                LocalDate today, LocalDate compareStart,
                                                LocalDate compareEnd, boolean compareSamePeriod) {
        AnnualOverviewVO.Period period = new AnnualOverviewVO.Period();
        period.setYear(year);
        period.setLabel(year + "年");
        period.setStartDate(start.toString());
        period.setEndDate(end.toString());
        period.setMonthsCovered(monthsCovered);
        period.setPartialYear(currentYear);
        period.setPartialMonth(currentYear && today.getDayOfMonth() < today.lengthOfMonth());
        period.setDaysCovered(currentYear ? today.getDayOfYear()
                : (Year.isLeap(year) ? 366 : 365));
        period.setCompareYear(year - 1);
        period.setCompareSamePeriod(compareSamePeriod);
        period.setCompareStartDate(compareStart.toString());
        period.setCompareEndDate(compareEnd.toString());
        period.setCompareLabel(compareSamePeriod
                ? (year - 1) + "年同期（1–" + monthsCovered + "月）"
                : (year - 1) + "年");
        return period;
    }

    private List<AnnualOverviewVO.MonthItem> buildMonthly(
            int year, boolean currentYear, LocalDate today,
            List<Transaction> currentExpenses, List<Transaction> currentIncomes,
            List<Transaction> prevExpenses, List<Transaction> prevIncomes,
            boolean compareAvailable, Map<Long, Category> catMap) {
        List<AnnualOverviewVO.MonthItem> list = new ArrayList<>(12);
        BigDecimal cumulative = BigDecimal.ZERO;
        for (int m = 1; m <= 12; m++) {
            AnnualOverviewVO.MonthItem item = new AnnualOverviewVO.MonthItem();
            item.setMonth(YearMonth.of(year, m).toString());
            item.setLabel(m + "月");

            if (currentYear && m > today.getMonthValue()) {
                item.setFuture(true);
                item.setCumulativeNet(scale2(cumulative));
                list.add(item);
                continue;
            }

            YearMonth ym = YearMonth.of(year, m);
            BigDecimal monthIncome = BigDecimal.ZERO;
            BigDecimal monthExpense = BigDecimal.ZERO;
            BigDecimal daily = BigDecimal.ZERO;
            BigDecimal fixed = BigDecimal.ZERO;
            BigDecimal special = BigDecimal.ZERO;
            boolean hasData = false;

            for (Transaction tx : currentIncomes) {
                if (ym.equals(YearMonth.from(tx.getTransactionDate()))) {
                    monthIncome = monthIncome.add(nz(tx.getAmountBase()));
                    hasData = true;
                }
            }
            for (Transaction tx : currentExpenses) {
                if (!ym.equals(YearMonth.from(tx.getTransactionDate()))) {
                    continue;
                }
                BigDecimal delta = netDelta(tx);
                monthExpense = monthExpense.add(delta);
                hasData = true;
                String scope = scopeOf(tx, catMap);
                if ("FIXED".equals(scope)) {
                    fixed = fixed.add(delta);
                } else if ("SPECIAL".equals(scope)) {
                    special = special.add(delta);
                } else {
                    daily = daily.add(delta);
                }
            }

            BigDecimal monthNet = monthIncome.subtract(monthExpense);
            cumulative = cumulative.add(monthNet);
            item.setIncome(scale2(monthIncome));
            item.setExpense(scale2(monthExpense));
            item.setNet(scale2(monthNet));
            item.setSavingsRate(monthIncome.signum() == 0 ? BigDecimal.ZERO
                    : scale1(monthNet.divide(monthIncome, 4, RoundingMode.HALF_UP)
                    .multiply(ONE_HUNDRED)));
            item.setDaily(scale2(daily));
            item.setFixed(scale2(fixed));
            item.setSpecial(scale2(special));
            item.setCumulativeNet(scale2(cumulative));
            item.setHasData(hasData);

            if (compareAvailable) {
                YearMonth prevYm = YearMonth.of(year - 1, m);
                BigDecimal prevIncome = BigDecimal.ZERO;
                BigDecimal prevExpense = BigDecimal.ZERO;
                for (Transaction tx : prevIncomes) {
                    if (prevYm.equals(YearMonth.from(tx.getTransactionDate()))) {
                        prevIncome = prevIncome.add(nz(tx.getAmountBase()));
                    }
                }
                for (Transaction tx : prevExpenses) {
                    if (prevYm.equals(YearMonth.from(tx.getTransactionDate()))) {
                        prevExpense = prevExpense.add(netDelta(tx));
                    }
                }
                item.setPrevIncome(scale2(prevIncome));
                item.setPrevExpense(scale2(prevExpense));
            }
            list.add(item);
        }
        return list;
    }

    private void setHighlights(AnnualOverviewVO vo, List<AnnualOverviewVO.MonthItem> monthly) {
        List<AnnualOverviewVO.MonthItem> candidates = monthly.stream()
                .filter(m -> !m.isFuture() && m.isHasData())
                .toList();
        if (candidates.isEmpty()) {
            return;
        }
        vo.setBestMonth(highlight(candidates.stream()
                .max(Comparator.comparing(AnnualOverviewVO.MonthItem::getNet))
                .orElse(null)));
        vo.setWorstMonth(highlight(candidates.stream()
                .min(Comparator.comparing(AnnualOverviewVO.MonthItem::getNet))
                .orElse(null)));
    }

    private AnnualOverviewVO.MonthHighlight highlight(AnnualOverviewVO.MonthItem item) {
        if (item == null) {
            return null;
        }
        AnnualOverviewVO.MonthHighlight highlight = new AnnualOverviewVO.MonthHighlight();
        highlight.setMonth(item.getMonth());
        highlight.setLabel(item.getLabel());
        highlight.setNet(item.getNet());
        highlight.setIncome(item.getIncome());
        highlight.setExpense(item.getExpense());
        return highlight;
    }

    private AnnualOverviewVO.Kpis buildKpis(BigDecimal income, BigDecimal expense, BigDecimal net,
                                            List<Transaction> currentExpenses,
                                            List<Transaction> currentIncomes, LocalDate end,
                                            BigDecimal prevIncome, BigDecimal prevExpense,
                                            BigDecimal prevNet, boolean compareAvailable,
                                            int monthsCovered) {
        AnnualOverviewVO.Kpis kpis = new AnnualOverviewVO.Kpis();
        kpis.setIncome(scale2(income));
        kpis.setExpense(scale2(expense));
        kpis.setNet(scale2(net));
        kpis.setSavingsRate(income.signum() == 0 ? BigDecimal.ZERO
                : scale1(net.divide(income, 4, RoundingMode.HALF_UP).multiply(ONE_HUNDRED)));
        kpis.setAvgMonthlyIncome(scale2(income.divide(
                BigDecimal.valueOf(monthsCovered), 4, RoundingMode.HALF_UP)));
        kpis.setAvgMonthlyExpense(scale2(expense.divide(
                BigDecimal.valueOf(monthsCovered), 4, RoundingMode.HALF_UP)));
        kpis.setAvgMonthlyNet(scale2(net.divide(
                BigDecimal.valueOf(monthsCovered), 4, RoundingMode.HALF_UP)));
        kpis.setTransactionCount(currentExpenses.size() + currentIncomes.size());

        Set<LocalDate> spendingDates = new HashSet<>();
        for (Transaction tx : currentExpenses) {
            if (tx.getType() != null && tx.getType() == 1 && tx.getTransactionDate() != null) {
                spendingDates.add(tx.getTransactionDate());
            }
        }
        kpis.setSpendingDays(spendingDates.size());

        LocalDate firstDate = null;
        for (Transaction tx : currentExpenses) {
            firstDate = earlier(firstDate, tx.getTransactionDate());
        }
        for (Transaction tx : currentIncomes) {
            firstDate = earlier(firstDate, tx.getTransactionDate());
        }
        int activityDays = firstDate == null ? 0
                : (int) ChronoUnit.DAYS.between(firstDate, end) + 1;
        activityDays = Math.max(activityDays, 0);
        kpis.setActivityDays(activityDays);
        kpis.setNoSpendDays(Math.max(0, activityDays - spendingDates.size()));

        if (compareAvailable) {
            kpis.setPrevIncome(scale2(prevIncome));
            kpis.setPrevExpense(scale2(prevExpense));
            kpis.setPrevNet(scale2(prevNet));
            kpis.setIncomeChangePct(changePct(income, prevIncome));
            kpis.setExpenseChangePct(changePct(expense, prevExpense));
            kpis.setNetChangePct(prevNet.signum() > 0 ? changePct(net, prevNet) : null);
            if (prevIncome.signum() > 0) {
                BigDecimal prevRate = scale1(prevNet.divide(prevIncome, 4, RoundingMode.HALF_UP)
                        .multiply(ONE_HUNDRED));
                kpis.setPrevSavingsRate(prevRate);
                kpis.setSavingsRateChangePp(scale1(kpis.getSavingsRate().subtract(prevRate)));
            }
        }
        return kpis;
    }

    private List<AnnualOverviewVO.ScopeItem> buildScopes(
            List<Transaction> currentExpenses, List<Transaction> prevExpenses,
            BigDecimal expenseTotal, boolean compareAvailable, int monthsCovered,
            Map<Long, Category> catMap) {
        Map<String, AnnualOverviewVO.ScopeItem> map = new LinkedHashMap<>();
        for (String scope : SCOPE_ORDER) {
            AnnualOverviewVO.ScopeItem item = new AnnualOverviewVO.ScopeItem();
            item.setScope(scope);
            map.put(scope, item);
        }
        for (Transaction tx : currentExpenses) {
            AnnualOverviewVO.ScopeItem item = map.get(scopeOf(tx, catMap));
            if (item == null) {
                continue;
            }
            item.setAmount(item.getAmount().add(netDelta(tx)));
            if (tx.getType() != null && tx.getType() == 1) {
                item.setCount(item.getCount() + 1);
            }
        }
        Map<String, BigDecimal> prevAmounts = new HashMap<>();
        if (compareAvailable) {
            for (Transaction tx : prevExpenses) {
                prevAmounts.merge(scopeOf(tx, catMap), netDelta(tx), BigDecimal::add);
            }
        }
        for (AnnualOverviewVO.ScopeItem item : map.values()) {
            item.setAmount(scale2(item.getAmount()));
            item.setShare(sharePct(item.getAmount(), expenseTotal));
            item.setAvgPerMonth(scale2(item.getAmount().divide(
                    BigDecimal.valueOf(monthsCovered), 4, RoundingMode.HALF_UP)));
            if (compareAvailable) {
                BigDecimal prev = scale2(prevAmounts.getOrDefault(item.getScope(), BigDecimal.ZERO));
                item.setPrevAmount(prev);
                item.setDeltaPct(prev.signum() > 0 ? changePct(item.getAmount(), prev) : null);
            }
        }
        return new ArrayList<>(map.values());
    }

    private List<AnnualOverviewVO.CategoryItem> buildCategories(
            List<Transaction> currentExpenses, List<Transaction> prevExpenses,
            BigDecimal expenseTotal, boolean compareAvailable, int monthsCovered,
            int level, Map<Long, Category> catMap) {
        Map<Long, CategoryAgg> current = groupCategories(currentExpenses, catMap, level);
        Map<Long, CategoryAgg> prev = compareAvailable
                ? groupCategories(prevExpenses, catMap, level) : Map.of();

        List<AnnualOverviewVO.CategoryItem> items = new ArrayList<>();
        for (Map.Entry<Long, CategoryAgg> entry : current.entrySet()) {
            CategoryAgg agg = entry.getValue();
            if (agg.amount.compareTo(MIN_AMOUNT) <= 0) {
                continue;
            }
            Long groupId = entry.getKey();
            Category display = catMap.get(groupId);
            AnnualOverviewVO.CategoryItem item = new AnnualOverviewVO.CategoryItem();
            item.setCategoryId(groupId);
            item.setName(display == null ? "已删除分类" : display.getName());
            item.setColor(display == null ? "#98a2b3" : display.getColor());
            item.setAmount(scale2(agg.amount));
            item.setShare(sharePct(agg.amount, expenseTotal));
            item.setCount(agg.count);
            item.setAvgPerMonth(scale2(agg.amount.divide(
                    BigDecimal.valueOf(monthsCovered), 4, RoundingMode.HALF_UP)));
            item.setScope(dominantScope(agg.scopeAmounts));

            if (level == 2 && display != null) {
                item.setParentId(display.getParentId());
                Category parent = display.getParentId() == null
                        ? null : catMap.get(display.getParentId());
                item.setParentName(parent == null ? null : parent.getName());
            }
            if (compareAvailable) {
                CategoryAgg prevAgg = prev.get(groupId);
                BigDecimal prevAmount = prevAgg == null
                        ? BigDecimal.ZERO : scale2(prevAgg.amount);
                item.setPrevAmount(prevAmount);
                item.setDeltaPct(prevAmount.signum() > 0
                        ? changePct(item.getAmount(), prevAmount) : null);
            }
            items.add(item);
        }
        items.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return items;
    }

    private List<AnnualOverviewVO.IncomeCategoryItem> buildIncomeCategories(
            List<Transaction> currentIncomes, List<Transaction> prevIncomes,
            BigDecimal incomeTotal, boolean compareAvailable, int monthsCovered,
            Map<Long, Category> catMap) {
        Map<Long, CategoryAgg> current = groupIncomeCategories(currentIncomes, catMap);
        Map<Long, CategoryAgg> prev = compareAvailable
                ? groupIncomeCategories(prevIncomes, catMap) : Map.of();

        List<AnnualOverviewVO.IncomeCategoryItem> items = new ArrayList<>();
        for (Map.Entry<Long, CategoryAgg> entry : current.entrySet()) {
            CategoryAgg agg = entry.getValue();
            if (agg.amount.compareTo(MIN_AMOUNT) <= 0) {
                continue;
            }
            Long groupId = entry.getKey();
            Category category = groupId == 0L ? null : catMap.get(groupId);
            AnnualOverviewVO.IncomeCategoryItem item = new AnnualOverviewVO.IncomeCategoryItem();
            item.setCategoryId(category == null ? null : groupId);
            item.setName(category == null ? "未分类" : category.getName());
            item.setColor(category == null ? "#98a2b3" : category.getColor());
            item.setAmount(scale2(agg.amount));
            item.setShare(sharePct(agg.amount, incomeTotal));
            item.setCount(agg.count);
            item.setAvgPerMonth(scale2(agg.amount.divide(
                    BigDecimal.valueOf(monthsCovered), 4, RoundingMode.HALF_UP)));
            if (compareAvailable) {
                CategoryAgg prevAgg = prev.get(groupId);
                BigDecimal prevAmount = prevAgg == null
                        ? BigDecimal.ZERO : scale2(prevAgg.amount);
                item.setPrevAmount(prevAmount);
                item.setDeltaPct(prevAmount.signum() > 0
                        ? changePct(item.getAmount(), prevAmount) : null);
            }
            items.add(item);
        }
        items.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return items.stream().limit(10).toList();
    }

    private Map<Long, CategoryAgg> groupCategories(List<Transaction> txs,
                                                   Map<Long, Category> catMap, int level) {
        Map<Long, CategoryAgg> map = new LinkedHashMap<>();
        for (Transaction tx : txs) {
            if (tx.getCategoryId() == null) {
                continue;
            }
            Category category = catMap.get(tx.getCategoryId());
            if (category == null) {
                continue;
            }
            Long groupId = category.getId();
            if (level == 1) {
                Long parentId = topParentId(category);
                if (catMap.containsKey(parentId)) {
                    groupId = parentId;
                }
            }
            CategoryAgg agg = map.computeIfAbsent(groupId, k -> new CategoryAgg());
            BigDecimal delta = netDelta(tx);
            agg.amount = agg.amount.add(delta);
            if (tx.getType() != null && tx.getType() == 1) {
                agg.count++;
            }
            agg.scopeAmounts.merge(scopeOf(tx, catMap), delta, BigDecimal::add);
        }
        return map;
    }

    private Map<Long, CategoryAgg> groupIncomeCategories(List<Transaction> txs,
                                                         Map<Long, Category> catMap) {
        Map<Long, CategoryAgg> map = new LinkedHashMap<>();
        for (Transaction tx : txs) {
            Category category = tx.getCategoryId() == null
                    ? null : catMap.get(tx.getCategoryId());
            Long groupId;
            if (category == null) {
                groupId = 0L;
            } else {
                groupId = category.getId();
                Long parentId = topParentId(category);
                if (catMap.containsKey(parentId)) {
                    groupId = parentId;
                }
            }
            CategoryAgg agg = map.computeIfAbsent(groupId, k -> new CategoryAgg());
            agg.amount = agg.amount.add(nz(tx.getAmountBase()));
            agg.count++;
        }
        return map;
    }

    private String dominantScope(Map<String, BigDecimal> scopeAmounts) {
        return scopeAmounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("DAILY");
    }

    private AnnualOverviewVO.NetWorthBlock buildNetWorth(
            int year, boolean currentYear, LocalDate today, int monthsCovered,
            int monthsWithData, BigDecimal annualNet, List<AnnualOverviewVO.ScopeItem> scopes) {
        AnnualOverviewVO.NetWorthBlock block = new AnnualOverviewVO.NetWorthBlock();
        block.setAnnualNet(scale2(annualNet));

        YearMonth endMonth = currentYear ? YearMonth.from(today) : YearMonth.of(year, 12);
        YearMonth startMonth = endMonth.minusMonths(monthsCovered);
        int span = monthsCovered + 1;

        Long userId = getUserId();
        List<ReportOverviewVO.AccountBlock.NetWorthItem> raw = userId == null
                ? List.of() : accountSnapshotService.netWorthTrend(userId, endMonth, span);
        BigDecimal startNet = BigDecimal.ZERO;
        BigDecimal endNet = BigDecimal.ZERO;
        if (raw != null && !raw.isEmpty()) {
            List<AnnualOverviewVO.NetWorthBlock.Point> points = new ArrayList<>();
            for (ReportOverviewVO.AccountBlock.NetWorthItem item : raw) {
                AnnualOverviewVO.NetWorthBlock.Point point = new AnnualOverviewVO.NetWorthBlock.Point();
                point.setMonth(item.getMonth());
                point.setNetWorth(scale2(item.getNetWorth()));
                points.add(point);
            }
            block.setTrend(points);
            block.setStartMonth(raw.get(0).getMonth());
            block.setStartLabel(monthLabel(raw.get(0).getMonth()));
            block.setStartNetWorth(scale2(raw.get(0).getNetWorth()));
            block.setEndMonth(raw.get(raw.size() - 1).getMonth());
            block.setEndLabel(monthLabel(raw.get(raw.size() - 1).getMonth()));
            block.setEndNetWorth(scale2(raw.get(raw.size() - 1).getNetWorth()));
            startNet = nz(raw.get(0).getNetWorth());
            endNet = nz(raw.get(raw.size() - 1).getNetWorth());
            block.setChange(scale2(endNet.subtract(startNet)));
            block.setNetWorthAvailable(raw.size() >= 2
                    && (startNet.signum() != 0 || endNet.signum() != 0));
            if (startNet.signum() > 0) {
                block.setChangePct(changePct(endNet, startNet));
            }
            if (startNet.signum() != 0 && raw.size() >= 2 && monthsWithData >= 3) {
                block.setOtherChange(scale2(endNet.subtract(startNet).subtract(annualNet)));
            }
        } else {
            block.setStartMonth(startMonth.toString());
            block.setStartLabel(monthLabel(startMonth.toString()));
            block.setEndMonth(endMonth.toString());
            block.setEndLabel(monthLabel(endMonth.toString()));
        }

        BigDecimal essential = BigDecimal.ZERO;
        for (AnnualOverviewVO.ScopeItem scope : scopes) {
            if ("DAILY".equals(scope.getScope()) || "FIXED".equals(scope.getScope())) {
                essential = essential.add(scope.getAmount());
            }
        }
        BigDecimal essentialMonthly = scale2(essential.divide(
                BigDecimal.valueOf(Math.max(monthsCovered, 1)), 4, RoundingMode.HALF_UP));
        block.setEssentialMonthlyExpense(essentialMonthly);

        if (userId != null) {
            try {
                NetWorthSnapshotVO detail = accountSnapshotService.snapshotDetail(userId, endMonth);
                if (detail != null && detail.getAccounts() != null) {
                    BigDecimal assets = BigDecimal.ZERO;
                    BigDecimal liabilities = BigDecimal.ZERO;
                    BigDecimal liquid = BigDecimal.ZERO;
                    Map<String, BigDecimal> groupSums = new HashMap<>();
                    for (NetWorthSnapshotVO.Row row : detail.getAccounts()) {
                        BigDecimal balance = nz(row.getBalance());
                        if (balance.signum() >= 0) {
                            assets = assets.add(balance);
                        } else {
                            liabilities = liabilities.add(balance.abs());
                        }
                        if (balance.signum() > 0 && LIQUID_ACCOUNT_TYPES.contains(row.getType())) {
                            liquid = liquid.add(balance);
                        }
                        String type = row.getType() == null ? "未知" : row.getType();
                        groupSums.merge(type, balance, BigDecimal::add);
                    }
                    block.setAssets(scale2(assets));
                    block.setLiabilities(scale2(liabilities));
                    block.setLiquidAssets(scale2(liquid));
                    block.setGroups(orderedGroups(groupSums));
                    if (essentialMonthly.signum() > 0) {
                        block.setEmergencyFundMonths(scale1(liquid.divide(
                                essentialMonthly, 4, RoundingMode.HALF_UP)));
                    }
                }
            } catch (Exception e) {
                log.warn("Load annual net worth breakdown failed for {}: {}", year, e.getMessage());
            }
        }
        return block;
    }

    private List<AnnualOverviewVO.NetWorthBlock.Group> orderedGroups(Map<String, BigDecimal> sums) {
        return sums.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> {
                    int index = ACCOUNT_TYPE_ORDER.indexOf(e.getKey());
                    return index < 0 ? 99 : index;
                }))
                .map(e -> {
                    AnnualOverviewVO.NetWorthBlock.Group group =
                            new AnnualOverviewVO.NetWorthBlock.Group();
                    group.setType(e.getKey());
                    group.setBalance(scale2(e.getValue()));
                    return group;
                })
                .toList();
    }

    private AnnualOverviewVO.AnnualBudgetBlock buildAnnualBudget(int year, LocalDate today) {
        AnnualOverviewVO.AnnualBudgetBlock block = new AnnualOverviewVO.AnnualBudgetBlock();
        block.setYear(year);
        block.setTimePct(annualTimePct(year, today));
        if (!annualBudgetService.exists(String.valueOf(year))) {
            return block;
        }
        block.setExists(true);
        try {
            AnnualBudgetProgressVO progress = annualBudgetService.getProgress(String.valueOf(year));
            if (progress == null) {
                return block;
            }
            block.setTotal(scale2(progress.getTotalBudget()));
            block.setSpent(scale2(progress.getTotalSpent()));
            block.setRemaining(scale2(progress.getRemaining()));
            block.setPct(scale1(progress.getProgressPct()));
            List<AnnualOverviewVO.AnnualBudgetBlock.Item> items = new ArrayList<>();
            if (progress.getItems() != null) {
                for (AnnualBudgetProgressVO.ItemProgress bi : progress.getItems()) {
                    AnnualOverviewVO.AnnualBudgetBlock.Item item =
                            new AnnualOverviewVO.AnnualBudgetBlock.Item();
                    item.setCategoryId(bi.getCategoryId());
                    item.setName(bi.getCategoryName());
                    item.setAmount(scale2(bi.getBudget()));
                    item.setSpent(scale2(bi.getSpent()));
                    item.setRemaining(scale2(bi.getRemaining()));
                    item.setPct(scale1(bi.getProgressPct()));
                    items.add(item);
                }
            }
            block.setItems(items);
        } catch (Exception e) {
            log.warn("Load annual budget failed for {}: {}", year, e.getMessage());
        }
        return block;
    }

    private AnnualOverviewVO.MonthlyBudgetBlock buildMonthlyBudget(int year, int monthsCovered) {
        AnnualOverviewVO.MonthlyBudgetBlock block = new AnnualOverviewVO.MonthlyBudgetBlock();
        block.setMonthsCovered(monthsCovered);
        BigDecimal budgetTotal = BigDecimal.ZERO;
        BigDecimal spentTotal = BigDecimal.ZERO;
        List<AnnualOverviewVO.MonthlyBudgetBlock.Item> items = new ArrayList<>();
        for (int m = 1; m <= monthsCovered; m++) {
            String ym = YearMonth.of(year, m).toString();
            try {
                if (!budgetService.exists(ym)) {
                    continue;
                }
                BudgetProgressVO progress = budgetService.getProgress(ym);
                if (progress == null) {
                    continue;
                }
                BigDecimal budget = nz(progress.getTotalBudget());
                BigDecimal spent = nz(progress.getTotalSpent());
                AnnualOverviewVO.MonthlyBudgetBlock.Item item =
                        new AnnualOverviewVO.MonthlyBudgetBlock.Item();
                item.setMonth(ym);
                item.setBudget(scale2(budget));
                item.setSpent(scale2(spent));
                item.setPct(progress.getProgressPct() == null
                        ? sharePct(spent, budget) : scale1(progress.getProgressPct()));
                item.setOver(spent.compareTo(budget) > 0);
                items.add(item);
                budgetTotal = budgetTotal.add(budget);
                spentTotal = spentTotal.add(spent);
            } catch (Exception e) {
                log.warn("Load monthly budget failed for {}: {}", ym, e.getMessage());
            }
        }
        block.setItems(items);
        block.setMonthsWithBudget(items.size());
        block.setMonthsOver((int) items.stream().filter(AnnualOverviewVO.MonthlyBudgetBlock.Item::isOver).count());
        block.setBudgetTotal(scale2(budgetTotal));
        block.setSpentTotal(scale2(spentTotal));
        block.setPct(sharePct(spentTotal, budgetTotal));
        block.setExists(!items.isEmpty());
        return block;
    }

    private List<AnnualOverviewVO.MerchantItem> buildMerchants(
            List<Transaction> currentExpenses, BigDecimal expenseTotal,
            Map<Long, Category> catMap) {
        Map<Long, List<Transaction>> grouped = new LinkedHashMap<>();
        for (Transaction tx : currentExpenses) {
            if (tx.getMerchantId() != null) {
                grouped.computeIfAbsent(tx.getMerchantId(), k -> new ArrayList<>()).add(tx);
            }
        }
        if (grouped.isEmpty()) {
            return List.of();
        }
        Map<Long, String> names;
        try {
            List<Merchant> merchants = merchantMapper.selectBatchIds(grouped.keySet());
            names = merchants == null ? Map.of() : merchants.stream()
                    .collect(Collectors.toMap(Merchant::getId,
                            m -> m.getName() == null ? "" : m.getName(), (a, b) -> a));
        } catch (Exception e) {
            log.warn("Load merchant names failed: {}", e.getMessage());
            names = Map.of();
        }

        List<AnnualOverviewVO.MerchantItem> items = new ArrayList<>();
        for (Map.Entry<Long, List<Transaction>> entry : grouped.entrySet()) {
            String name = names.getOrDefault(entry.getKey(), "");
            if (name.isBlank()) {
                continue;
            }
            List<Transaction> txs = entry.getValue();
            BigDecimal amount = netOf(txs);
            if (amount.compareTo(MIN_AMOUNT) <= 0) {
                continue;
            }
            AnnualOverviewVO.MerchantItem item = new AnnualOverviewVO.MerchantItem();
            item.setMerchantId(entry.getKey());
            item.setName(name);
            item.setAmount(scale2(amount));
            item.setCount((int) txs.stream()
                    .filter(t -> t.getType() != null && t.getType() == 1).count());
            item.setShare(sharePct(amount, expenseTotal));
            item.setCategoryName(dominantCategoryName(txs, catMap));
            items.add(item);
        }
        items.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return items.stream().limit(10).toList();
    }

    private String dominantCategoryName(List<Transaction> txs, Map<Long, Category> catMap) {
        Map<String, Integer> counts = new HashMap<>();
        for (Transaction tx : txs) {
            Category category = tx.getCategoryId() == null
                    ? null : catMap.get(tx.getCategoryId());
            if (category != null && category.getName() != null) {
                counts.merge(category.getName(), 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }

    private AnnualOverviewVO.LargestExpense buildLargestExpense(
            List<Transaction> currentExpenses, Map<Long, Category> catMap) {
        Transaction largest = null;
        for (Transaction tx : currentExpenses) {
            if (tx.getType() == null || tx.getType() != 1) {
                continue;
            }
            if (largest == null || nz(tx.getAmountBase()).compareTo(nz(largest.getAmountBase())) > 0) {
                largest = tx;
            }
        }
        if (largest == null) {
            return null;
        }
        AnnualOverviewVO.LargestExpense item = new AnnualOverviewVO.LargestExpense();
        item.setDate(largest.getTransactionDate() == null
                ? null : largest.getTransactionDate().toString());
        item.setAmount(scale2(largest.getAmountBase()));
        item.setNote(largest.getNote());
        Category category = largest.getCategoryId() == null
                ? null : catMap.get(largest.getCategoryId());
        item.setCategoryName(category == null ? null : category.getName());
        if (largest.getMerchantId() != null) {
            try {
                Merchant merchant = merchantMapper.selectById(largest.getMerchantId());
                item.setMerchantName(merchant == null ? null : merchant.getName());
            } catch (Exception e) {
                log.warn("Load largest expense merchant failed: {}", e.getMessage());
            }
        }
        return item;
    }

    private AnnualOverviewVO.DataStates buildDataStates(
            AnnualOverviewVO vo, BigDecimal income, BigDecimal expense,
            List<Transaction> currentExpenses, List<Transaction> currentIncomes,
            Set<YearMonth> currentMonths, boolean compareAvailable,
            boolean compareInsufficient) {
        AnnualOverviewVO.DataStates states = new AnnualOverviewVO.DataStates();
        states.setPartialYear(vo.getPeriod().isPartialYear());
        states.setNoData(income.signum() == 0 && expense.signum() == 0);
        states.setMultiCurrency(currentExpenses.stream()
                .anyMatch(tx -> tx.getCurrency() != null && !"CNY".equals(tx.getCurrency()))
                || currentIncomes.stream()
                .anyMatch(tx -> tx.getCurrency() != null && !"CNY".equals(tx.getCurrency())));
        states.setAnnualBudgetMissing(!vo.getAnnualBudget().isExists());
        states.setMonthlyBudgetMissing(!vo.getMonthlyBudget().isExists());
        states.setMonthsWithData(currentMonths.size());
        states.setCompareAvailable(compareAvailable);
        states.setCompareInsufficient(compareInsufficient);
        states.setNetWorthAvailable(vo.getNetWorth().isNetWorthAvailable());
        return states;
    }

    private List<AnnualOverviewVO.InsightItem> buildInsights(
            AnnualOverviewVO vo, boolean compareAvailable, boolean compareInsufficient) {
        List<AnnualOverviewVO.InsightItem> insights = new ArrayList<>();
        AnnualOverviewVO.Kpis kpis = vo.getKpis();

        if (vo.getDataStates().isNoData()) {
            insights.add(insight("neutral", "本年度暂无流水",
                    "换一个年份，或确认交易是否已入账、已复核。"));
            return insights;
        }

        if (kpis.getIncome().signum() > 0) {
            BigDecimal rate = nz(kpis.getSavingsRate());
            String tone = rate.compareTo(BigDecimal.valueOf(30)) >= 0 ? "good"
                    : rate.compareTo(BigDecimal.ZERO) < 0 ? "bad"
                    : rate.compareTo(BigDecimal.TEN) >= 0 ? "neutral" : "warn";
            String text = "全年收入 ¥" + money(kpis.getIncome())
                    + "，支出 ¥" + money(kpis.getExpense())
                    + "，结余 " + signedMoney(kpis.getNet()) + "。";
            if (kpis.getPrevSavingsRate() != null) {
                text += "较去年同期 " + signedPp(rate.subtract(kpis.getPrevSavingsRate())) + "。";
            }
            insights.add(insight(tone, "储蓄率 " + rate.setScale(1, RoundingMode.HALF_UP) + "%", text));
        }

        if (compareAvailable && kpis.getIncomeChangePct() != null
                && kpis.getExpenseChangePct() != null) {
            BigDecimal incomeChange = kpis.getIncomeChangePct();
            BigDecimal expenseChange = kpis.getExpenseChangePct();
            String tone = expenseChange.compareTo(incomeChange.add(BigDecimal.valueOf(5))) > 0
                    ? "warn" : expenseChange.signum() < 0 ? "good" : "neutral";
            String text = expenseChange.compareTo(incomeChange.add(BigDecimal.valueOf(5))) > 0
                    ? "支出增速快于收入，注意给弹性消费设上限。"
                    : "收支增速基本同步。";
            if (compareInsufficient) {
                text += "去年同期数据不足，同比仅供参考。";
            }
            insights.add(insight(tone,
                    "支出同比 " + signedPct(expenseChange) + "，收入同比 " + signedPct(incomeChange),
                    text));
        }

        AnnualOverviewVO.CategoryItem topChange = null;
        BigDecimal maxDiff = BigDecimal.ZERO;
        for (AnnualOverviewVO.CategoryItem category : vo.getCategories()) {
            if (category.getPrevAmount() == null || category.getPrevAmount().signum() <= 0) {
                continue;
            }
            BigDecimal diff = category.getAmount().subtract(category.getPrevAmount()).abs();
            if (diff.compareTo(maxDiff) > 0) {
                maxDiff = diff;
                topChange = category;
            }
        }
        if (topChange != null && topChange.getDeltaPct() != null) {
            BigDecimal diff = topChange.getAmount().subtract(topChange.getPrevAmount());
            String tone = diff.signum() > 0 ? "warn" : "good";
            String text = "较去年同期" + (diff.signum() > 0 ? "增加 " : "减少 ")
                    + "¥" + money(diff) + "，是变化最大的分类。";
            if (compareInsufficient) {
                text += "去年同期数据不足，同比仅供参考。";
            }
            insights.add(insight(tone,
                    topChange.getName() + " 同比 " + signedPct(topChange.getDeltaPct()), text));
        }

        AnnualOverviewVO.NetWorthBlock netWorth = vo.getNetWorth();
        if (netWorth.isNetWorthAvailable()) {
            BigDecimal change = nz(netWorth.getChange());
            String tone = change.signum() > 0 ? "good" : change.signum() < 0 ? "bad" : "neutral";
            String title = "净资产" + (change.signum() >= 0 ? "增加 " : "减少 ")
                    + "¥" + money(change);
            String text = "从 " + signedMoney(netWorth.getStartNetWorth())
                    + " 到 " + signedMoney(netWorth.getEndNetWorth()) + "。";
            if (netWorth.getOtherChange() != null
                    && netWorth.getOtherChange().abs().compareTo(BigDecimal.ONE) > 0) {
                text += "全年结余 " + signedMoney(netWorth.getAnnualNet())
                        + "，差额 " + signedMoney(netWorth.getOtherChange())
                        + "（投资增值、往来款、余额调整或未记账等）。";
            }
            insights.add(insight(tone, title, text));
        }

        if (netWorth.getEmergencyFundMonths() != null) {
            BigDecimal months = netWorth.getEmergencyFundMonths();
            String tone = months.compareTo(BigDecimal.valueOf(6)) >= 0 ? "good"
                    : months.compareTo(BigDecimal.valueOf(3)) >= 0 ? "warn" : "bad";
            insights.add(insight(tone,
                    "应急金约 " + months.setScale(1, RoundingMode.HALF_UP) + " 个月",
                    "可随时动用 ¥" + money(netWorth.getLiquidAssets())
                            + "，按日常+固定月均 ¥" + money(netWorth.getEssentialMonthlyExpense())
                            + " 估算。"));
        }

        AnnualOverviewVO.AnnualBudgetBlock budget = vo.getAnnualBudget();
        if (budget.isExists()) {
            BigDecimal pct = nz(budget.getPct());
            BigDecimal timePct = nz(budget.getTimePct());
            String tone = pct.compareTo(BigDecimal.valueOf(100)) > 0 ? "bad"
                    : pct.compareTo(timePct.add(BigDecimal.valueOf(5))) > 0 ? "warn" : "good";
            insights.add(insight(tone,
                    "年度预算已用 " + pct.setScale(1, RoundingMode.HALF_UP) + "%",
                    "总预算 ¥" + money(budget.getTotal())
                            + "，已用 ¥" + money(budget.getSpent())
                            + (budget.getRemaining().signum() < 0
                            ? "，超支 ¥" : "，剩余 ¥") + money(budget.getRemaining())
                            + "；时间进度 " + timePct.setScale(1, RoundingMode.HALF_UP) + "%。"));
        } else {
            insights.add(insight("neutral", "未设置年度预算",
                    "年度预算覆盖固定和专项支出，设置后可以对比时间进度。"));
        }
        return insights;
    }

    private AnnualOverviewVO.InsightItem insight(String tone, String title, String text) {
        AnnualOverviewVO.InsightItem item = new AnnualOverviewVO.InsightItem();
        item.setTone(tone);
        item.setTitle(title);
        item.setText(text);
        return item;
    }

    private Map<Long, Category> loadCategoryMap() {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<>());
        if (categories == null) {
            return Map.of();
        }
        return categories.stream().collect(Collectors.toMap(
                Category::getId, c -> c, (a, b) -> a, LinkedHashMap::new));
    }

    private List<Transaction> reportableExpenses(List<Transaction> all, LocalDate start,
                                                 LocalDate end, Map<Long, Category> catMap) {
        return all.stream()
                .filter(t -> inRange(t, start, end))
                .filter(t -> t.getType() != null && (t.getType() == 1 || t.getType() == 4))
                .filter(t -> isReportableExpense(t, catMap))
                .toList();
    }

    private List<Transaction> reportableIncomes(List<Transaction> all, LocalDate start,
                                                LocalDate end, Map<Long, Category> catMap) {
        return all.stream()
                .filter(t -> inRange(t, start, end))
                .filter(t -> t.getType() != null && t.getType() == 2)
                .filter(t -> isReportableIncome(t, catMap))
                .toList();
    }

    private boolean inRange(Transaction tx, LocalDate start, LocalDate end) {
        return tx.getTransactionDate() != null
                && !tx.getTransactionDate().isBefore(start)
                && !tx.getTransactionDate().isAfter(end);
    }

    private boolean isReportableExpense(Transaction tx, Map<Long, Category> categories) {
        if (tx.getCategoryId() == null) {
            return false;
        }
        Category category = categories.get(tx.getCategoryId());
        return category != null && category.getType() != null && category.getType() == 1
                && !"NONE".equals(category.getBudgetType())
                && !Boolean.TRUE.equals(category.getExcludeFromReports());
    }

    private boolean isReportableIncome(Transaction tx, Map<Long, Category> categories) {
        if (tx.getCategoryId() == null) {
            return true;
        }
        Category category = categories.get(tx.getCategoryId());
        return category == null || !Boolean.TRUE.equals(category.getExcludeFromReports());
    }

    private String scopeOf(Transaction tx, Map<Long, Category> catMap) {
        Category category = tx.getCategoryId() == null ? null : catMap.get(tx.getCategoryId());
        if (category == null || category.getBudgetType() == null) {
            return "DAILY";
        }
        if ("FIXED".equals(category.getBudgetType())) {
            return "FIXED";
        }
        if ("SPECIAL".equals(category.getBudgetType())) {
            return "SPECIAL";
        }
        return "DAILY";
    }

    private Long topParentId(Category category) {
        if (category.getParentId() != null && category.getParentId() != 0) {
            return category.getParentId();
        }
        return category.getId();
    }

    private Set<YearMonth> monthsOf(List<Transaction> expenses, List<Transaction> incomes) {
        Set<YearMonth> months = new HashSet<>();
        for (Transaction tx : expenses) {
            if (tx.getTransactionDate() != null) {
                months.add(YearMonth.from(tx.getTransactionDate()));
            }
        }
        for (Transaction tx : incomes) {
            if (tx.getTransactionDate() != null) {
                months.add(YearMonth.from(tx.getTransactionDate()));
            }
        }
        return months;
    }

    private LocalDate earlier(LocalDate current, LocalDate candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.isBefore(current)) {
            return candidate;
        }
        return current;
    }

    private BigDecimal netDelta(Transaction tx) {
        BigDecimal amount = nz(tx.getAmountBase());
        return tx.getType() != null && tx.getType() == 4 ? amount.negate() : amount;
    }

    private BigDecimal netOf(List<Transaction> txs) {
        return txs.stream().map(this::netDelta).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumBase(List<Transaction> txs) {
        return txs.stream().map(t -> nz(t.getAmountBase()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal changePct(BigDecimal current, BigDecimal prev) {
        if (prev == null || prev.signum() <= 0) {
            return null;
        }
        return scale1(current.subtract(prev).divide(prev, 4, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED));
    }

    private BigDecimal sharePct(BigDecimal amount, BigDecimal total) {
        if (total == null || total.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return scale1(amount.divide(total, 4, RoundingMode.HALF_UP).multiply(ONE_HUNDRED));
    }

    private BigDecimal annualTimePct(int year, LocalDate today) {
        if (year < today.getYear()) {
            return scale1(ONE_HUNDRED);
        }
        if (year > today.getYear()) {
            return BigDecimal.ZERO;
        }
        return scale1(BigDecimal.valueOf(today.getDayOfYear())
                .divide(BigDecimal.valueOf(today.lengthOfYear()), 4, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED));
    }

    private String monthLabel(String month) {
        if (month == null || month.isBlank()) {
            return "";
        }
        YearMonth ym = YearMonth.parse(month);
        return ym.getYear() + "年" + ym.getMonthValue() + "月";
    }

    private String money(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value.abs();
        return String.format(Locale.CHINA, "%,.0f", v);
    }

    private String signedMoney(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        return (v.signum() < 0 ? "-¥" : "¥") + money(v);
    }

    private String signedPct(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        BigDecimal v = value.setScale(1, RoundingMode.HALF_UP);
        return (v.signum() > 0 ? "+" : "") + v.toPlainString() + "%";
    }

    private String signedPp(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        BigDecimal v = value.setScale(1, RoundingMode.HALF_UP);
        return (v.signum() > 0 ? "+" : "") + v.toPlainString() + "pp";
    }

    private BigDecimal scale1(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal scale2(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long getUserId() {
        Long tenant = TenantContext.get();
        if (tenant != null) {
            return tenant;
        }
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    private static class CategoryAgg {
        private BigDecimal amount = BigDecimal.ZERO;
        private int count;
        private final Map<String, BigDecimal> scopeAmounts = new HashMap<>();
    }
}