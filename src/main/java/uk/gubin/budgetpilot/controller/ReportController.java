package uk.gubin.budgetpilot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.service.ReportService;
import uk.gubin.budgetpilot.vo.NetWorthSnapshotVO;
import uk.gubin.budgetpilot.vo.ReportOverviewVO;
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

    /** 某个月的净资产快照：每个账户月末余额 + 环比变化 */
    @GetMapping("/net-worth")
    public Result<NetWorthSnapshotVO> netWorth(
            @RequestParam(required = false) String month) {
        if (month != null && !month.isBlank()) {
            validateMonth(month);
        }
        return Result.ok(reportService.netWorthSnapshot(month));
    }

    /** 报表聚合：一次返回月报页所需的全部数据 */
    @GetMapping("/overview")
    public Result<ReportOverviewVO> overview(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "PREV") String compare,
            @RequestParam(defaultValue = "ALL") String scope,
            @RequestParam(defaultValue = "1") Integer level) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        if (end.isBefore(start)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }
        if (level == null || (level != 1 && level != 2)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "level 只能为 1 或 2");
        }
        return Result.ok(reportService.overview(startDate, endDate, compare, scope, level));
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "日期格式不正确，应为 yyyy-MM-dd");
        }
    }

    @GetMapping("/monthly-summary")
    public Result<ReportVO> monthlySummary(@RequestParam String month,
                                           @RequestParam(defaultValue = "1") Integer level) {
        validateMonth(month);
        if (level == null || (level != 1 && level != 2)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "level 只能为 1 或 2");
        }
        return Result.ok(reportService.monthlySummary(month, level));
    }

    @GetMapping("/yearly-summary")
    public Result<ReportVO> yearlySummary(@RequestParam Integer year,
                                          @RequestParam(defaultValue = "1") Integer level) {
        if (year == null || year < 2000 || year > 2100) {
            throw new BizException(ErrorCode.PARAM_ERROR, "年份不合法，应在 2000 到 2100 之间");
        }
        if (level == null || (level != 1 && level != 2)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "level 只能为 1 或 2");
        }
        return Result.ok(reportService.yearlySummary(year, level));
    }

    @GetMapping("/category-detail")
    public Result<ReportVO> categoryDetail(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam Long categoryId,
            @RequestParam(defaultValue = "ALL") String scope) {
        if (startDate != null || endDate != null) {
            if (startDate == null || endDate == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "startDate 与 endDate 必须同时提供");
            }
            LocalDate start = parseDate(startDate);
            LocalDate end = parseDate(endDate);
            if (end.isBefore(start)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "结束日期不能早于开始日期");
            }
            return Result.ok(reportService.categoryDetail(start, end, categoryId, scope));
        }
        if (month == null || month.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "month 与 startDate/endDate 至少提供一组");
        }
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

    @GetMapping("/ext-field-distribution")
    public Result<java.util.List<ReportVO.ExtFieldShareItem>> extFieldDistribution(
            @RequestParam String month,
            @RequestParam String key,
            @RequestParam(defaultValue = "1") Integer type) {
        validateMonth(month);
        if (key == null || key.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "扩展字段 key 不能为空");
        }
        if (type == null || (type != 1 && type != 2 && type != 3 && type != 4)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "type 只能为 1、2、3 或 4");
        }
        return Result.ok(reportService.extFieldDistribution(month, key.trim(), type));
    }
}
