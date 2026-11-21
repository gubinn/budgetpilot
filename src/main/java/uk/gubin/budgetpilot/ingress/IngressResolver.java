package uk.gubin.budgetpilot.ingress;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 根据 TCP 对端地址和 Cloudflare 头判断请求来源。
 *
 * <p>关键原则：</p>
 * <ul>
 *   <li>对端地址不可伪造，是判断的基础；</li>
 *   <li>只有隧道 connector（可信代理）或 Tailscale 节点发来的 CF 头才可信；</li>
 *   <li>其他来源即使带 CF 头也不会被当作公网请求放行。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngressResolver {

    private static final String HEADER_CF_RAY = "CF-Ray";
    private static final String HEADER_CF_CONNECTING_IP = "CF-Connecting-IP";

    private final IngressProperties properties;

    private final Set<String> trustedProxies = new HashSet<>();
    private final List<CidrMatcher> tailscaleMatchers = new ArrayList<>();
    private final List<CidrMatcher> lanMatchers = new ArrayList<>();

    @PostConstruct
    void init() {
        properties.getTrustedProxies().forEach(ip -> {
            String canonical = canonicalIp(ip);
            if (canonical != null) {
                trustedProxies.add(canonical);
            }
        });
        properties.getTailscaleCidrs().forEach(cidr -> tailscaleMatchers.add(new CidrMatcher(cidr)));
        properties.getLanCidrs().forEach(cidr -> lanMatchers.add(new CidrMatcher(cidr)));
        log.info("来源识别已启用，可信代理={}, Tailscale 网段={}", trustedProxies, properties.getTailscaleCidrs());
    }

    public IngressInfo resolve(HttpServletRequest request) {
        String peerIp = canonicalIp(request.getRemoteAddr());
        if (peerIp == null) {
            peerIp = request.getRemoteAddr();
        }

        boolean hasCloudflareHeaders = isNotBlank(request.getHeader(HEADER_CF_RAY))
                && isNotBlank(request.getHeader(HEADER_CF_CONNECTING_IP));

        InetAddress address = parse(peerIp);
        boolean trustedProxy = trustedProxies.contains(peerIp);
        boolean tailscale = matches(tailscaleMatchers, address);

        if (hasCloudflareHeaders) {
            if (trustedProxy || tailscale) {
                if (!trustedProxy) {
                    // 隧道 connector 换 IP 时会走到这里：仍然按公网处理（需要验证码），只提示修正配置
                    log.warn("收到 Cloudflare 头，但对端 {} 不在可信代理列表中，已按公网来源处理", peerIp);
                }
                return new IngressInfo(IngressType.CLOUDFLARE, peerIp, resolveClientIp(request, peerIp));
            }
            log.warn("收到 Cloudflare 头，但来源 {} 既不是可信代理也不是 Tailscale 节点，已拒绝", peerIp);
            return new IngressInfo(IngressType.UNKNOWN, peerIp, peerIp);
        }

        if (trustedProxy || tailscale) {
            return new IngressInfo(IngressType.TAILSCALE, peerIp, peerIp);
        }
        if (address != null && address.isLoopbackAddress()) {
            return new IngressInfo(IngressType.LOOPBACK, peerIp, peerIp);
        }
        if (matches(lanMatchers, address)) {
            return new IngressInfo(IngressType.LAN, peerIp, peerIp);
        }
        return new IngressInfo(IngressType.UNKNOWN, peerIp, peerIp);
    }

    private String resolveClientIp(HttpServletRequest request, String fallback) {
        String cfConnectingIp = canonicalIp(request.getHeader(HEADER_CF_CONNECTING_IP));
        if (cfConnectingIp != null) {
            return cfConnectingIp;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            for (String part : forwardedFor.split(",")) {
                String candidate = canonicalIp(part);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return fallback;
    }

    private boolean matches(List<CidrMatcher> matchers, InetAddress address) {
        if (address == null) {
            return false;
        }
        for (CidrMatcher matcher : matchers) {
            if (matcher.matches(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把 IPv4-mapped IPv6（如 ::ffff:100.64.1.2）归一化成普通 IPv4。
     */
    static String canonicalIp(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        int zone = value.indexOf('%');
        if (zone > 0) {
            value = value.substring(0, zone);
        }
        if (value.startsWith("[") && value.contains("]")) {
            value = value.substring(1, value.indexOf(']'));
        }
        try {
            return InetAddress.getByName(value).getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static InetAddress parse(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}