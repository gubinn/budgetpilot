package uk.gubin.budgetpilot.ingress;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IngressResolverTest {

    private IngressResolver resolver;

    @BeforeEach
    void setUp() {
        IngressProperties properties = new IngressProperties();
        properties.setTrustedProxies(List.of("100.64.1.10"));
        resolver = new IngressResolver(properties);
        resolver.init();
    }

    @Test
    void tailscalePeerWithoutCloudflareHeaders() {
        MockHttpServletRequest request = request("100.64.2.20");
        assertEquals(IngressType.TAILSCALE, resolver.resolve(request).type());
    }

    @Test
    void trustedProxyWithCloudflareHeadersIsPublic() {
        MockHttpServletRequest request = request("100.64.1.10");
        request.addHeader("CF-Ray", "8f00-HKG");
        request.addHeader("CF-Connecting-IP", "203.0.113.9");
        IngressInfo info = resolver.resolve(request);
        assertEquals(IngressType.CLOUDFLARE, info.type());
        assertEquals("203.0.113.9", info.clientIp());
        assertEquals("100.64.1.10", info.peerIp());
    }

    @Test
    void trustedProxyWithoutCloudflareHeadersIsTailscale() {
        MockHttpServletRequest request = request("100.64.1.10");
        assertEquals(IngressType.TAILSCALE, resolver.resolve(request).type());
    }

    @Test
    void lanPeerIsLan() {
        MockHttpServletRequest request = request("192.168.1.50");
        assertEquals(IngressType.LAN, resolver.resolve(request).type());
    }

    @Test
    void publicPeerIsUnknown() {
        MockHttpServletRequest request = request("8.8.8.8");
        assertEquals(IngressType.UNKNOWN, resolver.resolve(request).type());
    }

    @Test
    void forgedCloudflareHeadersFromTailscaleStillRequireOtp() {
        MockHttpServletRequest request = request("100.64.2.20");
        request.addHeader("CF-Ray", "8f00-HKG");
        request.addHeader("CF-Connecting-IP", "203.0.113.9");
        assertEquals(IngressType.CLOUDFLARE, resolver.resolve(request).type());
    }

    @Test
    void ipv4MappedIpv6PeerIsNormalized() {
        MockHttpServletRequest request = request("::ffff:100.64.1.10");
        request.addHeader("CF-Ray", "8f00-HKG");
        request.addHeader("CF-Connecting-IP", "203.0.113.9");
        assertEquals(IngressType.CLOUDFLARE, resolver.resolve(request).type());
    }

    private static MockHttpServletRequest request(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}