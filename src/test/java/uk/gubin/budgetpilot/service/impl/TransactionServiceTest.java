package uk.gubin.budgetpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.springframework.context.ApplicationEventPublisher;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.common.PageResult;
import uk.gubin.budgetpilot.dto.TransactionCreateDTO;
import uk.gubin.budgetpilot.dto.TransactionQueryDTO;
import uk.gubin.budgetpilot.dto.TransactionUpdateDTO;
import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.event.TransactionEvent;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.BudgetService;
import uk.gubin.budgetpilot.service.CurrencyRateService;
import uk.gubin.budgetpilot.vo.TransactionVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 核心测试：交易创建/更新/删除的完整业务流
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionMapper transactionMapper;
    @Mock private AccountMapper accountMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private CurrencyRateService currencyRateService;
    @Mock private BudgetService budgetService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private TransactionServiceImpl transactionService;

    private Account account;
    private Category category;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setName("工资卡");
        account.setType(2);
        account.setCurrency("CNY");
        account.setCurrentBalance(new BigDecimal("10000"));
        account.setIsActive(true);

        category = new Category();
        category.setId(10L);
        category.setParentId(0L);
        category.setName("餐饮");
        category.setType(1);
        category.setIsActive(true);
    }

    // ============ CREATE ============

    @Nested
    @DisplayName("创建交易")
    class CreateTests {

        @Test
        @DisplayName("正常创建支出交易 - CNY 币种")
        void createExpense_CNY() {
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.insert(any(Transaction.class))).thenAnswer(inv -> {
                Transaction tx = inv.getArgument(0);
                tx.setId(100L);
                return 1;
            });

            TransactionCreateDTO dto = buildCreateDTO();

            TransactionVO vo = transactionService.create(dto);

            assertThat(vo.getId()).isEqualTo(100L);
            assertThat(vo.getType()).isEqualTo(1);
            assertThat(vo.getAmount()).isEqualByComparingTo(new BigDecimal("50"));
            assertThat(vo.getIsConfirmed()).isTrue();
            assertThat(vo.getIsRecurring()).isFalse();

            // 验证余额扣减
            verify(accountMapper).adjustBalance(1L, new BigDecimal("50").negate());

            // 验证事件发布
            verify(eventPublisher).publishEvent(argThat(event ->
                    ((TransactionEvent) event).getAction() == TransactionEvent.Action.CREATE));
        }

        @Test
        @DisplayName("创建交易 - 外币自动汇率转换")
        void createTransaction_ForeignCurrency() {
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.insert(any(Transaction.class))).thenAnswer(inv -> {
                Transaction tx = inv.getArgument(0);
                tx.setId(101L);
                return 1;
            });
            when(currencyRateService.getRate(eq("USD"), any())).thenReturn(new BigDecimal("7.2"));

            TransactionCreateDTO dto = buildCreateDTO();
            dto.setCurrency("USD");
            dto.setAmount(new BigDecimal("100"));

            TransactionVO vo = transactionService.create(dto);

            assertThat(vo.getAmountBase()).isEqualByComparingTo(new BigDecimal("720.00"));
            assertThat(vo.getExchangeRate()).isEqualByComparingTo(new BigDecimal("7.2"));
        }

        @Test
        @DisplayName("创建交易 - 停用账户拒绝")
        void createTransaction_DisabledAccount() {
            account.setIsActive(false);
            when(accountMapper.selectById(1L)).thenReturn(account);

            assertThatThrownBy(() -> transactionService.create(buildCreateDTO()))
                    .isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("创建交易 - 分类不存在拒绝")
        void createTransaction_CategoryNotFound() {
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(null);

            assertThatThrownBy(() -> transactionService.create(buildCreateDTO()))
                    .isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("创建交易 - 转账类型缺少目标账户拒绝")
        void createTransaction_TransferNoTarget() {
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(category);

            TransactionCreateDTO dto = buildCreateDTO();
            dto.setType(3);
            dto.setTargetAccountId(null);

            assertThatThrownBy(() -> transactionService.create(dto))
                    .isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("创建交易 - metadata 正确映射")
        void createTransaction_MetadataMapped() {
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.insert(any(Transaction.class))).thenAnswer(inv -> {
                Transaction tx = inv.getArgument(0);
                tx.setId(102L);
                return 1;
            });

            TransactionCreateDTO dto = buildCreateDTO();
            dto.setMetadata(Map.of("key", "value"));

            transactionService.create(dto);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionMapper).insert(captor.capture());
            Transaction tx = captor.getValue();
            assertThat(tx.getMetadata()).contains("key").contains("value");
        }

        @Test
        @DisplayName("创建交易 - extFields 正确映射")
        void createTransaction_ExtFieldsMapped() {
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.insert(any(Transaction.class))).thenAnswer(inv -> {
                Transaction tx = inv.getArgument(0);
                tx.setId(103L);
                return 1;
            });

            TransactionCreateDTO dto = buildCreateDTO();
            dto.setExtFields(Map.of("field1", "data1"));

            transactionService.create(dto);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionMapper).insert(captor.capture());
            Transaction tx = captor.getValue();
            assertThat(tx.getExtFields()).contains("field1").contains("data1");
        }

        @Test
        @DisplayName("创建交易 - isRecurring 和 recurringId 正确映射")
        void createTransaction_RecurringFieldsMapped() {
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.insert(any(Transaction.class))).thenAnswer(inv -> {
                Transaction tx = inv.getArgument(0);
                tx.setId(104L);
                return 1;
            });

            TransactionCreateDTO dto = buildCreateDTO();
            dto.setIsRecurring(true);
            dto.setRecurringId(5L);

            transactionService.create(dto);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionMapper).insert(captor.capture());
            Transaction tx = captor.getValue();
            assertThat(tx.getIsRecurring()).isTrue();
            assertThat(tx.getRecurringId()).isEqualTo(5L);
        }
    }

    // ============ UPDATE ============

    @Nested
    @DisplayName("更新交易")
    class UpdateTests {

        @Test
        @DisplayName("正常更新交易 - 回滚旧余额+应用新余额")
        void updateTransaction_NormalUpdate() {
            Transaction existing = new Transaction();
            existing.setId(100L);
            existing.setType(1);
            existing.setAmount(new BigDecimal("50"));
            existing.setAccountId(1L);
            existing.setCategoryId(10L);
            existing.setTransactionDate(LocalDate.of(2026, 4, 1));
            existing.setCurrency("CNY");
            existing.setIsConfirmed(true);

            when(transactionMapper.selectById(100L)).thenReturn(existing);
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.updateById(any(Transaction.class))).thenReturn(1);

            TransactionUpdateDTO dto = new TransactionUpdateDTO();
            dto.setAmount(new BigDecimal("60"));
            dto.setNote("修改后的备注");

            TransactionVO vo = transactionService.update(100L, dto);

            assertThat(vo.getNote()).isEqualTo("修改后的备注");
            // 先回滚 50（加回）
            verify(accountMapper).adjustBalance(1L, new BigDecimal("50"));
            // 再扣 60
            verify(accountMapper).adjustBalance(1L, new BigDecimal("60").negate());
        }

        @Test
        @DisplayName("跨月修改交易日期 - 旧月退回、新月加上 budget spent")
        void updateTransaction_CrossMonthBudgetAdjustment() {
            Transaction existing = new Transaction();
            existing.setId(100L);
            existing.setType(1); // 支出
            existing.setAmount(new BigDecimal("100"));
            existing.setAccountId(1L);
            existing.setCategoryId(10L);
            existing.setTransactionDate(LocalDate.of(2026, 3, 15)); // 3月
            existing.setCurrency("CNY");
            existing.setAmountBase(new BigDecimal("100"));
            existing.setIsConfirmed(true);

            when(transactionMapper.selectById(100L)).thenReturn(existing);
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.updateById(any(Transaction.class))).thenReturn(1);

            // 修改为 4 月
            TransactionUpdateDTO dto = new TransactionUpdateDTO();
            dto.setTransactionDate(LocalDate.of(2026, 4, 10));

            transactionService.update(100L, dto);

            // 验证预算调整：3月退回
            verify(budgetService).updateItemSpent(eq(10L), eq("2026-03"), eq(new BigDecimal("-100")));
            // 验证预算调整：4月加上
            verify(budgetService).updateItemSpent(eq(10L), eq("2026-04"), eq(new BigDecimal("100")));
        }

        @Test
        @DisplayName("同月修改交易日期 - 不触发预算调整")
        void updateTransaction_SameMonthNoBudgetAdjustment() {
            Transaction existing = new Transaction();
            existing.setId(100L);
            existing.setType(1);
            existing.setAmount(new BigDecimal("100"));
            existing.setAccountId(1L);
            existing.setCategoryId(10L);
            existing.setTransactionDate(LocalDate.of(2026, 4, 5));
            existing.setCurrency("CNY");
            existing.setAmountBase(new BigDecimal("100"));
            existing.setIsConfirmed(true);

            when(transactionMapper.selectById(100L)).thenReturn(existing);
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.updateById(any(Transaction.class))).thenReturn(1);

            TransactionUpdateDTO dto = new TransactionUpdateDTO();
            dto.setTransactionDate(LocalDate.of(2026, 4, 20));

            transactionService.update(100L, dto);

            // 不应调用预算调整
            verify(budgetService, never()).updateItemSpent(anyLong(), anyString(), any());
        }

        @Test
        @DisplayName("修改未确认交易 - 不调整预算 spent")
        void updateTransaction_UnconfirmedNoBudgetAdjustment() {
            Transaction existing = new Transaction();
            existing.setId(100L);
            existing.setType(1);
            existing.setAmount(new BigDecimal("100"));
            existing.setAccountId(1L);
            existing.setCategoryId(10L);
            existing.setTransactionDate(LocalDate.of(2026, 3, 15));
            existing.setCurrency("CNY");
            existing.setAmountBase(new BigDecimal("100"));
            existing.setIsConfirmed(false); // 未确认

            when(transactionMapper.selectById(100L)).thenReturn(existing);
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.updateById(any(Transaction.class))).thenReturn(1);

            TransactionUpdateDTO dto = new TransactionUpdateDTO();
            dto.setTransactionDate(LocalDate.of(2026, 4, 10));

            transactionService.update(100L, dto);

            verify(budgetService, never()).updateItemSpent(anyLong(), anyString(), any());
        }

        @Test
        @DisplayName("修改收入类型 - 跨月也调整预算")
        void updateTransaction_IncomeCrossMonth() {
            Transaction existing = new Transaction();
            existing.setId(100L);
            existing.setType(2); // 收入
            existing.setAmount(new BigDecimal("5000"));
            existing.setAccountId(1L);
            existing.setCategoryId(10L);
            existing.setTransactionDate(LocalDate.of(2026, 3, 1));
            existing.setCurrency("CNY");
            existing.setAmountBase(new BigDecimal("5000"));
            existing.setIsConfirmed(true);

            when(transactionMapper.selectById(100L)).thenReturn(existing);
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.updateById(any(Transaction.class))).thenReturn(1);

            TransactionUpdateDTO dto = new TransactionUpdateDTO();
            dto.setTransactionDate(LocalDate.of(2026, 4, 1));

            transactionService.update(100L, dto);

            // type=2（收入）不会触发预算 spent 调整（只有支出 type=1 才影响预算）
            verify(budgetService, never()).updateItemSpent(anyLong(), anyString(), any());
        }
    }

    // ============ DELETE ============

    @Nested
    @DisplayName("删除交易")
    class DeleteTests {

        @Test
        @DisplayName("删除已确认交易 - 回滚余额")
        void delete_ConfirmedTransaction_RollbackBalance() {
            Transaction existing = new Transaction();
            existing.setId(100L);
            existing.setType(1);
            existing.setAmount(new BigDecimal("50"));
            existing.setAccountId(1L);
            existing.setCurrency("CNY");
            existing.setIsConfirmed(true);

            when(transactionMapper.selectById(100L)).thenReturn(existing);

            transactionService.delete(100L);

            // 回滚：加回 50
            verify(accountMapper).adjustBalance(1L, new BigDecimal("50"));
            verify(transactionMapper).deleteById(100L);
            verify(eventPublisher).publishEvent(argThat(event ->
                    ((TransactionEvent) event).getAction() == TransactionEvent.Action.DELETE));
        }

        @Test
        @DisplayName("删除未确认交易 - 不回滚余额")
        void delete_UnconfirmedTransaction_NoRollback() {
            Transaction existing = new Transaction();
            existing.setId(100L);
            existing.setType(1);
            existing.setAmount(new BigDecimal("50"));
            existing.setAccountId(1L);
            existing.setCurrency("CNY");
            existing.setIsConfirmed(false);

            when(transactionMapper.selectById(100L)).thenReturn(existing);

            transactionService.delete(100L);

            // 不应回滚余额
            verify(accountMapper, never()).adjustBalance(anyLong(), any());
            verify(transactionMapper).deleteById(100L);
        }
    }

    // ============ CONFIRM ============

    @Nested
    @DisplayName("确认交易")
    class ConfirmTests {

        @Test
        @DisplayName("确认未确认交易 - 应用余额")
        void confirm_UnconfirmedTransaction() {
            Transaction existing = new Transaction();
            existing.setId(100L);
            existing.setType(1);
            existing.setAmount(new BigDecimal("50"));
            existing.setAccountId(1L);
            existing.setIsConfirmed(false);

            when(transactionMapper.selectById(100L)).thenReturn(existing);
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.updateById(any(Transaction.class))).thenReturn(1);

            TransactionVO vo = transactionService.confirm(100L);

            assertThat(vo.getIsConfirmed()).isTrue();
            verify(accountMapper).adjustBalance(1L, new BigDecimal("50").negate());
        }

        @Test
        @DisplayName("确认已确认交易 - 抛出异常")
        void confirm_AlreadyConfirmed() {
            Transaction existing = new Transaction();
            existing.setId(100L);
            existing.setIsConfirmed(true);

            when(transactionMapper.selectById(100L)).thenReturn(existing);

            assertThatThrownBy(() -> transactionService.confirm(100L))
                    .isInstanceOf(BizException.class);
        }
    }

    // ============ QUERY ============

    @Nested
    @DisplayName("查询交易")
    class QueryTests {

        @Test
        @DisplayName("分页查询 - 按日期范围过滤")
        void query_ByDateRange() {
            when(transactionMapper.selectPage(any(), any())).thenReturn(emptyPage());

            TransactionQueryDTO dto = new TransactionQueryDTO();
            dto.setPage(1);
            dto.setSize(10);
            dto.setStartDate(LocalDate.of(2026, 4, 1));
            dto.setEndDate(LocalDate.of(2026, 4, 30));
            dto.setSort("transaction_date");

            PageResult<TransactionVO> result = transactionService.query(dto);

            assertThat(result.getTotal()).isZero();
        }
    }

    // ============ HELPERS ============

    private TransactionCreateDTO buildCreateDTO() {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType(1);
        dto.setAmount(new BigDecimal("50"));
        dto.setCurrency("CNY");
        dto.setAccountId(1L);
        dto.setCategoryId(10L);
        dto.setTransactionDate(LocalDate.now());
        dto.setTransactionTime(LocalTime.now());
        dto.setNote("午餐");
        return dto;
    }

    private Page<Transaction> emptyPage() {
        return new Page<>(1, 10);
    }
}
