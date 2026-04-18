package uk.gubin.budgetpilot.filter;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gubin.budgetpilot.entity.User;
import uk.gubin.budgetpilot.mapper.UserMapper;

import java.io.IOException;

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
