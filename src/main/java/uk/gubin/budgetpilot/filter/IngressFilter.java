package uk.gubin.budgetpilot.filter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.gubin.budgetpilot.ingress.IngressContext;
import uk.gubin.budgetpilot.ingress.IngressInfo;
import uk.gubin.budgetpilot.ingress.IngressProperties;
import uk.gubin.budgetpilot.ingress.IngressResolver;
import uk.gubin.budgetpilot.ingress.IngressType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 来源识别过滤器：识别 Tailscale / 公网 / 局域网，默认拒绝未知来源。
 */
@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class IngressFilter implements Filter {

    private final IngressResolver ingressResolver;
    private final IngressProperties ingressProperties;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (httpRequest.getDispatcherType() == DispatcherType.ERROR) {
            chain.doFilter(request, response);
            return;
        }

        IngressInfo ingress = ingressResolver.resolve(httpRequest);
        httpRequest.setAttribute(IngressContext.ATTRIBUTE, ingress);

        httpResponse.setHeader("X-BudgetPilot-Ingress", ingress.type().name());
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("Referrer-Policy", "no-referrer");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        if (ingress.isPublic()) {
            httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000");
        }

        if (ingress.isPublic() && httpRequest.getRequestURI().startsWith("/api/")) {
            log.info("公网请求 {} {} clientIp={} peer={}",
                    httpRequest.getMethod(), httpRequest.getRequestURI(), ingress.clientIp(), ingress.peerIp());
        }

        if (ingress.type() == IngressType.UNKNOWN && ingressProperties.isBlockUnknown()) {
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"code\":80002,\"message\":\"来源不可信，已拒绝访问\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}