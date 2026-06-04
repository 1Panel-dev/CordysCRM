package cn.cordys.crm.integration.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP 请求工具类，基于 Spring RestTemplate 实现，内建重试机制。
 *
 * <p>默认重试 3 次，采用指数退避策略（1s → 2s → 4s，上限 30s）。
 * 支持 GET / POST / 通用 method 请求，可通过重载方法自定义重试次数。</p>
 */
@Slf4j
public class HttpClientUtils {

    /**
     * 默认最大重试次数
     */
    private static final int DEFAULT_MAX_RETRIES = 3;

    /**
     * 默认连接超时
     */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * 默认读取超时
     */
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 退避时间上限
     */
    private static final long MAX_BACKOFF_MS = Duration.ofSeconds(30).toMillis();

    private static final RestTemplate restTemplate;

    static {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        restTemplate = new RestTemplate(factory);
    }

    /**
     * 发送 GET 请求（默认重试 3 次）
     */
    public static String sendGetRequest(String url, Map<String, String> headers) {
        return executeWithRetry(() -> doGet(url, headers), DEFAULT_MAX_RETRIES, url);
    }

    /**
     * 发送 GET 请求，指定重试次数
     */
    public static String sendGetRequest(String url, Map<String, String> headers, int maxRetries) {
        return executeWithRetry(() -> doGet(url, headers), maxRetries, url);
    }

    private static String doGet(String url, Map<String, String> headers) {
        var entity = new HttpEntity<>(null, buildHeaders(headers));
        return extractBody(restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, String.class));
    }

    /**
     * 发送 POST 请求（默认重试 3 次，Content-Type 默认为 application/json）
     */
    public static String sendPostRequest(String url, String body, Map<String, String> headers) {
        return executeWithRetry(() -> doPost(url, body, headers), DEFAULT_MAX_RETRIES, url);
    }

    /**
     * 发送 POST 请求，指定重试次数
     */
    public static String sendPostRequest(String url, String body, Map<String, String> headers, int maxRetries) {
        return executeWithRetry(() -> doPost(url, body, headers), maxRetries, url);
    }

    private static String doPost(String url, String body, Map<String, String> headers) {
        var httpHeaders = buildHeaders(headers);
        if (!httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        }
        var entity = new HttpEntity<>(body, httpHeaders);
        return extractBody(restTemplate.exchange(URI.create(url), HttpMethod.POST, entity, String.class));
    }

    /**
     * 发送指定 HTTP 方法的请求（默认重试 3 次）
     */
    public static String sendRequest(HttpMethod method, String url, String body, Map<String, String> headers) {
        return executeWithRetry(() -> doRequest(method, url, body, headers), DEFAULT_MAX_RETRIES, url);
    }

    /**
     * 发送指定 HTTP 方法的请求，指定重试次数
     */
    public static String sendRequest(HttpMethod method, String url, String body, Map<String, String> headers, int maxRetries) {
        return executeWithRetry(() -> doRequest(method, url, body, headers), maxRetries, url);
    }

    private static String doRequest(HttpMethod method, String url, String body, Map<String, String> headers) {
        var httpHeaders = buildHeaders(headers);
        if (body != null && !httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        }
        var entity = new HttpEntity<>(body, httpHeaders);
        return extractBody(restTemplate.exchange(URI.create(url), method, entity, String.class));
    }

    /**
     * 提取响应体：2xx 直接返回 body，非 2xx 记录日志后仍返回 body（兼容原有行为）
     */
    private static String extractBody(ResponseEntity<String> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        var msg = "Error: " + response.getStatusCode().value() + " - " + response.getBody();
        log.warn("HTTP 请求返回非成功状态码: {}", msg);
        return msg;
    }

    /**
     * 带重试的执行器，指数退避策略。
     *
     * @param callable   请求逻辑
     * @param maxRetries 最大重试次数
     * @param url        请求 URL（仅用于日志）
     *
     * @return 响应字符串
     *
     * @throws RestClientException 重试耗尽后抛出
     */
    private static String executeWithRetry(HttpCallable callable, int maxRetries, String url) {
        var retries = Math.max(maxRetries, 1);
        Exception lastException = null;

        for (int attempt = 1; attempt <= retries; attempt++) {
            try {
                return callable.call();
            } catch (Exception e) {
                lastException = e;
                if (attempt >= retries) {
                    break;
                }
                var delayMs = computeBackoff(attempt);
                log.error("HTTP 请求失败 (第 {}/{} 次), URL: {}, 原因: {}, {}ms 后重试",
                        attempt, retries, url, e.getMessage(), delayMs);
                sleepUnchecked(delayMs);
            }
        }

        log.error("HTTP 请求重试 {} 次后仍然失败, URL: {}", retries, url, lastException);
        throw new RestClientException("请求失败，已重试 " + retries + " 次: " + url, lastException);
    }

    /**
     * 指数退避：1s, 2s, 4s, 8s, … 上限 30s
     */
    private static long computeBackoff(int attempt) {
        return Math.min(1_000L << (attempt - 1), MAX_BACKOFF_MS);
    }

    private static void sleepUnchecked(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RestClientException("重试等待被中断", e);
        }
    }

    /**
     * URL 模板拼接，参数自动 URL 编码，null 值替换为空字符串。
     *
     * <pre>{@code
     *   urlTransfer("https://api.example.com/{0}/{1}", token, keyword)
     * }</pre>
     */
    public static String urlTransfer(String urlPattern, Object... params) {
        var vars = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            vars[i] = (params[i] == null) ? "" : URLEncoder.encode(StringUtils.stripToEmpty(params[i].toString()), StandardCharsets.UTF_8);
        }
        return MessageFormat.format(urlPattern, vars);
    }

    private static HttpHeaders buildHeaders(Map<String, String> headers) {
        var httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::set);
        }
        return httpHeaders;
    }

    @FunctionalInterface
    private interface HttpCallable {
        String call() throws Exception;
    }
}
