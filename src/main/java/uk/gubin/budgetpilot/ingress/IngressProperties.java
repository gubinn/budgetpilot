package uk.gubin.budgetpilot.ingress;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 来源识别与公网登录校验配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "budgetpilot.ingress")
public class IngressProperties {

    /** Cloudflare Tunnel connector 的 Tailscale IP（只信任这些对端发来的 CF 头） */
    private List<String> trustedProxies = new ArrayList<>();

    /** Tailscale CGNAT / IPv6 网段 */
    private List<String> tailscaleCidrs = List.of("100.64.0.0/10", "fd7a:115c:a1e0::/48");

    /** 局域网网段 */
    private List<String> lanCidrs = List.of("127.0.0.0/8", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16");

    /** 是否拒绝无法识别的来源 */
    private boolean blockUnknown = true;

    /** 公网登录后的 token 有效期（秒），默认 12 小时 */
    private long publicSessionTimeout = 43200;

    /** 公网登录后的空闲超时（秒），默认 1 小时 */
    private long publicActiveTimeout = 3600;

    /** 验证码有效期（秒） */
    private long otpTtl = 300;

    /** 单个验证码最多尝试次数 */
    private int otpMaxAttempts = 5;

    /** 同一用户两次发送验证码的最小间隔（秒） */
    private long otpResendSeconds = 60;

    /** 同一 IP 10 分钟内最多发送验证码次数 */
    private int otpIpLimit = 10;
}