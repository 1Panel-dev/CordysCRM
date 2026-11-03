package cn.cordys.crm.approval.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.dto.response.ApprovalResubmitNodeResponse;
import cn.cordys.crm.approval.service.ApprovalActionService;
import cn.cordys.security.SessionUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApprovalActionControllerTest {

    @Test
    void resubmitNodesRouteUsesCurrentUserAndOrganizationAndReturnsNodeNumbers() throws Exception {
        ApprovalActionService service = mock(ApprovalActionService.class);
        ApprovalActionController controller = new ApprovalActionController();
        ReflectionTestUtils.setField(controller, "approvalActionService", service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(service.getResubmitNodes("task-1", "user-1", "org-1"))
                .thenReturn(List.of(new ApprovalResubmitNodeResponse("P" + "N052", "法务审核")));
        try (var session = mockStatic(SessionUtils.class, withSettings().mockMaker(MockMakers.INLINE));
                var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            session.when(SessionUtils::getUserId).thenReturn("user-1");
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-1");

            mvc.perform(get("/approval-action/resubmit-nodes/task-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].nodeNumber").value("P" + "N052"))
                    .andExpect(jsonPath("$[0].nodeName").value("法务审核"));

            verify(service).getResubmitNodes("task-1", "user-1", "org-1");
        }
    }
}
