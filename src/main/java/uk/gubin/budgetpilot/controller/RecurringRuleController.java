package uk.gubin.budgetpilot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.dto.RecurringRuleCreateDTO;
import uk.gubin.budgetpilot.dto.RecurringRuleUpdateDTO;
import uk.gubin.budgetpilot.service.RecurringRuleService;
import uk.gubin.budgetpilot.vo.RecurringRuleVO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recurring-rules")
@RequiredArgsConstructor
public class RecurringRuleController {

    private final RecurringRuleService recurringRuleService;

    @GetMapping
    public Result<List<RecurringRuleVO>> list() {
        return Result.ok(recurringRuleService.list().stream()
                .map(RecurringRuleVO::fromEntity)
                .toList());
    }

    @GetMapping("/{id}")
    public Result<RecurringRuleVO> get(@PathVariable Long id) {
        return Result.ok(RecurringRuleVO.fromEntity(recurringRuleService.getById(id)));
    }

    @PostMapping
    public Result<RecurringRuleVO> create(@Valid @RequestBody RecurringRuleCreateDTO dto) {
        return Result.ok(RecurringRuleVO.fromEntity(recurringRuleService.create(dto)));
    }

    @PutMapping("/{id}")
    public Result<RecurringRuleVO> update(@PathVariable Long id, @Valid @RequestBody RecurringRuleUpdateDTO dto) {
        return Result.ok(RecurringRuleVO.fromEntity(recurringRuleService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recurringRuleService.removeById(id);
        return Result.ok();
    }

    @PostMapping("/{id}/toggle")
    public Result<RecurringRuleVO> toggle(@PathVariable Long id) {
        return Result.ok(RecurringRuleVO.fromEntity(recurringRuleService.toggleActive(id)));
    }

    @PostMapping("/{id}/execute")
    public Result<Void> executeNow(@PathVariable Long id) {
        recurringRuleService.generateTransaction(recurringRuleService.getById(id));
        recurringRuleService.updateNextExecute(recurringRuleService.getById(id));
        return Result.ok();
    }
}