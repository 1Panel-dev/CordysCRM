package cn.cordys.crm.integration.common.client;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class QrCodeClient {

  private static final RestTemplate restTemplate;
  private static final int CONNECT_TIMEOUT = 10000;

  static {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    // 设置连接超时时间（单位：毫秒）
    factory.setConnectTimeout(CONNECT_TIMEOUT);

    // 设置读取超时时间（单位：毫秒）
    factory.setReadTimeout(CONNECT_TIMEOUT);

    restTemplate = new RestTemplate(factory);
  }

  /** 通用的异常处理方法 */
  private String handleRequestError(ResponseEntity<?> forEntity) throws Exception {
    if (forEntity.getStatusCode().is4xxClientError()
        || forEntity.getStatusCode().is5xxServerError()) {
      throw new Exception("StatusCode: " + forEntity.getStatusCode());
    }
    return JSON.toJSONString(forEntity.getBody());
  }

  /** 获取 HTTP GET 请求响应 */
  public String get(String url) {
    try {
      var forEntity = restTemplate.getForEntity(url, Object.class);
      return handleRequestError(forEntity);
    } catch (Exception e) {
      log.error("HttpClient查询失败", e);
      throw new GenericException(
          Translator.get("sync.organization.http.error") + ": " + e.getMessage());
    }
  }

  /** 发送带 Header 参数的 GET 请求 */
  public String exchange(
      String url,
      String headerParam,
      String paramName,
      MediaType mediaType,
      MediaType acceptMediaType) {
    var headers = createHeaders(headerParam, paramName, mediaType, acceptMediaType);
    var request = new HttpEntity<>(headers);
    try {
      var forEntity = restTemplate.exchange(url, HttpMethod.GET, request, Object.class);
      return handleRequestError(forEntity);
    } catch (Exception e) {
      log.error("HttpClient查询失败", e);
      throw new GenericException(
          Translator.get("sync.organization.http.error") + ": " + e.getMessage());
    }
  }

  /** 发送带 Header 参数的 GET 请求，并返回 String 类型的响应 */
  public String exchangeString(
      String url,
      String headerParam,
      String paramName,
      MediaType mediaType,
      MediaType acceptMediaType) {
    var headers = createHeaders(headerParam, paramName, mediaType, acceptMediaType);
    var request = new HttpEntity<>(headers);
    try {
      var forEntity = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
      if (forEntity.getStatusCode().is4xxClientError()
          || forEntity.getStatusCode().is5xxServerError()) {
        throw new Exception("StatusCode: " + forEntity.getStatusCode());
      }
      return JSON.toJSONString(forEntity.getBody());
    } catch (Exception e) {
      log.error("HttpClient查询失败", e);
      throw new GenericException(
          Translator.get("sync.organization.http.error") + ": " + e.getMessage());
    }
  }

  /** 发送带 Header 参数的 POST 请求 */
  public String postExchange(
      String url,
      String headerParam,
      String paramName,
      Object body,
      MediaType mediaType,
      MediaType acceptMediaType) {
    var headers = createHeaders(headerParam, paramName, mediaType, acceptMediaType);
    var requestEntity = new HttpEntity<>(body, headers);
    try {
      var forEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Object.class);
      return handleRequestError(forEntity);
    } catch (Exception e) {
      log.error("HttpClient查询失败", e);
      throw new GenericException(
          Translator.get("sync.organization.http.error") + ": " + e.getMessage());
    }
  }

  /** 创建 HttpHeaders，避免重复代码 */
  private HttpHeaders createHeaders(
      String headerParam, String paramName, MediaType mediaType, MediaType acceptMediaType) {
    var headers = new HttpHeaders();
    headers.setContentType(mediaType);
    if (acceptMediaType != null) {
      headers.setAccept(Collections.singletonList(acceptMediaType));
    }
    if (StringUtils.isNotBlank(paramName)) {
      headers.set(paramName, headerParam);
    }
    return headers;
  }
}
