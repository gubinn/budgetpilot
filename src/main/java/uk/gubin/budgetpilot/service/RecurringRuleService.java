package uk.gubin.budgetpilot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import uk.gubin.budgetpilot.dto.RecurringRuleCreateDTO;
import uk.gubin.budgetpilot.dto.RecurringRuleUpdateDTO;
import uk.gubin.budgetpilot.entity.RecurringRule;

import java.util.List;

public interface RecurringRuleService extends IService<RecurringRule> {
    /**
     * 获取今日需要执行的规则
     */
    List<RecurringRule> getRulesDueToday();

    /**
     * 更新规则的下次执行日期
     */
    void updateNextExecute(RecurringRule rule);

    /**
     * 按规则的 nextExecute 生成一笔交易（通过 TransactionService 确保余额、预算、预警全部正确）
     */
    void generateTransaction(RecurringRule rule);

    /**
     * 立即生成一笔今天的交易，不改变下次执行日期。
     */
    void executeNow(Long id);

    /**
     * 创建周期规则
     */
    RecurringRule create(RecurringRuleCreateDTO dto);

    /**
     * 更新周期规则
     */
    RecurringRule update(Long id, RecurringRuleUpdateDTO dto);

    /**
     * 切换激活状态
     */
    RecurringRule toggleActive(Long id);
}
