package cn.cordys.crm.approval.service;

import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.domain.ApprovalComment;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.mapper.ExtApprovalCommentMapper;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.MessageTask;
import cn.cordys.crm.system.mapper.ExtMessageTaskMapper;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.notice.NoticeSendService;
import cn.cordys.crm.system.notice.common.NoticeModel;
import cn.cordys.crm.system.notice.message.MessageDetailService;
import cn.cordys.crm.system.notice.sender.insite.InSiteNoticeSender;
import cn.cordys.crm.system.notice.sender.mail.MailNoticeSender;
import cn.cordys.crm.system.utils.MessageTemplateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ApprovalCommentNoticeServiceTest {

    private static final String MODULE = NotificationConstants.Module.APPROVAL;
    private static final String EVENT = NotificationConstants.Event.APPROVAL_COMMENT_MENTIONED;

    private ApprovalCommentNoticeService service;
    private ExtApprovalCommentMapper commentMapper;
    private ExtApprovalInstanceMapper instanceMapper;
    private ExtMessageTaskMapper messageTaskMapper;
    private CommonNoticeSendService noticeSender;
    private ApprovalInstance instance;
    private MessageSource previousMessageSource;
    private LocaleContext previousLocaleContext;

    @BeforeEach
    void setUp() {
        previousMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        previousLocaleContext = LocaleContextHolder.getLocaleContext();
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/cordys-crm");
        messageSource.setDefaultEncoding("UTF-8");
        ReflectionTestUtils.setField(Translator.class, "messageSource", messageSource);
        service = new ApprovalCommentNoticeService();
        commentMapper = mock(ExtApprovalCommentMapper.class);
        instanceMapper = mock(ExtApprovalInstanceMapper.class);
        messageTaskMapper = mock(ExtMessageTaskMapper.class);
        noticeSender = mock(CommonNoticeSendService.class);
        ReflectionTestUtils.setField(service, "extApprovalCommentMapper", commentMapper);
        ReflectionTestUtils.setField(service, "extApprovalInstanceMapper", instanceMapper);
        ReflectionTestUtils.setField(service, "extMessageTaskMapper", messageTaskMapper);
        ReflectionTestUtils.setField(service, "commonNoticeSendService", noticeSender);
        instance = new ApprovalInstance();
        instance.setId("instance-a");
        instance.setType("contract");
        instance.setResourceName("合同甲");
        instance.setApprovalStatus("APPROVED");
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(Translator.class, "messageSource", previousMessageSource);
        LocaleContextHolder.setLocaleContext(previousLocaleContext);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void skipsNoticeWhenConfigurationOrScopedInstanceIsMissing(boolean configured) {
        if (configured) {
            when(messageTaskMapper.getMessageByModuleAndEvent(MODULE, EVENT, "org-a")).thenReturn(new MessageTask());
        }

        assertDoesNotThrow(() -> service.sendNotice(comment("comment-a"), List.of("editor"), false));

        verifyNoInteractions(noticeSender);
        if (configured) {
            verify(commentMapper).selectLatestInstance("resource-a", "org-a");
        } else {
            verifyNoInteractions(commentMapper);
        }
        verifyNoInteractions(instanceMapper);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void differentCommentsInSameResourceSendSeparateNoticesIncludingSelfMentions() {
        enableNotice();

        service.sendNotice(comment("comment-a"), List.of("editor"), false);
        service.sendNotice(comment("comment-b"), List.of("editor"), false);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass((Class) Map.class);
        verify(noticeSender, times(2)).sendNotice(eq(MODULE), eq(EVENT), params.capture(),
                eq("editor"), eq("org-a"), eq(List.of("editor")), eq(false));
        assertEquals(List.of("comment-a", "comment-b"), params.getAllValues().stream().map(map -> map.get("resourceId")).toList());
        assertEquals("合同甲", params.getValue().get("name"));
        assertFalse(params.getValue().containsKey("commentSummary"));
        assertEquals("CONTRACT", params.getValue().get("resourceType"));
        assertFalse(params.getValue().containsKey("type"));
        assertFalse(params.getValue().containsKey("state"));
        verify(commentMapper, times(2)).selectLatestInstance("resource-a", "org-a");
    }

    @ParameterizedTest
    @CsvSource({"clue,CLUE", "customer,CUSTOMER", "opportunity,OPPORTUNITY", "quotation,OPPORTUNITY",
            "contract,CONTRACT", "invoice,CONTRACT", "order,ORDER", "custom-form-a,APPROVAL"})
    @SuppressWarnings({"unchecked", "rawtypes"})
    void groupsMessagesByBusinessWhileKeepingApprovalNotificationConfiguration(String formType, String resourceType) {
        enableNotice();
        instance.setType(formType);
        instance.setResourceName(null);

        service.sendNotice(comment("comment-a"), List.of("receiver"), false);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass((Class) Map.class);
        verify(noticeSender).sendNotice(eq(MODULE), eq(EVENT), params.capture(),
                eq("editor"), eq("org-a"), eq(List.of("receiver")), eq(false));
        assertEquals(resourceType, params.getValue().get("resourceType"));
        assertEquals("", params.getValue().get("name"));
    }

    @ParameterizedTest
    @CsvSource({
            "zh-CN,false,true,【评论提醒】张三 在【合同甲】的审批记录添加了评论并@了你",
            "zh-CN,true,true,【评论提醒】张三 在【合同甲】的审批记录回复了评论并@了你",
            "en-US,false,true,[Comment Reminder] Zhang added a comment to the approval record for [合同甲] and @ed you as well",
            "en-US,true,true,[Comment Reminder] Zhang replied to a comment on the approval record for [合同甲] and @ed you as well",
            "zh-CN,false,false,【评论提醒】张三 在【合同甲】的审批记录添加了评论",
            "zh-CN,true,false,【评论提醒】张三 在【合同甲】的审批记录回复了评论",
            "en-US,false,false,[Comment Reminder] Zhang added a comment to the approval record for [合同甲]",
            "en-US,true,false,[Comment Reminder] Zhang replied to a comment on the approval record for [合同甲]"
    })
    @SuppressWarnings({"unchecked", "rawtypes"})
    void noticeTemplateMatchesRecipientAndCommentActionWithoutIncludingContent(String language, boolean reply,
            boolean mentioned, String expected) {
        enableNotice();
        when(instanceMapper.getResourceOwner("contract", "resource-a", "org-a")).thenReturn("receiver");
        LocaleContextHolder.setLocale(Locale.forLanguageTag(language));
        ApprovalComment comment = comment("comment-a");
        comment.setParentId(reply ? "parent-a" : null);
        comment.setContent("@其他成员 请确认付款条款。");

        service.sendNotice(comment, mentioned ? List.of("receiver") : List.of(), !mentioned);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass((Class) Map.class);
        verify(noticeSender).sendNotice(eq(MODULE), eq(EVENT), params.capture(),
                eq("editor"), eq("org-a"), eq(List.of("receiver")), eq(false));
        Map<String, Object> variables = new HashMap<>(params.getValue());
        variables.put("OPERATOR", language.startsWith("zh") ? "张三" : "Zhang");
        variables.put("Language", language);
        variables.put("organizationId", "org-a");
        assertEquals("true", variables.get("useTemplate"));
        String action = reply ? "reply_" : "";
        assertEquals("message.approval_comment_" + action + (mentioned ? "mentioned_text" : "added_text"),
                variables.get("templateKey"));
        assertFalse(variables.containsKey("commentSummary"));

        MessageDetailService messageDetails = mock(MessageDetailService.class);
        NoticeSendService sender = new NoticeSendService(mock(MailNoticeSender.class), mock(InSiteNoticeSender.class),
                messageDetails);
        LocaleContextHolder.setLocale(language.startsWith("zh") ? Locale.US : Locale.SIMPLIFIED_CHINESE);
        sender.sendOther(MODULE, NoticeModel.builder().event(EVENT).paramMap(variables).build(), false);

        ArgumentCaptor<String> template = ArgumentCaptor.forClass(String.class);
        verify(messageDetails).searchMessageByTypeAndOrgId(eq(MODULE), eq(true), template.capture(), eq("org-a"));
        assertEquals(expected, StringSubstitutor.replace(template.getValue(), variables));
        assertEquals(language.startsWith("zh") ? "审批评论提醒" : "Approval comment reminder",
                MessageTemplateUtils.getEventMap().get(EVENT));
    }

    @Test
    void notificationFailureDoesNotFailCommittedComment() {
        when(messageTaskMapper.getMessageByModuleAndEvent(MODULE, EVENT, "org-a"))
                .thenThrow(new IllegalStateException("通知配置读取失败"));

        assertDoesNotThrow(() -> service.sendNotice(comment("comment-a"), List.of("receiver"), false));

        verifyNoInteractions(noticeSender);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SuppressWarnings({"unchecked", "rawtypes"})
    void ownerReceivesCommentOrReplyTemplateWithoutMentions(boolean reply) {
        enableNotice();
        when(instanceMapper.getResourceOwner("contract", "resource-a", "org-a")).thenReturn("owner");
        ApprovalComment comment = comment("comment-a");
        comment.setParentId(reply ? "parent-a" : null);

        service.sendNotice(comment, List.of(), true);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass((Class) Map.class);
        verify(noticeSender).sendNotice(eq(MODULE), eq(EVENT), params.capture(),
                eq("editor"), eq("org-a"), eq(List.of("owner")), eq(false));
        verify(instanceMapper).getResourceOwner("contract", "resource-a", "org-a");
        assertEquals(reply ? "message.approval_comment_reply_added_text" : "message.approval_comment_added_text",
                params.getValue().get("templateKey"));
        assertEquals("true", params.getValue().get("useTemplate"));
        assertEquals("comment-a", params.getValue().get("resourceId"));
        assertEquals("CONTRACT", params.getValue().get("resourceType"));
        verifyNoMoreInteractions(noticeSender);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SuppressWarnings({"unchecked", "rawtypes"})
    void ownerAndMentionedUsersReceiveDifferentTemplates(boolean reply) {
        enableNotice();
        when(instanceMapper.getResourceOwner("contract", "resource-a", "org-a")).thenReturn("owner");
        ApprovalComment comment = comment("comment-a");
        comment.setParentId(reply ? "parent-a" : null);

        service.sendNotice(comment, List.of("receiver"), true);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass((Class) Map.class);
        verify(noticeSender).sendNotice(eq(MODULE), eq(EVENT), params.capture(),
                eq("editor"), eq("org-a"), eq(List.of("receiver")), eq(false));
        assertEquals(reply ? "message.approval_comment_reply_mentioned_text" : "message.approval_comment_mentioned_text",
                params.getValue().get("templateKey"));
        verify(noticeSender).sendNotice(eq(MODULE), eq(EVENT), params.capture(),
                eq("editor"), eq("org-a"), eq(List.of("owner")), eq(false));
        assertEquals(reply ? "message.approval_comment_reply_added_text" : "message.approval_comment_added_text",
                params.getValue().get("templateKey"));
        verifyNoMoreInteractions(noticeSender);
    }

    @ParameterizedTest
    @CsvSource({"true,owner", "false,receiver"})
    @SuppressWarnings({"unchecked", "rawtypes"})
    void duplicateMentionsReceiveOneNoticeIncludingMentionedOwner(boolean notifyOwner, String mentionedUser) {
        enableNotice();
        when(instanceMapper.getResourceOwner("contract", "resource-a", "org-a")).thenReturn("owner");

        service.sendNotice(comment("comment-a"), List.of(mentionedUser, mentionedUser), notifyOwner);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass((Class) Map.class);
        verify(noticeSender).sendNotice(eq(MODULE), eq(EVENT), params.capture(),
                eq("editor"), eq("org-a"), eq(List.of(mentionedUser)), eq(false));
        assertEquals("message.approval_comment_mentioned_text", params.getValue().get("templateKey"));
        verifyNoMoreInteractions(noticeSender);
        if (!notifyOwner) {
            verifyNoInteractions(instanceMapper);
        }
    }

    @ParameterizedTest
    @CsvSource({"clue,clue", "customer,customer", "opportunity,opportunity", "contract,contract",
            "invoice,contract_invoice", "order,sales_order", "custom-form-a,custom_form_data"})
    @SuppressWarnings({"unchecked", "rawtypes"})
    void currentOwnerQueryUsesBusinessTableAndCommentOrganization(String formType, String table) {
        enableNotice();
        instance.setType(formType);
        when(instanceMapper.getResourceOwner(table, "resource-a", "org-a")).thenReturn("editor");

        service.sendNotice(comment("comment-a"), List.of(), true);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass((Class) Map.class);
        verify(noticeSender).sendNotice(eq(MODULE), eq(EVENT), params.capture(),
                eq("editor"), eq("org-a"), eq(List.of("editor")), eq(false));
        verify(instanceMapper).getResourceOwner(table, "resource-a", "org-a");
    }

    @ParameterizedTest
    @ValueSource(strings = {"contract", "quotation"})
    @SuppressWarnings({"unchecked", "rawtypes"})
    void resourcesWithoutOwnerKeepMentionNotice(String formType) {
        enableNotice();
        instance.setType(formType);

        service.sendNotice(comment("comment-a"), List.of("receiver"), true);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass((Class) Map.class);
        verify(noticeSender).sendNotice(eq(MODULE), eq(EVENT), params.capture(),
                eq("editor"), eq("org-a"), eq(List.of("receiver")), eq(false));
        verifyNoMoreInteractions(noticeSender);
        if ("quotation".equals(formType)) {
            verifyNoInteractions(instanceMapper);
        } else {
            verify(instanceMapper).getResourceOwner("contract", "resource-a", "org-a");
        }
    }

    @Test
    void noOwnerAndNoMentionDoesNotSendNotice() {
        enableNotice();

        service.sendNotice(comment("comment-a"), List.of(), true);

        verify(instanceMapper).getResourceOwner("contract", "resource-a", "org-a");
        verifyNoInteractions(noticeSender);
    }

    private void enableNotice() {
        when(messageTaskMapper.getMessageByModuleAndEvent(MODULE, EVENT, "org-a")).thenReturn(new MessageTask());
        when(commentMapper.selectLatestInstance("resource-a", "org-a")).thenReturn(instance);
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
