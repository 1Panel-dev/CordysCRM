package io.cordys.crm.system.controller;

import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.dto.request.UserPageRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrganizationUserControllerTests extends BaseTest {

    public static final String USER_LIST = "/user/list";

    @Sql(scripts = {"/dml/init_user_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Test
    @Order(1)
    public void userList() throws Exception {
        UserPageRequest request = new UserPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setDepartmentId("8");
        this.requestPost(USER_LIST, request).andExpect(status().isOk());
    }
}
