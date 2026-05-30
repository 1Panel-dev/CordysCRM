package cn.cordys.crm.integration.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;

public class HttpRequestUtil {

    private static final HttpClient client = HttpClient.newHttpClient();

    // 发送 GET 请求
    public static String sendGetRequest(String url, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET(); // 设置请求方法为 GET

        // 设置请求头
        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        HttpRequest request = requestBuilder.build();

        // 发送请求并获取响应
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response);
    }

    // 发送 POST 请求
    public static String sendPostRequest(String url, String body, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(BodyPublishers.ofString(body, StandardCharsets.UTF_8)) // 设置请求方法为 POST 和请求体
                .header("Content-Type", "application/json");  // 默认设置 Content-Type 为 application/json

        // 设置请求头
        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        HttpRequest request = requestBuilder.build();

        // 发送请求并获取响应
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response);
    }

    // 处理 HTTP 响应
    private static String handleResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String responseBody = response.body();
        if (statusCode >= 200 && statusCode < 300) {
            // 成功响应
            return responseBody;
        } else {
            // 非成功响应
            return "Error: " + statusCode + " - " + responseBody;
        }
    }


    /**
     * url转换
     */
    public static String urlTransfer(String urlPattern, Object... params) {
        Object[] vars = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            if (Objects.isNull(params[i])) {
                vars[i] = "";
                continue;
            }
            String var = StringUtils.stripToEmpty(params[i].toString());
            vars[i] = URLEncoder.encode(var, StandardCharsets.UTF_8);
        }
        return MessageFormat.format(urlPattern, vars);
    }

}