package cn.cordys.crm.home;

import cn.cordys.common.constants.BusinessSearchType;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.home.dto.request.HomeStatisticBaseSearchRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @Author: jianxing
 * @CreateTime: 2025-07-15  16:24
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HomeStatisticControllerTest extends BaseTest {

    protected static final String OPPORTUNITY = "opportunity";
    protected static final String OPPORTUNITY_SUCCESS = "opportunity/success";
    protected static final String CLUE = "lead";
    protected static final String DEPARTMENT_TREE = "department/tree";
    private static final String BASE_PATH = "/home/statistic/";

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(0)
    void testGetOpportunityStatistic() throws Exception {
        HomeStatisticBaseSearchRequest request = new HomeStatisticBaseSearchRequest();
        request.setSearchType(BusinessSearchType.ALL.name());

        this.requestPostWithOkAndReturn(OPPORTUNITY, request);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, OPPORTUNITY, request);
    }

    @Test
    @Order(0)
    void testGetOpportunitySuccessStatistic() throws Exception {
        HomeStatisticBaseSearchRequest request = new HomeStatisticBaseSearchRequest();
        request.setSearchType(BusinessSearchType.ALL.name());

        this.requestPostWithOkAndReturn(OPPORTUNITY_SUCCESS, request);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, OPPORTUNITY_SUCCESS, request);
    }

    @Test
    @Order(0)
    void testGetClueStatistic() throws Exception {
        HomeStatisticBaseSearchRequest request = new HomeStatisticBaseSearchRequest();
        request.setSearchType(BusinessSearchType.ALL.name());

        this.requestPostWithOkAndReturn(CLUE, request);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_READ, CLUE, request);
    }

    @Test
    @Order(1)
    void getDepartmentTree() throws Exception {
        this.requestGetWithOkAndReturn(DEPARTMENT_TREE);
    }
}
