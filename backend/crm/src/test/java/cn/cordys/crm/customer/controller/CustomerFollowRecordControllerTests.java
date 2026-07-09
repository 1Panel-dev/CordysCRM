package cn.cordys.crm.customer.controller;


import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.customer.domain.Customer;
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
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomerFollowRecordControllerTests extends BaseTest {

    private static final String BASE_PATH = "/account/follow/record/";

    private static final String POOL_PAGE = "pool/page";
    private static FollowUpRecord addFollowUpRecord;
    @Resource
    private BaseMapper<FollowUpRecord> followUpRecordMapper;
    @Resource
    private BaseMapper<Customer> customerMapper;

    public static String customerId;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(1)
    void testAdd() throws Exception {
        insertCustomer();
        FollowUpRecordAddRequest request = new FollowUpRecordAddRequest();
        request.setCustomerId(customerId);
        request.setOpportunityId("12345");
        request.setOwner("admin");
        request.setContactId("123456");
        request.setFollowMethod("1");
        request.setType("CUSTOMER");
        request.setContent("跟进一下");
        request.setFollowTime(System.currentTimeMillis());
        request.setModuleFields(List.of(new BaseModuleFieldValue("id", "value")));
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        FollowUpRecord resultData = getResultData(mvcResult, FollowUpRecord.class);
        addFollowUpRecord = followUpRecordMapper.selectByPrimaryKey(resultData.getId());
    }

    private Customer insertCustomer() {
        Customer customer = new Customer();
        customer.setId(IDGenerator.nextStr());
        customer.setName(UUID.randomUUID().toString());
        customer.setOwner(InternalUser.ADMIN.getValue());
        customer.setCollectionTime(System.currentTimeMillis());
        customer.setPoolId("testPoolId");
        customer.setInSharedPool(false);
        customer.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        customer.setCreateTime(System.currentTimeMillis());
        customer.setCreateUser(InternalUser.ADMIN.getValue());
        customer.setUpdateTime(System.currentTimeMillis());
        customer.setUpdateUser(InternalUser.ADMIN.getValue());
        customerMapper.insert(customer);
        customerId = customer.getId();
        return customer;
    }

    @Test
    @Order(2)
    void testUpdate() throws Exception {
        FollowUpRecordUpdateRequest request = new FollowUpRecordUpdateRequest();
        request.setId("1234");
        request.setCustomerId(customerId);
        request.setOpportunityId("12345");
        request.setOwner("admin");
        request.setContactId("1234567");
        request.setFollowMethod("2");
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
        request.setSourceId(customerId);
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
        request.setSourceId(customerId);
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
