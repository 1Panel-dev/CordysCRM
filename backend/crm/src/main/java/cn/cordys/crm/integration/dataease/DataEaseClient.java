package cn.cordys.crm.integration.dataease;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.integration.common.dto.ThirdConfigurationDTO;
import cn.cordys.crm.integration.dataease.dto.*;
import cn.cordys.crm.integration.dataease.dto.request.*;
import cn.cordys.crm.integration.dataease.dto.response.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataEaseClient {

    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

    private static final String HEADER_ACCESS_KEY = "accessKey";
    private static final String HEADER_SIGNATURE = "signature";
    private static final String HEADER_TOKEN = "x-de-ask-token";

    private final String accessKey;
    private final String secretKey;
    private final String endpoint;

    public DataEaseClient(ThirdConfigurationDTO cfg) {
        this.accessKey = cfg.getDeAccessKey();
        this.secretKey = cfg.getDeSecretKey();
        this.endpoint = trimEnd(cfg.getRedirectUrl());
    }

    /**
     * 去掉 URL 尾部 "/"
     */
    private String trimEnd(String url) {
        if (url == null || !url.endsWith("/")) return url;
        return url.substring(0, url.length() - 1);
    }

    /**
     * ========== 公共方法区域 ==========
     */

    public boolean validate() {
        try {
            get("user/personInfo");
            return true;
        } catch (Exception e) {
            LogUtils.error(e);
            return false;
        }
    }

    /**
     * ========== 批量封装的方法 ==========
     */

    public <T extends DataEaseBaseResponse> T post(String path, Object body, Class<T> cls, Object... vars) {
        return exchange(path, HttpMethod.POST, body, cls, vars);
    }

    public DataEaseBaseResponse post(String path, Object body, Object... vars) {
        return post(path, body, DataEaseBaseResponse.class, vars);
    }

    public DataEaseBaseResponse post(String path, String... vars) {
        return post(path, Collections.emptyMap(), (Object) vars);
    }

    public <T extends DataEaseBaseResponse> T post(String path, Class<T> cls, Object... vars) {
        return post(path, Collections.emptyMap(), cls, vars);
    }

    public DataEaseBaseResponse get(String path, Object... vars) {
        return get(path, DataEaseBaseResponse.class, vars);
    }

    public <T extends DataEaseBaseResponse> T get(String path, Class<T> cls, Object... vars) {
        return exchange(path, HttpMethod.GET, null, cls, vars);
    }

    private <T extends DataEaseBaseResponse> T exchange(
            String path,
            HttpMethod method,
            Object body,
            Class<T> cls,
            Object... vars
    ) {
        String url = getUrl(path);

        HttpEntity<?> entity = (body == null)
                ? new HttpEntity<>(buildHeaders())
                : new HttpEntity<>(body, buildHeaders());

        T result = REST_TEMPLATE.exchange(url, method, entity, cls, vars).getBody();

        if (result != null && result.getCode() != 0) {
            throw new GenericException(result.getMsg());
        }
        return result;
    }

    /**
     * ========== 业务方法 ==========
     */

    public List<SysVariableDTO> listSysVariable() {
        return post("sysVariable/query", SysVariableListResponse.class).getData();
    }

    public List<SysVariableValueDTO> listSysVariableValue(String sysVariableId) {
        return post(
                "sysVariable/value/selected/1/10000",
                Map.of("sysVariableId", sysVariableId),
                SysVariableValueListResponse.class
        ).getData().getRecords();
    }

    public SysVariableDTO createSysVariable(SysVariableCreateRequest req) {
        return post("sysVariable/create", req, SysVariableCreateResponse.class).getData();
    }

    public Long createRole(RoleCreateRequest req) {
        return (Long) post("role/create", req, DataEaseResponse.class).getData();
    }

    public void switchOrg(String orgId) {
        post("user/switch/{orgId}", orgId);
    }

    public void roleMountUser(RoleMountUserRequest req) {
        post("role/mountUser", req);
    }

    public List<RoleDTO> listRole() {
        return post("role/query", RoleListResponse.class).getData();
    }

    public SysVariableValueDTO createSysVariableValue(SysVariableValueCreateRequest req) {
        return post("sysVariable/value/create", req, SysVariableValueCreateResponse.class).getData();
    }

    public void batchDelSysVariableValue(List<String> ids) {
        post("sysVariable/value/batchDel", ids);
    }

    public PageDTO<UserPageDTO> listUserPage(Integer pageNum, Integer pageSize) {
        return post("user/pager/{pageNum}/{pageSize}", UserListResponse.class,
                pageNum.toString(), pageSize.toString()
        ).getData();
    }

    public void createUser(UserCreateRequest req) {
        post("user/create", req, DataEaseBaseResponse.class);
    }

    public void editUser(UserUpdateRequest req) {
        post("user/edit", req);
    }

    public void deleteUser(String id) {
        post("user/delete/{id}", id);
    }

    public List<OptionDTO> listOrg() {
        return post("org/page/lazyTree", OrgListResponse.class).getData().getNodes();
    }

    /**
     * ========== 私有方法 ==========
     */

    private String getUrl(String path) {
        String prefix = "/de2api/";
        return endpoint + prefix + path;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set(HEADER_ACCESS_KEY, accessKey);

        String signature = generateSignature();
        headers.set(HEADER_SIGNATURE, signature);
        headers.set(HEADER_TOKEN, generateJWT(signature));

        return headers;
    }

    private String generateSignature() {
        return aesEncrypt(
                accessKey + "|" + UUID.randomUUID() + "|" + System.currentTimeMillis(),
                secretKey,
                accessKey
        );
    }

    private String generateJWT(String signature) {
        return JWT.create()
                .withClaim("accessKey", accessKey)
                .withClaim("signature", signature)
                .sign(Algorithm.HMAC256(secretKey));
    }

    /**
     * AES 加密（原命名 aesDecrypt 实际逻辑为加密 → 仅重命名）
     */
    private String aesEncrypt(String src, String key, String iv) {
        if (StringUtils.isBlank(src) || StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("Input or secretKey cannot be empty");
        }

        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    keySpec,
                    new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8))
            );

            return Base64.encodeBase64String(cipher.doFinal(src.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("AES encrypt error:", e);
        }
    }
}
