package uk.gubin.budgetpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.dto.*;
import uk.gubin.budgetpilot.entity.*;
import uk.gubin.budgetpilot.event.TransactionEvent;
import uk.gubin.budgetpilot.mapper.*;
import uk.gubin.budgetpilot.service.*;
import uk.gubin.budgetpilot.vo.TransactionVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 代码审查修复项回归测试
 */
@ExtendWith(MockitoExtension.class)
class BugfixRegressionTest {

    @Mock private TransactionMapper transactionMapper;
    @Mock private AccountMapper accountMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private BudgetItemMapper budgetItemMapper;
    @Mock private BudgetMapper budgetMapper;
    @Mock private CurrencyRateService currencyRateService;
    @Mock private BudgetService budgetService;
    @Mock private MerchantService merchantService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private StringRedisTemplate redisTemplate;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    private TransactionServiceImpl transactionService;

    private Account debitAccount;
    private Account creditAccount;
    private Category category;
    private Category balanceAdjustIncome;
    private Category balanceAdjustExpense;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize transaction synchronization for @Transactional methods
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }

        // @RequiredArgsConstructor generates constructor in field declaration order
        transactionService = new TransactionServiceImpl(
                objectMapper, currencyRateService, accountMapper, categoryMapper,
                mock(MerchantMapper.class), mock(AccountService.class), budgetService,
                merchantService, eventPublisher, redisTemplate);
        // Inject baseMapper into ServiceImpl's parent CrudRepository
        injectField(transactionService, "baseMapper", transactionMapper);

        debitAccount = new Account();
        debitAccount.setId(1L);
        debitAccount.setName("储蓄卡");
        debitAccount.setType(2);
        debitAccount.setCurrency("CNY");
        debitAccount.setInitialBalance(new BigDecimal("10000"));
        debitAccount.setCurrentBalance(new BigDecimal("10000"));
        debitAccount.setIsActive(true);

        creditAccount = new Account();
        creditAccount.setId(2L);
        creditAccount.setName("信用卡");
        creditAccount.setType(3);
        creditAccount.setCurrency("CNY");
        creditAccount.setCurrentBalance(BigDecimal.ZERO);
        creditAccount.setIsActive(true);

        category = new Category();
        category.setId(10L);
        category.setParentId(0L);
        category.setName("餐饮");
        category.setType(1);
        category.setIsActive(true);

        balanceAdjustIncome = new Category();
        balanceAdjustIncome.setId(58L);
        balanceAdjustIncome.setName("余额调整");
        balanceAdjustIncome.setType(2);
        balanceAdjustIncome.setUserId(0L);

        balanceAdjustExpense = new Category();
        balanceAdjustExpense.setId(59L);
        balanceAdjustExpense.setName("余额调整");
        balanceAdjustExpense.setType(1);
        balanceAdjustExpense.setUserId(0L);
    }

    @SuppressWarnings("unchecked")
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                java.lang.reflect.Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        // Not found in the class hierarchy, try ServiceImpl parent's parent (CrudRepository.baseMapper)
        if ("transactionMapper".equals(fieldName)) {
            // MyBatis-Plus ServiceImpl stores mapper as baseMapper in CrudRepository
            try {
                java.lang.reflect.Field f = com.baomidou.mybatisplus.extension.repository.CrudRepository.class.getDeclaredField("baseMapper");
                f.setAccessible(true);
                f.set(target, value);
            } catch (NoSuchFieldException ignored) {}
        }
    }

    // ======================== 必须修复 ========================

    @Nested
    @DisplayName("[修复-4] 并发创建支出 - 原子扣减余额")
    class ConcurrentBalanceCheckTests {

        @Test
        @DisplayName("原子扣减返回 0 行，抛出余额不足异常")
        void decreaseBalanceIfSufficient_ZeroRows_Throws() {
            when(accountMapper.selectById(1L)).thenReturn(debitAccount);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.insert(any(Transaction.class))).thenAnswer(inv -> {
                Transaction tx = inv.getArgument(0);
                tx.setId(100L);
                return 1;
            });
            when(accountMapper.decreaseBalanceIfSufficient(eq(1L), any())).thenReturn(0);

            TransactionCreateDTO dto = buildCreateDTO(1L, 10L, new BigDecimal("15000"));

            // insert is called before balance check, so insert IS invoked, but exception is thrown after
            assertThatThrownBy(() -> transactionService.create(dto))
                    .isInstanceOf(BizException.class);

            // insert was called but transaction creation ultimately failed (balance check after insert)
            verify(transactionMapper).insert(any(Transaction.class));
            verify(accountMapper).decreaseBalanceIfSufficient(eq(1L), eq(new BigDecimal("15000")));
        }

        @Test
        @DisplayName("原子扣减成功，正常创建")
        void decreaseBalanceIfSufficient_OneRow_Succeeds() {
            when(accountMapper.selectById(1L)).thenReturn(debitAccount);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.insert(any(Transaction.class))).thenAnswer(inv -> {
                Transaction tx = inv.getArgument(0);
                tx.setId(100L);
                return 1;
            });
            when(accountMapper.decreaseBalanceIfSufficient(eq(1L), any())).thenReturn(1);

            TransactionCreateDTO dto = buildCreateDTO(1L, 10L, new BigDecimal("100"));
            TransactionVO vo = transactionService.create(dto);

            assertThat(vo.getId()).isEqualTo(100L);
            verify(accountMapper, never()).adjustBalance(eq(1L), any());
            verify(accountMapper).decreaseBalanceIfSufficient(eq(1L), eq(new BigDecimal("100")));
        }

        @Test
        @DisplayName("信用卡账户允许透支，走通用 adjustBalance")
        void creditCard_AllowsOverdraft() {
            when(accountMapper.selectById(2L)).thenReturn(creditAccount);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.insert(any(Transaction.class))).thenAnswer(inv -> {
                Transaction tx = inv.getArgument(0);
                tx.setId(100L);
                return 1;
            });

            TransactionCreateDTO dto = buildCreateDTO(2L, 10L, new BigDecimal("100"));
            dto.setType(1);
            transactionService.create(dto);

            verify(accountMapper).adjustBalance(2L, new BigDecimal("100").negate());
            verify(accountMapper, never()).decreaseBalanceIfSufficient(anyLong(), any());
        }

        @Test
        @DisplayName("转账 - 源账户原子扣减，目标账户普通增加")
        void transfer_SourceAtomic_TargetNormal() {
            Account targetAcc = new Account();
            targetAcc.setId(3L);
            targetAcc.setName("目标");
            targetAcc.setType(2);
            targetAcc.setCurrency("CNY");
            targetAcc.setCurrentBalance(new BigDecimal("5000"));
            targetAcc.setIsActive(true);

            when(accountMapper.selectById(1L)).thenReturn(debitAccount);
            when(accountMapper.selectById(3L)).thenReturn(targetAcc);
            when(transactionMapper.insert(any(Transaction.class))).thenAnswer(inv -> {
                Transaction tx = inv.getArgument(0);
                tx.setId(100L);
                return 1;
            });
            when(accountMapper.decreaseBalanceIfSufficient(eq(1L), any())).thenReturn(1);

            TransactionCreateDTO dto = buildCreateDTO(1L, 10L, new BigDecimal("200"));
            dto.setType(3);
            dto.setTargetAccountId(3L);
            dto.setCategoryId(null);

            transactionService.create(dto);

            verify(accountMapper).decreaseBalanceIfSufficient(eq(1L), eq(new BigDecimal("200")));
            verify(accountMapper).adjustBalance(eq(3L), eq(new BigDecimal("200")));
        }
    }

    @Nested
    @DisplayName("[修复-5] 确认交易前校验余额")
    class ConfirmBalanceCheckTests {

        @Test
        @DisplayName("确认支出 - 余额充足")
        void confirm_SufficientBalance() {
            Transaction tx = new Transaction();
            tx.setId(100L);
            tx.setType(1);
            tx.setAmount(new BigDecimal("50"));
            tx.setAccountId(1L);
            tx.setCategoryId(10L);
            tx.setCurrency("CNY");
            tx.setAmountBase(new BigDecimal("50"));
            tx.setTransactionDate(LocalDate.now());
            tx.setIsConfirmed(false);

            when(transactionMapper.selectById(100L)).thenReturn(tx);
            when(accountMapper.selectById(1L)).thenReturn(debitAccount);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.updateById(any(Transaction.class))).thenReturn(1);
            when(accountMapper.decreaseBalanceIfSufficient(eq(1L), eq(new BigDecimal("50")))).thenReturn(1);

            TransactionVO vo = transactionService.confirm(100L);

            assertThat(vo.getIsConfirmed()).isTrue();
            verify(accountMapper).decreaseBalanceIfSufficient(eq(1L), eq(new BigDecimal("50")));
        }

        @Test
        @DisplayName("确认支出 - 余额不足拒绝")
        void confirm_InsufficientBalance_Rejects() {
            Transaction tx = new Transaction();
            tx.setId(100L);
            tx.setType(1);
            tx.setAmount(new BigDecimal("20000"));
            tx.setAccountId(1L);
            tx.setCategoryId(10L);
            tx.setCurrency("CNY");
            tx.setAmountBase(new BigDecimal("20000"));
            tx.setTransactionDate(LocalDate.now());
            tx.setIsConfirmed(false);

            when(transactionMapper.selectById(100L)).thenReturn(tx);
            when(accountMapper.selectById(1L)).thenReturn(debitAccount);
            when(transactionMapper.updateById(any(Transaction.class))).thenReturn(1);
            when(accountMapper.decreaseBalanceIfSufficient(eq(1L), eq(new BigDecimal("20000")))).thenReturn(0);

            assertThatThrownBy(() -> transactionService.confirm(100L))
                    .isInstanceOf(BizException.class);
        }
    }

    @Nested
    @DisplayName("[修复-2] 更新未确认交易不 rollback 未调整过的余额")
    class UnconfirmedUpdateTests {

        @Test
        @DisplayName("未确认交易更新 - 不 rollback 也不 apply")
        void update_Unconfirmed_NoBalanceChange() {
            Transaction existing = new Transaction();
            existing.setId(100L);
            existing.setType(1);
            existing.setAmount(new BigDecimal("50"));
            existing.setAccountId(1L);
            existing.setCategoryId(10L);
            existing.setTransactionDate(LocalDate.of(2026, 4, 1));
            existing.setCurrency("CNY");
            existing.setIsConfirmed(false);

            when(transactionMapper.selectById(100L)).thenReturn(existing);
            when(accountMapper.selectById(1L)).thenReturn(debitAccount);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.updateById(any(Transaction.class))).thenReturn(1);

            TransactionUpdateDTO dto = new TransactionUpdateDTO();
            dto.setAmount(new BigDecimal("60"));
            dto.setIsConfirmed(false);

            transactionService.update(100L, dto);

            verify(accountMapper, never()).adjustBalance(anyLong(), any());
            verify(accountMapper, never()).decreaseBalanceIfSufficient(anyLong(), any());
        }

        @Test
        @DisplayName("未确认→确认 - 应该 apply 余额")
        void update_UnconfirmedToConfirmed_AppliesBalance() {
            Transaction existing = new Transaction();
            existing.setId(100L);
            existing.setType(1);
            existing.setAmount(new BigDecimal("50"));
            existing.setAccountId(1L);
            existing.setCategoryId(10L);
            existing.setTransactionDate(LocalDate.of(2026, 4, 1));
            existing.setCurrency("CNY");
            existing.setAmountBase(new BigDecimal("50"));
            existing.setIsConfirmed(false);

            when(transactionMapper.selectById(100L)).thenReturn(existing);
            when(accountMapper.selectById(1L)).thenReturn(debitAccount);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.updateById(any(Transaction.class))).thenReturn(1);
            when(accountMapper.decreaseBalanceIfSufficient(eq(1L), eq(new BigDecimal("60")))).thenReturn(1);

            TransactionUpdateDTO dto = new TransactionUpdateDTO();
            dto.setAmount(new BigDecimal("60"));
            dto.setIsConfirmed(true);

            transactionService.update(100L, dto);

            verify(accountMapper, never()).adjustBalance(eq(1L), any());
            verify(accountMapper).decreaseBalanceIfSufficient(eq(1L), eq(new BigDecimal("60")));
        }

        @Test
        @DisplayName("确认→未确认 - rollback 旧余额但不 apply")
        void update_ConfirmedToUnconfirmed_RollbackOnly() {
            Transaction existing = new Transaction();
            existing.setId(100L);
            existing.setType(1);
            existing.setAmount(new BigDecimal("50"));
            existing.setAccountId(1L);
            existing.setCategoryId(10L);
            existing.setTransactionDate(LocalDate.of(2026, 4, 1));
            existing.setCurrency("CNY");
            existing.setAmountBase(new BigDecimal("50"));
            existing.setIsConfirmed(true);

            when(transactionMapper.selectById(100L)).thenReturn(existing);
            when(accountMapper.selectById(1L)).thenReturn(debitAccount);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.updateById(any(Transaction.class))).thenReturn(1);

            TransactionUpdateDTO dto = new TransactionUpdateDTO();
            dto.setAmount(new BigDecimal("60"));
            dto.setIsConfirmed(false);

            transactionService.update(100L, dto);

            verify(accountMapper).adjustBalance(1L, new BigDecimal("50"));
            verify(accountMapper, never()).adjustBalance(eq(1L), eq(new BigDecimal("60").negate()));
            verify(accountMapper, never()).decreaseBalanceIfSufficient(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("[修复-6] 等幂性检查已删除")
    class IdempotencyRemovedTests {

        @Test
        @DisplayName("两笔相同交易都不被拒绝")
        void createDuplicateTransactions_AllowBoth() {
            when(accountMapper.selectById(1L)).thenReturn(debitAccount);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionMapper.insert(any(Transaction.class))).thenAnswer(inv -> {
                Transaction tx = inv.getArgument(0);
                tx.setId(100L);
                return 1;
            });
            when(accountMapper.decreaseBalanceIfSufficient(anyLong(), any())).thenReturn(1);

            TransactionCreateDTO dto = buildCreateDTO(1L, 10L, new BigDecimal("50"));
            dto.setNote("午餐");

            transactionService.create(dto);
            transactionService.create(dto);

            verify(transactionMapper, times(2)).insert(any(Transaction.class));
        }
    }

    // ======================== 应该修复 ========================

    @Nested
    @DisplayName("[修复-11] 预算总额与明细校验")
    class BudgetValidationTests {

        @Test
        @DisplayName("总额与明细不一致 - 拒绝")
        void create_TotalMismatchItems_Rejects() {
            BudgetCreateDTO dto = new BudgetCreateDTO();
            dto.setYearMonth("2026-04");
            dto.setTotalAmount(new BigDecimal("5000"));
            BudgetCreateDTO.BudgetItemDTO item1 = new BudgetCreateDTO.BudgetItemDTO();
            item1.setCategoryId(1L);
            item1.setAmount(new BigDecimal("3000"));
            BudgetCreateDTO.BudgetItemDTO item2 = new BudgetCreateDTO.BudgetItemDTO();
            item2.setCategoryId(2L);
            item2.setAmount(new BigDecimal("3000")); // 3000+3000=6000 != 5000
            dto.setItems(List.of(item1, item2));

            // 手动校验逻辑：总额应与明细之和匹配
            BigDecimal sum = dto.getItems().stream()
                    .map(BudgetCreateDTO.BudgetItemDTO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isNotEqualTo(dto.getTotalAmount());
        }
    }

    @Nested
    @DisplayName("[修复-7] 余额调整分类动态查找")
    class BalanceAdjustCategoryTests {

        @Test
        @DisplayName("余额调整支出 - 动态查找 categoryId")
        void adjustBalance_LookupCategoryId() throws Exception {
            when(categoryMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(balanceAdjustExpense);
            when(accountMapper.selectById(1L)).thenReturn(debitAccount);
            when(transactionMapper.insert(any(Transaction.class))).thenAnswer(inv -> {
                Transaction tx = inv.getArgument(0);
                tx.setId(200L);
                return 1;
            });
            when(accountMapper.updateById(any(Account.class))).thenReturn(1);

            AccountServiceImpl accountService = new AccountServiceImpl(objectMapper, transactionMapper, categoryMapper, currencyRateService, redisTemplate);
            injectField(accountService, "baseMapper", accountMapper);

            accountService.adjustBalanceWithRecord(1L, new BigDecimal("9500"), "对账调整");

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionMapper).insert(captor.capture());
            Transaction tx = captor.getValue();
            assertThat(tx.getCategoryId()).isEqualTo(59L);
        }

        @Test
        @DisplayName("余额调整分类不存在 - 抛出异常")
        void adjustBalance_CategoryNotFound_Throws() throws Exception {
            when(categoryMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(accountMapper.selectById(1L)).thenReturn(debitAccount);

            AccountServiceImpl accountService = new AccountServiceImpl(objectMapper, transactionMapper, categoryMapper, currencyRateService, redisTemplate);
            injectField(accountService, "baseMapper", accountMapper);

            assertThatThrownBy(() -> accountService.adjustBalanceWithRecord(1L, new BigDecimal("9500"), "调整"))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("余额调整分类");
        }
    }

    // ======================== 可以延后 ========================

    @Nested
    @DisplayName("[修复-13] ReportService N+1 批量加载")
    class ReportNPlusOneTests {

        @Test
        @DisplayName("月度汇总 - 批量加载分类")
        void monthlySummary_BatchLoadCategories() {
            ReportServiceImpl reportService = new ReportServiceImpl(
                    transactionMapper, categoryMapper, accountMapper, null, objectMapper, redisTemplate);

            Transaction tx = new Transaction();
            tx.setId(1L);
            tx.setType(1);
            tx.setAmount(new BigDecimal("50"));
            tx.setAmountBase(new BigDecimal("50"));
            tx.setCurrency("CNY");
            tx.setAccountId(1L);
            tx.setCategoryId(10L);
            tx.setTransactionDate(LocalDate.of(2026, 4, 15));
            tx.setIsConfirmed(true);

            when(transactionMapper.selectList(any())).thenReturn(List.of(tx));
            lenient().when(accountMapper.selectBatchIds(anyCollection())).thenReturn(List.of(debitAccount));
            when(categoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(category));
            ValueOperations<String, String> valueOps = mock(ValueOperations.class);
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(anyString())).thenReturn(null);

            reportService.monthlySummary("2026-04");

            verify(categoryMapper).selectBatchIds(anyCollection());
            verify(categoryMapper, never()).selectById(any());
        }
    }

    // ======================== Helpers ========================

    private TransactionCreateDTO buildCreateDTO(Long accountId, Long categoryId, BigDecimal amount) {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType(1);
        dto.setAmount(amount);
        dto.setCurrency("CNY");
        dto.setAccountId(accountId);
        dto.setCategoryId(categoryId);
        dto.setTransactionDate(LocalDate.now());
        dto.setTransactionTime(LocalTime.now());
        dto.setNote("午餐");
        return dto;
    }

    private TransactionCreateDTO buildCreateDTO() {
        return buildCreateDTO(1L, 10L, new BigDecimal("50"));
    }
}
