package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import uk.gubin.budgetpilot.dto.TransactionCreateDTO;
import uk.gubin.budgetpilot.dto.TransactionQueryDTO;
import uk.gubin.budgetpilot.dto.TransactionReclassifyDTO;
import uk.gubin.budgetpilot.dto.TransactionUpdateDTO;
import uk.gubin.budgetpilot.entity.Transaction;
import uk.gubin.budgetpilot.common.PageResult;
import uk.gubin.budgetpilot.vo.TransactionSummaryVO;
import uk.gubin.budgetpilot.vo.TransactionVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TransactionService extends IService<Transaction> {
    TransactionVO create(TransactionCreateDTO dto);
    PageResult<TransactionVO> query(TransactionQueryDTO dto);

    /** 按同一组条件汇总金额（不分页） */
    TransactionSummaryVO summary(TransactionQueryDTO dto);
    TransactionVO getById(Long id);
    TransactionVO update(Long id, TransactionUpdateDTO dto);
    void delete(Long id);
    TransactionVO confirm(Long id);

    /** 批量修改交易分类（不影响余额/预算），返回实际更新数量 */
    int reclassify(TransactionReclassifyDTO dto);
    Map<String, Object> getDailySpent(LocalDate date);
    Map<String, Object> getMonthlySpent(String yearMonth);
    Map<String, Object> getWeeklySpent(LocalDate date);

    /** 当前用户使用过的全部标签（去重排序） */
    List<String> listTags();

    /** 当前用户使用过的全部扩展字段 key（去重排序） */
    List<String> listExtFieldKeys();

    /** 批量补充标签/扩展字段（不触碰余额/预算），返回更新数量 */
    int annotate(uk.gubin.budgetpilot.dto.TransactionAnnotateDTO dto);
}
