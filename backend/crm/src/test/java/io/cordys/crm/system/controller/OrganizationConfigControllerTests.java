package io.cordys.crm.system.controller;

import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.constants.OrganizationConfigConstants;
import io.cordys.crm.system.domain.OrganizationConfig;
import io.cordys.crm.system.domain.OrganizationConfigDetail;
import io.cordys.crm.system.dto.response.EmailDTO;
import io.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import io.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrganizationConfigControllerTests extends BaseTest {

    @Resource
    private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;

    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;


    @Test
    @Order(1)
    public void testAdd() throws Exception {
        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setHost("test.com");
        emailDTO.setPort("25");   
        emailDTO.setPassword("test");
        this.requestPost("/organization/config/edit/email", emailDTO).andExpect(status().isOk());

    }

    @Test
    @Order(2)
    public void testEdit() throws Exception {
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(DEFAULT_ORGANIZATION_ID, OrganizationConfigConstants.ConfigType.EMAIL.name());
        OrganizationConfigDetail organizationConfigDetail = extOrganizationConfigDetailMapper.getOrganizationConfigDetail(organizationConfig.getId());
        Assertions.assertNotNull(organizationConfigDetail);
        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setHost("test.com");
        emailDTO.setPort("25");   
        emailDTO.setPassword("test");
        emailDTO.setAccount("sddd");
        emailDTO.setFrom("sddd");
        emailDTO.setRecipient("sddd");
        emailDTO.setSsl("true");
        emailDTO.setTsl("true");
        this.requestPost("/organization/config/edit/email", emailDTO).andExpect(status().isOk());

    }

    //获取邮件接口的测试
    @Test
    @Order(3)
    public void testGetEmail() throws Exception {
        this.requestGet("/organization/config/email").andExpect(status().isOk());
    }


    @Test
    @Order(5)
    public void testEmailConnect() throws Exception {
        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setHost("https://baidu.com");
        emailDTO.setPort("80");
        emailDTO.setAccount("aaa@fit2cloud.com");
        emailDTO.setPassword("test");
        emailDTO.setFrom("aaa@fit2cloud.com");
        emailDTO.setRecipient("aaa@fit2cloud.com");
        emailDTO.setSsl("true");
        emailDTO.setTsl("false");
        this.requestPost("/organization/config/test/email", emailDTO, status().is5xxServerError());
    }

   
} 