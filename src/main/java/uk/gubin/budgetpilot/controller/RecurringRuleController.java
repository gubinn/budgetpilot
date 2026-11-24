package uk.gubin.budgetpilot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.common.BizException;
import uk.gubin.budgetpilot.common.ErrorCode;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.dto.RecurringRuleCreateDTO;
import uk.gubin.budgetpilot.dto.RecurringRuleUpdateDTO;
import uk.gubin.budgetpilot.entity.Account;
import uk.gubin.budgetpilot.entity.Category;
import uk.gubin.budgetpilot.entity.Merchant;
import uk.gubin.budgetpilot.entity.RecurringRule;
import uk.gubin.budgetpilot.mapper.AccountMapper;
import uk.gubin.budgetpilot.mapper.CategoryMapper;
import uk.gubin.budgetpilot.mapper.MerchantMapper;
import uk.gubin.budgetpilot.service.RecurringRuleService;
import uk.gubin.budgetpilot.vo.RecurringRuleVO;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/recurring-rules")
@RequiredArgsConstructor
public class RecurringRuleController {

    private final RecurringRuleService recurringRuleService;
    private final MerchantMapper merchantMapper;
    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;

    @GetMapping
    public Result<List<RecurringRuleVO>> list() {
        return Result.ok(recurringRuleService.list().stream()
                .map(this::toVO)
                .toList());
    }

    @GetMapping("/{id}")
    public Result<RecurringRuleVO> get(@PathVariable Long id) {
        RecurringRule rule = recurringRuleService.getById(id);
        if (rule == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "周期规则不存在");
        }
        return Result.ok(toVO(rule));
    }

    private RecurringRuleVO toVO(RecurringRule entity) {
        RecurringRuleVO vo = RecurringRuleVO.fromEntity(entity);
        if (vo == null) {
            return null;
        }
        if (entity.getAccountId() != null) {
            Account account = accountMapper.selectById(entity.getAccountId());
            if (account != null) {
                vo.setAccountName(account.getName());
            }
        }
        if (entity.getCategoryId() != null) {
            Category category = categoryMapper.selectById(entity.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
                vo.setCategoryColor(category.getColor());
            }
        }
        if (entity.getMerchantId() != null) {
            Merchant merchant = merchantMapper.selectById(entity.getMerchantId());
            if (merchant != null) {
                vo.setMerchantName(merchant.getName());
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
        log.info("Deleted recurring rule: id={}", id);
        return Result.ok();
    }

    @PostMapping("/{id}/toggle")
    public Result<RecurringRuleVO> toggle(@PathVariable Long id) {
        return Result.ok(toVO(recurringRuleService.toggleActive(id)));
    }

    @PostMapping("/{id}/execute")
    public Result<Void> executeNow(@PathVariable Long id) {
        recurringRuleService.executeNow(id);
        return Result.ok();
    }
}