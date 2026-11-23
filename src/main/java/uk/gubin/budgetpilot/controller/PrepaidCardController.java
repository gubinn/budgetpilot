package uk.gubin.budgetpilot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.dto.PrepaidCardAdjustDTO;
import uk.gubin.budgetpilot.dto.PrepaidCardCreateDTO;
import uk.gubin.budgetpilot.dto.PrepaidCardUpdateDTO;
import uk.gubin.budgetpilot.service.PrepaidCardService;
import uk.gubin.budgetpilot.vo.PrepaidCardSummaryVO;
import uk.gubin.budgetpilot.vo.PrepaidCardUsageVO;
import uk.gubin.budgetpilot.vo.PrepaidCardVO;

import java.util.List;

/**
 * 卡券包（预付卡/券备忘）接口。
 *
 * <p>与账户、交易完全解耦：这里的增删改查不会影响总资产和收支报表。</p>
 */
@RestController
@RequestMapping("/api/v1/prepaid-cards")
@RequiredArgsConstructor
public class PrepaidCardController {

    private final PrepaidCardService prepaidCardService;

    @GetMapping
    public Result<List<PrepaidCardVO>> list(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Integer kind,
            @RequestParam(required = false) Integer source) {
        return Result.ok(prepaidCardService.list(state, keyword, channel, kind, source));
    }

    @GetMapping("/summary")
    public Result<PrepaidCardSummaryVO> summary() {
        return Result.ok(prepaidCardService.summary());
    }

    @GetMapping("/{id}")
    public Result<PrepaidCardVO> get(@PathVariable Long id) {
        return Result.ok(prepaidCardService.get(id));
    }

    @GetMapping("/{id}/usages")
    public Result<List<PrepaidCardUsageVO>> usages(@PathVariable Long id) {
        return Result.ok(prepaidCardService.usages(id));
    }

    @PostMapping
    public Result<PrepaidCardVO> create(@Valid @RequestBody PrepaidCardCreateDTO dto) {
        return Result.ok(prepaidCardService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<PrepaidCardVO> update(@PathVariable Long id,
                                        @Valid @RequestBody PrepaidCardUpdateDTO dto) {
        return Result.ok(prepaidCardService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        prepaidCardService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/use")
    public Result<PrepaidCardVO> use(@PathVariable Long id,
                                     @Valid @RequestBody PrepaidCardAdjustDTO dto) {
        return Result.ok(prepaidCardService.use(id, dto));
    }

    @PostMapping("/{id}/recharge")
    public Result<PrepaidCardVO> recharge(@PathVariable Long id,
                                          @Valid @RequestBody PrepaidCardAdjustDTO dto) {
        return Result.ok(prepaidCardService.recharge(id, dto));
    }

    @PostMapping("/{id}/void")
    public Result<Void> voidCard(@PathVariable Long id) {
        prepaidCardService.voidCard(id);
        return Result.ok();
    }

    @PostMapping("/{id}/restore")
    public Result<Void> restoreCard(@PathVariable Long id) {
        prepaidCardService.restoreCard(id);
        return Result.ok();
    }
}