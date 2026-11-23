package uk.gubin.budgetpilot.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Merchant;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.MerchantMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.AccountSnapshotService;
import uk.gubin.budgetpilot.service.AnnualBudgetService;
import uk.gubin.budgetpilot.service.BudgetService;
import uk.gubin.budgetpilot.service.ReportService;
import uk.gubin.budgetpilot.vo.AnnualBudgetProgressVO;
import uk.gubin.budgetpilot.vo.BudgetProgressVO;
import uk.gubin.budgetpilot.vo.NetWorthSnapshotVO;
import uk.gubin.budgetpilot.vo.ReportOverviewVO;
import uk.gubin.budgetpilot.vo.ReportVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final TransactionMapper transactionMapper;
    private final CategoryMapper categoryMapper;
    private final AccountMapper accountMapper;
    private final MerchantMapper merchantMapper;
    private final BudgetService budgetService;
    private final AnnualBudgetService annualBudgetService;
    private final AccountSnapshotService accountSnapshotService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public ReportVO monthlySummary(String month) {
        return monthlySummary(month, 1);
    }

    @Override
    public ReportVO monthlySummary(String month, int level) {
        int reportLevel = level == 2 ? 2 : 1;
        String cacheKey = "report:monthly-summary:" + getUserId() + ":" + month + ":L" + reportLevel;
        ReportVO cached = getCached(cacheKey);
        if (cached != null) return cached;

        ReportVO vo = new ReportVO();
        ReportVO.MonthlySummary summary = new ReportVO.MonthlySummary();
        summary.setMonth(month);

        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // 支出（含退款冲减；排除非预算流水和待复核）
        LambdaQueryWrapper<Transaction> expenseQuery = new LambdaQueryWrapper<>();
        expenseQuery.in(Transaction::getType, 1, 4).eq(Transaction::getIsConfirmed, true)
                .eq(Transaction::getReviewStatus, "REVIEWED")
                .ge(Transaction::getTransactionDate, start).le(Transaction::getTransactionDate, end);
        List<Transaction> expenseTxs = transactionMapper.selectList(expenseQuery);
        Map<Long, Category> catMap = loadCategoryMap();
        List<Transaction> expenses = expenseTxs.stream()
                .filter(tx -> isReportableExpense(tx, catMap))
                .toList();
        BigDecimal totalExpense = netExpense(expenses);
        summary.setTotalExpense(totalExpense);
        summary.setAvgDailyExpense(totalExpense.divide(BigDecimal.valueOf(ym.lengthOfMonth()), 2, RoundingMode.HALF_UP));

        // 收入
        LambdaQueryWrapper<Transaction> incomeQuery = new LambdaQueryWrapper<>();
        incomeQuery.eq(Transaction::getType, 2).eq(Transaction::getIsConfirmed, true)
                .eq(Transaction::getReviewStatus, "REVIEWED")
                .ge(Transaction::getTransactionDate, start).le(Transaction::getTransactionDate, end);
        List<Transaction> incomes = transactionMapper.selectList(incomeQuery).stream()
                .filter(tx -> isReportableIncome(tx, catMap))
                .toList();
        summary.setTotalIncome(sumBase(incomes));
        summary.setBalance(summary.getTotalIncome().subtract(totalExpense));

        // 分类占比：level=1 归并到父分类；level=2 按实际分类展示；退款冲减原分类
        Map<Long, BigDecimal> categoryAmounts = groupCategoryAmounts(expenses, catMap, reportLevel);
        summary.setCategoryShares(buildCategoryShares(categoryAmounts, totalExpense, catMap, reportLevel));

        // 分类占比（剔除固定支出）：固定支出不参与该视图，百分比按剔除后的总额计算
        List<Transaction> nonFixedExpenses = expenses.stream()
                .filter(tx -> {
                    Category cat = catMap.get(tx.getCategoryId());
                    return cat == null || !"FIXED".equals(cat.getBudgetType());
                })
                .toList();
        BigDecimal nonFixedExpense = sumNetExpense(nonFixedExpenses);
        Map<Long, BigDecimal> nonFixedAmounts = groupCategoryAmounts(nonFixedExpenses, catMap, reportLevel);
        summary.setCategorySharesExcludingFixed(
                buildCategoryShares(nonFixedAmounts, nonFixedExpense, catMap, reportLevel));

        summary.setTagShares(buildTagShares(expenses, totalExpense));

        vo.setMonthlySummary(summary);
        putCache(cacheKey, vo, isCurrentMonth(month) ? 1 : 24); // 当前月份缓存1小时，历史月份24小时
        return vo;
    }

    @Override
    public ReportVO yearlySummary(int year, int level) {
        int reportLevel = level == 2 ? 2 : 1;
        String cacheKey = "report:yearly-summary:" + getUserId() + ":" + year + ":L" + reportLevel;
        ReportVO cached = getCached(cacheKey);
        if (cached != null) return cached;

        Map<Long, Category> catMap = loadCategoryMap();
        YearTotals totals = loadYearTotals(year, catMap);

        ReportVO.YearlySummary summary = new ReportVO.YearlySummary();
        summary.setYear(year);
        summary.setTotalIncome(totals.income());
        summary.setTotalExpense(totals.expense());
        summary.setBalance(totals.income().subtract(totals.expense()));

        // 本年按已过月份计算月均，历史年份按 12 个月
        int monthCount = year == LocalDate.now().getYear()
                ? Math.max(1, LocalDate.now().getMonthValue()) : 12;
        summary.setMonthCount(monthCount);
        summary.setAvgMonthlyExpense(totals.expense()
                .divide(BigDecimal.valueOf(monthCount), 2, RoundingMode.HALF_UP));

        summary.setMonths(buildMonthlyTrend(year, totals.transactions(), catMap));

        summary.setCategoryShares(buildCategoryShares(
                groupCategoryAmounts(totals.expenses(), catMap, reportLevel),
                totals.expense(), catMap, reportLevel));

        // 分类占比（剔除固定支出）：与月度视图口径一致
        List<Transaction> nonFixedExpenses = totals.expenses().stream()
                .filter(tx -> {
                    Category cat = catMap.get(tx.getCategoryId());
                    return cat == null || !"FIXED".equals(cat.getBudgetType());
                })
                .toList();
        BigDecimal nonFixedExpense = sumNetExpense(nonFixedExpenses);
        summary.setCategorySharesExcludingFixed(buildCategoryShares(
                groupCategoryAmounts(nonFixedExpenses, catMap, reportLevel),
                nonFixedExpense, catMap, reportLevel));

        summary.setTagShares(buildTagShares(totals.expenses(), totals.expense()));
        summary.setMerchantShares(buildMerchantShares(totals.expenses()));
        summary.setCompare(buildYearlyCompare(year, totals, catMap));

        ReportVO vo = new ReportVO();
        vo.setYearlySummary(summary);
        putCache(cacheKey, vo, year == LocalDate.now().getYear() ? 1 : 24);
        return vo;
    }

    /** 全年可统计交易，按月度视图的口径过滤。 */
    private YearTotals loadYearTotals(int year, Map<Long, Category> catMap) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.in(Transaction::getType, 1, 2, 4).eq(Transaction::getIsConfirmed, true)
                .eq(Transaction::getReviewStatus, "REVIEWED")
                .ge(Transaction::getTransactionDate, start).le(Transaction::getTransactionDate, end);
        List<Transaction> txs = transactionMapper.selectList(query);

        List<Transaction> expenses = txs.stream()
                .filter(tx -> tx.getType() != 2)
                .filter(tx -> isReportableExpense(tx, catMap))
                .toList();
        BigDecimal income = txs.stream()
                .filter(tx -> tx.getType() == 2)
                .filter(tx -> isReportableIncome(tx, catMap))
                .map(tx -> tx.getAmountBase() == null ? BigDecimal.ZERO : tx.getAmountBase())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new YearTotals(income, sumNetExpense(expenses), txs, expenses);
    }

    /** 12 个月的收入/支出/结余，无数据的月份补零。 */
    private List<ReportVO.TrendItem> buildMonthlyTrend(int year, List<Transaction> txs,
                                                       Map<Long, Category> catMap) {
        List<ReportVO.TrendItem> months = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            List<Transaction> monthTxs = txs.stream()
                    .filter(tx -> tx.getTransactionDate() != null
                            && YearMonth.from(tx.getTransactionDate()).equals(ym))
                    .toList();
            BigDecimal income = monthTxs.stream()
                    .filter(tx -> tx.getType() == 2)
                    .filter(tx -> isReportableIncome(tx, catMap))
                    .map(tx -> tx.getAmountBase() == null ? BigDecimal.ZERO : tx.getAmountBase())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expense = sumNetExpense(monthTxs.stream()
                    .filter(tx -> tx.getType() != 2)
                    .filter(tx -> isReportableExpense(tx, catMap))
                    .toList());

            ReportVO.TrendItem item = new ReportVO.TrendItem();
            item.setMonth(ym.toString());
            item.setIncome(income);
            item.setExpense(expense);
            item.setBalance(income.subtract(expense));
            months.add(item);
        }
        return months;
    }

    private ReportVO.YearlyCompare buildYearlyCompare(int year, YearTotals current,
                                                      Map<Long, Category> catMap) {
        YearTotals previous = loadYearTotals(year - 1, catMap);
        ReportVO.YearlyCompare compare = new ReportVO.YearlyCompare();
        compare.setCompareYear(year - 1);
        compare.setCurrentIncome(current.income());
        compare.setCurrentExpense(current.expense());
        compare.setCompareIncome(previous.income());
        compare.setCompareExpense(previous.expense());
        compare.setIncomeChangePct(changePct(current.income(), previous.income()));
        compare.setExpenseChangePct(changePct(current.expense(), previous.expense()));
        return compare;
    }

    private BigDecimal changePct(BigDecimal current, BigDecimal compareWith) {
        if (compareWith == null || compareWith.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(compareWith)
                .divide(compareWith, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
    }

    private List<ReportVO.TagShareItem> buildTagShares(List<Transaction> expenses, BigDecimal totalExpense) {
        // 仅统计支出交易；一笔交易有多个标签时，每个标签各计全额
        Map<String, BigDecimal> tagAmounts = new LinkedHashMap<>();
        Map<String, Integer> tagCounts = new LinkedHashMap<>();
        for (Transaction tx : expenses) {
            if (tx.getType() != 1 || tx.getTags() == null) continue;
            try {
                List<String> tags = objectMapper.readValue(tx.getTags(), new TypeReference<List<String>>() {});
                Set<String> distinct = new LinkedHashSet<>();
                for (String tag : tags) {
                    if (tag != null && !tag.isBlank()) distinct.add(tag.trim());
                }
                for (String tag : distinct) {
                    tagAmounts.merge(tag, tx.getAmountBase(), BigDecimal::add);
                    tagCounts.merge(tag, 1, Integer::sum);
                }
            } catch (Exception ignored) {
                // 忽略历史脏数据
            }
        }
        List<ReportVO.TagShareItem> tagShares = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : tagAmounts.entrySet()) {
            ReportVO.TagShareItem item = new ReportVO.TagShareItem();
            item.setTag(entry.getKey());
            item.setAmount(entry.getValue());
            item.setCount(tagCounts.getOrDefault(entry.getKey(), 0));
            item.setPercentage(totalExpense.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : entry.getValue().divide(totalExpense, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));
            tagShares.add(item);
        }
        tagShares.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return tagShares;
    }

    @Override
    public ReportVO categoryDetail(String month, Long categoryId) {
        YearMonth ym = YearMonth.parse(month);
        return categoryDetail(ym.atDay(1), ym.atEndOfMonth(), categoryId, "ALL");
    }

    @Override
    public ReportVO categoryDetail(LocalDate start, LocalDate end, Long categoryId, String scope) {
        ReportVO vo = new ReportVO();

        List<Long> catIds = new ArrayList<>();
        catIds.add(categoryId);
        catIds.addAll(getChildCategoryIds(categoryId));

        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.eq(Transaction::getIsConfirmed, true)
                .eq(Transaction::getReviewStatus, "REVIEWED")
                .in(Transaction::getType, 1, 4)
                .in(Transaction::getCategoryId, catIds)
                .ge(Transaction::getTransactionDate, start)
                .le(Transaction::getTransactionDate, end)
                .orderByDesc(Transaction::getTransactionDate)
                .last("LIMIT 500");

        List<Transaction> txs = transactionMapper.selectList(query);
        Map<Long, Category> cats = loadCategoryMap();
        String reportScope = scope == null ? "ALL" : scope.trim().toUpperCase(Locale.ROOT);
        List<Transaction> filtered = txs.stream()
                .filter(tx -> "ALL".equals(reportScope) || reportScope.equals(scopeOf(tx, cats)))
                .toList();

        // 批量加载账户，避免 N+1
        List<Long> accIds = filtered.stream().map(Transaction::getAccountId).distinct().toList();
        Map<Long, Account> accMap = accIds.isEmpty() ? Map.of()
                : accountMapper.selectBatchIds(accIds).stream().collect(Collectors.toMap(Account::getId, a -> a));

        List<ReportVO.CategoryDetailItem> items = new ArrayList<>();
        for (Transaction tx : filtered) {
            ReportVO.CategoryDetailItem item = new ReportVO.CategoryDetailItem();
            item.setId(tx.getId());
            item.setType(tx.getType());
            item.setAmount(tx.getAmount());
            item.setCurrency(tx.getCurrency());
            item.setAmountBase(tx.getAmountBase());
            item.setTransactionDate(tx.getTransactionDate().toString());
            item.setNote(tx.getNote());
            Account acc = accMap.get(tx.getAccountId());
            if (acc != null) item.setAccountName(acc.getName());
            if (tx.getTags() != null) {
                try { item.setTags(objectMapper.readValue(tx.getTags(), new TypeReference<>() {})); } catch (Exception ignored) {}
            }
            if (tx.getExtFields() != null) {
                try { item.setExtFields(objectMapper.readValue(tx.getExtFields(), new TypeReference<>() {})); } catch (Exception ignored) {}
            }
            items.add(item);
        }
        vo.setCategoryDetails(items);
        return vo;
    }

    @Override
    public ReportVO trend(int months, Integer type) {
        ReportVO vo = new ReportVO();
        List<ReportVO.TrendItem> trend = new ArrayList<>();
        // 分类树在循环外加载一次，避免每个月都全表读取分类
        Map<Long, Category> trendCats = loadCategoryMap();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            String month = ym.toString();
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
            query.eq(Transaction::getIsConfirmed, true)
                    .eq(Transaction::getReviewStatus, "REVIEWED")
                    .ge(Transaction::getTransactionDate, start)
                    .le(Transaction::getTransactionDate, end);
            if (type != null && type == 1) {
                query.in(Transaction::getType, 1, 4);
            } else if (type != null) {
                query.eq(Transaction::getType, type);
            }

            List<Transaction> txs = transactionMapper.selectList(query);

            BigDecimal income = txs.stream().filter(t -> t.getType() == 2)
                    .filter(t -> isReportableIncome(t, trendCats))
                    .map(Transaction::getAmountBase).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expense = netExpense(txs);

            ReportVO.TrendItem item = new ReportVO.TrendItem();
            item.setMonth(month);
            item.setIncome(income);
            item.setExpense(expense);
            item.setBalance(income.subtract(expense));
            trend.add(item);
        }
        vo.setTrend(trend);
        return vo;
    }

    @Override
    public ReportVO compare(String month, String compareWith) {
        ReportVO vo = new ReportVO();
        ReportVO.CompareItem ci = new ReportVO.CompareItem();
        ci.setCurrentMonth(month);
        ci.setCompareMonth(compareWith);

        MonthData current = getMonthData(month);
        MonthData compare = getMonthData(compareWith);

        ci.setCurrentIncome(current.income);
        ci.setCurrentExpense(current.expense);
        ci.setCompareIncome(compare.income);
        ci.setCompareExpense(compare.expense);

        if (compare.income.compareTo(BigDecimal.ZERO) > 0) {
            ci.setIncomeChangePct(current.income.subtract(compare.income)
                    .divide(compare.income, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));
        }
        if (compare.expense.compareTo(BigDecimal.ZERO) > 0) {
            ci.setExpenseChangePct(current.expense.subtract(compare.expense)
                    .divide(compare.expense, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));
        }

        vo.setCompare(ci);
        return vo;
    }

    @Override
    public ReportVO accountSummary() {
        ReportVO vo = new ReportVO();

        LambdaQueryWrapper<Account> accQuery = new LambdaQueryWrapper<>();
        accQuery.eq(Account::getIsActive, true);
        List<Account> accounts = accountMapper.selectList(accQuery);

        YearMonth ym = YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<ReportVO.AccountSummaryItem> items = new ArrayList<>();
        Map<Long, Category> accountCats = loadCategoryMap();
        for (Account acc : accounts) {
            ReportVO.AccountSummaryItem item = new ReportVO.AccountSummaryItem();
            item.setAccountId(acc.getId());
            item.setAccountName(acc.getName());
            item.setAccountType(accountTypeLabel(acc.getType()));
            item.setCurrency(acc.getCurrency());
            item.setBalance(acc.getCurrentBalance());

            LambdaQueryWrapper<Transaction> incomeQ = new LambdaQueryWrapper<>();
            incomeQ.eq(Transaction::getType, 2).eq(Transaction::getAccountId, acc.getId())
                    .eq(Transaction::getIsConfirmed, true)
                    .eq(Transaction::getReviewStatus, "REVIEWED")
                    .ge(Transaction::getTransactionDate, start).le(Transaction::getTransactionDate, end);
            item.setMonthIncome(transactionMapper.selectList(incomeQ).stream()
                    .filter(tx -> isReportableIncome(tx, accountCats))
                    .map(Transaction::getAmountBase)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            LambdaQueryWrapper<Transaction> expenseQ = new LambdaQueryWrapper<>();
            expenseQ.in(Transaction::getType, 1, 4).eq(Transaction::getAccountId, acc.getId())
                    .eq(Transaction::getIsConfirmed, true)
                    .eq(Transaction::getReviewStatus, "REVIEWED")
                    .ge(Transaction::getTransactionDate, start).le(Transaction::getTransactionDate, end);
            item.setMonthExpense(netExpense(transactionMapper.selectList(expenseQ)));

            items.add(item);
        }
        vo.setAccountSummary(items);
        return vo;
    }

    @Override
    public ReportVO dailyHeatmap(int year) {
        ReportVO vo = new ReportVO();
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.in(Transaction::getType, 1, 4).eq(Transaction::getIsConfirmed, true)
                .eq(Transaction::getReviewStatus, "REVIEWED")
                .ge(Transaction::getTransactionDate, start).le(Transaction::getTransactionDate, end);

        Map<Long, Category> heatmapCats = loadCategoryMap();
        List<Transaction> txs = transactionMapper.selectList(query).stream()
                .filter(tx -> isReportableExpense(tx, heatmapCats))
                .toList();
        Map<LocalDate, List<Transaction>> byDate = txs.stream()
                .collect(Collectors.groupingBy(Transaction::getTransactionDate));

        List<ReportVO.HeatmapItem> items = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Transaction>> entry : byDate.entrySet()) {
            ReportVO.HeatmapItem item = new ReportVO.HeatmapItem();
            item.setDate(entry.getKey().format(DateTimeFormatter.ISO_LOCAL_DATE));
            item.setAmount(netExpense(entry.getValue()));
            item.setCount(entry.getValue().size());
            items.add(item);
        }
        vo.setHeatmap(items);
        return vo;
    }

    @Override
    public ReportVO budgetReview(String month) {
        ReportVO vo = new ReportVO();
        // Delegate to monthly summary for now - this can be extended with budget comparison
        vo.setMonthlySummary(monthlySummary(month).getMonthlySummary());
        return vo;
    }

    @Override
    public ReportVO currencyDistribution(String month) {
        ReportVO vo = new ReportVO();

        YearMonth ym = YearMonth.parse(month);
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.in(Transaction::getType, 1, 4).eq(Transaction::getIsConfirmed, true)
                .eq(Transaction::getReviewStatus, "REVIEWED")
                .ge(Transaction::getTransactionDate, ym.atDay(1))
                .le(Transaction::getTransactionDate, ym.atEndOfMonth());

        Map<Long, Category> catMap = loadCategoryMap();
        List<Transaction> txs = transactionMapper.selectList(query).stream()
                .filter(tx -> isReportableExpense(tx, catMap))
                .toList();
        Map<String, List<Transaction>> byCurrency = txs.stream()
                .collect(Collectors.groupingBy(Transaction::getCurrency));

        BigDecimal grandTotal = netExpense(txs);
        List<ReportVO.CurrencyItem> items = new ArrayList<>();
        for (Map.Entry<String, List<Transaction>> entry : byCurrency.entrySet()) {
            ReportVO.CurrencyItem item = new ReportVO.CurrencyItem();
            item.setCurrency(entry.getKey());
            BigDecimal totalOriginal = entry.getValue().stream()
                    .map(tx -> tx.getType() == 4 ? tx.getAmount().negate() : tx.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalBase = netExpense(entry.getValue());
            item.setTotalAmount(totalOriginal);
            item.setTotalBase(totalBase);
            item.setPercentage(grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? totalBase.divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            items.add(item);
        }
        vo.setCurrencyDistribution(items);
        return vo;
    }

    @Override
    public List<ReportVO.ExtFieldShareItem> extFieldDistribution(String month, String key, int type) {
        YearMonth ym = YearMonth.parse(month);
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.eq(Transaction::getIsConfirmed, true)
                .eq(Transaction::getReviewStatus, "REVIEWED")
                .eq(Transaction::getType, type)
                .ge(Transaction::getTransactionDate, ym.atDay(1))
                .le(Transaction::getTransactionDate, ym.atEndOfMonth());
        Map<Long, Category> extCats = loadCategoryMap();
        List<Transaction> txs = transactionMapper.selectList(query).stream()
                .filter(tx -> type != 2 || isReportableIncome(tx, extCats))
                .toList();
        BigDecimal total = sumBase(txs);

        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Transaction tx : txs) {
            String label = "未标注";
            if (tx.getExtFields() != null) {
                try {
                    Map<String, Object> ext = objectMapper.readValue(
                            tx.getExtFields(), new TypeReference<Map<String, Object>>() {});
                    Object value = ext.get(key);
                    if (value != null && !String.valueOf(value).isBlank()) {
                        label = String.valueOf(value);
                    }
                } catch (Exception ignored) {
                    // 忽略历史脏数据
                }
            }
            amounts.merge(label, tx.getAmountBase() == null ? BigDecimal.ZERO : tx.getAmountBase(), BigDecimal::add);
            counts.merge(label, 1, Integer::sum);
        }

        List<ReportVO.ExtFieldShareItem> items = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : amounts.entrySet()) {
            ReportVO.ExtFieldShareItem item = new ReportVO.ExtFieldShareItem();
            item.setValue(entry.getKey());
            item.setAmount(entry.getValue());
            item.setCount(counts.getOrDefault(entry.getKey(), 0));
            item.setPercentage(total.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : entry.getValue().divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));
            items.add(item);
        }
        items.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return items;
    }

    // ==================== 报表聚合（月度复盘） ====================

    @Override
    public ReportOverviewVO overview(String startDate, String endDate,
                                     String compare, String scope, int level) {
        LocalDate start = parseReportDate(startDate, "startDate");
        LocalDate end = parseReportDate(endDate, "endDate");
        if (end.isBefore(start)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }
        String cmp = compare == null ? "PREV" : compare.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("PREV", "YOY", "NONE").contains(cmp)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "compare 只能为 PREV、YOY 或 NONE");
        }
        String reportScope = scope == null ? "ALL" : scope.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ALL", "DAILY", "FIXED", "SPECIAL").contains(reportScope)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "scope 只能为 ALL、DAILY、FIXED 或 SPECIAL");
        }
        int reportLevel = level == 2 ? 2 : 1;
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        YearMonth endMonth = YearMonth.from(end);
        YearMonth currentMonth = YearMonth.now();

        YearMonth startMonth = YearMonth.from(start);
        boolean fullMonthPeriod = start.equals(startMonth.atDay(1))
                && end.equals(startMonth.atEndOfMonth());
        LocalDate cmpStart = null;
        LocalDate cmpEnd = null;
        if ("PREV".equals(cmp)) {
            if (fullMonthPeriod) {
                // 整月报表按“上一个自然月”对比，符合“环比上月”的直觉
                YearMonth prevMonth = startMonth.minusMonths(1);
                cmpStart = prevMonth.atDay(1);
                cmpEnd = prevMonth.atEndOfMonth();
            } else {
                cmpEnd = start.minusDays(1);
                cmpStart = cmpEnd.minusDays(days - 1);
            }
        } else if ("YOY".equals(cmp)) {
            cmpStart = start.minusYears(1);
            cmpEnd = end.minusYears(1);
        }

        YearMonth trendStartMonth = endMonth.minusMonths(11);
        LocalDate queryStart = trendStartMonth.atDay(1);
        if (cmpStart != null && cmpStart.isBefore(queryStart)) {
            queryStart = cmpStart;
        }
        LocalDate queryEnd = end;
        if (cmpEnd != null && cmpEnd.isAfter(queryEnd)) {
            queryEnd = cmpEnd;
        }

        List<Transaction> all = transactionMapper.selectList(
                new LambdaQueryWrapper<Transaction>()
                        .eq(Transaction::getIsConfirmed, true)
                        .eq(Transaction::getReviewStatus, "REVIEWED")
                        .ge(Transaction::getTransactionDate, queryStart)
                        .le(Transaction::getTransactionDate, queryEnd));
        Map<Long, Category> catMap = loadCategoryMap();

        List<Transaction> currentExpenses = reportableExpenses(all, start, end, catMap);
        List<Transaction> currentIncomes = reportableIncomes(all, start, end, catMap);
        List<Transaction> prevExpenses = cmpStart == null ? null
                : reportableExpenses(all, cmpStart, cmpEnd, catMap);
        List<Transaction> prevIncomes = cmpStart == null ? null
                : reportableIncomes(all, cmpStart, cmpEnd, catMap);

        BigDecimal expenseAll = netOf(currentExpenses);
        BigDecimal income = sumBase(currentIncomes);
        BigDecimal net = income.subtract(expenseAll);
        List<Transaction> selected = filterScope(currentExpenses, reportScope, catMap);
        BigDecimal selectedExpense = netOf(selected);

        BigDecimal prevExpenseAll = prevExpenses == null ? null : netOf(prevExpenses);
        BigDecimal prevIncome = prevIncomes == null ? null : sumBase(prevIncomes);
        BigDecimal prevNet = prevExpenseAll == null ? null : prevIncome.subtract(prevExpenseAll);
        List<Transaction> prevSelected = prevExpenses == null ? null
                : filterScope(prevExpenses, reportScope, catMap);
        BigDecimal prevSelectedExpense = prevSelected == null ? null : netOf(prevSelected);

        ReportOverviewVO vo = new ReportOverviewVO();
        ReportOverviewVO.Period period = buildPeriod(start, end, days, cmpStart, cmpEnd, currentMonth);
        vo.setPeriod(period);
        vo.setKpis(buildKpis(income, selectedExpense, expenseAll, net,
                prevIncome, prevSelectedExpense, prevExpenseAll, prevNet,
                selected.size(), (int) days, cmpStart != null));
        vo.setScopes(buildScopes(currentExpenses, catMap));

        // ---- 月度预算 ----
        YearMonth periodMonth = startMonth;
        boolean fullMonth = fullMonthPeriod;
        ReportOverviewVO.BudgetBlock budgetBlock = new ReportOverviewVO.BudgetBlock();
        budgetBlock.setMonth(periodMonth.toString());
        budgetBlock.setExists(false);
        BudgetProgressVO budgetProgress = null;
        if (fullMonth && budgetService.exists(periodMonth.toString())) {
            try {
                budgetProgress = budgetService.getProgress(periodMonth.toString());
            } catch (Exception e) {
                log.warn("Load budget progress failed for {}: {}", periodMonth, e.getMessage());
            }
        }
        if (budgetProgress != null) {
            budgetBlock.setExists(true);
            budgetBlock.setTotal(scale2(budgetProgress.getTotalBudget()));
            budgetBlock.setSpent(scale2(budgetProgress.getTotalSpent()));
            budgetBlock.setRemaining(scale2(budgetProgress.getRemaining()));
            budgetBlock.setPct(scale1(budgetProgress.getProgressPct()));
            budgetBlock.setItems(mapBudgetItems(budgetProgress));
        }
        vo.setBudget(budgetBlock);

        vo.setCategories(buildCategoryRank(selected, prevSelected, catMap,
                reportLevel, null, selectedExpense, budgetProgress));
        vo.setTrend(buildOverviewTrend(all, catMap, trendStartMonth, endMonth));
        vo.setDaily(buildDaily(currentExpenses, catMap));
        vo.setMerchants(buildOverviewMerchants(selected, catMap, reportScope));
        vo.setAnnualBudget(buildAnnualBlock(endMonth.getYear()));
        vo.setAccounts(buildAccountBlock(endMonth));

        ReportOverviewVO.DataStates states = new ReportOverviewVO.DataStates();
        states.setBudgetMissing(fullMonth && !budgetBlock.isExists());
        states.setPartialMonth(period.isPartialMonth());
        states.setMultiCurrency(currentExpenses.stream().anyMatch(tx ->
                tx.getCurrency() != null && !"CNY".equals(tx.getCurrency())));
        states.setNoData(expenseAll.signum() == 0 && income.signum() == 0);
        vo.setDataStates(states);

        vo.setInsights(buildInsights(vo, reportScope));
        return vo;
    }

    @Override
    public NetWorthSnapshotVO netWorthSnapshot(String month) {
        YearMonth ym;
        if (month == null || month.isBlank()) {
            ym = YearMonth.now().minusMonths(1);
        } else {
            try {
                ym = YearMonth.parse(month);
            } catch (Exception e) {
                throw new BizException(ErrorCode.PARAM_ERROR, "月份格式不正确，应为 yyyy-MM");
            }
        }
        return accountSnapshotService.snapshotDetail(getUserId(), ym);
    }

    private ReportOverviewVO.Period buildPeriod(LocalDate start, LocalDate end, long days,
                                                LocalDate cmpStart, LocalDate cmpEnd,
                                                YearMonth currentMonth) {
        ReportOverviewVO.Period p = new ReportOverviewVO.Period();
        p.setStartDate(start.toString());
        p.setEndDate(end.toString());
        p.setLabel(periodLabel(start, end));
        p.setDays((int) days);
        p.setPartialMonth(YearMonth.from(end).equals(currentMonth)
                && !end.isBefore(LocalDate.now()));
        if (cmpStart != null) {
            p.setCompareStartDate(cmpStart.toString());
            p.setCompareEndDate(cmpEnd.toString());
            p.setCompareLabel(periodLabel(cmpStart, cmpEnd));
        }
        return p;
    }

    private String periodLabel(LocalDate start, LocalDate end) {
        YearMonth sm = YearMonth.from(start);
        YearMonth em = YearMonth.from(end);
        if (sm.equals(em) && start.getDayOfMonth() == 1 && end.equals(em.atEndOfMonth())) {
            return sm.getYear() + "年" + sm.getMonthValue() + "月";
        }
        return start + " ~ " + end;
    }

    private ReportOverviewVO.Kpis buildKpis(BigDecimal income, BigDecimal expense,
                                            BigDecimal expenseAll, BigDecimal net,
                                            BigDecimal prevIncome, BigDecimal prevExpense,
                                            BigDecimal prevExpenseAll, BigDecimal prevNet,
                                            int transactionCount, int days, boolean hasPrev) {
        ReportOverviewVO.Kpis k = new ReportOverviewVO.Kpis();
        k.setIncome(scale2(income));
        k.setExpense(scale2(expense));
        k.setExpenseAll(scale2(expenseAll));
        k.setNet(scale2(net));
        k.setSavingsRate(income.signum() == 0 ? BigDecimal.ZERO
                : scale1(net.divide(income, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))));
        k.setAvgDaily(scale2(expense.divide(BigDecimal.valueOf(Math.max(days, 1)), 4, RoundingMode.HALF_UP)));
        k.setTransactionCount(transactionCount);
        if (!hasPrev) {
            return k;
        }
        k.setPrevIncome(scale2(prevIncome));
        k.setPrevExpense(scale2(prevExpense));
        k.setPrevExpenseAll(scale2(prevExpenseAll));
        k.setPrevNet(scale2(prevNet));
        BigDecimal prevRate = prevIncome.signum() == 0 ? null
                : prevNet.divide(prevIncome, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
        k.setPrevSavingsRate(prevRate);
        BigDecimal prevAvg = prevExpense.divide(BigDecimal.valueOf(Math.max(days, 1)), 4, RoundingMode.HALF_UP);
        k.setPrevAvgDaily(scale2(prevAvg));
        k.setIncomeChangePct(overviewChangePct(income, prevIncome));
        k.setExpenseChangePct(overviewChangePct(expense, prevExpense));
        k.setNetChangePct(prevNet.signum() > 0 ? overviewChangePct(net, prevNet) : null);
        if (prevRate != null) {
            k.setSavingsRateChangePp(k.getSavingsRate().subtract(prevRate).setScale(1, RoundingMode.HALF_UP));
        }
        k.setAvgDailyChangePct(prevAvg.signum() > 0 ? overviewChangePct(k.getAvgDaily(), prevAvg) : null);
        return k;
    }

    private BigDecimal overviewChangePct(BigDecimal current, BigDecimal prev) {
        if (prev == null || prev.signum() <= 0) {
            return null;
        }
        return current.subtract(prev).divide(prev, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
    }

    private List<ReportOverviewVO.ScopeItem> buildScopes(List<Transaction> txs,
                                                         Map<Long, Category> cats) {
        Map<String, ReportOverviewVO.ScopeItem> map = new LinkedHashMap<>();
        for (String s : List.of("DAILY", "FIXED", "SPECIAL")) {
            ReportOverviewVO.ScopeItem item = new ReportOverviewVO.ScopeItem();
            item.setScope(s);
            map.put(s, item);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction tx : txs) {
            ReportOverviewVO.ScopeItem item = map.get(scopeOf(tx, cats));
            if (item == null) continue;
            BigDecimal delta = netDelta(tx);
            item.setAmount(item.getAmount().add(delta));
            if (tx.getType() != null && tx.getType() == 1) {
                item.setCount(item.getCount() + 1);
            }
            total = total.add(delta);
        }
        for (ReportOverviewVO.ScopeItem item : map.values()) {
            item.setAmount(scale2(item.getAmount()));
            item.setShare(total.signum() == 0 ? BigDecimal.ZERO
                    : scale1(item.getAmount().divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))));
        }
        return new ArrayList<>(map.values());
    }

    private List<ReportOverviewVO.CategoryItem> buildCategoryRank(
            List<Transaction> current, List<Transaction> prev, Map<Long, Category> cats,
            int level, Long parentFilter, BigDecimal shareTotal, BudgetProgressVO budgetProgress) {
        Map<Long, List<Transaction>> grouped = new LinkedHashMap<>();
        for (Transaction tx : current) {
            Category cat = cats.get(tx.getCategoryId());
            if (cat == null) continue;
            Long parentId = topParentId(cat);
            if (parentFilter != null && !parentFilter.equals(parentId)) continue;
            Long groupId = level == 1 ? parentId : cat.getId();
            grouped.computeIfAbsent(groupId, k -> new ArrayList<>()).add(tx);
        }
        Map<Long, BigDecimal> prevAmounts = new HashMap<>();
        if (prev != null) {
            for (Transaction tx : prev) {
                Category cat = cats.get(tx.getCategoryId());
                if (cat == null) continue;
                Long parentId = topParentId(cat);
                if (parentFilter != null && !parentFilter.equals(parentId)) continue;
                Long groupId = level == 1 ? parentId : cat.getId();
                prevAmounts.merge(groupId, netDelta(tx), BigDecimal::add);
            }
        }

        List<ReportOverviewVO.CategoryItem> items = new ArrayList<>();
        for (Map.Entry<Long, List<Transaction>> entry : grouped.entrySet()) {
            Long groupId = entry.getKey();
            List<Transaction> txs = entry.getValue();
            BigDecimal amount = netOf(txs);
            if (amount.compareTo(new BigDecimal("0.004")) <= 0) continue;

            ReportOverviewVO.CategoryItem item = new ReportOverviewVO.CategoryItem();
            item.setCategoryId(groupId);
            item.setAmount(scale2(amount));
            item.setCount((int) txs.stream().filter(t -> t.getType() != null && t.getType() == 1).count());
            item.setShare(shareTotal.signum() == 0 ? BigDecimal.ZERO
                    : scale1(amount.divide(shareTotal, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))));

            Category cat = cats.get(groupId);
            if (cat != null) {
                item.setName(cat.getName());
                item.setColor(cat.getColor());
                if (level == 2) {
                    Category parent = cats.get(cat.getParentId());
                    if (parent != null) {
                        item.setParentId(parent.getId());
                        item.setParentName(parent.getName());
                    }
                }
            }
            Map<String, BigDecimal> scopeAmounts = new HashMap<>();
            for (Transaction tx : txs) {
                scopeAmounts.merge(scopeOf(tx, cats), netDelta(tx), BigDecimal::add);
            }
            item.setScope(scopeAmounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("DAILY"));

            if (prev != null) {
                BigDecimal prevAmount = prevAmounts.getOrDefault(groupId, BigDecimal.ZERO);
                item.setPrevAmount(scale2(prevAmount));
                if (prevAmount.signum() > 0) {
                    item.setDeltaPct(scale1(amount.subtract(prevAmount)
                            .divide(prevAmount, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))));
                }
            }
            if (level == 1 && budgetProgress != null) {
                budgetProgress.getItems().stream()
                        .filter(bi -> groupId.equals(bi.getCategoryId()))
                        .findFirst()
                        .ifPresent(bi -> {
                            item.setBudgetAmount(scale2(bi.getBudget()));
                            item.setBudgetPct(scale1(bi.getProgressPct()));
                        });
            }
            if (level == 1) {
                List<ReportOverviewVO.CategoryItem> children = buildCategoryRank(
                        current, prev, cats, 2, groupId, amount, null);
                if (!children.isEmpty()) {
                    item.setChildren(children);
                }
            }
            items.add(item);
        }
        items.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return items;
    }

    private Long topParentId(Category cat) {
        if (cat.getParentId() != null && cat.getParentId() != 0) {
            return cat.getParentId();
        }
        return cat.getId();
    }

    private List<ReportOverviewVO.TrendItem> buildOverviewTrend(
            List<Transaction> all, Map<Long, Category> cats, YearMonth from, YearMonth to) {
        List<ReportOverviewVO.TrendItem> list = new ArrayList<>();
        for (YearMonth ym = from; !ym.isAfter(to); ym = ym.plusMonths(1)) {
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            ReportOverviewVO.TrendItem item = new ReportOverviewVO.TrendItem();
            item.setMonth(ym.toString());
            for (Transaction tx : all) {
                if (tx.getTransactionDate() == null
                        || tx.getTransactionDate().isBefore(start)
                        || tx.getTransactionDate().isAfter(end)) {
                    continue;
                }
                if (tx.getType() != null && tx.getType() == 2) {
                    if (isReportableIncome(tx, cats)) {
                        item.setIncome(item.getIncome().add(tx.getAmountBase()));
                    }
                    continue;
                }
                if (tx.getType() == null || (tx.getType() != 1 && tx.getType() != 4)
                        || !isReportableExpense(tx, cats)) {
                    continue;
                }
                BigDecimal delta = netDelta(tx);
                String scope = scopeOf(tx, cats);
                item.setExpense(item.getExpense().add(delta));
                if ("FIXED".equals(scope)) {
                    item.setFixed(item.getFixed().add(delta));
                } else if ("SPECIAL".equals(scope)) {
                    item.setSpecial(item.getSpecial().add(delta));
                } else {
                    item.setDaily(item.getDaily().add(delta));
                }
            }
            item.setIncome(scale2(item.getIncome()));
            item.setExpense(scale2(item.getExpense()));
            item.setDaily(scale2(item.getDaily()));
            item.setFixed(scale2(item.getFixed()));
            item.setSpecial(scale2(item.getSpecial()));
            list.add(item);
        }
        return list;
    }

    private List<ReportOverviewVO.DailyItem> buildDaily(List<Transaction> txs,
                                                        Map<Long, Category> cats) {
        Map<LocalDate, ReportOverviewVO.DailyItem> map = new TreeMap<>();
        for (Transaction tx : txs) {
            if (tx.getTransactionDate() == null) continue;
            ReportOverviewVO.DailyItem item = map.computeIfAbsent(tx.getTransactionDate(), d -> {
                ReportOverviewVO.DailyItem di = new ReportOverviewVO.DailyItem();
                di.setDate(d.toString());
                return di;
            });
            BigDecimal delta = netDelta(tx);
            item.setExpense(item.getExpense().add(delta));
            item.setCount(item.getCount() + 1);
            String scope = scopeOf(tx, cats);
            if ("FIXED".equals(scope)) {
                item.setFixed(item.getFixed().add(delta));
            } else if ("SPECIAL".equals(scope)) {
                item.setSpecial(item.getSpecial().add(delta));
            } else {
                item.setDaily(item.getDaily().add(delta));
            }
        }
        List<ReportOverviewVO.DailyItem> list = new ArrayList<>(map.values());
        for (ReportOverviewVO.DailyItem item : list) {
            item.setExpense(scale2(item.getExpense()));
            item.setDaily(scale2(item.getDaily()));
            item.setFixed(scale2(item.getFixed()));
            item.setSpecial(scale2(item.getSpecial()));
        }
        return list;
    }

    private List<ReportOverviewVO.MerchantItem> buildOverviewMerchants(
            List<Transaction> txs, Map<Long, Category> cats, String scope) {
        Map<Long, List<Transaction>> grouped = new LinkedHashMap<>();
        for (Transaction tx : txs) {
            if (tx.getMerchantId() != null) {
                grouped.computeIfAbsent(tx.getMerchantId(), k -> new ArrayList<>()).add(tx);
            }
        }
        if (grouped.isEmpty()) {
            return List.of();
        }
        Map<Long, String> names = merchantMapper.selectBatchIds(grouped.keySet()).stream()
                .collect(Collectors.toMap(Merchant::getId, Merchant::getName, (a, b) -> a));
        List<ReportOverviewVO.MerchantItem> items = new ArrayList<>();
        for (Map.Entry<Long, List<Transaction>> entry : grouped.entrySet()) {
            String name = names.get(entry.getKey());
            if (name == null || name.isBlank()) continue;
            List<Transaction> list = entry.getValue();
            ReportOverviewVO.MerchantItem item = new ReportOverviewVO.MerchantItem();
            item.setMerchantId(entry.getKey());
            item.setName(name);
            item.setAmount(scale2(netOf(list)));
            item.setCount((int) list.stream()
                    .filter(t -> t.getType() != null && t.getType() == 1).count());
            Map<String, Integer> categoryCounts = new HashMap<>();
            for (Transaction tx : list) {
                Category cat = cats.get(tx.getCategoryId());
                if (cat != null) {
                    categoryCounts.merge(cat.getName(), 1, Integer::sum);
                }
            }
            item.setCategoryName(categoryCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(""));
            Map<String, BigDecimal> scopeAmounts = new HashMap<>();
            for (Transaction tx : list) {
                scopeAmounts.merge(scopeOf(tx, cats), netDelta(tx), BigDecimal::add);
            }
            item.setScope(scopeAmounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("DAILY"));
            items.add(item);
        }
        items.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return items.stream().limit(20).toList();
    }

    private ReportOverviewVO.AnnualBlock buildAnnualBlock(int year) {
        ReportOverviewVO.AnnualBlock block = new ReportOverviewVO.AnnualBlock();
        block.setYear(year);
        String y = String.valueOf(year);
        if (!annualBudgetService.exists(y)) {
            return block;
        }
        try {
            AnnualBudgetProgressVO p = annualBudgetService.getProgress(y);
            block.setTotal(scale2(p.getTotalBudget()));
            block.setSpent(scale2(p.getTotalSpent()));
            block.setRemaining(scale2(p.getRemaining()));
            block.setPct(scale1(p.getProgressPct()));
            LocalDate today = LocalDate.now();
            BigDecimal timePct;
            if (year < today.getYear()) {
                timePct = BigDecimal.valueOf(100);
            } else if (year > today.getYear()) {
                timePct = BigDecimal.ZERO;
            } else {
                timePct = BigDecimal.valueOf(today.getDayOfYear())
                        .divide(BigDecimal.valueOf(today.lengthOfYear()), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
            }
            block.setTimePct(timePct);
            List<ReportOverviewVO.AnnualBlock.Item> items = new ArrayList<>();
            for (AnnualBudgetProgressVO.ItemProgress bi : p.getItems()) {
                ReportOverviewVO.AnnualBlock.Item item = new ReportOverviewVO.AnnualBlock.Item();
                item.setCategoryId(bi.getCategoryId());
                item.setName(bi.getCategoryName());
                item.setAmount(scale2(bi.getBudget()));
                item.setSpent(scale2(bi.getSpent()));
                item.setPct(scale1(bi.getProgressPct()));
                items.add(item);
            }
            block.setItems(items);
        } catch (Exception e) {
            log.warn("Load annual budget failed for {}: {}", year, e.getMessage());
        }
        return block;
    }

    private ReportOverviewVO.AccountBlock buildAccountBlock(YearMonth endMonth) {
        ReportOverviewVO.AccountBlock block = new ReportOverviewVO.AccountBlock();
        List<Account> accounts = accountMapper.selectList(
                new LambdaQueryWrapper<Account>().eq(Account::getIsActive, true));
        Map<String, BigDecimal> groupSums = new HashMap<>();
        BigDecimal assets = BigDecimal.ZERO;
        BigDecimal liabilities = BigDecimal.ZERO;
        List<ReportOverviewVO.AccountBlock.Item> items = new ArrayList<>();
        for (Account account : accounts) {
            BigDecimal balance = account.getCurrentBalance() == null
                    ? BigDecimal.ZERO : account.getCurrentBalance();
            ReportOverviewVO.AccountBlock.Item item = new ReportOverviewVO.AccountBlock.Item();
            item.setId(account.getId());
            item.setName(account.getName());
            item.setType(accountTypeLabel(account.getType()));
            item.setCurrency(account.getCurrency());
            item.setBalance(scale2(balance));
            items.add(item);
            groupSums.merge(item.getType(), balance, BigDecimal::add);
            if (balance.signum() >= 0) {
                assets = assets.add(balance);
            } else {
                liabilities = liabilities.add(balance.abs());
            }
        }
        items.sort((a, b) -> b.getBalance().abs().compareTo(a.getBalance().abs()));
        block.setNetWorth(scale2(assets.subtract(liabilities)));
        block.setAssets(scale2(assets));
        block.setLiabilities(scale2(liabilities));
        List<ReportOverviewVO.AccountBlock.Group> groups = new ArrayList<>();
        for (String type : List.of("储蓄卡", "信用卡", "投资账户", "电子钱包", "现金", "未知")) {
            if (groupSums.containsKey(type)) {
                ReportOverviewVO.AccountBlock.Group group = new ReportOverviewVO.AccountBlock.Group();
                group.setType(type);
                group.setBalance(scale2(groupSums.get(type)));
                groups.add(group);
            }
        }
        block.setGroups(groups);
        block.setAccounts(items);
        block.setNetWorthTrend(accountSnapshotService.netWorthTrend(getUserId(), endMonth, 12));
        return block;
    }

    private List<ReportOverviewVO.BudgetBlock.Item> mapBudgetItems(BudgetProgressVO p) {
        List<ReportOverviewVO.BudgetBlock.Item> items = new ArrayList<>();
        if (p.getItems() == null) {
            return items;
        }
        for (BudgetProgressVO.ItemProgress bi : p.getItems()) {
            ReportOverviewVO.BudgetBlock.Item item = new ReportOverviewVO.BudgetBlock.Item();
            item.setCategoryId(bi.getCategoryId());
            item.setName(bi.getCategoryName());
            item.setAmount(scale2(bi.getBudget()));
            item.setSpent(scale2(bi.getSpent()));
            item.setPct(scale1(bi.getProgressPct()));
            items.add(item);
        }
        return items;
    }

    private List<ReportOverviewVO.InsightItem> buildInsights(ReportOverviewVO vo, String scope) {
        List<ReportOverviewVO.InsightItem> insights = new ArrayList<>();
        ReportOverviewVO.BudgetBlock budget = vo.getBudget();

        if (budget != null && budget.isExists() && budget.getTotal().signum() > 0) {
            BigDecimal over = budget.getSpent().subtract(budget.getTotal());
            ReportOverviewVO.InsightItem item = new ReportOverviewVO.InsightItem();
            if (over.signum() > 0) {
                item.setTone("bad");
                item.setTitle("日常超支 " + money(over));
                List<ReportOverviewVO.BudgetBlock.Item> overs = budget.getItems().stream()
                        .filter(bi -> bi.getSpent().compareTo(bi.getAmount()) > 0)
                        .sorted((a, b) -> b.getSpent().subtract(b.getAmount())
                                .compareTo(a.getSpent().subtract(a.getAmount())))
                        .limit(3).toList();
                BigDecimal overSum = overs.stream()
                        .map(bi -> bi.getSpent().subtract(bi.getAmount()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                item.setText("日常支出占预算的 " + budget.getPct().toPlainString() + "%。"
                        + (overs.isEmpty() ? "" : "超支主要来自"
                        + overs.stream().map(ReportOverviewVO.BudgetBlock.Item::getName)
                        .collect(Collectors.joining("、"))
                        + "，合计超出 " + money(overSum) + "。"));
            } else {
                item.setTone("good");
                item.setTitle("日常还剩 " + money(over.abs()));
                item.setText("日常支出占预算的 " + budget.getPct().toPlainString()
                        + "%，按当前节奏可以撑到月底。");
            }
            insights.add(item);
        } else {
            ReportOverviewVO.CategoryItem top = vo.getCategories().stream()
                    .max(Comparator.comparing(ReportOverviewVO.CategoryItem::getAmount))
                    .orElse(null);
            ReportOverviewVO.InsightItem item = new ReportOverviewVO.InsightItem();
            item.setTone("neutral");
            if (top == null) {
                item.setTitle("本期暂无支出");
                item.setText("换个区间，或在交易页确认本月流水是否已入账。");
            } else {
                item.setTitle(top.getName() + " 是本期最大分类");
                item.setText(money(top.getAmount()) + "，占当前范围的 "
                        + top.getShare().toPlainString() + "%。设置月度预算后可以跟踪超支。");
            }
            insights.add(item);
        }

        BigDecimal fixed = BigDecimal.ZERO;
        BigDecimal special = BigDecimal.ZERO;
        BigDecimal all = BigDecimal.ZERO;
        if (vo.getScopes() != null) {
            for (ReportOverviewVO.ScopeItem s : vo.getScopes()) {
                all = all.add(s.getAmount());
                if ("FIXED".equals(s.getScope())) fixed = s.getAmount();
                if ("SPECIAL".equals(s.getScope())) special = s.getAmount();
            }
        }
        ReportOverviewVO.InsightItem structure = new ReportOverviewVO.InsightItem();
        structure.setTone("neutral");
        if (all.signum() == 0) {
            structure.setTitle("固定与专项占 0%");
            structure.setText("本期没有支出记录。");
        } else {
            BigDecimal fixedShare = fixed.divide(all, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
            BigDecimal specialShare = special.divide(all, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
            BigDecimal dailyShare = BigDecimal.valueOf(100).subtract(fixedShare).subtract(specialShare);
            structure.setTitle("固定与专项占 " + fixedShare.add(specialShare).setScale(0, RoundingMode.HALF_UP) + "%");
            structure.setText("固定支出 " + money(fixed) + "（" + fixedShare.toPlainString()
                    + "%），专项 " + money(special) + "（" + specialShare.toPlainString()
                    + "%），日常只占 " + dailyShare.setScale(1, RoundingMode.HALF_UP) + "%。");
        }
        insights.add(structure);

        ReportOverviewVO.CategoryItem topChange = vo.getCategories().stream()
                .filter(c -> c.getPrevAmount() != null && c.getPrevAmount().signum() > 0)
                .max(Comparator.comparing(c -> c.getAmount().subtract(c.getPrevAmount())))
                .orElse(null);
        ReportOverviewVO.InsightItem change = new ReportOverviewVO.InsightItem();
        if (topChange != null) {
            BigDecimal diff = topChange.getAmount().subtract(topChange.getPrevAmount());
            change.setTone(diff.signum() > 0 ? "warn" : "good");
            change.setTitle(topChange.getName() + "环比 "
                    + (topChange.getDeltaPct() == null ? "—"
                    : (diff.signum() > 0 ? "+" : "") + topChange.getDeltaPct().toPlainString() + "%"));
            change.setText("较上期" + (diff.signum() > 0 ? "增加 " : "减少 ") + money(diff.abs())
                    + "，是本口径下变化最大的分类。");
        } else {
            change.setTone("neutral");
            change.setTitle("暂无可比变化");
            change.setText("所选区间没有可比较的上一期数据，换一个月份或对比方式试试。");
        }
        insights.add(change);
        return insights;
    }

    private List<Transaction> reportableExpenses(List<Transaction> all, LocalDate start,
                                                 LocalDate end, Map<Long, Category> cats) {
        return all.stream()
                .filter(t -> inRange(t, start, end))
                .filter(t -> t.getType() != null && (t.getType() == 1 || t.getType() == 4))
                .filter(t -> isReportableExpense(t, cats))
                .toList();
    }

    private List<Transaction> reportableIncomes(List<Transaction> all, LocalDate start,
                                                LocalDate end, Map<Long, Category> cats) {
        return all.stream()
                .filter(t -> inRange(t, start, end))
                .filter(t -> t.getType() != null && t.getType() == 2)
                .filter(t -> isReportableIncome(t, cats))
                .toList();
    }

    private boolean inRange(Transaction tx, LocalDate start, LocalDate end) {
        return tx.getTransactionDate() != null
                && !tx.getTransactionDate().isBefore(start)
                && !tx.getTransactionDate().isAfter(end);
    }

    private List<Transaction> filterScope(List<Transaction> txs, String scope,
                                          Map<Long, Category> cats) {
        if ("ALL".equals(scope)) {
            return txs;
        }
        return txs.stream().filter(t -> scope.equals(scopeOf(t, cats))).toList();
    }

    private String scopeOf(Transaction tx, Map<Long, Category> cats) {
        Category cat = tx.getCategoryId() == null ? null : cats.get(tx.getCategoryId());
        if (cat == null || cat.getBudgetType() == null) {
            return "DAILY";
        }
        if ("FIXED".equals(cat.getBudgetType())) {
            return "FIXED";
        }
        if ("SPECIAL".equals(cat.getBudgetType())) {
            return "SPECIAL";
        }
        return "DAILY";
    }

    private BigDecimal netDelta(Transaction tx) {
        BigDecimal amount = tx.getAmountBase() == null ? BigDecimal.ZERO : tx.getAmountBase();
        return tx.getType() != null && tx.getType() == 4 ? amount.negate() : amount;
    }

    private BigDecimal netOf(List<Transaction> txs) {
        return txs.stream().map(this::netDelta).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDate parseReportDate(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, field + " 不能为空");
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "日期格式不正确，应为 yyyy-MM-dd");
        }
    }

    private BigDecimal scale2(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale1(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(1, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal v) {
        return "¥" + String.format("%,.0f", v == null ? BigDecimal.ZERO : v);
    }

    @Override
    public ReportVO merchantDistribution(String month) {
        ReportVO vo = new ReportVO();

        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // 查询当月支出交易（商户维度）
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.in(Transaction::getType, 1, 4)
                .eq(Transaction::getIsConfirmed, true)
                .eq(Transaction::getReviewStatus, "REVIEWED")
                .ge(Transaction::getTransactionDate, start)
                .le(Transaction::getTransactionDate, end)
                .isNotNull(Transaction::getMerchantId);

        Map<Long, Category> catMap = loadCategoryMap();
        List<Transaction> txs = transactionMapper.selectList(query).stream()
                .filter(tx -> isReportableExpense(tx, catMap))
                .toList();

        vo.setMerchantShares(buildMerchantShares(txs));

        // 商户消费占比（剔除固定支出）：按交易分类的 budgetType 过滤
        List<Transaction> nonFixedTxs = txs.stream()
                .filter(tx -> {
                    Category cat = catMap.get(tx.getCategoryId());
                    return cat == null || !"FIXED".equals(cat.getBudgetType());
                })
                .toList();
        vo.setMerchantSharesExcludingFixed(buildMerchantShares(nonFixedTxs));

        return vo;
    }

    /** 按商户聚合消费金额（退款冲减），按金额取前 10。 */
    private List<ReportVO.MerchantShareItem> buildMerchantShares(List<Transaction> txs) {
        Map<Long, List<Transaction>> byMerchant = txs.stream()
                .filter(t -> t.getMerchantId() != null)
                .collect(Collectors.groupingBy(Transaction::getMerchantId));

        BigDecimal grandTotal = sumNetExpense(txs);
        List<ReportVO.MerchantShareItem> items = new ArrayList<>();

        for (Map.Entry<Long, List<Transaction>> entry : byMerchant.entrySet()) {
            ReportVO.MerchantShareItem item = new ReportVO.MerchantShareItem();
            item.setMerchantId(entry.getKey());

            BigDecimal merchantTotal = sumNetExpense(entry.getValue());
            item.setAmount(merchantTotal);
            item.setTransactionCount(entry.getValue().size());

            if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
                item.setPercentage(merchantTotal
                        .divide(grandTotal, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP));
            }

            // 填充商户信息
            Merchant merchant = merchantMapper.selectById(entry.getKey());
            if (merchant != null) {
                item.setMerchantName(merchant.getName());
                item.setMerchantColor(merchant.getColor());
                item.setCategoryId(merchant.getCategoryId());

                // 填充分类信息
                if (merchant.getCategoryId() != null) {
                    Category cat = categoryMapper.selectById(merchant.getCategoryId());
                    if (cat != null) {
                        item.setCategoryName(cat.getName());
                    }
                }
            }
            items.add(item);
        }

        items.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return items.stream().limit(10).collect(Collectors.toList());
    }

    // ==================== Helper Methods ====================

    private record MonthData(BigDecimal income, BigDecimal expense) {}

    /** 一个自然年的原始交易、可统计支出与净支出汇总 */
    private record YearTotals(BigDecimal income, BigDecimal expense,
                              List<Transaction> transactions, List<Transaction> expenses) {}

    private MonthData getMonthData(String month) {
        YearMonth ym = YearMonth.parse(month);
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.eq(Transaction::getIsConfirmed, true)
                .eq(Transaction::getReviewStatus, "REVIEWED")
                .ge(Transaction::getTransactionDate, ym.atDay(1))
                .le(Transaction::getTransactionDate, ym.atEndOfMonth());
        List<Transaction> txs = transactionMapper.selectList(query);
        Map<Long, Category> cats = loadCategoryMap();
        BigDecimal income = txs.stream().filter(t -> t.getType() == 2)
                .filter(t -> isReportableIncome(t, cats))
                .map(Transaction::getAmountBase).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = netExpense(txs);
        return new MonthData(income, expense);
    }

    private Map<Long, Category> loadCategoryMap() {
        return categoryMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
    }

    /**
     * 只统计真实支出分类：排除“往来款/借出款”等 budgetType=NONE 的流水。
     * 收入分类的 budgetType 也是 NONE，但不会进入支出统计。
     */
    private boolean isReportableExpense(Transaction tx, Map<Long, Category> categories) {
        if (tx.getCategoryId() == null) return false;
        Category category = categories.get(tx.getCategoryId());
        return category != null && category.getType() != null && category.getType() == 1
                && !"NONE".equals(category.getBudgetType())
                && !Boolean.TRUE.equals(category.getExcludeFromReports());
    }

    /** 收入默认计入，但“往来款收入/余额调整”等分类只走账户余额，不进收支报表。 */
    private boolean isReportableIncome(Transaction tx, Map<Long, Category> categories) {
        if (tx.getCategoryId() == null) return true;
        Category category = categories.get(tx.getCategoryId());
        return category == null || !Boolean.TRUE.equals(category.getExcludeFromReports());
    }

    /** 按一级/二级分类归并支出金额；退款冲减原分类。 */
    private Map<Long, BigDecimal> groupCategoryAmounts(List<Transaction> txs,
                                                       Map<Long, Category> catMap,
                                                       int reportLevel) {
        Map<Long, BigDecimal> amounts = new HashMap<>();
        for (Transaction tx : txs) {
            Long catId = tx.getCategoryId();
            if (catId == null) continue;
            Long groupId = catId;
            Category cat = catMap.get(catId);
            if (reportLevel == 1 && cat != null && cat.getParentId() != null && cat.getParentId() != 0) {
                groupId = cat.getParentId();
            }
            BigDecimal delta = tx.getType() == 4 ? tx.getAmountBase().negate() : tx.getAmountBase();
            amounts.merge(groupId, delta, BigDecimal::add);
        }
        return amounts;
    }

    private List<ReportVO.MonthlySummary.CategoryShareItem> buildCategoryShares(
            Map<Long, BigDecimal> categoryAmounts, BigDecimal total,
            Map<Long, Category> catMap, int reportLevel) {
        List<ReportVO.MonthlySummary.CategoryShareItem> shares = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : categoryAmounts.entrySet()) {
            ReportVO.MonthlySummary.CategoryShareItem item = new ReportVO.MonthlySummary.CategoryShareItem();
            item.setCategoryId(entry.getKey());
            item.setAmount(entry.getValue());
            if (total.compareTo(BigDecimal.ZERO) == 0) {
                item.setPercentage(BigDecimal.ZERO);
            } else {
                item.setPercentage(entry.getValue().divide(total, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));
            }

            Category cat = catMap.get(entry.getKey());
            if (cat != null) {
                item.setCategoryName(cat.getName());
                item.setCategoryColor(cat.getColor());
                if (reportLevel == 2 && cat.getParentId() != null && cat.getParentId() != 0) {
                    Category parent = catMap.get(cat.getParentId());
                    if (parent != null) {
                        item.setParentId(parent.getId());
                        item.setParentName(parent.getName());
                        if (item.getCategoryColor() == null) {
                            item.setCategoryColor(parent.getColor());
                        }
                    }
                }
            }
            shares.add(item);
        }
        shares.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return shares;
    }

    /** 净支出 = 支出(type=1) - 退款(type=4)，入参应已过滤可统计交易。 */
    private BigDecimal sumNetExpense(List<Transaction> txs) {
        if (txs == null || txs.isEmpty()) return BigDecimal.ZERO;
        return txs.stream()
                .map(tx -> tx.getType() == 4 ? tx.getAmountBase().negate() : tx.getAmountBase())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 净支出 = 支出(type=1) - 退款(type=4)。 */
    private BigDecimal netExpense(List<Transaction> txs) {
        if (txs == null || txs.isEmpty()) return BigDecimal.ZERO;
        Map<Long, Category> categories = loadCategoryMap();
        return txs.stream()
                .filter(tx -> isReportableExpense(tx, categories))
                .map(tx -> tx.getType() == 4 ? tx.getAmountBase().negate() : tx.getAmountBase())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumBase(List<Transaction> txs) {
        return txs.stream().map(Transaction::getAmountBase).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Long> getChildCategoryIds(Long parentId) {
        LambdaQueryWrapper<Category> query = new LambdaQueryWrapper<>();
        query.eq(Category::getParentId, parentId).select(Category::getId);
        return categoryMapper.selectList(query).stream().map(Category::getId).toList();
    }

    private String accountTypeLabel(Integer type) {
        return switch (type) {
            case 1 -> "现金";
            case 2 -> "储蓄卡";
            case 3 -> "信用卡";
            case 4 -> "电子钱包";
            case 5 -> "投资账户";
            default -> "未知";
        };
    }

    private Long getUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isCurrentMonth(String month) {
        return month.equals(YearMonth.now().toString());
    }

    private ReportVO getCached(String key) {
        try {
            String val = redisTemplate.opsForValue().get(key);
            if (val != null) {
                return objectMapper.readValue(val, ReportVO.class);
            }
        } catch (Exception e) {
            log.warn("Cache read failed for {}", key, e);
        }
        return null;
    }

    private void putCache(String key, ReportVO vo, int hours) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(vo), hours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Cache write failed for {}", key, e);
        }
    }
}
