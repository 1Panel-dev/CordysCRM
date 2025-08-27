package cn.cordys.crm.clue.controller;


import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.dto.request.FollowUpRecordAddRequest;
import cn.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import cn.cordys.crm.follow.dto.request.FollowUpRecordUpdateRequest;
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
public class ClueFollowRecordControllerTests extends BaseTest {

    private static final String BASE_PATH = "/clue/follow/record/";

    private static final String POOL_PAGE = "pool/page";

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
        request.setClueId("123456");
        request.setOwner("admin");
        request.setContactId("123456");
        request.setFollowMethod("1");
        request.setType("CUSTOMER");
        request.setContent("跟进一下");
        request.setFollowTime(System.currentTimeMillis());
        request.setModuleFields(List.of(new BaseModuleFieldValue("id", "value")));
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        FollowUpRecord resultData = getResultData(mvcResult, FollowUpRecord.class);
        FollowUpRecord followUpRecord = followUpRecordMapper.selectByPrimaryKey(resultData.getId());
        addFollowUpRecord = followUpRecord;
    }


    @Test
    @Order(2)
    void testUpdate() throws Exception {
        FollowUpRecordUpdateRequest request = new FollowUpRecordUpdateRequest();
        request.setId("1234");
        request.setCustomerId("123");
        request.setOpportunityId("12345");
        request.setClueId("123456");
        request.setOwner("admin");
        request.setFollowMethod("2");
        request.setContactId("1234567");
        request.setType("CUSTOMER");
        request.setContent("跟进两下");
        request.setModuleFields(List.of(new BaseModuleFieldValue("id", "value")));
        this.requestPost(DEFAULT_UPDATE, request);

        request.setId(addFollowUpRecord.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, request);
    }

    @Test
    @Order(3)
    void testList() throws Exception {
        FollowUpRecordPageRequest request = new FollowUpRecordPageRequest();
        request.setSourceId("123456");
        request.setCurrent(1);
        request.setPageSize(10);
        this.requestPost(DEFAULT_PAGE, request);
    }

    @Test
    @Order(4)
    void testGet() throws Exception {
        this.requestGet(DEFAULT_GET, addFollowUpRecord.getId());
    }


    @Test
    @Order(3)
    void testPoolList() throws Exception {
        FollowUpRecordPageRequest request = new FollowUpRecordPageRequest();
        request.setSourceId("123456");
        request.setCurrent(1);
        request.setPageSize(10);
        this.requestPost(POOL_PAGE, request);
    }

    @Test
    @Order(5)
    void testDeleteRecord() throws Exception {
        this.requestGetWithOk(DEFAULT_DELETE, addFollowUpRecord.getId());
    }
}
