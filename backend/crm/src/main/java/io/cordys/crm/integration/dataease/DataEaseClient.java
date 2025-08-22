package io.cordys.crm.integration.dataease;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.integration.auth.dto.ThirdConfigurationDTO;
import io.cordys.crm.integration.dataease.dto.*;
import io.cordys.crm.integration.dataease.dto.request.*;
import io.cordys.crm.integration.dataease.dto.response.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataEaseClient {

    protected static RestTemplate restTemplate;
    private String accessKey;
    private String secretKey;
    protected String endpoint;
    protected String prefix = "/de2api/";

    static {
        restTemplate = new RestTemplate();
    }

    public DataEaseClient(ThirdConfigurationDTO thirdConfiguration) {
        this.accessKey = thirdConfiguration.getDeAccessKey();
        this.secretKey = thirdConfiguration.getDeSecretKey();
        this.endpoint = thirdConfiguration.getRedirectUrl();
        if (this.endpoint.endsWith("/")) {
            this.endpoint = this.endpoint.substring(0, this.endpoint.length() - 1);
        }
    }

    public boolean validate() {
        String url = getUrl("user/personInfo");
        try {
            restTemplate.exchange(url,
                    HttpMethod.GET, getHttpEntity(Map.of()), String.class);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public List<SysVariableDTO> listSysVariable() {
        String url = getUrl("sysVariable/query");
        ResponseEntity<SysVariableListResponse> response = restTemplate.exchange(url,
                HttpMethod.POST, getHttpEntity(Map.of()), SysVariableListResponse.class);
        return response.getBody().getData();
    }

    public List<SysVariableValueDTO> listSysVariableValue(String sysVariableId) {
        String url = getUrl("sysVariable/value/selected/1/10000");
        ResponseEntity<SysVariableValueListResponse> response = restTemplate.exchange(url,
                HttpMethod.POST, getHttpEntity(Map.of("sysVariableId", sysVariableId)), SysVariableValueListResponse.class);
        return response.getBody().getData();
    }

    public SysVariableDTO editSysVariable(SysVariableUpdateRequest request) {
        String url = getUrl("sysVariable/edit");
        return restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(request), SysVariableCreateResponse.class).getBody().getData();
    }

    public void deleteSysVariable(String id) {
        String url = getUrl("sysVariable/delete/{id}");
        restTemplate.exchange(url, HttpMethod.GET, getHttpEntity(), String.class, id);
    }

    public SysVariableDTO createSysVariable(SysVariableCreateRequest request) {
        String url = getUrl("sysVariable/create");
        return restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(request), SysVariableCreateResponse.class).getBody().getData();
    }

    public Long createRole(RoleCreateRequest request) {
        String url = getUrl("role/create");
        return (Long) restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(request), DataEaseResponse.class).getBody().getData();
    }

    public Long createOrg(OrgCreateRequest request) {
        String url = getUrl("org/page/create");
        return (Long) restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(request), DataEaseResponse.class).getBody().getData();
    }

    public void switchOrg(String orgId) {
        String url = getUrl("user/switch/{orgId}");
        restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(), String.class, orgId);
    }

    public void roleMountUser(RoleMountUserRequest request) {
        String url = getUrl("role/mountUser");
        restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(request), String.class);
    }

    public List<RoleDTO> listRole() {
        String url = getUrl("role/query");
        return restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(Map.of()), RoleListResponse.class).getBody().getData();
    }

    public void editRole(RoleUpdateRequest request) {
        String url = getUrl("role/edit");
        restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(request), String.class);
    }

    public void deleteRole(String id) {
        String url = getUrl("role/delete/{id}");
        restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(), String.class, id);
    }

    public SysVariableValueDTO createSysVariableValue(SysVariableValueCreateRequest request) {
        String url = getUrl("sysVariable/value/create");
        return restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(request), SysVariableValueCreateResponse.class).getBody().getData();
    }

    public void batchDelSysVariableValue(List<String> valueIds) {
        String url = getUrl("sysVariable/value/batchDel");
        restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(valueIds), String.class);
    }

    public PageDTO<UserPageDTO> listUserPage(int pageNum, int pageSize) {
        String url = getUrl("user/pager/{pageNum}/{pageSize}");
        return restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(Map.of()), UserListResponse.class, pageNum, pageSize)
                .getBody()
                .getData();
    }

    public void createUser(UserCreateRequest request) {
        String url = getUrl("user/create");
        restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(request), String.class);
    }

    public void batchImportUser(File file) {
        String url = getUrl("user/batchImport");

        FileSystemResource fileResource = new FileSystemResource(file);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpEntity<Object> httpEntity = getHttpEntity(body);
        httpEntity.getHeaders().setContentType(MediaType.MULTIPART_FORM_DATA);

        restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);
    }

    public void editUser(UserUpdateRequest request) {
        String url = getUrl("user/edit");
        restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(request), String.class);
    }

    public void deleteUser(String id) {
        String url = getUrl("user/delete/{id}");
        restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(), String.class, id);
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
        String url = getUrl("org/page/lazyTree");
        return restTemplate.exchange(url, HttpMethod.POST, getHttpEntity(Map.of()), OrgListResponse.class)
                .getBody()
                .getData()
                .getNodes();
    }
}
