package io.cordys.crm.opportunity.controller;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.follow.constants.FollowUpPlanStatusType;
import io.cordys.crm.follow.domain.FollowUpPlan;
import io.cordys.crm.follow.domain.FollowUpRecord;
import io.cordys.crm.follow.dto.request.FollowUpPlanAddRequest;
import io.cordys.crm.follow.dto.request.FollowUpPlanStatusRequest;
import io.cordys.crm.follow.dto.request.FollowUpPlanUpdateRequest;
import io.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import io.cordys.crm.opportunity.domain.Opportunity;
import io.cordys.crm.system.job.listener.FollowUpPlanRemindListener;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OpportunityFollowPlanControllerTests extends BaseTest {

    private static final String BASE_PATH = "/opportunity/follow/plan/";

    private static final String CANCEL_PLAN = "cancel/{0}";
    private static final String STATUS_UPDATE = "status/update";

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }


    private static FollowUpPlan addFollowUpPlan;
    @Resource
    private BaseMapper<FollowUpPlan> followUpPlanMapper;
    @Resource
    private FollowUpPlanRemindListener followUpPlanRemindListener;
    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private BaseMapper<Opportunity> opportunityMapper;

    @Test
    @Order(1)
    void testAdd() throws Exception {
        long timestamp = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toEpochSecond() * 1000;
        FollowUpPlanAddRequest request = new FollowUpPlanAddRequest();
        request.setCustomerId("wx_123");
        request.setOpportunityId("wx_12345");
        request.setOwner("admin");
        request.setContactId("123456");
        request.setType("CUSTOMER");
        request.setMethod("1");
        request.setContent("计划一下");
        request.setEstimatedTime(timestamp);
        request.setModuleFields(List.of(new BaseModuleFieldValue("id", "value")));
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        FollowUpRecord resultData = getResultData(mvcResult, FollowUpRecord.class);
        FollowUpPlan followUpPlan = followUpPlanMapper.selectByPrimaryKey(resultData.getId());
        addFollowUpPlan = followUpPlan;
    }


    @Test
    @Order(2)
    void testUpdate() throws Exception {
        long timestamp = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toEpochSecond() * 1000;
        FollowUpPlanUpdateRequest request = new FollowUpPlanUpdateRequest();
        request.setId("1234");
        request.setCustomerId("wx_123");
        request.setOpportunityId("wx_12345");
        request.setOwner("admin");
        request.setContactId("1234567");
        request.setMethod("2");
        request.setType("CUSTOMER");
        request.setContent("计划两下");
        request.setEstimatedTime(timestamp);
        request.setConverted(false);
        request.setModuleFields(List.of(new BaseModuleFieldValue("id", "value")));
        this.requestPost(DEFAULT_UPDATE, request);

        request.setId(addFollowUpPlan.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, request);
    }


    @Test
    @Order(3)
    void testList() throws Exception {
        FollowUpRecordPageRequest request = new FollowUpRecordPageRequest();
        request.setSourceId("123");
        request.setCurrent(1);
        request.setPageSize(10);
        this.requestPost(DEFAULT_PAGE, request);
    }

    @Test
    @Order(4)
    void testGet() throws Exception {
        this.requestGet(DEFAULT_GET, addFollowUpPlan.getId());
    }


    @Test
    @Order(4)
    void testStatusUpdate() throws Exception {
        FollowUpPlanStatusRequest request = new FollowUpPlanStatusRequest();
        request.setId(addFollowUpPlan.getId());
        request.setStatus(FollowUpPlanStatusType.COMPLETED.name());
        this.requestPost(STATUS_UPDATE, request);
    }

    @Test
    @Order(5)
    void testCancelPlan() throws Exception {
        this.requestGetWithOk(CANCEL_PLAN, addFollowUpPlan.getId());
    }

    @Test
    @Order(6)
    void tesDeletePlan() throws Exception {
        this.requestGetWithOk(DEFAULT_DELETE, addFollowUpPlan.getId());
    }


    @Test
    @Order(3)
    void testJob() throws Exception {
        Customer customer = new Customer();
        customer.setId("wx_123");
        customer.setName("测试客户");
        customer.setCreateUser("admin");
        customer.setCreateTime(System.currentTimeMillis());
        customer.setUpdateUser("admin");
        customer.setUpdateTime(System.currentTimeMillis());
        customer.setOrganizationId("100001");
        customer.setInSharedPool(false);
        customerMapper.insert(customer);

        Opportunity opportunity = new Opportunity();
        opportunity.setId("wx_12345");
        opportunity.setName("测试商机");
        opportunity.setCustomerId("123");
        opportunity.setProducts(List.of("123456"));
        opportunity.setOrganizationId("100001");
        opportunity.setStage("STAGE_1");
        opportunity.setContactId("1233");
        opportunity.setOwner("admin");
        opportunity.setCreateUser("admin");
        opportunity.setCreateTime(System.currentTimeMillis());
        opportunity.setUpdateUser("admin");
        opportunity.setUpdateTime(System.currentTimeMillis());
        opportunity.setExpectedEndTime(System.currentTimeMillis());
        opportunityMapper.insert(opportunity);
        followUpPlanRemindListener.followUpPlanRemind();

        customerMapper.deleteByPrimaryKey("wx_123");
        opportunityMapper.deleteByPrimaryKey("wx_123");
    }

}
