package cn.cordys.crm.system.controller;


import cn.cordys.crm.contract.constants.ContractApprovalStatus;
import cn.cordys.crm.contract.constants.ContractPaymentPlanStatus;
import cn.cordys.crm.contract.constants.ContractStage;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.opportunity.constants.ApprovalState;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.domain.OpportunityQuotation;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.response.NotificationDTO;
import cn.cordys.crm.system.job.NoticeExpireJob;
import cn.cordys.crm.system.mapper.ExtNotificationMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AddNoticeExpireTests {

    @Resource
    private NoticeExpireJob noticeExpireJob;

    @Resource
    private BaseMapper<OpportunityQuotation> opportunityQuotationBaseMapper;

    @Resource
    private BaseMapper<ContractPaymentPlan> contractPaymentPlanBaseMapper;

    @Resource
    private BaseMapper<Contract> contractBaseMapper;

    @Resource
    private BaseMapper<Opportunity> opportunityBaseMapper;

    @Resource
    private BaseMapper<Customer> customerBaseMapper;

    @Resource
    private BaseMapper<User> userBaseMapper;

    @Resource
    private BaseMapper<OrganizationUser> organizationUserBaseMapper;


    @Resource
    private ExtNotificationMapper extNotificationMapper;

    void saveNotice() {
        OpportunityQuotation opportunityQuotation = new OpportunityQuotation();
        opportunityQuotation.setId("SDDFDJJND");
        opportunityQuotation.setName("测试报价");
        opportunityQuotation.setApprovalStatus(ApprovalState.APPROVED.getId());
        opportunityQuotation.setOpportunityId("SDDFDJJND");
        opportunityQuotation.setCreateUser("aaa");
        opportunityQuotation.setCreateTime(System.currentTimeMillis());
        opportunityQuotation.setUpdateTime(System.currentTimeMillis());
        opportunityQuotation.setUpdateUser("admin");
        opportunityQuotation.setAmount(BigDecimal.valueOf(10000));
        opportunityQuotation.setUntilTime(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 4);
        opportunityQuotation.setOrganizationId("100001");
        opportunityQuotationBaseMapper.insert(opportunityQuotation);

        opportunityQuotation = new OpportunityQuotation();
        opportunityQuotation.setId("SgDFDJJND");
        opportunityQuotation.setName("测试报价");
        opportunityQuotation.setApprovalStatus(ApprovalState.APPROVED.getId());
        opportunityQuotation.setOpportunityId("SDDFDJJND");
        opportunityQuotation.setCreateUser("aaa");
        opportunityQuotation.setCreateTime(System.currentTimeMillis());
        opportunityQuotation.setUpdateTime(System.currentTimeMillis());
        opportunityQuotation.setUpdateUser("admin");
        opportunityQuotation.setAmount(BigDecimal.valueOf(10000));
        opportunityQuotation.setUntilTime(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
        opportunityQuotation.setOrganizationId("100001");
        opportunityQuotationBaseMapper.insert(opportunityQuotation);

        ContractPaymentPlan contractPaymentPlan = new ContractPaymentPlan();
        contractPaymentPlan.setId("SDDFDJJND");
		contractPaymentPlan.setName("test");
        contractPaymentPlan.setOrganizationId("100001");
        contractPaymentPlan.setContractId("SDDFDJJND");
        contractPaymentPlan.setPlanStatus(ContractPaymentPlanStatus.PENDING.name());
        contractPaymentPlan.setPlanAmount(BigDecimal.valueOf(10000));
        contractPaymentPlan.setOwner("aaa");
        contractPaymentPlan.setPlanEndTime(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 4);
        contractPaymentPlan.setCreateUser("admin");
        contractPaymentPlan.setCreateTime(System.currentTimeMillis());
        contractPaymentPlan.setUpdateUser("admin");
        contractPaymentPlan.setUpdateTime(System.currentTimeMillis());
        contractPaymentPlanBaseMapper.insert(contractPaymentPlan);

        contractPaymentPlan = new ContractPaymentPlan();
        contractPaymentPlan.setId("SgDFDJJND");
		contractPaymentPlan.setName("test");
        contractPaymentPlan.setOrganizationId("100001");
        contractPaymentPlan.setContractId("SDDFDJJND");
        contractPaymentPlan.setPlanStatus(ContractPaymentPlanStatus.PENDING.name());
        contractPaymentPlan.setPlanAmount(BigDecimal.valueOf(10000));
        contractPaymentPlan.setOwner("aaa");
        contractPaymentPlan.setPlanEndTime(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
        contractPaymentPlan.setCreateUser("admin");
        contractPaymentPlan.setCreateTime(System.currentTimeMillis());
        contractPaymentPlan.setUpdateUser("admin");
        contractPaymentPlan.setUpdateTime(System.currentTimeMillis());
        contractPaymentPlanBaseMapper.insert(contractPaymentPlan);

        Contract contract = new Contract();
        contract.setId("SDDFDJJND");
        contract.setOrganizationId("100001");
        contract.setNumber("SDDFDJJND");
        contract.setName("测试合同");
        contract.setCustomerId("100001");
        contract.setAmount(BigDecimal.valueOf(10000));
        contract.setApprovalStatus(ContractApprovalStatus.APPROVED.name());
        contract.setStage(ContractStage.PENDING_SIGNING.name());
        contract.setCreateUser("admin");
        contract.setOwner("aaa");
        contract.setStartTime(System.currentTimeMillis());
        contract.setEndTime(System.currentTimeMillis());
        contract.setCreateTime(System.currentTimeMillis());
        contract.setUpdateUser("admin");
        contract.setUpdateTime(System.currentTimeMillis());
        contractBaseMapper.insert(contract);

        Opportunity opportunity = new Opportunity();
        opportunity.setId("SDDFDJJND");
        opportunity.setName("测试商机");
        opportunity.setCustomerId("100001");
        opportunity.setOwner("aaa");
        opportunity.setAmount(BigDecimal.valueOf(10000));
        opportunity.setPossible(BigDecimal.valueOf(70));
        opportunity.setPos(500L);
        opportunity.setStage("SUCCESS");
        opportunity.setFollower("aaa");
        opportunity.setContactId("aaa");
        opportunity.setActualEndTime(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 10);
        opportunity.setExpectedEndTime(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 10);
        opportunity.setProducts(List.of("100001"));
        opportunity.setCreateUser("admin");
        opportunity.setCreateTime(System.currentTimeMillis());
        opportunity.setUpdateUser("admin");
        opportunity.setUpdateTime(System.currentTimeMillis());
        opportunity.setOrganizationId("100001");
        opportunityBaseMapper.insert(opportunity);

        Customer customer = new Customer();
        customer.setId("100001");
        customer.setName("测试客户");
        customer.setOwner("aaa");
        customer.setInSharedPool(false);
        customer.setFollower("aaa");
        customer.setFollowTime(System.currentTimeMillis());
        customer.setCreateUser("admin");
        customer.setCreateTime(System.currentTimeMillis());
        customer.setUpdateUser("admin");
        customer.setUpdateTime(System.currentTimeMillis());
        customer.setOrganizationId("100001");
        customerBaseMapper.insert(customer);

        User user = new User();
        user.setId("aaa");
        user.setName("测试用户");
        user.setEmail("test@example.com");
        user.setPhone("1234567890");
        user.setPassword("123456");
        user.setGender(true);
        user.setLanguage("zh_CN");
        user.setCreateUser("admin");
        user.setCreateTime(System.currentTimeMillis());
        user.setUpdateUser("admin");
        user.setUpdateTime(System.currentTimeMillis());
        userBaseMapper.insert(user);

        OrganizationUser organizationUser = new OrganizationUser();
        organizationUser.setId("100001");
        organizationUser.setOrganizationId("100001");
        organizationUser.setDepartmentId("100001");
        organizationUser.setEnable(true);
        organizationUser.setUserId("aaa");
        organizationUser.setCreateUser("admin");
        organizationUser.setCreateTime(System.currentTimeMillis());
        organizationUser.setUpdateUser("admin");
        organizationUser.setUpdateTime(System.currentTimeMillis());
        organizationUserBaseMapper.insert(organizationUser);

    }

    @Test
    @Order(0)
    public void cleanupNotificationSuccess() {
        saveNotice();
        noticeExpireJob.onEvent();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Notification notification = new Notification();
        notification.setOrganizationId("100001");
        notification.setResourceType("OPPORTUNITY");
        List<NotificationDTO> notifications = extNotificationMapper.selectByAnyOne(notification);
        Assertions.assertTrue(CollectionUtils.isNotEmpty(notifications));
        notification = new Notification();
        notification.setOrganizationId("100001");
        notification.setResourceType("CONTRACT");
        notifications = extNotificationMapper.selectByAnyOne(notification);
        Assertions.assertTrue(CollectionUtils.isNotEmpty(notifications));
    }
}
