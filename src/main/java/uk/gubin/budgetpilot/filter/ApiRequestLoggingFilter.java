package uk.gubin.budgetpilot.filter;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.gubin.budgetpilot.common.ReadonlyContext;
import uk.gubin.budgetpilot.ingress.IngressContext;
import uk.gubin.budgetpilot.ingress.IngressInfo;

import java.io.IOException;

/**
 * API 访问审计日志。
 *
 * <p>业务异常由 {@code GlobalExceptionHandler} 记录，这里负责补上请求维度的
 * 信息：方法、路径、状态码、耗时、用户和只读分享。GET 正常请求只记 DEBUG，
 * 写请求记 INFO，HTTP 错误和慢请求记 WARN，5xx 记 ERROR，避免刷日志。</p>
 *
 * <p>只记录 URI，不记录查询参数、请求体和 Authorization，避免把搜索词、
 * 交易备注或令牌写进日志。</p>
 */
@Slf4j
@Component
@Order(1)
public class ApiRequestLoggingFilter implements Filter {

    private static final long SLOW_REQUEST_MS = 3000;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!httpRequest.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        long start = System.nanoTime();
        Throwable failure = null;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException e) {
            failure = e;
            throw e;
        } finally {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            int status = httpResponse.getStatus();
            String method = httpRequest.getMethod();
            String path = httpRequest.getRequestURI();
            boolean write = isWrite(method);
            Long userId = currentUserId();
            Long shareId = ReadonlyContext.getShareId();
            IngressInfo ingress = IngressContext.get(httpRequest);

            if (failure != null) {
                log.error("API {} {} failed after {} ms (user={}, share={}, ingress={})",
                        method, path, costMs, value(userId), value(shareId), ingress.type(), failure);
            } else if (status >= 500) {
                log.error("API {} {} -> {} ({} ms, user={}, share={}, ingress={})",
                        method, path, status, costMs, value(userId), value(shareId), ingress.type());
            } else if (status >= 400) {
                log.warn("API {} {} -> {} ({} ms, user={}, share={}, ingress={})",
                        method, path, status, costMs, value(userId), value(shareId), ingress.type());
            } else if (costMs >= SLOW_REQUEST_MS) {
                log.warn("Slow API {} {} -> {} ({} ms, user={}, share={}, ingress={})",
                        method, path, status, costMs, value(userId), value(shareId), ingress.type());
            } else if (write) {
                log.info("API {} {} -> {} ({} ms, user={}, share={}, ingress={})",
                        method, path, status, costMs, value(userId), value(shareId), ingress.type());
            } else {
                log.debug("API {} {} -> {} ({} ms, user={}, share={}, ingress={})",
                        method, path, status, costMs, value(userId), value(shareId), ingress.type());
            }
        }
    }

    private static boolean isWrite(String method) {
        return !"GET".equals(method) && !"HEAD".equals(method) && !"OPTIONS".equals(method);
    }

    private static Long currentUserId() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            return loginId == null ? null : Long.valueOf(String.valueOf(loginId));
        } catch (Exception e) {
            // 未登录、令牌无效或不在 Sa-Token 上下文时，只记匿名请求。
            return null;
        }
    }

    private static String value(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}