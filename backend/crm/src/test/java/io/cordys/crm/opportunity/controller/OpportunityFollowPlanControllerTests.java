package io.cordys.crm.opportunity.controller;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.follow.domain.FollowUpPlan;
import io.cordys.crm.follow.domain.FollowUpRecord;
import io.cordys.crm.follow.dto.request.FollowUpPlanAddRequest;
import io.cordys.crm.follow.dto.request.FollowUpPlanUpdateRequest;
import io.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OpportunityFollowPlanControllerTests extends BaseTest {

    private static final String BASE_PATH = "/opportunity/follow/plan/";

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }


    private static FollowUpPlan addFollowUpPlan;
    @Resource
    private BaseMapper<FollowUpPlan> followUpPlanMapper;

    @Test
    @Order(1)
    void testAdd() throws Exception {
        FollowUpPlanAddRequest request = new FollowUpPlanAddRequest();
        request.setCustomerId("123");
        request.setOpportunityId("12345");
        request.setOwner("admin");
        request.setContactId("123456");
        request.setType("CUSTOMER");
        request.setContent("计划一下");
        request.setEstimatedTime(System.currentTimeMillis());
        request.setModuleFields(List.of(new BaseModuleFieldValue("id", "value")));
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        FollowUpRecord resultData = getResultData(mvcResult, FollowUpRecord.class);
        FollowUpPlan followUpPlan = followUpPlanMapper.selectByPrimaryKey(resultData.getId());
        this.addFollowUpPlan = followUpPlan;
    }


    @Test
    @Order(2)
    void testUpdate() throws Exception {
        FollowUpPlanUpdateRequest request = new FollowUpPlanUpdateRequest();
        request.setId("1234");
        request.setCustomerId("123");
        request.setOpportunityId("12345");
        request.setOwner("admin");
        request.setContactId("1234567");
        request.setType("CUSTOMER");
        request.setContent("计划两下");
        request.setEstimatedTime(System.currentTimeMillis());
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
}
