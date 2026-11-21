package uk.gubin.budgetpilot.ingress;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 从当前请求中读取来源信息的工具类。
 */
public final class IngressContext {

    public static final String ATTRIBUTE = IngressContext.class.getName();

    private IngressContext() {
    }

    public static IngressInfo get(HttpServletRequest request) {
        Object value = request.getAttribute(ATTRIBUTE);
        if (value instanceof IngressInfo info) {
            return info;
        }
        String remote = request.getRemoteAddr();
        return new IngressInfo(IngressType.UNKNOWN, remote, remote);
    }
}