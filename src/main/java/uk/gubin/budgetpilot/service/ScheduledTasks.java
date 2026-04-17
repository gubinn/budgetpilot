package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.entity.*;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 定时任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTasks {

    private final CurrencyRateService currencyRateService;
    private final BudgetService budgetService;
    private final RecurringRuleService recurringRuleService;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;
    private final CategoryMapper categoryMapper;
    private final AlertLogService alertLogService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 每日 08:00 - 汇率更新
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void refreshRates() {
        log.info("Scheduled: refreshing exchange rates");
        currencyRateService.refreshRates();
    }

    /**
     * 每日 08:00 - 周期交易生成
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void processRecurringRules() {
        log.info("Scheduled: processing recurring rules");
        List<RecurringRule> rules = recurringRuleService.getRulesDueToday();
        log.info("Found {} recurring rules to process", rules.size());

        for (RecurringRule rule : rules) {
            try {
                recurringRuleService.generateTransaction(rule);
                recurringRuleService.updateNextExecute(rule);
                log.info("Generated transaction from recurring rule: {}", rule.getName());
            } catch (Exception e) {
                log.error("Failed to process recurring rule {}", rule.getId(), e);
            }
        }
    }

    /**
     * 每日 09:00 - 信用卡还款提醒
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkCreditCardRepayment() {
        log.info("Scheduled: checking credit card repayment");
        LambdaQueryWrapper<Account> query = new LambdaQueryWrapper<>();
        query.eq(Account::getType, 3)
                .eq(Account::getIsActive, true)
                .isNotNull(Account::getPaymentDay);

        List<Account> creditCards = accountMapper.selectList(query);
        LocalDate today = LocalDate.now();

        for (Account card : creditCards) {
            int advanceDays = 3;
            LocalDate paymentDate = getPaymentDate(card, today);
            long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, paymentDate);

            if (daysUntil >= 0 && daysUntil <= advanceDays) {
                String title = "\\ud83d\\udcb3 *信用卡还款提醒*";
                String content = String.format("%s 将于 *%d月%d日* 到期还款\\n信用额度：\\u00a5%s\\n当前余额：\\u00a5%s\\n\\n_距还款日还有 %d 天_",
                        card.getName(),
                        paymentDate.getMonthValue(), paymentDate.getDayOfMonth(),
                        card.getCreditLimit() != null ? card.getCreditLimit().toPlainString() : "未设置",
                        card.getCurrentBalance().toPlainString(),
                        daysUntil);
                alertLogService.logAndNotify(5L, 5, title, content, "TELEGRAM");
            }
        }
    }

    /**
     * 每日 09:00 - 周期账单提醒（规则 6）
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkUpcomingRecurring() {
        log.info("Scheduled: checking upcoming recurring rules");
        int advanceDays = 1;
        LocalDate upcoming = LocalDate.now().plusDays(advanceDays);

        List<RecurringRule> upcomingRules = recurringRuleService.list(
                new LambdaQueryWrapper<RecurringRule>()
                        .eq(RecurringRule::getIsActive, true)
                        .eq(RecurringRule::getNextExecute, upcoming));

        for (RecurringRule rule : upcomingRules) {
            String title = "\\ud83d\\udccb *周期账单提醒*";
            String content = String.format("%s 将于 *明天* 生成\\n金额：\\u00a5%s",
                    rule.getName(), rule.getAmount().toPlainString());
            alertLogService.logAndNotify(6L, 6, title, content, "TELEGRAM");
        }
    }

    /**
     * 每日 02:30 - 账户余额校准
     */
    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    public void calibrateAccountBalances() {
        log.info("Scheduled: calibrating account balances");
        LambdaQueryWrapper<Account> query = new LambdaQueryWrapper<>();
        query.eq(Account::getIsActive, true);
        List<Account> accounts = accountMapper.selectList(query);

        for (Account account : accounts) {
            LocalDate start = LocalDate.of(2000, 1, 1);
            LambdaQueryWrapper<Transaction> txQuery = new LambdaQueryWrapper<>();
            txQuery.eq(Transaction::getAccountId, account.getId())
                    .eq(Transaction::getIsConfirmed, true)
                    .ge(Transaction::getTransactionDate, start);

            List<Transaction> txs = transactionMapper.selectList(txQuery);
            BigDecimal income = txs.stream().filter(t -> t.getType() == 2)
                    .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expense = txs.stream().filter(t -> t.getType() == 1)
                    .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal transferOut = txs.stream().filter(t -> t.getType() == 3)
                    .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal transferIn = txs.stream()
                    .filter(t -> t.getType() == 3 && account.getId().equals(t.getTargetAccountId()))
                    .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal calibrated = account.getInitialBalance()
                    .add(income).subtract(expense)
                    .subtract(transferOut).add(transferIn);
            account.setCurrentBalance(calibrated);
            accountMapper.updateById(account);
        }
    }

    /**
     * 每日 20:00 - 预算进度检查
     */
    @Scheduled(cron = "0 0 20 * * ?")
    public void checkBudgetProgress() {
        log.info("Scheduled: checking budget progress");
        String currentMonth = YearMonth.now().toString();
        if (!budgetService.exists(currentMonth)) {
            return;
        }

        try {
            var progress = budgetService.getProgress(currentMonth);
            if (progress.getProgressPct().compareTo(new BigDecimal("100")) >= 0) {
                String title = "\\ud83d\\udcca *预算超支*";
                String content = String.format("%s 月总预算已超支\\n总预算：\\u00a5%s\\n已消费：\\u00a5%s",
                        currentMonth,
                        progress.getTotalBudget().toPlainString(),
                        progress.getTotalSpent().toPlainString());
                alertLogService.logAndNotify(1L, 1, title, content, "TELEGRAM");
            }
        } catch (Exception e) {
            log.warn("Budget progress check failed", e);
        }
    }

    /**
     * 每周日 22:00 - 周消费异常检测（规则 4）
     */
    @Scheduled(cron = "0 0 22 ? * SUN")
    public void checkWeeklyAnomaly() {
        log.info("Scheduled: checking weekly spending anomaly");
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(java.time.DayOfWeek.SUNDAY);

        BigDecimal thisWeek = getWeekExpense(weekStart, weekEnd);

        BigDecimal avgExpense = BigDecimal.ZERO;
        for (int i = 1; i <= 4; i++) {
            LocalDate ws = weekStart.minusWeeks(i);
            LocalDate we = ws.with(java.time.DayOfWeek.SUNDAY);
            avgExpense = avgExpense.add(getWeekExpense(ws, we));
        }
        avgExpense = avgExpense.divide(BigDecimal.valueOf(4), 2, BigDecimal.ROUND_HALF_UP);

        if (avgExpense.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal deviation = thisWeek.subtract(avgExpense)
                .divide(avgExpense, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, BigDecimal.ROUND_HALF_UP);

        int threshold = 50;
        if (deviation.compareTo(BigDecimal.valueOf(threshold)) > 0) {
            String title = "\\ud83d\\udd0d *周消费异常*";
            String content = String.format("本周消费 \\u00a5%s vs 近 4 周平均 \\u00a5%s\\n偏离 *+%s%%*（阈值 %d%%）",
                    thisWeek.toPlainString(), avgExpense.toPlainString(), deviation.toPlainString(), threshold);
            alertLogService.logAndNotify(4L, 4, title, content, "TELEGRAM");
        } else if (deviation.abs().compareTo(BigDecimal.valueOf(threshold)) > 0 && deviation.compareTo(BigDecimal.ZERO) < 0) {
            String title = "\\ud83d\\udcc9 *周消费偏低*";
            String content = String.format("本周消费 \\u00a5%s vs 近 4 周平均 \\u00a5%s\\n偏离 *%s%%*",
                    thisWeek.toPlainString(), avgExpense.toPlainString(), deviation.toPlainString());
            alertLogService.logAndNotify(4L, 4, title, content, "TELEGRAM");
        }
    }

    /**
     * 每月 1 日 03:00 - 月度预算锁定 + 总结
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    public void lockMonthlyBudget() {
        log.info("Scheduled: locking previous month budget");
        budgetService.lockPreviousMonth();

        String prevMonth = YearMonth.now().minusMonths(1).toString();
        try {
            var progress = budgetService.getProgress(prevMonth);
            String title = "\\ud83d\\udcca *月度预算总结*";
            String content = String.format("%s 月预算已锁定\\n总预算：\\u00a5%s\\n已消费：\\u00a5%s\\n剩余：\\u00a5%s\\n进度：*%s%%*",
                    prevMonth,
                    progress.getTotalBudget().toPlainString(),
                    progress.getTotalSpent().toPlainString(),
                    progress.getRemaining().toPlainString(),
                    progress.getProgressPct().toPlainString());
            alertLogService.logAndNotify(7L, 7, title, content, "TELEGRAM");
        } catch (Exception e) {
            log.error("Failed to generate monthly summary", e);
        }
    }

    /**
     * 每月 25 日 09:00 - 月度预算未设定提醒
     */
    @Scheduled(cron = "0 0 9 25 * ?")
    public void checkBudgetNotSet() {
        log.info("Scheduled: checking if next month budget is set");
        String nextMonth = YearMonth.now().plusMonths(1).toString();
        if (!budgetService.exists(nextMonth)) {
            String title = "\\u26a0\\ufe0f *预算未设定*";
            String content = nextMonth + " 月预算尚未创建\\n请及时设定";
            alertLogService.logAndNotify(7L, 7, title, content, "TELEGRAM");
        }
    }

    /**
     * 每月 1 日 04:00 - 汇率数据清理
     */
    @Scheduled(cron = "0 0 4 1 * ?")
    public void cleanupOldRates() {
        log.info("Scheduled: cleaning up old rate data");
        currencyRateService.cleanupOldRates(6);
    }

    // ==================== Internal Methods ====================

    private BigDecimal getWeekExpense(LocalDate start, LocalDate end) {
        LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
        query.eq(Transaction::getType, 1)
                .eq(Transaction::getIsConfirmed, true)
                .ge(Transaction::getTransactionDate, start)
                .le(Transaction::getTransactionDate, end);
        List<Transaction> txs = transactionMapper.selectList(query);
        return txs.stream().map(Transaction::getAmountBase).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDate getPaymentDate(Account card, LocalDate today) {
        YearMonth ym = YearMonth.from(today);
        int paymentDay = Math.min(card.getPaymentDay(), ym.lengthOfMonth());
        LocalDate date = ym.atDay(paymentDay);
        if (date.isBefore(today)) {
            ym = ym.plusMonths(1);
            paymentDay = Math.min(card.getPaymentDay(), ym.lengthOfMonth());
            date = ym.atDay(paymentDay);
        }
        return date;
    }
}
