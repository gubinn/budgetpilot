package uk.gubin.budgetpilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import uk.gubin.budgetpilot.entity.AlertRule;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.event.TransactionEvent;
import uk.gubin.budgetpilot.mapper.CategoryMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventListenerTest {

    @Mock private BudgetService budgetService;
    @Mock private AlertRuleService alertRuleService;
    @Mock private AlertLogService alertLogService;
    @Mock private TransactionService transactionService;
    @Mock private CategoryMapper categoryMapper;
    @Mock private StringRedisTemplate redisTemplate;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private TransactionEventListener eventListener;

    // ============ BUDGET ============

    @Test
    @DisplayName("已确认支出新建 → 更新预算 spent")
    void createConfirmedExpense_updatesBudget() {
        Transaction tx = expense(LocalDate.now(), "100");

        eventListener.onTransactionEvent(new TransactionEvent(this, tx, TransactionEvent.Action.CREATE));

        verify(budgetService).updateItemSpent(eq(10L), eq(YearMonth.now().toString()),
                eq(new BigDecimal("100")));
    }

    @Test
    @DisplayName("未确认交易 → 不更新预算、不检查预警")
    void unconfirmedExpense_doesNotUpdateBudgetOrAlert() {
        Transaction tx = expense(LocalDate.now(), "100");
        tx.setIsConfirmed(false);

        eventListener.onTransactionEvent(new TransactionEvent(this, tx, TransactionEvent.Action.CREATE));

        verify(budgetService, never()).updateItemSpent(anyLong(), anyString(), any());
        verifyNoInteractions(alertLogService);
    }

    @Test
    @DisplayName("收入交易 → 不更新预算")
    void income_doesNotUpdateBudget() {
        Transaction tx = expense(LocalDate.now(), "5000");
        tx.setType(2);

        eventListener.onTransactionEvent(new TransactionEvent(this, tx, TransactionEvent.Action.CREATE));

        verify(budgetService, never()).updateItemSpent(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("删除已确认支出 → 退回预算 spent")
    void deleteConfirmedExpense_refundsBudget() {
        Transaction tx = expense(LocalDate.now(), "100");

        eventListener.onTransactionEvent(new TransactionEvent(this, tx, TransactionEvent.Action.DELETE));

        verify(budgetService).updateItemSpent(eq(10L), eq(YearMonth.now().toString()),
                eq(new BigDecimal("-100")));
    }

    // ============ ALERTS ============

    @Test
    @DisplayName("新建当天的大额支出 → 触发大额预警")
    void createTodayLargeExpense_triggersAlert() {
        Transaction tx = expense(LocalDate.now(), "5000");
        when(alertRuleService.getActiveRulesByUserId(2L))
                .thenReturn(List.of(rule(22L, 2, "{\"max_amount\":\"1000\"}")));
        when(categoryMapper.selectById(10L)).thenReturn(category("大额购物"));

        eventListener.onTransactionEvent(new TransactionEvent(this, tx, TransactionEvent.Action.CREATE));

        verify(alertLogService).logAndNotify(eq(22L), eq(2), contains("大额"), anyString(), eq("WHATSAPP"));
    }

    @Test
    @DisplayName("编辑历史交易 → 不重新触发大额/日限额预警")
    void updateHistoricalExpense_doesNotTriggerAlert() {
        Transaction tx = expense(LocalDate.of(2026, 4, 2), "7712.40");
        tx.setId(148L);

        eventListener.onTransactionEvent(new TransactionEvent(this, tx, TransactionEvent.Action.UPDATE));

        verify(alertRuleService, never()).getActiveRulesByUserId(any());
        verifyNoInteractions(alertLogService);
        // 编辑的预算重算由 TransactionServiceImpl 负责，这里不重复更新
        verify(budgetService, never()).updateItemSpent(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("补录历史日期的支出 → 不触发当天消费预警")
    void createHistoricalExpense_doesNotTriggerAlert() {
        Transaction tx = expense(LocalDate.of(2026, 4, 2), "5000");
        when(alertRuleService.getActiveRulesByUserId(2L))
                .thenReturn(List.of(rule(22L, 2, "{\"max_amount\":\"1000\"}")));

        eventListener.onTransactionEvent(new TransactionEvent(this, tx, TransactionEvent.Action.CREATE));

        verify(alertLogService, never()).logAndNotify(anyLong(), anyInt(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("日消费上限同一交易日期只提醒一次")
    void dailyLimitAlert_firesOncePerDay() {
        Transaction tx = expense(LocalDate.now(), "100");
        when(alertRuleService.getActiveRulesByUserId(2L))
                .thenReturn(List.of(rule(25L, 3, "{\"daily_limit\":\"500\"}")));
        when(transactionService.getDailySpent(any(LocalDate.class)))
                .thenReturn(Map.<String, Object>of("totalSpent", new BigDecimal("800")));

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true, false);

        eventListener.onTransactionEvent(new TransactionEvent(this, tx, TransactionEvent.Action.CREATE));
        eventListener.onTransactionEvent(new TransactionEvent(this, tx, TransactionEvent.Action.CREATE));

        verify(alertLogService, times(1))
                .logAndNotify(eq(25L), eq(3), contains("日消费上限"), anyString(), eq("WHATSAPP"));
    }

    @Test
    @DisplayName("预警检查异常不影响预算更新")
    void alertCheckError_doesNotBreakBudgetUpdate() {
        Transaction tx = expense(LocalDate.now(), "100");
        when(alertRuleService.getActiveRulesByUserId(2L)).thenThrow(new RuntimeException("DB error"));

        assertThatNoException().isThrownBy(() ->
                eventListener.onTransactionEvent(new TransactionEvent(this, tx, TransactionEvent.Action.CREATE)));

        verify(budgetService).updateItemSpent(anyLong(), anyString(), any());
    }

    // ============ HELPERS ============

    private Transaction expense(LocalDate date, String amount) {
        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setUserId(2L);
        tx.setType(1);
        tx.setCategoryId(10L);
        tx.setAmountBase(new BigDecimal(amount));
        tx.setIsConfirmed(true);
        tx.setTransactionDate(date);
        return tx;
    }

    private AlertRule rule(long id, int type, String config) {
        AlertRule rule = new AlertRule();
        rule.setId(id);
        rule.setType(type);
        rule.setName("测试规则");
        rule.setIsActive(true);
        rule.setConfig(config);
        rule.setNotifyChannel("WHATSAPP");
        return rule;
    }

    private Category category(String name) {
        Category cat = new Category();
        cat.setId(10L);
        cat.setName(name);
        cat.setType(1);
        cat.setBudgetType("DAILY");
        return cat;
    }
}