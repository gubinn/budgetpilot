package uk.gubin.budgetpilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import uk.gubin.budgetpilot.entity.AlertRule;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.event.TransactionEvent;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.service.TransactionEventListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
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

    // ============ CACHE CLEAR TESTS ============

    @Nested
    @DisplayName("缓存清除测试")
    class CacheClearTests {

        @Test
        @DisplayName("交易事件 → 清除相关月份报表缓存")
        void onTransactionEvent_ClearsReportCache() {
            Transaction tx = new Transaction();
            tx.setId(1L);
            tx.setType(1);
            tx.setCategoryId(10L);
            tx.setAmountBase(new BigDecimal("100"));
            tx.setIsConfirmed(false); // 未确认也清除缓存
            tx.setTransactionDate(LocalDate.of(2026, 4, 15));

            TransactionEvent event = new TransactionEvent(this, tx, TransactionEvent.Action.CREATE);

            eventListener.onTransactionEvent(event);

            // 无登录上下文时 getUserId() 返回 null，不清除 userId 专属缓存
            // 但仍清除全局缓存
            verify(redisTemplate).delete("report:account-summary");
        }

        @Test
        @DisplayName("缓存清除失败不影响主流程")
        void onTransactionEvent_CacheError_DoesNotAffectMainFlow() {
            Transaction tx = new Transaction();
            tx.setId(1L);
            tx.setType(1);
            tx.setCategoryId(10L);
            tx.setAmountBase(new BigDecimal("100"));
            tx.setIsConfirmed(true);
            tx.setTransactionDate(LocalDate.of(2026, 4, 15));

            Category cat = new Category();
            cat.setId(10L);
            cat.setParentId(0L);
            when(categoryMapper.selectById(10L)).thenReturn(cat);
            when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis error"));

            TransactionEvent event = new TransactionEvent(this, tx, TransactionEvent.Action.CREATE);

            // 不抛异常
            assertThatNoException().isThrownBy(() -> eventListener.onTransactionEvent(event));

            // 预算更新不受影响
            verify(budgetService).updateItemSpent(anyLong(), anyString(), any());
        }
    }

    // ============ CREATE EVENT ============

    @Nested
    @DisplayName("交易创建事件")
    class CreateEventTests {

        @Test
        @DisplayName("已确认支出 → 更新预算 spent")
        void onTransactionEvent_ConfirmedExpense_UpdatesBudget() {
            Transaction tx = new Transaction();
            tx.setId(1L);
            tx.setType(1); // 支出
            tx.setCategoryId(10L);
            tx.setAmountBase(new BigDecimal("100"));
            tx.setIsConfirmed(true);
            tx.setTransactionDate(LocalDate.of(2026, 4, 15));

            Category cat = new Category();
            cat.setId(10L);
            cat.setParentId(0L); // 一级分类
            when(categoryMapper.selectById(10L)).thenReturn(cat);

            TransactionEvent event = new TransactionEvent(this, tx, TransactionEvent.Action.CREATE);

            eventListener.onTransactionEvent(event);

            verify(budgetService).updateItemSpent(eq(10L), eq("2026-04"), eq(new BigDecimal("100")));
        }

        @Test
        @DisplayName("子分类支出 → 更新父分类预算 spent")
        void onTransactionEvent_SubCategory_UpdatesParentBudget() {
            Transaction tx = new Transaction();
            tx.setId(1L);
            tx.setType(1);
            tx.setCategoryId(20L); // 子分类
            tx.setAmountBase(new BigDecimal("50"));
            tx.setIsConfirmed(true);
            tx.setTransactionDate(LocalDate.of(2026, 4, 15));

            Category subCat = new Category();
            subCat.setId(20L);
            subCat.setParentId(10L); // 父分类
            when(categoryMapper.selectById(20L)).thenReturn(subCat);

            TransactionEvent event = new TransactionEvent(this, tx, TransactionEvent.Action.CREATE);

            eventListener.onTransactionEvent(event);

            // 更新的是父分类的 spent
            verify(budgetService).updateItemSpent(eq(10L), eq("2026-04"), eq(new BigDecimal("50")));
        }

        @Test
        @DisplayName("未确认交易 → 不更新预算")
        void onTransactionEvent_Unconfirmed_NoBudgetUpdate() {
            Transaction tx = new Transaction();
            tx.setId(1L);
            tx.setType(1);
            tx.setCategoryId(10L);
            tx.setAmountBase(new BigDecimal("100"));
            tx.setIsConfirmed(false);
            tx.setTransactionDate(LocalDate.of(2026, 4, 15));

            TransactionEvent event = new TransactionEvent(this, tx, TransactionEvent.Action.CREATE);

            eventListener.onTransactionEvent(event);

            verify(budgetService, never()).updateItemSpent(anyLong(), anyString(), any());
        }

        @Test
        @DisplayName("收入交易 → 不更新预算 spent")
        void onTransactionEvent_Income_NoBudgetUpdate() {
            Transaction tx = new Transaction();
            tx.setId(1L);
            tx.setType(2); // 收入
            tx.setCategoryId(10L);
            tx.setAmountBase(new BigDecimal("5000"));
            tx.setIsConfirmed(true);
            tx.setTransactionDate(LocalDate.of(2026, 4, 15));

            TransactionEvent event = new TransactionEvent(this, tx, TransactionEvent.Action.CREATE);

            eventListener.onTransactionEvent(event);

            verify(budgetService, never()).updateItemSpent(anyLong(), anyString(), any());
        }
    }

    // ============ DELETE EVENT ============

    @Nested
    @DisplayName("交易删除事件")
    class DeleteEventTests {

        @Test
        @DisplayName("删除已确认支出 → 退回预算 spent")
        void onTransactionEvent_DeleteConfirmedExpense_RefundsBudget() {
            Transaction tx = new Transaction();
            tx.setId(1L);
            tx.setType(1);
            tx.setCategoryId(10L);
            tx.setAmountBase(new BigDecimal("100"));
            tx.setIsConfirmed(true);
            tx.setTransactionDate(LocalDate.of(2026, 4, 15));

            Category cat = new Category();
            cat.setId(10L);
            cat.setParentId(0L);
            when(categoryMapper.selectById(10L)).thenReturn(cat);

            TransactionEvent event = new TransactionEvent(this, tx, TransactionEvent.Action.DELETE);

            eventListener.onTransactionEvent(event);

            // spent 减少（负数）
            ArgumentCaptor<BigDecimal> captor = ArgumentCaptor.forClass(BigDecimal.class);
            verify(budgetService).updateItemSpent(eq(10L), eq("2026-04"), captor.capture());
            assertThat(captor.getValue()).isEqualByComparingTo(new BigDecimal("-100"));
        }
    }

    // ============ ALERT CHECKS ============

    @Nested
    @DisplayName("预警检查")
    class AlertCheckTests {

        @Test
        @DisplayName("支出事件触发大额金额预警")
        void onTransactionEvent_LargeAmountAlert() {
            Transaction tx = new Transaction();
            tx.setId(1L);
            tx.setType(1);
            tx.setCategoryId(10L);
            tx.setAmountBase(new BigDecimal("5000"));
            tx.setIsConfirmed(true);
            tx.setTransactionDate(LocalDate.of(2026, 4, 15));

            Category cat = new Category();
            cat.setId(10L);
            cat.setParentId(0L);
            when(categoryMapper.selectById(10L)).thenReturn(cat);

            AlertRule largeAmountRule = new AlertRule();
            largeAmountRule.setId(1L);
            largeAmountRule.setType(2); // 大额金额
            largeAmountRule.setName("大额预警");
            largeAmountRule.setIsActive(true);
            largeAmountRule.setConfig("{\"max_amount\":\"1000\"}");
            largeAmountRule.setNotifyChannel("TELEGRAM");

            when(alertRuleService.getActiveRules()).thenReturn(List.of(largeAmountRule));

            TransactionEvent event = new TransactionEvent(this, tx, TransactionEvent.Action.CREATE);

            eventListener.onTransactionEvent(event);

            // 预算更新 + 预警通知
            verify(alertLogService).logAndNotify(anyLong(), anyInt(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("预警检查异常不影响主流程")
        void onTransactionEvent_AlertError_DoesNotAffectMainFlow() {
            Transaction tx = new Transaction();
            tx.setId(1L);
            tx.setType(1);
            tx.setCategoryId(10L);
            tx.setAmountBase(new BigDecimal("100"));
            tx.setIsConfirmed(true);
            tx.setTransactionDate(LocalDate.of(2026, 4, 15));

            Category cat = new Category();
            cat.setId(10L);
            cat.setParentId(0L);
            when(categoryMapper.selectById(10L)).thenReturn(cat);

            when(alertRuleService.getActiveRules()).thenThrow(new RuntimeException("DB error"));

            TransactionEvent event = new TransactionEvent(this, tx, TransactionEvent.Action.CREATE);

            // 不抛异常
            assertThatNoException().isThrownBy(() -> eventListener.onTransactionEvent(event));

            // 预算更新不受影响
            verify(budgetService).updateItemSpent(anyLong(), anyString(), any());
        }
    }
}
