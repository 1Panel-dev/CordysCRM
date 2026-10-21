package cn.cordys.crm.approval.service;

import cn.cordys.crm.approval.domain.ApprovalComment;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.mapper.ExtApprovalCommentMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.MessageTask;
import cn.cordys.crm.system.mapper.ExtMessageTaskMapper;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApprovalCommentNoticeServiceTest {

    private static final String MODULE = NotificationConstants.Module.APPROVAL;
    private static final String EVENT = NotificationConstants.Event.APPROVAL_COMMENT_MENTIONED;

    private ApprovalCommentNoticeService service;
    private ExtApprovalCommentMapper commentMapper;
    private ExtMessageTaskMapper messageTaskMapper;
    private CommonNoticeSendService noticeSender;
    private ApprovalInstance instance;

    @BeforeEach
    void setUp() {
        service = new ApprovalCommentNoticeService();
        commentMapper = mock(ExtApprovalCommentMapper.class);
        messageTaskMapper = mock(ExtMessageTaskMapper.class);
        noticeSender = mock(CommonNoticeSendService.class);
        ReflectionTestUtils.setField(service, "extApprovalCommentMapper", commentMapper);
        ReflectionTestUtils.setField(service, "extMessageTaskMapper", messageTaskMapper);
        ReflectionTestUtils.setField(service, "commonNoticeSendService", noticeSender);
        instance = new ApprovalInstance();
        instance.setId("instance-a");
        instance.setType("contract");
        instance.setResourceName("合同甲");
        instance.setApprovalStatus("APPROVED");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void skipsNoticeWhenConfigurationOrScopedInstanceIsMissing(boolean configured) {
        if (configured) {
            when(messageTaskMapper.getMessageByModuleAndEvent(MODULE, EVENT, "org-a")).thenReturn(new MessageTask());
        }

        assertDoesNotThrow(() -> service.sendMentionNotice(comment("comment-a"), List.of("editor")));

        verifyNoInteractions(noticeSender);
        if (configured) {
            verify(commentMapper).selectInstance("instance-a", "org-a");
        } else {
            verifyNoInteractions(commentMapper);
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void differentCommentsInSameInstanceSendSeparateNoticesIncludingSelfMentions() {
        enableNotice();

        service.sendMentionNotice(comment("comment-a"), List.of("editor"));
        service.sendMentionNotice(comment("comment-b"), List.of("editor"));

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass((Class) Map.class);
        verify(noticeSender, times(2)).sendNotice(eq(MODULE), eq(EVENT), params.capture(),
                eq("editor"), eq("org-a"), eq(List.of("editor")), eq(false));
        assertEquals(List.of("comment-a", "comment-b"), params.getAllValues().stream().map(map -> map.get("resourceId")).toList());
        assertEquals("合同甲", params.getValue().get("name"));
        assertEquals("请确认付款条款。", params.getValue().get("commentSummary"));
        assertEquals("CONTRACT", params.getValue().get("resourceType"));
        assertFalse(params.getValue().containsKey("type"));
        assertFalse(params.getValue().containsKey("state"));
        verify(commentMapper, times(2)).selectInstance("instance-a", "org-a");
    }

    @ParameterizedTest
    @CsvSource({"clue,CLUE", "customer,CUSTOMER", "opportunity,OPPORTUNITY", "quotation,OPPORTUNITY",
            "contract,CONTRACT", "invoice,CONTRACT", "order,ORDER", "custom-form-a,APPROVAL"})
    @SuppressWarnings({"unchecked", "rawtypes"})
    void groupsMessagesByBusinessWhileKeepingApprovalNotificationConfiguration(String formType, String resourceType) {
        enableNotice();
        instance.setType(formType);
        instance.setResourceName(null);

        service.sendMentionNotice(comment("comment-a"), List.of("receiver"));

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass((Class) Map.class);
        verify(noticeSender).sendNotice(eq(MODULE), eq(EVENT), params.capture(),
                eq("editor"), eq("org-a"), eq(List.of("receiver")), eq(false));
        assertEquals(resourceType, params.getValue().get("resourceType"));
        assertEquals("", params.getValue().get("name"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SuppressWarnings({"unchecked", "rawtypes"})
    void summaryNormalizesWhitespaceAndTruncatesWithoutSplittingEmoji(boolean longComment) {
        enableNotice();
        ApprovalComment comment = comment("comment-a");
        comment.setContent(longComment ? "评".repeat(99) + "😀补充" : "  请确认\n\t付款条款。  ");

        service.sendMentionNotice(comment, List.of("receiver"));

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass((Class) Map.class);
        verify(noticeSender).sendNotice(eq(MODULE), eq(EVENT), params.capture(),
                eq("editor"), eq("org-a"), eq(List.of("receiver")), eq(false));
        assertEquals(longComment ? "评".repeat(99) + "😀…" : "请确认 付款条款。", params.getValue().get("commentSummary"));
    }

    @ParameterizedTest
    @CsvSource({"zh-CN,张三 在 合同甲 的审批评论中 @了你：请确认付款条款。",
            "en-US,Zhang mentioned you in an approval comment on 合同甲: 请确认付款条款。"})
    void defaultMessageContainsOnlyCommenterResourceNameAndSummary(String language, String expected) {
        Map<String, Object> params = new HashMap<>();
        params.put("OPERATOR", language.startsWith("zh") ? "张三" : "Zhang");
        params.put("name", "合同甲");
        params.put("commentSummary", "请确认付款条款。");
        String template = ResourceBundle.getBundle("i18n.cordys-crm", Locale.forLanguageTag(language))
                .getString("message.approval_comment_mentioned_text");

        assertEquals(expected, StringSubstitutor.replace(template, params));
    }

    @Test
    void notificationFailureDoesNotFailCommittedComment() {
        when(messageTaskMapper.getMessageByModuleAndEvent(MODULE, EVENT, "org-a"))
                .thenThrow(new IllegalStateException("通知配置读取失败"));

        assertDoesNotThrow(() -> service.sendMentionNotice(comment("comment-a"), List.of("receiver")));

        verifyNoInteractions(noticeSender);
    }

    private void enableNotice() {
        when(messageTaskMapper.getMessageByModuleAndEvent(MODULE, EVENT, "org-a")).thenReturn(new MessageTask());
        when(commentMapper.selectInstance("instance-a", "org-a")).thenReturn(instance);
    }

    private ApprovalComment comment(String id) {
        ApprovalComment comment = new ApprovalComment();
        comment.setId(id);
        comment.setInstanceId("instance-a");
        comment.setOrganizationId("org-a");
        comment.setContent("请确认付款条款。");
        comment.setCreateUser("author");
        comment.setUpdateUser("editor");
        return comment;
    }
}
