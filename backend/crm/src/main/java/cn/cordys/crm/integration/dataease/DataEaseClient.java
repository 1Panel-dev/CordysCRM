package cn.cordys.crm.integration.dataease;


import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.integration.common.dto.ThirdConfigurationDTO;
import cn.cordys.crm.integration.dataease.dto.*;
import cn.cordys.crm.integration.dataease.dto.request.*;
import cn.cordys.crm.integration.dataease.dto.response.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
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

public class DataEaseClient {

    protected static RestTemplate restTemplate;

    static {
        restTemplate = new RestTemplate();
    }

    protected String endpoint;
    protected String prefix = "/de2api/";
    private String accessKey;
    private String secretKey;

    public DataEaseClient(ThirdConfigurationDTO thirdConfiguration) {
        this.accessKey = thirdConfiguration.getDeAccessKey();
        this.secretKey = thirdConfiguration.getDeSecretKey();
        this.endpoint = thirdConfiguration.getRedirectUrl();
        if (this.endpoint != null && this.endpoint.endsWith("/")) {
            this.endpoint = this.endpoint.substring(0, this.endpoint.length() - 1);
        }
    }

    public boolean validate() {
        try {
            get("user/personInfo");
        } catch (Exception e) {
            LogUtils.error(e);
            return false;
        }
        return true;
    }

    public List<SysVariableDTO> listSysVariable() {
        return post("sysVariable/query", SysVariableListResponse.class).getData();
    }

    public boolean validateEmbeddedToken(String token) {
        try {
            String url = getUrl("sysParameter/shareBase");
            HttpHeaders headers = new HttpHeaders();
            headers.add("x-embedded-token", token);
            HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
            new RestTemplate().exchange(url, HttpMethod.GET, httpEntity, String.class);
        } catch (Exception e) {
            LogUtils.error(e);
            return false;
        }
        return true;
    }

    public List<SysVariableValueDTO> listSysVariableValue(String sysVariableId) {
        return post("sysVariable/value/selected/1/10000", Map.of("sysVariableId", sysVariableId), SysVariableValueListResponse.class).getData()
                .getRecords();
    }

    public SysVariableDTO editSysVariable(SysVariableUpdateRequest request) {
        return post("sysVariable/edit", request, SysVariableCreateResponse.class).getData();
    }

    public void deleteSysVariable(String id) {
        get("sysVariable/delete/{id}", id);
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

    public void roleMountUser(RoleMountUserRequest request) {
        post("role/mountUser", request);
    }

    public List<RoleDTO> listRole() {
        return post("role/query", RoleListResponse.class).getData();
    }

    public void editRole(RoleUpdateRequest request) {
        post("role/edit", request);
    }

    public void deleteRole(String id) {
        post("role/delete/{id}", Map.of(), DataEaseResponse.class, id);
    }

    public SysVariableValueDTO createSysVariableValue(SysVariableValueCreateRequest request) {
        return post("sysVariable/value/create", request, SysVariableValueCreateResponse.class).getData();
    }

    public void batchDelSysVariableValue(List<String> valueIds) {
        post("sysVariable/value/batchDel", valueIds);
    }

    public PageDTO<UserPageDTO> listUserPage(Integer pageNum, Integer pageSize) {
        return post("user/pager/{pageNum}/{pageSize}", UserListResponse.class, pageNum.toString(), pageSize.toString()).getData();
    }

    public void createUser(UserCreateRequest request) {
        post("user/create", request, DataEaseBaseResponse.class);
    }

    public DataEaseBaseResponse post(String path, Object body, String... uriVariables) {
        return post(path, body, DataEaseBaseResponse.class, uriVariables);
    }

    public DataEaseBaseResponse post(String path, String... uriVariables) {
        return post(path, Map.of(), DataEaseBaseResponse.class, uriVariables);
    }

    public <V extends DataEaseBaseResponse> V post(String path, Class<V> responseClass, String... uriVariables) {
        return post(path, Map.of(), responseClass, uriVariables);
    }

    public <V extends DataEaseBaseResponse> V post(String path, Object body, Class<V> responseClass, String... uriVariables) {
        String url = getUrl(path);
        V dataEaseResponse = restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(body), responseClass, uriVariables)
                .getBody();
        if (dataEaseResponse.getCode() != 0) {
            throw new GenericException(dataEaseResponse.getMsg());
        }
        return dataEaseResponse;
    }

    public DataEaseBaseResponse get(String path, Object... uriVariables) {
        return get(path, DataEaseBaseResponse.class, uriVariables);
    }

    public <V extends DataEaseBaseResponse> V get(String path, Class<V> responseClass, Object... uriVariables) {
        String url = getUrl(path);
        V dataEaseResponse = restTemplate.exchange(url, HttpMethod.GET, getHttpEntity(), responseClass, uriVariables)
                .getBody();
        if (dataEaseResponse.getCode() != 0) {
            throw new GenericException(dataEaseResponse.getMsg());
        }
        return dataEaseResponse;
    }

    public void editUser(UserUpdateRequest request) {
        post("user/edit", request);
    }

    public void deleteUser(String id) {
        post("user/delete/{id}", id);
    }

    private String getUrl(String path) {
        return this.endpoint + prefix + path;
    }

    private String getSignature() {
        return aesDecrypt(accessKey + "|" + UUID.randomUUID() + "|" + System.currentTimeMillis(),
                secretKey, accessKey);
    }

    protected HttpEntity<MultiValueMap> getHttpEntity() {
        return new HttpEntity<>(getHeader());
    }

    protected HttpEntity<Object> getHttpEntity(Object object) {
        return new HttpEntity<>(object, getHeader());
    }

    protected HttpEntity<Object> getHttpEntity(Object object, MultiValueMap<String, String> headers) {
        return new HttpEntity<>(object, headers);
    }

    protected HttpHeaders getHeader() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.set("accessKey", accessKey);
        String signature = getSignature();
        httpHeaders.set("signature", signature);
        httpHeaders.set("x-de-ask-token", getJWTToken(signature));
        return httpHeaders;
    }

    private String getJWTToken(String signature) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        JWTCreator.Builder builder = JWT.create();
        builder.withClaim("accessKey", accessKey).withClaim("signature", signature);
        return builder.sign(algorithm);
    }

    public String aesDecrypt(String src, String secretKey, String iv) {
        if (StringUtils.isBlank(src) || StringUtils.isBlank(secretKey)) {
            throw new IllegalArgumentException("Input or secretKey cannot be null or empty");
        }

        try {
            byte[] raw = secretKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec iv1 = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv1);
            byte[] encrypted = cipher.doFinal(src.getBytes("UTF-8"));
            return Base64.encodeBase64String(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM decrypt error:", e);
        }
    }

    public List<OptionDTO> listOrg() {
        return post("org/page/lazyTree", OrgListResponse.class).getData()
                .getNodes();
    }
}
