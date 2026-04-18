package uk.gubin.budgetpilot.service.impl;

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
        String cacheKey = "report:monthly-summary:" + month;
        ReportVO cached = getCached(cacheKey);
        if (cached != null) return cached;

        ReportVO vo = new ReportVO();
        ReportVO.MonthlySummary summary = new ReportVO.MonthlySummary();
        summary.setMonth(month);

        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // 支出
        LambdaQueryWrapper<Transaction> expenseQuery = new LambdaQueryWrapper<>();
        expenseQuery.eq(Transaction::getType, 1).eq(Transaction::getIsConfirmed, true)
                .ge(Transaction::getTransactionDate, start).le(Transaction::getTransactionDate, end);
        List<Transaction> expenses = transactionMapper.selectList(expenseQuery);
        BigDecimal totalExpense = sumBase(expenses);
        summary.setTotalExpense(totalExpense);
        summary.setAvgDailyExpense(totalExpense.divide(BigDecimal.valueOf(ym.lengthOfMonth()), 2, RoundingMode.HALF_UP));

        // 收入
        LambdaQueryWrapper<Transaction> incomeQuery = new LambdaQueryWrapper<>();
        incomeQuery.eq(Transaction::getType, 2).eq(Transaction::getIsConfirmed, true)
                .ge(Transaction::getTransactionDate, start).le(Transaction::getTransactionDate, end);
        List<Transaction> incomes = transactionMapper.selectList(incomeQuery);
        summary.setTotalIncome(sumBase(incomes));
        summary.setBalance(summary.getTotalIncome().subtract(totalExpense));

        // 分类占比
        Map<Long, BigDecimal> categoryAmounts = expenses.stream()
                .collect(Collectors.groupingBy(Transaction::getCategoryId,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmountBase, BigDecimal::add)));

        List<ReportVO.MonthlySummary.CategoryShareItem> shares = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : categoryAmounts.entrySet()) {
            ReportVO.MonthlySummary.CategoryShareItem item = new ReportVO.MonthlySummary.CategoryShareItem();
            item.setCategoryId(entry.getKey());
            item.setAmount(entry.getValue());
            if (totalExpense.compareTo(BigDecimal.ZERO) == 0) {
                item.setPercentage(BigDecimal.ZERO);
            } else {
                item.setPercentage(entry.getValue().divide(totalExpense, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));
            }

            Category cat = categoryMapper.selectById(entry.getKey());
            if (cat != null) {
                item.setCategoryName(cat.getName());
                item.setCategoryColor(cat.getColor());
            }
            shares.add(item);
        }
        shares.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        summary.setCategoryShares(shares);

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
                .in(Transaction::getCategoryId, catIds)
                .ge(Transaction::getTransactionDate, ym.atDay(1))
                .le(Transaction::getTransactionDate, ym.atEndOfMonth())
                .orderByDesc(Transaction::getTransactionDate);

        List<Transaction> txs = transactionMapper.selectList(query);
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
            Account acc = accountMapper.selectById(tx.getAccountId());
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
                    .ge(Transaction::getTransactionDate, start)
                    .le(Transaction::getTransactionDate, end);
            if (type != null) query.eq(Transaction::getType, type);

            List<Transaction> txs = transactionMapper.selectList(query);

            BigDecimal income = txs.stream().filter(t -> t.getType() == 2)
                    .map(Transaction::getAmountBase).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expense = txs.stream().filter(t -> t.getType() == 1)
                    .map(Transaction::getAmountBase).reduce(BigDecimal.ZERO, BigDecimal::add);

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
                    .ge(Transaction::getTransactionDate, start).le(Transaction::getTransactionDate, end);
            item.setMonthIncome(sumBase(transactionMapper.selectList(incomeQ)));

            LambdaQueryWrapper<Transaction> expenseQ = new LambdaQueryWrapper<>();
            expenseQ.eq(Transaction::getType, 1).eq(Transaction::getAccountId, acc.getId())
                    .eq(Transaction::getIsConfirmed, true)
                    .ge(Transaction::getTransactionDate, start).le(Transaction::getTransactionDate, end);
            item.setMonthExpense(sumBase(transactionMapper.selectList(expenseQ)));

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
        query.eq(Transaction::getType, 1).eq(Transaction::getIsConfirmed, true)
                .ge(Transaction::getTransactionDate, start).le(Transaction::getTransactionDate, end);

        List<Transaction> txs = transactionMapper.selectList(query);
        Map<LocalDate, List<Transaction>> byDate = txs.stream()
                .collect(Collectors.groupingBy(Transaction::getTransactionDate));

        List<ReportVO.HeatmapItem> items = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Transaction>> entry : byDate.entrySet()) {
            ReportVO.HeatmapItem item = new ReportVO.HeatmapItem();
            item.setDate(entry.getKey().format(DateTimeFormatter.ISO_LOCAL_DATE));
            item.setAmount(sumBase(entry.getValue()));
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
        query.eq(Transaction::getType, 1).eq(Transaction::getIsConfirmed, true)
                .ge(Transaction::getTransactionDate, ym.atDay(1))
                .le(Transaction::getTransactionDate, ym.atEndOfMonth());

        List<Transaction> txs = transactionMapper.selectList(query);
        Map<String, List<Transaction>> byCurrency = txs.stream()
                .collect(Collectors.groupingBy(Transaction::getCurrency));

        BigDecimal grandTotal = sumBase(txs);
        List<ReportVO.CurrencyItem> items = new ArrayList<>();
        for (Map.Entry<String, List<Transaction>> entry : byCurrency.entrySet()) {
            ReportVO.CurrencyItem item = new ReportVO.CurrencyItem();
            item.setCurrency(entry.getKey());
            BigDecimal totalOriginal = entry.getValue().stream()
                    .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalBase = sumBase(entry.getValue());
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
    public ReportVO merchantDistribution(String month) {
        ReportVO vo = new ReportVO();

        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // 查询当月支出交易（商户维度）
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.eq(Transaction::getType, 1)
                .eq(Transaction::getIsConfirmed, true)
                .ge(Transaction::getTransactionDate, start)
                .le(Transaction::getTransactionDate, end)
                .isNotNull(Transaction::getMerchantId);

        List<Transaction> txs = transactionMapper.selectList(query);

        // 按商户聚合
        Map<Long, List<Transaction>> byMerchant = txs.stream()
                .filter(t -> t.getMerchantId() != null)
                .collect(Collectors.groupingBy(Transaction::getMerchantId));

        BigDecimal grandTotal = sumBase(txs);
        List<ReportVO.MerchantShareItem> items = new ArrayList<>();

        for (Map.Entry<Long, List<Transaction>> entry : byMerchant.entrySet()) {
            ReportVO.MerchantShareItem item = new ReportVO.MerchantShareItem();
            item.setMerchantId(entry.getKey());

            BigDecimal merchantTotal = sumBase(entry.getValue());
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

        // 按金额排序，取前 10 个
        items.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        vo.setMerchantShares(items.stream().limit(10).collect(Collectors.toList()));

        return vo;
    }

    // ==================== Helper Methods ====================

    private record MonthData(BigDecimal income, BigDecimal expense) {}

    private MonthData getMonthData(String month) {
        YearMonth ym = YearMonth.parse(month);
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.eq(Transaction::getIsConfirmed, true)
                .ge(Transaction::getTransactionDate, ym.atDay(1))
                .le(Transaction::getTransactionDate, ym.atEndOfMonth());
        List<Transaction> txs = transactionMapper.selectList(query);
        BigDecimal income = txs.stream().filter(t -> t.getType() == 2).map(Transaction::getAmountBase).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = txs.stream().filter(t -> t.getType() == 1).map(Transaction::getAmountBase).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MonthData(income, expense);
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
