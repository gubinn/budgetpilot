package uk.gubin.budgetpilot.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import uk.gubin.budgetpilot.common.ReadonlyContext;
import uk.gubin.budgetpilot.service.ReadonlySessionManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 只读共享会话的写保护。
 *
 * <p>只读令牌统一以 {@code ro_} 开头，不依赖前端隐藏按钮；任何非 GET 请求
 * （/auth/logout 除外）以及用户/分享管理接口都会在这里被拒绝。</p>
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class ReadonlyFilter implements Filter {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!httpRequest.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        // 分享页的公开检查/验证接口不参与只读会话判断：
// 即使浏览器里还留着已撤销的旧令牌，也必须能重新打开分享页
        if (httpRequest.getRequestURI().startsWith("/api/v1/shares/public/")) {
            chain.doFilter(request, response);
            return;
        }

        String token = resolveToken(httpRequest);
        Long shareId = shareIdOf(token);
        if (shareId == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            if (isRevoked(shareId)) {
                log.warn("Readonly session rejected: shareId={} has been revoked ({} {})",
                        shareId, httpRequest.getMethod(), httpRequest.getRequestURI());
                writeJson(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, 80001, "只读会话已失效");
                return;
            }
        } catch (Exception e) {
            log.error("Failed to check readonly share {} status", shareId, e);
            writeJson(httpResponse, HttpServletResponse.SC_SERVICE_UNAVAILABLE, 10003, "服务暂不可用");
            return;
        }

        ReadonlyContext.set(shareId);
        try {
            if (!isAllowed(httpRequest.getMethod(), httpRequest.getRequestURI())) {
                log.warn("Readonly session rejected write: shareId={} {} {}",
                        shareId, httpRequest.getMethod(), httpRequest.getRequestURI());
                writeJson(httpResponse, HttpServletResponse.SC_FORBIDDEN, 80002,
                        "只读访问，不能修改数据");
                return;
            }
            chain.doFilter(request, response);
        } finally {
            ReadonlyContext.clear();
        }
    }

    /**
     * package-private，便于单测覆盖放行规则。
     */
    static boolean isAllowed(String method, String uri) {
        // 分享页的检查和验证接口必须放行，否则已有只读会话无法打开另一条分享
        if (uri.startsWith("/api/v1/shares/public/")) {
            return true;
        }
        if (uri.startsWith("/api/v1/shares")) {
            return false;
        }
        if (uri.startsWith("/api/v1/users")) {
            return false;
        }
        if (uri.startsWith("/api/v1/system/config")) {
            return false;
        }
        if (uri.startsWith("/api/v1/system/alerts")) {
            return false;
        }
        // 只读范围外的业务页（对账、周期、预警、商户）直接拒绝
        if (uri.startsWith("/api/v1/reconciliation")
                || uri.startsWith("/api/v1/recurring-rules")
                || uri.startsWith("/api/v1/alert-rules")
                || uri.startsWith("/api/v1/merchants")) {
            return false;
        }
        if (uri.startsWith("/api/v1/auth/api-key")) {
            return false;
        }
        if (uri.startsWith("/api/v1/auth")) {
            return ("GET".equals(method) && "/api/v1/auth/info".equals(uri))
                    || ("POST".equals(method) && "/api/v1/auth/logout".equals(uri));
        }
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return true;
        }
        return false;
    }

    private boolean isRevoked(Long shareId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(
                String.format(ReadonlySessionManager.SHARE_REVOKED_KEY, shareId)));
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && !header.isBlank()) {
            String value = header.trim();
            return value.startsWith("Bearer ") ? value.substring(7).trim() : value;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("Authorization".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private Long shareIdOf(String token) {
        if (token == null || !token.startsWith(ReadonlySessionManager.TOKEN_PREFIX)) {
            return null;
        }
        int first = token.indexOf('_');
        int second = token.indexOf('_', first + 1);
        if (first < 0 || second <= first + 1) {
            return null;
        }
        try {
            return Long.parseLong(token.substring(first + 1, second));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void writeJson(HttpServletResponse response, int status, int code, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\"}");
    }
}