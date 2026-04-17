package uk.gubin.budgetpilot.service;

import uk.gubin.budgetpilot.vo.ReportVO;

import java.time.LocalDate;

public interface ReportService {
    ReportVO monthlySummary(String month);
    ReportVO categoryDetail(String month, Long categoryId);
    ReportVO trend(int months, Integer type);
    ReportVO compare(String month, String compareWith);
    ReportVO accountSummary();
    ReportVO dailyHeatmap(int year);
    ReportVO budgetReview(String month);
    ReportVO currencyDistribution(String month);
}
