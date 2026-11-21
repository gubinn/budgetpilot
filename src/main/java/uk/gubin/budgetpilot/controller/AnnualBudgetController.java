package uk.gubin.budgetpilot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.dto.AnnualBudgetCreateDTO;
import uk.gubin.budgetpilot.dto.AnnualBudgetUpdateDTO;
import uk.gubin.budgetpilot.service.AnnualBudgetService;
import uk.gubin.budgetpilot.vo.AnnualBudgetProgressVO;

/**
 * 年度预算 Controller（目前用于专项支出）
 */
@RestController
@RequestMapping("/api/v1/annual-budgets")
@RequiredArgsConstructor
public class AnnualBudgetController {

    private final AnnualBudgetService annualBudgetService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<?> create(@Valid @RequestBody AnnualBudgetCreateDTO dto) {
        return Result.ok(annualBudgetService.create(dto));
    }

    @GetMapping("/{year}")
    public Result<AnnualBudgetProgressVO> getProgress(@PathVariable String year) {
        return Result.ok(annualBudgetService.getProgress(year));
    }

    @GetMapping("/{year}/progress")
    public Result<AnnualBudgetProgressVO> getProgressAlias(@PathVariable String year) {
        return Result.ok(annualBudgetService.getProgress(year));
    }

    @PutMapping("/{year}")
    public Result<?> update(@PathVariable String year, @Valid @RequestBody AnnualBudgetUpdateDTO dto) {
        return Result.ok(annualBudgetService.update(year, dto));
    }
}