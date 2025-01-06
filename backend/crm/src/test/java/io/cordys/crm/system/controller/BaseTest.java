package io.cordys.crm.system.controller;

import com.jayway.jsonpath.JsonPath;
import io.cordys.common.util.JSON;
import io.cordys.common.util.LogUtils;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class BaseTest {

    @Resource
    protected MockMvc mockMvc;

    protected static String sessionId;
    protected static String csrfToken;
    protected static AuthInfo adminAuthInfo;
    protected static Map<String, AuthInfo> permissionAuthInfoMap = new HashMap<>();

    protected static final String DEFAULT_USER_PASSWORD = "CordysCRM";
    protected static final String DEFAULT_PLATFORM = "mobile";

    protected String getBasePath() {
        return StringUtils.EMPTY;
    }

    @BeforeEach
    public void login() throws Exception {
        if (adminAuthInfo == null) {
            adminAuthInfo = initAuthInfo();
            sessionId = adminAuthInfo.getSessionId();
            csrfToken = adminAuthInfo.getCsrfToken();
            permissionAuthInfoMap.put("ORGANIZATION", adminAuthInfo);
        }
        // todo add other permission
    }

    private AuthInfo initAuthInfo() throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .content(String.format("{\"username\":\"%s\",\"password\":\"%s\",\"platform\":\"%s\"}", "admin", BaseTest.DEFAULT_USER_PASSWORD, BaseTest.DEFAULT_PLATFORM))
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
