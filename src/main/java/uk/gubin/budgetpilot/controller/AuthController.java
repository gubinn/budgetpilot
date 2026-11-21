package uk.gubin.budgetpilot.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uk.gubin.budgetpilot.dto.ChangePasswordDTO;
import uk.gubin.budgetpilot.dto.LoginDTO;
import uk.gubin.budgetpilot.dto.OtpResendDTO;
import uk.gubin.budgetpilot.dto.OtpVerifyDTO;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.ingress.IngressContext;
import uk.gubin.budgetpilot.ingress.IngressInfo;
import uk.gubin.budgetpilot.ingress.IngressProperties;
import uk.gubin.budgetpilot.ingress.IngressType;
import uk.gubin.budgetpilot.mapper.UserMapper;
import uk.gubin.budgetpilot.service.LoginOtpService;
import uk.gubin.budgetpilot.service.UserService;
import uk.gubin.budgetpilot.vo.LoginVO;
import uk.gubin.budgetpilot.vo.OtpChallengeVO;
import uk.gubin.budgetpilot.vo.UserVO;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final LoginOtpService loginOtpService;
    private final IngressProperties ingressProperties;

    /**
     * 登录：Tailscale / 局域网直接创建会话；公网来源先发送验证码。
     */
    @PostMapping("/login")
    public uk.gubin.budgetpilot.common.Result<?> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        IngressInfo ingress = IngressContext.get(request);
        User user = userService.authenticate(dto, ingress.clientIp());
        if (ingress.isPublic()) {
            return uk.gubin.budgetpilot.common.Result.ok(loginOtpService.start(user, ingress));
        }
        return uk.gubin.budgetpilot.common.Result.ok(
                userService.createSession(user, 0, 0, ingress.type()));
    }

    /**
     * 校验公网登录验证码并创建会话。
     */
    @PostMapping("/verify")
    public uk.gubin.budgetpilot.common.Result<LoginVO> verify(@Valid @RequestBody OtpVerifyDTO dto) {
        User user = loginOtpService.verify(dto.getChallengeId(), dto.getCode());
        return uk.gubin.budgetpilot.common.Result.ok(userService.createSession(
                user,
                ingressProperties.getPublicSessionTimeout(),
                ingressProperties.getPublicActiveTimeout(),
                IngressType.CLOUDFLARE));
    }

    /**
     * 重新发送验证码。
     */
    @PostMapping("/otp/resend")
    public uk.gubin.budgetpilot.common.Result<OtpChallengeVO> resend(@Valid @RequestBody OtpResendDTO dto,
                                                                     HttpServletRequest request) {
        IngressInfo ingress = IngressContext.get(request);
        return uk.gubin.budgetpilot.common.Result.ok(loginOtpService.resend(dto.getChallengeId(), ingress));
    }

    /**
     * 诊断接口：返回当前请求的来源类型，便于验证 Cloudflare / Tailscale 判定。
     */
    @GetMapping("/ingress")
    public uk.gubin.budgetpilot.common.Result<Map<String, String>> ingress(HttpServletRequest request) {
        IngressInfo info = IngressContext.get(request);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("ingress", info.type().name());
        result.put("clientIp", info.clientIp());
        return uk.gubin.budgetpilot.common.Result.ok(result);
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
