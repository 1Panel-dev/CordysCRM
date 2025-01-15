package io.cordys.crm.system.controller;

import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.dto.request.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrganizationUserControllerTests extends BaseTest {

    public static final String USER_LIST = "/user/list";
    public static final String USER_ADD = "/user/add";
    public static final String USER_DETAIL = "/user/detail/";
    public static final String USER_UPDATE = "/user/update";
    public static final String USER_RESET_PASSWORD = "/user/reset-password/";
    public static final String USER_BATCH_ENABLE = "/user/batch-enable";
    public static final String USER_BATCH_RESET_PASSWORD = "/user/batch/reset-password";

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

    @Test
    @Order(2)
    public void userAdd() throws Exception {
        UserAddRequest request = new UserAddRequest();
        request.setName("test");
        request.setPhone("12345678901");
        request.setGender(true);
        request.setEnable(true);
        request.setEmail("1@Cordys.com");
        request.setDepartmentId("1");
        request.setRoleIds(List.of("1", "2"));
        this.requestPost(USER_ADD, request).andExpect(status().isOk());

        //格式错误
        request.setEmail("1234");
        this.requestPost(USER_ADD, request).andExpect(status().is4xxClientError());
        //邮箱重复
        request.setEmail("1@Cordys.com");
        this.requestPost(USER_ADD, request).andExpect(status().is5xxServerError());
        //电话重复
        request.setEmail("2@Cordys.com");
        this.requestPost(USER_ADD, request).andExpect(status().is5xxServerError());
    }


    @Test
    @Order(3)
    public void userDetail() throws Exception {
        this.requestGet(USER_DETAIL + "u_1").andExpect(status().isOk());
    }

    @Test
    @Order(4)
    public void userUpdate() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("test111");
        request.setPhone("12345633342");
        request.setGender(true);
        request.setEnable(true);
        request.setEmail("221@Cordys.com");
        request.setDepartmentId("8");
        request.setRoleIds(List.of("1", "2", "3"));
        request.setId("u_1");
        this.requestPost(USER_UPDATE, request).andExpect(status().isOk());
    }


    @Test
    @Order(5)
    public void resetPassword() throws Exception {
        this.requestGet(USER_RESET_PASSWORD + "5").andExpect(status().isOk());
    }

    @Test
    @Order(6)
    public void batchEnable() throws Exception {
        UserBatchEnableRequest request = new UserBatchEnableRequest();
        request.setEnable(true);
        request.setIds(List.of("u_1", "u_2"));
        this.requestPost(USER_BATCH_ENABLE, request).andExpect(status().isOk());
    }

    @Test
    @Order(7)
    public void batchResetPassword() throws Exception {
        UserBatchRequest request = new UserBatchRequest();
        request.setIds(List.of("u_1", "u_2"));
        this.requestPost(USER_BATCH_RESET_PASSWORD, request).andExpect(status().isOk());

    }
}
