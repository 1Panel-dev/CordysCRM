package cn.cordys.crm.approval.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.service.ApprovalCommentService;
import cn.cordys.crm.approval.service.ApprovalHistoryService;
import cn.cordys.crm.approval.service.ApprovalInstanceService;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import cn.cordys.security.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApprovalResourceHistoryControllerTest {

    private ApprovalHistoryService historyService;
    private ApprovalInstanceService instanceService;
    private ApprovalCommentService commentService;
    private ApprovalResourceService resourceService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        historyService = mock(ApprovalHistoryService.class);
        instanceService = mock(ApprovalInstanceService.class);
        commentService = mock(ApprovalCommentService.class);
        resourceService = mock(ApprovalResourceService.class);
        ApprovalResourceController controller = new ApprovalResourceController();
        ReflectionTestUtils.setField(controller, "approvalHistoryService", historyService);
        ReflectionTestUtils.setField(controller, "approvalInstanceService", instanceService);
        ReflectionTestUtils.setField(controller, "approvalCommentService", commentService);
        ReflectionTestUtils.setField(controller, "approvalResourceService", resourceService);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void historyListRouteIsRemoved() throws Exception {
        mvc.perform(get("/approval-resource/instances/resource-1"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(historyService, instanceService, commentService, resourceService);
    }

    @Test
    void historyDetailsUseResourceAndKeepOldestToNewestOrder() throws Exception {
        ApprovalInstanceDetail oldest = new ApprovalInstanceDetail();
        oldest.setId("history-1");
        oldest.setNodes(List.of());
        ApprovalInstanceDetail newest = new ApprovalInstanceDetail();
        newest.setId("history-2");
        newest.setNodes(List.of());
        when(historyService.getHistoryInstanceDetail("resource-1", "user-1", "org-1"))
                .thenReturn(List.of(oldest, newest));
        try (var session = mockStatic(SessionUtils.class, withSettings().mockMaker(MockMakers.INLINE));
                var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            session.when(SessionUtils::getUserId).thenReturn("user-1");
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-1");

            mvc.perform(get("/approval-resource/instance-detail/resource-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value("history-1"))
                    .andExpect(jsonPath("$[0].nodes").isEmpty())
                    .andExpect(jsonPath("$[1].id").value("history-2"))
                    .andExpect(jsonPath("$[1].nodes").isEmpty());

            verify(historyService).getHistoryInstanceDetail("resource-1", "user-1", "org-1");
            verifyNoInteractions(instanceService, commentService, resourceService);
        }
    }

    @Test
    void historyDetailsReturnEmptyArrayWhenNoHistoryExists() throws Exception {
        when(historyService.getHistoryInstanceDetail("resource-1", "user-1", "org-1")).thenReturn(List.of());
        try (var session = mockStatic(SessionUtils.class, withSettings().mockMaker(MockMakers.INLINE));
                var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            session.when(SessionUtils::getUserId).thenReturn("user-1");
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-1");

            mvc.perform(get("/approval-resource/instance-detail/resource-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            verify(historyService).getHistoryInstanceDetail("resource-1", "user-1", "org-1");
            verifyNoInteractions(instanceService, commentService, resourceService);
        }
    }

    @Test
    void existingDetailRouteKeepsLatestInstanceAndCommentPermission() throws Exception {
        ApprovalInstanceDetail detail = new ApprovalInstanceDetail();
        detail.setId("current-instance");
        when(instanceService.getLatestApprovalInstanceDetail("resource-1", "org-1")).thenReturn(detail);
        when(commentService.hasCommentPermission("current-instance", "user-1", "org-1")).thenReturn(true);
        try (var session = mockStatic(SessionUtils.class, withSettings().mockMaker(MockMakers.INLINE));
                var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            session.when(SessionUtils::getUserId).thenReturn("user-1");
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-1");

            mvc.perform(get("/approval-resource/detail/resource-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isMap())
                    .andExpect(jsonPath("$.id").value("current-instance"))
                    .andExpect(jsonPath("$.commentable").value(true));

            verify(resourceService).checkViewPermission("resource-1");
            verify(instanceService).getLatestApprovalInstanceDetail("resource-1", "org-1");
            verify(commentService).hasCommentPermission("current-instance", "user-1", "org-1");
            verifyNoInteractions(historyService);
        }
    }

    @Test
    void existingDetailRouteKeepsCommentMentionContext() throws Exception {
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId("mentioned-instance");
        ApprovalInstanceDetail detail = new ApprovalInstanceDetail();
        detail.setId(instance.getId());
        when(commentService.getMentionInstance("mention-1", "resource-1", "user-1", "org-1"))
                .thenReturn(instance);
        when(instanceService.getApprovalInstanceDetail(instance, "org-1")).thenReturn(detail);
        when(commentService.hasCommentPermission("mentioned-instance", "user-1", "org-1")).thenReturn(true);
        try (var session = mockStatic(SessionUtils.class, withSettings().mockMaker(MockMakers.INLINE));
                var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            session.when(SessionUtils::getUserId).thenReturn("user-1");
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-1");

            mvc.perform(get("/approval-resource/detail/resource-1").param("commentMentionId", "mention-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("mentioned-instance"))
                    .andExpect(jsonPath("$.commentable").value(true));

            verify(commentService).getMentionInstance("mention-1", "resource-1", "user-1", "org-1");
            verifyNoInteractions(historyService, resourceService);
        }
    }
}
