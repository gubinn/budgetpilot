package uk.gubin.budgetpilot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.mapper.AccountBalanceSnapshotMapper;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.AlertLogMapper;
import uk.gubin.budgetpilot.mapper.AlertRuleMapper;
import uk.gubin.budgetpilot.mapper.AnnualBudgetItemMapper;
import uk.gubin.budgetpilot.mapper.AnnualBudgetMapper;
import uk.gubin.budgetpilot.mapper.BudgetItemMapper;
import uk.gubin.budgetpilot.mapper.BudgetMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.MerchantAliasMapper;
import uk.gubin.budgetpilot.mapper.MerchantMapper;
import uk.gubin.budgetpilot.mapper.PrepaidCardMapper;
import uk.gubin.budgetpilot.mapper.PrepaidCardUsageMapper;
import uk.gubin.budgetpilot.mapper.ReadonlyShareMapper;
import uk.gubin.budgetpilot.mapper.ReconciliationBatchMapper;
import uk.gubin.budgetpilot.mapper.ReconciliationItemMapper;
import uk.gubin.budgetpilot.mapper.RecurringRuleMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.mapper.UserConfigMapper;
import uk.gubin.budgetpilot.mapper.UserMapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceDeleteCleanupTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserConfigMapper userConfigMapper = mock(UserConfigMapper.class);
    private final CategoryMapper categoryMapper = mock(CategoryMapper.class);
    private final AlertRuleMapper alertRuleMapper = mock(AlertRuleMapper.class);
    private final BudgetMapper budgetMapper = mock(BudgetMapper.class);
    private final BudgetItemMapper budgetItemMapper = mock(BudgetItemMapper.class);
    private final AnnualBudgetMapper annualBudgetMapper = mock(AnnualBudgetMapper.class);
    private final AnnualBudgetItemMapper annualBudgetItemMapper = mock(AnnualBudgetItemMapper.class);
    private final TransactionMapper transactionMapper = mock(TransactionMapper.class);
    private final AccountMapper accountMapper = mock(AccountMapper.class);
    private final MerchantAliasMapper merchantAliasMapper = mock(MerchantAliasMapper.class);
    private final MerchantMapper merchantMapper = mock(MerchantMapper.class);
    private final RecurringRuleMapper recurringRuleMapper = mock(RecurringRuleMapper.class);
    private final AlertLogMapper alertLogMapper = mock(AlertLogMapper.class);
    private final AccountBalanceSnapshotMapper snapshotMapper = mock(AccountBalanceSnapshotMapper.class);
    private final ReconciliationItemMapper reconciliationItemMapper = mock(ReconciliationItemMapper.class);
    private final ReconciliationBatchMapper reconciliationBatchMapper = mock(ReconciliationBatchMapper.class);
    private final PrepaidCardMapper prepaidCardMapper = mock(PrepaidCardMapper.class);
    private final PrepaidCardUsageMapper prepaidCardUsageMapper = mock(PrepaidCardUsageMapper.class);
    private final ReadonlyShareMapper readonlyShareMapper = mock(ReadonlyShareMapper.class);

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(
                userConfigMapper,
                categoryMapper,
                alertRuleMapper,
                budgetMapper,
                budgetItemMapper,
                annualBudgetMapper,
                annualBudgetItemMapper,
                transactionMapper,
                accountMapper,
                merchantAliasMapper,
                merchantMapper,
                recurringRuleMapper,
                alertLogMapper,
                snapshotMapper,
                reconciliationItemMapper,
                reconciliationBatchMapper,
                prepaidCardMapper,
                prepaidCardUsageMapper,
                readonlyShareMapper,
                mock(ConfigService.class),
                mock(StringRedisTemplate.class));
        // UserService 继承 ServiceImpl，baseMapper 不是构造器参数
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
    }

    @Test
    @DisplayName("删除用户时清理对账、净资产快照和卡券数据")
    void deleteCleansAllUserScopedTables() {
        User user = new User();
        user.setId(7L);
        user.setUsername("user-to-delete");
        when(userMapper.selectById(7L)).thenReturn(user);

        service.delete(7L);

        verify(alertLogMapper).deleteByUserId(7L);
        verify(alertRuleMapper).deleteByUserId(7L);
        verify(recurringRuleMapper).deleteByUserId(7L);
        verify(budgetItemMapper).deleteByUserId(7L);
        verify(budgetMapper).deleteByUserId(7L);
        verify(annualBudgetItemMapper).deleteByUserId(7L);
        verify(annualBudgetMapper).deleteByUserId(7L);
        verify(reconciliationItemMapper).deleteByUserId(7L);
        verify(reconciliationBatchMapper).deleteByUserId(7L);
        verify(transactionMapper).deleteByUserId(7L);
        verify(prepaidCardUsageMapper).deleteByUserId(7L);
        verify(prepaidCardMapper).deleteByUserId(7L);
        verify(merchantAliasMapper).deleteByUserId(7L);
        verify(merchantMapper).deleteByUserId(7L);
        verify(accountMapper).deleteByUserId(7L);
        verify(snapshotMapper).deleteByUserId(7L);
        verify(readonlyShareMapper).deleteByUserId(7L);
        verify(categoryMapper).deleteByUserId(7L);
        verify(userConfigMapper).deleteByUserId(7L);
        verify(userMapper).deleteById(7L);
    }
}