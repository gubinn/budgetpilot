package uk.gubin.budgetpilot.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import uk.gubin.budgetpilot.service.ReportService;
import uk.gubin.budgetpilot.vo.ReportVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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

        // 标签统计：仅统计支出交易；一笔交易有多个标签时，每个标签各计全额
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
        summary.setTagShares(tagShares);

        vo.setMonthlySummary(summary);
        putCache(cacheKey, vo, isCurrentMonth(month) ? 1 : 24); // 当前月份缓存1小时，历史月份24小时
        return vo;
    }

    @Override
    public ReportVO categoryDetail(String month, Long categoryId) {
        ReportVO vo = new ReportVO();

        YearMonth ym = YearMonth.parse(month);
        List<Long> catIds = new ArrayList<>();
        catIds.add(categoryId);
        catIds.addAll(getChildCategoryIds(categoryId));

        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.eq(Transaction::getIsConfirmed, true)
                .eq(Transaction::getReviewStatus, "REVIEWED")
                .in(Transaction::getType, 1, 4)
                .in(Transaction::getCategoryId, catIds)
                .ge(Transaction::getTransactionDate, ym.atDay(1))
                .le(Transaction::getTransactionDate, ym.atEndOfMonth())
                .orderByDesc(Transaction::getTransactionDate);

        List<Transaction> txs = transactionMapper.selectList(query);

        // 批量加载账户，避免 N+1
        List<Long> accIds = txs.stream().map(Transaction::getAccountId).distinct().toList();
        Map<Long, Account> accMap = accIds.isEmpty() ? Map.of()
                : accountMapper.selectBatchIds(accIds).stream().collect(Collectors.toMap(Account::getId, a -> a));

        List<ReportVO.CategoryDetailItem> items = new ArrayList<>();
        for (Transaction tx : txs) {
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

            Map<Long, Category> trendCats = loadCategoryMap();
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
            Map<Long, Category> accountCats = loadCategoryMap();
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
     * 只统计真实支出分类：排除“往来款/借出”等 budgetType=NONE 的流水。
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
