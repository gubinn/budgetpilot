package uk.gubin.budgetpilot.ingress;

/**
 * 请求来源类型。
 *
 * <p>区分依据是 TCP 对端地址 + Cloudflare 注入的请求头，而不是 Host 或域名。</p>
 */
public enum IngressType {
    /** 经 Cloudflare Tunnel 从公网进入（需要额外验证码校验） */
    CLOUDFLARE,
    /** 经 Tailscale 内网进入 */
    TAILSCALE,
    /** 本机局域网进入 */
    LAN,
    /** 本机回环地址 */
    LOOPBACK,
    /** 无法识别的来源（默认拒绝） */
    UNKNOWN
}