package uk.gubin.budgetpilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import uk.gubin.budgetpilot.entity.AlertRule;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.event.TransactionEvent;
import uk.gubin.budgetpilot.mapper.CategoryMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionEventListenerAlertTest {

    private final BudgetService budgetService = mock(BudgetService.class);
    private final AlertRuleService alertRuleService = mock(AlertRuleService.class);
    private final AlertLogService alertLogService = mock(AlertLogService.class);
    private final TransactionService transactionService = mock(TransactionService.class);
    private final CategoryMapper categoryMapper = mock(CategoryMapper.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

    private TransactionEventListener listener() {
        return new TransactionEventListener(
                budgetService, alertRuleService, alertLogService,
                transactionService, categoryMapper, new ObjectMapper(), redisTemplate);
    }

    private static Transaction expense(Long categoryId, String amount) {
        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setUserId(2L);
        tx.setType(1);
        tx.setCategoryId(categoryId);
        tx.setAmountBase(new BigDecimal(amount));
        tx.setIsConfirmed(true);
        tx.setTransactionDate(LocalDate.of(2026, 11, 23));
        return tx;
    }

    private static AlertRule largeAmountRule() {
        AlertRule rule = new AlertRule();
        rule.setId(9L);
        rule.setType(2);
        rule.setName("单笔大额预警");
        rule.setIsActive(true);
        rule.setConfig("{\"max_amount\":\"1000\"}");
        rule.setNotifyChannel("WHATSAPP");
        return rule;
    }

    @Test
    @DisplayName("固定分类的大额支出也触发单笔大额预警")
    void largeAmountFiresForFixedCategory() {
        Category fixed = new Category();
        fixed.setId(10L);
        fixed.setBudgetType("FIXED");
        when(categoryMapper.selectById(10L)).thenReturn(fixed);
        when(alertRuleService.getActiveRulesByUserId(2L)).thenReturn(List.of(largeAmountRule()));

        listener().onTransactionEvent(
                new TransactionEvent(this, expense(10L, "5000"), TransactionEvent.Action.CREATE));

        verify(alertLogService).logAndNotify(
                eq(9L), eq(2), anyString(), anyString(), eq("WHATSAPP"));
    }

    @Test
    @DisplayName("无分类的大额支出也触发单笔大额预警")
    void largeAmountFiresWithoutCategory() {
        when(alertRuleService.getActiveRulesByUserId(2L)).thenReturn(List.of(largeAmountRule()));

        listener().onTransactionEvent(
                new TransactionEvent(this, expense(null, "5000"), TransactionEvent.Action.CREATE));

        verify(alertLogService).logAndNotify(
                eq(9L), eq(2), anyString(), anyString(), eq("WHATSAPP"));
    }

    @Test
    @DisplayName("固定分类不参与预算阈值预警")
    void budgetThresholdIgnoresFixedCategory() {
        Category fixed = new Category();
        fixed.setId(10L);
        fixed.setBudgetType("FIXED");
        when(categoryMapper.selectById(10L)).thenReturn(fixed);

        AlertRule budgetRule = new AlertRule();
        budgetRule.setId(8L);
        budgetRule.setType(1);
        budgetRule.setIsActive(true);
        budgetRule.setConfig("{\"threshold_pct\":80}");
        budgetRule.setNotifyChannel("WHATSAPP");
        when(alertRuleService.getActiveRulesByUserId(2L)).thenReturn(List.of(budgetRule));

        listener().onTransactionEvent(
                new TransactionEvent(this, expense(10L, "5000"), TransactionEvent.Action.CREATE));

        verify(budgetService, never()).getItem(anyLong(), anyString());
        verify(alertLogService, never()).logAndNotify(anyLong(), any(), anyString(), anyString(), anyString());
    }
}