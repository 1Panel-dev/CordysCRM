package cn.cordys.crm.approval.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.service.ApprovalCommentService;
import cn.cordys.crm.approval.service.ApprovalInstanceService;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import cn.cordys.security.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockMakers;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ApprovalResourceControllerTest {

    private ApprovalResourceController controller;
    private ApprovalResourceService resourceService;
    private ApprovalInstanceService instanceService;
    private ApprovalCommentService commentService;

    @BeforeEach
    void setUp() {
        controller = new ApprovalResourceController();
        resourceService = mock(ApprovalResourceService.class);
        instanceService = mock(ApprovalInstanceService.class);
        commentService = mock(ApprovalCommentService.class);
        ReflectionTestUtils.setField(controller, "approvalResourceService", resourceService);
        ReflectionTestUtils.setField(controller, "approvalInstanceService", instanceService);
        ReflectionTestUtils.setField(controller, "approvalCommentService", commentService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void recordDetailIncludesCurrentInstancesCommentPermission(boolean commentable) {
        ApprovalInstanceDetail detail = new ApprovalInstanceDetail();
        detail.setId("instance-1");
        when(instanceService.getLatestApprovalInstanceDetail("resource-1", "org-1")).thenReturn(detail);
        when(commentService.hasCommentPermission("instance-1", "user-1", "org-1")).thenReturn(commentable);
        try (var session = mockStatic(SessionUtils.class, withSettings().mockMaker(MockMakers.INLINE));
             var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            session.when(SessionUtils::getUserId).thenReturn("user-1");
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-1");

            assertSame(detail, controller.getRecordDetail("resource-1"));

            assertEquals(commentable, detail.isCommentable());
            verify(resourceService).checkViewPermission("resource-1");
            verify(commentService).hasCommentPermission("instance-1", "user-1", "org-1");
        }
    }

    @Test
    void missingApprovalInstanceDoesNotCheckCommentPermission() {
        try (var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-1");

            assertNull(controller.getRecordDetail("resource-1"));

            verify(resourceService).checkViewPermission("resource-1");
            verify(instanceService).getLatestApprovalInstanceDetail("resource-1", "org-1");
            verifyNoInteractions(commentService);
        }
    }
}
