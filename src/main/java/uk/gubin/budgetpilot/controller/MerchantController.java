package uk.gubin.budgetpilot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.dto.MerchantCreateDTO;
import uk.gubin.budgetpilot.dto.MerchantQueryDTO;
import uk.gubin.budgetpilot.dto.MerchantUpdateDTO;
import uk.gubin.budgetpilot.service.MerchantService;
import uk.gubin.budgetpilot.vo.MerchantVO;

import java.util.List;

/**
 * 商户管理 Controller
 */
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * 创建商户
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<MerchantVO> create(@Valid @RequestBody MerchantCreateDTO dto) {
        return Result.ok(merchantService.create(dto));
    }

    /**
     * 商户列表（分页查询）
     */
    @GetMapping
    public Result<List<MerchantVO>> list(MerchantQueryDTO dto) {
        return Result.ok(merchantService.query(dto));
    }

    /**
     * 商户详情
     */
    @GetMapping("/{id}")
    public Result<MerchantVO> detail(@PathVariable Long id) {
        return Result.ok(merchantService.getById(id));
    }

    /**
     * 更新商户
     */
    @PutMapping("/{id}")
    public Result<MerchantVO> update(@PathVariable Long id, @Valid @RequestBody MerchantUpdateDTO dto) {
        return Result.ok(merchantService.update(id, dto));
    }

    /**
     * 删除商户
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        merchantService.delete(id);
        return Result.ok();
    }

    /**
     * 商户模糊搜索（用于交易表单）
     * @param keyword 搜索关键字
     * @param limit 返回数量限制（默认 10）
     */
    @GetMapping("/search")
    public Result<List<MerchantVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(merchantService.searchByName(keyword, limit));
    }
}