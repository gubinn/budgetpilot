package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gubin.budgetpilot.entity.AlertRule;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.event.TransactionEvent;
import uk.gubin.budgetpilot.mapper.CategoryMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 交易事件监听器：异步处理预算更新 + 预警检查 + 缓存清除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventListener {

    private final BudgetService budgetService;
    private final AlertRuleService alertRuleService;
    private final AlertLogService alertLogService;
    private final TransactionService transactionService;
    private final CategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionEvent(TransactionEvent event) {
        Transaction tx = event.getTransaction();

        // 不再在此清除缓存，由 TransactionServiceImpl 同步执行
        // 这里只处理预算更新和预警检查

        if (!Boolean.TRUE.equals(tx.getIsConfirmed())
                || "PENDING".equals(tx.getReviewStatus())
                || (tx.getType() != 1 && tx.getType() != 4)) {
            return;
        }

        String yearMonth = tx.getTransactionDate().format(MONTH_FMT);
        BigDecimal delta = tx.getType() == 4 ? tx.getAmountBase().negate() : tx.getAmountBase();

        if (event.getAction() == TransactionEvent.Action.DELETE) {
            if (tx.getCategoryId() != null) {
                budgetService.updateItemSpent(tx.getCategoryId(), yearMonth, delta.negate());
            }
            return;
        }

        // 1. 更新预算已消费（预算可能设在父分类或子分类，由 service 沿分类链命中）。
        //    编辑交易涉及旧/新分类、旧/新月，无法用单一增量表达；
        //    TransactionServiceImpl.update() 已按交易事实重算两侧，这里只处理新建。
        if (event.getAction() == TransactionEvent.Action.CREATE && tx.getCategoryId() != null) {
            budgetService.updateItemSpent(tx.getCategoryId(), yearMonth, delta);
        }

        // 2. 检查预警规则（使用显式 userId 避免异步线程租户上下文丢失）
        // 退款只冲减预算，不触发消费预警；固定/专项/非预算也不走月度预算预警
        if (tx.getType() == 1 && tx.getCategoryId() != null) {
            Category category = categoryMapper.selectById(tx.getCategoryId());
            if (category != null && "DAILY".equals(category.getBudgetType())) {
                checkAlerts(event.getUserId(), tx, yearMonth);
            }
        }
    }

    /**
     * 使用 SCAN 命令安全删除匹配的 keys
     */
    private void scanAndDelete(String pattern) {
        try {
            var cursor = redisTemplate.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build());
            while (cursor.hasNext()) {
                redisTemplate.delete(cursor.next());
            }
            cursor.close();
        } catch (Exception e) {
            log.warn("Failed to scan and delete keys with pattern {}", pattern, e);
        }
    }

    private void checkAlerts(Long userId, Transaction tx, String yearMonth) {
        List<AlertRule> rules = alertRuleService.getActiveRulesByUserId(userId);

        for (AlertRule rule : rules) {
            try {
                boolean triggered = switch (rule.getType()) {
                    case 1 -> checkBudgetThreshold(rule, tx, yearMonth);
                    case 2 -> checkLargeAmount(rule, tx);
                    case 3 -> checkDailyLimit(rule, tx);
                    default -> false;
                };
                if (triggered) {
                    fireAlert(rule, tx, yearMonth);
                }
            } catch (Exception e) {
                log.error("Alert rule {} check failed", rule.getId(), e);
            }
        }
    }

    private boolean checkBudgetThreshold(AlertRule rule, Transaction tx, String yearMonth) {
        try {
            var config = objectMapper.readTree(rule.getConfig());
            int threshold = config.path("threshold_pct").asInt(80);

            if (tx.getCategoryId() == null) return false;

            var item = budgetService.getItem(tx.getCategoryId(), yearMonth);
            if (item == null || item.getAmount().compareTo(BigDecimal.ZERO) == 0) return false;

            // 以交易事实实时汇总，避免依赖 denormalized spent 的增量维护。
            BigDecimal spent = budgetService.getCategorySpent(tx.getCategoryId(), yearMonth);
            BigDecimal pct = spent.divide(item.getAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            return pct.compareTo(BigDecimal.valueOf(threshold)) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkLargeAmount(AlertRule rule, Transaction tx) {
        try {
            var config = objectMapper.readTree(rule.getConfig());
            BigDecimal maxAmount = new BigDecimal(config.path("max_amount").asText("1000"));
            return tx.getAmountBase().compareTo(maxAmount) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkDailyLimit(AlertRule rule, Transaction tx) {
        try {
            var config = objectMapper.readTree(rule.getConfig());
            BigDecimal dailyLimit = new BigDecimal(config.path("daily_limit").asText("500"));
            var dailySpent = transactionService.getDailySpent(tx.getTransactionDate());
            BigDecimal total = (BigDecimal) dailySpent.get("totalSpent");
            return total.compareTo(dailyLimit) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void fireAlert(AlertRule rule, Transaction tx, String yearMonth) {
        String title;
        String content;

        switch (rule.getType()) {
            case 1 -> {
                title = "📊 预算预警";
                var item = tx.getCategoryId() != null
                        ? budgetService.getItem(tx.getCategoryId(), yearMonth) : null;
                if (item != null && item.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal spent = budgetService.getCategorySpent(tx.getCategoryId(), yearMonth);
                    BigDecimal pct = spent.divide(item.getAmount(), 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).setScale(0, BigDecimal.ROUND_HALF_UP);
                    content = String.format("""
                            📊 预算预警

                            • 进度: %s%%
                            • 本月预算: ¥%s
                            • 已消费: ¥%s
                            • 剩余: ¥%s

                            ━━━━━━━━━━━━━━
                            _%s_
                            """,
                            pct, item.getAmount().toPlainString(),
                            spent.toPlainString(),
                            item.getAmount().subtract(spent).toPlainString(),
                            LocalDate.now());
                } else {
                    content = "预算阈值预警已触发\n\n━━━━━━━━━━━━━\n_" + LocalDate.now() + "_";
                }
            }
            case 2 -> {
                title = "💰 大额支出提醒";
                content = String.format("""
                        💰 大额支出提醒

                        • 金额: ¥%s
                        • 分类: %s
                        • 备注: %s

                        ━━━━━━━━━━━━━━
                        _%s_
                        """,
                        tx.getAmountBase().toPlainString(),
                        getCategoryName(tx.getCategoryId()),
                        tx.getNote() != null ? tx.getNote() : "无",
                        LocalDate.now());
            }
            case 3 -> {
                title = "⚠️ 日消费上限";
                var spent = transactionService.getDailySpent(tx.getTransactionDate());
                content = String.format("""
                        ⚠️ 日消费上限预警

                        • 今日消费: ¥%s
                        • 已超过设定的上限

                        ━━━━━━━━━━━━━━
                        _%s_
                        """,
                        spent.get("totalSpent"),
                        LocalDate.now());
            }
            default -> {
                title = "🔔 预警提醒";
                content = rule.getName() + " 已触发\n\n━━━━━━━━━━━━━\n_" + LocalDate.now() + "_";
            }
        }

        alertLogService.logAndNotify(rule.getId(), rule.getType(), title, content, rule.getNotifyChannel());
    }

    private String getCategoryName(Long categoryId) {
        if (categoryId == null) return "未分类";
        Category cat = categoryMapper.selectById(categoryId);
        return cat != null ? cat.getName() : "未分类";
    }
}
