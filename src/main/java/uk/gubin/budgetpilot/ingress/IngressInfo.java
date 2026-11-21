package uk.gubin.budgetpilot.ingress;

/**
 * 一次请求的来源信息。
 *
 * @param type      来源类型
 * @param peerIp    TCP 对端地址（不可伪造）
 * @param clientIp  真实客户端地址（仅公网来源才可能来自 CF-Connecting-IP）
 */
public record IngressInfo(IngressType type, String peerIp, String clientIp) {

    public boolean isPublic() {
        return type == IngressType.CLOUDFLARE;
    }
}