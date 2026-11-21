package uk.gubin.budgetpilot.service;

import uk.gubin.budgetpilot.vo.ReportVO;

import java.time.LocalDate;

public interface ReportService {
    ReportVO monthlySummary(String month);

    /** level=1 按一级分类汇总，level=2 按实际二级分类展示 */
    ReportVO monthlySummary(String month, int level);
    ReportVO categoryDetail(String month, Long categoryId);
    ReportVO trend(int months, Integer type);
    ReportVO compare(String month, String compareWith);
    ReportVO accountSummary();
    ReportVO dailyHeatmap(int year);
    ReportVO budgetReview(String month);
    ReportVO currencyDistribution(String month);
    ReportVO merchantDistribution(String month);
}
