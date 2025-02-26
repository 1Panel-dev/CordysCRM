package io.cordys.crm.customer.controller;


import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.follow.dto.request.FollowUpRecordAddRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomerFollowRecordControllerTests extends BaseTest {

    private static final String BASE_PATH = "/customer/follow/record/";

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(1)
    void testAdd() throws Exception {
        FollowUpRecordAddRequest request = new FollowUpRecordAddRequest();
        request.setCustomerId("123");
        request.setOpportunityId("12345");
        request.setOwner("admin");
        request.setContactId("123456");
        request.setType("CUSTOMER");
        request.setModuleFields(List.of(new BaseModuleFieldValue("id", "value")));
        this.requestPostWithOk(DEFAULT_ADD, request);
    }
}
