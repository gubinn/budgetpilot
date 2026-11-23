package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import uk.gubin.budgetpilot.common.TenantContext;
import uk.gubin.budgetpilot.entity.AlertRule;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.mapper.UserMapper;
import uk.gubin.budgetpilot.vo.BudgetProgressVO;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ScheduledTasksAlertRuleTest {

    private final BudgetService budgetService = mock(BudgetService.class);
    private final AlertRuleService alertRuleService = mock(AlertRuleService.class);
    private final AlertLogService alertLogService = mock(AlertLogService.class);
    private final UserMapper userMapper = mock(UserMapper.class);

    private ScheduledTasks tasks() {
        return new ScheduledTasks(
                mock(CurrencyRateService.class),
                budgetService,
                mock(RecurringRuleService.class),
                alertRuleService,
                mock(AccountMapper.class),
                mock(TransactionMapper.class),
                mock(CategoryMapper.class),
                alertLogService,
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

    @Test
    @DisplayName("预算提醒使用规则里的阈值、规则 ID 和规则渠道")
    @SuppressWarnings("unchecked")
    void budgetProgressUsesRuleConfig() {
        User user = new User();
        user.setId(2L);
        user.setIsActive(true);
        when(userMapper.selectList(any())).thenReturn(List.of(user));

        AlertRule rule = new AlertRule();
        rule.setId(8L);
        rule.setType(1);
        rule.setIsActive(true);
        rule.setConfig("{\"threshold_pct\":80}");
        rule.setNotifyChannel("WHATSAPP");
        when(alertRuleService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(rule));

        BudgetProgressVO progress = new BudgetProgressVO();
        progress.setTotalBudget(new BigDecimal("1000"));
        progress.setTotalSpent(new BigDecimal("850"));
        progress.setRemaining(new BigDecimal("150"));
        progress.setProgressPct(new BigDecimal("85"));
        when(budgetService.exists(anyString())).thenReturn(true);
        when(budgetService.getProgress(anyString())).thenReturn(progress);

        tasks().checkBudgetProgress();

        verify(alertLogService).logAndNotify(
                eq(8L), eq(1), eq("📊 预算预警"),
                contains("预警阈值 80%"), eq("WHATSAPP"));
    }

    @Test
    @DisplayName("对应类型的规则停用或不存在时，定时提醒直接跳过")
    @SuppressWarnings("unchecked")
    void disabledRuleSkipsReminder() {
        User user = new User();
        user.setId(2L);
        user.setIsActive(true);
        when(userMapper.selectList(any())).thenReturn(List.of(user));
        when(alertRuleService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        tasks().checkBudgetProgress();

        verify(budgetService, never()).exists(anyString());
        verifyNoInteractions(alertLogService);
    }
}