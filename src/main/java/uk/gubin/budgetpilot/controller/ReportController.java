package uk.gubin.budgetpilot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.service.ReportService;
import uk.gubin.budgetpilot.vo.ReportVO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private void validateMonth(String month) {
        if (month == null || month.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "月份参数不能为空");
        }
        try {
            LocalDate.parse(month + "-01", DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "月份格式不正确，应为 yyyy-MM");
        }
    }

    @GetMapping("/monthly-summary")
    public Result<ReportVO> monthlySummary(@RequestParam String month) {
        validateMonth(month);
        return Result.ok(reportService.monthlySummary(month));
    }

    @GetMapping("/category-detail")
    public Result<ReportVO> categoryDetail(
            @RequestParam String month,
            @RequestParam Long categoryId) {
        validateMonth(month);
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
            @RequestParam String month,
            @RequestParam String compareWith) {
        validateMonth(month);
        validateMonth(compareWith);
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
    public Result<ReportVO> budgetReview(@RequestParam String month) {
        validateMonth(month);
        return Result.ok(reportService.budgetReview(month));
    }

    @GetMapping("/currency-distribution")
    public Result<ReportVO> currencyDistribution(@RequestParam String month) {
        validateMonth(month);
        return Result.ok(reportService.currencyDistribution(month));
    }

    @GetMapping("/merchant-distribution")
    public Result<ReportVO> merchantDistribution(@RequestParam String month) {
        validateMonth(month);
        return Result.ok(reportService.merchantDistribution(month));
    }
}
