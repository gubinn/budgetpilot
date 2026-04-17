package uk.gubin.budgetpilot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.dto.BudgetCreateDTO;
import uk.gubin.budgetpilot.dto.BudgetUpdateDTO;
import uk.gubin.budgetpilot.service.BudgetService;
import uk.gubin.budgetpilot.vo.BudgetProgressVO;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Result<?> create(@Valid @RequestBody BudgetCreateDTO dto) {
        return Result.ok(budgetService.create(dto));
    }

    @GetMapping("/{yearMonth}")
    public Result<BudgetProgressVO> getBudget(@PathVariable String yearMonth) {
        return Result.ok(budgetService.getProgress(yearMonth));
    }

    @PutMapping("/{yearMonth}")
    public Result<?> update(@PathVariable String yearMonth, @Valid @RequestBody BudgetUpdateDTO dto) {
        return Result.ok(budgetService.update(yearMonth, dto));
    }

    @PostMapping("/{yearMonth}/copy-from/{sourceMonth}")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Result<?> copyBudget(@PathVariable String yearMonth, @PathVariable String sourceMonth) {
        return Result.ok(budgetService.copyBudget(yearMonth, sourceMonth));
    }

    @GetMapping("/{yearMonth}/progress")
    public Result<BudgetProgressVO> getProgress(@PathVariable String yearMonth) {
        return Result.ok(budgetService.getProgress(yearMonth));
    }
}
