package uk.gubin.budgetpilot.ingress;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 轻量 CIDR 匹配器，支持 IPv4 / IPv6。
 */
public final class CidrMatcher {

    private final byte[] network;
    private final int prefixLength;

    public CidrMatcher(String cidr) {
        String[] parts = cidr.trim().split("/", 2);
        try {
            this.network = InetAddress.getByName(parts[0].trim()).getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("非法的 CIDR: " + cidr, e);
        }
        this.prefixLength = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : network.length * 8;
        if (prefixLength < 0 || prefixLength > network.length * 8) {
            throw new IllegalArgumentException("非法的 CIDR 前缀: " + cidr);
        }
    }

    public boolean matches(InetAddress address) {
        byte[] addr = address.getAddress();
        if (addr.length != network.length) {
            return false;
        }
        int fullBytes = prefixLength / 8;
        for (int i = 0; i < fullBytes; i++) {
            if (addr[i] != network[i]) {
                return false;
            }
        }
        int remainingBits = prefixLength % 8;
        if (remainingBits == 0) {
            return true;
        }
        int mask = 0xFF << (8 - remainingBits);
        return (addr[fullBytes] & mask) == (network[fullBytes] & mask);
    }
}