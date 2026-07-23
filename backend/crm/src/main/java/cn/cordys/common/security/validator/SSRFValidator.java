package cn.cordys.common.security.validator;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.*;
import java.util.*;

@Component
@Slf4j
public class SSRFValidator {

    @Value("${allowed.ip.ranges.enabled}")
    private Boolean whitelistEnabled;

    @Value("#{'${allowed.ip.ranges:}'.split(',')}")
    private List<String> allowedList;

    private static final List<String> ALLOWED_PROTOCOLS = List.of("http", "https");

    // ========== DNS rebinding & redirect notice ==========

    /**
     * <strong>Security notice</strong>
     * <ul>
     *   <li>This validation only verifies the target URL <em>once</em>.
     *       To prevent DNS rebinding, callers MUST use the IP addresses
     *       returned by {@link #resolveHost(String)} and connect directly
     *       to those IPs (using a custom Host header if necessary).</li>
     *   <li>Callers MUST disable automatic HTTP redirect following and
     *       re‑validate every redirect target through this service.</li>
     * </ul>
     */
    public void validate(String url) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("URL 不能为空");
        }

        URI uri = parseUri(url);

        // 1. 协议白名单
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_PROTOCOLS.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("仅允许 http/https 协议");
        }

        // 2. 禁止用户信息
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URL 中不允许包含用户信息");
        }

        String host = extractHost(uri);
        if (StringUtils.isBlank(host)) {
            throw new IllegalArgumentException("URL 缺少主机名");
        }

        // 3. 白名单检查（基于主机名）
        validateAgainstWhitelist(host);

        // 4. 解析并检查 IP
        InetAddress inetAddress = resolveHost(host);
        validateIpAddress(inetAddress);
        log.info("SSRF validation passed for {} -> {}", host, inetAddress.getHostAddress());
    }

    // ==================== public helpers ====================

    /**
     * 解析主机名并返回所有 IP 地址。
     * 调用方应直接使用这些 IP 建立连接以防止 DNS 重绑定。
     */
    public List<InetAddress> resolveSafeAddresses(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                validateIpAddress(addr);
            }
            return Collections.unmodifiableList(Arrays.asList(addresses));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("无法解析主机名: " + host, e);
        }
    }

    // ==================== private ====================

    /**
     * 从 URI 中提取主机名，并移除可能存在的 IPv6 zone ID（如 eth0）
     */
    private String extractHost(URI uri) {
        String host = uri.getHost();
        if (host != null && host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        // 移除 zone id (e.g. fe80::1%eth0 -> fe80::1)
        int zoneIdx = host.indexOf('%');
        if (zoneIdx > 0) {
            host = host.substring(0, zoneIdx);
        }
        return host;
    }

    private void validateIpAddress(InetAddress addr) {
        // 0.0.0.0 / ::
        if (addr.isAnyLocalAddress()) {
            throw new IllegalArgumentException("禁止访问任意本地地址");
        }

        // 回环 (127.0.0.0/8, ::1)
        if (addr.isLoopbackAddress()) {
            throw new IllegalArgumentException("禁止访问回环地址");
        }

        // 链路本地 (169.254.0.0/16, fe80::/10)
        if (addr.isLinkLocalAddress()) {
            throw new IllegalArgumentException("禁止访问链路本地地址（含云元数据）");
        }

        // 站点本地（私有）: IPv4 site-local, IPv6 deprecated site-local fec0::/10
        if (addr.isSiteLocalAddress()) {
            throw new IllegalArgumentException("禁止访问内网地址");
        }

        // 多播
        if (addr.isMulticastAddress()) {
            throw new IllegalArgumentException("禁止访问多播地址");
        }

        // ---- 处理 IPv4-mapped IPv6 (::ffff:x.x.x.x) ----
        if (addr instanceof Inet6Address) {
            InetAddress embedded = getEmbeddedIPv4Address((Inet6Address) addr);
            if (embedded != null) {
                // 递归校验内嵌的 IPv4 地址
                validateIpAddress(embedded);
                return; // 无需再检查 IPv6 特有段
            }

            // NAT64: 64:ff9b::/96
            embedded = extractIPv4FromNAT64((Inet6Address) addr);
            if (embedded != null) {
                validateIpAddress(embedded);
                return;
            }

            // 6to4: 2002::/16
            embedded = extractIPv4From6to4((Inet6Address) addr);
            if (embedded != null) {
                validateIpAddress(embedded);
                return;
            }

            // Teredo: 2001::/32
            embedded = extractIPv4FromTeredo((Inet6Address) addr);
            if (embedded != null) {
                validateIpAddress(embedded);
                return;
            }


            // IPv6 ULA (fc00::/7)
            if (isIPv6ULA((Inet6Address) addr)) {
                throw new IllegalArgumentException("禁止访问 IPv6 唯一本地地址 (ULA)");
            }
        }

        // ---- IPv4 特殊前缀兜底 ----
        if (addr instanceof Inet4Address) {
            String ip = addr.getHostAddress();
            for (String prefix : BLOCKED_IPV4_PREFIXES) {
                if (ip.startsWith(prefix)) {
                    throw new IllegalArgumentException("禁止访问保留地址: " + ip);
                }
            }
        }
    }

    /**
     * 从 IPv6 地址中提取内嵌的 IPv4 地址（仅支持 IPv4‑mapped 格式 ::ffff:x.x.x.x）
     */
    private InetAddress getEmbeddedIPv4Address(Inet6Address addr) {
        byte[] bytes = addr.getAddress();
        // IPv4-mapped: 前10字节为0，第11、12字节为0xFF，后4字节为IPv4
        if (bytes.length == 16) {
            for (int i = 0; i < 10; i++) {
                if (bytes[i] != 0) return null;
            }
            if (bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF) {
                try {
                    return InetAddress.getByAddress(
                            Arrays.copyOfRange(bytes, 12, 16));
                } catch (UnknownHostException e) {
                    // 不应发生
                    return null;
                }
            }
        }
        return null;
    }


    /**
     * NAT64 提取 (64:ff9b::/96)
     *
     * @param addr
     * @return
     */
    private InetAddress extractIPv4FromNAT64(Inet6Address addr) {
        byte[] bytes = addr.getAddress();
        if (bytes.length != 16) return null;
        // 前缀: 64 ff 9b，后面9个字节（从索引3到11）必须为0
        if (bytes[0] == 0x64 && bytes[1] == (byte) 0xff && bytes[2] == (byte) 0x9b) {
            for (int i = 3; i < 12; i++) {
                if (bytes[i] != 0) return null;
            }
            try {
                return InetAddress.getByAddress(Arrays.copyOfRange(bytes, 12, 16));
            } catch (UnknownHostException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 6to4 提取 (2002::/16)
     *
     * @param addr
     * @return
     */
    private InetAddress extractIPv4From6to4(Inet6Address addr) {
        byte[] bytes = addr.getAddress();
        if (bytes.length != 16) return null;
        // 前缀: 20 02
        if (bytes[0] == 0x20 && bytes[1] == 0x02) {
            // IPv4 位于索引 2~5
            try {
                return InetAddress.getByAddress(Arrays.copyOfRange(bytes, 2, 6));
            } catch (UnknownHostException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Teredo 提取 (2001::/32)
     *
     * @param addr
     * @return
     */
    private InetAddress extractIPv4FromTeredo(Inet6Address addr) {
        byte[] bytes = addr.getAddress();
        if (bytes.length != 16) return null;
        // 前缀: 20 01 00 00 (2001::/32)
        if (bytes[0] == 0x20 && bytes[1] == 0x01 && bytes[2] == 0x00 && bytes[3] == 0x00) {
            // Teredo 客户端 IPv4 位于最后 4 个字节，需要按位取反
            byte[] ipv4 = Arrays.copyOfRange(bytes, 12, 16);
            for (int i = 0; i < 4; i++) {
                ipv4[i] = (byte) ~ipv4[i];
            }
            try {
                return InetAddress.getByAddress(ipv4);
            } catch (UnknownHostException e) {
                return null;
            }
        }
        return null;
    }


    /**
     * 检查是否为 IPv6 唯一本地地址 (fc00::/7)
     */
    private boolean isIPv6ULA(Inet6Address addr) {
        byte[] bytes = addr.getAddress();
        // fc00::/7 → 第一个字节的高7位为 1111 110
        int first = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
        return first >= 0xFC00 && first <= 0xFDFF;
    }

    // ==================== DNS & URI helpers ====================
    private static URI parseUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL 格式不合法", e);
        }
    }

    private static InetAddress resolveHost(String host) {
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("无法解析主机名: " + host, e);
        }
    }

    // ==================== whitelist ====================
    public void validateAgainstWhitelist(String host) {
        if (!Boolean.TRUE.equals(whitelistEnabled)) {
            return;
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT).trim();
        List<String> allowed = allowedList == null ? Collections.emptyList() : allowedList;

        boolean matched = allowed.stream()
                .filter(Objects::nonNull)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(StringUtils::isNotBlank)
                .anyMatch(pattern -> matchesPattern(normalizedHost, pattern));

        if (!matched) {
            throw new GenericException(Translator.get("dashboard_url_not_allowed"));
        }
    }

    private static boolean matchesPattern(String host, String pattern) {
        if ("*".equals(pattern)) {
            return true;
        }
        if (pattern.startsWith("*.")) {
            return host.endsWith(pattern.substring(1));
        }
        return host.equals(pattern);
    }

    /**
     * 仅校验主机名是否安全（不限制协议）。
     * 适用于 SMTP、LDAP、数据库等出站连接。
     * 若全局白名单启用，则主机必须匹配白名单模式。
     *
     * @param host 主机名或 IP（建议不含协议和端口）
     * @throws IllegalArgumentException 如果主机不安全或被白名单禁止
     */
    public void validateHost(String host) {
        if (StringUtils.isBlank(host)) {
            throw new IllegalArgumentException("主机名不能为空");
        }

        // 1. 剥离可能误传入的端口号
        String cleanedHost = host.trim();
        int colonIdx = cleanedHost.lastIndexOf(':');
        if (colonIdx > 0 && !cleanedHost.startsWith("[")) {
            cleanedHost = cleanedHost.substring(0, colonIdx);
        }

        // 2. 白名单检查（如果启用）
        validateAgainstWhitelist(cleanedHost);

        // 3. 解析 IP 并执行安全策略（内网/回环/保留地址禁止）
        InetAddress inetAddress = resolveHost(cleanedHost);
        validateIpAddress(inetAddress);
        log.info("主机安全校验通过：{} -> {}", cleanedHost, inetAddress.getHostAddress());
    }

    // ==================== constants ====================
    /**
     * 需要显式禁止的 IPv4 前缀（补充 InetAddress 未覆盖的特殊段）。
     * 0.0.0.0/8、D 类、E 类等。
     */
    private static final List<String> BLOCKED_IPV4_PREFIXES = List.of(
            "0.",          // 当前网络 (0.0.0.0/8)
            "224.", "225.", "226.", "227.", "228.", "229.",
            "230.", "231.", "232.", "233.", "234.", "235.",
            "236.", "237.", "238.", "239.",             // D 类多播 224.0.0.0/4
            "240.", "241.", "242.", "243.", "244.", "245.",
            "246.", "247.", "248.", "249.", "250.", "251.",
            "252.", "253.", "254.", "255."               // E 类保留 240.0.0.0/4
    );
}