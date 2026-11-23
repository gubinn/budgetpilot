package uk.gubin.budgetpilot.service;

import uk.gubin.budgetpilot.dto.PrepaidCardAdjustDTO;
import uk.gubin.budgetpilot.dto.PrepaidCardCreateDTO;
import uk.gubin.budgetpilot.dto.PrepaidCardUpdateDTO;
import uk.gubin.budgetpilot.entity.PrepaidCard;
import uk.gubin.budgetpilot.vo.PrepaidCardSummaryVO;
import uk.gubin.budgetpilot.vo.PrepaidCardUsageVO;
import uk.gubin.budgetpilot.vo.PrepaidCardVO;

import java.util.List;

/**
 * 卡券包（表外备忘台账）。
 */
public interface PrepaidCardService {

    /**
     * @param state   ALL / ACTIVE / HAS_BALANCE / EXPIRING / EXPIRED / USED_UP / VOID
     * @param keyword 名称、发卡方、渠道、账号提示、备注
     */
    List<PrepaidCardVO> list(String state, String keyword, String channel, Integer kind, Integer source);

    /** 原始实体列表，供定时到期提醒使用。 */
    List<PrepaidCard> listAll();

    PrepaidCardVO get(Long id);

    PrepaidCardSummaryVO summary();

    PrepaidCardVO create(PrepaidCardCreateDTO dto);

    PrepaidCardVO update(Long id, PrepaidCardUpdateDTO dto);

    void delete(Long id);

    void voidCard(Long id);

    void restoreCard(Long id);

    PrepaidCardVO use(Long id, PrepaidCardAdjustDTO dto);

    PrepaidCardVO recharge(Long id, PrepaidCardAdjustDTO dto);

    List<PrepaidCardUsageVO> usages(Long id);
}