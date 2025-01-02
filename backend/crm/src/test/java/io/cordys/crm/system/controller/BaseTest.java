package io.cordys.crm.system.controller;

import com.jayway.jsonpath.JsonPath;
import io.cordys.common.exception.IResultCode;
import io.cordys.common.pager.Pager;
import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.domain.User;
import io.cordys.mybatis.BaseMapper;
import io.cordys.security.SessionConstants;
import jakarta.annotation.Resource;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class BaseTest {
    @Resource
    protected MockMvc mockMvc;
    protected static String sessionId;
    protected static String csrfToken;
    protected static AuthInfo adminAuthInfo;
    protected static Map<String, AuthInfo> permissionAuthInfoMap = new HashMap(3);
    @Resource
    private BaseMapper<User> userMapper;

    protected static final String DEFAULT_USER_PASSWORD = "CordysCRM";


    /**
     * 可以重写该方法定义 BASE_PATH
     */
    protected String getBasePath() {
        return StringUtils.EMPTY;
    }

    @BeforeEach
    public void login() throws Exception {
        if (this.adminAuthInfo == null) {
            this.adminAuthInfo = initAuthInfo("admin", DEFAULT_USER_PASSWORD);
            this.sessionId = this.adminAuthInfo.getSessionId();
            this.csrfToken = this.adminAuthInfo.getCsrfToken();
        }


        if (permissionAuthInfoMap.isEmpty()) {
            // 获取系统，组织，项目对应的权限测试用户的认证信息
            List<String> permissionUserNames = Arrays.asList("SYSTEM",
                    "ORGANIZATION",
                    "PROJECT");
            for (String permissionUserName : permissionUserNames) {
                User permissionUser = userMapper.selectByPrimaryKey(permissionUserName);
                // 有对应用户才初始化认证信息
                if (permissionUser != null) {
                    permissionAuthInfoMap.put(permissionUserName, initAuthInfo(permissionUserName, DEFAULT_USER_PASSWORD));
                }
            }
        }
    }

    public void login(String user, String password) throws Exception {
        this.adminAuthInfo = initAuthInfo(user, password);
        this.sessionId = this.adminAuthInfo.getSessionId();
        this.csrfToken = this.adminAuthInfo.getCsrfToken();
    }

    private AuthInfo initAuthInfo(String username, String password) throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .content(String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String sessionId = JsonPath.read(mvcResult.getResponse().getContentAsString(), "$.data.sessionId");
        String csrfToken = JsonPath.read(mvcResult.getResponse().getContentAsString(), "$.data.csrfToken");
        return new AuthInfo(sessionId, csrfToken);
    }

    protected MockHttpServletRequestBuilder getPostRequestBuilder(String url, Object param, Object... uriVariables) {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post(getBasePath() + url, uriVariables);
        return setRequestBuilderHeader(requestBuilder, adminAuthInfo)
                .content(JSON.toJSONString(param))
                .contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder getRequestBuilder(String url, Object... uriVariables) {
        try {
            MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(getBasePath() + url, uriVariables);
            return setRequestBuilderHeader(requestBuilder, adminAuthInfo);
        } catch (Exception e) {
            LogUtils.error(e);
        }
        return null;
    }

    protected MockHttpServletRequestBuilder getRequestBuilderByRole(String url, String userRoleType, Object... uriVariables) {
        // 使用对应的用户认证信息来请求, 非Admin
        AuthInfo authInfo = permissionAuthInfoMap.get(userRoleType);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(getBasePath() + url, uriVariables);
        return setRequestBuilderHeader(requestBuilder, authInfo == null ? adminAuthInfo : authInfo);
    }

    protected ResultActions requestPost(String url, Object param, Object... uriVariables) throws Exception {
        return mockMvc.perform(getPostRequestBuilder(url, param, uriVariables))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    protected MvcResult requestGetDownloadFile(String url, MediaType contentType, Object... uriVariables) throws Exception {
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return mockMvc.perform(getRequestBuilder(url, uriVariables))
                .andExpect(content().contentType(contentType))
                .andExpect(status().isOk()).andReturn();
    }

    protected MvcResult requestPostDownloadFile(String url, MediaType contentType, Object param) throws Exception {
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return mockMvc.perform(getPostRequestBuilder(url, param))
                .andExpect(content().contentType(contentType))
                .andExpect(status().isOk()).andReturn();
    }


    protected MvcResult requestPostAndReturn(String url, Object param, Object... uriVariables) throws Exception {
        return this.requestPost(url, param, uriVariables).andReturn();
    }

    protected ResultActions requestGet(String url, Object... uriVariables) throws Exception {
        return mockMvc.perform(getRequestBuilder(url, uriVariables))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    protected MvcResult requestGetAndReturn(String url, Object... uriVariables) throws Exception {
        return this.requestGet(url, uriVariables).andReturn();
    }

    protected ResultActions requestGetWithOk(String url, Object... uriVariables) throws Exception {
        return mockMvc.perform(getRequestBuilder(url, uriVariables))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    protected MvcResult requestGetWithOkAndReturn(String url, Object... uriVariables) throws Exception {
        return this.requestGetWithOk(url, uriVariables).andReturn();
    }

    protected ResultActions requestPostWithOk(String url, Object param, Object... uriVariables) throws Exception {
        return mockMvc.perform(getPostRequestBuilder(url, param, uriVariables))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    protected MvcResult requestPostWithOkAndReturn(String url, Object param, Object... uriVariables) throws Exception {
        return this.requestPostWithOk(url, param, uriVariables).andReturn();
    }

    protected ResultActions requestMultipartWithOk(String url, MultiValueMap<String, Object> paramMap, Object... uriVariables) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = getMultipartRequestBuilder(url, paramMap, uriVariables);
        return mockMvc.perform(requestBuilder)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    protected ResultActions requestMultipart(String url, MultiValueMap<String, Object> paramMap, Object... uriVariables) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = getMultipartRequestBuilder(url, paramMap, uriVariables);
        return mockMvc.perform(requestBuilder)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    protected MvcResult requestMultipartWithOkAndReturn(String url, MultiValueMap<String, Object> paramMap, Object... uriVariables) throws Exception {
        return this.requestMultipartWithOk(url, paramMap, uriVariables).andReturn();
    }

    protected ResultActions requestUploadFile(String url, MockMultipartFile file, Object... uriVariables) throws Exception {
        return mockMvc.perform(getUploadRequestBuilder(url, file, uriVariables))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    protected ResultActions requestUploadFileWithOk(String url, MockMultipartFile file) throws Exception {
        return requestUploadFile(url, file)
                .andExpect(status().isOk());
    }

    protected MvcResult requestUploadFileWithOkAndReturn(String url, MockMultipartFile file) throws Exception {
        return requestUploadFileWithOk(url, file)
                .andReturn();
    }
    private MockHttpServletRequestBuilder getPermissionMultipartRequestBuilder(String roleId, String url,
                                                                               MultiValueMap<String, Object> paramMap,
                                                                               Object[] uriVariables) {
        AuthInfo authInfo = getPermissionAuthInfo(roleId);
        MockMultipartHttpServletRequestBuilder requestBuilder = getMultipartRequestBuilderWithParam(url, paramMap, uriVariables);
        return setRequestBuilderHeader(requestBuilder, authInfo);
    }

    private MockHttpServletRequestBuilder getPermissionUploadRequestBuilder(String roleId, String url,
                                                                            MockMultipartFile file,
                                                                            Object[] uriVariables) {
        AuthInfo authInfo = getPermissionAuthInfo(roleId);
        MockMultipartHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.multipart(getBasePath() + url, uriVariables).file(file);
        return setRequestBuilderHeader(requestBuilder, authInfo);
    }

    private MockHttpServletRequestBuilder getMultipartRequestBuilder(String url,
                                                                     MultiValueMap<String, Object> paramMap,
                                                                     Object[] uriVariables) {
        MockMultipartHttpServletRequestBuilder requestBuilder = getMultipartRequestBuilderWithParam(url, paramMap, uriVariables);
        return setRequestBuilderHeader(requestBuilder, adminAuthInfo)
                .header(SessionConstants.HEADER_TOKEN, adminAuthInfo.getSessionId())
                .header(SessionConstants.CSRF_TOKEN, adminAuthInfo.getCsrfToken());
    }

    private MockMultipartHttpServletRequestBuilder getUploadRequestBuilder(String url, MockMultipartFile file, Object[] uriVariables) {
        MockMultipartHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.multipart(getBasePath() + url, uriVariables).file(file);
        setRequestBuilderHeader(requestBuilder, adminAuthInfo);
        return requestBuilder;
    }

    /**
     * 构建 multipart 带参数的请求
     *
     * @param url
     * @param paramMap
     * @param uriVariables
     * @return
     */
    private MockMultipartHttpServletRequestBuilder getMultipartRequestBuilderWithParam(String url, MultiValueMap<String, Object> paramMap, Object[] uriVariables) {
        MockMultipartHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.multipart(getBasePath() + url, uriVariables);
        paramMap.forEach((key, value) -> {
            List list = value;
            for (Object o : list) {
                try {
                    if (o == null) {
                        continue;
                    }
                    MockMultipartFile multipartFile;
                    if (o instanceof List) {
                        List listObject = ((List) o);
                        if (CollectionUtils.isEmpty(listObject)) {
                            continue;
                        }
                        if (listObject.getFirst() instanceof File || listObject.getFirst() instanceof MockMultipartFile) {
                            // 参数是多个文件时,设置多个文件
                            for (Object subObject : ((List) o)) {
                                multipartFile = getMockMultipartFile(key, subObject);
                                requestBuilder.file(multipartFile);
                            }
                        } else {
                            multipartFile = getMockMultipartFile(key, o);
                            requestBuilder.file(multipartFile);
                        }
                    } else {
                        multipartFile = getMockMultipartFile(key, o);
                        requestBuilder.file(multipartFile);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        });
        return requestBuilder;
    }

    private static MockMultipartFile getMockMultipartFile(String key, Object value) throws IOException {
        MockMultipartFile multipartFile;
        if (value instanceof File) {
            File file = (File) value;
            multipartFile = new MockMultipartFile(key, file.getName(),
                    MediaType.APPLICATION_OCTET_STREAM_VALUE, Files.readAllBytes(file.toPath()));
        } else if (value instanceof MockMultipartFile) {
            multipartFile = (MockMultipartFile) value;
            // 有些地方的参数 name 写的是文件名，这里统一处理成参数名 key
            multipartFile = new MockMultipartFile(key, multipartFile.getOriginalFilename(),
                    MediaType.APPLICATION_OCTET_STREAM_VALUE, multipartFile.getBytes());
        } else {
            multipartFile = new MockMultipartFile(key, key,
                    MediaType.APPLICATION_JSON_VALUE, value.toString().getBytes());
        }
        return multipartFile;
    }

    /**
     * 获取默认的 MultiValue 参数
     *
     * @param param
     * @param file
     * @return
     */
    protected MultiValueMap<String, Object> getDefaultMultiPartParam(Object param, File file) {
        MultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<>();
        paramMap.add("file", file);
        paramMap.add("request", JSON.toJSONString(param));
        return paramMap;
    }

    /**
     * 获取默认的 MultiValue 参数
     * 这里参数位置与上面的方法对调
     * 以便于方法重载时，可以 files 可以传 null
     * 不会与上面方法混淆
     *
     * @param files
     * @param param
     * @return
     */
    protected MultiValueMap<String, Object> getDefaultMultiPartParam(List<File> files, Object param) {
        MultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<>();
        paramMap.add("files", files);
        paramMap.add("request", JSON.toJSONString(param));
        return paramMap;
    }

    protected <T> T getResultData(MvcResult mvcResult, Class<T> clazz) throws Exception {
        Object data = parseResponse(mvcResult).get("data");
        return JSON.parseObject(JSON.toJSONString(data), clazz);
    }

    protected <T> T getResultMessageDetail(MvcResult mvcResult, Class<T> clazz) throws Exception {
        Object data = parseResponse(mvcResult).get("messageDetail");
        return JSON.parseObject(JSON.toJSONString(data), clazz);
    }

    protected <T> List<T> getResultDataArray(MvcResult mvcResult, Class<T> clazz) throws Exception {
        Object data = parseResponse(mvcResult).get("data");
        return JSON.parseArray(JSON.toJSONString(data), clazz);
    }

    protected static Map parseResponse(MvcResult mvcResult) throws UnsupportedEncodingException {
        return JSON.parseMap(mvcResult.getResponse().getContentAsString(Charset.defaultCharset()));
    }

    /**
     * 校验错误响应码
     */
    protected void assertErrorCode(ResultActions resultActions, IResultCode resultCode) throws Exception {
        resultActions
                .andExpect(
                        jsonPath("$.code")
                                .value(resultCode.getCode())
                );
    }

    protected <T> Pager<List<T>> getPageResult(MvcResult mvcResult, Class<T> clazz) throws Exception {
        Map<String, Object> pagerResult = (Map<String, Object>) parseResponse(mvcResult).get("data");
        List<T> list = JSON.parseArray(JSON.toJSONString(pagerResult.get("list")), clazz);
        Pager pager = new Pager();
        pager.setPageSize(Long.valueOf(pagerResult.get("pageSize").toString()));
        pager.setCurrent(Long.valueOf(pagerResult.get("current").toString()));
        pager.setTotal(Long.valueOf(pagerResult.get("total").toString()));
        pager.setList(list);
        return pager;
    }








    /**
     * 调用 is-login 接口刷新权限
     *
     * @param roleId
     * @throws Exception
     */
    private void refreshUserPermission(String roleId) throws Exception {
        AuthInfo authInfo = getPermissionAuthInfo(roleId);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/is-login");
        requestBuilder = setRequestBuilderHeader(requestBuilder, authInfo);
        mockMvc.perform(requestBuilder);
    }

    private void refreshUserPermissionByRoleId(String roleId) throws Exception {
        AuthInfo authInfo = getPermissionAuthInfo(roleId);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/is-login");
        requestBuilder = setRequestBuilderHeader(requestBuilder, authInfo);
        mockMvc.perform(requestBuilder);
    }



    protected ResultActions requestGetWithNoAdmin(String url, String userRoleType, Object... uriVariables) throws Exception {
        return mockMvc.perform(getRequestBuilderByRole(url, userRoleType, uriVariables))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }


    private MockHttpServletRequestBuilder getPermissionPostRequestBuilder(String roleId, String url, Object param, Object... uriVariables) {
        try {
            AuthInfo authInfo = getPermissionAuthInfo(roleId);
            MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post(getBasePath() + url, uriVariables);
            return setRequestBuilderHeader(requestBuilder, authInfo)
                    .content(JSON.toJSONString(param))
                    .contentType(MediaType.APPLICATION_JSON);
        } catch (Exception e) {
            LogUtils.error(e);
        }
        return null;
    }

    private MockHttpServletRequestBuilder setRequestBuilderHeader(MockHttpServletRequestBuilder requestBuilder, AuthInfo authInfo) {
        return requestBuilder
                .header(SessionConstants.HEADER_TOKEN, authInfo.getSessionId())
                .header(SessionConstants.CSRF_TOKEN, authInfo.getCsrfToken())
                .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN");
    }

    private AuthInfo getPermissionAuthInfo(String roleId) {
        AuthInfo authInfo = permissionAuthInfoMap.get(roleId);
        if (authInfo == null) {
            throw new RuntimeException("没有初始化权限认证用户信息!");
        }
        return authInfo;
    }

    private MockHttpServletRequestBuilder getPermissionRequestBuilder(String roleId, String url, Object... uriVariables) {
        AuthInfo authInfo = getPermissionAuthInfo(roleId);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(getBasePath() + url, uriVariables);
        return setRequestBuilderHeader(requestBuilder, authInfo);
    }

    public String getSessionId() {
        return adminAuthInfo.getSessionId();
    }

    public String getCsrfToken() {
        return adminAuthInfo.getCsrfToken();
    }

    @Data
    class AuthInfo {
        private String sessionId;
        private String csrfToken;

        public AuthInfo(String sessionId, String csrfToken) {
            this.sessionId = sessionId;
            this.csrfToken = csrfToken;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getCsrfToken() {
            return csrfToken;
        }
    }

}
