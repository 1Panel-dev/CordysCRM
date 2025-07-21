package io.cordys.crm.home;

import io.cordys.common.constants.BusinessSearchType;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.home.constants.HomeStatisticPeriod;
import io.cordys.crm.home.dto.request.HomeStatisticSearchRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @Author: jianxing
 * @CreateTime: 2025-07-15  16:24
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HomeStatisticControllerTest extends BaseTest {

    private static final String BASE_PATH = "/home/statistic/";

    protected static final String OPPORTUNITY = "opportunity";
    protected static final String CUSTOMER = "customer";
    protected static final String CLUE = "clue";
    protected static final String CONTACT = "contact";
    protected static final String FOLLOW_RECORD = "follow/record";
    protected static final String FOLLOW_PLAN = "follow/plan";
    protected static final String DEPARTMENT_TREE = "department/tree";

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(0)
    void testGetCustomerStatistic() throws Exception {
        HomeStatisticSearchRequest request = new HomeStatisticSearchRequest();
        request.setSearchType(BusinessSearchType.ALL.name());
        request.setPeriod(HomeStatisticPeriod.THIS_MONTH.name());

        this.requestPostWithOkAndReturn(CUSTOMER, request);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, CUSTOMER, request);
    }

    @Test
    @Order(0)
    void testGetOpportunityStatistic() throws Exception {
        HomeStatisticSearchRequest request = new HomeStatisticSearchRequest();
        request.setSearchType(BusinessSearchType.ALL.name());
        request.setPeriod(HomeStatisticPeriod.THIS_MONTH.name());

        this.requestPostWithOkAndReturn(OPPORTUNITY, request);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, OPPORTUNITY, request);
    }

    @Test
    @Order(0)
    void getContactStatistic() throws Exception {
        HomeStatisticSearchRequest request = new HomeStatisticSearchRequest();
        request.setSearchType(BusinessSearchType.ALL.name());
        request.setPeriod(HomeStatisticPeriod.THIS_MONTH.name());

        this.requestPostWithOkAndReturn(CONTACT, request);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ, CONTACT, request);
    }

    @Test
    @Order(0)
    void testGetClueStatistic() throws Exception {
        HomeStatisticSearchRequest request = new HomeStatisticSearchRequest();
        request.setSearchType(BusinessSearchType.ALL.name());
        request.setPeriod(HomeStatisticPeriod.THIS_MONTH.name());

        this.requestPostWithOkAndReturn(CLUE, request);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_READ, CLUE, request);
    }

    @Test
    @Order(0)
    void testGetFollowUpRecordStatistic() throws Exception {
        HomeStatisticSearchRequest request = new HomeStatisticSearchRequest();
        request.setSearchType(BusinessSearchType.ALL.name());
        request.setPeriod(HomeStatisticPeriod.THIS_MONTH.name());

        this.requestPostWithOkAndReturn(FOLLOW_RECORD, request);

        // 校验权限
        requestPostPermissionsTest(List.of(PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ), FOLLOW_RECORD, request);
    }

    @Test
    @Order(0)
    void testGetFollowUpPlanStatistic() throws Exception {
        HomeStatisticSearchRequest request = new HomeStatisticSearchRequest();
        request.setSearchType(BusinessSearchType.ALL.name());
        request.setPeriod(HomeStatisticPeriod.THIS_MONTH.name());

        this.requestPostWithOkAndReturn(FOLLOW_PLAN, request);

        // 校验权限
        requestPostPermissionsTest(List.of(PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ), FOLLOW_PLAN, request);
    }

    @Test
    @Order(1)
    void getDepartmentTree() throws Exception {
        this.requestGetWithOkAndReturn(DEPARTMENT_TREE);
    }
}
