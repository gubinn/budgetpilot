package uk.gubin.budgetpilot.filter;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.ingress.IngressContext;
import uk.gubin.budgetpilot.ingress.IngressInfo;
import uk.gubin.budgetpilot.ingress.IngressType;
import uk.gubin.budgetpilot.mapper.UserMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * API Key 过滤器
 * 如果请求头中没有 Authorization token，但有 X-Api-Key，则自动登录
 */
@Slf4j
@RequiredArgsConstructor
public class ApiKeyFilter implements Filter {

    private final UserMapper userMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        String path = httpReq.getRequestURI();
        // 只对 /api/v1/ 开头的路径做 API Key 校验
        if (!path.startsWith("/api/v1/") || path.startsWith("/api/v1/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        String authorization = httpReq.getHeader("Authorization");

        // 如果有 Authorization token，走 SaToken 拦截器校验
        if (authorization != null && !authorization.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        // 没有 token，但有 X-Api-Key，自动登录
        String apiKey = httpReq.getHeader("X-Api-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            // 公网来源禁止使用长期 API Key，必须走登录 + 验证码
            IngressInfo ingress = IngressContext.get(httpReq);
            if (ingress.type() == IngressType.CLOUDFLARE) {
                httpResp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResp.setCharacterEncoding(StandardCharsets.UTF_8.name());
                httpResp.setContentType("application/json;charset=UTF-8");
                httpResp.getWriter().write("{\"code\":80002,\"message\":\"公网访问不允许使用 API Key\"}");
                return;
            }

            // 通过 userId 找 apiKey
            User user = userMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                            .eq(User::getApiKey, apiKey)
                            .eq(User::getIsActive, true)
            );
            if (user != null) {
                StpUtil.login(user.getId());
            }
        }

        chain.doFilter(request, response);
    }
}
