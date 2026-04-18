package uk.gubin.budgetpilot.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.dto.ChangePasswordDTO;
import uk.gubin.budgetpilot.dto.LoginDTO;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.mapper.UserMapper;
import uk.gubin.budgetpilot.service.UserService;
import uk.gubin.budgetpilot.vo.LoginVO;
import uk.gubin.budgetpilot.vo.UserVO;

import java.security.SecureRandom;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserMapper userMapper;

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

    @PostMapping("/api-key/generate")
    @SaCheckLogin
    public uk.gubin.budgetpilot.common.Result<String> generateApiKey() {
        Long userId = StpUtil.getLoginIdAsLong();
        // 生成 32 字节随机字符串，转 base64url
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String apiKey = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        User user = new User();
        user.setId(userId);
        user.setApiKey(apiKey);
        userMapper.updateById(user);
        return uk.gubin.budgetpilot.common.Result.ok(apiKey);
    }

    @GetMapping("/api-key")
    @SaCheckLogin
    public uk.gubin.budgetpilot.common.Result<String> getApiKey() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        return uk.gubin.budgetpilot.common.Result.ok(user.getApiKey() != null ? user.getApiKey() : "");
    }
}
