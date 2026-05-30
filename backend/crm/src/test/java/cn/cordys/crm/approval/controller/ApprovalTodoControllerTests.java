package cn.cordys.crm.approval.controller;

import cn.cordys.common.pager.Pager;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.response.ApprovalTodoCountResponse;
import cn.cordys.crm.approval.dto.response.ApprovalTodoItemResponse;
import cn.cordys.crm.approval.service.ApprovalTodoService;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ApprovalTodoControllerTests extends BaseTest {

    private static final String TODO_LIST = "/pending/page";
    private static final String PROCESSED_PAGE = "/processed/page";
    private static final String INITIATED_PAGE = "/initiated/page";
    private static final String CC_PAGE = "/cc/page";
    private static final String PENDING_COUNT = "/pending/count";

    @Resource
    private ApprovalTodoService approvalTodoService;
    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;

    @Override
    protected String getBasePath() {
        return "/approval-todo";
    }

    @Sql(
            scripts = {"/dml/init_approval_todo_list_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )

    @Test
    @Order(1)
    void testTodoListPageWithAllType() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("current", 1);
        request.put("pageSize", 10);
        request.put("resourceType", "ALL");

        MvcResult mvcResult = requestPostWithOkAndReturn(TODO_LIST, request);
        Pager<List<ApprovalTodoItemResponse>> pager = getPageResult(mvcResult, ApprovalTodoItemResponse.class);
        Assertions.assertNotNull(pager);
        Assertions.assertEquals(3, pager.getTotal());
        Assertions.assertEquals(3, pager.getList().size());
        Assertions.assertTrue(pager.getList().stream().allMatch(item -> StringUtils.equals("APPROVING", item.getApprovalOperation())));
    }

    @Sql(
            scripts = {"/dml/init_approval_todo_list_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Test
    @Order(2)
    void testTodoListPageWithContractType() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("current", 1);
        request.put("pageSize", 10);
        request.put("resourceType", "CONTRACT");

        MvcResult mvcResult = requestPostWithOkAndReturn(TODO_LIST, request);
        Pager<List<ApprovalTodoItemResponse>> pager = getPageResult(mvcResult, ApprovalTodoItemResponse.class);

        Assertions.assertNotNull(pager);
        Assertions.assertEquals(2, pager.getTotal());
        Assertions.assertEquals(2, pager.getList().size());
        Assertions.assertEquals("CONTRACT", pager.getList().getFirst().getResourceType());
    }

    @Sql(
            scripts = {"/dml/init_approval_todo_processed_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Test
    @Order(3)
    void testProcessedPage() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("current", 1);
        request.put("pageSize", 10);

        MvcResult mvcResult = requestPostWithOkAndReturn(PROCESSED_PAGE, request);
        Pager<List<ApprovalTodoItemResponse>> pager = getPageResult(mvcResult, ApprovalTodoItemResponse.class);

        Assertions.assertNotNull(pager);
        Assertions.assertEquals(4, pager.getTotal());
        Assertions.assertEquals(4, pager.getList().size());
        Assertions.assertTrue(pager.getList().stream().anyMatch(item -> StringUtils.equals("todo_processed_resource_001", item.getResourceId())));
    }

    @Sql(
            scripts = {"/dml/init_approval_todo_cc_initiated_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Test
    @Order(4)
    void testInitiatedPage() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("current", 1);
        request.put("pageSize", 10);

        MvcResult mvcResult = requestPostWithOkAndReturn(INITIATED_PAGE, request);
        Pager<List<ApprovalTodoItemResponse>> pager = getPageResult(mvcResult, ApprovalTodoItemResponse.class);

        Assertions.assertNotNull(pager);
    }

    @Sql(
            scripts = {"/dml/init_approval_todo_cc_initiated_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Test
    @Order(5)
    void testCcPage() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("current", 1);
        request.put("pageSize", 10);

        MvcResult mvcResult = requestPostWithOkAndReturn(CC_PAGE, request);
        Pager<List<ApprovalTodoItemResponse>> pager = getPageResult(mvcResult, ApprovalTodoItemResponse.class);

        Assertions.assertNotNull(pager);
        Assertions.assertEquals(2, pager.getTotal());
        Assertions.assertEquals(2, pager.getList().size());
        Assertions.assertTrue(pager.getList().stream().allMatch(item -> StringUtils.equals("READ", item.getApprovalOperation())));
    }

    @Sql(
            scripts = {"/dml/init_approval_todo_list_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Test
    @Order(6)
    void testPendingCount() throws Exception {
        MvcResult mvcResult = requestGetWithOkAndReturn(PENDING_COUNT);
        ApprovalTodoCountResponse response = getResultData(mvcResult, ApprovalTodoCountResponse.class);

        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.getTotal() >= 0);
        Assertions.assertTrue(response.getQuotation() >= 0);
        Assertions.assertTrue(response.getContract() >= 0);
        Assertions.assertTrue(response.getOrder() >= 0);
        Assertions.assertTrue(response.getInvoice() >= 0);
        Assertions.assertEquals(response.getTotal(), response.getQuotation() + response.getContract() + response.getOrder() + response.getInvoice());
    }

    @Sql(
            scripts = {"/dml/init_approval_todo_list_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Test
    @Order(7)
    void testDeleteApprovalTaskByInstanceId() {
        approvalTodoService.deleteApprovalTaskByInstanceId("todo_list_inst_contract");

        List<ApprovalTask> remain = approvalTaskMapper.selectListByLambda(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, "todo_list_inst_contract"));
        Assertions.assertTrue(remain.isEmpty());
    }

    @Sql(
            scripts = {"/dml/init_approval_todo_list_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Test
    @Order(8)
    void testDeleteApprovalTaskByInstanceIds() {
        approvalTodoService.deleteApprovalTaskByInstanceIds(List.of("todo_list_inst_contract", "todo_list_inst_quote"));

        List<ApprovalTask> remain = approvalTaskMapper.selectListByLambda(new LambdaQueryWrapper<ApprovalTask>()
                .in(ApprovalTask::getInstanceId, List.of("todo_list_inst_contract", "todo_list_inst_quote")));
        Assertions.assertTrue(remain.isEmpty());
    }
}
