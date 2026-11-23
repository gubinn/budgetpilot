package uk.gubin.budgetpilot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.dto.RecurringRuleCreateDTO;
import uk.gubin.budgetpilot.dto.TransactionCreateDTO;
import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.RecurringRule;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.RecurringRuleMapper;
import uk.gubin.budgetpilot.service.impl.RecurringRuleServiceImpl;
import uk.gubin.budgetpilot.vo.TransactionVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecurringRuleScheduleTest {

    private final TransactionService transactionService = mock(TransactionService.class);
    private final AccountMapper accountMapper = mock(AccountMapper.class);
    private final CategoryMapper categoryMapper = mock(CategoryMapper.class);
    private final RecurringRuleMapper recurringRuleMapper = mock(RecurringRuleMapper.class);

    private RecurringRuleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RecurringRuleServiceImpl(transactionService, accountMapper, categoryMapper);
        ReflectionTestUtils.setField(service, "baseMapper", recurringRuleMapper);
    }

    private static RecurringRule monthlyRule(LocalDate next, int executeDay) {
        RecurringRule rule = new RecurringRule();
        rule.setId(7L);
        rule.setUserId(2L);
        rule.setName("房租");
        rule.setType(1);
        rule.setAmount(new BigDecimal("2000"));
        rule.setCurrency("CNY");
        rule.setAccountId(1L);
        rule.setCategoryId(2L);
        rule.setFrequency("MONTHLY");
        rule.setExecuteDay(executeDay);
        rule.setStartDate(LocalDate.of(2026, 1, 1));
        rule.setNextExecute(next);
        rule.setAutoConfirm(false);
        rule.setIsActive(true);
        return rule;
    }

    @Test
    @DisplayName("月末规则不会从 31 号漂移到 28 号")
    void monthlyRuleKeepsMonthEnd() {
        RecurringRule rule = monthlyRule(LocalDate.of(2026, 1, 31), 31);
        service.updateNextExecute(rule);

        assertThat(rule.getLastExecuted()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(rule.getNextExecute()).isEqualTo(LocalDate.of(2026, 2, 28));

        service.updateNextExecute(rule);
        assertThat(rule.getNextExecute()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    @DisplayName("闰日年规则在平年落 2/28，闰年回到 2/29")
    void yearlyLeapDayReturnsToFeb29() {
        RecurringRule rule = monthlyRule(LocalDate.of(2024, 2, 29), 0);
        rule.setFrequency("YEARLY");
        rule.setStartDate(LocalDate.of(2024, 2, 29));
        rule.setExecuteDay(null);

        service.updateNextExecute(rule);
        assertThat(rule.getNextExecute()).isEqualTo(LocalDate.of(2025, 2, 28));
        service.updateNextExecute(rule);
        assertThat(rule.getNextExecute()).isEqualTo(LocalDate.of(2026, 2, 28));
        service.updateNextExecute(rule);
        assertThat(rule.getNextExecute()).isEqualTo(LocalDate.of(2027, 2, 28));
        service.updateNextExecute(rule);
        assertThat(rule.getNextExecute()).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    @DisplayName("新建月规则会按执行日推到不早于开始日的第一天")
    void createAlignsMonthlyExecuteDay() {
        RecurringRuleCreateDTO dto = new RecurringRuleCreateDTO();
        dto.setName("房租");
        dto.setType(1);
        dto.setAmount(new BigDecimal("2000"));
        dto.setAccountId(1L);
        dto.setCategoryId(2L);
        dto.setFrequency("MONTHLY");
        dto.setExecuteDay(25);
        dto.setStartDate(LocalDate.of(2026, 11, 5));

        RecurringRule created = service.create(dto);

        assertThat(created.getNextExecute()).isEqualTo(LocalDate.of(2026, 11, 25));
        assertThat(created.getExecuteDay()).isEqualTo(25);
        verify(recurringRuleMapper).insert(created);
    }

    @Test
    @DisplayName("开始日晚于本月执行日时，首次执行顺延到下月")
    void createAlignsToNextMonth() {
        RecurringRuleCreateDTO dto = new RecurringRuleCreateDTO();
        dto.setName("房租");
        dto.setType(1);
        dto.setAmount(new BigDecimal("2000"));
        dto.setAccountId(1L);
        dto.setCategoryId(2L);
        dto.setFrequency("MONTHLY");
        dto.setExecuteDay(25);
        dto.setStartDate(LocalDate.of(2026, 11, 26));

        RecurringRule created = service.create(dto);

        assertThat(created.getNextExecute()).isEqualTo(LocalDate.of(2026, 12, 25));
    }

    @Test
    @DisplayName("计划执行生成的交易使用执行日作为交易日期，并带幂等键")
    void scheduledGenerationUsesDueDateAndExternalId() {
        RecurringRule rule = monthlyRule(LocalDate.of(2026, 11, 20), 20);
        Category category = new Category();
        category.setId(2L);
        when(categoryMapper.selectById(2L)).thenReturn(category);
        Account account = new Account();
        account.setId(1L);
        when(accountMapper.selectById(1L)).thenReturn(account);
        TransactionVO vo = new TransactionVO();
        vo.setId(100L);
        when(transactionService.create(any())).thenReturn(vo);

        service.generateTransaction(rule);

        ArgumentCaptor<TransactionCreateDTO> captor = ArgumentCaptor.forClass(TransactionCreateDTO.class);
        verify(transactionService).create(captor.capture());
        TransactionCreateDTO dto = captor.getValue();
        assertThat(dto.getTransactionDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(dto.getExternalId()).isEqualTo("recurring:7:2026-11-20");
        assertThat(dto.getIsRecurring()).isTrue();
        assertThat(dto.getRecurringId()).isEqualTo(7L);
        assertThat((String) ((Map<?, ?>) dto.getMetadata()).get("occurrence_date"))
                .isEqualTo("2026-11-20");
    }

    @Test
    @DisplayName("立即执行生成今天的交易，但不推进下次执行日期")
    void executeNowDoesNotChangeSchedule() {
        RecurringRule rule = monthlyRule(LocalDate.of(2026, 12, 25), 25);
        when(recurringRuleMapper.selectById(7L)).thenReturn(rule);
        Category category = new Category();
        category.setId(2L);
        when(categoryMapper.selectById(2L)).thenReturn(category);
        Account account = new Account();
        account.setId(1L);
        when(accountMapper.selectById(1L)).thenReturn(account);
        TransactionVO vo = new TransactionVO();
        vo.setId(101L);
        when(transactionService.create(any())).thenReturn(vo);

        service.executeNow(7L);

        ArgumentCaptor<TransactionCreateDTO> captor = ArgumentCaptor.forClass(TransactionCreateDTO.class);
        verify(transactionService).create(captor.capture());
        assertThat(captor.getValue().getTransactionDate()).isEqualTo(LocalDate.now());
        assertThat(captor.getValue().getExternalId())
                .isEqualTo("recurring:7:manual:" + LocalDate.now());
        assertThat(rule.getNextExecute()).isEqualTo(LocalDate.of(2026, 12, 25));
        verify(recurringRuleMapper, never()).updateById(any(RecurringRule.class));
    }

    @Test
    @DisplayName("停用规则不能立即执行")
    void executeNowRejectsInactiveRule() {
        RecurringRule rule = monthlyRule(LocalDate.of(2026, 12, 25), 25);
        rule.setIsActive(false);
        when(recurringRuleMapper.selectById(7L)).thenReturn(rule);

        assertThatThrownBy(() -> service.executeNow(7L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已停用");
        verify(transactionService, never()).create(any());
        verify(recurringRuleMapper, never()).updateById(any(RecurringRule.class));
    }
}