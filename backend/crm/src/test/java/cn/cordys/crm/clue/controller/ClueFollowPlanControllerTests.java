package cn.cordys.crm.clue.controller;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.follow.constants.FollowUpPlanStatusType;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.dto.request.FollowUpPlanAddRequest;
import cn.cordys.crm.follow.dto.request.FollowUpPlanStatusRequest;
import cn.cordys.crm.follow.dto.request.FollowUpPlanUpdateRequest;
import cn.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import cn.cordys.mybatis.BaseMapper;
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
public class ClueFollowPlanControllerTests extends BaseTest {

    private static final String BASE_PATH = "/lead/follow/plan/";

    private static final String CANCEL_PLAN = "cancel/{0}";

    private static final String STATUS_UPDATE = "status/update";

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
        request.setMethod("1");
        request.setContent("计划一下");
        request.setEstimatedTime(System.currentTimeMillis());
        request.setModuleFields(List.of(new BaseModuleFieldValue("id", "value")));
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        FollowUpRecord resultData = getResultData(mvcResult, FollowUpRecord.class);
        FollowUpPlan followUpPlan = followUpPlanMapper.selectByPrimaryKey(resultData.getId());
        addFollowUpPlan = followUpPlan;
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
        request.setMethod("2");
        request.setType("CUSTOMER");
        request.setContent("计划两下");
        request.setEstimatedTime(System.currentTimeMillis());
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
}
