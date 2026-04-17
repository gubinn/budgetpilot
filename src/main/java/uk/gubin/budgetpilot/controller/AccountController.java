package uk.gubin.budgetpilot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.dto.AccountCreateDTO;
import uk.gubin.budgetpilot.dto.AccountUpdateDTO;
import uk.gubin.budgetpilot.dto.BalanceAdjustDTO;
import uk.gubin.budgetpilot.service.AccountService;
import uk.gubin.budgetpilot.vo.AccountVO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Result<AccountVO> create(@Valid @RequestBody AccountCreateDTO dto) {
        return Result.ok(accountService.create(dto));
    }

    @GetMapping
    public Result<List<AccountVO>> list(
            @RequestParam(required = false, defaultValue = "true") Boolean active) {
        return Result.ok(accountService.listActive(active));
    }

    @GetMapping("/{id}")
    public Result<AccountVO> detail(@PathVariable Long id) {
        return Result.ok(accountService.getById(id));
    }

    @PatchMapping("/{id}")
    public Result<AccountVO> update(@PathVariable Long id, @RequestBody AccountUpdateDTO dto) {
        return Result.ok(accountService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/adjust-balance")
    public Result<Void> adjustBalance(@PathVariable Long id, @Valid @RequestBody BalanceAdjustDTO dto) {
        accountService.adjustBalanceWithRecord(id, dto.getNewBalance(), dto.getReason());
        return Result.ok();
    }
}
