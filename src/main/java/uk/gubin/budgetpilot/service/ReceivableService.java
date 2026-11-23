package uk.gubin.budgetpilot.service;

import uk.gubin.budgetpilot.vo.ReceivableOverviewVO;
import uk.gubin.budgetpilot.vo.ReceivableSummaryVO;

/**
 * 往来款（应收/应付）只读汇总。
 *
 * <p>数据来源是「往来款」分类下的交易流水，不新增台账表：
 * 借出款/借入款计入本金，收回借出款/归还借入款冲减本金。
 * 这些分类已经标记为不计入收支报表，因此不会影响收入、支出和预算。</p>
 */
public interface ReceivableService {

    /**
     * 往来款总览。
     *
     * @param includeUnconfirmed 是否包含待确认交易，默认 false
     */
    ReceivableOverviewVO overview(boolean includeUnconfirmed);

    /** 首页卡片用的轻量汇总。 */
    ReceivableSummaryVO summary();
}