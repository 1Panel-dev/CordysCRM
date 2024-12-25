package io.cordys.common.security;

import io.cordys.common.util.CodingUtils;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.crm.system.domain.UserKey;
import io.cordys.crm.system.service.UserKeyService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 处理 API 密钥验证的工具类，包括获取用户、验证请求是否包含 API 密钥以及验证签名的功能。
 */
public class ApiKeyHandler {

    public static final String API_ACCESS_KEY = "accessKey"; // API 密钥字段
    public static final String API_SIGNATURE = "signature";  // API 签名字段

    /**
     * 根据请求中的 API 密钥和签名获取用户 ID。
     *
     * @param request HTTP 请求
     * @return 用户 ID，如果请求无效则返回 null
     */
    public static String getUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return getUser(request.getHeader(API_ACCESS_KEY), request.getHeader(API_SIGNATURE));
    }

    /**
     * 判断请求是否包含有效的 API 密钥和签名。
     *
     * @param request HTTP 请求
     * @return 如果请求包含有效的 API 密钥和签名，返回 true；否则返回 false
     */
    public static Boolean isApiKeyCall(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        return !StringUtils.isBlank(request.getHeader(API_ACCESS_KEY)) && !StringUtils.isBlank(request.getHeader(API_SIGNATURE));
    }

    /**
     * 根据提供的 accessKey 和 signature 验证用户，并返回用户 ID。
     * 如果验证失败，则抛出相应的异常。
     *
     * @param accessKey API 密钥
     * @param signature API 签名
     * @return 用户 ID
     * @throws RuntimeException 如果验证失败，会抛出相应的异常
     */
    public static String getUser(String accessKey, String signature) {
        if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(signature)) {
            return null;
        }

        // 获取用户密钥信息
        UserKey userKey = Objects.requireNonNull(CommonBeanFactory.getBean(UserKeyService.class)).getUserKey(accessKey);
        if (userKey == null) {
            throw new RuntimeException("invalid accessKey");
        }

        // 检查 accessKey 是否启用
        if (BooleanUtils.isFalse(userKey.getEnable())) {
            throw new RuntimeException("accessKey is disabled");
        }

        // 检查 accessKey 是否过期
        if (BooleanUtils.isFalse(userKey.getForever())) {
            if (userKey.getExpireTime() == null || userKey.getExpireTime() < System.currentTimeMillis()) {
                throw new RuntimeException("accessKey is expired");
            }
        }

        // 解密签名并验证
        String signatureDecrypt;
        try {
            signatureDecrypt = CodingUtils.aesDecrypt(signature, userKey.getSecretKey(), accessKey.getBytes());
        } catch (Throwable t) {
            throw new RuntimeException("invalid signature", t);
        }

        String[] signatureArray = StringUtils.split(StringUtils.trimToNull(signatureDecrypt), "|");
        if (signatureArray.length < 2) {
            throw new RuntimeException("invalid signature");
        }

        // 验证签名中的 accessKey 是否匹配
        if (!StringUtils.equals(accessKey, signatureArray[0])) {
            throw new RuntimeException("invalid signature");
        }

        long signatureTime;
        try {
            signatureTime = Long.parseLong(signatureArray[signatureArray.length - 1]);
        } catch (Exception e) {
            throw new RuntimeException("invalid signature time", e);
        }

        // 验证签名是否超时（30分钟）
        if (Math.abs(System.currentTimeMillis() - signatureTime) > 1800000) {
            throw new RuntimeException("expired signature");
        }

        // 返回用户创建者 ID
        return userKey.getCreateUser();
    }
}
