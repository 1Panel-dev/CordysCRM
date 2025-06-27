package io.cordys.crm.system.controller;

import io.cordys.common.dto.SortRequest;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.dto.request.OperationLogRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OperationLogControllerTest extends BaseTest {
    public static final String OPERATION_LOG_LIST = "/operation/log/list";
    public static final String OPERATION_LOG_DETAIL = "/operation/log/detail/";

    @Sql(scripts = {"/dml/init_operation_log_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Test
    @Order(1)
    public void operationLogList() throws Exception {
        OperationLogRequest request = buildRequest();
        this.requestPost(OPERATION_LOG_LIST, request).andExpect(status().isOk());
        request.setStartTime(1735890402193L);
        request.setEndTime(1735808851000L);
        request.setSort(new SortRequest("id", "desc"));
        this.requestPost(OPERATION_LOG_LIST, request);
    }

    private OperationLogRequest buildRequest() {
        OperationLogRequest request = new OperationLogRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setStartTime(1735808851000L);
        request.setEndTime(1735890402193L);
        return request;
    }

    @Test
    @Order(3)
    public void operationLogDetail() throws Exception {
        this.requestGet(OPERATION_LOG_DETAIL + "123");
    }
}
