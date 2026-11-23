package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import uk.gubin.budgetpilot.common.TenantContext;
import uk.gubin.budgetpilot.entity.AlertRule;
import uk.gubin.budgetpilot.entity.PrepaidCard;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.mapper.UserMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
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

class ScheduledTasksPrepaidCardTest {

    private final AlertRuleService alertRuleService = mock(AlertRuleService.class);
    private final AlertLogService alertLogService = mock(AlertLogService.class);
    private final PrepaidCardService prepaidCardService = mock(PrepaidCardService.class);
    private final UserMapper userMapper = mock(UserMapper.class);

    private ScheduledTasks tasks() {
        return new ScheduledTasks(
                mock(CurrencyRateService.class),
                mock(BudgetService.class),
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
                prepaidCardService);
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("到期前 N 天、到期当天、过期次日的卡券会汇总成一条提醒")
    @SuppressWarnings("unchecked")
    void sendsDigestOnMilestones() {
        oneUser();
        when(alertRuleService.list(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(rule(10L, 10, "WHATSAPP")));

        LocalDate today = LocalDate.now();
        when(prepaidCardService.listAll()).thenReturn(List.of(
                card("星礼卡", "星巴克", 1, "100", today.plusDays(30), 1),
                card("电影票", "示例影院", 2, "6", today, 1),
                card("过期购物卡", "示例超市", 1, "50", today.minusDays(1), 1),
                // 以下都不该提醒
                card("还很远的卡", "示例商户", 1, "10", today.plusDays(31), 1),
                card("已用完", "示例商户", 1, "0", today.plusDays(3), 1),
                card("已作废", "示例商户", 1, "10", today.plusDays(3), 2),
                card("长期有效", "示例商户", 1, "10", null, 1)
        ));

        tasks().checkPrepaidCardExpiry();

        verify(alertLogService).logAndNotify(
                eq(10L), eq(10), eq("🎟️ 卡券到期提醒"),
                contains("还有 30 天到期"), eq("WHATSAPP"));
        verify(alertLogService).logAndNotify(
                eq(10L), eq(10), eq("🎟️ 卡券到期提醒"),
                contains("今天到期"), eq("WHATSAPP"));
        verify(alertLogService).logAndNotify(
                eq(10L), eq(10), eq("🎟️ 卡券到期提醒"),
                contains("已过期 1 天"), eq("WHATSAPP"));
        verify(alertLogService).logAndNotify(
                eq(10L), eq(10), eq("🎟️ 卡券到期提醒"),
                contains("剩余 6 次/张"), eq("WHATSAPP"));
    }

    @Test
    @DisplayName("没有启用规则时不扫描卡券")
    @SuppressWarnings("unchecked")
    void skipsWhenRuleMissing() {
        oneUser();
        when(alertRuleService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        tasks().checkPrepaidCardExpiry();

        verify(prepaidCardService, never()).listAll();
        verifyNoInteractions(alertLogService);
    }

    @Test
    @DisplayName("没有命中的卡券时不发消息")
    @SuppressWarnings("unchecked")
    void skipsWhenNoCardMatches() {
        oneUser();
        when(alertRuleService.list(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(rule(10L, 10, "WHATSAPP")));
        when(prepaidCardService.listAll()).thenReturn(List.of(
                card("长期有效", "示例商户", 1, "10", null, 1),
                card("还很远", "示例商户", 1, "10", LocalDate.now().plusDays(31), 1)
        ));

        tasks().checkPrepaidCardExpiry();

        verify(alertLogService, never()).logAndNotify(
                any(), any(), anyString(), anyString(), anyString());
    }

    private void oneUser() {
        User user = new User();
        user.setId(2L);
        user.setIsActive(true);
        when(userMapper.selectList(any())).thenReturn(List.of(user));
    }

    private AlertRule rule(Long id, int type, String channel) {
        AlertRule rule = new AlertRule();
        rule.setId(id);
        rule.setType(type);
        rule.setIsActive(true);
        rule.setConfig("{\"advance_days\":30}");
        rule.setNotifyChannel(channel);
        return rule;
    }

    private PrepaidCard card(String name, String issuer, int unit, String balance,
                             LocalDate expiry, int status) {
        PrepaidCard card = new PrepaidCard();
        card.setName(name);
        card.setIssuer(issuer);
        card.setUnit(unit);
        card.setBalance(new BigDecimal(balance));
        card.setExpiryDate(expiry);
        card.setStatus(status);
        return card;
    }
}