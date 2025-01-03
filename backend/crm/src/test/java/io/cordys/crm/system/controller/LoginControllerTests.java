package io.cordys.crm.system.controller;

import io.cordys.common.util.LogUtils;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginControllerTests {
    @Resource
    private MockMvc mockMvc;

    @Test
    @Sql(scripts = {"/dml/init_user_login_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void testLogin() throws Exception {
        // 1. 正常登录
        String login = "/login";
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(login)
                        .content(String.format("{\"username\":\"%s\",\"password\":\"%s\",\"platform\":\"%s\"}", "admin", "CordysCRM", "mobile"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // 验证返回结果
        String contentAsString = mvcResult.getResponse().getContentAsString();
        LogUtils.info(contentAsString);
    }
}
