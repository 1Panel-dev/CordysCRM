package cn.cordys.crm.approval.service;

import cn.cordys.crm.approval.domain.ApprovalComment;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.mapper.ExtApprovalCommentMapper;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.notice.common.NoticeSendContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApprovalCommentNoticeServiceTest {

    private static final String MODULE = NotificationConstants.Module.APPROVAL;

    private ApprovalCommentNoticeService service;
    private ExtApprovalCommentMapper commentMapper;
    private ExtApprovalInstanceMapper instanceMapper;
    private CommonNoticeSendService noticeSender;
    private ApprovalInstance instance;
    private final List<NoticeSendContext> sent = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new ApprovalCommentNoticeService();
        commentMapper = mock(ExtApprovalCommentMapper.class);
        instanceMapper = mock(ExtApprovalInstanceMapper.class);
        noticeSender = mock(CommonNoticeSendService.class);
        ReflectionTestUtils.setField(service, "extApprovalCommentMapper", commentMapper);
        ReflectionTestUtils.setField(service, "extApprovalInstanceMapper", instanceMapper);
        ReflectionTestUtils.setField(service, "commonNoticeSendService", noticeSender);
        doAnswer(invocation -> {
            sent.add(invocation.getArgument(0));
            return null;
        }).when(noticeSender).sendNoticeByConfig(any(NoticeSendContext.class));
        instance = new ApprovalInstance();
        instance.setId("instance-a");
        instance.setType("contract");
        instance.setResourceName("合同甲");
        instance.setApprovalStatus("APPROVED");
    }

    @Test
    void topLevelCommentNotifiesOwnerWithBusinessConfig() {
        stubInstance();
        when(instanceMapper.getResourceOwner("contract", "resource-a", "org-a")).thenReturn("owner");

        service.sendNotice(comment("comment-a"), List.of(), true);

        assertEquals(1, sent.size());
        NoticeSendContext context = sent.get(0);
        assertEquals(MODULE, context.getTaskType());
        assertEquals(NotificationConstants.Event.APPROVAL_COMMENT, context.getAction());
        assertEquals(Map.of("owner", "owner"), context.getNewValues());
        assertEquals("resource-a", context.getResourceId());
        assertEquals("comment-a", context.getParamMap().get("resourceId"));
        assertEquals("CONTRACT", context.getParamMap().get("resourceType"));
        assertEquals("合同甲", context.getParamMap().get("name"));
        assertEquals("editor", context.getOperatorId());
        assertEquals("org-a", context.getOrganizationId());
        assertTrue(context.isExcludeSelf());
        assertTrue(context.isEventOnly());
    }

    @Test
    void replyOnAddNotifiesOwnerAndReplyTarget() {
        stubInstance();
        when(instanceMapper.getResourceOwner("contract", "resource-a", "org-a")).thenReturn("owner");
        ApprovalComment comment = comment("comment-a");
        comment.setParentId("parent-a");
        comment.setReplyToUserId("replied");

        service.sendNotice(comment, List.of(), true);

        assertEquals(2, sent.size());
        find(NotificationConstants.Event.APPROVAL_COMMENT);
        assertEquals("replied", find(NotificationConstants.Event.APPROVAL_COMMENT_REPALY).getReplyToUserId());
    }

    @Test
    void mentionNotifiesDistinctMentionedUsers() {
        instance.setType("quotation");
        stubInstance();

        service.sendNotice(comment("comment-a"), List.of("m1", "m2", "m1"), true);

        assertEquals(1, sent.size());
        assertEquals(List.of("m1", "m2"), find(NotificationConstants.Event.APPROVAL_COMMENT_MENTIONED)
                .getMentionedUserIds());
        // 报价单没有业务负责人，不应查询负责人
        verifyNoInteractions(instanceMapper);
    }

    @Test
    void ownerAlsoMentionedReceivesOnlyMentionNotice() {
        stubInstance();
        when(instanceMapper.getResourceOwner("contract", "resource-a", "org-a")).thenReturn("owner");

        service.sendNotice(comment("comment-a"), List.of("owner"), true);

        assertEquals(1, sent.size());
        assertEquals(NotificationConstants.Event.APPROVAL_COMMENT_MENTIONED, sent.get(0).getAction());
        assertEquals(List.of("owner"), sent.get(0).getMentionedUserIds());
    }

    @Test
    void editNotifiesOnlyNewlyAddedMentions() {
        stubInstance();
        ApprovalComment comment = comment("comment-a");
        comment.setParentId("parent-a");
        comment.setReplyToUserId("replied");

        service.sendNotice(comment, List.of("newMention"), false);

        assertEquals(1, sent.size());
        assertEquals(NotificationConstants.Event.APPROVAL_COMMENT_MENTIONED, sent.get(0).getAction());
        assertEquals(List.of("newMention"), sent.get(0).getMentionedUserIds());
        // 编辑不重复提醒负责人
        verifyNoInteractions(instanceMapper);
    }

    @Test
    void editWithoutNewMentionSendsNothing() {
        service.sendNotice(comment("comment-a"), List.of(), false);

        verifyNoInteractions(commentMapper);
        verifyNoInteractions(noticeSender);
    }

    @Test
    void noOwnerAndNoMentionSendsNothingOnAdd() {
        stubInstance();

        service.sendNotice(comment("comment-a"), List.of(), true);

        verify(instanceMapper).getResourceOwner("contract", "resource-a", "org-a");
        verifyNoInteractions(noticeSender);
    }

    @Test
    void missingInstanceSendsNothing() {
        service.sendNotice(comment("comment-a"), List.of("m1"), true);

        verify(noticeSender, never()).sendNoticeByConfig(any(NoticeSendContext.class));
    }

    @Test
    void customFormUsesCustomTableAndApprovalResourceType() {
        instance.setType("custom-form-a");
        stubInstance();
        when(instanceMapper.getResourceOwner("custom_form_data", "resource-a", "org-a")).thenReturn("owner");

        service.sendNotice(comment("comment-a"), List.of(), true);

        assertEquals(1, sent.size());
        assertEquals("APPROVAL", sent.get(0).getParamMap().get("resourceType"));
        verify(instanceMapper).getResourceOwner("custom_form_data", "resource-a", "org-a");
    }

    @ParameterizedTest
    @CsvSource({"clue,CLUE", "customer,CUSTOMER", "opportunity,OPPORTUNITY", "quotation,OPPORTUNITY",
            "contract,CONTRACT", "invoice,CONTRACT", "order,ORDER", "custom-form-a,APPROVAL"})
    void resourceTypeMapsToBusinessModule(String formType, String resourceType) {
        instance.setType(formType);
        stubInstance();

        service.sendNotice(comment("comment-a"), List.of("receiver"), true);

        assertEquals(1, sent.size());
        assertEquals(resourceType, sent.get(0).getParamMap().get("resourceType"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"contract", "quotation"})
    void resourcesWithoutResolvedOwnerKeepMentionNotice(String formType) {
        instance.setType(formType);
        stubInstance();

        service.sendNotice(comment("comment-a"), List.of("receiver"), true);

        assertEquals(1, sent.size());
        assertEquals(NotificationConstants.Event.APPROVAL_COMMENT_MENTIONED, sent.get(0).getAction());
    }

    @Test
    void notificationFailureDoesNotFailCommittedComment() {
        when(commentMapper.selectLatestInstance("resource-a", "org-a"))
                .thenThrow(new IllegalStateException("通知配置读取失败"));

        assertDoesNotThrow(() -> service.sendNotice(comment("comment-a"), List.of("receiver"), true));

        verifyNoInteractions(noticeSender);
    }

    private void stubInstance() {
        when(commentMapper.selectLatestInstance("resource-a", "org-a")).thenReturn(instance);
    }

    private NoticeSendContext find(String action) {
        return sent.stream()
                .filter(context -> action.equals(context.getAction()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到事件为 " + action + " 的通知"));
    }

    private ApprovalComment comment(String id) {
        ApprovalComment comment = new ApprovalComment();
        comment.setId(id);
        comment.setResourceId("resource-a");
        comment.setOrganizationId("org-a");
        comment.setContent("请确认付款条款。");
        comment.setCreateUser("author");
        comment.setUpdateUser("editor");
        return comment;
    }
}
