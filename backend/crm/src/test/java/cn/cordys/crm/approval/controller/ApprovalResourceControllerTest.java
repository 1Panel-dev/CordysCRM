package cn.cordys.crm.approval.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.dto.response.ResourceApprovalResponse;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
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
    @ValueSource(booleans = { false, true })
    void recordDetailIncludesResourceCommentPermission(boolean commentable) {
        ApprovalInstanceDetail detail = new ApprovalInstanceDetail();
        detail.setId("instance-1");
        when(instanceService.getLatestApprovalInstanceDetail("resource-1", "org-1")).thenReturn(detail);
        when(commentService.hasCommentPermission("resource-1", "user-1", "org-1")).thenReturn(commentable);
        try (var session = mockStatic(SessionUtils.class, withSettings().mockMaker(MockMakers.INLINE));
                var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            session.when(SessionUtils::getUserId).thenReturn("user-1");
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-1");

            assertSame(detail, controller.getRecordDetail("resource-1", null));

            assertEquals(commentable, detail.isCommentable());
            verify(resourceService).checkViewPermission("resource-1");
            verify(commentService).hasCommentPermission("resource-1", "user-1", "org-1");
        }
    }

    @Test
    void missingApprovalInstanceDoesNotCheckCommentPermission() {
        try (var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-1");

            assertNull(controller.getRecordDetail("resource-1", null));

            verify(resourceService).checkViewPermission("resource-1");
            verify(instanceService).getLatestApprovalInstanceDetail("resource-1", "org-1");
            verifyNoInteractions(commentService);
        }
    }

    @Test
    void mentionContextOpensResourcesLatestInstanceWithResourceCommentPermission() {
        ApprovalInstance latest = new ApprovalInstance();
        latest.setId("instance-latest");
        latest.setResourceId("resource-1");
        ApprovalInstanceDetail detail = new ApprovalInstanceDetail();
        detail.setId(latest.getId());
        when(commentService.getMentionInstance("mention-1", "resource-1", "user-1", "org-1")).thenReturn(latest);
        when(instanceService.getApprovalInstanceDetail(latest, "org-1")).thenReturn(detail);
        when(commentService.hasCommentPermission("resource-1", "user-1", "org-1")).thenReturn(true);
        try (var session = mockStatic(SessionUtils.class, withSettings().mockMaker(MockMakers.INLINE));
             var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            session.when(SessionUtils::getUserId).thenReturn("user-1");
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-1");

            assertSame(detail, controller.getRecordDetail("resource-1", "mention-1"));

            assertTrue(detail.isCommentable());
            verify(commentService).getMentionInstance("mention-1", "resource-1", "user-1", "org-1");
            verify(instanceService).getApprovalInstanceDetail(latest, "org-1");
            verify(instanceService, never()).getLatestApprovalInstanceDetail(anyString(), anyString());
            verifyNoInteractions(resourceService);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void simpleDetailUsesMentionContextOnlyWhenProvided(boolean mentionContext) {
        ResourceApprovalResponse detail = new ResourceApprovalResponse();
        when(resourceService.resourceDetail("resource-1")).thenReturn(detail);
        if (mentionContext) {
            when(commentService.getMentionInstance("mention-1", "resource-1", "user-1", "org-1")).thenReturn(new ApprovalInstance());
        }
        try (var session = mockStatic(SessionUtils.class, withSettings().mockMaker(MockMakers.INLINE));
             var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            session.when(SessionUtils::getUserId).thenReturn("user-1");
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-1");

            assertSame(detail, controller.resourceDetail("resource-1", mentionContext ? "mention-1" : null));

            if (mentionContext) {
                verify(commentService).getMentionInstance("mention-1", "resource-1", "user-1", "org-1");
                verify(resourceService, never()).checkViewPermission(anyString());
            } else {
                verify(resourceService).checkViewPermission("resource-1");
                verifyNoInteractions(commentService);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void invalidMentionContextCannotOpenEitherApprovalDetail(boolean recordDetail) {
        when(commentService.getMentionInstance("mention-invalid", "resource-1", "user-1", "org-1"))
                .thenThrow(new GenericException("无权查看"));
        try (var session = mockStatic(SessionUtils.class, withSettings().mockMaker(MockMakers.INLINE));
             var organization = mockStatic(OrganizationContext.class, withSettings().mockMaker(MockMakers.INLINE))) {
            session.when(SessionUtils::getUserId).thenReturn("user-1");
            organization.when(OrganizationContext::getOrganizationId).thenReturn("org-1");

            assertThrows(GenericException.class, () -> {
                if (recordDetail) {
                    controller.getRecordDetail("resource-1", "mention-invalid");
                } else {
                    controller.resourceDetail("resource-1", "mention-invalid");
                }
            });

            verifyNoInteractions(instanceService, resourceService);
        }
    }
}
