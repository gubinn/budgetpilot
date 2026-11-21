package uk.gubin.budgetpilot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import uk.gubin.budgetpilot.common.PageResult;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.dto.TransactionAnnotateDTO;
import uk.gubin.budgetpilot.dto.TransactionCreateDTO;
import uk.gubin.budgetpilot.dto.TransactionQueryDTO;
import uk.gubin.budgetpilot.dto.TransactionReclassifyDTO;
import uk.gubin.budgetpilot.dto.TransactionUpdateDTO;
import uk.gubin.budgetpilot.service.TransactionService;
import uk.gubin.budgetpilot.vo.TransactionVO;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Result<TransactionVO> create(@Valid @RequestBody TransactionCreateDTO dto) {
        return Result.ok(transactionService.create(dto));
    }

    @GetMapping
    public Result<PageResult<TransactionVO>> query(TransactionQueryDTO dto) {
        return Result.ok(transactionService.query(dto));
    }

    @GetMapping("/tags")
    public Result<List<String>> tags() {
        return Result.ok(transactionService.listTags());
    }

    @GetMapping("/ext-keys")
    public Result<List<String>> extKeys() {
        return Result.ok(transactionService.listExtFieldKeys());
    }

    @PostMapping("/annotate")
    public Result<Integer> annotate(@Valid @RequestBody TransactionAnnotateDTO dto) {
        return Result.ok(transactionService.annotate(dto));
    }

    @GetMapping("/{id}")
    public Result<TransactionVO> detail(@PathVariable Long id) {
        return Result.ok(transactionService.getById(id));
    }

    @PutMapping("/{id}")
    public Result<TransactionVO> update(@PathVariable Long id, @Valid @RequestBody TransactionUpdateDTO dto) {
        return Result.ok(transactionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/confirm")
    public Result<TransactionVO> confirm(@PathVariable Long id) {
        return Result.ok(transactionService.confirm(id));
    }

    /**
     * 批量重分类：只更新 category_id，不触碰余额/预算/预警。
     * 用于邮件记账规则升级后的历史数据修正。
     */
    @PostMapping("/reclassify")
    public Result<Integer> reclassify(@Valid @RequestBody TransactionReclassifyDTO dto) {
        return Result.ok(transactionService.reclassify(dto));
    }
}
