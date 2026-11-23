package uk.gubin.budgetpilot.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Merchant;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.MerchantMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.vo.ReceivableGroupVO;
import uk.gubin.budgetpilot.vo.ReceivableOverviewVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReceivableServiceTest {

    @Mock private TransactionMapper transactionMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private AccountMapper accountMapper;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Spy private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks private ReceivableServiceImpl service;

    private Category parent;
    private Category lend;
    private Category collect;
    private Category borrow;
    private Category repay;
    private Merchant borrower;

    @BeforeEach
    void setUp() {
        parent = category(100L, 0L, "往来款", 1);
        lend = category(101L, 100L, "借出款", 1);
        collect = category(102L, 100L, "收回借出款", 2);
        borrow = category(103L, 100L, "借入款", 2);
        repay = category(104L, 100L, "归还借入款", 1);
        borrower = new Merchant();
        borrower.setId(44L);
        borrower.setName("张三");

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);
        lenient().when(categoryMapper.selectList(any())).thenReturn(
                List.of(parent, lend, collect, borrow, repay));
        lenient().when(categoryMapper.selectSystemDefaults()).thenReturn(List.of());
        lenient().when(merchantMapper.selectBatchIds(any())).thenReturn(List.of(borrower));

        Account account = new Account();
        account.setId(1L);
        account.setName("工资卡");
        lenient().when(accountMapper.selectBatchIds(any())).thenReturn(List.of(account));
    }

    @Test
    @DisplayName("部分收回：借出8000、收回3000，未结清5000")
    void partialRepayment() {
        whenTransactions(
                tx(1L, 1, lend.getId(), "8000", "2026-01-10", 44L, null, "借给张三"),
                tx(2L, 2, collect.getId(), "3000", "2026-03-15", 44L, null, "张三还款")
        );

        ReceivableOverviewVO result = service.buildOverview(2L, false);

        assertThat(result.getSummary().getReceivableTotal())
                .isEqualByComparingTo("5000.00");
        assertThat(result.getSummary().getPayableTotal()).isEqualByComparingTo("0.00");
        assertThat(result.getSummary().getCounterpartyCount()).isEqualTo(1);
        assertThat(result.getSummary().getOldestDays()).isEqualTo(
                ChronoUnit.DAYS.between(LocalDate.of(2026, 1, 10), LocalDate.now()));

        ReceivableGroupVO group = result.getGroups().get(0);
        assertThat(group.getDirection()).isEqualTo(ReceivableServiceImpl.DIR_RECEIVABLE);
        assertThat(group.getCounterparty()).isEqualTo("张三");
        assertThat(group.getPrincipalTotal()).isEqualByComparingTo("8000.00");
        assertThat(group.getSettledTotal()).isEqualByComparingTo("3000.00");
        assertThat(group.getOutstanding()).isEqualByComparingTo("5000.00");
        assertThat(group.getFirstOutstandingDate()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(group.getTransactions()).hasSize(2);
        // 明细按时间倒序，最近一笔在最前
        assertThat(group.getTransactions().get(0).getDate()).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    @DisplayName("没有商户时从银行扩展字段识别对方")
    void counterpartyFromExtFields() {
        whenTransactions(tx(1L, 1, lend.getId(), "800", "2026-05-01", null,
                "{\"招行储蓄卡对手信息\":\"张三\"}", "实时转账"));

        ReceivableOverviewVO result = service.buildOverview(2L, false);

        assertThat(result.getGroups()).hasSize(1);
        assertThat(result.getGroups().get(0).getCounterparty()).isEqualTo("张三");
        assertThat(result.getGroups().get(0).getOutstanding()).isEqualByComparingTo("800.00");
    }

    @Test
    @DisplayName("先进先出：还款优先冲减最早一笔借出")
    void fifoAppliesToOldestLoan() {
        whenTransactions(
                tx(1L, 1, lend.getId(), "100", "2026-01-01", 44L, null, "借出"),
                tx(2L, 1, lend.getId(), "200", "2026-02-01", 44L, null, "借出"),
                tx(3L, 2, collect.getId(), "150", "2026-03-01", 44L, null, "还款")
        );

        ReceivableGroupVO group = service.buildOverview(2L, false).getGroups().get(0);

        assertThat(group.getOutstanding()).isEqualByComparingTo("150.00");
        assertThat(group.getFirstOutstandingDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(group.getAgeDays()).isEqualTo(
                ChronoUnit.DAYS.between(LocalDate.of(2026, 2, 1), LocalDate.now()));
    }

    @Test
    @DisplayName("应付方向：借入1000、归还400，应付600")
    void payableDirection() {
        whenTransactions(
                tx(1L, 2, borrow.getId(), "1000", "2026-01-10", 44L, null, "借入"),
                tx(2L, 1, repay.getId(), "400", "2026-02-10", 44L, null, "归还")
        );

        ReceivableOverviewVO result = service.buildOverview(2L, false);

        assertThat(result.getSummary().getReceivableTotal()).isEqualByComparingTo("0.00");
        assertThat(result.getSummary().getPayableTotal()).isEqualByComparingTo("600.00");
        assertThat(result.getSummary().getNet()).isEqualByComparingTo("-600.00");
        ReceivableGroupVO group = result.getGroups().get(0);
        assertThat(group.getDirection()).isEqualTo(ReceivableServiceImpl.DIR_PAYABLE);
        assertThat(group.getOutstanding()).isEqualByComparingTo("600.00");
    }

    @Test
    @DisplayName("全部收回后未结清为0，账龄归零")
    void fullyRepaid() {
        whenTransactions(
                tx(1L, 1, lend.getId(), "1000", "2026-01-01", 44L, null, "借出"),
                tx(2L, 2, collect.getId(), "1000", "2026-02-01", 44L, null, "收回")
        );

        ReceivableOverviewVO result = service.buildOverview(2L, false);

        assertThat(result.getSummary().getReceivableTotal()).isEqualByComparingTo("0.00");
        ReceivableGroupVO group = result.getGroups().get(0);
        assertThat(group.getOutstanding()).isEqualByComparingTo("0.00");
        assertThat(group.getFirstOutstandingDate()).isNull();
        assertThat(group.getAgeDays()).isZero();
    }

    @Test
    @DisplayName("识别不出对方时归入未标注，并提示补商户")
    void unlabeledCounterparty() {
        whenTransactions(tx(1L, 1, lend.getId(), "500", "2026-01-01", null, null, "转账"));

        ReceivableOverviewVO result = service.buildOverview(2L, false);

        assertThat(result.getGroups().get(0).getCounterparty())
                .isEqualTo(ReceivableServiceImpl.UNLABELED);
        assertThat(result.getSummary().getUnlabeledCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("约定还款日已过时计算逾期天数")
    void overdueByExpectedDate() {
        whenTransactions(tx(1L, 1, lend.getId(), "1000", "2026-01-01", 44L,
                "{\"约定还款日\":\"2026-02-01\"}", "借出"));

        ReceivableGroupVO group = service.buildOverview(2L, false).getGroups().get(0);

        assertThat(group.getExpectedRepayDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(group.getOverdueDays()).isEqualTo(
                ChronoUnit.DAYS.between(LocalDate.of(2026, 2, 1), LocalDate.now()));
    }

    @Test
    @DisplayName("多个对方分别汇总")
    void multipleCounterparties() {
        Merchant other = new Merchant();
        other.setId(45L);
        other.setName("李四");
        lenient().when(merchantMapper.selectBatchIds(any())).thenReturn(List.of(borrower, other));

        whenTransactions(
                tx(1L, 1, lend.getId(), "100", "2026-01-01", 44L, null, "借出"),
                tx(2L, 1, lend.getId(), "200", "2026-01-02", 45L, null, "借出")
        );

        ReceivableOverviewVO result = service.buildOverview(2L, false);

        assertThat(result.getSummary().getReceivableTotal()).isEqualByComparingTo("300.00");
        assertThat(result.getSummary().getCounterpartyCount()).isEqualTo(2);
        assertThat(result.getGroups()).hasSize(2);
    }

    private void whenTransactions(Transaction... transactions) {
        lenient().when(transactionMapper.selectList(any())).thenReturn(List.of(transactions));
    }

    private Category category(Long id, Long parentId, String name, int type) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        category.setType(type);
        category.setIsActive(true);
        return category;
    }

    private Transaction tx(Long id, int type, Long categoryId, String amount, String date,
                           Long merchantId, String extFields, String note) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setType(type);
        tx.setCategoryId(categoryId);
        tx.setAmount(new BigDecimal(amount));
        tx.setAmountBase(new BigDecimal(amount));
        tx.setCurrency("CNY");
        tx.setAccountId(1L);
        tx.setMerchantId(merchantId);
        tx.setTransactionDate(LocalDate.parse(date));
        tx.setIsConfirmed(true);
        tx.setExtFields(extFields);
        tx.setNote(note);
        return tx;
    }
}