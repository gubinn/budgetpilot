package uk.gubin.budgetpilot.common;

/**
 * 非请求线程（定时任务、异步事件）的租户上下文。
 *
 * <p>租户拦截器和 MetaObjectHandler 优先读取这里；没有设置时才回退到 Sa-Token 登录态。
 * 定时任务按用户循环时设置，异步线程池通过 TaskDecorator 从提交线程复制。</p>
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Long get() {
        return CURRENT.get();
    }

    public static void set(Long userId) {
        if (userId == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(userId);
        }
    }

    public static void clear() {
        CURRENT.remove();
    }
}