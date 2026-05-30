package cn.cordys.crm.system.controller;


import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class LoginControllerTests {
    @Resource
    private MockMvc mockMvc;
    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;

    @Test
    @Sql(scripts = {"/dml/init_user_login_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void testLogin() throws Exception {
        // 1. 正常登录
        String login = "/login";
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(login)
                        .content("""
                                {
                                  "username": "test.login@cordys.io",
                                  "password": "test.login",
                                  "authenticate": "LOCAL",
                                  "loginAddress": "LOCAL",
                                  "platform": "LOCAL"
                                }
                                """)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // 验证返回结果
        String contentAsString = mvcResult.getResponse().getContentAsString();
        log.info(contentAsString);
    }
}
