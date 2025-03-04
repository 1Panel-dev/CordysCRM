package io.cordys.crm.system.controller;

import io.cordys.common.pager.Pager;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.dto.request.PersonalInfoRequest;
import io.cordys.crm.system.dto.request.PersonalPasswordRequest;
import io.cordys.crm.system.dto.request.UserAddRequest;
import io.cordys.crm.system.dto.request.UserPageRequest;
import io.cordys.crm.system.dto.response.EmailDTO;
import io.cordys.crm.system.dto.response.UserPageResponse;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PersonalCenterControllerTests extends BaseTest {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private BaseMapper<Department> departmentBaseMapper;


    @Test
    @Order(1)
    public void testGet() throws Exception {
        Department department = new Department();
        department.setId("221");
        department.setName("部门222");
        department.setOrganizationId("100001");
        department.setParentId("4");
        department.setPos(222L);
        department.setCreateTime(1736240043609L);
        department.setUpdateTime(1736240043609L);
        department.setCreateUser("gyq");
        department.setUpdateUser("gyq");
        department.setResource("INTERNAL");
        department.setResourceId(null);
        departmentBaseMapper.insert(department);
        UserAddRequest request = new UserAddRequest();
        request.setName("testPassword");
        request.setPhone("12345678911");
        request.setGender(true);
        request.setEnable(true);
        request.setEmail("3Gyq3@Cordys.com");
        request.setDepartmentId("222");
        request.setRoleIds(List.of("1", "2"));
        this.requestPost("/user/add", request).andExpect(status().isOk());

        UserPageRequest pageRequest = new UserPageRequest();
        pageRequest.setCurrent(1);
        pageRequest.setPageSize(10);
        pageRequest.setDepartmentIds(List.of("222"));
        MvcResult mvcResult = this.requestPostWithOkAndReturn("/user/list", pageRequest);
        Pager<List<UserPageResponse>> result = getPageResult(mvcResult, UserPageResponse.class);
        UserPageResponse first = result.getList().getFirst();
        Assertions.assertTrue(StringUtils.isNotBlank(first.getUserId()));
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
    public void testUpdateUser() throws Exception {
        PersonalInfoRequest personalInfoRequest = new PersonalInfoRequest();
        personalInfoRequest.setPhone("12345678912");
        personalInfoRequest.setEmail("3Gyq3@Cordys.com");

        this.requestPost("/personal/center/update", personalInfoRequest).andExpect(status().is5xxServerError());


    }

    @Test
    @Order(4)
    public void changePassword() throws Exception {
        String PREFIX = "personal_email_code:";  // Redis 存储前缀
        stringRedisTemplate.opsForValue().set(PREFIX + "3Gyq3@Cordys.com", "253574", 10, TimeUnit.MINUTES);
        PersonalPasswordRequest personalPasswordRequest  = new PersonalPasswordRequest();
        personalPasswordRequest.setPassword("Gyq124");
        personalPasswordRequest.setEmail("3Gyq3@Cordys.com");
        personalPasswordRequest.setCode("253574");
        this.requestPost("/personal/center/info/reset", personalPasswordRequest).andExpect(status().isOk());
    }

}
