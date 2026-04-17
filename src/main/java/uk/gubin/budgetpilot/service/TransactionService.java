package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import uk.gubin.budgetpilot.dto.TransactionCreateDTO;
import uk.gubin.budgetpilot.dto.TransactionQueryDTO;
import uk.gubin.budgetpilot.dto.TransactionUpdateDTO;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.common.PageResult;
import uk.gubin.budgetpilot.vo.TransactionVO;

import java.time.LocalDate;
import java.util.Map;

public interface TransactionService extends IService<Transaction> {
    TransactionVO create(TransactionCreateDTO dto);
    PageResult<TransactionVO> query(TransactionQueryDTO dto);
    TransactionVO getById(Long id);
    TransactionVO update(Long id, TransactionUpdateDTO dto);
    void delete(Long id);
    TransactionVO confirm(Long id);
    Map<String, Object> getDailySpent(LocalDate date);
    Map<String, Object> getMonthlySpent(String yearMonth);
    Map<String, Object> getWeeklySpent(LocalDate date);
}
