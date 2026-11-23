package uk.gubin.budgetpilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import uk.gubin.budgetpilot.common.TenantContext;
import uk.gubin.budgetpilot.entity.RecurringRule;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.mapper.UserMapper;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledTasksRecurringTest {

    private final RecurringRuleService recurringRuleService = mock(RecurringRuleService.class);
    private final UserMapper userMapper = mock(UserMapper.class);

    private ScheduledTasks tasks() {
        return new ScheduledTasks(
                mock(CurrencyRateService.class),
                mock(BudgetService.class),
                recurringRuleService,
                mock(AlertRuleService.class),
                mock(AccountMapper.class),
                mock(TransactionMapper.class),
                mock(CategoryMapper.class),
                mock(AlertLogService.class),
                mock(AccountSnapshotService.class),
                mock(ConfigService.class),
                mock(ApplicationEventPublisher.class),
                new ObjectMapper(),
                userMapper,
                mock(PrepaidCardService.class));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private void oneActiveUser() {
        User user = new User();
        user.setId(2L);
        user.setIsActive(true);
        when(userMapper.selectList(any())).thenReturn(List.of(user));
    }

    private static RecurringRule dailyRule(LocalDate next) {
        RecurringRule rule = new RecurringRule();
        rule.setId(5L);
        rule.setUserId(2L);
        rule.setName("每日测试");
        rule.setFrequency("DAILY");
        rule.setNextExecute(next);
        rule.setIsActive(true);
        return rule;
    }

    @Test
    @DisplayName("漏跑多天会按计划执行日逐次补发")
    void catchesUpEachMissedOccurrence() {
        oneActiveUser();
        LocalDate start = LocalDate.now().minusDays(2);
        RecurringRule rule = dailyRule(start);
        when(recurringRuleService.getRulesDueToday()).thenReturn(List.of(rule));
        when(recurringRuleService.getById(5L)).thenReturn(rule);
        doAnswer(invocation -> {
            RecurringRule current = invocation.getArgument(0);
            current.setLastExecuted(current.getNextExecute());
            current.setNextExecute(current.getNextExecute().plusDays(1));
            return null;
        }).when(recurringRuleService).updateNextExecute(any(RecurringRule.class));

        tasks().processRecurringRules();

        verify(recurringRuleService, times(3)).generateTransaction(rule);
        verify(recurringRuleService, times(3)).updateNextExecute(rule);
    }

    @Test
    @DisplayName("生成失败时不推进 nextExecute，留待下次重试")
    void failureDoesNotAdvanceSchedule() {
        oneActiveUser();
        RecurringRule rule = dailyRule(LocalDate.now());
        when(recurringRuleService.getRulesDueToday()).thenReturn(List.of(rule));
        doThrow(new RuntimeException("boom"))
                .when(recurringRuleService).generateTransaction(any(RecurringRule.class));

        tasks().processRecurringRules();

        verify(recurringRuleService).generateTransaction(rule);
        verify(recurringRuleService, never()).updateNextExecute(any(RecurringRule.class));
    }

    @Test
    @DisplayName("截止日期当天的最后一期会补发，之后停止")
    void stopsAtEndDate() {
        oneActiveUser();
        RecurringRule rule = dailyRule(LocalDate.now().minusDays(1));
        rule.setEndDate(LocalDate.now());
        when(recurringRuleService.getRulesDueToday()).thenReturn(List.of(rule));
        when(recurringRuleService.getById(5L)).thenReturn(rule);
        doAnswer(invocation -> {
            RecurringRule current = invocation.getArgument(0);
            current.setLastExecuted(current.getNextExecute());
            current.setNextExecute(current.getNextExecute().plusDays(1));
            return null;
        }).when(recurringRuleService).updateNextExecute(any(RecurringRule.class));

        tasks().processRecurringRules();

        verify(recurringRuleService, times(2)).generateTransaction(rule);
    }
}