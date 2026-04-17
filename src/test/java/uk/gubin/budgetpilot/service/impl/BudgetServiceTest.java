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
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.dto.BudgetCreateDTO;
import uk.gubin.budgetpilot.dto.BudgetUpdateDTO;
import uk.gubin.budgetpilot.entity.Budget;
import uk.gubin.budgetpilot.entity.BudgetItem;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.mapper.BudgetItemMapper;
import uk.gubin.budgetpilot.mapper.BudgetMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.TransactionMapper;
import uk.gubin.budgetpilot.vo.BudgetProgressVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock private BudgetMapper budgetMapper;
    @Mock private BudgetItemMapper budgetItemMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private TransactionMapper transactionMapper;
    @InjectMocks private BudgetServiceImpl budgetService;

    @BeforeEach
    void setUp() {
        doAnswer(inv -> {
            Budget b = inv.getArgument(0);
            b.setId(1L);
            return 1;
        }).when(budgetMapper).insert(any(Budget.class));
    }

    // ============ CREATE ============

    @Nested
    @DisplayName("创建预算")
    class CreateTests {

        @Test
        @DisplayName("正常创建预算+明细")
        void create_Normal() {
            BudgetCreateDTO dto = buildCreateDTO();

            budgetService.create(dto);

            ArgumentCaptor<Budget> budgetCaptor = ArgumentCaptor.forClass(Budget.class);
            verify(budgetMapper).insert(budgetCaptor.capture());
            assertThat(budgetCaptor.getValue().getYearMonth()).isEqualTo("2026-04");
            assertThat(budgetCaptor.getValue().getTotalAmount()).isEqualByComparingTo(new BigDecimal("5000"));

            // 验证创建了两条明细
            verify(budgetItemMapper, times(2)).insert(any(BudgetItem.class));
        }

        @Test
        @DisplayName("重复月份拒绝创建")
        void create_DuplicateMonth() {
            when(budgetMapper.selectCount(any())).thenReturn(1L);

            assertThatThrownBy(() -> budgetService.create(buildCreateDTO()))
                    .isInstanceOf(BizException.class);
        }
    }

    // ============ UPDATE ============

    @Nested
    @DisplayName("更新预算")
    class UpdateTests {

        @Test
        @DisplayName("只更新 note - 不影响 totalAmount")
        void update_OnlyNote() {
            Budget existing = new Budget();
            existing.setId(1L);
            existing.setYearMonth("2026-04");
            existing.setTotalAmount(new BigDecimal("5000"));
            existing.setIsLocked(false);

            when(budgetMapper.selectOne(any())).thenReturn(existing);

            BudgetUpdateDTO dto = new BudgetUpdateDTO();
            dto.setNote("修改备注");

            budgetService.update("2026-04", dto);

            ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
            verify(budgetMapper).updateById(captor.capture());
            // totalAmount 不变
            assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(captor.getValue().getNote()).isEqualTo("修改备注");
        }

        @Test
        @DisplayName("更新 totalAmount 成功")
        void update_TotalAmount() {
            Budget existing = new Budget();
            existing.setId(1L);
            existing.setYearMonth("2026-04");
            existing.setTotalAmount(new BigDecimal("5000"));
            existing.setIsLocked(false);

            when(budgetMapper.selectOne(any())).thenReturn(existing);

            BudgetUpdateDTO dto = new BudgetUpdateDTO();
            dto.setTotalAmount(new BigDecimal("6000"));

            budgetService.update("2026-04", dto);

            ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
            verify(budgetMapper).updateById(captor.capture());
            assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(new BigDecimal("6000"));
        }

        @Test
        @DisplayName("锁定预算拒绝修改")
        void update_LockedBudget() {
            Budget existing = new Budget();
            existing.setId(1L);
            existing.setYearMonth("2026-04");
            existing.setIsLocked(true);

            when(budgetMapper.selectOne(any())).thenReturn(existing);

            BudgetUpdateDTO dto = new BudgetUpdateDTO();
            dto.setNote("修改");

            assertThatThrownBy(() -> budgetService.update("2026-04", dto))
                    .isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("更新明细 - 已存在则更新")
        void update_ExistingItem() {
            Budget existing = new Budget();
            existing.setId(1L);
            existing.setYearMonth("2026-04");
            existing.setTotalAmount(new BigDecimal("5000"));
            existing.setIsLocked(false);

            BudgetItem existingItem = new BudgetItem();
            existingItem.setId(10L);
            existingItem.setBudgetId(1L);
            existingItem.setCategoryId(1L);
            existingItem.setAmount(new BigDecimal("1000"));
            existingItem.setSpent(new BigDecimal("500"));

            when(budgetMapper.selectOne(any())).thenReturn(existing);
            when(budgetItemMapper.selectOne(any())).thenReturn(existingItem);

            BudgetUpdateDTO dto = new BudgetUpdateDTO();
            BudgetUpdateDTO.BudgetItemDTO itemDTO = new BudgetUpdateDTO.BudgetItemDTO();
            itemDTO.setCategoryId(1L);
            itemDTO.setAmount(new BigDecimal("1500"));
            dto.setItems(List.of(itemDTO));

            budgetService.update("2026-04", dto);

            // 验证更新明细（updateById），不是新增
            verify(budgetItemMapper).updateById(any(BudgetItem.class));
            verify(budgetItemMapper, never()).insert(any(BudgetItem.class));
        }

        @Test
        @DisplayName("更新明细 - 不存在则新增")
        void update_NewItem() {
            Budget existing = new Budget();
            existing.setId(1L);
            existing.setYearMonth("2026-04");
            existing.setTotalAmount(new BigDecimal("5000"));
            existing.setIsLocked(false);

            when(budgetMapper.selectOne(any())).thenReturn(existing);
            when(budgetItemMapper.selectOne(any())).thenReturn(null);

            BudgetUpdateDTO dto = new BudgetUpdateDTO();
            BudgetUpdateDTO.BudgetItemDTO itemDTO = new BudgetUpdateDTO.BudgetItemDTO();
            itemDTO.setCategoryId(99L);
            itemDTO.setAmount(new BigDecimal("2000"));
            dto.setItems(List.of(itemDTO));

            budgetService.update("2026-04", dto);

            // 验证新增明细
            verify(budgetItemMapper).insert(any(BudgetItem.class));
        }
    }

    // ============ PROGRESS ============

    @Nested
    @DisplayName("预算进度")
    class ProgressTests {

        @Test
        @DisplayName("getProgress 正确计算总已消费")
        void getProgress_CalculateTotalSpent() {
            Budget budget = new Budget();
            budget.setId(1L);
            budget.setYearMonth("2026-04");
            budget.setTotalAmount(new BigDecimal("5000"));
            budget.setIsLocked(false);

            when(budgetMapper.selectOne(any())).thenReturn(budget);

            Transaction tx1 = new Transaction();
            tx1.setType(1);
            tx1.setAmountBase(new BigDecimal("100"));
            tx1.setIsConfirmed(true);
            Transaction tx2 = new Transaction();
            tx2.setType(1);
            tx2.setAmountBase(new BigDecimal("200"));
            tx2.setIsConfirmed(true);

            when(transactionMapper.selectList(any())).thenReturn(List.of(tx1, tx2));
            when(budgetItemMapper.selectList(any())).thenReturn(List.of());

            BudgetProgressVO vo = budgetService.getProgress("2026-04");

            assertThat(vo.getTotalBudget()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(vo.getTotalSpent()).isEqualByComparingTo(new BigDecimal("300"));
            assertThat(vo.getRemaining()).isEqualByComparingTo(new BigDecimal("4700"));
        }

        @Test
        @DisplayName("getProgress 不计入未确认交易")
        void getProgress_ExcludeUnconfirmed() {
            Budget budget = new Budget();
            budget.setId(1L);
            budget.setYearMonth("2026-04");
            budget.setTotalAmount(new BigDecimal("5000"));

            when(budgetMapper.selectOne(any())).thenReturn(budget);

            Transaction confirmed = new Transaction();
            confirmed.setType(1);
            confirmed.setAmountBase(new BigDecimal("100"));
            confirmed.setIsConfirmed(true);

            Transaction unconfirmed = new Transaction();
            unconfirmed.setType(1);
            unconfirmed.setAmountBase(new BigDecimal("5000")); // 大额但未确认
            unconfirmed.setIsConfirmed(false);

            when(transactionMapper.selectList(any())).thenReturn(List.of(confirmed));
            when(budgetItemMapper.selectList(any())).thenReturn(List.of());

            BudgetProgressVO vo = budgetService.getProgress("2026-04");

            assertThat(vo.getTotalSpent()).isEqualByComparingTo(new BigDecimal("100"));
        }
    }

    // ============ ITEM SPENT ============

    @Nested
    @DisplayName("更新分类已消费")
    class ItemSpentTests {

        @Test
        @DisplayName("updateItemSpent - 正数增加 spent")
        void updateItemSpent_Positive() {
            Budget budget = new Budget();
            budget.setId(1L);
            budget.setYearMonth("2026-04");
            budget.setTotalAmount(new BigDecimal("5000"));

            BudgetItem item = new BudgetItem();
            item.setId(1L);
            item.setBudgetId(1L);
            item.setCategoryId(10L);
            item.setAmount(new BigDecimal("1000"));
            item.setSpent(new BigDecimal("200"));

            when(budgetMapper.selectOne(any())).thenReturn(budget);
            when(budgetItemMapper.selectOne(any())).thenReturn(item);

            budgetService.updateItemSpent(10L, "2026-04", new BigDecimal("50"));

            ArgumentCaptor<BudgetItem> captor = ArgumentCaptor.forClass(BudgetItem.class);
            verify(budgetItemMapper).updateById(captor.capture());
            assertThat(captor.getValue().getSpent()).isEqualByComparingTo(new BigDecimal("250"));
        }

        @Test
        @DisplayName("updateItemSpent - 负数减少 spent（回退场景）")
        void updateItemSpent_Negative() {
            Budget budget = new Budget();
            budget.setId(1L);
            budget.setYearMonth("2026-04");

            BudgetItem item = new BudgetItem();
            item.setId(1L);
            item.setBudgetId(1L);
            item.setCategoryId(10L);
            item.setAmount(new BigDecimal("1000"));
            item.setSpent(new BigDecimal("500"));

            when(budgetMapper.selectOne(any())).thenReturn(budget);
            when(budgetItemMapper.selectOne(any())).thenReturn(item);

            budgetService.updateItemSpent(10L, "2026-04", new BigDecimal("-200"));

            ArgumentCaptor<BudgetItem> captor = ArgumentCaptor.forClass(BudgetItem.class);
            verify(budgetItemMapper).updateById(captor.capture());
            assertThat(captor.getValue().getSpent()).isEqualByComparingTo(new BigDecimal("300"));
        }

        @Test
        @DisplayName("updateItemSpent - 不存在该分类预算时忽略")
        void updateItemSpent_NoBudgetItem() {
            Budget budget = new Budget();
            budget.setId(1L);
            budget.setYearMonth("2026-04");

            when(budgetMapper.selectOne(any())).thenReturn(budget);
            when(budgetItemMapper.selectOne(any())).thenReturn(null);

            budgetService.updateItemSpent(99L, "2026-04", new BigDecimal("100"));

            verify(budgetItemMapper, never()).updateById(any(BudgetItem.class));
        }
    }

    // ============ HELPERS ============

    private BudgetCreateDTO buildCreateDTO() {
        BudgetCreateDTO dto = new BudgetCreateDTO();
        dto.setYearMonth("2026-04");
        dto.setTotalAmount(new BigDecimal("5000"));
        dto.setNote("四月预算");

        BudgetCreateDTO.BudgetItemDTO item1 = new BudgetCreateDTO.BudgetItemDTO();
        item1.setCategoryId(1L);
        item1.setAmount(new BigDecimal("1000"));

        BudgetCreateDTO.BudgetItemDTO item2 = new BudgetCreateDTO.BudgetItemDTO();
        item2.setCategoryId(2L);
        item2.setAmount(new BigDecimal("2000"));

        dto.setItems(List.of(item1, item2));
        return dto;
    }
}
