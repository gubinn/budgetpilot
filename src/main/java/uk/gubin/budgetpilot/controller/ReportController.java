package uk.gubin.budgetpilot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.service.ReportService;
import uk.gubin.budgetpilot.vo.ReportVO;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly-summary")
    public Result<ReportVO> monthlySummary(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") String month) {
        return Result.ok(reportService.monthlySummary(month));
    }

    @GetMapping("/category-detail")
    public Result<ReportVO> categoryDetail(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") String month,
            @RequestParam Long categoryId) {
        return Result.ok(reportService.categoryDetail(month, categoryId));
    }

    @GetMapping("/trend")
    public Result<ReportVO> trend(
            @RequestParam(defaultValue = "12") int months,
            @RequestParam(required = false) Integer type) {
        return Result.ok(reportService.trend(months, type));
    }

    @GetMapping("/compare")
    public Result<ReportVO> compare(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") String month,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") String compareWith) {
        return Result.ok(reportService.compare(month, compareWith));
    }

    @GetMapping("/account-summary")
    public Result<ReportVO> accountSummary() {
        return Result.ok(reportService.accountSummary());
    }

    @GetMapping("/daily-heatmap")
    public Result<ReportVO> dailyHeatmap(@RequestParam int year) {
        return Result.ok(reportService.dailyHeatmap(year));
    }

    @GetMapping("/budget-review")
    public Result<ReportVO> budgetReview(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") String month) {
        return Result.ok(reportService.budgetReview(month));
    }

    @GetMapping("/currency-distribution")
    public Result<ReportVO> currencyDistribution(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") String month) {
        return Result.ok(reportService.currencyDistribution(month));
    }

    @GetMapping("/merchant-distribution")
    public Result<ReportVO> merchantDistribution(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") String month) {
        return Result.ok(reportService.merchantDistribution(month));
    }
}
