package uk.gubin.budgetpilot.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.dto.ChangePasswordDTO;
import uk.gubin.budgetpilot.dto.LoginDTO;
import uk.gubin.budgetpilot.service.UserService;
import uk.gubin.budgetpilot.vo.LoginVO;
import uk.gubin.budgetpilot.vo.UserVO;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public uk.gubin.budgetpilot.common.Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = userService.login(dto);
        return uk.gubin.budgetpilot.common.Result.ok(vo);
    }

    @PostMapping("/logout")
    @SaCheckLogin
    public uk.gubin.budgetpilot.common.Result<Void> logout() {
        userService.logout();
        return uk.gubin.budgetpilot.common.Result.ok();
    }

    @GetMapping("/info")
    @SaCheckLogin
    public uk.gubin.budgetpilot.common.Result<UserVO> info() {
        return uk.gubin.budgetpilot.common.Result.ok(userService.getCurrentUser());
    }

    @PostMapping("/change-password")
    @SaCheckLogin
    public uk.gubin.budgetpilot.common.Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(dto);
        return uk.gubin.budgetpilot.common.Result.ok();
    }
}
