package cn.cordys.crm.system.controller;

import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.CodingUtils;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.follow.dto.request.FollowUpPlanPageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import cn.cordys.crm.opportunity.constants.StageType;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.system.domain.Department;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.request.*;
import cn.cordys.crm.system.dto.response.EmailDTO;
import cn.cordys.crm.system.dto.response.UserPageResponse;
import cn.cordys.crm.system.dto.response.UserResponse;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
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
    @Resource
    private BaseMapper<Customer>customerBaseMapper;
    @Resource
    private BaseMapper<CustomerContact>customerContactBaseMapper;
    @Resource
    private BaseMapper<User>userBaseMapper;

    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;

    @Resource
    private BaseMapper<Opportunity> opportunityBaseMapper;

    private static String userId = "";


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
        request.setDepartmentId("221");
        request.setRoleIds(List.of("1", "2"));
        this.requestPostWithOkAndReturn("/user/add", request);

        UserPageRequest pageRequest = new UserPageRequest();
        pageRequest.setCurrent(1);
        pageRequest.setPageSize(10);
        pageRequest.setDepartmentIds(List.of("221"));
        MvcResult mvcResult = this.requestPostWithOkAndReturn("/user/list", pageRequest);
        Pager<List<UserPageResponse>> result = getPageResult(mvcResult, UserPageResponse.class);
        UserPageResponse first = result.getList().getFirst();
        Assertions.assertTrue(StringUtils.isNotBlank(first.getUserId()));
        userId = first.getId();

        User user = new User();
        user.setId(userId);
        user.setName("fff");
        user.setPhone("12345678911");
        user.setPassword("############");
        user.setEmail("4545@qq.com");
        user.setGender(true);
        user.setCreateTime(System.currentTimeMillis());
        user.setCreateUser("admin");
        user.setUpdateTime(System.currentTimeMillis());
        user.setUpdateUser("admin");
        userBaseMapper.insert(user);
        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setDepartmentId("221");
        orgUser.setEnable(true);
        orgUser.setId(IDGenerator.nextStr());
        orgUser.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        orgUser.setUserId(userId);
        orgUser.setCreateTime(System.currentTimeMillis());
        orgUser.setCreateUser("admin");
        orgUser.setUpdateTime(System.currentTimeMillis());
        orgUser.setUpdateUser("admin");
        organizationUserMapper.insert(orgUser);

        UserResponse userDetail = extUserMapper.getUserDetail(first.getId());
        Assertions.assertNotNull(userDetail);

    }

    @Test
    @Order(2)
    public void testSend() throws Exception {
        String email = "test@qq.com";
        SendEmailDTO sendEmailDTO = new SendEmailDTO();
        sendEmailDTO.setEmail(email);

        this.requestPost("/personal/center/mail/code/send", sendEmailDTO).andExpect(status().is5xxServerError());

        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setHost("smtp.163.com");
        emailDTO.setPort("465");
        emailDTO.setAccount("Test@163.com");
        emailDTO.setPassword("ABCDEFGHI");
        emailDTO.setSsl("true");
        emailDTO.setTsl("true");
        this.requestPost("/organization/settings/email/edit", emailDTO).andExpect(status().isOk());


        this.requestPost("/personal/center/mail/code/send", sendEmailDTO).andExpect(status().is5xxServerError());

    }

    @Test
    @Order(3)
    public void testUpdateUser() throws Exception {
        PersonalInfoRequest personalInfoRequest = new PersonalInfoRequest();
        personalInfoRequest.setPhone("12345678912");
        personalInfoRequest.setEmail("3Gyq356@Cordys.com");

        this.requestPost("/personal/center/update", personalInfoRequest).andExpect(status().isOk());


    }



    @Test
    @Order(4)
    public void getRepeatCustomer() throws Exception {
        String s = IDGenerator.nextStr();
        Opportunity opportunity = new Opportunity();
        opportunity.setCustomerId(s);
        opportunity.setOwner(userId);
        opportunity.setName("商机一");
        opportunity.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        opportunity.setAmount(BigDecimal.ONE);
        opportunity.setPossible(BigDecimal.TEN);
        opportunity.setStage(StageType.CREATE.name());
        opportunity.setProducts(List.of("1","2"));
        opportunity.setContactId("admin");
        opportunity.setId(IDGenerator.nextStr());
        opportunity.setCreateTime(System.currentTimeMillis());
        opportunity.setCreateUser("admin");
        opportunity.setUpdateTime(System.currentTimeMillis());
        opportunity.setUpdateUser("admin");
        opportunity.setExpectedEndTime(System.currentTimeMillis());
        opportunityBaseMapper.insert(opportunity);

        Customer customer = new Customer();
        customer.setId(s);
        customer.setName("kehu");
        customer.setOwner(userId);
        customer.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        customer.setCreateTime(System.currentTimeMillis());
        customer.setCreateUser("admin");
        customer.setUpdateTime(System.currentTimeMillis());
        customer.setUpdateUser("admin");
        customer.setInSharedPool(false);
        customerBaseMapper.insert(customer);

        CustomerContact customerContact = new CustomerContact();
        customerContact.setId(IDGenerator.nextStr());
        customerContact.setCustomerId(s);
        customerContact.setOwner(userId);
        customerContact.setName("联系人一");
        customerContact.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        customerContact.setCreateTime(System.currentTimeMillis());
        customerContact.setCreateUser("admin");
        customerContact.setUpdateTime(System.currentTimeMillis());
        customerContact.setUpdateUser("admin");
        customerContact.setPhone("12345678912");
        customerContact.setEnable(true);
        customerContactBaseMapper.insert(customerContact);


        RepeatCustomerPageRequest request = new RepeatCustomerPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setName("kehu");
        //this.requestPost("/personal/center/repeat/customer", request).andExpect(status().isOk());

        request = new RepeatCustomerPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setName("12345678912");
        //this.requestPost("/personal/center/repeat/contact", request).andExpect(status().isOk());
    }

    @Test
    @Order(5)
    void testFollowPlanList() throws Exception {
        FollowUpPlanPageRequest request = new FollowUpPlanPageRequest();
        request.setSourceId("NULL");
        request.setCurrent(1);
        request.setPageSize(10);
        request.setStatus("ALL");
        MvcResult mvcResult = this.requestPostWithOkAndReturn("/personal/center/follow/plan/list", request);
        Pager<List<FollowUpPlanListResponse>> result = getPageResult(mvcResult, FollowUpPlanListResponse.class);
        Assertions.assertNotNull(result.getList());
    }


    @Test
    @Order(6)
    public void testAnnouncement() throws Exception {
        RepeatCustomerPageRequest request = new RepeatCustomerPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setName("kehu");
        //this.requestPost("/personal/center/repeat/clue", request).andExpect(status().isOk());
    }

    @Test
    @Order(7)
    public void testAnnouncementDetail() throws Exception {
        RepeatCustomerDetailPageRequest request = new RepeatCustomerDetailPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setName("kehu");
        //this.requestPost("/personal/center/repeat/clue/detail", request).andExpect(status().isOk());
    }

    @Test
    @Order(8)
    public void testRepeatOpportunityDetail() throws Exception {
        RepeatCustomerDetailPageRequest request = new RepeatCustomerDetailPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setName("kehu");
       // this.requestPost("/personal/center/repeat/opportunity/detail", request).andExpect(status().isOk());
    }

    @Test
    @Order(9)
    public void changePassword() throws Exception {
        PersonalInfoRequest personalInfoRequest = new PersonalInfoRequest();
        personalInfoRequest.setPhone("4000520755");
        personalInfoRequest.setEmail("admin@cordys-crm.io");

        this.requestPost("/personal/center/update", personalInfoRequest).andExpect(status().isOk());
        String PREFIX = "personal_email_code:";  // Redis 存储前缀
        stringRedisTemplate.opsForValue().set(PREFIX + "3Gyq3@Cordys.com", "253574", 10, TimeUnit.MINUTES);
        PersonalPasswordRequest personalPasswordRequest  = new PersonalPasswordRequest();
        personalPasswordRequest.setPassword("Gyq124");
        personalPasswordRequest.setOriginPassword("678911");
        this.requestPost("/personal/center/info/reset", personalPasswordRequest);
        extUserMapper.updateUserPassword(CodingUtils.md5(DEFAULT_USER_PASSWORD), "admin");
       // personalPasswordRequest.setPassword(DEFAULT_USER_PASSWORD);
        adminAuthInfo=null;
        permissionAuthInfo=null;
        //DEFAULT_USER_PASSWORD=CodingUtils.md5("Gyq124");
        login();


    }
}
