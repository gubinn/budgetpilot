package uk.gubin.budgetpilot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.dto.RecurringRuleCreateDTO;
import uk.gubin.budgetpilot.dto.RecurringRuleUpdateDTO;
import uk.gubin.budgetpilot.entity.Merchant;
import uk.gubin.budgetpilot.entity.RecurringRule;
import uk.gubin.budgetpilot.mapper.MerchantMapper;
import uk.gubin.budgetpilot.service.RecurringRuleService;
import uk.gubin.budgetpilot.vo.RecurringRuleVO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recurring-rules")
@RequiredArgsConstructor
public class RecurringRuleController {

    private final RecurringRuleService recurringRuleService;
    private final MerchantMapper merchantMapper;

    @GetMapping
    public Result<List<RecurringRuleVO>> list() {
        return Result.ok(recurringRuleService.list().stream()
                .map(this::toVO)
                .toList());
    }

    @GetMapping("/{id}")
    public Result<RecurringRuleVO> get(@PathVariable Long id) {
        return Result.ok(toVO(recurringRuleService.getById(id)));
    }

    private RecurringRuleVO toVO(RecurringRule entity) {
        RecurringRuleVO vo = RecurringRuleVO.fromEntity(entity);
        if (entity.getMerchantId() != null) {
            Merchant m = merchantMapper.selectById(entity.getMerchantId());
            if (m != null) {
                vo.setMerchantName(m.getName());
            }
        }
        return vo;
    }

    @PostMapping
    public Result<RecurringRuleVO> create(@Valid @RequestBody RecurringRuleCreateDTO dto) {
        return Result.ok(toVO(recurringRuleService.create(dto)));
    }

    @PutMapping("/{id}")
    public Result<RecurringRuleVO> update(@PathVariable Long id, @Valid @RequestBody RecurringRuleUpdateDTO dto) {
        return Result.ok(toVO(recurringRuleService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recurringRuleService.removeById(id);
        return Result.ok();
    }

    @PostMapping("/{id}/toggle")
    public Result<RecurringRuleVO> toggle(@PathVariable Long id) {
        return Result.ok(toVO(recurringRuleService.toggleActive(id)));
    }

    @PostMapping("/{id}/execute")
    public Result<Void> executeNow(@PathVariable Long id) {
        recurringRuleService.generateTransaction(recurringRuleService.getById(id));
        recurringRuleService.updateNextExecute(recurringRuleService.getById(id));
        return Result.ok();
    }
}