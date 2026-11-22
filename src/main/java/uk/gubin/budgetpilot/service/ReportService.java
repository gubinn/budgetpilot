package uk.gubin.budgetpilot.service;

import uk.gubin.budgetpilot.vo.ReportVO;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    ReportVO monthlySummary(String month);

    /** level=1 按一级分类汇总，level=2 按实际二级分类展示 */
    ReportVO monthlySummary(String month, int level);

    /** 年度总览：12 个月收支、分类占比、标签/商户统计与同比 */
    ReportVO yearlySummary(int year, int level);
    ReportVO categoryDetail(String month, Long categoryId);
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
