package cn.cordys.crm.integration.dataease;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.crm.integration.common.request.DeThirdConfigRequest;
import cn.cordys.crm.integration.dataease.dto.*;
import cn.cordys.crm.integration.dataease.dto.request.*;
import cn.cordys.crm.integration.dataease.dto.response.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class DataEaseClient {

    protected static final RestTemplate restTemplate = new RestTemplate();

    private final String accessKey;
    private final String secretKey;
    protected String endpoint;
    protected String prefix = "/de2api/";

    public DataEaseClient(DeThirdConfigRequest cfg) {
        this.accessKey = cfg.getDeAccessKey();
        this.secretKey = cfg.getDeSecretKey();
        this.endpoint = trimTrailingSlash(cfg.getRedirectUrl());
    }

    private String trimTrailingSlash(String url) {
        return (url != null && url.endsWith("/"))
                ? url.substring(0, url.length() - 1)
                : url;
    }

    public boolean validate() {
        try {
            get("user/personInfo");
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public List<SysVariableDTO> listSysVariable() {
        return post("sysVariable/query", SysVariableListResponse.class).getData();
    }

    public List<SysVariableValueDTO> listSysVariableValue(String sysVariableId) {
        return post("sysVariable/value/selected/1/10000",
                Map.of("sysVariableId", sysVariableId),
                SysVariableValueListResponse.class)
                .getData()
                .getRecords();
    }

    public SysVariableDTO createSysVariable(SysVariableCreateRequest request) {
        return post("sysVariable/create", request, SysVariableCreateResponse.class).getData();
    }

    public Long createRole(RoleCreateRequest request) {
        return (Long) post("role/create", request, DataEaseResponse.class).getData();
    }

    public void switchOrg(String orgId) {
        post("user/switch/{orgId}", orgId);
    }

    public List<RoleDTO> listRole() {
        return post("role/query", RoleListResponse.class).getData();
    }

    public SysVariableValueDTO createSysVariableValue(SysVariableValueCreateRequest request) {
        return post("sysVariable/value/create",
                request,
                SysVariableValueCreateResponse.class).getData();
    }

    public void batchDelSysVariableValue(List<String> valueIds) {
        post("sysVariable/value/batchDel", valueIds);
    }

    public PageDTO<UserPageDTO> listUserPage(Integer pageNum, Integer pageSize) {
        return post("user/pager/{pageNum}/{pageSize}",
                UserListResponse.class,
                pageNum.toString(),
                pageSize.toString()).getData();
    }

    public void createUser(UserCreateRequest request) {
        post("user/create", request, DataEaseBaseResponse.class);
    }

    // ---------- post wrappers ----------

    public DataEaseBaseResponse post(String path, Object body, String... uriVariables) {
        return post(path, body, DataEaseBaseResponse.class, uriVariables);
    }

    public DataEaseBaseResponse post(String path, String... uriVariables) {
        return post(path, Map.of(), DataEaseBaseResponse.class, uriVariables);
    }

    public <V extends DataEaseBaseResponse> V post(String path, Class<V> responseClass, String... uriVars) {
        return post(path, Map.of(), responseClass, uriVars);
    }

    public <V extends DataEaseBaseResponse> V post(String path, Object body, Class<V> responseClass, String... uriVars) {
        var url = getUrl(path);
        var response = restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(body), responseClass, (Object[]) uriVars)
                .getBody();
        validateResponse(response);
        return response;
    }

    // ---------- get wrappers ----------

    public DataEaseBaseResponse get(String path, Object... uriVars) {
        return get(path, DataEaseBaseResponse.class, uriVars);
    }

    public <V extends DataEaseBaseResponse> V get(String path, Class<V> responseClass, Object... uriVars) {
        var url = getUrl(path);
        var response = restTemplate.exchange(url, HttpMethod.GET, getHttpEntity(), responseClass, uriVars)
                .getBody();
        validateResponse(response);
        return response;
    }

    private <T extends DataEaseBaseResponse> void validateResponse(T resp) {
        if (resp != null && resp.getCode() != 0) {
            throw new GenericException(resp.getMsg());
        }
    }

    public void editUser(UserUpdateRequest request) {
        post("user/edit", request);
    }

    public void deleteUser(String id) {
        post("user/delete/{id}", id);
    }

    private String getUrl(String path) {
        return endpoint + prefix + path;
    }

    private String getSignature() {
        return aesEncrypt(accessKey + "|" + UUID.randomUUID() + "|" + System.currentTimeMillis(),
                secretKey, accessKey);
    }

    protected HttpEntity<MultiValueMap<String, Object>> getHttpEntity() {
        return new HttpEntity<>(getHeader());
    }

    protected HttpEntity<Object> getHttpEntity(Object obj) {
        return new HttpEntity<>(obj, getHeader());
    }

    protected HttpHeaders getHeader() {
        var headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accessKey", accessKey);

        var sig = getSignature();
        headers.set("signature", sig);
        headers.set("x-de-ask-token", getJWTToken(sig));
        return headers;
    }

    private String getJWTToken(String signature) {
        var algorithm = Algorithm.HMAC256(secretKey);
        JWTCreator.Builder builder = JWT.create();
        builder.withClaim("accessKey", accessKey)
                .withClaim("signature", signature);
        return builder.sign(algorithm);
    }

    public String aesEncrypt(String src, String key, String iv) {
        if (StringUtils.isBlank(src) || StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("Input or secretKey cannot be null or empty");
        }
        try {
            var raw = key.getBytes(StandardCharsets.UTF_8);
            var secretKeySpec = new SecretKeySpec(raw, "AES");
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var ivSpec = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);
            return Base64.encodeBase64String(cipher.doFinal(src.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("AES encrypt error:", e);
        }
    }

    public List<OptionDTO> listOrg() {
        return post("org/page/lazyTree", OrgListResponse.class)
                .getData()
                .getNodes();
    }
}
