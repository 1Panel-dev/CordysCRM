package io.cordys.crm.opportunity.controller;

import io.cordys.common.dto.condition.FilterCondition;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.dto.request.UserViewAddRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OpportunityUserViewControllerTest extends BaseTest {

    private static final String BASE_PATH = "/opportunity/view/";

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(1)
    void testAdd() throws Exception {

        UserViewAddRequest request = new UserViewAddRequest();
        request.setName("测试视图11");
        request.setSearchMode("AND");
        request.setResourceType("OPPORTUNITY");
        request.setConditions(buildConditions());
        this.requestPost(DEFAULT_ADD, request);
    }

    private List<FilterCondition> buildConditions() {
        List<FilterCondition> conditions = new ArrayList<>();

        FilterCondition condition1 = new FilterCondition();
        condition1.setName("123123312312");
        condition1.setValue(null);
        condition1.setOperator("EMPTY");
        condition1.setMultipleValue(false);
        condition1.setType("LOCATION");

        FilterCondition condition2 = new FilterCondition();
        condition2.setName("1231233333");
        condition2.setValue("[\n" +
                "                \"2\"\n" +
                "            ]");
        condition2.setOperator("IN");
        condition2.setMultipleValue(false);
        condition2.setType("1231");

        conditions.add(condition1);
        conditions.add(condition2);
        return conditions;
    }
}
