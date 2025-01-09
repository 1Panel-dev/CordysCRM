package io.cordys.crm.base;

import com.jayway.jsonpath.JsonPath;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.Pager;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
import io.cordys.crm.system.domain.OrganizationUser;
import io.cordys.crm.system.domain.RolePermission;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class BaseTest {
    protected static AuthInfo adminAuthInfo;
    protected static AuthInfo permissionAuthInfo;
    protected static final String DEFAULT_USER_PASSWORD = "CordysCRM";
    protected static final String DEFAULT_PLATFORM = "mobile";
    protected static final String DEFAULT_PAGE = "page";
    protected static final String DEFAULT_LIST = "list";
    protected static final String DEFAULT_GET = "get/{0}";
    protected static final String DEFAULT_ADD = "add";
    protected static final String DEFAULT_UPDATE = "update";
    protected static final String DEFAULT_DELETE = "delete/{0}";
    public static final String DEFAULT_ORGANIZATION_ID = "100001";
    public static String PERMISSION_USER_NAME = "permission_test";


    @Resource
    protected MockMvc mockMvc;
    @Resource
    private BaseMapper<RolePermission> rolePermissionMapper;
    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;
    @Resource
    private BaseMapper<User> userMapper;

    protected String getBasePath() {
        return StringUtils.EMPTY;
    }

    @BeforeEach
    public void login() throws Exception {
        if (adminAuthInfo == null) {
            this.adminAuthInfo = initAuthInfo(InternalUser.ADMIN.getValue(), DEFAULT_USER_PASSWORD);
        }

        User permissionUser = userMapper.selectByPrimaryKey(PERMISSION_USER_NAME);
        // 有对应用户才初始化认证信息
        if (permissionUser != null && permissionAuthInfo == null) {
            this.permissionAuthInfo = initAuthInfo(PERMISSION_USER_NAME, DEFAULT_USER_PASSWORD);
        }
    }

    private AuthInfo initAuthInfo(String username, String password) throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .content(String.format("{\"username\":\"%s\",\"password\":\"%s\",\"platform\":\"%s\"}", username, password, DEFAULT_PLATFORM))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String sessionId = JsonPath.read(mvcResult.getResponse().getContentAsString(), "$.data.sessionId");
        String csrfToken = JsonPath.read(mvcResult.getResponse().getContentAsString(), "$.data.csrfToken");
        return new AuthInfo(sessionId, csrfToken);
    }

    protected MockHttpServletRequestBuilder getPostRequestBuilder(String url, Object param, Object... uriVariables) {
        return setRequestBuilderHeader(MockMvcRequestBuilders.post(getBasePath() + url, uriVariables), adminAuthInfo)
                .content(JSON.toJSONString(param))
                .contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder setRequestBuilderHeader(MockHttpServletRequestBuilder requestBuilder, AuthInfo authInfo) {
        return requestBuilder
                .header(SessionConstants.HEADER_TOKEN, authInfo.getSessionId())
                .header(SessionConstants.CSRF_TOKEN, authInfo.getCsrfToken())
                .header("Organization-Id", DEFAULT_ORGANIZATION_ID) //TODO 暂时加上默认的组织ID
                .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN");
    }

    protected ResultActions requestPost(String url, Object param, Object... uriVariables) throws Exception {
        return mockMvc.perform(getPostRequestBuilder(url, param, uriVariables))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    protected MvcResult requestPostWithOkAndReturn(String url, Object param, Object... uriVariables) throws Exception {
        return this.requestPostWithOk(url, param, uriVariables).andReturn();
    }

    protected ResultActions requestPostWithOk(String url, Object param, Object... uriVariables) throws Exception {
        return mockMvc.perform(getPostRequestBuilder(url, param, uriVariables))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
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

    protected ResultActions requestGetWithOk(String url, Object... uriVariables) throws Exception {
        return mockMvc.perform(getRequestBuilder(url, uriVariables))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    protected MvcResult requestGetWithOkAndReturn(String url, Object... uriVariables) throws Exception {
        return this.requestGetWithOk(url, uriVariables).andReturn();
    }

    protected static Map<?, ?> parseResponse(MvcResult mvcResult) throws UnsupportedEncodingException {
        return JSON.parseMap(mvcResult.getResponse().getContentAsString(Charset.defaultCharset()));
    }

    protected <T> List<T> getResultDataArray(MvcResult mvcResult, Class<T> clazz) throws Exception {
        Object data = parseResponse(mvcResult).get("data");
        return JSON.parseArray(JSON.toJSONString(data), clazz);
    }

    protected <T> T getResultData(MvcResult mvcResult, Class<T> clazz) throws Exception {
        Object data = parseResponse(mvcResult).get("data");
        return JSON.parseObject(JSON.toJSONString(data), clazz);
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

    protected ResultActions requestGet(String url, Object... uriVariables) throws Exception {
        return mockMvc.perform(getRequestBuilder(url, uriVariables))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    protected void requestPostPermissionTest(String permissionId, String url, Object param, Object... uriVariables) throws Exception {
        requestPermissionTest(permissionId, url, () -> getPermissionPostRequestBuilder(url, param, uriVariables));
    }

    protected void requestGetPermissionTest(String permissionId, String url, Object... uriVariables) throws Exception {
        requestPermissionTest(permissionId, url, () -> getPermissionRequestBuilder(url, uriVariables));
    }

    protected void requestPostPermissionsTest(List<String> permissionIds, String url, Object param, Object... uriVariables) throws Exception {
        requestPermissionsTest(permissionIds, url, () -> getPermissionPostRequestBuilder(url, param, uriVariables), mockMvc);
    }

    protected void requestGetPermissionsTest(List<String> permissionIds, String url, Object... uriVariables) throws Exception {
        requestPermissionsTest(permissionIds, url, () -> getPermissionRequestBuilder(url, uriVariables), mockMvc);
    }

    private MockHttpServletRequestBuilder getPermissionRequestBuilder(String url, Object... uriVariables) {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(getBasePath() + url, uriVariables);
        return setRequestBuilderHeader(requestBuilder, permissionAuthInfo);
    }

    protected MockHttpServletRequestBuilder getPermissionPostRequestBuilder(String url, Object param, Object... uriVariables) {
        return setRequestBuilderHeader(MockMvcRequestBuilders.post(getBasePath() + url, uriVariables), permissionAuthInfo)
                .content(JSON.toJSONString(param))
                .contentType(MediaType.APPLICATION_JSON);
    }

    /**
     * 校验多个权限(同级别权限: 列如都是SYSTEM)
     *
     * @param permissionIds         多个权限
     * @param url                   请求url
     * @param requestBuilderGetFunc 请求构造器
     * @throws Exception 请求抛出异常
     */
    public void requestPermissionsTest(List<String> permissionIds, String url, Supplier<MockHttpServletRequestBuilder> requestBuilderGetFunc, MockMvc mockMvc) throws Exception {
        for (String permissionId : permissionIds) {
            initUserRolePermission(permissionId);
        }

        // 刷新用户
        refreshUserPermission();

        int status = mockMvc.perform(requestBuilderGetFunc.get())
                .andReturn()
                .getResponse()
                .getStatus();

        // 校验是否有权限
        if (status == HttpStatus.FORBIDDEN.value()) {
            throw new GenericException(String.format("接口 %s 权限校验失败 %s", url, permissionIds));
        }

        // 删除权限
        RolePermission example = new RolePermission();
        example.setRoleId(PERMISSION_USER_NAME);
        rolePermissionMapper.delete(example);

        // 删除后刷新下权限
        refreshUserPermission();

        // 删除权限后，调用接口，校验是否没有权限
        status = mockMvc.perform(requestBuilderGetFunc.get())
                .andReturn()
                .getResponse()
                .getStatus();

        if (status != HttpStatus.FORBIDDEN.value()) {
            throw new GenericException(String.format("接口 %s 没有设置权限 %s", url, permissionIds));
        }
    }

    /**
     * 校验权限
     * 实现步骤
     * 1. 在 application.properties 配置权限的初始化 sql
     *      spring.sql.init.mode=always
     *      spring.sql.init.schema-locations=classpath*:dml/init_permission_test.sql
     * 2. 在 init_permission_test.sql 中配置权限，
     * 3. 向该用户组中添加权限测试是否生效，删除权限测试是否可以访问
     *
     * @param permissionId
     * @param url
     * @param requestBuilderGetFunc 请求构造器，一个 builder 只能使用一次，需要重新生成
     * @throws Exception
     */
    public void requestPermissionTest(String permissionId, String url, Supplier<MockHttpServletRequestBuilder> requestBuilderGetFunc) throws Exception {
        // 先给初始化的用户组添加权限
        RolePermission userRolePermission = initUserRolePermission(permissionId);

        // 添加后刷新下权限
        refreshUserPermission();

        int status = mockMvc.perform(requestBuilderGetFunc.get())
                .andReturn()
                .getResponse()
                .getStatus();

        // 校验是否有权限
        if (status == HttpStatus.FORBIDDEN.value()) {
            throw new GenericException(String.format("接口 %s 权限校验失败 %s", url, permissionId));
        }

        // 删除权限
        rolePermissionMapper.deleteByPrimaryKey(userRolePermission.getId());

        // 删除后刷新下权限
        refreshUserPermission();

        // 删除权限后，调用接口，校验是否没有权限
        status = mockMvc.perform(requestBuilderGetFunc.get())
                .andReturn()
                .getResponse()
                .getStatus();
        if (status != HttpStatus.FORBIDDEN.value()) {
            throw new GenericException(String.format("接口 %s 没有设置权限 %s", url, permissionId));
        }
    }

    /**
     * 给用户组绑定对应权限
     *
     * @param permissionId
     * @return
     */
    private RolePermission initUserRolePermission(String permissionId) {
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleId(PERMISSION_USER_NAME);
        rolePermission.setId(IDGenerator.nextStr());
        rolePermission.setPermissionId(permissionId);
        rolePermission.setCreateUser(InternalUser.ADMIN.getValue());
        rolePermission.setUpdateUser(InternalUser.ADMIN.getValue());
        rolePermission.setCreateTime(System.currentTimeMillis());
        rolePermission.setUpdateTime(System.currentTimeMillis());
        rolePermissionMapper.insert(rolePermission);
        return rolePermission;
    }

    /**
     * 调用 is-login 接口刷新权限
     *
     * @throws Exception
     */
    private void refreshUserPermission() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/is-login");
        requestBuilder = setRequestBuilderHeader(requestBuilder, permissionAuthInfo);
        mockMvc.perform(requestBuilder);
    }

    @Data
    public static class AuthInfo {
        private String sessionId;
        private String csrfToken;

        public AuthInfo(String sessionId, String csrfToken) {
            this.sessionId = sessionId;
            this.csrfToken = csrfToken;
        }
    }
}
