package uk.gubin.budgetpilot.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gubin.budgetpilot.dto.TransactionCreateDTO;
import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.MerchantMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.service.AccountService;
import uk.gubin.budgetpilot.service.BudgetService;
import uk.gubin.budgetpilot.service.CurrencyRateService;
import uk.gubin.budgetpilot.service.MerchantService;
import uk.gubin.budgetpilot.vo.TransactionVO;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 外部幂等 ID 行为测试。
 *
 * 原有测试类无法正常运行（MyBatis-Plus 3.5.9 的 baseMapper 不由 @InjectMocks 注入），
 * 这里显式构造 service 并通过 ReflectionTestUtils 注入 baseMapper。
 */
@ExtendWith(MockitoExtension.class)
class ExternalIdIdempotencyTest {

    @Mock private TransactionMapper transactionMapper;
    @Mock private AccountMapper accountMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private AccountService accountService;
    @Mock private BudgetService budgetService;
    @Mock private MerchantService merchantService;
    @Mock private CurrencyRateService currencyRateService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private StringRedisTemplate redisTemplate;

    private TransactionServiceImpl service;
    private Account account;
    private Category category;
    private Transaction existing;

    @BeforeEach
    void setUp() {
        service = new TransactionServiceImpl(
                new ObjectMapper(), currencyRateService, accountMapper, categoryMapper,
                merchantMapper, accountService, budgetService, merchantService,
                eventPublisher, redisTemplate);
        ReflectionTestUtils.setField(service, "baseMapper", transactionMapper);

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
        category.setName("工资");
        category.setType(2);
        category.setIsActive(true);

        existing = new Transaction();
        existing.setId(5L);
        existing.setExternalId("cmb:100:0");
        existing.setType(2);
        existing.setAmount(new BigDecimal("100.00"));
        existing.setCurrency("CNY");
        existing.setAmountBase(new BigDecimal("100.00"));
        existing.setAccountId(1L);
        existing.setCategoryId(10L);
        existing.setTransactionDate(LocalDate.of(2026, 4, 19));
        existing.setIsConfirmed(true);
    }

    private TransactionCreateDTO dto() {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType(2);
        dto.setAmount(new BigDecimal("100.00"));
        dto.setAccountId(1L);
        dto.setCategoryId(10L);
        dto.setTransactionDate(LocalDate.of(2026, 4, 19));
        dto.setExternalId("cmb:100:0");
        return dto;
    }

    @Test
    @DisplayName("相同 externalId 直接返回已有交易，不重复插入")
    void duplicateExternalIdReturnsExisting() {
        when(transactionMapper.selectOne(any())).thenReturn(existing);
        when(transactionMapper.selectById(5L)).thenReturn(existing);
        when(accountMapper.selectById(1L)).thenReturn(account);
        when(categoryMapper.selectById(10L)).thenReturn(category);

        TransactionVO vo = service.create(dto());

        assertThat(vo.getId()).isEqualTo(5L);
        assertThat(vo.getExternalId()).isEqualTo("cmb:100:0");
        verify(transactionMapper, never()).insert(any(Transaction.class));
        verify(accountMapper, never()).adjustBalance(any(), any());
    }

    @Test
    @DisplayName("并发穿越幂等预查时由唯一索引兜底，仍返回已有交易")
    void concurrentInsertFallsBackToUniqueIndex() {
        when(transactionMapper.selectOne(any())).thenReturn(null, existing);
        when(transactionMapper.selectById(5L)).thenReturn(existing);
        when(accountMapper.selectById(1L)).thenReturn(account);
        when(categoryMapper.selectById(10L)).thenReturn(category);
        when(transactionMapper.insert(any(Transaction.class)))
                .thenThrow(new DuplicateKeyException("uk_user_external"));

        TransactionVO vo = service.create(dto());

        assertThat(vo.getId()).isEqualTo(5L);
        verify(transactionMapper, times(1)).insert(any(Transaction.class));
        verify(accountMapper, never()).adjustBalance(any(), any());
    }
}