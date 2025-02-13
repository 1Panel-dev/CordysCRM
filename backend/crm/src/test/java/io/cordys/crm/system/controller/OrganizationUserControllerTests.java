package io.cordys.crm.system.controller;

import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.dto.request.*;
import io.cordys.crm.utils.FileBaseUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;

import java.util.List;
import java.util.Objects;

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
    public static final String USER_BATCH_EDIT = "/user/batch/edit";
    public static final String USER_DOWNLOAD_TEMPLATE = "/user/download/template";
    public static final String USER_IMPORT_PRE_CHECK = "/user/import/pre-check";
    public static final String USER_IMPORT = "/user/import";
    public static final String USER_OPTION = "/user/option";
    public static final String USER_ROLE_OPTION = "/user/role/option";
    public static final String USER_SYNC_CHECK = "/user/sync-check";
    public static final String USER_DELETE = "/user/delete/";
    public static final String USER_DELETE_CHECK = "/user/delete/check/";


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
        request.setDepartmentId("9");
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

    @Test
    @Order(9)
    public void batchEdit() throws Exception {
        UserBatchEditRequest request = new UserBatchEditRequest();
        request.setIds(List.of("u_1", "u_2"));
        request.setWorkCity("深圳");
        this.requestPost(USER_BATCH_EDIT, request).andExpect(status().isOk());
    }


    @Test
    @Order(10)
    public void downloadTemplate() throws Exception {
        this.requestGetExcel(USER_DOWNLOAD_TEMPLATE);
    }

    private MvcResult requestGetExcel(String url) throws Exception {
        return mockMvc.perform(getRequestBuilder(url))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    @Order(11)
    public void testImportCheckExcel() throws Exception {
        String filePath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("file/user.xlsx")).getPath();
        MockMultipartFile file = new MockMultipartFile("file", "11.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, FileBaseUtils.getFileBytes(filePath));
        LinkedMultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<>();
        paramMap.add("file", file);
        this.requestMultipart(USER_IMPORT_PRE_CHECK, paramMap);

    }

    @Test
    @Order(12)
    public void testImportExcel() throws Exception {
        String filePath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("file/user1.xlsx")).getPath();
        MockMultipartFile file = new MockMultipartFile("file", "1111.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, FileBaseUtils.getFileBytes(filePath));
        LinkedMultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<>();
        paramMap.add("file", file);
        this.requestMultipart(USER_IMPORT, paramMap);

    }

    @Test
    @Order(13)
    public void testUserOption() throws Exception {
        this.requestGet(USER_OPTION).andExpect(status().isOk());
    }

    @Test
    @Order(14)
    public void testUserRoleOption() throws Exception {
        this.requestGet(USER_ROLE_OPTION).andExpect(status().isOk());
    }

    @Test
    @Order(15)
    public void testSyncCheck() throws Exception {
        this.requestGet(USER_SYNC_CHECK).andExpect(status().isOk());
    }

    @Test
    @Order(16)
    public void testUserDelete() throws Exception {
        this.requestGet(USER_DELETE + "u_5").andExpect(status().isOk());
    }

    @Test
    @Order(15)
    public void testUserDeleteCheck() throws Exception {
        this.requestGet(USER_DELETE_CHECK + "u_5").andExpect(status().isOk());
    }
}
