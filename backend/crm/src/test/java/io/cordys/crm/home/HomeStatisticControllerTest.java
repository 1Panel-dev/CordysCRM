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

/**
 * @Author: jianxing
 * @CreateTime: 2025-07-15  16:24
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HomeStatisticControllerTest extends BaseTest {

    private static final String BASE_PATH = "/home/statistic/";

    protected static final String CUSTOMER = "customer";

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

}
