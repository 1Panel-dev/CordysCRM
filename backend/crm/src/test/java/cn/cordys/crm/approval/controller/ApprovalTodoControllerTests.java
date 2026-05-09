package cn.cordys.crm.approval.controller;

import cn.cordys.common.pager.Pager;
import cn.cordys.crm.approval.dto.response.ApprovalTodoItemResponse;
import cn.cordys.crm.base.BaseTest;
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
        Assertions.assertEquals(2, pager.getList().size());
        Assertions.assertTrue(pager.getList().stream().allMatch(item -> StringUtils.equals("PENDING", item.getApprovalOperation())));
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
        Assertions.assertEquals(1, pager.getList().size());
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
        Assertions.assertEquals(1, pager.getTotal());
        Assertions.assertEquals(1, pager.getList().size());

        ApprovalTodoItemResponse item = pager.getList().getFirst();
        Assertions.assertEquals("todo_processed_resource_001", item.getResourceId());
        Assertions.assertEquals("CONTRACT", item.getResourceType());
        Assertions.assertTrue(StringUtils.isNotBlank(item.getApplicant()));
        Assertions.assertEquals("APPROVED", item.getApprovalOperation());
        Assertions.assertEquals("APPROVED", item.getDataResult());
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
        Assertions.assertEquals(2, pager.getTotal());
        Assertions.assertEquals(2, pager.getList().size());
        Assertions.assertTrue(pager.getList().stream().allMatch(item -> StringUtils.isNotBlank(item.getApplicant())));
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
}
