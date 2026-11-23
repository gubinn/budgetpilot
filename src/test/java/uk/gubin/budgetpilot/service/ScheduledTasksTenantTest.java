package uk.gubin.budgetpilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import uk.gubin.budgetpilot.common.TenantContext;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.mapper.UserMapper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduledTasksTenantTest {

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("定时任务按活跃用户循环，并在每次执行时设置/清理租户上下文")
    @SuppressWarnings("unchecked")
    void forEachActiveUserSetsTenantContext() throws Exception {
        UserMapper userMapper = mock(UserMapper.class);
        User u1 = new User();
        u1.setId(10L);
        u1.setIsActive(true);
        User u2 = new User();
        u2.setId(20L);
        u2.setIsActive(true);
        when(userMapper.selectList(any())).thenReturn(List.of(u1, u2));

        ScheduledTasks tasks = new ScheduledTasks(
                mock(CurrencyRateService.class),
                mock(BudgetService.class),
                mock(RecurringRuleService.class),
                mock(AlertRuleService.class),
                mock(AccountMapper.class),
                mock(TransactionMapper.class),
                mock(CategoryMapper.class),
                mock(AlertLogService.class),
                mock(AccountSnapshotService.class),
                mock(ConfigService.class),
                mock(ApplicationEventPublisher.class),
                mock(ObjectMapper.class),
                userMapper,
                mock(PrepaidCardService.class));

        List<Long> seen = new ArrayList<>();
        Method method = ScheduledTasks.class.getDeclaredMethod("forEachActiveUser", Consumer.class);
        method.setAccessible(true);
        method.invoke(tasks, (Consumer<Long>) userId -> {
            seen.add(userId);
            assertThat(TenantContext.get()).isEqualTo(userId);
        });

        assertThat(seen).containsExactly(10L, 20L);
        assertThat(TenantContext.get()).isNull();
    }
}