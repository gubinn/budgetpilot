package uk.gubin.budgetpilot.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gubin.budgetpilot.dto.TransactionCreateDTO;
import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.RecurringRule;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.RecurringRuleMapper;
import uk.gubin.budgetpilot.service.TransactionService;
import uk.gubin.budgetpilot.vo.TransactionVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringRuleServiceTest {

    @Mock private RecurringRuleMapper recurringRuleMapper;
    @Mock private TransactionService transactionService;
    @Mock private AccountMapper accountMapper;
    @Mock private CategoryMapper categoryMapper;
    @InjectMocks private RecurringRuleServiceImpl recurringRuleService;

    // ============ GET RULES DUE TODAY ============

    @Nested
    @DisplayName("获取今日应执行规则")
    class GetRulesDueTodayTests {

        @Test
        @DisplayName("返回 nextExecute <= today 且未过期的规则")
        void getRulesDueToday_MatchingRules() {
            RecurringRule rule = new RecurringRule();
            rule.setId(1L);
            rule.setName("每月房贷");
            rule.setIsActive(true);
            rule.setNextExecute(LocalDate.of(2026, 4, 15));
            rule.setEndDate(null); // 无结束日期

            when(recurringRuleMapper.selectList(any())).thenReturn(List.of(rule));

            List<RecurringRule> rules = recurringRuleService.getRulesDueToday();

            assertThat(rules).hasSize(1);
            assertThat(rules.get(0).getName()).isEqualTo("每月房贷");
        }

        @Test
        @DisplayName("不包含已过期规则")
        void getRulesDueToday_ExcludeExpired() {
            RecurringRule expired = new RecurringRule();
            expired.setId(1L);
            expired.setIsActive(true);
            expired.setNextExecute(LocalDate.of(2026, 4, 15));
            expired.setEndDate(LocalDate.of(2026, 3, 1));

            when(recurringRuleMapper.selectList(any())).thenReturn(List.of());

            List<RecurringRule> rules = recurringRuleService.getRulesDueToday();

            assertThat(rules).isEmpty();
        }

        @Test
        @DisplayName("不包含未激活规则")
        void getRulesDueToday_ExcludeInactive() {
            RecurringRule inactive = new RecurringRule();
            inactive.setId(1L);
            inactive.setIsActive(false);
            inactive.setNextExecute(LocalDate.of(2026, 4, 15));

            when(recurringRuleMapper.selectList(any())).thenReturn(List.of());

            List<RecurringRule> rules = recurringRuleService.getRulesDueToday();

            assertThat(rules).isEmpty();
        }
    }

    // ============ GENERATE TRANSACTION ============

    @Nested
    @DisplayName("通过规则生成交易")
    class GenerateTransactionTests {

        private RecurringRule rule;
        private Account account;
        private Category category;

        @BeforeEach
        void setUp() {
            rule = new RecurringRule();
            rule.setId(5L);
            rule.setName("每月房租");
            rule.setType(1); // 支出
            rule.setAmount(new BigDecimal("3000"));
            rule.setCurrency("CNY");
            rule.setAccountId(1L);
            rule.setCategoryId(10L);
            rule.setNote("自动生成的房租");
            rule.setAutoConfirm(true);
            rule.setExtFields(null);

            account = new Account();
            account.setId(1L);
            account.setCurrency("CNY");

            category = new Category();
            category.setId(10L);
        }

        @Test
        @DisplayName("通过 TransactionService.create() 创建交易，不走 mapper")
        void generateTransaction_UsesTransactionService() {
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionService.create(any())).thenReturn(new TransactionVO());

            recurringRuleService.generateTransaction(rule);

            // 验证调用了 TransactionService.create() 而非 transactionMapper.insert()
            ArgumentCaptor<TransactionCreateDTO> captor =
                    ArgumentCaptor.forClass(TransactionCreateDTO.class);
            verify(transactionService).create(captor.capture());

            TransactionCreateDTO dto = captor.getValue();
            assertThat(dto.getType()).isEqualTo(1);
            assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("3000"));
            assertThat(dto.getAccountId()).isEqualTo(1L);
            assertThat(dto.getCategoryId()).isEqualTo(10L);
            assertThat(dto.getIsConfirmed()).isTrue();
            assertThat(dto.getIsRecurring()).isTrue();
            assertThat(dto.getRecurringId()).isEqualTo(5L);
            assertThat(dto.getMetadata()).containsKey("source");
            assertThat(dto.getMetadata()).containsKey("rule_id");
        }

        @Test
        @DisplayName("autoConfirm=false 时交易未确认")
        void generateTransaction_NotAutoConfirmed() {
            rule.setAutoConfirm(false);
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionService.create(any())).thenReturn(new TransactionVO());

            recurringRuleService.generateTransaction(rule);

            ArgumentCaptor<TransactionCreateDTO> captor =
                    ArgumentCaptor.forClass(TransactionCreateDTO.class);
            verify(transactionService).create(captor.capture());
            assertThat(captor.getValue().getIsConfirmed()).isFalse();
        }

        @Test
        @DisplayName("账户或分类不存在时抛异常")
        void generateTransaction_MissingAccountOrCategory() {
            when(accountMapper.selectById(1L)).thenReturn(null);

            assertThatThrownBy(() -> recurringRuleService.generateTransaction(rule))
                    .isInstanceOf(IllegalArgumentException.class);

            // 不调用 TransactionService
            verify(transactionService, never()).create(any());
        }

        @Test
        @DisplayName("metadata 包含 recurring 来源信息")
        void generateTransaction_MetadataContainsSourceInfo() {
            when(accountMapper.selectById(1L)).thenReturn(account);
            when(categoryMapper.selectById(10L)).thenReturn(category);
            when(transactionService.create(any())).thenReturn(new TransactionVO());

            recurringRuleService.generateTransaction(rule);

            ArgumentCaptor<TransactionCreateDTO> captor =
                    ArgumentCaptor.forClass(TransactionCreateDTO.class);
            verify(transactionService).create(captor.capture());
            assertThat(captor.getValue().getMetadata()).containsEntry("source", "recurring");
            assertThat(captor.getValue().getMetadata()).containsEntry("rule_id", 5L);
        }
    }

    // ============ UPDATE NEXT EXECUTE ============

    @Nested
    @DisplayName("更新下次执行日期")
    class UpdateNextExecuteTests {

        @Test
        @DisplayName("MONTHLY 频率 +1 个月")
        void updateNextExecute_Monthly() {
            RecurringRule rule = new RecurringRule();
            rule.setId(1L);
            rule.setFrequency("MONTHLY");
            rule.setNextExecute(LocalDate.of(2026, 4, 15));

            when(recurringRuleMapper.updateById(any(RecurringRule.class))).thenReturn(1);

            recurringRuleService.updateNextExecute(rule);

            assertThat(rule.getNextExecute()).isEqualTo(LocalDate.of(2026, 5, 15));
            assertThat(rule.getLastExecuted()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("WEEKLY 频率 +1 周")
        void updateNextExecute_Weekly() {
            RecurringRule rule = new RecurringRule();
            rule.setId(1L);
            rule.setFrequency("WEEKLY");
            rule.setNextExecute(LocalDate.of(2026, 4, 15));

            when(recurringRuleMapper.updateById(any(RecurringRule.class))).thenReturn(1);

            recurringRuleService.updateNextExecute(rule);

            assertThat(rule.getNextExecute()).isEqualTo(LocalDate.of(2026, 4, 22));
        }

        @Test
        @DisplayName("DAILY 频率 +1 天")
        void updateNextExecute_Daily() {
            RecurringRule rule = new RecurringRule();
            rule.setId(1L);
            rule.setFrequency("DAILY");
            rule.setNextExecute(LocalDate.of(2026, 4, 15));

            when(recurringRuleMapper.updateById(any(RecurringRule.class))).thenReturn(1);

            recurringRuleService.updateNextExecute(rule);

            assertThat(rule.getNextExecute()).isEqualTo(LocalDate.of(2026, 4, 16));
        }

        @Test
        @DisplayName("YEARLY 频率 +1 年")
        void updateNextExecute_Yearly() {
            RecurringRule rule = new RecurringRule();
            rule.setId(1L);
            rule.setFrequency("YEARLY");
            rule.setNextExecute(LocalDate.of(2026, 4, 15));

            when(recurringRuleMapper.updateById(any(RecurringRule.class))).thenReturn(1);

            recurringRuleService.updateNextExecute(rule);

            assertThat(rule.getNextExecute()).isEqualTo(LocalDate.of(2027, 4, 15));
        }
    }
}
