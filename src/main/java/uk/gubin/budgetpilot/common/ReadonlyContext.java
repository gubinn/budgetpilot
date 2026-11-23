package uk.gubin.budgetpilot.common;

/**
 * 只读共享会话的请求上下文。
 *
 * <p>只读令牌统一以 {@code ro_} 开头，过滤器识别后写入这里；
 * 真正的权限边界是过滤器里的写请求拒绝，而不是前端隐藏按钮。</p>
 */
public final class ReadonlyContext {

    private static final ThreadLocal<Long> SHARE_ID = new ThreadLocal<>();

    private ReadonlyContext() {
    }

    public static void set(Long shareId) {
        SHARE_ID.set(shareId);
    }

    public static Long getShareId() {
        return SHARE_ID.get();
    }

    public static boolean isReadOnly() {
        return SHARE_ID.get() != null;
    }

    public static void clear() {
        SHARE_ID.remove();
    }
}