package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gubin.budgetpilot.common.TenantContext;
import uk.gubin.budgetpilot.entity.*;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.mapper.UserMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 定时任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTasks {

    /** 单条周期规则一次最多补发多少次，防止误设很早的开始日期时生成海量历史交易。 */
    private static final int MAX_CATCH_UP_PER_RULE = 366;

    private final CurrencyRateService currencyRateService;
    private final BudgetService budgetService;
    private final RecurringRuleService recurringRuleService;
    private final AlertRuleService alertRuleService;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;
    private final CategoryMapper categoryMapper;
    private final AlertLogService alertLogService;
    private final AccountSnapshotService accountSnapshotService;
    private final ConfigService configService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final PrepaidCardService prepaidCardService;

    /**
     * 每日 08:00 - 汇率更新
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void refreshRates() {
        log.info("Scheduled: refreshing exchange rates");
        currencyRateService.refreshRates();
    }

    /**
     * 每日 08:00 - 周期交易生成。
     *
     * <p>如果进程停过几天，会把漏掉的每一次都补出来（单条规则单次最多 366 次），
     * 交易日期使用各自的计划执行日，而不是统一记到今天。
     * 每条规则独立事务，生成失败不会影响其他规则，也不会推进 nextExecute。</p>
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void processRecurringRules() {
        log.info("Scheduled: processing recurring rules");
        LocalDate today = LocalDate.now();
        forEachActiveUser(userId -> {
            List<RecurringRule> rules = recurringRuleService.getRulesDueToday();
            log.info("Found {} recurring rules to process", rules.size());

            for (RecurringRule rule : rules) {
                int generated = 0;
                try {
                    RecurringRule current = rule;
                    while (current != null
                            && Boolean.TRUE.equals(current.getIsActive())
                            && current.getNextExecute() != null
                            && !current.getNextExecute().isAfter(today)
                            && (current.getEndDate() == null
                                || !current.getNextExecute().isAfter(current.getEndDate()))
                            && generated < MAX_CATCH_UP_PER_RULE) {
                        recurringRuleService.generateTransaction(current);
                        recurringRuleService.updateNextExecute(current);
                        generated++;
                        // 重新读取，拿到推进后的 nextExecute / 新的启用状态
                        current = recurringRuleService.getById(current.getId());
                    }
                    if (generated >= MAX_CATCH_UP_PER_RULE && current != null
                            && current.getNextExecute() != null
                            && !current.getNextExecute().isAfter(today)) {
                        log.warn("Recurring rule {} reached catch-up cap ({})", rule.getId(), MAX_CATCH_UP_PER_RULE);
                    } else if (generated > 0) {
                        log.info("Generated {} transaction(s) from recurring rule: {}", generated, rule.getName());
                    }
                } catch (Exception e) {
                    log.error("Failed to process recurring rule {} after {} transaction(s)",
                            rule.getId(), generated, e);
                }
            }
        });
    }

    /**
     * 每日 09:00 - 信用卡还款提醒
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkCreditCardRepayment() {
        log.info("Scheduled: checking credit card repayment");
        forEachActiveUser(userId -> {
            AlertRule rule = findActiveRule(5);
            if (rule == null) {
                return;
            }
            int advanceDays = clamp(configInt(rule, "advance_days", 3), 0, 30);

            LambdaQueryWrapper<Account> query = new LambdaQueryWrapper<>();
            query.eq(Account::getType, 3)
                    .eq(Account::getIsActive, true)
                    .isNotNull(Account::getPaymentDay);

            List<Account> creditCards = accountMapper.selectList(query);
            log.info("Credit cards to check: {}", creditCards.size());
            LocalDate today = LocalDate.now();

            for (Account card : creditCards) {
                LocalDate paymentDate = getPaymentDate(card, today);
                long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, paymentDate);

                if (daysUntil >= 0 && daysUntil <= advanceDays) {
                    String title = "💳 信用卡还款提醒";
                    String content = String.format("""
                            💳 信用卡还款提醒

                            • 卡名: %s
                            • 还款日: %d月%d日
                            • 信用额度: ¥%s
                            • 当前余额: ¥%s
                            • 剩余天数: %d 天

                            ━━━━━━━━━━━━━━
                            _%s_
                            """,
                            card.getName(),
                            paymentDate.getMonthValue(), paymentDate.getDayOfMonth(),
                            card.getCreditLimit() != null ? card.getCreditLimit().toPlainString() : "未设置",
                            card.getCurrentBalance() != null ? card.getCurrentBalance().toPlainString() : "0",
                            daysUntil,
                            LocalDate.now());
                    alertLogService.logAndNotify(rule.getId(), 5, title, content, ruleChannel(rule));
                }
            }
        });
    }

    /**
     * 每日 09:00 - 周期账单提醒（规则 6）
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkUpcomingRecurring() {
        log.info("Scheduled: checking upcoming recurring rules");
        forEachActiveUser(userId -> {
            AlertRule rule = findActiveRule(6);
            if (rule == null) {
                return;
            }
            int advanceDays = clamp(configInt(rule, "advance_days", 1), 1, 30);
            LocalDate upcoming = LocalDate.now().plusDays(advanceDays);

            List<RecurringRule> upcomingRules = recurringRuleService.list(
                    new LambdaQueryWrapper<RecurringRule>()
                            .eq(RecurringRule::getIsActive, true)
                            .eq(RecurringRule::getNextExecute, upcoming));
            log.info("Upcoming recurring rules: {}", upcomingRules.size());

            for (RecurringRule recurring : upcomingRules) {
                String title = "📋 周期账单提醒";
                String content = String.format("""
                        📋 周期账单提醒

                        • 名称: %s
                        • 金额: ¥%s
                        • 执行日期: %d 天后

                        ━━━━━━━━━━━━━━
                        _%s_
                        """,
                        recurring.getName(), recurring.getAmount().toPlainString(), advanceDays,
                        LocalDate.now());
                alertLogService.logAndNotify(rule.getId(), 6, title, content, ruleChannel(rule));
            }
        });
    }

    /**
     * 每日 02:30 - 账户余额校准
     */
    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    public void calibrateAccountBalances() {
        // 定时任务线程没有登录用户，必须使用带 @InterceptorIgnore 的 mapper 查询/更新，
        // 否则租户拦截器会追加 user_id = null，导致查询 0 行、更新 0 行。
        List<Account> accounts = accountMapper.selectAllActive();
        log.info("Scheduled: calibrating {} active accounts", accounts.size());
        LocalDate start = LocalDate.of(2000, 1, 1);

        for (Account account : accounts) {
            List<Transaction> txs = transactionMapper.selectForCalibration(account.getId(), start);
            BigDecimal calibrated = calculateCalibratedBalance(
                    account.getInitialBalance(), account.getId(), txs);
            BigDecimal old = account.getCurrentBalance();
            if (old == null || old.compareTo(calibrated) != 0) {
                log.info("Calibrating account {} ({}): {} -> {}",
                        account.getName(), account.getId(), old, calibrated);
                accountMapper.updateBalanceForCalibration(account.getId(), calibrated);
            }
        }
    }

    /**
     * 按账户币种口径重算余额。
     *
     * <p>交易表的 {@code amount} 是账户币种金额，{@code amountBase} 是 CNY 金额；
     * 账户余额必须用 {@code amount}。收入/退款增加余额，支出/转出减少余额；
     * 作为转账目标账户时增加转入金额。</p>
     */
    static BigDecimal calculateCalibratedBalance(BigDecimal initialBalance, Long accountId, List<Transaction> txs) {
        return uk.gubin.budgetpilot.common.BalanceCalculator.balanceAt(initialBalance, accountId, txs);
    }

    /**
     * 每日 02:50 - 最近 12 个已结束月份的账户月末余额快照。
     *
     * <p>放在余额校准（02:30）之后，保证用的是校准后的 current_balance。
     * 重刷 12 个月是为了覆盖迟到导入的历史流水和手工改账；
     * 单次只有十几条账户 × 12 个月，成本可以忽略。</p>
     */
    @Scheduled(cron = "0 50 2 * * ?")
    public void snapshotRecentAccountBalances() {
        YearMonth prev = YearMonth.now().minusMonths(1);
        List<YearMonth> months = new java.util.ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            months.add(prev.minusMonths(i));
        }
        log.info("Scheduled: snapshotting {} months of account balances", months.size());
        forEachActiveUser(userId -> accountSnapshotService.snapshotMonths(userId, months));
    }

    /**
     * 每日 20:00 - 预算进度检查
     */
    @Scheduled(cron = "0 0 20 * * ?")
    public void checkBudgetProgress() {
        log.info("Scheduled: checking budget progress");
        String currentMonth = YearMonth.now().toString();
        forEachActiveUser(userId -> {
            AlertRule rule = findActiveRule(1);
            if (rule == null) {
                return;
            }
            int threshold = clamp(configInt(rule, "threshold_pct", 80), 1, 100);

            boolean hasBudget = budgetService.exists(currentMonth);
            log.info("Budget exists for {}: {}", currentMonth, hasBudget);
            if (!hasBudget) {
                return;
            }

            try {
                var progress = budgetService.getProgress(currentMonth);
                if (progress.getProgressPct().compareTo(BigDecimal.valueOf(threshold)) >= 0) {
                    boolean exceeded = progress.getProgressPct().compareTo(new BigDecimal("100")) >= 0;
                    String title = exceeded ? "📊 预算超支" : "📊 预算预警";
                    String content = String.format("""
                            📊 %s

                            • 月份: %s
                            • 总预算: ¥%s
                            • 已消费: ¥%s
                            • 进度: %s%%（预警阈值 %d%%）
                            • %s: ¥%s

                            ━━━━━━━━━━━━━━
                            _%s_
                            """,
                            exceeded ? "预算超支警告" : "预算进度预警",
                            currentMonth,
                            progress.getTotalBudget().toPlainString(),
                            progress.getTotalSpent().toPlainString(),
                            progress.getProgressPct().toPlainString(),
                            threshold,
                            exceeded ? "超支" : "剩余",
                            exceeded
                                    ? progress.getTotalSpent().subtract(progress.getTotalBudget()).toPlainString()
                                    : progress.getRemaining().toPlainString(),
                            LocalDate.now());
                    alertLogService.logAndNotify(rule.getId(), 1, title, content, ruleChannel(rule));
                }
            } catch (Exception e) {
                log.warn("Budget progress check failed", e);
            }
        });
    }

    /**
     * 每周日 22:00 - 周消费异常检测（规则 4）
     */
    @Scheduled(cron = "0 0 22 ? * SUN")
    public void checkWeeklyAnomaly() {
        log.info("Scheduled: checking weekly spending anomaly");
        forEachActiveUser(userId -> {
            AlertRule rule = findActiveRule(4);
            if (rule == null) {
                return;
            }
            int threshold = clamp(configInt(rule, "deviation_pct", 50), 1, 1000);

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
            avgExpense = avgExpense.divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);

            if (avgExpense.compareTo(BigDecimal.ZERO) == 0) return;

            BigDecimal deviation = thisWeek.subtract(avgExpense)
                    .divide(avgExpense, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);

            if (deviation.compareTo(BigDecimal.valueOf(threshold)) > 0) {
                String title = "🔍 周消费异常";
                String content = String.format("""
                        🔍 周消费异常警告

                        • 本周消费: ¥%s
                        • 4周平均: ¥%s
                        • 偏离: +%s%%
                        • 阈值: %d%%

                        ━━━━━━━━━━━━━━
                        _%s_
                        """,
                        thisWeek.toPlainString(), avgExpense.toPlainString(), deviation.toPlainString(), threshold,
                        LocalDate.now());
                alertLogService.logAndNotify(rule.getId(), 4, title, content, ruleChannel(rule));
            } else if (deviation.abs().compareTo(BigDecimal.valueOf(threshold)) > 0 && deviation.compareTo(BigDecimal.ZERO) < 0) {
                String title = "📉 周消费偏低";
                String content = String.format("""
                        📉 周消费偏低提醒

                        • 本周消费: ¥%s
                        • 4周平均: ¥%s
                        • 偏离: %s%%

                        ━━━━━━━━━━━━━━
                        _%s_
                        """,
                        thisWeek.toPlainString(), avgExpense.toPlainString(), deviation.toPlainString(),
                        LocalDate.now());
                alertLogService.logAndNotify(rule.getId(), 4, title, content, ruleChannel(rule));
            }
        });
    }

    /**
     * 每月 1 日 03:00 - 月度预算锁定 + 总结
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    public void lockMonthlyBudget() {
        log.info("Scheduled: locking previous month budget");
        forEachActiveUser(userId -> {
            budgetService.lockPreviousMonth();

            String prevMonth = YearMonth.now().minusMonths(1).toString();
            try {
                var progress = budgetService.getProgress(prevMonth);
                String title = "📊 月度预算总结";
                String content = String.format("""
                        📊 月度预算总结

                        • 月份: %s
                        • 总预算: ¥%s
                        • 已消费: ¥%s
                        • 剩余: ¥%s
                        • 进度: %s%%

                        ━━━━━━━━━━━━━━
                        _%s_
                        """,
                        prevMonth,
                        progress.getTotalBudget().toPlainString(),
                        progress.getTotalSpent().toPlainString(),
                        progress.getRemaining().toPlainString(),
                        progress.getProgressPct().toPlainString(),
                        LocalDate.now());
                // 月度总结是系统报告，不对应可配置的预警规则；rule_id=0 表示系统消息
                alertLogService.logAndNotify(0L, 9, title, content, defaultChannel());
            } catch (Exception e) {
                log.error("Failed to generate monthly summary", e);
            }
        });
    }

    /**
     * 每日 09:00 - 月度预算未设定提醒（规则 7，按规则里的 check_day 判断当天是否提醒）
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkBudgetNotSet() {
        log.info("Scheduled: checking if next month budget is set");
        forEachActiveUser(userId -> {
            AlertRule rule = findActiveRule(7);
            if (rule == null) {
                return;
            }
            int checkDay = clamp(configInt(rule, "check_day", 25), 1, 28);
            if (LocalDate.now().getDayOfMonth() != checkDay) {
                return;
            }

            String nextMonth = YearMonth.now().plusMonths(1).toString();
            if (!budgetService.exists(nextMonth)) {
                String title = "⚠️ 预算未设定";
                String content = String.format("""
                        ⚠️ 预算未设定提醒

                        • %s 月预算尚未创建
                        • 请及时设定下月预算

                        ━━━━━━━━━━━━━━
                        _%s_
                        """,
                        nextMonth,
                        LocalDate.now());
                alertLogService.logAndNotify(rule.getId(), 7, title, content, ruleChannel(rule));
            }
        });
    }

    /**
     * 每日 10:00 - 待确认交易提醒（规则 8）
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void checkUnconfirmedTransactions() {
        log.info("Scheduled: checking unconfirmed transactions");

        forEachActiveUser(userId -> {
            // 查询启用的待确认交易预警规则
            List<AlertRule> rules = alertRuleService.list(
                    new LambdaQueryWrapper<AlertRule>()
                            .eq(AlertRule::getIsActive, true)
                            .eq(AlertRule::getType, 8));

            if (rules.isEmpty()) return;

            // 查询未确认的交易。按交易日期倒序，避免历史导入的旧账单占据列表前几条；
            // 创建时间只作为同一天的次级排序。
            LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
            query.eq(Transaction::getIsConfirmed, false)
                    .orderByDesc(Transaction::getTransactionDate)
                    .orderByDesc(Transaction::getCreatedAt);
            List<Transaction> unconfirmed = transactionMapper.selectList(query);
            log.info("Unconfirmed transactions: {}", unconfirmed.size());

            if (unconfirmed.isEmpty()) return;

            // 检查每个规则
            for (AlertRule rule : rules) {
                try {
                    var config = objectMapper.readTree(rule.getConfig());
                    int minCount = config.path("min_count").asInt(1);

                    if (unconfirmed.size() >= minCount) {
                        // 汇总未确认交易信息
                        BigDecimal totalAmount = unconfirmed.stream()
                                .map(Transaction::getAmountBase)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        StringBuilder details = new StringBuilder();
                        for (int i = 0; i < Math.min(5, unconfirmed.size()); i++) {
                            Transaction t = unconfirmed.get(i);
                            details.append("• ").append(t.getTransactionDate()).append(" ")
                                    .append(t.getNote() != null ? t.getNote() : "无备注")
                                    .append(" - ¥").append(t.getAmountBase().toPlainString())
                                    .append("\n");
                        }
                        if (unconfirmed.size() > 5) {
                            details.append("• 还有 ").append(unconfirmed.size() - 5).append(" 条...\n");
                        }

                        LocalDate earliest = unconfirmed.stream()
                                .map(Transaction::getTransactionDate)
                                .filter(java.util.Objects::nonNull)
                                .min(LocalDate::compareTo)
                                .orElse(null);
                        String range = earliest != null ? "（最早 " + earliest + "）" : "";

                        String title = "📝 待确认交易提醒";
                        String content = String.format("""
                                📝 待确认交易提醒

                                • 待确认数量: %d 条%s
                                • 涉及金额: ¥%s

                                %s
                                请及时确认这些交易！

                                ━━━━━━━━━━━━━━
                                _%s_
                                """,
                                unconfirmed.size(),
                                range,
                                totalAmount.toPlainString(),
                                details.toString(),
                                LocalDate.now());

                        alertLogService.logAndNotify(rule.getId(), 8, title, content, ruleChannel(rule));
                        log.info("Unconfirmed transaction alert sent: {} transactions", unconfirmed.size());
                    }
                } catch (Exception e) {
                    log.error("Failed to check unconfirmed transactions for rule {}", rule.getId(), e);
                }
            }
        });
    }

    /**
     * 每日 09:00 - 卡券到期提醒（规则 9）。
     *
     * <p>一张卡只在三个节点提醒：到期前 advance_days 天（默认 30）、
     * 到期当天、过期次日（仍有余额时）。汇总成一条消息，不逐卡轰炸。</p>
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkPrepaidCardExpiry() {
        log.info("Scheduled: checking prepaid card expiry");
        forEachActiveUser(userId -> {
            AlertRule rule = findActiveRule(10);
            if (rule == null) {
                return;
            }
            int advanceDays = clamp(configInt(rule, "advance_days", 30), 1, 90);
            LocalDate today = LocalDate.now();
            List<String> lines = new ArrayList<>();

            for (PrepaidCard card : prepaidCardService.listAll()) {
                if (!Integer.valueOf(1).equals(card.getStatus())
                        || card.getBalance() == null
                        || card.getBalance().signum() <= 0
                        || card.getExpiryDate() == null) {
                    continue;
                }
                long days = ChronoUnit.DAYS.between(today, card.getExpiryDate());
                String when;
                if (days == advanceDays) {
                    when = "还有 " + days + " 天到期";
                } else if (days == 0) {
                    when = "今天到期";
                } else if (days == -1) {
                    when = "已过期 1 天";
                } else {
                    continue;
                }
                String place = card.getChannel() == null || card.getChannel().isBlank()
                        ? "" : "（" + card.getChannel() + "）";
                lines.add("• " + card.getName() + " · " + card.getIssuer() + place
                        + "\n  " + remainingText(card) + " · " + when);
            }

            if (lines.isEmpty()) {
                return;
            }
            String title = "🎟️ 卡券到期提醒";
            String content = title + "\n\n" + String.join("\n\n", lines)
                    + "\n\n别让它们悄悄过期，有空去用掉吧。"
                    + "\n\n━━━━━━━━━━━━━━\n_" + today + "_";
            alertLogService.logAndNotify(rule.getId(), 10, title, content, ruleChannel(rule));
            log.info("Prepaid card expiry alert sent: {} card(s)", lines.size());
        });
    }

    private String remainingText(PrepaidCard card) {
        BigDecimal balance = card.getBalance();
        if (Integer.valueOf(2).equals(card.getUnit())) {
            return "剩余 " + balance.stripTrailingZeros().toPlainString() + " 次/张";
        }
        return "余额 ¥" + balance.setScale(2, RoundingMode.HALF_UP).toPlainString();
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

    /**
     * 定时任务线程没有 Sa-Token 登录态。按用户循环并设置 TenantContext，
     * 否则租户拦截器会拼出 user_id = null，所有查询恒为空。
     */
    private void forEachActiveUser(java.util.function.Consumer<Long> action) {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getIsActive, true));
        if (users.isEmpty()) {
            log.warn("No active users, scheduled task skipped");
            return;
        }
        for (User user : users) {
            TenantContext.set(user.getId());
            try {
                action.accept(user.getId());
            } catch (Exception e) {
                log.error("Scheduled task failed for user {}", user.getId(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    /**
     * 取当前租户下某类型的启用规则。
     *
     * <p>系统定时提醒（1/4/5/6/7）以前只认硬编码的规则 ID 和阈值，
     * 导致用户在页面上停用或改配置后完全不生效，日志还会挂到其他租户的规则 ID 上。
     * 这里按类型取当前用户第一条启用规则，规则不存在或已停用就跳过。</p>
     */
    private AlertRule findActiveRule(int type) {
        List<AlertRule> rules = alertRuleService.list(
                new LambdaQueryWrapper<AlertRule>()
                        .eq(AlertRule::getIsActive, true)
                        .eq(AlertRule::getType, type)
                        .orderByAsc(AlertRule::getId));
        return rules.isEmpty() ? null : rules.get(0);
    }

    private int configInt(AlertRule rule, String key, int fallback) {
        if (rule.getConfig() == null || rule.getConfig().isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readTree(rule.getConfig()).path(key).asInt(fallback);
        } catch (Exception e) {
            log.warn("Invalid config for alert rule {}: {}", rule.getId(), rule.getConfig());
            return fallback;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 规则未指定渠道时回落到全局默认渠道（t_config.notify_channel）。
     */
    private String ruleChannel(AlertRule rule) {
        String channel = rule.getNotifyChannel();
        return (channel == null || channel.isBlank()) ? defaultChannel() : channel;
    }

    /**
     * 全局默认渠道（t_config.notify_channel），未配置时为 TELEGRAM。
     * 用于没有对应规则的月度总结，以及规则未配置渠道时的回落。
     */
    private String defaultChannel() {
        String channel = configService.get("notify_channel");
        return (channel == null || channel.isBlank()) ? "TELEGRAM" : channel;
    }

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
