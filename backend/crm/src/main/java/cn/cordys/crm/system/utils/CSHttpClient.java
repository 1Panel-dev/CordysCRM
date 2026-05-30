package cn.cordys.crm.system.utils;

import cn.cordys.common.util.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * CRM 系统 HTTP 客户端工具类。
 * <p>
 * 提供简单的 GET 请求能力，并从响应头 {@code Date} 解析服务端年份。
 * 线程安全，可复用。
 */
@Slf4j
public class CSHttpClient {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 基于完整 URL 的 GET 调用（保留以兼容旧用法）。
     *
     * @param url 绝对 URL
     *
     * @return 下载信息
     */
    public static DownloadInfo sendGetRequest(String url) {
        DownloadInfo info;
        String dateHeader = null;

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("User-Agent", "CORDYS-CRM-Client/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            dateHeader = response.headers().firstValue("Date").orElse(null);
            info = handleResponse(response);
        } catch (Exception e) {
            log.warn("GET 请求失败: {}", e.toString());
            info = new DownloadInfo();
        }

        if (info == null) {
            info = new DownloadInfo();
        }
        info.setYear(dateHeader(dateHeader));
        return info;
    }

    /**
     * 基于基础地址与查询参数的 GET 调用，自动进行 URL 编码并忽略空参数。
     *
     * @param queryParams 查询参数 Map（null/空值将被忽略）
     *
     * @return 下载信息
     */
    public static DownloadInfo sendGetRequest(Map<String, String> queryParams) {
        String baseUrl = "https://community.fit2cloud.com/api/version";
        String url = buildUrl(baseUrl, queryParams);
        return sendGetRequest(url);
    }

    /**
     * 将查询参数拼接到基础 URL，并进行 UTF-8 编码。
     *
     * @param baseUrl 基础地址
     * @param params  参数
     *
     * @return 完整 URL
     */
    private static String buildUrl(String baseUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }
        StringBuilder sb = new StringBuilder(baseUrl);
        boolean hasQuery = baseUrl.contains("?");
        for (Map.Entry<String, String> e : params.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            if (StringUtils.isAnyBlank(key, val)) {
                continue;
            }
            sb.append(hasQuery ? '&' : '?');
            hasQuery = true;
            sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(val, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private static DownloadInfo handleResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String responseBody = response.body();
        if (statusCode >= 200 && statusCode < 300 && StringUtils.isNotBlank(responseBody)) {
            try {
                return JSON.parseObject(responseBody, DownloadInfo.class);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    public static int dateHeader(String dateHeader) {
        try {
            if (StringUtils.isBlank(dateHeader)) {
                return Year.now().getValue();
            }
            ZonedDateTime gmt = ZonedDateTime.parse(dateHeader, DateTimeFormatter.RFC_1123_DATE_TIME);
            return gmt.withZoneSameInstant(ZoneId.of("Asia/Shanghai")).getYear();
        } catch (Exception e) {
            return Year.now().getValue();
        }
    }

    @Data
    public static class DownloadInfo {
        private String latestVersion;
        private String currentVersion;
        private String edition;
        private boolean updateAvailable;
        private int year;
    }
}