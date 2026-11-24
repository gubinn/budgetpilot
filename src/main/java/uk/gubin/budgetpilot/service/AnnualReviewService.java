package uk.gubin.budgetpilot.service;

import uk.gubin.budgetpilot.vo.AnnualOverviewVO;

public interface AnnualReviewService {

    /**
     * 年度复盘聚合。
     *
     * @param year  年份；为空取当前年
     * @param level 1=一级分类，2=二级分类
     */
    AnnualOverviewVO overview(Integer year, int level);
}