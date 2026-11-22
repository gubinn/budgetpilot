package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import uk.gubin.budgetpilot.dto.BudgetCreateDTO;
import uk.gubin.budgetpilot.dto.BudgetUpdateDTO;
import uk.gubin.budgetpilot.entity.Budget;
import uk.gubin.budgetpilot.entity.BudgetItem;
import uk.gubin.budgetpilot.vo.BudgetProgressVO;
import uk.gubin.budgetpilot.vo.FixedExpenseVO;

import java.math.BigDecimal;
import java.util.List;

public interface BudgetService extends IService<Budget> {
    Budget create(BudgetCreateDTO dto);
    BudgetProgressVO getProgress(String yearMonth);
    Budget update(String yearMonth, BudgetUpdateDTO dto);
    Budget copyBudget(String yearMonth, String sourceMonth);
    BudgetItem getItem(Long categoryId, String yearMonth);
    void updateItemSpent(Long categoryId, String yearMonth, BigDecimal spentDelta);
    /** 实时汇总某分类（含 DAILY 子分类）在指定月份的净支出，供预警判断使用。 */
    BigDecimal getCategorySpent(Long categoryId, String yearMonth);
    /** 按交易事实重算预算项的 spent，幂等；用于交易编辑等增量无法正确表达的场景。 */
    void recalculateItemSpent(Long categoryId, String yearMonth);
    List<FixedExpenseVO> fixedSummary(String yearMonth);
    boolean exists(String yearMonth);
    void lockPreviousMonth();
}
