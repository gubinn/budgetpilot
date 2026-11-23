package uk.gubin.budgetpilot.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 往来款页面一次性返回的数据。
 */
@Data
public class ReceivableOverviewVO {
    private ReceivableSummaryVO summary = new ReceivableSummaryVO();
    private List<ReceivableGroupVO> groups = new ArrayList<>();
}