package uk.gubin.budgetpilot.service.impl;

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
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.dto.AccountCreateDTO;
import uk.gubin.budgetpilot.dto.AccountUpdateDTO;
import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.vo.AccountVO;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountMapper accountMapper;
    @Mock private TransactionMapper transactionMapper;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        // 让 save() / updateById() 生效 —— 模拟 BaseMapper 的行为
        doAnswer(inv -> {
            Account acc = inv.getArgument(0);
            acc.setId(1L);
            return 1;
        }).when(accountMapper).insert(any(Account.class));
    }

    // ============ CREATE ============

    @Nested
    @DisplayName("创建账户")
    class CreateTests {

        @Test
        @DisplayName("正常创建 - currentBalance = initialBalance")
        void create_Normal() {
            AccountCreateDTO dto = buildCreateDTO();

            AccountVO vo = accountService.create(dto);

            assertThat(vo.getName()).isEqualTo("工资卡");
            assertThat(vo.getType()).isEqualTo(2);
            assertThat(vo.getInitialBalance()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(vo.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(vo.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("extFields 正确序列化")
        void create_WithExtFields() {
            AccountCreateDTO dto = buildCreateDTO();
            dto.setExtFields(Map.of("bank", "ICBC"));

            accountService.create(dto);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountMapper).insert(captor.capture());
            assertThat(captor.getValue().getExtFields()).contains("bank").contains("ICBC");
        }

        @Test
        @DisplayName("重复名称拒绝")
        void create_DuplicateName() {
            AccountCreateDTO dto = buildCreateDTO();
            when(accountMapper.selectCount(any())).thenReturn(1L);

            assertThatThrownBy(() -> accountService.create(dto))
                    .isInstanceOf(BizException.class);
        }
    }

    // ============ DELETE ============

    @Nested
    @DisplayName("删除账户")
    class DeleteTests {

        @Test
        @DisplayName("有关联交易 - 拒绝删除")
        void delete_WithTransactions() {
            Account acc = new Account();
            acc.setId(1L);
            acc.setName("工资卡");
            acc.setIsActive(true);
            when(accountMapper.selectById(1L)).thenReturn(acc);
            when(transactionMapper.selectCount(any())).thenReturn(5L);

            assertThatThrownBy(() -> accountService.delete(1L))
                    .isInstanceOf(BizException.class);

            verify(accountMapper, never()).updateById(any(Account.class));
        }

        @Test
        @DisplayName("无关联交易 - 逻辑删除（停用）")
        void delete_NoTransactions() {
            Account acc = new Account();
            acc.setId(1L);
            acc.setName("工资卡");
            acc.setIsActive(true);
            when(accountMapper.selectById(1L)).thenReturn(acc);
            when(transactionMapper.selectCount(any())).thenReturn(0L);

            accountService.delete(1L);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountMapper).updateById(captor.capture());
            assertThat(captor.getValue().getIsActive()).isFalse();
        }

        @Test
        @DisplayName("删除不存在的账户 - 抛异常")
        void delete_NotFound() {
            when(accountMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> accountService.delete(999L))
                    .isInstanceOf(BizException.class);
        }
    }

    // ============ UPDATE ============

    @Nested
    @DisplayName("更新账户")
    class UpdateTests {

        @Test
        @DisplayName("更新 metadata")
        void update_Metadata() {
            Account acc = new Account();
            acc.setId(1L);
            acc.setName("工资卡");
            acc.setCurrency("CNY");
            acc.setIsActive(true);
            when(accountMapper.selectById(1L)).thenReturn(acc);

            AccountUpdateDTO dto = new AccountUpdateDTO();
            dto.setMetadata(Map.of("note", "main account"));

            accountService.update(1L, dto);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountMapper).updateById(captor.capture());
            assertThat(captor.getValue().getMetadata()).contains("note");
        }
    }

    // ============ BALANCE ============

    @Nested
    @DisplayName("余额调整")
    class BalanceTests {

        @Test
        @DisplayName("调整余额 - 调用 mapper")
        void adjustBalance() {
            when(accountMapper.adjustBalance(1L, new BigDecimal("-100"))).thenReturn(1);

            accountService.adjustBalance(1L, new BigDecimal("-100"));

            verify(accountMapper).adjustBalance(1L, new BigDecimal("-100"));
        }

        @Test
        @DisplayName("调整不存在的账户 - 抛异常")
        void adjustBalance_NotFound() {
            when(accountMapper.adjustBalance(anyLong(), any())).thenReturn(0);

            assertThatThrownBy(() -> accountService.adjustBalance(999L, new BigDecimal("100")))
                    .isInstanceOf(BizException.class);
        }
    }

    // ============ HELPERS ============

    private AccountCreateDTO buildCreateDTO() {
        AccountCreateDTO dto = new AccountCreateDTO();
        dto.setName("工资卡");
        dto.setType(2);
        dto.setCurrency("CNY");
        dto.setInitialBalance(new BigDecimal("10000"));
        dto.setSortOrder(1);
        return dto;
    }
}
