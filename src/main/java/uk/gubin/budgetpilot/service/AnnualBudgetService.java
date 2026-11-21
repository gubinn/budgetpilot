package uk.gubin.budgetpilot.service;

import uk.gubin.budgetpilot.dto.AnnualBudgetCreateDTO;
import uk.gubin.budgetpilot.dto.AnnualBudgetUpdateDTO;
import uk.gubin.budgetpilot.entity.AnnualBudget;
import uk.gubin.budgetpilot.vo.AnnualBudgetProgressVO;

public interface AnnualBudgetService {
    AnnualBudget create(AnnualBudgetCreateDTO dto);

    AnnualBudgetProgressVO getProgress(String year);

    AnnualBudget update(String year, AnnualBudgetUpdateDTO dto);

    boolean exists(String year);
}