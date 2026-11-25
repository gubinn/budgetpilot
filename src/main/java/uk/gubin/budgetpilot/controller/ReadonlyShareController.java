package uk.gubin.budgetpilot.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gubin.budgetpilot.common.Result;
import uk.gubin.budgetpilot.dto.ReadonlyShareCreateDTO;
import uk.gubin.budgetpilot.dto.ReadonlyShareVerifyDTO;
import uk.gubin.budgetpilot.ingress.IngressContext;
import uk.gubin.budgetpilot.service.ReadonlyShareService;
import uk.gubin.budgetpilot.vo.ReadonlySessionVO;
import uk.gubin.budgetpilot.vo.ReadonlyShareCreatedVO;
import uk.gubin.budgetpilot.vo.ReadonlyShareVO;
import uk.gubin.budgetpilot.vo.ShareCheckVO;

import java.util.List;

/**
 * 只读共享账本。
 *
 * <p>{@code /public/**} 下的接口免登录，供家人打开分享链接时使用；
 * 其余接口需要主人登录。</p>
 */
@RestController
@RequestMapping("/api/v1/shares")
@RequiredArgsConstructor
public class ReadonlyShareController {

    private final ReadonlyShareService readonlyShareService;

    @GetMapping
    public Result<List<ReadonlyShareVO>> list() {
        return Result.ok(readonlyShareService.list());
    }

    @PostMapping
    public Result<ReadonlyShareCreatedVO> create(@Valid @RequestBody ReadonlyShareCreateDTO dto) {
        return Result.ok(readonlyShareService.create(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> revoke(@PathVariable Long id) {
        readonlyShareService.revoke(id);
        return Result.ok();
    }

    /** 彻底删除已撤销/已过期的分享记录，避免列表越积越多。 */
    @DeleteMapping("/{id}/permanent")
    public Result<Void> delete(@PathVariable Long id) {
        readonlyShareService.delete(id);
        return Result.ok();
    }

    @GetMapping("/public/{code}")
    public Result<ShareCheckVO> check(@PathVariable String code) {
        return Result.ok(readonlyShareService.check(code));
    }

    @PostMapping("/public/{code}/verify")
    public Result<ReadonlySessionVO> verify(@PathVariable String code,
                                            @Valid @RequestBody(required = false) ReadonlyShareVerifyDTO dto,
                                            HttpServletRequest request) {
        String pin = dto == null ? null : dto.getPin();
        String clientIp = IngressContext.get(request).clientIp();
        return Result.ok(readonlyShareService.verify(code, pin, clientIp));
    }
}