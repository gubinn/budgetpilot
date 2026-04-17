package uk.gubin.budgetpilot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.dto.AlertRuleCreateDTO;
import uk.gubin.budgetpilot.dto.AlertRuleUpdateDTO;
import uk.gubin.budgetpilot.service.AlertRuleService;
import uk.gubin.budgetpilot.vo.AlertRuleVO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alert-rules")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @GetMapping
    public Result<List<AlertRuleVO>> list() {
        return Result.ok(alertRuleService.list().stream()
                .map(AlertRuleVO::fromEntity)
                .toList());
    }

    @GetMapping("/{id}")
    public Result<AlertRuleVO> get(@PathVariable Long id) {
        return Result.ok(AlertRuleVO.fromEntity(alertRuleService.getById(id)));
    }

    @PostMapping
    public Result<AlertRuleVO> create(@Valid @RequestBody AlertRuleCreateDTO dto) {
        return Result.ok(AlertRuleVO.fromEntity(alertRuleService.create(dto)));
    }

    @PutMapping("/{id}")
    public Result<AlertRuleVO> update(@PathVariable Long id, @Valid @RequestBody AlertRuleUpdateDTO dto) {
        return Result.ok(AlertRuleVO.fromEntity(alertRuleService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        alertRuleService.removeById(id);
        return Result.ok();
    }

    @PostMapping("/{id}/toggle")
    public Result<AlertRuleVO> toggle(@PathVariable Long id) {
        return Result.ok(AlertRuleVO.fromEntity(alertRuleService.toggleActive(id)));
    }
}