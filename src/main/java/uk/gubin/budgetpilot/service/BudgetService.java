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

    /**
     * @param fallback 当月没有预算时，是否回退到最近一期预算（优先往前找，其次往后找）
     */
    BudgetProgressVO getProgress(String yearMonth, boolean fallback);
    Budget update(String yearMonth, BudgetUpdateDTO dto);
    Budget copyBudget(String yearMonth, String sourceMonth);
    BudgetItem getItem(Long categoryId, String yearMonth);
    void updateItemSpent(Long categoryId, String yearMonth, BigDecimal spentDelta);
    List<FixedExpenseVO> fixedSummary(String yearMonth);
    boolean exists(String yearMonth);
    void lockPreviousMonth();
}
