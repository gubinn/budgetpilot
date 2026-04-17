package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gubin.budgetpilot.entity.AlertRule;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.event.TransactionEvent;
import uk.gubin.budgetpilot.mapper.CategoryMapper;

import java.math.BigDecimal;
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
    @EventListener
    public void onTransactionEvent(TransactionEvent event) {
        Transaction tx = event.getTransaction();

        // 清除相关月份的报表缓存
        clearReportCache(tx.getTransactionDate());

        if (!Boolean.TRUE.equals(tx.getIsConfirmed()) || tx.getType() != 1) {
            return;
        }

        String yearMonth = tx.getTransactionDate().format(MONTH_FMT);

        if (event.getAction() == TransactionEvent.Action.DELETE) {
            Long topCategoryId = getTopCategoryId(tx.getCategoryId());
            if (topCategoryId != null) {
                budgetService.updateItemSpent(topCategoryId, yearMonth, tx.getAmountBase().negate());
            }
            return;
        }

        // 1. 更新预算已消费
        Long topCategoryId = getTopCategoryId(tx.getCategoryId());
        if (topCategoryId != null) {
            budgetService.updateItemSpent(topCategoryId, yearMonth, tx.getAmountBase());
        }

        // 2. 检查预警规则
        checkAlerts(tx, yearMonth);
    }

    /**
     * 清除交易日期所在月份的报表缓存
     */
    private void clearReportCache(LocalDate date) {
        String month = date.format(MONTH_FMT);
        try {
            // 清除月度汇总缓存
            redisTemplate.delete("report:monthly-summary:" + month);
            // 清除分类详情缓存（可能影响多个分类）
            var keys = redisTemplate.keys("report:category-detail:" + month + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            // 清除账户汇总缓存
            redisTemplate.delete("report:account-summary");
            log.info("Cleared report cache for month {}", month);
        } catch (Exception e) {
            log.warn("Failed to clear report cache for month {}", month, e);
        }
    }

    private void checkAlerts(Transaction tx, String yearMonth) {
        List<AlertRule> rules = alertRuleService.getActiveRules();

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

            Long topCategoryId = getTopCategoryId(tx.getCategoryId());
            if (topCategoryId == null) return false;

            var item = budgetService.getItem(topCategoryId, yearMonth);
            if (item == null || item.getAmount().compareTo(BigDecimal.ZERO) == 0) return false;

            BigDecimal pct = item.getSpent().divide(item.getAmount(), 4, BigDecimal.ROUND_HALF_UP)
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
                title = "\\ud83d\\udcca *预算预警*";
                Long topId = getTopCategoryId(tx.getCategoryId());
                var item = topId != null ? budgetService.getItem(topId, yearMonth) : null;
                if (item != null && item.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal pct = item.getSpent().divide(item.getAmount(), 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).setScale(0, BigDecimal.ROUND_HALF_UP);
                    content = String.format("预算已消费 *%s%%*\\n本月预算：¥%s\\n已消费：¥%s\\n剩余：¥%s",
                            pct, item.getAmount().toPlainString(),
                            item.getSpent().toPlainString(),
                            item.getAmount().subtract(item.getSpent()).toPlainString());
                } else {
                    content = "预算阈值预警已触发";
                }
            }
            case 2 -> {
                title = "\\ud83d\\udcb0 *大额支出提醒*";
                content = String.format("金额：¥%s\\n备注：%s",
                        tx.getAmountBase().toPlainString(),
                        tx.getNote() != null ? tx.getNote() : "无");
            }
            case 3 -> {
                title = "\\u26a0\\ufe0f *日消费上限*";
                var spent = transactionService.getDailySpent(tx.getTransactionDate());
                content = String.format("今日消费已超过设定上限\\n当前合计：¥%s", spent.get("totalSpent"));
            }
            default -> {
                title = "\\ud83d\\udd14 *预警提醒*";
                content = rule.getName() + " 已触发";
            }
        }

        content += "\\n\\n_" + LocalDate.now() + "_";
        alertLogService.logAndNotify(rule.getId(), rule.getType(), title, content, rule.getNotifyChannel());
    }

    private Long getTopCategoryId(Long categoryId) {
        Category cat = categoryMapper.selectById(categoryId);
        if (cat == null) return null;
        // 如果是一级分类，直接返回
        if (cat.getParentId() == 0) return cat.getId();
        // 否则返回父级
        return cat.getParentId();
    }
}
