package io.cordys.crm.system.controller;

import io.cordys.common.pager.Pager;
import io.cordys.common.util.CodingUtils;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.request.UserAddRequest;
import io.cordys.crm.system.dto.request.UserPageRequest;
import io.cordys.crm.system.dto.response.EmailDTO;
import io.cordys.crm.system.dto.response.UserPageResponse;
import io.cordys.crm.system.service.PersonalCenterService;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PersonalCenterControllerTests extends BaseTest {

    @Resource
    private BaseMapper<User> userMapper;

    private static String userId = "";

    @Resource
    private PersonalCenterService personalCenterService;


    @Sql(scripts = {"/dml/init_department_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)

    @Test
    @Order(1)
    public void testGet() throws Exception {
        UserAddRequest request = new UserAddRequest();
        request.setName("testPassword");
        request.setPhone("12345678901");
        request.setGender(true);
        request.setEnable(true);
        request.setEmail("3Gyq3@Cordys.com");
        request.setDepartmentId("2222");
        request.setRoleIds(List.of("1", "2"));
        this.requestPost("/user/add", request).andExpect(status().isOk());

        UserPageRequest pageRequest = new UserPageRequest();
        pageRequest.setCurrent(1);
        pageRequest.setPageSize(10);
        pageRequest.setDepartmentId("2222");
        MvcResult mvcResult = this.requestPostWithOkAndReturn("/user/list", pageRequest);
        Pager<List<UserPageResponse>> result = getPageResult(mvcResult, UserPageResponse.class);
        UserPageResponse first = result.getList().getFirst();
        userId= first.getUserId();
    }

    @Test
    @Order(2)
    public void testSend() throws Exception {
        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setHost("smtp.163.com");
        emailDTO.setPort("465");
        emailDTO.setAccount("youliyuan0120@163.com");
        emailDTO.setPassword("PABMJIUZZIZWZVCY");
        emailDTO.setSsl("true");
        emailDTO.setTsl("true");
        this.requestPost("/organization/config/edit/email", emailDTO).andExpect(status().isOk());


        this.requestPost("/personal/center/mail/send_code?email=test@qq.com", null).andExpect(status().isOk());

    }

    @Test
    @Order(3)
    public void testVerify() throws Exception {
        this.requestPost("/personal/center/verifyCode?email=test@qq.com&code=3434323", null).andExpect(status().isOk());

    }

    @Test
    @Order(4)
    public void changePassword() throws Exception {

        this.requestPost("/personal/center/info/reset?password=Gyq124", null).andExpect(status().is5xxServerError());


       personalCenterService.resetUserPassword("Gyq124",userId);

        User user = userMapper.selectByPrimaryKey(userId);

        Assertions.assertTrue(StringUtils.equalsIgnoreCase(CodingUtils.md5("Gyq124"), user.getPassword()));
    }

}
