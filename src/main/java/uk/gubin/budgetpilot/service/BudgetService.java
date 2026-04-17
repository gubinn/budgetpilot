package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import uk.gubin.budgetpilot.dto.BudgetCreateDTO;
import uk.gubin.budgetpilot.dto.BudgetUpdateDTO;
import uk.gubin.budgetpilot.entity.Budget;
import uk.gubin.budgetpilot.entity.BudgetItem;
import uk.gubin.budgetpilot.vo.BudgetProgressVO;

import java.math.BigDecimal;
import java.util.List;

public interface BudgetService extends IService<Budget> {
    Budget create(BudgetCreateDTO dto);
    BudgetProgressVO getProgress(String yearMonth);
    Budget update(String yearMonth, BudgetUpdateDTO dto);
    Budget copyBudget(String yearMonth, String sourceMonth);
    BudgetItem getItem(Long categoryId, String yearMonth);
    void updateItemSpent(Long categoryId, String yearMonth, BigDecimal spentDelta);
    boolean exists(String yearMonth);
    void lockPreviousMonth();
}
