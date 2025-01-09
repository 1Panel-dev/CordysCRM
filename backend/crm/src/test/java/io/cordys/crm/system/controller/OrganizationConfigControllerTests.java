package io.cordys.crm.system.controller;

import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.constants.OrganizationConfigConstants;
import io.cordys.crm.system.domain.OrganizationConfig;
import io.cordys.crm.system.domain.OrganizationConfigDetail;
import io.cordys.crm.system.dto.response.EmailDTO;
import io.cordys.crm.system.dto.response.SyncOrganizationDTO;
import io.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import io.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import io.cordys.mybatis.BaseMapper;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.annotation.Resource;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrganizationConfigControllerTests extends BaseTest {

    @Resource
    private BaseMapper<OrganizationConfig> organizationConfigMapper;

    @Resource
    private BaseMapper<OrganizationConfigDetail> organizationConfigDetailMapper;

    @Resource
    private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;

    @Resource
    private ExtOrganizationConfigMapper extOrganizationConfigMapper;

    // 创建组织配置
    public String testCreateOrganizationConfig() {
        OrganizationConfig organizationConfig = new OrganizationConfig();
        organizationConfig.setId(IDGenerator.nextStr());
        organizationConfig.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        organizationConfig.setType(OrganizationConfigConstants.ConfigType.EMAIL.name());
        organizationConfig.setCreateTime(System.currentTimeMillis());
        organizationConfig.setCreateUser("admin");
        organizationConfig.setUpdateTime(System.currentTimeMillis());
        organizationConfig.setUpdateUser("admin");
        organizationConfigMapper.insert(organizationConfig);
        OrganizationConfigDetail organizationConfigDetail = new OrganizationConfigDetail();
        organizationConfigDetail.setId(IDGenerator.nextStr());
        organizationConfigDetail.setConfigId(organizationConfig.getId());
        organizationConfigDetail.setType(OrganizationConfigConstants.ConfigType.EMAIL.name());
        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setHost("test.com");
        emailDTO.setPort("25");   
        emailDTO.setPassword("test");
        organizationConfigDetail.setContent(JSON.toJSONBytes(emailDTO));
        organizationConfigDetail.setCreateTime(System.currentTimeMillis());
        organizationConfigDetail.setCreateUser("admin");
        organizationConfigDetail.setUpdateTime(System.currentTimeMillis());
        organizationConfigDetail.setUpdateUser("admin");
        organizationConfigDetailMapper.insert(organizationConfigDetail);
        return organizationConfigDetail.getId();
    }


    @Test
    @Order(1)
    public void testAdd() throws Exception {
        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setHost("test.com");
        emailDTO.setPort("25");   
        emailDTO.setPassword("test");
        this.requestPost("/organization/config/add/email", emailDTO).andExpect(status().isOk());

        SyncOrganizationDTO syncOrganizationDTO = new SyncOrganizationDTO();
        syncOrganizationDTO.setCorpId("test");
        syncOrganizationDTO.setAgentId("test");
        syncOrganizationDTO.setAppSecret("test");
        syncOrganizationDTO.setType("WECOM");
        this.requestPost("/organization/config/add/synchronization", syncOrganizationDTO).andExpect(status().isOk());
    }

    @Test
    @Order(2)
    public void testEdit() throws Exception {
        OrganizationConfig organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(DEFAULT_ORGANIZATION_ID, OrganizationConfigConstants.ConfigType.EMAIL.name());
        OrganizationConfigDetail organizationConfigDetail = extOrganizationConfigDetailMapper.getOrganizationConfigDetail(organizationConfig.getId());
        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setHost("test.com");
        emailDTO.setPort("25");   
        emailDTO.setPassword("test");
        emailDTO.setAccount("sddd");
        emailDTO.setFrom("sddd");
        emailDTO.setRecipient("sddd");
        emailDTO.setSsl("true");
        emailDTO.setTsl("true");
        emailDTO.setId(organizationConfigDetail.getId());
        this.requestPost("/organization/config/update/email", emailDTO).andExpect(status().isOk());

        organizationConfig = extOrganizationConfigMapper.getOrganizationConfig(DEFAULT_ORGANIZATION_ID, OrganizationConfigConstants.ConfigType.SYNCHRONIZATION.name());
        organizationConfigDetail = extOrganizationConfigDetailMapper.getOrganizationConfigDetail(organizationConfig.getId());
        SyncOrganizationDTO syncOrganizationDTO = new SyncOrganizationDTO();
        syncOrganizationDTO.setCorpId("test");
        syncOrganizationDTO.setAgentId("test");
        syncOrganizationDTO.setAppSecret("test");
        syncOrganizationDTO.setType("WECOM");
        syncOrganizationDTO.setId(organizationConfigDetail.getId());
        this.requestPost("/organization/config/update/synchronization", syncOrganizationDTO).andExpect(status().isOk());
    }
       

    @Test
    @Order(3)
    public void testDelete() throws Exception {
        String id = this.testCreateOrganizationConfig();
        this.requestGet("/organization/config/delete/" + id).andExpect(status().isOk());
    }

    //获取邮件接口的测试
    @Test
    @Order(4)
    public void testGetEmail() throws Exception {
        this.requestGet("/organization/config/email").andExpect(status().isOk());
    }

    //获取同步组织接口的测试
    @Test
    @Order(5)
    public void testGetSynOrganization() throws Exception {
        this.requestGet("/organization/config/synchronization").andExpect(status().isOk());
    }

   
} 