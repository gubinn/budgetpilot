package uk.gubin.budgetpilot.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.dto.UserCreateDTO;
import uk.gubin.budgetpilot.dto.UserUpdateDTO;
import uk.gubin.budgetpilot.service.UserService;
import uk.gubin.budgetpilot.vo.UserVO;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @SaCheckRole("ADMIN")
    public uk.gubin.budgetpilot.common.Result<List<UserVO>> list() {
        return uk.gubin.budgetpilot.common.Result.ok(userService.listUsers());
    }

    @PostMapping
    @SaCheckRole("ADMIN")
    public uk.gubin.budgetpilot.common.Result<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return uk.gubin.budgetpilot.common.Result.ok(userService.create(dto));
    }

    @PatchMapping("/{id}")
    @SaCheckRole("ADMIN")
    public uk.gubin.budgetpilot.common.Result<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        return uk.gubin.budgetpilot.common.Result.ok(userService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @SaCheckRole("ADMIN")
    public uk.gubin.budgetpilot.common.Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return uk.gubin.budgetpilot.common.Result.ok();
    }

    @PutMapping("/{id}/password")
    @SaCheckRole("ADMIN")
    public uk.gubin.budgetpilot.common.Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return uk.gubin.budgetpilot.common.Result.ok();
    }

    // ---- 用户配置 ----

    @GetMapping("/config")
    public uk.gubin.budgetpilot.common.Result<Map<String, String>> config() {
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        return uk.gubin.budgetpilot.common.Result.ok(userService.getUserConfigAll(userId));
    }

    @PutMapping("/config/{key}")
    public uk.gubin.budgetpilot.common.Result<Void> setConfig(@PathVariable String key, @RequestBody Map<String, String> body) {
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        userService.setUserConfig(userId, key, body.get("value"));
        return uk.gubin.budgetpilot.common.Result.ok();
    }
}
