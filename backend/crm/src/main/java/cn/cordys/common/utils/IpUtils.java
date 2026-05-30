package cn.cordys.common.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

public class IpUtils {
    private static final List<String> IP_HEADER_CANDIDATES = Arrays.asList(
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    );

    private static final String UNKNOWN = "unknown";

    public static String getClientIpAddress(HttpServletRequest request) {
        return IP_HEADER_CANDIDATES.stream()
                .map(request::getHeader)
                .filter(ip -> ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip))
                .map(IpUtils::getFirstIp)
                .findFirst()
                .orElse(request.getRemoteAddr());
    }

    private static String getFirstIp(String ip) {
        int index = ip.indexOf(',');
        return index != -1 ? ip.substring(0, index) : ip;
    }
}