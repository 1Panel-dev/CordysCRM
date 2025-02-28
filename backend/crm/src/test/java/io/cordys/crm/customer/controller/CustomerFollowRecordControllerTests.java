package io.cordys.crm.customer.controller;


import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.follow.domain.FollowUpRecord;
import io.cordys.crm.follow.dto.request.FollowUpRecordAddRequest;
import io.cordys.crm.follow.dto.request.FollowUpRecordUpdateRequest;
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
public class CustomerFollowRecordControllerTests extends BaseTest {

    private static final String BASE_PATH = "/customer/follow/record/";

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    private static FollowUpRecord addFollowUpRecord;
    @Resource
    private BaseMapper<FollowUpRecord> followUpRecordMapper;

    @Test
    @Order(1)
    void testAdd() throws Exception {
        FollowUpRecordAddRequest request = new FollowUpRecordAddRequest();
        request.setCustomerId("123");
        request.setOpportunityId("12345");
        request.setOwner("admin");
        request.setContactId("123456");
        request.setType("CUSTOMER");
        request.setContent("跟进一下");
        request.setModuleFields(List.of(new BaseModuleFieldValue("id", "value")));
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        FollowUpRecord resultData = getResultData(mvcResult, FollowUpRecord.class);
        FollowUpRecord followUpRecord = followUpRecordMapper.selectByPrimaryKey(resultData.getId());
        this.addFollowUpRecord = followUpRecord;
    }


    @Test
    @Order(2)
    void testUpdate() throws Exception {
        FollowUpRecordUpdateRequest request = new FollowUpRecordUpdateRequest();
        request.setId("1234");
        request.setCustomerId("123");
        request.setOpportunityId("12345");
        request.setOwner("admin");
        request.setContactId("1234567");
        request.setType("CUSTOMER");
        request.setContent("跟进两下");
        request.setModuleFields(List.of(new BaseModuleFieldValue("id", "value")));
        this.requestPost(DEFAULT_UPDATE, request);

        request.setId(addFollowUpRecord.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, request);
    }
}
