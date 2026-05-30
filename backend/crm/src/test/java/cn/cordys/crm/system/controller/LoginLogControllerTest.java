package cn.cordys.crm.system.controller;

import cn.cordys.common.dto.SortRequest;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.dto.request.LoginLogRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginLogControllerTest extends BaseTest {
    public static final String LOGIN_LOG_LIST = "/login/log/list";

    private LoginLogRequest buildRequest() {
        LoginLogRequest request = new LoginLogRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setStartTime(1735808851000L);
        request.setEndTime(1735890402193L);
        return request;
    }

    @Test
    @Order(2)
    public void operationLogLoginList() throws Exception {
        LoginLogRequest request = buildRequest();
        this.requestPost(LOGIN_LOG_LIST, request).andExpect(status().isOk());
        request.setStartTime(1735890402193L);
        request.setEndTime(1735808851000L);
        request.setSort(new SortRequest("id", "desc"));
        this.requestPost(LOGIN_LOG_LIST, request);
    }
}
