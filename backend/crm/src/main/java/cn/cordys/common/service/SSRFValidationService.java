package cn.cordys.common.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Slf4j
public class SSRFValidationService {

    @Value("${allowed.ip.ranges.enabled}")
    private Boolean whitelistEnabled;

    @Value("#{'${allowed.ip.ranges:}'.split(',')}")
    private List<String> allowedList;

    /**
     * 只允许 http 和 https 协议
     */
    private static final List<String> ALLOWED_PROTOCOLS = List.of("http", "https");

    /**
     * 禁用的私有/保留 IP 段
     */
    private static final List<String> BLOCKED_IP_PREFIXES = List.of(
            "0.",          // 当前网络
            "10.",         // A 类私有
            "127.",        // 回环
            "169.254.",    // 链路本地（含云元数据）
            "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.",
            "172.24.", "172.25.", "172.26.", "172.27.",
            "172.28.", "172.29.", "172.30.", "172.31.", // B 类私有
            "192.168.",    // C 类私有
            "224.", "225.", "226.", "227.", "228.", "229.",
            "230.", "231.", "232.", "233.", "234.", "235.",
            "236.", "237.", "238.", "239.",             // D 类多播
            "240.", "241.", "242.", "243.", "244.", "245.",
            "246.", "247.", "248.", "249.", "250.", "251.",
            "252.", "253.", "254.", "255."               // E 类保留
    );

    /**
     * 校验 URL 是否安全，防止 SSRF
     *
     * @param url 用户提供的目标 URL
     *
     * @throws IllegalArgumentException 若 URL 不合规
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

        // 2. 禁止用户信息（防止绕过：http://safe@evil.com）
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URL 中不允许包含用户信息");
        }

        String host = uri.getHost();
        if (StringUtils.isBlank(host)) {
            throw new IllegalArgumentException("URL 缺少主机名");
        }

        // 3. 白名单检查（基于主机名，避免重复解析 URL）
        validateAgainstWhitelist(host);

        // 4. 解析为 IP 并检查
        InetAddress inetAddress = resolveHost(host);
        String ip = inetAddress.getHostAddress();
        log.info("解析 {} -> {}", host, ip);

        // 5. 回环地址
        if (inetAddress.isLoopbackAddress()) {
            throw new IllegalArgumentException("禁止访问回环地址");
        }

        // 6. 站点本地地址（私有网络）
        if (inetAddress.isSiteLocalAddress()) {
            throw new IllegalArgumentException("禁止访问内网地址");
        }

        // 7. 链路本地地址（含 169.254.x.x 云元数据）
        if (inetAddress.isLinkLocalAddress()) {
            throw new IllegalArgumentException("禁止访问链路本地地址（含云元数据）");
        }

        // 8. IP 前缀黑名单兜底（如 0.x.x.x 容器网络、D/E 类保留地址）
        for (String prefix : BLOCKED_IP_PREFIXES) {
            if (ip.startsWith(prefix)) {
                throw new IllegalArgumentException("禁止访问保留地址: " + ip);
            }
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 解析 URL 字符串为 URI
     */
    private static URI parseUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL 格式不合法", e);
        }
    }

    /**
     * DNS 解析主机名
     */
    private static InetAddress resolveHost(String host) {
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("无法解析主机名: " + host, e);
        }
    }

    /**
     * 检查主机名是否在白名单中（白名单功能启用时）
     * <p>支持三种模式：</p>
     * <ul>
     *   <li>{@code *} — 放行全部</li>
     *   <li>{@code *.example.com} — 通配子域名</li>
     *   <li>{@code exact.host.com} — 精确匹配</li>
     * </ul>
     */
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

    /**
     * 判断 host 是否匹配白名单模式
     */
    private static boolean matchesPattern(String host, String pattern) {
        if ("*".equals(pattern)) {
            return true;
        }
        if (pattern.startsWith("*.")) {
            // *.example.com → .example.com，匹配 foo.example.com
            return host.endsWith(pattern.substring(1));
        }
        return host.equals(pattern);
    }
}