package cn.cordys.crm.approval.controller;

import cn.cordys.crm.approval.dto.response.ResourceApprovalResponse;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.contract.constants.ContractApprovalStatus;
import cn.cordys.security.UserApprovalDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ResourceApprovalControllerTests extends BaseTest {

    private static final String DETAIL = "/detail/{0}";
    private static final String RESOURCE_ID = "approval_resource_test_001";
    private static final String CURRENT_APPROVER_ID = "appr_user_curr";

    @Override
    protected String getBasePath() {
        return "/resource-approve";
    }

    @Sql(
            scripts = {"/dml/init_resource_approval_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Test
    @Order(1)
    void testResourceDetailOnlyCurrentNodeTasks() throws Exception {
        MvcResult mvcResult = requestGetWithOkAndReturn(DETAIL, RESOURCE_ID);
        ResourceApprovalResponse response = getResultData(mvcResult, ResourceApprovalResponse.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(RESOURCE_ID, response.getResourceId());
        Assertions.assertEquals(ContractApprovalStatus.APPROVING.name(), response.getApproveStatus());

        List<UserApprovalDTO> approveUserList = response.getApproveUserList();
        Assertions.assertNotNull(approveUserList);
        Assertions.assertEquals(1, approveUserList.size());

        UserApprovalDTO currentApprover = approveUserList.getFirst();
        Assertions.assertEquals(CURRENT_APPROVER_ID, currentApprover.getId());
        Assertions.assertEquals(ContractApprovalStatus.APPROVED.name(), currentApprover.getApproveResult());
        Assertions.assertEquals("current-node-comment", currentApprover.getApproveReason());
    }
}
