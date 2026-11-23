package uk.gubin.budgetpilot.service;

import uk.gubin.budgetpilot.vo.NetWorthSnapshotVO;
import uk.gubin.budgetpilot.vo.ReportOverviewVO;
import uk.gubin.budgetpilot.vo.ReportVO;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    /**
     * 报表聚合：一次返回区间内 KPI、结构、分类排行、趋势、日历、商户、
     * 月度预算、年度预算、账户与净资产、洞察。
     *
     * @param compare PREV（上一同长区间）/ YOY（去年同期）/ NONE
     * @param scope   ALL / DAILY / FIXED / SPECIAL
     * @param level   1=一级分类，2=二级分类
     */
    ReportOverviewVO overview(String startDate, String endDate, String compare, String scope, int level);

    /** 某个月的净资产快照明细；month 为空时取上一个已结束月份 */
    NetWorthSnapshotVO netWorthSnapshot(String month);

    ReportVO monthlySummary(String month);

    /** level=1 按一级分类汇总，level=2 按实际二级分类展示 */
    ReportVO monthlySummary(String month, int level);

    /** 年度总览：12 个月收支、分类占比、标签/商户统计与同比 */
    ReportVO yearlySummary(int year, int level);
    ReportVO categoryDetail(String month, Long categoryId);

    /** 区间版分类明细，scope 可按预算类型过滤；用于报表下钻 */
    ReportVO categoryDetail(LocalDate start, LocalDate end, Long categoryId, String scope);
    ReportVO trend(int months, Integer type);
    ReportVO compare(String month, String compareWith);
    ReportVO accountSummary();
    ReportVO dailyHeatmap(int year);
    ReportVO budgetReview(String month);
    ReportVO currencyDistribution(String month);
    ReportVO merchantDistribution(String month);

    /** 按任意扩展字段 key 统计分布（缺失的记录归入“未标注”） */
    List<ReportVO.ExtFieldShareItem> extFieldDistribution(String month, String key, int type);
}
