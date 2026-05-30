package cn.cordys.crm.system.dto.response;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public class SafeHostValidator implements ConstraintValidator<SafeHost, String> {

    private static final List<String> BLOCKED_CIDRS = Arrays.asList(
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16",
            "127.0.0.0/8",
            "169.254.0.0/16",
            "0.0.0.0/8",
            "100.64.0.0/10",
            "224.0.0.0/4",
            "::1/128",
            "fe80::/10",
            "fc00::/7"
    );

    @Override
    public boolean isValid(String host, ConstraintValidatorContext context) {
        if (host == null || host.isEmpty()) {
            return true; // 由 @NotNull 控制
        }
        try {
            InetAddress addr = InetAddress.getByName(host);
            return !isBlocked(addr);
        } catch (UnknownHostException e) {
            // 无法解析的域名一律放行？建议拒绝，因为可能被用来绕过（可改为 return false）
            return false;
        }
    }

    private boolean isBlocked(InetAddress addr) {
        for (String cidr : BLOCKED_CIDRS) {
            if (matchesCIDR(addr, cidr)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesCIDR(InetAddress addr, String cidr) {
        try {
            String[] parts = cidr.split("/");
            InetAddress network = InetAddress.getByName(parts[0]);
            int prefix = Integer.parseInt(parts[1]);
            byte[] addrBytes = addr.getAddress();
            byte[] networkBytes = network.getAddress();
            if (addrBytes.length != networkBytes.length) return false;

            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (addrBytes[i] != networkBytes[i]) return false;
            }
            if (remainingBits > 0 && fullBytes < addrBytes.length) {
                int mask = (0xFF << (8 - remainingBits)) & 0xFF;
                return (addrBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}